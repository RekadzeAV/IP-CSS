# Phase 3 - Анализ статуса и план задач

**Дата:** 11 June 2026  
**Версия:** Alfa-0.1.2-beta  
**Статус:** 🟡 **READY TO START**

---

## 📊 Текущее состояние проекта

### Общий прогресс

| Фаза | Статус | Прогресс |
|------|--------|----------|
| Phase 1 MVP | ✅ COMPLETE | 100% |
| Phase 2 MVP | ✅ COMPLETE | 100% |
| **Phase 3** | 🟡 **READY** | **0%** |

**Общий прогресс проекта:** 85% → (цель Phase 3: 95%)

---

## 🎯 Анализ текущего состояния vs План Phase 3

### 1. Comparison: Текущий статус vs План Phase 3

| Компонент | Текущий статус | План Phase 3 | Gap |
|-----------|----------------|--------------|-----|
| **Desktop Live Video** | ⚠️ 30% (UI начат) | ✅ Стабильность live-view | 70% |
| **Desktop System Integration** | ❌ 0% | ✅ Tray/Autostart/Hotkeys | 100% |
| **Desktop ARM Parity** | ❌ 0% | ✅ ARM x86_64 parity | 100% |
| **NAS Build Baseline** | 🟡 50% (скрипты есть) | ✅ Production-ready пакеты | 50% |
| **NAS QNAP/Synology** | 🟡 80% (сборка PASS) | ✅ Install/upgrade/rollback | 20% |
| **Face Recognition** | ⚠️ 5% (структура) | ✅ End-to-end pipeline | 95% |
| **Analytics Reports** | 🟡 40% (экспорт CSV) | ✅ CSV/PDF генерация | 60% |
| **ANPR Hardening** | ⚠️ 5% (структура) | ✅ Production hardening | 95% |
| **QA Automation** | 🟡 55% (базовые тесты) | ✅ Smoke + Regression | 45% |

### 2. Legacy Phase 3 от May 2026

**Из отчетов Phase 3 (May 2026):**
- ✅ NAS сборка: 80% (15/15 PASS)
- ✅ Desktop compile/tests: 70%
- ✅ Analytics baseline: 60%
- ✅ Chained execution: 50%

**Общий прогресс May 2026:** 65%

**Проблема:** Отчеты May 2026 показывают автоматизированную часть, но field validation не выполнена.

### 3. Gap Analysis

| Проблема | Причина | Решение |
|----------|---------|---------|
| Отчеты May 2026 не актуальны | Phase 2 RTSP был незавершен | Переосмыслить Phase 3 после Phase 2 MVP |
| Field validation не выполнена | Нет реального оборудования | Опционально для CONDITIONAL GO |
| Desktop UI только 30% | Фокус на Phase 2 RTSP | Приоритизировать Desktop в Phase 3 |
| Face/Analytics только 5% | Базовая структура | Реализовать end-to-end pipeline |

---

## 🎯 Обновленный план Phase 3

### Переосмысление Phase 3 после Phase 2 MVP

**Phase 2 MVP COMPLETE** (RTSP Client) меняет приоритеты:

**До Phase 2:**
- RTSP client был критическим блокером (15% готов)

**После Phase 2:**
- RTSP client завершен (90% готов)
- Можно приступать к Desktop/UI и Analytics

### Новый фокус Phase 3

| Приоритет | Epic | Обоснование |
|-----------|------|-------------|
| **P0** | Desktop Production Readiness | Live video теперь работает, нужно UI |
| **P1** | NAS Production Ready | Field validation для релиза |
| **P2** | Advanced Analytics | Face + Reports для MVP |
| **P3** | QA/Release Hardening | Сквозная подготовка к релизу |

---

## 📋 Список задач Phase 3

### EPIC-3.2: Desktop Production Readiness (P0 - Критический путь)

**Цель:** Desktop приложение готово к production use с стабильным live-view.

#### Story 3.2.1: Live Video RTSP/HLS Стабильность (8-12 недель)

| Task | Статус | Оценка | Приоритет |
|------|--------|--------|-----------|
| T-3.2.1.1: Профиль-матрица камер | ⚪ Не начато | 2d | High |
| T-3.2.1.2: Устойчивый reconnect/backoff | ⚪ Не начато | 5d | Critical |
| T-3.2.1.3: Fallback RTSP → HLS | ⚪ Не начато | 3d | Medium |
| T-3.2.1.4: Telemetry (startup time, rebuffer) | ⚪ Не начато | 3d | Medium |
| T-3.2.1.5: Soak-тесты 2h/камера | ⚪ Не начато | 3d | High |

