# KMP Phase 1 Progress (Platform Stabilization)

## Completed tasks

### KMP-001: Security/Encryption expect registry

Security-related `expect` declarations identified:

- `core/common/src/commonMain/kotlin/com/company/ipcamera/core/common/security/LocalDataEncryption.kt`
  - `expect class SecureLocalDataEncryption() : LocalDataEncryption`
- `core/common/src/commonMain/kotlin/com/company/ipcamera/core/common/security/PasswordEncryption.kt`
  - `expect class SecurePasswordEncryption() : PasswordEncryption`
- `core/common/src/commonMain/kotlin/com/company/ipcamera/core/common/security/MobileSecurityLogger.kt`
  - `expect class SecureMobileSecurityLogger() : MobileSecurityLogger`
- `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/auth/DigestCrypto.kt`
  - `expect object DigestCrypto`

### KMP-002..KMP-009: `actual` presence per platform

#### `SecureLocalDataEncryption`
- Android: `core/common/src/androidMain/kotlin/com/company/ipcamera/core/common/security/LocalDataEncryption.android.kt`
- Desktop/JVM: `core/common/src/desktopMain/kotlin/com/company/ipcamera/core/common/security/LocalDataEncryption.jvm.kt`
- iOS: `core/common/src/iosMain/kotlin/com/company/ipcamera/core/common/security/LocalDataEncryption.ios.kt`
- Native: `core/common/src/nativeMain/kotlin/com/company/ipcamera/core/common/security/LocalDataEncryption.native.kt`

#### `SecurePasswordEncryption`
- Android: `core/common/src/androidMain/kotlin/com/company/ipcamera/core/common/security/PasswordEncryption.android.kt`
- Desktop/JVM: `core/common/src/desktopMain/kotlin/com/company/ipcamera/core/common/security/PasswordEncryption.jvm.kt`
- iOS: `core/common/src/iosMain/kotlin/com/company/ipcamera/core/common/security/PasswordEncryption.ios.kt`
- Native: `core/common/src/nativeMain/kotlin/com/company/ipcamera/core/common/security/PasswordEncryption.native.kt`

#### `SecureMobileSecurityLogger`
- Android: `core/common/src/androidMain/kotlin/com/company/ipcamera/core/common/security/MobileSecurityLogger.android.kt`
- Desktop/JVM: `core/common/src/desktopMain/kotlin/com/company/ipcamera/core/common/security/MobileSecurityLogger.jvm.kt`
- iOS: `core/common/src/iosMain/kotlin/com/company/ipcamera/core/common/security/MobileSecurityLogger.ios.kt`
- Native: `core/common/src/nativeMain/kotlin/com/company/ipcamera/core/common/security/MobileSecurityLogger.native.kt`

#### `DigestCrypto`
- Android: `core/network/src/androidMain/kotlin/com/company/ipcamera/core/network/auth/DigestCrypto.android.kt`
- JVM: `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/auth/DigestCrypto.jvm.kt`
- iOS: `core/network/src/iosMain/kotlin/com/company/ipcamera/core/network/auth/DigestCrypto.ios.kt`
- Native Linux: `core/network/src/nativeLinuxMain/kotlin/com/company/ipcamera/core/network/auth/DigestCrypto.nativeLinux.kt`
- Native macOS Arm64: `core/network/src/nativeMacosArm64Main/kotlin/com/company/ipcamera/core/network/auth/DigestCrypto.nativeMacos.kt`
- Native macOS X64: `core/network/src/nativeMacosX64Main/kotlin/com/company/ipcamera/core/network/auth/DigestCrypto.nativeMacos.kt`
- Native Windows: `core/network/src/nativeWindowsMain/kotlin/com/company/ipcamera/core/network/auth/DigestCrypto.nativeWindows.kt`

Result: no missing `actual` found for the identified security/encryption `expect` declarations.

### KMP-014..KMP-017: commonMain platform API audit

Search scope: all `**/src/commonMain/kotlin/**/*.kt` under repository root.

Checked patterns:
- `import java.*`
- `import javax.*`
- `import android.*`
- fully-qualified usage of `java.*`, `javax.*`, `android.*`

