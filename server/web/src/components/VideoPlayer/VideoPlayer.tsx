'use client';

import React, { useRef, useEffect, useState, useCallback } from 'react';
import { Box, Paper, Typography, CircularProgress, IconButton, Menu, MenuItem, Tooltip } from '@mui/material';
import { PlayArrow, Pause, Stop, Fullscreen, FullscreenExit, PhotoCamera, Settings } from '@mui/icons-material';
import Hls from 'hls.js';
import type { Camera } from '@/types';
import { streamService } from '@/services/streamService';

interface VideoPlayerProps {
  camera: Camera;
  autoPlay?: boolean;
  controls?: boolean;
  width?: string | number;
  height?: string | number;
}

type StreamQuality = 'low' | 'medium' | 'high' | 'ultra';

export default function VideoPlayer({
  camera,
  autoPlay = true,
  controls = true,
  width = '100%',
  height = 'auto',
}: VideoPlayerProps) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [streamStarted, setStreamStarted] = useState(false);
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [quality, setQuality] = useState<StreamQuality>('medium');
  const [qualityMenuAnchor, setQualityMenuAnchor] = useState<null | HTMLElement>(null);

  // Инициализация и управление HLS потоком
  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    let hls: Hls | null = null;
    let mounted = true;

    const initializeStream = async () => {
      try {
        setLoading(true);
        setError(null);

        // Проверяем, поддерживает ли браузер HLS нативно
        const isNativeHlsSupported = video.canPlayType('application/vnd.apple.mpegurl');

        // Получаем HLS URL
        const hlsUrl = streamService.getHlsUrl(camera.id);

        if (Hls.isSupported()) {
          // Используем HLS.js для браузеров, которые не поддерживают HLS нативно
          hls = new Hls({
            enableWorker: true,
            lowLatencyMode: false,
            backBufferLength: 90,
          });

          hls.loadSource(hlsUrl);
          hls.attachMedia(video);

          hls.on(Hls.Events.MANIFEST_PARSED, () => {
            if (mounted) {
              setLoading(false);
              if (autoPlay) {
                video.play().catch((e: unknown) => {
                  console.error('Error auto-playing video:', e);
                });
              }
            }
          });

          hls.on(Hls.Events.ERROR, (_event: string, data: any) => {
            if (data.fatal) {
              switch (data.type) {
                case Hls.ErrorTypes.NETWORK_ERROR:
                  console.error('Network error, trying to recover...');
                  if (mounted) {
                    setError('Ошибка сети. Попытка восстановления...');
                  }
                  hls?.startLoad();
                  break;
                case Hls.ErrorTypes.MEDIA_ERROR:
                  console.error('Media error, trying to recover...');
                  hls?.recoverMediaError();
                  break;
                default:
                  console.error('Fatal error, destroying HLS instance');
                  if (mounted) {
                    setError('Критическая ошибка воспроизведения');
                  }
                  hls?.destroy();
                  break;
              }
            }
          });
        } else if (isNativeHlsSupported) {
          // Используем нативную поддержку HLS (Safari)
          video.src = hlsUrl;
          video.addEventListener('loadedmetadata', () => {
            if (mounted) {
              setLoading(false);
              if (autoPlay) {
                video.play().catch((e: unknown) => {
                  console.error('Error auto-playing video:', e);
                });
              }
            }
          });
        } else {
          throw new Error('HLS не поддерживается в этом браузере');
        }

        hlsRef.current = hls;
        setStreamStarted(true);
      } catch (err) {
        console.error('Error initializing stream:', err);
        if (mounted) {
          setError(err instanceof Error ? err.message : 'Ошибка инициализации потока');
          setLoading(false);
        }
      }
    };

    initializeStream();

    const handlePlay = () => {
      setIsPlaying(true);
    };

    const handlePause = () => {
      setIsPlaying(false);
    };

    const handleWaiting = () => {
      setLoading(true);
    };

    const handleCanPlay = () => {
      setLoading(false);
    };

    const handleError = () => {
      setLoading(false);
      setError('Ошибка воспроизведения видео');
    };

    video.addEventListener('play', handlePlay);
    video.addEventListener('pause', handlePause);
    video.addEventListener('waiting', handleWaiting);
    video.addEventListener('canplay', handleCanPlay);
    video.addEventListener('error', handleError);

    return () => {
      mounted = false;
      video.removeEventListener('play', handlePlay);
      video.removeEventListener('pause', handlePause);
      video.removeEventListener('waiting', handleWaiting);
      video.removeEventListener('canplay', handleCanPlay);
      video.removeEventListener('error', handleError);

      if (hls) {
        hls.destroy();
        hlsRef.current = null;
      }
    };
  }, [camera.id, autoPlay]);

  // Управление воспроизведением
  const handlePlay = useCallback(() => {
    const video = videoRef.current;
    if (video) {
      video.play().catch((err: unknown) => {
        console.error('Error playing video:', err);
        setError('Ошибка воспроизведения');
      });
    }
  }, []);

  const handlePause = useCallback(() => {
    const video = videoRef.current;
    if (video) {
      video.pause();
    }
  }, []);

  const handleStop = useCallback(async () => {
    const video = videoRef.current;
    if (video) {
      video.pause();
      video.currentTime = 0;
    }
    try {
      await streamService.stopStream(camera.id);
      setStreamStarted(false);
    } catch (err) {
      console.error('Error stopping stream:', err);
    }
  }, [camera.id]);

  const handleFullscreen = useCallback(() => {
    const video = videoRef.current;
    if (!video) return;

    if (!document.fullscreenElement) {
      video.requestFullscreen().then(() => setIsFullscreen(true)).catch((err: unknown) => {
        console.error('Error entering fullscreen:', err);
      });
    } else {
      document.exitFullscreen().then(() => setIsFullscreen(false)).catch((err: unknown) => {
        console.error('Error exiting fullscreen:', err);
      });
    }
  }, []);

  useEffect(() => {
    const handleFullscreenChange = () => {
      setIsFullscreen(!!document.fullscreenElement);
    };
    document.addEventListener('fullscreenchange', handleFullscreenChange);
    return () => document.removeEventListener('fullscreenchange', handleFullscreenChange);
  }, []);

  const handleScreenshot = useCallback(async () => {
    try {
      const response = await fetch(`/api/v1/cameras/${camera.id}/stream/screenshot`, {
        method: 'POST',
        headers: {
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
      });
      if (response.ok) {
        const data = await response.json();
        if (data.success && data.data) {
          // Можно открыть изображение в новой вкладке или скачать
          window.open(data.data, '_blank');
        }
      }
    } catch (err) {
      console.error('Error capturing screenshot:', err);
    }
  }, [camera.id]);

  const handleQualityChange = useCallback(async (newQuality: StreamQuality) => {
    setQuality(newQuality);
    setQualityMenuAnchor(null);
    // TODO: Отправить запрос на изменение качества через API
    // await streamService.setStreamQuality(camera.id, newQuality);
  }, [camera.id]);

  return (
    <Paper sx={{ p: 2, position: 'relative' }}>
      {loading && (
        <Box
          sx={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            zIndex: 2,
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
          <Typography color="error" variant="h6">
            {error}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Камера: {camera.name}
          </Typography>
        </Box>
      ) : (
        <>
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
            {loading && streamStarted && (
              <Box
                sx={{
                  position: 'absolute',
                  top: '50%',
                  left: '50%',
                  transform: 'translate(-50%, -50%)',
                  color: '#fff',
                  zIndex: 1,
                }}
              >
                <Typography>Подключение к камере...</Typography>
              </Box>
            )}
          </Box>
          {!controls && (
            <Box
              sx={{
                display: 'flex',
                justifyContent: 'center',
                gap: 1,
                mt: 2,
              }}
            >
              <Tooltip title={isPlaying ? "Pause" : "Play"}>
                <IconButton
                  color="primary"
                  onClick={isPlaying ? handlePause : handlePlay}
                  disabled={loading || !!error}
                >
                  {isPlaying ? <Pause /> : <PlayArrow />}
                </IconButton>
              </Tooltip>
              <Tooltip title="Stop">
                <IconButton
                  color="primary"
                  onClick={handleStop}
                  disabled={loading || !!error || !streamStarted}
                >
                  <Stop />
                </IconButton>
              </Tooltip>
              <Tooltip title="Screenshot">
                <IconButton
                  color="primary"
                  onClick={handleScreenshot}
                  disabled={loading || !!error}
                >
                  <PhotoCamera />
                </IconButton>
              </Tooltip>
              <Tooltip title={isFullscreen ? "Exit Fullscreen" : "Fullscreen"}>
                <IconButton
                  color="primary"
                  onClick={handleFullscreen}
                >
                  {isFullscreen ? <FullscreenExit /> : <Fullscreen />}
                </IconButton>
              </Tooltip>
              <Tooltip title="Quality">
                <IconButton
                  color="primary"
                  onClick={(e: React.MouseEvent<HTMLButtonElement>) => setQualityMenuAnchor(e.currentTarget)}
                >
                  <Settings />
                </IconButton>
              </Tooltip>
              <Menu
                anchorEl={qualityMenuAnchor}
                open={!!qualityMenuAnchor}
                onClose={() => setQualityMenuAnchor(null)}
              >
                <MenuItem onClick={() => handleQualityChange('low')} selected={quality === 'low'}>
                  Low (640x360)
                </MenuItem>
                <MenuItem onClick={() => handleQualityChange('medium')} selected={quality === 'medium'}>
                  Medium (1280x720)
                </MenuItem>
                <MenuItem onClick={() => handleQualityChange('high')} selected={quality === 'high'}>
                  High (1920x1080)
                </MenuItem>
                <MenuItem onClick={() => handleQualityChange('ultra')} selected={quality === 'ultra'}>
                  Ultra (1920x1080)
                </MenuItem>
              </Menu>
            </Box>
          )}
        </>
      )}
    </Paper>
  );
}
