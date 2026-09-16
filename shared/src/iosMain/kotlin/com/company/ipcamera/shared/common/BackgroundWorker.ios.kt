package com.company.ipcamera.shared.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSystemFreeSize
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSHost
import platform.Foundation.NSNumber
import platform.Foundation.NSProcessInfo
import platform.darwin.UIDevice
import platform.darwin.UIDeviceBatteryStateCharging
import platform.darwin.UIDeviceBatteryStateFull

actual class BackgroundWorker actual constructor(private val context: Any?) {
    private val taskRegistry = mutableMapOf<String, BackgroundTask>()
    private val taskJobs = mutableMapOf<String, Job>()
    private val scope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    private val _taskEventsFlow = MutableStateFlow<TaskEvent?>(null)
    val taskEventsFlow: Flow<TaskEvent>
        get() = _taskEventsFlow.asStateFlow().filterNotNull()

    actual fun initialize() {
        // Регистрируем задачи для фонового выполнения
        registerBackgroundTasks()
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

            val job =
                scope.launch {
                    while (isActive) {
                        try {
                            if (checkConstraints(constraints)) {
                                _taskEventsFlow.value = TaskEvent.Started(taskId)
                                val result = task.execute(context)
                                _taskEventsFlow.value = TaskEvent.Completed(taskId, result)
                            }
                        } catch (e: Exception) {
                            _taskEventsFlow.value = TaskEvent.Failed(taskId, e)
                        }
                        delay(interval)
                    }
                }

            taskJobs[taskId] = job

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

            val job =
                scope.launch {
                    delay(delay)
                    try {
                        if (checkConstraints(constraints)) {
                            _taskEventsFlow.value = TaskEvent.Started(taskId)
                            val result = task.execute(context)
                            _taskEventsFlow.value = TaskEvent.Completed(taskId, result)
                        } else {
                            _taskEventsFlow.value = TaskEvent.Cancelled(taskId)
                        }
                    } catch (e: Exception) {
                        _taskEventsFlow.value = TaskEvent.Failed(taskId, e)
                    } finally {
                        taskRegistry.remove(taskId)
                        taskJobs.remove(taskId)
                    }
                }

            taskJobs[taskId] = job

            true
        } catch (e: Exception) {
            false
        }
    }

    actual suspend fun cancelTask(taskId: String): Boolean {
        return try {
            taskJobs[taskId]?.cancel()
            taskJobs.remove(taskId)
            taskRegistry.remove(taskId)
            _taskEventsFlow.value = TaskEvent.Cancelled(taskId)
            true
        } catch (e: Exception) {
            false
        }
    }

    actual suspend fun cancelAllTasks(): Boolean {
        return try {
            taskJobs.values.forEach { it.cancel() }
            taskJobs.clear()
            taskRegistry.clear()
            true
        } catch (e: Exception) {
            false
        }
    }

    actual suspend fun getTaskStatus(taskId: String): TaskStatus? {
        return when {
            !taskRegistry.containsKey(taskId) -> null
            taskJobs[taskId]?.isCompleted == true -> TaskStatus.SUCCEEDED
            taskJobs[taskId]?.isCancelled == true -> TaskStatus.CANCELLED
            taskJobs[taskId]?.isActive == true -> TaskStatus.RUNNING
            else -> TaskStatus.PENDING
        }
    }

    actual fun getTaskEvents(): Flow<TaskEvent> {
        return taskEventsFlow
    }

    private suspend fun checkConstraints(constraints: TaskConstraints): Boolean {
        if (constraints.requiresNetwork && !hasUsableNetworkConnection()) {
            return false
        }
        if (constraints.requiresBatteryNotLow && isBatteryLow()) {
            return false
        }
        if (constraints.requiresCharging && !isCharging()) {
            return false
        }
        if (constraints.requiresDeviceIdle && !isDeviceIdle()) {
            return false
        }
        if (constraints.requiresStorageNotLow && !hasEnoughDiskSpace()) {
            return false
        }
        return true
    }

    private fun registerBackgroundTasks() {
        // BGTaskScheduler registration is app-specific and must be done in AppDelegate.
        // This worker keeps only shared task orchestration in Kotlin.
    }

    private fun hasUsableNetworkConnection(): Boolean {
        return try {
            val addresses = NSHost.hostWithName("one.one.one.one").addresses
            !addresses.isNullOrEmpty()
        } catch (_: Exception) {
            false
        }
    }

    private fun isBatteryLow(): Boolean {
        return NSProcessInfo.processInfo.isLowPowerModeEnabled
    }

    private fun isCharging(): Boolean {
        val device = UIDevice.currentDevice
        val previousMonitoringState = device.batteryMonitoringEnabled
        device.batteryMonitoringEnabled = true
        val charging =
            when (device.batteryState) {
                UIDeviceBatteryStateCharging, UIDeviceBatteryStateFull -> true
                else -> false
            }
        device.batteryMonitoringEnabled = previousMonitoringState
        return charging
    }

    private fun isDeviceIdle(): Boolean {
        // Use low power mode as conservative approximation for non-idle state.
        return !NSProcessInfo.processInfo.isLowPowerModeEnabled
    }

    private fun hasEnoughDiskSpace(): Boolean {
        return try {
            val attributes =
                NSFileManager.defaultManager.attributesOfFileSystemForPath(
                    NSHomeDirectory(),
                    error = null,
                )
            val freeSizeNumber = attributes?.get(NSFileSystemFreeSize) as? NSNumber
            val freeBytes = freeSizeNumber?.longLongValue ?: 0L
            freeBytes >= MIN_DISK_SPACE_BYTES
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val MIN_DISK_SPACE_BYTES = 100L * 1024L * 1024L // 100 MB
    }
}
