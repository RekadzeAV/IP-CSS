#include "audio_decoder.h"

#ifdef ENABLE_FFMPEG
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavutil/avutil.h>
#include <libswresample/swresample.h>
}
#include <cstring>
#include <cstdlib>
#include <vector>
#include <string>
#include <sstream>

// Вспомогательная функция для конвертации hex строки в байты
static std::vector<uint8_t> hex_string_to_bytes(const std::string& hex) {
    std::vector<uint8_t> bytes;
    for (size_t i = 0; i < hex.length(); i += 2) {
        if (i + 1 >= hex.length()) break;
        std::string byteString = hex.substr(i, 2);
        uint8_t byte = (uint8_t)strtol(byteString.c_str(), nullptr, 16);
        bytes.push_back(byte);
    }
    return bytes;
}

// Структура для AAC декодера (C++ реализация)
struct AACDecoderImpl {
    AVCodecContext* codecContext;
    const AVCodec* codec;
    bool initialized;
    int sampleRate;
    int channels;
    AVSampleFormat sampleFormat;

    AACDecoderImpl() : codecContext(nullptr), codec(nullptr),
                       initialized(false), sampleRate(0), channels(0),
                       sampleFormat(AV_SAMPLE_FMT_S16) {}
};

// Инициализация AAC декодера
struct AACDecoder* init_aac_decoder(
    const char* config,
    int sampleRate,
    int channels
) {
    if (!config || sampleRate <= 0 || channels <= 0) {
        return nullptr;
    }

    AACDecoderImpl* decoder = new AACDecoderImpl();
    decoder->sampleRate = sampleRate;
    decoder->channels = channels;
    decoder->initialized = false;

    // Поиск AAC декодера
    const AVCodec* codec = avcodec_find_decoder(AV_CODEC_ID_AAC);
    if (!codec) {
        delete decoder;
        return nullptr;
    }
    decoder->codec = codec;
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
    // Используем новый API FFmpeg для channel layout
    AVChannelLayout chLayout;
    av_channel_layout_default(&chLayout, channels);
    av_channel_layout_copy(&decoder->codecContext->ch_layout, &chLayout);
    av_channel_layout_uninit(&chLayout);
    decoder->codecContext->sample_fmt = AV_SAMPLE_FMT_S16; // PCM 16-bit

    // Парсинг AAC config из SDP
    // Формат: config=hex_string (например, config=1210)
    std::string configStr(config);
    if (!configStr.empty()) {
        // Конвертация hex строки в бинарные данные
        std::vector<uint8_t> extradata = hex_string_to_bytes(configStr);
        if (!extradata.empty()) {
            decoder->codecContext->extradata = (uint8_t*)av_malloc(extradata.size() + AV_INPUT_BUFFER_PADDING_SIZE);
            if (decoder->codecContext->extradata) {
                memcpy(decoder->codecContext->extradata, extradata.data(), extradata.size());
                decoder->codecContext->extradata_size = extradata.size();
                memset(decoder->codecContext->extradata + extradata.size(), 0, AV_INPUT_BUFFER_PADDING_SIZE);
            }
        }
    }

    // Открытие декодера
    if (avcodec_open2(decoder->codecContext, decoder->codec, nullptr) < 0) {
        if (decoder->codecContext->extradata) {
            av_free(decoder->codecContext->extradata);
        }
        avcodec_free_context(&decoder->codecContext);
        delete decoder;
        return nullptr;
    }

    decoder->initialized = true;
    decoder->sampleFormat = AV_SAMPLE_FMT_S16;

    return reinterpret_cast<AACDecoder*>(decoder);
}

// Освобождение AAC декодера
void free_aac_decoder(struct AACDecoder* decoder) {
    if (!decoder) return;

    AACDecoderImpl* impl = reinterpret_cast<AACDecoderImpl*>(decoder);
    if (impl->codecContext) {
        if (impl->codecContext->extradata) {
            av_free(impl->codecContext->extradata);
        }
        avcodec_free_context(&impl->codecContext);
    }
    delete impl;
}

// Структура для G.711 декодера
struct G711DecoderImpl {
    bool isPCMU;
    int sampleRate;
    int channels;

    G711DecoderImpl() : isPCMU(false), sampleRate(8000), channels(1) {}
};

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
        output[i] = (int16_t)std::max(-32768, std::min(32767, sample));
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
        output[i] = (int16_t)std::max(-32768, std::min(32767, sample));
    }
}

