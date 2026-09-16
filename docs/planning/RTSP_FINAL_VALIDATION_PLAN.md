# План финальной валидации RTSP

**Дата:** 27 May 2026  
**Цель:** Подтвердить стабильность RTSP клиента для production  
**Срок:** 3-5 дней

---

## 📊 Текущее состояние

| Показатель | Значение |
|------------|----------|
| RTSP интеграция | 95% |
| Аудио декодирование | 100% |
| FFI биндинги | 100% |
| Long-run тесты | 🟡 Запущен fast mode |
| Реальные камеры | ⏳ Не тестированы |

---

## 🎯 Критерии успеха

### Must Have (обязательно для прохождения)

- [ ] **Smoke тест (10 мин)** — все эмуляторы PASS
- [ ] **Short тест (2 часа)** — стабильность без критичных ошибок
- [ ] **Long тест (24 часа)** — 0 memory leaks, ≤3 reconnects
- [ ] **Реальные камеры (5+ устройств)** — различные производители
- [ ] **Кодеки** — H.264, H.265, MJPEG, AAC, PCMU, PCMA
- [ ] **AV синхронизация** — drift <50ms

### Nice to Have (рекомендуется)

- [ ] **4K разрешение** — тестирование производительности
- [ ] **Низкий битрейт** — тестирование стабильности
- [ ] **Множественные потоки** — одновременное подключение к 3+ камерам
- [ ] **Network loss simulation** — тестирование при потере пакетов

---

## 📋 Профили тестирования

### 1. Smoke (10 минут)

**Цель:** Быстрая проверка базовой функциональности

**Камеры:**
- Emulator_AAC (H.264 + AAC)
- Emulator_PCMU (H.264 + PCMU)
- Emulator_PCMA (H.264 + PCMA)

**Ожидаемые результаты:**
- Все 3 камеры подключаются успешно
- Получение видео и аудио кадров
- 0 ошибок декодирования
- FPS ≥ 15

**Команда:**
```powershell
.\scripts\run-rtsp-final-validation.ps1 -Profile smoke
```

---

### 2. Short (2 часа)

**Цель:** Проверка стабильности на короткой дистанции

**Камеры:**
- Emulator_AAC
- Hikvision_Test (если доступна)
- Dahua_Test (если доступна)

**Ожидаемые результаты:**
- ≤1 reconnect на камеру
- 0 memory leaks
- FPS стабильный (±20%)
- ≤5 decoding errors

**Команда:**
```powershell
.\scripts\run-rtsp-final-validation.ps1 -Profile short
```

---

### 3. Long (24 часа)

**Цель:** Подтверждение долгосрочной стабильности

**Камеры:**
- Emulator_AAC
- Emulator_PCMU

**Ожидаемые результаты:**
- ≤3 reconnects за 24 часа
- Memory growth <20%
- 0 crashes
- Стабильный FPS
- 0 critical errors

**Команда:**
```powershell
.\scripts\run-rtsp-final-validation.ps1 -Profile long
```

---

### 4. Full (48 часов)

**Цель:** Полное тестирование всех сценариев

**Камеры:**
- Emulator_AAC
- Emulator_PCMU
- Emulator_PCMA
- Emulator_MJPEG
- Emulator_H265
- Emulator_LowBitrate
- Emulator_4K

**Ожидаемые результаты:**
- Все кодеки работают
- Все профили PASS
- Memory leaks <10%
- Стабильная работа на всех профилях

**Команда:**
```powershell
.\scripts\run-rtsp-final-validation.ps1 -Profile full
```

---

## 📅 План выполнения

### День 1: Smoke + Short

| Время | Задача | Результат |
|-------|--------|-----------|
| 09:00 | Запуск smoke теста | 3 эмулятора PASS |
| 10:00 | Анализ результатов | Issues задокументированы |
| 11:00 | Исправление проблем | Критичные issues fixed |
| 13:00 | Запуск short теста | 2 часа тестирования |
| 15:00 | Параллельный мониторинг | Метрики собираются |
| 17:00 | Анализ short результатов | Отчёт готов |

**KPI Дня 1:**
- Smoke тест PASS
- Short тест запущен
- 0 критичных blockеров

---

### День 2: Long тест

| Время | Задача | Результат |
|-------|--------|-----------|
| 09:00 | Запуск long теста (24ч) | Тест запущен |
| 09:30 | Настройка мониторинга | Metрики собираются |
| 12:00 | Проверка через 2ч | baseline OK |
| 15:00 | Проверка через 6ч | Стабильность OK |
| 18:00 | Проверка через 9ч | Memory OK |
| 09:00+1 | Проверка через 24ч | Финальный анализ |

**KPI Дня 2:**
- Long тест запущен
- Мониторинг работает
- 0 critical alerts

---

### День 3: Реальные камеры

| Время | Задача | Результат |
|-------|--------|-----------|
| 09:00 | Подготовка камер (5+) | Hikvision, Dahua, Axis |
| 10:00 | Тестирование Hikvision | PASS |
| 11:00 | Тестирование Dahua | PASS |
| 12:00 | Тестирование Axis | PASS |
| 14:00 | Тестирование Sony | PASS |
| 15:00 | Тестирование Generic ONVIF | PASS |
| 16:00 | Генерация матрицы совместимости | Документ готов |

**KPI Дня 3:**
- 5+ камер протестированы
- Матрица совместимости создана
- 0 critical issues

