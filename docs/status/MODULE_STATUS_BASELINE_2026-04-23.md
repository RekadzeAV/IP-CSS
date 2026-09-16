# Module Status Baseline (2026-04-23)

## Purpose

This file is the operational source of truth for module maturity used by phased implementation and release decisions.
If another document conflicts with this baseline, this baseline wins until conflict is resolved.

## Status Scale

- `release_ready`: implemented, tested on target path, release-gated in CI.
- `integration_ready`: implemented and validated in unit/component scope, needs broader integration coverage.
- `in_progress`: partial implementation, known blockers remain.
- `deferred`: intentionally out of current release scope.

## Module Baseline

| Module | Status | Notes |
|---|---|---|
| `core/common` | integration_ready | Cross-platform contracts and security abstractions are present; kept in CI compile checks. |
| `core/network` | in_progress | Core clients exist, but RTSP/native and end-to-end stability still block release confidence. |
| `shared` | integration_ready | Domain/repository foundation is broad; migration and end-to-end confidence need reinforcement. |
| `android/app` | in_progress | Major screens/viewmodels are present; runtime stream path still not production-stable. |
| `server/api` | integration_ready | API/routing/security are broad; production confidence depends on strict gates and runtime validation. |
| `server/web` | in_progress | Functional surface is broad; video playback path still depends on backend stream stability. |
| `native/*` | in_progress | Build/test infrastructure exists; target runtime parity and long-run reliability still incomplete. |
| `core/license` | deferred | Deliberately excluded from MVP/release-critical path. |

## Release-Critical Blockers

1. RTSP/native runtime path stability on target platforms.
2. End-to-end scenario reliability for discovery -> connect -> playback -> event.
3. Strict CI gates (no soft-fail `|| true` for core build/test path).
4. Certificate pinning and HTTPS operational policy consistency across docs/config.

## Evidence Pointers

- `docs/status/PROJECT_STATUS.md`
- `core/network/NETWORK_LAYER_100_PERCENT_COMPLETE.md`
- `core/network/CERTIFICATE_PINNING.md`
- `core/network/IOS_CERTIFICATE_PINNING_COMPLETE.md`
- `.github/workflows/ci.yml`

## Update Rule

Update this file when one of the following changes:

- CI release gates become stricter or looser.
- A blocker transitions state.
- Module maturity moves between `in_progress`, `integration_ready`, and `release_ready`.
