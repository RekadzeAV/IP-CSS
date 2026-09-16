# Требования для локальной компиляции пакета под NAS

**Дата создания:** 26 January 2026
**Версия:** 1.0

## Обзор

Данный документ описывает все требования и зависимости, необходимые для локальной компиляции пакетов под NAS системы (Synology, QNAP, Asustor, TrueNAS).

---

## Приоритизация требований

### 🔴 КРИТИЧЕСКИ ВАЖНО (P0) - Без этого сборка невозможна

#### 1. Java Development Kit (JDK) 17
**Приоритет:** P0 - Критический
**Назначение:** Компиляция Kotlin/JVM модулей (`:server:api`)

**Требования:**
- JDK 17 (LTS)
- JVM Toolchain 17 (указано в `server/api/build.gradle.kts`)

**Установка:**

**Windows:**
```powershell
# Через Chocolatey
choco install openjdk17

# Или скачать с https://adoptium.net/
# Установить и добавить в PATH
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get update
sudo apt-get install openjdk-17-jdk
```

**macOS:**
```bash
brew install openjdk@17
```

**Проверка:**
```bash
java -version
# Должно показать: openjdk version "17.x.x"
javac -version
```

---

#### 2. Gradle Build Tool
**Приоритет:** P0 - Критический
**Назначение:** Система сборки проекта

**Требования:**
- Gradle Wrapper уже включен в проект (`gradlew`, `gradlew.bat`)
- Минимальная версия: 8.0+ (определяется в `gradle/wrapper/gradle-wrapper.properties`)

**Проверка:**
```bash
# Windows
.\gradlew.bat --version

# Linux/macOS
./gradlew --version
```

**Примечание:** Gradle автоматически скачает нужную версию при первом запуске.

---

#### 3. Kotlin Multiplatform Plugin
**Приоритет:** P0 - Критический
**Назначение:** Компиляция Kotlin модулей

**Требования:**
- Kotlin 2.0.21 (указано в `build.gradle.kts`)
- Устанавливается автоматически через Gradle plugins

**Проверка:**
```bash
./gradlew :shared:build --dry-run
```

---

### 🟠 ВЫСОКИЙ ПРИОРИТЕТ (P1) - Необходимо для полной функциональности

#### 4. CMake 3.15+
**Приоритет:** P1 - Высокий
**Назначение:** Сборка нативных C++ библиотек (`native/`)

**Требования:**
- CMake ≥ 3.15
- Необходим для сборки модулей:
  - `native/video-processing/` (RTSP клиент)
  - `native/analytics/` (AI аналитика)
  - `native/codecs/` (кодеки)

**Установка:**

**Windows:**
```powershell
# Через Chocolatey
choco install cmake

# Или скачать с https://cmake.org/download/
# Добавить в PATH
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get install cmake build-essential
```

**macOS:**
```bash
brew install cmake
```

**Проверка:**
```bash
cmake --version
# Должно показать: cmake version 3.15 или выше
```

---

#### 5. FFmpeg (с dev библиотеками)
**Приоритет:** P1 - Высокий
**Назначение:** RTSP клиент, декодирование/кодирование видео

**Требования:**
- FFmpeg с библиотеками:
  - `libavformat-dev`
  - `libavcodec-dev`
  - `libavutil-dev`
  - `libswscale-dev`
  - `libswresample-dev`

**Установка:**

**Windows:**
```powershell
# Через Chocolatey
choco install ffmpeg

# Или скачать с https://ffmpeg.org/download.html
# Распаковать в C:\ffmpeg
# Установить переменную окружения: FFMPEG_DIR=C:\ffmpeg
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get update
sudo apt-get install ffmpeg \
  libavformat-dev \
  libavcodec-dev \
  libavutil-dev \
  libswscale-dev \
  libswresample-dev \
  pkg-config
```

**macOS:**
```bash
brew install ffmpeg pkg-config
```

**Проверка:**
```bash
ffmpeg -version
pkg-config --exists libavformat && echo "FFmpeg OK" || echo "FFmpeg NOT FOUND"
```

---

#### 6. Node.js 20+ и npm 10+
**Приоритет:** P1 - Высокий
**Назначение:** Сборка веб-интерфейса (`server/web/`)

**Требования:**
- Node.js ≥ 20.0.0
- npm ≥ 10.0.0
- Указано в `server/web/package.json` → `engines`

**Установка:**

**Windows:**
```powershell
# Через Chocolatey
choco install nodejs-lts

# Или скачать с https://nodejs.org/
```

**Linux (Ubuntu/Debian):**
```bash
# Через NodeSource
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs
```

**macOS:**
```bash
brew install node@20
```

**Проверка:**
```bash
node --version
npm --version
```

---

#### 7. C++ компилятор
**Приоритет:** P1 - Высокий
**Назначение:** Компиляция нативных библиотек

**Требования:**
- C++17 стандарт
- GCC или Clang

**Установка:**

**Windows:**
```powershell
# Visual Studio Build Tools или MinGW-w64
choco install mingw
# Или установить Visual Studio 2022 с C++ компонентами
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get install build-essential g++ gcc
```

