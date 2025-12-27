package com.company.ipcamera.core.network

import com.company.ipcamera.core.network.security.CertificatePinner
import io.ktor.client.engine.android.Android

/**
 * Создает Android engine по умолчанию
 */
actual fun ApiClient.Companion.createDefaultEngine(): io.ktor.client.engine.HttpClientEngine {
    return Android.create()
}

/**
 * Создает Android engine с certificate pinning
 */
actual fun ApiClient.Companion.createEngineWithPinning(config: com.company.ipcamera.core.network.security.CertificatePinningConfig): io.ktor.client.engine.HttpClientEngine {
    val pinner = CertificatePinner(config)
    return pinner.createEngineWithPinning()
}
