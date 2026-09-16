import React, { useState, useEffect } from 'react';
import { Card } from '../common/Card';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

interface NetworkTrafficWidgetProps {
  // totalBandwidth?: number; // Reserved for future use
}

export const NetworkTrafficWidget: React.FC<NetworkTrafficWidgetProps> = () => {
  const [uploadSpeed, setUploadSpeed] = useState(0.3);
  const [downloadSpeed, setDownloadSpeed] = useState(0.9);
  const [chartData, setChartData] = useState(mockChartData);

  useEffect(() => {
    const interval = setInterval(() => {
      setUploadSpeed(prev => Math.max(0.1, Math.min(1.5, prev + (Math.random() - 0.5) * 0.2)));
      setDownloadSpeed(prev => Math.max(0.5, Math.min(2.0, prev + (Math.random() - 0.5) * 0.3)));
      
      setChartData(prev => {
        const newData = [...prev.slice(1)];
        newData.push({
          time: new Date().toLocaleTimeString(),
          upload: parseFloat(uploadSpeed.toFixed(2)),
          download: parseFloat(downloadSpeed.toFixed(2))
        });
        return newData;
      });
    }, 2000);

    return () => clearInterval(interval);
  }, [uploadSpeed, downloadSpeed]);

  return (
    <Card title="Network Traffic" subtitle="Real-time bandwidth">
      <div className="grid grid-cols-3 gap-4 mb-4">
        <div className="bg-blue-50 dark:bg-blue-900 rounded-lg p-3 text-center">
          <div className="text-sm text-blue-700 dark:text-blue-300">Upload</div>
          <div className="text-2xl font-bold text-blue-600 dark:text-blue-400">
            {uploadSpeed.toFixed(2)} <span className="text-sm">MB/s</span>
          </div>
        </div>
        <div className="bg-green-50 dark:bg-green-900 rounded-lg p-3 text-center">
          <div className="text-sm text-green-700 dark:text-green-300">Download</div>
          <div className="text-2xl font-bold text-green-600 dark:text-green-400">
            {downloadSpeed.toFixed(2)} <span className="text-sm">MB/s</span>
          </div>
        </div>
        <div className="bg-purple-50 dark:bg-purple-900 rounded-lg p-3 text-center">
          <div className="text-sm text-purple-700 dark:text-purple-300">Total</div>
          <div className="text-2xl font-bold text-purple-600 dark:text-purple-400">
            {(uploadSpeed + downloadSpeed).toFixed(2)} <span className="text-sm">MB/s</span>
          </div>
        </div>
      </div>

      <div className="mb-4">
        <h4 className="text-sm font-medium text-gray-700 dark:text-gray-300 mb-2">Bandwidth Over Time</h4>
        <ResponsiveContainer width="100%" height={180}>
          <LineChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="time" tick={{ fontSize: 10 }} />
            <YAxis tick={{ fontSize: 10 }} />
            <Tooltip />
            <Legend />
            <Line type="monotone" dataKey="upload" stroke="#3b82f6" strokeWidth={2} name="Upload" />
            <Line type="monotone" dataKey="download" stroke="#22c55e" strokeWidth={2} name="Download" />
          </LineChart>
        </ResponsiveContainer>
      </div>

      <div className="grid grid-cols-2 gap-4 text-sm">
        <div>
          <div className="text-gray-600 dark:text-gray-400">Peak Bandwidth</div>
          <div className="font-semibold text-gray-900 dark:text-white">2.8 MB/s</div>
        </div>
        <div>
          <div className="text-gray-600 dark:text-gray-400">Active Connections</div>
          <div className="font-semibold text-gray-900 dark:text-white">12</div>
        </div>
      </div>
    </Card>
  );
};

const mockChartData = Array.from({ length: 20 }, (_, i) => ({
  time: new Date(Date.now() - (19 - i) * 2000).toLocaleTimeString(),
  upload: 0.3 + Math.random() * 0.2,
  download: 0.9 + Math.random() * 0.3
}));
