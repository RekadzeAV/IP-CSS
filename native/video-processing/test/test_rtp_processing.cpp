#include <iostream>
#include <vector>
#include <cstring>
#include <cstdint>

// Тесты для RTP обработки (фрагментация NAL units)

// RTP заголовок (12 байт)
struct RTPHeader {
    uint8_t version : 2;
    uint8_t padding : 1;
    uint8_t extension : 1;
    uint8_t cc : 4;
    uint8_t marker : 1;
    uint8_t pt : 7;
    uint16_t sequence;
    uint32_t timestamp;
    uint32_t ssrc;
};

// FU-A заголовок (1 байт после RTP заголовка)
struct FU_A_Header {
    uint8_t start : 1;
    uint8_t end : 1;
    uint8_t reserved : 1;
    uint8_t nal_type : 5;
};

// Создание RTP пакета
std::vector<uint8_t> create_rtp_packet(uint8_t payload_type, uint16_t sequence, uint32_t timestamp, const uint8_t* payload, size_t payload_size) {
    std::vector<uint8_t> packet;
    packet.resize(12 + payload_size); // 12 байт RTP заголовок

    RTPHeader* header = reinterpret_cast<RTPHeader*>(packet.data());
    header->version = 2;
    header->padding = 0;
    header->extension = 0;
    header->cc = 0;
    header->marker = 0;
    header->pt = payload_type;
    header->sequence = htons(sequence);
    header->timestamp = htonl(timestamp);
    header->ssrc = htonl(0x12345678);

    if (payload_size > 0) {
        std::memcpy(packet.data() + 12, payload, payload_size);
    }

    return packet;
}

// Тест обработки FU-A фрагментации
void test_rtp_fragmentation_fu_a() {
    std::cout << "  Testing RTP FU-A fragmentation..." << std::endl;

    // Создаем большой NAL unit (больше MTU)
    const size_t large_nal_size = 2000;
    std::vector<uint8_t> large_nal(large_nal_size);

    // Заполняем тестовыми данными
    for (size_t i = 0; i < large_nal_size; i++) {
        large_nal[i] = static_cast<uint8_t>(i % 256);
    }

    // Симулируем фрагментацию на 3 пакета
    const size_t fragment_size = 1200; // Размер фрагмента (без RTP заголовка)
    const size_t num_fragments = (large_nal_size + fragment_size - 1) / fragment_size;

    std::vector<std::vector<uint8_t>> fragments;
    uint16_t sequence = 100;

    for (size_t i = 0; i < num_fragments; i++) {
        size_t offset = i * fragment_size;
        size_t size = std::min(fragment_size, large_nal_size - offset);

        // Создаем FU-A пакет
        std::vector<uint8_t> fragment;
        fragment.resize(12 + 1 + size); // RTP header + FU-A header + payload

        RTPHeader* header = reinterpret_cast<RTPHeader*>(fragment.data());
        header->version = 2;
        header->marker = (i == num_fragments - 1) ? 1 : 0; // Последний фрагмент
        header->pt = 96; // H.264 payload type
        header->sequence = htons(sequence++);
        header->timestamp = htonl(1000);
        header->ssrc = htonl(0x12345678);

        // FU-A заголовок
        FU_A_Header* fu_header = reinterpret_cast<FU_A_Header*>(fragment.data() + 12);
        fu_header->start = (i == 0) ? 1 : 0;
        fu_header->end = (i == num_fragments - 1) ? 1 : 0;
        fu_header->reserved = 0;
        fu_header->nal_type = 1; // NAL type 1 (non-IDR)

        // Payload
        std::memcpy(fragment.data() + 13, large_nal.data() + offset, size);

        fragments.push_back(fragment);
    }

    // Проверка
    ASSERT_EQ(num_fragments, fragments.size());
    ASSERT_TRUE(fragments[0].size() > 12);
    ASSERT_TRUE(fragments[num_fragments - 1].size() > 12);

    // Проверка маркеров
    RTPHeader* first_header = reinterpret_cast<RTPHeader*>(fragments[0].data());
    RTPHeader* last_header = reinterpret_cast<RTPHeader*>(fragments[num_fragments - 1].data());

    ASSERT_EQ(0, first_header->marker); // Первый не должен быть помечен
    ASSERT_EQ(1, last_header->marker);  // Последний должен быть помечен

    // Проверка FU-A заголовков
    FU_A_Header* first_fu = reinterpret_cast<FU_A_Header*>(fragments[0].data() + 12);
    FU_A_Header* last_fu = reinterpret_cast<FU_A_Header*>(fragments[num_fragments - 1].data() + 12);

    ASSERT_EQ(1, first_fu->start);
    ASSERT_EQ(0, first_fu->end);
    ASSERT_EQ(0, last_fu->start);
    ASSERT_EQ(1, last_fu->end);

    test_results.push_back({"test_rtp_fragmentation_fu_a", true, ""});
}

