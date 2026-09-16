package com.company.ipcamera.desktop.ui.screens.live

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.rememberScrollState
import com.company.ipcamera.desktop.ui.components.ErrorView
import com.company.ipcamera.desktop.ui.components.LoadingIndicator
import com.company.ipcamera.desktop.ui.components.PTZControls
import com.company.ipcamera.desktop.ui.components.VideoPlayer
import com.company.ipcamera.desktop.ui.components.VideoPlaybackMetrics
import com.company.ipcamera.desktop.ui.viewmodel.CameraPlaybackMetrics
import com.company.ipcamera.desktop.ui.viewmodel.GridLayout
import com.company.ipcamera.desktop.ui.viewmodel.LiveViewViewModel
import com.company.ipcamera.desktop.ui.viewmodel.StreamPriority
import com.company.ipcamera.desktop.ui.viewmodel.TelemetryThresholds
import com.company.ipcamera.desktop.ui.viewmodel.TelemetryWarning
import com.company.ipcamera.desktop.ui.viewmodel.VisibilityGraceMode
import com.company.ipcamera.desktop.ui.viewmodel.CameraReconnectStatus
import com.company.ipcamera.desktop.ui.screens.live.ReconnectStatusIndicator
import com.company.ipcamera.shared.domain.model.ObservationCompliance
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import java.util.prefs.Preferences

private enum class TelemetryThresholdPreset {
    Conservative,
    Balanced,
    Sensitive,
    Custom
}

private enum class VisibilityGracePreset(val graceMs: Long) {
    Fast(500L),
    Balanced(1_500L),
    Stable(3_000L),
    Custom(-1L)
}

private fun TelemetryThresholdPreset.toThresholds(): TelemetryThresholds {
    return when (this) {
        TelemetryThresholdPreset.Conservative -> TelemetryThresholds(
            reconnects5mWarn = 10,
            reconnects15mWarn = 24,
            errors5mWarn = 8,
            errors15mWarn = 20,
            slowStartupMsWarn = 10_000L,
            warningCooldownMs = 90_000L
        )
        TelemetryThresholdPreset.Balanced -> TelemetryThresholds(
            reconnects5mWarn = 6,
            reconnects15mWarn = 16,
            errors5mWarn = 4,
            errors15mWarn = 12,
            slowStartupMsWarn = 7_000L,
            warningCooldownMs = 60_000L
        )
        TelemetryThresholdPreset.Sensitive -> TelemetryThresholds(
            reconnects5mWarn = 3,
            reconnects15mWarn = 8,
            errors5mWarn = 2,
            errors15mWarn = 6,
            slowStartupMsWarn = 4_000L,
            warningCooldownMs = 30_000L
        )
        TelemetryThresholdPreset.Custom -> TelemetryThresholds()
    }
}

private fun detectTelemetryThresholdPreset(thresholds: TelemetryThresholds): TelemetryThresholdPreset {
    return when (thresholds) {
        TelemetryThresholdPreset.Conservative.toThresholds() -> TelemetryThresholdPreset.Conservative
        TelemetryThresholdPreset.Balanced.toThresholds() -> TelemetryThresholdPreset.Balanced
        TelemetryThresholdPreset.Sensitive.toThresholds() -> TelemetryThresholdPreset.Sensitive
        else -> TelemetryThresholdPreset.Custom
    }
}

private fun detectVisibilityGracePreset(graceMs: Long): VisibilityGracePreset {
    return when (graceMs) {
        VisibilityGracePreset.Fast.graceMs -> VisibilityGracePreset.Fast
        VisibilityGracePreset.Balanced.graceMs -> VisibilityGracePreset.Balanced
        VisibilityGracePreset.Stable.graceMs -> VisibilityGracePreset.Stable
        else -> VisibilityGracePreset.Custom
    }
}

