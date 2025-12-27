package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.core.network.api.UserApiService
import com.company.ipcamera.core.network.dto.*
import com.company.ipcamera.shared.data.datasource.local.UserLocalDataSource
import com.company.ipcamera.shared.data.datasource.remote.UserRemoteDataSource
import com.company.ipcamera.shared.domain.model.User
import com.company.ipcamera.shared.domain.model.UserRole
import com.company.ipcamera.shared.domain.repository.LoginResult
import com.company.ipcamera.shared.domain.repository.PaginatedResult
import com.company.ipcamera.shared.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация UserRepository с использованием Data Sources (новая архитектура)
 *
 * Использует стратегию remote-first для аутентификации, local-first для данных пользователей
 */
class UserRepositoryImplV2(
    private val localDataSource: UserLocalDataSource,
    private val remoteDataSource: UserRemoteDataSource?,
    private val userApiService: UserApiService? = null // Для обратной совместимости
) : UserRepository {

    override suspend fun login(username: String, password: String): Result<LoginResult> = withContext(Dispatchers.IO) {
        try {
            val request = LoginRequest(username = username, password = password)

            // Используем UserApiService напрямую для аутентификации
            if (userApiService != null) {
                val response = userApiService.login(request)
                val user = response.user.toDomain()

                // Сохраняем пользователя в локальную БД после успешного входа
                localDataSource.saveUser(user).getOrNull()

                Result.success(
                    LoginResult(
                        user = user,
                        token = response.token,
                        refreshToken = response.refreshToken,
                        expiresIn = response.expiresIn
                    )
                )
            } else if (remoteDataSource != null) {
                // Альтернативный путь через RemoteDataSource (но в UserApiService нет метода login)
                Result.failure(Exception("UserApiService is required for login"))
            } else {
                Result.failure(Exception("Authentication service not available"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error logging in user: $username" }
            Result.failure(e)
        }
    }

    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // Используем UserApiService напрямую
            userApiService?.logout()
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error logging out" }
            Result.failure(e)
        }
    }

    override suspend fun register(
        username: String,
        email: String,
        password: String,
        fullName: String?
    ): Result<User> = withContext(Dispatchers.IO) {
        try {
            val request = RegisterRequest(username = username, email = email, password = password, fullName = fullName)

            // Используем UserApiService напрямую
            if (userApiService != null) {
                val response = userApiService.register(request)
                response.user?.let { userDto ->
                    val user = userDto.toDomain()
                    // Сохраняем пользователя в локальную БД после успешной регистрации
                    localDataSource.saveUser(user).getOrNull()
                    Result.success(user)
                } ?: Result.failure(Exception(response.message ?: "Registration failed"))
            } else if (remoteDataSource != null) {
                // Альтернативный путь через RemoteDataSource
                remoteDataSource.createUser(User(
                    id = "", // Будет создан на сервере
                    username = username,
                    email = email,
                    fullName = fullName,
                    role = UserRole.USER,
                    permissions = emptyList(),
                    createdAt = System.currentTimeMillis(),
                    lastLoginAt = null,
                    isActive = true
                )).fold(
                    onSuccess = { user ->
                        localDataSource.saveUser(user).getOrNull()
                        Result.success(user)
                    },
                    onError = { error ->
                        logger.error(error) { "Error registering user: $username" }
                        Result.failure(Exception(error.message))
                    }
                )
            } else {
                Result.failure(Exception("Registration service not available"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error registering user: $username" }
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUser(): User? = withContext(Dispatchers.Default) {
        try {
            // Получаем первого активного пользователя локально (упрощенная логика)
            // В реальном приложении текущий пользователь должен храниться отдельно
            val localUsers = localDataSource.getUsers()
            val currentUser = localUsers.firstOrNull { it.isActive } ?: run {
                // Если не найдено локально, пытаемся получить с сервера
                if (userApiService != null) {
                    try {
                        val response = userApiService.getCurrentUser()
                        val user = response.toDomain()
                        localDataSource.saveUser(user).getOrNull()
                        user
                    } catch (e: Exception) {
                        logger.warn(e) { "Failed to get current user from remote" }
                        null
                    }
                } else {
                    remoteDataSource?.getCurrentUser()?.fold(
                        onSuccess = { user ->
                            localDataSource.saveUser(user).getOrNull()
                            user
                        },
                        onError = {
                            logger.warn(it) { "Failed to get current user from remote" }
                            null
                        }
                    )
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting current user" }
            null
        }
    }

    override suspend fun updateCurrentUser(user: User): Result<User> = withContext(Dispatchers.Default) {
        try {
            // Обновляем локально
            val localResult = localDataSource.updateUser(user)

            // Если есть удаленный источник, синхронизируем
            if (userApiService != null) {
                try {
                    val request = UpdateUserRequest(
                        email = user.email,
                        fullName = user.fullName,
                        password = null
                    )
                    val response = userApiService.updateCurrentUser(request)
                    val updatedUser = response.toDomain()
                    localDataSource.updateUser(updatedUser).getOrNull()
                    Result.success(updatedUser)
                } catch (e: Exception) {
                    logger.warn(e) { "Failed to sync user update to remote, but updated locally: ${user.id}" }
                    localResult
                }
            } else if (remoteDataSource != null) {
                remoteDataSource.updateUser(user.id, user).fold(
                    onSuccess = { remoteUser ->
                        localDataSource.updateUser(remoteUser).getOrNull()
                        Result.success(remoteUser)
                    },
                    onError = { error ->
                        logger.warn(error) { "Failed to sync user update to remote, but updated locally: ${user.id}" }
                        localResult
                    }
                )
            } else {
                localResult
            }
        } catch (e: Exception) {
            logger.error(e) { "Error updating current user: ${user.id}" }
            Result.failure(e)
        }
    }

    override suspend fun getUsers(
        page: Int,
        limit: Int,
        role: String?
    ): PaginatedResult<User> = withContext(Dispatchers.Default) {
        try {
            // Получаем пользователей локально
            val allUsers = localDataSource.getUsers()
            val filteredUsers = role?.let { r ->
                allUsers.filter { it.role.name.equals(r, ignoreCase = true) }
            } ?: allUsers

            val offset = (page - 1) * limit
            val paginatedItems = filteredUsers.drop(offset).take(limit)
            val localResult = PaginatedResult(
                items = paginatedItems,
                total = filteredUsers.size,
                page = page,
                limit = limit,
                hasMore = (offset + limit) < filteredUsers.size
            )

            // Если локально пусто или есть удаленный источник, синхронизируем
            if ((localResult.items.isEmpty() || remoteDataSource != null) && remoteDataSource != null) {
                remoteDataSource.getUsers().fold(
                    onSuccess = { remoteUsers ->
                        localDataSource.saveUsers(remoteUsers).getOrNull()
                        val filtered = role?.let { r ->
                            remoteUsers.filter { it.role.name.equals(r, ignoreCase = true) }
                        } ?: remoteUsers
                        val offsetRemote = (page - 1) * limit
                        val paginatedRemote = filtered.drop(offsetRemote).take(limit)
                        PaginatedResult(
                            items = paginatedRemote,
                            total = filtered.size,
                            page = page,
                            limit = limit,
                            hasMore = (offsetRemote + limit) < filtered.size
                        )
                    },
                    onError = { error ->
                        logger.warn(error) { "Failed to get users from remote, using local" }
                        localResult
                    }
                )
            } else {
                localResult
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting users" }
            PaginatedResult(emptyList(), 0, page, limit, false)
        }
    }

    override suspend fun getUserById(id: String): User? = withContext(Dispatchers.Default) {
        try {
            // Сначала проверяем локально
            localDataSource.getUserById(id) ?: run {
                // Если не найдено локально, пытаемся получить с сервера
                remoteDataSource?.getUserById(id)?.fold(
                    onSuccess = { user ->
                        localDataSource.saveUser(user).getOrNull()
                        user
                    },
                    onError = {
                        logger.warn(it) { "Failed to get user from remote: $id" }
                        null
                    }
                )
            }
        } catch (e: Exception) {
            logger.error(e) { "Error getting user by id: $id" }
            null
        }
    }

    override suspend fun updateUser(user: User): Result<User> = withContext(Dispatchers.Default) {
        try {
            // Обновляем локально
            val localResult = localDataSource.updateUser(user)

            // Если есть удаленный источник, синхронизируем
            if (remoteDataSource != null && localResult.isSuccess) {
                remoteDataSource.updateUser(user.id, user).fold(
                    onSuccess = { remoteUser ->
                        localDataSource.updateUser(remoteUser).getOrNull()
                        Result.success(remoteUser)
                    },
                    onError = { error ->
                        logger.warn(error) { "Failed to sync user update to remote, but updated locally: ${user.id}" }
                        localResult
                    }
                )
            } else {
                localResult
            }
        } catch (e: Exception) {
            logger.error(e) { "Error updating user: ${user.id}" }
            Result.failure(e)
        }
    }

    override suspend fun deleteUser(id: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            // Удаляем локально
            val localResult = localDataSource.deleteUser(id)

            // Если есть удаленный источник, синхронизируем
            if (remoteDataSource != null && localResult.isSuccess) {
                remoteDataSource.deleteUser(id).fold(
                    onSuccess = {
                        Result.success(Unit)
                    },
                    onError = { error ->
                        logger.warn(error) { "Failed to delete user from remote, but deleted locally: $id" }
                        localResult
                    }
                )
            } else {
                localResult
            }
        } catch (e: Exception) {
            logger.error(e) { "Error deleting user: $id" }
            Result.failure(e)
        }
    }

    override suspend fun refreshToken(refreshToken: String): Result<LoginResult> = withContext(Dispatchers.IO) {
        try {
            if (userApiService != null) {
                val response = userApiService.refreshToken(refreshToken)
                val user = response.user.toDomain()

                // Обновляем пользователя в локальной БД
                localDataSource.saveUser(user).getOrNull()

                Result.success(
                    LoginResult(
                        user = user,
                        token = response.token,
                        refreshToken = response.refreshToken,
                        expiresIn = response.expiresIn
                    )
                )
            } else {
                Result.failure(Exception("UserApiService is required for token refresh"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error refreshing token" }
            Result.failure(e)
        }
    }

    /**
     * Маппинг UserResponse в Domain модель User
     */
    private fun UserResponse.toDomain(): User {
        return User(
            id = id,
            username = username,
            email = email,
            fullName = fullName,
            role = UserRole.valueOf(role.uppercase()),
            permissions = permissions,
            createdAt = createdAt,
            lastLoginAt = lastLoginAt,
            isActive = isActive
        )
    }
}

