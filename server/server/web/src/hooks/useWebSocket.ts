import { useEffect, useRef } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { WebSocketClient, WebSocketChannel, WebSocketFilters } from '@/utils/websocket';
import {
  setConnecting,
  setConnected,
  setError,
  subscribe as subscribeAction,
  unsubscribe as unsubscribeAction,
  setLastMessage,
} from '@/store/slices/websocketSlice';
import { updateCameraStatus, updateCameraFromWebSocket } from '@/store/slices/camerasSlice';
import { addEventFromWebSocket, updateEventFromWebSocket } from '@/store/slices/eventsSlice';
import { authService } from '@/services/authService';
import type { RootState } from '@/store';
import type { WebSocketMessage } from '@/utils/websocket';
import type { Camera, Event } from '@/types';

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:8080/api/v1/ws';

/**
 * Hook для управления WebSocket подключением
 */
export function useWebSocket() {
  const dispatch = useDispatch();
  const wsClientRef = useRef<WebSocketClient | null>(null);
  const { connected, connecting, subscribedChannels } = useSelector(
    (state: RootState) => state.websocket
  );
  const isAuthenticated = useSelector((state: RootState) => state.auth.isAuthenticated);

  // Функция для получения токена для WebSocket
  const getWebSocketToken = async (): Promise<string | null> => {
    try {
      // Пытаемся получить токен через специальный endpoint для WebSocket
      // Если такого нет, можно использовать cookie через API
      const response = await fetch('/api/v1/auth/ws-token', {
        method: 'GET',
        credentials: 'include', // Важно для отправки cookies
      });

      if (response.ok) {
        const data = await response.json();
        return data.token || null;
      }

      // Fallback: если endpoint не существует, возвращаем null
      // Сервер WebSocket должен поддерживать аутентификацию через cookie
      return null;
    } catch (error) {
      console.error('[WebSocket] Error getting token:', error);
      return null;
    }
  };

  // Инициализация WebSocket клиента
  useEffect(() => {
    if (!isAuthenticated || typeof window === 'undefined') {
      return;
    }

    // Создаем клиент
    wsClientRef.current = new WebSocketClient({
      url: WS_URL,
      getToken: getWebSocketToken, // Используем функцию для получения токена
      onConnect: () => {
        dispatch(setConnected(true));
        console.log('[WebSocket] Connected');
      },
      onDisconnect: () => {
        dispatch(setConnected(false));
        console.log('[WebSocket] Disconnected');
      },
      onError: (error) => {
        dispatch(setError(error.message));
        console.error('[WebSocket] Error:', error.message);
      },
      onMessage: (message: WebSocketMessage) => {
        dispatch(setLastMessage(message));
        handleWebSocketMessage(message);
      },
      autoReconnect: true,
      reconnectInterval: 3000,
    });

    // Подключаемся
    dispatch(setConnecting(true));
    wsClientRef.current.connect();

    // Очистка при размонтировании
    return () => {
      if (wsClientRef.current) {
        wsClientRef.current.disconnect();
        wsClientRef.current = null;
      }
    };
  }, [isAuthenticated, dispatch]);

  // Обновление при изменении статуса аутентификации
  useEffect(() => {
    if (!isAuthenticated && wsClientRef.current) {
      wsClientRef.current.disconnect();
      wsClientRef.current = null;
      dispatch(setConnected(false));
    }
  }, [isAuthenticated, dispatch]);

  // Обработка WebSocket сообщений и обновление состояния
  const handleWebSocketMessage = (message: WebSocketMessage) => {
    if (message.type === 'event' && message.data) {
      switch (message.channel) {
        case 'cameras':
          // Обновление статуса или данных камеры
          if (message.data.cameraId && message.data.status) {
            dispatch(updateCameraStatus({
              cameraId: message.data.cameraId,
              status: message.data.status,
            }));
          } else if (message.data.id) {
            // Полное обновление камеры
            dispatch(updateCameraFromWebSocket(message.data as Camera));
          }
          break;
        case 'events':
          // Новое или обновленное событие
          if (message.data.id) {
            const event = message.data as Event;
            // Всегда добавляем новое событие (фильтрация происходит в slice)
            dispatch(addEventFromWebSocket(event));
          }
          break;
        case 'recordings':
          // Обновление записи - можно обработать в recordingsSlice при необходимости
          break;
      case 'notifications':
        // Уведомления будут обработаны в компоненте WebSocketNotificationHandler
        console.log('[WebSocket] Notification:', message.data);
        break;
      }
    }
  };

  // Функции для подписки/отписки
  const subscribe = (channels: WebSocketChannel[], filters?: WebSocketFilters) => {
    if (wsClientRef.current && connected) {
      wsClientRef.current.subscribe(channels, filters);
      dispatch(subscribeAction(channels));
    }
  };

  const unsubscribe = (channels: WebSocketChannel[]) => {
    if (wsClientRef.current && connected) {
      wsClientRef.current.unsubscribe(channels);
      dispatch(unsubscribeAction(channels));
    }
  };

  return {
    connected,
    connecting,
    subscribe,
    unsubscribe,
    subscribedChannels,
  };
}

