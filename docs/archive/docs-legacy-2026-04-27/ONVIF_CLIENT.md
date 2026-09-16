# ONVIF Client Documentation

**Версия проекта:** Alfa-0.0.1
**Последнее обновление:** 26 January 2026

> **📚 Полный индекс документации:** [DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)

## Обзор

ONVIF клиент обеспечивает взаимодействие с IP-камерами по стандарту ONVIF (Open Network Video Interface Forum). Он поддерживает обнаружение камер, получение информации об устройствах, управление PTZ и получение URI потоков.

## Использование

### Создание клиента

```kotlin
import com.company.ipcamera.core.network.*

val engine = HttpClientEngineFactory.create() // Платформо-специфичный engine
val onvifClient = OnvifClient(engine)
```

### Обнаружение камер

```kotlin
val discoveredCameras = onvifClient.discoverCameras(timeoutMillis = 5000)
discoveredCameras.forEach { camera ->
    println("Found camera: ${camera.name}")
    println("  URL: ${camera.url}")
    println("  IP: ${camera.ipAddress}:${camera.port}")
    println("  Model: ${camera.model}")
    println("  Manufacturer: ${camera.manufacturer}")
}
```

### Получение информации об устройстве

```kotlin
val deviceInfo = onvifClient.getDeviceInformation(
    url = "http://192.168.1.100",
    username = "admin",
    password = "password123"
)

deviceInfo?.let {
    println("Manufacturer: ${it.manufacturer}")
    println("Model: ${it.model}")
    println("Firmware: ${it.firmwareVersion}")
    println("Serial: ${it.serialNumber}")
    println("Hardware ID: ${it.hardwareId}")
}
```

### Получение возможностей камеры

```kotlin
val capabilities = onvifClient.getCapabilities(
    url = "http://192.168.1.100",
    username = "admin",
    password = "password123"
)

capabilities?.let {
    println("Device Service: ${it.deviceServiceUrl}")
    println("Media Service: ${it.mediaServiceUrl}")
    println("PTZ Service: ${it.ptzServiceUrl}")
}
```

### Управление PTZ

```kotlin
// Движение вправо
onvifClient.movePtz(
    url = "http://192.168.1.100",
    direction = PtzDirection.RIGHT,
    speed = 0.5f,
    username = "admin",
    password = "password123"
)

// Движение вверх-вправо
onvifClient.movePtz(
    url = "http://192.168.1.100",
    direction = PtzDirection.UP_RIGHT,
    speed = 0.7f,
    username = "admin",
    password = "password123"
)

// Приближение (Zoom In)
onvifClient.zoomIn(
    url = "http://192.168.1.100",
    speed = 0.5f,
    profileToken = "Profile1",
    username = "admin",
    password = "password123"
)

// Отдаление (Zoom Out)
onvifClient.zoomOut(
    url = "http://192.168.1.100",
    speed = 0.5f,
    profileToken = "Profile1",
    username = "admin",
    password = "password123"
)

// Остановка движения
onvifClient.stopPtz(
    url = "http://192.168.1.100",
    profileToken = "Profile1",
    username = "admin",
    password = "password123"
)
```

### Получение профилей

```kotlin
val profiles = onvifClient.getProfiles(
    url = "http://192.168.1.100",
    username = "admin",
    password = "password123"
)

profiles.forEach { profile ->
    println("Profile: ${profile.name} (${profile.token})")
    println("  Resolution: ${profile.videoResolution}")
    println("  FPS: ${profile.fps}")
    println("  Codec: ${profile.codec}")
}
```

### Получение URI потока

```kotlin
val streamUri = onvifClient.getStreamUri(
    url = "http://192.168.1.100",
    profileToken = "Profile1",
    username = "admin",
    password = "password123"
)

streamUri?.let {
    println("Stream URI: $it")
    // Использовать URI с RTSP клиентом
}
```

### Проверка подключения к камере

```kotlin
val result = onvifClient.testConnection(
    url = "http://192.168.1.100",
    username = "admin",
    password = "password123"
)

when (result) {
    is ConnectionTestResult.Success -> {
        println("Connection successful")
        println("Streams: ${result.streams.size}")
        result.streams.forEach { stream ->
            println("  - ${stream.type}: ${stream.resolution} @ ${stream.fps} fps")
        }
        println("Capabilities:")
        println("  PTZ: ${result.capabilities.ptz}")
        println("  Audio: ${result.capabilities.audio}")
        println("  ONVIF: ${result.capabilities.onvif}")
    }
    is ConnectionTestResult.Failure -> {
        println("Connection failed: ${result.error}")
        println("Error code: ${result.code}")
    }
}
```

## API

### OnvifClient

Основной класс для работы с ONVIF камерами.

#### Методы

