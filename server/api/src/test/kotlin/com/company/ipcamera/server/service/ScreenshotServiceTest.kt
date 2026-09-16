package com.company.ipcamera.server.service

import com.company.ipcamera.core.network.RtspFrame
import com.company.ipcamera.core.network.RtspStreamType
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Paths
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ScreenshotServiceTest {

    @Test
    fun `captureFrame returns null for empty video frame`() = runBlocking {
        val dir = createTempDirectory("screenshots-test-").toString()
        val svc = ScreenshotService(screenshotsDirectory = dir)
        val frame = RtspFrame(data = ByteArray(0), timestamp = 0L, streamType = RtspStreamType.VIDEO)
        assertNull(svc.captureFrame(frame, cameraId = "cam-1"))
    }

    @Test
    fun `captureFrame returns null for non-video frame`() = runBlocking {
        val dir = createTempDirectory("screenshots-test-").toString()
        val svc = ScreenshotService(screenshotsDirectory = dir)
        val frame = RtspFrame(data = byteArrayOf(1, 2, 3), timestamp = 0L, streamType = RtspStreamType.AUDIO)
        assertNull(svc.captureFrame(frame, cameraId = "cam-1"))
    }

    @Test
    fun `getScreenshotUrl uses file name only`() {
        val dir = createTempDirectory("screenshots-test-").toString()
        val svc = ScreenshotService(screenshotsDirectory = dir)
        val absolute = Paths.get(dir, "cam-1_123.jpg").toString()
        assertEquals(
            "/api/v1/screenshots/cam-1_123.jpg",
            svc.getScreenshotUrl(absolute)
        )
    }

    @Test
    fun `cleanupOldScreenshots removes files older than maxAge`() {
        val dirPath = createTempDirectory("screenshots-cleanup-")
        val dir = dirPath.toFile()
        try {
            val svc = ScreenshotService(screenshotsDirectory = dir.absolutePath)
            val old = File(dir, "old.jpg")
            old.writeBytes(byteArrayOf(1))
            val past = System.currentTimeMillis() - 26L * 60 * 60 * 1000
            assertTrue(old.setLastModified(past), "setLastModified(old) failed")
            val fresh = File(dir, "fresh.jpg")
            fresh.writeBytes(byteArrayOf(1))
            assertTrue(fresh.setLastModified(System.currentTimeMillis()), "setLastModified(fresh) failed")
            svc.cleanupOldScreenshots(maxAgeHours = 24)
            assertFalse(old.exists(), "old screenshot should be removed")
            assertTrue(fresh.exists(), "recent screenshot should remain")
        } finally {
            if (dir.exists()) dir.walkBottomUp().forEach { it.delete() }
        }
    }
}
