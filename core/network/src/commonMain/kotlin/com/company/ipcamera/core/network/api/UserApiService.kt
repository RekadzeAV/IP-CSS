package com.company.ipcamera.core.network.api

import com.company.ipcamera.core.network.ApiClient
import com.company.ipcamera.core.network.dto.*
import kotlinx.serialization.builtins.serializer

/**
 * API сервис для работы с пользователями
 */
class UserApiService(
    private val apiClient: ApiClient,
    private val basePath: String = "/api/v1/users"
) {
    
    /**
     * Вход в систему
     * 
     * @param request данные для входа
     * @return токен доступа и информация о пользователе
     */
    suspend fun login(request: LoginRequest): LoginResponse {
        return apiClient.post(
            path = "/api/v1/auth/login",
            body = request,
            responseType = LoginResponse.serializer()
        )
    }
    
    /**
     * Регистрация нового пользователя
     * 
     * @param request данные для регистрации
     * @return результат регистрации
     */
    suspend fun register(request: RegisterRequest): RegisterResponse {
        return apiClient.post(
            path = "/api/v1/auth/register",
            body = request,
            responseType = RegisterResponse.serializer()
        )
    }
    
    /**
     * Выход из системы
     */
    suspend fun logout(): ApiResponse<Unit> {
        return apiClient.post(
            path = "/api/v1/auth/logout",
            responseType = ApiResponse.serializer(Unit.serializer())
        )
    }
    
    /**
     * Получить информацию о текущем пользователе
     * 
     * @return информация о пользователе
     */
    suspend fun getCurrentUser(): UserResponse {
        return apiClient.get(
            path = "$basePath/me",
            responseType = UserResponse.serializer()
        )
    }
    
    /**
     * Обновить профиль текущего пользователя
     * 
     * @param request данные для обновления
     * @return обновленная информация о пользователе
     */
    suspend fun updateCurrentUser(request: UpdateUserRequest): UserResponse {
        return apiClient.put(
            path = "$basePath/me",
            body = request,
            responseType = UserResponse.serializer()
        )
    }
    
    /**
     * Получить список пользователей (только для администраторов)
     * 
     * @param page номер страницы
     * @param limit количество элементов на странице
     * @param role фильтр по роли
     * @return список пользователей с пагинацией
     */
    suspend fun getUsers(
        page: Int = 1,
        limit: Int = 20,
        role: String? = null
    ): PaginatedResponse<UserResponse> {
        val queryParams = mutableMapOf<String, String>()
        queryParams["page"] = page.toString()
        queryParams["limit"] = limit.toString()
        role?.let { queryParams["role"] = it }
        
        return apiClient.get(
            path = basePath,
            queryParameters = queryParams,
            responseType = PaginatedResponse.serializer(UserResponse.serializer())
        )
    }
    
    /**
     * Получить пользователя по ID (только для администраторов)
     * 
     * @param id идентификатор пользователя
     * @return информация о пользователе
     */
    suspend fun getUserById(id: String): UserResponse {
        return apiClient.get(
            path = "$basePath/$id",
            responseType = UserResponse.serializer()
        )
    }
    
    /**
     * Обновить пользователя (только для администраторов)
     * 
     * @param id идентификатор пользователя
     * @param request данные для обновления
     * @return обновленная информация о пользователе
     */
    suspend fun updateUser(id: String, request: UpdateUserRequest): UserResponse {
        return apiClient.put(
            path = "$basePath/$id",
            body = request,
            responseType = UserResponse.serializer()
        )
    }
    
    /**
     * Удалить пользователя (только для администраторов)
     * 
     * @param id идентификатор пользователя
     */
    suspend fun deleteUser(id: String): ApiResponse<Unit> {
        return apiClient.delete(
            path = "$basePath/$id",
            responseType = ApiResponse.serializer(Unit.serializer())
        )
    }
    
    /**
     * Обновить токен доступа
     * 
     * @param refreshToken токен обновления
     * @return новый токен доступа
     */
    suspend fun refreshToken(refreshToken: String): LoginResponse {
        @kotlinx.serialization.Serializable
        data class RefreshTokenRequest(val refreshToken: String)
        
        return apiClient.post(
            path = "/api/v1/auth/refresh",
            body = RefreshTokenRequest(refreshToken),
            responseType = LoginResponse.serializer()
        )
    }
}

