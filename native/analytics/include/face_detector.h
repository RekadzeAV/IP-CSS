#ifndef FACE_DETECTOR_H
#define FACE_DETECTOR_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>
#include <stdbool.h>

// Обнаруженное лицо
typedef struct {
    float confidence;
    int x, y, width, height; // Bounding box
    int landmarks[10];        // Ключевые точки (5 точек: глаза, нос, уголки рта)
} DetectedFace;

// Результат детекции лиц
typedef struct {
    DetectedFace* faces;
    int faceCount;
} FaceDetectionResult;

// Параметры детекции
typedef struct {
    float scaleFactor;        // Фактор масштабирования для каскада
    int minNeighbors;         // Минимальное количество соседей
    int minSize;              // Минимальный размер лица
    int maxSize;              // Максимальный размер лица
} FaceDetectorParams;

// Структура детектора (opaque)
typedef struct FaceDetector FaceDetector;

// Создание детектора лиц
FaceDetector* face_detector_create(const FaceDetectorParams* params);

// Уничтожение детектора
void face_detector_destroy(FaceDetector* detector);

// Загрузка модели каскада
bool face_detector_load_cascade(FaceDetector* detector, const char* cascadePath);

// Детекция лиц в кадре
bool face_detector_detect(
    FaceDetector* detector,
    const uint8_t* frameData,
    int width,
    int height,
    FaceDetectionResult* result
);

// Освобождение результата
void face_detection_result_release(FaceDetectionResult* result);

#ifdef __cplusplus
}
#endif

#endif // FACE_DETECTOR_H