---

### День 4: Оптимизация и финализация

| Время | Задача | Результат |
|-------|--------|-----------|
| 09:00 | Анализ всех результатов | Issues consolidated |
| 11:00 | Исправление remaining issues | Fixes applied |
| 14:00 | Повторный smoke тест | Validation PASS |
| 16:00 | Генерация финального отчёта | Report ready |

**KPI Дня 4:**
- Все issues исправлены
- Final report готов
- RTSP validation = PASS

---

## 📁 Создаваемые артефакты

### Скрипты

1. **`scripts/run-rtsp-final-validation.ps1`** ✅
   - Автоматизированное тестирование
   - Поддержка всех профилей
   - Генерация отчётов

2. **`scripts/monitor-rtsp-performance.ps1`** ✅
   - Мониторинг в реальном времени
   - Memory/CPU метрики
   - Alert система

### Тесты

3. **`shared/src/desktopTest/.../RtspValidationTest.kt`** ✅
   - Валидация с реальными камерами
   - Сбор метрик
   - Генерация отчётов

### Конфигурация

4. **`config/test-cameras.rtsp.json`** ✅
   - 10 тестовых камер
   - 3 эмулятора
   - 7 реальных камер

### Отчёты

5. **`docs/reports/rtsp-validation/validation-report-{profile}-{date}.md`**
   - Отчёты по каждому профилю
   - Метрики и статистика
   - Список ошибок

6. **`docs/reports/rtsp-validation/camera-compatibility-matrix.md`**
   - Матрица совместимости камер
   - Поддерживаемые кодеки
   - Известные проблемы

7. **`docs/reports/rtsp-validation/long-run-test-results.md`**
   - Результаты 24ч теста
   - Memory analysis
   - Stability metrics

---

## 🚨 Обработка ошибок

### Критичные ошибки (блокируют прохождение)

- **Connection timeout** — превышено 10 сек
- **Decoding failure** — более 10 ошибок
- **Memory leak** — рост >20%
- **Crash** — любой crash процесса

**Действие:**
1. Задокументировать в `BLOCKERS_LOG.md`
2. Исправить в течение 2 часов
3. Перезапустить тест

### Предупреждения (не блокируют, но требуют внимания)

- **Reconnect >3** — переподключения
- **FPS drop** — падение FPS >30%
- **Audio errors** — ошибки аудио декодирования

**Действие:**
1. Задокументировать в `WARNINGS_LOG.md`
2. Исправить до финального релиза
3. Мониторить в production

---

## 📊 Метрики для отслеживания

### Подключение

| Метрика | Цель | Критично |
|---------|------|----------|
| Connection time | <5 sec | >10 sec |
| Reconnect count | ≤3 (24ч) | >5 (24ч) |
| Failed connections | 0 | >1 |

### Видео

| Метрика | Цель | Критично |
|---------|------|----------|
| FPS average | ≥15 | <10 |
| FPS stability | ±20% | ±50% |
| Dropped frames | <1% | >5% |
| Decoding errors | 0 | >10 |

### Аудио

| Метрика | Цель | Критично |
|---------|------|----------|
| Audio frames | >0 | 0 |
| Decoding errors | 0 | >5 |
| Sync drift | <50ms | >100ms |

### Память

| Метрика | Цель | Критично |
|---------|------|----------|
| Memory growth | <20% | >50% |
| Memory leak | NO | YES |
| Peak memory | <500 MB | >1 GB |

---

## ✅ Definition of Done (RTSP Validation)

### Критерии прохождения

- [ ] Smoke тест: 3/3 эмуляторов PASS
- [ ] Short тест: 2 часа без critical errors
- [ ] Long тест: 24 часа, memory growth <20%
- [ ] Реальные камеры: 5+ устройств PASS
- [ ] Кодеки: H.264, H.265, MJPEG, AAC, PCMU, PCMA
- [ ] Memory: 0 leaks detected
- [ ] Errors: 0 critical errors
- [ ] Documentation: все отчёты сгенерированы

### Артефакты для релиза

- [ ] `validation-report-smoke-*.md` — PASS
- [ ] `validation-report-short-*.md` — PASS
- [ ] `validation-report-long-*.md` — PASS
- [ ] `camera-compatibility-matrix.md` — заполнен
- [ ] `long-run-test-results.md` — 24ч PASS
- [ ] `RTSP_VALIDATION_SUMMARY.md` — финальная сводка

---

## 🚀 Следующие шаги

### Immediate (День 1)

1. **Запуск smoke теста**
   ```powershell
   .\scripts\run-rtsp-final-validation.ps1 -Profile smoke
   ```

2. **Мониторинг в реальном времени**
   ```powershell
   .\scripts\monitor-rtsp-performance.ps1 -DurationMinutes 10
   ```

3. **Анализ результатов**
   - Проверка отчётов
   - Выявление проблем
   - Исправление critical issues

### Short-term (День 2-3)

4. **Запуск long теста (24ч)**

5. **Тестирование реальных камер (5+)**

6. **Генерация матрицы совместимости**

### Final (День 4)

7. **Финальная проверка всех результатов**

8. **Генерация итогового отчёта**

9. **Go/No-Go решение**

---

**Дата создания:** 27 May 2026  
**Автор:** AI Assistant  
**Статус:** 🟡 План готов, выполнение начато  
**Следующий check-in:** После smoke теста (День 1, 10:00)
