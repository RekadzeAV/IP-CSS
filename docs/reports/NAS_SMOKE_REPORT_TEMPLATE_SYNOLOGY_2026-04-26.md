# NAS Smoke Report Template — Synology

**Platform:** Synology DSM  
**Date:** YYYY-MM-DD  
**Tester:** <name>  
**Artifact:** `ip-css-Alfa-0.1.1-synology-<arch>.spk`  
**Arch:** `x86_64|arm64`  
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

## Synology-specific checks

- Lifecycle scripts executed (`preinst/postinst/preuninst/postuninst`): `PASS|FAIL`
- Package Center card/metadata valid: `PASS|FAIL`

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
