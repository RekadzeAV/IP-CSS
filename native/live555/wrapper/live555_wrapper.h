/**
 * Live555 Wrapper Header
 * 
 * Упрощённый C API для Live555 библиотеки для использования через cinterop
 * Скрывает сложность C++ API Live555
 */

#ifndef LIVE555_WRAPPER_H
#define LIVE555_WRAPPER_H

#include <stdint.h>
#include <stdbool.h>

#ifdef __cplusplus
extern "C" {
#endif

// ============================================================================
// Типы данных
// ============================================================================

/**
 * Статус RTSP клиента
 */
typedef enum {
    RTSP_STATUS_DISCONNECTED = 0,
    RTSP_STATUS_CONNECTING,
    RTSP_STATUS_CONNECTED,
    RTSP_STATUS_PLAYING,
    RTSP_STATUS_PAUSED,
    RTSP_STATUS_ERROR
} RtspClientStatus;

/**
 * Тип медиа потока
 */
typedef enum {
    MEDIA_TYPE_UNKNOWN = 0,
    MEDIA_TYPE_VIDEO,
    MEDIA_TYPE_AUDIO,
    MEDIA_TYPE_SUBTITLE
} MediaType;

/**
 * Формат видео
 */
typedef enum {
    VIDEO_FORMAT_UNKNOWN = 0,
    VIDEO_FORMAT_H264,
    VIDEO_FORMAT_H265,
    VIDEO_FORMAT_MPEG4,
    VIDEO_FORMAT_MJPEG
} VideoFormat;

/**
 * Формат аудио
 */
typedef enum {
    AUDIO_FORMAT_UNKNOWN = 0,
    AUDIO_FORMAT_AAC,
    AUDIO_FORMAT_G711,
    AUDIO_FORMAT_G726,
    AUDIO_FORMAT_MP3
} AudioFormat;

/**
 * Информация о видео потоке
 */
typedef struct {
    VideoFormat format;
    int width;
    int height;
    int fps;
    int bitrate;
} VideoStreamInfo;

/**
 * Информация об аудио потоке
 */
typedef struct {
    AudioFormat format;
    int sampleRate;
    int channels;
    int bitrate;
} AudioStreamInfo;

/**
 * Фрейм видео/аудио
 */
typedef struct {
    uint8_t* data;
    size_t size;
    int64_t timestamp;
    bool isKeyFrame;
    MediaType type;
} MediaFrame;

// ============================================================================
// RTSP Клиент
// ============================================================================

/**
 * Оpaque указатель на RTSP клиент
 */
typedef void* RtspClientRef;

/**
 * Параметры подключения к RTSP серверу
 */
typedef struct {
    const char* url;              // RTSP URL (например, "rtsp://192.168.1.10:554/stream")
    const char* username;         // Имя пользователя (NULL если нет аутентификации)
    const char* password;         // Пароль (NULL если нет аутентификации)
    int timeoutMs;                // Таймаут подключения в мс
    bool enableVideo;             // Включить видео поток
    bool enableAudio;             // Включить аудио поток
    int rtpTransport;             // RTP transport (0=UDP, 1=TCP, 2=Multicast)
} RtspClientConfig;

/**
 * Создать новый RTSP клиент
 * 
 * @param config Конфигурация клиента
 * @return Указатель на клиент или NULL при ошибке
 */
RtspClientRef rtsp_client_create(const RtspClientConfig* config);

/**
 * Освободить ресурсы клиента
 * 
 * @param client Указатель на клиент
 */
void rtsp_client_destroy(RtspClientRef client);

/**
 * Подключиться к RTSP серверу
 * 
 * @param client Указатель на клиент
 * @return true при успехе, false при ошибке
 */
bool rtsp_client_connect(RtspClientRef client);

/**
 * Отключиться от RTSP сервера
 * 
 * @param client Указатель на клиент
 */
void rtsp_client_disconnect(RtspClientRef client);

/**
 * Начать воспроизведение
 * 
 * @param client Указатель на клиент
 * @return true при успехе, false при ошибке
 */
bool rtsp_client_play(RtspClientRef client);

/**
 * Приостановить воспроизведение
 * 
 * @param client Указатель на клиент
 * @return true при успехе, false при ошибке
 */
bool rtsp_client_pause(RtspClientRef client);

/**
 * Получить текущий статус клиента
 * 
 * @param client Указатель на клиент
 * @return Статус клиента
 */
RtspClientStatus rtsp_client_get_status(RtspClientRef client);

/**
 * Получить информацию о видео потоке
 * 
 * @param client Указатель на клиент
 * @param info Буфер для информации (должен быть выделен caller)
 * @return true если информация доступна, false если видео не включено
 */
bool rtsp_client_get_video_info(RtspClientRef client, VideoStreamInfo* info);

/**
 * Получить информацию об аудио потоке
 * 
 * @param client Указатель на клиент
 * @param info Буфер для информации (должен быть выделен caller)
 * @return true если информация доступна, false если аудио не включено
 */
bool rtsp_client_get_audio_info(RtspClientRef client, AudioStreamInfo* info);

// ============================================================================
// Получение фреймов
// ============================================================================

/**
 * Получить следующий видео фрейм (блокирующая операция)
 * 
 * @param client Указатель на клиент
 * @param frame Буфер для фрейма (должен быть выделен caller)
 * @param timeoutMs Таймаут ожидания фрейма в мс
 * @return true если фрейм получен, false если таймаут или ошибка
 */
bool rtsp_client_get_video_frame(RtspClientRef client, MediaFrame* frame, int timeoutMs);

/**
 * Получить следующий аудио фрейм (блокирующая операция)
 * 
 * @param client Указатель на клиент
 * @param frame Буфер для фрейма (должен быть выделен caller)
 * @param timeoutMs Таймаут ожидания фрейма в мс
 * @return true если фрейм получен, false если таймаут или ошибка
 */
bool rtsp_client_get_audio_frame(RtspClientRef client, MediaFrame* frame, int timeoutMs);

/**
 * Освободить фрейм (вызывать после использования)
 * 
 * @param frame Указатель на фрейм
 */
void rtsp_client_free_frame(MediaFrame* frame);

// ============================================================================
// Утилиты
// ============================================================================

/**
 * Получить последнюю ошибку
 * 
 * @return Строка с описанием ошибки (не освобождать caller)
 */
const char* rtsp_client_get_last_error(void);

/**
 * Проверить поддержку платформы
 * 
 * @return true если Live555 доступен на этой платформе
 */
bool rtsp_is_supported(void);

/**
 * Получить версию Live555
 * 
 * @return Строка с версией (не освобождать caller)
 */
const char* rtsp_get_version(void);

#ifdef __cplusplus
}
#endif

#endif // LIVE555_WRAPPER_H
