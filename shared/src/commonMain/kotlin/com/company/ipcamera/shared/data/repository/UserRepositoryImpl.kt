package com.company.ipcamera.shared.data.repository

import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.core.network.api.UserApiService
import com.company.ipcamera.core.network.dto.*
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
 * Реализация UserRepository с использованием API сервиса
 */
class UserRepositoryImpl(
    private val userApiService: UserApiService
) : UserRepository {
    
    override suspend fun login(username: String, password: String): Result<LoginResult> = withContext(Dispatchers.Default) {
        try {
            val request = LoginRequest(username = username, password = password)
            val result = userApiService.login(request)
            result.fold(
                onSuccess = { response ->
                    Result.success(
                        LoginResult(
                            user = response.user.toDomain(),
                            token = response.token,
                            refreshToken = response.refreshToken,
                            expiresIn = response.expiresIn
                        )
                    )
                },
                onError = { error ->
                    logger.error(error) { "Error logging in user: $username" }
                    Result.failure(Exception(error.message))
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error logging in user: $username" }
            Result.failure(e)
        }
    }
    
    override suspend fun logout(): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val result = userApiService.logout()
            result.fold(
                onSuccess = { 
                    Result.success(Unit)
                },
                onError = { error ->
                    logger.error(error) { "Error logging out" }
                    Result.failure(Exception(error.message))
                }
            )
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
    ): Result<User> = withContext(Dispatchers.Default) {
        try {
            val request = RegisterRequest(username = username, email = email, password = password, fullName = fullName)
            val result = userApiService.register(request)
            result.fold(
                onSuccess = { response ->
                    response.user?.let { 
                        Result.success(it.toDomain())
                    } ?: Result.failure(Exception(response.message ?: "Registration failed"))
                },
                onError = { error ->
                    logger.error(error) { "Error registering user: $username" }
                    Result.failure(Exception(error.message))
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error registering user: $username" }
            Result.failure(e)
        }
    }
    
    override suspend fun getCurrentUser(): User? = withContext(Dispatchers.Default) {
        try {
            val result = userApiService.getCurrentUser()
            result.fold(
                onSuccess = { dto -> dto.toDomain() },
                onError = { error ->
                    logger.error(error) { "Error getting current user" }
                    null
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting current user" }
            null
        }
    }
    
    override suspend fun updateCurrentUser(user: User): Result<User> = withContext(Dispatchers.Default) {
        try {
            val request = UpdateUserRequest(
                email = user.email,
                fullName = user.fullName,
                password = null // Password update should be separate endpoint
            )
            val result = userApiService.updateCurrentUser(request)
            result.fold(
                onSuccess = { dto -> 
                    Result.success(dto.toDomain())
                },
                onError = { error ->
                    logger.error(error) { "Error updating current user: ${user.id}" }
                    Result.failure(Exception(error.message))
                }
            )
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
            val result = userApiService.getUsers(page, limit, role)
            result.fold(
                onSuccess = { paginatedResponse ->
                    PaginatedResult(
                        items = paginatedResponse.items.map { it.toDomain() },
                        total = paginatedResponse.total,
                        page = paginatedResponse.page,
                        limit = paginatedResponse.limit,
                        hasMore = paginatedResponse.hasMore
                    )
                },
                onError = { error ->
                    logger.error(error) { "Error getting users" }
                    PaginatedResult(emptyList(), 0, page, limit, false)
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting users" }
            PaginatedResult(emptyList(), 0, page, limit, false)
        }
    }
    
    override suspend fun getUserById(id: String): User? = withContext(Dispatchers.Default) {
        try {
            val result = userApiService.getUserById(id)
            result.fold(
                onSuccess = { dto -> dto.toDomain() },
                onError = { error ->
                    logger.error(error) { "Error getting user by id: $id" }
                    null
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error getting user by id: $id" }
            null
        }
    }
    
    override suspend fun updateUser(user: User): Result<User> = withContext(Dispatchers.Default) {
        try {
            val request = UpdateUserRequest(
                email = user.email,
                fullName = user.fullName,
                password = null // Password update should be separate endpoint
            )
            val result = userApiService.updateUser(user.id, request)
            result.fold(
                onSuccess = { dto -> 
                    Result.success(dto.toDomain())
                },
                onError = { error ->
                    logger.error(error) { "Error updating user: ${user.id}" }
                    Result.failure(Exception(error.message))
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error updating user: ${user.id}" }
            Result.failure(e)
        }
    }
    
    override suspend fun deleteUser(id: String): Result<Unit> = withContext(Dispatchers.Default) {
        try {
            val result = userApiService.deleteUser(id)
            result.fold(
                onSuccess = { 
                    Result.success(Unit)
                },
                onError = { error ->
                    logger.error(error) { "Error deleting user: $id" }
                    Result.failure(Exception(error.message))
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error deleting user: $id" }
            Result.failure(e)
        }
    }
    
    override suspend fun refreshToken(refreshToken: String): Result<LoginResult> = withContext(Dispatchers.Default) {
        try {
            val result = userApiService.refreshToken(refreshToken)
            result.fold(
                onSuccess = { response ->
                    Result.success(
                        LoginResult(
                            user = response.user.toDomain(),
                            token = response.token,
                            refreshToken = response.refreshToken,
                            expiresIn = response.expiresIn
                        )
                    )
                },
                onError = { error ->
                    logger.error(error) { "Error refreshing token" }
                    Result.failure(Exception(error.message))
                }
            )
        } catch (e: Exception) {
            logger.error(e) { "Error refreshing token" }
            Result.failure(e)
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
            isActive = isActive
        )
    }
}

