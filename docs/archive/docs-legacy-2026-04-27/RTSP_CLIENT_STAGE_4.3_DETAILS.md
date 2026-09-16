# Детальная детализация этапа 4.3 RTSP клиент: Реализация декодирования аудио

**Версия:** 1.0
**Дата:** 26 January 2026
**Статус:** В процессе
**Приоритет:** 🔴 Критический

---

## 📋 Обзор этапа 4.3

**Этап 4.3** является частью **Этапа 4: Интеграция декодирования видео/аудио** и фокусируется на реализации декодирования аудио потоков в RTSP клиенте.

### Цель этапа
Реализовать полное декодирование аудио потоков, полученных через RTSP, с поддержкой различных аудио кодеков (AAC, G.711, MP3) и интеграцией с системой callback'ов для передачи декодированных данных в Kotlin слой.

### Контекст
- **Предыдущий этап:** 4.2 - Реализация декодирования H.264/H.265 (видео)
- **Следующий этап:** 4.4 - Интеграция декодирования с callback'ами
- **Зависимости:** Этап 4.1 (Настройка FFmpeg), Этап 4.2 (Декодирование видео)

---

## 🎯 Задачи этапа 4.3

### 4.3.1 Инициализация AAC декодера

**Файл:** `native/video-processing/src/rtsp_client.cpp`
**Приоритет:** 🔴 Критический
**Оценка времени:** 1-2 дня

#### Описание задачи
Реализовать инициализацию AAC (Advanced Audio Coding) декодера через FFmpeg для декодирования аудио потоков в формате AAC.

#### Технические детали

**1. Определение структуры для AAC декодера:**
```cpp
struct AACDecoder {
    AVCodecContext* codecContext;
    AVCodec* codec;
    bool initialized;
    int sampleRate;
    int channels;
    AVSampleFormat sampleFormat;
};
```

**2. Функция инициализации AAC декодера:**
```cpp
#ifdef ENABLE_FFMPEG
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/avutil.h>

// Инициализация AAC декодера
AACDecoder* init_aac_decoder(
    const std::string& config,  // AAC config из SDP (a=fmtp:96 config=...)
    int sampleRate,              // Частота дискретизации
    int channels                 // Количество каналов
) {
    AACDecoder* decoder = new AACDecoder();
    decoder->initialized = false;

    // Поиск AAC декодера
    decoder->codec = avcodec_find_decoder(AV_CODEC_ID_AAC);
    if (!decoder->codec) {
        delete decoder;
        return nullptr;
    }

    // Создание контекста декодера
    decoder->codecContext = avcodec_alloc_context3(decoder->codec);
    if (!decoder->codecContext) {
        delete decoder;
        return nullptr;
    }

    // Установка параметров
    decoder->codecContext->sample_rate = sampleRate;
    decoder->codecContext->channels = channels;
    decoder->codecContext->channel_layout = av_get_default_channel_layout(channels);
    decoder->codecContext->sample_fmt = AV_SAMPLE_FMT_S16; // PCM 16-bit

    // Парсинг AAC config из SDP
    // Формат: config=hex_string (например, config=1210)
    if (!config.empty()) {
        // Конвертация hex строки в бинарные данные
        std::vector<uint8_t> extradata = hex_string_to_bytes(config);
        if (!extradata.empty()) {
            decoder->codecContext->extradata = (uint8_t*)av_malloc(extradata.size() + AV_INPUT_BUFFER_PADDING_SIZE);
            if (decoder->codecContext->extradata) {
                memcpy(decoder->codecContext->extradata, extradata.data(), extradata.size());
                decoder->codecContext->extradata_size = extradata.size();
            }
        }
    }

    // Открытие декодера
    if (avcodec_open2(decoder->codecContext, decoder->codec, nullptr) < 0) {
        avcodec_free_context(&decoder->codecContext);
        delete decoder;
        return nullptr;
    }

    decoder->initialized = true;
    decoder->sampleRate = sampleRate;
    decoder->channels = channels;
    decoder->sampleFormat = AV_SAMPLE_FMT_S16;

    return decoder;
}

// Вспомогательная функция для конвертации hex строки в байты
static std::vector<uint8_t> hex_string_to_bytes(const std::string& hex) {
    std::vector<uint8_t> bytes;
    for (size_t i = 0; i < hex.length(); i += 2) {
        std::string byteString = hex.substr(i, 2);
        uint8_t byte = (uint8_t)strtol(byteString.c_str(), nullptr, 16);
        bytes.push_back(byte);
    }
    return bytes;
}
#endif
```

