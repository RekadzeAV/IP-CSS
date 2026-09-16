import { WebSocketClient } from './websocket';

type MessageEventLike = { data: string | ArrayBuffer | Blob };

class MockWebSocket {
  static OPEN = 1;

  readyState = MockWebSocket.OPEN;
  onopen: (() => void | Promise<void>) | null = null;
  onmessage: ((event: MessageEventLike) => void) | null = null;
  onerror: ((error: unknown) => void) | null = null;
  onclose: (() => void) | null = null;
  sentMessages: string[] = [];

  constructor(public url: string) {}

  send(payload: string): void {
    this.sentMessages.push(payload);
  }

  close(): void {
    this.onclose?.();
  }
}

describe('WebSocketClient protocol handling', () => {
  const originalWebSocket = global.WebSocket;

  beforeEach(() => {
    (global as unknown as { WebSocket: typeof MockWebSocket }).WebSocket = MockWebSocket as unknown as typeof WebSocket;
  });

  afterEach(() => {
    (global as unknown as { WebSocket: typeof WebSocket }).WebSocket = originalWebSocket;
  });

  it('handles typed auth_response and marks connection healthy', async () => {
    const onConnect = jest.fn();
    const onMessage = jest.fn();

    const client = new WebSocketClient({
      url: 'ws://localhost:8080/api/v1/ws',
      token: 'test-token',
      onConnect: onConnect,
      onMessage: onMessage,
      autoReconnect: false,
    });

    client.connect();

    const ws = (client as unknown as { ws: MockWebSocket | null }).ws;
    expect(ws).toBeTruthy();
    expect(ws?.url).toBe('ws://localhost:8080/api/v1/ws');

    await ws?.onopen?.();

    // Серверный контракт после фикса: type + data.success
    ws?.onmessage?.({
      data: JSON.stringify({
        type: 'auth_response',
        data: { success: true, message: 'Authentication successful' },
      }),
    });

    expect(onConnect).toHaveBeenCalledTimes(1);
    expect(client.isConnected()).toBe(true);
  });
});
