package com.company.ipcamera.shared.data.local

import com.company.ipcamera.core.common.model.CameraStatus
import com.company.ipcamera.core.common.model.Resolution
import com.company.ipcamera.shared.database.Camera
import com.company.ipcamera.shared.domain.model.*
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Маппер между сущностью базы данных и доменной моделью Camera
 */
internal class CameraEntityMapper {

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun toDomain(dbCamera: Camera): com.company.ipcamera.shared.domain.model.Camera {
        return com.company.ipcamera.shared.domain.model.Camera(
            id = dbCamera.id,
            name = dbCamera.name,
            url = dbCamera.url,
            username = dbCamera.username,
            password = dbCamera.password,
            model = dbCamera.model,
            status = CameraStatus.valueOf(dbCamera.status),
            resolution = dbCamera.resolution_width?.let { width ->
                dbCamera.resolution_height?.let { height ->
                    Resolution(width, height)
                }
            },
            fps = dbCamera.fps.toInt(),
            bitrate = dbCamera.bitrate.toInt(),
            codec = dbCamera.codec,
            audio = dbCamera.audio == 1L,
            ptz = dbCamera.ptz_config?.let { json.decodeFromString<PTZConfig>(it) },
            streams = dbCamera.streams?.let { json.decodeFromString<List<StreamConfig>>(it) } ?: emptyList(),
            settings = dbCamera.settings?.let { json.decodeFromString<CameraSettings>(it) } ?: CameraSettings(),
            statistics = dbCamera.statistics?.let { json.decodeFromString<CameraStatistics>(it) },
            createdAt = dbCamera.created_at,
            updatedAt = dbCamera.updated_at,
            lastSeen = dbCamera.last_seen
        )
    }

    fun toDatabase(camera: com.company.ipcamera.shared.domain.model.Camera): Camera {
        return Camera(
            id = camera.id,
            name = camera.name,
            url = camera.url,
            username = camera.username,
            password = camera.password,
            model = camera.model,
            status = camera.status.name,
            resolution_width = camera.resolution?.width?.toLong(),
            resolution_height = camera.resolution?.height?.toLong(),
            fps = camera.fps.toLong(),
            bitrate = camera.bitrate.toLong(),
            codec = camera.codec,
            audio = if (camera.audio) 1L else 0L,
            ptz_config = camera.ptz?.let { json.encodeToString(it) },
            streams = if (camera.streams.isNotEmpty()) json.encodeToString(camera.streams) else null,
            settings = json.encodeToString(camera.settings),
            statistics = camera.statistics?.let { json.encodeToString(it) },
            created_at = camera.createdAt,
            updated_at = camera.updatedAt,
            last_seen = camera.lastSeen
        )
    }
}

