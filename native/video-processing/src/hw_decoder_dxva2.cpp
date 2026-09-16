/**
 * @file hw_decoder_dxva2.cpp
 * @brief DXVA2 аппаратный декодер для Windows
 * 
 * Примечание: Это базовая реализация с заглушками.
 * Полная реализация требует дополнительных тестов с реальным оборудованием.
 */

#include "hw_decoder_dxva2.h"

#ifdef _WIN32

#ifndef DXVA2API_INCLUDED
#include <dxva2api.h>
#endif

#ifndef D3D11_VIDEO_DECODER_BUFFER_PICTURE_PARAMETER
#include <d3d11.h>
#endif

#include <comdef.h>
#include <mfidl.h>
#include <mfreadwrite.h>
#include <codecvt>
#include <locale>
#include <chrono>

HWDecoderDXVA2::HWDecoderDXVA2()
    : d3dDevice_(nullptr)
    , d3dContext_(nullptr)
    , videoDevice_(nullptr)
    , videoContext_(nullptr)
    , decoder_(nullptr)
    , decoderOutputView_(nullptr)
    , decoderOutputTexture_(nullptr)
    , codecType_(CodecType::UNKNOWN)
    , width_(0)
    , height_(0)
    , totalDecodeTimeNs_(0) {
}

HWDecoderDXVA2::~HWDecoderDXVA2() {
    releaseResources();
}

bool HWDecoderDXVA2::isSupported() {
#ifdef _WIN32
    // Проверка поддержки Direct3D 11
    D3D_FEATURE_LEVEL featureLevels[] = {
        D3D_FEATURE_LEVEL_11_1,
        D3D_FEATURE_LEVEL_11_0,
        D3D_FEATURE_LEVEL_10_1
    };
    
    ID3D11Device* tempDevice = nullptr;
    ID3D11DeviceContext* tempContext = nullptr;
    
    HRESULT hr = D3D11CreateDevice(
        nullptr,
        D3D_DRIVER_TYPE_HARDWARE,
        nullptr,
        D3D11_CREATE_DEVICE_VIDEO_SUPPORT,
        featureLevels,
        ARRAYSIZE(featureLevels),
        D3D11_SDK_VERSION,
        &tempDevice,
        nullptr,
        &tempContext
    );
    
    if (SUCCEEDED(hr)) {
        if (tempDevice) tempDevice->Release();
        if (tempContext) tempContext->Release();
        return true;
    }
    
    return false;
#else
    return false;
#endif
}

bool HWDecoderDXVA2::init(CodecType codecType, int width, int height) {
    codecType_ = codecType;
    width_ = width;
    height_ = height;
    
    // Инициализация Direct3D 11
    if (!initD3D11()) {
        return false;
    }
    
    // Создание DXVA2 decoder
    if (!createDecoder()) {
        releaseResources();
        return false;
    }
    
    // Создание вывода
    if (!createDecoderOutputView()) {
        releaseResources();
        return false;
    }
    
    return true;
}

bool HWDecoderDXVA2::initD3D11() {
#ifdef _WIN32
    D3D_FEATURE_LEVEL featureLevels[] = {
        D3D_FEATURE_LEVEL_11_1,
        D3D_FEATURE_LEVEL_11_0,
        D3D_FEATURE_LEVEL_10_1
    };
    
    HRESULT hr = D3D11CreateDevice(
        nullptr,
        D3D_DRIVER_TYPE_HARDWARE,
        nullptr,
        D3D11_CREATE_DEVICE_VIDEO_SUPPORT,
        featureLevels,
        ARRAYSIZE(featureLevels),
        D3D11_SDK_VERSION,
        &d3dDevice_,
        nullptr,
        &d3dContext_
    );
    
    if (FAILED(hr)) {
        return false;
    }
    
    // Получение видео устройства
    hr = d3dDevice_->QueryInterface(__uuidof(ID3D11VideoDevice), (void**)&videoDevice_);
    if (FAILED(hr)) {
        releaseResources();
        return false;
    }
    
    // Получение видео контекста
    d3dContext_->GetVideoContext(&videoContext_);
    if (!videoContext_) {
        releaseResources();
        return false;
    }
    
    return true;
#else
    return false;
#endif
}

