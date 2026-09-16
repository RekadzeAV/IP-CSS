# NAS Release Notes — QNAP

**Release:** Alfa-0.1.1  
**Platform:** QNAP QTS  
**Artifact(s):** `ip-css-Alfa-0.1.1-qnap-x86_64.qpkg`, `ip-css-Alfa-0.1.1-qnap-arm64.qpkg`, `ip-css-Alfa-0.1.1-qnap-armv7.qpkg`

## Included in this release

- QPKG packaging pipeline for x86_64/arm64/armv7.
- Management scripts: `init.sh`, `start.sh`, `stop.sh`, `uninstall.sh`, `service.sh`.
- Build automation via Gradle NAS tasks.
- Artifact checksum generation (`SHA256`).

## Verification status

- Local packaging precheck: `PASS`.
- Real-device field verification: `PENDING` (S2-S6).

## Known limitations

- App Center behavior can vary by QTS version/build.
- Runtime resource constraints on low-end devices need field tuning.

## Upgrade notes

- Upgrade expected to preserve data/config; validate storage paths per device.
- Keep previous known-good QPKG for fallback.

## Rollback

- Stop and uninstall current package from App Center.
- Reinstall previous verified QPKG artifact.
- See rollback template: `docs/reports/NAS_ROLLBACK_PLAN_TEMPLATE_2026-04-26.md`.
