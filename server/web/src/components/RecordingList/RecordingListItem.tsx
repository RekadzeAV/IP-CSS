'use client';

import React from 'react';
import {
  Card,
  CardContent,
  CardActions,
  Typography,
  Box,
  Chip,
  IconButton,
  Tooltip,
  Checkbox,
} from '@mui/material';
import {
  PlayArrow as PlayArrowIcon,
  Download as DownloadIcon,
  Delete as DeleteIcon,
  Videocam as VideocamIcon,
} from '@mui/icons-material';
import { Schedule as ScheduleIcon } from '@mui/icons-material';
import type { Recording } from '@/types';
import { formatDate, formatDuration, formatFileSize, getStatusColor, getStatusLabel } from './utils';

interface RecordingListItemProps {
  recording: Recording;
  selected?: boolean;
  onSelect?: (recordingId: string, selected: boolean) => void;
  onRecordingClick?: (recordingId: string) => void;
  onDelete?: (recordingId: string) => void;
  onDownload?: (recordingId: string) => void;
  onExport?: (recordingId: string) => void;
  showCheckbox?: boolean;
  viewMode?: 'grid' | 'list' | 'table';
  isNew?: boolean; // Флаг для анимации новых записей
}

export default function RecordingListItem({
  recording,
  selected = false,
  onSelect,
  onRecordingClick,
  onDelete,
  onDownload,
  onExport,
  showCheckbox = false,
  viewMode = 'grid',
  isNew = false,
}: RecordingListItemProps) {
  const handleCardClick = (e: React.MouseEvent) => {
    // Не переходить, если клик был по кнопке или чекбоксу
    if ((e.target as HTMLElement).closest('button, input')) {
      return;
    }
    onRecordingClick?.(recording.id);
  };

  const handleDelete = (e: React.MouseEvent) => {
    e.stopPropagation();
    onDelete?.(recording.id);
  };

  const handleDownload = (e: React.MouseEvent) => {
    e.stopPropagation();
    onDownload?.(recording.id);
  };

  const handleExport = (e: React.MouseEvent) => {
    e.stopPropagation();
    onExport?.(recording.id);
  };

  if (viewMode === 'table') {
    // Table view будет реализован отдельно
    return null;
  }

  return (
    <Card
      sx={{
        cursor: 'pointer',
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        border: selected ? 2 : 0,
        borderColor: 'primary.main',
        // Анимация для новых записей
        ...(isNew && {
          animation: 'slideIn 0.5s ease-out',
          '@keyframes slideIn': {
            '0%': {
              opacity: 0,
              transform: 'translateY(-20px)',
            },
            '100%': {
              opacity: 1,
              transform: 'translateY(0)',
            },
          },
        }),
        // Подсветка новых записей
        ...(isNew && {
          boxShadow: '0 4px 12px rgba(25, 118, 210, 0.3)',
          border: '1px solid',
          borderColor: 'primary.main',
        }),
      }}
      onClick={handleCardClick}
    >
      {recording.thumbnailUrl && (
        <Box
          sx={{
            width: '100%',
            height: viewMode === 'list' ? 120 : 200,
            backgroundImage: `url(${recording.thumbnailUrl})`,
            backgroundSize: 'cover',
            backgroundPosition: 'center',
            flexShrink: 0,
          }}
        />
      )}
      <CardContent sx={{ flexGrow: 1 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'start', mb: 1 }}>
          <Typography variant="h6" component="div" noWrap sx={{ flex: 1 }}>
            {recording.cameraName || recording.cameraId}
          </Typography>
          <Chip
            label={getStatusLabel(recording.status)}
            color={getStatusColor(recording.status)}
            size="small"
          />
        </Box>
        <Typography variant="body2" color="text.secondary" gutterBottom>
          <ScheduleIcon sx={{ fontSize: 16, verticalAlign: 'middle', mr: 0.5 }} />
          {formatDate(recording.startTime)}
        </Typography>
        <Box sx={{ mt: 1, display: 'flex', gap: 1, flexWrap: 'wrap' }}>
          <Chip
            label={`Длительность: ${formatDuration(recording.duration)}`}
            size="small"
            variant="outlined"
          />
          <Chip
            label={`Размер: ${formatFileSize(recording.fileSize)}`}
            size="small"
            variant="outlined"
          />
          <Chip label={recording.format} size="small" variant="outlined" />
          {recording.codec && <Chip label={recording.codec} size="small" color="secondary" variant="outlined" />}
        </Box>
      </CardContent>
      <CardActions>
        {showCheckbox && (
          <Checkbox
            checked={selected}
            onChange={(e) => {
              e.stopPropagation();
              onSelect?.(recording.id, e.target.checked);
            }}
            onClick={(e) => e.stopPropagation()}
            size="small"
          />
        )}
        <Tooltip title="Просмотр деталей">
          <IconButton
            size="small"
            color="primary"
            onClick={(e) => {
              e.stopPropagation();
              onRecordingClick?.(recording.id);
            }}
          >
            <PlayArrowIcon />
          </IconButton>
        </Tooltip>
        <Tooltip title="Скачать">
          <IconButton size="small" onClick={handleDownload} color="primary">
            <DownloadIcon />
          </IconButton>
        </Tooltip>
        <Tooltip title="Экспортировать">
          <IconButton size="small" onClick={handleExport} color="primary">
            <VideocamIcon />
          </IconButton>
        </Tooltip>
        <Box sx={{ flexGrow: 1 }} />
        <Tooltip title="Удалить">
          <IconButton size="small" onClick={handleDelete} color="error">
            <DeleteIcon />
          </IconButton>
        </Tooltip>
      </CardActions>
    </Card>
  );
}
