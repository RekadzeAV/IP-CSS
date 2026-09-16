/**
 * Live555 Wrapper Implementation
 * 
 * Обертка над Live555 библиотекой для RTSP клиентской функциональности
 * Предоставляет простой C API для использования из Kotlin через cinterop
 */

#include "live555_wrapper.h"
#include <liveMedia.hh>
#include <BasicUsageEnvironment.hh>
#include <GroupsockHelper.hh>

#include <mutex>
#include <condition_variable>
#include <thread>
#include <atomic>
#include <queue>

// ============================================================================
// Внутренние структуры
// ============================================================================

/**
 * Внутренняя структура RTSP клиента
 */
struct RtspClientImpl {
    // Конфигурация
    RtspClientConfig config;
    
    // Live555 компоненты
    UsageEnvironment* env;
    RTSPClient* rtspClient;
    MediaSession* mediaSession;
    
    // Статус
    std::atomic<RtspClientStatus> status;
    std::atomic<bool> isPlaying;
    std::atomic<bool> isPaused;
    
    // Потоки
    RTPSink** videoRTPSink;
    RTPSink** audioRTPSink;
    
    // Очереди фреймов
    std::queue<MediaFrame> videoFrameQueue;
    std::queue<MediaFrame> audioFrameQueue;
    std::mutex frameQueueMutex;
    std::condition_variable frameAvailable;
    
    // Информация о потоках
    VideoStreamInfo videoInfo;
    AudioStreamInfo audioInfo;
    
    // Thread management
    std::thread* eventThread;
    std::atomic<bool> shouldStop;
    
    // Error handling
    std::string lastError;
    std::mutex errorMutex;
    
    // Конструктор
    RtspClientImpl() 
        : status(RTSP_STATUS_DISCONNECTED)
        , isPlaying(false)
        , isPaused(false)
        , env(nullptr)
        , rtspClient(nullptr)
        , mediaSession(nullptr)
        , videoRTPSink(nullptr)
        , audioRTPSink(nullptr)
        , eventThread(nullptr)
        , shouldStop(false) {
        memset(&videoInfo, 0, sizeof(videoInfo));
        memset(&audioInfo, 0, sizeof(audioInfo));
    }
};

// Глобальная переменная для последней ошибки
static std::string g_lastError;
static std::mutex g_errorMutex;

// ============================================================================
// Утилиты
// ============================================================================

static void setLastError(const char* error) {
    std::lock_guard<std::mutex> lock(g_errorMutex);
    g_lastError = error ? error : "Unknown error";
}

const char* getLastError() {
    std::lock_guard<std::mutex> lock(g_errorMutex);
    return g_lastError.c_str();
}

// ============================================================================
// Live555 callback функции
// ============================================================================

/**
 * Callback для DESCRIBE ответа
 */
static void describeActionable(RTSPClient* rtspClient, int statusCode, 
                               char* responseString, char* responseEnd,
                               void* clientData) {
    RtspClientImpl* client = static_cast<RtspClientImpl*>(clientData);
    if (!client) return;
    
    UsageEnvironment& env = rtspClient->env();
    
    if (statusCode == 0) {
        // Успешный DESCRIBE
        char* sdpStart = responseString;
        if (sdpStart) {
            // Парсим SDP
            client->mediaSession = MediaSession::parseSDP(sdpStart, env);
            if (client->mediaSession) {
                // Извлекаем информацию о потоках
                MediaSubsessionIterator iter(*client->mediaSession);
                MediaSubsession* subsession;
                
                while ((subsession = iter.next()) != nullptr) {
                    const char* codecName = subsession->mediumName();
                    const char* codecSubname = subsession->codecName();
                    
                    if (strcmp(codecName, "video") == 0) {
                        // Видео поток
                        client->videoInfo.width = subsession->videoWidth();
                        client->videoInfo.height = subsession->videoHeight();
                        client->videoInfo.fps = static_cast<int>(subsession->videoFramerate());
                        
                        if (strcmp(codecSubname, "H264") == 0) {
                            client->videoInfo.format = VIDEO_FORMAT_H264;
                        } else if (strcmp(codecSubname, "H265") == 0) {
                            client->videoInfo.format = VIDEO_FORMAT_H265;
                        } else {
                            client->videoInfo.format = VIDEO_FORMAT_UNKNOWN;
                        }
                        
                        client->videoRTPSink = &subsession->rtpSink();
                        
                    } else if (strcmp(codecName, "audio") == 0) {
                        // Аудио поток
                        client->audioInfo.sampleRate = subsession->rtpTimestampFrequency();
                        client->audioInfo.channels = subsession->numChannels();
                        
                        if (strcmp(codecSubname, "AAC") == 0 || strcmp(codecSubname, "MPEG4-GENERIC") == 0) {
                            client->audioInfo.format = AUDIO_FORMAT_AAC;
                        } else if (strcmp(codecSubname, "PCMU") == 0) {
                            client->audioInfo.format = AUDIO_FORMAT_G711;
                        } else if (strcmp(codecSubname, "PCMA") == 0) {
                            client->audioInfo.format = AUDIO_FORMAT_G711;
                        } else {
                            client->audioInfo.format = AUDIO_FORMAT_UNKNOWN;
                        }
                        
                        client->audioRTPSink = &subsession->rtpSink();
                    }
                }
                
                client->status = RTSP_STATUS_CONNECTED;
            } else {
                setLastError("Failed to parse SDP");
                client->status = RTSP_STATUS_ERROR;
            }
        }
    } else {
        // Ошибка DESCRIBE
        std::string errorMsg = "DESCRIBE failed with status: ";
        errorMsg += std::to_string(statusCode);
        if (responseString) {
            errorMsg += " - ";
            errorMsg += responseString;
        }
        setLastError(errorMsg.c_str());
        client->status = RTSP_STATUS_ERROR;
    }
    
    // Освобождаем response
    delete[] responseString;
}

