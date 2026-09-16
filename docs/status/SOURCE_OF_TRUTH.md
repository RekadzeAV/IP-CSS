# Source of Truth for Project Status

**Last updated:** 27 April 2026

This document defines the canonical source for project status values.

## Canonical status document

- Main source: `docs/status/PROJECT_STATUS.md`
- Operational lock snapshot for server `1.5.x`: `docs/status/STATUS_LOCK_2026-04-27.md`.
- Operational lock snapshot for video gate `1.8`: `docs/status/VIDEO_GATE_LOCK_2026-04-27.md`.
- Active status set is listed in `docs/status/STATUS_ARCHIVE_REGISTRY_2026-04-27.md`.
- Documentation-wide synchronization anchor: `docs/reports/DOCUMENTATION_STATUS_SYNC_2026-04-27.md`.

## Rules

- Version, overall progress, and component progress must be synchronized with `PROJECT_STATUS.md`.
- For active wave execution, lock snapshots (`STATUS_LOCK_2026-04-27.md`, `VIDEO_GATE_LOCK_2026-04-27.md`) define authoritative operational decisions when legacy status bullets are ambiguous.
- If a specialized document (`RTSP_CLIENT.md`, `IMPLEMENTATION_STATUS.md`, etc.) differs from `PROJECT_STATUS.md`, `PROJECT_STATUS.md` is authoritative.
- Historical or archival reports must explicitly mark their data as historical snapshots.
- For NAS field validation consolidation, `docs/reports/NAS_FIELD_AGGREGATOR_2026-04-27.md` is the canonical field-evidence table and must be updated from platform reports (manually or via `scripts/nas-field-aggregate.ps1`).
- For NAS final release sync, run `scripts/nas-field-readiness-check.ps1` -> `scripts/nas-field-finalize.ps1` -> `scripts/sync-release-status-docs.ps1` (or one-command wrapper `scripts/nas-field-full-finalize.ps1`).
- For structured operator handoff, field outcomes can be provided as JSON and applied via `scripts/nas-field-apply-results.ps1` before finalization.
- Operator JSON handoff instructions are documented in `docs/reports/NAS_FIELD_JSON_HANDOFF_RUNBOOK_2026-04-27.md`.
- Recommended end-to-end command for JSON handoff is `scripts/nas-field-auto-orchestrator.ps1` (preflight -> apply -> finalize -> execution report).
- For Desktop story `3.2.1` completion tracking, use `docs/reports/DESKTOP_3_2_1_TO_100_EXECUTION_CHECKLIST_2026-04-27.md` as the execution checklist and keep evidence links synchronized with `PROJECT_STATUS.md` / `PROJECT_STATUS_PHASES.md`.

## Update workflow

1. Update `docs/status/PROJECT_STATUS.md`.
2. Update lock snapshots when decisions/gates change: `STATUS_LOCK_2026-04-27.md`, `VIDEO_GATE_LOCK_2026-04-27.md`.
3. Update linked status documents that reference changed numbers.
4. Add/update changelog entries when changes are significant.
5. When status docs become legacy snapshots, move them to `docs/archive/` and register them in `docs/status/STATUS_ARCHIVE_REGISTRY_2026-04-27.md`.
6. Run docs metadata audit (`docs/reports/DOCS_HEADER_AUDIT_2026-04-27.md`) and normalize high-priority docs first.

