# Phase1 Final Acceptance Matrix (W4-5)

Date: 2026-04-27  
Scope: consolidated acceptance status for Phase 1 closeout (automated + manual/field gates).

## Matrix

| Task ID | Scope | Status | Evidence | Gate note |
|---|---|---|---|---|
| 1.5.6 | PostgreSQL staging cutover | PASS | `docs/reports/POSTGRESQL_FINALIZATION_STAGING_REPORT_2026-04-27.md`, `diagnostics/postgres-finalization/go-no-go-20260427-215739.md` | strict evidence validation is green, GO decision recorded with rollback evidence |
| 1.5.6 | PostgreSQL rollback rehearsal | PASS | `diagnostics/postgres-finalization/rollback-rehearsal-20260427-2157.md` | rollback rehearsal documented and accepted in go/no-go checklist |
| 1.5.6 | DB smoke evidence after cutover/rollback | PASS | `diagnostics/postgres-finalization/staging-smoke-report-20260427-215739.md` | staging smoke `11/11` PASS after cutover/rollback flow |
| 1.8.4 | RTSP reconnect/drop recovery long-run | NOT_RUN | `docs/reports/RTSP_FOCUS_AUTOMATION_STATUS_2026-04-27.md` | long-run/env execution still pending |
| 1.8.4 | RTSP field stability on real streams | NOT_RUN | field report required | hardware/field dependent |
| 1.8.1 | Recording core integration coverage | PASS | `shared/src/commonTest/kotlin/com/company/ipcamera/shared/data/repository/RecordingRepositorySqlDelightIntegrationTest.kt` | targeted integration tests green |
| 1.8.2 | HLS live long-run stability | NOT_RUN | `docs/reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md` | long-run execution pending |
| 1.8.2 | HLS recordings long-run stability | NOT_RUN | `docs/reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md` | long-run execution pending |
| 1.8.3 | Screenshot FFmpeg field validation | NOT_RUN | field report required | field dependent |
| 1.7.2 | Android video runtime path | NOT_RUN | `scripts/android-video-background-smoke.ps1` | device/runtime evidence needed |
| 1.7.5 | Android background recording lifecycle | NOT_RUN | `scripts/android-video-background-smoke.ps1` | device lifecycle evidence needed |
| 1.7.5 | Android permissions/keystore smoke | NOT_RUN | `scripts/android-permissions-revoke-recover-smoke.ps1` | device + keystore evidence needed |
| 1.8.6 | Desktop long-run video validation | PASS | `diagnostics/platform-smoke/desktop/desktop-video-event-smoke-20260427-234142.md`, `diagnostics/video-longrun/video-longrun-report-20260427-234204.md` | desktop long-run runtime gate passed (`runtimeLongRun = PASS`, `18/18`) |
| 1.9.3 | Certificate pinning field validation | NOT_RUN | `scripts/security-field-staging-validation.ps1` | staging/field evidence required |
| 1.9.3 | HTTPS boundary field validation | NOT_RUN | `scripts/security-field-staging-validation.ps1` | staging/field evidence required |
| 1.9.5 | Encryption migration staging validation | PARTIAL | `shared/src/commonTest/kotlin/com/company/ipcamera/shared/data/local/MigrationDataSafetyIntegrationTest.kt` | local integration pass, no staging evidence |
| 1.9.6 | Audit critical security operations | NOT_RUN | audit report required | manual audit closure pending |
| 1.10.3 | DB/migration integration tests | PASS | `MigrationManagerIntegrationTest.kt`, `MigrationDataSafetyIntegrationTest.kt` | green targeted suites |
| 1.10.4 | Minimal E2E critical scenarios | PASS (baseline) | `scripts/phase1-critical-e2e-smoke.ps1`, `diagnostics/phase1-e2e-smoke/phase1-critical-e2e-smoke-20260427-174947.md` | core steps pass, report/matrix generation is stable |

## W4-5 decision precheck

- Current aggregate readiness: **CONDITIONAL GO** (profile-aware automated release gate is green; manual/hardware gates remain open).
- Blocking reasons:
  - field/hardware-dependent gates are still `NOT_RUN` (`1.7-F1/F2`, `1.7-F4/F5`, security field checks);
- manual/field gates are not yet attached to this matrix.
- Non-blocking completed baseline:
  - `1.5.6`, `1.8.1`, `1.8.6`, `1.10.3`, `1.10.4` automated scope delivered and green.

## Immediate next actions

1. Run Android device evidence gates (`1.7-F1/F2`) on connected device/emulator and attach reports.
2. Execute manual desktop perf/tray/ARM checks (`1.7-F4/F5`) and attach artifacts.
3. Attach security field validation artifacts (`1.9.3`, `1.9.6`) and finalize release-board GO/NO-GO.

## 1.5.6 precheck commands

- In-progress validation (does not block while filling report):
  `.\scripts\postgresql-staging-evidence-validate.ps1 -ReportPath "docs/reports/POSTGRESQL_FINALIZATION_STAGING_REPORT_2026-04-27.md" -AllowInProgress`
- Final strict validation (must be green for closing 1.5.6):
  `.\scripts\postgresql-staging-evidence-validate.ps1 -ReportPath "docs/reports/POSTGRESQL_FINALIZATION_STAGING_REPORT_2026-04-27.md"`
- W4 gate with strict PostgreSQL evidence content check:
  `.\scripts\w4-mvp-platform-and-gate.ps1 -RunProfile custom -RequirePostgresEvidence -RequireValidPostgresEvidence`
