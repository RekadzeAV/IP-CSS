import apiClient from '@/utils/api';
import type {
  ApiResponse,
  CameraDto,
  Camera,
  CreateCameraRequest,
  UpdateCameraRequest,
  ConnectionTestResultDto,
  DiscoveredCameraDto,
  CameraObservationSummaryDto,
} from '@/types';

export const cameraService = {
  /**
   * Получить список всех камер
   */
  async getCameras(): Promise<Camera[]> {
    const response = await apiClient.get<ApiResponse<CameraDto[]>>('/cameras');
    
    // Проверка формата ответа
    if (response.data.success && response.data.data) {
      // Если data - уже массив, возвращаем его
      if (Array.isArray(response.data.data)) {
        return response.data.data as unknown as Camera[];
      }
      // Если data - объект с ключами камер, преобразуем в массив
      if (typeof response.data.data === 'object') {
        return Object.values(response.data.data) as unknown as Camera[];
      }
    }
    
    // Fallback: проверяем, не является ли сам response.data массивом
    if (Array.isArray(response.data)) {
      return response.data as unknown as Camera[];
    }
    
    throw new Error(response.data.message || 'Failed to fetch cameras');
  },

  /**
   * Получить камеру по ID
   */
  async getCameraById(id: string): Promise<Camera> {
    const response = await apiClient.get<ApiResponse<CameraDto>>(`/cameras/${id}`);
    if (response.data.success && response.data.data) {
      return response.data.data as unknown as Camera;
    }
    throw new Error(response.data.message || 'Failed to fetch camera');
  },

  /**
   * Получить сводку наблюдения (px/m и предупреждение по разрешению)
   */
  async getObservationSummary(id: string): Promise<CameraObservationSummaryDto> {
    const response = await apiClient.get<ApiResponse<CameraObservationSummaryDto>>(
      `/cameras/${id}/observation-summary`
    );
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to fetch observation summary');
  },

  /**
   * Создать новую камеру
   */
  async createCamera(data: CreateCameraRequest): Promise<Camera> {
    const response = await apiClient.post<ApiResponse<CameraDto>>('/cameras', data);
    if (response.data.success && response.data.data) {
      return response.data.data as unknown as Camera;
    }
    throw new Error(response.data.message || 'Failed to create camera');
  },

  /**
   * Обновить камеру
   */
  async updateCamera(id: string, data: UpdateCameraRequest): Promise<Camera> {
    const response = await apiClient.put<ApiResponse<CameraDto>>(`/cameras/${id}`, data);
    if (response.data.success && response.data.data) {
      return response.data.data as unknown as Camera;
    }
    throw new Error(response.data.message || 'Failed to update camera');
  },

  /**
   * Удалить камеру
   */
  async deleteCamera(id: string): Promise<void> {
    const response = await apiClient.delete<ApiResponse<null>>(`/cameras/${id}`);
    if (!response.data.success) {
      throw new Error(response.data.message || 'Failed to delete camera');
    }
  },

  /**
   * Протестировать подключение к камере
   */
  async testConnection(id: string): Promise<ConnectionTestResultDto> {
    const response = await apiClient.post<ApiResponse<ConnectionTestResultDto>>(
      `/cameras/${id}/test`
    );
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to test connection');
  },

  /**
   * Обнаружить камеры в сети
   */
  async discoverCameras(): Promise<DiscoveredCameraDto[]> {
    const response = await apiClient.get<ApiResponse<DiscoveredCameraDto[]>>('/cameras/discover');
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to discover cameras');
  },

  /**
   * Обнаружить камеры с указанием протокола
   * @param protocol - 'onvif' (ONVIF WS-Discovery), 'rtsp' (только RTSP), 'all' (комбинированный)
   */
  async discoverCamerasWithProtocol(protocol: 'onvif' | 'rtsp' | 'all'): Promise<DiscoveredCameraDto[]> {
    const response = await apiClient.get<ApiResponse<DiscoveredCameraDto[]>>(
      `/cameras/discover?protocol=${protocol}&refresh=true`
    );
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to discover cameras');
  },

};



