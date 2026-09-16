package com.company.ipcamera.core.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OnvifExceptionsTest {

    @Test
    fun testOnvifException() {
        val exception = OnvifException("Test error")
        assertEquals("Test error", exception.message)
    }

    @Test
    fun testOnvifExceptionWithCause() {
        val cause = Exception("Root cause")
        val exception = OnvifException("Test error", cause)
        assertEquals("Test error", exception.message)
        assertEquals(cause, exception.cause)
    }

    @Test
    fun testOnvifFaultException() {
        val exception = OnvifFaultException(
            faultCode = "s:Sender",
            faultSubcode = "ter:InvalidArgs",
            faultReason = "Invalid argument provided",
            faultDetail = "The profile token is invalid"
        )

        assertEquals("s:Sender", exception.faultCode)
        assertEquals("ter:InvalidArgs", exception.faultSubcode)
        assertEquals("Invalid argument provided", exception.faultReason)
        assertEquals("The profile token is invalid", exception.faultDetail)
        val message = exception.message.orEmpty()
        assertTrue(message.contains("ONVIF Fault"))
        assertTrue(message.contains("s:Sender"))
        assertTrue(message.contains("Invalid argument provided"))
    }

    @Test
    fun testOnvifFaultExceptionWithoutSubcode() {
        val exception = OnvifFaultException(
            faultCode = "s:Receiver",
            faultReason = "Internal server error"
        )

        assertEquals("s:Receiver", exception.faultCode)
        assertEquals(null, exception.faultSubcode)
        assertEquals("Internal server error", exception.faultReason)
    }

    @Test
    fun testOnvifAuthenticationException() {
        val exception = OnvifAuthenticationException(
            message = "Authentication failed",
            httpStatusCode = 401
        )

        assertEquals("Authentication failed", exception.message)
        assertEquals(401, exception.httpStatusCode)
    }

    @Test
    fun testOnvifNotSupportedException() {
        val exception = OnvifNotSupportedException("GetPresets")
        val message = exception.message.orEmpty()
        assertTrue(message.contains("GetPresets"))
        assertTrue(message.contains("not supported"))
    }

    @Test
    fun testOnvifTimeoutException() {
        val exception = OnvifTimeoutException(
            operation = "GetCapabilities",
            timeoutMillis = 5000
        )

        val message = exception.message.orEmpty()
        assertTrue(message.contains("GetCapabilities"))
        assertTrue(message.contains("5000"))
        assertTrue(message.contains("timed out"))
    }

    @Test
    fun testOnvifNetworkException() {
        val cause = Exception("Connection refused")
        val exception = OnvifNetworkException(
            message = "Connection refused",
            cause = cause
        )

        val message = exception.message.orEmpty()
        assertTrue(message.contains("Network error"))
        assertTrue(message.contains("Connection refused"))
        assertEquals(cause, exception.cause)
    }

    @Test
    fun testOnvifParseException() {
        val exception = OnvifParseException(
            message = "Invalid XML format",
            xmlContent = "<invalid>xml</invalid>"
        )

        val message = exception.message.orEmpty()
        assertTrue(message.contains("Parse error"))
        assertTrue(message.contains("Invalid XML format"))
        assertEquals("<invalid>xml</invalid>", exception.xmlContent)
    }

    @Test
    fun testOnvifDiscoveryException() {
        val exception = OnvifDiscoveryException("No devices found")
        val message = exception.message.orEmpty()
        assertTrue(message.contains("Discovery error"))
        assertTrue(message.contains("No devices found"))
    }
}

class OnvifFaultParserTest {

    @Test
    fun testIsSoapFault() {
        val soapFaultXml = """
            <soap:Envelope>
                <soap:Body>
                    <soap:Fault>
                        <faultcode>s:Sender</faultcode>
                        <faultstring>Invalid argument</faultstring>
                    </soap:Fault>
                </soap:Body>
            </soap:Envelope>
        """.trimIndent()

        assertTrue(OnvifFaultParser.isSoapFault(soapFaultXml))
    }

    @Test
    fun testIsSoapFaultWithSFault() {
        val soapFaultXml = """
            <s:Envelope>
                <s:Body>
                    <s:Fault>
                        <s:Code>
                            <s:Value>s:Sender</s:Value>
                        </s:Code>
                    </s:Fault>
                </s:Body>
            </s:Envelope>
        """.trimIndent()

        assertTrue(OnvifFaultParser.isSoapFault(soapFaultXml))
    }