**Acceptance Criteria:**
- 6+ камер проходят длительный прогон без crash
- Неуспешное подключение - recoverable warning
- p95 старта потока соответствует SLO

#### Story 3.2.2: Записи и События E2E UX (5-7 недель)

| Task | Статус | Оценка | Приоритет |
|------|--------|--------|-----------|
| T-3.2.2.1: Фильтры/сортировка/пагинация | 🟡 В процессе | 3d | High |
| T-3.2.2.2: EventTimeline → позиция воспроизведения | ⚪ Не начато | 3d | High |
| T-3.2.2.3: Экспорт записи/фрагмента | ⚪ Не начато | 2d | Medium |
| T-3.2.2.4: Пустые/ошибочные состояния UI | ⚪ Не начато | 2d | Medium |

**Acceptance Criteria:**
- Пользователь выбирает событие → открывает фрагмент записи
- Фильтрация по камере/периоду работает
- Ошибки сети не приводят к зависанию

#### Story 3.2.3: System Integration (tray/autostart/hotkeys) (3-4 недели)

| Task | Статус | Оценка | Приоритет |
|------|--------|--------|-----------|
| T-3.2.3.1: System tray (open, pause/resume, quit) | ⚪ Не начато | 3d | High |
| T-3.2.3.2: Автозапуск на целевых ОС | ⚪ Не начато | 2d | Medium |
| T-3.2.3.3: Базовые hotkeys | ⚪ Не начато | 2d | Low |
| T-3.2.3.4: Системные уведомления | ⚪ Не начато | 2d | Medium |

**Acceptance Criteria:**
- Tray работает без потери состояния
- Автозапуск можно включить/выключить
- Hotkeys не конфликтуют с системными

#### Story 3.2.4: ARM Parity (2-3 недели)

| Task | Статус | Оценка | Приоритет |
|------|--------|--------|-----------|
| T-3.2.4.1: Чек-лист parity | ⚪ Не начато | 1d | Medium |
| T-3.2.4.2: Исправление платформенных расхождений | ⚪ Не начато | 5d | High |
| T-3.2.4.3: Smoke-прогоны на ARM | ⚪ Не начато | 2d | High |

**Acceptance Criteria:**
- Все сценарии parity-чеклиста на ARM
- Критические функции не отличаются от x86_64

---

### EPIC-3.1: NAS Platforms & Packaging (P1)

**Цель:** NAS пакеты production-ready для минимум 2 платформ.

#### Story 3.1.1: Build Baseline & CI Matrix (уже частично выполнено)

| Task | Статус | Оценка | Приоритет |
|------|--------|--------|-----------|
| T-3.1.1.1: Унифицировать структуру артефактов | 🟡 В процессе | 2d | High |
| T-3.1.1.2: Сборка x86_64/ARM через единые скрипты | 🟡 В процессе | 3d | High |
| T-3.1.1.3: CI matrix job (build + checksum) | 🟡 В процессе | 2d | High |
| T-3.1.1.4: Версионирование и changelog | ⚪ Не начато | 1d | Medium |

**Статус:** May 2026 отчет показывает 80% выполнено. Требуется доведение до 100%.

#### Story 3.1.2: QNAP QPKG Production-Ready (2-3 недели)

| Task | Статус | Оценка | Приоритет |
|------|--------|--------|-----------|
| T-3.1.2.1: Финализировать QPKG.INFO и lifecycle-скрипты | 🟡 В процессе | 2d | High |
| T-3.1.2.2: Pre-upgrade/post-upgrade проверка | ⚪ Не начато | 2d | High |
| T-3.1.2.3: Восстановление после reboot | ⚪ Не начато | 2d | High |
| T-3.1.2.4: Troubleshooting для распространенных ошибок | ⚪ Не начато | 1d | Medium |

**Acceptance Criteria:**
- install/upgrade/uninstall без ручных действий
- После reboot сервис автоматически поднимается

#### Story 3.1.3: Synology SPK Production-Ready (2-3 недели)

