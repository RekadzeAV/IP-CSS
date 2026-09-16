# Docker Manual QA Auth + Web Check (2026-04-27)

## Purpose

Prepare and keep a stable Docker environment for physical/manual server testing, including browser-based authorization checks.

## Applied Configuration

Manual environment file: `.env.docker-manual`

- `ENVIRONMENT=development`
- `NODE_ENV=development`
- `ADMIN_PASSWORD=ADMIN_PASSWORD@`
- `DB_PASSWORD=ManualCheckDb#2026!Secure`
- `REDIS_PASSWORD=ManualCheckRedis#2026!Secure`
- `JWT_SECRET=ManualCheckJwt#2026-SecureRandomValue-For-Local`
- `DATA_ENCRYPTION_KEY=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef`
- `AUDIT_PERSIST_ENABLED=false`
- `CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080`

Manual compose override: `docker-compose.manual.yml`

- Keeps dedicated named volumes for manual QA runs.
- Exposes:
  - API: `8080`
  - PostgreSQL: `55432`
  - Redis: `56379`

## Container Restart and Preserve Strategy

Compose files used:

- `docker-compose.yml`
- `docker-compose.manual.yml`

Command used for restart with updated auth settings:

`docker compose --env-file .env.docker-manual -f docker-compose.yml -f docker-compose.manual.yml up -d`

Current status after restart:

- `ip-camera-surveillance` - Up (healthy)
- `surveillance-postgres` - Up (healthy)
- `surveillance-redis` - Up (healthy)
- Health endpoint: `GET http://localhost:8080/api/v1/health` -> `200`

This container stack is kept running for physical/manual testing and additional verification.

## Authorization Data for Manual Testing

- Application base URL: `http://localhost:8080`
- API login endpoint: `POST /api/v1/auth/login`
- Default admin login: `admin`
- Admin password: `ADMIN_PASSWORD@`

Notes:

- `ENVIRONMENT=development` is required for local browser auth checks over plain HTTP because secure cookie restrictions are relaxed.
- If database volumes are recreated, the default admin bootstrap is executed again with `ADMIN_PASSWORD`.

## Web Interface Check

Result: web interface is already implemented in this repository.

Evidence:

- Web app root exists: `server/web`
- Next.js configuration exists: `server/web/next.config.js`
- App routes/pages exist (examples):
  - `server/web/src/app/login/page.tsx`
  - `server/web/src/app/dashboard/page.tsx`
  - `server/web/src/app/cameras/page.tsx`
  - `server/web/src/app/events/page.tsx`
  - `server/web/src/app/recordings/page.tsx`
  - `server/web/src/app/settings/page.tsx`

Decision:

- No additional task for "implement web interface from scratch" is required.
- Continue with functional/manual validation of the existing web interface against API.

## Recommended Manual Validation Checklist

1. Open web login page and sign in with `admin / ADMIN_PASSWORD@`.
2. Verify authenticated API call (`/api/v1/users/me`) works with browser cookies.
3. Open dashboard/cameras/events/recordings/settings pages.
4. Verify unauthorized access flow after logout.
5. Verify token refresh does not break active session during navigation.
