package com.company.ipcamera.shared.platform

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Определение NAS платформы через API
 *
 * Вариант 2: Проверка через API эндпоинты производителей
 * Используется как дополнительный метод, если определение через файлы не дало результата
 */
class ApiBasedNasDetector(
    private val fileBasedDetector: FileBasedNasDetector = FileBasedNasDetector(),
) : NasPlatformDetector {
    override fun detectPlatform(): NasPlatform? {
        // Сначала пробуем через файлы (быстрее и надежнее)
        val fileBasedResult = fileBasedDetector.detectPlatform()
        if (fileBasedResult != null) {
            return fileBasedResult
        }

        // CommonMain fallback: API probing is JVM-specific.
        return null
    }

    override fun getSystemPaths(): SystemPaths {
        val platform = detectPlatform() ?: return fileBasedDetector.getSystemPaths()

        return fileBasedDetector.getSystemPaths()
    }

    override fun isRunningOnNas(): Boolean {
        return detectPlatform() != null
    }

    override suspend fun getHardwareAcceleration(): com.company.ipcamera.shared.platform.hwaccel.HardwareAccelerationType {
        return com.company.ipcamera.shared.platform.hwaccel.HardwareAccelerationType.None
    }

    init {
        logger.debug { "ApiBasedNasDetector runs in common fallback mode" }
    }
}
