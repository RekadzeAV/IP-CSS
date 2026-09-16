package com.company.ipcamera.core.network

import com.company.ipcamera.core.network.security.CertificatePinningConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.java.Java

/**
 * JVM реализация createDefaultEngine
 */
actual fun ApiClient.Companion.createDefaultEngine(): HttpClientEngine {
    return Java.create()
}

/**
 * JVM stub реализация createEngineWithPinning
 */
actual fun ApiClient.Companion.createEngineWithPinning(config: CertificatePinningConfig): HttpClientEngine {
    // JVM: certificate pinning не поддерживается в stub
    return Java.create()
}
