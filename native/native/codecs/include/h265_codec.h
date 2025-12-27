#ifndef H265_CODEC_H
#define H265_CODEC_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>
#include <stdbool.h>

// Параметры H.265/HEVC кодека
typedef struct {
    int profile;            // Профиль (0=main, 1=main10)
    int tier;               // Уровень (0=main, 1=high)
    int level;              // Уровень (например, 120 для Level 4.0)
    int preset;            // Пресет (0=ultrafast, 1=fast, 2=medium, 3=slow)
    int crf;                // Constant Rate Factor (0-51, 28 по умолчанию)
    bool useBframes;        // Использовать B-кадры
    int maxBframes;         // Максимальное количество B-кадров
} H265CodecParams;

// Проверка поддержки H.265/HEVC
bool h265_codec_is_supported();

// Проверка аппаратного ускорения H.265/HEVC
bool h265_codec_has_hardware_acceleration();

// Получение информации о H.265/HEVC кодеке
bool h265_codec_get_info(int* maxWidth, int* maxHeight, bool* hardwareAccelerated);

// Установка параметров H.265/HEVC кодека
bool h265_codec_set_params(void* encoderContext, const H265CodecParams* params);

#ifdef __cplusplus
}
#endif

#endif // H265_CODEC_H



