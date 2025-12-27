#include "object_detector.h"
#include <memory>
#include <mutex>
#include <vector>
#include <string>
#include <algorithm>
#include <cmath>

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

        // Полная реализация парсинга YOLO выходов
        std::vector<DetectedObject> detectedObjects;

        // Размер входного изображения для нормализации
        const float inputWidth = 416.0f;
        const float inputHeight = 416.0f;
        const float scaleX = static_cast<float>(width) / inputWidth;
        const float scaleY = static_cast<float>(height) / inputHeight;

        // Обработка каждого выходного слоя (YOLO обычно имеет 3 слоя)
        for (size_t i = 0; i < outputs.size(); i++) {
            const cv::Mat& output = outputs[i];

            // YOLO выход: [batch, anchors, grid_h, grid_w, 5 + num_classes]
            // Или в плоском виде: [num_detections, 85] для COCO (80 классов + 5)
            const int numDetections = output.rows;
            const int numValues = output.cols;

            // Проверяем формат выхода
            if (numValues < 5) continue; // Минимум: x, y, w, h, confidence

            for (int j = 0; j < numDetections; j++) {
                const float* data = output.ptr<float>(j);

                // Извлекаем координаты центра и размеры (нормализованные 0-1)
                float centerX = data[0];
                float centerY = data[1];
                float boxWidth = data[2];
                float boxHeight = data[3];
                float confidence = data[4];

                // Пропускаем если confidence ниже порога
                if (confidence < detector->params.confidenceThreshold) {
                    continue;
                }

                // Находим класс с максимальной вероятностью
                int bestClass = 0;
                float bestClassProb = 0.0f;

                if (numValues > 5) {
                    // Ищем максимальную вероятность класса
                    for (int k = 5; k < numValues; k++) {
                        float classProb = data[k];
                        if (classProb > bestClassProb) {
                            bestClassProb = classProb;
                            bestClass = k - 5;
                        }
                    }

                    // Финальная уверенность = confidence * class_probability
                    confidence = confidence * bestClassProb;

                    // Еще раз проверяем порог с учетом класса
                    if (confidence < detector->params.confidenceThreshold) {
                        continue;
                    }
                }

                // Преобразуем нормализованные координаты в абсолютные пиксели
                int x = static_cast<int>((centerX - boxWidth / 2.0f) * inputWidth * scaleX);
                int y = static_cast<int>((centerY - boxHeight / 2.0f) * inputHeight * scaleY);
                int w = static_cast<int>(boxWidth * inputWidth * scaleX);
                int h = static_cast<int>(boxHeight * inputHeight * scaleY);

                // Ограничиваем координаты границами изображения
                x = std::max(0, std::min(x, width - 1));
                y = std::max(0, std::min(y, height - 1));
                w = std::max(1, std::min(w, width - x));
                h = std::max(1, std::min(h, height - y));

                // Определяем тип объекта на основе класса
                ObjectType objectType = OBJECT_TYPE_UNKNOWN;
                if (numValues > 5) {
                    // COCO dataset mapping: 0=person, 2=car, 3=motorcycle, 6=bus, 7=truck, etc.
                    if (bestClass == 0) {
                        objectType = OBJECT_TYPE_PERSON;
                    } else if (bestClass == 2 || bestClass == 5 || bestClass == 7) {
                        objectType = OBJECT_TYPE_VEHICLE;
                    } else if (bestClass == 3) {
                        objectType = OBJECT_TYPE_MOTORCYCLE;
                    } else if (bestClass == 1) {
                        objectType = OBJECT_TYPE_BICYCLE;
                    }
                }

                DetectedObject obj;
                obj.type = objectType;
                obj.confidence = confidence;
                obj.x = x;
                obj.y = y;
                obj.width = w;
                obj.height = h;

                detectedObjects.push_back(obj);
            }
        }

        // Применяем Non-Maximum Suppression (NMS) для удаления дубликатов
        if (detectedObjects.size() > 1) {
            std::vector<int> indices;
            std::vector<float> scores;
            std::vector<cv::Rect> boxes;

            for (const auto& obj : detectedObjects) {
                scores.push_back(obj.confidence);
                boxes.push_back(cv::Rect(obj.x, obj.y, obj.width, obj.height));
            }

            // Используем OpenCV NMS
            cv::dnn::NMSBoxes(boxes, scores, detector->params.confidenceThreshold, 0.4f, indices);

            // Оставляем только объекты, прошедшие NMS
            std::vector<DetectedObject> filteredObjects;
            for (int idx : indices) {
                filteredObjects.push_back(detectedObjects[idx]);
            }

            detectedObjects = std::move(filteredObjects);
        }

        // Сортируем по уверенности (от большей к меньшей)
        std::sort(detectedObjects.begin(), detectedObjects.end(),
            [](const DetectedObject& a, const DetectedObject& b) {
                return a.confidence > b.confidence;
            });

        // Ограничиваем количество объектов
        if (detectedObjects.size() > static_cast<size_t>(detector->params.maxObjects)) {
            detectedObjects.resize(detector->params.maxObjects);
        }

        // Копируем результаты
        if (detectedObjects.size() > 0) {
            result->objectCount = static_cast<int>(detectedObjects.size());
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