**3. Обработка ошибок:**
- Проверка наличия FFmpeg библиотек
- Валидация параметров (sampleRate, channels)
- Обработка ошибок инициализации декодера
- Освобождение ресурсов при ошибках

#### Критерии приемки
- [ ] AAC декодер успешно инициализируется с параметрами из SDP
- [ ] Корректно парсится AAC config из fmtp параметров
- [ ] Обрабатываются все ошибки инициализации
- [ ] Ресурсы корректно освобождаются при ошибках

---

### 4.3.2 Инициализация G.711 декодера (PCM)

**Файл:** `native/video-processing/src/rtsp_client.cpp`
**Приоритет:** 🔴 Критический
**Оценка времени:** 1 день

#### Описание задачи
Реализовать декодирование G.711 аудио потоков (PCMU/PCMA). G.711 - это кодек без потерь, который требует только конвертации формата.

#### Технические детали

**1. Определение структуры для G.711 декодера:**
```cpp
struct G711Decoder {
    bool isPCMU;  // true для PCMU (μ-law), false для PCMA (A-law)
    int sampleRate;
    int channels;
};
```

**2. Функция инициализации G.711 декодера:**
```cpp
#ifdef ENABLE_FFMPEG
// Инициализация G.711 декодера
G711Decoder* init_g711_decoder(
    bool isPCMU,      // true для PCMU (μ-law), false для PCMA (A-law)
    int sampleRate,
    int channels
) {
    G711Decoder* decoder = new G711Decoder();
    decoder->isPCMU = isPCMU;
    decoder->sampleRate = sampleRate;
    decoder->channels = channels;

    // G.711 не требует специальной инициализации через FFmpeg
    // Можно использовать встроенные функции конвертации

    return decoder;
}

// Декодирование G.711 μ-law в PCM
static void decode_pcmu_to_pcm(const uint8_t* input, int16_t* output, int samples) {
    for (int i = 0; i < samples; i++) {
        uint8_t ulaw = input[i];
        // Инвертирование битов
        ulaw = ~ulaw;

        // Извлечение знака и величины
        int sign = (ulaw & 0x80) ? -1 : 1;
        int exponent = (ulaw >> 4) & 0x07;
        int mantissa = (ulaw & 0x0F) | 0x10;

        // Вычисление значения
        int sample = sign * ((mantissa << (exponent + 1)) - 33);
        output[i] = (int16_t)sample;
    }
}

// Декодирование G.711 A-law в PCM
static void decode_pcma_to_pcm(const uint8_t* input, int16_t* output, int samples) {
    for (int i = 0; i < samples; i++) {
        uint8_t alaw = input[i];
        // Инвертирование четных битов
        alaw ^= 0x55;

        // Извлечение знака и величины
        int sign = (alaw & 0x80) ? -1 : 1;
        int exponent = (alaw >> 4) & 0x07;
        int mantissa = (alaw & 0x0F);

        // Вычисление значения
        int sample;
        if (exponent == 0) {
            sample = sign * (mantissa << 4);
        } else {
            sample = sign * ((mantissa << (exponent + 3)) + 132);
        }
        output[i] = (int16_t)sample;
    }
}
#endif
```

