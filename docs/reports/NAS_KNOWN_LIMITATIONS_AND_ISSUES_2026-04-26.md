# NAS Known Limitations and Issues

**Release:** Alfa-0.1.1  
**Date:** 2026-04-26

## Current known limitations

- Full S2-S6 verification on real NAS hardware is pending.
- Platform-specific package manager behavior can differ by firmware version.
- TrueNAS CORE/SCALE require host-level validation for deployment variants.

## Current known issues

- No blocking issues detected in local packaging/checksum precheck.
- Potential runtime/environment issues can only be detected during field execution.

## Mitigation plan

- Execute platform smoke runbook on target devices.
- Record results in platform reports and aggregator.
- Approve final GO/NO-GO only after field validation closure.

## References

- `docs/reports/NAS_PLATFORM_SMOKE_RUNBOOK_2026-04-26.md`
- `docs/reports/NAS_GO_NO_GO_AGGREGATOR_2026-04-26.md`
- `docs/reports/NAS_EXECUTION_LOG_2026-04-26.md`
