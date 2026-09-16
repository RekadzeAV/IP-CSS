# IP-CSS Project: Git & Execution Overview

**Дата:** 2026-01-27

---

## 📦 Git Configuration

### Репозиторий
```
Branch: chore/kmp-phase1-closure-reporting
Status: Ahead of origin by 5 commits
```

### Git Hooks
- **Location:** `.git/hooks/`
- **Setup script:** `scripts/setup-git-hooks.sh`

**Доступные hooks:**
- `pre-commit` - Проверка кода перед коммитом
  - Проверка запрещённых импортов в commonMain
  - Проверка expect/actual сигнатур
  - Валидация секретов

**Установка:**
```bash
# Linux/macOS
./scripts/setup-git-hooks.sh

# Windows PowerShell
.\scripts\setup-git-hooks.ps1  # (если существует)
```

### Git Ignore
**Файл:** `.gitignore`

**Основные категории игнорируемых файлов:**

#### Build artifacts
```
.gradle/
build/
*.class
*.jar
*.aar
*.so
*.dylib
*.dll
*.apk
*.aab
```

#### IDE files
```
.idea/
*.iml
.vscode/
*.swp
*.swo
.cursor/
```

#### Secrets & credentials
```
.env
.env.local
*.pem
*.key
*.p12
*.keystore
secrets/
credentials.json
```

#### OS files
```
.DS_Store
Thumbs.db
*.log
logs/
```

#### Local configs
```
config/test-cameras.local.json
data/postgres/
diagnostics/
PROJECT_STRUCTURE_AUTO.md
```

---

## 🔄 CI/CD Pipelines

### GitHub Actions Workflows

**Location:** `.github/workflows/`

#### 1. **CI Pipeline** (`ci.yml`)
**Триггеры:**
- Push: `develop`, `feature/*`, `release/*`
- PR: `main`, `develop`

**Jobs:**
1. `docs-link-check-active-scope` - Проверка ссылок в документации
2. `code-quality` - Статический анализ
3. `kmp-cross-compile` - KMP кросс-компиляция (macOS, Windows)
4. `build-and-test` - Сборка и тесты
5. `server-api-compose-integration` - Docker интеграция
6. `mvp-automated-acceptance` - MVP acceptance tests

**Команды Gradle:**
```bash
./gradlew :core:common:checkKotlinGradlePluginConfigurationErrors
./gradlew :core:network:checkKotlinGradlePluginConfigurationErrors
./gradlew :shared:checkKotlinGradlePluginConfigurationErrors
./gradlew detekt
./gradlew ciKmpLinuxSanity
./gradlew ciKmpMacosIosCompile
./gradlew ciKmpWindowsSanity
./gradlew ciBuildCoreSharedModules
./gradlew ciUnitSmokeSuite
./gradlew ciCoverageGate
```

**Python gates:**
```bash
python scripts/ci/verify-kmp-phase1.py --ci-profile
python scripts/ci/validate-video-e2e-profile.py
python scripts/ci/check-doc-links-active.py
```

#### 2. **CD Pipeline** (`cd.yml`)
**Триггеры:**
- Push tags: `v*.*.*`
- Manual dispatch

**Jobs:**
1. `build-and-push` - Build Docker image, push to GHCR
2. `create-release` - Create GitHub release

**Команды:**
```bash
./gradlew :server:api:build --no-daemon
docker build -t ghcr.io/{repo}:{version} .
docker push ghcr.io/{repo}:{version}
```

#### 3. **Other Workflows**
- `build-nas-packages.yml` - Synology/QNAP packages
- `build-native-libraries.yml` - C++ native libs
- `codeql.yml` - Security analysis
- `dependency-verification.yml` - Dependency checks
- `ffmpeg-tests.yml` - FFmpeg tests
- `nightly-live-integration.yml` - Nightly tests
- `phase1-mvp-verify.yml` - MVP verification
- `postgres-finalization-*` - PostgreSQL gates
- `secret-scan.yml` - Secret scanning
- `video-e2e-verify.yml` - Video E2E tests

---

## 🚀 Local Execution