**3. Альтернативная реализация через FFmpeg:**
```cpp
#ifdef ENABLE_FFMPEG
// Инициализация G.711 через FFmpeg
AVCodecContext* init_g711_decoder_ffmpeg(
    bool isPCMU,  // true для PCMU, false для PCMA
    int sampleRate,
    int channels
) {
    AVCodecID codecId = isPCMU ? AV_CODEC_ID_PCM_MULAW : AV_CODEC_ID_PCM_ALAW;

    const AVCodec* codec = avcodec_find_decoder(codecId);
    if (!codec) {
        return nullptr;
    }

    AVCodecContext* codecContext = avcodec_alloc_context3(codec);
    if (!codecContext) {
        return nullptr;
    }

    codecContext->sample_rate = sampleRate;
    codecContext->channels = channels;
    codecContext->channel_layout = av_get_default_channel_layout(channels);
    codecContext->sample_fmt = AV_SAMPLE_FMT_S16;

    if (avcodec_open2(codecContext, codec, nullptr) < 0) {
        avcodec_free_context(&codecContext);
        return nullptr;
    }

    return codecContext;
}
#endif
```

#### Критерии приемки
- [ ] G.711 декодер (PCMU/PCMA) успешно инициализируется
- [ ] Корректно декодируются PCMU потоки в PCM
- [ ] Корректно декодируются PCMA потоки в PCM
- [ ] Поддерживаются различные sample rates (8000, 16000 Hz)

---

### 4.3.3 Декодирование аудио пакетов

**Файл:** `native/video-processing/src/rtsp_client.cpp`
**Приоритет:** 🔴 Критический
**Оценка времени:** 2-3 дня

#### Описание задачи
Реализовать декодирование аудио пакетов, полученных через RTP, в PCM формат для передачи в callback'и.

#### Технические детали

**1. Структура для хранения декодированного аудио:**
```cpp
struct DecodedAudioFrame {
    std::vector<int16_t> samples;  // PCM samples (16-bit)
    int sampleRate;
    int channels;
    int64_t timestamp;  // RTP timestamp
    size_t sampleCount;
};
```

**2. Функция декодирования AAC пакетов:**
```cpp
#ifdef ENABLE_FFMPEG
// Декодирование AAC пакета
bool decode_aac_packet(
    AACDecoder* decoder,
    const uint8_t* data,
    int dataSize,
    DecodedAudioFrame& outputFrame
) {
    if (!decoder || !decoder->initialized) {
        return false;
    }

    // Создание AVPacket
    AVPacket* packet = av_packet_alloc();
    if (!packet) {
        return false;
    }

    packet->data = const_cast<uint8_t*>(data);
    packet->size = dataSize;

    // Отправка пакета в декодер
    int ret = avcodec_send_packet(decoder->codecContext, packet);
    if (ret < 0) {
        av_packet_free(&packet);
        return false;
    }

    // Получение декодированного кадра
    AVFrame* frame = av_frame_alloc();
    if (!frame) {
        av_packet_free(&packet);
        return false;
    }

    ret = avcodec_receive_frame(decoder->codecContext, frame);
    if (ret < 0) {
        av_frame_free(&frame);
        av_packet_free(&packet);
        return false;
    }

    // Конвертация в PCM S16
    outputFrame.sampleRate = frame->sample_rate;
    outputFrame.channels = frame->channels;
    outputFrame.sampleCount = frame->nb_samples;

    // Выделение памяти для samples
    size_t totalSamples = frame->nb_samples * frame->channels;
    outputFrame.samples.resize(totalSamples);

    // Конвертация формата (если необходимо)
    if (frame->format != AV_SAMPLE_FMT_S16) {
        // Использование libswresample для конвертации
        SwrContext* swr = swr_alloc();
        swr_alloc_set_opts(swr,
            av_get_default_channel_layout(frame->channels), AV_SAMPLE_FMT_S16, frame->sample_rate,
            frame->channel_layout, (AVSampleFormat)frame->format, frame->sample_rate,
            0, nullptr);

        if (swr_init(swr) < 0) {
            swr_free(&swr);
            av_frame_free(&frame);
            av_packet_free(&packet);
            return false;
        }

        uint8_t* outputBuffer = (uint8_t*)outputFrame.samples.data();
        const uint8_t** inputBuffer = (const uint8_t**)frame->data;

        int converted = swr_convert(swr, &outputBuffer, frame->nb_samples,
                                   inputBuffer, frame->nb_samples);

        swr_free(&swr);

        if (converted < 0) {
            av_frame_free(&frame);
            av_packet_free(&packet);
            return false;
        }
    } else {
        // Прямое копирование данных
        memcpy(outputFrame.samples.data(), frame->data[0],
               totalSamples * sizeof(int16_t));
    }

    av_frame_free(&frame);
    av_packet_free(&packet);

    return true;
}
#endif
```