- `discoverCameras(timeoutMillis: Long): List<DiscoveredCamera>` - обнаружить камеры в сети
- `getDeviceInformation(url: String, username: String?, password: String?): DeviceInformation?` - получить информацию об устройстве
- `getCapabilities(url: String, username: String?, password: String?): OnvifCapabilities?` - получить возможности камеры
- `movePtz(url: String, direction: PtzDirection, speed: Float, username: String?, password: String?): Boolean` - управление PTZ
- `stopPtz(url: String, profileToken: String, username: String?, password: String?): Boolean` - остановка PTZ
- `zoomIn(url: String, speed: Float, profileToken: String, username: String?, password: String?): Boolean` - приближение
- `zoomOut(url: String, speed: Float, profileToken: String, username: String?, password: String?): Boolean` - отдаление
- `getProfiles(url: String, username: String?, password: String?): List<OnvifProfile>` - получить профили камеры
- `getStreamUri(url: String, profileToken: String, username: String?, password: String?): String?` - получить URI потока
- `testConnection(url: String, username: String?, password: String?): ConnectionTestResult` - проверить подключение
- `close()` - закрыть клиент

### Типы данных

#### DiscoveredCamera
Обнаруженная камера:
```kotlin
data class DiscoveredCamera(
    val name: String,
    val url: String,
    val model: String?,
    val manufacturer: String?,
    val ipAddress: String,
    val port: Int = 554
)
```

#### DeviceInformation
Информация об устройстве:
```kotlin
data class DeviceInformation(
    val manufacturer: String,
    val model: String,
    val firmwareVersion: String,
    val serialNumber: String,
    val hardwareId: String
)
```

#### OnvifCapabilities
Возможности камеры:
```kotlin
data class OnvifCapabilities(
    val deviceServiceUrl: String?,
    val mediaServiceUrl: String?,
    val ptzServiceUrl: String?
)
```

#### OnvifProfile
Профиль камеры:
```kotlin
data class OnvifProfile(
    val token: String,
    val name: String,
    val videoResolution: Resolution?,
    val fps: Int?,
    val codec: String?,
    val hasAudio: Boolean = false,
    val audioCodec: String? = null
)
```

#### PtzDirection
Направление движения PTZ:
- `UP` - вверх
- `DOWN` - вниз
- `LEFT` - влево
- `RIGHT` - вправо
- `UP_LEFT` - вверх-влево
- `UP_RIGHT` - вверх-вправо
- `DOWN_LEFT` - вниз-влево
- `DOWN_RIGHT` - вниз-вправо
- `ZOOM_IN` - увеличение
- `ZOOM_OUT` - уменьшение
- `STOP` - остановка

## Примеры использования

### Интеграция с CameraRepository

```kotlin
class CameraDiscoveryService {
    private val onvifClient = OnvifClient(httpClientEngine)

    suspend fun discoverAndAddCameras(): List<Camera> {
        val discovered = onvifClient.discoverCameras()
        val cameras = mutableListOf<Camera>()

        discovered.forEach { discoveredCamera ->
            val deviceInfo = onvifClient.getDeviceInformation(
                url = discoveredCamera.url,
                username = null,
                password = null
            )

            val capabilities = onvifClient.getCapabilities(
                url = discoveredCamera.url
            )

            val profiles = onvifClient.getProfiles(
                url = discoveredCamera.url
            )

            if (profiles.isNotEmpty()) {
                val streamUri = onvifClient.getStreamUri(
                    url = discoveredCamera.url,
                    profileToken = profiles.first().token
                )

                val camera = Camera(
                    id = generateId(),
                    name = discoveredCamera.name,
                    url = streamUri ?: discoveredCamera.url,
                    model = deviceInfo?.model ?: discoveredCamera.model,
                    ptz = capabilities?.ptzServiceUrl?.let {
                        PTZConfig(
                            enabled = true,
                            type = PTZType.PTZ
                        )
                    },
                    resolution = profiles.first().videoResolution,
                    fps = profiles.first().fps ?: 25,
                    codec = profiles.first().codec ?: "H.264"
                )

                cameras.add(camera)
            }
        }

        return cameras
    }
}
```

### Управление PTZ камерой

```kotlin
class PtzController(private val camera: Camera) {
    private val onvifClient = OnvifClient(httpClientEngine)

    suspend fun move(direction: PtzDirection, speed: Float = 0.5f) {
        val url = camera.url.replace("rtsp://", "http://").substringBefore("/")
        onvifClient.movePtz(
            url = url,
            direction = direction,
            speed = speed,
            username = camera.username,
            password = camera.password
        )
    }

    suspend fun stop() {
        val url = camera.url.replace("rtsp://", "http://").substringBefore("/")
        onvifClient.stopPtz(
            url = url,
            profileToken = "Profile1",
            username = camera.username,
            password = camera.password
        )
    }
}
```

