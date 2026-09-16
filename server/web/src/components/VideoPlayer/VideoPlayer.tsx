'use client';

import { streamService } from '@/services/streamService';
import type { Camera } from '@/types';
import { Fullscreen, FullscreenExit, Pause, PhotoCamera, PlayArrow, Refresh, Settings, Stop } from '@mui/icons-material';
import { Box, Chip, CircularProgress, IconButton, Menu, MenuItem, Paper, Tooltip, Typography } from '@mui/material';
import Hls from 'hls.js';
import React, { useCallback, useEffect, useRef, useState } from 'react';
import { createHlsLivePlayerConfig } from '@/utils/hlsPlayerConfig';

interface VideoPlayerProps {
  camera: Camera;
  autoPlay?: boolean;
  controls?: boolean;
  width?: string | number;
  height?: string | number;
  streamType?: 'hls' | 'webrtc' | 'rtsp'; // Тип стрима: HLS (по умолчанию), WebRTC для низкой задержки, или RTSP для прямой трансляции
  onVideoElementReady?: (element: HTMLVideoElement | null) => void; // Callback для получения video элемента
}

type StreamQuality =
  | 'low'
  | 'medium'
  | 'high'
  | 'ultra'
  | 'qhd1440'
  | 'uhd4k'
  | 'qhd1440_h264'
  | 'uhd4k_h264';

type StreamStatus = 'idle' | 'starting' | 'connected' | 'playing' | 'error' | 'stopped';
type HlsErrorPayload = {
  fatal?: boolean;
  type?: string;
};

