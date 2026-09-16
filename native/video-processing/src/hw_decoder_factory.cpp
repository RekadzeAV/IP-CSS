/**
 * @file hw_decoder_factory.cpp
 * @brief Factory для создания аппаратных декодеров
 */

#include "hw_decoder_factory.h"
#include "hw_decoder.h"

#ifdef _WIN32
#include "hw_decoder_dxva2.h"
#endif

// Заглушки для macOS VideoToolbox (будут реализованы позже)
#ifdef __APPLE__
#include "hw_decoder_vt.h"
#else
class HWDecoderVideoToolbox : public HWDecoder {
public:
    bool init(CodecType codecType, int width, int height) override { return false; }
    bool decode(const uint8_t* nal, size_t size, int64_t timestamp, DecodedFrame& output) override { return false; }
    HWDecoderType getType() const override { return HWDecoderType::VideoToolbox; }
    std::string getName() const override { return "VideoToolbox"; }
    Stats getStats() const override { return Stats(); }
};
#endif

// Factory implementation
bool HWDecoderFactory::isHardwareDecodingSupported() {
#ifdef _WIN32
    return isDXVA2Supported();
#else
    return isVideoToolboxSupported();
#endif
}

bool HWDecoderFactory::isDXVA2Supported() {
#ifdef _WIN32
    return HWDecoderDXVA2::isSupported();
#else
    return false;
#endif
}
    
bool HWDecoderFactory::isVideoToolboxSupported() {
#ifdef __APPLE__
    return HWDecoderVideoToolbox::isSupported();
#else
    return false;
#endif
}

std::unique_ptr<HWDecoder> HWDecoderFactory::create(CodecType codecType, int width, int height) {
    HWDecoderType preferredType = getPreferredDecoderType();
    
    switch (preferredType) {
#ifdef _WIN32
        case HWDecoderType::DXVA2:
            if (isDXVA2Supported()) {
                auto decoder = std::make_unique<HWDecoderDXVA2>();
                if (decoder->init(codecType, width, height)) {
                    return decoder;
                }
            }
            break;
#endif
            
#ifndef _WIN32
        case HWDecoderType::VideoToolbox:
            if (isVideoToolboxSupported()) {
                auto decoder = std::make_unique<HWDecoderVideoToolbox>();
                if (decoder->init(codecType, width, height)) {
                    return decoder;
                }
            }
            break;
#endif
            
        default:
            break;
    }
    
    // Fallback на программное декодирование
    return nullptr;
}

HWDecoderType HWDecoderFactory::getPreferredDecoderType() {
#ifdef _WIN32
    if (isDXVA2Supported()) {
        return HWDecoderType::DXVA2;
    }
#else
    if (isVideoToolboxSupported()) {
        return HWDecoderType::VideoToolbox;
    }
#endif
    
    return HWDecoderType::None;
}

std::vector<HWDecoderType> HWDecoderFactory::getAvailableDecoders() {
    std::vector<HWDecoderType> decoders;
    
#ifdef _WIN32
    if (isDXVA2Supported()) {
        decoders.push_back(HWDecoderType::DXVA2);
    }
#else
    if (isVideoToolboxSupported()) {
        decoders.push_back(HWDecoderType::VideoToolbox);
    }
#endif
    
    if (decoders.empty()) {
        decoders.push_back(HWDecoderType::None);
    }
    
    return decoders;
}
