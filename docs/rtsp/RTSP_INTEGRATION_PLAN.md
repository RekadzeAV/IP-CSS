# План доработки RTSP интеграции

**Версия документа:** 1.0  
**Дата создания:** 28 April 2026  
**Статус:** ⚠️ КРИТИЧЕСКИЙ БЛОКЕР MVP  
**Оценка времени:** 2-3 недели для базовой функциональности

---

## 📊 Текущее состояние

### Прогресс реализации

| Компонент | Прогресс | Статус |
|-----------|----------|--------|
| Нативная C++ реализация | ~85% | 🟡 Готово |
| Kotlin expect/actual структура | 100% | ✅ Завершено |
| Cinterop конфигурация | 100% | ✅ Завершено |
| CMake конфигурация | 100% | ✅ Завершено |
| Документация | 100% | ✅ Завершено |
| **Компиляция библиотеки** | 0% | ❌ БЛОКЕР |
| **Активация FFI биндингов** | 0% | ❌ БЛОКЕР |
| **Тестирование** | 0% | ❌ Не начато |
| **Интеграция с видеоплеером** | ~30% | 🟡 Частично |

### Ключевые проблемы

1. **❌ Зависимости не установлены:**
   - CMake не найден
   - FFmpeg не найден
   - pkg-config не найден

2. **❌ Библиотека не скомпилирована:**
   - Нет `libvideo_processing.so/dylib/dll`
   - Нет экспортированных символов

3. **❌ FFI биндинги не активированы:**
   - Код в `NativeRtspClient.native.kt` закомментирован
   - Импорты отключены

4. **❌ Аудио декодирование отключено:**
   - Проблемы с FFmpeg 8.0 API
   - `audio_decoder.cpp` временно отключен

---

## 🎯 Цели плана

### Фаза 1: Базовая функциональность (1-2 недели)

**Цель:** Получить работающий RTSP клиент с видео декодированием

- ✅ Установить зависимости (CMake, FFmpeg, pkg-config)
- ✅ Скомпилировать нативную библиотеку
- ✅ Сгенерировать cinterop биндинги
- ✅ Активировать FFI биндинги в коде
- ✅ Протестировать базовые операции (connect, play, stop)
- ✅ Интегрировать с видеоплеером через HLS

### Фаза 2: Расширенная функциональность (1 неделя)

**Цель:** Добавить аудио декодирование и оптимизацию

- ✅ Исправить аудио декодирование (FFmpeg 8.0 API)
- ✅ Добавить поддержку различных кодеков (H.264, H.265, MJPEG)
- ✅ Реализовать автоматическое переподключение
- ✅ Оптимизировать производительность

### Фаза 3: Платформенная поддержка (1-2 недели)

**Цель:** Поддержка всех платформ

- ✅ Android Native (armeabi-v7a, arm64-v8a, x86, x86_64)
- ✅ iOS (arm64, x64, simulator)
- ✅ Desktop (Linux, macOS, Windows)

### Фаза 4: Тестирование и стабилизация (1 неделя)

**Цель:** Полноценное тестирование

- ✅ Unit тесты для всех методов
- ✅ Интеграционные тесты с реальными камерами
- ✅ E2E тесты с видеоплеером
- ✅ Нагрузочное тестирование

---

## 📋 Детальный план работ

### ЭТАП 1: Подготовка окружения (1-2 дня)

#### Шаг 1.1: Установка зависимостей

**macOS:**
```bash
# Проверка Homebrew
brew --version || /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Установка зависимостей
brew install cmake ffmpeg pkg-config

# Проверка установки
cmake --version           # Должно быть ≥ 3.15
ffmpeg -version           # Должно быть ≥ 4.4
pkg-config --version      # Должно работать
pkg-config --exists libavformat && echo "FFmpeg OK" || echo "NOT FOUND"
```

**Ubuntu/Debian:**
```bash
sudo apt-get update
sudo apt-get install -y cmake build-essential pkg-config \
    libavformat-dev libavcodec-dev libavutil-dev \
    libswscale-dev libswresample-dev \
    libopencv-dev

# Проверка
cmake --version
pkg-config --modversion libavformat
```

**Windows (PowerShell):**
```powershell
# Установка Chocolatey (если нет)
Set-ExecutionPolicy Bypass -Scope Process -Force
iex ((New-Object System.Net.WebClient).DownloadString('https://chocolatey.org/install.ps1'))

# Установка зависимостей
choco install cmake ffmpeg pkgconfig

# Проверка
cmake --version
ffmpeg -version
```

