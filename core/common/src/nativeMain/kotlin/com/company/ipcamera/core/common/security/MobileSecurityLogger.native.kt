package com.company.ipcamera.core.common.security

actual class SecureMobileSecurityLogger actual constructor() : MobileSecurityLogger {
    actual override fun log(event: MobileSecurityEvent) {
        // Native metadata targets do not require logging backend wiring.
        // Keep this implementation lightweight and side-effect free.
    }
}
