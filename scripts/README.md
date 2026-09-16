# Скрипты проекта IP-CSS

## ⚠️ Проверка Окружения и Зависимостей (НОВОЕ)

### 🔍 Проверка окружения перед сборкой/тестами

**ОБЯЗАТЕЛЬНО** перед запуском сборок, тестов или компиляции проверяйте наличие всех необходимых компонентов:

```bash
# Полный режим (Linux/macOS)
python scripts/check-environment-requirements.py --verbose

# Быстрый режим (только критические компоненты)
python scripts/check-environment-requirements.py --quick

# С генерацией JSON отчёта
python scripts/check-environment-requirements.py --report diagnostics/environment-report.json

# Windows PowerShell
.\scripts\check-environment-requirements.ps1 -Verbose
.\scripts\check-environment-requirements.ps1 -Quick
.\scripts\check-environment-requirements.ps1 -Report diagnostics\environment-report.json
```

**Что проверяет:**
- ✅ Python 3.10+ и pip
- ✅ Java JDK 17+
- ✅ Gradle Wrapper
- ✅ Node.js 18+ и npm (опционально)
- ✅ Docker 20.10+ (опционально)
- ✅ Системные ресурсы (RAM >= 8GB, Disk >= 50GB)

---

### 🔍 Проверка зависимостей перед установкой

**ОБЯЗАТЕЛЬНО** перед установкой новых модулей/библиотек проверяйте отсутствие конфликтов:

```bash
# Проверка конкретного пакета
python scripts/check-dependency-conflicts.py --package <package-name>

# Показать все установленные пакеты
python scripts/check-dependency-conflicts.py --list

# С генерацией JSON отчёта
python scripts/check-dependency-conflicts.py --report diagnostics/dependency-report.json

# Windows PowerShell
.\scripts\check-dependency-conflicts.ps1 -Package <package-name>
.\scripts\check-dependency-conflicts.ps1 -List
.\scripts\check-dependency-conflicts.ps1 -Report diagnostics\dependency-report.json
```

**Что проверяет:**
- ✅ Наличие уже установленных версий (Python pip, npm global/local)
- ✅ Конфликты версий между локальными и глобальными пакетами
- ✅ Совместимость версий

---

## 📦 Build Pipeline v2.0

Автоматизированная система сборки с логированием и анализом.

### Быстрый старт

```bash
# Полная сборка
./build.sh

# Режимы сборки
./build.sh full       # Все компоненты (по умолчанию)
./build.sh quick      # Быстрая проверка (тесты)
./build.sh native     # Только C++ библиотеки
./build.sh kotlin     # Только Kotlin модули
./build.sh web        # Только веб-интерфейс
./build.sh clean      # Очистка кэшей
```

### Логирование

Все логи сохраняются в `build-logs/`:

```bash
# Статистика
./analysis/analyze-build-logs.sh stats 7

# Поиск ошибок
./analysis/analyze-build-logs.sh errors "OutOfMemory"

# Временная шкала
./analysis/analyze-build-logs.sh timeline

# Последняя сборка
./analysis/analyze-build-logs.sh latest
```

### Структура

```
scripts/
├── build.sh                          # Универсальный запуск
├── build-pipeline.sh                 # Linux/macOS
├── build-pipeline.ps1                # Windows
├── core/                             # Ядро
│   ├── build-logger.sh               # Логирование
│   └── build-gradle-tasks.sh         # Gradle задачи
├── analysis/                         # Анализ
│   └── analyze-build-logs.sh         # Анализатор
├── validation/                       # Валидация
│   ├── validate-artifacts.sh         # Проверка артефактов
│   └── check-git-lfs.sh              # Git LFS
├── docs/                             # Документация
│   ├── BUILD_PIPELINE_README.md
│   └── CHANGES_SUMMARY.md
└── archive/                          # Архив устаревших скриптов
    └── SCRIPT_ARCHIVE_README.md      # Документация архива
```

### Переменные окружения

```bash
export BUILD_NATIVE=true
export BUILD_KOTLIN=true
export BUILD_WEB=true
export BUILD_DOCKER=false
export BUILD_ID=my-build-123
```

### Требования

- **Java 17+**
- **CMake 3.15+**
- **Node.js 20+**
- **Git 2.30+**
- **Git LFS** (рекомендуется)

