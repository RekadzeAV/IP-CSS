# Архив скриптов IP-CSS

Эта папка содержит устаревшие, дублирующие или неиспользуемые скрипты.

## 📅 Дата архивации: 2025-01-15

## 📋 Классификация архива

### Категория 1: Дубликаты сборки (заменены на build-pipeline.sh)

| Файл | Причина | Заменён на |
|------|---------|------------|
| `build-all-platforms.sh` | Устаревший, не учитывает структуру v2.0 | `scripts/build.sh` |
| `build-all-native-libs.sh` / `.ps1` | Частичная сборка, нет валидации | `scripts/build.sh --native` |
| `build-native-lib.sh` / `.ps1` | Устаревший API | `scripts/build.sh` |
| `build-video-processing-lib.sh` / `.ps1` | Узкоспециализированный | `scripts/build.sh` |
| `build-video-processing-linux.sh` / `.ps1` | Узкоспециализированный | `scripts/build.sh` |
| `build-video-processing-macos.sh` | Узкоспециализированный | `scripts/build.sh` |
| `build-video-processing-docker.sh` | Устаревший | `scripts/build.sh` |
| `build-android-native-libs.sh` / `.ps1` | Устаревший, не поддерживает vcpkg | `scripts/build.sh` |
| `build-android.sh` | Устаревший | `scripts/build.sh` |
| `build-ios-native-libs.sh` | Устаревший | `scripts/build.sh` |
| `build-ios.sh` | Устаревший | `scripts/build.sh` |
| `build-rtsp-native-libs.ps1` | Устаревший | `scripts/build.sh` |
| `build-synology-spk.sh` | Специфичный, редко используется | `scripts/build.sh` |
| `build-nas-package.sh` / `.ps1` | Устаревший | `scripts/build.sh` |
| `build-test-environment.sh` / `.ps1` | Заменён на `bootstrap-local-dev-env.ps1` | `scripts/build.sh` |
| `build-gradle-tasks.sh` | Дубликат (теперь в core/) | `scripts/core/build-gradle-tasks.sh` |
| `build-logger.sh` | Дубликат (теперь в core/) | `scripts/core/build-logger.sh` |
| `build-monitor.ps1` | Устаревший мониторинг | `scripts/analysis/analyze-build-logs.sh` |

### Категория 2: Устаревшие тесты и acceptance

| Файл | Причина | Заменён на |
|------|---------|------------|
| `analyze-camera-test-results.ps1` | Устаревший формат отчётов | `scripts/analysis/analyze-build-logs.sh` |
| `analyze-test-results.ps1` / `.sh` | Устаревший формат | `scripts/analysis/analyze-build-logs.sh` |
| `run-demo-tests.ps1` / `.sh` / `-simple.ps1` | Демо-тесты, не для production | `scripts/run-tests.ps1` |
| `run-ffmpeg-tests.ps1` / `.sh` | Заменён на phase1-automation | `scripts/ci/mvp-automated-acceptance.sh` |
| `long-run-test.ps1` | Заменён на RTSP long-run | `scripts/run-rtsp-long-run-stability-test.ps1` |
| `desktop-video-event-longrun-smoke.ps1` | Устаревший smoke | `scripts/phase3-*` |
| `process-c4-results.ps1` | Устаревший формат | `scripts/phase3-*` |
| `process-f2-results.ps1` | Устаревший формат | `scripts/phase3-*` |

### Категория 3: Устаревшая документация и управление

| Файл | Причина | Заменён на |
|------|---------|------------|
| `archive-documentation.sh` / `.ps1` | Устаревший | `scripts/manage-documentation.sh` |
| `check-documentation-links.py` | Дубликат | `scripts/check-docs-links.py` |
| `generate-project-structure.py` | Автоматизирован через hooks | `scripts/setup-git-hooks.sh` |
| `move-docs-files.ps1` | Временный скрипт | `scripts/manage-documentation.ps1` |
| `manage-documentation.sh` / `.ps1` | Устаревший | (планируется v2) |

### Категория 4: Устаревшие инструменты сборки

| Файл | Причина | Заменён на |
|------|---------|------------|
| `install-gradle.ps1` | Gradle wrapper автоматически | `./gradlew` |
| `quick-install.ps1` | Устаревший | `scripts/install-dependencies.ps1` |
| `bootstrap-local-test-configs.ps1` | Частично устаревший | `scripts/bootstrap-local-dev-env.ps1` |
| `configure-ffmpeg-manual.ps1` | Заменён на `install-ffmpeg.ps1` | `scripts/install-ffmpeg.ps1` |
| `setup-ffmpeg-dev.ps1` | Дубликат | `scripts/install-ffmpeg.ps1` |
| `download-ffmpeg-mingw.ps1` | Заменён на vcpkg | `scripts/build.sh` |