**3. Функция декодирования G.711 пакетов:**
```cpp
#ifdef ENABLE_FFMPEG
// Декодирование G.711 пакета
bool decode_g711_packet(
    G711Decoder* decoder,
    const uint8_t* data,
    int dataSize,
    DecodedAudioFrame& outputFrame
) {
    if (!decoder) {
        return false;
    }

    outputFrame.sampleRate = decoder->sampleRate;
    outputFrame.channels = decoder->channels;
    outputFrame.sampleCount = dataSize;  // G.711: 1 байт = 1 sample

    size_t totalSamples = dataSize * decoder->channels;
    outputFrame.samples.resize(totalSamples);

    if (decoder->isPCMU) {
        decode_pcmu_to_pcm(data, outputFrame.samples.data(), dataSize);
    } else {
        decode_pcma_to_pcm(data, outputFrame.samples.data(), dataSize);
    }

    // Если стерео, нужно дублировать samples
    if (decoder->channels == 2) {
        // Дублирование моно в стерео
        for (int i = dataSize - 1; i >= 0; i--) {
            outputFrame.samples[i * 2] = outputFrame.samples[i];
            outputFrame.samples[i * 2 + 1] = outputFrame.samples[i];
        }
    }

    return true;
}
#endif
```

**4. Интеграция с обработкой RTP пакетов:**
```cpp
// В функции process_rtp_packet для аудио потоков
if (stream.type == RTSP_STREAM_AUDIO) {
    // Получение декодера для потока
    AudioDecoder* audioDecoder = get_audio_decoder(stream);
    if (!audioDecoder) {
        // Инициализация декодера при первом пакете
        audioDecoder = init_audio_decoder_for_stream(stream);
        if (!audioDecoder) {
            return; // Ошибка инициализации
        }
    }

    // Декодирование пакета
    DecodedAudioFrame decodedFrame;
    bool success = false;

    if (stream.codec == "AAC") {
        success = decode_aac_packet(
            (AACDecoder*)audioDecoder,
            stream.rtpPayload.data(),
            stream.rtpPayload.size(),
            decodedFrame
        );
    } else if (stream.codec == "PCMU" || stream.codec == "PCMA") {
        success = decode_g711_packet(
            (G711Decoder*)audioDecoder,
            stream.rtpPayload.data(),
            stream.rtpPayload.size(),
            decodedFrame
        );
    }

    if (success) {
        // Вызов callback с декодированным аудио
        if (client->audioCallback) {
            RTSPFrame audioFrame;
            audioFrame.data = (uint8_t*)decodedFrame.samples.data();
            audioFrame.size = decodedFrame.samples.size() * sizeof(int16_t);
            audioFrame.timestamp = stream.rtpTimestamp;
            audioFrame.type = RTSP_STREAM_AUDIO;
            audioFrame.width = 0;
            audioFrame.height = 0;

            client->audioCallback(&audioFrame, client->audioUserData);
        }
    }
}
```

#### Критерии приемки
- [ ] AAC пакеты корректно декодируются в PCM
- [ ] G.711 пакеты корректно декодируются в PCM
- [ ] Поддерживаются различные sample rates и channel configurations
- [ ] Обрабатываются ошибки декодирования
- [ ] Декодированные данные передаются в callback'и

---

### 4.3.4 Конвертация аудио форматов

**Файл:** `native/video-processing/src/rtsp_client.cpp`
**Приоритет:** 🟡 Высокий
**Оценка времени:** 1-2 дня

#### Описание задачи
Реализовать конвертацию аудио форматов для унификации выходного формата (PCM S16, 44100 Hz, стерео).

#### Технические детали

