package com.company.ipcamera.shared.test

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.core.common.model.Resolution
import com.company.ipcamera.shared.domain.model.*

/**
 * Фабрика для создания тестовых данных
 */
object TestDataFactory {

    fun createTestCamera(
        id: String = "test-camera-1",
        name: String = "Test Camera",
        url: String = "rtsp://192.168.1.100:554/stream",
        username: String? = "admin",
        password: String? = "password",
        model: String? = "Test Model",
        status: CameraStatus = CameraStatus.ONLINE,
        resolution: Resolution? = Resolution(1920, 1080),
        fps: Int = 25,
        bitrate: Int = 4096,
        codec: String = "H.264",
        audio: Boolean = true,
        ptz: PTZConfig? = null,
        streams: List<StreamConfig> = emptyList(),
        settings: CameraSettings = CameraSettings(),
        statistics: CameraStatistics? = null,
        createdAt: Long = System.currentTimeMillis(),
        updatedAt: Long = System.currentTimeMillis(),
        lastSeen: Long? = System.currentTimeMillis()
    ): Camera {
        return Camera(
            id = id,
            name = name,
            url = url,
            username = username,
            password = password,
            model = model,
            status = status,
            resolution = resolution,
            fps = fps,
            bitrate = bitrate,
            codec = codec,
            audio = audio,
            ptz = ptz,
            streams = streams,
            settings = settings,
            statistics = statistics,
            createdAt = createdAt,
            updatedAt = updatedAt,
            lastSeen = lastSeen
        )
    }

    fun createTestCameras(count: Int): List<Camera> {
        return (1..count).map { index ->
            createTestCamera(
                id = "test-camera-$index",
                name = "Test Camera $index",
                url = "rtsp://192.168.1.${100 + index}:554/stream"
            )
        }
    }

    fun createTestPTZConfig(
        enabled: Boolean = true,
        type: PTZType = PTZType.PTZ,
        presets: List<String> = listOf("Preset 1", "Preset 2")
    ): PTZConfig {
        return PTZConfig(
            enabled = enabled,
            type = type,
            presets = presets
        )
    }

    fun createTestStreamConfig(
        type: StreamType = StreamType.MAIN,
        resolution: Resolution = Resolution(1920, 1080),
        fps: Int = 25,
        bitrate: Int = 4096
    ): StreamConfig {
        return StreamConfig(
            type = type,
            resolution = resolution,
            fps = fps,
            bitrate = bitrate
        )
    }

    fun createTestCameraSettings(
        recording: RecordingSettings = RecordingSettings(),
        analytics: AnalyticsSettings = AnalyticsSettings(),
        notifications: NotificationSettings = NotificationSettings()
    ): CameraSettings {
        return CameraSettings(
            recording = recording,
            analytics = analytics,
            notifications = notifications
        )
    }

    fun createTestCameraStatistics(
        uptime: Double = 100.0,
        recordedHours: Long = 24,
        eventsCount: Long = 100,
        storageUsed: Long = 1024 * 1024 * 1024 // 1GB
    ): CameraStatistics {
        return CameraStatistics(
            uptime = uptime,
            recordedHours = recordedHours,
            eventsCount = eventsCount,
            storageUsed = storageUsed
        )
    }

    fun createTestEvent(
        id: String = "test-event-1",
        cameraId: String = "test-camera-1",
        cameraName: String? = "Test Camera",
        type: EventType = EventType.MOTION_DETECTION,
        severity: EventSeverity = EventSeverity.INFO,
        timestamp: Long = System.currentTimeMillis(),
        description: String? = "Test event description",
        metadata: Map<String, String> = emptyMap(),
        acknowledged: Boolean = false,
        acknowledgedAt: Long? = null,
        acknowledgedBy: String? = null,
        thumbnailUrl: String? = null,
        videoUrl: String? = null
    ): Event {
        return Event(
            id = id,
            cameraId = cameraId,
            cameraName = cameraName,
            type = type,
            severity = severity,
            timestamp = timestamp,
            description = description,
            metadata = metadata,
            acknowledged = acknowledged,
            acknowledgedAt = acknowledgedAt,
            acknowledgedBy = acknowledgedBy,
            thumbnailUrl = thumbnailUrl,
            videoUrl = videoUrl
        )
    }

