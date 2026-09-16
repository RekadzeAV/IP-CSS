package com.company.ipcamera.core.common.security

/**
 * Input validator for preventing injections and invalid data
 */
object InputValidator {

    /**
     * Validates camera URL
     * Includes SSRF protection checks
     */
    fun validateCameraUrl(url: String): ValidationResult {
        if (url.isBlank()) {
            return ValidationResult.Error("URL cannot be empty")
        }

        // Length check
        if (url.length > 2048) {
            return ValidationResult.Error("URL is too long (maximum 2048 characters)")
        }

        // URL format check
        val urlPattern = Regex(
            "^https?://" + // Protocol
            "([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}" + // Domain
            "(:[0-9]{1,5})?" + // Port (optional)
            "(/.*)?$" // Path (optional)
        )

        val rtspPattern = Regex(
            "^rtsp://" + // RTSP protocol
            "([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}" + // Domain
            "(:[0-9]{1,5})?" + // Port (optional)
            "(/.*)?$" // Path (optional)
        )

        val rtspIpPattern = Regex(
            "^rtsp://" +
                "((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)" +
                "(:[0-9]{1,5})?" +
                "(/.*)?$"
        )

        val ipPattern = Regex(
            "^https?://" + // Protocol
            "((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)" + // IPv4
            "(:[0-9]{1,5})?" + // Port (optional)
            "(/.*)?$" // Path (optional)
        )

        if (!urlPattern.matches(url) && !rtspPattern.matches(url) && !rtspIpPattern.matches(url) &&
            !ipPattern.matches(url)
        ) {
            return ValidationResult.Error("Invalid URL format")
        }

        // SSRF protection - check for forbidden hostnames (common-safe parsing).
        val host = extractHost(url)
        if (host != null) {
            val forbiddenHostnames = setOf(
                "localhost", "127.0.0.1", "0.0.0.0", "::1",
                "metadata.google.internal", "169.254.169.254",
                "metadata.azure.com", "metadata.azure.net"
            )

            if (forbiddenHostnames.contains(host) ||
                host.startsWith("localhost.") ||
                host.endsWith(".localhost")
            ) {
                return ValidationResult.Error("URL host is not allowed (internal or metadata service)")
            }
        }

        // SQL injection check (basic)
        val sqlInjectionPatterns = listOf(
            "';", "--", "/*", "*/", "xp_", "sp_", "exec", "union", "select", "insert", "update", "delete", "drop"
        )
        val lowerUrl = url.lowercase()
        if (sqlInjectionPatterns.any { lowerUrl.contains(it) }) {
            return ValidationResult.Error("URL contains potentially dangerous characters")
        }

        // XSS check (basic)
        val xssPatterns = listOf("<script", "javascript:", "onerror=", "onload=")
        if (xssPatterns.any { lowerUrl.contains(it) }) {
            return ValidationResult.Error("URL contains potentially dangerous characters")
        }

        return ValidationResult.Success
    }

    /**
     * Проверяет, что при включённой настройке «только HTTPS» URL не является http://.
     * Используется для блокировки подключений к API и камерам по незашифрованному HTTP.
     *
     * @param url URL для проверки (камера, API и т.д.)
     * @param requireHttps true = включена настройка «только HTTPS», http:// запрещён
     * @return Error с сообщением, если requireHttps и url начинается с http://
     */
    fun validateUrlHttpsOnly(url: String, requireHttps: Boolean): ValidationResult {
        if (!requireHttps) return ValidationResult.Success
        val normalized = url.trim().lowercase()
        if (normalized.startsWith("http://")) {
            return ValidationResult.Error(
                "При включённой настройке «только HTTPS» подключение по адресу http:// запрещено. Используйте https://."
            )
        }
        return ValidationResult.Success
    }

    /**
     * Validates username
     */
    fun validateUsername(username: String?): ValidationResult {
        if (username == null) return ValidationResult.Success // Optional field

        if (username.isBlank()) {
            return ValidationResult.Error("Username cannot be empty")
        }

        // Length check
        if (username.length > 255) {
            return ValidationResult.Error("Username is too long (maximum 255 characters)")
        }

        // Allowed characters: letters, numbers, underscore, hyphen, dot
        val usernamePattern = Regex("^[a-zA-Z0-9._-]+$")
        if (!usernamePattern.matches(username)) {
            return ValidationResult.Error("Username contains invalid characters")
        }

        return ValidationResult.Success
    }

    /**
     * Валидирует пароль
     */
    fun validatePassword(password: String?): ValidationResult {
        if (password == null) return ValidationResult.Success // Опциональное поле

        // Проверка длины
        if (password.length > 1024) {
            return ValidationResult.Error("Пароль слишком длинный (максимум 1024 символа)")
        }

        // Проверка на SQL инъекции
        val sqlInjectionPatterns = listOf("';", "--", "/*", "*/")
        if (sqlInjectionPatterns.any { password.contains(it) }) {
            return ValidationResult.Error("Пароль содержит потенциально опасные символы")
        }

        return ValidationResult.Success
    }

    /**
     * Валидирует имя камеры
     */
    fun validateCameraName(name: String): ValidationResult {
        if (name.isBlank()) {
            return ValidationResult.Error("Имя камеры не может быть пустым")
        }

        // Проверка длины
        if (name.length > 255) {
            return ValidationResult.Error("Имя камеры слишком длинное (максимум 255 символов)")
        }

        // Проверка на XSS
        val xssPatterns = listOf("<script", "javascript:", "onerror=", "onload=")
        val lowerName = name.lowercase()
        if (xssPatterns.any { lowerName.contains(it) }) {
            return ValidationResult.Error("Имя камеры содержит потенциально опасные символы")
        }

        return ValidationResult.Success
    }

    /**
     * Санитизирует строку для безопасного использования в SQL
     */
    fun sanitizeForSql(input: String?): String {
        if (input == null) return ""
        // Удаляем потенциально опасные символы
        return input
            .replace("'", "''") // Экранирование одинарных кавычек
            .replace(";", "") // Удаление точки с запятой
            .replace("--", "") // Удаление комментариев
            .replace("/*", "") // Удаление комментариев
            .replace("*/", "") // Удаление комментариев
    }

    /**
     * Санитизирует строку для безопасного использования в XML
     */
    fun sanitizeForXml(input: String): String {
        return input
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    /**
     * Санитизирует строку для безопасного использования в JSON
     */
    fun sanitizeForJson(input: String): String {
        return input
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun extractHost(url: String): String? {
        val schemeEnd = url.indexOf("://")
        if (schemeEnd <= 0) return null
        val hostStart = schemeEnd + 3
        if (hostStart >= url.length) return null
        val pathStart = url.indexOf('/', hostStart).let { if (it == -1) url.length else it }
        val authority = url.substring(hostStart, pathStart)
        val hostPort = authority.substringAfter('@', authority)
        val host = hostPort.substringBefore(':').lowercase()
        return host.ifBlank { null }
    }
}

/**
 * Результат валидации
 */
sealed class ValidationResult {
    object Success : ValidationResult()
    data class Error(val message: String) : ValidationResult()

    val isSuccess: Boolean get() = this is Success
    val isError: Boolean get() = this is Error
}



