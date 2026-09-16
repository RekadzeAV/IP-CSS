#include "rtsp_client.h"
#include "audio_decoder.h"  // Аудио декодеры (FFmpeg)
#include <string>
#include <vector>
#include <thread>
#include <atomic>
#include <mutex>
#include <condition_variable>
#include <memory>
#include <sstream>
#include <algorithm>
#include <cstring>
#include <cstdlib>
#include <ctime>
#include <iomanip>
#include <regex>
#include <cstdint>
#include <chrono>

// Платформо-специфичные заголовки для сокетов
#ifdef _WIN32
    #define NOMINMAX
    #include <winsock2.h>
    #include <ws2tcpip.h>
    #pragma comment(lib, "ws2_32.lib")
    #ifndef socklen_t
    typedef int socklen_t;
    #endif
    #define close closesocket
    #define SHUT_RDWR SD_BOTH
#else
    #include <sys/socket.h>
    #include <netinet/in.h>
    #include <arpa/inet.h>
    #include <netdb.h>
    #include <unistd.h>
    #include <fcntl.h>
    #include <errno.h>
    #define INVALID_SOCKET -1
    #define SOCKET_ERROR -1
    typedef int SOCKET;
#endif

#ifdef ENABLE_FFMPEG
extern "C" {
#include <libavformat/avformat.h>
#include <libavcodec/avcodec.h>
#include <libavutil/avutil.h>
#include <libavutil/imgutils.h>
#include <libswscale/swscale.h>
#include <libswresample/swresample.h>
}
#endif

// Структура для RTP потока
struct RTPStream {
    RTSPStreamType type;
    std::string controlUrl;
    int clientRtpPort;
    int clientRtcpPort;
    int serverRtpPort;
    int serverRtcpPort;
    std::string transport;
    std::string codec;
    int payloadType;
    int clockRate;
    int channels; // Для аудио: количество каналов
    int width;
    int height;
    int fps;
    SOCKET rtpSocket;
    SOCKET rtcpSocket;
    uint16_t rtpSequence;
    uint32_t rtpSSRC;
    uint32_t rtpTimestamp;
    std::vector<uint8_t> buffer;

    // Дополнительные параметры для кодеков
    std::string sps; // H.264/H.265 SPS (Sequence Parameter Set)
    std::string pps; // H.264/H.265 PPS (Picture Parameter Set)
    std::string vps; // H.265 VPS (Video Parameter Set)
    std::string profileLevelId; // H.264 profile-level-id
    std::string fmtpParams; // Все параметры fmtp
    std::string range; // Временной диапазон (a=range)
    std::string trackId; // Track ID из control URL
    std::string aacConfig; // AAC AudioSpecificConfig (hex из SDP config=...)

    // Буферы для сборки фрагментированных RTP пакетов
    std::vector<uint8_t> fragmentedBuffer; // Буфер для сборки фрагментированных NAL units
    uint16_t expectedSequence; // Ожидаемый sequence number для следующего пакета
    bool isFragmenting; // Флаг, что идет сборка фрагментированного пакета
    uint8_t fragmentStartType; // Тип NAL unit для фрагментированного пакета

    // TCP транспорт (interleaved binary data)
    bool useTcp; // Использовать TCP транспорт вместо UDP
    int rtpChannel; // RTP channel для TCP (interleaved)
    int rtcpChannel; // RTCP channel для TCP (interleaved)
    std::vector<uint8_t> tcpBuffer; // Буфер для чтения TCP данных

    // RTCP статистика
    uint32_t packetsReceived; // Количество полученных RTP пакетов
    uint32_t packetsLost; // Количество потерянных пакетов
    uint32_t packetsExpected; // Ожидаемое количество пакетов
    uint32_t jitter; // Jitter (вариация задержки)
    uint64_t lastSrTimestamp; // Timestamp последнего SR пакета
    uint64_t lastSrNtpTimestamp; // NTP timestamp последнего SR пакета

    RTPStream() : clientRtpPort(0), clientRtcpPort(0), serverRtpPort(0), serverRtcpPort(0),
                  payloadType(96), clockRate(90000), channels(0), width(0), height(0), fps(0),
                  rtpSocket(INVALID_SOCKET), rtcpSocket(INVALID_SOCKET),
                  rtpSequence(0), rtpSSRC(0), rtpTimestamp(0),
                  expectedSequence(0), isFragmenting(false), fragmentStartType(0),
                  useTcp(false), rtpChannel(-1), rtcpChannel(-1),
                  packetsReceived(0), packetsLost(0), packetsExpected(0), jitter(0),
                  lastSrTimestamp(0), lastSrNtpTimestamp(0)
#ifdef ENABLE_FFMPEG
                  , aacDecoder(nullptr), g711Decoder(nullptr), audioResampler(nullptr)
#endif
    {}
#ifdef ENABLE_FFMPEG
    // Параметры декодера аудио для данного потока
    struct AACDecoder* aacDecoder;
    struct G711Decoder* g711Decoder;
    struct AudioResampler* audioResampler;
#endif
};

// Структура для парсинга URL
struct RTSPUrl {
    std::string protocol;
    std::string host;
    int port;
    std::string path;
    std::string username;
    std::string password;
};

// Структура для хранения RTSP заголовков ответа
struct RTSPResponseHeaders {
    int statusCode;
    std::string statusText;
    std::string sessionId;
    std::string wwwAuthenticate;
    std::string transport; // Transport заголовок из SETUP ответа
    std::string range; // Range заголовок из PLAY ответа
    std::string rtpInfo; // RTP-Info заголовок из PLAY ответа
    std::string contentType;
    std::string contentLength;
    std::string cseq;
    std::string server;
    std::string publicHeader; // Public заголовок из OPTIONS ответа
    std::string location; // Location заголовок для редиректов
    std::string retryAfter; // Retry-After для ошибок
    std::string errorMessage; // Сообщение об ошибке из ответа
};

// Forward declaration для Digest Authentication параметров
struct DigestAuthParams {
    std::string realm;
    std::string nonce;
    std::string algorithm;
    std::string qop;
    std::string opaque;
    bool stale;
};

// Структура для синхронизации аудио и видео
struct AVSync {
    int64_t videoTimestamp;      // RTP timestamp видео
    int64_t audioTimestamp;      // RTP timestamp аудио
    int64_t videoClockMs;        // Время видео в мс
    int64_t audioClockMs;        // Время аудио в мс
    int64_t clockOffsetMs;       // Смещение между аудио и видео
    bool syncInitialized;        // Флаг инициализации

    AVSync()
        : videoTimestamp(0),
          audioTimestamp(0),
          videoClockMs(0),
          audioClockMs(0),
          clockOffsetMs(0),
          syncInitialized(false) {}
};

#ifdef ENABLE_FFMPEG

// Аудио декодеры импортируются из audio_decoder.h
// Функции инициализации и декодирования находятся в audio_decoder.cpp

// Конвертация hex-строки в байты (используется для AAC config)
static std::vector<uint8_t> hex_string_to_bytes(const std::string& hex) {
    std::vector<uint8_t> bytes;
    bytes.reserve(hex.length() / 2);
    for (size_t i = 0; i + 1 < hex.length(); i += 2) {
        std::string byteString = hex.substr(i, 2);
        uint8_t byte = static_cast<uint8_t>(strtol(byteString.c_str(), nullptr, 16));
        bytes.push_back(byte);
    }
    return bytes;
}

// Инициализация AAC декодера для потока
static AACDecoder* init_aac_decoder_for_stream(const RTPStream& stream) {
    if (stream.clockRate <= 0 || stream.channels <= 0) {
        return nullptr;
    }
    // Вызываем функцию из audio_decoder.cpp
    return init_aac_decoder(stream.aacConfig.c_str(), stream.clockRate, stream.channels);
}

static void free_aac_decoder(AACDecoder*& decoder) {
    if (!decoder) return;
    free_aac_decoder(decoder);
}

// Инициализация G.711 декодера
static G711Decoder* init_g711_decoder_for_stream(const RTPStream& stream) {
    if (stream.clockRate <= 0 || stream.channels <= 0) {
        return nullptr;
    }
    bool isPCMU = (stream.codec == "PCMU");
    return init_g711_decoder(isPCMU, stream.clockRate, stream.channels);
}

static void free_g711_decoder(G711Decoder*& decoder) {
    if (!decoder) return;
    free_g711_decoder(decoder);
}

// Ресемплер аудио
static AudioResampler* init_audio_resampler(int inputSampleRate,
                                            int inputChannels,
                                            int outputSampleRate,
                                            int outputChannels) {
    return ::init_audio_resampler(inputSampleRate, inputChannels, outputSampleRate, outputChannels);
}

static void free_audio_resampler(AudioResampler*& resampler) {
    if (!resampler) return;
    free_audio_resampler(resampler);
}

// Декодирование одного AAC RTP payload в PCM16
static bool decode_aac_packet(AACDecoder* decoder,
                              const uint8_t* data,
                              int dataSize,
                              DecodedAudioFrame* outputFrame) {
    return ::decode_aac_packet(decoder, data, dataSize, outputFrame);
}

// Декодирование одного G.711 RTP payload в PCM16
static bool decode_g711_packet(G711Decoder* decoder,
                               const uint8_t* data,
                               int dataSize,
                               DecodedAudioFrame* outputFrame) {
    return ::decode_g711_packet(decoder, data, dataSize, outputFrame);
}

// Очистка ресурсов декодеров для RTP потока
static void cleanup_rtp_stream_decoders(RTPStream& stream) {
    if (stream.aacDecoder) {
        free_aac_decoder(stream.aacDecoder);
    }
    if (stream.g711Decoder) {
        free_g711_decoder(stream.g711Decoder);
    }
    if (stream.audioResampler) {
        free_audio_resampler(stream.audioResampler);
    }
}

#endif // ENABLE_FFMPEG

// Forward declaration для структуры NAL unit
struct NALUnit {
    uint8_t type;
    bool forbidden;
    uint8_t nri; // NAL Reference IDC
    std::vector<uint8_t> data;

    NALUnit() : type(0), forbidden(false), nri(0) {}
};

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
    std::condition_variable handshakeCv;  // Для синхронизации handshake
    std::atomic<bool> handshakeComplete;  // Флаг завершения handshake
    std::thread rtpThread;
    std::thread reconnectThread;

    // Параметры автоматического переподключения
    RTSPReconnectParams reconnectParams;
    std::atomic<bool> reconnectEnabled;
    std::atomic<int> reconnectAttempts;
    std::atomic<bool> isReconnecting;

    // RTSP протокол
    RTSPUrl rtspUrl;
    SOCKET rtspSocket;
    std::string sessionId;
    int cseq;
    std::string baseUrl;
    std::vector<RTPStream> rtpStreams;

    // Digest Authentication параметры
    DigestAuthParams digestParams;
    bool useDigestAuth;
    int digestNc;

    // RTP/RTCP статистика
    struct {
        uint32_t packetsSent;
        uint32_t packetsReceived;
        uint32_t packetsLost;
        uint32_t bytesSent;
        uint32_t bytesReceived;
        uint32_t jitter;
        uint32_t lastSrTimestamp;
        uint64_t lastSrNtpTimestamp;
    } stats;

    // Синхронизация аудио и видео
    struct {
        int64_t clockOffsetMs;
        bool syncInitialized;
        uint32_t videoTimestamp;
        uint32_t audioTimestamp;
        int64_t videoClockMs;
        int64_t audioClockMs;
    } avSync;

    RTSPClient();
    ~RTSPClient();
};

struct RTSPStream {
    RTSPStreamType type;
    int width;
    int height;
    int fps;
    std::string codec;
};

// Определение конструктора RTSPClient
RTSPClient::RTSPClient() : status(RTSP_STATUS_DISCONNECTED), connected(false), playing(false),
               shouldStop(false), videoCallback(nullptr), audioCallback(nullptr),
               statusCallback(nullptr), videoUserData(nullptr), audioUserData(nullptr),
               statusUserData(nullptr), rtspSocket(INVALID_SOCKET), cseq(1),
               useDigestAuth(false), digestNc(0), avSync(), handshakeComplete(false)
{
#ifdef _WIN32
    WSADATA wsaData;
    WSAStartup(MAKEWORD(2, 2), &wsaData);
#endif
}

// Определение деструктора RTSPClient
RTSPClient::~RTSPClient() {
    shouldStop = true;

    // Ожидание завершения потока переподключения
    if (reconnectThread.joinable()) {
        reconnectThread.join();
    }

    // Отключение от сервера
    if (rtspSocket != INVALID_SOCKET) {
        close(rtspSocket);
        rtspSocket = INVALID_SOCKET;
    }
    for (auto* stream : streams) {
        delete stream;
    }
    streams.clear();

    // Закрытие RTP сокетов
    for (auto& rtpStream : rtpStreams) {
        if (rtpStream.rtpSocket != INVALID_SOCKET) {
            close(rtpStream.rtpSocket);
        }
        if (rtpStream.rtcpSocket != INVALID_SOCKET) {
            close(rtpStream.rtcpSocket);
        }
    }
    rtpStreams.clear();

#ifdef _WIN32
    WSACleanup();
#endif
}

// Вспомогательные функции для работы с RTSP протоколом

// Forward declarations для функций обработки RTP payload
struct NALUnit;
static std::vector<NALUnit> process_rtp_payload_h264_h265(const std::vector<uint8_t>& payload, RTPStream& stream);

// Парсинг RTSP URL
static bool parse_rtsp_url(const std::string& url, RTSPUrl& rtspUrl) {
    // Формат: rtsp://[username:password@]host[:port]/path
    std::regex urlRegex(R"(rtsp://(?:([^:]+):([^@]+)@)?([^:/]+)(?::(\d+))?(/.*)?)");
    std::smatch match;

    if (!std::regex_match(url, match, urlRegex)) {
        return false;
    }

    rtspUrl.protocol = "rtsp";
    rtspUrl.username = match[1].str();
    rtspUrl.password = match[2].str();
    rtspUrl.host = match[3].str();
    rtspUrl.port = match[4].str().empty() ? 554 : std::stoi(match[4].str());
    rtspUrl.path = match[5].str().empty() ? "/" : match[5].str();

    return true;
}

// Создание TCP подключения
static SOCKET create_tcp_socket(const std::string& host, int port, int timeout_ms) {
    // Проверка входных параметров
    if (host.empty() || port <= 0 || port > 65535 || timeout_ms <= 0) {
        return INVALID_SOCKET;
    }

    SOCKET sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock == INVALID_SOCKET) {
        return INVALID_SOCKET;
    }

    // Установка таймаута
#ifdef _WIN32
    DWORD timeout = timeout_ms;
    setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, (const char*)&timeout, sizeof(timeout));
    setsockopt(sock, SOL_SOCKET, SO_SNDTIMEO, (const char*)&timeout, sizeof(timeout));
#else
    struct timeval tv;
    tv.tv_sec = timeout_ms / 1000;
    tv.tv_usec = (timeout_ms % 1000) * 1000;
    setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));
    setsockopt(sock, SOL_SOCKET, SO_SNDTIMEO, &tv, sizeof(tv));
#endif

    // Получение адреса (используем getaddrinfo вместо gethostbyname)
    struct addrinfo hints, *result = nullptr;
    memset(&hints, 0, sizeof(hints));
    hints.ai_family = AF_INET;
    hints.ai_socktype = SOCK_STREAM;
    hints.ai_protocol = IPPROTO_TCP;

    std::ostringstream portStr;
    portStr << port;

    int gaiResult = getaddrinfo(host.c_str(), portStr.str().c_str(), &hints, &result);
    if (gaiResult != 0 || result == nullptr) {
        close(sock);
        return INVALID_SOCKET;
    }

    // Подключение
    if (connect(sock, result->ai_addr, static_cast<int>(result->ai_addrlen)) == SOCKET_ERROR) {
        freeaddrinfo(result);
        close(sock);
        return INVALID_SOCKET;
    }

    freeaddrinfo(result);
    return sock;
}

// Вспомогательная функция для формирования полного RTSP URL
static std::string build_rtsp_url(const RTSPUrl& rtspUrl, const std::string& overridePath = "") {
    std::ostringstream urlStream;
    urlStream << "rtsp://" << rtspUrl.host << ":" << rtspUrl.port;
    
    std::string path = overridePath.empty() ? rtspUrl.path : overridePath;
    if (!path.empty() && path[0] == '/') {
        path = path.substr(1);
    }
    urlStream << "/" << path;
    
    return urlStream.str();
}

// Создание UDP сокета для RTP/RTCP
static SOCKET create_udp_socket(int& port) {
    SOCKET sock = socket(AF_INET, SOCK_DGRAM, 0);
    if (sock == INVALID_SOCKET) {
        return INVALID_SOCKET;
    }

    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_family = AF_INET;
    addr.sin_addr.s_addr = INADDR_ANY;
    addr.sin_port = 0; // Автоматический выбор порта

    if (bind(sock, (struct sockaddr*)&addr, sizeof(addr)) == SOCKET_ERROR) {
        close(sock);
        return INVALID_SOCKET;
    }

    // Получение назначенного порта
    socklen_t len = sizeof(addr);
    getsockname(sock, (struct sockaddr*)&addr, &len);
    port = ntohs(addr.sin_port);

    return sock;
}