    fun createTestEvents(count: Int, cameraId: String = "test-camera-1"): List<Event> {
        return (1..count).map { index ->
            createTestEvent(
                id = "test-event-$index",
                cameraId = cameraId,
                timestamp = System.currentTimeMillis() - (index * 1000L)
            )
        }
    }

    fun createTestRecording(
        id: String = "test-recording-1",
        cameraId: String = "test-camera-1",
        cameraName: String? = "Test Camera",
        startTime: Long = System.currentTimeMillis() - 3600000, // 1 hour ago
        endTime: Long? = System.currentTimeMillis(),
        duration: Long = 3600, // 1 hour in seconds
        filePath: String? = "/recordings/test-recording-1.mp4",
        fileSize: Long? = 1024 * 1024 * 100, // 100MB
        status: RecordingStatus = RecordingStatus.COMPLETED,
        quality: Quality = Quality.HIGH,
        format: RecordingFormat = RecordingFormat.MP4,
        thumbnailUrl: String? = null,
        createdAt: Long = System.currentTimeMillis()
    ): Recording {
        return Recording(
            id = id,
            cameraId = cameraId,
            cameraName = cameraName,
            startTime = startTime,
            endTime = endTime,
            duration = duration,
            filePath = filePath,
            fileSize = fileSize,
            status = status,
            quality = quality,
            format = format,
            thumbnailUrl = thumbnailUrl,
            createdAt = createdAt
        )
    }

    fun createTestRecordings(count: Int, cameraId: String = "test-camera-1"): List<Recording> {
        return (1..count).map { index ->
            createTestRecording(
                id = "test-recording-$index",
                cameraId = cameraId,
                startTime = System.currentTimeMillis() - (index * 3600000L) // Each recording 1 hour before previous
            )
        }
    }

    fun createTestSettings(
        id: String = "test-settings-1",
        category: SettingsCategory = SettingsCategory.SYSTEM,
        key: String = "test.key",
        value: String = "test-value",
        type: SettingsType = SettingsType.STRING,
        description: String? = "Test setting description",
        updatedAt: Long = System.currentTimeMillis()
    ): Settings {
        return Settings(
            id = id,
            category = category,
            key = key,
            value = value,
            type = type,
            description = description,
            updatedAt = updatedAt
        )
    }

    fun createTestUser(
        id: String = "test-user-1",
        username: String = "testuser",
        email: String? = "test@example.com",
        fullName: String? = "Test User",
        role: UserRole = UserRole.VIEWER,
        permissions: List<String> = emptyList(),
        createdAt: Long = System.currentTimeMillis(),
        lastLoginAt: Long? = null,
        isActive: Boolean = true
    ): User {
        return User(
            id = id,
            username = username,
            email = email,
            fullName = fullName,
            role = role,
            permissions = permissions,
            createdAt = createdAt,
            lastLoginAt = lastLoginAt,
            isActive = isActive
        )
    }

    fun createTestNotification(
        id: String = "test-notification-1",
        type: NotificationType = NotificationType.EVENT,
        title: String = "Test Notification",
        message: String = "Test notification message",
        priority: NotificationPriority = NotificationPriority.NORMAL,
        cameraId: String? = "test-camera-1",
        eventId: String? = null,
        recordingId: String? = null,
        channelId: String? = null,
        icon: String? = null,
        sound: Boolean = true,
        vibration: Boolean = false,
        read: Boolean = false,
        readAt: Long? = null,
        timestamp: Long = System.currentTimeMillis(),
        extras: Map<String, String> = emptyMap()
    ): Notification {
        return Notification(
            id = id,
            title = title,
            message = message,
            type = type,
            priority = priority,
            cameraId = cameraId,
            eventId = eventId,
            recordingId = recordingId,
            channelId = channelId,
            icon = icon,
            sound = sound,
            vibration = vibration,
            read = read,
            readAt = readAt,
            timestamp = timestamp,
            extras = extras
        )
    }

    fun createTestNotifications(count: Int): List<Notification> {
        return (1..count).map { index ->
            createTestNotification(
                id = "test-notification-$index",
                timestamp = System.currentTimeMillis() - (index * 1000L)
            )
        }
    }
}


