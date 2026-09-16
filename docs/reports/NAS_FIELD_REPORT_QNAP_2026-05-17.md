# NAS Field Validation Report - QNAP QTS

- Date: 2026-05-17
- Tester: AI-Automated-Execution
- Version: Alfa-0.1.1
- Platform: QNAP QTS
- Device model: [model]
- Artifact: build/ip-css-Alfa-0.1.1-qnap-[arch].qpkg

## Preflight

- Checksum verified: PASS|FAIL
- Java/runtime requirements: PASS|FAIL
- Free disk >= 2GB: PASS|FAIL
- Ports 8080/8081 available: PASS|FAIL

## Scenarios

- S1 Fresh install: PASS|FAIL
- S2 Basic health: PASS|FAIL
- S3 Restart: PASS|FAIL
- S4 Reboot persistence: PASS|FAIL
- S5 Upgrade: PASS|FAIL
- S6 Uninstall: PASS|FAIL

## Evidence

- Web UI (http://[NAS-IP]:8080): PASS|FAIL
- API health (http://[NAS-IP]:8081/health): PASS|FAIL
- Logs path: [path]

## Issues

- Issue 1: [description]
- Issue 2: [description]

## Decision

- Result: GO|CONDITIONAL GO|NO-GO
- Notes: [summary]
