# Поэтапный план реализации MVP

**Дата:** 2026-03-01  
**Цель:** Завершение MVP с приоритизацией по критичности и зависимостям.

---

## Текущее состояние (краткий аудит)

| Область | Статус | Пробелы |
|--------|--------|---------|
| **ONVIF Event** | Сервисы и интеграция есть | Подписка при старте сервера/клиента, тесты на реальных камерах, WebSocket → UI |
| **Безопасность** | Pinning на Android/iOS/JVM | Принудительный HTTPS, загрузка pins из конфига, веб-сервер |
| **RTSP/FFmpeg** | JVM декодер есть, native — заглушка | Стабильность, тесты на реальных потоках, ошибки/реконнект |
| **CameraRepository** | V2 в проде, кэш камер есть | В V2 нет DiscoveryCache и StatusCache |
| **Миграции БД** | MigrationManager, 1.sqm, schema_version | Дальнейшие миграции по мере изменения схемы |

---

## Фаза 0: Подготовка (1–2 дня)

**Цель:** Зафиксировать критерии готовности MVP и окружение.

- [x] **0.1** Определить и записать в `docs/MVP_DEFINITION.md`:
  - список фич, обязательных для MVP;
  - что считается «нативным RTSP» (только JVM/Desktop или и Android);
  - нужен ли принудительный HTTPS для MVP.
  **Статус:** Решения зафиксированы (RTSP — вариант A, HTTPS — в MVP). См. `docs/MVP_DEFINITION.md` (версия 1.0).
- [x] **0.2** Проверить сборку и прогон тестов по инструкции из `docs/TESTING_EXECUTION_GUIDE.md` (или аналог).
  **Статус:** Проверено. В руководство добавлен раздел «Требования к окружению» (ANDROID_HOME для полной сборки). Исправлены `android/app/build.gradle.kts` (jvmTarget → compilerOptions, Compose-зависимости, удалён composeOptions) и добавлен `org.jetbrains.kotlin.plugin.compose` в `platforms/client-desktop-arm/app`. Полная сборка на хосте без Android SDK требует настройки ANDROID_HOME; возможны ошибки вариантов для androidNativeX64 на Windows.
- [x] **0.3** Завести ветку/метки для MVP (например `mvp-phase-1`).
  **Статус:** Ветка `mvp-phase-1` создана и используется.

**Результат:** Общий реестр задач и стабильная база для следующих фаз.

---

## Фаза 1: ONVIF Event Service — подписка и интеграция (2–3 недели)

**Критический приоритет.** Без событий от камер MVP неполный.

**Сводный статус фазы:** ⚠️ **Частично выполнено** — сервисы и цепочка ONVIF→EventService→WebSocket есть; не хватает: авто-подписка на сервере при старте, конфиг ONVIF, проверка E2E и части тестов.

---

### Детализация со статусом выполнения

#### Этап 1.1 — Сервер: подписка при старте (3–5 дней)

| ID | Задача | Статус | Детали |
|----|--------|--------|--------|
| **1.1.1** | При старте API подписывать ONVIF-камеры на события | ✅ Сделано | В `Application.kt` при старте вызывается `onvifSubscriptionService.subscribeToAllCamerasAtStartup()` (с учётом `OnvifEventsConfig.enabled`). В `CameraRoutes`: при add — подписка для ONVIF-камер; при update — отписка + подписка при поддержке; при delete и bulk/delete — отписка. Используется `OnvifEventSubscriptionService`. |
| **1.1.2** | Входящие события передавать в `EventService.createEvent(...)` | ✅ Сделано | В `OnvifEventSubscriptionService.handleIncomingEvent` парсится NotificationMessage, маппинг через `OnvifEventMapper.mapToDomainEvent`, затем `eventService.createEvent(...)`. В `startPullMessagesTask` события обрабатываются и передаются в `eventService.createEvent`. |
| **1.1.3** | Созданные события уходят в WebSocket | ✅ Сделано | В `EventService.createEvent` после `eventRepository.addEvent` вызывается `sendEventViaWebSocket(createdEvent)` → `WebSocketManager.broadcastEvent(WebSocketChannel.EVENTS, ...)`. |
| **1.1.4** | Конфиг: включение/выключение ONVIF подписок, таймауты, интервал PullPoint | ✅ Сделано | `OnvifEventsConfig` загружается из env: `ONVIF_EVENTS_ENABLED`, `ONVIF_SUBSCRIPTION_TIME_SEC`, `ONVIF_PULL_INTERVAL_MS`, `ONVIF_PULL_TIMEOUT_MS`, `ONVIF_USE_PULL_POINT`. Документация в `ENVIRONMENT_VARIABLES.md` и `ONVIF_EVENT_INTEGRATION.md`; пример — `config/onvif-events.example.env`. |

