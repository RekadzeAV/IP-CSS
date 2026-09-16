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
import androidx.work.*
import com.company.ipcamera.android.MainActivity
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.model.Event
import com.company.ipcamera.shared.domain.model.EventSeverity
import com.company.ipcamera.shared.domain.model.EventType
import com.company.ipcamera.shared.domain.repository.CameraRepository
import com.company.ipcamera.shared.domain.repository.EventRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.net.InetSocketAddress
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Background Service для мониторинга состояния камер
 *
 * Периодически проверяет доступность камер, обнаруживает события
 * и отправляет уведомления пользователю.
 */
class CameraMonitoringService : Service() {

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private lateinit var notificationManager: NotificationManager
    private lateinit var cameraRepository: CameraRepository
    private lateinit var eventRepository: EventRepository

    private val monitoringJobs = mutableMapOf<String, Job>()
    private var isMonitoring = false

    private val _monitoringState = MutableStateFlow<MonitoringState>(MonitoringState.Idle)
    val monitoringState: StateFlow<MonitoringState> = _monitoringState.asStateFlow()

    companion object {
        private const val CHANNEL_ID = "camera_monitoring_channel"
        private const val NOTIFICATION_ID = 1002
        private const val ACTION_STOP_MONITORING = "com.company.ipcamera.STOP_MONITORING"

        private const val MONITORING_INTERVAL_MINUTES = 5L
        private const val CHECK_TIMEOUT_SECONDS = 10L
    }

    inner class LocalBinder : Binder() {
        fun getService(): CameraMonitoringService = this@CameraMonitoringService
    }

