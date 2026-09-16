package com.company.ipcamera.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * iOS реализация UPnP Discovery
 * TODO: Реализовать через NSURLSession или CFNetwork для iOS
 */
actual class UPnPDiscovery {
    actual suspend fun discover(timeoutMillis: Long): List<UPnPDevice> = withContext(Dispatchers.Default) {
        logger.warn { "UPnP Discovery not yet implemented for iOS" }
        emptyList()
    }

    actual fun close() {
        logger.debug { "UPnP discovery closed (iOS)" }
    }
}
