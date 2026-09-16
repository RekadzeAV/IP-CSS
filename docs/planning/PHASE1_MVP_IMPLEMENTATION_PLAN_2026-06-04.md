# План реализации Фазы 1 MVP — Актуализированный

**Дата:** 2026-06-04  
**Версия проекта:** Alfa-0.1.1  
**Текущий прогресс:** ~75%  
**Цель:** 100% завершение Фазы 1 MVP

---

## 1. Резюме текущего состояния

### Общий статус

| Показатель | Значение |
|------------|----------|
| **Общий прогресс** | **~75%** |
| **Критические блокеры** | 4 |
| **Оценка до 100%** | 2–3 недели при фокусе на MVP |
| **Релизный gate** | NO-GO (до закрытия runtime/field evidence) |

### Что реализовано

✅ **Инфраструктура (1.1)** — 100%  
✅ **Доменный слой (1.2)** — 82% (аналитика вне MVP scope)  
✅ **Слой данных (1.3)** — 98%  
✅ **Сетевой слой (1.4)** — 94% (ONVIF, WebSocket готовы)  
✅ **Веб-интерфейс (1.6)** — 95%  
🟡 **Серверная часть (1.5)** — 88% (PostgreSQL финализация)  
🟡 **Видео и запись (1.8)** — 63% (RTSP native не активирован)  
🟡 **Безопасность (1.9)** — 88% (требует staging evidence)  
⚠️ **Тестирование (1.10)** — 28%  
⚠️ **Платформы (1.7)** — 55% (Android video не завершено)

---

## 2. Критические блокеры (P0)

### Блокер 1: RTSP Native Integration

**Описание:** Нативная C++ библиотека реализована, но FFI не активирован, Kotlin cinterop не сгенерирован.

**Влияние:** Блокирует production-path видео на Desktop/Android/Linux.

**Задачи:**
1. [ ] Компиляция `libvideo_processing` для Desktop (Linux/Windows/macOS)
2. [ ] Генерация Kotlin cinterop биндингов
3. [ ] Активация FFI в `NativeRtspClient.native.kt`
4. [ ] Тестирование подключения к реальной камере
5. [ ] Долгосрочная стабильность (30+ минут soak test)

**Оценка:** 2–3 недели

---

### Блокер 2: HLS Runtime Stability

**Описание:** HLS pipeline работает, но требуется long-run валидация с реконнектом и очисткой.

**Влияние:** Риск деградации при длительном просмотре.

**Задачи:**
1. [ ] Long-run тест (30+ минут) с реальным потоком
2. [ ] Проверка reconnect логики
3. [ ] Валидация cleanup записей
4. [ ] Memory leak detection

**Оценка:** 3–5 дней

---

### Блокер 3: PostgreSQL Finalization

**Описание:** Требуется staging cutover + rollback rehearsal.

**Влияние:** Блокирует production DB path.

**Задачи:**
1. [ ] Staging cutover по runbook
2. [ ] Rollback rehearsal
3. [ ] Final smoke тесты
4. [ ] Документирование результатов

**Оценка:** 2–3 дня

---

### Блокер 4: Security MVP Evidence

**Описание:** Certificate pinning, HTTPS, шифрование credentials требуют полевой валидации.

**Влияние:** Блокирует релизный gate.

**Задачи:**
1. [ ] HTTPS topology validation
2. [ ] Certificate pinning field test
3. [ ] Credential migration logs
4. [ ] Audit persistence enabled

**Оценка:** 3–5 дней

---

## 3. Приоритизированный план задач

### Неделя 1: Foundation + RTSP Compilation

| ID | Задача | Приоритет | Оценка | Статус |
|----|--------|-----------|--------|--------|
| W1-01 | Фиксация MVP scope, синхронизация статусных документов | P0 | 1 день | ⚠️ Начато |
| W1-02 | PostgreSQL staging cutover + rollback rehearsal | P0 | 2–3 дня | ❌ Не начато |
| W1-03 | Установка зависимостей RTSP (CMake, FFmpeg, pkg-config) | P0 | 1 день | ❌ Не начато |
| W1-04 | Компиляция `libvideo_processing` для Desktop | P0 | 2–3 дня | ❌ Не начато |
| W1-05 | Генерация cinterop биндингов + проверка компиляции | P0 | 1–2 дня | ❌ Не начато |

**Критерий готовности недели 1:**
- [ ] PostgreSQL staging gate `GO`
- [ ] Native библиотека скомпилирована для Desktop
- [ ] Kotlin cinterop компилируется без ошибок

---

### Неделя 2: RTSP Activation + HLS Runtime + Screenshot

| ID | Задача | Приоритет | Оценка | Статус |
|----|--------|-----------|--------|--------|
| W2-01 | Активация FFI в `NativeRtspClient.native.kt` | P0 | 2–3 дня | ❌ Не начато |
| W2-02 | Интеграция RTSP с VideoPlayer (Web/Desktop) | P0 | 2–3 дня | ❌ Не начато |
| W2-03 | HLS long-run validation (30+ минут, reconnect, cleanup) | P1 | 2 дня | 🟡 Частично |
| W2-04 | Screenshot Pipeline: `captureFrame(...)` + FFmpeg fallback | P1 | 2–3 дня | ❌ Не начато |
| W2-05 | ONVIF Events: автоматизируемый HTTP-сценарий | P1 | 1 день | ✅ Выполнено |

**Критерий готовности недели 2:**
- [ ] RTSP native подключается к реальной камере, H.264 воспроизводится
- [ ] HLS pipeline стабилен при long-run
- [ ] Screenshot pipeline проходит smoke

---

### Неделя 3: Security Closure + Web Finalization + Android

