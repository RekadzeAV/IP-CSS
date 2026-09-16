# Docker Server Task Closure (2026-04-27)

## Scope

Закрытие задач по серверному Docker стенду: discover/auth стабильность, снижение шумов, наблюдаемость, smoke и эксплуатационная документация.

## Task status matrix

| ID | Task | Status | Evidence |
|---|---|---|---|
| P0-1 | Fallback-discovery по known hosts | DONE | `CameraRoutes.kt`, `docker-compose.manual.yml`, `config/test-cameras.local.json` |
| P0-2 | Active probe для fallback | DONE | `CameraRoutes.kt` (`isTcpPortOpen`, RTSP/HTTP reachability) |
| P0-3 | Управление таймаутами discover | DONE | `DISCOVERY_CONNECT_TIMEOUT_MS`, `refresh=true`, env/property override |
| P0-4 | Исправить цепочку `login -> refresh -> ws-token` | DONE | `Application.kt`, `AuthRoutes.kt`, `WebSocketContext.tsx`, smoke PASS |
| P0-5 | Снизить 401-шум на защищенных эндпоинтах | DONE | `RequestLoggingMiddleware.kt` |
| P1-6 | Починить UPnP cleanup warning | DONE | `UPnPDiscovery.jvm.kt` |
| P1-7 | Пересмотреть slow-request логирование | DONE | `RequestLoggingMiddleware.kt` (`/api/v1/ws` исключен) |
| P1-8 | Добавить структурированные логи discovery | DONE | `discover_result ...` лог в `CameraRoutes.kt` |
| P1-9 | Метрики discover/auth | DONE | `ApiMetricsService.kt`, `GET /api/v1/health/metrics` |
| P2-10 | Интеграционные тесты для `cameras/discover` | DONE | `CameraDiscoverFallbackIntegrationTest.kt` |
| P2-11 | Тесты auth-flow | DONE | `CookieJwtAuthFlowIntegrationTest.kt` |
| P2-12 | E2E smoke сценарий | DONE | `scripts/server-auth-discovery-smoke.ps1` |
| P2-13 | Обновить runbook | DONE | `DOCKER_WEB_MANUAL_RUNBOOK_2026-04-27.md` |
| P2-14 | Операционная памятка для Docker-сети | DONE | `DOCKER_DISCOVERY_NETWORK_MEMO_2026-04-27.md` |
| P2-15 | Metrics guard для pre-release | DONE | `scripts/server-metrics-guard.ps1`, `GET /api/v1/health/metrics` |
| P2-16 | Unified pre-release gate | DONE | `scripts/server-pre-release-gate.ps1`, report `DOCKER_PRE_RELEASE_GATE_REPORT_*.md` |
| P2-17 | Nightly gate wrapper (retry/rotation) | DONE | `scripts/server-nightly-gate.ps1`, `DOCKER_PRE_RELEASE_GATE_LAST.md` |
| P2-18 | Webhook/Telegram notifications for nightly gate | DONE | `scripts/server-nightly-gate.ps1`, `DOCKER_PRE_RELEASE_GATE_LAST.json` |
| P2-19 | Export gate artifacts to release-build/test | DONE | `scripts/server-export-release-artifacts.ps1`, `release-build/test/server-docker-gate/` |
| P2-20 | Integrated export into nightly gate | DONE | `scripts/server-nightly-gate.ps1` (`-ExportToReleaseBuild`) |
| P2-21 | Rotation policy for exported artifacts | DONE | `scripts/server-export-release-artifacts.ps1` (`-KeepExportReports`, `-KeepExportRuns`) |
| P2-22 | Stable CI status artifact for server gate | DONE | `release-build/test/server-docker-gate/server-docker-gate-status.json` |

## Verification snapshots

- `./gradlew :server:api:test --tests "com.company.ipcamera.server.integration.HealthMetricsIntegrationTest" --tests "com.company.ipcamera.server.integration.CameraDiscoverFallbackIntegrationTest" --tests "com.company.ipcamera.server.middleware.CookieJwtAuthFlowIntegrationTest"` -> PASS
- `powershell -ExecutionPolicy Bypass -File scripts/server-auth-discovery-smoke.ps1` -> PASS
- `powershell -ExecutionPolicy Bypass -File scripts/server-metrics-guard.ps1` -> PASS
- `powershell -ExecutionPolicy Bypass -File scripts/server-pre-release-gate.ps1` -> GO (+ markdown report)
- `powershell -ExecutionPolicy Bypass -File scripts/server-nightly-gate.ps1 -Attempts 2` -> GO (+ last-summary + rotation)
- Latest gate artifact: `docs/reports/DOCKER_PRE_RELEASE_GATE_REPORT_2026-04-27_234947.md`
- Latest nightly summary: `docs/reports/DOCKER_PRE_RELEASE_GATE_LAST.md`
- Latest nightly JSON summary: `docs/reports/DOCKER_PRE_RELEASE_GATE_LAST.json` (includes webhook-friendly `text`)
- `GET /api/v1/health/metrics` -> counters increment (`discoverRequests`, `discoverFallbackUsed`, `auth*`)
- Exported release artifacts: `release-build/test/server-docker-gate/DOCKER_GATE_EXPORT_MANIFEST.md`

## Residual manual checks (non-blocking for code closure)

- Проверка полного browser UI сценария в `server/web`.
- Полевая проверка физического видео-потока/ONVIF capabilities в целевой сети.
