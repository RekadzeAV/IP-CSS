package com.company.ipcamera.server.cloud

import com.company.ipcamera.server.config.CloudStorageConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.net.URI
import java.time.Duration

private val logger = KotlinLogging.logger {}

/**
 * S3-совместимый провайдер облачного хранилища (4.1.2).
 *
 * Поддерживает:
 * - AWS S3
 * - MinIO (настраивается через endpoint)
 * - Backblaze B2 (S3-совместимый API)
 * - Google Cloud Storage (S3-совместимый API)
 *
 * @param config Конфигурация облачного хранилища
 */
class S3CloudStorageProvider(
    private val config: CloudStorageConfig.S3Config
) : CloudStorageProvider {

    private val bucketName: String = config.bucketName

    private val client: S3Client by lazy {
        val builder = S3Client.builder()

        // Настройка endpoint для MinIO / Backblaze / кастомных S3
        if (config.endpoint != null) {
            builder.endpointOverride(URI.create(config.endpoint))
        }

        // Регион (обязательный параметр для AWS SDK)
        builder.region(Region.of(config.region.ifBlank { "us-east-1" }))

        // Аутентификация
        if (config.accessKey != null && config.secretKey != null) {
            builder.credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(config.accessKey, config.secretKey)
                )
            )
        }

        // Force path style для MinIO и Backblaze
        builder.forcePathStyle(config.forcePathStyle)

        builder.build()
    }

    /**
     * Предподписанный URL-генератор (S3Presigner) — даёт временные подписанные ссылки
     * для безопасной прямой загрузки/скачивания без публичного доступа к bucket.
     */
    private val presigner: S3Presigner by lazy {
        val builder = S3Presigner.builder()

        if (config.endpoint != null) {
            builder.endpointOverride(URI.create(config.endpoint))
        }
        builder.region(Region.of(config.region.ifBlank { "us-east-1" }))
        if (config.accessKey != null && config.secretKey != null) {
            builder.credentialsProvider(
                StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(config.accessKey, config.secretKey)
                )
            )
        }
        builder.serviceConfiguration(
            software.amazon.awssdk.services.s3.S3Configuration.builder()
                .pathStyleAccessEnabled(config.forcePathStyle)
                .build()
        )
        builder.build()
    }

    override suspend fun upload(key: String, data: ByteArray, contentType: String?): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                ensureBucketExists()

                val request = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType(contentType ?: "application/octet-stream")
                    .build()

                client.putObject(request, RequestBody.fromBytes(data))

                // Генерируем URL для доступа к объекту
                val objectUrl = "${config.endpoint ?: "https://$bucketName.s3.${config.region}.amazonaws.com"}/$bucketName/$key"

                logger.info { "S3 upload successful: $key (${"%.1f".format(data.size.toDouble() / 1024 / 1024)} MB)" }
                Result.success(objectUrl)
            } catch (e: Exception) {
                logger.error(e) { "S3 upload failed for key: $key" }
                Result.failure(S3StorageException("Upload failed: ${e.message}", e))
            }
        }

    override suspend fun download(key: String): Result<ByteArray> =
        withContext(Dispatchers.IO) {
            try {
                val request = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build()

                val response = client.getObject(request)
                val bytes = response.readAllBytes()

                logger.debug { "S3 download successful: $key (${"%.1f".format(bytes.size.toDouble() / 1024 / 1024)} MB)" }
                Result.success(bytes)
            } catch (e: NoSuchKeyException) {
                logger.warn { "S3 key not found: $key" }
                Result.failure(S3StorageException("Key not found: $key", e))
            } catch (e: Exception) {
                logger.error(e) { "S3 download failed for key: $key" }
                Result.failure(S3StorageException("Download failed: ${e.message}", e))
            }
        }

    override suspend fun delete(key: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            try {
                val request = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build()

                client.deleteObject(request)
                logger.info { "S3 delete successful: $key" }
                Result.success(Unit)
            } catch (e: Exception) {
                logger.error(e) { "S3 delete failed for key: $key" }
                Result.failure(S3StorageException("Delete failed: ${e.message}", e))
            }
        }

    override suspend fun list(prefix: String, maxKeys: Int): Result<List<CloudObjectInfo>> =
        withContext(Dispatchers.IO) {
            try {
                val objects = mutableListOf<CloudObjectInfo>()
                var continuationToken: String? = null

                do {
                    val request = ListObjectsV2Request.builder()
                        .bucket(bucketName)
                        .prefix(prefix)
                        .maxKeys(maxKeys)
                        .continuationToken(continuationToken)
                        .build()

                    val response = client.listObjectsV2(request)

                    response.contents().forEach { s3Object ->
                        objects.add(
                            CloudObjectInfo(
                                key = s3Object.key(),
                                size = s3Object.size(),
                                lastModified = s3Object.lastModified()?.toEpochMilli() ?: 0L
                            )
                        )
                    }

                    continuationToken = if (response.isTruncated) response.nextContinuationToken() else null
                } while (continuationToken != null)

                logger.debug { "S3 list successful: prefix=$prefix, count=${objects.size}" }
                Result.success(objects)
            } catch (e: Exception) {
                logger.error(e) { "S3 list failed for prefix: $prefix" }
                Result.failure(S3StorageException("List failed: ${e.message}", e))
            }
        }

    /**
     * Проверяет существование bucket'а и создаёт его при необходимости.
     */
    private fun ensureBucketExists() {
        try {
            val headRequest = HeadBucketRequest.builder()
                .bucket(bucketName)
                .build()
            client.headBucket(headRequest)
        } catch (e: NoSuchBucketException) {
            logger.info { "Bucket '$bucketName' does not exist. Creating..." }
            try {
                val createRequest = CreateBucketRequest.builder()
                    .bucket(bucketName)
                    .build()
                client.createBucket(createRequest)
                logger.info { "Bucket '$bucketName' created successfully" }
            } catch (createError: Exception) {
                logger.warn(createError) { "Failed to create bucket '$bucketName': ${createError.message}" }
                // Не выбрасываем исключение — возможно bucket уже существует
            }
        }
    }

    /**
     * Получить URL для предварительно подписанного доступа к объекту (GET).
     * Используется для безопасной прямой загрузки/скачивания.
     */
    suspend fun generatePresignedUrl(key: String, expirationMinutes: Int = 60): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val request = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes.toLong()))
                    .getObjectRequest(
                        GetObjectRequest.builder()
                            .bucket(bucketName)
                            .key(key)
                            .build()
                    )
                    .build()

                val presigned = presigner.presignGetObject(request)
                logger.info { "S3 presigned GET URL generated for: $key (expires in ${expirationMinutes}m)" }
                Result.success(presigned.url().toExternalForm())
            } catch (e: Exception) {
                Result.failure(S3StorageException("Failed to generate presigned GET URL: ${e.message}", e))
            }
        }

    /**
     * Получить URL для предварительно подписанной загрузки объекта (PUT).
     * Позволяет клиенту безопасно заливать объект напрямую в bucket.
     */
    suspend fun generatePresignedUploadUrl(
        key: String,
        contentType: String? = null,
        expirationMinutes: Int = 60
    ): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .also { if (contentType != null) it.contentType(contentType) }
                    .build()

                val request = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(expirationMinutes.toLong()))
                    .putObjectRequest(putRequest)
                    .build()

                val presigned = presigner.presignPutObject(request)
                logger.info { "S3 presigned PUT URL generated for: $key (expires in ${expirationMinutes}m)" }
                Result.success(presigned.url().toExternalForm())
            } catch (e: Exception) {
                Result.failure(S3StorageException("Failed to generate presigned PUT URL: ${e.message}", e))
            }
        }

    /**
     * Получить информацию об объекте.
     */
    suspend fun getObjectInfo(key: String): Result<CloudObjectInfo> =
        withContext(Dispatchers.IO) {
            try {
                val request = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .build()

                val response = client.headObject(request)

                Result.success(
                    CloudObjectInfo(
                        key = key,
                        size = response.contentLength(),
                        lastModified = response.lastModified()?.toEpochMilli() ?: 0L
                    )
                )
            } catch (e: Exception) {
                Result.failure(S3StorageException("Failed to get object info: ${e.message}", e))
            }
        }

    /**
     * Закрыть S3 клиент и генератор подписанных URL.
     */
    fun close() {
        try {
            client.close()
        } catch (e: Exception) {
            logger.warn(e) { "Error closing S3 client" }
        }
        try {
            presigner.close()
        } catch (e: Exception) {
            logger.warn(e) { "Error closing S3 presigner" }
        }
    }
}

/**
 * Исключение S3 хранилища.
 */
class S3StorageException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
