#include "analytics_error.h"

extern "C" const char* analytics_error_string(AnalyticsErrorCode code) {
    switch (code) {
        case ANALYTICS_OK: return "OK";
        case ANALYTICS_ERR_NULL_PTR: return "Null pointer";
        case ANALYTICS_ERR_INVALID_PARAM: return "Invalid parameter";
        case ANALYTICS_ERR_INVALID_SIZE: return "Invalid size";
        case ANALYTICS_ERR_MODEL_LOAD: return "Model load failed";
        case ANALYTICS_ERR_INFERENCE: return "Inference failed";
        case ANALYTICS_ERR_NOT_INITIALIZED: return "Not initialized";
        case ANALYTICS_ERR_OPENCV: return "OpenCV error";
        case ANALYTICS_ERR_TFLITE: return "TensorFlow Lite error";
        case ANALYTICS_ERR_TESSERACT: return "Tesseract OCR error";
        case ANALYTICS_ERR_UNKNOWN: return "Unknown error";
        default: return "Invalid error code";
    }
}
