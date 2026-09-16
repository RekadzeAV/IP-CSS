# 📋 Локальные требования для разработки IP-CSS

> **Версия документа:** 1.0
> **Дата:** 28 августа 2026 г.
> **Версия проекта:** 0.5.1.1-beta
> **Статус:** ✅ Актуально

Этот документ содержит исчерпывающий список всего, что необходимо для локальной разработки, сборки и тестирования проекта **IP-CSS (IP Camera Surveillance System)** на Windows, Linux и macOS.

---

## 📊 Быстрая сводка по платформам

| Компонент | Windows | Linux | macOS |
|-----------|---------|-------|-------|
| **JDK 17+** | ✅ OpenJDK 17.0.17 (установлен) | **Требуется** | **Требуется** |
| **Gradle 8.9** | ✅ Через wrapper | ✅ Через wrapper | ✅ Через wrapper |
| **Android SDK 34** | ✅ API 34, NDK 30 | ❌ Не установлен | ❌ Не установлен |
| **Docker** | ✅ 29.7.2 | ✅ | ✅ |
| **Node.js 20+** | ✅ 24.16.0 | **Требуется** | **Требуется** |
| **Python 3.9+** | ✅ 3.11.9 | ✅ | ✅ |
| **CMake 3.15+** | ✅ 4.2.1 | ✅ | ✅ |
| **FFmpeg** | ✅ 8.1.1 | **Требуется** | **Требуется** |
| **C++ компилятор** | ✅ G++ 15.2.0 (MinGW) | **Требуется** | ✅ Clang/Xcode |
| **PostgreSQL 16+** | ❌ | **Требуется** | **Требуется** |
| **Redis 7+** | ❌ | **Требуется** | **Требуется** |
| **VS Code** | ✅ | **Опц.** | **Опц.** |

---

## 🪟 Блок 1: Windows

| **VS Code** | ✅ | **Опц.** | **Опц.** |

### ✅ Уже установлено (локальная машина)

