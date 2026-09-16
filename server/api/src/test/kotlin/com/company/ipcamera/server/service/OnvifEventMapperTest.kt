package com.company.ipcamera.server.service

import com.company.ipcamera.core.network.onvif.OnvifEvent
import com.company.ipcamera.core.network.onvif.OnvifEventType
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Юнит-тесты маппинга ONVIF событий в доменную модель (EventType, EventSeverity).
 */
class OnvifEventMapperTest {

    private fun event(topic: String, message: String? = null, properties: Map<String, String> = emptyMap()) =
        OnvifEvent(topic = topic, timestamp = System.currentTimeMillis(), message = message, properties = properties)

    @Test
    fun `mapToDomainEvent - motion topic maps to MOTION_DETECTION and INFO`() {
        val onvif = event(OnvifEventType.MOTION_ALARM, "Motion detected")
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", "Camera 1")

        assertEquals(EventType.MOTION_DETECTION, domain.type)
        assertEquals(EventSeverity.INFO, domain.severity)
        assertEquals("cam-1", domain.cameraId)
        assertEquals("Camera 1", domain.cameraName)
        assertEquals(onvif.timestamp, domain.timestamp)
        assertNotNull(domain.id)
    }

    @Test
    fun `mapToDomainEvent - intrusion topic maps to OBJECT_DETECTION and CRITICAL`() {
        val onvif = event(OnvifEventType.INTRUSION_DETECTOR)
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(EventType.OBJECT_DETECTION, domain.type)
        assertEquals(EventSeverity.CRITICAL, domain.severity)
        assertEquals("cam-1", domain.cameraId)
    }

    @Test
    fun `mapToDomainEvent - line detector crossed maps to OBJECT_DETECTION and WARNING`() {
        val onvif = event(OnvifEventType.LINE_DETECTOR_CROSSED)
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(EventType.OBJECT_DETECTION, domain.type)
        assertEquals(EventSeverity.WARNING, domain.severity)
    }

    @Test
    fun `mapToDomainEvent - VideoSourceLost maps to CAMERA_OFFLINE and WARNING`() {
        val onvif = event(OnvifEventType.VIDEO_SOURCE_LOST)
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(EventType.CAMERA_OFFLINE, domain.type)
        assertEquals(EventSeverity.WARNING, domain.severity)
    }

    @Test
    fun `mapToDomainEvent - VideoSourceRecovered maps to CAMERA_ONLINE and INFO`() {
        val onvif = event(OnvifEventType.VIDEO_SOURCE_RECOVERED)
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(EventType.CAMERA_ONLINE, domain.type)
        assertEquals(EventSeverity.INFO, domain.severity)
    }

    @Test
    fun `mapToDomainEvent - device tamper maps to OTHER and CRITICAL`() {
        val onvif = event(OnvifEventType.DEVICE_TAMPER_DETECTED)
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", "Cam")

        assertEquals(EventType.OTHER, domain.type)
        assertEquals(EventSeverity.CRITICAL, domain.severity)
    }

    @Test
    fun `mapToDomainEvent - device offline maps to CAMERA_OFFLINE and WARNING`() {
        val onvif = event("tns1:Device/SomeOfflineEvent")
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(EventType.CAMERA_OFFLINE, domain.type)
        assertEquals(EventSeverity.WARNING, domain.severity)
    }

    @Test
    fun `mapToDomainEvent - device online maps to CAMERA_ONLINE and INFO`() {
        val onvif = event("tns1:Device/Online")
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(EventType.CAMERA_ONLINE, domain.type)
        assertEquals(EventSeverity.INFO, domain.severity)
    }

    @Test
    fun `mapToDomainEvent - recording started maps to RECORDING_STARTED and INFO`() {
        val onvif = event("tns1:Recording/Started")
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(EventType.RECORDING_STARTED, domain.type)
        assertEquals(EventSeverity.INFO, domain.severity)
    }

    @Test
    fun `mapToDomainEvent - RecordingConfig Recording Started topic maps to RECORDING_STARTED`() {
        val onvif = event("tns1:RecordingConfig/Recording/Started", "Recording started")
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", "Cam")

        assertEquals(EventType.RECORDING_STARTED, domain.type)
        assertEquals(EventSeverity.INFO, domain.severity)
    }

    @Test
    fun `mapToDomainEvent - recording stopped maps to RECORDING_STOPPED and INFO`() {
        val onvif = event("tns1:Recording/Stopped")
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(EventType.RECORDING_STOPPED, domain.type)
        assertEquals(EventSeverity.INFO, domain.severity)
    }

    @Test
    fun `mapToDomainEvent - SystemDateTimeChanged maps to OTHER and INFO`() {
        val onvif = event(OnvifEventType.SYSTEM_DATE_TIME_CHANGED)
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(EventType.OTHER, domain.type)
        assertEquals(EventSeverity.INFO, domain.severity)
    }

    @Test
    fun `mapToDomainEvent - system error topic maps to SYSTEM_ERROR and WARNING`() {
        val onvif = event("tns1:System/Error")
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(EventType.SYSTEM_ERROR, domain.type)
        assertEquals(EventSeverity.WARNING, domain.severity)
    }

    @Test
    fun `mapToDomainEvent - Device IO PortState maps to OTHER and INFO`() {
        val onvif = event(OnvifEventType.DEVICE_IO_PORT_STATE)
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(EventType.OTHER, domain.type)
        assertEquals(EventSeverity.INFO, domain.severity)
    }

