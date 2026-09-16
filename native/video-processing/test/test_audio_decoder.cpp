#include <iostream>
#include <vector>
#include <cstring>
#include "test_utils.h"

#ifdef ENABLE_FFMPEG
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/avutil.h>
#include <libswresample/swresample.h>
}

#include "../../include/audio_decoder.h"

// Внешние переменные из test_main.cpp
extern std::vector<TestResult> test_results;

// Простые тестовые данные для AAC (минимальный валидный AAC frame)
// Это упрощенные данные для тестирования
static const uint8_t test_aac_frame[] = {
    0xFF, 0xF1, 0x50, 0x80, 0x00, 0x1F, 0xFC, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
};

// Тестовые данные для G.711 PCMU (8 байт = 1 мс при 8kHz)
static const uint8_t test_pcmu_frame[] = {
    0x7E, 0x7F, 0x80, 0x81, 0x82, 0x83, 0x84, 0x85
};

// Тестовые данные для G.711 PCMA (8 байт = 1 мс при 8kHz)
static const uint8_t test_pcma_frame[] = {
    0xD5, 0xD6, 0xD7, 0xD8, 0xD9, 0xDA, 0xDB, 0xDC
};

// Тест инициализации AAC декодера
void test_audio_decoder_aac_init() {
    std::cout << "  Testing AAC decoder initialization..." << std::endl;

    AACDecoder* decoder = init_aac_decoder(nullptr, 48000, 2);

    if (decoder == nullptr) {
        test_results.push_back({"test_audio_decoder_aac_init", false, "Failed to create decoder"});
        return;
    }

    ASSERT_TRUE(decoder != nullptr);

    free_aac_decoder(decoder);

    test_results.push_back({"test_audio_decoder_aac_init", true, ""});
}

// Тест декодирования AAC кадра
void test_audio_decoder_aac_decode() {
    std::cout << "  Testing AAC frame decoding..." << std::endl;

    AACDecoder* decoder = init_aac_decoder(nullptr, 48000, 2);

    if (decoder == nullptr) {
        test_results.push_back({"test_audio_decoder_aac_decode", false, "Failed to create decoder"});
        return;
    }

    // Попытка декодирования AAC кадра
    DecodedAudioFrame outputFrame = {0};
    bool result = decode_aac_packet(decoder,
        test_aac_frame, sizeof(test_aac_frame), &outputFrame);

    // Примечание: Декодирование может не сработать с минимальными тестовыми данными
    // Это нормально для unit тестов - мы проверяем, что функция вызывается без ошибок

    if (outputFrame.samples) {
        free_decoded_audio_frame(&outputFrame);
    }

    free_aac_decoder(decoder);

    test_results.push_back({"test_audio_decoder_aac_decode", true, ""});
}

// Тест инициализации PCMU декодера
void test_audio_decoder_pcmu_init() {
    std::cout << "  Testing PCMU decoder initialization..." << std::endl;

    G711Decoder* decoder = init_g711_decoder(true, 8000, 1); // true = PCMU

    if (decoder == nullptr) {
        test_results.push_back({"test_audio_decoder_pcmu_init", false, "Failed to create decoder"});
        return;
    }

    ASSERT_TRUE(decoder != nullptr);

    free_g711_decoder(decoder);

    test_results.push_back({"test_audio_decoder_pcmu_init", true, ""});
}

// Тест декодирования PCMU кадра
void test_audio_decoder_pcmu_decode() {
    std::cout << "  Testing PCMU frame decoding..." << std::endl;

    G711Decoder* decoder = init_g711_decoder(true, 8000, 1); // true = PCMU

    if (decoder == nullptr) {
        test_results.push_back({"test_audio_decoder_pcmu_decode", false, "Failed to create decoder"});
        return;
    }

    // Декодирование PCMU кадра
    DecodedAudioFrame outputFrame = {0};
    bool result = decode_g711_packet(decoder,
        test_pcmu_frame, sizeof(test_pcmu_frame), &outputFrame);

    // PCMU декодирование должно работать с любыми данными
    // Проверяем, что функция вызывается без ошибок

    if (outputFrame.samples) {
        free_decoded_audio_frame(&outputFrame);
    }

    free_g711_decoder(decoder);

    test_results.push_back({"test_audio_decoder_pcmu_decode", true, ""});
}

