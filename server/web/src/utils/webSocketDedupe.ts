import type { WebSocketMessage } from '@/utils/websocket';

const DEFAULT_WINDOW_MS = 2000;
const DEFAULT_MAX_ENTRIES = 400;

export type WebSocketDedupeState = {
  map: Map<string, number>;
  windowMs: number;
  maxEntries: number;
};

export function createWebSocketDedupeState(
  windowMs = DEFAULT_WINDOW_MS,
  maxEntries = DEFAULT_MAX_ENTRIES
): WebSocketDedupeState {
  return { map: new Map(), windowMs, maxEntries };
}

function hasBinaryPayload(message: WebSocketMessage): boolean {
  return Boolean(
    message &&
      typeof message === 'object' &&
      'binaryData' in message &&
      (message as { binaryData?: unknown }).binaryData
  );
}

/**
 * Ключ для сравнения «того же» доменного события (не схлопывает осмысленно разные progress/status).
 */
export function webSocketMessageFingerprint(message: WebSocketMessage): string {
  if (hasBinaryPayload(message)) {
    const m = message as { metadata?: { messageId?: string; chunkIndex?: number } };
    const mid = m.metadata?.messageId ?? '';
    const idx = m.metadata?.chunkIndex ?? '';
    return `binary\0${message.type}\0${mid}\0${idx}`;
  }

  const payload = message.data;
  if (payload && typeof payload === 'object' && !Array.isArray(payload)) {
    const p = payload as Record<string, unknown>;
    const parts = [
      message.type,
      message.channel ?? '',
      String(p.channel ?? ''),
      String(p.id ?? p.cameraId ?? ''),
      String(p.event ?? p.type ?? ''),
      String(p.timestamp ?? ''),
      String(p.streamId ?? ''),
      String(p.progress ?? p.status ?? p.read ?? ''),
    ];
    return parts.join('\0');
  }
  return `${message.type}\0${message.channel ?? ''}\0${JSON.stringify(payload)}`;
}

/**
 * Возвращает true, если сообщение следует отбросить как дубликат (повтор за окно windowMs).
 */
export function shouldDropDuplicateWebSocketMessage(
  message: WebSocketMessage,
  state: WebSocketDedupeState
): boolean {
  if (hasBinaryPayload(message)) {
    return false;
  }

  const key = webSocketMessageFingerprint(message);
  const now = Date.now();
  const { map, windowMs, maxEntries } = state;
  const prev = map.get(key);
  if (prev !== undefined && now - prev < windowMs) {
    return true;
  }
  map.set(key, now);
  const cutoff = now - windowMs * 4;
  for (const [k, t] of map) {
    if (t < cutoff) {
      map.delete(k);
    }
  }
  while (map.size > maxEntries) {
    const first = map.keys().next().value;
    if (first !== undefined) {
      map.delete(first);
    } else {
      break;
    }
  }
  return false;
}
