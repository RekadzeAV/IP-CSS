package com.company.ipcamera.shared.test

import com.company.ipcamera.shared.data.datasource.local.UserLocalDataSource
import com.company.ipcamera.shared.domain.model.User

/**
 * Mock реализация UserLocalDataSource для тестов
 */
class MockUserLocalDataSource(
    private var users: MutableList<User> = mutableListOf(),
) : UserLocalDataSource {
    var shouldFailOnSave: Boolean = false
    var shouldFailOnUpdate: Boolean = false
    var shouldFailOnDelete: Boolean = false

    override suspend fun getUsers(): List<User> = users.toList()

    override suspend fun getUserById(id: String): User? = users.find { it.id == id }

    override suspend fun getUserByUsername(username: String): User? {
        return users.find { it.username == username }
    }

    override suspend fun getUsersByRole(role: String): List<User> {
        return users.filter { it.role.name.equals(role, ignoreCase = true) }
    }

    override suspend fun getActiveUsers(): List<User> {
        return users.filter { it.isActive }
    }

    override suspend fun saveUser(user: User): Result<User> {
        return if (shouldFailOnSave) {
            Result.failure(Exception("Mock save failure"))
        } else {
            users.add(user)
            Result.success(user)
        }
    }

    override suspend fun saveUsers(users: List<User>): Result<List<User>> {
        return if (shouldFailOnSave) {
            Result.failure(Exception("Mock save failure"))
        } else {
            this.users.addAll(users)
            Result.success(users)
        }
    }

    override suspend fun updateUser(user: User): Result<User> {
        return if (shouldFailOnUpdate) {
            Result.failure(Exception("Mock update failure"))
        } else {
            val index = users.indexOfFirst { it.id == user.id }
            if (index >= 0) {
                users[index] = user
                Result.success(user)
            } else {
                Result.failure(Exception("User not found: ${user.id}"))
            }
        }
    }

    override suspend fun updateUserLastLogin(
        id: String,
        timestamp: Long,
    ): Result<Unit> {
        val user = users.find { it.id == id }
        return if (user != null) {
            val updated = user.copy(lastLoginAt = timestamp)
            val index = users.indexOfFirst { it.id == id }
            users[index] = updated
            Result.success(Unit)
        } else {
            Result.failure(Exception("User not found: $id"))
        }
    }

    override suspend fun updateUserStatus(
        id: String,
        isActive: Boolean,
    ): Result<Unit> {
        val user = users.find { it.id == id }
        return if (user != null) {
            val updated = user.copy(isActive = isActive)
            val index = users.indexOfFirst { it.id == id }
            users[index] = updated
            Result.success(Unit)
        } else {
            Result.failure(Exception("User not found: $id"))
        }
    }

    override suspend fun deleteUser(id: String): Result<Unit> {
        return if (shouldFailOnDelete) {
            Result.failure(Exception("Mock delete failure"))
        } else {
            users.removeIf { it.id == id }
            Result.success(Unit)
        }
    }

    override suspend fun deleteAllUsers(): Result<Unit> {
        users.clear()
        return Result.success(Unit)
    }

    override suspend fun userExists(id: String): Boolean {
        return users.any { it.id == id }
    }

    override suspend fun userExistsByUsername(username: String): Boolean {
        return users.any { it.username == username }
    }

    fun clear() {
        users.clear()
    }

    fun addUserDirectly(user: User) {
        users.add(user)
    }
}
