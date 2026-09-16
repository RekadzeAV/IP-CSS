package com.company.ipcamera.core.network.onvif

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OnvifEventParserTest {

    @Test
    fun testParseTopicExpression() {
        val xml = """<wsnt:Topic Dialect="http://www.onvif.org/ver10/tev/topicExpression/ConcreteSet">
            tns1:VideoSource/MotionAlarm
        </wsnt:Topic>"""

        val topic = OnvifEventParser.parseTopicExpression(xml)
        assertNotNull(topic)
        assertTrue(topic!!.contains("MotionAlarm"))
    }

    @Test
    fun testParseMessageContent() {
        val xml = """
            <tns1:VideoSource>
                <tns1:VideoSourceToken>video-source-001</tns1:VideoSourceToken>
                <tns1:State>true</tns1:State>
            </tns1:VideoSource>
        """

        val content = OnvifEventParser.parseMessageContent(xml)
        assertTrue(content.isNotEmpty())
        assertTrue(content.containsKey("VideoSourceToken") || content.containsKey("State"))
    }

    @Test
    fun testCreateSubscribeRequest() {
        val request = OnvifEventParser.createSubscribeRequest(
            eventServiceUrl = "http://192.168.1.100/onvif/event_service",
            notificationConsumerUrl = "http://localhost:8080/events",
            subscriptionTime = 3600
        )

        assertTrue(request.contains("Subscribe"))
        assertTrue(request.contains("http://localhost:8080/events"))
        assertTrue(request.contains("PT3600S"))
    }

    @Test
    fun testCreateSubscribeRequestWithFilter() {
        val filter = OnvifEventFilter(
            topicExpression = "tns1:VideoSource/MotionAlarm"
        )

        val request = OnvifEventParser.createSubscribeRequest(
            eventServiceUrl = "http://192.168.1.100/onvif/event_service",
            notificationConsumerUrl = "http://localhost:8080/events",
            subscriptionTime = 3600,
            filter = filter
        )

        assertTrue(request.contains("Filter"))
        assertTrue(request.contains("MotionAlarm"))
    }

    @Test
    fun testParseTerminationTime() {
        val responseXml = """
            <wsnt:SubscribeResponse>
                <wsnt:SubscriptionReference>
                    <wsa:Address xmlns:wsa="http://www.w3.org/2005/08/addressing">http://camera/subscription/1</wsa:Address>
                </wsnt:SubscriptionReference>
                <wsnt:TerminationTime>PT3600S</wsnt:TerminationTime>
            </wsnt:SubscribeResponse>
        """.trimIndent()
        val result = OnvifEventParser.parseSubscribeResponse(responseXml)
        assertTrue(result.isSuccess)
        val terminationTime = result.getOrNull()?.expirationTime
        assertNotNull(terminationTime)
        assertTrue(terminationTime > System.currentTimeMillis())
    }

    @Test
    fun testParsePullMessagesResponse() {
        val xml = """
            <tev:PullMessagesResponse>
                <wsnt:NotificationMessage>
                    <wsnt:Topic>tns1:VideoSource/MotionAlarm</wsnt:Topic>
                    <wsnt:Message>Motion detected</wsnt:Message>
                </wsnt:NotificationMessage>
            </tev:PullMessagesResponse>
        """

        val result = OnvifEventParser.parsePullMessagesResponse(xml)
        assertTrue(result.isSuccess)
        val events = result.getOrNull()
        assertNotNull(events)
        assertTrue(events!!.isNotEmpty())
    }

    @Test
    fun `parseNotificationMessage handles malformed timestamp with fallback`() {
        val xml = """
            <wsnt:NotificationMessage>
                <wsnt:Topic>tns1:VideoSource/MotionAlarm</wsnt:Topic>
                <wsnt:Message>Motion detected</wsnt:Message>
                <wsnt:UtcTime>not-a-timestamp</wsnt:UtcTime>
            </wsnt:NotificationMessage>
        """.trimIndent()

        val before = System.currentTimeMillis()
        val result = OnvifEventParser.parseNotificationMessage(xml)
        val after = System.currentTimeMillis()

        assertTrue(result.isSuccess)
        val event = result.getOrNull()
        assertNotNull(event)
        assertEquals("tns1:VideoSource/MotionAlarm", event.topic)
        assertTrue(event.timestamp in before..after)
    }

    @Test
    fun `parseTopicExpression supports attribute based topic declaration`() {
        val xml = """<tt:SimpleItem Topic="tns1:RuleEngine/CellMotionDetector/Motion" Name="State" Value="true" />"""

        val topic = OnvifEventParser.parseTopicExpression(xml)
        assertEquals("tns1:RuleEngine/CellMotionDetector/Motion", topic)
    }

    @Test
    fun `parsePullMessagesResponse parses multiple notification messages`() {
        val xml = """
            <tev:PullMessagesResponse>
                <wsnt:NotificationMessage>
                    <wsnt:Topic>tns1:VideoSource/MotionAlarm</wsnt:Topic>
                    <wsnt:Message>Motion detected</wsnt:Message>
                </wsnt:NotificationMessage>
                <wsnt:NotificationMessage>
                    <wsnt:Topic>tns1:RuleEngine/LineDetector/Crossed</wsnt:Topic>
                    <wsnt:Message>Line crossed</wsnt:Message>
                </wsnt:NotificationMessage>
            </tev:PullMessagesResponse>
        """.trimIndent()

        val result = OnvifEventParser.parsePullMessagesResponse(xml)
        assertTrue(result.isSuccess)
        val events = result.getOrNull()
        assertNotNull(events)
        assertEquals(2, events.size)
        assertEquals("tns1:VideoSource/MotionAlarm", events[0].topic)
        assertEquals("tns1:RuleEngine/LineDetector/Crossed", events[1].topic)
    }

    @Test
    fun `parseSubscribeResponse fails when subscription reference is absent`() {
        val xml = """
            <wsnt:SubscribeResponse>
                <wsnt:TerminationTime>PT3600S</wsnt:TerminationTime>
            </wsnt:SubscribeResponse>
        """.trimIndent()

        val result = OnvifEventParser.parseSubscribeResponse(xml)
        assertTrue(result.isFailure)
        assertFails { result.getOrThrow() }
    }

    @Test
    fun `parseGetEventPropertiesResponse extracts topics filters and capabilities`() {
        val xml = """
            <tev:GetEventPropertiesResponse>
              <wsnt:TopicSet>
                <tt:Topic Name="MotionAlarm"/>
                <tt:Topic Name="TamperDetection"/>
              </wsnt:TopicSet>
              <wsnt:TopicExpressionDialects>
                <wsnt:TopicExpressionDialect>http://www.onvif.org/ver10/tev/topicExpression/ConcreteSet</wsnt:TopicExpressionDialect>
              </wsnt:TopicExpressionDialects>
              <tev:MessageContentFilterDialects>
                <tev:MessageContentFilterDialect>http://www.onvif.org/ver10/tev/messageContentFilter/ItemFilter</tev:MessageContentFilterDialect>
              </tev:MessageContentFilterDialects>
              <wsnt:MaxNotificationProducers>5</wsnt:MaxNotificationProducers>
            </tev:GetEventPropertiesResponse>
        """.trimIndent()

        val result = OnvifEventParser.parseGetEventPropertiesResponse(xml, "http://192.168.1.100/onvif/event")
        assertTrue(result.isSuccess)
        val props = result.getOrThrow()
        assertEquals(listOf("MotionAlarm", "TamperDetection"), props.supportedTopics)
        assertTrue(props.supportsPullPoint)
        assertTrue(props.supportsPush)
    }

    @Test
    fun `parseNotificationMessage handles empty properties`() {
        val xml = """
            <wsnt:NotificationMessage>
              <wsnt:Topic>tns1:Device/Tamper</wsnt:Topic>
            </wsnt:NotificationMessage>
        """.trimIndent()

        val result = OnvifEventParser.parseNotificationMessage(xml)
        assertTrue(result.isSuccess)
        val event = result.getOrThrow()
        assertEquals("tns1:Device/Tamper", event.topic)
        assertTrue(event.properties.isEmpty())
    }

    @Test
    fun `parsePullMessagesResponse returns empty list for no messages`() {
        val xml = """<tev:PullMessagesResponse></tev:PullMessagesResponse>"""
        val result = OnvifEventParser.parsePullMessagesResponse(xml)
        assertTrue(result.isSuccess)
        assertTrue(result.getOrThrow().isEmpty())
    }
}
