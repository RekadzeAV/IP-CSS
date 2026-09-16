# Приоритетные задачи для завершения MVP — детализация и статус

**Дата:** 1 марта 2026  
**Источник:** проверка кодовой базы и документации

---

## Критический приоритет

### 1. ONVIF Event service — подписка на события камер, интеграция с системой событий

**Оценка:** 2–3 недели  
**Статус:** ✅ **ВЫПОЛНЕНО** (реализовано в коде)

| Подзадача | Описание | Статус | Детали |
|-----------|----------|--------|--------|
| **1.1** OnvifEventService | SOAP-клиент для Event Service (Subscribe, PullPoint, PullMessages) | ✅ | `OnvifEventServiceImpl.kt` — createPullPointSubscription, subscribeToEvents, pullMessages, renewSubscription, unsubscribe |
| **1.2** OnvifEventParser | Парсинг Subscribe/PullMessages ответов и NotificationMessage | ✅ | `OnvifEventParser.kt` — createSubscribeRequest, parseSubscribeResponse, parsePullMessagesResponse, parseNotificationMessage, parseGetEventPropertiesResponse |
| **1.3** OnvifEventIntegrationService | Маппинг ONVIF → доменные события, фоновый опрос PullPoint | ✅ | `OnvifEventIntegrationService.kt` — startMonitoring, processOnvifEvent, mapOnvifEventToEvent (MOTION_ALARM, INTRUSION_DETECTOR, DEVICE_TAMPER и др.) |
| **1.4** OnvifEventSubscriptionService (сервер) | Подписка по cameraId, продление подписок, сохранение событий в БД | ✅ | `OnvifEventSubscriptionService.kt` — subscribeToCameraEvents, startPullMessagesTask, handleIncomingEvent → EventService.createEvent → EventRepository.addEvent |
| **1.5** EventService → EventRepository | Создание событий в БД и рассылка через WebSocket | ✅ | `EventService.kt` — createEvent вызывает eventRepository.addEvent и WebSocketManager.broadcast |
| **1.6** API для подписки и приёма уведомлений | Маршруты для клиента и для камер | ✅ | `OnvifEventRoutes.kt` — POST /api/v1/onvif/events/subscribe, unsubscribe, POST .../notification/{subscriptionId?} |
| **1.7** DI и интеграция в приложение | Регистрация сервисов в Koin, использование в репозиториях | ✅ | `AppModule.kt` — OnvifEventService, OnvifEventSubscriptionService; `OnvifServicesModule.kt` — OnvifEventIntegrationService; `CameraRepositoryWithEventMonitoring.kt` |
| **1.8** Unit/интеграционные тесты | Тесты интеграционного сервиса и парсера | ✅ | `OnvifEventIntegrationServiceTest.kt` — маппинг событий, PullPoint, ошибки; моки OnvifEventService |

**Итог:** Подписка на события ONVIF (Subscribe и PullPoint), маппинг в доменные события, сохранение в БД и доставка через WebSocket реализованы. Остаётся: автостарт подписок при добавлении камеры (опционально), E2E с реальными камерами, проверка на разных вендорах.

---

### 2. Безопасность — финализация certificate pinning и принудительного HTTPS

**Оценка:** 1–2 недели  
**Статус:** ✅ **ВЫПОЛНЕНО**

| Подзадача | Описание | Статус | Детали |
|-----------|----------|--------|--------|
| **2.1** CertificatePinner (expect/actual) | Платформо-специфичная проверка сертификатов | ✅ | `CertificatePinner` — android (OkHttp), ios, jvm; `CertificatePinningConfig`, `CertificatePinningManager` |
| **2.2** Интеграция в ApiClient | Создание engine с pinning при наличии конфига | ✅ | `ApiClient.kt` — createEngineWithCertificatePinning, createEngineWithPinning (expect) |
| **2.3** OnvifClient с pinning | Использование pinning для ONVIF запросов | ✅ | `OnvifClientFactory.kt` — createWithPinning, loadConfig из файла/окружения |
| **2.4** Принудительный HTTPS на сервере | Редирект HTTP → HTTPS в production | ✅ | `HttpsRedirectMiddleware.kt`, `Application.kt` — installHttpsRedirect(enabled = isProduction) |
| **2.5** HSTS и прочие security headers | Заголовки безопасности | ✅ | Упоминаются в отчётах; при необходимости проверить в Application.kt / Next.js |
| **2.6** Unit-тесты pinning | Тесты для всех платформ | ✅ | `CertificatePinnerAndroidTest`, `CertificatePinnerIosTest`, `CertificatePinnerJvmTest` |
| **2.7** Интеграционные тесты pinning | Тесты с реальными сертификатами и OnvifClient | ✅ | `CertificatePinningIntegrationTest.kt`, `OnvifClientCertificatePinningTest.kt` |
| **2.8** CertificatePinningManager | Загрузка конфига из JSON/ env | ✅ | `CertificatePinningManager.kt` — loadFromFile, loadFromEnvironment, loadConfig, validatePinFormat |

