package com.company.ipcamera.e2e.data

import com.company.ipcamera.shared.domain.model.*
import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.core.common.model.Resolution

/**
 * Factory для создания тестовых данных
 */
object TestDataFactory {

    fun createUniqueName(prefix: String = "e2e"): String {
        return "${prefix}_${System.currentTimeMillis()}"
    }

    fun createUser(
        username: String? = null,
        email: String? = null,
        role: UserRole = UserRole.OPERATOR,
        permissions: List<String> = emptyList()
    ): User {
        return User(
            id = "user_${System.currentTimeMillis()}",
            username = username ?: createUniqueName("user"),
            email = email ?: "${username ?: "test"}@example.com",
            fullName = "Test User",
            role = role,
            permissions = permissions,
            createdAt = System.currentTimeMillis(),
            lastLoginAt = null,
            isActive = true
        )
    }

    fun createCamera(
        name: String? = null,
        url: String = "rtsp://127.0.0.1:8554/test",
        username: String? = "admin",
        password: String? = "password",
        codec: String = "H.264",
        audio: Boolean = false,
        resolution: Resolution? = Resolution(1920, 1080)
    ): Camera {
        return Camera(
            id = "camera_${System.currentTimeMillis()}",
            name = name ?: createUniqueName("camera"),
            url = url,
            username = username,
            password = password,
            model = "Test Model",
            status = CameraStatus.OFFLINE,
            resolution = resolution,
            fps = 25,
            bitrate = 4096,
            codec = codec,
            audio = audio,
            streams = emptyList(),
            settings = createCameraSettings(),
            statistics = null,
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastSeen = null
        )
    }

    fun createCameraSettings(): CameraSettings {
        return CameraSettings(
            recording = RecordingSettings(
                enabled = true,
                mode = RecordingMode.CONTINUOUS,
                quality = Quality.HIGH,
                schedule = "24/7"
            ),
            analytics = AnalyticsSettings(
                motionDetection = true,
                motionThreshold = 0.5f,
                objectDetection = false,
                faceRecognition = false,
                anprEnabled = false
            ),
            notifications = NotificationSettings(
                enabled = true,
                channels = listOf("email"),
                events = listOf("motion", "alarm")
            ),
            observation = ObservationSettings()
        )
    }

    fun createRecording(
        cameraId: String = "camera_123",
        cameraName: String = "Test Camera",
        duration: Long = 3600000L
    ): Recording {
        val startTime = System.currentTimeMillis() - duration
        return Recording(
            id = "recording_${System.currentTimeMillis()}",
            cameraId = cameraId,
            cameraName = cameraName,
            startTime = startTime,
            endTime = System.currentTimeMillis(),
            duration = duration,
            filePath = "/recordings/test.mp4",
            fileSize = 1024 * 1024 * 100L,
            codec = "H.264",
            format = RecordingFormat.MP4,
            quality = Quality.HIGH,
            status = RecordingStatus.COMPLETED,
            thumbnailUrl = null,
            createdAt = startTime
        )
    }

    fun createEvent(
        type: EventType = EventType.MOTION_DETECTION,
        severity: EventSeverity = EventSeverity.WARNING,
        cameraId: String = "camera_123"
    ): Event {
        return Event(
            id = "event_${System.currentTimeMillis()}",
            cameraId = cameraId,
            cameraName = "Test Camera",
            type = type,
            severity = severity,
            timestamp = System.currentTimeMillis(),
            description = "Test event description",
            metadata = emptyMap(),
            acknowledged = false,
            acknowledgedAt = null,
            acknowledgedBy = null,
            thumbnailUrl = null,
            videoUrl = null
        )
    }

    fun userRoleToString(role: UserRole): String {
        return when (role) {
            UserRole.ADMIN -> "ADMIN"
            UserRole.OPERATOR -> "OPERATOR"
            UserRole.VIEWER -> "VIEWER"
            UserRole.GUEST -> "GUEST"
        }
    }

    fun stringToUserRole(role: String): UserRole {
        return when (role.uppercase()) {
            "ADMIN" -> UserRole.ADMIN
            "OPERATOR" -> UserRole.OPERATOR
            "VIEWER" -> UserRole.VIEWER
            "GUEST" -> UserRole.GUEST
            else -> UserRole.VIEWER
        }
    }
}
