package com.company.ipcamera.server.service

import mu.KotlinLogging
import java.security.MessageDigest
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

private val logger = KotlinLogging.logger {}

/**
 * Сервис для генерации подписанных URL для безопасного доступа к файлам
 *
 * Подписанные URL содержат:
 * - Подпись (HMAC-SHA256) для проверки целостности
 * - Время истечения (expires) для ограничения времени доступа
 * - Идентификатор файла/записи
 *
 * Формат: /api/v1/recordings/{id}/download?sig={signature}&expires={timestamp}
 */
@OptIn(ExperimentalEncodingApi::class)
class SignedUrlService(
    private val secretKey: String = System.getenv("SIGNED_URL_SECRET") ?: "default-secret-key-change-in-production"
) {
    private val algorithm = "HmacSHA256"
    private val defaultExpirationSeconds = 3600L // 1 час по умолчанию

    init {
        if (secretKey == "default-secret-key-change-in-production") {
            logger.warn { "Using default secret key for signed URLs. Change SIGNED_URL_SECRET in production!" }
        }
    }

    /**
     * Генерирует подписанный URL для доступа к записи
     *
     * @param recordingId ID записи
     * @param expirationSeconds Время жизни URL в секундах (по умолчанию 1 час)
     * @return Подписанный URL
     */
    fun generateSignedUrl(recordingId: String, expirationSeconds: Long = defaultExpirationSeconds): String {
        val expires = System.currentTimeMillis() / 1000 + expirationSeconds
        val signature = generateSignature(recordingId, expires)

        return "/api/v1/recordings/$recordingId/download?sig=$signature&expires=$expires"
    }

    /**
     * Генерирует подписанный URL для экспортированного файла
     *
     * @param recordingId ID записи
     * @param fileName Имя экспортированного файла
     * @param expirationSeconds Время жизни URL в секундах (по умолчанию 1 час)
     * @return Подписанный URL
     */
    fun generateExportSignedUrl(
        recordingId: String,
        fileName: String,
        expirationSeconds: Long = defaultExpirationSeconds
    ): String {
        val expires = System.currentTimeMillis() / 1000 + expirationSeconds
        val signature = generateSignature("$recordingId:$fileName", expires)

        return "/api/v1/recordings/$recordingId/export/download?file=$fileName&sig=$signature&expires=$expires"
    }

    /**
     * Проверяет валидность подписанного URL
     *
     * @param recordingId ID записи
     * @param signature Подпись из URL
     * @param expires Время истечения из URL
     * @return true если URL валиден
     */
    fun verifySignedUrl(recordingId: String, signature: String, expires: Long): Boolean {
        // Проверяем время истечения
        val currentTime = System.currentTimeMillis() / 1000
        if (currentTime > expires) {
            logger.warn { "Signed URL expired for recording: $recordingId" }
            return false
        }

        // Проверяем подпись
        val expectedSignature = generateSignature(recordingId, expires)
        val isValid = MessageDigest.isEqual(signature.toByteArray(), expectedSignature.toByteArray())

        if (!isValid) {
            logger.warn { "Invalid signature for recording: $recordingId" }
        }

        return isValid
    }

    /**
     * Проверяет валидность подписанного URL для экспортированного файла
     *
     * @param recordingId ID записи
     * @param fileName Имя файла
     * @param signature Подпись из URL
     * @param expires Время истечения из URL
     * @return true если URL валиден
     */
    fun verifyExportSignedUrl(
        recordingId: String,
        fileName: String,
        signature: String,
        expires: Long
    ): Boolean {
        // Проверяем время истечения
        val currentTime = System.currentTimeMillis() / 1000
        if (currentTime > expires) {
            logger.warn { "Signed export URL expired for recording: $recordingId, file: $fileName" }
            return false
        }

        // Проверяем подпись
        val expectedSignature = generateSignature("$recordingId:$fileName", expires)
        val isValid = MessageDigest.isEqual(signature.toByteArray(), expectedSignature.toByteArray())

        if (!isValid) {
            logger.warn { "Invalid export signature for recording: $recordingId, file: $fileName" }
        }

        return isValid
    }

    /**
     * Генерирует HMAC-SHA256 подпись
     *
     * @param data Данные для подписи (recordingId или recordingId:fileName)
     * @param expires Время истечения
     * @return Base64-encoded подпись
     */
    private fun generateSignature(data: String, expires: Long): String {
        val message = "$data:$expires"
        val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), algorithm)
        val mac = Mac.getInstance(algorithm)
        mac.init(secretKeySpec)
        val hash = mac.doFinal(message.toByteArray())
        val base64 = Base64.Default.encode(hash)
        return base64.replace("+", "-").replace("/", "_").replace("=", "")
    }
}
