#ifndef MJPEG_CODEC_H
#define MJPEG_CODEC_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>
#include <stdbool.h>

// Параметры MJPEG кодека
typedef struct {
    int quality;            // Качество JPEG (1-100, 90 по умолчанию)
    bool optimizeHuffman;   // Оптимизация таблиц Хаффмана
    bool progressive;       // Прогрессивный JPEG
} MjpegCodecParams;

// Проверка поддержки MJPEG
bool mjpeg_codec_is_supported();

// Проверка аппаратного ускорения MJPEG
bool mjpeg_codec_has_hardware_acceleration();

// Получение информации о MJPEG кодеке
bool mjpeg_codec_get_info(int* maxWidth, int* maxHeight, bool* hardwareAccelerated);

// Установка параметров MJPEG кодека
bool mjpeg_codec_set_params(void* encoderContext, const MjpegCodecParams* params);

#ifdef __cplusplus
}
#endif

#endif // MJPEG_CODEC_H