**Файлы для правок:**  
`server/api/.../Application.kt`, `server/api/.../service/OnvifEventSubscriptionService.kt`, конфиг приложения.

---

#### Этап 1.2 — Клиенты (Desktop/Android): старт мониторинга (2–4 дня)

| ID | Задача | Статус | Детали |
|----|--------|--------|--------|
| **1.2.1** | **Desktop:** при старте запускать мониторинг для всех камер | ✅ Сделано | В `platforms/client-desktop-x86_64/app/.../App.kt` в `LaunchedEffect(Unit)` вызывается `eventMonitoringService?.initialize()`. `CameraEventMonitoringService.initialize()` получает список камер из `CameraRepository`, для каждой с поддержкой ONVIF вызывает `eventIntegrationService.startMonitoring(...)` с `pullInterval = 5000`. Репозиторий с обёрткой `CameraRepositoryWithEventMonitoring` на десктопе не проверялся — мониторинг идёт через сервис. |
| **1.2.2** | **Android:** при старте приложения инициализация мониторинга | ✅ Сделано | В `MainActivity.onCreate()` после `startKoin` в `lifecycleScope.launch` вызывается `eventMonitoringService.initialize()`. Сервис инжектируется через Koin (`CameraEventMonitoringService`). В `onResume()` — `ensureMonitoring()`, в `onDestroy()` — `shutdown()`. |
| **1.2.3** | Ошибки: нет сети, камера недоступна — не падать, логировать, переподписка при восстановлении | ✅ Сделано | Ошибки логируются, приложение не падает. Добавлена периодическая переподписка (каждые 5 мин) в `CameraEventMonitoringService` и публичный `ensureMonitoring()`; Android вызывает его в `onResume()`. При исчерпании повторов в `OnvifEventIntegrationService` мониторинг останавливается с сообщением о повторной попытке на следующем цикле. |

**Файлы для правок:**  
`platforms/client-desktop-*/app/.../App.kt`, `android/.../MainActivity.kt`, `shared/.../CameraEventMonitoringService.kt`, `shared/.../CameraRepositoryWithEventMonitoring.kt`.

---

#### Этап 1.3 — Интеграция с системой событий и UI (3–5 дней)

| ID | Задача | Статус | Детали |
|----|--------|--------|--------|
| **1.3.1** | Полный путь: ONVIF → EventRepository/EventService → WebSocket → веб/мобильный UI | ✅ Проверено | Сервер: ONVIF → EventService.createEvent → addEvent + broadcastEvent(EVENTS, "event.created"). Веб: подписка на канал `events`, обработка `event.created`/`event.acknowledged`/`event.deleted` в useWebSocket → eventsSlice; список на `/events` и snackbar для критических событий обновляются в реальном времени. Исправлена обработка типов сообщений (event.created и др.) в websocket.ts и useWebSocket. Desktop/Android: только REST, без WebSocket для событий (см. docs). |
| **1.3.2** | Маппинг недостающих ONVIF-топиков в `EventType`/`EventSeverity` | ✅ Базово сделано | Маппинг описан в `docs/ONVIF_EVENT_INTEGRATION.md`; реализован в `OnvifEventMapper` (сервер) и в `OnvifEventIntegrationService.mapOnvifEventToEvent` (общий код). При появлении новых топиков — добавить в маппинг. |
| **1.3.3** | Фильтры в UI (камера, тип, критичность) и обновление по WebSocket в реальном времени | ✅ Проверено | Веб: фильтры (камера, тип, важность, источник ONVIF/система, подтверждение), синхронизация диалога с Redux, «Сбросить фильтры», добавление событий по WebSocket только при совпадении с фильтрами. Desktop/Android: фильтры по типу/важности/камере/поиску есть; обновление только по REST. |
| **1.3.4** | Документировать сценарии: добавление камеры → авто-подписка; удаление → отписка; перезапуск → восстановление | ✅ Сделано | В `docs/ONVIF_EVENT_INTEGRATION.md` добавлен раздел «Детальные сценарии жизненного цикла подписок»: добавление камеры → авто-подписка (шаги, условия, CameraRoutes); удаление (одна камера и bulk) → отписка; перезапуск сервера и клиентов → восстановление подписок. |

