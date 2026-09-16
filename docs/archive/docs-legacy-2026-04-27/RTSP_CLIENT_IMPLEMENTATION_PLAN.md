# Пошаговый план реализации RTSP клиента (4.3)

**Версия:** 1.4
**Дата:** 26 January 2026
**Статус:** В процессе
**Прогресс:** ~65% → Цель: 100%
**Прогресс:** ~65% → Цель: 100%

---

## 📋 Обзор

RTSP клиент является критическим блокером для завершения MVP. Текущая реализация имеет базовую структуру, но требует завершения интеграции Kotlin ↔ C++ и полной реализации RTSP протокола.

### Текущее состояние
- ✅ Kotlin обертка с базовой структурой (expect/actual)
- ✅ Нативная C++ библиотека с заголовками
- ✅ JNI обертки для Android
- ✅ cinterop определения для native платформ
- ✅ Базовая реализация RTSP протокола (DESCRIBE, SETUP, PLAY готовы)
- ✅ Улучшенный парсинг SDP (rtpmap, fmtp, control, framesize, framerate)
- ✅ Digest Authentication (полная реализация для DESCRIBE, SETUP, PLAY)
- ✅ Обработка RTP пакетов с поддержкой H.264/H.265 NAL units и фрагментации
- ✅ TCP транспорт для RTP (interleaved binary data)
- ⚠️ Интеграция Kotlin ↔ C++ (FFI биндинги настроены, требуется тестирование)
- ⚠️ RTP/RTCP обработка (базовая реализация есть, требуется улучшение)
- ❌ Декодирование видео/аудио (только заглушки)

---

## 🎯 Цели реализации

1. **Полная интеграция Kotlin ↔ C++** через FFI (cinterop) для всех платформ
2. **Завершение реализации RTSP протокола** (DESCRIBE, SETUP, PLAY, PAUSE, TEARDOWN)
3. **Реализация RTP/RTCP обработки** с поддержкой UDP и TCP транспорта
4. **Интеграция декодирования видео/аудио** через FFmpeg или нативные кодеки
5. **Реализация Digest Authentication** как альтернативы Basic
6. **Тестирование и отладка** на реальных IP-камерах

---

## 📝 Пошаговый план реализации

### Этап 1: Настройка FFI биндингов (cinterop) - 3-5 дней

#### 1.1 Проверка и исправление cinterop определений
**Файл:** `core/network/src/nativeInterop/cinterop/rtsp_client.def`

**Задачи:**
- [x] Проверить корректность всех определений типов
- [x] Добавить недостающие типы (RTSPStatus, RTSPStreamType, RTSPReconnectParams)
- [x] Настроить правильные пути к заголовкам для всех платформ
- [x] Настроить правильные пути к библиотекам для всех платформ
- [x] Добавить определения callback функций

**Детали:**
```kotlin
// Добавить в .def файл:
typedef enum RTSPStatus RTSPStatus;
typedef enum RTSPStreamType RTSPStreamType;
typedef struct RTSPReconnectParams RTSPReconnectParams;
typedef void (*RTSPFrameCallback)(RTSPFrame* frame, void* userData);
typedef void (*RTSPStatusCallback)(RTSPStatus status, const char* message, void* userData);
```

**Ожидаемый результат:** cinterop успешно генерирует биндинги для всех платформ

---

#### 1.2 Настройка сборки нативных библиотек
**Файлы:**
- `native/video-processing/CMakeLists.txt`
- `build.gradle.kts` (для модуля core:network)

**Задачи:**
- [ ] Убедиться, что CMake правильно собирает библиотеку для всех платформ
- [ ] Настроить правильные пути к скомпилированным библиотекам
- [ ] Добавить зависимости на нативную библиотеку в Gradle
- [ ] Настроить автоматическую сборку нативной библиотеки перед компиляцией Kotlin

**Детали:**
```kotlin
// В build.gradle.kts для core:network
kotlin {
    nativeMain.dependencies {
        implementation(project(":native:video-processing"))
    }
}
```

**Ожидаемый результат:** Нативная библиотека автоматически собирается и линкуется

---

#### 1.3 Тестирование FFI биндингов
**Файл:** `core/network/src/commonTest/kotlin/.../RtspClientFFITest.kt`

**Задачи:**
- [ ] Создать unit тесты для проверки создания/уничтожения клиента
- [ ] Проверить корректность конвертации типов
- [ ] Проверить работу callbacks
- [ ] Протестировать на всех платформах (Android, iOS, Desktop)

**Ожидаемый результат:** Все FFI биндинги работают корректно

---

### Этап 2: Завершение реализации RTSP протокола - 5-7 дней

#### 2.1 Улучшение парсинга SDP
**Файл:** `native/video-processing/src/rtsp_client.cpp` (функция `parse_sdp`)

**Задачи:**
- [x] Улучшить парсинг SDP для поддержки всех стандартных атрибутов
- [x] Добавить поддержку `a=fmtp` для параметров кодеков
- [x] Добавить поддержку `a=control` для track ID
- [x] Добавить поддержку `a=rtpmap` для всех кодеков (H.264, H.265, AAC, G.711 и т.д.)
- [x] Добавить поддержку `a=framesize` и `a=framerate` для разрешения и FPS
- [x] Добавить поддержку session-level `a=control`
- [x] Обработка ошибок парсинга

**Детали:**
```cpp
// Улучшенный парсинг SDP
static bool parse_sdp(const std::string& sdp, std::vector<RTPStream>& streams, RTSPClient* client) {
    // Парсинг v= (версия)
    // Парсинг o= (origin)
    // Парсинг s= (session name)
    // Парсинг m= (media description) для каждого потока
    // Парсинг a= (attributes) для каждого потока:
    //   - rtpmap: кодек и clock rate
    //   - fmtp: параметры кодеков (sprop-parameter-sets для H.264)
    //   - control: URL для SETUP запроса
    //   - range: временной диапазон
}
```

**Ожидаемый результат:** SDP корректно парсится для всех типов камер

---

#### 2.2 Реализация Digest Authentication
**Файл:** `native/video-processing/src/rtsp_client.cpp`

**Задачи:**
- [x] Добавить парсинг заголовка `WWW-Authenticate` из ответа 401
- [x] Реализовать алгоритм Digest Authentication (MD5)
- [x] Добавить генерацию `Authorization` заголовка для Digest
- [x] Поддержка различных алгоритмов (MD5, MD5-sess)
- [x] Поддержка qop (quality of protection)
- [x] Интеграция с существующей Basic Authentication
- [x] Добавить Digest Authentication в SETUP и PLAY запросы
- [x] Обработка stale nonce и обновление параметров

**Детали:**
```cpp
// Структура для Digest параметров
struct DigestAuthParams {
    std::string realm;
    std::string nonce;
    std::string algorithm;
    std::string qop;
    std::string opaque;
};

// Функция генерации Digest ответа
static std::string generate_digest_auth(
    const std::string& method,
    const std::string& uri,
    const std::string& username,
    const std::string& password,
    const DigestAuthParams& params
);
```

**Ожидаемый результат:** Поддержка Basic и Digest Authentication

---

#### 2.3 Улучшение обработки RTSP ответов
**Файл:** `native/video-processing/src/rtsp_client.cpp` (функция `parse_rtsp_response`)

**Задачи:**
- [ ] Улучшить парсинг RTSP ответов
- [ ] Добавить обработку всех стандартных заголовков
- [ ] Добавить обработку ошибок (4xx, 5xx)
- [ ] Добавить поддержку `Transport` заголовка в SETUP ответе
- [ ] Добавить поддержку `Range` заголовка в PLAY ответе
- [ ] Добавить поддержку `RTP-Info` заголовка

**Ожидаемый результат:** Корректная обработка всех RTSP ответов

---

#### 2.4 Реализация TCP транспорта для RTP
**Файл:** `native/video-processing/src/rtsp_client.cpp`

**Задачи:**
- [x] Добавить поддержку `RTP/AVP/TCP` транспорта
- [x] Реализовать инкапсуляцию RTP в RTSP (interleaved binary data)
- [x] Добавить обработку `interleaved` параметра в Transport заголовке
- [x] Реализовать чтение бинарных данных из RTSP сокета
- [x] Добавить выбор транспорта (UDP по умолчанию, TCP как fallback)
- [x] Поддержка платформо-специфичных вызовов (Windows/Linux)

**Детали:**
```cpp
// Структура для TCP RTP потока
struct TCPRTPStream {
    int channel; // RTP channel
    int rtcpChannel; // RTCP channel
    std::vector<uint8_t> buffer;
};

// Функция чтения interleaved данных
static bool read_interleaved_data(SOCKET sock, uint8_t& channel, uint16_t& length, std::vector<uint8_t>& data);
```

**Ожидаемый результат:** Поддержка UDP и TCP транспорта для RTP

---

### Этап 3: Реализация RTP/RTCP обработки - 7-10 дней

#### 3.1 Улучшение парсинга RTP пакетов ✅ ЗАВЕРШЕНО
**Файл:** `native/video-processing/src/rtsp_client.cpp` (функция `process_rtp_packet`)

**Задачи:**
- [x] Улучшить парсинг RTP заголовка (версия, padding, extension, CSRC)
  - Создана структура RTPPacket для детального парсинга
  - Реализована функция parse_rtp_packet
- [x] Добавить обработку RTP extension headers
  - Парсинг extension ID и extension length
  - Сохранение extension данных
- [x] Добавить проверку sequence number для обнаружения потерь
  - Улучшенная проверка с учетом wrap around
  - Подсчет потерянных пакетов
