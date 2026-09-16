'use client';

import React, { useMemo } from 'react';
import { Paper, Typography, Box } from '@mui/material';
import {
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  Legend,
  Tooltip,
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
} from 'recharts';
import type { Recording, Camera } from '@/types';
import { format } from 'date-fns';
import { ru } from 'date-fns/locale';

interface StorageChartProps {
  recordings: Recording[];
  cameras?: Camera[];
  totalStorage?: number; // Общий объем хранилища в байтах
  height?: number;
}

const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884d8', '#82ca9d'];

export default function StorageChart({
  recordings,
  totalStorage,
  height = 300,
}: StorageChartProps) {
  // Распределение по камерам
  const storageByCamera = useMemo(() => {
    const grouped: Record<string, { name: string; size: number; count: number }> = {};

    recordings.forEach((recording) => {
      const cameraName = recording.cameraName || recording.cameraId;
      if (!grouped[recording.cameraId]) {
        grouped[recording.cameraId] = { name: cameraName, size: 0, count: 0 };
      }
      grouped[recording.cameraId].size += recording.fileSize || 0;
      grouped[recording.cameraId].count++;
    });

    return Object.values(grouped)
      .map((item) => ({
        name: item.name,
        value: item.size / (1024 * 1024 * 1024), // Конвертируем в GB
        count: item.count,
      }))
      .sort((a, b) => b.value - a.value)
      .slice(0, 6); // Топ 6 камер
  }, [recordings]);

  // Использование хранилища по времени
  const storageOverTime = useMemo(() => {
    const grouped: Record<string, number> = {};
    let cumulativeSize = 0;

    recordings
      .sort((a, b) => a.startTime - b.startTime)
      .forEach((recording) => {
        cumulativeSize += recording.fileSize || 0;
        const date = format(new Date(recording.startTime), 'dd.MM', { locale: ru });
        if (!grouped[date]) {
          grouped[date] = 0;
        }
        grouped[date] = cumulativeSize / (1024 * 1024 * 1024); // Конвертируем в GB
      });

    return Object.entries(grouped)
      .map(([date, size]) => ({ date, size }))
      .sort((a, b) => {
        const dateA = new Date(a.date.split('.').reverse().join('-'));
        const dateB = new Date(b.date.split('.').reverse().join('-'));
        return dateA.getTime() - dateB.getTime();
      });
  }, [recordings]);

  const totalUsed = recordings.reduce((sum, r) => sum + (r.fileSize || 0), 0);
  const totalUsedGB = totalUsed / (1024 * 1024 * 1024);
  const totalStorageGB = totalStorage ? totalStorage / (1024 * 1024 * 1024) : null;
  const usagePercent = totalStorageGB ? (totalUsedGB / totalStorageGB) * 100 : null;

  return (
    <Paper sx={{ p: 2 }}>
      <Typography variant="h6" gutterBottom>
        Использование хранилища
      </Typography>

      {/* Общая статистика */}
      <Box sx={{ mb: 3, p: 2, bgcolor: 'grey.50', borderRadius: 1 }}>
        <Typography variant="body2" color="text.secondary">
          Использовано: <strong>{totalUsedGB.toFixed(2)} GB</strong>
          {totalStorageGB && (
            <>
              {' '}из <strong>{totalStorageGB.toFixed(2)} GB</strong>
              {' '}({usagePercent?.toFixed(1)}%)
            </>
          )}
        </Typography>
        {totalStorageGB && usagePercent && (
          <Box sx={{ mt: 1, width: '100%', height: 8, bgcolor: 'grey.300', borderRadius: 1, overflow: 'hidden' }}>
            <Box
              sx={{
                width: `${Math.min(usagePercent, 100)}%`,
                height: '100%',
                bgcolor: usagePercent > 90 ? 'error.main' : usagePercent > 70 ? 'warning.main' : 'primary.main',
                transition: 'width 0.3s',
              }}
            />
          </Box>
        )}
      </Box>

      <Box sx={{ width: '100%', height }}>
        <ResponsiveContainer>
          <PieChart>
            <Pie
              data={storageByCamera}
              cx="50%"
              cy="50%"
              labelLine={false}
              label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
              outerRadius={80}
              fill="#8884d8"
              dataKey="value"
            >
              {storageByCamera.map((_, index) => (
                <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
              ))}
            </Pie>
            <Tooltip formatter={(value: number) => `${value.toFixed(2)} GB`} />
            <Legend />
          </PieChart>
        </ResponsiveContainer>
      </Box>

      {storageOverTime.length > 0 && (
        <Box sx={{ mt: 3, width: '100%', height: 200 }}>
          <Typography variant="subtitle2" gutterBottom>
            Накопительное использование
          </Typography>
          <ResponsiveContainer>
            <LineChart data={storageOverTime}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" />
              <YAxis label={{ value: 'GB', angle: -90, position: 'insideLeft' }} />
              <Tooltip formatter={(value: number) => `${value.toFixed(2)} GB`} />
              <Line type="monotone" dataKey="size" stroke="#8884d8" name="Использовано" />
            </LineChart>
          </ResponsiveContainer>
        </Box>
      )}
    </Paper>
  );
}
