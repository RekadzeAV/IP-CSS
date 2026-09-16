# План продолжения работ: 9.1.4 Интеграция с RTSP

**Дата создания:** 26 January 2026
**Версия:** 1.0
**Статус:** В процессе реализации

---

## 📊 Анализ текущего состояния

### ✅ Что уже реализовано

#### 1. Kotlin обертка (RtspClient.kt) - **100%**
- ✅ Полная структура класса `RtspClient`
- ✅ Управление жизненным циклом (connect, play, stop, pause, disconnect)
- ✅ Flow для видеокадров и аудиокадров
- ✅ Callbacks для статуса и кадров
- ✅ Автоматическое переподключение (конфигурация)
- ✅ Определение аудио кодеков
- ✅ Интеграция с `NativeRtspClient`

**Файл:** `core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/RtspClient.kt`

#### 2. Native FFI биндинги (NativeRtspClient.native.kt) - **100%**
- ✅ Полная реализация через Kotlin/Native cinterop
- ✅ Все методы C API обернуты
- ✅ Правильная обработка callbacks через StableRef
- ✅ Конвертация типов между C и Kotlin
- ✅ Управление памятью (освобождение кадров)

**Файл:** `core/network/src/nativeMain/kotlin/com/company/ipcamera/core/network/rtsp/NativeRtspClient.native.kt`

#### 3. C Interop конфигурация - **100%**
- ✅ Файл `.def` настроен правильно
- ✅ Все функции объявлены
- ✅ Пути к заголовкам и библиотекам указаны

**Файл:** `core/network/src/nativeInterop/cinterop/rtsp_client.def`

#### 4. C++ библиотека (rtsp_client.cpp) - **~70%**
- ✅ Базовая структура RTSP клиента
- ✅ Парсинг RTSP URL
- ✅ TCP подключение к серверу
- ✅ RTSP протокол (OPTIONS, DESCRIBE, SETUP, PLAY, TEARDOWN)
- ✅ Парсинг SDP ответа
- ✅ RTP/RTCP обработка пакетов
- ✅ Поток приема RTP пакетов
- ✅ Обработка кадров и вызов callbacks
- ✅ Автоматическое переподключение
- ⚠️ **Заглушка без FFmpeg** (строка 978) - работает, но без декодирования
- ⚠️ **FFmpeg интеграция** - код есть, но требует тестирования

**Файл:** `native/video-processing/src/rtsp_client.cpp`

#### 5. Интеграция с сервером - **100%**
- ✅ `VideoStreamService` использует `RtspClient`
- ✅ Управление активными стримами
- ✅ Интеграция с `HlsGeneratorService`
- ✅ Обработка видеокадров

**Файл:** `server/api/src/main/kotlin/com/company/ipcamera/server/service/VideoStreamService.kt`

#### 6. CMake конфигурация - **100%**
- ✅ Настроена сборка нативной библиотеки
- ✅ Поддержка FFmpeg (опционально)
- ✅ Поддержка всех платформ (Windows, Linux, macOS, Android, iOS)

**Файлы:** `native/CMakeLists.txt`, `native/video-processing/CMakeLists.txt`

---

### ⚠️ Что требует доработки

#### 1. C++ библиотека - **Критические проблемы**

##### 1.1 FFmpeg интеграция (~30% готово)
**Проблема:** Код с FFmpeg есть, но:
- ⚠️ Не протестирован полностью
- ⚠️ Может быть неполная реализация декодирования
- ⚠️ Нет обработки ошибок декодирования
- ⚠️ Нет поддержки всех кодеков (H.264, H.265, MJPEG, AAC, PCM)

**Текущее состояние:**
- ✅ Есть код для инициализации FFmpeg контекста
- ✅ Есть код для декодирования через FFmpeg
- ⚠️ Заглушка без FFmpeg работает, но не декодирует кадры

**Что нужно:**
1. Завершить реализацию декодирования H.264/H.265 через FFmpeg
2. Добавить поддержку MJPEG декодирования
3. Реализовать декодирование аудио (AAC, PCM)
4. Добавить обработку ошибок декодирования
5. Оптимизировать производительность декодирования

##### 1.2 RTP/RTCP обработка (~80% готово)
**Проблема:**
- ⚠️ Обработка RTP пакетов реализована, но может быть неполной
- ⚠️ Нет обработки фрагментированных NAL units (H.264)
- ⚠️ Нет обработки RTCP пакетов (только заглушка)
- ⚠️ Нет синхронизации аудио/видео

**Что нужно:**
1. Реализовать сборку фрагментированных NAL units
2. Добавить обработку RTCP пакетов (синхронизация, статистика)
3. Реализовать синхронизацию аудио/видео по timestamp
4. Добавить обработку потери пакетов

##### 1.3 Аутентификация (~60% готово)
**Проблема:**
- ✅ Basic Authentication реализована
- ❌ Digest Authentication не реализована
- ⚠️ Нет обработки 401 Unauthorized с WWW-Authenticate

**Что нужно:**
1. Реализовать Digest Authentication
2. Добавить обработку 401 Unauthorized
3. Добавить поддержку разных методов аутентификации

##### 1.4 Обработка ошибок (~50% готово)
**Проблема:**
- ⚠️ Базовая обработка ошибок есть
- ⚠️ Нет детального логирования
- ⚠️ Нет восстановления после ошибок

**Что нужно:**
1. Добавить детальное логирование всех операций
2. Реализовать восстановление после сетевых ошибок
3. Добавить валидацию всех входных данных

#### 2. Тестирование - **0% готово**

**Проблема:** Нет тестов для RTSP интеграции

**Что нужно:**
1. Unit тесты для Kotlin обертки
2. Integration тесты для C++ библиотеки
3. End-to-end тесты с реальными камерами
4. Тесты на разных платформах

#### 3. Документация - **50% готово**

**Проблема:**
- ✅ Есть базовая документация
- ⚠️ Нет детального описания API
- ⚠️ Нет примеров использования
- ⚠️ Нет troubleshooting guide

**Что нужно:**
1. Детальное описание API
2. Примеры использования для всех платформ
3. Troubleshooting guide
4. Описание архитектуры

---

## 🎯 Детальный план продолжения работ

### Этап 1: Завершение C++ реализации RTSP клиента (2-3 недели)

#### Задача 1.1: Завершение FFmpeg интеграции (1 неделя)
**Приоритет:** 🔴 Критический

**Подзадачи:**
1. **Проверка и доработка декодирования H.264** (2 дня)
   - Проверить работу существующего кода
   - Исправить ошибки декодирования
   - Добавить обработку всех типов NAL units
   - Тестирование с реальными камерами

2. **Добавление поддержки H.265** (1 день)
   - Добавить декодер H.265
   - Тестирование

3. **Добавление поддержки MJPEG** (1 день)
   - Реализовать декодирование MJPEG
   - Тестирование

4. **Добавление декодирования аудио** (2 дня)
   - Реализовать декодирование AAC
   - Реализовать декодирование PCM/G.711
   - Тестирование

