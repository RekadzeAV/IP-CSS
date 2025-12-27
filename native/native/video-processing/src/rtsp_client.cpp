#include "rtsp_client.h"
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

// Платформо-специфичные заголовки для сокетов
#ifdef _WIN32
    #include <winsock2.h>
    #include <ws2tcpip.h>
    #pragma comment(lib, "ws2_32.lib")
    typedef int socklen_t;
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
    int width;
    int height;
    int fps;
    SOCKET rtpSocket;
    SOCKET rtcpSocket;
    uint16_t rtpSequence;
    uint32_t rtpSSRC;
    uint32_t rtpTimestamp;
    std::vector<uint8_t> buffer;

    RTPStream() : clientRtpPort(0), clientRtcpPort(0), serverRtpPort(0), serverRtcpPort(0),
                  payloadType(96), clockRate(90000), width(0), height(0), fps(0),
                  rtpSocket(INVALID_SOCKET), rtcpSocket(INVALID_SOCKET),
                  rtpSequence(0), rtpSSRC(0), rtpTimestamp(0) {}
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
    std::thread rtpThread;

    // RTSP протокол
    RTSPUrl rtspUrl;
    SOCKET rtspSocket;
    std::string sessionId;
    int cseq;
    std::string baseUrl;
    std::vector<RTPStream> rtpStreams;

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
                   statusUserData(nullptr), rtspSocket(INVALID_SOCKET), cseq(1)
#ifdef ENABLE_FFMPEG
                   , formatContext(nullptr), videoCodecContext(nullptr), audioCodecContext(nullptr),
                   swsContext(nullptr), videoStreamIndex(-1), audioStreamIndex(-1)
#endif
    {
#ifdef _WIN32
        WSADATA wsaData;
        WSAStartup(MAKEWORD(2, 2), &wsaData);
#endif
    }

    ~RTSPClient() {
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
};

struct RTSPStream {
    RTSPStreamType type;
    int width;
    int height;
    int fps;
    std::string codec;
};

// Вспомогательные функции для работы с RTSP протоколом

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

    // Получение адреса
    struct sockaddr_in serverAddr;
    memset(&serverAddr, 0, sizeof(serverAddr));
    serverAddr.sin_family = AF_INET;
    serverAddr.sin_port = htons(port);

    struct hostent* hostEntry = gethostbyname(host.c_str());
    if (!hostEntry) {
        close(sock);
        return INVALID_SOCKET;
    }

    memcpy(&serverAddr.sin_addr, hostEntry->h_addr_list[0], hostEntry->h_length);

    // Подключение
    if (connect(sock, (struct sockaddr*)&serverAddr, sizeof(serverAddr)) == SOCKET_ERROR) {
        close(sock);
        return INVALID_SOCKET;
    }

    return sock;
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
    std::istringstream sdpStream(sdp);
    std::string line;
    RTPStream* currentStream = nullptr;

    while (std::getline(sdpStream, line)) {
        // Удаление \r если есть
        if (!line.empty() && line.back() == '\r') {
            line.pop_back();
        }

        if (line.empty()) continue;

        if (line[0] == 'm' && line[1] == '=') {
            // Media description: m=video 0 RTP/AVP 96
            std::istringstream mediaStream(line.substr(2));
            std::string mediaType, port, protocol, payloadType;
            mediaStream >> mediaType >> port >> protocol >> payloadType;

            if (mediaType == "video" || mediaType == "audio") {
                RTPStream stream;
                stream.type = (mediaType == "video") ? RTSP_STREAM_VIDEO : RTSP_STREAM_AUDIO;
                stream.payloadType = std::stoi(payloadType);
                currentStream = &stream;
                streams.push_back(stream);
                currentStream = &streams.back();
            }
        } else if (line[0] == 'a' && line[1] == '=' && currentStream) {
            // Attribute
            size_t colonPos = line.find(':');
            if (colonPos == std::string::npos) {
                std::string attr = line.substr(2);
                if (attr == "control") {
                    // a=control:trackID=0
                    size_t trackPos = attr.find("trackID=");
                    if (trackPos != std::string::npos) {
                        // Обработка control URL
                    }
                }
            } else {
                std::string attrName = line.substr(2, colonPos - 2);
                std::string attrValue = line.substr(colonPos + 1);

                if (attrName == "rtpmap") {
                    // a=rtpmap:96 H264/90000
                    std::istringstream rtpmapStream(attrValue);
                    int pt;
                    std::string codecInfo;
                    rtpmapStream >> pt >> codecInfo;

                    size_t slashPos = codecInfo.find('/');
                    if (slashPos != std::string::npos) {
                        currentStream->codec = codecInfo.substr(0, slashPos);
                        currentStream->clockRate = std::stoi(codecInfo.substr(slashPos + 1));
                    }
                } else if (attrName == "control") {
                    currentStream->controlUrl = attrValue;
                } else if (attrName == "fmtp") {
                    // a=fmtp:96 profile-level-id=...;sprop-parameter-sets=...
                    // Парсинг параметров кодека
                }
            }
        }
    }

    // Создание RTSPStream для каждого найденного потока
    for (const auto& rtpStream : streams) {
        RTSPStream* stream = new RTSPStream();
        stream->type = rtpStream.type;
        stream->codec = rtpStream.codec;
        stream->width = rtpStream.width;
        stream->height = rtpStream.height;
        stream->fps = rtpStream.fps;
        client->streams.push_back(stream);
    }

    return !streams.empty();
}

