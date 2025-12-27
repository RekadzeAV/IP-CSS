#include "mjpeg_codec.h"

#ifdef ENABLE_FFMPEG
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/opt.h>
}
#endif

bool mjpeg_codec_is_supported() {
#ifdef ENABLE_FFMPEG
    const AVCodec* codec = avcodec_find_encoder(AV_CODEC_ID_MJPEG);
    return codec != nullptr;
#else
    return false;
#endif
}

bool mjpeg_codec_has_hardware_acceleration() {
#ifdef ENABLE_FFMPEG
    // Проверка аппаратных кодеков MJPEG
    const AVCodec* hwCodecs[] = {
        avcodec_find_encoder_by_name("mjpeg_qsv"),        // Intel Quick Sync
        avcodec_find_encoder_by_name("mjpeg_videotoolbox"), // Apple VideoToolbox
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

bool mjpeg_codec_get_info(int* maxWidth, int* maxHeight, bool* hardwareAccelerated) {
    if (!maxWidth || !maxHeight || !hardwareAccelerated) {
        return false;
    }

#ifdef ENABLE_FFMPEG
    const AVCodec* codec = avcodec_find_encoder(AV_CODEC_ID_MJPEG);
    if (!codec) {
        return false;
    }

    *maxWidth = 8192;  // MJPEG поддерживает большие разрешения
    *maxHeight = 8192;
    *hardwareAccelerated = mjpeg_codec_has_hardware_acceleration();
    return true;
#else
    *maxWidth = 1920;
    *maxHeight = 1080;
    *hardwareAccelerated = false;
    return false;
#endif
}

bool mjpeg_codec_set_params(void* encoderContext, const MjpegCodecParams* params) {
    if (!encoderContext || !params) {
        return false;
    }

#ifdef ENABLE_FFMPEG
    AVCodecContext* ctx = static_cast<AVCodecContext*>(encoderContext);

    // Установка качества JPEG (1-100)
    if (params->quality >= 1 && params->quality <= 100) {
        ctx->qmin = ctx->qmax = 2 + (100 - params->quality) * 31 / 100;
    }

    // Оптимизация таблиц Хаффмана
    if (params->optimizeHuffman) {
        av_opt_set_int(ctx->priv_data, "huffman", 1, 0);
    }

    // Прогрессивный JPEG
    if (params->progressive) {
        av_opt_set_int(ctx->priv_data, "progressive", 1, 0);
    }

    return true;
#else
    return false;
#endif
}



