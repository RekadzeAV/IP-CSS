# План доработки Фазы 1 (MVP) до 100%

**Проект:** IP-CSS  
**Версия проекта:** Alfa-0.1.1  
**Назначение:** базовый рабочий план для формирования процесса разработки по закрытию Фазы 1 (MVP) до 100%.

**Чеклисты и недельные итерации:** [TODO.md](../../archive/docs-duplicates-2026-08-08/TODO.md)  
**Детализация разделов 3–4** (пошагово, приоритеты при нехватке ресурса, актуальный статус и подэтапы): [PHASE1_MVP_TO_100_SECTIONS_3_4_DETAILED_PLAN.md](PHASE1_MVP_TO_100_SECTIONS_3_4_DETAILED_PLAN.md)  
**Границы MVP и операционные runbook'и:** [MVP_PHASE1_SCOPE_BOUNDARY.md](MVP_PHASE1_SCOPE_BOUNDARY.md) · [POSTGRESQL_STAGING_CUTOVER_AND_ROLLBACK_RUNBOOK.md](POSTGRESQL_STAGING_CUTOVER_AND_ROLLBACK_RUNBOOK.md) · [SECURITY_MVP_FIELD_VALIDATION_RUNBOOK.md](SECURITY_MVP_FIELD_VALIDATION_RUNBOOK.md) · [MVP_ONVIF_EVENTS_VERIFICATION.md](../automation/MVP_ONVIF_EVENTS_VERIFICATION.md)

**Позиция исполнения (2026-04-24, обновлено):** **Фаза 2 § 2.1** в [PROJECT_STATUS_PHASES.md](../status/PROJECT_STATUS_PHASES.md); **подфокус:** **2.1.7** (сквозная интеграция AI с видеопотоками RTSP/HLS → события). Прогон **1.10** по-прежнему не обязателен для сдвига этой позиции. **W4-5 / GO–NO-GO** ([RELEASE_GO_NO_GO_CHECKLIST.md](RELEASE_GO_NO_GO_CHECKLIST.md)) — отдельно, при закрытии Фазы 1.

---

## 1. Цель и область

Цель документа — зафиксировать правильную инженерную последовательность доработки MVP с учетом зависимостей между слоями кода, минимизировать повторные переделки и довести Фазу 1 до формального состояния "done".

Область плана:
- Этапы `1.1`-`1.10` из `docs/status/PROJECT_STATUS_PHASES.md`.
- Критические блокеры из `docs/planning/CRITICAL_BLOCKERS_REMEDIATION_PLAN.md`.
- Критерии приемки безопасности и релиза.

---

## 2. Последовательность выполнения (code-first)

1. **Foundation lock:** стабилизация контрактов и границ MVP.
2. **Data and backend baseline:** миграции БД, production DB path, стабильность хранилища.
3. **Video transport core:** RTSP/HLS/screenshot/runtime устойчивость.
4. **Server production closure:** окончательная серверная интеграция в production-контуре.
5. **Web closure:** realtime WebSocket + безопасное хранение токенов + video player integration.
6. **Security closure:** HTTPS boundary, pinning, аудит/журналирование.
7. **Platform closure:** Android/Desktop по обязательным MVP-сценариям.
8. **Test and acceptance gate:** integration/e2e матрица + go/no-go.

Такой порядок снижает риск, когда UI и E2E делаются раньше стабилизации transport/runtime.

---

## 3. Детальный план по этапам

## 3.1 Этап 0: фиксация контуров MVP

**Цель:** убрать плавающий scope и зафиксировать границы приемки.

Задачи:
- Утвердить must-have сценарии Фазы 1 (discover, live view, recording, events, auth, basic web/mobile/desktop) — зафиксировано в [MVP_PHASE1_SCOPE_BOUNDARY.md](MVP_PHASE1_SCOPE_BOUNDARY.md).
- Зафиксировать ограничения scope (например, iOS-видео не является блокером MVP, если так принято командой) — см. тот же документ.
- Привести в соответствие статусные документы и чеклисты приемки.

Критерий готовности:
- Единый зафиксированный scope без конфликтующих трактовок в статус-документах.

---

## 3.2 Этап 1: 1.3 + 1.5 (данные и серверный baseline)

**Цель:** получить предсказуемую и устойчивую backend-базу.

Задачи:
- Довести миграции БД и сценарии rollback/smoke.
- Финализировать переходный production path для PostgreSQL.
- Дозакрыть оставшиеся хвосты repository V2.

Критерий готовности:
- Staging cutover проходит с успешным rollback rehearsal.
- Базовые integration smoke по БД и репозиториям стабильны.

---

## 3.3 Этап 2: 1.4 + 1.8 (видео и транспорт)

**Цель:** закрыть основной production-blocker MVP.

Задачи:
- Довести RTSP native integration и fallback path.
- Закрыть runtime-стабильность HLS (long-run, reconnect, cleanup).
- Завершить screenshot pipeline (`captureFrame(...)`).
- ONVIF Events: автоматизируемый HTTP-сценарий `scripts/onvif-events-api-verification.ps1` (при наличии камеры и конфига); полностью без эмулятора железа — см. [MVP_PHASE1_AUTOMATED_ACCEPTANCE.md](../automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md).

