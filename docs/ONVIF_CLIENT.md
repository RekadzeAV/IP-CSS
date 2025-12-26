# ONVIF Client Documentation

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
    speed = 0.7f
)

// Остановка движения
onvifClient.stopPtz(
    url = "http://192.168.1.100",
    profileToken = "Profile1"
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
    val codec: String?
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

## Примечания

- ONVIF клиент использует SOAP протокол для коммуникации
- Для аутентификации используется WS-Security или Basic HTTP Authentication
- WS-Discovery (обнаружение камер) реализован в упрощенном виде
- Полноценный WS-Discovery требует UDP multicast на 239.255.255.250:3702
- Поддержка различных версий ONVIF (1.0, 2.0, 2.5) зависит от конкретной камеры
- Некоторые камеры могут требовать Digest Authentication вместо Basic

## Статус реализации

**Текущий прогресс:** ~40%

**Реализовано:**
- ✅ Базовые методы (getCapabilities, getDeviceInformation, getProfiles, getStreamUri)
- ✅ PTZ управление (movePtz, stopPtz)
- ✅ testConnection() - реализован

**Не реализовано:**
- ❌ WS-Discovery (discoverCameras возвращает пустой список)
- ❌ Полноценный XML парсинг (используется упрощенный через regex)
- ❌ Digest Authentication (только Basic)
- ❌ Дополнительные ONVIF функции (AbsoluteMove, GetPresets, Imaging, Events)

**Детальный анализ нереализованного функционала:** [MISSING_FUNCTIONALITY.md](MISSING_FUNCTIONALITY.md#onvifclient)
**Руководство по интеграции:** [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md#1-xml-парсинг-для-onvif)
**Разделение разработки по платформам:** [PLATFORMS.md](PLATFORMS.md)

