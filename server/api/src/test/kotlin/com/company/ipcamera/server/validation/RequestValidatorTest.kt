package com.company.ipcamera.server.validation

import com.company.ipcamera.server.dto.*
import com.company.ipcamera.server.routing.MarkNotificationsAsReadRequest
import kotlin.test.*

/**
 * Тесты для RequestValidator
 */
class RequestValidatorTest {

    @Test
    fun `validateLoginRequest valid`() {
        val result = RequestValidator.validateLoginRequest(LoginRequest("testuser", "password123"))
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateLoginRequest blank username`() {
        val result = RequestValidator.validateLoginRequest(LoginRequest("", "password123"))
        assertTrue(result is ValidationResult.Error)
        assertEquals("username", (result as ValidationResult.Error).field)
    }

    @Test
    fun `validateCreateUserRequest invalid role`() {
        val request = CreateUserRequest(
            username = "newuser",
            email = "user@example.com",
            password = "password123",
            role = "INVALID_ROLE"
        )
        val result = RequestValidator.validateCreateUserRequest(request)
        assertTrue(result is ValidationResult.Error)
        assertEquals("role", (result as ValidationResult.Error).field)
    }

    @Test
    fun `validateCreateCameraRequest valid https url`() {
        val request = CreateCameraRequest(
            name = "Cam",
            url = "https://example.com/cam",
            username = "admin",
            password = "secret"
        )
        val result = RequestValidator.validateCreateCameraRequest(request)
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateCreateCameraRequest valid rtsp url`() {
        val request = CreateCameraRequest(
            name = "Cam RTSP",
            url = "rtsp://example.com:554/stream",
            username = "admin",
            password = "secret"
        )
        val result = RequestValidator.validateCreateCameraRequest(request)
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateCreateCameraRequest invalid url`() {
        val request = CreateCameraRequest(name = "Cam", url = "not-a-url")
        val result = RequestValidator.validateCreateCameraRequest(request)
        assertTrue(result is ValidationResult.Error)
        assertEquals("url", (result as ValidationResult.Error).field)
    }

    @Test
    fun `validateUpdateCameraRequest invalid url`() {
        val request = UpdateCameraRequest(url = "invalid-url")
        val result = RequestValidator.validateUpdateCameraRequest(request)
        assertTrue(result is ValidationResult.Error)
        assertEquals("url", (result as ValidationResult.Error).field)
    }

    @Test
    fun `validateSetStreamQualityRequest valid`() {
        val result = RequestValidator.validateSetStreamQualityRequest(SetStreamQualityRequest("high"))
        assertTrue(result is ValidationResult.Success)
        val r2 = RequestValidator.validateSetStreamQualityRequest(SetStreamQualityRequest("qhd1440"))
        assertTrue(r2 is ValidationResult.Success)
        val r3 = RequestValidator.validateSetStreamQualityRequest(SetStreamQualityRequest("uhd4k"))
        assertTrue(r3 is ValidationResult.Success)
        val r4 = RequestValidator.validateSetStreamQualityRequest(SetStreamQualityRequest("qhd1440_h264"))
        assertTrue(r4 is ValidationResult.Success)
        val r5 = RequestValidator.validateSetStreamQualityRequest(SetStreamQualityRequest("uhd4k_h264"))
        assertTrue(r5 is ValidationResult.Success)
    }

    @Test
    fun `validateSetStreamQualityRequest invalid`() {
        val result = RequestValidator.validateSetStreamQualityRequest(SetStreamQualityRequest("super"))
        assertTrue(result is ValidationResult.Error)
        assertEquals("quality", (result as ValidationResult.Error).field)
    }

    @Test
    fun `validateWebRtcOfferRequest blank sdp`() {
        val request = WebRtcOfferRequest(WebRtcOfferDto(type = "offer", sdp = ""))
        val result = RequestValidator.validateWebRtcOfferRequest(request)
        assertTrue(result is ValidationResult.Error)
        assertEquals("offer.sdp", (result as ValidationResult.Error).field)
    }

    @Test
    fun `validateMarkNotificationsAsReadRequest empty ids`() {
        val result = RequestValidator.validateMarkNotificationsAsReadRequest(MarkNotificationsAsReadRequest(emptyList()))
        assertTrue(result is ValidationResult.Error)
        assertEquals("ids", (result as ValidationResult.Error).field)
    }

    @Test
    fun `validateCameraId valid`() {
        val result = RequestValidator.validateCameraId("camera-123")
        assertTrue(result is ValidationResult.Success)
    }

    @Test
    fun `validateCameraId blank`() {
        val result = RequestValidator.validateCameraId("")
        assertTrue(result is ValidationResult.Error)
        assertEquals("id", (result as ValidationResult.Error).field)
    }
}
