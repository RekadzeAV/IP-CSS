# Очередь задач на выполнение: Фаза 1 до 100%

**Дата создания:** 2026-05-27  
**Версия:** 1.0  
**Цель:** Довести Фазу 1 (MVP) до 100% за 3-4 недели

**Основано на:**
- [PROJECT_STATUS_PHASES.md](../status/PROJECT_STATUS_PHASES.md) — актуальные статусы
- [PHASE1_MVP_TO_100_PLAN.md](PHASE1_MVP_TO_100_PLAN.md) — общий план
- [PHASE1_MVP_TO_100_SECTIONS_3_4_DETAILED_PLAN.md](PHASE1_MVP_TO_100_SECTIONS_3_4_DETAILED_PLAN.md) — детализация
- [PHASE1_CRITICAL_BLOCKERS_REMEDIATION_STATUS_2026-05-25.md](../reports/PHASE1_CRITICAL_BLOCKERS_REMEDIATION_STATUS_2026-05-25.md) — статус блокеров

---

## 📋 Легенда приоритетов

| Приоритет | Обозначение | Описание |
|-----------|-------------|----------|
| 🔴 P0 | Критический | Блокирует MVP, выполнять в первую очередь |
| 🟠 P1 | Высокий | Критично для MVP, выполнять сразу после P0 |
| 🟡 P2 | Средний | Важно для MVP, выполнять после P0/P1 |
| 🟢 P3 | Низкий | Желательно, можно отложить при дефиците времени |

---

## 🎯 Итоговая цель

**Фаза 1 = 100%** при выполнении всех критериев:
- ✅ Все критические блокеры для MVP закрыты
- ✅ Видео-контур production-ready (RTSP/HLS/recording/events)
- ✅ Web realtime/auth/video сценарии завершены
- ✅ Security MVP критерии выполнены
- ✅ Integration/E2E smoke контур стабильно проходит
- ✅ Go/No-Go пакет подтвержден

---

## 📅 Неделя 1: Foundation + Video Core (Май 27 - Июнь 2)

### **Задача W1-0: Фиксация контуров MVP** (Приоритет: 🟡 P2)
**Срок:** 1 день  
**Статус:** ⚠️ Начато  
**Зависимости:** Нет

#### Декомпозиция:
- [ ] **W1-0.1:** Согласовать must-have сценарии (discover, live, record, replay, events, auth, web+Android+Desktop)
  - Файл: `docs/planning/MVP_PHASE1_SCOPE_BOUNDARY.md`
  - Критерий: Единый документ с подтверждением команды
  
- [ ] **W1-0.2:** Выровнять трактовку % готовности в статус-документах
  - Файлы: `PROJECT_STATUS_PHASES.md`, `TODO.md`, `PHASE1_IMPLEMENTATION_REPORT.md`
  - Критерий: Одна трактовка DoD во всех документах
  
- [ ] **W1-0.3:** Подтвердить iOS статус (вне MVP по плану)
  - Файл: `MVP_PHASE1_SCOPE_BOUNDARY.md`
  - Критерий: Явное указание "iOS не блокирует GO Фазы 1"

---

### **Задача W1-1: PostgreSQL финализация и миграции** (Приоритет: 🟠 P1)
**Срок:** 2-3 дня  
**Статус:** ⚠️ Начато (~88%)  
**Зависимости:** W1-0

#### Декомпозиция:
- [ ] **W1-1.1:** Завершить миграции SQLDelight (версии, MigrationManager, тесты)
  - Файлы: `core/database/schemas/`, `MigrationManager.kt`
  - Критерий: Все миграции имеют backward/forward тесты
  
- [ ] **W1-1.2:** Провести smoke тесты после миграции
  - Скрипт: `scripts/migration-smoke-test.ps1`
  - Критерий: Все базовые операции работают после миграции
  
- [ ] **W1-1.3:** Подготовить staging окружение для PostgreSQL cutover
  - Файл: `docker-compose.prod.yml`, `.env.production`
  - Критерий: Staging окружение готово к переключению
  
