package com.company.ipcamera.server.notification

/**
 * Минимальная конфигурация push-канала (FCM-style HTTP endpoint).
 */
data class PushConfig(
    val endpointUrl: String,
    val apiKey: String,
    val enabled: Boolean = true
) {
    companion object {
        fun fromEnvironment(): PushConfig? {
            val endpointUrl = System.getenv("PUSH_ENDPOINT_URL")?.trim().orEmpty()
            val apiKey = System.getenv("PUSH_API_KEY")?.trim().orEmpty()
            if (endpointUrl.isBlank() || apiKey.isBlank()) return null
            val enabled = System.getenv("PUSH_ENABLED")?.lowercase() != "false"
            return PushConfig(
                endpointUrl = endpointUrl,
                apiKey = apiKey,
                enabled = enabled
            )
        }
    }
}
