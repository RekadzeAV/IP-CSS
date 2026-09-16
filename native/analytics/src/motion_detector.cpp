#include "motion_detector.h"
#include <algorithm>
#include <cstddef>
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
    // Валидация входных параметров
    if (width <= 0 || height <= 0 || width > 10000 || height > 10000) {
        return nullptr;
    }

    auto* detector = new MotionDetector();
    detector->width = width;
    detector->height = height;

    if (params) {
        detector->params = *params;
        // Валидация и нормализация параметров
        detector->params.threshold = std::max(0.0f, std::min(1.0f, detector->params.threshold));
        detector->params.minArea = std::max(1, detector->params.minArea);
        detector->params.blurSize = std::max(1, std::min(21, detector->params.blurSize));
    } else {
        // Параметры по умолчанию
        detector->params.threshold = 0.5f;
        detector->params.minArea = 500;
        detector->params.useGaussianBlur = true;
        detector->params.blurSize = 5;
    }

#ifdef ENABLE_OPENCV
    try {
        // Использование MOG2 для детекции движения
        // Параметры: history=500 (количество кадров для обучения), varThreshold=16.0, detectShadows=false
        detector->bgSubtractor = cv::createBackgroundSubtractorMOG2(500, 16.0, false);
        detector->initialized = false;
    } catch (const cv::Exception& e) {
        delete detector;
        return nullptr;
    }
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

    // Валидация размеров
    if (width <= 0 || height <= 0 || width > 10000 || height > 10000) {
        result->motionDetected = false;
        result->confidence = 0.0f;
        result->x = result->y = result->width = result->height = 0;
        return false;
    }

    // Проверка размера данных (RGB24: width * height * 3)
    size_t expectedSize = static_cast<size_t>(width) * height * 3;
    if (expectedSize == 0) {
        result->motionDetected = false;
        result->confidence = 0.0f;
        result->x = result->y = result->width = result->height = 0;
        return false;
    }

    // Если размеры изменились, обновляем детектор
    if (width != detector->width || height != detector->height) {
        // Можно либо обновить размеры, либо вернуть ошибку
        // Для простоты обновляем размеры
        detector->width = width;
        detector->height = height;
#ifdef ENABLE_OPENCV
        // Пересоздаем background subtractor для нового размера
        detector->bgSubtractor = cv::createBackgroundSubtractorMOG2(500, 16.0, false);
        detector->initialized = false;
#endif
    }

#ifdef ENABLE_OPENCV
    std::lock_guard<std::mutex> lock(detector->mutex);

    try {
        // Создание OpenCV Mat из RGB данных
        cv::Mat frame(height, width, CV_8UC3, const_cast<uint8_t*>(frameData));
        cv::Mat gray;
        cv::cvtColor(frame, gray, cv::COLOR_RGB2GRAY);

        // Применение размытия для уменьшения шума
        if (detector->params.useGaussianBlur && detector->params.blurSize > 0) {
            int blurSize = detector->params.blurSize;
            // Убеждаемся, что размер размытия нечетный
            if (blurSize % 2 == 0) {
                blurSize++;
            }
            cv::GaussianBlur(gray, gray, cv::Size(blurSize, blurSize), 0);
        }

        // Применение фонового вычитания
        cv::Mat fgMask;
        detector->bgSubtractor->apply(gray, fgMask);

        // Применение пороговой обработки с учетом threshold параметра
        // Преобразуем threshold (0.0-1.0) в значение для threshold операции (0-255)
        int thresholdValue = static_cast<int>(detector->params.threshold * 255.0f);
        thresholdValue = std::max(1, std::min(255, thresholdValue)); // Ограничиваем диапазон

        cv::threshold(fgMask, fgMask, thresholdValue, 255, cv::THRESH_BINARY);

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
        int totalMotionArea = 0;

        for (const auto& contour : contours) {
            int area = static_cast<int>(cv::contourArea(contour));
            if (area > detector->params.minArea) {
                totalMotionArea += area;
                if (area > maxArea) {
                    maxArea = area;
                    maxRect = cv::boundingRect(contour);
                }
            }
        }

        // Вычисление общей площади кадра для нормализации confidence
        int frameArea = width * height;

        // Заполнение результата с учетом threshold
        if (maxArea > 0 && frameArea > 0) {
            // Вычисляем confidence как отношение площади движения к площади кадра
            float rawConfidence = static_cast<float>(totalMotionArea) / static_cast<float>(frameArea);

            // Применяем threshold для фильтрации слабых сигналов
            // Если confidence ниже threshold, считаем что движения нет
            if (rawConfidence >= detector->params.threshold) {
                result->motionDetected = true;
                // Нормализуем confidence относительно threshold
                // Если threshold = 0.5, то confidence 0.5 -> 0.0, confidence 1.0 -> 1.0
                float normalizedConfidence = (rawConfidence - detector->params.threshold) / (1.0f - detector->params.threshold);
                result->confidence = std::min(1.0f, std::max(0.0f, normalizedConfidence));
                result->x = maxRect.x;
                result->y = maxRect.y;
                result->width = maxRect.width;
                result->height = maxRect.height;
            } else {
                // Движение обнаружено, но confidence слишком низкий
                result->motionDetected = false;
                result->confidence = rawConfidence;
                result->x = result->y = result->width = result->height = 0;
            }
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
        result->x = result->y = result->width = result->height = 0;
        return false;
    } catch (const std::exception& e) {
        result->motionDetected = false;
        result->confidence = 0.0f;
        result->x = result->y = result->width = result->height = 0;
        return false;
    }
#else
    // Заглушка без OpenCV
    result->motionDetected = false;
    result->confidence = 0.0f;
    result->x = result->y = result->width = result->height = 0;
    return false;
#endif
}

bool motion_detector_set_params(MotionDetector* detector, const MotionDetectorParams* params) {
    if (!detector || !params) {
        return false;
    }

    // Валидация параметров
    MotionDetectorParams validatedParams = *params;
    validatedParams.threshold = std::max(0.0f, std::min(1.0f, validatedParams.threshold));
    validatedParams.minArea = std::max(1, validatedParams.minArea);
    validatedParams.blurSize = std::max(1, std::min(21, validatedParams.blurSize));

    std::lock_guard<std::mutex> lock(detector->mutex);
    detector->params = validatedParams;

    return true;
}



