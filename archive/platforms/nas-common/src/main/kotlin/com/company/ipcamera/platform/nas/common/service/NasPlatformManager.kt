package com.company.ipcamera.platform.nas.common.service

import com.company.ipcamera.platform.nas.asustor.AustorIntegrationService
import com.company.ipcamera.platform.nas.common.hardware.HardwareEncoder
import com.company.ipcamera.platform.nas.common.hardware.amd.AmdVCEEncoder
import com.company.ipcamera.platform.nas.common.hardware.arm.ArmMaliEncoder
import com.company.ipcamera.platform.nas.common.hardware.intel.IntelQuickSyncEncoder
import com.company.ipcamera.platform.nas.common.hardware.nvidia.NvidiaNVENCEncoder
import com.company.ipcamera.platform.nas.qnap.QnapIntegrationService
import com.company.ipcamera.platform.nas.synology.SynologyIntegrationService
import com.company.ipcamera.platform.nas.truenas.TrueNasIntegrationService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * NAS Platform Manager
 * Discovers and manages available NAS platform services
 */
class NasPlatformManager {
    
    private val platformServices = mutableListOf<NasPlatformService>()
    private var activeService: NasPlatformService? = null
    
    init {
        // Register all platform services
        registerService(SynologyNasPlatformService())
        registerService(QnapNasPlatformService())
        registerService(AsustorNasPlatformService())
        registerService(TrueNasPlatformService())
    }
    
    /**
     * Register a platform service
     */
    fun registerService(service: NasPlatformService) {
        platformServices.add(service)
        logger.debug { "Registered NAS platform service: ${service.platformName}" }
    }
    
    /**
     * Initialize and detect current platform
     */
    suspend fun initialize(): Result<Unit> {
        logger.info { "Initializing NAS Platform Manager" }
        
        for (service in platformServices) {
            if (service.isCurrentPlatform()) {
                logger.info { "Detected NAS platform: ${service.platformName}" }
                
                service.initialize()
                    .onSuccess {
                        activeService = service
                        logger.info { "Initialized ${service.platformName} platform service" }
                    }
                    .onFailure { error ->
                        logger.error(error) { "Failed to initialize ${service.platformName}" }
                    }
                
                return Result.success(Unit)
            }
        }
        
        logger.warn { "No supported NAS platform detected" }
        return Result.failure(Exception("No supported NAS platform detected"))
    }
    
    /**
     * Get current platform service
     */
    fun getCurrentService(): NasPlatformService? = activeService
    
    /**
     * Check if running on NAS
     */
    suspend fun isRunningOnNas(): Boolean {
        for (service in platformServices) {
            if (service.isCurrentPlatform()) {
                return true
            }
        }
        return false
    }
    
    /**
     * Send notification
     */
    suspend fun sendNotification(message: String, severity: NotificationSeverity): Result<Unit> {
        return activeService?.sendNotification(message, severity)
            ?: Result.failure(Exception("No active NAS platform service"))
    }
    
    /**
     * Get system resources
     */
    suspend fun getSystemResources(): Result<SystemResources> {
        return activeService?.getSystemResources()
            ?: Result.failure(Exception("No active NAS platform service"))
    }
    
    /**
     * Create backup
     */
    suspend fun createBackup(destination: String, backupType: BackupType): Result<Unit> {
        return activeService?.createBackup(destination, backupType)
            ?: Result.failure(Exception("No active NAS platform service"))
    }
    
    /**
     * Get hardware encoder
     */
    suspend fun getHardwareEncoder(): HardwareEncoder? {
        return activeService?.getHardwareEncoder()
    }
    
    /**
     * Get hardware info
     */
    suspend fun getHardwareInfo(): Result<com.company.ipcamera.platform.nas.common.hardware.HardwareInfo> {
        return activeService?.getHardwareInfo()
            ?: Result.failure(Exception("No active NAS platform service"))
    }
    
