import React from 'react';
import { Card } from '../common/Card';

export interface CameraStatus {
  id: string;
  name: string;
  status: 'online' | 'offline' | 'error' | 'recording';
  fps: number;
  resolution: string;
  lastSeen: Date;
  thumbnail?: string;
}

interface CameraStatusWidgetProps {
  cameras?: CameraStatus[];
}

export const CameraStatusWidget: React.FC<CameraStatusWidgetProps> = ({ 
  cameras = mockCameras 
}) => {
  const getStatusColor = (status: CameraStatus['status']) => {
    switch (status) {
      case 'online': return 'bg-green-500';
      case 'offline': return 'bg-gray-500';
      case 'error': return 'bg-red-500';
      case 'recording': return 'bg-blue-500';
    }
  };

  const onlineCount = cameras.filter(c => c.status === 'online').length;
  const offlineCount = cameras.filter(c => c.status === 'offline').length;
  const errorCount = cameras.filter(c => c.status === 'error').length;

  return (
    <Card title="Camera Status" subtitle={`${cameras.length} cameras total`}>
      <div className="grid grid-cols-4 gap-4 mb-6">
        <div className="bg-green-100 dark:bg-green-900 rounded-lg p-3 text-center">
          <div className="text-2xl font-bold text-green-600 dark:text-green-400">{onlineCount}</div>
          <div className="text-xs text-green-700 dark:text-green-300">Online</div>
        </div>
        <div className="bg-gray-100 dark:bg-gray-900 rounded-lg p-3 text-center">
          <div className="text-2xl font-bold text-gray-600 dark:text-gray-400">{offlineCount}</div>
          <div className="text-xs text-gray-700 dark:text-gray-300">Offline</div>
        </div>
        <div className="bg-red-100 dark:bg-red-900 rounded-lg p-3 text-center">
          <div className="text-2xl font-bold text-red-600 dark:text-red-400">{errorCount}</div>
          <div className="text-xs text-red-700 dark:text-red-300">Errors</div>
        </div>
        <div className="bg-blue-100 dark:bg-blue-900 rounded-lg p-3 text-center">
          <div className="text-2xl font-bold text-blue-600 dark:text-blue-400">{cameras.length - onlineCount - offlineCount - errorCount}</div>
          <div className="text-xs text-blue-700 dark:text-blue-300">Recording</div>
        </div>
      </div>

      <div className="space-y-3 max-h-64 overflow-y-auto">
        {cameras.map((camera) => (
          <div key={camera.id} className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-700 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-600 transition">
            <div className="flex items-center space-x-3">
              <div className={`w-3 h-3 rounded-full ${getStatusColor(camera.status)}`}></div>
              <div>
                <div className="font-medium text-gray-900 dark:text-white">{camera.name}</div>
                <div className="text-sm text-gray-500 dark:text-gray-400">{camera.resolution} • {camera.fps} FPS</div>
              </div>
            </div>
            <div className="flex items-center space-x-2">
              <button className="text-primary-600 hover:text-primary-700 text-sm">
                View
              </button>
              <button className="text-gray-500 hover:text-gray-700 text-sm">
                Settings
              </button>
            </div>
          </div>
        ))}
      </div>
    </Card>
  );
};

const mockCameras: CameraStatus[] = [
  { id: '1', name: 'Main Entrance', status: 'online', fps: 30, resolution: '1920x1080', lastSeen: new Date() },
  { id: '2', name: 'Parking Lot', status: 'online', fps: 25, resolution: '1920x1080', lastSeen: new Date() },
  { id: '3', name: 'Back Door', status: 'recording', fps: 30, resolution: '1280x720', lastSeen: new Date() },
  { id: '4', name: 'Warehouse', status: 'online', fps: 20, resolution: '1920x1080', lastSeen: new Date() },
  { id: '5', name: 'Office Area', status: 'offline', fps: 0, resolution: '1920x1080', lastSeen: new Date(Date.now() - 3600000) },
  { id: '6', name: 'Loading Dock', status: 'error', fps: 0, resolution: '1280x720', lastSeen: new Date() },
  { id: '7', name: 'Perimeter North', status: 'online', fps: 30, resolution: '1920x1080', lastSeen: new Date() },
  { id: '8', name: 'Perimeter South', status: 'online', fps: 28, resolution: '1920x1080', lastSeen: new Date() },
];
