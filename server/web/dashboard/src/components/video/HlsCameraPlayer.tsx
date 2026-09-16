import { useCallback, useEffect, useRef, useState } from 'react';
import Hls from 'hls.js';
import {
  cameraHlsPlaylistUrl,
  startCameraStream,
  stopCameraStream,
} from '../../lib/api';
import { createHlsLivePlayerConfig } from '../../lib/hlsPlayerConfig';

interface HlsCameraPlayerProps {
  cameraId: string;
  cameraName: string;
  className?: string;
}

export function HlsCameraPlayer({ cameraId, cameraName, className = '' }: HlsCameraPlayerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const [status, setStatus] = useState<'idle' | 'loading' | 'playing' | 'error'>('idle');
  const [error, setError] = useState<string | null>(null);
  const [active, setActive] = useState(false);

  const teardown = useCallback(() => {
    if (hlsRef.current) {
      hlsRef.current.destroy();
      hlsRef.current = null;
    }
    const video = videoRef.current;
    if (video) {
      video.removeAttribute('src');
      video.load();
    }
  }, []);

  const stop = useCallback(async () => {
    teardown();
    setActive(false);
    setStatus('idle');
    setError(null);
    try {
      await stopCameraStream(cameraId);
    } catch {
      // stream may already be stopped
    }
  }, [cameraId, teardown]);

  const start = useCallback(async () => {
    setStatus('loading');
    setError(null);
    try {
      await startCameraStream(cameraId);
      const video = videoRef.current;
      if (!video) return;

      const src = cameraHlsPlaylistUrl(cameraId);

      if (Hls.isSupported()) {
        teardown();
        const hls = new Hls(createHlsLivePlayerConfig());
        hlsRef.current = hls;
        hls.loadSource(src);
        hls.attachMedia(video);
        hls.on(Hls.Events.MANIFEST_PARSED, () => {
          void video.play().catch(() => undefined);
          setStatus('playing');
        });
        hls.on(Hls.Events.ERROR, (_event, data) => {
          if (data.fatal) {
            setStatus('error');
            setError(data.type || 'HLS playback error');
          }
        });
      } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
        video.src = src;
        video.addEventListener('loadedmetadata', () => {
          void video.play().catch(() => undefined);
          setStatus('playing');
        }, { once: true });
      } else {
        throw new Error('HLS не поддерживается в этом браузере');
      }

      setActive(true);
    } catch (e) {
      setStatus('error');
      setError(e instanceof Error ? e.message : 'Не удалось запустить поток');
      teardown();
    }
  }, [cameraId, teardown]);

  useEffect(() => {
    return () => {
      teardown();
      void stopCameraStream(cameraId).catch(() => undefined);
    };
  }, [cameraId, teardown]);

  return (
    <div className={`rounded-lg overflow-hidden bg-black ${className}`}>
      <video
        ref={videoRef}
        className="w-full aspect-video bg-black"
        muted
        playsInline
        controls={active}
        aria-label={`Поток ${cameraName}`}
      />
      <div className="flex items-center justify-between gap-2 p-2 bg-gray-900 text-white text-sm">
        <span className="truncate">{cameraName}</span>
        <div className="flex gap-2 shrink-0">
          {!active ? (
            <button
              type="button"
              onClick={() => void start()}
              disabled={status === 'loading'}
              className="px-2 py-1 rounded bg-primary-600 hover:bg-primary-700 disabled:opacity-50"
            >
              {status === 'loading' ? 'Подключение…' : 'Смотреть'}
            </button>
          ) : (
            <button
              type="button"
              onClick={() => void stop()}
              className="px-2 py-1 rounded bg-gray-600 hover:bg-gray-500"
            >
              Стоп
            </button>
          )}
        </div>
      </div>
      {error && (
        <p className="text-xs text-red-400 px-2 pb-2 bg-gray-900">{error}</p>
      )}
    </div>
  );
}
