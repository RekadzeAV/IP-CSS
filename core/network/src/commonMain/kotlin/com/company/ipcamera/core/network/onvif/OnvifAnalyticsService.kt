package com.company.ipcamera.core.network.onvif

/**
 * Интерфейс для работы с ONVIF Analytics Service
 *
 * Реализует ONVIF Analytics Specification для управления аналитическими движками камер
 */
interface OnvifAnalyticsService {
    /**
     * Получить список доступных аналитических движков
     *
     * @param cameraUrl URL камеры
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат со списком движков или ошибкой
     */
    suspend fun getAnalyticsEngines(
        cameraUrl: String,
        username: String? = null,
        password: String? = null
    ): Result<List<OnvifAnalyticsEngine>>

    /**
     * Получить информацию о конкретном аналитическом движке
     *
     * @param cameraUrl URL камеры
     * @param engineToken Токен движка
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат с информацией о движке или ошибкой
     */
    suspend fun getAnalyticsEngine(
        cameraUrl: String,
        engineToken: String,
        username: String? = null,
        password: String? = null
    ): Result<OnvifAnalyticsEngine>

    /**
     * Получить список входных данных для аналитического движка
     *
     * @param cameraUrl URL камеры
     * @param engineToken Токен движка
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат со списком входных данных или ошибкой
     */
    suspend fun getAnalyticsEngineInputs(
        cameraUrl: String,
        engineToken: String,
        username: String? = null,
        password: String? = null
    ): Result<List<OnvifAnalyticsEngineInput>>

    /**
     * Создать новый аналитический движок
     *
     * @param cameraUrl URL камеры
     * @param configuration Конфигурация движка
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат с созданным движком или ошибкой
     */
    suspend fun createAnalyticsEngine(
        cameraUrl: String,
        configuration: OnvifAnalyticsEngineConfiguration,
        username: String? = null,
        password: String? = null
    ): Result<OnvifAnalyticsEngine>

    /**
     * Обновить конфигурацию аналитического движка
     *
     * @param cameraUrl URL камеры
     * @param engineToken Токен движка
     * @param configuration Новая конфигурация
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат операции
     */
    suspend fun setAnalyticsEngine(
        cameraUrl: String,
        engineToken: String,
        configuration: OnvifAnalyticsEngineConfiguration,
        username: String? = null,
        password: String? = null
    ): Result<Unit>

    /**
     * Удалить аналитический движок
     *
     * @param cameraUrl URL камеры
     * @param engineToken Токен движка
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат операции
     */
    suspend fun deleteAnalyticsEngine(
        cameraUrl: String,
        engineToken: String,
        username: String? = null,
        password: String? = null
    ): Result<Unit>

    /**
     * Получить входные данные аналитического движка
     *
     * @param cameraUrl URL камеры
     * @param engineToken Токен движка
     * @param inputToken Токен входных данных
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат с входными данными или ошибкой
     */
    suspend fun getAnalyticsEngineInput(
        cameraUrl: String,
        engineToken: String,
        inputToken: String,
        username: String? = null,
        password: String? = null
    ): Result<OnvifAnalyticsEngineInput>

    /**
     * Установить входные данные для аналитического движка
     *
     * @param cameraUrl URL камеры
     * @param engineToken Токен движка
     * @param inputToken Токен входных данных
     * @param configuration Конфигурация входных данных
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат операции
     */
    suspend fun setAnalyticsEngineInput(
        cameraUrl: String,
        engineToken: String,
        inputToken: String,
        configuration: OnvifAnalyticsEngineInputConfiguration,
        username: String? = null,
        password: String? = null
    ): Result<Unit>
}
