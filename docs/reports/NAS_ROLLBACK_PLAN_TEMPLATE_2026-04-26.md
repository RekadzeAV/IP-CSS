# NAS Rollback Plan Template

**Release:** Alfa-0.1.1  
**Date:** YYYY-MM-DD  
**Owner:** <name>

## 1) Rollback trigger

- Define the incident and impact threshold that requires rollback.

## 2) Preconditions

- Previous stable artifact available.
- Access to NAS admin console confirmed.
- Backup/snapshot status confirmed.

## 3) Rollback steps

### Synology / QNAP / Asustor

1. Stop current package.
2. Export/backup critical config and data paths.
3. Uninstall current package if required by platform.
4. Install previous known-good artifact.
5. Validate health endpoint and UI availability.

### TrueNAS CORE

1. Stop service in jail.
2. Restore previous bundle/snapshot.
3. Restart jail/service.
4. Validate health endpoint and logs.

### TrueNAS SCALE

1. Roll back image tag/deployment manifest.
2. Reapply compose/k8s deployment.
3. Validate pod/container readiness and health endpoint.

## 4) Verification checklist

- API health returns HTTP 200.
- Web UI is reachable.
- Critical data paths are intact.
- No critical errors in logs.

## 5) Communication

- Notify stakeholders of rollback completion.
- Record timeline and root cause ticket.
- Update GO/NO-GO aggregator and executive summary.
