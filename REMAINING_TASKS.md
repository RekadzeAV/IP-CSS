# 📋 Сводный список незавершённых задач IP-CSS

**Дата:** 15.09.2026
**Версия:** 0.5.1.1-beta
**Общий прогресс:** ~99% (P0-контур закрыт 29.08–04.09: аналитика/PTZ/ffmpeg/репозитории на PostgreSQL; остались mobile/infra/CI-задачи)
**Оперативный план:** `PLAN_EXECUTION_MASTER.md` — этап 8 (полевая валидация) **частично пройден 14.09**: сервер загружается (`/health`=200), RTSP-валидация на локальных камерах **14/14 PASS** (`0458ad4a`, `2881efc4`); остаток этапа 8 — ONVIF backlog P0 + GO/NO-GO. Далее 9 (релиз 0.6.0-beta), 10 (v1.0.0)
**План завершения (последовательный, без возвратов):** `PLAN_COMPLETION_2026_Q4.md` — **Фаза 0 ✅ (push), Фаза 1 ✅ (KMP-гейты, detekt, gitignore, techdebt), Фаза 2 🟡 (2.1 AI-агент: pytest-гейт зелёный 42 passed/2 skipped; e2e — порт A), Фаза 3 ✅ (server-build, link-check 0 битых, секреты выведены из git, CI ужесточён; Trivy — CI-гейт)**. Далее Фаза 4 🟡 (**частично пройдена 14.09**: сервер `/health`=200, RTSP 14/14 PASS; остаток ONVIF backlog + GO/NO-GO — порт A1), Фаза 5 (релиз — после GO/NO-GO), Фаза 6 (пост-релиз). Программно-выполнимая часть без внешних ресурсов завершена.
**Статус этапов:** 0–3 ✅ (остаток `docs/planning` → этап 10), 4 ✅ (дубль Android устранён: в `platforms/client-android` только `README.md`; `:android:app` — единый источник в `settings.gradle.kts`), 5 🟡 (осталось `google-services.json` — порт A3), 6 🟡 (npm 22→0 ✅, dependabot ✅; остаток gradle-контур → этапы V3–V4), 7 ✅ (`b83b6140` сборки server/Docker/MSI/APK), 8 🟡 (**14.09: сервер стартует, `/health`=200, RTSP `field-validation` 14/14 PASS на локальных камерах** — `0458ad4a`/`2881efc4`; остаток ONVIF backlog P0 + GO/NO-GO), 9–10 ⬜

---

## Легенда

| Маркер | Значение |
|--------|----------|
| 🔴 | Высокий приоритет — блокирует релиз |
| 🟡 | Средний приоритет — желательно до релиза |
| 🟢 | Низкий приоритет — можно после релиза |
| ✅ | Выполнено |
| ⬜ | Не начато |

---

## 1. 🔴 Серверный stub-контур (блокирует «PRODUCTION READY»)

> Проект честно маркируется как DEVELOPMENT, пока эти заглушки не заменены реальными реализациями.

