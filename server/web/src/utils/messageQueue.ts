/**
 * Приоритет сообщения
 */
export enum MessagePriority {
  CRITICAL = 0,  // Аутентификация, ошибки
  HIGH = 1,      // Подписки, события
  NORMAL = 2,    // Обычные сообщения
  LOW = 3        // Статистика, heartbeat
}

/**
 * Стратегия обработки переполнения очереди
 */
export enum QueueOverflowStrategy {
  DROP_OLDEST,  // Удалять старые сообщения
  DROP_LOWEST,  // Удалять сообщения с низким приоритетом
  REJECT,       // Отклонять новые сообщения
  BLOCK         // Блокировать до освобождения места
}

/**
 * Сообщение в очереди с приоритетом
 */
interface QueuedMessage {
  message: unknown;
  priority: MessagePriority;
  timestamp: number;
  retryCount: number;
}

/**
 * Метрики очереди сообщений
 */
export interface QueueMetrics {
  currentSize: number;
  maxSize: number;
  averageWaitTime: number;  // мс
  droppedMessages: number;
  messagesByPriority: Record<MessagePriority, number>;
}

/**
 * Очередь сообщений с приоритетами
 */
export class MessageQueue {
  private queue: QueuedMessage[] = [];
  private maxSize: number;
  private overflowStrategy: QueueOverflowStrategy;
  private droppedMessagesCount = 0;
  private waitTimes: number[] = [];

  constructor(
    maxSize: number = 100,
    overflowStrategy: QueueOverflowStrategy = QueueOverflowStrategy.DROP_LOWEST
  ) {
    this.maxSize = maxSize;
    this.overflowStrategy = overflowStrategy;
  }

  /**
   * Добавить сообщение в очередь
   */
  enqueue(message: unknown, priority: MessagePriority = MessagePriority.NORMAL): boolean {
    // Проверяем, есть ли место в очереди
    if (this.queue.length >= this.maxSize) {
      return this.handleOverflow(message, priority);
    }

    // Есть место, добавляем сообщение
    this.queue.push({
      message,
      priority,
      timestamp: Date.now(),
      retryCount: 0,
    });

    // Сортируем по приоритету (меньше значение = выше приоритет)
    this.queue.sort((a, b) => {
      if (a.priority !== b.priority) {
        return a.priority - b.priority;
      }
      // Если приоритеты равны, старые сообщения первыми
      return a.timestamp - b.timestamp;
    });

    return true;
  }

  /**
   * Обработка переполнения очереди
   */
  private handleOverflow(message: unknown, priority: MessagePriority): boolean {
    switch (this.overflowStrategy) {
      case QueueOverflowStrategy.DROP_OLDEST:
        this.queue.shift(); // Удаляем самое старое
        this.droppedMessagesCount++;
        this.queue.push({
          message,
          priority,
          timestamp: Date.now(),
          retryCount: 0,
        });
        this.queue.sort((a, b) => {
          if (a.priority !== b.priority) return a.priority - b.priority;
          return a.timestamp - b.timestamp;
        });
        return true;

      case QueueOverflowStrategy.DROP_LOWEST:
        // Находим сообщение с самым низким приоритетом
        const lowestPriority = Math.max(...this.queue.map((q) => q.priority));
        if (lowestPriority > priority) {
          // Удаляем сообщения с низким приоритетом
          this.queue = this.queue.filter((q) => q.priority !== lowestPriority);
          this.droppedMessagesCount++;
          this.queue.push({
            message,
            priority,
            timestamp: Date.now(),
            retryCount: 0,
          });
          this.queue.sort((a, b) => {
            if (a.priority !== b.priority) return a.priority - b.priority;
            return a.timestamp - b.timestamp;
          });
          return true;
        }
        this.droppedMessagesCount++;
        return false;

      case QueueOverflowStrategy.REJECT:
        this.droppedMessagesCount++;
        return false;

      case QueueOverflowStrategy.BLOCK:
        // В реальной реализации здесь была бы блокировка
        // Для упрощения используем REJECT
        this.droppedMessagesCount++;
        return false;

      default:
        return false;
    }
  }

  /**
   * Извлечь сообщение из очереди (с наивысшим приоритетом)
   */
  dequeue(): unknown | null {
    if (this.queue.length === 0) {
      return null;
    }

    const queuedMessage = this.queue.shift();
    if (!queuedMessage) {
      return null;
    }
    const waitTime = Date.now() - queuedMessage.timestamp;
    this.waitTimes.push(waitTime);

    // Ограничиваем размер списка waitTimes для памяти
    if (this.waitTimes.length > 1000) {
      this.waitTimes.shift();
    }

    return queuedMessage.message;
  }

  /**
   * Получить размер очереди
   */
  size(): number {
    return this.queue.length;
  }

  /**
   * Проверить, пуста ли очередь
   */
  isEmpty(): boolean {
    return this.queue.length === 0;
  }

  /**
   * Очистить очередь
   */
  clear(): void {
    this.queue = [];
    this.waitTimes = [];
  }

  /**
   * Получить метрики очереди
   */
  getMetrics(): QueueMetrics {
    const messagesByPriority: Record<MessagePriority, number> = {
      [MessagePriority.CRITICAL]: 0,
      [MessagePriority.HIGH]: 0,
      [MessagePriority.NORMAL]: 0,
      [MessagePriority.LOW]: 0,
    };

    this.queue.forEach((q) => {
      messagesByPriority[q.priority]++;
    });

    const averageWaitTime =
      this.waitTimes.length > 0
        ? this.waitTimes.reduce((a, b) => a + b, 0) / this.waitTimes.length
        : 0;

    return {
      currentSize: this.queue.length,
      maxSize: this.maxSize,
      averageWaitTime: Math.round(averageWaitTime),
      droppedMessages: this.droppedMessagesCount,
      messagesByPriority,
    };
  }

  /**
   * Очистить старые сообщения (старше указанного времени)
   */
  clearOldMessages(maxAgeMillis: number): void {
    const now = Date.now();
    this.queue = this.queue.filter((q) => now - q.timestamp <= maxAgeMillis);
  }
}
