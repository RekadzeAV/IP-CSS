# Docker Pre-Release Gate Report

- Generated at: 2026-04-27 23:44:00 +04:00
- Base URL: `http://localhost:8080`
- Overall decision: **NO-GO**

## Checks

| Check | Status | Notes |
|---|---|---|
| Auth/Discovery Smoke | FAIL | server-auth-discovery-smoke failed with exit code 1 |
| Metrics Guard | FAIL | server-metrics-guard failed with exit code 1 |

## Action required

- Fix failed checks and rerun `scripts/server-pre-release-gate.ps1`.