5. **Оптимизация производительности** (1 день)
   - Оптимизация декодирования
   - Управление памятью
   - Профилирование

**Критерии готовности:**
- ✅ Все основные кодеки работают (H.264, H.265, MJPEG, AAC)
- ✅ Декодирование работает стабильно
- ✅ Нет утечек памяти
- ✅ Производительность достаточна для real-time

**Файлы для изменения:**
- `native/video-processing/src/rtsp_client.cpp` (строки 850-995)

**Детальные инструкции:**

##### 1.1.1 Проверка и доработка декодирования H.264

**Текущий код (строки 850-995):**
```cpp
#ifdef ENABLE_FFMPEG
    // Инициализация FFmpeg
    avformat_network_init();
    client->formatContext = avformat_alloc_context();

    // Открытие RTSP потока
    AVDictionary* options = nullptr;
    av_dict_set(&options, "rtsp_transport", "tcp", 0);
    av_dict_set(&options, "stimeout", "10000000", 0); // 10 секунд

    int ret = avformat_open_input(&client->formatContext, url, nullptr, &options);
    if (ret < 0) {
        // Обработка ошибки
        return false;
    }

    // Поиск видеопотока
    ret = avformat_find_stream_info(client->formatContext, nullptr);
    if (ret < 0) {
        // Обработка ошибки
        return false;
    }

    // Инициализация декодера для видеопотока
    client->videoStreamIndex = av_find_best_stream(
        client->formatContext, AVMEDIA_TYPE_VIDEO, -1, -1, nullptr, 0);

    if (client->videoStreamIndex >= 0) {
        AVCodecParameters* codecParams =
            client->formatContext->streams[client->videoStreamIndex]->codecpar;
        const AVCodec* codec = avcodec_find_decoder(codecParams->codec_id);

        if (codec) {
            client->videoCodecContext = avcodec_alloc_context3(codec);
            avcodec_parameters_to_context(client->videoCodecContext, codecParams);

            if (avcodec_open2(client->videoCodecContext, codec, nullptr) >= 0) {
                // Создание RTSPStream
                RTSPStream* videoStream = new RTSPStream();
                videoStream->type = RTSP_STREAM_VIDEO;
                videoStream->width = codecParams->width;
                videoStream->height = codecParams->height;
                videoStream->fps = av_q2d(client->formatContext->streams[client->videoStreamIndex]->r_frame_rate);
                videoStream->codec = avcodec_get_name(codecParams->codec_id);
                client->streams.push_back(videoStream);
            }
        }
    }
#endif
```

**Что нужно исправить:**
1. **Обработка всех типов NAL units:**
   - IDR (Instantaneous Decoder Refresh) - ключевые кадры
   - Non-IDR - обычные кадры
   - SPS (Sequence Parameter Set) - параметры последовательности
   - PPS (Picture Parameter Set) - параметры изображения
   - SEI (Supplemental Enhancement Information) - дополнительная информация

2. **Декодирование кадров:**
   - Использовать `av_read_frame()` для чтения пакетов
   - Использовать `avcodec_send_packet()` и `avcodec_receive_frame()` для декодирования
   - Конвертировать кадры в нужный формат (например, YUV420P → RGB)

3. **Обработка ошибок:**
   - Проверка всех возвращаемых значений
   - Освобождение ресурсов при ошибках
   - Логирование ошибок

**Пример исправленного кода:**
```cpp
// В потоке приема кадров
AVPacket* packet = av_packet_alloc();
AVFrame* frame = av_frame_alloc();

while (!client->shouldStop && client->playing) {
    ret = av_read_frame(client->formatContext, packet);
    if (ret < 0) {
        if (ret == AVERROR_EOF) {
            break; // Конец потока
        }
        // Обработка ошибки
        continue;
    }

    if (packet->stream_index == client->videoStreamIndex) {
        // Отправка пакета в декодер
        ret = avcodec_send_packet(client->videoCodecContext, packet);
        if (ret < 0) {
            // Обработка ошибки
            av_packet_unref(packet);
            continue;
        }

        // Получение декодированного кадра
        while (ret >= 0) {
            ret = avcodec_receive_frame(client->videoCodecContext, frame);
            if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
                break;
            }
            if (ret < 0) {
                // Ошибка декодирования
                break;
            }

            // Конвертация кадра в нужный формат
            // Создание RTSPFrame и вызов callback
            RTSPFrame* rtspFrame = convert_avframe_to_rtspframe(frame);
            if (client->videoCallback) {
                client->videoCallback(rtspFrame, client->videoUserData);
            }
        }
    }

    av_packet_unref(packet);
}

av_packet_free(&packet);
av_frame_free(&frame);
```

##### 1.1.2 Добавление поддержки H.265

**Что нужно:**
1. Проверить поддержку H.265 в FFmpeg (обычно есть)
2. Добавить обработку специфичных для H.265 NAL units
3. Тестирование с камерами, поддерживающими H.265

**Код:**
```cpp
// В функции инициализации декодера
if (codecParams->codec_id == AV_CODEC_ID_HEVC) {
    // H.265 специфичная инициализация
    // Обработка VPS, SPS, PPS
}
```

##### 1.1.3 Добавление поддержки MJPEG

**Что нужно:**
1. MJPEG декодируется через FFmpeg (libjpeg)
2. Каждый кадр - это отдельный JPEG изображение
3. Не требуется сборка NAL units

**Код:**
```cpp
if (codecParams->codec_id == AV_CODEC_ID_MJPEG) {
    // MJPEG не требует специальной обработки
    // Каждый пакет - это готовый JPEG кадр
}
```

##### 1.1.4 Добавление декодирования аудио

**Что нужно:**
1. Инициализация аудио декодера (AAC, PCM, G.711)
2. Обработка аудио пакетов
3. Конвертация формата аудио (если нужно)

**Код:**
```cpp
// Инициализация аудио декодера (аналогично видео)
client->audioStreamIndex = av_find_best_stream(
    client->formatContext, AVMEDIA_TYPE_AUDIO, -1, -1, nullptr, 0);

if (client->audioStreamIndex >= 0) {
    AVCodecParameters* codecParams =
        client->formatContext->streams[client->audioStreamIndex]->codecpar;
    const AVCodec* codec = avcodec_find_decoder(codecParams->codec_id);

    if (codec) {
        client->audioCodecContext = avcodec_alloc_context3(codec);
        avcodec_parameters_to_context(client->audioCodecContext, codecParams);

        if (avcodec_open2(client->audioCodecContext, codec, nullptr) >= 0) {
            // Создание RTSPStream для аудио
            RTSPStream* audioStream = new RTSPStream();
            audioStream->type = RTSP_STREAM_AUDIO;
            audioStream->codec = avcodec_get_name(codecParams->codec_id);
            client->streams.push_back(audioStream);
        }
    }
}

// В потоке приема кадров
if (packet->stream_index == client->audioStreamIndex) {
    ret = avcodec_send_packet(client->audioCodecContext, packet);
    if (ret >= 0) {
        while (ret >= 0) {
            ret = avcodec_receive_frame(client->audioCodecContext, frame);
            if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
                break;
            }

            // Создание RTSPFrame для аудио
            RTSPFrame* rtspFrame = convert_avframe_to_rtspframe_audio(frame);
            if (client->audioCallback) {
                client->audioCallback(rtspFrame, client->audioUserData);
            }
        }
    }
}
```

