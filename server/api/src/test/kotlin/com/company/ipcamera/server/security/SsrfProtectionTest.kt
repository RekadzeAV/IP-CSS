package com.company.ipcamera.server.security

import com.company.ipcamera.server.validation.ValidationResult
import kotlin.test.Test
import kotlin.test.assertTrue

class SsrfProtectionTest {

    @Test
    fun `validateUrlForSsrf accepts valid rtsp url`() {
        val result = SsrfProtection.validateUrlForSsrf("rtsp://example.com:554/stream")
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateUrlForSsrf rejects malformed rtsp url`() {
        val result = SsrfProtection.validateUrlForSsrf("rtsp:/broken-url")
        assertTrue(result is ValidationResult.Error)
    }
}

