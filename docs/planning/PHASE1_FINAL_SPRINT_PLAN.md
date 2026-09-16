# План выполнения Фазы 1 — Итоговый спринт

**Дата:** 27 May 2026  
**Цель:** Доведение Фазы 1 до 100% (Production Ready)  
**Срок:** 4 недели  
**Команда:** 1-2 разработчика

---

## 📊 Текущее состояние

| Показатель | Значение |
|------------|----------|
| Прогресс Фазы 1 | 85% |
| Критические блокеры | 0/8 ✅ |
| Высокий приоритет | 6 задач |
| Средний приоритет | 10 задач |
| Низкий приоритет | 3 задачи |
| Test coverage | ~28% |
| TODO/FIXME | ~75 |

---

## 🗓️ Спринт-план (4 недели)

### Неделя 1: Long-Run Тестирование и Стабильность

**Цель:** Подтвердить runtime стабильность всех компонентов

| День | Задача | Часов | Результат |
|------|--------|-------|-----------|
| Пн | RTSP long-run тесты (24ч) | 8 | Базовая стабильность подтверждена |
| Вт | Анализ RTSP результатов + исправления | 8 | Issues resolved |
| Ср | HLS long-run тесты (48ч старт) | 8 | Тест запущен |
| Чт | Security field validation (часть 1) | 8 | Certificate pinning проверен |
| Пт | PostgreSQL staging cutover | 8 | Database ready |

**KPI недели:**
- [ ] RTSP 24ч тест PASS
- [ ] HLS 24ч тест PASS
- [ ] Security validation PASS (частично)
- [ ] PostgreSQL migration verified

---

### Неделя 2: Валидация и Исправления

**Цель:** Завершить валидацию и исправить выявленные проблемы

| День | Задача | Часов | Результат |
|------|--------|-------|-----------|
| Пн | HLS long-run (продолжение 48ч) | 8 | Тест завершён |
| Вт | Анализ HLS + исправления | 8 | Memory leaks fixed |
| Ср | Security field validation (часть 2) | 8 | HTTPS/JWT проверены |
| Чт | Screenshot pipeline validation | 8 | Working on real streams |
| Пт | RTSP реальная камера (5 устройств) | 8 | Compatibility matrix |

**KPI недели:**
- [ ] HLS 48ч тест PASS
- [ ] Security validation 100% PASS
- [ ] Screenshot pipeline working
- [ ] 5+ камер протестированы

---

### Неделя 3: Платформы и Тестирование

**Цель:** Улучшить platform stability и увеличить coverage тестов

| День | Задача | Часов | Результат |
|------|--------|-------|-----------|
| Пн | Android video integration | 8 | RecordingService improved |
| Вт | Desktop video player optimization | 8 | Multi-camera stable |
| Ср | Unit тесты Use Cases (10 шт) | 8 | Coverage +5% |
| Чт | Integration тесты API (8 шт) | 8 | Coverage +8% |
| Пт | E2E критические сценарии (3 шт) | 8 | E2E baseline |

**KPI недели:**
- [ ] Android video stable
- [ ] Desktop video optimized
- [ ] Test coverage 40%+
- [ ] 3 E2E сценария PASS

---

### Неделя 4: Финальная Приёмка

**Цель:** Пройти acceptance и подготовить релиз

| День | Задача | Часов | Результат |
|------|--------|-------|-----------|
| Пн | E2E сценарии (дополнение 5 шт) | 8 | 8 E2E сценариев |
| Вт | TODO/FIXME рефакторинг | 8 | <50 TODOs |
| Ср | `mvpAutomatedAcceptance` прогон | 8 | PASS |
| Чт | Go/No-Go документация | 8 | GO decision |
| Пт | Release preparation + sign-off | 8 | Фаза 1 = 100% |

**KPI недели:**
- [ ] 8 E2E сценариев PASS
- [ ] TODO/FIXME <50
- [ ] `mvpAutomatedAcceptance` PASS
- [ ] Go/No-Go = GO

---

## 🎯 Приоритизация задач

### 🔴 Критический приоритет (Недели 1-2)

| ID | Задача | Статус | Приоритет | Оценка |
|----|--------|--------|-----------|--------|
| C1 | RTSP long-run тесты (24ч) | ⏳ | 🔴 | 2 дня |
| C2 | HLS long-run тесты (48ч) | ⏳ | 🔴 | 3 дня |
| C3 | Security field validation | ⏳ | 🔴 | 2 дня |
| C4 | PostgreSQL staging cutover | ⏳ | 🔴 | 1 день |
| C5 | Screenshot pipeline validation | ⏳ | 🔴 | 1 день |
| C6 | RTSP реальная камера (5+) | ⏳ | 🔴 | 1 день |

