'use client';

import React, { useRef, useEffect, useState } from 'react';
import { Box, Paper, Typography, CircularProgress, Button } from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';
import Hls from 'hls.js';
import type { Camera } from '@/types';

interface VideoPlayerProps {
  camera: Camera;
  autoPlay?: boolean;
  controls?: boolean;
  width?: string | number;
  height?: string | number;
  streamType?: 'hls' | 'webrtc' | 'auto';
}

export default function VideoPlayer({
  camera,
  autoPlay = true,
  controls = true,
  width = '100%',
  height = 'auto',
  streamType = 'hls',
}: VideoPlayerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);

  // Генерация URL потока
  const getStreamUrl = (): string => {
    const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';
    
    // Поддерживаем разные типы потоков
    switch (streamType) {
      case 'hls':
        // HLS поток (требует медиа-сервер для конвертации RTSP -> HLS)
        return `${API_URL}/streams/${camera.id}/hls/playlist.m3u8`;
      case 'webrtc':
        // WebRTC поток (требует WebRTC сервер)
        return `${API_URL}/streams/${camera.id}/webrtc`;
      default:
        // Прямой RTSP URL (не будет работать в браузере, нужна конвертация)
        return camera.url;
    }
  };

  // Инициализация HLS
  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    const streamUrl = getStreamUrl();

    // Если это HLS поток
    if (streamType === 'hls' && Hls.isSupported()) {
      const hls = new Hls({
        enableWorker: true,
        lowLatencyMode: true,
        backBufferLength: 90,
      });

      hlsRef.current = hls;

      hls.loadSource(streamUrl);
      hls.attachMedia(video);

      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        setLoading(false);
        setError(null);
        if (autoPlay) {
          video.play().catch((err) => {
            console.error('Error playing video:', err);
            setError('Не удалось воспроизвести видео');
          });
        }
      });

      hls.on(Hls.Events.ERROR, (event, data) => {
        if (data.fatal) {
          switch (data.type) {
            case Hls.ErrorTypes.NETWORK_ERROR:
              console.error('HLS Network Error:', data);
              setError('Ошибка сети. Попробуйте переподключиться.');
              setLoading(false);
              break;
            case Hls.ErrorTypes.MEDIA_ERROR:
              console.error('HLS Media Error:', data);
              // Попытка восстановления
              hls.recoverMediaError();
              break;
            default:
              console.error('HLS Fatal Error:', data);
              setError('Ошибка воспроизведения видео');
              setLoading(false);
              hls.destroy();
              break;
          }
        }
      });

      return () => {
        if (hlsRef.current) {
          hlsRef.current.destroy();
          hlsRef.current = null;
        }
      };
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
      // Нативная поддержка HLS (Safari)
      video.src = streamUrl;
      setLoading(false);
    } else {
      // Fallback для других форматов
      video.src = streamUrl;
      
      const handleLoadStart = () => {
        setLoading(true);
        setError(null);
      };

      const handleLoadedData = () => {
        setLoading(false);
        setIsPlaying(true);
      };

      const handleError = () => {
        setLoading(false);
        setError('Не удалось загрузить видео. Возможно, поток недоступен.');
      };

      video.addEventListener('loadstart', handleLoadStart);
      video.addEventListener('loadeddata', handleLoadedData);
      video.addEventListener('error', handleError);
      video.addEventListener('play', () => setIsPlaying(true));
      video.addEventListener('pause', () => setIsPlaying(false));

      return () => {
        video.removeEventListener('loadstart', handleLoadStart);
        video.removeEventListener('loadeddata', handleLoadedData);
        video.removeEventListener('error', handleError);
        video.removeEventListener('play', () => setIsPlaying(true));
        video.removeEventListener('pause', () => setIsPlaying(false));
      };
    }
  }, [camera, streamType, autoPlay]);

  // Очистка при размонтировании
  useEffect(() => {
    return () => {
      if (hlsRef.current) {
        hlsRef.current.destroy();
        hlsRef.current = null;
      }
    };
  }, []);

  const handleRetry = () => {
    setError(null);
    setLoading(true);
    const video = videoRef.current;
    if (video) {
      video.load();
    }
  };

  const streamUrl = getStreamUrl();

  return (
    <Paper sx={{ p: 2, position: 'relative' }}>
      {loading && (
        <Box
          sx={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            zIndex: 1,
          }}
        >
          <CircularProgress />
        </Box>
      )}
      {error ? (
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            minHeight: 300,
            gap: 2,
          }}
        >
          <Typography color="error" variant="body1" align="center">
            {error}
          </Typography>
          <Button
            variant="contained"
            startIcon={<RefreshIcon />}
            onClick={handleRetry}
          >
            Переподключиться
          </Button>
          <Typography variant="caption" color="text.secondary" align="center">
            Камера: {camera.name}
          </Typography>
        </Box>
      ) : (
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
            src={videoUrl}
            controls={controls}
            autoPlay={autoPlay}
            muted
            style={{
              width: '100%',
              height: '100%',
              display: loading ? 'none' : 'block',
            }}
          />
          {loading && !error && (
            <Box
              sx={{
                position: 'absolute',
                top: '50%',
                left: '50%',
                transform: 'translate(-50%, -50%)',
                color: '#fff',
                textAlign: 'center',
              }}
            >
              <CircularProgress size={40} sx={{ mb: 2, color: '#fff' }} />
              <Typography variant="body2">Подключение к камере...</Typography>
              <Typography variant="caption" sx={{ display: 'block', mt: 1, opacity: 0.7 }}>
                {camera.name}
              </Typography>
            </Box>
          )}
        </Box>
      )}
    </Paper>
  );
}

