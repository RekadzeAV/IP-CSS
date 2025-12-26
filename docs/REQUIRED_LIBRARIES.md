# Список необходимых библиотек, расширений и компонентов

## Анализ текущего состояния проекта

**Текущий прогресс:** ~20%
**Архитектура:** Kotlin Multiplatform (Android, iOS) + C++ нативные библиотеки + Next.js веб-интерфейс

---

## 📦 Kotlin Multiplatform библиотеки

### ✅ Уже интегрировано

1. **Ktor Client** (v2.3.5)
   - `io.ktor:ktor-client-core`
   - `io.ktor:ktor-client-content-negotiation`
   - `io.ktor:ktor-serialization-kotlinx-json`
   - `io.ktor:ktor-client-logging`
   - `io.ktor:ktor-client-websockets`
   - `io.ktor:ktor-client-android`
   - `io.ktor:ktor-client-darwin`

2. **Kotlinx Serialization** (v1.6.0)
   - `org.jetbrains.kotlinx:kotlinx-serialization-json`

3. **Kotlinx Coroutines** (v1.7.3)
   - `org.jetbrains.kotlinx:kotlinx-coroutines-core`
   - `org.jetbrains.kotlinx:kotlinx-coroutines-test`

4. **SQLDelight** (v2.0.0)
   - `app.cash.sqldelight:runtime`
   - `app.cash.sqldelight:android-driver`
   - `app.cash.sqldelight:native-driver`
   - `app.cash.sqldelight:sqlite-driver`

5. **Kotlin Logging** (v3.0.5)
   - `io.github.microutils:kotlin-logging`

6. **Kotlinx DateTime** (v0.5.0)
   - `org.jetbrains.kotlinx:kotlinx-datetime`

### ❌ Необходимо добавить

#### 1. **XML парсинг для ONVIF**
```kotlin
// Вариант 1: Ktor XML (рекомендуется)
implementation("io.ktor:ktor-serialization-kotlinx-xml:2.3.5")

// Вариант 2: Kotlinx XML (альтернатива)
implementation("org.jetbrains.kotlinx:kotlinx-serialization-xml:1.6.0")
```

#### 2. **Криптография для лицензирования**
```kotlin
// Для Android
androidMain {
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
}

// Для iOS (через Kotlin/Native)
// Использовать Security framework через cinterop

// Общая криптография (если доступна)
implementation("org.jetbrains.kotlinx:kotlinx-io:0.4.0")
```

#### 3. **Диапазоны версий и управление зависимостями**
```kotlin
// Version Catalog (рекомендуется создать libs.versions.toml)
// Для централизованного управления версиями
```

#### 4. **Тестирование**
```kotlin
// MockK для моков
commonTest {
    implementation("io.mockk:mockk:1.13.8")
}

// Turbine для тестирования Flow
commonTest {
    implementation("app.cash.turbine:turbine:1.0.0")
}
```

#### 5. **Dependency Injection (опционально)**
```kotlin
// Koin для KMP
implementation("io.insert-koin:koin-core:3.5.0")
implementation("io.insert-koin:koin-test:3.5.0")

// Или Kodein
implementation("org.kodein.di:kodein-di:7.20.2")
```

#### 6. **Управление состоянием**
```kotlin
// StateFlow уже есть через Coroutines
// Дополнительно можно добавить:
implementation("com.arkivanov.mvikotlin:mvikotlin:4.0.0")
// Или
implementation("com.badoo.reaktive:reaktive:1.2.2")
```

---

## 🎥 Видео обработка и RTSP

### ❌ Критически необходимо

#### 1. **RTSP клиент библиотека**

**Вариант A: Live555 (C++)** - Рекомендуется
```cmake
# В CMakeLists.txt
# Требуется добавить Live555 как подмодуль или через FetchContent
include(FetchContent)
FetchContent_Declare(
    live555
    GIT_REPOSITORY https://github.com/rgaufman/live555.git
    GIT_TAG master
)
FetchContent_MakeAvailable(live555)
```

**Вариант B: libVLC (C/C++)**
```cmake
find_package(VLC REQUIRED)
# Или через pkg-config на Linux
```

**Вариант C: FFmpeg (C)**
```cmake
find_package(FFmpeg REQUIRED COMPONENTS avformat avcodec avutil swscale)
# Или через vcpkg/conan
```

#### 2. **Декодирование видео**

**FFmpeg** (рекомендуется для кроссплатформенности)
```cmake
# В native/video-processing/CMakeLists.txt
find_package(PkgConfig REQUIRED)
pkg_check_modules(FFMPEG REQUIRED libavformat libavcodec libavutil libswscale)
```

**Альтернативы:**
- **MediaCodec** (Android) - встроен в Android SDK
- **VideoToolbox** (iOS) - встроен в iOS SDK
- **GStreamer** (Linux/Desktop) - альтернатива FFmpeg

#### 3. **Обработка изображений**

