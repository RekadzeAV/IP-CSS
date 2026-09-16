# Руководство по интеграции библиотек

## 🔴 Критические библиотеки - пошаговая интеграция

---

## 1. XML парсинг для ONVIF

### Проблема
ONVIF использует SOAP/XML протокол, текущая реализация использует упрощенный парсинг через регулярные выражения.

### Решение: Ktor XML Serialization

#### Шаг 1: Добавить зависимость
```kotlin
// core/network/build.gradle.kts
val commonMain by getting {
    dependencies {
        // ... существующие зависимости
        implementation("io.ktor:ktor-serialization-kotlinx-xml:2.3.5")
    }
}
```

#### Шаг 2: Обновить OnvifClient.kt
```kotlin
// core/network/src/commonMain/kotlin/.../OnvifClient.kt

import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.xml.*
import kotlinx.serialization.xml.*

// В конструкторе OnvifClient
private val client: HttpClient by lazy {
    HttpClient(engine) {
        install(ContentNegotiation) {
            xml() // Добавить XML поддержку
        }
        // ... остальная конфигурация
    }
}

// Создать data классы для SOAP ответов
@Serializable
@XmlSerialName("Envelope", namespace = "http://www.w3.org/2003/05/soap-envelope")
data class SoapEnvelope(
    @XmlElement(true) val body: SoapBody
)

@Serializable
@XmlSerialName("Body", namespace = "http://www.w3.org/2003/05/soap-envelope")
data class SoapBody(
    @XmlElement(true) val capabilities: CapabilitiesResponse? = null,
    @XmlElement(true) val deviceInformation: DeviceInformationResponse? = null
)

// Использовать в parseCapabilities:
private suspend fun parseCapabilities(xml: String): OnvifCapabilities? {
    return try {
        val envelope = Xml { 
            ignoreUnknownChildren = true
            coerceInputValues = true
        }.decodeFromString<SoapEnvelope>(xml)
        
        // Извлечь данные из envelope.body.capabilities
        // ...
    } catch (e: Exception) {
        logger.error(e) { "Error parsing capabilities" }
        null
    }
}
```

---

## 2. RTSP клиент - интеграция Live555

### Проблема
Текущая реализация RTSP клиента - заглушка, требуется реальная библиотека.

### Решение A: Live555 (рекомендуется)

#### Шаг 1: Добавить Live555 как подмодуль
```bash
cd native/video-processing
git submodule add https://github.com/rgaufman/live555.git third_party/live555
```

#### Шаг 2: Обновить CMakeLists.txt
```cmake
# native/video-processing/CMakeLists.txt

# Добавить Live555
add_subdirectory(third_party/live555)

# Создать библиотеку
add_library(rtsp_client_native SHARED
    src/rtsp_client.cpp
    include/rtsp_client.h
)

target_include_directories(rtsp_client_native PUBLIC
    ${CMAKE_CURRENT_SOURCE_DIR}/include
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/live555/BasicUsageEnvironment/include
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/live555/liveMedia/include
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/live555/groupsock/include
    ${CMAKE_CURRENT_SOURCE_DIR}/third_party/live555/UsageEnvironment/include
)

target_link_libraries(rtsp_client_native
    liveMedia
    groupsock
    BasicUsageEnvironment
    UsageEnvironment
    Threads::Threads
)
```

#### Шаг 3: Обновить rtsp_client.cpp
```cpp
// native/video-processing/src/rtsp_client.cpp

#include "rtsp_client.h"
#include "liveMedia.hh"
#include "BasicUsageEnvironment.hh"

// Использовать Live555 классы для RTSP подключения
// Пример:
class RTSPClientWrapper {
private:
    RTSPClient* rtspClient;
    MediaSession* session;
    // ...
    
public:
    bool connect(const char* url, const char* username, const char* password) {
        // Реализация через Live555
        // ...
    }
};
```

### Решение B: FFmpeg (альтернатива)

