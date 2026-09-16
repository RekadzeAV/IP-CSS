package com.company.ipcamera.desktop.ui.viewmodel

import com.company.ipcamera.desktop.reconnect.ReconnectController
import com.company.ipcamera.desktop.reconnect.ReconnectEvent
import com.company.ipcamera.desktop.reconnect.ReconnectPolicy
import com.company.ipcamera.shared.domain.model.Camera
import com.company.ipcamera.shared.domain.usecase.GetCamerasUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.prefs.Preferences
import kotlin.math.abs
import kotlin.math.pow

enum class GridLayout {
    SINGLE,    // 1 камера
    GRID_4,    // 2x2 (4 камеры)
    GRID_9,    // 3x3 (9 камер)
    GRID_16    // 4x4 (16 камер)
}

enum class StreamPriority {
    HIGH,
    NORMAL,
    BACKGROUND
}

enum class VisibilityGraceMode {
    AUTO,
    CUSTOM
}

data class LiveViewState(
    val cameras: List<Camera> = emptyList(),
    val selectedCameras: List<String> = emptyList(),
    val focusedCameraId: String? = null,
    val gridLayout: GridLayout = GridLayout.SINGLE,
    val safeRestoreEnabled: Boolean = true,
    val restoreMaxAgeHours: Int = 24,
    val telemetryThresholds: TelemetryThresholds = TelemetryThresholds(),
    val visibilityGraceMs: Long = 1_500L,
    val visibilityGraceMode: VisibilityGraceMode = VisibilityGraceMode.AUTO,
    val pendingRestoreCameraIds: List<String> = emptyList(),
    val cameraMetrics: Map<String, CameraPlaybackMetrics> = emptyMap(),
    val cameraReconnectPolicies: Map<String, ReconnectPolicy> = emptyMap(),
    val telemetrySummary: LiveTelemetrySummary = LiveTelemetrySummary(),
    val reconnectWarnings: Map<String, ReconnectWarning> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ReconnectWarning(
    val cameraId: String,
    val attempt: Int,
    val maxAttempts: Int,
    val lastError: String,
    val nextRetryInMs: Long?,
    val isExhausted: Boolean,
    val timestampMs: Long = System.currentTimeMillis()
)

/**
 * Статус reconnect для камеры.
 */
sealed class CameraReconnectStatus {
    /** Камера стабильна, проблем с подключением нет. */
    data object Stable : CameraReconnectStatus()
    
    /** Камера нестабильна, были ошибки но reconnect еще не начался. */
    data class Unstable(
        val errorCount: Int,
        val lastErrorAtMs: Long?
    ) : CameraReconnectStatus()
    
    /** Камера в процессе reconnect. */
    data class Reconnecting(
        val attempt: Int,
        val maxAttempts: Int,
        val lastError: String,
        val nextRetryInMs: Long?
    ) : CameraReconnectStatus()
    
    /** Исчерпаны все попытки reconnect. */
    data class Exhausted(
        val attempt: Int,
        val maxAttempts: Int,
        val lastError: String
    ) : CameraReconnectStatus()
}

data class CameraPlaybackMetrics(
    val startupTimeMs: Long? = null,
    val reconnectAttempts: Int = 0,
    val streamErrorCount: Int = 0,
    val lastErrorAtMs: Long? = null,
    val renderFps: Int = 0,
    val droppedFrames: Int = 0
)

data class LiveTelemetrySummary(
    val activeStreams: Int = 0,
    val streamsWithStartup: Int = 0,
    val avgStartupMs: Long? = null,
    val avgRenderFps: Int? = null,
    val totalDroppedFrames: Int = 0,
    val totalReconnects: Int = 0,
    val totalErrors: Int = 0,
    val reconnectsLast5m: Int = 0,
    val errorsLast5m: Int = 0,
    val reconnectsLast15m: Int = 0,
    val errorsLast15m: Int = 0,
    val warning: TelemetryWarning = TelemetryWarning.NONE
)

enum class TelemetryWarning {
    NONE,
    RECONNECT_STORM,
    HIGH_ERROR_RATE,
    SLOW_STARTUP
}

private data class TelemetrySample(
    val timestampMs: Long,
    val reconnectAttempts: Int,
    val streamErrorCount: Int
)

data class TelemetryThresholds(
    val reconnects5mWarn: Int = 4,
    val reconnects15mWarn: Int = 10,
    val errors5mWarn: Int = 3,
    val errors15mWarn: Int = 8,
    val slowStartupMsWarn: Long = 5_000L,
    val warningCooldownMs: Long = 60_000L
) {
    companion object {
        fun fromEnvironment(): TelemetryThresholds {
            fun envInt(name: String, default: Int): Int =
                System.getenv(name)?.toIntOrNull() ?: default
            fun envLong(name: String, default: Long): Long =
                System.getenv(name)?.toLongOrNull() ?: default

            return TelemetryThresholds(
                reconnects5mWarn = envInt("IPCSS_TELEMETRY_RECONNECTS_5M_WARN", 4),
                reconnects15mWarn = envInt("IPCSS_TELEMETRY_RECONNECTS_15M_WARN", 10),
                errors5mWarn = envInt("IPCSS_TELEMETRY_ERRORS_5M_WARN", 3),
                errors15mWarn = envInt("IPCSS_TELEMETRY_ERRORS_15M_WARN", 8),
                slowStartupMsWarn = envLong("IPCSS_TELEMETRY_SLOW_STARTUP_MS_WARN", 5_000L),
                warningCooldownMs = envLong("IPCSS_TELEMETRY_WARNING_COOLDOWN_MS", 60_000L)
            )
        }
    }
}

data class MetricsPublishConfig(
    val minMetricsPublishIntervalMs: Long = 1_000L,
    val minRenderFpsDeltaToPublish: Int = 2,
    val minDroppedFramesDeltaToPublish: Int = 5
) {
    companion object {
        fun fromEnvironment(): MetricsPublishConfig {
            fun envLong(name: String, default: Long): Long =
                System.getenv(name)?.toLongOrNull() ?: default
            fun envInt(name: String, default: Int): Int =
                System.getenv(name)?.toIntOrNull() ?: default

            return MetricsPublishConfig(
                minMetricsPublishIntervalMs = envLong("IPCSS_METRICS_PUBLISH_MIN_INTERVAL_MS", 1_000L).coerceIn(100L, 60_000L),
                minRenderFpsDeltaToPublish = envInt("IPCSS_METRICS_RENDER_FPS_DELTA", 2).coerceIn(1, 30),
                minDroppedFramesDeltaToPublish = envInt("IPCSS_METRICS_DROPPED_DELTA", 5).coerceIn(1, 500)
            )
        }
    }
}

class LiveViewViewModel(
    private val getCamerasUseCase: GetCamerasUseCase,
    private val coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Main),
    private val initialTelemetryThresholds: TelemetryThresholds = TelemetryThresholds.fromEnvironment(),
    private val metricsPublishConfig: MetricsPublishConfig = MetricsPublishConfig.fromEnvironment(),
    private val defaultReconnectPolicy: ReconnectPolicy = ReconnectPolicy.fromEnvironment()
) {
    private val minMetricsPublishIntervalMs = metricsPublishConfig.minMetricsPublishIntervalMs
    private val minRenderFpsDeltaToPublish = metricsPublishConfig.minRenderFpsDeltaToPublish
    private val minDroppedFramesDeltaToPublish = metricsPublishConfig.minDroppedFramesDeltaToPublish
    private var visibleCameraIds: Set<String> = emptySet()
    private val recentlyVisibleAtMs: MutableMap<String, Long> = mutableMapOf()
    private val prefs = Preferences.userRoot().node("com.company.ipcamera.desktop.live.state")
    
    // Reconnect controller for managing automatic reconnection
    private val reconnectController = ReconnectController(
        reconnectCallback = { cameraId ->
            // Callback to attempt reconnect - in real implementation this would trigger session reconnect
            // For now, we just track the attempt in metrics
            handleReconnectAttempt(cameraId)
        },
        eventCallback = { event -> handleReconnectEvent(event) },
        coroutineScope = coroutineScope
    )
    private val keyGridLayout = "grid_layout"
    private val keyFocusedCameraId = "focused_camera_id"
    private val keySelectedCameras = "selected_camera_ids"
    private val keyStateSavedAtMs = "state_saved_at_ms"
    private val keySafeRestoreEnabled = "safe_restore_enabled"
    private val keyRestoreMaxAgeMs = "restore_max_age_ms"
    private val keyReconnects5mWarn = "telemetry_reconnects_5m_warn"
    private val keyReconnects15mWarn = "telemetry_reconnects_15m_warn"
    private val keyErrors5mWarn = "telemetry_errors_5m_warn"
    private val keyErrors15mWarn = "telemetry_errors_15m_warn"
    private val keySlowStartupMsWarn = "telemetry_slow_startup_ms_warn"
    private val keyWarningCooldownMs = "telemetry_warning_cooldown_ms"
    private val keyVisibilityGraceMs = "visibility_grace_ms"
    private val keyVisibilityGraceMode = "visibility_grace_mode"

    private val defaultSafeRestoreEnabled =
        (System.getenv("IPCSS_LIVE_SAFE_RESTORE_ENABLED") ?: "true").equals("true", ignoreCase = true)
    private val defaultRestoreMaxAgeMs =
        System.getenv("IPCSS_LIVE_RESTORE_MAX_AGE_MS")?.toLongOrNull() ?: (24 * 60 * 60 * 1000L)
    private val defaultVisibilityGraceMs =
        System.getenv("IPCSS_LIVE_VISIBILITY_GRACE_MS")?.toLongOrNull() ?: 1_500L
    private val defaultTelemetryThresholds = initialTelemetryThresholds
    private var telemetryThresholds = TelemetryThresholds(
        reconnects5mWarn = prefs.getInt(keyReconnects5mWarn, defaultTelemetryThresholds.reconnects5mWarn),
        reconnects15mWarn = prefs.getInt(keyReconnects15mWarn, defaultTelemetryThresholds.reconnects15mWarn),
        errors5mWarn = prefs.getInt(keyErrors5mWarn, defaultTelemetryThresholds.errors5mWarn),
        errors15mWarn = prefs.getInt(keyErrors15mWarn, defaultTelemetryThresholds.errors15mWarn),
        slowStartupMsWarn = prefs.getLong(keySlowStartupMsWarn, defaultTelemetryThresholds.slowStartupMsWarn),
        warningCooldownMs = prefs.getLong(keyWarningCooldownMs, defaultTelemetryThresholds.warningCooldownMs)
    )
    private var safeRestoreEnabled =
        prefs.getBoolean(keySafeRestoreEnabled, defaultSafeRestoreEnabled)
    private var restoreMaxAgeMs =
        prefs.getLong(keyRestoreMaxAgeMs, defaultRestoreMaxAgeMs).coerceAtLeast(60 * 60 * 1000L)
    private var visibilityGraceMs =
        prefs.getLong(keyVisibilityGraceMs, defaultVisibilityGraceMs).coerceIn(0L, 10_000L)
    private var visibilityGraceMode = readPersistedVisibilityGraceMode()

    private val metricsHistory: MutableMap<String, MutableList<TelemetrySample>> = mutableMapOf()
    private val lastMetricsPublishedAtMs: MutableMap<String, Long> = mutableMapOf()
    private var currentWarning: TelemetryWarning = TelemetryWarning.NONE
    private var lastWarningChangedAtMs: Long = 0L
    private val _state = MutableStateFlow(
        LiveViewState(
            selectedCameras = readPersistedSelectedCameras(),
            focusedCameraId = readPersistedFocusedCameraId(),
            gridLayout = readPersistedGridLayout(),
            safeRestoreEnabled = safeRestoreEnabled,
            restoreMaxAgeHours = (restoreMaxAgeMs / (60 * 60 * 1000L)).toInt(),
            telemetryThresholds = telemetryThresholds,
            visibilityGraceMs = visibilityGraceMs,
            visibilityGraceMode = visibilityGraceMode
        )
    )
    val state: StateFlow<LiveViewState> = _state.asStateFlow()
    
    init {
        loadCameras()
    }

    fun loadCameras() {
        coroutineScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val cameras = getCamerasUseCase.invoke()
                val cameraIds = cameras.map { it.id }.toSet()
                val restoredSelected = _state.value.selectedCameras.filter { it in cameraIds }
                val trimmedSelected = trimByGridLayout(restoredSelected, _state.value.gridLayout)
                val focused = _state.value.focusedCameraId?.takeIf { it in trimmedSelected } ?: trimmedSelected.firstOrNull()
                val (selectedForStartup, pendingRestore) = if (safeRestoreEnabled && trimmedSelected.size > 1) {
                    val startupSelection = focused?.let { listOf(it) } ?: listOf(trimmedSelected.first())
                    val pending = trimmedSelected.filterNot { it in startupSelection }
                    startupSelection to pending
                } else {
                    trimmedSelected to emptyList()
                }
                _state.value = _state.value.copy(
                    cameras = cameras,
                    selectedCameras = selectedForStartup,
                    focusedCameraId = focused?.takeIf { it in selectedForStartup } ?: selectedForStartup.firstOrNull(),
                    pendingRestoreCameraIds = pendingRestore,
                    isLoading = false
                )
                persistLiveState()
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Не удалось загрузить камеры"
                )
            }
        }
    }
    
    fun selectCamera(cameraId: String) {
        val current = _state.value.selectedCameras.toMutableList()
        var focusedCameraId = _state.value.focusedCameraId
        if (current.contains(cameraId)) {
            current.remove(cameraId)
            if (focusedCameraId == cameraId) {
                focusedCameraId = current.firstOrNull()
            }
        } else {
            val maxCameras = when (_state.value.gridLayout) {
                GridLayout.SINGLE -> 1
                GridLayout.GRID_4 -> 4
                GridLayout.GRID_9 -> 9
                GridLayout.GRID_16 -> 16
            }
            if (current.size < maxCameras) {
                current.add(cameraId)
                if (focusedCameraId == null) {
                    focusedCameraId = cameraId
                }
            }
        }
        _state.value = _state.value.copy(
            selectedCameras = current,
            focusedCameraId = focusedCameraId,
            pendingRestoreCameraIds = _state.value.pendingRestoreCameraIds.filter { it in current }
        )
        persistLiveState()
    }
    
    fun removeCamera(cameraId: String) {
        val current = _state.value.selectedCameras.toMutableList()
        current.remove(cameraId)
        val focusedCameraId = if (_state.value.focusedCameraId == cameraId) {
            current.firstOrNull()
        } else {
            _state.value.focusedCameraId
        }
        val newMetrics = _state.value.cameraMetrics.toMutableMap()
        newMetrics.remove(cameraId)
        metricsHistory.remove(cameraId)
        val pendingRestore = _state.value.pendingRestoreCameraIds.filter { it != cameraId }
        
        // Очищаем reconnect ресурсы для камеры
        reconnectController.dispose(cameraId)
        val newWarnings = _state.value.reconnectWarnings.toMutableMap()
        newWarnings.remove(cameraId)
        val newPolicies = _state.value.cameraReconnectPolicies.toMutableMap()
        newPolicies.remove(cameraId)
        
        _state.value = _state.value.copy(
            selectedCameras = current,
            focusedCameraId = focusedCameraId,
            pendingRestoreCameraIds = pendingRestore,
            cameraMetrics = newMetrics,
            cameraReconnectPolicies = newPolicies,
            reconnectWarnings = newWarnings,
            telemetrySummary = buildTelemetrySummary(current, newMetrics)
        )
        persistLiveState()
    }

    fun setGridLayout(layout: GridLayout) {
        val previousLayout = _state.value.gridLayout
        val previousRecommendedGrace = recommendedVisibilityGraceForLayout(previousLayout)
        val nextRecommendedGrace = recommendedVisibilityGraceForLayout(layout)
        val shouldAutoAdjustGrace = visibilityGraceMode == VisibilityGraceMode.AUTO

        val current = _state.value.selectedCameras.toMutableList()
        val maxCameras = when (layout) {
            GridLayout.SINGLE -> 1
            GridLayout.GRID_4 -> 4
            GridLayout.GRID_9 -> 9
            GridLayout.GRID_16 -> 16
        }
        // Удаляем лишние камеры, если новый layout меньше
        while (current.size > maxCameras) {
            current.removeLast()
        }
        val allowedIds = current.toSet()
        val filteredMetrics = _state.value.cameraMetrics.filterKeys { it in allowedIds }
        metricsHistory.keys.toList().forEach { id ->
            if (id !in allowedIds) metricsHistory.remove(id)
        }
        val pendingRestore = _state.value.pendingRestoreCameraIds.filter { it in allowedIds }
        val focusedCameraId = _state.value.focusedCameraId?.takeIf { it in allowedIds } ?: current.firstOrNull()
        val effectiveGraceMs = if (shouldAutoAdjustGrace) nextRecommendedGrace else visibilityGraceMs
        if (shouldAutoAdjustGrace) {
            visibilityGraceMs = effectiveGraceMs
            prefs.putLong(keyVisibilityGraceMs, visibilityGraceMs)
            visibilityGraceMode = VisibilityGraceMode.AUTO
            prefs.put(keyVisibilityGraceMode, visibilityGraceMode.name)
        }
        _state.value = _state.value.copy(
            gridLayout = layout,
            selectedCameras = current,
            focusedCameraId = focusedCameraId,
            pendingRestoreCameraIds = pendingRestore,
            cameraMetrics = filteredMetrics,
            telemetrySummary = buildTelemetrySummary(current, filteredMetrics),
            visibilityGraceMs = effectiveGraceMs,
            visibilityGraceMode = visibilityGraceMode
        )
        persistLiveState()
    }

    fun getSelectedCameras(): List<Camera> {
        val selectedIds = _state.value.selectedCameras
        return _state.value.cameras.filter { it.id in selectedIds }
    }

    fun setFocusedCamera(cameraId: String) {
        if (cameraId !in _state.value.selectedCameras) return
        _state.value = _state.value.copy(focusedCameraId = cameraId)
        persistLiveState()
    }

    fun getStreamPriority(cameraId: String): StreamPriority {
        val selected = _state.value.selectedCameras
        if (cameraId !in selected) return StreamPriority.BACKGROUND
        if (_state.value.focusedCameraId == cameraId) return StreamPriority.HIGH
        if (visibleCameraIds.isNotEmpty() && cameraId !in visibleCameraIds) {
            val lastSeenAt = recentlyVisibleAtMs[cameraId]
            val isWithinGrace = lastSeenAt != null && (System.currentTimeMillis() - lastSeenAt) <= visibilityGraceMs
            if (!isWithinGrace) return StreamPriority.BACKGROUND
        }

        val normalBudget = when (_state.value.gridLayout) {
            GridLayout.SINGLE -> 1
            GridLayout.GRID_4 -> 2
            GridLayout.GRID_9 -> 4
            GridLayout.GRID_16 -> 6
        }
        val index = selected.indexOf(cameraId)
        return if (index in 0 until normalBudget) {
            StreamPriority.NORMAL
        } else {
            StreamPriority.BACKGROUND
        }
    }

    fun updateVisibleCameraIds(cameraIds: Set<String>) {
        visibleCameraIds = cameraIds
        val now = System.currentTimeMillis()
        cameraIds.forEach { recentlyVisibleAtMs[it] = now }
        val selectedSet = _state.value.selectedCameras.toSet()
        recentlyVisibleAtMs.keys.removeIf { it !in selectedSet }
    }

    /**
     * Обработать попытку reconnect.
     */
    private fun handleReconnectAttempt(cameraId: String): Boolean {
        // Увеличиваем счетчик reconnect attempts в метриках
        val currentMetrics = _state.value.cameraMetrics[cameraId]
        if (currentMetrics != null) {
            val updatedMetrics = currentMetrics.copy(
                reconnectAttempts = currentMetrics.reconnectAttempts + 1
            )
            val newMetrics = _state.value.cameraMetrics.toMutableMap()
            newMetrics[cameraId] = updatedMetrics
            _state.value = _state.value.copy(cameraMetrics = newMetrics)
        }
        // В реальной реализации здесь был бы вызов session.reconnect()
        return true // Предполагаем успех для демонстрации
    }

    /**
     * Обработать событие reconnect от контроллера.
     */
    private fun handleReconnectEvent(event: ReconnectEvent) {
        when (event) {
            is ReconnectEvent.ReconnectStarted -> {
                updateReconnectWarning(event.cameraId, event.attempt, event.maxAttempts, event.reason ?: "Reconnect started", null, false)
            }
            is ReconnectEvent.ReconnectSuccess -> {
                clearReconnectWarning(event.cameraId)
            }
            is ReconnectEvent.ReconnectFailed -> {
                val nextRetry = if (event.willRetry) calculateNextRetryMs(event.attempt) else null
                updateReconnectWarning(
                    event.cameraId,
                    event.attempt,
                    event.maxAttempts,
                    event.error,
                    nextRetry,
                    isExhausted = !event.willRetry
                )
            }
            is ReconnectEvent.ReconnectExhausted -> {
                updateReconnectWarning(
                    event.cameraId,
                    event.totalAttempts,
                    event.totalAttempts,
                    event.lastError,
                    null,
                    isExhausted = true
                )
            }
            is ReconnectEvent.ReconnectCancelled -> {
                clearReconnectWarning(event.cameraId)
            }
            is ReconnectEvent.ErrorOccurred -> {
                // Обновляем метрики ошибки
                val currentMetrics = _state.value.cameraMetrics[event.cameraId]
                if (currentMetrics != null) {
                    val updatedMetrics = currentMetrics.copy(
                        streamErrorCount = currentMetrics.streamErrorCount + 1,
                        lastErrorAtMs = System.currentTimeMillis()
                    )
                    val newMetrics = _state.value.cameraMetrics.toMutableMap()
                    newMetrics[event.cameraId] = updatedMetrics
                    _state.value = _state.value.copy(cameraMetrics = newMetrics)
                }
            }
        }
    }

    private fun updateReconnectWarning(
        cameraId: String,
        attempt: Int,
        maxAttempts: Int,
        lastError: String,
        nextRetryInMs: Long?,
        isExhausted: Boolean
    ) {
        val currentWarnings = _state.value.reconnectWarnings.toMutableMap()
        if (isExhausted || attempt >= maxAttempts) {
            currentWarnings[cameraId] = ReconnectWarning(
                cameraId = cameraId,
                attempt = attempt,
                maxAttempts = maxAttempts,
                lastError = lastError,
                nextRetryInMs = null,
                isExhausted = true
            )
        } else {
            currentWarnings[cameraId] = ReconnectWarning(
                cameraId = cameraId,
                attempt = attempt,
                maxAttempts = maxAttempts,
                lastError = lastError,
                nextRetryInMs = nextRetryInMs,
                isExhausted = false
            )
        }
        _state.value = _state.value.copy(reconnectWarnings = currentWarnings)
    }

    private fun clearReconnectWarning(cameraId: String) {
        val currentWarnings = _state.value.reconnectWarnings.toMutableMap()
        currentWarnings.remove(cameraId)
        _state.value = _state.value.copy(reconnectWarnings = currentWarnings)
    }

    private fun calculateNextRetryMs(attempt: Int): Long {
        // Упрощенный расчет - в реальности используется calculateReconnectDelay
        val baseDelay = defaultReconnectPolicy.initialDelayMs
        val exponentialDelay = (baseDelay * (2.0.pow(attempt - 1))).toLong()
        return exponentialDelay.coerceAtMost(defaultReconnectPolicy.maxDelayMs)
    }

    /**
     * Запросить ручной reconnect для камеры.
     */
    fun requestManualReconnect(cameraId: String) {
        // Сбрасываем состояние reconnect
        reconnectController.onConnected(cameraId)
        // Очищаем предупреждение
        clearReconnectWarning(cameraId)
        // В реальной реализации здесь был бы вызов session.manualReconnect()
    }

    /**
     * Получить текущий статус reconnect для камеры.
     */
    fun getReconnectStatus(cameraId: String): CameraReconnectStatus {
        val warning = _state.value.reconnectWarnings[cameraId]
        val metrics = _state.value.cameraMetrics[cameraId]
        
        return when {
            warning != null && warning.isExhausted -> CameraReconnectStatus.Exhausted(
                attempt = warning.attempt,
                maxAttempts = warning.maxAttempts,
                lastError = warning.lastError
            )
            warning != null -> CameraReconnectStatus.Reconnecting(
                attempt = warning.attempt,
                maxAttempts = warning.maxAttempts,
                lastError = warning.lastError,
                nextRetryInMs = warning.nextRetryInMs
            )
            metrics?.streamErrorCount ?: 0 > 0 -> CameraReconnectStatus.Unstable(
                errorCount = metrics!!.streamErrorCount,
                lastErrorAtMs = metrics.lastErrorAtMs
            )
            else -> CameraReconnectStatus.Stable
        }
    }

    /**
     * Получить политику reconnect для камеры.
     */
    fun getReconnectPolicy(cameraId: String): ReconnectPolicy {
        return _state.value.cameraReconnectPolicies[cameraId] ?: defaultReconnectPolicy
    }

    /**
     * Установить политику reconnect для камеры.
     */
    fun setReconnectPolicy(cameraId: String, policy: ReconnectPolicy) {
        val currentPolicies = _state.value.cameraReconnectPolicies.toMutableMap()
        currentPolicies[cameraId] = policy
        _state.value = _state.value.copy(cameraReconnectPolicies = currentPolicies)
        
        // Обновляем политику в контроллере
        reconnectController.startReconnectMonitoring(cameraId, policy)
    }

    /**
     * Установить глобальную политику reconnect для всех камер.
     */
    fun setGlobalReconnectPolicy(policy: ReconnectPolicy) {
        val currentPolicies = _state.value.cameraReconnectPolicies.toMutableMap()
        _state.value.selectedCameras.forEach { cameraId ->
            currentPolicies[cameraId] = policy
        }
        _state.value = _state.value.copy(cameraReconnectPolicies = currentPolicies)
        
        // Обновляем политику для всех камер в контроллере
        _state.value.selectedCameras.forEach { cameraId ->
            reconnectController.startReconnectMonitoring(cameraId, policy)
        }
    }

    fun setVisibilityGraceMs(ms: Long) {
        visibilityGraceMs = ms.coerceIn(0L, 10_000L)
        visibilityGraceMode = VisibilityGraceMode.CUSTOM
        prefs.putLong(keyVisibilityGraceMs, visibilityGraceMs)
        prefs.put(keyVisibilityGraceMode, visibilityGraceMode.name)
        _state.value = _state.value.copy(
            visibilityGraceMs = visibilityGraceMs,
            visibilityGraceMode = visibilityGraceMode
        )
    }

    fun setVisibilityGraceAuto() {
        visibilityGraceMs = recommendedVisibilityGraceForLayout(_state.value.gridLayout)
        visibilityGraceMode = VisibilityGraceMode.AUTO
        prefs.putLong(keyVisibilityGraceMs, visibilityGraceMs)
        prefs.put(keyVisibilityGraceMode, visibilityGraceMode.name)
        _state.value = _state.value.copy(
            visibilityGraceMs = visibilityGraceMs,
            visibilityGraceMode = visibilityGraceMode
        )
    }

    private fun recommendedVisibilityGraceForLayout(layout: GridLayout): Long {
        return when (layout) {
            GridLayout.SINGLE,
            GridLayout.GRID_4 -> 500L
            GridLayout.GRID_9 -> 1_500L
            GridLayout.GRID_16 -> 3_000L
        }
    }

    fun updateCameraMetrics(cameraId: String, metrics: CameraPlaybackMetrics) {
        // Сохраняем метрики только для камер в текущей сетке.
        if (cameraId !in _state.value.selectedCameras) return

        val previous = _state.value.cameraMetrics[cameraId]
        val now = System.currentTimeMillis()
        if (!shouldPublishCameraMetrics(previous, metrics, now, cameraId)) return

        val newMetrics = _state.value.cameraMetrics.toMutableMap()
        newMetrics[cameraId] = metrics
        appendHistorySample(cameraId, metrics)
        lastMetricsPublishedAtMs[cameraId] = now
        val selected = _state.value.selectedCameras
        _state.value = _state.value.copy(
            cameraMetrics = newMetrics,
            telemetrySummary = buildTelemetrySummary(selected, newMetrics)
        )
    }

    private fun shouldPublishCameraMetrics(
        previous: CameraPlaybackMetrics?,
        next: CameraPlaybackMetrics,
        nowMs: Long,
        cameraId: String
    ): Boolean {
        if (previous == null) return true

        // Критические метрики публикуем сразу.
        if (previous.startupTimeMs != next.startupTimeMs) return true
        if (previous.reconnectAttempts != next.reconnectAttempts) return true
        if (previous.streamErrorCount != next.streamErrorCount) return true
        if (previous.lastErrorAtMs != next.lastErrorAtMs) return true

        val fpsDelta = abs(next.renderFps - previous.renderFps)
        if (fpsDelta >= minRenderFpsDeltaToPublish) return true

        val droppedDelta = abs(next.droppedFrames - previous.droppedFrames)
        if (droppedDelta >= minDroppedFramesDeltaToPublish) return true

        val lastPublishAt = lastMetricsPublishedAtMs[cameraId] ?: 0L
        val renderChanged = next.renderFps != previous.renderFps || next.droppedFrames != previous.droppedFrames
        return renderChanged && (nowMs - lastPublishAt >= minMetricsPublishIntervalMs)
    }

    fun resetTelemetry() {
        val selected = _state.value.selectedCameras
        metricsHistory.clear()
        currentWarning = TelemetryWarning.NONE
        lastWarningChangedAtMs = 0L
        _state.value = _state.value.copy(
            cameraMetrics = emptyMap(),
            telemetrySummary = buildTelemetrySummary(selected, emptyMap())
        )
    }

    fun restoreAllPendingCameras() {
        val current = _state.value.selectedCameras.toMutableList()
        val maxCameras = when (_state.value.gridLayout) {
            GridLayout.SINGLE -> 1
            GridLayout.GRID_4 -> 4
            GridLayout.GRID_9 -> 9
            GridLayout.GRID_16 -> 16
        }
        val toRestore = _state.value.pendingRestoreCameraIds
        toRestore.forEach { cameraId ->
            if (current.size < maxCameras && cameraId !in current) current.add(cameraId)
        }
        _state.value = _state.value.copy(
            selectedCameras = current,
            pendingRestoreCameraIds = emptyList()
        )
        persistLiveState()
    }

    fun setSafeRestoreEnabled(enabled: Boolean) {
        safeRestoreEnabled = enabled
        prefs.putBoolean(keySafeRestoreEnabled, enabled)
        _state.value = _state.value.copy(safeRestoreEnabled = enabled)
    }

    fun setRestoreMaxAgeHours(hours: Int) {
        val clamped = hours.coerceIn(1, 24 * 30)
        restoreMaxAgeMs = clamped * 60L * 60L * 1000L
        prefs.putLong(keyRestoreMaxAgeMs, restoreMaxAgeMs)
        _state.value = _state.value.copy(restoreMaxAgeHours = clamped)
    }

    fun resetTelemetryThresholdsToDefaults() {
        telemetryThresholds = defaultTelemetryThresholds
        persistTelemetryThresholds()
        _state.value = _state.value.copy(telemetryThresholds = telemetryThresholds)
    }

    fun updateTelemetryThresholds(
        reconnects5mWarn: Int,
        reconnects15mWarn: Int,
        errors5mWarn: Int,
        errors15mWarn: Int,
        slowStartupMsWarn: Long,
        warningCooldownMs: Long
    ) {
        telemetryThresholds = TelemetryThresholds(
            reconnects5mWarn = reconnects5mWarn.coerceIn(1, 1000),
            reconnects15mWarn = reconnects15mWarn.coerceIn(1, 5000),
            errors5mWarn = errors5mWarn.coerceIn(1, 1000),
            errors15mWarn = errors15mWarn.coerceIn(1, 5000),
            slowStartupMsWarn = slowStartupMsWarn.coerceIn(500L, 120_000L),
            warningCooldownMs = warningCooldownMs.coerceIn(1_000L, 3_600_000L)
        )
        persistTelemetryThresholds()
        _state.value = _state.value.copy(
            telemetryThresholds = telemetryThresholds,
            telemetrySummary = buildTelemetrySummary(_state.value.selectedCameras, _state.value.cameraMetrics)
        )
    }

    private fun buildTelemetrySummary(
        selectedCameraIds: List<String>,
        metricsMap: Map<String, CameraPlaybackMetrics>
    ): LiveTelemetrySummary {
        if (selectedCameraIds.isEmpty()) return LiveTelemetrySummary()
        val selectedMetrics = selectedCameraIds.mapNotNull { metricsMap[it] }
        val startupValues = selectedMetrics.mapNotNull { it.startupTimeMs }
        val avgStartup = if (startupValues.isEmpty()) null else startupValues.average().toLong()
        val avgRenderFps = if (selectedMetrics.isEmpty()) null else selectedMetrics.map { it.renderFps }.average().toInt()
        val reconnects5m = selectedCameraIds.sumOf { recentDelta(it, 5 * 60 * 1000L) { s -> s.reconnectAttempts } }
        val errors5m = selectedCameraIds.sumOf { recentDelta(it, 5 * 60 * 1000L) { s -> s.streamErrorCount } }
        val reconnects15m = selectedCameraIds.sumOf { recentDelta(it, 15 * 60 * 1000L) { s -> s.reconnectAttempts } }
        val errors15m = selectedCameraIds.sumOf { recentDelta(it, 15 * 60 * 1000L) { s -> s.streamErrorCount } }
        val targetWarning = detectWarning(
            avgStartupMs = avgStartup,
            reconnects5m = reconnects5m,
            errors5m = errors5m,
            reconnects15m = reconnects15m,
            errors15m = errors15m
        )
        val stableWarning = applyWarningCooldown(targetWarning)
        return LiveTelemetrySummary(
            activeStreams = selectedCameraIds.size,
            streamsWithStartup = startupValues.size,
            avgStartupMs = avgStartup,
            avgRenderFps = avgRenderFps,
            totalDroppedFrames = selectedMetrics.sumOf { it.droppedFrames },
            totalReconnects = selectedMetrics.sumOf { it.reconnectAttempts },
            totalErrors = selectedMetrics.sumOf { it.streamErrorCount },
            reconnectsLast5m = reconnects5m,
            errorsLast5m = errors5m,
            reconnectsLast15m = reconnects15m,
            errorsLast15m = errors15m,
            warning = stableWarning
        )
    }

    private fun detectWarning(
        avgStartupMs: Long?,
        reconnects5m: Int,
        errors5m: Int,
        reconnects15m: Int,
        errors15m: Int
    ): TelemetryWarning {
        return when {
            reconnects5m >= this.telemetryThresholds.reconnects5mWarn ||
                reconnects15m >= this.telemetryThresholds.reconnects15mWarn -> TelemetryWarning.RECONNECT_STORM
            errors5m >= this.telemetryThresholds.errors5mWarn ||
                errors15m >= this.telemetryThresholds.errors15mWarn -> TelemetryWarning.HIGH_ERROR_RATE
            (avgStartupMs ?: 0L) >= this.telemetryThresholds.slowStartupMsWarn -> TelemetryWarning.SLOW_STARTUP
            else -> TelemetryWarning.NONE
        }
    }

    private fun applyWarningCooldown(target: TelemetryWarning): TelemetryWarning {
        val now = System.currentTimeMillis()
        if (target == currentWarning) return currentWarning
        if (lastWarningChangedAtMs == 0L || now - lastWarningChangedAtMs >= this.telemetryThresholds.warningCooldownMs) {
            currentWarning = target
            lastWarningChangedAtMs = now
        }
        return currentWarning
    }

    private fun appendHistorySample(cameraId: String, metrics: CameraPlaybackMetrics) {
        val now = System.currentTimeMillis()
        val list = metricsHistory.getOrPut(cameraId) { mutableListOf() }
        list += TelemetrySample(
            timestampMs = now,
            reconnectAttempts = metrics.reconnectAttempts,
            streamErrorCount = metrics.streamErrorCount
        )
        val minTs = now - 15 * 60 * 1000L
        while (list.isNotEmpty() && list.first().timestampMs < minTs) {
            list.removeAt(0)
        }
    }

    private fun recentDelta(
        cameraId: String,
        windowMs: Long,
        selector: (TelemetrySample) -> Int
    ): Int {
        val samples = metricsHistory[cameraId].orEmpty()
        if (samples.isEmpty()) return 0
        val fromTs = System.currentTimeMillis() - windowMs
        val inWindow = samples.filter { it.timestampMs >= fromTs }
        if (inWindow.size < 2) return 0
        val first = selector(inWindow.first())
        val last = selector(inWindow.last())
        return (last - first).coerceAtLeast(0)
    }

    private fun trimByGridLayout(selected: List<String>, layout: GridLayout): List<String> {
        val max = when (layout) {
            GridLayout.SINGLE -> 1
            GridLayout.GRID_4 -> 4
            GridLayout.GRID_9 -> 9
            GridLayout.GRID_16 -> 16
        }
        return selected.take(max)
    }

    private fun persistLiveState() {
        prefs.put(keyGridLayout, _state.value.gridLayout.name)
        prefs.put(keyFocusedCameraId, _state.value.focusedCameraId ?: "")
        prefs.put(keySelectedCameras, _state.value.selectedCameras.joinToString(","))
        prefs.putLong(keyStateSavedAtMs, System.currentTimeMillis())
    }

    private fun readPersistedGridLayout(): GridLayout {
        val value = prefs.get(keyGridLayout, GridLayout.SINGLE.name)
        return runCatching { GridLayout.valueOf(value) }.getOrDefault(GridLayout.SINGLE)
    }

    private fun readPersistedFocusedCameraId(): String? {
        return prefs.get(keyFocusedCameraId, "").takeIf { it.isNotBlank() }
    }

    private fun readPersistedSelectedCameras(): List<String> {
        val savedAt = prefs.getLong(keyStateSavedAtMs, 0L)
        if (savedAt <= 0L) return emptyList()
        if (System.currentTimeMillis() - savedAt > restoreMaxAgeMs) return emptyList()
        return prefs.get(keySelectedCameras, "")
            .split(',')
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
    }

    private fun readPersistedVisibilityGraceMode(): VisibilityGraceMode {
        val stored = prefs.get(keyVisibilityGraceMode, "")
        if (stored.isNotBlank()) {
            return runCatching { VisibilityGraceMode.valueOf(stored) }
                .getOrDefault(VisibilityGraceMode.AUTO)
        }
        val recommended = recommendedVisibilityGraceForLayout(readPersistedGridLayout())
        return if (visibilityGraceMs == recommended) VisibilityGraceMode.AUTO else VisibilityGraceMode.CUSTOM
    }

    private fun persistTelemetryThresholds() {
        prefs.putInt(keyReconnects5mWarn, telemetryThresholds.reconnects5mWarn)
        prefs.putInt(keyReconnects15mWarn, telemetryThresholds.reconnects15mWarn)
        prefs.putInt(keyErrors5mWarn, telemetryThresholds.errors5mWarn)
        prefs.putInt(keyErrors15mWarn, telemetryThresholds.errors15mWarn)
        prefs.putLong(keySlowStartupMsWarn, telemetryThresholds.slowStartupMsWarn)
        prefs.putLong(keyWarningCooldownMs, telemetryThresholds.warningCooldownMs)
    }
}

