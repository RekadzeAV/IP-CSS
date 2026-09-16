# MVP Domain Contract Freeze

## Scope

This freeze defines the minimum stable domain/data contracts required for Phase 2 integration and release gating.

**Product scope (Phase 1 MVP boundaries, must-have vs out-of-scope):** [MVP_PHASE1_SCOPE_BOUNDARY.md](MVP_PHASE1_SCOPE_BOUNDARY.md) (including native iOS app explicitly out of the Phase 1 critical path unless the team re-baselines that decision).

## Frozen Contracts (MVP)

- `CameraRepository` and discovery/test connection flows.
- `RecordingRepository` lifecycle operations: start/stop/pause/resume/list/delete.
- `EventRepository` read/acknowledge flows used by UI and notifications.
- SQLDelight schema/migration baseline in `shared/src/commonMain/sqldelight`.

## Source Files

- `shared/src/commonMain/kotlin/com/company/ipcamera/shared/data/repository/CameraRepositoryImpl.kt`
- `shared/src/commonMain/kotlin/com/company/ipcamera/shared/data/repository/RecordingRepositoryImplSqlDelight.kt`
- `shared/src/commonMain/kotlin/com/company/ipcamera/shared/data/repository/EventRepositoryImplSqlDelight.kt`
- `shared/src/commonMain/sqldelight/com/company/ipcamera/shared/database/migrations/1.sqm`

## Freeze Rules

1. Breaking change to repository signatures requires:
   - compatibility note in PR,
   - migration/test update,
   - downstream Android/Web integration verification.
2. DB schema change requires migration file + rollback notes.
3. Contract changes are blocked within release window unless tagged as `release-blocker`.

## Exit Criteria

- Discovery -> connect -> playback -> event scenario validated end-to-end.
- Migration verification passes on clean and upgraded datasets.
- Android and web clients consume frozen contracts without emergency patches.
