# NAS Field JSON Handoff Runbook (2026-04-27)

## Goal

Replace manual markdown edits of `NAS_FIELD_REPORT_*` with a structured JSON handoff and one-command finalization flow.

## Input file

- Template: `config/nas-field-results.template.json`
- Required sections: `platforms.synology|qnap|asustor|truenas`
- Required values:
  - `s2..s6`: `PASS` or `FAIL`
  - `result`: `GO`, `CONDITIONAL GO`, or `NO-GO`

## Step-by-step flow

1. Copy template and fill real field outcomes from NAS operator validation.
2. Run JSON preflight validation:
   - `./scripts/nas-field-results-preflight.ps1 -Date 2026-04-27 -ResultsPath <your-json>`
3. Apply values into reports:
   - `./scripts/nas-field-apply-results.ps1 -Date 2026-04-27 -ResultsPath <your-json>`
4. Run full finalization pipeline:
   - `./scripts/nas-field-full-finalize.ps1 -Date 2026-04-27`

## One-command combined option

- If JSON is already finalized and validated:
  - `./scripts/nas-field-apply-results.ps1 -Date 2026-04-27 -ResultsPath <your-json> -RunFullFinalize`
- Full orchestrator with execution report:
  - `./scripts/nas-field-auto-orchestrator.ps1 -Date 2026-04-27 -ResultsPath <your-json>`

## Safety gates

- `scripts/nas-field-results-preflight.ps1` validates JSON schema/value set before any report writes.
- `scripts/nas-field-results-preflight.ps1` enforces logical consistency: `result=GO` is rejected when any `S2..S6=FAIL`.
- `scripts/nas-field-readiness-check.ps1` blocks finalize when placeholders or non-final values remain.
- `scripts/nas-field-apply-results.ps1` rejects template-like notes/issues unless `-AllowTemplateValues` is explicitly set.

## Expected outputs

- Updated platform reports: `docs/reports/NAS_FIELD_REPORT_*_2026-04-27.md`
- Updated aggregator: `docs/reports/NAS_FIELD_AGGREGATOR_2026-04-27.md`
- Updated gate report: `release-build/test/video-e2e-go-no-go-report.md`
- Synchronized status docs via `scripts/sync-release-status-docs.ps1`
- Orchestration report: `docs/reports/NAS_FIELD_AUTOMATION_EXECUTION_2026-04-27.md`