| Task | Статус | Оценка | Приоритет |
|------|--------|--------|-----------|
| T-3.1.3.1: Финализировать INFO, package layout | 🟡 В процессе | 2d | High |
| T-3.1.3.2: Миграция конфигурации при upgrade | ⚪ Не начато | 2d | High |
| T-3.1.3.3: Валидация x86_64 и ARM | 🟡 В процессе | 2d | High |
| T-3.1.3.4: Rollback-процедура | ⚪ Не начато | 2d | High |

**Acceptance Criteria:**
- Пакет устанавливается/обновляется штатно
- Rollback документирован и проверен

#### Story 3.1.6: NAS Документация и Оптимизация (1-2 недели)

| Task | Статус | Оценка | Приоритет |
|------|--------|--------|-----------|
| T-3.1.6.1: Install/upgrade/rollback runbook | ⚪ Не начато | 2d | High |
| T-3.1.6.2: Known issues и troubleshooting matrix | ⚪ Не начато | 2d | Medium |
| T-3.1.6.3: Оптимизация CPU/RAM под слабые NAS | ⚪ Не начато | 3d | Medium |

---

### EPIC-3.3: Advanced Analytics (P2)

**Цель:** Face Recognition + Reports end-to-end.

#### Story 3.3.2: Face Recognition End-to-End (4-6 недель)

| Task | Статус | Оценка | Приоритет |
|------|--------|--------|-----------|
| T-3.3.2.1: Миграция БД для face_gallery | ⚪ Не начато | 2d | High |
| T-3.3.2.2: FaceRepositoryImplSqlDelight | ⚪ Не начато | 3d | High |
| T-3.3.2.3: API CRUD для базы лиц | ⚪ Не начато | 2d | High |
| T-3.3.2.4: Поиск по embedding и ранжирование | ⚪ Не начато | 5d | Critical |
| T-3.3.2.5: Интеграция в event pipeline | ⚪ Не начато | 3d | High |

**Acceptance Criteria:**
- Добавление/обновление/удаление лиц работает
- Поиск возвращает кандидатов с confidence score
- Face-recognition события в общем потоке

#### Story 3.3.3: Analytics Reports (3-4 недели)

| Task | Статус | Оценка | Приоритет |
|------|--------|--------|-----------|
| T-3.3.3.1: ReportServiceImpl (агрегация) | 🟡 В процессе | 3d | High |
| T-3.3.3.2: Экспорт CSV | 🟡 В процессе | 1d | High |
| T-3.3.3.3: Экспорт PDF | ⚪ Не начато | 3d | Medium |
| T-3.3.3.4: Шаблоны отчетов | ⚪ Не начато | 2d | Medium |
| T-3.3.3.5: Валидация корректности агрегатов | ⚪ Не начато | 2d | High |

**Acceptance Criteria:**
- Отчеты генерируются без ошибок
- CSV/PDF открываются с корректными данными
- Фильтры периода/камеры консистентны

#### Story 3.3.1: ANPR Hardening (2-3 недели)

| Task | Статус | Оценка | Приоритет |
|------|--------|--------|-----------|
| T-3.3.1.1: Проверка качества OCR | ⚪ Не начато | 3d | High |
| T-3.3.1.2: Нормализация форматов номеров | ⚪ Не начато | 2d | Medium |
| T-3.3.1.3: Тесты регрессии ложных срабатываний | ⚪ Не начато | 2d | High |

---

### EPIC-3.4: QA/Release Hardening (P3 - Сквозной)

**Цель:** Сквозная подготовка к релизу.

#### Story 3.4.1: Test Strategy & Automation (2-3 недели)

| Task | Статус | Оценка | Приоритет |
|------|--------|--------|-----------|
| T-3.4.1.1: Smoke-suite на неделю | 🟡 В процессе | 2d | High |
| T-3.4.1.2: Интеграционные тесты Desktop/NAS/Analytics | ⚪ Не начато | 3d | High |
| T-3.4.1.3: Nightly regression jobs | ⚪ Не начато | 2d | Medium |

#### Story 3.4.2: Release Readiness (1-2 недели)

| Task | Статус | Оценка | Приоритет |
|------|--------|--------|-----------|
| T-3.4.2.1: Чек-лист go/no-go для Phase 3 | ⚪ Не начато | 1d | Critical |
| T-3.4.2.2: Release notes и known limitations | ⚪ Не начато | 1d | High |
| T-3.4.2.3: Rollback rehearsal | ⚪ Не начато | 2d | High |