##### 1.1.5 Оптимизация производительности

**Что нужно:**
1. Использовать аппаратное ускорение (если доступно)
2. Оптимизировать управление памятью
3. Использовать пулы буферов
4. Профилирование с помощью инструментов (perf, valgrind)

**Пример оптимизации:**
```cpp
// Использование аппаратного декодера (если доступно)
const AVCodec* codec = nullptr;
if (ENABLE_HW_ACCELERATION) {
    codec = avcodec_find_decoder_by_name("h264_cuvid"); // NVIDIA CUDA
    if (!codec) {
        codec = avcodec_find_decoder_by_name("h264_qsv"); // Intel Quick Sync
    }
}
if (!codec) {
    codec = avcodec_find_decoder(codecParams->codec_id); // Программный
}
```

**Чеклист для проверки:**
- [ ] H.264 декодирование работает стабильно
- [ ] H.265 декодирование работает
- [ ] MJPEG декодирование работает
- [ ] AAC аудио декодирование работает
- [ ] PCM/G.711 аудио декодирование работает
- [ ] Нет утечек памяти (проверено valgrind)
- [ ] Производительность достаточна для real-time (30 FPS)
- [ ] Обработка ошибок работает корректно

---

#### Задача 1.2: Улучшение RTP/RTCP обработки (1 неделя)
**Приоритет:** 🔴 Критический

**Подзадачи:**
1. **Сборка фрагментированных NAL units** (2 дня)
   - Реализовать сборку FU-A (Fragmentation Unit)
   - Реализовать сборку STAP-A (Single-Time Aggregation Packet)
   - Тестирование с разными камерами

2. **Обработка RTCP пакетов** (2 дня)
   - Реализовать парсинг RTCP пакетов (SR, RR, SDES, BYE)
   - Реализовать синхронизацию по RTCP
   - Добавить статистику (потеря пакетов, jitter)

3. **Синхронизация аудио/видео** (2 дня)
   - Реализовать синхронизацию по RTP timestamp
   - Реализовать синхронизацию по RTCP
   - Тестирование

4. **Обработка потери пакетов** (1 день)
   - Добавить обнаружение потери пакетов
   - Добавить логирование потери
   - Опционально: запрос повторной передачи

**Критерии готовности:**
- ✅ Фрагментированные NAL units собираются правильно
- ✅ RTCP пакеты обрабатываются
- ✅ Аудио/видео синхронизированы
- ✅ Потеря пакетов обнаруживается и логируется

**Файлы для изменения:**
- `native/video-processing/src/rtsp_client.cpp` (строки 500-601)

**Детальные инструкции:**

##### 1.2.1 Сборка фрагментированных NAL units

**Проблема:** H.264 NAL units могут быть разбиты на несколько RTP пакетов (FU-A) или объединены в один пакет (STAP-A).

**Текущий код (строки 500-545):**
```cpp
static void process_rtp_packet(const uint8_t* data, int size, RTPStream& stream, RTSPClient* client) {
    // Парсинг RTP заголовка
    // ...
    int payloadSize = size - headerSize;
    const uint8_t* payload = data + headerSize;

    // Создание RTSPFrame напрямую из payload
    RTSPFrame* frame = new RTSPFrame();
    frame->data = new uint8_t[payloadSize];
    memcpy(frame->data, payload, payloadSize);
    // ...
}
```

**Что нужно добавить:**

1. **Обработка FU-A (Fragmentation Unit):**
```cpp
// Структура для хранения фрагментированных NAL units
struct FragmentedNAL {
    uint8_t* data;
    int size;
    uint32_t timestamp;
    bool isComplete;
};

// В структуре RTPStream добавить:
std::map<uint32_t, FragmentedNAL> fragmentedNals; // key = timestamp

// В process_rtp_packet:
static void process_rtp_packet(const uint8_t* data, int size, RTPStream& stream, RTSPClient* client) {
    // ... парсинг RTP заголовка ...

    const uint8_t* payload = data + headerSize;
    int payloadSize = size - headerSize;

    if (payloadSize < 1) return;

    uint8_t nalHeader = payload[0];
    uint8_t nalType = nalHeader & 0x1F;

    // Проверка на FU-A (Fragmentation Unit)
    if (nalType == 28 || nalType == 29) { // FU-A или FU-B
        uint8_t fuIndicator = payload[0];
        uint8_t fuHeader = payload[1];
        bool start = (fuHeader & 0x80) != 0;
        bool end = (fuHeader & 0x40) != 0;
        uint8_t nalTypeFu = fuHeader & 0x1F;

        // Восстановление NAL unit header
        uint8_t reconstructedNalHeader = (fuIndicator & 0xE0) | nalTypeFu;

        if (start) {
            // Начало фрагментированного NAL unit
            FragmentedNAL frag;
            frag.data = new uint8_t[payloadSize - 2 + 1]; // -2 для FU header, +1 для NAL header
            frag.data[0] = reconstructedNalHeader;
            memcpy(frag.data + 1, payload + 2, payloadSize - 2);
            frag.size = payloadSize - 2 + 1;
            frag.timestamp = timestamp;
            frag.isComplete = false;

            stream.fragmentedNals[timestamp] = frag;
        } else {
            // Продолжение фрагментированного NAL unit
            auto it = stream.fragmentedNals.find(timestamp);
            if (it != stream.fragmentedNals.end()) {
                FragmentedNAL& frag = it->second;
                uint8_t* newData = new uint8_t[frag.size + payloadSize - 2];
                memcpy(newData, frag.data, frag.size);
                memcpy(newData + frag.size, payload + 2, payloadSize - 2);
                delete[] frag.data;
                frag.data = newData;
                frag.size += payloadSize - 2;
                frag.isComplete = end;

                if (end) {
                    // NAL unit собран, создаем RTSPFrame
                    RTSPFrame* frame = new RTSPFrame();
                    frame->data = frag.data;
                    frame->size = frag.size;
                    frame->timestamp = timestamp;
                    frame->type = stream.type;
                    frame->width = stream.width;
                    frame->height = stream.height;

                    // Вызов callback
                    if (stream.type == RTSP_STREAM_VIDEO && client->videoCallback) {
                        client->videoCallback(frame, client->videoUserData);
                    }

                    stream.fragmentedNals.erase(it);
                }
            }
        }
    } else if (nalType == 24 || nalType == 25) {
        // STAP-A или STAP-B (Single-Time Aggregation Packet)
        // Обработка объединенных NAL units
        int offset = 1; // Пропускаем STAP header
        while (offset < payloadSize) {
            if (offset + 2 > payloadSize) break;

            uint16_t nalSize = (payload[offset] << 8) | payload[offset + 1];
            offset += 2;

            if (offset + nalSize > payloadSize) break;

            // Создание RTSPFrame для каждого NAL unit
            RTSPFrame* frame = new RTSPFrame();
            frame->data = new uint8_t[nalSize];
            memcpy(frame->data, payload + offset, nalSize);
            frame->size = nalSize;
            frame->timestamp = timestamp;
            frame->type = stream.type;
            frame->width = stream.width;
            frame->height = stream.height;

            // Вызов callback
            if (stream.type == RTSP_STREAM_VIDEO && client->videoCallback) {
                client->videoCallback(frame, client->videoUserData);
            }

            offset += nalSize;
        }
    } else {
        // Обычный NAL unit (не фрагментированный)
        RTSPFrame* frame = new RTSPFrame();
        frame->data = new uint8_t[payloadSize];
        memcpy(frame->data, payload, payloadSize);
        frame->size = payloadSize;
        frame->timestamp = timestamp;
        frame->type = stream.type;
        frame->width = stream.width;
        frame->height = stream.height;

        // Вызов callback
        if (stream.type == RTSP_STREAM_VIDEO && client->videoCallback) {
            client->videoCallback(frame, client->videoUserData);
        }
    }
}
```

