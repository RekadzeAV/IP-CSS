package com.company.ipcamera.server.cloud

/**
 * Провайдер облачного хранилища (4.1.2): S3-совместимый API (AWS S3, MinIO, GCS, Azure Blob).
 */
interface CloudStorageProvider {

    /** Загрузить объект по ключу. */
    suspend fun upload(key: String, data: ByteArray, contentType: String? = null): Result<String>

    /** Скачать объект по ключу. */
    suspend fun download(key: String): Result<ByteArray>

    /** Удалить объект. */
    suspend fun delete(key: String): Result<Unit>

    /** Список ключей с префиксом. */
    suspend fun list(prefix: String, maxKeys: Int = 1000): Result<List<CloudObjectInfo>>
}

data class CloudObjectInfo(
    val key: String,
    val size: Long,
    val lastModified: Long
)