- [x] Добавить обработку marker bit
  - Обработка marker bit для видео и аудио
- [x] Добавить поддержку различных payload types
  - Проверка соответствия payload type потоку
- [x] Добавить обработку padding
  - Корректное удаление padding из payload

**Выполнено:** 26 January 2026

**Детали:**
```cpp
// Структура для RTP пакета
struct RTPPacket {
    uint8_t version;
    bool padding;
    bool extension;
    uint8_t csrcCount;
    bool marker;
    uint8_t payloadType;
    uint16_t sequence;
    uint32_t timestamp;
    uint32_t ssrc;
    std::vector<uint32_t> csrc;
    std::vector<uint8_t> payload;
};

// Функция парсинга RTP пакета
static bool parse_rtp_packet(const uint8_t* data, int size, RTPPacket& packet);
```

**Ожидаемый результат:** Корректный парсинг всех типов RTP пакетов

---

#### 3.2 Реализация дефрагментации H.264/H.265 ✅ ЧАСТИЧНО ЗАВЕРШЕНО
**Файл:** `native/video-processing/src/rtsp_client.cpp`

**Задачи:**
- [x] Реализовать парсинг NAL units в RTP payload
  - Создана структура NALUnit
  - Функции parse_h264_nal_header и parse_h265_nal_header
- [x] Добавить обработку FU-A (Fragmentation Unit) для H.264
  - Функция process_fua_h264
  - Поддержка сборки фрагментированных NAL units
- [x] Добавить обработку single NAL unit для H.264
  - Функция process_single_nal_h264
- [x] Реализовать дефрагментацию для H.265 (HEVC)
  - Функция process_fua_h265
  - Поддержка H.265 NAL unit заголовков (2 байта)
- [x] Обработка последовательности пакетов и обнаружение потерь
  - Использование fragmentedBuffer для сборки
- [x] Добавить обработку STAP-A (Single Time Aggregation Packet)
  - Функция process_stapa_h264
- [ ] Добавить обработку STAP-B (для H.264)
- [ ] Добавить обработку MTAP16/MTAP24 (для H.264)
- [ ] Добавить обработку STAP-A/B для H.265

**Выполнено:** 26 January 2026
**Результат:**
- Полная поддержка FU-A для H.264 и H.265
- Поддержка single NAL unit для H.264 и H.265
- Поддержка STAP-A для H.264
- Интеграция дефрагментации в process_rtp_packet

**Детали:**
```cpp
// Структура для NAL unit
struct NALUnit {
    uint8_t type;
    bool forbidden;
    uint8_t nri;
    std::vector<uint8_t> data;
};

// Функция дефрагментации H.264
static std::vector<NALUnit> defragment_h264(const std::vector<RTPPacket>& packets);
```

**Ожидаемый результат:** Корректная дефрагментация H.264/H.265 потоков

---

#### 3.3 Реализация обработки RTCP пакетов
**Файл:** `native/video-processing/src/rtsp_client.cpp`

**Задачи:**
- [x] Реализовать парсинг RTCP пакетов (SR, RR, SDES, BYE, APP)
- [x] Добавить обработку Sender Report (SR)
- [x] Добавить обработку Receiver Report (RR)
- [x] Добавить обработку Source Description (SDES)
- [x] Добавить обработку BYE пакетов
- [x] Добавить расчет статистики (jitter, packet loss)
- [x] Интеграция с обработкой RTCP в receive_rtp_thread
- [ ] Реализовать отправку RTCP пакетов (RR для клиента)
- [ ] Интеграция с автоматическим переподключением на основе RTCP статистики

**Детали:**
```cpp
// Структура для RTCP статистики
struct RTCPStatistics {
    uint32_t ssrc;
    uint32_t packetsLost;
    uint32_t jitter;
    uint32_t lastSR;
    uint32_t delaySinceLastSR;
};

// Функция обработки RTCP пакета
static bool process_rtcp_packet(const uint8_t* data, int size, RTCPStatistics& stats);
```

**Ожидаемый результат:** Полная поддержка RTCP для мониторинга качества потока

---

#### 3.4 Улучшение потока приема RTP
**Файл:** `native/video-processing/src/rtsp_client.cpp` (функция `receive_rtp_thread`)

**Задачи:**
- [ ] Улучшить обработку множественных потоков
- [ ] Добавить правильную синхронизацию потоков
- [ ] Добавить обработку таймаутов
- [ ] Добавить обработку ошибок сети
- [ ] Оптимизировать использование памяти
- [ ] Добавить поддержку приоритетов потоков

**Ожидаемый результат:** Стабильный прием RTP пакетов для всех потоков

---

### Этап 4: Интеграция декодирования видео/аудио - 10-14 дней

#### 4.1 Настройка FFmpeg для декодирования
**Файл:** `native/video-processing/CMakeLists.txt`

**Задачи:**
- [ ] Убедиться, что FFmpeg правильно линкуется
- [ ] Настроить FFmpeg для всех платформ (Android, iOS, Desktop)
- [ ] Добавить поддержку необходимых кодеков (H.264, H.265, AAC, G.711)
- [ ] Настроить сборку FFmpeg для Android (через NDK)
- [ ] Настроить сборку FFmpeg для iOS

**Ожидаемый результат:** FFmpeg доступен для всех платформ

---

#### 4.2 Реализация декодирования H.264/H.265
**Файл:** `native/video-processing/src/rtsp_client.cpp` (в блоке `#ifdef ENABLE_FFMPEG`)

**Задачи:**
- [ ] Инициализация H.264 декодера через FFmpeg
- [ ] Парсинг SPS/PPS из SDP или из потока
- [ ] Декодирование NAL units в кадры
- [ ] Конвертация YUV в RGB (если необходимо)
- [ ] Обработка B-frames и P-frames
- [ ] Обработка ошибок декодирования

**Детали:**
```cpp
#ifdef ENABLE_FFMPEG
// Инициализация декодера
AVCodecContext* init_h264_decoder(const std::string& sps, const std::string& pps);

// Декодирование кадра
bool decode_h264_frame(
    AVCodecContext* codecContext,
    const std::vector<uint8_t>& nalUnits,
    AVFrame* outputFrame
);
#endif
```

**Ожидаемый результат:** Декодирование H.264/H.265 в кадры

---

#### 4.3 Реализация декодирования аудио ✅ ЗАВЕРШЕНО
**Файл:** `native/video-processing/src/rtsp_client.cpp`

**Задачи:**
- [x] Инициализация AAC декодера (`init_aac_decoder_for_stream`)
- [x] Инициализация G.711 декодера (`init_g711_decoder_for_stream`)
- [x] Декодирование аудио пакетов (`decode_aac_packet`, `decode_g711_packet`)
- [x] Конвертация аудио форматов (`init_audio_resampler`, ресемплинг через libswresample)
- [x] Синхронизация аудио и видео (структура `AVSync`, обновление timestamps)
- [x] Интеграция в `process_rtp_packet()` с вызовом callback'ов

**Статус:** Полностью реализовано и интегрировано в RTSP клиент

**Ожидаемый результат:** Декодирование аудио потоков

---

#### 4.4 Интеграция декодирования с callback'ами
**Файл:** `native/video-processing/src/rtsp_client.cpp`

**Задачи:**
- [ ] Интегрировать декодирование в поток приема RTP
- [ ] Вызывать callback с декодированными кадрами
- [ ] Обработка ошибок декодирования
- [ ] Освобождение ресурсов декодера

**Ожидаемый результат:** Декодированные кадры передаются в Kotlin через callbacks

---

### Этап 5: Улучшение Kotlin обертки - 3-5 дней

#### 5.1 Исправление NativeRtspClient.native.kt
**Файл:** `core/network/src/nativeMain/kotlin/.../NativeRtspClient.native.kt`

**Задачи:**
- [ ] Проверить корректность всех вызовов cinterop
- [ ] Исправить конвертацию типов
- [ ] Улучшить обработку ошибок
- [ ] Добавить логирование
- [ ] Оптимизировать работу с памятью

**Ожидаемый результат:** Native обертка работает корректно

---

#### 5.2 Исправление NativeRtspClient.jvm.kt
**Файл:** `core/network/src/jvmMain/kotlin/.../NativeRtspClient.jvm.kt`

**Задачи:**
- [ ] Убедиться, что JNI функции правильно вызываются
- [ ] Исправить загрузку библиотеки
- [ ] Улучшить обработку callbacks
- [ ] Добавить обработку ошибок

**Ожидаемый результат:** JVM обертка работает корректно

---

#### 5.3 Улучшение высокоуровневой обертки RtspClient
**Файл:** `core/network/src/commonMain/kotlin/.../RtspClient.kt`

**Задачи:**
- [ ] Улучшить обработку состояний
- [ ] Добавить retry логику
- [ ] Улучшить обработку ошибок
- [ ] Добавить метрики и статистику
- [ ] Оптимизировать работу с Flow

**Ожидаемый результат:** Высокоуровневая обертка удобна в использовании

---

### Этап 6: Тестирование и отладка - 5-7 дней

#### 6.1 Unit тесты
**Файлы:** `core/network/src/commonTest/kotlin/.../RtspClientTest.kt`

**Задачи:**
- [ ] Расширить существующие unit тесты
- [ ] Добавить тесты для всех методов
- [ ] Добавить тесты для обработки ошибок
- [ ] Добавить тесты для callbacks

**Ожидаемый результат:** Хорошее покрытие unit тестами

---

#### 6.2 Integration тесты
**Файлы:** `core/network/src/androidTest/kotlin/.../RtspClientIntegrationTest.kt`

