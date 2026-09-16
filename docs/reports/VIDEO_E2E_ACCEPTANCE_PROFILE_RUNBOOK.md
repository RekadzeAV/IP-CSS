# Video E2E Acceptance Profile Runbook

Date: 2026-04-23  
Scope: `scripts/video-e2e-go-no-go.ps1` release/runtime decisions

## Purpose

This runbook defines how to use `acceptance profile` for video e2e gate in MVP-like environments with mixed camera compatibility.

It separates:

- strict release decision (all controls are mandatory),
- profile-aware release decision (selected controls can be optional),
- runtime decision (blocking runtime controls only).

## Files

- Gate script: `scripts/video-e2e-go-no-go.ps1`
- Local profile: `config/video-e2e-acceptance-profile.local.json`
- Example profile: `config/video-e2e-acceptance-profile.example.json`
- CI profile (no pre-seeded diagnostics; used by `mvp-automated-acceptance` when `GITHUB_ACTIONS` is set unless overridden): `config/video-e2e-acceptance-profile.mvp-ci.json`. Override with env **`MVP_VIDEO_ACCEPTANCE_PROFILE`** (absolute or repo-relative path accepted by the gate script).
- Latest gate report: `release-build/test/video-e2e-go-no-go-report.md`
- Release checklist reference: `docs/planning/RELEASE_GO_NO_GO_CHECKLIST.md`
- Release record reference: `docs/reports/RELEASE_GO_NO_GO_RECORD_2026-03-27.md`

## Control Groups

- Blocking runtime controls:
  - `1.8.A` Core network/runtime baseline
  - `1.8.B` Long-run matrix scenario pass rate
  - `1.8.C` Long-run checks pass rate
- Non-blocking-by-profile candidates:
  - `1.8.A1` PullPoint compatibility coverage
  - `1.8.D` Canonical weighted readiness floor
  - `1.8.E` Recording WS lifecycle acceptance evidence

## Current MVP Profile

Current local profile (`mvp-current-cameras`) marks as optional:

- `1.8.A1`
- `1.8.D`
- `1.8.E`

This is valid when:

- runtime decision is `GO`,
- camera pool has known PullPoint limitations by vendor/firmware,
- canonical weighted percentage is below target but critical runtime KPIs are green.
- WS lifecycle evidence may be staged/manual and not always available for each gate recomputation.

## Commands

1) Bootstrap local configs:

```powershell
pwsh -NoProfile -File "scripts/bootstrap-local-test-configs.ps1"
```

2) Run fresh network smoke + matrix:

```powershell
pwsh -NoProfile -File "scripts/video-e2e-go-no-go.ps1" -RunNetworkSmoke -RunLongRunMatrix
```

3) Recompute report without rerun (use latest evidence):

```powershell
pwsh -NoProfile -File "scripts/video-e2e-go-no-go.ps1"
```

## Decision Policy

- Use `Release decision (profile-aware)` for MVP deployment decision in current camera profile.
- Use strict `Release decision` for cross-vendor/general release readiness.
- Use `Runtime decision` as operational signal for current environment stability.

## When To Return Controls To Mandatory

Move controls from optional to mandatory in profile when:

1. PullPoint coverage reaches stable threshold in your target camera pool.
2. Canonical `1.8` readiness reaches team-agreed floor (for example, >= 70%).
3. You prepare a broader release not tied to current camera compatibility envelope.

Then update `optionalControlIds` in `config/video-e2e-acceptance-profile.local.json` and rerun the gate.

## Guardrails

- Do not mark `1.8.A`, `1.8.B`, `1.8.C` as optional.
- Keep profile exceptions explicit and minimal.
- Revalidate profile exceptions after camera firmware updates or topology changes.

## Recording WS Lifecycle Acceptance (1.8.1)

Use this targeted scenario to validate runtime delivery of recording lifecycle events over WebSocket (`recording_started`, `recording_paused`, `recording_resumed`, `recording_stopped`) on channel `recordings`.

### Preconditions

- Server is running with WebSocket endpoint `/api/v1/ws`.
- JWT token for user with at least `OPERATOR` role is available.
- At least one camera is reachable for recording lifecycle actions.

### Steps

1. Open WebSocket connection to `/api/v1/ws`.
2. Send `auth` message with JWT token.
3. Send `subscribe` for channel `recordings`.
4. Trigger recording lifecycle via REST:
   - `POST /api/v1/recordings/start`
   - `POST /api/v1/recordings/pause/{cameraId}`
   - `POST /api/v1/recordings/resume/{cameraId}`
   - `POST /api/v1/recordings/stop/{cameraId}`
5. Capture received WS frames and verify:
   - `channel == "recordings"`
   - event `type` matches lifecycle action
   - payload contains `recordingId`, `cameraId`, `timestamp` (for stop also `duration`/`endTime`)
6. Verify channel isolation:
   - session subscribed only to `cameras` must not receive `recordings` events.

### PASS / FAIL Criteria

- **PASS**: all 4 lifecycle events are observed in correct order for the same `recordingId`/`cameraId`, with valid payload fields; no leakage to non-subscribed channels.
- **FAIL**: missing events, wrong channel, mismatched IDs/order, malformed payload, or cross-channel leakage.

### Evidence

- Attach WS frame log/snippets to acceptance artifact.
- Reference automated baseline test:
  - `server/api/src/test/kotlin/com/company/ipcamera/server/websocket/WebSocketManagerBroadcastTest.kt`
- Optional evidence recorder command:

```powershell
.\scripts\recording-ws-lifecycle-acceptance-evidence.ps1 `
  -EnvironmentName "staging" `
  -CameraId "cam-1" `
  -RecordingId "rec-123" `
  -StartedEvent PASS `
  -PausedEvent PASS `
  -ResumedEvent PASS `
  -StoppedEvent PASS `
  -ChannelIsolation PASS `
  -WsLogPath "diagnostics\ws\recordings-staging.log" `
  -Notes "Manual WS acceptance run after lifecycle API checks"
```

- Semi-automated runner (login + ws subscribe + lifecycle REST + evidence):

```powershell
.\scripts\run-recording-ws-lifecycle-acceptance.ps1 `
  -ApiBase "http://localhost:8080" `
  -AdminUser "admin" `
  -AdminPassword "<pwd>" `
  -CameraId "cam-1" `
  -EventTimeoutSeconds 20 `
  -Notes "Semi-auto WS lifecycle acceptance run"
```

- One-command wrapper (WS lifecycle acceptance + profile-aware video gate + combined report):

```powershell
.\scripts\run-recording-ws-gate.ps1 `
  -ApiBase "http://localhost:8080" `
  -AdminUser "admin" `
  -AdminPassword "<pwd>" `
  -CameraId "cam-1" `
  -AcceptanceProfilePath "config\video-e2e-acceptance-profile.local.json"
```

- View latest wrapper status without rerun:

```powershell
.\scripts\show-latest-recording-ws-gate-status.ps1
.\scripts\show-latest-recording-ws-gate-status.ps1 -AsJson
```

