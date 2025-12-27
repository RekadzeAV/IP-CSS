#ifndef RTSP_CLIENT_H
#define RTSP_CLIENT_H

#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>
#include <stdbool.h>

// Типы данных для RTSP клиента
typedef struct RTSPClient RTSPClient;
typedef struct RTSPStream RTSPStream;

// Тип потока
typedef enum {
    RTSP_STREAM_VIDEO,
    RTSP_STREAM_AUDIO,
    RTSP_STREAM_METADATA
} RTSPStreamType;

// Статус клиента
typedef enum {
    RTSP_STATUS_DISCONNECTED,
    RTSP_STATUS_CONNECTING,
    RTSP_STATUS_CONNECTED,
    RTSP_STATUS_PLAYING,
    RTSP_STATUS_ERROR
} RTSPStatus;

// Получение данных кадра (определено раньше для использования в callback)
typedef struct {
    uint8_t* data;
    int size;
    int64_t timestamp;
    RTSPStreamType type;
    int width;
    int height;
} RTSPFrame;

// Callback для получения кадров
typedef void (*RTSPFrameCallback)(RTSPFrame* frame, void* userData);
typedef void (*RTSPStatusCallback)(RTSPStatus status, const char* message, void* userData);

// Создание RTSP клиента
RTSPClient* rtsp_client_create();

// Уничтожение RTSP клиента
void rtsp_client_destroy(RTSPClient* client);

// Подключение к RTSP серверу
bool rtsp_client_connect(
    RTSPClient* client,
    const char* url,
    const char* username,
    const char* password,
    int timeout_ms
);

// Отключение от сервера
void rtsp_client_disconnect(RTSPClient* client);

// Получение статуса
RTSPStatus rtsp_client_get_status(RTSPClient* client);

// Начало воспроизведения
bool rtsp_client_play(RTSPClient* client);

// Остановка воспроизведения
bool rtsp_client_stop(RTSPClient* client);

// Пауза воспроизведения
bool rtsp_client_pause(RTSPClient* client);

// Получение количества потоков
int rtsp_client_get_stream_count(RTSPClient* client);

// Получение типа потока
RTSPStreamType rtsp_client_get_stream_type(RTSPClient* client, int streamIndex);

// Установка callback для получения кадров
void rtsp_client_set_frame_callback(
    RTSPClient* client,
    RTSPStreamType streamType,
    RTSPFrameCallback callback,
    void* userData
);

// Установка callback для изменения статуса
void rtsp_client_set_status_callback(
    RTSPClient* client,
    RTSPStatusCallback callback,
    void* userData
);

// Получение информации о потоке
bool rtsp_client_get_stream_info(
    RTSPClient* client,
    int streamIndex,
    int* width,
    int* height,
    int* fps,
    char* codec,
    int codecBufferSize
);

// Получение размера буфера кадра
int rtsp_frame_get_size(RTSPFrame* frame);

// Получение данных кадра
const uint8_t* rtsp_frame_get_data(RTSPFrame* frame);

// Получение временной метки кадра
int64_t rtsp_frame_get_timestamp(RTSPFrame* frame);

// Освобождение кадра
void rtsp_frame_release(RTSPFrame* frame);

// Параметры автоматического переподключения
typedef struct {
    bool enabled;           // Включить автоматическое переподключение
    int maxRetries;        // Максимальное количество попыток (0 = бесконечно)
    int initialDelayMs;    // Начальная задержка между попытками (мс)
    int maxDelayMs;        // Максимальная задержка между попытками (мс)
    float backoffMultiplier; // Множитель для экспоненциальной задержки
} RTSPReconnectParams;

// Установка параметров автоматического переподключения
void rtsp_client_set_reconnect_params(RTSPClient* client, const RTSPReconnectParams* params);

#ifdef __cplusplus
}
#endif

#endif // RTSP_CLIENT_H



