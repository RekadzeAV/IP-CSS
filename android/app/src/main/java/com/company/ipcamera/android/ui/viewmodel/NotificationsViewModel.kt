package com.company.ipcamera.android.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.company.ipcamera.shared.domain.model.Notification
import com.company.ipcamera.shared.domain.model.NotificationPriority
import com.company.ipcamera.shared.domain.model.NotificationType
import com.company.ipcamera.shared.domain.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationsUiState(
    val notifications: List<Notification> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val filterType: NotificationType? = null,
    val filterPriority: NotificationPriority? = null,
    val showRead: Boolean = true,
    val unreadCount: Int = 0,
    val currentPage: Int = 1,
    val hasMore: Boolean = true
)

class NotificationsViewModel(
    private val notificationRepository: NotificationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotificationsUiState())
    val uiState: StateFlow<NotificationsUiState> = _uiState.asStateFlow()

    init {
        loadNotifications()
        loadUnreadCount()
    }

    fun loadNotifications(page: Int = 1) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                val state = _uiState.value
                val result = notificationRepository.getNotifications(
                    type = state.filterType,
                    priority = state.filterPriority,
                    read = if (state.showRead) null else false,
                    page = page,
                    limit = 20
                )

                val newNotifications = if (page == 1) {
                    result.items
                } else {
                    state.notifications + result.items
                }

                _uiState.value = state.copy(
                    notifications = newNotifications,
                    isLoading = false,
                    currentPage = page,
                    hasMore = result.hasMore
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load notifications"
                )
            }
        }
    }

    fun loadUnreadCount() {
        viewModelScope.launch {
            try {
                val count = notificationRepository.getUnreadCount()
                _uiState.value = _uiState.value.copy(unreadCount = count)
            } catch (e: Exception) {
                // Игнорируем ошибки при загрузке счетчика
            }
        }
    }

    fun setFilterType(type: NotificationType?) {
        _uiState.value = _uiState.value.copy(filterType = type, currentPage = 1)
        loadNotifications(1)
    }

    fun setFilterPriority(priority: NotificationPriority?) {
        _uiState.value = _uiState.value.copy(filterPriority = priority, currentPage = 1)
        loadNotifications(1)
    }

    fun setShowRead(show: Boolean) {
        _uiState.value = _uiState.value.copy(showRead = show, currentPage = 1)
        loadNotifications(1)
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                val result = notificationRepository.markAsRead(notificationId)
                result.getOrElse { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to mark notification as read"
                    )
                    return@launch
                }
                // Обновляем локальное состояние
                val updatedNotifications = _uiState.value.notifications.map {
                    if (it.id == notificationId) it.markAsRead() else it
                }
                _uiState.value = _uiState.value.copy(notifications = updatedNotifications)
                loadUnreadCount()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to mark notification as read"
                )
            }
        }
    }

    fun deleteNotification(notificationId: String) {
        viewModelScope.launch {
            try {
                val result = notificationRepository.deleteNotification(notificationId)
                result.getOrElse { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to delete notification"
                    )
                    return@launch
                }
                // Удаляем из локального состояния
                val updatedNotifications = _uiState.value.notifications.filter {
                    it.id != notificationId
                }
                _uiState.value = _uiState.value.copy(notifications = updatedNotifications)
                loadUnreadCount()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete notification"
                )
            }
        }
    }

    fun markAllAsRead() {
        viewModelScope.launch {
            try {
                val unreadNotifications = _uiState.value.notifications.filter { !it.read }
                unreadNotifications.forEach { notification ->
                    notificationRepository.markAsRead(notification.id).getOrElse {
                        return@forEach
                    }
                }
                val updatedNotifications = _uiState.value.notifications.map { it.markAsRead() }
                _uiState.value = _uiState.value.copy(notifications = updatedNotifications)
                loadUnreadCount()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to mark all as read"
                )
            }
        }
    }

    fun loadMore() {
        if (!_uiState.value.isLoading && _uiState.value.hasMore) {
            loadNotifications(_uiState.value.currentPage + 1)
        }
    }

    fun refresh() {
        loadNotifications(1)
        loadUnreadCount()
    }
}
