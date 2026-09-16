# Project Structure (AI Baseline)

This document describes the current top-level directory intent.
It is used by AI assistants as a quick routing map before code changes.

## Top-Level Directories

- `android` - Android application and app-specific wiring
- `build` - generated build outputs
- `config` - shared runtime/build configuration
- `core` - core cross-platform modules (network/common)
- `data` - data assets, datasets, and model-related resources
- `docs` - project documentation and plans
- `gradle` - Gradle wrapper and build support
- `Inf-pipeline` - infrastructure/pipeline assets
- `native` - C++ modules for codecs/video/analytics
- `nginx` - reverse proxy/deployment configuration
- `platforms` - platform packaging/launch adapters
- `Release` - release artifacts and packaging resources
- `scripts` - automation scripts for build/test/release
- `server` - backend API and web frontend
- `shared` - Kotlin Multiplatform shared business/data logic
- `update-project` - migration and restructuring notes

## Key Module Notes

- `shared` is the main location for domain models, repositories, and use cases.
- `server/api` hosts Ktor routes, middleware, services, and security logic.
- `server/web` hosts the web client (React/Next.js stack).
- `native` must remain isolated from high-level domain/business rules.

## Placement Rules

1. New cross-platform business logic goes to `shared`.
2. New API endpoints/services go to `server/api`.
3. New web components/state/services go to `server/web`.
4. New native processing code goes to `native`.
5. Platform-specific packaging/deployment changes go to `platforms`.

## Maintenance

- Update this file when introducing a new top-level directory.
- Update this file when directory responsibility changes significantly.
