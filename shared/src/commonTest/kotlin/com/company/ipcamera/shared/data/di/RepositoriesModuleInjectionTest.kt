package com.company.ipcamera.shared.data.di

import com.company.ipcamera.shared.data.datasource.local.CameraLocalDataSource
import com.company.ipcamera.shared.data.datasource.local.EventLocalDataSource
import com.company.ipcamera.shared.data.datasource.local.NotificationLocalDataSource
import com.company.ipcamera.shared.data.datasource.local.RecordingLocalDataSource
import com.company.ipcamera.shared.data.datasource.local.SettingsLocalDataSource
import com.company.ipcamera.shared.data.datasource.local.UserLocalDataSource
import com.company.ipcamera.shared.data.datasource.remote.CameraRemoteDataSource
import com.company.ipcamera.shared.data.datasource.remote.EventRemoteDataSource
import com.company.ipcamera.shared.data.datasource.remote.NotificationRemoteDataSource
import com.company.ipcamera.shared.data.datasource.remote.RecordingRemoteDataSource
import com.company.ipcamera.shared.data.datasource.remote.SettingsRemoteDataSource
import com.company.ipcamera.shared.data.datasource.remote.UserRemoteDataSource
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.repository.NotificationRepository
import com.company.ipcamera.shared.domain.repository.RecordingRepository
import com.company.ipcamera.shared.domain.repository.SettingsRepository
import com.company.ipcamera.shared.domain.repository.UserRepository
import com.company.ipcamera.shared.test.MockCameraLocalDataSource
import com.company.ipcamera.shared.test.MockCameraRemoteDataSource
import com.company.ipcamera.shared.test.MockEventLocalDataSource
import com.company.ipcamera.shared.test.MockEventRemoteDataSource
import com.company.ipcamera.shared.test.MockNotificationLocalDataSource
import com.company.ipcamera.shared.test.MockNotificationRemoteDataSource
import com.company.ipcamera.shared.test.MockRecordingLocalDataSource
import com.company.ipcamera.shared.test.MockRecordingRemoteDataSource
import com.company.ipcamera.shared.test.MockSettingsLocalDataSource
import com.company.ipcamera.shared.test.MockSettingsRemoteDataSource
import com.company.ipcamera.shared.test.MockUserLocalDataSource
import com.company.ipcamera.shared.test.MockUserRemoteDataSource
import com.company.ipcamera.shared.test.TestDataFactory
import kotlinx.coroutines.test.runTest
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

class RepositoriesModuleInjectionTest {
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `camera repository from koin uses remote fallback and syncs local`() =
        runTest {
            stopKoin()

            val localCameraDataSource = MockCameraLocalDataSource()
            val remoteCameraDataSource = MockCameraRemoteDataSource()
            val remoteCamera = TestDataFactory.createTestCamera(id = "remote-cam-1", name = "Remote Camera")
            remoteCameraDataSource.addCameraDirectly(remoteCamera)

            val koinApp =
                startKoin {
                    modules(
                        module {
                            single<CameraLocalDataSource> { localCameraDataSource }
                            single<RecordingLocalDataSource> { MockRecordingLocalDataSource() }
                            single<EventLocalDataSource> { MockEventLocalDataSource() }
                            single<UserLocalDataSource> { MockUserLocalDataSource() }
                            single<SettingsLocalDataSource> { MockSettingsLocalDataSource() }
                            single<NotificationLocalDataSource> { MockNotificationLocalDataSource() }

                            single<CameraRemoteDataSource> { remoteCameraDataSource }
                            single<RecordingRemoteDataSource> { MockRecordingRemoteDataSource() }
                            single<EventRemoteDataSource> { MockEventRemoteDataSource() }
                            single<UserRemoteDataSource> { MockUserRemoteDataSource() }
                            single<SettingsRemoteDataSource> { MockSettingsRemoteDataSource() }
                            single<NotificationRemoteDataSource> { MockNotificationRemoteDataSource() }
                        },
                        repositoriesModule,
                    )
                }

            val repository = koinApp.koin.get<CameraRepository>()
            val loaded = repository.getCameras()

            assertEquals(1, loaded.size)
            assertEquals("remote-cam-1", loaded[0].id)
            assertEquals(1, localCameraDataSource.getCameras().size)
        }

