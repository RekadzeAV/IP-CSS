#include "test_utils.h"
#include <fstream>
#include <sstream>
#include <iomanip>
#include <cstring>

#ifdef ENABLE_FFMPEG
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/avutil.h>
}
#endif

namespace test_utils {

std::vector<uint8_t> load_file(const std::string& filename) {
    std::ifstream file(filename, std::ios::binary | std::ios::ate);
    if (!file.is_open()) {
        return {};
    }

    size_t size = file.tellg();
    file.seekg(0, std::ios::beg);

    std::vector<uint8_t> data(size);
    file.read(reinterpret_cast<char*>(data.data()), size);

    return data;
}

bool save_file(const std::string& filename, const uint8_t* data, size_t size) {
    std::ofstream file(filename, std::ios::binary);
    if (!file.is_open()) {
        return false;
    }

    file.write(reinterpret_cast<const char*>(data), size);
    return file.good();
}

std::vector<uint8_t> create_test_h264_sps(int width, int height, int fps) {
    // Упрощенный SPS для тестирования
    // В реальности SPS должен быть правильно сформирован согласно H.264 спецификации
    std::vector<uint8_t> sps;
    sps.push_back(0x67); // NAL header: type 7 (SPS)
    sps.push_back(0x64); // profile_idc
    sps.push_back(0x00); // flags
    sps.push_back(0x1e); // level_idc
    sps.push_back(0xac); // seq_parameter_set_id и другие параметры
    // Добавляем минимальные данные для тестирования
    for (int i = 0; i < 20; i++) {
        sps.push_back(0x00);
    }
    return sps;
}

std::vector<uint8_t> create_test_h264_pps() {
    std::vector<uint8_t> pps;
    pps.push_back(0x68); // NAL header: type 8 (PPS)
    pps.push_back(0xeb);
    pps.push_back(0xe3);
    pps.push_back(0xcb);
    return pps;
}

std::vector<uint8_t> create_test_h264_idr_frame(int width, int height) {
    std::vector<uint8_t> idr;
    idr.push_back(0x65); // NAL header: type 5 (IDR)
    idr.push_back(0x88);
    idr.push_back(0x84);
    idr.push_back(0x00);
    // Минимальные данные для тестирования
    for (int i = 0; i < 100; i++) {
        idr.push_back(static_cast<uint8_t>(i % 256));
    }
    return idr;
}

std::vector<uint8_t> create_test_h265_vps(int width, int height) {
    std::vector<uint8_t> vps;
    vps.push_back(0x40); // NAL header: type 32 (VPS)
    vps.push_back(0x01);
    // Минимальные данные
    for (int i = 0; i < 10; i++) {
        vps.push_back(0x00);
    }
    return vps;
}

std::vector<uint8_t> create_test_h265_sps(int width, int height, int fps) {
    std::vector<uint8_t> sps;
    sps.push_back(0x42); // NAL header: type 33 (SPS)
    sps.push_back(0x01);
    // Минимальные данные
    for (int i = 0; i < 20; i++) {
        sps.push_back(0x00);
    }
    return sps;
}

std::vector<uint8_t> create_test_h265_pps() {
    std::vector<uint8_t> pps;
    pps.push_back(0x44); // NAL header: type 34 (PPS)
    pps.push_back(0x01);
    // Минимальные данные
    for (int i = 0; i < 10; i++) {
        pps.push_back(0x00);
    }
    return pps;
}

std::vector<uint8_t> create_test_h265_idr_frame(int width, int height) {
    std::vector<uint8_t> idr;
    idr.push_back(0x26); // NAL header: type 19 (IDR)
    idr.push_back(0x01);
    // Минимальные данные
    for (int i = 0; i < 100; i++) {
        idr.push_back(static_cast<uint8_t>(i % 256));
    }
    return idr;
}

std::vector<uint8_t> create_rtp_packet(
    uint8_t payload_type,
    uint16_t sequence,
    uint32_t timestamp,
    uint32_t ssrc,
    const uint8_t* payload,
    size_t payload_size,
    bool marker) {

    std::vector<uint8_t> packet;
    packet.resize(12 + payload_size);

    // RTP заголовок (12 байт)
    packet[0] = 0x80 | (marker ? 0x80 : 0x00); // Version=2, Marker, Padding=0, Extension=0
    packet[1] = payload_type & 0x7F;
    packet[2] = (sequence >> 8) & 0xFF;
    packet[3] = sequence & 0xFF;
    packet[4] = (timestamp >> 24) & 0xFF;
    packet[5] = (timestamp >> 16) & 0xFF;
    packet[6] = (timestamp >> 8) & 0xFF;
    packet[7] = timestamp & 0xFF;
    packet[8] = (ssrc >> 24) & 0xFF;
    packet[9] = (ssrc >> 16) & 0xFF;
    packet[10] = (ssrc >> 8) & 0xFF;
    packet[11] = ssrc & 0xFF;

    // Payload
    if (payload && payload_size > 0) {
        std::memcpy(packet.data() + 12, payload, payload_size);
    }

    return packet;
}

bool parse_rtp_header(const uint8_t* data, size_t size, RTPHeader& header) {
    if (size < 12) {
        return false;
    }

    header.version = (data[0] >> 6) & 0x03;
    header.padding = (data[0] >> 5) & 0x01;
    header.extension = (data[0] >> 4) & 0x01;
    header.cc = data[0] & 0x0F;
    header.marker = (data[1] >> 7) & 0x01;
    header.payload_type = data[1] & 0x7F;
    header.sequence = (data[2] << 8) | data[3];
    header.timestamp = (data[4] << 24) | (data[5] << 16) | (data[6] << 8) | data[7];
    header.ssrc = (data[8] << 24) | (data[9] << 16) | (data[10] << 8) | data[11];

    return true;
}

std::vector<uint8_t> extract_rtp_payload(const uint8_t* rtp_packet, size_t packet_size) {
    if (packet_size < 12) {
        return {};
    }

    size_t payload_size = packet_size - 12;
    std::vector<uint8_t> payload(payload_size);
    std::memcpy(payload.data(), rtp_packet + 12, payload_size);

    return payload;
}

std::vector<uint8_t> create_fua_fragment(
    uint8_t nal_type,
    bool start,
    bool end,
    const uint8_t* data,
    size_t data_size) {

    std::vector<uint8_t> fragment;
    fragment.resize(1 + data_size);

    // FU-A заголовок
    fragment[0] = (start ? 0x80 : 0x00) | (end ? 0x40 : 0x00) | (nal_type & 0x1F);

    // Данные
    if (data && data_size > 0) {
        std::memcpy(fragment.data() + 1, data, data_size);
    }

    return fragment;
}

std::vector<uint8_t> create_stap_a_packet(
    const std::vector<std::vector<uint8_t>>& nal_units) {

    std::vector<uint8_t> packet;

    // Вычисляем общий размер
    size_t total_size = 1; // STAP-A NAL header
    for (const auto& nal : nal_units) {
        total_size += 2; // Размер NAL unit (2 байта)
        total_size += nal.size();
    }

    packet.resize(total_size);
    packet[0] = 0x78; // STAP-A NAL type

    size_t offset = 1;
    for (const auto& nal : nal_units) {
        uint16_t nal_size = static_cast<uint16_t>(nal.size());
        packet[offset++] = (nal_size >> 8) & 0xFF;
        packet[offset++] = nal_size & 0xFF;
        std::memcpy(packet.data() + offset, nal.data(), nal.size());
        offset += nal.size();
    }

    return packet;
}

bool is_valid_h264_nal(const uint8_t* data, size_t size) {
    if (size < 1) {
        return false;
    }

    uint8_t nal_type = data[0] & 0x1F;
    return nal_type > 0 && nal_type < 24; // Валидные типы NAL для H.264
}

bool is_valid_h265_nal(const uint8_t* data, size_t size) {
    if (size < 2) {
        return false;
    }

    uint8_t nal_type = (data[0] >> 1) & 0x3F;
    return nal_type >= 0 && nal_type <= 63; // Валидные типы NAL для H.265
}

uint8_t get_h264_nal_type(const uint8_t* data, size_t size) {
    if (size < 1) {
        return 0;
    }
    return data[0] & 0x1F;
}

uint8_t get_h265_nal_type(const uint8_t* data, size_t size) {
    if (size < 2) {
        return 0;
    }
    return (data[0] >> 1) & 0x3F;
}

std::string format_time_ms(int64_t ms) {
    int64_t seconds = ms / 1000;
    int64_t minutes = seconds / 60;
    int64_t hours = minutes / 60;

    std::ostringstream oss;
    if (hours > 0) {
        oss << hours << "h ";
    }
    if (minutes > 0) {
        oss << (minutes % 60) << "m ";
    }
    oss << (seconds % 60) << "." << (ms % 1000) << "s";

    return oss.str();
}

std::string format_size(size_t bytes) {
    const char* units[] = {"B", "KB", "MB", "GB"};
    size_t unit_index = 0;
    double size = static_cast<double>(bytes);

    while (size >= 1024.0 && unit_index < 3) {
        size /= 1024.0;
        unit_index++;
    }

    std::ostringstream oss;
    oss << std::fixed << std::setprecision(2) << size << " " << units[unit_index];
    return oss.str();
}

bool is_ffmpeg_available() {
#ifdef ENABLE_FFMPEG
    return true;
#else
    return false;
#endif
}

std::string get_ffmpeg_version() {
#ifdef ENABLE_FFMPEG
    return av_version_info();
#else
    return "FFmpeg not enabled";
#endif
}

} // namespace test_utils
