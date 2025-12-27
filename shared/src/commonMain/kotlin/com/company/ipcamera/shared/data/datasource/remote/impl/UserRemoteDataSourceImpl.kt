package com.company.ipcamera.shared.data.datasource.remote.impl

import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.core.network.api.UserApiService
import com.company.ipcamera.core.network.dto.*
import com.company.ipcamera.shared.data.datasource.remote.UserRemoteDataSource
import com.company.ipcamera.shared.domain.model.User
import com.company.ipcamera.shared.domain.model.UserRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Реализация UserRemoteDataSource с использованием UserApiService
 */
class UserRemoteDataSourceImpl(
    private val userApiService: UserApiService
) : UserRemoteDataSource {

    override suspend fun getUsers(): ApiResult<List<User>> = withContext(Dispatchers.IO) {
        try {
            // UserApiService.getUsers возвращает PaginatedResponse, получаем первую страницу
            val paginatedResponse = userApiService.getUsers(page = 1, limit = 1000)
            val users = paginatedResponse.items.map { it.toDomain() }
            ApiResult.Success(users)
        } catch (e: Exception) {
            logger.error(e) { "Error getting users from remote API" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun getUserById(id: String): ApiResult<User> = withContext(Dispatchers.IO) {
        try {
            val response = userApiService.getUserById(id)
            ApiResult.Success(response.toDomain())
        } catch (e: Exception) {
            logger.error(e) { "Error getting user by id from remote API: $id" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun getCurrentUser(): ApiResult<User> = withContext(Dispatchers.IO) {
        try {
            val response = userApiService.getCurrentUser()
            ApiResult.Success(response.toDomain())
        } catch (e: Exception) {
            logger.error(e) { "Error getting current user from remote API" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun createUser(user: User): ApiResult<User> = withContext(Dispatchers.IO) {
        try {
            val request = RegisterRequest(
                username = user.username,
                email = user.email ?: "",
                password = "", // Пароль не передается в User модели
                fullName = user.fullName
            )
            val response = userApiService.register(request)
            response.user?.let { userResponse ->
                ApiResult.Success(userResponse.toDomain())
            } ?: ApiResult.Error(
                com.company.ipcamera.core.network.ApiError.UnknownError(
                    Exception(response.message ?: "Registration failed")
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Error creating user on remote API: ${user.id}" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun updateUser(id: String, user: User): ApiResult<User> = withContext(Dispatchers.IO) {
        try {
            val request = UpdateUserRequest(
                email = user.email,
                fullName = user.fullName,
                password = null // Password update should be separate
            )
            val response = userApiService.updateUser(id, request)
            ApiResult.Success(response.toDomain())
        } catch (e: Exception) {
            logger.error(e) { "Error updating user on remote API: $id" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
        }
    }

    override suspend fun deleteUser(id: String): ApiResult<Unit> = withContext(Dispatchers.IO) {
        try {
            userApiService.deleteUser(id)
            ApiResult.Success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting user from remote API: $id" }
            ApiResult.Error(com.company.ipcamera.core.network.ApiError.UnknownError(e))
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

