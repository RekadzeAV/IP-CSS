# Интеграция Live555 через cinterop

## Обзор

Этот документ описывает процесс интеграции Live555 библиотеки в Kotlin Multiplatform проект через cinterop.

## Предварительные требования

1. **Установленные инструменты:**
   - Kotlin/Native Compiler (Kotlin 1.9+)
   - C/C++ Compiler (gcc/g++ для Linux, clang для macOS, MSVC для Windows)
   - CMake (опционально)

2. **Скомпилированные библиотеки Live555:**
   - Windows: `native/live555/build/x64/Release/lib/`
   - Linux: `native/live555/build/x64/Release/lib/`
   - macOS: `native/live555/build/universal/Release/lib/`

3. **Заголовочные файлы:**
   - `native/live555/wrapper/live555_wrapper.h`

## Шаг 1: Настройка build.gradle.kts

### Для Windows (mingwX64)

```kotlin
kotlin {
    mingwX64("windows") {
        compilations.main.cinterops {
            create("live555") {
                defFile(project.file("native/live555/live555.def"))
                header(project.file("native/live555/wrapper/live555_wrapper.h"))
                includeDirs(
                    project.file("native/live555/build/x64/Release/include"),
                    project.file("native/live555/build/x64/Release/include/live")
                )
                
                // Путь к библиотекам
                compilerOpts.add("-L${project.file("native/live555/build/x64/Release/lib").absolutePath}")
                linkerOpts.add("-L${project.file("native/live555/build/x64/Release/lib").absolutePath}")
                linkerOpts.add("-llibLiveMedia")
                linkerOpts.add("-llibgroupsock")
                linkerOpts.add("-llibbasicUsageEnvironment")
                linkerOpts.add("-llibUsageEnvironment")
            }
        }
        
        // Зависимости
        compilations.main.cinterops["live555"]
    }
}
```

### Для Linux (linuxX64)

```kotlin
kotlin {
    linuxX64("linux") {
        compilations.main.cinterops {
            create("live555") {
                defFile(project.file("native/live555/live555.def"))
                header(project.file("native/live555/wrapper/live555_wrapper.h"))
                includeDirs(
                    project.file("native/live555/build/x64/Release/include"),
                    project.file("native/live555/build/x64/Release/include/live")
                )
                
                // Путь к библиотекам
                compilerOpts.add("-L${project.file("native/live555/build/x64/Release/lib").absolutePath}")
                linkerOpts.add("-L${project.file("native/live555/build/x64/Release/lib").absolutePath}")
                linkerOpts.add("-lLiveMedia")
                linkerOpts.add("-lgroupsock")
                linkerOpts.add("-lbasicUsageEnvironment")
                linkerOpts.add("-lUsageEnvironment")
                linkerOpts.add("-lpthread")
                linkerOpts.add("-ldl")
            }
        }
    }
}
```

### Для macOS (macOS)

```kotlin
kotlin {
    macosX64("macos") {
        compilations.main.cinterops {
            create("live555") {
                defFile(project.file("native/live555/live555.def"))
                header(project.file("native/live555/wrapper/live555_wrapper.h"))
                includeDirs(
                    project.file("native/live555/build/universal/Release/include"),
                    project.file("native/live555/build/universal/Release/include/live")
                )
                
                // Путь к библиотекам
                compilerOpts.add("-L${project.file("native/live555/build/universal/Release/lib").absolutePath}")
                linkerOpts.add("-L${project.file("native/live555/build/universal/Release/lib").absolutePath}")
                linkerOpts.add("-lLiveMedia")
                linkerOpts.add("-lgroupsock")
                linkerOpts.add("-lbasicUsageEnvironment")
                linkerOpts.add("-lUsageEnvironment")
                linkerOpts.add("-framework", "CoreFoundation")
            }
        }
    }
    
    macosArm64("macosArm64") {
        // Аналогично macosX64
    }
}
```

### Для iOS (iosArm64, iosSimulatorArm64, iosX64)

```kotlin
kotlin {
    iosArm64("ios") {
        compilations.main.cinterops {
            create("live555") {
                defFile(project.file("native/live555/live555.def"))
                header(project.file("native/live555/wrapper/live555_wrapper.h"))
                includeDirs(
                    project.file("native/live555/build/universal/Release/include"),
                    project.file("native/live555/build/universal/Release/include/live")
                )
                
                // Путь к библиотекам
                compilerOpts.add("-L${project.file("native/live555/build/universal/Release/lib").absolutePath}")
                linkerOpts.add("-L${project.file("native/live555/build/universal/Release/lib").absolutePath}")
                linkerOpts.add("-lLiveMedia")
                linkerOpts.add("-lgroupsock")
                linkerOpts.add("-lbasicUsageEnvironment")
                linkerOpts.add("-lUsageEnvironment")
            }
        }
    }
    
    iosSimulatorArm64("iosSimulator") {
        // Аналогично iosArm64
    }
    
    iosX64("iosSimulatorX64") {
        // Аналогично iosArm64
    }
}
```

## Шаг 2: Создание .def файла

### Для Windows (live555.def)

```def
LIBRARY live555
EXPORTS
    rtsp_client_create
    rtsp_client_destroy
    rtsp_client_connect
    rtsp_client_disconnect
    rtsp_client_play
    rtsp_client_pause
    rtsp_client_get_status
    rtsp_client_get_video_info
    rtsp_client_get_audio_info
    rtsp_client_get_video_frame
    rtsp_client_get_audio_frame
    rtsp_client_free_frame
    rtsp_client_get_last_error
    rtsp_is_supported
    rtsp_get_version
```

