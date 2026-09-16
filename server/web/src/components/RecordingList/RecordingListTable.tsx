'use client';

import React from 'react';
import {
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Checkbox,
  IconButton,
  Tooltip,
  Chip,
  Box,
} from '@mui/material';
import {
  PlayArrow as PlayArrowIcon,
  Download as DownloadIcon,
  Delete as DeleteIcon,
  Videocam as VideocamIcon,
} from '@mui/icons-material';
import type { Recording } from '@/types';
import { formatDate, formatDuration, formatFileSize, getStatusColor, getStatusLabel } from './utils';

interface RecordingListTableProps {
  recordings: Recording[];
  selectedIds: string[];
  onSelect: (recordingId: string, selected: boolean) => void;
  onSelectAll: (selected: boolean) => void;
  onRecordingClick?: (recordingId: string) => void;
  onDelete?: (recordingId: string) => void;
  onDownload?: (recordingId: string) => void;
  onExport?: (recordingId: string) => void;
}

export default function RecordingListTable({
  recordings,
  selectedIds,
  onSelect,
  onSelectAll,
  onRecordingClick,
  onDelete,
  onDownload,
  onExport,
}: RecordingListTableProps) {
  const allSelected = recordings.length > 0 && selectedIds.length === recordings.length;
  const someSelected = selectedIds.length > 0 && selectedIds.length < recordings.length;

  const handleSelectAll = (event: React.ChangeEvent<HTMLInputElement>) => {
    onSelectAll(event.target.checked);
  };

  const handleSelect = (recordingId: string) => (event: React.ChangeEvent<HTMLInputElement>) => {
    onSelect(recordingId, event.target.checked);
  };

  const handleRowClick = (recordingId: string) => {
    onRecordingClick?.(recordingId);
  };

  const handleAction = (e: React.MouseEvent, action: () => void) => {
    e.stopPropagation();
    action();
  };

  return (
    <TableContainer component={Paper}>
      <Table>
        <TableHead>
          <TableRow>
            <TableCell padding="checkbox">
              <Checkbox
                indeterminate={someSelected}
                checked={allSelected}
                onChange={handleSelectAll}
              />
            </TableCell>
            <TableCell>Камера</TableCell>
            <TableCell>Дата начала</TableCell>
            <TableCell>Длительность</TableCell>
            <TableCell>Размер</TableCell>
            <TableCell>Формат</TableCell>
            <TableCell>Кодек</TableCell>
            <TableCell>Статус</TableCell>
            <TableCell align="right">Действия</TableCell>
          </TableRow>
        </TableHead>
        <TableBody>
          {recordings.length === 0 ? (
            <TableRow>
              <TableCell colSpan={9} align="center">
                Записи не найдены
              </TableCell>
            </TableRow>
          ) : (
            recordings.map((recording) => (
              <TableRow
                key={recording.id}
                hover
                onClick={() => handleRowClick(recording.id)}
                sx={{ cursor: 'pointer' }}
                selected={selectedIds.includes(recording.id)}
              >
                <TableCell padding="checkbox">
                  <Checkbox
                    checked={selectedIds.includes(recording.id)}
                    onChange={handleSelect(recording.id)}
                    onClick={(e) => e.stopPropagation()}
                  />
                </TableCell>
                <TableCell>
                  <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                    {recording.thumbnailUrl && (
                      <Box
                        component="img"
                        src={recording.thumbnailUrl}
                        alt=""
                        sx={{
                          width: 40,
                          height: 30,
                          objectFit: 'cover',
                          borderRadius: 1,
                        }}
                      />
                    )}
                    <Box>
                      <Box sx={{ fontWeight: 'medium' }}>
                        {recording.cameraName || recording.cameraId}
                      </Box>
                      <Box sx={{ fontSize: '0.75rem', color: 'text.secondary' }}>
                        ID: {recording.id.slice(0, 8)}...
                      </Box>
                    </Box>
                  </Box>
                </TableCell>
                <TableCell>{formatDate(recording.startTime)}</TableCell>
                <TableCell>{formatDuration(recording.duration)}</TableCell>
                <TableCell>{formatFileSize(recording.fileSize)}</TableCell>
                <TableCell>
                  <Chip label={recording.format} size="small" variant="outlined" />
                </TableCell>
                <TableCell>
                  {recording.codec ? (
                    <Chip label={recording.codec} size="small" color="secondary" variant="outlined" />
                  ) : (
                    '—'
                  )}
                </TableCell>
                <TableCell>
                  <Chip
                    label={getStatusLabel(recording.status)}
                    color={getStatusColor(recording.status)}
                    size="small"
                  />
                </TableCell>
                <TableCell align="right" onClick={(e) => e.stopPropagation()}>
                  <Box sx={{ display: 'flex', gap: 0.5, justifyContent: 'flex-end' }}>
                    <Tooltip title="Просмотр деталей">
                      <IconButton
                        size="small"
                        color="primary"
                        onClick={(e) => handleAction(e, () => onRecordingClick?.(recording.id))}
                      >
                        <PlayArrowIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Скачать">
                      <IconButton
                        size="small"
                        color="primary"
                        onClick={(e) => handleAction(e, () => onDownload?.(recording.id))}
                      >
                        <DownloadIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Экспортировать">
                      <IconButton
                        size="small"
                        color="primary"
                        onClick={(e) => handleAction(e, () => onExport?.(recording.id))}
                      >
                        <VideocamIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                    <Tooltip title="Удалить">
                      <IconButton
                        size="small"
                        color="error"
                        onClick={(e) => handleAction(e, () => onDelete?.(recording.id))}
                      >
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </Tooltip>
                  </Box>
                </TableCell>
              </TableRow>
            ))
          )}
        </TableBody>
      </Table>
    </TableContainer>
  );
}
