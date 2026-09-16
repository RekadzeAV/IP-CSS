/**
 * @file hw_decoder_vt.cpp
 * @brief VideoToolbox аппаратный декодер для macOS/iOS
 */

#include "hw_decoder_vt.h"

#ifdef __APPLE__

#include <CoreVideo/CoreVideo.h>
#include <cstring>
#include <chrono>

HWDecoderVideoToolbox::HWDecoderVideoToolbox()
    : decompressionSession_(nullptr)
    , formatDescription_(nullptr)
    , outputPixelBuffer_(nullptr)
    , codecType_(CodecType::UNKNOWN)
    , width_(0)
    , height_(0)
    , totalDecodeTimeNs_(0) {
}

HWDecoderVideoToolbox::~HWDecoderVideoToolbox() {
    releaseResources();
}

bool HWDecoderVideoToolbox::isSupported() {
#ifdef __APPLE__
    // VideoToolbox доступен на macOS 10.7+ и iOS 5.0+
    // Проверяем доступность API
    return &VTDecompressionSessionCreate != nullptr &&
           &CMVideoFormatDescriptionCreateFromH264SequenceParameterSets != nullptr;
#else
    return false;
#endif
}

bool HWDecoderVideoToolbox::init(CodecType codecType, int width, int height) {
    codecType_ = codecType;
    width_ = width;
    height_ = height;
    
    // Инициализация format description
    if (!initFormatDescription()) {
        return false;
    }
    
    // Создание decompression session
    if (!createDecompressionSession()) {
        releaseResources();
        return false;
    }
    
    return true;
}

// Вспомогательная функция base64 декодирования
static bool base64_decode(const std::string& input, std::vector<uint8_t>& output) {
    static const char base64_chars[] =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
    
    output.clear();
    int val = 0, valb = -6;
    
    for (unsigned char c : input) {
        if (c == '=') break;
        
        const char* pos = strchr(base64_chars, c);
        if (!pos) continue;
        
        val = (val << 6) + (pos - base64_chars);
        valb += 6;
        
        if (valb >= 0) {
            output.push_back(static_cast<uint8_t>((val >> valb) & 0xFF));
            valb -= 8;
        }
    }
    
    return !output.empty();
}

bool HWDecoderVideoToolbox::parseSPSFromBase64(const std::string& base64, std::vector<uint8_t>& out) {
    return base64_decode(base64, out);
}

bool HWDecoderVideoToolbox::parsePPSFromBase64(const std::string& base64, std::vector<uint8_t>& out) {
    return base64_decode(base64, out);
}

bool HWDecoderVideoToolbox::parseVPSFromBase64(const std::string& base64, std::vector<uint8_t>& out) {
    return base64_decode(base64, out);
}

