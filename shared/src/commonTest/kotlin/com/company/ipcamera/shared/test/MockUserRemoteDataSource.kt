package com.company.ipcamera.shared.test

import com.company.ipcamera.core.network.ApiError
import com.company.ipcamera.core.network.ApiResult
import com.company.ipcamera.shared.data.datasource.remote.UserRemoteDataSource
import com.company.ipcamera.shared.domain.model.User

/**
 * Mock реализация UserRemoteDataSource для тестов
 */
class MockUserRemoteDataSource(
    private var users: MutableList<User> = mutableListOf(),
) : UserRemoteDataSource {
    var shouldFailOnGet: Boolean = false
    var shouldFailOnCreate: Boolean = false
    var shouldFailOnUpdate: Boolean = false
    var networkError: Exception? = null

    private fun apiErr(): ApiError = (networkError ?: Exception("Mock network error")).let { ApiError.UnknownError(it) }

    override suspend fun getUsers(): ApiResult<List<User>> {
        return if (shouldFailOnGet) {
            ApiResult.Error(apiErr())
        } else {
            ApiResult.Success(users.toList())
        }
    }

    override suspend fun getUserById(id: String): ApiResult<User> {
        return if (shouldFailOnGet) {
            ApiResult.Error(apiErr())
        } else {
            val user = users.find { it.id == id }
            if (user != null) {
                ApiResult.Success(user)
            } else {
                ApiResult.Error(ApiError.UnknownError(Exception("User not found: $id")))
            }
        }
    }

    override suspend fun getCurrentUser(): ApiResult<User> {
        return if (shouldFailOnGet) {
            ApiResult.Error(apiErr())
        } else {
            val user = users.firstOrNull()
            if (user != null) {
                ApiResult.Success(user)
            } else {
                ApiResult.Error(ApiError.UnknownError(Exception("No current user")))
            }
        }
    }

    override suspend fun createUser(user: User): ApiResult<User> {
        return if (shouldFailOnCreate) {
            ApiResult.Error(apiErr())
        } else {
            users.add(user)
            ApiResult.Success(user)
        }
    }

    override suspend fun updateUser(
        id: String,
        user: User,
    ): ApiResult<User> {
        return if (shouldFailOnUpdate) {
            ApiResult.Error(apiErr())
        } else {
            val index = users.indexOfFirst { it.id == id }
            if (index >= 0) {
                users[index] = user
                ApiResult.Success(user)
            } else {
                ApiResult.Error(ApiError.UnknownError(Exception("User not found: $id")))
            }
        }
    }

    override suspend fun deleteUser(id: String): ApiResult<Unit> {
        users.removeIf { it.id == id }
        return ApiResult.Success(Unit)
    }

    fun clear() {
        users.clear()
    }

    fun addUserDirectly(user: User) {
        users.add(user)
    }
}
