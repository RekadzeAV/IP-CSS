#include "face_detector.h"
#include <memory>
#include <mutex>
#include <vector>

#ifdef ENABLE_OPENCV
#include <opencv2/opencv.hpp>
#include <opencv2/objdetect.hpp>
#endif

struct FaceDetector {
    FaceDetectorParams params;
    std::mutex mutex;
    bool cascadeLoaded;
    
#ifdef ENABLE_OPENCV
    cv::CascadeClassifier faceCascade;
#endif
};

FaceDetector* face_detector_create(const FaceDetectorParams* params) {
    auto* detector = new FaceDetector();
    
    if (params) {
        detector->params = *params;
    } else {
        detector->params.scaleFactor = 1.1f;
        detector->params.minNeighbors = 3;
        detector->params.minSize = 30;
        detector->params.maxSize = 0; // 0 = без ограничения
    }
    
    detector->cascadeLoaded = false;
    
    return detector;
}

void face_detector_destroy(FaceDetector* detector) {
    if (detector) {
        delete detector;
    }
}

bool face_detector_load_cascade(FaceDetector* detector, const char* cascadePath) {
    if (!detector || !cascadePath) {
        return false;
    }
    
    std::lock_guard<std::mutex> lock(detector->mutex);
    
#ifdef ENABLE_OPENCV
    if (detector->faceCascade.load(cascadePath)) {
        detector->cascadeLoaded = true;
        return true;
    }
#endif
    
    return false;
}

bool face_detector_detect(
    FaceDetector* detector,
    const uint8_t* frameData,
    int width,
    int height,
    FaceDetectionResult* result
) {
    if (!detector || !frameData || !result) {
        return false;
    }
    
    if (!detector->cascadeLoaded) {
        return false;
    }
    
    std::lock_guard<std::mutex> lock(detector->mutex);
    
    result->faces = nullptr;
    result->faceCount = 0;
    
#ifdef ENABLE_OPENCV
    try {
        cv::Mat frame(height, width, CV_8UC3, const_cast<uint8_t*>(frameData));
        cv::Mat gray;
        cv::cvtColor(frame, gray, cv::COLOR_RGB2GRAY);
        
        // Детекция лиц
        std::vector<cv::Rect> faces;
        cv::Size minSize(detector->params.minSize, detector->params.minSize);
        cv::Size maxSize;
        if (detector->params.maxSize > 0) {
            maxSize = cv::Size(detector->params.maxSize, detector->params.maxSize);
        }
        
        detector->faceCascade.detectMultiScale(
            gray,
            faces,
            detector->params.scaleFactor,
            detector->params.minNeighbors,
            0,
            minSize,
            maxSize
        );
        
        // Конвертация результатов
        if (faces.size() > 0) {
            result->faceCount = static_cast<int>(faces.size());
            result->faces = new DetectedFace[result->faceCount];
            
            for (size_t i = 0; i < faces.size(); i++) {
                result->faces[i].confidence = 1.0f; // Каскад не возвращает уверенность
                result->faces[i].x = faces[i].x;
                result->faces[i].y = faces[i].y;
                result->faces[i].width = faces[i].width;
                result->faces[i].height = faces[i].height;
                
                // Инициализация landmarks (заглушка)
                for (int j = 0; j < 10; j++) {
                    result->faces[i].landmarks[j] = 0;
                }
            }
        }
        
        return true;
        
    } catch (const cv::Exception& e) {
        return false;
    }
#else
    // Заглушка без OpenCV
    return false;
#endif
}

void face_detection_result_release(FaceDetectionResult* result) {
    if (result && result->faces) {
        delete[] result->faces;
        result->faces = nullptr;
        result->faceCount = 0;
    }
}