    @Test
    fun `mapToDomainEvent - VideoSourceConfiguration maps to OTHER and INFO`() {
        val onvif = event(OnvifEventType.VIDEO_SOURCE_CONFIGURATION)
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(EventType.OTHER, domain.type)
        assertEquals(EventSeverity.INFO, domain.severity)
    }

    @Test
    fun `mapToDomainEvent - AnalyticsStream maps to OTHER and INFO`() {
        val onvif = event(OnvifEventType.ANALYTICS_STREAM)
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(EventType.OTHER, domain.type)
        assertEquals(EventSeverity.INFO, domain.severity)
    }

    @Test
    fun `mapToDomainEvent - LoiteringDetector maps to OBJECT_DETECTION and CRITICAL`() {
        val onvif = event(OnvifEventType.LOITERING_DETECTOR)
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(EventType.OBJECT_DETECTION, domain.type)
        assertEquals(EventSeverity.CRITICAL, domain.severity)
    }

    @Test
    fun `mapToDomainEvent - System SystemError topic maps to SYSTEM_ERROR and WARNING`() {
        val onvif = event("tns1:System/SystemError", "Internal error")
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(EventType.SYSTEM_ERROR, domain.type)
        assertEquals(EventSeverity.WARNING, domain.severity)
    }

    @Test
    fun `mapToDomainEvent - StorageFull topic maps to STORAGE_FULL and WARNING`() {
        val onvif = event("tns1:Device/StorageFailure", "Storage full")
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(EventType.STORAGE_FULL, domain.type)
        assertEquals(EventSeverity.WARNING, domain.severity)
    }

    @Test
    fun `mapToDomainEvent - Storage Full topic variant maps to STORAGE_FULL and WARNING`() {
        val onvif = event("tns1:RecordingConfig/Storage/Full")
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(EventType.STORAGE_FULL, domain.type)
        assertEquals(EventSeverity.WARNING, domain.severity)
    }

    @Test
    fun `mapToDomainEvent - HardwareFailure topic maps to SYSTEM_ERROR and WARNING`() {
        val onvif = event(OnvifEventType.DEVICE_HARDWARE_FAILURE, "Storage failure")
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(EventType.SYSTEM_ERROR, domain.type)
        assertEquals(EventSeverity.WARNING, domain.severity)
    }

    @Test
    fun `mapToDomainEvent - unknown topic maps to OTHER and INFO`() {
        val onvif = event("tns1:Custom/UnknownTopic")
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(EventType.OTHER, domain.type)
        assertEquals(EventSeverity.INFO, domain.severity)
    }

    @Test
    fun `mapToDomainEvent - empty topic maps to OTHER and INFO`() {
        val onvif = event("")
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(EventType.OTHER, domain.type)
        assertEquals(EventSeverity.INFO, domain.severity)
    }

    @Test
    fun `mapToDomainEvent - topic matching is case-insensitive`() {
        val onvif = event("tns1:rules/INTRUSIONDETECTOR")
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(EventType.OBJECT_DETECTION, domain.type)
        assertEquals(EventSeverity.CRITICAL, domain.severity)
    }

    @Test
    fun `mapToDomainEvent - uses message when present`() {
        val onvif = event(OnvifEventType.MOTION_ALARM, "Custom message")
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals("Custom message", domain.description)
    }

    @Test
    fun `mapToDomainEvent - empty message falls back to generated description`() {
        val onvif = event(OnvifEventType.MOTION_ALARM, "")
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertTrue(domain.description?.isNotBlank() == true)
        assertTrue(domain.description != "")
    }

    @Test
    fun `mapToDomainEvent - metadata contains ONVIF properties`() {
        val props = mapOf("State" to "true", "Source" to "VideoSource_1")
        val onvif = event(OnvifEventType.MOTION_ALARM, properties = props)
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(props, domain.metadata)
    }

    @Test
    fun `mapToDomainEvent - topic priority wins over conflicting payload hints`() {
        val onvif = event(
            OnvifEventType.INTRUSION_DETECTOR,
            message = "severity=info",
            properties = mapOf("severity" to "INFO", "eventType" to "MOTION_DETECTION")
        )
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(EventType.OBJECT_DETECTION, domain.type)
        assertEquals(EventSeverity.CRITICAL, domain.severity)
    }

    @Test
    fun `mapToDomainEvent - missing payload fields handled safely`() {
        val onvif = OnvifEvent(topic = OnvifEventType.MOTION_ALARM, timestamp = 0L, message = null, properties = emptyMap())
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(EventType.MOTION_DETECTION, domain.type)
        assertEquals(EventSeverity.INFO, domain.severity)
        assertTrue(domain.description?.isNotBlank() == true)
        assertTrue(domain.metadata.isEmpty())
    }

    @Test
    fun `mapToDomainEvent - invalid severity source in payload does not override mapping`() {
        val onvif = event(
            "tns1:System/SystemError",
            message = "unexpected",
            properties = mapOf("severity" to "SEVEREST", "level" to "???")
        )
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertEquals(EventType.SYSTEM_ERROR, domain.type)
        assertEquals(EventSeverity.WARNING, domain.severity)
    }

    @Test
    fun `mapToDomainEvent - acknowledged is false`() {
        val onvif = event(OnvifEventType.MOTION_ALARM)
        val domain = OnvifEventMapper.mapToDomainEvent(onvif, "cam-1", null)

        assertTrue(!domain.acknowledged)
        assertEquals(null, domain.acknowledgedAt)
        assertEquals(null, domain.acknowledgedBy)
    }
}
