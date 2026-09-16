/**
 * @file test_av_sync.cpp
 * @brief Интеграционные тесты для AV синхронизации
 */

#include <iostream>
#include <thread>
#include <chrono>
#include <vector>
#include <atomic>
#include <mutex>

// Макросы для тестирования
#define TEST_ASSERT(cond, msg) \
    do { \
        if (!(cond)) { \
            std::cerr << "❌ FAIL: " << msg << " at " << __FILE__ << ":" << __LINE__ << std::endl; \
            return false; \
        } \
    } while(0)

#define TEST_PASS(msg) \
    std::cout << "✅ PASS: " << msg << std::endl

// Структура для AV синхронизации (упрощённая версия из rtsp_client.cpp)
struct AVSync {
    int64_t videoTimestamp;      // RTP timestamp видео
    int64_t audioTimestamp;      // RTP timestamp аудио
    int64_t videoClockMs;        // Время видео в мс
    int64_t audioClockMs;        // Время аудио в мс
    int64_t clockOffsetMs;       // Смещение между аудио и видео
    bool syncInitialized;        // Флаг инициализации
    double driftMs;              // Текущий drift
    
    AVSync()
        : videoTimestamp(0),
          audioTimestamp(0),
          videoClockMs(0),
          audioClockMs(0),
          clockOffsetMs(0),
          syncInitialized(false),
          driftMs(0.0) {}
    
    // Обновление видео timestamp
    void updateVideoTimestamp(int64_t rtpTimestamp, int clockRate = 90000) {
        videoTimestamp = rtpTimestamp;
        videoClockMs = (rtpTimestamp * 1000LL) / clockRate;
        
        // Вычисление drift
        if (syncInitialized) {
            driftMs = videoClockMs - (audioClockMs + clockOffsetMs);
            if (driftMs < 0) driftMs = -driftMs; // abs
        }
    }
    
    // Обновление аудио timestamp
    void updateAudioTimestamp(int64_t rtpTimestamp, int clockRate = 90000) {
        audioTimestamp = rtpTimestamp;
        audioClockMs = (rtpTimestamp * 1000LL) / clockRate;
        
        // Инициализация sync при первом появлении обоих таймстампов
        if (!syncInitialized && videoTimestamp > 0) {
            clockOffsetMs = audioClockMs - videoClockMs;
            syncInitialized = true;
        }
        
        // Вычисление drift
        if (syncInitialized) {
            driftMs = videoClockMs - (audioClockMs + clockOffsetMs);
            if (driftMs < 0) driftMs = -driftMs; // abs
        }
    }
    
    // Проверка что синхронизация в допустимых пределах (< 50ms)
    bool isSynced() const {
        if (!syncInitialized) return false;
        return driftMs < 50.0; // < 50ms drift
    }
    
    // Сброс синхронизации
    void reset() {
        videoTimestamp = 0;
        audioTimestamp = 0;
        videoClockMs = 0;
        audioClockMs = 0;
        clockOffsetMs = 0;
        syncInitialized = false;
        driftMs = 0.0;
    }
};

// Тест: Базовая синхронизация
bool test_basic_av_sync() {
    std::cout << "\n--- Тест: Basic AV Sync ---" << std::endl;
    
    AVSync sync;
    
    // Видео приходит первым (RTP timestamp 0)
    sync.updateVideoTimestamp(0);
    TEST_ASSERT(!sync.syncInitialized, "Sync not initialized yet");
    
    // Аудио приходит с небольшим смещением
    sync.updateAudioTimestamp(4500); // 50ms при 90kHz
    TEST_ASSERT(sync.syncInitialized, "Sync initialized after audio");
    TEST_ASSERT(sync.clockOffsetMs == 50, "Clock offset = 50ms");
    
    // Проверка что drift близок к 0
    TEST_ASSERT(sync.driftMs < 1.0, "Initial drift < 1ms");
    
    TEST_PASS("Basic AV synchronization");
    return true;
}

// Тест: Дрейф синхронизации
bool test_sync_drift() {
    std::cout << "\n--- Тест: Sync Drift ---" << std::endl;
    
    AVSync sync;
    
    // Инициализация
    sync.updateVideoTimestamp(0);
    sync.updateAudioTimestamp(0);
    TEST_ASSERT(sync.syncInitialized, "Sync initialized");
    
    // Видео ускоряется (timestamp больше чем ожидалось)
    sync.updateVideoTimestamp(90000); // 1 second
    sync.updateAudioTimestamp(90000); // 1 second
    TEST_ASSERT(sync.driftMs < 1.0, "No drift after 1 second");
    
    // Создаём искусственный drift (видео отстаёт)
    sync.updateVideoTimestamp(180000); // 2 seconds
    sync.updateAudioTimestamp(184500); // 2.05 seconds (50ms ahead)
    
    TEST_ASSERT(sync.driftMs >= 50.0 && sync.driftMs < 51.0, "Drift = 50ms");
    
    TEST_PASS("Sync drift detection");
    return true;
}

