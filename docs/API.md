# API Документация

**Версия проекта:** Alfa-0.0.1
**Последнее обновление:** 26 January 2026

> **📚 Полный индекс документации:** [DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)

## Базовый URL
```
https://api.company.com/v1
или
http://localhost:8080/api/v1
```

## Аутентификация

Все запросы (кроме публичных endpoints) требуют аутентификации через JWT токен.

> **🔒 Безопасность:** Токены хранятся в httpOnly cookies для защиты от XSS атак. Токены автоматически отправляются с каждым запросом.

### Получение токена:
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}
```

**Ответ:**
```json
{
  "success": true,
  "data": {
    "accessToken": "",
    "refreshToken": "",
    "user": {
      "id": "user-001",
      "username": "admin",
      "email": "admin@example.com",
      "role": "ADMIN",
      "permissions": ["*"]
    }
  },
  "message": "Login successful"
}
```

> **⚠️ Важно:** Токены не возвращаются в теле ответа для безопасности. Они автоматически устанавливаются в httpOnly cookies сервером.

**Cookies, устанавливаемые сервером:**
- `access_token` - JWT access token (15 минут жизни, httpOnly, secure в production)
- `refresh_token` - JWT refresh token (7 дней жизни, httpOnly, secure в production)

### Использование токена:

Токены автоматически отправляются с каждым запросом через cookies. Клиент должен использовать `withCredentials: true`:

**JavaScript/TypeScript:**
```typescript
const response = await fetch('/api/v1/cameras', {
  credentials: 'include'  // Отправляет cookies автоматически
});
```

**Axios:**
```typescript
const response = await axios.get('/api/v1/cameras', {
  withCredentials: true  // Отправляет cookies автоматически
});
```

**Для обратной совместимости** также поддерживается заголовок Authorization:
```http
GET /api/v1/cameras
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

> **Рекомендация:** Используйте cookies для максимальной безопасности. Заголовок Authorization поддерживается только для обратной совместимости.

### Обновление токена:
```http
POST /api/v1/auth/refresh
Content-Type: application/json
```

> **Примечание:** Refresh token автоматически берется из cookie. Тело запроса может быть пустым или содержать refreshToken для обратной совместимости.

**Ответ:**
```json
{
  "success": true,
  "data": {
    "accessToken": "",
    "refreshToken": ""
  },
  "message": "Token refreshed successfully"
}
```

Новые токены автоматически устанавливаются в cookies.

### Выход из системы:
```http
POST /api/v1/auth/logout
```

Удаляет все cookies с токенами и отзывает refresh token.

### Статус внешних провайдеров auth (admin):
```http
GET /api/v1/auth/providers/status
Authorization: Bearer <jwt-admin-token>
```

Возвращает безопасный статус внешних провайдеров (например, `ldap`, `kerberos`) без секретов:
- `enabled` — включён ли провайдер по конфигурации;
- `implemented` — реализован ли провайдер полностью;
- `mode` — режим работы (`active_or_fallback` или `stub`);
- `reachable` — доступен ли провайдер на момент проверки;
- `statusMessage` — безопасная диагностическая метка (`ok`, `disabled`, `missing_realm_or_kdc`, `configured_but_not_implemented`, `unreachable_or_invalid_config`).

## Endpoints

### Камеры (`/api/v1/cameras`)

#### Получить список камер
```http
GET /api/v1/cameras?page=1&limit=20&status=ONLINE
```

**Параметры запроса:**
- `page` (int, опционально) - номер страницы (по умолчанию: 1)
- `limit` (int, опционально) - количество элементов на странице (по умолчанию: 20)
- `status` (string, опционально) - фильтр по статусу (ONLINE, OFFLINE, ERROR, CONNECTING, UNKNOWN)

**Ответ:**
```json
{
  "items": [
    {
      "id": "cam-001",
      "name": "Камера 1",
      "url": "rtsp://192.168.1.101:554/stream1",
      "status": "ONLINE",
      "resolution": {
        "width": 1920,
        "height": 1080
      },
      "fps": 25,
      "bitrate": 4096,
      "codec": "H.264",
      "audio": true,
      "createdAt": 1642683600000,
      "updatedAt": 1642683600000
    }
  ],
  "total": 10,
  "page": 1,
  "limit": 20,
  "hasMore": false
}
```

#### Получить камеру по ID
```http
GET /api/v1/cameras/{id}
```