Result: no forbidden platform imports/usages found in `commonMain`.

## In progress (historical)

### KMP-010..KMP-013: Signature-level verification (`expect/actual`)

Current status:
- Metadata compile checks succeeded:
  - `:core:common:compileKotlinMetadata`
  - `:core:network:compileKotlinMetadata`
- Extended target compile attempt (Android/Desktop/Native Windows) is blocked by unrelated native C++ build failures in `native/video-processing` during `:core:network:buildNativeVideoProcessingForCurrentPlatform`.

Blocker details:
- C++ compile errors in `native/video-processing/src/rtsp_client.cpp` and related native code prevent full target compile verification in current environment.

## Phase closure

KMP Phase 1 ("Архитектурная стабилизация KMP платформ") is considered complete.

Closure criteria satisfied:
- `expect/actual` security coverage and signature-level checks are enforced via CI and local one-shot verifier.
- `commonMain` platform boundary guards are enforced (`java.*`/`javax.*`/`android.*` forbidden).
- Source-set dependency leakage checks for `iosMain/nativeMain` are enforced.
- One-shot verifier reporting is integrated into CI artifacts and GitHub Job Summary.
- DoD checklist is fully marked and aligned with current automation.

Final recommended acceptance command:
- `python scripts/ci/verify-kmp-phase1.py --ci-profile --report-json diagnostics/kmp/verify-report.json`

## CI hardening implemented

### KMP-055 (implemented): forbidden platform API guard in `commonMain`

- Added script: `scripts/ci/check-commonmain-forbidden-imports.py`
- Added CI step in `.github/workflows/ci.yml`:
  - `Enforce commonMain platform API boundaries`

This step fails the pipeline if `java.*`, `javax.*`, or `android.*` imports/usages appear in any `src/commonMain/kotlin/**/*.kt`.

### KMP-056 (partially implemented): explicit expect/actual compile gate

- Added CI step in `.github/workflows/ci.yml`:
  - `Verify core/common expect-actual compilation`
- Command:
  - `./gradlew :core:common:compileReleaseKotlinAndroid :core:common:compileKotlinDesktop :core:common:compileKotlinNativeLinux --no-daemon`

This provides an early fail-fast check for `expect/actual` compatibility for core security classes in `core/common`.

### KMP-010..KMP-013 (implemented): signature-level expect/actual checks

- Added script: `scripts/ci/check-security-expect-actual-signatures.py`
- Added CI step in `.github/workflows/ci.yml`:
  - `Verify security expect-actual signatures`

The script validates:
- required `expect` declarations for security/encryption in `commonMain`;
- required `actual` declarations on Android/Desktop(i.e. JVM)/iOS/Native;
- signature consistency for parameters and return types for:
  - `SecureLocalDataEncryption`
  - `SecurePasswordEncryption`
  - `SecureMobileSecurityLogger`
  - `DigestCrypto`

### KMP-027..KMP-031 (implemented in CI gate form): source set chain validation

- Added CI step in `.github/workflows/ci.yml`:
  - `Validate KMP source set configuration`
- Command:
  - `./gradlew :core:common:checkKotlinGradlePluginConfigurationErrors :core:network:checkKotlinGradlePluginConfigurationErrors :shared:checkKotlinGradlePluginConfigurationErrors --no-daemon`

This adds fail-fast validation for Kotlin Multiplatform source set configuration issues.

### CI matrix stabilization (incremental implementation)

- Added CI step in `.github/workflows/ci.yml`:
  - `KMP compile smoke (common-safe targets)`
- Command:
  - `./gradlew :core:common:compileReleaseKotlinAndroid :core:common:compileKotlinDesktop :core:common:compileKotlinNativeLinux :core:network:compileKotlinMetadata :shared:compileKotlinMetadata --no-daemon`

Purpose:
- keep KMP compile checks strict for common/security paths;
- avoid known unrelated native C++ blocker in `core:network` full native/desktop build path while preserving fail-fast checks for multiplatform contracts.

