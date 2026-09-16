#include "video_decoder.h"

#ifdef ENABLE_FFMPEG
extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libavutil/avutil.h>
#include <libavutil/imgutils.h>
#include <libswscale/swscale.h>
}
#endif

#include <cstring>
#include <cstdlib>
#include <memory>
#include <vector>

#ifdef ENABLE_FFMPEG

// Вспомогательная структура для NAL unit
struct NALUnit {
    std::vector<uint8_t> data;
    int type;
};

// Forward declaration
struct VideoDecoder;

// Извлечение NAL units из данных кадра
static std::vector<NALUnit> extractNALUnits(const uint8_t* data, size_t dataSize) {
    std::vector<NALUnit> nalUnits;
    size_t i = 0;

    while (i < dataSize - 3) {
        // Поиск start code: 0x00 0x00 0x00 0x01 или 0x00 0x00 0x01
        if (data[i] == 0x00 && data[i + 1] == 0x00) {
            size_t startCodeLength = 0;

            if (i + 3 < dataSize && data[i + 2] == 0x00 && data[i + 3] == 0x01) {
                startCodeLength = 4;
            } else if (i + 2 < dataSize && data[i + 2] == 0x01) {
                startCodeLength = 3;
            } else {
                i++;
                continue;
            }

            size_t startPos = i + startCodeLength;
            i = startPos;

            // Поиск следующего start code
            size_t endPos = dataSize;
            for (size_t j = startPos; j < dataSize - 2; j++) {
                if (data[j] == 0x00 && data[j + 1] == 0x00) {
                    if (j + 2 < dataSize && data[j + 2] == 0x01) {
                        endPos = j;
                        break;
                    } else if (j + 3 < dataSize && data[j + 2] == 0x00 && data[j + 3] == 0x01) {
                        endPos = j;
                        break;
                    }
                }
            }

            if (endPos > startPos) {
                NALUnit nal;
                nal.data.assign(data + startPos, data + endPos);
                if (!nal.data.empty()) {
                    // Определение типа NAL unit
                    nal.type = nal.data[0] & 0x1F; // Для H.264
                    nalUnits.push_back(nal);
                }
            }

            i = endPos;
        } else {
            i++;
        }
    }

    return nalUnits;
}

// Инициализация кодера с SPS/PPS для H.264 (объявление, реализация после структуры)
static bool initializeCodecWithSPSPPS(VideoDecoder* decoder);

struct VideoDecoder {
    AVCodecContext* codecContext;
    AVFrame* frame;
    AVFrame* frameRGB;
    AVPacket* packet;
    SwsContext* swsContext;
    VideoCodec codec;
    int width;
    int height;
    int actualWidth;  // Реальное разрешение из SPS
    int actualHeight; // Реальное разрешение из SPS
    FrameDecodedCallback callback;
    void* userData;

    // Параметры кодека для H.264/H.265
    uint8_t* sps;      // Sequence Parameter Set
    size_t spsSize;
    uint8_t* pps;      // Picture Parameter Set
    size_t ppsSize;
    bool codecContextInitialized;

    VideoDecoder() : codecContext(nullptr), frame(nullptr),
                     frameRGB(nullptr), packet(nullptr), swsContext(nullptr),
                     codec(VIDEO_CODEC_UNKNOWN), width(0), height(0),
                     actualWidth(0), actualHeight(0),
                     callback(nullptr), userData(nullptr),
                     sps(nullptr), spsSize(0),
                     pps(nullptr), ppsSize(0),
                     codecContextInitialized(false) {}

    ~VideoDecoder() {
        if (sps) {
            free(sps);
            sps = nullptr;
        }
        if (pps) {
            free(pps);
            pps = nullptr;
        }
    }
};

