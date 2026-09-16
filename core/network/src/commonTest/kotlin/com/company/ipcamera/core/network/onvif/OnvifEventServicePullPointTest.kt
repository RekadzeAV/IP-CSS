package com.company.ipcamera.core.network.onvif

import com.company.ipcamera.core.network.test.MockEngineFactory
import io.ktor.http.*
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Unit тесты для PullPoint lifecycle ONVIF Event Service.
 */
class OnvifEventServicePullPointTest {

    private val capabilitiesXml = """
        <SOAP-ENV:Envelope>
          <SOAP-ENV:Body>
            <tds:GetCapabilitiesResponse>
              <tds:Capabilities>
                <tt:Events>
                  <tt:XAddr>http://192.168.1.100/onvif/event_service</tt:XAddr>
                </tt:Events>
              </tds:Capabilities>
            </tds:GetCapabilitiesResponse>
          </SOAP-ENV:Body>
        </SOAP-ENV:Envelope>
    """.trimIndent()

    private val pullPointResponseXml = """
        <SOAP-ENV:Envelope>
          <SOAP-ENV:Body>
            <tev:CreatePullPointSubscriptionResponse>
              <tev:SubscriptionReference>
                <wsa:Address>http://192.168.1.100/onvif/event_service/PullPoint</wsa:Address>
              </tev:SubscriptionReference>
              <wsnt:TerminationTime>PT3600S</wsnt:TerminationTime>
            </tev:CreatePullPointSubscriptionResponse>
          </SOAP-ENV:Body>
        </SOAP-ENV:Envelope>
    """.trimIndent()

    private val pullMessagesResponseXml = """
        <SOAP-ENV:Envelope>
          <SOAP-ENV:Body>
            <tev:PullMessagesResponse>
              <wsnt:NotificationMessage>
                <wsnt:Topic>tns1:VideoSource/MotionAlarm</wsnt:Topic>
                <wsnt:Message>
                  <tt:Message UtcTime="2024-01-26T12:00:00Z">
                    <tt:Source><tt:SimpleItem Name="Source" Value="VideoSource_1"/></tt:Source>
                    <tt:Data><tt:SimpleItem Name="State" Value="true"/></tt:Data>
                  </tt:Message>
                </wsnt:Message>
              </wsnt:NotificationMessage>
            </tev:PullMessagesResponse>
          </SOAP-ENV:Body>
        </SOAP-ENV:Envelope>
    """.trimIndent()

    private val renewResponseXml = """
        <SOAP-ENV:Envelope>
          <SOAP-ENV:Body>
            <wsnt:RenewResponse>
              <wsnt:TerminationTime>PT7200S</wsnt:TerminationTime>
            </wsnt:RenewResponse>
          </SOAP-ENV:Body>
        </SOAP-ENV:Envelope>
    """.trimIndent()

    private val unsubscribeResponseXml = """
        <SOAP-ENV:Envelope>
          <SOAP-ENV:Body>
            <wsnt:UnsubscribeResponse/>
          </SOAP-ENV:Body>
        </SOAP-ENV:Envelope>
    """.trimIndent()

    @Test
    fun `test createPullPointSubscription succeeds`() = runTest {
        val engine = MockEngineFactory.createOnvif(
            soapResponses = mapOf(
                "GetCapabilities" to capabilitiesXml,
                "CreatePullPointSubscription" to pullPointResponseXml
            )
        )
        val service = OnvifEventServiceImpl(engine)

        val result = service.createPullPointSubscription(
            cameraUrl = "http://192.168.1.100",
            username = "admin",
            password = "password"
        )

        assertTrue(result.isSuccess)
        val subscription = result.getOrThrow()
        assertEquals("http://192.168.1.100/onvif/event_service/PullPoint", subscription.subscriptionReference)
        assertEquals(SubscriptionStatus.ACTIVE, subscription.status)
    }

    @Test
    fun `test pullMessages returns parsed events`() = runTest {
        val engine = MockEngineFactory.createOnvif(
            soapResponses = mapOf(
                "GetCapabilities" to capabilitiesXml,
                "CreatePullPointSubscription" to pullPointResponseXml,
                "PullMessages" to pullMessagesResponseXml
            )
        )
        val service = OnvifEventServiceImpl(engine)

        val createResult = service.createPullPointSubscription(
            cameraUrl = "http://192.168.1.100",
            username = "admin",
            password = "password"
        )
        val subscription = createResult.getOrThrow()

        val pullResult = service.pullMessages(subscription.id, timeout = 1000, maxMessages = 10)
        assertTrue(pullResult.isSuccess)
        val events = pullResult.getOrThrow()
        assertEquals(1, events.size)
        assertEquals("tns1:VideoSource/MotionAlarm", events.first().topic)
        assertEquals("true", events.first().properties["State"])
    }

    @Test
    fun `test renewSubscription updates expiration time`() = runTest {
        val engine = MockEngineFactory.createOnvif(
            soapResponses = mapOf(
                "GetCapabilities" to capabilitiesXml,
                "CreatePullPointSubscription" to pullPointResponseXml,
                "Renew" to renewResponseXml
            )
        )
        val service = OnvifEventServiceImpl(engine)

        val createResult = service.createPullPointSubscription(
            cameraUrl = "http://192.168.1.100",
            username = "admin",
            password = "password"
        )
        val subscription = createResult.getOrThrow()
        val originalExpiration = subscription.expirationTime

        val renewResult = service.renewSubscription(subscription.id, renewalTime = 7200)
        assertTrue(renewResult.isSuccess)
        val renewed = renewResult.getOrThrow()
        assertTrue(renewed.expirationTime > originalExpiration)
    }

