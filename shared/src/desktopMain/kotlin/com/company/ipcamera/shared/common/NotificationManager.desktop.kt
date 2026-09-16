package com.company.ipcamera.shared.common

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.java.Java
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import java.awt.Image
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.SystemTray
import java.awt.Toolkit
import java.awt.TrayIcon
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.SwingUtilities

actual class NotificationManager actual constructor(private val context: Any?) {
    private val systemTray: SystemTray? =
        if (SystemTray.isSupported()) {
            SystemTray.getSystemTray()
        } else {
            null
        }

    private val trayIcons = mutableMapOf<String, TrayIcon>()

    private val notificationEvents = MutableStateFlow<NotificationEvent?>(null)
    private val notificationEventsFlow =
        notificationEvents.asStateFlow()
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
                trayIcon.addMouseListener(
                    object : MouseAdapter() {
                        override fun mouseClicked(e: MouseEvent) {
                            if (e.clickCount >= 1) {
                                notificationEvents.value = NotificationEvent.Clicked(notification.id)
                            }
                        }
                    },
                )

                // Добавляем в системный трей
                systemTray.add(trayIcon)
                trayIcons[notification.id] = trayIcon

                // Показываем всплывающее уведомление
                trayIcon.displayMessage(
                    notification.title,
                    notification.message,
                    mapNotificationType(notification.type),
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
        actions: List<NotificationAction>,
    ) {
        if (systemTray == null) {
            showFallbackNotification(notification)
            return
        }

        SwingUtilities.invokeLater {
            try {
                val image = createNotificationImage(notification.icon)
                val trayIcon =
                    TrayIcon(image, notification.title).apply {
                        toolTip = notification.message
                        isImageAutoSize = true
                    }

                val popupMenu = PopupMenu()
                actions.forEach { action ->
                    val item = MenuItem(action.title)
                    item.addActionListener {
                        notificationEvents.value = NotificationEvent.Clicked(notification.id, action.id)
                    }
                    popupMenu.add(item)
                }
                trayIcon.popupMenu = popupMenu

                trayIcon.addMouseListener(
                    object : MouseAdapter() {
                        override fun mouseClicked(e: MouseEvent) {
                            if (e.clickCount >= 1) {
                                notificationEvents.value = NotificationEvent.Clicked(notification.id)
                            }
                        }
                    },
                )

                systemTray.add(trayIcon)
                trayIcons[notification.id] = trayIcon
                trayIcon.displayMessage(
                    notification.title,
                    notification.message,
                    mapNotificationType(notification.type),
                )

                Thread {
                    Thread.sleep(5000)
                    SwingUtilities.invokeLater {
                        systemTray.remove(trayIcon)
                        trayIcons.remove(notification.id)
                    }
                }.start()
            } catch (_: Exception) {
                showFallbackNotification(notification)
            }
        }
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
        importance: NotificationPriority,
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
        val iconPath = iconName?.takeIf { it.isNotBlank() } ?: "/icon.png"
        val image = javaClass.getResource(iconPath)?.let { Toolkit.getDefaultToolkit().createImage(it) }
        return image ?: createDefaultImage()
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