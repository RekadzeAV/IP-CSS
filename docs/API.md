# API Документация

## Базовый URL
```
https://api.company.com/v1
или
http://localhost:8080/api/v1
```

## Аутентификация

Все запросы (кроме публичных endpoints) требуют аутентификации через JWT токен.

### Получение токена:
```http
POST /auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}
```

### Использование токена:
```http
GET /cameras
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

## Endpoints

### Камеры

#### Получить список камер
```http
GET /cameras?page=1&limit=20&status=online
```

#### Добавить камеру
```http
POST /cameras
Content-Type: application/json

{
  "name": "Новая камера",
  "url": "rtsp://192.168.1.101:554/stream1",
  "username": "admin",
  "password": "camera123"
}
```

#### Получить информацию о камере
```http
GET /cameras/{id}
```

#### Управление камерой
```http
POST /cameras/{id}/control
Content-Type: application/json

{
  "action": "ptz_move",
  "parameters": {
    "direction": "right",
    "speed": 50
  }
}
```

### Записи

#### Получить список записей
```http
GET /recordings?camera_id=cam-001&start_time=2024-01-20T00:00:00Z
```

#### Начать запись
```http
POST /recordings/start
Content-Type: application/json

{
  "camera_id": "cam-001",
  "duration": 300
}
```

#### Экспорт записи
```http
POST /recordings/{id}/export
Content-Type: application/json

{
  "format": "mp4",
  "quality": "medium"
}
```

### События

#### Получить события
```http
GET /events?type=motion&camera_id=cam-001
```

#### Подтвердить событие
```http
POST /events/{id}/acknowledge
```

### Аналитика

#### Детекция объектов
```http
POST /analytics/detect
Content-Type: multipart/form-data

file: <image_file>
```

#### ANPR (Распознавание номеров)
```http
POST /analytics/anpr
Content-Type: multipart/form-data

file: <image_file>
```

### Система

#### Получить статус системы
```http
GET /system/status
```

#### Получить настройки
```http
GET /system/settings
```

### Лицензия

#### Получить информацию о лицензии
```http
GET /license
```

#### Активировать лицензию
```http
POST /license/activate
Content-Type: application/json

{
  "license_key": "ENT-1234-5678-9012"
}
```

## WebSocket API

### Подключение
```
wss://api.company.com/v1/ws
```

### Аутентификация
```json
{
  "type": "auth",
  "data": {
    "token": "jwt-token-here"
  }
}
```

### Подписка на события
```json
{
  "type": "subscribe",
  "data": {
    "channels": ["camera_events", "camera_status"],
    "filters": {
      "camera_ids": ["cam-001"]
    }
  }
}
```

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
  "error": {
    "code": "INVALID_REQUEST",
    "message": "Неверный формат запроса",
    "timestamp": "2024-01-20T14:30:00Z"
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

