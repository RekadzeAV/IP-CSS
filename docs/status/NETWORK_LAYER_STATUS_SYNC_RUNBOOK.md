# Runbook: Network Layer 1.4 Status Sync

Дата: 2026-04-27  
Область: автоматизация консистентности статусов `1.4.*`

## Цель

Обеспечить единый источник истины для раздела `1.4 Сетевой слой` и автоматическое выявление конфликтов со старыми документами.

## Канонический источник

- `docs/status/PROJECT_STATUS_PHASES.md` (таблица `1.4.1`-`1.4.7`)

## Скрипт синхронизации

- `scripts/sync-network-layer-1-4-status.ps1`

Скрипт:
- читает каноническую таблицу `1.4.*` из `PROJECT_STATUS_PHASES.md`;
- обновляет snapshot `docs/status/NETWORK_LAYER_1_4_STATUS_SYNC.md`;
- выполняет legacy audit по известным документам;
- различает актуальные legacy-конфликты и исторические формулировки (исторические сигналы пишет в `ignoredSignals`);
- добавляет метрику `ignoredSignalCount` в JSON/snapshot для мониторинга шумовых исторических сигналов;
- пишет machine-readable отчёт в `diagnostics/network-layer-status-sync/<run-id>/`.

## Режимы запуска

### Базовый (локально, без падения на legacy-конфликтах)

```powershell
.\scripts\sync-network-layer-1-4-status.ps1
```

### Строгий (gate, падение при конфликтах)

```powershell
.\scripts\sync-network-layer-1-4-status.ps1 -FailOnLegacyConflicts
```

## Коды выхода

- `0` — sync выполнен (конфликтов нет, либо strict-режим не включён)
- `2` — найден конфликт legacy-доков при `-FailOnLegacyConflicts`

## CI интеграция

### Job: live network checks (opt-in)

- Job: `network-layer-live-checks`
- Workflow: `.github/workflows/ci.yml`
- Включение: Repository Variable `ENABLE_NETWORK_LAYER_LIVE_CHECKS=true`
- Preflight step: `.\scripts\check-network-layer-ci-prerequisites.ps1 -RequireLiveChecks`
- Требуемые secrets:
  - `TEST_RTSP_URL`
  - `TEST_RTSP_USERNAME`
  - `TEST_RTSP_PASSWORD`
  - `TEST_WEBSOCKET_URL`
  - `TEST_WEBSOCKET_AUTH_TOKEN`
  - `TEST_WEBSOCKET_SUBSCRIBE_CHANNEL`

### Job: strict status sync audit (opt-in)

- Job: `network-layer-status-sync-audit`
- Workflow: `.github/workflows/ci.yml`
- Включение: Repository Variable `ENABLE_NETWORK_LAYER_STATUS_SYNC_AUDIT=true`
- Preflight step: `.\scripts\check-network-layer-ci-prerequisites.ps1 -RequireStatusSyncAudit`
- Команда: `.\scripts\sync-network-layer-1-4-status.ps1 -FailOnLegacyConflicts`

### Job: aggregate decision (opt-in)

- Job: `network-layer-aggregate-decision`
- Workflow: `.github/workflows/ci.yml`
- Включение: Repository Variable `ENABLE_NETWORK_LAYER_AGGREGATE_DECISION=true`
- Условие запуска: aggregate gate выполняется только при одновременном включении `ENABLE_NETWORK_LAYER_LIVE_CHECKS=true` и `ENABLE_NETWORK_LAYER_STATUS_SYNC_AUDIT=true`.
- Команда: `.\scripts\show-latest-network-layer-1-4-decision.ps1 -WriteReport -FailOnNoGo -FailOnConditional`
- Особенность: job скачивает артефакты `network-layer-automation-report` и `network-layer-status-sync-audit` (если доступны), затем строит единое решение `GO/CONDITIONAL/NO_GO`.

## Артефакты

### Локально/CI sync-аудит

- `docs/status/NETWORK_LAYER_1_4_STATUS_SYNC.md`
- `diagnostics/network-layer-status-sync/<run-id>/network-layer-1-4-status-sync-report.json`

### CI upload

- Artifact: `network-layer-status-sync-audit`
- Artifact (live checks): `network-layer-automation-report` (includes prerequisites report)
- Artifact (aggregate decision): `network-layer-aggregate-decision-report` (`diagnostics/network-layer-decision/**`)

## Preflight перед включением CI gates

Для быстрой локальной проверки готовности env-переменных (эмуляция repo vars/secrets):

```powershell
.\scripts\check-network-layer-ci-prerequisites.ps1 `
  -RequireLiveChecks `
  -RequireStatusSyncAudit
```

Локальный smoke-режим (без падения при незаполненных CI secrets, статус `WARN`):

```powershell
.\scripts\check-network-layer-ci-prerequisites.ps1 `
  -RequireLiveChecks `
  -RequireStatusSyncAudit `
  -LocalSmoke
```

Machine-readable вывод:

```powershell
.\scripts\check-network-layer-ci-prerequisites.ps1 `
  -RequireLiveChecks `
  -RequireStatusSyncAudit `
  -LocalSmoke `
  -AsJson
```

Агрегированное решение по latest-артефактам 1.4:

```powershell
.\scripts\show-latest-network-layer-1-4-decision.ps1
```

Строгий режим для CI:

```powershell
.\scripts\show-latest-network-layer-1-4-decision.ps1 -WriteReport -FailOnNoGo -FailOnConditional
```

One-command локальная проверка (smoke, без CI secrets):

```powershell
.\scripts\run-network-layer-1-4-local-smoke.ps1
```

## Операционная политика

- Канонический статус `1.4` обновляется только через `PROJECT_STATUS_PHASES.md`.
- `NETWORK_LAYER_1_4_STATUS_SYNC.md` — вычисляемый snapshot.
- Legacy-доки разрешены для исторического контекста, но не для принятия решений по актуальному прогрессу.

## Текущий operational baseline (2026-04-27)

- strict status-sync audit: `PASS` (legacy conflict signals для `1.4` устранены);
- local preflight smoke: `WARN` при отсутствии CI secrets, но без fail (используется только локально);
- aggregate decision:
  - strict CI mode: `-FailOnNoGo -FailOnConditional`;
  - local smoke mode: допускает `WARN` из preflight как информационный сигнал;
  - в агрегаторе `show-latest-network-layer-1-4-decision.ps1` `ignoredSignalCount` из status-sync отражается как информационный сигнал и не понижает решение.
  - консольный вывод агрегатора также показывает `Conflicts` и `Ignored` (по данным status-sync metrics).
