package com.company.ipcamera.shared.common

import platform.Foundation.*
import platform.UserNotifications.*
import platform.darwin.NSObject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.Flow

actual class NotificationManager actual constructor(private val context: Any?) {
    private val notificationCenter: UNUserNotificationCenter = UNUserNotificationCenter.currentNotificationCenter()
    
    private val _notificationEvents = MutableStateFlow<NotificationEvent?>(null)
    private val notificationEventsFlow = _notificationEvents.asStateFlow()
        .filterNotNull()
    
    private val delegate = NotificationDelegate { event ->
        _notificationEvents.value = event
    }
    
    actual fun initialize() {
        notificationCenter.delegate = delegate
    }
    
    actual suspend fun showNotification(notification: NotificationData) {
        val content = UNMutableNotificationContent().apply {
            title = notification.title
            body = notification.message
            sound = if (notification.sound) UNNotificationSound.defaultSound else null
            badge = null
            
            // Добавляем userInfo для идентификации уведомления
            userInfo = mapOf(
                "notification_id" to notification.id,
                "type" to notification.type.name
            )
            
            // Добавляем экстра данные
            notification.extras.forEach { (key, value) ->
                userInfo = userInfo + (key to value)
            }
        }
        
        val request = UNNotificationRequest.requestWithIdentifier(
            notification.id,
            content = content,
            trigger = null // Немедленное уведомление
        )
        
        notificationCenter.addNotificationRequest(request) { error ->
            if (error != null) {
                // Обработка ошибки
            }
        }
    }
    
    actual suspend fun showNotificationWithActions(
        notification: NotificationData,
        actions: List<NotificationAction>
    ) {
        val content = UNMutableNotificationContent().apply {
            title = notification.title
            body = notification.message
            sound = if (notification.sound) UNNotificationSound.defaultSound else null
            badge = null
            
            userInfo = mapOf(
                "notification_id" to notification.id,
                "type" to notification.type.name
            )
        }
        
        // Создаем действия
        val notificationActions = actions.map { action ->
            UNNotificationAction.actionWithIdentifier(
                action.id,
                action.title,
                UNNotificationActionOptionsNone
            )
        }
        
        // Создаем категорию с действиями
        val category = UNNotificationCategory.categoryWithIdentifier(
            "ACTION_CATEGORY_${notification.id}",
            actions = notificationActions.toList(),
            intentIdentifiers = emptyList(),
            options = UNNotificationCategoryOptionsNone
        )
        
        content.categoryIdentifier = category.identifier
        
        // Регистрируем категорию
        notificationCenter.setNotificationCategories(setOf(category))
        
        val request = UNNotificationRequest.requestWithIdentifier(
            notification.id,
            content = content,
            trigger = null
        )
        
        notificationCenter.addNotificationRequest(request) { error ->
            if (error != null) {
                // Обработка ошибки
            }
        }
    }
    
    actual fun cancelNotification(notificationId: String) {
        notificationCenter.removePendingNotificationRequestsWithIdentifiers(listOf(notificationId))
        notificationCenter.removeDeliveredNotificationsWithIdentifiers(listOf(notificationId))
    }
    
    actual fun cancelAllNotifications() {
        notificationCenter.removeAllPendingNotificationRequests()
        notificationCenter.removeAllDeliveredNotifications()
    }
    
    actual fun createNotificationChannel(
        channelId: String,
        channelName: String,
        description: String,
        importance: NotificationPriority
    ) {
        // На iOS каналы не используются, это только для совместимости с Android API
    }
    
    actual suspend fun hasPermission(): Boolean {
        var granted = false
        val semaphore = dispatch_semaphore_create(0)
        
        notificationCenter.getNotificationSettingsWithCompletionHandler { settings ->
            granted = settings.authorizationStatus == UNAuthorizationStatusAuthorized
            dispatch_semaphore_signal(semaphore)
        }
        
        dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER)
        return granted
    }
    
    actual suspend fun requestPermission(): Boolean {
        var granted = false
        val semaphore = dispatch_semaphore_create(0)
        
        val options = UNAuthorizationOptionAlert or 
                     UNAuthorizationOptionBadge or 
                     UNAuthorizationOptionSound
        
        notificationCenter.requestAuthorizationWithOptions(
            options,
            completionHandler = { success, error ->
                granted = success
                if (error != null) {
                    // Обработка ошибки
                }
                dispatch_semaphore_signal(semaphore)
            }
        )
        
        dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER)
        return granted
    }
    
    actual fun getNotificationEvents(): Flow<NotificationEvent> {
        return notificationEventsFlow
    }
}

/**
 * Делегат для обработки событий уведомлений
 */
class NotificationDelegate(
    private val onEvent: (NotificationEvent) -> Unit
) : NSObject(), UNUserNotificationCenterDelegateProtocol {
    
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit
    ) {
        val notificationId = didReceiveNotificationResponse.notification().request().identifier()
        val actionIdentifier = didReceiveNotificationResponse.actionIdentifier()
        
        if (actionIdentifier == UNNotificationDefaultActionIdentifier) {
            // Клик по уведомлению
            onEvent(NotificationEvent.Clicked(notificationId))
        } else {
            // Клик по действию
            onEvent(NotificationEvent.Clicked(notificationId, actionIdentifier))
        }
        
        withCompletionHandler()
    }
    
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (UNNotificationPresentationOptions) -> Unit
    ) {
        // Уведомление получено, когда приложение на переднем плане
        // Показываем уведомление даже если приложение активно
        withCompletionHandler(
            UNNotificationPresentationOptionAlert or
            UNNotificationPresentationOptionBadge or
            UNNotificationPresentationOptionSound
        )
    }
}



