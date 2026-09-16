# Инструкции по установке компонентов для сборки IP-CSS

**Версия документации:** 2.0
**Последнее обновление:** 28 January 2026

> **📚 Связанные документы:**
> - [ENGINEER_GUIDE.md](../../archive/docs/guides/ENGINEER_GUIDE.md) - Полное руководство для инженеров по развертыванию
> - [DEVELOPMENT.md](../DEVELOPMENT.md) - Руководство по разработке
> - [DEPLOYMENT_GUIDE.md](../../archive/docs/deployment/DEPLOYMENT_GUIDE.md) - Руководство по развертыванию

---

## Обзор

Данное руководство содержит инструкции по установке всех необходимых компонентов для сборки и разработки проекта IP-CSS.

### Что устанавливается

- **Java JDK 17+** - для сборки Kotlin модулей
- **Node.js 20+** - для веб-интерфейса (Next.js)
- **CMake 3.15+** - для сборки нативных C++ библиотек
- **FFmpeg** - для обработки видео и аудио (опционально)
- **Android SDK** - для сборки Android приложения (опционально)
- **C++ компилятор** - для нативных библиотек (опционально)
- **PostgreSQL 16+** - для интеграционных тестов и production
- **Redis 7+** - для кэширования, сессий и репликации
- **Docker 24.0+** - для контейнеризации и локальной разработки

---

## Быстрая установка (рекомендуется)

### Windows: Через Chocolatey

1. Запустите PowerShell от имени администратора
2. Установите Chocolatey (если не установлен):
```powershell
Set-ExecutionPolicy Bypass -Scope Process -Force
[System.Net.ServicePointManager]::SecurityProtocol = [System.Net.ServicePointManager]::SecurityProtocol -bor 3072
iex ((New-Object System.Net.WebClient).DownloadString('https://community.chocolatey.org/install.ps1'))
```

3. Установите все необходимые компоненты:
```powershell
choco install openjdk17 nodejs-lts cmake ffmpeg -y
```

4. Перезапустите терминал

### Linux (Ubuntu/Debian)

```bash
# Обновление системы
sudo apt update && sudo apt upgrade -y

# Java JDK 17
sudo apt install openjdk-17-jdk -y

# Node.js 20
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs

# CMake
sudo apt install cmake -y

# FFmpeg
sudo apt install ffmpeg -y
```

### macOS

```bash
# Через Homebrew
brew install openjdk@17 node@20 cmake ffmpeg
```

---

## Детальная установка

### 1. Java JDK 17+

#### Windows:
- **Скачать**: https://adoptium.net/
- **Выбрать**: OpenJDK 17 (LTS)
- **Проверка**: `java -version` и `javac -version`

#### Linux:
```bash
sudo apt install openjdk-17-jdk -y
```

#### macOS:
```bash
brew install openjdk@17
```

### 2. Node.js 20+

#### Windows:
- **Скачать**: https://nodejs.org/
- **Выбрать**: LTS версию (20.x или выше)
- **Проверка**: `node --version` и `npm --version`

#### Linux:
```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
```

#### macOS:
```bash
brew install node@20
```

### 3. CMake 3.15+

#### Windows:
- **Скачать**: https://cmake.org/download/
- **Установить**: Windows x64 Installer
- **Проверка**: `cmake --version`

#### Linux:
```bash
sudo apt install cmake -y
```

#### macOS:
```bash
brew install cmake
```

### 4. FFmpeg (опционально, для обработки видео)

#### Windows:
```powershell
choco install ffmpeg -y
```

#### Linux:
```bash
sudo apt install ffmpeg -y
```

#### macOS:
```bash
brew install ffmpeg
```

**Примечание**: Для сборки нативных библиотек также нужны dev библиотеки FFmpeg. На Windows рекомендуется использовать vcpkg.

### 5. Android SDK (только для сборки Android приложения)

