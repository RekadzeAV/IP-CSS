# Р вЂќР С•Р С”РЎС“Р СР ВµР Р…РЎвЂљР В°РЎвЂ Р С‘РЎРЏ Р С—РЎР‚Р С•Р ВµР С”РЎвЂљР В° IP-CSS

**Р вЂ™Р ВµРЎР‚РЎРѓР С‘РЎРЏ Р С—РЎР‚Р С•Р ВµР С”РЎвЂљР В°:** Alfa-0.1.1
**Р СџР С•РЎРѓР В»Р ВµР Т‘Р Р…Р ВµР Вµ Р С•Р В±Р Р…Р С•Р Р†Р В»Р ВµР Р…Р С‘Р Вµ:** 27 April 2026

> **СЂСџвЂњС™ Р СџР С•Р В»Р Р…РЎвЂ№Р в„– Р С‘Р Р…Р Т‘Р ВµР С”РЎРѓ Р Т‘Р С•Р С”РЎС“Р СР ВµР Р…РЎвЂљР В°РЎвЂ Р С‘Р С‘:** [DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md) РІР‚вЂќ Р ВµР Т‘Р С‘Р Р…Р В°РЎРЏ РЎвЂљР С•РЎвЂЎР С”Р В° Р Р†РЎвЂ¦Р С•Р Т‘Р В° РЎРѓР С• Р Р†РЎРѓР ВµР СР С‘ РЎРѓРЎРѓРЎвЂ№Р В»Р С”Р В°Р СР С‘ Р С‘ Р С”Р В°РЎР‚РЎвЂљР С•Р в„– РЎРѓР Р†РЎРЏР В·Р ВµР в„–.

---

## СЂСџвЂњРЉ Source of Truth (release status)

### Status updates quick start

- Canonical status: `status/PROJECT_STATUS.md`
- Server lock snapshot (`1.5.x`): `status/STATUS_LOCK_2026-04-27.md`
- Video gate lock snapshot (`1.8`): `status/VIDEO_GATE_LOCK_2026-04-27.md`
- One-page operator memo (update order and lock rules): `reports/STATUS_LOCK_AUTOMATION_MEMO_2026-04-27.md`

- Р С›РЎРѓР Р…Р С•Р Р†Р Р…Р С•Р в„– РЎР‚Р ВµР В»Р С‘Р В·Р Р…РЎвЂ№Р в„– РЎРѓРЎвЂљР В°РЎвЂљРЎС“РЎРѓ (РЎвЂљР ВµР С”РЎС“РЎвЂ°Р С‘Р в„–):  
  - `reports/PHASE1_AUTO_EXECUTION_STATUS_2026-04-26.md`
  - `reports/NAS_GO_NO_GO_AGGREGATOR_2026-04-26.md`
  - `reports/NAS_RELEASE_MASTER_INDEX_2026-04-26.md`
  - `planning/RELEASE_GO_NO_GO_CHECKLIST.md`
- Р СћР ВµР С”РЎС“РЎвЂ°Р ВµР Вµ РЎРѓР С‘Р Р…РЎвЂ¦РЎР‚Р С•Р Р…Р С‘Р В·Р С‘РЎР‚Р С•Р Р†Р В°Р Р…Р Р…Р С•Р Вµ РЎР‚Р ВµРЎв‚¬Р ВµР Р…Р С‘Р Вµ: packaging precheck `PASS`, program gate `NO-GO` Р Т‘Р С• Р В·Р В°Р С”РЎР‚РЎвЂ№РЎвЂљР С‘РЎРЏ runtime/matrix Р С‘ field validation evidence.
- Р СџР С•РЎРѓР В»Р ВµР Т‘Р Р…Р С‘Р в„– Р В»Р С•Р С”Р В°Р В»РЎРЉР Р…РЎвЂ№Р в„– Р С—Р ВµРЎР‚Р ВµРЎРѓРЎвЂЎР ВµРЎвЂљ video gate (`release-build/test/video-e2e-go-no-go-report.md`): strict `NO-GO`, profile-aware `GO`, runtime decision `GO`.
- Р вЂќР С•Р С”РЎС“Р СР ВµР Р…РЎвЂљРЎвЂ№ РЎРѓР С• РЎРѓРЎвЂљР В°РЎР‚РЎвЂ№Р СР С‘ Р Т‘Р В°РЎвЂљР В°Р СР С‘/Р СР ВµРЎвЂљРЎР‚Р С‘Р С”Р В°Р СР С‘ (Р Р…Р В°Р С—РЎР‚Р С‘Р СР ВµРЎР‚ РЎРЏР Р…Р Р†Р В°РЎР‚РЎРЉ 2026) РЎРѓРЎвЂЎР С‘РЎвЂљР В°РЎвЂљРЎРЉ Р С‘РЎРѓРЎвЂљР С•РЎР‚Р С‘РЎвЂЎР ВµРЎРѓР С”Р С‘Р СР С‘ Р С‘ Р Р…Р Вµ Р С‘РЎРѓР С—Р С•Р В»РЎРЉР В·Р С•Р Р†Р В°РЎвЂљРЎРЉ Р С”Р В°Р С” РЎвЂћР С‘Р Р…Р В°Р В»РЎРЉР Р…РЎвЂ№Р в„– release gate.
- Р С’РЎР‚РЎвЂ¦Р С‘Р Р† legacy-Р Т‘Р С•Р С”РЎС“Р СР ВµР Р…РЎвЂљР С•Р Р†: `archive/docs-legacy-2026-04-27/` Р С‘ `archive/status-legacy-2026-04-27/`.
- Р СџР С•Р В»Р Р…РЎвЂ№Р в„– auto-sync docs-pass Р В·Р В°Р Р†Р ВµРЎР‚РЎв‚¬РЎвЂР Р…: РЎРѓР С. `reports/DOCUMENTATION_STATUS_SYNC_2026-04-27.md` Р С‘ post-migration link audit `reports/DOCS_BROKEN_LINKS_AFTER_ARCHIVE_2026-04-27.md` (`0` broken links).
- Р С’РЎС“Р Т‘Р С‘РЎвЂљ Р СР ВµРЎвЂљР В°Р Т‘Р В°Р Р…Р Р…РЎвЂ№РЎвЂ¦ РЎв‚¬Р В°Р С—Р С•Р С” (Р Т‘Р В°РЎвЂљР В°/Р С•Р В±Р Р…Р С•Р Р†Р В»Р ВµР Р…Р С‘Р Вµ): `reports/DOCS_HEADER_AUDIT_2026-04-27.md` (coverage Р С‘ Р С•РЎРѓРЎвЂљР В°Р Р†РЎв‚¬Р С‘Р ВµРЎРѓРЎРЏ РЎвЂћР В°Р в„–Р В»РЎвЂ№ Р Т‘Р В»РЎРЏ Р С—Р С•РЎРЊРЎвЂљР В°Р С—Р Р…Р С•Р в„– Р Р…Р С•РЎР‚Р СР В°Р В»Р С‘Р В·Р В°РЎвЂ Р С‘Р С‘).
- NAS field stage automation:
  - quick runbook: `reports/NAS_FIELD_OPERATOR_QUICK_RUNBOOK_2026-04-27.md`
  - auto-aggregation: `scripts/nas-field-aggregate.ps1`
  - chained auto execution: `scripts/phase3-continue-auto.ps1` (`reports/PHASE3_CONTINUE_AUTO_EXECUTION_STATUS_2026-04-27.md`)
  - Desktop story `3.2.1` to-100 checklist: `reports/DESKTOP_3_2_1_TO_100_EXECUTION_CHECKLIST_2026-04-27.md`

