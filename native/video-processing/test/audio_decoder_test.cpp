// Unit tests for audio decoders
// FFmpeg 8.0 API compatibility tests

#include "audio_decoder.h"
#include <gtest/gtest.h>
#include <cstring>
#include <vector>

#ifdef ENABLE_FFMPEG

// Тест инициализации AAC декодера
TEST(AudioDecoderTest, InitAacDecoder) {
    const char* config = "1210";  // Пример AAC config
    int sampleRate = 48000;
    int channels = 2;

    struct AACDecoder* decoder = init_aac_decoder(config, sampleRate, channels);
    
    EXPECT_NE(decoder, nullptr);
    
    free_aac_decoder(decoder);
}

// Тест инициализации AAC декодера с невалидными параметрами
TEST(AudioDecoderTest, InitAacDecoderInvalidParams) {
    struct AACDecoder* decoder = init_aac_decoder(nullptr, 48000, 2);
    EXPECT_EQ(decoder, nullptr);

    decoder = init_aac_decoder("1210", 0, 2);
    EXPECT_EQ(decoder, nullptr);

    decoder = init_aac_decoder("1210", 48000, 0);
    EXPECT_EQ(decoder, nullptr);
}

// Тест инициализации G.711 декодера PCMU
TEST(AudioDecoderTest, InitG711DecoderPCMU) {
    struct G711Decoder* decoder = init_g711_decoder(true, 8000, 1);
    
    EXPECT_NE(decoder, nullptr);
    
    free_g711_decoder(decoder);
}

// Тест инициализации G.711 декодера PCMA
TEST(AudioDecoderTest, InitG711DecoderPCMA) {
    struct G711Decoder* decoder = init_g711_decoder(false, 8000, 1);
    
    EXPECT_NE(decoder, nullptr);
    
    free_g711_decoder(decoder);
}

// Тест декодирования PCMU в PCM
TEST(AudioDecoderTest, DecodePCMUtoPCM) {
    struct G711Decoder* decoder = init_g711_decoder(true, 8000, 1);
    EXPECT_NE(decoder, nullptr);

    // Тестовые данные PCMU (8 samples)
    uint8_t pcmuData[8] = {0x75, 0xdf, 0x4d, 0x92, 0xaa, 0x55, 0xb8, 0x47};
    struct DecodedAudioFrame frame;

    bool result = decode_g711_packet(decoder, pcmuData, 8, &frame);
    
    EXPECT_TRUE(result);
    EXPECT_EQ(frame.sampleRate, 8000);
    EXPECT_EQ(frame.channels, 1);
    EXPECT_EQ(frame.sampleCount, 8);
    EXPECT_NE(frame.samples, nullptr);

    free_decoded_audio_frame(&frame);
    free_g711_decoder(decoder);
}

// Тест декодирования PCMA в PCM
TEST(AudioDecoderTest, DecodePCMAtoPCM) {
    struct G711Decoder* decoder = init_g711_decoder(false, 8000, 1);
    EXPECT_NE(decoder, nullptr);

    // Тестовые данные PCMA (8 samples)
    uint8_t pcmaData[8] = {0xab, 0x26, 0xb4, 0x6d, 0x55, 0xaa, 0x47, 0xba};
    struct DecodedAudioFrame frame;

    bool result = decode_g711_packet(decoder, pcmaData, 8, &frame);
    
    EXPECT_TRUE(result);
    EXPECT_EQ(frame.sampleRate, 8000);
    EXPECT_EQ(frame.channels, 1);
    EXPECT_EQ(frame.sampleCount, 8);
    EXPECT_NE(frame.samples, nullptr);

    free_decoded_audio_frame(&frame);
    free_g711_decoder(decoder);
}

// Тест ресемплера
TEST(AudioDecoderTest, AudioResampler) {
    int inputSampleRate = 48000;
    int inputChannels = 2;
    int outputSampleRate = 44100;
    int outputChannels = 2;

    struct AudioResampler* resampler = init_audio_resampler(
        inputSampleRate, inputChannels, outputSampleRate, outputChannels
    );
    
    EXPECT_NE(resampler, nullptr);

    // Тест конвертации
    std::vector<int16_t> inputSamples(480);  // 10ms at 48kHz
    for (size_t i = 0; i < inputSamples.size(); i++) {
        inputSamples[i] = static_cast<int16_t>(i % 1000);
    }

    std::vector<int16_t> outputSamples(441);  // 10ms at 44.1kHz
    int outputCount = 0;

    bool result = resample_audio(
        resampler,
        inputSamples.data(),
        static_cast<int>(inputSamples.size()),
        outputSamples.data(),
        &outputCount
    );

    EXPECT_TRUE(result);
    EXPECT_GT(outputCount, 0);

    free_audio_resampler(resampler);
}

