#include "anpr_engine.h"
#include <memory>
#include <mutex>
#include <vector>
#include <string>
#include <cstring>
#include <regex>

#ifdef ENABLE_OPENCV
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc.hpp>
#include <opencv2/dnn.hpp>
#endif

// Tesseract OCR может быть интегрирован через cinterop или внешнюю библиотеку
// В текущей реализации используется упрощенный подход

struct ANPREngine {
    ANPREngineParams params;
    std::mutex mutex;
    bool ocrInitialized;
    std::string countryCode;  // Код страны для валидации формата номера

#ifdef ENABLE_OPENCV
    cv::dnn::Net plateDetector;  // Модель для детекции номерных знаков
#endif
};

ANPREngine* anpr_engine_create(const ANPREngineParams* params) {
    auto* engine = new ANPREngine();

    if (params) {
        engine->params = *params;
        // Извлекаем код страны из языка (например, "rus" -> "RUS", "eng" -> пустой)
        if (params->language && (strcmp(params->language, "rus") == 0 || strcmp(params->language, "rus+eng") == 0)) {
            engine->countryCode = "RUS";
        } else {
            engine->countryCode = "";  // Не определено
        }
    } else {
        engine->params.confidenceThreshold = 0.5f;
        engine->params.language = "eng";
        engine->countryCode = "";
    }

    engine->ocrInitialized = false;

    return engine;
}

void anpr_engine_destroy(ANPREngine* engine) {
    if (engine) {
        delete engine;
    }
}

bool anpr_engine_init_ocr(ANPREngine* engine) {
    if (!engine) {
        return false;
    }

    std::lock_guard<std::mutex> lock(engine->mutex);

    // TODO: Инициализация Tesseract OCR библиотеки
    // Требуется добавить зависимость tesseract и инициализировать через tesseract::TessBaseAPI
    // Настройка языка и пути к trained data для распознавания номерных знаков

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

                // TODO: Распознавание текста номера через Tesseract OCR
                // Использовать TessBaseAPI::SetImage() и TessBaseAPI::GetUTF8Text()
                // для извлечения текста из предобработанного изображения номера

                std::string plateText = "ABC123";  // Заглушка
                
                // Постобработка текста
                plateText = postprocessPlateText(plateText);
                
                // Валидация формата по стране
                if (!validatePlateFormat(plateText, engine->countryCode)) {
                    // Если формат не соответствует стране, пропускаем этот номер
                    continue;
                }

                RecognizedPlate plate;
                plate.text = new char[plateText.length() + 1];
                strcpy(plate.text, plateText.c_str());
                plate.confidence = 0.8f;
                plate.x = rect.x;
                plate.y = rect.y;
                plate.width = rect.width;
                plate.height = rect.height;

                plates.push_back(plate);
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

// Валидация формата номера по стране
static bool validatePlateFormat(const std::string& plate, const std::string& countryCode) {
    if (countryCode.empty()) {
        return true;  // Если страна не определена, пропускаем валидацию
    }

    if (countryCode == "RUS") {
        // Формат российского номера: А123БВ 777 или А123БВ777
        // Буквы: A, B, E, K, M, H, O, P, C, T, Y, X (латиница, совместимая с кириллицей)
        std::regex rusPattern("^[ABEKMHOPCTYX]\\d{3}[ABEKMHOPCTYX]{2}(\\d{2,3}|[ABEKMHOPCTYX]{2})$");
        return std::regex_match(plate, rusPattern);
    }
    else if (countryCode == "EU" || countryCode == "EUR") {
        // Формат европейского номера: XX12345 (2 буквы, 2-5 цифр)
        std::regex euPattern("^[A-Z]{2}\\d{2,5}[A-Z]{0,2}$");
        return std::regex_match(plate, euPattern);
    }
    else if (countryCode == "USA") {
        // Формат американского номера: варьируется, базовый шаблон
        std::regex usPattern("^[A-Z0-9]{3,7}$");
        return std::regex_match(plate, usPattern);
    }

    return true;  // Для неизвестных стран пропускаем валидацию
}

// Постобработка текста номера
static std::string postprocessPlateText(const std::string& text) {
    std::string result = text;
    
    // Приведение к верхнему регистру
    for (char& c : result) {
        if (c >= 'a' && c <= 'z') {
            c = c - 'a' + 'A';
        }
    }
    
    // Замена похожих символов
    std::string cleaned;
    for (char c : result) {
        switch (c) {
            case '0': case 'O': cleaned += '0'; break;
            case '1': case 'I': case 'l': cleaned += '1'; break;
            case '5': case 'S': cleaned += '5'; break;
            case '8': case 'B': cleaned += '8'; break;
            default: cleaned += c; break;
        }
    }
    
    return cleaned;
}

