# Manual Checklist — F2 Performance Validation

**Дата:** `2026-03-27`  
**Цель:** получить валидные Lighthouse-метрики для закрытия `F2` после невалидных headless-замеров (`NO_LCP`).

## Автоматизация (замена минимального perf-gate в CI)

Для **W3-4** и регресс-контроля без полного Lighthouse в каждом PR:

- В корне: `npm ci && npm run build && npm test` в каталоге `server/web`, либо job **`mvp-automated-acceptance`** в [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) (там же Gradle desktopTest, video gate, Phase1 summary).
- Скрипт: [`scripts/ci/mvp-automated-acceptance.sh`](../../scripts/ci/mvp-automated-acceptance.sh) (Linux/macOS) / [`scripts/ci/mvp-automated-acceptance.ps1`](../../scripts/ci/mvp-automated-acceptance.ps1) (Windows). См. [MVP_PHASE1_AUTOMATED_ACCEPTANCE.md](../automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md) (профиль **`mvp-ci`** в CI).

Полный чеклист ниже остаётся для **валидных** LCP/INP в обычном Chrome, когда нужны метрики уровня Lighthouse.

---

## 1) Подготовка окружения (1-2 мин)

1. Открыть терминал:

```powershell
cd D:\GitHub-Ai\IP-CSS\server\web
npm run build
npm run start
```

2. Открыть браузер (обычный Chrome, не headless): `http://localhost:3000/login`
3. Выполнить вход тестовым пользователем.
4. Открыть DevTools -> Lighthouse.

Параметры Lighthouse:
- Mode: `Navigation`
- Device: `Desktop`
- Category: `Performance` (только Performance)
- Throttling: `Simulated` (или `No throttling`, если simulated даёт артефакты)
- Extensions: выключить

---

## 2) Прогон ключевых страниц (3-4 мин)

Страницы:
- `/dashboard`
- `/cameras`
- `/events`
- `/recordings`

Для каждой страницы:
1. Обновить страницу и дождаться стабилизации UI (2-3 секунды).
2. Запустить Lighthouse.
3. Зафиксировать:
   - Performance score
   - LCP
   - INP (или TBT, если INP отсутствует)
   - CLS
   - наличие long tasks / runtime warnings

---

## 3) Таблица фиксации (копировать в PERF_REPORT)

| Страница | Performance | LCP (s) | INP (ms) / TBT (ms) | CLS | Long tasks | Итог |
|----------|-------------|---------|----------------------|-----|-----------|------|
| /dashboard | `...` | `...` | `...` | `...` | `...` | `PASS/FAIL` |
| /cameras | `...` | `...` | `...` | `...` | `...` | `PASS/FAIL` |
| /events | `...` | `...` | `...` | `...` | `...` | `PASS/FAIL` |
| /recordings | `...` | `...` | `...` | `...` | `...` | `PASS/FAIL` |

## 3.1 Быстрый формат для отправки в чат (1 сообщение)

Скопировать и заполнить:

```text
F2_RESULTS
/dashboard: perf=.., lcp=.., inp_or_tbt=.., cls=.., longTasks=..
/cameras: perf=.., lcp=.., inp_or_tbt=.., cls=.., longTasks=..
/events: perf=.., lcp=.., inp_or_tbt=.., cls=.., longTasks=..
/recordings: perf=.., lcp=.., inp_or_tbt=.., cls=.., longTasks=..
```

После отправки этого блока AI-агент должен:
1. обновить `docs/status/WEB_INTERFACE_PERF_REPORT.md`;
2. пересчитать средний score;
3. обновить `docs/status/WEB_INTERFACE_ACCEPTANCE_CHECKLIST_STATUS.md` (`F2`);
4. сообщить итог: `F2 -> Open/Partial/Done`.

Локальная автоматизация (опционально, без чата):

```powershell
cd D:\GitHub-Ai\IP-CSS
copy docs\status\F2_RESULTS_INPUT_TEMPLATE.txt docs\status\F2_RESULTS_INPUT.txt
# заполнить F2_RESULTS_INPUT.txt фактическими значениями
powershell -ExecutionPolicy Bypass -File scripts\process-f2-results.ps1 -InputPath "D:\GitHub-Ai\IP-CSS\docs\status\F2_RESULTS_INPUT.txt"
```

---

## 4) Критерии приёмки F2

- **Partial:** средний score >= 70, без блокирующих UI-freeze.
- **Done:** средний score >= 80, `LCP <= 3.0s` на ключевых страницах, без блокирующих long tasks.

---

## 5) Что обновить после прогона

1. `docs/status/WEB_INTERFACE_PERF_REPORT.md`
   - внести таблицу метрик;
   - пересчитать средний score;
   - вынести решение: `F2 -> Partial` или `F2 -> Done`.
2. `docs/status/WEB_INTERFACE_ACCEPTANCE_CHECKLIST_STATUS.md`
   - обновить строку `F2 Производительность`;
   - при выполнении критериев пересчитать итоговый статус блока `F`.