| Компонент | Версия | Путь |
|-----------|--------|------|
| **JDK** | OpenJDK 17.0.17 (Microsoft) | `C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot` |
| **JAVA_HOME** | `C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot` | Установлен |
| **Gradle Wrapper** | 8.9 | `e:\GitHub-Ai\IP-CSS\gradlew.bat` |
| **Docker** | 29.7.2 | |
| **Docker Compose** | v5.4.0 | |
| **Node.js** | 24.16.0 | |
| **npm** | 11.13.0 | |
| **Python** | 3.11.9 | |
| **pip** | 26.0 | |
| **Git** | 2.55.0 | |
| **CMake** | 4.2.1 | |
| **FFmpeg** | 8.1.1 | |
| **G++ (MinGW-W64)** | 15.2.0 | |
| **Android SDK** | API 34 | `C:\Users\Rekad\AppData\Local\Android\Sdk` (platform-tools, build-tools 33/34/37, NDK 30) |
| **VS Code** | — | `code` CLI пока не в PATH |
| **RAM** | 32 GB | |
| **Диск C:** | 930 GB total, 171 GB free | |
| **local-maven-repo** | 3201 файл | `e:\GitHub-Ai\IP-CSS\local-maven-repo\` (офлайн сборки) |

### ⚠️ Требуется настроить / установить

| Компонент | Действие |
|-----------|----------|
| **PostgreSQL 16+** | Установить для интеграционных тестов, либо использовать `docker-compose -f docker-compose.integration.yml up -d` |
| **Redis 7+** | Установить для тестов, либо использовать Docker |
| **ANDROID_HOME / ANDROID_SDK_ROOT** | Добавить в системные переменные PATH (сейчас только в `local.properties`) |
| **VS Code CLI** | Добавить `code` в PATH (в VS Code: `Shell Command: Install 'code' command in PATH`) |
| **MSVC / Visual Studio Build Tools** | Опционально для Windows native сборки (G++ через MinGW доступен) |

### 🔧 Переменные окружения (Windows)

| Переменная | Значение | Тип |
|------------|----------|-----|
| `JAVA_HOME` | `C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot` | ✅ Установлен |
| `ANDROID_HOME` | `C:\Users\Rekad\AppData\Local\Android\Sdk` | ⚠️ Только в `local.properties` |
| `ANDROID_SDK_ROOT` | `C:\Users\Rekad\AppData\Local\Android\Sdk` | ⚠️ Только в `local.properties` |
| `ANDROID_USER_HOME` | `%USERPROFILE%\.android` | Опционально |
| `GRADLE_OPTS` | `-Dorg.gradle.daemon=false -Dorg.gradle.parallel=true` | CI/CD |

### 📦 .env файлы и credentials (Windows)

| Файл | Статус | Описание |
|------|--------|----------|
| `.env` | ✅ | Основные env для Docker Compose (dev) |
| `.env.local` | ✅ | Локальное Docker окружение |
| `.env.example` | ✅ | Шаблон env переменных |
| `.env.docker-manual` | ✅ | Docker manual режим |
| `.env.nas` | ✅ | NAS конфигурация |
| `credentials-all.env` | ✅ | Все пароли (в `.gitignore`) |
| `secrets-local/credentials.env` | ⚠️ **Нужно создать** | Создать из `credentials.example.env`, заполнить реальными паролями |

### 🐳 Docker Compose стеки (Windows)

| Файл | Назначение | Сервисы | Запуск |
|------|-----------|---------|--------|
| `docker-compose.yml` | Основной (production) | mediamtx, surveillance, postgres:17, redis:7 | `docker-compose up -d` |
| `docker-compose.local.yml` | Локальная разработка | mediamtx, surveillance, postgres:15, redis:7 | `docker-compose -f docker-compose.local.yml up -d` |
| `docker-compose.dev.yml` | Dev окружение | postgres:15, redis:7, server | `docker-compose -f docker-compose.dev.yml up -d` |
| `docker-compose.integration.yml` | Интеграционные тесты | postgres:15 (55432), redis:7 (56379) | `docker-compose -f docker-compose.integration.yml up -d` |
| `docker-compose.ai.yml` | AI инфраструктура | chromadb:8000, ai-redis:6380 | `docker-compose -f docker-compose.ai.yml up -d` |
| `docker-compose.manual.yml` | Ручной запуск | переопределения | `docker-compose -f docker-compose.manual.yml up -d` |

#### 💡 Порты (Windows)

| Сервис | Порт | Назначение |
|--------|------|-----------|
| surveillance | 8080 | REST API + Web UI + HLS |
| mediamtx | 8554 | RTSP |
| mediamtx | 8889 | WebRTC |
| mediamtx | 8890 | HLS |
| mediamtx | 8891 | HLS (HTTP) |
| postgres (основной) | 5432 | PostgreSQL |
| postgres (интеграционные) | 55432 | PostgreSQL |
| postgres (manual) | 55432 | PostgreSQL |
| redis (основной) | 6379 | Redis |
| redis (интеграционные) | 56379 | Redis |
| redis (manual) | 56179 | Redis |
| chroma (AI) | 8000 | ChromaDB |
| ai-redis | 6380 | Redis для AI очередей |

### 🛠️ Команды для работы с проектом (Windows PowerShell)

```powershell
# ============================================
# 1. Настройка локального окружения
# ============================================

# Автоматическая настройка
.\scripts\bootstrap-local-dev-env.ps1

# Установка всех зависимостей (требует admin)
.\scripts\install-build-dependencies.ps1

# Проверка окружения
.\scripts\check-environment-requirements.ps1 -Verbose
.\scripts\ci\verify-kmp-phase1.ps1 -SkipGradle

# ============================================
# 2. Сборка
# ============================================

# Быстрая сборка Kotlin модулей
.\build-win.bat

# Сервер (Ktor API)
.\gradlew.bat :server:api:shadowJar --no-daemon

# Юнит-тесты
.\gradlew.bat test --no-daemon

# Интеграционные тесты
.\gradlew.bat integrationTest --no-daemon

# Detekt + Ktlint
.\gradlew.bat detekt ktlintCheck --no-daemon

# Полная сборка shared (KMP)
.\gradlew.bat :shared:build --no-daemon -x :shared:verifyCommonMainCameraDatabaseMigration

# Android
.\gradlew.bat :android:app:assembleDebug --no-daemon

