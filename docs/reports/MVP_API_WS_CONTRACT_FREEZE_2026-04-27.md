# MVP API + WS Contract Freeze (2026-04-27)

## Purpose

Lock Phase 1 API and WebSocket contracts to prevent backward incompatible changes during final MVP hardening.

## Scope (Frozen)

- REST contract source: `docs/api/openapi.yaml`
- Core MVP endpoints: `health`, `auth`, `cameras`, `recordings`, `audit`
- WebSocket channels:
  - `cameras`
  - `events`
  - `recordings`
  - `notifications`

## Freeze Rules

1. No breaking field rename/removal in MVP DTOs.
2. No enum value removal for statuses/events used by web/android/desktop clients.
3. New fields are additive-only and must be nullable or defaulted.
4. Any contract extension requires:
   - OpenAPI update,
   - compatibility note in PR,
   - at least one integration test touchpoint.

## Backward Compatibility Notes

- Existing clients must continue to parse API responses without required schema migration.
- Existing WS subscribers must continue to receive known event types and payload keys.
- If introducing new WS event keys, old keys remain intact through MVP closure.

## Verification Evidence

- `:server:api:test --tests "*Hls*IntegrationTest"` -> PASS
- `scripts/ci/run-server-api-integration-compose.ps1` -> PASS
- `./gradlew.bat mvpAutomatedAcceptance` -> PASS

## Change Window

Contract freeze effective immediately and valid until formal Phase 1 GO/NO-GO closure.
