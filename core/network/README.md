# Network Module

Модуль `:core:network` предоставляет базовый HTTP клиент для работы с REST API и функциональность для обнаружения IP камер.

## Архитектура KMP Source Sets

Модуль использует иерархическую структуру source sets для максимального переиспользования кода:

```
commonMain (общий код)
    ├── jvmMain (общий JVM код)
    │   ├── androidMain (Android-specific)
    │   └── desktopMain (Desktop-specific + JavaCV)
    ├── iosMain (iOS-specific)
    └── nativeMain (Linux, macOS, Windows)
```

### JVM Code Sharing

`jvmMain` содержит общие JVM реализации, которые используются как Android, так и Desktop таргетами:

- `Live555RTSPClient` — stub реализация RTSP клиента
- `RTSPSClient` — stub для TLS/SSL
- `UPnPDiscovery` — UPnP discovery через SSDP
- `WSDiscovery` — WS-Discovery для ONVIF
- `CertificatePinner` — certificate pinning (stub)
- `MediaFrame` — платформа-агностик класс для медиа фреймов
- `NetworkScanner` — сканер подсети
- `ApiClient` — HTTP клиент с Ktor

**Пример зависимости:**
```kotlin
// androidMain зависит от jvmMain
val androidMain by getting {
    dependsOn(jvmMain)
    dependencies {
        implementation(libs.ktor.client.android)
    }
}

// desktopMain зависит от jvmMain
val desktopMain by getting {
    dependsOn(jvmMain)
    dependencies {
        implementation(libs.ktor.client.java)
        implementation("org.bytedeco:javacv:1.5.13") // Только для desktop
    }
}
```

## Основные возможности

### HTTP Client
- ✅ Базовый HTTP клиент на основе Ktor
- ✅ Конфигурация (baseUrl, timeouts, interceptors)
- ✅ Сериализация (Kotlinx Serialization)
- ✅ Обработка ошибок
- ✅ Retry логика с экспоненциальной задержкой
- ✅ Кэширование ответов для GET запросов

### Network Scanner
- ✅ Сканирование подсети (CIDR /8, /16, /24, /32)
- ✅ Параллельное сканирование с контролем concurrency
- ✅ Прогресс-индикатор через Kotlin Flow
- ✅ Быстрое RTSP сканирование
- ✅ ONVIF обнаружение камер
- ✅ Утилиты для работы с результатами

### ONVIF Support
- ✅ WS-Discovery для автообнаружения
- ✅ GetCapabilities для проверки возможностей
- ✅ Тестирование подключения
- ✅ Кроссплатформенная поддержка

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

---

## Network Scanner

### Базовое сканирование

```kotlin
import com.company.ipcamera.core.network.NetworkScanner
import io.ktor.client.HttpClient

val httpClient = HttpClient()
val scanner = NetworkScanner(httpClient)

val cameras = scanner.scanSubnet(
    subnetRange = "192.168.1.0/24",
    ports = listOf(554, 80),
    timeoutPerHost = 1000
)

println("Найдено камер: ${cameras.size}")
cameras.forEach { camera ->
    println("  - ${camera.name}: ${camera.url}")
}
```

### Сканирование с прогрессом

```kotlin
scanner.scanSubnetWithProgress(
    subnetRange = "192.168.1.0/24",
    ports = listOf(554),
    timeoutPerHost = 1000,
    maxConcurrent = 50
).collect { progress ->
    println("Прогресс: ${progress.progressPercent}%")
    println("Хостов проверено: ${progress.hostsScanned}/${progress.hostsTotal}")
    println("Камер найдено: ${progress.camerasFound}")
    println("Действие: ${progress.currentActivity}")
}
```

### Утилиты ScannerUtils

```kotlin
import com.company.ipcamera.core.network.utils.ScannerUtils

// Валидация подсети
val valid = ScannerUtils.isValidSubnet("192.168.1.0/24")

// Парсинг в список IP
val ips = ScannerUtils.parseSubnetToIps("192.168.1.0/24")

// Оценка времени сканирования
val estimatedTime = ScannerUtils.estimateScanTime(
    subnetRange = "192.168.1.0/24",
    timeoutPerHost = 1000,
    ports = 1,
    maxConcurrent = 50
)

// Группировка по производителю
val byManufacturer = ScannerUtils.groupCamerasByManufacturer(cameras)

// Фильтрация
val hikvision = ScannerUtils.filterByManufacturer(cameras, "Hikvision")
val byIpRange = ScannerUtils.filterByIpRange(
    cameras,
    "192.168.1.10",
    "192.168.1.100"
)

// Дедупликация и сортировка
val unique = ScannerUtils.deduplicateCameras(cameras)
val sorted = ScannerUtils.sortCamerasByName(cameras)

// Статистика
val stats = ScannerUtils.getScanStatistics(cameras)
println("Всего: ${stats.totalCameras}, Производителей: ${stats.uniqueManufacturers}")
```

### Extension функции

```kotlin
import com.company.ipcamera.core.network.utils.*

// Progress расширения
val percent = progress.progressPercent
val seconds = progress.remainingTimeSeconds
val formatted = progress.remainingTimeFormatted // "5м 30с"
val isDone = progress.isCompleted

// Поиск камер
val byIp = cameras.findByIp("192.168.1.10")
val byName = cameras.findByName("Main Camera")
val byUrl = cameras.findByUrl("rtsp://...")
val exists = cameras.containsIp("192.168.1.10")
```

---

## ONVIF Support

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




