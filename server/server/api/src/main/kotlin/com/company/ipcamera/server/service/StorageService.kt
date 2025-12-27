package com.company.ipcamera.server.service

import mu.KotlinLogging
import java.io.File

private val logger = KotlinLogging.logger {}

/**
 * Сервис для управления дисковым пространством
 * 
 * Предоставляет функциональность для:
 * - Проверки доступного места на диске
 * - Управления квотами на запись
 * - Мониторинга использования дискового пространства
 */
class StorageService(
    private val recordingsDirectory: String = "recordings",
    private val maxStorageBytes: Long? = null, // null = без ограничений
    private val warningThreshold: Double = 0.8 // 80% использования
) {
    
    /**
     * Получить доступное место на диске в байтах
     */
    fun getAvailableSpace(): Long {
        return try {
            val directory = File(recordingsDirectory)
            if (!directory.exists()) {
                directory.mkdirs()
            }
            directory.usableSpace
        } catch (e: Exception) {
            logger.error(e) { "Error getting available space" }
            0L
        }
    }
    
    /**
     * Получить общий размер диска в байтах
     */
    fun getTotalSpace(): Long {
        return try {
            val directory = File(recordingsDirectory)
            if (!directory.exists()) {
                directory.mkdirs()
            }
            directory.totalSpace
        } catch (e: Exception) {
            logger.error(e) { "Error getting total space" }
            0L
        }
    }
    
    /**
     * Получить используемое место в байтах
     */
    fun getUsedSpace(): Long {
        return try {
            val directory = File(recordingsDirectory)
            if (!directory.exists()) {
                return 0L
            }
            calculateDirectorySize(directory)
        } catch (e: Exception) {
            logger.error(e) { "Error calculating used space" }
            0L
        }
    }
    
    /**
     * Проверить, достаточно ли места для записи указанного размера
     */
    fun hasEnoughSpace(requiredBytes: Long): Boolean {
        val available = getAvailableSpace()
        
        // Проверяем квоту, если установлена
        if (maxStorageBytes != null) {
            val used = getUsedSpace()
            val remaining = maxStorageBytes - used
            if (remaining < requiredBytes) {
                logger.warn { 
                    "Storage quota exceeded: used=$used, max=$maxStorageBytes, required=$requiredBytes" 
                }
                return false
            }
        }
        
        // Проверяем доступное место на диске
        if (available < requiredBytes) {
            logger.warn { 
                "Not enough disk space: available=$available, required=$requiredBytes" 
            }
            return false
        }
        
        return true
    }
    
    /**
     * Получить процент использования дискового пространства
     */
    fun getUsagePercentage(): Double {
        val total = getTotalSpace()
        if (total == 0L) return 0.0
        
        val used = getUsedSpace()
        return (used.toDouble() / total.toDouble()) * 100.0
    }
    
    /**
     * Проверить, превышен ли порог предупреждения
     */
    fun isWarningThresholdExceeded(): Boolean {
        val usage = getUsagePercentage() / 100.0
        return usage >= warningThreshold
    }
    
    /**
     * Получить информацию о хранилище
     */
    fun getStorageInfo(): StorageInfo {
        val total = getTotalSpace()
        val used = getUsedSpace()
        val available = getAvailableSpace()
        val usagePercentage = getUsagePercentage()
        
        return StorageInfo(
            totalBytes = total,
            usedBytes = used,
            availableBytes = available,
            usagePercentage = usagePercentage,
            warningThresholdExceeded = isWarningThresholdExceeded(),
            quotaBytes = maxStorageBytes,
            quotaUsedBytes = if (maxStorageBytes != null) used else null,
            quotaRemainingBytes = if (maxStorageBytes != null) {
                maxOf(0L, maxStorageBytes - used)
            } else null
        )
    }
    
    /**
     * Рекурсивно вычисляет размер директории
     */
    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        
        if (directory.exists() && directory.isDirectory) {
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    size += if (file.isDirectory) {
                        calculateDirectorySize(file)
                    } else {
                        file.length()
                    }
                }
            }
        } else if (directory.exists() && directory.isFile) {
            size = directory.length()
        }
        
        return size
    }
    
    /**
     * Информация о хранилище
     */
    data class StorageInfo(
        val totalBytes: Long,
        val usedBytes: Long,
        val availableBytes: Long,
        val usagePercentage: Double,
        val warningThresholdExceeded: Boolean,
        val quotaBytes: Long?,
        val quotaUsedBytes: Long?,
        val quotaRemainingBytes: Long?
    ) {
        fun getFormattedTotal(): String = formatBytes(totalBytes)
        fun getFormattedUsed(): String = formatBytes(usedBytes)
        fun getFormattedAvailable(): String = formatBytes(availableBytes)
        fun getFormattedQuota(): String? = quotaBytes?.let { formatBytes(it) }
        fun getFormattedQuotaUsed(): String? = quotaUsedBytes?.let { formatBytes(it) }
        fun getFormattedQuotaRemaining(): String? = quotaRemainingBytes?.let { formatBytes(it) }
        
        private fun formatBytes(bytes: Long): String {
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            val tb = gb / 1024.0
            
            return when {
                tb >= 1 -> String.format("%.2f TB", tb)
                gb >= 1 -> String.format("%.2f GB", gb)
                mb >= 1 -> String.format("%.2f MB", mb)
                kb >= 1 -> String.format("%.2f KB", kb)
                else -> "$bytes bytes"
            }
        }
    }
}

