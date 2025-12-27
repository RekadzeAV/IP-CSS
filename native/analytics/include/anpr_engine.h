#ifndef ANPR_ENGINE_H
#define ANPR_ENGINE_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>
#include <stdbool.h>

// Распознанный номер
typedef struct {
    char* text;             // Распознанный текст номера
    float confidence;       // Уверенность распознавания
    int x, y, width, height; // Область номера
} RecognizedPlate;

// Результат распознавания
typedef struct {
    RecognizedPlate* plates;
    int plateCount;
} ANPRResult;

// Параметры распознавания
typedef struct {
    float confidenceThreshold;
    const char* language;    // Язык для OCR (например, "eng")
} ANPREngineParams;

// Структура движка (opaque)
typedef struct ANPREngine ANPREngine;

// Создание движка ANPR
ANPREngine* anpr_engine_create(const ANPREngineParams* params);

// Уничтожение движка
void anpr_engine_destroy(ANPREngine* engine);

// Инициализация OCR
bool anpr_engine_init_ocr(ANPREngine* engine);

// Распознавание номеров в кадре
bool anpr_engine_recognize(
    ANPREngine* engine,
    const uint8_t* frameData,
    int width,
    int height,
    ANPRResult* result
);

// Освобождение результата
void anpr_result_release(ANPRResult* result);

#ifdef __cplusplus
}
#endif

#endif // ANPR_ENGINE_H