### KMP-032..KMP-033 (implemented): guard against JVM dependency leakage to iOS/Native

- Added script: `scripts/ci/check-no-jvm-deps-in-native-source-sets.py`
- Added CI step in `.github/workflows/ci.yml`:
  - `Guard against JVM deps in ios/native source sets`

Policy:
- scans all `build.gradle.kts`;
- validates that `iosMain` and `nativeMain` dependency blocks do not contain JVM/Android-only dependency tokens (e.g. `libs.ktor.client.java`, `libs.ktor.client.okhttp`, `androidx.*`, `org.bytedeco:javacv`).

### KMP-059 (implemented): CONTRIBUTING KMP rules

- Updated `CONTRIBUTING.md`:
  - added mandatory Kotlin Multiplatform section with rules for:
    - `commonMain` platform API boundaries;
    - required `expect/actual` platform coverage;
    - no JVM/Android-only deps in `iosMain`/`nativeMain`.

### KMP-060 (implemented): DoD checklist formalization

- Added: `docs/kmp-phase1-dod-checklist.md`
- The checklist defines acceptance criteria for:
  - `expect/actual` consistency;
  - `commonMain` platform boundaries;
  - source set/dependency validation;
  - CI fail-fast guards;
  - documentation completeness.

### KMP-020..KMP-026 (implemented): security contract formalization

- Added: `docs/kmp-security-contract.md`
- The document formalizes:
  - encryption/decryption round-trip and fallback policy;
  - password encryption behavior and error semantics;
  - digest API format/length/determinism requirements;
  - security logging redaction/privacy rules.

### KMP-035..KMP-040 (implemented): contract tests

- Added common contract tests:
  - `core/common/src/commonTest/kotlin/com/company/ipcamera/core/common/security/MobileSecurityLoggerContractTest.kt`
- Added desktop encryption contract tests:
  - `core/common/src/desktopTest/kotlin/com/company/ipcamera/core/common/security/SecurityEncryptionDesktopContractTest.kt`
- Added digest contract tests:
  - `core/network/src/commonTest/kotlin/com/company/ipcamera/core/network/auth/DigestCryptoContractTest.kt`
- Updated `core/common/build.gradle.kts` test dependencies for MPP test source sets.
- Added CI step:
  - `Run security contract tests (desktop/common-safe)`

### KMP-049..KMP-052 (implemented): target build jobs in CI

- Originally: separate jobs `kmp-targets-linux`, `kmp-targets-macos`, `kmp-targets-windows`.
- **Current** `.github/workflows/ci.yml`:
  - `code-quality` runs root Gradle task **`ciKmpLinuxSanity`** (Android + Desktop + Native Linux + metadata + KGP checks in one graph).
  - **`kmp-cross-compile`** matrix (macOS + Windows): `ciKmpMacosIosCompile`, `ciKmpWindowsSanity` (root `build.gradle.kts`).
  - **`build-and-test`** needs `code-quality` and **`kmp-cross-compile`** (all matrix legs); одна Gradle-задача **`ciBuildCoreSharedModules`** (`:core:common:build`, `:core:network:build`, `:shared:build` — без отдельных `:test`, они уже в `build` → `check`).
  - **`.github/actions/setup-jdk-gradle`**: shared JDK 17 + Gradle cache (cache key from workflow `hashFiles`).

### KMP-041..KMP-048 (practical implementation): platform smoke coverage

- Added desktop platform smoke tests:
  - `core/common/src/desktopTest/kotlin/com/company/ipcamera/core/common/security/SecurityPlatformSmokeDesktopTest.kt`
- Smoke coverage includes:
  - `SecureMobileSecurityLogger` runtime invocation path;
  - `SecureLocalDataEncryption` factory path;
  - `SecurePasswordEncryption` factory path.
- Extended macOS CI compile target coverage:
  - now compiles both `compileKotlinIosX64` and `compileKotlinIosSimulatorArm64` (task graph: `ciKmpMacosIosCompile`).
- Windows KMP matrix leg:
  - `ciKmpWindowsSanity`: `compileKotlinNativeWindows` and desktop + metadata-adjacent targets.

