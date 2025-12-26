package com.company.ipcamera.shared.common

import io.ktor.client.engine.*
import io.ktor.client.engine.java.Java

actual class Platform actual constructor() {
    actual val platform: String = "Desktop"
    actual val version: String = System.getProperty("os.version", "Unknown")
    actual val architecture: String = System.getProperty("os.arch", "Unknown")
}

actual fun createHttpClientEngine(): HttpClientEngine {
    return Java.create()
}