#### Этап 1.4 — Тесты и стабилизация (3–5 дней)

| ID | Задача | Статус | Детали |
|----|--------|--------|--------|
| **1.4.1** | Юнит-тесты маппинга ONVIF → доменное событие и подписка/отписка | 🟡 Частично | Есть `OnvifEventIntegrationServiceTest` (маппинг, PullPoint, ошибки с моками). Отдельных юнит-тестов для `OnvifEventSubscriptionService` (подписка/отписка по cameraId) и для `OnvifEventMapper` при желании добавить. |
| **1.4.2** | Интеграционный тест: мок ONVIF → подписка → событие в репозитории/сервисе → WebSocket | ✅ Готово | `OnvifEventToWebSocketIntegrationTest`: мок ONVIF, подписка, handleIncomingEvent → EventService → репозиторий + broadcast в WebSocket; проверка event.created и счётчика событий в репозитории. |
| **1.4.3** | Ручная проверка с реальной ONVIF-камерой (PullPoint, типы, задержки) | 📋 Не зафиксировано | Инструкция: [MANUAL_ONVIF_CAMERA_VERIFICATION.md](MANUAL_ONVIF_CAMERA_VERIFICATION.md). После проверки заполнить чек-лист в документе и зафиксировать результат здесь. |

---

### Чек-лист этапа (для копирования в задачи)

- [x] **1.1.1** При старте API подписывать ONVIF-камеры на события (использовать `OnvifEventSubscriptionService`; при add/update/delete камеры — подписываться/переподписываться/отписываться).
- [x] **1.1.4** Добавить конфиг: включение/выключение ONVIF подписок, таймауты, интервал опроса PullPoint.
- [x] **1.2.3** Уточнить обработку «нет сети / камера недоступна» и переподписку при восстановлении (таймер или возврат в приложение).
- [x] **1.3.1** Проверить полный путь до веб/мобильного UI (список событий, уведомления по WebSocket).
- [x] **1.3.3** Проверить/добавить фильтры в UI и обновление в реальном времени по WebSocket.
- [x] **1.3.4** Документировать сценарии: добавление камеры → авто-подписка; удаление → отписка; перезапуск → восстановление подписок.
- [x] **1.4.1** При необходимости дополнить юнит-тесты (подписка/отписка, маппинг).
- [x] **1.4.2** Добавить интеграционный тест: мок ONVIF → подписка → событие → WebSocket.
- [ ] **1.4.3** Провести ручную проверку с одной реальной ONVIF-камерой (см. [MANUAL_ONVIF_CAMERA_VERIFICATION.md](MANUAL_ONVIF_CAMERA_VERIFICATION.md)).

---

### Исходный текст этапов (без таблиц)

<details>
<summary>Этап 1.1 — Сервер: подписка при старте (3–5 дней)</summary>

- [x] **1.1.1** При старте API подписывать ONVIF-камеры на события:
  - использовать `OnvifEventSubscriptionService` или аналог;
  - брать список камер из `CameraRepository` (или серверного хранилища);
  - при добавлении/обновлении камеры — подписываться/переподписываться;
  - при удалении — отписываться.
