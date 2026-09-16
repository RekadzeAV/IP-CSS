# Отладка JNI биндингов

Руководство по отладке проблем с JNI в RTSP клиенте.

---

## 📋 Содержание

1. [Типичные проблемы](#1-типичные-проблемы)
2. [Проверка экспорта символов](#2-проверка-экспорта-символов)
3. [Отладка на Windows](#3-отладка-на-windows)
4. [Отладка на Linux](#4-отладка-на-linux)
5. [Отладка на macOS](#5-отладка-на-macos)
6. [Логирование](#6-логирование)

---

## 1. Типичные проблемы

### 1.1 `UnsatisfiedLinkError`

**Ошибка:**
```
java.lang.UnsatisfiedLinkError: no video_processing in java.library.path
```

**Причины:**
- Библиотека не найдена в `java.library.path`
- Несоответствие архитектуры (x64 vs x86)
- Библиотека не скомпилирована

**Решение:**
```kotlin
// Проверка пути
println(System.getProperty("java.library.path"))

// Явная установка пути
System.setProperty("java.library.path", "/path/to/lib")

// Или копирование в resources
// src/main/resources/libvideo_processing.so
```

---

### 1.2 `NoClassDefFoundError`

**Ошибка:**
```
java.lang.NoClassDefFoundError: Could not initialize class RtspClientNative
```

**Причины:**
- Ошибка при инициализации библиотеки
- Несоответствие версий JNI
- Отсутствующие зависимости

**Решение:**
```kotlin
// Проверка загрузки
try {
    System.loadLibrary("video_processing")
    println("Библиотека загружена успешно")
} catch (e: UnsatisfiedLinkError) {
    println("Ошибка загрузки: ${e.message}")
    e.printStackTrace()
}
```

---

### 1.3 `SIGSEGV` (Segmentation Fault)

**Ошибка:**
```
SIGSEGV at 0x00000000 in video_processing.dll
```

**Причины:**
- Access to freed memory
- Неправильная работа с указателями
- Race conditions

**Решение:**
- Используйте Address Sanitizer
- Проверьте lifetime объектов
- Используйте thread-safe структуры

---

## 2. Проверка экспорта символов

### 2.1 Windows

```powershell
# Просмотр экспортируемых символов
dumpbin /exports video_processing.dll

# Поиск RTSP функций
dumpbin /exports video_processing.dll | findstr rtsp_client

# Зависимости
dumpbin /dependents video_processing.dll
```

**Ожидаемые символы:**
```
ordinal  hex      name
     01   00010000 rtsp_client_create
     02   00020000 rtsp_client_destroy
     03   00030000 rtsp_client_connect
     04   00040000 rtsp_client_play
     05   00050000 rtsp_client_stop
     ...
```

### 2.2 Linux

```bash
# Просмотр символов
nm -D libvideo_processing.so | grep rtsp_client

# Зависимости
ldd libvideo_processing.so

# Статические символы
nm libvideo_processing.so | grep rtsp_client
```

**Ожидаемые символы:**
```
0000000000001234 T rtsp_client_create
0000000000001256 T rtsp_client_destroy
0000000000001278 T rtsp_client_connect
...
```

### 2.3 macOS

```bash
# Просмотр символов
nm -g libvideo_processing.dylib | grep rtsp_client

# Зависимости
otool -L libvideo_processing.dylib

# Objective-C классы (если есть)
otool -ov libvideo_processing.dylib | grep RTSP
```

---

## 3. Отладка на Windows

### 3.1 Использование Visual Studio Debugger

```powershell
# Запуск с отладчиком
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
.\build-windows.ps1 Debug

# Запуск приложения с отладкой
$env:JAVA_OPTS = "-agentpath:C:/Program Files/Microsoft Visual Studio/2022/Community/Common7/IDE/Remote Debugger/x64/msvcr120.dll"
java -jar app.jar
```

### 3.2 Включение JNI логирования

```bash
# JVM опции
java -Xcheck:jni -verbose:jni -Djava.library.path=./lib -jar app.jar
```

**Вывод:**
```
[JNI] Loading library video_processing
[JNI] JNI_OnLoad called
[JNI] JNI_OnLoad returned: 1
[JNI] JNIEXPORT void JNICALL Java_...
```

### 3.3 Использование ProcDump

```powershell
# Установка ProcDump
winget install Microsoft.ProcDump

# Мониторинг падений
procdump -ma -e 1 java.exe

# При падении создаст дампы памяти
```

---

## 4. Отладка на Linux

### 4.1 GDB отладка

```bash
# Запуск с GDB
gdb --args java -Djava.library.path=./lib -jar app.jar

# В GDB:
(gdb) run
(gdb) break rtsp_client_connect
(gdb) continue
(gdb) print *client
(gdb) bt  # Backtrace при падении
```

### 4.2 Valgrind

```bash
# Проверка утечек памяти
valgrind --leak-check=full --show-leak-kinds=all \
  java -Djava.library.path=./lib -jar app.jar

# Проверка ошибок доступа
valgrind --tool=memcheck --track-origins=yes \
  java -Djava.library.path=./lib -jar app.jar
```

### 4.3 Address Sanitizer

```bash
# Сборка с ASan
cmake .. -DCMAKE_BUILD_TYPE=Debug -DENABLE_ASAN=ON
cmake --build .

# Запуск
export ASAN_OPTIONS=detect_leaks=1
java -Djava.library.path=./lib -jar app.jar
```

---

## 5. Отладка на macOS

### 5.1 LLDB отладка

```bash
# Запуск с LLDB
lldb -- java -Djava.library.path=./lib -jar app.jar

# В LLDB:
(lldb) run
(lldb) breakpoint set --name rtsp_client_connect
(lldb) continue
(lldb) frame variable
(lldb) bt  # Backtrace
```

### 5.2 DTrace

```bash
# Трассировка системных вызовов
sudo dtrace -n 'pid$target::*:entry { printf("%s %s", execname, probefunc); }' \
  -c "java -Djava.library.path=./lib -jar app.jar"

# Отслеживание ошибок
sudo dtrace -n 'syscall::write*:entry /arg1 != 0/ { trace(copyinstr(arg0)); }'
```

### 5.3 Instruments

```bash
# Запуск Instruments
instruments -t "Time Profiler" \
  /Applications/IntelliJ\ IDEA.app/Contents/jbr/Contents/Home/bin/java \
  -Djava.library.path=./lib -jar app.jar
```

---

## 6. Логирование

### 6.1 Включение JNI логов в JVM

```bash
# Все JNI операции
java -Xcheck:jni -verbose:jni -Djava.library.path=./lib -jar app.jar

# Только ошибки
java -Xcheck:jni:fatal -Djava.library.path=./lib -jar app.jar

# С логами GC
java -Xcheck:jni -Xlog:gc* -Djava.library.path=./lib -jar app.jar
```

### 6.2 Кастомное логирование в C++

```cpp
// В rtsp_client.cpp
#ifdef DEBUG
#define LOG_DEBUG(msg) \
    fprintf(stderr, "[RTSP] %s:%d: %s\n", __FILE__, __LINE__, msg)
#else
#define LOG_DEBUG(msg)
#endif

void rtsp_client_connect(RtspClient* client) {
    LOG_DEBUG("Connecting to RTSP server");
    // ... код подключения
}
```

### 6.3 Логирование через JNI

```cpp
// В JavaRtspClient.cpp
JNIEXPORT void JNICALL
Java_com_company_ipcamera_RtspClient_nativeLog(JNIEnv* env, jclass clazz, jstring message) {
    const char* msg = env->GetStringUTFChars(message, 0);
    fprintf(stderr, "[JNI] %s\n", msg);
    env->ReleaseStringUTFChars(message, msg);
}
```

### 6.4 Использование логов в Kotlin

```kotlin
expect object RtspLogger {
    fun debug(message: String)
    fun info(message: String)
    fun error(message: String, exception: Exception? = null)
}

// JVM реализация
actual object RtspLogger {
    actual fun debug(message: String) {
        if (BuildConfig.DEBUG) {
            Log.d("RTSP", message)
        }
    }
    
    actual fun info(message: String) {
        Log.i("RTSP", message)
    }
    
    actual fun error(message: String, exception: Exception?) {
        Log.e("RTSP", message, exception)
    }
}
```

---

## 📞 Полезные команды

### Проверка совместимости

```bash
# Windows
file video_processing.dll  # Cygwin/WSL
dumpbin /headers video_processing.dll

# Linux
file libvideo_processing.so
readelf -h libvideo_processing.so

# macOS
file libvideo_processing.dylib
file -arch all libvideo_processing.dylib
```

### Проверка зависимостей

```bash
# Windows
dependencywalker video_processing.dll

# Linux
ldd libvideo_processing.so
ldd --verbose libvideo_processing.so

# macOS
otool -L libvideo_processing.dylib
```

---

## 🎯 Чеклист отладки

- [ ] Библиотека загружается без ошибок
- [ ] Все символы экспортированы
- [ ] Зависимости разрешены
- [ ] Нет утечек памяти (Valgrind/ASan)
- [ ] Нет race conditions (ThreadSanitizer)
- [ ] Логирование работает
- [ ] Обработка ошибок корректная
- [ ] Тесты проходят

---

**Версия:** 1.0  
**Обновлено:** 24 мая 2026
