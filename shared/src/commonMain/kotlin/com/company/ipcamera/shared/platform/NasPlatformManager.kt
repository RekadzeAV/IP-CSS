package com.company.ipcamera.shared.platform

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Менеджер для работы с NAS платформой
 *
 * Объединяет оба варианта определения платформы:
 * 1. Через системные файлы (FileBasedNasDetector)
 * 2. Через API (ApiBasedNasDetector)
 *
 * Использование:
 * ```kotlin
 * val manager = NasPlatformManager.create()
 * val platform = manager.getPlatform()
 * val paths = manager.getSystemPaths()
 * ```
 */
class NasPlatformManager private constructor(
    private val detector: NasPlatformDetector,
) {
    private var cachedPlatform: NasPlatform? = null
    private var cachedPaths: SystemPaths? = null

    companion object {
        /**
         * Создать менеджер с комбинированным детектором (файлы + API)
         */
        fun create(useApiDetection: Boolean = true): NasPlatformManager {
            val detector = NasPlatformDetectorFactory.create(useApiDetection)
            return NasPlatformManager(detector)
        }

        /**
         * Создать менеджер только с файловым детектором (быстрый вариант)
         */
        fun createFileBased(): NasPlatformManager {
            val detector = NasPlatformDetectorFactory.createFileBased()
            return NasPlatformManager(detector)
        }

        /**
         * Создать менеджер только с API детектором (для тестирования)
         */
        fun createApiBased(): NasPlatformManager {
            val detector = NasPlatformDetectorFactory.createApiBased()
            return NasPlatformManager(detector)
        }
    }

    /**
     * Получить определенную платформу
     *
     * Использует кэширование для избежания повторных проверок
     */
    fun getPlatform(): NasPlatform? {
        if (cachedPlatform == null) {
            cachedPlatform = detector.detectPlatform()
        }
        return cachedPlatform
    }

    /**
     * Получить системные пути
     *
     * Использует кэширование и автоматически валидирует/создает пути
     */
    fun getSystemPaths(validate: Boolean = true): SystemPaths {
        if (cachedPaths == null) {
            cachedPaths = detector.getSystemPaths()
        }

        val paths = cachedPaths!!

        // Валидация и создание директорий при необходимости
        if (validate) {
            val validationResult = SystemPathsValidator.validateAndCreate(paths)

            if (!validationResult.isValid) {
                logger.warn {
                    "Some system paths are invalid. Errors: ${validationResult.getErrors().joinToString()}"
                }
            } else {
                logger.info {
                    "System paths validated. Created: ${validationResult.createdCount}, " +
                        "Existing: ${validationResult.existingCount}"
                }
            }
        }

        return paths
    }

    /**
     * Проверить, запущено ли приложение на NAS
     */
    fun isRunningOnNas(): Boolean {
        return detector.isRunningOnNas()
    }

    /**
     * Получить информацию о платформе в виде строки
     */
    fun getPlatformInfo(): String {
        val platform = getPlatform() ?: return "Unknown platform"
        val paths = getSystemPaths(validate = false)

        return buildString {
            appendLine("NAS Platform: ${platform.name}")
            appendLine("Data Path: ${paths.dataPath}")
            appendLine("Recordings Path: ${paths.recordingsPath}")
            appendLine("Logs Path: ${paths.logsPath}")
            appendLine("Config Path: ${paths.configPath}")
        }
    }

    /**
     * Сбросить кэш (для повторного определения)
     */
    fun clearCache() {
        cachedPlatform = null
        cachedPaths = null
    }
}
