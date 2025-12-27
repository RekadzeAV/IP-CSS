# Требования для локальной сборки модулей проекта IP-CSS

**Версия документации:** 1.0
**Дата последнего обновления:** 27 декабря 2025
**Версия проекта:** Alfa-0.0.1

---

## Общие требования (для всех модулей)

### Критически важные (P0)

1. **JDK 17+** (Java Development Kit)
   - Требуется для: всех Kotlin/JVM модулей
   - Версия: JDK 17 (LTS) или выше
   - Проверка: `java -version`, `javac -version`

2. **Gradle 8.4+**
   - Требуется для: всех Gradle модулей
   - Версия: 8.4 (определяется в `gradle/wrapper/gradle-wrapper.properties`)
   - Включен в проект через Gradle Wrapper (`gradlew`, `gradlew.bat`)
   - Проверка: `./gradlew --version`

3. **Kotlin 2.0.21**
   - Требуется для: всех Kotlin модулей
   - Устанавливается автоматически через Gradle plugins
   - Версия указана в `build.gradle.kts`

---

## Модули проекта и их требования

### 1. `core:common` - Базовый модуль

**Описание:** Базовые типы и утилиты, используемые во всех модулях.

**Зависимости модуля:**
- Нет внутренних зависимостей на другие модули проекта

**Требования для сборки:**

**Критически важные (P0):**
- ✅ JDK 17+
- ✅ Gradle 8.4+
- ✅ Kotlin Multiplatform Plugin (автоматически)

**Платформы сборки:**
- Android (compileSdk 34, minSdk 26)
- JVM Desktop (Java 11)
- iOS (iosX64, iosArm64, iosSimulatorArm64)

**Внешние зависимости:**
- kotlinx-serialization-json (1.7.0)
- androidx.security:security-crypto (Android only)

**Команда сборки:**
```bash
./gradlew :core:common:build
```

**Публикация в Maven Local:**
```bash
./gradlew :core:common:publishToMavenLocal
```

---

### 2. `core:network` - Сетевой модуль

**Описание:** Сетевые клиенты (ONVIF, RTSP, WebSocket, HTTP).

**Зависимости модуля:**
- `:core:common` (обязательно)

**Требования для сборки:**

**Критически важные (P0):**
- ✅ JDK 17+
- ✅ Gradle 8.4+
- ✅ Kotlin Multiplatform Plugin
- ✅ Собранный модуль `:core:common`

**Высокий приоритет (P1):**
- ⚠️ **CMake 3.15+** (для cinterop с нативными библиотеками)
- ⚠️ **FFmpeg с dev библиотеками** (для RTSP клиента через cinterop):
  - libavformat-dev
  - libavcodec-dev
  - libavutil-dev
  - libswscale-dev
  - libswresample-dev
- ⚠️ **C++ компилятор** (GCC/Clang, C++17 стандарт)
- ⚠️ **pkg-config** (для поиска библиотек)

**Платформы сборки:**
- Android (compileSdk 34, minSdk 26)
- Android Native (androidNativeArm32, androidNativeArm64, androidNativeX86, androidNativeX64)
- JVM Desktop (Java 11)
- iOS (iosX64, iosArm64, iosSimulatorArm64)
- Linux x64 (native)
- macOS (x64, arm64)
- Windows (MinGW x64)

**Внешние зависимости:**
- kotlinx-serialization-json (1.7.0)
- kotlinx-serialization-xml
- ktor-client (2.3.12) - все платформенные варианты
- kotlinx-coroutines-core (1.8.0)
- kotlin-logging (3.0.5)
- kotlinx-datetime (0.6.0)

**Нативные библиотеки (через cinterop):**
- `native/video-processing/lib/*` (для RTSP клиента)

**Команда сборки:**
```bash
# Сначала собрать core:common
./gradlew :core:common:build
# Затем core:network
./gradlew :core:network:build
```

**Публикация в Maven Local:**
```bash
./gradlew :core:network:publishToMavenLocal
```

**Примечание:** Для сборки с нативными библиотеками необходимо предварительно собрать `native/video-processing` через CMake.

---

### 3. `core:license` - Модуль лицензирования

**Описание:** Система управления лицензиями.

**Зависимости модуля:**
- `:shared` (обязательно)
- `:core:common` (через shared)

**Требования для сборки:**

**Критически важные (P0):**
- ✅ JDK 17+
- ✅ Gradle 8.4+
- ✅ Kotlin Multiplatform Plugin
- ✅ Собранные модули: `:core:common`, `:shared`

**Платформы сборки:**
- Android (compileSdk 34, minSdk 26)
- JVM Desktop (Java 11)
- iOS (iosX64, iosArm64, iosSimulatorArm64)