bool HWDecoderDXVA2::createDecoder() {
#ifdef _WIN32
    // Выбор GUID декодера в зависимости от кодека
    GUID decoderGuid;
    if (codecType_ == CodecType::H264) {
        // H.264 Main Profile
        decoderGuid = DXVA2_ModeH264_M;
    } else if (codecType_ == CodecType::H265) {
        // H.265 Main Profile
        decoderGuid = DXVA2_ModeHEVC_Main;
    } else {
        return false;
    }
    
    // Получение списка поддерживаемых конфигураций декодера
    UINT configCount = 0;
    HRESULT hr = videoDevice_->GetVideoDecoderConfigCount(&configCount);
    if (FAILED(hr) || configCount == 0) {
        return false;
    }
    
    // Выделение памяти для конфигураций
    D3D11_VIDEO_DECODER_CONFIG* configs = new D3D11_VIDEO_DECODER_CONFIG[configCount];
    
    // Описание входных параметров
    D3D11_VIDEO_DECODER_INPUT_DESC inputDesc = {};
    inputDesc.VideoProfile = decoderGuid;
    inputDesc.BufferType = D3D11_VIDEO_DECODER_BUFFER_PICTURE_PARAMETER;
    inputDesc.ConversionFormat = DXGI_FORMAT_NV12;
    inputDesc.SampleWidth = width_;
    inputDesc.SampleHeight = height_;
    inputDesc.OutputFrameRate.Numerator = 30;
    inputDesc.OutputFrameRate.Denominator = 1;
    inputDesc.OutputWidth = width_;
    inputDesc.OutputHeight = height_;
    
    // Получение конфигураций
    hr = videoDevice_->GetVideoDecoderConfig(&inputDesc, configCount, &configCount, configs);
    if (FAILED(hr) || configCount == 0) {
        delete[] configs;
        return false;
    }
    
    // Выбор первой подходящей конфигурации
    D3D11_VIDEO_DECODER_CONFIG* selectedConfig = &configs[0];
    
    // Создание decoder
    D3D11_VIDEO_DECODER_DESC decoderDesc = {};
    decoderDesc.Guid = decoderGuid;
    decoderDesc.SampleWidth = width_;
    decoderDesc.SampleHeight = height_;
    decoderDesc.OutputFormat = DXGI_FORMAT_NV12;
    
    hr = videoDevice_->CreateVideoDecoder(
        &decoderDesc,
        selectedConfig,
        1,
        &decoder_
    );
    
    delete[] configs;
    
    return SUCCEEDED(hr);
#else
    return false;
#endif
}

bool HWDecoderDXVA2::createDecoderOutputView() {
#ifdef _WIN32
    // Описание текстуры
    D3D11_TEXTURE2D_DESC textureDesc = {};
    textureDesc.Width = width_;
    textureDesc.Height = height_;
    textureDesc.MipLevels = 1;
    textureDesc.ArraySize = 1;
    textureDesc.Format = DXGI_FORMAT_NV12;
    textureDesc.SampleDesc.Count = 1;
    textureDesc.Usage = D3D11_USAGE_DEFAULT;
    textureDesc.BindFlags = D3D11_BIND_DECODER_OUTPUT;
    
    HRESULT hr = d3dDevice_->CreateTexture2D(&textureDesc, nullptr, &decoderOutputTexture_);
    if (FAILED(hr)) {
        return false;
    }
    
    // Создание Output View
    D3D11_VIDEO_DECODER_OUTPUT_VIEW_DESC dovDesc = {};
    dovDesc.ViewDimension = D3D11_VDOV_DIMENSION_TEXTURE2D;
    dovDesc.Texture2D.MipSlice = 0;
    
    hr = videoDevice_->CreateVideoDecoderOutputView(
        decoderOutputTexture_,
        &dovDesc,
        &decoderOutputView_
    );
    
    return SUCCEEDED(hr);
#else
    return false;
#endif
}

