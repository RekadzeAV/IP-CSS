package com.company.ipcamera.server.routing

import com.company.ipcamera.server.config.OnvifEventsConfig
import com.company.ipcamera.server.dto.*
import com.company.ipcamera.server.middleware.AuthorizationMiddleware.requireRole
import com.company.ipcamera.server.middleware.validateRequest
import com.company.ipcamera.server.service.ExportService
import com.company.ipcamera.server.service.OnvifEventSubscriptionService
import com.company.ipcamera.server.service.ApiMetricsService
import com.company.ipcamera.server.service.CameraControlService
import com.company.ipcamera.server.validation.RequestValidator
import com.company.ipcamera.server.websocket.WebSocketManager
import com.company.ipcamera.server.websocket.WebSocketChannel
import com.company.ipcamera.shared.domain.model.ObservationCompliance
import com.company.ipcamera.shared.domain.model.UserRole
import com.company.ipcamera.shared.domain.repository.CameraRepository
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.*
import mu.KotlinLogging
import org.koin.ktor.ext.inject
import java.io.File
import java.net.InetSocketAddress
import java.net.Socket
import com.company.ipcamera.shared.domain.repository.DiscoveredCamera

private val logger = KotlinLogging.logger {}
private const val DISCOVERY_CONNECT_TIMEOUT_MS = 1200
private const val DISCOVERY_FALLBACK_ENABLED_KEY = "DISCOVERY_FALLBACK_ENABLED"
private const val DISCOVERY_KNOWN_HOSTS_CONFIG_KEY = "DISCOVERY_KNOWN_HOSTS_CONFIG"
private const val DISCOVERY_CONNECT_TIMEOUT_MS_KEY = "DISCOVERY_CONNECT_TIMEOUT_MS"

/**
 * Маршруты для управления камерами
 * Все маршруты требуют JWT аутентификации
 */
