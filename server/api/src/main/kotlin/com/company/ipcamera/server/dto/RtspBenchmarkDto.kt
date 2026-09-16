package com.company.ipcamera.server.dto

import kotlinx.serialization.Serializable

/**
 * Запрос на запуск RTSP бенчмарка
 */
@Serializable
data class RtspBenchmarkRequest(
    /** RTSP URL для тестирования */
    val rtspUrl: String,
    
    /** Длительность теста в секундах */
    val durationSeconds: Int = 30,
    
    /** Ожидаемый FPS */
    val expectedFps: Int = 25,
    
    /** Запускать ли несколько потоков (если URL содержит {1,2,3}) */
    val multiStream: Boolean = false
)

/**
 * Ответ с результатами RTSP бенчмарка
 */
@Serializable
data class RtspBenchmarkResponse(
    /** Статус теста */
    val status: String,
    
    /** ID теста */
    val testId: String,
    
    /** Длительность теста в секундах */
    val durationSeconds: Double,
    
    /** Средний FPS */
    val avgFps: Double,
    
    /** Минимальный FPS */
    val minFps: Double,
    
    /** Максимальный FPS */
    val maxFps: Double,
    
    /** Средний CPU usage (%) */
    val avgCpuUsage: Double,
    
    /** Максимальный CPU usage (%) */
    val maxCpuUsage: Double,
    
    /** Средняя память (MB) */
    val avgMemoryMb: Double,
    
    /** Максимальная память (MB) */
    val maxMemoryMb: Double,
    
    /** Средняя латентность (ms) */
    val avgLatencyMs: Double,
    
    /** Максимальная латентность (ms) */
    val maxLatencyMs: Double,
    
    /** Общее количество кадров */
    val totalFrames: Long,
    
    /** Пропущено кадров */
    val droppedFrames: Long,
    
    /** Количество переподключений */
    val reconnections: Long,
    
    /** Количество ошибок */
    val errors: Int,
    
    /** Список ошибок */
    val errorMessages: List<String>,
    
    /** Прошел ли тест критерии */
    val passed: Boolean,
    
    /** Время начала теста */
    val timestamp: Long
)

/**
 * Статус бенчмарка
 */
@Serializable
data class RtspBenchmarkStatus(
    val testId: String,
    val status: String, // RUNNING, COMPLETED, FAILED
    val progress: Double, // 0.0 - 1.0
    val currentFps: Double?,
    val message: String?
)