# Kover coverage report
.\gradlew.bat koverReport --no-daemon

# ============================================
# 3. Docker
# ============================================

# Локальная разработка
docker-compose -f docker-compose.local.yml up -d

# Интеграционные тесты (PostgreSQL + Redis)
docker-compose -f docker-compose.integration.yml up -d

# AI инфраструктура
docker-compose -f docker-compose.ai.yml up -d

# Production
docker-compose up -d
```

### 🐍 Python / AI-агент (Windows)

```powershell
# Создать и активировать виртуальное окружение
python -m venv ai-agent\.venv
ai-agent\.venv\Scripts\Activate.ps1

# Установить зависимости
pip install -r ai-agent\requirements.txt

# Запустить AI-агент
python ai-agent\src\main.py
```

---

## 🐧 Блок 2: Linux (Ubuntu/Debian)

### ✅ Минимальные системные требования

| Параметр | Минимум | Рекомендуется |
|----------|---------|---------------|
| **ОС** | Ubuntu 20.04 LTS / Debian 11 | Ubuntu 24.04 LTS |
| **RAM** | 8 GB | 16+ GB |
| **Дисковое пространство** | 50 GB | 100+ GB |
| **CPU** | 4 cores | 8+ cores |

### 📦 Установка (Linux / Ubuntu/Debian)

```bash
#!/bin/bash

# 1. Обновление системы
sudo apt-get update && sudo apt-get upgrade -y

# 2. JDK 17 (Temurin рекомендуется)
sudo apt-get install -y openjdk-17-jdk
# Или через Adoptium:
# wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo apt-key add -
# echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
# sudo apt-get update && sudo apt-get install -y temurin-17-jdk

# 3. Node.js 20+ (LTS)
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs

# 4. CMake 3.15+
sudo apt-get install -y cmake

# 5. FFmpeg + dev библиотеки
sudo apt-get install -y ffmpeg libavformat-dev libavcodec-dev libavutil-dev \
    libswscale-dev libswresample-dev

# 6. C++ компилятор
sudo apt-get install -y build-essential g++

# 7. pkg-config (для поиска библиотек)
sudo apt-get install -y pkg-config

# 8. Git
sudo apt-get install -y git

# 9. Docker CE + Docker Compose
sudo apt-get install -y ca-certificates curl gnupg lsb-release
curl -fsSL https://download.docker.com/linux/ubuntu/gpg | \
    sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg
echo "deb [arch=$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] \
    https://download.docker.com/linux/ubuntu $(lsb_release -cs) stable" | \
    sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
sudo apt-get update
sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-compose-plugin

# Добавить текущего пользователя в группу docker (перезагрузка требуется)
sudo usermod -aG docker $USER

# 10. Python 3.10+
sudo apt-get install -y python3 python3-pip python3-venv

# 11. PostgreSQL 16 (для тестов)
sudo apt-get install -y postgresql-16 postgresql-contrib
# Или через Docker (рекомендуется для тестов):
# docker run -d --name ipcss-postgres -p 5432:5432 \
#   -e POSTGRES_DB=ipcss_test -e POSTGRES_USER=ipcss -e POSTGRES_PASSWORD=ipcss_test \
#   postgres:16-alpine

# 12. Redis 7 (для тестов)
sudo apt-get install -y redis-server
# Или через Docker:
# docker run -d --name ipcss-redis -p 6379:6379 redis:7-alpine

# 13. OpenCV (опционально, для нативных библиотек)
sudo apt-get install -y libopencv-dev

# 14. Android SDK (только для Android сборки)
# Установить через Android Studio или вручную:
# wget https://dl.google.com/android/repository/commandlinetools-linux-*.zip
# Распаковать в ~/Android/Sdk

# 15. vcpkg (для C++ зависимостей)
# git clone https://github.com/Microsoft/vcpkg.git
# cd vcpkg && ./bootstrap-vcpkg.sh && sudo ./vcpkg integrate install

# 16. FFmpeg dev (альтернативно через vcpkg)
# vcpkg install ffmpeg libavformat libavcodec libavutil libswscale libswresample
```

### 🔧 Переменные окружения (Linux)

```bash
# Добавьте в ~/.bashrc или ~/.zshrc

