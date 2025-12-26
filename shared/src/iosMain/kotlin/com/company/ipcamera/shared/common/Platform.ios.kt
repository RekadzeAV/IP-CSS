package com.company.ipcamera.shared.common

import platform.UIKit.UIDevice

actual class Platform actual constructor() {
    actual val platform: String = "iOS"
    actual val version: String = UIDevice.currentDevice.systemVersion()
    actual val architecture: String = "arm64"
}

