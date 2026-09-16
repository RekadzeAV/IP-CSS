import React, { useState, useEffect } from 'react';
import { Card } from '../common/Card';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import type { PerformanceMetrics } from '../../types';

interface PerformanceWidgetProps {
  metrics?: PerformanceMetrics;
}

export const PerformanceMetricsWidget: React.FC<PerformanceWidgetProps> = ({ 
  metrics = mockMetrics 
}) => {
  const [chartData, setChartData] = useState(mockChartData);

  useEffect(() => {
    // Simulate real-time updates
    const interval = setInterval(() => {
      setChartData(prev => {
        const newData = [...prev.slice(1)];
        newData.push({
          time: new Date().toLocaleTimeString(),
          fps: Math.max(20, Math.min(35, 27 + Math.random() * 10 - 5)),
          latency: Math.max(50, Math.min(300, 150 + Math.random() * 100 - 50))
        });
        return newData;
      });
    }, 2000);

    return () => clearInterval(interval);
  }, []);

  const getStatusColor = () => {
    if (metrics.avgFps >= 25 && metrics.avgLatency <= 200) return 'text-green-600';
    if (metrics.avgFps >= 20 && metrics.avgLatency <= 300) return 'text-yellow-600';
    return 'text-red-600';
  };

  return (
    <Card 
      title="Performance Metrics" 
      subtitle="Real-time monitoring"
      action={
        <span className={`text-sm font-semibold ${getStatusColor()}`}>
          {metrics.avgFps >= 25 ? '✅ Good' : metrics.avgFps >= 20 ? '⚠️ Fair' : '❌ Poor'}
        </span>
      }
    >
      <div className="grid grid-cols-2 gap-4 mb-4">
        <div className="bg-blue-50 dark:bg-blue-900 rounded-lg p-4">
          <div className="text-sm text-blue-700 dark:text-blue-300 mb-1">Average FPS</div>
          <div className="text-3xl font-bold text-blue-600 dark:text-blue-400">
            {metrics.avgFps.toFixed(1)}
          </div>
          <div className="text-xs text-blue-600 dark:text-blue-400 mt-1">
            Min: {metrics.minFps.toFixed(1)} | Max: {metrics.maxFps.toFixed(1)}
          </div>
        </div>
        <div className="bg-purple-50 dark:bg-purple-900 rounded-lg p-4">
          <div className="text-sm text-purple-700 dark:text-purple-300 mb-1">Avg Latency</div>
          <div className="text-3xl font-bold text-purple-600 dark:text-purple-400">
            {metrics.avgLatency.toFixed(0)}ms
          </div>
          <div className="text-xs text-purple-600 dark:text-purple-400 mt-1">
            Target: ≤200ms
          </div>
        </div>
      </div>

      <div className="mb-4">
        <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">FPS Over Time</h4>
        <ResponsiveContainer width="100%" height={150}>
          <LineChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="time" tick={{ fontSize: 10 }} />
            <YAxis domain={[0, 40]} tick={{ fontSize: 10 }} />
            <Tooltip />
            <Line type="monotone" dataKey="fps" stroke="#3b82f6" strokeWidth={2} dot={false} />
          </LineChart>
        </ResponsiveContainer>
      </div>

      <div className="grid grid-cols-2 gap-4 text-sm">
        <div className="flex items-center space-x-2">
          <div className="w-3 h-3 bg-red-500 rounded-full"></div>
          <span className="text-gray-600 dark:text-gray-400">Dropped Frames:</span>
          <span className="font-semibold text-gray-900 dark:text-white">{metrics.droppedFrames}</span>
        </div>
        <div className="flex items-center space-x-2">
          <div className="w-3 h-3 bg-yellow-500 rounded-full"></div>
          <span className="text-gray-600 dark:text-gray-400">Reconnections:</span>
          <span className="font-semibold text-gray-900 dark:text-white">{metrics.reconnections}</span>
        </div>
      </div>
    </Card>
  );
};

const mockMetrics: PerformanceMetrics = {
  avgFps: 27.5,
  minFps: 22.0,
  maxFps: 32.0,
  avgLatency: 145.0,
  droppedFrames: 23,
  reconnections: 2
};

const mockChartData = Array.from({ length: 20 }, (_, i) => ({
  time: new Date(Date.now() - (19 - i) * 2000).toLocaleTimeString(),
  fps: 27 + Math.random() * 10 - 5,
  latency: 150 + Math.random() * 100 - 50
}));
