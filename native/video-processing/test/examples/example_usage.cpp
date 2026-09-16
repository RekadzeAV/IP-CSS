// Примеры использования тестовых утилит и API
// Этот файл демонстрирует, как использовать созданную инфраструктуру

#include <iostream>
#include <vector>
#include "../test_utils.h"

#ifdef ENABLE_FFMPEG
#include "../../include/video_decoder.h"
#include "../../include/audio_decoder.h"
#include "../../include/rtsp_client.h"
#endif

// Пример 1: Создание и использование тестовых H.264 данных
void example_create_h264_test_data() {
    std::cout << "=== Example 1: Creating H.264 Test Data ===" << std::endl;

    // Создание SPS для 1920x1080 @ 25fps
    auto sps = test_utils::create_test_h264_sps(1920, 1080, 25);
    std::cout << "Created SPS: " << sps.size() << " bytes" << std::endl;

    // Создание PPS
    auto pps = test_utils::create_test_h264_pps();
    std::cout << "Created PPS: " << pps.size() << " bytes" << std::endl;

    // Создание IDR кадра
    auto idr = test_utils::create_test_h264_idr_frame(1920, 1080);
    std::cout << "Created IDR frame: " << idr.size() << " bytes" << std::endl;

    // Валидация
    if (test_utils::is_valid_h264_nal(sps.data(), sps.size())) {
        std::cout << "SPS is valid H.264 NAL unit" << std::endl;
    }

    uint8_t nal_type = test_utils::get_h264_nal_type(sps.data(), sps.size());
    std::cout << "NAL type: " << static_cast<int>(nal_type) << " (7 = SPS)" << std::endl;
}

// Пример 2: Создание RTP пакетов
void example_create_rtp_packets() {
    std::cout << "\n=== Example 2: Creating RTP Packets ===" << std::endl;

    // Создание тестовых данных
    std::vector<uint8_t> payload = {0x65, 0x88, 0x84, 0x00, 0x10};

    // Создание RTP пакета
    auto rtp_packet = test_utils::create_rtp_packet(
        96,      // payload type (H.264)
        100,     // sequence number
        1000,    // timestamp
        0x12345678, // SSRC
        payload.data(),
        payload.size(),
        false    // marker
    );

    std::cout << "Created RTP packet: " << rtp_packet.size() << " bytes" << std::endl;

    // Парсинг RTP заголовка
    test_utils::RTPHeader header;
    if (test_utils::parse_rtp_header(rtp_packet.data(), rtp_packet.size(), header)) {
        std::cout << "RTP Header parsed:" << std::endl;
        std::cout << "  Version: " << static_cast<int>(header.version) << std::endl;
        std::cout << "  Payload Type: " << static_cast<int>(header.payload_type) << std::endl;
        std::cout << "  Sequence: " << header.sequence << std::endl;
        std::cout << "  Timestamp: " << header.timestamp << std::endl;
        std::cout << "  SSRC: 0x" << std::hex << header.ssrc << std::dec << std::endl;
    }

    // Извлечение payload
    auto extracted_payload = test_utils::extract_rtp_payload(
        rtp_packet.data(),
        rtp_packet.size()
    );
    std::cout << "Extracted payload: " << extracted_payload.size() << " bytes" << std::endl;
}

// Пример 3: Создание FU-A фрагментов
void example_create_fua_fragments() {
    std::cout << "\n=== Example 3: Creating FU-A Fragments ===" << std::endl;

    // Большой NAL unit (симуляция)
    std::vector<uint8_t> large_nal(2000);
    for (size_t i = 0; i < large_nal.size(); i++) {
        large_nal[i] = static_cast<uint8_t>(i % 256);
    }

    // Разбиение на фрагменты
    const size_t fragment_size = 1200;
    size_t offset = 0;
    int fragment_num = 0;

    while (offset < large_nal.size()) {
        size_t size = std::min(fragment_size, large_nal.size() - offset);
        bool is_start = (offset == 0);
        bool is_end = (offset + size >= large_nal.size());

        auto fragment = test_utils::create_fua_fragment(
            1,  // NAL type
            is_start,
            is_end,
            large_nal.data() + offset,
            size
        );

        std::cout << "Fragment " << fragment_num++
                  << ": " << fragment.size() << " bytes"
                  << " (start=" << is_start << ", end=" << is_end << ")" << std::endl;

        offset += size;
    }
}

