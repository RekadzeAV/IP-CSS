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
  Card,
  CardContent,
} from '@mui/material';
import {
  ArrowBack as ArrowBackIcon,
  CheckCircle as CheckCircleIcon,
  Delete as DeleteIcon,
  Schedule as ScheduleIcon,
  Videocam as VideocamIcon,
  Warning as WarningIcon,
  Info as InfoIcon,
  Error as ErrorIcon,
} from '@mui/icons-material';
import Layout from '@/components/Layout/Layout';
import ProtectedRoute from '@/components/ProtectedRoute/ProtectedRoute';
import { eventService } from '@/services/eventService';
import { useAppSelector, useAppDispatch } from '@/store/hooks';
import { fetchCameras } from '@/store/slices/camerasSlice';
import { deleteEvent, acknowledgeEvent } from '@/store/slices/eventsSlice';
import { useSnackbar } from 'notistack';
import type { Event } from '@/types';

function EventDetailContent() {
  const router = useRouter();
  const params = useParams();
  const dispatch = useAppDispatch();
  const { enqueueSnackbar } = useSnackbar();
  const eventId = params.id as string;
  const { cameras } = useAppSelector((state) => state.cameras);
  const [event, setEvent] = useState<Event | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    dispatch(fetchCameras());
  }, [dispatch]);

  useEffect(() => {
    if (eventId) {
      loadEvent();
    }
  }, [eventId]);

  const loadEvent = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await eventService.getEventById(eventId);
      setEvent(data);
    } catch (err: any) {
      setError(err.message || 'Ошибка при загрузке события');
      enqueueSnackbar('Ошибка при загрузке события', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleAcknowledge = async () => {
    try {
      await dispatch(acknowledgeEvent(eventId)).unwrap();
      enqueueSnackbar('Событие подтверждено', { variant: 'success' });
      loadEvent(); // Перезагружаем событие для обновления статуса
    } catch (err: any) {
      enqueueSnackbar(err || 'Ошибка при подтверждении события', { variant: 'error' });
    }
  };

  const handleDelete = async () => {
    if (!confirm('Вы уверены, что хотите удалить это событие?')) return;
    try {
      await dispatch(deleteEvent(eventId)).unwrap();
      enqueueSnackbar('Событие удалено', { variant: 'success' });
      router.push('/events');
    } catch (err: any) {
      enqueueSnackbar(err || 'Ошибка при удалении события', { variant: 'error' });
    }
  };

  const formatDate = (timestamp: number): string => {
    return new Date(timestamp).toLocaleString('ru-RU', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  };

  const getSeverityColor = (severity: string): 'success' | 'error' | 'warning' | 'info' | 'default' => {
    switch (severity) {
      case 'CRITICAL':
        return 'error';
      case 'HIGH':
        return 'error';
      case 'MEDIUM':
        return 'warning';
      case 'LOW':
        return 'info';
      default:
        return 'default';
    }
  };

  const getSeverityIcon = (severity: string) => {
    switch (severity) {
      case 'CRITICAL':
      case 'HIGH':
        return <ErrorIcon />;
      case 'MEDIUM':
        return <WarningIcon />;
      case 'LOW':
        return <InfoIcon />;
      default:
        return <InfoIcon />;
    }
  };

  const getSeverityLabel = (severity: string): string => {
    switch (severity) {
      case 'CRITICAL':
        return 'Критическая';
      case 'HIGH':
        return 'Высокая';
      case 'MEDIUM':
        return 'Средняя';
      case 'LOW':
        return 'Низкая';
      default:
        return 'Неизвестно';
    }
  };

  const getTypeLabel = (type: string): string => {
    const typeLabels: Record<string, string> = {
      MOTION_DETECTED: 'Обнаружено движение',
      OBJECT_DETECTED: 'Обнаружен объект',
      FACE_DETECTED: 'Обнаружено лицо',
      LICENSE_PLATE_DETECTED: 'Обнаружен номер',
      CAMERA_OFFLINE: 'Камера офлайн',
      CAMERA_ONLINE: 'Камера онлайн',
      RECORDING_STARTED: 'Запись начата',
      RECORDING_STOPPED: 'Запись остановлена',
      RECORDING_FAILED: 'Ошибка записи',
      SYSTEM_ERROR: 'Ошибка системы',
      SYSTEM_WARNING: 'Предупреждение системы',
    };
    return typeLabels[type] || type;
  };

  const camera = event ? cameras.find((c) => c.id === event.cameraId) : null;

  if (loading) {
    return (
      <Layout>
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
          <CircularProgress />
        </Box>
      </Layout>
    );
  }

  if (error || !event) {
    return (
      <Layout>
        <Alert severity="error">{error || 'Событие не найдено'}</Alert>
      </Layout>
    );
  }

  return (
    <Layout>
      <Box sx={{ mb: 3 }}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={() => router.push('/events')}
          sx={{ mb: 2 }}
        >
          Назад к списку
        </Button>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Box>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 1 }}>
              {getSeverityIcon(event.severity)}
              <Typography variant="h4">
                {getTypeLabel(event.type)}
              </Typography>
            </Box>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <Chip
                label={getSeverityLabel(event.severity)}
                color={getSeverityColor(event.severity)}
                size="small"
              />
              <Chip
                label={event.acknowledged ? 'Подтверждено' : 'Не подтверждено'}
                color={event.acknowledged ? 'success' : 'warning'}
                size="small"
              />
            </Box>
          </Box>
          <Box>
            {!event.acknowledged && (
              <Button
                startIcon={<CheckCircleIcon />}
                variant="contained"
                color="success"
                onClick={handleAcknowledge}
                sx={{ mr: 1 }}
              >
                Подтвердить
              </Button>
            )}
            <Button
              startIcon={<DeleteIcon />}
              color="error"
              onClick={handleDelete}
            >
              Удалить
            </Button>
          </Box>
        </Box>
      </Box>

      <Grid container spacing={3}>
        <Grid item xs={12} md={8}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Описание события
            </Typography>
            <Divider sx={{ my: 2 }} />
            <Typography variant="body1" paragraph>
              {event.message || 'Описание отсутствует'}
            </Typography>

            {event.metadata && Object.keys(event.metadata).length > 0 && (
              <>
                <Typography variant="h6" gutterBottom sx={{ mt: 3 }}>
                  Метаданные
                </Typography>
                <Divider sx={{ my: 2 }} />
                <Box component="pre" sx={{
                  p: 2,
                  backgroundColor: 'grey.100',
                  borderRadius: 1,
                  overflow: 'auto',
                  fontSize: '0.875rem',
                }}>
                  {JSON.stringify(event.metadata, null, 2)}
                </Box>
              </>
            )}

            {event.videoUrl && (
              <>
                <Typography variant="h6" gutterBottom sx={{ mt: 3 }}>
                  Видео
                </Typography>
                <Divider sx={{ my: 2 }} />
                <Box
                  sx={{
                    width: '100%',
                    aspectRatio: '16/9',
                    backgroundColor: '#000',
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center',
                    borderRadius: 1,
                  }}
                >
                  <Typography variant="body2" color="text.secondary">
                    Видеоплеер будет здесь (требуется интеграция)
                  </Typography>
                </Box>
              </>
            )}
          </Paper>
        </Grid>
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Информация о событии
            </Typography>
            <Divider sx={{ my: 2 }} />

            <Box sx={{ mb: 2 }}>
              <Typography variant="body2" color="text.secondary">
                ID события
              </Typography>
              <Typography variant="body1" sx={{ wordBreak: 'break-all', fontSize: '0.875rem' }}>
                {event.id}
              </Typography>
            </Box>

            {camera && (
              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  Камера
                </Typography>
                <Typography variant="body1">
                  <VideocamIcon sx={{ fontSize: 16, verticalAlign: 'middle', mr: 0.5 }} />
                  {camera.name || event.cameraId}
                </Typography>
              </Box>
            )}

            <Box sx={{ mb: 2 }}>
              <Typography variant="body2" color="text.secondary">
                Тип события
              </Typography>
              <Typography variant="body1">{getTypeLabel(event.type)}</Typography>
            </Box>

            <Box sx={{ mb: 2 }}>
              <Typography variant="body2" color="text.secondary">
                Важность
              </Typography>
              <Chip
                label={getSeverityLabel(event.severity)}
                color={getSeverityColor(event.severity)}
                size="small"
              />
            </Box>

            <Box sx={{ mb: 2 }}>
              <Typography variant="body2" color="text.secondary">
                Время события
              </Typography>
              <Typography variant="body1">
                <ScheduleIcon sx={{ fontSize: 16, verticalAlign: 'middle', mr: 0.5 }} />
                {formatDate(event.timestamp)}
              </Typography>
            </Box>

            <Box sx={{ mb: 2 }}>
              <Typography variant="body2" color="text.secondary">
                Статус подтверждения
              </Typography>
              <Chip
                label={event.acknowledged ? 'Подтверждено' : 'Не подтверждено'}
                color={event.acknowledged ? 'success' : 'warning'}
                size="small"
              />
            </Box>

            {event.acknowledgedAt && (
              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  Время подтверждения
                </Typography>
                <Typography variant="body1">
                  {formatDate(event.acknowledgedAt)}
                </Typography>
              </Box>
            )}
          </Paper>
        </Grid>
      </Grid>
    </Layout>
  );
}

export default function EventDetailPage() {
  return (
    <ProtectedRoute>
      <EventDetailContent />
    </ProtectedRoute>
  );
}

