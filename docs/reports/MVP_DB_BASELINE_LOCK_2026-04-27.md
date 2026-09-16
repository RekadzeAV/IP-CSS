# MVP DB Baseline Lock (2026-04-27)

## Purpose

Freeze server DB baseline for Phase 1 and enforce forward-only migration discipline before GO/NO-GO.

## Baseline Definition

- Migration directory: `server/api/src/main/resources/db/migration`
- Baseline sequence:
  - `V1__Initial_schema.sql`
  - `V2__Add_performance_indexes.sql`
  - `V3__Add_server_auth_tables.sql`
  - `V4__Add_audit_log_table.sql`
  - `V5__Add_audit_log_integrity_chain.sql`

## Lock Rules

1. Existing `V1..V5` files are immutable.
2. Any schema change is additive and introduced only by a new migration version.
3. No in-place edits of already applied migrations across environments.
4. Rollback strategy is documented and rehearsed via compose integration path.

## Runtime Guards

- Production DB startup preflight: `DatabaseConfig.validateDatabaseRequirementsForServerStartup()`
- Security startup preflight: `EnterpriseAuthConfig.validateSecurityRequirementsForServerStartup()`

## Verification Evidence

- Compose integration smoke:
  - `scripts/ci/run-server-api-integration-compose.ps1` -> PASS
- MVP acceptance:
  - `./gradlew.bat mvpAutomatedAcceptance` -> PASS

## Remaining DB Gate

Final staging cutover + rollback rehearsal report is still required for release packet completion.