#### Сводка наблюдения (px/m, предупреждение по разрешению сцены)
```http
GET /api/v1/cameras/{id}/observation-summary
```

Роль не ниже **VIEWER**. Ответ: `ApiResponse` с полем `data` типа `CameraObservationSummaryDto` — вычисленные px/m (если заданы геометрия сцены и целевой класс зоны), целевой минимум px/m из настроек и флаг `lowResolutionWarning`.

**Пример:**
```json
{
  "success": true,
  "data": {
    "computedPixelsPerMeter": 12.4,
    "targetPixelsPerMeterMin": 10.0,
    "lowResolutionWarning": false
  },
  "message": "Observation summary computed"
}
```

#### Добавить камеру
```http
POST /api/v1/cameras
Content-Type: application/json

{
  "name": "Новая камера",
  "url": "rtsp://192.168.1.101:554/stream1",
  "username": "admin",
  "password": "camera123",
  "model": "Hikvision DS-2CD2342WD-I",
  "resolution": {
    "width": 1920,
    "height": 1080
  },
  "fps": 25,
  "bitrate": 4096,
  "codec": "H.264",
  "audio": true,
  "ptz": {
    "enabled": true,
    "type": "PTZ",
    "presets": ["Home", "Position1"]
  }
}
```

#### Обновить камеру
```http
PUT /api/v1/cameras/{id}
Content-Type: application/json

{
  "name": "Обновленное имя",
  "fps": 30
}
```

#### Удалить камеру
```http
DELETE /api/v1/cameras/{id}
```

#### Протестировать подключение к камере
```http
POST /api/v1/cameras/{id}/test
```

**Ответ:**
```json
{
  "success": true,
  "data": {
    "status": "connected",
    "latency": "45ms",
    "resolution": "1920x1080"
  }
}
```

#### Управление камерой (PTZ, запись и т.д.)
```http
POST /api/v1/cameras/{id}/control
Content-Type: application/json

{
  "action": "ptz_move",
  "parameters": {
    "direction": "right",
    "speed": 50
  }
}
```

**Доступные действия:**
- `ptz_move` - движение PTZ камеры
- `ptz_preset` - переход к пресету
- `start_recording` - начать запись
- `stop_recording` - остановить запись
- `snapshot` - сделать снимок

#### Получить статус камеры
```http
GET /api/v1/cameras/{id}/status
```

### Записи (`/api/v1/recordings`)

#### Получить список записей
```http
GET /api/v1/recordings?camera_id=cam-001&start_time=1642683600000&end_time=1642770000000&page=1&limit=20
```

**Параметры запроса:**
- `camera_id` (string, опционально) - фильтр по ID камеры
- `start_time` (long, опционально) - начальное время (timestamp в миллисекундах)
- `end_time` (long, опционально) - конечное время (timestamp в миллисекундах)
- `page` (int, опционально) - номер страницы
- `limit` (int, опционально) - количество элементов на странице

**Ответ:**
```json
{
  "items": [
    {
      "id": "rec-001",
      "cameraId": "cam-001",
      "cameraName": "Камера 1",
      "startTime": 1642683600000,
      "endTime": 1642687200000,
      "duration": 3600,
      "filePath": "/recordings/rec-001.mp4",
      "fileSize": 104857600,
      "codec": "H.265",
      "format": "mp4",
      "quality": "HIGH",
      "status": "COMPLETED",
      "thumbnailUrl": "/thumbnails/rec-001.jpg",
      "createdAt": 1642683600000
    }
  ],
  "total": 50,
  "page": 1,
  "limit": 20,
  "hasMore": true
}
```

#### Получить запись по ID
```http
GET /api/v1/recordings/{id}
```

#### Паспорт записи (декларируемый и фактический кодек/параметры файла)
```http
GET /api/v1/recordings/{id}/passport
```

**Ответ:**
```json
{
  "success": true,
  "data": {
    "recordingId": "rec-001",
    "cameraId": "cam-001",
    "declaredFormat": "MP4",
    "declaredQuality": "HIGH",
    "status": "COMPLETED",
    "declaredCodec": "H.265",
    "actualCodec": "hevc",
    "width": 1920,
    "height": 1080,
    "durationSeconds": "12.341000",
    "bitrate": "8000000",
    "fileSizeBytes": 104857600
  },
  "message": "Recording passport retrieved successfully"
}
```

