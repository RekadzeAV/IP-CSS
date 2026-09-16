package com.company.ipcamera.server.notification

/**
 * Конфигурация SMTP для отправки email-уведомлений (B.1).
 * Значения берутся из переменных окружения или настроек.
 */
data class SmtpConfig(
    val host: String,
    val port: Int = 587,
    val username: String? = null,
    val password: String? = null,
    val fromAddress: String,
    val fromName: String? = null,
    val useTls: Boolean = true,
    val enabled: Boolean = true
) {
    companion object {
        private const val PREFIX = "SMTP_"

        /**
         * Создать конфиг из переменных окружения:
         * SMTP_HOST, SMTP_PORT, SMTP_USERNAME, SMTP_PASSWORD,
         * SMTP_FROM_ADDRESS, SMTP_FROM_NAME, SMTP_USE_TLS, SMTP_ENABLED
         */
        fun fromEnvironment(): SmtpConfig? {
            val host = System.getenv("${PREFIX}HOST") ?: return null
            if (host.isBlank()) return null
            return SmtpConfig(
                host = host.trim(),
                port = System.getenv("${PREFIX}PORT")?.toIntOrNull() ?: 587,
                username = System.getenv("${PREFIX}USERNAME")?.takeIf { it.isNotBlank() },
                password = System.getenv("${PREFIX}PASSWORD")?.takeIf { it.isNotBlank() },
                fromAddress = System.getenv("${PREFIX}FROM_ADDRESS")?.trim() ?: "noreply@localhost",
                fromName = System.getenv("${PREFIX}FROM_NAME")?.takeIf { it.isNotBlank() },
                useTls = System.getenv("${PREFIX}USE_TLS")?.lowercase() != "false",
                enabled = System.getenv("${PREFIX}ENABLED")?.lowercase() != "false"
            )
        }
    }
}
