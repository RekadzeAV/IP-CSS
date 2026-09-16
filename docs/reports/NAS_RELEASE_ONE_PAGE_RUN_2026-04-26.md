# NAS Release One-Page Run

**Release:** Alfa-0.1.1  
**Use case:** Fast execution by on-duty release engineer

## 10-Step Procedure

1. Open master index: `docs/reports/NAS_RELEASE_MASTER_INDEX_2026-04-26.md`.
2. Build NAS artifacts:
   - `:platforms:nas-x86_64:build:buildAllNasPackages`
   - `:platforms:nas-arm:build:buildAllNasPackages`
3. Confirm artifacts exist in `build/` (`.spk`, `.qpkg`, `.apk`, `truenas-*`).
4. Confirm checksum files exist in `build/checksums/*.sha256`.
5. Run local smoke precheck (`scripts/test-nas-build.ps1`).
6. Execute S1-S6 on target real NAS devices (see runbook).
7. Fill platform reports (Synology/QNAP/Asustor/TrueNAS).
8. Update GO/NO-GO aggregator with final platform outcomes.
9. Complete publish checklist and publish artifacts + checksums.
10. Finalize executive summary and collect sign-off in handoff packet.

## Stop/Block Conditions

- Any S1-S6 FAIL on a required platform.
- Any checksum mismatch.
- Missing rollback path.

## Completion Criteria

- Aggregator final decision is approved.
- Publish checklist fully complete.
- Artifacts published and download-verified.

## Fast links

- Runbook: `docs/reports/NAS_PLATFORM_SMOKE_RUNBOOK_2026-04-26.md`
- Aggregator: `docs/reports/NAS_GO_NO_GO_AGGREGATOR_2026-04-26.md`
- Publish checklist: `docs/reports/NAS_RELEASE_PUBLISH_CHECKLIST_2026-04-26.md`
- Handoff packet: `docs/reports/NAS_RELEASE_HANDOFF_PACKET_2026-04-26.md`