// Пример 4: Создание STAP-A пакета
void example_create_stap_a() {
    std::cout << "\n=== Example 4: Creating STAP-A Packet ===" << std::endl;

    // Создание нескольких маленьких NAL units
    std::vector<std::vector<uint8_t>> nal_units = {
        {0x67, 0x64, 0x00, 0x1e}, // SPS
        {0x68, 0xeb, 0xe3, 0xcb}, // PPS
        {0x65, 0x88, 0x84, 0x00}  // IDR
    };

    // Создание STAP-A пакета
    auto stap_packet = test_utils::create_stap_a_packet(nal_units);

    std::cout << "Created STAP-A packet: " << stap_packet.size() << " bytes" << std::endl;
    std::cout << "Contains " << nal_units.size() << " NAL units" << std::endl;
}

// Пример 5: Использование с видео декодером
#ifdef ENABLE_FFMPEG
void example_video_decoder_usage() {
    std::cout << "\n=== Example 5: Video Decoder Usage ===" << std::endl;

    // Создание декодера
    VideoDecoder* decoder = video_decoder_create(VIDEO_CODEC_H264, 1920, 1080);
    if (!decoder) {
        std::cout << "Failed to create decoder" << std::endl;
        return;
    }

    std::cout << "Decoder created successfully" << std::endl;

    // Создание тестовых данных
    auto sps = test_utils::create_test_h264_sps(1920, 1080, 25);
    auto pps = test_utils::create_test_h264_pps();
    auto idr = test_utils::create_test_h264_idr_frame(1920, 1080);

    // Декодирование SPS/PPS
    video_decoder_decode(decoder, sps.data(), sps.size(), 0);
    video_decoder_decode(decoder, pps.data(), pps.size(), 0);

    // Декодирование IDR кадра
    bool success = video_decoder_decode(decoder, idr.data(), idr.size(), 0);
    std::cout << "IDR frame decode: " << (success ? "success" : "failed") << std::endl;

    // Освобождение
    video_decoder_destroy(decoder);
    std::cout << "Decoder destroyed" << std::endl;
}
#endif

// Пример 6: Форматирование данных
void example_formatting() {
    std::cout << "\n=== Example 6: Data Formatting ===" << std::endl;

    // Форматирование времени
    int64_t time_ms = 123456;
    std::string time_str = test_utils::format_time_ms(time_ms);
    std::cout << "Time: " << time_ms << " ms = " << time_str << std::endl;

    // Форматирование размера
    size_t size_bytes = 1048576; // 1 MB
    std::string size_str = test_utils::format_size(size_bytes);
    std::cout << "Size: " << size_bytes << " bytes = " << size_str << std::endl;
}

// Пример 7: Проверка FFmpeg
void example_ffmpeg_check() {
    std::cout << "\n=== Example 7: FFmpeg Check ===" << std::endl;

    if (test_utils::is_ffmpeg_available()) {
        std::cout << "FFmpeg is available" << std::endl;
        std::string version = test_utils::get_ffmpeg_version();
        std::cout << "FFmpeg version: " << version << std::endl;
    } else {
        std::cout << "FFmpeg is not available" << std::endl;
    }
}

int main() {
    std::cout << "=== Test Utilities Examples ===" << std::endl;
    std::cout << std::endl;

    example_create_h264_test_data();
    example_create_rtp_packets();
    example_create_fua_fragments();
    example_create_stap_a();

#ifdef ENABLE_FFMPEG
    example_video_decoder_usage();
#endif

    example_formatting();
    example_ffmpeg_check();

    std::cout << "\n=== Examples Complete ===" << std::endl;

    return 0;
}
