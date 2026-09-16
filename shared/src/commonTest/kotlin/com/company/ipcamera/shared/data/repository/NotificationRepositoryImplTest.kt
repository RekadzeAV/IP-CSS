package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.shared.test.MockNotificationLocalDataSource
import com.company.ipcamera.shared.test.MockNotificationRemoteDataSource
import com.company.ipcamera.shared.test.TestDataFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для NotificationRepositoryImpl
 */
class NotificationRepositoryImplTest {
    private lateinit var localDataSource: MockNotificationLocalDataSource
    private lateinit var remoteDataSource: MockNotificationRemoteDataSource
    private lateinit var repository: NotificationRepositoryImpl

    @BeforeTest
    fun setup() {
        localDataSource = MockNotificationLocalDataSource()
        remoteDataSource = MockNotificationRemoteDataSource()
        repository = NotificationRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun `test getNotifications with filtering by read status`() =
        runTest {
            val readNotification = TestDataFactory.createTestNotification(id = "notif-1", read = true)
            val unreadNotification = TestDataFactory.createTestNotification(id = "notif-2", read = false)
            localDataSource.addNotificationDirectly(readNotification)
            localDataSource.addNotificationDirectly(unreadNotification)

            val result =
                repository.getNotifications(
                    userId = null,
                    type = null,
                    priority = null,
                    read = false,
                    page = 1,
                    limit = 20,
                )

            assertEquals(1, result.items.size)
            assertFalse(result.items[0].read)
        }

    @Test
    fun `test markAsRead batch operation`() =
        runTest {
            val notif1 = TestDataFactory.createTestNotification(id = "notif-1", read = false)
            val notif2 = TestDataFactory.createTestNotification(id = "notif-2", read = false)
            localDataSource.addNotificationDirectly(notif1)
            localDataSource.addNotificationDirectly(notif2)
            remoteDataSource.addNotificationDirectly(notif1)
            remoteDataSource.addNotificationDirectly(notif2)

            val result = repository.markAsRead(listOf("notif-1", "notif-2"))

            assertTrue(result.isSuccess)
            val notifications = localDataSource.getNotifications()
            assertTrue(notifications.all { it.read })
        }

    @Test
    fun `test markAllAsRead`() =
        runTest {
            val notif1 = TestDataFactory.createTestNotification(id = "notif-1", read = false)
            val notif2 = TestDataFactory.createTestNotification(id = "notif-2", read = false)
            localDataSource.addNotificationDirectly(notif1)
            localDataSource.addNotificationDirectly(notif2)

            val result = repository.markAllAsRead("user-1")

            assertTrue(result.isSuccess)
            val notifications = localDataSource.getNotifications()
            assertTrue(notifications.all { it.read })
        }

    @Test
    fun `test getUnreadCount`() =
        runTest {
            val readNotification = TestDataFactory.createTestNotification(id = "notif-1", read = true)
            val unreadNotification1 = TestDataFactory.createTestNotification(id = "notif-2", read = false)
            val unreadNotification2 = TestDataFactory.createTestNotification(id = "notif-3", read = false)
            localDataSource.addNotificationDirectly(readNotification)
            localDataSource.addNotificationDirectly(unreadNotification1)
            localDataSource.addNotificationDirectly(unreadNotification2)

            val result = repository.getUnreadCount("user-1")

            assertEquals(2, result)
        }

    @Test
    fun `test addNotification syncs with remote`() =
        runTest {
            val notification = TestDataFactory.createTestNotification(id = "notif-1")

            val result = repository.addNotification(notification)

            assertTrue(result.isSuccess)
            val localNotifications = localDataSource.getNotifications()
            assertEquals(1, localNotifications.size)
        }

    @Test
    fun `test getNotifications uses local-first strategy`() =
        runTest {
            val localNotification = TestDataFactory.createTestNotification(id = "local-1")
            val remoteNotification = TestDataFactory.createTestNotification(id = "remote-1")

            localDataSource.addNotificationDirectly(localNotification)
            remoteDataSource.addNotificationDirectly(remoteNotification)

            val result =
                repository.getNotifications(
                    userId = null,
                    type = null,
                    priority = null,
                    read = null,
                    page = 1,
                    limit = 20,
                )

            assertEquals(1, result.items.size)
            assertEquals("local-1", result.items[0].id)
        }

    @Test
    fun `test getNotifications with remote fallback when local is empty`() =
        runTest {
            val remoteNotification = TestDataFactory.createTestNotification(id = "remote-1")
            remoteDataSource.addNotificationDirectly(remoteNotification)

            val result =
                repository.getNotifications(
                    userId = null,
                    type = null,
                    priority = null,
                    read = null,
                    page = 1,
                    limit = 20,
                )

            assertEquals(1, result.items.size)
            assertEquals("remote-1", result.items[0].id)
            val localNotifications = localDataSource.getNotifications()
            assertEquals(1, localNotifications.size)
        }

    @Test
    fun `test getNotifications keeps local data when remote fails`() =
        runTest {
            val localNotification = TestDataFactory.createTestNotification(id = "local-1")
            localDataSource.addNotificationDirectly(localNotification)
            remoteDataSource.shouldFailOnGet = true
            remoteDataSource.networkError = Exception("Network error")

            val result =
                repository.getNotifications(
                    userId = null,
                    type = null,
                    priority = null,
                    read = null,
                    page = 1,
                    limit = 20,
                )

            assertEquals(1, result.items.size)
            assertEquals("local-1", result.items[0].id)
        }

    @Test
    fun `test repository works with local-only strategy when remote is null`() =
        runTest {
            val repositoryLocalOnly = NotificationRepositoryImpl(localDataSource, null)
            val notification = TestDataFactory.createTestNotification(id = "notif-1")
            localDataSource.addNotificationDirectly(notification)

            val result =
                repositoryLocalOnly.getNotifications(
                    userId = null,
                    type = null,
                    priority = null,
                    read = null,
                    page = 1,
                    limit = 20,
                )

            assertEquals(1, result.items.size)
            assertEquals("notif-1", result.items[0].id)
        }
}
