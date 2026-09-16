# Post-MVP Deferred Backlog

## Purpose

Track intentionally deferred work without blocking MVP/release-critical delivery.

## Track A: `core/license` (Deferred)

### Why deferred

- Not required for MVP user flow or release-critical runtime path.
- Requires architecture revisit and dedicated threat model.

### Entry Criteria

- MVP release gates are stable for 2 consecutive release cycles.
- Product decision confirms licensing scope (online/offline, grace period, revocation).

### Initial Backlog

1. Define license domain model v2 and contract boundaries.
2. Rework platform secure storage adapters.
3. Implement validation and renewal flows with audit logging.
4. Add unit + integration + failure mode tests.

## Track B: Performance/Runtime

1. Long-run RTSP/HLS soak tests by platform profile.
2. Playback startup latency and rebuffering thresholds.
3. CPU/memory regressions for native video pipelines.

## Track C: Advanced Hardening

1. Pin rotation automation with alerting.
2. Security incident audit trail normalization.
3. Extended auth integrations (LDAP/AD, SSO/OIDC, Kerberos) post-MVP.

## Ownership and Cadence

- Re-evaluate every 2 sprints.
- Move items from deferred to active only with explicit capacity allocation.