// Парсинг RTSP ответа для извлечения Session ID
static bool parse_rtsp_response(const std::string& response, int& statusCode, std::string& sessionId) {
    std::istringstream responseStream(response);
    std::string statusLine;
    std::getline(responseStream, statusLine);

    // RTSP/1.0 200 OK
    size_t space1 = statusLine.find(' ');
    if (space1 == std::string::npos) return false;

    size_t space2 = statusLine.find(' ', space1 + 1);
    if (space2 == std::string::npos) return false;

    statusCode = std::stoi(statusLine.substr(space1 + 1, space2 - space1 - 1));

    // Поиск Session ID
    size_t sessionPos = response.find("Session:");
    if (sessionPos != std::string::npos) {
        size_t valueStart = sessionPos + 8;
        while (valueStart < response.length() && response[valueStart] == ' ') valueStart++;
        size_t valueEnd = response.find("\r\n", valueStart);
        if (valueEnd != std::string::npos) {
            sessionId = response.substr(valueStart, valueEnd - valueStart);
            // Удаление параметров после точки с запятой
            size_t semicolonPos = sessionId.find(';');
            if (semicolonPos != std::string::npos) {
                sessionId = sessionId.substr(0, semicolonPos);
            }
        }
    }

    return true;
}

// Обработка RTP пакета
static void process_rtp_packet(const uint8_t* data, int size, RTPStream& stream, RTSPClient* client) {
    if (size < 12) return; // Минимальный размер RTP заголовка

    // Парсинг RTP заголовка
    uint8_t version = (data[0] >> 6) & 0x3;
    uint8_t padding = (data[0] >> 5) & 0x1;
    uint8_t extension = (data[0] >> 4) & 0x1;
    uint8_t csrcCount = data[0] & 0xF;
    uint8_t marker = (data[1] >> 7) & 0x1;
    uint8_t payloadType = data[1] & 0x7F;
    uint16_t sequence = (data[2] << 8) | data[3];
    uint32_t timestamp = (data[4] << 24) | (data[5] << 16) | (data[6] << 8) | data[7];
    uint32_t ssrc = (data[8] << 24) | (data[9] << 16) | (data[10] << 8) | data[11];

    int headerSize = 12 + (csrcCount * 4);
    if (extension) {
        uint16_t extensionLength = (data[headerSize + 2] << 8) | data[headerSize + 3];
        headerSize += 4 + (extensionLength * 4);
    }

    if (size <= headerSize) return;

    int payloadSize = size - headerSize;
    const uint8_t* payload = data + headerSize;

    // Создание RTSPFrame
    RTSPFrame* frame = new RTSPFrame();
    frame->data = new uint8_t[payloadSize];
    memcpy(frame->data, payload, payloadSize);
    frame->size = payloadSize;
    frame->timestamp = timestamp;
    frame->type = stream.type;
    frame->width = stream.width;
    frame->height = stream.height;

    // Вызов callback
    if (stream.type == RTSP_STREAM_VIDEO && client->videoCallback) {
        client->videoCallback(frame, client->videoUserData);
    } else if (stream.type == RTSP_STREAM_AUDIO && client->audioCallback) {
        client->audioCallback(frame, client->audioUserData);
    } else {
        // Освобождение если callback не установлен
        delete[] frame->data;
        delete frame;
    }
}

