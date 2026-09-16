# NAS Smoke Report — Asustor (Preliminary)

**Platform:** Asustor ADM  
**Date:** 2026-04-26  
**Tester:** Automation precheck  
**Artifact:** `ip-css-Alfa-0.1.1-asustor-x86_64.apk`, `ip-css-Alfa-0.1.1-asustor-arm64.apk`, `ip-css-Alfa-0.1.1-asustor-rtd1296.apk`  
**Arch:** `x86_64|arm64|rtd1296`  
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

## Asustor-specific checks

- Lifecycle scripts (`preinst/postinst/preuninst/postuninst`): `PRECHECK PASS (scripts present in package layout)`
- App Central metadata/rendering: `TBD (field)`
- RTD1296 run (if applicable): `TBD (field)`

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