#### Начать запись
```http
POST /api/v1/recordings/start
Content-Type: application/json

{
  "cameraId": "cam-001",
  "duration": 300,
  "quality": "HIGH",
  "format": "mp4"
}
```

**Ответ:**
```json
{
  "recordingId": "rec-002",
  "cameraId": "cam-001",
  "startTime": 1642683600000,
  "estimatedEndTime": 1642683900000
}
```

#### Остановить запись
```http
POST /api/v1/recordings/{id}/stop
```

#### Удалить запись
```http
DELETE /api/v1/recordings/{id}
```

#### Экспортировать запись
```http
POST /api/v1/recordings/{id}/export
Content-Type: application/json

{
  "format": "mp4",
  "quality": "medium",
  "startTime": 1642683600000,
  "endTime": 1642687200000
}
```

**Ответ:**
```json
{
  "exportId": "exp-001",
  "downloadUrl": "/exports/exp-001.mp4",
  "expiresAt": 1642770000000
}
```

#### Получить URL для скачивания записи
```http
GET /api/v1/recordings/{id}/download
```

### События (`/api/v1/events`)

#### Получить список событий
```http
GET /api/v1/events?type=motion&camera_id=cam-001&severity=WARNING&acknowledged=false&page=1&limit=20
```

**Параметры запроса:**
- `type` (string, опционально) - тип события (motion, object_detection, face_detection, etc.)
- `camera_id` (string, опционально) - фильтр по ID камеры
- `severity` (string, опционально) - важность (INFO, WARNING, ERROR, CRITICAL)
- `acknowledged` (boolean, опционально) - фильтр по статусу подтверждения
- `start_time` (long, опционально) - начальное время
- `end_time` (long, опционально) - конечное время
- `page` (int, опционально) - номер страницы
- `limit` (int, опционально) - количество элементов на странице

**Ответ:**
```json
{
  "items": [
    {
      "id": "evt-001",
      "cameraId": "cam-001",
      "cameraName": "Камера 1",
      "type": "motion",
      "severity": "WARNING",
      "timestamp": 1642683600000,
      "description": "Обнаружено движение",
      "metadata": {
        "zone": "Zone1",
        "confidence": "0.95"
      },
      "acknowledged": false,
      "thumbnailUrl": "/thumbnails/evt-001.jpg",
      "videoUrl": "/videos/evt-001.mp4"
    }
  ],
  "total": 100,
  "page": 1,
  "limit": 20,
  "hasMore": true
}
```

#### Получить событие по ID
```http
GET /api/v1/events/{id}
```

#### Подтвердить событие
```http
POST /api/v1/events/{id}/acknowledge
```

**Ответ:**
```json
{
  "success": true,
  "message": "Событие подтверждено"
}
```

#### Подтвердить несколько событий
```http
POST /api/v1/events/acknowledge
Content-Type: application/json

{
  "ids": ["evt-001", "evt-002", "evt-003"]
}
```

#### Удалить событие
```http
DELETE /api/v1/events/{id}
```

#### Получить статистику событий
```http
GET /api/v1/events/statistics?camera_id=cam-001&start_time=1642683600000&end_time=1642770000000
```

**Ответ:**
```json
{
  "success": true,
  "data": {
    "total": 150,
    "byType": {
      "motion": 100,
      "object_detection": 30,
      "face_detection": 20
    },
    "bySeverity": {
      "INFO": 50,
      "WARNING": 70,
      "ERROR": 20,
      "CRITICAL": 10
    }
  }
}
```

### Пользователи (`/api/v1/users`)

#### Вход в систему
```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}
```

#### Регистрация нового пользователя
```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "newuser",
  "email": "user@example.com",
  "password": "password123",
  "fullName": "Новый Пользователь"
}
```

#### Выход из системы
```http
POST /api/v1/auth/logout
```

#### Получить информацию о текущем пользователе
```http
GET /api/v1/users/me
```

**Ответ:**
```json
{
  "id": "user-001",
  "username": "admin",
  "email": "admin@example.com",
  "fullName": "Администратор",
  "role": "ADMIN",
  "permissions": ["read", "write", "delete", "admin"],
  "createdAt": 1642683600000,
  "lastLoginAt": 1642683600000,
  "isActive": true
}
```

#### Обновить профиль текущего пользователя
```http
PUT /api/v1/users/me
Content-Type: application/json

{
  "email": "newemail@example.com",
  "fullName": "Новое Имя",
  "password": "newpassword123"
}
```

