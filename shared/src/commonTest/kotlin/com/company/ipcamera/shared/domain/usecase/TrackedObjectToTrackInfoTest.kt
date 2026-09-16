package com.company.ipcamera.shared.domain.usecase

import com.company.ipcamera.shared.domain.model.BoundingBox
import kotlin.test.Test
import kotlin.test.assertEquals

class TrackedObjectToTrackInfoTest {
    @Test
    fun toTrackInfo_parsesNumericCounterFromStandardId() {
        val box = BoundingBox(1, 2, 10, 20)
        val t =
            TrackedObject(
                id = "track_7_1700000000000",
                objectType = "person",
                currentBoundingBox = box,
                trajectory = listOf(box),
                confidence = 0.88f,
                startTime = 100L,
                lastSeenTime = 200L,
            )
        val info = t.toTrackInfo()
        assertEquals(7, info.trackId)
        assertEquals("person", info.objectType)
        assertEquals(0.88f, info.confidence)
        assertEquals(200L, info.lastSeen)
        assertEquals(1, info.boundingBox.x)
        assertEquals(20, info.boundingBox.height)
    }

    @Test
    fun toTrackInfo_nonStandardId_usesPositiveHashFallback() {
        val box = BoundingBox(0, 0, 5, 5)
        val t =
            TrackedObject(
                id = "custom-track-xyz",
                objectType = "vehicle",
                currentBoundingBox = box,
                trajectory = listOf(box),
                confidence = 0.5f,
                startTime = 0L,
                lastSeenTime = 1L,
            )
        val info = t.toTrackInfo()
        assertEquals("vehicle", info.objectType)
        assertEquals(1L, info.lastSeen)
        assert(info.trackId >= 0)
    }
}