    sealed class MonitoringState {
        object Idle : MonitoringState()
        data class Monitoring(val cameraCount: Int, val lastCheckTime: Long) : MonitoringState()
        data class Error(val message: String) : MonitoringState()
    }

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
            ACTION_STOP_MONITORING -> {
                stopMonitoring()
            }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopMonitoring()
        serviceScope.cancel()
    }

    /**
     * Начать мониторинг камер
     */
    fun startMonitoring(
        cameraRepository: CameraRepository,
        eventRepository: EventRepository
    ) {
        if (isMonitoring) return

        this.cameraRepository = cameraRepository
        this.eventRepository = eventRepository
        isMonitoring = true

        serviceScope.launch {
            try {
                // Получаем все камеры
                val cameras = cameraRepository.getCameras()

                if (cameras.isEmpty()) {
                    _monitoringState.value = MonitoringState.Error("No cameras to monitor")
                    return@launch
                }

                // Запускаем мониторинг для каждой камеры
                cameras.forEach { camera ->
                    val job = serviceScope.launch {
                        monitorCamera(camera)
                    }
                    monitoringJobs[camera.id] = job
                }

                _monitoringState.value = MonitoringState.Monitoring(
                    cameraCount = cameras.size,
                    lastCheckTime = System.currentTimeMillis()
                )

                // Запускаем foreground service
                startForeground(NOTIFICATION_ID, createNotification())

                // Периодическая проверка
                while (isMonitoring) {
                    delay(TimeUnit.MINUTES.toMillis(MONITORING_INTERVAL_MINUTES))
                    checkAllCameras(cameras)
                    _monitoringState.value = MonitoringState.Monitoring(
                        cameraCount = cameras.size,
                        lastCheckTime = System.currentTimeMillis()
                    )
                    updateNotification()
                }

            } catch (e: Exception) {
                _monitoringState.value = MonitoringState.Error(e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Остановить мониторинг
     */
    fun stopMonitoring() {
        isMonitoring = false
        monitoringJobs.values.forEach { it.cancel() }
        monitoringJobs.clear()
        _monitoringState.value = MonitoringState.Idle

        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /**
     * Мониторинг одной камеры
     */
    private suspend fun monitorCamera(camera: Camera) = withContext(Dispatchers.IO) {
        while (isMonitoring && isActive) {
            try {
                // Проверяем доступность камеры
                val isOnline = checkCameraAvailability(camera)

                // Проверяем последнее состояние
                val lastEvent = eventRepository.getEvents(
                    cameraId = camera.id,
                    limit = 1
                ).items.firstOrNull()

                val wasOnline = lastEvent?.type != EventType.CAMERA_OFFLINE

                // Если состояние изменилось, создаем событие
                if (isOnline != wasOnline) {
                    val event = Event(
                        id = java.util.UUID.randomUUID().toString(),
                        cameraId = camera.id,
                        cameraName = camera.name,
                        type = if (isOnline) EventType.CAMERA_ONLINE else EventType.CAMERA_OFFLINE,
                        severity = if (isOnline) EventSeverity.INFO else EventSeverity.WARNING,
                        timestamp = System.currentTimeMillis(),
                        description = if (isOnline) {
                            "Camera ${camera.name} is now online"
                        } else {
                            "Camera ${camera.name} is offline"
                        },
                        acknowledged = false
                    )

                    eventRepository.addEvent(event)

                    // Отправляем уведомление
                    sendCameraStatusNotification(camera, isOnline)
                }

                // Проверяем новые события
                checkCameraEvents(camera)

            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Логируем ошибку, но продолжаем мониторинг
                delay(TimeUnit.SECONDS.toMillis(30)) // Повтор через 30 секунд при ошибке
            }

            delay(TimeUnit.MINUTES.toMillis(MONITORING_INTERVAL_MINUTES))
        }
    }

    /**
     * Проверить доступность камеры
     */
    private suspend fun checkCameraAvailability(camera: Camera): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val normalizedUrl = normalizeCameraUrl(camera.url)
                when {
                    normalizedUrl.startsWith("rtsp://", ignoreCase = true) -> {
                        val uri = URI(normalizedUrl)
                        val host = uri.host ?: return@withContext false
                        val port = if (uri.port > 0) uri.port else 554
                        java.net.Socket().use { socket ->
                            socket.connect(
                                InetSocketAddress(host, port),
                                TimeUnit.SECONDS.toMillis(CHECK_TIMEOUT_SECONDS).toInt()
                            )
                            socket.isConnected
                        }
                    }
                    normalizedUrl.startsWith("http://", ignoreCase = true) ||
                        normalizedUrl.startsWith("https://", ignoreCase = true) -> {
                        val url = URL(normalizedUrl)
                        val connection = (url.openConnection() as HttpURLConnection).apply {
                            requestMethod = "HEAD"
                            connectTimeout = TimeUnit.SECONDS.toMillis(CHECK_TIMEOUT_SECONDS).toInt()
                            readTimeout = TimeUnit.SECONDS.toMillis(CHECK_TIMEOUT_SECONDS).toInt()
                        }
                        try {
                            connection.responseCode in 200..499
                        } finally {
                            connection.disconnect()
                        }
                    }
                    else -> false
                }
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun normalizeCameraUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        if (trimmed.contains("://")) {
            return trimmed
        }
        return "rtsp://$trimmed"
    }

    /**
     * Проверить события камеры
     */
    private suspend fun checkCameraEvents(camera: Camera) {
        try {
            // Получаем последние события
            val recentEvents = eventRepository.getEvents(
                cameraId = camera.id,
                limit = 10
            ).items.filter {
                // События за последний час
                System.currentTimeMillis() - it.timestamp < TimeUnit.HOURS.toMillis(1)
            }

            // Отправляем уведомления для критических событий
            recentEvents.forEach { event ->
                if (event.severity == EventSeverity.CRITICAL ||
                    event.severity == EventSeverity.ERROR) {
                    sendEventNotification(event)
                }
            }

        } catch (e: Exception) {
            // Игнорируем ошибки при проверке событий
        }
    }

    /**
     * Проверить все камеры
     */
    private suspend fun checkAllCameras(cameras: List<Camera>) {
        cameras.forEach { camera ->
            try {
                val isOnline = checkCameraAvailability(camera)
                // Обновляем состояние, если необходимо
            } catch (e: Exception) {
                // Игнорируем ошибки
            }
        }
    }

    /**
     * Отправить уведомление о статусе камеры
     */
    private fun sendCameraStatusNotification(camera: Camera, isOnline: Boolean) {
        val title = if (isOnline) {
            "Camera Online: ${camera.name}"
        } else {
            "Camera Offline: ${camera.name}"
        }

        val contentText = if (isOnline) {
            "Camera ${camera.name} is now available"
        } else {
            "Camera ${camera.name} is not responding"
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("camera_id", camera.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, camera.id.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(if (isOnline) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(camera.id.hashCode(), notification)
    }

    /**
     * Отправить уведомление о событии
     */
    private fun sendEventNotification(event: Event) {
        val title = "${event.type.name}: ${event.cameraName ?: "Unknown"}"
        val contentText = event.description ?: "Event detected"

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("event_id", event.id)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, event.id.hashCode(), intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(
                when (event.severity) {
                    EventSeverity.CRITICAL -> NotificationCompat.PRIORITY_MAX
                    EventSeverity.ERROR -> NotificationCompat.PRIORITY_HIGH
                    EventSeverity.WARNING -> NotificationCompat.PRIORITY_DEFAULT
                    else -> NotificationCompat.PRIORITY_LOW
                }
            )
            .build()

        notificationManager.notify(event.id.hashCode(), notification)
    }

    /**
     * Создать канал уведомлений
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Camera Monitoring",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications about camera status and events"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Создать уведомление
     */
    private fun createNotification(): Notification {
        val state = _monitoringState.value
        val title = when (state) {
            is MonitoringState.Monitoring -> "Monitoring ${state.cameraCount} cameras"
            else -> "Camera Monitoring"
        }

        val contentText = when (state) {
            is MonitoringState.Monitoring -> {
                val lastCheck = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                    .format(java.util.Date(state.lastCheckTime))
                "Last check: $lastCheck"
            }
            else -> "Monitoring camera status"
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    /**
     * Обновить уведомление
     */
    private fun updateNotification() {
        notificationManager.notify(NOTIFICATION_ID, createNotification())
    }
}
