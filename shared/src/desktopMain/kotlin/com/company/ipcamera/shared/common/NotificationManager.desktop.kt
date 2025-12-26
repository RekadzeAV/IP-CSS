package com.company.ipcamera.shared.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.Flow
import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.Image
import java.awt.Toolkit
import java.awt.event.ActionListener
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

actual class NotificationManager actual constructor(private val context: Any?) {
    private val systemTray: SystemTray? = if (SystemTray.isSupported()) {
        SystemTray.getSystemTray()
    } else {
        null
    }
    
    private val trayIcons = mutableMapOf<String, TrayIcon>()
    
    private val _notificationEvents = MutableStateFlow<NotificationEvent?>(null)
    private val notificationEventsFlow = _notificationEvents.asStateFlow()
        .filterNotNull()
    
    actual fun initialize() {
        // Инициализация системного трея
        // На некоторых системах может быть недоступен
    }
    
    actual suspend fun showNotification(notification: NotificationData) {
        if (systemTray == null) {
            // Если системный трей недоступен, используем альтернативный метод
            showFallbackNotification(notification)
            return
        }
        
        SwingUtilities.invokeLater {
            try {
                val image = createNotificationImage(notification.icon)
                val trayIcon = TrayIcon(image, notification.title)
                
                trayIcon.toolTip = notification.message
                trayIcon.isImageAutoSize = true
                
                // Обработчик клика
                trayIcon.addMouseListener(object : MouseAdapter() {
                    override fun mouseClicked(e: MouseEvent) {
                        if (e.clickCount >= 1) {
                            _notificationEvents.value = NotificationEvent.Clicked(notification.id)
                        }
                    }
                })
                
                // Добавляем в системный трей
                systemTray.add(trayIcon)
                trayIcons[notification.id] = trayIcon
                
                // Показываем всплывающее уведомление
                trayIcon.displayMessage(
                    notification.title,
                    notification.message,
                    mapNotificationType(notification.type)
                )
                
                // Автоматически удаляем через 5 секунд
                Thread {
                    Thread.sleep(5000)
                    SwingUtilities.invokeLater {
                        systemTray.remove(trayIcon)
                        trayIcons.remove(notification.id)
                    }
                }.start()
                
            } catch (e: Exception) {
                // Обработка ошибки
                showFallbackNotification(notification)
            }
        }
    }
    
    actual suspend fun showNotificationWithActions(
        notification: NotificationData,
        actions: List<NotificationAction>
    ) {
        // На Desktop действия в уведомлениях ограничены
        // Показываем обычное уведомление
        showNotification(notification)
        
        // Действия можно обработать через контекстное меню или отдельное окно
        // TODO: Реализовать поддержку действий для Desktop
    }
    
    actual fun cancelNotification(notificationId: String) {
        SwingUtilities.invokeLater {
            trayIcons[notificationId]?.let { trayIcon ->
                systemTray?.remove(trayIcon)
                trayIcons.remove(notificationId)
            }
        }
    }
    
    actual fun cancelAllNotifications() {
        SwingUtilities.invokeLater {
            trayIcons.values.forEach { trayIcon ->
                systemTray?.remove(trayIcon)
            }
            trayIcons.clear()
        }
    }
    
    actual fun createNotificationChannel(
        channelId: String,
        channelName: String,
        description: String,
        importance: NotificationPriority
    ) {
        // На Desktop каналы не используются
    }
    
    actual suspend fun hasPermission(): Boolean {
        // На Desktop разрешения на уведомления обычно не требуются
        return SystemTray.isSupported()
    }
    
    actual suspend fun requestPermission(): Boolean {
        // На Desktop разрешения обычно не требуются
        return hasPermission()
    }
    
    actual fun getNotificationEvents(): Flow<NotificationEvent> {
        return notificationEventsFlow
    }
    
    private fun showFallbackNotification(notification: NotificationData) {
        // Альтернативный метод показа уведомления
        // Можно использовать системные команды или логирование
        println("Notification: ${notification.title} - ${notification.message}")
    }
    
    private fun createNotificationImage(iconName: String?): Image {
        // Создаем простую иконку по умолчанию
        // TODO: Реализовать загрузку кастомных иконок
        val image = Toolkit.getDefaultToolkit().createImage(
            javaClass.getResource("/icon.png")
        ) ?: createDefaultImage()
        return image
    }
    
    private fun createDefaultImage(): Image {
        // Создаем простую иконку программно
        val image = java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.color = java.awt.Color.BLUE
        g.fillOval(0, 0, 16, 16)
        g.dispose()
        return image
    }
    
    private fun mapNotificationType(type: NotificationType): TrayIcon.MessageType {
        return when (type) {
            NotificationType.ERROR -> TrayIcon.MessageType.ERROR
            NotificationType.WARNING -> TrayIcon.MessageType.WARNING
            NotificationType.INFO, NotificationType.EVENT, NotificationType.ALERT, NotificationType.SYSTEM -> 
                TrayIcon.MessageType.INFO
        }
    }
}