// Тест ресемплера с невалидными параметрами
TEST(AudioDecoderTest, AudioResamplerInvalidParams) {
    struct AudioResampler* resampler = init_audio_resampler(0, 2, 44100, 2);
    EXPECT_EQ(resampler, nullptr);

    resampler = init_audio_resampler(48000, 0, 44100, 2);
    EXPECT_EQ(resampler, nullptr);

    resampler = init_audio_resampler(48000, 2, 0, 2);
    EXPECT_EQ(resampler, nullptr);

    resampler = init_audio_resampler(48000, 2, 44100, 0);
    EXPECT_EQ(resampler, nullptr);
}

// Тест освобождения AAC декодера с nullptr
TEST(AudioDecoderTest, FreeAacDecoderNull) {
    EXPECT_NO_THROW(free_aac_decoder(nullptr));
}

// Тест освобождения G.711 декодера с nullptr
TEST(AudioDecoderTest, FreeG711DecoderNull) {
    EXPECT_NO_THROW(free_g711_decoder(nullptr));
}

// Тест освобождения ресемплера с nullptr
TEST(AudioDecoderTest, FreeAudioResamplerNull) {
    EXPECT_NO_THROW(free_audio_resampler(nullptr));
}

// Тест освобождения декодированного кадра с nullptr
TEST(AudioDecoderTest, FreeDecodedAudioFrameNull) {
    EXPECT_NO_THROW(free_decoded_audio_frame(nullptr));
}

// Тест декодирования AAC с невалидным декодером
TEST(AudioDecoderTest, DecodeAacPacketInvalidDecoder) {
    uint8_t data[1024];
    struct DecodedAudioFrame frame;

    bool result = decode_aac_packet(nullptr, data, 1024, &frame);
    EXPECT_FALSE(result);
}

// Тест декодирования G.711 с невалидным декодером
TEST(AudioDecoderTest, DecodeG711PacketInvalidDecoder) {
    uint8_t data[1024];
    struct DecodedAudioFrame frame;

    bool result = decode_g711_packet(nullptr, data, 1024, &frame);
    EXPECT_FALSE(result);
}

// Тест декодирования G.711 в стерео
TEST(AudioDecoderTest, DecodeG711Stereo) {
    struct G711Decoder* decoder = init_g711_decoder(true, 8000, 2);
    EXPECT_NE(decoder, nullptr);

    uint8_t pcmuData[8] = {0x75, 0xdf, 0x4d, 0x92, 0xaa, 0x55, 0xb8, 0x47};
    struct DecodedAudioFrame frame;

    bool result = decode_g711_packet(decoder, pcmuData, 8, &frame);
    
    EXPECT_TRUE(result);
    EXPECT_EQ(frame.sampleRate, 8000);
    EXPECT_EQ(frame.channels, 2);
    EXPECT_EQ(frame.sampleCount, 16);  // 8 * 2 канала

    free_decoded_audio_frame(&frame);
    free_g711_decoder(decoder);
}

// Тест инициализации ресемплера с одинаковыми частотами
TEST(AudioDecoderTest, AudioResamplerSameRates) {
    struct AudioResampler* resampler = init_audio_resampler(48000, 2, 48000, 2);
    EXPECT_NE(resampler, nullptr);

    std::vector<int16_t> inputSamples(480);
    for (size_t i = 0; i < inputSamples.size(); i++) {
        inputSamples[i] = static_cast<int16_t>(i % 1000);
    }

    std::vector<int16_t> outputSamples(480);
    int outputCount = 0;

    bool result = resample_audio(
        resampler,
        inputSamples.data(),
        static_cast<int>(inputSamples.size()),
        outputSamples.data(),
        &outputCount
    );

    EXPECT_TRUE(result);
    EXPECT_GT(outputCount, 0);

    free_audio_resampler(resampler);
}

#endif // ENABLE_FFMPEG
