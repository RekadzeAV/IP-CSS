#ifndef VIDEO_ENCODER_H
#define VIDEO_ENCODER_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>
#include <stdbool.h>
#include "video_decoder.h" // Для VideoCodec

// Параметры кодирования
typedef struct {
    int width;
    int height;
    int fps;
    int bitrate;            // Битрейт в битах/сек
    int gopSize;            // Размер группы кадров (GOP)
    VideoCodec codec;
} EncodingParams;

// Структура закодированного кадра
typedef struct {
    uint8_t* data;          // Закодированные данные
    size_t dataSize;         // Размер данных
    int64_t timestamp;       // Временная метка
    bool isKeyFrame;         // Является ли ключевым кадром
} EncodedFrame;

// Callback для получения закодированных кадров
typedef void (*FrameEncodedCallback)(EncodedFrame* frame, void* userData);

// Структура энкодера (opaque)
typedef struct VideoEncoder VideoEncoder;

// Создание энкодера
VideoEncoder* video_encoder_create(const EncodingParams* params);

// Уничтожение энкодера
void video_encoder_destroy(VideoEncoder* encoder);

// Кодирование кадра
bool video_encoder_encode(
    VideoEncoder* encoder,
    const uint8_t* frameData,  // RGB24 данные
    int64_t timestamp
);

// Установка callback для закодированных кадров
void video_encoder_set_callback(
    VideoEncoder* encoder,
    FrameEncodedCallback callback,
    void* userData
);

// Получение информации об энкодере
bool video_encoder_get_info(
    VideoEncoder* encoder,
    int* width,
    int* height,
    VideoCodec* codec
);

// Освобождение закодированного кадра
void encoded_frame_release(EncodedFrame* frame);

#ifdef __cplusplus
}
#endif

#endif // VIDEO_ENCODER_H



