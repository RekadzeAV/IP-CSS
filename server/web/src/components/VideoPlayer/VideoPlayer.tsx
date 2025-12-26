'use client';

import React, { useRef, useEffect, useState } from 'react';
import { Box, Paper, Typography, CircularProgress } from '@mui/material';
import type { Camera } from '@/types';

interface VideoPlayerProps {
  camera: Camera;
  autoPlay?: boolean;
  controls?: boolean;
  width?: string | number;
  height?: string | number;
}

export default function VideoPlayer({
  camera,
  autoPlay = true,
  controls = true,
  width = '100%',
  height = 'auto',
}: VideoPlayerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    // Для RTSP потока нужна трансляция через HLS или WebRTC
    // В реальном проекте здесь будет интеграция с медиа-сервером
    // Для демонстрации используем placeholder

    const handleLoadStart = () => {
      setLoading(true);
      setError(null);
    };

    const handleLoadedData = () => {
      setLoading(false);
    };

    const handleError = () => {
      setLoading(false);
      setError('Не удалось загрузить видео');
    };

    video.addEventListener('loadstart', handleLoadStart);
    video.addEventListener('loadeddata', handleLoadedData);
    video.addEventListener('error', handleError);

    return () => {
      video.removeEventListener('loadstart', handleLoadStart);
      video.removeEventListener('loadeddata', handleLoadedData);
      video.removeEventListener('error', handleError);
    };
  }, [camera]);

  // В реальном проекте URL будет преобразован через медиа-сервер
  // Например: /api/streams/{cameraId}/hls/playlist.m3u8
  const videoUrl = `/api/streams/${camera.id}`;

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
            alignItems: 'center',
            justifyContent: 'center',
            minHeight: 200,
          }}
        >
          <Typography color="error">{error}</Typography>
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
          {!videoRef.current?.readyState && (
            <Box
              sx={{
                position: 'absolute',
                top: '50%',
                left: '50%',
                transform: 'translate(-50%, -50%)',
                color: '#fff',
              }}
            >
              <Typography>Подключение к камере...</Typography>
            </Box>
          )}
        </Box>
      )}
    </Paper>
  );
}

