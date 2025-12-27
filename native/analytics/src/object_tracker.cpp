#include "object_tracker.h"
#include <memory>
#include <mutex>
#include <vector>
#include <algorithm>
#include <cmath>

struct TrackedObjectInternal {
    int id;
    ObjectType type;
    float confidence;
    int x, y, width, height;
    int64_t lastSeen;
    int age;
    int hits;
    
    TrackedObjectInternal() : id(-1), type(OBJECT_TYPE_UNKNOWN), confidence(0.0f),
                              x(0), y(0), width(0), height(0), lastSeen(0),
                              age(0), hits(0) {}
};

struct ObjectTracker {
    ObjectTrackerParams params;
    std::mutex mutex;
    std::vector<TrackedObjectInternal> tracks;
    int nextTrackId;
};

// Вычисление IoU (Intersection over Union)
static float calculate_iou(int x1, int y1, int w1, int h1, int x2, int y2, int w2, int h2) {
    int left = std::max(x1, x2);
    int top = std::max(y1, y2);
    int right = std::min(x1 + w1, x2 + w2);
    int bottom = std::min(y1 + h1, y2 + h2);
    
    if (right < left || bottom < top) {
        return 0.0f;
    }
    
    int intersection = (right - left) * (bottom - top);
    int area1 = w1 * h1;
    int area2 = w2 * h2;
    int unionArea = area1 + area2 - intersection;
    
    if (unionArea == 0) {
        return 0.0f;
    }
    
    return static_cast<float>(intersection) / unionArea;
}

ObjectTracker* object_tracker_create(const ObjectTrackerParams* params) {
    auto* tracker = new ObjectTracker();
    
    if (params) {
        tracker->params = *params;
    } else {
        tracker->params.iouThreshold = 0.3f;
        tracker->params.maxAge = 30;
        tracker->params.minConfidence = 0.5f;
    }
    
    tracker->nextTrackId = 1;
    
    return tracker;
}

void object_tracker_destroy(ObjectTracker* tracker) {
    if (tracker) {
        delete tracker;
    }
}

bool object_tracker_update(
    ObjectTracker* tracker,
    const DetectionResult* detections,
    TrackingResult* result
) {
    if (!tracker || !detections || !result) {
        return false;
    }
    
    std::lock_guard<std::mutex> lock(tracker->mutex);
    
    // Увеличение возраста всех треков
    for (auto& track : tracker->tracks) {
        track.age++;
    }
    
    // Удаление старых треков
    tracker->tracks.erase(
        std::remove_if(
            tracker->tracks.begin(),
            tracker->tracks.end(),
            [&](const TrackedObjectInternal& track) {
                return track.age > tracker->params.maxAge;
            }
        ),
        tracker->tracks.end()
    );
    
    // Сопоставление детекций с существующими треками
    std::vector<bool> matched(detections->objectCount, false);
    
    for (auto& track : tracker->tracks) {
        float bestIou = 0.0f;
        int bestMatch = -1;
        
        for (int i = 0; i < detections->objectCount; i++) {
            if (matched[i]) continue;
            if (detections->objects[i].type != track.type) continue;
            if (detections->objects[i].confidence < tracker->params.minConfidence) continue;
            
            float iou = calculate_iou(
                track.x, track.y, track.width, track.height,
                detections->objects[i].x, detections->objects[i].y,
                detections->objects[i].width, detections->objects[i].height
            );
            
            if (iou > bestIou && iou > tracker->params.iouThreshold) {
                bestIou = iou;
                bestMatch = i;
            }
        }
        
        if (bestMatch >= 0) {
            // Обновление трека
            const DetectedObject& det = detections->objects[bestMatch];
            track.x = det.x;
            track.y = det.y;
            track.width = det.width;
            track.height = det.height;
            track.confidence = det.confidence;
            track.age = 0;
            track.hits++;
            matched[bestMatch] = true;
        }
    }
    
    // Создание новых треков для несопоставленных детекций
    for (int i = 0; i < detections->objectCount; i++) {
        if (matched[i]) continue;
        if (detections->objects[i].confidence < tracker->params.minConfidence) continue;
        
        TrackedObjectInternal newTrack;
        newTrack.id = tracker->nextTrackId++;
        newTrack.type = detections->objects[i].type;
        newTrack.confidence = detections->objects[i].confidence;
        newTrack.x = detections->objects[i].x;
        newTrack.y = detections->objects[i].y;
        newTrack.width = detections->objects[i].width;
        newTrack.height = detections->objects[i].height;
        newTrack.age = 0;
        newTrack.hits = 1;
        
        tracker->tracks.push_back(newTrack);
    }
    
    // Удаление треков с малым количеством попаданий
    tracker->tracks.erase(
        std::remove_if(
            tracker->tracks.begin(),
            tracker->tracks.end(),
            [](const TrackedObjectInternal& track) {
                return track.hits < 3 && track.age > 5;
            }
        ),
        tracker->tracks.end()
    );
    
    // Заполнение результата
    result->objectCount = static_cast<int>(tracker->tracks.size());
    if (result->objectCount > 0) {
        result->objects = new TrackedObject[result->objectCount];
        for (size_t i = 0; i < tracker->tracks.size(); i++) {
            const TrackedObjectInternal& track = tracker->tracks[i];
            result->objects[i].id = track.id;
            result->objects[i].type = track.type;
            result->objects[i].confidence = track.confidence;
            result->objects[i].x = track.x;
            result->objects[i].y = track.y;
            result->objects[i].width = track.width;
            result->objects[i].height = track.height;
            result->objects[i].lastSeen = track.lastSeen;
        }
    } else {
        result->objects = nullptr;
    }
    
    return true;
}

void tracking_result_release(TrackingResult* result) {
    if (result && result->objects) {
        delete[] result->objects;
        result->objects = nullptr;
        result->objectCount = 0;
    }
}