- [ ] **W1-1.4:** Провести PostgreSQL cutover на staging
  - Runbook: `POSTGRESQL_STAGING_CUTOVER_AND_ROLLBACK_RUNBOOK.md`
  - Критерий: Успешное переключение + проверка работы
  
- [ ] **W1-1.5:** Провести rollback rehearsal
  - Runbook: `POSTGRESQL_STAGING_CUTOVER_AND_ROLLBACK_RUNBOOK.md`
  - Критерий: Успешный откат + восстановление

**Артефакты:**
- Отчёт по staging cutover: `docs/reports/POSTGRESQL_STAGING_CUTOVER_REPORT_YYYY-MM-DD.md`
- Отчёт по rollback rehearsal: `docs/reports/ROLLBACK_REHEARSAL_REPORT_YYYY-MM-DD.md`

---

### **Задача W1-2: RTSP Native Integration - Аудио декодирование** (Приоритет: 🔴 P0)
**Срок:** 2-3 дня  
**Статус:** 🟡 В работе (~85%)  
**Зависимости:** W1-0

#### Декомпозиция:
- [ ] **W1-2.1:** Исправить FFmpeg 8.0 API проблемы для аудио
  - Файлы: `native/video-processing/src/audio_decoder.cpp`, `include/audio_decoder.h`
  - Критерий: Компилляция без ошибок и предупреждений
  
- [ ] **W1-2.2:** Реализовать поддержку аудио кодеков (AAC, MP3, PCMU, PCMA)
  - Файлы: `audio_decoder.cpp`, `rtsp_client.cpp`
  - Критерий: Все кодеки декодируются успешно
  
- [ ] **W1-2.3:** Добавить аудио-видео синхронизацию (AV sync)
  - Файлы: `rtsp_client.cpp`, `NativeRtspClient.kt`
  - Критерий: Синхронизация <50ms drift
  
- [ ] **W1-2.4:** Реализовать buffer management для аудио
  - Файлы: `audio_decoder.cpp`, `rtp_packet_processor.cpp`
  - Критерий: Нет переполнений/недополнений буфера

**Артефакты:**
- Тесты декодирования: `core/network/src/commonTest/.../AudioDecodingTest.kt`
- Отчёт: `docs/reports/AUDIO_DECODING_IMPLEMENTATION_REPORT.md`

---

### **Задача W1-3: RTSP Integration тестирование с реальными камерами** (Приоритет: 🔴 P0)
**Срок:** 3-4 дня  
**Статус:** ⚠️ Начато  
**Зависимости:** W1-2 (частично параллельно)

#### Декомпозиция:
- [ ] **W1-3.1:** Настроить тестовую среду с реальными камерами (7 камер из config/test-cameras.rtsp.json)
  - Файл: `config/test-cameras.rtsp.json`
  - Критерий: Все 7 камер доступны для тестирования
  
- [ ] **W1-3.2:** Запустить подключение тесты (Connection tests)
  - Скрипт: `scripts/test-rtsp-real-cameras.ps1`
  - Критерий: Все камеры подключаются успешно (>99% success rate)
  
- [ ] **W1-3.3:** Запустить видео тесты (Video tests)
  - Скрипт: `scripts/test-rtsp-real-cameras.ps1 -FullTest`
  - Критерий: Видео потоки работают без артефактов
  
- [ ] **W1-3.4:** Запустить аудио тесты (Audio tests)
  - Скрипт: `scripts/test-rtsp-real-cameras.ps1 -FullTest`
  - Критерий: Аудио потоки работают без рассинхрона
  
- [ ] **W1-3.5:** Протестировать reconnect сценарии
  - Скрипт: `scripts/test-rtsp-real-cameras.ps1 -ReconnectTest`
  - Критерий: Reconnect работает <5s с backoff
  
- [ ] **W1-3.6:** Зафиксировать результаты тестирования
  - Файл: `docs/reports/RTSP_REAL_CAMERA_TEST_RESULTS.md`
  - Критерий: Полная документация с метриками

