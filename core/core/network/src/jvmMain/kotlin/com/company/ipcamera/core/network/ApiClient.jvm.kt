package com.company.ipcamera.core.network

import com.company.ipcamera.core.network.security.CertificatePinner
import io.ktor.client.engine.java.Java

/**
 * Создает JVM/Desktop engine по умолчанию
 */
actual fun ApiClient.Companion.createDefaultEngine(): io.ktor.client.engine.HttpClientEngine {
    return Java.create()
}

/**
 * Создает JVM/Desktop engine с certificate pinning
 */
actual fun ApiClient.Companion.createEngineWithPinning(config: com.company.ipcamera.core.network.security.CertificatePinningConfig): io.ktor.client.engine.HttpClientEngine {
    val pinner = CertificatePinner(config)
    return pinner.createEngineWithPinning()
}

