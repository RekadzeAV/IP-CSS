# NAS Release Master Index

**Release:** Alfa-0.1.1  
**Date:** 2026-04-26  
**Purpose:** Single entrypoint for NAS release workflow and evidence.

## 1) Quick Start Sequence

1. Run/build checks:
   - `:platforms:nas-x86_64:build:buildAllNasPackages`
   - `:platforms:nas-arm:build:buildAllNasPackages`
2. Verify artifacts and checksums in `build/` and `build/checksums/`.
3. Execute field smoke scenarios S1-S6 per platform.
4. Fill platform reports.
5. Update GO/NO-GO aggregator.
6. Finalize executive summary and publish checklist.

## 2) Core Control Documents

- Runbook: `docs/reports/NAS_PLATFORM_SMOKE_RUNBOOK_2026-04-26.md`
- GO/NO-GO aggregator: `docs/reports/NAS_GO_NO_GO_AGGREGATOR_2026-04-26.md`
- Executive summary template: `docs/reports/NAS_EXECUTIVE_SUMMARY_TEMPLATE_2026-04-26.md`
- Publish checklist: `docs/reports/NAS_RELEASE_PUBLISH_CHECKLIST_2026-04-26.md`
- Execution log: `docs/reports/NAS_EXECUTION_LOG_2026-04-26.md`

## 3) Platform Reports

### Templates

- Synology template: `docs/reports/NAS_SMOKE_REPORT_TEMPLATE_SYNOLOGY_2026-04-26.md`
- QNAP template: `docs/reports/NAS_SMOKE_REPORT_TEMPLATE_QNAP_2026-04-26.md`
- Asustor template: `docs/reports/NAS_SMOKE_REPORT_TEMPLATE_ASUSTOR_2026-04-26.md`
- TrueNAS template: `docs/reports/NAS_SMOKE_REPORT_TEMPLATE_TRUENAS_2026-04-26.md`

### Preliminary (auto-execution)

- Synology preliminary: `docs/reports/NAS_SMOKE_REPORT_SYNOLOGY_2026-04-26_PRELIMINARY.md`
- QNAP preliminary: `docs/reports/NAS_SMOKE_REPORT_QNAP_2026-04-26_PRELIMINARY.md`
- Asustor preliminary: `docs/reports/NAS_SMOKE_REPORT_ASUSTOR_2026-04-26_PRELIMINARY.md`
- TrueNAS preliminary: `docs/reports/NAS_SMOKE_REPORT_TRUENAS_2026-04-26_PRELIMINARY.md`

## 4) Artifacts and Integrity

- NAS artifacts directory: `build/`
- Checksums directory: `build/checksums/`
- Expected package families:
  - Synology: `.spk`
  - QNAP: `.qpkg`
  - Asustor: `.apk`
  - TrueNAS: bundle directory `truenas-*`

## 5) Current Program Status

- Local automation status: `PRECHECK PASS`
- Current release state: `NO-GO (PROGRAM GATE, PRELIMINARY PACKAGING ONLY)`
- Remaining gates:
  - real-device field validation for S2-S6 and final approval,
  - runtime evidence refresh for critical E2E/matrix gates (`http://localhost:8080` was unavailable in latest run).

## 6) Release Metadata Pack

- Synology release notes: `docs/reports/NAS_RELEASE_NOTES_SYNOLOGY_2026-04-26.md`
- QNAP release notes: `docs/reports/NAS_RELEASE_NOTES_QNAP_2026-04-26.md`
- Asustor release notes: `docs/reports/NAS_RELEASE_NOTES_ASUSTOR_2026-04-26.md`
- TrueNAS release notes: `docs/reports/NAS_RELEASE_NOTES_TRUENAS_2026-04-26.md`
- Known limitations and issues: `docs/reports/NAS_KNOWN_LIMITATIONS_AND_ISSUES_2026-04-26.md`
- Rollback plan template: `docs/reports/NAS_ROLLBACK_PLAN_TEMPLATE_2026-04-26.md`

## 7) Final Handoff

- Handoff packet (final SOP): `docs/reports/NAS_RELEASE_HANDOFF_PACKET_2026-04-26.md`
- One-page run (on-duty quick path): `docs/reports/NAS_RELEASE_ONE_PAGE_RUN_2026-04-26.md`

## 8) Automated Phase 3 Runner

- PowerShell orchestrator: `scripts/phase3-auto-execution.ps1`
- Purpose: contract validation + NAS precheck execution + status report generation.
- Example:
  - `.\scripts\phase3-auto-execution.ps1 -Version Alfa-0.1.1 -Packages "synology,qnap,asustor,truenas" -Architectures "x86_64,arm64"`
  - `.\scripts\phase3-auto-execution.ps1 -Version Alfa-0.1.1 -RunBuild`

## 9) Phase 3 Finalization Route (Operator)

- Full automatic status: `docs/reports/PHASE3_FULL_AUTO_EXECUTION_STATUS_2026-04-27.md`
- Operator handoff: `docs/reports/PHASE3_OPERATOR_HANDOFF_2026-04-27.md`
- One-command field pack generator: `scripts/phase3-field-validation-pack.ps1`
- Field decision file: `docs/reports/NAS_FIELD_AGGREGATOR_2026-04-27.md`