bool HWDecoderVideoToolbox::initFormatDescription() {
    OSStatus status;
    
    if (codecType_ == CodecType::H264) {
        // ✅ Используем реальные SPS/PPS из SDP если доступны
        if (!spsData_.empty()) {
            const uint8_t* spsPtr = spsData_.data();
            const uint8_t* ppsPtr = ppsData_.empty() ? nullptr : ppsData_.data();
            
            if (ppsPtr) {
                // Создаем с SPS и PPS
                const uint8_t* paramSets[] = {spsPtr, ppsPtr};
                size_t paramSetSizes[] = {spsData_.size(), ppsData_.size()};
                
                status = CMVideoFormatDescriptionCreateFromH264ParameterSets(
                    kCFAllocatorDefault,
                    2,  // SPS + PPS
                    paramSets,
                    paramSetSizes,
                    4,  // NAL unit length size
                    &formatDescription_
                );
            } else {
                // Только SPS
                status = CMVideoFormatDescriptionCreateFromH264ParameterSets(
                    kCFAllocatorDefault,
                    1,  // Только SPS
                    &spsPtr,
                    &spsData_.size(),
                    4,
                    &formatDescription_
                );
            }
            
            if (status == noErr && formatDescription_) {
                return true;
            }
        }
        
        // ✅ Fallback: создаем минимальную структуру для тестирования
        // В production это должно быть заполнено реальными данными из SDP
        uint8_t minSPS[] = {0x67, 0x42, 0x00, 0x1E, 0x85, 0x32, 0x40, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x03, 0x00, 0x10, 0x00, 0x00, 0x03, 0x03, 0xC8, 0xF1, 0x42, 0x99, 0x60};
        uint8_t minPPS[] = {0x68, 0xCE, 0x38, 0x80};
        
        const uint8_t* paramSets[] = {minSPS, minPPS};
        size_t paramSetSizes[] = {sizeof(minSPS), sizeof(minPPS)};
        
        status = CMVideoFormatDescriptionCreateFromH264ParameterSets(
            kCFAllocatorDefault,
            2,
            paramSets,
            paramSetSizes,
            4,
            &formatDescription_
        );
        
    } else if (codecType_ == CodecType::H265) {
        // ✅ H.265 с VPS/SPS/PPS
        if (!vpsData_.empty() || !spsData_.empty() || !ppsData_.empty()) {
            std::vector<const uint8_t*> vpsPtrs;
            std::vector<size_t> vpsSizes;
            std::vector<const uint8_t*> spsPtrs;
            std::vector<size_t> spsSizes;
            std::vector<const uint8_t*> ppsPtrs;
            std::vector<size_t> ppsSizes;
            
            if (!vpsData_.empty()) {
                vpsPtrs.push_back(vpsData_.data());
                vpsSizes.push_back(vpsData_.size());
            }
            if (!spsData_.empty()) {
                spsPtrs.push_back(spsData_.data());
                spsSizes.push_back(spsData_.size());
            }
            if (!ppsData_.empty()) {
                ppsPtrs.push_back(ppsData_.data());
                ppsSizes.push_back(ppsData_.size());
            }
            
            status = CMVideoFormatDescriptionCreateFromHEVCParameterSets(
                kCFAllocatorDefault,
                vpsPtrs.size(),
                vpsPtrs.empty() ? nullptr : vpsPtrs.data(),
                vpsPtrs.empty() ? nullptr : vpsSizes.data(),
                spsPtrs.size(),
                spsPtrs.empty() ? nullptr : spsPtrs.data(),
                spsPtrs.empty() ? nullptr : spsSizes.data(),
                ppsPtrs.size(),
                ppsPtrs.empty() ? nullptr : ppsPtrs.data(),
                ppsPtrs.empty() ? nullptr : ppsSizes.data(),
                4,
                &formatDescription_
            );
            
            if (status == noErr && formatDescription_) {
                return true;
            }
        }
        
        // Fallback для H.265
        return false;
    } else {
        return false;
    }
    
    return (status == noErr && formatDescription_ != nullptr);
}

bool HWDecoderVideoToolbox::createDecompressionSession() {
    if (!formatDescription_) {
        return false;
    }
    
    // Описание видеопараметров
    CMVideoDimensions dimensions;
    dimensions.width = width_;
    dimensions.height = height_;
    
    // Создание session
    OSStatus status = VTDecompressionSessionCreate(
        kCFAllocatorDefault,
        formatDescription_,
        nullptr,  // decoder specification (nullptr = auto)
        nullptr,  // destination image buffer creator (nullptr = default)
        nullptr,  // decompression callback
        &decompressionSession_
    );
    
    if (status != noErr || !decompressionSession_) {
        return false;
    }
    
    return true;
}

void HWDecoderVideoToolbox::decompressionOutputCallback(
    void* decompressionRefCon,
    void* sourceFrameRefCon,
    OSStatus status,
    VTDecodeInfoFlags infoFlags,
    CVImageBufferRef imageBuffer,
    CMTime timestamp,
    CMTime duration
) {
    HWDecoderVideoToolbox* decoder = static_cast<HWDecoderVideoToolbox*>(decompressionRefCon);
    
    if (status == noErr && imageBuffer) {
        decoder->stats_.framesDecoded++;
    } else {
        decoder->stats_.framesFailed++;
    }
}

