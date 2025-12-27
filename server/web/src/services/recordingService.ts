import apiClient from '@/utils/api';
import type {
  ApiResponse,
  Recording,
  PaginatedResponse,
} from '@/types';

export interface RecordingFilters {
  cameraId?: string;
  startTime?: number;
  endTime?: number;
  page?: number;
  limit?: number;
}

export const recordingService = {
  /**
   * Получить список записей с фильтрацией и пагинацией
   */
  async getRecordings(filters?: RecordingFilters): Promise<PaginatedResponse<Recording>> {
    const params = new URLSearchParams();
    
    if (filters?.cameraId) params.append('cameraId', filters.cameraId);
    if (filters?.startTime) params.append('startTime', String(filters.startTime));
    if (filters?.endTime) params.append('endTime', String(filters.endTime));
    if (filters?.page) params.append('page', String(filters.page));
    if (filters?.limit) params.append('limit', String(filters.limit));
    
    const response = await apiClient.get<ApiResponse<PaginatedResponse<Recording>>>(`/recordings?${params.toString()}`);
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to fetch recordings');
  },

  /**
   * Получить запись по ID
   */
  async getRecordingById(id: string): Promise<Recording> {
    const response = await apiClient.get<ApiResponse<Recording>>(`/recordings/${id}`);
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to fetch recording');
  },

  /**
   * Удалить запись
   */
  async deleteRecording(id: string): Promise<void> {
    const response = await apiClient.delete<ApiResponse<null>>(`/recordings/${id}`);
    if (!response.data.success) {
      throw new Error(response.data.message || 'Failed to delete recording');
    }
  },

  /**
   * Получить URL для скачивания записи
   */
  async getDownloadUrl(id: string): Promise<string> {
    const response = await apiClient.get<ApiResponse<string>>(`/recordings/${id}/download`);
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to get download URL');
  },

  /**
   * Экспортировать запись
   */
  async exportRecording(id: string, format?: string, quality?: string): Promise<string> {
    const params = new URLSearchParams();
    if (format) params.append('format', format);
    if (quality) params.append('quality', quality);
    
    const response = await apiClient.post<ApiResponse<string>>(
      `/recordings/${id}/export?${params.toString()}`
    );
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to export recording');
  },
};

