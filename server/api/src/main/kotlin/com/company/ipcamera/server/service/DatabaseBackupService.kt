package com.company.ipcamera.server.service

import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}

/**
 * Сервис для резервного копирования базы данных
 *
 * Поддерживает автоматическое и ручное резервное копирование PostgreSQL
 */
class DatabaseBackupService(
    private val dataSource: DataSource,
    private val backupDirectory: String = "backups/database"
) {
    private val backupDir = Paths.get(backupDirectory)
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")

    init {
        // Создаем директорию для бэкапов, если она не существует
        backupDir.toFile().mkdirs()
    }

    /**
     * Создать резервную копию базы данных
     *
     * @param customName Кастомное имя файла (опционально)
     * @return Путь к файлу резервной копии
     */
    suspend fun createBackup(customName: String? = null): Result<Path> = withContext(Dispatchers.IO) {
        try {
            if (dataSource !is HikariDataSource) {
                return@withContext Result.failure(
                    IllegalArgumentException("Backup is only supported for HikariDataSource with PostgreSQL")
                )
            }

            val jdbcUrl = dataSource.jdbcUrl
            if (!jdbcUrl.startsWith("jdbc:postgresql://")) {
                return@withContext Result.failure(
                    IllegalArgumentException("Backup is only supported for PostgreSQL")
                )
            }

            // Извлекаем имя базы данных из URL
            val dbName = extractDatabaseName(jdbcUrl)
            val timestamp = LocalDateTime.now().format(dateFormatter)
            val backupFileName = customName ?: "backup_${dbName}_$timestamp.sql"
            val backupPath = backupDir.resolve(backupFileName)

            // Используем pg_dump для создания резервной копии
            val username = dataSource.username
            val password = dataSource.password
            val host = extractHost(jdbcUrl)
            val port = extractPort(jdbcUrl) ?: 5432

            // Создаем команду pg_dump
            val processBuilder = ProcessBuilder(
                "pg_dump",
                "-h", host,
                "-p", port.toString(),
                "-U", username,
                "-d", dbName,
                "-F", "c", // Custom format (сжатый)
                "-f", backupPath.toString()
            )

            // Устанавливаем переменную окружения для пароля
            val env = processBuilder.environment()
            env["PGPASSWORD"] = password

            logger.info { "Creating database backup: $backupFileName" }

            val process = processBuilder.start()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                logger.info { "Database backup created successfully: $backupPath" }
                Result.success(backupPath)
            } else {
                val error = process.errorStream.bufferedReader().readText()
                logger.error { "Failed to create database backup. Exit code: $exitCode, Error: $error" }
                Result.failure(RuntimeException("pg_dump failed with exit code $exitCode: $error"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error creating database backup" }
            Result.failure(e)
        }
    }

    /**
     * Восстановить базу данных из резервной копии
     *
     * @param backupPath Путь к файлу резервной копии
     * @return Результат операции
     */
    suspend fun restoreBackup(backupPath: Path): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (dataSource !is HikariDataSource) {
                return@withContext Result.failure(
                    IllegalArgumentException("Restore is only supported for HikariDataSource with PostgreSQL")
                )
            }

            if (!Files.exists(backupPath)) {
                return@withContext Result.failure(
                    IllegalArgumentException("Backup file not found: $backupPath")
                )
            }

            val jdbcUrl = dataSource.jdbcUrl
            if (!jdbcUrl.startsWith("jdbc:postgresql://")) {
                return@withContext Result.failure(
                    IllegalArgumentException("Restore is only supported for PostgreSQL")
                )
            }

            val dbName = extractDatabaseName(jdbcUrl)
            val username = dataSource.username
            val password = dataSource.password
            val host = extractHost(jdbcUrl)
            val port = extractPort(jdbcUrl) ?: 5432

            logger.warn { "Restoring database from backup: $backupPath" }

            // Создаем команду pg_restore
            val processBuilder = ProcessBuilder(
                "pg_restore",
                "-h", host,
                "-p", port.toString(),
                "-U", username,
                "-d", dbName,
                "-c", // Clean (drop objects before recreating)
                "-v", // Verbose
                backupPath.toString()
            )

            val env = processBuilder.environment()
            env["PGPASSWORD"] = password

            val process = processBuilder.start()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                logger.info { "Database restored successfully from: $backupPath" }
                Result.success(Unit)
            } else {
                val error = process.errorStream.bufferedReader().readText()
                logger.error { "Failed to restore database. Exit code: $exitCode, Error: $error" }
                Result.failure(RuntimeException("pg_restore failed with exit code $exitCode: $error"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error restoring database backup" }
            Result.failure(e)
        }
    }

    /**
     * Получить список резервных копий
     */
    suspend fun listBackups(): List<BackupInfo> = withContext(Dispatchers.IO) {
        try {
            backupDir.toFile().listFiles()
                ?.filter { it.isFile && it.name.endsWith(".sql") || it.name.endsWith(".dump") }
                ?.map { file ->
                    BackupInfo(
                        fileName = file.name,
                        path = file.toPath(),
                        size = file.length(),
                        createdAt = file.lastModified()
                    )
                }
                ?.sortedByDescending { it.createdAt }
                ?: emptyList()
        } catch (e: Exception) {
            logger.error(e) { "Error listing backups" }
            emptyList()
        }
    }

    /**
     * Удалить старые резервные копии
     *
     * @param keepDays Количество дней для хранения (по умолчанию: 30)
     * @return Количество удаленных файлов
     */
    suspend fun cleanupOldBackups(keepDays: Int = 30): Int = withContext(Dispatchers.IO) {
        try {
            val cutoffTime = System.currentTimeMillis() - (keepDays * 24 * 60 * 60 * 1000L)
            val backups = listBackups()
            var deletedCount = 0

            backups.forEach { backup ->
                if (backup.createdAt < cutoffTime) {
                    try {
                        Files.delete(backup.path)
                        deletedCount++
                        logger.info { "Deleted old backup: ${backup.fileName}" }
                    } catch (e: Exception) {
                        logger.error(e) { "Error deleting backup: ${backup.fileName}" }
                    }
                }
            }

            logger.info { "Cleaned up $deletedCount old backup(s)" }
            deletedCount
        } catch (e: Exception) {
            logger.error(e) { "Error cleaning up old backups" }
            0
        }
    }

    /**
     * Извлечь имя базы данных из JDBC URL
     */
    private fun extractDatabaseName(jdbcUrl: String): String {
        val parts = jdbcUrl.split("/")
        return parts.last().split("?").first()
    }

    /**
     * Извлечь хост из JDBC URL
     */
    private fun extractHost(jdbcUrl: String): String {
        val url = jdbcUrl.removePrefix("jdbc:postgresql://")
        return url.split(":")[0].split("/")[0]
    }

    /**
     * Извлечь порт из JDBC URL
     */
    private fun extractPort(jdbcUrl: String): Int? {
        val url = jdbcUrl.removePrefix("jdbc:postgresql://")
        val parts = url.split(":")
        return if (parts.size > 1) {
            parts[1].split("/")[0].toIntOrNull()
        } else {
            null
        }
    }
}

/**
 * Информация о резервной копии
 */
data class BackupInfo(
    val fileName: String,
    val path: Path,
    val size: Long,
    val createdAt: Long
)
