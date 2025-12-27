#ifndef FRAME_PROCESSOR_H
#define FRAME_PROCESSOR_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>
#include <stdbool.h>

// Операции обработки кадров
typedef enum {
    FRAME_OP_RESIZE,
    FRAME_OP_ROTATE,
    FRAME_OP_FLIP_HORIZONTAL,
    FRAME_OP_FLIP_VERTICAL,
    FRAME_OP_CROP,
    FRAME_OP_BRIGHTNESS,
    FRAME_OP_CONTRAST,
    FRAME_OP_SATURATION,
    FRAME_OP_GRAYSCALE,
    FRAME_OP_BLUR,
    FRAME_OP_SHARPEN
} FrameOperation;

// Структура обработанного кадра
typedef struct {
    uint8_t* data;
    int width;
    int height;
    int format;  // 0=YUV420, 1=RGB24, 2=GRAYSCALE
    size_t dataSize;
} ProcessedFrame;

// Параметры обработки
typedef struct {
    FrameOperation operation;
    union {
        struct {
            int width;
            int height;
        } resize;
        struct {
            int angle;  // 90, 180, 270
        } rotate;
        struct {
            int x, y, width, height;
        } crop;
        struct {
            float value;  // -1.0 to 1.0
        } brightness;
        struct {
            float value;  // -1.0 to 1.0
        } contrast;
        struct {
            float value;  // -1.0 to 1.0
        } saturation;
        struct {
            int radius;  // Размер ядра
        } blur;
    } params;
} ProcessingParams;

// Структура процессора (opaque)
typedef struct FrameProcessor FrameProcessor;

// Создание процессора
FrameProcessor* frame_processor_create();

// Уничтожение процессора
void frame_processor_destroy(FrameProcessor* processor);

// Обработка кадра
bool frame_processor_process(
    FrameProcessor* processor,
    const uint8_t* inputData,
    int inputWidth,
    int inputHeight,
    int inputFormat,
    const ProcessingParams* params,
    ProcessedFrame* output
);

// Освобождение обработанного кадра
void processed_frame_release(ProcessedFrame* frame);

#ifdef __cplusplus
}
#endif

#endif // FRAME_PROCESSOR_H


