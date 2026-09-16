package com.company.ipcamera.platform.nas.common.service

import com.company.ipcamera.platform.nas.common.hardware.HardwareEncoder
import com.company.ipcamera.platform.nas.common.hardware.HardwareInfo
import kotlinx.coroutines.flow.Flow

/**
 * Unified NAS Platform Service Interface
 * Abstract interface for all NAS platform integrations
 */
interface NasPlatformService {
    
    /**
     * Get platform name
     */
    val platformName: String
    
    /**
     * Check if running on this NAS platform
     */
    suspend fun isCurrentPlatform(): Boolean
    
    /**
     * Initialize platform service
     */
    suspend fun initialize(): Result<Unit>
    
    /**
     * Send notification to NAS system
     */
    suspend fun sendNotification(message: String, severity: NotificationSeverity): Result<Unit>
    
    /**
     * Get system resources
     */
    suspend fun getSystemResources(): Result<SystemResources>
    
    /**
     * Create backup
     */
    suspend fun createBackup(destination: String, backupType: BackupType): Result<Unit>
    
    /**
     * Get hardware encoder for this platform
     */
    suspend fun getHardwareEncoder(): HardwareEncoder?
    
    /**
     * Get hardware information
     */
    suspend fun getHardwareInfo(): Result<HardwareInfo>
    
    /**
     * Shutdown platform service
     */
    suspend fun shutdown(): Result<Unit>
}

/**
 * Notification severity levels
 */
enum class NotificationSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

/**
 * Backup types
 */
enum class BackupType {
    FULL,
    INCREMENTAL,
    DIFFERENTIAL
}

/**
 * System resources data
 */
data class SystemResources(
    val cpuUsagePercent: Double,
    val memoryTotalBytes: Long,
    val memoryUsedBytes: Long,
    val memoryFreeBytes: Long,
    val diskTotalBytes: Long,
    val diskUsedBytes: Long,
    val diskFreeBytes: Long,
    val temperatureCelsius: Double?,
    val uptimeSeconds: Long
)