VideoDecoder* video_decoder_create(VideoCodec codec, int width, int height) {
    AVCodecID avCodecId;

    switch (codec) {
        case VIDEO_CODEC_H264:
            avCodecId = AV_CODEC_ID_H264;
            break;
        case VIDEO_CODEC_H265:
            avCodecId = AV_CODEC_ID_H265;
            break;
        case VIDEO_CODEC_MJPEG:
            avCodecId = AV_CODEC_ID_MJPEG;
            break;
        default:
            return nullptr;
    }

    const AVCodec* avCodec = avcodec_find_decoder(avCodecId);
    if (!avCodec) {
        return nullptr;
    }

    std::unique_ptr<VideoDecoder> decoder(new VideoDecoder());
    decoder->codec = codec;
    decoder->width = width;
    decoder->height = height;
    decoder->actualWidth = width;
    decoder->actualHeight = height;

    decoder->codecContext = avcodec_alloc_context3(avCodec);
    if (!decoder->codecContext) {
        return nullptr;
    }

    // Устанавливаем базовые параметры (будут обновлены при получении SPS/PPS)
    decoder->codecContext->width = width;
    decoder->codecContext->height = height;
    decoder->codecContext->pix_fmt = AV_PIX_FMT_YUV420P;

    // Для H.264/H.265 не открываем кодек сразу - ждем SPS/PPS
    if (codec == VIDEO_CODEC_MJPEG) {
        if (avcodec_open2(decoder->codecContext, avCodec, nullptr) < 0) {
            avcodec_free_context(&decoder->codecContext);
            return nullptr;
        }
        decoder->codecContextInitialized = true;
    }

    decoder->frame = av_frame_alloc();
    decoder->frameRGB = av_frame_alloc();
    decoder->packet = av_packet_alloc();

    if (!decoder->frame || !decoder->frameRGB || !decoder->packet) {
        video_decoder_destroy(decoder.release());
        return nullptr;
    }

    // Для MJPEG сразу создаем RGB буферы
    if (codec == VIDEO_CODEC_MJPEG) {
        int numBytes = av_image_get_buffer_size(AV_PIX_FMT_RGB24, width, height, 1);
        uint8_t* buffer = (uint8_t*)av_malloc(numBytes * sizeof(uint8_t));
        av_image_fill_arrays(decoder->frameRGB->data, decoder->frameRGB->linesize,
                             buffer, AV_PIX_FMT_RGB24, width, height, 1);

        decoder->swsContext = sws_getContext(
            width, height, AV_PIX_FMT_YUV420P,
            width, height, AV_PIX_FMT_RGB24,
            SWS_BILINEAR, nullptr, nullptr, nullptr
        );
    }
    // Для H.264/H.265 буферы будут созданы после получения SPS/PPS

    return decoder.release();
}

void video_decoder_destroy(VideoDecoder* decoder) {
    if (!decoder) return;

    if (decoder->swsContext) {
        sws_freeContext(decoder->swsContext);
    }

    if (decoder->frameRGB) {
        av_free(decoder->frameRGB->data[0]);
        av_frame_free(&decoder->frameRGB);
    }

    if (decoder->frame) {
        av_frame_free(&decoder->frame);
    }

    if (decoder->packet) {
        av_packet_free(&decoder->packet);
    }

    if (decoder->codecContext) {
        avcodec_free_context(&decoder->codecContext);
    }

    delete decoder;
}

