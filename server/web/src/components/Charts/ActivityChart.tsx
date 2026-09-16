'use client';

import React, { useMemo } from 'react';
import { Paper, Typography, Box } from '@mui/material';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer,
  Cell,
} from 'recharts';
import type { Event, Camera } from '@/types';

interface ActivityChartProps {
  events: Event[];
  cameras?: Camera[];
  height?: number;
  chartType?: 'hourly' | 'camera' | 'heatmap';
}

type HeatmapRow = {
  day: string;
} & Record<number, number>;

export default function ActivityChart({
  events,
  height = 300,
  chartType = 'hourly',
}: ActivityChartProps) {
  // Активность по часам дня
  const hourlyActivity = useMemo(() => {
    const hourly: Record<number, number> = {};
    for (let i = 0; i < 24; i++) {
      hourly[i] = 0;
    }

    events.forEach((event) => {
      const hour = new Date(event.timestamp).getHours();
      hourly[hour]++;
    });

    return Array.from({ length: 24 }, (_, i) => ({
      hour: `${i.toString().padStart(2, '0')}:00`,
      count: hourly[i],
    }));
  }, [events]);

  // Активность по камерам
  const cameraActivity = useMemo(() => {
    const activity: Record<string, { name: string; count: number }> = {};

    events.forEach((event) => {
      const cameraId = event.cameraId;
      const cameraName = event.cameraName || cameraId;

      if (!activity[cameraId]) {
        activity[cameraId] = { name: cameraName, count: 0 };
      }
      activity[cameraId].count++;
    });

    return Object.values(activity)
      .sort((a, b) => b.count - a.count)
      .slice(0, 10); // Топ 10 камер
  }, [events]);

  // Heatmap данных (часы дня vs дни недели)
  const heatmapData = useMemo<HeatmapRow[]>(() => {
    const heatmap: Record<string, Record<number, number>> = {};
    const days = ['Пн', 'Вт', 'Ср', 'Чт', 'Пт', 'Сб', 'Вс'];

    events.forEach((event) => {
      const date = new Date(event.timestamp);
      const dayOfWeek = date.getDay() === 0 ? 6 : date.getDay() - 1; // Понедельник = 0
      const hour = date.getHours();
      const dayKey = days[dayOfWeek];

      if (!heatmap[dayKey]) {
        heatmap[dayKey] = {};
      }
      if (!heatmap[dayKey][hour]) {
        heatmap[dayKey][hour] = 0;
      }
      heatmap[dayKey][hour]++;
    });

    return Object.entries(heatmap).map(([day, hours]) => {
      const row = { day } as HeatmapRow;
      for (let i = 0; i < 24; i++) {
        row[i] = hours[i] || 0;
      }
      return row;
    });
  }, [events]);

  const getMaxValue = (data: Array<{ count: number }>) => {
    return Math.max(...data.map((item) => item.count || 0));
  };

  const getColor = (value: number, max: number) => {
    const intensity = value / max;
    if (intensity > 0.7) return '#f44336';
    if (intensity > 0.4) return '#ff9800';
    if (intensity > 0.2) return '#ffc107';
    return '#4caf50';
  };

  const renderChart = () => {
    switch (chartType) {
      case 'camera':
        const maxCamera = getMaxValue(cameraActivity);
        return (
          <BarChart data={cameraActivity} layout="vertical">
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis type="number" />
            <YAxis dataKey="name" type="category" width={100} />
            <Tooltip />
            <Legend />
            <Bar dataKey="count" name="События">
              {cameraActivity.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={getColor(entry.count, maxCamera)} />
              ))}
            </Bar>
          </BarChart>
        );
      case 'heatmap':
        return (
          <BarChart data={heatmapData}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="day" />
            <YAxis />
            <Tooltip />
            <Legend />
            {Array.from({ length: 24 }, (_, i) => (
              <Bar key={i} dataKey={i} stackId="a" fill={getColor(heatmapData[0]?.[i] || 0, 10)} />
            ))}
          </BarChart>
        );
      default:
        const maxHourly = getMaxValue(hourlyActivity);
        return (
          <BarChart data={hourlyActivity}>
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="hour" />
            <YAxis />
            <Tooltip />
            <Legend />
            <Bar dataKey="count" name="События">
              {hourlyActivity.map((entry, index) => (
                <Cell key={`cell-${index}`} fill={getColor(entry.count, maxHourly)} />
              ))}
            </Bar>
          </BarChart>
        );
    }
  };

  return (
    <Paper sx={{ p: 2 }}>
      <Typography variant="h6" gutterBottom>
        Активность системы
      </Typography>
      <Box sx={{ width: '100%', height }}>
        <ResponsiveContainer>
          {renderChart()}
        </ResponsiveContainer>
      </Box>
    </Paper>
  );
}
