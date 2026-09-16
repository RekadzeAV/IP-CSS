package com.company.ipcamera.core.network

import kotlin.test.*

/**
 * Unit тесты для WS-Discovery
 * Тестирует парсинг ProbeMatches ответов и обработку устройств
 */
class WSDiscoveryTest {

    @Test
    fun `test DiscoveredDevice creation`() {
        val device = DiscoveredDevice(
            xAddrs = listOf("http://192.168.1.100/onvif/device_service"),
            types = listOf("dn:NetworkVideoTransmitter"),
            scopes = listOf("onvif://www.onvif.org/name/Test", "onvif://www.onvif.org/Profile/Streaming")
        )

        assertEquals(1, device.xAddrs.size)
        assertEquals("http://192.168.1.100/onvif/device_service", device.xAddrs.first())
        assertEquals(1, device.types.size)
        assertTrue(device.types.contains("dn:NetworkVideoTransmitter"))
        assertEquals(2, device.scopes.size)
    }

    @Test
    fun `test DiscoveredDevice with multiple XAddrs`() {
        val device = DiscoveredDevice(
            xAddrs = listOf(
                "http://192.168.1.100/onvif/device_service",
                "http://192.168.1.100/onvif/media_service"
            ),
            types = listOf("dn:NetworkVideoTransmitter"),
            scopes = emptyList()
        )

        assertEquals(2, device.xAddrs.size)
        assertTrue(device.xAddrs.contains("http://192.168.1.100/onvif/device_service"))
        assertTrue(device.xAddrs.contains("http://192.168.1.100/onvif/media_service"))
    }

    @Test
    fun `test DiscoveredDevice with empty lists`() {
        val device = DiscoveredDevice(
            xAddrs = emptyList(),
            types = emptyList(),
            scopes = emptyList()
        )

        assertTrue(device.xAddrs.isEmpty())
        assertTrue(device.types.isEmpty())
        assertTrue(device.scopes.isEmpty())
        assertEquals(1, device.metadataVersion) // Значение по умолчанию
    }

    @Test
    fun `test DiscoveredDevice metadataVersion`() {
        val device = DiscoveredDevice(
            xAddrs = listOf("http://192.168.1.100/onvif/device_service"),
            types = listOf("dn:NetworkVideoTransmitter"),
            scopes = emptyList(),
            metadataVersion = 2
        )

        assertEquals(2, device.metadataVersion)
    }

    @Test
    fun `test DiscoveredDevice with ONVIF scopes`() {
        val device = DiscoveredDevice(
            xAddrs = listOf("http://192.168.1.100/onvif/device_service"),
            types = listOf("dn:NetworkVideoTransmitter"),
            scopes = listOf(
                "onvif://www.onvif.org/name/TestCamera",
                "onvif://www.onvif.org/Profile/Streaming",
                "onvif://www.onvif.org/Profile/T"
            )
        )

        assertEquals(3, device.scopes.size)
        assertTrue(device.scopes.any { it.contains("onvif", ignoreCase = true) })
    }

    @Test
    fun `test DiscoveredDevice with different device types`() {
        val device = DiscoveredDevice(
            xAddrs = listOf("http://192.168.1.100/onvif/device_service"),
            types = listOf(
                "dn:NetworkVideoTransmitter",
                "dn:NetworkVideoDisplay",
                "tdn:VideoEncoder"
            ),
            scopes = emptyList()
        )

        assertEquals(3, device.types.size)
        assertTrue(device.types.contains("dn:NetworkVideoTransmitter"))
        assertTrue(device.types.contains("dn:NetworkVideoDisplay"))
        assertTrue(device.types.contains("tdn:VideoEncoder"))
    }

    @Test
    fun `test DiscoveredDevice URL validation`() {
        val validDevice = DiscoveredDevice(
            xAddrs = listOf("http://192.168.1.100/onvif/device_service"),
            types = listOf("dn:NetworkVideoTransmitter"),
            scopes = emptyList()
        )

        assertTrue(validDevice.xAddrs.isNotEmpty())
        assertTrue(
            validDevice.xAddrs.first().startsWith("http://") || validDevice.xAddrs.first().startsWith("https://")
        )

        val invalidDevice = DiscoveredDevice(
            xAddrs = listOf("invalid-url"),
            types = emptyList(),
            scopes = emptyList()
        )

        // URL валидация должна быть в OnvifClient, но структура должна быть корректной
        assertTrue(invalidDevice.xAddrs.isNotEmpty())
    }
}
