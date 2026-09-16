# NAS Release Notes — TrueNAS

**Release:** Alfa-0.1.1  
**Platform:** TrueNAS CORE / TrueNAS SCALE  
**Artifact(s):** `build/truenas-Alfa-0.1.1/`

## Included in this release

- TrueNAS bundle with:
  - CORE jail support materials (`core/setup-jail.sh`, docs).
  - SCALE Docker compose config.
  - SCALE Kubernetes manifests.
- Build automation via Gradle NAS tasks.
- Artifact checksum generation for package files.

## Verification status

- Local packaging precheck: `PASS`.
- Host-based field verification (CORE/SCALE): `PENDING` (S2-S6).

## Known limitations

- CORE path depends on host jail configuration and Java availability.
- SCALE path depends on container runtime/Kubernetes environment.

## Upgrade notes

- Validate persistence and configuration mapping before production rollout.
- Confirm restart/reboot behavior per deployment type (compose/k8s/jail).

## Rollback

- CORE: revert to previous jail deployment snapshot/version.
- SCALE: roll back compose image tag or k8s deployment version.
- See rollback template: `docs/reports/NAS_ROLLBACK_PLAN_TEMPLATE_2026-04-26.md`.
