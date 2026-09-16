package com.company.ipcamera.core.network.onvif

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class OnvifEventServiceTest {

    @Test
    fun testOnvifEventCreation() {
        val event = OnvifEvent(
            topic = "tns1:VideoSource/MotionAlarm",
            timestamp = System.currentTimeMillis(),
            message = "Motion detected",
            properties = mapOf("State" to "true"),
            source = "http://192.168.1.100"
        )

        assertEquals("tns1:VideoSource/MotionAlarm", event.topic)
        assertNotNull(event.timestamp)
        assertEquals("Motion detected", event.message)
        assertEquals("true", event.properties["State"])
    }

    @Test
    fun testOnvifEventTypeDetection() {
        assertTrue(OnvifEventType.isMotionDetection(OnvifEventType.MOTION_ALARM))
        assertTrue(OnvifEventType.isMotionDetection(OnvifEventType.CELL_MOTION_DETECTOR))
        assertTrue(OnvifEventType.isIntrusionDetection(OnvifEventType.INTRUSION_DETECTOR))
        assertTrue(OnvifEventType.isIntrusionDetection(OnvifEventType.LINE_DETECTOR_CROSSED))
        assertTrue(OnvifEventType.isAlarm(OnvifEventType.DEVICE_TAMPER_DETECTED))
    }

    @Test
    fun testOnvifEventSubscription() {
        val subscription = OnvifEventSubscription(
            id = "sub-123",
            cameraUrl = "http://192.168.1.100",
            notificationConsumerUrl = "http://localhost:8080/events",
            subscriptionReference = "http://192.168.1.100/onvif/subscription",
            expirationTime = System.currentTimeMillis() + 3600000,
            createdAt = System.currentTimeMillis(),
            status = SubscriptionStatus.ACTIVE
        )

        assertEquals("sub-123", subscription.id)
        assertEquals(SubscriptionStatus.ACTIVE, subscription.status)
        assertTrue(!subscription.isExpired())
        assertTrue(subscription.needsRenewal(System.currentTimeMillis() + 3300000)) // 5 минут до истечения
    }

    @Test
    fun testOnvifEventFilter() {
        val filter = OnvifEventFilter(
            topicExpression = "tns1:VideoSource/MotionAlarm",
            includedTopics = listOf("Motion", "Intrusion"),
            excludedTopics = listOf("System")
        )

        val matchingEvent = OnvifEvent(
            topic = "tns1:VideoSource/MotionAlarm",
            timestamp = System.currentTimeMillis()
        )

        val nonMatchingEvent = OnvifEvent(
            topic = "tns1:System/SystemDateTimeChanged",
            timestamp = System.currentTimeMillis()
        )

        assertTrue(filter.matches(matchingEvent))
        assertTrue(!filter.matches(nonMatchingEvent))
    }

    @Test
    fun testOnvifEventProperties() {
        val properties = OnvifEventProperties(
            eventServiceUrl = "http://192.168.1.100/onvif/event_service",
            supportedTopics = listOf("Motion", "Intrusion", "Tampering"),
            supportsPullPoint = true,
            supportsPush = true
        )

        assertEquals("http://192.168.1.100/onvif/event_service", properties.eventServiceUrl)
        assertEquals(3, properties.supportedTopics.size)
        assertTrue(properties.supportsPullPoint)
        assertTrue(properties.supportsPush)
    }

    @Test
    fun testSubscriptionStatus() {
        assertEquals(SubscriptionStatus.ACTIVE, SubscriptionStatus.valueOf("ACTIVE"))
        assertEquals(SubscriptionStatus.EXPIRED, SubscriptionStatus.valueOf("EXPIRED"))
        assertEquals(SubscriptionStatus.CANCELLED, SubscriptionStatus.valueOf("CANCELLED"))
    }
}
