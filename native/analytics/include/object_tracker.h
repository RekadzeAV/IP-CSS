#ifndef OBJECT_TRACKER_H
#define OBJECT_TRACKER_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>
#include <stdbool.h>
#include "object_detector.h"

// Отслеживаемый объект
typedef struct {
    int id;                 // Уникальный ID объекта
    ObjectType type;
    float confidence;
    int x, y, width, height;
    int64_t lastSeen;       // Временная метка последнего обнаружения
} TrackedObject;

// Результат трекинга
typedef struct {
    TrackedObject* objects;
    int objectCount;
} TrackingResult;

// Параметры трекинга
typedef struct {
    float iouThreshold;     // Порог IoU для сопоставления
    int maxAge;             // Максимальный возраст объекта без обновления (в кадрах)
    float minConfidence;    // Минимальная уверенность для инициализации трека
} ObjectTrackerParams;

// Структура трекера (opaque)
typedef struct ObjectTracker ObjectTracker;

// Создание трекера
ObjectTracker* object_tracker_create(const ObjectTrackerParams* params);

// Уничтожение трекера
void object_tracker_destroy(ObjectTracker* tracker);

// Обновление треков на основе новых детекций
bool object_tracker_update(
    ObjectTracker* tracker,
    const DetectionResult* detections,
    TrackingResult* result
);

// Освобождение результата
void tracking_result_release(TrackingResult* result);

#ifdef __cplusplus
}
#endif

#endif // OBJECT_TRACKER_H


