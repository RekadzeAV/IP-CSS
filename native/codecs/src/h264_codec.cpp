#include "h264_codec.h"

#ifdef ENABLE_FFMPEG
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/opt.h>
}
#endif

bool h264_codec_is_supported() {
#ifdef ENABLE_FFMPEG
    const AVCodec* codec = avcodec_find_encoder(AV_CODEC_ID_H264);
    return codec != nullptr;
#else
    return false;
#endif
}

bool h264_codec_has_hardware_acceleration() {
#ifdef ENABLE_FFMPEG
    // Проверка аппаратных кодеков H.264
    const AVCodec* hwCodecs[] = {
        avcodec_find_encoder_by_name("h264_nvenc"),      // NVIDIA
        avcodec_find_encoder_by_name("h264_qsv"),        // Intel Quick Sync
        avcodec_find_encoder_by_name("h264_videotoolbox"), // Apple VideoToolbox
        avcodec_find_encoder_by_name("h264_omx"),        // OpenMAX
        avcodec_find_encoder_by_name("h264_v4l2m2m"),    // V4L2
        nullptr
    };

    for (int i = 0; hwCodecs[i] != nullptr; i++) {
        if (hwCodecs[i] != nullptr) {
            return true;
        }
    }
#endif
    return false;
}

bool h264_codec_get_info(int* maxWidth, int* maxHeight, bool* hardwareAccelerated) {
    if (!maxWidth || !maxHeight || !hardwareAccelerated) {
        return false;
    }

#ifdef ENABLE_FFMPEG
    const AVCodec* codec = avcodec_find_encoder(AV_CODEC_ID_H264);
    if (!codec) {
        return false;
    }

    *maxWidth = 7680;  // 8K
    *maxHeight = 4320;
    *hardwareAccelerated = h264_codec_has_hardware_acceleration();
    return true;
#else
    *maxWidth = 1920;
    *maxHeight = 1080;
    *hardwareAccelerated = false;
    return false;
#endif
}

bool h264_codec_set_params(void* encoderContext, const H264CodecParams* params) {
    if (!encoderContext || !params) {
        return false;
    }

#ifdef ENABLE_FFMPEG
    AVCodecContext* ctx = static_cast<AVCodecContext*>(encoderContext);

    // Установка профиля
    switch (params->profile) {
        case 0: // baseline
            ctx->profile = FF_PROFILE_H264_BASELINE;
            break;
        case 1: // main
            ctx->profile = FF_PROFILE_H264_MAIN;
            break;
        case 2: // high
            ctx->profile = FF_PROFILE_H264_HIGH;
            break;
        default:
            ctx->profile = FF_PROFILE_H264_MAIN;
    }

    // Установка уровня
    if (params->level > 0) {
        ctx->level = params->level;
    }

    // Установка CRF (Constant Rate Factor)
    if (params->crf >= 0 && params->crf <= 51) {
        av_opt_set_int(ctx->priv_data, "crf", params->crf, 0);
    }

    // Установка пресета
    const char* presets[] = {"ultrafast", "fast", "medium", "slow"};
    if (params->preset >= 0 && params->preset < 4) {
        av_opt_set(ctx->priv_data, "preset", presets[params->preset], 0);
    }

    // Настройка B-кадров
    if (params->useBframes) {
        ctx->max_b_frames = params->maxBframes > 0 ? params->maxBframes : 2;
    } else {
        ctx->max_b_frames = 0;
    }

    // CABAC
    if (params->useCabac) {
        av_opt_set_int(ctx->priv_data, "cabac", 1, 0);
    } else {
        av_opt_set_int(ctx->priv_data, "cabac", 0, 0);
    }

    return true;
#else
    return false;
#endif
}



