'use client';

import React, { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Chip,
  IconButton,
  Tooltip,
  CircularProgress,
} from '@mui/material';
import {
  Download as DownloadIcon,
  Visibility as ViewIcon,
} from '@mui/icons-material';
import Layout from '@/components/Layout/Layout';
import ProtectedRoute from '@/components/ProtectedRoute/ProtectedRoute';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { fetchReports, exportReport } from '@/store/slices/reportsSlice';
import type { Report } from '@/types';

function ReportsPageContent() {
  const dispatch = useAppDispatch();
  const { reports, loading, error } = useAppSelector((state) => state.reports);
  const [selectedReport, setSelectedReport] = useState<Report | null>(null);

  useEffect(() => {
    dispatch(fetchReports({}));
  }, [dispatch]);

  const handleExport = async (format: 'pdf' | 'csv' | 'xlsx') => {
    try {
      await dispatch(exportReport({ format }));
    } catch (err) {
      console.error('Export failed:', err);
    }
  };

  const handleViewReport = (report: Report) => {
    setSelectedReport(report);
    // TODO: Open modal or navigate to detailed view
  };

  const getStatusColor = (status: string) => {
    switch (status) {
      case 'COMPLETED':
        return 'success';
      case 'PROCESSING':
        return 'warning';
      case 'FAILED':
        return 'error';
      default:
        return 'default';
    }
  };

  if (loading && reports.length === 0) {
    return (
      <Layout>
        <Box sx={{ display: 'flex', justifyContent: 'center', alignItems: 'center', minHeight: '400px' }}>
          <CircularProgress />
        </Box>
      </Layout>
    );
  }

  return (
    <Layout>
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" gutterBottom>
          Отчёты
        </Typography>
        <Box sx={{ display: 'flex', gap: 2, mt: 2 }}>
          <Tooltip title="Экспорт в PDF">
            <IconButton onClick={() => handleExport('pdf')} color="primary">
              <DownloadIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="Экспорт в CSV">
            <IconButton onClick={() => handleExport('csv')} color="primary">
              <DownloadIcon />
            </IconButton>
          </Tooltip>
          <Tooltip title="Экспорт в Excel">
            <IconButton onClick={() => handleExport('xlsx')} color="primary">
              <DownloadIcon />
            </IconButton>
          </Tooltip>
        </Box>
      </Box>

      {error && (
        <Typography color="error" sx={{ mb: 2 }}>
          {error}
        </Typography>
      )}

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>ID</TableCell>
              <TableCell>Тип</TableCell>
              <TableCell>Камера</TableCell>
              <TableCell>Период</TableCell>
              <TableCell>Статус</TableCell>
              <TableCell>Дата создания</TableCell>
              <TableCell>Действия</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {reports.length === 0 ? (
              <TableRow>
                <TableCell colSpan={7} align="center">
                  <Typography variant="body2" color="text.secondary">
                    Отчёты отсутствуют
                  </Typography>
                </TableCell>
              </TableRow>
            ) : (
              reports.map((report) => (
                <TableRow key={report.id}>
                  <TableCell>{report.id}</TableCell>
                  <TableCell>{report.type}</TableCell>
                  <TableCell>{report.cameraName || report.cameraId}</TableCell>
                  <TableCell>
                    {new Date(report.period.start).toLocaleDateString()} -{' '}
                    {new Date(report.period.end).toLocaleDateString()}
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={report.status}
                      color={getStatusColor(report.status) as any}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    {new Date(report.createdAt).toLocaleString('ru-RU')}
                  </TableCell>
                  <TableCell>
                    <Tooltip title="Просмотр">
                      <IconButton
                        size="small"
                        onClick={() => handleViewReport(report)}
                        color="primary"
                      >
                        <ViewIcon />
                      </IconButton>
                    </Tooltip>
                  </TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </Layout>
  );
}

export default function ReportsPage() {
  return (
    <ProtectedRoute>
      <ReportsPageContent />
    </ProtectedRoute>
  );
}
