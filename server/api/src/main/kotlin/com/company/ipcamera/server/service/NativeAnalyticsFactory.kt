package com.company.ipcamera.server.service

import com.company.ipcamera.core.network.analytics.NativeAnalytics

/**
 * Фабрика серверной нативной аналитики.
 *
 * Единая точка создания [NativeAnalytics] (JNI-мост к C++ OpenCV-детекторам из
 * native/analytics). Инкапсулирует graceful degradation: если нативная библиотека
 * недоступна (не собрана / нет рядом с процессом), детекторы сообщают о недоступности,
 * а сервисы-потребители продолжают работать в degraded-режиме (запуск возвращает ошибку
 * с понятным сообщением, без падения сервера).
 */
object NativeAnalyticsFactory {

    /** Ленивый синглтон — загрузка .dll/.so выполняется один раз на процесс. */
    private val native: NativeAnalytics by lazy { NativeAnalytics() }

    fun get(): NativeAnalytics = native

    /** Проверка работоспособности JNI-моста (загружена ли нативная библиотека). */
    fun isAvailable(): Boolean = try {
        get().getVersion() != "unavailable"
    } catch (_: Throwable) {
        false
    }

    /** Версия нативной библиотеки (для статусов/диагностики). */
    fun version(): String = try {
        get().getVersion()
    } catch (_: Throwable) {
        "unavailable"
    }
}