### Gradle Tasks (Root)

**Основные задачи:**

#### Сборка
```bash
# Clean build
./gradlew clean build --no-daemon

# Build core modules only
./gradlew :core:common:build :core:network:build :shared:build --no-daemon

# Build server API
./gradlew :server:api:build --no-daemon

# Build NAS packages
./gradlew buildNasPackages
```

#### Специализированные задачи
```bash
# KMP checks
./gradlew ciKmpLinuxSanity --no-daemon
./gradlew ciKmpMacosIosCompile --no-daemon
./gradlew ciKmpWindowsSanity --no-daemon

# Tests
./gradlew ciUnitSmokeSuite --no-daemon
./gradlew :core:common:test --no-daemon
./gradlew :core:network:test --no-daemon
./gradlew :shared:test --no-daemon

# Coverage
./gradlew ciCoverageGate --no-daemon

# KMP Phase 1 verification
./gradlew ciBuildCoreSharedModules --no-daemon

# MVP acceptance
./gradlew mvpAutomatedAcceptance --no-daemon

# Clean
./gradlew clean --no-daemon
./gradlew cleanNativeLibraries --no-daemon

# Daemon control
./gradlew --stop
```

#### Helper tasks
```bash
# Version management
./gradlew getVersion
./gradlew incrementVersion
./gradlew updateDocumentationVersion

# Documentation
./gradlew generateDocumentation
./gradlew prepareDocumentation
./gradlew dokkaHtml

# Publishing
./gradlew publishToLocalMaven
./gradlew :core:common:publishToMavenLocal
```

### Docker Commands

**Основной compose:**
```bash
# Start all services
docker-compose up -d

# Build and start
docker-compose build
docker-compose up -d

# Stop
docker-compose down

# View logs
docker-compose logs -f surveillance

# Single service
docker-compose up -d surveillance
docker-compose up -d postgres
docker-compose up -d redis
```

**Специализированные compose:**
```bash
# Development
docker-compose -f docker-compose.dev.yml up -d

# Integration tests
docker-compose -f docker-compose.integration.yml up -d

# Manual testing
docker-compose -f docker-compose.manual.yml up -d
```

### Python Scripts

**CI/CD gates:**
```bash
# KMP Phase 1 verification (full)
python scripts/ci/verify-kmp-phase1.py

# CI profile (skip Gradle)
python scripts/ci/verify-kmp-phase1.py --ci-profile

# Strict mode
python scripts/ci/verify-kmp-phase1.py --strict-runtime-matrix

# Individual checks
python scripts/ci/check-commonmain-forbidden-imports.py
python scripts/ci/check-security-expect-actual-signatures.py
python scripts/ci/check-no-jvm-deps-in-native-source-sets.py
python scripts/ci/check-video-runtime-matrix-config.py
python scripts/ci/validate-video-e2e-profile.py
python scripts/ci/check-doc-links-active.py
python scripts/ci/render-kmp-verify-report.py --input diagnostics/kmp/verify-report.json --output diagnostics/kmp/verify-report.md
```

**MVP acceptance:**
```bash
# Full acceptance
bash scripts/ci/mvp-automated-acceptance.sh --generate-phase1-summary
```

### PowerShell Scripts (Windows)

**Build & test:**
```powershell
# Full CI
.\scripts\ci-run-all.ps1

# RTSP tests
.\scripts\run-rtsp-test-with-checks.ps1
.\scripts\run-rtsp-long-run-stability-test.ps1

# ONVIF tests
.\scripts\run-phase1-onvif-acceptance.ps1
.\scripts\run-phase1-onvif-gate.ps1

# MVP acceptance
.\scripts\phase1-critical-e2e-smoke.ps1
.\scripts\run-mvp-acceptance-with-infra.ps1

# Video decoder
.\scripts\test-video-decoder.ps1
.\scripts\profile-video-decoder.ps1

# Desktop smoke
.\scripts\desktop-video-event-longrun-smoke.ps1

# Android smoke
.\scripts\android-video-background-smoke.ps1

# NAS tests
.\scripts\test-nas-build.ps1

# Web interface
.\scripts\run-web-interface-acceptance-pipeline.ps1

# Auth discovery
.\scripts\server-auth-discovery-smoke.ps1

# Recording tests
.\scripts\run-recording-ws-lifecycle-acceptance.ps1

# Network layer
.\scripts\run-network-layer-automation.ps1

# Postgres finalization
.\scripts\run-postgres-finalization-suite.ps1

# FFmpeg tests
.\scripts\run-ffmpeg-tests.ps1
```

