'use client';

import { Box, CircularProgress, Typography } from '@mui/material';
import Hls from 'hls.js';
import React, { useEffect, useRef, useState } from 'react';
import type { Recording } from '@/types';

interface RecordingPlayerProps {
  recording: Recording;
  autoPlay?: boolean;
  controls?: boolean;
  width?: string | number;
  height?: string | number;
  quality?: 'low' | 'medium' | 'high' | 'ultra';
}

export default function RecordingPlayer({
  recording,
  autoPlay = true,
  controls = true,
  width = '100%',
  height = 'auto',
  quality = 'medium',
}: RecordingPlayerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const video = videoRef.current;
    if (!video || !recording.filePath) return;

    let hls: Hls | null = null;
    let mounted = true;

    const cleanup = () => {
      if (hls) {
        hls.destroy();
        hlsRef.current = null;
      }
    };

    const initializePlayback = async () => {
      if (!mounted) return;

      try {
        setLoading(true);
        setError(null);

        // Получаем HLS URL для записи
        const hlsUrl = `/api/v1/recordings/${recording.id}/hls/playlist.m3u8?quality=${quality}`;
        const isNativeHlsSupported = video.canPlayType('application/vnd.apple.mpegurl');

        if (Hls.isSupported()) {
          hls = new Hls({
            enableWorker: true,
            lowLatencyMode: false, // Для записей не нужна низкая задержка
            backBufferLength: 90,
            maxBufferLength: 30,
            maxMaxBufferLength: 60,
            debug: false,
          });

          hls.loadSource(hlsUrl);
          hls.attachMedia(video);

          hls.on(Hls.Events.MANIFEST_PARSED, () => {
            if (mounted) {
              setLoading(false);
              if (autoPlay) {
                video.play().catch((e: unknown) => {
                  console.error('Error auto-playing video:', e);
                  if (mounted) {
                    setError('Не удалось автоматически начать воспроизведение');
                  }
                });
              }
            }
          });

          hls.on(Hls.Events.ERROR, (_event: string, data: any) => {
            if (!mounted) return;

            if (data.fatal) {
              switch (data.type) {
                case Hls.ErrorTypes.NETWORK_ERROR:
                  console.error('Network error, trying to recover...');
                  setError('Ошибка сети. Попытка восстановления...');
                  hls?.startLoad();
                  break;
                case Hls.ErrorTypes.MEDIA_ERROR:
                  console.error('Media error, trying to recover...');
                  try {
                    hls?.recoverMediaError();
                  } catch (e) {
                    console.error('Failed to recover from media error:', e);
                    setError('Ошибка воспроизведения медиа');
                  }
                  break;
                default:
                  console.error('Fatal error');
                  setError('Критическая ошибка воспроизведения');
                  cleanup();
                  break;
              }
            }
          });
        } else if (isNativeHlsSupported) {
          // Используем нативную поддержку HLS (Safari)
          video.src = hlsUrl;

          const handleLoadedMetadata = () => {
            if (mounted) {
              setLoading(false);
              if (autoPlay) {
                video.play().catch((e: unknown) => {
                  console.error('Error auto-playing video:', e);
                });
              }
            }
          };

          video.addEventListener('loadedmetadata', handleLoadedMetadata);

          video.addEventListener('error', () => {
            if (mounted) {
              setError('Ошибка загрузки видео');
            }
          });
        } else {
          throw new Error('HLS не поддерживается в этом браузере');
        }

        hlsRef.current = hls;
      } catch (err) {
        console.error('Error initializing playback:', err);
        if (mounted) {
          setError(err instanceof Error ? err.message : 'Ошибка инициализации воспроизведения');
          setLoading(false);
        }
      }
    };

    initializePlayback();

    return () => {
      mounted = false;
      cleanup();
    };
  }, [recording.id, recording.filePath, autoPlay, quality]);

  if (error) {
    return (
      <Box
        sx={{
          width: '100%',
          aspectRatio: '16/9',
          backgroundColor: '#000',
          display: 'flex',
          flexDirection: 'column',
          alignItems: 'center',
          justifyContent: 'center',
          borderRadius: 1,
          p: 3,
        }}
      >
        <Typography color="error" variant="h6" align="center">
          {error}
        </Typography>
        <Typography variant="body2" color="text.secondary" align="center" sx={{ mt: 1 }}>
          Запись: {recording.id.substring(0, 8)}
        </Typography>
      </Box>
    );
  }

  return (
    <Box
      sx={{
        position: 'relative',
        width,
        height,
        backgroundColor: '#000',
        borderRadius: 1,
        overflow: 'hidden',
      }}
    >
      <video
        ref={videoRef}
        controls={controls}
        autoPlay={autoPlay}
        muted
        playsInline
        style={{
          width: '100%',
          height: '100%',
          display: 'block',
        }}
      />
      {loading && (
        <Box
          sx={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            color: '#fff',
            zIndex: 1,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: 1,
          }}
        >
          <CircularProgress size={24} sx={{ color: '#fff' }} />
          <Typography variant="body2">Загрузка...</Typography>
        </Box>
      )}
    </Box>
  );
}
