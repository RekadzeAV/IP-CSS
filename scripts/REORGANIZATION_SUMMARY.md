# Реорганизация скриптов IP-CSS

## 📅 Дата: 2025-01-15

## 📊 Итоги реорганизации

| Категория | Количество | Статус |
|-----------|------------|--------|
| **Архивировано** | 87 файлов | ✅ Перемещено в `scripts/archive/` |
| **Оставлено** | ~120 файлов | ✅ Активные скрипты |
| **Создано новых** | 12 файлов | ✅ Build Pipeline v2.0 |

---

## 📁 Новая структура

```
scripts/
├── build.sh                          # Универсальный запуск (v2.0)
├── build-pipeline.sh                 # Linux/macOS pipeline (v2.0)
├── build-pipeline.ps1                # Windows pipeline (v2.0)
│
├── core/                             # Ядро системы (NEW)
│   ├── build-logger.sh               # Библиотека логирования
│   └── build-gradle-tasks.sh         # Gradle задачи
│
├── analysis/                         # Анализ (NEW)
│   └── analyze-build-logs.sh         # Анализатор логов
│
├── validation/                       # Валидация (NEW)
│   └── validate-artifacts.sh         # Проверка артефактов
│
├── docs/                             # Документация (NEW)
│   ├── BUILD_PIPELINE_README.md
│   └── CHANGES_SUMMARY.md
│
├── ci/                               # CI/CD скрипты
│   ├── mvp-automated-acceptance.sh
│   └── ...
│
├── archive/                          # Архив (NEW)
│   └── SCRIPT_ARCHIVE_README.md      # Документация архива
│
├── build.sh                          # Универсальный запуск
├── build-pipeline.sh                 # Linux/macOS
├── build-pipeline.ps1                # Windows
│
├── bootstrap-local-dev-env.ps1       # Настройка окружения
├── check-dependencies.ps1            # Проверка зависимостей
├── check-docs-links.ps1/.py          # Проверка документации
├── check-git-lfs.sh                  # Проверка Git LFS
├── check-ide-setup.ps1               # Проверка IDE
│
├── cleanup-old-branches.ps1          # Очистка веток
├── create-release-build.ps1          # Создание релиза
│
├── download-*.sh/.ps1                # Загрузка моделей (YOLO, Face, Crowd, Behavior)
├── install-*.sh/.ps1                 # Установка зависимостей (FFmpeg, Android SDK, VSCode)
├── monitor-*.sh/.ps1                 # Мониторинг (FFmpeg, RTSP, сертификаты)
│
├── onvif-events-*.ps1                # ONVIF Events валидация
├── network-layer-*.ps1               # Network Layer smoke tests
├── run-phase1-onvif-*.ps1            # Phase 1 ONVIF acceptance
│
├── profile-video-decoder.ps1/.sh     # Профилирование декодера
├── rpi-*.sh/.ps1                     # Raspberry Pi скрипты
├── run-rtsp-*.ps1                    # RTSP тесты
├── run-tests.ps1                     # Запуск тестов
├── run-web-interface-acceptance-pipeline.ps1
│
├── setup-git-hooks.sh                # Установка git hooks
├── setup-*.ps1/.sh                   # Настройка окружения
├── show-latest-*.ps1                 # Показ статуса последних сборок
│
├── test-*.ps1/.sh                    # Интеграционные тесты
├── validate-*.ps1/.sh                # Валидация артефактов
├── video-e2e-go-no-go.ps1            # Video E2E gate
├── w4-mvp-platform-and-gate.ps1      # W4 MVP gate
│
└── README.md                         # Основная документация
```

---

## 🗑️ Архивированные скрипты (87 файлов)

### Дубликаты сборки (15 файлов)
- `build-all-platforms.sh`
- `build-all-native-libs.sh/.ps1`
- `build-native-lib.sh/.ps1`
- `build-video-processing-lib.sh/.ps1`
- `build-video-processing-linux.sh`
- `build-video-processing-macos.sh`
- `build-video-processing-docker.sh`
- `build-android-native-libs.sh/.ps1`
- `build-android.sh`
- `build-ios-native-libs.sh`
- `build-ios.sh`
- `build-rtsp-native-libs.ps1`
- `build-synology-spk.sh`
- `build-nas-package.sh/.ps1`
- `build-test-environment.sh/.ps1`
- `build-phase2-native.ps1`
- `build-monitor.ps1`
- `build-pipeline-analysis.md`

