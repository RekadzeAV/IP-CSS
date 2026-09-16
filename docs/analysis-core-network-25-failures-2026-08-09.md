# Анализ 25 падений `:core:network:desktopTest` (09 August 2026)

**Прогон (было):** offline, `./gradlew :core:network:desktopTest --offline`
**Итог до:** 544 tests completed, 25 failed, 38 skipped
**Итог после исправлений:** ✅ **544 tests, 0 failures, 0 errors, 38 skipped** (доп. проверка полным прогоном подтверждена).

## Классификация падений (было 25)

| # | Группа | Кол-во | Симптомы | Примеры тестов | Вероятная причина |
|---|--------|-------:|----------|----------------|-------------------|
| A | XML-парсеры ONVIF (`jvmMain`) | 10 | `actual value is null`, `expected:<1> but was:<0>` | `parseCapabilities*` (3), `parseDeviceInformation*` (2), `parseProfiles*` (2), `parseStreamUri`, `POST request`, `empty response` | Парсинг SOAP XML: XAddr извлекался как атрибут, а в XML это дочерний элемент; профили искались как `trt:Profile` вместо `trt:Profiles` |
| B | SOAP-запросы через `MockEngineFactory.createOnvif` | 6 | `SOAP request failed with status: 404 Not Found` | subscription/pull messages | MockEngine: `SOAPAction: ""` → `?: extractSoapAction()` не срабатывал (пустая строка, не null) → 404 |
| C | Сериализация `WebSocketMessage` | 1 | конфликт discriminator `type` | `WebSocketMessage serialization performance` | Поле `EventMessage.type` конфликтует с дефолтным дискриминатором sealed-класса |
| D | Coroutines (незакрытые child jobs) | 2 | `UncompletedCoroutinesError` | `testDiscoverCameras`, `testUrlNormalization` | Реальные сетевые discovery-вызовы не завершались в `runTest` |
| E | RTSP/backoff | 2 | `Backoff delays`, `expected:<DISCONNECTED> but was:<CONNECTED>` | backoff timing, disconnect-during-fallback | `runTest` ускоряет `delay` (виртуальное время); race: `connect()` перезаписывал статус после `disconnect()` и повторно добавлял streams |
| F | Прочие assertion | 4 | `Expected value to be true` | `pullMessages`, `HTML report`, `parseNotificationMessage empty props` | `SimpleItem Name=.. Value=..` не парсился как свойство; locale-зависимый формат `%.2f` |

## Что исправлено (файлы)

**Продакшн-код (`commonMain`/`jvmMain`):**
- `OnvifClient.kt`: `parseCapabilitiesFallback` извлекает `XAddr` как дочерний элемент (helper `extractXmlChildValue`); корректный `null` для пустого XML; `parseProfiles` ищет `trt:Profiles?` и парсит видео/аудио из блока профиля.
- `MockEngineFactory.kt`: `SOAPAction` пустая строка → fallback на извлечение из тела.
- `ApiClient.kt`: 204/пустое тело для `String` → успех с `""`.
- `OnvifEventParser.kt`: парсинг `SimpleItem Name= Value= `; исключение служебных WS-Notification тегов из properties.
- `RtspClient.kt`: `connect()` не перезаписывает статус/streams, если клиент был отключён во время fallback.
- `RtspBenchmarkConfig.kt` (`jvmMain`): `toFixed` — locale-независимое форматирование (`Locale.ROOT`).

**Тесты:**
- `OnvifXmlParserTest.kt`: добавлен стандартный `GetCapabilities` ответ для `getDeviceInformation`/`getStreamUri`/`getProfiles`.
- `ApiClientIntegrationTest.kt`: корректное чтение `ByteArrayContent`/`TextContent`.
- `RtspClientReconnectIntegrationTest.kt`: backoff-тест переведён на `runBlocking` (реальное время), проверка нескольких попыток.
- `NetworkPerformanceTest.kt`: локальный `Json { classDiscriminator = "msg_type" }`.
- `RtspBenchmarkConfigTest.kt`: ожидание `29.50`/`29.5`.
- `OnvifClientIntegrationTest.kt`: `testDiscoverCameras`/`testUrlNormalization` — `withTimeout` + tolerate без камер.

## Связанные файлы (после фикса)
- Тесты: `core/network/src/commonTest/.../onvif/OnvifXmlParserTest.kt`, `OnvifEventParserTest.kt`, `integration/ApiClientIntegrationTest.kt`, `RtspClientTest.kt`, `RtspClientReconnectIntegrationTest.kt`, `performance/NetworkPerformanceTest.kt`, `rtsp/RtspBenchmarkConfigTest.kt`
- Мок-движок: `core/network/src/commonTest/.../test/MockEngineFactory.kt`
- Код: `OnvifClient.kt`, `OnvifEventParser.kt`, `ApiClient.kt`, `RtspClient.kt`, `jvmMain/.../rtsp/RtspBenchmarkConfig.kt`

**Итог:** `:core:network:desktopTest` → **544 tests, 0 failed, 38 skipped**. Блокер тестовой готовности закрыт.