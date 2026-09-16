package com.company.ipcamera.server.validation

object WebhookQueryValidator {
    fun parseSuccess(raw: String?): Boolean? {
        if (raw == null) return null
        return when (raw.lowercase()) {
            "true" -> true
            "false" -> false
            else -> throw IllegalArgumentException("Invalid 'success' query parameter. Use 'true' or 'false'.")
        }
    }

    fun parseTimeRange(fromRaw: String?, toRaw: String?): Pair<Long?, Long?> {
        val from = fromRaw?.toLongOrNull()
        val to = toRaw?.toLongOrNull()
        if (fromRaw != null && from == null) {
            throw IllegalArgumentException("Invalid 'from' query parameter. Must be a unix timestamp.")
        }
        if (toRaw != null && to == null) {
            throw IllegalArgumentException("Invalid 'to' query parameter. Must be a unix timestamp.")
        }
        if (from != null && to != null && from > to) {
            throw IllegalArgumentException("Invalid time range: 'from' cannot be greater than 'to'.")
        }
        return from to to
    }

    fun parsePage(raw: String?): Int {
        if (raw == null) return 1
        val page = raw.toIntOrNull()
            ?: throw IllegalArgumentException("Invalid 'page' query parameter. Must be an integer.")
        if (page < 1) {
            throw IllegalArgumentException("Invalid 'page' query parameter. Must be >= 1.")
        }
        return page
    }

    fun parseLimit(raw: String?): Int {
        if (raw == null) return 100
        val limit = raw.toIntOrNull()
            ?: throw IllegalArgumentException("Invalid 'limit' query parameter. Must be an integer.")
        if (limit !in 1..500) {
            throw IllegalArgumentException("Invalid 'limit' query parameter. Must be between 1 and 500.")
        }
        return limit
    }
}