Критерий готовности:
- Устойчивый сценарий: discover -> play -> record -> replay -> events.
- Длительные прогоны без деградации и утечек.

---

## 3.4 Этап 3: 1.6 (Web closure)

**Цель:** довести web-клиент до полностью приемочного MVP-состояния.

Задачи:
- Полная интеграция WebSocket клиента (subscribe, reconnect, dedupe, backoff).
- Безопасное хранение JWT/refresh (httpOnly cookie flow и lifecycle).
- Финализация интеграции видеоплеера с RTSP/HLS backend path.
- Регресс web по автоматизируемому минимуму: `npm run build` + `npm test` в `server/web` (см. [MVP_PHASE1_AUTOMATED_ACCEPTANCE.md](../automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md)); полный Lighthouse — по [WEB_INTERFACE_PERF_MANUAL_CHECKLIST.md](../status/WEB_INTERFACE_PERF_MANUAL_CHECKLIST.md) § Автоматизация, если нужны «идеальные» метрики.

Критерий готовности:
- Все критические web P1 задачи закрыты.
- Метрики производительности валидны для приемки.

---

## 3.5 Этап 4: 1.9 (Security MVP closure)

**Цель:** обеспечить минимально необходимый production уровень безопасности для MVP.

Задачи:
- Финализировать HTTPS enforcement.
- Довести certificate pinning в поддерживаемых клиентских контурах.
- Подтвердить защиту чувствительных данных и миграцию legacy plaintext.
- Закрыть минимальный аудит и security logging для критических операций.

Критерий готовности:
- Security MVP checklist находится в зеленом статусе.
- Нет открытых high-severity проблем в auth/session/transport.

---

## 3.6 Этап 5: 1.7 (Android/Desktop closure)

**Цель:** закрыть практический MVP baseline клиентских платформ.

Задачи:
- Android: довести video + background recording до стабильного сценария.
- Desktop: завершить long-run валидацию видеоплеера и событийных экранов.
- Проверить ARM/x86 parity по обязательным сценариям.

Критерий готовности:
- Android и Desktop проходят обязательные MVP smoke без критических регрессий.

---

## 3.7 Этап 6: 1.10 (тестирование и приемка)

**Цель:** закрыть разрыв по качеству и воспроизводимости.

Задачи:
- Довести integration tests для API, БД/миграций, video pipeline (базовый контур: `./gradlew mvpAutomatedAcceptance`, CI job **`mvp-automated-acceptance`** — `scripts/ci/mvp-automated-acceptance.sh` с web + video gate + Phase1 summary; в CI video gate с **`video-e2e-acceptance-profile.mvp-ci.json`**; см. [MVP_PHASE1_AUTOMATED_ACCEPTANCE.md](../automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md)). Сводка `generate-phase1-go-no-go-summary.ps1` при наличии артефакта подключает последний **`w4-mvp-platform-gate-*.json`** в таблицу/JSON **только для контекста** (на решение GO/CONDITIONAL/NO_GO не влияет).
- Операторский контур локальных обёрток: все **`scripts/**/*.ps1`** и **`native/build-stub-libs.ps1`** поддерживают **`-ShowHelp`** (где применимо); реестр — раздел **8** того же документа (**§8.10–8.15**, в т.ч. **`native\`** в §8.15).
- Добавить минимальный E2E набор для критических пользовательских сценариев.
- Свести результаты в единую приемочную матрицу go/no-go ([RELEASE_GO_NO_GO_CHECKLIST.md](RELEASE_GO_NO_GO_CHECKLIST.md)).

Критерий готовности:
- Обязательный тестовый контур стабильно green.
- Все MVP-критичные сценарии покрыты воспроизводимыми проверками.

---

## 4. Приоритеты реализации

Порядок приоритета:
1. RTSP native + HLS runtime stability.
2. Полный WebSocket contour в web.
3. Безопасное хранение JWT в web.
4. HTTPS enforcement + pinning + security closure.
5. PostgreSQL finalization и migration rehearsal.
6. Android/Desktop MVP stability.
7. Integration/E2E completion.

---

## 5. Definition of Done для Фазы 1 = 100%

Фаза 1 считается закрытой при одновременном выполнении:
- Все критические блокеры для MVP закрыты.
- Видео-контур production-ready (RTSP/HLS/recording/events).
- Web realtime/auth/video сценарии завершены.
- Security MVP критерии выполнены.
- Integration/E2E smoke контур стабильно проходит.
- Go/No-Go пакет подтвержден релизными отчетами.

---

## 6. Рекомендуемый календарный ритм

- **Неделя 1:** Этап 0-1 и старт Этапа 2.
- **Неделя 2:** завершение Этапа 2, старт Этапа 3.
- **Неделя 3:** Этап 3-4.
- **Неделя 4:** Этап 5-6 и финальный go/no-go.

Оценка: при текущем состоянии проекта достижение 100% по Фазе 1 реалистично в диапазоне 3-4 недель при фиксированном scope.
