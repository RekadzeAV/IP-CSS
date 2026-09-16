package com.company.ipcamera.core.network.test

import com.company.ipcamera.core.network.MediaFrame
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for MediaFrame JVM implementation.
 * 
 * Tests verify the basic contract and behavior of MediaFrame
 * in the jvmMain source set.
 */
class MediaFrameJvmTest {

    @Test
    fun `frame data should be non-empty`() {
        // Given
        val data = ByteArray(1024) { 0x42 }
        val timestamp = System.currentTimeMillis()

        // When
        val frame = MediaFrame(data, timestamp)

        // Then
        assertTrue(frame.data.isNotEmpty(), "Frame data should not be empty")
        assertEquals(1024, frame.data.size, "Frame data size should match input")
    }

    @Test
    fun `frame timestamp should be positive`() {
        // Given
        val data = ByteArray(1024)
        val timestamp = System.currentTimeMillis()

        // When
        val frame = MediaFrame(data, timestamp)

        // Then
        assertTrue(frame.timestamp > 0, "Frame timestamp should be positive")
        assertEquals(timestamp, frame.timestamp, "Frame timestamp should match input")
    }

    @Test
    fun `frame should preserve data integrity`() {
        // Given
        val originalData = ByteArray(256) { it.toByte() }
        val timestamp = System.currentTimeMillis()
        val frame = MediaFrame(originalData, timestamp)

        // When - verify data hasn't changed
        val dataCopy = frame.data.copyOf()

        // Then
        assertTrue(originalData.contentEquals(dataCopy), "Frame data should preserve integrity")
    }

    @Test
    fun `frame with empty data should be valid`() {
        // Given
        val emptyData = ByteArray(0)
        val timestamp = System.currentTimeMillis()

        // When
        val frame = MediaFrame(emptyData, timestamp)

        // Then
        assertNotNull(frame, "Frame with empty data should be created")
        assertEquals(0, frame.data.size, "Frame data size should be 0")
    }

    @Test
    fun `frame with large data should be valid`() {
        // Given
        val largeData = ByteArray(1024 * 1024) { 0xAA } // 1MB
        val timestamp = System.currentTimeMillis()

        // When
        val frame = MediaFrame(largeData, timestamp)

        // Then
        assertNotNull(frame, "Frame with large data should be created")
        assertEquals(1024 * 1024, frame.data.size, "Frame data size should match input")
    }

    @Test
    fun `frame timestamp should be current time`() {
        // Given
        val data = ByteArray(1024)
        val beforeCreation = System.currentTimeMillis()

        // When
        val frame = MediaFrame(data, beforeCreation)

        // Then
        val afterCreation = System.currentTimeMillis()
        assertTrue(
            frame.timestamp >= beforeCreation && frame.timestamp <= afterCreation,
            "Frame timestamp should be within creation time range"
        )
    }
}