### Для Linux/macOS (live555.def)

```def
EXPORTS
    rtsp_client_create
    rtsp_client_destroy
    rtsp_client_connect
    rtsp_client_disconnect
    rtsp_client_play
    rtsp_client_pause
    rtsp_client_get_status
    rtsp_client_get_video_info
    rtsp_client_get_audio_info
    rtsp_client_get_video_frame
    rtsp_client_get_audio_frame
    rtsp_client_free_frame
    rtsp_client_get_last_error
    rtsp_is_supported
    rtsp_get_version
```

## Шаг 3: Генерация cinterop биндингов

### Через Gradle

```bash
./gradlew :core:network:generateKotlinNativeBindings
```

### Вручную

```bash
# Windows
cinterop -def native/live555/live555.def \
  -header native/live555/wrapper/live555_wrapper.h \
  -includePath "native/live555/build/x64/Release/include" \
  -language C \
  -target x64-windows \
  -output native/live555/bindings/windows/live555.kt

# Linux
cinterop -def native/live555/live555.def \
  -header native/live555/wrapper/live555_wrapper.h \
  -includePath "native/live555/build/x64/Release/include" \
  -language C \
  -target x64-linux \
  -output native/live555/bindings/linux/live555.kt

# macOS
cinterop -def native/live555/live555.def \
  -header native/live555/wrapper/live555_wrapper.h \
  -includePath "native/live555/build/universal/Release/include" \
  -language C \
  -target x64-macos \
  -output native/live555/bindings/macos/live555.kt
```

## Шаг 4: Проверка сгенерированных биндингов

Сгенерированные биндинги должны содержать следующие типы:

```kotlin
// Ожидаемый вывод
external interface live555 {
    fun rtsp_client_create(config: CPointer<out CPointed>?): NativePtr
    fun rtsp_client_destroy(client: NativePtr)
    fun rtsp_client_connect(client: NativePtr): Byte
    // ... остальные функции
}
```

## Шаг 5: Использование биндингов

### Пример использования

```kotlin
import kotlinx.cinterop.*

fun createClient(url: String): NativePtr {
    memScoped {
        val config = alloc<RtspClientConfigVar>()
        config.url = url.cstr.ptr
        config.timeoutMs = 5000
        config.enableVideo = 1
        config.enableAudio = 1
        
        return rtsp_client_create(config.ptr)
    }
}
```

## Шаг 6: Сборка проекта

```bash
# Сборка для всех платформ
./gradlew :core:network:assemble

# Сборка для конкретной платформы
./gradlew :core:network:compileKotlinWindowsX64
./gradlew :core:network:compileKotlinLinuxX64
./gradlew :core:network:compileKotlinMacosX64
./gradlew :core:network:compileKotlinIosArm64
```

## Troubleshooting

### Ошибка: "Cannot find header file"

**Проблема:** cinterop не может найти заголовочный файл

**Решение:**
1. Проверьте путь к заголовку
2. Убедитесь, что файл существует
3. Проверьте includeDirs

```kotlin
includeDirs(
    project.file("native/live555/wrapper"),
    project.file("native/live555/build/x64/Release/include")
)
```

### Ошибка: "Cannot find library"

**Проблема:** Ссылочный редактор не может найти библиотеку

**Решение:**
1. Проверьте путь к библиотеке
2. Убедитесь, что библиотека существует
3. Проверьте имя библиотеки (lib*.a или *.lib)

```kotlin
linkerOpts.add("-L${project.file("native/live555/build/x64/Release/lib").absolutePath}")
linkerOpts.add("-lLiveMedia") // без lib-префикса и расширения
```

### Ошибка: "Symbol not found"

**Проблема:** Символ не экспортируется из библиотеки

**Решение:**
1. Проверьте .def файл
2. Убедитесь, что функция экспортируется
3. Проверьте имя функции (case-sensitive)

```def
EXPORTS
    rtsp_client_create  # Должно точно совпадать
```

### Ошибка: "Type mismatch"

**Проблема:** Несовпадение типов между Kotlin и C

**Решение:**
1. Используйте правильные типы в Kotlin:
   - `int` → `Int`
   - `bool` → `Byte` (0/1)
   - `char*` → `CPointer<ByteVar>` или `String`
   - `void*` → `NativePtr`

2. Используйте конвертеры:
   ```kotlin
   val str = "hello".cstr.ptr
   val result = function(str)
   ```

## Дополнительные ресурсы

- [Kotlin/Native cinterop документация](https://kotlinlang.org/docs/native-cinterop.html)
- [Live555 documentation](http://www.live555.com/liveMedia/publications/)
- [Kotlin/Native platform targets](https://kotlinlang.org/docs/native-target-support.html)

## Чек-лист интеграции

- [ ] Библиотеки Live555 скомпилированы для всех платформ
- [ ] Заголовочные файлы готовы
- [ ] .def файл создан
- [ ] build.gradle.kts настроен
- [ ] cinterop биндинги сгенерированы
- [ ] Платформенные реализации написаны
- [ ] Тесты написаны
- [ ] Сборка проходит без ошибок
- [ ] Тесты проходят успешно

---

**Версия:** 1.0  
**Дата:** 2025-01-15  
**Автор:** NLP-Core-Team