### Категория 5: Специфичные для NAS/Synology (редко используются)

| Файл | Причина | Статус |
|------|---------|--------|
| `build-synology-spk.sh` | Синялогия, редкие релизы | Архив (сохранить) |
| `manage-nas-git.sh` / `.ps1` | NAS-specific | Архив (сохранить) |
| `nas-field-*` | NAS field deployment | Архив (сохранить) |
| `prepare-synology-release.sh` | Synology-only | Архив (сохранить) |

### Категория 6: Дубликаты и временные скрипты

| Файл | Причина | Статус |
|------|---------|--------|
| `build-pipeline-analysis.md` | Документ, не скрипт | Архив |
| `CHANGES_SUMMARY.md` | Перемещён в docs/ | Архив (дубликат) |
| `LOGGING_README.md` | Перемещён в docs/ | Архив (дубликат) |
| `BUILD_PIPELINE_README.md` | Перемещён в docs/ | Архив (дубликат) |

---

## ✅ Активные скрипты (не архивируются)

### Build Pipeline (новые)
- `build.sh`
- `build-pipeline.sh`
- `build-pipeline.ps1`
- `core/*`
- `analysis/*`
- `validation/*`
- `docs/*`

### CI/CD
- `ci/*`

### Raspberry Pi
- `rpi-connect.sh` / `.ps1`
- `rpi-docker.sh`
- `rpi-check.sh`
- `rpi-deploy-test.sh`
- `rpi-config-check.sh`
- `rpi-generate-config-report.sh`

### ONVIF / Phase 1
- `run-phase1-onvif-acceptance.ps1`
- `run-phase1-onvif-acceptance-secure.ps1`
- `run-phase1-onvif-gate.ps1`
- `onvif-events-api-verification.ps1`
- `onvif-events-resilience-verification.ps1`
- `generate-phase1-onvif-evidence-report.ps1`
- `show-latest-phase1-onvif-decision.ps1`
- `validate-phase1-onvif-artifacts.ps1`
- `validate-onvif-test-camera-config.ps1`

### Phase 3 / Analytics
- `phase3-*`

### PostgreSQL Finalization
- `postgres-finalization-*`
- `postgresql-staging-evidence-*`

### Security
- `security-*`
- `check-security-*`

### Video Processing
- `profile-video-decoder.ps1` / `.sh`
- `monitor-rtsp-performance.ps1`
- `run-rtsp-test-with-checks.ps1`
- `run-rtsp-final-validation.ps1`
- `run-real-camera-tests.ps1`

### Tools
- `install-dependencies.ps1` / `.sh`
- `install-ffmpeg.ps1` / `.sh`
- `install-android-sdk.ps1`
- `install-vscode-extensions.ps1` / `.sh`
- `cleanup-old-branches.ps1`
- `create-platform-branches.ps1` / `.sh`
- `generate-compile-commands.ps1` / `.sh`
- `download-models.ps1` / `.sh`
- `download-yolo-models.ps1` / `.sh`
- `download-face-recognition-models.ps1`
- `download-crowd-density-models.ps1`
- `download-behavior-analysis-models.ps1` / `.sh`
- `check-git-lfs.sh`
- `setup-git-hooks.sh`
- `check-ide-setup.ps1`
- `check-certificate-expiry.sh`
- `monitor-ffmpeg-performance.ps1` / `.sh`

### Utilities
- `find-duplicates.py`
- `increment-version.ps1` / `.sh`
- `get-release-dir.ps1` / `.sh`
- `publish-local.sh`
- `restart-api-local.ps1`

---

## 📝 Примечания

1. **Архивация не означает удаление** — файлы сохранены для исторической справки
2. **Возврат возможен** — при необходимости файлы можно восстановить из архива
3. **Версионирование** — рекомендуется тегировать релизы перед архивацией
4. **Документация** — обновите README с актуальными путями скриптов

## 🔍 Поиск в архиве

```bash
# Поиск по содержимому
grep -r "pattern" scripts/archive/

# PowerShell
Select-String -Path "scripts/archive/*.ps1" -Pattern "pattern"
```

---

*Архив создан: 2025-01-15*  
*Версия: 1.0*
