'use client';

import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Box,
  Grid,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Switch,
  FormControlLabel,
  Typography,
  Divider,
  Alert,
} from '@mui/material';
import { useForm, Controller } from 'react-hook-form';
import { yupResolver } from '@hookform/resolvers/yup';
import * as yup from 'yup';
import { useAppDispatch } from '@/store/hooks';
import { updateCamera } from '@/store/slices/camerasSlice';
import { useSnackbar } from 'notistack';
import type { Camera, UpdateCameraRequest } from '@/types';

interface CameraEditDialogProps {
  open: boolean;
  onClose: () => void;
  camera: Camera | null;
}

const schema = yup.object({
  name: yup.string().required('Название обязательно'),
  url: yup.string().url('Некорректный URL').required('URL обязателен'),
  username: yup.string(),
  password: yup.string(),
  fps: yup.number().min(1).max(60),
  bitrate: yup.number().min(100),
  codec: yup.string(),
});

export default function CameraEditDialog({ open, onClose, camera }: CameraEditDialogProps) {
  const dispatch = useAppDispatch();
  const { enqueueSnackbar } = useSnackbar();
  const [loading, setLoading] = useState(false);

  const {
    control,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<UpdateCameraRequest>({
    resolver: yupResolver(schema),
    defaultValues: {
      name: '',
      url: '',
      username: '',
      password: '',
      fps: 30,
      bitrate: 2000,
      codec: 'H264',
    },
  });

  useEffect(() => {
    if (camera && open) {
      reset({
        name: camera.name || '',
        url: camera.url || '',
        username: camera.credentials?.username || '',
        password: '', // Не заполняем пароль для безопасности
        fps: camera.fps || 30,
        bitrate: camera.bitrate || 2000,
        codec: camera.codec || 'H264',
      });
    }
  }, [camera, open, reset]);

  const onSubmit = async (data: UpdateCameraRequest) => {
    if (!camera) return;

    try {
      setLoading(true);
      await dispatch(updateCamera({ id: camera.id, data })).unwrap();
      enqueueSnackbar('Камера успешно обновлена', { variant: 'success' });
      onClose();
    } catch (error: any) {
      enqueueSnackbar(error || 'Ошибка при обновлении камеры', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <form onSubmit={handleSubmit(onSubmit)}>
        <DialogTitle>Редактировать камеру</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 2 }}>
            <Grid container spacing={2}>
              <Grid item xs={12}>
                <Controller
                  name="name"
                  control={control}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      fullWidth
                      label="Название"
                      error={!!errors.name}
                      helperText={errors.name?.message}
                    />
                  )}
                />
              </Grid>

              <Grid item xs={12}>
                <Controller
                  name="url"
                  control={control}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      fullWidth
                      label="URL (RTSP)"
                      placeholder="rtsp://192.168.1.100:554/stream"
                      error={!!errors.url}
                      helperText={errors.url?.message}
                    />
                  )}
                />
              </Grid>

              <Grid item xs={12} sm={6}>
                <Controller
                  name="username"
                  control={control}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      fullWidth
                      label="Имя пользователя"
                      error={!!errors.username}
                      helperText={errors.username?.message}
                    />
                  )}
                />
              </Grid>

              <Grid item xs={12} sm={6}>
                <Controller
                  name="password"
                  control={control}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      fullWidth
                      type="password"
                      label="Пароль"
                      helperText="Оставьте пустым, чтобы не изменять"
                      error={!!errors.password}
                    />
                  )}
                />
              </Grid>

              <Grid item xs={12}>
                <Divider sx={{ my: 1 }} />
                <Typography variant="subtitle2" gutterBottom>
                  Настройки видео
                </Typography>
              </Grid>

              <Grid item xs={12} sm={4}>
                <Controller
                  name="fps"
                  control={control}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      fullWidth
                      type="number"
                      label="FPS"
                      inputProps={{ min: 1, max: 60 }}
                      error={!!errors.fps}
                      helperText={errors.fps?.message}
                    />
                  )}
                />
              </Grid>

              <Grid item xs={12} sm={4}>
                <Controller
                  name="bitrate"
                  control={control}
                  render={({ field }) => (
                    <TextField
                      {...field}
                      fullWidth
                      type="number"
                      label="Битрейт (kbps)"
                      inputProps={{ min: 100 }}
                      error={!!errors.bitrate}
                      helperText={errors.bitrate?.message}
                    />
                  )}
                />
              </Grid>

              <Grid item xs={12} sm={4}>
                <Controller
                  name="codec"
                  control={control}
                  render={({ field }) => (
                    <FormControl fullWidth>
                      <InputLabel>Кодек</InputLabel>
                      <Select {...field} label="Кодек">
                        <MenuItem value="H264">H.264</MenuItem>
                        <MenuItem value="H265">H.265</MenuItem>
                        <MenuItem value="MJPEG">MJPEG</MenuItem>
                      </Select>
                    </FormControl>
                  )}
                />
              </Grid>
            </Grid>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={loading}>
            Отмена
          </Button>
          <Button type="submit" variant="contained" disabled={loading}>
            Сохранить
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
}

