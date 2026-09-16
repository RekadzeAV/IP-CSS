'use client';

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  type ReactNode,
} from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { WebSocketClient, type WebSocketChannel, type WebSocketFilters } from '@/utils/websocket';
import {
  setConnecting,
  setConnected,
  setError,
  subscribe as subscribeAction,
  unsubscribe as unsubscribeAction,
  setLastMessage,
} from '@/store/slices/websocketSlice';
import { updateCameraStatus, updateCameraFromWebSocket } from '@/store/slices/camerasSlice';
import {
  addEventFromWebSocket,
  updateEventPartiallyFromWebSocket,
  removeEventFromWebSocket,
} from '@/store/slices/eventsSlice';
import {
  addRecordingFromWebSocket,
  updateRecordingFromWebSocket,
  removeRecordingFromWebSocket,
} from '@/store/slices/recordingsSlice';
import {
  addNotificationFromWebSocket,
  updateNotificationFromWebSocket,
} from '@/store/slices/notificationsSlice';
import { addRealtimeEvent } from '@/store/slices/analyticsSlice';
import { updateStreamFromWebSocket } from '@/store/slices/streamsSlice';
import type { RootState } from '@/store';
import type { WebSocketMessage } from '@/utils/websocket';
import type { ApiResponse, Camera, Event, Recording } from '@/types';
import apiClient from '@/utils/api';
import { createWebSocketDedupeState, shouldDropDuplicateWebSocketMessage } from '@/utils/webSocketDedupe';
import { authService } from '@/services/authService';

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || 'ws://localhost:8080/api/v1/ws';

export interface WebSocketContextValue {
  connected: boolean;
  connecting: boolean;
  subscribe: (channels: WebSocketChannel[], filters?: WebSocketFilters) => void;
  unsubscribe: (channels: WebSocketChannel[]) => void;
  subscribedChannels: WebSocketChannel[];
}

const WebSocketContext = createContext<WebSocketContextValue | null>(null);

/**
 * Один экземпляр WebSocket на приложение; дочерние компоненты используют useWebSocket().
 */
