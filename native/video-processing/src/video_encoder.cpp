#include "video_encoder.h"
#include <memory>
#include <mutex>
#include <cstring>

#ifdef ENABLE_FFMPEG
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/avutil.h>
#include <libavutil/imgutils.h>
#include <libavutil/opt.h>
#include <libswscale/swscale.h>
}
#endif

struct VideoEncoder {
    EncodingParams params;
    FrameEncodedCallback callback;
    void* userData;
    std::mutex mutex;
    int frameCount;
    
#ifdef ENABLE_FFMPEG
    AVCodecContext* codecContext;
    AVFrame* frame;
    AVPacket* packet;
    SwsContext* swsContext;
    bool initialized;
#endif
};

VideoEncoder* video_encoder_create(const EncodingParams* params) {
    if (!params) return nullptr;
    
    auto* encoder = new VideoEncoder();
    encoder->params = *params;
    encoder->callback = nullptr;
    encoder->userData = nullptr;
    encoder->frameCount = 0;
    
#ifdef ENABLE_FFMPEG
    encoder->codecContext = nullptr;
    encoder->frame = nullptr;
    encoder->packet = nullptr;
    encoder->swsContext = nullptr;
    encoder->initialized = false;
    
    // Определение кодека
    AVCodecID codecId = AV_CODEC_ID_NONE;
    const AVCodec* avCodec = nullptr;
    
    switch (params->codec) {
        case VIDEO_CODEC_H264:
            codecId = AV_CODEC_ID_H264;
            avCodec = avcodec_find_encoder(codecId);
            break;
        case VIDEO_CODEC_H265:
            codecId = AV_CODEC_ID_HEVC;
            avCodec = avcodec_find_encoder(codecId);
            break;
        case VIDEO_CODEC_MJPEG:
            codecId = AV_CODEC_ID_MJPEG;
            avCodec = avcodec_find_encoder(codecId);
            break;
        default:
            delete encoder;
            return nullptr;
    }
    
    if (!avCodec) {
        delete encoder;
        return nullptr;
    }
    
    encoder->codecContext = avcodec_alloc_context3(avCodec);
    if (!encoder->codecContext) {
        delete encoder;
        return nullptr;
    }
    
    // Установка параметров кодирования
    encoder->codecContext->width = params->width;
    encoder->codecContext->height = params->height;
    encoder->codecContext->time_base = {1, params->fps};
    encoder->codecContext->framerate = {params->fps, 1};
    encoder->codecContext->pix_fmt = AV_PIX_FMT_YUV420P;
    encoder->codecContext->bit_rate = params->bitrate;
    encoder->codecContext->gop_size = params->gopSize;
    
    // Настройки для H.264
    if (params->codec == VIDEO_CODEC_H264) {
        av_opt_set(encoder->codecContext->priv_data, "preset", "medium", 0);
        av_opt_set(encoder->codecContext->priv_data, "tune", "zerolatency", 0);
    }
    
    // Открытие кодека
    if (avcodec_open2(encoder->codecContext, avCodec, nullptr) < 0) {
        avcodec_free_context(&encoder->codecContext);
        delete encoder;
        return nullptr;
    }
    
    encoder->frame = av_frame_alloc();
    encoder->packet = av_packet_alloc();
    
    if (!encoder->frame || !encoder->packet) {
        if (encoder->frame) av_frame_free(&encoder->frame);
        if (encoder->packet) av_packet_free(&encoder->packet);
        avcodec_free_context(&encoder->codecContext);
        delete encoder;
        return nullptr;
    }
    
    encoder->frame->format = encoder->codecContext->pix_fmt;
    encoder->frame->width = encoder->codecContext->width;
    encoder->frame->height = encoder->codecContext->height;
    
    if (av_frame_get_buffer(encoder->frame, 0) < 0) {
        av_frame_free(&encoder->frame);
        av_packet_free(&encoder->packet);
        avcodec_free_context(&encoder->codecContext);
        delete encoder;
        return nullptr;
    }
    
    encoder->initialized = true;
#endif
    
    return encoder;
}