**Внешние зависимости:**
- kotlinx-serialization-json (1.7.0)
- kotlinx-coroutines-core (1.8.0)
- kotlin-logging (3.0.5)
- androidx.security:security-crypto (Android only)
- bouncycastle-provider (Android only)
- bouncycastle-pkix (Android only)

**Команда сборки:**
```bash
# Сначала собрать зависимости
./gradlew :core:common:build :shared:build
# Затем core:license
./gradlew :core:license:build
```

**Публикация в Maven Local:**
```bash
./gradlew :core:license:publishToMavenLocal
```

---

### 4. `shared` - Общий Kotlin Multiplatform модуль

**Описание:** Основная бизнес-логика, используемая на всех платформах.

**Зависимости модуля:**
- `:core:common` (обязательно)
- `:core:network` (обязательно)

**Требования для сборки:**

**Критически важные (P0):**
- ✅ JDK 17+
- ✅ Gradle 8.4+
- ✅ Kotlin Multiplatform Plugin
- ✅ SQLDelight Plugin (2.0.3)
- ✅ Собранные модули: `:core:common`, `:core:network`

**Платформы сборки:**
- Android (compileSdk 34, minSdk 26, targetSdk 34)
- JVM Desktop (Java 11)
- iOS (iosX64, iosArm64, iosSimulatorArm64)

**Внешние зависимости:**
- kotlinx-serialization-json (1.7.0)
- kotlinx-coroutines-core (1.8.0)
- ktor-client (2.3.12)
- sqldelight-runtime (2.0.3)
- sqldelight-drivers (платформо-специфичные)
- kotlin-logging (3.0.5)
- kotlinx-datetime (0.6.0)
- koin-core (3.6.0)
- androidx.work:work-runtime-ktx (2.9.1) (Android only)

**База данных:**
- SQLDelight база данных: `CameraDatabase`
- Миграции: `src/commonMain/sqldelight/migrations`

**Команда сборки:**
```bash
# Сначала собрать зависимости
./gradlew :core:common:build :core:network:build
# Затем shared
./gradlew :shared:build
```

**Публикация в Maven Local:**
```bash
./gradlew :shared:publishToMavenLocal
```

---

### 5. `android:app` - Android приложение

**Описание:** Android клиентское приложение.

**Зависимости модуля:**
- `:shared` (обязательно)
- `:core:network` (обязательно)

**Требования для сборки:**

**Критически важные (P0):**
- ✅ JDK 17+
- ✅ Gradle 8.4+
- ✅ **Android SDK** (обязательно):
  - Android SDK Build Tools 34.0.0+
  - Android SDK Platform 34
  - Android SDK Platform-Tools
  - Android SDK Command-line Tools
- ✅ **Android Gradle Plugin 8.7.0**
- ✅ Kotlin Android Plugin
- ✅ Jetpack Compose Plugin 1.7.0
- ✅ Собранные модули: `:core:common`, `:core:network`, `:shared`

**Конфигурация Android:**
- compileSdk: 34
- minSdk: 26
- targetSdk: 34
- package: com.company.ipcamera.android

**Внешние зависимости:**
- Jetpack Compose (1.7.0):
  - compose.runtime
  - compose.foundation
  - compose.material3
  - compose.ui
  - compose.uiToolingPreview
  - compose.activityCompose
- AndroidX:
  - core-ktx (1.13.1)
  - lifecycle-runtime-ktx (2.8.6)
  - lifecycle-viewmodel-compose (2.8.6)
  - navigation-compose (2.8.4)
- Koin (3.6.0) + koin-androidx-compose
- kotlinx-coroutines-android (1.8.0)
- logback-classic (1.5.9)
- Media3 ExoPlayer (1.2.1):
  - media3-exoplayer
  - media3-ui
  - media3-common
  - media3-datasource-rtsp

**Команда сборки:**
```bash
# Сначала собрать зависимости
./gradlew :core:common:build :core:network:build :shared:build
# Затем Android приложение
./gradlew :android:app:assembleDebug
# Или для release
./gradlew :android:app:assembleRelease
```

**Установка Android SDK:**
```bash
# Windows (через Android Studio или через командную строку)
# Установить Android Studio и SDK через SDK Manager
# Или через командную строку:
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"

# Linux/macOS
# Аналогично
```

**Переменные окружения:**
```bash
export ANDROID_HOME=$HOME/Android/Sdk
export PATH=$PATH:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools
```

---

### 6. `server:api` - Серверный API (JVM)

**Описание:** Ktor серверный API на JVM.

**Зависимости модуля:**
- `:shared` (обязательно)
- `:core:network` (обязательно)

**Требования для сборки:**

