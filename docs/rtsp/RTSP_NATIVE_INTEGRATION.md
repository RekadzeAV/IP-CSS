# RTSP Клиент — Интеграция с Нативной Библиотекой

**Дата создания:** 26 January 2026
**Версия:** 1.0
**Статус:** ✅ Завершено

## Обзор

RTSP клиент полностью интегрирован с нативной C++ библиотекой через FFI (Foreign Function Interface) для всех поддерживаемых платформ. Интеграция использует Kotlin/Native cinterop для нативных платформ и JNI для Android/JVM.

## Архитектура интеграции

```
┌─────────────────────────────────┐
│      RtspClient (Kotlin)       │
│   (High-level API для клиентов) │
└──────────────┬──────────────────┘
               │
               ▼
┌─────────────────────────────────┐
│   NativeRtspClient (expect)     │
│   (Платформо-независимый API)   │
└──────────────┬──────────────────┘
               │
       ┌───────┴───────┐
       │               │
       ▼               ▼
┌─────────────┐  ┌─────────────┐
│  Android    │  │    iOS      │
│  (JNI)      │  │  (cinterop) │
└─────────────┘  └─────────────┘
       │               │
       └───────┬───────┘
               ▼
┌─────────────────────────────────┐
│   rtsp_client (C++ библиотека)  │
│   (Нативная реализация)         │
└─────────────────────────────────┘
```

## Структура файлов

### Нативная библиотека

- **Заголовочный файл:** `native/video-processing/include/rtsp_client.h`
- **Реализация:** `native/video-processing/src/rtsp_client.cpp`
- **JNI обертка (Android):** `native/video-processing/src/jni/rtsp_client_jni.cpp`

### Kotlin обертки

- **Expect класс:** `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/rtsp/NativeRtspClient.kt`
- **Native реализация (Linux/macOS/iOS):** `core/network/src/nativeMain/kotlin/com/company/ipcamera/core/network/rtsp/NativeRtspClient.native.kt`
- **iOS реализация:** `core/network/src/iosMain/kotlin/com/company/ipcamera/core/network/rtsp/NativeRtspClient.ios.kt`
- **Android реализация:** `core/network/src/androidMain/kotlin/com/company/ipcamera/core/network/rtsp/NativeRtspClient.android.kt`
- **JVM реализация:** `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/rtsp/NativeRtspClient.jvm.kt`

### Конфигурация cinterop

- **.def файл:** `core/network/src/nativeInterop/cinterop/rtsp_client.def`
- **Build конфигурация:** `core/network/build.gradle.kts`

## Поддерживаемые платформы

### Нативные платформы (через cinterop)

- ✅ **Linux x64** (`linuxX64`)
- ✅ **macOS x64** (`macosX64`)
- ✅ **macOS ARM64** (`macosArm64`)
- ✅ **iOS x64** (`iosX64`)
- ✅ **iOS ARM64** (`iosArm64`)
- ✅ **iOS Simulator ARM64** (`iosSimulatorArm64`)
- ✅ **Windows x64** (`mingwX64`)
- ✅ **Android Native ARM32** (`androidNativeArm32`)
- ✅ **Android Native ARM64** (`androidNativeArm64`)
- ✅ **Android Native x86** (`androidNativeX86`)
- ✅ **Android Native x64** (`androidNativeX64`)

### JVM платформы (через JNI)

- ✅ **Android** (через JNI)
- ✅ **Desktop JVM** (через JNI)

## Настройка cinterop

### .def файл

Файл `core/network/src/nativeInterop/cinterop/rtsp_client.def` содержит:

1. **Определения типов:**
   - Структуры: `RTSPClient`, `RTSPStream`, `RTSPFrame`
   - Enum типы: `RTSPStreamType`, `RTSPStatus`
   - Callback типы: `RTSPFrameCallback`, `RTSPStatusCallback`
   - Структура параметров: `RTSPReconnectParams`

2. **Определения функций:**
   - Создание/уничтожение клиента
   - Подключение/отключение
   - Управление воспроизведением
   - Получение информации о потоках
   - Установка callbacks

### Build конфигурация

В `core/network/build.gradle.kts` настроены cinterop для всех платформ:

```kotlin
linuxX64("native") {
    compilations.getByName("main") {
        cinterops {
            val rtspClient by creating {
                defFile(project.file("src/nativeInterop/cinterop/rtsp_client.def"))
                packageName("com.company.ipcamera.core.network.rtsp")
                compilerOpts("-I${project.rootDir}/../native/video-processing/include")
                includeDirs("${project.rootDir}/../native/video-processing/include")
                linkerOpts("-L${project.rootDir}/../native/video-processing/lib/linux/x64 -lvideo_processing")
            }
        }
    }
}
```

## Использование

### Создание клиента

```kotlin
val config = RtspClientConfig(
    url = "rtsp://192.168.1.100:554/stream1",
    username = "admin",
    password = "password",
    timeoutMillis = 10000
)

val client = RtspClient(config)
```

