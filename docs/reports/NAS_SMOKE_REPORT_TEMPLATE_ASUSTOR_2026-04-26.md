# NAS Smoke Report Template — Asustor

**Platform:** Asustor ADM  
**Date:** YYYY-MM-DD  
**Tester:** <name>  
**Artifact:** `ip-css-Alfa-0.1.1-asustor-<arch>.apk`  
**Arch:** `x86_64|arm64|rtd1296`  
**Device model:** <model>

## Preflight

- Checksum verified: `PASS|FAIL`
- Java requirement satisfied: `PASS|FAIL`
- Free disk >= 2 GB: `PASS|FAIL`
- Ports 8080/8081 available: `PASS|FAIL`

## Scenarios

- S1 Fresh install: `PASS|FAIL`
- S2 Basic health: `PASS|FAIL`
- S3 Restart (stop/start): `PASS|FAIL`
- S4 Reboot persistence: `PASS|FAIL`
- S5 Upgrade: `PASS|FAIL`
- S6 Uninstall: `PASS|FAIL`

## Asustor-specific checks

- Lifecycle scripts (`preinst/postinst/preuninst/postuninst`): `PASS|FAIL`
- App Central metadata/rendering: `PASS|FAIL`
- RTD1296 run (if applicable): `PASS|FAIL|N/A`

## Evidence

- Web UI check: `http://<NAS-IP>:8080` (`PASS|FAIL`)
- API health check: `http://<NAS-IP>:8081/health` (`PASS|FAIL`)
- Logs path: <path>

## Issues

- Issue 1: <description>
- Issue 2: <description>

## Decision

- Result: `GO|CONDITIONAL GO|NO-GO`
- Notes: <summary>