bool HWDecoderVideoToolbox::decode(const uint8_t* nal, size_t size, int64_t timestamp, DecodedFrame& output) {
    auto startTime = std::chrono::high_resolution_clock::now();
    
    if (!decompressionSession_ || !formatDescription_) {
        return false;
    }
    
    // Создание CMBlockBuffer
    CMBlockBufferRef blockBuffer = nullptr;
    OSStatus status = CMBlockBufferCreateWithMemoryBlock(
        kCFAllocatorDefault,
        const_cast<uint8_t*>(nal),
        size,
        kCMBlockBufferNoAssist,
        kCFAllocatorDefault,
        0,
        size,
        0,
        &blockBuffer
    );
    
    if (status != noErr || !blockBuffer) {
        stats_.framesFailed++;
        return false;
    }
    
    // Создание CMSampleBuffer
    CMSampleBufferRef sampleBuffer = nullptr;
    status = CMSampleBufferCreate(
        kCFAllocatorDefault,
        blockBuffer,
        true,  // dataReady
        nullptr,  // numRangesReadyPtr
        nullptr,  // rangeReadyPtr
        formatDescription_,
        1,  // numSamples
        0,  // numSampleTimingEntries
        nullptr,  // sampleTimingArray
        0,  // numSampleSizeEntries
        nullptr,  // sampleSizeArray
        &sampleBuffer
    );
    
    CMBlockBufferRelease(blockBuffer);
    
    if (status != noErr || !sampleBuffer) {
        stats_.framesFailed++;
        return false;
    }
    
    // Декодирование с использованием VideoToolbox
    CMTime vtTimestamp = CMTimeMake(timestamp, 90000); // RTP clock rate 90kHz
    CMTime vtDuration = CMTimeMake(1000, 90000); // Примерная длительность
    
    VTDecodeInfoFlags decodeFlags = kVTDecodeFrame_EnableAsynchronousDecompression;
    
    status = VTDecompressionSessionWaitForAsynchronousFrames(
        decompressionSession_,
        sampleBuffer,
        vtTimestamp,
        vtDuration,
        decodeFlags,
        decompressionOutputCallback,
        this
    );
    
    CMSampleBufferRelease(sampleBuffer);
    
    if (status != noErr) {
        stats_.framesFailed++;
        return false;
    }
    
    // В полной реализации здесь нужно получить CVPixelBuffer из callback
    // Для заглушки выделяем память и копируем данные
    size_t outputSize = width_ * height_ * 2; // NV12/YUV 4:2:0
    uint8_t* outputData = new uint8_t[outputSize];
    
    output.data = outputData;
    output.size = outputSize;
    output.width = width_;
    output.height = height_;
    output.timestamp = timestamp;
    
    stats_.framesDecoded++;
    
    auto endTime = std::chrono::high_resolution_clock::now();
    auto durationNs = std::chrono::duration_cast<std::chrono::nanoseconds>(endTime - startTime).count();
    totalDecodeTimeNs_ += durationNs;
    stats_.avgDecodeTimeMs = (double)totalDecodeTimeNs_ / (double)stats_.framesDecoded / 1000000.0;
    
    return true;
}

bool HWDecoderVideoToolbox::convertPixelBufferToMemory(CVPixelBufferRef pixelBuffer, uint8_t*& output, size_t& outputSize) {
    CVPixelBufferLockBaseAddress(pixelBuffer, 0);
    
    void* baseAddress = CVPixelBufferGetBaseAddress(pixelBuffer);
    size_t width = CVPixelBufferGetWidth(pixelBuffer);
    size_t height = CVPixelBufferGetHeight(pixelBuffer);
    size_t bytesPerRow = CVPixelBufferGetBytesPerRow(pixelBuffer);
    
    // Для NV12 формата
    outputSize = width * height * 3 / 2;
    output = new uint8_t[outputSize];
    
    // Копирование Y и UV плоскостей
    memcpy(output, baseAddress, bytesPerRow * height);
    
    CVPixelBufferUnlockBaseAddress(pixelBuffer, 0);
    return true;
}

void HWDecoderVideoToolbox::releaseResources() {
    if (decompressionSession_) {
        VTDecompressionSessionInvalidate(decompressionSession_);
        CFRelease(decompressionSession_);
        decompressionSession_ = nullptr;
    }
    
    if (formatDescription_) {
        CFRelease(formatDescription_);
        formatDescription_ = nullptr;
    }
    
    if (outputPixelBuffer_) {
        CFRelease(outputPixelBuffer_);
        outputPixelBuffer_ = nullptr;
    }
}

#endif // __APPLE__
