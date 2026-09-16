/**
 * @file hw_decoder_nal.cpp
 * @brief Обработка NAL units для аппаратного декодирования
 */

#include "hw_decoder_nal.h"
#include <chrono>
#include <cstring>

NALUnitAssembler::NALUnitAssembler(const NALUnitAssemblerConfig& config)
    : config_(config)
    , codecType_(CODEC_UNKNOWN) {
    memset(&stats_, 0, sizeof(stats_));
}

NALUnitAssembler::~NALUnitAssembler() {
    flush();
}

NALUnitAssembler::Stats NALUnitAssembler::getStats() const {
    return stats_;
}

bool NALUnitAssembler::isKeyFrameType(uint8_t type, CodecType codec) const {
    if (codec == CODEC_H264) {
        return (type == H264_NAL_IDR);
    } else if (codec == CODEC_H265) {
        return (type == H265_NAL_IDR_W_RADL || 
                type == H265_NAL_IDR_N_LP || 
                type == H265_NAL_BLA_W_LP ||
                type == H265_NAL_BLA_W_RADL ||
                type == H265_NAL_BLA_N_LP ||
                type == H265_NAL_CRA_NUT);
    }
    return false;
}

bool NALUnitAssembler::parseH264NALHeader(uint8_t byte, uint8_t& type, uint8_t& nalRefIdc) {
    bool forbidden = (byte >> 7) & 0x1;
    nalRefIdc = (byte >> 5) & 0x3;
    type = byte & 0x1F;
    return !forbidden;
}

bool NALUnitAssembler::parseH265NALHeader(uint16_t header, uint8_t& type, uint8_t& layerId) {
    bool forbidden = (header >> 15) & 0x1;
    type = (header >> 9) & 0x3F;
    layerId = (header >> 1) & 0x3F;
    return !forbidden;
}

bool NALUnitAssembler::hasCompleteNALUs() const {
    return !completedNALUs_.empty();
}

bool NALUnitAssembler::hasKeyFrame() const {
    for (const auto& nal : completedNALUs_) {
        if (nal.isKeyFrame) {
            return true;
        }
    }
    return false;
}

void NALUnitAssembler::flush() {
    while (!completedNALUs_.empty()) {
        completedNALUs_.pop_front();
    }
    resetFragmentBuffer();
}

void NALUnitAssembler::resetFragmentBuffer() {
    fragmentBuffer_.data.clear();
    fragmentBuffer_.startTime = 0;
    fragmentBuffer_.startSeq = 0;
    fragmentBuffer_.expectedSeq = 0;
    fragmentBuffer_.complete = false;
}

void NALUnitAssembler::completeFragment(int64_t timestamp) {
    if (fragmentBuffer_.data.size() > 0) {
        NALUnit nal;
        nal.type = fragmentBuffer_.data[0]; // NAL type из первого байта
        nal.data = fragmentBuffer_.data;
        nal.timestamp = timestamp;
        nal.isKeyFrame = isKeyFrameType(nal.type, codecType_);
        
        completedNALUs_.push_back(nal);
        stats_.nalsAssembled++;
    }
    resetFragmentBuffer();
}

bool NALUnitAssembler::processSingleNALUnit(const uint8_t* data, size_t size, int64_t timestamp) {
    if (size < 1) return false;
    
    NALUnit nal;
    nal.data.assign(data, data + size);
    nal.timestamp = timestamp;
    
    if (codecType_ == CODEC_H264) {
        parseH264NALHeader(data[0], nal.type, nal.nalRefIdc);
    } else if (codecType_ == CODEC_H265) {
        uint16_t header = (data[0] << 8) | data[1];
        parseH265NALHeader(header, nal.type, nal.layerId);
    }
    
    nal.isKeyFrame = isKeyFrameType(nal.type, codecType_);
    
    completedNALUs_.push_back(nal);
    stats_.nalsAssembled++;
    
    return true;
}