### Документация

- BUILD_PIPELINE_README.md *(утерян/в архиве)*
- CHANGES_SUMMARY.md *(утерян/в архиве)*
- [REORGANIZATION_SUMMARY.md](REORGANIZATION_SUMMARY.md)
- `.koda/skills/ip-css-build.md` — Правила сборки

---

## 📡 Raspberry Pi Scripts

### `rpi-connect.sh` / `rpi-connect.ps1`
Подключение к Raspberry Pi 4 через SSH.

**Использование:**
```bash
# Bash
bash scripts/rpi-connect.sh

# PowerShell (Windows)
. .\scripts\rpi-connect.ps1
Connect-RPi
```

**Требует:** `credentials/.env` с настройками подключения

---

### `rpi-docker.sh`
Управление Docker контейнерами на Raspberry Pi.

**Использование:**
```bash
# Список контейнеров
bash scripts/rpi-docker.sh ps

# Pull образа
bash scripts/rpi-docker.sh pull nginx:latest

# Запуск контейнера
bash scripts/rpi-docker.sh run -d -p 8080:80 nginx:latest

# Portainer URL
bash scripts/rpi-docker.sh portainer
```

---

### `rpi-check.sh`
Проверка подключения к Raspberry Pi и сервисам (Docker, Portainer).

**Использование:**
```bash
bash scripts/rpi-check.sh
```

---

### `rpi-deploy-test.sh`
Развертывание тестовых Docker образов на Raspberry Pi.

**Использование:**
```bash
bash scripts/rpi-deploy-test.sh
```

**Доступные образы:**
1. nginx:latest - Веб-сервер (port 8080)
2. redis:alpine - Redis кэш (port 6379)
3. postgres:alpine - PostgreSQL (port 5432)
4. mongo:alpine - MongoDB (port 27017)
5. alpine:latest - Минимальный Linux

---

## 🎯 ONVIF / Phase 1 Acceptance

### Основные скрипты

- **`run-phase1-onvif-gate.ps1`** — One-command wrapper для полной проверки Phase 1
- **`run-phase1-onvif-acceptance-secure.ps1`** — Безопасный запуск acceptance с паролем
- **`show-latest-phase1-onvif-decision.ps1`** — Показать последнее решение gate
- **`validate-phase1-onvif-artifacts.ps1`** — Валидация артефактов сборки

### Детальная проверка

- **`onvif-events-api-verification.ps1`** — Проверка ONVIF Events API
- **`onvif-events-resilience-verification.ps1`** — Проверка устойчивости
- **`validate-onvif-test-camera-config.ps1`** — Валидация конфигурации камер

### Документация

См. **`docs/automation/MVP_PHASE1_AUTOMATED_ACCEPTANCE.md`**

---

## 📹 Video Processing & RTSP

### Тестирование

- **`run-rtsp-test-with-checks.ps1`** — RTSP тесты с проверками
- **`run-rtsp-final-validation.ps1`** — Финальная валидация RTSP
- **`run-rtsp-long-run-stability-test.ps1`** — Долгосрочный тест стабильности
- **`run-real-camera-tests.ps1`** — Тесты с реальными камерами
- **`test-rtsp-*.ps1`** — Интеграционные тесты RTSP

### Мониторинг

- **`monitor-rtsp-performance.ps1`** — Мониторинг производительности RTSP
- **`monitor-ffmpeg-performance.ps1` / `.sh`** — Мониторинг FFmpeg
- **`profile-video-decoder.ps1` / `.sh`** — Профилирование видеодекодера

### Gate

- **`video-e2e-go-no-go.ps1`** — Video E2E gate решение
- **`w4-mvp-platform-and-gate.ps1`** — W4 MVP platform и gate

---

## 🔐 Security

- **`check-certificate-expiry.sh`** — Проверка срока действия сертификатов
- **`get-certificate-fingerprint.ps1` / `.sh`** — Получение отпечатка сертификата
- **`monitor-certificate-pinning.sh`** — Мониторинг certificate pinning

---

## 📝 Управление Документацией

- **`docs-lint.sh`** — Автоматизированная проверка документации ✅ **NEW!**

**Использование:**
```bash
# Проверить всю документацию
./scripts/docs-lint.sh docs

# Проверить конкретный файл
./scripts/docs-lint.sh docs/API_V2.md

# Проверить с verbose выводом
./scripts/docs-lint.sh docs --verbose
```

