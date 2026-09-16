package com.company.ipcamera.desktop

import java.awt.SystemTray
import java.awt.TrayIcon
import java.awt.Image
import java.awt.MenuItem
import java.awt.PopupMenu
import java.awt.Toolkit
import java.awt.event.ActionListener

/**
 * Системная интеграция: иконка в трее и меню.
 * На Linux может потребоваться зависимость libappindicator.
 */
object SystemIntegration {

    private var trayIcon: TrayIcon? = null

    /**
     * Показать иконку в системном трее с меню "Показать" и "Выход".
     * @param onShow вызвать при выборе "Показать" (показать окно)
     * @param onExit вызвать при выборе "Выход"
     * @return true если трей поддерживается и иконка добавлена
     */
    fun installTray(
        onShow: () -> Unit,
        onExit: () -> Unit
    ): Boolean {
        if (!SystemTray.isSupported()) return false
        val tray = SystemTray.getSystemTray() ?: return false

        val popup = PopupMenu()
        val showItem = MenuItem("Показать")
        showItem.addActionListener(ActionListener { onShow() })
        popup.add(showItem)
        popup.addSeparator()
        val exitItem = MenuItem("Выход")
        exitItem.addActionListener(ActionListener { onExit() })
        popup.add(exitItem)

        try {
            // Используем простую иконку 16x16 (можно заменить на ресурс)
            val image = createDefaultTrayImage()
            val icon = TrayIcon(image, "IP-CSS Desktop", popup).apply {
                isImageAutoSize = true
                addActionListener(ActionListener { onShow() })
            }
            tray.add(icon)
            trayIcon = icon
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun removeTray() {
        trayIcon?.let { icon ->
            SystemTray.getSystemTray().remove(icon)
            trayIcon = null
        }
    }

    private fun createDefaultTrayImage(): Image {
        val size = 16
        val img = java.awt.image.BufferedImage(size, size, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val g = img.createGraphics()
        try {
            g.color = java.awt.Color(0x20, 0x6A, 0xB5)
            g.fillRect(0, 0, size, size)
            g.color = java.awt.Color.WHITE
            g.fillRect(4, 4, 4, 4)
            g.fillRect(8, 8, 4, 4)
        } finally {
            g.dispose()
        }
        return img
    }
}
