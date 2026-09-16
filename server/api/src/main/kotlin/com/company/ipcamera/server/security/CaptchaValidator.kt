package com.company.ipcamera.server.security

import mu.KotlinLogging
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

private val logger = KotlinLogging.logger {}

/**
 * Простой валидатор CAPTCHA для защиты от автоматизированных атак
 *
 * В production рекомендуется использовать сервисы типа Google reCAPTCHA,
 * hCaptcha или Cloudflare Turnstile
 */
object CaptchaValidator {

    private val secretKey = System.getenv("CAPTCHA_SECRET_KEY") ?: generateDefaultSecret()
    private val hmac = Mac.getInstance("HmacSHA256")
    private val random = SecureRandom()

    init {
        val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")
        hmac.init(secretKeySpec)
    }

    /**
     * Генерирует CAPTCHA токен
     *
     * @return Токен CAPTCHA, который должен быть отправлен клиентом
     */
    fun generateToken(): String {
        val timestamp = System.currentTimeMillis()
        val randomBytes = ByteArray(16)
        random.nextBytes(randomBytes)

        val data = "$timestamp:${Base64.getEncoder().encodeToString(randomBytes)}"
        val signature = hmac.doFinal(data.toByteArray())

        return Base64.getEncoder().encodeToString("$data:${Base64.getEncoder().encodeToString(signature)}".toByteArray())
    }

    /**
     * Валидирует CAPTCHA токен
     *
     * @param token Токен от клиента
     * @return true, если токен валиден и не истек
     */
    fun validateToken(token: String): Boolean {
        return try {
            val decoded = Base64.getDecoder().decode(token)
            val parts = String(decoded).split(":")

            if (parts.size != 3) {
                logger.warn { "Invalid CAPTCHA token format" }
                return false
            }

            val timestamp = parts[0].toLongOrNull() ?: return false
            val randomData = parts[1]
            val receivedSignature = Base64.getDecoder().decode(parts[2])

            // Проверяем, не истек ли токен (5 минут)
            val now = System.currentTimeMillis()
            val tokenAge = now - timestamp
            if (tokenAge > 5.minutes.inWholeMilliseconds) {
                logger.warn { "CAPTCHA token expired (age: ${tokenAge}ms)" }
                return false
            }

            // Проверяем подпись
            val data = "$timestamp:$randomData"
            val expectedSignature = hmac.doFinal(data.toByteArray())

            if (!expectedSignature.contentEquals(receivedSignature)) {
                logger.warn { "CAPTCHA token signature mismatch" }
                return false
            }

            true
        } catch (e: Exception) {
            logger.error(e) { "Error validating CAPTCHA token" }
            false
        }
    }

    /**
     * Генерирует секретный ключ по умолчанию (только для development)
     * В production должен быть установлен через переменную окружения
     */
    private fun generateDefaultSecret(): String {
        logger.warn { "Using default CAPTCHA secret key - NOT SECURE FOR PRODUCTION!" }
        return "default-captcha-secret-key-change-in-production-${System.currentTimeMillis()}"
    }
}
