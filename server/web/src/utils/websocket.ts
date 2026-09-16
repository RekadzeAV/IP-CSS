/**
 * WebSocket клиент для real-time обновлений
 */

import { MessageQueue, MessagePriority, QueueOverflowStrategy, QueueMetrics } from './messageQueue';
import { RateLimiter, RateLimitConfig, RateLimitMetrics } from './rateLimiter';
import { ChunkingManager } from './chunkingManager';

export type WebSocketChannel = 'cameras' | 'events' | 'recordings' | 'notifications' | 'analytics';

/**
 * Тип бинарного сообщения
 */
export enum BinaryMessageType {
  IMAGE = 'IMAGE',
  VIDEO_CHUNK = 'VIDEO_CHUNK',
  FILE = 'FILE',
  CUSTOM = 'CUSTOM',
}

/**
 * Метаданные для бинарных сообщений
 */
export interface BinaryMessageMetadata {
  type: string;
  mimeType: string;
  size: number;
  messageId: string;
  chunkIndex?: number;
  totalChunks?: number;
  timestamp?: number;
}

/**
 * Базовое WebSocket сообщение
 */
export interface WebSocketMessage {
  type: string;
  channel?: string;
  data?: unknown;
  error?: string;
  code?: string;
}

/**
 * Сообщение с изображением
 */
export interface ImageMessage extends WebSocketMessage {
  type: 'image';
  binaryData: ArrayBuffer;
  metadata: BinaryMessageMetadata;
}

/**
 * Сообщение с видео фрагментом
 */
export interface VideoChunkMessage extends WebSocketMessage {
  type: 'video_chunk';
  binaryData: ArrayBuffer;
  metadata: BinaryMessageMetadata;
}

/**
 * Сообщение с файлом
 */
export interface FileMessage extends WebSocketMessage {
  type: 'file';
  binaryData: ArrayBuffer;
  metadata: BinaryMessageMetadata;
  fileName: string;
}

/**
 * Пользовательское бинарное сообщение
 */
export interface CustomBinaryMessage extends WebSocketMessage {
  type: 'custom_binary';
  binaryData: ArrayBuffer;
  metadata: BinaryMessageMetadata;
  format: string;
}

export interface WebSocketFilters {
  cameraIds?: string[];
  eventTypes?: string[];
  severities?: string[];
  [key: string]: unknown;
}

export interface WebSocketConfig {
  url: string;
  token?: string; // Опциональный, если не указан, будет получен через API
  getToken?: () => Promise<string | null>; // Функция для получения токена
  onMessage: (message: WebSocketMessage) => void;
  onError?: (error: Error) => void;
  onConnect?: () => void;
  onDisconnect?: () => void;
  autoReconnect?: boolean;
  /** Базовая задержка перед первым переподключением (мс). При exponential backoff используется как начальное значение. */
  reconnectInterval?: number;
  /** Экспоненциальный backoff: delay = min(reconnectMaxDelayMs, reconnectInterval * 2^attempt) + jitter */
  reconnectExponentialBackoff?: boolean;
  /** Верхняя граница задержки переподключения (мс) */
  reconnectMaxDelayMs?: number;
  /** Доля джиттера от delay (0–1), снижает thundering herd */
  reconnectJitterRatio?: number;
  reconnectMaxAttempts?: number;
  queueMaxSize?: number;
  queueOverflowStrategy?: QueueOverflowStrategy;
  rateLimitConfig?: RateLimitConfig;
  onRateLimitExceeded?: (operationType: string, limitType: string) => void;
  onQueueOverflow?: (strategy: QueueOverflowStrategy, droppedCount: number) => void;
}

export class WebSocketClient {
  private ws: WebSocket | null = null;
  private config: WebSocketConfig;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;
  private isReconnecting = false;
  /** Счётчик неудачных переподключений подряд; сбрасывается после успешной auth_response */
  private reconnectAttempt = 0;
  private subscribedChannels: Set<WebSocketChannel> = new Set();
  private isAuthenticated = false;
  private messageQueue: MessageQueue;
  private rateLimiter: RateLimiter;
  private chunkingManager: ChunkingManager;

