import type { ReactNode } from 'react';
import { act, renderHook } from '@testing-library/react';
import { WebSocketConnectionProvider, useWebSocket } from '@/contexts/WebSocketContext';

const dispatchMock = jest.fn();
let mockState = {
  websocket: {
    connected: true,
    connecting: false,
    subscribedChannels: [] as string[],
  },
  auth: {
    isAuthenticated: true,
  },
};

const connectMock = jest.fn();
const disconnectMock = jest.fn();
const subscribeMock = jest.fn();
const unsubscribeMock = jest.fn();
type MockWsConfig = {
  onMessage: (message: { type: string; channel?: string; data?: Record<string, unknown> }) => void;
  onConnect?: () => void;
  onDisconnect?: () => void;
  onError?: (error: Error) => void;
  autoReconnect?: boolean;
  reconnectInterval?: number;
};
let lastConfig: MockWsConfig | null = null;
const addEventFromWebSocketMock = jest.fn((payload: unknown) => ({
  type: 'events/addEventFromWebSocket',
  payload,
}));
const addNotificationFromWebSocketMock = jest.fn((payload: unknown) => ({
  type: 'notifications/addNotificationFromWebSocket',
  payload,
}));
const addRealtimeEventMock = jest.fn((payload: unknown) => ({
  type: 'analytics/addRealtimeEvent',
  payload,
}));

jest.mock('react-redux', () => ({
  useDispatch: () => dispatchMock,
  useSelector: (selector: (state: unknown) => unknown) => selector(mockState),
}));

jest.mock('@/store/slices/eventsSlice', () => ({
  addEventFromWebSocket: (payload: unknown) => addEventFromWebSocketMock(payload),
  updateEventPartiallyFromWebSocket: jest.fn((payload: unknown) => ({
    type: 'events/updateEventPartiallyFromWebSocket',
    payload,
  })),
  removeEventFromWebSocket: jest.fn((payload: unknown) => ({
    type: 'events/removeEventFromWebSocket',
    payload,
  })),
}));

jest.mock('@/store/slices/notificationsSlice', () => ({
  addNotificationFromWebSocket: (payload: unknown) => addNotificationFromWebSocketMock(payload),
}));

jest.mock('@/store/slices/analyticsSlice', () => ({
  addRealtimeEvent: (payload: unknown) => addRealtimeEventMock(payload),
}));

jest.mock('@/utils/websocket', () => ({
  WebSocketClient: jest.fn().mockImplementation((config: MockWsConfig) => {
    lastConfig = config;
    return {
      connect: connectMock,
      disconnect: disconnectMock,
      subscribe: subscribeMock,
      unsubscribe: unsubscribeMock,
    };
  }),
}));

function wrapper({ children }: { children: ReactNode }) {
  return <WebSocketConnectionProvider>{children}</WebSocketConnectionProvider>;
}

describe('useWebSocket', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    mockState = {
      websocket: {
        connected: true,
        connecting: false,
        subscribedChannels: [],
      },
      auth: {
        isAuthenticated: true,
      },
    };
    lastConfig = null;
  });

  it('initializes websocket client and connects when authenticated', () => {
    renderHook(() => useWebSocket(), { wrapper });

    expect(connectMock).toHaveBeenCalledTimes(1);
    expect(dispatchMock).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'websocket/setConnecting', payload: true })
    );
    expect(lastConfig).toEqual(
      expect.objectContaining({
        autoReconnect: true,
        reconnectInterval: 3000,
      })
    );
  });

  it('subscribes and unsubscribes through websocket client', () => {
    const { result } = renderHook(() => useWebSocket(), { wrapper });

    act(() => {
      result.current.subscribe(['events']);
      result.current.unsubscribe(['events']);
    });

    expect(subscribeMock).toHaveBeenCalledWith(['events'], undefined);
    expect(unsubscribeMock).toHaveBeenCalledWith(['events']);
    expect(dispatchMock).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'websocket/subscribe', payload: ['events'] })
    );
    expect(dispatchMock).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'websocket/unsubscribe', payload: ['events'] })
    );
  });

  it('dispatches event action when events message arrives', () => {
    renderHook(() => useWebSocket(), { wrapper });

    act(() => {
      lastConfig?.onMessage({
        type: 'event.created',
        channel: 'events',
        data: { id: 'ev-1', type: 'MOTION_DETECTION', cameraId: 'cam-1' },
      });
    });

    expect(addEventFromWebSocketMock).toHaveBeenCalledWith(
      expect.objectContaining({ id: 'ev-1', type: 'MOTION_DETECTION' })
    );
    expect(dispatchMock).toHaveBeenCalledWith(
      expect.objectContaining({
        type: 'events/addEventFromWebSocket',
        payload: expect.objectContaining({ id: 'ev-1' }),
      })
    );
  });

  it('dispatches notification action when notifications message arrives', async () => {
    renderHook(() => useWebSocket(), { wrapper });

    act(() => {
      lastConfig?.onMessage({
        type: 'notification.created',
        channel: 'notifications',
        data: { id: 'n-1', title: 'Alert', read: false },
      });
    });

    await act(async () => {
      await Promise.resolve();
      await Promise.resolve();
    });

    expect(addNotificationFromWebSocketMock).toHaveBeenCalledWith(
      expect.objectContaining({ id: 'n-1', title: 'Alert' })
    );
    expect(dispatchMock).toHaveBeenCalledWith(
      expect.objectContaining({
        type: 'notifications/addNotificationFromWebSocket',
        payload: expect.objectContaining({ id: 'n-1' }),
      })
    );
  });

  it('dispatches analytics realtime event when analytics message arrives', () => {
    renderHook(() => useWebSocket(), { wrapper });

    act(() => {
      lastConfig?.onMessage({
        type: 'motion_detected',
        channel: 'analytics',
        data: {
          cameraId: 'cam-1',
          timestamp: 1710000000000,
          resultType: 'motion_detected',
          detectors: ['motion'],
          summary: { motionDetected: true, objectsCount: 0, facesCount: 0, platesCount: 0 },
        },
      });
    });

    expect(addRealtimeEventMock).toHaveBeenCalledWith(
      expect.objectContaining({
        cameraId: 'cam-1',
        resultType: 'motion_detected',
      })
    );
    expect(dispatchMock).toHaveBeenCalledWith(
      expect.objectContaining({
        type: 'analytics/addRealtimeEvent',
      })
    );
  });

  it('updates store when websocket disconnect and reconnect callbacks fire', () => {
    renderHook(() => useWebSocket(), { wrapper });

    act(() => {
      lastConfig?.onDisconnect?.();
      lastConfig?.onConnect?.();
    });

    expect(dispatchMock).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'websocket/setConnected', payload: false })
    );
    expect(dispatchMock).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'websocket/setConnected', payload: true })
    );
  });

  it('stores websocket error when transport fails', () => {
    renderHook(() => useWebSocket(), { wrapper });

    act(() => {
      lastConfig?.onError?.(new Error('socket dropped'));
    });

    expect(dispatchMock).toHaveBeenCalledWith(
      expect.objectContaining({ type: 'websocket/setError', payload: 'socket dropped' })
    );
  });

  it('ignores empty and partially invalid events payloads', () => {
    renderHook(() => useWebSocket(), { wrapper });

    act(() => {
      lastConfig?.onMessage({
        type: 'event.created',
        channel: 'events',
        data: {},
      });
      lastConfig?.onMessage({
        type: 'event.created',
        channel: 'events',
        data: { cameraId: 'cam-1' },
      });
    });

    expect(addEventFromWebSocketMock).not.toHaveBeenCalled();
  });
});
