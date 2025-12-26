#include "object_detector.h"
#include <memory>
#include <mutex>
#include <vector>
#include <string>

#ifdef ENABLE_OPENCV
#include <opencv2/opencv.hpp>
#include <opencv2/dnn.hpp>
#endif

#ifdef ENABLE_TENSORFLOW
#include "tensorflow/lite/interpreter.h"
#include "tensorflow/lite/model.h"
#include "tensorflow/lite/kernels/register.h"
#endif

struct ObjectDetector {
    ObjectDetectorParams params;
    std::mutex mutex;
    bool modelLoaded;
    std::string modelPath;

#ifdef ENABLE_OPENCV
    cv::dnn::Net dnnNet;
#endif

#ifdef ENABLE_TENSORFLOW
    std::unique_ptr<tflite::FlatBufferModel> model;
    std::unique_ptr<tflite::Interpreter> interpreter;
#endif
};

ObjectDetector* object_detector_create(const ObjectDetectorParams* params) {
    auto* detector = new ObjectDetector();

    if (params) {
        detector->params = *params;
    } else {
        detector->params.confidenceThreshold = 0.5f;
        detector->params.maxObjects = 10;
        detector->params.useGPU = false;
    }

    detector->modelLoaded = false;

    return detector;
}

void object_detector_destroy(ObjectDetector* detector) {
    if (detector) {
        delete detector;
    }
}

bool object_detector_load_model(ObjectDetector* detector, const char* modelPath) {
    if (!detector || !modelPath) {
        return false;
    }

    std::lock_guard<std::mutex> lock(detector->mutex);

#ifdef ENABLE_OPENCV
    try {
        // Попытка загрузить модель через OpenCV DNN
        detector->dnnNet = cv::dnn::readNetFromONNX(modelPath);
        if (detector->dnnNet.empty()) {
            // Попытка загрузить как TensorFlow
            detector->dnnNet = cv::dnn::readNetFromTensorflow(modelPath);
        }

        if (!detector->dnnNet.empty()) {
            if (detector->params.useGPU) {
                detector->dnnNet.setPreferableBackend(cv::dnn::DNN_BACKEND_CUDA);
                detector->dnnNet.setPreferableTarget(cv::dnn::DNN_TARGET_CUDA);
            } else {
                detector->dnnNet.setPreferableBackend(cv::dnn::DNN_BACKEND_OPENCV);
                detector->dnnNet.setPreferableTarget(cv::dnn::DNN_TARGET_CPU);
            }

            detector->modelPath = modelPath;
            detector->modelLoaded = true;
            return true;
        }
    } catch (const cv::Exception& e) {
        // Ошибка загрузки модели
    }
#endif

#ifdef ENABLE_TENSORFLOW
    // Попытка загрузить через TensorFlow Lite
    detector->model = tflite::FlatBufferModel::BuildFromFile(modelPath);
    if (detector->model) {
        tflite::ops::builtin::BuiltinOpResolver resolver;
        tflite::InterpreterBuilder builder(*(detector->model), resolver);

        if (builder(&detector->interpreter) == kTfLiteOk && detector->interpreter) {
            if (detector->interpreter->AllocateTensors() == kTfLiteOk) {
                detector->modelPath = modelPath;
                detector->modelLoaded = true;
                return true;
            }
        }
    }
#endif

    return false;
}

bool object_detector_detect(
    ObjectDetector* detector,
    const uint8_t* frameData,
    int width,
    int height,
    DetectionResult* result
) {
    if (!detector || !frameData || !result) {
        return false;
    }

    if (!detector->modelLoaded) {
        return false;
    }

    std::lock_guard<std::mutex> lock(detector->mutex);

    result->objects = nullptr;
    result->objectCount = 0;

#ifdef ENABLE_OPENCV
    try {
        cv::Mat frame(height, width, CV_8UC3, const_cast<uint8_t*>(frameData));

        // Подготовка входного блоба для DNN
        cv::Mat blob;
        cv::dnn::blobFromImage(frame, blob, 1.0/255.0, cv::Size(416, 416), cv::Scalar(0, 0, 0), true, false);

        detector->dnnNet.setInput(blob);

        // Получение выходных слоев (обычно для YOLO это output или yolo_82, yolo_94, yolo_106)
        std::vector<cv::String> outNames = detector->dnnNet.getUnconnectedOutLayersNames();
        std::vector<cv::Mat> outputs;
        detector->dnnNet.forward(outputs, outNames);

        // Парсинг результатов (упрощенная версия для YOLO)
        std::vector<DetectedObject> detectedObjects;

        // TODO: Полная реализация парсинга YOLO выходов
        // Здесь должна быть обработка outputs для извлечения bounding boxes

        // Временная заглушка
        if (detectedObjects.size() > 0) {
            result->objectCount = std::min(static_cast<int>(detectedObjects.size()), detector->params.maxObjects);
            result->objects = new DetectedObject[result->objectCount];
            for (int i = 0; i < result->objectCount; i++) {
                result->objects[i] = detectedObjects[i];
            }
        }

        return true;

    } catch (const cv::Exception& e) {
        return false;
    }
#else
    // Заглушка без OpenCV/TensorFlow
    return false;
#endif
}

void detection_result_release(DetectionResult* result) {
    if (result && result->objects) {
        delete[] result->objects;
        result->objects = nullptr;
        result->objectCount = 0;
    }
}

