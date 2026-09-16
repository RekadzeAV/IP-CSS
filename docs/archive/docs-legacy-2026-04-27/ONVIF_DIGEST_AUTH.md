# ONVIF Digest Authentication

**Версия:** 1.1
**Дата:** 26 January 2026
**Статус:** ✅ Реализовано (базовая версия, требуется тестирование)

> **📚 Связанные документы:**
> - [ONVIF_CLIENT.md](ONVIF_CLIENT.md) - Основная документация ONVIF клиента
> - [ONVIF_CLIENT_IMPLEMENTATION_PLAN.md](ONVIF_CLIENT_IMPLEMENTATION_PLAN.md) - План реализации

---

## Обзор

Digest Authentication - это метод аутентификации, который более безопасен, чем Basic Authentication, так как пароль не передается в открытом виде. Многие производители камер (Hikvision, Dahua, Bosch) требуют Digest Authentication для работы с ONVIF.

## Реализация

✅ **Digest Authentication реализован** в ONVIF клиенте. Клиент автоматически определяет тип аутентификации и переключается с Basic на Digest при необходимости.

### Реализованные возможности

- ✅ Парсинг WWW-Authenticate заголовка
- ✅ Генерация Digest Authorization заголовка
- ✅ Поддержка MD5 и SHA-256 алгоритмов
- ✅ Поддержка MD5-sess и SHA-256-sess
- ✅ Поддержка qop (auth, auth-int)
- ✅ Обработка stale nonce
- ✅ Автоматическое переключение с Basic на Digest
- ✅ Кэширование Digest параметров
- ✅ Unit тесты

### Требуется доработка

- 🟡 Тестирование с реальными камерами (Hikvision, Dahua, Bosch)
- 🟡 Документация использования

### Пример использования

```kotlin
val capabilities = onvifClient.getCapabilities(
    url = "http://192.168.1.100",
    username = "admin",
    password = "password123"
)
// Клиент автоматически определит тип аутентификации (Basic/Digest)
// и переключится на Digest, если камера требует его
```

### Как это работает

1. Клиент отправляет запрос с Basic Authentication
2. Если камера отвечает 401 с WWW-Authenticate заголовком, клиент автоматически:
   - Парсит Digest параметры
   - Генерирует Digest Authorization заголовок
   - Повторяет запрос с Digest Authentication
3. Digest параметры кэшируются для последующих запросов

## Как работает Digest Authentication

### Алгоритм

1. **Клиент отправляет запрос без аутентификации**
   ```
   GET /onvif/device_service HTTP/1.1
   ```

2. **Сервер отвечает 401 с WWW-Authenticate заголовком**
   ```
   HTTP/1.1 401 Unauthorized
   WWW-Authenticate: Digest realm="IP Camera", nonce="abc123", qop="auth", algorithm=MD5
   ```

3. **Клиент вычисляет response:**
   - `HA1 = MD5(username:realm:password)`
   - `HA2 = MD5(method:uri)`
   - `response = MD5(HA1:nonce:nc:cnonce:qop:HA2)`

4. **Клиент отправляет запрос с Authorization заголовком**
   ```
   GET /onvif/device_service HTTP/1.1
   Authorization: Digest username="admin", realm="IP Camera", nonce="abc123",
                  uri="/onvif/device_service", response="xyz789...",
                  qop=auth, nc=00000001, cnonce="def456"
   ```

### Параметры WWW-Authenticate

- **realm** - область аутентификации
- **nonce** - одноразовое число от сервера
- **qop** - качество защиты (auth, auth-int)
- **algorithm** - алгоритм хеширования (MD5, SHA-256)

### Параметры Authorization

- **username** - имя пользователя
- **realm** - область аутентификации (из WWW-Authenticate)
- **nonce** - одноразовое число (из WWW-Authenticate)
- **uri** - URI запроса
- **response** - вычисленный ответ
- **qop** - качество защиты (из WWW-Authenticate)
- **nc** - счетчик запросов (nonce count)
- **cnonce** - клиентское одноразовое число

