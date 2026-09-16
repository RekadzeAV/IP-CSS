package com.company.ipcamera.shared.platform

/**
 * Интерфейс для определения NAS платформы
 *
 * Поддерживает два варианта определения:
 * 1. Через системные файлы (основной метод)
 * 2. Через NAS API (если доступно)
 */
interface NasPlatformDetector {
    /**
     * Определить платформу
     *
     * Использует комбинацию методов:
     * 1. Проверка системных файлов
     * 2. Проверка через API (если доступно)
     *
     * @return Определенная платформа или null, если не удалось определить
     */
    fun detectPlatform(): NasPlatform?

    /**
     * Получить системные пути для текущей платформы
     *
     * @return SystemPaths с путями для данных, записей, логов и конфигурации
     */
    fun getSystemPaths(): SystemPaths

    /**
     * Проверить, запущено ли приложение на NAS платформе
     *
     * @return true, если запущено на NAS, false иначе
     */
    fun isRunningOnNas(): Boolean

    /**
     * Получить информацию об аппаратном ускорении
     *
     * @return Тип аппаратного ускорения или null если не определено
     */
    suspend fun getHardwareAcceleration(): com.company.ipcamera.shared.platform.hwaccel.HardwareAccelerationType
}

/**
 * Платформенная реализация NasPlatformDetector (expect/actual).
 */
expect class NasPlatformDetectorImpl() : NasPlatformDetector {
    override fun detectPlatform(): NasPlatform?

    override fun getSystemPaths(): SystemPaths

    override fun isRunningOnNas(): Boolean

    override suspend fun getHardwareAcceleration(): com.company.ipcamera.shared.platform.hwaccel.HardwareAccelerationType
}
