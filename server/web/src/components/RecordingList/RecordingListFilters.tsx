'use client';

import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Chip,
} from '@mui/material';
import type { RecordingListFilters } from './types';
import type { Camera } from '@/types';

interface RecordingListFiltersProps {
  open: boolean;
  onClose: () => void;
  onApply: (filters: RecordingListFilters) => void;
  filters: RecordingListFilters;
  cameras?: Camera[];
}

export default function RecordingListFiltersComponent({
  open,
  onClose,
  onApply,
  filters,
  cameras = [],
}: RecordingListFiltersProps) {
  const [localFilters, setLocalFilters] = useState<{
    cameraId: string;
    startTime: string;
    endTime: string;
    status: string;
  }>({
    cameraId: filters.cameraId || '',
    startTime: filters.startTime
      ? new Date(filters.startTime).toISOString().slice(0, 16)
      : '',
    endTime: filters.endTime ? new Date(filters.endTime).toISOString().slice(0, 16) : '',
    status: filters.status || '',
  });

  useEffect(() => {
    setLocalFilters({
      cameraId: filters.cameraId || '',
      startTime: filters.startTime
        ? new Date(filters.startTime).toISOString().slice(0, 16)
        : '',
      endTime: filters.endTime ? new Date(filters.endTime).toISOString().slice(0, 16) : '',
      status: filters.status || '',
    });
  }, [filters]);

  const handleApply = () => {
    const filterParams: RecordingListFilters = {};
    if (localFilters.cameraId) filterParams.cameraId = localFilters.cameraId;
    if (localFilters.startTime) {
      filterParams.startTime = new Date(localFilters.startTime).getTime();
    }
    if (localFilters.endTime) {
      filterParams.endTime = new Date(localFilters.endTime).getTime();
    }
    if (localFilters.status) filterParams.status = localFilters.status;

    onApply(filterParams);
    onClose();
  };

  const handleClear = () => {
    setLocalFilters({
      cameraId: '',
      startTime: '',
      endTime: '',
      status: '',
    });
  };

  const hasActiveFilters = Boolean(
    localFilters.cameraId || localFilters.startTime || localFilters.endTime || localFilters.status
  );

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle>
        Фильтры записей
        {hasActiveFilters && (
          <Chip
            label="Активные фильтры"
            size="small"
            color="primary"
            sx={{ ml: 2 }}
          />
        )}
      </DialogTitle>
      <DialogContent>
        <FormControl fullWidth sx={{ mt: 2, mb: 2 }}>
          <InputLabel>Камера</InputLabel>
          <Select
            value={localFilters.cameraId}
            onChange={(e) => setLocalFilters({ ...localFilters, cameraId: e.target.value })}
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
          <InputLabel>Статус</InputLabel>
          <Select
            value={localFilters.status}
            onChange={(e) => setLocalFilters({ ...localFilters, status: e.target.value })}
            label="Статус"
          >
            <MenuItem value="">Все</MenuItem>
            <MenuItem value="COMPLETED">Завершена</MenuItem>
            <MenuItem value="RECORDING">Идет запись</MenuItem>
            <MenuItem value="FAILED">Ошибка</MenuItem>
            <MenuItem value="PENDING">Ожидание</MenuItem>
          </Select>
        </FormControl>

        <TextField
          fullWidth
          label="Начало периода"
          type="datetime-local"
          value={localFilters.startTime}
          onChange={(e) => setLocalFilters({ ...localFilters, startTime: e.target.value })}
          InputLabelProps={{
            shrink: true,
          }}
          sx={{ mb: 2 }}
        />
        <TextField
          fullWidth
          label="Конец периода"
          type="datetime-local"
          value={localFilters.endTime}
          onChange={(e) => setLocalFilters({ ...localFilters, endTime: e.target.value })}
          InputLabelProps={{
            shrink: true,
          }}
        />
      </DialogContent>
      <DialogActions>
        <Button onClick={handleClear} disabled={!hasActiveFilters}>
          Очистить
        </Button>
        <Button onClick={onClose}>Отмена</Button>
        <Button onClick={handleApply} variant="contained">
          Применить
        </Button>
      </DialogActions>
    </Dialog>
  );
}