export function WebSocketConnectionProvider({ children }: { children: ReactNode }) {
  const dispatch = useDispatch();
  const wsClientRef = useRef<WebSocketClient | null>(null);
  const { connected, connecting, subscribedChannels } = useSelector(
    (state: RootState) => state.websocket
  );
  const isAuthenticated = useSelector((state: RootState) => state.auth.isAuthenticated);
  const dedupeStateRef = useRef(createWebSocketDedupeState());
  const refreshInFlightRef = useRef<Promise<boolean> | null>(null);
  const lastRefreshAttemptRef = useRef(0);

  const getWebSocketToken = useCallback(async (): Promise<string | null> => {
    try {
      const response = await apiClient.get<ApiResponse<{ token: string }>>('/auth/ws-token');
      if (response.data.success && response.data.data?.token) {
        return response.data.data.token;
      }
      return null;
    } catch (error) {
      // Session may be stale. Use single-flight refresh with cooldown to avoid request storms.
      try {
        const now = Date.now();
        const refreshCooldownMs = 15_000;
        if (now - lastRefreshAttemptRef.current < refreshCooldownMs) {
          return null;
        }

        lastRefreshAttemptRef.current = now;
        if (!refreshInFlightRef.current) {
          refreshInFlightRef.current = authService
            .refreshToken()
            .then(() => true)
            .catch(() => false)
            .finally(() => {
              refreshInFlightRef.current = null;
            });
        }

        const refreshed = await refreshInFlightRef.current;
        if (!refreshed) {
          return null;
        }

        const retry = await apiClient.get<ApiResponse<{ token: string }>>('/auth/ws-token');
        if (retry.data.success && retry.data.data?.token) {
          return retry.data.data.token;
        }
      } catch (retryError) {
        console.error('[WebSocket] Error getting token after refresh:', retryError);
      }
      console.error('[WebSocket] Error getting token:', error);
      return null;
    }
  }, []);

  const handleWebSocketMessage = useCallback((message: WebSocketMessage) => {
    const payload = (message.data ?? null) as Record<string, unknown> | null;
    const channel = message.channel || (payload?.channel as string | undefined);
    if (channel && payload) {
      const eventType = (payload.event as string) || (payload.type as string) || message.type;

      switch (channel) {
        case 'cameras':
          if (eventType === 'stream_started' || eventType === 'stream_stopped') {
            const cameraId = payload.cameraId as string | undefined;
            const streamId = payload.streamId as string | undefined;
            if (cameraId) {
              dispatch(
                updateCameraStatus({
                  cameraId,
                  status: eventType === 'stream_started' ? 'ONLINE' : 'OFFLINE',
                })
              );
              dispatch(
                updateStreamFromWebSocket({
                  cameraId,
                  streamId,
                  event: eventType as 'stream_started' | 'stream_stopped',
                  ...(eventType === 'stream_started' && {
                    serverAnalyticsRequested:
                      typeof payload.analyticsRequested === 'boolean'
                        ? payload.analyticsRequested
                        : undefined,
                    serverAnalyticsPipelineActive:
                      typeof payload.analyticsPipelineActive === 'boolean'
                        ? payload.analyticsPipelineActive
                        : undefined,
                    serverHasRtspFrameSource:
                      typeof payload.hasRtspFrameSource === 'boolean'
                        ? payload.hasRtspFrameSource
                        : undefined,
                  }),
                })
              );
            }
          } else if (payload.cameraId && payload.status) {
            dispatch(
              updateCameraStatus({
                cameraId: payload.cameraId as string,
                status: payload.status as Camera['status'],
              })
            );
          } else if (payload.id) {
            dispatch(updateCameraFromWebSocket(payload as unknown as Camera));
          }
          break;
        case 'events':
          if (!payload.id) break;
          if (message.type === 'event.created') {
            dispatch(addEventFromWebSocket(payload as unknown as Event));
          } else if (message.type === 'event.acknowledged') {
            dispatch(updateEventPartiallyFromWebSocket({ id: payload.id as string, ...payload }));
          } else if (message.type === 'event.deleted') {
            dispatch(removeEventFromWebSocket(payload.id as string));
          } else {
            dispatch(addEventFromWebSocket(payload as unknown as Event));
          }
          break;
        case 'recordings':
          if (payload) {
            if (eventType === 'recording_created' && payload.id) {
              const recording = payload as unknown as Recording;
              dispatch(addRecordingFromWebSocket(recording));
            } else if (
              (eventType === 'recording_updated' ||
                eventType === 'recording_status_changed' ||
                eventType === 'recording_progress') &&
              payload.id
            ) {
              const recording = payload as unknown as Recording;
              dispatch(updateRecordingFromWebSocket(recording));
            } else if (eventType === 'recording_deleted') {
              const recordingId =
                (payload.id as string | undefined) || (payload.recordingId as string | undefined);
              if (recordingId) {
                dispatch(removeRecordingFromWebSocket(recordingId));
              }
            }
          }
          break;
        case 'notifications':
          if (!payload.id) break;
          if (message.type === 'notification.updated') {
            dispatch(
              updateNotificationFromWebSocket(payload as unknown as import('@/types').Notification)
            );
          } else {
            dispatch(addNotificationFromWebSocket(payload as unknown as import('@/types').Notification));
          }
          break;
        case 'analytics':
          if (!payload.cameraId || !payload.timestamp || !payload.resultType) break;
          dispatch(
            addRealtimeEvent({
              cameraId: String(payload.cameraId),
              timestamp: Number(payload.timestamp),
              resultType: String(payload.resultType),
              detectors: Array.isArray(payload.detectors)
                ? payload.detectors.map((item) => String(item))
                : [],
              summary:
                payload.summary && typeof payload.summary === 'object'
                  ? {
                      motionDetected: Boolean(
                        (payload.summary as Record<string, unknown>).motionDetected
                      ),
                      objectsCount: Number((payload.summary as Record<string, unknown>).objectsCount ?? 0),
                      facesCount: Number((payload.summary as Record<string, unknown>).facesCount ?? 0),
                      platesCount: Number((payload.summary as Record<string, unknown>).platesCount ?? 0),
                    }
                  : undefined,
            })
          );
          break;
      }
    }
  }, [dispatch]);

  const onMessageRef = useRef<(message: WebSocketMessage) => void>(() => {});
  onMessageRef.current = (message: WebSocketMessage) => {
    if (shouldDropDuplicateWebSocketMessage(message, dedupeStateRef.current)) {
      return;
    }
    dispatch(setLastMessage(message));
    handleWebSocketMessage(message);
  };

  useEffect(() => {
    if (!isAuthenticated || typeof window === 'undefined') {
      return;
    }

    wsClientRef.current = new WebSocketClient({
      url: WS_URL,
      getToken: getWebSocketToken,
      onConnect: () => {
        dispatch(setConnected(true));
        console.log('[WebSocket] Connected');
      },
      onDisconnect: () => {
        dedupeStateRef.current.map.clear();
        dispatch(setConnecting(false));
        dispatch(setConnected(false));
        console.log('[WebSocket] Disconnected');
      },
      onError: (error) => {
        dispatch(setError(error.message));
        console.error('[WebSocket] Error:', error.message);
      },
      onMessage: (message: WebSocketMessage) => onMessageRef.current(message),
      autoReconnect: true,
      reconnectInterval: 3000,
      reconnectExponentialBackoff: true,
      reconnectMaxDelayMs: 60_000,
      reconnectJitterRatio: 0.15,
    });

    dispatch(setConnecting(true));
    wsClientRef.current.connect();

    return () => {
      if (wsClientRef.current) {
        wsClientRef.current.disconnect();
        wsClientRef.current = null;
      }
    };
  }, [isAuthenticated, dispatch, getWebSocketToken]);

  useEffect(() => {
    if (!isAuthenticated && wsClientRef.current) {
      wsClientRef.current.disconnect();
      wsClientRef.current = null;
      dispatch(setConnected(false));
    }
  }, [isAuthenticated, dispatch]);

  useEffect(() => {
    if (!isAuthenticated || !connecting || connected) {
      return;
    }

    const timeoutId = window.setTimeout(() => {
      dispatch(setError('WebSocket connection timeout'));
      dispatch(setConnecting(false));
    }, 20_000);

    return () => {
      window.clearTimeout(timeoutId);
    };
  }, [isAuthenticated, connecting, connected, dispatch]);

  const subscribe = useCallback(
    (channels: WebSocketChannel[], filters?: WebSocketFilters) => {
      if (wsClientRef.current) {
        wsClientRef.current.subscribe(channels, filters);
        dispatch(subscribeAction(channels));
      }
    },
    [dispatch]
  );

  const unsubscribe = useCallback(
    (channels: WebSocketChannel[]) => {
      if (wsClientRef.current) {
        wsClientRef.current.unsubscribe(channels);
        dispatch(unsubscribeAction(channels));
      }
    },
    [dispatch]
  );

  const value = useMemo<WebSocketContextValue>(
    () => ({
      connected,
      connecting,
      subscribe,
      unsubscribe,
      subscribedChannels,
    }),
    [connected, connecting, subscribe, unsubscribe, subscribedChannels]
  );

  return <WebSocketContext.Provider value={value}>{children}</WebSocketContext.Provider>;
}

export function useWebSocket(): WebSocketContextValue {
  const ctx = useContext(WebSocketContext);
  if (!ctx) {
    throw new Error('useWebSocket must be used within WebSocketConnectionProvider');
  }
  return ctx;
}