2. **Очистка старых фрагментированных NAL units:**
```cpp
// В receive_rtp_thread добавить таймаут для очистки
static void cleanup_old_fragments(RTPStream& stream, uint32_t currentTimestamp, uint32_t timeout = 90000) {
    // Очистка фрагментов старше timeout (например, 1 секунда при 90kHz)
    auto it = stream.fragmentedNals.begin();
    while (it != stream.fragmentedNals.end()) {
        if (currentTimestamp - it->first > timeout) {
            delete[] it->second.data;
            it = stream.fragmentedNals.erase(it);
        } else {
            ++it;
        }
    }
}
```

##### 1.2.2 Обработка RTCP пакетов

**Что нужно:**
1. Парсинг RTCP пакетов (SR, RR, SDES, BYE)
2. Извлечение статистики (потеря пакетов, jitter)
3. Синхронизация по RTCP

**Код:**
```cpp
// Структура для RTCP статистики
struct RTCPStats {
    uint32_t packetsLost;
    uint32_t jitter;
    uint64_t ntpTimestamp;
    uint32_t rtpTimestamp;
};

// Парсинг RTCP пакета
static void process_rtcp_packet(const uint8_t* data, int size, RTPStream& stream, RTSPClient* client) {
    if (size < 8) return; // Минимальный размер RTCP пакета

    uint8_t version = (data[0] >> 6) & 0x3;
    uint8_t padding = (data[0] >> 5) & 0x1;
    uint8_t rc = data[0] & 0x1F;
    uint8_t pt = data[1];
    uint16_t length = (data[2] << 8) | data[3];

    if (pt == 200) { // SR (Sender Report)
        if (size < 28) return;

        RTCPStats stats;
        stats.ntpTimestamp = ((uint64_t)data[8] << 56) | ((uint64_t)data[9] << 48) |
                             ((uint64_t)data[10] << 40) | ((uint64_t)data[11] << 32) |
                             ((uint64_t)data[12] << 24) | ((uint64_t)data[13] << 16) |
                             ((uint64_t)data[14] << 8) | data[15];
        stats.rtpTimestamp = (data[16] << 24) | (data[17] << 16) | (data[18] << 8) | data[19];
        stats.packetsLost = (data[20] << 16) | (data[21] << 8) | data[22];
        stats.jitter = (data[24] << 24) | (data[25] << 16) | (data[26] << 8) | data[27];

        // Сохранение статистики
        stream.rtcpStats = stats;

        // Логирование потери пакетов
        if (stats.packetsLost > 0) {
            // Логирование или callback
        }
    } else if (pt == 201) { // RR (Receiver Report)
        // Аналогично SR, но для получателя
    }
}
```

##### 1.2.3 Синхронизация аудио/видео

**Что нужно:**
1. Использование RTP timestamp для синхронизации
2. Использование RTCP для коррекции времени
3. Буферизация кадров для синхронизации

**Код:**
```cpp
// Структура для буферизации кадров
struct BufferedFrame {
    RTSPFrame* frame;
    uint64_t presentationTime; // Время презентации
};

// В RTSPClient добавить:
std::vector<BufferedFrame> videoFrameBuffer;
std::vector<BufferedFrame> audioFrameBuffer;
std::mutex bufferMutex;

// Функция синхронизации
static void synchronize_frames(RTSPClient* client) {
    std::lock_guard<std::mutex> lock(client->bufferMutex);

    // Вычисление времени презентации на основе RTP timestamp
    // и RTCP синхронизации

    // Сортировка кадров по времени презентации
    std::sort(client->videoFrameBuffer.begin(), client->videoFrameBuffer.end(),
              [](const BufferedFrame& a, const BufferedFrame& b) {
                  return a.presentationTime < b.presentationTime;
              });

    // Отправка кадров в правильном порядке
    for (auto& buffered : client->videoFrameBuffer) {
        if (buffered.presentationTime <= get_current_time()) {
            if (client->videoCallback) {
                client->videoCallback(buffered.frame, client->videoUserData);
            }
        }
    }
}
```

**Чеклист для проверки:**
- [ ] FU-A фрагментированные NAL units собираются правильно
- [ ] STAP-A объединенные NAL units обрабатываются
- [ ] RTCP SR пакеты парсятся
- [ ] RTCP RR пакеты парсятся
- [ ] Статистика потери пакетов извлекается
- [ ] Jitter вычисляется
- [ ] Аудио/видео синхронизированы по timestamp
- [ ] Потеря пакетов логируется

---

#### Задача 1.3: Реализация Digest Authentication (3 дня)
**Приоритет:** 🟡 Высокий

**Подзадачи:**
1. **Парсинг WWW-Authenticate заголовка** (1 день)
   - Парсинг realm, nonce, algorithm
   - Поддержка разных алгоритмов (MD5, SHA-256)

2. **Генерация Digest ответа** (1 день)
   - Реализация MD5/SHA-256 хеширования
   - Генерация response
   - Добавление Authorization заголовка

3. **Интеграция в RTSP протокол** (1 день)
   - Обработка 401 Unauthorized
   - Автоматический retry с Digest
   - Тестирование

**Критерии готовности:**
- ✅ Digest Authentication работает
- ✅ Поддерживаются MD5 и SHA-256
- ✅ Автоматический retry после 401

**Файлы для изменения:**
- `native/video-processing/src/rtsp_client.cpp` (добавить функции)

**Детальные инструкции:**

##### 1.3.1 Парсинг WWW-Authenticate заголовка

**Что нужно:**
1. Парсинг realm, nonce, algorithm, qop из WWW-Authenticate заголовка
2. Поддержка разных алгоритмов (MD5, SHA-256)

