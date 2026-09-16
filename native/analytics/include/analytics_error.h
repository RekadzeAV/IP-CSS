#ifndef ANALYTICS_ERROR_H
#define ANALYTICS_ERROR_H

#ifdef __cplusplus
extern "C" {
#endif

/** Коды ошибок модуля аналитики */
typedef enum {
    ANALYTICS_OK = 0,
    ANALYTICS_ERR_NULL_PTR,
    ANALYTICS_ERR_INVALID_PARAM,
    ANALYTICS_ERR_INVALID_SIZE,
    ANALYTICS_ERR_MODEL_LOAD,
    ANALYTICS_ERR_INFERENCE,
    ANALYTICS_ERR_NOT_INITIALIZED,
    ANALYTICS_ERR_OPENCV,
    ANALYTICS_ERR_TFLITE,
    ANALYTICS_ERR_TESSERACT,
    ANALYTICS_ERR_UNKNOWN
} AnalyticsErrorCode;

/** Возвращает строковое описание кода ошибки */
const char* analytics_error_string(AnalyticsErrorCode code);

#ifdef __cplusplus
}
#endif

#endif /* ANALYTICS_ERROR_H */