### Process integration (implemented)

- Updated PR template `.github/pull_request_template.md`:
  - added KMP stabilization checklist for PRs touching `core/*` and `shared/*`.
- Updated high-level project docs:
  - `PROJECT_STRUCTURE.md` now links KMP stabilization artifacts.
  - `PROJECT_PROMPT.md` now links KMP stabilization artifacts.
  - `DOCUMENTATION_INDEX.md` now includes KMP stabilization artifacts in Security section and quick links.
  - `docs/README.md` now includes KMP stabilization navigation links.

### CI portability hardening (implemented)

- Added cross-platform Python checks:
  - `scripts/ci/check-commonmain-forbidden-imports.py`
  - `scripts/ci/check-security-expect-actual-signatures.py`
- Kept legacy Bash entrypoints as compatibility wrappers:
  - `scripts/ci/check-commonmain-forbidden-imports.sh` -> delegates to Python
  - `scripts/ci/check-security-expect-actual-signatures.sh` -> delegates to Python
- Updated CI workflow `.github/workflows/ci.yml` to use Python checks instead of Bash scripts for these gates.
- Result:
  - KMP fail-fast checks run consistently on Linux/macOS/Windows environments and local Windows setups.

### CI workflow simplification (implemented)

- Consolidated Python KMP gates in `.github/workflows/ci.yml` into a single canonical step:
  - `Run KMP Phase 1 Python gates (one-shot CI profile)`
  - command: `python scripts/ci/verify-kmp-phase1.py --ci-profile`
- Removed duplicated standalone Python gate steps from workflow in favor of one entrypoint.
- Re-validated full local one-shot run with Gradle after simplification:
  - `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/ci/verify-kmp-phase1.ps1`
  - all checks passed.
- Re-validated CI-equivalent Python gates locally:
  - `python scripts/ci/verify-kmp-phase1.py --ci-profile`
  - checks passed.

### Runtime matrix config validation (implemented)

- Added validator:
  - `scripts/ci/check-video-runtime-matrix-config.py`
- Integrated into one-shot verifier:
  - `scripts/ci/verify-kmp-phase1.py` now includes runtime-matrix config validation in Python gate phase.
- Added documentation references:
  - `README.md` KMP pre-PR checks section
  - `docs/automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md` (optional config validation command)
- Current local config warning is non-blocking by default:
  - enabled scenario with empty `playlistUrls` reports warning (not failure) unless strict mode is requested.
- Added strict-mode propagation in one-shot verifier wrappers:
  - Python: `--strict-runtime-matrix`
  - PowerShell: `-StrictRuntimeMatrix`
  - Bash: `--strict-runtime-matrix`
- Local runtime matrix config aligned for strict mode:
  - `config/video-runtime-matrix.local.json` enabled desktop scenario now has non-empty `playlistUrls`.
- Strict fast-run verification succeeded:
  - `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/ci/verify-kmp-phase1.ps1 -StrictRuntimeMatrix -SkipGradle`.

### Acceptance profile config validation (implemented)

- Upgraded validator:
  - `scripts/ci/validate-video-e2e-profile.py`
  - now validates `video-e2e-acceptance-profile.example.json`, `video-e2e-acceptance-profile.mvp-ci.json`, and optional `video-e2e-acceptance-profile.local.json`.
- Integrated into one-shot verifier:
  - `scripts/ci/verify-kmp-phase1.py` now runs acceptance profile validation in Python gate phase.
- Documentation updated:
  - `README.md` and `docs/automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md`.
  - `DOCUMENTATION_INDEX.md` and `docs/README.md` include strict/individual validator commands.

### Strict end-to-end local validation (completed)

- Full strict one-shot run with Gradle completed successfully:
  - `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/ci/verify-kmp-phase1.ps1 -StrictRuntimeMatrix`
- Verified in one run:
  - commonMain forbidden imports check,
  - expect/actual signature check,
  - no JVM deps in ios/native source sets,
  - runtime matrix strict validation,
  - video e2e profile validation,
  - metadata compilation,
  - desktop tests,
  - native Windows compilation.