#### Получить список пользователей (только для администраторов)
```http
GET /api/v1/users?page=1&limit=20&role=USER
```

#### Получить пользователя по ID (только для администраторов)
```http
GET /api/v1/users/{id}
```

#### Обновить пользователя (только для администраторов)
```http
PUT /api/v1/users/{id}
Content-Type: application/json

{
  "email": "updated@example.com",
  "fullName": "Обновленное Имя"
}
```

#### Удалить пользователя (только для администраторов)
```http
DELETE /api/v1/users/{id}
```

### Настройки (`/api/v1/settings`)

> **⚠️ Примечание:** API для управления лицензиями временно отключен. Функционал лицензирования вынесен за рамки проекта и будет реализован в отдельной доработке.

#### Получить все настройки
```http
GET /api/v1/settings?category=recording
```

**Параметры запроса:**
- `category` (string, опционально) - фильтр по категории

**Ответ:**
```json
[
  {
    "id": "setting-001",
    "category": "recording",
    "key": "default_quality",
    "value": "HIGH",
    "type": "string",
    "description": "Качество записи по умолчанию",
    "updatedAt": 1642683600000
  }
]
```

#### Получить настройку по ключу
```http
GET /api/v1/settings/{key}
```

#### Обновить настройки
```http
PUT /api/v1/settings
Content-Type: application/json

{
  "settings": {
    "default_quality": "ULTRA",
    "max_storage_size": "1000000000"
  }
}
```

**Ответ:**
```json
{
  "success": true,
  "updated": 2,
  "message": "Настройки обновлены"
}
```

#### Обновить одну настройку
```http
PUT /api/v1/settings/{key}
Content-Type: application/json

{
  "value": "ULTRA"
}
```

#### Удалить настройку
```http
DELETE /api/v1/settings/{key}
```

#### Получить системные настройки
```http
GET /api/v1/settings/system
```

**Ответ:**
```json
{
  "recording": {
    "defaultQuality": "HIGH",
    "defaultFormat": "mp4",
    "maxDuration": 3600,
    "autoDelete": true,
    "retentionDays": 30
  },
  "storage": {
    "maxStorageSize": 1000000000000,
    "currentStorageUsed": 50000000000,
    "storagePath": "/recordings",
    "autoCleanup": true
  },
  "notifications": {
    "emailEnabled": false,
    "smsEnabled": false,
    "pushEnabled": true,
    "webhookUrl": null
  },
  "security": {
    "requireAuth": true,
    "sessionTimeout": 3600,
    "passwordPolicy": {
      "minLength": 8,
      "requireUppercase": true,
      "requireLowercase": true,
      "requireNumbers": true,
      "requireSpecialChars": false
    }
  },
  "network": {
    "apiPort": 8080,
    "websocketPort": 8081,
    "allowRemoteAccess": false,
    "sslEnabled": false
  }
}
```

#### Обновить системные настройки
```http
PUT /api/v1/settings/system
Content-Type: application/json

{
  "recording": {
    "defaultQuality": "ULTRA",
    "retentionDays": 60
  }
}
```

#### Сбросить настройки к значениям по умолчанию
```http
POST /api/v1/settings/reset?category=recording
```

#### Экспортировать настройки
```http
GET /api/v1/settings/export
```

**Ответ:**
```json
{
  "success": true,
  "data": {
    "default_quality": "HIGH",
    "max_storage_size": "1000000000"
  }
}
```

#### Импортировать настройки
```http
POST /api/v1/settings/import
Content-Type: application/json

{
  "settings": {
    "default_quality": "HIGH",
    "max_storage_size": "1000000000"
  }
}
```

## WebSocket API

WebSocket сервер реализован и поддерживает real-time коммуникацию для получения обновлений о камерах, событиях, записях и уведомлениях.

### Подключение
```
wss://api.company.com/api/v1/ws
или
ws://localhost:8080/api/v1/ws
```

### Аутентификация

После подключения к WebSocket необходимо выполнить аутентификацию с помощью JWT токена:

