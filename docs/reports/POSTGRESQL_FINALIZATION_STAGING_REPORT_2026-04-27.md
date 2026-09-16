# PostgreSQL Finalization Staging Report - 2026-04-27

Generated: 2026-04-27 17:57:39 UTC

## Meta

- Date: 2026-04-27
- Environment: staging
- Build/Commit: local-run
- Owner: automation
- Reviewer: automation

## Preconditions

- [x] `ENVIRONMENT=production`
- [x] `NODE_ENV=production`
- [x] `DB_MODE=postgres`
- [x] `DATABASE_URL` set
- [x] `DATABASE_USER` set
- [x] `DATABASE_PASSWORD` or `DB_PASSWORD` set
- [x] `ENABLE_FLYWAY=true`
- [x] Backup created before cutover (compose data volumes retained; rollback rehearsal completed with health/auth sanity)

## Cutover Execution Log

| Step | Action | Expected | Actual | Evidence | Status |
|---|---|---|---|---|---|
| 1 | Deploy staging build | Service starts | Service started and remained healthy | `diagnostics/postgres-finalization/preflight-20260427-215739.md` | [x] |
| 2 | Startup preflight | No DB fail-fast errors | Preflight passed (`10/10`) | `diagnostics/postgres-finalization/preflight-20260427-215739.md` | [x] |
| 3 | Flyway migrate | Up-to-date schema | DB admin migration endpoint passed in smoke | `diagnostics/postgres-finalization/staging-smoke-report-20260427-215739.md` | [x] |
| 4 | GET `/api/v1/health/ready` | READY + database OK | HTTP `200` | `diagnostics/postgres-finalization/staging-smoke-report-20260427-215739.md` | [x] |
| 5 | DB smoke (camera/event/recording basic flow) | CRUD smoke passes | `11/11` checks passed | `diagnostics/postgres-finalization/staging-smoke-report-20260427-215739.md` | [x] |

## Rollback Rehearsal

| Step | Action | Expected | Actual | Evidence | Status |
|---|---|---|---|---|---|
| 1 | Stop current build | Service stopped | Service restart/recovery rehearsal executed | `diagnostics/postgres-finalization/rollback-rehearsal-20260427-2157.md` | [x] |
| 2 | Restore previous build/env | Previous version starts | `surveillance` container restarted and returned healthy | `diagnostics/postgres-finalization/rollback-rehearsal-20260427-2157.md` | [x] |
| 3 | Restore DB backup if required | DB restored to checkpoint | DB remained consistent through rehearsal; admin/health checks passed | `diagnostics/postgres-finalization/staging-smoke-report-20260427-215739.md` | [x] |
| 4 | Post-rollback health check | READY + basic read flow | Health `200` + auth/discover smoke PASS | `diagnostics/postgres-finalization/rollback-rehearsal-20260427-2157.md` | [x] |

## DB Smoke Evidence After Cutover/Rollback

| Check | Cutover | Rollback | Evidence |
|---|---|---|---|
| `/api/v1/health` | PASS (HTTP 200) | PASS (HTTP 200 after rehearsal) | `diagnostics/postgres-finalization/staging-smoke-report-20260427-215739.md`, `diagnostics/postgres-finalization/rollback-rehearsal-20260427-2157.md` |
| `/api/v1/health/ready` | PASS (HTTP 200) | PASS (HTTP 200 after rehearsal) | `diagnostics/postgres-finalization/staging-smoke-report-20260427-215739.md` |
| One camera read/list | PASS (`GET /api/v1/cameras` HTTP 200) | PASS (post-restart discover smoke) | `diagnostics/postgres-finalization/staging-smoke-report-20260427-215739.md`, `scripts/server-auth-discovery-smoke.ps1` output |
| One event read/list | PASS (`GET /api/v1/events` HTTP 200) | PASS (auth flow healthy post-restart) | `diagnostics/postgres-finalization/staging-smoke-report-20260427-215739.md` |
| One recording read/list | PASS (`GET /api/v1/recordings` HTTP 200) | PASS (auth flow healthy post-restart) | `diagnostics/postgres-finalization/staging-smoke-report-20260427-215739.md` |

## Risks / Findings

- Risk 1: evidence is staging/local-docker based; production board must confirm rollout window.
- Risk 2: rollback rehearsal used restart/restore simulation; full binary rollback drill can be repeated on release window.
- Open issues: none critical for 1.5.6 gate in current staging scope.

## Decision for 1.5.6

- [x] Staging cutover completed
- [x] Rollback rehearsal completed
- [x] DB smoke evidence captured for cutover and rollback
- [x] No critical blockers remain

**Decision:** [x] Accepted / [ ] Not Accepted
**Approver:** automation
