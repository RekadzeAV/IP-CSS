#include <iostream>
#include <vector>
#include <string>
#include <fstream>
#include <chrono>
#include <thread>
#include <atomic>
#include <cstring>

// Внешние переменные из test_main.cpp
extern std::vector<struct TestResult> test_results;

#ifdef ENABLE_FFMPEG
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/avutil.h>
}

#include "../../include/rtsp_client.h"

// Простой JSON парсер для конфигурации (упрощенная версия)
struct CameraConfig {
    std::string name;
    std::string url;
    std::string username;
    std::string password;
    std::string codec;
    std::string resolution;
    int fps;
};

// Глобальные переменные для тестирования
std::atomic<int> frames_received{0};
std::atomic<int> frames_decoded{0};
std::atomic<bool> test_running{false};
std::atomic<bool> connection_success{false};

// Callback для видеокадров (совместим с RTSPFrameCallback)
void video_frame_callback(RTSPFrame* frame, void* userData) {
    if (frame && frame->data && frame->size > 0) {
        frames_received++;

        // Проверка метаданных
        if (frame->width > 0 && frame->height > 0) {
            frames_decoded++;
        }

        // Освобождение кадра
        if (frame->data) {
            delete[] frame->data;
        }
        delete frame;
    }
}

// Callback для статуса
void status_callback(RTSPStatus status, const char* message, void* userData) {
    std::cout << "  Status: " << static_cast<int>(status);
    if (message) {
        std::cout << " - " << message;
    }
    std::cout << std::endl;

    if (status == RTSP_STATUS_CONNECTED || status == RTSP_STATUS_PLAYING) {
        connection_success = true;
    }
}

// Тест подключения к камере
void test_rtsp_connection(const CameraConfig& config) {
    std::cout << "Testing connection to: " << config.name << std::endl;
    std::cout << "  URL: " << config.url << std::endl;

    frames_received = 0;
    frames_decoded = 0;
    connection_success = false;
    test_running = true;

    // Создание RTSP клиента
    RTSPClient* client = rtsp_client_create();
    if (!client) {
        test_results.push_back({"test_rtsp_connection_" + config.name, false, "Failed to create RTSP client"});
        return;
    }

    // Установка callbacks
    rtsp_client_set_frame_callback(client, RTSP_STREAM_VIDEO, video_frame_callback, nullptr);
    rtsp_client_set_status_callback(client, status_callback, nullptr);

    // Подключение
    const char* username = config.username.empty() ? nullptr : config.username.c_str();
    const char* password = config.password.empty() ? nullptr : config.password.c_str();

    bool connected = rtsp_client_connect(client, config.url.c_str(), username, password, 5000);

    if (!connected) {
        std::cout << "  ❌ Connection failed" << std::endl;
        rtsp_client_destroy(client);
        test_results.push_back({"test_rtsp_connection_" + config.name, false, "Connection failed"});
        return;
    }

    std::cout << "  ✅ Connected successfully" << std::endl;

    // Запуск воспроизведения
    bool playing = rtsp_client_play(client);
    if (!playing) {
        std::cout << "  ⚠️  Play failed, but connection succeeded" << std::endl;
    } else {
        std::cout << "  ✅ Playing" << std::endl;
    }

    // Ожидание получения кадров (5 секунд)
    auto start = std::chrono::steady_clock::now();
    while (std::chrono::steady_clock::now() - start < std::chrono::seconds(5)) {
        std::this_thread::sleep_for(std::chrono::milliseconds(100));

        if (frames_received > 0) {
            std::cout << "  ✅ Received " << frames_received << " frames" << std::endl;
            break;
        }
    }

    // Остановка
    rtsp_client_stop(client);
    rtsp_client_disconnect(client);
    rtsp_client_destroy(client);

    test_running = false;

    // Проверка результатов
    if (connection_success && frames_received > 0) {
        test_results.push_back({"test_rtsp_connection_" + config.name, true,
            "Received " + std::to_string(frames_received) + " frames"});
    } else {
        test_results.push_back({"test_rtsp_connection_" + config.name, false,
            "No frames received or connection failed"});
    }
}

