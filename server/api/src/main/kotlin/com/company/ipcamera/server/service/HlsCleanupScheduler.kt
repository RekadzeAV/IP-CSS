package com.company.ipcamera.server.service

import mu.KotlinLogging
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

/**
 * Планировщик автоматической очистки старых HLS сегментов.
 * 
 * Ограничивает количество сегментов на стрим и удаляет старые файлы .ts
 * для предотвращения переполнения диска.
 */
class HlsCleanupScheduler(
    private val cleanupIntervalMs: Long = 5 * 60 * 1000L, // 5 минут
    private val maxSegmentsPerStream: Int = 180, // 6 минут при 2s segments
    private val minFreeSpaceGb: Double = 2.0 // Минимальное свободное место
) {
    private val isRunning = AtomicBoolean(false)
    private var cleanupJob: Thread? = null
    
    /**
     * Запустить планировщик очистки
     */
    fun startCleanupScheduler(hlsOutputDirectory: String) {
        if (isRunning.getAndSet(true)) {
            logger.warn { "Cleanup scheduler already running" }
            return
        }
        
        cleanupJob = Thread {
            logger.info { "Starting HLS cleanup scheduler (interval: ${cleanupIntervalMs / 1000}s)" }
            
            while (isRunning.get()) {
                try {
                    cleanupOldSegments(hlsOutputDirectory)
                    Thread.sleep(cleanupIntervalMs)
                } catch (e: InterruptedException) {
                    logger.info { "Cleanup scheduler interrupted" }
                    break
                } catch (e: Exception) {
                    logger.error(e) { "Error in cleanup scheduler" }
                    Thread.sleep(1000) // Подождать перед повторной попыткой
                }
            }
            
            logger.info { "HLS cleanup scheduler stopped" }
        }.apply { isDaemon = true }.also { it.start() }
    }
    
    /**
     * Остановить планировщик очистки
     */
    fun stop() {
        if (!isRunning.getAndSet(false)) {
            return
        }
        
        cleanupJob?.interrupt()
        cleanupJob?.join(5000)
        logger.info { "HLS cleanup scheduler stopped" }
    }
    
    /**
     * Очистить старые сегменты
     */
    fun cleanupOldSegments(hlsOutputDirectory: String) {
        val hlsDir = File(hlsOutputDirectory)
        if (!hlsDir.exists()) {
            logger.debug { "HLS directory does not exist: $hlsOutputDirectory" }
            return
        }
        
        // Проверить свободное место
        checkDiskSpace(hlsOutputDirectory)
        
        // Обработать каждый поток
        hlsDir.listFiles()?.filter { it.isDirectory }?.forEach { streamDir ->
            cleanupStreamSegments(streamDir)
        }
    }
    
    /**
     * Очистить сегменты для одного потока
     */
    private fun cleanupStreamSegments(streamDir: File) {
        val streamName = streamDir.name
        
        // Найти все файлы .ts
        val tsFiles = streamDir.listFiles { _, name -> name.endsWith(".ts") }
            ?.sortedBy { it.lastModified() }?.toMutableList() ?: return
        
        // Удалить старые если превышено лимит
        while (tsFiles.size > maxSegmentsPerStream) {
            val oldest = tsFiles.first()
            if (oldest.delete()) {
                tsFiles.removeAt(0)
                logger.debug { "Deleted old segment: ${oldest.name} (stream: $streamName)" }
            } else {
                logger.warn { "Failed to delete segment: ${oldest.name}" }
                break
            }
        }
        
        // Также проверить вложенные директории (для адаптивного HLS)
        streamDir.listFiles()?.filter { it.isDirectory }?.forEach { variantDir ->
            val variantFiles = variantDir.listFiles { _, name -> name.endsWith(".ts") }
                ?.sortedBy { it.lastModified() }?.toMutableList() ?: return@forEach
            
            while (variantFiles.size > maxSegmentsPerStream) {
                val oldest = variantFiles.first()
                if (oldest.delete()) {
                    variantFiles.removeAt(0)
                    logger.debug { "Deleted old segment: ${oldest.name} (variant: ${variantDir.name})" }
                } else {
                    break
                }
            }
        }
    }
    
    /**
     * Проверить свободное место на диске
     */
    private fun checkDiskSpace(hlsOutputDirectory: String): Boolean {
        val hlsDir = File(hlsOutputDirectory)
        val parent = hlsDir.parentFile ?: return true
        
        val freeSpaceGb = parent.freeSpace / (1024.0 * 1024 * 1024)
        
        if (freeSpaceGb < minFreeSpaceGb) {
            logger.warn { 
                "Low disk space: ${freeSpaceGb.toFixed(2)}GB free in ${parent.absolutePath}. " +
                "Initiating aggressive cleanup..."
            }
            
            // Удалить больше сегментов при нехватке места
            aggressiveCleanup(hlsOutputDirectory)
            return false
        }
        
        logger.debug { "Disk space OK: ${freeSpaceGb.toFixed(2)}GB free" }
        return true
    }
    
    /**
     * Агрессивная очистка при нехватке места
     */
    private fun aggressiveCleanup(hlsOutputDirectory: String) {
        val hlsDir = File(hlsOutputDirectory)
        hlsDir.listFiles()?.filter { it.isDirectory }?.forEach { streamDir ->
            val tsFiles = streamDir.listFiles { _, name -> name.endsWith(".ts") }
                ?.sortedBy { it.lastModified() } ?: return@forEach
            
            // Оставить только последние 30 сегментов (1 минуту)
            val toKeep = 30
            val toDelete = tsFiles.size - toKeep
            
            if (toDelete > 0) {
                logger.warn { 
                    "Aggressive cleanup: deleting ${toDelete} segments from ${streamDir.name}"
                }
                
                for (i in 0 until toDelete) {
                    tsFiles[i].delete()
                }
            }
        }
    }
}

/**
 * Форматировать число с фиксированной точкой
 */
private fun Double.toFixed(digits: Int): String {
    return String.format("%.${digits}f", this)
}
