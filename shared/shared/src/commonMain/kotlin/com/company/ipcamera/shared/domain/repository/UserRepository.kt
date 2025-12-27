package com.company.ipcamera.shared.domain.repository

import com.company.ipcamera.shared.domain.model.User

/**
 * Репозиторий для работы с пользователями
 */
interface UserRepository {
    /**
     * Войти в систему
     */
    suspend fun login(username: String, password: String): Result<LoginResult>
    
    /**
     * Выйти из системы
     */
    suspend fun logout(): Result<Unit>
    
    /**
     * Зарегистрировать нового пользователя
     */
    suspend fun register(
        username: String,
        email: String,
        password: String,
        fullName: String? = null
    ): Result<User>
    
    /**
     * Получить текущего пользователя
     */
    suspend fun getCurrentUser(): User?
    
    /**
     * Обновить профиль текущего пользователя
     */
    suspend fun updateCurrentUser(user: User): Result<User>
    
    /**
     * Получить всех пользователей
     */
    suspend fun getUsers(
        page: Int = 1,
        limit: Int = 20,
        role: String? = null
    ): PaginatedResult<User>
    
    /**
     * Получить пользователя по ID
     */
    suspend fun getUserById(id: String): User?
    
    /**
     * Обновить пользователя
     */
    suspend fun updateUser(user: User): Result<User>
    
    /**
     * Удалить пользователя
     */
    suspend fun deleteUser(id: String): Result<Unit>
    
    /**
     * Обновить токен доступа
     */
    suspend fun refreshToken(refreshToken: String): Result<LoginResult>
}

/**
 * Результат входа в систему
 */
data class LoginResult(
    val user: User,
    val token: String,
    val refreshToken: String? = null,
    val expiresIn: Long
)



