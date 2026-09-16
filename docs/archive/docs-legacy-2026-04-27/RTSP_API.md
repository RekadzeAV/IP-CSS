# RTSP Native Library API Documentation

**Дата создания:** 27 января 2026
**Версия:** 1.0
**Статус:** Актуально

> **📚 Связанные документы:**
> - [RTSP_CLIENT.md](RTSP_CLIENT.md) - Полная документация RTSP клиента
> - [planning/DESKTOP_SERVER_IMPLEMENTATION_PLAN_V2.md](planning/DESKTOP_SERVER_IMPLEMENTATION_PLAN_V2.md) - План реализации

---

## Обзор

Нативная C++ библиотека RTSP клиента предоставляет низкоуровневый API для работы с RTSP потоками. Библиотека поддерживает подключение к IP-камерам, получение видеопотоков, декодирование и обработку кадров.

**Расположение:**
- Заголовочный файл: `native/video-processing/include/rtsp_client.h`
- Реализация: `native/video-processing/src/rtsp_client.cpp`
- JNI обертка (Android): `native/video-processing/src/jni/rtsp_client_jni.cpp`

---

## Типы данных

### RTSPClient
```c
typedef struct RTSPClient RTSPClient;
```
Непрозрачный указатель на RTSP клиент. Создается через `rtsp_client_create()`.

### RTSPStreamType
```c
typedef enum {
    RTSP_STREAM_VIDEO,      // Видеопоток
    RTSP_STREAM_AUDIO,      // Аудиопоток
    RTSP_STREAM_METADATA    // Метаданные
} RTSPStreamType;
```

### RTSPStatus
```c
typedef enum {
    RTSP_STATUS_DISCONNECTED,  // Отключен
    RTSP_STATUS_CONNECTING,     // Подключение
    RTSP_STATUS_CONNECTED,      // Подключен
    RTSP_STATUS_PLAYING,        // Воспроизведение
    RTSP_STATUS_ERROR           // Ошибка
} RTSPStatus;
```

### RTSPFrame
```c
typedef struct {
    uint8_t* data;          // Данные кадра
    int size;               // Размер данных
    int64_t timestamp;     // Временная метка
    RTSPStreamType type;    // Тип потока
    int width;              // Ширина (для видео)
    int height;             // Высота (для видео)
} RTSPFrame;
```

### RTSPReconnectParams
```c
typedef struct {
    bool enabled;           // Включить автоматическое переподключение
    int maxRetries;         // Максимальное количество попыток (0 = бесконечно)
    int initialDelayMs;     // Начальная задержка между попытками (мс)
    int maxDelayMs;         // Максимальная задержка между попытками (мс)
    float backoffMultiplier; // Множитель для экспоненциальной задержки
} RTSPReconnectParams;
```

---

## Callback функции

### RTSPFrameCallback
```c
typedef void (*RTSPFrameCallback)(RTSPFrame* frame, void* userData);
```
Вызывается при получении нового кадра. После обработки кадра необходимо вызвать `rtsp_frame_release()`.

**Параметры:**
- `frame` - указатель на кадр (не должен быть освобожден вручную, используйте `rtsp_frame_release()`)
- `userData` - пользовательские данные, переданные при регистрации callback

### RTSPStatusCallback
```c
typedef void (*RTSPStatusCallback)(RTSPStatus status, const char* message, void* userData);
```
Вызывается при изменении статуса клиента.

**Параметры:**
- `status` - новый статус
- `message` - сообщение об ошибке (может быть NULL)
- `userData` - пользовательские данные

---

## Основные функции

### rtsp_client_create()
```c
RTSPClient* rtsp_client_create();
```
Создает новый экземпляр RTSP клиента.

**Возвращает:** Указатель на RTSP клиент или NULL при ошибке

**Пример:**
```c
RTSPClient* client = rtsp_client_create();
if (!client) {
    // Обработка ошибки
}
```

---

### rtsp_client_destroy()
```c
void rtsp_client_destroy(RTSPClient* client);
```
Уничтожает RTSP клиент и освобождает все ресурсы.

**Параметры:**
- `client` - указатель на RTSP клиент (может быть NULL)

**Примечание:** После вызова этой функции указатель становится недействительным.

---

### rtsp_client_connect()
```c
bool rtsp_client_connect(
    RTSPClient* client,
    const char* url,
    const char* username,
    const char* password,
    int timeout_ms
);
```
Подключается к RTSP серверу.

**Параметры:**
- `client` - указатель на RTSP клиент
- `url` - RTSP URL (например, "rtsp://192.168.1.100:554/stream")
- `username` - имя пользователя (может быть NULL)
- `password` - пароль (может быть NULL)
- `timeout_ms` - таймаут подключения в миллисекундах

**Возвращает:** `true` при успешном подключении, `false` при ошибке

