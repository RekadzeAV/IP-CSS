/**
 * WebRTC утилиты для низкой задержки видеопотоков
 */

export interface WebRTCOptions {
  iceServers: RTCIceServer[];
  onTrack?: (event: RTCTrackEvent) => void;
  onConnectionStateChange?: (state: RTCPeerConnectionState) => void;
  onError?: (error: Error) => void;
}

export interface WebRTCStats {
  bytesReceived: number;
  bytesSent: number;
  packetsReceived: number;
  packetsSent: number;
  jitter: number;
  rtt: number;
  framesReceived: number;
  framesDropped: number;
  frameRate?: number;
  bitrate?: number;
}

/**
 * Инициализация WebRTC соединения для видеопотока
 */
export async function initWebRTCConnection(
  cameraId: string,
  apiUrl: string,
  options: WebRTCOptions
): Promise<{ stream: MediaStream; peerConnection: RTCPeerConnection }> {
  const peerConnection = new RTCPeerConnection({
    iceServers: options.iceServers,
    iceCandidatePoolSize: 10,
  });

  const iceCandidatesQueue: RTCIceCandidate[] = [];

  // Обработка локальных ICE candidates
  peerConnection.onicecandidate = (event) => {
    if (event.candidate) {
      iceCandidatesQueue.push(event.candidate);
      // Можно отправить на сервер, если требуется
    }
  };

  // Обработка изменения состояния соединения
  peerConnection.onconnectionstatechange = () => {
    const state = peerConnection.connectionState;
    options.onConnectionStateChange?.(state);

    if (state === 'failed' || state === 'disconnected') {
      options.onError?.(new Error(`WebRTC connection ${state}`));
    }
  };

  // Обработка получения треков
  peerConnection.ontrack = (event) => {
    options.onTrack?.(event);
  };

  // Обработка ошибок ICE
  peerConnection.oniceconnectionstatechange = () => {
    const state = peerConnection.iceConnectionState;
    if (state === 'failed' || state === 'disconnected') {
      options.onError?.(new Error(`ICE connection ${state}`));
    }
  };

  try {
    // Создаем offer
    const offer = await peerConnection.createOffer({
      offerToReceiveVideo: true,
      offerToReceiveAudio: true,
    });

    await peerConnection.setLocalDescription(offer);

    // Отправляем offer на сервер и получаем answer
    const response = await fetch(`${apiUrl}/cameras/${cameraId}/stream/webrtc/offer`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
      },
      credentials: 'include',
      body: JSON.stringify({
        offer: {
          type: offer.type,
          sdp: offer.sdp,
        },
      }),
    });

    if (!response.ok) {
      throw new Error(`Failed to create WebRTC offer: ${response.statusText}`);
    }

    const data = await response.json();
    if (!data.success || !data.data?.answer) {
      throw new Error(data.message || 'Failed to get WebRTC answer');
    }

    const answer = data.data.answer;
    await peerConnection.setRemoteDescription(
      new RTCSessionDescription({
        type: answer.type,
        sdp: answer.sdp,
      })
    );
    // Добавляем ICE candidates из очереди
    while (iceCandidatesQueue.length > 0) {
      const candidate = iceCandidatesQueue.shift();
      if (candidate) {
        try {
          await peerConnection.addIceCandidate(candidate);
        } catch (e) {
          console.warn('Failed to add queued ICE candidate:', e);
        }
      }
    }

    // Обрабатываем ICE candidates от сервера
    if (data.data.iceCandidates) {
      for (const candidate of data.data.iceCandidates) {
        try {
          await peerConnection.addIceCandidate(
            new RTCIceCandidate(candidate)
          );
        } catch (e) {
          console.warn('Failed to add server ICE candidate:', e);
        }
      }
    }

    // Создаем MediaStream из треков
    const stream = new MediaStream();

    // Ждем получения треков
    return new Promise<{ stream: MediaStream; peerConnection: RTCPeerConnection }>((resolve, reject) => {
      const timeout = setTimeout(() => {
        reject(new Error('Timeout waiting for WebRTC tracks'));
      }, 10000);

      peerConnection.ontrack = (event) => {
        clearTimeout(timeout);
        event.streams[0].getTracks().forEach((track) => {
          stream.addTrack(track);
        });
        options.onTrack?.(event);
        resolve({ stream, peerConnection });
      };

      // Если треки уже получены
      peerConnection.getReceivers().forEach((receiver) => {
        if (receiver.track) {
          stream.addTrack(receiver.track);
        }
      });

      if (stream.getTracks().length > 0) {
        clearTimeout(timeout);
        resolve({ stream, peerConnection });
      }
    });
  } catch (error) {
    peerConnection.close();
    throw error;
  }
}

/**
 * Получить статистику WebRTC соединения
 */
export async function getWebRTCStats(
  peerConnection: RTCPeerConnection
): Promise<WebRTCStats | null> {
  try {
    const stats = await peerConnection.getStats();
    let bytesReceived = 0;
    let bytesSent = 0;
    let packetsReceived = 0;
    let packetsSent = 0;
    let jitter = 0;
    let rtt = 0;
    let framesReceived = 0;
    let framesDropped = 0;
    let frameRate = 0;
    let bitrate = 0;

    const getNumericField = (report: RTCStats, field: string): number => {
      const value = (report as unknown as Record<string, unknown>)[field];
      return typeof value === 'number' ? value : 0;
    };

    const getBooleanField = (report: RTCStats, field: string): boolean => {
      const value = (report as unknown as Record<string, unknown>)[field];
      return typeof value === 'boolean' ? value : false;
    };

    stats.forEach((report) => {
      if (report.type === 'inbound-rtp' && 'mediaType' in report && report.mediaType === 'video') {
        bytesReceived += getNumericField(report, 'bytesReceived');
        packetsReceived += getNumericField(report, 'packetsReceived');
        jitter += getNumericField(report, 'jitter');
        framesReceived += getNumericField(report, 'framesReceived');
        framesDropped += getNumericField(report, 'framesDropped');
        frameRate = getNumericField(report, 'framesPerSecond');
      } else if (report.type === 'outbound-rtp' && 'mediaType' in report && report.mediaType === 'video') {
        bytesSent += getNumericField(report, 'bytesSent');
        packetsSent += getNumericField(report, 'packetsSent');
      } else if (report.type === 'candidate-pair' && getBooleanField(report, 'selected')) {
        const currentRoundTripTime = getNumericField(report, 'currentRoundTripTime');
        rtt = currentRoundTripTime > 0 ? currentRoundTripTime * 1000 : 0;
      }
    });

    // Вычисляем битрейт (байты в секунду * 8)
    if (bytesReceived > 0) {
      bitrate = (bytesReceived / 1000) * 8; // kbps
    }

    return {
      bytesReceived,
      bytesSent,
      packetsReceived,
      packetsSent,
      jitter,
      rtt,
      framesReceived,
      framesDropped,
      frameRate,
      bitrate,
    };
  } catch (error) {
    console.error('Error getting WebRTC stats:', error);
    return null;
  }
}

/**
 * Закрыть WebRTC соединение
 */
export function closeWebRTCConnection(peerConnection: RTCPeerConnection): void {
  peerConnection.getReceivers().forEach((receiver) => {
    receiver.track?.stop();
  });
  peerConnection.getSenders().forEach((sender) => {
    if (sender.track) {
      sender.track.stop();
    }
    try {
      peerConnection.removeTrack(sender);
    } catch {
      // Ignore removeTrack errors on already-closed senders.
    }
  });
  peerConnection.close();
}