**Критически важные (P0):**
- ✅ **JDK 17** (обязательно, указано jvmToolchain(17))
- ✅ Gradle 8.4+
- ✅ Kotlin JVM Plugin
- ✅ Собранные модули: `:core:common`, `:core:network`, `:shared`

**Внешние зависимости:**
- ktor-server (2.3.12):
  - ktor-server-core
  - ktor-server-netty
  - ktor-server-content-negotiation
  - ktor-server-cors
  - ktor-server-call-logging
  - ktor-server-websockets
  - ktor-server-auth
  - ktor-server-auth-jwt
  - ktor-server-sessions
- kotlinx-serialization-json (1.7.0)
- kotlin-logging (3.0.5) + logback-classic (1.5.9)
- koin-core (3.6.0) + koin-ktor (3.6.0)
- kotlinx-coroutines-core (1.8.0)
- java-jwt (4.4.0) - JWT библиотека
- jbcrypt (0.4) - BCrypt для паролей
- lettuce-core (6.3.2.RELEASE) - Redis клиент

**Дополнительные сервисы (опционально):**
- Redis сервер (для кэширования и сессий)

**Команда сборки:**
```bash
# Сначала собрать зависимости
./gradlew :core:common:build :core:network:build :shared:build
# Затем server:api
./gradlew :server:api:build
```

**Запуск:**
```bash
./gradlew :server:api:run
```

**JAR файл:**
```bash
./gradlew :server:api:jar
# JAR будет в: server/api/build/libs/
```

---

### 7. `server:web` - Веб-интерфейс (Node.js)

**Описание:** Next.js веб-интерфейс для системы.

**Тип проекта:** Node.js/Next.js (не Gradle модуль)

**Требования для сборки:**

**Критически важные (P0):**
- ✅ **Node.js 20.0.0+** (обязательно, указано в package.json engines)
- ✅ **npm 10.0.0+** (обязательно)

**Технологии:**
- Next.js 15.0.3
- React 18.3.1
- TypeScript 5.6.3
- Material-UI (MUI) 5.16.7
- Redux Toolkit 2.2.7

**Внешние зависимости:**
- React и React DOM (18.3.1)
- Next.js (15.0.3)
- Material-UI компоненты
- Redux Toolkit и react-redux
- Axios (1.7.7) - HTTP клиент
- socket.io-client (4.7.5) - WebSocket клиент
- Видео библиотеки (react-player, hls.js, video.js)
- И другие (см. server/web/package.json)

**Dev зависимости:**
- TypeScript
- ESLint
- Prettier
- Jest + Testing Library

**Команда сборки:**
```bash
cd server/web
npm install
npm run build
```

**Запуск в режиме разработки:**
```bash
cd server/web
npm install
npm run dev
# Приложение доступно на http://localhost:3000
```

**Проверка:**
```bash
node --version  # Должно быть >= 20.0.0
npm --version   # Должно быть >= 10.0.0
```

---

### 8. `native` - Нативные C++ библиотеки

**Описание:** Нативные библиотеки для обработки видео и аналитики.

**Тип проекта:** CMake (не Gradle модуль)

**Структура:**
- `native/video-processing/` - RTSP клиент, обработка видео
- `native/analytics/` - AI аналитика
- `native/codecs/` - Кодеки

**Требования для сборки:**

**Критически важные (P0):**
- ✅ **CMake 3.15+** (обязательно)
- ✅ **C++ компилятор с поддержкой C++17:**
  - GCC (Linux)
  - Clang (macOS, Linux)
  - MSVC или MinGW-w64 (Windows)

**Высокий приоритет (P1):**
- ⚠️ **FFmpeg с dev библиотеками:**
  - libavformat-dev
  - libavcodec-dev
  - libavutil-dev
  - libswscale-dev
  - libswresample-dev
- ⚠️ **pkg-config** (для поиска библиотек)

**Средний приоритет (P2):**
- ⚠️ **OpenCV 4.x** (опционально, можно отключить через `ENABLE_OPENCV=OFF`)
  - Для обработки изображений и детекции движения

**Низкий приоритет (P3):**
- ⚠️ **TensorFlow Lite** (опционально, можно отключить через `ENABLE_TENSORFLOW=OFF`)
  - Для AI/ML inference
- ⚠️ **CUDA** (опционально, для NVIDIA GPU)
- ⚠️ **OpenCL** (опционально, для GPU ускорения)

**Команда сборки:**
```bash
cd native
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build . --config Release
```

**С опциями:**
```bash
cmake .. \
  -DCMAKE_BUILD_TYPE=Release \
  -DENABLE_OPENCV=ON \
  -DENABLE_TENSORFLOW=OFF \
  -DENABLE_GPU=OFF \
  -DENABLE_FFMPEG=ON
```

**Платформо-специфичная сборка:**