**macOS:**
```bash
# Xcode Command Line Tools
xcode-select --install
# Или через Homebrew
brew install gcc
```

**Проверка:**
```bash
g++ --version
# или
clang++ --version
```

---

### 🟡 СРЕДНИЙ ПРИОРИТЕТ (P2) - Желательно для расширенной функциональности

#### 8. OpenCV 4.x
**Приоритет:** P2 - Средний
**Назначение:** Обработка изображений, детекция движения

**Требования:**
- OpenCV ≥ 4.0
- Опционально (можно отключить через CMake опцию `ENABLE_OPENCV=OFF`)

**Установка:**

**Windows:**
```powershell
# Скачать с https://opencv.org/releases/
# Распаковать в C:\opencv
# Установить переменную окружения: OPENCV_DIR=C:\opencv\build
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get install libopencv-dev
```

**macOS:**
```bash
brew install opencv
```

**Проверка:**
```bash
pkg-config --modversion opencv4
```

---

#### 9. pkg-config
**Приоритет:** P2 - Средний
**Назначение:** Поиск библиотек для CMake

**Требования:**
- Обычно устанавливается вместе с FFmpeg/OpenCV
- Необходим для автоматического поиска библиотек

**Установка:**

**Windows:**
```powershell
# Через MSYS2 или установить вручную
# https://www.freedesktop.org/wiki/Software/pkg-config/
```

**Linux (Ubuntu/Debian):**
```bash
sudo apt-get install pkg-config
```

**macOS:**
```bash
brew install pkg-config
```

**Проверка:**
```bash
pkg-config --version
```

---

### 🟢 НИЗКИЙ ПРИОРИТЕТ (P3) - Опционально, для специфических функций

#### 10. TensorFlow Lite
**Приоритет:** P3 - Низкий
**Назначение:** AI/ML inference (детекция объектов, распознавание лиц)

**Требования:**
- Опционально (можно отключить через CMake опцию `ENABLE_TENSORFLOW=OFF`)
- Требуется только для AI аналитики

**Установка:**
- Скачать с https://www.tensorflow.org/lite
- Установить в `native/third_party/tensorflow/`

---

#### 11. CUDA / OpenCL
**Приоритет:** P3 - Низкий
**Назначение:** GPU ускорение для AI аналитики

**Требования:**
- Опционально (можно отключить через CMake опцию `ENABLE_GPU=OFF`)
- CUDA для NVIDIA GPU
- OpenCL для других GPU

---

#### 12. Инструменты для создания пакетов NAS
**Приоритет:** P3 - Низкий
**Назначение:** Создание `.spk`, `.qpkg`, `.apk` пакетов

**Требования:**

**Synology SPK:**
- Synology Package Toolkit (SDK)
- Или ручное создание через tar/gzip

**QNAP QPKG:**
- QPKG Tool
- Или ручное создание через tar/gzip

**Asustor APK:**
- ADM Toolkit
- Или ручное создание через tar/gzip

**TrueNAS:**
- Docker (для SCALE)
- FreeBSD pkg tools (для CORE)

---

### 🔵 КРОСС-КОМПИЛЯЦИЯ (P2) - Для сборки под другую архитектуру

#### 13. Кросс-компилятор для целевой архитектуры

**Для NAS ARM (ARMv8/aarch64):**
- Если собираете на x86_64, нужен кросс-компилятор:
  - `aarch64-linux-gnu-gcc` (Linux)
  - Или использовать Docker с QEMU

**Для NAS x86_64:**
- Обычно не требуется (если собираете на x86_64)
- Если собираете на ARM, нужен кросс-компилятор:
  - `x86_64-linux-gnu-gcc` (Linux)

**Установка (Linux):**
```bash
# Для ARM64
sudo apt-get install gcc-aarch64-linux-gnu g++-aarch64-linux-gnu

# Для x86_64 (если на ARM)
sudo apt-get install gcc-x86-64-linux-gnu g++-x86-64-linux-gnu
```

**Альтернатива:** Использовать Docker с multi-arch поддержкой

---

## Минимальный набор для базовой сборки

Для минимальной сборки (без нативных библиотек и веб-интерфейса) достаточно:

1. ✅ JDK 17
2. ✅ Gradle (wrapper)
3. ✅ Kotlin Multiplatform Plugin (автоматически)

**Команда:**
```bash
./gradlew :server:api:build
```

---

## Полный набор для полной сборки

Для полной сборки со всеми компонентами:

1. ✅ JDK 17
2. ✅ Gradle (wrapper)
3. ✅ CMake 3.15+
4. ✅ FFmpeg с dev библиотеками
5. ✅ C++ компилятор (GCC/Clang)
6. ✅ Node.js 20+ и npm 10+
7. ✅ pkg-config
8. ⚠️ OpenCV 4.x (опционально)
9. ⚠️ TensorFlow Lite (опционально)

**Команды:**
```bash
# 1. Сборка нативных библиотек
cd native
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release
cd ../..

# 2. Сборка Kotlin модулей
./gradlew buildAll

# 3. Сборка веб-интерфейса
cd server/web
npm install
npm run build
cd ../..
```

