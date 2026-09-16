package com.company.ipcamera.server.cloud

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

/**
 * Локальный провайдер «облака» на файловой системе (для тестов и без облака).
 * Путь задаётся через CLOUD_STORAGE_LOCAL_PATH или по умолчанию cloud-storage.
 */
class FileBasedCloudStorageProvider(
    private val basePath: String = System.getenv("CLOUD_STORAGE_LOCAL_PATH") ?: "cloud-storage"
) : CloudStorageProvider {

    private val root: Path = Paths.get(basePath).also { it.toFile().mkdirs() }

    override suspend fun upload(key: String, data: ByteArray, contentType: String?): Result<String> = withContext(Dispatchers.IO) {
        try {
            val path = root.resolve(key.replace("//", "/"))
            Files.createDirectories(path.parent)
            Files.write(path, data)
            Result.success(key)
        } catch (e: Exception) {
            logger.error(e) { "Upload failed: $key" }
            Result.failure(e)
        }
    }

    override suspend fun download(key: String): Result<ByteArray> = withContext(Dispatchers.IO) {
        try {
            val path = root.resolve(key.replace("//", "/"))
            if (!Files.exists(path)) return@withContext Result.failure(java.nio.file.NoSuchFileException(path.toString()))
            Result.success(Files.readAllBytes(path))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun delete(key: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val path = root.resolve(key.replace("//", "/"))
            Files.deleteIfExists(path)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun list(prefix: String, maxKeys: Int): Result<List<CloudObjectInfo>> = withContext(Dispatchers.IO) {
        try {
            val dir = root.resolve(prefix.replace("//", "/"))
            if (!Files.exists(dir) || !Files.isDirectory(dir)) return@withContext Result.success(emptyList())
            val list = Files.walk(dir, 2).use { stream ->
                stream.filter { Files.isRegularFile(it) }
                    .limit(maxKeys.toLong())
                    .map { p: java.nio.file.Path ->
                        val rel = root.relativize(p).toString().replace("\\", "/")
                        CloudObjectInfo(key = rel, size = Files.size(p), lastModified = Files.getLastModifiedTime(p).toMillis())
                    }
                    .toList()
            }
            Result.success(list)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