# JDK
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64

# Android SDK (если установлен)
export ANDROID_HOME=$HOME/Android/Sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/platform-tools

# FFmpeg (если установлен вручную)
export FFMPEG_DIR=/usr/local

# OpenCV (если установлен вручную)
export OpenCV_DIR=/usr/local/share/OpenCV

# PostgreSQL (для локальной установки)
export DATABASE_URL=jdbc:postgresql://localhost:5432/ipcss_test
export DATABASE_USER=ipcss
export DATABASE_PASSWORD=ipcss_test

# Redis
export REDIS_HOST=localhost
export REDIS_PORT=6379

# Применить изменения
source ~/.bashrc
```

### 🐳 Docker Compose (Linux)

```bash
# Проверка Docker без sudo (после добавления в группу docker)
docker --version
docker compose version

# Локальная разработка
cd /path/to/IP-CSS
docker compose -f docker-compose.local.yml up -d

# Интеграционные тесты
docker compose -f docker-compose.integration.yml up -d

# AI инфраструктура
docker compose -f docker-compose.ai.yml up -d

# Production
docker compose up -d
```

### 🛠️ Команды для работы с проектом (Linux)

```bash
# ============================================
# 1. Настройка локального окружения
# ============================================

# Проверка окружения
python3 scripts/check-environment-requirements.py --verbose
scripts/ci/verify-kmp-phase1.sh

# ============================================
# 2. Сборка
# ============================================

# Сервер (Ktor API)
./gradlew :server:api:shadowJar --no-daemon

# Юнит-тесты
./gradlew test --no-daemon

# Интеграционные тесты
./gradlew integrationTest --no-daemon

# Detekt + Ktlint
./gradlew detekt ktlintCheck --no-daemon

# Полная сборка shared (KMP)
./gradlew :shared:build --no-daemon -x :shared:verifyCommonMainCameraDatabaseMigration

# Kover coverage report
./gradlew koverReport --no-daemon

# ============================================
# 3. Нативные библиотеки (C++)
# ============================================

cd native
mkdir -p build
cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release -j$(nproc)

# ============================================
# 4. Docker
# ============================================

docker compose -f docker-compose.local.yml up -d

# ============================================
# 5. AI-агент (Python)
# ============================================

python3 -m venv ai-agent/.venv
source ai-agent/.venv/bin/activate
pip install -r ai-agent/requirements.txt
python ai-agent/src/main.py
```

### 📋 Специфичные замечания для Linux

| Замечание | Описание |
|-----------|----------|
| **NVIDIA GPU** | Установите `nvidia-docker2` и используйте `--gpus all` для AI-агента и аналитики |
| **systemd сервис** | Создайте unit-файл для автозапуска surveillance сервера |
| **NAS интеграция** | Настройте NFS монтирование: `sudo mount -t nfs nas-server:/share /mnt/recordings` |
| **Права на Docker** | Добавьте текущего пользователя в группу `docker` для запуска без `sudo` |
| **SELinux/AppArmor** | Если используется, настройте политики для Docker контейнеров |

---

## 🍎 Блок 3: macOS

### ✅ Минимальные системные требования

| Параметр | Минимум | Рекомендуется |
|----------|---------|---------------|
| **ОС** | macOS 12.0 (Monterey) | macOS 14+ (Sonoma) |
| **Архитектура** | Intel или Apple Silicon | Apple Silicon (M1/M2/M3) |
| **RAM** | 8 GB | 16+ GB |
| **Дисковое пространство** | 50 GB | 100+ GB |

### 📦 Установка (macOS через Homebrew)

```bash
#!/bin/bash

# 1. Установить Homebrew (если не установлен)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 2. JDK 17 (Temurin)
brew install --cask temurin17

# 3. Node.js 20+ (LTS)
brew install node@20

# 4. CMake
brew install cmake

# 5. FFmpeg + библиотеки
brew install ffmpeg
# Dev библиотеки входят в FFmpeg formula автоматически

# 6. C++ компилятор (через Xcode)
xcode-select --install

# 7. pkg-config
brew install pkg-config

# 8. Git
brew install git