**Артефакты:**
- Результаты тестов: `docs/reports/RTSP_REAL_CAMERA_TEST_RESULTS.md`
- Логи: `build_logs/rtsp_integration_tests_YYYY-MM-DD/`

---

## 📅 Неделя 2: Video Stability + Web Realtime (Июнь 3-9)

### **Задача W2-1: HLS Pipeline Runtime Stability** (Приоритет: 🟠 P1)
**Срок:** 2-3 дня  
**Статус:** 🟡 В работе (~88%)  
**Зависимости:** W1-1, W1-3 (частично)

#### Декомпозиция:
- [ ] **W2-1.1:** Long-run тесты HLS (24+ часа)
  - Скрипт: `scripts/hls-long-run-test.ps1`
  - Критерий: Нет утечек памяти, стабильная работа
  
- [ ] **W2-1.2:** Реализовать cleanup процессов/файлов при остановке
  - Файлы: `HlsStreamService.kt`, `FfmpegCli.kt`
  - Критерий: Все процессы завершаются, файлы чистятся
  
- [ ] **W2-1.3:** Реализовать reconnect логику для HLS потоков
  - Файлы: `HlsStreamService.kt`, `HlsGeneratorService.kt`
  - Критерий: Автоматическое восстановление при разрыве
  
- [ ] **W2-1.4:** Оптимизация буферизации для низкой задержки (<500ms)
  - Файлы: `FfmpegCli.kt`, HLS конфиги
  - Критерий: Latency <500ms в нормальных условиях

**Артефакты:**
- Отчёт по long-run тестам: `docs/reports/HLS_LONG_RUN_TEST_RESULTS.md`
- Метрики производительности: `docs/reports/HLS_PERFORMANCE_METRICS.md`

---

### **Задача W2-2: Screenshot Pipeline завершение** (Приоритет: 🟡 P2)
**Срок:** 1-2 дня  
**Статус:** 🟡 В работе (~54%)  
**Зависимости:** W1-3

#### Декомпозиция:
- [ ] **W2-2.1:** Полевая валидация FFmpeg screenshot
  - Скрипт: `scripts/test-screenshot-pipeline.ps1`
  - Критерий: Успешный захват кадра с реальных камер
  
- [ ] **W2-2.2:** Исправить обработку ошибок в `captureFrame()`
  - Файлы: `ScreenshotService.kt`, `FfmpegCli.kt`
  - Критерий: Все ошибки корректно маппятся в API responses
  
- [ ] **W2-2.3:** Добавить кэширование для часто запрашиваемых скриншотов
  - Файлы: `ScreenshotService.kt`, `ScreenshotCache.kt`
  - Критерий: TTL кэширование работает, снижает нагрузку

**Артефакты:**
- Тесты: `ScreenshotServiceIntegrationTest.kt`
- Отчёт: `docs/reports/SCREENSHOT_PIPELINE_VALIDATION.md`

---

### **Задача W2-3: WebSocket в веб-клиенте - Полная интеграция** (Приоритет: 🟠 P1)
**Срок:** 2 дня  
**Статус:** ✅ Завершено (100%) по документам, требуется верификация  
**Зависимости:** W1-1

#### Декомпозиция:
- [ ] **W2-3.1:** Верификация WebSocket reconnect с backoff
  - Файлы: `server/web/src/hooks/useWebSocket.ts`, `WebSocketProvider.tsx`
  - Критерий: Reconnect работает по exponential backoff
  
- [ ] **W2-3.2:** Проверить re-subscription после reconnect
  - Файлы: `WebSocketProvider.tsx`, Redux slices
  - Критерий: Автоматическая ре-подписка на все каналы
  
- [ ] **W2-3.3:** Dedupe сообщений и rate limiting на клиенте
  - Файлы: `WebSocketProvider.tsx`
  - Критерий: Нет дубликатов в UI при burst сообщениях
  
- [ ] **W2-3.4:** E2E тесты (connect/auth/subscribe/reconnect)
  - Файлы: `server/web/src/tests/e2e/WebSocketE2ETest.ts`
  - Критерий: Все сценарии проходят

