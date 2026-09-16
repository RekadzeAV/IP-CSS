package com.company.ipcamera.shared.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

actual class BackgroundWorker actual constructor(private val context: Any?) {
    private val taskRegistry = mutableMapOf<String, BackgroundTask>()
    private val scheduledTasks = mutableMapOf<String, ScheduledFuture<*>>()
    private val executorService: ScheduledExecutorService by lazy {
        Executors.newScheduledThreadPool(4)
    }

    private val taskEvents = MutableStateFlow<TaskEvent?>(null)
    private val taskEventsFlow =
        taskEvents.asStateFlow()
            .filterNotNull()

    private val isShutdown = AtomicBoolean(false)

    actual fun initialize() {
        // Инициализация executor service
        // Добавляем shutdown hook для корректного завершения
        Runtime.getRuntime().addShutdownHook(
            Thread {
                shutdown()
            },
        )
    }

    actual suspend fun schedulePeriodicTask(
        taskId: String,
        task: BackgroundTask,
        interval: Long,
        flexInterval: Long?,
        constraints: TaskConstraints,
    ): Boolean {
        return try {
            taskRegistry[taskId] = task

            val runnable =
                Runnable {
                    CoroutineScope(Dispatchers.Default).launch {
                        try {
                            if (checkConstraints(constraints)) {
                                taskEvents.value = TaskEvent.Started(taskId)
                                val result = task.execute(context)
                                taskEvents.value = TaskEvent.Completed(taskId, result)
                            } else {
                                // Ограничения не выполнены, пропускаем выполнение
                            }
                        } catch (e: Exception) {
                            taskEvents.value = TaskEvent.Failed(taskId, e)
                        }
                    }
                }

            val scheduledFuture =
                executorService.scheduleAtFixedRate(
                    runnable,
                    interval,
                    interval,
                    TimeUnit.MILLISECONDS,
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
        constraints: TaskConstraints,
    ): Boolean {
        return try {
            taskRegistry[taskId] = task

            val runnable =
                Runnable {
                    CoroutineScope(Dispatchers.Default).launch {
                        try {
                            if (checkConstraints(constraints)) {
                                taskEvents.value = TaskEvent.Started(taskId)
                                val result = task.execute(context)
                                taskEvents.value = TaskEvent.Completed(taskId, result)
                            } else {
                                // Ограничения не выполнены, отменяем задачу
                                taskEvents.value = TaskEvent.Cancelled(taskId)
                            }
                        } catch (e: Exception) {
                            taskEvents.value = TaskEvent.Failed(taskId, e)
                        } finally {
                            taskRegistry.remove(taskId)
                            scheduledTasks.remove(taskId)
                        }
                    }
                }

            val scheduledFuture =
                executorService.schedule(
                    runnable,
                    delay,
                    TimeUnit.MILLISECONDS,
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
            taskEvents.value = TaskEvent.Cancelled(taskId)
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
        if (constraints.requiresNetwork && !hasUsableNetworkConnection()) {
            return false
        }

        if (constraints.requiresStorageNotLow && !hasEnoughDiskSpace()) {
            return false
        }

        // Остальные ограничения (зарядка, батарея, idle) не применимы к Desktop.
        return true
    }

    private fun hasUsableNetworkConnection(): Boolean {
        return try {
            val hasActiveInterface =
                NetworkInterface.getNetworkInterfaces()
                    ?.asSequence()
                    ?.any { !it.isLoopback && it.isUp && it.interfaceAddresses?.isNotEmpty() == true }
                    ?: false

            if (!hasActiveInterface) {
                return false
            }

            // Best-effort reachability check; failure falls back to interface status.
            InetAddress.getByName("1.1.1.1").isReachable(NETWORK_REACHABILITY_TIMEOUT_MS) || hasActiveInterface
        } catch (_: Exception) {
            false
        }
    }

    private fun hasEnoughDiskSpace(): Boolean {
        return try {
            val availableBytes = File(".").usableSpace
            availableBytes >= MIN_DISK_SPACE_BYTES
        } catch (_: Exception) {
            false
        }
    }

    private fun shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            runBlocking(Dispatchers.Default) { cancelAllTasks() }
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

    companion object {
        private const val NETWORK_REACHABILITY_TIMEOUT_MS = 750
        private const val MIN_DISK_SPACE_BYTES = 100L * 1024L * 1024L // 100 MB
    }
}
