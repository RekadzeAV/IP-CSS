package com.company.ipcamera.desktop.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.company.ipcamera.desktop.ui.common.UiState

/**
 * Универсальный компонент для отображения состояний UI
 *
 * Автоматически обрабатывает Loading, Success, Error и Empty состояния
 */
@Composable
fun <T> StateContent(
    state: UiState<T>,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    emptyMessage: String = "Нет данных",
    loadingMessage: String = "Загрузка...",
    content: @Composable (T) -> Unit
) {
    when (state) {
        is UiState.Loading -> {
            LoadingIndicator(
                message = loadingMessage,
                modifier = modifier
            )
        }
        is UiState.Success -> {
            content(state.data)
        }
        is UiState.Error -> {
            ErrorView(
                message = state.message,
                onRetry = onRetry,
                modifier = modifier
            )
        }
        is UiState.Empty -> {
            EmptyView(
                message = state.message.ifEmpty { emptyMessage },
                modifier = modifier
            )
        }
    }
}
