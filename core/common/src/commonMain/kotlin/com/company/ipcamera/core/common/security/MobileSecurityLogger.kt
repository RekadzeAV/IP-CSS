package com.company.ipcamera.core.common.security

import kotlinx.datetime.Clock

/**
 * Типы событий безопасности для мобильных платформ
 */
enum class MobileSecurityEventType {
    ENCRYPTION_SUCCESS,
    ENCRYPTION_FAILURE,
    DECRYPTION_SUCCESS,
    DECRYPTION_FAILURE,
    KEYSTORE_ACCESS,
    KEYSTORE_ERROR,
    CERTIFICATE_PINNING_SUCCESS,
    CERTIFICATE_PINNING_FAILURE,
    TLS_ERROR,
    NETWORK_ERROR,
    SUSPICIOUS_ACTIVITY,
    AUTHENTICATION_SUCCESS,
    AUTHENTICATION_FAILURE,
    TOKEN_REFRESH,
    TOKEN_EXPIRED,
    DATA_ACCESS,
    FILE_ACCESS_ERROR
}

/**
 * Уровень серьезности события
 */
enum class MobileSecurityEventSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

/**
 * Данные события безопасности
 */
data class MobileSecurityEvent(
    val type: MobileSecurityEventType,
    val severity: MobileSecurityEventSeverity,
    val details: Map<String, String> = emptyMap(),
    val timestamp: Long = Clock.System.now().toEpochMilliseconds()
)

/**
 * Интерфейс для логирования событий безопасности на мобильных платформах
 */
interface MobileSecurityLogger {
    /**
     * Логирует событие безопасности
     * ВАЖНО: Не логирует пароли, токены, ключи или другую чувствительную информацию
     */
    fun log(event: MobileSecurityEvent)

    /**
     * Логирует успешное шифрование
     */
    fun logEncryptionSuccess(dataType: String) {
        log(
            MobileSecurityEvent(
                type = MobileSecurityEventType.ENCRYPTION_SUCCESS,
                severity = MobileSecurityEventSeverity.INFO,
                details = mapOf("data_type" to dataType)
            )
        )
    }

    /**
     * Логирует ошибку шифрования (без деталей пароля)
     */
    fun logEncryptionFailure(dataType: String, error: String) {
        log(
            MobileSecurityEvent(
                type = MobileSecurityEventType.ENCRYPTION_FAILURE,
                severity = MobileSecurityEventSeverity.ERROR,
                details = mapOf(
                    "data_type" to dataType,
                    "error" to error
                )
            )
        )
    }

    /**
     * Логирует успешную расшифровку
     */
    fun logDecryptionSuccess(dataType: String) {
        log(
            MobileSecurityEvent(
                type = MobileSecurityEventType.DECRYPTION_SUCCESS,
                severity = MobileSecurityEventSeverity.INFO,
                details = mapOf("data_type" to dataType)
            )
        )
    }

    /**
     * Логирует ошибку расшифровки (без деталей данных)
     */
    fun logDecryptionFailure(dataType: String, error: String) {
        log(
            MobileSecurityEvent(
                type = MobileSecurityEventType.DECRYPTION_FAILURE,
                severity = MobileSecurityEventSeverity.ERROR,
                details = mapOf(
                    "data_type" to dataType,
                    "error" to error
                )
            )
        )
    }

    /**
     * Логирует ошибку Keystore/Keychain
     */
    fun logKeystoreError(operation: String, error: String) {
        log(
            MobileSecurityEvent(
                type = MobileSecurityEventType.KEYSTORE_ERROR,
                severity = MobileSecurityEventSeverity.ERROR,
                details = mapOf(
                    "operation" to operation,
                    "error" to error
                )
            )
        )
    }

    /**
     * Логирует успешную проверку certificate pinning
     */
    fun logCertificatePinningSuccess(host: String) {
        log(
            MobileSecurityEvent(
                type = MobileSecurityEventType.CERTIFICATE_PINNING_SUCCESS,
                severity = MobileSecurityEventSeverity.INFO,
                details = mapOf("host" to host)
            )
        )
    }

    /**
     * Логирует ошибку certificate pinning (возможная MITM атака)
     */
    fun logCertificatePinningFailure(host: String, error: String) {
        log(
            MobileSecurityEvent(
                type = MobileSecurityEventType.CERTIFICATE_PINNING_FAILURE,
                severity = MobileSecurityEventSeverity.CRITICAL,
                details = mapOf(
                    "host" to host,
                    "error" to error
                )
            )
        )
    }

    /**
     * Логирует ошибку TLS
     */
    fun logTlsError(host: String, error: String) {
        log(
            MobileSecurityEvent(
                type = MobileSecurityEventType.TLS_ERROR,
                severity = MobileSecurityEventSeverity.ERROR,
                details = mapOf(
                    "host" to host,
                    "error" to error
                )
            )
        )
    }

    /**
     * Логирует подозрительную активность
     */
    fun logSuspiciousActivity(description: String, details: Map<String, String> = emptyMap()) {
        log(
            MobileSecurityEvent(
                type = MobileSecurityEventType.SUSPICIOUS_ACTIVITY,
                severity = MobileSecurityEventSeverity.WARNING,
                details = details + mapOf("description" to description)
            )
        )
    }
}

/**
 * Expect класс для платформо-специфичных реализаций
 */
expect class SecureMobileSecurityLogger() : MobileSecurityLogger {
    override fun log(event: MobileSecurityEvent)
}

/**
 * Фабрика для создания экземпляра логгера
 */
object MobileSecurityLoggerFactory {
    fun create(): MobileSecurityLogger {
        return SecureMobileSecurityLogger()
    }
}
