# ONVIF Follow-up Backlog (1 problem = 1 task)

- **P0** `Real-camera WebSocket e2e: event without refresh`
  - **Problem:** нужно зафиксировать, что событие с реальной камеры приходит в WS-канал без ручного refresh.
  - **Task/DoD:** `scripts/onvif-events-api-verification.ps1` запускается с `-EnableWebSocketCheck`, в JSON-артефакте `websocketStatus=PASS` и `websocketEventReceived=true`.

- **P0** `Latency gate relative to poll interval`
  - **Problem:** в приёмке отсутствует формальный latency threshold.
  - **Task/DoD:** JSON-артефакт содержит `latencyMs`, `latencyThresholdMs`, `latencyStatus`; pipeline сравнивает и фиксирует PASS/FAIL.

- **P1** `Mapper payload-priority edge-case`
  - **Problem:** конфликт источников (topic vs payload fields) не был покрыт отдельным тестом.
  - **Task/DoD:** добавить unit-тест, подтверждающий приоритет topic.

- **P1** `Mapper missing payload fields`
  - **Problem:** поведение при пустых/missing полях не было выделено отдельно.
  - **Task/DoD:** отдельный unit-тест подтверждает safe fallback и отсутствие падений.

- **P1** `Mapper invalid severity hints`
  - **Problem:** невалидные severity hints из payload не проверялись отдельно.
  - **Task/DoD:** unit-тест подтверждает, что маппинг severity определяется валидными правилами по topic.

- **P1** `Lifecycle retry after transient pull failure`
  - **Problem:** сценарий transient failure -> recovery не был выделен отдельным lifecycle test.
  - **Task/DoD:** тест демонстрирует повтор pull после временной ошибки и последующую генерацию доменного события.

- **P1** `Lifecycle ensureMonitoring idempotency`
  - **Problem:** после отката зависающего теста не было отдельной проверки идемпотентности.
  - **Task/DoD:** repeated `ensureMonitoring()` не создаёт дубликаты подписок.

- **P1** `Lifecycle race add/remove camera`
  - **Problem:** нет отдельного сценария конкурентных subscribe/unsubscribe.
  - **Task/DoD:** тест на конкурентный add/remove не падает и оставляет систему в валидном состоянии.

- **P1** `UI regression for realtime channel state`
  - **Problem:** не было отдельного теста индикации disconnect/reconnect.
  - **Task/DoD:** hook test проверяет диспатч `websocket/setConnected false/true`.

- **P1** `UI regression for malformed realtime payload`
  - **Problem:** не было отдельного теста устойчивости к пустому/частично невалидному payload.
  - **Task/DoD:** hook test подтверждает, что такие сообщения игнорируются и не ломают store.
