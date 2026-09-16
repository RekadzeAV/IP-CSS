import React from 'react';

interface SidebarProps {
  isOpen?: boolean;
}

export const Sidebar: React.FC<SidebarProps> = ({ isOpen = true }) => {
  const menuItems = [
    { icon: '📊', label: 'Dashboard', href: '/dashboard', active: true },
    { icon: '📹', label: 'Cameras', href: '/cameras', badge: 8 },
    { icon: '📈', label: 'Analytics', href: '/analytics', badge: 12 },
    { icon: '🔔', label: 'Alerts', href: '/alerts', badge: 3 },
    { icon: '💾', label: 'Recordings', href: '/recordings' },
    { icon: '⚙️', label: 'Settings', href: '/settings' },
  ];

  return (
    <aside className={`bg-gray-800 text-white ${isOpen ? 'w-64' : 'w-20'} transition-all duration-300`}>
      <div className="p-4">
        <h2 className="text-lg font-semibold mb-6">Menu</h2>
        <nav className="space-y-2">
          {menuItems.map((item) => (
            <a
              key={item.href}
              href={item.href}
              className={`flex items-center p-3 rounded-lg transition ${
                item.active
                  ? 'bg-primary-600'
                  : 'hover:bg-gray-700'
              }`}
            >
              <span className="text-xl">{item.icon}</span>
              {isOpen && (
                <>
                  <span className="ml-3 flex-1">{item.label}</span>
                  {item.badge && (
                    <span className="bg-red-500 text-xs px-2 py-1 rounded-full">
                      {item.badge}
                    </span>
                  )}
                </>
              )}
            </a>
          ))}
        </nav>
      </div>
    </aside>
  );
};
