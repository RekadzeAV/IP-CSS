package com.company.ipcamera.core.network.security

import io.ktor.client.engine.HttpClientEngine

/**
 * JVM stub реализация CertificatePinner
 */
actual class CertificatePinner actual constructor(private val config: CertificatePinningConfig) {
    /**
     * Применяет certificate pinning к HTTP engine
     */
    actual fun applyToEngine(engine: HttpClientEngine): HttpClientEngine {
        // JVM: certificate pinning не поддерживается в stub
        return engine
    }

    /**
     * Проверяет, поддерживается ли certificate pinning на текущей платформе
     */
    actual fun isSupported(): Boolean = false
}
