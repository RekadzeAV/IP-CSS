'use client';

import { useEffect } from 'react';
import { useSelector } from 'react-redux';
import { WebSocketConnectionProvider, useWebSocket } from '@/contexts/WebSocketContext';
import { WebSocketNotificationHandler } from '@/components/WebSocketNotificationHandler';
import type { RootState } from '@/store';

interface WebSocketProviderProps {
  children: React.ReactNode;
}

/**
 * Подписка на каналы (один общий WebSocket — см. WebSocketConnectionProvider).
 */
function WebSocketSubscriptions({ children }: WebSocketProviderProps) {
  const { connected, subscribe } = useWebSocket();
  const isAuthenticated = useSelector((state: RootState) => state.auth.isAuthenticated);
  const eventFilters = useSelector((state: RootState) => state.events.filters);

  useEffect(() => {
    if (connected && isAuthenticated) {
      const eventFiltersForWS: {
        cameraIds?: string[];
        eventTypes?: string[];
        severities?: string[];
      } = {};
      if (eventFilters.cameraId) {
        eventFiltersForWS.cameraIds = [eventFilters.cameraId];
      }
      if (eventFilters.type) {
        eventFiltersForWS.eventTypes = [eventFilters.type];
      }
      if (eventFilters.severity) {
        eventFiltersForWS.severities = [eventFilters.severity];
      }

      subscribe(['cameras', 'events', 'recordings', 'notifications', 'analytics'], {
        events: Object.keys(eventFiltersForWS).length > 0 ? eventFiltersForWS : undefined,
      });
    }
  }, [connected, isAuthenticated, subscribe, eventFilters]);

  return (
    <>
      <WebSocketNotificationHandler />
      {children}
    </>
  );
}

/**
 * Единое WebSocket-подключение для приложения и автоматическая подписка на каналы.
 */
export function WebSocketProvider({ children }: WebSocketProviderProps) {
  return (
    <WebSocketConnectionProvider>
      <WebSocketSubscriptions>{children}</WebSocketSubscriptions>
    </WebSocketConnectionProvider>
  );
}