**Код:**
```cpp
// Структура для Digest параметров
struct DigestParams {
    std::string realm;
    std::string nonce;
    std::string algorithm; // MD5, MD5-sess, SHA-256
    std::string qop; // quality of protection
    std::string opaque;
    bool stale;
};

// Парсинг WWW-Authenticate заголовка
static bool parse_www_authenticate(const std::string& header, DigestParams& params) {
    // Формат: Digest realm="...", nonce="...", algorithm=MD5, qop="auth"

    // Парсинг realm
    size_t realmPos = header.find("realm=\"");
    if (realmPos != std::string::npos) {
        size_t realmStart = realmPos + 7;
        size_t realmEnd = header.find("\"", realmStart);
        if (realmEnd != std::string::npos) {
            params.realm = header.substr(realmStart, realmEnd - realmStart);
        }
    }

    // Парсинг nonce
    size_t noncePos = header.find("nonce=\"");
    if (noncePos != std::string::npos) {
        size_t nonceStart = noncePos + 7;
        size_t nonceEnd = header.find("\"", nonceStart);
        if (nonceEnd != std::string::npos) {
            params.nonce = header.substr(nonceStart, nonceEnd - nonceStart);
        }
    }

    // Парсинг algorithm
    size_t algorithmPos = header.find("algorithm=");
    if (algorithmPos != std::string::npos) {
        size_t algorithmStart = algorithmPos + 10;
        size_t algorithmEnd = header.find_first_of(", \"", algorithmStart);
        if (algorithmEnd == std::string::npos) algorithmEnd = header.length();
        params.algorithm = header.substr(algorithmStart, algorithmEnd - algorithmStart);
    } else {
        params.algorithm = "MD5"; // По умолчанию
    }

    // Парсинг qop
    size_t qopPos = header.find("qop=\"");
    if (qopPos != std::string::npos) {
        size_t qopStart = qopPos + 5;
        size_t qopEnd = header.find("\"", qopStart);
        if (qopEnd != std::string::npos) {
            params.qop = header.substr(qopStart, qopEnd - qopStart);
        }
    }

    // Парсинг stale
    size_t stalePos = header.find("stale=");
    if (stalePos != std::string::npos) {
        params.stale = (header.find("true", stalePos) != std::string::npos);
    }

    return !params.realm.empty() && !params.nonce.empty();
}
```

##### 1.3.2 Генерация Digest ответа

**Что нужно:**
1. Реализация MD5/SHA-256 хеширования
2. Генерация response согласно RFC 2617
3. Формирование Authorization заголовка

**Код:**
```cpp
// MD5 хеширование
#include <openssl/md5.h> // или другая библиотека

static std::string md5_hash(const std::string& input) {
    unsigned char digest[MD5_DIGEST_LENGTH];
    MD5((unsigned char*)input.c_str(), input.length(), digest);

    char mdString[33];
    for (int i = 0; i < 16; i++) {
        sprintf(&mdString[i*2], "%02x", (unsigned int)digest[i]);
    }
    return std::string(mdString);
}

// Генерация Digest response
static std::string generate_digest_response(
    const std::string& method,
    const std::string& uri,
    const std::string& username,
    const std::string& password,
    const DigestParams& params,
    const std::string& cnonce
) {
    // HA1 = MD5(username:realm:password)
    std::string ha1Input = username + ":" + params.realm + ":" + password;
    std::string ha1 = md5_hash(ha1Input);

    // Если algorithm = "MD5-sess", то HA1 = MD5(HA1:nonce:cnonce)
    if (params.algorithm == "MD5-sess") {
        ha1 = md5_hash(ha1 + ":" + params.nonce + ":" + cnonce);
    }

    // HA2 = MD5(method:uri)
    std::string ha2Input = method + ":" + uri;
    std::string ha2 = md5_hash(ha2Input);

    // Если qop = "auth" или "auth-int", то response = MD5(HA1:nonce:nonceCount:cnonce:qop:HA2)
    // Иначе response = MD5(HA1:nonce:HA2)
    std::string response;
    if (!params.qop.empty()) {
        std::string nonceCount = "00000001"; // Увеличивать для каждого запроса
        std::string responseInput = ha1 + ":" + params.nonce + ":" + nonceCount + ":" + cnonce + ":" + params.qop + ":" + ha2;
        response = md5_hash(responseInput);
    } else {
        std::string responseInput = ha1 + ":" + params.nonce + ":" + ha2;
        response = md5_hash(responseInput);
    }

    return response;
}

// Формирование Authorization заголовка
static std::string generate_digest_auth_header(
    const std::string& username,
    const std::string& uri,
    const DigestParams& params,
    const std::string& response,
    const std::string& cnonce
) {
    std::ostringstream auth;
    auth << "Digest username=\"" << username << "\", "
         << "realm=\"" << params.realm << "\", "
         << "nonce=\"" << params.nonce << "\", "
         << "uri=\"" << uri << "\", "
         << "response=\"" << response << "\"";

    if (!params.algorithm.empty() && params.algorithm != "MD5") {
        auth << ", algorithm=" << params.algorithm;
    }

    if (!params.qop.empty()) {
        auth << ", qop=" << params.qop
             << ", nc=00000001"
             << ", cnonce=\"" << cnonce << "\"";
    }

    if (!params.opaque.empty()) {
        auth << ", opaque=\"" << params.opaque << "\"";
    }

    return auth.str();
}
```

##### 1.3.3 Интеграция в RTSP протокол

**Что нужно:**
1. Обработка 401 Unauthorized ответа
2. Автоматический retry с Digest Authentication
3. Сохранение Digest параметров для последующих запросов

**Код:**
```cpp
// В RTSPClient добавить:
DigestParams digestParams;
bool useDigestAuth = false;
std::string cnonce;

// Генерация cnonce (client nonce)
static std::string generate_cnonce() {
    // Генерация случайной строки (например, 16 байт в hex)
    char cnonce[33];
    for (int i = 0; i < 16; i++) {
        sprintf(&cnonce[i*2], "%02x", rand() % 256);
    }
    return std::string(cnonce);
}

// В rtsp_client_connect после отправки DESCRIBE:
// Проверка ответа на 401 Unauthorized
if (statusCode == 401) {
    // Поиск WWW-Authenticate заголовка
    size_t wwwAuthPos = describeResponse.find("WWW-Authenticate:");
    if (wwwAuthPos != std::string::npos) {
        size_t lineEnd = describeResponse.find("\r\n", wwwAuthPos);
        std::string wwwAuthHeader = describeResponse.substr(wwwAuthPos, lineEnd - wwwAuthPos);

        // Проверка на Digest
        if (wwwAuthHeader.find("Digest") != std::string::npos) {
            if (parse_www_authenticate(wwwAuthHeader, client->digestParams)) {
                client->useDigestAuth = true;
                client->cnonce = generate_cnonce();

                // Retry DESCRIBE с Digest Authentication
                std::ostringstream describeHeaders;
                describeHeaders << "CSeq: " << client->cseq++ << "\r\n";
                describeHeaders << "Accept: application/sdp\r\n";

                std::string digestResponse = generate_digest_response(
                    "DESCRIBE",
                    client->rtspUrl.path,
                    client->username,
                    client->password,
                    client->digestParams,
                    client->cnonce
                );

                std::string authHeader = generate_digest_auth_header(
                    client->username,
                    client->rtspUrl.path,
                    client->digestParams,
                    digestResponse,
                    client->cnonce
                );

                describeHeaders << "Authorization: " << authHeader << "\r\n";
                describeHeaders << "User-Agent: IP-CSS RTSP Client\r\n";

                std::string describeResponse2;
                if (!send_rtsp_request(client->rtspSocket, "DESCRIBE", client->rtspUrl.path,
                                      describeHeaders.str(), "", describeResponse2)) {
                    // Обработка ошибки
                    return false;
                }

                // Парсинг ответа
                if (!parse_rtsp_response(describeResponse2, statusCode, sessionId)) {
                    return false;
                }

                if (statusCode == 200) {
                    // Успешно, продолжаем с SDP
                    describeResponse = describeResponse2;
                } else {
                    return false;
                }
            }
        }
    }
}
```