---

## СЂСџвЂњС™ Р СњР В°Р Р†Р С‘Р С–Р В°РЎвЂ Р С‘РЎРЏ Р С—Р С• Р Т‘Р С•Р С”РЎС“Р СР ВµР Р…РЎвЂљР В°РЎвЂ Р С‘Р С‘

### СЂСџРЏвЂ”РїС‘РЏ Р С’РЎР‚РЎвЂ¦Р С‘РЎвЂљР ВµР С”РЎвЂљРЎС“РЎР‚Р В° Р С‘ РЎРѓРЎвЂљРЎР‚РЎС“Р С”РЎвЂљРЎС“РЎР‚Р В°

- **[ARCHITECTURE.md](ARCHITECTURE.md)** - Р С’РЎР‚РЎвЂ¦Р С‘РЎвЂљР ВµР С”РЎвЂљРЎС“РЎР‚Р В° РЎРѓР С‘РЎРѓРЎвЂљР ВµР СРЎвЂ№, РЎРѓР В»Р С•Р С‘, Р СР С•Р Т‘РЎС“Р В»Р С‘, Р С—РЎР‚Р С‘Р Р…РЎвЂ Р С‘Р С—РЎвЂ№ Р С—РЎР‚Р С•Р ВµР С”РЎвЂљР С‘РЎР‚Р С•Р Р†Р В°Р Р…Р С‘РЎРЏ
- **[PROJECT_STRUCTURE.md](../PROJECT_STRUCTURE.md)** - Р РЋРЎвЂљРЎР‚РЎС“Р С”РЎвЂљРЎС“РЎР‚Р В° Р С—РЎР‚Р С•Р ВµР С”РЎвЂљР В°, Р СР С•Р Т‘РЎС“Р В»Р С‘, Р В·Р В°Р Р†Р С‘РЎРѓР С‘Р СР С•РЎРѓРЎвЂљР С‘
- **[PLATFORMS.md](archive/docs-legacy-2026-04-27/PLATFORMS.md)** - Р В Р В°Р В·Р Т‘Р ВµР В»Р ВµР Р…Р С‘Р Вµ Р С—Р С• Р С—Р В»Р В°РЎвЂљРЎвЂћР С•РЎР‚Р СР В°Р С (Android, iOS, Desktop, Web, NAS)

### СЂСџвЂњР‰ Р РЋРЎвЂљР В°РЎвЂљРЎС“РЎРѓ Р С‘ Р С—Р В»Р В°Р Р…Р С‘РЎР‚Р С•Р Р†Р В°Р Р…Р С‘Р Вµ

