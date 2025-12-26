#ifndef CODEC_MANAGER_H
#define CODEC_MANAGER_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>
#include <stdbool.h>

// Типы кодеков
typedef enum {
    CODEC_TYPE_H264,
    CODEC_TYPE_H265,
    CODEC_TYPE_MJPEG
} CodecType;

// Информация о кодеке
typedef struct {
    CodecType type;
    const char* name;
    bool hardwareAccelerated;
    int maxWidth;
    int maxHeight;
} CodecInfo;

// Получение информации о кодеке
bool codec_get_info(CodecType type, CodecInfo* info);

// Проверка поддержки кодека
bool codec_is_supported(CodecType type);

// Проверка аппаратного ускорения
bool codec_has_hardware_acceleration(CodecType type);

#ifdef __cplusplus
}
#endif

#endif // CODEC_MANAGER_H

