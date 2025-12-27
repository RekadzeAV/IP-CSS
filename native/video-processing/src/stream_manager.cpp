#include "stream_manager.h"
#include "rtsp_client.h"
#include <map>
#include <mutex>
#include <memory>
#include <string>
#include <vector>

struct StreamInfo {
    int id;
    StreamType type;
    StreamConfig config;
    StreamStatus status;
    RTSPClient* rtspClient;
    StreamFrameCallback frameCallback;
    StreamStatusCallback statusCallback;
    void* userData;
    
    StreamInfo() : id(-1), type(STREAM_TYPE_RTSP), status(STREAM_STATUS_IDLE),
                   rtspClient(nullptr), frameCallback(nullptr),
                   statusCallback(nullptr), userData(nullptr) {}
};

struct StreamManager {
    std::map<int, StreamInfo> streams;
    std::mutex mutex;
    int nextStreamId;
    StreamStatusCallback globalStatusCallback;
    
    StreamManager() : nextStreamId(1), globalStatusCallback(nullptr) {}
};

// Вспомогательная функция для конвертации RTSP статуса в StreamStatus
static StreamStatus rtsp_status_to_stream_status(RTSPStatus rtspStatus) {
    switch (rtspStatus) {
        case RTSP_STATUS_DISCONNECTED:
            return STREAM_STATUS_IDLE;
        case RTSP_STATUS_CONNECTING:
            return STREAM_STATUS_CONNECTING;
        case RTSP_STATUS_CONNECTED:
            return STREAM_STATUS_CONNECTED;
        case RTSP_STATUS_PLAYING:
            return STREAM_STATUS_PLAYING;
        case RTSP_STATUS_ERROR:
            return STREAM_STATUS_ERROR;
        default:
            return STREAM_STATUS_IDLE;
    }
}

// Callback для RTSP клиента
static void rtsp_frame_callback_wrapper(RTSPFrame* frame, void* userData) {
    StreamInfo* streamInfo = static_cast<StreamInfo*>(userData);
    if (streamInfo && streamInfo->frameCallback) {
        streamInfo->frameCallback(frame, streamInfo->userData);
    }
}

static void rtsp_status_callback_wrapper(RTSPStatus status, const char* message, void* userData) {
    StreamInfo* streamInfo = static_cast<StreamInfo*>(userData);
    if (streamInfo) {
        streamInfo->status = rtsp_status_to_stream_status(status);
        if (streamInfo->statusCallback) {
            streamInfo->statusCallback(streamInfo->status, message, streamInfo->userData);
        }
    }
}

StreamManager* stream_manager_create() {
    return new StreamManager();
}

void stream_manager_destroy(StreamManager* manager) {
    if (!manager) return;
    
    std::lock_guard<std::mutex> lock(manager->mutex);
    
    // Отключение всех потоков
    for (auto& pair : manager->streams) {
        StreamInfo& stream = pair.second;
        if (stream.rtspClient) {
            rtsp_client_disconnect(stream.rtspClient);
            rtsp_client_destroy(stream.rtspClient);
        }
    }
    
    manager->streams.clear();
    delete manager;
}

int stream_manager_add_stream(StreamManager* manager, const StreamConfig* config) {
    if (!manager || !config) {
        return -1;
    }
    
    std::lock_guard<std::mutex> lock(manager->mutex);
    
    StreamInfo streamInfo;
    streamInfo.id = manager->nextStreamId++;
    streamInfo.type = config->type;
    streamInfo.config = *config;
    streamInfo.status = STREAM_STATUS_IDLE;
    
    // Создание RTSP клиента для RTSP потоков
    if (config->type == STREAM_TYPE_RTSP) {
        streamInfo.rtspClient = rtsp_client_create();
        if (streamInfo.rtspClient) {
            rtsp_client_set_frame_callback(
                streamInfo.rtspClient,
                RTSP_STREAM_VIDEO,
                rtsp_frame_callback_wrapper,
                &streamInfo
            );
            rtsp_client_set_status_callback(
                streamInfo.rtspClient,
                rtsp_status_callback_wrapper,
                &streamInfo
            );
        }
    }
    
    manager->streams[streamInfo.id] = streamInfo;
    
    return streamInfo.id;
}

bool stream_manager_remove_stream(StreamManager* manager, int streamId) {
    if (!manager) return false;
    
    std::lock_guard<std::mutex> lock(manager->mutex);
    
    auto it = manager->streams.find(streamId);
    if (it == manager->streams.end()) {
        return false;
    }
    
    StreamInfo& stream = it->second;
    
    // Отключение и уничтожение клиента
    if (stream.rtspClient) {
        rtsp_client_disconnect(stream.rtspClient);
        rtsp_client_destroy(stream.rtspClient);
    }
    
    manager->streams.erase(it);
    
    return true;
}