**Артефакты:**
- Отчёт верификации: `docs/reports/WEBSOCKET_VERIFICATION_REPORT.md`

---

### **Задача W2-4: ONVIF Event Service финализация** (Приоритет: 🟡 P2)
**Срок:** 1-2 дня  
**Статус:** 🟢 Готово (~95%)  
**Зависимости:** W1-3

#### Декомпозиция:
- [ ] **W2-4.1:** Финальная ручная валидация на различных сетевых конфигурациях
  - Скрипт: `scripts/onvif-events-api-verification.ps1`
  - Критерий: Работает на разных типах сетей (NAT, прямое подключение)
  
- [ ] **W2-4.2:** Оптимизация для больших сетей (100+ устройств)
  - Файлы: `OnvifEventServiceImpl.kt`, `OnvifDiscoveryService.kt`
  - Критерий: Производительность не деградирует
  
- [ ] **W2-4.3:** Автоматизированные acceptance тесты
  - Файлы: `core/onvif/src/commonTest/.../OnvifEventsTest.kt`
  - Критерий: Тесты в CI/CD pipeline

**Артефакты:**
- Отчёт: `docs/reports/ONVIF_EVENTS_VALIDATION_REPORT.md`

---

## 📅 Неделя 3: Security + Platforms (Июнь 10-16)

### **Задача W3-1: HTTPS Enforcement и Certificate Pinning** (Приоритет: 🟠 P1)
**Срок:** 2-3 дня  
**Статус:** 🟡 В работе (~80-100%)  
**Зависимости:** W1-1, W2-1

#### Декомпозиция:
- [ ] **W3-1.1:** Полевая валидация HTTPS/pins на staging
  - Runbook: `SECURITY_MVP_FIELD_VALIDATION_RUNBOOK.md`
  - Критерий: Все запросы идут через HTTPS, pins валидируются
  
- [ ] **W3-1.2:** Certificate pinning для Desktop (Windows/Linux/macOS)
  - Файлы: `core/network/src/desktopMain/.../CertificatePinning.kt`
  - Критерий: Pinning работает на всех desktop платформах
  
- [ ] **W3-1.3:** Certificate pinning для Android
  - Файлы: `core/network/src/androidMain/.../CertificatePinning.kt`
  - Критерий: Pinning работает, сборка проходит
  
- [ ] **W3-1.4:** Валидация шифрования учётных данных камер в БД
  - Скрипт: `scripts/validate-encryption.ps1`
  - Критерий: Все чувствительные данные зашифрованы
  
- [ ] **W3-1.5:** Security logging для критических операций
  - Файлы: `SecurityLogger.kt`, `AuditRoutes.kt`
  - Критерий: Все критичные операции логируются

**Артефакты:**
- Отчёт: `docs/reports/SECURITY_MVP_FIELD_VALIDATION_REPORT.md`
- Чеклист: `SECURITY_MVP_READINESS.md` (заполненный)

---

### **Задача W3-2: Android платформа - Фоновая запись и RTSP** (Приоритет: 🟠 P1)
**Срок:** 3 дня  
**Статус:** ⚠️ Начато (~30-48%)  
**Зависимости:** W1-2, W1-3, W3-1

#### Декомпозиция:
- [ ] **W3-2.1:** Реализовать фоновую запись (Background Recording Service)
  - Файлы: `platforms/android/app/src/main/java/.../BackgroundRecordingService.kt`
  - Критерий: Запись работает в фоне при закрытом приложении
  
- [ ] **W3-2.2:** Настройка разрешений (Permissions)
  - Файлы: `AndroidManifest.xml`, `PermissionsManager.kt`
  - Критерий: Все необходимые разрешения запрашиваются и работают
  
- [ ] **W3-2.3:** Интеграция RTSP с Android Video Player
  - Файлы: `VideoPlayerActivity.kt`, `NativeRtspClient.android.kt`
  - Критерий: RTSP поток воспроизводится без задержек
  