**Пример:**
```c
bool success = rtsp_client_connect(
    client,
    "rtsp://192.168.1.100:554/stream",
    "admin",
    "password123",
    10000
);
```

---

### rtsp_client_disconnect()
```c
void rtsp_client_disconnect(RTSPClient* client);
```
Отключается от RTSP сервера.

**Параметры:**
- `client` - указатель на RTSP клиент

---

### rtsp_client_get_status()
```c
RTSPStatus rtsp_client_get_status(RTSPClient* client);
```
Получает текущий статус клиента.

**Параметры:**
- `client` - указатель на RTSP клиент

**Возвращает:** Текущий статус

---

### rtsp_client_play()
```c
bool rtsp_client_play(RTSPClient* client);
```
Начинает воспроизведение потока.

**Параметры:**
- `client` - указатель на RTSP клиент

**Возвращает:** `true` при успехе, `false` при ошибке

**Примечание:** Клиент должен быть подключен (`RTSP_STATUS_CONNECTED`).

---

### rtsp_client_stop()
```c
bool rtsp_client_stop(RTSPClient* client);
```
Останавливает воспроизведение потока.

**Параметры:**
- `client` - указатель на RTSP клиент

**Возвращает:** `true` при успехе, `false` при ошибке

---

### rtsp_client_pause()
```c
bool rtsp_client_pause(RTSPClient* client);
```
Приостанавливает воспроизведение потока.

**Параметры:**
- `client` - указатель на RTSP клиент

**Возвращает:** `true` при успехе, `false` при ошибке

---

### rtsp_client_get_stream_count()
```c
int rtsp_client_get_stream_count(RTSPClient* client);
```
Получает количество доступных потоков.

**Параметры:**
- `client` - указатель на RTSP клиент

**Возвращает:** Количество потоков (обычно 1-3: видео, аудио, метаданные)

---

### rtsp_client_get_stream_type()
```c
RTSPStreamType rtsp_client_get_stream_type(RTSPClient* client, int streamIndex);
```
Получает тип потока по индексу.

**Параметры:**
- `client` - указатель на RTSP клиент
- `streamIndex` - индекс потока (0..stream_count-1)

**Возвращает:** Тип потока

---

### rtsp_client_get_stream_info()
```c
bool rtsp_client_get_stream_info(
    RTSPClient* client,
    int streamIndex,
    int* width,
    int* height,
    int* fps,
    char* codec,
    int codecBufferSize
);
```
Получает информацию о потоке.

**Параметры:**
- `client` - указатель на RTSP клиент
- `streamIndex` - индекс потока
- `width` - указатель для сохранения ширины (может быть NULL для аудио)
- `height` - указатель для сохранения высоты (может быть NULL для аудио)
- `fps` - указатель для сохранения FPS (может быть NULL)
- `codec` - буфер для сохранения названия кодека (например, "H264", "H265", "MJPEG")
- `codecBufferSize` - размер буфера codec

**Возвращает:** `true` при успехе, `false` при ошибке

**Пример:**
```c
int width, height, fps;
char codec[64];
if (rtsp_client_get_stream_info(client, 0, &width, &height, &fps, codec, sizeof(codec))) {
    printf("Stream: %dx%d @ %d fps, codec: %s\n", width, height, fps, codec);
}
```

---

### rtsp_client_set_frame_callback()
```c
void rtsp_client_set_frame_callback(
    RTSPClient* client,
    RTSPStreamType streamType,
    RTSPFrameCallback callback,
    void* userData
);
```
Устанавливает callback для получения кадров определенного типа.

**Параметры:**
- `client` - указатель на RTSP клиент
- `streamType` - тип потока (VIDEO, AUDIO, METADATA)
- `callback` - функция callback (может быть NULL для отмены)
- `userData` - пользовательские данные, передаваемые в callback

**Пример:**
```c
void onFrameReceived(RTSPFrame* frame, void* userData) {
    // Обработка кадра
    printf("Received frame: %dx%d, size: %d\n", frame->width, frame->height, frame->size);

    // Обязательно освободить кадр
    rtsp_frame_release(frame);
}

rtsp_client_set_frame_callback(client, RTSP_STREAM_VIDEO, onFrameReceived, NULL);
```

---

### rtsp_client_set_status_callback()
```c
void rtsp_client_set_status_callback(
    RTSPClient* client,
    RTSPStatusCallback callback,
    void* userData
);
```
Устанавливает callback для отслеживания изменений статуса.

**Параметры:**
- `client` - указатель на RTSP клиент
- `callback` - функция callback (может быть NULL для отмены)
- `userData` - пользовательские данные

