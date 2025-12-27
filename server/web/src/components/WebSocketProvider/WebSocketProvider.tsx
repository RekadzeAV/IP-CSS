'use client';

import { useEffect } from 'react';
import { useWebSocket } from '@/hooks/useWebSocket';
import { useSelector } from 'react-redux';
import { WebSocketNotificationHandler } from '@/components/WebSocketNotificationHandler';
import type { RootState } from '@/store';

interface WebSocketProviderProps {
  children: React.ReactNode;
}

/**
 * Компонент для управления WebSocket подключением
 * Автоматически подключается при наличии токена и подписывается на каналы
 */
export function WebSocketProvider({ children }: WebSocketProviderProps) {
  const { connected, subscribe } = useWebSocket();
  const isAuthenticated = useSelector((state: RootState) => state.auth.isAuthenticated);
  const eventFilters = useSelector((state: RootState) => state.events.filters);

  // Автоматическая подписка на каналы при подключении
  useEffect(() => {
    if (connected && isAuthenticated) {
      // Подписываемся на все каналы с фильтрами для событий
      const eventFiltersForWS: any = {};
      if (eventFilters.cameraId) {
        eventFiltersForWS.cameraIds = [eventFilters.cameraId];
      }
      if (eventFilters.type) {
        eventFiltersForWS.eventTypes = [eventFilters.type];
      }
      if (eventFilters.severity) {
        eventFiltersForWS.severities = [eventFilters.severity];
      }

      subscribe(['cameras', 'events', 'recordings', 'notifications'], {
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

