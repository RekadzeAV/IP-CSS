package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.shared.common.createHttpClientEngine
import com.company.ipcamera.shared.data.local.CameraEntityMapper
import com.company.ipcamera.shared.data.local.DatabaseFactory
import com.company.ipcamera.shared.data.local.createDatabase
import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.*
import com.company.ipcamera.core.network.OnvifClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация CameraRepository с использованием SQLDelight
 */
class CameraRepositoryImpl(
    private val databaseFactory: DatabaseFactory
) : CameraRepository {

    private val database = createDatabase(databaseFactory.createDriver())
    private val mapper = CameraEntityMapper()

    override suspend fun getCameras(): List<Camera> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.selectAll().executeAsList().map { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting cameras" }
            emptyList()
        }
    }

    override suspend fun getCameraById(id: String): Camera? = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.selectById(id).executeAsOneOrNull()?.let { mapper.toDomain(it) }
        } catch (e: Exception) {
            logger.error(e) { "Error getting camera by id: $id" }
            null
        }
    }

    override suspend fun addCamera(camera: Camera): Result<Camera> = withContext(Dispatchers.Default) {
        try {
            // Валидация входных данных
            val urlValidation = InputValidator.validateCameraUrl(camera.url)
            if (urlValidation is com.company.ipcamera.core.common.security.ValidationResult.Error) {
                return@withContext Result.failure(IllegalArgumentException("Некорректный URL: ${urlValidation.message}"))
            }

            val nameValidation = InputValidator.validateCameraName(camera.name)
            if (nameValidation is com.company.ipcamera.core.common.security.ValidationResult.Error) {
                return@withContext Result.failure(IllegalArgumentException("Некорректное имя: ${nameValidation.message}"))
            }

            val usernameValidation = InputValidator.validateUsername(camera.username)
            if (usernameValidation is com.company.ipcamera.core.common.security.ValidationResult.Error) {
                return@withContext Result.failure(IllegalArgumentException("Некорректное имя пользователя: ${usernameValidation.message}"))
            }

            val passwordValidation = InputValidator.validatePassword(camera.password)
            if (passwordValidation is com.company.ipcamera.core.common.security.ValidationResult.Error) {
                return@withContext Result.failure(IllegalArgumentException("Некорректный пароль: ${passwordValidation.message}"))
            }

            val dbCamera = mapper.toDatabase(camera)
            database.cameraDatabaseQueries.insertCamera(
                id = dbCamera.id,
                name = dbCamera.name,
                url = dbCamera.url,
                username = dbCamera.username,
                password = dbCamera.password,
                model = dbCamera.model,
                status = dbCamera.status,
                resolution_width = dbCamera.resolution_width,
                resolution_height = dbCamera.resolution_height,
                fps = dbCamera.fps,
                bitrate = dbCamera.bitrate,
                codec = dbCamera.codec,
                audio = dbCamera.audio,
                ptz_config = dbCamera.ptz_config,
                streams = dbCamera.streams,
                settings = dbCamera.settings,
                statistics = dbCamera.statistics,
                created_at = dbCamera.created_at,
                updated_at = dbCamera.updated_at,
                last_seen = dbCamera.last_seen
            )
            Result.success(camera)
        } catch (e: Exception) {
            logger.error(e) { "Error adding camera: ${camera.id}" }
            Result.failure(e)
        }
    }

    override suspend fun updateCamera(camera: Camera): Result<Camera> = withContext(Dispatchers.Default) {
        try {
            // Валидация входных данных
            val urlValidation = InputValidator.validateCameraUrl(camera.url)
            if (urlValidation is com.company.ipcamera.core.common.security.ValidationResult.Error) {
                return@withContext Result.failure(IllegalArgumentException("Некорректный URL: ${urlValidation.message}"))
            }

            val nameValidation = InputValidator.validateCameraName(camera.name)
            if (nameValidation is com.company.ipcamera.core.common.security.ValidationResult.Error) {
                return@withContext Result.failure(IllegalArgumentException("Некорректное имя: ${nameValidation.message}"))
            }

            val usernameValidation = InputValidator.validateUsername(camera.username)
            if (usernameValidation is com.company.ipcamera.core.common.security.ValidationResult.Error) {
                return@withContext Result.failure(IllegalArgumentException("Некорректное имя пользователя: ${usernameValidation.message}"))
            }

            val passwordValidation = InputValidator.validatePassword(camera.password)
            if (passwordValidation is com.company.ipcamera.core.common.security.ValidationResult.Error) {
                return@withContext Result.failure(IllegalArgumentException("Некорректный пароль: ${passwordValidation.message}"))
            }

            val dbCamera = mapper.toDatabase(camera.copy(updatedAt = System.currentTimeMillis()))
            // Используем правильный UPDATE запрос вместо INSERT OR REPLACE
            // Это сохраняет created_at и правильно обновляет только измененные поля
            database.cameraDatabaseQueries.updateCamera(
                name = dbCamera.name,
                url = dbCamera.url,
                username = dbCamera.username,
                password = dbCamera.password,
                model = dbCamera.model,
                status = dbCamera.status,
                resolution_width = dbCamera.resolution_width,
                resolution_height = dbCamera.resolution_height,
                fps = dbCamera.fps,
                bitrate = dbCamera.bitrate,
                codec = dbCamera.codec,
                audio = dbCamera.audio,
                ptz_config = dbCamera.ptz_config,
                streams = dbCamera.streams,
                settings = dbCamera.settings,
                statistics = dbCamera.statistics,
                updated_at = dbCamera.updated_at,
                last_seen = dbCamera.last_seen,
                id = dbCamera.id
            )
            Result.success(camera.copy(updatedAt = System.currentTimeMillis()))
        } catch (e: Exception) {
            logger.error(e) { "Error updating camera: ${camera.id}" }
            Result.failure(e)
        }
    }

    override suspend fun removeCamera(id: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            database.cameraDatabaseQueries.deleteCamera(id)
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error removing camera: $id" }
            Result.failure(e)
        }
    }

    override suspend fun discoverCameras(): List<DiscoveredCamera> = withContext(Dispatchers.IO) {
        try {
            logger.info { "Starting camera discovery via ONVIF..." }
            val engine = createHttpClientEngine()
            val onvifClient = OnvifClient(engine)

            try {
                // OnvifClient.discoverCameras() использует WS-Discovery для обнаружения камер
                // WS-Discovery полностью реализован для всех платформ (JVM, Android, iOS)
                // Метод отправляет Probe запрос на multicast адрес 239.255.255.250:3702
                // и собирает ответы от ONVIF устройств в сети
                val discovered = onvifClient.discoverCameras(timeoutMillis = 5000)

                logger.info { "Discovered ${discovered.size} cameras via ONVIF" }
                discovered
            } finally {
                onvifClient.close()
                engine.close()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error during camera discovery: ${e.message}" }
            emptyList()
        }
    }

    override suspend fun testConnection(camera: Camera): ConnectionTestResult = withContext(Dispatchers.IO) {
        try {
            logger.info { "Testing connection to camera: ${camera.name} (${camera.url})" }
            val engine = createHttpClientEngine()
            val onvifClient = OnvifClient(engine)

            try {
                // OnvifClient.testConnection() проверяет подключение через ONVIF GetCapabilities
                // и возвращает информацию о потоках и возможностях камеры
                val result = onvifClient.testConnection(
                    url = camera.url,
                    username = camera.username,
                    password = camera.password
                )

                when (result) {
                    is ConnectionTestResult.Success -> {
                        logger.info { "Camera connection test successful: ${camera.name}" }
                    }
                    is ConnectionTestResult.Failure -> {
                        logger.warn { "Camera connection test failed: ${camera.name} - ${result.error}" }
                    }
                }

                result
            } finally {
                onvifClient.close()
                engine.close()
            }
        } catch (e: Exception) {
            logger.error(e) { "Error testing camera connection: ${e.message}" }
            ConnectionTestResult.Failure(
                error = e.message ?: "Unknown error during connection test",
                code = ErrorCode.CONNECTION_FAILED
            )
        }
    }

    /**
     * Извлекает IP адрес из URL
     */
    private fun extractIpFromUrl(url: String): String {
        return try {
            val cleanUrl = url
                .removePrefix("rtsp://")
                .removePrefix("http://")
                .removePrefix("https://")
                .substringBefore("/")
                .substringBefore(":")
            cleanUrl
        } catch (e: Exception) {
            logger.warn(e) { "Failed to extract IP from URL: $url" }
            url
        }
    }

    /**
     * Извлекает порт из URL
     */
    private fun extractPortFromUrl(url: String): Int? {
        return try {
            val cleanUrl = url
                .removePrefix("rtsp://")
                .removePrefix("http://")
                .removePrefix("https://")
                .substringBefore("/")

            if (cleanUrl.contains(":")) {
                cleanUrl.substringAfter(":").toIntOrNull()
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getCameraStatus(id: String): CameraStatus = withContext(Dispatchers.Default) {
        try {
            val camera = getCameraById(id)
            camera?.status ?: CameraStatus.UNKNOWN
        } catch (e: Exception) {
            logger.error(e) { "Error getting camera status: $id" }
            CameraStatus.ERROR
        }
    }
}

