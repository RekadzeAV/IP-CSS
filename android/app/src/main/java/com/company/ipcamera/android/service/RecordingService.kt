package com.company.ipcamera.android.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.company.ipcamera.android.MainActivity
import com.company.ipcamera.android.R
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.Quality
import com.company.ipcamera.shared.domain.model.Recording
import com.company.ipcamera.shared.domain.model.RecordingFormat
import com.company.ipcamera.shared.domain.model.RecordingStatus
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.RecordingRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

/**
 * Foreground Service для записи видео с камер
 *
 * Управляет записью видеопотоков в фоновом режиме,
 * отображает уведомления с прогрессом и управлением записью.
 */
class RecordingService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var notificationManager: NotificationManager
    private lateinit var cameraRepository: CameraRepository
    private lateinit var recordingRepository: RecordingRepository

    private val activeRecordings = mutableMapOf<String, RecordingJob>()
    private val pausedCameraIds = mutableSetOf<String>()

    private val _recordingsState = MutableStateFlow<Map<String, RecordingState>>(emptyMap())
    val recordingsState: StateFlow<Map<String, RecordingState>> = _recordingsState.asStateFlow()

    companion object {
        private const val CHANNEL_ID = "recording_service_channel"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_STOP_RECORDING = "com.company.ipcamera.STOP_RECORDING"
        private const val ACTION_PAUSE_RECORDING = "com.company.ipcamera.PAUSE_RECORDING"
        private const val ACTION_RESUME_RECORDING = "com.company.ipcamera.RESUME_RECORDING"

        private const val EXTRA_CAMERA_ID = "camera_id"
        private const val EXTRA_RECORDING_ID = "recording_id"
    }

    inner class LocalBinder : Binder() {
        fun getService(): RecordingService = this@RecordingService
    }

    data class RecordingState(
        val recording: Recording,
        val isPaused: Boolean = false,
        val error: String? = null
    )

    private data class RecordingJob(
        val recording: Recording,
        val job: Job,
        val camera: Camera
    )

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP_RECORDING -> {
                val cameraId = intent.getStringExtra(EXTRA_CAMERA_ID)
                val recordingId = intent.getStringExtra(EXTRA_RECORDING_ID)
                if (cameraId != null) {
                    stopRecording(cameraId)
                } else if (recordingId != null) {
                    stopRecordingById(recordingId)
                }
            }
            ACTION_PAUSE_RECORDING -> {
                val cameraId = intent.getStringExtra(EXTRA_CAMERA_ID)
                if (cameraId != null) {
                    pauseRecording(cameraId)
                }
            }
            ACTION_RESUME_RECORDING -> {
                val cameraId = intent.getStringExtra(EXTRA_CAMERA_ID)
                if (cameraId != null) {
                    resumeRecording(cameraId)
                }
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        activeRecordings.values.forEach { it.job.cancel() }
        activeRecordings.clear()
    }

    /**
     * Начать запись с камеры
     */
    fun startRecording(
        cameraId: String,
        format: RecordingFormat = RecordingFormat.MP4,
        quality: Quality = Quality.HIGH,
        duration: Long? = null,
        cameraRepository: CameraRepository,
        recordingRepository: RecordingRepository
    ) {
        this.cameraRepository = cameraRepository
        this.recordingRepository = recordingRepository

        if (activeRecordings.containsKey(cameraId)) {
            return // Запись уже идет
        }

        serviceScope.launch {
            try {
                // Получаем камеру
                val camera = cameraRepository.getCameraById(cameraId)
                    ?: throw IllegalArgumentException("Camera not found: $cameraId")

                // Создаем запись
                val recordingId = UUID.randomUUID().toString()
                val startTime = System.currentTimeMillis()

                val recording = Recording(
                    id = recordingId,
                    cameraId = camera.id,
                    cameraName = camera.name,
                    startTime = startTime,
                    endTime = null,
                    duration = 0,
                    filePath = null,
                    fileSize = null,
                    format = format,
                    quality = quality,
                    status = RecordingStatus.ACTIVE,
                    thumbnailUrl = null,
                    createdAt = startTime
                )

                // Сохраняем запись
                val addResult = recordingRepository.addRecording(recording)
                if (addResult.isFailure) {
                    throw addResult.exceptionOrNull() ?: Exception("Failed to add recording")
                }

                // Обновляем состояние
                _recordingsState.value = _recordingsState.value + (cameraId to RecordingState(recording))

                // Запускаем запись
                val job = serviceScope.launch(Dispatchers.IO) {
                    recordStream(recording, camera, duration)
                }

                activeRecordings[cameraId] = RecordingJob(recording, job, camera)

                // Обновляем уведомление
                updateNotification()

                // Запускаем foreground service
                startForeground(NOTIFICATION_ID, createNotification())

            } catch (e: Exception) {
                _recordingsState.value = _recordingsState.value + (cameraId to RecordingState(
                    recording = Recording(
                        id = UUID.randomUUID().toString(),
                        cameraId = cameraId,
                        cameraName = null,
                        startTime = System.currentTimeMillis(),
                        endTime = null,
                        duration = 0,
                        filePath = null,
                        fileSize = null,
                        format = format,
                        quality = quality,
                        status = RecordingStatus.FAILED,
                        thumbnailUrl = null,
                        createdAt = System.currentTimeMillis()
                    ),
                    error = e.message
                ))
            }
        }
    }

    /**
     * Остановить запись
     */
    fun stopRecording(cameraId: String) {
        val recordingJob = activeRecordings.remove(cameraId) ?: return
        pausedCameraIds.remove(cameraId)

        serviceScope.launch {
            try {
                recordingJob.job.cancel()

                // Обновляем запись
                val recording = recordingJob.recording.copy(
                    status = RecordingStatus.COMPLETED,
                    endTime = System.currentTimeMillis(),
                    duration = System.currentTimeMillis() - recordingJob.recording.startTime
                )

                recordingRepository.updateRecording(recording)

                // Обновляем состояние
                val currentState = _recordingsState.value
                _recordingsState.value = currentState - cameraId

                // Обновляем уведомление
                if (activeRecordings.isEmpty()) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                } else {
                    updateNotification()
                }
            } catch (e: Exception) {
                // Обработка ошибок
            }
        }
    }

    /**
     * Остановить запись по ID
     */
    fun stopRecordingById(recordingId: String) {
        val entry = activeRecordings.entries.find { it.value.recording.id == recordingId }
        entry?.let { stopRecording(it.key) }
    }

    /**
     * Приостановить запись
     */
    fun pauseRecording(cameraId: String) {
        if (!activeRecordings.containsKey(cameraId)) return

        serviceScope.launch {
            // Обновляем состояние
            val currentState = _recordingsState.value
            val state = currentState[cameraId]
            if (state != null) {
                val pausedRecording = state.recording.copy(status = RecordingStatus.PAUSED)
                _recordingsState.value = currentState + (cameraId to state.copy(isPaused = true))
                recordingRepository.updateRecording(pausedRecording)
            }

            pausedCameraIds.add(cameraId)
            updateNotification()
        }
    }

    /**
     * Возобновить запись
     */
    fun resumeRecording(cameraId: String) {
        if (!activeRecordings.containsKey(cameraId)) return

        serviceScope.launch {
            // Обновляем состояние
            val currentState = _recordingsState.value
            val state = currentState[cameraId]
            if (state != null) {
                val resumedRecording = state.recording.copy(status = RecordingStatus.ACTIVE)
                _recordingsState.value = currentState + (cameraId to state.copy(
                    recording = resumedRecording,
                    isPaused = false
                ))
                recordingRepository.updateRecording(resumedRecording)
            }

            pausedCameraIds.remove(cameraId)
            updateNotification()
        }
    }

    /**
     * Получить активные записи
     */
    fun getActiveRecordings(): List<Recording> {
        return activeRecordings.values.map { it.recording }
    }

    /**
     * Проверить, идет ли запись с камеры
     */
    fun isRecording(cameraId: String): Boolean {
        return activeRecordings.containsKey(cameraId)
    }

    /**
     * Запись потока.
     * В текущем Android baseline используется time-based fallback loop:
     * сервис поддерживает lifecycle, state и pause/resume, но не выполняет
     * прямой RTSP muxing на устройстве до включения production video pipeline.
     */
    private suspend fun recordStream(
        recording: Recording,
        camera: Camera,
        duration: Long?
    ) = withContext(Dispatchers.IO) {
        try {
            val endTime = if (duration != null) {
                recording.startTime + duration
            } else {
                Long.MAX_VALUE
            }

            while (System.currentTimeMillis() < endTime && isActive) {
                delay(1000)
                if (pausedCameraIds.contains(recording.cameraId)) {
                    continue
                }

                // Обновляем длительность
                val currentDuration = System.currentTimeMillis() - recording.startTime
                val updatedRecording = recording.copy(duration = currentDuration)

                // Обновляем состояние
                val currentState = _recordingsState.value
                val state = currentState[recording.cameraId]
                if (state != null) {
                    _recordingsState.value = currentState + (recording.cameraId to state.copy(
                        recording = updatedRecording
                    ))
                }

                // Обновляем уведомление
                updateNotification()
            }

            // Завершаем запись
            if (isActive) {
                stopRecording(recording.cameraId)
            }

        } catch (e: CancellationException) {
            // Запись отменена
            throw e
        } catch (e: Exception) {
            // Ошибка записи
            val currentState = _recordingsState.value
            val state = currentState[recording.cameraId]
            if (state != null) {
                _recordingsState.value = currentState + (recording.cameraId to state.copy(
                    error = e.message
                ))
            }
        }
    }

    /**
     * Создать канал уведомлений
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Recording Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows recording status and controls"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Создать уведомление
     */
    private fun createNotification(): Notification {
        val activeRecordingsList = activeRecordings.values.toList()

        val title = if (activeRecordingsList.size == 1) {
            "Recording: ${activeRecordingsList.first().camera.name}"
        } else {
            "Recording: ${activeRecordingsList.size} cameras"
        }

        val contentText = if (activeRecordingsList.size == 1) {
            val recording = activeRecordingsList.first().recording
            val duration = formatDuration(recording.duration)
            "Duration: $duration"
        } else {
            "Multiple recordings in progress"
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, RecordingService::class.java).apply {
            action = ACTION_STOP_RECORDING
            putExtra(EXTRA_CAMERA_ID, activeRecordingsList.firstOrNull()?.camera?.id)
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "Stop",
                stopPendingIntent
            )
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Обновить уведомление
     */
    private fun updateNotification() {
        if (activeRecordings.isNotEmpty()) {
            notificationManager.notify(NOTIFICATION_ID, createNotification())
        }
    }

    /**
     * Форматировать длительность
     */
    private fun formatDuration(millis: Long): String {
        val seconds = millis / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        return if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes % 60, seconds % 60)
        } else {
            String.format("%02d:%02d", minutes, seconds % 60)
        }
    }
}
