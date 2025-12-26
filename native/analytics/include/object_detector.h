#ifndef OBJECT_DETECTOR_H
#define OBJECT_DETECTOR_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>
#include <stdbool.h>

// Тип объекта
typedef enum {
    OBJECT_TYPE_PERSON,
    OBJECT_TYPE_VEHICLE,
    OBJECT_TYPE_BICYCLE,
    OBJECT_TYPE_MOTORCYCLE,
    OBJECT_TYPE_UNKNOWN
} ObjectType;

// Обнаруженный объект
typedef struct {
    ObjectType type;
    float confidence;       // 0.0 - 1.0
    int x, y, width, height; // Bounding box
} DetectedObject;

// Результат детекции
typedef struct {
    DetectedObject* objects;
    int objectCount;
} DetectionResult;

// Параметры детекции
typedef struct {
    float confidenceThreshold;  // Минимальный порог уверенности
    int maxObjects;             // Максимальное количество объектов
    bool useGPU;                // Использовать GPU ускорение
} ObjectDetectorParams;

// Структура детектора (opaque)
typedef struct ObjectDetector ObjectDetector;

// Создание детектора объектов
ObjectDetector* object_detector_create(const ObjectDetectorParams* params);

// Уничтожение детектора
void object_detector_destroy(ObjectDetector* detector);

// Загрузка модели
bool object_detector_load_model(ObjectDetector* detector, const char* modelPath);

// Детекция объектов в кадре
bool object_detector_detect(
    ObjectDetector* detector,
    const uint8_t* frameData,  // RGB24 данные
    int width,
    int height,
    DetectionResult* result
);

// Освобождение результата детекции
void detection_result_release(DetectionResult* result);

#ifdef __cplusplus
}
#endif

#endif // OBJECT_DETECTOR_H

