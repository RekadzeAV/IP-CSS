package com.company.ipcamera.shared.platform

/**
 * iOS реализация NasPlatformDetector
 * iOS устройства не являются NAS платформами
 */
actual class NasPlatformDetectorImpl actual constructor() : NasPlatformDetector {
    override fun isRunningOnNas(): Boolean = false

    override fun detectPlatform(): NasPlatform? {
        // iOS устройства не являются NAS платформами
        return null
    }

    override fun getSystemPaths(): SystemPaths {
        // Возвращаем пути по умолчанию для iOS
        return SystemPaths.default()
    }
}
