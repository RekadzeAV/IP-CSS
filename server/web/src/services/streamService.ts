import apiClient, { API_URL } from '@/utils/api';

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
    const response = await apiClient.post<{ success: boolean; data: string; message: string }>(
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
    const response = await apiClient.post<{ success: boolean; message: string }>(
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
    const response = await apiClient.get<{ success: boolean; data: StreamStatus; message: string }>(
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
    const response = await apiClient.get<{ success: boolean; data: RtspStreamUrl; message: string }>(
      `/cameras/${cameraId}/stream/rtsp`
    );
    if (response.data.success && response.data.data) {
      return response.data.data.rtspUrl;
    }
    throw new Error(response.data.message || 'Failed to get RTSP URL');
  },

  /**
   * Получить HLS URL для веб-плеера
   */
  getHlsUrl(cameraId: string): string {
    return `${API_URL}/cameras/${cameraId}/stream/hls/playlist.m3u8`;
  },

  /**
   * Создать снимок экрана с потока камеры
   */
  async captureScreenshot(cameraId: string): Promise<string> {
    const response = await apiClient.post<{ success: boolean; data: string; message: string }>(
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
  async setStreamQuality(cameraId: string, quality: 'low' | 'medium' | 'high' | 'ultra'): Promise<void> {
    const response = await apiClient.post<{ success: boolean; message: string }>(
      `/cameras/${cameraId}/stream/quality?quality=${quality}`
    );
    if (!response.data.success) {
      throw new Error(response.data.message || 'Failed to change stream quality');
    }
  },
};

