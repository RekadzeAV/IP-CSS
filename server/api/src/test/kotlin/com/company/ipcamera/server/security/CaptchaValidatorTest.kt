package com.company.ipcamera.server.security

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CaptchaValidatorTest {

    @Test
    fun `generate and validate roundtrip`() {
        val token = CaptchaValidator.generateToken()
        assertTrue(CaptchaValidator.validateToken(token))
    }

    @Test
    fun `tokens are unique`() {
        assertNotEquals(CaptchaValidator.generateToken(), CaptchaValidator.generateToken())
    }

    @Test
    fun `invalid format rejected`() {
        assertFalse(CaptchaValidator.validateToken("not-valid-token"))
        assertFalse(CaptchaValidator.validateToken("only-one-part"))
        assertFalse(CaptchaValidator.validateToken("a:b")) // 2 части, нужны 3
    }

    @Test
    fun `garbage base64 rejected without throwing`() {
        assertFalse(CaptchaValidator.validateToken("!!! not base64 !!!"))
    }
}