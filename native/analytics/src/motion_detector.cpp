#include "motion_detector.h"
#include <memory>
#include <mutex>

#ifdef ENABLE_OPENCV
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/video.hpp>
#endif

struct MotionDetector {
    int width;
    int height;
    MotionDetectorParams params;
    std::mutex mutex;
    
#ifdef ENABLE_OPENCV
    cv::Ptr<cv::BackgroundSubtractor> bgSubtractor;
    cv::Mat previousFrame;
    bool initialized;
#endif
};

MotionDetector* motion_detector_create(int width, int height, const MotionDetectorParams* params) {
    auto* detector = new MotionDetector();
    detector->width = width;
    detector->height = height;
    
    if (params) {
        detector->params = *params;
    } else {
        // Параметры по умолчанию
        detector->params.threshold = 0.5f;
        detector->params.minArea = 500;
        detector->params.useGaussianBlur = true;
        detector->params.blurSize = 5;
    }
    
#ifdef ENABLE_OPENCV
    // Использование MOG2 для детекции движения
    detector->bgSubtractor = cv::createBackgroundSubtractorMOG2(500, 16.0, false);
    detector->initialized = false;
#endif
    
    return detector;
}

void motion_detector_destroy(MotionDetector* detector) {
    if (detector) {
        delete detector;
    }
}

bool motion_detector_detect(
    MotionDetector* detector,
    const uint8_t* frameData,
    int width,
    int height,
    MotionDetectionResult* result
) {
    if (!detector || !frameData || !result) {
        return false;
    }
    
    if (width != detector->width || height != detector->height) {
        return false;
    }
    
#ifdef ENABLE_OPENCV
    std::lock_guard<std::mutex> lock(detector->mutex);
    
    try {
        // Создание OpenCV Mat из RGB данных
        cv::Mat frame(height, width, CV_8UC3, const_cast<uint8_t*>(frameData));
        cv::Mat gray;
        cv::cvtColor(frame, gray, cv::COLOR_RGB2GRAY);
        
        // Применение размытия для уменьшения шума
        if (detector->params.useGaussianBlur) {
            cv::GaussianBlur(gray, gray, cv::Size(detector->params.blurSize, detector->params.blurSize), 0);
        }
        
        // Применение фонового вычитания
        cv::Mat fgMask;
        detector->bgSubtractor->apply(gray, fgMask);
        
        // Морфологические операции для удаления шума
        cv::Mat kernel = cv::getStructuringElement(cv::MORPH_ELLIPSE, cv::Size(5, 5));
        cv::morphologyEx(fgMask, fgMask, cv::MORPH_CLOSE, kernel);
        cv::morphologyEx(fgMask, fgMask, cv::MORPH_OPEN, kernel);
        
        // Поиск контуров
        std::vector<std::vector<cv::Point>> contours;
        cv::findContours(fgMask, contours, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_SIMPLE);
        
        // Поиск наибольшей области движения
        int maxArea = 0;
        cv::Rect maxRect;
        
        for (const auto& contour : contours) {
            int area = static_cast<int>(cv::contourArea(contour));
            if (area > detector->params.minArea && area > maxArea) {
                maxArea = area;
                maxRect = cv::boundingRect(contour);
            }
        }
        
        // Заполнение результата
        if (maxArea > 0) {
            result->motionDetected = true;
            result->confidence = std::min(1.0f, static_cast<float>(maxArea) / (width * height));
            result->x = maxRect.x;
            result->y = maxRect.y;
            result->width = maxRect.width;
            result->height = maxRect.height;
        } else {
            result->motionDetected = false;
            result->confidence = 0.0f;
            result->x = result->y = result->width = result->height = 0;
        }
        
        detector->initialized = true;
        return true;
        
    } catch (const cv::Exception& e) {
        result->motionDetected = false;
        result->confidence = 0.0f;
        return false;
    }
#else
    // Заглушка без OpenCV
    result->motionDetected = false;
    result->confidence = 0.0f;
    return false;
#endif
}

bool motion_detector_set_params(MotionDetector* detector, const MotionDetectorParams* params) {
    if (!detector || !params) {
        return false;
    }
    
    std::lock_guard<std::mutex> lock(detector->mutex);
    detector->params = *params;
    
    return true;
}

