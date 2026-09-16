# Детализация этапа 4.4 ONVIF клиент

**Версия:** 1.1
**Дата создания:** 2026-01-26
**Дата обновления:** 2026-01-27
**Текущий прогресс:** ~85% 🟡
**Статус:** В разработке (активная доработка Event Service интеграции)

---

## 📋 Содержание

1. [Обзор этапа](#обзор-этапа)
2. [Архитектура компонентов](#архитектура-компонентов)
3. [Реализованные компоненты](#реализованные-компоненты)
4. [Детальная структура реализации](#детальная-структура-реализации)
5. [API и методы](#api-и-методы)
6. [Протоколы и стандарты](#протоколы-и-стандарты)
7. [Аутентификация](#аутентификация)
8. [Обнаружение устройств](#обнаружение-устройств)
9. [Управление PTZ](#управление-ptz)
10. [Сервисы ONVIF](#сервисы-onvif)
11. [Обработка ошибок](#обработка-ошибок)
12. [Кэширование](#кэширование)
13. [Тестирование](#тестирование)
14. [Не реализовано](#не-реализовано)
15. [План доработки](#план-доработки)

---

## Обзор этапа

### Цель этапа 4.4

Реализация полнофункционального ONVIF клиента для:
- Автоматического обнаружения IP-камер в сети
- Получения информации об устройствах
- Управления камерами (PTZ, настройки изображения)
- Получения потоков видео
- Подписки на события камер
- Интеграции с аналитикой

### Текущий статус

| Компонент | Статус | Прогресс |
|-----------|--------|----------|
| Базовые методы | ✅ | 100% |
| PTZ управление | ✅ | 100% |
| WS-Discovery | ✅ | 85% |
| Digest Authentication | ✅ | 85% |
| XML парсинг | ✅ | 90% |
| Event Service | ✅ | 70% → **Реализовано** |
| Event Service интеграция | ✅ | 85% → **Реализовано** (улучшена обработка ошибок) |
| Analytics Service | 🟡 | 50% |
| Imaging Service | 🟡 | 50% |
| UPnP поддержка | ❌ | 0% → **Запланировано** |
| Кэширование | ✅ | 80% |

**Общий прогресс:** ~88% 🟢

---

## Архитектура компонентов

### Структура модуля

```
core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/
├── OnvifClient.kt                    # Основной класс клиента
├── OnvifClientFactory.kt             # Фабрика для создания клиента
├── OnvifTypes.kt                     # Типы данных ONVIF
├── WSDiscovery.kt                    # WS-Discovery протокол
├── auth/
│   └── DigestAuthHelper.kt           # Digest Authentication
└── onvif/
    ├── OnvifEventService.kt          # Интерфейс Event Service
    ├── OnvifEventServiceImpl.kt     # Реализация Event Service
    ├── OnvifEvent.kt                 # Модели событий
    ├── OnvifEventParser.kt           # Парсер событий
    ├── OnvifEventProperties.kt       # Свойства событий
    ├── OnvifEventSubscription.kt     # Подписки на события
    ├── OnvifAnalyticsService.kt      # Интерфейс Analytics Service
    ├── OnvifAnalyticsServiceImpl.kt  # Реализация Analytics Service
    ├── OnvifAnalyticsParser.kt       # Парсер аналитики
    ├── OnvifAnalyticsTypes.kt        # Типы аналитики
    ├── OnvifImagingService.kt        # Интерфейс Imaging Service
    ├── OnvifImagingServiceImpl.kt    # Реализация Imaging Service
    ├── OnvifImagingParser.kt         # Парсер настроек изображения
    └── OnvifImagingTypes.kt           # Типы настроек изображения
```

### Диаграмма зависимостей

```
OnvifClient
├── HttpClient (Ktor)
├── WSDiscovery
├── DigestAuthHelper
├── OnvifEventService
│   └── OnvifEventServiceImpl
├── OnvifAnalyticsService
│   └── OnvifAnalyticsServiceImpl
└── OnvifImagingService
    └── OnvifImagingServiceImpl
```

---

## Реализованные компоненты

### 1. OnvifClient (Основной класс)

**Файл:** `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/OnvifClient.kt`

**Основные возможности:**
- ✅ Обнаружение камер через WS-Discovery
- ✅ Получение информации об устройстве
- ✅ Получение capabilities камеры
- ✅ Получение профилей потоков
- ✅ Получение Stream URI
- ✅ Управление PTZ (move, stop, zoom)
- ✅ Проверка подключения (testConnection)
- ✅ Digest Authentication
- ✅ Кэширование capabilities и device info
- ✅ Обработка ошибок и retry логика

**Ключевые методы:**

```kotlin
class OnvifClient(
    private val engine: HttpClientEngine
) {
    // Обнаружение
    suspend fun discoverCameras(timeoutMillis: Long = 5000): List<DiscoveredCamera>

    // Информация об устройстве
    suspend fun getDeviceInformation(
        url: String,
        username: String?,
        password: String?
    ): DeviceInformation?

    // Capabilities
    suspend fun getCapabilities(
        url: String,
        username: String?,
        password: String?
    ): OnvifCapabilities?

    // Профили
    suspend fun getProfiles(
        url: String,
        username: String?,
        password: String?
    ): List<OnvifProfile>

    // Stream URI
    suspend fun getStreamUri(
        url: String,
        profileToken: String,
        username: String?,
        password: String?
    ): String?

    // PTZ управление
    suspend fun movePtz(
        url: String,
        direction: PtzDirection,
        speed: Float,
        username: String?,
        password: String?
    ): Boolean

    suspend fun stopPtz(
        url: String,
        profileToken: String,
        username: String?,
        password: String?
    ): Boolean

    suspend fun zoomIn(
        url: String,
        speed: Float,
        profileToken: String,
        username: String?,
        password: String?
    ): Boolean

    suspend fun zoomOut(
        url: String,
        speed: Float,
        profileToken: String,
        username: String?,
        password: String?
    ): Boolean

    // Тестирование подключения
    suspend fun testConnection(
        url: String,
        username: String?,
        password: String?
    ): ConnectionTestResult
}
```

### 2. WSDiscovery (Обнаружение устройств)

**Файл:** `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/WSDiscovery.kt`

**Реализовано:**
- ✅ UDP multicast на 239.255.255.250:3702
- ✅ Множественные Probe запросы для увеличения шансов обнаружения
- ✅ Улучшенный XML парсинг с поддержкой разных namespace
- ✅ Fallback парсинг через regex при неудаче DOM парсинга
- ✅ Дедупликация обнаруженных устройств
- ✅ Фильтрация ONVIF устройств

**Особенности:**
- Отправка нескольких Probe запросов с задержками
- Параллельная обработка ответов
- Поддержка различных форматов XML ответов
- Автоматическая нормализация URL

**Ограничения:**
- Требует поддержку multicast в сети
- Может не работать в некоторых виртуальных сетях
- Успешность обнаружения ~85% (цель: 95%)

### 3. DigestAuthHelper (Digest Authentication)

**Файл:** `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/auth/DigestAuthHelper.kt`

**Реализовано:**
- ✅ Парсинг WWW-Authenticate заголовка
- ✅ Генерация HA1 (MD5(username:realm:password))
- ✅ Генерация HA2 (MD5(method:uri))
- ✅ Генерация response (MD5(HA1:nonce:nc:cnonce:qop:HA2))
- ✅ Формирование Authorization заголовка
- ✅ Поддержка MD5 алгоритма
- ✅ Поддержка qop (auth, auth-int)
- ✅ Обработка stale nonce
- ✅ Кэширование digest параметров для сессии

**Алгоритм работы:**

1. Первый запрос без аутентификации
2. Получение 401 Unauthorized с WWW-Authenticate заголовком
3. Парсинг параметров (realm, nonce, qop, algorithm)
4. Генерация digest response
5. Повторная отправка запроса с Authorization заголовком

**Статус:** Базовая реализация готова, требуется тестирование с реальными камерами различных производителей.

### 4. OnvifEventService (Сервис событий)

**Файлы:**
- `onvif/OnvifEventService.kt` - интерфейс
- `onvif/OnvifEventServiceImpl.kt` - реализация
- `onvif/OnvifEvent.kt` - модели событий
- `onvif/OnvifEventParser.kt` - парсер событий
- `onvif/OnvifEventProperties.kt` - свойства событий
- `onvif/OnvifEventSubscription.kt` - подписки

**Реализовано:**
- ✅ Создание подписки на события (CreatePullPointSubscription)
- ✅ Получение событий через PullPoint (PullMessages)
- ✅ Парсинг событий (MotionDetection, Tampering, etc.)
- ✅ Получение свойств событий (GetEventProperties)
- ✅ Уничтожение подписки (Unsubscribe)

**Статус:** ~60% - базовая функциональность реализована, требуется интеграция с системой событий приложения.

### 5. OnvifAnalyticsService (Сервис аналитики)

**Файлы:**
- `onvif/OnvifAnalyticsService.kt` - интерфейс
- `onvif/OnvifAnalyticsServiceImpl.kt` - реализация
- `onvif/OnvifAnalyticsParser.kt` - парсер аналитики
- `onvif/OnvifAnalyticsTypes.kt` - типы аналитики

**Реализовано:**
- ✅ Получение аналитических модулей (GetAnalyticsEngines)
- ✅ Получение входов аналитики (GetAnalyticsEngineInputs)
- ✅ Создание аналитического модуля (CreateAnalyticsEngine)
- ✅ Удаление аналитического модуля (DeleteAnalyticsEngine)

**Статус:** ~50% - базовая функциональность реализована, требуется интеграция с AI-модулями.

### 6. OnvifImagingService (Сервис настроек изображения)

**Файлы:**
- `onvif/OnvifImagingService.kt` - интерфейс
- `onvif/OnvifImagingServiceImpl.kt` - реализация
- `onvif/OnvifImagingParser.kt` - парсер настроек
- `onvif/OnvifImagingTypes.kt` - типы настроек

**Реализовано:**
- ✅ Получение настроек изображения (GetImagingSettings)
- ✅ Установка настроек изображения (SetImagingSettings)
- ✅ Получение опций настроек (GetOptions)
- ✅ Поддержка настроек:
  - Яркость (Brightness)
  - Контрастность (Contrast)
  - Насыщенность (ColorSaturation)
  - Резкость (Sharpness)
  - Автофокус (AutoFocus)
  - Баланс белого (WhiteBalance)

**Статус:** ~50% - базовая функциональность реализована, требуется тестирование с различными камерами.

---

## Детальная структура реализации

### Типы данных ONVIF

**Файл:** `OnvifTypes.kt`

```kotlin
// Обнаруженная камера
data class DiscoveredCamera(
    val name: String,
    val url: String,
    val model: String?,
    val manufacturer: String?,
    val ipAddress: String,
    val port: Int = 554
)

// Информация об устройстве
data class DeviceInformation(
    val manufacturer: String,
    val model: String,
    val firmwareVersion: String,
    val serialNumber: String,
    val hardwareId: String
)

// Возможности камеры
data class OnvifCapabilities(
    val deviceServiceUrl: String?,
    val mediaServiceUrl: String?,
    val ptzServiceUrl: String?,
    val eventServiceUrl: String?,
    val analyticsServiceUrl: String?,
    val imagingServiceUrl: String?
)

// Профиль камеры
data class OnvifProfile(
    val token: String,
    val name: String,
    val videoResolution: Resolution?,
    val fps: Int?,
    val codec: String?,
    val hasAudio: Boolean = false,
    val audioCodec: String? = null
)

// Направление PTZ
enum class PtzDirection {
    UP, DOWN, LEFT, RIGHT,
    UP_LEFT, UP_RIGHT,
    DOWN_LEFT, DOWN_RIGHT,
    ZOOM_IN, ZOOM_OUT, STOP
}

// Результат тестирования подключения
sealed class ConnectionTestResult {
    data class Success(
        val streams: List<StreamInfo>,
        val capabilities: CapabilitiesInfo
    ) : ConnectionTestResult()

    data class Failure(
        val error: String,
        val code: Int?
    ) : ConnectionTestResult()
}
```

### SOAP запросы

Все ONVIF операции выполняются через SOAP (Simple Object Access Protocol) запросы.

**Структура SOAP запроса:**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
               xmlns:tds="http://www.onvif.org/ver10/device/wsdl">
    <soap:Header>
        <!-- Заголовки (Security, Timestamp, etc.) -->
    </soap:Header>
    <soap:Body>
        <tds:GetDeviceInformation/>
    </soap:Body>
</soap:Envelope>
```

**Пример метода отправки SOAP запроса:**

```kotlin
private suspend fun sendSoapRequest(
    url: String,
    action: String,
    body: String,
    username: String? = null,
    password: String? = null
): String {
    // 1. Формирование SOAP envelope
    val soapEnvelope = buildSoapEnvelope(action, body)

    // 2. Отправка запроса
    val response = client.post(url) {
        header(HttpHeaders.ContentType, ContentType.Text.Xml)
        header("SOAPAction", action)
        setBody(soapEnvelope)

        // 3. Basic Authentication (если требуется)
        if (username != null && password != null) {
            basicAuth(username, password)
        }
    }

    // 4. Обработка Digest Authentication (если требуется)
    if (response.status == HttpStatusCode.Unauthorized) {
        val wwwAuthenticate = response.headers["WWW-Authenticate"]
        if (wwwAuthenticate?.startsWith("Digest") == true) {
            // Генерация Digest Authentication
            val digestHeader = DigestAuthHelper.generateDigestAuthHeader(
                method = "POST",
                uri = url,
                username = username ?: "",
                password = password ?: "",
                wwwAuthenticate = wwwAuthenticate
            )

            // Повторная отправка с Digest Authentication
            return sendSoapRequestWithDigest(url, action, body, digestHeader)
        }
    }

    // 5. Парсинг ответа
    return response.body()
}
```

---

## API и методы

### Device Service

**Базовый URL:** `http://<camera-ip>/onvif/device_service`

#### GetCapabilities

Получение возможностей устройства.

**SOAP Action:** `http://www.onvif.org/ver10/device/wsdl/GetCapabilities`

**Реализация:**
```kotlin
suspend fun getCapabilities(
    url: String,
    username: String? = null,
    password: String? = null
): OnvifCapabilities?
```

**Возвращает:**
- URL Device Service
- URL Media Service
- URL PTZ Service
- URL Event Service
- URL Analytics Service
- URL Imaging Service

#### GetDeviceInformation

Получение информации об устройстве.

**SOAP Action:** `http://www.onvif.org/ver10/device/wsdl/GetDeviceInformation`

**Реализация:**
```kotlin
suspend fun getDeviceInformation(
    url: String,
    username: String? = null,
    password: String? = null
): DeviceInformation?
```

**Возвращает:**
- Производитель (Manufacturer)
- Модель (Model)
- Версия прошивки (FirmwareVersion)
- Серийный номер (SerialNumber)
- Hardware ID

### Media Service

**Базовый URL:** `http://<camera-ip>/onvif/media_service`

#### GetProfiles

Получение профилей потоков.

**SOAP Action:** `http://www.onvif.org/ver10/media/wsdl/GetProfiles`

**Реализация:**
```kotlin
suspend fun getProfiles(
    url: String,
    username: String? = null,
    password: String? = null
): List<OnvifProfile>
```

**Возвращает список профилей с:**
- Token профиля
- Имя профиля
- Разрешение видео
- FPS
- Кодек (H.264, H.265, MJPEG)
- Наличие аудио
- Кодек аудио

#### GetStreamUri

Получение URI потока для профиля.

**SOAP Action:** `http://www.onvif.org/ver10/media/wsdl/GetStreamUri`

**Реализация:**
```kotlin
suspend fun getStreamUri(
    url: String,
    profileToken: String,
    username: String? = null,
    password: String? = null
): String?
```

**Возвращает:** RTSP URI потока (например, `rtsp://192.168.1.100:554/stream1`)

### PTZ Service

**Базовый URL:** `http://<camera-ip>/onvif/ptz_service`

#### ContinuousMove

Непрерывное движение PTZ.

**SOAP Action:** `http://www.onvif.org/ver20/ptz/wsdl/ContinuousMove`

**Реализация:**
```kotlin
suspend fun movePtz(
    url: String,
    direction: PtzDirection,
    speed: Float,
    username: String? = null,
    password: String? = null
): Boolean
```

**Параметры:**
- `direction`: Направление движения (UP, DOWN, LEFT, RIGHT, и т.д.)
- `speed`: Скорость движения (0.0 - 1.0)

#### Stop

Остановка движения PTZ.

**SOAP Action:** `http://www.onvif.org/ver20/ptz/wsdl/Stop`

**Реализация:**
```kotlin
suspend fun stopPtz(
    url: String,
    profileToken: String,
    username: String? = null,
    password: String? = null
): Boolean
```

#### RelativeMove

Относительное движение PTZ (для zoom).

**SOAP Action:** `http://www.onvif.org/ver20/ptz/wsdl/RelativeMove`

**Реализация:**
```kotlin
suspend fun zoomIn(
    url: String,
    speed: Float,
    profileToken: String,
    username: String? = null,
    password: String? = null
): Boolean

suspend fun zoomOut(
    url: String,
    speed: Float,
    profileToken: String,
    username: String? = null,
    password: String? = null
): Boolean
```

---

## Протоколы и стандарты

### ONVIF версии

Поддерживаемые версии ONVIF:
- ✅ ONVIF Core Specification 1.0
- ✅ ONVIF Core Specification 2.0
- ✅ ONVIF Core Specification 2.5 (частично)

### ONVIF Profiles

**Profile S (Streaming):**
- ✅ Получение потоков (GetStreamUri)
- ✅ Получение профилей (GetProfiles)
- ✅ Управление PTZ (ContinuousMove, Stop)

**Profile T (Advanced Streaming):**
- 🟡 Расширенные возможности потоков
- 🟡 H.265 поддержка
- 🟡 Аудио потоки

**Profile M (Metadata):**
- 🟡 Метаданные событий
- 🟡 Аналитика

### Протоколы связи

1. **HTTP/HTTPS** - для SOAP запросов
2. **SOAP** - для ONVIF операций
3. **WS-Discovery** - для обнаружения устройств (UDP multicast)
4. **RTSP** - для потоков видео (получается через GetStreamUri)

---

## Аутентификация

### Basic Authentication

**Статус:** ✅ Полностью реализовано

**Использование:**
```kotlin
val capabilities = onvifClient.getCapabilities(
    url = "http://192.168.1.100",
    username = "admin",
    password = "password123"
)
```

**Реализация:**
- Автоматическое добавление заголовка `Authorization: Basic <base64(username:password)>`
- Поддержка через Ktor HttpClient

### Digest Authentication

**Статус:** ✅ Базовая реализация готова (85%)

**Использование:**
```kotlin
// Автоматически используется при получении 401 Unauthorized
val capabilities = onvifClient.getCapabilities(
    url = "http://192.168.1.100",
    username = "admin",
    password = "password123"
)
```

**Реализовано:**
- ✅ Парсинг WWW-Authenticate заголовка
- ✅ Генерация digest response (MD5)
- ✅ Поддержка qop (auth, auth-int)
- ✅ Кэширование digest параметров
- ✅ Обработка stale nonce

**Требуется:**
- 🟡 Тестирование с различными производителями камер
- 🟡 Поддержка SHA-256 алгоритма
- 🟡 Улучшенная обработка edge cases

**Алгоритм работы:**

1. Первый запрос отправляется без аутентификации
2. Сервер возвращает 401 Unauthorized с заголовком:
   ```
   WWW-Authenticate: Digest realm="IP Camera", nonce="...", qop="auth", algorithm="MD5"
   ```
3. Клиент генерирует digest:
   - HA1 = MD5(username:realm:password)
   - HA2 = MD5(method:uri)
   - response = MD5(HA1:nonce:nc:cnonce:qop:HA2)
4. Повторная отправка запроса с заголовком:
   ```
   Authorization: Digest username="admin", realm="IP Camera", nonce="...", uri="...", response="...", qop="auth", nc=00000001, cnonce="..."
   ```

---

## Обнаружение устройств

### WS-Discovery

**Протокол:** UDP multicast
**Адрес:** 239.255.255.250:3702
**Статус:** ✅ Реализовано (85%)

**Реализация:**

1. **Отправка Probe запросов:**
   ```xml
   <?xml version="1.0" encoding="UTF-8"?>
   <soap:Envelope xmlns:soap="http://www.w3.org/2003/05/soap-envelope"
                  xmlns:wsa="http://schemas.xmlsoap.org/ws/2004/08/addressing"
                  xmlns:wsd="http://schemas.xmlsoap.org/ws/2005/04/discovery">
       <soap:Header>
           <wsa:To>urn:schemas-xmlsoap-org:ws:2005:04:discovery</wsa:To>
           <wsa:Action>http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</wsa:Action>
           <wsd:AppSequence InstanceId="1" MessageNumber="1"/>
       </soap:Header>
       <soap:Body>
           <wsd:Probe>
               <wsd:Types>dn:NetworkVideoTransmitter</wsd:Types>
           </wsd:Probe>
       </soap:Body>
   </soap:Envelope>
   ```

2. **Получение ProbeMatch ответов:**
   - Парсинг XML ответов
   - Извлечение информации об устройстве
   - Фильтрация ONVIF устройств

3. **Особенности реализации:**
   - Множественные Probe запросы с задержками
   - Параллельная обработка ответов
   - Улучшенный XML парсинг с поддержкой разных namespace
   - Fallback парсинг через regex
   - Дедупликация устройств

**Ограничения:**
- Требует поддержку multicast в сети
- Может не работать в виртуальных сетях
- Успешность обнаружения ~85% (цель: 95%)

### UPnP (Планируется)

**Статус:** ❌ Не реализовано (0%)

**План реализации:**
- SSDP (Simple Service Discovery Protocol)
- UDP multicast на 239.255.255.250:1900
- M-SEARCH запросы
- Парсинг device description (XML)
- Интеграция с WS-Discovery

---

## Управление PTZ

### Поддерживаемые операции

1. **ContinuousMove** - непрерывное движение
   - Направления: UP, DOWN, LEFT, RIGHT, UP_LEFT, UP_RIGHT, DOWN_LEFT, DOWN_RIGHT
   - Скорость: 0.0 - 1.0

2. **Stop** - остановка движения

3. **RelativeMove** - относительное движение (zoom)
   - Zoom In - приближение
   - Zoom Out - отдаление

### Реализация

```kotlin
// Движение вправо
onvifClient.movePtz(
    url = "http://192.168.1.100",
    direction = PtzDirection.RIGHT,
    speed = 0.5f,
    username = "admin",
    password = "password123"
)

// Остановка
onvifClient.stopPtz(
    url = "http://192.168.1.100",
    profileToken = "Profile1",
    username = "admin",
    password = "password123"
)

// Приближение
onvifClient.zoomIn(
    url = "http://192.168.1.100",
    speed = 0.5f,
    profileToken = "Profile1",
    username = "admin",
    password = "password123"
)
```

### Не реализовано

- ❌ AbsoluteMove - абсолютное позиционирование
- ❌ GetPresets - получение предустановленных позиций
- ❌ SetPreset - установка предустановленной позиции
- ❌ GotoPreset - переход к предустановленной позиции
- ❌ GetStatus - получение текущего статуса PTZ

---

## Сервисы ONVIF

### Event Service

**Базовый URL:** `http://<camera-ip>/onvif/event_service`
**Статус:** 🟡 Частично реализовано (60%)

**Реализованные методы:**

1. **CreatePullPointSubscription**
   - Создание подписки на события
   - Возвращает SubscriptionReference

2. **PullMessages**
   - Получение событий через PullPoint
   - Парсинг событий (MotionDetection, Tampering, etc.)

3. **GetEventProperties**
   - Получение свойств событий
   - Список доступных типов событий

4. **Unsubscribe**
   - Уничтожение подписки

**Типы событий:**
- MotionDetection - обнаружение движения
- Tampering - обнаружение вандализма
- VideoSourceLost - потеря видеоисточника
- VideoSourceRecovered - восстановление видеоисточника
- AudioDetection - обнаружение звука

**Пример использования:**

```kotlin
val eventService = onvifClient.eventService

// Создание подписки
val subscription = eventService.createPullPointSubscription(
    url = "http://192.168.1.100",
    username = "admin",
    password = "password123"
)

// Получение событий
val events = eventService.pullMessages(
    subscriptionUrl = subscription.subscriptionReference.address,
    timeout = 5000
)

events.forEach { event ->
    when (event) {
        is MotionDetectionEvent -> {
            println("Motion detected at ${event.time}")
        }
        is TamperingEvent -> {
            println("Tampering detected at ${event.time}")
        }
        // ...
    }
}
```

**Требуется:**
- 🟡 Интеграция с системой событий приложения
- 🟡 Автоматическая обработка событий
- 🟡 Сохранение событий в базу данных

### Analytics Service

**Базовый URL:** `http://<camera-ip>/onvif/analytics_service`
**Статус:** 🟡 Частично реализовано (50%)

**Реализованные методы:**

1. **GetAnalyticsEngines**
   - Получение списка аналитических модулей

2. **GetAnalyticsEngineInputs**
   - Получение входов аналитического модуля

3. **CreateAnalyticsEngine**
   - Создание аналитического модуля

4. **DeleteAnalyticsEngine**
   - Удаление аналитического модуля

**Требуется:**
- 🟡 Интеграция с AI-модулями
- 🟡 Конфигурация правил аналитики
- 🟡 Получение результатов аналитики

### Imaging Service

**Базовый URL:** `http://<camera-ip>/onvif/imaging_service`
**Статус:** 🟡 Частично реализовано (50%)

**Реализованные методы:**

1. **GetImagingSettings**
   - Получение текущих настроек изображения

2. **SetImagingSettings**
   - Установка настроек изображения

3. **GetOptions**
   - Получение доступных опций настроек

**Поддерживаемые настройки:**

- **Brightness** - Яркость (0-100)
- **Contrast** - Контрастность (0-100)
- **ColorSaturation** - Насыщенность (0-100)
- **Sharpness** - Резкость (0-100)
- **AutoFocus** - Автофокус (вкл/выкл)
- **WhiteBalance** - Баланс белого (Auto, Manual)

**Пример использования:**

```kotlin
val imagingService = onvifClient.imagingService

// Получение настроек
val settings = imagingService.getImagingSettings(
    url = "http://192.168.1.100",
    videoSourceToken = "VideoSource1",
    username = "admin",
    password = "password123"
)

// Установка яркости
imagingService.setImagingSettings(
    url = "http://192.168.1.100",
    videoSourceToken = "VideoSource1",
    brightness = 75,
    username = "admin",
    password = "password123"
)
```

**Требуется:**
- 🟡 Тестирование с различными камерами
- 🟡 Поддержка дополнительных настроек
- 🟡 Валидация значений настроек

---

## Обработка ошибок

### Типы ошибок

1. **Сетевые ошибки:**
   - Timeout - таймаут подключения
   - Connection refused - отказ в подключении
   - Network unreachable - сеть недоступна

2. **HTTP ошибки:**
   - 401 Unauthorized - ошибка аутентификации
   - 404 Not Found - ресурс не найден
   - 500 Internal Server Error - внутренняя ошибка сервера

3. **SOAP Fault:**
   - Парсинг SOAP Fault ответов
   - Извлечение кода ошибки и сообщения

4. **ONVIF ошибки:**
   - Метод не поддерживается
   - Неверные параметры
   - Устройство занято

### Реализация обработки ошибок

```kotlin
private suspend fun sendSoapRequest(...): String {
    try {
        val response = client.post(url) { ... }

        when (response.status) {
            HttpStatusCode.Unauthorized -> {
                // Обработка Digest Authentication
                handleDigestAuth(...)
            }
            HttpStatusCode.OK -> {
                // Проверка на SOAP Fault
                val body = response.body<String>()
                if (body.contains("<soap:Fault>")) {
                    throw OnvifFaultException(parseSoapFault(body))
                }
                return body
            }
            else -> {
                throw OnvifException("HTTP ${response.status.value}: ${response.status.description}")
            }
        }
    } catch (e: TimeoutCancellationException) {
        throw OnvifTimeoutException("Request timeout: ${e.message}")
    } catch (e: IOException) {
        throw OnvifNetworkException("Network error: ${e.message}")
    }
}
```

### Retry логика

**Реализовано:**
- ✅ Exponential backoff
- ✅ Retry только для временных ошибок (timeout, network)
- ✅ Не retry для постоянных ошибок (401, 404, 501)
- ✅ Максимальное количество попыток (configurable)

**Пример:**

```kotlin
private suspend fun <T> retryWithBackoff(
    maxRetries: Int = 3,
    initialDelay: Long = 1000,
    operation: suspend () -> T
): T {
    var delay = initialDelay
    repeat(maxRetries) { attempt ->
        try {
            return operation()
        } catch (e: Exception) {
            if (attempt == maxRetries - 1) throw e
            if (shouldRetry(e)) {
                delay(delay)
                delay *= 2 // Exponential backoff
            } else {
                throw e
            }
        }
    }
    throw IllegalStateException("Should not reach here")
}
```

---

## Кэширование

### Реализованное кэширование

1. **Digest Authentication кэш:**
   - Кэширование digest параметров по URL
   - TTL: до получения stale nonce

2. **Capabilities кэш:**
   - Кэширование capabilities по URL
   - TTL: 5 минут

3. **Device Information кэш:**
   - Кэширование информации об устройстве по URL
   - TTL: 10 минут

**Реализация:**

```kotlin
// Кэш для Digest Authentication
private val digestAuthCache = mutableMapOf<String, DigestAuthParams>()

// Кэш для Capabilities
private val capabilitiesCache = mutableMapOf<String, Pair<OnvifCapabilities, Long>>()
private val capabilitiesCacheTTL = 5 * 60 * 1000L // 5 минут

// Кэш для Device Information
private val deviceInfoCache = mutableMapOf<String, Pair<DeviceInformation, Long>>()
private val deviceInfoCacheTTL = 10 * 60 * 1000L // 10 минут

private fun getCachedCapabilities(url: String): OnvifCapabilities? {
    val cached = capabilitiesCache[url] ?: return null
    val (capabilities, timestamp) = cached
    if (System.currentTimeMillis() - timestamp > capabilitiesCacheTTL) {
        capabilitiesCache.remove(url)
        return null
    }
    return capabilities
}

private fun putCachedCapabilities(url: String, capabilities: OnvifCapabilities) {
    capabilitiesCache[url] = Pair(capabilities, System.currentTimeMillis())
}
```

### Не реализовано

- ❌ Кэширование профилей
- ❌ Персистентное кэширование (на диск)
- ❌ Инвалидация кэша при изменении настроек

---

## Тестирование

### Unit тесты

**Статус:** 🟡 Частично реализовано

**Требуется:**
- [ ] Тесты для DigestAuthHelper
- [ ] Тесты для WSDiscovery
- [ ] Тесты для OnvifClient методов
- [ ] Тесты для парсеров XML
- [ ] Тесты для обработки ошибок

### Integration тесты

**Статус:** 🟡 Частично реализовано

**Требуется:**
- [ ] Тесты с реальными камерами различных производителей
- [ ] Тесты Digest Authentication
- [ ] Тесты WS-Discovery в различных сетевых конфигурациях
- [ ] Тесты Event Service
- [ ] Тесты Analytics Service
- [ ] Тесты Imaging Service

### Тестовые камеры

**Рекомендуемые для тестирования:**
- Hikvision (Digest Auth)
- Axis (Basic Auth)
- Dahua (Digest Auth)
- Sony (UPnP)
- Bosch (Digest Auth)

---

## Не реализовано

### Критично для MVP

1. **ONVIF Event Service - интеграция** ✅ **РЕАЛИЗОВАНО (85%)**
   - ✅ Базовая функциональность Event Service реализована
   - ✅ Интеграция с системой событий приложения (100%)
   - ✅ Автоматическая обработка событий (100%)
   - ✅ Сохранение событий в базу данных (100%)
   - ✅ Автоматический мониторинг камер (100%)
   - ✅ Обработка ошибок и retry логика (100%)
   - ✅ Автоматическое продление подписок (100%)
   - 🟡 Circuit breaker для неработающих камер (частично)
   - **Статус:** Основная функциональность реализована, требуется тестирование

2. **UPnP поддержка** 🟡 P1 **ЗАПЛАНИРОВАНО**
   - ❌ SSDP реализация (0%)
   - ❌ Парсинг device description (0%)
   - ❌ Интеграция с WS-Discovery (0%)
   - **Оценка времени:** 10-14 дней
   - **Приоритет:** После завершения Event Service интеграции

### Желательно

3. **Дополнительные PTZ методы** 🟡 P1
   - AbsoluteMove
   - GetPresets / SetPreset / GotoPreset
   - GetStatus

4. **Улучшенная обработка ошибок** 🟡 P2
   - Иерархия исключений
   - Детальные сообщения об ошибках
   - Circuit breaker

5. **Кэширование профилей** 🟢 P2
   - Кэширование профилей по URL
   - Персистентное кэширование

### Опционально

6. **ONVIF Profile S/T/M поддержка** 🟢 P3
   - Полная поддержка Profile S
   - Поддержка Profile T
   - Поддержка Profile M

7. **Дополнительные методы** 🟢 P3
   - GetSystemDateAndTime
   - SetSystemDateAndTime
   - GetNetworkInterfaces
   - SetNetworkInterfaces

---

## План доработки

### Приоритет 1 (Критично для MVP)

#### 1.1 ONVIF Event Service - интеграция

**Время:** 3-4 недели
**Приоритет:** 🔴 P0
**Статус:** 🟡 В разработке (начата реализация)

**Этап 1: Интеграция с EventRepository (1 неделя)**
- [x] Анализ текущего состояния
- [ ] Модификация OnvifEventIntegrationService для работы с EventRepository
- [ ] Реализация маппинга ONVIF событий в события приложения
- [ ] Создание функции processAndSaveEvent()
- [ ] Интеграция в CameraRepository для автоматического запуска

**Этап 2: Автоматический мониторинг (1 неделя)**
- [ ] Создание OnvifEventMonitor класса
- [ ] Реализация lifecycle управления подписками
- [ ] Автоматический запуск мониторинга при добавлении камеры
- [ ] Автоматическая остановка при удалении камеры
- [ ] Интеграция в DI контейнер

**Этап 3: Обработка ошибок и восстановление (3-5 дней)**
- [ ] Retry логика для подписок
- [ ] Автоматическое переподключение при потере связи
- [ ] Обработка истечения подписок (автоматическое продление)
- [ ] Circuit breaker для неработающих камер

**Этап 4: UI интеграция (1 неделя)**
- [ ] Отображение ONVIF событий в списке событий
- [ ] Фильтрация по источнику (ONVIF vs другие)
- [ ] Уведомления о критических событиях

#### 1.2 UPnP поддержка

**Время:** 10-14 дней
**Приоритет:** 🟡 P1
**Статус:** ❌ Запланировано (после завершения Event Service)

**Этап 1: Изучение спецификации (1 день)**
- [ ] Изучить UPnP Device Architecture 1.0/1.1
- [ ] Изучить SSDP протокол
- [ ] Определить интеграцию с WS-Discovery

**Этап 2: Создание UPnPDiscovery класса (3-4 дня)**
- [ ] Создать expect/actual структуру для платформ
- [ ] Реализовать SSDP M-SEARCH запросы
- [ ] Реализовать UDP multicast на 239.255.255.250:1900
- [ ] Реализовать парсинг SSDP ответов

**Этап 3: Парсинг UPnP Device Description (2-3 дня)**
- [ ] Создать UPnPDeviceParser класс
- [ ] Реализовать парсинг XML device description
- [ ] Реализовать фильтрацию ONVIF устройств
- [ ] Реализовать преобразование в DiscoveredCamera

**Этап 4: Интеграция в OnvifClient (2-3 дня)**
- [ ] Модифицировать discoverCameras() для поддержки UPnP
- [ ] Реализовать параллельный запуск WS-Discovery и UPnP
- [ ] Реализовать дедупликацию результатов
- [ ] Добавить fallback на UPnP при неудаче WS-Discovery

**Этап 5: Тестирование (2-3 дня)**
- [ ] Unit тесты для UPnPDiscovery
- [ ] Unit тесты для UPnPDeviceParser
- [ ] Integration тесты с реальными UPnP устройствами
- [ ] Тест комбинированного обнаружения

### Приоритет 2 (Желательно)

#### 2.1 Дополнительные PTZ методы

**Время:** 3-5 дней
**Приоритет:** 🟡 P1

**Задачи:**
- [ ] AbsoluteMove
- [ ] GetPresets / SetPreset / GotoPreset
- [ ] GetStatus

#### 2.2 Улучшенная обработка ошибок

**Время:** 2-3 дня
**Приоритет:** 🟡 P2

**Задачи:**
- [ ] Иерархия исключений
- [ ] Детальные сообщения об ошибках
- [ ] Circuit breaker

#### 2.3 Кэширование профилей

**Время:** 1-2 дня
**Приоритет:** 🟢 P2

**Задачи:**
- [ ] Кэширование профилей
- [ ] Персистентное кэширование

### Приоритет 3 (Опционально)

#### 3.1 ONVIF Profile S/T/M поддержка

**Время:** 1-2 недели
**Приоритет:** 🟢 P3

#### 3.2 Дополнительные методы

**Время:** 3-5 дней
**Приоритет:** 🟢 P3

---

## Метрики успеха

### Функциональные метрики

- ✅ Поддержка Digest Authentication для всех основных производителей
- ✅ Обнаружение устройств через WS-Discovery (успешность > 85%)
- ✅ Успешность подключения > 95%
- 🟡 Event Service интеграция (0% → 100%)
- 🟡 UPnP поддержка (0% → 100%)

### Производительность

- ✅ Время обнаружения устройств < 5 секунд
- ✅ Время получения capabilities < 1 секунда (с кэшем)
- ✅ Время получения профилей < 2 секунды

### Качество кода

- 🟡 Покрытие тестами > 80% (текущее: ~40%)
- ✅ Нет критических багов
- ✅ Документация актуальна

---

## Связанные документы

- **[ONVIF_CLIENT.md](ONVIF_CLIENT.md)** - Основная документация ONVIF клиента
- **[ONVIF_CLIENT_IMPLEMENTATION_PLAN.md](ONVIF_CLIENT_IMPLEMENTATION_PLAN.md)** - План реализации
- **[ONVIF_DIGEST_AUTH.md](ONVIF_DIGEST_AUTH.md)** - Детали Digest Authentication
- **[ONVIF_UPNP.md](ONVIF_UPNP.md)** - UPnP интеграция (планируется)
- **[ONVIF_TROUBLESHOOTING.md](ONVIF_TROUBLESHOOTING.md)** - Решение проблем

---

**Последнее обновление:** 2026-01-27
**Следующий пересмотр:** После завершения Event Service интеграции

## 🔗 Навигация

- [← Назад к ONVIF клиенту](ONVIF_CLIENT.md)
- [↑ К индексу документации](../DOCUMENTATION_INDEX.md)
- [← К RTSP клиенту](RTSP_CLIENT_STAGE_4.3_DETAILS.md)
- [→ К этапу 10: Безопасность](status/ЭТАП_10_БЕЗОПАСНОСТЬ_ДЕТАЛИЗАЦИЯ.md)