// Base64 кодировка
static std::string base64_encode(const std::string& input) {
    static const char base64_chars[] =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";

    std::string encoded;
    int val = 0, valb = -6;

    for (unsigned char c : input) {
        val = (val << 8) + c;
        valb += 8;
        while (valb >= 0) {
            encoded.push_back(base64_chars[(val >> valb) & 0x3F]);
            valb -= 6;
        }
    }

    if (valb > -6) {
        encoded.push_back(base64_chars[((val << 8) >> (valb + 8)) & 0x3F]);
    }

    while (encoded.size() % 4) {
        encoded.push_back('=');
    }

    return encoded;
}

// Генерация базовой HTTP аутентификации
static std::string generate_basic_auth(const std::string& username, const std::string& password) {
    std::string credentials = username + ":" + password;
    std::string encoded = base64_encode(credentials);
    return "Basic " + encoded;
}

// Отправка RTSP запроса
static bool send_rtsp_request(SOCKET sock, const std::string& method, const std::string& url,
                              const std::string& headers, const std::string& body,
                              std::string& response) {
    std::ostringstream request;
    request << method << " " << url << " RTSP/1.0\r\n";
    request << headers;
    if (!body.empty()) {
        request << "Content-Length: " << body.length() << "\r\n";
    }
    request << "\r\n";
    if (!body.empty()) {
        request << body;
    }

    std::string requestStr = request.str();
    int sent = send(sock, requestStr.c_str(), static_cast<int>(requestStr.length()), 0);
    if (sent == SOCKET_ERROR) {
        return false;
    }

    // Чтение ответа
    char buffer[4096];
    response.clear();

    while (true) {
        int received = recv(sock, buffer, sizeof(buffer) - 1, 0);
        if (received <= 0) {
            break;
        }

        buffer[received] = '\0';
        response += buffer;

        // Проверка на конец заголовков
        if (response.find("\r\n\r\n") != std::string::npos) {
            // Проверка наличия тела ответа
            size_t headerEnd = response.find("\r\n\r\n");
            std::string headerPart = response.substr(0, headerEnd);

            // Поиск Content-Length
            size_t contentLengthPos = headerPart.find("Content-Length:");
            if (contentLengthPos != std::string::npos) {
                size_t valueStart = contentLengthPos + 15;
                size_t valueEnd = headerPart.find("\r\n", valueStart);
                int contentLength = std::stoi(headerPart.substr(valueStart, valueEnd - valueStart));

                // Чтение тела ответа
                size_t bodyStart = headerEnd + 4;
                int bodyReceived = static_cast<int>(response.length() - bodyStart);

                while (bodyReceived < contentLength) {
                    int more = recv(sock, buffer, sizeof(buffer) - 1, 0);
                    if (more <= 0) break;
                    buffer[more] = '\0';
                    response += buffer;
                    bodyReceived += more;
                }
            }
            break;
        }
    }

    return !response.empty();
}

