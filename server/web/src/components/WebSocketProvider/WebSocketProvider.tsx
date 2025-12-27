'use client';

import { useEffect } from 'react';
import { useWebSocket } from '@/hooks/useWebSocket';
import { useSelector } from 'react-redux';
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

  // Автоматическая подписка на каналы при подключении
  useEffect(() => {
    if (connected && isAuthenticated) {
      // Подписываемся на все каналы
      subscribe(['cameras', 'events', 'recordings', 'notifications']);
    }
  }, [connected, isAuthenticated, subscribe]);

  return <>{children}</>;
}

