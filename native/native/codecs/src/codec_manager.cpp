#include "codec_manager.h"
#include "h264_codec.h"
#include "h265_codec.h"
#include "mjpeg_codec.h"

#ifdef ENABLE_FFMPEG
extern "C" {
#include <libavcodec/avcodec.h>
}
#endif

bool codec_get_info(CodecType type, CodecInfo* info) {
    if (!info) return false;

    bool supported = false;
    int maxWidth = 0;
    int maxHeight = 0;
    bool hwAccel = false;

    switch (type) {
        case CODEC_TYPE_H264:
            supported = h264_codec_get_info(&maxWidth, &maxHeight, &hwAccel);
            if (supported) {
                info->type = CODEC_TYPE_H264;
                info->name = "H.264";
                info->hardwareAccelerated = hwAccel;
                info->maxWidth = maxWidth;
                info->maxHeight = maxHeight;
            }
            break;

        case CODEC_TYPE_H265:
            supported = h265_codec_get_info(&maxWidth, &maxHeight, &hwAccel);
            if (supported) {
                info->type = CODEC_TYPE_H265;
                info->name = "H.265/HEVC";
                info->hardwareAccelerated = hwAccel;
                info->maxWidth = maxWidth;
                info->maxHeight = maxHeight;
            }
            break;

        case CODEC_TYPE_MJPEG:
            supported = mjpeg_codec_get_info(&maxWidth, &maxHeight, &hwAccel);
            if (supported) {
                info->type = CODEC_TYPE_MJPEG;
                info->name = "MJPEG";
                info->hardwareAccelerated = hwAccel;
                info->maxWidth = maxWidth;
                info->maxHeight = maxHeight;
            }
            break;

        default:
            return false;
    }

    return supported;
}

bool codec_is_supported(CodecType type) {
    switch (type) {
        case CODEC_TYPE_H264:
            return h264_codec_is_supported();
        case CODEC_TYPE_H265:
            return h265_codec_is_supported();
        case CODEC_TYPE_MJPEG:
            return mjpeg_codec_is_supported();
        default:
            return false;
    }
}

bool codec_has_hardware_acceleration(CodecType type) {
    switch (type) {
        case CODEC_TYPE_H264:
            return h264_codec_has_hardware_acceleration();
        case CODEC_TYPE_H265:
            return h265_codec_has_hardware_acceleration();
        case CODEC_TYPE_MJPEG:
            return mjpeg_codec_has_hardware_acceleration();
        default:
            return false;
    }
}

