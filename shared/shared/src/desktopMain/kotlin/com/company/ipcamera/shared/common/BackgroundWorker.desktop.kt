package com.company.ipcamera.shared.common

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicBoolean

actual class BackgroundWorker actual constructor(private val context: Any?) {
    private val taskRegistry = mutableMapOf<String, BackgroundTask>()
    private val scheduledTasks = mutableMapOf<String, ScheduledFuture<*>>()
    private val executorService: ScheduledExecutorService = Executors.newScheduledThreadPool(4)

    private val _taskEvents = MutableStateFlow<TaskEvent?>(null)
    private val taskEventsFlow = _taskEvents.asStateFlow()
        .filterNotNull()

    private val isShutdown = AtomicBoolean(false)

    actual fun initialize() {
        // Инициализация executor service
        // Добавляем shutdown hook для корректного завершения
        Runtime.getRuntime().addShutdownHook(Thread {
            shutdown()
        })
    }

    actual suspend fun schedulePeriodicTask(
        taskId: String,
        task: BackgroundTask,
        interval: Long,
        flexInterval: Long?,
        constraints: TaskConstraints
    ): Boolean {
        return try {
            taskRegistry[taskId] = task

            val runnable = Runnable {
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        if (checkConstraints(constraints)) {
                            _taskEvents.value = TaskEvent.Started(taskId)
                            val result = task.execute(context)
                            _taskEvents.value = TaskEvent.Completed(taskId, result)
                        } else {
                            // Ограничения не выполнены, пропускаем выполнение
                        }
                    } catch (e: Exception) {
                        _taskEvents.value = TaskEvent.Failed(taskId, e)
                    }
                }
            }

            val scheduledFuture = executorService.scheduleAtFixedRate(
                runnable,
                interval,
                interval,
                TimeUnit.MILLISECONDS
            )

            scheduledTasks[taskId] = scheduledFuture

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
            taskRegistry[taskId] = task

            val runnable = Runnable {
                CoroutineScope(Dispatchers.Default).launch {
                    try {
                        if (checkConstraints(constraints)) {
                            _taskEvents.value = TaskEvent.Started(taskId)
                            val result = task.execute(context)
                            _taskEvents.value = TaskEvent.Completed(taskId, result)
                        } else {
                            // Ограничения не выполнены, отменяем задачу
                            _taskEvents.value = TaskEvent.Cancelled(taskId)
                        }
                    } catch (e: Exception) {
                        _taskEvents.value = TaskEvent.Failed(taskId, e)
                    } finally {
                        taskRegistry.remove(taskId)
                        scheduledTasks.remove(taskId)
                    }
                }
            }

            val scheduledFuture = executorService.schedule(
                runnable,
                delay,
                TimeUnit.MILLISECONDS
            )

            scheduledTasks[taskId] = scheduledFuture

            true
        } catch (e: Exception) {
            false
        }
    }

    actual suspend fun cancelTask(taskId: String): Boolean {
        return try {
            scheduledTasks[taskId]?.cancel(true)
            scheduledTasks.remove(taskId)
            taskRegistry.remove(taskId)
            _taskEvents.value = TaskEvent.Cancelled(taskId)
            true
        } catch (e: Exception) {
            false
        }
    }

    actual suspend fun cancelAllTasks(): Boolean {
        return try {
            scheduledTasks.values.forEach { it.cancel(true) }
            scheduledTasks.clear()
            taskRegistry.clear()
            true
        } catch (e: Exception) {
            false
        }
    }

    actual suspend fun getTaskStatus(taskId: String): TaskStatus? {
        return when {
            !taskRegistry.containsKey(taskId) -> null
            scheduledTasks[taskId]?.isDone == true -> TaskStatus.SUCCEEDED
            scheduledTasks[taskId]?.isCancelled == true -> TaskStatus.CANCELLED
            else -> TaskStatus.PENDING
        }
    }

    actual fun getTaskEvents(): Flow<TaskEvent> {
        return taskEventsFlow
    }

    private fun checkConstraints(constraints: TaskConstraints): Boolean {
        // Проверка ограничений для Desktop
        // Большинство ограничений не применимы к Desktop, но можно проверить сеть и хранилище

        if (constraints.requiresNetwork) {
            // TODO: Реализовать проверку сетевого подключения
            // Пока возвращаем true
        }

        if (constraints.requiresStorageNotLow) {
            // TODO: Реализовать проверку свободного места на диске
            // Пока возвращаем true
        }

        // Остальные ограничения (зарядка, батарея, idle) не применимы к Desktop
        return true
    }

    private fun shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            cancelAllTasks()
            executorService.shutdown()
            try {
                if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                    executorService.shutdownNow()
                }
            } catch (e: InterruptedException) {
                executorService.shutdownNow()
            }
        }
    }
}

