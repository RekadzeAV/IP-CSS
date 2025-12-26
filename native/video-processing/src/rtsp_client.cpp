#include "rtsp_client.h"
#include <string>
#include <vector>
#include <thread>
#include <atomic>
#include <mutex>
#include <condition_variable>
#include <memory>

#ifdef ENABLE_FFMPEG
extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libavutil/avutil.h>
#include <libavutil/imgutils.h>
#include <libswscale/swscale.h>
}
#endif

struct RTSPClient {
    std::string url;
    std::string username;
    std::string password;
    RTSPStatus status;
    std::atomic<bool> connected;
    std::atomic<bool> playing;
    std::atomic<bool> shouldStop;
    
    RTSPFrameCallback videoCallback;
    RTSPFrameCallback audioCallback;
    RTSPStatusCallback statusCallback;
    void* videoUserData;
    void* audioUserData;
    void* statusUserData;
    
    std::vector<RTSPStream*> streams;
    std::mutex mutex;
    std::thread receiveThread;
    
#ifdef ENABLE_FFMPEG
    AVFormatContext* formatContext;
    AVCodecContext* videoCodecContext;
    AVCodecContext* audioCodecContext;
    SwsContext* swsContext;
    int videoStreamIndex;
    int audioStreamIndex;
#endif
    
    RTSPClient() : status(RTSP_STATUS_DISCONNECTED), connected(false), playing(false),
                   shouldStop(false), videoCallback(nullptr), audioCallback(nullptr),
                   statusCallback(nullptr), videoUserData(nullptr), audioUserData(nullptr),
                   statusUserData(nullptr)
#ifdef ENABLE_FFMPEG
                   , formatContext(nullptr), videoCodecContext(nullptr), audioCodecContext(nullptr),
                   swsContext(nullptr), videoStreamIndex(-1), audioStreamIndex(-1)
#endif
    {}
    
    ~RTSPClient() {
        disconnect();
        for (auto* stream : streams) {
            delete stream;
        }
        streams.clear();
    }
};

struct RTSPStream {
    RTSPStreamType type;
    int width;
    int height;
    int fps;
    std::string codec;
};

