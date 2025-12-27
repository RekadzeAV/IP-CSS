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

CodecType codec_select_best(CodecType preferredType, bool preferHardware) {
    // Список кодеков для проверки в порядке приоритета
    CodecType codecsToCheck[] = {
        preferredType,
        CODEC_TYPE_H264,
        CODEC_TYPE_H265,
        CODEC_TYPE_MJPEG
    };

    CodecType bestCodec = CODEC_TYPE_H264; // По умолчанию
    bool bestHasHw = false;
    bool foundPreferred = false;

    // Сначала проверяем предпочтительный кодек
    for (int i = 0; i < 4; i++) {
        CodecType codec = codecsToCheck[i];

        // Пропускаем дубликаты
        if (i > 0 && codec == preferredType) {
            continue;
        }

        if (!codec_is_supported(codec)) {
            continue;
        }

        bool hasHw = codec_has_hardware_acceleration(codec);

        // Если предпочитаем аппаратное ускорение
        if (preferHardware) {
            if (hasHw) {
                // Нашли кодек с аппаратным ускорением
                if (codec == preferredType || !foundPreferred) {
                    bestCodec = codec;
                    bestHasHw = true;
                    if (codec == preferredType) {
                        foundPreferred = true;
                        break; // Предпочтительный с HW - идеально
                    }
                }
            } else if (!bestHasHw && !foundPreferred) {
                // Сохраняем как запасной вариант (без HW)
                bestCodec = codec;
            }
        } else {
            // Не предпочитаем аппаратное ускорение, выбираем первый поддерживаемый
            if (codec == preferredType || !foundPreferred) {
                bestCodec = codec;
                bestHasHw = hasHw;
                if (codec == preferredType) {
                    foundPreferred = true;
                    break;
                }
            }
        }
    }

    // Если предпочитаем HW, но не нашли, ищем любой с HW
    if (preferHardware && !bestHasHw) {
        for (int i = 0; i < 3; i++) {
            CodecType codec = static_cast<CodecType>(i);
            if (codec_is_supported(codec) && codec_has_hardware_acceleration(codec)) {
                bestCodec = codec;
                bestHasHw = true;
                break;
            }
        }
    }

    return bestCodec;
}

