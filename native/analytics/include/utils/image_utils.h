#ifndef IMAGE_UTILS_H
#define IMAGE_UTILS_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>
#include <stdbool.h>
#include <stddef.h>

/** Формат пикселей */
typedef enum {
    IMAGE_FORMAT_RGB24,
    IMAGE_FORMAT_BGR24,
    IMAGE_FORMAT_GRAY8
} ImageFormat;

/** Нормализация: 0..1 или -1..1 */
typedef enum {
    NORM_0_1,
    NORM_MINUS1_1
} NormRange;

/**
 * Конвертация RGB24 -> BGR24 (in-place, размер = width * height * 3)
 */
void image_utils_rgb_to_bgr(uint8_t* data, int width, int height);

/**
 * Конвертация BGR24 -> RGB24 (in-place)
 */
void image_utils_bgr_to_rgb(uint8_t* data, int width, int height);

/**
 * Ресайз изображения. out_data должен быть выделен вызывающим (out_width * out_height * channels).
 * channels = 1 (gray) или 3 (RGB/BGR).
 */
bool image_utils_resize(
    const uint8_t* in_data, int in_width, int in_height, int in_channels,
    uint8_t* out_data, int out_width, int out_height
);

/**
 * Нормализация буфера в float. out_data: out_width * out_height * channels * sizeof(float).
 * range: NORM_0_1 или NORM_MINUS1_1.
 */
bool image_utils_normalize(
    const uint8_t* in_data, int width, int height, int channels,
    float* out_data, NormRange range
);

/**
 * Препроцессинг под YOLO: ресайз до targetSize x targetSize, нормализация 0..1, запись в blob (NCHW).
 * blob должен быть выделен: targetSize * targetSize * 3 * sizeof(float).
 */
bool image_utils_preprocess_yolo(
    const uint8_t* in_data, int in_width, int in_height,
    float* blob, int target_size, bool bgr_order
);

#ifdef __cplusplus
}
#endif

#endif /* IMAGE_UTILS_H */
