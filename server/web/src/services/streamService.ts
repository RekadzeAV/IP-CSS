import apiClient, { API_URL } from '@/utils/api';
import type { ApiResponse } from '@/types';

export interface StreamStatus {
  active: boolean;
  streamId: string | null;
  hlsUrl: string | null;
  rtspUrl: string | null;
}

export interface RtspStreamUrl {
  rtspUrl: string;
}

/**
 * Сервис для работы с видеопотоками
 */
export const streamService = {
  /**
   * Начать трансляцию для камеры
   */
  async startStream(cameraId: string): Promise<string> {
    const response = await apiClient.post<ApiResponse<string>>(
      `/cameras/${cameraId}/stream/start`
    );
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to start stream');
  },

  /**
   * Остановить трансляцию для камеры
   */
  async stopStream(cameraId: string): Promise<void> {
    const response = await apiClient.post<ApiResponse<null>>(
      `/cameras/${cameraId}/stream/stop`
    );
    if (!response.data.success) {
      throw new Error(response.data.message || 'Failed to stop stream');
    }
  },

  /**
   * Получить статус трансляции
   */
  async getStreamStatus(cameraId: string): Promise<StreamStatus> {
    const response = await apiClient.get<ApiResponse<StreamStatus>>(
      `/cameras/${cameraId}/stream/status`
    );
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to get stream status');
  },

  /**
   * Получить RTSP URL для прямой трансляции
   */
  async getRtspUrl(cameraId: string): Promise<string> {
    const response = await apiClient.get<ApiResponse<RtspStreamUrl>>(
      `/cameras/${cameraId}/stream/rtsp`
    );
    if (response.data.success && response.data.data) {
      return response.data.data.rtspUrl;
    }
    throw new Error(response.data.message || 'Failed to get RTSP URL');
  },

  /**
   * Получить HLS URL для веб-плеера (одиночное качество)
   */
  getHlsUrl(cameraId: string): string {
    return `${API_URL}/cameras/${cameraId}/stream/hls/playlist.m3u8`;
  },

  /**
   * Получить Master HLS URL для адаптивного битрейта
   */
  getMasterHlsUrl(cameraId: string): string {
    return `${API_URL}/cameras/${cameraId}/stream/hls/master.m3u8`;
  },

  /**
   * Получить HLS URL (master или обычный) - автоматический выбор
   */
  getAdaptiveHlsUrl(cameraId: string): string {
    // Пытаемся использовать master playlist для адаптивного битрейта
    // Если не доступен, fallback на обычный плейлист
    return this.getMasterHlsUrl(cameraId);
  },

  /**
   * Создать снимок экрана с потока камеры
   */
  async captureScreenshot(cameraId: string): Promise<string> {
    const response = await apiClient.post<ApiResponse<string>>(
      `/cameras/${cameraId}/stream/screenshot`
    );
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to capture screenshot');
  },

  /**
   * Изменить качество потока
   */
  async setStreamQuality(
    cameraId: string,
    quality: 'low' | 'medium' | 'high' | 'ultra' | 'qhd1440' | 'uhd4k' | 'qhd1440_h264' | 'uhd4k_h264'
  ): Promise<void> {
    const response = await apiClient.post<ApiResponse<null>>(
      `/cameras/${cameraId}/stream/quality?quality=${quality}`
    );
    if (!response.data.success) {
      throw new Error(response.data.message || 'Failed to change stream quality');
    }
  },
};

