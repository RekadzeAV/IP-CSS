import apiClient from '@/utils/api';
import type {
  ApiResponse,
  Notification,
  PaginatedResponse,
  NotificationType,
  NotificationPriority,
} from '@/types';

export interface NotificationFilters {
  type?: NotificationType;
  priority?: NotificationPriority;
  read?: boolean;
  page?: number;
  limit?: number;
}

export const notificationService = {
  /**
   * Получить список уведомлений с фильтрацией и пагинацией
   */
  async getNotifications(filters?: NotificationFilters): Promise<PaginatedResponse<Notification>> {
    const params = new URLSearchParams();

    if (filters?.type) params.append('type', filters.type);
    if (filters?.priority) params.append('priority', filters.priority);
    if (filters?.read !== undefined) params.append('read', String(filters.read));
    if (filters?.page) params.append('page', String(filters.page));
    if (filters?.limit) params.append('limit', String(filters.limit));

    const response = await apiClient.get<ApiResponse<PaginatedResponse<Notification>>>(`/notifications?${params.toString()}`);
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to fetch notifications');
  },

  /**
   * Отметить уведомление как прочитанное
   */
  async markAsRead(id: string): Promise<Notification> {
    const response = await apiClient.post<ApiResponse<Notification>>(`/notifications/${id}/read`);
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to mark notification as read');
  },

  /**
   * Массовое подтверждение уведомлений
   */
  async markAsReadMultiple(ids: string[]): Promise<Notification[]> {
    const response = await apiClient.post<ApiResponse<Notification[]>>('/notifications/read', { ids });
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to mark notifications as read');
  },
};

