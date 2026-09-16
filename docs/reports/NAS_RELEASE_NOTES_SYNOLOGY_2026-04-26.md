# NAS Release Notes — Synology

**Release:** Alfa-0.1.1  
**Platform:** Synology DSM  
**Artifact(s):** `ip-css-Alfa-0.1.1-synology-x86_64.spk`, `ip-css-Alfa-0.1.1-synology-arm64.spk`

## Included in this release

- Synology SPK packaging pipeline.
- Lifecycle scripts: `preinst`, `postinst`, `preuninst`, `postuninst`.
- Build automation via Gradle NAS tasks.
- Artifact checksum generation (`SHA256`).

## Verification status

- Local packaging precheck: `PASS`.
- Real-device field verification: `PENDING` (S2-S6).

## Known limitations

- Final runtime/lifecycle behavior depends on DSM version and hardware.
- Real-device reboot persistence confirmation is pending.

## Upgrade notes

- Upgrade path should preserve config/data if platform install path remains unchanged.
- Validate data directories before production rollout.

## Rollback

- Use standard package uninstall flow.
- Reinstall previous verified SPK artifact.
- See rollback template: `docs/reports/NAS_ROLLBACK_PLAN_TEMPLATE_2026-04-26.md`.