**1. Использование libswresample для конвертации:**
```cpp
#ifdef ENABLE_FFMPEG
#include <libswresample/swresample.h>

// Структура для конвертера форматов
struct AudioResampler {
    SwrContext* swrContext;
    int inputSampleRate;
    int inputChannels;
    AVSampleFormat inputFormat;
    int outputSampleRate;
    int outputChannels;
    AVSampleFormat outputFormat;
};

// Инициализация ресемплера
AudioResampler* init_audio_resampler(
    int inputSampleRate,
    int inputChannels,
    AVSampleFormat inputFormat,
    int outputSampleRate = 44100,
    int outputChannels = 2,
    AVSampleFormat outputFormat = AV_SAMPLE_FMT_S16
) {
    AudioResampler* resampler = new AudioResampler();

    resampler->inputSampleRate = inputSampleRate;
    resampler->inputChannels = inputChannels;
    resampler->inputFormat = inputFormat;
    resampler->outputSampleRate = outputSampleRate;
    resampler->outputChannels = outputChannels;
    resampler->outputFormat = outputFormat;

    // Создание контекста ресемплера
    resampler->swrContext = swr_alloc();
    if (!resampler->swrContext) {
        delete resampler;
        return nullptr;
    }

    // Настройка параметров
    av_opt_set_int(resampler->swrContext, "in_channel_layout",
                   av_get_default_channel_layout(inputChannels), 0);
    av_opt_set_int(resampler->swrContext, "in_sample_rate", inputSampleRate, 0);
    av_opt_set_sample_fmt(resampler->swrContext, "in_sample_fmt", inputFormat, 0);

    av_opt_set_int(resampler->swrContext, "out_channel_layout",
                   av_get_default_channel_layout(outputChannels), 0);
    av_opt_set_int(resampler->swrContext, "out_sample_rate", outputSampleRate, 0);
    av_opt_set_sample_fmt(resampler->swrContext, "out_sample_fmt", outputFormat, 0);

    // Инициализация
    if (swr_init(resampler->swrContext) < 0) {
        swr_free(&resampler->swrContext);
        delete resampler;
        return nullptr;
    }

    return resampler;
}

// Конвертация аудио
bool resample_audio(
    AudioResampler* resampler,
    const uint8_t* inputData,
    int inputSamples,
    std::vector<int16_t>& outputSamples
) {
    if (!resampler || !resampler->swrContext) {
        return false;
    }

    // Вычисление размера выходного буфера
    int maxOutputSamples = av_rescale_rnd(
        inputSamples,
        resampler->outputSampleRate,
        resampler->inputSampleRate,
        AV_ROUND_UP
    );

    outputSamples.resize(maxOutputSamples * resampler->outputChannels);

    const uint8_t** inputBuffer = &inputData;
    uint8_t* outputBuffer = (uint8_t*)outputSamples.data();

    // Конвертация
    int convertedSamples = swr_convert(
        resampler->swrContext,
        &outputBuffer, maxOutputSamples,
        inputBuffer, inputSamples
    );

    if (convertedSamples < 0) {
        return false;
    }

    // Обрезка до реального размера
    outputSamples.resize(convertedSamples * resampler->outputChannels);

    return true;
}

// Освобождение ресемплера
void free_audio_resampler(AudioResampler* resampler) {
    if (resampler) {
        if (resampler->swrContext) {
            swr_free(&resampler->swrContext);
        }
        delete resampler;
    }
}
#endif
```

#### Критерии приемки
- [ ] Ресемплинг работает для различных sample rates
- [ ] Конвертация каналов (моно ↔ стерео) работает корректно
- [ ] Конвертация форматов работает без потерь качества
- [ ] Обрабатываются ошибки конвертации

---

### 4.3.5 Синхронизация аудио и видео

**Файл:** `native/video-processing/src/rtsp_client.cpp`
**Приоритет:** 🟡 Высокий
**Оценка времени:** 2-3 дня

#### Описание задачи
Реализовать синхронизацию аудио и видео потоков для корректного воспроизведения.

#### Технические детали

