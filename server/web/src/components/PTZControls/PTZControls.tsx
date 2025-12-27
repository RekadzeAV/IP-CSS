'use client';

import React, { useState, useEffect, useCallback } from 'react';
import {
  Box,
  Paper,
  IconButton,
  Slider,
  Typography,
  Grid,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  Chip,
  Tooltip,
} from '@mui/material';
import {
  ArrowUpward,
  ArrowDownward,
  ArrowBack,
  ArrowForward,
  ZoomIn,
  ZoomOut,
  Stop,
  Bookmark,
  BookmarkBorder,
  Delete as DeleteIcon,
} from '@mui/icons-material';
import { ptzService } from '@/services/ptzService';
import { useSnackbar } from 'notistack';
import type { Camera } from '@/types';
import type { PTZPreset } from '@/services/ptzService';

interface PTZControlsProps {
  camera: Camera;
  disabled?: boolean;
}

export default function PTZControls({ camera, disabled = false }: PTZControlsProps) {
  const { enqueueSnackbar } = useSnackbar();
  const [speed, setSpeed] = useState(0.5);
  const [presets, setPresets] = useState<PTZPreset[]>([]);
  const [presetsDialogOpen, setPresetsDialogOpen] = useState(false);
  const [newPresetName, setNewPresetName] = useState('');
  const [loading, setLoading] = useState(false);

  // Проверяем, поддерживает ли камера PTZ
  const supportsPTZ = camera.ptz?.type === 'PTZ' || camera.ptz?.type === 'PT';

  useEffect(() => {
    if (supportsPTZ && camera.id) {
      loadPresets();
    }
  }, [camera.id, supportsPTZ]);

  const loadPresets = async () => {
    try {
      const data = await ptzService.getPresets(camera.id);
      setPresets(data);
    } catch (error: any) {
      console.error('Error loading presets:', error);
    }
  };

  const handlePTZAction = useCallback(
    async (action: 'move' | 'zoom', direction: string) => {
      if (disabled || !supportsPTZ) return;

      try {
        setLoading(true);
        const request: any = { action, direction, speed };

        if (action === 'move') {
          switch (direction) {
            case 'up':
              await ptzService.moveUp(camera.id, speed);
              break;
            case 'down':
              await ptzService.moveDown(camera.id, speed);
              break;
            case 'left':
              await ptzService.moveLeft(camera.id, speed);
              break;
            case 'right':
              await ptzService.moveRight(camera.id, speed);
              break;
          }
        } else if (action === 'zoom') {
          if (direction === 'in') {
            await ptzService.zoomIn(camera.id, speed);
          } else {
            await ptzService.zoomOut(camera.id, speed);
          }
        }
      } catch (error: any) {
        enqueueSnackbar(error?.message || 'Ошибка управления PTZ', { variant: 'error' });
      } finally {
        setLoading(false);
      }
    },
    [camera.id, speed, disabled, supportsPTZ, enqueueSnackbar]
  );

  const handleStop = useCallback(async () => {
    if (disabled || !supportsPTZ) return;

    try {
      setLoading(true);
      await ptzService.stopPTZ(camera.id);
    } catch (error: any) {
      enqueueSnackbar(error?.message || 'Ошибка остановки PTZ', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [camera.id, disabled, supportsPTZ, enqueueSnackbar]);

  const handleSavePreset = async () => {
    if (!newPresetName.trim()) {
      enqueueSnackbar('Введите название пресета', { variant: 'warning' });
      return;
    }

    try {
      setLoading(true);
      await ptzService.savePreset(camera.id, newPresetName);
      enqueueSnackbar('Пресет сохранен', { variant: 'success' });
      setNewPresetName('');
      await loadPresets();
    } catch (error: any) {
      enqueueSnackbar(error?.message || 'Ошибка сохранения пресета', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleRecallPreset = async (presetId: number) => {
    try {
      setLoading(true);
      await ptzService.recallPreset(camera.id, presetId);
      enqueueSnackbar('Пресет вызван', { variant: 'success' });
    } catch (error: any) {
      enqueueSnackbar(error?.message || 'Ошибка вызова пресета', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  const handleDeletePreset = async (presetId: number) => {
    if (!confirm('Удалить этот пресет?')) return;

    try {
      setLoading(true);
      await ptzService.deletePreset(camera.id, presetId);
      enqueueSnackbar('Пресет удален', { variant: 'success' });
      await loadPresets();
    } catch (error: any) {
      enqueueSnackbar(error?.message || 'Ошибка удаления пресета', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  };

  if (!supportsPTZ) {
    return (
      <Paper sx={{ p: 2 }}>
        <Typography variant="body2" color="text.secondary" align="center">
          Эта камера не поддерживает PTZ управление
        </Typography>
      </Paper>
    );
  }

  return (
    <Paper sx={{ p: 2 }}>
      <Typography variant="h6" gutterBottom>
        PTZ Управление
      </Typography>

      {/* Скорость */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="body2" gutterBottom>
          Скорость: {Math.round(speed * 100)}%
        </Typography>
        <Slider
          value={speed}
          min={0.1}
          max={1}
          step={0.1}
          onChange={(_, value) => setSpeed(value as number)}
          disabled={disabled || loading}
          marks={[
            { value: 0.1, label: '10%' },
            { value: 0.5, label: '50%' },
            { value: 1, label: '100%' },
          ]}
        />
      </Box>

      {/* Панель управления */}
      <Grid container spacing={2} sx={{ mb: 2 }}>
        {/* Верхняя строка - Вверх */}
        <Grid item xs={12} sx={{ display: 'flex', justifyContent: 'center' }}>
          <Tooltip title="Вверх">
            <IconButton
              color="primary"
              size="large"
              disabled={disabled || loading}
              onMouseDown={() => handlePTZAction('move', 'up')}
              onMouseUp={handleStop}
              onMouseLeave={handleStop}
            >
              <ArrowUpward />
            </IconButton>
          </Tooltip>
        </Grid>

        {/* Средняя строка - Влево, Стоп, Вправо */}
        <Grid item xs={12} sx={{ display: 'flex', justifyContent: 'center', gap: 1 }}>
          <Tooltip title="Влево">
            <IconButton
              color="primary"
              size="large"
              disabled={disabled || loading}
              onMouseDown={() => handlePTZAction('move', 'left')}
              onMouseUp={handleStop}
              onMouseLeave={handleStop}
            >
              <ArrowBack />
            </IconButton>
          </Tooltip>

          <Tooltip title="Стоп">
            <IconButton
              color="error"
              size="large"
              disabled={disabled || loading}
              onClick={handleStop}
            >
              <Stop />
            </IconButton>
          </Tooltip>

          <Tooltip title="Вправо">
            <IconButton
              color="primary"
              size="large"
              disabled={disabled || loading}
              onMouseDown={() => handlePTZAction('move', 'right')}
              onMouseUp={handleStop}
              onMouseLeave={handleStop}
            >
              <ArrowForward />
            </IconButton>
          </Tooltip>
        </Grid>

        {/* Нижняя строка - Вниз */}
        <Grid item xs={12} sx={{ display: 'flex', justifyContent: 'center' }}>
          <Tooltip title="Вниз">
            <IconButton
              color="primary"
              size="large"
              disabled={disabled || loading}
              onMouseDown={() => handlePTZAction('move', 'down')}
              onMouseUp={handleStop}
              onMouseLeave={handleStop}
            >
              <ArrowDownward />
            </IconButton>
          </Tooltip>
        </Grid>

        {/* Zoom */}
        <Grid item xs={12} sx={{ display: 'flex', justifyContent: 'center', gap: 2, mt: 2 }}>
          <Tooltip title="Zoom In">
            <IconButton
              color="secondary"
              disabled={disabled || loading}
              onMouseDown={() => handlePTZAction('zoom', 'in')}
              onMouseUp={handleStop}
              onMouseLeave={handleStop}
            >
              <ZoomIn />
            </IconButton>
          </Tooltip>
          <Tooltip title="Zoom Out">
            <IconButton
              color="secondary"
              disabled={disabled || loading}
              onMouseDown={() => handlePTZAction('zoom', 'out')}
              onMouseUp={handleStop}
              onMouseLeave={handleStop}
            >
              <ZoomOut />
            </IconButton>
          </Tooltip>
        </Grid>
      </Grid>

      {/* Пресеты */}
      <Box sx={{ mt: 3, pt: 2, borderTop: 1, borderColor: 'divider' }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
          <Typography variant="subtitle1">Пресеты</Typography>
          <Button
            size="small"
            startIcon={<BookmarkBorder />}
            onClick={() => setPresetsDialogOpen(true)}
            disabled={disabled || loading}
          >
            Управление
          </Button>
        </Box>

        {presets.length > 0 ? (
          <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
            {presets.map((preset) => (
              <Chip
                key={preset.id}
                label={preset.name}
                onClick={() => handleRecallPreset(preset.id)}
                onDelete={() => handleDeletePreset(preset.id)}
                disabled={disabled || loading}
                color="primary"
                variant="outlined"
              />
            ))}
          </Box>
        ) : (
          <Typography variant="body2" color="text.secondary">
            Пресеты отсутствуют
          </Typography>
        )}
      </Box>

      {/* Диалог управления пресетами */}
      <Dialog open={presetsDialogOpen} onClose={() => setPresetsDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Управление пресетами</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            label="Название пресета"
            value={newPresetName}
            onChange={(e) => setNewPresetName(e.target.value)}
            sx={{ mb: 2 }}
            onKeyPress={(e) => {
              if (e.key === 'Enter') {
                handleSavePreset();
              }
            }}
          />
          <Button
            variant="outlined"
            startIcon={<Bookmark />}
            onClick={handleSavePreset}
            disabled={!newPresetName.trim() || loading}
            fullWidth
            sx={{ mb: 2 }}
          >
            Сохранить текущую позицию
          </Button>

          <Typography variant="subtitle2" gutterBottom sx={{ mt: 2 }}>
            Сохраненные пресеты:
          </Typography>
          <List>
            {presets.map((preset) => (
              <ListItem key={preset.id}>
                <ListItemText
                  primary={preset.name}
                  secondary={`Pan: ${preset.position.pan}, Tilt: ${preset.position.tilt}, Zoom: ${preset.position.zoom}`}
                />
                <ListItemSecondaryAction>
                  <IconButton
                    edge="end"
                    onClick={() => handleRecallPreset(preset.id)}
                    disabled={loading}
                    sx={{ mr: 1 }}
                  >
                    <BookmarkBorder />
                  </IconButton>
                  <IconButton
                    edge="end"
                    onClick={() => handleDeletePreset(preset.id)}
                    disabled={loading}
                    color="error"
                  >
                    <DeleteIcon />
                  </IconButton>
                </ListItemSecondaryAction>
              </ListItem>
            ))}
          </List>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setPresetsDialogOpen(false)}>Закрыть</Button>
        </DialogActions>
      </Dialog>
    </Paper>
  );
}

