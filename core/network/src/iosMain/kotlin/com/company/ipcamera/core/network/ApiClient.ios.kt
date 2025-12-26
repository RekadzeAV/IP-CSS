package com.company.ipcamera.core.network

import io.ktor.client.engine.darwin.Darwin

actual fun ApiClient.Companion.createDefaultEngine(): io.ktor.client.engine.HttpClientEngine {
    return Darwin.create()
}
