# Checklist реализации: 1.4.4 / 1.4.5 / 1.4.6

Дата старта: 2026-03-27
Статус: Completed

## 1.4.4 WebSocketClient

- [x] C1. Синхронизировать unit-тесты с актуальным API `WebSocketMessage` и `JsonObject`.
- [x] C2. Добавить/усилить E2E сценарий с реальным WebSocket endpoint (connect/auth/subscribe/reconnect).
- [x] C3. Добавить стресс-сценарий очереди + rate limiting (burst + backpressure).
- [x] C4. Зафиксировать единый процент готовности в статус-документах.

## 1.4.5 RtspClient

- [x] C5. Перевести интеграционные RTSP тесты в управляемый opt-in режим через env флаг (вместо полного отключения).
- [x] C6. Добавить smoke-check на длительный прогон (soak) с метриками стабильности.
- [x] C7. Зафиксировать критерии "готово" для native path и fallback path.

## 1.4.6 ONVIF Event service (PullPoint + mapping)

- [x] C8. Исправить обработку timeout в PullPoint polling (не логировать штатные timeout как warning).
- [x] C9. Добавить тест на поведение PullPoint polling при timeout/ошибках сети.
- [x] C10. Расширить validation matrix по камерам до 6/6 или документировать поддерживаемый профиль.
- [x] C11. Добавить интеграционный сценарий renew + pull + sync с проверкой маппинга в доменные события.

## Прогресс текущей сессии

- Выполнено: C1, C2, C3, C4, C5, C6, C7, C8, C9, C10, C11
- В работе далее: финальная валидация прогонов в чистом build-контуре

## Разблокировка build-контура (Этап 0)

- [x] Восстановлена компиляция `:server:api:compileKotlin` (27 Mar 2026).
- [x] Выполнить целевые тестовые прогоны (`:server:api:test` и сетевые интеграционные наборы) после стабилизации flaky/infra-зависимых сценариев (27 Mar 2026, `BUILD SUCCESSFUL`).