**1. Структура для синхронизации:**
```cpp
struct AVSync {
    int64_t videoTimestamp;      // Текущий timestamp видео
    int64_t audioTimestamp;      // Текущий timestamp аудио
    int64_t videoClock;          // Видео clock (в миллисекундах)
    int64_t audioClock;          // Аудио clock (в миллисекундах)
    int64_t clockOffset;         // Смещение между аудио и видео
    bool syncInitialized;        // Флаг инициализации синхронизации
    int64_t lastVideoTimestamp; // Последний timestamp видео
    int64_t lastAudioTimestamp; // Последний timestamp аудио
};

// Инициализация синхронизации
void init_av_sync(AVSync* sync) {
    sync->videoTimestamp = 0;
    sync->audioTimestamp = 0;
    sync->videoClock = 0;
    sync->audioClock = 0;
    sync->clockOffset = 0;
    sync->syncInitialized = false;
    sync->lastVideoTimestamp = 0;
    sync->lastAudioTimestamp = 0;
}

// Обновление видео timestamp
void update_video_timestamp(AVSync* sync, int64_t rtpTimestamp, int clockRate) {
    sync->videoTimestamp = rtpTimestamp;
    sync->videoClock = (rtpTimestamp * 1000) / clockRate;  // Конвертация в миллисекунды

    if (!sync->syncInitialized && sync->audioTimestamp > 0) {
        // Инициализация синхронизации при первом видео кадре
        sync->clockOffset = sync->audioClock - sync->videoClock;
        sync->syncInitialized = true;
    }
}

// Обновление аудио timestamp
void update_audio_timestamp(AVSync* sync, int64_t rtpTimestamp, int clockRate) {
    sync->audioTimestamp = rtpTimestamp;
    sync->audioClock = (rtpTimestamp * 1000) / clockRate;  // Конвертация в миллисекунды

    if (!sync->syncInitialized && sync->videoTimestamp > 0) {
        // Инициализация синхронизации при первом аудио пакете
        sync->clockOffset = sync->audioClock - sync->videoClock;
        sync->syncInitialized = true;
    }
}

// Получение синхронизированного timestamp для видео
int64_t get_synced_video_timestamp(AVSync* sync) {
    if (!sync->syncInitialized) {
        return sync->videoClock;
    }

    // Применение смещения для синхронизации
    return sync->videoClock + sync->clockOffset;
}

// Получение синхронизированного timestamp для аудио
int64_t get_synced_audio_timestamp(AVSync* sync) {
    if (!sync->syncInitialized) {
        return sync->audioClock;
    }

    // Применение смещения для синхронизации
    return sync->audioClock - sync->clockOffset;
}

// Проверка необходимости задержки для синхронизации
int64_t calculate_sync_delay(AVSync* sync, int64_t currentTimestamp, bool isVideo) {
    if (!sync->syncInitialized) {
        return 0;
    }

    int64_t referenceTimestamp = isVideo ? sync->audioClock : sync->videoClock;
    int64_t delay = currentTimestamp - referenceTimestamp;

    // Ограничение задержки (не более 100ms вперед, не более 500ms назад)
    if (delay > 100) {
        delay = 100;
    } else if (delay < -500) {
        delay = -500;
    }

    return delay;
}
```

**2. Интеграция синхронизации в обработку пакетов:**
```cpp
// В функции process_rtp_packet
if (stream.type == RTSP_STREAM_VIDEO) {
    // Обновление видео timestamp
    update_video_timestamp(&client->avSync, stream.rtpTimestamp, stream.clockRate);

    // Получение синхронизированного timestamp
    int64_t syncedTimestamp = get_synced_video_timestamp(&client->avSync);

    // Вычисление задержки для синхронизации
    int64_t delay = calculate_sync_delay(&client->avSync, syncedTimestamp, true);

    // Применение задержки (если необходимо)
    if (delay > 0) {
        // Задержка перед отправкой кадра
        std::this_thread::sleep_for(std::chrono::milliseconds(delay));
    }

    // Отправка видео кадра
    // ...
}

if (stream.type == RTSP_STREAM_AUDIO) {
    // Обновление аудио timestamp
    update_audio_timestamp(&client->avSync, stream.rtpTimestamp, stream.clockRate);

    // Получение синхронизированного timestamp
    int64_t syncedTimestamp = get_synced_audio_timestamp(&client->avSync);

    // Вычисление задержки для синхронизации
    int64_t delay = calculate_sync_delay(&client->avSync, syncedTimestamp, false);

    // Применение задержки (если необходимо)
    if (delay > 0) {
        std::this_thread::sleep_for(std::chrono::milliseconds(delay));
    }

    // Отправка аудио кадра
    // ...
}
```

