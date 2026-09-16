# NAS Smoke Report — QNAP (Preliminary)

**Platform:** QNAP QTS  
**Date:** 2026-04-26  
**Tester:** Automation precheck  
**Artifact:** `ip-css-Alfa-0.1.1-qnap-x86_64.qpkg`, `ip-css-Alfa-0.1.1-qnap-arm64.qpkg`, `ip-css-Alfa-0.1.1-qnap-armv7.qpkg`  
**Arch:** `x86_64|arm64|armv7`  
**Device model:** `FIELD TBD`

## Preflight

- Checksum verified: `PASS (local artifact checksum files generated)`
- Java requirement satisfied: `TBD (field)`
- Free disk >= 2 GB: `TBD (field)`
- Ports 8080/8081 available: `TBD (field)`

## Scenarios

- S1 Fresh install: `PRECHECK PASS (package assembled)`
- S2 Basic health: `TBD (field)`
- S3 Restart (stop/start): `TBD (field)`
- S4 Reboot persistence: `TBD (field)`
- S5 Upgrade: `TBD (field)`
- S6 Uninstall: `TBD (field)`

## QNAP-specific checks

- ServiceShell lifecycle via App Center: `PRECHECK PASS (service scripts present)`
- `init.sh` registration and `uninstall.sh` cleanup: `PRECHECK PASS (scripts present in package layout)`

## Evidence

- Web UI check: `http://<NAS-IP>:8080` (`TBD (field)`)
- API health check: `http://<NAS-IP>:8081/health` (`TBD (field)`)
- Logs path: `TBD (field)`
- Local execution log: `docs/reports/NAS_EXECUTION_LOG_2026-04-26.md`

## Issues

- No blocking issues detected in local packaging/checksum precheck.

## Decision

- Result: `CONDITIONAL GO (PRELIMINARY)`
- Notes: Final GO requires real-device execution of S2-S6.