    /**
     * Get all available hardware encoders
     */
    suspend fun getAvailableEncoders(): List<HardwareEncoder> {
        val encoders = mutableListOf<HardwareEncoder>()
        
        // Check each encoder type
        val intelEncoder = IntelQuickSyncEncoder()
        if (intelEncoder.isSupported()) {
            encoders.add(intelEncoder)
        }
        
        val nvidiaEncoder = NvidiaNVENCEncoder()
        if (nvidiaEncoder.isSupported()) {
            encoders.add(nvidiaEncoder)
        }
        
        val amdEncoder = AmdVCEEncoder()
        if (amdEncoder.isSupported()) {
            encoders.add(amdEncoder)
        }
        
        val armEncoder = ArmMaliEncoder()
        if (armEncoder.isSupported()) {
            encoders.add(armEncoder)
        }
        
        logger.info { "Found ${encoders.size} hardware encoders" }
        return encoders
    }
    
    /**
     * Get platform status flow
     */
    fun getPlatformStatusFlow(): Flow<PlatformStatus> = flow {
        while (true) {
            val status = when {
                activeService == null -> PlatformStatus.NOT_DETECTED
                else -> {
                    val resources = getSystemResources().getOrNull()
                    if (resources != null) {
                        PlatformStatus.RUNNING(resources)
                    } else {
                        PlatformStatus.ERROR
                    }
                }
            }
            emit(status)
            kotlinx.coroutines.delay(5000)
        }
    }
    
    /**
     * Shutdown all services
     */
    suspend fun shutdown(): Result<Unit> {
        logger.info { "Shutting down NAS Platform Manager" }
        
        var result = Result.success(Unit)
        
        for (service in platformServices) {
            service.shutdown()
                .onFailure { error ->
                    logger.error(error) { "Error shutting down ${service.platformName}" }
                    result = Result.failure(error)
                }
        }
        
        activeService = null
        platformServices.clear()
        
        return result
    }
    
    /**
     * Platform status
     */
    sealed class PlatformStatus {
        object NOT_DETECTED : PlatformStatus()
        object ERROR : PlatformStatus()
        data class RUNNING(val resources: SystemResources) : PlatformStatus()
    }
}

// Platform Service Implementations

class SynologyNasPlatformService : NasPlatformService {
    private val service = SynologyIntegrationService()
    
    override val platformName = "Synology DSM"
    
    override suspend fun isCurrentPlatform(): Boolean {
        return java.io.File("/etc.defaults/VERSION").exists()
    }
    
    override suspend fun initialize(): Result<Unit> = Result.success(Unit)
    
    override suspend fun sendNotification(message: String, severity: NotificationSeverity): Result<Unit> {
        return service.sendNotification(message, convertSeverity(severity))
    }
    
    override suspend fun getSystemResources(): Result<SystemResources> {
        return service.getSystemResources().map { it.toCommonResources() }
    }
    
    override suspend fun createBackup(destination: String, backupType: BackupType): Result<Unit> {
        return service.createBackup(destination, convertBackupType(backupType))
    }
    
    override suspend fun getHardwareEncoder(): HardwareEncoder? {
        return null // Synology doesn't provide hardware encoder
    }
    
    override suspend fun getHardwareInfo(): Result<com.company.ipcamera.platform.nas.common.hardware.HardwareInfo> {
        return Result.failure(Exception("Hardware info not available on Synology"))
    }
    
    override suspend fun shutdown(): Result<Unit> = Result.success(Unit)
    
    private fun convertSeverity(severity: NotificationSeverity): SynologyIntegrationService.NotificationSeverity {
        return when (severity) {
            NotificationSeverity.INFO -> SynologyIntegrationService.NotificationSeverity.INFO
            NotificationSeverity.WARNING -> SynologyIntegrationService.NotificationSeverity.WARNING
            NotificationSeverity.ERROR -> SynologyIntegrationService.NotificationSeverity.ERROR
            NotificationSeverity.CRITICAL -> SynologyIntegrationService.NotificationSeverity.CRITICAL
        }
    }
}

