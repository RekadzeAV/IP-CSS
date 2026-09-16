package com.company.ipcamera.server.cloud

import com.company.ipcamera.server.config.CloudStorageConfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Юнит-тесты для S3 presigned URL генерации.
 *
 * S3Presigner формирует подпись и URL локально (без сетевых вызовов), поэтому
 * тесты работают офлайн с MinIO-совместимым конфигом.
 */
class S3CloudStorageProviderTest {

    private fun createProvider(
        bucket: String = "test-bucket",
        endpoint: String = "http://localhost:9000",
        region: String = "us-east-1",
        accessKey: String = "test-access-key",
        secretKey: String = "test-secret-key"
    ): S3CloudStorageProvider = S3CloudStorageProvider(
        CloudStorageConfig.S3Config(
            bucketName = bucket,
            endpoint = endpoint,
            region = region,
            accessKey = accessKey,
            secretKey = secretKey,
            forcePathStyle = true
        )
    )

    @Test
    fun `generatePresignedUrl returns signed GET url`() = runTest {
        val provider = createProvider()
        val result = provider.generatePresignedUrl("recordings/2026/08/01/clip.mp4", expirationMinutes = 60)

        assertTrue(result.isSuccess, "Presigned GET URL should be generated")
        val url = result.getOrThrow()
        assertNotNull(url)
        assertTrue(url.contains("X-Amz-Expires=3600"), "URL must contain signature expiration")
        assertTrue(url.contains("X-Amz-Credential="), "URL must contain credential signature")
        assertTrue(url.contains("X-Amz-Signature="), "URL must contain signature")
        assertTrue(url.contains("clip.mp4"), "URL must reference the object key")
    }

    @Test
    fun `generatePresignedUrl respects custom expiration`() = runTest {
        val provider = createProvider()
        val result = provider.generatePresignedUrl("key.bin", expirationMinutes = 15)

        assertTrue(result.isSuccess, "Presigned GET URL should be generated")
        val url = result.getOrThrow()
        assertTrue(url.contains("X-Amz-Expires=900"), "15 minutes = 900 seconds")
    }

    @Test
    fun `generatePresignedUploadUrl returns signed PUT url with content type`() = runTest {
        val provider = createProvider()
        val result = provider.generatePresignedUploadUrl(
            key = "snapshots/cam-01.jpg",
            contentType = "image/jpeg",
            expirationMinutes = 30
        )

        assertTrue(result.isSuccess, "Presigned PUT URL should be generated")
        val url = result.getOrThrow()
        assertNotNull(url)
        assertTrue(url.contains("X-Amz-Expires=1800"), "PUT url must contain signature expiration")
        assertTrue(url.contains("X-Amz-Signature="), "PUT url must contain signature")
        assertTrue(url.contains("cam-01.jpg"), "PUT url must reference the object key")
    }

    @Test
    fun `presigned urls differ per key`() = runTest {
        val provider = createProvider()
        val urlA = provider.generatePresignedUrl("a.txt").getOrThrow()
        val urlB = provider.generatePresignedUrl("b.txt").getOrThrow()

        assertFalse(urlA == urlB, "Different keys must produce different presigned urls")
    }
}