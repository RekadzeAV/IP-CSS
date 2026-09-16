package com.company.ipcamera.server.service

import com.company.ipcamera.core.network.OnvifClient
import com.company.ipcamera.core.network.onvif.*
import com.company.ipcamera.shared.domain.repository.CameraRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Сервис для управления ONVIF Analytics Engines
 *
 * Предоставляет высокоуровневый API для работы с аналитическими движками камер
 * через ONVIF Analytics Service
 */
class AnalyticsEngineService(
    private val onvifClient: OnvifClient,
    private val cameraRepository: CameraRepository
) {
    /**
     * Получить список всех аналитических движков для камеры
     *
     * @param cameraId ID камеры
     * @return Результат со списком движков или ошибкой
     */
    suspend fun getAnalyticsEngines(cameraId: String): Result<List<OnvifAnalyticsEngine>> =
        withContext(Dispatchers.IO) {
            try {
                val camera = cameraRepository.getCameraById(cameraId)
                    ?: return@withContext Result.failure(
                        IllegalArgumentException("Camera not found: $cameraId")
                    )

                val result = onvifClient.getAnalyticsEngines(
                    url = camera.url,
                    username = camera.username,
                    password = camera.password
                )

                result.fold(
                    onSuccess = { engines ->
                        logger.info { "Retrieved ${engines.size} analytics engines for camera: $cameraId" }
                        Result.success(engines)
                    },
                    onFailure = { error ->
                        logger.error(error) { "Failed to get analytics engines for camera: $cameraId" }
                        Result.failure(error)
                    }
                )
            } catch (e: Exception) {
                logger.error(e) { "Error getting analytics engines for camera: $cameraId" }
                Result.failure(e)
            }
        }

    /**
     * Получить информацию о конкретном аналитическом движке
     *
     * @param cameraId ID камеры
     * @param engineToken Токен движка
     * @return Результат с информацией о движке или ошибкой
     */
    suspend fun getAnalyticsEngine(
        cameraId: String,
        engineToken: String
    ): Result<OnvifAnalyticsEngine> = withContext(Dispatchers.IO) {
        try {
            val camera = cameraRepository.getCameraById(cameraId)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Camera not found: $cameraId")
                )

            val result = onvifClient.getAnalyticsEngine(
                url = camera.url,
                engineToken = engineToken,
                username = camera.username,
                password = camera.password
            )

            result.fold(
                onSuccess = { engine ->
                    logger.info { "Retrieved analytics engine: $engineToken for camera: $cameraId" }
                    Result.success(engine)
                },
                onFailure = { error ->
                    logger.error(error) { "Failed to get analytics engine: $engineToken for camera: $cameraId" }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting analytics engine: $engineToken for camera: $cameraId" }
            Result.failure(e)
        }
    }

    /**
     * Получить входные данные для аналитического движка
     *
     * @param cameraId ID камеры
     * @param engineToken Токен движка
     * @return Результат со списком входных данных или ошибкой
     */
    suspend fun getAnalyticsEngineInputs(
        cameraId: String,
        engineToken: String
    ): Result<List<OnvifAnalyticsEngineInput>> = withContext(Dispatchers.IO) {
        try {
            val camera = cameraRepository.getCameraById(cameraId)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Camera not found: $cameraId")
                )

            val result = onvifClient.getAnalyticsEngineInputs(
                url = camera.url,
                engineToken = engineToken,
                username = camera.username,
                password = camera.password
            )

            result.fold(
                onSuccess = { inputs ->
                    logger.info { "Retrieved ${inputs.size} inputs for engine: $engineToken" }
                    Result.success(inputs)
                },
                onFailure = { error ->
                    logger.error(error) { "Failed to get inputs for engine: $engineToken" }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting inputs for engine: $engineToken" }
            Result.failure(e)
        }
    }

    /**
     * Создать новый аналитический движок
     *
     * @param cameraId ID камеры
     * @param configuration Конфигурация движка
     * @return Результат с созданным движком или ошибкой
     */
    suspend fun createAnalyticsEngine(
        cameraId: String,
        configuration: OnvifAnalyticsEngineConfiguration
    ): Result<OnvifAnalyticsEngine> = withContext(Dispatchers.IO) {
        try {
            val camera = cameraRepository.getCameraById(cameraId)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Camera not found: $cameraId")
                )

            val result = onvifClient.createAnalyticsEngine(
                url = camera.url,
                configuration = configuration,
                username = camera.username,
                password = camera.password
            )

            result.fold(
                onSuccess = { engine ->
                    logger.info { "Created analytics engine: ${engine.token} for camera: $cameraId" }
                    Result.success(engine)
                },
                onFailure = { error ->
                    logger.error(error) { "Failed to create analytics engine for camera: $cameraId" }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error creating analytics engine for camera: $cameraId" }
            Result.failure(e)
        }
    }

    /**
     * Обновить конфигурацию аналитического движка
     *
     * @param cameraId ID камеры
     * @param engineToken Токен движка
     * @param configuration Новая конфигурация
     * @return Результат операции
     */
    suspend fun updateAnalyticsEngine(
        cameraId: String,
        engineToken: String,
        configuration: OnvifAnalyticsEngineConfiguration
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val camera = cameraRepository.getCameraById(cameraId)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Camera not found: $cameraId")
                )

            val result = onvifClient.setAnalyticsEngine(
                url = camera.url,
                engineToken = engineToken,
                configuration = configuration,
                username = camera.username,
                password = camera.password
            )

            result.fold(
                onSuccess = {
                    logger.info { "Updated analytics engine: $engineToken for camera: $cameraId" }
                    Result.success(Unit)
                },
                onFailure = { error ->
                    logger.error(error) { "Failed to update analytics engine: $engineToken" }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error updating analytics engine: $engineToken" }
            Result.failure(e)
        }
    }

    /**
     * Удалить аналитический движок
     *
     * @param cameraId ID камеры
     * @param engineToken Токен движка
     * @return Результат операции
     */
    suspend fun deleteAnalyticsEngine(
        cameraId: String,
        engineToken: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val camera = cameraRepository.getCameraById(cameraId)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Camera not found: $cameraId")
                )

            val result = onvifClient.deleteAnalyticsEngine(
                url = camera.url,
                engineToken = engineToken,
                username = camera.username,
                password = camera.password
            )

            result.fold(
                onSuccess = {
                    logger.info { "Deleted analytics engine: $engineToken for camera: $cameraId" }
                    Result.success(Unit)
                },
                onFailure = { error ->
                    logger.error(error) { "Failed to delete analytics engine: $engineToken" }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error deleting analytics engine: $engineToken" }
            Result.failure(e)
        }
    }

    /**
     * Получить входные данные аналитического движка
     *
     * @param cameraId ID камеры
     * @param engineToken Токен движка
     * @param inputToken Токен входных данных
     * @return Результат с входными данными или ошибкой
     */
    suspend fun getAnalyticsEngineInput(
        cameraId: String,
        engineToken: String,
        inputToken: String
    ): Result<OnvifAnalyticsEngineInput> = withContext(Dispatchers.IO) {
        try {
            val camera = cameraRepository.getCameraById(cameraId)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Camera not found: $cameraId")
                )

            val result = onvifClient.getAnalyticsEngineInput(
                url = camera.url,
                engineToken = engineToken,
                inputToken = inputToken,
                username = camera.username,
                password = camera.password
            )

            result.fold(
                onSuccess = { input ->
                    logger.info { "Retrieved input: $inputToken for engine: $engineToken" }
                    Result.success(input)
                },
                onFailure = { error ->
                    logger.error(error) { "Failed to get input: $inputToken" }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting input: $inputToken" }
            Result.failure(e)
        }
    }

    /**
     * Установить входные данные для аналитического движка
     *
     * @param cameraId ID камеры
     * @param engineToken Токен движка
     * @param inputToken Токен входных данных
     * @param configuration Конфигурация входных данных
     * @return Результат операции
     */
    suspend fun setAnalyticsEngineInput(
        cameraId: String,
        engineToken: String,
        inputToken: String,
        configuration: OnvifAnalyticsEngineInputConfiguration
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val camera = cameraRepository.getCameraById(cameraId)
                ?: return@withContext Result.failure(
                    IllegalArgumentException("Camera not found: $cameraId")
                )

            val result = onvifClient.setAnalyticsEngineInput(
                url = camera.url,
                engineToken = engineToken,
                inputToken = inputToken,
                configuration = configuration,
                username = camera.username,
                password = camera.password
            )

            result.fold(
                onSuccess = {
                    logger.info { "Set input: $inputToken for engine: $engineToken" }
                    Result.success(Unit)
                },
                onFailure = { error ->
                    logger.error(error) { "Failed to set input: $inputToken" }
                    Result.failure(error)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error setting input: $inputToken" }
            Result.failure(e)
        }
    }
}
