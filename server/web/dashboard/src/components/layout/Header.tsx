import React from 'react';

interface HeaderProps {
  title?: string;
}

export const Header: React.FC<HeaderProps> = ({ title = 'IP Camera Dashboard' }) => {
  return (
    <header className="bg-primary-600 text-white shadow-lg">
      <div className="container mx-auto px-4 py-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <svg className="w-8 h-8" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 10l4.553-2.276A1 1 0 0121 8.618v6.764a1 1 0 01-1.447.894L15 14M5 18h8a2 2 0 002-2V8a2 2 0 00-2-2H5a2 2 0 00-2 2v8a2 2 0 002 2z" />
            </svg>
            <h1 className="text-2xl font-bold">{title}</h1>
          </div>
          <nav className="flex space-x-6">
            <a href="/dashboard" className="hover:text-primary-200 transition">Dashboard</a>
            <a href="/cameras" className="hover:text-primary-200 transition">Cameras</a>
            <a href="/analytics" className="hover:text-primary-200 transition">Analytics</a>
            <a href="/settings" className="hover:text-primary-200 transition">Settings</a>
          </nav>
          <div className="flex items-center space-x-4">
            <span className="text-sm text-primary-200">Admin</span>
            <button className="bg-primary-700 hover:bg-primary-800 px-3 py-1 rounded transition">
              Logout
            </button>
          </div>
        </div>
      </div>
    </header>
  );
};
