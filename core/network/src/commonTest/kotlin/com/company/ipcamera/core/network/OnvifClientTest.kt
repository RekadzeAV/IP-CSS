package com.company.ipcamera.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OnvifClientTest {

    @Test
    fun testPtzDirectionPan() {
        assertEquals(-0.5f, PtzDirection.LEFT.getPan(0.5f))
        assertEquals(0.5f, PtzDirection.RIGHT.getPan(0.5f))
        assertEquals(0f, PtzDirection.UP.getPan(0.5f))
        assertEquals(-0.5f, PtzDirection.UP_LEFT.getPan(0.5f))
    }

    @Test
    fun testPtzDirectionTilt() {
        assertEquals(0.5f, PtzDirection.UP.getTilt(0.5f))
        assertEquals(-0.5f, PtzDirection.DOWN.getTilt(0.5f))
        assertEquals(0f, PtzDirection.LEFT.getTilt(0.5f))
        assertEquals(0.5f, PtzDirection.UP_RIGHT.getTilt(0.5f))
    }

    @Test
    fun testPtzDirectionZoom() {
        assertEquals(0.5f, PtzDirection.ZOOM_IN.getZoom(0.5f))
        assertEquals(-0.5f, PtzDirection.ZOOM_OUT.getZoom(0.5f))
        assertEquals(0f, PtzDirection.UP.getZoom(0.5f))
    }

    @Test
    fun testOnvifProfileCreation() {
        val profile = OnvifProfile(
            token = "Profile1",
            name = "Main Profile",
            videoResolution = com.company.ipcamera.core.common.model.Resolution(1920, 1080),
            fps = 25,
            codec = "H.264",
            hasAudio = true,
            audioCodec = "AAC"
        )

        assertEquals("Profile1", profile.token)
        assertEquals("Main Profile", profile.name)
        assertEquals(1920, profile.videoResolution?.width)
        assertEquals(1080, profile.videoResolution?.height)
        assertEquals(25, profile.fps)
        assertEquals("H.264", profile.codec)
        assertTrue(profile.hasAudio)
        assertEquals("AAC", profile.audioCodec)
    }

    @Test
    fun testOnvifCapabilities() {
        val capabilities = OnvifCapabilities(
            deviceServiceUrl = "http://192.168.1.100/onvif/device_service",
            mediaServiceUrl = "http://192.168.1.100/onvif/media_service",
            ptzServiceUrl = "http://192.168.1.100/onvif/ptz_service"
        )

        assertNotNull(capabilities.deviceServiceUrl)
        assertNotNull(capabilities.mediaServiceUrl)
        assertNotNull(capabilities.ptzServiceUrl)
    }

    @Test
    fun testDeviceInformation() {
        val deviceInfo = DeviceInformation(
            manufacturer = "Hikvision",
            model = "DS-2CD2T47G1-L",
            firmwareVersion = "V5.7.0",
            serialNumber = "123456789",
            hardwareId = "HW001"
        )

        assertEquals("Hikvision", deviceInfo.manufacturer)
        assertEquals("DS-2CD2T47G1-L", deviceInfo.model)
        assertEquals("V5.7.0", deviceInfo.firmwareVersion)
    }
}