**OpenCV** (уже упомянут в CMakeLists.txt, но не интегрирован)
```cmake
# В native/CMakeLists.txt
find_package(OpenCV REQUIRED)
target_link_libraries(video_processing ${OpenCV_LIBS})
```

**Для Android:**
```kotlin
// В android/build.gradle.kts
dependencies {
    implementation("org.opencv:opencv-android:4.8.0")
}
```

#### 4. **Запись видео**

**FFmpeg** (для кодирования в MP4/MKV)
```cmake
# Требуется libavformat, libavcodec
```

**Платформо-специфичные:**
- **Android:** MediaRecorder API (встроен)
- **iOS:** AVFoundation (встроен)

---

## 🤖 AI и машинное обучение

### ❌ Необходимо добавить

#### 1. **TensorFlow Lite** (уже упомянут в CMakeLists.txt)

**Для Android:**
```kotlin
androidMain {
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0") // GPU ускорение
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
}
```

**Для iOS:**
```cmake
# Через CocoaPods или SPM
# Или статическая библиотека
```

**Для C++ (нативный код):**
```cmake
# В native/analytics/CMakeLists.txt
# Требуется скачать TensorFlow Lite C++ API
```

#### 2. **ONNX Runtime** (альтернатива TensorFlow Lite)
```kotlin
// Для Android
implementation("com.microsoft.onnxruntime:onnxruntime-android:1.16.0")
```

#### 3. **OpenCV DNN модуль** (для предобработки)
```cmake
# OpenCV с модулем DNN
find_package(OpenCV REQUIRED COMPONENTS core imgproc dnn)
```

#### 4. **Библиотеки для детекции объектов**

**YOLO модели:**
- YOLOv8 через TensorFlow Lite
- Или через ONNX Runtime

**Дополнительные модели:**
- Face detection: MediaPipe или OpenCV
- ANPR (распознавание номеров): Tesseract OCR + предобработка

---

## 🔐 Безопасность и лицензирование

### ❌ Необходимо добавить

#### 1. **Криптография**

**Android:**
```kotlin
androidMain {
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    // Для AES шифрования
    implementation("org.bouncycastle:bcprov-jdk15on:1.70")
    implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
}
```

**iOS:**
- Использовать Security framework через cinterop
- Или использовать общую библиотеку через Kotlin/Native

#### 2. **Хеширование**
```kotlin
// SHA-256, MD5 (через platform-specific реализации)
// Android: java.security.MessageDigest
// iOS: CommonCrypto через cinterop
```

#### 3. **JWT токены** (для API аутентификации)
```kotlin
implementation("com.auth0:java-jwt:4.4.0") // Только для JVM
// Для KMP нужна альтернатива или platform-specific реализация
```

---

## 📱 UI библиотеки (для будущих модулей)

### Android

#### 1. **Jetpack Compose**
```kotlin
// Уже упомянут в build.gradle.kts
implementation("androidx.compose.ui:ui:1.5.3")
implementation("androidx.compose.ui:ui-tooling-preview:1.5.3")
implementation("androidx.compose.material3:material3:1.1.1")
implementation("androidx.compose.runtime:runtime-livedata:1.5.3")
```

#### 2. **Compose для видео**
```kotlin
// Для отображения видео потоков
implementation("androidx.media3:media3-exoplayer:1.2.0")
implementation("androidx.media3:media3-ui:1.2.0")
```

#### 3. **Navigation**
```kotlin
implementation("androidx.navigation:navigation-compose:2.7.5")
```

#### 4. **ViewModel и Lifecycle**
```kotlin
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
```

### iOS

#### 1. **SwiftUI** (нативный, не требует библиотек)
- Использовать встроенные компоненты iOS

#### 2. **Video Player**
- AVKit (встроен в iOS)
- Или использовать SwiftUI VideoPlayer

---

## 🌐 Веб-интерфейс (Next.js)

### ✅ Уже интегрировано

1. **React** (v18.2.0)
2. **Next.js** (v14.0.4)
3. **Material-UI** (v5.14.20)
4. **Redux Toolkit** (v2.0.1)
5. **Axios** (v1.6.2)
6. **Socket.io-client** (v4.6.1)
7. **React Player** (v2.13.0)
8. **React Hook Form** (v7.49.2)
9. **Yup** (v1.3.3)

### ❌ Рекомендуется добавить

#### 1. **Видео стриминг**
```json
{
  "hls.js": "^1.4.12",
  "video.js": "^8.6.1"
}
```

#### 2. **WebRTC** (для прямого стриминга)
```json
{
  "simple-peer": "^9.11.1",
  "socket.io-client": "^4.6.1" // уже есть
}
```

#### 3. **Графики и аналитика**
```json
{
  "recharts": "^2.10.3" // уже есть
}
```

#### 4. **Управление состоянием форм**
```json
{
  "react-hook-form": "^7.49.2", // уже есть
  "@hookform/resolvers": "^3.3.2" // уже есть
}
```

#### 5. **Уведомления**
```json
{
  "react-toastify": "^9.1.3",
  "notistack": "^3.0.1"
}
```

