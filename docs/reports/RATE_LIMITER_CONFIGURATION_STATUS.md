# Rate Limiter Configuration Status

**Дата:** 2026-04-27  
**Статус:** ✅ COMPLETE

---

## ✅ Текущее состояние

Rate Limiter уже полностью настроен и интегрирован в проект!

---

## 📊 Архитектура

### 1. RateLimitMiddleware

**Файл:** `server/api/src/main/kotlin/com/company/ipcamera/server/middleware/RateLimitMiddleware.kt`

**Реализация:**
- Redis-based distributed rate limiting
- Sliding window алгоритм
- Multiple endpoint configurations

**Конфигурации:**

#### Login Configuration:
```kotlin
val loginConfig = RateLimitConfig(
    maxAttempts = 5,                    // 5 попыток
    windowDuration = 15.minutes,        // за 15 минут
    blockDuration = 30.minutes          // блокировка на 30 минут
)
```

#### General Configuration:
```kotlin
val generalConfig = RateLimitConfig(
    maxAttempts = 100,                  // 100 запросов
    windowDuration = 1.minute,          // за 1 минуту
    blockDuration = 5.minutes           // блокировка на 5 минут
)
```

#### Registration Configuration:
```kotlin
val registrationConfig = RateLimitConfig(
    maxAttempts = 3,                    // 3 попытки
    windowDuration = 60.minutes,        // за 60 минут
    blockDuration = 120.minutes         // блокировка на 2 часа
)
```

**Environment Variables:**
```bash
# Login limits
RATE_LIMIT_LOGIN_MAX_ATTEMPTS=5
RATE_LIMIT_LOGIN_WINDOW_MINUTES=15
RATE_LIMIT_LOGIN_BLOCK_MINUTES=30

# General limits
RATE_LIMIT_GENERAL_MAX_ATTEMPTS=100
RATE_LIMIT_GENERAL_WINDOW_MINUTES=1
RATE_LIMIT_GENERAL_BLOCK_MINUTES=5

# Registration limits
RATE_LIMIT_REGISTRATION_MAX_ATTEMPTS=3
RATE_LIMIT_REGISTRATION_WINDOW_MINUTES=60
RATE_LIMIT_REGISTRATION_BLOCK_MINUTES=120
```

---

### 2. Интеграция в Application.kt

#### DI Registration (AppModule.kt):
```kotlin
single<RateLimitMiddleware> {
    RateLimitMiddleware(get<RedisCoroutinesCommands<String, String>>())
}
```

#### Auth Routes Integration:
```kotlin
// POST /api/v1/auth/login
val rateLimiter: RateLimitMiddleware by inject()
val identifier = "login:$clientIp"

if (!call.checkRateLimit(identifier, rateLimiter, rateLimiter.loginConfig)) {
    return@post // 429 Too Many Requests
}

// После успешного входа
rateLimiter.resetLimit("login:$clientIp")
```

#### Global Rate Limiting (GlobalRateLimitMiddleware.kt):
```kotlin
install(GlobalRateLimitMiddleware) {
    // Применяется ко всем API endpoints
    intercept(ApplicationCallPipeline.Call) {
        val ip = call.request.local.remoteHost
        val identifier = "api_global:$ip"
        
        val rateLimiter = globalRateLimiter()
        if (!call.checkRateLimit(identifier, rateLimiter, rateLimiter.generalConfig)) {
            return@intercept
        }
    }
}
```

---

## 🔧 Redis Keys

### Rate Limit Keys:
```
rate_limit:{identifier}
```

**Примеры:**
- `rate_limit:login:192.168.1.100`
- `rate_limit:api_global:192.168.1.100`
- `rate_limit:registration:user123`

**Type:** Redis Sorted Set  
**Score:** Timestamp (milliseconds)  
**Member:** Unique identifier (timestamp + random)  
**TTL:** blockDuration (время блокировки)

---

## 📡 HTTP Headers

При превышении лимита возвращаются заголовки:

```
X-RateLimit-Limit: 5
X-RateLimit-Remaining: 0
X-RateLimit-Reset: 1682534400
```

**Пример ответа:**
```json
{
  "success": false,
  "data": null,
  "message": "Too many requests. Please try again later."
}
```

**Status Code:** `429 Too Many Requests`

---

## 🧪 Тестирование

### Unit Tests:
```kotlin
@Test
fun `test login rate limit exceeded`() = runTest {
    val rateLimiter = RateLimitMiddleware(mockRedis)
    
    repeat(6) {
        val (allowed, _) = rateLimiter.checkLimit(
            "login:192.168.1.1",
            rateLimiter.loginConfig
        )
    }
    
    assertFalse(allowed)
}

@Test
fun `test rate limit reset`() = runTest {
    val rateLimiter = RateLimitMiddleware(mockRedis)
    
    rateLimiter.checkLimit("login:192.168.1.1", rateLimiter.loginConfig)
    rateLimiter.resetLimit("login:192.168.1.1")
    
    val (allowed, info) = rateLimiter.checkLimit(
        "login:192.168.1.1",
        rateLimiter.loginConfig
    )
    
    assertTrue(allowed)
    assertEquals(5, info.remaining)
}
```

