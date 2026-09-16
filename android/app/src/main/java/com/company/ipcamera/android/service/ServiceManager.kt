package com.company.ipcamera.android.service

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.model.RecordingFormat
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.EventRepository
import com.company.ipcamera.shared.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.StateFlow

/**
 * Менеджер для управления фоновыми сервисами
 */
class ServiceManager(private val context: Context) {

    private var recordingService: RecordingService? = null
    private var monitoringService: CameraMonitoringService? = null

    private var recordingServiceBound = false
    private var monitoringServiceBound = false
    private val pendingRecordingActions = mutableListOf<(RecordingService) -> Unit>()
    private val pendingMonitoringActions = mutableListOf<(CameraMonitoringService) -> Unit>()

    private val recordingServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as RecordingService.LocalBinder
            recordingService = binder.getService()
            recordingServiceBound = true
            flushPendingRecordingActions()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            recordingService = null
            recordingServiceBound = false
        }
    }

    private val monitoringServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as CameraMonitoringService.LocalBinder
            monitoringService = binder.getService()
            monitoringServiceBound = true
            flushPendingMonitoringActions()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            monitoringService = null
            monitoringServiceBound = false
        }
    }

    /**
     * Запустить RecordingService
     */
    fun startRecordingService(
        cameraId: String,
        format: RecordingFormat = RecordingFormat.MP4,
        quality: Quality = Quality.HIGH,
        duration: Long? = null,
        cameraRepository: CameraRepository,
        recordingRepository: RecordingRepository
    ) {
        val intent = Intent(context, RecordingService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, recordingServiceConnection, Context.BIND_AUTO_CREATE)
        withRecordingService { service ->
            service.startRecording(
                cameraId = cameraId,
                format = format,
                quality = quality,
                duration = duration,
                cameraRepository = cameraRepository,
                recordingRepository = recordingRepository
            )
        }
    }

    /**
     * Остановить запись
     */
    fun stopRecording(cameraId: String) {
        withRecordingService { it.stopRecording(cameraId) }
    }

    /**
     * Остановить запись по ID
     */
    fun stopRecordingById(recordingId: String) {
        withRecordingService { it.stopRecordingById(recordingId) }
    }

    /**
     * Приостановить запись
     */
    fun pauseRecording(cameraId: String) {
        withRecordingService { it.pauseRecording(cameraId) }
    }

    /**
     * Возобновить запись
     */
    fun resumeRecording(cameraId: String) {
        withRecordingService { it.resumeRecording(cameraId) }
    }

    /**
     * Получить состояние записей
     */
    fun getRecordingsState(): StateFlow<Map<String, RecordingService.RecordingState>>? {
        return recordingService?.recordingsState
    }

    /**
     * Проверить, идет ли запись
     */
    fun isRecording(cameraId: String): Boolean {
        return recordingService?.isRecording(cameraId) ?: false
    }

    /**
     * Получить активные записи
     */
    fun getActiveRecordings(): List<com.company.ipcamera.shared.domain.model.Recording> {
        return recordingService?.getActiveRecordings() ?: emptyList()
    }

    /**
     * Запустить CameraMonitoringService
     */
    fun startMonitoringService(
        cameraRepository: CameraRepository,
        eventRepository: EventRepository
    ) {
        val intent = Intent(context, CameraMonitoringService::class.java)
        context.startForegroundService(intent)
        context.bindService(intent, monitoringServiceConnection, Context.BIND_AUTO_CREATE)
        withMonitoringService { it.startMonitoring(cameraRepository, eventRepository) }
    }

    /**
     * Остановить мониторинг
     */
    fun stopMonitoringService() {
        withMonitoringService { it.stopMonitoring() }
    }

    /**
     * Получить состояние мониторинга
     */
    fun getMonitoringState(): StateFlow<CameraMonitoringService.MonitoringState>? {
        return monitoringService?.monitoringState
    }

    /**
     * Проверить, работает ли мониторинг
     */
    fun isMonitoring(): Boolean {
        return monitoringService?.let {
            val state = it.monitoringState.value
            state !is CameraMonitoringService.MonitoringState.Idle
        } ?: false
    }

    /**
     * Проверить, запущен ли сервис
     */
    fun isServiceRunning(serviceClass: Class<*>): Boolean {
        return when (serviceClass) {
            RecordingService::class.java -> recordingServiceBound
            CameraMonitoringService::class.java -> monitoringServiceBound
            else -> false
        }
    }

    /**
     * Отключить все сервисы
     */
    fun unbindAll() {
        if (recordingServiceBound) {
            context.unbindService(recordingServiceConnection)
            recordingServiceBound = false
        }
        if (monitoringServiceBound) {
            context.unbindService(monitoringServiceConnection)
            monitoringServiceBound = false
        }
    }

    private fun withRecordingService(action: (RecordingService) -> Unit) {
        val service = recordingService
        if (service != null && recordingServiceBound) {
            action(service)
        } else {
            pendingRecordingActions += action
        }
    }

    private fun withMonitoringService(action: (CameraMonitoringService) -> Unit) {
        val service = monitoringService
        if (service != null && monitoringServiceBound) {
            action(service)
        } else {
            pendingMonitoringActions += action
        }
    }

    private fun flushPendingRecordingActions() {
        val service = recordingService ?: return
        val actions = pendingRecordingActions.toList()
        pendingRecordingActions.clear()
        actions.forEach { it(service) }
    }

    private fun flushPendingMonitoringActions() {
        val service = monitoringService ?: return
        val actions = pendingMonitoringActions.toList()
        pendingMonitoringActions.clear()
        actions.forEach { it(service) }
    }
}