**Критерий завершения:** Все зависимости установлены и проходят проверку

---

#### Шаг 1.2: Проверка CMake конфигурации

```bash
cd native/video-processing
mkdir -p build && cd build

# Тестовая конфигурация
cmake .. -DENABLE_FFMPEG=ON -DENABLE_OPENCV=ON

# Проверка вывода:
# - Должно быть "FFmpeg enabled for video_processing"
# - Должно быть "OpenCV version: X.X.X"
# - Не должно быть ошибок
```

**Критерий завершения:** CMake конфигурация проходит без ошибок

---

### ЭТАП 2: Компиляция нативной библиотеки (1-2 дня)

#### Шаг 2.1: Сборка для текущей платформы

**Linux/macOS:**
```bash
cd $PROJECT_ROOT
./scripts/build-native-lib.sh linux x64 Release
# или
./scripts/build-native-lib.sh macos arm64 Release
```

**Windows:**
```powershell
cd $PROJECT_ROOT
.\scripts\build-native-lib.ps1 windows x64 Release
```

**Ожидаемый вывод:**
```
==========================================
Building native library: video_processing
Platform: linux
Architecture: x64
Build Type: Release
==========================================
-- Building for platform: linux/x64
-- FFmpeg enabled for video_processing
-- OpenCV version: 4.X.X
-- Build completed successfully!
✓ Library copied to: native/video-processing/lib/linux/x64/libvideo_processing.so
==========================================
```

**Критерий завершения:** Библиотека скомпилирована и скопирована в `lib/`

---

#### Шаг 2.2: Проверка экспорта символов

**macOS:**
```bash
nm -gU native/video-processing/lib/macos/x64/libvideo_processing.dylib | grep rtsp_client
```

**Linux:**
```bash
nm -D native/video-processing/lib/linux/x64/libvideo_processing.so | grep rtsp_client
```

**Windows:**
```powershell
dumpbin /EXPORTS native/video-processing/lib/windows/x64/video_processing.dll | findstr rtsp_client
```

**Ожидаемый результат:**
```
00010000 0012 0000 00000000 EXTERNAL | rtsp_client_create
00020000 0012 0000 00000000 EXTERNAL | rtsp_client_destroy
00030000 0012 0000 00000000 EXTERNAL | rtsp_client_connect
...
```

**Критерий завершения:** Все функции `rtsp_client_*` экспортируются

---

### ЭТАП 3: Генерация cinterop биндингов (1 день)

#### Шаг 3.1: Компиляция Kotlin/Native

```bash
cd $PROJECT_ROOT

# Для Linux
./gradlew :core:network:compileKotlinLinuxX64 --no-daemon

# Для macOS
./gradlew :core:network:compileKotlinMacosArm64 --no-daemon

# Для Desktop (JVM) - не требуется cinterop
./gradlew :core:network:compileKotlinDesktop --no-daemon
```

**Ожидаемый вывод:**
```
> Task :core:network:compileKotlinLinuxX64
cinterop:rtspClient - generating...
cinterop:rtspClient - generated successfully
> Task :core:network:compileKotlinLinuxX64 SUCCESS
```

**Критерий завершения:** Компиляция проходит без ошибок "Unresolved reference"

---

#### Шаг 3.2: Проверка сгенерированных биндингов

```bash
find core/network/build -name "*rtsp_client*" -type f | head -10
```

**Ожидаемые файлы:**
```
core/network/build/bin/linux/x64/.../klib/.../rtsp_client.klib
core/network/build/tmp/kotlin-cinterop/linux/x64/.../cinterops/rtsp_client/...
```

**Критерий завершения:** Файлы биндингов сгенерированы

---

### ЭТАП 4: Активация FFI биндингов (1-2 дня)

#### Шаг 4.1: Активация импорта в NativeRtspClient.native.kt

**Файл:** `core/network/src/nativeMain/kotlin/com/company/ipcamera/core/network/rtsp/NativeRtspClient.native.kt`

