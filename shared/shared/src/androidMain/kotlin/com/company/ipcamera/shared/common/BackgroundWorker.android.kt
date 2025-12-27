package com.company.ipcamera.shared.common

import android.content.Context
import androidx.work.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.TimeUnit

actual class BackgroundWorker actual constructor(private val context: Any?) {
    private val androidContext: Context = context as? Context
        ?: throw IllegalArgumentException("Context is required for Android BackgroundWorker")
    
    private val workManager: WorkManager by lazy {
        WorkManager.getInstance(androidContext)
    }
    
    private val _taskEvents = MutableStateFlow<TaskEvent?>(null)
    private val taskEventsFlow = _taskEvents.asStateFlow()
        .filterNotNull()
    
    actual fun initialize() {
        // Инициализация WorkManager
        // WorkManager инициализируется автоматически при первом использовании
    }
    
    actual suspend fun schedulePeriodicTask(
        taskId: String,
        task: BackgroundTask,
        interval: Long,
        flexInterval: Long?,
        constraints: TaskConstraints
    ): Boolean {
        return try {
            // Сохраняем задачу для выполнения
            TaskRegistry.register(taskId, task)
            
            val inputData = Data.Builder()
                .putString("task_id", taskId)
                .build()
            
            val workRequest = PeriodicWorkRequestBuilder<BackgroundTaskWorker>(
                interval,
                TimeUnit.MILLISECONDS,
                flexInterval ?: interval,
                TimeUnit.MILLISECONDS
            )
                .setConstraints(buildConstraints(constraints))
                .setInputData(inputData)
                .addTag(taskId)
                .build()
            
            workManager.enqueue(workRequest)
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    actual suspend fun scheduleOneTimeTask(
        taskId: String,
        task: BackgroundTask,
        delay: Long,
        constraints: TaskConstraints
    ): Boolean {
        return try {
            // Сохраняем задачу для выполнения
            TaskRegistry.register(taskId, task)
            
            val inputData = Data.Builder()
                .putString("task_id", taskId)
                .build()
            
            val workRequest = OneTimeWorkRequestBuilder<BackgroundTaskWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .setConstraints(buildConstraints(constraints))
                .setInputData(inputData)
                .addTag(taskId)
                .build()
            
            workManager.enqueue(workRequest)
            
            true
        } catch (e: Exception) {
            false
        }
    }
    
    actual suspend fun cancelTask(taskId: String): Boolean {
        return try {
            workManager.cancelAllWorkByTag(taskId)
            TaskRegistry.unregister(taskId)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    actual suspend fun cancelAllTasks(): Boolean {
        return try {
            workManager.cancelAllWork()
            TaskRegistry.clear()
            true
        } catch (e: Exception) {
            false
        }
    }
    
    actual suspend fun getTaskStatus(taskId: String): TaskStatus? {
        return try {
            val workInfos = workManager.getWorkInfosByTag(taskId).await()
            val workInfo = workInfos.firstOrNull() ?: return null
            
            mapWorkStateToTaskStatus(workInfo.state)
        } catch (e: Exception) {
            null
        }
    }
    
    actual fun getTaskEvents(): Flow<TaskEvent> {
        return taskEventsFlow
    }
    
    private fun buildConstraints(constraints: TaskConstraints): androidx.work.Constraints {
        val builder = Constraints.Builder()
        
        if (constraints.requiresNetwork) {
            builder.setRequiredNetworkType(NetworkType.CONNECTED)
        }
        
        if (constraints.requiresCharging) {
            builder.setRequiresCharging(true)
        }
        
        if (constraints.requiresBatteryNotLow) {
            builder.setRequiresBatteryNotLow(true)
        }
        
        if (constraints.requiresDeviceIdle) {
            builder.setRequiresDeviceIdle(true)
        }
        
        if (constraints.requiresStorageNotLow) {
            builder.setRequiresStorageNotLow(true)
        }
        
        return builder.build()
    }
    
    private fun mapWorkStateToTaskStatus(state: WorkInfo.State): TaskStatus {
        return when (state) {
            WorkInfo.State.ENQUEUED -> TaskStatus.PENDING
            WorkInfo.State.RUNNING -> TaskStatus.RUNNING
            WorkInfo.State.SUCCEEDED -> TaskStatus.SUCCEEDED
            WorkInfo.State.FAILED -> TaskStatus.FAILED
            WorkInfo.State.CANCELLED -> TaskStatus.CANCELLED
            WorkInfo.State.BLOCKED -> TaskStatus.BLOCKED
        }
    }
}

/**
 * Worker для выполнения фоновых задач
 */
class BackgroundTaskWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result {
        val taskId = inputData.getString(TASK_ID_KEY) ?: return Result.failure()
        val task = TaskRegistry.get(taskId) ?: return Result.failure()
        
        return try {
            val result = task.execute(applicationContext)
            
            when (result) {
                is TaskResult.Success -> Result.success()
                is TaskResult.Retry -> Result.retry()
                is TaskResult.Failure -> Result.failure()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
    
    companion object {
        private const val TASK_ID_KEY = "task_id"
    }
}

/**
 * Реестр задач для доступа из Worker
 */
object TaskRegistry {
    private val tasks = mutableMapOf<String, BackgroundTask>()
    
    fun register(id: String, task: BackgroundTask) {
        tasks[id] = task
    }
    
    fun unregister(id: String) {
        tasks.remove(id)
    }
    
    fun get(id: String): BackgroundTask? {
        return tasks[id]
    }
    
    fun clear() {
        tasks.clear()
    }
}

