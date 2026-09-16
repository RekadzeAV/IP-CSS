package com.company.ipcamera.shared.platform

/**
 * Android реализация NasPlatformDetector
 * Android устройства не являются NAS платформами
 */
actual class NasPlatformDetectorImpl actual constructor() : NasPlatformDetector {
    actual override fun isRunningOnNas(): Boolean = false

    actual override fun detectPlatform(): NasPlatform? {
        // Android устройства не являются NAS платформами
        return null
    }

    actual override fun getSystemPaths(): SystemPaths {
        // Возвращаем пути по умолчанию для Android
        return SystemPaths.default()
    }

    actual override suspend fun getHardwareAcceleration(): com.company.ipcamera.shared.platform.hwaccel.HardwareAccelerationType {
        // Android устройства могут иметь HW ускорение, но детекция платформо-специфична
        return com.company.ipcamera.shared.platform.hwaccel.HardwareAccelerationType.None
    }
}
