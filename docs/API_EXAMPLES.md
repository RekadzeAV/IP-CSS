# Примеры использования API

**Версия проекта:** Alfa-0.0.1
**Последнее обновление:** Январь 2025

> **📚 Связанные документы:**
> - [API.md](API.md) - Полная документация API
> - [TROUBLESHOOTING.md](TROUBLESHOOTING.md) - Устранение неполадок

---

## Базовые примеры

### Аутентификация

```bash
# Вход в систему
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "password123"
  }'

# Сохранение токена
export TOKEN="eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

### Получение списка камер

```bash
curl -X GET http://localhost:8080/api/v1/cameras \
  -H "Authorization: Bearer $TOKEN"
```

---

## Примеры на разных языках

### JavaScript/TypeScript

```typescript
// Аутентификация
const response = await fetch('http://localhost:8080/api/v1/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    username: 'admin',
    password: 'password123'
  })
});

const { token } = await response.json();

// Получение камер
const cameras = await fetch('http://localhost:8080/api/v1/cameras', {
  headers: { 'Authorization': `Bearer ${token}` }
}).then(r => r.json());
```

### Python

```python
import requests

# Аутентификация
response = requests.post('http://localhost:8080/api/v1/auth/login', json={
    'username': 'admin',
    'password': 'password123'
})
token = response.json()['token']

# Получение камер
headers = {'Authorization': f'Bearer {token}'}
cameras = requests.get('http://localhost:8080/api/v1/cameras', headers=headers).json()
```

### Kotlin

```kotlin
// Используя Ktor Client
val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        json()
    }
}

// Аутентификация
val loginResponse = client.post("http://localhost:8080/api/v1/auth/login") {
    contentType(ContentType.Application.Json)
    setBody(LoginRequest("admin", "password123"))
}.body<LoginResponse>()

val token = loginResponse.token

// Получение камер
val cameras = client.get("http://localhost:8080/api/v1/cameras") {
    header("Authorization", "Bearer $token")
}.body<List<Camera>>()
```

---

## Работа с камерами

### Добавление камеры

```bash
curl -X POST http://localhost:8080/api/v1/cameras \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Входная камера",
    "url": "rtsp://192.168.1.100:554/stream1",
    "username": "admin",
    "password": "camera123"
  }'
```

### Управление PTZ

```bash
curl -X POST http://localhost:8080/api/v1/cameras/cam-001/control \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "action": "ptz_move",
    "parameters": {
      "direction": "right",
      "speed": 50
    }
  }'
```

---

## Работа с записями

### Начало записи

```bash
curl -X POST http://localhost:8080/api/v1/recordings/start \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "cameraId": "cam-001",
    "duration": 300,
    "quality": "HIGH"
  }'
```

### Поиск записей

```bash
curl -X GET "http://localhost:8080/api/v1/recordings?camera_id=cam-001&start_time=1642683600000&end_time=1642770000000" \
  -H "Authorization: Bearer $TOKEN"
```

---

**Последнее обновление:** Январь 2025