### Устаревшие тесты (12 файлов)
- `analyze-camera-test-results.ps1`
- `analyze-test-results.ps1/.sh`
- `run-demo-tests.ps1/.sh/-simple.ps1`
- `run-ffmpeg-tests.ps1/.sh`
- `long-run-test.ps1`
- `desktop-video-event-longrun-smoke.ps1`
- `process-c4-results.ps1`
- `process-f2-results.ps1`

### Устаревшая документация (6 файлов)
- `archive-documentation.sh/.ps1`
- `check-documentation-links.py`
- `generate-project-structure.py`
- `move-docs-files.ps1`
- `manage-documentation.sh/.ps1`

### Устаревшие инструменты (12 файлов)
- `install-gradle.ps1`
- `quick-install.ps1`
- `bootstrap-local-test-configs.ps1`
- `configure-ffmpeg-manual.ps1`
- `setup-ffmpeg-dev.ps1`
- `download-ffmpeg-mingw.ps1`
- `generate-cinterop-bindings.sh/.ps1`
- `generate-compile-commands.sh/.ps1`
- `generate-coverage.sh`
- `generate-f2-results-from-lighthouse.ps1`
- `generate-readiness-charts.py`
- `check-timeline.sh`

### NAS/Synology специфичные (11 файлов)
- `nas-field-*.ps1` (7 файлов)
- `manage-nas-git.sh/.ps1`
- `prepare-synology-release.sh`
- `build-synology-spk.sh`

### Phase 1 / 2 / 3 устаревшие (12 файлов)
- `automated-phase1-completion.ps1`
- `phase1-automation-controller.ps1`
- `phase1-critical-e2e-smoke.ps1`
- `generate-phase1-go-no-go-summary.ps1`
- `run-mvp-acceptance-with-infra.ps1`
- `run-network-layer-automation.ps1`
- `phase3-*.ps1` (5 файлов)
- `security-field-staging-validation.ps1`
- `security-mvp-readiness-check.ps1`

### Валидация и проверка (9 файлов)
- `validate-api-docs.py`
- `validate-audit-integrity-artifacts.py`
- `validate-nas-contracts.sh/.ps1`
- `validate-video-e2e-profile.py`
- `verify-kmp-phase1.sh/.ps1/.py`
- `check-video-runtime-matrix-config.py`
- `check-network-layer-ci-prerequisites.ps1`

### Прочие (10 файлов)
- `activate-native-decoder.ps1`
- `activate-rtsp-client.sh`
- `create-platform-branches.sh/.ps1`
- `create-package-icons.py`
- `find-duplicates.py`
- `server-export-release-artifacts.ps1`
- `server-metrics-guard.ps1`
- `server-nightly-gate.ps1`
- `server-pre-release-gate.ps1`
- `publish-web-interface-automation-status.ps1`
- `cleanup-web-interface-automation-artifacts.ps1`
- `android-permissions-revoke-recover-smoke.ps1`
- `android-device-evidence-pack.ps1`
- `server-auth-discovery-smoke.ps1`
- `mobile-desktop-1-7-auto-closure.ps1`
- `recording-ws-lifecycle-acceptance-evidence.ps1`
- `run-recording-ws-lifecycle-acceptance.ps1`
- `onvif-manual-verification.ps1`
- `onvif-user-rights-check.ps1`
- `ci-run-all.sh/.ps1`
- `postgres-finalization-*.ps1` (7 файлов)
- `postgresql-staging-evidence-*.ps1` (2 файла)
- `run-postgres-finalization-suite.ps1`

---

## ✅ Активные скрипты (~120 файлов)

### Build Pipeline v2.0
- `build.sh` - Универсальный запуск
- `build-pipeline.sh` - Linux/macOS
- `build-pipeline.ps1` - Windows
- `core/*` - Ядро системы
- `analysis/*` - Анализ логов
- `validation/*` - Валидация
- `docs/*` - Документация

### CI/CD
- `ci/mvp-automated-acceptance.sh`
- `ci/mvp-automated-acceptance.ps1`
- `ci/verify-kmp-phase1.sh`
- `ci/verify-kmp-phase1.ps1`
- `ci/verify-kmp-phase1.py`

