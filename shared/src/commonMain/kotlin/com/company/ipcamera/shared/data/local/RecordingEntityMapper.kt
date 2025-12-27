package com.company.ipcamera.shared.data.local

import com.company.ipcamera.shared.database.Recording as DbRecording
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.model.RecordingFormat
import com.company.ipcamera.shared.domain.model.RecordingStatus
import com.company.ipcamera.shared.domain.model.Quality

/**
 * Маппер между сущностью базы данных и доменной моделью Recording
 */
internal class RecordingEntityMapper {

    fun toDomain(dbRecording: DbRecording): Recording {
        return Recording(
            id = dbRecording.id,
            cameraId = dbRecording.camera_id,
            cameraName = dbRecording.camera_name,
            startTime = dbRecording.start_time,
            endTime = dbRecording.end_time,
            duration = dbRecording.duration,
            filePath = dbRecording.file_path,
            fileSize = dbRecording.file_size,
            format = RecordingFormat.valueOf(dbRecording.format),
            quality = Quality.valueOf(dbRecording.quality),
            status = RecordingStatus.valueOf(dbRecording.status),
            thumbnailUrl = dbRecording.thumbnail_url,
            createdAt = dbRecording.created_at
        )
    }

    fun toDatabase(recording: Recording): DbRecording {
        return DbRecording(
            id = recording.id,
            camera_id = recording.cameraId,
            camera_name = recording.cameraName,
            start_time = recording.startTime,
            end_time = recording.endTime,
            duration = recording.duration,
            file_path = recording.filePath,
            file_size = recording.fileSize,
            format = recording.format.name,
            quality = recording.quality.name,
            status = recording.status.name,
            thumbnail_url = recording.thumbnailUrl,
            created_at = recording.createdAt
        )
    }
}

