package com.company.ipcamera.core.network.onvif

/**
 * Интерфейс для работы с ONVIF Event Service
 *
 * Реализует WS-Notification стандарт для подписки на события камер
 */
interface OnvifEventService {
    /**
     * Подписаться на события камеры
     *
     * @param cameraUrl URL камеры
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @param filter Фильтр событий (опционально)
     * @param notificationConsumerUrl URL для получения уведомлений (опционально, будет сгенерирован автоматически)
     * @param subscriptionTime Время подписки в секундах (по умолчанию 3600 = 1 час)
     * @return Результат с подпиской или ошибкой
     */
    suspend fun subscribeToEvents(
        cameraUrl: String,
        username: String? = null,
        password: String? = null,
        filter: OnvifEventFilter? = null,
        notificationConsumerUrl: String? = null,
        subscriptionTime: Long = 3600
    ): Result<OnvifEventSubscription>

    /**
     * Отписаться от событий
     *
     * @param subscriptionId Идентификатор подписки
     * @return Результат операции
     */
    suspend fun unsubscribe(subscriptionId: String): Result<Unit>

    /**
     * Продлить подписку
     *
     * @param subscriptionId Идентификатор подписки
     * @param renewalTime Время продления в секундах (по умолчанию 3600 = 1 час)
     * @return Результат с обновленной подпиской или ошибкой
     */
    suspend fun renewSubscription(
        subscriptionId: String,
        renewalTime: Long = 3600
    ): Result<OnvifEventSubscription>

    /**
     * Получить свойства Event Service камеры
     *
     * @param cameraUrl URL камеры
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @return Результат со свойствами или ошибкой
     */
    suspend fun getEventProperties(
        cameraUrl: String,
        username: String? = null,
        password: String? = null
    ): Result<OnvifEventProperties>

    /**
     * Получить активную подписку по ID
     *
     * @param subscriptionId Идентификатор подписки
     * @return Подписка или null, если не найдена
     */
    suspend fun getSubscription(subscriptionId: String): OnvifEventSubscription?

    /**
     * Получить все активные подписки для камеры
     *
     * @param cameraUrl URL камеры
     * @return Список активных подписок
     */
    suspend fun getSubscriptionsForCamera(cameraUrl: String): List<OnvifEventSubscription>

    /**
     * Отменить все подписки для камеры
     *
     * @param cameraUrl URL камеры
     * @return Результат операции
     */
    suspend fun unsubscribeAll(cameraUrl: String): Result<Unit>

    // === PullPoint Subscription методы ===

    /**
     * Создать PullPoint подписку (более надежный способ для камер без push-поддержки)
     *
     * @param cameraUrl URL камеры
     * @param username Имя пользователя (опционально)
     * @param password Пароль (опционально)
     * @param filter Фильтр событий (опционально)
     * @param subscriptionTime Время подписки в секундах (по умолчанию 3600 = 1 час)
     * @return Результат с подпиской или ошибкой
     */
    suspend fun createPullPointSubscription(
        cameraUrl: String,
        username: String? = null,
        password: String? = null,
        filter: OnvifEventFilter? = null,
        subscriptionTime: Long = 3600
    ): Result<OnvifEventSubscription>

    /**
     * Получить события через PullPoint (PullMessages)
     *
     * @param subscriptionId Идентификатор подписки
     * @param timeout Таймаут ожидания событий в миллисекундах (по умолчанию 1000)
     * @param maxMessages Максимальное количество сообщений для получения (по умолчанию 10)
     * @return Результат со списком событий или ошибкой
     */
    suspend fun pullMessages(
        subscriptionId: String,
        timeout: Long = 1000,
        maxMessages: Int = 10
    ): Result<List<OnvifEvent>>

    /**
     * Установить точку синхронизации для PullPoint (SetSynchronizationPoint)
     *
     * @param subscriptionId Идентификатор подписки
     * @return Результат операции
     */
    suspend fun setSynchronizationPoint(subscriptionId: String): Result<Unit>
}
