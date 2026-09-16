'use client';

import React, { useEffect, useState } from 'react';
import { useRouter, useParams } from 'next/navigation';
import {
  Box,
  Typography,
  Paper,
  Grid,
  Button,
  CircularProgress,
  Chip,
  Divider,
  Alert,
} from '@mui/material';
import {
  ArrowBack as ArrowBackIcon,
  Edit as EditIcon,
  Refresh as RefreshIcon,
  CropFree as CropFreeIcon,
} from '@mui/icons-material';
import Layout from '@/components/Layout/Layout';
import ProtectedRoute from '@/components/ProtectedRoute/ProtectedRoute';
import VideoPlayer from '@/components/VideoPlayer/VideoPlayer';
import PTZControls from '@/components/PTZControls/PTZControls';
import CameraEditDialog from '@/components/CameraEditDialog/CameraEditDialog';
import MotionZoneEditor from '@/components/MotionZoneEditor/MotionZoneEditor';
import { useAppSelector, useAppDispatch } from '@/store/hooks';
import { fetchCameraById, testConnection, clearError } from '@/store/slices/camerasSlice';
import { cameraService } from '@/services/cameraService';
import { useSnackbar } from 'notistack';
import type { CameraObservationSummaryDto, DetectionZone } from '@/types';

function CameraDetailContent() {
  const router = useRouter();
  const params = useParams();
  const dispatch = useAppDispatch();
  const { enqueueSnackbar } = useSnackbar();
  const cameraId = params.id as string;
  const { selectedCamera, loading, error, connectionTest } = useAppSelector(
    (state) => state.cameras
  );
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [showZoneEditor, setShowZoneEditor] = useState(false);
  const [videoElement, setVideoElement] = useState<HTMLVideoElement | null>(null);
  const [observationSummary, setObservationSummary] = useState<CameraObservationSummaryDto | null>(null);
  const [observationSummaryError, setObservationSummaryError] = useState<string | null>(null);
  const isCurrentCameraLoaded = selectedCamera?.id === cameraId;

  useEffect(() => {
    if (cameraId) {
      dispatch(fetchCameraById(cameraId));
      setObservationSummary(null);
      setObservationSummaryError(null);
    }
  }, [dispatch, cameraId]);

  useEffect(() => {
    let cancelled = false;
    const loadObservationSummary = async () => {
      if (!cameraId || !isCurrentCameraLoaded) return;
      try {
        const summary = await cameraService.getObservationSummary(cameraId);
        if (!cancelled) {
          setObservationSummary(summary);
          setObservationSummaryError(null);
        }
      } catch (error: unknown) {
        if (!cancelled) {
          const message = error instanceof Error ? error.message : 'Ошибка загрузки observation summary';
          setObservationSummary(null);
          setObservationSummaryError(message);
        }
      }
    };
    loadObservationSummary();
    return () => {
      cancelled = true;
    };
  }, [cameraId, isCurrentCameraLoaded]);

  const handleTestConnection = async () => {
    try {
      await dispatch(testConnection(cameraId)).unwrap();
      enqueueSnackbar('Тест подключения выполнен', { variant: 'success' });
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Ошибка при тестировании подключения';
      enqueueSnackbar(message, { variant: 'error' });
    }
  };

  const getStatusColor = (status: string): 'success' | 'error' | 'warning' | 'default' => {
    switch (status) {
      case 'ONLINE':
        return 'success';
      case 'OFFLINE':
        return 'error';
      case 'ERROR':
        return 'error';
      case 'CONNECTING':
        return 'warning';
      default:
        return 'default';
    }
  };

  const getStatusLabel = (status: string): string => {
    switch (status) {
      case 'ONLINE':
        return 'Онлайн';
      case 'OFFLINE':
        return 'Офлайн';
      case 'ERROR':
        return 'Ошибка';
      case 'CONNECTING':
        return 'Подключение';
      default:
        return 'Неизвестно';
    }
  };

  if (loading && !selectedCamera) {
    return (
      <Layout>
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
          <CircularProgress />
        </Box>
      </Layout>
    );
  }

  if (!isCurrentCameraLoaded) {
    return (
      <Layout>
        <Alert severity="error">Камера не найдена</Alert>
      </Layout>
    );
  }

  return (
    <Layout>
      <Box sx={{ mb: 3 }}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={() => router.push('/cameras')}
          sx={{ mb: 2 }}
        >
          Назад к списку
        </Button>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Box>
            <Typography variant="h4" gutterBottom>
              {selectedCamera.name}
            </Typography>
            <Chip
              label={getStatusLabel(selectedCamera.status)}
              color={getStatusColor(selectedCamera.status)}
              size="small"
            />
          </Box>
          <Box>
            <Button
              startIcon={<RefreshIcon />}
              onClick={handleTestConnection}
              sx={{ mr: 1 }}
            >
              Тест подключения
            </Button>
            <Button
              startIcon={<CropFreeIcon />}
              onClick={() => setShowZoneEditor(!showZoneEditor)}
              sx={{ mr: 1 }}
              variant={showZoneEditor ? 'contained' : 'outlined'}
            >
              {showZoneEditor ? 'Скрыть зоны' : 'Зоны детекции'}
            </Button>
            <Button
              startIcon={<EditIcon />}
              variant="contained"
              onClick={() => setEditDialogOpen(true)}
            >
              Редактировать
            </Button>
          </Box>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => dispatch(clearError())}>
          {error}
        </Alert>
      )}

      {connectionTest && (
        <Alert
          severity={connectionTest.success ? 'success' : 'error'}
          sx={{ mb: 2 }}
        >
          {connectionTest.success
            ? 'Подключение успешно'
            : `Ошибка подключения: ${connectionTest.error}`}
        </Alert>
      )}

      <Grid container spacing={3}>
        <Grid item xs={12} md={8}>
          <VideoPlayer
            camera={selectedCamera}
            onVideoElementReady={(element) => setVideoElement(element)}
          />
          {showZoneEditor && (
            <Box sx={{ mt: 2 }}>
              <MotionZoneEditor
                camera={selectedCamera}
                videoElement={videoElement}
                onSave={async (zones: DetectionZone[]) => {
                  try {
                    const serverZones = zones
                      .filter((z) => z.enabled)
                      .map((z) => ({
                        name: z.name,
                        polygon: z.points.map((p) => [Math.round(p.x), Math.round(p.y)]),
                        sensitivity: z.sensitivity ?? 80,
                      }));
                    await cameraService.updateCamera(selectedCamera.id, {
                      settings: {
                        ...selectedCamera.settings,
                        analytics: {
                          ...selectedCamera.settings?.analytics,
                          zones: serverZones,
                        },
                      },
                    });
                    enqueueSnackbar('Зоны детекции сохранены', { variant: 'success' });
                  } catch (error: unknown) {
                    const message = error instanceof Error ? error.message : 'Ошибка при сохранении зон';
                    enqueueSnackbar(message, { variant: 'error' });
                    throw error;
                  }
                }}
              />
            </Box>
          )}
        </Grid>
        <Grid item xs={12} md={4}>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 3 }}>
            <Paper sx={{ p: 3 }}>
              <Typography variant="h6" gutterBottom>
                Информация о камере
              </Typography>
              <Divider sx={{ my: 2 }} />
              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  URL
                </Typography>
                <Typography variant="body1">{selectedCamera.url}</Typography>
              </Box>
              {selectedCamera.model && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="body2" color="text.secondary">
                    Модель
                  </Typography>
                  <Typography variant="body1">{selectedCamera.model}</Typography>
                </Box>
              )}
              {selectedCamera.resolution && (
                <Box sx={{ mb: 2 }}>
                  <Typography variant="body2" color="text.secondary">
                    Разрешение
                  </Typography>
                  <Typography variant="body1">
                    {selectedCamera.resolution.width}x{selectedCamera.resolution.height}
                  </Typography>
                </Box>
              )}
              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  FPS
                </Typography>
                <Typography variant="body1">{selectedCamera.fps}</Typography>
              </Box>
              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  Битрейт
                </Typography>
                <Typography variant="body1">{selectedCamera.bitrate} kbps</Typography>
              </Box>
              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  Кодек
                </Typography>
                <Typography variant="body1">{selectedCamera.codec}</Typography>
              </Box>
            </Paper>

            <Paper sx={{ p: 3 }}>
              <Typography variant="h6" gutterBottom>
                Observation Summary
              </Typography>
              <Divider sx={{ my: 2 }} />
              {observationSummaryError ? (
                <Alert severity="warning">{observationSummaryError}</Alert>
              ) : (
                <>
                  <Box sx={{ mb: 2 }}>
                    <Typography variant="body2" color="text.secondary">
                      Вычислено, px/m
                    </Typography>
                    <Typography variant="body1">
                      {observationSummary?.computedPixelsPerMeter != null
                        ? observationSummary.computedPixelsPerMeter.toFixed(2)
                        : 'N/A'}
                    </Typography>
                  </Box>
                  <Box sx={{ mb: 2 }}>
                    <Typography variant="body2" color="text.secondary">
                      Целевой минимум, px/m
                    </Typography>
                    <Typography variant="body1">
                      {observationSummary?.targetPixelsPerMeterMin != null
                        ? observationSummary.targetPixelsPerMeterMin.toFixed(2)
                        : 'N/A'}
                    </Typography>
                  </Box>
                  <Chip
                    color={observationSummary?.lowResolutionWarning ? 'warning' : 'success'}
                    label={observationSummary?.lowResolutionWarning ? 'Низкое разрешение для наблюдения' : 'Разрешение в норме'}
                    size="small"
                  />
                </>
              )}
            </Paper>

            {/* PTZ Controls */}
            <PTZControls camera={selectedCamera} disabled={selectedCamera.status !== 'ONLINE'} />
          </Box>
        </Grid>
      </Grid>

      {/* Диалог редактирования камеры */}
      <CameraEditDialog
        open={editDialogOpen}
        onClose={() => setEditDialogOpen(false)}
        camera={selectedCamera}
      />
    </Layout>
  );
}

export default function CameraDetailPage() {
  return (
    <ProtectedRoute>
      <CameraDetailContent />
    </ProtectedRoute>
  );
}