export default function VideoPlayer({
  camera,
  autoPlay = true,
  controls = true,
  width = '100%',
  height = 'auto',
  streamType = 'hls',
  onVideoElementReady,
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
  const [webrtcStats, setWebRTCStats] = useState<{
    rtt: number;
    framesReceived: number;
    bitrate: number;
    frameRate: number;
  } | null>(null);

  // Уведомляем родительский компонент о готовности video элемента
  useEffect(() => {
    if (onVideoElementReady && videoRef.current) {
      onVideoElementReady(videoRef.current);
    }
    return () => {
      if (onVideoElementReady) {
        onVideoElementReady(null);
      }
    };
  }, [onVideoElementReady]);

  // Инициализация и управление потоком (HLS или WebRTC)
  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    let hls: Hls | null = null;
    let mounted = true;
    let retryAttempt = 0;
    let effectiveStreamType: 'hls' | 'webrtc' | 'rtsp' = streamType;

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

        if (effectiveStreamType === 'webrtc') {
          // WebRTC реализация
          try {
            const { initWebRTCConnection, getWebRTCStats } = await import('@/utils/webrtc');
            const apiUrl = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

            const { stream, peerConnection } = await initWebRTCConnection(camera.id, apiUrl, {
              iceServers: [
                { urls: 'stun:stun.l.google.com:19302' },
                { urls: 'stun:stun1.l.google.com:19302' },
              ],
              onTrack: (event) => {
                if (mounted && video) {
                  video.srcObject = event.streams[0];
                  setStreamStatus('connected');
                }
              },
              onConnectionStateChange: (state) => {
                if (mounted) {
                  if (state === 'connected') {
                    setStreamStatus('playing');
                    setLoading(false);
                  } else if (state === 'failed' || state === 'disconnected') {
                    setStreamStatus('error');
                    setError('WebRTC соединение потеряно. Переключение на HLS...');
                    // Fallback на HLS при ошибках
                    if (mounted) {
                      effectiveStreamType = 'hls';
                      // Продолжаем выполнение для HLS
                    }
                  }
                }
              },
              onError: (error) => {
                console.error('WebRTC error:', error);
                if (mounted) {
                  setError('Ошибка WebRTC соединения. Переключение на HLS...');
                  effectiveStreamType = 'hls';
                }
              },
            });

            if (mounted && video) {
              video.srcObject = stream;
              setStreamActive(true);
              setStreamStatus('connected');
              setLoading(false);

              // Сохраняем peerConnection для статистики
              pcRef.current = peerConnection;

              if (peerConnection) {

                // Обновление статистики каждую секунду
                const statsInterval = setInterval(async () => {
                  if (mounted && peerConnection) {
                    try {
                      const stats = await getWebRTCStats(peerConnection);
                      if (stats && mounted) {
                        setWebRTCStats({
                          rtt: stats.rtt,
                          framesReceived: stats.framesReceived,
                          bitrate: stats.bitrate || 0,
                          frameRate: stats.frameRate || 0,
                        });
                      }
                    } catch (e) {
                      console.warn('Failed to get WebRTC stats:', e);
                    }
                  } else {
                    clearInterval(statsInterval);
                  }
                }, 1000);

                // Очистка интервала при размонтировании
                return () => {
                  clearInterval(statsInterval);
                };
              }

              if (autoPlay) {
                video.play().catch((e: unknown) => {
                  console.error('Error auto-playing video:', e);
                  if (mounted) {
                    setError('Не удалось автоматически начать воспроизведение');
                  }
                });
              }
            }
          } catch (webrtcError) {
            console.error('WebRTC error:', webrtcError);
            if (mounted) {
              setError('Ошибка WebRTC соединения. Переключение на HLS...');
              // Fallback на HLS
              effectiveStreamType = 'hls';
              // Продолжаем выполнение для HLS
            } else {
              return;
            }
          }

          // Если WebRTC успешно инициализирован, не продолжаем дальше
          if (effectiveStreamType === 'webrtc') {
            return;
          }
        }

        if (effectiveStreamType === 'rtsp') {
          // RTSP поток - в браузерах RTSP не поддерживается напрямую
          // Бэкенд автоматически конвертирует RTSP в HLS, поэтому используем HLS URL
          console.log('RTSP stream type selected - using HLS conversion for browser compatibility');

          const hlsUrl = streamService.getHlsUrl(camera.id);
          const adaptiveHlsUrl = streamService.getAdaptiveHlsUrl(camera.id);
          const finalHlsUrl = adaptiveHlsUrl || hlsUrl;
          const isNativeHlsSupported = video.canPlayType('application/vnd.apple.mpegurl');

          if (Hls.isSupported()) {
            hls = new Hls(createHlsLivePlayerConfig());
            const hlsInstance = hls;

            hlsInstance.loadSource(finalHlsUrl);
            hlsInstance.attachMedia(video);

            // Настройка адаптивного битрейта
            hlsInstance.on(Hls.Events.MANIFEST_PARSED, () => {
                // HLS.js автоматически выберет оптимальное качество на основе пропускной способности
                if (hlsInstance.levels && hlsInstance.levels.length > 0) {
                    console.log(`HLS manifest parsed with ${hlsInstance.levels.length} quality levels`);
                }
            });

            hlsInstance.on(Hls.Events.MANIFEST_PARSED, () => {
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

            hlsInstance.on(Hls.Events.ERROR, (_event: string, data: HlsErrorPayload) => {
              if (!mounted) return;

              if (data.fatal) {
                switch (data.type) {
                  case Hls.ErrorTypes.NETWORK_ERROR:
                    console.error('Network error, trying to recover...');
                    setError('Ошибка сети. Попытка восстановления...');

                    if (retryAttempt < maxRetries) {
                      retryAttempt++;
                      retryTimeoutRef.current = setTimeout(() => {
                        hlsInstance.startLoad();
                      }, 2000 * retryAttempt);
                    } else {
                      setStreamStatus('error');
                      handleInternalReconnect();
                    }
                    break;
                  case Hls.ErrorTypes.MEDIA_ERROR:
                    console.error('Media error, trying to recover...');
                    try {
                      hlsInstance.recoverMediaError();
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

        // HLS стриминг - используем master playlist для адаптивного битрейта
        const hlsUrl = streamService.getHlsUrl(camera.id);
        const adaptiveHlsUrl = streamService.getAdaptiveHlsUrl(camera.id);
        const isNativeHlsSupported = video.canPlayType('application/vnd.apple.mpegurl');
        const finalHlsUrl = adaptiveHlsUrl || hlsUrl;

        if (Hls.isSupported()) {
          hls = new Hls(createHlsLivePlayerConfig());

          hls.loadSource(finalHlsUrl);
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

            hls.on(Hls.Events.ERROR, (_event: string, data: HlsErrorPayload) => {
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

    // Останавливаем WebRTC stream если активен
    if (video?.srcObject) {
      const stream = video.srcObject as MediaStream;
      stream.getTracks().forEach((track) => track.stop());
      video.srcObject = null;
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
      await streamService.setStreamQuality(camera.id, newQuality);
      // Перезапускаем поток с новым качеством
      setReconnectTrigger((prev: number) => prev + 1);
    } catch (err) {
      console.error('Error changing stream quality:', err);
      setError('Ошибка изменения качества потока');
      // Возвращаем предыдущее качество при ошибке
      setQuality(quality);
    }
  }, [camera.id, quality]);

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
            {/* WebRTC статистика */}
            {webrtcStats && streamType === 'webrtc' && (
              <Box
                sx={{
                  position: 'absolute',
                  top: 8,
                  right: 8,
                  bgcolor: 'rgba(0, 0, 0, 0.7)',
                  color: 'white',
                  p: 1,
                  borderRadius: 1,
                  zIndex: 3,
                  fontSize: '0.75rem',
                }}
              >
                <Typography variant="caption" display="block">
                  RTT: {webrtcStats.rtt.toFixed(0)}ms
                </Typography>
                <Typography variant="caption" display="block">
                  FPS: {webrtcStats.frameRate.toFixed(1)}
                </Typography>
                <Typography variant="caption" display="block">
                  Bitrate: {webrtcStats.bitrate.toFixed(0)} kbps
                </Typography>
                <Typography variant="caption" display="block">
                  Frames: {webrtcStats.framesReceived}
                </Typography>
              </Box>
            )}
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
                  aria-label={isPlaying ? 'Пауза' : 'Воспроизвести'}
                  onClick={isPlaying ? handlePause : handlePlay}
                  disabled={loading || !!error}
                >
                  {isPlaying ? <Pause /> : <PlayArrow />}
                </IconButton>
              </Tooltip>
              <Tooltip title="Stop">
                <IconButton
                  color="primary"
                  aria-label="Остановить"
                  onClick={handleStop}
                  disabled={loading || !!error || !streamActive}
                >
                  <Stop />
                </IconButton>
              </Tooltip>
              <Tooltip title="Screenshot">
                <IconButton
                  color="primary"
                  aria-label="Сделать снимок"
                  onClick={handleScreenshot}
                  disabled={loading || !!error}
                >
                  <PhotoCamera />
                </IconButton>
              </Tooltip>
              <Tooltip title={isFullscreen ? "Exit Fullscreen" : "Fullscreen"}>
                <IconButton
                  color="primary"
                  aria-label={isFullscreen ? 'Выйти из полноэкранного режима' : 'Полноэкранный режим'}
                  onClick={handleFullscreen}
                >
                  {isFullscreen ? <FullscreenExit /> : <Fullscreen />}
                </IconButton>
              </Tooltip>
              <Tooltip title="Качество">
                <IconButton
                  color="primary"
                  aria-label="Изменить качество видео"
                  onClick={(e: React.MouseEvent<HTMLButtonElement>) => setQualityMenuAnchor(e.currentTarget)}
                >
                  <Settings />
                </IconButton>
              </Tooltip>
              <Tooltip title="Переподключиться">
                <IconButton
                  color="primary"
                  aria-label="Переподключить поток"
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
                <MenuItem onClick={() => handleQualityChange('qhd1440')} selected={quality === 'qhd1440'}>
                  2K (2560×1440)
                </MenuItem>
                <MenuItem onClick={() => handleQualityChange('uhd4k')} selected={quality === 'uhd4k'}>
                  4K (3840×2160)
                </MenuItem>
                <MenuItem onClick={() => handleQualityChange('qhd1440_h264')} selected={quality === 'qhd1440_h264'}>
                  2K H.264 fallback (2560×1440)
                </MenuItem>
                <MenuItem onClick={() => handleQualityChange('uhd4k_h264')} selected={quality === 'uhd4k_h264'}>
                  4K H.264 fallback (3840×2160)
                </MenuItem>
              </Menu>
            </Box>
          )}
        </>
      )}
    </Paper>
  );
}