- **[status/PROJECT_STATUS.md](status/PROJECT_STATUS.md)** - РІВ­С’ **Р С›Р РЋР СњР С›Р вЂ™Р СњР С›Р в„ў** Р РЋРЎвЂљР В°РЎвЂљРЎС“РЎРѓ Р С‘ Р С—Р В»Р В°Р Р… РЎР‚Р В°Р В·РЎР‚Р В°Р В±Р С•РЎвЂљР С”Р С‘ (~81%), РЎвЂћР В°Р В·РЎвЂ№, Р В±Р В»Р С•Р С”Р ВµРЎР‚РЎвЂ№, Р С—РЎР‚Р С‘Р С•РЎР‚Р С‘РЎвЂљР ВµРЎвЂљРЎвЂ№
- **[status/MODULE_STATUS_BASELINE_2026-04-23.md](status/MODULE_STATUS_BASELINE_2026-04-23.md)** - Р вЂўР Т‘Р С‘Р Р…РЎвЂ№Р в„– baseline Р СР С•Р Т‘РЎС“Р В»Р ВµР в„– Р С‘ Р С—РЎР‚Р С•Р С–РЎР‚Р ВµРЎРѓРЎРѓР В° Р Р…Р В° 23 April 2026
- **[reports/STATUS_AUDIT_2026-04-23.md](reports/STATUS_AUDIT_2026-04-23.md)** - Р С’РЎС“Р Т‘Р С‘РЎвЂљ РЎРѓРЎвЂљР В°РЎвЂљРЎС“РЎРѓР С•Р Р† Р С—Р С• Р С”Р С•Р Т‘РЎС“: Р С—Р С•Р Т‘РЎвЂљР Р†Р ВµРЎР‚Р В¶Р Т‘Р ВµР Р…Р С‘Р Вµ Р СР ВµРЎвЂљРЎР‚Р С‘Р С” Р С‘ РЎвЂљР ВµР С”РЎС“РЎвЂ°Р С‘РЎвЂ¦ РЎР‚Р С‘РЎРѓР С”Р С•Р Р†
- **[kmp-phase1-progress.md](kmp-phase1-progress.md)** - Р СџРЎР‚Р С•Р С–РЎР‚Р ВµРЎРѓРЎРѓ Р В¤Р В°Р В·РЎвЂ№ 1: KMP platform stabilization
- **[kmp-phase1-dod-checklist.md](kmp-phase1-dod-checklist.md)** - DoD-РЎвЂЎР ВµР С”Р В»Р С‘РЎРѓРЎвЂљ Р С—РЎР‚Р С‘Р ВµР СР С”Р С‘ Р В¤Р В°Р В·РЎвЂ№ 1 KMP
- **[status/PROJECT_STATUS_PHASES.md](status/PROJECT_STATUS_PHASES.md)** - Р вЂќР ВµРЎвЂљР В°Р В»РЎРЉР Р…РЎвЂ№Р в„– Р С—Р В»Р В°Р Р… Р С—Р С• РЎвЂћР В°Р В·Р В°Р С, РЎРЊРЎвЂљР В°Р С—Р В°Р С Р С‘ Р В·Р В°Р Т‘Р В°РЎвЂЎР В°Р С РЎРѓР С• РЎРѓРЎвЂљР В°РЎвЂљРЎС“РЎРѓР С•Р С Р Р†РЎвЂ№Р С—Р С•Р В»Р Р…Р ВµР Р…Р С‘РЎРЏ
- **[TODO.md](../archive/docs-duplicates-2026-08-08/TODO.md)** - Р вЂўР Т‘Р С‘Р Р…РЎвЂ№Р в„– РЎРѓР С—Р С‘РЎРѓР С•Р С” Р В·Р В°Р Т‘Р В°РЎвЂЎ (To-Do): Р С—РЎР‚Р С‘Р С•РЎР‚Р С‘РЎвЂљР ВµРЎвЂљРЎвЂ№, Р Р…Р ВµР Т‘Р ВµР В»РЎРЉР Р…РЎвЂ№Р Вµ Р С‘РЎвЂљР ВµРЎР‚Р В°РЎвЂ Р С‘Р С‘, РЎвЂћР В°Р В·РЎвЂ№
- **[archive/2026-03-30/TASKS_WITH_DESCRIPTIONS.md](archive/2026-03-30/TASKS_WITH_DESCRIPTIONS.md)** - Р С’РЎР‚РЎвЂ¦Р С‘Р Р†: Р В·Р В°Р Т‘Р В°РЎвЂЎР С‘ РЎРѓ Р С•Р С—Р С‘РЎРѓР В°Р Р…Р С‘РЎРЏР СР С‘ Р С—Р С• Р С•Р В±Р В»Р В°РЎРѓРЎвЂљРЎРЏР С (Р Т‘Р С• Р С”Р С•Р Р…РЎРѓР С•Р В»Р С‘Р Т‘Р В°РЎвЂ Р С‘Р С‘)
- **[IMPLEMENTATION_STATUS.md](../archive/docs-duplicates-2026-08-08/IMPLEMENTATION_STATUS.md)** - Р вЂќР ВµРЎвЂљР В°Р В»РЎРЉР Р…РЎвЂ№Р в„– РЎРѓРЎвЂљР В°РЎвЂљРЎС“РЎРѓ РЎР‚Р ВµР В°Р В»Р С‘Р В·Р В°РЎвЂ Р С‘Р С‘ Р Р†РЎРѓР ВµРЎвЂ¦ Р С”Р С•Р СР С—Р С•Р Р…Р ВµР Р…РЎвЂљР С•Р Р†
- **[MISSING_FUNCTIONALITY.md](../archive/docs-deprecated-2026-09-04/MISSING_FUNCTIONALITY.md)** - Р С’Р Р…Р В°Р В»Р С‘Р В· Р Р…Р ВµРЎР‚Р ВµР В°Р В»Р С‘Р В·Р С•Р Р†Р В°Р Р…Р Р…Р С•Р С–Р С• РЎвЂћРЎС“Р Р…Р С”РЎвЂ Р С‘Р С•Р Р…Р В°Р В»Р В°
- **[DEVELOPMENT_PLAN.md](../archive/docs-deprecated-2026-09-04/DEVELOPMENT_PLAN.md)** - Р СџР В»Р В°Р Р… Р Т‘Р В°Р В»РЎРЉР Р…Р ВµР в„–РЎв‚¬Р ВµР в„– РЎР‚Р В°Р В·РЎР‚Р В°Р В±Р С•РЎвЂљР С”Р С‘ Р С—Р С• РЎвЂћР В°Р В·Р В°Р С
- **[planning/DETAILED_DEVELOPMENT_PLAN.md](planning/DETAILED_DEVELOPMENT_PLAN.md)** - Р вЂќР ВµРЎвЂљР В°Р В»РЎРЉР Р…РЎвЂ№Р в„– Р С—Р В»Р В°Р Р… РЎРѓ РЎРЊРЎвЂљР В°Р С—Р В°Р СР С‘ Р С‘ Р В·Р В°Р Т‘Р В°РЎвЂЎР В°Р СР С‘ Р С—Р С• РЎР‚Р В°Р В·Р Т‘Р ВµР В»Р В°Р С
- **[planning/CRITICAL_BLOCKERS_REMEDIATION_PLAN.md](planning/CRITICAL_BLOCKERS_REMEDIATION_PLAN.md)** - Р СџР В»Р В°Р Р… РЎС“РЎРѓРЎвЂљРЎР‚Р В°Р Р…Р ВµР Р…Р С‘РЎРЏ Р С”РЎР‚Р С‘РЎвЂљР С‘РЎвЂЎР ВµРЎРѓР С”Р С‘РЎвЂ¦ Р В±Р В»Р С•Р С”Р ВµРЎР‚Р С•Р Р†
- **[planning/BLOCKS_8_9_ANALYTICS_AND_FEATURES.md](planning/BLOCKS_8_9_ANALYTICS_AND_FEATURES.md)** - Р вЂР В»Р С•Р С”Р С‘ 8РІР‚вЂњ9: РЎР‚Р В°РЎРѓРЎв‚¬Р С‘РЎР‚Р ВµР Р…Р Р…Р В°РЎРЏ Р В°Р Р…Р В°Р В»Р С‘РЎвЂљР С‘Р С”Р В° Р С‘ Р Т‘Р С•Р С—. РЎвЂћРЎС“Р Р…Р С”РЎвЂ Р С‘Р С•Р Р…Р В°Р В»

### СЂСџвЂњР‰ Р В Р ВµР В°Р В»Р С‘Р В·Р С•Р Р†Р В°Р Р…Р Р…РЎвЂ№Р Вµ Р С—Р С•Р Т‘РЎРѓР С‘РЎРѓРЎвЂљР ВµР СРЎвЂ№ (РЎРѓРЎвЂљР В°РЎвЂљРЎС“РЎРѓ)

