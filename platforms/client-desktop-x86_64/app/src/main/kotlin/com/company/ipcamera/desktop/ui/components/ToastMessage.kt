package com.company.ipcamera.desktop.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Тип сообщения для Toast
 */
enum class ToastType(val icon: ImageVector) {
    SUCCESS(Icons.Default.CheckCircle),
    ERROR(Icons.Default.Error),
    INFO(Icons.Default.Info),
    WARNING(Icons.Default.Warning)
}

/**
 * Получить цвет контейнера для типа Toast
 */
@Composable
fun ToastType.getContainerColor(): Color {
    return when (this) {
        ToastType.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
        ToastType.ERROR -> MaterialTheme.colorScheme.errorContainer
        ToastType.INFO -> MaterialTheme.colorScheme.secondaryContainer
        ToastType.WARNING -> MaterialTheme.colorScheme.tertiaryContainer
    }
}

/**
 * Состояние Toast сообщения
 */
data class ToastState(
    val message: String,
    val type: ToastType = ToastType.INFO,
    val duration: Long = 3000L
)

/**
 * Компонент для отображения Toast уведомлений
 */
@Composable
fun ToastMessage(
    state: ToastState?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state != null,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeIn(animationSpec = tween(300)),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(animationSpec = tween(300)),
        modifier = modifier
    ) {
        state?.let { toastState ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = toastState.type.getContainerColor()
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = toastState.type.icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = toastState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Хук для управления Toast сообщениями
 */
@Composable
fun rememberToastState(): ToastStateHolder {
    val state = remember { mutableStateOf<ToastState?>(null) }

    LaunchedEffect(state.value) {
        state.value?.let { toastState ->
            delay(toastState.duration)
            state.value = null
        }
    }

    return remember {
        object : ToastStateHolder {
            override fun show(message: String, type: ToastType, duration: Long) {
                state.value = ToastState(message, type, duration)
            }

            override fun showSuccess(message: String) {
                show(message, ToastType.SUCCESS)
            }

            override fun showError(message: String) {
                show(message, ToastType.ERROR, 5000L)
            }

            override fun showInfo(message: String) {
                show(message, ToastType.INFO)
            }

            override fun showWarning(message: String) {
                show(message, ToastType.WARNING)
            }

            override fun dismiss() {
                state.value = null
            }

            override val currentState: ToastState?
                get() = state.value
        }
    }
}

/**
 * Интерфейс для управления Toast состоянием
 */
interface ToastStateHolder {
    fun show(message: String, type: ToastType, duration: Long = 3000L)
    fun showSuccess(message: String)
    fun showError(message: String)
    fun showInfo(message: String)
    fun showWarning(message: String)
    fun dismiss()
    val currentState: ToastState?
}
