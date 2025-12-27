'use client';

import React, { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  Paper,
  Grid,
  Card,
  CardContent,
  CardActions,
  Button,
  CircularProgress,
  Alert,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  IconButton,
  Tooltip,
} from '@mui/material';
import {
  PlayArrow as PlayArrowIcon,
  Download as DownloadIcon,
  Delete as DeleteIcon,
  FilterList as FilterListIcon,
  Schedule as ScheduleIcon,
  Videocam as VideocamIcon,
} from '@mui/icons-material';
import Layout from '@/components/Layout/Layout';
import ProtectedRoute from '@/components/ProtectedRoute/ProtectedRoute';
import { useAppSelector, useAppDispatch } from '@/store/hooks';
import {
  fetchRecordings,
  deleteRecording,
  getDownloadUrl,
  exportRecording,
  setFilters,
  clearError,
} from '@/store/slices/recordingsSlice';
import { fetchCameras } from '@/store/slices/camerasSlice';
import { useSnackbar } from 'notistack';

function RecordingsContent() {
  const dispatch = useAppDispatch();
  const { enqueueSnackbar } = useSnackbar();
  const { recordings, loading, error, pagination } = useAppSelector((state) => state.recordings);
  const { cameras } = useAppSelector((state) => state.cameras);
  const [filterDialogOpen, setFilterDialogOpen] = useState(false);
  const [filters, setLocalFilters] = useState({
    cameraId: '',
    startTime: '',
    endTime: '',
  });

  useEffect(() => {
    dispatch(fetchCameras());
    dispatch(fetchRecordings());
  }, [dispatch]);

  const handleFilter = () => {
    const filterParams: any = {};
    if (filters.cameraId) filterParams.cameraId = filters.cameraId;
    if (filters.startTime) {
      const startDate = new Date(filters.startTime).getTime();
      filterParams.startTime = startDate;
    }
    if (filters.endTime) {
      const endDate = new Date(filters.endTime).getTime();
      filterParams.endTime = endDate;
    }

    dispatch(setFilters(filterParams));
    dispatch(fetchRecordings(filterParams));
    setFilterDialogOpen(false);
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Вы уверены, что хотите удалить эту запись?')) return;
    try {
      await dispatch(deleteRecording(id)).unwrap();
      enqueueSnackbar('Запись удалена', { variant: 'success' });
    } catch (error: any) {
      enqueueSnackbar(error || 'Ошибка при удалении записи', { variant: 'error' });
    }
  };

  const handleDownload = async (id: string) => {
    try {
      const result = await dispatch(getDownloadUrl(id)).unwrap();
      if (result.url) {
        window.open(result.url, '_blank');
        enqueueSnackbar('Начата загрузка записи', { variant: 'success' });
      }
    } catch (error: any) {
      enqueueSnackbar(error || 'Ошибка при получении ссылки на скачивание', { variant: 'error' });
    }
  };

  const handleExport = async (id: string) => {
    try {
      const result = await dispatch(exportRecording({ id, format: 'mp4', quality: 'medium' })).unwrap();
      if (result.url) {
        window.open(result.url, '_blank');
        enqueueSnackbar('Запись экспортирована', { variant: 'success' });
      }
    } catch (error: any) {
      enqueueSnackbar(error || 'Ошибка при экспорте записи', { variant: 'error' });
    }
  };

  const formatDate = (timestamp: number) => {
    return new Date(timestamp).toLocaleString('ru-RU');
  };

  const formatDuration = (duration: number) => {
    const hours = Math.floor(duration / 3600);
    const minutes = Math.floor((duration % 3600) / 60);
    const seconds = duration % 60;
    return `${hours}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}`;
  };

  const formatFileSize = (bytes?: number) => {
    if (!bytes) return 'Неизвестно';
    const mb = bytes / (1024 * 1024);
    return `${mb.toFixed(2)} МБ`;
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return 'success';
      case 'RECORDING':
        return 'info';
      case 'FAILED':
        return 'error';
      case 'PENDING':
        return 'warning';
      default:
        return 'default';
    }
  };

  const getStatusLabel = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return 'Завершена';
      case 'RECORDING':
        return 'Идет запись';
      case 'FAILED':
        return 'Ошибка';
      case 'PENDING':
        return 'Ожидание';
      default:
        return status;
    }
  };

  return (
    <Layout>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Записи</Typography>
        <Button
          variant="outlined"
          startIcon={<FilterListIcon />}
          onClick={() => setFilterDialogOpen(true)}
        >
          Фильтры
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
      ) : recordings.length === 0 ? (
        <Paper sx={{ p: 3, mt: 2 }}>
          <Typography variant="body1" color="text.secondary" align="center">
            Записи не найдены
          </Typography>
        </Paper>
      ) : (
        <Grid container spacing={3}>
          {recordings.map((recording) => (
            <Grid item xs={12} sm={6} md={4} key={recording.id}>
              <Card>
                {recording.thumbnailUrl && (
                  <Box
                    sx={{
                      width: '100%',
                      height: 200,
                      backgroundImage: `url(${recording.thumbnailUrl})`,
                      backgroundSize: 'cover',
                      backgroundPosition: 'center',
                    }}
                  />
                )}
                <CardContent>
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', mb: 1 }}>
                    <Typography variant="h6" component="div" noWrap>
                      {recording.cameraName || recording.cameraId}
                    </Typography>
                    <Chip
                      label={getStatusLabel(recording.status)}
                      color={getStatusColor(recording.status) as any}
                      size="small"
                    />
                  </Box>
                  <Typography variant="body2" color="text.secondary" gutterBottom>
                    <ScheduleIcon sx={{ fontSize: 16, verticalAlign: 'middle', mr: 0.5 }} />
                    {formatDate(recording.startTime)}
                  </Typography>
                  <Box sx={{ mt: 1, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
                    <Chip
                      label={`Длительность: ${formatDuration(recording.duration)}`}
                      size="small"
                      variant="outlined"
                    />
                    <Chip
                      label={`Размер: ${formatFileSize(recording.fileSize)}`}
                      size="small"
                      variant="outlined"
                    />
                    <Chip label={recording.format} size="small" variant="outlined" />
                  </Box>
                </CardContent>
                <CardActions>
                  <Tooltip title="Воспроизвести">
                    <IconButton size="small" color="primary">
                      <PlayArrowIcon />
                    </IconButton>
                  </Tooltip>
                  <Tooltip title="Скачать">
                    <IconButton
                      size="small"
                      onClick={() => handleDownload(recording.id)}
                      color="primary"
                    >
                      <DownloadIcon />
                    </IconButton>
                  </Tooltip>
                  <Tooltip title="Экспортировать">
                    <IconButton
                      size="small"
                      onClick={() => handleExport(recording.id)}
                      color="primary"
                    >
                      <VideocamIcon />
                    </IconButton>
                  </Tooltip>
                  <Box sx={{ flexGrow: 1 }} />
                  <Tooltip title="Удалить">
                    <IconButton
                      size="small"
                      onClick={() => handleDelete(recording.id)}
                      color="error"
                    >
                      <DeleteIcon />
                    </IconButton>
                  </Tooltip>
                </CardActions>
              </Card>
            </Grid>
          ))}
        </Grid>
      )}

      {pagination.hasMore && (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 3 }}>
          <Button
            variant="outlined"
            onClick={() =>
              dispatch(
                fetchRecordings({
                  ...filters,
                  page: pagination.page + 1,
                  limit: pagination.limit,
                })
              )
            }
          >
            Загрузить еще
          </Button>
        </Box>
      )}

      <Dialog open={filterDialogOpen} onClose={() => setFilterDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Фильтры записей</DialogTitle>
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
          <TextField
            fullWidth
            label="Начало периода"
            type="datetime-local"
            value={filters.startTime}
            onChange={(e) => setLocalFilters({ ...filters, startTime: e.target.value })}
            InputLabelProps={{
              shrink: true,
            }}
            sx={{ mb: 2 }}
          />
          <TextField
            fullWidth
            label="Конец периода"
            type="datetime-local"
            value={filters.endTime}
            onChange={(e) => setLocalFilters({ ...filters, endTime: e.target.value })}
            InputLabelProps={{
              shrink: true,
            }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setFilterDialogOpen(false)}>Отмена</Button>
          <Button onClick={handleFilter} variant="contained">
            Применить
          </Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
}

export default function RecordingsPage() {
  return (
    <ProtectedRoute>
      <RecordingsContent />
    </ProtectedRoute>
  );
}
