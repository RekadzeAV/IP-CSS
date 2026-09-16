/**
 * Конфигурация rate limiting
 */
export interface RateLimitConfig {
  messagesPerSecond: number;
  subscriptionsPerSecond: number;
  bytesPerSecond: number;
  windowSizeMillis: number;
  enabled: boolean;
}

/**
 * Нарушение rate limit
 */
export interface RateLimitViolation {
  timestamp: number;
  operationType: string;
  limitType: string;
  currentRate: number;
  limit: number;
}

/**
 * Метрики rate limiting
 */
export interface RateLimitMetrics {
  blockedRequests: number;
  currentMessageRate: number;
  currentSubscriptionRate: number;
  currentBytesRate: number;
  violations: RateLimitViolation[];
}

/**
 * Token Bucket для rate limiting
 */
class TokenBucket {
  private tokens: number;
  private capacity: number;
  private refillRate: number; // Токенов в секунду
  private lastRefillTime: number;
  private consumed = 0;
  private blocked = 0;

  constructor(capacity: number, refillRate: number) {
    this.capacity = capacity;
    this.refillRate = refillRate;
    this.tokens = capacity;
    this.lastRefillTime = Date.now();
  }

  /**
   * Попытаться потребить токены
   */
  tryConsume(amount: number = 1.0): boolean {
    this.refill();

    if (this.tokens >= amount) {
      this.tokens -= amount;
      this.consumed++;
      return true;
    } else {
      this.blocked++;
      return false;
    }
  }

  /**
   * Пополнить токены
   */
  private refill(): void {
    const now = Date.now();
    const elapsed = (now - this.lastRefillTime) / 1000.0; // в секундах

    if (elapsed > 0) {
      const tokensToAdd = elapsed * this.refillRate;
      this.tokens = Math.min(this.capacity, this.tokens + tokensToAdd);
      this.lastRefillTime = now;
    }
  }

  /**
   * Получить текущее количество токенов
   */
  getCurrentTokens(): number {
    this.refill();
    return this.tokens;
  }

  /**
   * Получить текущую скорость потребления
   */
  getCurrentRate(): number {
    const elapsed = (Date.now() - this.lastRefillTime) / 1000.0;
    return elapsed > 0 ? this.consumed / elapsed : 0;
  }

  /**
   * Сбросить счетчики
   */
  reset(): void {
    this.tokens = this.capacity;
    this.lastRefillTime = Date.now();
    this.consumed = 0;
    this.blocked = 0;
  }

  getBlockedCount(): number {
    return this.blocked;
  }
}

/**
 * Rate Limiter с алгоритмом Token Bucket
 */
export class RateLimiter {
  private messageBucket: TokenBucket;
  private subscriptionBucket: TokenBucket;
  private bytesBucket: TokenBucket;
  private config: RateLimitConfig;
  private violations: RateLimitViolation[] = [];

  constructor(config: RateLimitConfig) {
    this.config = config;
    this.messageBucket = new TokenBucket(
      config.messagesPerSecond,
      config.messagesPerSecond
    );
    this.subscriptionBucket = new TokenBucket(
      config.subscriptionsPerSecond,
      config.subscriptionsPerSecond
    );
    this.bytesBucket = new TokenBucket(
      config.bytesPerSecond,
      config.bytesPerSecond
    );
  }

  /**
   * Проверить лимит для отправки сообщения
   */
  checkMessageLimit(): boolean {
    if (!this.config.enabled) return true;

    const allowed = this.messageBucket.tryConsume(1.0);
    if (!allowed) {
      this.recordViolation(
        'message',
        'messages_per_second',
        this.messageBucket.getCurrentRate(),
        this.config.messagesPerSecond
      );
      console.warn(
        `[RateLimiter] Message rate limit exceeded: ${this.messageBucket.getCurrentRate()}/${this.config.messagesPerSecond} msg/s`
      );
    }
    return allowed;
  }

  /**
   * Проверить лимит для подписки/отписки
   */
  checkSubscriptionLimit(): boolean {
    if (!this.config.enabled) return true;

    const allowed = this.subscriptionBucket.tryConsume(1.0);
    if (!allowed) {
      this.recordViolation(
        'subscription',
        'subscriptions_per_second',
        this.subscriptionBucket.getCurrentRate(),
        this.config.subscriptionsPerSecond
      );
      console.warn(
        `[RateLimiter] Subscription rate limit exceeded: ${this.subscriptionBucket.getCurrentRate()}/${this.config.subscriptionsPerSecond} ops/s`
      );
    }
    return allowed;
  }

  /**
   * Проверить лимит для размера сообщения
   */
  checkBytesLimit(size: number): boolean {
    if (!this.config.enabled) return true;

    const allowed = this.bytesBucket.tryConsume(size);
    if (!allowed) {
      this.recordViolation(
        'bytes',
        'bytes_per_second',
        this.bytesBucket.getCurrentRate(),
        this.config.bytesPerSecond
      );
      console.warn(
        `[RateLimiter] Bytes rate limit exceeded: ${this.bytesBucket.getCurrentRate()}/${this.config.bytesPerSecond} bytes/s`
      );
    }
    return allowed;
  }

  /**
   * Записать нарушение лимита
   */
  private recordViolation(
    operationType: string,
    limitType: string,
    currentRate: number,
    limit: number
  ): void {
    this.violations.push({
      timestamp: Date.now(),
      operationType,
      limitType,
      currentRate,
      limit,
    });

    // Ограничиваем размер списка нарушений
    if (this.violations.length > 1000) {
      this.violations.shift();
    }
  }

  /**
   * Получить метрики
   */
  getMetrics(): RateLimitMetrics {
    return {
      blockedRequests:
        this.messageBucket.getBlockedCount() +
        this.subscriptionBucket.getBlockedCount() +
        this.bytesBucket.getBlockedCount(),
      currentMessageRate: this.messageBucket.getCurrentRate(),
      currentSubscriptionRate: this.subscriptionBucket.getCurrentRate(),
      currentBytesRate: this.bytesBucket.getCurrentRate(),
      violations: [...this.violations],
    };
  }

  /**
   * Сбросить лимиты
   */
  reset(): void {
    this.messageBucket.reset();
    this.subscriptionBucket.reset();
    this.bytesBucket.reset();
    this.violations = [];
  }
}
