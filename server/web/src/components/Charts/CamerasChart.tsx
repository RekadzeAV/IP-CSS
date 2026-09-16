'use client';

import React from 'react';
import { Paper, Typography, Box } from '@mui/material';
import {
  PieChart,
  Pie,
  Cell,
  ResponsiveContainer,
  Legend,
  Tooltip,
} from 'recharts';
import type { Camera } from '@/types';

interface CamerasChartProps {
  cameras: Camera[];
  height?: number;
  onSegmentClick?: (data: unknown) => void;
  showLegend?: boolean;
}

const COLORS = ['#4caf50', '#f44336', '#ff9800', '#9e9e9e'];

export default function CamerasChart({
  cameras,
  height = 300,
  onSegmentClick,
  showLegend = true,
}: CamerasChartProps) {
  const data = [
    {
      name: 'Онлайн',
      value: cameras.filter((c) => c.status === 'ONLINE').length,
    },
    {
      name: 'Офлайн',
      value: cameras.filter((c) => c.status === 'OFFLINE').length,
    },
    {
      name: 'Ошибка',
      value: cameras.filter((c) => c.status === 'ERROR').length,
    },
    {
      name: 'Подключение',
      value: cameras.filter((c) => c.status === 'CONNECTING').length,
    },
  ].filter((item) => item.value > 0);

  return (
    <Paper sx={{ p: 2 }}>
      <Typography variant="h6" gutterBottom>
        Статус камер
      </Typography>
      <Box sx={{ width: '100%', height }}>
        <ResponsiveContainer>
          <PieChart>
            <Pie
              data={data}
              cx="50%"
              cy="50%"
              labelLine={false}
              label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
              outerRadius={80}
              fill="#8884d8"
              dataKey="value"
              onClick={onSegmentClick}
              activeIndex={undefined}
            >
              {data.map((_, index) => (
                <Cell
                  key={`cell-${index}`}
                  fill={COLORS[index % COLORS.length]}
                  style={{ cursor: onSegmentClick ? 'pointer' : 'default' }}
                />
              ))}
            </Pie>
            <Tooltip
              contentStyle={{
                backgroundColor: 'rgba(255, 255, 255, 0.95)',
                border: '1px solid #ccc',
                borderRadius: '4px',
              }}
              formatter={(value: number, name: string) => [
                `${value} камер`,
                name,
              ]}
            />
            {showLegend && <Legend />}
          </PieChart>
        </ResponsiveContainer>
      </Box>
    </Paper>
  );
}