  constructor(config: WebSocketConfig) {
    this.config = {
      autoReconnect: true,
      reconnectInterval: 3000,
      reconnectExponentialBackoff: true,
      reconnectMaxDelayMs: 60_000,
      reconnectJitterRatio: 0.15,
      reconnectMaxAttempts: 20,
      queueMaxSize: 100,
      queueOverflowStrategy: QueueOverflowStrategy.DROP_LOWEST,
      rateLimitConfig: {
        messagesPerSecond: 100,
        subscriptionsPerSecond: 10,
        bytesPerSecond: 10 * 1024 * 1024, // 10MB/сек
        windowSizeMillis: 1000,
        enabled: true,
      },
      ...config,
    };

    this.messageQueue = new MessageQueue(
      this.config.queueMaxSize ?? 100,
      this.config.queueOverflowStrategy ?? QueueOverflowStrategy.DROP_LOWEST
    );
    this.rateLimiter = new RateLimiter(this.config.rateLimitConfig ?? {
      messagesPerSecond: 100,
      subscriptionsPerSecond: 10,
      bytesPerSecond: 10 * 1024 * 1024,
      windowSizeMillis: 1000,
      enabled: true,
    });
    this.chunkingManager = new ChunkingManager();
  }

  /**
   * Подключение к WebSocket серверу
   */
  connect(): void {
    if (this.ws?.readyState === WebSocket.OPEN) {
      console.log('[WebSocket] Already connected');
      return;
    }

    try {
      this.ws = new WebSocket(this.config.url);
      this.setupEventHandlers();
    } catch (error) {
      console.error('[WebSocket] Connection error:', error);
      this.config.onError?.(error as Error);
      this.scheduleReconnect();
    }
  }

  /**
   * Настройка обработчиков событий WebSocket
   */
  private setupEventHandlers(): void {
    if (!this.ws) return;

    this.ws.onopen = async () => {
      console.log('[WebSocket] Connected');
      this.isReconnecting = false;
      this.isAuthenticated = false;
      // reconnectAttempt сбрасывается после успешной auth_response (стабильная сессия)
      await this.authenticate();

      // Отправка сообщений из очереди
      this.sendQueuedMessages();
    };

    this.ws.onmessage = (event) => {
      try {
        // Проверяем, является ли это бинарным сообщением
        if (event.data instanceof ArrayBuffer || event.data instanceof Blob) {
          this.handleBinaryMessage(event.data);
          return;
        }

        // Обычное текстовое сообщение
        const message: WebSocketMessage = JSON.parse(event.data);
        this.handleMessage(message);
      } catch (error) {
        console.error('[WebSocket] Error parsing message:', error);
      }
    };

    this.ws.onerror = (error) => {
      console.error('[WebSocket] Error:', error);
      this.config.onError?.(new Error('WebSocket error'));
    };

    this.ws.onclose = () => {
      console.log('[WebSocket] Disconnected');
      this.isAuthenticated = false;
      this.ws = null;

      if (this.config.autoReconnect && !this.isReconnecting) {
        this.scheduleReconnect();
      }

      this.config.onDisconnect?.();
    };
  }

  /**
   * Аутентификация на сервере
   */
  private async authenticate(): Promise<void> {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      return;
    }

    // Получаем токен
    let token = this.config.token;
    if (!token && this.config.getToken) {
      token = await this.config.getToken() || undefined;
    }

    if (!token) {
      console.error('[WebSocket] No token available for authentication');
      this.config.onError?.(new Error('No token available'));
      // Close socket so reconnect backoff can retry later.
      this.ws?.close();
      return;
    }

    const authMessage = {
      type: 'auth',
      data: {
        token: token,
      },
    };