/**
 * Callback для SETUP ответа
 */
static void setupActionable(RTSPClient* rtspClient, int statusCode, 
                            char* responseString, char* responseEnd,
                            void* clientData) {
    RtspClientImpl* client = static_cast<RtspClientImpl*>(clientData);
    if (!client) return;
    
    // Освобождаем response
    delete[] responseString;
    
    // Проверяем статус
    if (statusCode != 0) {
        std::string errorMsg = "SETUP failed with status: ";
        errorMsg += std::to_string(statusCode);
        setLastError(errorMsg.c_str());
        client->status = RTSP_STATUS_ERROR;
        return;
    }
    
    // Продолжаем настройку следующих подсессий
    // В полной реализации здесь будет логика для обработки нескольких потоков
}

/**
 * Callback для PLAY ответа
 */
static void playActionable(RTSPClient* rtspClient, int statusCode, 
                           char* responseString, char* responseEnd,
                           void* clientData) {
    RtspClientImpl* client = static_cast<RtspClientImpl*>(clientData);
    if (!client) return;
    
    // Освобождаем response
    delete[] responseString;
    
    if (statusCode == 0) {
        client->isPlaying = true;
        client->status = RTSP_STATUS_PLAYING;
    } else {
        std::string errorMsg = "PLAY failed with status: ";
        errorMsg += std::to_string(statusCode);
        setLastError(errorMsg.c_str());
        client->status = RTSP_STATUS_ERROR;
    }
}

// ============================================================================
// Event loop thread
// ============================================================================

static void eventLoopThreadFunc(RtspClientImpl* client) {
    if (!client || !client->env) return;
    
    // Запускаем event loop Live555
    while (!client->shouldStop) {
        client->env->taskScheduler().doEventLoop();
    }
}

// ============================================================================
// Public API Implementation
// ============================================================================

RtspClientRef rtsp_client_create(const RtspClientConfig* config) {
    if (!config || !config->url) {
        setLastError("Invalid configuration");
        return nullptr;
    }
    
    try {
        RtspClientImpl* client = new RtspClientImpl();
        client->config = *config;
        client->status = RTSP_STATUS_DISCONNECTED;
        
        // Создаем UsageEnvironment
        client->env = BasicUsageEnvironment::create();
        
        // Создаем RTSP client
        client->rtspClient = RTSPClient::create(*client->env, config->url,
                                                false, // verbosity
                                                nullptr); // ourRTSPClient
        
        if (!client->rtspClient) {
            setLastError("Failed to create RTSP client");
            delete client->env;
            delete client;
            return nullptr;
        }
        
        client->status = RTSP_STATUS_CONNECTED;
        
        return client;
    } catch (const std::exception& e) {
        setLastError(e.what());
        return nullptr;
    }
}

void rtsp_client_destroy(RtspClientRef clientRef) {
    if (!clientRef) return;
    
    RtspClientImpl* client = static_cast<RtspClientImpl*>(clientRef);
    
    // Останавливаем event loop
    client->shouldStop = true;
    
    // Отключаемся
    rtsp_client_disconnect(clientRef);
    
    // Останавливаем event thread
    if (client->eventThread && client->eventThread->joinable()) {
        client->eventThread->join();
        delete client->eventThread;
        client->eventThread = nullptr;
    }
    
    // Освобождаем ресурсы Live555
    if (client->rtspClient) {
        Medium::close(client->rtspClient);
        client->rtspClient = nullptr;
    }
    
    if (client->env) {
        Medium::close(client->env);
        client->env = nullptr;
    }
    
    delete client;
}