**Linux:**
```bash
sudo apt-get install cmake build-essential g++ \
  ffmpeg libavformat-dev libavcodec-dev libavutil-dev \
  libswscale-dev libswresample-dev pkg-config
cd native && mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build .
```

**Windows:**
```powershell
# Установить CMake, Visual Studio Build Tools или MinGW
# Установить FFmpeg (через Chocolatey: choco install ffmpeg)
cd native
mkdir build
cd build
cmake .. -G "MinGW Makefiles" -DCMAKE_BUILD_TYPE=Release
cmake --build .
```

**macOS:**
```bash
brew install cmake ffmpeg pkg-config
cd native && mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build .
```

**Выходные файлы:**
- Библиотеки будут в `native/build/lib/`
- Исполняемые файлы в `native/build/bin/`

---

## Порядок сборки всех модулей

### Минимальная сборка (без нативных библиотек и веб-интерфейса)

```bash
# 1. Собрать базовые модули
./gradlew :core:common:build
./gradlew :core:network:build

# 2. Собрать shared
./gradlew :shared:build

# 3. Собрать core:license
./gradlew :core:license:build

# 4. Собрать server:api
./gradlew :server:api:build
```

### Полная сборка всех модулей

```bash
# 1. Собрать нативные библиотеки (если нужны)
cd native
mkdir -p build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build .
cd ../..

# 2. Собрать Kotlin модули
./gradlew buildAll

# 3. Собрать Android приложение
./gradlew :android:app:assembleDebug

# 4. Собрать веб-интерфейс
cd server/web
npm install
npm run build
cd ../..
```

### Использование готового скрипта

```bash
# Сборка всех модулей (если скрипт существует)
./scripts/build-all-platforms.sh
```

---

## Сводная таблица требований по модулям

| Модуль | JDK | Gradle | Android SDK | Node.js | CMake | FFmpeg | C++ Compiler | Другие зависимости |
|--------|-----|--------|-------------|---------|-------|--------|--------------|-------------------|
| `core:common` | ✅ 17+ | ✅ 8.4+ | - | - | - | - | - | - |
| `core:network` | ✅ 17+ | ✅ 8.4+ | - | - | ⚠️ 3.15+ | ⚠️ dev libs | ⚠️ C++17 | pkg-config |
| `core:license` | ✅ 17+ | ✅ 8.4+ | - | - | - | - | - | `:shared` |
| `shared` | ✅ 17+ | ✅ 8.4+ | - | - | - | - | - | SQLDelight |
| `android:app` | ✅ 17+ | ✅ 8.4+ | ✅ SDK 34 | - | - | - | - | Compose, AndroidX |
| `server:api` | ✅ 17 | ✅ 8.4+ | - | - | - | - | - | Ktor Server |
| `server:web` | - | - | - | ✅ 20+ | - | - | - | npm 10+ |
| `native` | - | - | - | - | ✅ 3.15+ | ⚠️ dev libs | ✅ C++17 | pkg-config, OpenCV (opt) |

**Условные обозначения:**
- ✅ - Обязательно
- ⚠️ - Рекомендуется/высокий приоритет
- \- - Не требуется

---

## Быстрая проверка всех зависимостей

```bash
#!/bin/bash
# check-all-dependencies.sh

echo "=== Проверка зависимостей для сборки IP-CSS ==="
echo ""

check_cmd() {
    if command -v $1 &> /dev/null; then
        version=$($1 --version 2>&1 | head -1)
        echo "✅ $1: $version"
        return 0
    else
        echo "❌ $1: НЕ УСТАНОВЛЕН"
        return 1
    fi
}

echo "Критически важные:"
check_cmd java
check_cmd javac
check_cmd ./gradlew

echo ""
echo "Для Android:"
if [ -n "$ANDROID_HOME" ]; then
    echo "✅ ANDROID_HOME: $ANDROID_HOME"
else
    echo "❌ ANDROID_HOME: НЕ УСТАНОВЛЕН"
fi

echo ""
echo "Для веб-интерфейса:"
check_cmd node
check_cmd npm

echo ""
echo "Для нативных библиотек:"
check_cmd cmake
check_cmd g++
check_cmd ffmpeg
check_cmd pkg-config

echo ""
echo "Проверка завершена!"
```

---

## Полезные ссылки

- [Kotlin Multiplatform Documentation](https://kotlinlang.org/docs/multiplatform.html)
- [Gradle User Guide](https://docs.gradle.org/)
- [Android Developer Guide](https://developer.android.com/)
- [Next.js Documentation](https://nextjs.org/docs)
- [CMake Documentation](https://cmake.org/documentation/)
- [FFmpeg Documentation](https://ffmpeg.org/documentation.html)
- [Node.js Documentation](https://nodejs.org/docs/)

---

**Последнее обновление:** 27 декабря 2025
