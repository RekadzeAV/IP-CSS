# Markdown Link Audit Report

Date: 2026-03-27
Scope: all markdown files in repository
Raw data file: diagnostics/doc-audit/md-link-audit-2026-03-27.json

## Summary Metrics

- Markdown files scanned: 1867
- Markdown links found: 29436
- Local links validated: 2310
- Missing local targets: 676

## Missing Links by Category

| Category | Count | Note |
|---|---:|---|
| Active docs/repo | 213 | Actionable |
| server/web/node_modules | 351 | Third-party docs |
| docs/archive | 112 | Historical docs |
| diagnostics/build artifacts | 0 | Generated outputs |

## Top Files With Missing Links (Active Scope)

| File | Missing links |
|---|---:|
| docs/status/CURRENT_STATUS.md | 26 |
| docs/planning/DEVELOPMENT_ROADMAP.md | 24 |
| docs/reports/ФАЗА_1_MVP_ДЕТАЛЬНЫЙ_ОТЧЕТ.md | 16 |
| docs/PLATFORMS.md | 13 |
| docs/ARCHITECTURE.md | 9 |
| docs/planning/DEVELOPMENT_MAP.md | 8 |
| docs/implementation/RTSP_CLIENT_INTEGRATION_PROGRESS.md | 7 |
| docs/summaries/MODELS_INTEGRATION_COMPLETE.md | 6 |
| docs/ANALYSIS_SUMMARY_2025.md | 5 |
| docs/status/NAS_PLATFORM_STATUS_TABLE.md | 5 |
| docs/DEVELOPMENT_PLAN.md | 4 |
| docs/IMPLEMENTATION_STATUS.md | 4 |
| docs/TIMELINE_SETUP.md | 4 |
| docs/reports/DOCUMENTATION_V2_SUMMARY.md | 4 |
| docs/summaries/DOCUMENTATION_V2_SUMMARY.md | 4 |
| PROJECT_PROMPT.md | 3 |
| docs/AI_ANALYTICS.md | 3 |
| docs/ONVIF_CLIENT_STAGE_4.4_DETAILS.md | 3 |
| docs/RTSP_CLIENT_STAGE_4.3_DETAILS.md | 3 |
| docs/TIMELINE_GUIDE.md | 3 |
| docs/ДЕТАЛИЗАЦИЯ_ЭТАПА_6.3_КОМПОНЕНТЫ.md | 3 |
| docs/analysis/STRUCTURE_ANALYSIS_AND_RECOMMENDATIONS.md | 3 |
| docs/status/NATIVE_LIBRARIES_STATUS.md | 3 |
| docs/summaries/OBJECT_DETECTOR_COMPLETE.md | 3 |
| PROJECT_STRUCTURE.md | 2 |

## Top Missing Targets (Active Scope)

| Target | Count |
|---|---:|
| DOCUMENTATION_INDEX.md | 22 |
| ../CURRENT_STATUS.md | 8 |
| ../../PLATFORM_STRUCTURE.md | 6 |
| ../../platforms/client-desktop-x86_64/IMPLEMENTATION_STATUS.md | 6 |
| АНАЛИЗ_VIDEOPLAYER_6.3.3.md | 6 |
| ../../DOCUMENTATION_INDEX.md | 5 |
| ../DEVELOPMENT_ROADMAP.md | 5 |
| ../TIMELINE.md | 5 |
| RTSP_BUILD_INSTRUCTIONS.md | 5 |
| docs/IMPLEMENTATION_STATUS.md | 5 |
| PLATFORM_STRUCTURE.md | 4 |
| ../PROJECT_ROADMAP.md | 4 |
| PROJECT_ROADMAP.md | 4 |
| TIMELINE.md | 4 |
| docs/MISSING_FUNCTIONALITY.md | 4 |
| README.md | 3 |
| CURRENT_STATUS.md | 3 |
| docs/TECHNICAL_DEBT.md | 3 |
| docs/DEVELOPMENT_PLAN.md | 3 |
| docs/WEBSOCKET_CLIENT_IMPLEMENTATION_PLAN.md | 3 |
| ../Log-server/RASPBERRY_PI_4_ANALYSIS.md | 3 |
| CRITICAL_BLOCKERS_REMEDIATION_PLAN.md | 2 |
| ../planning/DEVELOPMENT_ROADMAP.md | 2 |
| ../АНАЛИЗ_VIDEOPLAYER_6.3.3.md | 2 |
| docs/RTSP_CLIENT.md | 2 |

## Findings

- Most actionable issues are in legacy planning/status docs and old cross-links to merged documents.
- A large non-actionable portion is from third-party markdown files under server/web/node_modules.
- Archive markdown files keep historical links that no longer exist by design.

## Recommended Next Pass

1. Fix top contributors first: docs/status/CURRENT_STATUS.md and docs/planning/DEVELOPMENT_ROADMAP.md.
2. Replace legacy targets (CURRENT_STATUS, PROJECT_ROADMAP, DEVELOPMENT_ROADMAP) with canonical status docs.
3. For CI quality gate, audit only active docs and exclude node_modules, docs/archive, diagnostics.