- [ ] **W3-2.4:** Хранение секретов в Android Keystore
  - Файлы: `KeystoreTokenStorage.kt`
  - Критерий: JWT и токены хранятся в Keystore
  
- [ ] **W3-2.5:** Instrumented E2E тесты
  - Файлы: `platforms/android/app/src/androidTest/.../E2ETests.kt`
  - Критерий: Все критичные сценарии протестированы на устройстве

**Артефакты:**
- Отчёт: `docs/reports/ANDROID_MVP_VALIDATION_REPORT.md`
- Скриншоты тестов: `docs/reports/android_test_screenshots/`

---

### **Задача W3-3: Desktop платформа - Long-run стабильность** (Приоритет: 🟡 P2)
**Срок:** 2-3 дня  
**Статус:** 🟡 В работе (~70%)  
**Зависимости:** W1-2, W1-3, W3-1

#### Декомпозиция:
- [ ] **W3-3.1:** Long-run тесты видеоплеера (24+ часа)
  - Скрипт: `scripts/desktop-video-long-run-test.ps1`
  - Критерий: Стабильная работа без memory leaks
  
- [ ] **W3-3.2:** Long-run тесты экранов событий
  - Скрипт: `scripts/desktop-events-long-run-test.ps1`
  - Критерий: EventTimeline работает стабильно
  
- [ ] **W3-3.3:** Интеграция NativeRtspClient на Desktop
  - Файлы: `NativeRtspClient.native.kt`, `rtsp_client_jni_desktop.cpp`
  - Критерий: RTSP работает на Windows/Linux/macOS
  
- [ ] **W3-3.4:** Проверка ARM/x86 паритета
  - Скрипт: `scripts/desktop-architecture-test.ps1`
  - Критерий: Все платформы работают одинаково

**Артефакты:**
- Отчёт: `docs/reports/DESKTOP_MVP_VALIDATION_REPORT.md`
- Метрики: `docs/reports/DESKTOP_PERFORMANCE_METRICS.md`

---

### **Задача W3-4: Web Video Player Stability** (Приоритет: 🟡 P2)
**Срок:** 1-2 дня  
**Статус:** 🟡 В работе (~75%)  
**Зависимости:** W2-1, W2-3

#### Декомпозиция:
- [ ] **W3-4.1:** Единый backend path для RTSP/HLS во всех платформах
  - Файлы: `server/web/src/services/StreamApiService.ts`, `StreamService.kt`
  - Критерий: Все платформы используют один и тот же API
  
- [ ] **W3-4.2:** Обработка ошибок видеоплеера (единая семантика)
  - Файлы: `server/web/src/components/VideoPlayer.tsx`
  - Критерий: Ошибки маппятся в понятные UI сообщения
  
- [ ] **W3-4.3:** Lighthouse performance audit
  - Скрипт: `server/web/scripts/run-lighthouse-audit.sh`
  - Критерий: Метрики в пределах целевых значений

**Артефакты:**
- Отчёт Lighthouse: `docs/reports/WEB_LIGHTHOUSE_AUDIT_YYYY-MM-DD.html`
- Отчёт: `docs/reports/WEB_VIDEO_PLAYER_VALIDATION.md`

---

## 📅 Неделя 4: Testing + Acceptance + GO/NO-GO (Июнь 17-23)

### **Задача W4-1: Integration Tests - MVP Automated Acceptance** (Приоритет: 🟠 P1)
**Срок:** 2-3 дня  
**Статус:** 🟡 В работе (~28%)  
**Зависимости:** Все предыдущие задачи

#### Декомпозиция:
- [ ] **W4-1.1:** Расширить JVM integration tests для API
  - Файлы: `server/api/src/test/kotlin/.../*IntegrationTest.kt`
  - Критерий: Покрытие критичных endpoints >= 70%
  
- [ ] **W4-1.2:** Расширить integration тесты БД/миграций
  - Файлы: `core/database/src/test/.../MigrationManagerTest.kt`
  - Критерий: Все миграции покрыты тестами
  
