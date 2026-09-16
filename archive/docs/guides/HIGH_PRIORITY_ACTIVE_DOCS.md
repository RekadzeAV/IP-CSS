# High-Priority Active Docs

**Last updated:** 27 April 2026  
**Purpose:** define the minimal critical documentation set that must remain continuously synchronized with project reality.

## Priority set (P0/P1)

### P0 (authoritative control docs)

1. `docs/status/PROJECT_STATUS.md`
2. `docs/status/SOURCE_OF_TRUTH.md`
3. `docs/status/STATUS_LOCK_2026-04-27.md`
4. `docs/status/VIDEO_GATE_LOCK_2026-04-27.md`
5. `docs/WORKING_DOCUMENTS_INDEX.md`
6. `docs/README.md`
7. `DOCUMENTATION_INDEX.md`
8. `docs/TODO.md`

### P1 (execution-critical docs)

1. `docs/status/PROJECT_STATUS_PHASES.md`
2. `docs/planning/PHASE1_MVP_TO_100_PLAN.md`
3. `docs/planning/RELEASE_GO_NO_GO_CHECKLIST.md`
4. `docs/planning/LOCAL_RELEASE_BUILD_MASTER_PLAN.md`
5. `docs/automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md`
6. `docs/reports/DOCUMENTATION_STATUS_SYNC_2026-04-27.md`
7. `docs/reports/DOCS_ACTIVE_LINK_AUDIT_2026-04-27.md`
8. `docs/DEPRECATED_ROOT_STUBS.md`

## Update policy

- Any status/progress decision must first be reflected in `docs/status/PROJECT_STATUS.md`.
- Operational status decisions for active execution waves must also be reflected in lock snapshots (`docs/status/STATUS_LOCK_2026-04-27.md`, `docs/status/VIDEO_GATE_LOCK_2026-04-27.md`).
- Any docs restructuring must update `docs/WORKING_DOCUMENTS_INDEX.md` and `docs/README.md` in the same change.
- Any archival move must be registered in:
  - `docs/status/STATUS_ARCHIVE_REGISTRY_2026-04-27.md`
  - `docs/DEPRECATED_ROOT_STUBS.md` (if a root stub is introduced)
- Any link-impacting change should be followed by active-docs link audit and result refresh in `docs/reports/`.

## Target freshness SLA

- P0 docs: update within the same working session where the change occurred.
- P1 docs: update no later than the next status synchronization pass.
- If stale data is detected, add explicit `historical snapshot` note until synchronized.

## Cross-links

- `docs/status/SOURCE_OF_TRUTH.md`
- `docs/WORKING_DOCUMENTS_INDEX.md`
- `docs/reports/DOCS_ACTIVE_LINK_AUDIT_2026-04-27.md`
- `docs/DEPRECATED_ROOT_STUBS.md`