@Composable
fun LiveViewScreen(
    viewModel: LiveViewViewModel = koinInject()
) {
    val state by viewModel.state.collectAsState()
    var showCameraSelector by remember { mutableStateOf(false) }
    var latestVisibleCameraIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    var restoreHoursInput by remember(state.restoreMaxAgeHours) { mutableStateOf(state.restoreMaxAgeHours.toString()) }
    var reconnects5mInput by remember(state.telemetryThresholds.reconnects5mWarn) { mutableStateOf(state.telemetryThresholds.reconnects5mWarn.toString()) }
    var reconnects15mInput by remember(state.telemetryThresholds.reconnects15mWarn) { mutableStateOf(state.telemetryThresholds.reconnects15mWarn.toString()) }
    var errors5mInput by remember(state.telemetryThresholds.errors5mWarn) { mutableStateOf(state.telemetryThresholds.errors5mWarn.toString()) }
    var errors15mInput by remember(state.telemetryThresholds.errors15mWarn) { mutableStateOf(state.telemetryThresholds.errors15mWarn.toString()) }
    var startupMsInput by remember(state.telemetryThresholds.slowStartupMsWarn) { mutableStateOf(state.telemetryThresholds.slowStartupMsWarn.toString()) }
    var cooldownMsInput by remember(state.telemetryThresholds.warningCooldownMs) { mutableStateOf(state.telemetryThresholds.warningCooldownMs.toString()) }
    var visibilityGraceInput by remember(state.visibilityGraceMs) { mutableStateOf(state.visibilityGraceMs.toString()) }
    val hasUnsavedThresholdChanges = remember(
        reconnects5mInput,
        reconnects15mInput,
        errors5mInput,
        errors15mInput,
        startupMsInput,
        cooldownMsInput,
        state.telemetryThresholds
    ) {
        reconnects5mInput != state.telemetryThresholds.reconnects5mWarn.toString() ||
            reconnects15mInput != state.telemetryThresholds.reconnects15mWarn.toString() ||
            errors5mInput != state.telemetryThresholds.errors5mWarn.toString() ||
            errors15mInput != state.telemetryThresholds.errors15mWarn.toString() ||
            startupMsInput != state.telemetryThresholds.slowStartupMsWarn.toString() ||
            cooldownMsInput != state.telemetryThresholds.warningCooldownMs.toString()
    }
    val visibilityGraceValid = remember(visibilityGraceInput) {
        visibilityGraceInput.toLongOrNull()?.let { it in 0L..10_000L } == true
    }
    val hasUnsavedVisibilityGraceChanges = remember(visibilityGraceInput, state.visibilityGraceMs) {
        visibilityGraceInput != state.visibilityGraceMs.toString()
    }
    val activePreset = remember(state.telemetryThresholds) {
        detectTelemetryThresholdPreset(state.telemetryThresholds)
    }
    val activeVisibilityPreset = remember(state.visibilityGraceMs) {
        detectVisibilityGracePreset(state.visibilityGraceMs)
    }
    val selectedCameras by remember(state.cameras, state.selectedCameras) {
        // derivedStateOf уменьшает лишние пересчеты выборки камер при частых обновлениях телеметрии.
        derivedStateOf {
            val selectedIds = state.selectedCameras.toSet()
            state.cameras.filter { it.id in selectedIds }
        }
    }
    val lazyPausedTilesCount by remember(
        state.gridLayout,
        selectedCameras,
        state.focusedCameraId,
        latestVisibleCameraIds
    ) {
        derivedStateOf {
            when (state.gridLayout) {
                GridLayout.SINGLE,
                GridLayout.GRID_4 -> 0
                GridLayout.GRID_9,
                GridLayout.GRID_16 -> {
                    if (latestVisibleCameraIds.isEmpty()) {
                        0
                    } else {
                        selectedCameras.count { camera ->
                            camera.id !in latestVisibleCameraIds && camera.id != state.focusedCameraId
                        }
                    }
                }
            }
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
        // Панель управления
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Прямой эфир",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.weight(1f)
            )

            // Выбор layout
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(
                    onClick = { viewModel.setGridLayout(GridLayout.SINGLE) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (state.gridLayout == GridLayout.SINGLE) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Icon(Icons.Default.CropFree, contentDescription = "1 камера")
                }
                IconButton(
                    onClick = { viewModel.setGridLayout(GridLayout.GRID_4) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (state.gridLayout == GridLayout.GRID_4) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Icon(Icons.Default.Grid4x4, contentDescription = "4 камеры")
                }
                IconButton(
                    onClick = { viewModel.setGridLayout(GridLayout.GRID_9) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (state.gridLayout == GridLayout.GRID_9) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Icon(Icons.Default.GridOn, contentDescription = "9 камер")
                }
                IconButton(
                    onClick = { viewModel.setGridLayout(GridLayout.GRID_16) },
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = if (state.gridLayout == GridLayout.GRID_16) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    )
                ) {
                    Icon(Icons.Default.ViewModule, contentDescription = "16 камер")
                }
            }

            // Кнопка выбора камер
            Button(onClick = { showCameraSelector = true }) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Выбрать камеры")
            }
        }

        HorizontalDivider()

        LiveTelemetryHeader(
            activeStreams = state.telemetrySummary.activeStreams,
            streamsWithStartup = state.telemetrySummary.streamsWithStartup,
            avgStartupMs = state.telemetrySummary.avgStartupMs,
            avgRenderFps = state.telemetrySummary.avgRenderFps,
            totalDroppedFrames = state.telemetrySummary.totalDroppedFrames,
            lazyPausedTiles = lazyPausedTilesCount,
            totalReconnects = state.telemetrySummary.totalReconnects,
            totalErrors = state.telemetrySummary.totalErrors,
            reconnectsLast5m = state.telemetrySummary.reconnectsLast5m,
            errorsLast5m = state.telemetrySummary.errorsLast5m,
            reconnectsLast15m = state.telemetrySummary.reconnectsLast15m,
            errorsLast15m = state.telemetrySummary.errorsLast15m,
            warning = state.telemetrySummary.warning,
            onResetTelemetry = { viewModel.resetTelemetry() }
        )

        LiveRestoreConfigBar(
            safeRestoreEnabled = state.safeRestoreEnabled,
            restoreMaxAgeHoursInput = restoreHoursInput,
            thresholds = state.telemetryThresholds,
            reconnects5mInput = reconnects5mInput,
            reconnects15mInput = reconnects15mInput,
            errors5mInput = errors5mInput,
            errors15mInput = errors15mInput,
            startupMsInput = startupMsInput,
            cooldownMsInput = cooldownMsInput,
            onRestoreHoursInputChange = { value ->
                if (value.isEmpty() || value.all { it.isDigit() }) restoreHoursInput = value
            },
            onReconnects5mInputChange = { value -> if (value.isEmpty() || value.all { it.isDigit() }) reconnects5mInput = value },
            onReconnects15mInputChange = { value -> if (value.isEmpty() || value.all { it.isDigit() }) reconnects15mInput = value },
            onErrors5mInputChange = { value -> if (value.isEmpty() || value.all { it.isDigit() }) errors5mInput = value },
            onErrors15mInputChange = { value -> if (value.isEmpty() || value.all { it.isDigit() }) errors15mInput = value },
            onStartupMsInputChange = { value -> if (value.isEmpty() || value.all { it.isDigit() }) startupMsInput = value },
            onCooldownMsInputChange = { value -> if (value.isEmpty() || value.all { it.isDigit() }) cooldownMsInput = value },
            onSafeRestoreToggle = { enabled -> viewModel.setSafeRestoreEnabled(enabled) },
            onApplyRestoreHours = {
                restoreHoursInput.toIntOrNull()?.let { viewModel.setRestoreMaxAgeHours(it) }
            },
            onApplyThresholds = {
                val r5 = reconnects5mInput.toIntOrNull() ?: state.telemetryThresholds.reconnects5mWarn
                val r15 = reconnects15mInput.toIntOrNull() ?: state.telemetryThresholds.reconnects15mWarn
                val e5 = errors5mInput.toIntOrNull() ?: state.telemetryThresholds.errors5mWarn
                val e15 = errors15mInput.toIntOrNull() ?: state.telemetryThresholds.errors15mWarn
                val startup = startupMsInput.toLongOrNull() ?: state.telemetryThresholds.slowStartupMsWarn
                val cooldown = cooldownMsInput.toLongOrNull() ?: state.telemetryThresholds.warningCooldownMs
                viewModel.updateTelemetryThresholds(r5, r15, e5, e15, startup, cooldown)
                scope.launch { snackbarHostState.showSnackbar("Пороги применены") }
            },
            onRevertThresholdInputs = {
                reconnects5mInput = state.telemetryThresholds.reconnects5mWarn.toString()
                reconnects15mInput = state.telemetryThresholds.reconnects15mWarn.toString()
                errors5mInput = state.telemetryThresholds.errors5mWarn.toString()
                errors15mInput = state.telemetryThresholds.errors15mWarn.toString()
                startupMsInput = state.telemetryThresholds.slowStartupMsWarn.toString()
                cooldownMsInput = state.telemetryThresholds.warningCooldownMs.toString()
                scope.launch { snackbarHostState.showSnackbar("Локальные изменения отменены") }
            },
            hasUnsavedThresholdChanges = hasUnsavedThresholdChanges,
            activePreset = activePreset,
            visibilityGraceMs = state.visibilityGraceMs,
            visibilityGraceMode = state.visibilityGraceMode,
            visibilityGraceInput = visibilityGraceInput,
            visibilityGraceValid = visibilityGraceValid,
            hasUnsavedVisibilityGraceChanges = hasUnsavedVisibilityGraceChanges,
            activeVisibilityPreset = activeVisibilityPreset,
            onApplyPreset = { preset ->
                val presetThresholds = preset.toThresholds()
                viewModel.updateTelemetryThresholds(
                    reconnects5mWarn = presetThresholds.reconnects5mWarn,
                    reconnects15mWarn = presetThresholds.reconnects15mWarn,
                    errors5mWarn = presetThresholds.errors5mWarn,
                    errors15mWarn = presetThresholds.errors15mWarn,
                    slowStartupMsWarn = presetThresholds.slowStartupMsWarn,
                    warningCooldownMs = presetThresholds.warningCooldownMs
                )
                reconnects5mInput = presetThresholds.reconnects5mWarn.toString()
                reconnects15mInput = presetThresholds.reconnects15mWarn.toString()
                errors5mInput = presetThresholds.errors5mWarn.toString()
                errors15mInput = presetThresholds.errors15mWarn.toString()
                startupMsInput = presetThresholds.slowStartupMsWarn.toString()
                cooldownMsInput = presetThresholds.warningCooldownMs.toString()
                scope.launch { snackbarHostState.showSnackbar("Применен пресет: ${preset.name}") }
            },
            onApplyVisibilityPreset = { preset ->
                if (preset != VisibilityGracePreset.Custom) {
                    viewModel.setVisibilityGraceMs(preset.graceMs)
                    visibilityGraceInput = preset.graceMs.toString()
                    scope.launch { snackbarHostState.showSnackbar("Visibility grace: ${preset.graceMs} ms") }
                }
            },
            onVisibilityGraceInputChange = { value ->
                if (value.isEmpty() || value.all { it.isDigit() }) visibilityGraceInput = value
            },
            onApplyVisibilityGraceInput = {
                val value = visibilityGraceInput.toLongOrNull()
                if (value != null && value in 0L..10_000L) {
                    viewModel.setVisibilityGraceMs(value)
                    scope.launch { snackbarHostState.showSnackbar("Visibility grace применен: ${value} ms") }
                }
            },
            onRevertVisibilityGraceInput = {
                visibilityGraceInput = state.visibilityGraceMs.toString()
                scope.launch { snackbarHostState.showSnackbar("Visibility grace изменения отменены") }
            },
            onSetVisibilityGraceAuto = {
                viewModel.setVisibilityGraceAuto()
                visibilityGraceInput = viewModel.state.value.visibilityGraceMs.toString()
                scope.launch { snackbarHostState.showSnackbar("Visibility grace: Auto") }
            },
            onResetThresholds = {
                viewModel.resetTelemetryThresholdsToDefaults()
                scope.launch { snackbarHostState.showSnackbar("Пороги сброшены к значениям по умолчанию") }
            }
        )

        if (state.pendingRestoreCameraIds.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                color = MaterialTheme.colorScheme.secondaryContainer,
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Доступно для восстановления: ${state.pendingRestoreCameraIds.size} камер",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    TextButton(onClick = { viewModel.restoreAllPendingCameras() }) {
                        Text("Восстановить все")
                    }
                }
            }
        }

        // Контент
        when {
            state.isLoading -> {
                LoadingIndicator(message = "Загрузка камер...")
            }
            state.error != null -> {
                ErrorView(
                    message = state.error!!,
                    onRetry = { viewModel.loadCameras() }
                )
            }
            else -> {
                if (selectedCameras.isEmpty()) {
                    EmptyLiveViewState(
                        onSelectCameras = { showCameraSelector = true }
                    )
                } else {
                    VideoGrid(
                        cameras = selectedCameras,
                        gridLayout = state.gridLayout,
                        focusedCameraId = state.focusedCameraId,
                        streamPriorityProvider = { cameraId -> viewModel.getStreamPriority(cameraId) },
                        reconnectStatusProvider = { cameraId -> viewModel.getReconnectStatus(cameraId) },
                        onFocusCamera = { cameraId -> viewModel.setFocusedCamera(cameraId) },
                        onMetricsUpdate = { cameraId, metrics ->
                            viewModel.updateCameraMetrics(cameraId, metrics.toViewModelMetrics())
                        },
                        onRemoveCamera = { cameraId ->
                            viewModel.removeCamera(cameraId)
                        },
                        onManualReconnect = { cameraId -> viewModel.requestManualReconnect(cameraId) },
                        onVisibleCamerasChanged = { visibleIds ->
                            latestVisibleCameraIds = visibleIds
                            viewModel.updateVisibleCameraIds(visibleIds)
                        }
                    )
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .padding(16.dp)
        )
    }

    // Диалог выбора камер
    if (showCameraSelector) {
        CameraSelectorDialog(
            availableCameras = state.cameras,
            selectedCameras = state.selectedCameras,
            gridLayout = state.gridLayout,
            onDismiss = { showCameraSelector = false },
            onCameraSelected = { cameraId ->
                viewModel.selectCamera(cameraId)
            },
            onCameraDeselected = { cameraId ->
                viewModel.removeCamera(cameraId)
            }
        )
    }
}
}

