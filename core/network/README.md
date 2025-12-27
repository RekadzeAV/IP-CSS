# Network Module

Модуль `:core:network` предоставляет базовый HTTP клиент для работы с REST API.

## Основные возможности

- ✅ Базовый HTTP клиент на основе Ktor
- ✅ Конфигурация (baseUrl, timeouts, interceptors)
- ✅ Сериализация (Kotlinx Serialization)
- ✅ Обработка ошибок
- ✅ Retry логика с экспоненциальной задержкой
- ✅ Кэширование ответов для GET запросов

## Использование

### Создание клиента

```kotlin
import com.company.ipcamera.core.network.*

val config = ApiClientConfig(
    baseUrl = "https://api.example.com",
    connectTimeout = 30.seconds,
    enableLogging = true,
    enableRetry = true,
    maxRetries = 3,
    enableCache = true,
    apiKey = "your-api-key",
    authToken = "your-auth-token"
)

val apiClient = ApiClient.create(config)
```

### Выполнение запросов

#### GET запрос

```kotlin
@Serializable
data class User(val id: String, val name: String)

val result: ApiResult<User> = apiClient.get<User>("/users/123")

result.fold(
    onSuccess = { user -> println("User: ${user.name}") },
    onError = { error -> println("Error: ${error.message}") }
)
```

#### POST запрос

```kotlin
@Serializable
data class CreateUserRequest(val name: String, val email: String)
@Serializable
data class UserResponse(val id: String, val name: String)

val request = CreateUserRequest(name = "John", email = "john@example.com")
val result: ApiResult<UserResponse> = apiClient.post<UserResponse>("/users", request)
```

#### PUT/PATCH/DELETE запросы

```kotlin
// PUT
val result = apiClient.put<UserResponse>("/users/123", updateRequest)

// PATCH
val result = apiClient.patch<UserResponse>("/users/123", patchRequest)

// DELETE
val result = apiClient.delete<Unit>("/users/123")
```

### Загрузка файлов

```kotlin
// Простая загрузка
val fileData = File("image.jpg").readBytes()
val result = apiClient.upload<UploadResponse>(
    path = "/upload",
    fileData = fileData,
    fileName = "image.jpg",
    contentType = ContentType.Image.JPEG
)

// Multipart загрузка
val result = apiClient.uploadMultipart<UploadResponse>(
    path = "/upload",
    fileData = fileData,
    fileName = "image.jpg",
    fieldName = "file",
    additionalFields = mapOf("description" to "Profile picture")
)
```

### Обработка ошибок

```kotlin
when (val error = result.error) {
    is ApiError.NetworkError -> {
        // Проблемы с сетью
        println("Network error: ${error.cause.message}")
    }
    is ApiError.HttpError -> {
        // HTTP ошибки (4xx, 5xx)
        println("HTTP ${error.statusCode}: ${error.message}")
    }
    is ApiError.TimeoutError -> {
        // Таймаут
        println("Request timeout: ${error.message}")
    }
    is ApiError.SerializationError -> {
        // Ошибки сериализации
        println("Serialization error: ${error.cause.message}")
    }
    is ApiError.UnknownError -> {
        // Неизвестная ошибка
        println("Unknown error: ${error.cause.message}")
    }
}
```

### Кэширование

Кэширование автоматически включено для GET запросов. Можно отключить для конкретного запроса:

```kotlin
// Без кэша
val result = apiClient.get<User>("/users/123", useCache = false)
```

Очистка кэша:

```kotlin
apiClient.clearCache()
```

### Обновление токена авторизации

```kotlin
val newClient = apiClient.updateAuthToken("new-token")
```

### Закрытие клиента

```kotlin
apiClient.close()
```

## Конфигурация

### ApiClientConfig

- `baseUrl` - базовый URL API (обязательно)
- `connectTimeout` - таймаут подключения (по умолчанию: 30 секунд)
- `socketTimeout` - таймаут сокета (по умолчанию: 30 секунд)
- `requestTimeout` - таймаут запроса (по умолчанию: 30 секунд)
- `enableLogging` - включить логирование (по умолчанию: true)
- `enableRetry` - включить retry логику (по умолчанию: true)
- `maxRetries` - максимальное количество повторов (по умолчанию: 3)
- `retryDelay` - задержка между повторами (по умолчанию: 1 секунда)
- `enableCache` - включить кэширование (по умолчанию: true)
- `cacheMaxSize` - максимальный размер кэша (по умолчанию: 100)
- `cacheExpirationTime` - время жизни кэша (по умолчанию: 5 минут)
- `headers` - дополнительные заголовки
- `apiKey` - API ключ (добавляется в заголовок X-API-Key)
- `authToken` - токен авторизации (добавляется в заголовок Authorization)

## Retry логика

Retry логика автоматически повторяет запросы при следующих ошибках:
- Сетевые ошибки (NetworkError)
- Таймауты (TimeoutError)
- HTTP ошибки сервера (5xx)

Количество повторов и задержка настраиваются в конфигурации. Используется экспоненциальная задержка (каждый следующий повтор с удвоенной задержкой, максимум 10 секунд).

## Платформо-специфичные реализации

Модуль поддерживает Android и iOS:
- Android: использует `io.ktor:ktor-client-android`
- iOS: использует `io.ktor:ktor-client-darwin`

Движок HTTP клиента выбирается автоматически в зависимости от платформы.