    @Test
    fun testIsNotSoapFault() {
        val normalXml = """
            <s:Envelope>
                <s:Body>
                    <tds:GetCapabilitiesResponse>
                        <tds:Capabilities>
                            <tds:Device>
                                <tds:XAddr>http://192.168.1.100/onvif/device_service</tds:XAddr>
                            </tds:Device>
                        </tds:Capabilities>
                    </tds:GetCapabilitiesResponse>
                </s:Body>
            </s:Envelope>
        """.trimIndent()

        assertTrue(!OnvifFaultParser.isSoapFault(normalXml))
    }

    @Test
    fun testParseSoapFault() {
        val soapFaultXml = """
            <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope">
                <s:Body>
                    <s:Fault>
                        <s:Code>
                            <s:Value>s:Sender</s:Value>
                            <s:Subcode>
                                <s:Value>ter:InvalidArgs</s:Value>
                            </s:Subcode>
                        </s:Code>
                        <s:Reason>
                            <s:Text xml:lang="en">Invalid argument provided</s:Text>
                        </s:Reason>
                        <s:Detail>
                            <ter:InvalidArgs>The profile token is invalid</ter:InvalidArgs>
                        </s:Detail>
                    </s:Fault>
                </s:Body>
            </s:Envelope>
        """.trimIndent()

        val exception = OnvifFaultParser.parseSoapFault(soapFaultXml)

        assertEquals("s:Sender", exception.faultCode)
        assertEquals("ter:InvalidArgs", exception.faultSubcode)
        assertEquals("Invalid argument provided", exception.faultReason)
        assertNotNull(exception.faultDetail)
        assertTrue(exception.faultDetail!!.contains("profile token"))
    }

    @Test
    fun testParseSoapFaultWithoutSubcode() {
        val soapFaultXml = """
            <s:Envelope xmlns:s="http://www.w3.org/2003/05/soap-envelope">
                <s:Body>
                    <s:Fault>
                        <s:Code>
                            <s:Value>s:Receiver</s:Value>
                        </s:Code>
                        <s:Reason>
                            <s:Text xml:lang="en">Internal server error</s:Text>
                        </s:Reason>
                    </s:Fault>
                </s:Body>
            </s:Envelope>
        """.trimIndent()

        val exception = OnvifFaultParser.parseSoapFault(soapFaultXml)

        assertEquals("s:Receiver", exception.faultCode)
        assertEquals(null, exception.faultSubcode)
        assertEquals("Internal server error", exception.faultReason)
    }

    @Test
    fun testParseSoapFaultLegacyFormat() {
        val soapFaultXml = """
            <soap:Envelope>
                <soap:Body>
                    <soap:Fault>
                        <faultcode>s:Sender</faultcode>
                        <faultstring>Invalid argument</faultstring>
                        <detail>Some detail</detail>
                    </soap:Fault>
                </soap:Body>
            </soap:Envelope>
        """.trimIndent()

        val exception = OnvifFaultParser.parseSoapFault(soapFaultXml)

        assertEquals("s:Sender", exception.faultCode)
        assertEquals("Invalid argument", exception.faultReason)
    }

    @Test
    fun testParseSoapFaultWithInvalidXml() {
        val invalidXml = "Not XML at all"

        val exception = OnvifFaultParser.parseSoapFault(invalidXml)

        // Должен вернуть общее исключение с информацией об ошибке
        assertEquals("s:Receiver", exception.faultCode)
        assertTrue(exception.faultReason.contains("Failed to parse"))
    }

    @Test
    fun testExtractFaultCode() {
        val xml = """
            <s:Code>
                <s:Value>s:Sender</s:Value>
            </s:Code>
        """.trimIndent()

        // Используем рефлексию для тестирования приватного метода через публичный parseSoapFault
        val fullXml = """
            <s:Envelope>
                <s:Body>
                    <s:Fault>
                        <s:Code>
                            <s:Value>s:Sender</s:Value>
                        </s:Code>
                        <s:Reason>
                            <s:Text>Test</s:Text>
                        </s:Reason>
                    </s:Fault>
                </s:Body>
            </s:Envelope>
        """.trimIndent()

        val exception = OnvifFaultParser.parseSoapFault(fullXml)
        assertEquals("s:Sender", exception.faultCode)
    }
}
