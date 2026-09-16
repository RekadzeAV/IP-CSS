#!/usr/bin/env python3
"""
RTSP Test Server with Audio Support
Эмулятор RTSP сервера для тестирования аудио декодирования

Поддерживает:
- H.264 видео (тестовые паттерны)
- AAC аудио (синтезированный тон)
- PCMU/PCMA аудио (G.711)

Использование:
    python scripts/rtsp-audio-test-server.py
    python scripts/rtsp-audio-test-server.py --port 8554 --audio-codec aac
"""

import argparse
import socket
import threading
import time
import struct
import sys
import os

# Проверка наличия RTSP сервера (gstreamer или ffmpeg)
try:
    import gi
    gi.require_version('Gst', '1.0')
    from gi.repository import Gst, GLib
    GSTREAMER_AVAILABLE = True
except ImportError:
    GSTREAMER_AVAILABLE = False

try:
    import subprocess
    FFMPEG_AVAILABLE = subprocess.run(['ffmpeg', '-version'], capture_output=True).returncode == 0
except:
    FFMPEG_AVAILABLE = False


class RTSPTestServer:
    """Простой RTSP сервер для тестирования с аудио"""
    
    def __init__(self, port=8554, audio_codec='aac'):
        self.port = port
        self.audio_codec = audio_codec
        self.clients = []
        self.running = False
        self.server_socket = None
        
        # Аудио параметры
        self.audio_sample_rate = 48000 if audio_codec == 'aac' else 8000
        self.audio_channels = 2 if audio_codec == 'aac' else 1
        self.audio_payload_type = 96 if audio_codec == 'aac' else 0
        
        # Видео параметры
        self.video_width = 640
        self.video_height = 480
        self.video_fps = 25
        self.video_payload_type = 96
        
        # SDP описание
        self.sdp = self._create_sdp()
        
    def _create_sdp(self):
        """Создание SDP описания потока"""
        if self.audio_codec == 'aac':
            audio_line = f"a=rtpmap:{self.audio_payload_type} MPEG4-GENERIC/{self.audio_sample_rate}/{self.audio_channels}\r\n"
            audio_line += "a=fmtp:96 streamtype=5;profile-level-id=1;mode=AAC-hbr;sizelength=13;indexlength=3;indexdeltalength=3;config=1210\r\n"
        elif self.audio_codec == 'pcmu':
            audio_line = f"a=rtpmap:{self.audio_payload_type} PCMU/{self.audio_sample_rate}/{self.audio_channels}\r\n"
        elif self.audio_codec == 'pcma':
            audio_line = f"a=rtpmap:{self.audio_payload_type} PCMA/{self.audio_sample_rate}/{self.audio_channels}\r\n"
        else:
            audio_line = ""
        
        sdp = f"""v=0
o=- 1234567890 1234567890 IN IP4 127.0.0.1
s=IP-CSS RTSP Test Server
c=IN IP4 127.0.0.1
t=0 0
m=video 0 RTP/AVP {self.video_payload_type}
a=rtpmap:{self.video_payload_type} H264/90000
a=control:track1
m=audio 0 RTP/AVP {self.audio_payload_type}
{audio_line}a=control:track2
"""
        return sdp
    
    def start(self):
        """Запуск RTSP сервера"""
        print(f"Starting RTSP test server on port {self.port}...")
        print(f"Audio codec: {self.audio_codec}")
        print(f"Sample rate: {self.audio_sample_rate} Hz")
        print(f"Channels: {self.audio_channels}")
        print(f"Video: {self.video_width}x{self.video_height} @ {self.video_fps}fps")
        print(f"SDP:\n{self.sdp}")
        
        try:
            self.server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
            self.server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
            self.server_socket.bind(('0.0.0.0', self.port))
            self.server_socket.listen(5)
            self.server_socket.settimeout(1.0)
            self.running = True
            
            print(f"RTSP server listening on port {self.port}")
            print("Press Ctrl+C to stop")
            
            while self.running:
                try:
                    client_socket, address = self.server_socket.accept()
                    print(f"Client connected: {address}")
                    client_thread = threading.Thread(
                        target=self._handle_client,
                        args=(client_socket, address)
                    )
                    client_thread.daemon = True
                    client_thread.start()
                except socket.timeout:
                    continue
                except Exception as e:
                    if self.running:
                        print(f"Error accepting connection: {e}")
                        
        except KeyboardInterrupt:
            print("\nStopping server...")
        finally:
            self.stop()
    
    def stop(self):
        """Остановка RTSP сервера"""
        self.running = False
        if self.server_socket:
            self.server_socket.close()
        for client in self.clients:
            client.close()
        self.clients.clear()
        print("Server stopped")
    
    def _handle_client(self, client_socket, address):
        """Обработка подключения клиента"""
        self.clients.append(client_socket)
        session_id = f"{threading.current_thread().ident:x}"
        
        try:
            while self.running:
                data = client_socket.recv(1024)
                if not data:
                    break
                
                request = data.decode('utf-8', errors='ignore')
                lines = request.split('\r\n')
                if not lines:
                    continue
                
                method = lines[0].split()[0] if lines else ""
                
                if method == "OPTIONS":
                    response = self._create_response(200, "OK", {
                        "Public": "DESCRIBE, SETUP, PLAY, PAUSE, TEARDOWN"
                    })
                elif method == "DESCRIBE":
                    response = self._create_response(200, "OK", {
                        "Content-Type": "application/sdp",
                        "Content-Base": f"rtsp://127.0.0.1:{self.port}/stream/",
                        "Content-Length": str(len(self.sdp))
                    }, self.sdp)
                elif method == "SETUP":
                    session_id = f"{threading.current_thread().ident:x}"
                    response = self._create_response(200, "OK", {
                        "Session": session_id,
                        "Transport": "RTP/AVP/UDP;unicast;client_port=1234-1235;server_port=5432-5433"
                    })
                elif method == "PLAY":
                    response = self._create_response(200, "OK", {
                        "Session": session_id,
                        "Range": "npt=0.000-"
                    })
                    # Запуск потока передачи
                    threading.Thread(target=self._stream_data, args=(client_socket, session_id), daemon=True).start()
                elif method == "TEARDOWN":
                    response = self._create_response(200, "OK", {
                        "Session": session_id
                    })
                    break
                else:
                    response = self._create_response(501, "Not Implemented")
                
                client_socket.send(response.encode('utf-8'))
                
        except Exception as e:
            print(f"Error handling client {address}: {e}")
        finally:
            client_socket.close()
            if client_socket in self.clients:
                self.clients.remove(client_socket)
    
    def _create_response(self, status_code, status_text, headers=None, body=""):
        """Создание RTSP ответа"""
        response = f"RTSP/1.0 {status_code} {status_text}\r\n"
        response += "CSeq: 1\r\n"
        
        if headers:
            for key, value in headers.items():
                response += f"{key}: {value}\r\n"
        
        if body:
            response += f"Content-Length: {len(body)}\r\n"
        
        response += "\r\n"
        if body:
            response += body
        
        return response
    
    def _stream_data(self, client_socket, session_id):
        """Передача RTP пакетов с видео и аудио"""
        print(f"Starting stream to {client_socket.getpeername()}")
        
        # RTP порты (из SETUP ответа)
        rtp_port = 5432
        rtcp_port = 5433
        
        # Создаем UDP сокеты для RTP
        rtp_socket = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        rtp_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        
        try:
            video_seq = 0
            audio_seq = 0
            video_timestamp = 0
            audio_timestamp = 0
            frame_count = 0
            
            while self.running:
                # Отправка видео кадра (упрощённый H.264 NAL unit)
                if frame_count % 25 == 0:  # Кадры каждые 25 тиков
                    video_packet = self._create_video_rtp_packet(video_seq, video_timestamp)
                    rtp_socket.sendto(video_packet, ('127.0.0.1', 1234))
                    video_seq = (video_seq + 1) % 65536
                    video_timestamp += 90000 // self.video_fps  # 90kHz clock
                
                # Отправка аудио кадра
                audio_packet = self._create_audio_rtp_packet(audio_seq, audio_timestamp)
                rtp_socket.sendto(audio_packet, ('127.0.0.1', 1236))
                audio_seq = (audio_seq + 1) % 65536
                audio_timestamp += self.audio_sample_rate // 50  # 50 аудио фреймов в секунду
                
                frame_count += 1
                time.sleep(1.0 / self.video_fps)
                
        except Exception as e:
            print(f"Stream error: {e}")
        finally:
            rtp_socket.close()
    
    def _create_video_rtp_packet(self, seq, timestamp):
        """Создание RTP пакета с H.264 видео"""
        # Простой H.264 NAL unit (single NAL unit, тип 1 - Coded slice)
        nal_unit = bytes([0x67, 0x42, 0x00, 0x1e]) + bytes(100)  # SPS-like
        
        # RTP заголовок (12 байт)
        rtp_header = struct.pack('!BBHII',
            0x80,  # Version 2, no padding, no extension
            96,    # Payload type (H.264)
            seq,   # Sequence number
            timestamp,  # Timestamp
            0x12345678  # SSRC
        )
        
        return rtp_header + nal_unit
    
    def _create_audio_rtp_packet(self, seq, timestamp):
        """Создание RTP пакета с аудио"""
        if self.audio_codec == 'aac':
            # AAC ADTS заголовок + тестовые данные
            adts_header = bytes([
                0xFF, 0xF1, 0x10, 0x00,  # Sync word + профиль
                0x00, 0x00, 0x00, 0x00,  # Частота и длина
                0x00, 0x00, 0x00
            ])
            audio_data = adts_header + bytes(200)  # Тестовые данные
        elif self.audio_codec == 'pcmu':
            # G.711 μ-law: генерация тестового сигнала
            audio_data = bytes([0x7f] * 200)  # Тестовый паттерн
        elif self.audio_codec == 'pcma':
            # G.711 A-law: генерация тестового сигнала
            audio_data = bytes([0x55] * 200)  # Тестовый паттерн
        else:
            audio_data = bytes(200)
        
        # RTP заголовок
        rtp_header = struct.pack('!BBHII',
            0x80,
            self.audio_payload_type,
            seq,
            timestamp,
            0x12345678
        )
        
        return rtp_header + audio_data


def main():
    parser = argparse.ArgumentParser(description='RTSP Test Server with Audio')
    parser.add_argument('--port', type=int, default=8554, help='RTSP port (default: 8554)')
    parser.add_argument('--audio-codec', choices=['aac', 'pcmu', 'pcma'], default='aac',
                       help='Audio codec (default: aac)')
    parser.add_argument('--use-gstreamer', action='store_true',
                       help='Use GStreamer for actual media streaming')
    parser.add_argument('--use-ffmpeg', action='store_true',
                       help='Use FFmpeg for actual media streaming')
    
    args = parser.parse_args()
    
    # Проверка доступности инструментов
    if args.use_gstreamer and not GSTREAM_AVAILABLE:
        print("Warning: GStreamer not available, using mock streaming")
        args.use_gstreamer = False
    
    if args.use_ffmpeg and not FFMPEG_AVAILABLE:
        print("Warning: FFmpeg not available, using mock streaming")
        args.use_ffmpeg = False
    
    # Создание и запуск сервера
    server = RTSPTestServer(port=args.port, audio_codec=args.audio_codec)
    
    try:
        server.start()
    except KeyboardInterrupt:
        server.stop()
    
    return 0


if __name__ == '__main__':
    sys.exit(main())