extern "C" {

RTSPClient* rtsp_client_create() {
    return new RTSPClient();
}

void rtsp_client_destroy(RTSPClient* client) {
    if (client) {
        delete client;
    }
}

bool rtsp_client_connect(
    RTSPClient* client,
    const char* url,
    const char* username,
    const char* password,
    int timeout_ms
) {
    if (!client || !url) return false;
    
    std::lock_guard<std::mutex> lock(client->mutex);
    
    client->url = url;
    client->username = username ? username : "";
    client->password = password ? password : "";
    client->status = RTSP_STATUS_CONNECTING;
    client->shouldStop = false;
    
#ifdef ENABLE_FFMPEG
    // Инициализация FFmpeg
    avformat_network_init();
    
    // Создание контекста формата
    client->formatContext = avformat_alloc_context();
    if (!client->formatContext) {
        client->status = RTSP_STATUS_ERROR;
        if (client->statusCallback) {
            client->statusCallback(RTSP_STATUS_ERROR, "Failed to allocate format context", client->statusUserData);
        }
        return false;
    }
    
    // Установка таймаута
    AVDictionary* options = nullptr;
    av_dict_set(&options, "rtsp_transport", "tcp", 0);
    av_dict_set_int(&options, "stimeout", timeout_ms * 1000, 0);
    
    // Добавление авторизации в URL, если нужно
    std::string fullUrl = client->url;
    if (!client->username.empty() && !client->password.empty()) {
        size_t protocolPos = fullUrl.find("://");
        if (protocolPos != std::string::npos) {
            std::string protocol = fullUrl.substr(0, protocolPos + 3);
            std::string rest = fullUrl.substr(protocolPos + 3);
            fullUrl = protocol + client->username + ":" + client->password + "@" + rest;
        }
    }
    
    // Открытие RTSP потока
    int ret = avformat_open_input(&client->formatContext, fullUrl.c_str(), nullptr, &options);
    av_dict_free(&options);
    
    if (ret < 0) {
        char errorBuf[256];
        av_strerror(ret, errorBuf, sizeof(errorBuf));
        client->status = RTSP_STATUS_ERROR;
        if (client->statusCallback) {
            client->statusCallback(RTSP_STATUS_ERROR, errorBuf, client->statusUserData);
        }
        avformat_free_context(client->formatContext);
        client->formatContext = nullptr;
        return false;
    }
    
    // Получение информации о потоках
    ret = avformat_find_stream_info(client->formatContext, nullptr);
    if (ret < 0) {
        client->status = RTSP_STATUS_ERROR;
        if (client->statusCallback) {
            client->statusCallback(RTSP_STATUS_ERROR, "Failed to find stream info", client->statusUserData);
        }
        avformat_close_input(&client->formatContext);
        return false;
    }
    
    // Поиск видеопотока и аудиопотока
    client->videoStreamIndex = -1;
    client->audioStreamIndex = -1;
    
    for (unsigned int i = 0; i < client->formatContext->nb_streams; i++) {
        AVStream* stream = client->formatContext->streams[i];
        AVCodecParameters* codecParams = stream->codecpar;
        
        if (codecParams->codec_type == AVMEDIA_TYPE_VIDEO && client->videoStreamIndex == -1) {
            client->videoStreamIndex = i;
            
            const AVCodec* codec = avcodec_find_decoder(codecParams->codec_id);
            if (codec) {
                client->videoCodecContext = avcodec_alloc_context3(codec);
                avcodec_parameters_to_context(client->videoCodecContext, codecParams);
                
                if (avcodec_open2(client->videoCodecContext, codec, nullptr) >= 0) {
                    RTSPStream* videoStream = new RTSPStream();
                    videoStream->type = RTSP_STREAM_VIDEO;
                    videoStream->width = codecParams->width;
                    videoStream->height = codecParams->height;
                    videoStream->fps = stream->r_frame_rate.num / stream->r_frame_rate.den;
                    videoStream->codec = avcodec_get_name(codecParams->codec_id);
                    client->streams.push_back(videoStream);
                }
            }
        } else if (codecParams->codec_type == AVMEDIA_TYPE_AUDIO && client->audioStreamIndex == -1) {
            client->audioStreamIndex = i;
            
            const AVCodec* codec = avcodec_find_decoder(codecParams->codec_id);
            if (codec) {
                client->audioCodecContext = avcodec_alloc_context3(codec);
                avcodec_parameters_to_context(client->audioCodecContext, codecParams);
                
                if (avcodec_open2(client->audioCodecContext, codec, nullptr) >= 0) {
                    RTSPStream* audioStream = new RTSPStream();
                    audioStream->type = RTSP_STREAM_AUDIO;
                    audioStream->width = 0;
                    audioStream->height = 0;
                    audioStream->fps = 0;
                    audioStream->codec = avcodec_get_name(codecParams->codec_id);
                    client->streams.push_back(audioStream);
                }
            }
        }
    }
    
    client->status = RTSP_STATUS_CONNECTED;
    client->connected = true;
    
    if (client->statusCallback) {
        client->statusCallback(RTSP_STATUS_CONNECTED, "Connected successfully", client->statusUserData);
    }
    
    return true;
#else
    // Заглушка без FFmpeg
    client->status = RTSP_STATUS_CONNECTED;
    client->connected = true;
    
    RTSPStream* videoStream = new RTSPStream();
    videoStream->type = RTSP_STREAM_VIDEO;
    videoStream->width = 1920;
    videoStream->height = 1080;
    videoStream->fps = 25;
    videoStream->codec = "H.264";
    client->streams.push_back(videoStream);
    
    if (client->statusCallback) {
        client->statusCallback(RTSP_STATUS_CONNECTED, "Connected (FFmpeg not available)", client->statusUserData);
    }
    
    return true;
#endif
}

void rtsp_client_disconnect(RTSPClient* client) {
    if (!client) return;
    
    std::lock_guard<std::mutex> lock(client->mutex);
    
    rtsp_client_stop(client);
    client->shouldStop = true;
    
#ifdef ENABLE_FFMPEG
    if (client->swsContext) {
        sws_freeContext(client->swsContext);
        client->swsContext = nullptr;
    }
    if (client->videoCodecContext) {
        avcodec_free_context(&client->videoCodecContext);
        client->videoCodecContext = nullptr;
    }
    if (client->audioCodecContext) {
        avcodec_free_context(&client->audioCodecContext);
        client->audioCodecContext = nullptr;
    }
    if (client->formatContext) {
        avformat_close_input(&client->formatContext);
        client->formatContext = nullptr;
    }
    avformat_network_deinit();
#endif
    
    client->connected = false;
    client->status = RTSP_STATUS_DISCONNECTED;
    
    if (client->statusCallback) {
        client->statusCallback(RTSP_STATUS_DISCONNECTED, "Disconnected", client->statusUserData);
    }
}

RTSPStatus rtsp_client_get_status(RTSPClient* client) {
    if (!client) return RTSP_STATUS_DISCONNECTED;
    return client->status;
}

// Функция для приема кадров в отдельном потоке
static void receive_frames_thread(RTSPClient* client) {
    if (!client) return;
    
#ifdef ENABLE_FFMPEG
    AVPacket* packet = av_packet_alloc();
    AVFrame* frame = av_frame_alloc();
    AVFrame* frameRGB = av_frame_alloc();
    
    if (!packet || !frame || !frameRGB) {
        if (packet) av_packet_free(&packet);
        if (frame) av_frame_free(&frame);
        if (frameRGB) av_frame_free(&frameRGB);
        return;
    }
    
    // Инициализация контекста масштабирования
    if (client->videoCodecContext) {
        client->swsContext = sws_getContext(
            client->videoCodecContext->width,
            client->videoCodecContext->height,
            client->videoCodecContext->pix_fmt,
            client->videoCodecContext->width,
            client->videoCodecContext->height,
            AV_PIX_FMT_RGB24,
            SWS_BILINEAR,
            nullptr, nullptr, nullptr
        );
        
        if (client->swsContext && frameRGB) {
            int numBytes = av_image_get_buffer_size(
                AV_PIX_FMT_RGB24,
                client->videoCodecContext->width,
                client->videoCodecContext->height,
                1
            );
            uint8_t* buffer = (uint8_t*)av_malloc(numBytes * sizeof(uint8_t));
            av_image_fill_arrays(
                frameRGB->data,
                frameRGB->linesize,
                buffer,
                AV_PIX_FMT_RGB24,
                client->videoCodecContext->width,
                client->videoCodecContext->height,
                1
            );
        }
    }
    
    while (!client->shouldStop && client->playing) {
        int ret = av_read_frame(client->formatContext, packet);
        if (ret < 0) {
            if (ret == AVERROR_EOF) {
                break;
            }
            continue;
        }
        
        if (packet->stream_index == client->videoStreamIndex && client->videoCodecContext) {
            // Декодирование видеокадра
            ret = avcodec_send_packet(client->videoCodecContext, packet);
            if (ret >= 0) {
                ret = avcodec_receive_frame(client->videoCodecContext, frame);
                if (ret >= 0 && client->swsContext && frameRGB) {
                    // Конвертация в RGB
                    sws_scale(
                        client->swsContext,
                        frame->data,
                        frame->linesize,
                        0,
                        client->videoCodecContext->height,
                        frameRGB->data,
                        frameRGB->linesize
                    );
                    
                    // Создание RTSPFrame для callback
                    if (client->videoCallback) {
                        RTSPFrame* rtspFrame = new RTSPFrame();
                        int rgbSize = client->videoCodecContext->width * client->videoCodecContext->height * 3;
                        rtspFrame->data = new uint8_t[rgbSize];
                        rtspFrame->size = rgbSize;
                        rtspFrame->timestamp = frame->pts;
                        rtspFrame->type = RTSP_STREAM_VIDEO;
                        rtspFrame->width = client->videoCodecContext->width;
                        rtspFrame->height = client->videoCodecContext->height;
                        
                        memcpy(rtspFrame->data, frameRGB->data[0], rgbSize);
                        
                        client->videoCallback(rtspFrame, client->videoUserData);
                    }
                }
            }
        } else if (packet->stream_index == client->audioStreamIndex && client->audioCodecContext) {
            // Декодирование аудиокадра
            ret = avcodec_send_packet(client->audioCodecContext, packet);
            if (ret >= 0) {
                ret = avcodec_receive_frame(client->audioCodecContext, frame);
                if (ret >= 0 && client->audioCallback) {
                    // Создание RTSPFrame для аудио
                    RTSPFrame* rtspFrame = new RTSPFrame();
                    int audioSize = frame->nb_samples * frame->channels * av_get_bytes_per_sample((AVSampleFormat)frame->format);
                    rtspFrame->data = new uint8_t[audioSize];
                    rtspFrame->size = audioSize;
                    rtspFrame->timestamp = frame->pts;
                    rtspFrame->type = RTSP_STREAM_AUDIO;
                    rtspFrame->width = 0;
                    rtspFrame->height = 0;
                    
                    memcpy(rtspFrame->data, frame->data[0], audioSize);
                    
                    client->audioCallback(rtspFrame, client->audioUserData);
                }
            }
        }
        
        av_packet_unref(packet);
    }
    
    av_packet_free(&packet);
    av_frame_free(&frame);
    av_frame_free(&frameRGB);
#endif
}

bool rtsp_client_play(RTSPClient* client) {
    if (!client || !client->connected) return false;
    
    {
        std::lock_guard<std::mutex> lock(client->mutex);
        
        if (client->playing) {
            return true; // Уже воспроизводится
        }
        
        client->playing = true;
        client->status = RTSP_STATUS_PLAYING;
        
        // Запуск потока для приема кадров
        if (client->receiveThread.joinable()) {
            client->receiveThread.join();
        }
        client->receiveThread = std::thread(receive_frames_thread, client);
    }
    
    if (client->statusCallback) {
        client->statusCallback(RTSP_STATUS_PLAYING, "Playing", client->statusUserData);
    }
    
    return true;
}

bool rtsp_client_stop(RTSPClient* client) {
    if (!client) return false;
    
    {
        std::lock_guard<std::mutex> lock(client->mutex);
        
        if (!client->playing) {
            return true; // Уже остановлен
        }
        
        client->shouldStop = true;
        client->playing = false;
    }
    
    // Ожидание завершения потока приема
    if (client->receiveThread.joinable()) {
        client->receiveThread.join();
    }
    
    {
        std::lock_guard<std::mutex> lock(client->mutex);
        if (client->connected) {
            client->status = RTSP_STATUS_CONNECTED;
        } else {
            client->status = RTSP_STATUS_DISCONNECTED;
        }
    }
    
    return true;
}

bool rtsp_client_pause(RTSPClient* client) {
    if (!client || !client->playing) return false;
    
    {
        std::lock_guard<std::mutex> lock(client->mutex);
        client->shouldStop = true;
        client->playing = false;
    }
    
    // Ожидание завершения потока приема
    if (client->receiveThread.joinable()) {
        client->receiveThread.join();
    }
    
    {
        std::lock_guard<std::mutex> lock(client->mutex);
        client->status = RTSP_STATUS_CONNECTED;
    }
    
    return true;
}

int rtsp_client_get_stream_count(RTSPClient* client) {
    if (!client) return 0;
    std::lock_guard<std::mutex> lock(client->mutex);
    return static_cast<int>(client->streams.size());
}

RTSPStreamType rtsp_client_get_stream_type(RTSPClient* client, int streamIndex) {
    if (!client || streamIndex < 0 || streamIndex >= static_cast<int>(client->streams.size())) {
        return RTSP_STREAM_VIDEO;
    }
    std::lock_guard<std::mutex> lock(client->mutex);
    return client->streams[streamIndex]->type;
}

void rtsp_client_set_frame_callback(
    RTSPClient* client,
    RTSPStreamType streamType,
    RTSPFrameCallback callback,
    void* userData
) {
    if (!client) return;
    
    std::lock_guard<std::mutex> lock(client->mutex);
    
    if (streamType == RTSP_STREAM_VIDEO) {
        client->videoCallback = callback;
        client->videoUserData = userData;
    } else if (streamType == RTSP_STREAM_AUDIO) {
        client->audioCallback = callback;
        client->audioUserData = userData;
    }
}

void rtsp_client_set_status_callback(
    RTSPClient* client,
    RTSPStatusCallback callback,
    void* userData
) {
    if (!client) return;
    
    std::lock_guard<std::mutex> lock(client->mutex);
    client->statusCallback = callback;
    client->statusUserData = userData;
}

bool rtsp_client_get_stream_info(
    RTSPClient* client,
    int streamIndex,
    int* width,
    int* height,
    int* fps,
    char* codec,
    int codecBufferSize
) {
    if (!client || streamIndex < 0 || streamIndex >= static_cast<int>(client->streams.size())) {
        return false;
    }
    
    std::lock_guard<std::mutex> lock(client->mutex);
    RTSPStream* stream = client->streams[streamIndex];
    
    if (width) *width = stream->width;
    if (height) *height = stream->height;
    if (fps) *fps = stream->fps;
    if (codec && codecBufferSize > 0) {
        strncpy(codec, stream->codec.c_str(), codecBufferSize - 1);
        codec[codecBufferSize - 1] = '\0';
    }
    
    return true;
}

int rtsp_frame_get_size(RTSPFrame* frame) {
    return frame ? frame->size : 0;
}

const uint8_t* rtsp_frame_get_data(RTSPFrame* frame) {
    return frame ? frame->data : nullptr;
}

int64_t rtsp_frame_get_timestamp(RTSPFrame* frame) {
    return frame ? frame->timestamp : 0;
}

void rtsp_frame_release(RTSPFrame* frame) {
    if (frame && frame->data) {
        delete[] frame->data;
        delete frame;
    }
}

} // extern "C"

