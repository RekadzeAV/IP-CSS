# NAS Smoke Report Template — TrueNAS

**Platform:** TrueNAS `CORE|SCALE`  
**Date:** YYYY-MM-DD  
**Tester:** <name>  
**Artifact:** `build/truenas-Alfa-0.1.1/`  
**Device model / host:** <model>

## Preflight

- Checksum verified (if packed artifact used): `PASS|FAIL|N/A`
- Java requirement (CORE jail) satisfied: `PASS|FAIL|N/A`
- Container runtime/K8s ready (SCALE): `PASS|FAIL|N/A`
- Free disk >= 2 GB: `PASS|FAIL`
- Ports 8080/8081 available: `PASS|FAIL`

## Scenarios

- S1 Fresh install/deploy: `PASS|FAIL`
- S2 Basic health: `PASS|FAIL`
- S3 Restart (service/pod/container): `PASS|FAIL`
- S4 Reboot persistence: `PASS|FAIL`
- S5 Upgrade: `PASS|FAIL`
- S6 Uninstall/teardown: `PASS|FAIL`

## TrueNAS-specific checks

- CORE jail path validated (`setup-jail.sh`): `PASS|FAIL|N/A`
- SCALE docker-compose path validated: `PASS|FAIL|N/A`
- SCALE kubernetes manifests validated: `PASS|FAIL|N/A`
- Persistence volume retains data after restart: `PASS|FAIL|N/A`

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
