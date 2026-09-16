'use client';

import React, { useMemo, useState } from 'react';
import { useDebounce } from '@/hooks/useDebounce';
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
  Download as DownloadIcon,
  Image as ImageIcon,
} from '@mui/icons-material';
import { format, startOfDay, subDays, addDays } from 'date-fns';
import { ru } from 'date-fns/locale';
import type { Event, EventType, EventSeverity } from '@/types';

// Утилиты для работы с временными зонами
const getAvailableTimeZones = () => {
  return Intl.supportedValuesOf('timeZone');
};

const getTimeZoneOffset = (timeZone: string, date: Date = new Date()) => {
  const formatter = new Intl.DateTimeFormat('en-US', {
    timeZone,
    timeZoneName: 'short',
  });
  const parts = formatter.formatToParts(date);
  const timeZoneName = parts.find((part) => part.type === 'timeZoneName')?.value || '';
  return timeZoneName;
};

const formatDateInTimeZone = (date: Date, timeZone: string, formatStr: string) => {
  const formatter = new Intl.DateTimeFormat('ru-RU', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false,
  });

  if (formatStr.includes('HH:mm')) {
    const parts = formatter.formatToParts(date);
    const hour = parts.find((p) => p.type === 'hour')?.value || '00';
    const minute = parts.find((p) => p.type === 'minute')?.value || '00';
    return `${hour}:${minute}`;
  }

  return format(date, formatStr, { locale: ru });
};

interface EventTimelineProps {
  events: Event[];
  onEventClick?: (event: Event) => void;
  onEventSeek?: (event: Event) => void; // Переход к моменту события в видеоплеере
  height?: number;
  cameras?: Array<{ id: string; name: string }>;
}

interface TimelineEvent extends Event {
  position: number; // Позиция на временной шкале (0-100)
}