## Протокол ONVIF

### Основные сервисы

1. **Device Service** (`/onvif/device_service`) - управление устройством
2. **Media Service** (`/onvif/media_service`) - управление медиапотоками
3. **PTZ Service** (`/onvif/ptz_service`) - управление PTZ

### Основные операции

- `GetCapabilities` - получение возможностей устройства
- `GetDeviceInformation` - получение информации об устройстве
- `GetProfiles` - получение профилей потоков
- `GetStreamUri` - получение URI потока
- `ContinuousMove` - непрерывное движение PTZ
- `Stop` - остановка PTZ

## Аутентификация

### Basic Authentication
Поддерживается по умолчанию. Учетные данные передаются через параметры `username` и `password`:

```kotlin
val capabilities = onvifClient.getCapabilities(
    url = "http://192.168.1.100",
    username = "admin",
    password = "password123"
)
```

### Digest Authentication
**⚠️ В разработке** - планируется в ближайших версиях. Некоторые камеры (особенно Hikvision, Dahua) требуют Digest Authentication вместо Basic.

**Проблема:** При использовании Basic Authentication с камерами, требующими Digest, вы получите ошибку 401 Unauthorized.

**Временное решение:** Используйте учетные данные, которые поддерживают Basic Authentication, или дождитесь реализации Digest Authentication.