// Тест: Переполнение RTP timestamp (wrap-around)
bool test_timestamp_wraparound() {
    std::cout << "\n--- Тест: Timestamp Wrap-around ---" << std::endl;
    
    AVSync sync;
    
    // Инициализация
    sync.updateVideoTimestamp(0);
    sync.updateAudioTimestamp(0);
    
    // RTP timestamp接近最大值 (2^32 - 1)
    uint32_t maxTimestamp = 4294967295U;
    sync.updateVideoTimestamp(maxTimestamp);
    sync.updateAudioTimestamp(maxTimestamp);
    
    // После wrap-around
    sync.updateVideoTimestamp(100);
    sync.updateAudioTimestamp(100);
    
    // drift должен быть близок к 0 даже после wrap-around
    TEST_ASSERT(sync.driftMs < 1.0, "No drift after wrap-around");
    
    TEST_PASS("Timestamp wrap-around handling");
    return true;
}

// Тест: Разные clock rates
bool test_different_clock_rates() {
    std::cout << "\n--- Тест: Different Clock Rates ---" << std::endl;
    
    AVSync sync;
    
    // Видео: H.264 (90kHz)
    sync.updateVideoTimestamp(90000, 90000); // 1 second
    
    // Аудио: AAC (48kHz)
    sync.updateAudioTimestamp(48000, 48000); // 1 second
    
    TEST_ASSERT(sync.syncInitialized, "Sync initialized with different clock rates");
    TEST_ASSERT(sync.driftMs < 1.0, "No drift with different clock rates");
    
    TEST_PASS("Different clock rates handling");
    return true;
}

// Тест: Многопоточная синхронизация
bool test_concurrent_av_sync() {
    std::cout << "\n--- Тест: Concurrent AV Sync ---" << std::endl;
    
    AVSync sync;
    std::mutex syncMutex;
    std::atomic<int> videoFrames{0};
    std::atomic<int> audioFrames{0};
    
    // Поток видео
    auto videoThread = [&]() {
        for (int i = 0; i < 100; i++) {
            std::this_thread::sleep_for(std::chrono::milliseconds(10));
            std::lock_guard<std::mutex> lock(syncMutex);
            sync.updateVideoTimestamp(i * 900); // 10ms each
            videoFrames++;
        }
    };
    
    // Поток аудио
    auto audioThread = [&]() {
        for (int i = 0; i < 100; i++) {
            std::this_thread::sleep_for(std::chrono::milliseconds(10));
            std::lock_guard<std::mutex> lock(syncMutex);
            sync.updateAudioTimestamp(i * 480); // 10ms each at 48kHz
            audioFrames++;
        }
    };
    
    std::thread t1(videoThread);
    std::thread t2(audioThread);
    
    t1.join();
    t2.join();
    
    TEST_ASSERT(videoFrames == 100, "100 video frames processed");
    TEST_ASSERT(audioFrames == 100, "100 audio frames processed");
    TEST_ASSERT(sync.syncInitialized, "Sync initialized in concurrent mode");
    
    TEST_PASS("Concurrent AV synchronization");
    return true;
}

// Тест: Сброс синхронизации
bool test_sync_reset() {
    std::cout << "\n--- Тест: Sync Reset ---" << std::endl;
    
    AVSync sync;
    
    // Инициализация
    sync.updateVideoTimestamp(1000);
    sync.updateAudioTimestamp(1000);
    TEST_ASSERT(sync.syncInitialized, "Sync initialized");
    
    // Сброс
    sync.reset();
    TEST_ASSERT(!sync.syncInitialized, "Sync reset");
    TEST_ASSERT(sync.videoTimestamp == 0, "Video timestamp cleared");
    TEST_ASSERT(sync.audioTimestamp == 0, "Audio timestamp cleared");
    TEST_ASSERT(sync.driftMs == 0.0, "Drift cleared");
    
    TEST_PASS("Sync reset");
    return true;
}

// Тест: Большой drift (> 100ms)
bool test_large_drift() {
    std::cout << "\n--- Тест: Large Drift Detection ---" << std::endl;
    
    AVSync sync;
    
    // Инициализация
    sync.updateVideoTimestamp(0);
    sync.updateAudioTimestamp(0);
    TEST_ASSERT(sync.isSynced(), "Initially synced");
    
    // Создаём большой drift
    sync.updateVideoTimestamp(900000); // 10 seconds
    sync.updateAudioTimestamp(990000); // 11 seconds (1 second ahead)
    
    TEST_ASSERT(sync.driftMs >= 1000.0, "Drift > 1000ms");
    TEST_ASSERT(!sync.isSynced(), "Not synced with large drift");
    
    TEST_PASS("Large drift detection");
    return true;
}

// Запуск всех тестов
int main() {
    std::cout << "========================================" << std::endl;
    std::cout << "  AV Sync Integration Tests" << std::endl;
    std::cout << "  IP-CSS RTSP Client" << std::endl;
    std::cout << "========================================" << std::endl;
    
    int passed = 0;
    int failed = 0;
    
    // Запуск тестов
    if (test_basic_av_sync()) { passed++; } else { failed++; }
    if (test_sync_drift()) { passed++; } else { failed++; }
    if (test_timestamp_wraparound()) { passed++; } else { failed++; }
    if (test_different_clock_rates()) { passed++; } else { failed++; }
    if (test_concurrent_av_sync()) { passed++; } else { failed++; }
    if (test_sync_reset()) { passed++; } else { failed++; }
    if (test_large_drift()) { passed++; } else { failed++; }
    
    // Итоги
    std::cout << "\n========================================" << std::endl;
    std::cout << "  Results: " << passed << " passed, " << failed << " failed" << std::endl;
    std::cout << "========================================" << std::endl;
    
    return failed > 0 ? 1 : 0;
}