void video_encoder_destroy(VideoEncoder* encoder) {
    if (!encoder) return;
    
#ifdef ENABLE_FFMPEG
    if (encoder->swsContext) {
        sws_freeContext(encoder->swsContext);
    }
    if (encoder->frame) {
        av_frame_free(&encoder->frame);
    }
    if (encoder->packet) {
        av_packet_free(&encoder->packet);
    }
    if (encoder->codecContext) {
        avcodec_free_context(&encoder->codecContext);
    }
#endif
    
    delete encoder;
}

bool video_encoder_encode(
    VideoEncoder* encoder,
    const uint8_t* frameData,
    int64_t timestamp
) {
    if (!encoder || !frameData) {
        return false;
    }
    
#ifdef ENABLE_FFMPEG
    if (!encoder->initialized) {
        return false;
    }
    
    std::lock_guard<std::mutex> lock(encoder->mutex);
    
    // Инициализация контекста масштабирования для конвертации RGB -> YUV
    if (!encoder->swsContext) {
        encoder->swsContext = sws_getContext(
            encoder->params.width,
            encoder->params.height,
            AV_PIX_FMT_RGB24,
            encoder->params.width,
            encoder->params.height,
            AV_PIX_FMT_YUV420P,
            SWS_BILINEAR,
            nullptr, nullptr, nullptr
        );
        
        if (!encoder->swsContext) {
            return false;
        }
    }
    
    // Убедимся, что кадр готов
    if (av_frame_make_writable(encoder->frame) < 0) {
        return false;
    }
    
    // Конвертация RGB24 -> YUV420P
    const uint8_t* srcData[1] = {frameData};
    int srcLinesize[1] = {encoder->params.width * 3};
    
    sws_scale(
        encoder->swsContext,
        srcData,
        srcLinesize,
        0,
        encoder->params.height,
        encoder->frame->data,
        encoder->frame->linesize
    );
    
    encoder->frame->pts = encoder->frameCount++;
    
    // Кодирование кадра
    int ret = avcodec_send_frame(encoder->codecContext, encoder->frame);
    if (ret < 0) {
        return false;
    }
    
    // Получение закодированных пакетов
    while (ret >= 0) {
        ret = avcodec_receive_packet(encoder->codecContext, encoder->packet);
        if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF) {
            break;
        }
        if (ret < 0) {
            return false;
        }
        
        // Создание структуры закодированного кадра
        if (encoder->callback) {
            EncodedFrame encodedFrame;
            encodedFrame.dataSize = encoder->packet->size;
            encodedFrame.data = new uint8_t[encodedFrame.dataSize];
            encodedFrame.timestamp = timestamp;
            encodedFrame.isKeyFrame = (encoder->packet->flags & AV_PKT_FLAG_KEY) != 0;
            
            memcpy(encodedFrame.data, encoder->packet->data, encodedFrame.dataSize);
            
            encoder->callback(&encodedFrame, encoder->userData);
        }
        
        av_packet_unref(encoder->packet);
    }
    
    return true;
#else
    // Заглушка без FFmpeg
    return false;
#endif
}

void video_encoder_set_callback(
    VideoEncoder* encoder,
    FrameEncodedCallback callback,
    void* userData
) {
    if (!encoder) return;
    
    std::lock_guard<std::mutex> lock(encoder->mutex);
    encoder->callback = callback;
    encoder->userData = userData;
}

bool video_encoder_get_info(
    VideoEncoder* encoder,
    int* width,
    int* height,
    VideoCodec* codec
) {
    if (!encoder) return false;
    
    std::lock_guard<std::mutex> lock(encoder->mutex);
    
    if (width) *width = encoder->params.width;
    if (height) *height = encoder->params.height;
    if (codec) *codec = encoder->params.codec;
    
    return true;
}

void encoded_frame_release(EncodedFrame* frame) {
    if (frame && frame->data) {
        delete[] frame->data;
        frame->data = nullptr;
    }
}