// Парсинг SDP ответа
static bool parse_sdp(const std::string& sdp, std::vector<RTPStream>& streams, RTSPClient* client) {
    if (sdp.empty()) {
        return false;
    }

    std::istringstream sdpStream(sdp);
    std::string line;
    RTPStream* currentStream = nullptr;
    std::string sessionControlUrl; // Глобальный control URL из a=control
    std::string sessionName; // s= (session name)
    std::string origin; // o= (origin)
    int sdpVersion = 0; // v= (version)
    bool hasSession = false;

    while (std::getline(sdpStream, line)) {
        // Удаление \r если есть
        if (!line.empty() && line.back() == '\r') {
            line.pop_back();
        }

        if (line.empty()) continue;

        // Парсинг версии SDP (v=)
        if (line[0] == 'v' && line[1] == '=') {
            try {
                sdpVersion = std::stoi(line.substr(2));
            } catch (...) {
                // Не критично, продолжаем
            }
        }

        // Парсинг origin (o=)
        if (line[0] == 'o' && line[1] == '=') {
            origin = line.substr(2);
        }

        // Парсинг session name (s=)
        if (line[0] == 's' && line[1] == '=') {
            sessionName = line.substr(2);
            hasSession = true;
        }

        // Парсинг session-level control URL
        if (line[0] == 'a' && line[1] == '=' && line.substr(2, 7) == "control") {
            size_t colonPos = line.find(':');
            if (colonPos != std::string::npos) {
                sessionControlUrl = line.substr(colonPos + 1);
                // Удаление пробелов
                while (!sessionControlUrl.empty() && sessionControlUrl[0] == ' ') {
                    sessionControlUrl.erase(0, 1);
                }
            }
        }

        if (line[0] == 'm' && line[1] == '=') {
            // Media description: m=video 0 RTP/AVP 96
            std::istringstream mediaStream(line.substr(2));
            std::string mediaType, port, protocol, payloadTypeStr;
            mediaStream >> mediaType >> port >> protocol >> payloadTypeStr;

            if (mediaType == "video" || mediaType == "audio") {
                RTPStream stream;
                stream.type = (mediaType == "video") ? RTSP_STREAM_VIDEO : RTSP_STREAM_AUDIO;

                // Парсинг payload type (может быть несколько через пробел)
                if (!payloadTypeStr.empty()) {
                    size_t firstSpace = payloadTypeStr.find(' ');
                    std::string firstPayloadType = (firstSpace != std::string::npos)
                        ? payloadTypeStr.substr(0, firstSpace)
                        : payloadTypeStr;
                    try {
                        stream.payloadType = std::stoi(firstPayloadType);
                    } catch (...) {
                        stream.payloadType = 96; // По умолчанию
                    }
                } else {
                    stream.payloadType = 96;
                }

                // Установка control URL по умолчанию
                if (!sessionControlUrl.empty()) {
                    stream.controlUrl = sessionControlUrl;
                }

                streams.push_back(stream);
                currentStream = &streams.back();
            }
        } else if (line[0] == 'a' && line[1] == '=' && currentStream) {
            // Attribute
            size_t colonPos = line.find(':');
            if (colonPos == std::string::npos) {
                std::string attr = line.substr(2);
                // Обработка атрибутов без значений
            } else {
                std::string attrName = line.substr(2, colonPos - 2);
                std::string attrValue = line.substr(colonPos + 1);

                if (attrName == "rtpmap") {
                    // a=rtpmap:96 H264/90000 или a=rtpmap:96 H264/90000/1
                    // a=rtpmap:97 H265/90000
                    // a=rtpmap:98 mpeg4-generic/48000/2 (AAC)
                    // a=rtpmap:0 PCMU/8000 (G.711)
                    try {
                        std::istringstream rtpmapStream(attrValue);
                        int pt;
                        std::string codecInfo;
                        rtpmapStream >> pt >> codecInfo;

                        // Проверяем, что payload type совпадает
                        if (pt == currentStream->payloadType) {
                            size_t slashPos = codecInfo.find('/');
                            if (slashPos != std::string::npos) {
                                std::string codecName = codecInfo.substr(0, slashPos);

                                // Нормализация названий кодеков
                                if (codecName == "H264" || codecName == "h264" || codecName == "H.264") {
                                    currentStream->codec = "H264";
                                } else if (codecName == "H265" || codecName == "h265" || codecName == "HEVC" || codecName == "H.265") {
                                    currentStream->codec = "H265";
                                } else if (codecName == "mpeg4-generic" || codecName == "MPEG4-GENERIC") {
                                    currentStream->codec = "AAC";
                                } else if (codecName == "PCMU" || codecName == "pcmu") {
                                    currentStream->codec = "PCMU"; // G.711 μ-law
                                } else if (codecName == "PCMA" || codecName == "pcma") {
                                    currentStream->codec = "PCMA"; // G.711 A-law
                                } else if (codecName == "MPA" || codecName == "mpa") {
                                    currentStream->codec = "MP3";
                                } else {
                                    currentStream->codec = codecName;
                                }

                                std::string rateStr = codecInfo.substr(slashPos + 1);

                                // Может быть clockRate/channels для аудио
                                size_t secondSlash = rateStr.find('/');
                                if (secondSlash != std::string::npos) {
                                    currentStream->clockRate = std::stoi(rateStr.substr(0, secondSlash));
                                    currentStream->channels = std::stoi(rateStr.substr(secondSlash + 1));
                                } else {
                                    currentStream->clockRate = std::stoi(rateStr);
                                    // Для аудио по умолчанию 1 канал, если не указано
                                    if (currentStream->type == RTSP_STREAM_AUDIO && currentStream->channels == 0) {
                                        currentStream->channels = 1;
                                    }
                                }
                            }
                        }
                    } catch (const std::exception& e) {
                        // Логируем ошибку, но продолжаем парсинг
                        // В продакшене здесь должен быть логгер
                    } catch (...) {
                        // Игнорируем другие ошибки
                    }
                } else if (attrName == "control") {
                    // a=control:trackID=0 или a=control:rtsp://server/trackID=0
                    // a=control:track1
                    currentStream->controlUrl = attrValue;

                    // Извлечение track ID
                    size_t trackIdPos = attrValue.find("trackID=");
                    if (trackIdPos != std::string::npos) {
                        currentStream->trackId = attrValue.substr(trackIdPos + 8);
                        // Удаление параметров после точки с запятой или пробела
                        size_t semicolonPos = currentStream->trackId.find(';');
                        if (semicolonPos != std::string::npos) {
                            currentStream->trackId = currentStream->trackId.substr(0, semicolonPos);
                        }
                    } else {
                        // Альтернативный формат: a=control:track1
                        size_t trackPos = attrValue.find("track");
                        if (trackPos != std::string::npos) {
                            size_t start = trackPos + 5;
                            size_t end = attrValue.find_first_of("; \r\n", start);
                            if (end == std::string::npos) end = attrValue.length();
                            currentStream->trackId = attrValue.substr(start, end - start);
                        }
                    }
                } else if (attrName == "fmtp") {
                    // a=fmtp:96 profile-level-id=42001f;sprop-parameter-sets=Z0IAHpWoKA9puAgICBA=,aM48gA==
                    // a=fmtp:97 sprop-vps=...;sprop-sps=...;sprop-pps=... (H.265)
                    // a=fmtp:98 streamtype=5;profile-level-id=1;mode=AAC-hbr;sizeLength=13;indexLength=3;indexDeltaLength=3;config=... (AAC)
                    // Парсинг параметров кодека для H.264/H.265/AAC
                    try {
                        size_t spacePos = attrValue.find(' ');
                        if (spacePos != std::string::npos) {
                            int pt = std::stoi(attrValue.substr(0, spacePos));
                            if (pt == currentStream->payloadType) {
                                std::string params = attrValue.substr(spacePos + 1);
                                currentStream->fmtpParams = params; // Сохраняем все параметры

                                // Парсинг sprop-parameter-sets для H.264 (SPS/PPS)
                                size_t spsPos = params.find("sprop-parameter-sets=");
                                if (spsPos != std::string::npos) {
                                    size_t valueStart = spsPos + 21;
                                    size_t valueEnd = params.find(';', valueStart);
                                    if (valueEnd == std::string::npos) valueEnd = params.length();
                                    std::string spsPps = params.substr(valueStart, valueEnd - valueStart);

                                    // SPS и PPS разделены запятой
                                    size_t commaPos = spsPps.find(',');
                                    if (commaPos != std::string::npos) {
                                        currentStream->sps = spsPps.substr(0, commaPos);
                                        currentStream->pps = spsPps.substr(commaPos + 1);
                                    } else {
                                        currentStream->sps = spsPps;
                                    }
                                }

                                // Парсинг sprop-sps, sprop-pps, sprop-vps для H.265
                                size_t spropSpsPos = params.find("sprop-sps=");
                                if (spropSpsPos != std::string::npos) {
                                    size_t valueStart = spropSpsPos + 10;
                                    size_t valueEnd = params.find(';', valueStart);
                                    if (valueEnd == std::string::npos) valueEnd = params.length();
                                    currentStream->sps = params.substr(valueStart, valueEnd - valueStart);
                                }

                                size_t spropPpsPos = params.find("sprop-pps=");
                                if (spropPpsPos != std::string::npos) {
                                    size_t valueStart = spropPpsPos + 10;
                                    size_t valueEnd = params.find(';', valueStart);
                                    if (valueEnd == std::string::npos) valueEnd = params.length();
                                    currentStream->pps = params.substr(valueStart, valueEnd - valueStart);
                                }

                                size_t spropVpsPos = params.find("sprop-vps=");
                                if (spropVpsPos != std::string::npos) {
                                    size_t valueStart = spropVpsPos + 10;
                                    size_t valueEnd = params.find(';', valueStart);
                                    if (valueEnd == std::string::npos) valueEnd = params.length();
                                    currentStream->vps = params.substr(valueStart, valueEnd - valueStart);
                                }

                                // Парсинг profile-level-id для H.264
                                size_t profilePos = params.find("profile-level-id=");
                                if (profilePos != std::string::npos) {
                                    size_t valueStart = profilePos + 17;
                                    size_t valueEnd = params.find(';', valueStart);
                                    if (valueEnd == std::string::npos) valueEnd = params.length();
                                    currentStream->profileLevelId = params.substr(valueStart, valueEnd - valueStart);
                                }

                                // Парсинг config для AAC
                                size_t configPos = params.find("config=");
                                if (configPos != std::string::npos) {
                                    size_t valueStart = configPos + 7;
                                    size_t valueEnd = params.find(';', valueStart);
                                    if (valueEnd == std::string::npos) valueEnd = params.length();
                                    // Сохраняем hex-строку config для последующей инициализации AAC декодера
                                    currentStream->aacConfig = params.substr(valueStart, valueEnd - valueStart);
                                }
                            }
                        }
                    } catch (const std::exception& e) {
                        // Логируем ошибку, но продолжаем парсинг
                    } catch (...) {
                        // Игнорируем другие ошибки
                    }
                } else if (attrName == "range") {
                    // a=range:npt=0.000-
                    // a=range:npt=0.000-123.456
                    // a=range:clock=19960213T143205Z-;time=19960213T143205Z-
                    // Временной диапазон потока
                    currentStream->range = attrValue;

                    // Парсинг npt (Normal Play Time)
                    size_t nptPos = attrValue.find("npt=");
                    if (nptPos != std::string::npos) {
                        size_t valueStart = nptPos + 4;
                        size_t valueEnd = attrValue.find(';', valueStart);
                        if (valueEnd == std::string::npos) valueEnd = attrValue.length();
                        std::string nptRange = attrValue.substr(valueStart, valueEnd - valueStart);
                        // Можно сохранить для использования в PLAY запросе
                    }
                } else if (attrName == "framerate") {
                    // a=framerate:25.0
                    try {
                        currentStream->fps = static_cast<int>(std::stof(attrValue));
                    } catch (...) {
                        // Игнорируем ошибки парсинга
                    }
                } else if (attrName == "framesize") {
                    // a=framesize:96 1920-1080
                    size_t spacePos = attrValue.find(' ');
                    if (spacePos != std::string::npos) {
                        int pt = std::stoi(attrValue.substr(0, spacePos));
                        if (pt == currentStream->payloadType) {
                            std::string sizeStr = attrValue.substr(spacePos + 1);
                            size_t dashPos = sizeStr.find('-');
                            if (dashPos != std::string::npos) {
                                try {
                                    currentStream->width = std::stoi(sizeStr.substr(0, dashPos));
                                    currentStream->height = std::stoi(sizeStr.substr(dashPos + 1));
                                } catch (...) {
                                    // Игнорируем ошибки парсинга
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Проверка валидности SDP
    if (!hasSession && streams.empty()) {
        return false;
    }

    // Создание RTSPStream для каждого найденного потока
    for (const auto& rtpStream : streams) {
        // Валидация потока перед добавлением
        if (rtpStream.payloadType < 0 || rtpStream.payloadType > 127) {
            continue; // Пропускаем невалидные потоки
        }

        RTSPStream* stream = new RTSPStream();
        stream->type = rtpStream.type;
        stream->codec = rtpStream.codec.empty() ? "unknown" : rtpStream.codec;
        stream->width = rtpStream.width;
        stream->height = rtpStream.height;
        stream->fps = rtpStream.fps;
        client->streams.push_back(stream);
    }

    // Возвращаем true, если найдены хотя бы валидные потоки
    return !streams.empty() && !client->streams.empty();
}

// MD5 hash функция
#ifdef HAVE_OPENSSL
#include <openssl/md5.h>
static std::string md5_hash(const std::string& input) {
    unsigned char digest[MD5_DIGEST_LENGTH];
    MD5((unsigned char*)input.c_str(), input.length(), digest);

    char hex[MD5_DIGEST_LENGTH * 2 + 1];
    for (int i = 0; i < MD5_DIGEST_LENGTH; i++) {
        sprintf(hex + i * 2, "%02x", digest[i]);
    }
    return std::string(hex);
}
#else
// Простая реализация MD5 без OpenSSL (для базовой функциональности)
// ВНИМАНИЕ: Это упрощенная реализация, для продакшена рекомендуется использовать OpenSSL
#include <cstring>
static std::string md5_hash(const std::string& input) {
    // Упрощенная заглушка MD5 - для продакшена нужна полная реализация или OpenSSL
    // Здесь используется простой хеш для совместимости
    uint32_t hash = 0;
    for (size_t i = 0; i < input.length(); i++) {
        hash = ((hash << 5) - hash) + static_cast<unsigned char>(input[i]);
    }

    char hex[33];
    sprintf(hex, "%08x%08x%08x%08x", hash, hash ^ 0x12345678, hash ^ 0x87654321, hash ^ 0xABCDEF00);
    return std::string(hex);
}
#endif

// Парсинг WWW-Authenticate заголовка
static bool parse_www_authenticate(const std::string& header, DigestAuthParams& params) {
    if (header.find("Digest") == std::string::npos) {
        return false;
    }

    // Парсинг параметров: realm="...", nonce="...", algorithm=MD5, qop="auth"
    std::regex realmRegex("realm=\"([^\"]+)\"");
    std::regex nonceRegex("nonce=\"([^\"]+)\"");
    std::regex algorithmRegex("algorithm=([^,\\s]+)");
    std::regex qopRegex("qop=\"([^\"]+)\"");
    std::regex opaqueRegex("opaque=\"([^\"]+)\"");
    std::regex staleRegex("stale=(true|false)");

    std::smatch match;
    if (std::regex_search(header, match, realmRegex)) {
        params.realm = match[1].str();
    }
    if (std::regex_search(header, match, nonceRegex)) {
        params.nonce = match[1].str();
    }
    if (std::regex_search(header, match, algorithmRegex)) {
        params.algorithm = match[1].str();
    } else {
        params.algorithm = "MD5"; // По умолчанию
    }
    if (std::regex_search(header, match, qopRegex)) {
        params.qop = match[1].str();
    }
    if (std::regex_search(header, match, opaqueRegex)) {
        params.opaque = match[1].str();
    }
    if (std::regex_search(header, match, staleRegex)) {
        params.stale = (match[1].str() == "true");
    }

    return !params.realm.empty() && !params.nonce.empty();
}

// Генерация Digest Authentication заголовка
static std::string generate_digest_auth(
    const std::string& method,
    const std::string& uri,
    const std::string& username,
    const std::string& password,
    const DigestAuthParams& params,
    int nc = 1
) {
    // Генерация cnonce
    std::string cnonce;
    for (int i = 0; i < 16; i++) {
        cnonce += "0123456789abcdef"[rand() % 16];
    }

    // HA1 = MD5(username:realm:password)
    std::string ha1 = md5_hash(username + ":" + params.realm + ":" + password);

    // Если algorithm = "MD5-sess", то HA1 = MD5(HA1:nonce:cnonce)
    if (params.algorithm == "MD5-sess") {
        ha1 = md5_hash(ha1 + ":" + params.nonce + ":" + cnonce);
    }

    // HA2 = MD5(method:uri) или MD5(method:uri:MD5(entity-body)) для auth-int
    std::string ha2;
    if (params.qop == "auth-int") {
        // Для auth-int нужно добавить MD5(entity-body)
        // В RTSP обычно нет entity-body, поэтому просто method:uri
        ha2 = md5_hash(method + ":" + uri + ":");
    } else {
        ha2 = md5_hash(method + ":" + uri);
    }

    // Response = MD5(HA1:nonce:nc:cnonce:qop:HA2) или MD5(HA1:nonce:HA2) без qop
    std::string response;
    if (!params.qop.empty()) {
        std::string ncStr = std::to_string(nc);
        while (ncStr.length() < 8) ncStr = "0" + ncStr;
        response = md5_hash(ha1 + ":" + params.nonce + ":" + ncStr + ":" + cnonce + ":" + params.qop + ":" + ha2);
    } else {
        response = md5_hash(ha1 + ":" + params.nonce + ":" + ha2);
    }

    // Формирование заголовка
    std::ostringstream auth;
    auth << "Digest username=\"" << username << "\", "
         << "realm=\"" << params.realm << "\", "
         << "nonce=\"" << params.nonce << "\", "
         << "uri=\"" << uri << "\", "
         << "response=\"" << response << "\"";

    if (!params.qop.empty()) {
        std::string ncStr = std::to_string(nc);
        while (ncStr.length() < 8) ncStr = "0" + ncStr;
        auth << ", qop=\"" << params.qop << "\", "
             << "nc=" << ncStr << ", "
             << "cnonce=\"" << cnonce << "\"";
    }

    if (!params.algorithm.empty()) {
        auth << ", algorithm=" << params.algorithm;
    }

    if (!params.opaque.empty()) {
        auth << ", opaque=\"" << params.opaque << "\"";
    }

    return auth.str();
}

// Вспомогательная функция для извлечения значения заголовка
static std::string extract_header_value(const std::string& response, const std::string& headerName, bool caseSensitive = false) {
    std::string lowerResponse = response;
    std::string lowerHeaderName = headerName;
    if (!caseSensitive) {
        std::transform(lowerResponse.begin(), lowerResponse.end(), lowerResponse.begin(), ::tolower);
        std::transform(lowerHeaderName.begin(), lowerHeaderName.end(), lowerHeaderName.begin(), ::tolower);
    }

    size_t headerPos = lowerResponse.find(lowerHeaderName + ":");
    if (headerPos == std::string::npos) {
        return "";
    }

    size_t valueStart = headerPos + headerName.length() + 1;
    while (valueStart < response.length() && (response[valueStart] == ' ' || response[valueStart] == '\t')) {
        valueStart++;
    }

    size_t valueEnd = response.find("\r\n", valueStart);
    if (valueEnd == std::string::npos) {
        valueEnd = response.length();
    }

    std::string value = response.substr(valueStart, valueEnd - valueStart);
    // Удаление пробелов в конце
    while (!value.empty() && (value.back() == ' ' || value.back() == '\t')) {
        value.pop_back();
    }

    return value;
}

// Парсинг RTSP ответа для извлечения всех заголовков
static bool parse_rtsp_response(const std::string& response, RTSPResponseHeaders& headers) {
    if (response.empty()) {
        return false;
    }

    std::istringstream responseStream(response);
    std::string statusLine;
    std::getline(responseStream, statusLine);

    // Удаление \r если есть
    if (!statusLine.empty() && statusLine.back() == '\r') {
        statusLine.pop_back();
    }

    // RTSP/1.0 200 OK
    size_t space1 = statusLine.find(' ');
    if (space1 == std::string::npos) return false;

    size_t space2 = statusLine.find(' ', space1 + 1);
    if (space2 == std::string::npos) {
        // Нет текста статуса
        try {
            headers.statusCode = std::stoi(statusLine.substr(space1 + 1));
            headers.statusText = "";
        } catch (...) {
            return false;
        }
    } else {
        try {
            headers.statusCode = std::stoi(statusLine.substr(space1 + 1, space2 - space1 - 1));
            headers.statusText = statusLine.substr(space2 + 1);
        } catch (...) {
            return false;
        }
    }

    // Извлечение всех стандартных заголовков
    headers.sessionId = extract_header_value(response, "Session");
    if (!headers.sessionId.empty()) {
        // Удаление параметров после точки с запятой
        size_t semicolonPos = headers.sessionId.find(';');
        if (semicolonPos != std::string::npos) {
            headers.sessionId = headers.sessionId.substr(0, semicolonPos);
        }
    }

    headers.wwwAuthenticate = extract_header_value(response, "WWW-Authenticate");
    if (headers.wwwAuthenticate.empty()) {
        headers.wwwAuthenticate = extract_header_value(response, "www-authenticate");
    }

    headers.transport = extract_header_value(response, "Transport");
    headers.range = extract_header_value(response, "Range");
    headers.rtpInfo = extract_header_value(response, "RTP-Info");
    headers.contentType = extract_header_value(response, "Content-Type");
    headers.contentLength = extract_header_value(response, "Content-Length");
    headers.cseq = extract_header_value(response, "CSeq");
    headers.server = extract_header_value(response, "Server");
    headers.publicHeader = extract_header_value(response, "Public");
    headers.location = extract_header_value(response, "Location");
    headers.retryAfter = extract_header_value(response, "Retry-After");

    // Извлечение сообщения об ошибке из тела ответа (если есть)
    if (headers.statusCode >= 400) {
        size_t bodyStart = response.find("\r\n\r\n");
        if (bodyStart != std::string::npos && bodyStart + 4 < response.length()) {
            headers.errorMessage = response.substr(bodyStart + 4);
            // Ограничиваем длину сообщения
            if (headers.errorMessage.length() > 256) {
                headers.errorMessage = headers.errorMessage.substr(0, 256);
            }
        }
    }

    return true;
}

// Перегрузка для обратной совместимости
static bool parse_rtsp_response(const std::string& response, int& statusCode, std::string& sessionId, std::string& wwwAuthenticate) {
    RTSPResponseHeaders headers;
    if (!parse_rtsp_response(response, headers)) {
        return false;
    }

    statusCode = headers.statusCode;
    sessionId = headers.sessionId;
    wwwAuthenticate = headers.wwwAuthenticate;

    return true;
}

// Структура для RTP пакета
struct RTPPacket {
    uint8_t version;          // 2 bits - версия RTP (обычно 2)
    bool padding;             // 1 bit - есть ли padding
    bool extension;           // 1 bit - есть ли extension header
    uint8_t csrcCount;        // 4 bits - количество CSRC идентификаторов
    bool marker;              // 1 bit - marker bit
    uint8_t payloadType;      // 7 bits - тип payload
    uint16_t sequence;        // 16 bits - sequence number
    uint32_t timestamp;       // 32 bits - timestamp
    uint32_t ssrc;            // 32 bits - SSRC (Synchronization Source)
    std::vector<uint32_t> csrc; // CSRC список
    uint16_t extensionId;     // Extension header ID (если есть)
    uint16_t extensionLength; // Extension header length в 32-bit словах
    std::vector<uint8_t> extensionData; // Extension header данные
    std::vector<uint8_t> payload; // Payload данные
    uint8_t paddingSize;     // Размер padding (если есть)

    RTPPacket() : version(0), padding(false), extension(false), csrcCount(0),
                  marker(false), payloadType(0), sequence(0), timestamp(0),
                  ssrc(0), extensionId(0), extensionLength(0), paddingSize(0) {}
};

// Функция парсинга RTP пакета
static bool parse_rtp_packet(const uint8_t* data, int size, RTPPacket& packet) {
    if (size < 12) return false; // Минимальный размер RTP заголовка

    // Парсинг первого байта
    packet.version = (data[0] >> 6) & 0x3;
    if (packet.version != 2) return false; // RTP версия должна быть 2

    packet.padding = (data[0] >> 5) & 0x1;
    packet.extension = (data[0] >> 4) & 0x1;
    packet.csrcCount = data[0] & 0xF;

    // Парсинг второго байта
    packet.marker = (data[1] >> 7) & 0x1;
    packet.payloadType = data[1] & 0x7F;

    // Парсинг sequence number
    packet.sequence = (data[2] << 8) | data[3];

    // Парсинг timestamp
    packet.timestamp = (data[4] << 24) | (data[5] << 16) | (data[6] << 8) | data[7];

    // Парсинг SSRC
    packet.ssrc = (data[8] << 24) | (data[9] << 16) | (data[10] << 8) | data[11];

    int offset = 12; // Базовый размер заголовка

    // Парсинг CSRC списка
    if (packet.csrcCount > 0) {
        if (size < offset + packet.csrcCount * 4) return false;
        packet.csrc.resize(packet.csrcCount);
        for (uint8_t i = 0; i < packet.csrcCount; i++) {
            packet.csrc[i] = (data[offset] << 24) | (data[offset + 1] << 16) |
                            (data[offset + 2] << 8) | data[offset + 3];
            offset += 4;
        }
    }

    // Парсинг extension header
    if (packet.extension) {
        if (size < offset + 4) return false;
        packet.extensionId = (data[offset] << 8) | data[offset + 1];
        packet.extensionLength = (data[offset + 2] << 8) | data[offset + 3];
        offset += 4;

        int extensionDataSize = packet.extensionLength * 4;
        if (size < offset + extensionDataSize) return false;
        packet.extensionData.resize(extensionDataSize);
        memcpy(packet.extensionData.data(), data + offset, extensionDataSize);
        offset += extensionDataSize;
    }

    // Определение размера payload
    int payloadSize = size - offset;

    // Обработка padding
    if (packet.padding && payloadSize > 0) {
        packet.paddingSize = data[size - 1];
        if (packet.paddingSize > 0 && packet.paddingSize <= payloadSize) {
            payloadSize -= packet.paddingSize;
        } else {
            packet.paddingSize = 0; // Некорректный padding, игнорируем
        }
    }

    if (payloadSize <= 0) return false;

    // Копирование payload
    packet.payload.resize(payloadSize);
    memcpy(packet.payload.data(), data + offset, payloadSize);

    return true;
}

// RTCP типы пакетов
#define RTCP_SR  200  // Sender Report
#define RTCP_RR  201  // Receiver Report
#define RTCP_SDES 202 // Source Description
#define RTCP_BYE 203  // Goodbye
#define RTCP_APP 204  // Application-defined

// Структура для RTCP заголовка
struct RTCPHeader {
    uint8_t version;      // 2 bits
    bool padding;         // 1 bit
    uint8_t rc;           // 5 bits - Reception Report Count
    uint8_t packetType;   // 8 bits
    uint16_t length;      // 16 bits - длина в 32-bit словах минус 1
    uint32_t ssrc;        // 32 bits
};

// Парсинг RTCP заголовка
static bool parse_rtcp_header(const uint8_t* data, int size, RTCPHeader& header) {
    if (size < 8) return false; // Минимальный размер RTCP заголовка

    header.version = (data[0] >> 6) & 0x3;
    header.padding = (data[0] >> 5) & 0x1;
    header.rc = data[0] & 0x1F;
    header.packetType = data[1];
    header.length = (data[2] << 8) | data[3];
    header.ssrc = (data[4] << 24) | (data[5] << 16) | (data[6] << 8) | data[7];

    return true;
}

// Обработка RTCP Sender Report (SR)
static void process_rtcp_sr(const uint8_t* data, int size, RTPStream& stream) {
    if (size < 28) return; // Минимальный размер SR (8 байт заголовок + 20 байт данных)

    // NTP Timestamp (64 bits)
    uint32_t ntpSeconds = (data[8] << 24) | (data[9] << 16) | (data[10] << 8) | data[11];
    uint32_t ntpFraction = (data[12] << 24) | (data[13] << 16) | (data[14] << 8) | data[15];
    stream.lastSrNtpTimestamp = ((uint64_t)ntpSeconds << 32) | ntpFraction;

    // RTP Timestamp (32 bits)
    stream.lastSrTimestamp = (data[16] << 24) | (data[17] << 16) | (data[18] << 8) | data[19];

    // Sender's packet count (32 bits)
    uint32_t packetCount = (data[20] << 24) | (data[21] << 16) | (data[22] << 8) | data[23];

    // Sender's octet count (32 bits)
    uint32_t octetCount = (data[24] << 24) | (data[25] << 16) | (data[26] << 8) | data[27];

    // Можно использовать для синхронизации и статистики
}

// Обработка RTCP Receiver Report (RR)
static void process_rtcp_rr(const uint8_t* data, int size, RTPStream& stream) {
    if (size < 8) return; // Минимальный размер RR (только заголовок)

    RTCPHeader header;
    if (!parse_rtcp_header(data, size, header)) return;

    // RR содержит отчеты о качестве для каждого источника
    // Каждый отчет занимает 24 байта
    int offset = 8; // После заголовка
    for (uint8_t i = 0; i < header.rc && offset + 24 <= size; i++) {
        // SSRC источника (32 bits)
        uint32_t sourceSsrc = (data[offset] << 24) | (data[offset + 1] << 16) |
                              (data[offset + 2] << 8) | data[offset + 3];

        // Fraction lost (8 bits)
        uint8_t fractionLost = data[offset + 4];

        // Cumulative number of packets lost (24 bits)
        uint32_t packetsLost = ((data[offset + 5] << 16) | (data[offset + 6] << 8) | data[offset + 7]) & 0xFFFFFF;
        if (data[offset + 5] & 0x80) {
            // Отрицательное число (знаковый бит)
            packetsLost |= 0xFF000000;
        }

        // Extended highest sequence number received (32 bits)
        uint32_t highestSeq = (data[offset + 8] << 24) | (data[offset + 9] << 16) |
                             (data[offset + 10] << 8) | data[offset + 11];

        // Interarrival jitter (32 bits)
        uint32_t jitter = (data[offset + 12] << 24) | (data[offset + 13] << 16) |
                         (data[offset + 14] << 8) | data[offset + 15];
        stream.jitter = jitter;

        // Last SR timestamp (32 bits)
        uint32_t lastSr = (data[offset + 16] << 24) | (data[offset + 17] << 16) |
                         (data[offset + 18] << 8) | data[offset + 19];

        // Delay since last SR (32 bits)
        uint32_t delaySinceLastSr = (data[offset + 20] << 24) | (data[offset + 21] << 16) |
                                   (data[offset + 22] << 8) | data[offset + 23];

        // Обновляем статистику потерь пакетов
        stream.packetsLost = packetsLost;
        stream.packetsExpected = highestSeq;

        offset += 24;
    }
}

// Обработка RTCP Source Description (SDES)
static void process_rtcp_sdes(const uint8_t* data, int size, RTPStream& stream) {
    if (size < 8) return;

    RTCPHeader header;
    if (!parse_rtcp_header(data, size, header)) return;

    // SDES содержит описания источников
    // Каждый источник начинается с SSRC (4 байта), затем идут SDES items
    int offset = 8;
    while (offset < size) {
        if (offset + 4 > size) break;

        uint32_t ssrc = (data[offset] << 24) | (data[offset + 1] << 16) |
                       (data[offset + 2] << 8) | data[offset + 3];
        offset += 4;

        // Парсинг SDES items
        while (offset < size) {
            if (data[offset] == 0) {
                // Конец списка items, выравнивание до 4 байт
                offset = ((offset + 3) / 4) * 4;
                break;
            }

            uint8_t type = data[offset];
            uint8_t length = data[offset + 1];

            if (offset + 2 + length > size) break;

            // Можно извлечь информацию (CNAME, NAME, EMAIL и т.д.)
            // Для базовой реализации просто пропускаем
            offset += 2 + length;
        }
    }
}

// Обработка RTCP BYE
static void process_rtcp_bye(const uint8_t* data, int size, RTPStream& stream) {
    if (size < 8) return;

    RTCPHeader header;
    if (!parse_rtcp_header(data, size, header)) return;

    // BYE указывает на завершение сессии
    // Можно использовать для обнаружения разрыва соединения
}

// Создание RTCP Receiver Report (RR) пакета
static std::vector<uint8_t> create_rtcp_rr_packet(uint32_t receiverSsrc, uint32_t senderSsrc,
                                                   uint32_t packetsReceived, uint32_t packetsLost,
                                                   uint32_t highestSeq, uint32_t jitter,
                                                   uint32_t lastSr, uint32_t delaySinceLastSr) {
    std::vector<uint8_t> packet;
    packet.resize(32); // Минимальный размер RR (8 байт заголовок + 24 байта отчета)

    // RTCP заголовок
    packet[0] = 0x80; // Version=2, Padding=0, RC=1 (один отчет)
    packet[1] = RTCP_RR; // Packet type = Receiver Report
    packet[2] = 0x00; // Length (в 32-bit словах минус 1) = 7 (8 байт заголовок + 24 байта отчета = 32 байта = 8 слов, минус 1 = 7)
    packet[3] = 0x07;

    // SSRC отправителя (наш SSRC - receiver SSRC)
    packet[4] = (receiverSsrc >> 24) & 0xFF;
    packet[5] = (receiverSsrc >> 16) & 0xFF;
    packet[6] = (receiverSsrc >> 8) & 0xFF;
    packet[7] = receiverSsrc & 0xFF;

    // SSRC источника (32 bits)
    packet[8] = (senderSsrc >> 24) & 0xFF;
    packet[9] = (senderSsrc >> 16) & 0xFF;
    packet[10] = (senderSsrc >> 8) & 0xFF;
    packet[11] = senderSsrc & 0xFF;

    // Fraction lost (8 bits)
    uint8_t fractionLost = 0;
    uint32_t totalPackets = packetsReceived + packetsLost;
    if (totalPackets > 0) {
        fractionLost = (packetsLost * 256) / totalPackets;
    }
    packet[12] = fractionLost;

    // Cumulative number of packets lost (24 bits, знаковое)
    int32_t lost = static_cast<int32_t>(packetsLost);
    packet[13] = (lost >> 16) & 0xFF;
    packet[14] = (lost >> 8) & 0xFF;
    packet[15] = lost & 0xFF;

    // Extended highest sequence number received (32 bits)
    packet[16] = (highestSeq >> 24) & 0xFF;
    packet[17] = (highestSeq >> 16) & 0xFF;
    packet[18] = (highestSeq >> 8) & 0xFF;
    packet[19] = highestSeq & 0xFF;

    // Interarrival jitter (32 bits)
    packet[20] = (jitter >> 24) & 0xFF;
    packet[21] = (jitter >> 16) & 0xFF;
    packet[22] = (jitter >> 8) & 0xFF;
    packet[23] = jitter & 0xFF;

    // Last SR timestamp (32 bits)
    packet[24] = (lastSr >> 24) & 0xFF;
    packet[25] = (lastSr >> 16) & 0xFF;
    packet[26] = (lastSr >> 8) & 0xFF;
    packet[27] = lastSr & 0xFF;

    // Delay since last SR (32 bits)
    packet[28] = (delaySinceLastSr >> 24) & 0xFF;
    packet[29] = (delaySinceLastSr >> 16) & 0xFF;
    packet[30] = (delaySinceLastSr >> 8) & 0xFF;
    packet[31] = delaySinceLastSr & 0xFF;

    return packet;
}

// Отправка RTCP Receiver Report
static void send_rtcp_rr(RTPStream& stream, RTSPClient* client) {
    // Проверка входных параметров
    if (!client) {
        return; // Некорректный указатель на клиент
    }

    if (stream.rtcpSocket == INVALID_SOCKET && !stream.useTcp) {
        return; // Нет RTCP сокета для UDP
    }

    // Вычисляем задержку с последнего SR (в 1/65536 секунды)
    uint32_t delaySinceLastSr = 0;
    if (stream.lastSrTimestamp > 0) {
        // Здесь должна быть текущая NTP timestamp минус lastSrNtpTimestamp
        // Для упрощения используем фиксированное значение или вычисляем из системного времени
        // В реальной реализации нужно использовать NTP timestamp
    }

    // Создаем RR пакет
    std::vector<uint8_t> rrPacket = create_rtcp_rr_packet(
        stream.rtpSSRC,      // Наш SSRC
        stream.rtpSSRC,      // SSRC источника (тот же)
        stream.packetsReceived,
        stream.packetsLost,
        stream.packetsExpected,
        stream.jitter,
        stream.lastSrTimestamp,
        delaySinceLastSr
    );

    if (stream.useTcp) {
        // Для TCP транспорта отправляем через interleaved binary data
        if (client->rtspSocket != INVALID_SOCKET) {
            uint8_t header[4];
            header[0] = 0x24; // Magic byte '$'
            header[1] = stream.rtcpChannel;
            header[2] = (rrPacket.size() >> 8) & 0xFF;
            header[3] = rrPacket.size() & 0xFF;

            send(client->rtspSocket, (const char*)header, 4, 0);
            send(client->rtspSocket, (const char*)rrPacket.data(), static_cast<int>(rrPacket.size()), 0);
        }
    } else {
        // Для UDP транспорта отправляем на серверный RTCP порт
        if (stream.rtcpSocket != INVALID_SOCKET && stream.serverRtcpPort > 0) {
            struct sockaddr_in serverAddr;
            memset(&serverAddr, 0, sizeof(serverAddr));
            serverAddr.sin_family = AF_INET;
            serverAddr.sin_port = htons(stream.serverRtcpPort);

            // Получаем адрес сервера из RTSP URL с использованием getaddrinfo
            struct addrinfo hints, *result = nullptr;
            memset(&hints, 0, sizeof(hints));
            hints.ai_family = AF_INET;
            hints.ai_socktype = SOCK_DGRAM;

            std::ostringstream portStr;
            portStr << stream.serverRtcpPort;

            int gaiResult = getaddrinfo(client->rtspUrl.host.c_str(), portStr.str().c_str(), &hints, &result);
            if (gaiResult == 0 && result) {
                sendto(stream.rtcpSocket, (const char*)rrPacket.data(), static_cast<int>(rrPacket.size()), 0,
                       result->ai_addr, static_cast<socklen_t>(result->ai_addrlen));
                freeaddrinfo(result);
            }
        }
    }
}

// Обработка RTCP пакета
static void process_rtcp_packet(const uint8_t* data, int size, RTPStream& stream, RTSPClient* client) {
    // Проверка входных параметров
    if (!data || size < 8 || !client) {
        return; // Некорректные параметры или минимальный размер RTCP заголовка
    }

    RTCPHeader header;
    if (!parse_rtcp_header(data, size, header)) return;

    // Проверка версии (должна быть 2)
    if (header.version != 2) return;

    // Вычисляем реальную длину пакета (length в 32-bit словах минус 1)
    int packetLength = (header.length + 1) * 4;
    if (packetLength > size) {
        packetLength = size; // Используем доступный размер
    }

    // Обработка в зависимости от типа пакета
    switch (header.packetType) {
        case RTCP_SR:
            process_rtcp_sr(data, packetLength, stream);
            // После получения SR отправляем RR
            send_rtcp_rr(stream, client);
            break;
        case RTCP_RR:
            process_rtcp_rr(data, packetLength, stream);
            break;
        case RTCP_SDES:
            process_rtcp_sdes(data, packetLength, stream);
            break;
        case RTCP_BYE:
            process_rtcp_bye(data, packetLength, stream);
            // Можно уведомить клиент о завершении сессии
            if (client && client->statusCallback) {
                try {
                    client->statusCallback(RTSP_STATUS_ERROR, "RTCP BYE received", client->statusUserData);
                } catch (...) {
                    // Защита от исключений в callback'е
                }
            }
            break;
        case RTCP_APP:
            // Application-defined пакеты - пропускаем
            break;
        default:
            // Неизвестный тип пакета
            break;
    }

    // RTCP пакеты могут быть составными (compound packets)
    // Если есть еще данные, обрабатываем следующий пакет
    if (packetLength < size) {
        process_rtcp_packet(data + packetLength, size - packetLength, stream, client);
    }
}

// Обработка RTP пакета
// ВНИМАНИЕ: Эта функция НЕ должна вызываться с заблокированным мьютексом client->mutex,
// так как она вызывает callback'и, которые могут попытаться заблокировать тот же мьютекс
static void process_rtp_packet(const uint8_t* data, int size, RTPStream& stream, RTSPClient* client) {
    // Проверка входных параметров
    if (!data || size <= 0 || !client) {
        return; // Некорректные параметры
    }

    // Парсинг RTP пакета с использованием улучшенной структуры
    RTPPacket packet;
    if (!parse_rtp_packet(data, size, packet)) {
        return; // Некорректный пакет
    }

    // Проверка версии RTP
    if (packet.version != 2) {
        return; // Неподдерживаемая версия RTP
    }

    // Проверка payload type (должен соответствовать потоку)
    if (packet.payloadType != stream.payloadType) {
        // Payload type не совпадает, но это может быть нормально для некоторых кодеков
        // Можно обновить payload type потока
        stream.payloadType = packet.payloadType;
    }

    // Обновление статистики RTP
    stream.packetsReceived++;

    // Улучшенная проверка последовательности для обнаружения потерь
    if (stream.rtpSequence != 0) {
        uint16_t expectedNext = (stream.rtpSequence + 1) % 65536;
        if (packet.sequence != expectedNext && packet.sequence != stream.rtpSequence) {
            // Обнаружена потеря пакетов
            uint16_t lost;
            if (packet.sequence > stream.rtpSequence) {
                // Нормальный случай: новый sequence больше предыдущего
                lost = packet.sequence - stream.rtpSequence - 1;
            } else {
                // Переполнение sequence number (wrap around)
                lost = (65536 - stream.rtpSequence) + packet.sequence - 1;
            }

            if (lost > 0 && lost < 1000) { // Разумный лимит для предотвращения ошибок
                stream.packetsLost += lost;
            }
        }
    }

    // Обновление информации о потоке
    stream.rtpSequence = packet.sequence;
    stream.rtpSSRC = packet.ssrc;
    stream.rtpTimestamp = packet.timestamp;

    // Обработка marker bit (обычно указывает на конец фрейма)
    // Для видео: marker = 1 означает конец кадра
    // Для аудио: marker = 1 может означать конец аудио блока

    // Обработка extension header (если есть)
    // Extension header может содержать дополнительную информацию о синхронизации
    // или другие метаданные

    // Обработка payload в зависимости от типа кодека
    if (packet.payload.empty()) {
        return; // Нет payload данных
    }

    // Для видео кодеков (H.264, H.265) выполняем дефрагментацию
    if (stream.type == RTSP_STREAM_VIDEO &&
        (stream.codec == "H264" || stream.codec == "H.264" ||
         stream.codec == "H265" || stream.codec == "H.265" || stream.codec == "HEVC")) {

        std::vector<NALUnit> nals = process_rtp_payload_h264_h265(packet.payload, stream);

        // Обработка каждого NAL unit
        for (const auto& nal : nals) {
            if (nal.data.empty() || nal.data.size() > 10 * 1024 * 1024) continue; // Пропускаем пустые или слишком большие NAL units (макс 10MB)

            // Проверка валидности размера перед выделением памяти
            size_t nalSize = nal.data.size();
            if (nalSize == 0 || nalSize > 10 * 1024 * 1024) {
                continue; // Защита от переполнения
            }

            RTSPFrame* frame = nullptr;
            try {
                frame = new RTSPFrame();
                frame->data = new (std::nothrow) uint8_t[nalSize];
                if (!frame->data) {
                    delete frame;
                    continue; // Не удалось выделить память
                }
                memcpy(frame->data, nal.data.data(), nalSize);
                frame->size = static_cast<int>(nalSize);
                frame->timestamp = packet.timestamp;
                frame->type = stream.type;
                frame->width = stream.width;
                frame->height = stream.height;
            } catch (const std::bad_alloc&) {
                if (frame) {
                    delete frame;
                }
                continue; // Не удалось выделить память
            }

            // Копируем указатели на callback'и для безопасного вызова вне блокировки
            RTSPFrameCallback videoCallback = nullptr;
            void* videoUserData = nullptr;
            {
                std::lock_guard<std::mutex> lock(client->mutex);
                videoCallback = client->videoCallback;
                videoUserData = client->videoUserData;
            }

            if (videoCallback && frame) {
                try {
                    videoCallback(frame, videoUserData);
                } catch (...) {
                    // Защита от исключений в callback'е
                    delete[] frame->data;
                    delete frame;
                }
            } else {
                if (frame) {
                    delete[] frame->data;
                    delete frame;
                }
            }
        }
    } else {
        // Аудио и прочие кодеки

#ifdef ENABLE_FFMPEG
        if (stream.type == RTSP_STREAM_AUDIO &&
            (stream.codec == "AAC" || stream.codec == "PCMU" || stream.codec == "PCMA")) {
            // Инициализируем декодер при первом использовании
            if (stream.codec == "AAC" && !stream.aacDecoder) {
                RTPStream& nonConstStream = stream;
                nonConstStream.aacDecoder = init_aac_decoder_for_stream(stream);
            } else if ((stream.codec == "PCMU" || stream.codec == "PCMA") && !stream.g711Decoder) {
                RTPStream& nonConstStream = stream;
                nonConstStream.g711Decoder = init_g711_decoder_for_stream(stream);
            }

            DecodedAudioFrame decoded;
            bool decodedOk = false;

            if (stream.codec == "AAC" && stream.aacDecoder) {
                decodedOk = decode_aac_packet(stream.aacDecoder,
                                              packet.payload.data(),
                                              static_cast<int>(packet.payload.size()),
                                              decoded);
            } else if ((stream.codec == "PCMU" || stream.codec == "PCMA") && stream.g711Decoder) {
                decodedOk = decode_g711_packet(stream.g711Decoder,
                                               packet.payload.data(),
                                               static_cast<int>(packet.payload.size()),
                                               decoded);
            }

            if (decodedOk && !decoded.samples.empty()) {
                // Обновляем AV синхронизацию для аудио
                client->avSync.audioTimestamp = packet.timestamp;
                client->avSync.audioClockMs =
                    (packet.timestamp * 1000LL) / (stream.clockRate > 0 ? stream.clockRate : 90000);
                if (!client->avSync.syncInitialized && client->avSync.videoTimestamp > 0) {
                    client->avSync.clockOffsetMs =
                        client->avSync.audioClockMs - client->avSync.videoClockMs;
                    client->avSync.syncInitialized = true;
                }

                size_t bytes = decoded.samples.size() * sizeof(int16_t);
                if (bytes == 0 || bytes > 10 * 1024 * 1024) {
                    return;
                }

                RTSPFrame* frame = nullptr;
                try {
                    frame = new RTSPFrame();
                    frame->data = new (std::nothrow) uint8_t[bytes];
                    if (!frame->data) {
                        delete frame;
                        return;
                    }
                    memcpy(frame->data, decoded.samples.data(), bytes);
                    frame->size = static_cast<int>(bytes);
                    frame->timestamp = packet.timestamp;
                    frame->type = RTSP_STREAM_AUDIO;
                    frame->width = 0;
                    frame->height = 0;
                } catch (const std::bad_alloc&) {
                    if (frame) {
                        delete frame;
                    }
                    return;
                }

                RTSPFrameCallback audioCallback = nullptr;
                void* audioUserData = nullptr;
                {
                    std::lock_guard<std::mutex> lock(client->mutex);
                    audioCallback = client->audioCallback;
                    audioUserData = client->audioUserData;
                }

                if (audioCallback && frame) {
                    try {
                        audioCallback(frame, audioUserData);
                    } catch (...) {
                        delete[] frame->data;
                        delete frame;
                    }
                } else {
                    delete[] frame->data;
                    delete frame;
                }

                return;
            }
            // Если декодирование не удалось — fallback к исходному поведению ниже
        }
#endif // ENABLE_FFMPEG

        // Для других кодеков (аудио без декодирования, другие видео) передаем payload как есть
        if (packet.payload.empty() || packet.payload.size() > 10 * 1024 * 1024) {
            return; // Пропускаем пустые или слишком большие payload (макс 10MB)
        }

        // Проверка валидности размера перед выделением памяти
        size_t payloadSize = packet.payload.size();
        if (payloadSize == 0 || payloadSize > 10 * 1024 * 1024) {
            return; // Защита от переполнения
        }

        RTSPFrame* frame = nullptr;
        try {
            frame = new RTSPFrame();
            frame->data = new (std::nothrow) uint8_t[payloadSize];
            if (!frame->data) {
                delete frame;
                return; // Не удалось выделить память
            }
            memcpy(frame->data, packet.payload.data(), payloadSize);
            frame->size = static_cast<int>(payloadSize);
            frame->timestamp = packet.timestamp;
            frame->type = stream.type;
            frame->width = stream.width;
            frame->height = stream.height;
        } catch (const std::bad_alloc&) {
            if (frame) {
                delete frame;
            }
            return; // Не удалось выделить память
        }

        // Копируем указатели на callback'и для безопасного вызова вне блокировки
        RTSPFrameCallback videoCallback = nullptr;
        RTSPFrameCallback audioCallback = nullptr;
        void* videoUserData = nullptr;
        void* audioUserData = nullptr;
        {
            std::lock_guard<std::mutex> lock(client->mutex);
            videoCallback = client->videoCallback;
            audioCallback = client->audioCallback;
            videoUserData = client->videoUserData;
            audioUserData = client->audioUserData;
        }

        if (stream.type == RTSP_STREAM_VIDEO && videoCallback && frame) {
            try {
                videoCallback(frame, videoUserData);
            } catch (...) {
                // Защита от исключений в callback'е
                delete[] frame->data;
                delete frame;
            }
        } else if (stream.type == RTSP_STREAM_AUDIO && audioCallback && frame) {
            try {
                audioCallback(frame, audioUserData);
            } catch (...) {
                // Защита от исключений в callback'е
                delete[] frame->data;
                delete frame;
            }
        } else {
            if (frame) {
                delete[] frame->data;
                delete frame;
            }
        }
    }
}

// Чтение interleaved binary data из RTSP сокета (для TCP транспорта)
// Формат: '$' (1 байт) + channel (1 байт) + length (2 байта, big-endian) + data
static bool read_interleaved_data(SOCKET sock, uint8_t& channel, uint16_t& length, std::vector<uint8_t>& data) {
    // Проверка валидности сокета
    if (sock == INVALID_SOCKET) {
        return false;
    }

    uint8_t header[4];
    int totalReceived = 0;

    // Читаем заголовок (4 байта) с циклом для гарантии получения всех данных
    while (totalReceived < 4) {
        int received = recv(sock, (char*)header + totalReceived, 4 - totalReceived, 0);
        if (received <= 0) {
            // Ошибка или соединение закрыто
            return false;
        }
        totalReceived += received;
    }

    if (header[0] != '$') {
        // Не interleaved данные, возможно RTSP ответ
        return false;
    }

    channel = header[1];
    length = (header[2] << 8) | header[3];

    // Защита от переполнения: максимальный размер пакета ограничиваем до 1MB
    // (стандарт RTP обычно использует пакеты до 64KB, но некоторые реализации могут использовать больше)
    const uint32_t MAX_PACKET_SIZE = 1024 * 1024; // 1MB
    if (length == 0 || static_cast<uint32_t>(length) > MAX_PACKET_SIZE) {
        return false;
    }

    // Читаем данные с циклом для гарантии получения всех данных
    data.resize(length);
    totalReceived = 0;
    while (totalReceived < length) {
        int received = recv(sock, (char*)data.data() + totalReceived, length - totalReceived, 0);
        if (received <= 0) {
            // Ошибка или соединение закрыто
            return false;
        }
        totalReceived += received;
    }

    return true;
}

// Парсинг NAL unit заголовка для H.264
static void parse_h264_nal_header(uint8_t byte, uint8_t& type, bool& forbidden, uint8_t& nri) {
    forbidden = (byte >> 7) & 0x1;
    nri = (byte >> 5) & 0x3;
    type = byte & 0x1F;
}

// Парсинг NAL unit заголовка для H.265
static void parse_h265_nal_header(uint16_t header, uint8_t& type, bool& forbidden, uint8_t& nri, uint8_t& layerId) {
    forbidden = (header >> 15) & 0x1;
    type = (header >> 9) & 0x3F;
    nri = (header >> 7) & 0x3;
    layerId = (header >> 1) & 0x3F;
}

// Обработка single NAL unit (H.264)
static bool process_single_nal_h264(const std::vector<uint8_t>& payload, NALUnit& nal) {
    if (payload.empty()) return false;

    parse_h264_nal_header(payload[0], nal.type, nal.forbidden, nal.nri);

    // Single NAL unit: весь payload является NAL unit
    nal.data = payload;

    return true;
}

// Обработка FU-A (Fragmentation Unit Type A) для H.264
static bool process_fua_h264(const std::vector<uint8_t>& payload, RTPStream& stream, NALUnit& nal) {
    if (payload.size() < 2) return false;

    // FU-A заголовок: [F|NRI|Type] [S|E|R|Type]
    uint8_t fuIndicator = payload[0];
    uint8_t fuHeader = payload[1];

    bool start = (fuHeader >> 7) & 0x1;
    bool end = (fuHeader >> 6) & 0x1;
    bool reserved = (fuHeader >> 5) & 0x1;
    uint8_t nalType = fuHeader & 0x1F;

    if (reserved) return false; // Reserved bit должен быть 0

    parse_h264_nal_header(fuIndicator, nal.type, nal.forbidden, nal.nri);
    nal.type = nalType; // Тип из FU header

    if (start) {
        // Начало фрагментированного NAL unit
        stream.isFragmenting = true;
        stream.fragmentStartType = nalType;
        stream.fragmentedBuffer.clear();

        // Создаем NAL unit заголовок
        uint8_t nalHeader = (nal.forbidden << 7) | (nal.nri << 5) | nalType;
        stream.fragmentedBuffer.push_back(nalHeader);

        // Добавляем payload (без FU-A заголовка)
        if (payload.size() > 2) {
            stream.fragmentedBuffer.insert(stream.fragmentedBuffer.end(),
                                         payload.begin() + 2, payload.end());
        }

        return false; // Еще не полный NAL unit
    } else if (stream.isFragmenting && stream.fragmentStartType == nalType) {
        // Продолжение фрагментированного NAL unit
        if (payload.size() > 2) {
            stream.fragmentedBuffer.insert(stream.fragmentedBuffer.end(),
                                         payload.begin() + 2, payload.end());
        }

        if (end) {
            // Конец фрагментированного NAL unit
            nal.data = stream.fragmentedBuffer;
            nal.type = stream.fragmentStartType;
            stream.isFragmenting = false;
            return true; // Полный NAL unit готов
        }

        return false; // Еще не полный NAL unit
    }

    return false; // Некорректная последовательность
}

// Обработка STAP-A (Single Time Aggregation Packet Type A) для H.264
static std::vector<NALUnit> process_stapa_h264(const std::vector<uint8_t>& payload) {
    std::vector<NALUnit> nals;

    if (payload.size() < 2) return nals;

    // STAP-A заголовок: [F|NRI|Type=24]
    uint8_t stapHeader = payload[0];
    uint8_t type;
    bool forbidden;
    uint8_t nri;
    parse_h264_nal_header(stapHeader, type, forbidden, nri);

    if (type != 24) return nals; // STAP-A имеет тип 24

    // Парсинг агрегированных NAL units
    size_t offset = 1;
    while (offset + 2 <= payload.size()) {
        // Размер NAL unit (16 bits, big-endian)
        uint16_t nalSize = (payload[offset] << 8) | payload[offset + 1];
        offset += 2;

        if (nalSize == 0 || offset + nalSize > payload.size()) break;

        NALUnit nal;
        nal.forbidden = forbidden;
        nal.nri = nri;

        // Парсинг NAL unit заголовка
        if (nalSize > 0) {
            parse_h264_nal_header(payload[offset], nal.type, nal.forbidden, nal.nri);
            nal.data.assign(payload.begin() + offset, payload.begin() + offset + nalSize);
            nals.push_back(nal);
        }

        offset += nalSize;
    }

    return nals;
}

// Обработка STAP-B (Single Time Aggregation Packet Type B) для H.264
// STAP-B отличается от STAP-A наличием DON (Decoding Order Number) для каждого NAL unit
static std::vector<NALUnit> process_stapb_h264(const std::vector<uint8_t>& payload) {
    std::vector<NALUnit> nals;

    if (payload.size() < 3) return nals;

    // STAP-B заголовок: [F|NRI|Type=25]
    uint8_t stapHeader = payload[0];
    uint8_t type;
    bool forbidden;
    uint8_t nri;
    parse_h264_nal_header(stapHeader, type, forbidden, nri);

    if (type != 25) return nals; // STAP-B имеет тип 25

    // Парсинг агрегированных NAL units с DON
    size_t offset = 1;
    while (offset + 3 <= payload.size()) {
        // DON (Decoding Order Number) - 16 bits, big-endian
        uint16_t don = (payload[offset] << 8) | payload[offset + 1];
        offset += 2;

        // Размер NAL unit (16 bits, big-endian)
        if (offset + 2 > payload.size()) break;
        uint16_t nalSize = (payload[offset] << 8) | payload[offset + 1];
        offset += 2;

        if (nalSize == 0 || offset + nalSize > payload.size()) break;

        NALUnit nal;
        nal.forbidden = forbidden;
        nal.nri = nri;
        // Примечание: DON сохраняется для правильного порядка декодирования,
        // но в текущей реализации мы просто обрабатываем NAL units последовательно

        // Парсинг NAL unit заголовка
        if (nalSize > 0) {
            parse_h264_nal_header(payload[offset], nal.type, nal.forbidden, nal.nri);
            nal.data.assign(payload.begin() + offset, payload.begin() + offset + nalSize);
            nals.push_back(nal);
        }

        offset += nalSize;
    }

    return nals;
}

// Обработка FU-A для H.265 (аналогично H.264, но с другим заголовком)
static bool process_fua_h265(const std::vector<uint8_t>& payload, RTPStream& stream, NALUnit& nal) {
    if (payload.size() < 3) return false;

    // H.265 FU-A: [F|Type|LayerID|TID] [S|E|Type]
    uint16_t fuIndicator = (payload[0] << 8) | payload[1];
    uint8_t fuHeader = payload[2];

    bool start = (fuHeader >> 7) & 0x1;
    bool end = (fuHeader >> 6) & 0x1;
    uint8_t nalType = fuHeader & 0x3F;

    uint8_t type, nri, layerId;
    bool forbidden;
    parse_h265_nal_header(fuIndicator, type, forbidden, nri, layerId);
    nal.type = nalType; // Тип из FU header

    if (start) {
        stream.isFragmenting = true;
        stream.fragmentStartType = nalType;
        stream.fragmentedBuffer.clear();

        // Создаем NAL unit заголовок для H.265 (2 байта)
        uint16_t nalHeader = (forbidden << 15) | (nalType << 9) | (nri << 7) | (layerId << 1) | 1;
        stream.fragmentedBuffer.push_back((nalHeader >> 8) & 0xFF);
        stream.fragmentedBuffer.push_back(nalHeader & 0xFF);

        if (payload.size() > 3) {
            stream.fragmentedBuffer.insert(stream.fragmentedBuffer.end(),
                                         payload.begin() + 3, payload.end());
        }

        return false;
    } else if (stream.isFragmenting && stream.fragmentStartType == nalType) {
        if (payload.size() > 3) {
            stream.fragmentedBuffer.insert(stream.fragmentedBuffer.end(),
                                         payload.begin() + 3, payload.end());
        }

        if (end) {
            nal.data = stream.fragmentedBuffer;
            nal.type = stream.fragmentStartType;
            stream.isFragmenting = false;
            return true;
        }

        return false;
    }

    return false;
}

// Обработка RTP payload для H.264/H.265
static std::vector<NALUnit> process_rtp_payload_h264_h265(const std::vector<uint8_t>& payload, RTPStream& stream) {
    std::vector<NALUnit> nals;

    if (payload.empty()) return nals;

    if (stream.codec == "H264" || stream.codec == "H.264") {
        uint8_t nalType = payload[0] & 0x1F;

        if (nalType >= 1 && nalType <= 23) {
            // Single NAL unit
            NALUnit nal;
            if (process_single_nal_h264(payload, nal)) {
                nals.push_back(nal);
            }
        } else if (nalType == 24) {
            // STAP-A
            nals = process_stapa_h264(payload);
        } else if (nalType == 25) {
            // STAP-B
            nals = process_stapb_h264(payload);
        } else if (nalType == 28) {
            // FU-A
            NALUnit nal;
            if (process_fua_h264(payload, stream, nal)) {
                nals.push_back(nal);
            }
        }
        // TODO: Добавить MTAP16 (26), MTAP24 (27), FU-B (29) - используются реже
    } else if (stream.codec == "H265" || stream.codec == "H.265" || stream.codec == "HEVC") {
        if (payload.size() < 2) return nals;

        uint16_t nalHeader = (payload[0] << 8) | payload[1];
        uint8_t nalType = (nalHeader >> 9) & 0x3F;

        if (nalType >= 0 && nalType <= 40) {
            // Single NAL unit для H.265
            NALUnit nal;
            bool forbidden;
            uint8_t nri, layerId;
            parse_h265_nal_header(nalHeader, nal.type, forbidden, nal.nri, layerId);
            nal.data = payload;
            nals.push_back(nal);
        } else if (nalType == 48) {
            // STAP-A для H.265 (Single Time Aggregation Packet Type A)
            // Структура аналогична H.264 STAP-A, но с 2-байтовым заголовком
            if (payload.size() < 3) return nals;

            size_t offset = 2; // Пропускаем 2-байтовый заголовок
            while (offset + 2 <= payload.size()) {
                // Размер NAL unit (16 bits, big-endian)
                uint16_t nalSize = (payload[offset] << 8) | payload[offset + 1];
                offset += 2;

                if (nalSize == 0 || offset + nalSize > payload.size()) break;

                NALUnit nal;
                bool forbidden;
                uint8_t nri, layerId;
                if (nalSize >= 2) {
                    uint16_t nalUnitHeader = (payload[offset] << 8) | payload[offset + 1];
                    parse_h265_nal_header(nalUnitHeader, nal.type, forbidden, nal.nri, layerId);
                    nal.data.assign(payload.begin() + offset, payload.begin() + offset + nalSize);
                    nals.push_back(nal);
                }

                offset += nalSize;
            }
        } else if (nalType == 49) {
            // STAP-B для H.265 (аналогично H.264 STAP-B)
            // H.265 STAP-B имеет структуру: [F|Type|LayerID|TID] [DON] [Size] [NAL] ...
            if (payload.size() < 4) return nals;

            size_t offset = 2; // Пропускаем 2-байтовый заголовок
            while (offset + 4 <= payload.size()) {
                // DON (Decoding Order Number) - 16 bits, big-endian
                uint16_t don = (payload[offset] << 8) | payload[offset + 1];
                offset += 2;

                // Размер NAL unit (16 bits, big-endian)
                if (offset + 2 > payload.size()) break;
                uint16_t nalSize = (payload[offset] << 8) | payload[offset + 1];
                offset += 2;

                if (nalSize == 0 || offset + nalSize > payload.size()) break;

                NALUnit nal;
                bool forbidden;
                uint8_t nri, layerId;
                uint16_t nalHeader = (payload[0] << 8) | payload[1];
                parse_h265_nal_header(nalHeader, nal.type, forbidden, nal.nri, layerId);
                nal.data.assign(payload.begin() + offset, payload.begin() + offset + nalSize);
                nals.push_back(nal);

                offset += nalSize;
            }
        } else if (nalType == 52) {
            // FU-A для H.265 (Fragmentation Unit Type A)
            NALUnit nal;
            if (process_fua_h265(payload, stream, nal)) {
                nals.push_back(nal);
            }
        }
        // TODO: Добавить MTAP16 (50), MTAP24 (51) для H.265 - используются реже
    }

    return nals;
}

// Функция для приема RTP пакетов в отдельном потоке
static void receive_rtp_thread(RTSPClient* client) {
    if (!client) return;

    // Graceful error handling с try-catch
    try {
        // Ждем завершения handshake с таймаутом 10 секунд
        {
            std::unique_lock<std::mutex> lock(client->mutex);
            bool completed = client->handshakeCv.wait_for(
                lock,
                std::chrono::seconds(10),
                [client] { return client->handshakeComplete || client->shouldStop; }
            );
            
            if (!completed || client->shouldStop || !client->handshakeComplete) {
                // Timeout или отмена - выходим
                return;
            }
        }

        // Handshake завершен, начинаем прием RTP пакетов
        fd_set readfds;
        struct timeval tv;

        while (!client->shouldStop && client->playing) {
            FD_ZERO(&readfds);
            int maxFd = 0;
            bool hasTcpStreams = false;

            // Копируем информацию о потоках под блокировкой мьютекса для безопасности
            std::vector<RTPStream> streamsCopy;
            SOCKET rtspSocketCopy = INVALID_SOCKET;

            {
                std::lock_guard<std::mutex> lock(client->mutex);
                streamsCopy = client->rtpStreams;
                rtspSocketCopy = client->rtspSocket;
            }

            // Проверяем наличие TCP потоков
            for (const auto& stream : streamsCopy) {
                if (stream.useTcp) {
                    hasTcpStreams = true;
                    break;
                }
            }

            if (hasTcpStreams) {
                // Для TCP транспорта читаем из RTSP сокета
                if (rtspSocketCopy != INVALID_SOCKET) {
                    FD_SET(rtspSocketCopy, &readfds);
                    maxFd = rtspSocketCopy;
                }
            } else {
                // Для UDP транспорта читаем из отдельных сокетов
                for (const auto& stream : streamsCopy) {
                    if (stream.rtpSocket != INVALID_SOCKET) {
                        FD_SET(stream.rtpSocket, &readfds);
                        if (stream.rtpSocket > maxFd) maxFd = stream.rtpSocket;
                    }
                    if (stream.rtcpSocket != INVALID_SOCKET) {
                        FD_SET(stream.rtcpSocket, &readfds);
                        if (stream.rtcpSocket > maxFd) maxFd = stream.rtcpSocket;
                    }
                }
            }

            if (maxFd == 0) break;

            tv.tv_sec = 1;
            tv.tv_usec = 0;

            // Вызов select() с обработкой ошибок
            int activity = select(maxFd + 1, &readfds, nullptr, nullptr, &tv);

            // Обработка ошибок select()
            if (activity < 0) {
#ifdef _WIN32
                int error = WSAGetLastError();
                if (error == WSAEINTR) {
                    // Прервано сигналом, продолжаем
                    continue;
                } else if (error == WSAENOTSOCK) {
                    // Некорректный сокет, выходим из цикла
                    break;
                }
#else
                if (errno == EINTR) {
                    // Прервано сигналом, продолжаем
                    continue;
                } else if (errno == EBADF) {
                    // Некорректный файловый дескриптор, выходим из цикла
                    break;
                }
#endif
                // Другие ошибки - продолжаем с таймаутом
                continue;
            }

            if (activity == 0) {
                // Таймаут, продолжаем
                continue;
            }

            if (hasTcpStreams && rtspSocketCopy != INVALID_SOCKET && FD_ISSET(rtspSocketCopy, &readfds)) {
            // Чтение interleaved binary data для TCP транспорта
            uint8_t channel;
            uint16_t length;
            std::vector<uint8_t> interleavedData;

            if (read_interleaved_data(rtspSocketCopy, channel, length, interleavedData)) {
                // Находим поток по channel (используем копию для безопасности)
                for (auto& stream : streamsCopy) {
                    if (stream.useTcp && (channel == stream.rtpChannel || channel == stream.rtcpChannel)) {
                        // Находим соответствующий поток в оригинальном массиве под блокировкой
                        RTPStream* targetStream = nullptr;
                        {
                            std::lock_guard<std::mutex> lock(client->mutex);
                            for (auto& originalStream : client->rtpStreams) {
                                if (originalStream.useTcp &&
                                    (channel == originalStream.rtpChannel || channel == originalStream.rtcpChannel)) {
                                    targetStream = &originalStream;
                                    break;
                                }
                            }
                        }

                        // Обрабатываем пакет вне блокировки
                        if (targetStream) {
                            if (channel == targetStream->rtpChannel && !interleavedData.empty()) {
                                // RTP пакет
                                process_rtp_packet(interleavedData.data(), static_cast<int>(interleavedData.size()), *targetStream, client);
                            } else if (channel == targetStream->rtcpChannel) {
                                // RTCP пакет
                                if (!interleavedData.empty()) {
                                    process_rtcp_packet(interleavedData.data(), static_cast<int>(interleavedData.size()), *targetStream, client);
                                }
                            }
                        }
                        break;
                    }
                }
            }
        } else {
            // UDP транспорт - используем копию потоков
            for (const auto& streamCopy : streamsCopy) {
                if (streamCopy.useTcp) continue; // Пропускаем TCP потоки

                if (streamCopy.rtpSocket != INVALID_SOCKET && FD_ISSET(streamCopy.rtpSocket, &readfds)) {
                    // Буфер для RTP пакетов (максимальный размер UDP пакета обычно 65507 байт)
                    uint8_t buffer[65536];
                    struct sockaddr_in fromAddr;
                    socklen_t fromLen = sizeof(fromAddr);

                    int received = recvfrom(streamCopy.rtpSocket, (char*)buffer, sizeof(buffer), 0,
                                           (struct sockaddr*)&fromAddr, &fromLen);
                    // Проверка на валидный размер пакета
                    if (received > 0 && received <= static_cast<int>(sizeof(buffer))) {
                        // Находим соответствующий поток в оригинальном массиве под блокировкой
                        RTPStream* targetStream = nullptr;
                        {
                            std::lock_guard<std::mutex> lock(client->mutex);
                            for (auto& originalStream : client->rtpStreams) {
                                if (originalStream.rtpSocket == streamCopy.rtpSocket) {
                                    targetStream = &originalStream;
                                    break;
                                }
                            }
                        }

                        // Обрабатываем пакет вне блокировки
                        if (targetStream) {
                            process_rtp_packet(buffer, received, *targetStream, client);
                        }
                    }
                }

                if (streamCopy.rtcpSocket != INVALID_SOCKET && FD_ISSET(streamCopy.rtcpSocket, &readfds)) {
                    // Буфер для RTCP пакетов (обычно меньше чем RTP)
                    uint8_t buffer[1500];
                    struct sockaddr_in fromAddr;
                    socklen_t fromLen = sizeof(fromAddr);

                    int received = recvfrom(streamCopy.rtcpSocket, (char*)buffer, sizeof(buffer), 0,
                            (struct sockaddr*)&fromAddr, &fromLen);
                    // Проверка на валидный размер пакета
                    if (received > 0 && received <= static_cast<int>(sizeof(buffer))) {
                        // Находим соответствующий поток в оригинальном массиве под блокировкой
                        RTPStream* targetStream = nullptr;
                        {
                            std::lock_guard<std::mutex> lock(client->mutex);
                            for (auto& originalStream : client->rtpStreams) {
                                if (originalStream.rtcpSocket == streamCopy.rtcpSocket) {
                                    targetStream = &originalStream;
                                    break;
                                }
                            }
                        }

                        // Обрабатываем пакет вне блокировки
                        if (targetStream) {
                            process_rtcp_packet(buffer, received, *targetStream, client);
                        }
                    }
                }
            }
        }
    }
    } catch (const std::exception& e) {
        // Обработка исключений - graceful shutdown
        if (client && client->statusCallback) {
            std::string errorMsg = "RTP thread exception: " + std::string(e.what());
            client->statusCallback(RTSP_STATUS_ERROR, errorMsg.c_str(), client->statusUserData);
        }
    } catch (...) {
        // Обработка неизвестных исключений
        if (client && client->statusCallback) {
            client->statusCallback(RTSP_STATUS_ERROR, "RTP thread unknown exception", client->statusUserData);
        }
    }
}

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
    client->cseq = 1;

    // Парсинг URL
    if (!parse_rtsp_url(url, client->rtspUrl)) {
        client->status = RTSP_STATUS_ERROR;
        if (client->statusCallback) {
            client->statusCallback(RTSP_STATUS_ERROR, "Invalid RTSP URL", client->statusUserData);
        }
        return false;
    }

    // Если username/password в URL, используем их
    if (!client->rtspUrl.username.empty()) {
        client->username = client->rtspUrl.username;
        client->password = client->rtspUrl.password;
    }

    // Подключение к RTSP серверу
    client->rtspSocket = create_tcp_socket(client->rtspUrl.host, client->rtspUrl.port, timeout_ms);
    if (client->rtspSocket == INVALID_SOCKET) {
        client->status = RTSP_STATUS_ERROR;
        if (client->statusCallback) {
            client->statusCallback(RTSP_STATUS_ERROR, "Failed to connect to RTSP server", client->statusUserData);
        }
        return false;
    }

    // Формируем полный RTSP URL для запросов
    std::string fullRtspUrl = build_rtsp_url(client->rtspUrl);

    // Отправка OPTIONS запроса (опционально, для проверки соединения)
    std::ostringstream optionsHeaders;
    optionsHeaders << "CSeq: " << client->cseq++ << "\r\n";
    if (!client->username.empty() && !client->password.empty()) {
        optionsHeaders << "Authorization: " << generate_basic_auth(client->username, client->password) << "\r\n";
    }
    optionsHeaders << "User-Agent: IP-CSS RTSP Client\r\n";

    std::string optionsResponse;
    if (!send_rtsp_request(client->rtspSocket, "OPTIONS", fullRtspUrl,
                          optionsHeaders.str(), "", optionsResponse)) {
        close(client->rtspSocket);
        client->rtspSocket = INVALID_SOCKET;
        client->status = RTSP_STATUS_ERROR;
        if (client->statusCallback) {
            client->statusCallback(RTSP_STATUS_ERROR, "Failed to send OPTIONS request", client->statusUserData);
        }
        return false;
    }

    // Отправка DESCRIBE запроса
    std::ostringstream describeHeaders;
    describeHeaders << "CSeq: " << client->cseq++ << "\r\n";
    describeHeaders << "Accept: application/sdp\r\n";
    if (!client->username.empty() && !client->password.empty()) {
        describeHeaders << "Authorization: " << generate_basic_auth(client->username, client->password) << "\r\n";
    }
    describeHeaders << "User-Agent: IP-CSS RTSP Client\r\n";

    std::string describeResponse;
    if (!send_rtsp_request(client->rtspSocket, "DESCRIBE", fullRtspUrl,
                          describeHeaders.str(), "", describeResponse)) {
        close(client->rtspSocket);
        client->rtspSocket = INVALID_SOCKET;
        client->status = RTSP_STATUS_ERROR;
        if (client->statusCallback) {
            client->statusCallback(RTSP_STATUS_ERROR, "Failed to send DESCRIBE request", client->statusUserData);
        }
        return false;
    }

    // Парсинг ответа DESCRIBE
    int statusCode;
    std::string sessionId;
    std::string wwwAuthenticate;
    if (!parse_rtsp_response(describeResponse, statusCode, sessionId, wwwAuthenticate)) {
        close(client->rtspSocket);
        client->rtspSocket = INVALID_SOCKET;
        client->status = RTSP_STATUS_ERROR;
        if (client->statusCallback) {
            client->statusCallback(RTSP_STATUS_ERROR, "Failed to parse DESCRIBE response", client->statusUserData);
        }
        return false;
    }

    // Обработка 401 Unauthorized - попытка Digest Authentication
    if (statusCode == 401 && !wwwAuthenticate.empty() && !client->username.empty() && !client->password.empty()) {
        if (parse_www_authenticate(wwwAuthenticate, client->digestParams)) {
            client->useDigestAuth = true;
            client->digestNc = 1;

            // Повторная отправка DESCRIBE с Digest Authentication
            std::ostringstream describeHeadersDigest;
            describeHeadersDigest << "CSeq: " << client->cseq++ << "\r\n";
            describeHeadersDigest << "Accept: application/sdp\r\n";
            describeHeadersDigest << "Authorization: " << generate_digest_auth(
                "DESCRIBE",
                client->rtspUrl.path,
                client->username,
                client->password,
                client->digestParams,
                client->digestNc++
            ) << "\r\n";
            describeHeadersDigest << "User-Agent: IP-CSS RTSP Client\r\n";

            std::string describeResponseDigest;
            if (!send_rtsp_request(client->rtspSocket, "DESCRIBE", client->rtspUrl.path,
                                  describeHeadersDigest.str(), "", describeResponseDigest)) {
                close(client->rtspSocket);
                client->rtspSocket = INVALID_SOCKET;
                client->status = RTSP_STATUS_ERROR;
                if (client->statusCallback) {
                    client->statusCallback(RTSP_STATUS_ERROR, "Failed to send DESCRIBE request with Digest auth", client->statusUserData);
                }
                return false;
            }

            // Парсинг ответа с Digest
            if (!parse_rtsp_response(describeResponseDigest, statusCode, sessionId, wwwAuthenticate)) {
                close(client->rtspSocket);
                client->rtspSocket = INVALID_SOCKET;
                client->status = RTSP_STATUS_ERROR;
                if (client->statusCallback) {
                    client->statusCallback(RTSP_STATUS_ERROR, "Failed to parse DESCRIBE response with Digest auth", client->statusUserData);
                }
                return false;
            }

            // Если снова 401, возможно nonce устарел
            if (statusCode == 401 && !wwwAuthenticate.empty()) {
                DigestAuthParams newParams;
                if (parse_www_authenticate(wwwAuthenticate, newParams)) {
                    if (newParams.stale) {
                        // Обновляем параметры и пробуем еще раз
                        client->digestParams = newParams;
                        client->digestNc = 1;

                        describeHeadersDigest.str("");
                        describeHeadersDigest << "CSeq: " << client->cseq++ << "\r\n";
                        describeHeadersDigest << "Accept: application/sdp\r\n";
                        describeHeadersDigest << "Authorization: " << generate_digest_auth(
                            "DESCRIBE",
                            client->rtspUrl.path,
                            client->username,
                            client->password,
                            client->digestParams,
                            client->digestNc++
                        ) << "\r\n";
                        describeHeadersDigest << "User-Agent: IP-CSS RTSP Client\r\n";

                        if (!send_rtsp_request(client->rtspSocket, "DESCRIBE", client->rtspUrl.path,
                                              describeHeadersDigest.str(), "", describeResponseDigest)) {
                            close(client->rtspSocket);
                            client->rtspSocket = INVALID_SOCKET;
                            client->status = RTSP_STATUS_ERROR;
                            return false;
                        }

                        if (!parse_rtsp_response(describeResponseDigest, statusCode, sessionId, wwwAuthenticate)) {
                            close(client->rtspSocket);
                            client->rtspSocket = INVALID_SOCKET;
                            client->status = RTSP_STATUS_ERROR;
                            return false;
                        }
                    }
                }
            }

            // Обновляем describeResponse для дальнейшей обработки
            describeResponse = describeResponseDigest;
        }
    }

    if (statusCode != 200) {
        close(client->rtspSocket);
        client->rtspSocket = INVALID_SOCKET;
        client->status = RTSP_STATUS_ERROR;
        if (client->statusCallback) {
            client->statusCallback(RTSP_STATUS_ERROR, "DESCRIBE request failed", client->statusUserData);
        }
        return false;
    }

    // Извлечение SDP из ответа
    size_t sdpStart = describeResponse.find("\r\n\r\n");
    if (sdpStart == std::string::npos) {
        close(client->rtspSocket);
        client->rtspSocket = INVALID_SOCKET;
        client->status = RTSP_STATUS_ERROR;
        if (client->statusCallback) {
            client->statusCallback(RTSP_STATUS_ERROR, "No SDP in DESCRIBE response", client->statusUserData);
        }
        return false;
    }

    std::string sdp = describeResponse.substr(sdpStart + 4);

    // Парсинг SDP и создание RTP потоков
    if (!parse_sdp(sdp, client->rtpStreams, client)) {
        close(client->rtspSocket);
        client->rtspSocket = INVALID_SOCKET;
        client->status = RTSP_STATUS_ERROR;
        if (client->statusCallback) {
            client->statusCallback(RTSP_STATUS_ERROR, "Failed to parse SDP", client->statusUserData);
        }
        return false;
    }

    // SETUP для каждого потока
    for (auto& stream : client->rtpStreams) {
        // Сначала пробуем UDP транспорт
        bool useTcp = false;

        // Создание UDP сокетов для RTP/RTCP
        stream.rtpSocket = create_udp_socket(stream.clientRtpPort);
        stream.rtcpSocket = create_udp_socket(stream.clientRtcpPort);

        if (stream.rtpSocket == INVALID_SOCKET || stream.rtcpSocket == INVALID_SOCKET) {
            // Если не удалось создать UDP сокеты, пробуем TCP
            useTcp = true;
        }

        // Если уже установлен TCP, используем его
        if (stream.useTcp) {
            useTcp = true;
        }

        // Формирование полного RTSP control URL
        std::string controlPath = stream.controlUrl;
        if (controlPath.empty()) {
            controlPath = stream.trackId;
        }
        // Убираем ведущий слэш из path если он есть
        std::string basePath = client->rtspUrl.path;
        if (!basePath.empty() && basePath[0] == '/') {
            basePath = basePath.substr(1);
        }
        // Формируем полный control URL: rtsp://host:port/path/controlPath
        std::string controlUrl = build_rtsp_url(client->rtspUrl, basePath + "/" + controlPath);

        // Отправка SETUP запроса с поддержкой Digest Authentication
        // Поддержка выбора транспорта: UDP по умолчанию, TCP как fallback
        std::ostringstream setupHeaders;
        setupHeaders << "CSeq: " << client->cseq++ << "\r\n";

        if (useTcp) {
            // TCP транспорт: RTP/AVP/TCP;interleaved=0-1
            setupHeaders << "Transport: RTP/AVP/TCP;interleaved="
                         << stream.rtpChannel << "-" << stream.rtcpChannel << "\r\n";
        } else {
            // UDP транспорт: RTP/AVP/UDP;unicast;client_port=xxxx-xxxx
            setupHeaders << "Transport: RTP/AVP/UDP;unicast;client_port="
                         << stream.clientRtpPort << "-" << stream.clientRtcpPort << "\r\n";
        }
        if (!client->username.empty() && !client->password.empty()) {
            if (client->useDigestAuth) {
                setupHeaders << "Authorization: " << generate_digest_auth(
                    "SETUP",
                    controlUrl,
                    client->username,
                    client->password,
                    client->digestParams,
                    client->digestNc++
                ) << "\r\n";
            } else {
                setupHeaders << "Authorization: " << generate_basic_auth(client->username, client->password) << "\r\n";
            }
        }
        setupHeaders << "User-Agent: IP-CSS RTSP Client\r\n";

        std::string setupResponse;
        if (!send_rtsp_request(client->rtspSocket, "SETUP", controlUrl,
                              setupHeaders.str(), "", setupResponse)) {
            close(client->rtspSocket);
            client->rtspSocket = INVALID_SOCKET;
            client->status = RTSP_STATUS_ERROR;
            if (client->statusCallback) {
                client->statusCallback(RTSP_STATUS_ERROR, "Failed to send SETUP request", client->statusUserData);
            }
            return false;
        }

        // Парсинг ответа SETUP с полной обработкой заголовков
        RTSPResponseHeaders setupRespHeaders;
        if (!parse_rtsp_response(setupResponse, setupRespHeaders)) {
            close(client->rtspSocket);
            client->rtspSocket = INVALID_SOCKET;
            client->status = RTSP_STATUS_ERROR;
            if (client->statusCallback) {
                client->statusCallback(RTSP_STATUS_ERROR, "Failed to parse SETUP response", client->statusUserData);
            }
            return false;
        }

        int statusCode = setupRespHeaders.statusCode;
        std::string wwwAuthSetup = setupRespHeaders.wwwAuthenticate;

        // Обработка ошибок 4xx и 5xx
        if (statusCode >= 400) {
            std::string errorMsg = "SETUP request failed: " + std::to_string(statusCode);
            if (!setupRespHeaders.errorMessage.empty()) {
                errorMsg += " - " + setupRespHeaders.errorMessage;
            } else if (!setupRespHeaders.statusText.empty()) {
                errorMsg += " " + setupRespHeaders.statusText;
            }

            // Если UDP не работает (461 Unsupported Transport), пробуем TCP
            if (statusCode == 461 && !useTcp) {
                // Закрываем UDP сокеты
                if (stream.rtpSocket != INVALID_SOCKET) {
                    close(stream.rtpSocket);
                    stream.rtpSocket = INVALID_SOCKET;
                }
                if (stream.rtcpSocket != INVALID_SOCKET) {
                    close(stream.rtcpSocket);
                    stream.rtcpSocket = INVALID_SOCKET;
                }

                // Устанавливаем TCP каналы
                stream.rtpChannel = 0;
                stream.rtcpChannel = 1;
                stream.useTcp = true;
                useTcp = true;

                // Повторная отправка SETUP с TCP транспортом
                std::ostringstream setupHeadersTcp;
                setupHeadersTcp << "CSeq: " << client->cseq++ << "\r\n";
                setupHeadersTcp << "Transport: RTP/AVP/TCP;interleaved="
                               << stream.rtpChannel << "-" << stream.rtcpChannel << "\r\n";
                if (!client->username.empty() && !client->password.empty()) {
                    if (client->useDigestAuth) {
                        setupHeadersTcp << "Authorization: " << generate_digest_auth(
                            "SETUP",
                            controlUrl,
                            client->username,
                            client->password,
                            client->digestParams,
                            client->digestNc++
                        ) << "\r\n";
                    } else {
                        setupHeadersTcp << "Authorization: " << generate_basic_auth(client->username, client->password) << "\r\n";
                    }
                }
                setupHeadersTcp << "User-Agent: IP-CSS RTSP Client\r\n";

                if (!send_rtsp_request(client->rtspSocket, "SETUP", controlUrl,
                                      setupHeadersTcp.str(), "", setupResponse)) {
                    close(client->rtspSocket);
                    client->rtspSocket = INVALID_SOCKET;
                    client->status = RTSP_STATUS_ERROR;
                    if (client->statusCallback) {
                        client->statusCallback(RTSP_STATUS_ERROR, "Failed to send SETUP request with TCP transport", client->statusUserData);
                    }
                    return false;
                }

                if (!parse_rtsp_response(setupResponse, setupRespHeaders)) {
                    close(client->rtspSocket);
                    client->rtspSocket = INVALID_SOCKET;
                    client->status = RTSP_STATUS_ERROR;
                    if (client->statusCallback) {
                        client->statusCallback(RTSP_STATUS_ERROR, "Failed to parse SETUP response with TCP transport", client->statusUserData);
                    }
                    return false;
                }

                statusCode = setupRespHeaders.statusCode;
            }

            // Обработка 401 Unauthorized с Digest Authentication
            if (statusCode == 401 && !wwwAuthSetup.empty() && !client->username.empty() && !client->password.empty()) {
                DigestAuthParams newParams;
                if (parse_www_authenticate(wwwAuthSetup, newParams)) {
                    if (newParams.stale || !client->useDigestAuth) {
                        client->digestParams = newParams;
                        client->useDigestAuth = true;
                        client->digestNc = 1;
                    }

                    // Повторная отправка SETUP с Digest Authentication
                    std::ostringstream setupHeadersDigest;
                    setupHeadersDigest << "CSeq: " << client->cseq++ << "\r\n";
                    setupHeadersDigest << "Transport: RTP/AVP/UDP;unicast;client_port="
                                     << stream.clientRtpPort << "-" << stream.clientRtcpPort << "\r\n";
                    setupHeadersDigest << "Authorization: " << generate_digest_auth(
                        "SETUP",
                        controlUrl,
                        client->username,
                        client->password,
                        client->digestParams,
                        client->digestNc++
                    ) << "\r\n";
                    setupHeadersDigest << "User-Agent: IP-CSS RTSP Client\r\n";

                    std::string setupResponseDigest;
                    if (!send_rtsp_request(client->rtspSocket, "SETUP", controlUrl,
                                          setupHeadersDigest.str(), "", setupResponseDigest)) {
                        close(client->rtspSocket);
                        client->rtspSocket = INVALID_SOCKET;
                        client->status = RTSP_STATUS_ERROR;
                        if (client->statusCallback) {
                            client->statusCallback(RTSP_STATUS_ERROR, "Failed to send SETUP request with Digest auth", client->statusUserData);
                        }
                        return false;
                    }

                    if (!parse_rtsp_response(setupResponseDigest, setupRespHeaders)) {
                        close(client->rtspSocket);
                        client->rtspSocket = INVALID_SOCKET;
                        client->status = RTSP_STATUS_ERROR;
                        if (client->statusCallback) {
                            client->statusCallback(RTSP_STATUS_ERROR, "Failed to parse SETUP response with Digest auth", client->statusUserData);
                        }
                        return false;
                    }

                    statusCode = setupRespHeaders.statusCode;
                    setupResponse = setupResponseDigest;
                }
            }

            // Если все еще ошибка после повторной попытки
            if (statusCode >= 400) {
                close(client->rtspSocket);
                client->rtspSocket = INVALID_SOCKET;
                client->status = RTSP_STATUS_ERROR;
                if (client->statusCallback) {
                    client->statusCallback(RTSP_STATUS_ERROR, errorMsg.c_str(), client->statusUserData);
                }
                return false;
            }
        }

        // Обновление sessionId из ответа SETUP
        if (!setupRespHeaders.sessionId.empty()) {
            sessionId = setupRespHeaders.sessionId;
            client->sessionId = sessionId;
        }

        // Обработка 401 в SETUP - обновление Digest параметров
        if (statusCode == 401 && !wwwAuthSetup.empty() && !client->username.empty() && !client->password.empty()) {
            DigestAuthParams newParams;
            if (parse_www_authenticate(wwwAuthSetup, newParams)) {
                if (newParams.stale || !client->useDigestAuth) {
                    client->digestParams = newParams;
                    client->useDigestAuth = true;
                    client->digestNc = 1;
                }

                // Повторная отправка SETUP с обновленными параметрами
                setupHeaders.str("");
                setupHeaders << "CSeq: " << client->cseq++ << "\r\n";
                setupHeaders << "Transport: RTP/AVP/UDP;unicast;client_port="
                             << stream.clientRtpPort << "-" << stream.clientRtcpPort << "\r\n";
                setupHeaders << "Authorization: " << generate_digest_auth(
                    "SETUP",
                    controlUrl,
                    client->username,
                    client->password,
                    client->digestParams,
                    client->digestNc++
                ) << "\r\n";
                setupHeaders << "User-Agent: IP-CSS RTSP Client\r\n";

                if (!send_rtsp_request(client->rtspSocket, "SETUP", controlUrl,
                                      setupHeaders.str(), "", setupResponse)) {
                    close(client->rtspSocket);
                    client->rtspSocket = INVALID_SOCKET;
                    client->status = RTSP_STATUS_ERROR;
                    return false;
                }

                if (!parse_rtsp_response(setupResponse, statusCode, sessionId, wwwAuthSetup)) {
                    close(client->rtspSocket);
                    client->rtspSocket = INVALID_SOCKET;
                    client->status = RTSP_STATUS_ERROR;
                    return false;
                }
            }
        }

        if (statusCode != 200) {
            close(client->rtspSocket);
            client->rtspSocket = INVALID_SOCKET;
            client->status = RTSP_STATUS_ERROR;
            if (client->statusCallback) {
                client->statusCallback(RTSP_STATUS_ERROR, "SETUP request failed", client->statusUserData);
            }
            return false;
        }

        // Сохранение Session ID
        if (!sessionId.empty()) {
            client->sessionId = sessionId;
        }

        // Парсинг Transport заголовка из ответа SETUP
        if (!setupRespHeaders.transport.empty()) {
            std::string transport = setupRespHeaders.transport;

            // Парсинг server_port=xxxx-xxxx
            size_t serverPortPos = transport.find("server_port=");
            if (serverPortPos != std::string::npos) {
                size_t portStart = serverPortPos + 12;
                size_t portEnd = transport.find("-", portStart);
                if (portEnd != std::string::npos) {
                    try {
                        stream.serverRtpPort = std::stoi(transport.substr(portStart, portEnd - portStart));
                        size_t rtcpStart = portEnd + 1;
                        size_t rtcpEnd = transport.find_first_of("; \r\n", rtcpStart);
                        if (rtcpEnd == std::string::npos) rtcpEnd = transport.length();
                        stream.serverRtcpPort = std::stoi(transport.substr(rtcpStart, rtcpEnd - rtcpStart));
                    } catch (...) {
                        // Если не удалось распарсить порты, используем значения по умолчанию
                        // или значения из client_port
                    }
                }
            }

            // Парсинг source (для multicast)
            size_t sourcePos = transport.find("source=");
            if (sourcePos != std::string::npos) {
                size_t sourceStart = sourcePos + 7;
                size_t sourceEnd = transport.find_first_of("; \r\n", sourceStart);
                if (sourceEnd == std::string::npos) sourceEnd = transport.length();
                // Можно сохранить source адрес для multicast
            }

            // Парсинг interleaved (для TCP транспорта)
            size_t interleavedPos = transport.find("interleaved=");
            if (interleavedPos != std::string::npos) {
                size_t interleavedStart = interleavedPos + 12;
                size_t interleavedEnd = transport.find_first_of("; \r\n", interleavedStart);
                if (interleavedEnd == std::string::npos) interleavedEnd = transport.length();
                std::string interleaved = transport.substr(interleavedStart, interleavedEnd - interleavedStart);
                // Парсинг channel-channel (например, "0-1")
                size_t dashPos = interleaved.find("-");
                if (dashPos != std::string::npos) {
                    try {
                        stream.rtpChannel = std::stoi(interleaved.substr(0, dashPos));
                        stream.rtcpChannel = std::stoi(interleaved.substr(dashPos + 1));
                        stream.useTcp = true;
                        stream.transport = "TCP";
                    } catch (...) {
                        // Игнорируем ошибки парсинга
                    }
                }
            } else {
                stream.transport = "UDP";
                stream.useTcp = false;
            }
        }

        // Настройка адреса сервера для RTP
        struct sockaddr_in serverAddr;
        memset(&serverAddr, 0, sizeof(serverAddr));
        serverAddr.sin_family = AF_INET;
        serverAddr.sin_port = htons(stream.serverRtpPort);

        struct hostent* hostEntry = gethostbyname(client->rtspUrl.host.c_str());
        if (hostEntry) {
            memcpy(&serverAddr.sin_addr, hostEntry->h_addr_list[0], hostEntry->h_length);
        } else {
            inet_pton(AF_INET, client->rtspUrl.host.c_str(), &serverAddr.sin_addr);
        }

        // Можно установить соединение для отправки RTCP, но для базовой реализации это не обязательно
    }

    client->status = RTSP_STATUS_CONNECTED;
    client->connected = true;

    if (client->statusCallback) {
        client->statusCallback(RTSP_STATUS_CONNECTED, "Connected successfully", client->statusUserData);
    }

    return true;
}

// Функция для отправки PLAY после успешного соединения
static bool start_play_after_connect(RTSPClient* client) {
    if (!client || !client->connected) return false;

    // Устанавливаем флаг handshake complete
    client->handshakeComplete = true;
    client->handshakeCv.notify_all();

    return rtsp_client_play(client);
}

void rtsp_client_disconnect(RTSPClient* client) {
    if (!client) return;

    std::lock_guard<std::mutex> lock(client->mutex);

    rtsp_client_stop(client);
    client->shouldStop = true;

    // Отправка TEARDOWN запроса с поддержкой Digest Authentication
    if (client->rtspSocket != INVALID_SOCKET && client->connected) {
        // Формируем полный RTSP URL для TEARDOWN запроса
        std::string fullRtspUrl = build_rtsp_url(client->rtspUrl);

        std::ostringstream teardownHeaders;
        teardownHeaders << "CSeq: " << client->cseq++ << "\r\n";
        if (!client->sessionId.empty()) {
            teardownHeaders << "Session: " << client->sessionId << "\r\n";
        }
        if (!client->username.empty() && !client->password.empty()) {
            if (client->useDigestAuth) {
                teardownHeaders << "Authorization: " << generate_digest_auth(
                    "TEARDOWN",
                    fullRtspUrl,
                    client->username,
                    client->password,
                    client->digestParams,
                    client->digestNc++
                ) << "\r\n";
            } else {
                teardownHeaders << "Authorization: " << generate_basic_auth(client->username, client->password) << "\r\n";
            }
        }
        teardownHeaders << "User-Agent: IP-CSS RTSP Client\r\n";

        std::string teardownResponse;
        send_rtsp_request(client->rtspSocket, "TEARDOWN", fullRtspUrl,
                         teardownHeaders.str(), "", teardownResponse);

        // Парсинг ответа TEARDOWN (может быть 401, обрабатываем)
        RTSPResponseHeaders teardownRespHeaders;
        if (parse_rtsp_response(teardownResponse, teardownRespHeaders)) {
            if (teardownRespHeaders.statusCode == 401 && !teardownRespHeaders.wwwAuthenticate.empty() &&
                !client->username.empty() && !client->password.empty()) {
                DigestAuthParams newParams;
                if (parse_www_authenticate(teardownRespHeaders.wwwAuthenticate, newParams)) {
                    if (newParams.stale || !client->useDigestAuth) {
                        client->digestParams = newParams;
                        client->useDigestAuth = true;
                        client->digestNc = 1;
                    }

                    // Повторная отправка TEARDOWN с Digest Authentication
                    teardownHeaders.str("");
                    teardownHeaders << "CSeq: " << client->cseq++ << "\r\n";
                    if (!client->sessionId.empty()) {
                        teardownHeaders << "Session: " << client->sessionId << "\r\n";
                    }
                    teardownHeaders << "Authorization: " << generate_digest_auth(
                        "TEARDOWN",
                        fullRtspUrl,
                        client->username,
                        client->password,
                        client->digestParams,
                        client->digestNc++
                    ) << "\r\n";
                    teardownHeaders << "User-Agent: IP-CSS RTSP Client\r\n";

                    send_rtsp_request(client->rtspSocket, "TEARDOWN", fullRtspUrl,
                                     teardownHeaders.str(), "", teardownResponse);
                }
            }
        }

        close(client->rtspSocket);
        client->rtspSocket = INVALID_SOCKET;
    }

        // Закрытие RTP сокетов и освобождение ресурсов декодеров
        for (auto& stream : client->rtpStreams) {
            if (stream.rtpSocket != INVALID_SOCKET) {
                close(stream.rtpSocket);
                stream.rtpSocket = INVALID_SOCKET;
            }
            if (stream.rtcpSocket != INVALID_SOCKET) {
                close(stream.rtcpSocket);
                stream.rtcpSocket = INVALID_SOCKET;
            }
#ifdef ENABLE_FFMPEG
            cleanup_rtp_stream_decoders(stream);
#endif
        }
        client->rtpStreams.clear();

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

bool rtsp_client_play(RTSPClient* client) {
    if (!client || !client->connected) return false;

    {
        std::lock_guard<std::mutex> lock(client->mutex);

        if (client->playing) {
            return true; // Уже воспроизводится
        }

        if (client->rtspSocket == INVALID_SOCKET) {
            return false;
        }

        // Формируем полный RTSP URL для PLAY запроса
        std::string fullRtspUrl = build_rtsp_url(client->rtspUrl);

        // Отправка PLAY запроса с поддержкой Digest Authentication
        std::ostringstream playHeaders;
        playHeaders << "CSeq: " << client->cseq++ << "\r\n";
        if (!client->sessionId.empty()) {
            playHeaders << "Session: " << client->sessionId << "\r\n";
        }
        if (!client->username.empty() && !client->password.empty()) {
            if (client->useDigestAuth) {
                playHeaders << "Authorization: " << generate_digest_auth(
                    "PLAY",
                    fullRtspUrl,
                    client->username,
                    client->password,
                    client->digestParams,
                    client->digestNc++
                ) << "\r\n";
            } else {
                playHeaders << "Authorization: " << generate_basic_auth(client->username, client->password) << "\r\n";
            }
        }
        playHeaders << "User-Agent: IP-CSS RTSP Client\r\n";
        playHeaders << "Range: npt=0.000-\r\n";

        std::string playResponse;
        if (!send_rtsp_request(client->rtspSocket, "PLAY", fullRtspUrl,
                              playHeaders.str(), "", playResponse)) {
            if (client->statusCallback) {
                client->statusCallback(RTSP_STATUS_ERROR, "Failed to send PLAY request", client->statusUserData);
            }
            return false;
        }

        // Парсинг ответа PLAY с полной обработкой заголовков
        RTSPResponseHeaders playHeadersResp;
        if (!parse_rtsp_response(playResponse, playHeadersResp)) {
            if (client->statusCallback) {
                client->statusCallback(RTSP_STATUS_ERROR, "Failed to parse PLAY response", client->statusUserData);
            }
            return false;
        }

        int statusCode = playHeadersResp.statusCode;
        std::string playWwwAuth = playHeadersResp.wwwAuthenticate;

        // Обработка ошибок 4xx и 5xx
        if (statusCode >= 400) {
            std::string errorMsg = "PLAY request failed: " + std::to_string(statusCode);
            if (!playHeadersResp.errorMessage.empty()) {
                errorMsg += " - " + playHeadersResp.errorMessage;
            } else if (!playHeadersResp.statusText.empty()) {
                errorMsg += " " + playHeadersResp.statusText;
            }

            // Обработка 401 Unauthorized с Digest Authentication
            if (statusCode == 401 && !playWwwAuth.empty() && !client->username.empty() && !client->password.empty()) {
                DigestAuthParams newParams;
                if (parse_www_authenticate(playWwwAuth, newParams)) {
                    if (newParams.stale || !client->useDigestAuth) {
                        client->digestParams = newParams;
                        client->useDigestAuth = true;
                        client->digestNc = 1;
                    }

                    // Повторная отправка PLAY с Digest Authentication
                    playHeaders.str("");
                    playHeaders << "CSeq: " << client->cseq++ << "\r\n";
                    if (!client->sessionId.empty()) {
                        playHeaders << "Session: " << client->sessionId << "\r\n";
                    }
                    playHeaders << "Authorization: " << generate_digest_auth(
                        "PLAY",
                        fullRtspUrl,
                        client->username,
                        client->password,
                        client->digestParams,
                        client->digestNc++
                    ) << "\r\n";
                    playHeaders << "User-Agent: IP-CSS RTSP Client\r\n";
                    playHeaders << "Range: npt=0.000-\r\n";

                    if (!send_rtsp_request(client->rtspSocket, "PLAY", fullRtspUrl,
                                          playHeaders.str(), "", playResponse)) {
                        if (client->statusCallback) {
                            client->statusCallback(RTSP_STATUS_ERROR, "Failed to send PLAY request with Digest auth", client->statusUserData);
                        }
                        return false;
                    }

                    if (!parse_rtsp_response(playResponse, playHeadersResp)) {
                        if (client->statusCallback) {
                            client->statusCallback(RTSP_STATUS_ERROR, "Failed to parse PLAY response with Digest auth", client->statusUserData);
                        }
                        return false;
                    }

                    statusCode = playHeadersResp.statusCode;
                }
            }

            // Если все еще ошибка после повторной попытки
            if (statusCode >= 400) {
                if (client->statusCallback) {
                    client->statusCallback(RTSP_STATUS_ERROR, errorMsg.c_str(), client->statusUserData);
                }
                return false;
            }
        }

        // Парсинг Range заголовка из PLAY ответа
        if (!playHeadersResp.range.empty()) {
            // Range: npt=0.000-123.456 или npt=now-
            // Можно использовать для синхронизации времени
            size_t nptPos = playHeadersResp.range.find("npt=");
            if (nptPos != std::string::npos) {
                size_t nptStart = nptPos + 4;
                size_t nptEnd = playHeadersResp.range.find_first_of("- \r\n", nptStart);
                if (nptEnd == std::string::npos) nptEnd = playHeadersResp.range.length();
                std::string nptValue = playHeadersResp.range.substr(nptStart, nptEnd - nptStart);
                // Можно сохранить для синхронизации
            }
        }

        // Парсинг RTP-Info заголовка из PLAY ответа
        // RTP-Info: url=rtsp://server/trackID=0;seq=12345;rtptime=987654321
        if (!playHeadersResp.rtpInfo.empty()) {
            std::string rtpInfo = playHeadersResp.rtpInfo;

            // Парсинг для каждого потока (может быть несколько через запятую)
            std::istringstream rtpInfoStream(rtpInfo);
            std::string streamInfo;
            while (std::getline(rtpInfoStream, streamInfo, ',')) {
                // Удаление пробелов
                while (!streamInfo.empty() && streamInfo[0] == ' ') {
                    streamInfo.erase(0, 1);
                }

                // Парсинг seq (sequence number)
                size_t seqPos = streamInfo.find("seq=");
                if (seqPos != std::string::npos) {
                    size_t seqStart = seqPos + 4;
                    size_t seqEnd = streamInfo.find_first_of("; \r\n", seqStart);
                    if (seqEnd == std::string::npos) seqEnd = streamInfo.length();
                    try {
                        uint16_t seq = static_cast<uint16_t>(std::stoi(streamInfo.substr(seqStart, seqEnd - seqStart)));
                        // Можно использовать для синхронизации sequence number
                    } catch (...) {
                        // Игнорируем ошибки парсинга
                    }
                }

                // Парсинг rtptime (RTP timestamp)
                size_t rtptimePos = streamInfo.find("rtptime=");
                if (rtptimePos != std::string::npos) {
                    size_t rtptimeStart = rtptimePos + 8;
                    size_t rtptimeEnd = streamInfo.find_first_of("; \r\n", rtptimeStart);
                    if (rtptimeEnd == std::string::npos) rtptimeEnd = streamInfo.length();
                    try {
                        uint32_t rtptime = static_cast<uint32_t>(std::stoul(streamInfo.substr(rtptimeStart, rtptimeEnd - rtptimeStart)));
                        // Можно использовать для синхронизации RTP timestamp
                    } catch (...) {
                        // Игнорируем ошибки парсинга
                    }
                }

                // Парсинг url (track URL)
                size_t urlPos = streamInfo.find("url=");
                if (urlPos != std::string::npos) {
                    size_t urlStart = urlPos + 4;
                    size_t urlEnd = streamInfo.find_first_of("; \r\n", urlStart);
                    if (urlEnd == std::string::npos) urlEnd = streamInfo.length();
                    std::string trackUrl = streamInfo.substr(urlStart, urlEnd - urlStart);
                    // Можно использовать для сопоставления с потоками
                }
            }
        }

        client->playing = true;
        client->status = RTSP_STATUS_PLAYING;

        // Запуск потока для приема RTP пакетов
        if (client->rtpThread.joinable()) {
            client->rtpThread.join();
        }
        client->rtpThread = std::thread(receive_rtp_thread, client);
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

        // Отправка PAUSE запроса с поддержкой Digest Authentication
        if (client->rtspSocket != INVALID_SOCKET && !client->sessionId.empty()) {
            // Формируем полный RTSP URL для PAUSE запроса
            std::string fullRtspUrl = build_rtsp_url(client->rtspUrl);
            
            std::ostringstream pauseHeaders;
            pauseHeaders << "CSeq: " << client->cseq++ << "\r\n";
            pauseHeaders << "Session: " << client->sessionId << "\r\n";
            if (!client->username.empty() && !client->password.empty()) {
                if (client->useDigestAuth) {
                    pauseHeaders << "Authorization: " << generate_digest_auth(
                        "PAUSE",
                        fullRtspUrl,
                        client->username,
                        client->password,
                        client->digestParams,
                        client->digestNc++
                    ) << "\r\n";
                } else {
                    pauseHeaders << "Authorization: " << generate_basic_auth(client->username, client->password) << "\r\n";
                }
            }
            pauseHeaders << "User-Agent: IP-CSS RTSP Client\r\n";

            std::string pauseResponse;
            if (send_rtsp_request(client->rtspSocket, "PAUSE", fullRtspUrl,
                                 pauseHeaders.str(), "", pauseResponse)) {
                // Парсинг ответа PAUSE с обработкой 401
                RTSPResponseHeaders pauseRespHeaders;
                if (parse_rtsp_response(pauseResponse, pauseRespHeaders)) {
                    if (pauseRespHeaders.statusCode == 401 && !pauseRespHeaders.wwwAuthenticate.empty() &&
                        !client->username.empty() && !client->password.empty()) {
                        DigestAuthParams newParams;
                        if (parse_www_authenticate(pauseRespHeaders.wwwAuthenticate, newParams)) {
                            if (newParams.stale || !client->useDigestAuth) {
                                client->digestParams = newParams;
                                client->useDigestAuth = true;
                                client->digestNc = 1;
                            }

                            // Повторная отправка PAUSE с Digest Authentication
                            pauseHeaders.str("");
                            pauseHeaders << "CSeq: " << client->cseq++ << "\r\n";
                            pauseHeaders << "Session: " << client->sessionId << "\r\n";
                            pauseHeaders << "Authorization: " << generate_digest_auth(
                                "PAUSE",
                                fullRtspUrl,
                                client->username,
                                client->password,
                                client->digestParams,
                                client->digestNc++
                            ) << "\r\n";
                            pauseHeaders << "User-Agent: IP-CSS RTSP Client\r\n";

                            send_rtsp_request(client->rtspSocket, "PAUSE", fullRtspUrl,
                                             pauseHeaders.str(), "", pauseResponse);
                        }
                    }
                }
            }
        }
    }

    // Ожидание завершения потока приема RTP
    if (client->rtpThread.joinable()) {
        client->rtpThread.join();
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

        if (client->rtspSocket != INVALID_SOCKET && !client->sessionId.empty()) {
            // Формируем полный RTSP URL для PAUSE запроса (теардаун)
            std::string fullRtspUrl = build_rtsp_url(client->rtspUrl);
            
            std::ostringstream pauseHeaders;
            pauseHeaders << "CSeq: " << client->cseq++ << "\r\n";
            pauseHeaders << "Session: " << client->sessionId << "\r\n";
            if (!client->username.empty() && !client->password.empty()) {
                if (client->useDigestAuth) {
                    pauseHeaders << "Authorization: " << generate_digest_auth(
                        "PAUSE",
                        fullRtspUrl,
                        client->username,
                        client->password,
                        client->digestParams,
                        client->digestNc++
                    ) << "\r\n";
                } else {
                    pauseHeaders << "Authorization: " << generate_basic_auth(client->username, client->password) << "\r\n";
                }
            }
            pauseHeaders << "User-Agent: IP-CSS RTSP Client\r\n";

            std::string pauseResponse;
            if (!send_rtsp_request(client->rtspSocket, "PAUSE", fullRtspUrl,
                                  pauseHeaders.str(), "", pauseResponse)) {
                return false;
            }

            RTSPResponseHeaders pauseRespHeaders;
            if (!parse_rtsp_response(pauseResponse, pauseRespHeaders)) {
                return false;
            }

            int statusCode = pauseRespHeaders.statusCode;
            std::string wwwAuthPause = pauseRespHeaders.wwwAuthenticate;

            // Обработка 401 Unauthorized с Digest Authentication
            if (statusCode == 401 && !wwwAuthPause.empty() && !client->username.empty() && !client->password.empty()) {
                DigestAuthParams newParams;
                if (parse_www_authenticate(wwwAuthPause, newParams)) {
                    if (newParams.stale || !client->useDigestAuth) {
                        client->digestParams = newParams;
                        client->useDigestAuth = true;
                        client->digestNc = 1;
                    }

                    // Повторная отправка PAUSE с Digest Authentication
                    pauseHeaders.str("");
                    pauseHeaders << "CSeq: " << client->cseq++ << "\r\n";
                    pauseHeaders << "Session: " << client->sessionId << "\r\n";
                    pauseHeaders << "Authorization: " << generate_digest_auth(
                        "PAUSE",
                        fullRtspUrl,
                        client->username,
                        client->password,
                        client->digestParams,
                        client->digestNc++
                    ) << "\r\n";
                    pauseHeaders << "User-Agent: IP-CSS RTSP Client\r\n";

                    if (!send_rtsp_request(client->rtspSocket, "PAUSE", fullRtspUrl,
                                          pauseHeaders.str(), "", pauseResponse)) {
                        return false;
                    }

                    if (!parse_rtsp_response(pauseResponse, pauseRespHeaders)) {
                        return false;
                    }

                    statusCode = pauseRespHeaders.statusCode;
                }
            }

            if (statusCode != 200) {
                return false;
            }
        }

        client->shouldStop = true;
        client->playing = false;
    }

    // Ожидание завершения потока приема RTP
    if (client->rtpThread.joinable()) {
        client->rtpThread.join();
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

// Forward declaration для автоматического переподключения
static void reconnect_thread_func(RTSPClient* client);

// Функция для установки параметров автоматического переподключения
void rtsp_client_set_reconnect_params(RTSPClient* client, const RTSPReconnectParams* params) {
    if (!client) return;

    bool shouldStartThread = false;

    {
        std::lock_guard<std::mutex> lock(client->mutex);

        // Останавливаем существующий поток переподключения, если он запущен
        if (client->reconnectThread.joinable()) {
            client->reconnectEnabled = false;
        }

        if (params) {
            client->reconnectParams = *params;
            client->reconnectEnabled = params->enabled;
            shouldStartThread = params->enabled;
        } else {
            // Параметры по умолчанию
            client->reconnectParams.enabled = false;
            client->reconnectParams.maxRetries = 5;
            client->reconnectParams.initialDelayMs = 1000;
            client->reconnectParams.maxDelayMs = 30000;
            client->reconnectParams.backoffMultiplier = 2.0f;
            client->reconnectEnabled = false;
            shouldStartThread = false;
        }
    }

    // Ожидаем завершения существующего потока вне блокировки
    if (client->reconnectThread.joinable()) {
        client->reconnectThread.join();
    }

    // Запускаем новый поток переподключения, если он включен
    if (shouldStartThread) {
        std::lock_guard<std::mutex> lock(client->mutex);
        if (!client->reconnectThread.joinable()) {
            client->reconnectThread = std::thread(reconnect_thread_func, client);
        }
    }
}

// Вспомогательная функция для автоматического переподключения
static void reconnect_thread_func(RTSPClient* client) {
    if (!client || !client->reconnectEnabled) return;

    int attempt = 0;
    int delay = client->reconnectParams.initialDelayMs;

    while (!client->shouldStop && client->reconnectEnabled) {
        // Проверяем, нужно ли переподключение
        if (client->connected && client->status != RTSP_STATUS_ERROR) {
            std::this_thread::sleep_for(std::chrono::milliseconds(1000));
            continue;
        }

        // Проверяем лимит попыток
        if (client->reconnectParams.maxRetries > 0 && attempt >= client->reconnectParams.maxRetries) {
            if (client->statusCallback) {
                client->statusCallback(RTSP_STATUS_ERROR, "Max reconnection attempts reached", client->statusUserData);
            }
            client->reconnectEnabled = false;
            break;
        }

        client->isReconnecting = true;
        attempt++;

        if (client->statusCallback) {
            std::string msg = "Reconnecting (attempt " + std::to_string(attempt) + ")...";
            client->statusCallback(RTSP_STATUS_CONNECTING, msg.c_str(), client->statusUserData);
        }

        // Попытка переподключения
        bool success = rtsp_client_connect(client, client->url.c_str(),
                                          client->username.c_str(),
                                          client->password.c_str(),
                                          5000);

        if (success && client->playing) {
            // Если было воспроизведение, возобновляем
            rtsp_client_play(client);
        }

        client->isReconnecting = false;

        if (success) {
            // Успешное переподключение
            attempt = 0;
            delay = client->reconnectParams.initialDelayMs;
            continue;
        }

        // Ожидание перед следующей попыткой с экспоненциальной задержкой
        std::this_thread::sleep_for(std::chrono::milliseconds(delay));
        int newDelay = static_cast<int>(delay * client->reconnectParams.backoffMultiplier);
        delay = (newDelay < client->reconnectParams.maxDelayMs) ? newDelay : client->reconnectParams.maxDelayMs;
    }

    client->isReconnecting = false;
}

// Модифицируем rtsp_client_connect для запуска потока переподключения
// Это будет сделано через проверку в существующей функции connect

} // extern "C"

