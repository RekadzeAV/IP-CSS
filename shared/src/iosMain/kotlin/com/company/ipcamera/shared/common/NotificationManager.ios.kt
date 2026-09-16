package com.company.ipcamera.shared.common

import kotlinx.cinterop.CValue
import kotlinx.cinterop.dispatch_semaphore_create
import kotlinx.cinterop.dispatch_semaphore_signal
import kotlinx.cinterop.dispatch_semaphore_wait
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import platform.Foundation.mapOf
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationActionOptionsNone
import platform.UserNotifications.UNNotificationCategoryOptionsNone
import platform.UserNotifications.UNNotificationDefaultActionIdentifier
import platform.UserNotifications.UNNotificationPresentationOptionAlert
import platform.UserNotifications.UNNotificationPresentationOptionBadge
import platform.UserNotifications.UNNotificationPresentationOptionSound
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.DISPATCH_TIME_FOREVER
import platform.darwin.NSObject

actual class NotificationManager actual constructor(private val context: Any?) {
    private val notificationCenter: UNUserNotificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    private val notificationEventsState = MutableStateFlow<NotificationEvent?>(null)
    val notificationEventsFlow: Flow<NotificationEvent>
        get() = notificationEventsState.asStateFlow().filterNotNull()

    private val delegate =
        NotificationDelegate { event ->
            notificationEventsState.value = event
        }

    actual fun initialize() {
        notificationCenter.delegate = delegate
    }

    actual suspend fun showNotification(notification: NotificationData) {
        val content =
            UNMutableNotificationContent().apply {
                title = notification.title
                body = notification.message
                sound = if (notification.sound) UNNotificationSound.defaultSound else null
                badge = null

                userInfo =
                    mapOf(
                        "notification_id" to notification.id,
                        "type" to notification.type.name,
                    )

                notification.extras.forEach { (key, value) ->
                    userInfo = userInfo + (key to value)
                }
            }

        val request =
            UNNotificationRequest.requestWithIdentifier(
                notification.id,
                content = content,
                trigger = null,
            )

        notificationCenter.addNotificationRequest(request) { error ->
            if (error != null) {
                // Обработка ошибки
            }
        }
    }

    actual suspend fun showNotificationWithActions(
        notification: NotificationData,
        actions: List<NotificationAction>,
    ) {
        val content =
            UNMutableNotificationContent().apply {
                title = notification.title
                body = notification.message
                sound = if (notification.sound) UNNotificationSound.defaultSound else null
                badge = null

                userInfo =
                    mapOf(
                        "notification_id" to notification.id,
                        "type" to notification.type.name,
                    )
            }

        val notificationActions =
            actions.map { action ->
                UNNotificationAction.actionWithIdentifier(
                    action.id,
                    action.title,
                    UNNotificationActionOptionsNone,
                )
            }

        val category =
            UNNotificationCategory.categoryWithIdentifier(
                "ACTION_CATEGORY_${notification.id}",
                actions = notificationActions.toList(),
                intentIdentifiers = emptyList(),
                options = UNNotificationCategoryOptionsNone,
            )

        content.categoryIdentifier = category.identifier
        notificationCenter.setNotificationCategories(setOf(category))

        val request =
            UNNotificationRequest.requestWithIdentifier(
                notification.id,
                content = content,
                trigger = null,
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
        importance: NotificationPriority,
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

        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionBadge or UNAuthorizationOptionSound

        notificationCenter.requestAuthorizationWithOptions(
            options,
            completionHandler = { success, error ->
                granted = success
                if (error != null) {
                    // Обработка ошибки
                }
                dispatch_semaphore_signal(semaphore)
            },
        )

        dispatch_semaphore_wait(semaphore, DISPATCH_TIME_FOREVER)
        return granted
    }

    actual fun getNotificationEvents(): Flow<NotificationEvent> = notificationEventsFlow
}

/**
 * Делегат для обработки событий уведомлений
 */
class NotificationDelegate(
    private val onEvent: (NotificationEvent) -> Unit,
) : NSObject(), UNUserNotificationCenterDelegateProtocol {
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit,
    ) {
        val notificationId = didReceiveNotificationResponse.notification().request().identifier()
        val actionIdentifier = didReceiveNotificationResponse.actionIdentifier()

        if (actionIdentifier == UNNotificationDefaultActionIdentifier) {
            onEvent(NotificationEvent.Clicked(notificationId))
        } else {
            onEvent(NotificationEvent.Clicked(notificationId, actionIdentifier))
        }

        withCompletionHandler()
    }

    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (CValue<UInt>) -> Unit,
    ) {
        withCompletionHandler(
            UNNotificationPresentationOptionAlert or
                UNNotificationPresentationOptionBadge or
                UNNotificationPresentationOptionSound,
        )
    }
}
