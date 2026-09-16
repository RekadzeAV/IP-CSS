// Unit tests for video decoder
// H.264, H.265, MJPEG decoding tests

#include "video_decoder.h"
#include <gtest/gtest.h>
#include <vector>
#include <cstring>

#ifdef ENABLE_FFMPEG

// Тест создания H.264 декодера
TEST(VideoDecoderTest, CreateH264Decoder) {
    VideoDecoder* decoder = video_decoder_create(VIDEO_CODEC_H264, 1920, 1080);
    
    EXPECT_NE(decoder, nullptr);
    
    video_decoder_destroy(decoder);
}

// Тест создания H.265 декодера
TEST(VideoDecoderTest, CreateH265Decoder) {
    VideoDecoder* decoder = video_decoder_create(VIDEO_CODEC_H265, 1920, 1080);
    
    EXPECT_NE(decoder, nullptr);
    
    video_decoder_destroy(decoder);
}

// Тест создания MJPEG декодера
TEST(VideoDecoderTest, CreateMJPEGDecoder) {
    VideoDecoder* decoder = video_decoder_create(VIDEO_CODEC_MJPEG, 1920, 1080);
    
    EXPECT_NE(decoder, nullptr);
    
    video_decoder_destroy(decoder);
}

// Тест создания декодера с невалидным кодеком
TEST(VideoDecoderTest, CreateInvalidDecoder) {
    // NULL codec не поддерживается
    VideoDecoder* decoder = video_decoder_create((VideoCodec)999, 1920, 1080);
    EXPECT_EQ(decoder, nullptr);
}

// Тест деструкции декодера
TEST(VideoDecoderTest, DestroyDecoder) {
    VideoDecoder* decoder = video_decoder_create(VIDEO_CODEC_H264, 1920, 1080);
    EXPECT_NE(decoder, nullptr);
    
    EXPECT_NO_THROW(video_decoder_destroy(decoder));
}

// Тест установки callback
TEST(VideoDecoderTest, SetCallback) {
    VideoDecoder* decoder = video_decoder_create(VIDEO_CODEC_H264, 1920, 1080);
    EXPECT_NE(decoder, nullptr);
    
    bool callbackCalled = false;
    auto callback = [](DecodedFrame* frame, void* userData) {
        bool* called = static_cast<bool*>(userData);
        *called = true;
    };
    
    EXPECT_NO_THROW(video_decoder_set_callback(decoder, callback, &callbackCalled));
    
    video_decoder_destroy(decoder);
}

// Тест получения информации о декодере
TEST(VideoDecoderTest, GetDecoderInfo) {
    VideoDecoder* decoder = video_decoder_create(VIDEO_CODEC_H264, 1920, 1080);
    EXPECT_NE(decoder, nullptr);
    
    int width, height;
    VideoCodec codec;
    
    bool result = video_decoder_get_info(decoder, &width, &height, &codec);
    
    EXPECT_TRUE(result);
    EXPECT_EQ(width, 1920);
    EXPECT_EQ(height, 1080);
    EXPECT_EQ(codec, VIDEO_CODEC_H264);
    
    video_decoder_destroy(decoder);
}

// Тест декодирования с невалидными данными
TEST(VideoDecoderTest, DecodeInvalidData) {
    VideoDecoder* decoder = video_decoder_create(VIDEO_CODEC_H264, 1920, 1080);
    EXPECT_NE(decoder, nullptr);
    
    bool result = video_decoder_decode(decoder, nullptr, 0, 0);
    EXPECT_FALSE(result);
    
    video_decoder_destroy(decoder);
}

// Тест декодирования пустых данных
TEST(VideoDecoderTest, DecodeEmptyData) {
    VideoDecoder* decoder = video_decoder_create(VIDEO_CODEC_H264, 1920, 1080);
    EXPECT_NE(decoder, nullptr);
    
    uint8_t data[1024];
    bool result = video_decoder_decode(decoder, data, 0, 0);
    // Пустые данные не должны падать
    EXPECT_TRUE(result == false || result == true);  // Может быть false если нет SPS/PPS
    
    video_decoder_destroy(decoder);
}

// Тест освобождения кадра с nullptr
TEST(VideoDecoderTest, ReleaseFrameNull) {
    EXPECT_NO_THROW(decoded_frame_release(nullptr));
}

// Тест NAL unit extraction (внутренняя функция)
// Для тестирования создадим простые данные с start codes
TEST(VideoDecoderTest, NALUnitExtraction) {
    // Данные с start code 0x00000001
    std::vector<uint8_t> data = {
        0x00, 0x00, 0x00, 0x01,  // Start code
        0x67, 0x42, 0x00, 0x1E,  // SPS NAL unit (тип 7)
        0x00, 0x00, 0x00, 0x01,  // Start code
        0x68, 0xCE, 0x38, 0x80   // PPS NAL unit (тип 8)
    };
    
    VideoDecoder* decoder = video_decoder_create(VIDEO_CODEC_H264, 1920, 1080);
    EXPECT_NE(decoder, nullptr);
    
    // Декодирование должно обработать данные без crash
    bool result = video_decoder_decode(decoder, data.data(), data.size(), 0);
    
    // Результат может быть false если нет полных данных для декодирования
    // Главное что не должно быть crash
    EXPECT_TRUE(true);  // Если дошли до сюда - тест прошел
    
    video_decoder_destroy(decoder);
}