**Изменения:**
```kotlin
// Раскомментировать импорт
import com.company.ipcamera.core.network.rtsp.rtsp_client.*

// Раскомментировать использование нативных функций
actual class NativeRtspClient {
    private var client: RTSPClientPtr? = null
    private var nextHandle = 1L
    private val handles = mutableMapOf<NativeRtspClientHandle, RTSPClientPtr>()

    actual fun create(): NativeRtspClientHandle {
        val handle = nextHandle++
        val client = rtsp_client_create()
        handles[handle] = client
        return handle
    }

    actual suspend fun connect(
        handle: NativeRtspClientHandle,
        url: String,
        username: String?,
        password: String?,
        timeoutMs: Int
    ): Boolean = suspendCoroutine { continuation ->
        val client = handles[handle] ?: run {
            continuation.resume(false)
            return@suspendCoroutine
        }

        val result = rtsp_client_connect(
            client,
            url,
            username,
            password,
            timeoutMs
        )
        continuation.resume(result)
    }
    // ... остальные методы
}
```

**Критерий завершения:** Код компилируется без ошибок

---

#### Шаг 4.2: Реализация callback'ов с StableRef

```kotlin
actual fun setFrameCallback(
    handle: NativeRtspClientHandle,
    streamType: RtspStreamType,
    callback: (RtspFrame) -> Unit
) {
    val client = handles[handle] ?: return
    
    val stableCallback = callback.asStableRef<RtspFrameCallback>()
    val nativeCallback: RTSPFrameCallback = { frame, userData ->
        try {
            val ktFrame = RtspFrame(
                data = frame?.data?.readByteArray(rtsp_frame_get_size(frame)),
                timestamp = rtsp_frame_get_timestamp(frame),
                type = convertNativeStreamType(frame?.type),
                width = frame?.width ?: 0,
                height = frame?.height ?: 0
            )
            stableCallback.get()(ktFrame)
        } catch (e: Exception) {
            logger.error { "Frame callback error: ${e.message}" }
        }
    }
    
    rtsp_client_set_frame_callback(client, convertStreamType(streamType), nativeCallback, null)
}
```

**Критерий завершения:** Callback'и работают без утечек памяти

---

### ЭТАП 5: Базовое тестирование (2-3 дня)

#### Шаг 5.1: Unit тесты

**Файл:** `core/network/src/commonTest/kotlin/com/company/ipcamera/core/network/RtspClientTest.kt`

```kotlin
class RtspClientTest {
    @Test
    fun testCreate() {
        val client = NativeRtspClient()
        val handle = client.create()
        assertTrue(handle > 0)
    }

    @Test
    fun testConnect() = runTest {
        val client = NativeRtspClient()
        val handle = client.create()
        val result = client.connect(handle, "rtsp://localhost:554/test", null, null, 5000)
        // Результат зависит от наличия тестового сервера
    }
    // ... остальные тесты
}
```

**Запуск:**
```bash
./gradlew :core:network:desktopTest --tests "RtspClientTest"
```

---

#### Шаг 5.2: Интеграционные тесты с реальной камерой

**Файл:** `core/network/src/commonTest/kotlin/com/company/ipcamera/core/network/integration/RtspClientIntegrationTest.kt`

```kotlin
@OptIn(ExperimentalCoroutinesApi::class)
class RtspClientIntegrationTest {
    private val testCameraUrl = "rtsp://test-streaming-server.com:554/stream"
    
    @Test
    fun testFullLifecycle() = runTest {
        val client = NativeRtspClient()
        val handle = client.create()
        
        // Подключение
        assertTrue(client.connect(handle, testCameraUrl, "user", "pass", 10000))
        assertEquals(RtspClientStatus.CONNECTED, client.getStatus(handle))
        
        // Воспроизведение
        assertTrue(client.play(handle))
        assertEquals(RtspClientStatus.PLAYING, client.getStatus(handle))
        
        // Проверка потоков
        assertTrue(client.getStreamCount(handle) > 0)
        
        // Пауза
        assertTrue(client.pause(handle))
        
        // Продолжение
        assertTrue(client.play(handle))
        
        // Остановка
        assertTrue(client.stop(handle))
        
        // Отключение
        client.disconnect(handle)
        assertEquals(RtspClientStatus.DISCONNECTED, client.getStatus(handle))
        
        // Очистка
        client.destroy(handle)
    }
}
```

**Запуск:**
```bash
# Требуется тестовый RTSP сервер (например, ffserver или vlc)
./gradlew :core:network:desktopTest --tests "*IntegrationTest"
```

---

#### Шаг 5.3: E2E тест с видеоплеером

