'use client';

import React, { useState, useRef, useEffect } from 'react';
import {
  Paper,
  Box,
  Typography,
  IconButton,
  Tooltip,
  Menu,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  Chip,
} from '@mui/material';
import {
  Download as DownloadIcon,
  Image as ImageIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material';
import { format } from 'date-fns';
import { ru } from 'date-fns/locale';
import { useWebSocket } from '@/hooks/useWebSocket';
import type { WebSocketChannel } from '@/utils/websocket';

export type ChartPeriod = 'day' | 'week' | 'month' | 'year' | 'custom';

interface ChartContainerProps {
  title: string;
  children: React.ReactNode;
  height?: number;
  period?: ChartPeriod;
  onPeriodChange?: (period: ChartPeriod) => void;
  onRefresh?: () => void;
  onExport?: (format: 'png' | 'svg' | 'csv') => void;
  showControls?: boolean;
  loading?: boolean;
  customDateRange?: {
    start: Date;
    end: Date;
  };
  onCustomDateRangeChange?: (start: Date, end: Date) => void;
  enableLiveUpdates?: boolean; // Включить real-time обновления через WebSocket
  liveChannels?: WebSocketChannel[]; // Каналы WebSocket для подписки
}

export default function ChartContainer({
  title,
  children,
  height = 300,
  period = 'week',
  onPeriodChange,
  onRefresh,
  onExport,
  showControls = true,
  loading = false,
  customDateRange,
  onCustomDateRangeChange,
  enableLiveUpdates = false,
  liveChannels = [],
}: ChartContainerProps) {
  const [menuAnchor, setMenuAnchor] = useState<null | HTMLElement>(null);
  const chartRef = useRef<HTMLDivElement>(null);
  const { connected, subscribe } = useWebSocket();

  // Подписка на WebSocket каналы для live обновлений
  useEffect(() => {
    if (enableLiveUpdates && connected && liveChannels.length > 0) {
      subscribe(liveChannels);
    }
  }, [enableLiveUpdates, connected, liveChannels, subscribe]);

  const handleExport = async (exportFormat: 'png' | 'svg' | 'csv') => {
    if (exportFormat === 'png' && chartRef.current) {
      try {
        const html2canvas = (await import('html2canvas')).default;
        const canvas = await html2canvas(chartRef.current, {
          backgroundColor: '#ffffff',
          scale: 2,
        });
        const link = document.createElement('a');
        link.download = `${title}-${format(new Date(), 'yyyy-MM-dd')}.png`;
        link.href = canvas.toDataURL('image/png');
        link.click();
      } catch (error) {
        console.error('Error exporting chart:', error);
        onExport?.(exportFormat);
      }
    } else {
      onExport?.(exportFormat);
    }
    setMenuAnchor(null);
  };

  const getPeriodLabel = () => {
    switch (period) {
      case 'day':
        return 'День';
      case 'week':
        return 'Неделя';
      case 'month':
        return 'Месяц';
      case 'year':
        return 'Год';
      case 'custom':
        return customDateRange
          ? `${format(customDateRange.start, 'dd.MM', { locale: ru })} - ${format(customDateRange.end, 'dd.MM', { locale: ru })}`
          : 'Кастомный';
      default:
        return 'Неделя';
    }
  };

  return (
    <Paper sx={{ p: 2, position: 'relative' }}>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
          <Typography variant="h6">{title}</Typography>
          {enableLiveUpdates && connected && (
            <Chip
              label="Live"
              size="small"
              color="success"
              sx={{
                animation: 'pulse 2s infinite',
                '@keyframes pulse': {
                  '0%, 100%': { opacity: 1 },
                  '50%': { opacity: 0.7 },
                },
              }}
            />
          )}
        </Box>
        {showControls && (
          <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
            {onPeriodChange && (
              <FormControl size="small" sx={{ minWidth: 120 }}>
                <InputLabel>Период</InputLabel>
                <Select
                  value={period}
                  onChange={(e) => onPeriodChange(e.target.value as ChartPeriod)}
                  label="Период"
                >
                  <MenuItem value="day">День</MenuItem>
                  <MenuItem value="week">Неделя</MenuItem>
                  <MenuItem value="month">Месяц</MenuItem>
                  <MenuItem value="year">Год</MenuItem>
                  <MenuItem value="custom">Кастомный</MenuItem>
                </Select>
              </FormControl>
            )}
            {onRefresh && (
              <Tooltip title="Обновить">
                <IconButton size="small" onClick={onRefresh} disabled={loading}>
                  <RefreshIcon />
                </IconButton>
              </Tooltip>
            )}
            {onExport && (
              <>
                <Tooltip title="Экспорт">
                  <IconButton
                    size="small"
                    onClick={(e) => setMenuAnchor(e.currentTarget)}
                  >
                    <DownloadIcon />
                  </IconButton>
                </Tooltip>
                <Menu
                  anchorEl={menuAnchor}
                  open={Boolean(menuAnchor)}
                  onClose={() => setMenuAnchor(null)}
                >
                  <MenuItem onClick={() => handleExport('png')}>
                    <ImageIcon sx={{ mr: 1 }} fontSize="small" />
                    Экспорт PNG
                  </MenuItem>
                  <MenuItem onClick={() => handleExport('svg')}>
                    <ImageIcon sx={{ mr: 1 }} fontSize="small" />
                    Экспорт SVG
                  </MenuItem>
                  <MenuItem onClick={() => handleExport('csv')}>
                    <DownloadIcon sx={{ mr: 1 }} fontSize="small" />
                    Экспорт CSV
                  </MenuItem>
                </Menu>
              </>
            )}
          </Box>
        )}
      </Box>

      {period === 'custom' && onCustomDateRangeChange && (
        <Box sx={{ mb: 2, p: 2, bgcolor: 'grey.50', borderRadius: 1 }}>
          <Typography variant="body2" gutterBottom>
            Выберите диапазон дат:
          </Typography>
          <Box sx={{ display: 'flex', gap: 2, mt: 1 }}>
            <input
              type="date"
              value={customDateRange?.start ? format(customDateRange.start, 'yyyy-MM-dd') : ''}
              onChange={(e) => {
                const start = e.target.value ? new Date(e.target.value) : new Date();
                onCustomDateRangeChange(
                  start,
                  customDateRange?.end || new Date()
                );
              }}
            />
            <input
              type="date"
              value={customDateRange?.end ? format(customDateRange.end, 'yyyy-MM-dd') : ''}
              onChange={(e) => {
                const end = e.target.value ? new Date(e.target.value) : new Date();
                onCustomDateRangeChange(
                  customDateRange?.start || new Date(),
                  end
                );
              }}
            />
          </Box>
        </Box>
      )}

      <Box
        ref={chartRef}
        sx={{
          width: '100%',
          height,
          position: 'relative',
          '& .recharts-wrapper': {
            width: '100% !important',
            height: '100% !important',
          },
        }}
      >
        {loading ? (
          <Box
            sx={{
              display: 'flex',
              justifyContent: 'center',
              alignItems: 'center',
              height: '100%',
            }}
          >
            <Typography variant="body2" color="text.secondary">
              Загрузка данных...
            </Typography>
          </Box>
        ) : (
          children
        )}
      </Box>

      {period !== 'custom' && (
        <Typography variant="caption" color="text.secondary" sx={{ mt: 1, display: 'block' }}>
          Период: {getPeriodLabel()}
        </Typography>
      )}
    </Paper>
  );
}
