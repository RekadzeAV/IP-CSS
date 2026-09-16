# Security MVP Readiness (1.9)

Date: 2026-04-27  
Scope: `1.9 Безопасность (MVP)`  
Status: In progress

---

## Purpose

This checklist defines objective go/no-go controls for security readiness of MVP section `1.9`.

---

## Readiness Matrix

| ID | Control | Current State | Gate |
|----|---------|---------------|------|
| 1.9.1 | JWT auth + RBAC | Implemented in server routes/middleware, RBAC tests exist | PASS |
| 1.9.2 | CORS + rate limiting + secure runtime config | CORS allowlist and rate limits implemented; compose hardened | PASS (with operational validation) |
| 1.9.3 | Certificate pinning (platform-appropriate) | Mobile/Desktop pinning path exists; web explicitly uses HTTPS-only trust boundary and no fake pinning | CONDITIONAL |
| 1.9.4 | Enforced HTTPS | Redirect/HSTS implemented; startup guardrails added; production topology validation required | CONDITIONAL |
| 1.9.5 | Camera credential encryption at rest | Fail-closed mapper enforced; startup migration re-encrypts legacy plaintext camera passwords | CONDITIONAL |
| 1.9.6 | Audit logging for critical operations | Security logger and audit routes exist; PostgreSQL durable repository added for `AUDIT_PERSIST_ENABLED=true` | CONDITIONAL |

---

## Go/No-Go Rules

- GO is allowed only when all controls are PASS.
- CONDITIONAL means implementation exists, but deployment and evidence are incomplete.
- FAIL means control is not acceptable for release security baseline.

Current decision: **NO-GO**.

Cross-document sync (2026-04-27):

- Security control implementation status: mostly `PASS/CONDITIONAL` (requires staging/field evidence closure).
- Program release gate status: `NO-GO (PROGRAM GATE)` until runtime/matrix and field validation evidence is complete.
- Synced with:
  - `docs/reports/PHASE1_AUTO_EXECUTION_STATUS_2026-04-26.md`
  - `docs/reports/NAS_GO_NO_GO_AGGREGATOR_2026-04-26.md`
  - `docs/reports/NAS_RELEASE_MASTER_INDEX_2026-04-26.md`
  - `docs/status/NAS_PLATFORM_STATUS_TABLE.md`

---

## Mandatory Actions to Reach GO

1. Complete evidence that web path enforces HTTPS/TLS trust boundary in production and does not claim browser pinning.
2. Validate legacy camera credential migration in staging and attach migration evidence (count, logs, rollback notes).
3. Validate HTTPS production topology end-to-end:
   - direct TLS (`USE_HTTPS=true`), or
   - trusted reverse proxy (`ALLOW_EXTERNAL_TLS_TERMINATION=true`) with correct forwarded headers.
4. Enable `AUDIT_PERSIST_ENABLED=true` in staging/prod and attach evidence from `audit_log` queries.
5. Produce evidence package: tests/logs/config snapshot proving each 1.9 control.

---

## Evidence Pointers

- Server config hardening: `server/api/src/main/kotlin/com/company/ipcamera/server/config/ServerConfig.kt`
- HTTPS redirect proxy-awareness: `server/api/src/main/kotlin/com/company/ipcamera/server/middleware/HttpsRedirectMiddleware.kt`
- Compose hardening: `docker-compose.yml`
- Security status table: `docs/status/PROJECT_STATUS_PHASES.md`

