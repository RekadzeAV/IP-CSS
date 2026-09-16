package com.company.ipcamera.shared.domain.model

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.core.common.model.Resolution
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ObservationComplianceTest {
    @Test
    fun horizontalPixelsPerMeter() {
        assertNull(ObservationCompliance.horizontalPixelsPerMeter(0, 10.0))
        assertEquals(192.0, ObservationCompliance.horizontalPixelsPerMeter(1920, 10.0)!!, 1e-9)
    }

    @Test
    fun summarizeWarningWhenBelowTarget() {
        val cam =
            Camera(
                id = "1",
                name = "c",
                url = "rtsp://x",
                resolution = Resolution(1280, 720),
                settings =
                    CameraSettings(
                        observation =
                            ObservationSettings(
                                zoneClass = ObservationZoneClass.STANDARD,
                                sceneWidthMeters = 100.0,
                            ),
                    ),
                status = CameraStatus.UNKNOWN,
            )
        val s = ObservationCompliance.summarize(cam)
        assertTrue(s.lowResolutionWarning)
        assertEquals(12.8, s.computedPixelsPerMeter!!, 1e-9)
        assertEquals(20.0, s.targetPixelsPerMeterMin!!, 1e-9)
    }

    @Test
    fun summarizeNoWarningWhenSufficient() {
        val cam =
            Camera(
                id = "1",
                name = "c",
                url = "rtsp://x",
                resolution = Resolution(3840, 2160),
                settings =
                    CameraSettings(
                        observation =
                            ObservationSettings(
                                zoneClass = ObservationZoneClass.STANDARD,
                                sceneWidthMeters = 10.0,
                            ),
                    ),
                status = CameraStatus.UNKNOWN,
            )
        val s = ObservationCompliance.summarize(cam)
        assertFalse(s.lowResolutionWarning)
    }
}
