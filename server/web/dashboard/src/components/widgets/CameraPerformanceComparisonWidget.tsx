import React from 'react';
import { Card } from '../common/Card';

interface CameraPerformance {
  cameraId: string;
  name: string;
  avgFps: number;
  avgLatency: number;
  uptime: number;
  errorRate: number;
}

interface CameraPerformanceWidgetProps {
  cameras?: CameraPerformance[];
}

export const CameraPerformanceComparisonWidget: React.FC<CameraPerformanceWidgetProps> = ({
  cameras = mockCameras
}) => {
  const avgFps = cameras.reduce((sum, c) => sum + c.avgFps, 0) / cameras.length;
  const avgLatency = cameras.reduce((sum, c) => sum + c.avgLatency, 0) / cameras.length;
  const avgUptime = cameras.reduce((sum, c) => sum + c.uptime, 0) / cameras.length;

  const getPerformanceScore = (camera: CameraPerformance) => {
    const fpsScore = (camera.avgFps / 30) * 40;
    const latencyScore = Math.max(0, (1 - camera.avgLatency / 300) * 30);
    const uptimeScore = (camera.uptime / 100) * 30;
    return Math.round(fpsScore + latencyScore + uptimeScore);
  };

  const getScoreColor = (score: number) => {
    if (score >= 85) return 'bg-green-500';
    if (score >= 70) return 'bg-yellow-500';
    return 'bg-red-500';
  };

  const sortedCameras = [...cameras].sort((a, b) => 
    getPerformanceScore(b) - getPerformanceScore(a)
  );

  return (
    <Card title="Camera Performance" subtitle="Comparison ranking">
      <div className="grid grid-cols-3 gap-4 mb-4">
        <div className="text-center">
          <div className="text-sm text-gray-600 dark:text-gray-400">Avg FPS</div>
          <div className="text-xl font-bold text-gray-900 dark:text-white">{avgFps.toFixed(1)}</div>
        </div>
        <div className="text-center">
          <div className="text-sm text-gray-600 dark:text-gray-400">Avg Latency</div>
          <div className="text-xl font-bold text-gray-900 dark:text-white">{avgLatency.toFixed(0)}ms</div>
        </div>
        <div className="text-center">
          <div className="text-sm text-gray-600 dark:text-gray-400">Avg Uptime</div>
          <div className="text-xl font-bold text-gray-900 dark:text-white">{avgUptime.toFixed(1)}%</div>
        </div>
      </div>

      <div className="space-y-3">
        {sortedCameras.map((camera, index) => {
          const score = getPerformanceScore(camera);
          return (
            <div key={camera.cameraId} className="p-3 bg-gray-50 dark:bg-gray-700 rounded-lg">
              <div className="flex items-center justify-between mb-2">
                <div className="flex items-center space-x-3">
                  <div className={`w-6 h-6 rounded-full ${getScoreColor(score)} flex items-center justify-center text-xs text-white font-bold`}>
                    {index + 1}
                  </div>
                  <div>
                    <div className="font-medium text-gray-900 dark:text-white">{camera.name}</div>
                    <div className="text-xs text-gray-500 dark:text-gray-400">
                      {camera.avgFps} FPS • {camera.avgLatency}ms latency
                    </div>
                  </div>
                </div>
                <div className="text-right">
                  <div className={`text-lg font-bold ${score >= 85 ? 'text-green-600' : score >= 70 ? 'text-yellow-600' : 'text-red-600'}`}>
                    {score}
                  </div>
                  <div className="text-xs text-gray-500 dark:text-gray-400">score</div>
                </div>
              </div>
              <div className="flex items-center space-x-4 text-xs">
                <div className="flex-1">
                  <div className="flex justify-between mb-1">
                    <span className="text-gray-600 dark:text-gray-400">Uptime</span>
                    <span className="font-medium">{camera.uptime}%</span>
                  </div>
                  <div className="w-full bg-gray-200 dark:bg-gray-600 rounded-full h-1.5">
                    <div 
                      className={`h-1.5 rounded-full ${camera.uptime >= 95 ? 'bg-green-500' : camera.uptime >= 90 ? 'bg-yellow-500' : 'bg-red-500'}`}
                      style={{ width: `${camera.uptime}%` }}
                    ></div>
                  </div>
                </div>
                <div className="flex-1">
                  <div className="flex justify-between mb-1">
                    <span className="text-gray-600 dark:text-gray-400">Errors</span>
                    <span className="font-medium">{(camera.errorRate * 100).toFixed(1)}%</span>
                  </div>
                  <div className="w-full bg-gray-200 dark:bg-gray-600 rounded-full h-1.5">
                    <div 
                      className={`h-1.5 rounded-full ${camera.errorRate <= 0.01 ? 'bg-green-500' : camera.errorRate <= 0.05 ? 'bg-yellow-500' : 'bg-red-500'}`}
                      style={{ width: `${Math.min(100, camera.errorRate * 1000)}%` }}
                    ></div>
                  </div>
                </div>
              </div>
            </div>
          );
        })}
      </div>

      <div className="mt-4 flex justify-between text-xs text-gray-500 dark:text-gray-400">
        <button className="px-3 py-1 bg-primary-100 dark:bg-primary-900 text-primary-700 dark:text-primary-300 rounded hover:bg-primary-200 dark:hover:bg-primary-800 transition">
          Export CSV
        </button>
        <button className="px-3 py-1 bg-gray-100 dark:bg-gray-700 text-gray-700 dark:text-gray-300 rounded hover:bg-gray-200 dark:hover:bg-gray-600 transition">
          Refresh
        </button>
      </div>
    </Card>
  );
};

const mockCameras: CameraPerformance[] = [
  { cameraId: '1', name: 'Main Entrance', avgFps: 29.5, avgLatency: 120, uptime: 99.8, errorRate: 0.002 },
  { cameraId: '2', name: 'Parking Lot', avgFps: 28.0, avgLatency: 145, uptime: 98.5, errorRate: 0.015 },
  { cameraId: '3', name: 'Back Door', avgFps: 25.5, avgLatency: 180, uptime: 97.2, errorRate: 0.028 },
  { cameraId: '4', name: 'Warehouse', avgFps: 22.0, avgLatency: 220, uptime: 95.0, errorRate: 0.05 },
  { cameraId: '5', name: 'Office Area', avgFps: 0, avgLatency: 0, uptime: 0, errorRate: 1.0 },
];
