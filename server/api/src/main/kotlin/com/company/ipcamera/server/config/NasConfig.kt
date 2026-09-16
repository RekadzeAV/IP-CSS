package com.company.ipcamera.server.config

import com.company.ipcamera.shared.platform.NasPlatformDetector
import com.company.ipcamera.shared.platform.NasPlatformDetectorImpl
import com.company.ipcamera.shared.platform.SystemPaths
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Конфигурация для NAS платформ
 *
 * Автоматически определяет платформу и настраивает системные пути
 */
object NasConfig {
    private val detector: NasPlatformDetector = NasPlatformDetectorImpl()
    private var systemPaths: SystemPaths? = null

    /**
     * Инициализирует конфигурацию NAS
     * Определяет платформу и получает системные пути
     */
    fun initialize(): SystemPaths {
        if (systemPaths == null) {
            val platform = detector.detectPlatform()

            if (platform != null) {
                logger.info { "Detected NAS platform: $platform" }
                systemPaths = detector.getSystemPaths()

                // Создаем директории, если они не существуют
                createDirectories(systemPaths!!)

                logger.info {
                    "NAS paths configured: " +
                    "data=${systemPaths!!.dataPath}, " +
                    "recordings=${systemPaths!!.recordingsPath}, " +
                    "logs=${systemPaths!!.logsPath}, " +
                    "config=${systemPaths!!.configPath}"
                }
            } else {
                logger.info { "Not running on NAS platform, using default paths" }
                systemPaths = SystemPaths.default()
            }
        }

        return systemPaths!!
    }

    /**
     * Получить системные пути
     * Если не инициализировано, инициализирует автоматически
     */
    fun getSystemPaths(): SystemPaths {
        return systemPaths ?: initialize()
    }

    /**
     * Проверить, запущено ли на NAS платформе
     */
    fun isRunningOnNas(): Boolean {
        return detector.isRunningOnNas()
    }

    /**
     * Получить путь к директории записей
     */
    fun getRecordingsPath(): String {
        return getSystemPaths().recordingsPath
    }

    /**
     * Получить путь к директории данных
     */
    fun getDataPath(): String {
        return getSystemPaths().dataPath
    }

    /**
     * Получить путь к директории логов
     */
    fun getLogsPath(): String {
        return getSystemPaths().logsPath
    }

    /**
     * Получить путь к директории конфигурации
     */
    fun getConfigPath(): String {
        return getSystemPaths().configPath
    }

    /**
     * Создает необходимые директории
     */
    private fun createDirectories(paths: SystemPaths) {
        try {
            listOf(
                paths.dataPath,
                paths.recordingsPath,
                paths.logsPath,
                paths.configPath
            ).forEach { path ->
                val dir = File(path)
                if (!dir.exists()) {
                    dir.mkdirs()
                    logger.info { "Created directory: $path" }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error creating NAS directories" }
        }
    }
}
