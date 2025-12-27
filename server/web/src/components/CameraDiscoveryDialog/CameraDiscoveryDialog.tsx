'use client';

import React, { useState } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  Box,
  CircularProgress,
  Typography,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  Chip,
  Alert,
  IconButton,
  Tooltip,
} from '@mui/material';
import {
  Add as AddIcon,
  Refresh as RefreshIcon,
  CheckCircle as CheckCircleIcon,
  Error as ErrorIcon,
} from '@mui/icons-material';
import { useAppDispatch } from '@/store/hooks';
import { discoverCameras, createCamera } from '@/store/slices/camerasSlice';
import { useSnackbar } from 'notistack';
import type { DiscoveredCameraDto } from '@/types';

interface CameraDiscoveryDialogProps {
  open: boolean;
  onClose: () => void;
}

export default function CameraDiscoveryDialog({ open, onClose }: CameraDiscoveryDialogProps) {
  const dispatch = useAppDispatch();
  const { enqueueSnackbar } = useSnackbar();
  const [discovering, setDiscovering] = useState(false);
  const [discoveredCameras, setDiscoveredCameras] = useState<DiscoveredCameraDto[]>([]);
  const [addingCameras, setAddingCameras] = useState<Set<string>>(new Set());

  const handleDiscover = async () => {
    try {
      setDiscovering(true);
      setDiscoveredCameras([]);
      const result = await dispatch(discoverCameras()).unwrap();
      setDiscoveredCameras(result);
      if (result.length === 0) {
        enqueueSnackbar('Камеры не найдены', { variant: 'info' });
      } else {
        enqueueSnackbar(`Найдено камер: ${result.length}`, { variant: 'success' });
      }
    } catch (error: any) {
      enqueueSnackbar(error || 'Ошибка при обнаружении камер', { variant: 'error' });
    } finally {
      setDiscovering(false);
    }
  };

  const handleAddCamera = async (discoveredCamera: DiscoveredCameraDto) => {
    try {
      setAddingCameras((prev) => new Set(prev).add(discoveredCamera.id || discoveredCamera.url));
      await dispatch(
        createCamera({
          name: discoveredCamera.name || `Камера ${discoveredCamera.url}`,
          url: discoveredCamera.url,
          username: discoveredCamera.username,
          password: discoveredCamera.password,
        })
      ).unwrap();
      enqueueSnackbar('Камера успешно добавлена', { variant: 'success' });
      // Удаляем из списка обнаруженных
      setDiscoveredCameras((prev) =>
        prev.filter((c) => (c.id || c.url) !== (discoveredCamera.id || discoveredCamera.url))
      );
    } catch (error: any) {
      enqueueSnackbar(error || 'Ошибка при добавлении камеры', { variant: 'error' });
    } finally {
      setAddingCameras((prev) => {
        const next = new Set(prev);
        next.delete(discoveredCamera.id || discoveredCamera.url);
        return next;
      });
    }
  };

  const handleAddAll = async () => {
    for (const camera of discoveredCameras) {
      await handleAddCamera(camera);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Typography variant="h6">Обнаружение камер</Typography>
          <Button
            startIcon={discovering ? <CircularProgress size={16} /> : <RefreshIcon />}
            onClick={handleDiscover}
            disabled={discovering}
            variant="outlined"
          >
            {discovering ? 'Поиск...' : 'Найти камеры'}
          </Button>
        </Box>
      </DialogTitle>
      <DialogContent>
        {discoveredCameras.length === 0 && !discovering && (
          <Alert severity="info" sx={{ mb: 2 }}>
            Нажмите "Найти камеры" для начала поиска камер в сети
          </Alert>
        )}

        {discovering && (
          <Box sx={{ display: 'flex', flexDirection: 'column', alignItems: 'center', py: 4 }}>
            <CircularProgress />
            <Typography variant="body2" color="text.secondary" sx={{ mt: 2 }}>
              Поиск камер в сети...
            </Typography>
          </Box>
        )}

        {discoveredCameras.length > 0 && (
          <>
            <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
              <Typography variant="body2" color="text.secondary">
                Найдено камер: {discoveredCameras.length}
              </Typography>
              <Button
                size="small"
                startIcon={<AddIcon />}
                onClick={handleAddAll}
                disabled={addingCameras.size > 0}
                variant="outlined"
              >
                Добавить все
              </Button>
            </Box>
            <List>
              {discoveredCameras.map((camera, index) => {
                const isAdding = addingCameras.has(camera.id || camera.url);
                return (
                  <ListItem
                    key={camera.id || camera.url || index}
                    sx={{
                      border: 1,
                      borderColor: 'divider',
                      borderRadius: 1,
                      mb: 1,
                    }}
                  >
                    <ListItemText
                      primary={camera.name || `Камера ${camera.url}`}
                      secondary={
                        <Box>
                          <Typography variant="caption" display="block">
                            URL: {camera.url}
                          </Typography>
                          {camera.model && (
                            <Typography variant="caption" display="block">
                              Модель: {camera.model}
                            </Typography>
                          )}
                          {camera.manufacturer && (
                            <Typography variant="caption" display="block">
                              Производитель: {camera.manufacturer}
                            </Typography>
                          )}
                          {camera.onvif && (
                            <Chip label="ONVIF" size="small" color="primary" sx={{ mt: 0.5 }} />
                          )}
                        </Box>
                      }
                    />
                    <ListItemSecondaryAction>
                      <Tooltip title="Добавить камеру">
                        <IconButton
                          edge="end"
                          onClick={() => handleAddCamera(camera)}
                          disabled={isAdding}
                          color="primary"
                        >
                          {isAdding ? <CircularProgress size={20} /> : <AddIcon />}
                        </IconButton>
                      </Tooltip>
                    </ListItemSecondaryAction>
                  </ListItem>
                );
              })}
            </List>
          </>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose}>Закрыть</Button>
      </DialogActions>
    </Dialog>
  );
}