bool rtsp_client_connect(RtspClientRef clientRef) {
    if (!clientRef) return false;
    
    RtspClientImpl* client = static_cast<RtspClientImpl*>(clientRef);
    
    if (!client->rtspClient || !client->env) {
        setLastError("RTSP client not initialized");
        return false;
    }
    
    client->status = RTSP_STATUS_CONNECTING;
    
    // Отправляем DESCRIBE запрос
    char* url = strdup(client->config.url);
    client->rtspClient->sendDescribeCommand(
        url,
        describeActionable,
        clientRef,
        client->config.timeoutMs / 1000.0 // timeout in seconds
    );
    free(url);
    
    // Запускаем event loop в отдельном потоке
    client->eventThread = new std::thread(eventLoopThreadFunc, client);
    
    // Ждем завершения подключения (блокирующая операция для упрощения)
    // В продакшене нужно использовать асинхронный API
    std::this_thread::sleep_for(std::chrono::milliseconds(client->config.timeoutMs));
    
    if (client->status == RTSP_STATUS_ERROR) {
        return false;
    }
    
    return (client->status == RTSP_STATUS_CONNECTED);
}

void rtsp_client_disconnect(RtspClientRef clientRef) {
    if (!clientRef) return;
    
    RtspClientImpl* client = static_cast<RtspClientImpl*>(clientRef);
    
    if (client->isPlaying) {
        // Отправляем TEARDOWN
        if (client->rtspClient) {
            client->rtspClient->sendTeardownCommand(nullptr, nullptr);
        }
        client->isPlaying = false;
    }
    
    client->status = RTSP_STATUS_DISCONNECTED;
}

bool rtsp_client_play(RtspClientRef clientRef) {
    if (!clientRef) return false;
    
    RtspClientImpl* client = static_cast<RtspClientImpl*>(clientRef);
    
    if (!client->rtspClient || !client->mediaSession) {
        setLastError("Not connected or no media session");
        return false;
    }
    
    // Отправляем PLAY запрос
    client->rtspClient->sendPlayCommand(
        client->mediaSession,
        playActionable,
        clientRef
    );
    
    // Ждем ответа (упрощенно)
    std::this_thread::sleep_for(std::chrono::milliseconds(1000));
    
    return client->isPlaying;
}

bool rtsp_client_pause(RtspClientRef clientRef) {
    if (!clientRef) return false;
    
    RtspClientImpl* client = static_cast<RtspClientImpl*>(clientRef);
    
    if (!client->rtspClient) {
        setLastError("RTSP client not initialized");
        return false;
    }
    
    // Live555 не имеет прямого PAUSE, нужно использовать TEARDOWN и reconnect
    // В полной реализации можно использовать RTSP PAUSE запрос
    
    if (client->isPlaying) {
        client->isPaused = !client->isPaused;
        // TODO: Реализовать PAUSE через RTSP PAUSE command
    }
    
    return client->isPaused;
}

RtspClientStatus rtsp_client_get_status(RtspClientRef clientRef) {
    if (!clientRef) return RTSP_STATUS_DISCONNECTED;
    
    RtspClientImpl* client = static_cast<RtspClientImpl*>(clientRef);
    return client->status;
}

bool rtsp_client_get_video_info(RtspClientRef clientRef, VideoStreamInfo* info) {
    if (!clientRef || !info) return false;
    
    RtspClientImpl* client = static_cast<RtspClientImpl*>(clientRef);
    
    if (client->videoInfo.format == VIDEO_FORMAT_UNKNOWN) {
        return false;
    }
    
    *info = client->videoInfo;
    return true;
}

bool rtsp_client_get_audio_info(RtspClientRef clientRef, AudioStreamInfo* info) {
    if (!clientRef || !info) return false;
    
    RtspClientImpl* client = static_cast<RtspClientImpl*>(clientRef);
    
    if (client->audioInfo.format == AUDIO_FORMAT_UNKNOWN) {
        return false;
    }
    
    *info = client->audioInfo;
    return true;
}

bool rtsp_client_get_video_frame(RtspClientRef clientRef, MediaFrame* frame, int timeoutMs) {
    if (!clientRef || !frame) return false;
    
    // TODO: Реализовать получение видео фреймов из RTP sink
    // Это требует более глубокой интеграции с Live555 RTP handling
    
    return false;
}

bool rtsp_client_get_audio_frame(RtspClientRef clientRef, MediaFrame* frame, int timeoutMs) {
    if (!clientRef || !frame) return false;
    
    // TODO: Реализовать получение аудио фреймов из RTP sink
    
    return false;
}

void rtsp_client_free_frame(MediaFrame* frame) {
    if (!frame) return;
    // Фреймы управляются caller'ом в текущей реализации
}

const char* rtsp_client_get_last_error(void) {
    return getLastError();
}

bool rtsp_is_supported(void) {
    return true;
}

const char* rtsp_get_version(void) {
    return "1.0.0";
}
