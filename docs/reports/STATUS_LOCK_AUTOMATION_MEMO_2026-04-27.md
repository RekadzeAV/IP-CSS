# Status Lock Automation Memo (2026-04-27)

## Purpose

Provide a one-page operator memo for the new status lock contour introduced during the current automation wave.

## Active lock documents

- Server/status lock: `docs/status/STATUS_LOCK_2026-04-27.md`
- Video gate lock: `docs/status/VIDEO_GATE_LOCK_2026-04-27.md`

## Where to update first

1. `docs/status/PROJECT_STATUS.md` (main canonical status narrative)
2. Lock snapshots:
   - `docs/status/STATUS_LOCK_2026-04-27.md`
   - `docs/status/VIDEO_GATE_LOCK_2026-04-27.md`
3. Linked status views:
   - `docs/status/PROJECT_STATUS_PHASES.md`
   - `docs/status/CURRENT_STATUS.md`
4. Policy/index surfaces:
   - `docs/status/SOURCE_OF_TRUTH.md`
   - `docs/WORKING_DOCUMENTS_INDEX.md`
   - `docs/HIGH_PRIORITY_ACTIVE_DOCS.md`
5. Changelog/backlog:
   - `docs/TODO.md`

## Current locked gate snapshot

- Server `1.5.5`: complete (Redis/rate-limit guardrails fixed in docs and tests)
- Server `1.5.6`: in progress (staging cutover/rollback gate)
- Server `1.5.7`: deferred to Enterprise `4.3.3` (not an MVP blocker)
- Video gate:
  - strict: `NO-GO`
  - profile-aware: `GO`
  - runtime decision: `GO`
  - final: `NO-GO` until field evidence is complete

## Decision rule

If legacy/historical bullets conflict with current-wave lock docs, the lock docs are operationally authoritative for active execution and release synchronization.
