package com.company.ipcamera.core.common.security

/**
 * Валидатор входных данных для предотвращения инъекций и некорректных данных
 */
object InputValidator {

    /**
     * Валидирует URL камеры
     */
    fun validateCameraUrl(url: String): ValidationResult {
        if (url.isBlank()) {
            return ValidationResult.Error("URL не может быть пустым")
        }

        // Проверка длины
        if (url.length > 2048) {
            return ValidationResult.Error("URL слишком длинный (максимум 2048 символов)")
        }

        // Проверка формата URL
        val urlPattern = Regex(
            "^https?://" + // Протокол
            "([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}" + // Домен
            "(:[0-9]{1,5})?" + // Порт (опционально)
            "(/.*)?$" // Путь (опционально)
        )

        val rtspPattern = Regex(
            "^rtsp://" + // RTSP протокол
            "([a-zA-Z0-9]([a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}" + // Домен
            "(:[0-9]{1,5})?" + // Порт (опционально)
            "(/.*)?$" // Путь (опционально)
        )

        val ipPattern = Regex(
            "^https?://" + // Протокол
            "((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)" + // IPv4
            "(:[0-9]{1,5})?" + // Порт (опционально)
            "(/.*)?$" // Путь (опционально)
        )

        if (!urlPattern.matches(url) && !rtspPattern.matches(url) && !ipPattern.matches(url)) {
            return ValidationResult.Error("Некорректный формат URL")
        }

        // Проверка на SQL инъекции (базовая)
        val sqlInjectionPatterns = listOf(
            "';", "--", "/*", "*/", "xp_", "sp_", "exec", "union", "select", "insert", "update", "delete", "drop"
        )
        val lowerUrl = url.lowercase()
        if (sqlInjectionPatterns.any { lowerUrl.contains(it) }) {
            return ValidationResult.Error("URL содержит потенциально опасные символы")
        }

        // Проверка на XSS (базовая)
        val xssPatterns = listOf("<script", "javascript:", "onerror=", "onload=")
        if (xssPatterns.any { lowerUrl.contains(it) }) {
            return ValidationResult.Error("URL содержит потенциально опасные символы")
        }

        return ValidationResult.Success
    }

    /**
     * Валидирует имя пользователя
     */
    fun validateUsername(username: String?): ValidationResult {
        if (username == null) return ValidationResult.Success // Опциональное поле

        if (username.isBlank()) {
            return ValidationResult.Error("Имя пользователя не может быть пустым")
        }

        // Проверка длины
        if (username.length > 255) {
            return ValidationResult.Error("Имя пользователя слишком длинное (максимум 255 символов)")
        }

        // Разрешенные символы: буквы, цифры, подчеркивание, дефис, точка
        val usernamePattern = Regex("^[a-zA-Z0-9._-]+$")
        if (!usernamePattern.matches(username)) {
            return ValidationResult.Error("Имя пользователя содержит недопустимые символы")
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

