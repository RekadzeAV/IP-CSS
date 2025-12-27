/**
 * WebSocket клиент для real-time обновлений
 */

export type WebSocketChannel = 'cameras' | 'events' | 'recordings' | 'notifications';

export interface WebSocketMessage {
  type: string;
  channel?: string;
  data?: any;
  error?: string;
  code?: string;
}

export interface WebSocketConfig {
  url: string;
  token: string;
  onMessage: (message: WebSocketMessage) => void;
  onError?: (error: Error) => void;
  onConnect?: () => void;
  onDisconnect?: () => void;
  autoReconnect?: boolean;
  reconnectInterval?: number;
}

export class WebSocketClient {
  private ws: WebSocket | null = null;
  private config: WebSocketConfig;
  private reconnectTimer: NodeJS.Timeout | null = null;
  private isReconnecting = false;
  private subscribedChannels: Set<WebSocketChannel> = new Set();
  private isAuthenticated = false;

  constructor(config: WebSocketConfig) {
    this.config = {
      autoReconnect: true,
      reconnectInterval: 3000,
      ...config,
    };
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

    this.ws.onopen = () => {
      console.log('[WebSocket] Connected');
      this.isReconnecting = false;
      this.isAuthenticated = false;
      this.authenticate();
    };

    this.ws.onmessage = (event) => {
      try {
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
  private authenticate(): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN) {
      return;
    }

    const authMessage = {
      type: 'auth',
      data: {
        token: this.config.token,
      },
    };

    this.ws.send(JSON.stringify(authMessage));
  }

  /**
   * Обработка входящих сообщений
   */
  private handleMessage(message: WebSocketMessage): void {
    switch (message.type) {
      case 'auth_response':
        if (message.data?.success) {
          console.log('[WebSocket] Authenticated');
          this.isAuthenticated = true;
          this.config.onConnect?.();
          // Повторно подписываемся на каналы после переподключения
          this.resubscribe();
        } else {
          console.error('[WebSocket] Authentication failed:', message.data?.message);
          this.config.onError?.(new Error(message.data?.message || 'Authentication failed'));
        }
        break;

      case 'subscribe_response':
        if (message.data?.success) {
          console.log('[WebSocket] Subscribed to channels:', message.data.channels);
        } else {
          console.error('[WebSocket] Subscribe failed:', message.data?.message);
        }
        break;

      case 'unsubscribe_response':
        if (message.data?.success) {
          console.log('[WebSocket] Unsubscribed from channels:', message.data.channels);
        }
        break;

      case 'event':
        // Пробрасываем событие в обработчик
        this.config.onMessage(message);
        break;

      case 'error':
        console.error('[WebSocket] Server error:', message.error, message.code);
        this.config.onError?.(new Error(message.error || 'Server error'));
        break;

      default:
        console.warn('[WebSocket] Unknown message type:', message.type);
    }
  }

  /**
   * Подписка на канал
   */
  subscribe(channels: WebSocketChannel[]): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN || !this.isAuthenticated) {
      // Сохраняем каналы для подписки после подключения
      channels.forEach((channel) => this.subscribedChannels.add(channel));
      return;
    }

    const subscribeMessage = {
      type: 'subscribe',
      data: {
        channels: channels,
      },
    };

    this.ws.send(JSON.stringify(subscribeMessage));
    channels.forEach((channel) => this.subscribedChannels.add(channel));
  }

  /**
   * Отписка от канала
   */
  unsubscribe(channels: WebSocketChannel[]): void {
    if (!this.ws || this.ws.readyState !== WebSocket.OPEN || !this.isAuthenticated) {
      channels.forEach((channel) => this.subscribedChannels.delete(channel));
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

    this.isReconnecting = true;
    this.reconnectTimer = setTimeout(() => {
      console.log('[WebSocket] Attempting to reconnect...');
      this.isReconnecting = false;
      this.connect();
    }, this.config.reconnectInterval);
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
    this.config.autoReconnect = false;

    if (this.ws) {
      this.ws.close();
      this.ws = null;
    }

    this.isAuthenticated = false;
    this.subscribedChannels.clear();
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
}