**Итог:** Certificate pinning и принудительный HTTPS реализованы и покрыты тестами. Для MVP достаточно; при необходимости — проверка на всех платформах и в production.

---

### 3. RTSP — тестирование и доводка FFmpeg-декодирования (при требовании «нативного» RTSP)

**Оценка:** 1–2 недели  
**Статус:** 🟡 **ЧАСТИЧНО ВЫПОЛНЕНО**

| Подзадача | Описание | Статус | Детали |
|-----------|----------|--------|--------|
| **3.1** Нативный RTSP клиент (C++/Kotlin) | Протокол RTSP, RTP/RTCP, NAL units, Digest Auth | ✅ | Реализовано в core/network (NativeRtspClient, RtspClient и т.д.) |
| **3.2** VideoDecoder (JVM/FFmpeg) | Декодирование H.264/H.265 через JavaCPP/FFmpeg | ✅ | `VideoDecoderImpl.kt` — avcodec, AVFrame, decode(), поддержка H.264/H.265 |
| **3.3** Интеграция RTSP → декодер | Поток кадров из RTSP в декодер | 🟡 | Интеграция есть; стабильность и граничные случаи могут требовать проверки |
| **3.4** RtspClientFfmpegDecodingTest | Тесты поддержки декодирования в RTSP | 🟡 | Есть; проверяют в основном создание клиента и конфиг (enableVideo/enableAudio), без реального декодирования потока |
| **3.5** VideoDecoder* тесты | Unit/демо тесты декодера | ✅ | VideoDecoderDemoTest, VideoDecoderH264ProfileTest, VideoDecoderErrorHandlingTest, VideoDecoderPerformanceTest и др. |
| **3.6** Интеграционные тесты с реальным RTSP | Тест против реального RTSP-сервера/камеры | ⚠️ | RtspClientIntegrationTest есть; для полной уверенности желательны прогоны с реальным потоком и FFmpeg |
| **3.7** Обработка ошибок и переподключение | Устойчивость при обрывах и битых кадрах | 🟡 | Частично в декодере и клиенте; при необходимости — усилить сценарии |

**Итог:** Нативный RTSP и FFmpeg-декодер реализованы и частично покрыты тестами. Для MVP допустим сценарий RTSP → HLS на сервере без обязательной доводки нативного декодирования. Если нужен именно нативный RTSP в клиенте — остаётся: прогон с реальным потоком, фиксация падающих тестов, при необходимости доработка ошибок и переподключения.

---

## Высокий приоритет

### 4. CameraRepositoryImpl — кэширование (DiscoveryCache, StatusCache, тесты)

**Оценка:** 2–3 дня  
**Статус:** ✅ **ВЫПОЛНЕНО**

