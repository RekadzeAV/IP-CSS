import { useEffect, useRef } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { WebSocketClient, WebSocketChannel } from '@/utils/websocket';
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
  const token = useSelector((state: RootState) => state.auth.token);

  // Инициализация WebSocket клиента
  useEffect(() => {
    if (!token || typeof window === 'undefined') {
      return;
    }

    // Создаем клиент
    wsClientRef.current = new WebSocketClient({
      url: WS_URL,
      token,
      onConnect: () => {
        dispatch(setConnected(true));
      },
      onDisconnect: () => {
        dispatch(setConnected(false));
      },
      onError: (error) => {
        dispatch(setError(error.message));
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
  }, [token, dispatch]);

  // Обновление токена при изменении
  useEffect(() => {
    if (!token && wsClientRef.current) {
      wsClientRef.current.disconnect();
      wsClientRef.current = null;
      dispatch(setConnected(false));
    }
  }, [token, dispatch]);

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
          // Показать уведомление пользователю
          if (message.data.title || message.data.message) {
            // Можно использовать notistack для показа уведомлений
            console.log('[WebSocket] Notification:', message.data);
          }
          break;
      }
    }
  };

  // Функции для подписки/отписки
  const subscribe = (channels: WebSocketChannel[]) => {
    if (wsClientRef.current && connected) {
      wsClientRef.current.subscribe(channels);
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

