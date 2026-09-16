package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.shared.test.MockUserLocalDataSource
import com.company.ipcamera.shared.test.MockUserRemoteDataSource
import com.company.ipcamera.shared.test.TestDataFactory
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/**
 * Тесты для UserRepositoryImpl
 */
class UserRepositoryImplTest {
    private lateinit var localDataSource: MockUserLocalDataSource
    private lateinit var remoteDataSource: MockUserRemoteDataSource
    private lateinit var repository: UserRepositoryImpl

    @BeforeTest
    fun setup() {
        localDataSource = MockUserLocalDataSource()
        remoteDataSource = MockUserRemoteDataSource()
        repository = UserRepositoryImpl(localDataSource, remoteDataSource)
    }

    @Test
    fun `test getUserById uses local-first strategy`() =
        runTest {
            // Arrange
            val localUser = TestDataFactory.createTestUser(id = "user-1", username = "localuser")
            val remoteUser = TestDataFactory.createTestUser(id = "user-2", username = "remoteuser")

            localDataSource.addUserDirectly(localUser)
            remoteDataSource.addUserDirectly(remoteUser)

            // Act
            val result = repository.getUserById("user-1")

            // Assert - должен вернуть локальные данные
            assertNotNull(result)
            assertEquals("localuser", result?.username)
        }

    @Test
    fun `test getUserById with remote fallback when local is empty`() =
        runTest {
            // Arrange
            val remoteUser = TestDataFactory.createTestUser(id = "user-1", username = "remoteuser")
            remoteDataSource.addUserDirectly(remoteUser)
            // localDataSource пустой

            // Act
            val result = repository.getUserById("user-1")

            // Assert - должен вернуть данные с remote и сохранить в local
            assertNotNull(result)
            assertEquals("remoteuser", result?.username)
            // Проверяем, что данные сохранились в local
            val localUsers = localDataSource.getUsers()
            assertEquals(1, localUsers.size)
        }

    @Test
    fun `test updateUser syncs with remote`() =
        runTest {
            // Arrange
            val user = TestDataFactory.createTestUser(id = "user-1", username = "original")
            localDataSource.addUserDirectly(user)
            remoteDataSource.addUserDirectly(user)

            val updatedUser = user.copy(username = "updated")

            // Act
            val result = repository.updateUser(updatedUser)

            // Assert
            assertTrue(result.isSuccess)
            // Проверяем, что пользователь обновлен в local
            val localUser = localDataSource.getUserById("user-1")
            assertEquals("updated", localUser?.username)
        }

    @Test
    fun `test getUsers uses local-first strategy`() =
        runTest {
            // Arrange
            val localUser = TestDataFactory.createTestUser(id = "user-1", username = "localuser")
            localDataSource.addUserDirectly(localUser)

            // Act
            val result = repository.getUsers()

            // Assert
            assertEquals(1, result.items.size)
            assertEquals("localuser", result.items[0].username)
        }

    @Test
    fun `test getUsers with remote fallback when local is empty`() =
        runTest {
            val remoteUser = TestDataFactory.createTestUser(id = "user-remote", username = "remoteuser")
            remoteDataSource.addUserDirectly(remoteUser)

            val result = repository.getUsers()

            assertEquals(1, result.items.size)
            assertEquals("remoteuser", result.items[0].username)
            val localUsers = localDataSource.getUsers()
            assertEquals(1, localUsers.size)
            assertEquals("user-remote", localUsers[0].id)
        }

    @Test
    fun `test getUsers keeps local data when remote fails`() =
        runTest {
            val localUser = TestDataFactory.createTestUser(id = "user-1", username = "localuser")
            localDataSource.addUserDirectly(localUser)
            remoteDataSource.shouldFailOnGet = true
            remoteDataSource.networkError = Exception("Network error")

            val result = repository.getUsers()

            assertEquals(1, result.items.size)
            assertEquals("localuser", result.items[0].username)
        }

    @Test
    fun `test repository works with local-only strategy when remote is null`() =
        runTest {
            // Arrange
            val repositoryLocalOnly = UserRepositoryImpl(localDataSource, null)
            val user = TestDataFactory.createTestUser(id = "user-1", username = "localuser")
            localDataSource.addUserDirectly(user)

            // Act
            val result = repositoryLocalOnly.getUsers()

            // Assert
            assertEquals(1, result.items.size)
            assertEquals("localuser", result.items[0].username)
        }
}
