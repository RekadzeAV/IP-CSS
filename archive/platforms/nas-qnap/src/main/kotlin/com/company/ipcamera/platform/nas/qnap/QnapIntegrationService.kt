package com.company.ipcamera.platform.nas.qnap

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * QNAP QTS Integration Service
 * Provides integration with QNAP NAS system APIs
 */
class QnapIntegrationService {
    
    private val qnotifyPath = "/usr/bin/qnotify"
    private val qnapConfigPath = "/etc/config/qpkg.conf"
    
    /**
     * Send notification to QNAP QTS
     */
    suspend fun sendNotification(
        message: String,
        severity: NotificationSeverity = NotificationSeverity.INFO
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val qnotify = File(qnotifyPath)
            
            if (!qnotify.exists() || !qnotify.canExecute()) {
                logger.warn { "qnotify not available" }
                return@withContext Result.failure(Exception("qnotify not found"))
            }
            
            val command = buildString {
                append(qnotifyPath)
                append(" -t \"IP-CSS\"")
                append(" -m \"${escapeShell(message)}\"")
            }
            
            val process = ProcessBuilder("sh", "-c", command)
                .redirectErrorStream(true)
                .start()
            
            val exitCode = process.waitFor()
            
            if (exitCode == 0) {
                logger.info { "Notification sent: $message" }
                Result.success(Unit)
            } else {
                logger.error { "Failed to send notification, exit code: $exitCode" }
                Result.failure(Exception("Notification failed"))
            }
        } catch (e: Exception) {
            logger.error(e) { "Error sending notification" }
            Result.failure(e)
        }
    }
    
    /**
     * Get QNAP system resources
     */
    suspend fun getSystemResources(): Result<QnapSystemResources> = withContext(Dispatchers.IO) {
        try {
            val cpuUsage = readCpuUsage()
            val memoryInfo = readMemoryInfo()
            val diskInfo = readDiskUsage()
            val systemInfo = readSystemInfo()
            
            Result.success(
                QnapSystemResources(
                    cpuUsagePercent = cpuUsage,
                    memoryTotalBytes = memoryInfo.total,
                    memoryUsedBytes = memoryInfo.used,
                    memoryFreeBytes = memoryInfo.free,
                    diskTotalBytes = diskInfo.total,
                    diskUsedBytes = diskInfo.used,
                    diskFreeBytes = diskInfo.free,
                    modelName = systemInfo.model,
                    firmwareVersion = systemInfo.firmwareVersion,
                    uptimeSeconds = systemInfo.uptime
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Error reading system resources" }
            Result.failure(e)
        }
    }
    
    /**
     * Create backup using QNAP Hybrid Backup Sync
     */
    suspend fun createBackup(
        destination: String,
        backupType: BackupType = BackupType.INCREMENTAL
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            logger.info { "Creating backup to $destination (type: $backupType)" }
            
            val backupDir = File(destination)
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            // Copy configuration
            val configDir = File("/share/CACHEDEV1_DATA/.qpkg/IP-CSS/data/config")
            if (configDir.exists()) {
                configDir.copyRecursively(File(backupDir, "config"))
            }
            
            // Copy database
            val databaseDir = File("/share/CACHEDEV1_DATA/.qpkg/IP-CSS/data/database")
            if (databaseDir.exists()) {
                databaseDir.copyRecursively(File(backupDir, "database"))
            }
            
            logger.info { "Backup completed successfully" }
            Result.success(Unit)
        } catch (e: Exception) {
            logger.error(e) { "Error creating backup" }
            Result.failure(e)
        }
    }
    
    /**
     * Get QNAP storage pool information
     */
    suspend fun getStoragePoolInfo(): Result<List<StoragePool>> = withContext(Dispatchers.IO) {
        try {
            val pools = mutableListOf<StoragePool>()
            
            // Read from /proc/partitions or use getcfg
            val process = ProcessBuilder("getcfg", "Storage", "List", "-f", "/etc/config/mtab")
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readLines()
            
            output.forEach { line ->
                val parts = line.split("=")
                if (parts.size >= 2) {
                    pools.add(
                        StoragePool(
                            name = parts[0],
                            path = parts[1],
                            status = "Active"
                        )
                    )
                }
            }
            
            Result.success(pools)
        } catch (e: Exception) {
            logger.error(e) { "Error reading storage pool info" }
            Result.failure(e)
        }
    }
    
    // Helper functions
    
    private suspend fun readCpuUsage(): Double {
        return try {
            val statFile = File("/proc/stat")
            val lines = statFile.readLines()
            val cpuLine = lines.first { it.startsWith("cpu ") }
            val values = cpuLine.split("\\s+".toRegex()).filter { it.isNotEmpty() }.map { it.toLong() }
            
            val idle = values[4]
            val total = values.sum()
            
            (1.0 - idle.toDouble() / total) * 100.0
        } catch (e: Exception) {
            logger.warn(e) { "Failed to read CPU usage" }
            0.0
        }
    }
    
    private suspend fun readMemoryInfo(): MemoryInfo {
        return try {
            val meminfoFile = File("/proc/meminfo")
            val lines = meminfoFile.readLines()
            
            val getValue = { key: String ->
                lines.find { it.startsWith(key) }
                    ?.split("\\s+".toRegex())
                    ?.getOrNull(1)
                    ?.toLongOrNull() ?: 0L
            }
            
            val totalKb = getValue("MemTotal:")
            val freeKb = getValue("MemFree:")
            val availableKb = getValue("MemAvailable:")
            
            MemoryInfo(
                total = totalKb * 1024,
                free = availableKb * 1024,
                used = (totalKb - availableKb) * 1024
            )
        } catch (e: Exception) {
            logger.warn(e) { "Failed to read memory info" }
            MemoryInfo(0, 0, 0)
        }
    }
    
    private suspend fun readDiskUsage(): DiskInfo {
        return try {
            val process = ProcessBuilder("df", "-B1", "/share/CACHEDEV1_DATA")
                .redirectErrorStream(true)
                .start()
            
            val output = process.inputStream.bufferedReader().readLines()
            
            if (output.size >= 2) {
                val values = output[1].split("\\s+".toRegex()).filter { it.isNotEmpty() }
                DiskInfo(
                    total = values[1].toLong(),
                    used = values[2].toLong(),
                    free = values[3].toLong()
                )
            } else {
                DiskInfo(0, 0, 0)
            }
        } catch (e: Exception) {
            logger.warn(e) { "Failed to read disk usage" }
            DiskInfo(0, 0, 0)
        }
    }
    
    private suspend fun readSystemInfo(): SystemInfo {
        return try {
            val modelFile = File("/etc/config/uLinux.conf")
            val model = if (modelFile.exists()) {
                modelFile.readLines()
                    .find { it.startsWith("Model") }
                    ?.split("=")
                    ?.getOrNull(1)
                    ?.trim() ?: "Unknown"
            } else "Unknown"
            
            val versionFile = File("/etc/config/version")
            val firmwareVersion = if (versionFile.exists()) {
                versionFile.readText().trim()
            } else "Unknown"
            
            val uptime = readUptime()
            
            SystemInfo(model, firmwareVersion, uptime)
        } catch (e: Exception) {
            logger.warn(e) { "Failed to read system info" }
            SystemInfo("Unknown", "Unknown", 0L)
        }
    }
    
    private suspend fun readUptime(): Long {
        return try {
            val uptimeFile = File("/proc/uptime")
            val uptime = uptimeFile.readText().trim().split(" ")[0].toDouble()
            uptime.toLong()
        } catch (e: Exception) {
            0L
        }
    }
    
    private fun escapeShell(text: String): String {
        return text.replace("\"", "\\\"")
            .replace("$", "\\$")
            .replace("`", "\\`")
    }
    
    // Data classes
    
    enum class NotificationSeverity {
        INFO, WARNING, ERROR, CRITICAL
    }
    
    enum class BackupType {
        FULL, INCREMENTAL, DIFFERENTIAL
    }
    
    data class QnapSystemResources(
        val cpuUsagePercent: Double,
        val memoryTotalBytes: Long,
        val memoryUsedBytes: Long,
        val memoryFreeBytes: Long,
        val diskTotalBytes: Long,
        val diskUsedBytes: Long,
        val diskFreeBytes: Long,
        val modelName: String,
        val firmwareVersion: String,
        val uptimeSeconds: Long
    )
    
    data class MemoryInfo(
        val total: Long,
        val used: Long,
        val free: Long
    )
    
    data class DiskInfo(
        val total: Long,
        val used: Long,
        val free: Long
    )
    
    data class SystemInfo(
        val model: String,
        val firmwareVersion: String,
        val uptime: Long
    )
    
    data class StoragePool(
        val name: String,
        val path: String,
        val status: String
    )
}