# 9. Docker Desktop (или Colima на Apple Silicon)
brew install --cask docker
# Или для Apple Silicon без Docker Desktop:
# brew install colima
# colima start

# 10. Python 3.10+
brew install python@3.11

# 11. PostgreSQL 16 (для тестов)
brew install postgresql@16
brew services start postgresql@16
# Или через Docker

# 12. Redis 7 (для тестов)
brew install redis
brew services start redis
# Или через Docker

# 13. Android SDK (только для Android сборки)
brew install android-commandlinetools
# Или установить Android Studio через brew

# 14. OpenCV (опционально)
brew install opencv
```

### 🔧 Переменные окружения (macOS)

```bash
# Добавьте в ~/.zshrc (или ~/.bash_profile для Intel)

# JDK (для Temurin)
export JAVA_HOME=/Library/Java/JavaVirtualMachines/temurin-17-jdk/Contents/Home

# Android SDK (если установлен)
export ANDROID_HOME=$HOME/Library/Android/sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/platform-tools

# FFmpeg (для ручной установки)
export FFMPEG_DIR=/opt/homebrew/opt/ffmpeg  # Apple Silicon
# export FFMPEG_DIR=/usr/local/opt/ffmpeg     # Intel

# OpenCV (для ручной установки через Homebrew)
export OpenCV_DIR=/opt/homebrew/opt/opencv   # Apple Silicon
# export OpenCV_DIR=/usr/local/opt/opencv      # Intel

# PostgreSQL (для локальной установки)
export DATABASE_URL=jdbc:postgresql://localhost:5432/ipcss_test
export DATABASE_USER=ipcss
export DATABASE_PASSWORD=ipcss_test

# Redis
export REDIS_HOST=localhost
export REDIS_PORT=6379

# Применить изменения
source ~/.zshrc
```

### 🐳 Docker (macOS)

```bash
# Проверка Docker
docker --version
docker compose version

# Для Apple Silicon:
# Если используете Docker Desktop, в настройках включите Rosetta для эмуляции x86-64 образов

# Локальная разработка
docker compose -f docker-compose.local.yml up -d

# Интеграционные тесты
docker compose -f docker-compose.integration.yml up -d

# AI инфраструктура
docker compose -f docker-compose.ai.yml up -d
```

### 🛠️ Команды для работы с проектом (macOS)

```bash
# ============================================
# 1. Сборка
# ============================================

# Сервер (Ktor API)
./gradlew :server:api:shadowJar --no-daemon

# Юнит-тесты
./gradlew test --no-daemon

# Интеграционные тесты
./gradlew integrationTest --no-daemon

# Detekt + Ktlint
./gradlew detekt ktlintCheck --no-daemon

# Полная сборка shared (KMP)
./gradlew :shared:build --no-daemon -x :shared:verifyCommonMainCameraDatabaseMigration

# Kover coverage report
./gradlew koverReport --no-daemon

# ============================================
# 2. Нативные библиотеки (C++)
# ============================================

cd native
mkdir -p build
cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release -j$(sysctl -n hw.ncpu)

# ============================================
# 3. Docker
# ============================================

docker compose -f docker-compose.local.yml up -d

# ============================================
# 4. AI-агент (Python)
# ============================================