- [ ] **W4-1.3:** Настроить CI job `mvp-automated-acceptance`
  - Файлы: `.github/workflows/mvp-automated-acceptance.yml`, `scripts/ci/mvp-automated-acceptance.sh`
  - Критерий: Job проходит в CI с видео gate
  
- [ ] **W4-1.4:** Video E2E acceptance profile (CI)
  - Файлы: `config/video-e2e-acceptance-profile.mvp-ci.json`
  - Критерий: Profile-aware testing в CI

**Артефакты:**
- Отчёт CI: GitHub Actions `mvp-automated-acceptance` job
- Покрытие тестами: Jacoco report

---

### **Задача W4-2: E2E / UI Тесты** (Приоритет: 🟡 P2)
**Срок:** 2 дня  
**Статус:** ❌ Не начато  
**Зависимости:** W4-1

#### Декомпозиция:
- [ ] **W4-2.1:** Определить критические пользовательские сценарии для E2E
  - Файл: `docs/testing/E2E_TEST_SCENARIOS.md`
  - Критерий: 5-7 критичных сценариев утверждены
  
- [ ] **W4-2.2:** Реализовать E2E тесты (Playwright/Cypress)
  - Файлы: `server/web/e2e/*/*.spec.ts`
  - Критерий: Все сценарии автоматизированы
  
- [ ] **W4-2.3:** Интегрировать E2E в CI pipeline
  - Файлы: `.github/workflows/e2e-tests.yml`
  - Критерий: E2E запускается в CI на каждый PR

**Артефакты:**
- E2E тесты: `server/web/e2e/`
- Отчёты: `server/web/e2e/reports/`

---

### **Задача W4-3: Go/No-Go Gate и Финальные отчёты** (Приоритет: 🔴 P0)
**Срок:** 1-2 дня  
**Статус:** ⚠️ Начато  
**Зависимости:** W4-1, W4-2, все предыдущие

#### Декомпозиция:
- [ ] **W4-3.1:** Запустить `w4-mvp-platform-and-gate.ps1`
  - Скрипт: `scripts/w4-mvp-platform-and-gate.ps1 -Full`
  - Критерий: Все платформы проходят smoke тесты
  
- [ ] **W4-3.2:** Запустить `video-e2e-go-no-go.ps1`
  - Скрипт: `scripts/video-e2e-go-no-go.ps1`
  Критерий: Video gate проходит по profile
  
- [ ] **W4-3.3:** Запустить `generate-phase1-go-no-go-summary.ps1`
  - Скрипт: `scripts/generate-phase1-go-no-go-summary.ps1`
  - Критерий: Генерируется итоговый отчёт GO/CONDITIONAL/NO-GO
  
- [ ] **W4-3.4:** Заполнить RELEASE_GO_NO_GO_CHECKLIST.md
  - Файл: `docs/planning/RELEASE_GO_NO_GO_CHECKLIST.md`
  - Критерий: Все чекбоксы проверены
  
- [ ] **W4-3.5:** Подготовить финальные отчёты
  - Файлы:
    - `docs/reports/PHASE1_FINAL_COMPLETION_REPORT.md`
    - `docs/reports/PHASE1_MVP_READINESS_ASSESSMENT.md`
    - `docs/reports/PHASE1_GO_NO_GO_DECISION.md`
  - Критерий: Все отчёты утверждены командой

**Артефакты:**
- GO/NO-GO решение: `docs/reports/PHASE1_GO_NO_GO_DECISION.md`
- Финальный отчёт: `docs/reports/PHASE1_FINAL_COMPLETION_REPORT.md`
- Release notes: `RELEASE_NOTES.md` (обновлённый)

---

## 📊 Матрица приоритетов при дефиците ресурса

### Если времени мало (сжать до 2 недель):

| Приоритет | Задачи | Отложить |
|-----------|--------|----------|
| **P0** (обязательно) | W1-2, W1-3, W2-1, W3-1, W4-3 | Всё остальное |
| **P1** (критично) | W1-1, W2-3, W3-2 | W2-2, W2-4, W3-3, W3-4, W4-2 |
| **P2** (желательно) | W1-0, W4-1 | W3-4, W4-2 |
| **P3** (можно без) | - | W2-2, W2-4 |

