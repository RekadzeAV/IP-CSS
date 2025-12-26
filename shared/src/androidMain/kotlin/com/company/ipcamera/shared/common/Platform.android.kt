package com.company.ipcamera.shared.common

import android.os.Build

actual class Platform actual constructor() {
    actual val platform: String = "Android"
    actual val version: String = Build.VERSION.RELEASE
    actual val architecture: String = Build.SUPPORTED_ABIS[0]
}

