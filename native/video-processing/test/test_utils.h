#ifndef TEST_UTILS_H
#define TEST_UTILS_H

#include <string>
#include <vector>
#include <cstdint>

#ifdef ENABLE_FFMPEG
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/avutil.h>
}
#endif

namespace test_utils {

// Загрузка файла в память
std::vector<uint8_t> load_file(const std::string& filename);

// Сохранение данных в файл
bool save_file(const std::string& filename, const uint8_t* data, size_t size);

// Создание тестового H.264 SPS (минимальный валидный)
std::vector<uint8_t> create_test_h264_sps(int width, int height, int fps);

// Создание тестового H.264 PPS
std::vector<uint8_t> create_test_h264_pps();

// Создание тестового H.264 IDR кадра
std::vector<uint8_t> create_test_h264_idr_frame(int width, int height);

// Создание тестового H.265 VPS
std::vector<uint8_t> create_test_h265_vps(int width, int height);

// Создание тестового H.265 SPS
std::vector<uint8_t> create_test_h265_sps(int width, int height, int fps);

// Создание тестового H.265 PPS
std::vector<uint8_t> create_test_h265_pps();

// Создание тестового H.265 IDR кадра
std::vector<uint8_t> create_test_h265_idr_frame(int width, int height);

// Создание RTP пакета
std::vector<uint8_t> create_rtp_packet(
    uint8_t payload_type,
    uint16_t sequence,
    uint32_t timestamp,
    uint32_t ssrc,
    const uint8_t* payload,
    size_t payload_size,
    bool marker = false
);

// Парсинг RTP заголовка
struct RTPHeader {
    uint8_t version;
    bool padding;
    bool extension;
    uint8_t cc;
    bool marker;
    uint8_t payload_type;
    uint16_t sequence;
    uint32_t timestamp;
    uint32_t ssrc;
};

bool parse_rtp_header(const uint8_t* data, size_t size, RTPHeader& header);

// Извлечение payload из RTP пакета
std::vector<uint8_t> extract_rtp_payload(const uint8_t* rtp_packet, size_t packet_size);

// Создание FU-A фрагмента
std::vector<uint8_t> create_fua_fragment(
    uint8_t nal_type,
    bool start,
    bool end,
    const uint8_t* data,
    size_t data_size
);

// Создание STAP-A пакета
std::vector<uint8_t> create_stap_a_packet(
    const std::vector<std::vector<uint8_t>>& nal_units
);

// Проверка валидности H.264 NAL unit
bool is_valid_h264_nal(const uint8_t* data, size_t size);

// Проверка валидности H.265 NAL unit
bool is_valid_h265_nal(const uint8_t* data, size_t size);

// Получение типа NAL unit (H.264)
uint8_t get_h264_nal_type(const uint8_t* data, size_t size);

// Получение типа NAL unit (H.265)
uint8_t get_h265_nal_type(const uint8_t* data, size_t size);

// Форматирование времени
std::string format_time_ms(int64_t ms);

// Форматирование размера
std::string format_size(size_t bytes);

// Проверка FFmpeg доступности
bool is_ffmpeg_available();

// Получение версии FFmpeg
std::string get_ffmpeg_version();

} // namespace test_utils

#endif // TEST_UTILS_H
