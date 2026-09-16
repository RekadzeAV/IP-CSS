# Mobile/Desktop 1.7 Closure Tasklist (2026-04-27)

## Goal

Close `1.7` for Android/Desktop with concrete runtime evidence and remove code-level blockers that prevent stable smoke acceptance.

## Execution mode

- Automatic by default for repository code/tasks.
- Manual only for hardware-dependent evidence (Android device, desktop long-run with real streams).

## Task list

| ID | Task | Status | Evidence / Output |
|---|---|---|---|
| 1.7-A1 | Replace fire-and-forget service calls with bind-safe execution queue in `ServiceManager` | DONE | `android/.../service/ServiceManager.kt` |
| 1.7-A2 | Implement effective pause/resume handling in `RecordingService` state and repository updates | DONE | `android/.../service/RecordingService.kt` |
| 1.7-A3 | Replace camera-availability stub with real RTSP/HTTP connectivity checks | DONE | `android/.../service/CameraMonitoringService.kt` |
| 1.7-A4 | Remove nav TODO by adding `RecordingPlaybackScreen` and wiring route | DONE | `android/.../ui/screens/recordings/RecordingPlaybackScreen.kt`, `AppNavigation.kt` |
| 1.7-A5 | Run Android compile validation for changed module | DONE | `:android:app:compileDebugKotlin` PASS |
| 1.7-A6 | Run lint diagnostics for changed files and fix regressions | DONE | `ReadLints` clean for changed files |
| 1.7-A7 | Harden Android smoke scripts for adb auto-discovery/bootstrap | DONE | `scripts/android-video-background-smoke.ps1`, `scripts/android-permissions-revoke-recover-smoke.ps1` |
| 1.7-A8 | Add one-command auto orchestrator for 1.7 closure baselines | DONE | `scripts/mobile-desktop-1-7-auto-closure.ps1`, report `mobile-desktop-1-7-auto-closure-20260427-200831.json` |
| 1.7-A9 | Lock external worktree changes outside 1.7 scope | DONE | `docs/reports/EXTERNAL_WORKTREE_STATE_LOCK_2026-04-27.md` |
| 1.7-F1 | Android runtime smoke on connected device (`1.7.2`/`1.7.5`) | PARTIAL (auto rerun) | `android-video-background-smoke-20260427-214027.md` (compile/assemble PASS, `adb devices: 0`) |
| 1.7-F2 | Android permissions/keystore revoke-recover smoke on device | PARTIAL (auto rerun) | `android-permissions-revoke-recover-smoke-20260427-214116.md` (no connected device/emulator) |
| 1.7-F3 | Desktop runtime long-run (`runtimeLongRun = PASS`) | DONE (auto rerun) | `desktop-video-event-smoke-20260427-234142.md` (`runtimeLongRun = PASS`), `video-longrun-report-20260427-234204.md` (`18/18 PASS`) |
| 1.7-F4 | Desktop performance matrix (`GRID_4/9/16`) | TODO (manual) | `docs/status/DESKTOP_LIVEVIEW_PERF_MANUAL_CHECKLIST.md` updated |
| 1.7-F5 | Desktop tray/autostart + ARM parity smoke | TODO (manual) | desktop smoke reports |
| 1.7-F6 | Sync final status docs after evidence (`PROJECT_STATUS*`, acceptance matrix) | DONE (auto sync) | `w4-mvp-platform-gate-20260427-235440.md`, `release-build/test/video-e2e-go-no-go-report.md`, `docs/reports/PHASE1_FINAL_ACCEPTANCE_MATRIX_2026-04-27.md` |

## Notes

- Items `1.7-F1`, `1.7-F2`, `1.7-F4`, `1.7-F5` cannot be fully auto-closed without physical/runtime environments.
- Code-level blockers in Android service/nav layer are addressed first to unblock runtime validation.
- Automatic smoke baselines for Android/Desktop are refreshed in this run and attached above.
- Strict PostgreSQL evidence gate is now valid, and W4 platform gate reruns are `SUCCESS`; remaining blockers are hardware/manual runtime checks.