- **[status/CLOUD_SYNC_AND_STORAGE.md](status/CLOUD_SYNC_AND_STORAGE.md)** - Р РЋР С‘Р Р…РЎвЂ¦РЎР‚Р С•Р Р…Р С‘Р В·Р В°РЎвЂ Р С‘РЎРЏ, Р С•Р В±Р В»Р В°РЎвЂЎР Р…Р С•Р Вµ РЎвЂ¦РЎР‚Р В°Р Р…Р С‘Р В»Р С‘РЎвЂ°Р Вµ, Р В±РЎРЊР С”Р В°Р С—РЎвЂ№
- **[status/CLUSTER_LOAD_BALANCING_REPLICATION.md](status/CLUSTER_LOAD_BALANCING_REPLICATION.md)** - Р С™Р В»Р В°РЎРѓРЎвЂљР ВµРЎР‚Р С‘Р В·Р В°РЎвЂ Р С‘РЎРЏ, Р В±Р В°Р В»Р В°Р Р…РЎРѓР С‘РЎР‚Р С•Р Р†Р С”Р В°, РЎР‚Р ВµР С—Р В»Р С‘Р С”Р В°РЎвЂ Р С‘РЎРЏ Р вЂР вЂќ
- **[status/PLAN_OAUTH2_AND_SECURITY_MONITORING.md](status/PLAN_OAUTH2_AND_SECURITY_MONITORING.md)** - OAuth2/OIDC Р С‘ Р СР С•Р Р…Р С‘РЎвЂљР С•РЎР‚Р С‘Р Р…Р С– Р В±Р ВµР В·Р С•Р С—Р В°РЎРѓР Р…Р С•РЎРѓРЎвЂљР С‘

### СЂСџвЂќВ§ Р С™Р С•Р СР С—Р С•Р Р…Р ВµР Р…РЎвЂљРЎвЂ№ Р С‘ Р С‘Р Р…РЎвЂљР ВµР С–РЎР‚Р В°РЎвЂ Р С‘РЎРЏ

#### Р РЋР ВµРЎвЂљР ВµР Р†РЎвЂ№Р Вµ Р С”Р В»Р С‘Р ВµР Р…РЎвЂљРЎвЂ№
- **[ONVIF_CLIENT.md](archive/docs-legacy-2026-04-27/ONVIF_CLIENT.md)** - Р вЂќР С•Р С”РЎС“Р СР ВµР Р…РЎвЂљР В°РЎвЂ Р С‘РЎРЏ ONVIF Р С”Р В»Р С‘Р ВµР Р…РЎвЂљР В° (Р С‘РЎРѓР С—Р С•Р В»РЎРЉР В·Р С•Р Р†Р В°Р Р…Р С‘Р Вµ, API)
- **[RTSP_CLIENT.md](archive/docs-legacy-2026-04-27/RTSP_CLIENT.md)** - Р вЂќР С•Р С”РЎС“Р СР ВµР Р…РЎвЂљР В°РЎвЂ Р С‘РЎРЏ RTSP Р С”Р В»Р С‘Р ВµР Р…РЎвЂљР В° (Р С‘РЎРѓР С—Р С•Р В»РЎРЉР В·Р С•Р Р†Р В°Р Р…Р С‘Р Вµ, API)
- **[WEBSOCKET_CLIENT.md](archive/docs-legacy-2026-04-27/WEBSOCKET_CLIENT.md)** - Р вЂќР С•Р С”РЎС“Р СР ВµР Р…РЎвЂљР В°РЎвЂ Р С‘РЎРЏ WebSocket Р С”Р В»Р С‘Р ВµР Р…РЎвЂљР В° (Р С‘РЎРѓР С—Р С•Р В»РЎРЉР В·Р С•Р Р†Р В°Р Р…Р С‘Р Вµ, API)
- **[INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md)** - Р В РЎС“Р С”Р С•Р Р†Р С•Р Т‘РЎРѓРЎвЂљР Р†Р С• Р С—Р С• Р С‘Р Р…РЎвЂљР ВµР С–РЎР‚Р В°РЎвЂ Р С‘Р С‘ Р В±Р С‘Р В±Р В»Р С‘Р С•РЎвЂљР ВµР С” (XML Р С—Р В°РЎР‚РЎРѓР С‘Р Р…Р С–, Live555, FFmpeg, OpenCV, TensorFlow Lite)

#### Р вЂєР С‘РЎвЂ Р ВµР Р…Р В·Р С‘РЎР‚Р С•Р Р†Р В°Р Р…Р С‘Р Вµ
- **[LICENSE_SYSTEM.md](archive/docs-legacy-2026-04-27/LICENSE_SYSTEM.md)** - Р РЋР С‘РЎРѓРЎвЂљР ВµР СР В° Р В»Р С‘РЎвЂ Р ВµР Р…Р В·Р С‘РЎР‚Р С•Р Р†Р В°Р Р…Р С‘РЎРЏ (РЎвЂљР С‘Р С—РЎвЂ№ Р В»Р С‘РЎвЂ Р ВµР Р…Р В·Р С‘Р в„–, Р В°Р С”РЎвЂљР С‘Р Р†Р В°РЎвЂ Р С‘РЎРЏ, Р С—Р ВµРЎР‚Р ВµР Р…Р С•РЎРѓ)

#### KMP Security / Stabilization
- **[kmp-security-contract.md](kmp-security-contract.md)** - Р вЂўР Т‘Р С‘Р Р…РЎвЂ№Р в„– Р С”Р С•Р Р…РЎвЂљРЎР‚Р В°Р С”РЎвЂљ security/encryption Р Т‘Р В»РЎРЏ KMP expect/actual

### СЂСџРЉС’ API Р С‘ РЎРѓР ВµРЎР‚Р Р†Р ВµРЎР‚

- **[API.md](API.md)** - REST API Р Т‘Р С•Р С”РЎС“Р СР ВµР Р…РЎвЂљР В°РЎвЂ Р С‘РЎРЏ (endpoints, Р СР С•Р Т‘Р ВµР В»Р С‘, Р С—РЎР‚Р С‘Р СР ВµРЎР‚РЎвЂ№)
- **[DEPLOYMENT_GUIDE.md](../archive/docs/deployment/DEPLOYMENT_GUIDE.md)** - Р В РЎС“Р С”Р С•Р Р†Р С•Р Т‘РЎРѓРЎвЂљР Р†Р С• Р С—Р С• РЎР‚Р В°Р В·Р Р†Р ВµРЎР‚РЎвЂљРЎвЂ№Р Р†Р В°Р Р…Р С‘РЎР‹ (Docker, Kubernetes, NAS)

### СЂСџВ§Р„ Р СћР ВµРЎРѓРЎвЂљР С‘РЎР‚Р С•Р Р†Р В°Р Р…Р С‘Р Вµ

- **[TESTING.md](TESTING.md)** - Р РЋРЎвЂљРЎР‚Р В°РЎвЂљР ВµР С–Р С‘РЎРЏ РЎвЂљР ВµРЎРѓРЎвЂљР С‘РЎР‚Р С•Р Р†Р В°Р Р…Р С‘РЎРЏ, РЎвЂљР С‘Р С—РЎвЂ№ РЎвЂљР ВµРЎРѓРЎвЂљР С•Р Р†
- **[TESTS_SUMMARY.md](../archive/docs-duplicates-2026-08-08/TESTS_SUMMARY.md)** - Р РЋР Р†Р С•Р Т‘Р С”Р В° РЎРѓР С•Р В·Р Т‘Р В°Р Р…Р Р…РЎвЂ№РЎвЂ¦ РЎвЂљР ВµРЎРѓРЎвЂљР С•Р Р†

