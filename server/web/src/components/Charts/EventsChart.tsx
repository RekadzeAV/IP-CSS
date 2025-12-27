'use client';

import React from 'react';
import { Paper, Typography, Box } from '@mui/material';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
} from 'recharts';
import type { Event } from '@/types';

interface EventsChartProps {
  events: Event[];
  height?: number;
}

export default function EventsChart({ events, height = 300 }: EventsChartProps) {
  // Группируем события по дням
  const eventsByDay = events.reduce((acc, event) => {
    const date = new Date(event.timestamp);
    const dayKey = date.toLocaleDateString('ru-RU', { day: '2-digit', month: '2-digit' });
    if (!acc[dayKey]) {
      acc[dayKey] = { date: dayKey, total: 0, critical: 0, error: 0, warning: 0, info: 0 };
    }
    acc[dayKey].total++;
    if (event.severity === 'CRITICAL') acc[dayKey].critical++;
    else if (event.severity === 'ERROR') acc[dayKey].error++;
    else if (event.severity === 'WARNING') acc[dayKey].warning++;
    else if (event.severity === 'INFO') acc[dayKey].info++;
    return acc;
  }, {} as Record<string, any>);

  const chartData = Object.values(eventsByDay).sort((a: any, b: any) => {
    return new Date(a.date.split('.').reverse().join('-')).getTime() -
      new Date(b.date.split('.').reverse().join('-')).getTime();
  });

  return (
    <Paper sx={{ p: 2 }}>
      <Typography variant="h6" gutterBottom>
        События по дням
      </Typography>
      <Box sx={{ width: '100%', height }}>
        <ResponsiveContainer>
          <LineChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis />
            <Tooltip />
            <Legend />
            <Line type="monotone" dataKey="total" stroke="#8884d8" name="Всего" />
            <Line type="monotone" dataKey="critical" stroke="#f44336" name="Критические" />
            <Line type="monotone" dataKey="error" stroke="#ff9800" name="Ошибки" />
            <Line type="monotone" dataKey="warning" stroke="#ffc107" name="Предупреждения" />
            <Line type="monotone" dataKey="info" stroke="#2196f3" name="Информация" />
          </LineChart>
        </ResponsiveContainer>
      </Box>
    </Paper>
  );
}

