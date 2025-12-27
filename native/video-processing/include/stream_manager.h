#ifndef STREAM_MANAGER_H
#define STREAM_MANAGER_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>
#include <stdbool.h>
#include "rtsp_client.h"

// Тип потока
typedef enum {
    STREAM_TYPE_RTSP,
    STREAM_TYPE_FILE,
    STREAM_TYPE_NETWORK
} StreamType;

// Статус потока
typedef enum {
    STREAM_STATUS_IDLE,
    STREAM_STATUS_CONNECTING,
    STREAM_STATUS_CONNECTED,
    STREAM_STATUS_PLAYING,
    STREAM_STATUS_PAUSED,
    STREAM_STATUS_ERROR
} StreamStatus;

// Callback для получения кадров
typedef void (*StreamFrameCallback)(RTSPFrame* frame, void* userData);
typedef void (*StreamStatusCallback)(StreamStatus status, const char* message, void* userData);

// Конфигурация потока
typedef struct {
    StreamType type;
    char url[512];
    char username[128];
    char password[128];
    int timeoutMs;
    bool enableVideo;
    bool enableAudio;
} StreamConfig;

// Структура менеджера потоков (opaque)
typedef struct StreamManager StreamManager;

// Создание менеджера потоков
StreamManager* stream_manager_create();

// Уничтожение менеджера потоков
void stream_manager_destroy(StreamManager* manager);

// Добавление потока
int stream_manager_add_stream(StreamManager* manager, const StreamConfig* config);

// Удаление потока
bool stream_manager_remove_stream(StreamManager* manager, int streamId);

// Подключение к потоку
bool stream_manager_connect_stream(StreamManager* manager, int streamId);

// Отключение от потока
bool stream_manager_disconnect_stream(StreamManager* manager, int streamId);

// Начало воспроизведения
bool stream_manager_play_stream(StreamManager* manager, int streamId);

// Остановка воспроизведения
bool stream_manager_stop_stream(StreamManager* manager, int streamId);

// Пауза воспроизведения
bool stream_manager_pause_stream(StreamManager* manager, int streamId);

// Получение статуса потока
StreamStatus stream_manager_get_status(StreamManager* manager, int streamId);

// Установка callback для кадров
void stream_manager_set_frame_callback(
    StreamManager* manager,
    int streamId,
    StreamFrameCallback callback,
    void* userData
);

// Установка callback для статуса
void stream_manager_set_status_callback(
    StreamManager* manager,
    StreamStatusCallback callback,
    void* userData
);

// Получение количества потоков
int stream_manager_get_stream_count(StreamManager* manager);

#ifdef __cplusplus
}
#endif

#endif // STREAM_MANAGER_H