**Setup & install:**
```powershell
# Full setup
.\scripts\bootstrap-local-dev-env.ps1
.\scripts\install-dependencies.ps1

# FFmpeg
.\scripts\install-ffmpeg.ps1
.\scripts\install-ffmpeg-dev.ps1

# Gradle
.\scripts\install-gradle.ps1

# Android SDK
.\scripts\install-android-sdk.ps1

# VS Code extensions
.\scripts\install-vscode-extensions.ps1

# Models
.\scripts\download-models.ps1
.\scripts\download-yolo-models.ps1
.\scripts\download-face-recognition-models.ps1
.\scripts\download-behavior-analysis-models.ps1
.\scripts\download-crowd-density-models.ps1

# Test cameras
.\scripts\setup-test-cameras.ps1
.\scripts\setup-test-cameras-config.ps1
```

**Build native libs:**
```powershell
# All native libs
.\scripts\build-all-native-libs.ps1

# Specific libs
.\scripts\build-video-processing-lib.ps1
.\scripts\build-rtsp-native-libs.ps1
.\scripts\build-android-native-libs.ps1
.\scripts\build-ios-native-libs.ps1
```

**CI/CD & acceptance:**
```powershell
# MVP acceptance
.\scripts\mvp-automated-acceptance.ps1

# Generate reports
.\scripts\generate-phase1-go-no-go-summary.ps1
.\scripts\generate-phase1-onvif-evidence-report.ps1
.\scripts\generate-f2-results-from-lighthouse.ps1

# Evidence pack
.\scripts\android-device-evidence-pack.ps1
.\scripts\postgresql-staging-evidence-pack.ps1

# Results processing
.\scripts\process-c4-results.ps1
.\scripts\process-f2-results.ps1
.\scripts\analyze-test-results.ps1
.\scripts\analyze-camera-test-results.ps1

# Show status
.\scripts\show-latest-phase1-onvif-decision.ps1
.\scripts\show-latest-w4-gate-status.ps1
.\scripts\show-latest-network-layer-1-4-decision.ps1
.\scripts\show-web-interface-acceptance-status.ps1

# Sync status
.\scripts\sync-network-layer-1-4-status.ps1
.\scripts\sync-release-status-docs.ps1

# NAS field
.\scripts\nas-field-aggregate.ps1
.\scripts\nas-field-apply-results.ps1
.\scripts\nas-field-auto-orchestrator.ps1
.\scripts\nas-field-finalize.ps1
.\scripts\nas-field-full-finalize.ps1
.\scripts\nas-field-readiness-check.ps1
.\scripts\nas-field-results-preflight.ps1
```

**Postgres finalization:**
```powershell
.\scripts\postgres-finalization-generate-go-no-go.ps1
.\scripts\postgres-finalization-generate-staging-report.ps1
.\scripts\postgres-finalization-preflight.ps1
.\scripts\postgres-finalization-staging-smoke.ps1
.\scripts\start-postgres-finalization-staging.ps1
.\scripts\validate-postgres-finalization-report.ps1
```