---

## Проверка всех зависимостей

Создайте скрипт проверки:

```bash
#!/bin/bash
# check-dependencies.sh

echo "Проверка зависимостей для сборки NAS пакета..."
echo "=============================================="

check_cmd() {
    if command -v $1 &> /dev/null; then
        echo "✅ $1: $(command -v $1)"
        $1 --version 2>&1 | head -1
    else
        echo "❌ $1: НЕ УСТАНОВЛЕН"
    fi
}

echo ""
echo "Критически важные:"
check_cmd java
check_cmd javac
check_cmd ./gradlew

echo ""
echo "Высокий приоритет:"
check_cmd cmake
check_cmd ffmpeg
check_cmd pkg-config
check_cmd node
check_cmd npm
check_cmd g++

echo ""
echo "Средний приоритет:"
pkg-config --exists opencv4 && echo "✅ OpenCV: $(pkg-config --modversion opencv4)" || echo "❌ OpenCV: НЕ УСТАНОВЛЕН"

echo ""
echo "Проверка завершена!"
```

---

## Рекомендуемый порядок установки

### Windows

```powershell
# 1. Установить Chocolatey (если еще не установлен)
# https://chocolatey.org/install

# 2. Установить критически важные
choco install openjdk17 -y

# 3. Установить высокий приоритет
choco install cmake ffmpeg nodejs-lts mingw -y

# 4. Проверить установку
java -version
cmake --version
ffmpeg -version
node --version
```

### Linux (Ubuntu/Debian)

```bash
# 1. Обновить пакеты
sudo apt-get update

# 2. Установить критически важные
sudo apt-get install -y openjdk-17-jdk

# 3. Установить высокий приоритет
sudo apt-get install -y \
  cmake \
  build-essential \
  g++ \
  gcc \
  ffmpeg \
  libavformat-dev \
  libavcodec-dev \
  libavutil-dev \
  libswscale-dev \
  libswresample-dev \
  pkg-config \
  curl

# 4. Установить Node.js 20
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs

# 5. Установить средний приоритет (опционально)
sudo apt-get install -y libopencv-dev

# 6. Проверить установку
java -version
cmake --version
ffmpeg -version
node --version
```

### macOS

```bash
# 1. Установить Homebrew (если еще не установлен)
# /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# 2. Установить критически важные
brew install openjdk@17

# 3. Установить высокий приоритет
brew install cmake ffmpeg node@20 pkg-config

# 4. Установить Xcode Command Line Tools (для C++ компилятора)
xcode-select --install

# 5. Установить средний приоритет (опционально)
brew install opencv

# 6. Настроить PATH для Java
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc

# 7. Проверить установку
java -version
cmake --version
ffmpeg -version
node --version
```

---

## Переменные окружения

Для нестандартных путей установки библиотек:

**Windows:**
```powershell
$env:FFMPEG_DIR = "C:\ffmpeg"
$env:OPENCV_DIR = "C:\opencv\build"
$env:TFLITE_INCLUDE_DIR = "C:\tensorflow\lite\c"
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
```

**Linux/macOS:**
```bash
export FFMPEG_DIR=/usr/local
export OPENCV_DIR=/usr/local
export TFLITE_INCLUDE_DIR=/path/to/tensorflow/lite/c
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk
```

---

## Следующие шаги

После установки всех зависимостей:

1. **Проверить сборку базовых модулей:**
   ```bash
   ./gradlew :core:common:build :shared:build :server:api:build
   ```

2. **Собрать нативные библиотеки:**
   ```bash
   cd native
   mkdir build && cd build
   cmake .. -DCMAKE_BUILD_TYPE=Release
   cmake --build . --config Release
   ```

3. **Собрать веб-интерфейс:**
   ```bash
   cd server/web
   npm install
   npm run build
   ```

4. **Создать NAS пакет:**
   - См. документацию в `platforms/nas-arm/README.md` или `platforms/nas-x86_64/README.md`
   - Использовать скрипты сборки (если созданы)

---

## Полезные ссылки

- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Gradle User Guide](https://docs.gradle.org/)
- [CMake Documentation](https://cmake.org/documentation/)
- [FFmpeg Documentation](https://ffmpeg.org/documentation.html)
- [Node.js Documentation](https://nodejs.org/docs/)
- [Synology Package Toolkit](https://github.com/Synology/Synology_Package_Toolkit)
- [QNAP QPKG Development](https://www.qnap.com/en/how-to/knowledge-base/article/developing-qpkg-applications)

---

## Примечания

- **Минимальная сборка:** Можно собрать только JVM модули без нативных библиотек и веб-интерфейса
- **Полная сборка:** Требует все зависимости из секции P0 и P1
- **Кросс-компиляция:** Для сборки под другую архитектуру рекомендуется использовать Docker
- **CI/CD:** Для автоматической сборки рекомендуется настроить GitHub Actions или GitLab CI

---

**Последнее обновление:** 26 January 2026