fun Route.cameraRoutes() {
    val cameraRepository: CameraRepository by inject()
    val exportService: ExportService by inject()
    val cameraControlService: CameraControlService by inject()
    val onvifSubscriptionService: OnvifEventSubscriptionService by inject()
    val onvifEventsConfig: OnvifEventsConfig by inject()
    val apiMetricsService: ApiMetricsService by inject()

    authenticate("jwt-auth") {
        route("/cameras") {
            // GET /api/v1/cameras - список всех камер с пагинацией и фильтрацией
            // Минимум VIEWER для просмотра камер
            get {
                requireRole(UserRole.VIEWER)
                try {
                    val page = call.request.queryParameters["page"]?.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull()?.coerceAtLeast(1)?.coerceAtMost(100) ?: 20
                    val statusFilter = call.request.queryParameters["status"]?.uppercase()

                    var cameras = cameraRepository.getCameras()

                    // Фильтрация по статусу
                    if (statusFilter != null) {
                        cameras = cameras.filter { it.status.name == statusFilter }
                    }

                    val total = cameras.size
                    val totalPages = if (total == 0) 1 else (total + limit - 1) / limit
                    val fromIndex = (page - 1) * limit
                    val pagedCameras = if (fromIndex < total) {
                        cameras.subList(fromIndex, minOf(fromIndex + limit, total))
                    } else {
                        emptyList()
                    }

                    val camerasDto = pagedCameras.map { it.toDto() }
                    val paginatedResponse = PaginatedResponse(
                        items = camerasDto,
                        total = total,
                        page = page,
                        limit = limit,
                        totalPages = totalPages
                    )

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = paginatedResponse,
                            message = "Cameras retrieved successfully"
                        )
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<PaginatedResponse<CameraDto>>(
                            success = false,
                            data = null,
                            message = "Error retrieving cameras: ${e.message}"
                        )
                    )
                }
            }

            // POST /api/v1/cameras - создание новой камеры (требует роль OPERATOR или выше)
            post {
                requireRole(UserRole.OPERATOR)

                try {
                    val request = call.receive<CreateCameraRequest>()

                    // Валидация запроса
                    if (!validateRequest(request) { RequestValidator.validateCreateCameraRequest(it) }) {
                        return@post
                    }

                    val camera = request.toDomain()

                    val result = cameraRepository.addCamera(camera)
                    result.fold(
                        onSuccess = { createdCamera ->
                            // Отправляем WebSocket событие о новой камере
                            try {
                                WebSocketManager.broadcastEvent(
                                    WebSocketChannel.CAMERAS,
                                    "camera_created",
                                    buildJsonObject {
                                        put("cameraId", createdCamera.id)
                                        put("name", createdCamera.name)
                                        put("url", createdCamera.url)
                                        put("status", createdCamera.status.name)
                                        put("timestamp", System.currentTimeMillis())
                                    }
                                )
                            } catch (e: Exception) {
                                logger.warn(e) { "Failed to send WebSocket event for camera creation" }
                            }

                            // ONVIF: авто-подписка на события при добавлении камеры
                            if (onvifEventsConfig.enabled && onvifSubscriptionService.supportsOnvifEvents(createdCamera)) {
                                CoroutineScope(Dispatchers.Default).launch {
                                    onvifSubscriptionService.subscribeToCameraEvents(
                                        createdCamera.id,
                                        usePullPoint = onvifEventsConfig.usePullPointByDefault
                                    ).onFailure { e ->
                                        logger.warn(e) { "Failed to subscribe to ONVIF events for new camera: ${createdCamera.id}" }
                                    }
                                }
                            }

                            call.respond(
                                HttpStatusCode.Created,
                                ApiResponse(
                                    success = true,
                                    data = createdCamera.toDto(),
                                    message = "Camera created successfully"
                                )
                            )
                        },
                        onFailure = { error ->
                            call.respond(
                                HttpStatusCode.BadRequest,
                                ApiResponse<CameraDto>(
                                    success = false,
                                    data = null,
                                    message = "Error creating camera: ${error.message}"
                                )
                            )
                        }
                    )
                } catch (e: Exception) {
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<CameraDto>(
                            success = false,
                            data = null,
                            message = "Internal server error: ${e.message}"
                        )
                    )
                }
            }

            // GET /api/v1/cameras/discover - обнаружение камер в сети
            get("/discover") {
                try {
                    val startedAt = System.currentTimeMillis()
                    val forceRefresh = call.request.queryParameters["refresh"]?.toBooleanStrictOrNull() ?: false
                    val protocol = call.request.queryParameters["protocol"] ?: "onvif" // onvif, rtsp, or all
                    
                    val discoveredCameras = when (protocol) {
                        "rtsp" -> {
                            // Только RTSP discovery через known hosts fallback
                            logger.info { "RTSP-only discovery requested" }
                            discoverCamerasFromKnownHosts()
                        }
                        "onvif" -> {
                            // Только ONVIF WS-Discovery
                            logger.info { "ONVIF-only discovery requested" }
                            cameraRepository.discoverCameras(forceRefresh = forceRefresh)
                        }
                        "all" -> {
                            // ONVIF + RTSP fallback
                            logger.info { "Combined discovery requested (ONVIF + RTSP)" }
                            val onvifCameras = cameraRepository.discoverCameras(forceRefresh = forceRefresh)
                            if (onvifCameras.isEmpty()) {
                                discoverCamerasFromKnownHosts()
                            } else {
                                onvifCameras
                            }
                        }
                        else -> {
                            // По умолчанию ONVIF
                            cameraRepository.discoverCameras(forceRefresh = forceRefresh)
                        }
                    }
                    
                    val fallbackEnabled = envOrProperty(DISCOVERY_FALLBACK_ENABLED_KEY)?.toBooleanStrictOrNull() ?: true
                                        // Fallback к known-hosts используется, когда ONVIF discovery пуст — как для
                    // явного протокола "onvif", так и для значения по умолчанию (параметр protocol не задан).
                    val usedFallback = discoveredCameras.isEmpty() && fallbackEnabled &&
                        (call.request.queryParameters["protocol"] == null || protocol == "onvif")
                    val mergedDiscovered = if (usedFallback) {
                        val fallback = discoverCamerasFromKnownHosts()
                        if (fallback.isNotEmpty()) {
                            logger.info { "Discovery fallback found ${fallback.size} camera(s) from known hosts config" }
                        }
                        fallback
                    } else {
                        discoveredCameras
                    }
                    val durationMs = System.currentTimeMillis() - startedAt
                    apiMetricsService.markDiscoverRequest(usedFallback = usedFallback)
                    logger.info {
                        "discover_result protocol=$protocol forceRefresh=$forceRefresh usedFallback=$usedFallback " +
                            "totalCount=${mergedDiscovered.size} durationMs=$durationMs"
                    }
                    val camerasDto = mergedDiscovered.map { it.toDto() }

                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = camerasDto,
                            message = "Cameras discovered successfully"
                        )
                    )
                } catch (e: Exception) {
                    apiMetricsService.markDiscoverFailure()
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<List<DiscoveredCameraDto>>(
                            success = false,
                            data = null,
                            message = "Error discovering cameras: ${e.message}"
                        )
                    )
                }
            }

            route("/{id}") {
                // GET /api/v1/cameras/{id} - получение камеры по ID
                get {
                    try {
                        val id = call.parameters["id"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<CameraDto>(
                                success = false,
                                data = null,
                                message = "Camera ID is required"
                            )
                        )

                        val camera = cameraRepository.getCameraById(id)
                        if (camera != null) {
                            call.respond(
                                HttpStatusCode.OK,
                                ApiResponse(
                                    success = true,
                                    data = camera.toDto(),
                                    message = "Camera retrieved successfully"
                                )
                            )
                        } else {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<CameraDto>(
                                    success = false,
                                    data = null,
                                    message = "Camera not found"
                                )
                            )
                        }
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<CameraDto>(
                                success = false,
                                data = null,
                                message = "Internal server error: ${e.message}"
                            )
                        )
                    }
                }

                // GET /api/v1/cameras/{id}/observation-summary — px/m и предупреждение по настройкам сцены
                get("/observation-summary") {
                    requireRole(UserRole.VIEWER)
                    try {
                        val id = call.parameters["id"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<CameraObservationSummaryDto>(
                                success = false,
                                data = null,
                                message = "Camera ID is required"
                            )
                        )

                        val camera = cameraRepository.getCameraById(id)
                        if (camera == null) {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<CameraObservationSummaryDto>(
                                    success = false,
                                    data = null,
                                    message = "Camera not found"
                                )
                            )
                            return@get
                        }

                        val summary = ObservationCompliance.summarize(camera)
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                data = summary.toDto(),
                                message = "Observation summary computed"
                            )
                        )
                    } catch (e: Exception) {
                        logger.error(e) { "Error computing observation summary" }
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<CameraObservationSummaryDto>(
                                success = false,
                                data = null,
                                message = "Internal server error: ${e.message}"
                            )
                        )
                    }
                }

                // PUT /api/v1/cameras/{id} - обновление камеры (требует роль OPERATOR или выше)
                put {
                    requireRole(UserRole.OPERATOR)

                    try {
                        val id = call.parameters["id"] ?: return@put call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<CameraDto>(
                                success = false,
                                data = null,
                                message = "Camera ID is required"
                            )
                        )

                        val request = call.receive<UpdateCameraRequest>()

                        // Валидация запроса
                        if (!validateRequest(request) { RequestValidator.validateUpdateCameraRequest(it) }) {
                            return@put
                        }

                        val existingCamera = cameraRepository.getCameraById(id)

                        if (existingCamera == null) {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<CameraDto>(
                                    success = false,
                                    data = null,
                                    message = "Camera not found"
                                )
                            )
                            return@put
                        }

                        // Обновляем только переданные поля
                        val updatedCamera = existingCamera.copy(
                            name = request.name ?: existingCamera.name,
                            url = request.url ?: existingCamera.url,
                            username = request.username ?: existingCamera.username,
                            password = request.password ?: existingCamera.password,
                            model = request.model ?: existingCamera.model,
                            resolution = request.resolution?.let {
                                com.company.ipcamera.core.common.model.Resolution(it.width, it.height)
                            } ?: existingCamera.resolution,
                            fps = request.fps ?: existingCamera.fps,
                            bitrate = request.bitrate ?: existingCamera.bitrate,
                            codec = request.codec ?: existingCamera.codec,
                            audio = request.audio ?: existingCamera.audio,
                            ptz = request.ptz?.let {
                                com.company.ipcamera.shared.domain.model.PTZConfig(
                                    enabled = it.enabled,
                                    type = com.company.ipcamera.shared.domain.model.PTZType.valueOf(it.type),
                                    presets = it.presets
                                )
                            } ?: existingCamera.ptz,
                            streams = request.streams?.map { stream ->
                                com.company.ipcamera.shared.domain.model.StreamConfig(
                                    type = com.company.ipcamera.shared.domain.model.StreamType.valueOf(stream.type),
                                    resolution = com.company.ipcamera.core.common.model.Resolution(stream.resolution.width, stream.resolution.height),
                                    fps = stream.fps,
                                    bitrate = stream.bitrate
                                )
                            } ?: existingCamera.streams,
                            settings = request.settings?.let { s ->
                                com.company.ipcamera.shared.domain.model.CameraSettings(
                                    recording = s.recording?.let { r ->
                                        com.company.ipcamera.shared.domain.model.RecordingSettings(
                                            enabled = r.enabled,
                                            mode = com.company.ipcamera.shared.domain.model.RecordingMode.valueOf(r.mode),
                                            quality = com.company.ipcamera.shared.domain.model.Quality.valueOf(r.quality),
                                            schedule = r.schedule
                                        )
                                    } ?: existingCamera.settings.recording,
                                analytics = s.analytics?.let { a ->
                                    com.company.ipcamera.shared.domain.model.AnalyticsSettings(
                                        motionDetection = a.motionDetection,
                                        zones = a.zones.map { zone ->
                                            com.company.ipcamera.shared.domain.model.DetectionZone(
                                                name = zone.name,
                                                polygon = zone.polygon,
                                                sensitivity = zone.sensitivity
                                            )
                                        },
                                        objectDetection = a.objectDetection,
                                        objectTypes = a.objectTypes,
                                        executionLocation = a.executionLocation?.let {
                                            com.company.ipcamera.shared.domain.model.AnalyticsExecutionLocation.valueOf(it)
                                        } ?: existingCamera.settings.analytics.executionLocation
                                    )
                                } ?: existingCamera.settings.analytics,
                                notifications = s.notifications?.let { n ->
                                    com.company.ipcamera.shared.domain.model.NotificationSettings(
                                        enabled = n.enabled,
                                        channels = n.channels,
                                        events = n.events
                                    )
                                } ?: existingCamera.settings.notifications,
                                observation = s.observation?.let { o ->
                                    com.company.ipcamera.shared.domain.model.ObservationSettings(
                                        zoneClass = o.zoneClass?.let { com.company.ipcamera.shared.domain.model.ObservationZoneClass.valueOf(it) },
                                        targetStreamFps = o.targetStreamFps,
                                        sceneWidthMeters = o.sceneWidthMeters,
                                        observationDistanceMeters = o.observationDistanceMeters,
                                        pixelsPerMeterOverride = o.pixelsPerMeterOverride,
                                        targetPixelsPerMeterMin = o.targetPixelsPerMeterMin
                                    )
                                } ?: existingCamera.settings.observation
                                )
                            } ?: existingCamera.settings,
                            updatedAt = System.currentTimeMillis()
                        )

                        val result = cameraRepository.updateCamera(updatedCamera)
                        result.fold(
                            onSuccess = { camera ->
                                // Отправляем WebSocket событие об обновлении камеры
                                try {
                                    WebSocketManager.broadcastEvent(
                                        WebSocketChannel.CAMERAS,
                                        "camera_updated",
                                        buildJsonObject {
                                            put("cameraId", camera.id)
                                            put("name", camera.name)
                                            put("status", camera.status.name)
                                            put("timestamp", System.currentTimeMillis())
                                        }
                                    )
                                } catch (e: Exception) {
                                    logger.warn(e) { "Failed to send WebSocket event for camera update" }
                                }

                                // ONVIF: переподписка после обновления камеры (отписка + подписка если подходит)
                                if (onvifEventsConfig.enabled) {
                                    CoroutineScope(Dispatchers.Default).launch {
                                        onvifSubscriptionService.unsubscribeFromCameraEvents(camera.id)
                                        if (onvifSubscriptionService.supportsOnvifEvents(camera)) {
                                            onvifSubscriptionService.subscribeToCameraEvents(
                                                camera.id,
                                                usePullPoint = onvifEventsConfig.usePullPointByDefault
                                            ).onFailure { e ->
                                                logger.warn(e) { "Failed to re-subscribe ONVIF for camera: ${camera.id}" }
                                            }
                                        }
                                    }
                                }

                                call.respond(
                                    HttpStatusCode.OK,
                                    ApiResponse(
                                        success = true,
                                        data = camera.toDto(),
                                        message = "Camera updated successfully"
                                    )
                                )
                            },
                            onFailure = { error ->
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<CameraDto>(
                                        success = false,
                                        data = null,
                                        message = "Error updating camera: ${error.message}"
                                    )
                                )
                            }
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<CameraDto>(
                                success = false,
                                data = null,
                                message = "Internal server error: ${e.message}"
                            )
                        )
                    }
                }

                // DELETE /api/v1/cameras/{id} - удаление камеры (только ADMIN)
                delete {
                    requireRole(UserRole.ADMIN)

                    try {
                        val id = call.parameters["id"] ?: return@delete call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Unit>(
                                success = false,
                                data = null,
                                message = "Camera ID is required"
                            )
                        )

                        val result = cameraRepository.removeCamera(id)
                        result.fold(
                            onSuccess = {
                                // ONVIF: отписка от событий камеры
                                if (onvifEventsConfig.enabled) {
                                    CoroutineScope(Dispatchers.Default).launch {
                                        onvifSubscriptionService.unsubscribeFromCameraEvents(id)
                                            .onFailure { e -> logger.warn(e) { "Failed to unsubscribe ONVIF for camera: $id" } }
                                    }
                                }

                                // Отправляем WebSocket событие об удалении камеры
                                try {
                                    WebSocketManager.broadcastEvent(
                                        WebSocketChannel.CAMERAS,
                                        "camera_deleted",
                                        buildJsonObject {
                                            put("cameraId", id)
                                            put("timestamp", System.currentTimeMillis())
                                        }
                                    )
                                } catch (e: Exception) {
                                    logger.warn(e) { "Failed to send WebSocket event for camera deletion" }
                                }

                                call.respond(
                                    HttpStatusCode.OK,
                                    ApiResponse<Unit>(
                                        success = true,
                                        data = null,
                                        message = "Camera deleted successfully"
                                    )
                                )
                            },
                            onFailure = { error ->
                                call.respond(
                                    HttpStatusCode.BadRequest,
                                    ApiResponse<Unit>(
                                        success = false,
                                        data = null,
                                        message = "Error deleting camera: ${error.message}"
                                    )
                                )
                            }
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Unit>(
                                success = false,
                                data = null,
                                message = "Internal server error: ${e.message}"
                            )
                        )
                    }
                }

                // GET /api/v1/cameras/{id}/status - получить статус камеры
                get("/status") {
                    try {
                        val id = call.parameters["id"] ?: return@get call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<Map<String, String>>(
                                success = false,
                                data = null,
                                message = "Camera ID is required"
                            )
                        )

                        val camera = cameraRepository.getCameraById(id)
                        if (camera == null) {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<Map<String, String>>(
                                    success = false,
                                    data = null,
                                    message = "Camera not found"
                                )
                            )
                            return@get
                        }

                        val status = cameraRepository.getCameraStatus(id)
                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                data = mapOf("status" to status.name),
                                message = "Camera status retrieved successfully"
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Map<String, String>>(
                                success = false,
                                data = null,
                                message = "Internal server error: ${e.message}"
                            )
                        )
                    }
                }

                // POST /api/v1/cameras/{id}/control - управление камерой (PTZ и т.д.)
                post("/control") {
                    requireRole(UserRole.OPERATOR)
                    try {
                        val id = call.parameters["id"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<CameraControlResponse>(
                                success = false,
                                data = null,
                                message = "Camera ID is required"
                            )
                        )

                        val camera = cameraRepository.getCameraById(id)
                        if (camera == null) {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<CameraControlResponse>(
                                    success = false,
                                    data = null,
                                    message = "Camera not found"
                                )
                            )
                            return@post
                        }

                        val request = call.receive<CameraControlRequest>()

                        // Реальное PTZ-управление через ONVIF
                        // (ContinuousMove / Stop / Zoom / GotoPreset через CameraControlService)
                        val result = cameraControlService.execute(
                            camera = camera,
                            action = request.action,
                            parameters = request.parameters,
                        )
                        val (status, message) = when (val r = result) {
                            is CameraControlService.ControlResult.Success -> HttpStatusCode.OK to r.message
                            is CameraControlService.ControlResult.Failure ->
                                if (r.message.startsWith("PTZ is not enabled") ||
                                    r.message.startsWith("Unsupported action") ||
                                    r.message.startsWith("Preset command")
                                ) {
                                    HttpStatusCode.BadRequest to r.message
                                } else {
                                    HttpStatusCode.BadGateway to r.message
                                }
                        }
                        val data = CameraControlResponse(
                            success = result is CameraControlService.ControlResult.Success,
                            message = message
                        )

                        call.respond(
                            status,
                            ApiResponse(
                                success = status == HttpStatusCode.OK,
                                data = data,
                                message = message
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<CameraControlResponse>(
                                success = false,
                                data = null,
                                message = "Internal server error: ${e.message}"
                            )
                        )
                    }
                }

                // POST /api/v1/cameras/{id}/test - тест подключения к камере
                post("/test") {
                    try {
                        val id = call.parameters["id"] ?: return@post call.respond(
                            HttpStatusCode.BadRequest,
                            ApiResponse<ConnectionTestResultDto>(
                                success = false,
                                data = null,
                                message = "Camera ID is required"
                            )
                        )

                        val camera = cameraRepository.getCameraById(id)
                        if (camera == null) {
                            call.respond(
                                HttpStatusCode.NotFound,
                                ApiResponse<ConnectionTestResultDto>(
                                    success = false,
                                    data = null,
                                    message = "Camera not found"
                                )
                            )
                            return@post
                        }

                        val testResult = cameraRepository.testConnection(camera)
                        val resultDto = testResult.toDto()

                        call.respond(
                            HttpStatusCode.OK,
                            ApiResponse(
                                success = true,
                                data = resultDto,
                                message = "Connection test completed"
                            )
                        )
                    } catch (e: Exception) {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<ConnectionTestResultDto>(
                                success = false,
                                data = null,
                                message = "Internal server error: ${e.message}"
                            )
                        )
                    }
                }
            }

            // POST /api/v1/cameras/bulk/delete - массовое удаление камер
            // Только ADMIN для массового удаления камер
            post("/bulk/delete") {
                requireRole(UserRole.ADMIN)
                try {
                    val request = call.receive<BulkDeleteCamerasRequest>()

                    // Валидация запроса
                    if (!validateRequest(request) { RequestValidator.validateBulkDeleteCamerasRequest(it) }) {
                        return@post
                    }

                    val failedIds = mutableListOf<String>()
                    val deletedIds = mutableListOf<String>()

                    // Удаляем каждую камеру
                    for (id in request.ids) {
                        val result = cameraRepository.removeCamera(id)
                        result.fold(
                            onSuccess = {
                                deletedIds.add(id)
                            },
                            onFailure = { error ->
                                logger.warn(error) { "Failed to delete camera: $id" }
                                failedIds.add(id)
                            }
                        )
                    }

                    // ONVIF: отписка от событий по каждой удалённой камере
                    if (onvifEventsConfig.enabled && deletedIds.isNotEmpty()) {
                        CoroutineScope(Dispatchers.Default).launch {
                            deletedIds.forEach { cameraId ->
                                onvifSubscriptionService.unsubscribeFromCameraEvents(cameraId)
                                    .onFailure { e -> logger.warn(e) { "Failed to unsubscribe ONVIF for camera: $cameraId" } }
                            }
                        }
                    }

                    // Отправляем WebSocket событие о массовом удалении камер
                    try {
                        WebSocketManager.broadcastEvent(
                            WebSocketChannel.CAMERAS,
                            "cameras_bulk_deleted",
                            buildJsonObject {
                                put("deletedCount", deletedIds.size)
                                put("failedCount", failedIds.size)
                                put("failedIds", JsonArray(failedIds.map { JsonPrimitive(it) }))
                                put("timestamp", System.currentTimeMillis())
                            }
                        )
                    } catch (e: Exception) {
                        logger.warn(e) { "Failed to send WebSocket event for bulk camera deletion" }
                    }

                    val response = BulkDeleteCamerasResponse(
                        deletedCount = deletedIds.size,
                        failedCount = failedIds.size,
                        failedIds = failedIds
                    )

                    val statusCode = if (failedIds.isEmpty()) HttpStatusCode.OK else HttpStatusCode.PartialContent
                    call.respond(
                        statusCode,
                        ApiResponse(
                            success = failedIds.isEmpty(),
                            data = response,
                            message = if (failedIds.isEmpty()) {
                                "All cameras deleted successfully"
                            } else {
                                "Some cameras could not be deleted"
                            }
                        )
                    )
                } catch (e: Exception) {
                    logger.error(e) { "Error bulk deleting cameras" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<BulkDeleteCamerasResponse>(
                            success = false,
                            data = null,
                            message = "Internal server error: ${e.message}"
                        )
                    )
                }
            }

            // GET /api/v1/cameras/export/csv - экспорт камер в CSV формат
            // Минимум VIEWER для экспорта камер
            get("/export/csv") {
                requireRole(UserRole.VIEWER)
                try {
                    val cameras = cameraRepository.getCameras()

                    // Экспортируем в CSV
                    val csvContent = exportService.exportCamerasToCsv(cameras)

                    // Устанавливаем заголовки для скачивания файла
                    call.response.headers.append("Content-Disposition", "attachment; filename=\"cameras_${System.currentTimeMillis()}.csv\"")
                    call.response.headers.append("Content-Type", "text/csv; charset=utf-8")

                    call.respondText(csvContent, ContentType.Text.CSV)
                } catch (e: Exception) {
                    logger.error(e) { "Error exporting cameras to CSV" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error exporting cameras to CSV: ${e.message}"
                        )
                    )
                }
            }

            // GET /api/v1/cameras/export/json - экспорт камер в JSON формат
            // Минимум VIEWER для экспорта камер
            get("/export/json") {
                requireRole(UserRole.VIEWER)
                try {
                    val cameras = cameraRepository.getCameras()

                    // Экспортируем в JSON
                    val jsonContent = exportService.exportCamerasToJson(cameras)

                    // Устанавливаем заголовки для скачивания файла
                    call.response.headers.append("Content-Disposition", "attachment; filename=\"cameras_${System.currentTimeMillis()}.json\"")
                    call.response.headers.append("Content-Type", "application/json; charset=utf-8")

                    call.respondText(jsonContent, ContentType.Application.Json)
                } catch (e: Exception) {
                    logger.error(e) { "Error exporting cameras to JSON" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                        ApiResponse<String>(
                            success = false,
                            data = null,
                            message = "Error exporting cameras to JSON: ${e.message}"
                        )
                    )
                }
            }
        }
    }
}

