package com.company.ipcamera.server.service

import com.company.ipcamera.shared.domain.model.*
import mu.KotlinLogging
import java.text.SimpleDateFormat
import java.util.*

private val logger = KotlinLogging.logger {}

/**
 * Сервис для экспорта данных в различные форматы (CSV, JSON)
 */
class ExportService {

    /**
     * Экспортирует записи в CSV формат
     */
    fun exportRecordingsToCsv(recordings: List<Recording>): String {
        val csv = StringBuilder()

        // Заголовки CSV
        csv.append("ID,Camera ID,Camera Name,Start Time,End Time,Duration (ms),File Path,File Size (bytes),Format,Quality,Status,Thumbnail URL,Created At\n")

        // Данные
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        recordings.forEach { recording ->
            csv.append("\"${recording.id}\",")
            csv.append("\"${recording.cameraId}\",")
            csv.append("\"${recording.cameraName ?: ""}\",")
            csv.append("\"${dateFormat.format(Date(recording.startTime))}\",")
            csv.append("\"${recording.endTime?.let { dateFormat.format(Date(it)) } ?: ""}\",")
            csv.append("${recording.duration},")
            csv.append("\"${recording.filePath ?: ""}\",")
            csv.append("${recording.fileSize ?: ""},")
            csv.append("\"${recording.format.name}\",")
            csv.append("\"${recording.quality.name}\",")
            csv.append("\"${recording.status.name}\",")
            csv.append("\"${recording.thumbnailUrl ?: ""}\",")
            csv.append("\"${dateFormat.format(Date(recording.createdAt))}\"\n")
        }

        return csv.toString()
    }

    /**
     * Экспортирует записи в JSON формат
     */
    fun exportRecordingsToJson(recordings: List<Recording>): String {
        val json = StringBuilder()
        json.append("{\n")
        json.append("  \"recordings\": [\n")

        recordings.forEachIndexed { index, recording ->
            json.append("    {\n")
            json.append("      \"id\": \"${recording.id}\",\n")
            json.append("      \"cameraId\": \"${recording.cameraId}\",\n")
            json.append("      \"cameraName\": ${if (recording.cameraName != null) "\"${recording.cameraName}\"" else "null"},\n")
            json.append("      \"startTime\": ${recording.startTime},\n")
            json.append("      \"endTime\": ${recording.endTime ?: "null"},\n")
            json.append("      \"duration\": ${recording.duration},\n")
            json.append("      \"filePath\": ${if (recording.filePath != null) "\"${recording.filePath}\"" else "null"},\n")
            json.append("      \"fileSize\": ${recording.fileSize ?: "null"},\n")
            json.append("      \"format\": \"${recording.format.name}\",\n")
            json.append("      \"quality\": \"${recording.quality.name}\",\n")
            json.append("      \"status\": \"${recording.status.name}\",\n")
            json.append("      \"thumbnailUrl\": ${if (recording.thumbnailUrl != null) "\"${recording.thumbnailUrl}\"" else "null"},\n")
            json.append("      \"createdAt\": ${recording.createdAt}\n")
            json.append("    }")
            if (index < recordings.size - 1) {
                json.append(",")
            }
            json.append("\n")
        }

        json.append("  ],\n")
        json.append("  \"total\": ${recordings.size},\n")
        json.append("  \"exportedAt\": ${System.currentTimeMillis()}\n")
        json.append("}\n")

        return json.toString()
    }

