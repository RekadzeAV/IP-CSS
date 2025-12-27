'use client';

import React, { useRef, useEffect, useState, useCallback } from 'react';
import {
  Box,
  Paper,
  Typography,
  Button,
  IconButton,
  Tooltip,
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
  Alert,
} from '@mui/material';
import {
  Add as AddIcon,
  Delete as DeleteIcon,
  Edit as EditIcon,
  Save as SaveIcon,
  Cancel as CancelIcon,
  Visibility as VisibilityIcon,
  VisibilityOff as VisibilityOffIcon,
} from '@mui/icons-material';
import { useSnackbar } from 'notistack';
import type { Camera, DetectionZone } from '@/types';

interface MotionZoneEditorProps {
  camera: Camera;
  videoElement?: HTMLVideoElement | null;
  onSave?: (zones: DetectionZone[]) => Promise<void>;
}

export default function MotionZoneEditor({
  camera,
  videoElement,
  onSave,
}: MotionZoneEditorProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const { enqueueSnackbar } = useSnackbar();
  const [zones, setZones] = useState<DetectionZone[]>([]);
  const [editingZone, setEditingZone] = useState<DetectionZone | null>(null);
  const [isDrawing, setIsDrawing] = useState(false);
  const [currentPoints, setCurrentPoints] = useState<Array<{ x: number; y: number }>>([]);
  const [selectedZone, setSelectedZone] = useState<string | null>(null);
  const [zoneDialogOpen, setZoneDialogOpen] = useState(false);
  const [zoneName, setZoneName] = useState('');
  const [zoneSensitivity, setZoneSensitivity] = useState(50);

  // Загружаем зоны из настроек камеры
  useEffect(() => {
    if (camera.settings?.analytics?.detectionZones) {
      setZones(camera.settings.analytics.detectionZones as DetectionZone[]);
    }
  }, [camera]);

  // Отрисовка зон на canvas
  const drawZones = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Получаем размеры из video элемента или используем размеры canvas
    let videoWidth = 640;
    let videoHeight = 480;

    if (videoElement) {
      videoWidth = videoElement.videoWidth || videoElement.clientWidth || 640;
      videoHeight = videoElement.videoHeight || videoElement.clientHeight || 480;
    } else {
      // Используем размеры canvas если video элемент недоступен
      const rect = canvas.getBoundingClientRect();
      videoWidth = rect.width;
      videoHeight = rect.height;
    }

    // Синхронизируем размер canvas с видео
    canvas.width = videoWidth;
    canvas.height = videoHeight;

    // Очищаем canvas
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Рисуем все зоны
    zones.forEach((zone) => {
      if (zone.points.length < 3) return;

      ctx.beginPath();
      ctx.moveTo(zone.points[0].x, zone.points[0].y);

      for (let i = 1; i < zone.points.length; i++) {
        ctx.lineTo(zone.points[i].x, zone.points[i].y);
      }

      ctx.closePath();

      // Заливка
      ctx.fillStyle = zone.enabled
        ? `rgba(255, 0, 0, ${0.2 + (zone.sensitivity || 50) / 200})`
        : 'rgba(128, 128, 128, 0.2)';
      ctx.fill();

      // Обводка
      ctx.strokeStyle = zone.enabled
        ? selectedZone === zone.id
          ? '#ff0000'
          : '#ff6666'
        : '#888888';
      ctx.lineWidth = selectedZone === zone.id ? 3 : 2;
      ctx.stroke();

      // Текст с названием
      if (zone.points.length > 0) {
        const centerX = zone.points.reduce((sum, p) => sum + p.x, 0) / zone.points.length;
        const centerY = zone.points.reduce((sum, p) => sum + p.y, 0) / zone.points.length;

        ctx.fillStyle = zone.enabled ? '#ffffff' : '#888888';
        ctx.font = '14px Arial';
        ctx.textAlign = 'center';
        ctx.fillText(zone.name || `Zone ${zone.id}`, centerX, centerY);
      }
    }

    // Рисуем текущую зону в процессе создания
    if (isDrawing && currentPoints.length > 0) {
      ctx.beginPath();
      ctx.moveTo(currentPoints[0].x, currentPoints[0].y);

      for (let i = 1; i < currentPoints.length; i++) {
        ctx.lineTo(currentPoints[i].x, currentPoints[i].y);
      }

      ctx.strokeStyle = '#00ff00';
      ctx.lineWidth = 2;
      ctx.setLineDash([5, 5]);
      ctx.stroke();
      ctx.setLineDash([]);
    }
  }, [zones, videoElement, isDrawing, currentPoints, selectedZone]);

  useEffect(() => {
    drawZones();
  }, [drawZones]);

  // Обработка клика на canvas
  const handleCanvasClick = useCallback(
    (event: React.MouseEvent<HTMLCanvasElement>) => {
      if (!canvasRef.current) return;

      const canvas = canvasRef.current;
      const rect = canvas.getBoundingClientRect();
      const x = ((event.clientX - rect.left) / rect.width) * canvas.width;
      const y = ((event.clientY - rect.top) / rect.height) * canvas.height;

      if (isDrawing) {
        // Добавляем точку к текущей зоне
        setCurrentPoints([...currentPoints, { x, y }]);
      } else {
        // Проверяем, кликнули ли на существующую зону
        const clickedZone = zones.find((zone) => {
          if (zone.points.length < 3) return false;
          // Простая проверка попадания в полигон
          let inside = false;
          for (let i = 0, j = zone.points.length - 1; i < zone.points.length; j = i++) {
            const xi = zone.points[i].x;
            const yi = zone.points[i].y;
            const xj = zone.points[j].x;
            const yj = zone.points[j].y;

            const intersect =
              yi > y !== yj > y && x < ((xj - xi) * (y - yi)) / (yj - yi) + xi;
            if (intersect) inside = !inside;
          }
          return inside;
        });

        if (clickedZone) {
          setSelectedZone(clickedZone.id);
        } else {
          setSelectedZone(null);
        }
      }
    },
    [isDrawing, currentPoints, zones]
  );

  // Начать создание новой зоны
  const handleStartDrawing = () => {
    setIsDrawing(true);
    setCurrentPoints([]);
    setSelectedZone(null);
  };

  // Завершить создание зоны
  const handleFinishDrawing = () => {
    if (currentPoints.length < 3) {
      enqueueSnackbar('Зона должна содержать минимум 3 точки', { variant: 'warning' });
      return;
    }

    setZoneDialogOpen(true);
  };

  // Сохранить новую зону
  const handleSaveZone = () => {
    if (!zoneName.trim()) {
      enqueueSnackbar('Введите название зоны', { variant: 'warning' });
      return;
    }

    const newZone: DetectionZone = {
      id: `zone-${Date.now()}`,
      name: zoneName,
      points: [...currentPoints],
      enabled: true,
      sensitivity: zoneSensitivity,
    };

    setZones([...zones, newZone]);
    setCurrentPoints([]);
    setIsDrawing(false);
    setZoneName('');
    setZoneSensitivity(50);
    setZoneDialogOpen(false);
    enqueueSnackbar('Зона создана', { variant: 'success' });
  };

  // Удалить зону
  const handleDeleteZone = (zoneId: string) => {
    setZones(zones.filter((z) => z.id !== zoneId));
    if (selectedZone === zoneId) {
      setSelectedZone(null);
    }
    enqueueSnackbar('Зона удалена', { variant: 'success' });
  };

  // Переключить активность зоны
  const handleToggleZone = (zoneId: string) => {
    setZones(
      zones.map((z) => (z.id === zoneId ? { ...z, enabled: !z.enabled } : z))
    );
  };

  // Сохранить все зоны
  const handleSaveAll = async () => {
    if (onSave) {
      try {
        await onSave(zones);
        enqueueSnackbar('Зоны сохранены', { variant: 'success' });
      } catch (error: any) {
        enqueueSnackbar(error.message || 'Ошибка при сохранении зон', { variant: 'error' });
      }
    }
  };


  return (
    <Paper sx={{ p: 2 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h6">Редактор зон детекции движения</Typography>
        <Box>
          {isDrawing ? (
            <>
              <Button
                startIcon={<SaveIcon />}
                onClick={handleFinishDrawing}
                variant="contained"
                sx={{ mr: 1 }}
                disabled={currentPoints.length < 3}
              >
                Завершить
              </Button>
              <Button
                startIcon={<CancelIcon />}
                onClick={() => {
                  setIsDrawing(false);
                  setCurrentPoints([]);
                }}
              >
                Отмена
              </Button>
            </>
          ) : (
            <>
              <Button startIcon={<AddIcon />} onClick={handleStartDrawing} sx={{ mr: 1 }}>
                Новая зона
              </Button>
              <Button startIcon={<SaveIcon />} onClick={handleSaveAll} variant="contained">
                Сохранить все
              </Button>
            </>
          )}
        </Box>
      </Box>

      <Box sx={{ position: 'relative', mb: 2, minHeight: 300 }}>
        {videoElement ? (
          <Box sx={{ position: 'relative', width: '100%' }}>
            {/* Canvas поверх видео для рисования зон */}
            <canvas
              ref={canvasRef}
              onClick={handleCanvasClick}
              style={{
                position: 'absolute',
                top: 0,
                left: 0,
                width: '100%',
                height: '100%',
                cursor: isDrawing ? 'crosshair' : 'pointer',
                zIndex: 10,
                backgroundColor: 'transparent',
              }}
            />
            {/* Видео элемент (используем существующий из VideoPlayer) */}
            <Box
              sx={{
                width: '100%',
                height: 'auto',
                '& video': {
                  width: '100%',
                  height: 'auto',
                  display: 'block',
                },
              }}
            >
              {/* Видео уже отображается в VideoPlayer, здесь только canvas для зон */}
            </Box>
          </Box>
        ) : (
          <Alert severity="info" sx={{ mb: 2 }}>
            Видео элемент не найден. Запустите видеоплеер выше для редактирования зон.
          </Alert>
        )}
      </Box>

      {isDrawing && (
        <Alert severity="info" sx={{ mb: 2 }}>
          Кликайте на видео для добавления точек зоны. Минимум 3 точки.
        </Alert>
      )}

      <Typography variant="subtitle1" gutterBottom>
        Зоны ({zones.length})
      </Typography>

      {zones.length === 0 ? (
        <Typography variant="body2" color="text.secondary">
          Нет созданных зон. Нажмите "Новая зона" для создания.
        </Typography>
      ) : (
        <List>
          {zones.map((zone) => (
            <ListItem
              key={zone.id}
              sx={{
                border: 1,
                borderColor: selectedZone === zone.id ? 'primary.main' : 'divider',
                borderRadius: 1,
                mb: 1,
                backgroundColor: selectedZone === zone.id ? 'action.selected' : 'transparent',
              }}
            >
              <ListItemText
                primary={
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    <Typography variant="body1">{zone.name}</Typography>
                    <Chip
                      label={zone.enabled ? 'Активна' : 'Неактивна'}
                      color={zone.enabled ? 'success' : 'default'}
                      size="small"
                    />
                    {zone.sensitivity && (
                      <Chip label={`Чувствительность: ${zone.sensitivity}%`} size="small" variant="outlined" />
                    )}
                  </Box>
                }
                secondary={`Точек: ${zone.points.length}`}
              />
              <ListItemSecondaryAction>
                <Tooltip title={zone.enabled ? 'Отключить' : 'Включить'}>
                  <IconButton
                    edge="end"
                    onClick={() => handleToggleZone(zone.id)}
                    sx={{ mr: 1 }}
                  >
                    {zone.enabled ? <VisibilityIcon /> : <VisibilityOffIcon />}
                  </IconButton>
                </Tooltip>
                <Tooltip title="Удалить">
                  <IconButton edge="end" onClick={() => handleDeleteZone(zone.id)} color="error">
                    <DeleteIcon />
                  </IconButton>
                </Tooltip>
              </ListItemSecondaryAction>
            </ListItem>
          ))}
        </List>
      )}

      <Dialog open={zoneDialogOpen} onClose={() => setZoneDialogOpen(false)}>
        <DialogTitle>Создать зону</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            label="Название зоны"
            value={zoneName}
            onChange={(e) => setZoneName(e.target.value)}
            sx={{ mb: 2, mt: 1 }}
          />
          <TextField
            fullWidth
            type="number"
            label="Чувствительность (%)"
            value={zoneSensitivity}
            onChange={(e) => setZoneSensitivity(Number(e.target.value))}
            inputProps={{ min: 0, max: 100 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setZoneDialogOpen(false)}>Отмена</Button>
          <Button onClick={handleSaveZone} variant="contained" disabled={!zoneName.trim()}>
            Создать
          </Button>
        </DialogActions>
      </Dialog>
    </Paper>
  );
}

