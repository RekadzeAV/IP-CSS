package com.company.ipcamera.core.network.api

import com.company.ipcamera.core.network.ApiClient
import com.company.ipcamera.core.network.dto.*

/**
 * API сервис для работы с пользователями
 */
class UserApiService(
    private val apiClient: ApiClient,
    private val basePath: String = "/api/v1/users"
) {

    private suspend inline fun <T> com.company.ipcamera.core.network.ApiResult<T>.getOrThrow(): T = fold(
        onSuccess = { it },
        onError = { throw it }
    )

    suspend fun login(request: LoginRequest): LoginResponse {
        return apiClient.post<LoginRequest, LoginResponse>(path = "/api/v1/auth/login", body = request).getOrThrow()
    }

    suspend fun register(request: RegisterRequest): RegisterResponse {
        return apiClient.post<RegisterRequest, RegisterResponse>(path = "/api/v1/auth/register", body = request).getOrThrow()
    }

    suspend fun logout(): ApiResponse<Unit> {
        return apiClient.post<Unit, ApiResponse<Unit>>(path = "/api/v1/auth/logout").getOrThrow()
    }

    suspend fun getCurrentUser(): UserResponse {
        return apiClient.get<UserResponse>(path = "$basePath/me").getOrThrow()
    }

    suspend fun updateCurrentUser(request: UpdateUserRequest): UserResponse {
        return apiClient.put<UpdateUserRequest, UserResponse>(path = "$basePath/me", body = request).getOrThrow()
    }

    suspend fun getUsers(
        page: Int = 1,
        limit: Int = 20,
        role: String? = null
    ): PaginatedResponse<UserResponse> {
        val queryParams = mutableMapOf<String, String>()
        queryParams["page"] = page.toString()
        queryParams["limit"] = limit.toString()
        role?.let { queryParams["role"] = it }
        return apiClient.get<PaginatedResponse<UserResponse>>(path = basePath, queryParameters = queryParams).getOrThrow()
    }

    suspend fun getUserById(id: String): UserResponse {
        return apiClient.get<UserResponse>(path = "$basePath/$id").getOrThrow()
    }

    suspend fun updateUser(id: String, request: UpdateUserRequest): UserResponse {
        return apiClient.put<UpdateUserRequest, UserResponse>(path = "$basePath/$id", body = request).getOrThrow()
    }

    suspend fun deleteUser(id: String): ApiResponse<Unit> {
        return apiClient.delete<ApiResponse<Unit>>(path = "$basePath/$id").getOrThrow()
    }

    suspend fun refreshToken(refreshToken: String = ""): LoginResponse {
        @kotlinx.serialization.Serializable
        data class RefreshTokenRequest(val refreshToken: String = "")
        return apiClient.post<RefreshTokenRequest, LoginResponse>(
            path = "/api/v1/auth/refresh",
            body = RefreshTokenRequest(refreshToken)
        ).getOrThrow()
    }
}
