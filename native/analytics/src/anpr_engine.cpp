#include "anpr_engine.h"
#include <memory>
#include <mutex>
#include <vector>
#include <string>
#include <cstring>
#include <algorithm>
#include <cctype>

#ifdef ENABLE_OPENCV
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/dnn.hpp>
#endif

#ifdef ENABLE_TESSERACT
#include <tesseract/baseapi.h>
#include <leptonica/allheaders.h>
#endif

struct ANPREngine {
    ANPREngineParams params;
    std::mutex mutex;
    bool ocrInitialized;

#ifdef ENABLE_OPENCV
    cv::dnn::Net plateDetector;  // Модель для детекции номерных знаков
#endif

#ifdef ENABLE_TESSERACT
    tesseract::TessBaseAPI* tesseractAPI;
#endif
};

ANPREngine* anpr_engine_create(const ANPREngineParams* params) {
    auto* engine = new ANPREngine();

    if (params) {
        engine->params = *params;
    } else {
        engine->params.confidenceThreshold = 0.5f;
        engine->params.language = "eng";
    }

    engine->ocrInitialized = false;

#ifdef ENABLE_TESSERACT
    engine->tesseractAPI = nullptr;
#endif

    return engine;
}

void anpr_engine_destroy(ANPREngine* engine) {
    if (engine) {
#ifdef ENABLE_TESSERACT
        if (engine->tesseractAPI) {
            engine->tesseractAPI->End();
            delete engine->tesseractAPI;
            engine->tesseractAPI = nullptr;
        }
#endif
        delete engine;
    }
}

bool anpr_engine_init_ocr(ANPREngine* engine) {
    if (!engine) {
        return false;
    }

    std::lock_guard<std::mutex> lock(engine->mutex);

#ifdef ENABLE_TESSERACT
    if (!engine->tesseractAPI) {
        engine->tesseractAPI = new tesseract::TessBaseAPI();

        // Инициализация Tesseract с указанным языком
        const char* lang = engine->params.language ? engine->params.language : "eng";
        if (engine->tesseractAPI->Init(nullptr, lang, tesseract::OEM_LSTM_ONLY) != 0) {
            // Попытка инициализации без LSTM
            if (engine->tesseractAPI->Init(nullptr, lang) != 0) {
                delete engine->tesseractAPI;
                engine->tesseractAPI = nullptr;
                engine->ocrInitialized = false;
                return false;
            }
        }

        // Настройка параметров для распознавания номерных знаков
        engine->tesseractAPI->SetPageSegMode(tesseract::PSM_SINGLE_BLOCK);
        engine->tesseractAPI->SetVariable("tessedit_char_whitelist", "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");

        engine->ocrInitialized = true;
        return true;
    }
#else
    // Без Tesseract OCR не может работать
    engine->ocrInitialized = false;
    return false;
#endif

    engine->ocrInitialized = true;
    return true;
}

bool anpr_engine_recognize(
    ANPREngine* engine,
    const uint8_t* frameData,
    int width,
    int height,
    ANPRResult* result
) {
    if (!engine || !frameData || !result) {
        return false;
    }

    if (!engine->ocrInitialized) {
        return false;
    }

    std::lock_guard<std::mutex> lock(engine->mutex);

    result->plates = nullptr;
    result->plateCount = 0;

#ifdef ENABLE_OPENCV
    try {
        cv::Mat frame(height, width, CV_8UC3, const_cast<uint8_t*>(frameData));
        cv::Mat gray;
        cv::cvtColor(frame, gray, cv::COLOR_RGB2GRAY);

        // Предобработка для улучшения распознавания
        cv::Mat processed;
        cv::GaussianBlur(gray, processed, cv::Size(5, 5), 0);
        cv::adaptiveThreshold(processed, processed, 255, cv::ADAPTIVE_THRESH_GAUSSIAN_C, cv::THRESH_BINARY, 11, 2);

        // Поиск контуров, которые могут быть номерными знаками
        std::vector<std::vector<cv::Point>> contours;
        cv::findContours(processed, contours, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_SIMPLE);

        std::vector<RecognizedPlate> plates;

        for (const auto& contour : contours) {
            cv::Rect rect = cv::boundingRect(contour);

            // Фильтрация по размеру и соотношению сторон (номерные знаки обычно прямоугольные)
            float aspectRatio = static_cast<float>(rect.width) / rect.height;
            if (aspectRatio > 1.5f && aspectRatio < 5.0f && rect.area() > 1000) {
                // Извлечение области номера
                cv::Mat plateROI = gray(rect);

                // Улучшение качества изображения для OCR
                cv::Mat enhanced;
                cv::resize(plateROI, enhanced, cv::Size(rect.width * 2, rect.height * 2), 0, 0, cv::INTER_CUBIC);
                cv::GaussianBlur(enhanced, enhanced, cv::Size(3, 3), 0);
                cv::adaptiveThreshold(enhanced, enhanced, 255, cv::ADAPTIVE_THRESH_GAUSSIAN_C, cv::THRESH_BINARY, 11, 2);

                RecognizedPlate plate;
                plate.text = nullptr;
                plate.confidence = 0.0f;
                plate.x = rect.x;
                plate.y = rect.y;
                plate.width = rect.width;
                plate.height = rect.height;

#ifdef ENABLE_TESSERACT
                if (engine->tesseractAPI && engine->ocrInitialized) {
                    // Конвертируем OpenCV Mat в формат, понятный Tesseract
                    // Tesseract работает с PIX (Leptonica) или напрямую с данными
                    std::string recognizedText;
                    float confidence = 0.0f;

                    try {
                        // Устанавливаем изображение для распознавания
                        engine->tesseractAPI->SetImage(enhanced.data, enhanced.cols, enhanced.rows, 1, enhanced.step);

                        // Получаем распознанный текст
                        char* text = engine->tesseractAPI->GetUTF8Text();
                        if (text) {
                            recognizedText = text;
                            delete[] text;
                        }

                        // Получаем уверенность распознавания
                        confidence = engine->tesseractAPI->MeanTextConf() / 100.0f;

                        // Фильтруем результаты по порогу уверенности
                        if (confidence >= engine->params.confidenceThreshold && !recognizedText.empty()) {
                            // Удаляем пробелы и непечатаемые символы
                            recognizedText.erase(std::remove_if(recognizedText.begin(), recognizedText.end(),
                                [](char c) { return std::isspace(c) || !std::isprint(c); }), recognizedText.end());

                            if (!recognizedText.empty() && recognizedText.length() >= 3) {
                                plate.text = new char[recognizedText.length() + 1];
                                strcpy(plate.text, recognizedText.c_str());
                                plate.confidence = confidence;
                                plates.push_back(plate);
                            }
                        }
                    } catch (...) {
                        // Ошибка при распознавании, пропускаем этот номер
                    }
                }
#else
                // Без Tesseract OCR не может распознать текст
                // Пропускаем этот номерной знак
                continue;
#endif
            }
        }

        if (plates.size() > 0) {
            result->plateCount = static_cast<int>(plates.size());
            result->plates = new RecognizedPlate[result->plateCount];
            for (size_t i = 0; i < plates.size(); i++) {
                result->plates[i] = plates[i];
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

void anpr_result_release(ANPRResult* result) {
    if (result && result->plates) {
        for (int i = 0; i < result->plateCount; i++) {
            if (result->plates[i].text) {
                delete[] result->plates[i].text;
            }
        }
        delete[] result->plates;
        result->plates = nullptr;
        result->plateCount = 0;
    }
}

