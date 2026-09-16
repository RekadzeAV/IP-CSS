package com.company.ipcamera.core.network.onvif

import com.company.ipcamera.core.network.OnvifClientFactory
import com.company.ipcamera.core.network.test.MockEngineFactory
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Unit тесты для XML парсинга ONVIF ответов.
 * Использует MockEngine для изоляции от сети.
 */
class OnvifXmlParserTest {

    /** Стандартный ответ GetCapabilities: нужен getDeviceInformation/getStreamUri/getProfiles. */
    private val standardCapabilitiesXml = """
        <SOAP-ENV:Envelope>
          <SOAP-ENV:Body>
            <tds:GetCapabilitiesResponse>
              <tds:Capabilities>
                <tt:Device>
                  <tt:XAddr>http://192.168.1.100/onvif/device_service</tt:XAddr>
                </tt:Device>
                <tt:Media>
                  <tt:XAddr>http://192.168.1.100/onvif/media_service</tt:XAddr>
                </tt:Media>
                <tt:PTZ>
                  <tt:XAddr>http://192.168.1.100/onvif/ptz_service</tt:XAddr>
                </tt:PTZ>
                <tt:Events>
                  <tt:XAddr>http://192.168.1.100/onvif/event_service</tt:XAddr>
                </tt:Events>
              </tds:Capabilities>
            </tds:GetCapabilitiesResponse>
          </SOAP-ENV:Body>
        </SOAP-ENV:Envelope>
    """.trimIndent()