| ID | Задача | Приоритет | Оценка | Статус |
|----|--------|-----------|--------|--------|
| W3-01 | Security evidence: HTTPS, pinning, credentials, audit | P1 | 3–5 дней | 🟡 Частично |
| W3-02 | Web closure: финальная интеграция видеоплеера | P1 | 2 дня | ✅ Выполнено |
| W3-03 | Android: интеграция видеоплеера с RTSP/HLS | P2 | 1–2 недели | ⚠️ В процессе |
| W3-04 | Desktop: long-run validation видеоплеера (30+ минут) | P2 | 2–3 дня | 🟡 Частично |
| W3-05 | Rate limiting + Redis preflight тесты | P2 | 1 день | ✅ Выполнено |

**Критерий готовности недели 3:**
- [ ] Security MVP checklist `GO`
- [ ] Web video player стабилен
- [ ] Android проходит обязательные MVP smoke
- [ ] Desktop проходит long-run validation

---

### Неделя 4: Testing + Acceptance Gate

| ID | Задача | Приоритет | Оценка | Статус |
|----|--------|-----------|--------|--------|
| W4-01 | Integration tests: API, БД/миграции, video pipeline | P2 | 2–3 дня | 🟡 Частично |
| W4-02 | Video E2E gate: `scripts/video-e2e-go-no-go.ps1` | P2 | 2 дня | ❌ Не начато |
| W4-03 | Минимальный E2E набор: discover → add → play → record → replay | P2 | 2–3 дня | ❌ Не начато |
| W4-04 | Сводка в go/no-go матрицу | P2 | 1 день | ❌ Не начато |

**Критерий готовности недели 4:**
- [ ] Обязательный тестовый контур стабильно green
- [ ] Все MVP-критичные сценарии покрыты воспроизводимыми проверками
- [ ] Go/No-Go пакет подтвержден

---

## 4. Детализация задач по дням

### День 1: PostgreSQL Finalization

**Цель:** Закрыть PostgreSQL cutover

**Действия:**
1. Изучить runbook `POSTGRESQL_FINALIZATION_RUNBOOK.md`
2. Подготовить staging окружение
3. Выполнить cutover
4. Провести smoke тесты
5. Провести rollback rehearsal
6. Задокументировать результаты

**Результат:** Отчет `POSTGRESQL_FINALIZATION_STAGING_REPORT_*.md`

---

### День 2–3: RTSP Native Compilation

**Цель:** Скомпилировать нативную библиотеку

**Действия:**
1. Установить CMake, FFmpeg, pkg-config
2. Проверить `native/video-processing/CMakeLists.txt`
3. Скомпилировать для текущей платформы
4. Проверить экспорт символов
5. Поместить библиотеку в правильный каталог

**Результат:** `.so/.dylib/.dll` с верифицированными символами

---

### День 4–5: Kotlin Cinterop

**Цель:** Сгенерировать и активировать FFI биндинги

**Действия:**
1. Создать `.def` файл для cinterop
2. Настроить `build.gradle.kts` для cinterop
3. Сгенерировать биндинги
4. Исправить ошибки компиляции
5. Активировать FFI в `NativeRtspClient.native.kt`

**Результат:** `core:network:compileKotlin*` проходит

---

## 5. Definition of Done для Фазы 1 = 100%

Фаза 1 считается закрытой при **одновременном** выполнении:

1. ✅ Все критические блокеры для MVP закрыты (RTSP native, HLS runtime, PostgreSQL)
2. ✅ Видео-контур production-ready: discover → play → record → replay → events
3. ✅ Web realtime/auth/video сценарии завершены
4. ✅ Security MVP критерии выполнены (все контроли `PASS`)
5. ✅ Integration/E2E smoke контур стабильно проходит в CI
6. ✅ Android и Desktop проходят обязательные MVP smoke
7. ✅ Go/No-Go пакет подтвержден

---

## 6. Риски и митигация

| Риск | Вероятность | Влияние | Митигация |
|------|-------------|---------|-----------|
| FFmpeg API несовместимость | Средняя | Высокое | Fallback к HLS, документирование |
| Memory leaks в RTSP | Средняя | Высокое | Valgrind/AddressSanitizer тесты |
| Проблемы производительности | Высокая | Среднее | Раннее бенчмаркирование |
| Несовместимость камер | Высокая | Среднее | Тестирование с 3+ камерами |

---

## 7. Метрики успеха

| Метрика | Цель | Текущее |
|---------|------|---------|
| Успешный старт live playback | ≥ 95% | ~75% |
| Непрерывное воспроизведение | ≥ 30 минут | Не подтверждено |
| Ошибки start/stop записи | ≤ 5% | ~82% |
| Время до первого кадра (TTFF) | ≤ 5с | ~88% |
| Покрытие тестами | ≥ 50% | ~28% |
| Security controls | Все `PASS` | 2 `PASS`, 4 `CONDITIONAL` |

---

## 8. Связанные документы

- [PHASE1_MVP_TO_100_PLAN.md](PHASE1_MVP_TO_100_PLAN.md) — базовый план
- [MVP_PRIORITY_TASKS_STATUS.md](../../archive/docs-duplicates-2026-08-08/MVP_PRIORITY_TASKS_STATUS.md) — статус задач
- [RTSP_FOCUS_AUTOMATION_STATUS_2026-04-27.md](../reports/RTSP_FOCUS_AUTOMATION_STATUS_2026-04-27.md) — RTSP статус
- [SECURITY_MVP_READINESS.md](../status/SECURITY_MVP_READINESS.md) — security статус
- [POSTGRESQL_FINALIZATION_RUNBOOK.md](../POSTGRESQL_FINALIZATION_RUNBOOK.md) — DB runbook

---

**Документ создан:** 2026-06-04  
**Следующее обновление:** После закрытия каждой недели или критического блокера
