package com.company.ipcamera.server.cloud

import com.company.ipcamera.server.config.CloudStorageConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

/**
 * Сервис облачного хранилища записей (4.1.2).
 * При включённом провайдере — загрузка/скачивание/удаление; иначе — заглушка.
 */
class CloudStorageService(
    private val provider: CloudStorageProvider?,
    private val prefix: String = CloudStorageConfig.prefix
) {

    fun isEnabled(): Boolean = provider != null

    suspend fun uploadRecording(recordingId: String, filePath: Path): Result<String> = withContext(Dispatchers.IO) {
        val p = provider ?: return@withContext Result.failure(IllegalStateException("Cloud storage not configured"))
        val key = "$prefix/$recordingId/${filePath.fileName}"
        val data = Files.readAllBytes(filePath)
        p.upload(key, data, "video/mp4").onSuccess {
            logger.info { "Uploaded recording $recordingId to cloud" }
        }.onFailure {
            logger.error(it) { "Upload failed for $recordingId" }
        }
    }

    suspend fun downloadRecording(recordingId: String, keyOrPath: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        val p = provider ?: return@withContext Result.failure(IllegalStateException("Cloud storage not configured"))
        val key = if (keyOrPath.startsWith(prefix)) keyOrPath else "$prefix/$recordingId/$keyOrPath"
        p.download(key)
    }

    suspend fun deleteRecording(recordingId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val p = provider ?: return@withContext Result.failure(IllegalStateException("Cloud storage not configured"))
        val listResult = p.list("$prefix/$recordingId/")
        listResult.getOrElse { return@withContext Result.failure(it) }.forEach { obj ->
            p.delete(obj.key).onFailure { logger.warn(it) { "Failed to delete ${obj.key}" } }
        }
        Result.success(Unit)
    }

    suspend fun listRecordings(): Result<List<CloudObjectInfo>> = withContext(Dispatchers.IO) {
        val p = provider ?: return@withContext Result.failure(IllegalStateException("Cloud storage not configured"))
        p.list("$prefix/")
    }
}
