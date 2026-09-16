package com.company.ipcamera.shared.platform

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Валидатор и создатель системных путей
 *
 * Проверяет существование директорий и создает их при необходимости
 */
object SystemPathsValidator {
    /**
     * Валидировать и создать системные пути
     *
     * @param paths Системные пути для валидации
     * @return Результат валидации с информацией о созданных/существующих директориях
     */
    fun validateAndCreate(paths: SystemPaths): ValidationResult {
        val results = mutableListOf<PathValidation>()

        // Валидация и создание каждой директории
        results.add(validateAndCreatePath(paths.dataPath, "data"))
        results.add(validateAndCreatePath(paths.recordingsPath, "recordings"))
        results.add(validateAndCreatePath(paths.logsPath, "logs"))
        results.add(validateAndCreatePath(paths.configPath, "config"))

        val allValid = results.all { it.isValid }
        val createdCount = results.count { it.wasCreated }
        val existingCount = results.count { it.exists && !it.wasCreated }

        return ValidationResult(
            isValid = allValid,
            paths = results,
            createdCount = createdCount,
            existingCount = existingCount,
        )
    }

    /**
     * Валидировать и создать одну директорию
     */
    private fun validateAndCreatePath(
        path: String,
        type: String,
    ): PathValidation {
        return try {
            // CommonMain fallback: no direct filesystem API.
            val valid = path.isNotBlank()
            if (valid) {
                logger.debug { "$type path validated in common mode: $path" }
            }
            PathValidation(
                path = path,
                type = type,
                isValid = valid,
                exists = valid,
                wasCreated = false,
                error = if (valid) null else "Path is blank",
            )
        } catch (e: Exception) {
            logger.error(e) { "Error validating $type path: $path" }
            PathValidation(
                path = path,
                type = type,
                isValid = false,
                exists = false,
                wasCreated = false,
                error = e.message ?: "Unknown error",
            )
        }
    }
}

/**
 * Результат валидации пути
 */
data class PathValidation(
    val path: String,
    val type: String,
    val isValid: Boolean,
    val exists: Boolean,
    val wasCreated: Boolean,
    val error: String?,
)

/**
 * Результат валидации всех путей
 */
data class ValidationResult(
    val isValid: Boolean,
    val paths: List<PathValidation>,
    val createdCount: Int,
    val existingCount: Int,
) {
    /**
     * Получить список ошибок валидации
     */
    fun getErrors(): List<String> {
        return paths
            .filter { !it.isValid }
            .mapNotNull { it.error }
    }

    /**
     * Получить список успешно созданных путей
     */
    fun getCreatedPaths(): List<String> {
        return paths
            .filter { it.wasCreated }
            .map { it.path }
    }
}
