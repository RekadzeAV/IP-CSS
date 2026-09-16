import { useCallback, useEffect, useRef, useState } from 'react';
import { fetchWsToken, getStoredUser } from '../lib/api';

export type WsChannel = 'cameras' | 'events' | 'recordings' | 'notifications';

export interface WsLiveEvent {
  id: string;
  cameraId: string;
  type: string;
  severity: string;
  timestamp: number;
  description?: string;
}

function wsUrl(): string {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
  return `${protocol}//${window.location.host}/api/v1/ws`;
}

const RECONNECT_MS = 3000;
const MAX_RECONNECT_MS = 30_000;

export function useDashboardWebSocket(channels: WsChannel[] = ['events', 'cameras']) {
  const [connected, setConnected] = useState(false);
  const [lastLiveEvent, setLastLiveEvent] = useState<WsLiveEvent | null>(null);
  const socketRef = useRef<WebSocket | null>(null);
  const reconnectAttemptRef = useRef(0);
  const reconnectTimerRef = useRef<number | null>(null);
  const channelsRef = useRef(channels);
  channelsRef.current = channels;

  const clearReconnectTimer = useCallback(() => {
    if (reconnectTimerRef.current != null) {
      window.clearTimeout(reconnectTimerRef.current);
      reconnectTimerRef.current = null;
    }
  }, []);

  const scheduleReconnect = useCallback(
    (connectFn: () => void) => {
      if (!getStoredUser()) return;
      clearReconnectTimer();
      const delay = Math.min(
        RECONNECT_MS * 2 ** reconnectAttemptRef.current,
        MAX_RECONNECT_MS,
      );
      reconnectAttemptRef.current += 1;
      reconnectTimerRef.current = window.setTimeout(connectFn, delay);
    },
    [clearReconnectTimer],
  );

  const connect = useCallback(() => {
    if (!getStoredUser()) {
      return;
    }

    clearReconnectTimer();

    void (async () => {
      let token: string;
      try {
        token = await fetchWsToken();
      } catch {
        scheduleReconnect(connect);
        return;
      }

      const socket = new WebSocket(wsUrl());
      socketRef.current = socket;

      socket.onopen = () => {
        reconnectAttemptRef.current = 0;
        socket.send(JSON.stringify({ type: 'auth', data: { token } }));
      };

      socket.onmessage = (event) => {
        try {
          const message = JSON.parse(event.data as string) as {
            type?: string;
            channel?: string;
            data?: Record<string, unknown>;
          };

          if (message.type === 'auth_response') {
            const success = Boolean(message.data?.success);
            if (success) {
              setConnected(true);
              socket.send(
                JSON.stringify({
                  type: 'subscribe',
                  data: { channels: channelsRef.current },
                }),
              );
            } else {
              setConnected(false);
              socket.close();
            }
            return;
          }

          if (message.channel === 'events' && message.data) {
            const data = message.data;
            const id = String(data.id ?? data.eventId ?? '');
            if (!id) return;
            setLastLiveEvent({
              id,
              cameraId: String(data.cameraId ?? ''),
              type: String(data.type ?? 'EVENT'),
              severity: String(data.severity ?? 'INFO'),
              timestamp: Number(data.timestamp ?? Date.now()),
              description: data.description != null ? String(data.description) : undefined,
            });
          }
        } catch {
          // ignore malformed frames
        }
      };

      socket.onclose = () => {
        setConnected(false);
        socketRef.current = null;
        scheduleReconnect(connect);
      };

      socket.onerror = () => {
        socket.close();
      };
    })();
  }, [clearReconnectTimer, scheduleReconnect]);

  useEffect(() => {
    connect();
    return () => {
      clearReconnectTimer();
      socketRef.current?.close();
      socketRef.current = null;
      setConnected(false);
    };
  }, [connect, clearReconnectTimer]);

  return { connected, lastLiveEvent };
}
