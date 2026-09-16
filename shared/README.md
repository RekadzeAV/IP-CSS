# Shared Module

Модуль `:shared` содержит общую бизнес-логику приложения, включая репозитории, доменные модели и конфигурацию.

## Архитектура

```
shared/
├── src/
│   ├── commonMain/
│   │   ├── kotlin/
│   │   │   ├── .../domain/
│   │   │   │   ├── model/          # Доменные модели
│   │   │   │   └── repository/     # Интерфейсы репозиториев
│   │   │   └── data/
│   │   │       ├── local/          # Local storage (Database)
│   │   │       └── repository/     # Реализации репозиториев
│   │   └── commonTest/
│   │       └── kotlin/
│   │           └── .../repository/ # Тесты репозиториев
```

## Доменные модели

### Camera

Основная модель камеры:

```kotlin
data class Camera(
    val id: String,
    val name: String,
    val url: String,
    val username: String?,
    val password: String?,
    val status: CameraStatus,
    val manufacturer: String?,
    val model: String?,
    val createdAt: Instant?,
    val updatedAt: Instant?
)
```

**Поля:**
- `id` - уникальный идентификатор
- `name` - имя камеры
- `url` - URL доступа (RTSP/HTTP)
- `username` - имя пользователя (опционально)
- `password` - пароль (опционально)
- `status` - статус (ONLINE, OFFLINE, UNKNOWN)
- `manufacturer` - производитель
- `model` - модель
- `createdAt/updatedAt` - метки времени

### DiscoveredCamera

Модель обнаруженной камеры:

```kotlin
data class DiscoveredCamera(
    val name: String,
    val url: String,
    val model: String?,
    val manufacturer: String?,
    val ipAddress: String,
    val port: Int
)
```

## Интерфейсы репозиториев

### CameraRepository

```kotlin
interface CameraRepository {
    // CRUD операции
    suspend fun getAllCameras(): List<Camera>
    suspend fun getCameraById(id: String): Camera?
    suspend fun createCamera(camera: Camera): String
    suspend fun updateCamera(camera: Camera)
    suspend fun deleteCamera(id: String)
    
    // Обнаружение камер
    suspend fun discoverCameras(forceRefresh: Boolean): List<DiscoveredCamera>
    suspend fun discoverCamerasWithProgress(
        forceRefresh: Boolean,
        config: DiscoveryConfig
    ): Flow<DiscoveryProgress>
    
    // Тестирование подключения
    suspend fun testConnection(camera: Camera): ConnectionTestResult
}
```

## Конфигурация обнаружения

### DiscoveryConfig

```kotlin
data class DiscoveryConfig(
    val methods: List<DiscoveryMethod> = listOf(
        DiscoveryMethod.WS_DISCOVERY,
        DiscoveryMethod.SUBNET_SCAN
    ),
    val subnetRange: String? = null,
    val ports: List<Int> = listOf(554, 80, 8080),
    val timeoutPerMethod: Long = 5000,
    val parallelDiscovery: Boolean = true,
    val cacheEnabled: Boolean = true,
    val cacheTtlMs: Long = 7 * 60 * 1000
)
```

**Методы обнаружения:**
- `WS_DISCOVERY` - ONVIF WS-Discovery (multicast)
- `UPNP_DISCOVERY` - UPnP Device Discovery
- `SUBNET_SCAN` - Сканирование подсети
- `MANUAL_INPUT` - Ручной ввод (заготовка)
- `DNS_SD` - DNS Service Discovery (заготовка)

### DiscoveryProgress

Отслеживание прогресса обнаружения:

```kotlin
data class DiscoveryProgress(
    val currentMethod: DiscoveryMethod,
    val methodProgress: Float,
    val overallProgress: Float,
    val devicesFoundSoFar: Int,
    val devicesTotalEstimated: Int?,
    val elapsedTimeMs: Long,
    val remainingTimeMs: Long?,
    val currentActivity: String,
    val discoveredCamerasSnapshot: List<DiscoveredCamera>
)
```

## Тестирование подключения

### ConnectionTestResult

Результат тестирования подключения:

```kotlin
sealed class ConnectionTestResult {
    data class Success(
        val streams: List<StreamInfo>,
        val capabilities: CameraCapabilities,
        val onvifVersion: String?,
        val rtspVersion: String?,
        val latencyMs: Long?,
        val supportedCodecs: List<String>,
        val authenticationMethod: String?,
        val connectionQuality: Float?
    ) : ConnectionTestResult()
    
    data class Failure(
        val error: String,
        val code: ErrorCode,
        val diagnostics: ConnectionDiagnostics?
    ) : ConnectionTestResult()
}
```

**Расширенные поля Success:**
- `onvifVersion` - версия ONVIF протокола
- `rtspVersion` - версия RTSP протокола
- `latencyMs` - измеренная задержка
- `supportedCodecs` - поддерживаемые кодеки
- `authenticationMethod` - метод аутентификации (Digest, Basic, None)
- `connectionQuality` - качество подключения (0.0-1.0)