### Если нет тестового оборудования (камер):

1. Эмулировать RTSP через сокет-сервер
2. Использовать публичные RTSP тестовые потоки
3. Сфокусироваться на unit/integration тестах без реального железа

---

## 🎯 Критерии завершения Фазы 1

### Обязательные (MVP Must-Have):

- [x] **1.1** Инфраструктура и основа ✅ 100%
- [ ] **1.2** Доменный слой ≥ 90% (осталось 1.2.4 в Phase 2)
- [x] **1.3** Слой данных ≥ 95% ✅ 98%
- [ ] **1.4** Сетевой слой ≥ 95% (W1-2, W1-3, W2-4)
- [ ] **1.5** Серверная часть ≥ 95% (W1-1)
- [ ] **1.6** Веб-интерфейс ≥ 95% (W2-3, W3-4)
- [ ] **1.7** Мобильные и десктоп ≥ 80% (W3-2, W3-3)
- [ ] **1.8** Видео и запись ≥ 90% (W1-2, W1-3, W2-1, W2-2)
- [ ] **1.9** Безопасность ≥ 90% (W3-1)
- [ ] **1.10** Тестирование ≥ 50% (W4-1, W4-2)

### Критические блокеры:

- [ ] RTSP Native Integration работает стабильно на реальных камерах
- [ ] HLS Pipeline работает без утечек в long-run (24h+)
- [ ] WebSocket real-time работает с reconnect
- [ ] Certificate Pinning валидирован на staging
- [ ] PostgreSQL cutover + rollback rehearsal успешны
- [ ] All integration tests passing
- [ ] GO/NO-GO = GO

---

## 📈 Метрики успеха

### Технические метрики:

| Метрика | Цель | Текущее |
|---------|------|---------|
| Coverage тестами | ≥ 50% | ~28% |
| RTSP latency | <200ms | TBD |
| HLS latency | <500ms | TBD |
| Reconnect time | <5s | TBD |
| Memory stability | No leaks 24h+ | TBD |
| Camera connection rate | >99% | TBD |

### Бизнес метрики:

- [x] Все must-have сценарии работают
- [ ] Production-ready для 5+ одновременных потоков
- [ ] Security MVP критерии выполнены
- [ ] Документация полная и актуальная

---

## 🔄 Процесс управления

### Еженедельные активности:

1. **Monday:** Planning session, сплит задач на неделю
2. **Daily:** Standup, проверка прогресса
3. **Friday:** Review, демонстрация, ретроспектива
4. **End of Week:** Обновление статус-документов

### Точки контроля:

- **End of Week 1:** W1-0, W1-1, W1-2, W1-3 (50% видео)
- **End of Week 2:** W2-1, W2-2, W2-3, W2-4 (video stable, web ready)
- **End of Week 3:** W3-1, W3-2, W3-3, W3-4 (security, platforms ready)
- **End of Week 4:** W4-1, W4-2, W4-3 (testing, GO/NO-GO)

### Риск-менеджмент:

| Риск | Вероятность | Влияние | Митигация |
|------|-------------|---------|-----------|
| FFmpeg API проблемы | Средний | Высокое | Фолбек на JVCV, упрощённый код |
| Нет реальных камер | Низкий | Высокое | Эмуляторы, публичные потоки |
| Memory leaks | Средний | Высокое | Profiling на каждой итерации |
| Платформенные баги | Высокий | Среднее | Параллельная разработка |

---

## 📝 История изменений

| Версия | Дата | Автор | Изменения |
|--------|------|-------|-----------|
| 1.0 | 2026-05-27 | AI Assistant | Первоначальная версия |

---

**Следующий шаг:** Начать с задачи **W1-0** (фиксация scope) → **W1-2** (RTSP аудио) параллельно с **W1-1** (PostgreSQL).

**Ответственный:** Команда разработки  
**Обновление статуса:** Еженедельно, по пятницам