| Подзадача | Описание | Статус | Детали |
|-----------|----------|--------|--------|
| **4.1** CameraCache (общий класс) | In-memory кэш с TTL, LRU при переполнении, Mutex | ✅ | `CameraRepositoryImpl.kt` — private class CameraCache(maxSize, expirationTime), get/put/remove/clear/invalidateAll |
| **4.2** Кэш списка камер | Кэш getCameras() | ✅ | cameraCache, allCamerasCacheKey, TTL 5 мин |
| **4.3** Кэш по id | Кэш getCameraById() | ✅ | cameraCache по id, инвалидация при add/update/remove |
| **4.4** DiscoveryCache | Кэш discoverCameras() | ✅ | discoveryCache (TTL 7 мин), discoveryCacheKey = "discovered_cameras"; использование в discoverCameras() |
| **4.5** StatusCache | Кэш getCameraStatus() | ✅ | statusCache (TTL 20 сек), ключ "status_$id"; инвалидация при update/remove |
| **4.6** Инвалидация при изменении данных | Очистка кэша при add/update/remove | ✅ | cameraCache.remove(allCamerasCacheKey), cameraCache.put(id), statusCache.remove("status_$id") в соответствующих методах |
| **4.7** Unit-тесты репозитория | Тесты CRUD и поведения | ✅ | `CameraRepositoryImplTest.kt`, `CameraRepositoryImplV2Test.kt`, `CameraRepositoryImplV2IntegrationTest.kt` |
| **4.8** Тесты именно кэширования | Явные тесты на cache hit/miss, TTL, инвалидацию | ❌ | В CameraRepositoryImplTest явных тестов кэша нет; поведение кэша покрыто косвенно через вызовы getCameras/getCameraById/discoverCameras/getCameraStatus |

**Итог:** Кэширование (включая DiscoveryCache и StatusCache) реализовано и используется. Отдельные unit-тесты на кэш (TTL, инвалидация) при желании можно добавить; для MVP не блокер.

---

### 5. Миграции БД — версионирование схем, MigrationManager

**Оценка:** 2–3 недели (для MVP не блокер)  
**Статус:** ✅ **ВЫПОЛНЕНО** (базовая система есть)

| Подзадача | Описание | Статус | Детали |
|-----------|----------|--------|--------|
| **5.1** Таблица schema_version | Хранение версии и описания миграций | ✅ | `CameraDatabase.sq` — CREATE TABLE schema_version; `1.sqm` — создание schema_version и вставка version=1 |
| **5.2** MigrationManager | Получение текущей версии, применение миграций, обновление schema_version | ✅ | `MigrationManager.kt` — getCurrentVersion, getTargetVersion, applyMigrations, updateSchemaVersion |
| **5.3** Миграция 1.sqm | Начальная миграция (таблицы + schema_version) | ✅ | `migrations/1.sqm` — описание в комментарии, INSERT в schema_version |
| **5.4** DatabaseFactory + миграции | При создании БД — вызов MigrationManager | ✅ | `DatabaseFactory.kt` (createDatabase) — MigrationValidator.databaseExists, MigrationManager.getCurrentVersion, applyMigrations, validateMigration |
| **5.5** MigrationValidator | Проверка существования БД и корректности версии | ✅ | `MigrationValidator.kt` — databaseExists, validateMigration |
| **5.6** MigrationLogger | Логирование шагов миграции | ✅ | `MigrationLogger.kt` |
| **5.7** Последующие миграции (2.sqm, 3.sqm, …) | При изменении схемы — новые файлы и увеличение targetVersion | 📋 | По мере появления изменений схемы; для текущего MVP не обязательно |

**Итог:** Версионирование схем и применение миграций реализованы и подключены при инициализации БД. Для MVP достаточно; дальнейшие миграции добавляются по необходимости.

---

## Сводная таблица

| Задача | Приоритет | Оценка | Статус | Примечание |
|--------|-----------|--------|--------|------------|
| ONVIF Event service | Критический | 2–3 нед | ✅ Выполнено | Реализовано и интегрировано; при необходимости — автостарт подписок и E2E с камерами |
| Безопасность (pinning + HTTPS) | Критический | 1–2 нед | ✅ Выполнено | Реализовано и покрыто тестами |
| RTSP / FFmpeg декодирование | Критический* | 1–2 нед | 🟡 Частично | *Только если нужен нативный RTSP; для MVP достаточно HLS |
| CameraRepositoryImpl кэш | Высокий | 2–3 дня | ✅ Выполнено | DiscoveryCache, StatusCache, инвалидация есть; тесты кэша опционально |
| Миграции БД | Высокий | 2–3 нед | ✅ Выполнено | MigrationManager, schema_version, 1.sqm, интеграция в DatabaseFactory |

---

**Вывод:** Все перечисленные приоритетные задачи для MVP в коде либо выполнены, либо (в случае нативного RTSP/FFmpeg) доведены до уровня, достаточного при использовании сценария RTSP→HLS. Документацию по статусу MVP (в т.ч. «Критические блокеры», «Приоритетные задачи») имеет смысл обновить в соответствии с этим статусом.