class QnapNasPlatformService : NasPlatformService {
    private val service = QnapIntegrationService()
    
    override val platformName = "QNAP QTS"
    
    override suspend fun isCurrentPlatform(): Boolean {
        return java.io.File("/etc/config/qpkg.conf").exists()
    }
    
    override suspend fun initialize(): Result<Unit> = Result.success(Unit)
    
    override suspend fun sendNotification(message: String, severity: NotificationSeverity): Result<Unit> {
        return service.sendNotification(message, convertSeverity(severity))
    }
    
    override suspend fun getSystemResources(): Result<SystemResources> {
        return service.getSystemResources().map { it.toCommonResources() }
    }
    
    override suspend fun createBackup(destination: String, backupType: BackupType): Result<Unit> {
        return service.createBackup(destination, convertBackupType(backupType))
    }
    
    override suspend fun getHardwareEncoder(): HardwareEncoder? = null
    
    override suspend fun getHardwareInfo(): Result<com.company.ipcamera.platform.nas.common.hardware.HardwareInfo> {
        return Result.failure(Exception("Hardware info not available on QNAP"))
    }
    
    override suspend fun shutdown(): Result<Unit> = Result.success(Unit)
    
    private fun convertSeverity(severity: NotificationSeverity): QnapIntegrationService.NotificationSeverity {
        return when (severity) {
            NotificationSeverity.INFO -> QnapIntegrationService.NotificationSeverity.INFO
            NotificationSeverity.WARNING -> QnapIntegrationService.NotificationSeverity.WARNING
            NotificationSeverity.ERROR -> QnapIntegrationService.NotificationSeverity.ERROR
            NotificationSeverity.CRITICAL -> QnapIntegrationService.NotificationSeverity.CRITICAL
        }
    }
}

class AsustorNasPlatformService : NasPlatformService {
    private val service = AsustorIntegrationService()
    
    override val platformName = "Asustor ADM"
    
    override suspend fun isCurrentPlatform(): Boolean {
        return java.io.File("/usr/local/IP-CSS").exists()
    }
    
    override suspend fun initialize(): Result<Unit> = Result.success(Unit)
    
    override suspend fun sendNotification(message: String, severity: NotificationSeverity): Result<Unit> {
        return service.sendNotification(message, convertSeverity(severity))
    }
    
    override suspend fun getSystemResources(): Result<SystemResources> {
        return service.getSystemResources().map { it.toCommonResources() }
    }
    
    override suspend fun createBackup(destination: String, backupType: BackupType): Result<Unit> {
        return Result.success(Unit) // Not implemented
    }
    
    override suspend fun getHardwareEncoder(): HardwareEncoder? = null
    
    override suspend fun getHardwareInfo(): Result<com.company.ipcamera.platform.nas.common.hardware.HardwareInfo> {
        return Result.failure(Exception("Hardware info not available on Asustor"))
    }
    
    override suspend fun shutdown(): Result<Unit> = Result.success(Unit)
    
    private fun convertSeverity(severity: NotificationSeverity): AsustorIntegrationService.NotificationSeverity {
        return when (severity) {
            NotificationSeverity.INFO -> AsustorIntegrationService.NotificationSeverity.INFO
            NotificationSeverity.WARNING -> AsustorIntegrationService.NotificationSeverity.WARNING
            NotificationSeverity.ERROR -> AsustorIntegrationService.NotificationSeverity.ERROR
            NotificationSeverity.CRITICAL -> AsustorIntegrationService.NotificationSeverity.CRITICAL
        }
    }
}

class TrueNasPlatformService : NasPlatformService {
    override val platformName = "TrueNAS SCALE"
    
    override suspend fun isCurrentPlatform(): Boolean {
        return java.io.File("/.dockerenv").exists() || 
               System.getenv("TRUENAS") == "true"
    }
    