### Raspberry Pi
- `rpi-connect.sh/.ps1`
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

### Video Processing
- `profile-video-decoder.ps1/.sh`
- `monitor-rtsp-performance.ps1`
- `run-rtsp-test-with-checks.ps1`
- `run-rtsp-final-validation.ps1`
- `run-rtsp-long-run-stability-test.ps1`
- `run-real-camera-tests.ps1`
- `test-rtsp-*.ps1` (5 файлов)
- `video-e2e-go-no-go.ps1`
- `video-runtime-*.ps1` (2 файла)
- `w4-mvp-platform-and-gate.ps1`

### Network Layer
- `network-layer-smoke-test.ps1`
- `run-network-layer-1-4-local-smoke.ps1`
- `show-latest-network-layer-1-4-decision.ps1`
- `sync-network-layer-1-4-status.ps1`

### PostgreSQL Finalization
- `start-postgres-finalization-staging.ps1`
- `validate-postgres-finalization-report.ps1`

### Security
- `check-certificate-expiry.sh`
- `get-certificate-fingerprint.ps1/.sh`
- `monitor-certificate-pinning.sh`

### Testing
- `run-tests.ps1`
- `test-integration-rtsp.ps1`
- `test-hls-integration.ps1`
- `test-connection.ps1`
- `test-nas-build.sh/.ps1`
- `test-video-decoder.ps1/.sh`

### Utilities
- `bootstrap-local-dev-env.ps1`
- `check-dependencies.ps1`
- `check-docs-links.ps1/.py`
- `check-ide-setup.ps1`
- `cleanup-old-branches.ps1`
- `create-release-build.ps1`
- `download-*.sh/.ps1` (8 файлов)
- `get-release-dir.sh/.ps1`
- `increment-version.sh/.ps1`
- `install-*.sh/.ps1` (6 файлов)
- `monitor-*.sh/.ps1` (3 файла)
- `publish-local.sh`
- `restart-api-local.ps1`
- `setup-git-hooks.sh`
- `setup-*.sh/.ps1` (6 файлов)
- `show-latest-*.ps1` (4 файла)
- `update-documentation.ps1/.py`
- `validate-artifacts.sh`
- `wait-for-build-and-continue.ps1`

### Documentation
- `README.md`
- `README_DOCUMENTATION_MANAGEMENT.md`
- `README_VERSION.md`

### Python Scripts
- `rtsp-audio-test-server.py`

---

## 📝 Рекомендации

### 1. Обновите .gitignore

Добавьте:
```
# Архив скриптов
scripts/archive/
```

### 2. Обновите CI/CD

Проверьте все workflow и обновите пути к скриптам:
- Удалите ссылки на архивированные скрипты
- Обновите ссылки на новые пути (core/, analysis/, validation/)

### 3. Обновите документацию

- Обновите README с новыми путями
- Удалите упоминания архивированных скриптов
- Добавьте ссылки на Build Pipeline v2.0

### 4. Версионирование

Перед коммитом:
```bash
git tag scripts-reorg-2025-01-15
```

### 5. Уведомление команды

Сообщите команде:
- Какие скрипты перемещены
- Где находятся новые версии
- Как пользоваться архивом

---

## 🔍 Как пользоваться архивом

### Поиск файла
```bash
# Bash
ls scripts/archive/ | grep pattern

# PowerShell
Get-ChildItem scripts/archive/ | Select-String pattern
```

### Восстановление файла
```bash
# Bash
cp scripts/archive/old-script.sh scripts/

# PowerShell
Copy-Item scripts/archive/old-script.ps1 scripts/
```

### Просмотр содержимого
```bash
# Bash
cat scripts/archive/old-script.sh

# PowerShell
Get-Content scripts/archive/old-script.ps1
```

---

## ✅ Проверка

Перед коммитом выполните:

```bash
# Проверка структуры
ls -la scripts/

# Проверка build pipeline
./scripts/build.sh --help

# Проверка анализа логов
./scripts/analysis/analyze-build-logs.sh --help

# Проверка валидации
./scripts/validation/validate-artifacts.sh --help
```

---

*Реорганизация выполнена: 2025-01-15*  
*Версия: 1.0*  
*Проект: IP-CSS*