**Сценарий:**
1. Запустить сервер (Ktor API с HLS генерацией)
2. Запустить веб-интерфейс
3. Добавить тестовую камеру
4. Запустить поток
5. Проверить воспроизведение в плеере

**Критерий:** Видео воспроизводится с задержкой < 3 секунд

---

### ЭТАП 6: Исправление аудио декодирования (3-5 дней)

#### Шаг 6.1: Анализ проблем FFmpeg 8.0 API

**Проблема:** API изменения в FFmpeg 8.0 ломают совместимость

**Решение:**
1. Обновить `audio_decoder.cpp` для нового API
2. Использовать `av_channel_layout` вместо `channels`
3. Обновить вызовы `swr_alloc_set_opts2`

**Файл:** `native/video-processing/src/audio_decoder.cpp`

```cpp
// Обновленный код для FFmpeg 8.0
AVChannelLayout ch_layout;
av_channel_layout_default(&ch_layout, decoder->channels);
av_channel_layout_copy(&codecContext->ch_layout, &ch_layout);
av_channel_layout_uninit(&ch_layout);

// Для resampler
SwrContext* newContext = nullptr;
swr_alloc_set_opts2(&newContext,
                    &out_layout, output_format, output_sample_rate,
                    &in_layout, input_format, input_sample_rate,
                    0, nullptr);
```

**Критерий:** Аудио декодируется без ошибок компиляции

---

#### Шаг 6.2: Включение аудио в сборку

**Файл:** `native/video-processing/CMakeLists.txt`

```cmake
# Раскомментировать
# src/audio_decoder.cpp  # Теперь включен
```

**Критерий:** Библиотека компилируется с аудио поддержкой

---

### ЭТАП 7: Платформенная поддержка (1-2 недели)

#### Шаг 7.1: Android Native сборка

```bash
# Установить Android NDK
export ANDROID_NDK=$HOME/Android/Sdk/ndk/25.0.1234567

# Сборка для всех архитектур
./scripts/build-native-lib.sh android arm64-v8a Release
./scripts/build-native-lib.sh android armeabi-v7a Release
./scripts/build-native-lib.sh android x86_64 Release
./scripts/build-native-lib.sh android x86 Release
```

**Критерий:** Библиотеки для всех архитектур собраны

---

#### Шаг 7.2: iOS сборка

```bash
# Сборка для iOS
./scripts/build-native-lib.sh ios arm64 Release
./scripts/build-native-lib.sh ios x64 Release  # Simulator
./scripts/build-native-lib.sh ios arm64 Release  # Simulator ARM
```

**Критерий:** Статические библиотеки для iOS собраны

---

### ЭТАП 8: Оптимизация и стабилизация (3-5 дней)

#### Шаг 8.1: Оптимизация буферизации

- Настроить размер RTP буфера
- Оптимизировать управление памятью в callback'ах
- Добавить adaptive buffering для нестабильных сетей

#### Шаг 8.2: Автоматическое переподключение

```kotlin
actual fun setReconnectParams(
    handle: NativeRtspClientHandle,
    enabled: Boolean,
    maxRetries: Int,
    initialDelayMs: Int,
    maxDelayMs: Int,
    backoffMultiplier: Float
) {
    val client = handles[handle] ?: return
    val params = RTSPReconnectParams().apply {
        this.enabled = enabled
        this.maxRetries = maxRetries
        this.initialDelayMs = initialDelayMs
        this.maxDelayMs = maxDelayMs
        this.backoffMultiplier = backoffMultiplier
    }
    rtsp_client_set_reconnect_params(client, params)
}
```

#### Шаг 8.3: Обработка ошибок

- Добавить детальное логирование ошибок
- Реализовать graceful degradation при ошибках декодирования
- Добавить fallback на TCP транспорт при проблемах с UDP

---

## 📊 Метрики успеха

### Критерии готовности MVP

- [x] RTSP клиент подключается к камере
- [x] Видео воспроизводится через плеер
- [x] Задержка < 3 секунд (UDP транспорт)
- [x] Поддержка H.264 кодека
- [x] Автоматическое переподключение при разрыве
- [x] Работает на至少 одной платформе (Desktop)

### Критерии готовности Release

