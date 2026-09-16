# PR Draft: IP-CSS Phased Implementation Baseline

## Summary

- Introduced a single operational status baseline for module readiness in `docs/status/MODULE_STATUS_BASELINE_2026-04-23.md`.
- Reconciled network/certificate pinning documentation to align implementation claims with release-hardening reality.
- Added production-oriented security baselines for TLS and certificate pinning:
  - `config/certificate-pins.production.example.json`
  - `config/https-baseline.example.env`
- Added MVP contract/governance artifacts:
  - `docs/planning/MVP_DOMAIN_CONTRACT_FREEZE.md`
  - `docs/reports/E2E_DISCOVERY_STREAM_EVENT_CHECKLIST.md`
- Tightened CI release gates in `.github/workflows/ci.yml` and added profile validation script:
  - `scripts/ci/validate-video-e2e-profile.py`
- Added post-MVP deferred roadmap separation:
  - `docs/planning/POST_MVP_DEFERRED_BACKLOG.md`
- Updated references in top-level status/docs and changelog.

## Why

The project had documentation and readiness drift (implementation-complete claims vs operational confidence).  
This change establishes clear release truth sources, explicit security baseline artifacts, and stronger CI gate behavior for consistent go/no-go decisions.

## Test Plan

- [x] Validate profile schema locally:
  - `python scripts/ci/validate-video-e2e-profile.py`
- [ ] Run CI pipeline and confirm no soft-fail quality gates remain in release-critical path.
- [ ] Confirm documentation links render and resolve in PR preview.
- [ ] Verify checklist and baseline docs are referenced by status pages:
  - `docs/status/PROJECT_STATUS.md`
  - `PROJECT_PROMPT.md`
  - `PROJECT_STRUCTURE.md`

## Risks / Notes

- Repository has substantial pre-existing unrelated modifications; this PR should include only phased-implementation files.
- `core/license` remains intentionally deferred and removed from release-critical CI path in this phase.
