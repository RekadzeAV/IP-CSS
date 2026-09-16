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
  Chip,
  Alert,
  IconButton,
  Tooltip,
  RadioGroup,
  FormControlLabel,
  Radio,
  FormControl,
  FormLabel,
} from '@mui/material';
import {
  Add as AddIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material';
import { useAppDispatch } from '@/store/hooks';
import { discoverCameras, createCamera } from '@/store/slices/camerasSlice';
import { useSnackbar } from 'notistack';
import type { DiscoveredCameraDto } from '@/types';

interface CameraDiscoveryDialogProps {
  open: boolean;
  onClose: () => void;
}

const getErrorMessage = (error: unknown, fallback: string): string =>
  error instanceof Error ? error.message : fallback;

type ExtendedDiscoveredCamera = DiscoveredCameraDto & {
  id?: string;
  onvif?: boolean;
  username?: string;
  password?: string;
};

const blurActiveElement = () => {
  const activeElement = document.activeElement;
  if (activeElement instanceof HTMLElement) {
    activeElement.blur();
  }
};

export default function CameraDiscoveryDialog({ open, onClose }: CameraDiscoveryDialogProps) {
  const dispatch = useAppDispatch();
  const { enqueueSnackbar } = useSnackbar();
  const [discovering, setDiscovering] = useState(false);
  const [discoveredCameras, setDiscoveredCameras] = useState<ExtendedDiscoveredCamera[]>([]);
  const [addingCameras, setAddingCameras] = useState<Set<string>>(new Set());
  const [discoveryProtocol, setDiscoveryProtocol] = useState<'onvif' | 'rtsp' | 'all'>('onvif');

  const handleDiscover = async () => {
    try {
      setDiscovering(true);
      setDiscoveredCameras([]);
      
      // Используем стандартный action с параметром протокола
      const result = await dispatch(discoverCameras(discoveryProtocol)).unwrap() as ExtendedDiscoveredCamera[];
      setDiscoveredCameras(result);
      
      if (result.length === 0) {
        enqueueSnackbar('Камеры не найдены', { variant: 'info' });
      } else {
        enqueueSnackbar(`Найдено камер: ${result.length}`, { variant: 'success' });
      }
    } catch (error: unknown) {
      enqueueSnackbar(getErrorMessage(error, 'Ошибка при обнаружении камер'), { variant: 'error' });
    } finally {
      setDiscovering(false);
    }
  };

  const handleAddCamera = async (discoveredCamera: ExtendedDiscoveredCamera) => {
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
    } catch (error: unknown) {
      enqueueSnackbar(getErrorMessage(error, 'Ошибка при добавлении камеры'), { variant: 'error' });
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

  const handleDialogClose = () => {
    blurActiveElement();
    onClose();
  };

  return (
    <Dialog open={open} onClose={handleDialogClose} maxWidth="md" fullWidth>
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
        <FormControl component="fieldset" sx={{ mb: 3, width: '100%' }}>
          <FormLabel component="legend" sx={{ mb: 1 }}>Метод обнаружения</FormLabel>
          <RadioGroup
            value={discoveryProtocol}
            onChange={(e) => setDiscoveryProtocol(e.target.value as 'onvif' | 'rtsp' | 'all')}
            sx={{ ml: 2 }}
          >
            <FormControlLabel
              value="onvif"
              control={<Radio />}
              label={
                <Box>
                  <Typography variant="body1">ONVIF WS-Discovery</Typography>
                  <Typography variant="caption" color="text.secondary" display="block">
                    Поиск камер через ONVIF протокол (рекомендуется)
                  </Typography>
                </Box>
              }
            />
            <FormControlLabel
              value="rtsp"
              control={<Radio />}
              label={
                <Box>
                  <Typography variant="body1">RTSP только</Typography>
                  <Typography variant="caption" color="text.secondary" display="block">
                    Проверка известных RTSP адресов из конфигурации
                  </Typography>
                </Box>
              }
            />
            <FormControlLabel
              value="all"
              control={<Radio />}
              label={
                <Box>
                  <Typography variant="body1">Комбинированный</Typography>
                  <Typography variant="caption" color="text.secondary" display="block">
                    ONVIF + RTSP fallback (максимальное покрытие)
                  </Typography>
                </Box>
              }
            />
          </RadioGroup>
        </FormControl>

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
                    secondaryAction={
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
                    }
                    sx={{
                      border: 1,
                      borderColor: 'divider',
                      borderRadius: 1,
                      mb: 1,
                    }}
                  >
                    <ListItemText
                      primary={camera.name || `Камера ${camera.url}`}
                      secondaryTypographyProps={{ component: 'div' }}
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
                  </ListItem>
                );
              })}
            </List>
          </>
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={handleDialogClose}>Закрыть</Button>
      </DialogActions>
    </Dialog>
  );
}