**Utilities:**
```powershell
# Cleanup
.\scripts\cleanup-old-branches.ps1
.\scripts\cleanup-web-interface-automation-artifacts.ps1

# Version
.\scripts\increment-version.ps1

# Documentation
.\scripts\archive-documentation.ps1
.\scripts\manage-documentation.ps1
.\scripts\update-documentation.ps1
.\scripts\update-dates.ps1

# Generate
.\scripts\generate-cinterop-bindings.ps1
.\scripts\generate-compile-commands.ps1
.\scripts\create-package-icons.py
.\scripts\generate-project-structure.py
.\scripts\generate-readiness-charts.py

# Git
.\scripts\cleanup-old-branches.ps1
.\scripts\create-platform-branches.ps1

# IDE
.\scripts\check-ide-setup.ps1
.\scripts\quick-install.ps1

# Release
.\scripts\server-export-release-artifacts.ps1
.\scripts\get-release-dir.ps1

# Monitoring
.\scripts\monitor-ffmpeg-performance.ps1
.\scripts\monitor-test-performance.ps1
.\scripts\certificate-pinning/monitor-certificate-pinning.sh

# Demo tests
.\scripts\run-demo-tests.ps1

# Security
.\scripts\security-field-staging-validation.ps1
.\scripts\security-mvp-readiness-check.ps1
.\scripts\get-certificate-fingerprint.ps1
.\scripts\check-certificate-expiry.sh

# Video
.\scripts\test-hls-integration.ps1

# Mobile
.\scripts\mobile-desktop-1-7-auto-closure.ps1
.\scripts\android-permissions-revoke-recover-smoke.ps1

# ONVIF
.\scripts\onvif-events-api-verification.ps1
.\scripts\onvif-events-resilience-verification.ps1
.\scripts\onvif-manual-verification.ps1
.\scripts\onvif-user-rights-check.ps1
.\scripts\validate-onvif-test-camera-config.ps1
.\scripts\validate-phase1-onvif-artifacts.ps1

# Phase 3 (Analytics)
.\scripts\phase3-analytics-auto-execution.ps1
.\scripts\phase3-auto-execution.ps1
.\scripts\phase3-continue-auto.ps1
.\scripts\phase3-desktop-auto-execution.ps1
.\scripts\phase3-field-validation-pack.ps1

# W4 MVP
.\scripts\w4-mvp-platform-and-gate.ps1
.\scripts\run-w4-and-diagnose.ps1
.\scripts\run-w4-profile.ps1

# Recording
.\scripts\recording-ws-lifecycle-acceptance-evidence.ps1

# Web
.\scripts\publish-web-interface-automation-status.ps1

# Server
.\scripts\server-metrics-guard.ps1
.\scripts\server-nightly-gate.ps1
.\scripts\server-pre-release-gate.ps1

# Network
.\scripts\check-network-layer-ci-prerequisites.ps1
.\scripts\run-network-layer-1-4-local-smoke.ps1

# Misc
.\scripts\desktop-video-event-longrun-smoke.ps1
.\scripts\restart-api-local.ps1
.\scripts\publish-local.sh
.\scripts\activate-rtsp-client.sh
.\scripts\activate-native-decoder.ps1
.\scripts\build-phase2-native.ps1
```

### Bash Scripts (Linux/macOS)

**Build:**
```bash
# All platforms
./scripts/build-all-platforms.sh

# Native libs
./scripts/build-all-native-libs.sh
./scripts/build-video-processing-linux.sh
./scripts/build-video-processing-macos.sh

# NAS packages
./scripts/build-nas-package.sh

# iOS
./scripts/build-ios.sh
./scripts/build-ios-native-libs.sh

# Android
./scripts/build-android.sh
./scripts/build-android-native-libs.sh

# FFmpeg
./scripts/build-video-processing-docker.sh
./scripts/build-video-processing-linux.sh
```

**CI/CD:**
```bash
# MVP acceptance
./scripts/ci/mvp-automated-acceptance.sh --generate-phase1-summary

# Server integration
./scripts/ci/run-server-api-integration-compose.sh

# KMP verification
./scripts/ci/verify-kmp-phase1.sh
./scripts/ci/check-commonmain-forbidden-imports.sh
./scripts/ci/check-security-expect-actual-signatures.sh
```

**Setup:**
```bash
# Dependencies
./scripts/install-dependencies.sh
./scripts/install-ffmpeg.sh
./scripts/bootstrap-local-dev-env.sh

# Test environment
./scripts/setup-test-environment.sh
./scripts/setup-test-cameras.sh
./scripts/setup-git-hooks.sh
```

