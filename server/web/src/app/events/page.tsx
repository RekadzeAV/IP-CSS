'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import {
  Box,
  Typography,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  IconButton,
  Button,
  CircularProgress,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  Checkbox,
  Tooltip,
} from '@mui/material';
import {
  CheckCircle as CheckCircleIcon,
  Delete as DeleteIcon,
  FilterList as FilterListIcon,
  Timeline as TimelineIcon,
  FileDownload as FileDownloadIcon,
} from '@mui/icons-material';
import Layout from '@/components/Layout/Layout';
import ProtectedRoute from '@/components/ProtectedRoute/ProtectedRoute';
import EventTimeline from '@/components/EventTimeline/EventTimeline';
import Pagination from '@/components/Pagination/Pagination';
import { useAppSelector, useAppDispatch } from '@/store/hooks';
import {
  fetchEvents,
  deleteEvent,
  acknowledgeEvent,
  acknowledgeEvents,
  fetchStatistics,
  setFilters,
  clearFilters,
  clearError,
} from '@/store/slices/eventsSlice';
import { fetchCameras } from '@/store/slices/camerasSlice';
import { useSnackbar } from 'notistack';
import { exportEvents } from '@/utils/export';
import type { EventType, EventSeverity } from '@/types';

function EventsContent() {
  const router = useRouter();
  const dispatch = useAppDispatch();
  const { enqueueSnackbar } = useSnackbar();
  const { events, loading, error, pagination, statistics, filters: reduxFilters } = useAppSelector(
    (state) => state.events
  );
  const { cameras } = useAppSelector((state) => state.cameras);
  const [filterDialogOpen, setFilterDialogOpen] = useState(false);
  const [selectedEvents, setSelectedEvents] = useState<string[]>([]);
  const [filters, setLocalFilters] = useState({
    cameraId: '',
    type: '' as EventType | '',
    severity: '' as EventSeverity | '',
    acknowledged: undefined as boolean | undefined,
    source: '' as 'ONVIF' | '' | 'OTHER', // Фильтр по источнику
  });
  const [showTimeline, setShowTimeline] = useState(false);
  type EventFiltersRequest = {
    cameraId?: string;
    type?: EventType;
    severity?: EventSeverity;
    acknowledged?: boolean;
    page?: number;
    limit?: number;
  };

  const toEventFilterParams = (
    currentFilters: typeof filters,
    paging?: Pick<EventFiltersRequest, 'page' | 'limit'>
  ): EventFiltersRequest => {
    const result: EventFiltersRequest = {};
    if (currentFilters.cameraId) result.cameraId = currentFilters.cameraId;
    if (currentFilters.type) result.type = currentFilters.type;
    if (currentFilters.severity) result.severity = currentFilters.severity;
    if (currentFilters.acknowledged !== undefined) result.acknowledged = currentFilters.acknowledged;
    if (paging?.page !== undefined) result.page = paging.page;
    if (paging?.limit !== undefined) result.limit = paging.limit;
    return result;
  };
  // Синхронизация локальных фильтров с Redux при открытии диалога (чтобы отображались применённые значения)
  useEffect(() => {
    if (filterDialogOpen && reduxFilters) {
      setLocalFilters((prev) => ({
        ...prev,
        cameraId: (reduxFilters.cameraId as string) ?? '',
        type: (reduxFilters.type as EventType) ?? '',
        severity: (reduxFilters.severity as EventSeverity) ?? '',
        acknowledged: reduxFilters.acknowledged,
      }));
    }
  }, [filterDialogOpen, reduxFilters]);

  const hasActiveFilters =
    Boolean(reduxFilters?.cameraId || reduxFilters?.type || reduxFilters?.severity || reduxFilters?.acknowledged !== undefined) ||
    Boolean(filters.source);

  useEffect(() => {
    dispatch(fetchCameras());
    dispatch(fetchEvents());
    dispatch(fetchStatistics({}));
  }, [dispatch]);

  // Уведомления о критических событиях (только для новых неподтвержденных)
  const [notifiedEvents, setNotifiedEvents] = useState<Set<string>>(new Set());

  useEffect(() => {
    const criticalEvents = events.filter(
      (event) =>
        event.severity === 'CRITICAL' &&
        !event.acknowledged &&
        !notifiedEvents.has(event.id)
    );

    if (criticalEvents.length > 0) {
      criticalEvents.forEach((event) => {
        const isOnvif = isOnvifEvent(event);
        const sourceLabel = isOnvif ? 'ONVIF' : 'Система';
        enqueueSnackbar(
          `Критическое событие (${sourceLabel}): ${event.description || event.type}`,
          {
            variant: 'error',
            autoHideDuration: 10000,
            anchorOrigin: { vertical: 'top', horizontal: 'right' },
          }
        );
        setNotifiedEvents((prev) => new Set([...prev, event.id]));
      });
    }
  }, [events, enqueueSnackbar, notifiedEvents]);

  const handleFilter = () => {
    const filterParams = toEventFilterParams(filters);
    // Фильтр по источнику обрабатывается на клиенте

    dispatch(setFilters(filterParams));
    dispatch(fetchEvents(filterParams));
    setFilterDialogOpen(false);
  };

  const handleClearFilters = () => {
    setLocalFilters({
      cameraId: '',
      type: '',
      severity: '',
      acknowledged: undefined,
      source: '',
    });
    dispatch(clearFilters());
    dispatch(fetchEvents({}));
  };

  // Фильтрация событий по источнику на клиенте
  const filteredEvents = filters.source
    ? events.filter((event) => {
        const isOnvif = isOnvifEvent(event);
        if (filters.source === 'ONVIF') return isOnvif;
        if (filters.source === 'OTHER') return !isOnvif;
        return true;
      })
    : events;

  // Проверка, является ли событие ONVIF событием
  const isOnvifEvent = (event: { metadata?: Record<string, unknown> }): boolean => {
    return event.metadata?.onvif_topic != null || event.metadata?.source === 'ONVIF';
  };

  const handleDelete = async (id: string) => {
    try {
      await dispatch(deleteEvent(id)).unwrap();
      enqueueSnackbar('Событие удалено', { variant: 'success' });
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Ошибка при удалении события';
      enqueueSnackbar(message, { variant: 'error' });
    }
  };

  const handleAcknowledge = async (id: string) => {
    try {
      await dispatch(acknowledgeEvent(id)).unwrap();
      enqueueSnackbar('Событие подтверждено', { variant: 'success' });
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Ошибка при подтверждении события';
      enqueueSnackbar(message, { variant: 'error' });
    }
  };

  const handleBulkAcknowledge = async () => {
    if (selectedEvents.length === 0) return;
    try {
      await dispatch(acknowledgeEvents(selectedEvents)).unwrap();
      enqueueSnackbar(`Подтверждено событий: ${selectedEvents.length}`, { variant: 'success' });
      setSelectedEvents([]);
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Ошибка при подтверждении событий';
      enqueueSnackbar(message, { variant: 'error' });
    }
  };

  const getSeverityColor = (severity: EventSeverity) => {
    switch (severity) {
      case 'CRITICAL':
        return 'error';
      case 'ERROR':
        return 'error';
      case 'WARNING':
        return 'warning';
      case 'INFO':
        return 'info';
      default:
        return 'default';
    }
  };

  const formatDate = (timestamp: number) => {
    return new Date(timestamp).toLocaleString('ru-RU');
  };

  return (
    <Layout>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">События</Typography>
        <Box>
          <Button
            variant={showTimeline ? 'contained' : 'outlined'}
            startIcon={<TimelineIcon />}
            onClick={() => setShowTimeline(!showTimeline)}
            sx={{ mr: 1 }}
          >
            {showTimeline ? 'Скрыть шкалу' : 'Показать шкалу'}
          </Button>
          <Button
            variant="outlined"
            startIcon={<FileDownloadIcon />}
            onClick={() => {
              exportEvents(events, 'csv');
              enqueueSnackbar('События экспортированы', { variant: 'success' });
            }}
            sx={{ mr: 1 }}
          >
            Экспорт CSV
          </Button>
          <Button
            variant="outlined"
            startIcon={<FilterListIcon />}
            onClick={() => setFilterDialogOpen(true)}
            color={hasActiveFilters ? 'primary' : 'inherit'}
            sx={{ mr: 1 }}
          >
            Фильтры{hasActiveFilters ? ' (активны)' : ''}
          </Button>
          {hasActiveFilters && (
            <Button variant="outlined" onClick={handleClearFilters} sx={{ mr: 1 }}>
              Сбросить фильтры
            </Button>
          )}
          {selectedEvents.length > 0 && (
            <Button
              variant="contained"
              startIcon={<CheckCircleIcon />}
              onClick={handleBulkAcknowledge}
            >
              Подтвердить выбранные ({selectedEvents.length})
            </Button>
          )}
        </Box>
      </Box>

      {showTimeline && (
        <Box sx={{ mb: 3 }}>
          <EventTimeline
            events={events}
            cameras={cameras}
            onEventClick={(event) => router.push(`/events/${event.id}`)}
            onEventSeek={(event) => {
              // TODO: Интеграция с видеоплеером для перехода к моменту события
              console.log('Seek to event:', event);
              enqueueSnackbar('Функция перехода к моменту события будет реализована', { variant: 'info' });
            }}
          />
        </Box>
      )}

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => dispatch(clearError())}>
          {error}
        </Alert>
      )}

      {statistics && (
        <Box sx={{ mb: 3, display: 'flex', gap: 2 }}>
          <Paper sx={{ p: 2, flex: 1 }}>
            <Typography variant="body2" color="text.secondary">
              Всего событий
            </Typography>
            <Typography variant="h5">{statistics.total}</Typography>
          </Paper>
          <Paper sx={{ p: 2, flex: 1 }}>
            <Typography variant="body2" color="text.secondary">
              Неподтвержденных
            </Typography>
            <Typography variant="h5">{statistics.unacknowledged}</Typography>
          </Paper>
        </Box>
      )}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
          <CircularProgress />
        </Box>
      ) : events.length === 0 ? (
        <Paper sx={{ p: 3, mt: 2 }}>
          <Typography variant="body1" color="text.secondary" align="center">
            События не найдены
          </Typography>
        </Paper>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell padding="checkbox">
                  <Checkbox
                    indeterminate={selectedEvents.length > 0 && selectedEvents.length < events.length}
                    checked={filteredEvents.length > 0 && selectedEvents.length === filteredEvents.length}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setSelectedEvents(filteredEvents.map((e) => e.id));
                      } else {
                        setSelectedEvents([]);
                      }
                    }}
                  />
                </TableCell>
                <TableCell>Время</TableCell>
                <TableCell>Камера</TableCell>
                <TableCell>Тип</TableCell>
                <TableCell>Источник</TableCell>
                <TableCell>Уровень</TableCell>
                <TableCell>Описание</TableCell>
                <TableCell>Статус</TableCell>
                <TableCell>Действия</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {filteredEvents.map((event) => (
                <TableRow
                  key={event.id}
                  hover
                  sx={{ cursor: 'pointer' }}
                  onClick={() => router.push(`/events/${event.id}`)}
                >
                  <TableCell padding="checkbox">
                    <Checkbox
                      checked={selectedEvents.includes(event.id)}
                      onChange={(e) => {
                        if (e.target.checked) {
                          setSelectedEvents([...selectedEvents, event.id]);
                        } else {
                          setSelectedEvents(selectedEvents.filter((id) => id !== event.id));
                        }
                      }}
                    />
                  </TableCell>
                  <TableCell>{formatDate(event.timestamp)}</TableCell>
                  <TableCell>{event.cameraName || event.cameraId}</TableCell>
                  <TableCell>{event.type}</TableCell>
                  <TableCell>
                    {isOnvifEvent(event) ? (
                      <Chip
                        label="ONVIF"
                        color="primary"
                        size="small"
                        sx={{ fontWeight: 'bold' }}
                      />
                    ) : (
                      <Chip
                        label="Система"
                        color="default"
                        size="small"
                      />
                    )}
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={event.severity}
                      color={getSeverityColor(event.severity)}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>{event.description || '-'}</TableCell>
                  <TableCell>
                    {event.acknowledged ? (
                      <Chip label="Подтверждено" color="success" size="small" />
                    ) : (
                      <Chip label="Неподтверждено" color="warning" size="small" />
                    )}
                  </TableCell>
                  <TableCell onClick={(e) => e.stopPropagation()}>
                    <Tooltip title="Подтвердить">
                      <IconButton
                        size="small"
                        onClick={() => handleAcknowledge(event.id)}
                        disabled={event.acknowledged}
                        color="success"
                      >
                        <CheckCircleIcon />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Удалить">
                      <IconButton
                        size="small"
                        onClick={() => handleDelete(event.id)}
                        color="error"
                      >
                        <DeleteIcon />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {pagination && (
        <Pagination
          page={pagination.page}
          limit={pagination.limit}
          total={pagination.total || events.length}
          onPageChange={(page) =>
            dispatch(
              fetchEvents(toEventFilterParams(filters, { page, limit: pagination.limit }))
            )
          }
          onLimitChange={(limit) =>
            dispatch(
              fetchEvents(toEventFilterParams(filters, { page: 1, limit }))
            )
          }
        />
      )}

      <Dialog open={filterDialogOpen} onClose={() => setFilterDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Фильтры событий</DialogTitle>
        <DialogContent>
          <FormControl fullWidth sx={{ mt: 2, mb: 2 }}>
            <InputLabel>Камера</InputLabel>
            <Select
              value={filters.cameraId}
              onChange={(e) => setLocalFilters({ ...filters, cameraId: e.target.value })}
              label="Камера"
            >
              <MenuItem value="">Все</MenuItem>
              {cameras.map((camera) => (
                <MenuItem key={camera.id} value={camera.id}>
                  {camera.name}
                </MenuItem>
              ))}
            </Select>
          </FormControl>
          <FormControl fullWidth sx={{ mb: 2 }}>
            <InputLabel>Тип события</InputLabel>
            <Select
              value={filters.type}
              onChange={(e) => setLocalFilters({ ...filters, type: e.target.value as EventType | '' })}
              label="Тип события"
            >
              <MenuItem value="">Все</MenuItem>
              <MenuItem value="MOTION_DETECTION">Детекция движения</MenuItem>
              <MenuItem value="OBJECT_DETECTION">Детекция объектов</MenuItem>
              <MenuItem value="FACE_DETECTION">Детекция лиц</MenuItem>
              <MenuItem value="LICENSE_PLATE_RECOGNITION">Распознавание номеров</MenuItem>
              <MenuItem value="CAMERA_OFFLINE">Камера офлайн</MenuItem>
              <MenuItem value="CAMERA_ONLINE">Камера онлайн</MenuItem>
              <MenuItem value="RECORDING_STARTED">Запись начата</MenuItem>
              <MenuItem value="RECORDING_STOPPED">Запись остановлена</MenuItem>
              <MenuItem value="SYSTEM_ERROR">Ошибка системы</MenuItem>
            </Select>
          </FormControl>
          <FormControl fullWidth sx={{ mb: 2 }}>
            <InputLabel>Уровень важности</InputLabel>
            <Select
              value={filters.severity}
              onChange={(e) => setLocalFilters({ ...filters, severity: e.target.value as EventSeverity | '' })}
              label="Уровень важности"
            >
              <MenuItem value="">Все</MenuItem>
              <MenuItem value="CRITICAL">Критический</MenuItem>
              <MenuItem value="ERROR">Ошибка</MenuItem>
              <MenuItem value="WARNING">Предупреждение</MenuItem>
              <MenuItem value="INFO">Информация</MenuItem>
            </Select>
          </FormControl>
          <FormControl fullWidth sx={{ mb: 2 }}>
            <InputLabel>Источник</InputLabel>
            <Select
              value={filters.source}
              onChange={(e) => setLocalFilters({ ...filters, source: e.target.value as 'ONVIF' | '' | 'OTHER' })}
              label="Источник"
            >
              <MenuItem value="">Все</MenuItem>
              <MenuItem value="ONVIF">ONVIF</MenuItem>
              <MenuItem value="OTHER">Система</MenuItem>
            </Select>
          </FormControl>
          <FormControl fullWidth>
            <InputLabel>Статус подтверждения</InputLabel>
            <Select
              value={filters.acknowledged === undefined ? '' : String(filters.acknowledged)}
              onChange={(e) =>
                setLocalFilters({
                  ...filters,
                  acknowledged: e.target.value === '' ? undefined : e.target.value === 'true',
                })
              }
              label="Статус подтверждения"
            >
              <MenuItem value="">Все</MenuItem>
              <MenuItem value="false">Неподтвержденные</MenuItem>
              <MenuItem value="true">Подтвержденные</MenuItem>
            </Select>
          </FormControl>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setFilterDialogOpen(false)}>Отмена</Button>
          {hasActiveFilters && (
            <Button onClick={handleClearFilters} color="inherit">
              Сбросить
            </Button>
          )}
          <Button onClick={handleFilter} variant="contained">
            Применить
          </Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
}

export default function EventsPage() {
  return (
    <ProtectedRoute>
      <EventsContent />
    </ProtectedRoute>
  );
}