    /**
     * Экспортирует события в CSV формат
     */
    fun exportEventsToCsv(events: List<Event>): String {
        val csv = StringBuilder()

        // Заголовки CSV
        csv.append("ID,Camera ID,Type,Severity,Message,Timestamp,Acknowledged,Acknowledged At,Acknowledged By,Metadata\n")

        // Данные
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        events.forEach { event ->
            csv.append("\"${event.id}\",")
            csv.append("\"${event.cameraId}\",")
            csv.append("\"${event.type.name}\",")
            csv.append("\"${event.severity.name}\",")
            csv.append("\"${(event.description ?: "").replace("\"", "\"\"")}\",")
            csv.append("\"${dateFormat.format(Date(event.timestamp))}\",")
            csv.append("${event.acknowledged},")
            csv.append("\"${event.acknowledgedAt?.let { dateFormat.format(Date(it)) } ?: ""}\",")
            csv.append("\"${event.acknowledgedBy ?: ""}\",")
            csv.append("\"${event.metadata?.toString()?.replace("\"", "\"\"") ?: ""}\"\n")
        }

        return csv.toString()
    }

    /**
     * Экспортирует события в JSON формат
     */
    fun exportEventsToJson(events: List<Event>): String {
        val json = StringBuilder()
        json.append("{\n")
        json.append("  \"events\": [\n")

        events.forEachIndexed { index, event ->
            json.append("    {\n")
            json.append("      \"id\": \"${event.id}\",\n")
            json.append("      \"cameraId\": \"${event.cameraId}\",\n")
            json.append("      \"type\": \"${event.type.name}\",\n")
            json.append("      \"severity\": \"${event.severity.name}\",\n")
            json.append("      \"message\": \"${(event.description ?: "").replace("\"", "\\\"")}\",\n")
            json.append("      \"timestamp\": ${event.timestamp},\n")
            json.append("      \"acknowledged\": ${event.acknowledged},\n")
            json.append("      \"acknowledgedAt\": ${event.acknowledgedAt ?: "null"},\n")
            json.append("      \"acknowledgedBy\": ${if (event.acknowledgedBy != null) "\"${event.acknowledgedBy}\"" else "null"},\n")
            json.append("      \"metadata\": ${if (event.metadata != null) "\"${event.metadata}\"" else "null"}\n")
            json.append("    }")
            if (index < events.size - 1) {
                json.append(",")
            }
            json.append("\n")
        }

        json.append("  ],\n")
        json.append("  \"total\": ${events.size},\n")
        json.append("  \"exportedAt\": ${System.currentTimeMillis()}\n")
        json.append("}\n")

        return json.toString()
    }

    /**
     * Экспортирует камеры в CSV формат
     */
    fun exportCamerasToCsv(cameras: List<Camera>): String {
        val csv = StringBuilder()

        // Заголовки CSV
        csv.append("ID,Name,URL,Username,Status,Created At,Updated At\n")

        // Данные
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        cameras.forEach { camera ->
            csv.append("\"${camera.id}\",")
            csv.append("\"${camera.name.replace("\"", "\"\"")}\",")
            csv.append("\"${camera.url.replace("\"", "\"\"")}\",")
            csv.append("\"${camera.username ?: ""}\",")
            csv.append("\"${camera.status.name}\",")
            csv.append("\"${dateFormat.format(Date(camera.createdAt))}\",")
            csv.append("\"${dateFormat.format(Date(camera.updatedAt))}\"\n")
        }

        return csv.toString()
    }

    /**
     * Экспортирует камеры в JSON формат
     */
    fun exportCamerasToJson(cameras: List<Camera>): String {
        val json = StringBuilder()
        json.append("{\n")
        json.append("  \"cameras\": [\n")

        cameras.forEachIndexed { index, camera ->
            json.append("    {\n")
            json.append("      \"id\": \"${camera.id}\",\n")
            json.append("      \"name\": \"${camera.name.replace("\"", "\\\"")}\",\n")
            json.append("      \"url\": \"${camera.url.replace("\"", "\\\"")}\",\n")
            json.append("      \"username\": ${if (camera.username != null) "\"${camera.username}\"" else "null"},\n")
            json.append("      \"status\": \"${camera.status.name}\",\n")
            json.append("      \"createdAt\": ${camera.createdAt},\n")
            json.append("      \"updatedAt\": ${camera.updatedAt}\n")
            json.append("    }")
            if (index < cameras.size - 1) {
                json.append(",")
            }
            json.append("\n")
        }

        json.append("  ],\n")
        json.append("  \"total\": ${cameras.size},\n")
        json.append("  \"exportedAt\": ${System.currentTimeMillis()}\n")
        json.append("}\n")

        return json.toString()
    }
}
