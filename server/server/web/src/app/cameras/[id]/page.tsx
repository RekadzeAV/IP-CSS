'use client';

import React, { useEffect } from 'react';
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
} from '@mui/icons-material';
import Layout from '@/components/Layout/Layout';
import ProtectedRoute from '@/components/ProtectedRoute/ProtectedRoute';
import VideoPlayer from '@/components/VideoPlayer/VideoPlayer';
import { useAppSelector, useAppDispatch } from '@/store/hooks';
import { fetchCameraById, testConnection, clearError } from '@/store/slices/camerasSlice';
import { useSnackbar } from 'notistack';

function CameraDetailContent() {
  const router = useRouter();
  const params = useParams();
  const dispatch = useAppDispatch();
  const { enqueueSnackbar } = useSnackbar();
  const cameraId = params.id as string;
  const { selectedCamera, loading, error, connectionTest } = useAppSelector(
    (state) => state.cameras
  );

  useEffect(() => {
    if (cameraId) {
      dispatch(fetchCameraById(cameraId));
    }
  }, [dispatch, cameraId]);

  const handleTestConnection = async () => {
    try {
      await dispatch(testConnection(cameraId)).unwrap();
      enqueueSnackbar('Тест подключения выполнен', { variant: 'success' });
    } catch (error: any) {
      enqueueSnackbar(error || 'Ошибка при тестировании подключения', { variant: 'error' });
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

  if (!selectedCamera) {
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
            <Button startIcon={<EditIcon />} variant="contained">
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
          <VideoPlayer camera={selectedCamera} />
        </Grid>
        <Grid item xs={12} md={4}>
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
        </Grid>
      </Grid>
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