    @Test
    fun `test parseCapabilities extracts all service URLs`() = runTest {
        val capabilitiesXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <SOAP-ENV:Envelope>
              <SOAP-ENV:Body>
                <tds:GetCapabilitiesResponse>
                  <tds:Capabilities>
                    <tt:Device>
                      <tt:XAddr>http://192.168.1.100/onvif/device_service</tt:XAddr>
                    </tt:Device>
                    <tt:Media>
                      <tt:XAddr>http://192.168.1.100/onvif/media_service</tt:XAddr>
                    </tt:Media>
                    <tt:PTZ>
                      <tt:XAddr>http://192.168.1.100/onvif/ptz_service</tt:XAddr>
                    </tt:PTZ>
                    <tt:Events>
                      <tt:XAddr>http://192.168.1.100/onvif/event_service</tt:XAddr>
                    </tt:Events>
                    <tt:Analytics>
                      <tt:XAddr>http://192.168.1.100/onvif/analytics_service</tt:XAddr>
                    </tt:Analytics>
                    <tt:Imaging>
                      <tt:XAddr>http://192.168.1.100/onvif/imaging_service</tt:XAddr>
                    </tt:Imaging>
                  </tds:Capabilities>
                </tds:GetCapabilitiesResponse>
              </SOAP-ENV:Body>
            </SOAP-ENV:Envelope>
        """.trimIndent()

        val engine = MockEngineFactory.createOnvif(
            soapResponses = mapOf(
                "GetCapabilities" to capabilitiesXml
            )
        )
        val client = OnvifClientFactory.create(engine = engine)

        val caps = client.getCapabilities("http://192.168.1.100", useCache = false)
        assertNotNull(caps)
        assertEquals("http://192.168.1.100/onvif/device_service", caps.deviceServiceUrl)
        assertEquals("http://192.168.1.100/onvif/media_service", caps.mediaServiceUrl)
        assertEquals("http://192.168.1.100/onvif/ptz_service", caps.ptzServiceUrl)
        assertEquals("http://192.168.1.100/onvif/event_service", caps.eventServiceUrl)
        assertEquals("http://192.168.1.100/onvif/analytics_service", caps.analyticsServiceUrl)
        assertEquals("http://192.168.1.100/onvif/imaging_service", caps.imagingServiceUrl)
    }

    @Test
    fun `test parseCapabilities handles missing optional services`() = runTest {
        val xml = """
            <SOAP-ENV:Envelope>
              <SOAP-ENV:Body>
                <tds:GetCapabilitiesResponse>
                  <tds:Capabilities>
                    <tt:Device>
                      <tt:XAddr>http://192.168.1.100/onvif/device_service</tt:XAddr>
                    </tt:Device>
                    <tt:Media>
                      <tt:XAddr>http://192.168.1.100/onvif/media_service</tt:XAddr>
                    </tt:Media>
                  </tds:Capabilities>
                </tds:GetCapabilitiesResponse>
              </SOAP-ENV:Body>
            </SOAP-ENV:Envelope>
        """.trimIndent()

        val engine = MockEngineFactory.createOnvif(
            soapResponses = mapOf("GetCapabilities" to xml)
        )
        val client = OnvifClientFactory.create(engine = engine)

        val caps = client.getCapabilities("http://192.168.1.100", useCache = false)
        assertNotNull(caps)
        assertNull(caps.ptzServiceUrl)
        assertNull(caps.eventServiceUrl)
    }

    @Test
    fun `test parseDeviceInformation extracts all fields`() = runTest {
        val xml = """
            <SOAP-ENV:Envelope>
              <SOAP-ENV:Body>
                <tds:GetDeviceInformationResponse>
                  <tds:Manufacturer>Hikvision</tds:Manufacturer>
                  <tds:Model>DS-2CD2T47G1-L</tds:Model>
                  <tds:FirmwareVersion>V5.7.0</tds:FirmwareVersion>
                  <tds:SerialNumber>123456789</tds:SerialNumber>
                  <tds:HardwareId>HW001</tds:HardwareId>
                </tds:GetDeviceInformationResponse>
              </SOAP-ENV:Body>
            </SOAP-ENV:Envelope>
        """.trimIndent()

        val engine = MockEngineFactory.createOnvif(
            soapResponses = mapOf(
                "GetCapabilities" to standardCapabilitiesXml,
                "GetDeviceInformation" to xml
            )
        )
        val client = OnvifClientFactory.create(engine = engine)

        val info = client.getDeviceInformation("http://192.168.1.100", useCache = false)
        assertNotNull(info)
        assertEquals("Hikvision", info.manufacturer)
        assertEquals("DS-2CD2T47G1-L", info.model)
        assertEquals("V5.7.0", info.firmwareVersion)
        assertEquals("123456789", info.serialNumber)
        assertEquals("HW001", info.hardwareId)
    }

    @Test
    fun `test parseDeviceInformation falls back to Unknown for missing fields`() = runTest {
        val xml = """
            <SOAP-ENV:Envelope>
              <SOAP-ENV:Body>
                <tds:GetDeviceInformationResponse>
                  <tds:Manufacturer>Dahua</tds:Manufacturer>
                </tds:GetDeviceInformationResponse>
              </SOAP-ENV:Body>
            </SOAP-ENV:Envelope>
        """.trimIndent()

        val engine = MockEngineFactory.createOnvif(
            soapResponses = mapOf(
                "GetCapabilities" to standardCapabilitiesXml,
                "GetDeviceInformation" to xml
            )
        )
        val client = OnvifClientFactory.create(engine = engine)

        val info = client.getDeviceInformation("http://192.168.1.100", useCache = false)
        assertNotNull(info)
        assertEquals("Dahua", info.manufacturer)
        assertEquals("Unknown", info.model)
        assertEquals("Unknown", info.firmwareVersion)
    }

    @Test
    fun `test parseProfiles extracts video and audio info`() = runTest {
        val xml = """
            <SOAP-ENV:Envelope>
              <SOAP-ENV:Body>
                <trt:GetProfilesResponse>
                  <trt:Profiles token="Profile1" fixed="true">
                    <tt:Name>MainStream</tt:Name>
                    <tt:VideoEncoderConfiguration>
                      <tt:Encoding>H.264</tt:Encoding>
                      <tt:Resolution>
                        <tt:Width>1920</tt:Width>
                        <tt:Height>1080</tt:Height>
                      </tt:Resolution>
                      <tt:RateControl>
                        <tt:FrameRateLimit>25</tt:FrameRateLimit>
                      </tt:RateControl>
                    </tt:VideoEncoderConfiguration>
                    <tt:AudioEncoderConfiguration>
                      <tt:Encoding>AAC</tt:Encoding>
                    </tt:AudioEncoderConfiguration>
                  </trt:Profiles>
                </trt:GetProfilesResponse>
              </SOAP-ENV:Body>
            </SOAP-ENV:Envelope>
        """.trimIndent()

        val engine = MockEngineFactory.createOnvif(
            soapResponses = mapOf(
                "GetCapabilities" to standardCapabilitiesXml,
                "GetProfiles" to xml
            )
        )
        val client = OnvifClientFactory.create(engine = engine)

        val profiles = client.getProfiles("http://192.168.1.100", useCache = false)
        assertEquals(1, profiles.size)
        val profile = profiles.first()
        assertEquals("Profile1", profile.token)
        assertEquals("MainStream", profile.name)
        assertEquals("H.264", profile.codec)
        assertEquals(1920, profile.videoResolution?.width)
        assertEquals(1080, profile.videoResolution?.height)
        assertEquals(25, profile.fps)
        assertTrue(profile.hasAudio)
        assertEquals("AAC", profile.audioCodec)
    }

    @Test
    fun `test parseProfiles returns default profile when no profiles found`() = runTest {
        val xml = """
            <SOAP-ENV:Envelope>
              <SOAP-ENV:Body>
                <trt:GetProfilesResponse>
                </trt:GetProfilesResponse>
              </SOAP-ENV:Body>
            </SOAP-ENV:Envelope>
        """.trimIndent()

        val engine = MockEngineFactory.createOnvif(
            soapResponses = mapOf(
                "GetCapabilities" to standardCapabilitiesXml,
                "GetProfiles" to xml
            )
        )
        val client = OnvifClientFactory.create(engine = engine)

        val profiles = client.getProfiles("http://192.168.1.100", useCache = false)
        assertEquals(1, profiles.size)
        assertEquals("Profile1", profiles.first().token)
    }

    @Test
    fun `test parseStreamUri extracts URI`() = runTest {
        val xml = """
            <SOAP-ENV:Envelope>
              <SOAP-ENV:Body>
                <trt:GetStreamUriResponse>
                  <trt:MediaUri>
                    <tt:Uri>rtsp://192.168.1.100:554/stream1</tt:Uri>
                  </trt:MediaUri>
                </trt:GetStreamUriResponse>
              </SOAP-ENV:Body>
            </SOAP-ENV:Envelope>
        """.trimIndent()

        val engine = MockEngineFactory.createOnvif(
            soapResponses = mapOf(
                "GetCapabilities" to standardCapabilitiesXml,
                "GetStreamUri" to xml
            )
        )
        val client = OnvifClientFactory.create(engine = engine)

        val uri = client.getStreamUri("http://192.168.1.100", "Profile1")
        assertEquals("rtsp://192.168.1.100:554/stream1", uri)
    }

    @Test
    fun `test parseStreamUri returns null for empty response`() = runTest {
        val xml = """
            <SOAP-ENV:Envelope>
              <SOAP-ENV:Body>
                <trt:GetStreamUriResponse>
                </trt:GetStreamUriResponse>
              </SOAP-ENV:Body>
            </SOAP-ENV:Envelope>
        """.trimIndent()

        val engine = MockEngineFactory.createOnvif(
            soapResponses = mapOf(
                "GetCapabilities" to standardCapabilitiesXml,
                "GetStreamUri" to xml
            )
        )
        val client = OnvifClientFactory.create(engine = engine)

        val uri = client.getStreamUri("http://192.168.1.100", "Profile1")
        assertNull(uri)
    }

    @Test
    fun `test parseCapabilities returns null on invalid XML`() = runTest {
        val xml = "<<<not xml at all>>>"

        val engine = MockEngineFactory.createOnvif(
            soapResponses = mapOf("GetCapabilities" to xml)
        )
        val client = OnvifClientFactory.create(engine = engine)

        val caps = client.getCapabilities("http://192.168.1.100", useCache = false)
        assertNull(caps)
    }

    @Test
    fun `test parseCapabilities handles Event fallback tag`() = runTest {
        val xml = """
            <SOAP-ENV:Envelope>
              <SOAP-ENV:Body>
                <tds:GetCapabilitiesResponse>
                  <tds:Capabilities>
                    <tt:Event>
                      <tt:XAddr>http://192.168.1.100/onvif/event_alt</tt:XAddr>
                    </tt:Event>
                  </tds:Capabilities>
                </tds:GetCapabilitiesResponse>
              </SOAP-ENV:Body>
            </SOAP-ENV:Envelope>
        """.trimIndent()

        val engine = MockEngineFactory.createOnvif(
            soapResponses = mapOf("GetCapabilities" to xml)
        )
        val client = OnvifClientFactory.create(engine = engine)

        val caps = client.getCapabilities("http://192.168.1.100", useCache = false)
        assertNotNull(caps)
        assertEquals("http://192.168.1.100/onvif/event_alt", caps.eventServiceUrl)
    }
}
