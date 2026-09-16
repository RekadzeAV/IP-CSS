# 1.4 Сетевой слой: backlog автоматизации

Дата: 2026-04-27  
Область: `1.4.1` - `1.4.7`

## Цель

Свести ручные проверки сетевого слоя к повторяемым автоматическим шагам с явными env-флагами и артефактами.

## Список задач (автоматизируемый контур)

- [x] A1. Добавить opt-in live integration тест для `WebSocketClient` в `desktopTest`.
- [x] A2. Добавить единый PowerShell orchestration-скрипт для автопрогона 1.4 (`run-network-layer-automation.ps1`).
- [x] A3. Добавить в CI отдельный job/step для opt-in 1.4 live-checks (RTSP/WS) с безопасными secret/env.
- [x] A4. Добавить machine-readable сводку прогона (json/md) в `diagnostics/network-layer-automation`.
- [x] A5. Добавить opt-in WebSocket сценарий `connect/auth/subscribe/reconnect` с тестовым endpoint.
- [x] A6. Расширить RTSP soak-автоматизацию: пороги по `RtspRuntimeDiagnostics` + стабильные критерии PASS/FAIL.
- [x] A7. Авто-консолидация статусов 1.4 (чтобы legacy-доки не конфликтовали с актуальными).
- [x] A8. Добавить в CI opt-in job агрегированного решения `1.4` (`network-layer-aggregate-decision`) в strict-режиме + artifact с `json/md`.

## Что уже реализовано в этой итерации

- `core/network/src/desktopTest/kotlin/com/company/ipcamera/core/network/WebSocketClientLiveIntegrationDesktopTest.kt`
- `scripts/run-network-layer-automation.ps1`
- `scripts/sync-network-layer-1-4-status.ps1`
- `scripts/check-network-layer-ci-prerequisites.ps1`
- `scripts/show-latest-network-layer-1-4-decision.ps1`
- `scripts/run-network-layer-1-4-local-smoke.ps1`
- `diagnostics/network-layer-automation/<run-id>/network-layer-automation-summary.json`
- `diagnostics/network-layer-automation/<run-id>/network-layer-automation-summary.md`
- `docs/status/NETWORK_LAYER_1_4_STATUS_SYNC.md`
- `docs/status/NETWORK_LAYER_STATUS_SYNC_RUNBOOK.md`
- CI opt-in hardening:
  - `network-layer-live-checks` (var `ENABLE_NETWORK_LAYER_LIVE_CHECKS`)
  - `network-layer-status-sync-audit` (var `ENABLE_NETWORK_LAYER_STATUS_SYNC_AUDIT`, strict mode)
  - `network-layer-aggregate-decision` (var `ENABLE_NETWORK_LAYER_AGGREGATE_DECISION`, strict mode)

## Текущий статус (зафиксировано)

- Каноническая сводка operational baseline также закреплена в `docs/status/PROJECT_STATUS_PHASES.md` (раздел `1.4`).
- Автоматизационный контур `A1-A8` закрыт (`[x]`).
- Legacy-конфликт по `1.4` устранён: strict sync-аудит `.\scripts\sync-network-layer-1-4-status.ps1 -FailOnLegacyConflicts` проходит с exit `0`.
- Локальный preflight поддерживает два режима:
  - strict (по умолчанию): missing env/secrets => `FAIL`, exit `1`;
  - local smoke (`-LocalSmoke`): missing env/secrets => `WARN`, exit `0`.
- Агрегатор `.\scripts\show-latest-network-layer-1-4-decision.ps1` учитывает `WARN` из local-smoke как информирующий сигнал без понижения решения до `CONDITIONAL`.
- CI aggregate-gate запускается только при совместном включении:
  - `ENABLE_NETWORK_LAYER_AGGREGATE_DECISION=true`
  - `ENABLE_NETWORK_LAYER_LIVE_CHECKS=true`
  - `ENABLE_NETWORK_LAYER_STATUS_SYNC_AUDIT=true`

## Как запустить

```powershell
.\scripts\run-network-layer-automation.ps1
```

Синхронизация статуса 1.4 как отдельный шаг:

```powershell
.\scripts\sync-network-layer-1-4-status.ps1
```

Для opt-in RTSP/WS:

```powershell
$env:ENABLE_RTSP_INTEGRATION_TESTS = "true"
$env:TEST_RTSP_URL = "rtsp://<host>:554/stream"
$env:ENABLE_WEBSOCKET_LIVE_TESTS = "true"
$env:TEST_WEBSOCKET_URL = "wss://<endpoint>"
.\scripts\run-network-layer-automation.ps1 -RunRtspIntegration -RunWebSocketLive
```

Пороги RTSP soak можно задавать параметрами orchestration-скрипта:

```powershell
.\scripts\run-network-layer-automation.ps1 `
  -RunRtspIntegration `
  -RtspSoakDurationSec 120 `
  -RtspSoakMaxConnectFailures 0 `
  -RtspSoakMaxConsecutiveFailures 0 `
  -RtspSoakMaxReconnectFailures 0 `
  -RtspSoakMaxFrameSilenceMs 15000
```

Быстрый локальный статус-проход (без CI secrets):

```powershell
.\scripts\run-network-layer-1-4-local-smoke.ps1
```
