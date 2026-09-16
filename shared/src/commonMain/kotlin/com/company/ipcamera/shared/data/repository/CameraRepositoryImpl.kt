package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.core.common.security.InputValidator
import com.company.ipcamera.core.network.OnvifClient
import com.company.ipcamera.shared.common.createHttpClientEngine
import com.company.ipcamera.shared.data.datasource.local.CameraLocalDataSource
import com.company.ipcamera.shared.data.datasource.remote.CameraRemoteDataSource
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.ConnectionTestResult
import com.company.ipcamera.shared.domain.repository.DiscoveredCamera
import com.company.ipcamera.shared.domain.repository.DiscoveryConfig
import com.company.ipcamera.shared.domain.repository.DiscoveryMethod
import com.company.ipcamera.shared.domain.repository.DiscoveryProgress
import com.company.ipcamera.shared.domain.repository.ErrorCode
import com.company.ipcamera.shared.domain.repository.SettingsRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.datetime.Clock
import mu.KotlinLogging
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import com.company.ipcamera.core.network.DiscoveredCamera as OnvifDiscoveredCamera

private val logger = KotlinLogging.logger {}

/**
 * Реализация CameraRepository с использованием Data Sources (новая архитектура)
 *
 * Использует стратегию local-first: сначала проверяет локальную БД,
 * затем синхронизирует с удаленным API при необходимости.
 * При включённой настройке «только HTTPS» (network.sslEnabled) блокирует добавление/обновление камер с http:// URL.
 */
