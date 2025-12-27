'use client';

import Layout from '@/components/Layout/Layout';
import ProtectedRoute from '@/components/ProtectedRoute/ProtectedRoute';
import RecordingPlayer from '@/components/RecordingPlayer/RecordingPlayer';
import { recordingService } from '@/services/recordingService';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { fetchCameras } from '@/store/slices/camerasSlice';
import { deleteRecording, getDownloadUrl } from '@/store/slices/recordingsSlice';
import type { Camera, Recording } from '@/types';
import {
  ArrowBack as ArrowBackIcon,
  Delete as DeleteIcon,
  Download as DownloadIcon,
  Folder as FolderIcon,
  Schedule as ScheduleIcon
} from '@mui/icons-material';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Divider,
  Grid,
  Paper,
  Typography
} from '@mui/material';
import { useParams, useRouter } from 'next/navigation';
import { useSnackbar } from 'notistack';
import { useEffect, useState } from 'react';

function RecordingDetailContent() {
  const router = useRouter();
  const params = useParams();
  const dispatch = useAppDispatch();
  const { enqueueSnackbar } = useSnackbar();
  const recordingId = params.id as string;
  const { cameras } = useAppSelector((state) => state.cameras);
  const [recording, setRecording] = useState<Recording | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    dispatch(fetchCameras());
  }, [dispatch]);

  useEffect(() => {
    if (recordingId) {
      loadRecording();
    }
  }, [recordingId]);

  const loadRecording = async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await recordingService.getRecordingById(recordingId);
      setRecording(data);
    } catch (err: any) {
      setError(err.message || 'Ошибка при загрузке записи');
      enqueueSnackbar('Ошибка при загрузке записи', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async () => {
    if (!confirm('Вы уверены, что хотите удалить эту запись?')) return;
    try {
      await dispatch(deleteRecording(recordingId)).unwrap();
      enqueueSnackbar('Запись удалена', { variant: 'success' });
      router.push('/recordings');
    } catch (err: any) {
      enqueueSnackbar(err || 'Ошибка при удалении записи', { variant: 'error' });
    }
  };

  const handleDownload = async () => {
    try {
      const result = await dispatch(getDownloadUrl(recordingId)).unwrap();
      if (result.url) {
        window.open(result.url, '_blank');
        enqueueSnackbar('Начата загрузка записи', { variant: 'success' });
      }
    } catch (err: any) {
      enqueueSnackbar(err || 'Ошибка при получении ссылки на скачивание', { variant: 'error' });
    }
  };

  const handleExport = async () => {
    try {
      const url = await recordingService.exportRecording(recordingId);
      window.open(url, '_blank');
      enqueueSnackbar('Экспорт записи начат', { variant: 'success' });
    } catch (err: any) {
      enqueueSnackbar(err || 'Ошибка при экспорте записи', { variant: 'error' });
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

  const formatDuration = (seconds: number): string => {
    const hours = Math.floor(seconds / 3600);
    const minutes = Math.floor((seconds % 3600) / 60);
    const secs = Math.floor(seconds % 60);

    if (hours > 0) {
      return `${hours}:${minutes.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
    }
    return `${minutes}:${secs.toString().padStart(2, '0')}`;
  };

  const formatFileSize = (bytes: number): string => {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round((bytes / Math.pow(k, i)) * 100) / 100 + ' ' + sizes[i];
  };

  const getStatusColor = (status: string): 'success' | 'error' | 'warning' | 'default' => {
    switch (status) {
      case 'COMPLETED':
        return 'success';
      case 'RECORDING':
        return 'warning';
      case 'FAILED':
        return 'error';
      default:
        return 'default';
    }
  };

  const getStatusLabel = (status: string): string => {
    switch (status) {
      case 'COMPLETED':
        return 'Завершена';
      case 'RECORDING':
        return 'Запись';
      case 'PENDING':
        return 'Ожидание';
      case 'FAILED':
        return 'Ошибка';
      default:
        return 'Неизвестно';
    }
  };

  const camera = recording ? cameras.find((c: Camera) => c.id === recording.cameraId) : null;

  if (loading) {
    return (
      <Layout>
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
          <CircularProgress />
        </Box>
      </Layout>
    );
  }

  if (error || !recording) {
    return (
      <Layout>
        <Alert severity="error">{error || 'Запись не найдена'}</Alert>
      </Layout>
    );
  }

  return (
    <Layout>
      <Box sx={{ mb: 3 }}>
        <Button
          startIcon={<ArrowBackIcon />}
          onClick={() => router.push('/recordings')}
          sx={{ mb: 2 }}
        >
          Назад к списку
        </Button>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Box>
            <Typography variant="h4" gutterBottom>
              Запись #{recording.id.substring(0, 8)}
            </Typography>
            <Chip
              label={getStatusLabel(recording.status)}
              color={getStatusColor(recording.status)}
              size="small"
              sx={{ mt: 1 }}
            />
          </Box>
          <Box>
            <Button
              startIcon={<DownloadIcon />}
              onClick={handleDownload}
              sx={{ mr: 1 }}
              disabled={recording.status !== 'COMPLETED'}
            >
              Скачать
            </Button>
            <Button
              startIcon={<DownloadIcon />}
              onClick={handleExport}
              sx={{ mr: 1 }}
              disabled={recording.status !== 'COMPLETED'}
            >
              Экспорт
            </Button>
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
          <Paper sx={{ p: 2 }}>
            <Typography variant="h6" gutterBottom>
              Видеоплеер
            </Typography>
            {recording.status === 'COMPLETED' && recording.filePath ? (
              <RecordingPlayer
                recording={recording}
                autoPlay={false}
                controls={true}
                width="100%"
                height="auto"
              />
            ) : (
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
                  {recording.status === 'COMPLETED'
                    ? 'Файл записи недоступен'
                    : 'Запись еще не завершена'}
                </Typography>
              </Box>
            )}
          </Paper>
        </Grid>
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Информация о записи
            </Typography>
            <Divider sx={{ my: 2 }} />

            {camera && (
              <Box sx={{ mb: 2 }}>
                <Typography variant="body2" color="text.secondary">
                  Камера
                </Typography>
                <Typography variant="body1">{camera.name || recording.cameraId}</Typography>
              </Box>
            )}

            <Box sx={{ mb: 2 }}>
              <Typography variant="body2" color="text.secondary">
                Начало записи
              </Typography>
              <Typography variant="body1">
                <ScheduleIcon sx={{ fontSize: 16, verticalAlign: 'middle', mr: 0.5 }} />
                {formatDate(recording.startTime)}
              </Typography>
            </Box>

            <Box sx={{ mb: 2 }}>
              <Typography variant="body2" color="text.secondary">
                Конец записи
              </Typography>
              <Typography variant="body1">
                <ScheduleIcon sx={{ fontSize: 16, verticalAlign: 'middle', mr: 0.5 }} />
                {formatDate(recording.endTime)}
              </Typography>
            </Box>

            <Box sx={{ mb: 2 }}>
              <Typography variant="body2" color="text.secondary">
                Длительность
              </Typography>
              <Typography variant="body1">
                {formatDuration(recording.duration)}
              </Typography>
            </Box>

            <Box sx={{ mb: 2 }}>
              <Typography variant="body2" color="text.secondary">
                Формат
              </Typography>
              <Typography variant="body1">{recording.format}</Typography>
            </Box>

            <Box sx={{ mb: 2 }}>
              <Typography variant="body2" color="text.secondary">
                Качество
              </Typography>
              <Typography variant="body1">
                {recording.quality ? `${recording.quality.width}x${recording.quality.height}` : 'N/A'}
              </Typography>
            </Box>

            <Box sx={{ mb: 2 }}>
              <Typography variant="body2" color="text.secondary">
                Размер файла
              </Typography>
              <Typography variant="body1">
                <FolderIcon sx={{ fontSize: 16, verticalAlign: 'middle', mr: 0.5 }} />
                {formatFileSize(recording.fileSize)}
              </Typography>
            </Box>

            <Box sx={{ mb: 2 }}>
              <Typography variant="body2" color="text.secondary">
                Путь к файлу
              </Typography>
              <Typography variant="body1" sx={{ wordBreak: 'break-all', fontSize: '0.875rem' }}>
                {recording.filePath}
              </Typography>
            </Box>

            <Box sx={{ mb: 2 }}>
              <Typography variant="body2" color="text.secondary">
                Создано
              </Typography>
              <Typography variant="body1">
                {formatDate(recording.createdAt)}
              </Typography>
            </Box>
          </Paper>
        </Grid>
      </Grid>
    </Layout>
  );
}

export default function RecordingDetailPage() {
  return (
    <ProtectedRoute>
      <RecordingDetailContent />
    </ProtectedRoute>
  );
}

