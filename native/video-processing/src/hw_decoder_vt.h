/**
 * @file hw_decoder_vt.h
 * @brief VideoToolbox аппаратный декодер для macOS/iOS
 */

#ifndef HW_DECODER_VT_H
#define HW_DECODER_VT_H

#ifdef __APPLE__

#include "hw_decoder.h"
#include <VideoToolbox/VideoToolbox.h>
#include <CoreMedia/CoreMedia.h>
#include <vector>

class HWDecoderVideoToolbox : public HWDecoder {
public:
    HWDecoderVideoToolbox();
    ~HWDecoderVideoToolbox() override;
    
    bool init(CodecType codecType, int width, int height) override;
    bool decode(const uint8_t* nal, size_t size, int64_t timestamp, DecodedFrame& output) override;
    HWDecoderType getType() const override { return HWDecoderType::VideoToolbox; }
    std::string getName() const override { return "VideoToolbox"; }
    Stats getStats() const override { return stats_; }
    
    // Проверка поддержки VideoToolbox на системе
    static bool isSupported();
    
private:
    // VideoToolbox компоненты
    VTDecompressionSessionRef decompressionSession_;
    CMVideoFormatDescriptionRef formatDescription_;
    CVPixelBufferRef outputPixelBuffer_;
    
    // Параметры видео
    CodecType codecType_;
    int width_;
    int height_;
    
    // SPS/PPS из SDP для инициализации format description
    std::vector<uint8_t> spsData_;
    std::vector<uint8_t> ppsData_;
    std::vector<uint8_t> vpsData_;
    
    // Статистика
    mutable Stats stats_;
    uint64_t totalDecodeTimeNs_;
    
    // Внутренние методы
    bool initFormatDescription();
    bool createDecompressionSession();
    void releaseResources();
    
    // Вспомогательные методы
    bool parseSPSFromBase64(const std::string& base64, std::vector<uint8_t>& out);
    bool parsePPSFromBase64(const std::string& base64, std::vector<uint8_t>& out);
    bool parseVPSFromBase64(const std::string& base64, std::vector<uint8_t>& out);
    
    // Callback для VideoToolbox
    static void decompressionOutputCallback(
        void* decompressionRefCon,
        void* sourceFrameRefCon,
        OSStatus status,
        VTDecodeInfoFlags infoFlags,
        CVImageBufferRef imageBuffer,
        CMTime timestamp,
        CMTime duration
    );
    
    // Конвертация CVPixelBuffer в системную память
    bool convertPixelBufferToMemory(CVPixelBufferRef pixelBuffer, uint8_t*& output, size_t& outputSize);
};

#endif // __APPLE__

#endif // HW_DECODER_VT_H
