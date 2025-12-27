import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { notificationService, NotificationFilters } from '@/services/notificationService';
import type { Notification } from '@/types';

interface NotificationsState {
  notifications: Notification[];
  loading: boolean;
  error: string | null;
  pagination: {
    page: number;
    limit: number;
    total: number;
    hasMore: boolean;
  };
  filters: NotificationFilters;
  unreadCount: number;
}

const initialState: NotificationsState = {
  notifications: [],
  loading: false,
  error: null,
  pagination: {
    page: 1,
    limit: 20,
    total: 0,
    hasMore: false,
  },
  filters: {},
  unreadCount: 0,
};

// Async thunks
export const fetchNotifications = createAsyncThunk(
  'notifications/fetchNotifications',
  async (filters?: NotificationFilters, { rejectWithValue }) => {
    try {
      const result = await notificationService.getNotifications(filters);
      return result;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to fetch notifications');
    }
  }
);

export const markNotificationAsRead = createAsyncThunk(
  'notifications/markAsRead',
  async (id: string, { rejectWithValue }) => {
    try {
      const notification = await notificationService.markAsRead(id);
      return notification;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to mark notification as read');
    }
  }
);

export const markNotificationsAsRead = createAsyncThunk(
  'notifications/markAsReadMultiple',
  async (ids: string[], { rejectWithValue }) => {
    try {
      const notifications = await notificationService.markAsReadMultiple(ids);
      return notifications;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to mark notifications as read');
    }
  }
);

const notificationsSlice = createSlice({
  name: 'notifications',
  initialState,
  reducers: {
    setFilters: (state, action: PayloadAction<NotificationFilters>) => {
      state.filters = { ...state.filters, ...action.payload };
    },
    clearError: (state) => {
      state.error = null;
    },
    addNotificationFromWebSocket: (state, action: PayloadAction<Notification>) => {
      const notification = action.payload;
      // Добавляем новое уведомление в начало списка
      const exists = state.notifications.find((n) => n.id === notification.id);
      if (!exists) {
        state.notifications.unshift(notification);
        if (!notification.read) {
          state.unreadCount++;
        }
      }
    },
    updateNotificationFromWebSocket: (state, action: PayloadAction<Notification>) => {
      const notification = action.payload;
      const index = state.notifications.findIndex((n) => n.id === notification.id);
      if (index !== -1) {
        const wasUnread = !state.notifications[index].read;
        state.notifications[index] = notification;
        // Обновляем счетчик непрочитанных
        if (wasUnread && notification.read) {
          state.unreadCount = Math.max(0, state.unreadCount - 1);
        } else if (!wasUnread && !notification.read) {
          state.unreadCount++;
        }
      }
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch notifications
      .addCase(fetchNotifications.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchNotifications.fulfilled, (state, action) => {
        state.loading = false;
        state.notifications = action.payload.items;
        state.pagination = {
          page: action.payload.page,
          limit: action.payload.limit,
          total: action.payload.total,
          hasMore: action.payload.hasMore,
        };
        // Пересчитываем непрочитанные
        state.unreadCount = state.notifications.filter((n) => !n.read).length;
      })
      .addCase(fetchNotifications.rejected, (state, action) => {
        state.loading = false;
        state.error = (action.payload as string) || 'Failed to fetch notifications';
      })
      // Mark as read
      .addCase(markNotificationAsRead.fulfilled, (state, action) => {
        const index = state.notifications.findIndex((n) => n.id === action.payload.id);
        if (index !== -1) {
          const wasUnread = !state.notifications[index].read;
          state.notifications[index] = action.payload;
          if (wasUnread) {
            state.unreadCount = Math.max(0, state.unreadCount - 1);
          }
        }
      })
      // Mark multiple as read
      .addCase(markNotificationsAsRead.fulfilled, (state, action) => {
        const updatedIds = new Set(action.payload.map((n) => n.id));
        action.payload.forEach((notification) => {
          const index = state.notifications.findIndex((n) => n.id === notification.id);
          if (index !== -1) {
            const wasUnread = !state.notifications[index].read;
            state.notifications[index] = notification;
            if (wasUnread) {
              state.unreadCount = Math.max(0, state.unreadCount - 1);
            }
          }
        });
      });
  },
});

export const { setFilters, clearError, addNotificationFromWebSocket, updateNotificationFromWebSocket } =
  notificationsSlice.actions;
export default notificationsSlice.reducer;

