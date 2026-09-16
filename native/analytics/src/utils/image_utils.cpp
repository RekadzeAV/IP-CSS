#include "utils/image_utils.h"
#include <algorithm>
#include <cstring>

#ifdef ENABLE_OPENCV
#include <opencv2/opencv.hpp>
#include <opencv2/imgproc.hpp>
#endif

void image_utils_rgb_to_bgr(uint8_t* data, int width, int height) {
    if (!data || width <= 0 || height <= 0) return;
    const size_t n = static_cast<size_t>(width * height * 3);
    for (size_t i = 0; i < n; i += 3) {
        std::swap(data[i], data[i + 2]);
    }
}

void image_utils_bgr_to_rgb(uint8_t* data, int width, int height) {
    image_utils_rgb_to_bgr(data, width, height);
}

bool image_utils_resize(
    const uint8_t* in_data, int in_width, int in_height, int in_channels,
    uint8_t* out_data, int out_width, int out_height
) {
    if (!in_data || !out_data || in_width <= 0 || in_height <= 0 || out_width <= 0 || out_height <= 0) {
        return false;
    }
    if (in_channels != 1 && in_channels != 3) return false;

#ifdef ENABLE_OPENCV
    cv::Mat src(in_height, in_width, in_channels == 3 ? CV_8UC3 : CV_8UC1, (void*)in_data);
    cv::Mat dst;
    cv::resize(src, dst, cv::Size(out_width, out_height), 0, 0, cv::INTER_LINEAR);
    if (dst.isContinuous() && dst.total() * dst.elemSize() <= static_cast<size_t>(out_width * out_height * in_channels)) {
        std::memcpy(out_data, dst.data, dst.total() * dst.elemSize());
        return true;
    }
    return false;
#else
    (void)out_data;
    (void)out_width;
    (void)out_height;
    (void)in_channels;
    return false;
#endif
}

bool image_utils_normalize(
    const uint8_t* in_data, int width, int height, int channels,
    float* out_data, NormRange range
) {
    if (!in_data || !out_data || width <= 0 || height <= 0 || channels != 1 && channels != 3) {
        return false;
    }
    const float scale = (range == NORM_0_1) ? 1.0f / 255.0f : 2.0f / 255.0f;
    const float offset = (range == NORM_0_1) ? 0.0f : -1.0f;
    const size_t n = static_cast<size_t>(width * height * channels);
    for (size_t i = 0; i < n; ++i) {
        out_data[i] = in_data[i] * scale + offset;
    }
    return true;
}

bool image_utils_preprocess_yolo(
    const uint8_t* in_data, int in_width, int in_height,
    float* blob, int target_size, bool bgr_order
) {
    if (!in_data || !blob || in_width <= 0 || in_height <= 0 || target_size <= 0) {
        return false;
    }

#ifdef ENABLE_OPENCV
    cv::Mat src(in_height, in_width, CV_8UC3, (void*)in_data);
    cv::Mat resized;
    cv::resize(src, resized, cv::Size(target_size, target_size), 0, 0, cv::INTER_LINEAR);
    if (bgr_order) {
        // OpenCV Mat is BGR
        for (int y = 0; y < target_size; ++y) {
            for (int x = 0; x < target_size; ++x) {
                const cv::Vec3b& p = resized.at<cv::Vec3b>(y, x);
                const int base = (y * target_size + x) * 3;
                blob[base + 0] = p[0] / 255.0f;
                blob[base + 1] = p[1] / 255.0f;
                blob[base + 2] = p[2] / 255.0f;
            }
        }
    } else {
        for (int y = 0; y < target_size; ++y) {
            for (int x = 0; x < target_size; ++x) {
                const cv::Vec3b& p = resized.at<cv::Vec3b>(y, x);
                const int base = (y * target_size + x) * 3;
                blob[base + 0] = p[2] / 255.0f;
                blob[base + 1] = p[1] / 255.0f;
                blob[base + 2] = p[0] / 255.0f;
            }
        }
    }
    return true;
#else
    (void)blob;
    (void)target_size;
    (void)bgr_order;
    return false;
#endif
}