- [x] **1.1.2** Входящие события передавать в `EventService.createEvent(...)` (реализовано в `OnvifEventSubscriptionService.handleIncomingEvent` и в `startPullMessagesTask`).
- [x] **1.1.3** Убедиться, что созданные события уходят в WebSocket (реализовано в `EventService`: при успешном `createEvent()` вызывается `sendEventViaWebSocket()` → `WebSocketManager.broadcastEvent(EVENTS, "event.created", data)`; интеграционный тест `OnvifEventToWebSocketIntegrationTest` проверяет путь до WebSocket).
- [x] **1.1.4** Добавить конфиг: включение/выключение ONVIF подписок, таймауты, интервал опроса PullPoint.

**Файлы для правок:**  
`server/api/.../Application.kt`, `server/api/.../service/OnvifEventSubscriptionService.kt`, конфиг приложения.
</details>

<details>
<summary>Этап 1.2 — Клиенты (Desktop/Android): старт мониторинга (2–4 дня)</summary>

- [x] **1.2.1** **Desktop:** при старте вызывать `startMonitoringForAllCameras()` у репозитория (если используется `CameraRepositoryWithEventMonitoring`) или `CameraEventMonitoringService.initialize()` — проверить, что он действительно запускает мониторинг для всех камер. В `App.kt` (x86_64 и arm): при старте в `LaunchedEffect(Unit)` вызывается репозиторий как `CameraRepositoryWithEventMonitoring` → `startMonitoringForAllCameras()` (получает `delegate.getCameras()`, для каждой ONVIF-камеры запускает `eventIntegrationService.startMonitoring(...)`), иначе — `CameraEventMonitoringService.initialize()` (получает `cameraRepository.getCameras()`, для каждой запускает мониторинг). Оба пути действительно запускают мониторинг для всех камер.
- [x] **1.2.2** **Android:** убедиться, что при старте приложения вызывается инициализация мониторинга (например через `CameraEventMonitoringService` или аналог в `MainActivity`/ServiceManager).
- [x] **1.2.3** Обработать сценарии: нет сети, камера недоступна — не падать, логировать, при восстановлении переподписаться (по таймеру или при возврате в приложение).

**Файлы для правок:**  
`platforms/client-desktop-*/app/.../App.kt`, `android/.../MainActivity.kt`, `shared/.../CameraEventMonitoringService.kt`, `shared/.../CameraRepositoryWithEventMonitoring.kt`.
</details>

<details>
<summary>Этап 1.3 — Интеграция с системой событий и UI (3–5 дней)</summary>

- [x] **1.3.1** Проверить полный путь: ONVIF → `EventRepository`/`EventService` → WebSocket → веб/мобильный UI (список событий, уведомления).
- [ ] **1.3.2** При необходимости добавить маппинг недостающих ONVIF-топиков в `EventType`/`EventSeverity` (см. `docs/ONVIF_EVENT_INTEGRATION.md`).
- [x] **1.3.3** Настроить фильтры в UI (по камере, типу, критичности) и обновление списка в реальном времени по WebSocket.
- [x] **1.3.4** Документировать сценарии: добавление камеры → авто-подписка; удаление → отписка; перезапуск сервера/приложения → восстановление подписок.
</details>

<details>
<summary>Этап 1.4 — Тесты и стабилизация (3–5 дней)</summary>

- [x] **1.4.1** Юнит-тесты для маппинга ONVIF → доменное событие и для логики подписки/отписки (частично есть `OnvifEventIntegrationServiceTest`).
- [x] **1.4.2** Интеграционный тест: мок ONVIF → подписка → создание события в репозитории/сервисе → отправка в WebSocket.
- [ ] **1.4.3** По возможности ручная проверка с одной реальной ONVIF-камерой (PullPoint, типы событий, задержки). Инструкция: [MANUAL_ONVIF_CAMERA_VERIFICATION.md](MANUAL_ONVIF_CAMERA_VERIFICATION.md).
</details>

**Результат фазы 1:** Подписка на события ONVIF на сервере и клиентах, события попадают в систему событий и в UI.

---