**Чеклист для проверки:**
- [ ] WWW-Authenticate заголовок парсится правильно
- [ ] Digest response генерируется согласно RFC 2617
- [ ] MD5 алгоритм работает
- [ ] SHA-256 алгоритм работает (если поддерживается камерой)
- [ ] Автоматический retry после 401 работает
- [ ] Digest Authentication работает с разными камерами

---

#### Задача 1.4: Улучшение обработки ошибок и логирования (2 дня)
**Приоритет:** 🟡 Высокий

**Подзадачи:**
1. **Детальное логирование** (1 день)
   - Добавить логирование всех RTSP запросов/ответов
   - Добавить логирование RTP пакетов (опционально, debug режим)
   - Добавить логирование ошибок декодирования

2. **Восстановление после ошибок** (1 день)
   - Автоматическое переподключение при сетевых ошибках
   - Восстановление после ошибок декодирования
   - Graceful degradation

**Критерии готовности:**
- ✅ Все операции логируются
- ✅ Ошибки обрабатываются правильно
- ✅ Система восстанавливается после ошибок

**Файлы для изменения:**
- `native/video-processing/src/rtsp_client.cpp`

---

### Этап 2: Тестирование и отладка (1-2 недели)

#### Задача 2.1: Unit тесты для Kotlin обертки (3 дня)
**Приоритет:** 🟡 Высокий

**Подзадачи:**
1. Тесты для `RtspClient.connect()`
2. Тесты для `RtspClient.play()`, `stop()`, `pause()`
3. Тесты для callbacks
4. Тесты для Flow (видеокадры, аудиокадры)
5. Тесты для автоматического переподключения

**Файлы для создания:**
- `core/network/src/commonTest/kotlin/com/company/ipcamera/core/network/RtspClientTest.kt`

---

#### Задача 2.2: Integration тесты для C++ библиотеки (3 дня)
**Приоритет:** 🟡 Высокий

**Подзадачи:**
1. Тесты с mock RTSP сервером
2. Тесты с реальными камерами (опционально)
3. Тесты на разных платформах
4. Тесты производительности

**Файлы для создания:**
- `native/video-processing/tests/rtsp_client_test.cpp`

---

#### Задача 2.3: End-to-end тесты (2 дня)
**Приоритет:** 🟡 Высокий

**Подзадачи:**
1. Тесты полного цикла (подключение → воспроизведение → остановка)
2. Тесты с разными камерами
3. Тесты с разными кодеками
4. Тесты производительности

---

### Этап 3: Документация и примеры (3-5 дней)

#### Задача 3.1: Детальная документация API (2 дня)
**Приоритет:** 🟢 Средний

**Подзадачи:**
1. Описание всех методов `RtspClient`
2. Описание конфигурации
3. Описание callbacks
4. Примеры использования

**Файлы для создания/обновления:**
- `docs/RTSP_CLIENT_API.md`

---

#### Задача 3.2: Примеры использования (2 дня)
**Приоритет:** 🟢 Средний

**Подзадачи:**
1. Пример для веб-сервера (Kotlin)
2. Пример для Android приложения
3. Пример для iOS приложения
4. Пример для Desktop приложения

**Файлы для создания:**
- `docs/examples/rtsp_client_examples.md`

---

#### Задача 3.3: Troubleshooting guide (1 день)
**Приоритет:** 🟢 Средний

**Подзадачи:**
1. Типичные проблемы и решения
2. Отладка подключения
3. Отладка декодирования
4. Производительность

**Файлы для создания:**
- `docs/RTSP_TROUBLESHOOTING.md`

---

## 📋 Приоритизация задач

### Критический приоритет (блокируют MVP)
1. ✅ **Задача 1.1: Завершение FFmpeg интеграции** (1 неделя)
2. ✅ **Задача 1.2: Улучшение RTP/RTCP обработки** (1 неделя)

### Высокий приоритет (важно для стабильности)
3. ✅ **Задача 1.3: Реализация Digest Authentication** (3 дня)
4. ✅ **Задача 1.4: Улучшение обработки ошибок** (2 дня)
5. ✅ **Задача 2.1: Unit тесты** (3 дня)
6. ✅ **Задача 2.2: Integration тесты** (3 дня)

### Средний приоритет (желательно)
7. ✅ **Задача 2.3: End-to-end тесты** (2 дня)
8. ✅ **Задача 3.1-3.3: Документация** (5 дней)

---

## ⏱️ Оценка времени

### Минимальный вариант (только критическое)
- **Задача 1.1:** 1 неделя
- **Задача 1.2:** 1 неделя
- **Итого:** 2 недели

### Рекомендуемый вариант (критическое + высокий приоритет)
- **Задача 1.1:** 1 неделя
- **Задача 1.2:** 1 неделя
- **Задача 1.3:** 3 дня
- **Задача 1.4:** 2 дня
- **Задача 2.1:** 3 дня
- **Задача 2.2:** 3 дня
- **Итого:** ~3 недели

### Полный вариант (все задачи)
- **Этап 1:** 2-3 недели
- **Этап 2:** 1-2 недели
- **Этап 3:** 3-5 дней
- **Итого:** ~4-5 недель

---

## 🎯 Критерии завершения раздела 9.1.4

### Минимальные требования (MVP)
- ✅ RTSP клиент подключается к камерам
- ✅ Получает видеопоток
- ✅ Декодирует H.264 видео
- ✅ Передает кадры в Kotlin код
- ✅ Работает на сервере (Linux/Windows)

### Рекомендуемые требования
- ✅ Поддержка всех основных кодеков (H.264, H.265, MJPEG, AAC)
- ✅ Digest Authentication
- ✅ Стабильная работа с разными камерами
- ✅ Обработка ошибок и восстановление
- ✅ Тесты покрывают основные сценарии

