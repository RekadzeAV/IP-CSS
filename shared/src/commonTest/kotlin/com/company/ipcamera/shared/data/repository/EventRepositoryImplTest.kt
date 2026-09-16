package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.test.MockEventLocalDataSource
import com.company.ipcamera.shared.test.MockEventRemoteDataSource
import com.company.ipcamera.shared.test.TestDataFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для EventRepositoryImpl
 */
class EventRepositoryImplTest {
    private lateinit var localDataSource: MockEventLocalDataSource
    private lateinit var remoteDataSource: MockEventRemoteDataSource
    private lateinit var repository: EventRepositoryImpl

    @BeforeTest
    fun setup() {
        localDataSource = MockEventLocalDataSource()
        remoteDataSource = MockEventRemoteDataSource()
        repository = EventRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun `test getEvents with filtering by type`() =
        runTest {
            // Arrange
            val motionEvent =
                TestDataFactory.createTestEvent(
                    id = "event-1",
                    type = EventType.MOTION_DETECTION,
                    cameraId = "camera-1",
                )
            val objectEvent =
                TestDataFactory.createTestEvent(
                    id = "event-2",
                    type = EventType.OBJECT_DETECTION,
                    cameraId = "camera-1",
                )
            localDataSource.addEventDirectly(motionEvent)
            localDataSource.addEventDirectly(objectEvent)

            // Act
            val result =
                repository.getEvents(
                    type = EventType.MOTION_DETECTION,
                    cameraId = null,
                    severity = null,
                    acknowledged = null,
                    startTime = null,
                    endTime = null,
                    page = 1,
                    limit = 10,
                )

            // Assert
            assertEquals(1, result.items.size)
            assertEquals(EventType.MOTION_DETECTION, result.items[0].type)
        }

    @Test
    fun `test getEvents with filtering by severity`() =
        runTest {
            // Arrange
            val criticalEvent =
                TestDataFactory.createTestEvent(
                    id = "event-1",
                    severity = EventSeverity.CRITICAL,
                )
            val infoEvent =
                TestDataFactory.createTestEvent(
                    id = "event-2",
                    severity = EventSeverity.INFO,
                )
            localDataSource.addEventDirectly(criticalEvent)
            localDataSource.addEventDirectly(infoEvent)

            // Act
            val result =
                repository.getEvents(
                    type = null,
                    cameraId = null,
                    severity = EventSeverity.CRITICAL,
                    acknowledged = null,
                    startTime = null,
                    endTime = null,
                    page = 1,
                    limit = 10,
                )

            // Assert
            assertEquals(1, result.items.size)
            assertEquals(EventSeverity.CRITICAL, result.items[0].severity)
        }

    @Test
    fun `test acknowledgeEvents batch operation`() =
        runTest {
            // Arrange
            val event1 = TestDataFactory.createTestEvent(id = "event-1", acknowledged = false)
            val event2 = TestDataFactory.createTestEvent(id = "event-2", acknowledged = false)
            localDataSource.addEventDirectly(event1)
            localDataSource.addEventDirectly(event2)
            remoteDataSource.addEventDirectly(event1)
            remoteDataSource.addEventDirectly(event2)

            // Act
            val result = repository.acknowledgeEvents(listOf("event-1", "event-2"), "user-1")

            // Assert
            assertTrue(result.isSuccess)
            val events = localDataSource.getEvents()
            assertTrue(events.all { it.acknowledged })
        }

    @Test
    fun `test getEventStatistics`() =
        runTest {
            // Arrange
            val event1 =
                TestDataFactory.createTestEvent(
                    id = "event-1",
                    type = EventType.MOTION_DETECTION,
                    severity = EventSeverity.WARNING,
                )
            val event2 =
                TestDataFactory.createTestEvent(
                    id = "event-2",
                    type = EventType.OBJECT_DETECTION,
                    severity = EventSeverity.CRITICAL,
                )
            remoteDataSource.addEventDirectly(event1)
            remoteDataSource.addEventDirectly(event2)

            // Act
            val result = repository.getEventStatistics(cameraId = null, startTime = null, endTime = null)

            // Assert
            assertNotNull(result)
            assertTrue(result.getOrNull()?.containsKey("total") == true)
        }

    @Test
    fun `test getEventStatistics falls back to local when remote is unavailable`() =
        runTest {
            // Arrange
            val repositoryLocalOnly = EventRepositoryImpl(localDataSource, null)
            val event1 =
                TestDataFactory.createTestEvent(
                    id = "event-local-1",
                    type = EventType.MOTION_DETECTION,
                    severity = EventSeverity.WARNING,
                    acknowledged = false,
                )
            val event2 =
                TestDataFactory.createTestEvent(
                    id = "event-local-2",
                    type = EventType.OBJECT_DETECTION,
                    severity = EventSeverity.CRITICAL,
                    acknowledged = true,
                )
            localDataSource.addEventDirectly(event1)
            localDataSource.addEventDirectly(event2)

            // Act
            val result = repositoryLocalOnly.getEventStatistics(cameraId = null, startTime = null, endTime = null)
            val stats = result.getOrNull()

            // Assert
            assertTrue(result.isSuccess)
            assertNotNull(stats)
            assertEquals(2, stats["total"])
            assertEquals(1, stats["unacknowledged"])
        }

    @Test
    fun `test createEvent syncs with remote`() =
        runTest {
            // Arrange
            val event = TestDataFactory.createTestEvent(id = "event-1", cameraId = "camera-1")

            // Act
            val result = repository.addEvent(event)

            // Assert
            assertTrue(result.isSuccess)
            // Проверяем, что событие добавлено в local
            val localEvents = localDataSource.getEvents()
            assertEquals(1, localEvents.size)
            // Проверяем, что событие синхронизировано с remote
            val remoteEvents =
                remoteDataSource.getEvents().fold(
                    onSuccess = { it.items },
                    onError = { emptyList() },
                )
            assertEquals(1, remoteEvents.size)
        }

    @Test
    fun `test addEvent succeeds when remote create fails and local is updated`() =
        runTest {
            val event = TestDataFactory.createTestEvent(id = "event-1", cameraId = "camera-1")
            remoteDataSource.shouldFailOnCreate = true
            remoteDataSource.networkError = Exception("Network error")

            val result = repository.addEvent(event)

            assertTrue(result.isSuccess)
            val localEvents = localDataSource.getEvents()
            assertEquals(1, localEvents.size)
            assertEquals("event-1", localEvents[0].id)
        }

    @Test
    fun `test getEvents handles network errors gracefully`() =
        runTest {
            // Arrange
            remoteDataSource.shouldFailOnGet = true
            remoteDataSource.networkError = Exception("Network error")
            // localDataSource пустой

            // Act
            val result =
                repository.getEvents(
                    type = null,
                    cameraId = null,
                    severity = null,
                    acknowledged = null,
                    startTime = null,
                    endTime = null,
                    page = 1,
                    limit = 10,
                )

            // Assert - должен вернуть пустой результат, не упав с ошибкой
            assertEquals(0, result.items.size)
        }

    @Test
    fun `test getEvents uses local-first strategy`() =
        runTest {
            // Arrange
            val localEvent = TestDataFactory.createTestEvent(id = "local-1", cameraId = "camera-1")
            val remoteEvent = TestDataFactory.createTestEvent(id = "remote-1", cameraId = "camera-1")

            localDataSource.addEventDirectly(localEvent)
            remoteDataSource.addEventDirectly(remoteEvent)

            // Act
            val result =
                repository.getEvents(
                    type = null,
                    cameraId = null,
                    severity = null,
                    acknowledged = null,
                    startTime = null,
                    endTime = null,
                    page = 1,
                    limit = 10,
                )

            // Assert - должен вернуть локальные данные
            assertEquals(1, result.items.size)
            assertEquals("local-1", result.items[0].id)
        }

    @Test
    fun `test getEvents with remote fallback when local is empty`() =
        runTest {
            // Arrange
            val remoteEvent = TestDataFactory.createTestEvent(id = "remote-1", cameraId = "camera-1")
            remoteDataSource.addEventDirectly(remoteEvent)
            // localDataSource пустой

            // Act
            val result =
                repository.getEvents(
                    type = null,
                    cameraId = null,
                    severity = null,
                    acknowledged = null,
                    startTime = null,
                    endTime = null,
                    page = 1,
                    limit = 10,
                )

            // Assert - должен вернуть данные с remote и сохранить в local
            assertEquals(1, result.items.size)
            assertEquals("remote-1", result.items[0].id)
            // Проверяем, что данные сохранились в local
            val localEvents = localDataSource.getEvents()
            assertEquals(1, localEvents.size)
        }

    @Test
    fun `test repository works with local-only strategy when remote is null`() =
        runTest {
            // Arrange
            val repositoryLocalOnly = EventRepositoryImpl(localDataSource, null)
            val event = TestDataFactory.createTestEvent(id = "event-1", cameraId = "camera-1")
            localDataSource.addEventDirectly(event)

            // Act
            val result =
                repositoryLocalOnly.getEvents(
                    type = null,
                    cameraId = null,
                    severity = null,
                    acknowledged = null,
                    startTime = null,
                    endTime = null,
                    page = 1,
                    limit = 10,
                )

            // Assert
            assertEquals(1, result.items.size)
            assertEquals("event-1", result.items[0].id)
        }

    @Test
    fun `test acknowledgeEvent succeeds when remote acknowledge fails`() =
        runTest {
            val event = TestDataFactory.createTestEvent(id = "event-1", cameraId = "camera-1", acknowledged = false)
            localDataSource.addEventDirectly(event)
            remoteDataSource.addEventDirectly(event)
            remoteDataSource.shouldFailOnAcknowledge = true
            remoteDataSource.networkError = Exception("Network error")

            val result = repository.acknowledgeEvent("event-1", "user-1")

            assertTrue(result.isSuccess)
            val localEvent = localDataSource.getEventById("event-1")
            assertNotNull(localEvent)
            assertTrue(localEvent.acknowledged)
            assertEquals("user-1", localEvent.acknowledgedBy)
        }
}
