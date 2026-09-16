# Анализ и исправления сборки core/network

## Что исправлено (по запросу)

### 1. SettingsApiService, UserApiService, StreamApiService

**Проблема:** Использовалась старая сигнатура ApiClient с `responseType` и `queryParameters` у `post()`; тип возврата ожидался не `ApiResult<T>`.

**Решение:**
- Вызовы переведены на текущий API: `apiClient.get<T>(path, queryParameters)`, `apiClient.post<T>(path, body)`, `apiClient.put<T>(path, body)`, `apiClient.delete<T>(path)`.
- Результат разворачивается через расширение `getOrThrow()` (`.fold(onSuccess = { it }, onError = { throw it })`).
- Для `resetSettings(category)` query-параметр передаётся в path: `"$basePath/reset?category=$category"` (у `post()` нет параметра queryParameters).
- В `StreamApiService.setStreamQuality` параметр `quality` передаётся в path: `"...?quality=$quality"`.

### 2. NetworkMetrics.kt — конфликт перегрузок getRecentMetrics

**Проблема:** Две перегрузки `getRecentMetrics(minutes: Int)` и `getRecentMetrics(count: Int = 100)` не различаются при вызове с одним аргументом (например, `getRecentMetrics(5)`).

**Решение:** Переименованы в:
- `getRecentMetricsByMinutes(minutes: Int)` — метрики за последние N минут;
- `getRecentMetricsByCount(count: Int = 100)` — последние N записей.

В `NetworkMetricsTest.kt` вызов обновлён на `getRecentMetricsByCount(50)`.

### 3. OnvifAnalyticsTypes.kt — сериализатор для Any

**Проблема:** Поле `settings: Map<String, Any>` в `OnvifAnalyticsEngineConfiguration` не сериализуется (нет сериализатора для `Any`).

**Решение:** Тип заменён на `Map<String, String>`. Для сложных значений можно передавать JSON-строку. Комментарий в коде обновлён.

### 4. CertificatePinner.jvm.kt — "Declaration must be marked with 'actual'"

**Проблема:** Для таргета `jvm("desktop")` компилятор ожидает `actual` в source set **desktopMain**, а реализация была только в **jvmMain**. При сборке desktop используется desktopMain, поэтому actual из jvmMain не подставлялся.

**Решение:**
- Добавлен файл `core/network/src/desktopMain/.../security/CertificatePinner.kt` с полной actual-реализацией для desktop (JVM).
- Удалён `core/network/src/jvmMain/.../CertificatePinner.jvm.kt`, чтобы не было двух actual для одного expect при сборке desktop.
- В desktop-реализации убран лишний блок `Java.create { config { } }`, используется `Java.create()`.

---

## Дополнительно исправлено в ApiClient.kt

- **Логгер в Logging plugin:** В callback для Ktor Logging использовалось поле `logger`, которое внутри объекта разрешалось в сам объект. Переименовано файловое свойство в `apiClientLogging` и в callback вызывается `apiClientLogging.info { message }`.
- **ApiError.HttpError:** Во всех вызовах параметр `message` заменён на `errorMessage` (в соответствии с объявлением `HttpError(val statusCode, val errorMessage, val body?)`).
- **Импорт для multipart:** Добавлен `import io.ktor.client.request.forms.*` для `MultiPartFormDataContent` и `formData` (если появятся ошибки в uploadMultipart — возможно, понадобится уточнить API Ktor для commonMain).

---

## Что остаётся (вне четырёх пунктов)

Сборка `:core:network:compileKotlinDesktop` по-прежнему может падать из-за:

1. **ApiClient.kt — Public-API inline**
   - Публичные `suspend inline fun <reified T> get/post/put/delete/upload/uploadMultipart` при инлайнинге в другие модули обращаются к `private`/internal членам (`executeRequest`, `handleResponse`, `cache`, `json`, константы и т.д.). В KMP это запрещено.
   - **Варианты:** убрать `inline` и перейти на API с явной передачей `KSerializer<T>`; либо сделать все используемые из инлайна члены публичными (в т.ч. типы вроде `ResponseCache`).

2. **OnvifClient.kt**
   - Отсутствуют ссылки на `xml`, `Xml`, `XmlSerialName`, `XmlElement`, `XmlAttribute` (вероятно, `kotlinx.serialization.xml` или другой артефакт не подключён/не используется в commonMain).
   - Ошибки по expect-классам `WSDiscovery`, `UPnPDiscovery` (нет default-конструктора, другие сигнатуры).
   - Параметры `ipAddress`/`port` не найдены; `requestTimeoutMillis`/`connectTimeoutMillis` и т.д.

