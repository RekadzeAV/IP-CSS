package com.company.ipcamera.shared.common

import kotlinx.coroutines.flow.Flow

/**
 * Менеджер фоновой работы для кроссплатформенной системы
 * 
 * Поддерживает:
 * - Периодические задачи
 * - Одноразовые задачи
 * - Задачи с ограничениями (сеть, зарядка и т.д.)
 * - Мониторинг состояния задач
 */
expect class BackgroundWorker(context: Any?) {
    /**
     * Инициализация менеджера фоновой работы
     */
    fun initialize()
    
    /**
     * Запуск периодической задачи
     */
    suspend fun schedulePeriodicTask(
        taskId: String,
        task: BackgroundTask,
        interval: Long,
        flexInterval: Long? = null,
        constraints: TaskConstraints = TaskConstraints()
    ): Boolean
    
    /**
     * Запуск одноразовой задачи
     */
    suspend fun scheduleOneTimeTask(
        taskId: String,
        task: BackgroundTask,
        delay: Long = 0,
        constraints: TaskConstraints = TaskConstraints()
    ): Boolean
    
    /**
     * Отмена задачи
     */
    suspend fun cancelTask(taskId: String): Boolean
    
    /**
     * Отмена всех задач
     */
    suspend fun cancelAllTasks(): Boolean
    
    /**
     * Проверка статуса задачи
     */
    suspend fun getTaskStatus(taskId: String): TaskStatus?
    
    /**
     * Получение потока событий задач
     */
    fun getTaskEvents(): Flow<TaskEvent>
}

/**
 * Фоновая задача
 */
interface BackgroundTask {
    /**
     * Выполнение задачи
     */
    suspend fun execute(context: Any?): TaskResult
}

/**
 * Результат выполнения задачи
 */
sealed class TaskResult {
    object Success : TaskResult()
    data class Retry(val delay: Long = 0) : TaskResult()
    data class Failure(val error: Throwable) : TaskResult()
}

/**
 * Ограничения для задачи
 */
data class TaskConstraints(
    val requiresNetwork: Boolean = false,
    val requiresCharging: Boolean = false,
    val requiresBatteryNotLow: Boolean = false,
    val requiresDeviceIdle: Boolean = false,
    val requiresStorageNotLow: Boolean = false
)

/**
 * Статус задачи
 */
enum class TaskStatus {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    BLOCKED
}

/**
 * Событие задачи
 */
sealed class TaskEvent {
    data class Started(val taskId: String) : TaskEvent()
    data class Completed(val taskId: String, val result: TaskResult) : TaskEvent()
    data class Failed(val taskId: String, val error: Throwable) : TaskEvent()
    data class Cancelled(val taskId: String) : TaskEvent()
}

