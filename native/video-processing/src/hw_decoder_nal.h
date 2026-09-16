/**
 * @file hw_decoder_nal.h
 * @brief Обработка NAL units для аппаратного декодирования
 */

#ifndef HW_DECODER_NAL_H
#define HW_DECODER_NAL_H

#include <cstdint>
#include <vector>
#include <deque>

// Типы NAL units для H.264
enum H264NALType {
    H264_NAL_UNSPECIFIED = 0,
    H264_NAL_NON_IDR = 1,      // Кодированный слайд без IDR
    H264_NAL_DPA = 2,
    H264_NAL_DPB = 3,
    H264_NAL_DPC = 4,
    H264_NAL_IDR = 5,          // IDR слайд (ключевой кадр)
    H264_NAL_SEI = 6,          // Supplemental Enhancement Information
    H264_NAL_SPS = 7,          // Sequence Parameter Set
    H264_NAL_PPS = 8,          // Picture Parameter Set
    H264_NAL_AUD = 9,
    H264_NAL_FU_A = 28,        // Fragmentation Unit Type A
    H264_NAL_FU_B = 29,
    H264_NAL_STAP_A = 24,      // Single Time Aggregation Packet
    H264_NAL_STAP_B = 25,
    H264_NAL_MTAP16 = 26,
    H264_NAL_MTAP24 = 27,
    H264_NAL_FILLER = 12,
    H264_NAL_END_SEQUENCE = 10,
    H264_NAL_END_STREAM = 11
};

// Типы NAL units для H.265
enum H265NALType {
    H265_NAL_UNSPECIFIED_0 = 0,
    H265_NAL_TRAIL_N = 1,      // Trail picture (non-TSA/STSA)
    H265_NAL_TRAIL_R = 2,
    H265_NAL_TSA_N = 3,
    H265_NAL_TSA_R = 4,
    H265_NAL_STSA_N = 5,
    H265_NAL_STSA_R = 6,
    H265_NAL_RADL_N = 7,
    H265_NAL_RADL_R = 8,
    H265_NAL_RASL_N = 9,
    H265_NAL_RASL_R = 10,
    H265_NAL_BLA_W_LP = 16,    // BLA picture
    H265_NAL_BLA_W_RADL = 17,
    H265_NAL_BLA_N_LP = 18,
    H265_NAL_IDR_W_RADL = 19,  // IDR picture
    H265_NAL_IDR_N_LP = 20,
    H265_NAL_CRA_NUT = 21,
    H265_NAL_VPS = 32,         // Video Parameter Set
    H265_NAL_SPS = 33,         // Sequence Parameter Set
    H265_NAL_PPS = 34,         // Picture Parameter Set
    H265_NAL_AUD = 35,
    H265_NAL_FU_A = 46,        // Fragmentation Unit
    H265_NAL_EOS_NUT = 36,
    H265_NAL_EOB_NUT = 37
};

// Структура NAL unit
struct NALUnit {
    uint8_t type;
    uint8_t nalRefIdc; // Для H.264
    uint8_t layerId;   // Для H.265
    std::vector<uint8_t> data;
    int64_t timestamp;
    bool isKeyFrame;
    
    NALUnit() : type(0), nalRefIdc(0), layerId(0), timestamp(0), isKeyFrame(false) {}
};

// Конфигурация сборщика NAL units
struct NALUnitAssemblerConfig {
    size_t maxBufferSize;           // Максимальный размер буфера (по умолчанию 10MB)
    int maxFragmentWaitMs;          // Максимальное время ожидания фрагментов (500ms)
    bool autoFlushOnKeyFrame;       // Автоматическая очистка при ключевои_кадре
    bool dropOutOfOrder;            // Отбрасывать пакеты вне порядка
    
    NALUnitAssemblerConfig() 
        : maxBufferSize(10 * 1024 * 1024)
        , maxFragmentWaitMs(500)
        , autoFlushOnKeyFrame(true)
        , dropOutOfOrder(true) {}
};

// Результат сборки NAL units
struct NALUnitAssemblyResult {
    std::vector<NALUnit> nals;
    bool hasKeyFrame;
    bool hasIncomplete;
    std::string error;
    
    NALUnitAssemblyResult() : hasKeyFrame(false), hasIncomplete(false) {}
};

// Класс для сборки NAL units из RTP пакетов
class NALUnitAssembler {
public:
    NALUnitAssembler(const NALUnitAssemblerConfig& config = NALUnitAssemblerConfig());
    ~NALUnitAssembler();
    
    // Добавление RTP payload с NAL unit
    // @param payload Данные RTP payload
    // @param timestamp RTP timestamp
    // @param isMarker Флаг последнего пакета фрейма
    // @return true если пакет успешно добавлен
    bool addPacket(const uint8_t* payload, size_t size, int64_t timestamp, bool isMarker);
    
    // Проверка готовности полных NAL units
    // @return true если есть готовые NAL units
    bool hasCompleteNALUs() const;
    
    // Получение готовых NAL units
    // @param nals Вектор для хранения результатов
    // @return true если есть готовые NAL units
    bool getCompleteNALUs(std::vector<NALUnit>& nals);
    
    // Принудительная очистка буфера
    void flush();
    
    // Проверка на ключевой кадр в буфере
    bool hasKeyFrame() const;
    
    // Получение статистики
    struct Stats {
        size_t packetsReceived;
        size_t nalsAssembled;
        size_t fragmentsProcessed;
        size_t droppedPackets;
        size_t bufferOverflows;
    };
    
    Stats getStats() const;
    
    // Определение типа кодека (H.264 или H.265)
    enum CodecType {
        CODEC_UNKNOWN,
        CODEC_H264,
        CODEC_H265
    };
    
    void setCodecType(CodecType type) { codecType_ = type; }
    CodecType getCodecType() const { return codecType_; }
    
private:
    NALUnitAssemblerConfig config_;
    CodecType codecType_;
    
    // Буфер для сбора фрагментированных NAL units
    struct FragmentBuffer {
        std::vector<uint8_t> data;
        int64_t startTime;
        uint16_t startSeq;
        uint16_t expectedSeq;
        bool complete;
        
        FragmentBuffer() : startTime(0), startSeq(0), expectedSeq(0), complete(false) {}
    };
    
    FragmentBuffer fragmentBuffer_;
    
    // Буфер для полных NAL units
    std::deque<NALUnit> completedNALUs_;
    
    // Статистика
    Stats stats_;
    
    // Внутренние методы
    bool processSingleNALUnit(const uint8_t* data, size_t size, int64_t timestamp);
    bool processSTAP_A(const uint8_t* data, size_t size, int64_t timestamp);
    bool processSTAP_B(const uint8_t* data, size_t size, int64_t timestamp);
    bool processFU_A(const uint8_t* data, size_t size, int64_t timestamp, bool isMarker);
    
    bool parseH264NALHeader(uint8_t byte, uint8_t& type, uint8_t& nalRefIdc);
    bool parseH265NALHeader(uint16_t header, uint8_t& type, uint8_t& layerId);
    
    bool isKeyFrameType(uint8_t type, CodecType codec) const;
    void resetFragmentBuffer();
    void completeFragment(int64_t timestamp);
};

#endif // HW_DECODER_NAL_H
