# Runbook закрытия Open-пунктов 1.6

**Область:** `1.6 Веб-интерфейс`  
**Цель:** перевести `C4` и `F2` из `Open` в `Partial/Done` по воспроизводимым сценариям.

---

## 1) C4 — Устойчивость WebSocket при деградации сети

### 1.1 Предусловия

- Backend и web-клиент подняты локально.
- Есть тестовый пользователь с доступом к страницам `events/notifications/recordings`.
- В браузере доступен DevTools (Network throttling + offline toggle).

### 1.2 Базовый smoke (без деградации)

```powershell
cd D:\GitHub-Ai\IP-CSS\server\web
npm test -- useWebSocket.test.tsx websocketSlice.test.ts
```

**Критерий:** тесты проходят без падений.

### 1.3 Ручной сценарий деградации (DevTools)

1. Открыть UI и страницу с активными real-time обновлениями.
2. В DevTools включить throttling: `Slow 3G` на 60–90 сек.
3. Проверить, что:
   - соединение не приводит к крашу UI;
   - после восстановления сети сообщения продолжают приходить.
4. Переключить `Offline` на 20–30 сек, затем вернуть `Online`.
5. Повторить 3 цикла подряд.

**Критерий Partial:** нет зависаний/крэшей, reconnect работает хотя бы в 2/3 циклов.  
**Критерий Done:** 3/3 цикла успешны, данные после reconnection консистентны (нет дубликатов и потери критичных уведомлений).

### 1.4 Артефакты

- Короткий лог прогона в `docs/status/WEB_INTERFACE_RUNTIME_VALIDATION_LOG.md`:
  - дата/время;
  - браузер и версия;
  - результат по каждому циклу;
  - итог: `C4 -> Partial` или `C4 -> Done`.

---

## 2) F2 — Performance pass для web-интерфейса

### 2.1 Предусловия

- Production-like сборка web-клиента:

```powershell
cd D:\GitHub-Ai\IP-CSS\server\web
npm run build
npm run start
```

### 2.2 Проверка базовой производительности

1. Открыть ключевые страницы: `dashboard`, `cameras`, `events`, `recordings`.
2. Для каждой страницы собрать Lighthouse profile (Desktop).
3. Зафиксировать метрики:
   - Performance score;
   - LCP;
   - INP/TBT;
   - CLS.

### 2.3 Пороговые критерии

- **Partial:**
  - средний Lighthouse Performance >= 70;
  - нет критических регрессий (блокирующие long tasks, явные UI-freeze).
- **Done:**
  - средний Lighthouse Performance >= 80;
  - LCP <= 3.0s на ключевых страницах;
  - нет блокирующих long tasks в основных пользовательских сценариях.

### 2.4 Артефакты

- Сохранить результаты в `docs/status/WEB_INTERFACE_PERF_REPORT.md`:
  - страница -> метрики;
  - узкие места;
  - список конкретных оптимизаций (если `Partial`).
- Быстрый ручной сценарий выполнения: `docs/status/WEB_INTERFACE_PERF_MANUAL_CHECKLIST.md`

---

## 3) Правило обновления статуса 1.6

После завершения прогонов:

1. Обновить `docs/status/WEB_INTERFACE_ACCEPTANCE_CHECKLIST_STATUS.md`
   - `C4` и/или `F2` на новый статус.
2. Пересчитать итоговый `CONDITIONAL GO / GO`.
3. Добавить дату и ссылку на артефакты (`RUNTIME_VALIDATION_LOG` / `PERF_REPORT`).

### Шаблоны артефактов

- `docs/status/WEB_INTERFACE_RUNTIME_VALIDATION_LOG.md`
- `docs/status/WEB_INTERFACE_PERF_REPORT.md`
- `docs/status/WEB_INTERFACE_PERF_MANUAL_CHECKLIST.md`

