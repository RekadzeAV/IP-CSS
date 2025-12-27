import apiClient from '@/utils/api';
import type {
  ApiResponse,
  SystemSettings,
} from '@/types';

export interface SettingsDto {
  id: string;
  category: string;
  key: string;
  value: string;
  type: string;
  description?: string;
  updatedAt: number;
}

export interface UpdateSettingsRequest {
  settings: Record<string, string>;
}

export interface UpdateSettingRequest {
  value: string;
}

export const settingsService = {
  /**
   * Получить все настройки
   */
  async getSettings(category?: string): Promise<SettingsDto[]> {
    const params = category ? `?category=${category}` : '';
    const response = await apiClient.get<ApiResponse<SettingsDto[]>>(`/settings${params}`);
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to fetch settings');
  },

  /**
   * Получить настройку по ключу
   */
  async getSetting(key: string): Promise<SettingsDto> {
    const response = await apiClient.get<ApiResponse<SettingsDto>>(`/settings/${key}`);
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to fetch setting');
  },

  /**
   * Обновить настройки
   */
  async updateSettings(settings: Record<string, string>): Promise<number> {
    const response = await apiClient.put<ApiResponse<number>>('/settings', { settings });
    if (response.data.success && response.data.data !== null) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to update settings');
  },

  /**
   * Обновить одну настройку
   */
  async updateSetting(key: string, value: string): Promise<SettingsDto> {
    const response = await apiClient.put<ApiResponse<SettingsDto>>(`/settings/${key}`, { value });
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to update setting');
  },

  /**
   * Удалить настройку
   */
  async deleteSetting(key: string): Promise<void> {
    const response = await apiClient.delete<ApiResponse<null>>(`/settings/${key}`);
    if (!response.data.success) {
      throw new Error(response.data.message || 'Failed to delete setting');
    }
  },

  /**
   * Получить системные настройки
   */
  async getSystemSettings(): Promise<SystemSettings | null> {
    const response = await apiClient.get<ApiResponse<SystemSettings>>('/settings/system');
    if (response.data.success) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to fetch system settings');
  },

  /**
   * Экспортировать настройки
   */
  async exportSettings(): Promise<Record<string, string>> {
    const response = await apiClient.post<ApiResponse<Record<string, string>>>('/settings/export');
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to export settings');
  },

  /**
   * Импортировать настройки
   */
  async importSettings(settings: Record<string, string>): Promise<void> {
    const response = await apiClient.post<ApiResponse<null>>('/settings/import', { settings });
    if (!response.data.success) {
      throw new Error(response.data.message || 'Failed to import settings');
    }
  },

  /**
   * Сбросить настройки
   */
  async resetSettings(category?: string): Promise<void> {
    const params = category ? `?category=${category}` : '';
    const response = await apiClient.post<ApiResponse<null>>(`/settings/reset${params}`);
    if (!response.data.success) {
      throw new Error(response.data.message || 'Failed to reset settings');
    }
  },
};

