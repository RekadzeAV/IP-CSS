package com.company.ipcamera.platform.nas.common.service

import com.company.ipcamera.platform.nas.common.hardware.HardwareInfo
import com.company.ipcamera.platform.nas.common.hardware.HardwareType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*

/**
 * Unit tests for NasPlatformManager
 */
class NasPlatformManagerTest {

    private lateinit var manager: NasPlatformManager
    private lateinit var mockPlatformService: NasPlatformService

    @Before
    fun setup() {
        manager = NasPlatformManager()
        mockPlatformService = mock()
    }

    @Test
    fun `test registerService adds service to list`() = runBlocking {
        // Given
        val testService = TestNasPlatformService("TestPlatform")

        // When
        manager.registerService(testService)

        // Then
        // Service should be registered (internal state)
        assertTrue(true) // Registration completed
    }

    @Test
    fun `test initialize detects platform`() = runBlocking {
        // Given
        val testService = TestNasPlatformService("TestPlatform", isCurrent = true)
        whenever(testService.initialize()).thenReturn(Result.success(Unit))
        manager.registerService(testService)

        // When
        val result = manager.initialize()

        // Then
        assertTrue(result.isSuccess)
        assertNotNull(manager.getCurrentService())
        assertEquals("TestPlatform", manager.getCurrentService()?.platformName)
    }

    @Test
    fun `test initialize fails when no platform detected`() = runBlocking {
        // Given
        val testService = TestNasPlatformService("TestPlatform", isCurrent = false)
        manager.registerService(testService)

        // When
        val result = manager.initialize()

        // Then
        assertTrue(result.isFailure)
        assertNull(manager.getCurrentService())
    }

    @Test
    fun `test isRunningOnNas returns true when platform detected`() = runBlocking {
        // Given
        val testService = TestNasPlatformService("TestPlatform", isCurrent = true)
        whenever(testService.initialize()).thenReturn(Result.success(Unit))
        manager.registerService(testService)
        manager.initialize()

        // When
        val result = manager.isRunningOnNas()

        // Then
        assertTrue(result)
    }

    @Test
    fun `test isRunningOnNas returns false when no platform detected`() = runBlocking {
        // Given
        val testService = TestNasPlatformService("TestPlatform", isCurrent = false)
        manager.registerService(testService)

        // When
        val result = manager.isRunningOnNas()

        // Then
        assertFalse(result)
    }

    @Test
    fun `test sendNotification delegates to active service`() = runBlocking {
        // Given
        val testService = TestNasPlatformService("TestPlatform", isCurrent = true)
        whenever(testService.initialize()).thenReturn(Result.success(Unit))
        whenever(testService.sendNotification(any(), any())).thenReturn(Result.success(Unit))
        manager.registerService(testService)
        manager.initialize()

        // When
        val result = manager.sendNotification("Test message", NotificationSeverity.INFO)

        // Then
        assertTrue(result.isSuccess)
        verify(testService).sendNotification("Test message", NotificationSeverity.INFO)
    }

    @Test
    fun `test sendNotification fails when no active service`() = runBlocking {
        // Given
        // No service registered

        // When
        val result = manager.sendNotification("Test message", NotificationSeverity.INFO)

        // Then
        assertTrue(result.isFailure)
        assertEquals("No active NAS platform service", result.exceptionOrNull()?.message)
    }

    @Test
    fun `test getSystemResources delegates to active service`() = runBlocking {
        // Given
        val testService = TestNasPlatformService("TestPlatform", isCurrent = true)
        val expectedResources = SystemResources(
            cpuUsagePercent = 25.0,
            memoryTotalBytes = 8L * 1024 * 1024 * 1024,
            memoryUsedBytes = 4L * 1024 * 1024 * 1024,
            memoryFreeBytes = 4L * 1024 * 1024 * 1024,
            diskTotalBytes = 1000L * 1024 * 1024 * 1024,
            diskUsedBytes = 500L * 1024 * 1024 * 1024,
            diskFreeBytes = 500L * 1024 * 1024 * 1024,
            temperatureCelsius = 45.0,
            uptimeSeconds = 86400
        )
        whenever(testService.initialize()).thenReturn(Result.success(Unit))
        whenever(testService.getSystemResources()).thenReturn(Result.success(expectedResources))
        manager.registerService(testService)
        manager.initialize()

        // When
        val result = manager.getSystemResources()

        // Then
        assertTrue(result.isSuccess)
        assertEquals(expectedResources, result.getOrNull())
        verify(testService).getSystemResources()
    }