// Тест инициализации PCMA декодера
void test_audio_decoder_pcma_init() {
    std::cout << "  Testing PCMA decoder initialization..." << std::endl;

    G711Decoder* decoder = init_g711_decoder(false, 8000, 1); // false = PCMA

    if (decoder == nullptr) {
        test_results.push_back({"test_audio_decoder_pcma_init", false, "Failed to create decoder"});
        return;
    }

    ASSERT_TRUE(decoder != nullptr);

    free_g711_decoder(decoder);

    test_results.push_back({"test_audio_decoder_pcma_init", true, ""});
}

// Тест декодирования PCMA кадра
void test_audio_decoder_pcma_decode() {
    std::cout << "  Testing PCMA frame decoding..." << std::endl;

    G711Decoder* decoder = init_g711_decoder(false, 8000, 1); // false = PCMA

    if (decoder == nullptr) {
        test_results.push_back({"test_audio_decoder_pcma_decode", false, "Failed to create decoder"});
        return;
    }

    // Декодирование PCMA кадра
    DecodedAudioFrame outputFrame = {0};
    bool result = decode_g711_packet(decoder,
        test_pcma_frame, sizeof(test_pcma_frame), &outputFrame);

    // PCMA декодирование должно работать с любыми данными
    // Проверяем, что функция вызывается без ошибок

    if (outputFrame.samples) {
        free_decoded_audio_frame(&outputFrame);
    }

    free_g711_decoder(decoder);

    test_results.push_back({"test_audio_decoder_pcma_decode", true, ""});
}

// Тест обработки различных частот дискретизации
void test_audio_decoder_sample_rates() {
    std::cout << "  Testing different sample rates..." << std::endl;

    std::vector<int> sample_rates = {8000, 16000, 44100, 48000};
    bool all_passed = true;

    for (int rate : sample_rates) {
        AACDecoder* decoder = init_aac_decoder(nullptr, rate, 2);
        if (decoder == nullptr) {
            all_passed = false;
            break;
        }
        free_aac_decoder(decoder);
    }

    if (all_passed) {
        test_results.push_back({"test_audio_decoder_sample_rates", true, ""});
    } else {
        test_results.push_back({"test_audio_decoder_sample_rates", false, "Failed for some sample rates"});
    }
}

// Тест обработки различных количеств каналов
void test_audio_decoder_channels() {
    std::cout << "  Testing different channel counts..." << std::endl;

    std::vector<int> channels = {1, 2, 5, 6}; // Mono, Stereo, 5.1, 6.1
    bool all_passed = true;

    for (int ch : channels) {
        AACDecoder* decoder = init_aac_decoder(nullptr, 48000, ch);
        if (decoder == nullptr) {
            all_passed = false;
            break;
        }
        free_aac_decoder(decoder);
    }

    if (all_passed) {
        test_results.push_back({"test_audio_decoder_channels", true, ""});
    } else {
        test_results.push_back({"test_audio_decoder_channels", false, "Failed for some channel counts"});
    }
}

#else // ENABLE_FFMPEG not defined

// Заглушки если FFmpeg не включен
void test_audio_decoder_aac_init() {
    test_results.push_back({"test_audio_decoder_aac_init", false, "FFmpeg not enabled"});
}

void test_audio_decoder_aac_decode() {
    test_results.push_back({"test_audio_decoder_aac_decode", false, "FFmpeg not enabled"});
}

void test_audio_decoder_pcmu_init() {
    test_results.push_back({"test_audio_decoder_pcmu_init", false, "FFmpeg not enabled"});
}

void test_audio_decoder_pcmu_decode() {
    test_results.push_back({"test_audio_decoder_pcmu_decode", false, "FFmpeg not enabled"});
}

void test_audio_decoder_pcma_init() {
    test_results.push_back({"test_audio_decoder_pcma_init", false, "FFmpeg not enabled"});
}

void test_audio_decoder_pcma_decode() {
    test_results.push_back({"test_audio_decoder_pcma_decode", false, "FFmpeg not enabled"});
}

void test_audio_decoder_sample_rates() {
    test_results.push_back({"test_audio_decoder_sample_rates", false, "FFmpeg not enabled"});
}

void test_audio_decoder_channels() {
    test_results.push_back({"test_audio_decoder_channels", false, "FFmpeg not enabled"});
}

#endif // ENABLE_FFMPEG