### ConnectionDiagnostics

Детальная диагностика:

```kotlin
data class ConnectionDiagnostics(
    val networkReachable: Boolean,
    val portOpen: Boolean,
    val authenticationSupported: Boolean,
    val onvifServiceAvailable: Boolean,
    val rtspServiceAvailable: Boolean,
    val sslCertificateValid: Boolean?,
    val bandwidthMbs: Float?,
    val recommendedSettings: ConnectionRecommendances?
)
```

### CameraCapabilities

Возможности камеры:

```kotlin
data class CameraCapabilities(
    val ptz: Boolean,
    val audio: Boolean,
    val onvif: Boolean,
    val analytics: Boolean,
    val supportedResolutions: List<String>,
    val supportedCodecs: List<String>,
    val motionDetection: Boolean,
    val objectDetection: Boolean
)
```

## Реализация репозитория

### CameraRepositoryImpl

```kotlin
class CameraRepositoryImpl(
    private val databaseFactory: DatabaseFactory
) : CameraRepository {
    // ...
}
```

**Фичи:**
- Кэширование данных с TTL
- Интеграция с NetworkScanner
- Расширенное тестирование подключений (ONVIF + RTSP)
- Прогресс-индикатор для discovery
- Обработка ошибок

### Кэширование

Репозиторий использует многоуровневое кэширование:

```kotlin
// Кэш всех камер (5 минут)
private val allCamerasCacheKey = "all_cameras"

// Кэш отдельных камер (5 минут)
private val cameraCache = CameraCache(maxSize = 1000, expirationTime = 5.minutes)

// Кэш результатов discovery (7 минут)
private val discoveryCache = CameraCache(maxSize = 10, expirationTime = 7.minutes)

// Кэш статусов (20 секунд)
private val statusCache = CameraCache(maxSize = 1000, expirationTime = 20.seconds)
```

## Примеры использования

### Создание репозитория

```kotlin
val databaseFactory = DatabaseFactory()
val repository = CameraRepositoryImpl(databaseFactory)
```

### Получение всех камер

```kotlin
val cameras = repository.getAllCameras()
```

### Обнаружение камер с прогрессом

```kotlin
lifecycleScope.launch {
    repository.discoverCamerasWithProgress(
        forceRefresh = true,
        config = DiscoveryConfig(
            methods = listOf(
                DiscoveryMethod.WS_DISCOVERY,
                DiscoveryMethod.SUBNET_SCAN
            ),
            subnetRange = "192.168.1.0/24"
        )
    ).collect { progress ->
        // Обновление UI
        updateProgressUI(progress)
        
        if (progress.isCompleted) {
            showCamerasList(progress.discoveredCamerasSnapshot)
        }
    }
}
```

### Тестирование подключения

```kotlin
val camera = Camera(
    id = "camera-1",
    name = "Main Camera",
    url = "rtsp://192.168.1.10:554/stream",
    username = "admin",
    password = "password",
    status = CameraStatus.UNKNOWN
)

val result = repository.testConnection(camera)

when (result) {
    is ConnectionTestResult.Success -> {
        println("ONVIF version: ${result.onvifVersion}")
        println("Latency: ${result.latencyMs}ms")
        println("Quality: ${result.connectionQuality}")
        
        result.streams.forEach { stream ->
            println("Stream: ${stream.type} - ${stream.resolution}")
        }
    }
    is ConnectionTestResult.Failure -> {
        println("Error: ${result.error}")
        result.diagnostics?.let { diag ->
            println("Network reachable: ${diag.networkReachable}")
            println("Port open: ${diag.portOpen}")
        }
    }
}
```

## Тестирование

### Unit тесты

```kotlin
class CameraRepositoryImplTest {
    
    @Test
    fun testGetAllCameras() = runTest {
        val repository = CameraRepositoryImpl(createInMemoryDatabase())
        val cameras = repository.getAllCameras()
        
        assertTrue(cameras is List<*>)
    }
}
```

### Интеграционные тесты

```kotlin
class CameraRepositoryImplIntegrationTest {
    
    @Test
    fun testDiscoverCamerasWithProgress() = runTest {
        val repository = CameraRepositoryImpl(createInMemoryDatabase())
        
        val progressFlow = repository.discoverCamerasWithProgress(
            forceRefresh = true,
            config = DiscoveryConfig(
                methods = listOf(DiscoveryMethod.WS_DISCOVERY)
            )
        )
        
        val firstProgress = progressFlow.first()
        assertNotNull(firstProgress)
    }
}
```

## Зависимости

- `:core:network` - HTTP клиент и NetworkScanner
- `:core:common` - Общие утилиты и модели
- `ktor-client-core` - HTTP клиент
- `kotlinx-coroutines-core` - Корутин
- `kotlinx-datetime` - Работа со временем

## License

Copyright © 2025 NLP-Core-Team
