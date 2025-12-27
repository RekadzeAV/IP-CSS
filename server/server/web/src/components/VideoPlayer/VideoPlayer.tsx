'use client';

import { streamService } from '@/services/streamService';
import type { Camera } from '@/types';
import { Fullscreen, FullscreenExit, Pause, PhotoCamera, PlayArrow, Refresh, Settings, Stop } from '@mui/icons-material';
import { Box, Chip, CircularProgress, IconButton, Menu, MenuItem, Paper, Tooltip, Typography } from '@mui/material';
import Hls from 'hls.js';
import React, { useCallback, useEffect, useRef, useState } from 'react';

interface VideoPlayerProps {
  camera: Camera;
  autoPlay?: boolean;
  controls?: boolean;
  width?: string | number;
  height?: string | number;
  streamType?: 'hls' | 'webrtc' | 'rtsp'; // Тип стрима: HLS (по умолчанию), WebRTC для низкой задержки, или RTSP для прямой трансляции
}

type StreamQuality = 'low' | 'medium' | 'high' | 'ultra';

type StreamStatus = 'idle' | 'starting' | 'connected' | 'playing' | 'error' | 'stopped';

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
  const pcRef = useRef<RTCPeerConnection | null>(null); // Для WebRTC
  const retryTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);
  const retryCountRef = useRef(0);
  const streamActiveRef = useRef(false); // Используем ref для отслеживания состояния потока
  const maxRetries = 3;

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [streamStatus, setStreamStatus] = useState<StreamStatus>('idle');
  const [isFullscreen, setIsFullscreen] = useState(false);
  const [quality, setQuality] = useState<StreamQuality>('medium');
  const [qualityMenuAnchor, setQualityMenuAnchor] = useState<null | HTMLElement>(null);
  const [streamActive, setStreamActive] = useState(false); // Для отображения в UI
  const [reconnectTrigger, setReconnectTrigger] = useState(0); // Триггер для переподключения

  // Инициализация и управление потоком (HLS или WebRTC)
  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    let hls: Hls | null = null;
    let mounted = true;
    let retryAttempt = 0;

    const cleanup = () => {
      if (retryTimeoutRef.current) {
        clearTimeout(retryTimeoutRef.current);
        retryTimeoutRef.current = null;
      }
      if (hls) {
        hls.destroy();
        hlsRef.current = null;
      }
      if (pcRef.current) {
        pcRef.current.close();
        pcRef.current = null;
      }
    };

    const initializeStream = async () => {
      if (!mounted) return;

      try {
        setLoading(true);
        setError(null);
        setStreamStatus('starting');

        // Сначала запускаем стрим через API
        if (!streamActiveRef.current) {
          try {
            await streamService.startStream(camera.id);
            streamActiveRef.current = true;
            setStreamActive(true);
            setStreamStatus('connected');
          } catch (startError) {
            console.error('Error starting stream via API:', startError);
            if (mounted) {
              setError('Не удалось запустить трансляцию. Проверьте подключение к камере.');
              setStreamStatus('error');
              setLoading(false);
            }
            return;
          }
        }

        if (streamType === 'webrtc') {
          // WebRTC реализация (если нужно добавить позже)
          console.warn('WebRTC streaming not yet implemented, falling back to HLS');
          // TODO: Реализовать WebRTC стриминг
          return;
        }

        if (streamType === 'rtsp') {
          // RTSP поток - в браузерах RTSP не поддерживается напрямую
          // Бэкенд автоматически конвертирует RTSP в HLS, поэтому используем HLS URL
          // Это обеспечивает совместимость с браузерами
          console.log('RTSP stream type selected - using HLS conversion for browser compatibility');

          // Используем HLS URL, который бэкенд генерирует из RTSP потока
          const hlsUrl = streamService.getHlsUrl(camera.id);
          const isNativeHlsSupported = video.canPlayType('application/vnd.apple.mpegurl');

          if (Hls.isSupported()) {
            hls = new Hls({
              enableWorker: true,
              lowLatencyMode: true, // Низкая задержка для RTSP потоков
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
                setStreamStatus('playing');
                retryCountRef.current = 0;

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

                    if (retryAttempt < maxRetries) {
                      retryAttempt++;
                      retryTimeoutRef.current = setTimeout(() => {
                        hls?.startLoad();
                      }, 2000 * retryAttempt);
                    } else {
                      setStreamStatus('error');
                      handleInternalReconnect();
                    }
                    break;
                  case Hls.ErrorTypes.MEDIA_ERROR:
                    console.error('Media error, trying to recover...');
                    try {
                      hls?.recoverMediaError();
                    } catch (e) {
                      console.error('Failed to recover from media error:', e);
                      setStreamStatus('error');
                      handleInternalReconnect();
                    }
                    break;
                  default:
                    console.error('Fatal error, destroying HLS instance');
                    setError('Критическая ошибка воспроизведения');
                    setStreamStatus('error');
                    cleanup();
                    handleInternalReconnect();
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
                setStreamStatus('playing');
                retryCountRef.current = 0;

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
                setStreamStatus('error');
                handleInternalReconnect();
              }
            });
          } else {
            throw new Error('HLS не поддерживается в этом браузере');
          }

          hlsRef.current = hls;
          return;
        }

        // HLS стриминг
        const hlsUrl = streamService.getHlsUrl(camera.id);
        const isNativeHlsSupported = video.canPlayType('application/vnd.apple.mpegurl');

        if (Hls.isSupported()) {
          hls = new Hls({
            enableWorker: true,
            lowLatencyMode: true, // Низкая задержка для RTSP потоков
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
              setStreamStatus('playing');
              retryCountRef.current = 0; // Сбрасываем счетчик повторных попыток

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

                  // Попытка восстановления
                  if (retryAttempt < maxRetries) {
                    retryAttempt++;
                    retryTimeoutRef.current = setTimeout(() => {
                      hls?.startLoad();
                    }, 2000 * retryAttempt); // Экспоненциальная задержка
                  } else {
                    // Если не удалось восстановиться, пытаемся переподключиться
                    setStreamStatus('error');
                    handleInternalReconnect();
                  }
                  break;
                case Hls.ErrorTypes.MEDIA_ERROR:
                  console.error('Media error, trying to recover...');
                  try {
                    hls?.recoverMediaError();
                  } catch (e) {
                    console.error('Failed to recover from media error:', e);
                    setStreamStatus('error');
                    handleInternalReconnect();
                  }
                  break;
                default:
                  console.error('Fatal error, destroying HLS instance');
                  setError('Критическая ошибка воспроизведения');
                  setStreamStatus('error');
                  cleanup();
                  handleInternalReconnect();
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
              setStreamStatus('playing');
              retryCountRef.current = 0;

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
              setStreamStatus('error');
              handleInternalReconnect();
            }
          });
        } else {
          throw new Error('HLS не поддерживается в этом браузере');
        }

        hlsRef.current = hls;
      } catch (err) {
        console.error('Error initializing stream:', err);
        if (mounted) {
          setError(err instanceof Error ? err.message : 'Ошибка инициализации потока');
          setStreamStatus('error');
          setLoading(false);

          if (retryCountRef.current < maxRetries) {
            handleInternalReconnect();
          }
        }
      }
    };

    const handleInternalReconnect = () => {
      if (retryCountRef.current >= maxRetries) {
        setError(`Не удалось подключиться после ${maxRetries} попыток`);
        return;
      }

      retryCountRef.current++;
      setLoading(true);
      setError(`Попытка переподключения (${retryCountRef.current}/${maxRetries})...`);

      cleanup();

      retryTimeoutRef.current = setTimeout(() => {
        initializeStream();
      }, 3000 * retryCountRef.current); // Экспоненциальная задержка
    };

    // Инициализация при монтировании компонента
    initializeStream();

    // Обработчики событий видео
    const handlePlay = () => {
      setIsPlaying(true);
      setStreamStatus('playing');
    };

    const handlePause = () => {
      setIsPlaying(false);
      setStreamStatus('connected');
    };

    const handleWaiting = () => {
      if (mounted) {
        setLoading(true);
      }
    };

    const handleCanPlay = () => {
      if (mounted) {
        setLoading(false);
      }
    };

    const handleVideoError = () => {
      if (mounted) {
        setLoading(false);
        setError('Ошибка воспроизведения видео');
        setStreamStatus('error');
      }
    };

    video.addEventListener('play', handlePlay);
    video.addEventListener('pause', handlePause);
    video.addEventListener('waiting', handleWaiting);
    video.addEventListener('canplay', handleCanPlay);
    video.addEventListener('error', handleVideoError);

    return () => {
      mounted = false;
      video.removeEventListener('play', handlePlay);
      video.removeEventListener('pause', handlePause);
      video.removeEventListener('waiting', handleWaiting);
      video.removeEventListener('canplay', handleCanPlay);
      video.removeEventListener('error', handleVideoError);

      cleanup();
    };
  }, [camera.id, autoPlay, streamType, reconnectTrigger]);

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

    // Останавливаем HLS
    if (hlsRef.current) {
      hlsRef.current.destroy();
      hlsRef.current = null;
    }

    // Останавливаем WebRTC если активен
    if (pcRef.current) {
      pcRef.current.close();
      pcRef.current = null;
    }

    try {
      await streamService.stopStream(camera.id);
      streamActiveRef.current = false;
      setStreamActive(false);
      setStreamStatus('stopped');
      setError(null);
      retryCountRef.current = 0;
    } catch (err) {
      console.error('Error stopping stream:', err);
      setError('Ошибка остановки потока');
    }
  }, [camera.id]);

  const handleReconnect = useCallback(async () => {
    // Останавливаем текущий поток
    if (streamActiveRef.current) {
      try {
        await streamService.stopStream(camera.id);
      } catch (err) {
        console.error('Error stopping stream before reconnect:', err);
      }
    }

    streamActiveRef.current = false;
    setStreamActive(false);
    setError(null);
    retryCountRef.current = 0;

    // Перезапускаем поток
    const video = videoRef.current;
    if (video) {
      video.pause();
      video.src = '';
    }

    if (hlsRef.current) {
      hlsRef.current.destroy();
      hlsRef.current = null;
    }

    // Принудительно перезапускаем поток через изменение триггера
    setReconnectTrigger((prev: number) => prev + 1);
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
      const response = await streamService.captureScreenshot(camera.id);
      if (response) {
        // Открываем изображение в новой вкладке
        window.open(response, '_blank');
      }
    } catch (err) {
      console.error('Error capturing screenshot:', err);
      setError('Не удалось создать снимок экрана');
    }
  }, [camera.id]);

  const handleQualityChange = useCallback(async (newQuality: StreamQuality) => {
    setQuality(newQuality);
    setQualityMenuAnchor(null);

    try {
      // TODO: Добавить метод в streamService для изменения качества
      // await streamService.setStreamQuality(camera.id, newQuality);
      console.log(`Quality change requested to: ${newQuality}`);
    } catch (err) {
      console.error('Error changing stream quality:', err);
      setError('Ошибка изменения качества потока');
    }
  }, [camera.id]);

  const getStatusColor = (status: StreamStatus): 'success' | 'error' | 'warning' | 'default' => {
    switch (status) {
      case 'playing':
        return 'success';
      case 'connected':
        return 'success';
      case 'error':
        return 'error';
      case 'starting':
        return 'warning';
      case 'stopped':
        return 'default';
      default:
        return 'default';
    }
  };

  const getStatusLabel = (status: StreamStatus): string => {
    switch (status) {
      case 'idle':
        return 'Ожидание';
      case 'starting':
        return 'Запуск...';
      case 'connected':
        return 'Подключено';
      case 'playing':
        return 'Воспроизведение';
      case 'error':
        return 'Ошибка';
      case 'stopped':
        return 'Остановлено';
      default:
        return 'Неизвестно';
    }
  };

  return (
    <Paper sx={{ p: 2, position: 'relative' }}>
      {/* Статус стрима */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Chip
          label={getStatusLabel(streamStatus)}
          color={getStatusColor(streamStatus)}
          size="small"
        />
        {streamType === 'hls' && (
          <Chip label="HLS" size="small" variant="outlined" />
        )}
        {streamType === 'webrtc' && (
          <Chip label="WebRTC" size="small" variant="outlined" color="primary" />
        )}
        {streamType === 'rtsp' && (
          <Chip label="RTSP" size="small" variant="outlined" color="secondary" />
        )}
      </Box>

      {loading && (
        <Box
          sx={{
            position: 'absolute',
            top: '50%',
            left: '50%',
            transform: 'translate(-50%, -50%)',
            zIndex: 2,
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            gap: 2,
          }}
        >
          <CircularProgress />
          <Typography variant="body2" color="text.secondary">
            {streamStatus === 'starting' ? 'Запуск трансляции...' : 'Подключение...'}
          </Typography>
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
            p: 3,
          }}
        >
          <Typography color="error" variant="h6" align="center">
            {error}
          </Typography>
          <Typography variant="body2" color="text.secondary" align="center">
            Камера: {camera.name}
          </Typography>
          <Box sx={{ display: 'flex', gap: 2 }}>
            <IconButton color="primary" onClick={handleReconnect} title="Переподключиться">
              <Refresh />
            </IconButton>
          </Box>
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
            {loading && streamStatus === 'connected' && (
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
                <Typography variant="body2">Буферизация...</Typography>
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
                  disabled={loading || !!error || !streamActive}
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
              <Tooltip title="Качество">
                <IconButton
                  color="primary"
                  onClick={(e: React.MouseEvent<HTMLButtonElement>) => setQualityMenuAnchor(e.currentTarget)}
                >
                  <Settings />
                </IconButton>
              </Tooltip>
              <Tooltip title="Переподключиться">
                <IconButton
                  color="primary"
                  onClick={handleReconnect}
                  disabled={loading}
                >
                  <Refresh />
                </IconButton>
              </Tooltip>
              <Menu
                anchorEl={qualityMenuAnchor}
                open={!!qualityMenuAnchor}
                onClose={() => setQualityMenuAnchor(null)}
              >
                <MenuItem onClick={() => handleQualityChange('low')} selected={quality === 'low'}>
                  Низкое (640x360)
                </MenuItem>
                <MenuItem onClick={() => handleQualityChange('medium')} selected={quality === 'medium'}>
                  Среднее (1280x720)
                </MenuItem>
                <MenuItem onClick={() => handleQualityChange('high')} selected={quality === 'high'}>
                  Высокое (1920x1080)
                </MenuItem>
                <MenuItem onClick={() => handleQualityChange('ultra')} selected={quality === 'ultra'}>
                  Максимальное (1920x1080)
                </MenuItem>
              </Menu>
            </Box>
          )}
        </>
      )}
    </Paper>
  );
}

