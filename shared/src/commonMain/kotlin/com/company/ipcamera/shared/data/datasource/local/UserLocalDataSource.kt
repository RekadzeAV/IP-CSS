package com.company.ipcamera.shared.data.datasource.local

import com.company.ipcamera.shared.domain.model.User

/**
 * Локальный источник данных для пользователей.
 * Отвечает за работу с локальной базой данных SQLite.
 */
interface UserLocalDataSource {
    /**
     * Получить всех пользователей из локальной БД
     */
    suspend fun getUsers(): List<User>

    /**
     * Получить пользователя по ID из локальной БД
     */
    suspend fun getUserById(id: String): User?

    /**
     * Получить пользователя по username из локальной БД
     */
    suspend fun getUserByUsername(username: String): User?

    /**
     * Получить пользователей по роли
     */
    suspend fun getUsersByRole(role: String): List<User>

    /**
     * Получить активных пользователей
     */
    suspend fun getActiveUsers(): List<User>

    /**
     * Сохранить пользователя в локальную БД
     */
    suspend fun saveUser(user: User): Result<User>

    /**
     * Сохранить список пользователей в локальную БД (batch операция)
     */
    suspend fun saveUsers(users: List<User>): Result<List<User>>

    /**
     * Обновить пользователя в локальной БД
     */
    suspend fun updateUser(user: User): Result<User>

    /**
     * Обновить последний вход пользователя
     */
    suspend fun updateUserLastLogin(id: String, timestamp: Long): Result<Unit>

    /**
     * Обновить статус активности пользователя
     */
    suspend fun updateUserStatus(id: String, isActive: Boolean): Result<Unit>

    /**
     * Удалить пользователя из локальной БД
     */
    suspend fun deleteUser(id: String): Result<Unit>

    /**
     * Удалить всех пользователей из локальной БД
     */
    suspend fun deleteAllUsers(): Result<Unit>

    /**
     * Проверить существование пользователя
     */
    suspend fun userExists(id: String): Boolean

    /**
     * Проверить существование пользователя по username
     */
    suspend fun userExistsByUsername(username: String): Boolean
}