**Статус:** См. [План реализации](ONVIF_CLIENT_IMPLEMENTATION_PLAN.md#этап-1-digest-authentication-критично-для-mvp)

## Обнаружение устройств

### WS-Discovery
Реализован через UDP multicast на адресе `239.255.255.250:3702`. Клиент отправляет несколько Probe запросов для увеличения шансов обнаружения устройств.

**Особенности:**
- Автоматическая фильтрация ONVIF устройств
- Поддержка различных форматов XML ответов
- Fallback парсинг через regex при неудаче DOM парсинга
- Дедупликация обнаруженных устройств

**Ограничения:**
- Требует поддержку multicast в сети
- Может не работать в некоторых виртуальных сетях
- Таймаут по умолчанию: 5 секунд

### UPnP Discovery (альтернатива WS-Discovery)
Реализован как альтернативный метод обнаружения устройств. Работает параллельно с WS-Discovery для увеличения шансов обнаружения.

**Особенности:**
- Отправка M-SEARCH запросов через SSDP
- Парсинг UPnP device descriptions
- Интеграция в discoverCameras() с автоматическим fallback
- Кэширование обнаруженных устройств

**Использование:**
```kotlin
// UPnP включён по умолчанию
val cameras = onvifClient.discoverCameras(timeoutMillis = 5000, useUPnP = true)

// Только WS-Discovery
val cameras = onvifClient.discoverCameras(timeoutMillis = 5000, useUPnP = false)
```

**Ограничения:**
- Меньше совместимость с ONVIF камерами, чем WS-Discovery
- Требует поддержку SSDP в сети
- Базовая реализация device description parsing

## Обработка ошибок

Клиент автоматически обрабатывает различные типы ошибок:

- **401 Unauthorized** - ошибка аутентификации (возможно требуется Digest Auth)
- **Timeout** - таймаут подключения
- **SOAP Fault** - ошибка в SOAP запросе (парсится и логируется)
- **Network errors** - автоматический retry с exponential backoff

Пример обработки ошибок:

```kotlin
try {
    val capabilities = onvifClient.getCapabilities(url, username, password)
    // ...
} catch (e: Exception) {
    when {
        e.message?.contains("401") == true -> {
            println("Authentication failed. Camera may require Digest Authentication.")
        }
        e.message?.contains("timeout") == true -> {
            println("Connection timeout. Check network connectivity.")
        }
        else -> {
            println("Error: ${e.message}")
        }
    }
}
```

## Примечания

- ONVIF клиент использует SOAP протокол для коммуникации
- Для аутентификации используется Basic HTTP Authentication (Digest в разработке)
- WS-Discovery реализован с улучшенным XML парсингом и множественными Probe запросами
- Поддержка различных версий ONVIF (1.0, 2.0, 2.5) зависит от конкретной камеры
- XML парсинг использует как DOM парсинг, так и fallback через regex для максимальной совместимости
- Клиент автоматически нормализует URL (добавляет http:// если отсутствует)
- Поддержка различных namespace в XML ответах

## Статус реализации

**Текущий прогресс:** ~70% 🟡

**Текущий прогресс:** ~85% 🟢 **NEARLY COMPLETE**

**Реализовано:**
- ✅ Базовые методы (getCapabilities, getDeviceInformation, getProfiles, getStreamUri)
- ✅ PTZ управление (movePtz, stopPtz, zoomIn, zoomOut)
- ✅ testConnection() с детальной диагностикой
- ✅ WS-Discovery улучшен (улучшенный XML парсинг, множественные Probe запросы)
- ✅ Полноценный XML парсинг (улучшенный с поддержкой разных namespace)
- ✅ Fallback механизмы для парсинга XML
- ✅ WS-Discovery полная реализация (DOM + regex fallback, multiple probe strategies)
- ✅ UPnP Discovery как альтернатива (интегрирован в discoverCameras())
- ✅ Параллельный запуск WS-Discovery и UPnP
- ✅ Кэширование capabilities, device info, profiles (TTL-based)
- ✅ Event Service (subscribe, renew, pull messages)
- ✅ Analytics Service (engines, inputs)
- ✅ Imaging Service
- ✅ Обработка ошибок и retry логика
- ✅ Поддержка Basic Authentication
- ✅ DigestAuthHelper (структура готова, требуется интеграция)
- ✅ Обработка SOAP Fault
- ✅ Дедупликация обнаруженных устройств

**В процессе / Частично реализовано:**
- 🟡 WS-Discovery работает, но требует доработки для некоторых сетевых конфигураций
- 🟡 XML парсинг улучшен, но может требовать доработки для специфичных камер

**В процессе / Требует доработки:**
- 🟡 Тестирование WS-Discovery с реальными камерами разных производителей
- 🟡 Оптимизация таймаутов для разных сетевых конфигураций
- 🟡 UI для выбора обнаруженных камер
- 🟡 Troubleshooting guide

**Не реализовано:**
- ❌ Digest Authentication (только Basic) - **КРИТИЧНО для MVP**
- ❌ UPnP поддержка как альтернатива WS-Discovery
- ❌ Кэширование capabilities и профилей
- ❌ Дополнительные ONVIF функции (AbsoluteMove, GetPresets, Imaging, Events, Analytics)
- ❌ Поддержка ONVIF Profile S/T/M

**📋 План реализации:** [ONVIF_CLIENT_IMPLEMENTATION_PLAN.md](ONVIF_CLIENT_IMPLEMENTATION_PLAN.md)

- ❌ Digest Authentication (полная интеграция) - **КРИТИЧНО для MVP**
- ❌ UPnP Device Description parsing (полный)
- ❌ UPnP service discovery (AVTransport, RenderingControl)
- ❌ Дополнительные ONVIF функции (AbsoluteMove, GetPresets, etc.)
- ❌ Поддержка ONVIF Profile S/T/M (частично)

**📋 План доработки:** [ONVIF_DISCOVERY_IMPLEMENTATION_PLAN_2026-04-27.md](../../planning/ONVIF_DISCOVERY_IMPLEMENTATION_PLAN_2026-04-27.md)

**Детальный анализ нереализованного функционала:** [MISSING_FUNCTIONALITY.md](MISSING_FUNCTIONALITY.md#onvifclient)
**Руководство по интеграции:** [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md#1-xml-парсинг-для-onvif)
**Разделение разработки по платформам:** [PLATFORMS.md](PLATFORMS.md)

---

## Связанные документы

> **📚 Полный индекс документации:** [DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)

### Основные документы
- **[DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)** - Полный индекс документации
- **[README.md](../README.md)** - Обзор проекта

### Статус и анализ
- **[ONVIF_CLIENT_IMPLEMENTATION_PLAN.md](ONVIF_CLIENT_IMPLEMENTATION_PLAN.md)** - 📋 Детальный пошаговый план реализации ONVIF клиента
- **[MISSING_FUNCTIONALITY.md](MISSING_FUNCTIONALITY.md#onvifclient)** - Детальный анализ нереализованного функционала ONVIF клиента
- **[IMPLEMENTATION_STATUS.md](IMPLEMENTATION_STATUS.md)** - Статус реализации компонентов

### Интеграция и разработка
- **[INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md#1-xml-парсинг-для-onvif)** - Руководство по интеграции библиотек (XML парсинг для ONVIF)
- **[PLATFORMS.md](PLATFORMS.md)** - Разделение разработки по платформам
- **[DEVELOPMENT.md](DEVELOPMENT.md)** - Руководство по разработке

### Дополнительная документация
- **[ONVIF_CLIENT_IMPLEMENTATION_PLAN.md](ONVIF_CLIENT_IMPLEMENTATION_PLAN.md)** - 📋 Детальный пошаговый план реализации
- **[ONVIF_DIGEST_AUTH.md](ONVIF_DIGEST_AUTH.md)** - 🔐 Digest Authentication (в разработке)
- **[ONVIF_UPNP.md](ONVIF_UPNP.md)** - 🔍 UPnP интеграция (запланировано)
- **[ONVIF_TROUBLESHOOTING.md](ONVIF_TROUBLESHOOTING.md)** - 🔧 Решение проблем и FAQ

---

**Последнее обновление:** 26 January 2026

