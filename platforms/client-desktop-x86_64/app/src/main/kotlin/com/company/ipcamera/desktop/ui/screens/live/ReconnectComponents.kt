package com.company.ipcamera.desktop.ui.screens.live

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.company.ipcamera.desktop.ui.viewmodel.CameraReconnectStatus

val WARNING_COLOR = Color(0xFFFF9800) // Amber warning color
val WARNING_CONTAINER_COLOR = Color(0xFFFFCC80) // Light amber for container

@Composable
fun ReconnectStatusIndicator(
    status: CameraReconnectStatus,
    modifier: Modifier = Modifier
) {
    if (status is CameraReconnectStatus.Stable) {
        return // Не показываем индикатор для стабильных камер
    }

    val (text, color, backgroundColor) = when (status) {
        is CameraReconnectStatus.Exhausted -> Triple(
            "⚠️ Exhausted",
            MaterialTheme.colorScheme.onErrorContainer,
            MaterialTheme.colorScheme.errorContainer
        )
        is CameraReconnectStatus.Reconnecting -> {
            val retryText = status.nextRetryInMs?.let { " in ${it / 1000}s" } ?: ""
            Triple(
                "🔄 ${status.attempt}/${status.maxAttempts}$retryText",
                MaterialTheme.colorScheme.onErrorContainer,
                WARNING_CONTAINER_COLOR
            )
        }
        is CameraReconnectStatus.Unstable -> Triple(
            "⚡ ${status.errorCount} errors",
            MaterialTheme.colorScheme.onErrorContainer,
            MaterialTheme.colorScheme.tertiaryContainer
        )
        CameraReconnectStatus.Stable -> Triple(
            "",
            MaterialTheme.colorScheme.onSurface,
            MaterialTheme.colorScheme.surface
        )
    }

    if (text.isEmpty()) return

    Surface(
        modifier = modifier.padding(8.dp),
        color = backgroundColor,
        shape = RoundedCornerShape(4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            textAlign = TextAlign.Center
        )
    }
}
