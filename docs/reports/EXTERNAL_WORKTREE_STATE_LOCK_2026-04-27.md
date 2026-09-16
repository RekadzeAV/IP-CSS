# External Worktree State Lock (2026-04-27)

## Purpose

Lock the current non-`1.7` external changes in the working tree before continuing Mobile/Desktop closure tasks.

## Scope Split

### In scope (current 1.7 work)

- `android/app/src/main/java/com/company/ipcamera/android/service/CameraMonitoringService.kt`
- `android/app/src/main/java/com/company/ipcamera/android/service/RecordingService.kt`
- `android/app/src/main/java/com/company/ipcamera/android/service/ServiceManager.kt`
- `android/app/src/main/java/com/company/ipcamera/android/ui/navigation/AppNavigation.kt`
- `android/app/src/main/java/com/company/ipcamera/android/ui/screens/camera/CameraDetailScreen.kt`
- `android/app/src/main/java/com/company/ipcamera/android/ui/screens/recordings/RecordingPlaybackScreen.kt`
- `scripts/android-video-background-smoke.ps1`
- `scripts/android-permissions-revoke-recover-smoke.ps1`
- `scripts/mobile-desktop-1-7-auto-closure.ps1`
- `docs/reports/MOBILE_DESKTOP_1_7_CLOSURE_TASKLIST_2026-04-27.md`

### External / out of 1.7 scope (do not modify in this track)

- `data/postgres/global/pg_control`
- `data/postgres/postmaster.pid` (deleted)
- `data/redis/appendonlydir/appendonly.aof.1.base.rdb`
- `data/redis/appendonlydir/appendonly.aof.1.incr.aof`
- `data/redis/appendonlydir/appendonly.aof.manifest`
- `data/redis/dump.rdb`
- `.env.docker-manual`
- `docker-compose.manual.yml`
- `docs/reports/DOCKER_MANUAL_AUTH_WEB_CHECK_2026-04-27.md`
- `docs/reports/DOCKER_WEB_MANUAL_RUNBOOK_2026-04-27.md`
- `server/api/src/main/resources/logback.xml`

## Notes

- Accidental temporary output folder `-CompileAndTestOnly/` created by an earlier script call was cleaned up.
- This lock is informational and used to avoid touching unrelated runtime/manual docker changes while finishing `1.7`.
