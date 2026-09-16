# NAS Release Notes — Asustor

**Release:** Alfa-0.1.1  
**Platform:** Asustor ADM  
**Artifact(s):** `ip-css-Alfa-0.1.1-asustor-x86_64.apk`, `ip-css-Alfa-0.1.1-asustor-arm64.apk`, `ip-css-Alfa-0.1.1-asustor-rtd1296.apk`

## Included in this release

- Asustor APK packaging pipeline for x86_64/arm64/rtd1296.
- Lifecycle scripts: `preinst`, `postinst`, `preuninst`, `postuninst`.
- Build automation via Gradle NAS tasks.
- Artifact checksum generation (`SHA256`).

## Verification status

- Local packaging precheck: `PASS`.
- Real-device field verification: `PENDING` (S2-S6).

## Known limitations

- ADM variant differences may impact service registration.
- RTD1296 target requires dedicated field run confirmation.

## Upgrade notes

- Validate storage and startup behavior after upgrade per model.
- Keep previous known-good APK for fast rollback.

## Rollback

- Stop and remove package in App Central.
- Reinstall previous verified APK artifact.
- See rollback template: `docs/reports/NAS_ROLLBACK_PLAN_TEMPLATE_2026-04-26.md`.
