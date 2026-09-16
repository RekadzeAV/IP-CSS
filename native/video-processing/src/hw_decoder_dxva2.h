/**
 * @file hw_decoder_dxva2.h
 * @brief DXVA2 аппаратный декодер для Windows
 */

#ifndef HW_DECODER_DXVA2_H
#define HW_DECODER_DXVA2_H

#ifdef _WIN32

#include "hw_decoder.h"
#include "hw_decoder_nal.h"
#ifdef _WIN32
#include <d3d11.h>
#include <dxva2api.h>
#include <mfapi.h>
#include <vector>

class HWDecoderDXVA2 : public HWDecoder {
public:
    HWDecoderDXVA2();
    ~HWDecoderDXVA2() override;
    
    bool init(CodecType codecType, int width, int height) override;
    bool decode(const uint8_t* nal, size_t size, int64_t timestamp, DecodedFrame& output) override;
    HWDecoderType getType() const override { return HWDecoderType::DXVA2; }
    std::string getName() const override { return "DXVA2"; }
    Stats getStats() const override { return stats_; }
    
    // Проверка поддержки DXVA2 на системе
    static bool isSupported();
    
private:
    // Direct3D 11 устройства
    ID3D11Device* d3dDevice_;
    ID3D11DeviceContext* d3dContext_;
    ID3D11VideoDevice* videoDevice_;
    ID3D11VideoContext* videoContext_;
    
    // DXVA2 decoder
    ID3D11VideoDecoder* decoder_;
    ID3D11VideoDecoderOutputView* decoderOutputView_;
    ID3D11Texture2D* decoderOutputTexture_;
    
    // Параметры видео
    CodecType codecType_;
    int width_;
    int height_;
    
    // NAL unit assembler для сборки фрагментированных пакетов
    NALUnitAssembler nalAssembler_;
    
    // Статистика
    mutable Stats stats_;
    uint64_t totalDecodeTimeNs_;
    
    // Внутренние методы
    bool initD3D11();
    bool createDecoder();
    bool createDecoderOutputView();
    void releaseResources();
    
    // Конвертация из DXVA2 в RGB/YUV
    bool convertToSystemMemory(const uint8_t* dxva2Data, size_t size, uint8_t*& output, size_t& outputSize);
};

#endif // _WIN32

#endif // HW_DECODER_DXVA2_H
