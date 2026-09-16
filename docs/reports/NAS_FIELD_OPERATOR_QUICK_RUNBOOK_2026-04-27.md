# NAS Field Operator Quick Runbook (S2-S6)

- Date: 2026-04-27
- Scope: Synology DSM, QNAP QTS, Asustor ADM, TrueNAS CORE/SCALE
- Goal: convert `PENDING (field)` to `GO/CONDITIONAL GO/NO-GO` with reproducible evidence

## 1) Preflight (all platforms, 3-5 min)

- Verify artifact + checksum:
  - `build/` contains package for target platform.
  - `build/checksums/` contains matching checksum.
- Verify free disk and ports:
  - free space >= 2 GB
  - target ports `8080` and `8081` available
- Record in each platform report:
  - `docs/reports/NAS_FIELD_REPORT_<PLATFORM>_2026-04-27.md`

## 2) Scenario S2 Basic Health (5 min)

- Install/ensure service is running.
- Validate UI and API:
  - `http://<NAS-IP>:8080` opens successfully
  - `http://<NAS-IP>:8081/health` returns healthy response
- Mark `S2` as `PASS/FAIL` and capture:
  - timestamp
  - screenshot/UI proof (if available)
  - exact health response

## 3) Scenario S3 Restart (5-10 min)

- Restart package/service from NAS UI/CLI.
- Re-check:
  - UI at `:8080`
  - API health at `:8081/health`
- Mark `S3` + note startup time and errors (if any).

## 4) Scenario S4 Reboot Persistence (10-20 min)

- Reboot NAS.
- After boot, confirm service autostart (or expected manual start flow).
- Re-check UI/API endpoints.
- Mark `S4`; attach boot/persistence notes.

## 5) Scenario S5 Upgrade (10-20 min)

- Upgrade package from previous working build to current `Alfa-0.1.1`.
- Validate config/data preservation:
  - service starts
  - UI/API healthy
  - no critical migration errors
- Mark `S5`; include upgrade path used.

## 6) Scenario S6 Uninstall (5-10 min)

- Uninstall package.
- Confirm service/process removal and port release.
- Confirm expected data cleanup/retention behavior.
- Mark `S6` and log residual artifacts (if any).

## 7) Decision Rules (per platform)

- `GO`: S2-S6 all `PASS`, no critical issues.
- `CONDITIONAL GO`: minor non-blocking issues with workaround.
- `NO-GO`: any critical failure in health/restart/persistence/upgrade/uninstall.

## 8) Final Consolidation

- Update platform reports:
  - `NAS_FIELD_REPORT_SYNOLOGY_2026-04-27.md`
  - `NAS_FIELD_REPORT_QNAP_2026-04-27.md`
  - `NAS_FIELD_REPORT_ASUSTOR_2026-04-27.md`
  - `NAS_FIELD_REPORT_TRUENAS_2026-04-27.md`
- Update aggregator:
  - `docs/reports/NAS_FIELD_AGGREGATOR_2026-04-27.md`
  - fill S2-S6 cells and per-platform decision
  - set `Field validation` and `Final decision`

