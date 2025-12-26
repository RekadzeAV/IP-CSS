package com.company.ipcamera.shared.common

import io.ktor.client.engine.*

expect class Platform() {
    val platform: String
    val version: String
    val architecture: String
}

/**
 * Получить HttpClientEngine для текущей платформы
 */
expect fun createHttpClientEngine(): HttpClientEngine

