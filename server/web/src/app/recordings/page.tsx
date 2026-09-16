'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { Box, Typography, Button } from '@mui/material';
import { FileDownload as FileDownloadIcon } from '@mui/icons-material';
import Layout from '@/components/Layout/Layout';
import ProtectedRoute from '@/components/ProtectedRoute/ProtectedRoute';
import { RecordingList } from '@/components/RecordingList';
import type { RecordingListFilters } from '@/components/RecordingList/types';
import { useAppSelector, useAppDispatch } from '@/store/hooks';
import {
  fetchRecordings,
  deleteRecording,
  getDownloadUrl,
  exportRecording,
  setFilters as setRecordingsFilters,
  clearError,
} from '@/store/slices/recordingsSlice';
import { fetchCameras } from '@/store/slices/camerasSlice';
import { useSnackbar } from 'notistack';
import { exportRecordings } from '@/utils/export';

function RecordingsContent() {
  const router = useRouter();
  const dispatch = useAppDispatch();
  const { enqueueSnackbar } = useSnackbar();
  const { recordings, loading, error, pagination } = useAppSelector((state) => state.recordings);
  const { cameras } = useAppSelector((state) => state.cameras);
  const [filters, setLocalFilters] = useState<RecordingListFilters>({});

  // Автоматическое обновление при изменении записей через WebSocket
  // Записи обновляются автоматически через Redux slice

  useEffect(() => {
    dispatch(fetchCameras());
    dispatch(fetchRecordings());
  }, [dispatch]);

  const handleRecordingClick = (recordingId: string) => {
    router.push(`/recordings/${recordingId}`);
  };

  const handleDelete = async (id: string) => {
    if (!confirm('Вы уверены, что хотите удалить эту запись?')) return;
    try {
      await dispatch(deleteRecording(id)).unwrap();
      enqueueSnackbar('Запись удалена', { variant: 'success' });
      dispatch(fetchRecordings());
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Ошибка при удалении записи';
      enqueueSnackbar(message, { variant: 'error' });
    }
  };

  const handleDownload = async (id: string) => {
    try {
      const downloadUrl = await dispatch(getDownloadUrl(id)).unwrap();
      if (downloadUrl) {
        window.open(downloadUrl, '_blank');
        enqueueSnackbar('Начата загрузка записи', { variant: 'success' });
      }
    } catch (error: unknown) {
      const message =
        error instanceof Error ? error.message : 'Ошибка при получении ссылки на скачивание';
      enqueueSnackbar(message, { variant: 'error' });
    }
  };

  const handleExport = async (id: string) => {
    try {
      const exportUrl = await dispatch(exportRecording({ id, format: 'mp4', quality: 'medium' })).unwrap();
      if (exportUrl) {
        window.open(exportUrl, '_blank');
        enqueueSnackbar('Запись экспортирована', { variant: 'success' });
      }
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Ошибка при экспорте записи';
      enqueueSnackbar(message, { variant: 'error' });
    }
  };

  const handleBulkDelete = async (ids: string[]) => {
    if (!confirm(`Удалить ${ids.length} записей?`)) return;
    try {
      await Promise.all(ids.map((id) => dispatch(deleteRecording(id)).unwrap()));
      enqueueSnackbar(`Удалено записей: ${ids.length}`, { variant: 'success' });
      dispatch(fetchRecordings());
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Ошибка при удалении записей';
      enqueueSnackbar(message, { variant: 'error' });
    }
  };

  const handleBulkExport = async (ids: string[]) => {
    try {
      const results = await Promise.all(
        ids.map((id) => dispatch(exportRecording({ id, format: 'mp4', quality: 'medium' })).unwrap())
      );
      const successCount = results.filter(Boolean).length;
      enqueueSnackbar(`Экспортировано записей: ${successCount}`, { variant: 'success' });
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Ошибка при экспорте записей';
      enqueueSnackbar(message, { variant: 'error' });
    }
  };

  const handleFiltersChange = (newFilters: RecordingListFilters) => {
    setLocalFilters(newFilters);
    const filterParams: {
      cameraId?: string;
      startTime?: number;
      endTime?: number;
      status?: string;
    } = {};
    if (newFilters.cameraId) filterParams.cameraId = newFilters.cameraId;
    if (newFilters.startTime) filterParams.startTime = newFilters.startTime;
    if (newFilters.endTime) filterParams.endTime = newFilters.endTime;
    if (newFilters.status) filterParams.status = newFilters.status;

    dispatch(setRecordingsFilters(filterParams));
    dispatch(fetchRecordings(filterParams));
  };

  const handlePageChange = (page: number) => {
    const filterParams = { ...filters, page, limit: pagination?.limit || 20 };
    dispatch(fetchRecordings(filterParams));
  };

  const handleLimitChange = (limit: number) => {
    const filterParams = { ...filters, page: 1, limit };
    dispatch(fetchRecordings(filterParams));
  };

  return (
    <Layout>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Записи</Typography>
        <Button
          variant="outlined"
          startIcon={<FileDownloadIcon />}
          onClick={() => {
            exportRecordings(recordings, 'csv');
            enqueueSnackbar('Записи экспортированы', { variant: 'success' });
          }}
        >
          Экспорт CSV
        </Button>
      </Box>

      <RecordingList
        recordings={recordings}
        loading={loading}
        error={error}
        pagination={pagination}
        cameras={cameras}
        filters={filters}
        onRecordingClick={handleRecordingClick}
        onDelete={handleDelete}
        onDownload={handleDownload}
        onExport={handleExport}
        onBulkDelete={handleBulkDelete}
        onBulkExport={handleBulkExport}
        onFiltersChange={handleFiltersChange}
        onPageChange={handlePageChange}
        onLimitChange={handleLimitChange}
        onClearError={() => dispatch(clearError())}
      />
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
