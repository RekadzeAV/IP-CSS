package com.company.ipcamera.core.network.onvif

/**
 * Интерфейс для работы с ONVIF Imaging Service
 *
 * Реализует ONVIF Imaging Specification для управления настройками изображения камеры
 */
interface OnvifImagingService {
    /**
     * Получить настройки изображения
     *
     * @param cameraUrl URL камеры
     * @param videoSourceToken Токен видеоисточника
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат с настройками изображения или ошибкой
     */
    suspend fun getImagingSettings(
        cameraUrl: String,
        videoSourceToken: String,
        username: String? = null,
        password: String? = null
    ): Result<OnvifImagingSettings>

    /**
     * Установить настройки изображения
     *
     * @param cameraUrl URL камеры
     * @param videoSourceToken Токен видеоисточника
     * @param settings Настройки изображения
     * @param forcePersistence Принудительно сохранить настройки
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат операции
     */
    suspend fun setImagingSettings(
        cameraUrl: String,
        videoSourceToken: String,
        settings: OnvifImagingSettings,
        forcePersistence: Boolean = false,
        username: String? = null,
        password: String? = null
    ): Result<Unit>

    /**
     * Получить доступные опции для настроек изображения
     *
     * @param cameraUrl URL камеры
     * @param videoSourceToken Токен видеоисточника
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат с опциями или ошибкой
     */
    suspend fun getOptions(
        cameraUrl: String,
        videoSourceToken: String,
        username: String? = null,
        password: String? = null
    ): Result<OnvifImagingOptions>

    /**
     * Получить опции для перемещения (для PTZ камер)
     *
     * @param cameraUrl URL камеры
     * @param videoSourceToken Токен видеоисточника
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат с опциями перемещения или ошибкой
     */
    suspend fun getMoveOptions(
        cameraUrl: String,
        videoSourceToken: String,
        username: String? = null,
        password: String? = null
    ): Result<OnvifMoveOptions>

    /**
     * Переместить (для PTZ камер с оптическим зумом)
     *
     * @param cameraUrl URL камеры
     * @param videoSourceToken Токен видеоисточника
     * @param focus Focus настройки
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат операции
     */
    suspend fun move(
        cameraUrl: String,
        videoSourceToken: String,
        focus: OnvifFocusSettings,
        username: String? = null,
        password: String? = null
    ): Result<Unit>

    /**
     * Остановить перемещение
     *
     * @param cameraUrl URL камеры
     * @param videoSourceToken Токен видеоисточника
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат операции
     */
    suspend fun stop(
        cameraUrl: String,
        videoSourceToken: String,
        username: String? = null,
        password: String? = null
    ): Result<Unit>

    /**
     * Получить статус (для асинхронных операций)
     *
     * @param cameraUrl URL камеры
     * @param videoSourceToken Токен видеоисточника
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат со статусом или ошибкой
     */
    suspend fun getStatus(
        cameraUrl: String,
        videoSourceToken: String,
        username: String? = null,
        password: String? = null
    ): Result<OnvifImagingStatus>

    /**
     * Получить список пресетов изображения
     *
     * @param cameraUrl URL камеры
     * @param videoSourceToken Токен видеоисточника
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат со списком пресетов или ошибкой
     */
    suspend fun getPresets(
        cameraUrl: String,
        videoSourceToken: String,
        username: String? = null,
        password: String? = null
    ): Result<List<OnvifImagingPreset>>

    /**
     * Установить пресет изображения
     *
     * @param cameraUrl URL камеры
     * @param videoSourceToken Токен видеоисточника
     * @param presetToken Токен пресета (опционально, для обновления существующего)
     * @param presetName Имя пресета
     * @param settings Настройки изображения для пресета
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат с токеном пресета или ошибкой
     */
    suspend fun setPreset(
        cameraUrl: String,
        videoSourceToken: String,
        presetToken: String? = null,
        presetName: String,
        settings: OnvifImagingSettings,
        username: String? = null,
        password: String? = null
    ): Result<String>

    /**
     * Удалить пресет изображения
     *
     * @param cameraUrl URL камеры
     * @param videoSourceToken Токен видеоисточника
     * @param presetToken Токен пресета
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат операции
     */
    suspend fun removePreset(
        cameraUrl: String,
        videoSourceToken: String,
        presetToken: String,
        username: String? = null,
        password: String? = null
    ): Result<Unit>
}
