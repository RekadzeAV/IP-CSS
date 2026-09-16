package com.company.ipcamera.server.security

import com.company.ipcamera.server.config.EnterpriseAuthConfig
import dev.samstevens.totp.code.DefaultCodeGenerator
import dev.samstevens.totp.code.DefaultCodeVerifier
import dev.samstevens.totp.code.HashingAlgorithm
import dev.samstevens.totp.exceptions.QrGenerationException
import dev.samstevens.totp.qr.QrData
import dev.samstevens.totp.qr.ZxingPngQrGenerator
import dev.samstevens.totp.secret.DefaultSecretGenerator
import dev.samstevens.totp.secret.SecretGenerator
import dev.samstevens.totp.time.SystemTimeProvider
import mu.KotlinLogging
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val logger = KotlinLogging.logger {}

/**
 * Сервис TOTP (Time-based One-Time Password) для двухфакторной аутентификации (4.3.2.2).
 */
object TotpService {

    private const val SECRET_LENGTH = 32
    private const val QR_SIZE = 256

    private val secretGenerator: SecretGenerator = DefaultSecretGenerator(SECRET_LENGTH)
    private val codeGenerator = DefaultCodeGenerator(HashingAlgorithm.SHA1, 6)
    private val qrGenerator = ZxingPngQrGenerator()

    /**
     * Генерирует новый секрет для TOTP (при включении 2FA пользователем).
     */
    fun generateSecret(): String = secretGenerator.generate()

    /**
     * Проверяет код TOTP.
     * @param secret Base32-секрет пользователя
     * @param code Код из приложения (6 цифр)
     * @return true если код верный
     */
    fun verifyCode(secret: String, code: String): Boolean {
        if (secret.isBlank() || code.isBlank()) return false
        val verifier = DefaultCodeVerifier(codeGenerator, SystemTimeProvider())
        return verifier.isValidCode(secret, code.replace(" ", ""))
    }

    /**
     * Генерирует данные для QR-кода (otpauth://...) в формате, пригодном для приложений Google Authenticator и аналогов.
     */
    fun getQrCodeData(secret: String, accountName: String): String {
        val issuer = EnterpriseAuthConfig.totpIssuer
        val encodedIssuer = URLEncoder.encode(issuer, StandardCharsets.UTF_8)
        val encodedAccountName = URLEncoder.encode(accountName, StandardCharsets.UTF_8)
        return "otpauth://totp/$encodedIssuer:$encodedAccountName?secret=$secret&issuer=$encodedIssuer&algorithm=SHA1&digits=6&period=30"
    }

    /**
     * Генерирует PNG QR-код для отображения пользователю при настройке 2FA.
     */
    fun generateQrCodePng(secret: String, accountName: String): ByteArray? {
        return try {
            val qrData = QrData.Builder()
                .label(accountName)
                .secret(secret)
                .issuer(EnterpriseAuthConfig.totpIssuer)
                .algorithm(HashingAlgorithm.SHA1)
                .digits(6)
                .period(30)
                .build()
            qrGenerator.generate(qrData)
        } catch (e: QrGenerationException) {
            logger.warn(e) { "Failed to generate TOTP QR code" }
            null
        }
    }
}