#### Установка Android Studio:
1. Скачайте: https://developer.android.com/studio
2. Установите Android Studio
3. Настройте переменную окружения:
   ```powershell
   # Windows
   [System.Environment]::SetEnvironmentVariable("ANDROID_HOME", "C:\Users\$env:USERNAME\AppData\Local\Android\Sdk", "User")

   # Linux/macOS
   export ANDROID_HOME=$HOME/Android/Sdk
   echo 'export ANDROID_HOME=$HOME/Android/Sdk' >> ~/.bashrc
   ```

4. Установите необходимые компоненты:
   ```bash
   $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
   ```

#### Альтернатива: Только Command-line Tools
См. https://developer.android.com/studio#command-tools

### 6. C++ компилятор (только для нативных библиотек)

#### Windows:
- **Вариант 1**: Visual Studio 2022 с компонентами "Desktop development with C++"
- **Вариант 2**: MinGW-w64 через Chocolatey: `choco install mingw -y`

#### Linux:
```bash
sudo apt install build-essential -y
```

#### macOS:
```bash
# Установите Xcode Command Line Tools
xcode-select --install
```

---

## Проверка установки

### Автоматическая проверка

#### Windows:
```powershell
.\scripts\check-dependencies.ps1
```

#### Linux/macOS:
```bash
./scripts/check-dependencies.sh
```

### Ручная проверка

```bash
# Java
java -version
javac -version

# Node.js
node --version
npm --version

# CMake
cmake --version

# FFmpeg (если установлен)
ffmpeg -version

# Android SDK (если установлен)
echo $ANDROID_HOME
```

---

## Минимальная установка

Если вам не нужны нативные библиотеки и веб-интерфейс, достаточно установить только:

- **Java JDK 17+**
- **Gradle** (входит в проект как wrapper)

Для сборки только Kotlin модулей:

```bash
./gradlew :core:common:build
./gradlew :core:network:build
./gradlew :shared:build
./gradlew :server:api:build
```

---

## Следующие шаги

1. Перезапустите терминал после установки новых компонентов
2. Проверьте установку: `.\scripts\check-dependencies.ps1` (Windows) или `./scripts/check-dependencies.sh` (Linux/macOS)
3. Начните сборку проекта:
   ```bash
   ./gradlew build
   ```
4. Для развертывания см. [ENGINEER_GUIDE.md](../../archive/docs/guides/ENGINEER_GUIDE.md)

---

## Устранение проблем

### Проблема: Компонент не найден после установки

**Решение:**
- Перезапустите терминал
- Проверьте переменные окружения PATH
- На Windows: перезагрузите компьютер

### Проблема: Версия компонента не соответствует требованиям

**Решение:**
- Обновите компонент до требуемой версии
- Проверьте совместимость версий

### Проблема: Права доступа при установке

**Решение:**
- На Windows: запустите PowerShell от имени администратора
- На Linux/macOS: используйте `sudo` для системных пакетов

---

## Дополнительные ресурсы

- **[LOCAL_DEVELOPMENT_REQUIREMENTS.md](../../LOCAL_DEVELOPMENT_REQUIREMENTS.md)** - Полный справочник требований (Windows/Linux/macOS)
- **[ENGINEER_GUIDE.md](../../archive/docs/guides/ENGINEER_GUIDE.md)** - Полное руководство для инженеров
- **[DEVELOPMENT.md](../DEVELOPMENT.md)** - Руководство по разработке
- **[BUILD_QUICK_REFERENCE.md](../../archive/docs/guides/BUILD_QUICK_REFERENCE.md)** - Быстрая справка по сборке
- **[BUILD_TROUBLESHOOTING.md](../../archive/docs/guides/BUILD_TROUBLESHOOTING.md)** - Устранение проблем при сборке
- **[docs/installation/QUICK_INSTALL.md](QUICK_INSTALL.md)** - Быстрая установка

---

**Версия документации:** 2.0
**Последнее обновление:** 28 January 2026


