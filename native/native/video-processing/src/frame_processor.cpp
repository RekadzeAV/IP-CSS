#include "frame_processor.h"
#include <memory>
#include <mutex>
#include <cstring>

#ifdef ENABLE_OPENCV
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc.hpp>
#endif

struct FrameProcessor {
    std::mutex mutex;
    
#ifdef ENABLE_OPENCV
    bool opencvAvailable;
#endif
};

FrameProcessor* frame_processor_create() {
    auto* processor = new FrameProcessor();
    
#ifdef ENABLE_OPENCV
    processor->opencvAvailable = true;
#endif
    
    return processor;
}

void frame_processor_destroy(FrameProcessor* processor) {
    if (processor) {
        delete processor;
    }
}

bool frame_processor_process(
    FrameProcessor* processor,
    const uint8_t* inputData,
    int inputWidth,
    int inputHeight,
    int inputFormat,
    const ProcessingParams* params,
    ProcessedFrame* output
) {
    if (!processor || !inputData || !params || !output) {
        return false;
    }
    
#ifdef ENABLE_OPENCV
    std::lock_guard<std::mutex> lock(processor->mutex);
    
    try {
        // Создание OpenCV Mat из входных данных
        cv::Mat inputMat;
        
        if (inputFormat == 1) {  // RGB24
            inputMat = cv::Mat(inputHeight, inputWidth, CV_8UC3, const_cast<uint8_t*>(inputData));
        } else if (inputFormat == 0) {  // YUV420
            // Конвертация YUV420 -> RGB
            cv::Mat yuvMat(inputHeight * 3 / 2, inputWidth, CV_8UC1, const_cast<uint8_t*>(inputData));
            cv::cvtColor(yuvMat, inputMat, cv::COLOR_YUV2RGB_I420);
        } else if (inputFormat == 2) {  // Grayscale
            inputMat = cv::Mat(inputHeight, inputWidth, CV_8UC1, const_cast<uint8_t*>(inputData));
        } else {
            return false;
        }
        
        cv::Mat resultMat;
        
        // Применение операции
        switch (params->operation) {
            case FRAME_OP_RESIZE: {
                cv::Size newSize(params->params.resize.width, params->params.resize.height);
                cv::resize(inputMat, resultMat, newSize, 0, 0, cv::INTER_LINEAR);
                break;
            }
            
            case FRAME_OP_ROTATE: {
                cv::Point2f center(inputWidth / 2.0f, inputHeight / 2.0f);
                cv::Mat rotationMatrix = cv::getRotationMatrix2D(center, params->params.rotate.angle, 1.0);
                cv::warpAffine(inputMat, resultMat, rotationMatrix, inputMat.size());
                break;
            }
            
            case FRAME_OP_FLIP_HORIZONTAL: {
                cv::flip(inputMat, resultMat, 1);
                break;
            }
            
            case FRAME_OP_FLIP_VERTICAL: {
                cv::flip(inputMat, resultMat, 0);
                break;
            }
            
            case FRAME_OP_CROP: {
                cv::Rect roi(
                    params->params.crop.x,
                    params->params.crop.y,
                    params->params.crop.width,
                    params->params.crop.height
                );
                resultMat = inputMat(roi).clone();
                break;
            }
            
            case FRAME_OP_BRIGHTNESS: {
                inputMat.convertTo(resultMat, -1, 1.0, params->params.brightness.value * 255);
                break;
            }
            
            case FRAME_OP_CONTRAST: {
                double alpha = 1.0 + params->params.contrast.value;
                inputMat.convertTo(resultMat, -1, alpha, 0);
                break;
            }
            
            case FRAME_OP_SATURATION: {
                cv::Mat hsv;
                cv::cvtColor(inputMat, hsv, cv::COLOR_RGB2HSV);
                std::vector<cv::Mat> channels;
                cv::split(hsv, channels);
                channels[1] *= (1.0 + params->params.saturation.value);
                cv::merge(channels, hsv);
                cv::cvtColor(hsv, resultMat, cv::COLOR_HSV2RGB);
                break;
            }
            
            case FRAME_OP_GRAYSCALE: {
                if (inputMat.channels() == 3) {
                    cv::cvtColor(inputMat, resultMat, cv::COLOR_RGB2GRAY);
                } else {
                    resultMat = inputMat.clone();
                }
                break;
            }
            
            case FRAME_OP_BLUR: {
                cv::Size kernelSize(
                    params->params.blur.radius * 2 + 1,
                    params->params.blur.radius * 2 + 1
                );
                cv::GaussianBlur(inputMat, resultMat, kernelSize, 0);
                break;
            }
            
            case FRAME_OP_SHARPEN: {
                cv::Mat kernel = (cv::Mat_<float>(3, 3) <<
                    0, -1, 0,
                    -1, 5, -1,
                    0, -1, 0);
                cv::filter2D(inputMat, resultMat, -1, kernel);
                break;
            }
            
            default:
                return false;
        }
        
        // Копирование результата в выходную структуру
        output->width = resultMat.cols;
        output->height = resultMat.rows;
        output->format = (resultMat.channels() == 1) ? 2 : 1;  // Grayscale или RGB
        
        size_t dataSize = resultMat.total() * resultMat.elemSize();
        output->dataSize = dataSize;
        output->data = new uint8_t[dataSize];
        
        memcpy(output->data, resultMat.data, dataSize);
        
        return true;
        
    } catch (const cv::Exception& e) {
        return false;
    }
#else
    // Заглушка без OpenCV
    return false;
#endif
}

void processed_frame_release(ProcessedFrame* frame) {
    if (frame && frame->data) {
        delete[] frame->data;
        frame->data = nullptr;
    }
}



