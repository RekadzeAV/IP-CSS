#ifndef H264_CODEC_H
#define H264_CODEC_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>
#include <stdbool.h>

// Параметры H.264 кодека
typedef struct {
    int profile;            // Профиль (0=baseline, 1=main, 2=high)
    int level;              // Уровень (например, 40 для Level 4.0)
    int preset;            // Пресет (0=ultrafast, 1=fast, 2=medium, 3=slow)
    int crf;                // Constant Rate Factor (0-51, 23 по умолчанию)
    bool useBframes;        // Использовать B-кадры
    int maxBframes;         // Максимальное количество B-кадров
    bool useCabac;          // Использовать CABAC (Context-Adaptive Binary Arithmetic Coding)
} H264CodecParams;

// Проверка поддержки H.264
bool h264_codec_is_supported();

// Проверка аппаратного ускорения H.264
bool h264_codec_has_hardware_acceleration();

// Получение информации о H.264 кодеке
bool h264_codec_get_info(int* maxWidth, int* maxHeight, bool* hardwareAccelerated);

// Установка параметров H.264 кодека
bool h264_codec_set_params(void* encoderContext, const H264CodecParams* params);

#ifdef __cplusplus
}
#endif

#endif // H264_CODEC_H