// Инициализация G.711 декодера
struct G711Decoder* init_g711_decoder(
    bool isPCMU,
    int sampleRate,
    int channels
) {
    if (sampleRate <= 0 || channels <= 0) {
        return nullptr;
    }

    G711DecoderImpl* decoder = new G711DecoderImpl();
    decoder->isPCMU = isPCMU;
    decoder->sampleRate = sampleRate;
    decoder->channels = channels;

    return reinterpret_cast<G711Decoder*>(decoder);
}

// Освобождение G.711 декодера
void free_g711_decoder(struct G711Decoder* decoder) {
    if (decoder) {
        delete reinterpret_cast<G711DecoderImpl*>(decoder);
    }
}

// Декодирование AAC пакета
bool decode_aac_packet(
    struct AACDecoder* decoder,
    const uint8_t* data,
    int dataSize,
    struct DecodedAudioFrame* outputFrame
) {
    if (!decoder || !data || dataSize <= 0 || !outputFrame) {
        return false;
    }

    AACDecoderImpl* impl = reinterpret_cast<AACDecoderImpl*>(decoder);
    if (!impl->initialized || !impl->codecContext) {
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
    int ret = avcodec_send_packet(impl->codecContext, packet);
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

    ret = avcodec_receive_frame(impl->codecContext, frame);
    if (ret < 0) {
        av_frame_free(&frame);
        av_packet_free(&packet);
        return false;
    }

    // Конвертация в PCM S16
    outputFrame->sampleRate = frame->sample_rate;
    outputFrame->channels = frame->ch_layout.nb_channels;
    outputFrame->sampleCount = frame->nb_samples;

    // Выделение памяти для samples
    size_t totalSamples = frame->nb_samples * frame->ch_layout.nb_channels;
    outputFrame->samples = (int16_t*)malloc(totalSamples * sizeof(int16_t));
    if (!outputFrame->samples) {
        av_frame_free(&frame);
        av_packet_free(&packet);
        return false;
    }

    // Конвертация формата (если необходимо)
    if (frame->format != AV_SAMPLE_FMT_S16) {
        // Использование libswresample для конвертации
        SwrContext* swr = nullptr;
        AVChannelLayout outLayout, inLayout;
        av_channel_layout_default(&outLayout, frame->ch_layout.nb_channels);
        inLayout = frame->ch_layout; // Копируем layout из кадра
        swr_alloc_set_opts2(&swr,
            &outLayout, AV_SAMPLE_FMT_S16, frame->sample_rate,
            &inLayout, (AVSampleFormat)frame->format, frame->sample_rate,
            0, nullptr);

        if (swr_init(swr) < 0) {
            swr_free(&swr);
            free(outputFrame->samples);
            av_frame_free(&frame);
            av_packet_free(&packet);
            return false;
        }

        uint8_t* outputBuffer = (uint8_t*)outputFrame->samples;
        const uint8_t** inputBuffer = (const uint8_t**)frame->data;

        int converted = swr_convert(swr, &outputBuffer, frame->nb_samples,
                                   inputBuffer, frame->nb_samples);

        swr_free(&swr);

        if (converted < 0) {
            free(outputFrame->samples);
            av_frame_free(&frame);
            av_packet_free(&packet);
            return false;
        }
    } else {
        // Прямое копирование данных
        memcpy(outputFrame->samples, frame->data[0],
               totalSamples * sizeof(int16_t));
    }

    av_frame_free(&frame);
    av_packet_free(&packet);

    return true;
}

// Декодирование G.711 пакета
bool decode_g711_packet(
    struct G711Decoder* decoder,
    const uint8_t* data,
    int dataSize,
    struct DecodedAudioFrame* outputFrame
) {
    if (!decoder || !data || dataSize <= 0 || !outputFrame) {
        return false;
    }

    G711DecoderImpl* impl = reinterpret_cast<G711DecoderImpl*>(decoder);

    outputFrame->sampleRate = impl->sampleRate;
    outputFrame->channels = impl->channels;
    outputFrame->sampleCount = dataSize; // G.711: 1 байт = 1 sample

    size_t totalSamples = dataSize * impl->channels;
    outputFrame->samples = (int16_t*)malloc(totalSamples * sizeof(int16_t));
    if (!outputFrame->samples) {
        return false;
    }

    if (impl->isPCMU) {
        decode_pcmu_to_pcm(data, outputFrame->samples, dataSize);
    } else {
        decode_pcma_to_pcm(data, outputFrame->samples, dataSize);
    }

    // Если стерео, нужно дублировать samples
    if (impl->channels == 2) {
        // Дублирование моно в стерео
        for (int i = dataSize - 1; i >= 0; i--) {
            outputFrame->samples[i * 2] = outputFrame->samples[i];
            outputFrame->samples[i * 2 + 1] = outputFrame->samples[i];
        }
    }

    return true;
}

// Структура для ресемплера
struct AudioResamplerImpl {
    SwrContext* swrContext;
    int inputSampleRate;
    int inputChannels;
    int outputSampleRate;
    int outputChannels;

    AudioResamplerImpl() : swrContext(nullptr), inputSampleRate(0),
                          inputChannels(0), outputSampleRate(0), outputChannels(0) {}
};

// Инициализация ресемплера аудио
struct AudioResampler* init_audio_resampler(
    int inputSampleRate,
    int inputChannels,
    int outputSampleRate,
    int outputChannels
) {
    if (inputSampleRate <= 0 || inputChannels <= 0 ||
        outputSampleRate <= 0 || outputChannels <= 0) {
        return nullptr;
    }

    AudioResamplerImpl* resampler = new AudioResamplerImpl();
    resampler->inputSampleRate = inputSampleRate;
    resampler->inputChannels = inputChannels;
    resampler->outputSampleRate = outputSampleRate;
    resampler->outputChannels = outputChannels;

    // Создание контекста ресемплера
    resampler->swrContext = swr_alloc();
    if (!resampler->swrContext) {
        delete resampler;
        return nullptr;
    }

    // Настройка параметров через новый API
    AVChannelLayout inLayout, outLayout;
    av_channel_layout_default(&inLayout, inputChannels);
    av_channel_layout_default(&outLayout, outputChannels);

    // Пересоздаем контекст с правильными параметрами
    SwrContext* newContext = nullptr;
    swr_alloc_set_opts2(&newContext,
                        &outLayout, AV_SAMPLE_FMT_S16, outputSampleRate,
                        &inLayout, AV_SAMPLE_FMT_S16, inputSampleRate,
                        0, nullptr);

    if (newContext) {
        swr_free(&resampler->swrContext);
        resampler->swrContext = newContext;
    }

    av_channel_layout_uninit(&inLayout);
    av_channel_layout_uninit(&outLayout);

    // Инициализация
    if (swr_init(resampler->swrContext) < 0) {
        swr_free(&resampler->swrContext);
        delete resampler;
        return nullptr;
    }

    return reinterpret_cast<AudioResampler*>(resampler);
}

// Конвертация аудио
bool resample_audio(
    struct AudioResampler* resampler,
    const int16_t* inputSamples,
    int inputSampleCount,
    int16_t* outputSamples,
    int* outputSampleCount
) {
    if (!resampler || !inputSamples || inputSampleCount <= 0 ||
        !outputSamples || !outputSampleCount) {
        return false;
    }

    AudioResamplerImpl* impl = reinterpret_cast<AudioResamplerImpl*>(resampler);
    if (!impl->swrContext) {
        return false;
    }

    // Вычисление размера выходного буфера
    int maxOutputSamples = av_rescale_rnd(
        inputSampleCount,
        impl->outputSampleRate,
        impl->inputSampleRate,
        AV_ROUND_UP
    );

    const uint8_t** inputBuffer = (const uint8_t**)&inputSamples;
    uint8_t* outputBuffer = (uint8_t*)outputSamples;

    // Конвертация
    int convertedSamples = swr_convert(
        impl->swrContext,
        &outputBuffer, maxOutputSamples,
        inputBuffer, inputSampleCount
    );

    if (convertedSamples < 0) {
        return false;
    }

    *outputSampleCount = convertedSamples * impl->outputChannels;

    return true;
}

// Освобождение ресемплера
void free_audio_resampler(struct AudioResampler* resampler) {
    if (resampler) {
        AudioResamplerImpl* impl = reinterpret_cast<AudioResamplerImpl*>(resampler);
        if (impl->swrContext) {
            swr_free(&impl->swrContext);
        }
        delete impl;
    }
}

// Освобождение декодированного аудио кадра
void free_decoded_audio_frame(struct DecodedAudioFrame* frame) {
    if (frame && frame->samples) {
        free(frame->samples);
        frame->samples = nullptr;
        frame->sampleCount = 0;
    }
}

#endif // ENABLE_FFMPEG
