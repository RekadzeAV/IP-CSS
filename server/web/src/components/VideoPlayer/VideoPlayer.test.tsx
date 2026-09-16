import { render, waitFor } from '@testing-library/react';
import Hls from 'hls.js';
import VideoPlayer from './VideoPlayer';

const startStreamMock = jest.fn();
const getHlsUrlMock = jest.fn();
const getAdaptiveHlsUrlMock = jest.fn();

const hlsLoadSourceMock = jest.fn();
const hlsAttachMediaMock = jest.fn();
const hlsOnMock = jest.fn();
const hlsDestroyMock = jest.fn();

jest.mock('hls.js', () => ({
  __esModule: true,
  default: Object.assign(
    jest.fn().mockImplementation(() => ({
      loadSource: hlsLoadSourceMock,
      attachMedia: hlsAttachMediaMock,
      on: hlsOnMock,
      destroy: hlsDestroyMock,
      startLoad: jest.fn(),
      recoverMediaError: jest.fn(),
      levels: [],
    })),
    {
      isSupported: jest.fn(() => true),
      Events: {
        MANIFEST_PARSED: 'MANIFEST_PARSED',
        ERROR: 'ERROR',
      },
      ErrorTypes: {
        NETWORK_ERROR: 'NETWORK_ERROR',
        MEDIA_ERROR: 'MEDIA_ERROR',
      },
    }
  ),
}));

jest.mock('@/services/streamService', () => ({
  streamService: {
    startStream: (...args: unknown[]) => startStreamMock(...args),
    getHlsUrl: (...args: unknown[]) => getHlsUrlMock(...args),
    getAdaptiveHlsUrl: (...args: unknown[]) => getAdaptiveHlsUrlMock(...args),
    stopStream: jest.fn(),
    captureScreenshot: jest.fn(),
    setStreamQuality: jest.fn(),
  },
}));

jest.mock('@/utils/webrtc', () => ({
  initWebRTCConnection: jest.fn().mockRejectedValue(new Error('webrtc failed')),
  getWebRTCStats: jest.fn(),
}));

describe('VideoPlayer', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    startStreamMock.mockResolvedValue(undefined);
    getHlsUrlMock.mockReturnValue('http://localhost/hls/stream.m3u8');
    getAdaptiveHlsUrlMock.mockReturnValue('http://localhost/hls/master.m3u8');

    Object.defineProperty(HTMLMediaElement.prototype, 'play', {
      configurable: true,
      value: jest.fn().mockResolvedValue(undefined),
    });
    Object.defineProperty(HTMLMediaElement.prototype, 'pause', {
      configurable: true,
      value: jest.fn(),
    });
  });

  it('falls back to HLS when WebRTC initialization fails', async () => {
    render(
      <VideoPlayer
        camera={{
          id: 'cam-1',
          name: 'Camera 1',
          url: 'rtsp://example/stream',
          status: 'ONLINE',
          fps: 25,
          bitrate: 1024,
          codec: 'H264',
        } as never}
        streamType="webrtc"
      />
    );

    await waitFor(() => {
      expect(startStreamMock).toHaveBeenCalledWith('cam-1');
      expect(Hls).toHaveBeenCalled();
      expect(hlsLoadSourceMock).toHaveBeenCalledWith('http://localhost/hls/master.m3u8');
    });
  });

  it('uses adaptive HLS playlist when streamType is rtsp', async () => {
    render(
      <VideoPlayer
        camera={{
          id: 'cam-1',
          name: 'Camera 1',
          url: 'rtsp://example/stream',
          status: 'ONLINE',
          fps: 25,
          bitrate: 1024,
          codec: 'H264',
        } as never}
        streamType="rtsp"
      />
    );

    await waitFor(() => {
      expect(startStreamMock).toHaveBeenCalledWith('cam-1');
      expect(Hls).toHaveBeenCalled();
      // RTSP ветка должна использовать адаптивный (master) HLS плейлист
      expect(hlsLoadSourceMock).toHaveBeenCalledWith('http://localhost/hls/master.m3u8');
    });
  });
});