    this.ws.send(JSON.stringify(authMessage));
  }

  /**
   * Обработка бинарных сообщений
   */
  private async handleBinaryMessage(data: ArrayBuffer | Blob): Promise<void> {
    try {
      const arrayBuffer = data instanceof Blob ? await data.arrayBuffer() : data;
      const uint8Array = new Uint8Array(arrayBuffer);

      // Проверяем, является ли это chunked сообщением
      // Формат: [4 байта - размер JSON метаданных][JSON метаданные][данные chunk]
      if (uint8Array.length >= 4) {
        // Читаем размер метаданных (big-endian)
        const metadataSize =
          ((uint8Array[0] << 24) |
            (uint8Array[1] << 16) |
            (uint8Array[2] << 8) |
            uint8Array[3]) >>>
          0;

        // Если есть метаданные и они помещаются в сообщение
        if (metadataSize > 0 && metadataSize < uint8Array.length - 4) {
          try {
            // Читаем JSON метаданные
            const metadataJson = new TextDecoder().decode(
              uint8Array.slice(4, 4 + metadataSize)
            );

            const metadata: BinaryMessageMetadata = JSON.parse(metadataJson);

            // Если это chunked сообщение
            if (
              metadata.chunkIndex !== undefined &&
              metadata.totalChunks !== undefined
            ) {
              // Читаем данные chunk
              const chunkData = uint8Array.slice(4 + metadataSize);

              // Добавляем chunk к менеджеру
              const completeData = this.chunkingManager.addChunk(
                metadata.messageId,
                metadata.chunkIndex,
                metadata.totalChunks,
                chunkData,
                metadata
              );

              if (completeData) {
                // Все chunks получены, обрабатываем полное сообщение
                console.debug(
                  `[WebSocket] Assembled chunked message ${metadata.messageId} from ${metadata.totalChunks} chunks`
                );
                this.processCompleteBinaryMessage(completeData, metadata);
              } else {
                // Chunks еще собираются
                console.debug(
                  `[WebSocket] Received chunk ${metadata.chunkIndex + 1}/${metadata.totalChunks} of message ${metadata.messageId}`
                );
              }
              return;
            }
          } catch (e) {
            console.warn(
              '[WebSocket] Failed to parse chunk metadata, treating as regular binary message',
              e
            );
            // Продолжаем обработку как обычное сообщение
          }
        }
      }

      // Обычное (не chunked) бинарное сообщение
      this.processCompleteBinaryMessage(uint8Array, null);
    } catch (error) {
      console.error('[WebSocket] Error processing binary message:', error);
      this.config.onError?.(error as Error);
    }
  }

  /**
   * Обработка полного бинарного сообщения
   */
  private processCompleteBinaryMessage(
    binaryData: Uint8Array,
    existingMetadata: BinaryMessageMetadata | null
  ): void {
    // Определяем тип бинарного сообщения
    const messageType = this.detectBinaryMessageType(binaryData);
    const mimeType = this.detectMimeType(binaryData);

    // Валидация данных
    if (!this.validateBinaryData(binaryData, messageType)) {
      console.warn('[WebSocket] Binary data validation failed');
      this.config.onError?.(new Error('Binary data validation failed'));
      return;
    }

    // Создаем метаданные
    const messageId =
      existingMetadata?.messageId || this.generateMessageId();
    const metadata: BinaryMessageMetadata = existingMetadata || {
      type: messageType || BinaryMessageType.CUSTOM,
      mimeType: mimeType,
      size: binaryData.length,
      messageId: messageId,
      timestamp: Date.now(),
    };

    // Создаем соответствующее сообщение
    let binaryMessage: WebSocketMessage;
    switch (messageType) {
      case BinaryMessageType.IMAGE:
        binaryMessage = {
          type: 'image',
          binaryData: binaryData.buffer,
          metadata: metadata,
        } as ImageMessage;
        break;
      case BinaryMessageType.VIDEO_CHUNK:
        binaryMessage = {
          type: 'video_chunk',
          binaryData: binaryData.buffer,
          metadata: metadata,
        } as VideoChunkMessage;
        break;
      case BinaryMessageType.FILE:
        binaryMessage = {
          type: 'file',
          binaryData: binaryData.buffer,
          metadata: metadata,
          fileName: `file_${messageId}`,
        } as FileMessage;
        break;
      default:
        binaryMessage = {
          type: 'custom_binary',
          binaryData: binaryData.buffer,
          metadata: metadata,
          format: mimeType,
        } as CustomBinaryMessage;
    }

    this.config.onMessage(binaryMessage);
  }

  /**
   * Определить тип бинарного сообщения по magic bytes
   */
  private detectBinaryMessageType(data: Uint8Array): BinaryMessageType | null {
    if (data.length < 4) return null;

    // JPEG: FF D8 FF
    if (data[0] === 0xFF && data[1] === 0xD8 && data[2] === 0xFF) {
      return BinaryMessageType.IMAGE;
    }

    // PNG: 89 50 4E 47 0D 0A 1A 0A
    if (
      data[0] === 0x89 &&
      data[1] === 0x50 &&
      data[2] === 0x4E &&
      data[3] === 0x47 &&
      data[4] === 0x0D &&
      data[5] === 0x0A &&
      data[6] === 0x1A &&
      data[7] === 0x0A
    ) {
      return BinaryMessageType.IMAGE;
    }

    // WebP: RIFF...WEBP
    if (
      data[0] === 0x52 &&
      data[1] === 0x49 &&
      data[2] === 0x46 &&
      data[3] === 0x46 &&
      data.length >= 12 &&
      data[8] === 0x57 &&
      data[9] === 0x45 &&
      data[10] === 0x42 &&
      data[11] === 0x50
    ) {
      return BinaryMessageType.IMAGE;
    }

    // H.264/H.265 NAL: 00 00 00 01 или 00 00 01
    if (
      (data[0] === 0x00 && data[1] === 0x00 && data[2] === 0x00 && data[3] === 0x01) ||
      (data[0] === 0x00 && data[1] === 0x00 && data[2] === 0x01)
    ) {
      return BinaryMessageType.VIDEO_CHUNK;
    }

    return BinaryMessageType.FILE;
  }

  /**
   * Определить MIME тип по данным
   */
  private detectMimeType(data: Uint8Array): string {
    const messageType = this.detectBinaryMessageType(data);
    switch (messageType) {
      case BinaryMessageType.IMAGE:
        if (data[0] === 0xFF && data[1] === 0xD8) return 'image/jpeg';
        if (data[0] === 0x89 && data[1] === 0x50) return 'image/png';
        if (data[8] === 0x57 && data[9] === 0x45) return 'image/webp';
        return 'image/jpeg';
      case BinaryMessageType.VIDEO_CHUNK:
        return 'video/h264';
      default:
        return 'application/octet-stream';
    }
  }

  /**
   * Валидация бинарных данных
   */
  private validateBinaryData(data: Uint8Array, expectedType: BinaryMessageType | null): boolean {
    // Проверка размера
    if (data.length === 0) {
      console.warn('[WebSocket] Binary data is empty');
      return false;
    }

    const MAX_MESSAGE_SIZE = 100 * 1024 * 1024; // 100MB
    if (data.length > MAX_MESSAGE_SIZE) {
      console.warn(
        `[WebSocket] Binary data size (${data.length}) exceeds maximum (${MAX_MESSAGE_SIZE})`
      );
      return false;
    }

    // Проверка типа, если указан
    if (expectedType !== null) {
      const detectedType = this.detectBinaryMessageType(data);
      if (detectedType !== expectedType && expectedType !== BinaryMessageType.CUSTOM) {
        console.warn(
          `[WebSocket] Binary data type mismatch: expected ${expectedType}, detected ${detectedType}`
        );
        return false;
      }
    }

    return true;
  }

  /**
   * Генерация уникального ID сообщения
   */
  private generateMessageId(): string {
    return `${Date.now()}-${Math.random().toString(36).substr(2, 9)}`;
  }

  /**
   * Обработка входящих сообщений
   */
  private handleMessage(message: WebSocketMessage): void {
    const data = (message.data ?? null) as Record<string, unknown> | null;
    switch (message.type) {
      case 'auth_response':
        if (data?.success) {
          console.log('[WebSocket] Authenticated');
          this.isAuthenticated = true;
          this.reconnectAttempt = 0;
          this.config.onConnect?.();
          // Повторно подписываемся на каналы после переподключения
          this.resubscribe();
        } else {
          const authError = (data?.message as string | undefined) || 'Authentication failed';
          console.error('[WebSocket] Authentication failed:', authError);
          this.config.onError?.(new Error(authError));
          // Force reconnect cycle to avoid hanging in unauthenticated OPEN state.
          this.ws?.close();
        }
        break;

      case 'subscribe_response':
        if (data?.success) {
          console.log('[WebSocket] Subscribed to channels:', data.channels);
        } else {
          console.error('[WebSocket] Subscribe failed:', data?.message);
        }
        break;

      case 'unsubscribe_response':
        if (data?.success) {
          console.log('[WebSocket] Unsubscribed from channels:', data.channels);
        }
        break;

      case 'event':
      case 'event.created':
      case 'event.acknowledged':
      case 'event.deleted':
        // События канала EVENTS от сервера (event.created и т.д.)
        this.config.onMessage(message);
        break;

      case 'notification.created':
      case 'notification.updated':
      case 'notification.deleted':
      case 'camera.created':
      case 'camera.updated':
      case 'camera.deleted':
      case 'recording.created':
      case 'recording.updated':
      case 'recording.deleted':
      case 'analytics.created':
      case 'analytics.updated':
        // Канальные события, совместимые с форматом broadcastEvent(channel, type, data)
        this.config.onMessage(message);
        break;

      case 'error':
        console.error('[WebSocket] Server error:', message.error, message.code);
        this.config.onError?.(new Error(message.error || 'Server error'));
        break;

      default:
        // Сервер может отправлять кастомные типы по каналу (например motion_detected)
        if (message.channel && message.data) {
          this.config.onMessage(message);
        } else {
          console.warn('[WebSocket] Unknown message type:', message.type);
        }
    }
  }

  /**
   * Подписка на канал
   */
  subscribe(channels: WebSocketChannel[], filters?: WebSocketFilters): void {
    // Проверка rate limit для подписок
    if (!this.rateLimiter.checkSubscriptionLimit()) {
      console.warn('[WebSocket] Subscription rate limit exceeded, subscription queued');
      const subscribeMessage: {
        type: 'subscribe';
        data: { channels: WebSocketChannel[]; filters?: WebSocketFilters };
      } = {
        type: 'subscribe',
        data: {
          channels: channels,
        },
      };
      if (filters && Object.keys(filters).length > 0) {
        subscribeMessage.data.filters = filters;
      }
      this.messageQueue.enqueue(subscribeMessage, MessagePriority.HIGH);
      this.config.onRateLimitExceeded?.('subscription', 'subscriptions_per_second');
      return;
    }

    if (!this.ws || this.ws.readyState !== WebSocket.OPEN || !this.isAuthenticated) {
      // Сохраняем каналы для подписки после подключения
      channels.forEach((channel) => this.subscribedChannels.add(channel));
      // Добавляем в очередь
      const subscribeMessage: {
        type: 'subscribe';
        data: { channels: WebSocketChannel[]; filters?: WebSocketFilters };
      } = {
        type: 'subscribe',
        data: {
          channels: channels,
        },
      };
      if (filters && Object.keys(filters).length > 0) {
        subscribeMessage.data.filters = filters;
      }
      this.messageQueue.enqueue(subscribeMessage, MessagePriority.HIGH);
      return;
    }

    const subscribeMessage: {
      type: 'subscribe';
      data: { channels: WebSocketChannel[]; filters?: WebSocketFilters };
    } = {
      type: 'subscribe',
      data: {
        channels: channels,
      },
    };

    if (filters && Object.keys(filters).length > 0) {
      subscribeMessage.data.filters = filters;
    }

    this.ws.send(JSON.stringify(subscribeMessage));
    channels.forEach((channel) => this.subscribedChannels.add(channel));
  }

  /**
   * Отписка от канала
   */
  unsubscribe(channels: WebSocketChannel[]): void {
    // Проверка rate limit для отписок
    if (!this.rateLimiter.checkSubscriptionLimit()) {
      console.warn('[WebSocket] Unsubscription rate limit exceeded, unsubscription queued');
      const unsubscribeMessage = {
        type: 'unsubscribe',
        data: {
          channels: channels,
        },
      };
      this.messageQueue.enqueue(unsubscribeMessage, MessagePriority.HIGH);
      this.config.onRateLimitExceeded?.('subscription', 'subscriptions_per_second');
      return;
    }

    if (!this.ws || this.ws.readyState !== WebSocket.OPEN || !this.isAuthenticated) {
      channels.forEach((channel) => this.subscribedChannels.delete(channel));
      // Добавляем в очередь
      const unsubscribeMessage = {
        type: 'unsubscribe',
        data: {
          channels: channels,
        },
      };
      this.messageQueue.enqueue(unsubscribeMessage, MessagePriority.HIGH);
      return;
    }

    const unsubscribeMessage = {
      type: 'unsubscribe',
      data: {
        channels: channels,
      },
    };

    this.ws.send(JSON.stringify(unsubscribeMessage));
    channels.forEach((channel) => this.subscribedChannels.delete(channel));
  }

  /**
   * Повторная подписка на сохраненные каналы
   */
  private resubscribe(): void {
    if (this.subscribedChannels.size > 0) {
      this.subscribe(Array.from(this.subscribedChannels));
    }
  }

  /**
   * Запланировать переподключение
   */
  private scheduleReconnect(): void {
    if (this.isReconnecting) return;
    const maxAttempts = this.config.reconnectMaxAttempts ?? 20;
    if (this.reconnectAttempt >= maxAttempts) {
      this.config.onError?.(
        new Error(`WebSocket reconnect aborted after ${maxAttempts} attempts`)
      );
      return;
    }

    this.isReconnecting = true;
    const baseMs = this.config.reconnectInterval ?? 3000;
    const maxDelayMs = this.config.reconnectMaxDelayMs ?? 60_000;
    const useExp = this.config.reconnectExponentialBackoff !== false;
    const jitterRatio = Math.min(1, Math.max(0, this.config.reconnectJitterRatio ?? 0.15));

    const expDelay = useExp
      ? Math.min(maxDelayMs, baseMs * Math.pow(2, Math.min(this.reconnectAttempt, 16)))
      : baseMs;
    const jitter = expDelay * jitterRatio * Math.random();
    const delayMs = Math.min(maxDelayMs, Math.floor(expDelay + jitter));

    this.reconnectAttempt += 1;

    this.reconnectTimer = setTimeout(() => {
      console.log(
        `[WebSocket] Attempting to reconnect (delay=${delayMs}ms, attempt=${this.reconnectAttempt})...`
      );
      this.isReconnecting = false;
      this.connect();
    }, delayMs);
  }

  /**
   * Проверка состояния подключения
   */
  isConnected(): boolean {
    return this.ws?.readyState === WebSocket.OPEN && this.isAuthenticated;
  }

  /**
   * Получить состояние подключения
   */
  getReadyState(): number {
    return this.ws?.readyState ?? WebSocket.CLOSED;
  }

  /**
   * Отправить бинарное сообщение
   */
  sendBinary(data: ArrayBuffer | Uint8Array, priority: MessagePriority = MessagePriority.NORMAL): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN || !this.isAuthenticated) {
      console.warn('[WebSocket] Cannot send binary: not connected, queuing');
      // Добавляем в очередь
      const message = {
        type: 'binary',
        binaryData: data instanceof Uint8Array ? data.buffer : data,
        priority,
      };
      this.messageQueue.enqueue(message, priority);
      return;
    }

    const uint8Array = data instanceof Uint8Array ? data : new Uint8Array(data);

    // Проверка rate limit (кроме CRITICAL приоритета)
    if (priority !== MessagePriority.CRITICAL) {
      if (!this.rateLimiter.checkMessageLimit()) {
        console.warn('[WebSocket] Message rate limit exceeded, queuing');
        const message = {
          type: 'binary',
          binaryData: uint8Array.buffer,
          priority,
        };
        this.messageQueue.enqueue(message, priority);
        this.config.onRateLimitExceeded?.('message', 'messages_per_second');
        return;
      }

      // Проверка лимита на размер
      if (!this.rateLimiter.checkBytesLimit(uint8Array.length)) {
        console.warn('[WebSocket] Bytes rate limit exceeded, queuing');
        const message = {
          type: 'binary',
          binaryData: uint8Array.buffer,
          priority,
        };
        this.messageQueue.enqueue(message, priority);
        this.config.onRateLimitExceeded?.('bytes', 'bytes_per_second');
        return;
      }
    }

    // Отправка с поддержкой chunking
    this.sendBinaryData(uint8Array);
  }

  /**
   * Отправить бинарные данные с поддержкой chunking
   */
  private sendBinaryData(data: Uint8Array, metadata?: BinaryMessageMetadata): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) return;

    // Проверяем, нужно ли разбивать на chunks
    if (ChunkingManager.needsChunking(data)) {
      const messageId = metadata?.messageId || this.generateMessageId();
      const chunks = ChunkingManager.splitIntoChunks(data, messageId);
      const totalChunks = chunks.length;

      // Отправляем каждый chunk
      chunks.forEach((chunk, index) => {
        const chunkMetadata: BinaryMessageMetadata = metadata
          ? {
              ...metadata,
              messageId,
              chunkIndex: index,
              totalChunks,
              size: data.length,
            }
          : {
              type: this.detectBinaryMessageType(data) || BinaryMessageType.CUSTOM,
              mimeType: this.detectMimeType(data),
              size: data.length,
              messageId,
              chunkIndex: index,
              totalChunks,
              timestamp: Date.now(),
            };

        // Отправляем chunk с метаданными в заголовке
        // Формат: [4 байта - размер JSON][JSON метаданные][данные chunk]
        const metadataJson = JSON.stringify(chunkMetadata);
        const metadataBytes = new TextEncoder().encode(metadataJson);
        const metadataSize = metadataBytes.length;

        const chunkWithMetadata = new Uint8Array(4 + metadataSize + chunk.length);
        // Записываем размер метаданных (big-endian)
        chunkWithMetadata[0] = (metadataSize >>> 24) & 0xff;
        chunkWithMetadata[1] = (metadataSize >>> 16) & 0xff;
        chunkWithMetadata[2] = (metadataSize >>> 8) & 0xff;
        chunkWithMetadata[3] = metadataSize & 0xff;
        // Копируем метаданные
        chunkWithMetadata.set(metadataBytes, 4);
        // Копируем данные chunk
        chunkWithMetadata.set(chunk, 4 + metadataSize);

        this.ws?.send(chunkWithMetadata.buffer);
      });
    } else {
      // Отправляем как обычное сообщение
      this.ws.send(data.buffer);
    }
  }

  /**
   * Отправка сообщений из очереди
   */
  private sendQueuedMessages(): void {
    while (!this.messageQueue.isEmpty()) {
      const queuedMessage = this.messageQueue.dequeue() as
        | { type?: string; binaryData?: ArrayBuffer | Uint8Array }
        | null;
      if (!queuedMessage) break;

      try {
        // Определяем приоритет сообщения
        let priority = MessagePriority.NORMAL;
        if (queuedMessage.type === 'auth' || queuedMessage.type === 'error') {
          priority = MessagePriority.CRITICAL;
        } else if (
          queuedMessage.type === 'subscribe' ||
          queuedMessage.type === 'unsubscribe' ||
          queuedMessage.type === 'event'
        ) {
          priority = MessagePriority.HIGH;
        }

        // Проверяем rate limit перед отправкой
        if (priority === MessagePriority.CRITICAL || this.rateLimiter.checkMessageLimit()) {
          if (queuedMessage.type === 'binary') {
            if (queuedMessage.binaryData) {
              this.sendBinary(queuedMessage.binaryData, priority);
            }
          } else {
            this.ws?.send(JSON.stringify(queuedMessage));
          }
        } else {
          // Если лимит все еще превышен, возвращаем в очередь
          this.messageQueue.enqueue(queuedMessage, priority);
          break;
        }
      } catch (e) {
        console.warn('[WebSocket] Failed to send queued message', e);
      }
    }
  }

  /**
   * Получить метрики rate limiting
   */
  getRateLimitMetrics(): RateLimitMetrics {
    return this.rateLimiter.getMetrics();
  }

  /**
   * Получить метрики очереди сообщений
   */
  getQueueMetrics(): QueueMetrics {
    return this.messageQueue.getMetrics();
  }

  /**
   * Отключение от WebSocket сервера
   */
  disconnect(): void {
    if (this.reconnectTimer) {
      clearTimeout(this.reconnectTimer);
      this.reconnectTimer = null;
    }

    this.isReconnecting = false;
    this.reconnectAttempt = 0;
    this.config.autoReconnect = false;

    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }

    this.isAuthenticated = false;
    this.subscribedChannels.clear();
    this.chunkingManager.clear();
    this.messageQueue.clear();
    this.rateLimiter.reset();
  }
}