// Реализация функции initializeCodecWithSPSPPS после определения структуры
static bool initializeCodecWithSPSPPS(VideoDecoder* decoder) {
    if (!decoder || !decoder->sps || !decoder->pps || decoder->spsSize == 0 || decoder->ppsSize == 0) {
        return false;
    }

    if (decoder->codecContextInitialized) {
        // Освобождаем старый контекст
        if (decoder->codecContext->extradata) {
            av_free(decoder->codecContext->extradata);
            decoder->codecContext->extradata = nullptr;
        }
        avcodec_free_context(&decoder->codecContext);
        decoder->codecContextInitialized = false;

        // Создаем новый контекст
        AVCodecID avCodecId = (decoder->codec == VIDEO_CODEC_H264) ? AV_CODEC_ID_H264 : AV_CODEC_ID_H265;
        const AVCodec* avCodec = avcodec_find_decoder(avCodecId);
        if (!avCodec) {
            return false;
        }
        decoder->codecContext = avcodec_alloc_context3(avCodec);
        if (!decoder->codecContext) {
            return false;
        }
    }

    // Определяем кодек
    AVCodecID avCodecId = (decoder->codec == VIDEO_CODEC_H264) ? AV_CODEC_ID_H264 : AV_CODEC_ID_H265;
    const AVCodec* avCodec = avcodec_find_decoder(avCodecId);
    if (!avCodec) {
        return false;
    }

    // Устанавливаем параметры из SPS (упрощенная версия - используем базовые значения)
    decoder->codecContext->width = decoder->width;
    decoder->codecContext->height = decoder->height;
    decoder->codecContext->pix_fmt = AV_PIX_FMT_YUV420P;

    // Для H.264 создаем extradata в формате AVCC
    if (decoder->codec == VIDEO_CODEC_H264) {
        size_t extradataSize = 8 + decoder->spsSize + 3 + decoder->ppsSize;
        uint8_t* extradata = (uint8_t*)av_malloc(extradataSize + AV_INPUT_BUFFER_PADDING_SIZE);
        if (!extradata) {
            return false;
        }

        extradata[0] = 0x01; // version
        if (decoder->spsSize > 1) extradata[1] = decoder->sps[1]; // profile
        if (decoder->spsSize > 2) extradata[2] = decoder->sps[2]; // profile compat
        if (decoder->spsSize > 3) extradata[3] = decoder->sps[3]; // level
        extradata[4] = 0xFF; // reserved + lengthSizeMinusOne
        extradata[5] = 0xE1; // numOfSequenceParameterSets (1)

        // SPS length (2 bytes, big-endian)
        extradata[6] = (decoder->spsSize >> 8) & 0xFF;
        extradata[7] = decoder->spsSize & 0xFF;

        // SPS data
        memcpy(extradata + 8, decoder->sps, decoder->spsSize);

        // PPS count
        size_t ppsOffset = 8 + decoder->spsSize;
        extradata[ppsOffset] = 0x01; // numOfPictureParameterSets (1)

        // PPS length (2 bytes, big-endian)
        extradata[ppsOffset + 1] = (decoder->ppsSize >> 8) & 0xFF;
        extradata[ppsOffset + 2] = decoder->ppsSize & 0xFF;

        // PPS data
        memcpy(extradata + ppsOffset + 3, decoder->pps, decoder->ppsSize);

        decoder->codecContext->extradata = extradata;
        decoder->codecContext->extradata_size = static_cast<int>(extradataSize);
    }

    // Открываем кодек
    if (avcodec_open2(decoder->codecContext, avCodec, nullptr) < 0) {
        if (decoder->codecContext->extradata) {
            av_free(decoder->codecContext->extradata);
            decoder->codecContext->extradata = nullptr;
        }
        return false;
    }

    decoder->codecContextInitialized = true;

    // Создаем RGB буферы и swsContext
    if (decoder->swsContext) {
        sws_freeContext(decoder->swsContext);
        decoder->swsContext = nullptr;
    }

    if (decoder->frameRGB->data[0]) {
        av_free(decoder->frameRGB->data[0]);
        decoder->frameRGB->data[0] = nullptr;
    }

    int w = decoder->actualWidth > 0 ? decoder->actualWidth : decoder->width;
    int h = decoder->actualHeight > 0 ? decoder->actualHeight : decoder->height;

    int numBytes = av_image_get_buffer_size(AV_PIX_FMT_RGB24, w, h, 1);
    uint8_t* buffer = (uint8_t*)av_malloc(numBytes * sizeof(uint8_t));
    av_image_fill_arrays(decoder->frameRGB->data, decoder->frameRGB->linesize,
                         buffer, AV_PIX_FMT_RGB24, w, h, 1);

    decoder->swsContext = sws_getContext(
        w, h, AV_PIX_FMT_YUV420P,
        w, h, AV_PIX_FMT_RGB24,
        SWS_BILINEAR, nullptr, nullptr, nullptr
    );

    return true;
}

