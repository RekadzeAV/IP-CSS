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
  onDataPointClick?: (data: unknown) => void;
  showLegend?: boolean;
}

export default function EventsChart({
  events,
  height = 300,
  onDataPointClick,
  showLegend = true,
}: EventsChartProps) {
  // Группируем события по дням
  type DayEvents = { date: string; total: number; critical: number; error: number; warning: number; info: number };
  const eventsByDay = events.reduce<Record<string, DayEvents>>((acc, event) => {
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
  }, {});

  const chartData = Object.values(eventsByDay).sort((a, b) => {
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
          <LineChart
            data={chartData}
            onClick={onDataPointClick ? (data) => onDataPointClick(data) : undefined}
          >
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis />
            <Tooltip
              contentStyle={{
                backgroundColor: 'rgba(255, 255, 255, 0.95)',
                border: '1px solid #ccc',
                borderRadius: '4px',
              }}
              formatter={(value: number, name: string) => [
                value,
                name === 'total'
                  ? 'Всего'
                  : name === 'critical'
                  ? 'Критические'
                  : name === 'error'
                  ? 'Ошибки'
                  : name === 'warning'
                  ? 'Предупреждения'
                  : 'Информация',
              ]}
            />
            {showLegend && <Legend />}
            <Line
              type="monotone"
              dataKey="total"
              stroke="#8884d8"
              name="Всего"
              strokeWidth={2}
              dot={{ r: 4 }}
              activeDot={{ r: 6 }}
            />
            <Line
              type="monotone"
              dataKey="critical"
              stroke="#f44336"
              name="Критические"
              strokeWidth={2}
              dot={{ r: 4 }}
              activeDot={{ r: 6 }}
            />
            <Line
              type="monotone"
              dataKey="error"
              stroke="#ff9800"
              name="Ошибки"
              strokeWidth={2}
              dot={{ r: 4 }}
              activeDot={{ r: 6 }}
            />
            <Line
              type="monotone"
              dataKey="warning"
              stroke="#ffc107"
              name="Предупреждения"
              strokeWidth={2}
              dot={{ r: 4 }}
              activeDot={{ r: 6 }}
            />
            <Line
              type="monotone"
              dataKey="info"
              stroke="#2196f3"
              name="Информация"
              strokeWidth={2}
              dot={{ r: 4 }}
              activeDot={{ r: 6 }}
            />
          </LineChart>
        </ResponsiveContainer>
      </Box>
    </Paper>
  );
}