class CameraRepositoryImpl(
    private val localDataSource: CameraLocalDataSource,
    private val remoteDataSource: CameraRemoteDataSource? = null,
    private val settingsRepository: SettingsRepository? = null,
) : CameraRepository {
    /**
     * Стратегия выбора источника данных
     */
    private enum class DataSourceStrategy {
        LOCAL_ONLY, // Только локальная БД
        REMOTE_ONLY, // Только удаленный API
        LOCAL_FIRST, // Сначала локальная, затем удаленная (fallback)
        REMOTE_FIRST, // Сначала удаленная, затем локальная (fallback)
    }

    private val strategy =
        if (remoteDataSource != null) {
            DataSourceStrategy.LOCAL_FIRST
        } else {
            DataSourceStrategy.LOCAL_ONLY
        }

    // Кэш для списка всех камер
    private val allCamerasCacheKey = "all_cameras"

    // Кэш для отдельных камер по ID
    private val cameraCache = CameraCache(maxSize = 1000, expirationTime = 5.minutes)
    private val discoveryCacheKey = "discovered_cameras"
    private val discoveryCache = CameraCache(maxSize = 10, expirationTime = 7.minutes)
    private val statusCache = CameraCache(maxSize = 1000, expirationTime = 20.seconds)

    override suspend fun getCameras(): List<Camera> =
        withContext(Dispatchers.Default) {
            // Проверяем кэш
            val cached = cameraCache.get<List<Camera>>(allCamerasCacheKey)
            if (cached != null) {
                logger.debug { "Cache hit for all cameras" }
                return@withContext cached
            }

            // Если нет в кэше, загружаем данные
            val cameras =
                when (strategy) {
                    DataSourceStrategy.LOCAL_ONLY -> {
                        localDataSource.getCameras()
                    }
                    DataSourceStrategy.REMOTE_ONLY -> {
                        remoteDataSource?.getCameras()?.fold(
                            onSuccess = { it },
                            onError = {
                                logger.error(it) { "Error getting cameras from remote" }
                                emptyList()
                            },
                        ) ?: emptyList()
                    }
                    DataSourceStrategy.LOCAL_FIRST -> {
                        val localCameras = localDataSource.getCameras()
                        if (localCameras.isEmpty() && remoteDataSource != null) {
                            // Если локально пусто, пытаемся получить с сервера
                            remoteDataSource.getCameras().fold(
                                onSuccess = { remoteCameras ->
                                    // Сохраняем в локальную БД для кэширования
                                    localDataSource.saveCameras(remoteCameras).getOrNull()
                                    remoteCameras
                                },
                                onError = {
                                    logger.warn(it) { "Failed to get cameras from remote, using local" }
                                    localCameras
                                },
                            )
                        } else {
                            localCameras
                        }
                    }
                    DataSourceStrategy.REMOTE_FIRST -> {
                        remoteDataSource?.getCameras()?.fold(
                            onSuccess = { remoteCameras ->
                                // Сохраняем в локальную БД
                                localDataSource.saveCameras(remoteCameras).getOrNull()
                                remoteCameras
                            },
                            onError = {
                                logger.warn(it) { "Failed to get cameras from remote, using local" }
                                localDataSource.getCameras()
                            },
                        ) ?: localDataSource.getCameras()
                    }
                }

            // Сохраняем в кэш
            cameraCache.put(allCamerasCacheKey, cameras)
            logger.debug { "Loaded ${cameras.size} cameras and cached" }
            cameras
        }

    override suspend fun getCameraById(id: String): Camera? =
        withContext(Dispatchers.Default) {
            // Проверяем кэш
            val cached = cameraCache.get<Camera>(id)
            if (cached != null) {
                logger.debug { "Cache hit for camera: $id" }
                return@withContext cached
            }

            // Если нет в кэше, загружаем данные
            val camera =
                when (strategy) {
                    DataSourceStrategy.LOCAL_ONLY -> {
                        localDataSource.getCameraById(id)
                    }
                    DataSourceStrategy.REMOTE_ONLY -> {
                        remoteDataSource?.getCameraById(id)?.fold(
                            onSuccess = { it },
                            onError = {
                                logger.error(it) { "Error getting camera by id from remote: $id" }
                                null
                            },
                        )
                    }
                    DataSourceStrategy.LOCAL_FIRST -> {
                        localDataSource.getCameraById(id) ?: run {
                            // Если не найдено локально, пытаемся получить с сервера
                            remoteDataSource?.getCameraById(id)?.fold(
                                onSuccess = { camera ->
                                    // Сохраняем в локальную БД
                                    localDataSource.saveCamera(camera).getOrNull()
                                    camera
                                },
                                onError = {
                                    logger.warn(it) { "Failed to get camera from remote: $id" }
                                    null
                                },
                            )
                        }
                    }
                    DataSourceStrategy.REMOTE_FIRST -> {
                        remoteDataSource?.getCameraById(id)?.fold(
                            onSuccess = { camera ->
                                // Сохраняем в локальную БД
                                localDataSource.saveCamera(camera).getOrNull()
                                camera
                            },
                            onError = {
                                logger.warn(it) { "Failed to get camera from remote, trying local: $id" }
                                localDataSource.getCameraById(id)
                            },
                        ) ?: localDataSource.getCameraById(id)
                    }
                }

            // Сохраняем в кэш, если камера найдена
            camera?.let { cameraCache.put(id, it) }
            camera
        }

    override suspend fun addCamera(camera: Camera): Result<Camera> =
        withContext(Dispatchers.Default) {
            try {
                // Валидация входных данных
                val urlValidation = InputValidator.validateCameraUrl(camera.url)
                if (urlValidation is com.company.ipcamera.core.common.security.ValidationResult.Error) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Некорректный URL: ${urlValidation.message}"),
                    )
                }

                val nameValidation = InputValidator.validateCameraName(camera.name)
                if (nameValidation is com.company.ipcamera.core.common.security.ValidationResult.Error) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Некорректное имя: ${nameValidation.message}"),
                    )
                }

                val usernameValidation = InputValidator.validateUsername(camera.username)
                if (usernameValidation is com.company.ipcamera.core.common.security.ValidationResult.Error) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Некорректное имя пользователя: ${usernameValidation.message}"),
                    )
                }

                val passwordValidation = InputValidator.validatePassword(camera.password)
                if (passwordValidation is com.company.ipcamera.core.common.security.ValidationResult.Error) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Некорректный пароль: ${passwordValidation.message}"),
                    )
                }

                // Сохраняем локально
                val localResult = localDataSource.saveCamera(camera)

                // Если есть удаленный источник, синхронизируем
                val result =
                    if (remoteDataSource != null && localResult.isSuccess) {
                        remoteDataSource.createCamera(camera).fold(
                            onSuccess = { remoteCamera ->
                                // Обновляем локальную версию данными с сервера
                                localDataSource.updateCamera(remoteCamera).getOrNull()
                                Result.success(remoteCamera)
                            },
                            onError = { error ->
                                logger.warn(
                                    error,
                                ) { "Failed to sync camera to remote, but saved locally: ${camera.id}" }
                                // Возвращаем локальную версию, но помечаем для синхронизации
                                localResult
                            },
                        )
                    } else {
                        localResult
                    }

                // Инвалидируем кэш списка всех камер и добавляем новую камеру в кэш
                if (result.isSuccess) {
                    cameraCache.remove(allCamerasCacheKey)
                    result.getOrNull()?.let { cameraCache.put(it.id, it) }
                    logger.debug { "Cache invalidated and camera ${result.getOrNull()?.id ?: camera.id} cached after add" }
                }

                result
            } catch (e: Exception) {
                logger.error(e) { "Error adding camera: ${camera.id}" }
                Result.failure(e)
            }
        }

    override suspend fun updateCamera(camera: Camera): Result<Camera> =
        withContext(Dispatchers.Default) {
            try {
                // Валидация входных данных
                val urlValidation = InputValidator.validateCameraUrl(camera.url)
                if (urlValidation is com.company.ipcamera.core.common.security.ValidationResult.Error) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Некорректный URL: ${urlValidation.message}"),
                    )
                }
                // При включённой настройке «только HTTPS» блокируем http:// URL
                val requireHttps = settingsRepository?.getSystemSettings()?.network?.sslEnabled == true
                val httpsValidation = InputValidator.validateUrlHttpsOnly(camera.url, requireHttps)
                if (httpsValidation is com.company.ipcamera.core.common.security.ValidationResult.Error) {
                    return@withContext Result.failure(IllegalArgumentException(httpsValidation.message))
                }

                val nameValidation = InputValidator.validateCameraName(camera.name)
                if (nameValidation is com.company.ipcamera.core.common.security.ValidationResult.Error) {
                    return@withContext Result.failure(
                        IllegalArgumentException("Некорректное имя: ${nameValidation.message}"),
                    )
                }

                // Обновляем локально
                val localResult = localDataSource.updateCamera(camera)

                // Если есть удаленный источник, синхронизируем
                val result =
                    if (remoteDataSource != null && localResult.isSuccess) {
                        remoteDataSource.updateCamera(camera.id, camera).fold(
                            onSuccess = { remoteCamera ->
                                // Обновляем локальную версию данными с сервера
                                localDataSource.updateCamera(remoteCamera).getOrNull()
                                Result.success(remoteCamera)
                            },
                            onError = { error ->
                                logger.warn(
                                    error,
                                ) { "Failed to sync camera update to remote, but updated locally: ${camera.id}" }
                                // Возвращаем локальную версию
                                localResult
                            },
                        )
                    } else {
                        localResult
                    }

                // Инвалидируем кэш списка всех камер и обновляем кэш конкретной камеры; сбрасываем кэш статуса
                if (result.isSuccess) {
                    cameraCache.remove(allCamerasCacheKey)
                    result.getOrNull()?.let {
                        cameraCache.put(it.id, it)
                        statusCache.remove("status_${it.id}")
                    }
                    logger.debug { "Cache invalidated and camera ${result.getOrNull()?.id ?: camera.id} cached after update" }
                }

                result
            } catch (e: Exception) {
                logger.error(e) { "Error updating camera: ${camera.id}" }
                Result.failure(e)
            }
        }

    override suspend fun removeCamera(id: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                // Удаляем локально
                val localResult = localDataSource.deleteCamera(id)

                // Если есть удаленный источник, синхронизируем
                val result =
                    if (remoteDataSource != null && localResult.isSuccess) {
                        remoteDataSource.deleteCamera(id).fold(
                            onSuccess = {
                                Result.success(Unit)
                            },
                            onError = { error ->
                                logger.warn(error) { "Failed to delete camera from remote, but deleted locally: $id" }
                                // Возвращаем успех, так как локально удалено
                                localResult
                            },
                        )
                    } else {
                        localResult
                    }

                // Инвалидируем кэш списка всех камер, status cache и удаляем камеру из кэша
                if (result.isSuccess) {
                    cameraCache.remove(allCamerasCacheKey)
                    cameraCache.remove(id)
                    statusCache.remove("status_$id")
                    logger.debug { "Cache invalidated and camera $id removed from cache after delete" }
                }

                result
            } catch (e: Exception) {
                logger.error(e) { "Error removing camera: $id" }
                Result.failure(e)
            }
        }

    override suspend fun discoverCameras(forceRefresh: Boolean): List<DiscoveredCamera> =
        withContext(Dispatchers.IO) {
            try {
                if (forceRefresh) {
                    discoveryCache.remove(discoveryCacheKey)
                }
                val cached = discoveryCache.get<List<DiscoveredCamera>>(discoveryCacheKey)
                if (cached != null) {
                    logger.debug { "Cache hit for discovered cameras (${cached.size} cameras)" }
                    return@withContext cached
                }

                logger.info { "Starting camera discovery via ONVIF..." }
                val engine = createHttpClientEngine()
                val onvifClient = OnvifClient(engine)

                try {
                    val discovered: List<OnvifDiscoveredCamera> = onvifClient.discoverCameras(timeoutMillis = 5000)
                    logger.info { "Discovered ${discovered.size} cameras via ONVIF" }
                    val result =
                        discovered.map { onvifCamera ->
                            val ipAddress = extractIpFromUrl(onvifCamera.url)
                            val port = extractPortFromUrl(onvifCamera.url) ?: 554
                            DiscoveredCamera(
                                name = onvifCamera.name ?: "Unknown Camera",
                                url = onvifCamera.url,
                                model = onvifCamera.model,
                                manufacturer = onvifCamera.manufacturer,
                                ipAddress = ipAddress,
                                port = port,
                            )
                        }
                    discoveryCache.put(discoveryCacheKey, result)
                    logger.debug { "Cached ${result.size} discovered cameras" }
                    result
                } finally {
                    onvifClient.close()
                    engine.close()
                }
            } catch (e: Exception) {
                logger.error(e) { "Error during camera discovery: ${e.message}" }
                emptyList()
            }
        }

    private fun extractIpFromUrl(url: String): String =
        try {
            url
                .removePrefix("rtsp://")
                .removePrefix("http://")
                .removePrefix("https://")
                .substringBefore("/")
                .substringBefore(":")
        } catch (e: Exception) {
            logger.warn(e) { "Failed to extract IP from URL: $url" }
            url
        }

    private fun extractPortFromUrl(url: String): Int? =
        try {
            val cleanUrl =
                url
                    .removePrefix("rtsp://")
                    .removePrefix("http://")
                    .removePrefix("https://")
                    .substringBefore("/")
            if (cleanUrl.contains(":")) cleanUrl.substringAfter(":").toIntOrNull() else null
        } catch (e: Exception) {
            logger.warn(e) { "Failed to extract port from URL: $url" }
            null
        }

    override suspend fun testConnection(camera: Camera): ConnectionTestResult =
        withContext(Dispatchers.IO) {
            try {
                logger.info { "Testing connection to camera: ${camera.name} (${camera.url})" }
                val engine = createHttpClientEngine()
                val onvifClient = OnvifClient(engine)

                try {
                    val result =
                        onvifClient.testConnection(
                            url = camera.url,
                            username = camera.username,
                            password = camera.password,
                        )

                    val mapped =
                        when (result) {
                            is com.company.ipcamera.core.network.ConnectionTestResult.Success -> {
                                logger.info { "Camera connection test successful: ${camera.name}" }
                                ConnectionTestResult.Success(
                                    streams =
                                        result.streams.map { s ->
                                            com.company.ipcamera.shared.domain.repository.StreamInfo(
                                                type = s.type,
                                                resolution = s.resolution,
                                                fps = s.fps,
                                                codec = s.codec,
                                            )
                                        },
                                    capabilities =
                                        com.company.ipcamera.shared.domain.repository.CameraCapabilities(
                                            ptz = result.capabilities.ptz,
                                            audio = result.capabilities.audio,
                                            onvif = result.capabilities.onvif,
                                            analytics = result.capabilities.analytics,
                                        ),
                                )
                            }
                            is com.company.ipcamera.core.network.ConnectionTestResult.Failure -> {
                                logger.warn { "Camera connection test failed: ${camera.name} - ${result.error}" }
                                ConnectionTestResult.Failure(
                                    error = result.error,
                                    code = mapErrorCode(result.code),
                                )
                            }
                        }
                    mapped
                } finally {
                    onvifClient.close()
                    engine.close()
                }
            } catch (e: Exception) {
                logger.error(e) { "Error testing camera connection: ${e.message}" }
                ConnectionTestResult.Failure(
                    error = e.message ?: "Unknown error during connection test",
                    code = ErrorCode.CONNECTION_FAILED,
                )
            }
        }

    private fun mapErrorCode(code: com.company.ipcamera.core.network.ErrorCode): ErrorCode =
        when (code) {
            com.company.ipcamera.core.network.ErrorCode.CONNECTION_FAILED -> ErrorCode.CONNECTION_FAILED
            com.company.ipcamera.core.network.ErrorCode.AUTHENTICATION_FAILED -> ErrorCode.AUTHENTICATION_FAILED
            com.company.ipcamera.core.network.ErrorCode.TIMEOUT -> ErrorCode.TIMEOUT
            com.company.ipcamera.core.network.ErrorCode.INVALID_RESPONSE,
            com.company.ipcamera.core.network.ErrorCode.UNKNOWN_ERROR,
            -> ErrorCode.UNKNOWN
        }

    override suspend fun getCameraStatus(id: String): CameraStatus =
        withContext(Dispatchers.Default) {
            try {
                val statusCacheKey = "status_$id"
                val cachedStatus = statusCache.get<CameraStatus>(statusCacheKey)
                if (cachedStatus != null) {
                    logger.debug { "Cache hit for camera status: $id = $cachedStatus" }
                    return@withContext cachedStatus
                }
                val camera = getCameraById(id)
                val status = camera?.status ?: CameraStatus.UNKNOWN
                statusCache.put(statusCacheKey, status)
                logger.debug { "Cached camera status: $id = $status" }
                status
            } catch (e: Exception) {
                logger.error(e) { "Error getting camera status: $id" }
                CameraStatus.ERROR
            }
        }

    override suspend fun discoverCamerasWithProgress(
        forceRefresh: Boolean,
        config: DiscoveryConfig,
    ): Flow<DiscoveryProgress> =
        flow {
            try {
                val startTime = Clock.System.now().toEpochMilliseconds()

                emit(
                    DiscoveryProgress(
                        currentMethod = config.methods.firstOrNull() ?: DiscoveryMethod.WS_DISCOVERY,
                        methodProgress = 0f,
                        overallProgress = 0f,
                        devicesFoundSoFar = 0,
                        devicesTotalEstimated = null,
                        elapsedTimeMs = 0,
                        remainingTimeMs = null,
                        currentActivity = "Starting camera discovery...",
                        discoveredCamerasSnapshot = emptyList(),
                    ),
                )

                val onvifClient = OnvifClient(createHttpClientEngine())
                try {
                    val discovered = onvifClient.discoverCameras(timeoutMillis = config.timeoutPerMethod)
                    val result =
                        discovered.map { device ->
                            DiscoveredCamera(
                                name = device.name ?: "Unknown Camera",
                                url = device.url,
                                model = device.model,
                                manufacturer = device.manufacturer,
                                ipAddress = extractIpFromUrl(device.url),
                                port = extractPortFromUrl(device.url) ?: 554,
                            )
                        }

                    emit(
                        DiscoveryProgress(
                            currentMethod = DiscoveryMethod.WS_DISCOVERY,
                            methodProgress = 1f,
                            overallProgress = 1f,
                            devicesFoundSoFar = result.size,
                            devicesTotalEstimated = null,
                            elapsedTimeMs = Clock.System.now().toEpochMilliseconds() - startTime,
                            remainingTimeMs = 0L,
                            currentActivity = "Discovery completed",
                            discoveredCamerasSnapshot = result,
                        ),
                    )
                } finally {
                    onvifClient.close()
                }
            } catch (e: Exception) {
                logger.error(e) { "Camera discovery with progress failed: ${e.message}" }
                emit(
                    DiscoveryProgress(
                        currentMethod = DiscoveryMethod.WS_DISCOVERY,
                        methodProgress = 0f,
                        overallProgress = 0f,
                        devicesFoundSoFar = 0,
                        devicesTotalEstimated = null,
                        elapsedTimeMs = 0,
                        remainingTimeMs = null,
                        currentActivity = "Discovery failed: ${e.message}",
                        discoveredCamerasSnapshot = emptyList(),
                    ),
                )
            }
        }
}
