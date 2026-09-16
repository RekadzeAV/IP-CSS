# 1.4 Network Layer Status Sync

Generated: 2026-04-27 17:56:40
Canonical source: docs/status/PROJECT_STATUS_PHASES.md

## Canonical 1.4 Rows

| ID | Task | Status | Note |
|----|------|--------|------|
| 1.4.1 | ApiClient (HTTP) | ✅ | Полностью |
| 1.4.2 | API сервисы и DTO | ✅ | Все |
| 1.4.3 | OnvifClient (Discovery, Device, Media, PTZ, Digest Auth) | 🟢 | ~92%, проверка на 6 камерах (ONVIF 200/6; Media+Events 4/6), WS-Discovery улучшен |
| 1.4.4 | WebSocketClient | 🟢 | ~92%, базовая функциональность + reconnect/rate limiting + burst/stress-тесты очереди + E2E сценарий connect/auth/subscribe/reconnect |
| 1.4.5 | RtspClient | 🟡 | ~76%, интеграция с NativeRtspClient + fallback; `disconnect()` всегда отменяет `receiveJob`; JVM-тесты fallback lifecycle (`RtspClientTest`); длительные реальные потоки — в полевой валидации |
| 1.4.6 | ONVIF Event service (PullPoint, маппинг в события IP-CSS) | 🟢 | ~92%, PullPoint/renew/pull/sync реализованы + покрыты сервисными тестами timeout/sync/pull/renew/mapping; по совместимости камер подтверждено 4/6 (MVP профиль задокументирован) |
| 1.4.7 | ONVIF Digest Authentication | 🟢 | ~95%, расширенная обработка nonce/stale/realm + кэш параметров |

## Legacy Conflict Audit

- No legacy conflict signals detected.

## Ignored Legacy Signals

- ignoredSignalCount: 0

- none

## Policy

- Canonical source for 1.4 is docs/status/PROJECT_STATUS_PHASES.md.
- Legacy planning/progress docs are informational and can diverge.