#### Шаг 1: Установить FFmpeg
```bash
# Linux
sudo apt-get install libavformat-dev libavcodec-dev libavutil-dev libswscale-dev

# macOS
brew install ffmpeg

# Windows
# Скачать с https://ffmpeg.org/download.html
```

#### Шаг 2: Обновить CMakeLists.txt
```cmake
find_package(PkgConfig REQUIRED)
pkg_check_modules(FFMPEG REQUIRED libavformat libavcodec libavutil libswscale)

target_link_libraries(rtsp_client_native
    ${FFMPEG_LIBRARIES}
)
target_include_directories(rtsp_client_native PUBLIC ${FFMPEG_INCLUDE_DIRS})
```

---

## 3. FFmpeg для декодирования видео

### Шаг 1: Добавить FFmpeg в CMakeLists.txt
```cmake
# native/video-processing/CMakeLists.txt

find_package(PkgConfig REQUIRED)
pkg_check_modules(FFMPEG REQUIRED 
    libavformat 
    libavcodec 
    libavutil 
    libswscale
    libswresample
)

# Создать модуль декодирования
add_library(video_decoder SHARED
    src/video_decoder.cpp
    include/video_decoder.h
)

target_link_libraries(video_decoder
    ${FFMPEG_LIBRARIES}
    Threads::Threads
)

target_include_directories(video_decoder PUBLIC
    ${FFMPEG_INCLUDE_DIRS}
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)
```

### Шаг 2: Создать video_decoder.cpp
```cpp
// native/video-processing/src/video_decoder.cpp

extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libswscale/swscale.h>
}

// Реализация декодера H.264/H.265
// ...
```

### Шаг 3: Интеграция с Kotlin через cinterop
```kotlin
// Создать .def файл для Kotlin/Native
// native/video-processing/video_decoder.def

headers = video_decoder.h
headerFilter = video_decoder.h
package = native.videoprocessing
```

---

## 4. OpenCV интеграция

### Шаг 1: Установить OpenCV
```bash
# Linux
sudo apt-get install libopencv-dev

# macOS
brew install opencv

# Windows
# Скачать с https://opencv.org/releases/
```

### Шаг 2: Обновить CMakeLists.txt
```cmake
# native/CMakeLists.txt

find_package(OpenCV REQUIRED COMPONENTS core imgproc)

if(OpenCV_FOUND)
    message(STATUS "OpenCV version: ${OpenCV_VERSION}")
    message(STATUS "OpenCV libraries: ${OpenCV_LIBS}")
    message(STATUS "OpenCV include dirs: ${OpenCV_INCLUDE_DIRS}")
    
    include_directories(${OpenCV_INCLUDE_DIRS})
endif()

# В video-processing/CMakeLists.txt
target_link_libraries(video_processing ${OpenCV_LIBS})
```

### Шаг 3: Для Android
```kotlin
// android/build.gradle.kts
dependencies {
    implementation("org.opencv:opencv-android:4.8.0")
}
```

### Шаг 4: Использование в коде
```cpp
// native/video-processing/src/image_processor.cpp

#include <opencv2/opencv.hpp>
#include <opencv2/imgproc.hpp>

// Обработка кадра
cv::Mat processFrame(const uint8_t* data, int width, int height) {
    cv::Mat frame(height, width, CV_8UC3, (void*)data);
    cv::Mat processed;
    // Обработка...
    return processed;
}
```

---

## 5. Криптография для лицензирования

### Android

#### Шаг 1: Добавить зависимости
```kotlin
// core/license/build.gradle.kts

val androidMain by getting {
    dependencies {
        implementation("androidx.security:security-crypto:1.1.0-alpha06")
        implementation("org.bouncycastle:bcprov-jdk15on:1.70")
        implementation("org.bouncycastle:bcpkix-jdk15on:1.70")
    }
}
```

