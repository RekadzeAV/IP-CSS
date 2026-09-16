'use client';

import React, { useMemo } from 'react';
import { Paper, Typography, Box, FormControl, InputLabel, Select, MenuItem } from '@mui/material';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import type { Recording } from '@/types';
import { format } from 'date-fns';
import { ru } from 'date-fns/locale';

interface RecordingsChartProps {
  recordings: Recording[];
  height?: number;
  chartType?: 'line' | 'bar' | 'area';
  metric?: 'count' | 'size' | 'duration';
  period?: 'day' | 'hour';
  onDataPointClick?: (data: unknown) => void;
  showControls?: boolean;
}

export default function RecordingsChart({
  recordings,
  height = 300,
  chartType = 'line',
  metric = 'count',
  period = 'day',
  onDataPointClick,
  showControls = true,
}: RecordingsChartProps) {
  const chartData = useMemo(() => {
    const grouped: Record<string, { date: string; count: number; size: number; duration: number }> = {};

    recordings.forEach((recording) => {
      const date = new Date(recording.startTime);
      let key: string;

      if (period === 'hour') {
        key = format(date, 'dd.MM HH:00', { locale: ru });
      } else {
        key = format(date, 'dd.MM.yyyy', { locale: ru });
      }

      if (!grouped[key]) {
        grouped[key] = { date: key, count: 0, size: 0, duration: 0 };
      }

      grouped[key].count++;
      grouped[key].size += recording.fileSize || 0;
      grouped[key].duration += recording.duration;
    });

    return Object.values(grouped).sort((a, b) => {
      const dateA = new Date(a.date.split('.').reverse().join('-'));
      const dateB = new Date(b.date.split('.').reverse().join('-'));
      return dateA.getTime() - dateB.getTime();
    });
  }, [recordings, period]);

  const getDataKey = () => {
    switch (metric) {
      case 'size':
        return 'size';
      case 'duration':
        return 'duration';
      default:
        return 'count';
    }
  };

  const getYAxisLabel = () => {
    switch (metric) {
      case 'size':
        return 'Размер (МБ)';
      case 'duration':
        return 'Длительность (сек)';
      default:
        return 'Количество';
    }
  };

  const formatValue = (value: number) => {
    switch (metric) {
      case 'size':
        return `${(value / (1024 * 1024)).toFixed(2)} МБ`;
      case 'duration':
        return `${Math.floor(value / 3600)}:${Math.floor((value % 3600) / 60).toString().padStart(2, '0')}`;
      default:
        return value.toString();
    }
  };

  const renderChart = () => {
    const dataKey = getDataKey();
    const commonProps = {
      data: chartData,
      margin: { top: 5, right: 30, left: 20, bottom: 5 },
    };

    switch (chartType) {
      case 'bar':
        return (
          <BarChart
            {...commonProps}
            onClick={onDataPointClick ? (data) => onDataPointClick(data) : undefined}
          >
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis label={{ value: getYAxisLabel(), angle: -90, position: 'insideLeft' }} />
            <Tooltip
              formatter={(value: number) => formatValue(value)}
              contentStyle={{
                backgroundColor: 'rgba(255, 255, 255, 0.95)',
                border: '1px solid #ccc',
                borderRadius: '4px',
              }}
            />
            <Legend />
            <Bar
              dataKey={dataKey}
              fill="#8884d8"
              name={metric === 'count' ? 'Количество' : metric === 'size' ? 'Размер' : 'Длительность'}
            />
          </BarChart>
        );
      case 'area':
        return (
          <AreaChart
            {...commonProps}
            onClick={onDataPointClick ? (data) => onDataPointClick(data) : undefined}
          >
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis label={{ value: getYAxisLabel(), angle: -90, position: 'insideLeft' }} />
            <Tooltip
              formatter={(value: number) => formatValue(value)}
              contentStyle={{
                backgroundColor: 'rgba(255, 255, 255, 0.95)',
                border: '1px solid #ccc',
                borderRadius: '4px',
              }}
            />
            <Legend />
            <Area
              type="monotone"
              dataKey={dataKey}
              stroke="#8884d8"
              fill="#8884d8"
              name={metric === 'count' ? 'Количество' : metric === 'size' ? 'Размер' : 'Длительность'}
            />
          </AreaChart>
        );
      default:
        return (
          <LineChart
            {...commonProps}
            onClick={onDataPointClick ? (data) => onDataPointClick(data) : undefined}
          >
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis label={{ value: getYAxisLabel(), angle: -90, position: 'insideLeft' }} />
            <Tooltip
              formatter={(value: number) => formatValue(value)}
              contentStyle={{
                backgroundColor: 'rgba(255, 255, 255, 0.95)',
                border: '1px solid #ccc',
                borderRadius: '4px',
              }}
            />
            <Legend />
            <Line
              type="monotone"
              dataKey={dataKey}
              stroke="#8884d8"
              name={metric === 'count' ? 'Количество' : metric === 'size' ? 'Размер' : 'Длительность'}
              strokeWidth={2}
              dot={{ r: 4 }}
              activeDot={{ r: 6 }}
            />
          </LineChart>
        );
    }
  };

  return (
    <Paper sx={{ p: 2 }}>
        {showControls && (
          <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
            <Typography variant="h6">Записи по времени</Typography>
            <Box sx={{ display: 'flex', gap: 1 }}>
              <FormControl size="small" sx={{ minWidth: 120 }}>
                <InputLabel>Тип</InputLabel>
                <Select value={chartType} label="Тип" disabled>
                  <MenuItem value="line">Линия</MenuItem>
                  <MenuItem value="bar">Столбцы</MenuItem>
                  <MenuItem value="area">Область</MenuItem>
                </Select>
              </FormControl>
              <FormControl size="small" sx={{ minWidth: 120 }}>
                <InputLabel>Метрика</InputLabel>
                <Select value={metric} label="Метрика" disabled>
                  <MenuItem value="count">Количество</MenuItem>
                  <MenuItem value="size">Размер</MenuItem>
                  <MenuItem value="duration">Длительность</MenuItem>
                </Select>
              </FormControl>
            </Box>
          </Box>
        )}
      <Box sx={{ width: '100%', height }}>
        <ResponsiveContainer>
          {renderChart()}
        </ResponsiveContainer>
      </Box>
    </Paper>
  );
}
