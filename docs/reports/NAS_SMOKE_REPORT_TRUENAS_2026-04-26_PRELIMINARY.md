# NAS Smoke Report — TrueNAS (Preliminary)

**Platform:** TrueNAS `CORE|SCALE`  
**Date:** 2026-04-26  
**Tester:** Automation precheck  
**Artifact:** `build/truenas-Alfa-0.1.1/`  
**Device model / host:** `FIELD TBD`

## Preflight

- Checksum verified (if packed artifact used): `N/A (directory bundle in local run)`
- Java requirement (CORE jail) satisfied: `TBD (field)`
- Container runtime/K8s ready (SCALE): `TBD (field)`
- Free disk >= 2 GB: `TBD (field)`
- Ports 8080/8081 available: `TBD (field)`

## Scenarios

- S1 Fresh install/deploy: `PRECHECK PASS (bundle generated)`
- S2 Basic health: `TBD (field)`
- S3 Restart (service/pod/container): `TBD (field)`
- S4 Reboot persistence: `TBD (field)`
- S5 Upgrade: `TBD (field)`
- S6 Uninstall/teardown: `TBD (field)`

## TrueNAS-specific checks

- CORE jail path validated (`setup-jail.sh`): `PRECHECK PASS (scripts/materials present)`
- SCALE docker-compose path validated: `PRECHECK PASS (bundle contains compose config)`
- SCALE kubernetes manifests validated: `PRECHECK PASS (bundle contains k8s manifests)`
- Persistence volume retains data after restart: `TBD (field)`

## Evidence

- Web UI check: `http://<NAS-IP>:8080` (`TBD (field)`)
- API health check: `http://<NAS-IP>:8081/health` (`TBD (field)`)
- Logs path: `TBD (field)`
- Local execution log: `docs/reports/NAS_EXECUTION_LOG_2026-04-26.md`

## Issues

- No blocking issues detected in local packaging/checksum precheck.

## Decision

- Result: `CONDITIONAL GO (PRELIMINARY)`
- Notes: Final GO requires host-based validation for CORE and SCALE scenarios.
