'use client';

import React from 'react';
import {
  Card,
  CardContent,
  CardActions,
  Typography,
  Button,
  Chip,
  Box,
  Avatar,
  IconButton,
} from '@mui/material';
import {
  Videocam as VideocamIcon,
  CheckCircle as CheckCircleIcon,
  Error as ErrorIcon,
  Warning as WarningIcon,
  MoreVert as MoreVertIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
} from '@mui/icons-material';
import { useRouter } from 'next/navigation';
import type { Camera, CameraStatus } from '@/types';

interface CameraCardProps {
  camera: Camera;
  onEdit?: (camera: Camera) => void;
  onDelete?: (cameraId: string) => void;
}

const getStatusColor = (status: CameraStatus): 'success' | 'error' | 'warning' | 'default' => {
  switch (status) {
    case 'ONLINE':
      return 'success';
    case 'OFFLINE':
      return 'error';
    case 'ERROR':
      return 'error';
    case 'CONNECTING':
      return 'warning';
    default:
      return 'default';
  }
};

const getStatusIcon = (status: CameraStatus) => {
  switch (status) {
    case 'ONLINE':
      return <CheckCircleIcon />;
    case 'OFFLINE':
    case 'ERROR':
      return <ErrorIcon />;
    case 'CONNECTING':
      return <WarningIcon />;
    default:
      return null;
  }
};

const getStatusLabel = (status: CameraStatus): string => {
  switch (status) {
    case 'ONLINE':
      return 'Онлайн';
    case 'OFFLINE':
      return 'Офлайн';
    case 'ERROR':
      return 'Ошибка';
    case 'CONNECTING':
      return 'Подключение';
    default:
      return 'Неизвестно';
  }
};

export default function CameraCard({ camera, onEdit, onDelete }: CameraCardProps) {
  const router = useRouter();

  const handleView = () => {
    router.push(`/cameras/${camera.id}`);
  };

  const handleEdit = (e: React.MouseEvent) => {
    e.stopPropagation();
    onEdit?.(camera);
  };

  const handleDelete = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (window.confirm(`Удалить камеру "${camera.name}"?`)) {
      onDelete?.(camera.id);
    }
  };

  return (
    <Card
      sx={{
        height: '100%',
        display: 'flex',
        flexDirection: 'column',
        cursor: 'pointer',
        '&:hover': {
          boxShadow: 6,
        },
      }}
      onClick={handleView}
    >
      <CardContent sx={{ flexGrow: 1 }}>
        <Box sx={{ display: 'flex', alignItems: 'center', mb: 2 }}>
          <Avatar sx={{ bgcolor: 'primary.main', mr: 2 }}>
            <VideocamIcon />
          </Avatar>
          <Box sx={{ flexGrow: 1 }}>
            <Typography variant="h6" component="div">
              {camera.name}
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {camera.url}
            </Typography>
          </Box>
          <Chip
            icon={getStatusIcon(camera.status)}
            label={getStatusLabel(camera.status)}
            color={getStatusColor(camera.status)}
            size="small"
          />
        </Box>

        <Box sx={{ mt: 2 }}>
          {camera.model && (
            <Typography variant="body2" color="text.secondary">
              Модель: {camera.model}
            </Typography>
          )}
          {camera.resolution && (
            <Typography variant="body2" color="text.secondary">
              Разрешение: {camera.resolution.width}x{camera.resolution.height}
            </Typography>
          )}
          <Typography variant="body2" color="text.secondary">
            FPS: {camera.fps} | Битрейт: {camera.bitrate} kbps
          </Typography>
        </Box>
      </CardContent>
      <CardActions sx={{ justifyContent: 'space-between', px: 2, pb: 2 }}>
        <Button size="small" onClick={handleView}>
          Просмотр
        </Button>
        <Box>
          <IconButton size="small" onClick={handleEdit} color="primary">
            <EditIcon />
          </IconButton>
          <IconButton size="small" onClick={handleDelete} color="error">
            <DeleteIcon />
          </IconButton>
        </Box>
      </CardActions>
    </Card>
  );
}

