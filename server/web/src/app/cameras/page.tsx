'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import {
  Box,
  Grid,
  Typography,
  Button,
  CircularProgress,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Alert,
} from '@mui/material';
import { Add as AddIcon } from '@mui/icons-material';
import Layout from '@/components/Layout/Layout';
import ProtectedRoute from '@/components/ProtectedRoute/ProtectedRoute';
import CameraCard from '@/components/CameraCard/CameraCard';
import { useAppSelector, useAppDispatch } from '@/store/hooks';
import {
  fetchCameras,
  createCamera,
  deleteCamera,
  clearError,
} from '@/store/slices/camerasSlice';
import { useSnackbar } from 'notistack';
import type { CreateCameraRequest } from '@/types';

function CamerasContent() {
  const router = useRouter();
  const dispatch = useAppDispatch();
  const { enqueueSnackbar } = useSnackbar();
  const { cameras, loading, error } = useAppSelector((state) => state.cameras);
  const [openDialog, setOpenDialog] = useState(false);
  const [formData, setFormData] = useState<CreateCameraRequest>({
    name: '',
    url: '',
    username: '',
    password: '',
  });

  useEffect(() => {
    dispatch(fetchCameras());
  }, [dispatch]);

  const handleCreateCamera = async () => {
    try {
      await dispatch(createCamera(formData)).unwrap();
      enqueueSnackbar('Камера успешно добавлена', { variant: 'success' });
      setOpenDialog(false);
      setFormData({ name: '', url: '', username: '', password: '' });
    } catch (error: any) {
      enqueueSnackbar(error || 'Ошибка при добавлении камеры', { variant: 'error' });
    }
  };

  const handleDeleteCamera = async (cameraId: string) => {
    try {
      await dispatch(deleteCamera(cameraId)).unwrap();
      enqueueSnackbar('Камера успешно удалена', { variant: 'success' });
    } catch (error: any) {
      enqueueSnackbar(error || 'Ошибка при удалении камеры', { variant: 'error' });
    }
  };

  const handleEditCamera = (camera: any) => {
    router.push(`/cameras/${camera.id}`);
  };

  return (
    <Layout>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Камеры</Typography>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setOpenDialog(true)}
        >
          Добавить камеру
        </Button>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => dispatch(clearError())}>
          {error}
        </Alert>
      )}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
          <CircularProgress />
        </Box>
      ) : cameras.length === 0 ? (
        <Box sx={{ textAlign: 'center', mt: 4 }}>
          <Typography variant="h6" color="text.secondary">
            Камеры не найдены
          </Typography>
          <Button
            variant="outlined"
            startIcon={<AddIcon />}
            onClick={() => setOpenDialog(true)}
            sx={{ mt: 2 }}
          >
            Добавить первую камеру
          </Button>
        </Box>
      ) : (
        <Grid container spacing={3}>
          {cameras.map((camera) => (
            <Grid item xs={12} sm={6} md={4} key={camera.id}>
              <CameraCard
                camera={camera}
                onEdit={handleEditCamera}
                onDelete={handleDeleteCamera}
              />
            </Grid>
          ))}
        </Grid>
      )}

      <Dialog open={openDialog} onClose={() => setOpenDialog(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Добавить камеру</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Название"
            fullWidth
            variant="outlined"
            value={formData.name}
            onChange={(e) => setFormData({ ...formData, name: e.target.value })}
            sx={{ mb: 2 }}
          />
          <TextField
            margin="dense"
            label="URL (RTSP)"
            fullWidth
            variant="outlined"
            value={formData.url}
            onChange={(e) => setFormData({ ...formData, url: e.target.value })}
            placeholder="rtsp://192.168.1.100:554/stream"
            sx={{ mb: 2 }}
          />
          <TextField
            margin="dense"
            label="Имя пользователя"
            fullWidth
            variant="outlined"
            value={formData.username}
            onChange={(e) => setFormData({ ...formData, username: e.target.value })}
            sx={{ mb: 2 }}
          />
          <TextField
            margin="dense"
            label="Пароль"
            type="password"
            fullWidth
            variant="outlined"
            value={formData.password}
            onChange={(e) => setFormData({ ...formData, password: e.target.value })}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Отмена</Button>
          <Button
            onClick={handleCreateCamera}
            variant="contained"
            disabled={!formData.name || !formData.url}
          >
            Добавить
          </Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
}

export default function CamerasPage() {
  return (
    <ProtectedRoute>
      <CamerasContent />
    </ProtectedRoute>
  );
}

