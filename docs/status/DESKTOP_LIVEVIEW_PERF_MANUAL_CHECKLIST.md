# Manual Checklist — Desktop LiveView Performance (4.3.3.1)

**Дата:** `2026-03-27`  
**Цель:** получить воспроизводимые метрики производительности Desktop LiveView для `GRID_4/9/16` и формально закрыть анализ производительности в `4.3.3.1`.

---

## 1) Подготовка окружения (2-3 мин)

1. Запустить backend/API и источник(и) RTSP потоков.
2. Запустить Desktop клиент и открыть экран LiveView.
3. Подготовить стабильный набор тестовых камер:
   - одинаковый codec/bitrate по возможности;
   - одинаковое разрешение для сравнимых результатов.
4. Зафиксировать env-профиль телеметрии (если используется):
   - `IPCSS_METRICS_PUBLISH_MIN_INTERVAL_MS`
   - `IPCSS_METRICS_RENDER_FPS_DELTA`
   - `IPCSS_METRICS_DROPPED_DELTA`

Рекомендуемые профили:
- `GRID_9`: `1000 / 2 / 5`
- `GRID_16`: `1500 / 3 / 8`

Рекомендуемые `staggered connect` профили (RTSP cold start):

| Класс ПК | NORMAL base/jitter (ms) | BACKGROUND base/jitter (ms) | Когда использовать |
|----------|---------------------------|-------------------------------|--------------------|
| High-end | `80 / 160` | `240 / 300` | Сильный CPU/сеть, минимизировать startup latency |
| Mid-range | `120 / 240` | `450 / 500` | Базовый профиль по умолчанию |
| Entry-level | `180 / 320` | `700 / 900` | Слабый CPU/диск/сеть, снизить burst на старте |

Переменные окружения для профилей:
- `IPCSS_RTSP_STAGGER_NORMAL_BASE_MS`
- `IPCSS_RTSP_STAGGER_NORMAL_JITTER_MS`
- `IPCSS_RTSP_STAGGER_BACKGROUND_BASE_MS`
- `IPCSS_RTSP_STAGGER_BACKGROUND_JITTER_MS`

---

## 2) Сценарий прогона (на каждый layout)

Layouts: `GRID_4`, `GRID_9`, `GRID_16`.

Для каждого layout:
1. Выбрать соответствующее число камер.
2. Дождаться стабилизации потока `60-90` секунд.
3. Зафиксировать значения из telemetry header:
   - `Render FPS(avg)`
   - `Dropped`
   - `Lazy paused`
   - `Reconnects`
   - `Errors`
4. Зафиксировать системные метрики:
   - CPU процесса Desktop клиента;
   - RAM процесса Desktop клиента.
5. Пролистать grid вниз/вверх (для `9/16`) и убедиться:
   - невидимые камеры уходят в background;
   - после возврата в viewport поток восстанавливается без зависаний.
6. Переключить layout `4 -> 9 -> 16 -> 4`, проверить отсутствие визуальных фризов и корректность восстановления.
7. Выполнить сценарий массового старта:
   - открыть `GRID_16` на холодном старте LiveView;
   - убедиться, что подключения поднимаются распределенно (без резкого всплеска reconnect/error);
   - зафиксировать наблюдение в графе `Result` для `GRID_16`.

---

## 3) Таблица фиксации результатов

| Layout | Cameras | Render FPS(avg) | Dropped | Lazy paused | Reconnects | Errors | CPU % | RAM MB | UI smoothness | Result |
|--------|---------|------------------|---------|-------------|------------|--------|-------|--------|---------------|--------|
| GRID_4 | `...` | `...` | `...` | `...` | `...` | `...` | `...` | `...` | `PASS/FAIL` | `PASS/FAIL` |
| GRID_9 | `...` | `...` | `...` | `...` | `...` | `...` | `...` | `...` | `PASS/FAIL` | `PASS/FAIL` |
| GRID_16 | `...` | `...` | `...` | `...` | `...` | `...` | `...` | `...` | `PASS/FAIL` | `PASS/FAIL` |

---

## 4) Критерии приемки (4.3.3)

- **Functional PASS**
  - layout-переключение без зависаний/крашей;
  - viewport-priority работает корректно (невидимые background, видимые восстанавливаются);
  - staggered start работает корректно (нет connect/reconnect storm на старте `GRID_16`);
  - нет каскадного роста reconnect/error при обычной навигации.

- **Performance PASS**
  - `GRID_16`: стабильная работа без критического деградационного дрейфа;
  - CPU в целевом диапазоне (< 80% на среднем ПК, ориентир плана);
  - RAM в целевом диапазоне (< 2 GB для 16 камер, ориентир плана).

---

## 5) Быстрый формат отправки в чат

```text
DESKTOP_4_3_3_RESULTS
GRID_4: cams=.., fps=.., dropped=.., lazyPaused=.., rec=.., err=.., cpu=.., ram=.., smooth=PASS/FAIL
GRID_9: cams=.., fps=.., dropped=.., lazyPaused=.., rec=.., err=.., cpu=.., ram=.., smooth=PASS/FAIL
GRID_16: cams=.., fps=.., dropped=.., lazyPaused=.., rec=.., err=.., cpu=.., ram=.., smooth=PASS/FAIL
```

После отправки AI-агент должен:
1. обновить `docs/DESKTOP_REFINEMENT_PLAN.md` (статус 4.3.3.1 и примечания);
2. обновить `docs/status/PROJECT_STATUS.md` / `PROJECT_STATUS_PHASES.md` при необходимости;
3. обновить `docs/status/DESKTOP_LIVEVIEW_PERF_REPORT.md`;
4. зафиксировать итог: `4.3.3.1 -> Open/Partial/Done`.

