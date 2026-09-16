# Фаза 3 — Detailed Backlog (Epic -> Story -> Task -> Acceptance)

**Дата обновления:** 27 March 2026  
**Источник статуса:** `docs/status/PROJECT_STATUS_PHASES.md`, `docs/archive/2026-03-30/TASKS_WITH_DESCRIPTIONS.md` (архив детальных таблиц), `docs/planning/BLOCKS_8_9_ANALYTICS_AND_FEATURES.md`, `docs/TODO.md`  
**Горизонт планирования:** 12-16 недель  
**Цель:** довести Фазу 3 до состояния release-ready (Desktop + NAS + расширенная аналитика).

---

## 1) Принципы исполнения

- Приоритет по критическому пути: **Desktop live video -> NAS installability -> Face/Reports**.
- Каждый Epic закрывается только при наличии:
  - рабочих сценариев end-to-end;
  - smoke/integration тестов;
  - документации по эксплуатации.
- Рекомендуемый режим: 2 параллельных потока работ:
  - **Поток A:** Desktop/Video.
  - **Поток B:** NAS/Analytics.

---

## 2) Карта Epic'ов Фазы 3

| Epic ID | Название | Приоритет | Оценка | Зависимости |
|---|---|---|---|---|
| EPIC-3.1 | NAS Platforms & Packaging | High | 13-18 недель | Build baseline, CI matrix |
| EPIC-3.2 | Desktop Production Readiness | Critical | 7-11 недель | RTSP/HLS stability |
| EPIC-3.3 | Advanced Analytics (Face + Reports) | High | 5-8 недель | Face DB migration, data aggregation |
| EPIC-3.4 | QA/Release Hardening | Critical | 3-4 недели (сквозной) | Все epics 3.1-3.3 |

---

## 3) EPIC-3.2 — Desktop Production Readiness (критический путь)

### Story 3.2.1 — Live Video RTSP/HLS стабильность
**Описание:** обеспечить надежный просмотр live-потоков на Desktop с recoverable деградацией.

**Tasks:**
- T-3.2.1.1: Уточнить профиль-матрицу камер (codec/transport/auth).
- T-3.2.1.2: Реализовать устойчивый reconnect/backoff для RTSP.
- T-3.2.1.3: Добавить fallback RTSP -> HLS при деградации.
- T-3.2.1.4: Ввести telemetry (startup time, rebuffer count, stream drops).
- T-3.2.1.5: Прогон soak-тестов 2h/камера на целевом наборе.

**Acceptance Criteria:**
- 6+ камер проходят длительный прогон без crash приложения.
- Неуспешное подключение отображается как recoverable warning.
- p95 старта потока и rebuffer-rate соответствуют целевым SLO команды.

---

### Story 3.2.2 — Записи и события (end-to-end UX)
**Описание:** завершить сценарий "событие -> запись -> воспроизведение фрагмента".

**Tasks:**
- T-3.2.2.1: Доработать фильтры/сортировку/пагинацию экранов записей.
- T-3.2.2.2: Привязать EventTimeline к позиции воспроизведения.
- T-3.2.2.3: Добавить экспорт записи/фрагмента (минимум базовый формат).
- T-3.2.2.4: Добавить пустые/ошибочные состояния UI.

**Acceptance Criteria:**
- Пользователь выбирает событие и открывает соответствующий фрагмент записи.
- Фильтрация по камере/периоду работает корректно.
- Ошибки сети/данных не приводят к зависанию интерфейса.

---

### Story 3.2.3 — System Integration (tray/autostart/hotkeys)
**Описание:** довести системную интеграцию Desktop до рабочего уровня.

**Tasks:**
- T-3.2.3.1: Реализовать базовый system tray (open, pause/resume, quit).
- T-3.2.3.2: Автозапуск приложения на целевых ОС.
- T-3.2.3.3: Базовые hotkeys для ключевых действий.
- T-3.2.3.4: Системные уведомления по критическим событиям.

**Acceptance Criteria:**
- Приложение корректно работает через tray без потери состояния.
- Автозапуск можно включить/выключить в настройках.
- Hotkeys не конфликтуют с системными сочетаниями (по целевым ОС).

---

### Story 3.2.4 — ARM parity
**Описание:** обеспечить функциональный паритет Desktop ARM с x86_64.

