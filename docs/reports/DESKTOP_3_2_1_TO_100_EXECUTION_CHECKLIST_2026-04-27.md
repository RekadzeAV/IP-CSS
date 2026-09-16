# Desktop 3.2.1 to 100% Execution Checklist (2026-04-27)

## Scope

- Target story: `3.2.1` (Desktop live view RTSP/HLS).
- Goal: move from baseline/conditional readiness to formal `100%` completion with evidence.
- Canonical phase status: `docs/status/PROJECT_STATUS_PHASES.md`.

## Current automatic execution snapshot

- Chain run: `scripts/phase3-continue-auto.ps1` -> `PASS` (`docs/reports/PHASE3_CONTINUE_AUTO_EXECUTION_STATUS_2026-04-27.md`).
- Desktop auto report: `PASS`, but decision remains `CONDITIONAL GO (Desktop compile/tests)` (`docs/reports/PHASE3_DESKTOP_AUTO_EXECUTION_STATUS_2026-04-27.md`).
- Latest desktop smoke evidence: `diagnostics/platform-smoke/desktop/desktop-video-event-smoke-20260427-120728.md`.
- Runtime detail from latest smoke: `runtimeLongRun = NOT_RUN` (no live playlist/runtime stream evidence in this auto pass).

## Detailed execution plan (to reach 100%)

### A. Runtime video path closure (critical)

- [ ] A1. Run runtime long-run with real playlist(s) for Desktop live path.
  - Command template:
    - `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/desktop-video-event-longrun-smoke.ps1 -BaseUrl "http://localhost:8080" -Username "admin" -Password "<pwd>" -DurationMinutes 20 -PlaylistUrls "<m3u8-url-1>","<m3u8-url-2>"`
  - Evidence:
    - `diagnostics/platform-smoke/desktop/desktop-video-event-smoke-*.md|.json`
    - `diagnostics/video-longrun/video-longrun-report-*.md`
  - DoD:
    - `runtimeLongRun = PASS` and no critical runtime errors.

- [ ] A2. Validate reconnect/recovery behavior under transient failures.
  - Validate drop/recover and stream restart scenarios.
  - DoD: no reconnect storm, no progressive degradation, recovery is stable.

- [ ] A3. Confirm RTSP native integration behavior on Desktop live path.
  - DoD: no blocker-level issues in native path on target runtime profile.

### B. LiveView performance acceptance (manual + evidence)

- [ ] B1. Execute `GRID_4` acceptance.
- [ ] B2. Execute `GRID_9` acceptance (including viewport leave/return).
- [ ] B3. Execute `GRID_16` acceptance (cold start + staggered connect behavior).
- [ ] B4. Validate CPU/RAM/FPS thresholds and stability drift.
  - Checklist source: `docs/status/DESKTOP_LIVEVIEW_PERF_MANUAL_CHECKLIST.md`.
  - DoD: all required rows marked `PASS` with captured metrics.

### C. Platform closure dependencies for story 3.2

- [ ] C1. `3.2.3` tray/autostart manual acceptance.
- [ ] C2. `3.2.4` ARM parity smoke.
  - DoD: story 3.2 has no `PENDING` platform blockers for acceptance.

### D. Status synchronization and release traceability

- [ ] D1. Update status lines in:
  - `docs/status/PROJECT_STATUS_PHASES.md` (`3.2.1` note + evidence references).
  - `docs/status/PROJECT_STATUS.md` (phase summary alignment).
- [ ] D2. Keep source-of-truth links aligned:
  - `docs/status/SOURCE_OF_TRUTH.md`
  - `docs/README.md`
- [ ] D3. Update final decision context only after runtime/manual evidence is attached.
  - Expected transition: `CONDITIONAL GO` -> `GO` for Desktop story scope.

## What is done automatically vs what still requires operator/runtime

- Done automatically now:
  - NAS precheck, Desktop compile/tests baseline, Analytics baseline (`PASS` chain).
- Not yet done automatically in this run:
  - Desktop runtime long-run with actual HLS playlists/active streams.
  - Manual perf acceptance matrix (`GRID_4/9/16`).
  - OS-level tray/autostart acceptance and ARM parity smoke.

## Final readiness rule for 3.2.1

`3.2.1` can be marked `✅` only when all are true:

1. Runtime long-run evidence exists and is `PASS`.
2. Perf checklist (`GRID_4/9/16`) is completed with `PASS`.
3. No critical RTSP native/runtime regressions remain.
4. Status documents are synchronized and reference concrete evidence paths.