    override suspend fun initialize(): Result<Unit> = Result.success(Unit)
    
    override suspend fun sendNotification(message: String, severity: NotificationSeverity): Result<Unit> {
        // TrueNAS doesn't have native notification API
        logger.info { "TrueNAS notification: $message ($severity)" }
        return Result.success(Unit)
    }
    
    override suspend fun getSystemResources(): Result<SystemResources> {
        // Implement TrueNAS resource monitoring
        return Result.success(
            SystemResources(
                cpuUsagePercent = 0.0,
                memoryTotalBytes = 0,
                memoryUsedBytes = 0,
                memoryFreeBytes = 0,
                diskTotalBytes = 0,
                diskUsedBytes = 0,
                diskFreeBytes = 0,
                temperatureCelsius = null,
                uptimeSeconds = 0
            )
        )
    }
    
    override suspend fun createBackup(destination: String, backupType: BackupType): Result<Unit> {
        return Result.success(Unit) // Not implemented
    }
    
    override suspend fun getHardwareEncoder(): HardwareEncoder? = null
    
    override suspend fun getHardwareInfo(): Result<com.company.ipcamera.platform.nas.common.hardware.HardwareInfo> {
        return Result.failure(Exception("Hardware info not available on TrueNAS"))
    }
    
    override suspend fun shutdown(): Result<Unit> = Result.success(Unit)
}

// Extension functions

fun SynologyIntegrationService.SystemResources.toCommonResources(): SystemResources {
    return SystemResources(
        cpuUsagePercent = cpuUsagePercent,
        memoryTotalBytes = memoryTotalBytes,
        memoryUsedBytes = memoryUsedBytes,
        memoryFreeBytes = memoryFreeBytes,
        diskTotalBytes = diskTotalBytes,
        diskUsedBytes = diskUsedBytes,
        diskFreeBytes = diskFreeBytes,
        temperatureCelsius = temperatureCelsius,
        uptimeSeconds = uptimeSeconds
    )
}

fun QnapIntegrationService.QnapSystemResources.toCommonResources(): SystemResources {
    return SystemResources(
        cpuUsagePercent = cpuUsagePercent,
        memoryTotalBytes = memoryTotalBytes,
        memoryUsedBytes = memoryUsedBytes,
        memoryFreeBytes = memoryFreeBytes,
        diskTotalBytes = diskTotalBytes,
        diskUsedBytes = diskUsedBytes,
        diskFreeBytes = diskFreeBytes,
        temperatureCelsius = null,
        uptimeSeconds = uptimeSeconds
    )
}

fun AsustorIntegrationService.AsustorSystemResources.toCommonResources(): SystemResources {
    return SystemResources(
        cpuUsagePercent = cpuUsagePercent,
        memoryTotalBytes = memoryTotalBytes,
        memoryUsedBytes = memoryUsedBytes,
        memoryFreeBytes = memoryFreeBytes,
        diskTotalBytes = diskTotalBytes,
        diskUsedBytes = diskUsedBytes,
        diskFreeBytes = diskFreeBytes,
        temperatureCelsius = null,
        uptimeSeconds = 0
    )
}

fun convertBackupType(type: BackupType): SynologyIntegrationService.BackupType {
    return when (type) {
        BackupType.FULL -> SynologyIntegrationService.BackupType.FULL
        BackupType.INCREMENTAL -> SynologyIntegrationService.BackupType.INCREMENTAL
        BackupType.DIFFERENTIAL -> SynologyIntegrationService.BackupType.DIFFERENTIAL
    }
}

fun convertBackupType(type: BackupType): QnapIntegrationService.BackupType {
    return when (type) {
        BackupType.FULL -> QnapIntegrationService.BackupType.FULL
        BackupType.INCREMENTAL -> QnapIntegrationService.BackupType.INCREMENTAL
        BackupType.DIFFERENTIAL -> QnapIntegrationService.BackupType.DIFFERENTIAL
    }
}
