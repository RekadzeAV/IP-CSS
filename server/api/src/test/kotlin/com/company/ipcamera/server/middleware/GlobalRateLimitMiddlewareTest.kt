package com.company.ipcamera.server.middleware

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GlobalRateLimitMiddlewareTest {

    @Test
    fun skipsHealthAndHlsAndWs() {
        assertTrue(shouldSkipGlobalRateLimit("/api/v1/health"))
        assertTrue(shouldSkipGlobalRateLimit("/api/v1/health/live"))
        assertTrue(shouldSkipGlobalRateLimit("/api/v1/database/health"))
        assertTrue(shouldSkipGlobalRateLimit("/api/v1/auth/refresh"))
        assertTrue(shouldSkipGlobalRateLimit("/api/v1/auth/ws-token"))
        assertTrue(shouldSkipGlobalRateLimit("/api/v1/cameras/x/streams/y/hls/playlist.m3u8"))
        assertTrue(shouldSkipGlobalRateLimit("/api/v1/ws"))
        assertTrue(shouldSkipGlobalRateLimit("/api/v1/screenshots/a.jpg"))
        assertTrue(shouldSkipGlobalRateLimit("/api/v1/onvif/events/in"))
        assertTrue(shouldSkipGlobalRateLimit("/api/v1/cluster/me"))
    }

    @Test
    fun doesNotSkipOrdinaryApi() {
        assertFalse(shouldSkipGlobalRateLimit("/api/v1/cameras"))
        assertFalse(shouldSkipGlobalRateLimit("/api/v1/auth/login"))
    }
}