**Итого критических:** 10 человеко-дней

---

### 🟠 Высокий приоритет (Недели 2-3)

| ID | Задача | Статус | Приоритет | Оценка |
|----|--------|--------|-----------|--------|
| H1 | Android RecordingService улучшение | ⏳ | 🟠 | 2 дня |
| H2 | Desktop VideoPlayer оптимизация | ⏳ | 🟠 | 2 дня |
| H3 | Unit тесты Use Cases (10 шт) | ⏳ | 🟠 | 2 дня |
| H4 | Integration тесты API (8 шт) | ⏳ | 🟠 | 2 дня |
| H5 | Лицензирование — онлайн активация | ⏳ | 🟠 | 3 дня |

**Итого высоких:** 11 человеко-дней

---

### 🟡 Средний приоритет (Недели 3-4)

| ID | Задача | Статус | Приоритет | Оценка |
|----|--------|--------|-----------|--------|
| M1 | E2E тесты (8 сценариев) | ⏳ | 🟡 | 3 дня |
| M2 | TODO/FIXME рефакторинг | ⏳ | 🟡 | 2 дня |
| M3 | AnalyticsRuleService улучшения | ⏳ | 🟡 | 2 дня |
| M4 | Native analytics (YOLO, OCR) | ⏳ | 🟡 | 3 дня |
| M5 | NotificationManager платформы | ⏳ | 🟡 | 2 дня |

**Итого средних:** 12 человеко-дней

---

### 🟢 Низкий приоритет (Неделя 4, опционально)

| ID | Задача | Статус | Приоритет | Оценка |
|----|--------|--------|-----------|--------|
| L1 | Документация обновление | ⏳ | 🟢 | 1 день |
| L2 | ONVIF/UPnP улучшения | ⏳ | 🟢 | 1 день |
| L3 | Performance benchmarks | ⏳ | 🟢 | 1 день |

**Итого низких:** 3 человеко-дня

---

## 📋 Детальные задачи по дням

### День 1: RTSP Long-Run Старт

**Задачи:**
1. Настроить тестовую камеру (RTSP URL, credentials)
2. Запустить `./gradlew :shared:desktopTest --tests RtspLongRunStabilityTest`
3. Мониторить ресурсы (CPU, Memory, Network)
4. Логировать ошибки и reconnect события

**Команды:**
```powershell
# Запуск long-run теста на 24 часа
.\scripts\run-rtsp-long-run-stability-test.ps1 -DurationHours 24 -CameraName "TestCam1"

# Мониторинг в реальном времени
.\scripts\monitor-rtsp-performance.ps1
```

**Deliverables:**
- `docs/reports/RTSP_LONG_RUN_TEST_DAY1.md`
- Метрики производительности
- Список выявленных проблем

---

### День 2: Анализ RTSP + Исправления

**Задачи:**
1. Проанализировать логи long-run теста
2. Выявить memory leaks, reconnect проблемы
3. Исправить критичные issues
4. Запустить короткий smoke тест (2ч) для верификации

**Deliverables:**
- `docs/reports/RTSP_LONG_RUN_TEST_ANALYSIS.md`
- Исправленные файлы
- Smoke тест PASS

---

### День 3: HLS Long-Run Старт

**Задачи:**
1. Настроить FFmpeg окружение
2. Запустить HLS генерацию с RTSP источника
3. Мониторить сегменты, playlist, storage
4. Проверить playback в веб-плеере

**Команды:**
```powershell
# Запуск HLS long-run теста
.\scripts\run-hls-long-run-test.ps1 -DurationHours 48 -RtspUrl "rtsp://..."

# Мониторинг HLS сегментов
.\scripts\monitor-hls-stream.ps1
```

**Deliverables:**
- `docs/reports/HLS_LONG_RUN_TEST_DAY1.md`
- HLS segment logs
- Playback verification

---

### День 4: Security Field Validation (Часть 1)

**Задачи:**
1. Certificate Pinning — Android/Dekstop
2. Проверка HTTPS redirect
3. HSTS headers validation
4. JWT secure storage (Android Keystore)

**Команды:**
```powershell
# Security validation скрипт
.\scripts\security-mvp-readiness-check.ps1 -FullValidation
```

