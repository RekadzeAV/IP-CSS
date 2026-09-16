# NAS Release Publish Checklist

**Release:** Alfa-0.1.1  
**Date:** YYYY-MM-DD  
**Owner:** <name>

## 1) Build and Artifacts

- [x] `:platforms:nas-x86_64:build:buildAllNasPackages` completed
- [x] `:platforms:nas-arm:build:buildAllNasPackages` completed
- [x] Artifacts exist in `build/` (`.spk`, `.qpkg`, `.apk`, `truenas-*`)
- [x] Checksums generated in `build/checksums/*.sha256`

## 2) Validation

- [x] Local smoke scripts passed (`scripts/test-nas-build.ps1` and/or `.sh`)
- [ ] Platform smoke reports filled (Synology, QNAP, Asustor, TrueNAS)
- [x] Aggregator updated: `docs/reports/NAS_GO_NO_GO_AGGREGATOR_2026-04-26.md`
- [ ] Final decision approved (`GO` / `CONDITIONAL GO` / `NO-GO`)

## 3) Release Metadata

- [x] Version in artifacts matches target release
- [x] Release notes prepared
- [x] Known limitations documented
- [x] Rollback procedure attached

## 4) Publication

- [ ] Upload artifacts to release storage
- [ ] Upload checksum files
- [ ] Publish links for each platform package
- [ ] Communicate release package matrix to stakeholders

## 5) Post-publish Verification

- [ ] Download-and-verify checksum from published location
- [ ] Install sanity check from published artifact (at least one platform)
- [ ] Open issue list for post-release tracking

## Current status note

- This checklist is partially completed by automated local execution.
- Final checkboxes depend on real NAS field validation and publication workflow.
- Main navigation document: `docs/reports/NAS_RELEASE_MASTER_INDEX_2026-04-26.md`.
- Final SOP handoff: `docs/reports/NAS_RELEASE_HANDOFF_PACKET_2026-04-26.md`.