## Примеры использования (после реализации)

### Автоматическое определение типа аутентификации

```kotlin
val capabilities = onvifClient.getCapabilities(
    url = "http://192.168.1.100",
    username = "admin",
    password = "password123"
)
// Клиент автоматически определит тип аутентификации (Basic/Digest)
// и выполнит соответствующий алгоритм
```

### Принудительное использование Digest

```kotlin
val capabilities = onvifClient.getCapabilities(
    url = "http://192.168.1.100",
    username = "admin",
    password = "password123",
    authType = AuthType.DIGEST // Опционально
)
```

## Поддерживаемые алгоритмы

### MD5 (по умолчанию)
- Наиболее распространен
- Поддерживается всеми камерами

### SHA-256
- Более безопасный
- Поддерживается новыми камерами

## Поддерживаемые qop значения

### auth
- Аутентификация без проверки целостности
- Наиболее распространен

### auth-int
- Аутентификация с проверкой целостности
- Редко используется

## Временные решения

### Решение 1: Использование Basic Authentication

Если камера поддерживает оба типа аутентификации, можно использовать Basic:

```kotlin
// Некоторые камеры позволяют переключиться на Basic Auth
// через веб-интерфейс камеры
```

### Решение 2: Использование учетных данных с Basic Auth

Создайте отдельного пользователя с поддержкой Basic Authentication:

```kotlin
// В веб-интерфейсе камеры создайте пользователя с Basic Auth
val capabilities = onvifClient.getCapabilities(
    url = "http://192.168.1.100",
    username = "basic_user", // Пользователь с Basic Auth
    password = "password123"
)
```

### Решение 3: Ожидание реализации

Дождитесь реализации Digest Authentication согласно [плану](ONVIF_CLIENT_IMPLEMENTATION_PLAN.md#этап-1-digest-authentication-критично-для-mvp).

## Производители камер, требующие Digest Auth

- ✅ **Hikvision** - требует Digest Authentication
- ✅ **Dahua** - требует Digest Authentication
- ✅ **Bosch** - требует Digest Authentication
- ⚠️ **Axis** - поддерживает Basic и Digest
- ⚠️ **Sony** - поддерживает Basic и Digest

## Тестирование

После реализации будут доступны тесты:

```kotlin
// Unit тесты
class DigestAuthHelperTest {
    @Test
    fun testGenerateHA1() { ... }

    @Test
    fun testGenerateHA2() { ... }

    @Test
    fun testGenerateResponse() { ... }
}

// Integration тесты
class OnvifClientDigestAuthTest {
    @Test
    fun testDigestAuthWithHikvision() { ... }

    @Test
    fun testDigestAuthWithDahua() { ... }
}
```

## Спецификации

- **RFC 2617** - HTTP Digest Authentication
- **ONVIF Core Specification** - требования к аутентификации
- **WS-Security** - стандарт безопасности для веб-сервисов

## Статус реализации

| Компонент | Статус | Приоритет |
|-----------|--------|-----------|
| DigestAuthHelper | ✅ Реализовано | 🔴 Высокий |
| Интеграция в OnvifClient | ✅ Реализовано | 🔴 Высокий |
| Поддержка MD5 | ✅ Реализовано | 🔴 Высокий |
| Поддержка SHA-256 | ✅ Реализовано | 🟡 Средний |
| Поддержка MD5-sess/SHA-256-sess | ✅ Реализовано | 🟡 Средний |
| Поддержка qop=auth-int | ✅ Реализовано | 🟢 Низкий |
| Обработка stale nonce | ✅ Реализовано | 🔴 Высокий |
| Unit тесты | ✅ Реализовано | 🔴 Высокий |
| Тестирование с реальными камерами | 🟡 Требуется | 🔴 Высокий |

**Прогресс:** 85% ✅

---

**Последнее обновление:** 26 January 2026
**Статус:** ✅ Базовая реализация завершена, требуется тестирование с реальными камерами
