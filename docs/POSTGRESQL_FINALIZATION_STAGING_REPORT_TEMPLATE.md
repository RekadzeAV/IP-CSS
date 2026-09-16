# PostgreSQL Finalization Staging Report (Template)

## Meta

- Date:
- Environment:
- Build/Commit:
- Owner:
- Reviewer:

## Preconditions

- [ ] `ENVIRONMENT=production`
- [ ] `NODE_ENV=production`
- [ ] `DB_MODE=postgres`
- [ ] `DATABASE_URL` set
- [ ] `DATABASE_USER` set
- [ ] `DATABASE_PASSWORD` (or `DB_PASSWORD`) set
- [ ] `ENABLE_FLYWAY=true`
- [ ] Backup created before cutover

## Cutover Execution Log

| Step | Command / Action | Expected Result | Actual Result | Evidence | Status |
|---|---|---|---|---|---|
| 1 | Deploy new build | Service starts |  |  | ⬜ |
| 2 | Startup preflight | No fail-fast errors |  |  | ⬜ |
| 3 | Flyway migrations | Applied successfully / up-to-date |  |  | ⬜ |
| 4 | `/api/v1/health` | `status=OK/DEGRADED` with valid checks |  |  | ⬜ |
| 5 | `/api/v1/health/ready` | `READY` |  |  | ⬜ |
| 6 | `/api/v1/database/health` (admin) | DB healthy |  |  | ⬜ |
| 7 | `/api/v1/database/migrations` (admin) | Up-to-date |  |  | ⬜ |
| 8 | `/api/v1/database/pool/stats` (admin) | Pool healthy |  |  | ⬜ |

## Functional Smoke

| Scenario | Expected | Actual | Evidence | Status |
|---|---|---|---|---|
| Login | 200, auth cookies set |  |  | ⬜ |
| Refresh | 200, token rotation works |  |  | ⬜ |
| Logout | 200, refresh token revoked |  |  | ⬜ |
| GET `/api/v1/cameras` | 200 for authorized role |  |  | ⬜ |
| GET `/api/v1/events` | 200 for authorized role |  |  | ⬜ |
| GET `/api/v1/recordings` | 200 for authorized role |  |  | ⬜ |

## Rollback Rehearsal

| Step | Action | Expected | Actual | Evidence | Status |
|---|---|---|---|---|---|
| 1 | Stop new deployment | Service stopped |  |  | ⬜ |
| 2 | Restore previous build | Previous version starts |  |  | ⬜ |
| 3 | Restore DB backup (if required) | DB restored |  |  | ⬜ |
| 4 | Post-rollback health/smoke | System operational |  |  | ⬜ |

## Risks / Findings

- Risk 1:
- Risk 2:
- Open issues:

## Final Acceptance for 1.5.6

- [ ] Production startup blocked without PostgreSQL config
- [ ] No in-memory user repository fallback in production
- [ ] Safe admin bootstrap policy validated
- [ ] Readiness reflects real PostgreSQL availability
- [ ] CI gate `PostgreSQL Finalization Gate` is green
- [ ] Staging cutover + rollback rehearsal completed

**Decision:** ⬜ Accepted (100%) / ⬜ Not Accepted  
**Approver:**
