# E2E Checklist: Discovery -> Stream -> Event

## Goal

Validate the MVP end-to-end scenario required by Phase 2:
camera discovery -> successful connect -> playback stream -> event visible in clients.

## Pre-conditions

- API server is running and healthy (`/api/v1/health`).
- At least one test camera is reachable from test environment.
- `config/video-e2e-acceptance-profile.example.json` is valid (CI также проверяет `video-e2e-acceptance-profile.mvp-ci.json` через `scripts/ci/validate-video-e2e-profile.py`).

## Checklist

- [ ] Camera appears in discovery results (ONVIF/WS-Discovery path).
- [ ] Connection test returns success with auth applied.
- [ ] Stream start succeeds and playlist/stream endpoint is reachable.
- [ ] Android client opens live view without fatal playback errors.
- [ ] Web client opens HLS/video playback path without fatal playback errors.
- [ ] Event is generated and appears in API payload and client UI.
- [ ] Event acknowledgement flow updates state in API and client.

## Required Evidence

- CI log link with strict gates green.
- Runtime probe output (acceptance script and/or profile runbook).
- Short incident note for each flaky failure (root cause + mitigation).

## Gate Decision

- **Go**: all checklist items complete with evidence.
- **No-Go**: any item missing evidence or failing repeatedly.
