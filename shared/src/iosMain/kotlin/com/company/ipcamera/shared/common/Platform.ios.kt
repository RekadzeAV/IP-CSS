package com.company.ipcamera.shared.common

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin
import platform.UIKit.UIDevice

actual class Platform actual constructor() {
    actual val platform: String = "iOS"
    actual val version: String = UIDevice.currentDevice.systemVersion()
    actual val architecture: String = "arm64"
}

actual fun createHttpClientEngine(): HttpClientEngine {
    return Darwin.create()
}
