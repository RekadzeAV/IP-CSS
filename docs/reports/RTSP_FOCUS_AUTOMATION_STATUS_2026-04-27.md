# RTSP Focus Automation Status (2026-04-27)

## Scope

Automated implementation and verification for RTSP-focus tasks:

- `VIDEO-2`
- `VIDEO-3`
- `AI-RTSP-1`
- `QA-2`

## Completed

- `VIDEO-2`
  - `RtspClient` runtime diagnostics hardened:
    - `play()` failure now records runtime failure metrics.
    - reconnect and failure counters are tracked consistently.
    - frame/playing/disconnect timestamps are updated on active paths.
- `VIDEO-3`
  - `VideoStreamService` improved for production observability:
    - explicit `rtspConnectError` propagation on connect failures.
    - stream-start payload includes `analyticsFrameSource` and `rtspConnectError`.
    - `/api/v1/cameras/{id}/stream/status` exposes `rtspDiagnostics`.
- `AI-RTSP-1`
  - analytics fallback path for HLS-only mode:
    - when analytics is requested but RTSP frame source is unavailable, service starts analytics with `AnalyticsFrameInput.none()`.
    - runtime logs explicitly include `frameSource=NONE`.
- `QA-2`
  - Added/extended tests:
    - `RtspClientTest` diagnostics coverage for reconnect/fallback timestamps.
    - `HlsStreamRoutesIntegrationTest` coverage for stream status diagnostics payload.
    - `WebSocketManagerBroadcastTest` coverage for `stream_started` event payload fields (`analyticsFrameSource`, `rtspConnectError`).
  - Added testability hooks in websocket session manager:
    - `getSubscribersCount(channel)`
    - `clearAll()`
- Runtime gate automation fixes:
  - `scripts/video-runtime-platform-matrix.ps1`: fixed optional `cameraIds`/`playlistUrls` handling under strict mode.
  - `scripts/video-e2e-go-no-go.ps1`: fixed matrix evidence discovery to use latest run that actually contains `video-runtime-matrix-summary.md` (instead of failing on incomplete latest folder).

## Verification Runs

- `:core:network:test` -> `PASS`
- `:server:api:compileKotlin` -> `PASS`
- `:server:api:test --tests com.company.ipcamera.server.integration.HlsStreamRoutesIntegrationTest` -> `PASS`
- `:server:api:test --tests com.company.ipcamera.server.websocket.WebSocketManagerBroadcastTest` -> `PASS`
- `scripts/video-e2e-go-no-go.ps1` (acceptance profile `mvp-current-cameras`) -> strict `NO-GO`, profile-aware `GO`, runtime decision `GO` (`release-build/test/video-e2e-go-no-go-report.md`).

## Remaining for Full RTSP Readiness (non-automatable in local CI-only loop)

- Long-run runtime validation on real RTSP camera streams:
  - disconnect/reconnect under packet loss and camera reboot.
  - extended soak stability and memory/CPU behavior.
- Field evidence collection for release gate:
  - profile-aware runtime matrix on target camera models.
  - final GO/NO-GO evidence package.
- Canonical 1.8 weighted readiness remains below strict threshold:
  - current canonical value is 64% (control `1.8.D` is `CONDITIONAL` in strict decision mode).

## Conclusion

The code/test automation scope for RTSP-focus tasks is implemented and passing.
Remaining items are field/staging validation activities that require real hardware streams.