### Integration Tests:
```bash
# Test login rate limit
for i in {1..10}; do
  curl -X POST http://localhost:8080/api/v1/auth/login \
    -H "Content-Type: application/json" \
    -d '{"username":"test","password":"wrong"}'
done

# Expected: 429 after 5 attempts
```

---

## 🎯 Рекомендации по конфигурации

### Production:
```bash
# Более строгие лимиты для production
RATE_LIMIT_LOGIN_MAX_ATTEMPTS=3
RATE_LIMIT_LOGIN_WINDOW_MINUTES=10
RATE_LIMIT_LOGIN_BLOCK_MINUTES=60

RATE_LIMIT_GENERAL_MAX_ATTEMPTS=50
RATE_LIMIT_GENERAL_WINDOW_MINUTES=1
RATE_LIMIT_GENERAL_BLOCK_MINUTES=10

RATE_LIMIT_REGISTRATION_MAX_ATTEMPTS=2
RATE_LIMIT_REGISTRATION_WINDOW_MINUTES=60
RATE_LIMIT_REGISTRATION_BLOCK_MINUTES=240
```

### Development:
```bash
# Более мягкие лимиты для разработки
RATE_LIMIT_LOGIN_MAX_ATTEMPTS=10
RATE_LIMIT_LOGIN_WINDOW_MINUTES=30
RATE_LIMIT_LOGIN_BLOCK_MINUTES=5

RATE_LIMIT_GENERAL_MAX_ATTEMPTS=200
RATE_LIMIT_GENERAL_WINDOW_MINUTES=1
RATE_LIMIT_GENERAL_BLOCK_MINUTES=2
```

---

## 🚀 Дополнительные улучшения (рекомендуется)

### 1. User-based rate limiting:
```kotlin
// Для authenticated пользователей
val userIdentifier = "user:${user.id}"
val (allowed, _) = rateLimiter.checkLimit(userIdentifier, generalConfig)
```

### 2. Endpoint-specific configs:
```kotlin
val uploadConfig = RateLimitConfig(
    maxAttempts = 10,
    windowDuration = 10.minutes,
    blockDuration = 30.minutes
)

post("/upload") {
    val identifier = "upload:${clientIp}"
    if (!call.checkRateLimit(identifier, rateLimiter, uploadConfig)) {
        return@post
    }
    // ...
}
```

### 3. Dynamic configuration:
```kotlin
// Загрузка конфигурации из DB/Redis
suspend fun loadRateLimitConfig(): RateLimitConfig {
    val configJson = redisCommands.get("rate_limit_config")
    return Json.decodeFromString(configJson)
}
```

### 4. Rate limit metrics:
```kotlin
data class RateLimitMetrics(
    val totalRequests: Long,
    val blockedRequests: Long,
    val blockRate: Double
)
```

---

## 📊 Мониторинг

### Prometheus Metrics (рекомендуется добавить):
```kotlin
val rateLimitCounter = Counter.build()
    .name("rate_limit_requests_total")
    .labelNames("endpoint", "ip")
    .help("Total rate limited requests")
    .register()

val rateLimitExceededCounter = Counter.build()
    .name("rate_limit_exceeded_total")
    .labelNames("endpoint", "ip")
    .help("Rate limit exceeded events")
    .register()
```

### Logging:
```kotlin
logger.warn {
    "Rate limit exceeded: identifier=$identifier, " +
    "attempts=$attemptCount/${config.maxAttempts}, " +
    "endpoint=$endpoint"
}
```

---

## 🔐 Security Considerations

### 1. Fail-open vs Fail-closed:
**Текущая реализация:** Fail-open (в случае Redis ошибки разрешаем запросы)
```kotlin
catch (e: Exception) {
    logger.error(e) { "Error checking rate limit" }
    Pair(true, RateLimitInfo(...)) // allow request
}
```

**Рекомендация:** Для production можно использовать fail-closed:
```kotlin
catch (e: Exception) {
    logger.error(e) { "Error checking rate limit" }
    Pair(false, RateLimitInfo(...)) // block request
}
```

### 2. IP spoofing protection:
```kotlin
// Использовать X-Forwarded-For для проксированных запросов
val clientIp = call.request.headers["X-Forwarded-For"]
    ?: call.request.local.remoteHost
```

### 3. Distributed systems:
Redis-based реализация уже поддерживает распределенные системы!

---

## ✅ Summary

**Статус:** ✅ COMPLETE  
**Время выполнения:** N/A (уже реализовано)  
**Файлы:**
- `server/api/src/main/kotlin/com/company/ipcamera/server/middleware/RateLimitMiddleware.kt`
- `server/api/src/main/kotlin/com/company/ipcamera/server/middleware/GlobalRateLimitMiddleware.kt`
- `server/api/src/main/kotlin/com/company/ipcamera/server/di/AppModule.kt`
- `server/api/src/main/kotlin/com/company/ipcamera/server/routing/AuthRoutes.kt`

**Функциональность:**
- ✅ Redis-based distributed rate limiting
- ✅ Multiple endpoint configurations (login, general, registration)
- ✅ Sliding window algorithm
- ✅ HTTP headers (X-RateLimit-*)
- ✅ Integration with Auth routes
- ✅ Global API protection
- ✅ Environment variable configuration
- ✅ Fail-open fallback

---

**Документация создана:** 2026-04-27  
**Последнее обновление:** 2026-04-27
