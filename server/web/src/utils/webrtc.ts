/**
 * WebRTC утилиты для видеопотоков
 */

export interface WebRTCConfig {
  iceServers: RTCConfiguration['iceServers'];
  onTrack?: (event: RTCTrackEvent) => void;
  onIceCandidate?: (candidate: RTCIceCandidate) => void;
  onConnectionStateChange?: (state: RTCPeerConnectionState) => void;
}

export class WebRTCClient {
  private pc: RTCPeerConnection | null = null;
  private config: WebRTCConfig;
  private stream: MediaStream | null = null;

  constructor(config: WebRTCConfig) {
    this.config = config;
  }

  /**
   * Создать RTCPeerConnection
   */
  createPeerConnection(): RTCPeerConnection {
    this.pc = new RTCPeerConnection({
      iceServers: this.config.iceServers || [
        { urls: 'stun:stun.l.google.com:19302' },
        { urls: 'stun:stun1.l.google.com:19302' },
      ],
    });

    // Обработка ICE candidates
    this.pc.onicecandidate = (event) => {
      if (event.candidate && this.config.onIceCandidate) {
        this.config.onIceCandidate(event.candidate);
      }
    };

    // Обработка изменения состояния соединения
    this.pc.onconnectionstatechange = () => {
      if (this.pc && this.config.onConnectionStateChange) {
        this.config.onConnectionStateChange(this.pc.connectionState);
      }
    };

    // Обработка входящих треков
    this.pc.ontrack = (event) => {
      if (this.config.onTrack) {
        this.config.onTrack(event);
      }
      // Добавляем трек в локальный stream
      if (event.streams && event.streams[0]) {
        this.stream = event.streams[0];
      }
    };

    return this.pc;
  }

  /**
   * Создать offer для WebRTC соединения
   */
  async createOffer(): Promise<RTCSessionDescriptionInit> {
    if (!this.pc) {
      throw new Error('PeerConnection not created');
    }

    const offer = await this.pc.createOffer({
      offerToReceiveAudio: true,
      offerToReceiveVideo: true,
    });

    await this.pc.setLocalDescription(offer);
    return offer;
  }

  /**
   * Установить remote description
   */
  async setRemoteDescription(description: RTCSessionDescriptionInit): Promise<void> {
    if (!this.pc) {
      throw new Error('PeerConnection not created');
    }
    await this.pc.setRemoteDescription(new RTCSessionDescription(description));
  }

  /**
   * Добавить ICE candidate
   */
  async addIceCandidate(candidate: RTCIceCandidateInit): Promise<void> {
    if (!this.pc) {
      throw new Error('PeerConnection not created');
    }
    await this.pc.addIceCandidate(new RTCIceCandidate(candidate));
  }

  /**
   * Создать answer для WebRTC соединения
   */
  async createAnswer(): Promise<RTCSessionDescriptionInit> {
    if (!this.pc) {
      throw new Error('PeerConnection not created');
    }

    const answer = await this.pc.createAnswer();
    await this.pc.setLocalDescription(answer);
    return answer;
  }

  /**
   * Получить локальный stream
   */
  getStream(): MediaStream | null {
    return this.stream;
  }

  /**
   * Закрыть соединение
   */
  close(): void {
    if (this.pc) {
      this.pc.close();
      this.pc = null;
    }
    if (this.stream) {
      this.stream.getTracks().forEach((track) => track.stop());
      this.stream = null;
    }
  }

  /**
   * Получить состояние соединения
   */
  getConnectionState(): RTCPeerConnectionState | null {
    return this.pc?.connectionState || null;
  }
}

/**
 * Инициализация WebRTC соединения через медиа-сервер
 */
export async function initWebRTCConnection(
  cameraId: string,
  apiUrl: string,
  config: WebRTCConfig
): Promise<MediaStream> {
  const client = new WebRTCClient(config);
  const pc = client.createPeerConnection();

  try {
    // Создаем offer
    const offer = await client.createOffer();

    // Отправляем offer на сервер через API
    const response = await fetch(`${apiUrl}/cameras/${cameraId}/webrtc/offer`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({ offer }),
    });

    if (!response.ok) {
      throw new Error('Failed to create WebRTC connection');
    }

    const data = await response.json();

    // Устанавливаем remote description (answer от сервера)
    await client.setRemoteDescription(data.answer);

    // Добавляем ICE candidates от сервера
    if (data.iceCandidates) {
      for (const candidate of data.iceCandidates) {
        await client.addIceCandidate(candidate);
      }
    }

    // Ждем получения stream
    return new Promise((resolve, reject) => {
      const timeout = setTimeout(() => {
        reject(new Error('WebRTC connection timeout'));
      }, 10000);

      const checkStream = () => {
        const stream = client.getStream();
        if (stream) {
          clearTimeout(timeout);
          resolve(stream);
        } else {
          setTimeout(checkStream, 100);
        }
      };

      checkStream();
    });
  } catch (error) {
    client.close();
    throw error;
  }
}