### Подключение

```kotlin
// Подключение к серверу
client.connect()

// Ожидание подключения
client.getStatus().collect { status ->
    when (status) {
        RtspClientStatus.CONNECTED -> {
            println("Connected successfully")
        }
        RtspClientStatus.ERROR -> {
            println("Connection failed")
        }
        else -> {}
    }
}
```

### Воспроизведение

```kotlin
// Начать воспроизведение
client.play()

// Получение видеокадров
client.getVideoFrames().collect { frame ->
    // Обработка кадра
    println("Received frame: ${frame.width}x${frame.height}, size: ${frame.data.size}")
}

// Получение аудиокадров
client.getAudioFrames().collect { frame ->
    // Обработка аудиокадра
    println("Received audio frame: size: ${frame.data.size}")
}
```

### Callbacks

```kotlin
// Установка callback для видеокадров
client.setVideoFrameCallback { frame ->
    // Обработка кадра
}

// Установка callback для изменения статуса
client.setStatusCallback { status, message ->
    println("Status changed: $status - $message")
}
```

### Отключение

```kotlin
// Остановка воспроизведения
client.stop()

// Отключение от сервера
client.disconnect()

// Освобождение ресурсов
client.close()
```

## Особенности реализации

### Native платформы (Linux/macOS/iOS)

- Используется **cinterop** для генерации Kotlin биндингов из C заголовочного файла
- Callbacks передаются через `StableRef` для безопасного вызова из нативного кода
- Указатели конвертируются в `Long` для хранения handle'ов

### Android/JVM

- Используется **JNI** для вызова нативных функций
- Библиотека загружается через `System.loadLibrary("video_processing")`
- Callbacks передаются через Java функциональные интерфейсы (`Consumer`, `BiConsumer`)

### Управление памятью

- Кадры освобождаются автоматически после вызова callback'а
- `StableRef` для callbacks освобождаются при уничтожении клиента
- Нативная библиотека управляет памятью для RTSP клиента

## Сборка нативной библиотеки

### Требования

- CMake 3.15+
- Компилятор C++ (GCC, Clang, MSVC)
- FFmpeg библиотеки (опционально, для декодирования видео)

### Сборка для Linux

```bash
cd native/video-processing
mkdir -p build && cd build
cmake ..
make
```

### Сборка для Windows

```powershell
cd native/video-processing
mkdir build
cd build
cmake .. -G "MinGW Makefiles"
mingw32-make
```

### Сборка для macOS

```bash
cd native/video-processing
mkdir -p build && cd build
cmake ..
make
```

### Сборка для Android

```bash
cd native/video-processing
mkdir -p build && cd build
cmake .. \
  -DCMAKE_TOOLCHAIN_FILE=$ANDROID_NDK/build/cmake/android.toolchain.cmake \
  -DANDROID_ABI=arm64-v8a \
  -DANDROID_PLATFORM=android-26
make
```

## Тестирование

### Unit-тесты

```kotlin
@Test
fun testRtspClientCreation() {
    val config = RtspClientConfig(url = "rtsp://test.com/stream")
    val client = RtspClient(config)
    assertNotNull(client)
}
```

### Интеграционные тесты

Для тестирования с реальным RTSP сервером:

```kotlin
@Test
suspend fun testRtspConnection() = runTest {
    val config = RtspClientConfig(
        url = "rtsp://test-server.com/stream",
        username = "test",
        password = "test"
    )
    val client = RtspClient(config)

    client.connect()
    delay(5000) // Ожидание подключения

    assertEquals(RtspClientStatus.CONNECTED, client.getStatus().value)

    client.disconnect()
}
```

## Известные проблемы и ограничения

1. **FFmpeg опционален:** Если FFmpeg не найден при сборке, RTSP клиент будет работать в упрощенном режиме без декодирования видео.

2. **Платформо-специфичные пути:** Пути к библиотекам в `build.gradle.kts` должны соответствовать структуре директорий после сборки нативной библиотеки.

3. **Callbacks в JNI:** Для Android/JVM требуется правильная настройка JNI методов и загрузка библиотеки.

## Следующие шаги

1. ✅ Интеграция с нативной библиотекой завершена
2. ⏳ Тестирование с реальными IP-камерами
3. ⏳ Оптимизация производительности
4. ⏳ Добавление поддержки TCP транспорта для RTSP
5. ⏳ Улучшение обработки ошибок и переподключения

## Связанные документы

- [RTSP_CLIENT.md](../../archive/docs-duplicates-2026-08-08/RTSP_CLIENT.md) - Полная документация RTSP клиента
- [RTSP_CLIENT_INTEGRATION_PROGRESS.md](../implementation/RTSP_CLIENT_INTEGRATION_PROGRESS.md) - Прогресс интеграции
- [RTSP_FFI_INTEGRATION.md](RTSP_FFI_INTEGRATION.md) - Детали FFI интеграции

---

**Последнее обновление:** 26 January 2026