    @Test
    fun `camera repository from koin prefers local data when local is not empty`() =
        runTest {
            stopKoin()

            val localCameraDataSource = MockCameraLocalDataSource()
            val remoteCameraDataSource = MockCameraRemoteDataSource()
            localCameraDataSource.addCameraDirectly(
                TestDataFactory.createTestCamera(id = "local-cam-1", name = "Local Camera"),
            )
            remoteCameraDataSource.addCameraDirectly(
                TestDataFactory.createTestCamera(id = "remote-cam-1", name = "Remote Camera"),
            )

            val koinApp =
                startKoin {
                    modules(
                        module {
                            single<CameraLocalDataSource> { localCameraDataSource }
                            single<RecordingLocalDataSource> { MockRecordingLocalDataSource() }
                            single<EventLocalDataSource> { MockEventLocalDataSource() }
                            single<UserLocalDataSource> { MockUserLocalDataSource() }
                            single<SettingsLocalDataSource> { MockSettingsLocalDataSource() }
                            single<NotificationLocalDataSource> { MockNotificationLocalDataSource() }

                            single<CameraRemoteDataSource> { remoteCameraDataSource }
                            single<RecordingRemoteDataSource> { MockRecordingRemoteDataSource() }
                            single<EventRemoteDataSource> { MockEventRemoteDataSource() }
                            single<UserRemoteDataSource> { MockUserRemoteDataSource() }
                            single<SettingsRemoteDataSource> { MockSettingsRemoteDataSource() }
                            single<NotificationRemoteDataSource> { MockNotificationRemoteDataSource() }
                        },
                        repositoriesModule,
                    )
                }

            val repository = koinApp.koin.get<CameraRepository>()
            val loaded = repository.getCameras()

            assertEquals(1, loaded.size)
            assertEquals("local-cam-1", loaded[0].id)
        }

    @Test
    fun `notification repository from koin uses remote fallback and syncs local`() =
        runTest {
            stopKoin()

            val localNotificationDataSource = MockNotificationLocalDataSource()
            val remoteNotificationDataSource = MockNotificationRemoteDataSource()
            val remoteNotification = TestDataFactory.createTestNotification(id = "remote-notif-1")
            remoteNotificationDataSource.addNotificationDirectly(remoteNotification)

            val koinApp =
                startKoin {
                    modules(
                        module {
                            single<CameraLocalDataSource> { MockCameraLocalDataSource() }
                            single<RecordingLocalDataSource> { MockRecordingLocalDataSource() }
                            single<EventLocalDataSource> { MockEventLocalDataSource() }
                            single<UserLocalDataSource> { MockUserLocalDataSource() }
                            single<SettingsLocalDataSource> { MockSettingsLocalDataSource() }
                            single<NotificationLocalDataSource> { localNotificationDataSource }

                            single<CameraRemoteDataSource> { MockCameraRemoteDataSource() }
                            single<RecordingRemoteDataSource> { MockRecordingRemoteDataSource() }
                            single<EventRemoteDataSource> { MockEventRemoteDataSource() }
                            single<UserRemoteDataSource> { MockUserRemoteDataSource() }
                            single<SettingsRemoteDataSource> { MockSettingsRemoteDataSource() }
                            single<NotificationRemoteDataSource> { remoteNotificationDataSource }
                        },
                        repositoriesModule,
                    )
                }

            val repository = koinApp.koin.get<NotificationRepository>()
            val loaded =
                repository.getNotifications(
                    userId = null,
                    type = null,
                    priority = null,
                    read = null,
                    page = 1,
                    limit = 10,
                )

            assertEquals(1, loaded.items.size)
            assertEquals("remote-notif-1", loaded.items[0].id)
            assertEquals(1, localNotificationDataSource.getNotifications().size)
        }

    @Test
    fun `notification repository from koin prefers local data when local is not empty`() =
        runTest {
            stopKoin()

            val localNotificationDataSource = MockNotificationLocalDataSource()
            val remoteNotificationDataSource = MockNotificationRemoteDataSource()
            localNotificationDataSource.addNotificationDirectly(
                TestDataFactory.createTestNotification(id = "local-notif-1"),
            )
            remoteNotificationDataSource.addNotificationDirectly(
                TestDataFactory.createTestNotification(id = "remote-notif-1"),
            )

            val koinApp =
                startKoin {
                    modules(
                        module {
                            single<CameraLocalDataSource> { MockCameraLocalDataSource() }
                            single<RecordingLocalDataSource> { MockRecordingLocalDataSource() }
                            single<EventLocalDataSource> { MockEventLocalDataSource() }
                            single<UserLocalDataSource> { MockUserLocalDataSource() }
                            single<SettingsLocalDataSource> { MockSettingsLocalDataSource() }
                            single<NotificationLocalDataSource> { localNotificationDataSource }

                            single<CameraRemoteDataSource> { MockCameraRemoteDataSource() }
                            single<RecordingRemoteDataSource> { MockRecordingRemoteDataSource() }
                            single<EventRemoteDataSource> { MockEventRemoteDataSource() }
                            single<UserRemoteDataSource> { MockUserRemoteDataSource() }
                            single<SettingsRemoteDataSource> { MockSettingsRemoteDataSource() }
                            single<NotificationRemoteDataSource> { remoteNotificationDataSource }
                        },
                        repositoriesModule,
                    )
                }

            val repository = koinApp.koin.get<NotificationRepository>()
            val loaded =
                repository.getNotifications(
                    userId = null,
                    type = null,
                    priority = null,
                    read = null,
                    page = 1,
                    limit = 10,
                )

            assertEquals(1, loaded.items.size)
            assertEquals("local-notif-1", loaded.items[0].id)
        }