```json
{
  "type": "auth",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**Ответ при успешной аутентификации:**
```json
{
  "type": "auth_response",
  "data": {
    "success": true,
    "message": "Authenticated successfully"
  }
}
```

**Ответ при ошибке аутентификации:**
```json
{
  "type": "auth_response",
  "data": {
    "success": false,
    "message": "Invalid token"
  }
}
```

### Подписка на каналы

После аутентификации можно подписаться на каналы:

```json
{
  "type": "subscribe",
  "data": {
    "channels": ["cameras", "events", "recordings", "notifications"],
    "filters": {
      "camera_ids": ["cam-001", "cam-002"]
    }
  }
}
```

**Доступные каналы:**
- `cameras` - обновления статуса камер
- `events` - новые события
- `recordings` - обновления записей
- `notifications` - системные уведомления

**Ответ при успешной подписке:**
```json
{
  "type": "subscribe_response",
  "data": {
    "success": true,
    "channels": ["cameras", "events"],
    "message": "Subscribed successfully"
  }
}
```

### Отписка от каналов

```json
{
  "type": "unsubscribe",
  "data": {
    "channels": ["events"]
  }
}
```

**Ответ при успешной отписке:**
```json
{
  "type": "unsubscribe_response",
  "data": {
    "success": true,
    "channels": ["events"],
    "message": "Unsubscribed successfully"
  }
}
```

### Получение сообщений

После подписки на каналы, сервер будет отправлять сообщения в формате:

#### Обновление статуса камеры
```json
{
  "type": "event",
  "channel": "cameras",
  "data": {
    "cameraId": "cam-001",
    "status": "ONLINE",
    "timestamp": 1642683600000
  }
}
```

#### Новое событие
```json
{
  "type": "event",
  "channel": "events",
  "data": {
    "id": "evt-001",
    "cameraId": "cam-001",
    "cameraName": "Камера 1",
    "type": "motion",
    "severity": "WARNING",
    "timestamp": 1642683600000,
    "description": "Обнаружено движение",
    "metadata": {
      "zone": "Zone1",
      "confidence": 0.95
    },
    "acknowledged": false
  }
}
```

#### Обновление записи
```json
{
  "type": "event",
  "channel": "recordings",
  "data": {
    "id": "rec-001",
    "cameraId": "cam-001",
    "status": "COMPLETED",
    "duration": 3600,
    "fileSize": 104857600
  }
}
```

#### Системное уведомление
```json
{
  "type": "event",
  "channel": "notifications",
  "data": {
    "id": "notif-001",
    "type": "system",
    "severity": "INFO",
    "title": "Системное уведомление",
    "message": "Камера cam-001 восстановлена",
    "timestamp": 1642683600000
  }
}
```

### Обработка ошибок

При возникновении ошибки сервер отправляет сообщение:

```json
{
  "type": "error",
  "data": {
    "error": "Invalid channel name",
    "code": "INVALID_CHANNEL"
  }
}
```

### Отключение

При отключении клиента сессия автоматически очищается, все подписки удаляются.

## Analytics Rules: Notification Policy

Новые endpoints для управления политикой каналов уведомлений правила аналитики (без изменения общего контракта `AnalyticsRule`):

- `GET /api/v1/analytics/rules/{id}/notification-policy` — получить текущую политику;
- `PUT /api/v1/analytics/rules/{id}/notification-policy` — обновить политику.

### GET /api/v1/analytics/rules/{id}/notification-policy

**Auth:** JWT, роль `VIEWER` и выше.

**Response (200):**
```json
{
  "success": true,
  "data": {
    "ruleId": "rule-6",
    "sendNotification": true,
    "notifyInApp": false,
    "notifyEmail": true,
    "notifyTelegram": false,
    "notificationType": "WARNING"
  },
  "message": "Analytics rule notification policy retrieved successfully"
}
```

### PUT /api/v1/analytics/rules/{id}/notification-policy

**Auth:** JWT, роль `OPERATOR` и выше.

**Request body (partial update):**
```json
{
  "sendNotification": true,
  "notifyInApp": false,
  "notifyEmail": true,
  "notifyTelegram": false,
  "notificationType": "WARNING"
}
```

**Правила валидации:**
- Если хотя бы один канал (`notifyInApp`, `notifyEmail`, `notifyTelegram`) включен, `sendNotification` должен быть `true`.
- `notificationType` должен быть валидным значением enum (`EVENT`, `ALERT`, `INFO`, `WARNING`, `ERROR`, `SYSTEM`, `RECORDING`, `LICENSE`, `USER`).

**Ошибки:**
- `400 Bad Request` — некорректный enum / нарушение валидации;
- `404 Not Found` — правило не найдено;
- `403 Forbidden` — недостаточно прав.

### Служебные ключи маршрутизации каналов уведомлений (backend)

Для внутренних интеграций (rule engine / automation) в `NotificationService.sendNotification(..., extras)` поддерживаются служебные ключи:

- `__notifyChannels`: csv-список каналов (`in-app,email,telegram`) — приоритетен над legacy-флагами;
- `__notifyInApp`, `__notifyEmail`, `__notifyTelegram`: legacy fallback флаги каналов;
- `__notifyWebhook`: `true/false` — включить webhook-канал;
- `__notifyWebhookUrl`: URL endpoint для webhook-доставки.
- `__notifySms`: `true/false` — включить SMS-канал;
- `__notifySmsTo`: csv-список номеров получателей для SMS.
- `__notifyPush`: `true/false` — включить push-канал;
- `__notifyPushTokens`: csv-список push/device токенов.
- `__emailSubjectTemplate`: шаблон темы email (fallback: стандартная тема сервера);
- `__emailBodyTemplate`: HTML-шаблон тела email (fallback: стандартный шаблон сервера).

Webhook-доставка подписывается заголовком `X-IPCSS-Signature` (HMAC SHA-256) при наличии `NOTIFICATION_WEBHOOK_SECRET`.
Поля маршрутизации не сохраняются в `notification.extras` (санитизируются перед записью в БД).

Для правил аналитики email-шаблоны могут задаваться в `actions.additionalActions` ключами:
- `emailSubjectTemplate`
- `emailBodyTemplate`

Для правил аналитики SMS может включаться через `actions.additionalActions`:
- `notifySms` (`true`/`false`)
- `smsTo` (csv-список номеров)

Для правил аналитики push может включаться через `actions.additionalActions`:
- `notifyPush` (`true`/`false`)
- `pushTokens` (csv-список токенов)

### Push token lifecycle API (backend)

`NotificationRoutes` поддерживает минимальный lifecycle для device tokens:

- `GET /api/v1/notifications/push-tokens` — список токенов текущего пользователя;
- `POST /api/v1/notifications/push-tokens` — регистрация/обновление токена;
- `DELETE /api/v1/notifications/push-tokens/{token}` — отзыв токена.
- `POST /api/v1/notifications/push-tokens/revoke-invalid` — пакетный отзыв невалидных токенов.

Пример `POST`:
```json
{
  "token": "push-token-1",
  "platform": "android"
}
```

Требуется JWT аутентификация (`jwt-auth`).

Если в `NotificationService` включён push (`__notifyPush=true`), но `__notifyPushTokens` не переданы,
сервер использует токены текущего пользователя из lifecycle-хранилища `PushTokenService`.

## Ошибки

Стандартные HTTP коды ошибок:
- `400 Bad Request` - неверный запрос
- `401 Unauthorized` - требуется аутентификация
- `403 Forbidden` - недостаточно прав
- `404 Not Found` - ресурс не найден
- `500 Internal Server Error` - внутренняя ошибка сервера

Формат ошибки:
```json
{
  "success": false,
  "error": {
    "code": "INVALID_REQUEST",
    "message": "Неверный формат запроса",
    "timestamp": "2026-01-20T14:30:00Z"
  }
}
```

## Rate Limiting

Лимиты запросов:
- Анонимные: 100 запросов в час
- Пользователи: 1000 запросов в час
- API ключи: 10000 запросов в час

Заголовки ответа:
```
X-RateLimit-Limit: 1000
X-RateLimit-Remaining: 850
X-RateLimit-Reset: 1642683600
```

---

## Связанные документы

> **📚 Полный индекс документации:** [DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)

### Основные документы
- **[DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)** - Полный индекс документации
- **[README.md](../README.md)** - Обзор проекта

### Техническая документация
- **[DEPLOYMENT_GUIDE.md](../archive/docs/deployment/DEPLOYMENT_GUIDE.md)** - Руководство по развертыванию
- **[CONFIGURATION.md](CONFIGURATION.md)** - Руководство по настройке и конфигурации
- **[WEBSOCKET_CLIENT.md](../archive/docs-duplicates-2026-08-08/WEBSOCKET_CLIENT.md)** - Документация WebSocket клиента

### Серверная часть
- **[server/web/README.md](../server/web/README.md)** - Документация веб-интерфейса
- **[server/web/WEB_UI_IMPLEMENTATION.md](../server/web/WEB_UI_IMPLEMENTATION.md)** - Детальная документация реализации веб-интерфейса

---

**Последнее обновление:** 27 April 2026