// Тест декодирования кадров
void test_frame_decoding(const CameraConfig& config) {
    std::cout << "Testing frame decoding: " << config.name << std::endl;

    frames_received = 0;
    frames_decoded = 0;
    connection_success = false;
    test_running = true;

    RTSPClient* client = rtsp_client_create();
    if (!client) {
        test_results.push_back({"test_frame_decoding_" + config.name, false, "Failed to create RTSP client"});
        return;
    }

    rtsp_client_set_frame_callback(client, RTSP_STREAM_VIDEO, video_frame_callback, nullptr);
    rtsp_client_set_status_callback(client, status_callback, nullptr);

    const char* username = config.username.empty() ? nullptr : config.username.c_str();
    const char* password = config.password.empty() ? nullptr : config.password.c_str();

    bool connected = rtsp_client_connect(client, config.url.c_str(), username, password, 5000);
    if (!connected) {
        rtsp_client_destroy(client);
        test_results.push_back({"test_frame_decoding_" + config.name, false, "Connection failed"});
        return;
    }

    bool playing = rtsp_client_play(client);
    if (!playing) {
        rtsp_client_disconnect(client);
        rtsp_client_destroy(client);
        test_results.push_back({"test_frame_decoding_" + config.name, false, "Play failed"});
        return;
    }

    // Ожидание получения и декодирования кадров (10 секунд)
    auto start = std::chrono::steady_clock::now();
    auto last_frame_time = start;
    int last_frame_count = 0;

    while (std::chrono::steady_clock::now() - start < std::chrono::seconds(10)) {
        std::this_thread::sleep_for(std::chrono::milliseconds(100));

        if (frames_decoded > last_frame_count) {
            last_frame_count = frames_decoded;
            last_frame_time = std::chrono::steady_clock::now();
        }

        // Проверка на застревание (нет новых кадров за 3 секунды)
        if (frames_decoded > 0 &&
            std::chrono::steady_clock::now() - last_frame_time > std::chrono::seconds(3)) {
            std::cout << "  ⚠️  No new frames for 3 seconds" << std::endl;
            break;
        }
    }

    rtsp_client_stop(client);
    rtsp_client_disconnect(client);
    rtsp_client_destroy(client);

    test_running = false;

    // Вычисление FPS
    auto duration = std::chrono::duration_cast<std::chrono::milliseconds>(
        std::chrono::steady_clock::now() - start).count();
    double fps = (duration > 0) ? (frames_decoded * 1000.0 / duration) : 0.0;

    std::cout << "  Frames decoded: " << frames_decoded << std::endl;
    std::cout << "  Average FPS: " << fps << std::endl;

    if (frames_decoded > 0 && fps >= 10.0) {
        test_results.push_back({"test_frame_decoding_" + config.name, true,
            "Decoded " + std::to_string(frames_decoded) + " frames, FPS: " + std::to_string(fps)});
    } else {
        test_results.push_back({"test_frame_decoding_" + config.name, false,
            "Insufficient frames or low FPS"});
    }
}

// Основная функция для интеграционных тестов
void run_integration_tests(const std::vector<CameraConfig>& cameras) {
    std::cout << "=== Integration Tests ===" << std::endl;
    std::cout << "Cameras to test: " << cameras.size() << std::endl;
    std::cout << std::endl;

    for (const auto& camera : cameras) {
        std::cout << "--- Testing Camera: " << camera.name << " ---" << std::endl;

        // Тест подключения
        test_rtsp_connection(camera);
        std::this_thread::sleep_for(std::chrono::seconds(1));

        // Тест декодирования
        test_frame_decoding(camera);
        std::this_thread::sleep_for(std::chrono::seconds(1));

        std::cout << std::endl;
    }
}

#else // ENABLE_FFMPEG not defined

void run_integration_tests(const std::vector<CameraConfig>& cameras) {
    std::cout << "FFmpeg not enabled, integration tests skipped" << std::endl;
    test_results.push_back({"integration_tests", false, "FFmpeg not enabled"});
}

#endif // ENABLE_FFMPEG

// Простой парсер конфигурации (упрощенный, без JSON библиотеки)
std::vector<CameraConfig> load_test_config(const std::string& config_file) {
    std::vector<CameraConfig> cameras;

    // Если файл не существует, создаем пример конфигурации
    std::ifstream file(config_file);
    if (!file.is_open()) {
        std::cout << "Config file not found: " << config_file << std::endl;
        std::cout << "Creating example config..." << std::endl;

        // Создаем пример конфигурации
        CameraConfig example;
        example.name = "Example Camera";
        example.url = "rtsp://example.com:554/stream";
        example.username = "admin";
        example.password = "password";
        example.codec = "H264";
        example.resolution = "1920x1080";
        example.fps = 25;

        cameras.push_back(example);
        return cameras;
    }

    // Простой парсинг (для полной версии использовать JSON библиотеку)
    // Здесь упрощенная версия - просто возвращаем пример
    CameraConfig config;
    config.name = "Test Camera";
    config.url = "rtsp://192.168.1.100:554/stream";
    config.username = "";
    config.password = "";
    config.codec = "H264";
    config.resolution = "1920x1080";
    config.fps = 25;

    cameras.push_back(config);
    return cameras;
}

// Регистрация тестов
void register_integration_tests() {
    // Интеграционные тесты запускаются через run_integration_tests()
    // Регистрация происходит в main()
}
