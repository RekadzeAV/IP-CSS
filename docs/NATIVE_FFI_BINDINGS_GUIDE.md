# Руководство по FFI биндингам для Kotlin/Native

## Содержание

1. [Введение](#введение)
2. [Архитектура FFI биндингов](#архитектура-ffi-биндингов)
3. [Создание новых биндингов](#создание-новых-биндингов)
4. [Генерация биндингов](#генерация-биндингов)
5. [Тестирование биндингов](#тестирование-биндингов)
6. [Отладка FFI проблем](#отладка-ffi-проблем)
7. [Поддержка нескольких платформ](#поддержка-нескольких-платформ)
8. [Примеры](#примеры)

---

## Введение

FFI (Foreign Function Interface) биндинги позволяют Kotlin/Native коду взаимодействовать с нативными C/C++ библиотеками. В этом проекте мы используем cinterop для генерации биндингов между Kotlin и нативной библиотекой `video_processing`.

### Зачем нужны FFI биндинги?

- **Производительность**: Прямой вызов нативного кода без накладных расходов JVM
- **Доступ к платформенным API**: Использование специфичных API операционной системы
- **Интеграция с существующим кодом**: Переиспользование существующих C/C++ библиотек
- **Кроссплатформенность**: Единый Kotlin API для разных платформ

---

## Архитектура FFI биндингов

```
┌─────────────────────────────────────────────────────────────┐
│                    Kotlin/Native Code                       │
│  (shared/src/nativeMain/kotlin/.../rtsp/NativeRtspClient.kt) │
└──────────────────────┬──────────────────────────────────────┘
                       │ cinterop generated bindings
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              cinterop Generated Bindings                    │
│  (shared/src/nativeMain/kotlin/generated/...)               │
│  - rtsp_clientBindings.kt                                   │
│  - video_decoderBindings.kt                                 │
│  - audio_decoderBindings.kt                                 │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              C Header Files                                 │
│  (native/video-processing/include/...)                      │
│  - rtsp_client.h                                            │
│  - video_decoder.h                                          │
│  - audio_decoder.h                                          │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              C++ Implementation                             │
│  (native/video-processing/src/...)                          │
│  - rtsp_client.cpp                                          │
│  - video_decoder.cpp                                        │
│  - audio_decoder.cpp                                        │
└─────────────────────────────────────────────────────────────┘
```

---

## Создание новых биндингов

### Шаг 1: Определить C интерфейс

Создайте заголовочный файл в `native/video-processing/include/`:

```c
// include/my_feature.h
#ifndef MY_FEATURE_H
#define MY_FEATURE_H

#include <stdint.h>
#include <stdbool.h>

// Оpaque pointer
typedef struct MyFeature MyFeature;

// Callback типы
typedef void (*MyFeatureCallback)(int result, const char* message, void* userData);

// Создание
MyFeature* my_feature_create(int param1, float param2);

// Обработка
bool my_feature_process(MyFeature* feature, const uint8_t* data, int size);

// Получение результата
int my_feature_get_result(MyFeature* feature);

// Уничтожение
void my_feature_destroy(MyFeature* feature);

// Установка callback
void my_feature_set_callback(MyFeature* feature, MyFeatureCallback callback, void* userData);

#endif // MY_FEATURE_H
```

### Шаг 2: Реализовать C++ функции

```cpp
// src/my_feature.cpp
#include "my_feature.h"
#include <cstdlib>
#include <cstring>

struct MyFeature {
    int param1;
    float param2;
    MyFeatureCallback callback;
    void* userData;
    int result;
};

MyFeature* my_feature_create(int param1, float param2) {
    MyFeature* feature = (MyFeature*)malloc(sizeof(MyFeature));
    feature->param1 = param1;
    feature->param2 = param2;
    feature->callback = nullptr;
    feature->userData = nullptr;
    feature->result = 0;
    return feature;
}

bool my_feature_process(MyFeature* feature, const uint8_t* data, int size) {
    if (!feature || !data || size <= 0) return false;
    
    // Обработка данных
    feature->result = size * feature->param1;
    
    if (feature->callback) {
        feature->callback(feature->result, "Processing complete", feature->userData);
    }
    
    return true;
}

int my_feature_get_result(MyFeature* feature) {
    return feature ? feature->result : -1;
}

void my_feature_destroy(MyFeature* feature) {
    if (feature) {
        free(feature);
    }
}

void my_feature_set_callback(MyFeature* feature, MyFeatureCallback callback, void* userData) {
    if (feature) {
        feature->callback = callback;
        feature->userData = userData;
    }
}
```

### Шаг 3: Создать .def файл для cinterop

Создайте файл в `bindings/my-feature.def`:

```
package = com.company.ipcamera.core.network.native

compilerOpts = -Inative/video-processing/include -D_ENABLE_MY_FEATURE

stubsDir = shared/src/nativeMain/kotlin/generated

include = "my_feature.h"
```

### Шаг 4: Сгенерировать биндинги

```bash
# Автоматически через скрипт
.\scripts\generate-native-bindings.ps1

# Вручную
cinterop -def bindings/my-feature.def -output shared/src/nativeMain/kotlin/generated/MyFeatureBindings.kt
```

### Шаг 5: Создать Kotlin обертку

```kotlin
// shared/src/nativeMain/kotlin/com/company/ipcamera/core/network/rtsp/NativeMyFeature.kt
package com.company.ipcamera.core.network.rtsp

import com.company.ipcamera.core.network.native.*
import kotlinx.cinterop.*

/**
 * Kotlin/Native обертка над нативной функцией MyFeature
 */
actual class NativeMyFeature {
    private var handle: Long = 0L
    private val stableRef: StableRef<(Int, String?) -> Unit>? = null
    
    actual fun create(param1: Int, param2: Float): Boolean {
        memScoped {
            val ptr = my_feature_create(param1, param2)
            if (ptr == null) return false
            handle = ptr.rawValue.toLong()
            return true
        }
    }
    
    actual fun process(data: ByteArray): Boolean {
        if (handle == 0L) return false
        
        return data.usePinned { pinned ->
            my_feature_process(
                interpretCPointer<MyFeature>(NativePtr(handle)),
                pinned.addressOf(0),
                data.size
            )
        }
    }
    
    actual fun getResult(): Int {
        if (handle == 0L) return -1
        return my_feature_get_result(interpretCPointer<MyFeature>(NativePtr(handle)))
    }
    
    actual fun destroy() {
        if (handle != 0L) {
            val ptr = interpretCPointer<MyFeature>(NativePtr(handle))
            my_feature_destroy(ptr)
            handle = 0L
            stableRef?.dispose()
        }
    }
    
    actual fun setCallback(callback: (Int, String?) -> Unit) {
        // Dispose старого callback если есть
        stableRef?.dispose()
        
        // Создаем новый StableRef
        val newRef = StableRef.create(callback)
        
        memScoped {
            val cCallback = staticCFunction<Int, CPointer<ByteVar>?, COpaquePointer?, Unit> { result, message, userData ->
                if (userData != null) {
                    try {
                        val kotlinCallback = userData.asStableRef<(Int, String?) -> Unit>()
                        val kotlinMessage = message?.toKString()
                        kotlinCallback.get().invoke(result, kotlinMessage)
                    } catch (e: Exception) {
                        // Обработка ошибки в callback
                    }
                }
            }
            
            my_feature_set_callback(
                interpretCPointer<MyFeature>(NativePtr(handle)),
                cCallback,
                newRef.asCPointer()
            )
        }
    }
}
```

---

## Генерация биндингов

### Автоматическая генерация

```powershell
# Windows
.\scripts\generate-native-bindings.ps1

# Linux/macOS
./scripts/generate-native-bindings.sh
```

### Ручная генерация

```bash
# Linux/macOS
cinterop -def bindings/rtsp.def -output shared/src/nativeMain/kotlin/generated/RtspClientBindings.kt \
    -compiler-opts "-Inative/video-processing/include -D_LINUX"

# Windows (MinGW)
cinterop -def bindings/rtsp.def -output shared/src/nativeMain/kotlin/generated/RtspClientBindings.kt ^
    -compiler-opts "-Inative/video-processing/include -D_WIN32"
```

### Параметры .def файла

```
# Пакет для generated bindings
package = com.company.ipcamera.core.network.native

# Опции компилятора C
compilerOpts = -Inative/video-processing/include -D_ENABLE_FFMPEG

# Директория для stub файлов
stubsDir = shared/src/nativeMain/kotlin/generated

# Заголовки для inclusion
include = "rtsp_client.h"
include = "video_decoder.h"

# Опции линковщика (если нужны)
linkerOpts = -Lnative/video-processing/lib -lvideo_processing
```

---

## Тестирование биндингов

### Проверка совместимости типов

```kotlin
// shared/src/nativeTest/kotlin/com/company/ipcamera/core/network/BindingCompatibilityTest.kt
package com.company.ipcamera.core.network

import kotlin.test.*
import kotlinx.cinterop.*

class BindingCompatibilityTest {
    
    @Test
    fun verifyStructSize() {
        // Проверка размера структур
        val expectedSize = 64 // Из C кода
        val actualSize = sizeof<MyFeatureStruct>()
        assertEquals(expectedSize, actualSize.toInt(), "Struct size mismatch")
    }
    
    @Test
    fun verifyStructOffsets() {
        // Проверка смещений полей
        memScoped {
            val ptr = alloc<MyFeatureStruct>()
            
            val field1Offset = ptr.field1.offset.toInt()
            val field2Offset = ptr.field2.offset.toInt()
            
            // Убедитесь, что смещения соответствуют ожидаемым
            assertEquals(0, field1Offset, "field1 offset mismatch")
            assertEquals(8, field2Offset, "field2 offset mismatch")
        }
    }
    
    @Test
    fun testCreateDestroy() {
        // Тест базовых операций create/destroy
        val handle = my_feature_create(100, 3.14f)
        assertNotNull(handle)
        
        my_feature_destroy(handle)
    }
    
    @Test
    fun testProcessFunction() {
        // Тест обработки данных
        val handle = my_feature_create(100, 3.14f)
        val testData = ByteArray(1024) { 0xFF.toByte() }
        
        val result = testData.usePinned {
            my_feature_process(handle, it.addressOf(0), testData.size)
        }
        
        assertTrue(result, "Process function should succeed")
        
        my_feature_destroy(handle)
    }
}
```

### Интеграционные тесты

```kotlin
// shared/src/nativeTest/kotlin/com/company/ipcamera/core/network/IntegrationTest.kt
package com.company.ipcamera.core.network

import kotlin.test.*
import kotlinx.coroutines.*
import kotlinx.cinterop.*

class RtspIntegrationTest {
    
    @Test
    fun testRtspConnection() = runBlocking {
        val client = rtsp_client_create()
        assertNotNull(client)
        
        try {
            val connected = rtsp_client_connect(
                client,
                "rtsp://192.168.1.100:554/stream",
                "admin",
                "password",
                5000
            )
            
            assertTrue(connected, "RTSP connection should succeed")
            
            val status = rtsp_client_get_status(client)
            assertEquals(RTSPStatus.RTSP_STATUS_CONNECTED, status)
            
        } finally {
            rtsp_client_destroy(client)
        }
    }
}
```

---

## Отладка FFI проблем

### Общие проблемы и решения

#### 1. Segfault при вызове функции

**Причины:**
- NULL указатель
- Неправильные типы параметров
- Неправильное выравнивание структур

**Решение:**
```kotlin
// Проверяем NULL перед использованием
val ptr = my_function_create()
if (ptr == null) {
    throw IllegalStateException("Failed to create handle")
}

// Используем safe calls
ptr?.let { safePtr ->
    my_function_process(safePtr, data)
}
```

#### 2. Утечки памяти

**Причины:**
- Забыли вызвать destroy
- Не disposed StableRef для callbacks

**Решение:**
```kotlin
class MyFeature {
    private var handle: Long = 0L
    private var callbackRef: StableRef<*>? = null
    
    fun destroy() {
        if (handle != 0L) {
            val ptr = interpretCPointer<MyFeatureStruct>(NativePtr(handle))
            my_feature_destroy(ptr)
            handle = 0L
        }
        callbackRef?.dispose() // Важно!
    }
    
    fun close() {
        destroy()
    }
}

// Используйте try-with-resources
fun useFeature() {
    val feature = NativeMyFeature()
    try {
        feature.create(100, 3.14f)
        feature.process(data)
    } finally {
        feature.close()
    }
}
```

#### 3. Неправильные значения в структурах

**Причины:**
- Разный размер типов между C и Kotlin
- Endianness проблемы
- Неправильное определение типов

**Решение:**
```kotlin
// Используйте правильные типы
// C: int32_t -> Kotlin: Int
// C: uint8_t* -> Kotlin: CPointer<UInt8Var>
// C: double -> Kotlin: Double

// Проверяйте размеры
assertEquals(4, sizeof<Int>().toInt())
assertEquals(8, sizeof<Double>().toInt())

// Для строк используйте toKString() и memScoped
memScoped {
    val str = "Hello"
    val cString = str.cstr
    my_function_accepts_string(cString.ptr)
}
```

### Инструменты отладки

```bash
# Проверка символов в библиотеке
nm -D libvideo_processing.so | grep my_feature

# Dump символов
objdump -t libvideo_processing.so | grep my_feature

# Valgrind для поиска утечек
valgrind --leak-check=full --show-leak-kinds=all ./my_app

# AddressSanitizer
clang -fsanitize=address -g my_app.c -o my_app
./my_app

# GDB отладка
gdb ./my_app
(gdb) break my_feature_process
(gdb) run
(gdb) print *feature
```

---

## Поддержка нескольких платформ

### Windows (MinGW)

```bash
cinterop -def bindings/rtsp.def \
    -output shared/src/nativeMain/kotlin/generated/RtspClientBindings.kt \
    -compiler-opts "-Inative/video-processing/include -D_WIN32 -DUNICODE" \
    -target "windows_x86_64"
```

### Linux

```bash
cinterop -def bindings/rtsp.def \
    -output shared/src/nativeMain/kotlin/generated/RtspClientBindings.kt \
    -compiler-opts "-Inative/video-processing/include -D_LINUX -D_GNU_SOURCE" \
    -target "linux_x64"
```

### macOS

```bash
cinterop -def bindings/rtsp.def \
    -output shared/src/nativeMain/kotlin/generated/RtspClientBindings.kt \
    -compiler-opts "-Inative/video-processing/include -D_MACOS" \
    -target "macos_x64"

# Для Apple Silicon
cinterop -def bindings/rtsp.def \
    -output shared/src/nativeMain/kotlin/generated/RtspClientBindings.kt \
    -compiler-opts "-Inative/video-processing/include -D_MACOS" \
    -target "macos_arm64"
```

### iOS

```bash
cinterop -def bindings/rtsp.def \
    -output shared/src/iosMain/kotlin/generated/RtspClientBindings.kt \
    -compiler-opts "-Inative/video-processing/include -D_IOS" \
    -target "ios_arm64"
```

---

## Примеры

### Пример 1: RTSP Client

```kotlin
// C header: rtsp_client.h
typedef struct RTSPClient RTSPClient;
RTSPClient* rtsp_client_create();
bool rtsp_client_connect(RTSPClient* client, const char* url, ...);
void rtsp_client_destroy(RTSPClient* client);

// Kotlin wrapper
class RtspClient {
    private var handle: Long = 0L
    
    fun connect(url: String): Boolean {
        val ptr = rtsp_client_create()
        if (ptr == null) return false
        
        handle = ptr.rawValue.toLong()
        
        return url.useCstring { urlCstr ->
            rtsp_client_connect(ptr, urlCstr, null, null, 5000)
        }
    }
    
    fun destroy() {
        if (handle != 0L) {
            val ptr = interpretCpointer<RTSPClient>(NativePtr(handle))
            rtsp_client_destroy(ptr)
            handle = 0L
        }
    }
}
```

### Пример 2: Video Decoder с callback

```kotlin
// C header: video_decoder.h
typedef void (*VideoFrameCallback)(VideoFrame* frame, void* userData);
void video_decoder_set_callback(VideoDecoder* decoder, VideoFrameCallback callback, void* userData);

// Kotlin wrapper
class VideoDecoder {
    private var handle: Long = 0L
    private var callbackRef: StableRef<((VideoFrame) -> Unit)?>? = null
    
    fun setCallback(callback: (VideoFrame) -> Unit) {
        callbackRef?.dispose()
        
        val stableRef = StableRef.create(callback)
        callbackRef = stableRef
        
        memScoped {
            val cCallback = staticCFunction { framePtr, userData ->
                if (userData != null && framePtr != null) {
                    val kotlinCallback = userData.asStableRef<(VideoFrame) -> Unit>()
                    val frame = convertVideoFrame(framePtr)
                    kotlinCallback.get().invoke(frame)
                    video_frame_release(framePtr)
                }
            }
            
            video_decoder_set_callback(
                interpretCPointer<VideoDecoder>(NativePtr(handle)),
                cCallback,
                stableRef.asCPointer()
            )
        }
    }
}
```

---

## Ссылки

- [Kotlin/Native cinterop documentation](https://kotlinlang.org/docs/native-c-interop.html)
- [Kotlin/Native Foreign Function Interface](https://kotlinlang.org/api/latest/kotlin.native.utils/kotlinx.cinterop/)
- [C type to Kotlin type mapping](https://kotlinlang.org/docs/native-c-interop.html#ffi-unknowns)

---

**Автор:** NLP-Core-Team  
**Версия:** 1.0  
**Последнее обновление:** 2025-01-15