#### Шаг 2: Обновить LicenseManager.android.kt
```kotlin
// core/license/src/androidMain/kotlin/.../LicenseManager.android.kt

import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import java.security.Security
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

class LicenseManagerAndroid : LicenseManager {
    
    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }
    
    private val encryptedPrefs: SharedPreferences by lazy {
        EncryptedSharedPreferences.create(
            context,
            "license_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
    
    override suspend fun validateLicense(key: String): LicenseValidationResult {
        // Реализация валидации с использованием криптографии
        // ...
    }
    
    private fun encryptLicenseData(data: String): String {
        // Шифрование данных лицензии
        // ...
    }
}
```

### iOS

#### Шаг 1: Использовать Security framework через cinterop
```kotlin
// Создать .def файл
// core/license/ios/security.def

language = Objective-C
headers = Security/Security.h
headerFilter = Security/**
package = platform.security
```

#### Шаг 2: Использование в LicenseManager.ios.kt
```kotlin
// core/license/src/iosMain/kotlin/.../LicenseManager.ios.kt

import platform.Security.*
import platform.Foundation.*

class LicenseManagerIOS : LicenseManager {
    
    override suspend fun validateLicense(key: String): LicenseValidationResult {
        // Использовать Security framework для валидации
        // ...
    }
}
```

---

## 6. TensorFlow Lite для AI аналитики

### Android

#### Шаг 1: Добавить зависимости
```kotlin
// android/build.gradle.kts (или в отдельном модуле analytics)

dependencies {
    implementation("org.tensorflow:tensorflow-lite:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-gpu:2.14.0")
    implementation("org.tensorflow:tensorflow-lite-support:0.4.4")
}
```

#### Шаг 2: Создать Kotlin обертку
```kotlin
// shared/src/commonMain/kotlin/.../ObjectDetector.kt

expect class ObjectDetector {
    suspend fun detect(frame: ByteArray, width: Int, height: Int): List<Detection>
}

// shared/src/androidMain/kotlin/.../ObjectDetector.android.kt

import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage

actual class ObjectDetector {
    private val interpreter: Interpreter by lazy {
        val model = loadModelFile("yolov8.tflite")
        Interpreter(model)
    }
    
    actual suspend fun detect(frame: ByteArray, width: Int, height: Int): List<Detection> {
        // Обработка кадра через TensorFlow Lite
        // ...
    }
}
```

### C++ (нативный код)

#### Шаг 1: Скачать TensorFlow Lite C++ API
```bash
cd native/analytics
wget https://github.com/tensorflow/tensorflow/archive/v2.14.0.tar.gz
# Или использовать pre-built библиотеки
```

#### Шаг 2: Обновить CMakeLists.txt
```cmake
# native/analytics/CMakeLists.txt

# Добавить TensorFlow Lite
set(TFLITE_DIR "${CMAKE_SOURCE_DIR}/third_party/tensorflow")
include_directories(
    ${TFLITE_DIR}/tensorflow/lite/tools/make/downloads/flatbuffers/include
    ${TFLITE_DIR}/tensorflow/lite/c
    ${TFLITE_DIR}/tensorflow/lite/kernels/internal
)

# Создать библиотеку
add_library(analytics SHARED
    src/object_detector.cpp
    include/object_detector.h
)

target_link_libraries(analytics
    # TensorFlow Lite библиотеки
    # ...
)
```

---

## 7. Тестирование библиотеки

### Шаг 1: Добавить MockK и Turbine
```kotlin
// build.gradle.kts (в commonTest)

val commonTest by getting {
    dependencies {
        implementation(kotlin("test-common"))
        implementation(kotlin("test-annotations-common"))
        implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
        
        // Новые зависимости
        implementation("io.mockk:mockk:1.13.8")
        implementation("app.cash.turbine:turbine:1.0.0")
    }
}
```

### Шаг 2: Пример использования
```kotlin
// shared/src/commonTest/kotlin/.../CameraRepositoryImplTest.kt

import io.mockk.*
import app.cash.turbine.test
import kotlinx.coroutines.test.runTest

class CameraRepositoryImplTest {
    
    @Test
    fun `test get cameras flow`() = runTest {
        val repository = CameraRepositoryImpl(mockDatabase)
        
        repository.getCameras().test {
            val cameras = awaitItem()
            assertEquals(0, cameras.size)
            awaitComplete()
        }
    }
}
```