bool NALUnitAssembler::processSTAP_A(const uint8_t* data, size_t size, int64_t timestamp) {
    if (size < 2) return false;
    
    size_t offset = 1; // Пропускаем заголовок STAP-A
    
    while (offset + 2 <= size) {
        uint16_t nalSize = (data[offset] << 8) | data[offset + 1];
        offset += 2;
        
        if (nalSize == 0 || offset + nalSize > size) break;
        
        // Создаем NAL unit из агрегированного пакета
        NALUnit nal;
        nal.data.assign(data + offset, data + offset + nalSize);
        nal.timestamp = timestamp;
        
        if (codecType_ == CODEC_H264) {
            parseH264NALHeader(nal.data[0], nal.type, nal.nalRefIdc);
        } else if (codecType_ == CODEC_H265) {
            uint16_t header = (nal.data[0] << 8) | nal.data[1];
            parseH265NALHeader(header, nal.type, nal.layerId);
        }
        
        nal.isKeyFrame = isKeyFrameType(nal.type, codecType_);
        
        completedNALUs_.push_back(nal);
        stats_.nalsAssembled++;
        
        offset += nalSize;
    }
    
    return true;
}

bool NALUnitAssembler::processFU_A(const uint8_t* data, size_t size, int64_t timestamp, bool isMarker) {
    if (size < 2) return false;
    
    uint8_t fuIndicator = data[0];
    uint8_t fuHeader = data[1];
    
    uint8_t start = (fuHeader >> 7) & 0x1;
    uint8_t end = (fuHeader >> 6) & 0x1;
    uint8_t nalType = fuHeader & 0x3F;
    
    if (start) {
        // Начало фрагментированного NAL unit
        if (fragmentBuffer_.data.size() > 0) {
            // Предыдущий фрагмент не завершен, сбрасываем
            stats_.droppedPackets++;
        }
        
        resetFragmentBuffer();
        
        fragmentBuffer_.startSeq = stats_.packetsReceived;
        fragmentBuffer_.expectedSeq = stats_.packetsReceived + 1;
        fragmentBuffer_.startTime = std::chrono::duration_cast<std::chrono::milliseconds>(
            std::chrono::system_clock::now().time_since_epoch()
        ).count();
        
        // Создаем NAL unit заголовок
        if (codecType_ == CODEC_H264) {
            uint8_t nalHeader = (fuIndicator & 0xE0) | nalType;
            fragmentBuffer_.data.push_back(nalHeader);
        } else if (codecType_ == CODEC_H265) {
            uint16_t nalHeader = (fuIndicator & 0x81E0) | (nalType << 1) | 1;
            fragmentBuffer_.data.push_back((nalHeader >> 8) & 0xFF);
            fragmentBuffer_.data.push_back(nalHeader & 0xFF);
        }
        
        // Добавляем payload (без FU заголовка)
        if (size > 2) {
            fragmentBuffer_.data.insert(fragmentBuffer_.data.end(),
                                       data + 2, data + size);
        }
        
    } else if (fragmentBuffer_.data.size() > 0) {
        // Продолжение фрагментированного NAL unit
        if (size > 2) {
            fragmentBuffer_.data.insert(fragmentBuffer_.data.end(),
                                       data + 2, data + size);
        }
        
        if (end || isMarker) {
            // Конец фрагментированного NAL unit
            completeFragment(timestamp);
        }
    }
    
    return true;
}

bool NALUnitAssembler::addPacket(const uint8_t* payload, size_t size, int64_t timestamp, bool isMarker) {
    if (payload == nullptr || size == 0) return false;
    
    stats_.packetsReceived++;
    
    // Проверка на переполнение буфера
    if (fragmentBuffer_.data.size() + size > config_.maxBufferSize) {
        stats_.bufferOverflows++;
        resetFragmentBuffer();
        return false;
    }
    
    uint8_t nalType = payload[0] & 0x1F; // H.264
    if (codecType_ == CODEC_H265) {
        nalType = (payload[0] >> 1) & 0x3F; // H.265
    }
    
    switch (nalType) {
        case H264_NAL_STAP_A:
        case H265_NAL_STAP_A:
            return processSTAP_A(payload, size, timestamp);
            
        case H264_NAL_FU_A:
        case H265_NAL_FU_A:
            return processFU_A(payload, size, timestamp, isMarker);
            
        default:
            // Single NAL unit
            return processSingleNALUnit(payload, size, timestamp);
    }
}

bool NALUnitAssembler::getCompleteNALUs(std::vector<NALUnit>& nals) {
    nals.clear();
    
    while (!completedNALUs_.empty()) {
        nals.push_back(completedNALUs_.front());
        completedNALUs_.pop_front();
    }
    
    return !nals.empty();
}
