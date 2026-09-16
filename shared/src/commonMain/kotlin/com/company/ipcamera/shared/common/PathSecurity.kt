package com.company.ipcamera.shared.common

/**
 * Утилита для защиты от Path Traversal атак
 *
 * Валидирует пути, чтобы предотвратить доступ к файлам вне разрешенных директорий
 */
object PathSecurity {
    /**
     * Проверяет, содержит ли путь опасные символы для path traversal
     */
    fun containsPathTraversal(path: String): Boolean {
        // Проверка на .. (parent directory)
        if (path.contains("..")) {
            return true
        }

        // Проверка на различные варианты path traversal
        val dangerousPatterns =
            listOf(
                "../",
                "..\\",
                "%2e%2e%2f", // URL encoded ../
                "%2e%2e%5c", // URL encoded ..\
                "..%2f", // URL encoded ../
                "..%5c", // URL encoded ..\
                "....//", // Двойной ../
                "....\\\\", // Двойной ..\
            )

        val lowerPath = path.lowercase()
        return dangerousPatterns.any { lowerPath.contains(it) }
    }

    /**
     * Валидирует путь относительно базовой директории
     *
     * @param path Путь для валидации
     * @param baseDirectory Базовая директория (например, documents, cache)
     * @return Валидированный нормализованный путь или null если путь небезопасен
     */
    fun validateAndNormalizePath(
        path: String,
        baseDirectory: String,
    ): String? {
        // Проверка на path traversal
        if (containsPathTraversal(path)) {
            return null
        }

        // Нормализуем базовую директорию (убираем trailing slash)
        val normalizedBase = baseDirectory.trimEnd('/', '\\')

        // Нормализуем путь
        val normalizedPath = normalizePath(path)

        // Проверяем, что путь не является абсолютным вне базовой директории
        if (normalizedPath.startsWith("/") ||
            (normalizedPath.length >= 2 && normalizedPath[1] == ':')
        ) {
            // Абсолютный путь - проверяем, что он начинается с базовой директории
            if (!normalizedPath.startsWith(normalizedBase, ignoreCase = true)) {
                return null
            }
        }

        // Строим полный путь
        val fullPath =
            if (normalizedPath.startsWith(normalizedBase, ignoreCase = true)) {
                normalizedPath
            } else {
                joinPath(normalizedBase, normalizedPath)
            }

        // Проверяем, что итоговый путь находится внутри базовой директории
        val resolvedPath = resolvePath(fullPath)
        if (!resolvedPath.startsWith(normalizedBase, ignoreCase = true)) {
            return null
        }

        return resolvedPath
    }

    /**
     * Нормализует путь (убирает лишние разделители, но не разрешает ..)
     */
    private fun normalizePath(path: String): String {
        // Заменяем обратные слеши на прямые для единообразия
        var normalized = path.replace('\\', '/')

        // Убираем множественные слеши
        while (normalized.contains("//")) {
            normalized = normalized.replace("//", "/")
        }

        // Убираем ведущие и завершающие слеши (кроме корня)
        normalized = normalized.trim('/')

        return normalized
    }

    /**
     * Разрешает путь (убирает . и .., но только если они безопасны)
     */
    private fun resolvePath(path: String): String {
        val parts = path.split('/').filter { it.isNotEmpty() && it != "." }
        val resolved = mutableListOf<String>()

        for (part in parts) {
            when (part) {
                ".." -> {
                    // Если есть родительская директория, удаляем её
                    if (resolved.isNotEmpty()) {
                        resolved.removeAt(resolved.size - 1)
                    } else {
                        // Попытка выйти за пределы корня - небезопасно
                        return path // Возвращаем исходный путь, валидация отклонит его
                    }
                }
                else -> resolved.add(part)
            }
        }

        return resolved.joinToString("/")
    }

    /**
     * Объединяет пути безопасным способом
     */
    private fun joinPath(
        base: String,
        relative: String,
    ): String {
        val normalizedBase = base.trimEnd('/', '\\')
        val normalizedRelative = relative.trimStart('/', '\\')
        return "$normalizedBase/$normalizedRelative"
    }

    /**
     * Проверяет, что путь находится внутри разрешенной директории
     *
     * @param filePath Полный путь к файлу
     * @param allowedDirectories Список разрешенных базовых директорий
     * @return true если путь безопасен
     */
    fun isPathWithinAllowedDirectories(
        filePath: String,
        allowedDirectories: List<String>,
    ): Boolean {
        val normalizedPath = normalizePath(filePath)

        return allowedDirectories.any { baseDir ->
            val normalizedBase = baseDir.trimEnd('/', '\\')
            normalizedPath.startsWith(normalizedBase, ignoreCase = true)
        }
    }

    /**
     * Санитизирует имя файла, удаляя опасные символы
     */
    fun sanitizeFileName(fileName: String): String {
        // Удаляем опасные символы
        var sanitized =
            fileName
                .replace("..", "")
                .replace("/", "_")
                .replace("\\", "_")
                .replace(":", "_")
                .replace("*", "_")
                .replace("?", "_")
                .replace("\"", "_")
                .replace("<", "_")
                .replace(">", "_")
                .replace("|", "_")

        // Убираем ведущие и завершающие точки и пробелы
        sanitized = sanitized.trim('.', ' ')

        // Если имя стало пустым, используем дефолтное
        if (sanitized.isEmpty()) {
            sanitized = "file"
        }

        return sanitized
    }
}