### Идеальные требования
- ✅ Все кодеки поддерживаются
- ✅ Все методы аутентификации
- ✅ Полное покрытие тестами
- ✅ Детальная документация
- ✅ Примеры для всех платформ

---

## 📝 Следующие шаги

1. **Немедленно:**
   - Начать с Задачи 1.1 (FFmpeg интеграция)
   - Проверить работу существующего кода с FFmpeg
   - Исправить критические ошибки

2. **В течение недели:**
   - Завершить Задачу 1.1
   - Начать Задачу 1.2 (RTP/RTCP)

3. **В течение 2-3 недель:**
   - Завершить все критические задачи
   - Начать тестирование

4. **В течение месяца:**
   - Завершить все задачи высокого приоритета
   - Подготовить документацию

---

## 📦 Зависимости и требования

### Системные зависимости

#### Для сборки нативной библиотеки:
- **CMake** 3.15 или выше
- **C++ компилятор** с поддержкой C++17:
  - GCC 7+ (Linux)
  - Clang 5+ (macOS, Linux)
  - MSVC 2017+ (Windows)
- **FFmpeg** (опционально, но рекомендуется):
  - libavformat
  - libavcodec
  - libavutil
  - libswscale
  - libswresample
- **OpenSSL** (для Digest Authentication):
  - libcrypto
  - libssl

#### Для Kotlin/Native:
- **Kotlin** 1.9.0 или выше
- **Kotlin/Native** компилятор
- **Gradle** 7.0 или выше

### Установка зависимостей

#### Linux (Ubuntu/Debian):
```bash
sudo apt-get update
sudo apt-get install -y \
    cmake \
    build-essential \
    libavformat-dev \
    libavcodec-dev \
    libavutil-dev \
    libswscale-dev \
    libswresample-dev \
    libssl-dev
```

#### macOS:
```bash
brew install cmake ffmpeg openssl
```

#### Windows:
1. Установить CMake: https://cmake.org/download/
2. Установить FFmpeg: https://ffmpeg.org/download.html
3. Установить OpenSSL: https://slproweb.com/products/Win32OpenSSL.html

### Проверка установки зависимостей

```bash
# Проверка CMake
cmake --version

# Проверка FFmpeg
ffmpeg -version

# Проверка OpenSSL
openssl version

# Проверка компилятора
g++ --version  # или clang++ --version
```

## 🧪 Инструкции по тестированию

### Подготовка тестового окружения

1. **Настройка тестовой камеры:**
   - IP-камера с RTSP поддержкой
   - Или RTSP сервер (например, VLC Media Server)
   - Или mock RTSP сервер для unit тестов

2. **Сборка нативной библиотеки:**
```bash
cd native/video-processing
mkdir build && cd build
cmake .. -DENABLE_FFMPEG=ON
make
```

3. **Сборка Kotlin проекта:**
```bash
./gradlew build
```

### Unit тесты

**Создание тестового файла:**
```kotlin
// core/network/src/commonTest/kotlin/com/company/ipcamera/core/network/RtspClientTest.kt
package com.company.ipcamera.core.network

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RtspClientTest {
    @Test
    fun testCreateClient() = runTest {
        val config = RtspClientConfig(
            url = "rtsp://test.example.com/stream",
            username = "test",
            password = "test"
        )
        val client = RtspClient(config)
        assertNotNull(client)
    }

    @Test
    fun testStatusFlow() = runTest {
        val config = RtspClientConfig(url = "rtsp://test.example.com/stream")
        val client = RtspClient(config)

        val status = client.getStatus().value
        assertEquals(RtspClientStatus.DISCONNECTED, status)
    }

    // Добавить больше тестов...
}
```

### Integration тесты

**Создание тестового файла для C++:**
```cpp
// native/video-processing/tests/rtsp_client_test.cpp
#include "rtsp_client.h"
#include <cassert>
#include <iostream>

void test_create_client() {
    RTSPClient* client = rtsp_client_create();
    assert(client != nullptr);
    rtsp_client_destroy(client);
    std::cout << "✓ test_create_client passed" << std::endl;
}

void test_connect() {
    RTSPClient* client = rtsp_client_create();
    bool result = rtsp_client_connect(
        client,
        "rtsp://test.example.com/stream",
        "test",
        "test",
        10000
    );
    // Проверка результата
    rtsp_client_destroy(client);
    std::cout << "✓ test_connect passed" << std::endl;
}

int main() {
    test_create_client();
    test_connect();
    // Добавить больше тестов...
    return 0;
}
```

### End-to-end тесты

**Сценарий тестирования:**
1. Подключение к реальной камере
2. Получение видеопотока
3. Проверка получения кадров
4. Проверка декодирования
5. Проверка производительности

**Пример скрипта:**
```bash
#!/bin/bash
# scripts/test_rtsp_integration.sh

CAMERA_URL="rtsp://192.168.1.100:554/stream"
USERNAME="admin"
PASSWORD="password"

echo "Testing RTSP integration..."
echo "Camera URL: $CAMERA_URL"

# Запуск тестов
./gradlew :core:network:test
./gradlew :server:api:test

# Запуск C++ тестов
cd native/video-processing/build
./tests/rtsp_client_test

echo "Tests completed"
```

## 📊 Диаграммы последовательности

### Подключение к RTSP серверу

```
Client                    Native Client              RTSP Server
  |                            |                          |
  |-- connect() -------------->|                          |
  |                            |-- TCP Connect ---------->|
  |                            |<-- TCP Connected --------|
  |                            |-- OPTIONS ------------->|
  |                            |<-- 200 OK --------------|
  |                            |-- DESCRIBE ------------->|
  |                            |<-- 200 OK + SDP ---------|
  |                            |-- SETUP (video) -------->|
  |                            |<-- 200 OK + Session -----|
  |                            |-- SETUP (audio) -------->|
  |                            |<-- 200 OK --------------|
  |                            |-- PLAY ----------------->|
  |                            |<-- 200 OK --------------|
  |<-- CONNECTED --------------|                          |
  |                            |<-- RTP Packets ---------|
  |                            |-- Process RTP ---------->|
  |<-- Video Frames -----------|                          |
```

### Обработка фрагментированных NAL units

```
RTP Stream                RTP Processor              Frame Callback
  |                            |                          |
  |-- RTP Packet (FU-A start)->|                          |
  |                            |-- Store fragment ------->|
  |-- RTP Packet (FU-A cont) -->|                          |
  |                            |-- Append fragment ------>|
  |-- RTP Packet (FU-A end) -->|                          |
  |                            |-- Reconstruct NAL ------>|
  |                            |-- Create RTSPFrame ----->|
  |                            |-- Call callback -------->|
  |                            |                          |-- Frame ready
```

## ✅ Чеклист для проверки готовности