export default function EventTimeline({
  events,
  onEventClick,
  onEventSeek,
  height = 200,
  cameras = [],
}: EventTimelineProps) {
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [zoom, setZoom] = useState(1); // 1 = день, 2 = 12 часов, 4 = 6 часов
  const [filterType, setFilterType] = useState<EventType | 'ALL'>('ALL');
  const [filterSeverity, setFilterSeverity] = useState<EventSeverity | 'ALL'>('ALL');
  const [filterCameraId, setFilterCameraId] = useState<string>('ALL');

  // Debounced фильтры для оптимизации производительности
  const debouncedFilterType = useDebounce(filterType, 300);
  const debouncedFilterSeverity = useDebounce(filterSeverity, 300);
  const debouncedFilterCameraId = useDebounce(filterCameraId, 300);
  const [selectedEvents] = useState<Set<string>>(new Set());
  const [contextMenu, setContextMenu] = useState<{
    mouseX: number;
    mouseY: number;
    event: Event | null;
  } | null>(null);
  const [exportMenuAnchor, setExportMenuAnchor] = useState<null | HTMLElement>(null);
  const [timeZone, setTimeZone] = useState<string>(() => {
    if (typeof window !== 'undefined') {
      return Intl.DateTimeFormat().resolvedOptions().timeZone;
    }
    return 'UTC';
  });
  const timelineRef = React.useRef<HTMLDivElement>(null);

  // Получаем список доступных временных зон
  const availableTimeZones = useMemo(() => {
    try {
      return getAvailableTimeZones();
    } catch {
      // Fallback для браузеров без поддержки
      return [
        'UTC',
        'Europe/Moscow',
        'Europe/Kiev',
        'America/New_York',
        'America/Los_Angeles',
        'Asia/Tokyo',
        'Asia/Shanghai',
      ];
    }
  }, []);

  // Фильтрация событий с использованием debounced фильтров
  const filteredEvents = useMemo(() => {
    let filtered = events;

    // Фильтр по дате с учетом временной зоны
    const selectedDateStr = selectedDate.toLocaleDateString('en-CA');
    filtered = filtered.filter((e) => {
      const eventDateInTZ = new Date(e.timestamp).toLocaleDateString('en-CA', { timeZone });
      return eventDateInTZ === selectedDateStr;
    });

    // Фильтр по типу (используем debounced значение)
    if (debouncedFilterType !== 'ALL') {
      filtered = filtered.filter((e) => e.type === debouncedFilterType);
    }

    // Фильтр по важности (используем debounced значение)
    if (debouncedFilterSeverity !== 'ALL') {
      filtered = filtered.filter((e) => e.severity === debouncedFilterSeverity);
    }

    // Фильтр по камере (используем debounced значение)
    if (debouncedFilterCameraId !== 'ALL') {
      filtered = filtered.filter((e) => e.cameraId === debouncedFilterCameraId);
    }

    return filtered;
  }, [events, selectedDate, debouncedFilterType, debouncedFilterSeverity, debouncedFilterCameraId, timeZone]);

  // Вычисление позиций событий на временной шкале
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

  // Группировка событий по часам с учетом временной зоны
  const eventsByHour = useMemo(() => {
    const groups: Record<number, Event[]> = {};
    timelineEvents.forEach((event) => {
      const eventDate = new Date(event.timestamp);
      const hour = parseInt(
        eventDate.toLocaleTimeString('en-US', {
          timeZone,
          hour12: false,
          hour: '2-digit',
        }),
        10
      );
      if (!groups[hour]) {
        groups[hour] = [];
      }
      groups[hour].push(event);
    });
    return groups;
  }, [timelineEvents, timeZone]);

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

  const handleCloseContextMenu = () => {
    setContextMenu(null);
  };

  const handleSeekToEvent = () => {
    if (contextMenu?.event) {
      onEventSeek?.(contextMenu.event);
      handleCloseContextMenu();
    }
  };

  const handleExportTimeline = async (exportFormat: 'png' | 'svg') => {
    if (!timelineRef.current) return;

    try {
      if (exportFormat === 'png') {
        const html2canvas = (await import('html2canvas')).default;
        const canvas = await html2canvas(timelineRef.current, {
          backgroundColor: '#ffffff',
          scale: 2,
          logging: false,
        });
        const link = document.createElement('a');
        link.download = `timeline-${format(selectedDate, 'yyyy-MM-dd')}.png`;
        link.href = canvas.toDataURL('image/png');
        link.click();
      } else if (exportFormat === 'svg') {
        // SVG экспорт можно реализовать через создание SVG элемента
        // Для простоты пока используем PNG
        handleExportTimeline('png');
      }
      setExportMenuAnchor(null);
    } catch (error) {
      console.error('Error exporting timeline:', error);
      setExportMenuAnchor(null);
    }
  };

  return (
    <Paper sx={{ p: 2 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h6">Временная шкала событий</Typography>
        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
          <FormControl size="small" sx={{ minWidth: 200 }}>
            <InputLabel>Временная зона</InputLabel>
            <Select
              value={timeZone}
              onChange={(e) => setTimeZone(e.target.value)}
              label="Временная зона"
            >
              {availableTimeZones.slice(0, 50).map((tz) => (
                <MenuItem key={tz} value={tz}>
                  {tz} ({getTimeZoneOffset(tz)})
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <Tooltip title="Экспорт">
            <IconButton
              size="small"
              onClick={(e) => setExportMenuAnchor(e.currentTarget)}
            >
              <DownloadIcon />
            </IconButton>
          </Tooltip>
          <Menu
            anchorEl={exportMenuAnchor}
            open={Boolean(exportMenuAnchor)}
            onClose={() => setExportMenuAnchor(null)}
          >
            <MenuItem onClick={() => handleExportTimeline('png')}>
              <ListItemIcon>
                <ImageIcon fontSize="small" />
              </ListItemIcon>
              <ListItemText>Экспорт PNG</ListItemText>
            </MenuItem>
            <MenuItem onClick={() => handleExportTimeline('svg')}>
              <ListItemIcon>
                <ImageIcon fontSize="small" />
              </ListItemIcon>
              <ListItemText>Экспорт SVG</ListItemText>
            </MenuItem>
          </Menu>
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
            {formatDateInTimeZone(selectedDate, timeZone, 'd MMMM yyyy')}
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
            onChange={(_, value) => {
              const newZoom = value as number;
              setZoom(newZoom);
              // Плавная анимация изменения масштаба
              const timelineElement = document.querySelector('[data-timeline-container]');
              if (timelineElement) {
                (timelineElement as HTMLElement).style.transition = 'all 0.3s ease-in-out';
                setTimeout(() => {
                  if (timelineElement) {
                    (timelineElement as HTMLElement).style.transition = '';
                  }
                }, 300);
              }
            }}
            sx={{ flexGrow: 1 }}
          />
          <ZoomInIcon fontSize="small" />
        </Box>
      </Box>

      {/* Временная шкала */}
      <Box
        ref={timelineRef}
        data-timeline-container
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
          {timelineEvents.map((event) => (
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
                onClick={() => onEventClick?.(event)}
                sx={{
                  position: 'absolute',
                  left: `${event.position}%`,
                  top: 10,
                  width: 8,
                  height: 8,
                  borderRadius: '50%',
                  backgroundColor: getSeverityColor(event.severity),
                  border: 2,
                  borderColor: 'white',
                  cursor: 'pointer',
                  '&:hover': {
                    transform: 'scale(1.5)',
                    zIndex: 10,
                  },
                }}
              />
            </Tooltip>
          ))}

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

      {/* Индикатор выбранных событий */}
      {selectedEvents.size > 0 && (
        <Box sx={{ mt: 2, p: 1, bgcolor: 'primary.light', borderRadius: 1 }}>
          <Typography variant="body2">
            Выбрано событий: {selectedEvents.size}
          </Typography>
        </Box>
      )}

      <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
        Всего событий: {filteredEvents.length} | Выбрано: {selectedEvents.size}
      </Typography>
    </Paper>
  );
}