bool stream_manager_connect_stream(StreamManager* manager, int streamId) {
    if (!manager) return false;
    
    std::lock_guard<std::mutex> lock(manager->mutex);
    
    auto it = manager->streams.find(streamId);
    if (it == manager->streams.end()) {
        return false;
    }
    
    StreamInfo& stream = it->second;
    
    if (stream.type == STREAM_TYPE_RTSP && stream.rtspClient) {
        stream.status = STREAM_STATUS_CONNECTING;
        
        bool success = rtsp_client_connect(
            stream.rtspClient,
            stream.config.url,
            stream.config.username[0] ? stream.config.username : nullptr,
            stream.config.password[0] ? stream.config.password : nullptr,
            stream.config.timeoutMs
        );
        
        if (success) {
            stream.status = STREAM_STATUS_CONNECTED;
        } else {
            stream.status = STREAM_STATUS_ERROR;
        }
        
        return success;
    }
    
    return false;
}

bool stream_manager_disconnect_stream(StreamManager* manager, int streamId) {
    if (!manager) return false;
    
    std::lock_guard<std::mutex> lock(manager->mutex);
    
    auto it = manager->streams.find(streamId);
    if (it == manager->streams.end()) {
        return false;
    }
    
    StreamInfo& stream = it->second;
    
    if (stream.rtspClient) {
        rtsp_client_disconnect(stream.rtspClient);
        stream.status = STREAM_STATUS_IDLE;
        return true;
    }
    
    return false;
}

bool stream_manager_play_stream(StreamManager* manager, int streamId) {
    if (!manager) return false;
    
    std::lock_guard<std::mutex> lock(manager->mutex);
    
    auto it = manager->streams.find(streamId);
    if (it == manager->streams.end()) {
        return false;
    }
    
    StreamInfo& stream = it->second;
    
    if (stream.rtspClient) {
        bool success = rtsp_client_play(stream.rtspClient);
        if (success) {
            stream.status = STREAM_STATUS_PLAYING;
        }
        return success;
    }
    
    return false;
}

bool stream_manager_stop_stream(StreamManager* manager, int streamId) {
    if (!manager) return false;
    
    std::lock_guard<std::mutex> lock(manager->mutex);
    
    auto it = manager->streams.find(streamId);
    if (it == manager->streams.end()) {
        return false;
    }
    
    StreamInfo& stream = it->second;
    
    if (stream.rtspClient) {
        bool success = rtsp_client_stop(stream.rtspClient);
        if (success) {
            stream.status = STREAM_STATUS_CONNECTED;
        }
        return success;
    }
    
    return false;
}

bool stream_manager_pause_stream(StreamManager* manager, int streamId) {
    if (!manager) return false;
    
    std::lock_guard<std::mutex> lock(manager->mutex);
    
    auto it = manager->streams.find(streamId);
    if (it == manager->streams.end()) {
        return false;
    }
    
    StreamInfo& stream = it->second;
    
    if (stream.rtspClient) {
        bool success = rtsp_client_pause(stream.rtspClient);
        if (success) {
            stream.status = STREAM_STATUS_PAUSED;
        }
        return success;
    }
    
    return false;
}

StreamStatus stream_manager_get_status(StreamManager* manager, int streamId) {
    if (!manager) return STREAM_STATUS_ERROR;
    
    std::lock_guard<std::mutex> lock(manager->mutex);
    
    auto it = manager->streams.find(streamId);
    if (it == manager->streams.end()) {
        return STREAM_STATUS_ERROR;
    }
    
    return it->second.status;
}

void stream_manager_set_frame_callback(
    StreamManager* manager,
    int streamId,
    StreamFrameCallback callback,
    void* userData
) {
    if (!manager) return;
    
    std::lock_guard<std::mutex> lock(manager->mutex);
    
    auto it = manager->streams.find(streamId);
    if (it != manager->streams.end()) {
        it->second.frameCallback = callback;
        it->second.userData = userData;
    }
}

void stream_manager_set_status_callback(
    StreamManager* manager,
    StreamStatusCallback callback,
    void* userData
) {
    if (!manager) return;
    
    std::lock_guard<std::mutex> lock(manager->mutex);
    manager->globalStatusCallback = callback;
}

int stream_manager_get_stream_count(StreamManager* manager) {
    if (!manager) return 0;
    
    std::lock_guard<std::mutex> lock(manager->mutex);
    return static_cast<int>(manager->streams.size());
}