3. **ChunkingManager.kt**
   - Обращение к `private val receivedChunks` в `ChunkedMessage` — нужно ослабить видимость или добавить доступ через метод.

4. **Методы clearAllCaches в OnvifClient.kt**
   - Конфликт перегрузок `clearAllCaches()` — требуется переименование или объединение сигнатур.

Эти места требуют отдельного прохода по модулю core/network (и при необходимости по зависимостям/expect-actual для ONVIF и XML).

---

## Дополнительные исправления (expect/actual, Regex, typealias)

### 5. OnvifClient: WSDiscovery и UPnPDiscovery для таргета desktop

**Проблема:** Для `jvm("desktop")` expect-классы `WSDiscovery` и `UPnPDiscovery` не находили actual («does not have default constructor»), т.к. платформенный source set — `desktopMain`, а actual были только в `jvmMain`.

**Решение:**
- В `jvmMain` реализация вынесена в базовые классы без `actual`: `WSDiscoveryJvmImpl` и `UPnPDiscoveryJvmImpl` (открытые, с `open fun discover`/`close`).
- В `desktopMain` добавлены обёртки: `actual class WSDiscovery` и `actual class UPnPDiscovery`, делегирующие в `WSDiscoveryJvmImpl()` и `UPnPDiscoveryJvmImpl()`.
- В **expect**-объявлениях добавлен явный конструктор: `expect class WSDiscovery constructor()` и `expect class UPnPDiscovery constructor()` — это устранило ошибку «does not have default constructor» при компиляции desktop.
- В `OnvifClient` для переменных discovery добавлены явные типы: `val wsDiscovery: WSDiscovery = WSDiscovery()` и `val upnpDiscovery: UPnPDiscovery = UPnPDiscovery()`.

### 6. OnvifClient: конфликт «CoroutineDispatcher.close()»

**Проблема:** Сообщение «Cannot access 'fun CoroutineDispatcher.close(): it is private in file» при вызове `wsDiscovery.close()` / `upnpDiscovery.close()`.

**Решение:** После исправления expect/actual для desktop (п. 5) типы `wsDiscovery` и `upnpDiscovery` корректно разрешаются в actual-классы, вызовы `close()` перестают конфликтовать с расширением для `CoroutineDispatcher`.

### 7. OnvifExceptions.kt: Regex с несколькими опциями

**Проблема:** Использование `Regex(..., RegexOption.DOT_MATCHES_ALL or RegexOption.IGNORE_CASE)` — в Kotlin для нескольких опций нужен `Set<RegexOption>`, а не битовая комбинация.

**Решение:** Заменено на `Regex(..., setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))` в `extractFaultDetail`.

### 8. RtspClient / NativeRtspClient: typealias и конструктор

**Проблема:** В `commonMain` использовался тип `NativeRtspClientHandle`, объявленный только в платформенных actual; для desktop expect-класс `NativeRtspClient` давал «does not have default constructor».

**Решение:**
- В `commonMain/rtsp/NativeRtspClient.kt` добавлен общий тип: `typealias NativeRtspClientHandle = Long` (без expect/actual).
- Удалены все платформенные `actual typealias NativeRtspClientHandle = Long` из `jvmMain`, `nativeMain`, `iosMain`, `androidMain`.
- В expect добавлен явный конструктор: `expect class NativeRtspClient constructor()`.

### 9. CertificatePinner (desktop): «Declaration must be marked with 'actual'»

**Проблема:** В `desktopMain/.../CertificatePinner.kt` компилятор требовал пометку actual для объявления.

**Решение:** Для primary constructor добавлена пометка: `actual class CertificatePinner actual constructor(private val config: CertificatePinningConfig)`.

---

## Что по-прежнему остаётся (сборка desktop)

- **WebSocketClient:** сериализатор для `Map<String, Any>`, ссылка на `KotlinxWebsocketSerializationConverter`, синтаксическая ошибка около строки 421 (label/for), `JsonDecodingException`/`message`, вызовы `encodeToString` с `Map<String, Any>`.
- **EventApiService, CameraApiService, SettingsApiService, StreamApiService, UserApiService:** вызовы `apiClient.get/post/put/delete` с устаревшим параметром `responseType` и без указания двух type-аргументов для `post`/`put`; разбор результата через `.success`/`.data`/`.error` вместо `ApiResult.fold`/`getOrThrow()`.