| # | Задача | Файл(-ы) | Статус |
|---|--------|----------|--------|
| 1.1 | ~~Подключить реальную нативную AI-аналитику на сервере~~ | **Реализовано 29.08 (вечер)**: **RtspFrameSource** (ProcessBuilder ffmpeg → RGB24 Flow, graceful `isAvailable()`), **FrameCodec** (JPEG↔RGB24), **MotionZoneFilter** (ray-casting зон), **NativeAnalyticsFactory** (JNI graceful), **ServerCameraRepository** (SQLDelight, credentials для RTSP, DI), **MotionDetectorService** (RTSP→JNI MOG2→зоны→cooldown→событие→снапшот→уведомление), **ObjectDetectionService** (YOLO ONNX: env `OBJECT_DETECTION_MODEL`/`models/*.onnx`, класс-фильтр, cooldown, события/снапшоты/уведомления, on-demand `processFrame/analyzeFrame`), **VideoAnalyticsService** (реальный конвейер motion+objects+трекинг+метрики/статусы, replace стаба). Graceful degradation: без ffmpeg/native — диагностическая ошибка запуска, сервер работает. **+10 тестов** (MotionZoneFilterTest 8, Graceful 2) | ✅ |
| 1.4 | ~~Миграция server-репозиториев на SQLDelight/PostgreSQL~~ | **ServerEventRepositorySqlDelight** (event) + **ServerRecordingRepositorySqlDelight** (recording) + **ServerCameraRepository** (RTSP creds) + **ServerSettingsRepositorySqlDelight** (setting) — все в DI, in-memory удалён; +23 теста (Recording 9, Settings 14) через createDatabaseSync | ✅ Полностью (04.09) |
| 1.2 | ~~Реальный PTZ-контроль через ONVIF/проприетарные протоколы~~ | `routing/CameraRoutes.kt` + **новый** `service/CameraControlService.kt` | ✅ Реализовано 29.08: ContinuousMove/Stop/Zoom/**GotoPreset** через `OnvifClient` (добавлен `gotoPreset` + `createGotoPresetRequest`); timeout 10с, clamp speed 0.1–1.0, HTTP-семантика 400/502; **+12 unit-тестов** `CameraControlServiceTest` |
| 1.3 | ~~Реальные ffmpeg-операции (encode/export/transcode/pipe)~~ | `service/FfmpegService.kt` | ✅ Реализовано 29.08: ProcessBuilder `ffmpeg`/`ffprobe` (env `FFMPEG_PATH`/`FFPROBE_PATH` → PATH-lookup), real encode/export/transcode/pipe, `getVideoInfo` через ffprobe JSON; graceful `false`/`null` без бинарника; `AudioRecordingTest` переработан |
| 1.7 | ~~Удалить мёртвые стабы~~ | `LdapUserDetailsService` (Spring-эпоха, реальный LDAP — `LdapAuthService`), `TelegramBotService` (дублирует `TelegramNotificationSender`+`TelegramBroadcastChannel`) | ✅ Удалены (`git rm`) + убран тест LdapUserDetailsServiceTest + DI-регистрация TelegramBotService |
| 1.8 | ~~DatabasePerformanceService~~ | `service/DatabasePerformanceService.kt` | ✅ Реальная статистика HikariCP-пула (`getPoolStats`: active/idle/total/awaiting/timeout/maxPool/minIdle) + `checkHealth` через JDBC-метаданные; +3 теста |
| 1.9 | ~~MotionNotificationService~~ | `service/MotionNotificationService.kt` | ✅ Маршрутизация уведомлений движения в реальный `NotificationService` (HIGH priority, cameraId/eventId/recordingId, extras) + сохранение снапшота; +3 теста |
| 1.5 | ~~Убрать UseCase-стабы~~ | `server/api/.../shared/domain/usecase/UseCaseStubs.kt` | ✅ **Файл удалён** (`git rm`): затенял реальные UseCase из `shared` (тот же FQN-пакет), 0 потребителей; из `AppModule` убраны 7 стаб-регистраций |
| 1.6 | ~~EventRepository не зарегистрирован в server DI~~ | `di/AppModule.kt` | ✅ **Починено 29.08**: `ServerEventRepository` зарегистрирован — ранее `EventRoutes` (`/events`) падал с Koin InstanceCreationException в runtime (тесты не ловили: свой Koin) |
| 1.7 | ~~ScannerUtils: расчёт оставшегося времени сканирования~~ | `core/network/.../utils/ScannerUtils.kt`, `NetworkScanner.kt` | ✅ Реализовано 29.08: `NetworkScanProgress.elapsedMs` (заполняется в эмиттерах), `remainingTimeSeconds/Formatted` — линейная экстраполяция; **+7 юнит-тестов** `ScanProgressEtaTest` |

## 2. 🟡 Mobile (Android/iOS) — финальная полировка

| # | Задача | Статус |
|---|--------|--------|
| 2.1 | iOS: проверить сборку всех 5 экранов (требует macOS/Xcode) | ⬜ (нет окружения) — 10 Swift-файлов в `platforms/client-ios/IP-CSS/` (App/Core/UI), `.xcodeproj` отсутствует → порт A2 |
| 2.2 | Android: `google-services.json` для FCM | 🟡 05.09: **блокер снят** — FCM-сервис+каналы+регистрация токена готовы, google-services подключается условно; остаётся получить конфиг из Firebase Console (внешняя зависимость) |
| 2.3 | Android: `POST_NOTIFICATIONS` permission (API 33+) | ✅ 05.09: манифест (предсуществовавший) + runtime-запрос в MainActivity (TIRAMISU) |
| 2.4 | Собрать Android-модуль `core:ui-bridge` | ✅ 05.09: `:core:ui-bridge:assemble` зелёная, AAR debug/release собраны, androidMain компилируется |
| 2.5 | Реальные уведомления Telegram/email/SMS-каналы (сейчас broadcast-заглушки) | ✅ проверено 05.09: серверные отправители **реальные** — TelegramNotificationSender (Ktor → api.telegram.org), EmailNotificationSender (JavaMail SMTP/STARTTLS), PushNotificationDelivery (FCM HTTP v1, RS256); SMS не в скоупе |
| 2.6 | Mobile security logger → отправка критических событий на сервер | ✅ 05.09: `SecureMobileSecurityLogger.remoteSink` (core/common androidMain, @Volatile) → `SecurityEventUploader` (android/app: POST /api/v1/audit/client-events, батч, маппинг Mobile→Server enum, when-без-else для новых типов, JSON-escape, неблокирующий поток); install() в MainActivity; core:common debug+desktop тесты зелёные |

## 3. 🟢 Инфраструктура и документация

| # | Задача | Статус |
|---|--------|--------|
| 3.1 | Настроить GitHub Secrets (`DOCKER_USERNAME`, `DOCKER_PASSWORD`, `GH_TOKEN`) + registry `ghcr.io/rekadzeav/ip-css` | ⬜ |
| 3.2 | Обработка 112+ уязвимостей dependabot (61 high, 54 moderate, 5 low) | 🟡 **npm-контур закрыт 14.09: 22 → 0** (`bcc8193b`, 5 high + 17 moderate; `overrides` postcss/extract-zip + lighthouse 13.4.1); **dependabot включён** (`.github/dependabot.yml`, npm/gradle/pip/github-actions, weekly) + CI-гейт `security-audit` в `ci.yml`. Ранее (07.09, `1f8d4fec`): bump ktor 2.3.13, sqldelight 2.0.2, datetime 0.6.1, bouncycastle 1.79, mockk 1.13.9, turbine 1.1.0, koin 3.5.6, detekt 1.23.6, ktlint 12.1.2, dokka 1.9.20. **Остаток: gradle-контур + pip-lock (V3–V4)** → см. `PLAN_VULNERABILITIES.md` |
| 3.3 | Прогнать KMP-гейты `verify-kmp-phase1.py` в полном режиме (с Gradle/native шагами) | ✅ **14.09**: полный прогон `verify-kmp-phase1.py --root . --report-json` (5 python-гейтов + 3 gradle-шага) — **100% зелёный** (000 exit); КОТЛИН-гейты `check-commonmain-forbidden-imports`/`check-security-expect-actual-signatures` 22/22; лог в `build-logs/kmp-phase1-full.json` (gitignored) |
| 3.4 | Подключить `integrationTest` (PostgreSQL/Redis) в CI | ✅ В `ci.yml` строгий гейт `./gradlew :server:api:integrationTest` (без `\|\| true`), services postgres/redis подняты (Этап 2, 04.09); перепроверено 14.09 |
| 3.5 | Gradle-контур: CVE-скан + обновления зависимостей (остаток Этапа 6) | 🟡 dependabot для gradle активен (`.github/dependabot.yml`); 15.09: волна Dependabot инвентаризирована (23 PR → волны W2–W9 в `PLAN_VULNERABILITIES.md`, Этап V4): W2 gradle-wave **(ИСПОЛНЕНО напрямую 16.09: foojay 0.8.0→1.0.0, coroutines 1.10.2, bouncycastle 1.77→1.86, detekt 1.23.6→1.23.8; PR#24 supersede; client-desktop-arm починен)**, W3 ktlint 14, W4 koin 4.2.2, W5 ktor 3.5.2 (после 0.6.0-beta), W6 npm-minor, W7 eslint/jest мажоры, W8 typescript 7 (после релиза), W9 pip (после V3). **W1 (github-actions) закрыта 15.09** — бампы применены в main напрямую (`0401cdc1`, `2b5d1432`), PR#6–#10 закрыты как superseded; **github-actions dependabot.yml актуализирован 16.09** — allow контурные actions, ignore won'> majors → не порождает новые PR. |
| 3.6 | pip-lock для `ai-agent/requirements.txt` (воспроизводимость + pip-audit) | ⬜ `requirements.lock` отсутствует; открытые диапазоны `>=` → этап V3 `PLAN_VULNERABILITIES.md` |
| 3.7 | Чистка устаревших веток GitHub (30 legacy-веток: `develop/*`, `test/*`, `dev/*`, `feature/*`, `refactor/*`, `chore/*`, `main-old`, `old_prodject_alfa_0.0.1_defeat`, `commit`) | ✅ **15.09**: удалены с remote; полный бэкап — `old-branches.bundle` (34 refs, 61,3 MB, complete history, verified) + refs `refs/archive/branches-2026-09-15/*` локально; README+запись в `ARCHIVE_MANIFEST.md`. Dependabot-ветки (23) и `main` не затронуты. **Бандл хранится локально** (61MB > 50MB лимита GitHub) — скопировать на NAS/внешний диск |

---

## 7. 🟡 Этап 7 — Тестовые сборки всех платформ

> Зависимости обновлены (Этап 6), тесты зелёные. Артефакты собраны 07.09.

| # | Задача | Платформа | Статус |
|---|--------|-----------|--------|
| 7.1 | JAR-сборка сервера (`:server:api:installDist`) | Desktop | ✅ 07.09: `build/install/api/` + `distributions/api-0.5.1.1-beta.{tar,zip}` |
| 7.2 | Docker-образ | Docker | ✅ 07.09: `docker images ipcss:0.5.1.1-beta` (1.46 GB, ID `799ad9c78ad5`) |
| 7.3 | Desktop-установщик (MSI/DEB/DMG) | Desktop | ✅ 07.09: `IP-CSS Desktop-1.0.0.msi` (302 MB); JAR `app-0.5.1.1-beta.jar`; fix `App.kt` |
| 7.4 | Android APK | Android | ✅ 07.09: `android/app/build/outputs/apk/debug/app-debug.apk` (37.5 MB) |
| 7.5 | NAS-пакеты (SPK/QPKG/APK) | NAS | ⛔ блокировано (нет bash; нужен Linux-хост) |

---

## 8. 🟡 Этап 8 — Полевая валидация + GO/NO-GO

> **Частично пройден 14.09:** сервер собран и запущен (`/health`=200 после починки DI — коммиты `0458ad4a`, `2881efc4`), RTSP-валидация прошла на локальных камерах (**14/14 PASS**).
>
> **Остаток блокирован (перепроверено 15.09):**
> - `config/test-cameras-local-network.example.json` отсутствует (нет реестра камер)
> - ONVIF backlog P0 (WS e2e без refresh, latency-gate)
> - GO/NO-GO чеклист (`PLAN_RELEASE.md` п.2)
>
> Готово: PowerShell 7 ✅, FFmpeg 8.1.1 ✅, диагностика ✅, `scripts/field-validation.ps1` + `field-validation-preflight.ps1` ✅.

| # | Задача | Статус |
|---|--------|--------|
| 8.1 | Прогнать `field-validation.ps1` на реальных камерах (ONVIF/RTSP/HLS) | 🟡 **RTSP 14/14 PASS (14.09)**; ONVIF/HLS — остаток |
| 8.2 | ONVIF backlog P0: WS e2e без refresh, latency-gate | ⛔ требует камер |
| 8.3 | GO/NO-GO по PLAN_RELEASE | ⛔ после 8.1–8.2 |

---

## ✅ УЖЕ ВЫПОЛНЕНО (дополнение от 29.08.2026)

| # | Задача | Статус |
|---|--------|--------|
| 1 | Repo-гигиена: `git rm --cached '--help'` и `config/postgresql.env` (утечка секретов); удалены `*.bak`, логи `full.log/r3.log/t2.log`, `onvif_cookies.txt` | ✅ |
| 2 | `.gitignore`: добавлена защита `config/*.env` (коммитятся только `*.example.env`) | ✅ |
| 3 | **S3 presigned URL** — заменён TODO `S3CloudStorageProvider.kt:193` на `S3Presigner` (GET + новый PUT); +4 unit-теста `S3CloudStorageProviderTest` (Kover) | ✅ |
| 4 | **Desktop UiBridge интегрирован с реальными UseCase** (login/logout/register, cameras (get/add/update/delete/discover/PTZ), recordings, settings, notifications, events) с legacy-fallback при null UseCase | ✅ |
| 5 | **Фабрика UiBridge** — исправлена: `createUiBridge(...)` теперь реально прокидывает UseCase через `createUiBridgeWithDependencies` (ранее параметры игнорировались) | ✅ |
| 6 | swaggerRoutes / digestAuthRoutes / discoveryRoutes подключены в `Routing.kt` (ранее числились ⬜ в этом трекере) | ✅ |
| 7 | `:core:network:desktopTest` — 544 tests / 0 failed / 38 skipped (25 падений устранены) | ✅ |
| 8 | Android `assembleDebug` собирается (APK ~33MB), D8 OOM не проявился | ✅ |
| 9 | **PTZ через ONVIF** — `CameraControlService` + `OnvifClient.gotoPreset`, роут `/control` без стаба (+12 тестов) | ✅ |
| 10 | **Реальный FfmpegService** — ffmpeg/ffprobe ProcessBuilder вместо echo-стаба | ✅ |
| 11 | **Удалён `UseCaseStubs.kt`** (shadowing реальных UseCase) + починен DI (`EventRepository`) | ✅ |
| 12 | **ScannerUtils ETA** — реализован расчёт оставшегося времени (+7 тестов) | ✅ |
| 13 | **Нативная AI-аналитика сервера (1.1)** — RtspFrameSource/FrameCodec/MotionZoneFilter/NativeAnalyticsFactory/ServerCameraRepository; Motion/Object/VideoAnalytics сервисы на реальном конвейере RTSP→JNI OpenCV (MOG2/YOLO) + graceful degradation (+10 тестов) | ✅ |
| 14 | **Уязвимости npm `server/web` (ревизия 14.09)** — 22 (5 high) → 0 без breaking changes; починен невалидный `package.json` (дубль `scripts` блокировал npm-команды пакета); реестр и процесс — `PLAN_VULNERABILITIES.md` | ✅ |
| 15 | **CI-гейт `security-audit` + Dependabot (14.09)** — `npm audit` (runtime high+ блокирует, full critical блокирует) + `deps:validate` в `ci.yml`; `.github/dependabot.yml` (npm/gradle/pip/github-actions, weekly) — раннее обнаружение CVE | ✅ |

---

## 📊 Сводка

| Метрика | Значение |
|---------|----------|
| ✅ Выполнено (итог когорты) | ~40 задач |
| ✅ Осталось критичных (P0) | 0 — все P0 закрыты (AI-аналитика, PTZ, ffmpeg, DI-фиксы, миграция репозиториев на PostgreSQL/SQLDelight) |
| 🟡 Осталось (mobile/infra) | 4: `google-services.json` (порт A3), GitHub Secrets (3.1), gradle-контур CVE (V3–V4), ONVIF backlog + GO/NO-GO (8.2–8.3) |
| 🟢 Осталось (пост-релиз, этап 10) | 6: унификация Model/DTO, detekt→0, HA/мониторинг, `docs/planning` (97 файлов), дубли гайдов, pip-lock |
W2 gradle-wave (ИСПОЛНЕНО напрямую 16.09: foojay 0.8.0 to 1.0.0, coroutines 1.10.2, bouncycastle 1.77 to 1.86, detekt 1.23.6 to 1.23.8; PR#24 supersede; client-desktop-arm починен); W3 ktlint 14
