package com.company.ipcamera.server.service

import com.company.ipcamera.server.repository.MotionEventRepository
import com.company.ipcamera.shared.domain.model.NotificationPriority
import com.company.ipcamera.shared.domain.model.NotificationType
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Уведомления о событиях детекции движения.
 *
 * Маршрутизирует через реальный серверный [NotificationService] (persistence +
 * push/broadcast-каналы: APNs/WebPush/FCM/Telegram/Email/SMS). Ранее был стаб-нооп.
 *
 * @param notificationService реальный сервис уведомлений (опционально)
 */
class MotionNotificationService(
    private val motionEventRepository: MotionEventRepository,
    private val snapshotService: MotionSnapshotService,
    private val notificationService: NotificationService? = null,
) {

    suspend fun sendNotification(event: com.company.ipcamera.shared.domain.model.MotionEvent): Result<Unit> {
        val service = notificationService
        if (service == null) {
            logger.debug { "NotificationService not configured — motion notification skipped" }
            return Result.success(Unit)
        }
        val confidencePct = ((event.confidence * 1000).toInt() / 10.0)
        val title = "Обнаружено движение"
        val message = buildString {
            append("Камера: ${event.cameraId} — движение (уверенность $confidencePct%)")
            event.zoneId?.let { append(", зона $it") }
            event.snapshotPath?.let { append(", кадр: $it") }
        }
        val extras = buildMap {
            put("motionEventId", event.id)
            put("confidence", event.confidence.toString())
            event.zoneId?.let { put("zoneId", it) }
        }
        return dispatch(title, message, event.cameraId, event.id, event.recordingId, extras)
    }

    /** Уведомление о событии детекции объектов (реальный канал через NotificationService). */
    suspend fun sendNotification(event: com.company.ipcamera.shared.domain.model.ObjectDetectionEvent): Result<Unit> {
        val service = notificationService
        if (service == null) {
            logger.debug { "NotificationService not configured — object notification skipped" }
            return Result.success(Unit)
        }
        val objs = event.detectedObjects
        val top = objs.maxByOrNull { it.confidence }
        val classes = objs.groupingBy { it.type }.eachCount()
            .entries.sortedByDescending { it.value }.joinToString(", ") { "${it.key}×${it.value}" }
        val title = "Обнаружены объекты"
        val message = buildString {
            append("Камера: ${event.cameraId} — объектов: ${objs.size}")
            classes.takeIf { it.isNotBlank() }?.let { append(" ($classes)") }
            top?.let { append(", лучшая уверенность ${((it.confidence * 1000).toInt() / 10.0)}%") }
            event.snapshotPath?.let { append(", кадр: $it") }
        }
        val extras = buildMap {
            put("objectDetectionEventId", event.id)
            put("objectCount", objs.size.toString())
            classes.takeIf { it.isNotBlank() }?.let { put("classes", classes) }
            top?.let { put("confidence", it.confidence.toString()) }
        }
        return dispatch(title, message, event.cameraId, event.id, event.recordingId, extras)
    }

    private suspend fun dispatch(
        title: String,
        message: String,
        cameraId: String,
        eventId: String,
        recordingId: String?,
        extras: Map<String, String>,
    ): Result<Unit> {
        val service = notificationService ?: return Result.success(Unit)
        return try {
            service.sendNotification(
                title = title,
                message = message,
                type = NotificationType.EVENT,
                priority = NotificationPriority.HIGH,
                userId = null,
                cameraId = cameraId,
                eventId = eventId,
                recordingId = recordingId,
                extras = extras,
            ).map { }
        } catch (e: Exception) {
            logger.error(e) { "Failed to send analytics notification for event $eventId" }
            Result.failure(e)
        }
    }

    suspend fun sendSnapshot(event: com.company.ipcamera.shared.domain.model.MotionEvent): Result<Unit> {
        // Сохранение снапшота выполняется MotionSnapshotService; доставка вложений
        // обрабатывается на уровне каналов уведомлений.
        return try {
            val saved = snapshotService.saveSnapshot(event, event.snapshotPath?.let { File(it).readBytes() } ?: ByteArray(0))
            if (saved.isSuccess) {
                Result.success(Unit)
            } else {
                Result.failure(saved.exceptionOrNull() ?: Exception("Snapshot save failed"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to persist motion snapshot for event ${event.id}" }
            Result.failure(e)
        }
    }
}