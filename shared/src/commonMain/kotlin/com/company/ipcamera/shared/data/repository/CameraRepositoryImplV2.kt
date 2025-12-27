package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.core.common.security.InputValidator
import com.company.ipcamera.shared.common.createHttpClientEngine
import com.company.ipcamera.shared.data.datasource.local.CameraLocalDataSource
import com.company.ipcamera.shared.data.datasource.remote.CameraRemoteDataSource
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.repository.*
import com.company.ipcamera.core.network.OnvifClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация CameraRepository с использованием Data Sources (новая архитектура)
 *
 * Использует стратегию local-first: сначала проверяет локальную БД,
 * затем синхронизирует с удаленным API при необходимости
 */
class CameraRepositoryImplV2(
    private val localDataSource: CameraLocalDataSource,
    private val remoteDataSource: CameraRemoteDataSource? = null
) : CameraRepository {

    /**
     * Стратегия выбора источника данных
     */
    private enum class DataSourceStrategy {
        LOCAL_ONLY,      // Только локальная БД
        REMOTE_ONLY,     // Только удаленный API
        LOCAL_FIRST,     // Сначала локальная, затем удаленная (fallback)
        REMOTE_FIRST     // Сначала удаленная, затем локальная (fallback)
    }

    private val strategy = if (remoteDataSource != null) {
        DataSourceStrategy.LOCAL_FIRST
    } else {
        DataSourceStrategy.LOCAL_ONLY
    }

    override suspend fun getCameras(): List<Camera> = withContext(Dispatchers.Default) {
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
                    }
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
                        }
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
                    }
                ) ?: localDataSource.getCameras()
            }
        }
    }

    override suspend fun getCameraById(id: String): Camera? = withContext(Dispatchers.Default) {
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
                    }
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
                        }
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
                    }
                ) ?: localDataSource.getCameraById(id)
            }
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

            // Сохраняем локально
            val localResult = localDataSource.saveCamera(camera)

            // Если есть удаленный источник, синхронизируем
            if (remoteDataSource != null && localResult.isSuccess) {
                remoteDataSource.createCamera(camera).fold(
                    onSuccess = { remoteCamera ->
                        // Обновляем локальную версию данными с сервера
                        localDataSource.updateCamera(remoteCamera).getOrNull()
                        Result.success(remoteCamera)
                    },
                    onError = { error ->
                        logger.warn(error) { "Failed to sync camera to remote, but saved locally: ${camera.id}" }
                        // Возвращаем локальную версию, но помечаем для синхронизации
                        localResult
                    }
                )
            } else {
                localResult
            }
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

            // Обновляем локально
            val localResult = localDataSource.updateCamera(camera)

            // Если есть удаленный источник, синхронизируем
            if (remoteDataSource != null && localResult.isSuccess) {
                remoteDataSource.updateCamera(camera.id, camera).fold(
                    onSuccess = { remoteCamera ->
                        // Обновляем локальную версию данными с сервера
                        localDataSource.updateCamera(remoteCamera).getOrNull()
                        Result.success(remoteCamera)
                    },
                    onError = { error ->
                        logger.warn(error) { "Failed to sync camera update to remote, but updated locally: ${camera.id}" }
                        // Возвращаем локальную версию
                        localResult
                    }
                )
            } else {
                localResult
            }
        } catch (e: Exception) {
            logger.error(e) { "Error updating camera: ${camera.id}" }
            Result.failure(e)
        }
    }

    override suspend fun removeCamera(id: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            // Удаляем локально
            val localResult = localDataSource.deleteCamera(id)

            // Если есть удаленный источник, синхронизируем
            if (remoteDataSource != null && localResult.isSuccess) {
                remoteDataSource.deleteCamera(id).fold(
                    onSuccess = {
                        Result.success(Unit)
                    },
                    onError = { error ->
                        logger.warn(error) { "Failed to delete camera from remote, but deleted locally: $id" }
                        // Возвращаем успех, так как локально удалено
                        localResult
                    }
                )
            } else {
                localResult
            }
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