**Tasks:**
- T-3.2.4.1: Чек-лист parity (live view, events, recordings, settings).
- T-3.2.4.2: Исправить платформенные расхождения поведения/перформанса.
- T-3.2.4.3: Отдельные smoke-прогоны на ARM.

**Acceptance Criteria:**
- Все сценарии parity-чеклиста выполняются на ARM.
- Критические функции не отличаются по поведению от x86_64.

---

## 4) EPIC-3.1 — NAS Platforms & Packaging

### Story 3.1.1 — Build baseline и CI matrix
**Описание:** единая инфраструктура сборки и публикации артефактов NAS.

**Tasks:**
- T-3.1.1.1: Унифицировать структуру артефактов и naming convention.
- T-3.1.1.2: Сборка для x86_64/ARM через единые скрипты.
- T-3.1.1.3: CI matrix job (build + checksum + artifacts).
- T-3.1.1.4: Версионирование и changelog для пакетов.

**Acceptance Criteria:**
- Сборка воспроизводима локально и в CI.
- Артефакты содержат версии, checksum и install-инструкции.

---

### Story 3.1.2 — QNAP QPKG production-ready
**Описание:** довести QPKG-пакет до готовности установки в App Center.

**Tasks:**
- T-3.1.2.1: Финализировать `QPKG.INFO` и lifecycle-скрипты.
- T-3.1.2.2: Реализовать pre-upgrade/post-upgrade проверку конфигурации.
- T-3.1.2.3: Проверить корректное восстановление после reboot.
- T-3.1.2.4: Подготовить troubleshooting для распространенных ошибок.

**Acceptance Criteria:**
- install/upgrade/uninstall проходят без ручных действий.
- После reboot сервис автоматически поднимается и проходит health-check.

---

### Story 3.1.3 — Synology SPK production-ready
**Описание:** завершить SPK-пакет и совместимость с Package Center.

**Tasks:**
- T-3.1.3.1: Финализировать `INFO`, package layout, pre/post scripts.
- T-3.1.3.2: Реализовать миграцию конфигурации при upgrade.
- T-3.1.3.3: Валидация на x86_64 и ARM целевых профилях.
- T-3.1.3.4: Сформировать rollback-процедуру.

**Acceptance Criteria:**
- Пакет устанавливается и обновляется через штатный процесс.
- Rollback документирован и проверен на тестовом стенде.

---

### Story 3.1.4 — Asustor APK и 3.1.5 TrueNAS (phase 3.5 при нехватке capacity)
**Описание:** расширение платформ после стабилизации QNAP/Synology.

**Tasks:**
- T-3.1.4.1: APK packaging flow и валидация lifecycle.
- T-3.1.5.1: TrueNAS SCALE container path + документация.
- T-3.1.5.2: TrueNAS CORE (если подтвержден целевой спрос).

**Acceptance Criteria:**
- Минимум Asustor как beta-supported.
- TrueNAS имеет документированный путь деплоя (если включено в фазу).

---

### Story 3.1.6 — NAS документация и оптимизация
**Описание:** эксплуатационная готовность NAS-решений.

**Tasks:**
- T-3.1.6.1: Install/upgrade/rollback runbook для каждой платформы.
- T-3.1.6.2: Known issues и troubleshooting matrix.
- T-3.1.6.3: Оптимизация CPU/RAM профилей под слабые NAS.

**Acceptance Criteria:**
- Оператор может выполнить полный lifecycle по инструкции.
- Есть измеримые лимиты ресурсов и рекомендации по sizing.

---

## 5) EPIC-3.3 — Advanced Analytics (Face + Reports)

### Story 3.3.2 — Face Recognition end-to-end
**Описание:** завершить работу с лицами: хранение, поиск, интеграция в события.

**Tasks:**
- T-3.3.2.1: Миграция БД для `face_gallery`.
- T-3.3.2.2: Реализация `FaceRepositoryImplSqlDelight`.
- T-3.3.2.3: API CRUD для базы лиц.
- T-3.3.2.4: Поиск по embedding и ранжирование кандидатов.
- T-3.3.2.5: Интеграция результатов в event pipeline.

**Acceptance Criteria:**
- Добавление/обновление/удаление лиц работает стабильно.
- Поиск по лицу возвращает кандидатов с confidence score.
- События face-recognition доступны в общем потоке событий.

---

