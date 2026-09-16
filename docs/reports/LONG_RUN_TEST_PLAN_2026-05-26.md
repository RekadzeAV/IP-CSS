# План: Long-run тестирование стабильности

**Дата:** 26 May 2026  
**Приоритет:** 2 Фазы 2  
**Статус:** 🟡 В процессе  
**Срок:** 31 May 2026 (5 дней)

---

## 🎯 Цель

Проверить стабильность RTSP клиента с аудио декодированием и AV синхронизацией на протяжении 2+ часов непрерывной работы.

---

## 📋 Тестовые сценарии

### Сценарий 1: AAC аудио (эмулятор)

**Команда:**
```powershell
# Запуск эмулятора
python scripts/rtsp-audio-test-server.py --audio-codec aac --port 8554

# Long-run тест (2 часа)
.\scripts\long-run-test.ps1 -DurationSeconds 7200 -CameraName "Emulator_AAC"
```

**Проверяем:**
- [ ] Стабильность подключения
- [ ] Отсутствие memory leaks
- [ ] AV синхронизация <50ms drift
- [ ] Нет crash'ов или exceptions

### Сценарий 2: PCMU аудио (эмулятор)

**Команда:**
```powershell
# Запуск эмулятора
python scripts/rtsp-audio-test-server.py --audio-codec pcmu --port 8555

# Long-run тест
.\scripts\long-run-test.ps1 -DurationSeconds 7200 -CameraName "Emulator_PCMU"
```

### Сценарий 3: PCMA аудио (эмулятор)

**Команда:**
```powershell
# Запуск эмулятора
python scripts/rtsp-audio-test-server.py --audio-codec pcma --port 8556

# Long-run тест
.\scripts\long-run-test.ps1 -DurationSeconds 7200 -CameraName "Emulator_PCMA"
```

---

## 📊 Метрики успеха

| Метрика | Цель | Статус |
|---------|------|--------|
| Duration | 7200 сек (2 часа) | ⏳ Планируется |
| Memory growth | <10% | ⏳ Планируется |
| Connection success rate | >95% | ⏳ Планируется |
| AV drift | <50ms | ⏳ Планируется |
| Crash count | 0 | ⏳ Планируется |
| Memory leaks | 0 | ⏳ Планируется |

---

## 🔧 Мониторинг

### Memory usage

**Инструменты:**
- Windows: Task Manager / Process Explorer
- Linux: `top`, `htop`, `valgrind --leak-check=full`

**Что мониторим:**
- Working Set (физическая память)
- Private Memory (выделенная память)
- Handle count (обработчики файлов/сокетов)
- Thread count (потоки)

**Критерии memory leak:**
- Рост памяти >10% за 2 часа
- Непрерывный рост без спадов
- Handle count растёт без стабилизации

### Connection stability

**Что проверяем:**
- Периодические reconnect (если enabled)
- Ошибки подключения
- Таймауты
- Состояние RTSP сессии

### AV Sync stability

**Что проверяем:**
- drift между аудио и видео
- Пропущенные фреймы
- Задержки
- Корректность сброса при reconnect

---

## 📁 Выходные данные

### Отчёты

1. **Markdown отчёт:** `diagnostics/long-run-tests/long-run-test-{timestamp}.md`
   - Summary метрик
   - Memory snapshots
   - Выводы о стабильности

2. **JSON отчёт:** `diagnostics/long-run-tests/long-run-test-{timestamp}.json`
   - Полные данные в структурированном формате
   - Метрики для анализа

3. **CSV метрики:** `diagnostics/long-run-tests/long-run-test-{timestamp}.csv`
   - По-итерационные метрики
   - Для построения графиков

### Логи

- RTSP client logs
- Memory usage snapshots
- Connection events
- AV sync statistics

---

## 🚀 План выполнения

### День 1 (26 May 2026)

**Утро:**
1. [x] Создать скрипт `long-run-test.ps1`
2. [x] Запустить эмулятор AAC (порт 8554)
3. [ ] Провести quick test (5 минут)
4. [ ] Проверить корректность сбора метрик

**День:**
5. [ ] Запустить long-run тест на 2 часа (AAC)
6. [ ] Мониторить memory usage в реальном времени
7. [ ] Проверять AV drift каждые 30 минут

**Вечер:**
8. [ ] Собрать результаты
9. [ ] Создать отчёт
10. [ ] Планировать следующие тесты

### День 2 (27 May 2026)

**Утро:**
1. [ ] Запустить эмулятор PCMU (порт 8555)
2. [ ] Long-run тест на 2 часа (PCMU)
3. [ ] Мониторинг memory

**День:**
4. [ ] Запустить эмулятор PCMA (порт 8556)
5. [ ] Long-run тест на 2 часа (PCMA)
6. [ ] Мониторинг memory

**Вечер:**
7. [ ] Сравнить результаты всех 3 кодеков
8. [ ] Создать итоговый отчёт

---

## 📈 Ожидаемые результаты

### Успешный тест

```
================================================================================
  Final Summary
================================================================================

Duration:        7200 seconds (120.0 minutes)
Iterations:      120
Success Rate:    100.00%
Avg Memory:      145.23 MB
Max Memory:      152.67 MB
Memory Trend:    Stable

Reports:
  Markdown: diagnostics/long-run-tests/long-run-test-20260527-120000.md
  JSON:     diagnostics/long-run-tests/long-run-test-20260527-120000.json
  CSV:      diagnostics/long-run-tests/long-run-test-20260527-120000.csv

✅ Long-run test passed!
```

### Обнаружение memory leak

```
================================================================================
  Final Summary
================================================================================

Duration:        7200 seconds (120.0 minutes)
Iterations:      120
Success Rate:    100.00%
Avg Memory:      145.23 MB
Max Memory:      312.45 MB
Memory Trend:    Increasing (+115.02%)

Reports:
  Markdown: diagnostics/long-run-tests/long-run-test-20260527-120000.md
  JSON:     diagnostics/long-run-tests/long-run-test-20260527-120000.json
  CSV:      diagnostics/long-run-tests/long-run-test-20260527-120000.csv

⚠️ Memory leak detected! Memory increased by 115.02% during the test.
```

---

## 🎯 Критерии завершения Приоритета 2

### MVP Ready (обязательные)

- [ ] Проведён тест на 2+ часа для AAC
- [ ] Проведён тест на 2+ часа для PCMU
- [ ] Проведён тест на 2+ часа для PCMA
- [ ] Memory usage задокументирована
- [ ] AV drift измерен
- [ ] Отчёты сгенерированы

### Production Ready (желательные)

- [ ] Нет memory leaks подтверждено
- [ ] Connection success rate >95%
- [ ] AV drift <50ms подтверждено
- [ ] Стабильность при reconnect проверена

---

## 📚 Связанные документы

- [AV_SYNC_IMPLEMENTATION_STATUS_2026-05-26.md](AV_SYNC_IMPLEMENTATION_STATUS_2026-05-26.md) — Приоритет 1
- [PHASE1_PRODUCTION_READINESS_PLAN_2026-05-26.md](PHASE1_PRODUCTION_READINESS_PLAN_2026-05-26.md) — План Фазы 2
- scripts/long-run-test.ps1 *(утерян/в архиве)* — Скрипт тестирования
- [scripts/rtsp-audio-test-server.py](../../scripts/rtsp-audio-test-server.py) — RTSP эмулятор

---

**План утверждён:** ________________  
**Дата начала:** 26 May 2026  
**Дата окончания:** 31 May 2026  
**Ответственный:** AI Assistant
