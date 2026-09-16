# Session Commit Grouping (2026-04-23)

Date: 23 April 2026  
Scope: changes implemented in current assistant session only

## Important

Repository has a large pre-existing dirty tree.  
Use path-scoped staging for each commit group below to avoid capturing unrelated files.

## Group 1 — Discovery Reliability

### Files

- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/OnvifClient.kt`

### Purpose

- Parallel WS-Discovery + UPnP discovery path.
- Merge/dedup discovered cameras with richer metadata preference.

### Suggested commit message

`improve camera discovery reliability with parallel ws-discovery and upnp merge`

## Group 2 — Runtime and Security Gates

### Files

- `scripts/security-mvp-readiness-check.ps1`
- `scripts/video-e2e-go-no-go.ps1`
- `scripts/video-runtime-longrun-smoke.ps1`
- `scripts/bootstrap-local-test-configs.ps1`
- `config/video-e2e-acceptance-profile.example.json`
- `config/video-e2e-acceptance-profile.local.json`
- `config/video-runtime-matrix.local.json` (optional, local-env specific)

### Purpose

- Security MVP readiness automation.
- Video e2e strict/profile-aware/runtime gate with freshness checks.
- Long-run smoke auth fallback for local runtime validation.
- Local test config bootstrap and acceptance profile support.

### Suggested commit message

`add security and video e2e gate automation with profile-aware release policy`

## Group 3 — Documentation Synchronization

### Files

- `docs/reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md`
- `docs/reports/SESSION_PROGRESS_2026-04-23.md`
- `docs/reports/SESSION_COMMIT_GROUPING_2026-04-23.md`
- `docs/planning/RELEASE_GO_NO_GO_CHECKLIST.md`
- `docs/reports/RELEASE_GO_NO_GO_RECORD_2026-03-27.md`
- `docs/status/PROJECT_STATUS.md`
- `docs/status/PROJECT_STATUS_PHASES.md`

### Purpose

- Link video profile-aware gate into release/status docs.
- Record session outcomes and decision model.
- Align update dates and navigation links.

### Suggested commit message

`synchronize release and status docs for video profile-aware go-no-go workflow`

## Optional Generated Artifacts

These are runtime reports; include only if your workflow keeps diagnostics in VCS:

- `release-build/test/security-mvp-readiness-report.md`
- `release-build/test/video-e2e-go-no-go-report.md`

If you do not version generated reports, skip them.

## Safe Staging Pattern (example)

Use path-scoped staging for each group:

```powershell
git add "core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/OnvifClient.kt"
```

```powershell
git add "scripts/security-mvp-readiness-check.ps1" "scripts/video-e2e-go-no-go.ps1" "scripts/video-runtime-longrun-smoke.ps1" "scripts/bootstrap-local-test-configs.ps1" "config/video-e2e-acceptance-profile.example.json" "config/video-e2e-acceptance-profile.local.json"
```

```powershell
git add "docs/reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md" "docs/reports/SESSION_PROGRESS_2026-04-23.md" "docs/reports/SESSION_COMMIT_GROUPING_2026-04-23.md" "docs/planning/RELEASE_GO_NO_GO_CHECKLIST.md" "docs/reports/RELEASE_GO_NO_GO_RECORD_2026-03-27.md" "docs/status/PROJECT_STATUS.md" "docs/status/PROJECT_STATUS_PHASES.md"
```