**Задачи:**
- [ ] Создать тесты с реальными RTSP серверами
- [ ] Тестирование на различных камерах
- [ ] Тестирование различных кодеков
- [ ] Тестирование различных методов аутентификации
- [ ] Тестирование переподключения

**Ожидаемый результат:** Интеграционные тесты проходят на реальных камерах

---

#### 6.3 Отладка на реальных камерах
**Задачи:**
- [ ] Тестирование на камерах различных производителей
- [ ] Тестирование различных форматов потоков
- [ ] Тестирование при плохом качестве сети
- [ ] Оптимизация производительности
- [ ] Исправление найденных багов

**Ожидаемый результат:** RTSP клиент работает стабильно на реальных камерах

---

## 🔧 Технические детали

### Зависимости
- **FFmpeg** (для декодирования видео/аудио)
- **OpenCV** (опционально, для обработки изображений)
- **Threads** (для многопоточности)
- **Sockets** (для сетевого взаимодействия)

### Платформы
- ✅ Android (через JNI)
- ✅ iOS (через cinterop)
- ✅ Desktop (Windows, Linux, macOS через cinterop)

### Поддерживаемые кодеки
- **Видео:** H.264, H.265 (HEVC)
- **Аудио:** AAC, G.711 (PCM), MP3

### Поддерживаемые методы аутентификации
- Basic Authentication
- Digest Authentication

---

## 📊 Метрики успеха

### Функциональные требования
- [ ] Подключение к RTSP серверу
- [ ] Воспроизведение видеопотока
- [ ] Воспроизведение аудиопотока (если доступен)
- [ ] Поддержка Basic и Digest Authentication
- [ ] Автоматическое переподключение
- [ ] Обработка ошибок сети

### Производительность
- [ ] Задержка воспроизведения < 2 секунд
- [ ] Поддержка потоков до 1080p@30fps
- [ ] Использование памяти < 100MB на поток
- [ ] CPU использование < 30% на поток

### Надежность
- [ ] Работа без падений в течение 24 часов
- [ ] Корректное восстановление после потери сети
- [ ] Обработка всех типов ошибок

---

## ⚠️ Риски и митигация

### Риск 1: Проблемы с FFI биндингами
**Вероятность:** Средняя
**Влияние:** Высокое
**Митигация:**
- Тщательное тестирование на всех платформах
- Использование существующих примеров cinterop
- Консультации с сообществом Kotlin/Native

### Риск 2: Сложность реализации RTP дефрагментации
**Вероятность:** Высокая
**Влияние:** Среднее
**Митигация:**
- Использование существующих библиотек (libavformat)
- Пошаговая реализация с тестированием на каждом этапе
- Документация RFC 6184 (RTP Payload Format for H.264)

### Риск 3: Проблемы с производительностью
**Вероятность:** Средняя
**Влияние:** Среднее
**Митигация:**
- Профилирование на ранних этапах
- Оптимизация критических участков
- Использование многопоточности

---

## 📅 Оценка времени

| Этап | Оценка времени | Приоритет |
|------|----------------|-----------|
| 1. Настройка FFI биндингов | 3-5 дней | Критический |
| 2. Завершение RTSP протокола | 5-7 дней | Критический |
| 3. Реализация RTP/RTCP | 7-10 дней | Критический |
| 4. Интеграция декодирования | 10-14 дней | Критический |
| 5. Улучшение Kotlin обертки | 3-5 дней | Высокий |
| 6. Тестирование и отладка | 5-7 дней | Высокий |
| **ИТОГО** | **33-48 дней** | |

**Оптимистичная оценка:** 4-5 недель
**Реалистичная оценка:** 6-7 недель
**Пессимистичная оценка:** 8-9 недель

---

## 🚀 Следующие шаги

1. **Немедленно:**
   - Начать с Этапа 1 (Настройка FFI биндингов)
   - Создать тестовую среду с реальной IP-камерой
   - Настроить CI/CD для автоматического тестирования

2. **В течение недели:**
   - Завершить Этап 1
   - Начать Этап 2 (Завершение RTSP протокола)

3. **В течение месяца:**
   - Завершить Этапы 1-3
   - Начать Этап 4 (Интеграция декодирования)

---

## 📚 Полезные ресурсы