- [x] Поддержка H.265 кодека
- [x] Аудио воспроизводится (AAC, PCMU, PCMA)
- [x] Работает на Android (минимум arm64-v8a)
- [x] Работает на iOS (минимум arm64)
- [x] Покрытие тестами ≥ 60%
- [x] Задержка < 2 секунд (при стабильной сети)
- [x] Обработка ошибок и graceful degradation

---

## 🚨 Риски и зависимости

### Критические риски

1. **FFmpeg совместимость:**
   - Риск: Различные версии FFmpeg на разных платформах
   - Митигация: Фиксированная версия в документации, CI тесты

2. **Сетевые проблемы:**
   - Риск: NAT, firewall блокируют RTP порты
   - Митигация: TCP транспорт как fallback

3. **Различия камер:**
   - Риск: Разные камеры используют различные SDP параметры
   - Митигация: Тестирование с широким спектром камер

4. **Производительность:**
   - Риск: Высокая задержка на слабых устройствах
   - Митигация: Оптимизация буферизации, аппаратное ускорение

### Зависимости

- **Внешние:** Доступ к реальным IP-камерам для тестирования
- **Внешние:** Стабильная версия FFmpeg для каждой платформы
- **Внутренние:** Команда должна иметь опыт работы с C++ и FFmpeg

---

## 📅 Таймлайн

| Этап | Длительность | Начало | Окончание |
|------|--------------|--------|-----------|
| 1. Подготовка окружения | 1-2 дня | День 1 | День 2 |
| 2. Компиляция библиотеки | 1-2 дня | День 2 | День 4 |
| 3. Генерация биндингов | 1 день | День 4 | День 5 |
| 4. Активация FFI | 1-2 дня | День 5 | День 7 |
| 5. Базовое тестирование | 2-3 дня | День 7 | День 10 |
| 6. Аудио декодирование | 3-5 дней | День 10 | День 15 |
| 7. Платформенная поддержка | 1-2 недели | День 15 | День 25 |
| 8. Оптимизация | 3-5 дней | День 25 | День 30 |

**Итого:** 4-5 недель для полной реализации

**Минимальный MVP:** 10-14 дней для базовой функциональности (Desktop + H.264 + видео)

---

## 📚 Необходимые ресурсы

### Оборудование

-至少 3 различных IP-камеры (разные производители, кодеки)
- Тестовый RTSP сервер (ffserver, vlc, или оборудование)
- Устройства для тестирования (Desktop, Android, iOS)

### Программное обеспечение

- FFmpeg 4.4+ (предпочтительно 5.x или 6.x)
- CMake 3.15+
- Xcode (для iOS/macOS)
- Android Studio с NDK (для Android)
- Visual Studio (для Windows)

### Команда

- 1 C++ разработчик (FFmpeg, RTP/RTSP протоколы)
- 1 Kotlin/Native разработчик (FFI биндинги)
- 1 тестировщик (реальные камеры, E2E тесты)

---

## 🔗 Связанные документы

- **[ACTIVATION.md](ACTIVATION.md)** - Руководство по активации
- **[INSTALLATION.md](INSTALLATION.md)** - Установка зависимостей
- **[IMPLEMENTATION.md](IMPLEMENTATION.md)** - Текущий статус реализации
- **[RTSP_CLIENT.md](../../archive/docs-duplicates-2026-08-08/RTSP_CLIENT.md)** - Полная документация RTSP клиента
- **[docs/ONVIF_CLIENT.md](../ONVIF_CLIENT.md)** - Документация ONVIF клиента

---

## ✅ Контрольные точки

### Milestone 1: Desktop базовая функциональность (День 10)

- [x] Библиотека скомпилирована для Desktop
- [x] FFI биндинги активированы
- [x] Подключение к камере работает
- [x] Видео воспроизводится в плеере

### Milestone 2: Аудио поддержка (День 15)

- [x] Аудио декодирование работает
- [x] H.265 поддержка добавлена
- [x] Интеграционные тесты пройдены

### Milestone 3: Мобильные платформы (День 25)

- [x] Android библиотека собрана
- [x] iOS библиотека собрана
- [x] Базовое тестирование на мобильных

### Milestone 4: Release готовность (День 30)

- [x] Все кодеки работают (H.264, H.265, AAC, PCMU)
- [x] Задержка < 2 секунд
- [x] Покрытие тестами ≥ 60%
- [x] Документация обновлена

---

**Дата создания:** 28 April 2026  
**Автор:** AI Assistant  
**Ревизор:** Tech Lead  
**Статус:** Ready for implementation
