'use client';

import React, { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import {
  Box,
  Typography,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  IconButton,
  Button,
  CircularProgress,
  Alert,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  Checkbox,
  Toolbar,
  Tooltip,
  Badge,
} from '@mui/material';
import {
  CheckCircle as CheckCircleIcon,
  Delete as DeleteIcon,
  FilterList as FilterListIcon,
  MarkEmailRead as MarkEmailReadIcon,
  Notifications as NotificationsIcon,
  Visibility as VisibilityIcon,
} from '@mui/icons-material';
import Layout from '@/components/Layout/Layout';
import ProtectedRoute from '@/components/ProtectedRoute/ProtectedRoute';
import { useAppSelector, useAppDispatch } from '@/store/hooks';
import {
  fetchNotifications,
  markNotificationAsRead,
  markNotificationsAsRead,
  setFilters,
  clearError,
  addNotificationFromWebSocket,
} from '@/store/slices/notificationsSlice';
import { useSnackbar } from 'notistack';
import type { NotificationType, NotificationPriority } from '@/types';

function NotificationsContent() {
  const router = useRouter();
  const dispatch = useAppDispatch();
  const { enqueueSnackbar } = useSnackbar();
  const { notifications, loading, error, pagination, filters, unreadCount } = useAppSelector(
    (state) => state.notifications
  );
  const [filterDialogOpen, setFilterDialogOpen] = useState(false);
  const [selectedNotifications, setSelectedNotifications] = useState<string[]>([]);
  const [localFilters, setLocalFilters] = useState({
    type: '' as NotificationType | '',
    priority: '' as NotificationPriority | '',
    read: undefined as boolean | undefined,
  });

  useEffect(() => {
    dispatch(fetchNotifications());
  }, [dispatch]);

  useEffect(() => {
    // Подписываемся на WebSocket уведомления
    const handleWebSocketNotification = (message: any) => {
      if (message.type === 'event' && message.channel === 'notifications' && message.data) {
        dispatch(addNotificationFromWebSocket(message.data));
      }
    };
    // Это будет обрабатываться через useWebSocket hook
  }, [dispatch]);

  const handleFilter = () => {
    const filterParams: any = {};
    if (localFilters.type) filterParams.type = localFilters.type;
    if (localFilters.priority) filterParams.priority = localFilters.priority;
    if (localFilters.read !== undefined) filterParams.read = localFilters.read;

    dispatch(setFilters(filterParams));
    dispatch(fetchNotifications(filterParams));
    setFilterDialogOpen(false);
  };

  const handleMarkAsRead = async (id: string) => {
    try {
      await dispatch(markNotificationAsRead(id)).unwrap();
      enqueueSnackbar('Уведомление отмечено как прочитанное', { variant: 'success' });
    } catch (error: any) {
      enqueueSnackbar(error || 'Ошибка при отметке уведомления', { variant: 'error' });
    }
  };

  const handleMarkAllAsRead = async () => {
    if (selectedNotifications.length === 0) {
      enqueueSnackbar('Выберите уведомления для отметки', { variant: 'warning' });
      return;
    }
    try {
      await dispatch(markNotificationsAsRead(selectedNotifications)).unwrap();
      enqueueSnackbar('Уведомления отмечены как прочитанные', { variant: 'success' });
      setSelectedNotifications([]);
    } catch (error: any) {
      enqueueSnackbar(error || 'Ошибка при отметке уведомлений', { variant: 'error' });
    }
  };

  const formatDate = (timestamp: number): string => {
    return new Date(timestamp).toLocaleString('ru-RU', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
    });
  };

  const getPriorityColor = (priority: string): 'success' | 'error' | 'warning' | 'info' | 'default' => {
    switch (priority) {
      case 'URGENT':
        return 'error';
      case 'HIGH':
        return 'error';
      case 'NORMAL':
        return 'info';
      case 'LOW':
        return 'default';
      default:
        return 'default';
    }
  };

  const getTypeLabel = (type: string): string => {
    const typeLabels: Record<string, string> = {
      EVENT: 'Событие',
      ALERT: 'Предупреждение',
      INFO: 'Информация',
      WARNING: 'Внимание',
      ERROR: 'Ошибка',
      SYSTEM: 'Система',
      RECORDING: 'Запись',
      LICENSE: 'Лицензия',
      USER: 'Пользователь',
    };
    return typeLabels[type] || type;
  };

  const getPriorityLabel = (priority: string): string => {
    const priorityLabels: Record<string, string> = {
      URGENT: 'Срочно',
      HIGH: 'Высокий',
      NORMAL: 'Обычный',
      LOW: 'Низкий',
    };
    return priorityLabels[priority] || priority;
  };

  return (
    <Layout>
      <Box sx={{ mb: 3 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            <Badge badgeContent={unreadCount} color="error">
              <NotificationsIcon sx={{ fontSize: 32 }} />
            </Badge>
            <Typography variant="h4">Уведомления</Typography>
          </Box>
          <Box>
            <Tooltip title="Фильтры">
              <IconButton onClick={() => setFilterDialogOpen(true)} sx={{ mr: 1 }}>
                <FilterListIcon />
              </IconButton>
            </Tooltip>
            {selectedNotifications.length > 0 && (
              <Button
                startIcon={<MarkEmailReadIcon />}
                variant="contained"
                color="success"
                onClick={handleMarkAllAsRead}
              >
                Отметить как прочитанные ({selectedNotifications.length})
              </Button>
            )}
          </Box>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => dispatch(clearError())}>
          {error}
        </Alert>
      )}

      {loading && notifications.length === 0 ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
          <CircularProgress />
        </Box>
      ) : notifications.length === 0 ? (
        <Paper sx={{ p: 3, mt: 2 }}>
          <Typography variant="body1" color="text.secondary" align="center">
            Уведомления не найдены
          </Typography>
        </Paper>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell padding="checkbox">
                  <Checkbox
                    indeterminate={selectedNotifications.length > 0 && selectedNotifications.length < notifications.length}
                    checked={notifications.length > 0 && selectedNotifications.length === notifications.length}
                    onChange={(e) => {
                      if (e.target.checked) {
                        setSelectedNotifications(notifications.map((n) => n.id));
                      } else {
                        setSelectedNotifications([]);
                      }
                    }}
                  />
                </TableCell>
                <TableCell>Время</TableCell>
                <TableCell>Тип</TableCell>
                <TableCell>Приоритет</TableCell>
                <TableCell>Заголовок</TableCell>
                <TableCell>Сообщение</TableCell>
                <TableCell>Статус</TableCell>
                <TableCell>Действия</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {notifications.map((notification) => (
                <TableRow
                  key={notification.id}
                  hover
                  sx={{
                    cursor: 'pointer',
                    backgroundColor: notification.read ? 'transparent' : 'action.hover',
                  }}
                  onClick={() => {
                    if (!notification.read) {
                      handleMarkAsRead(notification.id);
                    }
                  }}
                >
                  <TableCell padding="checkbox" onClick={(e) => e.stopPropagation()}>
                    <Checkbox
                      checked={selectedNotifications.includes(notification.id)}
                      onChange={(e) => {
                        if (e.target.checked) {
                          setSelectedNotifications([...selectedNotifications, notification.id]);
                        } else {
                          setSelectedNotifications(selectedNotifications.filter((id) => id !== notification.id));
                        }
                      }}
                    />
                  </TableCell>
                  <TableCell>{formatDate(notification.timestamp)}</TableCell>
                  <TableCell>
                    <Chip label={getTypeLabel(notification.type)} size="small" variant="outlined" />
                  </TableCell>
                  <TableCell>
                    <Chip
                      label={getPriorityLabel(notification.priority)}
                      color={getPriorityColor(notification.priority)}
                      size="small"
                    />
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" fontWeight={notification.read ? 'normal' : 'bold'}>
                      {notification.title}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    <Typography variant="body2" color="text.secondary">
                      {notification.message}
                    </Typography>
                  </TableCell>
                  <TableCell>
                    {notification.read ? (
                      <Chip label="Прочитано" color="success" size="small" />
                    ) : (
                      <Chip label="Непрочитано" color="warning" size="small" />
                    )}
                  </TableCell>
                  <TableCell onClick={(e) => e.stopPropagation()}>
                    {!notification.read && (
                      <Tooltip title="Отметить как прочитанное">
                        <IconButton
                          size="small"
                          onClick={() => handleMarkAsRead(notification.id)}
                          color="success"
                        >
                          <CheckCircleIcon />
                        </IconButton>
                      </Tooltip>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      {/* Dialog для фильтров */}
      <Dialog open={filterDialogOpen} onClose={() => setFilterDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Фильтры уведомлений</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2, mt: 1 }}>
            <FormControl fullWidth>
              <InputLabel>Тип</InputLabel>
              <Select
                value={localFilters.type}
                onChange={(e) => setLocalFilters({ ...localFilters, type: e.target.value as NotificationType | '' })}
                label="Тип"
              >
                <MenuItem value="">Все</MenuItem>
                <MenuItem value="EVENT">Событие</MenuItem>
                <MenuItem value="ALERT">Предупреждение</MenuItem>
                <MenuItem value="INFO">Информация</MenuItem>
                <MenuItem value="WARNING">Внимание</MenuItem>
                <MenuItem value="ERROR">Ошибка</MenuItem>
                <MenuItem value="SYSTEM">Система</MenuItem>
                <MenuItem value="RECORDING">Запись</MenuItem>
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Приоритет</InputLabel>
              <Select
                value={localFilters.priority}
                onChange={(e) =>
                  setLocalFilters({ ...localFilters, priority: e.target.value as NotificationPriority | '' })
                }
                label="Приоритет"
              >
                <MenuItem value="">Все</MenuItem>
                <MenuItem value="URGENT">Срочно</MenuItem>
                <MenuItem value="HIGH">Высокий</MenuItem>
                <MenuItem value="NORMAL">Обычный</MenuItem>
                <MenuItem value="LOW">Низкий</MenuItem>
              </Select>
            </FormControl>
            <FormControl fullWidth>
              <InputLabel>Статус прочтения</InputLabel>
              <Select
                value={localFilters.read === undefined ? '' : String(localFilters.read)}
                onChange={(e) =>
                  setLocalFilters({
                    ...localFilters,
                    read: e.target.value === '' ? undefined : e.target.value === 'true',
                  })
                }
                label="Статус прочтения"
              >
                <MenuItem value="">Все</MenuItem>
                <MenuItem value="false">Непрочитанные</MenuItem>
                <MenuItem value="true">Прочитанные</MenuItem>
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setFilterDialogOpen(false)}>Отмена</Button>
          <Button onClick={handleFilter} variant="contained">
            Применить
          </Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
}

export default function NotificationsPage() {
  return (
    <ProtectedRoute>
      <NotificationsContent />
    </ProtectedRoute>
  );
}