### Документация
- [RFC 2326 - RTSP Protocol](https://tools.ietf.org/html/rfc2326)
- [RFC 3550 - RTP Protocol](https://tools.ietf.org/html/rfc3550)
- [RFC 6184 - RTP Payload Format for H.264](https://tools.ietf.org/html/rfc6184)
- [RFC 2617 - HTTP Digest Authentication](https://tools.ietf.org/html/rfc2617)

### Библиотеки
- [FFmpeg Documentation](https://ffmpeg.org/documentation.html)
- [Live555 Media Server](http://www.live555.com/) (для тестирования)

### Инструменты
- Wireshark (для анализа RTSP/RTP трафика)
- VLC (для тестирования RTSP потоков)
- RTSP Test Server (для локального тестирования)

---

## 🏗️ Архитектура RTSP клиента

### Общая архитектура

```
┌─────────────────────────────────────────────────────────────┐
│                    Kotlin Application Layer                  │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              RtspClient (High-level API)             │   │
│  │  - StateFlow<RtspClientStatus>                       │   │
│  │  - SharedFlow<RtspFrame> (video/audio)               │   │
│  │  - Coroutines для async операций                      │   │
│  └──────────────────┬───────────────────────────────────┘   │
└─────────────────────┼───────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────┐
│              Platform-specific FFI Layer                    │
│  ┌──────────────────┴──────────────────┐                   │
│  │  NativeRtspClient (expect/actual)    │                   │
│  │  - Android: JNI                     │                   │
│  │  - iOS/Desktop: cinterop             │                   │
│  └──────────────────┬──────────────────┘                   │
└─────────────────────┼───────────────────────────────────────┘
                      │
┌─────────────────────┼───────────────────────────────────────┐
│              Native C++ Library Layer                       │
│  ┌──────────────────┴──────────────────┐                   │
│  │         RTSP Client Core              │                   │
│  │  ┌──────────────────────────────┐    │                   │
│  │  │  RTSP Protocol Handler       │    │                   │
│  │  │  - DESCRIBE, SETUP, PLAY     │    │                   │
│  │  │  - PAUSE, TEARDOWN           │    │                   │
│  │  │  - Authentication (Basic/Digest) │    │                   │
│  │  └──────────┬───────────────────┘    │                   │
│  │  ┌──────────┴───────────────────┐    │                   │
│  │  │  RTP/RTCP Handler            │    │                   │
│  │  │  - RTP packet parsing        │    │                   │
│  │  │  - H.264/H.265 defragmentation│   │                   │
│  │  │  - RTCP statistics           │    │                   │
│  │  └──────────┬───────────────────┘    │                   │
│  │  ┌──────────┴───────────────────┐    │                   │
│  │  │  Video/Audio Decoder (FFmpeg) │    │                   │
│  │  │  - H.264/H.265 decoding       │    │                   │
│  │  │  - AAC/PCM decoding           │    │                   │
│  │  └──────────┬───────────────────┘    │                   │
│  │  ┌──────────┴───────────────────┐    │                   │
│  │  │  Network Layer                │    │                   │
│  │  │  - TCP/UDP sockets            │    │                   │
│  │  │  - Thread management         │    │                   │
│  │  └───────────────────────────────┘    │                   │
│  └───────────────────────────────────────┘                   │
└─────────────────────────────────────────────────────────────┘
```

### Поток данных

```
RTSP Server
    │
    │ RTSP (TCP)
    ▼
┌─────────────────┐
│ RTSP Protocol   │ ──► Parse SDP ──► Extract stream info
│ Handler         │ ──► Setup streams ──► Get RTP ports
└────────┬────────┘
         │
         │ RTP (UDP/TCP)
         ▼
┌─────────────────┐
│ RTP Handler     │ ──► Parse RTP packets ──► Defragment H.264
│                 │ ──► Extract NAL units ──► Buffer frames
└────────┬────────┘
         │
         │ NAL Units
         ▼
┌─────────────────┐
│ FFmpeg Decoder  │ ──► Decode H.264 ──► YUV frames
│                 │ ──► Convert format ──► RGB frames
└────────┬────────┘
         │
         │ Decoded Frames
         ▼
┌─────────────────┐
│ Kotlin Callback │ ──► RtspFrame ──► Flow<RtspFrame>
│                 │ ──► UI Update
└─────────────────┘
```

---

## 📖 Детальные примеры кода

### Пример 1: Полная реализация Digest Authentication

```cpp
// Структура для Digest параметров
struct DigestAuthParams {
    std::string realm;
    std::string nonce;
    std::string algorithm;  // "MD5" или "MD5-sess"
    std::string qop;        // "auth" или "auth-int"
    std::string opaque;
    std::string nonceCount; // Счетчик для qop
};

// Парсинг WWW-Authenticate заголовка
static bool parse_www_authenticate(const std::string& header, DigestAuthParams& params) {
    // Формат: Digest realm="...", nonce="...", algorithm=MD5, qop="auth"
    std::regex realmRegex(R"(realm="([^"]+)")");
    std::regex nonceRegex(R"(nonce="([^"]+)")");
    std::regex algorithmRegex(R"(algorithm=([^,\s]+))");
    std::regex qopRegex(R"(qop="([^"]+)")");
    std::regex opaqueRegex(R"(opaque="([^"]+)")");

    std::smatch match;
    if (std::regex_search(header, match, realmRegex)) {
        params.realm = match[1].str();
    }
    if (std::regex_search(header, match, nonceRegex)) {
        params.nonce = match[1].str();
    }
    if (std::regex_search(header, match, algorithmRegex)) {
        params.algorithm = match[1].str();
    }
    if (std::regex_search(header, match, qopRegex)) {
        params.qop = match[1].str();
    }
    if (std::regex_search(header, match, opaqueRegex)) {
        params.opaque = match[1].str();
    }

    return !params.realm.empty() && !params.nonce.empty();
}

// MD5 хеширование
#include <openssl/md5.h>
static std::string md5_hash(const std::string& input) {
    unsigned char digest[MD5_DIGEST_LENGTH];
    MD5((unsigned char*)input.c_str(), input.length(), digest);

    char hex[MD5_DIGEST_LENGTH * 2 + 1];
    for (int i = 0; i < MD5_DIGEST_LENGTH; i++) {
        sprintf(hex + i * 2, "%02x", digest[i]);
    }
    return std::string(hex);
}

// Генерация Digest ответа
static std::string generate_digest_auth(
    const std::string& method,
    const std::string& uri,
    const std::string& username,
    const std::string& password,
    const DigestAuthParams& params,
    const std::string& nonceCount = "00000001",
    const std::string& cnonce = ""
) {
    // HA1 = MD5(username:realm:password)
    std::string ha1_input = username + ":" + params.realm + ":" + password;
    std::string ha1 = md5_hash(ha1_input);

    // Если algorithm = "MD5-sess", то HA1 = MD5(HA1:nonce:cnonce)
    if (params.algorithm == "MD5-sess") {
        std::string cnonce_val = cnonce.empty() ? generate_cnonce() : cnonce;
        ha1 = md5_hash(ha1 + ":" + params.nonce + ":" + cnonce_val);
    }

    // HA2 = MD5(method:uri) или MD5(method:uri:MD5(entity-body))
    std::string ha2_input = method + ":" + uri;
    if (params.qop == "auth-int") {
        // Для auth-int нужно добавить MD5(entity-body)
        // В RTSP обычно нет entity-body, поэтому просто method:uri
    }
    std::string ha2 = md5_hash(ha2_input);

    // Response = MD5(HA1:nonce:nonceCount:cnonce:qop:HA2)
    std::string response_input;
    if (!params.qop.empty()) {
        response_input = ha1 + ":" + params.nonce + ":" + nonceCount + ":" +
                        (cnonce.empty() ? generate_cnonce() : cnonce) + ":" +
                        params.qop + ":" + ha2;
    } else {
        response_input = ha1 + ":" + params.nonce + ":" + ha2;
    }
    std::string response = md5_hash(response_input);

    // Формирование Authorization заголовка
    std::ostringstream auth;
    auth << "Digest username=\"" << username << "\", "
         << "realm=\"" << params.realm << "\", "
         << "nonce=\"" << params.nonce << "\", "
         << "uri=\"" << uri << "\", "
         << "response=\"" << response << "\"";

    if (!params.algorithm.empty()) {
        auth << ", algorithm=" << params.algorithm;
    }
    if (!params.qop.empty()) {
        auth << ", qop=" << params.qop
             << ", nc=" << nonceCount
             << ", cnonce=\"" << (cnonce.empty() ? generate_cnonce() : cnonce) << "\"";
    }
    if (!params.opaque.empty()) {
        auth << ", opaque=\"" << params.opaque << "\"";
    }

    return auth.str();
}

// Генерация случайного cnonce
static std::string generate_cnonce() {
    char cnonce[16];
    for (int i = 0; i < 16; i++) {
        cnonce[i] = "0123456789abcdef"[rand() % 16];
    }
    return std::string(cnonce, 16);
}
```

### Пример 2: Дефрагментация H.264 FU-A

```cpp
// Структура для NAL unit
struct NALUnit {
    uint8_t type;
    bool forbidden;
    uint8_t nri;  // NAL Reference IDC
    std::vector<uint8_t> data;
};

// Парсинг NAL unit header из RTP payload
static bool parse_nal_header(const uint8_t* payload, int size, NALUnit& nal) {
    if (size < 1) return false;

    uint8_t firstByte = payload[0];
    nal.forbidden = (firstByte >> 7) & 0x01;
    nal.nri = (firstByte >> 5) & 0x03;
    nal.type = firstByte & 0x1F;

    return true;
}

// Дефрагментация FU-A (Fragmentation Unit Type A)
static bool defragment_fu_a(
    const std::vector<RTPPacket>& packets,
    std::vector<NALUnit>& nalUnits
) {
    if (packets.empty()) return false;

    // Первый пакет содержит FU indicator и FU header
    const auto& firstPacket = packets[0];
    if (firstPacket.payload.size() < 2) return false;

    uint8_t fu_indicator = firstPacket.payload[0];
    uint8_t fu_header = firstPacket.payload[1];

    bool start = (fu_header >> 7) & 0x01;
    bool end = (fu_header >> 6) & 0x01;
    uint8_t nal_type = fu_header & 0x1F;

    if (!start) {
        // Ожидаем начало фрагмента
        return false;
    }

    // Собираем полный NAL unit
    NALUnit nal;
    nal.type = nal_type;
    nal.forbidden = (fu_indicator >> 7) & 0x01;
    nal.nri = (fu_indicator >> 5) & 0x03;

    // Добавляем данные из всех пакетов
    for (const auto& packet : packets) {
        if (packet.payload.size() < 2) continue;

        // Пропускаем FU indicator и FU header
        nal.data.insert(nal.data.end(),
                       packet.payload.begin() + 2,
                       packet.payload.end());
    }

    // Проверяем, что последний пакет имеет end flag
    const auto& lastPacket = packets.back();
    if (lastPacket.payload.size() >= 2) {
        uint8_t last_fu_header = lastPacket.payload[1];
        bool last_end = (last_fu_header >> 6) & 0x01;
        if (!last_end) {
            // Фрагмент не завершен
            return false;
        }
    }

    nalUnits.push_back(nal);
    return true;
}

// Буферизация и сборка фрагментированных NAL units
class NALFragmentBuffer {
private:
    struct FragmentInfo {
        uint16_t sequence;
        uint32_t timestamp;
        uint8_t nalType;
        bool start;
        bool end;
        std::vector<uint8_t> data;
    };

    std::map<uint32_t, std::map<uint16_t, FragmentInfo>> buffers; // timestamp -> sequence -> fragment

public:
    void add_packet(const RTPPacket& packet) {
        if (packet.payload.size() < 2) return;

        uint8_t fu_indicator = packet.payload[0];
        uint8_t fu_header = packet.payload[1];

        bool start = (fu_header >> 7) & 0x01;
        bool end = (fu_header >> 6) & 0x01;
        uint8_t nal_type = fu_header & 0x1F;

        FragmentInfo frag;
        frag.sequence = packet.sequence;
        frag.timestamp = packet.timestamp;
        frag.nalType = nal_type;
        frag.start = start;
        frag.end = end;
        frag.data.assign(packet.payload.begin() + 2, packet.payload.end());

        buffers[packet.timestamp][packet.sequence] = frag;
    }

    bool get_complete_nal(uint32_t timestamp, NALUnit& nal) {
        auto& seqMap = buffers[timestamp];
        if (seqMap.empty()) return false;

        // Находим первый пакет (start = true)
        auto startIt = std::find_if(seqMap.begin(), seqMap.end(),
            [](const auto& pair) { return pair.second.start; });

        if (startIt == seqMap.end()) return false;

        // Собираем все пакеты до end
        nal.type = startIt->second.nalType;
        nal.data.clear();

        for (auto it = startIt; it != seqMap.end(); ++it) {
            nal.data.insert(nal.data.end(),
                           it->second.data.begin(),
                           it->second.data.end());

            if (it->second.end) {
                // Удаляем собранные пакеты
                seqMap.erase(seqMap.begin(), std::next(it));
                if (seqMap.empty()) {
                    buffers.erase(timestamp);
                }
                return true;
            }
        }

        return false; // Фрагмент не завершен
    }
};
```

### Пример 3: Инициализация FFmpeg декодера

```cpp
#ifdef ENABLE_FFMPEG
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/avutil.h>

// Инициализация H.264 декодера
AVCodecContext* init_h264_decoder(const std::string& sps, const std::string& pps) {
    const AVCodec* codec = avcodec_find_decoder(AV_CODEC_ID_H264);
    if (!codec) {
        return nullptr;
    }

    AVCodecContext* codecContext = avcodec_alloc_context3(codec);
    if (!codecContext) {
        return nullptr;
    }

    // Парсинг SPS для получения параметров
    // SPS содержит информацию о разрешении, FPS и т.д.
    uint8_t* spsData = (uint8_t*)sps.data();
    int spsSize = sps.size();

    // Создание extradata для H.264
    // Формат: [0x00 0x00 0x00 0x01] SPS [0x00 0x00 0x00 0x01] PPS
    int extradataSize = 8 + spsSize + 4 + pps.size();
    uint8_t* extradata = (uint8_t*)av_malloc(extradataSize + AV_INPUT_BUFFER_PADDING_SIZE);
    if (!extradata) {
        avcodec_free_context(&codecContext);
        return nullptr;
    }

    int offset = 0;
    // SPS
    extradata[offset++] = 0x00;
    extradata[offset++] = 0x00;
    extradata[offset++] = 0x00;
    extradata[offset++] = 0x01;
    memcpy(extradata + offset, spsData, spsSize);
    offset += spsSize;

    // PPS
    extradata[offset++] = 0x00;
    extradata[offset++] = 0x00;
    extradata[offset++] = 0x00;
    extradata[offset++] = 0x01;
    memcpy(extradata + offset, (uint8_t*)pps.data(), pps.size());
    offset += pps.size();

    codecContext->extradata = extradata;
    codecContext->extradata_size = extradataSize;

    // Открытие декодера
    if (avcodec_open2(codecContext, codec, nullptr) < 0) {
        av_free(extradata);
        avcodec_free_context(&codecContext);
        return nullptr;
    }

    return codecContext;
}

// Декодирование NAL units в кадр
bool decode_h264_frame(
    AVCodecContext* codecContext,
    const std::vector<NALUnit>& nalUnits,
    AVFrame* outputFrame
) {
    AVPacket* packet = av_packet_alloc();
    if (!packet) return false;

    // Создание AVPacket из NAL units
    // Нужно добавить start codes (0x00 0x00 0x00 0x01) перед каждым NAL
    std::vector<uint8_t> packetData;
    for (const auto& nal : nalUnits) {
        packetData.push_back(0x00);
        packetData.push_back(0x00);
        packetData.push_back(0x00);
        packetData.push_back(0x01);

        // Добавляем NAL header
        uint8_t nalHeader = (nal.forbidden << 7) | (nal.nri << 5) | nal.type;
        packetData.push_back(nalHeader);

        // Добавляем данные
        packetData.insert(packetData.end(), nal.data.begin(), nal.data.end());
    }

    packet->data = packetData.data();
    packet->size = packetData.size();

    // Отправка пакета в декодер
    int ret = avcodec_send_packet(codecContext, packet);
    if (ret < 0) {
        av_packet_free(&packet);
        return false;
    }

    // Получение декодированного кадра
    ret = avcodec_receive_frame(codecContext, outputFrame);
    av_packet_free(&packet);

    return ret >= 0;
}
#endif
```

### Пример 4: Использование в Kotlin

```kotlin
// Создание RTSP клиента
val config = RtspClientConfig(
    url = "rtsp://192.168.1.100:554/stream1",
    username = "admin",
    password = "password123",
    timeoutMillis = 10000,
    reconnectConfig = RtspReconnectConfig(
        enabled = true,
        maxRetries = 5,
        initialDelayMs = 1000,
        maxDelayMs = 30000,
        backoffMultiplier = 2.0f
    )
)

val rtspClient = RtspClient(config)

// Подписка на статус
rtspClient.getStatus().collect { status ->
    when (status) {
        RtspClientStatus.CONNECTING -> println("Подключение...")
        RtspClientStatus.CONNECTED -> println("Подключено")
        RtspClientStatus.PLAYING -> println("Воспроизведение")
        RtspClientStatus.ERROR -> println("Ошибка")
        RtspClientStatus.DISCONNECTED -> println("Отключено")
    }
}

// Подписка на видеокадры
lifecycleScope.launch {
    rtspClient.getVideoFrames().collect { frame ->
        // Обработка кадра
        updateVideoView(frame.data, frame.width, frame.height)
    }
}

// Подключение и воспроизведение
lifecycleScope.launch {
    try {
        rtspClient.connect()
        delay(1000) // Ждем подключения
        rtspClient.play()
    } catch (e: Exception) {
        println("Ошибка подключения: ${e.message}")
    }
}

// Остановка и отключение
lifecycleScope.launch {
    rtspClient.stop()
    rtspClient.disconnect()
}
```

---

## 🔄 Последовательности вызовов

### Последовательность подключения и воспроизведения

```
Client                          RTSP Server
  │                                  │
  │─── OPTIONS rtsp://... ──────────>│
  │<── 200 OK ───────────────────────│
  │                                  │
  │─── DESCRIBE rtsp://... ─────────>│
  │    Authorization: Basic ...     │
  │<── 200 OK ───────────────────────│
  │    Content-Type: application/sdp │
  │    [SDP content]                │
  │                                  │
  │─── SETUP rtsp://.../trackID=0 ──>│
  │    Transport: RTP/AVP/UDP;     │
  │         client_port=5000-5001   │
  │<── 200 OK ───────────────────────│
  │    Transport: RTP/AVP/UDP;      │
  │         server_port=3056-3057   │
  │    Session: 12345678            │
  │                                  │
  │─── SETUP rtsp://.../trackID=1 ──>│
  │    Transport: RTP/AVP/UDP;     │
  │         client_port=5002-5003   │
  │<── 200 OK ───────────────────────│
  │    Transport: RTP/AVP/UDP;      │
  │         server_port=3058-3059   │
  │    Session: 12345678            │
  │                                  │
  │─── PLAY rtsp://... ──────────────>│
  │    Session: 12345678            │
  │    Range: npt=0.000-            │
  │<── 200 OK ───────────────────────│
  │    Session: 12345678            │
  │    RTP-Info: url=...;seq=0;rtptime=0│
  │                                  │
  │<── RTP Packets (UDP) ────────────│
  │    [Video frames]               │
  │<── RTP Packets (UDP) ────────────│
  │    [Audio frames]               │
  │                                  │
  │─── PAUSE rtsp://... ────────────>│
  │    Session: 12345678            │
  │<── 200 OK ───────────────────────│
  │                                  │
  │─── TEARDOWN rtsp://... ─────────>│
  │    Session: 12345678            │
  │<── 200 OK ───────────────────────│
```

### Последовательность Digest Authentication

```
Client                          RTSP Server
  │                                  │
  │─── DESCRIBE rtsp://... ─────────>│
  │    (без авторизации)            │
  │<── 401 Unauthorized ────────────│
  │    WWW-Authenticate: Digest     │
  │         realm="Camera",         │
  │         nonce="abc123",         │
  │         algorithm=MD5,          │
  │         qop="auth"              │
  │                                  │
  │ [Вычисление Digest response]   │
  │                                  │
  │─── DESCRIBE rtsp://... ─────────>│
  │    Authorization: Digest        │
  │         username="admin",       │
  │         realm="Camera",         │
  │         nonce="abc123",         │
  │         uri="rtsp://...",      │
  │         response="xyz789",     │
  │         algorithm=MD5,           │
  │         qop=auth,               │
  │         nc=00000001,            │
  │         cnonce="def456"         │
  │<── 200 OK ───────────────────────│
  │    [SDP content]                │
```

---

## 🔍 Troubleshooting

### Проблема 1: FFI биндинги не генерируются

**Симптомы:**
- Ошибка компиляции: "Unresolved reference: rtsp_client_create"
- cinterop не находит заголовки

**Решения:**
1. Проверить пути в `rtsp_client.def`:
   ```kotlin
   compilerOpts = -I../../../../native/video-processing/include
   linkerOpts = -L../../../../native/video-processing/lib -lvideo_processing
   ```

2. Убедиться, что библиотека собрана:
   ```bash
   cd native/video-processing
   mkdir build && cd build
   cmake ..
   cmake --build .
   ```

3. Проверить, что библиотека находится в правильной директории:
   - Linux: `libvideo_processing.so`
   - macOS: `libvideo_processing.dylib`
   - Windows: `video_processing.dll`

### Проблема 2: Ошибка подключения к RTSP серверу

**Симптомы:**
- Статус остается `CONNECTING`
- Ошибка "Failed to connect to RTSP server"

**Решения:**
1. Проверить доступность сервера:
   ```bash
   telnet <server_ip> 554
   ```

2. Проверить URL формат:
   ```
   rtsp://username:password@host:port/path
   ```

3. Проверить firewall и сетевые настройки

4. Включить детальное логирование:
   ```kotlin
   val config = RtspClientConfig(
       url = "...",
       // ... другие параметры
   )
   // Добавить логирование в NativeRtspClient
   ```

### Проблема 3: Кадры не приходят

**Симптомы:**
- Подключение успешно, но `getVideoFrames()` не получает данные
- Callback не вызывается

**Решения:**
1. Проверить, что `play()` вызван после `connect()`

2. Проверить RTP порты:
   - Убедиться, что порты не заблокированы firewall
   - Проверить, что порты доступны для UDP

3. Проверить декодирование:
   - Убедиться, что FFmpeg правильно настроен
   - Проверить логи декодера

4. Использовать Wireshark для анализа трафика:
   ```
   Filter: rtp
   ```

### Проблема 4: Digest Authentication не работает

**Симптомы:**
- Получаем 401 Unauthorized даже с правильными credentials
- Ошибка "Authentication failed"

**Решения:**
1. Проверить парсинг `WWW-Authenticate` заголовка

2. Проверить вычисление MD5 хеша:
   ```cpp
   // Убедиться, что используется правильный алгоритм
   // Проверить формат nonce и cnonce
   ```

3. Проверить qop параметр:
   - Некоторые серверы требуют qop="auth"
   - Убедиться, что nc и cnonce правильно формируются

4. Fallback на Basic Authentication:
   ```cpp
   if (digest_failed) {
       try_basic_auth();
   }
   ```

### Проблема 5: Высокое использование памяти

**Симптомы:**
- Приложение потребляет много памяти
- Утечки памяти при длительной работе

**Решения:**
1. Ограничить размер буферов:
   ```cpp
   const size_t MAX_BUFFER_SIZE = 10 * 1024 * 1024; // 10MB
   ```

2. Освобождать кадры после использования:
   ```kotlin
   rtspClient.getVideoFrames().collect { frame ->
       try {
           processFrame(frame)
       } finally {
           // Кадр автоматически освобождается
       }
   }
   ```

3. Использовать ограниченные Flow буферы:
   ```kotlin
   private val videoFrameFlow = MutableSharedFlow<RtspFrame>(
       extraBufferCapacity = 5, // Ограничить буфер
       onBufferOverflow = BufferOverflow.DROP_OLDEST
   )
   ```

---

## 📋 Checklist для каждого этапа

### Этап 1: Настройка FFI биндингов
- [ ] cinterop определения проверены и исправлены
- [ ] Библиотека собирается для всех платформ
- [ ] Тесты FFI биндингов проходят
- [ ] Документация обновлена

### Этап 2: Завершение RTSP протокола
- [ ] SDP парсинг работает для всех типов камер
- [ ] Digest Authentication реализована и протестирована
- [ ] TCP транспорт для RTP работает
- [ ] Все RTSP методы (DESCRIBE, SETUP, PLAY, PAUSE, TEARDOWN) работают

### Этап 3: Реализация RTP/RTCP
- [ ] RTP пакеты корректно парсятся
- [ ] H.264/H.265 дефрагментация работает
- [ ] RTCP статистика собирается
- [ ] Буферизация и восстановление порядка пакетов работает

### Этап 4: Интеграция декодирования
- [ ] FFmpeg настроен для всех платформ
- [ ] H.264/H.265 декодирование работает
- [ ] Аудио декодирование работает
- [ ] Декодированные кадры передаются в Kotlin

### Этап 5: Улучшение Kotlin обертки
- [ ] Все платформенные реализации работают
- [ ] Обработка ошибок улучшена
- [ ] Логирование добавлено
- [ ] Документация API обновлена

### Этап 6: Тестирование
- [ ] Unit тесты написаны и проходят
- [ ] Integration тесты написаны и проходят
- [ ] Тестирование на реальных камерах завершено
- [ ] Производительность соответствует требованиям

---

## 🧪 Тестовые сценарии

### Сценарий 1: Базовое подключение
```
1. Создать RtspClient с валидным URL
2. Вызвать connect()
3. Проверить, что статус меняется на CONNECTED
4. Вызвать play()
5. Проверить, что статус меняется на PLAYING
6. Проверить, что кадры приходят
```

### Сценарий 2: Аутентификация
```
1. Создать RtspClient с username/password
2. Вызвать connect()
3. Проверить, что используется Basic или Digest Authentication
4. Проверить успешное подключение
```

### Сценарий 3: Переподключение
```
1. Создать RtspClient с включенным переподключением
2. Подключиться и начать воспроизведение
3. Симулировать потерю сети (отключить WiFi)
4. Проверить, что клиент пытается переподключиться
5. Восстановить сеть
6. Проверить, что подключение восстановлено
```

### Сценарий 4: Множественные потоки
```
1. Подключиться к камере с видео и аудио
2. Проверить, что getStreamCount() возвращает 2
3. Проверить, что видеокадры приходят
4. Проверить, что аудиокадры приходят
```

### Сценарий 5: Обработка ошибок
```
1. Попытаться подключиться с неверным URL
2. Проверить, что статус меняется на ERROR
3. Проверить, что callback вызывается с сообщением об ошибке
4. Попытаться подключиться с неверными credentials
5. Проверить обработку ошибки аутентификации
```

---

## ⚙️ Конфигурационные параметры

### RtspClientConfig

```kotlin
data class RtspClientConfig(
    // Обязательные параметры
    val url: String,

    // Аутентификация
    val username: String? = null,
    val password: String? = null,

    // Таймауты
    val timeoutMillis: Long = 10000, // 10 секунд

    // Буферы
    val bufferSize: Int = 1024 * 1024, // 1MB

    // Потоки
    val enableAudio: Boolean = true,
    val enableVideo: Boolean = true,
    val enableMetadata: Boolean = false,

    // Переподключение
    val reconnectConfig: RtspReconnectConfig = RtspReconnectConfig(),

    // Дополнительные параметры
    val transport: RtspTransport = RtspTransport.UDP, // UDP или TCP
    val videoCodec: VideoCodec? = null, // Автоопределение если null
    val audioCodec: AudioCodec? = null, // Автоопределение если null
    val maxFrameRate: Int? = null, // Ограничение FPS
    val maxResolution: Resolution? = null // Ограничение разрешения
)

enum class RtspTransport {
    UDP,  // По умолчанию
    TCP   // Fallback если UDP не работает
}

enum class VideoCodec {
    H264,
    H265
}

enum class AudioCodec {
    AAC,
    PCM,
    MP3
}
```

---

---

## 📋 Упорядоченный план реализации (приоритизированный)

### 🔴 КРИТИЧЕСКИЙ ПРИОРИТЕТ — Этап 1: Настройка FFI биндингов (3-5 дней)

#### 1.1 Проверка и исправление cinterop определений ✅ ЗАВЕРШЕНО
**Файл:** `core/network/src/nativeInterop/cinterop/rtsp_client.def`

**Последовательность:**
1. ✅ Проверить корректность всех определений типов
2. ✅ Добавить недостающие типы (RTSPStatus, RTSPStreamType, RTSPReconnectParams)
3. ✅ Настроить правильные пути к заголовкам для всех платформ
4. ✅ Настроить правильные пути к библиотекам для всех платформ (настроено в build.gradle.kts)
5. ✅ Добавить определения callback функций (RTSPFrameCallback, RTSPStatusCallback)

**Выполнено:** 26 January 2026
**Результат:** Файл `rtsp_client.def` обновлен со всеми необходимыми определениями типов, enum'ов, структур и callback функций.

#### 1.2 Настройка сборки нативных библиотек ✅ ЗАВЕРШЕНО
**Файлы:** `native/video-processing/CMakeLists.txt`, `core/network/build.gradle.kts`

**Последовательность:**
1. ✅ Убедиться, что CMake правильно собирает библиотеку для всех платформ (проверено - CMakeLists.txt настроен)
2. ✅ Настроить правильные пути к скомпилированным библиотекам (настроено в build.gradle.kts для всех платформ)
3. ✅ Добавить зависимости на нативную библиотеку в Gradle (настроено через cinterop)
4. ✅ Настроить автоматическую сборку нативной библиотеки перед компиляцией Kotlin (добавлены задачи в build.gradle.kts)

**Выполнено:** 26 January 2026
**Результат:**
- Добавлены задачи `buildNativeVideoProcessingForCurrentPlatform` и `checkNativeLibrary` в `core/network/build.gradle.kts`
- Настроена автоматическая сборка нативной библиотеки перед компиляцией Kotlin
- Проверка существования библиотеки перед компиляцией

#### 1.3 Тестирование FFI биндингов ✅ ЗАВЕРШЕНО
**Файл:** `core/network/src/commonTest/kotlin/com/company/ipcamera/core/network/RtspClientFFITest.kt`

**Последовательность:**
1. ✅ Создать unit тесты для проверки создания/уничтожения клиента
2. ✅ Проверить корректность конвертации типов
3. ✅ Проверить работу callbacks
4. ⏸️ Протестировать на всех платформах (Android, iOS, Desktop) - требует ручного запуска

**Выполнено:** 26 January 2026
**Результат:**
- Создан файл `RtspClientFFITest.kt` с 20+ unit тестами для FFI биндингов
- Тесты покрывают: создание/уничтожение, конвертацию типов, работу callbacks, базовые операции
- Тесты написаны с учетом отсутствия нативной библиотеки (проверка handle != 0L)

**Примечание:** Тестирование на всех платформах требует:
- Скомпилированных нативных библиотек для каждой платформы
- Запуска тестов на реальных устройствах/эмуляторах

---

### 🔴 КРИТИЧЕСКИЙ ПРИОРИТЕТ — Этап 2: Завершение RTSP протокола (5-7 дней)

#### 2.1 Улучшение парсинга SDP ✅ ЗАВЕРШЕНО
**Файл:** `native/video-processing/src/rtsp_client.cpp` (функция `parse_sdp`)

**Последовательность:**
1. ✅ Улучшить парсинг SDP для поддержки всех стандартных атрибутов
   - Добавлен парсинг v= (версия), o= (origin), s= (session name)
   - Улучшена обработка session-level атрибутов
2. ✅ Добавить поддержку `a=fmtp` для параметров кодеков
   - Полный парсинг параметров для H.264 (sprop-parameter-sets, profile-level-id)
   - Поддержка H.265 (sprop-sps, sprop-pps, sprop-vps)
   - Поддержка AAC (config параметр)
   - Сохранение всех параметров fmtp
3. ✅ Добавить поддержку `a=control` для track ID
   - Парсинг trackID= из control URL
   - Поддержка альтернативного формата track1, track2
   - Извлечение и сохранение track ID
4. ✅ Добавить поддержку `a=rtpmap` для всех кодеков (H.264, H.265, AAC, G.711)
   - Нормализация названий кодеков (H264, H265, AAC, PCMU, PCMA, MP3)
   - Парсинг clockRate и channels для аудио
   - Обработка различных форматов rtpmap
5. ✅ Добавить поддержку `a=range` для временных диапазонов
   - Парсинг npt (Normal Play Time)
   - Сохранение range для использования в PLAY запросах
6. ✅ Реализовать обработку ошибок парсинга
   - Try-catch блоки для всех операций парсинга
   - Валидация payload type (0-127)
   - Проверка наличия session перед возвратом результата
   - Пропуск невалидных потоков вместо падения

**Выполнено:** 26 January 2026
**Результат:**
- Расширена структура RTPStream с дополнительными полями (sps, pps, vps, profileLevelId, fmtpParams, range, trackId, channels)
- Улучшен парсинг всех стандартных SDP атрибутов
- Добавлена поддержка всех основных кодеков (H.264, H.265, AAC, G.711, MP3)
- Добавлена обработка ошибок с graceful degradation

#### 2.2 Реализация Digest Authentication ✅ ЗАВЕРШЕНО
**Файл:** `native/video-processing/src/rtsp_client.cpp`

**Последовательность:**
1. ✅ Добавить парсинг заголовка `WWW-Authenticate` из ответа 401
   - Функция parse_rtsp_response уже извлекает WWW-Authenticate
   - Функция parse_www_authenticate парсит все параметры Digest
2. ✅ Реализовать алгоритм Digest Authentication (MD5)
   - Функция md5_hash реализована через OpenSSL
   - Функция generate_digest_auth генерирует правильный response
3. ✅ Добавить генерацию `Authorization` заголовка для Digest
   - Полная поддержка всех параметров Digest
4. ✅ Поддержка различных алгоритмов (MD5, MD5-sess)
   - MD5: HA1 = MD5(username:realm:password)
   - MD5-sess: HA1 = MD5(HA1:nonce:cnonce)
5. ✅ Поддержка qop (quality of protection)
   - qop="auth": стандартный режим
   - qop="auth-int": с проверкой целостности (поддержка добавлена)
6. ✅ Интеграция с существующей Basic Authentication
   - Автоматическое переключение с Basic на Digest при получении 401
   - Fallback на Basic, если Digest не поддерживается

**Выполнено:** 26 January 2026
**Результат:**
- Полная поддержка Digest Authentication с автоматическим переключением
- Поддержка MD5 и MD5-sess алгоритмов
- Поддержка qop="auth" и qop="auth-int"
- Интеграция в DESCRIBE запрос с обработкой 401 ответа

#### 2.3 Улучшение обработки RTSP ответов
**Файл:** `native/video-processing/src/rtsp_client.cpp` (функция `parse_rtsp_response`)

**Последовательность:**
1. ⏳ Улучшить парсинг RTSP ответов
2. ⏳ Добавить обработку всех стандартных заголовков
3. ⏳ Добавить обработку ошибок (4xx, 5xx)
4. ⏳ Добавить поддержку `Transport` заголовка в SETUP ответе
5. ⏳ Добавить поддержку `Range` заголовка в PLAY ответе
6. ⏳ Добавить поддержку `RTP-Info` заголовка

#### 2.4 Реализация TCP транспорта для RTP
**Файл:** `native/video-processing/src/rtsp_client.cpp`

**Последовательность:**
1. ⏳ Добавить поддержку `RTP/AVP/TCP` транспорта
2. ⏳ Реализовать инкапсуляцию RTP в RTSP (interleaved binary data)
3. ⏳ Добавить обработку `interleaved` параметра в Transport заголовке
4. ⏳ Реализовать чтение бинарных данных из RTSP сокета
5. ⏳ Добавить выбор транспорта (UDP по умолчанию, TCP как fallback)

---

### 🔴 КРИТИЧЕСКИЙ ПРИОРИТЕТ — Этап 3: Реализация RTP/RTCP обработки (7-10 дней)

#### 3.1 Улучшение парсинга RTP пакетов
**Файл:** `native/video-processing/src/rtsp_client.cpp` (функция `process_rtp_packet`)

**Последовательность:**
1. ⏳ Улучшить парсинг RTP заголовка (версия, padding, extension, CSRC)
2. ⏳ Добавить обработку RTP extension headers
3. ⏳ Добавить проверку sequence number для обнаружения потерь
4. ⏳ Добавить обработку marker bit
5. ⏳ Добавить поддержку различных payload types
6. ⏳ Добавить буферизацию для восстановления порядка пакетов

#### 3.2 Реализация дефрагментации H.264/H.265
**Файл:** `native/video-processing/src/rtsp_client.cpp`

**Последовательность:**
1. ⏳ Реализовать парсинг NAL units в RTP payload
2. ⏳ Добавить обработку FU-A (Fragmentation Unit) для H.264
3. ⏳ Добавить обработку FU-B для H.264
4. ⏳ Добавить обработку STAP-A (Single Time Aggregation Packet)
5. ⏳ Добавить обработку STAP-B
6. ⏳ Добавить обработку MTAP16/MTAP24
7. ⏳ Реализовать дефрагментацию для H.265 (HEVC)

#### 3.3 Реализация обработки RTCP пакетов
**Файл:** `native/video-processing/src/rtsp_client.cpp`

**Последовательность:**
1. ⏳ Реализовать парсинг RTCP пакетов (SR, RR, SDES, BYE, APP)
2. ⏳ Добавить обработку Sender Report (SR)
3. ⏳ Добавить обработку Receiver Report (RR)
4. ⏳ Реализовать отправку RTCP пакетов
5. ⏳ Добавить расчет статистики (jitter, packet loss)
6. ⏳ Интеграция с автоматическим переподключением

#### 3.4 Улучшение потока приема RTP
**Файл:** `native/video-processing/src/rtsp_client.cpp` (функция `receive_rtp_thread`)

**Последовательность:**
1. ⏳ Улучшить обработку множественных потоков
2. ⏳ Добавить правильную синхронизацию потоков
3. ⏳ Добавить обработку таймаутов
4. ⏳ Добавить обработку ошибок сети
5. ⏳ Оптимизировать использование памяти
6. ⏳ Добавить поддержку приоритетов потоков

---

### 🔴 КРИТИЧЕСКИЙ ПРИОРИТЕТ — Этап 4: Интеграция декодирования видео/аудио (10-14 дней)

#### 4.1 Настройка FFmpeg для декодирования
**Файл:** `native/video-processing/CMakeLists.txt`

**Последовательность:**
1. ⏳ Убедиться, что FFmpeg правильно линкуется
2. ⏳ Настроить FFmpeg для всех платформ (Android, iOS, Desktop)
3. ⏳ Добавить поддержку необходимых кодеков (H.264, H.265, AAC, G.711)
4. ⏳ Настроить сборку FFmpeg для Android (через NDK)
5. ⏳ Настроить сборку FFmpeg для iOS

#### 4.2 Реализация декодирования H.264/H.265
**Файл:** `native/video-processing/src/rtsp_client.cpp` (в блоке `#ifdef ENABLE_FFMPEG`)

**Последовательность:**
1. ⏳ Инициализация H.264 декодера через FFmpeg
2. ⏳ Парсинг SPS/PPS из SDP или из потока
3. ⏳ Декодирование NAL units в кадры
4. ⏳ Конвертация YUV в RGB (если необходимо)
5. ⏳ Обработка B-frames и P-frames
6. ⏳ Обработка ошибок декодирования

#### 4.3 Реализация декодирования аудио
**Файл:** `native/video-processing/src/rtsp_client.cpp`

**Последовательность:**
1. ⏳ Инициализация AAC декодера
2. ⏳ Инициализация G.711 декодера (PCM)
3. ⏳ Декодирование аудио пакетов
4. ⏳ Конвертация аудио форматов (если необходимо)
5. ⏳ Синхронизация аудио и видео

#### 4.4 Интеграция декодирования с callback'ами
**Файл:** `native/video-processing/src/rtsp_client.cpp`

**Последовательность:**
1. ⏳ Интегрировать декодирование в поток приема RTP
2. ⏳ Вызывать callback с декодированными кадрами
3. ⏳ Обработка ошибок декодирования
4. ⏳ Освобождение ресурсов декодера

---

### 🟡 ВЫСОКИЙ ПРИОРИТЕТ — Этап 5: Улучшение Kotlin обертки (3-5 дней)

#### 5.1 Исправление NativeRtspClient.native.kt
**Файл:** `core/network/src/nativeMain/kotlin/.../NativeRtspClient.native.kt`

**Последовательность:**
1. ⏳ Проверить корректность всех вызовов cinterop
2. ⏳ Исправить конвертацию типов
3. ⏳ Улучшить обработку ошибок
4. ⏳ Добавить логирование
5. ⏳ Оптимизировать работу с памятью

#### 5.2 Исправление NativeRtspClient.jvm.kt
**Файл:** `core/network/src/jvmMain/kotlin/.../NativeRtspClient.jvm.kt`

**Последовательность:**
1. ⏳ Убедиться, что JNI функции правильно вызываются
2. ⏳ Исправить загрузку библиотеки
3. ⏳ Улучшить обработку callbacks
4. ⏳ Добавить обработку ошибок

#### 5.3 Улучшение высокоуровневой обертки RtspClient
**Файл:** `core/network/src/commonMain/kotlin/.../RtspClient.kt`

**Последовательность:**
1. ⏳ Улучшить обработку состояний
2. ⏳ Добавить retry логику
3. ⏳ Улучшить обработку ошибок
4. ⏳ Добавить метрики и статистику
5. ⏳ Оптимизировать работу с Flow

---

### 🟡 ВЫСОКИЙ ПРИОРИТЕТ — Этап 6: Тестирование и отладка (5-7 дней)

#### 6.1 Unit тесты
**Файлы:** `core/network/src/commonTest/kotlin/.../RtspClientTest.kt`

**Последовательность:**
1. ⏳ Расширить существующие unit тесты
2. ⏳ Добавить тесты для всех методов
3. ⏳ Добавить тесты для обработки ошибок
4. ⏳ Добавить тесты для callbacks

#### 6.2 Integration тесты
**Файлы:** `core/network/src/androidTest/kotlin/.../RtspClientIntegrationTest.kt`

**Последовательность:**
1. ⏳ Создать тесты с реальными RTSP серверами
2. ⏳ Тестирование на различных камерах
3. ⏳ Тестирование различных кодеков
4. ⏳ Тестирование различных методов аутентификации
5. ⏳ Тестирование переподключения

#### 6.3 Отладка на реальных камерах
**Последовательность:**
1. ⏳ Тестирование на камерах различных производителей
2. ⏳ Тестирование различных форматов потоков
3. ⏳ Тестирование при плохом качестве сети
4. ⏳ Оптимизация производительности
5. ⏳ Исправление найденных багов

---

## 📊 Сводная таблица приоритетов

| Этап | Количество задач | Время | Приоритет | Зависимости |
|------|------------------|-------|-----------|-------------|
| **1. FFI биндинги** | 13 задач | 3-5 дней | 🔴 Критический | Нет |
| **2. RTSP протокол** | 24 задачи | 5-7 дней | 🔴 Критический | Этап 1 |
| **3. RTP/RTCP** | 24 задачи | 7-10 дней | 🔴 Критический | Этап 2 |
| **4. Декодирование** | 19 задач | 10-14 дней | 🔴 Критический | Этап 3 |
| **5. Kotlin обертка** | 14 задач | 3-5 дней | 🟡 Высокий | Этапы 1-4 |
| **6. Тестирование** | 14 задач | 5-7 дней | 🟡 Высокий | Этапы 1-5 |

**Всего задач:** 108
**Общее время:** 33-48 дней (6-7 недель)

**Статус выполнения:**
- ✅ = Выполнено
- ⏳ = В процессе
- ⏸️ = Ожидает

---

## 📝 Журнал выполнения

### 26 January 2026 - Этап 1.1: Настройка cinterop определений ✅

**Выполненные задачи:**
1. ✅ Проверена корректность всех определений типов в `rtsp_client.def`
2. ✅ Добавлены недостающие типы:
   - `typedef enum RTSPStatus RTSPStatus;`
   - `typedef enum RTSPStreamType RTSPStreamType;`
   - `typedef struct RTSPReconnectParams RTSPReconnectParams;`
3. ✅ Добавлены определения callback функций:
   - `typedef void (*RTSPFrameCallback)(RTSPFrame* frame, void* userData);`
   - `typedef void (*RTSPStatusCallback)(RTSPStatus status, const char* message, void* userData);`
4. ✅ Проверены пути к заголовкам и библиотекам (настроены в `build.gradle.kts` для всех платформ)

**Измененные файлы:**
- `core/network/src/nativeInterop/cinterop/rtsp_client.def` - полностью обновлен

**Следующие шаги:**
- Этап 1.2: Настройка сборки нативных библиотек ✅

### 26 January 2026 - Этап 1.2: Настройка сборки нативных библиотек ✅

**Выполненные задачи:**
1. ✅ Проверена конфигурация CMake для всех платформ (CMakeLists.txt настроен корректно)
2. ✅ Проверены пути к скомпилированным библиотекам в build.gradle.kts (настроены для всех платформ)
3. ✅ Проверены зависимости на нативную библиотеку в Gradle (настроены через cinterop)
4. ✅ Добавлена автоматическая сборка нативной библиотеки перед компиляцией Kotlin:
   - Задача `buildNativeVideoProcessingForCurrentPlatform` для сборки библиотеки
   - Задача `checkNativeLibrary` для проверки существования библиотеки
   - Автоматическая зависимость компиляции Kotlin от сборки нативной библиотеки

**Измененные файлы:**
- `core/network/build.gradle.kts` - добавлены задачи для автоматической сборки

**Следующие шаги:**
- Этап 1.3: Тестирование FFI биндингов ✅

### 26 January 2026 - Этап 1.3: Тестирование FFI биндингов ✅

**Выполненные задачи:**
1. ✅ Создан файл `RtspClientFFITest.kt` с комплексными unit тестами
2. ✅ Добавлены тесты для проверки создания/уничтожения клиента
3. ✅ Добавлены тесты для проверки конвертации типов (статусы, типы потоков)
4. ✅ Добавлены тесты для проверки работы callbacks (frame, status)
5. ✅ Добавлены тесты для базовых операций (connect, play, stop, pause)
6. ✅ Добавлены тесты для множественных экземпляров и очистки ресурсов

**Созданные файлы:**
- `core/network/src/commonTest/kotlin/com/company/ipcamera/core/network/RtspClientFFITest.kt` - 20+ тестов

**Статус Этапа 1: ✅ ЗАВЕРШЕН**
- Все задачи Этапа 1 (FFI биндинги) выполнены
- Готово к переходу на Этап 2 (Завершение RTSP протокола)

**Следующие шаги:**
- Этап 2: Завершение реализации RTSP протокола
  - ✅ 2.1 Улучшение парсинга SDP

### 26 January 2026 - Этап 2.1: Улучшение парсинга SDP ✅

**Выполненные задачи:**
1. ✅ Расширена структура RTPStream с дополнительными полями:
   - sps, pps, vps (для H.264/H.265)
   - profileLevelId, fmtpParams
   - range, trackId, channels
2. ✅ Улучшен парсинг стандартных SDP атрибутов:
   - v= (версия), o= (origin), s= (session name)
   - session-level control URL
3. ✅ Полная поддержка a=fmtp для всех кодеков:
   - H.264: sprop-parameter-sets, profile-level-id
   - H.265: sprop-sps, sprop-pps, sprop-vps
   - AAC: config параметр
4. ✅ Улучшен парсинг a=control с извлечением track ID
5. ✅ Расширена поддержка a=rtpmap для всех кодеков:
   - H.264, H.265, AAC, PCMU (G.711 μ-law), PCMA (G.711 A-law), MP3
   - Нормализация названий кодеков
   - Парсинг clockRate и channels
6. ✅ Добавлена поддержка a=range для временных диапазонов
7. ✅ Реализована обработка ошибок с try-catch и валидацией

**Измененные файлы:**
- `native/video-processing/src/rtsp_client.cpp` - улучшена функция parse_sdp

**Следующие шаги:**
- Этап 2.2: Реализация Digest Authentication ✅

### 26 January 2026 - Этап 2.2: Реализация Digest Authentication ✅

**Выполненные задачи:**
1. ✅ Улучшена поддержка MD5-sess алгоритма в generate_digest_auth
2. ✅ Добавлена поддержка qop="auth-int" (в дополнение к qop="auth")
3. ✅ Добавлена обработка 401 Unauthorized в DESCRIBE запросе
4. ✅ Реализовано автоматическое переключение с Basic на Digest Authentication
5. ✅ Интегрирована Digest Authentication с существующей Basic Authentication
6. ✅ Обновлены все вызовы parse_rtsp_response для поддержки wwwAuthenticate

**Измененные файлы:**
- `native/video-processing/src/rtsp_client.cpp` - улучшена generate_digest_auth, добавлена обработка 401

**Следующие шаги:**
- Этап 2.3: Улучшение обработки RTSP ответов ✅

### 26 January 2026 - Этап 2.3: Улучшение обработки RTSP ответов ✅

**Выполненные задачи:**
1. ✅ Создана структура RTSPResponseHeaders для хранения всех заголовков ответа
2. ✅ Улучшена функция parse_rtsp_response с поддержкой всех стандартных заголовков:
   - Session, WWW-Authenticate, Transport, Range, RTP-Info
   - Content-Type, Content-Length, CSeq, Server, Public, Location, Retry-After
3. ✅ Добавлена обработка ошибок (4xx, 5xx) с детальными сообщениями
4. ✅ Улучшен парсинг Transport заголовка в SETUP ответе:
   - Парсинг server_port для UDP
   - Парсинг interleaved для TCP транспорта
   - Парсинг source для multicast
5. ✅ Добавлен парсинг Range заголовка в PLAY ответе (npt значения)
6. ✅ Добавлен парсинг RTP-Info заголовка в PLAY ответе:
   - Парсинг seq (sequence number)
   - Парсинг rtptime (RTP timestamp)
   - Парсинг url (track URL)
   - Поддержка множественных потоков

**Измененные файлы:**
- `native/video-processing/src/rtsp_client.cpp` - улучшена parse_rtsp_response, добавлена RTSPResponseHeaders

**Следующие шаги:**
- Этап 2.4: Реализация TCP транспорта для RTP ✅

### 26 January 2026 - Этап 2.4: Реализация TCP транспорта для RTP ✅

**Выполненные задачи:**
1. ✅ Добавлена поддержка RTP/AVP/TCP транспорта в структуре RTPStream
2. ✅ Реализована функция read_interleaved_data для чтения interleaved binary data
3. ✅ Добавлена обработка interleaved параметра в Transport заголовке (rtpChannel, rtcpChannel)
4. ✅ Реализовано чтение бинарных данных из RTSP сокета с поддержкой формата $<channel><length><data>
5. ✅ Обновлена функция receive_rtp_thread для поддержки как UDP, так и TCP транспорта
6. ✅ Добавлен выбор транспорта: UDP по умолчанию, TCP как fallback при ошибке 461 (Unsupported Transport)
7. ✅ Добавлена обработка ошибок при неудаче UDP с автоматическим переключением на TCP

**Измененные файлы:**
- `native/video-processing/src/rtsp_client.cpp` - добавлена поддержка TCP транспорта, функция read_interleaved_data, обновлен receive_rtp_thread

**Следующие шаги:**
- Этап 3: Реализация обработки RTP/RTCP пакетов

---

**Последнее обновление:** 26 January 2026
**Ответственный:** Development Team
**Статус:** В процессе (Этап 1.1 - Настройка FFI биндингов)
