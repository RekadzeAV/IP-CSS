import apiClient from '@/utils/api';
import type {
  ApiResponse,
  Event,
  PaginatedResponse,
  EventType,
  EventSeverity,
} from '@/types';

export interface EventFilters {
  cameraId?: string;
  type?: EventType;
  severity?: EventSeverity;
  acknowledged?: boolean;
  startTime?: number;
  endTime?: number;
  page?: number;
  limit?: number;
}

export interface AcknowledgeEventsRequest {
  ids: string[];
}

export interface EventStatistics {
  total: number;
  byType: Record<string, number>;
  bySeverity: Record<string, number>;
  unacknowledged: number;
}

export const eventService = {
  /**
   * Получить список событий с фильтрацией и пагинацией
   */
  async getEvents(filters?: EventFilters): Promise<PaginatedResponse<Event>> {
    const params = new URLSearchParams();
    
    if (filters?.cameraId) params.append('cameraId', filters.cameraId);
    if (filters?.type) params.append('type', filters.type);
    if (filters?.severity) params.append('severity', filters.severity);
    if (filters?.acknowledged !== undefined) params.append('acknowledged', String(filters.acknowledged));
    if (filters?.startTime) params.append('startTime', String(filters.startTime));
    if (filters?.endTime) params.append('endTime', String(filters.endTime));
    if (filters?.page) params.append('page', String(filters.page));
    if (filters?.limit) params.append('limit', String(filters.limit));
    
    const response = await apiClient.get<ApiResponse<PaginatedResponse<Event>>>(`/events?${params.toString()}`);
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to fetch events');
  },

  /**
   * Получить событие по ID
   */
  async getEventById(id: string): Promise<Event> {
    const response = await apiClient.get<ApiResponse<Event>>(`/events/${id}`);
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to fetch event');
  },

  /**
   * Удалить событие
   */
  async deleteEvent(id: string): Promise<void> {
    const response = await apiClient.delete<ApiResponse<null>>(`/events/${id}`);
    if (!response.data.success) {
      throw new Error(response.data.message || 'Failed to delete event');
    }
  },

  /**
   * Подтвердить событие
   */
  async acknowledgeEvent(id: string): Promise<Event> {
    const response = await apiClient.post<ApiResponse<Event>>(`/events/${id}/acknowledge`);
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to acknowledge event');
  },

  /**
   * Массовое подтверждение событий
   */
  async acknowledgeEvents(ids: string[]): Promise<Event[]> {
    const response = await apiClient.post<ApiResponse<Event[]>>('/events/acknowledge', { ids });
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to acknowledge events');
  },

  /**
   * Получить статистику событий
   */
  async getStatistics(cameraId?: string, startTime?: number, endTime?: number): Promise<EventStatistics> {
    const params = new URLSearchParams();
    if (cameraId) params.append('cameraId', cameraId);
    if (startTime) params.append('startTime', String(startTime));
    if (endTime) params.append('endTime', String(endTime));
    
    const response = await apiClient.get<ApiResponse<EventStatistics>>(`/events/statistics?${params.toString()}`);
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to fetch event statistics');
  },
};