// Тест обработки STAP-A (Single Time Aggregation Packet)
void test_rtp_fragmentation_stap_a() {
    std::cout << "  Testing RTP STAP-A aggregation..." << std::endl;

    // Создаем несколько маленьких NAL units
    std::vector<std::vector<uint8_t>> nal_units = {
        {0x67, 0x64, 0x00, 0x1e}, // SPS
        {0x68, 0xeb, 0xe3, 0xcb}, // PPS
        {0x65, 0x88, 0x84, 0x00}  // IDR
    };

    // Создаем STAP-A пакет
    std::vector<uint8_t> stap_packet;
    size_t total_size = 12; // RTP header

    // Добавляем размеры и данные каждого NAL unit
    for (const auto& nal : nal_units) {
        total_size += 2; // 2 байта для размера
        total_size += nal.size();
    }

    stap_packet.resize(total_size);

    // RTP заголовок
    RTPHeader* header = reinterpret_cast<RTPHeader*>(stap_packet.data());
    header->version = 2;
    header->marker = 1; // STAP-A обычно помечается
    header->pt = 96;
    header->sequence = htons(200);
    header->timestamp = htonl(2000);
    header->ssrc = htonl(0x12345678);

    // Добавляем NAL units
    size_t offset = 12;
    for (const auto& nal : nal_units) {
        // Размер NAL unit (2 байта, big-endian)
        uint16_t nal_size = htons(static_cast<uint16_t>(nal.size()));
        std::memcpy(stap_packet.data() + offset, &nal_size, 2);
        offset += 2;

        // Данные NAL unit
        std::memcpy(stap_packet.data() + offset, nal.data(), nal.size());
        offset += nal.size();
    }

    // Проверка
    ASSERT_EQ(total_size, stap_packet.size());
    ASSERT_EQ(1, header->marker);

    // Проверка, что все NAL units присутствуют
    offset = 12;
    for (size_t i = 0; i < nal_units.size(); i++) {
        uint16_t nal_size = ntohs(*reinterpret_cast<uint16_t*>(stap_packet.data() + offset));
        ASSERT_EQ(nal_units[i].size(), nal_size);
        offset += 2 + nal_size;
    }

    test_results.push_back({"test_rtp_fragmentation_stap_a", true, ""});
}

// Вспомогательные функции для network byte order
#ifdef _WIN32
#include <winsock2.h>
#else
#include <arpa/inet.h>
#endif

uint16_t htons(uint16_t hostshort) {
#ifdef _WIN32
    return ::htons(hostshort);
#else
    return ::htons(hostshort);
#endif
}

uint32_t htonl(uint32_t hostlong) {
#ifdef _WIN32
    return ::htonl(hostlong);
#else
    return ::htonl(hostlong);
#endif
}

uint16_t ntohs(uint16_t netshort) {
#ifdef _WIN32
    return ::ntohs(netshort);
#else
    return ::ntohs(netshort);
#endif
}