python3 -m venv ai-agent/.venv
source ai-agent/.venv/bin/activate
pip install -r ai-agent/requirements.txt
python ai-agent/src/main.py
```

### 📋 Специфичные замечания для macOS

| Замечание | Описание |
|-----------|----------|
| **Apple Silicon (M1/M2/M3)** | Используйте Rosetta 2 для сборки x86-64 нативных библиотек: `arch -x86_64 ./gradlew ...` |
| **Docker Desktop** | На Apple Silicon образы должны быть ARM-версии; включите Rosetta в настройках Docker для x86-64 образов |
| **Xcode** | Для iOS сборки требуется Xcode 15+ (только на macOS) |
| **Homebrew пути** | На Apple Silicon используйте `/opt/homebrew/` вместо `/usr/local/` |
| **FFmpeg** | Dev библиотеки входят в стандартную установку через Homebrew |
| **Keychain** | macOS Keychain используется для хранения сертификатов и паролей |

---

## 📋 Конфигурационные файлы и переменные окружения

### .env файлы

| Файл | Назначение | Статус |
|------|-----------|--------|
| `.env` | Основные env для Docker Compose (dev) | ✅ Существует |
| `.env.local` | Локальное Docker окружение | ✅ Существует |
| `.env.example` | Шаблон env переменных | ✅ Существует |
| `.env.docker-manual` | Docker manual режим | ✅ Существует |
| `.env.nas` | NAS конфигурация | ✅ Существует |
| `credentials-all.env` | Все пароли (в `.gitignore`) | ✅ Существует |
| `secrets-local/credentials.env` | **Требуется создать** | ⚠️ Создать из `credentials.example.env` |

### Переменные окружения (Docker Compose)

```bash
# Обязательные для docker-compose.yml:
DB_PASSWORD=<required>           # Пароль PostgreSQL
REDIS_PASSWORD=<required>        # Пароль Redis
JWT_SECRET=<required>            # JWT секрет (мин. 32 символа)
DATA_ENCRYPTION_KEY=<required>   # Ключ шифрования (32 байта hex)
ADMIN_PASSWORD=<required>        # Пароль администратора

# Опциональные:
ENVIRONMENT=development
CORS_ALLOWED_ORIGINS=http://localhost:3000,http://localhost:8080
FORCE_HTTPS=true
ENABLE_FLYWAY=true
LDAP_ENABLED=true
AUTH_LDAP_SERVER=ldap://192.168.10.37:389
```

---

## 📦 Системные требования

| Параметр | Минимум | Рекомендуется |
|----------|---------|---------------|
| **ОС** | Windows 10, Ubuntu 20.04, macOS 12 | Windows 11, Ubuntu 24.04, macOS 14 |
| **JDK** | 17 | 17 (Temurin) |
| **Kotlin** | 2.0.0 | 2.0.0 (через Gradle) |
| **Gradle** | 8.9 | 8.9 (через wrapper) |
| **Android SDK** | API 34 | API 34 + NDK 30 |
| **RAM** | 8 GB | 16+ GB (32 GB для full build) |
| **Дисковое пространство** | 50 GB | 100+ GB |
| **Docker** | 24.0+ | Последняя версия |
| **PostgreSQL** | 16 | 17 (для production) |
| **Redis** | 7 | 7-alpine |
| **CMake** | 3.15 | 3.20+ |
| **FFmpeg** | 6.0 | Последняя |
| **Node.js** | 20 | 20+ (LTS) |
| **Python** | 3.9 | 3.11+ |

---

## 🧪 Тестирование

### CI/CD Pipeline (`ci.yml`)

| Job | Описание | Инструменты |
|-----|----------|-------------|
| `validate` | Detekt + ktlint + spotless | `./gradlew detekt ktlintCheck spotlessCheck` |
| `test` | Юнит + интеграционные тесты | PostgreSQL 16, Redis 7 |
| `server` | Сборка сервера | `./gradlew :server:api:build` |
| `android` | Сборка Android APK | `assembleDebug` |
| `docker` | Docker образ + Trivy скан | docker buildx, Trivy |
| `kmp` | KMP shared сборка | `./gradlew :shared:build` |
| `summary` | Сводка статусов | GitHub Step Summary |

### Метрики тестов

| Модуль | Тесты | Статус |
|--------|-------|--------|
| `:server:api:test` | 328 tests, 0 failures | ✅ |
| `:shared:desktopTest` | 245 tests, 1 skipped | ✅ |
| `:core:network:desktopTest` | 544 tests, 25 failed, 38 skipped | ⚠️ |
| `:core:common:desktopTest` | 23 tests, 5 skipped | ✅ |
| `:core:test-jvm:test` | 19 tests, 0 failures | ✅ |
| `:core:ui-bridge:desktopTest` | 46 tests, 0 failures | ✅ |
| `:server:api` покрытие Kover | 26.6% LINE (порог 20%) | ✅ |

---

## 🛠️ Ключевые команды для работы с проектом (универсальные)

```bash
# ============================================
# Минимальная сборка (только Kotlin/JVM, без Android/native)
# ============================================
./gradlew :core:common:build :core:network:build :shared:build :server:api:build --no-daemon

