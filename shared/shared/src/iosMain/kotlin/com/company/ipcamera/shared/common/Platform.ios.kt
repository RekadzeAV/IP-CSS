package com.company.ipcamera.shared.common

import platform.UIKit.UIDevice
import io.ktor.client.engine.*
import io.ktor.client.engine.darwin.Darwin

actual class Platform actual constructor() {
    actual val platform: String = "iOS"
    actual val version: String = UIDevice.currentDevice.systemVersion()
    actual val architecture: String = "arm64"
}

actual fun createHttpClientEngine(): HttpClientEngine {
    return Darwin.create()
}

