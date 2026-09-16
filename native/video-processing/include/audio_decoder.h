#ifndef AUDIO_DECODER_H
#define AUDIO_DECODER_H

#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

#ifdef ENABLE_FFMPEG

// Структура для декодированного аудио кадра
struct DecodedAudioFrame {
    int16_t* samples;        // PCM samples (16-bit)
    size_t sampleCount;      // Количество samples
    int sampleRate;          // Частота дискретизации
    int channels;            // Количество каналов
    int64_t timestamp;       // RTP timestamp
};

// Структура для AAC декодера
struct AACDecoder {
    void* codecContext;      // AVCodecContext* (скрыт для C совместимости)
    bool initialized;
    int sampleRate;
    int channels;
};

// Структура для G.711 декодера
struct G711Decoder {
    bool isPCMU;             // true для PCMU (μ-law), false для PCMA (A-law)
    int sampleRate;
    int channels;
};

// Структура для ресемплера аудио
struct AudioResampler {
    void* swrContext;        // SwrContext* (скрыт для C совместимости)
    int inputSampleRate;
    int inputChannels;
    int outputSampleRate;
    int outputChannels;
};

// Инициализация AAC декодера
// config: AAC config из SDP (a=fmtp:96 config=...)
// sampleRate: Частота дискретизации
// channels: Количество каналов
struct AACDecoder* init_aac_decoder(
    const char* config,
    int sampleRate,
    int channels
);

// Освобождение AAC декодера
void free_aac_decoder(struct AACDecoder* decoder);

// Инициализация G.711 декодера
// isPCMU: true для PCMU (μ-law), false для PCMA (A-law)
struct G711Decoder* init_g711_decoder(
    bool isPCMU,
    int sampleRate,
    int channels
);

// Освобождение G.711 декодера
void free_g711_decoder(struct G711Decoder* decoder);

// Декодирование AAC пакета
bool decode_aac_packet(
    struct AACDecoder* decoder,
    const uint8_t* data,
    int dataSize,
    struct DecodedAudioFrame* outputFrame
);

// Декодирование G.711 пакета
bool decode_g711_packet(
    struct G711Decoder* decoder,
    const uint8_t* data,
    int dataSize,
    struct DecodedAudioFrame* outputFrame
);

// Инициализация ресемплера аудио
struct AudioResampler* init_audio_resampler(
    int inputSampleRate,
    int inputChannels,
    int outputSampleRate,
    int outputChannels
);

// Конвертация аудио
bool resample_audio(
    struct AudioResampler* resampler,
    const int16_t* inputSamples,
    int inputSampleCount,
    int16_t* outputSamples,
    int* outputSampleCount
);

// Освобождение ресемплера
void free_audio_resampler(struct AudioResampler* resampler);

// Освобождение декодированного аудио кадра
void free_decoded_audio_frame(struct DecodedAudioFrame* frame);

#endif // ENABLE_FFMPEG

#ifdef __cplusplus
}
#endif

#endif // AUDIO_DECODER_H