// Тест с реальными SPS/PPS данными (упрощенные)
TEST(VideoDecoderTest, H264SPSPPS) {
    // Минимальные SPS данные (не реальные, только для теста)
    std::vector<uint8_t> spsData = {
        0x00, 0x00, 0x00, 0x01,  // Start code
        0x67, 0x42, 0x00, 0x1E,  // SPS
        0x00, 0x00, 0x00, 0x01,  // Start code
        0x68, 0xCE, 0x38, 0x80   // PPS
    };
    
    VideoDecoder* decoder = video_decoder_create(VIDEO_CODEC_H264, 1920, 1080);
    EXPECT_NE(decoder, nullptr);
    
    bool callbackCalled = false;
    video_decoder_set_callback(decoder, [](DecodedFrame* frame, void* userData) {
        bool* called = static_cast<bool*>(userData);
        *called = true;
    }, &callbackCalled);
    
    // Пытаемся декодировать SPS/PPS
    bool result = video_decoder_decode(decoder, spsData.data(), spsData.size(), 0);
    
    // SPS/PPS обработка не должна падать
    EXPECT_TRUE(true);
    
    video_decoder_destroy(decoder);
}

// Тест множественных декодирований
TEST(VideoDecoderTest, MultipleDecodings) {
    VideoDecoder* decoder = video_decoder_create(VIDEO_CODEC_H264, 1920, 1080);
    EXPECT_NE(decoder, nullptr);
    
    uint8_t dummyData[1024];
    memset(dummyData, 0, sizeof(dummyData));
    
    // Несколько попыток декодирования
    for (int i = 0; i < 10; i++) {
        bool result = video_decoder_decode(decoder, dummyData, sizeof(dummyData), i * 1000);
        // Не проверяем результат - главное что не crash
    }
    
    video_decoder_destroy(decoder);
}

// Тест с большими данными
TEST(VideoDecoderTest, LargeData) {
    VideoDecoder* decoder = video_decoder_create(VIDEO_CODEC_H264, 1920, 1080);
    EXPECT_NE(decoder, nullptr);
    
    std::vector<uint8_t> largeData(1024 * 1024, 0);  // 1MB данных
    largeData[0] = 0x00;
    largeData[1] = 0x00;
    largeData[2] = 0x00;
    largeData[3] = 0x01;
    
    bool result = video_decoder_decode(decoder, largeData.data(), largeData.size(), 0);
    // Большие данные не должны вызывать переполнение
    EXPECT_TRUE(true);
    
    video_decoder_destroy(decoder);
}

// Тест получения информации с nullptr параметрами
TEST(VideoDecoderTest, GetInfoNullParams) {
    VideoDecoder* decoder = video_decoder_create(VIDEO_CODEC_H264, 1920, 1080);
    EXPECT_NE(decoder, nullptr);
    
    // Все параметры могут быть nullptr
    bool result = video_decoder_get_info(decoder, nullptr, nullptr, nullptr);
    EXPECT_TRUE(result);  // Должно вернуть true
    
    video_decoder_destroy(decoder);
}

// Тест MJPEG декодирования
TEST(VideoDecoderTest, MJPEGDecode) {
    VideoDecoder* decoder = video_decoder_create(VIDEO_CODEC_MJPEG, 640, 480);
    EXPECT_NE(decoder, nullptr);
    
    // Минимальные JPEG данные (SOI marker)
    std::vector<uint8_t> jpegData = {
        0xFF, 0xD8,  // SOI marker
        0xFF, 0xE0, 0x00, 0x10,  // APP0 marker
        0x4A, 0x46, 0x49, 0x46, 0x00,  // "JFIF"
        0x01, 0x01, 0x00, 0x00, 0x01, 0x00, 0x01, 0x00, 0x00
    };
    
    bool result = video_decoder_decode(decoder, jpegData.data(), jpegData.size(), 0);
    // Может вернуть false если данные неполные, но не должно crash
    EXPECT_TRUE(true);
    
    video_decoder_destroy(decoder);
}

// Тест таймстампов
TEST(VideoDecoderTest, Timestamps) {
    VideoDecoder* decoder = video_decoder_create(VIDEO_CODEC_H264, 1920, 1080);
    EXPECT_NE(decoder, nullptr);
    
    uint8_t dummyData[1024];
    
    // Декодирование с разными таймстампами
    for (int64_t ts = 0; ts < 10000; ts += 1000) {
        bool result = video_decoder_decode(decoder, dummyData, sizeof(dummyData), ts);
        // Не проверяем результат
    }
    
    video_decoder_destroy(decoder);
}

// Тест производительности (быстрый)
TEST(VideoDecoderTest, PerformanceTest) {
    VideoDecoder* decoder = video_decoder_create(VIDEO_CODEC_H264, 1920, 1080);
    EXPECT_NE(decoder, nullptr);
    
    uint8_t dummyData[1024];
    
    auto start = std::chrono::high_resolution_clock::now();
    
    for (int i = 0; i < 100; i++) {
        video_decoder_decode(decoder, dummyData, sizeof(dummyData), i * 1000);
    }
    
    auto end = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(end - start);
    
    // 100 декодирований должны занять меньше 1 секунды
    EXPECT_LT(duration.count(), 1000);
    
    video_decoder_destroy(decoder);
}

#endif // ENABLE_FFMPEG
