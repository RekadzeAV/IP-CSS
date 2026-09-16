# Video E2E Go/No-Go Report

- Generated: 2026-04-27 23:54:33 +04:00
- Release decision: **NO-GO**
- Release decision (profile-aware): **GO**
- Acceptance profile: **mvp-ci-no-hardware-evidence**
- Optional controls in profile: **1.8.A, 1.8.A1, 1.8.B, 1.8.C, 1.8.D, 1.8.E**
- Runtime decision (1.8.A/1.8.B/1.8.C): **GO**
- Runtime readiness (signals): **91.4%**
- Canonical weighted readiness (1.8): **66%**

| ID | Control | State | Evidence |
|---|---|---|---|
| 1.8.A | Core network/runtime baseline (RTSP/HTTP/ONVIF/Media+Events) | PASS | rtsp=100%, http=100%, onvif=100%, mediaEvents=71.4% (core thresholds satisfied). Source: D:\GitHub-Ai\IP-CSS\diagnostics\network-smoke\20260427-234916\summary.json |
| 1.8.A1 | PullPoint compatibility coverage | CONDITIONAL | pullPointPct=0% indicates broad camera/vendor limitation or missing config. |
| 1.8.B | Long-run matrix scenario pass rate | PASS | 1/1 (100%) >= 80. Source: D:\GitHub-Ai\IP-CSS\diagnostics\video-longrun-matrix\20260427-235130\video-runtime-matrix-summary.md |
| 1.8.C | Long-run checks pass rate | PASS | 96/96 (100%) >= 95. |
| 1.8.E | Recording WS lifecycle acceptance evidence | CONDITIONAL | recordingWsOverall=PARTIAL (env=local). Source: D:\GitHub-Ai\IP-CSS\diagnostics\recording-ws-acceptance\recording-ws-lifecycle-acceptance-20260427-171507.json |
| 1.8.D | Canonical weighted readiness floor | CONDITIONAL | Canonical 1.8 readiness=66% (target >= 70% not met yet). |
