/**
 * @file hw_decoder.h
 * @brief Общий интерфейс для аппаратного декодирования видео
 */

#ifndef HW_DECODER_H
#define HW_DECODER_H

#include <cstdint>
#include <cstddef>
#include <memory>
#include <string>

// Типы кодеков
enum class CodecType {
    H264,
    H265,
    UNKNOWN
};

// Типы аппаратных декодеров
enum class HWDecoderType {
    DXVA2,      // Windows
    VideoToolbox, // macOS
    None        // Программное
};

// Результат декодирования
struct DecodedFrame {
    uint8_t* data;      // Выделенная память ( caller должен освободить )
    size_t size;
    int width;
    int height;
    int64_t timestamp;
    
    DecodedFrame() : data(nullptr), size(0), width(0), height(0), timestamp(0) {}
};

// Базовый класс аппаратного декодера
class HWDecoder {
public:
    virtual ~HWDecoder() = default;
    
    // Инициализация декодера
    virtual bool init(CodecType codecType, int width, int height) = 0;
    
    // Декодирование NAL unit
    virtual bool decode(const uint8_t* nal, size_t size, int64_t timestamp, DecodedFrame& output) = 0;
    
    // Тип декодера
    virtual HWDecoderType getType() const = 0;
    virtual std::string getName() const = 0;
    
    // Статистика
    virtual Stats getStats() const = 0;
    
    // Установка SPS/PPS/VPS из SDP (опционально, для некоторых декодеров)
    virtual void setSPS(const std::vector<uint8_t>& sps) {}
    virtual void setPPS(const std::vector<uint8_t>& pps) {}
    virtual void setVPS(const std::vector<uint8_t>& vps) {}
};

    virtual Stats getStats() const = 0;
};

// Фабрика для создания аппаратных декодеров
class HWDecoderFactory {
public:
    // Проверка поддержки аппаратного декодирования
    static bool isHardwareDecodingSupported();
    
    // Проверка поддержки конкретного типа декодера
    static bool isDXVA2Supported();
    static bool isVideoToolboxSupported();
    
    // Создание декодера
    // @param codecType Тип кодека
    // @param width Ширина видео
    // @param height Высота видео
    // @return Умный указатель на декодер или nullptr если не поддерживается
    static std::unique_ptr<HWDecoder> create(CodecType codecType, int width, int height);
    
    // Получение предпочтительного типа декодера
    static HWDecoderType getPreferredDecoderType();
    
    // Получение списка доступных декодеров
    static std::vector<HWDecoderType> getAvailableDecoders();
};

// Вспомогательные функции
inline CodecType stringToCodecType(const std::string& codec) {
    if (codec == "H.264" || codec == "AVC") {
        return CodecType::H264;
    } else if (codec == "H.265" || codec == "HEVC") {
        return CodecType::H265;
    }
    return CodecType::UNKNOWN;
}

inline std::string codecTypeToString(CodecType type) {
    switch (type) {
        case CodecType::H264: return "H.264";
        case CodecType::H265: return "H.265";
        default: return "UNKNOWN";
    }
}

#endif // HW_DECODER_H