### Функциональность
- [ ] RTSP клиент подключается к камерам
- [ ] Получает видеопоток
- [ ] Получает аудиопоток (если есть)
- [ ] Декодирует H.264 видео
- [ ] Декодирует H.265 видео (если поддерживается)
- [ ] Декодирует MJPEG видео
- [ ] Декодирует AAC аудио
- [ ] Декодирует PCM/G.711 аудио
- [ ] Обрабатывает фрагментированные NAL units
- [ ] Обрабатывает RTCP пакеты
- [ ] Синхронизирует аудио/видео
- [ ] Basic Authentication работает
- [ ] Digest Authentication работает

### Производительность
- [ ] Декодирование работает в real-time (30 FPS)
- [ ] Нет утечек памяти
- [ ] Использование CPU приемлемо (< 50% на одном ядре)
- [ ] Использование памяти стабильно
- [ ] Задержка потока минимальна (< 1 секунда)

### Стабильность
- [ ] Обработка сетевых ошибок
- [ ] Автоматическое переподключение
- [ ] Восстановление после ошибок декодирования
- [ ] Graceful shutdown
- [ ] Нет крашей при длительной работе

### Тестирование
- [ ] Unit тесты покрывают основные сценарии
- [ ] Integration тесты проходят
- [ ] End-to-end тесты проходят
- [ ] Тесты на разных платформах проходят

### Документация
- [ ] API документация готова
- [ ] Примеры использования готовы
- [ ] Troubleshooting guide готов
- [ ] Инструкции по сборке готовы

## 🔗 Связанные документы

- [RTSP_CLIENT_INTEGRATION.md](RTSP_CLIENT_INTEGRATION.md) - Общая информация об интеграции
- [RTSP_FFI_INTEGRATION.md](rtsp/RTSP_FFI_INTEGRATION.md) - Детали FFI биндингов
- [MISSING_FUNCTIONALITY.md](MISSING_FUNCTIONALITY.md) - Общий список отсутствующего функционала
- [RTSP_CLIENT.md](RTSP_CLIENT.md) - Общее описание RTSP клиента
- [ARCHITECTURE.md](ARCHITECTURE.md) - Архитектура системы

## ⚠️ Известные проблемы и решения

### Проблема 1: FFmpeg не найден при сборке

**Симптомы:**
```
CMake Error: Could not find FFmpeg
```

**Решение:**
1. Установить FFmpeg зависимости (см. раздел "Зависимости и требования")
2. Указать путь к FFmpeg в CMake:
```bash
cmake .. -DFFMPEG_INCLUDE_DIR=/usr/local/include -DFFMPEG_LIB_DIR=/usr/local/lib
```

### Проблема 2: Утечки памяти при декодировании

**Симптомы:**
- Память растет со временем
- Приложение падает после длительной работы

**Решение:**
1. Убедиться, что все `av_packet_unref()` и `av_frame_unref()` вызываются
2. Проверить освобождение кадров в callbacks
3. Использовать valgrind для поиска утечек:
```bash
valgrind --leak-check=full ./rtsp_client_test
```

### Проблема 3: Кадры не приходят или приходят с задержкой

**Симптомы:**
- Callback не вызывается
- Кадры приходят с большой задержкой

**Решение:**
1. Проверить, что поток приема RTP запущен
2. Проверить обработку фрагментированных NAL units
3. Проверить буферизацию в Flow
4. Увеличить размер буфера, если нужно

### Проблема 4: Digest Authentication не работает

**Симптомы:**
- 401 Unauthorized даже с правильными credentials
- Digest response отклоняется

**Решение:**
1. Проверить генерацию nonce и cnonce
2. Убедиться, что алгоритм хеширования правильный
3. Проверить формат Authorization заголовка
4. Использовать Wireshark для анализа RTSP трафика

### Проблема 5: Синхронизация аудио/видео нарушена

**Симптомы:**
- Аудио опережает видео или наоборот
- Прыжки в воспроизведении

**Решение:**
1. Проверить использование RTP timestamp
2. Использовать RTCP для коррекции времени
3. Реализовать буферизацию кадров
4. Проверить clock rate в SDP

### Проблема 6: Компиляция Kotlin/Native не находит нативную библиотеку

**Симптомы:**
```
Unresolved reference: rtsp_client_create
```

**Решение:**
1. Убедиться, что библиотека скомпилирована
2. Проверить пути в `.def` файле
3. Проверить настройки cinterop в `build.gradle.kts`
4. Пересобрать проект:
```bash
./gradlew clean build
```

### Проблема 7: Callbacks вызываются из неправильного потока

**Симптомы:**
- Краши при вызове callbacks
- Проблемы с синхронизацией

**Решение:**
1. Использовать `StableRef` для callbacks (уже реализовано)
2. Убедиться, что callbacks вызываются из правильного контекста
3. Использовать `Dispatchers.IO` для обработки кадров

### Проблема 8: Производительность декодирования низкая

**Симптомы:**
- Высокое использование CPU
- Пропуск кадров

**Решение:**
1. Использовать аппаратное ускорение (если доступно)
2. Оптимизировать конвертацию форматов
3. Использовать многопоточность для декодирования
4. Уменьшить разрешение или FPS, если возможно

## 🛠️ Инструменты для отладки

### Wireshark
**Использование:** Анализ RTSP/RTP трафика
```bash
# Фильтр для RTSP
rtsp

# Фильтр для RTP
rtp

# Фильтр для конкретной камеры
ip.addr == 192.168.1.100 && rtsp
```

### FFmpeg
**Использование:** Тестирование подключения к RTSP
```bash
# Проверка подключения
ffmpeg -rtsp_transport tcp -i rtsp://camera.example.com/stream -frames:v 1 test.jpg

# Запись потока
ffmpeg -rtsp_transport tcp -i rtsp://camera.example.com/stream -c copy output.mp4
```

### Valgrind
**Использование:** Поиск утечек памяти
```bash
valgrind --leak-check=full --show-leak-kinds=all ./rtsp_client_test
```

### GDB/LLDB
**Использование:** Отладка C++ кода
```bash
# Linux
gdb ./rtsp_client_test

# macOS
lldb ./rtsp_client_test
```

### Kotlin Debugger
**Использование:** Отладка Kotlin кода
- Использовать IntelliJ IDEA или Android Studio
- Установить breakpoints в `RtspClient.kt`
- Использовать Evaluate Expression для проверки значений

## 📚 Дополнительные ресурсы

### RFC документы
- **RFC 2326** - RTSP (Real Time Streaming Protocol)
- **RFC 3550** - RTP: A Transport Protocol for Real-Time Applications
- **RFC 3551** - RTP Profile for Audio and Video Conferences
- **RFC 2617** - HTTP Digest Authentication
- **RFC 3984** - RTP Payload Format for H.264 Video
- **RFC 7798** - RTP Payload Format for High Efficiency Video Coding (HEVC)

### Полезные ссылки
- FFmpeg документация: https://ffmpeg.org/documentation.html
- H.264 NAL unit types: https://yumichan.net/video-processing/video-compression/introduction-to-h264-nal-unit/
- RTSP клиенты (для сравнения): VLC, FFmpeg, GStreamer

---

**Последнее обновление:** 26 January 2026
**Ответственный:** Development Team
**Статус:** В процессе реализации
