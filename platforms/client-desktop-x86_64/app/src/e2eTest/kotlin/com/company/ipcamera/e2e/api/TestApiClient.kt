@file:Suppress("UNCHECKED_CAST")

package com.company.ipcamera.e2e.api

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.e2e.data.TestDataFactory
import com.company.ipcamera.shared.domain.model.*
import io.ktor.client.*
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

/**
 * API клиент для E2E тестов.
 * Использует bodyAsText() вместо deprecated readText()
 */
class TestApiClient(
    private val baseUrl: String = "http://localhost:8080/api/v1",
    private val adminUsername: String = "admin",
    private val adminPassword: String = "admin123"
) {
    private var httpClient: HttpClient? = null
    private var jwtToken: String? = null
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun initialize() {
        httpClient = HttpClient {
            install(ContentNegotiation) { json(json) }
        }
        runBlocking { login(adminUsername, adminPassword) }
    }

    private suspend fun login(username: String, password: String): String {
        val client = httpClient ?: throw IllegalStateException("HttpClient not initialized")
        val response = client.post("$baseUrl/auth/login") {
            contentType(ContentType.Application.Json)
            setBody(mapOf("username" to username, "password" to password))
        }
        
        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Login failed: ${response.status}")
        }
        
        val body = response.body<Map<String, String>>()
        jwtToken = body["token"] ?: throw IllegalStateException("No token in response")
        return jwtToken!!
    }

    suspend fun createCamera(camera: Camera): Camera {
        require(jwtToken != null) { "Must login first" }
        val client = httpClient ?: throw IllegalStateException("HttpClient not initialized")
        
        val response = client.post("$baseUrl/cameras") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $jwtToken")
            setBody(mapOf(
                "name" to camera.name, "url" to camera.url,
                "username" to camera.username, "password" to camera.password,
                "codec" to camera.codec, "audio" to camera.audio
            ))
        }
        
        if (response.status !in listOf(HttpStatusCode.Created, HttpStatusCode.OK)) {
            throw IllegalStateException("Create camera failed: ${response.status}")
        }
        
        return parseCameraFromResponse(response.body<Map<String, Any?>>())
    }

    suspend fun getCameras(): List<Camera> {
        require(jwtToken != null) { "Must login first" }
        val client = httpClient ?: throw IllegalStateException("HttpClient not initialized")
        
        val response = client.get("$baseUrl/cameras") {
            header("Authorization", "Bearer $jwtToken")
        }
        
        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Get cameras failed: ${response.status}")
        }
        
        val body = response.body<Map<String, Any?>>()
        val camerasJson = body["cameras"] as? List<Map<String, Any?>> ?: emptyList()
        return camerasJson.map { parseCameraFromResponse(it) }
    }

    suspend fun updateCamera(cameraId: String, updates: Map<String, Any>): Camera {
        require(jwtToken != null) { "Must login first" }
        val client = httpClient ?: throw IllegalStateException("HttpClient not initialized")
        
        val response = client.put("$baseUrl/cameras/$cameraId") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $jwtToken")
            setBody(updates)
        }
        
        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Update camera failed: ${response.status}")
        }
        
        return parseCameraFromResponse(response.body<Map<String, Any?>>())
    }

    suspend fun deleteCamera(cameraId: String) {
        require(jwtToken != null) { "Must login first" }
        val client = httpClient ?: throw IllegalStateException("HttpClient not initialized")
        
        val response = client.delete("$baseUrl/cameras/$cameraId") {
            header("Authorization", "Bearer $jwtToken")
        }
        
        if (response.status !in listOf(HttpStatusCode.OK, HttpStatusCode.NoContent)) {
            throw IllegalStateException("Delete camera failed: ${response.status}")
        }
    }

    suspend fun createUser(user: User): User {
        require(jwtToken != null) { "Must login first" }
        val client = httpClient ?: throw IllegalStateException("HttpClient not initialized")
        
        val response = client.post("$baseUrl/users") {
            contentType(ContentType.Application.Json)
            header("Authorization", "Bearer $jwtToken")
            setBody(mapOf(
                "username" to user.username, "email" to user.email,
                "password" to "TestPass123!",
                "role" to TestDataRole.userRoleToString(user.role)
            ))
        }
        
        if (response.status !in listOf(HttpStatusCode.Created, HttpStatusCode.OK)) {
            throw IllegalStateException("Create user failed: ${response.status}")
        }
        
        return parseUserFromResponse(response.body<Map<String, Any?>>())
    }

    suspend fun deleteUser(userId: String) {
        require(jwtToken != null) { "Must login first" }
        val client = httpClient ?: throw IllegalStateException("HttpClient not initialized")
        
        val response = client.delete("$baseUrl/users/$userId") {
            header("Authorization", "Bearer $jwtToken")
        }
        
        if (response.status !in listOf(HttpStatusCode.OK, HttpStatusCode.NoContent)) {
            throw IllegalStateException("Delete user failed: ${response.status}")
        }
    }

    suspend fun getRecordings(cameraId: String? = null): List<Recording> {
        require(jwtToken != null) { "Must login first" }
        val client = httpClient ?: throw IllegalStateException("HttpClient not initialized")
        
        val url = if (cameraId != null) "$baseUrl/recordings?cameraId=$cameraId" else "$baseUrl/recordings"
        val response = client.get(url) { header("Authorization", "Bearer $jwtToken") }
        
        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Get recordings failed: ${response.status}")
        }
        
        val body = response.body<Map<String, Any?>>()
        val recordingsJson = body["recordings"] as? List<Map<String, Any?>> ?: emptyList()
        return recordingsJson.map { parseRecordingFromResponse(it) }
    }

    suspend fun cleanupTestData() {
        require(jwtToken != null) { "Must login first" }
        try {
            getCameras().filter { it.name.startsWith("E2E") || it.name.startsWith("Test") }.forEach {
                try { deleteCamera(it.id) } catch (_: Exception) {}
            }
            getUsers().filter { it.username.startsWith("e2e_") || it.username.startsWith("test_") }.forEach {
                try { deleteUser(it.id) } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    private suspend fun getUsers(): List<User> {
        require(jwtToken != null) { "Must login first" }
        val client = httpClient ?: throw IllegalStateException("HttpClient not initialized")
        
        val response = client.get("$baseUrl/users") { header("Authorization", "Bearer $jwtToken") }
        if (response.status != HttpStatusCode.OK) {
            throw IllegalStateException("Get users failed: ${response.status}")
        }
        
        val body = response.body<Map<String, Any?>>()
        val usersJson = body["users"] as? List<Map<String, Any?>> ?: emptyList()
        return usersJson.map { parseUserFromResponse(it) }
    }

    private fun parseCameraFromResponse(data: Map<String, Any?>): Camera {
        return Camera(
            id = (data["id"] as? String) ?: "",
            name = (data["name"] as? String) ?: "",
            url = (data["url"] as? String) ?: "",
            username = data["username"] as? String,
            password = data["password"] as? String,
            model = data["model"] as? String,
            status = CameraStatus.UNKNOWN,
            resolution = null,
            fps = (data["fps"] as? Int) ?: 25,
            bitrate = (data["bitrate"] as? Int) ?: 4096,
            codec = (data["codec"] as? String) ?: "H.264",
            audio = (data["audio"] as? Boolean) ?: false,
            streams = emptyList(),
            settings = TestDataFactory.createCameraSettings(),
            statistics = null,
            createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis(),
            updatedAt = (data["updatedAt"] as? Long) ?: System.currentTimeMillis(),
            lastSeen = data["lastSeen"] as? Long
        )
    }

    private fun parseUserFromResponse(data: Map<String, Any?>): User {
        return User(
            id = (data["id"] as? String) ?: "",
            username = (data["username"] as? String) ?: "",
            email = data["email"] as? String,
            fullName = data["fullName"] as? String,
            role = TestDataRole.stringToUserRole((data["role"] as? String) ?: "VIEWER"),
            permissions = (data["permissions"] as? List<*>)?.map { it.toString() } ?: emptyList(),
            createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis(),
            lastLoginAt = data["lastLoginAt"] as? Long,
            isActive = (data["isActive"] as? Boolean) ?: true
        )
    }

    private fun parseRecordingFromResponse(data: Map<String, Any?>): Recording {
        return Recording(
            id = (data["id"] as? String) ?: "",
            cameraId = (data["cameraId"] as? String) ?: "",
            cameraName = data["cameraName"] as? String,
            startTime = (data["startTime"] as? Long) ?: 0,
            endTime = data["endTime"] as? Long,
            duration = (data["duration"] as? Long) ?: 0,
            filePath = data["filePath"] as? String,
            fileSize = data["fileSize"] as? Long,
            codec = data["codec"] as? String,
            format = RecordingFormat.MP4,
            quality = Quality.HIGH,
            status = RecordingStatus.COMPLETED,
            thumbnailUrl = data["thumbnailUrl"] as? String,
            createdAt = (data["createdAt"] as? Long) ?: System.currentTimeMillis()
        )
    }

    fun close() { httpClient?.close() }
}

object TestDataRole {
    fun userRoleToString(role: UserRole): String = when (role) {
        UserRole.ADMIN -> "ADMIN"; UserRole.OPERATOR -> "OPERATOR"
        UserRole.VIEWER -> "VIEWER"; UserRole.GUEST -> "GUEST"
    }
    fun stringToUserRole(role: String): UserRole = when (role.uppercase()) {
        "ADMIN" -> UserRole.ADMIN; "OPERATOR" -> UserRole.OPERATOR
        "VIEWER" -> UserRole.VIEWER; "GUEST" -> UserRole.GUEST
        else -> UserRole.VIEWER
    }
}