**Проверки:**
- ✅ Broken links
- ✅ Markdown style (mdl)
- ✅ Required sections
- ✅ YAML syntax
- ✅ Code block syntax
- ✅ API endpoint format
- ✅ Image alt text

**Требует:**
```bash
npm install -g markdown-link-check
gem install mdl
```

**Конфигурация:**
- [docs/.mdlrc](../docs/.mdlrc) — Правила markdown lint
- [docs/VERSIONING_GUIDE.md](../docs/VERSIONING_GUIDE.md) — Версионирование
- [docs/README_DOCUMENTATION_MANAGEMENT.md](../docs/README_DOCUMENTATION_MANAGEMENT.md) — Управление документацией

---

## 🌐 Network Layer

- **`run-network-layer-1-4-local-smoke.ps1`** — Локальный smoke тест Network Layer
- **`show-latest-network-layer-1-4-decision.ps1`** — Показать решение gate
- **`sync-network-layer-1-4-status.ps1`** — Синхронизация статуса

---

## 🛠 Утилиты

### Установка и настройка

- **`install-dependencies.ps1` / `.sh`** — Установка всех зависимостей
- **`install-ffmpeg.ps1` / `.sh`** — Установка FFmpeg
- **`install-android-sdk.ps1`** — Установка Android SDK
- **`install-vscode-extensions.ps1` / `.sh`** — Установка расширений VS Code
- **`bootstrap-local-dev-env.ps1`** — Настройка локального окружения

### Загрузка моделей

- **`download-yolo-models.ps1`** — YOLO модели
- **`download-face-recognition-models.ps1`** — Модели распознавания лиц
- **`download-crowd-density-models.ps1`** — Модели подсчёта толпы
- **`download-behavior-analysis-models.ps1` / `.sh`** — Модели анализа поведения
- **`download-models.ps1` / `.sh`** — Все модели

### Git и версии

- **`cleanup-old-branches.ps1`** — Очистка старых веток
- **`setup-git-hooks.sh`** — Установка git hooks
- **`increment-version.ps1` / `.sh`** — Инкремент версии
- **`create-release-build.ps1`** — Создание релизной сборки

### Мониторинг и проверка

- **`check-dependencies.ps1`** — Проверка зависимостей
- **`check-docs-links.ps1` / `.py`** — Проверка ссылок в документации
- **`check-ide-setup.ps1`** — Проверка настройки IDE
- **`monitor-*.ps1` / `.sh`** — Мониторинг различных компонентов

### Utilities

- **`get-release-dir.ps1` / `.sh`** — Получить директорию релиза
- **`publish-local.sh`** — Публикация локально
- **`restart-api-local.ps1`** — Перезапуск API локально
- **`update-documentation.ps1` / `.py`** — Обновление документации

---

## 🧪 Testing

### Запуск тестов

- **`run-tests.ps1`** — Универсальный запуск тестов
- **`test-integration-rtsp.ps1`** — Интеграционные тесты RTSP
- **`test-hls-integration.ps1`** — HLS интеграция
- **`test-video-decoder.ps1` / `.sh`** — Тесты видеодекодера
- **`test-nas-build.ps1` / `.sh`** — Тесты NAS сборки

### CI/CD

- **`ci/mvp-automated-acceptance.sh` / `.ps1`** — MVP automated acceptance
- **`ci/verify-kmp-phase1.sh` / `.ps1` / `.py`** — Verification KMP Phase 1

---

## 🗂 Архив

Устаревшие и дублирующие скрипты перемещены в **`scripts/archive/`**.

Для просмотра списка архивированных файлов см. **[REORGANIZATION_SUMMARY.md](REORGANIZATION_SUMMARY.md)**

---

## Требования

- **Python 3.7+**
- **Bash** — для `.sh` скриптов
- **PowerShell** — для `.ps1` скриптов (Windows)

---

## 📝 Примечания

1. **Build Pipeline v2.0** — используйте `scripts/build.sh` вместо старых скриптов сборки
2. **Архив** — устаревшие скрипты сохранены, но не поддерживаются
3. **Автоматизация** — все скрипты автоматически запускаются в CI/CD через GitHub Actions