bool video_decoder_decode(
    VideoDecoder* decoder,
    const uint8_t* data,
    size_t dataSize,
    int64_t timestamp
) {
    if (!decoder || !data || dataSize == 0) {
        return false;
    }

    // Для H.264/H.265 извлекаем NAL units и обрабатываем SPS/PPS
    if (decoder->codec == VIDEO_CODEC_H264 || decoder->codec == VIDEO_CODEC_H265) {
        std::vector<NALUnit> nalUnits = extractNALUnits(data, dataSize);
        bool hasSpsPpsUpdate = false;

        for (const auto& nal : nalUnits) {
            if (nal.data.empty()) continue;

            int nalType = nal.type;

            if (decoder->codec == VIDEO_CODEC_H264) {
                if (nalType == 7) { // SPS
                    if (decoder->sps) {
                        free(decoder->sps);
                    }
                    decoder->spsSize = nal.data.size();
                    decoder->sps = (uint8_t*)malloc(decoder->spsSize);
                    memcpy(decoder->sps, nal.data.data(), decoder->spsSize);
                    hasSpsPpsUpdate = true;
                } else if (nalType == 8) { // PPS
                    if (decoder->pps) {
                        free(decoder->pps);
                    }
                    decoder->ppsSize = nal.data.size();
                    decoder->pps = (uint8_t*)malloc(decoder->ppsSize);
                    memcpy(decoder->pps, nal.data.data(), decoder->ppsSize);
                    hasSpsPpsUpdate = true;
                } else if (nalType == 5) { // IDR frame
                    // Если есть SPS/PPS, но кодек не инициализирован, инициализируем
                    if (!decoder->codecContextInitialized && decoder->sps && decoder->pps) {
                        if (initializeCodecWithSPSPPS(decoder)) {
                            // Продолжаем декодирование
                        } else {
                            return false;
                        }
                    }
                }
            }
        }

        // Если SPS/PPS обновились, переинициализируем кодек
        if (hasSpsPpsUpdate && decoder->sps && decoder->pps) {
            if (!decoder->codecContextInitialized) {
                if (!initializeCodecWithSPSPPS(decoder)) {
                    return false;
                }
            }
        }

        // Если кодек не инициализирован, пропускаем кадр
        if (!decoder->codecContextInitialized) {
            return false;
        }

        // Создаем AVPacket из всех NAL units
        size_t totalSize = 0;
        for (const auto& nal : nalUnits) {
            totalSize += nal.data.size();
        }

        if (totalSize == 0) {
            return false;
        }

        uint8_t* packetData = (uint8_t*)av_malloc(totalSize);
        if (!packetData) {
            return false;
        }

        size_t offset = 0;
        for (const auto& nal : nalUnits) {
            memcpy(packetData + offset, nal.data.data(), nal.data.size());
            offset += nal.data.size();
        }

        decoder->packet->data = packetData;
        decoder->packet->size = static_cast<int>(totalSize);
        decoder->packet->pts = timestamp;
        decoder->packet->dts = timestamp;
    } else {
        // Для MJPEG используем данные напрямую
        decoder->packet->data = const_cast<uint8_t*>(data);
        decoder->packet->size = static_cast<int>(dataSize);
        decoder->packet->pts = timestamp;
        decoder->packet->dts = timestamp;
    }

    int ret = avcodec_send_packet(decoder->codecContext, decoder->packet);
    if (decoder->codec == VIDEO_CODEC_H264 || decoder->codec == VIDEO_CODEC_H265) {
        av_free(decoder->packet->data);
        decoder->packet->data = nullptr;
    }

    if (ret < 0) {
        // AVERROR(EAGAIN) означает, что нужно отправить больше данных
        if (ret != AVERROR(EAGAIN)) {
            return false;
        }
    }

    ret = avcodec_receive_frame(decoder->codecContext, decoder->frame);
    if (ret < 0) {
        // AVERROR(EAGAIN) означает, что нужно отправить больше данных
        if (ret != AVERROR(EAGAIN)) {
            return false;
        }
        return false;
    }

    // Обновляем реальные размеры из декодированного кадра
    if (decoder->frame->width > 0 && decoder->frame->height > 0) {
        decoder->actualWidth = decoder->frame->width;
        decoder->actualHeight = decoder->frame->height;
    }

    // Конвертируем YUV в RGB
    if (decoder->swsContext) {
        sws_scale(decoder->swsContext,
                  decoder->frame->data, decoder->frame->linesize, 0, decoder->actualHeight,
                  decoder->frameRGB->data, decoder->frameRGB->linesize);
    }

    if (decoder->callback) {
        DecodedFrame decodedFrame;
        decodedFrame.width = decoder->actualWidth;
        decodedFrame.height = decoder->actualHeight;
        decodedFrame.timestamp = timestamp;
        decodedFrame.format = 1; // RGB24
        decodedFrame.dataSize = decoder->actualWidth * decoder->actualHeight * 3;

        // Копируем данные кадра
        decodedFrame.data = (uint8_t*)malloc(decodedFrame.dataSize);
        if (decodedFrame.data && decoder->frameRGB->data[0]) {
            memcpy(decodedFrame.data, decoder->frameRGB->data[0], decodedFrame.dataSize);
        } else {
            free(decodedFrame.data);
            decodedFrame.data = nullptr;
            return false;
        }

        decoder->callback(&decodedFrame, decoder->userData);
    }

    return true;
}

void video_decoder_set_callback(
    VideoDecoder* decoder,
    FrameDecodedCallback callback,
    void* userData
) {
    if (decoder) {
        decoder->callback = callback;
        decoder->userData = userData;
    }
}

bool video_decoder_get_info(
    VideoDecoder* decoder,
    int* width,
    int* height,
    VideoCodec* codec
) {
    if (!decoder) {
        return false;
    }

    // Возвращаем реальные размеры если они доступны
    if (width) *width = (decoder->actualWidth > 0) ? decoder->actualWidth : decoder->width;
    if (height) *height = (decoder->actualHeight > 0) ? decoder->actualHeight : decoder->height;
    if (codec) *codec = decoder->codec;

    return true;
}

void decoded_frame_release(DecodedFrame* frame) {
    if (frame && frame->data) {
        free(frame->data);
        frame->data = nullptr;
    }
}

#else
// Заглушки если FFmpeg не включен
VideoDecoder* video_decoder_create(VideoCodec codec, int width, int height) {
    return nullptr;
}

void video_decoder_destroy(VideoDecoder* decoder) {
}

bool video_decoder_decode(
    VideoDecoder* decoder,
    const uint8_t* data,
    size_t dataSize,
    int64_t timestamp
) {
    return false;
}

void video_decoder_set_callback(
    VideoDecoder* decoder,
    FrameDecodedCallback callback,
    void* userData
) {
}

bool video_decoder_get_info(
    VideoDecoder* decoder,
    int* width,
    int* height,
    VideoCodec* codec
) {
    return false;
}

void decoded_frame_release(DecodedFrame* frame) {
}
#endif
