package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.core.network.api.UserApiService
import com.company.ipcamera.core.network.dto.*
import com.company.ipcamera.shared.data.datasource.local.UserLocalDataSource
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
 * Реализация UserRepository с использованием SQLDelight для локальных операций
 * и API сервиса для аутентификации
 *
 * Локальное хранение делегируется [UserLocalDataSource] (INSERT OR REPLACE / upsert),
 * без второго подключения к БД и расхождения с [com.company.ipcamera.shared.data.datasource.local.impl.UserLocalDataSourceImpl].
 */
class UserRepositoryImplSqlDelight(
    private val userLocalDataSource: UserLocalDataSource,
    private val userApiService: UserApiService,
) : UserRepository {
    override suspend fun login(
        username: String,
        password: String,
    ): Result<LoginResult> =
        withContext(
            Dispatchers.Default,
        ) {
            try {
                val request = LoginRequest(username = username, password = password)
                val response = userApiService.login(request)
                val user = response.user.toDomain()
                saveUserLocally(user)
                Result.success(
                    LoginResult(
                        user = user,
                        token = response.token,
                        refreshToken = response.refreshToken ?: "",
                        expiresIn = response.expiresIn,
                    ),
                )
            } catch (e: Exception) {
                logger.error(e) { "Error logging in user: $username" }
                Result.failure(e)
            }
        }

    override suspend fun logout(): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                userApiService.logout()
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
        fullName: String?,
    ): Result<User> =
        withContext(Dispatchers.Default) {
            try {
                val request =
                    RegisterRequest(username = username, email = email, password = password, fullName = fullName)
                val response = userApiService.register(request)
                response.user?.let { userDto ->
                    val user = userDto.toDomain()
                    saveUserLocally(user)
                    Result.success(user)
                } ?: Result.failure(Exception(response.message ?: "Registration failed"))
            } catch (e: Exception) {
                logger.error(e) { "Error registering user: $username" }
                Result.failure(e)
            }
        }

    override suspend fun getCurrentUser(): User? =
        withContext(Dispatchers.Default) {
            try {
                val dto = userApiService.getCurrentUser()
                val user = dto.toDomain()
                saveUserLocally(user)
                user
            } catch (e: Exception) {
                logger.warn(e) { "Error getting current user from API, trying local DB" }
                userLocalDataSource.getActiveUsers().firstOrNull()
            }
        }

    override suspend fun updateCurrentUser(user: User): Result<User> =
        withContext(Dispatchers.Default) {
            try {
                val request =
                    UpdateUserRequest(
                        email = user.email,
                        fullName = user.fullName,
                        password = null, // Password update should be separate endpoint
                    )
                val dto = userApiService.updateCurrentUser(request)
                val updatedUser = dto.toDomain()
                saveUserLocally(updatedUser)
                Result.success(updatedUser)
            } catch (e: Exception) {
                logger.error(e) { "Error updating current user: ${user.id}" }
                Result.failure(e)
            }
        }

    override suspend fun getUsers(
        page: Int,
        limit: Int,
        role: String?,
    ): PaginatedResult<User> =
        withContext(Dispatchers.Default) {
            try {
                val offset = (page - 1) * limit

                val allUsers =
                    if (role != null) {
                        userLocalDataSource.getUsersByRole(role)
                    } else {
                        userLocalDataSource.getUsers()
                    }

                val total = allUsers.size
                val paginatedUsers = allUsers.drop(offset).take(limit)

                PaginatedResult(
                    items = paginatedUsers,
                    total = total,
                    page = page,
                    limit = limit,
                    hasMore = offset + limit < total,
                )
            } catch (e: Exception) {
                logger.error(e) { "Error getting users from local DB" }
                PaginatedResult(emptyList(), 0, page, limit, false)
            }
        }

    override suspend fun getUserById(id: String): User? =
        withContext(Dispatchers.Default) {
            try {
                userLocalDataSource.getUserById(id)
                    ?: run {
                        try {
                            val dto = userApiService.getUserById(id)
                            val user = dto.toDomain()
                            saveUserLocally(user)
                            user
                        } catch (e: Exception) {
                            logger.warn(e) { "User not found: $id" }
                            null
                        }
                    }
            } catch (e: Exception) {
                logger.error(e) { "Error getting user by id: $id" }
                null
            }
        }

    override suspend fun updateUser(user: User): Result<User> =
        withContext(Dispatchers.Default) {
            try {
                userLocalDataSource.updateUser(user).onFailure { e ->
                    logger.error(e) { "Error updating user locally: ${user.id}" }
                }

                try {
                    val request =
                        UpdateUserRequest(
                            email = user.email,
                            fullName = user.fullName,
                            password = null,
                        )
                    val dto = userApiService.updateUser(user.id, request)
                    val updatedUser = dto.toDomain()
                    saveUserLocally(updatedUser)
                    Result.success(updatedUser)
                } catch (e: Exception) {
                    logger.warn(e) { "Error syncing user update to API, but saved locally" }
                    Result.success(user)
                }
            } catch (e: Exception) {
                logger.error(e) { "Error updating user: ${user.id}" }
                Result.failure(e)
            }
        }

    override suspend fun deleteUser(id: String): Result<Unit> =
        withContext(Dispatchers.Default) {
            try {
                userLocalDataSource.deleteUser(id).getOrElse { return@withContext Result.failure(it) }

                try {
                    userApiService.deleteUser(id)
                    Result.success(Unit)
                } catch (e: Exception) {
                    logger.warn(e) { "Error deleting user from API, but deleted locally" }
                    Result.success(Unit)
                }
            } catch (e: Exception) {
                logger.error(e) { "Error deleting user: $id" }
                Result.failure(e)
            }
        }

    override suspend fun refreshToken(refreshToken: String): Result<LoginResult> =
        withContext(Dispatchers.Default) {
            try {
                val response = userApiService.refreshToken(refreshToken)
                val user = response.user.toDomain()
                saveUserLocally(user)
                Result.success(
                    LoginResult(
                        user = user,
                        token = response.token,
                        refreshToken = response.refreshToken ?: "",
                        expiresIn = response.expiresIn,
                    ),
                )
            } catch (e: Exception) {
                logger.error(e) { "Error refreshing token" }
                Result.failure(e)
            }
        }

    private suspend fun saveUserLocally(user: User) {
        userLocalDataSource.saveUser(user).onFailure { e ->
            logger.error(e) { "Error saving user locally: ${user.id}" }
        }
    }

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
            isActive = isActive,
        )
    }
}
