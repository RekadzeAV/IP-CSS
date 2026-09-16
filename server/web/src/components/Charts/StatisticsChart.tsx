'use client';

import React, { useMemo } from 'react';
import { Paper, Typography, Box, Grid, Card, CardContent } from '@mui/material';
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
import type { Event, Recording, Camera } from '@/types';
import { format, subDays } from 'date-fns';
import { ru } from 'date-fns/locale';

interface StatisticsChartProps {
  events?: Event[];
  recordings?: Recording[];
  cameras?: Camera[];
  height?: number;
  period?: number; // Количество дней для анализа
  compareWithPrevious?: boolean; // Сравнение с предыдущим периодом
  onDataPointClick?: (data: unknown) => void;
}

export default function StatisticsChart({
  events = [],
  recordings = [],
  cameras = [],
  height = 300,
  period = 7,
  compareWithPrevious = false,
  onDataPointClick,
}: StatisticsChartProps) {
  // Ключевые метрики (KPI)
  const kpi = useMemo(() => {
    const now = new Date();
    const periodStart = subDays(now, period).getTime();

    const recentEvents = events.filter((e) => e.timestamp >= periodStart);
    const recentRecordings = recordings.filter((r) => r.startTime >= periodStart);

    const totalStorage = recordings.reduce((sum, r) => sum + (r.fileSize || 0), 0);
    const onlineCameras = cameras.filter((c) => c.status === 'ONLINE').length;

    return {
      totalEvents: events.length,
      recentEvents: recentEvents.length,
      totalRecordings: recordings.length,
      recentRecordings: recentRecordings.length,
      totalStorage: totalStorage / (1024 * 1024 * 1024), // GB
      onlineCameras,
      totalCameras: cameras.length,
      criticalEvents: recentEvents.filter((e) => e.severity === 'CRITICAL').length,
    };
  }, [events, recordings, cameras, period]);

  // Тренды по дням
  const trends = useMemo(() => {
    const now = new Date();
    const trendsData: Array<{
      date: string;
      events: number;
      recordings: number;
      storage: number;
    }> = [];

    for (let i = period - 1; i >= 0; i--) {
      const date = subDays(now, i);
      const dayStart = startOfDay(date).getTime();
      const dayEnd = endOfDay(date).getTime();

      const dayEvents = events.filter(
        (e) => e.timestamp >= dayStart && e.timestamp <= dayEnd
      ).length;

      const dayRecordings = recordings.filter(
        (r) => r.startTime >= dayStart && r.startTime <= dayEnd
      );

      const dayStorage = dayRecordings.reduce(
        (sum, r) => sum + (r.fileSize || 0),
        0
      ) / (1024 * 1024 * 1024); // GB

      trendsData.push({
        date: format(date, 'dd.MM', { locale: ru }),
        events: dayEvents,
        recordings: dayRecordings.length,
        storage: dayStorage,
      });
    }

    return trendsData;
  }, [events, recordings, period]);

  const startOfDay = (date: Date) => {
    const d = new Date(date);
    d.setHours(0, 0, 0, 0);
    return d;
  };

  const endOfDay = (date: Date) => {
    const d = new Date(date);
    d.setHours(23, 59, 59, 999);
    return d;
  };

  return (
    <Paper sx={{ p: 2 }}>
      <Typography variant="h6" gutterBottom>
        Общая статистика системы
      </Typography>

      {/* KPI карточки */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={6} sm={3}>
          <Card>
            <CardContent>
              <Typography variant="h4" color="primary">
                {kpi.totalEvents}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Всего событий
              </Typography>
              <Typography variant="caption" color="text.secondary">
                За период: {kpi.recentEvents}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={6} sm={3}>
          <Card>
            <CardContent>
              <Typography variant="h4" color="primary">
                {kpi.totalRecordings}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Всего записей
              </Typography>
              <Typography variant="caption" color="text.secondary">
                За период: {kpi.recentRecordings}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={6} sm={3}>
          <Card>
            <CardContent>
              <Typography variant="h4" color="primary">
                {kpi.totalStorage.toFixed(1)}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                GB хранилища
              </Typography>
              <Typography variant="caption" color="text.secondary">
                Использовано
              </Typography>
            </CardContent>
          </Card>
        </Grid>
        <Grid item xs={6} sm={3}>
          <Card>
            <CardContent>
              <Typography variant="h4" color={kpi.onlineCameras === kpi.totalCameras ? 'success.main' : 'warning.main'}>
                {kpi.onlineCameras}/{kpi.totalCameras}
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Камер онлайн
              </Typography>
              {kpi.criticalEvents > 0 && (
                <Typography variant="caption" color="error">
                  Критичных: {kpi.criticalEvents}
                </Typography>
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* График трендов */}
      <Box sx={{ width: '100%', height }}>
        <Typography variant="subtitle2" gutterBottom>
          Тренды за последние {period} дней
        </Typography>
        <ResponsiveContainer>
          <LineChart
            data={trends}
            onClick={onDataPointClick ? (data) => onDataPointClick(data) : undefined}
          >
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="date" />
            <YAxis yAxisId="left" />
            <YAxis yAxisId="right" orientation="right" />
            <Tooltip
              contentStyle={{
                backgroundColor: 'rgba(255, 255, 255, 0.95)',
                border: '1px solid #ccc',
                borderRadius: '4px',
              }}
            />
            <Legend />
            <Line
              yAxisId="left"
              type="monotone"
              dataKey="events"
              stroke="#8884d8"
              name="События"
              strokeWidth={2}
              dot={{ r: 4 }}
              activeDot={{ r: 6 }}
            />
            <Line
              yAxisId="left"
              type="monotone"
              dataKey="recordings"
              stroke="#82ca9d"
              name="Записи"
              strokeWidth={2}
              dot={{ r: 4 }}
              activeDot={{ r: 6 }}
            />
            <Line
              yAxisId="right"
              type="monotone"
              dataKey="storage"
              stroke="#ffc658"
              name="Хранилище (GB)"
              strokeWidth={2}
              dot={{ r: 4 }}
              activeDot={{ r: 6 }}
            />
            {compareWithPrevious && (
              <>
                <Line
                  yAxisId="left"
                  type="monotone"
                  dataKey="eventsPrevious"
                  stroke="#8884d8"
                  strokeDasharray="5 5"
                  name="События (пред. период)"
                  strokeWidth={1}
                  dot={false}
                />
                <Line
                  yAxisId="left"
                  type="monotone"
                  dataKey="recordingsPrevious"
                  stroke="#82ca9d"
                  strokeDasharray="5 5"
                  name="Записи (пред. период)"
                  strokeWidth={1}
                  dot={false}
                />
                <Line
                  yAxisId="right"
                  type="monotone"
                  dataKey="storagePrevious"
                  stroke="#ffc658"
                  strokeDasharray="5 5"
                  name="Хранилище (пред. период)"
                  strokeWidth={1}
                  dot={false}
                />
              </>
            )}
          </LineChart>
        </ResponsiveContainer>
      </Box>
    </Paper>
  );
}