---

## 8. Version Catalog (управление версиями)

### Шаг 1: Создать libs.versions.toml
```toml
# gradle/libs.versions.toml

[versions]
kotlin = "1.9.20"
ktor = "2.3.5"
sqldelight = "2.0.0"
coroutines = "1.7.3"
serialization = "1.6.0"
logging = "3.0.5"
datetime = "0.5.0"

[libraries]
ktor-client-core = { module = "io.ktor:ktor-client-core", version.ref = "ktor" }
ktor-client-android = { module = "io.ktor:ktor-client-android", version.ref = "ktor" }
ktor-client-darwin = { module = "io.ktor:ktor-client-darwin", version.ref = "ktor" }
ktor-serialization-xml = { module = "io.ktor:ktor-serialization-kotlinx-xml", version.ref = "ktor" }

kotlinx-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "coroutines" }
kotlinx-serialization-json = { module = "org.jetbrains.kotlinx:kotlinx-serialization-json", version.ref = "serialization" }

sqldelight-runtime = { module = "app.cash.sqldelight:runtime", version.ref = "sqldelight" }
sqldelight-android-driver = { module = "app.cash.sqldelight:android-driver", version.ref = "sqldelight" }
sqldelight-native-driver = { module = "app.cash.sqldelight:native-driver", version.ref = "sqldelight" }

kotlin-logging = { module = "io.github.microutils:kotlin-logging", version.ref = "logging" }
kotlinx-datetime = { module = "org.jetbrains.kotlinx:kotlinx-datetime", version.ref = "datetime" }

[bundles]
ktor = [
    "ktor-client-core",
    "ktor-client-content-negotiation",
    "ktor-serialization-kotlinx-json",
    "ktor-client-logging",
    "ktor-client-websockets"
]

[plugins]
kotlin-multiplatform = { id = "org.jetbrains.kotlin.multiplatform", version.ref = "kotlin" }
kotlin-serialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
sqldelight = { id = "app.cash.sqldelight", version.ref = "sqldelight" }
```

### Шаг 2: Использовать в build.gradle.kts
```kotlin
// build.gradle.kts

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.serialization)
    // ...
}

dependencies {
    implementation(libs.bundles.ktor)
    implementation(libs.kotlinx.coroutines.core)
    // ...
}
```

---

## 🔧 Проверка интеграции

### После каждой интеграции выполнить:

1. **Сборка проекта**
```bash
./gradlew clean build
```

2. **Запуск тестов**
```bash
./gradlew test
```

3. **Проверка линтера**
```bash
./gradlew detekt
```

4. **Проверка размера APK/IPA** (для мобильных платформ)
```bash
# Android
./gradlew :android:assembleDebug
# Проверить размер APK
```

---

## ⚠️ Частые проблемы и решения

### Проблема 1: Конфликты версий
**Решение:** Использовать Version Catalog и BOM где возможно

### Проблема 2: Нативные библиотеки не линкуются
**Решение:** Проверить CMakeLists.txt, пути к библиотекам, архитектуру

### Проблема 3: Большой размер приложения
**Решение:** 
- Использовать App Bundle для Android
- Включить ProGuard/R8
- Использовать динамические библиотеки где возможно

### Проблема 4: Проблемы с кодировкой (Windows)
**Решение:** Убедиться что все файлы в UTF-8, настроить Gradle:
```properties
# gradle.properties
org.gradle.jvmargs=-Dfile.encoding=UTF-8
```

---

## 📚 Дополнительные ресурсы

- [Ktor Documentation](https://ktor.io/docs/)
- [FFmpeg Documentation](https://ffmpeg.org/documentation.html)
- [OpenCV Documentation](https://docs.opencv.org/)
- [TensorFlow Lite Guide](https://www.tensorflow.org/lite/guide)
- [Live555 Documentation](http://www.live555.com/liveMedia/)

---

**Следующие шаги:** После интеграции критических библиотек перейти к реализации UI компонентов и AI аналитики.