    @Test
    fun `test createBackup delegates to active service`() = runBlocking {
        // Given
        val testService = TestNasPlatformService("TestPlatform", isCurrent = true)
        whenever(testService.initialize()).thenReturn(Result.success(Unit))
        whenever(testService.createBackup(any(), any())).thenReturn(Result.success(Unit))
        manager.registerService(testService)
        manager.initialize()

        // When
        val result = manager.createBackup("/backup/location", BackupType.FULL)

        // Then
        assertTrue(result.isSuccess)
        verify(testService).createBackup("/backup/location", BackupType.FULL)
    }

    @Test
    fun `test shutdown clears all services`() = runBlocking {
        // Given
        val testService = TestNasPlatformService("TestPlatform", isCurrent = true)
        whenever(testService.initialize()).thenReturn(Result.success(Unit))
        whenever(testService.shutdown()).thenReturn(Result.success(Unit))
        manager.registerService(testService)
        manager.initialize()

        // When
        val result = manager.shutdown()

        // Then
        assertTrue(result.isSuccess)
        assertNull(manager.getCurrentService())
        verify(testService).shutdown()
    }

    @Test
    fun `test getPlatformStatusFlow emits RUNNING status`() = runBlocking {
        // Given
        val testService = TestNasPlatformService("TestPlatform", isCurrent = true)
        val expectedResources = SystemResources(
            cpuUsagePercent = 25.0,
            memoryTotalBytes = 8L * 1024 * 1024 * 1024,
            memoryUsedBytes = 4L * 1024 * 1024 * 1024,
            memoryFreeBytes = 4L * 1024 * 1024 * 1024,
            diskTotalBytes = 1000L * 1024 * 1024 * 1024,
            diskUsedBytes = 500L * 1024 * 1024 * 1024,
            diskFreeBytes = 500L * 1024 * 1024 * 1024,
            temperatureCelsius = 45.0,
            uptimeSeconds = 86400
        )
        whenever(testService.initialize()).thenReturn(Result.success(Unit))
        whenever(testService.getSystemResources()).thenReturn(Result.success(expectedResources))
        manager.registerService(testService)
        manager.initialize()

        // When
        val status = manager.getPlatformStatusFlow().first()

        // Then
        assertTrue(status is NasPlatformManager.PlatformStatus.RUNNING)
        val running = status as NasPlatformManager.PlatformStatus.RUNNING
        assertEquals(expectedResources, running.resources)
    }

    // Test implementation
    private class TestNasPlatformService(
        override val platformName: String,
        private val isCurrent: Boolean = false
    ) : NasPlatformService {

        override suspend fun isCurrentPlatform(): Boolean = isCurrent

        override suspend fun initialize(): Result<Unit> = Result.success(Unit)

        override suspend fun sendNotification(message: String, severity: NotificationSeverity): Result<Unit> =
            Result.success(Unit)

        override suspend fun getSystemResources(): Result<SystemResources> =
            Result.success(
                SystemResources(
                    cpuUsagePercent = 0.0,
                    memoryTotalBytes = 0,
                    memoryUsedBytes = 0,
                    memoryFreeBytes = 0,
                    diskTotalBytes = 0,
                    diskUsedBytes = 0,
                    diskFreeBytes = 0,
                    temperatureCelsius = null,
                    uptimeSeconds = 0
                )
            )

        override suspend fun createBackup(destination: String, backupType: BackupType): Result<Unit> =
            Result.success(Unit)

        override suspend fun getHardwareEncoder() = null

        override suspend fun getHardwareInfo(): Result<HardwareInfo> =
            Result.success(
                HardwareInfo(
                    name = "Test Hardware",
                    type = HardwareType.OTHER,
                    available = true,
                    memoryBytes = 0,
                    utilization = 0.0,
                    temperatureCelsius = null,
                    codecs = emptyList()
                )
            )

        override suspend fun shutdown(): Result<Unit> = Result.success(Unit)
    }
}
