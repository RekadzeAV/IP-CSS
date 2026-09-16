# Анализ и поэтапный план реализации: Этап 4. СЕТЕВОЙ СЛОЙ

**Дата анализа:** 2026-01-26
**Текущий статус:** ~85% (по документации ~100%, но есть критические пробелы)
**Целевой статус:** 100% с полной функциональностью

---

## 📊 Текущее состояние компонентов

### ✅ Полностью реализованные компоненты (100%)

#### 1. REST API Client (100%)
- ✅ HTTP клиент на Ktor
- ✅ Метрики и мониторинг (NetworkMetricsCollector)
- ✅ Rate limiting (TokenBucketRateLimiter, FixedIntervalRateLimiter)
- ✅ Request Interceptors
- ✅ Аутентификация
- ✅ Retry механизм
- ✅ Обработка ошибок
- ✅ Поддержка для загрузки файлов

**Файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/ApiClient.kt`
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/metrics/NetworkMetrics.kt`
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/ratelimit/RateLimiter.kt`
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/interceptor/RequestInterceptor.kt`

#### 2. API Services (100%)
- ✅ CameraApiService
- ✅ RecordingApiService
- ✅ EventApiService
- ✅ UserApiService
- ✅ SettingsApiService
- ✅ LicenseApiService
- ✅ StreamApiService

**Файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/api/*.kt`

#### 3. DTO Classes (100%)
- ✅ ApiResponse<T>
- ✅ CameraDto
- ✅ RecordingDto
- ✅ EventDto
- ✅ UserDto
- ✅ SettingsDto
- ✅ LicenseDto
- ✅ StreamDto
- ✅ NotificationDto

**Файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/dto/*.kt`

#### 4. RTSP Client (100%)
- ✅ Kotlin обертка с нативными библиотеками
- ✅ Интеграция с C++ библиотекой RTSP клиента
- ✅ Связка Kotlin ↔ C++ (JNI интеграция для Android)
- ✅ Все основные RTSP команды (OPTIONS, DESCRIBE, SETUP, PLAY, PAUSE, TEARDOWN)
- ✅ RTP/RTCP обработка и парсинг
- ✅ Поддержка кодеков/форматов (H.264, H.265, MJPEG, AAC, PCMU, PCMA, MP3)
- ✅ Аутентификация (Basic, Digest с обработкой stale nonce)
- ✅ Автоматическое переподключение
- ✅ Поддержка множественных потоков (Main/Sub или разные URL)
- ✅ Интеграция с видеосервисами (HLS генерация через FFmpeg)

**Файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/RtspClient.kt`
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/rtsp/NativeRtspClient.kt`
- `native/video-processing/src/jni/rtsp_client_jni.cpp`

#### 5. WebSocket Client (100%)
- ✅ Подключение/отключение
- ✅ Автоматическое переподключение
- ✅ Подписка на каналы
- ✅ Обработка текстовых сообщений
- ✅ Поддержка бинарных сообщений (BinaryMessage)
- ✅ Поддержка сжатия (WebSocketDeflateExtension)
- ✅ Аутентификация сообщений
- ✅ Rate limiting

**Файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/WebSocketClient.kt`
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/BinaryMessageHandler.kt`
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/ChunkingManager.kt`
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/MessageQueue.kt`

#### 6. WS-Discovery (100%)
- ✅ Android реализация
- ✅ JVM реализация
- ✅ iOS реализация
- ✅ Native реализация
- ✅ Парсинг ProbeMatches
- ✅ Фильтрация устройств

**Файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/WSDiscovery.kt`
- `core/network/src/androidMain/kotlin/com/company/ipcamera/core/network/WSDiscovery.android.kt`
- `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/WSDiscovery.jvm.kt`
- `core/network/src/iosMain/kotlin/com/company/ipcamera/core/network/WSDiscovery.ios.kt`
- `core/network/src/nativeMain/kotlin/com/company/ipcamera/core/network/WSDiscovery.native.kt`

#### 7. Certificate Pinning (100%)
- ✅ Android: Интеграция с OkHttp
- ✅ iOS: Интеграция с URLSessionDelegate
- ✅ JVM: Нативная реализация
- ✅ SHA-256 fingerprint calculation
- ✅ Поддержка backup pins
- ✅ Enforce pinning режим

**Файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/security/CertificatePinner.kt`
- `core/network/src/androidMain/kotlin/com/company/ipcamera/core/network/security/CertificatePinner.android.kt`
- `core/network/src/iosMain/kotlin/com/company/ipcamera/core/network/security/CertificatePinner.ios.kt`
- `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/security/CertificatePinner.jvm.kt`

---

### ⚠️ Частично реализованные компоненты

#### 1. ONVIF Client (~85%)

**Реализовано:**
- ✅ Базовые методы (getCapabilities, getDeviceInformation, getProfiles, getStreamUri)
- ✅ PTZ управление (movePtz, stopPtz, zoomIn, zoomOut)
- ✅ testConnection()
- ✅ WS-Discovery интеграция (автоматическое обнаружение камер, но есть проблемы с ~60% обнаруженных камер)
- ✅ Улучшенный XML парсинг (поддержка разных namespace, fallback на regex)
- ✅ Digest Authentication (реализовано)
- ✅ Обнаружение audio в профилях

**Не реализовано (на момент первичного аудита, историческая оценка):**
- ⚠️ **ONVIF Event service** - было оценено как критичный пробел
- ⚠️ **ONVIF Analytics service** - было отмечено как пробел
- ⚠️ **ONVIF Imaging service** - было отмечено как пробел
- ❌ Улучшение WS-Discovery (проблемы с обнаружением)
- ❌ Полная поддержка всех ONVIF профилей

Актуальный статус `1.4.*` ведётся в каноническом документе: `docs/status/PROJECT_STATUS_PHASES.md`.

**Файлы:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/OnvifClient.kt`
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/OnvifClientFactory.kt`
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/OnvifTypes.kt`
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/auth/DigestAuthHelper.kt`

---

### ❌ Не реализованные компоненты

#### 1. Тестирование (40%)
- ⚠️ Unit тесты для основных компонентов (частично)
- ❌ Unit тесты для OnvifClient (полные)
- ❌ Unit тесты для WebSocketClient (полные)
- ❌ Integration тесты (отсутствуют)
- ❌ Тесты для метрик и rate limiting

**Файлы:**
- `core/network/src/commonTest/kotlin/com/company/ipcamera/core/network/*.kt`

---

## 🎯 Критические пробелы и приоритеты

### 🔴 Критический приоритет (P0)

#### 1. ONVIF Event Service (0% → 100%)
**Почему критично:**
- Без Event service невозможно получать события от камер в реальном времени
- Это ключевая функциональность для системы видеонаблюдения
- Требуется для автоматического обнаружения событий (движение, вторжение и т.д.)

**Что нужно реализовать:**
- Subscribe к событиям камеры
- PullPoint subscription для получения событий
- Парсинг ONVIF событий (motion detection, tampering, etc.)
- Интеграция с системой событий приложения

**Оценка:** 2-3 недели

#### 2. Улучшение WS-Discovery (60% → 95%)
**Проблема:**
- Только ~60% обнаруженных камер успешно обрабатываются
- Проблемы с парсингом некоторых типов устройств
- Недостаточная фильтрация не-ONVIF устройств

**Что нужно улучшить:**
- Улучшить парсинг ProbeMatches
- Добавить больше fallback механизмов
- Улучшить фильтрацию устройств
- Добавить retry логику для проблемных устройств

**Оценка:** 1 неделя

---

### 🟡 Высокий приоритет (P1)

#### 3. ONVIF Analytics Service (0% → 100%)
**Зачем нужно:**
- Получение аналитических данных от камер
- Интеграция с AI-модулями
- Статистика и отчеты

**Что нужно реализовать:**
- GetAnalyticsEngineInputs
- GetAnalyticsEngines
- GetAnalyticsEngine
- CreateAnalyticsEngine
- DeleteAnalyticsEngine
- SetAnalyticsEngine
- GetAnalyticsEngineInput
- SetAnalyticsEngineInput

**Оценка:** 1-2 недели

#### 4. ONVIF Imaging Service (0% → 100%)
**Зачем нужно:**
- Управление настройками изображения камеры
- Яркость, контраст, насыщенность
- Автофокус, баланс белого
- Настройки экспозиции

**Что нужно реализовать:**
- GetImagingSettings
- SetImagingSettings
- GetOptions
- GetMoveOptions
- Move
- Stop
- GetStatus
- GetPresets
- SetPreset
- RemovePreset

**Оценка:** 1-2 недели

#### 5. Полное тестирование (40% → 90%)
**Что нужно:**
- Unit тесты для всех основных компонентов
- Integration тесты для ONVIF
- Тесты для WebSocket
- Тесты для RTSP
- Тесты для метрик и rate limiting

**Оценка:** 2-3 недели

---

### 🟢 Средний приоритет (P2)

#### 6. Дополнительные ONVIF сервисы
- ONVIF Device Management Service (расширение)
- ONVIF Media Service (расширение)
- ONVIF PTZ Service (расширение - больше функций)

**Оценка:** 1-2 недели

#### 7. Оптимизация и производительность
- Кэширование ONVIF capabilities
- Оптимизация WebSocket сообщений
- Улучшение RTSP буферизации

**Оценка:** 1 неделя

---

## 📋 Поэтапный план реализации

### Этап 1: Критические компоненты (3-4 недели)

#### Неделя 1-2: ONVIF Event Service
**Цель:** Реализовать полную поддержку ONVIF Event service

**Задачи:**
1. **Изучение ONVIF Event Service спецификации**
   - Изучить ONVIF Core Specification, раздел Event Service
   - Проанализировать примеры реализации
   - Определить необходимые SOAP методы

2. **Создание базовой структуры**
   - Создать `OnvifEventService.kt`
   - Определить data classes для событий
   - Создать SOAP request builders для Event service

3. **Реализация PullPoint Subscription**
   - `CreatePullPointSubscription()` - создание подписки
   - `PullMessages()` - получение событий
   - `Renew()` - продление подписки
   - `Unsubscribe()` - отмена подписки
   - `SetSynchronizationPoint()` - установка точки синхронизации

4. **Реализация Basic Notification Interface**
   - `Subscribe()` - подписка на события
   - `Unsubscribe()` - отмена подписки
   - `Renew()` - продление подписки
   - `GetCurrentMessage()` - получение текущего сообщения

5. **Парсинг событий**
   - Парсинг TopicExpression
   - Парсинг MessageContent
   - Определение типов событий (motion, tampering, etc.)
   - Маппинг ONVIF событий в внутренние Event модели

6. **Интеграция с OnvifClient**
   - Добавить методы в OnvifClient
   - Интеграция с системой событий приложения
   - Обработка ошибок и переподключений

7. **Тестирование**
   - Unit тесты для Event service
   - Integration тесты с реальными камерами
   - Тесты обработки различных типов событий

**Файлы для создания:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/onvif/OnvifEventService.kt`
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/onvif/OnvifEventTypes.kt`
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/onvif/OnvifEventParser.kt`
- `core/network/src/commonTest/kotlin/com/company/ipcamera/core/network/onvif/OnvifEventServiceTest.kt`

**Критерии готовности:**
- ✅ Все методы Event service реализованы
- ✅ Парсинг основных типов событий работает
- ✅ Интеграция с OnvifClient завершена
- ✅ Unit тесты покрывают 80%+ кода
- ✅ Integration тесты проходят с реальными камерами

---

#### Неделя 3: Улучшение WS-Discovery
**Цель:** Улучшить обнаружение камер до 95% успешности

**Задачи:**
1. **Анализ проблем**
   - Изучить логи неудачных обнаружений
   - Определить типы устройств, которые не обрабатываются
   - Проанализировать структуру ProbeMatches для проблемных устройств

2. **Улучшение парсинга**
   - Расширить поддержку различных форматов ProbeMatches
   - Добавить больше fallback механизмов парсинга
   - Улучшить обработку пустых или некорректных полей

3. **Улучшение фильтрации**
   - Более точная фильтрация ONVIF устройств
   - Улучшенная обработка устройств без явных ONVIF маркеров
   - Добавление эвристик для определения ONVIF устройств

4. **Retry логика**
   - Добавить retry для проблемных устройств
   - Улучшить обработку таймаутов
   - Добавить параллельную обработку устройств

5. **Тестирование**
   - Тесты с различными типами устройств
   - Тесты обработки edge cases
   - Integration тесты с реальной сетью

**Файлы для изменения:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/WSDiscovery.kt`
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/OnvifClient.kt` (метод discoverCameras)

**Критерии готовности:**
- ✅ Успешность обнаружения ≥ 95%
- ✅ Улучшена обработка edge cases
- ✅ Добавлены тесты для проблемных сценариев

---

#### Неделя 4: ONVIF Analytics Service (начало)
**Цель:** Начать реализацию ONVIF Analytics Service

**Задачи:**
1. **Изучение спецификации**
   - Изучить ONVIF Analytics Specification
   - Определить необходимые методы

2. **Создание базовой структуры**
   - Создать `OnvifAnalyticsService.kt`
   - Определить data classes для аналитики
   - Создать SOAP request builders

3. **Реализация базовых методов**
   - `GetAnalyticsEngineInputs()`
   - `GetAnalyticsEngines()`
   - `GetAnalyticsEngine()`

**Файлы для создания:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/onvif/OnvifAnalyticsService.kt`
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/onvif/OnvifAnalyticsTypes.kt`

---

### Этап 2: Высокоприоритетные компоненты (2-3 недели)

#### Неделя 5-6: Завершение ONVIF Analytics Service
**Цель:** Завершить реализацию ONVIF Analytics Service

**Задачи:**
1. **Реализация управления аналитикой**
   - `CreateAnalyticsEngine()`
   - `DeleteAnalyticsEngine()`
   - `SetAnalyticsEngine()`
   - `GetAnalyticsEngineInput()`
   - `SetAnalyticsEngineInput()`

2. **Парсинг аналитических данных**
   - Парсинг конфигураций аналитических движков
   - Парсинг входных данных
   - Маппинг в внутренние модели

3. **Интеграция**
   - Интеграция с OnvifClient
   - Интеграция с AI-модулями (если есть)

4. **Тестирование**
   - Unit тесты
   - Integration тесты

**Критерии готовности:**
- ✅ Все методы Analytics service реализованы
- ✅ Парсинг работает корректно
- ✅ Тесты покрывают 80%+ кода

---

#### Неделя 7: ONVIF Imaging Service
**Цель:** Реализовать ONVIF Imaging Service

**Задачи:**
1. **Изучение спецификации**
   - Изучить ONVIF Imaging Specification

2. **Создание структуры**
   - Создать `OnvifImagingService.kt`
   - Определить data classes для настроек изображения

3. **Реализация методов**
   - `GetImagingSettings()`
   - `SetImagingSettings()`
   - `GetOptions()`
   - `GetMoveOptions()`
   - `Move()` (для PTZ камер)
   - `Stop()`
   - `GetStatus()`
   - `GetPresets()`
   - `SetPreset()`
   - `RemovePreset()`

4. **Интеграция и тестирование**
   - Интеграция с OnvifClient
   - Unit тесты
   - Integration тесты

**Файлы для создания:**
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/onvif/OnvifImagingService.kt`
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/onvif/OnvifImagingTypes.kt`

**Критерии готовности:**
- ✅ Все методы Imaging service реализованы
- ✅ Тесты покрывают 80%+ кода
- ✅ Интеграция завершена

---

### Этап 3: Тестирование и оптимизация (2-3 недели)

#### Неделя 8-9: Полное тестирование
**Цель:** Довести покрытие тестами до 90%

**Задачи:**
1. **Unit тесты для OnvifClient**
   - Тесты всех методов
   - Тесты парсинга XML
   - Тесты обработки ошибок
   - Тесты аутентификации

2. **Unit тесты для WebSocketClient**
   - Тесты подключения/отключения
   - Тесты отправки/получения сообщений
   - Тесты подписки на каналы
   - Тесты бинарных сообщений
   - Тесты переподключения

3. **Unit тесты для ApiClient**
   - Тесты HTTP запросов
   - Тесты retry логики
   - Тесты rate limiting
   - Тесты interceptors
   - Тесты метрик

4. **Unit тесты для RTSP Client**
   - Тесты основных команд
   - Тесты парсинга
   - Тесты обработки ошибок

5. **Integration тесты**
   - Тесты с реальными ONVIF камерами
   - Тесты WebSocket соединений
   - Тесты RTSP потоков
   - Тесты WS-Discovery в реальной сети

**Файлы для создания/обновления:**
- `core/network/src/commonTest/kotlin/com/company/ipcamera/core/network/OnvifClientTest.kt` (расширение)
- `core/network/src/commonTest/kotlin/com/company/ipcamera/core/network/WebSocketClientTest.kt` (расширение)
- `core/network/src/commonTest/kotlin/com/company/ipcamera/core/network/ApiClientTest.kt` (расширение)
- `core/network/src/commonTest/kotlin/com/company/ipcamera/core/network/RtspClientTest.kt` (расширение)
- `core/network/src/commonTest/kotlin/com/company/ipcamera/core/network/integration/*.kt` (новые)

**Критерии готовности:**
- ✅ Покрытие тестами ≥ 90%
- ✅ Все integration тесты проходят
- ✅ Тесты стабильны и воспроизводимы

---

#### Неделя 10: Оптимизация и улучшения
**Цель:** Оптимизировать производительность и улучшить надежность

**Задачи:**
1. **Кэширование ONVIF capabilities**
   - Реализовать кэш для capabilities
   - Реализовать кэш для device information
   - Добавить TTL для кэша
   - Инвалидация кэша при необходимости

2. **Оптимизация WebSocket**
   - Оптимизация размера сообщений
   - Улучшение сжатия
   - Оптимизация очереди сообщений

3. **Улучшение RTSP**
   - Оптимизация буферизации
   - Улучшение обработки пакетов
   - Оптимизация памяти

4. **Мониторинг и метрики**
   - Расширение метрик
   - Добавление метрик для ONVIF
   - Добавление метрик для WebSocket
   - Добавление метрик для RTSP

5. **Документация**
   - Обновить документацию API
   - Добавить примеры использования
   - Обновить README

**Критерии готовности:**
- ✅ Кэширование реализовано и работает
- ✅ Производительность улучшена
- ✅ Метрики расширены
- ✅ Документация обновлена

---

## 📈 Метрики успеха

### Технические метрики
- ✅ Покрытие тестами: ≥ 90%
- ✅ Успешность WS-Discovery: ≥ 95%
- ✅ Время отклика ONVIF запросов: < 2 секунд
- ✅ Стабильность WebSocket соединений: ≥ 99%
- ✅ Успешность RTSP подключений: ≥ 95%

### Функциональные метрики
- ✅ Все ONVIF сервисы реализованы (Event, Analytics, Imaging)
- ✅ Все API сервисы работают корректно
- ✅ WebSocket поддерживает все типы сообщений
- ✅ RTSP поддерживает все необходимые кодеки

---

## 🔄 Интеграция с другими этапами

### Связь с этапом 5 (Backend сервер)
- ONVIF Event service → EventService в backend
- WebSocket Client → WebSocket сервер в backend
- API Services → REST API endpoints в backend

### Связь с этапом 6 (Frontend)
- WebSocket Client → WebSocket клиент во frontend
- API Services → API вызовы из frontend
- DTO Classes → Типы данных для frontend

### Связь с этапом 3 (База данных)
- Event данные → Сохранение в БД
- Camera данные → Синхронизация с БД
- Recording данные → Интеграция с БД

---

## 📝 Чеклист готовности этапа 4

### REST API Client
- [x] HTTP клиент на Ktor
- [x] Метрики и мониторинг
- [x] Rate limiting
- [x] Request Interceptors
- [x] Аутентификация
- [x] Retry механизм
- [x] Обработка ошибок
- [x] Поддержка загрузки файлов

### WebSocket Client
- [x] Подключение/отключение
- [x] Автоматическое переподключение
- [x] Подписка на каналы
- [x] Обработка текстовых сообщений
- [x] Поддержка бинарных сообщений
- [x] Поддержка сжатия
- [x] Аутентификация
- [x] Rate limiting

### RTSP Client
- [x] Все основные RTSP команды
- [x] RTP/RTCP обработка
- [x] Поддержка кодеков
- [x] Аутентификация
- [x] Автоматическое переподключение
- [x] Поддержка множественных потоков
- [x] Интеграция с видеосервисами

### ONVIF Client
- [x] Базовые методы
- [x] PTZ управление
- [x] WS-Discovery
- [x] Digest Authentication
- [x] XML парсинг
- [ ] **ONVIF Event Service** ← КРИТИЧНО
- [ ] **ONVIF Analytics Service** ← ВЫСОКИЙ ПРИОРИТЕТ
- [ ] **ONVIF Imaging Service** ← ВЫСОКИЙ ПРИОРИТЕТ
- [ ] Улучшение WS-Discovery (95% успешности)

### WS-Discovery
- [x] Android реализация
- [x] JVM реализация
- [x] iOS реализация
- [x] Native реализация
- [ ] Улучшение парсинга (95% успешности)

### Certificate Pinning
- [x] Android реализация
- [x] iOS реализация
- [x] JVM реализация
- [x] SHA-256 fingerprints
- [x] Backup pins

### Тестирование
- [ ] Unit тесты для OnvifClient (полные)
- [ ] Unit тесты для WebSocketClient (полные)
- [ ] Unit тесты для ApiClient (полные)
- [ ] Unit тесты для RTSP Client
- [ ] Integration тесты
- [ ] Покрытие ≥ 90%

---

## 🎯 Итоговый план по неделям

| Неделя | Фокус | Приоритет | Статус |
|--------|-------|-----------|--------|
| 1-2 | ONVIF Event Service | P0 | ⏳ Планируется |
| 3 | Улучшение WS-Discovery | P0 | ⏳ Планируется |
| 4 | ONVIF Analytics Service (начало) | P1 | ⏳ Планируется |
| 5-6 | ONVIF Analytics Service (завершение) | P1 | ⏳ Планируется |
| 7 | ONVIF Imaging Service | P1 | ⏳ Планируется |
| 8-9 | Полное тестирование | P1 | ⏳ Планируется |
| 10 | Оптимизация и улучшения | P2 | ⏳ Планируется |

**Общая оценка:** 10 недель (2.5 месяца)

---

## 📚 Дополнительные ресурсы

### Документация ONVIF
- [ONVIF Core Specification](https://www.onvif.org/specs/srv/core/ONVIF-Core-Specification.pdf)
- [ONVIF Event Service Specification](https://www.onvif.org/specs/srv/event/ONVIF-Event-Service-Specification.pdf)
- [ONVIF Analytics Service Specification](https://www.onvif.org/specs/srv/analytics/ONVIF-Analytics-Service-Specification.pdf)
- [ONVIF Imaging Service Specification](https://www.onvif.org/specs/srv/img/ONVIF-Imaging-Service-Specification.pdf)

### Полезные ссылки
- [Ktor Documentation](https://ktor.io/docs/)
- [WebSocket RFC 6455](https://tools.ietf.org/html/rfc6455)
- [RTSP RFC 2326](https://tools.ietf.org/html/rfc2326)
- [WS-Discovery Specification](https://docs.oasis-open.org/ws-dd/discovery/1.1/os/wsdd-discovery-1.1-spec-os.html)

---

**Дата создания:** 2026-01-26
**Последнее обновление:** 2026-01-26
**Версия:** 1.0