// Функция для приема RTP пакетов в отдельном потоке
static void receive_rtp_thread(RTSPClient* client) {
    if (!client) return;

    fd_set readfds;
    struct timeval tv;

    while (!client->shouldStop && client->playing) {
        FD_ZERO(&readfds);
        int maxFd = 0;

        for (auto& stream : client->rtpStreams) {
            if (stream.rtpSocket != INVALID_SOCKET) {
                FD_SET(stream.rtpSocket, &readfds);
                if (stream.rtpSocket > maxFd) maxFd = stream.rtpSocket;
            }
            if (stream.rtcpSocket != INVALID_SOCKET) {
                FD_SET(stream.rtcpSocket, &readfds);
                if (stream.rtcpSocket > maxFd) maxFd = stream.rtcpSocket;
            }
        }

        if (maxFd == 0) break;

        tv.tv_sec = 1;
        tv.tv_usec = 0;

        int activity = select(maxFd + 1, &readfds, nullptr, nullptr, &tv);
        if (activity <= 0) continue;

        for (auto& stream : client->rtpStreams) {
            if (stream.rtpSocket != INVALID_SOCKET && FD_ISSET(stream.rtpSocket, &readfds)) {
                uint8_t buffer[65536];
                struct sockaddr_in fromAddr;
                socklen_t fromLen = sizeof(fromAddr);

                int received = recvfrom(stream.rtpSocket, (char*)buffer, sizeof(buffer), 0,
                                       (struct sockaddr*)&fromAddr, &fromLen);
                if (received > 0) {
                    process_rtp_packet(buffer, received, stream, client);
                }
            }

            if (stream.rtcpSocket != INVALID_SOCKET && FD_ISSET(stream.rtcpSocket, &readfds)) {
                uint8_t buffer[1500];
                struct sockaddr_in fromAddr;
                socklen_t fromLen = sizeof(fromAddr);

                recvfrom(stream.rtcpSocket, (char*)buffer, sizeof(buffer), 0,
                        (struct sockaddr*)&fromAddr, &fromLen);
                // Обработка RTCP пакетов (можно добавить позже)
            }
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

    // Отправка OPTIONS запроса (опционально, для проверки соединения)
    std::ostringstream optionsHeaders;
    optionsHeaders << "CSeq: " << client->cseq++ << "\r\n";
    if (!client->username.empty() && !client->password.empty()) {
        optionsHeaders << "Authorization: " << generate_basic_auth(client->username, client->password) << "\r\n";
    }
    optionsHeaders << "User-Agent: IP-CSS RTSP Client\r\n";

    std::string optionsResponse;
    if (!send_rtsp_request(client->rtspSocket, "OPTIONS", client->rtspUrl.path,
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
    if (!send_rtsp_request(client->rtspSocket, "DESCRIBE", client->rtspUrl.path,
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
    if (!parse_rtsp_response(describeResponse, statusCode, sessionId)) {
        close(client->rtspSocket);
        client->rtspSocket = INVALID_SOCKET;
        client->status = RTSP_STATUS_ERROR;
        if (client->statusCallback) {
            client->statusCallback(RTSP_STATUS_ERROR, "Failed to parse DESCRIBE response", client->statusUserData);
        }
        return false;
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
        // Создание UDP сокетов для RTP/RTCP
        stream.rtpSocket = create_udp_socket(stream.clientRtpPort);
        stream.rtcpSocket = create_udp_socket(stream.clientRtcpPort);

        if (stream.rtpSocket == INVALID_SOCKET || stream.rtcpSocket == INVALID_SOCKET) {
            close(client->rtspSocket);
            client->rtspSocket = INVALID_SOCKET;
            client->status = RTSP_STATUS_ERROR;
            if (client->statusCallback) {
                client->statusCallback(RTSP_STATUS_ERROR, "Failed to create RTP sockets", client->statusUserData);
            }
            return false;
        }

        // Формирование control URL
        std::string controlUrl = stream.controlUrl;
        if (controlUrl.empty() || controlUrl[0] != '/') {
            controlUrl = client->rtspUrl.path + "/" + controlUrl;
        }

        // Отправка SETUP запроса
        std::ostringstream setupHeaders;
        setupHeaders << "CSeq: " << client->cseq++ << "\r\n";
        setupHeaders << "Transport: RTP/AVP/UDP;unicast;client_port="
                     << stream.clientRtpPort << "-" << stream.clientRtcpPort << "\r\n";
        if (!client->username.empty() && !client->password.empty()) {
            setupHeaders << "Authorization: " << generate_basic_auth(client->username, client->password) << "\r\n";
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

        // Парсинг ответа SETUP
        if (!parse_rtsp_response(setupResponse, statusCode, sessionId)) {
            close(client->rtspSocket);
            client->rtspSocket = INVALID_SOCKET;
            client->status = RTSP_STATUS_ERROR;
            if (client->statusCallback) {
                client->statusCallback(RTSP_STATUS_ERROR, "Failed to parse SETUP response", client->statusUserData);
            }
            return false;
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

        // Парсинг Transport из ответа для получения server портов
        size_t transportPos = setupResponse.find("Transport:");
        if (transportPos != std::string::npos) {
            std::string transportLine = setupResponse.substr(transportPos);
            size_t lineEnd = transportLine.find("\r\n");
            if (lineEnd != std::string::npos) {
                transportLine = transportLine.substr(0, lineEnd);
                // Парсинг server_port=xxxx-xxxx
                size_t serverPortPos = transportLine.find("server_port=");
                if (serverPortPos != std::string::npos) {
                    size_t portStart = serverPortPos + 12;
                    size_t portEnd = transportLine.find("-", portStart);
                    if (portEnd != std::string::npos) {
                        stream.serverRtpPort = std::stoi(transportLine.substr(portStart, portEnd - portStart));
                        size_t rtcpStart = portEnd + 1;
                        size_t rtcpEnd = transportLine.find(";", rtcpStart);
                        if (rtcpEnd == std::string::npos) rtcpEnd = transportLine.length();
                        stream.serverRtcpPort = std::stoi(transportLine.substr(rtcpStart, rtcpEnd - rtcpStart));
                    }
                }
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

    // Отправка TEARDOWN запроса
    if (client->rtspSocket != INVALID_SOCKET && client->connected) {
        std::ostringstream teardownHeaders;
        teardownHeaders << "CSeq: " << client->cseq++ << "\r\n";
        if (!client->sessionId.empty()) {
            teardownHeaders << "Session: " << client->sessionId << "\r\n";
        }
        if (!client->username.empty() && !client->password.empty()) {
            teardownHeaders << "Authorization: " << generate_basic_auth(client->username, client->password) << "\r\n";
        }
        teardownHeaders << "User-Agent: IP-CSS RTSP Client\r\n";

        std::string teardownResponse;
        send_rtsp_request(client->rtspSocket, "TEARDOWN", client->rtspUrl.path,
                         teardownHeaders.str(), "", teardownResponse);

        close(client->rtspSocket);
        client->rtspSocket = INVALID_SOCKET;
    }

    // Закрытие RTP сокетов
    for (auto& stream : client->rtpStreams) {
        if (stream.rtpSocket != INVALID_SOCKET) {
            close(stream.rtpSocket);
            stream.rtpSocket = INVALID_SOCKET;
        }
        if (stream.rtcpSocket != INVALID_SOCKET) {
            close(stream.rtcpSocket);
            stream.rtcpSocket = INVALID_SOCKET;
        }
    }
    client->rtpStreams.clear();

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

        // Отправка PLAY запроса
        std::ostringstream playHeaders;
        playHeaders << "CSeq: " << client->cseq++ << "\r\n";
        if (!client->sessionId.empty()) {
            playHeaders << "Session: " << client->sessionId << "\r\n";
        }
        if (!client->username.empty() && !client->password.empty()) {
            playHeaders << "Authorization: " << generate_basic_auth(client->username, client->password) << "\r\n";
        }
        playHeaders << "User-Agent: IP-CSS RTSP Client\r\n";
        playHeaders << "Range: npt=0.000-\r\n";

        std::string playResponse;
        if (!send_rtsp_request(client->rtspSocket, "PLAY", client->rtspUrl.path,
                              playHeaders.str(), "", playResponse)) {
            if (client->statusCallback) {
                client->statusCallback(RTSP_STATUS_ERROR, "Failed to send PLAY request", client->statusUserData);
            }
            return false;
        }

        // Парсинг ответа PLAY
        int statusCode;
        std::string sessionId;
        if (!parse_rtsp_response(playResponse, statusCode, sessionId)) {
            if (client->statusCallback) {
                client->statusCallback(RTSP_STATUS_ERROR, "Failed to parse PLAY response", client->statusUserData);
            }
            return false;
        }

        if (statusCode != 200) {
            if (client->statusCallback) {
                client->statusCallback(RTSP_STATUS_ERROR, "PLAY request failed", client->statusUserData);
            }
            return false;
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

        // Отправка PAUSE запроса (или можно использовать TEARDOWN)
        if (client->rtspSocket != INVALID_SOCKET && !client->sessionId.empty()) {
            std::ostringstream pauseHeaders;
            pauseHeaders << "CSeq: " << client->cseq++ << "\r\n";
            pauseHeaders << "Session: " << client->sessionId << "\r\n";
            if (!client->username.empty() && !client->password.empty()) {
                pauseHeaders << "Authorization: " << generate_basic_auth(client->username, client->password) << "\r\n";
            }
            pauseHeaders << "User-Agent: IP-CSS RTSP Client\r\n";

            std::string pauseResponse;
            send_rtsp_request(client->rtspSocket, "PAUSE", client->rtspUrl.path,
                             pauseHeaders.str(), "", pauseResponse);
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
            std::ostringstream pauseHeaders;
            pauseHeaders << "CSeq: " << client->cseq++ << "\r\n";
            pauseHeaders << "Session: " << client->sessionId << "\r\n";
            if (!client->username.empty() && !client->password.empty()) {
                pauseHeaders << "Authorization: " << generate_basic_auth(client->username, client->password) << "\r\n";
            }
            pauseHeaders << "User-Agent: IP-CSS RTSP Client\r\n";

            std::string pauseResponse;
            if (!send_rtsp_request(client->rtspSocket, "PAUSE", client->rtspUrl.path,
                                  pauseHeaders.str(), "", pauseResponse)) {
                return false;
            }

            int statusCode;
            std::string sessionId;
            if (parse_rtsp_response(pauseResponse, statusCode, sessionId) && statusCode != 200) {
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

} // extern "C"