### Verifier UX improvements (implemented)

- Added `--ci-profile` flag in `scripts/ci/verify-kmp-phase1.py`:
  - equivalent to `--skip-gradle --strict-runtime-matrix`.
  - canonical CI/local strict Python-gates entrypoint.
- Added wrapper-level CI profile aliases:
  - PowerShell: `-CiProfile`
  - Bash: `--ci-profile`
- Added machine-readable reporting:
  - `--report-json <path>` in `scripts/ci/verify-kmp-phase1.py`
  - PowerShell wrapper passthrough: `-ReportJson <path>`
- Improved stdout determinism with `flush=True` for verifier status lines (`[RUN]/[OK]/[FAIL]` and header/footer).
- Updated docs/commands:
  - `README.md`, `CONTRIBUTING.md`, `docs/automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md`.

### CI artifact integration (implemented)

- CI one-shot Python gate step now generates report:
  - `python scripts/ci/verify-kmp-phase1.py --ci-profile --report-json diagnostics/kmp/verify-report.json`
- Added artifact upload in CI:
  - artifact name: `kmp-phase1-verify-report`
  - path: `diagnostics/kmp/verify-report.json`
- Added markdown rendering + job summary integration:
  - renderer script: `scripts/ci/render-kmp-verify-report.py`
  - markdown output: `diagnostics/kmp/verify-report.md`
  - appended to `GITHUB_STEP_SUMMARY` in CI.

### Developer workflow alignment (implemented)

- Updated `CONTRIBUTING.md` with a local KMP-gates command block (same checks as CI).
- Updated `README.md` with mandatory KMP pre-PR checks for `core/*` and `shared/*` changes.
- Added one-shot local verification entrypoint:
  - `scripts/ci/verify-kmp-phase1.py`
  - runs all Phase 1 Python gates and key Gradle checks in a single command.
- Added Windows PowerShell wrapper:
  - `scripts/ci/verify-kmp-phase1.ps1`
  - supports `-SkipGradle` fast mode for quick pre-checks.
- Added Unix shell wrapper:
  - `scripts/ci/verify-kmp-phase1.sh`
  - supports `--skip-gradle` pass-through for quick pre-checks.

### Local full verification status (validated)

- Fixed Windows-specific verifier issue:
  - `verify-kmp-phase1.py` now uses `gradlew.bat` on Windows and `./gradlew` on Unix-like systems.
- Full one-shot verification with Gradle completed successfully on Windows:
  - `powershell -NoProfile -ExecutionPolicy Bypass -File scripts/ci/verify-kmp-phase1.ps1`
  - all Python gates and Gradle checks passed.

### Documentation consistency (implemented)

- Added one-shot verifier references to `DOCUMENTATION_INDEX.md` (Security section + quick links).
- Added one-shot verifier launch hints to `docs/README.md` development section.
- Added one-shot verifier hints to:
  - `docs/kmp-phase1-dod-checklist.md` (local acceptance command block),
  - `.github/pull_request_template.md` (KMP stabilization checklist item).
- Extended strict quick links with wrapper aliases in:
  - `DOCUMENTATION_INDEX.md`,
  - `docs/README.md`.
- Added one-shot verifier section to automation runbook:
  - `docs/automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md` (§ 2.1 KMP stabilization gates).

### KMP-053..KMP-054 (implemented): tests are now hard-gated in CI

- Updated `.github/workflows/ci.yml` test steps in `build-and-test`:
  - removed `|| true` from `:core:common:test`
  - removed `|| true` from `:core:network:test`
  - removed `|| true` from `:core:license:test`
  - removed `|| true` from `:shared:test`

Result: test failures now fail PR pipeline instead of being soft-ignored.

### KMP-056/KMP-057 (finalized): missing-actual and signature gates

- Added explicit CI step:
  - `Verify no missing actual declarations`
- Command:
  - `./gradlew :core:common:compileKotlinMetadata :core:network:compileKotlinMetadata :shared:compileKotlinMetadata --no-daemon`

Combined with existing signature script checks, this closes both declaration presence and signature consistency gates.