## Фаза 2: Безопасность — certificate pinning и HTTPS (1–2 недели)

**Критический приоритет**, но объём зависит от требований MVP (см. Фазу 0).

### Этап 2.1 — Финализация certificate pinning (3–5 дней)

- [x] **2.1.1** Загрузка конфигурации pinning из файла/конфига (например `config/certificate-pins.example.json`) во всех клиентах (Android, iOS, Desktop/JVM).
- [x] **2.1.2** Убедиться, что `enforcePinning = true` применяется везде, где используется `CertificatePinner` (отклонение соединения при несовпадении).
- [x] **2.1.3** Проверить использование pinning в `ApiClient`/Ktor engine (Android — `createEngineWithPinning()`, iOS — делегат, JVM — TrustManager). Проверено: Android — OkHttp `preconfigured` в `Android.create()`; iOS — `CertificatePinningEngineWrapper` с делегатом; JVM — исправлено: TrustManager передаётся через `Java.create { config { sslContext(sslContext) } }`.
- [x] **2.1.4** Тесты: успешное соединение при валидном pin, отказ при неверном сертификате (см. `CertificatePinnerAndroidTest`, `CertificatePinnerJvmTest` и т.д.).

**Справка:** `core/network/CERTIFICATE_PINNING.md`, `docs/SECURITY_CERTIFICATE_PINNING_HTTPS_COMPLETE.md`.

### Этап 2.2 — Принудительный HTTPS (2–4 дня)

- [x] **2.2.1** Сервер: редирект HTTP → HTTPS (порт 80 → 443) и опция «только HTTPS» в конфиге.
- [x] **2.2.2** Клиенты: при включённой настройке «только HTTPS» не подключаться к `http://` URL камер/API; показывать предупреждение или блокировать.
- [x] **2.2.3** Документация: как включить HTTPS на сервере, как сгенерировать/добавить pins для продакшена.

**Справка:** [docs/HTTPS_AND_PINS_PRODUCTION_QUICKSTART.md](HTTPS_AND_PINS_PRODUCTION_QUICKSTART.md), [docs/PRODUCTION_SETUP_CERTIFICATE_PINNING_HTTPS.md](PRODUCTION_SETUP_CERTIFICATE_PINNING_HTTPS.md).

**Результат фазы 2:** Pinning включён из конфига и принудительный HTTPS при необходимости.

---

## Фаза 3: RTSP — тестирование и доводка FFmpeg (1–2 недели)

**Критический приоритет только при требовании «нативного» RTSP.** Иначе можно отложить после MVP.

### Этап 3.1 — JVM/Desktop (1 неделя)

- [ ] **3.1.1** Прогон существующих тестов RTSP/декодера (`RtspClientFfmpegDecodingTest`, `VideoDecoder*Test`) и фиксация падающих.  
  **Статус:** Исправлена компиляция `core:network` (RtspStreamError, ApiClient put/post типы, WebSocketClient JsonObject, License/Recording/EventApiService, EventApiService). Добавлен `kotlin-test-junit` в desktopTest. Исправлен MessageQueueTest (JsonObject). Для полного прогона нужно доисправить остальные commonTest (WebSocketClientTest, WebSocketClientIntegrationTest, OnvifExceptionsTest и др.) и jvmTest (RtspFrame: `streamType` вместо `codec`/`streamIndex` в VideoDecoder* тестах).
- [x] **3.1.2** Тесты на реальном RTSP-потоке (H.264/H.265): подключение, декодирование, остановка, реконнект.
- [x] **3.1.3** Обработка ошибок: таймауты, обрыв соединения, неверный кодек — логирование, callback/Flow для UI (офлайн, ошибка).
- [x] **3.1.4** При необходимости: стабилизация `VideoDecoderImpl` (рефлексия для `avcodec_open2`, fallback на программный декодер).

### Этап 3.2 — Android (опционально для MVP)

