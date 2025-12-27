package com.company.ipcamera.server.repository

import com.company.ipcamera.server.service.PasswordService
import com.company.ipcamera.shared.domain.model.User
import com.company.ipcamera.shared.domain.model.UserRole
import com.company.ipcamera.shared.domain.repository.LoginResult
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Серверная реализация UserRepository
 * 
 * TODO: Мигрировать на SQLDelight/PostgreSQL для продакшена
 * Сейчас используется in-memory хранилище для MVP
 */
class ServerUserRepository {
    private val users = mutableMapOf<String, User>()
    private val passwordHashes = mutableMapOf<String, String>() // userId -> hashedPassword
    private val refreshTokens = mutableMapOf<String, String>() // refreshToken -> userId
    private val mutex = Mutex()
    
    init {
        // Создаем дефолтного администратора для MVP
        // В продакшене это должно быть через миграцию БД или переменные окружения
        createDefaultAdmin()
    }
    
    /**
     * Создает дефолтного администратора
     * Пароль: admin (должен быть изменен при первом входе!)
     */
    private fun createDefaultAdmin() {
        val adminId = UUID.randomUUID().toString()
        val adminPassword = System.getenv("ADMIN_PASSWORD") ?: "admin"
        val hashedPassword = PasswordService.hashPassword(adminPassword)
        
        val admin = User(
            id = adminId,
            username = "admin",
            email = "admin@example.com",
            fullName = "Administrator",
            role = UserRole.ADMIN,
            permissions = listOf("*"), // Все разрешения
            createdAt = System.currentTimeMillis(),
            lastLoginAt = null,
            isActive = true
        )
        
        users[adminId] = admin
        passwordHashes[adminId] = hashedPassword
        
        logger.warn { "Default admin user created. Username: admin, Password: $adminPassword. CHANGE THIS IN PRODUCTION!" }
    }
    
    /**
     * Аутентификация пользователя
     */
    suspend fun authenticate(username: String, password: String): User? = mutex.withLock {
        val user = users.values.find { it.username == username && it.isActive }
        if (user == null) {
            logger.warn { "Authentication failed: user not found or inactive: $username" }
            return null
        }
        
        val hashedPassword = passwordHashes[user.id]
        if (hashedPassword == null) {
            logger.error { "User ${user.id} has no password hash" }
            return null
        }
        
        if (!PasswordService.verifyPassword(password, hashedPassword)) {
            logger.warn { "Authentication failed: invalid password for user: $username" }
            return null
        }
        
        // Обновляем время последнего входа
        val updatedUser = user.copy(lastLoginAt = System.currentTimeMillis())
        users[user.id] = updatedUser
        
        logger.info { "User authenticated successfully: $username" }
        return updatedUser
    }
    
    /**
     * Получение пользователя по ID
     */
    suspend fun getUserById(id: String): User? = mutex.withLock {
        return users[id]
    }
    
    /**
     * Получение пользователя по username
     */
    suspend fun getUserByUsername(username: String): User? = mutex.withLock {
        return users.values.find { it.username == username }
    }
    
    /**
     * Сохранение refresh token
     */
    suspend fun saveRefreshToken(refreshToken: String, userId: String) = mutex.withLock {
        refreshTokens[refreshToken] = userId
    }
    
    /**
     * Получение userId по refresh token
     */
    suspend fun getUserIdByRefreshToken(refreshToken: String): String? = mutex.withLock {
        return refreshTokens[refreshToken]
    }
    
    /**
     * Удаление refresh token
     */
    suspend fun revokeRefreshToken(refreshToken: String) = mutex.withLock {
        refreshTokens.remove(refreshToken)
    }
    
    /**
     * Создание нового пользователя (для регистрации)
     */
    suspend fun createUser(
        username: String,
        email: String?,
        password: String,
        fullName: String?,
        role: UserRole = UserRole.VIEWER
    ): User {
        return mutex.withLock {
            // Проверяем, что пользователь с таким username не существует
            if (users.values.any { it.username == username }) {
                throw IllegalArgumentException("User with username $username already exists")
            }
            
            val userId = UUID.randomUUID().toString()
            val hashedPassword = PasswordService.hashPassword(password)
            
            val user = User(
                id = userId,
                username = username,
                email = email,
                fullName = fullName,
                role = role,
                permissions = emptyList(),
                createdAt = System.currentTimeMillis(),
                lastLoginAt = null,
                isActive = true
            )
            
            users[userId] = user
            passwordHashes[userId] = hashedPassword
            
            logger.info { "User created: $username (id: $userId)" }
            user
        }
    }
    
    /**
     * Получение всех пользователей (для администрирования)
     */
    suspend fun getAllUsers(): List<User> = mutex.withLock {
        return users.values.toList()
    }
    
    /**
     * Обновление пользователя
     */
    suspend fun updateUser(user: User): Result<User> = mutex.withLock {
        try {
            if (!users.containsKey(user.id)) {
                return Result.failure(IllegalArgumentException("User not found: ${user.id}"))
            }
            users[user.id] = user
            logger.info { "User updated: ${user.username} (id: ${user.id})" }
            Result.success(user)
        } catch (e: Exception) {
            logger.error(e) { "Error updating user: ${user.id}" }
            Result.failure(e)
        }
    }
    
    /**
     * Удаление пользователя
     */
    suspend fun deleteUser(id: String): Result<Unit> = mutex.withLock {
        try {
            if (!users.containsKey(id)) {
                return Result.failure(IllegalArgumentException("User not found: $id"))
            }
            users.remove(id)
            passwordHashes.remove(id)
            logger.info { "User deleted: $id" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error deleting user: $id" }
            Result.failure(e)
        }
    }
}