### СЂСџвЂњВ¦ Р вЂ”Р В°Р Р†Р С‘РЎРѓР С‘Р СР С•РЎРѓРЎвЂљР С‘

- **[REQUIRED_LIBRARIES.md](REQUIRED_LIBRARIES.md)** - Р СџР С•Р В»Р Р…РЎвЂ№Р в„– РЎРѓР С—Р С‘РЎРѓР С•Р С” РЎвЂљРЎР‚Р ВµР В±РЎС“Р ВµР СРЎвЂ№РЎвЂ¦ Р В±Р С‘Р В±Р В»Р С‘Р С•РЎвЂљР ВµР С”
- **[REQUIRED_LIBRARIES_SUMMARY.md](REQUIRED_LIBRARIES_SUMMARY.md)** - Р С™РЎР‚Р В°РЎвЂљР С”Р В°РЎРЏ РЎРѓР Р†Р С•Р Т‘Р С”Р В° Р В±Р С‘Р В±Р В»Р С‘Р С•РЎвЂљР ВµР С”

### СЂСџвЂњСњ Р В Р В°Р В·РЎР‚Р В°Р В±Р С•РЎвЂљР С”Р В°

- **[DEVELOPMENT.md](DEVELOPMENT.md)** - Р В РЎС“Р С”Р С•Р Р†Р С•Р Т‘РЎРѓРЎвЂљР Р†Р С• Р С—Р С• РЎР‚Р В°Р В·РЎР‚Р В°Р В±Р С•РЎвЂљР С”Р Вµ (Р Р…Р В°РЎРѓРЎвЂљРЎР‚Р С•Р в„–Р С”Р В° Р С•Р С”РЎР‚РЎС“Р В¶Р ВµР Р…Р С‘РЎРЏ, РЎРѓР В±Р С•РЎР‚Р С”Р В°, Git workflow)
- **KMP Phase 1 one-shot verifier:** `python scripts/ci/verify-kmp-phase1.py` (`--skip-gradle` Р Т‘Р В»РЎРЏ Р В±РЎвЂ№РЎРѓРЎвЂљРЎР‚Р С•Р С–Р С• РЎР‚Р ВµР В¶Р С‘Р СР В°)
- **Windows PowerShell:** `.\scripts\ci\verify-kmp-phase1.ps1` (`-SkipGradle` Р Т‘Р В»РЎРЏ Р В±РЎвЂ№РЎРѓРЎвЂљРЎР‚Р С•Р С–Р С• РЎР‚Р ВµР В¶Р С‘Р СР В°)
- **Linux/macOS/WSL:** `./scripts/ci/verify-kmp-phase1.sh` (`--skip-gradle` Р Т‘Р В»РЎРЏ Р В±РЎвЂ№РЎРѓРЎвЂљРЎР‚Р С•Р С–Р С• РЎР‚Р ВµР В¶Р С‘Р СР В°)
- **CI-equivalent gates (strict):** `python scripts/ci/verify-kmp-phase1.py --ci-profile` | `.\scripts\ci\verify-kmp-phase1.ps1 -CiProfile` | `./scripts/ci/verify-kmp-phase1.sh --ci-profile`
- **Runtime matrix config validation:** `python scripts/ci/check-video-runtime-matrix-config.py --root .`
- **Video e2e profile validation:** `python scripts/ci/validate-video-e2e-profile.py --root .`
- **[planning/LOCAL_RELEASE_BUILD_MASTER_PLAN.md](planning/LOCAL_RELEASE_BUILD_MASTER_PLAN.md)** - Р СљР В°РЎРѓРЎвЂљР ВµРЎР‚-Р С—Р В»Р В°Р Р… Р В»Р С•Р С”Р В°Р В»РЎРЉР Р…Р С•Р в„– РЎР‚Р ВµР В»Р С‘Р В·Р Р…Р С•Р в„– РЎРѓР В±Р С•РЎР‚Р С”Р С‘ Р С—Р С• РЎвЂљР С‘Р С—Р В°Р С Р Р†РЎвЂ№Р С—РЎС“РЎРѓР С”Р В°
- **[planning/RELEASE_GO_NO_GO_CHECKLIST.md](planning/RELEASE_GO_NO_GO_CHECKLIST.md)** - Р В¤Р С‘Р Р…Р В°Р В»РЎРЉР Р…РЎвЂ№Р в„– go/no-go checklist Р С—Р ВµРЎР‚Р ВµР Т‘ Р С—РЎС“Р В±Р В»Р С‘Р С”Р В°РЎвЂ Р С‘Р ВµР в„– РЎР‚Р ВµР В»Р С‘Р В·Р В°
- **[reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md](reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md)** - Video e2e strict/profile-aware/runtime gate runbook
- **[reports/DOCUMENTATION_LINK_AUDIT_2026-04-23.md](reports/DOCUMENTATION_LINK_AUDIT_2026-04-23.md)** - Р ВРЎвЂљР С•Р С–Р С•Р Р†РЎвЂ№Р в„– Р В°РЎС“Р Т‘Р С‘РЎвЂљ РЎРѓР Р†РЎРЏР В·Р Р…Р С•РЎРѓРЎвЂљР С‘ РЎРѓРЎРѓРЎвЂ№Р В»Р С•Р С” Р С‘ Р Т‘Р С•Р С”РЎС“Р СР ВµР Р…РЎвЂљР В°РЎвЂ Р С‘Р С•Р Р…Р Р…Р С•Р С–Р С• Р С”Р С•Р Р…РЎвЂљРЎС“РЎР‚Р В°
- **[PROMPT_ANALYSIS.md](PROMPT_ANALYSIS.md)** - Р С’Р Р…Р В°Р В»Р С‘Р В· Р С‘РЎРѓРЎвЂ¦Р С•Р Т‘Р Р…Р С•Р С–Р С• Р С—РЎР‚Р С•Р СРЎвЂљР В° Р С—РЎР‚Р С•Р ВµР С”РЎвЂљР В°
- **[../CONTRIBUTING.md](../CONTRIBUTING.md)** - Р В РЎС“Р С”Р С•Р Р†Р С•Р Т‘РЎРѓРЎвЂљР Р†Р С• Р С—Р С• Р Р†Р Р…Р ВµРЎРѓР ВµР Р…Р С‘РЎР‹ Р Р†Р С”Р В»Р В°Р Т‘Р В° (Git workflow, Р С—РЎР‚Р С•РЎвЂ Р ВµРЎРѓРЎРѓ РЎР‚Р В°Р В·РЎР‚Р В°Р В±Р С•РЎвЂљР С”Р С‘)