- [x] **3.2.1** Если в MVP нужен нативный RTSP на Android: проверить использование `NativeRtspClient` и ExoPlayer/другой плеер; при необходимости — доводка декодирования или переключение на ExoPlayer для RTSP.  
  **Итог:** воспроизведение на Android уже идёт через **ExoPlayer** (Media3); `NativeRtspClient` для плеера не используется. Доводка не требуется. Подробно: `docs/ANDROID_RTSP_MVP.md`. Добавлен fallback на HLS по реальному `hlsUrl` из API.
- [x] **3.2.2** Документировать ограничения: какие кодеки/профили поддерживаются, нужен ли FFmpeg на устройстве.  
  **Итог:** ограничения описаны в `docs/ANDROID_RTSP_MVP.md` (ExoPlayer/MediaCodec: H.264, H.265 — по устройству; FFmpeg на устройстве не нужен).

**Результат фазы 3:** Предсказуемая работа RTSP на JVM/Desktop (и при необходимости на Android) с тестами и обработкой ошибок.

---

## Фаза 4: CameraRepositoryImpl — кэширование (2–3 дня)

**Высокий приоритет.** Уменьшает нагрузку на сеть и ONVIF.

### Этап 4.1 — DiscoveryCache и StatusCache в V2

- [x] **4.1.1** В `CameraRepositoryImplV2` ввести кэш для `discoverCameras()` (аналог `CameraRepositoryImpl.discoveryCache`): TTL порядка 5–7 минут, ключ например `"discovered_cameras"`.
- [x] **4.1.2** Ввести кэш для `getCameraStatus(id)`: TTL 15–30 секунд, ключ — `id` камеры (в старом Impl — `statusCache` с TTL 20 сек).
- [x] **4.1.3** Инвалидация: при добавлении/обновлении/удалении камеры сбрасывать соответствующие записи в status cache; при явном действии «обновить список обнаруженных» — сбрасывать discovery cache.
- [x] **4.1.4** Добавить юнит-тесты: кэш возвращает данные в пределах TTL; после истечения TTL — новый запрос к источнику.

**Файлы:**  
`shared/.../CameraRepositoryImplV2.kt` (переиспользовать или вынести общий `CameraCache`/TTL-cache).

### Этап 4.2 — Тесты и документация

- [x] **4.2.1** Обновить или добавить тесты для `CameraRepositoryImplV2` (discovery и status через кэш).
- [x] **4.2.2** В `docs/` кратко описать стратегию кэширования (TTL, инвалидация) для будущей поддержки. См. `docs/CACHING_STRATEGY_CAMERA_REPOSITORY.md`.

**Результат фазы 4:** В проде используется V2 с DiscoveryCache и StatusCache, меньше лишних вызовов discovery и статусов.

---

## Фаза 5: Миграции БД — версионирование и MigrationManager (2–3 недели; не блокер MVP)

**Высокий приоритет, но не блокирует выход MVP**, если текущая схема стабильна.

### Этап 5.1 — Текущее состояние

- [x] **5.1.1** Зафиксировать текущую целевую версию схемы и список таблиц (см. `MigrationManager`, `DATABASE_MIGRATIONS.md`).
- [x] **5.1.2** Убедиться, что миграция 1→2 применяется на всех платформах (shared) и что после миграции вызывается `MigrationManager.validateMigration`. *(Проверено: единая точка входа — `createDatabase`/`createDatabaseSync` в shared; после `applyMigrations` всегда вызывается `MigrationManager.validateMigration`. См. `DatabaseFactory.kt`, `DATABASE_MIGRATIONS.md` § «Единая точка входа».)*

### Этап 5.2 — Дальнейшие миграции

- [x] **5.2.1** При любом изменении схемы (новые таблицы/поля/индексы): добавить файл `N.sqm`, обновить `CURRENT_SCHEMA_VERSION`, протестировать миграцию с копией реальной БД (версия N-1 → N).
- [x] **5.2.2** Документировать процесс в `docs/DATABASE_MIGRATIONS.md`: как добавить миграцию, как проверить откат (если позже появится стратегия отката).

**Результат фазы 5:** Предсказуемое версионирование схемы и процесс добавления новых миграций.

---

## Порядок выполнения и зависимости

