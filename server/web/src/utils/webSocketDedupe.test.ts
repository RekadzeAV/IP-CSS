import {
  createWebSocketDedupeState,
  shouldDropDuplicateWebSocketMessage,
  webSocketMessageFingerprint,
} from './webSocketDedupe';
import type { WebSocketMessage } from '@/utils/websocket';

describe('webSocketDedupe', () => {
  it('drops identical channel payloads within window', () => {
    const state = createWebSocketDedupeState(2000, 100);
    const msg: WebSocketMessage = {
      type: 'event.created',
      channel: 'events',
      data: { id: 'e1', type: 'MOTION', cameraId: 'c1' },
    };
    expect(shouldDropDuplicateWebSocketMessage(msg, state)).toBe(false);
    expect(shouldDropDuplicateWebSocketMessage(msg, state)).toBe(true);
  });

  it('allows same id when progress changes', () => {
    const state = createWebSocketDedupeState(2000, 100);
    const a: WebSocketMessage = {
      type: 'x',
      channel: 'recordings',
      data: { id: 'r1', event: 'recording_progress', progress: 10 },
    };
    const b: WebSocketMessage = {
      type: 'x',
      channel: 'recordings',
      data: { id: 'r1', event: 'recording_progress', progress: 20 },
    };
    expect(shouldDropDuplicateWebSocketMessage(a, state)).toBe(false);
    expect(shouldDropDuplicateWebSocketMessage(b, state)).toBe(false);
  });

  it('never drops binary messages', () => {
    const state = createWebSocketDedupeState(2000, 100);
    const msg = {
      type: 'image',
      binaryData: new ArrayBuffer(4),
      metadata: { messageId: 'm1', chunkIndex: 0 },
    } as unknown as WebSocketMessage;
    expect(shouldDropDuplicateWebSocketMessage(msg, state)).toBe(false);
    expect(shouldDropDuplicateWebSocketMessage(msg, state)).toBe(false);
  });

  it('fingerprint is stable for same logical event', () => {
    const m1: WebSocketMessage = {
      type: 'notification.created',
      channel: 'notifications',
      data: { id: 'n1', title: 't' },
    };
    const m2: WebSocketMessage = {
      type: 'notification.created',
      channel: 'notifications',
      data: { id: 'n1', title: 't' },
    };
    expect(webSocketMessageFingerprint(m1)).toBe(webSocketMessageFingerprint(m2));
  });
});