---

## СЂСџвЂќвЂ” Р вЂРЎвЂ№РЎРѓРЎвЂљРЎР‚РЎвЂ№Р Вµ РЎРѓРЎРѓРЎвЂ№Р В»Р С”Р С‘

### Р вЂќР В»РЎРЏ Р Р…Р С•Р Р†РЎвЂ№РЎвЂ¦ РЎР‚Р В°Р В·РЎР‚Р В°Р В±Р С•РЎвЂљРЎвЂЎР С‘Р С”Р С•Р Р†

1. **[DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)** РІР‚вЂќ Р С—Р С•Р В»Р Р…РЎвЂ№Р в„– Р С‘Р Р…Р Т‘Р ВµР С”РЎРѓ Р С‘ Р В±РЎвЂ№РЎРѓРЎвЂљРЎР‚РЎвЂ№Р Вµ РЎРѓРЎРѓРЎвЂ№Р В»Р С”Р С‘
2. [ARCHITECTURE.md](ARCHITECTURE.md) РІР‚вЂќ Р С—Р С•Р Р…РЎРЏРЎвЂљРЎРЉ Р В°РЎР‚РЎвЂ¦Р С‘РЎвЂљР ВµР С”РЎвЂљРЎС“РЎР‚РЎС“
3. [status/PROJECT_STATUS.md](status/PROJECT_STATUS.md) РІР‚вЂќ РЎвЂљР ВµР С”РЎС“РЎвЂ°Р С‘Р в„– РЎРѓРЎвЂљР В°РЎвЂљРЎС“РЎРѓ Р С‘ РЎвЂћР В°Р В·РЎвЂ№ (~81%)
4. [DEVELOPMENT.md](DEVELOPMENT.md) РІР‚вЂќ Р Р…Р В°РЎРѓРЎвЂљРЎР‚Р С•Р в„–Р С”Р В° Р С•Р С”РЎР‚РЎС“Р В¶Р ВµР Р…Р С‘РЎРЏ Р С‘ Git workflow
5. [IMPLEMENTATION_STATUS.md](../archive/docs-duplicates-2026-08-08/IMPLEMENTATION_STATUS.md) РІР‚вЂќ РЎвЂЎРЎвЂљР С• РЎС“Р В¶Р Вµ РЎР‚Р ВµР В°Р В»Р С‘Р В·Р С•Р Р†Р В°Р Р…Р С•
6. [MISSING_FUNCTIONALITY.md](../archive/docs-deprecated-2026-09-04/MISSING_FUNCTIONALITY.md) РІР‚вЂќ РЎвЂЎРЎвЂљР С• Р Р…РЎС“Р В¶Р Р…Р С• РЎР‚Р ВµР В°Р В»Р С‘Р В·Р С•Р Р†Р В°РЎвЂљРЎРЉ
7. [planning/CRITICAL_BLOCKERS_REMEDIATION_PLAN.md](planning/CRITICAL_BLOCKERS_REMEDIATION_PLAN.md) РІР‚вЂќ Р С”РЎР‚Р С‘РЎвЂљР С‘РЎвЂЎР ВµРЎРѓР С”Р С‘Р Вµ Р В±Р В»Р С•Р С”Р ВµРЎР‚РЎвЂ№
8. [CONTRIBUTING.md](../CONTRIBUTING.md) РІР‚вЂќ Р С—РЎР‚Р С•РЎвЂ Р ВµРЎРѓРЎРѓ Р Р†Р Р…Р ВµРЎРѓР ВµР Р…Р С‘РЎРЏ Р Р†Р С”Р В»Р В°Р Т‘Р В°
9. [planning/LOCAL_RELEASE_BUILD_MASTER_PLAN.md](planning/LOCAL_RELEASE_BUILD_MASTER_PLAN.md) РІР‚вЂќ РЎР‚Р ВµР В»Р С‘Р В·Р Р…Р В°РЎРЏ РЎРѓР В±Р С•РЎР‚Р С”Р В° Р С—Р С• РЎвЂљР С‘Р С—Р В°Р С Р Р†РЎвЂ№Р С—РЎС“РЎРѓР С”Р В°
10. [planning/RELEASE_GO_NO_GO_CHECKLIST.md](planning/RELEASE_GO_NO_GO_CHECKLIST.md) РІР‚вЂќ РЎвЂћР С‘Р Р…Р В°Р В»РЎРЉР Р…Р С•Р Вµ РЎР‚Р ВµРЎв‚¬Р ВµР Р…Р С‘Р Вµ GO/NO-GO

### Р вЂќР В»РЎРЏ РЎР‚Р В°Р В±Р С•РЎвЂљРЎвЂ№ РЎРѓ Р С”Р С•Р СР С—Р С•Р Р…Р ВµР Р…РЎвЂљР В°Р СР С‘

