package com.company.ipcamera.shared.data.datasource.remote

import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.shared.domain.model.User

/**
 * Удаленный источник данных для пользователей.
 * Отвечает за работу с REST API.
 */
interface UserRemoteDataSource {
    /**
     * Получить список пользователей с сервера
     */
    suspend fun getUsers(): ApiResult<List<User>>

    /**
     * Получить пользователя по ID с сервера
     */
    suspend fun getUserById(id: String): ApiResult<User>

    /**
     * Получить текущего пользователя (me)
     */
    suspend fun getCurrentUser(): ApiResult<User>

    /**
     * Создать пользователя на сервере
     */
    suspend fun createUser(user: User): ApiResult<User>

    /**
     * Обновить пользователя на сервере
     */
    suspend fun updateUser(id: String, user: User): ApiResult<User>

    /**
     * Удалить пользователя с сервера
     */
    suspend fun deleteUser(id: String): ApiResult<Unit>
}

