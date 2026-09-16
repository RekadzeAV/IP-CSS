package com.company.ipcamera.android.security

import android.view.WindowManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Утилита для защиты экранов от скриншотов и записи экрана
 *
 * Использование:
 * ```
 * @Composable
 * fun SensitiveScreen() {
 *     PreventScreenshots()
 *     // Ваш контент
 * }
 * ```
 */
@Composable
fun PreventScreenshots() {
    val view = LocalView.current

    DisposableEffect(Unit) {
        val window = (view.context as? android.app.Activity)?.window
        if (window == null) {
            onDispose {}
        } else {
            // Устанавливаем FLAG_SECURE для предотвращения скриншотов
            window.setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
            )

            // Также блокируем запись экрана
            WindowCompat.setDecorFitsSystemWindows(window, false)

            onDispose {
                // Снимаем флаг при выходе с экрана
                window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            }
        }
    }
}

/**
 * Утилита для защиты конкретного окна от скриншотов
 *
 * Используется в Activity для защиты всего окна
 */
object ScreenSecurityHelper {
    /**
     * Включает защиту от скриншотов для окна
     */
    fun enableScreenshotProtection(window: android.view.Window) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
    }

    /**
     * Отключает защиту от скриншотов для окна
     */
    fun disableScreenshotProtection(window: android.view.Window) {
        window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    /**
     * Проверяет, включена ли защита от скриншотов
     */
    fun isScreenshotProtectionEnabled(window: android.view.Window): Boolean {
        return (window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
    }
}
