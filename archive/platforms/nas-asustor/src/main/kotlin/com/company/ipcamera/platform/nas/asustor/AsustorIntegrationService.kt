package com.company.ipcamera.platform.nas.asustor

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Asustor ADM Integration Service
 * Provides integration with Asustor NAS system APIs
 */
class AsustorIntegrationService {
    
    private val actrlPath = "/usr/bin/actrl"
    
    /**
     * Send notification to Asustor ADM
     */
    suspend fun sendNotification(
        message: String,
        severity: NotificationSeverity = NotificationSeverity.INFO
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val command = buildString {
                append(actrlPath)
                append(" --notify \"IP-CSS: ${escapeShell(message)}\"")
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
     * Get Asustor system resources
     */
    suspend fun getSystemResources(): Result<AsustorSystemResources> = withContext(Dispatchers.IO) {
        try {
            val cpuUsage = readCpuUsage()
            val memoryInfo = readMemoryInfo()
            val diskInfo = readDiskUsage()
            
            Result.success(
                AsustorSystemResources(
                    cpuUsagePercent = cpuUsage,
                    memoryTotalBytes = memoryInfo.total,
                    memoryUsedBytes = memoryInfo.used,
                    memoryFreeBytes = memoryInfo.free,
                    diskTotalBytes = diskInfo.total,
                    diskUsedBytes = diskInfo.used,
                    diskFreeBytes = diskInfo.free
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Error reading system resources" }
            Result.failure(e)
        }
    }
    
    /**
     * Integrate with Asustor AiCentral
     */
    suspend fun getAiCentralStatus(): Result<AiCentralStatus> = withContext(Dispatchers.IO) {
        try {
            // Check if AiCenter is running
            val aicenterRunning = File("/var/run/aicenter.pid").exists()
            
            Result.success(
                AiCentralStatus(
                    enabled = aicenterRunning,
                    status = if (aicenterRunning) "Running" else "Stopped"
                )
            )
        } catch (e: Exception) {
            logger.error(e) { "Error reading AiCenter status" }
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
            val availableKb = getValue("MemAvailable:")
            
            MemoryInfo(
                total = totalKb * 1024,
                free = availableKb * 1024,
                used = (totalKb - availableKb) * 1024
            )
        } catch (e: Exception) {
            MemoryInfo(0, 0, 0)
        }
    }
    
    private suspend fun readDiskUsage(): DiskInfo {
        return try {
            val process = ProcessBuilder("df", "-B1", "/share/MD0_DATA")
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
            DiskInfo(0, 0, 0)
        }
    }
    
    private fun escapeShell(text: String): String {
        return text.replace("\"", "\\\"")
            .replace("$", "\\$")
    }
    
    // Data classes
    
    enum class NotificationSeverity {
        INFO, WARNING, ERROR, CRITICAL
    }
    
    data class AsustorSystemResources(
        val cpuUsagePercent: Double,
        val memoryTotalBytes: Long,
        val memoryUsedBytes: Long,
        val memoryFreeBytes: Long,
        val diskTotalBytes: Long,
        val diskUsedBytes: Long,
        val diskFreeBytes: Long
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
    
    data class AiCentralStatus(
        val enabled: Boolean,
        val status: String
    )
}
