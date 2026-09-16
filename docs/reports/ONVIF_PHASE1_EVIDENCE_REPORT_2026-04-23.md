# ONVIF Phase 1 Evidence Report

- Generated at: 2026-04-23 23:50:09
- Overall status: **PARTIAL**

## Scope

- 1.4.3 ONVIF camera verification via automated scripts
- 1.4.1 unit coverage for mapping and subscription lifecycle
- short UI real-time regression pass

## Evidence Artifacts

- ONVIF acceptance json: .\diagnostics\onvif-events\phase1-onvif-acceptance-20260423-234056.json
- ONVIF resilience summary: .\diagnostics\onvif-events\onvif-resilience-20260423-235004-summary.md
- ONVIF phase1 json: <not found>
- ONVIF phase2 json: <not found>

## Status Matrix

| Item | Status | Evidence |
|------|--------|----------|
| 1.4.3 automated ONVIF verification | NOT_RUN | scripts: onvif-events-api-verification.ps1 (WS/latency), onvif-events-resilience-verification.ps1 |
| 1.4.1 unit tests (mapping+lifecycle) | PASS | OnvifEventMapperTest, OnvifEventSubscriptionServiceTest, CameraEventMonitoringServiceJvmTest |
| UI real-time regression (short) | PASS | eventsSlice.test.ts + useWebSocket.test.tsx |

## Commands Baseline

- .\scripts\onvif-events-api-verification.ps1 -AdminPassword <pwd> -EnableWebSocketCheck -PollIntervalMs 5000 -PullTimeoutMs 500 -LatencyBufferMs 10000
- .\scripts\onvif-events-resilience-verification.ps1 -AdminPassword <pwd> -EnableWebSocketCheck
- ./gradlew :server:api:test --tests "com.company.ipcamera.server.service.OnvifEventMapperTest" --tests "com.company.ipcamera.server.service.OnvifEventSubscriptionServiceTest" --tests "com.company.ipcamera.server.service.CameraEventMonitoringServiceJvmTest"
- cd server/web; npm test -- --runInBand src/store/slices/eventsSlice.test.ts src/hooks/useWebSocket.test.tsx

## ONVIF Diagnostics

- WebSocket status (phase1/phase2): NOT_RUN / NOT_RUN
- Latency status (phase1/phase2): NOT_MEASURED / NOT_MEASURED
- Gate policy requireOnvifEvidence: False

## Notes

- run-phase1-onvif-acceptance: From resilience summary: onvif-resilience-20260423-235004-summary.md; ws=(NOT_RUN,NOT_RUN), latency=(NOT_MEASURED,NOT_MEASURED)

## Verdict

- PASS: all three statuses are PASS and diagnostics artifacts are present.
- PARTIAL: at least one block is PARTIAL/NOT_RUN or artifacts are missing.
- FAIL: at least one block is FAIL.