**Deliverables:**
- `docs/reports/SECURITY_FIELD_VALIDATION_PART1.md`
- Certificate pinning reports
- HTTPS/HSTS verification

---

### День 5: PostgreSQL Staging Cutover

**Задачи:**
1. Подготовить staging окружение
2. Запустить миграции Flyway
3. Перенести тестовые данные
4. Провести smoke тесты

**Команды:**
```powershell
# Запуск миграций
.\scripts\run-postgres-migrations.ps1

# Smoke тесты БД
.\scripts\test-database-migration.ps1
```

**Deliverables:**
- `docs/reports/POSTGRESQL_STAGING_CUTOVER.md`
- Migration logs
- Smoke тест результаты

---

## 📊 Трекинг прогресса

### Еженедельные check-in

| Неделя | Планируемый прогресс | Фактический прогресс | Статус |
|--------|---------------------|---------------------|--------|
| 1 | 25% | — | ⏳ Планируется |
| 2 | 50% | — | ⏳ Планируется |
| 3 | 75% | — | ⏳ Планируется |
| 4 | 100% | — | ⏳ Планируется |

### Ежедневные stand-up

**Формат:**
```
Вчера:
- Задача X выполнена
- Прогресс Y%

Сегодня:
- Задача Z старт

Блокеры:
- Нет / Описание
```

---

## 🚨 Блокеры и риски

### Известные риски

| Риск | Вероятность | Влияние | Митигация |
|------|-------------|---------|-----------|
| Long-run тесты выявят критичные баги | Высокая | Высокое | Заложено 2 дня на исправления |
| Security validation не пройдена | Средняя | Высокое | Заложено 2 дня на доработку |
| Недостаточно тестовых камер | Средняя | Среднее | Использовать эмуляторы |
| PostgreSQL migration issues | Низкая | Высокое | Rollback rehearsal подготовлен |

### Эскалация

При возникновении блокеров:
1. Документировать в `docs/reports/BLOCKERS_LOG.md`
2. Уведомить команду в течение 1 часа
3. Предложить варианты решения

---

## ✅ Definition of Done (Фаза 1)

### Must Have

- [ ] RTSP long-run тест 24ч PASS
- [ ] HLS long-run тест 48ч PASS
- [ ] Security field validation PASS
- [ ] PostgreSQL staging cutover SUCCESS
- [ ] 5+ реальных камер протестированы
- [ ] Test coverage 40%+
- [ ] 8 E2E сценариев PASS
- [ ] TODO/FIXME <50
- [ ] `mvpAutomatedAcceptance` PASS
- [ ] Go/No-Go = GO

### Nice to Have

- [ ] Performance benchmarks документированы
- [ ] Production runbook завершён
- [ ] Known issues список минимален
- [ ] User guide для операторов

---

## 📁 Артефакты спринта

### Создаваемые документы

1. `docs/reports/RTSP_LONG_RUN_TEST_DAY1-7.md` (ежедневные отчёты)
2. `docs/reports/HLS_LONG_RUN_TEST_DAY1-7.md` (ежедневные отчёты)
3. `docs/reports/SECURITY_FIELD_VALIDATION_PART1-2.md`
4. `docs/reports/POSTGRESQL_STAGING_CUTOVER.md`
5. `docs/reports/PHASE1_FINAL_ACCEPTANCE_MATRIX.md`
6. `docs/reports/PHASE1_GO_NO_GO_SUMMARY.md`
7. `docs/reports/RELEASE_NOTES_PHASE1.md`

### Создаваемые скрипты

1. `scripts/run-rtsp-long-run-stability-test.ps1`
2. `scripts/run-hls-long-run-test.ps1`
3. `scripts/monitor-rtsp-performance.ps1`
4. `scripts/monitor-hls-stream.ps1`
5. `scripts/security-mvp-readiness-check.ps1`

---

## 🎯 Критерии успеха

| Метрика | Целевое значение |
|---------|-----------------|
| RTSP стабильность (24ч) | 0 критичных ошибок |
| HLS стабильность (48ч) | 0 критичных ошибок |
| Test coverage | 40%+ |
| E2E сценарии | 8 PASS |
| TODO/FIXME | <50 |
| Security уязвимости | 0 критичных |
| Production readiness | 100% |

---

**Дата создания:** 27 May 2026  
**Автор:** AI Assistant  
**Статус:** 🟡 Ожидает запуска  
**Следующий check-in:** День 1, конец дня