**Tests:**
```bash
# RTSP
./scripts/run-rtsp-test-with-checks.ps1
./scripts/run-rtsp-long-run-stability-test.ps1

# Video decoder
./scripts/test-video-decoder.sh
./scripts/profile-video-decoder.sh

# FFmpeg
./scripts/run-ffmpeg-tests.sh

# Demo tests
./scripts/run-demo-tests.sh

# Analyze
./scripts/analyze-test-results.sh
```

**Release:**
```bash
# Synology
./scripts/prepare-synology-release.sh
./scripts/build-synology-spk.sh
```

**Monitoring:**
```bash
./scripts/monitor-certificate-pinning.sh
./scripts/check-certificate-expiry.sh
./scripts/check-timeline.sh
```

### Direct Execution

**Gradle wrapper:**
```bash
# Linux/macOS
./gradlew <task>

# Windows
gradlew.bat <task>
```

**Node.js (Web interface):**
```bash
cd server/web
npm install
npm run dev        # Development server
npm run build      # Production build
npm run lint       # ESLint
npm run type-check # TypeScript check
npm test           # Tests
```

**Docker:**
```bash
# Build
docker build -t ip-camera-surveillance .

# Run
docker run -p 8080:8080 \
  -e DB_PASSWORD=your_password \
  -e REDIS_PASSWORD=your_redis_password \
  -e JWT_SECRET=your_jwt_secret \
  ip-camera-surveillance

# With compose
docker-compose up -d
```

**Python:**
```bash
# CI scripts
python scripts/ci/<script>.py [options]

# Utilities
python scripts/find-duplicates.py
python scripts/generate-project-structure.py
python scripts/update-documentation.py
python scripts/create-package-icons.py
python scripts/generate-readiness-charts.py
```

---

## 📋 Quick Reference

### Common Commands

**Stop Gradle daemons:**
```bash
./gradlew --stop
# or
gradlew.bat --stop
```

**Clean project:**
```bash
./gradlew clean
```

**Build all:**
```bash
./gradlew build --no-daemon
```

**Run tests:**
```bash
./gradlew test --no-daemon
```

**Check dependencies:**
```bash
./gradlew dependencies
```

**Format code:**
```bash
./gradlew ktlintFormat
```

**Check formatting:**
```bash
./gradlew ktlintCheck
```

### Environment Setup

**Required environment variables:**
```bash
export DB_PASSWORD=your_password
export REDIS_PASSWORD=your_redis_password
export JWT_SECRET=your_jwt_secret
export ENVIRONMENT=production
```

**Using .env file:**
```bash
cp .env.example .env
# Edit .env with your values
docker-compose up -d
```

---

## 📊 Execution Summary

| Category | Count | Location |
|----------|-------|----------|
| GitHub Workflows | 14 | `.github/workflows/` |
| PowerShell Scripts | 100+ | `scripts/*.ps1` |
| Bash Scripts | 40+ | `scripts/*.sh` |
| Python Scripts | 20+ | `scripts/*.py`, `scripts/ci/*.py` |
| Gradle Tasks | 50+ | `build.gradle.kts`, `settings.gradle.kts` |
| Docker Compose | 4 | `docker-compose*.yml` |

---

## 🔐 Security

**Secrets Management:**
- All secrets via `.env` (ignored in `.gitignore`)
- No hardcoded credentials in code
- CI/CD uses GitHub Secrets
- Docker uses environment variables

**Git Protection:**
- Pre-commit hooks scan for secrets
- Dependency review action checks for vulnerabilities
- Secret scan workflow on push/PR

---

## 📦 Artifacts

**Build outputs:**
- `build/` - Gradle build artifacts
- `release-build/` - Release packages
- `diagnostics/` - Test reports (ignored)
- `coverage/` - Test coverage (ignored)

**Docker images:**
- `ghcr.io/{org}/ip-css:{tag}` - Container registry

**GitHub Releases:**
- Tags `v*.*.*` trigger automatic release
- Changelog auto-generated from git history

---

**Последнее обновление:** 2026-01-27
**Статус:** Актуально
