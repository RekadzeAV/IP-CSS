package com.company.ipcamera.server.config

/**
 * Конфигурация облачного хранилища (4.1.2): S3-совместимые провайдеры (AWS S3, MinIO, GCS, Azure Blob, Backblaze B2).
 */
object CloudStorageConfig {

    /** Включить облачное/локальное хранилище (при true используется файловый провайдер, если не задан S3). */
    val enabled: Boolean
        get() = System.getenv("CLOUD_STORAGE_ENABLED")?.lowercase() == "true" || provider != null

    /** s3 | minio | gcs | azure | b2 */
    val provider: String?
        get() = System.getenv("CLOUD_STORAGE_PROVIDER")?.takeIf { it.isNotBlank() }

    val bucket: String?
        get() = System.getenv("CLOUD_STORAGE_BUCKET")?.takeIf { it.isNotBlank() }

    val endpoint: String?
        get() = System.getenv("CLOUD_STORAGE_ENDPOINT")?.takeIf { it.isNotBlank() }

    val region: String
        get() = System.getenv("CLOUD_STORAGE_REGION") ?: "us-east-1"

    val accessKey: String?
        get() = System.getenv("CLOUD_STORAGE_ACCESS_KEY")?.takeIf { it.isNotBlank() }

    val secretKey: String?
        get() = System.getenv("CLOUD_STORAGE_SECRET_KEY")?.takeIf { it.isNotBlank() }

    val prefix: String
        get() = System.getenv("CLOUD_STORAGE_PREFIX")?.takeIf { it.isNotBlank() } ?: "ip-css/recordings"

    val pathStyleAccess: Boolean
        get() = System.getenv("CLOUD_STORAGE_PATH_STYLE")?.lowercase() == "true"

    /**
     * S3-специфичная конфигурация, собранная из переменных окружения.
     */
    val s3Config: S3Config
        get() = S3Config(
            bucketName = bucket ?: throw IllegalStateException("CLOUD_STORAGE_BUCKET is required"),
            endpoint = endpoint,
            region = region,
            accessKey = accessKey,
            secretKey = secretKey,
            forcePathStyle = pathStyleAccess
        )

    /**
     * Data class для S3-совместимой конфигурации (AWS S3, MinIO, Backblaze B2, GCS).
     */
    data class S3Config(
        val bucketName: String,
        val endpoint: String? = null,
        val region: String = "us-east-1",
        val accessKey: String? = null,
        val secretKey: String? = null,
        val forcePathStyle: Boolean = false
    )
}