**Пример:**
```c
void onStatusChanged(RTSPStatus status, const char* message, void* userData) {
    switch (status) {
        case RTSP_STATUS_CONNECTED:
            printf("Connected to server\n");
            break;
        case RTSP_STATUS_ERROR:
            printf("Error: %s\n", message ? message : "Unknown error");
            break;
        // ...
    }
}

rtsp_client_set_status_callback(client, onStatusChanged, NULL);
```

---

### rtsp_client_set_reconnect_params()
```c
void rtsp_client_set_reconnect_params(RTSPClient* client, const RTSPReconnectParams* params);
```
Устанавливает параметры автоматического переподключения.

**Параметры:**
- `client` - указатель на RTSP клиент
- `params` - параметры переподключения (может быть NULL для отключения)

**Пример:**
```c
RTSPReconnectParams params = {
    .enabled = true,
    .maxRetries = 5,
    .initialDelayMs = 1000,
    .maxDelayMs = 30000,
    .backoffMultiplier = 2.0f
};
rtsp_client_set_reconnect_params(client, &params);
```

---

## Функции работы с кадрами

### rtsp_frame_get_size()
```c
int rtsp_frame_get_size(RTSPFrame* frame);
```
Получает размер данных кадра.

---

### rtsp_frame_get_data()
```c
const uint8_t* rtsp_frame_get_data(RTSPFrame* frame);
```
Получает указатель на данные кадра.

**Примечание:** Данные действительны до вызова `rtsp_frame_release()`.

---

### rtsp_frame_get_timestamp()
```c
int64_t rtsp_frame_get_timestamp(RTSPFrame* frame);
```
Получает временную метку кадра.

---

### rtsp_frame_release()
```c
void rtsp_frame_release(RTSPFrame* frame);
```
Освобождает кадр. **Обязательно вызывать** после обработки кадра в callback.

**Параметры:**
- `frame` - указатель на кадр (может быть NULL)

---

## Типичный сценарий использования

```c
// 1. Создание клиента
RTSPClient* client = rtsp_client_create();
if (!client) {
    return -1;
}

// 2. Установка callbacks
rtsp_client_set_frame_callback(client, RTSP_STREAM_VIDEO, onFrameReceived, NULL);
rtsp_client_set_status_callback(client, onStatusChanged, NULL);

// 3. Настройка переподключения
RTSPReconnectParams reconnectParams = {
    .enabled = true,
    .maxRetries = 5,
    .initialDelayMs = 1000,
    .maxDelayMs = 30000,
    .backoffMultiplier = 2.0f
};
rtsp_client_set_reconnect_params(client, &reconnectParams);

// 4. Подключение
if (!rtsp_client_connect(client, "rtsp://192.168.1.100:554/stream", "admin", "password", 10000)) {
    rtsp_client_destroy(client);
    return -1;
}

// 5. Начало воспроизведения
if (!rtsp_client_play(client)) {
    rtsp_client_disconnect(client);
    rtsp_client_destroy(client);
    return -1;
}

// 6. Получение информации о потоке
int width, height, fps;
char codec[64];
if (rtsp_client_get_stream_info(client, 0, &width, &height, &fps, codec, sizeof(codec))) {
    printf("Stream info: %dx%d @ %d fps, codec: %s\n", width, height, fps, codec);
}

// 7. Ожидание кадров (callback будет вызываться автоматически)
// ...

// 8. Остановка и очистка
rtsp_client_stop(client);
rtsp_client_disconnect(client);
rtsp_client_destroy(client);
```

---

## Поддерживаемые кодеки

### Видео:
- **H.264** (AVC) - полная поддержка
- **H.265** (HEVC) - полная поддержка
- **MJPEG** - полная поддержка

### Аудио:
- **AAC** - поддержка
- **PCMU** (G.711 μ-law) - поддержка
- **PCMA** (G.711 A-law) - поддержка
- **MP3** - поддержка

---

## Транспорт

Библиотека поддерживает:
- **UDP** (RTP/RTCP) - по умолчанию
- **TCP** (interleaved binary data) - автоматически при необходимости

---

## Платформы

Библиотека компилируется для:
- **Windows** (x64) - `.dll`
- **Linux** (x64, ARM) - `.so`
- **macOS** (Intel x64, Apple Silicon ARM64) - `.dylib`
- **Android** (ARM32, ARM64, x86, x64) - `.so` (через JNI)

---

## Сборка

См. [docs/rtsp/NATIVE_LIBRARY_BUILD.md](rtsp/NATIVE_LIBRARY_BUILD.md) для инструкций по сборке.

---

## JNI интеграция

Для использования из Java/Kotlin (JVM) требуется JNI обертка. Текущая реализация:
- **Android**: `native/video-processing/src/jni/rtsp_client_jni.cpp` ✅
- **Desktop JVM**: Требуется создание отдельной обертки (см. план реализации)

---

**Последнее обновление:** 27 января 2026
