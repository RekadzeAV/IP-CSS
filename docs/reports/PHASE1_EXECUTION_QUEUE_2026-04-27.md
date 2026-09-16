# PHASE1 Execution Queue (ordered)

Date: 2026-04-27  
Scope: decomposition provided by user, preserved order, one task = one question.

## Executive checklist

| Task ID | Task | Input | Output evidence | DoD | Execution mode | Status |
|---|---|---|---|---|---|---|
| 1.5.6 | Staging cutover PostgreSQL | `docs/planning/POSTGRESQL_STAGING_CUTOVER_AND_ROLLBACK_RUNBOOK.md`, staging env access | `docs/reports/POSTGRESQL_FINALIZATION_STAGING_REPORT_<date>.md` | cutover passed + ready health + DB smoke green | manual (env-bound) | in_progress (evidence scaffold generated) |
| 1.5.6 | Rollback rehearsal PostgreSQL | same as above + rollback artifact/snapshot | same report with rollback section, timing and result | rollback reproducible and validated | manual (env-bound) | in_progress (report scaffold ready) |
| 1.5.6 | DB smoke evidence after cutover/rollback | logs from cutover and rollback windows | smoke checklist in report + attached command outputs | all mandatory smoke points marked PASS | mixed (script + operator) | in_progress (scaffold + validator ready) |
| 1.8.4 | RTSP reconnect/drop recovery long-run | test streams profile, runtime scripts | `diagnostics/video-runtime/*`, long-run report | reconnect/drop scenarios stable in long-run | mixed | pending |
| 1.8.4 | RTSP field stability on real streams | real cameras and network profile | field report with timestamps/errors | no critical regressions on field streams | manual (field) | pending |
| 1.8.1 | Integration coverage for recording core | shared DB schema + repository layer | integration test file + green test run output | critical recording CRUD/filter/export flow covered | auto (repo) | done |
| 1.8.2 | HLS live long-run stability | HLS live scenario scripts | long-run diagnostics and summary | no fatal playback/runtime issues in target window | mixed | pending |
| 1.8.2 | HLS recordings long-run stability | recordings playback profile | long-run diagnostics and summary | no fatal regressions in recordings playback | mixed | pending |
| 1.8.3 | Screenshot via FFmpeg field validation | ffmpeg runtime + real streams | field validation report with samples | screenshot success and quality baseline confirmed | manual (field) | pending |
| 1.7.2 | Android video runtime path closure | Android runtime + video smoke script | android smoke report/log | mandatory runtime path pass | mixed | pending |
| 1.7.5 | Android background recording lifecycle | Android background smoke path | lifecycle smoke logs/report | start/background/stop lifecycle pass | mixed | pending |
| 1.7.5 | Android permissions/keystore smoke | keystore + permission scenarios | smoke evidence + policy checks | permissions and signing smoke pass | mixed | pending |
| 1.8.6 | Desktop long-run video validation | desktop runtime profile | desktop long-run report + logs | target long-run stability reached | mixed | pending |
| 1.9.3 | Field validation certificate pinning | staging/field HTTPS certs | security validation report | pinning checks pass in supported clients | manual (field) | pending |
| 1.9.3 | Field validation HTTPS boundary | HTTPS enforcement config | security validation report | plaintext boundary violations absent | manual (field) | pending |
| 1.9.5 | Staging validation encryption migration | staging DB with legacy rows | migration validation report | legacy plaintext migrated safely | mixed | pending |
| 1.9.6 | Audit critical security operations | security logging/audit hooks | audit checklist and sample logs | audit trail exists for critical ops | mixed | pending |
| 1.10.3 | Add DB/migration integration tests | `shared` test infrastructure | new/updated integration tests + run output | migration path and DB integrity covered | auto (repo) | done |
| 1.10.4 | Add minimal E2E critical scenarios | existing acceptance scripts/tasks | E2E smoke orchestration report | critical scenarios reproducible in one command | auto (repo) | done (baseline) |
| W4-5 | Final acceptance matrix | all previous evidence artifacts | consolidated matrix file | matrix complete and traceable | mixed | done (pre-final, NO-GO state fixed) |
| W4-5 | Final GO/NO-GO decision for Phase 1 | matrix + gate summaries | signed decision record in release checklist/report | formal GO/CONDITIONAL/NO-GO fixed | manual (release board) | pending |

## Notes for execution sequence

- Items marked `manual (field/env-bound)` require staging or physical devices/cameras and cannot be fully completed from repository-only automation.
- `auto (repo)` items can be implemented immediately in code/scripts and validated via CI/local runs.
- Auto baseline completed for `1.8.1`, `1.10.3`, `1.10.4`; remaining closure is mostly field/staging/hardware dependent.
- `1.5.6` now has script-level guardrails: report generator (`scripts/postgresql-staging-evidence-pack.ps1`) and strict validator (`scripts/postgresql-staging-evidence-validate.ps1`), integrated into W4 gate via `-RequireValidPostgresEvidence`.