- **ONVIF Р С”Р В»Р С‘Р ВµР Р…РЎвЂљ:** [ONVIF_CLIENT.md](archive/docs-legacy-2026-04-27/ONVIF_CLIENT.md) РІвЂ вЂ™ [MISSING_FUNCTIONALITY.md](../archive/docs-deprecated-2026-09-04/MISSING_FUNCTIONALITY.md#onvifclient) РІвЂ вЂ™ [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md#1-xml-Р С—Р В°РЎР‚РЎРѓР С‘Р Р…Р С–-Р Т‘Р В»РЎРЏ-onvif)
- **RTSP Р С”Р В»Р С‘Р ВµР Р…РЎвЂљ:** [RTSP_CLIENT.md](archive/docs-legacy-2026-04-27/RTSP_CLIENT.md) РІвЂ вЂ™ [MISSING_FUNCTIONALITY.md](../archive/docs-deprecated-2026-09-04/MISSING_FUNCTIONALITY.md#rtspclient) РІвЂ вЂ™ [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md#2-rtsp-Р С”Р В»Р С‘Р ВµР Р…РЎвЂљ---Р С‘Р Р…РЎвЂљР ВµР С–РЎР‚Р В°РЎвЂ Р С‘РЎРЏ-live555)
- **WebSocket Р С”Р В»Р С‘Р ВµР Р…РЎвЂљ:** [WEBSOCKET_CLIENT.md](archive/docs-legacy-2026-04-27/WEBSOCKET_CLIENT.md) РІвЂ вЂ™ [MISSING_FUNCTIONALITY.md](../archive/docs-deprecated-2026-09-04/MISSING_FUNCTIONALITY.md#websocketclient)
- **Р вЂєР С‘РЎвЂ Р ВµР Р…Р В·Р С‘РЎР‚Р С•Р Р†Р В°Р Р…Р С‘Р Вµ:** [LICENSE_SYSTEM.md](archive/docs-legacy-2026-04-27/LICENSE_SYSTEM.md) РІвЂ вЂ™ [MISSING_FUNCTIONALITY.md](../archive/docs-deprecated-2026-09-04/MISSING_FUNCTIONALITY.md#licensemanager) РІвЂ вЂ™ [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md#5-Р С”РЎР‚Р С‘Р С—РЎвЂљР С•Р С–РЎР‚Р В°РЎвЂћР С‘РЎРЏ-Р Т‘Р В»РЎРЏ-Р В»Р С‘РЎвЂ Р ВµР Р…Р В·Р С‘РЎР‚Р С•Р Р†Р В°Р Р…Р С‘РЎРЏ)

### Р вЂќР В»РЎРЏ РЎР‚Р В°Р В·Р Р†Р ВµРЎР‚РЎвЂљРЎвЂ№Р Р†Р В°Р Р…Р С‘РЎРЏ

1. [DEPLOYMENT_GUIDE.md](../archive/docs/deployment/DEPLOYMENT_GUIDE.md) - Р С•Р В±РЎвЂ°Р ВµР Вµ РЎР‚РЎС“Р С”Р С•Р Р†Р С•Р Т‘РЎРѓРЎвЂљР Р†Р С•
2. [API.md](API.md) - API Р Т‘Р С•Р С”РЎС“Р СР ВµР Р…РЎвЂљР В°РЎвЂ Р С‘РЎРЏ
3. [REQUIRED_LIBRARIES.md](REQUIRED_LIBRARIES.md) - Р В·Р В°Р Р†Р С‘РЎРѓР С‘Р СР С•РЎРѓРЎвЂљР С‘
4. [DSM_SMOKE_AND_ROLLBACK.md](../archive/docs/guides/DSM_SMOKE_AND_ROLLBACK.md) - smoke/rollback runbook Р Т‘Р В»РЎРЏ Р С—Р В°Р С”Р ВµРЎвЂљР В° `ip-css` Р Р…Р В° DSM
5. [RELEASE_HANDOFF_SYNOLOGY.md](../archive/docs-duplicates-2026-08-08/RELEASE_HANDOFF_SYNOLOGY.md) - release handoff Р С—Р С• Р В°РЎР‚РЎвЂљР ВµРЎвЂћР В°Р С”РЎвЂљР В°Р С Р С‘ go/no-go Р Т‘Р В»РЎРЏ Synology

---

## СЂСџвЂњв‚¬ Р РЋРЎвЂљР В°РЎвЂљРЎС“РЎРѓ Р Т‘Р С•Р С”РЎС“Р СР ВµР Р…РЎвЂљР В°РЎвЂ Р С‘Р С‘

| Р вЂќР С•Р С”РЎС“Р СР ВµР Р…РЎвЂљ | Р РЋРЎвЂљР В°РЎвЂљРЎС“РЎРѓ | Р РЋР Р†РЎРЏР В·РЎРЉ |
|----------|--------|--------|
| DOCUMENTATION_INDEX.md | РІСљвЂ¦ Р вЂњР В»Р В°Р Р†Р Р…РЎвЂ№Р в„– Р С‘Р Р…Р Т‘Р ВµР С”РЎРѓ | Р С™Р С•РЎР‚Р ВµР Р…РЎРЉ Р С—РЎР‚Р С•Р ВµР С”РЎвЂљР В° |
| status/PROJECT_STATUS.md | РІСљвЂ¦ Р С’Р С”РЎвЂљРЎС“Р В°Р В»Р ВµР Р… | Р В¤Р В°Р В·РЎвЂ№, Р В±Р В»Р С•Р С”Р ВµРЎР‚РЎвЂ№, ~81% |
| TODO.md | РІСљвЂ¦ Р С’Р С”РЎвЂљРЎС“Р В°Р В»Р ВµР Р… | Р вЂўР Т‘Р С‘Р Р…РЎвЂ№Р в„– To-Do Р С‘ РЎвЂЎР ВµР С”Р В»Р С‘РЎРѓРЎвЂљРЎвЂ№ |
| planning/DETAILED_DEVELOPMENT_PLAN.md | РІСљвЂ¦ Р С’Р С”РЎвЂљРЎС“Р В°Р В»Р ВµР Р… | Р В­РЎвЂљР В°Р С—РЎвЂ№ Р С—Р С• РЎР‚Р В°Р В·Р Т‘Р ВµР В»Р В°Р С 1РІР‚вЂњ17 |
| planning/CRITICAL_BLOCKERS_REMEDIATION_PLAN.md | СЂСџСџРЋ Р вЂ™ Р С—РЎР‚Р С•РЎвЂ Р ВµРЎРѓРЎРѓР Вµ | 3/6 Р В±Р В»Р С•Р С”Р ВµРЎР‚Р С•Р Р† Р В·Р В°Р С”РЎР‚РЎвЂ№РЎвЂљР С• |
| ARCHITECTURE.md | РІСљвЂ¦ Р С’Р С”РЎвЂљРЎС“Р В°Р В»Р ВµР Р… | РІР‚вЂќ |
| IMPLEMENTATION_STATUS.md | РІСљвЂ¦ Р С›Р В±Р Р…Р С•Р Р†Р В»Р ВµР Р… | РІР‚вЂќ |
| MISSING_FUNCTIONALITY.md | РІСљвЂ¦ Р С’Р С”РЎвЂљРЎС“Р В°Р В»Р ВµР Р… | ONVIF, RTSP, License |
| RTSP_CLIENT.md, ONVIF_CLIENT.md, WEBSOCKET_CLIENT.md | РІСљвЂ¦ Р С›Р В±Р Р…Р С•Р Р†Р В»Р ВµР Р…РЎвЂ№ | Р РЋР С. DOCUMENTATION_INDEX |

---

## СЂСџвЂ вЂў Р СџР С•РЎРѓР В»Р ВµР Т‘Р Р…Р С‘Р Вµ Р С•Р В±Р Р…Р С•Р Р†Р В»Р ВµР Р…Р С‘РЎРЏ

### Р вЂќР ВµР С”Р В°Р В±РЎР‚РЎРЉ 2024

- РІСљвЂ¦ **Р С›Р В±Р Р…Р С•Р Р†Р В»Р ВµР Р…Р В° РЎРѓРЎвЂљРЎР‚РЎС“Р С”РЎвЂљРЎС“РЎР‚Р В° Git Р Р†Р ВµРЎвЂљР С•Р С”** - РЎРѓР С•Р В·Р Т‘Р В°Р Р…Р В° Р С—Р В»Р В°РЎвЂљРЎвЂћР С•РЎР‚Р СР С•-Р С•РЎР‚Р С‘Р ВµР Р…РЎвЂљР С‘РЎР‚Р С•Р Р†Р В°Р Р…Р Р…Р В°РЎРЏ РЎРѓРЎвЂљРЎР‚РЎС“Р С”РЎвЂљРЎС“РЎР‚Р В° Р Р†Р ВµРЎвЂљР С•Р С” РЎР‚Р В°Р В·РЎР‚Р В°Р В±Р С•РЎвЂљР С”Р С‘:
  - `dev/android`, `dev/ios`, `dev/desktop` - Р Р†Р ВµРЎвЂљР С”Р С‘ РЎР‚Р В°Р В·РЎР‚Р В°Р В±Р С•РЎвЂљР С”Р С‘ Р Т‘Р В»РЎРЏ Р С”Р В°Р В¶Р Т‘Р С•Р в„– Р С—Р В»Р В°РЎвЂљРЎвЂћР С•РЎР‚Р СРЎвЂ№
  - `test/android`, `test/ios`, `test/desktop` - Р Р†Р ВµРЎвЂљР С”Р С‘ РЎвЂљР ВµРЎРѓРЎвЂљР С‘РЎР‚Р С•Р Р†Р В°Р Р…Р С‘РЎРЏ Р Т‘Р В»РЎРЏ Р С”Р В°Р В¶Р Т‘Р С•Р в„– Р С—Р В»Р В°РЎвЂљРЎвЂћР С•РЎР‚Р СРЎвЂ№
  - Р С›Р В±Р Р…Р С•Р Р†Р В»Р ВµР Р…РЎвЂ№ [DEVELOPMENT.md](DEVELOPMENT.md) Р С‘ [CONTRIBUTING.md](../CONTRIBUTING.md) РЎРѓ Р С—Р С•Р Т‘РЎР‚Р С•Р В±Р Р…РЎвЂ№Р С Р С•Р С—Р С‘РЎРѓР В°Р Р…Р С‘Р ВµР С workflow
- РІСљвЂ¦ Р С›Р В±Р Р…Р С•Р Р†Р В»Р ВµР Р…РЎвЂ№ РЎРѓРЎРѓРЎвЂ№Р В»Р С”Р С‘ Р Р…Р В° GitHub РЎР‚Р ВµР С—Р С•Р В·Р С‘РЎвЂљР С•РЎР‚Р С‘Р в„– - Р В·Р В°Р СР ВµР Р…Р ВµР Р…РЎвЂ№ Р Р…Р В° Р В°Р С”РЎвЂљРЎС“Р В°Р В»РЎРЉР Р…РЎвЂ№Р в„– `RekadzeAV/IP-CSS`
- РІСљвЂ¦ Р РЋР С•Р В·Р Т‘Р В°Р Р… [MISSING_FUNCTIONALITY.md](../archive/docs-deprecated-2026-09-04/MISSING_FUNCTIONALITY.md) - Р Т‘Р ВµРЎвЂљР В°Р В»РЎРЉР Р…РЎвЂ№Р в„– Р В°Р Р…Р В°Р В»Р С‘Р В· Р Р…Р ВµРЎР‚Р ВµР В°Р В»Р С‘Р В·Р С•Р Р†Р В°Р Р…Р Р…Р С•Р С–Р С• РЎвЂћРЎС“Р Р…Р С”РЎвЂ Р С‘Р С•Р Р…Р В°Р В»Р В°
- РІСљвЂ¦ Р С›Р В±Р Р…Р С•Р Р†Р В»Р ВµР Р… [IMPLEMENTATION_STATUS.md](../archive/docs-duplicates-2026-08-08/IMPLEMENTATION_STATUS.md) - РЎвЂљР С•РЎвЂЎР Р…Р В°РЎРЏ Р С‘Р Р…РЎвЂћР С•РЎР‚Р СР В°РЎвЂ Р С‘РЎРЏ Р С• РЎРѓР ВµРЎвЂљР ВµР Р†РЎвЂ№РЎвЂ¦ Р С”Р В»Р С‘Р ВµР Р…РЎвЂљР В°РЎвЂ¦
- РІСљвЂ¦ Р С›Р В±Р Р…Р С•Р Р†Р В»Р ВµР Р… [DEVELOPMENT_PLAN.md](../archive/docs-deprecated-2026-09-04/DEVELOPMENT_PLAN.md) - Р Т‘Р С•Р В±Р В°Р Р†Р В»Р ВµР Р…РЎвЂ№ РЎРѓРЎРѓРЎвЂ№Р В»Р С”Р С‘ Р Р…Р В° Р Т‘Р ВµРЎвЂљР В°Р В»РЎРЉР Р…РЎвЂ№Р в„– Р В°Р Р…Р В°Р В»Р С‘Р В·
- РІСљвЂ¦ Р С›Р В±Р Р…Р С•Р Р†Р В»Р ВµР Р…РЎвЂ№ Р Т‘Р С•Р С”РЎС“Р СР ВµР Р…РЎвЂљРЎвЂ№ Р С”Р С•Р СР С—Р С•Р Р…Р ВµР Р…РЎвЂљР С•Р Р† - Р Т‘Р С•Р В±Р В°Р Р†Р В»Р ВµР Р…РЎвЂ№ РЎР‚Р В°Р В·Р Т‘Р ВµР В»РЎвЂ№ "Р РЋРЎвЂљР В°РЎвЂљРЎС“РЎРѓ РЎР‚Р ВµР В°Р В»Р С‘Р В·Р В°РЎвЂ Р С‘Р С‘" РЎРѓ РЎРѓРЎРѓРЎвЂ№Р В»Р С”Р В°Р СР С‘

---

**Р вЂ™Р ВµРЎР‚РЎРѓР С‘РЎРЏ Р Т‘Р С•Р С”РЎС“Р СР ВµР Р…РЎвЂљР В°РЎвЂ Р С‘Р С‘:** 2.1
**Р СџР С•РЎРѓР В»Р ВµР Т‘Р Р…Р ВµР Вµ Р С•Р В±Р Р…Р С•Р Р†Р В»Р ВµР Р…Р С‘Р Вµ:** 27 April 2026
- Auto-synced gate snapshot: strict "NO-GO", profile-aware "GO", runtime decision "GO"; field validation "PENDING"; final decision "NO-GO (until field validation complete)".