# ============================================
# Полные проверки
# ============================================
./gradlew detekt ktlintCheck --no-daemon
./gradlew test --no-daemon
./gradlew integrationTest --no-daemon
./gradlew koverReport --no-daemon

# ============================================
# Сборка и запуск сервера
# ============================================
./gradlew :server:api:shadowJar --no-daemon
java -jar server/api/build/libs/server-api-*.jar

# ============================================
# Docker (production)
# ============================================
docker compose up -d
# Health check: http://localhost:8080/api/v1/health
```

---

## 📁 Конфигурационные файлы проекта

| Файл | Описание |
|------|----------|
| `config/cameras.json` | Конфигурация камер (Hikvision, Dahua, Axis) |
| `config/nas_integration.json` | NAS + LDAP + хранилище конфигурация |
| `config/mediamtx/mediamtx.yml` | MediaMTX конфигурация (RTSP/WebRTC/HLS) |
| `config/nginx/nginx-load-balancer.conf` | NGINX LB (SSL, WebSocket, HLS, rate limiting) |
| `config/nginx/nginx.conf.example` | Шаблон nginx.conf |
| `config/certificate-pins.example.json` | Certificate pinning |
| `config/postgresql.env` | PostgreSQL конфигурация |
| `config/test-cameras.local.json` | Тестовые камеры |
| `gradle/libs.versions.toml` | Version Catalog (Kotlin 2.0, Ktor 2.3.12, ...) |
| `gradle.properties` | Настройки Gradle (память, D8, KMP) |
| `settings.gradle.kts` | Модули Gradle (9 модулей + 4 платформенных) |
| `Dockerfile` | Multi-stage (gradle:8.9-jdk17 → jre17-alpine) |

---

## 📚 Полезные документы и скрипты

### Документация

| Документ | Описание |
|----------|----------|
| `README.md` | Главная страница проекта |
| `PROJECT_STRUCTURE.md` | Структура проекта |
| `CHANGELOG.md` | История изменений |
| `docs/USER_MANUAL.md` | Руководство пользователя |
| `docs/DOCUMENTATION_INDEX.md` | Индекс документации |
| `scripts/README.md` | Справочник по скриптам |
| `docs/installation/INSTALL_INSTRUCTIONS.md` | Инструкции по установке |
| `docs/installation/DEPENDENCIES_INSTALLATION_GUIDE.md` | Руководство по зависимостям |

### Ключевые скрипты

| Скрипт | Назначение |
|--------|-----------|
| `scripts/bootstrap-local-dev-env.ps1` | Автоматическая настройка локального окружения |
| `scripts/install-build-dependencies.ps1` | Установка build-зависимостей |
| `scripts/install-android-sdk.ps1` | Установка Android SDK |
| `scripts/install-ffmpeg.ps1` | Установка FFmpeg |
| `scripts/install-vscode-extensions.ps1` | Установка VS Code расширений |
| `scripts/check-environment-requirements.ps1` | Проверка окружения |
| `scripts/check-environment-requirements.py` | Проверка окружения (Python) |
| `scripts/ci/verify-kmp-phase1.ps1` | KMP preflight проверка |
| `scripts/ci/verify-kmp-phase1.py` | KMP preflight проверка (Python) |
| `build-win.bat` | Быстрая сборка (Windows) |

---

## ⚠️ Известные проблемы и ограничения

| Проблема | Описание | Статус |
|----------|----------|--------|
| `server:api` | 33 compilation errors (model mismatch) | ⚠️ Известно в `build-win.bat` |
| `platforms:client-desktop` | Compose Desktop ошибки | ⚠️ Известно |
| `:shared:lint` | MissingPermission error | ⚠️ Известно |
| `:shared:verifyMigration` | SQLite JDBC lock на Windows | ⚠️ Известно |
| `:core:network:desktopTest` | 25 failed / 38 skipped | ⚠️ Сборка восстановлена |

---

> **Дата анализа:** 28 августа 2026 г.
> **Версия проекта:** 0.5.1.1-beta
> **Статус документа:** ✅ Актуально

Этот документ компилирует всю необходимую информацию для работы с проектом IP-CSS в одном месте. Информация основана на реальном анализе локального окружения и файлов проекта.