private fun discoverCamerasFromKnownHosts(): List<DiscoveredCamera> {
    val configPath = envOrProperty(DISCOVERY_KNOWN_HOSTS_CONFIG_KEY)
        ?.takeIf { it.isNotBlank() }
        ?: "config/test-cameras.local.json"
    val connectTimeoutMs = envOrProperty(DISCOVERY_CONNECT_TIMEOUT_MS_KEY)?.toIntOrNull() ?: DISCOVERY_CONNECT_TIMEOUT_MS
    val configFile = File(configPath)
    if (!configFile.exists()) {
        logger.info { "Discovery fallback config file not found: $configPath" }
        return emptyList()
    }

    return runCatching {
        val root = Json.parseToJsonElement(configFile.readText()).jsonObject
        val defaults = root["defaults"]?.jsonObject
        val defaultRtspPort = defaults?.get("rtspPort")?.jsonPrimitive?.intOrNull ?: 554
        val defaultRtspPath = defaults?.get("rtspPath")?.jsonPrimitive?.contentOrNull ?: "/stream"
        val defaultHttpPorts = defaults
            ?.get("httpPorts")
            ?.jsonArray
            ?.mapNotNull { it.jsonPrimitive.intOrNull }
            ?.ifEmpty { null }
            ?: listOf(80, 8080, 443)

        val cameras = root["cameras"]?.jsonArray ?: return emptyList()
        cameras.mapNotNull { item ->
            val obj = item.jsonObject
            val host = obj["host"]?.jsonPrimitive?.contentOrNull?.trim().orEmpty()
            if (host.isEmpty()) return@mapNotNull null
            val name = obj["name"]?.jsonPrimitive?.contentOrNull?.trim().takeUnless { it.isNullOrEmpty() }
                ?: obj["id"]?.jsonPrimitive?.contentOrNull?.trim()
                ?: "Known Camera"
            val rtspPort = obj["rtspPort"]?.jsonPrimitive?.intOrNull ?: defaultRtspPort
            val rtspPath = obj["rtspPath"]?.jsonPrimitive?.contentOrNull ?: defaultRtspPath

            val rtspReachable = isTcpPortOpen(host, rtspPort, connectTimeoutMs)
            val anyHttpReachable = defaultHttpPorts.any { isTcpPortOpen(host, it, connectTimeoutMs) }
            if (!rtspReachable && !anyHttpReachable) {
                return@mapNotNull null
            }

            DiscoveredCamera(
                name = name,
                url = "rtsp://$host:$rtspPort${if (rtspPath.startsWith("/")) rtspPath else "/$rtspPath"}",
                model = "known-host-fallback",
                manufacturer = "configured",
                ipAddress = host,
                port = rtspPort
            )
        }
    }.onFailure { e ->
        logger.warn(e) { "Failed to parse discovery fallback config: ${configFile.absolutePath}" }
    }.getOrDefault(emptyList())
}

private fun isTcpPortOpen(host: String, port: Int, timeoutMs: Int): Boolean {
    return runCatching {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), timeoutMs)
        }
        true
    }.getOrDefault(false)
}

private fun envOrProperty(key: String): String? =
    System.getProperty(key)?.takeIf { it.isNotBlank() } ?: System.getenv(key)

