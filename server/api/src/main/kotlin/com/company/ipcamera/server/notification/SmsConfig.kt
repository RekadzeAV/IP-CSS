package com.company.ipcamera.server.notification

/**
 * Конфигурация SMS-провайдера для backend-канала уведомлений.
 */
data class SmsConfig(
    val endpointUrl: String,
    val apiKey: String,
    val from: String? = null,
    val defaultTo: List<String> = emptyList(),
    val enabled: Boolean = true
) {
    companion object {
        fun fromEnvironment(): SmsConfig? {
            val endpointUrl = System.getenv("SMS_ENDPOINT_URL")?.trim().orEmpty()
            val apiKey = System.getenv("SMS_API_KEY")?.trim().orEmpty()
            if (endpointUrl.isBlank() || apiKey.isBlank()) return null

            val from = System.getenv("SMS_FROM")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            val defaultTo = System.getenv("SMS_DEFAULT_TO")
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                .orEmpty()
            val enabled = System.getenv("SMS_ENABLED")?.lowercase() != "false"

            return SmsConfig(
                endpointUrl = endpointUrl,
                apiKey = apiKey,
                from = from,
                defaultTo = defaultTo,
                enabled = enabled
            )
        }
    }
}