---

## 📊 Сводная таблица задач Phase 3

### По Epic

| Epic | Статус | Задач | Оценка | Приоритет |
|------|--------|-------|--------|-----------|
| EPIC-3.2 Desktop | 🟡 READY | 16 задач | 55-75d | P0 Critical |
| EPIC-3.1 NAS | 🟡 80% | 13 задач | 25-35d | P1 High |
| EPIC-3.3 Analytics | ⚪ 5% | 13 задач | 30-40d | P2 High |
| EPIC-3.4 QA/Release | 🟡 55% | 6 задач | 10-15d | P3 Medium |

**Итого Phase 3:** 48 задач, ~120-165 дней (4-5.5 месяцев)

### По Приоритету

| Приоритет | Задач | Оценка |
|-----------|-------|--------|
| Critical | 5 | ~15d |
| High | 25 | ~70d |
| Medium | 15 | ~35d |
| Low | 3 | ~5d |

### По Статусу

| Статус | Задач |
|--------|-------|
| ✅ Завершено | 0 |
| 🟡 В процессе | 8 |
| ⚪ Не начато | 40 |

---

## 🎯 Спринтовый план Phase 3

### Sprint 1-2 (Недели 1-4) - Desktop Live Video Baseline

**Фокус:**
- T-3.2.1.1: Профиль-матрица камер
- T-3.2.1.2: Reconnect/backoff
- T-3.2.1.4: Telemetry

**Выход:** Stable live-view RC1

### Sprint 3-4 (Недели 5-8) - Desktop UX + NAS QNAP/Synology

**Фокус:**
- T-3.2.2.x: Записи и события E2E
- T-3.2.3.x: System Integration
- T-3.1.2.x: QNAP QPKG
- T-3.1.3.x: Synology SPK

**Выход:** Desktop functional e2e + QNAP/Synology v1

### Sprint 5-6 (Недели 9-12) - Analytics + ARM Parity

**Фокус:**
- T-3.3.2.x: Face Recognition
- T-3.3.3.x: Reports
- T-3.2.4.x: ARM Parity

**Выход:** Face + Reports end-to-end RC

### Sprint 7-8 (Недели 13-16) - QA Hardening + Release

**Фокус:**
- T-3.4.1.x: Test Strategy
- T-3.4.2.x: Release Readiness
- T-3.1.6.x: NAS Documentation

**Выход:** Phase 3 closeout package

---

## ⚠️ Риски и Контрмеры

| Риск | Вероятность | Влияние | Контрмера |
|------|-------------|---------|-----------|
| Нестабильность RTSP на части камер | Medium | High | Камерная матрица + fallback |
| Сложности lifecycle на NAS | Medium | High | Стандартизованные скрипты |
| Просадка производительности face-search | Medium | Medium | Индексация/батчинг |
| Неконсистентные отчеты | Low | Medium | Контрольные датасеты |

---

## ✅ Definition of Done - Phase 3

**Desktop:**
- ✅ Live-view стабильно (6+ камер, 2h soak)
- ✅ Events/Recordings E2E UX
- ✅ System Integration (tray/autostart)
- ✅ ARM parity

**NAS:**
- ✅ Минимум 2 платформы production-ready (QNAP, Synology)
- ✅ Install/upgrade/uninstall/rollback проверены

**Analytics:**
- ✅ Face Recognition end-to-end
- ✅ Reports CSV/PDF работают
- ✅ ANPR hardening

**QA/Release:**
- ✅ Smoke + Integration + Nightly regression
- ✅ Release checklist подписан
- ✅ Rollback доказан

---

## 🚀 Следующие шаги

### Immediate (Сprint 1)

1. **Создать Jira/Linear задачи** из списка выше
2. **Назначить ответственных** на каждый Epic
3. **Запустить Sprint 1** с фокусом на Desktop Live Video

### Recommended Setup

1. **Поток A (Desktop):** 2-3 разработчика
2. **Поток B (NAS/Analytics):** 2 разработчика
3. **QA:** 1 разработчик (сквозной)

### Timeline

- **Start:** Сразу после утверждения плана
- **Duration:** 16 недель (4 месяца)
- **Target Release:** Q3 2026

---

**Отчет составлен:** 11 June 2026  
**Готовность к старту:** ✅ **YES**  
**Рекомендация:** Приступать к Sprint 1 немедленно
