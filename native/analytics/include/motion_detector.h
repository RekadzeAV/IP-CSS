#ifndef MOTION_DETECTOR_H
#define MOTION_DETECTOR_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>
#include <stdbool.h>

// Результат детекции движения
typedef struct {
    bool motionDetected;
    float confidence;       // 0.0 - 1.0
    int x, y, width, height; // Область движения
} MotionDetectionResult;

// Параметры детекции движения
typedef struct {
    float threshold;        // Порог чувствительности (0.0 - 1.0)
    int minArea;           // Минимальная область движения в пикселях
    bool useGaussianBlur;  // Использовать размытие для уменьшения шума
    int blurSize;          // Размер ядра размытия
} MotionDetectorParams;

// Структура детектора (opaque)
typedef struct MotionDetector MotionDetector;

// Создание детектора движения
MotionDetector* motion_detector_create(int width, int height, const MotionDetectorParams* params);

// Уничтожение детектора
void motion_detector_destroy(MotionDetector* detector);

// Детекция движения в кадре
bool motion_detector_detect(
    MotionDetector* detector,
    const uint8_t* frameData,  // RGB24 данные
    int width,
    int height,
    MotionDetectionResult* result
);

// Обновление параметров
bool motion_detector_set_params(MotionDetector* detector, const MotionDetectorParams* params);

#ifdef __cplusplus
}
#endif

#endif // MOTION_DETECTOR_H



