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
        statistics: CameraStatistics? = null
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
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            lastSeen = System.currentTimeMillis()
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
}


