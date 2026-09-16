'use client';

import React, { useMemo, useState, useCallback, useRef } from 'react';
import {
  Box,
  Paper,
  Typography,
  Chip,
  Tooltip,
  IconButton,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Slider,
  Button,
  Menu,
  ListItemIcon,
  ListItemText,
  Divider,
} from '@mui/material';
import {
  ZoomIn as ZoomInIcon,
  ZoomOut as ZoomOutIcon,
  Today as TodayIcon,
  FilterList as FilterListIcon,
  PlayArrow as PlayArrowIcon,
  Image as ImageIcon,
} from '@mui/icons-material';
import { format, startOfDay, endOfDay, subDays, addDays } from 'date-fns';
import { ru } from 'date-fns/locale';
import type { Event, EventType, EventSeverity } from '@/types';

interface EventTimelineProps {
  events: Event[];
  onEventClick?: (event: Event) => void;
  onEventSeek?: (event: Event) => void; // Переход к моменту события в видеоплеере
  onExport?: () => void; // Экспорт временной шкалы
  height?: number;
  cameras?: Array<{ id: string; name: string }>;
}

interface TimelineEvent extends Event {
  position: number; // Позиция на временной шкале (0-100)
}

export default function EventTimelineEnhanced({
  events,
  onEventClick,
  onEventSeek,
  onExport,
  height = 200,
  cameras = [],
}: EventTimelineProps) {
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [zoom, setZoom] = useState(1); // 1 = день, 2 = 12 часов, 4 = 6 часов
  const [filterType, setFilterType] = useState<EventType | 'ALL'>('ALL');
  const [filterSeverity, setFilterSeverity] = useState<EventSeverity | 'ALL'>('ALL');
  const [filterCameraId, setFilterCameraId] = useState<string>('ALL');
  const [selectedEvents, setSelectedEvents] = useState<Set<string>>(new Set());
  const [contextMenu, setContextMenu] = useState<{
    mouseX: number;
    mouseY: number;
    event: Event | null;
  } | null>(null);
  const timelineRef = useRef<HTMLDivElement>(null);

  // Мемоизация фильтрации событий
  const filteredEvents = useMemo(() => {
    let filtered = events;

    // Фильтр по дате
    const start = startOfDay(selectedDate).getTime();
    const end = endOfDay(selectedDate).getTime();
    filtered = filtered.filter(
      (e) => e.timestamp >= start && e.timestamp <= end
    );

    // Фильтр по типу
    if (filterType !== 'ALL') {
      filtered = filtered.filter((e) => e.type === filterType);
    }

    // Фильтр по важности
    if (filterSeverity !== 'ALL') {
      filtered = filtered.filter((e) => e.severity === filterSeverity);
    }

    // Фильтр по камере
    if (filterCameraId !== 'ALL') {
      filtered = filtered.filter((e) => e.cameraId === filterCameraId);
    }

    return filtered;
  }, [events, selectedDate, filterType, filterSeverity, filterCameraId]);

  // Мемоизация вычисления позиций событий
  const timelineEvents = useMemo((): TimelineEvent[] => {
    if (filteredEvents.length === 0) return [];

    const start = startOfDay(selectedDate).getTime();
    const hoursInView = 24 / zoom;

    return filteredEvents.map((event) => {
      const eventTime = event.timestamp;
      const hoursFromStart = (eventTime - start) / (1000 * 60 * 60);
      const position = (hoursFromStart / hoursInView) * 100;

      return {
        ...event,
        position: Math.max(0, Math.min(100, position)),
      };
    });
  }, [filteredEvents, selectedDate, zoom]);

  // Мемоизация группировки событий по часам
  const eventsByHour = useMemo(() => {
    const groups: Record<number, Event[]> = {};
    timelineEvents.forEach((event) => {
      const hour = new Date(event.timestamp).getHours();
      if (!groups[hour]) {
        groups[hour] = [];
      }
      groups[hour].push(event);
    });
    return groups;
  }, [timelineEvents]);

  const getSeverityColor = (severity: EventSeverity): string => {
    switch (severity) {
      case 'CRITICAL':
        return '#f44336';
      case 'ERROR':
        return '#ff9800';
      case 'WARNING':
        return '#ffc107';
      case 'INFO':
        return '#2196f3';
      default:
        return '#757575';
    }
  };

  const handlePreviousDay = () => {
    setSelectedDate(subDays(selectedDate, 1));
  };

  const handleNextDay = () => {
    setSelectedDate(addDays(selectedDate, 1));
  };

  const handleToday = () => {
    setSelectedDate(new Date());
  };

  const handleEventClick = useCallback((event: Event, e: React.MouseEvent) => {
    if (e.ctrlKey || e.metaKey) {
      // Множественный выбор с Ctrl/Cmd
      setSelectedEvents((prev) => {
        const newSet = new Set(prev);
        if (newSet.has(event.id)) {
          newSet.delete(event.id);
        } else {
          newSet.add(event.id);
        }
        return newSet;
      });
    } else if (e.shiftKey && selectedEvents.size > 0) {
      // Выбор диапазона с Shift
      const selectedArray = Array.from(selectedEvents);
      const lastSelected = timelineEvents.find((e) => selectedArray.includes(e.id));
      if (lastSelected) {
        const startIdx = timelineEvents.findIndex((e) => e.id === lastSelected.id);
        const endIdx = timelineEvents.findIndex((e) => e.id === event.id);
        const range = timelineEvents.slice(
          Math.min(startIdx, endIdx),
          Math.max(startIdx, endIdx) + 1
        );
        setSelectedEvents(new Set([...selectedEvents, ...range.map((e) => e.id)]));
      }
    } else {
      // Обычный клик
      setSelectedEvents(new Set([event.id]));
      onEventClick?.(event);
    }
  }, [selectedEvents, timelineEvents, onEventClick]);

  const handleContextMenu = useCallback((event: Event, e: React.MouseEvent) => {
    e.preventDefault();
    setContextMenu(
      contextMenu === null
        ? {
            mouseX: e.clientX + 2,
            mouseY: e.clientY - 6,
            event,
          }
        : null
    );
  }, [contextMenu]);

  const handleCloseContextMenu = () => {
    setContextMenu(null);
  };

  const handleSeekToEvent = () => {
    if (contextMenu?.event) {
      onEventSeek?.(contextMenu.event);
      handleCloseContextMenu();
    }
  };

  const handleExportTimeline = async () => {
    if (!timelineRef.current || !onExport) return;

    try {
      // Используем html2canvas для экспорта
      const html2canvas = (await import('html2canvas')).default;
      const canvas = await html2canvas(timelineRef.current, {
        backgroundColor: '#ffffff',
        scale: 2,
      });

      const link = document.createElement('a');
      link.download = `timeline-${format(selectedDate, 'yyyy-MM-dd')}.png`;
      link.href = canvas.toDataURL('image/png');
      link.click();
    } catch (error) {
      console.error('Error exporting timeline:', error);
      // Fallback: вызываем callback если он есть
      onExport();
    }
  };

  const handleZoomChange = useCallback((newZoom: number) => {
    setZoom(newZoom);
    // Плавная анимация изменения масштаба
    if (timelineRef.current) {
      timelineRef.current.style.transition = 'all 0.3s ease-in-out';
      setTimeout(() => {
        if (timelineRef.current) {
          timelineRef.current.style.transition = '';
        }
      }, 300);
    }
  }, []);

  return (
    <Paper sx={{ p: 2 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h6">Временная шкала событий</Typography>
        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
          {onExport && (
            <Tooltip title="Экспорт временной шкалы">
              <IconButton size="small" onClick={handleExportTimeline}>
                <ImageIcon />
              </IconButton>
            </Tooltip>
          )}
          <FormControl size="small" sx={{ minWidth: 120 }}>
            <InputLabel>Тип</InputLabel>
            <Select
              value={filterType}
              onChange={(e) => setFilterType(e.target.value as EventType | 'ALL')}
              label="Тип"
            >
              <MenuItem value="ALL">Все</MenuItem>
              <MenuItem value="MOTION_DETECTION">Движение</MenuItem>
              <MenuItem value="OBJECT_DETECTION">Объекты</MenuItem>
              <MenuItem value="FACE_DETECTION">Лица</MenuItem>
              <MenuItem value="CAMERA_OFFLINE">Камера офлайн</MenuItem>
            </Select>
          </FormControl>
          <FormControl size="small" sx={{ minWidth: 120 }}>
            <InputLabel>Важность</InputLabel>
            <Select
              value={filterSeverity}
              onChange={(e) => setFilterSeverity(e.target.value as EventSeverity | 'ALL')}
              label="Важность"
            >
              <MenuItem value="ALL">Все</MenuItem>
              <MenuItem value="CRITICAL">Критично</MenuItem>
              <MenuItem value="ERROR">Ошибка</MenuItem>
              <MenuItem value="WARNING">Предупреждение</MenuItem>
              <MenuItem value="INFO">Информация</MenuItem>
            </Select>
          </FormControl>
          {cameras.length > 0 && (
            <FormControl size="small" sx={{ minWidth: 120 }}>
              <InputLabel>Камера</InputLabel>
              <Select
                value={filterCameraId}
                onChange={(e) => setFilterCameraId(e.target.value)}
                label="Камера"
              >
                <MenuItem value="ALL">Все</MenuItem>
                {cameras.map((camera) => (
                  <MenuItem key={camera.id} value={camera.id}>
                    {camera.name}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          )}
        </Box>
      </Box>

      {/* Управление датой */}
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
          <Button size="small" onClick={handlePreviousDay}>
            ←
          </Button>
          <Button
            size="small"
            startIcon={<TodayIcon />}
            onClick={handleToday}
            variant={format(selectedDate, 'yyyy-MM-dd') === format(new Date(), 'yyyy-MM-dd') ? 'contained' : 'outlined'}
          >
            {format(selectedDate, 'd MMMM yyyy', { locale: ru })}
          </Button>
          <Button size="small" onClick={handleNextDay}>
            →
          </Button>
        </Box>
        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center', minWidth: 200 }}>
          <ZoomOutIcon fontSize="small" />
          <Slider
            value={zoom}
            min={1}
            max={4}
            step={1}
            marks={[
              { value: 1, label: '24ч' },
              { value: 2, label: '12ч' },
              { value: 4, label: '6ч' },
            ]}
            onChange={(_, value) => handleZoomChange(value as number)}
            sx={{ flexGrow: 1 }}
          />
          <ZoomInIcon fontSize="small" />
        </Box>
      </Box>

      {/* Индикатор выбранных событий */}
      {selectedEvents.size > 0 && (
        <Box sx={{ mb: 2, p: 1, bgcolor: 'primary.light', borderRadius: 1 }}>
          <Typography variant="body2">
            Выбрано событий: {selectedEvents.size}
          </Typography>
        </Box>
      )}

      {/* Временная шкала */}
      <Box
        ref={timelineRef}
        sx={{
          position: 'relative',
          height,
          border: 1,
          borderColor: 'divider',
          borderRadius: 1,
          overflow: 'hidden',
          backgroundColor: 'grey.50',
        }}
      >
        {/* Часовые метки */}
        <Box
          sx={{
            position: 'absolute',
            top: 0,
            left: 0,
            right: 0,
            height: 30,
            display: 'flex',
            borderBottom: 1,
            borderColor: 'divider',
            backgroundColor: 'grey.100',
          }}
        >
          {Array.from({ length: Math.floor(24 / zoom) + 1 }).map((_, i) => {
            const hour = i * zoom;
            const position = (i / (24 / zoom)) * 100;
            return (
              <Box
                key={i}
                sx={{
                  position: 'absolute',
                  left: `${position}%`,
                  top: 0,
                  height: '100%',
                  borderLeft: 1,
                  borderColor: 'divider',
                  px: 0.5,
                  fontSize: '0.75rem',
                }}
              >
                {hour.toString().padStart(2, '0')}:00
              </Box>
            );
          })}
        </Box>

        {/* События */}
        <Box sx={{ position: 'relative', height: '100%', pt: 4 }}>
          {timelineEvents.map((event) => {
            const isSelected = selectedEvents.has(event.id);
            return (
              <Tooltip
                key={event.id}
                title={
                  <Box>
                    <Typography variant="body2" fontWeight="bold">
                      {event.type}
                    </Typography>
                    <Typography variant="caption" display="block">
                      {format(event.timestamp, 'HH:mm:ss', { locale: ru })}
                    </Typography>
                    {event.cameraName && (
                      <Typography variant="caption" display="block">
                        Камера: {event.cameraName}
                      </Typography>
                    )}
                    {event.description && (
                      <Typography variant="caption" display="block" sx={{ mt: 0.5 }}>
                        {event.description}
                      </Typography>
                    )}
                    {onEventSeek && (
                      <Button
                        size="small"
                        startIcon={<PlayArrowIcon />}
                        onClick={(e) => {
                          e.stopPropagation();
                          onEventSeek(event);
                        }}
                        sx={{ mt: 1 }}
                      >
                        Перейти к моменту
                      </Button>
                    )}
                  </Box>
                }
                arrow
              >
                <Box
                  onClick={(e) => handleEventClick(event, e)}
                  onContextMenu={(e) => handleContextMenu(event, e)}
                  sx={{
                    position: 'absolute',
                    left: `${event.position}%`,
                    top: 10,
                    width: isSelected ? 12 : 8,
                    height: isSelected ? 12 : 8,
                    borderRadius: '50%',
                    backgroundColor: getSeverityColor(event.severity),
                    border: isSelected ? 3 : 2,
                    borderColor: isSelected ? 'primary.main' : 'white',
                    cursor: 'pointer',
                    transition: 'all 0.2s',
                    '&:hover': {
                      transform: 'scale(1.5)',
                      zIndex: 10,
                    },
                  }}
                />
              </Tooltip>
            );
          })}

          {/* Группированные события */}
          {Object.entries(eventsByHour).map(([hour, hourEvents]) => {
            if (hourEvents.length <= 1) return null;
            const position = (Number(hour) / (24 / zoom)) * 100;
            return (
              <Tooltip
                key={hour}
                title={`${hourEvents.length} событий в ${hour}:00`}
              >
                <Box
                  sx={{
                    position: 'absolute',
                    left: `${position}%`,
                    top: 25,
                    px: 1,
                    py: 0.5,
                    borderRadius: 1,
                    backgroundColor: 'primary.main',
                    color: 'white',
                    fontSize: '0.75rem',
                    cursor: 'pointer',
                  }}
                >
                  {hourEvents.length}
                </Box>
              </Tooltip>
            );
          })}
        </Box>
      </Box>

      {/* Контекстное меню */}
      <Menu
        open={contextMenu !== null}
        onClose={handleCloseContextMenu}
        anchorReference="anchorPosition"
        anchorPosition={
          contextMenu !== null
            ? { top: contextMenu.mouseY, left: contextMenu.mouseX }
            : undefined
        }
      >
        {contextMenu?.event && (
          <>
            <MenuItem onClick={() => {
              const selectedEvent = contextMenu.event;
              if (selectedEvent) {
                onEventClick?.(selectedEvent);
              }
              handleCloseContextMenu();
            }}>
              <ListItemIcon>
                <FilterListIcon fontSize="small" />
              </ListItemIcon>
              <ListItemText>Просмотр деталей</ListItemText>
            </MenuItem>
            {onEventSeek && (
              <MenuItem onClick={handleSeekToEvent}>
                <ListItemIcon>
                  <PlayArrowIcon fontSize="small" />
                </ListItemIcon>
                <ListItemText>Перейти к моменту</ListItemText>
              </MenuItem>
            )}
            <Divider />
            <MenuItem onClick={handleCloseContextMenu}>
              <ListItemText>Закрыть</ListItemText>
            </MenuItem>
          </>
        )}
      </Menu>

      {/* Легенда */}
      <Box sx={{ display: 'flex', gap: 2, mt: 2, flexWrap: 'wrap' }}>
        <Chip
          label="Критично"
          size="small"
          sx={{ backgroundColor: '#f44336', color: 'white' }}
        />
        <Chip
          label="Ошибка"
          size="small"
          sx={{ backgroundColor: '#ff9800', color: 'white' }}
        />
        <Chip
          label="Предупреждение"
          size="small"
          sx={{ backgroundColor: '#ffc107', color: 'white' }}
        />
        <Chip
          label="Информация"
          size="small"
          sx={{ backgroundColor: '#2196f3', color: 'white' }}
        />
      </Box>

      <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
        Всего событий: {filteredEvents.length} | Выбрано: {selectedEvents.size}
      </Typography>
    </Paper>
  );
}
