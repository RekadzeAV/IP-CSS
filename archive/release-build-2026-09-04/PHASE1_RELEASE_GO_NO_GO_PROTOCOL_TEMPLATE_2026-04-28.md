# Phase 1 Release GO / NO-GO Protocol (template)

Date: _______________  
Release tag / build ref: _______________  
Chair: _______________

## Decision (check one)

- [ ] **GO** — all mandatory gates PASS; no open P0 blockers.
- [ ] **CONDITIONAL GO** — GO with documented post-release follow-ups (list below).
- [ ] **NO-GO** — blocking issues remain; do not promote release.

## Approvals

| Role | Name | Signature / date |
|------|------|-------------------|
| Release owner | | |
| QA lead | | |
| Security (if applicable) | | |

## Mandatory evidence checklist (fill paths or N/A)

| Gate | Required state | Evidence path / note |
|------|----------------|----------------------|
| W4 platform gate | PASS | `diagnostics/platform-smoke/w4-mvp-platform-gate-*.md` |
| Video E2E (policy in force) | PASS per policy | `release-build/test/video-e2e-go-no-go-report.md` |
| Security MVP readiness | PASS | `release-build/test/security-mvp-readiness-report.md` |
| PostgreSQL 1.5.6 | PASS | `docs/reports/POSTGRESQL_FINALIZATION_STAGING_REPORT_*.md` |
| Phase1 acceptance matrix | Updated | `docs/reports/PHASE1_FINAL_ACCEPTANCE_MATRIX_*.md` |
| Android device (if in scope) | PASS or waived | |
| Desktop manual (if in scope) | PASS or waived | |
| Security field (if in scope) | PASS or waived | |

## Open risks / follow-ups (CONDITIONAL GO only)

1. _________________________________________________
2. _________________________________________________

## Record

**Final decision:** _______________  
**Recorded by:** _______________  
**Time (UTC):** _______________
