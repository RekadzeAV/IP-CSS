package com.company.ipcamera.core.network

import io.ktor.client.engine.android.Android

actual fun ApiClient.Companion.createDefaultEngine(): io.ktor.client.engine.HttpClientEngine {
    return Android.create()
}
