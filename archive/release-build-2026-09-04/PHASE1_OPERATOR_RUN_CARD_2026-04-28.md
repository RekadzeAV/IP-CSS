# Phase 1 Operator Run Card (2026-04-28)

Purpose: close remaining manual/hardware gates and unblock final GO decision.

## 1) Android device gates

1. Connect device/emulator and confirm `adb devices` shows at least one target.
2. Run:
   - `scripts/android-video-background-smoke.ps1 -RunInstallIfDevicePresent`
   - `scripts/android-permissions-revoke-recover-smoke.ps1 -FailIfNoDevice`
3. Save generated reports under `diagnostics/platform-smoke/android/`.

## 2) Desktop manual gates

1. Execute manual performance matrix (`GRID_4/9/16`) per checklist:
   - `docs/status/DESKTOP_LIVEVIEW_PERF_MANUAL_CHECKLIST.md`
2. Execute tray/autostart + ARM parity smoke.
3. Store reports in `diagnostics/platform-smoke/desktop/` (or agreed report path).

## 3) Security field/manual gates

1. Run/record certificate pinning field validation (`1.9.3`).
2. Run/record HTTPS boundary field validation (`1.9.3`).
3. Attach audit critical operations evidence (`1.9.6`).

## 4) Evidence sync (required)

1. Update:
   - `docs/reports/PHASE1_FINAL_ACCEPTANCE_MATRIX_2026-04-27.md`
   - `docs/reports/MOBILE_DESKTOP_1_7_CLOSURE_TASKLIST_2026-04-27.md`
2. Ensure each remaining gate has:
   - artifact path,
   - explicit PASS/FAIL state,
   - short gate note.

## 5) Final decision

1. Re-run summary review using:
   - `release-build/release/PHASE1_RELEASE_BOARD_SUMMARY_2026-04-28.md`
   - `release-build/release/PHASE1_RELEASE_ARTIFACT_INDEX_2026-04-28.md`
2. Release board issues final GO/CONDITIONAL/NO-GO record.