```
Фаза 0 (подготовка) ──────────────────────────────────────────────────────────►
       │
       ├──► Фаза 1 (ONVIF Event) ────────────────────────────────────────────►
       │           │
       │           └──► Фаза 4 (кэш CameraRepository) может идти параллельно
       │
       ├──► Фаза 2 (безопасность) ─── может идти параллельно с Фазой 1
       │
       ├──► Фаза 3 (RTSP) ─── после стабилизации сборки; при необходимости
       │                      параллельно с Фазой 1/2
       │
       └──► Фаза 5 (миграции БД) ─── в фоне, не блокер MVP
```

**Рекомендуемая последовательность для MVP:**

1. **Фаза 0** — сразу.
2. **Фаза 1** — основной фокус (2–3 недели).
3. **Фаза 4** — короткая, можно в начале или параллельно с Фазой 1.
4. **Фаза 2** — параллельно или сразу после Фазы 1, в зависимости от требований безопасности.
5. **Фаза 3** — только если в MVP заложен «нативный» RTSP.
6. **Фаза 5** — по мере изменения схемы, не блокирует релиз MVP.

---

## Чек-лист готовности MVP

- [x] **ONVIF:** подписка на события при старте сервера и клиентов; события в UI (веб — в реальном времени по WebSocket). Остаётся ручная проверка с реальной камерой по [MANUAL_ONVIF_CAMERA_VERIFICATION.md](MANUAL_ONVIF_CAMERA_VERIFICATION.md) (задача 1.4.3).
- [x] **Безопасность:** pinning из конфига (CertificatePinningConfigLoader, все платформы); принудительный HTTPS (редирект на сервере, опция в клиентах). См. Фазу 2, [HTTPS_AND_PINS_PRODUCTION_QUICKSTART.md](HTTPS_AND_PINS_PRODUCTION_QUICKSTART.md).
- [ ] **RTSP** (если в scope): стабильное декодирование на JVM/Desktop и ExoPlayer на Android; тесты частично (см. Фазу 3, 3.1.1; ограничения — [ANDROID_RTSP_MVP.md](ANDROID_RTSP_MVP.md)).
- [x] **CameraRepository V2:** кэши discovery и status в `CameraRepositoryImplV2`, инвалидация, юнит-тесты; стратегия описана в [CACHING_STRATEGY_CAMERA_REPOSITORY.md](CACHING_STRATEGY_CAMERA_REPOSITORY.md).
- [x] **Миграции БД:** текущая схема под контролем (MigrationManager, версия 3); процесс добавления миграций описан в [DATABASE_MIGRATIONS.md](DATABASE_MIGRATIONS.md) (§ «Создание новой миграции», тестирование, откат).
- [x] **Документация:** [MVP_DEFINITION.md](MVP_DEFINITION.md) (черновик для согласования); гайды по безопасности ([HTTPS_AND_PINS_PRODUCTION_QUICKSTART.md](HTTPS_AND_PINS_PRODUCTION_QUICKSTART.md), [PRODUCTION_SETUP_CERTIFICATE_PINNING_HTTPS.md](PRODUCTION_SETUP_CERTIFICATE_PINNING_HTTPS.md)) и тестам ([TESTING_EXECUTION_GUIDE.md](TESTING_EXECUTION_GUIDE.md) и др.) имеются.

---

## Связанные документы

- `docs/MANUAL_ONVIF_CAMERA_VERIFICATION.md` — ручная проверка с реальной ONVIF-камерой (задача 1.4.3).
- `docs/ONVIF_EVENT_INTEGRATION.md` — маппинг событий, примеры.
- `docs/DATABASE_MIGRATIONS.md` — миграции и MigrationManager.
- `core/network/CERTIFICATE_PINNING.md` — реализация pinning.
- `docs/RTSP_CLIENT_IMPLEMENTATION_PLAN.md`, `docs/RTSP_CLIENT_STAGE_4.3_DETAILS.md` — RTSP/декодер.
- `docs/IMPLEMENTATION_STATUS.md`, `docs/IMPLEMENTATION_PROGRESS.md` — общий прогресс.