    @Test
    fun `test unsubscribe marks subscription cancelled`() = runTest {
        val engine = MockEngineFactory.createOnvif(
            soapResponses = mapOf(
                "GetCapabilities" to capabilitiesXml,
                "CreatePullPointSubscription" to pullPointResponseXml,
                "Unsubscribe" to unsubscribeResponseXml
            )
        )
        val service = OnvifEventServiceImpl(engine)

        val createResult = service.createPullPointSubscription(
            cameraUrl = "http://192.168.1.100",
            username = "admin",
            password = "password"
        )
        val subscription = createResult.getOrThrow()

        val unsubscribeResult = service.unsubscribe(subscription.id)
        assertTrue(unsubscribeResult.isSuccess)

        val updated = service.getSubscription(subscription.id)
        assertNotNull(updated)
        assertEquals(SubscriptionStatus.CANCELLED, updated.status)
    }

    @Test
    fun `test unsubscribeAll removes all camera subscriptions`() = runTest {
        val engine = MockEngineFactory.createOnvif(
            soapResponses = mapOf(
                "GetCapabilities" to capabilitiesXml,
                "CreatePullPointSubscription" to pullPointResponseXml,
                "Unsubscribe" to unsubscribeResponseXml
            )
        )
        val service = OnvifEventServiceImpl(engine)

        val result = service.createPullPointSubscription(
            cameraUrl = "http://192.168.1.100",
            username = "admin",
            password = "password"
        )
        val subscription = result.getOrThrow()

        assertEquals(1, service.getSubscriptionsForCamera("http://192.168.1.100").size)

        val unsubscribeAllResult = service.unsubscribeAll("http://192.168.1.100")
        assertTrue(unsubscribeAllResult.isSuccess)
        assertEquals(0, service.getSubscriptionsForCamera("http://192.168.1.100").size)
    }

    @Test
    fun `test getSubscriptionsForCamera filters by active status`() = runTest {
        val engine = MockEngineFactory.createOnvif(
            soapResponses = mapOf(
                "GetCapabilities" to capabilitiesXml,
                "CreatePullPointSubscription" to pullPointResponseXml,
                "Unsubscribe" to unsubscribeResponseXml
            )
        )
        val service = OnvifEventServiceImpl(engine)

        val result = service.createPullPointSubscription("http://192.168.1.100")
        val subscription = result.getOrThrow()

        service.unsubscribe(subscription.id)

        val activeSubs = service.getSubscriptionsForCamera("http://192.168.1.100")
        assertTrue(activeSubs.isEmpty())
    }

    @Test
    fun `test getEventProperties returns parsed properties`() = runTest {
        val eventPropertiesXml = """
            <SOAP-ENV:Envelope>
              <SOAP-ENV:Body>
                <tev:GetEventPropertiesResponse>
                  <wsnt:FixedTopicSet>true</wsnt:FixedTopicSet>
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
                  <wsnt:MaxNotificationProducers>10</wsnt:MaxNotificationProducers>
                </tev:GetEventPropertiesResponse>
              </SOAP-ENV:Body>
            </SOAP-ENV:Envelope>
        """.trimIndent()

        val engine = MockEngineFactory.createOnvif(
            soapResponses = mapOf(
                "GetCapabilities" to capabilitiesXml,
                "GetEventProperties" to eventPropertiesXml
            )
        )
        val service = OnvifEventServiceImpl(engine)

        val result = service.getEventProperties("http://192.168.1.100")
        assertTrue(result.isSuccess)
        val props = result.getOrThrow()
        assertEquals("http://192.168.1.100/onvif/event_service", props.eventServiceUrl)
        assertEquals(listOf("MotionAlarm", "TamperDetection"), props.supportedTopics)
        assertTrue(props.supportsPullPoint)
        assertTrue(props.supportsPush)
    }

    @Test
    fun `test pullMessages fails for non-existent subscription`() = runTest {
        val engine = MockEngineFactory.createOnvif(soapResponses = emptyMap())
        val service = OnvifEventServiceImpl(engine)

        val result = service.pullMessages("non-existent-id")
        assertTrue(result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message?.contains("not found") == true)
    }

    @Test
    fun `test renewSubscription fails for cancelled subscription`() = runTest {
        val engine = MockEngineFactory.createOnvif(
            soapResponses = mapOf(
                "GetCapabilities" to capabilitiesXml,
                "CreatePullPointSubscription" to pullPointResponseXml,
                "Unsubscribe" to unsubscribeResponseXml
            )
        )
        val service = OnvifEventServiceImpl(engine)

        val createResult = service.createPullPointSubscription("http://192.168.1.100")
        val subscription = createResult.getOrThrow()
        service.unsubscribe(subscription.id)

        val renewResult = service.renewSubscription(subscription.id)
        assertTrue(renewResult.isFailure)
    }
}