    @Test
    fun `repositories module works in local-only mode when remote bindings are absent`() =
        runTest {
            stopKoin()

            val localCameraDataSource = MockCameraLocalDataSource()
            val localNotificationDataSource = MockNotificationLocalDataSource()
            localCameraDataSource.addCameraDirectly(TestDataFactory.createTestCamera(id = "local-cam-1"))
            localNotificationDataSource.addNotificationDirectly(
                TestDataFactory.createTestNotification(id = "local-notif-1"),
            )

            val koinApp =
                startKoin {
                    modules(
                        module {
                            single<CameraLocalDataSource> { localCameraDataSource }
                            single<RecordingLocalDataSource> { MockRecordingLocalDataSource() }
                            single<EventLocalDataSource> { MockEventLocalDataSource() }
                            single<UserLocalDataSource> { MockUserLocalDataSource() }
                            single<SettingsLocalDataSource> { MockSettingsLocalDataSource() }
                            single<NotificationLocalDataSource> { localNotificationDataSource }
                        },
                        repositoriesModule,
                    )
                }

            val cameraRepository = koinApp.koin.get<CameraRepository>()
            val notificationRepository = koinApp.koin.get<NotificationRepository>()
            val cameras = cameraRepository.getCameras()
            val notifications =
                notificationRepository.getNotifications(
                    userId = null,
                    type = null,
                    priority = null,
                    read = null,
                    page = 1,
                    limit = 10,
                )

            assertEquals(1, cameras.size)
            assertEquals("local-cam-1", cameras[0].id)
            assertEquals(1, notifications.items.size)
            assertEquals("local-notif-1", notifications.items[0].id)
        }

    @Test
    fun `repositories module resolves all repositories in local-only profile`() =
        runTest {
            stopKoin()

            val localCameraDataSource = MockCameraLocalDataSource()
            localCameraDataSource.addCameraDirectly(TestDataFactory.createTestCamera(id = "local-cam-1"))

            val koinApp =
                startKoin {
                    modules(
                        module {
                            single<CameraLocalDataSource> { localCameraDataSource }
                            single<RecordingLocalDataSource> { MockRecordingLocalDataSource() }
                            single<EventLocalDataSource> { MockEventLocalDataSource() }
                            single<UserLocalDataSource> { MockUserLocalDataSource() }
                            single<SettingsLocalDataSource> { MockSettingsLocalDataSource() }
                            single<NotificationLocalDataSource> { MockNotificationLocalDataSource() }
                        },
                        repositoriesModule,
                    )
                }

            val cameraRepository = koinApp.koin.get<CameraRepository>()
            val recordingRepository = koinApp.koin.get<RecordingRepository>()
            val eventRepository = koinApp.koin.get<EventRepository>()
            val userRepository = koinApp.koin.get<UserRepository>()
            val settingsRepository = koinApp.koin.get<SettingsRepository>()
            val notificationRepository = koinApp.koin.get<NotificationRepository>()

            val cameras = cameraRepository.getCameras()
            val recordings =
                recordingRepository.getRecordings(
                    cameraId = null,
                    startTime = null,
                    endTime = null,
                    page = 1,
                    limit = 10,
                )
            val events =
                eventRepository.getEvents(
                    type = null,
                    cameraId = null,
                    severity = null,
                    acknowledged = null,
                    startTime = null,
                    endTime = null,
                    page = 1,
                    limit = 10,
                )
            val users = userRepository.getUsers()
            val settings = settingsRepository.getSettings(category = null)
            val notifications =
                notificationRepository.getNotifications(
                    userId = null,
                    type = null,
                    priority = null,
                    read = null,
                    page = 1,
                    limit = 10,
                )

            assertEquals(1, cameras.size)
            assertEquals(0, recordings.items.size)
            assertEquals(0, events.items.size)
            assertEquals(0, users.items.size)
            assertEquals(0, settings.size)
            assertEquals(0, notifications.items.size)
        }
}
