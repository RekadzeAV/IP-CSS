#ifndef VIDEO_DECODER_H
#define VIDEO_DECODER_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>
#include <stdbool.h>

// Типы кодеков
typedef enum {
    VIDEO_CODEC_H264,
    VIDEO_CODEC_H265,
    VIDEO_CODEC_MJPEG,
    VIDEO_CODEC_UNKNOWN
} VideoCodec;

// Структура декодированного кадра
typedef struct {
    uint8_t* data;          // Данные кадра (YUV или RGB)
    int width;              // Ширина кадра
    int height;             // Высота кадра
    int64_t timestamp;      // Временная метка
    int format;             // Формат пикселей (0=YUV420, 1=RGB24, и т.д.)
    size_t dataSize;        // Размер данных в байтах
} DecodedFrame;

// Callback для получения декодированных кадров
typedef void (*FrameDecodedCallback)(DecodedFrame* frame, void* userData);

// Структура декодера (opaque)
typedef struct VideoDecoder VideoDecoder;

// Создание декодера
VideoDecoder* video_decoder_create(VideoCodec codec, int width, int height);

// Уничтожение декодера
void video_decoder_destroy(VideoDecoder* decoder);

// Декодирование кадра
bool video_decoder_decode(
    VideoDecoder* decoder,
    const uint8_t* data,
    size_t dataSize,
    int64_t timestamp
);

// Установка callback для декодированных кадров
void video_decoder_set_callback(
    VideoDecoder* decoder,
    FrameDecodedCallback callback,
    void* userData
);

// Получение информации о декодере
bool video_decoder_get_info(
    VideoDecoder* decoder,
    int* width,
    int* height,
    VideoCodec* codec
);

// Освобождение кадра
void decoded_frame_release(DecodedFrame* frame);

#ifdef __cplusplus
}
#endif

#endif // VIDEO_DECODER_H