@Composable
private fun LiveRestoreConfigBar(
    safeRestoreEnabled: Boolean,
    restoreMaxAgeHoursInput: String,
    thresholds: TelemetryThresholds,
    reconnects5mInput: String,
    reconnects15mInput: String,
    errors5mInput: String,
    errors15mInput: String,
    startupMsInput: String,
    cooldownMsInput: String,
    onRestoreHoursInputChange: (String) -> Unit,
    onReconnects5mInputChange: (String) -> Unit,
    onReconnects15mInputChange: (String) -> Unit,
    onErrors5mInputChange: (String) -> Unit,
    onErrors15mInputChange: (String) -> Unit,
    onStartupMsInputChange: (String) -> Unit,
    onCooldownMsInputChange: (String) -> Unit,
    onSafeRestoreToggle: (Boolean) -> Unit,
    onApplyRestoreHours: () -> Unit,
    onApplyThresholds: () -> Unit,
    onRevertThresholdInputs: () -> Unit,
    hasUnsavedThresholdChanges: Boolean,
    activePreset: TelemetryThresholdPreset,
    visibilityGraceMs: Long,
    visibilityGraceMode: VisibilityGraceMode,
    visibilityGraceInput: String,
    visibilityGraceValid: Boolean,
    hasUnsavedVisibilityGraceChanges: Boolean,
    activeVisibilityPreset: VisibilityGracePreset,
    onApplyPreset: (TelemetryThresholdPreset) -> Unit,
    onApplyVisibilityPreset: (VisibilityGracePreset) -> Unit,
    onVisibilityGraceInputChange: (String) -> Unit,
    onApplyVisibilityGraceInput: () -> Unit,
    onRevertVisibilityGraceInput: () -> Unit,
    onSetVisibilityGraceAuto: () -> Unit,
    onResetThresholds: () -> Unit
) {
    val r5Valid = reconnects5mInput.toIntOrNull()?.let { it in 1..1000 } == true
    val r15Valid = reconnects15mInput.toIntOrNull()?.let { it in 1..5000 } == true
    val e5Valid = errors5mInput.toIntOrNull()?.let { it in 1..1000 } == true
    val e15Valid = errors15mInput.toIntOrNull()?.let { it in 1..5000 } == true
    val startupValid = startupMsInput.toLongOrNull()?.let { it in 500L..120_000L } == true
    val cooldownValid = cooldownMsInput.toLongOrNull()?.let { it in 1_000L..3_600_000L } == true
    val thresholdsInputValid = r5Valid && r15Valid && e5Valid && e15Valid && startupValid && cooldownValid

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Safe restore",
                    style = MaterialTheme.typography.labelMedium
                )
                Switch(
                    checked = safeRestoreEnabled,
                    onCheckedChange = onSafeRestoreToggle
                )
                OutlinedTextField(
                    value = restoreMaxAgeHoursInput,
                    onValueChange = onRestoreHoursInputChange,
                    label = { Text("TTL (часы)") },
                    modifier = Modifier.width(140.dp),
                    singleLine = true
                )
                TextButton(onClick = onApplyRestoreHours) {
                    Text("Применить TTL")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TelemetryThresholdEditor(
                    reconnects5mInput = reconnects5mInput,
                    reconnects15mInput = reconnects15mInput,
                    errors5mInput = errors5mInput,
                    errors15mInput = errors15mInput,
                    startupMsInput = startupMsInput,
                    cooldownMsInput = cooldownMsInput,
                    r5Valid = r5Valid,
                    r15Valid = r15Valid,
                    e5Valid = e5Valid,
                    e15Valid = e15Valid,
                    startupValid = startupValid,
                    cooldownValid = cooldownValid,
                    thresholdsInputValid = thresholdsInputValid,
                    onRevertThresholdInputs = onRevertThresholdInputs,
                    hasUnsavedThresholdChanges = hasUnsavedThresholdChanges,
                    activePreset = activePreset,
                    onReconnects5mInputChange = onReconnects5mInputChange,
                    onReconnects15mInputChange = onReconnects15mInputChange,
                    onErrors5mInputChange = onErrors5mInputChange,
                    onErrors15mInputChange = onErrors15mInputChange,
                    onStartupMsInputChange = onStartupMsInputChange,
                    onCooldownMsInputChange = onCooldownMsInputChange,
                    onApplyThresholds = onApplyThresholds,
                    onApplyPreset = onApplyPreset,
                    onResetThresholds = onResetThresholds
                )
            }
            StreamPerformanceConfigRow(
                visibilityGraceMs = visibilityGraceMs,
                visibilityGraceMode = visibilityGraceMode,
                visibilityGraceInput = visibilityGraceInput,
                visibilityGraceValid = visibilityGraceValid,
                hasUnsavedVisibilityGraceChanges = hasUnsavedVisibilityGraceChanges,
                activeVisibilityPreset = activeVisibilityPreset,
                onApplyVisibilityPreset = onApplyVisibilityPreset,
                onVisibilityGraceInputChange = onVisibilityGraceInputChange,
                onApplyVisibilityGraceInput = onApplyVisibilityGraceInput,
                onRevertVisibilityGraceInput = onRevertVisibilityGraceInput,
                onSetVisibilityGraceAuto = onSetVisibilityGraceAuto
            )
            Text(
                text = "Диапазоны: R5/E5=1..1000, R15/E15=1..5000, Startup=500..120000 ms, Cooldown=1000..3600000 ms, VisibilityGrace=0..10000 ms",
                style = MaterialTheme.typography.labelSmall
            )
            if (!thresholdsInputValid) {
                Text(
                    text = "Проверьте значения порогов: есть пустые или вне диапазона.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (hasUnsavedThresholdChanges) {
                Text(
                    text = "Есть несохраненные пороги. Нажмите \"Применить пороги\".",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
        }
        Text(
            text = "Thresholds: R5=${thresholds.reconnects5mWarn}, R15=${thresholds.reconnects15mWarn}, E5=${thresholds.errors5mWarn}, E15=${thresholds.errors15mWarn}, Startup=${thresholds.slowStartupMsWarn}ms, Cooldown=${thresholds.warningCooldownMs}ms, VisibilityGrace=${visibilityGraceMs}ms",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun TelemetryThresholdEditor(
    reconnects5mInput: String,
    reconnects15mInput: String,
    errors5mInput: String,
    errors15mInput: String,
    startupMsInput: String,
    cooldownMsInput: String,
    r5Valid: Boolean,
    r15Valid: Boolean,
    e5Valid: Boolean,
    e15Valid: Boolean,
    startupValid: Boolean,
    cooldownValid: Boolean,
    thresholdsInputValid: Boolean,
    hasUnsavedThresholdChanges: Boolean,
    activePreset: TelemetryThresholdPreset,
    onReconnects5mInputChange: (String) -> Unit,
    onReconnects15mInputChange: (String) -> Unit,
    onErrors5mInputChange: (String) -> Unit,
    onErrors15mInputChange: (String) -> Unit,
    onStartupMsInputChange: (String) -> Unit,
    onCooldownMsInputChange: (String) -> Unit,
    onApplyThresholds: () -> Unit,
    onRevertThresholdInputs: () -> Unit,
    onApplyPreset: (TelemetryThresholdPreset) -> Unit,
    onResetThresholds: () -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = reconnects5mInput,
            onValueChange = onReconnects5mInputChange,
            label = { Text("R5") },
            modifier = Modifier.width(84.dp),
            singleLine = true,
            isError = reconnects5mInput.isNotEmpty() && !r5Valid
        )
        OutlinedTextField(
            value = reconnects15mInput,
            onValueChange = onReconnects15mInputChange,
            label = { Text("R15") },
            modifier = Modifier.width(84.dp),
            singleLine = true,
            isError = reconnects15mInput.isNotEmpty() && !r15Valid
        )
        OutlinedTextField(
            value = errors5mInput,
            onValueChange = onErrors5mInputChange,
            label = { Text("E5") },
            modifier = Modifier.width(84.dp),
            singleLine = true,
            isError = errors5mInput.isNotEmpty() && !e5Valid
        )
        OutlinedTextField(
            value = errors15mInput,
            onValueChange = onErrors15mInputChange,
            label = { Text("E15") },
            modifier = Modifier.width(84.dp),
            singleLine = true,
            isError = errors15mInput.isNotEmpty() && !e15Valid
        )
        OutlinedTextField(
            value = startupMsInput,
            onValueChange = onStartupMsInputChange,
            label = { Text("Startup ms") },
            modifier = Modifier.width(120.dp),
            singleLine = true,
            isError = startupMsInput.isNotEmpty() && !startupValid
        )
        OutlinedTextField(
            value = cooldownMsInput,
            onValueChange = onCooldownMsInputChange,
            label = { Text("Cooldown ms") },
            modifier = Modifier.width(120.dp),
            singleLine = true,
            isError = cooldownMsInput.isNotEmpty() && !cooldownValid
        )
        FilterChip(
            selected = activePreset == TelemetryThresholdPreset.Conservative,
            onClick = { onApplyPreset(TelemetryThresholdPreset.Conservative) },
            label = { Text("Conservative") }
        )
        FilterChip(
            selected = activePreset == TelemetryThresholdPreset.Balanced,
            onClick = { onApplyPreset(TelemetryThresholdPreset.Balanced) },
            label = { Text("Balanced") }
        )
        FilterChip(
            selected = activePreset == TelemetryThresholdPreset.Sensitive,
            onClick = { onApplyPreset(TelemetryThresholdPreset.Sensitive) },
            label = { Text("Sensitive") }
        )
        FilterChip(
            selected = activePreset == TelemetryThresholdPreset.Custom,
            onClick = {},
            enabled = false,
            label = { Text("Custom") }
        )
        TextButton(
            onClick = onApplyThresholds,
            enabled = thresholdsInputValid && hasUnsavedThresholdChanges
        ) { Text("Применить пороги") }
        TextButton(
            onClick = onRevertThresholdInputs,
            enabled = hasUnsavedThresholdChanges
        ) { Text("Вернуть текущие") }
        TextButton(onClick = onResetThresholds) { Text("Сбросить") }
    }
}

@Composable
private fun StreamPerformanceConfigRow(
    visibilityGraceMs: Long,
    visibilityGraceMode: VisibilityGraceMode,
    visibilityGraceInput: String,
    visibilityGraceValid: Boolean,
    hasUnsavedVisibilityGraceChanges: Boolean,
    activeVisibilityPreset: VisibilityGracePreset,
    onApplyVisibilityPreset: (VisibilityGracePreset) -> Unit,
    onVisibilityGraceInputChange: (String) -> Unit,
    onApplyVisibilityGraceInput: () -> Unit,
    onRevertVisibilityGraceInput: () -> Unit,
    onSetVisibilityGraceAuto: () -> Unit
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier.horizontalScroll(scrollState),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Stream performance",
            style = MaterialTheme.typography.labelMedium
        )
        AssistChip(
            onClick = {},
            enabled = false,
            label = {
                Text(
                    text = if (visibilityGraceMode == VisibilityGraceMode.AUTO) "Mode: Auto" else "Mode: Custom"
                )
            },
            colors = AssistChipDefaults.assistChipColors(
                containerColor = if (visibilityGraceMode == VisibilityGraceMode.AUTO) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.tertiaryContainer
                },
                labelColor = if (visibilityGraceMode == VisibilityGraceMode.AUTO) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onTertiaryContainer
                }
            )
        )
        FilterChip(
            selected = activeVisibilityPreset == VisibilityGracePreset.Fast,
            onClick = { onApplyVisibilityPreset(VisibilityGracePreset.Fast) },
            label = { Text("Fast") }
        )
        FilterChip(
            selected = activeVisibilityPreset == VisibilityGracePreset.Balanced,
            onClick = { onApplyVisibilityPreset(VisibilityGracePreset.Balanced) },
            label = { Text("Balanced") }
        )
        FilterChip(
            selected = activeVisibilityPreset == VisibilityGracePreset.Stable,
            onClick = { onApplyVisibilityPreset(VisibilityGracePreset.Stable) },
            label = { Text("Stable") }
        )
        FilterChip(
            selected = activeVisibilityPreset == VisibilityGracePreset.Custom,
            onClick = {},
            enabled = false,
            label = { Text("Custom") }
        )
        FilterChip(
            selected = visibilityGraceMode == VisibilityGraceMode.AUTO,
            onClick = onSetVisibilityGraceAuto,
            label = { Text("Auto") }
        )
        OutlinedTextField(
            value = visibilityGraceInput,
            onValueChange = onVisibilityGraceInputChange,
            label = { Text("Grace ms") },
            modifier = Modifier.width(120.dp),
            singleLine = true,
            isError = visibilityGraceInput.isNotEmpty() && !visibilityGraceValid
        )
        TextButton(
            onClick = onApplyVisibilityGraceInput,
            enabled = visibilityGraceValid && hasUnsavedVisibilityGraceChanges
        ) { Text("Применить") }
        TextButton(
            onClick = onRevertVisibilityGraceInput,
            enabled = hasUnsavedVisibilityGraceChanges
        ) { Text("Вернуть") }
        Text(
            text = "Рекомендации: Fast=500ms, Balanced=1500ms, Stable=3000ms; текущее=${visibilityGraceMs}ms (${if (visibilityGraceMode == VisibilityGraceMode.AUTO) "Auto" else "Custom"})",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
@OptIn(FlowPreview::class)
private fun VideoGrid(
    cameras: List<com.company.ipcamera.shared.domain.model.Camera>,
    gridLayout: GridLayout,
    focusedCameraId: String?,
    streamPriorityProvider: (String) -> StreamPriority,
    reconnectStatusProvider: (String) -> CameraReconnectStatus,
    onFocusCamera: (String) -> Unit,
    onMetricsUpdate: (cameraId: String, metrics: VideoPlaybackMetrics) -> Unit,
    onRemoveCamera: (String) -> Unit,
    onManualReconnect: (String) -> Unit,
    onVisibleCamerasChanged: (Set<String>) -> Unit
) {
    val gridState = rememberLazyGridState()
    val localFrameAnalyticsEnabled = remember {
        System.getenv("IPCSS_LOCAL_FRAME_ANALYTICS") == "true"
    }
    val columns = when (gridLayout) {
        GridLayout.SINGLE -> 1
        GridLayout.GRID_4 -> 2
        GridLayout.GRID_9 -> 3
        GridLayout.GRID_16 -> 4
    }
    val visibleCameraIds by remember(cameras, gridState) {
        // derivedStateOf ограничивает перерасчеты набора видимых камер изменениями layoutInfo.
        derivedStateOf {
            gridState.layoutInfo.visibleItemsInfo
                .mapNotNull { info -> cameras.getOrNull(info.index)?.id }
                .toSet()
        }
    }

    LaunchedEffect(cameras) {
        snapshotFlow { visibleCameraIds }
            .distinctUntilChanged()
            .debounce(350)
            .collect { visibleIds -> onVisibleCamerasChanged(visibleIds) }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(cameras, key = { it.id }) { camera ->
            val shouldRenderVideoPlayer = when (gridLayout) {
                GridLayout.SINGLE,
                GridLayout.GRID_4 -> true
                GridLayout.GRID_9,
                GridLayout.GRID_16 -> {
                    camera.id in visibleCameraIds || focusedCameraId == camera.id
                }
            }
            Box(
                modifier = Modifier
                    .aspectRatio(16f / 9f)
                    .clickable { onFocusCamera(camera.id) }
                    .border(
                        width = if (focusedCameraId == camera.id) 2.dp else 0.dp,
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .fillMaxWidth()
            ) {
                if (shouldRenderVideoPlayer) {
                    VideoPlayer(
                        camera = camera,
                        streamPriority = streamPriorityProvider(camera.id),
                        onMetricsUpdate = { metrics ->
                            onMetricsUpdate(camera.id, metrics)
                        },
                        onError = { error ->
                            // Обработка ошибки
                        },
                        localFrameAnalyticsEnabled = localFrameAnalyticsEnabled
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = camera.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Вне viewport (lazy render)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                val observationSummary = remember(camera) { ObservationCompliance.summarize(camera) }
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)) {
                        Text(
                            text = "px/m: ${observationSummary.computedPixelsPerMeter?.let { String.format("%.2f", it) } ?: "n/a"}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = "min: ${observationSummary.targetPixelsPerMeterMin?.let { String.format("%.2f", it) } ?: "n/a"}",
                            style = MaterialTheme.typography.labelSmall
                        )
                        Text(
                            text = if (observationSummary.lowResolutionWarning) "Low-res warning" else "Resolution OK",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (observationSummary.lowResolutionWarning) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // PTZ управление для камер с поддержкой PTZ
                if (camera.ptz?.enabled == true) {
                    PTZControls(
                        camera = camera,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(8.dp)
                    )
                }

                // Кнопка закрытия
                IconButton(
                    onClick = { onRemoveCamera(camera.id) },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Закрыть",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveTelemetryHeader(
    activeStreams: Int,
    streamsWithStartup: Int,
    avgStartupMs: Long?,
    avgRenderFps: Int?,
    totalDroppedFrames: Int,
    lazyPausedTiles: Int,
    totalReconnects: Int,
    totalErrors: Int,
    reconnectsLast5m: Int,
    errorsLast5m: Int,
    reconnectsLast15m: Int,
    errorsLast15m: Int,
    warning: TelemetryWarning,
    onResetTelemetry: () -> Unit
) {
    val prefs = remember { Preferences.userRoot().node("com.company.ipcamera.desktop.live.telemetry") }
    var selectedWindow by rememberSaveable {
        mutableStateOf(
            prefs.get("telemetry_window", "5m")
                .takeIf { it == "5m" || it == "15m" }
                ?: "5m"
        )
    }
    LaunchedEffect(selectedWindow) {
        prefs.put("telemetry_window", selectedWindow)
    }
    val windowReconnects = if (selectedWindow == "5m") reconnectsLast5m else reconnectsLast15m
    val windowErrors = if (selectedWindow == "5m") errorsLast5m else errorsLast15m
    val windowLabel = if (selectedWindow == "5m") "5m" else "15m"

    val warningText = when (warning) {
        TelemetryWarning.RECONNECT_STORM -> "Warning: reconnect storm"
        TelemetryWarning.HIGH_ERROR_RATE -> "Warning: high stream error rate"
        TelemetryWarning.SLOW_STARTUP -> "Warning: slow startup latency"
        TelemetryWarning.NONE -> null
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = MaterialTheme.shapes.small
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "Потоки: $activeStreams | Startup: ${avgStartupMs?.let { "${it}ms ($streamsWithStartup)" } ?: "n/a"}",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "Render FPS(avg): ${avgRenderFps ?: 0} | Dropped: $totalDroppedFrames | Lazy paused: $lazyPausedTiles",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "Reconnects: $totalReconnects | Errors: $totalErrors",
                    style = MaterialTheme.typography.labelMedium
                )
                Text(
                    text = "$windowLabel window: R=$windowReconnects E=$windowErrors  (5m: R=$reconnectsLast5m E=$errorsLast5m | 15m: R=$reconnectsLast15m E=$errorsLast15m)",
                    style = MaterialTheme.typography.labelSmall
                )
                if (warningText != null) {
                    Text(
                        text = warningText,
                        style = MaterialTheme.typography.labelSmall,
                        color = when (warning) {
                            TelemetryWarning.RECONNECT_STORM,
                            TelemetryWarning.HIGH_ERROR_RATE -> MaterialTheme.colorScheme.error
                            TelemetryWarning.SLOW_STARTUP -> MaterialTheme.colorScheme.tertiary
                            TelemetryWarning.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                FilterChip(
                    selected = selectedWindow == "5m",
                    onClick = { selectedWindow = "5m" },
                    label = { Text("5m") }
                )
                FilterChip(
                    selected = selectedWindow == "15m",
                    onClick = { selectedWindow = "15m" },
                    label = { Text("15m") }
                )
                TextButton(onClick = onResetTelemetry) {
                    Text("Сброс")
                }
            }
        }
    }
}

private fun VideoPlaybackMetrics.toViewModelMetrics(): CameraPlaybackMetrics {
    return CameraPlaybackMetrics(
        startupTimeMs = startupTimeMs,
        reconnectAttempts = reconnectAttempts,
        streamErrorCount = streamErrorCount,
        lastErrorAtMs = lastErrorAtMs,
        renderFps = renderFps,
        droppedFrames = droppedFrames
    )
}

@Composable
private fun EmptyLiveViewState(
    onSelectCameras: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.VideoLibrary,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Нет выбранных камер",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Выберите камеры для просмотра",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onSelectCameras) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Выбрать камеры")
        }
    }
}

@Composable
private fun CameraSelectorDialog(
    availableCameras: List<com.company.ipcamera.shared.domain.model.Camera>,
    selectedCameras: List<String>,
    gridLayout: GridLayout,
    onDismiss: () -> Unit,
    onCameraSelected: (String) -> Unit,
    onCameraDeselected: (String) -> Unit
) {
    val maxCameras = when (gridLayout) {
        GridLayout.SINGLE -> 1
        GridLayout.GRID_4 -> 4
        GridLayout.GRID_9 -> 9
        GridLayout.GRID_16 -> 16
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Выбор камер (максимум $maxCameras)") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                availableCameras.forEach { camera ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = camera.id in selectedCameras,
                            onCheckedChange = { checked ->
                                if (checked) {
                                    if (selectedCameras.size < maxCameras) {
                                        onCameraSelected(camera.id)
                                    }
                                } else {
                                    onCameraDeselected(camera.id)
                                }
                            },
                            enabled = camera.id in selectedCameras || selectedCameras.size < maxCameras
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = camera.name,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Готово")
            }
        }
    )
}

