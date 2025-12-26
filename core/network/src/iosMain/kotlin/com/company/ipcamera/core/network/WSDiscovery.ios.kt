package com.company.ipcamera.core.network

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * iOS реализация WS-Discovery
 * TODO: Реализовать через CocoaAsyncSocket или Network framework
 * Пока возвращает пустой список
 */
actual class WSDiscovery {
    actual suspend fun discover(timeoutMillis: Long): List<DiscoveredDevice> {
        logger.warn { "WS-Discovery on iOS is not yet implemented" }
        return emptyList()
    }

    actual fun close() {
        // No-op for now
    }
}

