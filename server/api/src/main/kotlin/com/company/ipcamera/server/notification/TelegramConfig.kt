package com.company.ipcamera.server.notification

/**
 * Telegram Bot configuration for notification delivery.
 */
data class TelegramConfig(
    val botToken: String,
    val chatIds: List<String>,
    val enabled: Boolean = true
) {
    companion object {
        /**
         * Reads config from environment:
         * - TELEGRAM_BOT_TOKEN (required)
         * - TELEGRAM_CHAT_ID (optional, single chat)
         * - TELEGRAM_CHAT_IDS (optional, comma-separated chat IDs)
         * - TELEGRAM_ENABLED (optional, default true)
         */
        fun fromEnvironment(): TelegramConfig? {
            val token = System.getenv("TELEGRAM_BOT_TOKEN")?.trim().orEmpty()
            if (token.isBlank()) return null

            val singleChatId = System.getenv("TELEGRAM_CHAT_ID")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            val multipleChatIds = System.getenv("TELEGRAM_CHAT_IDS")
                ?.split(",")
                ?.map { it.trim() }
                ?.filter { it.isNotBlank() }
                .orEmpty()

            val resolvedChatIds = buildList {
                if (singleChatId != null) add(singleChatId)
                addAll(multipleChatIds)
            }.distinct()

            if (resolvedChatIds.isEmpty()) return null

            val enabled = System.getenv("TELEGRAM_ENABLED")?.lowercase() != "false"
            return TelegramConfig(
                botToken = token,
                chatIds = resolvedChatIds,
                enabled = enabled
            )
        }
    }
}