bool HWDecoderDXVA2::decode(const uint8_t* nal, size_t size, int64_t timestamp, DecodedFrame& output) {
#ifdef _WIN32
    auto startTime = std::chrono::high_resolution_clock::now();
    
    if (!decoder_ || !decoderOutputView_ || !videoContext_ || !d3dContext_) {
#ifdef _DEBUG
        OutputDebugStringA("DXVA2 decode: decoder not initialized\n");
#endif
        return false;
    }
    
    // Добавляем пакет в сборщик NAL units
    bool isMarker = true; // По умолчанию считаем, что это последний пакет фрейма
    nalAssembler_.addPacket(nal, size, timestamp, isMarker);
    
    // Получаем полные NAL units из сборщика
    std::vector<NALUnit> nals;
    nalAssembler_.getCompleteNALUs(nals);
    
    if (nals.empty()) {
        // Нет готовых NAL units, ждем следующих пакетов
        return false;
    }
    
    // Собираем все NAL units в один буфер для декодирования
    std::vector<uint8_t> bitstream;
    for (const auto& nal : nals) {
        // Добавляем start code (0x00000001) перед каждым NAL unit
        bitstream.push_back(0x00);
        bitstream.push_back(0x00);
        bitstream.push_back(0x00);
        bitstream.push_back(0x01);
        bitstream.insert(bitstream.end(), nal.data.begin(), nal.data.end());
    }
    
    // Создание структуры для декодирования
    D3D11_VIDEO_DECODER_INPUT_INFO inputInfo = {};
    inputInfo.InputBitstream = bitstream.data();
    inputInfo.BitstreamLength = static_cast<UINT>(bitstream.size());
    inputInfo.DecodedPicture = decoderOutputView_;
    inputInfo.ReferencePicture = nullptr;
    inputInfo.OutputPicture = decoderOutputView_;
    inputInfo.QuantumTable = nullptr;
    inputInfo.HuffmanTable = nullptr;
    inputInfo.DecodedFrameCounter = 0;
    inputInfo.DecodedDeltaCounter = 0;
    
    // Вызов DecodePicture
    HRESULT hr = videoContext_->DecodePicture(decoder_, &inputInfo, 1);
    
    if (FAILED(hr)) {
        stats_.framesFailed++;
#ifdef _DEBUG
        char msg[256];
        sprintf_s(msg, "DXVA2 DecodePicture failed: 0x%08X\n", hr);
        OutputDebugStringA(msg);
#endif
        return false;
    }
    
    // Получение данных из decoder output view
    D3D11_MAPPED_SUBRESOURCE mappedResource;
    hr = d3dContext_->Map(decoderOutputTexture_, 0, D3D11_MAP_READ, 0, &mappedResource);
    
    if (FAILED(hr)) {
        stats_.framesFailed++;
#ifdef _DEBUG
        char msg[256];
        sprintf_s(msg, "DXVA2 Map failed: 0x%08X\n", hr);
        OutputDebugStringA(msg);
#endif
        return false;
    }
    
    // Выделение памяти для вывода
    size_t outputSize = width_ * height_ * 2; // NV12: Y + UV interleaved
    uint8_t* outputData = new uint8_t[outputSize];
    
    // Копирование данных из текстуры в системную память
    size_t rowPitch = mappedResource.RowPitch;
    for (UINT y = 0; y < height_; y++) {
        memcpy(outputData + y * rowPitch, 
               static_cast<uint8_t*>(mappedResource.pData) + y * rowPitch,
               width_);
    }
    
    d3dContext_->Unmap(decoderOutputTexture_, 0);
    
    output.data = outputData;
    output.size = outputSize;
    output.width = width_;
    output.height = height_;
    output.timestamp = timestamp;
    
    stats_.framesDecoded++;
    
    auto endTime = std::chrono::high_resolution_clock::now();
    auto duration = std::chrono::duration_cast<std::chrono::nanoseconds>(endTime - startTime).count();
    totalDecodeTimeNs_ += duration;
    stats_.avgDecodeTimeMs = (double)totalDecodeTimeNs_ / (double)stats_.framesDecoded / 1000000.0;
    
    return true;
#else
    return false;
#endif
}

void HWDecoderDXVA2::releaseResources() {
#ifdef _WIN32
    if (decoderOutputView_) {
        decoderOutputView_->Release();
        decoderOutputView_ = nullptr;
    }
    
    if (decoderOutputTexture_) {
        decoderOutputTexture_->Release();
        decoderOutputTexture_ = nullptr;
    }
    
    if (decoder_) {
        decoder_->Release();
        decoder_ = nullptr;
    }
    
    if (videoContext_) {
        videoContext_->Release();
        videoContext_ = nullptr;
    }
    
    if (videoDevice_) {
        videoDevice_->Release();
        videoDevice_ = nullptr;
    }
    
    if (d3dContext_) {
        d3dContext_->Release();
        d3dContext_ = nullptr;
    }
    
    if (d3dDevice_) {
        d3dDevice_->Release();
        d3dDevice_ = nullptr;
    }
#endif
}

bool HWDecoderDXVA2::convertToSystemMemory(const uint8_t* dxva2Data, size_t size, uint8_t*& output, size_t& outputSize) {
#ifdef _WIN32
    // Конвертация из DXVA2 формата в системную память
    // Это упрощённая реализация
    outputSize = size;
    output = new uint8_t[size];
    memcpy(output, dxva2Data, size);
    return true;
#else
    return false;
#endif
}

#endif // _WIN32