#### 6. **Загрузка файлов**
```json
{
  "react-dropzone": "^14.2.3" // уже есть
}
```

---

## 🖥️ Desktop приложения (если планируется)

### Compose Multiplatform Desktop

```kotlin
// В desktop/build.gradle.kts
implementation(compose.desktop.currentOs)

// Для видео
implementation("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:0.7.7")
```

---

## 🔧 Инструменты разработки

### ❌ Рекомендуется добавить

#### 1. **Version Catalog**
Создать `gradle/libs.versions.toml` для централизованного управления версиями

#### 2. **Detekt** (статический анализ кода)
```kotlin
// В build.gradle.kts
plugins {
    id("io.gitlab.arturbosch.detekt") version "1.23.1"
}
```

#### 3. **Ktlint** (форматирование)
```kotlin
plugins {
    id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
}
```

#### 4. **Dokka** (документация)
```kotlin
plugins {
    id("org.jetbrains.dokka") version "1.9.10"
}
```

---

## 📦 Нативные зависимости (C++)

### ❌ Критически необходимо

#### 1. **RTSP библиотека**
- **Live555** (рекомендуется) - полная реализация RTSP
- **libVLC** - альтернатива с поддержкой множества форматов
- **FFmpeg** - для декодирования и кодирования

#### 2. **Медиа обработка**
- **FFmpeg** (libavformat, libavcodec, libavutil, libswscale)
- **OpenCV** (для обработки изображений)

#### 3. **AI/ML**
- **TensorFlow Lite C++ API**
- **ONNX Runtime C++** (альтернатива)

#### 4. **Системные библиотеки**
```cmake
find_package(Threads REQUIRED)
find_package(ZLIB REQUIRED)
find_package(OpenSSL REQUIRED) # Для HTTPS/RTSP over TLS
```

---

## 🚀 Серверная часть (если планируется Ktor/Spring Boot)

### Ktor Server (рекомендуется для Kotlin)

```kotlin
// В server/build.gradle.kts
implementation("io.ktor:ktor-server-core:2.3.5")
implementation("io.ktor:ktor-server-netty:2.3.5")
implementation("io.ktor:ktor-server-content-negotiation:2.3.5")
implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.5")
implementation("io.ktor:ktor-server-websockets:2.3.5")
implementation("io.ktor:ktor-server-cors:2.3.5")
implementation("io.ktor:ktor-server-auth:2.3.5")
implementation("io.ktor:ktor-server-auth-jwt:2.3.5")
```

### База данных
```kotlin
// PostgreSQL
implementation("org.postgresql:postgresql:42.7.1")
implementation("com.zaxxer:HikariCP:5.1.0")

// Или Exposed (Kotlin ORM)
implementation("org.jetbrains.exposed:exposed-core:0.44.1")
implementation("org.jetbrains.exposed:exposed-dao:0.44.1")
implementation("org.jetbrains.exposed:exposed-jdbc:0.44.1")
```

---

## 📋 Приоритетный список интеграции

### 🔴 Критический приоритет (для базовой функциональности)

1. **XML парсинг для ONVIF** - `ktor-serialization-kotlinx-xml`
2. **RTSP библиотека (C++)** - Live555 или FFmpeg
3. **FFmpeg** - для декодирования видео
4. **OpenCV** - для обработки изображений
5. **Криптография** - для лицензирования

### 🟡 Высокий приоритет (для полной функциональности)

6. **TensorFlow Lite** - для AI аналитики
7. **MediaCodec/VideoToolbox** - платформенные декодеры
8. **Jetpack Compose** - для Android UI
9. **Тестирование** - MockK, Turbine
10. **Version Catalog** - управление зависимостями

### 🟢 Средний приоритет (для улучшения)

11. **Dependency Injection** - Koin или Kodein
12. **Детекция объектов** - YOLO модели
13. **OCR** - Tesseract для ANPR
14. **Инструменты разработки** - Detekt, Ktlint, Dokka

---

## 📝 Рекомендации по интеграции

### 1. Начать с критических зависимостей
- XML парсинг для ONVIF
- RTSP библиотека
- FFmpeg для видео

### 2. Постепенная интеграция
- Не добавлять все сразу
- Тестировать каждую библиотеку отдельно
- Документировать процесс интеграции

### 3. Управление версиями
- Создать `libs.versions.toml` для централизованного управления
- Использовать BOM (Bill of Materials) где возможно

### 4. Тестирование
- Добавить тесты для каждой новой библиотеки
- Использовать моки для изоляции тестов

---

## 🔗 Полезные ссылки

- [Ktor Documentation](https://ktor.io/docs/)
- [SQLDelight](https://cashapp.github.io/sqldelight/)
- [Live555](http://www.live555.com/liveMedia/)
- [FFmpeg](https://ffmpeg.org/)
- [OpenCV](https://opencv.org/)
- [TensorFlow Lite](https://www.tensorflow.org/lite)
- [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/)

---

**Дата анализа:** 2025
**Версия проекта:** Alfa-0.0.1
**Статус:** В разработке (~20%)

