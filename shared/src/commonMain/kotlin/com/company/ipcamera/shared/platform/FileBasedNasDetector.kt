package com.company.ipcamera.shared.platform

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Определение NAS платформы через системные файлы
 *
 * Вариант 1: Проверка характерных системных файлов и директорий
 */
class FileBasedNasDetector : NasPlatformDetector {
    override fun detectPlatform(): NasPlatform? {
        logger.debug { "FileBasedNasDetector common fallback: platform unknown" }
        return null
    }

    override fun getSystemPaths(): SystemPaths {
        return getDefaultPaths()
    }

    override fun isRunningOnNas(): Boolean {
        return detectPlatform() != null
    }

    override suspend fun getHardwareAcceleration(): com.company.ipcamera.shared.platform.hwaccel.HardwareAccelerationType {
        return com.company.ipcamera.shared.platform.hwaccel.HardwareAccelerationType.None
    }

    private fun getDefaultPaths(): SystemPaths {
        val userHome = "."
        return SystemPaths(
            dataPath = "$userHome/.ip-css/data",
            recordingsPath = "$userHome/.ip-css/recordings",
            logsPath = "$userHome/.ip-css/logs",
            configPath = "$userHome/.ip-css/config",
        )
    }
}