#### Критерии приемки
- [ ] Аудио и видео синхронизированы с точностью до 100ms
- [ ] Корректно обрабатываются случаи отсутствия одного из потоков
- [ ] Синхронизация автоматически восстанавливается после разрыва
- [ ] Задержки применяются корректно без заиканий

---

## 📊 Сводная таблица задач этапа 4.3

| Задача | Приоритет | Время | Статус | Зависимости |
|--------|-----------|-------|--------|-------------|
| 4.3.1 Инициализация AAC декодера | 🔴 Критический | 1-2 дня | ✅ Завершено | 4.1 |
| 4.3.2 Инициализация G.711 декодера | 🔴 Критический | 1 день | ✅ Завершено | 4.1 |
| 4.3.3 Декодирование аудио пакетов | 🔴 Критический | 2-3 дня | ✅ Завершено | 4.3.1, 4.3.2 |
| 4.3.4 Конвертация аудио форматов | 🟡 Высокий | 1-2 дня | ✅ Завершено | 4.3.3 |
| 4.3.5 Синхронизация аудио и видео | 🟡 Высокий | 2-3 дня | ✅ Завершено | 4.3.3, 4.2 |

**Общее время этапа:** 7-11 дней
**Критический путь:** 4.3.1 → 4.3.2 → 4.3.3 → 4.3.4 → 4.3.5

---

## 🔧 Технические требования

### Зависимости
- **FFmpeg** (libavcodec, libavformat, libavutil, libswresample)
- **OpenSSL** (для MD5, если используется в аутентификации)
- **C++11** или выше

### Платформы
- ✅ Android (через JNI)
- ✅ iOS (через cinterop)
- ✅ Desktop (Windows, Linux, macOS через cinterop)

### Поддерживаемые аудио кодеки
- **AAC** (Advanced Audio Coding) - приоритетный
- **PCMU** (G.711 μ-law)
- **PCMA** (G.711 A-law)
- **MP3** (опционально, если требуется)

---

## 📝 Примеры использования

### Пример 1: Инициализация AAC декодера

```cpp
// Получение параметров из SDP
std::string aacConfig = "1210";  // Из a=fmtp:96 config=1210
int sampleRate = 44100;
int channels = 2;

// Инициализация декодера
AACDecoder* decoder = init_aac_decoder(aacConfig, sampleRate, channels);
if (!decoder) {
    // Обработка ошибки
    return;
}

// Использование декодера
DecodedAudioFrame frame;
if (decode_aac_packet(decoder, audioData, audioDataSize, frame)) {
    // Обработка декодированного аудио
    process_audio_frame(frame);
}
```

### Пример 2: Декодирование G.711

```cpp
// Инициализация G.711 декодера (PCMU)
G711Decoder* decoder = init_g711_decoder(true, 8000, 1);  // PCMU, 8kHz, моно

// Декодирование пакета
DecodedAudioFrame frame;
if (decode_g711_packet(decoder, audioData, audioDataSize, frame)) {
    // Обработка декодированного аудио
    process_audio_frame(frame);
}
```

### Пример 3: Синхронизация аудио и видео

```cpp
// Инициализация синхронизации
AVSync sync;
init_av_sync(&sync);

// При получении видео кадра
update_video_timestamp(&sync, videoRtpTimestamp, 90000);
int64_t syncedVideoTime = get_synced_video_timestamp(&sync);

// При получении аудио пакета
update_audio_timestamp(&sync, audioRtpTimestamp, 44100);
int64_t syncedAudioTime = get_synced_audio_timestamp(&sync);
```

