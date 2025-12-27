import api from '@/utils/api';
import type { PTZConfig, PTZType } from '@/types';

export interface PTZControlRequest {
  action: 'move' | 'stop' | 'zoom' | 'preset';
  direction?: 'up' | 'down' | 'left' | 'right' | 'in' | 'out';
  speed?: number;
  presetId?: number;
}

export interface PTZPreset {
  id: number;
  name: string;
  position: {
    pan: number;
    tilt: number;
    zoom: number;
  };
}

class PTZService {
  /**
   * Управление PTZ камерой
   */
  async controlPTZ(cameraId: string, request: PTZControlRequest): Promise<void> {
    await api.post(`/cameras/${cameraId}/ptz`, request);
  }

  /**
   * Остановка движения PTZ
   */
  async stopPTZ(cameraId: string): Promise<void> {
    await api.post(`/cameras/${cameraId}/ptz`, { action: 'stop' });
  }

  /**
   * Движение вверх
   */
  async moveUp(cameraId: string, speed: number = 0.5): Promise<void> {
    await this.controlPTZ(cameraId, { action: 'move', direction: 'up', speed });
  }

  /**
   * Движение вниз
   */
  async moveDown(cameraId: string, speed: number = 0.5): Promise<void> {
    await this.controlPTZ(cameraId, { action: 'move', direction: 'down', speed });
  }

  /**
   * Движение влево
   */
  async moveLeft(cameraId: string, speed: number = 0.5): Promise<void> {
    await this.controlPTZ(cameraId, { action: 'move', direction: 'left', speed });
  }

  /**
   * Движение вправо
   */
  async moveRight(cameraId: string, speed: number = 0.5): Promise<void> {
    await this.controlPTZ(cameraId, { action: 'move', direction: 'right', speed });
  }

  /**
   * Zoom in
   */
  async zoomIn(cameraId: string, speed: number = 0.5): Promise<void> {
    await this.controlPTZ(cameraId, { action: 'zoom', direction: 'in', speed });
  }

  /**
   * Zoom out
   */
  async zoomOut(cameraId: string, speed: number = 0.5): Promise<void> {
    await this.controlPTZ(cameraId, { action: 'zoom', direction: 'out', speed });
  }

  /**
   * Получить список пресетов
   */
  async getPresets(cameraId: string): Promise<PTZPreset[]> {
    const response = await api.get(`/cameras/${cameraId}/ptz/presets`);
    return response.data;
  }

  /**
   * Сохранить пресет
   */
  async savePreset(cameraId: string, name: string): Promise<PTZPreset> {
    const response = await api.post(`/cameras/${cameraId}/ptz/presets`, { name });
    return response.data;
  }

  /**
   * Вызвать пресет
   */
  async recallPreset(cameraId: string, presetId: number): Promise<void> {
    await api.post(`/cameras/${cameraId}/ptz/presets/${presetId}/recall`);
  }

  /**
   * Удалить пресет
   */
  async deletePreset(cameraId: string, presetId: number): Promise<void> {
    await api.delete(`/cameras/${cameraId}/ptz/presets/${presetId}`);
  }
}

export const ptzService = new PTZService();

