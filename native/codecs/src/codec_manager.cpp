#include "codec_manager.h"

#ifdef ENABLE_FFMPEG
extern "C" {
#include <libavcodec/avcodec.h>
}
#endif

bool codec_get_info(CodecType type, CodecInfo* info) {
    if (!info) return false;

#ifdef ENABLE_FFMPEG
    const AVCodec* codec = nullptr;

    switch (type) {
        case CODEC_TYPE_H264:
            codec = avcodec_find_encoder(AV_CODEC_ID_H264);
            if (codec) {
                info->type = CODEC_TYPE_H264;
                info->name = "H.264";
                info->hardwareAccelerated = false; // TODO: проверка аппаратного ускорения
                info->maxWidth = 7680;  // 8K
                info->maxHeight = 4320;
            }
            break;

        case CODEC_TYPE_H265:
            codec = avcodec_find_encoder(AV_CODEC_ID_HEVC);
            if (codec) {
                info->type = CODEC_TYPE_H265;
                info->name = "H.265/HEVC";
                info->hardwareAccelerated = false; // TODO: проверка аппаратного ускорения
                info->maxWidth = 7680;
                info->maxHeight = 4320;
            }
            break;

        case CODEC_TYPE_MJPEG:
            codec = avcodec_find_encoder(AV_CODEC_ID_MJPEG);
            if (codec) {
                info->type = CODEC_TYPE_MJPEG;
                info->name = "MJPEG";
                info->hardwareAccelerated = false;
                info->maxWidth = 8192;
                info->maxHeight = 8192;
            }
            break;

        default:
            return false;
    }

    return codec != nullptr;
#else
    // Заглушка без FFmpeg
    switch (type) {
        case CODEC_TYPE_H264:
            info->type = CODEC_TYPE_H264;
            info->name = "H.264";
            info->hardwareAccelerated = false;
            info->maxWidth = 1920;
            info->maxHeight = 1080;
            return true;

        case CODEC_TYPE_H265:
            info->type = CODEC_TYPE_H265;
            info->name = "H.265/HEVC";
            info->hardwareAccelerated = false;
            info->maxWidth = 1920;
            info->maxHeight = 1080;
            return true;

        case CODEC_TYPE_MJPEG:
            info->type = CODEC_TYPE_MJPEG;
            info->name = "MJPEG";
            info->hardwareAccelerated = false;
            info->maxWidth = 1920;
            info->maxHeight = 1080;
            return true;

        default:
            return false;
    }
#endif
}

bool codec_is_supported(CodecType type) {
#ifdef ENABLE_FFMPEG
    const AVCodec* codec = nullptr;

    switch (type) {
        case CODEC_TYPE_H264:
            codec = avcodec_find_encoder(AV_CODEC_ID_H264);
            break;
        case CODEC_TYPE_H265:
            codec = avcodec_find_encoder(AV_CODEC_ID_HEVC);
            break;
        case CODEC_TYPE_MJPEG:
            codec = avcodec_find_encoder(AV_CODEC_ID_MJPEG);
            break;
        default:
            return false;
    }

    return codec != nullptr;
#else
    return true; // Заглушка
#endif
}

bool codec_has_hardware_acceleration(CodecType type) {
    // TODO: Реализовать проверку аппаратного ускорения (CUDA, VideoToolbox, MediaCodec)
    return false;
}

