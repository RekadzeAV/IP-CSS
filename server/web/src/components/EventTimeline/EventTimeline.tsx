'use client';

import React, { useMemo, useState } from 'react';
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
} from '@mui/material';
import {
  ZoomIn as ZoomInIcon,
  ZoomOut as ZoomOutIcon,
  Today as TodayIcon,
  FilterList as FilterListIcon,
} from '@mui/icons-material';
import { format, startOfDay, endOfDay, subDays, addDays } from 'date-fns';
import { ru } from 'date-fns/locale';
import type { Event, EventType, EventSeverity } from '@/types';

interface EventTimelineProps {
  events: Event[];
  onEventClick?: (event: Event) => void;
  height?: number;
}

interface TimelineEvent extends Event {
  position: number; // Позиция на временной шкале (0-100)
}

export default function EventTimeline({
  events,
  onEventClick,
  height = 200,
}: EventTimelineProps) {
  const [selectedDate, setSelectedDate] = useState(new Date());
  const [zoom, setZoom] = useState(1); // 1 = день, 2 = 12 часов, 4 = 6 часов
  const [filterType, setFilterType] = useState<EventType | 'ALL'>('ALL');
  const [filterSeverity, setFilterSeverity] = useState<EventSeverity | 'ALL'>('ALL');

  // Фильтрация событий
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

    return filtered;
  }, [events, selectedDate, filterType, filterSeverity]);

  // Вычисление позиций событий на временной шкале
  const timelineEvents = useMemo((): TimelineEvent[] => {
    if (filteredEvents.length === 0) return [];

    const start = startOfDay(selectedDate).getTime();
    const end = endOfDay(selectedDate).getTime();
    const range = end - start;
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

  // Группировка событий по часам
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

  return (
    <Paper sx={{ p: 2 }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h6">Временная шкала событий</Typography>
        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
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
            onChange={(_, value) => setZoom(value as number)}
            sx={{ flexGrow: 1 }}
          />
          <ZoomInIcon fontSize="small" />
        </Box>
      </Box>

      {/* Временная шкала */}
      <Box
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
                  <Typography variant="body2">{event.type}</Typography>
                  <Typography variant="caption">
                    {format(event.timestamp, 'HH:mm:ss', { locale: ru })}
                  </Typography>
                  {event.description && (
                    <Typography variant="caption" display="block">
                      {event.description}
                    </Typography>
                  )}
                </Box>
              }
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

      <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
        Всего событий: {filteredEvents.length}
      </Typography>
    </Paper>
  );
}