---

## ⚠️ Риски и митигация

### Риск 1: Проблемы с FFmpeg линковкой
**Вероятность:** Средняя
**Влияние:** Высокое
**Митигация:**
- Тщательная проверка путей к библиотекам
- Использование pkg-config для обнаружения FFmpeg
- Тестирование на всех платформах

### Риск 2: Проблемы с синхронизацией
**Вероятность:** Высокая
**Влияние:** Среднее
**Митигация:**
- Использование проверенных алгоритмов синхронизации
- Тестирование на различных потоках
- Настройка параметров синхронизации

### Риск 3: Производительность декодирования
**Вероятность:** Средняя
**Влияние:** Среднее
**Митигация:**
- Оптимизация использования памяти
- Использование аппаратного декодирования (если доступно)
- Профилирование и оптимизация критических участков

---

## 📚 Полезные ресурсы

### Документация
- [FFmpeg Audio Decoding](https://ffmpeg.org/doxygen/trunk/group__lavc__decoding.html)
- [AAC Audio Codec](https://en.wikipedia.org/wiki/Advanced_Audio_Coding)
- [G.711 Audio Codec](https://en.wikipedia.org/wiki/G.711)
- [RTP Audio Payload Formats](https://tools.ietf.org/html/rfc3551)

### RFC документы
- [RFC 3551 - RTP Profile for Audio and Video](https://tools.ietf.org/html/rfc3551)
- [RFC 3640 - RTP Payload Format for Transport of MPEG-4 Elementary Streams](https://tools.ietf.org/html/rfc3640)

---

## ✅ Критерии завершения этапа 4.3

### Функциональные требования
- [ ] AAC декодер инициализируется и работает корректно
- [ ] G.711 декодер (PCMU/PCMA) инициализируется и работает корректно
- [ ] Аудио пакеты декодируются в PCM формат
- [ ] Конвертация аудио форматов работает (если реализована)
- [ ] Синхронизация аудио и видео работает (если реализована)
- [ ] Декодированные данные передаются в callback'и

### Технические требования
- [ ] Код компилируется без ошибок
- [ ] Нет утечек памяти
- [ ] Обрабатываются все ошибки
- [ ] Код документирован

### Тестирование
- [ ] Unit тесты для декодеров
- [ ] Интеграционные тесты с реальными RTSP потоками
- [ ] Тестирование различных кодеков и конфигураций

---

**Последнее обновление:** 28 February 2026
**Ответственный:** Development Team

## 📝 Журнал изменений

### 28 February 2026 - Завершение реализации этапа 4.3
- ✅ Создан файл `audio_decoder.h` с определениями структур и функций (универсальный API)
- ✅ Создан файл `audio_decoder.cpp` с реализацией:
  - Инициализация AAC декодера (`init_aac_decoder`)
  - Инициализация G.711 декодера (`init_g711_decoder`)
  - Декодирование AAC пакетов (`decode_aac_packet`)
  - Декодирование G.711 пакетов (`decode_g711_packet`)
  - Ресемплинг аудио (`init_audio_resampler`, `resample_audio`)
- ✅ Добавлен include `audio_decoder.h` в `rtsp_client.cpp`
- ✅ Обнаружена полная реализация декодирования аудио в `rtsp_client.cpp`:
  - Функции `init_aac_decoder_for_stream()` и `init_g711_decoder_for_stream()`
  - Функции `decode_aac_packet()` и `decode_g711_packet()`
  - Интеграция в `process_rtp_packet()` с обработкой аудио потоков
  - Синхронизация аудио и видео через структуру `AVSync`
  - Вызов callback'ов для передачи декодированного аудио в Kotlin
- ✅ Этап 4.3 полностью реализован и интегрирован

## 🔗 Навигация

- [← Назад к RTSP клиенту](RTSP_CLIENT.md)
- [↑ К индексу документации](../DOCUMENTATION_INDEX.md)
- [→ К ONVIF клиенту](ONVIF_CLIENT_STAGE_4.4_DETAILS.md)
