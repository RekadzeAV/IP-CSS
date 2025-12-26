package com.company.ipcamera.shared.common

actual class Platform actual constructor() {
    actual val platform: String = "Desktop"
    actual val version: String = System.getProperty("os.version", "Unknown")
    actual val architecture: String = System.getProperty("os.arch", "Unknown")
}

