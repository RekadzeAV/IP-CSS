import React from 'react';
import { Header } from '../components/layout/Header';
import { Sidebar } from '../components/layout/Sidebar';
import { CameraStatusWidget } from '../components/widgets/CameraStatusWidget';
import { PerformanceMetricsWidget } from '../components/widgets/PerformanceMetricsWidget';
import { NetworkTrafficWidget } from '../components/widgets/NetworkTrafficWidget';
import { UserActivityWidget } from '../components/widgets/UserActivityWidget';
import { CameraPerformanceComparisonWidget } from '../components/widgets/CameraPerformanceComparisonWidget';
import { Card } from '../components/common/Card';

export const Dashboard: React.FC = () => {
  return (
    <div className="min-h-screen bg-gray-100 dark:bg-gray-900">
      <Header />
      <div className="flex">
        <Sidebar />
        <main className="flex-1 p-6">
          <div className="mb-6">
            <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Dashboard</h1>
            <p className="text-gray-600 dark:text-gray-400">Welcome to IP Camera Surveillance System</p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            <CameraStatusWidget />
            
            <PerformanceMetricsWidget />
            
            <NetworkTrafficWidget />

            <UserActivityWidget />

            <CameraPerformanceComparisonWidget />

            <Card title="System Health" subtitle="Real-time metrics">
              <div className="space-y-4">
                <div>
                  <div className="flex justify-between mb-1">
                    <span className="text-sm font-medium text-gray-700 dark:text-gray-300">CPU</span>
                    <span className="text-sm font-medium text-gray-700 dark:text-gray-300">23%</span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2.5 dark:bg-gray-700">
                    <div className="bg-primary-600 h-2.5 rounded-full" style={{ width: '23%' }}></div>
                  </div>
                </div>
                <div>
                  <div className="flex justify-between mb-1">
                    <span className="text-sm font-medium text-gray-700 dark:text-gray-300">Memory</span>
                    <span className="text-sm font-medium text-gray-700 dark:text-gray-300">45%</span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2.5 dark:bg-gray-700">
                    <div className="bg-green-500 h-2.5 rounded-full" style={{ width: '45%' }}></div>
                  </div>
                </div>
                <div>
                  <div className="flex justify-between mb-1">
                    <span className="text-sm font-medium text-gray-700 dark:text-gray-300">Disk</span>
                    <span className="text-sm font-medium text-gray-700 dark:text-gray-300">67%</span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-2.5 dark:bg-gray-700">
                    <div className="bg-yellow-500 h-2.5 rounded-full" style={{ width: '67%' }}></div>
                  </div>
                </div>
              </div>
            </Card>

            <Card title="Analytics Overview" subtitle="Last 24 hours">
              <div className="grid grid-cols-2 gap-4">
                <div className="bg-blue-50 dark:bg-blue-900 rounded-lg p-4 text-center">
                  <div className="text-2xl font-bold text-blue-600 dark:text-blue-400">156</div>
                  <div className="text-sm text-blue-700 dark:text-blue-300">Motion Events</div>
                </div>
                <div className="bg-purple-50 dark:bg-purple-900 rounded-lg p-4 text-center">
                  <div className="text-2xl font-bold text-purple-600 dark:text-purple-400">23</div>
                  <div className="text-sm text-purple-700 dark:text-purple-300">Objects</div>
                </div>
                <div className="bg-green-50 dark:bg-green-900 rounded-lg p-4 text-center">
                  <div className="text-2xl font-bold text-green-600 dark:text-green-400">8</div>
                  <div className="text-sm text-green-700 dark:text-green-300">Faces</div>
                </div>
                <div className="bg-orange-50 dark:bg-orange-900 rounded-lg p-4 text-center">
                  <div className="text-2xl font-bold text-orange-600 dark:text-orange-400">5</div>
                  <div className="text-sm text-orange-700 dark:text-orange-300">License Plates</div>
                </div>
              </div>
            </Card>

            <Card title="Active Streams" subtitle="Real-time monitoring">
              <div className="text-center py-8">
                <div className="text-4xl font-bold text-primary-600 dark:text-primary-400">5/8</div>
                <div className="text-sm text-gray-600 dark:text-gray-400 mt-2">cameras streaming</div>
                <div className="mt-4 flex justify-center space-x-4">
                  <div className="text-center">
                    <div className="text-lg font-semibold text-gray-900 dark:text-white">1.2 GB/s</div>
                    <div className="text-xs text-gray-500 dark:text-gray-400">Bandwidth</div>
                  </div>
                  <div className="text-center">
                    <div className="text-lg font-semibold text-gray-900 dark:text-white">28 FPS</div>
                    <div className="text-xs text-gray-500 dark:text-gray-400">Avg FPS</div>
                  </div>
                </div>
              </div>
            </Card>

            <Card title="Storage Usage" subtitle="Current utilization">
              <div className="space-y-4">
                <div>
                  <div className="flex justify-between mb-2">
                    <span className="text-sm font-medium text-gray-700 dark:text-gray-300">Total: 2.4 TB</span>
                    <span className="text-sm font-medium text-gray-700 dark:text-gray-300">67% used</span>
                  </div>
                  <div className="w-full bg-gray-200 rounded-full h-4 dark:bg-gray-700">
                    <div className="bg-primary-600 h-4 rounded-full" style={{ width: '67%' }}></div>
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-4 text-sm">
                  <div>
                    <div className="text-gray-600 dark:text-gray-400">Recordings</div>
                    <div className="font-semibold text-gray-900 dark:text-white">1.2 TB</div>
                  </div>
                  <div>
                    <div className="text-gray-600 dark:text-gray-400">Snapshots</div>
                    <div className="font-semibold text-gray-900 dark:text-white">450 GB</div>
                  </div>
                </div>
                <div className="text-xs text-gray-500 dark:text-gray-400">
                  ~12 days remaining at current rate
                </div>
              </div>
            </Card>

            <Card title="Recent Alerts" subtitle="Latest notifications">
              <div className="space-y-3">
                <div className="flex items-start space-x-3 p-2 bg-red-50 dark:bg-red-900 rounded">
                  <div className="text-red-600 dark:text-red-400">⚠️</div>
                  <div className="flex-1">
                    <div className="text-sm font-medium text-gray-900 dark:text-white">Camera Offline</div>
                    <div className="text-xs text-gray-600 dark:text-gray-400">Office Area • 1h ago</div>
                  </div>
                </div>
                <div className="flex items-start space-x-3 p-2 bg-yellow-50 dark:bg-yellow-900 rounded">
                  <div className="text-yellow-600 dark:text-yellow-400">⚡</div>
                  <div className="flex-1">
                    <div className="text-sm font-medium text-gray-900 dark:text-white">High CPU Usage</div>
                    <div className="text-xs text-gray-600 dark:text-gray-400">Server • 2h ago</div>
                  </div>
                </div>
                <div className="flex items-start space-x-3 p-2 bg-blue-50 dark:bg-blue-900 rounded">
                  <div className="text-blue-600 dark:text-blue-400">ℹ️</div>
                  <div className="flex-1">
                    <div className="text-sm font-medium text-gray-900 dark:text-white">Backup Complete</div>
                    <div className="text-xs text-gray-600 dark:text-gray-400">System • 3h ago</div>
                  </div>
                </div>
              </div>
            </Card>
          </div>
        </main>
      </div>
    </div>
  );
};
