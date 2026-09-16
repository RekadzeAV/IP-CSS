import React from 'react';
import { Card } from '../common/Card';

interface UserActivityWidgetProps {
  activeUsers?: number;
  recentLogins?: Array<{
    username: string;
    timestamp: Date;
    action: string;
    avatar?: string;
  }>;
}

export const UserActivityWidget: React.FC<UserActivityWidgetProps> = ({ 
  activeUsers = 3,
  recentLogins = mockLogins 
}) => {
  const getRoleColor = (role: string) => {
    switch (role) {
      case 'admin': return 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-300';
      case 'operator': return 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-300';
      case 'viewer': return 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-300';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getActionIcon = (action: string) => {
    if (action.includes('login')) return '🔐';
    if (action.includes('camera')) return '📹';
    if (action.includes('settings')) return '⚙️';
    return '📝';
  };

  return (
    <Card title="User Activity" subtitle="Recent sessions">
      <div className="mb-4">
        <div className="text-center py-4 bg-primary-50 dark:bg-primary-900 rounded-lg">
          <div className="text-4xl font-bold text-primary-600 dark:text-primary-400">{activeUsers}</div>
          <div className="text-sm text-primary-700 dark:text-primary-300">Active Users</div>
        </div>
      </div>

      <div className="space-y-3">
        {recentLogins.map((login, index) => (
          <div key={index} className="flex items-center space-x-3 p-2 hover:bg-gray-50 dark:hover:bg-gray-700 rounded-lg transition">
            <div className="w-10 h-10 rounded-full bg-primary-200 dark:bg-primary-800 flex items-center justify-center text-lg">
              {login.avatar || login.username.charAt(0).toUpperCase()}
            </div>
            <div className="flex-1">
              <div className="flex items-center space-x-2">
                <span className="font-medium text-gray-900 dark:text-white">{login.username}</span>
                <span className={`text-xs px-2 py-0.5 rounded-full ${getRoleColor('operator')}`}>
                  Operator
                </span>
              </div>
              <div className="text-sm text-gray-600 dark:text-gray-400">
                {getActionIcon(login.action)} {login.action}
              </div>
            </div>
            <div className="text-xs text-gray-500 dark:text-gray-400">
              {formatTimeAgo(login.timestamp)}
            </div>
          </div>
        ))}
      </div>

      <div className="mt-4 pt-4 border-t border-gray-200 dark:border-gray-700">
        <div className="grid grid-cols-3 gap-2 text-center text-xs">
          <div>
            <div className="font-semibold text-gray-900 dark:text-white">12</div>
            <div className="text-gray-500 dark:text-gray-400">Today</div>
          </div>
          <div>
            <div className="font-semibold text-gray-900 dark:text-white">87</div>
            <div className="text-gray-500 dark:text-gray-400">Week</div>
          </div>
          <div>
            <div className="font-semibold text-gray-900 dark:text-white">342</div>
            <div className="text-gray-500 dark:text-gray-400">Month</div>
          </div>
        </div>
      </div>
    </Card>
  );
};

const formatTimeAgo = (date: Date): string => {
  const now = new Date();
  const diff = Math.floor((now.getTime() - date.getTime()) / 1000);
  
  if (diff < 60) return 'Just now';
  if (diff < 3600) return `${Math.floor(diff / 60)}m ago`;
  if (diff < 86400) return `${Math.floor(diff / 3600)}h ago`;
  return `${Math.floor(diff / 86400)}d ago`;
};

const mockLogins = [
  { username: 'admin', timestamp: new Date(), action: 'Logged in', avatar: 'A' },
  { username: 'operator1', timestamp: new Date(Date.now() - 1800000), action: 'Viewed camera Main Entrance' },
  { username: 'operator2', timestamp: new Date(Date.now() - 3600000), action: 'Changed settings' },
  { username: 'viewer1', timestamp: new Date(Date.now() - 7200000), action: 'Logged in' },
];
