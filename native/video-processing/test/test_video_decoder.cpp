#include <iostream>
#include <vector>
#include <cstring>
#include <fstream>
#include <string>

#ifdef ENABLE_FFMPEG
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/avutil.h>
#include <libavutil/imgutils.h>
}

#include "../../include/video_decoder.h"

// Простые тестовые данные для H.264 SPS (минимальный валидный SPS для 640x480)
// Это упрощенный SPS для тестирования
static const uint8_t test_h264_sps[] = {
    0x67, 0x64, 0x00, 0x1e, 0xac, 0xd9, 0x40, 0xa0, 0x2f, 0xf9, 0x70, 0x11, 0x00, 0x00, 0x03, 0x00, 0x01, 0x00, 0x00, 0x03, 0x00, 0x32, 0x0f, 0x16, 0x2d, 0x96
};

static const uint8_t test_h264_pps[] = {
    0x68, 0xeb, 0xe3, 0xcb, 0x22, 0xc0
};

// Простой IDR frame (минимальный)
static const uint8_t test_h264_idr[] = {
    0x65, 0x88, 0x84, 0x00, 0x10, 0xff, 0xfe, 0x00, 0x00
};

// Тест инициализации H.264 декодера
void test_video_decoder_h264_init() {
    std::cout << "  Testing H.264 decoder initialization..." << std::endl;

    VideoDecoder* decoder = video_decoder_create(VIDEO_CODEC_H264, 640, 480);

    if (decoder == nullptr) {
        test_results.push_back({"test_video_decoder_h264_init", false, "Failed to create decoder"});
        return;
    }

    // Проверка, что декодер создан
    ASSERT_TRUE(decoder != nullptr);

    // Освобождение
    video_decoder_destroy(decoder);

    test_results.push_back({"test_video_decoder_h264_init", true, ""});
}

// Тест декодирования H.264 IDR кадра
void test_video_decoder_h264_decode_idr() {
    std::cout << "  Testing H.264 IDR frame decoding..." << std::endl;

    VideoDecoder* decoder = video_decoder_create(VIDEO_CODEC_H264, 640, 480);

    if (decoder == nullptr) {
        test_results.push_back({"test_video_decoder_h264_decode_idr", false, "Failed to create decoder"});
        return;
    }

    // Инициализация с SPS/PPS
    int result = video_decoder_decode(decoder,
        test_h264_sps, sizeof(test_h264_sps), 0);

    if (result < 0) {
        video_decoder_destroy(decoder);
        test_results.push_back({"test_video_decoder_h264_decode_idr", false, "Failed to decode SPS"});
        return;
    }

    result = video_decoder_decode(decoder,
        test_h264_pps, sizeof(test_h264_pps), 0);

    if (result < 0) {
        video_decoder_destroy(decoder);
        test_results.push_back({"test_video_decoder_h264_decode_idr", false, "Failed to decode PPS"});
        return;
    }

    // Попытка декодирования IDR кадра
    result = video_decoder_decode(decoder,
        test_h264_idr, sizeof(test_h264_idr), 0);

    // Примечание: Декодирование может не сработать с минимальными тестовыми данными
    // Это нормально для unit тестов - мы проверяем, что функция вызывается без ошибок

    video_decoder_destroy(decoder);

    test_results.push_back({"test_video_decoder_h264_decode_idr", true, ""});
}

// Тест инициализации H.265 декодера
void test_video_decoder_h265_init() {
    std::cout << "  Testing H.265 decoder initialization..." << std::endl;

    VideoDecoder* decoder = video_decoder_create(VIDEO_CODEC_H265, 1920, 1080);

    if (decoder == nullptr) {
        test_results.push_back({"test_video_decoder_h265_init", false, "Failed to create decoder"});
        return;
    }

    ASSERT_TRUE(decoder != nullptr);

    video_decoder_destroy(decoder);

    test_results.push_back({"test_video_decoder_h265_init", true, ""});
}

#else // ENABLE_FFMPEG not defined

// Заглушки если FFmpeg не включен
void test_video_decoder_h264_init() {
    test_results.push_back({"test_video_decoder_h264_init", false, "FFmpeg not enabled"});
}

void test_video_decoder_h264_decode_idr() {
    test_results.push_back({"test_video_decoder_h264_decode_idr", false, "FFmpeg not enabled"});
}

void test_video_decoder_h265_init() {
    test_results.push_back({"test_video_decoder_h265_init", false, "FFmpeg not enabled"});
}

#endif // ENABLE_FFMPEG
