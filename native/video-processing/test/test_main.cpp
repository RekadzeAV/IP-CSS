#include <iostream>
#include <vector>
#include <string>
#include <cstring>

#ifdef ENABLE_FFMPEG
extern "C" {
#include <libavcodec/avcodec.h>
#include <libavutil/avutil.h>
}
#endif

// Простой тестовый фреймворк (если GTest не доступен)
struct TestResult {
    std::string name;
    bool passed;
    std::string message;
};

std::vector<TestResult> test_results;

#define TEST(name) \
    void test_##name(); \
    struct TestReg_##name { \
        TestReg_##name() { \
            test_registry.push_back({#name, test_##name}); \
        } \
    } test_reg_##name; \
    void test_##name()

#define ASSERT_TRUE(condition) \
    do { \
        if (!(condition)) { \
            test_results.push_back({__FUNCTION__, false, "ASSERT_TRUE failed: " #condition}); \
            return; \
        } \
    } while(0)

#define ASSERT_FALSE(condition) \
    do { \
        if (condition) { \
            test_results.push_back({__FUNCTION__, false, "ASSERT_FALSE failed: " #condition}); \
            return; \
        } \
    } while(0)

#define ASSERT_EQ(expected, actual) \
    do { \
        if ((expected) != (actual)) { \
            test_results.push_back({__FUNCTION__, false, \
                "ASSERT_EQ failed: expected " + std::to_string(expected) + \
                ", got " + std::to_string(actual)}); \
            return; \
        } \
    } while(0)

#define ASSERT_NE(expected, actual) \
    do { \
        if ((expected) == (actual)) { \
            test_results.push_back({__FUNCTION__, false, \
                "ASSERT_NE failed: values are equal"}); \
            return; \
        } \
    } while(0)

struct TestFunction {
    std::string name;
    void (*func)();
};

std::vector<TestFunction> test_registry;

// Объявления тестов
extern void test_video_decoder_h264_init();
extern void test_video_decoder_h264_decode_idr();
extern void test_video_decoder_h265_init();
extern void test_rtp_fragmentation_fu_a();
extern void test_rtp_fragmentation_stap_a();

// Аудио тесты
extern void test_audio_decoder_aac_init();
extern void test_audio_decoder_aac_decode();
extern void test_audio_decoder_pcmu_init();
extern void test_audio_decoder_pcmu_decode();
extern void test_audio_decoder_pcma_init();
extern void test_audio_decoder_pcma_decode();
extern void test_audio_decoder_sample_rates();
extern void test_audio_decoder_channels();

// Интеграционные тесты
struct CameraConfig {
    std::string name;
    std::string url;
    std::string username;
    std::string password;
    std::string codec;
    std::string resolution;
    int fps;
};

extern void run_integration_tests(const std::vector<CameraConfig>& cameras);
extern std::vector<CameraConfig> load_test_config(const std::string& config_file);

int main(int argc, char* argv[]) {
    std::cout << "=== Video Processing Library Tests ===" << std::endl;
    std::cout << std::endl;

#ifdef ENABLE_FFMPEG
    std::cout << "FFmpeg enabled" << std::endl;
    std::cout << "FFmpeg version: " << av_version_info() << std::endl;
#else
    std::cout << "FFmpeg disabled" << std::endl;
#endif

    std::cout << std::endl;

    // Регистрация unit тестов
    test_registry.push_back({"video_decoder_h264_init", test_video_decoder_h264_init});
    test_registry.push_back({"video_decoder_h264_decode_idr", test_video_decoder_h264_decode_idr});
    test_registry.push_back({"video_decoder_h265_init", test_video_decoder_h265_init});
    test_registry.push_back({"rtp_fragmentation_fu_a", test_rtp_fragmentation_fu_a});
    test_registry.push_back({"rtp_fragmentation_stap_a", test_rtp_fragmentation_stap_a});

    // Аудио тесты
    test_registry.push_back({"audio_decoder_aac_init", test_audio_decoder_aac_init});
    test_registry.push_back({"audio_decoder_aac_decode", test_audio_decoder_aac_decode});
    test_registry.push_back({"audio_decoder_pcmu_init", test_audio_decoder_pcmu_init});
    test_registry.push_back({"audio_decoder_pcmu_decode", test_audio_decoder_pcmu_decode});
    test_registry.push_back({"audio_decoder_pcma_init", test_audio_decoder_pcma_init});
    test_registry.push_back({"audio_decoder_pcma_decode", test_audio_decoder_pcma_decode});
    test_registry.push_back({"audio_decoder_sample_rates", test_audio_decoder_sample_rates});
    test_registry.push_back({"audio_decoder_channels", test_audio_decoder_channels});

    // Проверка аргументов командной строки для интеграционных тестов
    bool run_integration = false;
    std::string config_file = "test_config.json";

    for (int i = 1; i < argc; i++) {
        if (std::string(argv[i]) == "--integration" || std::string(argv[i]) == "-i") {
            run_integration = true;
        } else if (std::string(argv[i]) == "--config" || std::string(argv[i]) == "-c") {
            if (i + 1 < argc) {
                config_file = argv[i + 1];
                i++;
            }
        }
    }

    // Запуск тестов
    int passed = 0;
    int failed = 0;

    for (const auto& test : test_registry) {
        std::cout << "Running test: " << test.name << "..." << std::flush;

        // Сброс результатов для этого теста
        test_results.clear();

        // Запуск теста
        try {
            test.func();

            // Если тест не добавил результатов, считаем его успешным
            if (test_results.empty()) {
                test_results.push_back({test.name, true, ""});
            }
        } catch (const std::exception& e) {
            test_results.push_back({test.name, false, "Exception: " + std::string(e.what())});
        } catch (...) {
            test_results.push_back({test.name, false, "Unknown exception"});
        }

        // Проверка результатов
        bool test_passed = true;
        for (const auto& result : test_results) {
            if (!result.passed) {
                test_passed = false;
                break;
            }
        }

        if (test_passed) {
            std::cout << " PASSED" << std::endl;
            passed++;
        } else {
            std::cout << " FAILED" << std::endl;
            failed++;
            for (const auto& result : test_results) {
                if (!result.passed) {
                    std::cout << "  " << result.message << std::endl;
                }
            }
        }
    }

    // Итоги unit тестов
    std::cout << std::endl;
    std::cout << "=== Unit Test Results ===" << std::endl;
    std::cout << "Passed: " << passed << std::endl;
    std::cout << "Failed: " << failed << std::endl;
    std::cout << "Total: " << (passed + failed) << std::endl;

    // Интеграционные тесты (если запрошены)
    if (run_integration) {
        std::cout << std::endl;
        std::cout << "=== Running Integration Tests ===" << std::endl;
        std::cout << "Config file: " << config_file << std::endl;
        std::cout << std::endl;

        auto cameras = load_test_config(config_file);
        if (cameras.empty()) {
            std::cout << "No cameras configured. Create test_config.json or use --config option." << std::endl;
        } else {
            run_integration_tests(cameras);
        }

        std::cout << std::endl;
        std::cout << "=== Integration Test Results ===" << std::endl;
        int integration_passed = 0;
        int integration_failed = 0;

        for (const auto& result : test_results) {
            if (result.name.find("integration") != std::string::npos ||
                result.name.find("rtsp_connection") != std::string::npos ||
                result.name.find("frame_decoding") != std::string::npos) {
                if (result.passed) {
                    integration_passed++;
                } else {
                    integration_failed++;
                    std::cout << "FAILED: " << result.name << " - " << result.message << std::endl;
                }
            }
        }

        std::cout << "Passed: " << integration_passed << std::endl;
        std::cout << "Failed: " << integration_failed << std::endl;
    } else {
        std::cout << std::endl;
        std::cout << "To run integration tests, use: --integration or -i" << std::endl;
        std::cout << "To specify config file: --config <file> or -c <file>" << std::endl;
    }

    return (failed == 0) ? 0 : 1;
}
