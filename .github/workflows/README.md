# CI/CD Workflows

> **Актуализация 15.09.2026.** Каталог сокращён с 24 до 13 workflows: удалены дубли/мёртвые пайплайны (нативные сборки ×4, CD-дубль, python-gates, phase1-mvp-verify, postgres-finalization one-command/report, nightly-live-integration). Оригиналы: [`archive/workflows-2026-09-15/`](../../archive/workflows-2026-09-15/).

## Критическая находка (15.09.2026): `allowed_actions: local_only`

Все workflows репозитория были **отключены вручную в мае–июне 2026** (`startup_failure` в статусах, 0 jobs). Корень проблемы: в настройках репозитория Actions было выставлено `allowed_actions: local_only` — запуск любых внешних actions (`actions/checkout@v4` и др.) блокировался на старте воркфлоу. `nightly.yml` при этом оставался активным и падал `startup_failure` ежедневно.

**Исправление:** `allowed_actions` переключён на `all` (через REST API 15.09.2026). Проверка настройки:
```
Settings → Actions → General → Actions permissions → "Allow all actions and reusable workflows"
```

## Актуальный состав (13 workflows)

| Workflow | Триггеры | Назначение |
|---|---|---|
| `ci.yml` | push/PR в `main`, dispatch | **Основной гейт**: detekt, ktlint, npm-audit, тесты (unit+integration), сервер installDist, Android APK, Docker+Trivy, KMP-гейты, summary |
| `nightly.yml` | schedule 02:00 UTC, dispatch | Полный тест-набор, Trivy fs-scan → SARIF, distribution smoke (installDist), сводка |
| `release.yml` | теги `v*`, dispatch | Валидация версии/CHANGELOG, сборка артефактов (JAR+NAS-пакеты), Docker push, GitHub Release |
| `build-nas-packages.yml` | теги `v*`/`Alfa-*`/`Beta-*`, dispatch | SPK/QPKG/APK/TrueNAS-пакеты + checksums (matrix) |
| `codeql.yml` | push/PR `main`, schedule Пн 03:00 | SAST java-kotlin + javascript-typescript → Security tab |
| `secret-scan.yml` | push/PR `main` | Gitleaks v3 |
| `container-scan.yml` | push/PR `main` (пути Dockerfile) | Trivy image-scan → SARIF |
| `dependency-verification.yml` | push/PR `main` | Gradle `--write-verification-metadata` (артефакт XML) |
| `gradle-wrapper-validation.yml` | push/PR `main` | `gradle/actions/wrapper-validation@v6` |
| `postgres-finalization-gate.yml` | PR/push `main` (пути server/api и др.), dispatch | Инварианты PostgreSQL-финализации (`check-postgres-finalization.sh`) |
| `ffmpeg-tests.yml` | push/PR `main` (пути native/video-processing, core/network video/rtsp), dispatch | CMake-сборка + нативные unit-тесты (matrix OS) |
| `rtsp-native-build.yml` | push/PR (пути native/video-processing), dispatch | Кросс-платформенная сборка native-видеобиблиотеки (Win/Linux/macOS) |
| `video-e2e-verify.yml` | dispatch, schedule Пн 12:00 | Валидация video-E2E профилей (`validate-video-e2e-profile.py`) |

## Политика версионируемых actions

- `actions/*` → checkout/setup-java/setup-node/setup-python v4; **upload/download-artifact v7** (бамп применён 15.09 из закрытого PR#7)
- `gradle/actions/*` → v6 (wrapper-validation); `gradle/actions/setup-gradle@v3` — допустимо (живой тег)
- `docker/*` → build-push v7, login v4, metadata/setup-buildx v5/v3
- `android-actions/setup-android` → v4
- `github/codeql-action/*` → v3
- `gitleaks/gitleaks-action` → v3
- `aquasecurity/trivy-action` → SHA-pinned (container-scan) / @master (nightly, ci — non-blocking режим)

Bump-PR Dependabot по github-actions (**#6–#10**) закрыты 15.09 как superseded: все бампы применены в `main` напрямую (gitleaks v3, build-push v7, login v4, setup-android v4, upload-artifact v7 ×18 замен).

## Известные ограничения

1. **Нативные сборки** (`ffmpeg-tests`, `rtsp-native-build`) требуют доналадки на runner'ах — исторически отключены; держатся как рабочие заготовки.
2. **Live-интеграция RTSP** (`ciLiveIntegrationSuite`) требует внешнего окружения камер (порт A1) — workflow удалён из расписания, таска осталась в `build.gradle.kts:313`.
3. **Secrets для релиза**: `DOCKER_USERNAME`/`DOCKER_PASSWORD`/`GH_TOKEN` (план 5.2) — push в ghcr.io через `GITHUB_TOKEN` настроен, внешний DockerHub требует secrets.
4. **`develop`-ветка удалена** (trunk-based модель): триггеры всех workflows нормализованы на `main`.

## Локальный запуск

```powershell
# Windows
.\scripts\ci\verify-kmp-phase1.ps1

# Python-гейты (как в ci.yml kmp job)
python scripts/ci/check-commonmain-forbidden-imports.py .
python scripts/ci/check-security-expect-actual-signatures.py .
python scripts/ci/check-no-jvm-deps-in-native-source-sets.py .
python scripts/ci/check-video-runtime-matrix-config.py --root .
python scripts/ci/validate-video-e2e-profile.py --root .
```

## Артефакты

| Артефакт | Workflow | Хранение |
|----------|----------|----------|
| coverage-report, test-results | ci.yml | default |
| server-libs, android-apk | ci.yml | default |
| nightly-test-report | nightly.yml | default |
| trivy-results (SARIF) | nightly.yml | Security tab |
| release-artifacts, NAS-пакеты | release.yml, build-nas-packages.yml | Release |
| dependency-verification-metadata | dependency-verification.yml | default |
| video-e2e-report (log+meta) | video-e2e-verify.yml | 14 дней |