### Story 3.3.3 — Analytics Reports
**Описание:** реализовать отчеты по аналитике и экспорт.

**Tasks:**
- T-3.3.3.1: Реализовать `ReportServiceImpl` (агрегация из репозиториев).
- T-3.3.3.2: Экспорт CSV (обязательный).
- T-3.3.3.3: Экспорт PDF (целевая функциональность).
- T-3.3.3.4: Шаблоны отчетов (по камере, периоду, типу событий).
- T-3.3.3.5: Валидация корректности агрегатов на контрольном датасете.

**Acceptance Criteria:**
- Отчеты генерируются на реальных данных без ошибок.
- CSV/PDF открываются и содержат корректные агрегации.
- Фильтры периода/камеры работают консистентно.

---

### Story 3.3.1 — ANPR hardening (дозавершение)
**Описание:** ANPR уже реализован частично/в основном; нужен production hardening.

**Tasks:**
- T-3.3.1.1: Проверка качества OCR на контрольных наборах.
- T-3.3.1.2: Нормализация форматов номеров по регионам.
- T-3.3.1.3: Добавить тесты регрессии для ложных срабатываний.

**Acceptance Criteria:**
- Качество распознавания соответствует внутреннему baseline.
- Ложные срабатывания контролируются и мониторятся.

---

## 6) EPIC-3.4 — QA / Release Hardening (сквозной)

### Story 3.4.1 — Test Strategy & Automation
**Tasks:**
- T-3.4.1.1: Определить обязательный smoke-suite на каждую неделю.
- T-3.4.1.2: Добавить интеграционные тесты Desktop/NAS/Analytics.
- T-3.4.1.3: Наладить nightly regression jobs.

**Acceptance Criteria:**
- Все критические пользовательские флоу покрыты smoke.
- Регрессия запускается автоматически и дает воспроизводимые отчеты.

---

### Story 3.4.2 — Release Readiness
**Tasks:**
- T-3.4.2.1: Чек-лист go/no-go для Фазы 3.
- T-3.4.2.2: Release notes и known limitations.
- T-3.4.2.3: Rollback rehearsal для NAS и Desktop критичных сценариев.

**Acceptance Criteria:**
- Формальный go/no-go подписан ответственными.
- Rollback доказан практическим прогоном.

---

## 7) Спринтовый план (предложение)

| Спринт | Фокус | Выход |
|---|---|---|
| Sprint 1-2 (Недели 1-4) | 3.2.1 + 3.1.1 + smoke-baseline | Stable live-view RC + reproducible NAS build |
| Sprint 3-4 (Недели 5-8) | 3.1.2/3.1.3 + 3.2.2/3.2.3 + ARM parity start | QNAP/Synology v1 + Desktop functional e2e |
| Sprint 5-6 (Недели 9-12) | 3.3.2 + 3.3.3 + QA hardening | Face + Reports end-to-end RC |
| Sprint 7-8 (Недели 13-16) | 3.1.4/3.1.5 (по capacity) + release docs | Phase 3 closeout package |

---

## 8) Риски и контрмеры

| Риск | Вероятность | Влияние | Контрмера |
|---|---|---|---|
| Нестабильность RTSP на части камер | High | High | Камерная матрица + fallback + soak |
| Сложности lifecycle на NAS | Medium | High | Стандартизованные скрипты + upgrade/rollback tests |
| Просадка производительности face-search | Medium | Medium | Индексация/батчинг + лимиты на embedding pipeline |
| Неконсистентные отчеты | Medium | High | Контрольные датасеты и сверка агрегатов |

---

## 9) Definition of Done (Фаза 3)

- Desktop: live-view, events, recordings, settings/system integration, ARM parity.
- NAS: минимум 2 production-ready платформы с install/upgrade/uninstall/rollback.
- Analytics: face-search end-to-end, reports CSV/PDF, ANPR hardening.
- QA/Release: smoke + integration + nightly regression + runbook + release checklist.

---

## 10) Готовность к импорту в трекер

Для быстрой загрузки в Jira/Linear рекомендуется использовать идентификаторы:
- Epic: `EPIC-3.1` ... `EPIC-3.4`
- Story: `S-3.x.y`
- Task: `T-3.x.y.z`

Этот файл можно напрямую разбивать на:
- 4 Epic,
- 12-16 Story,
- 45-60 Task (в зависимости от детализации QA/NAS).

