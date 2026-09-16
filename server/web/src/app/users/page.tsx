'use client';

import React, { useCallback, useEffect, useState } from 'react';
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
  TextField,
  MenuItem,
  FormControl,
  InputLabel,
  Select,
  Tooltip,
  TablePagination,
} from '@mui/material';
import {
  Add as AddIcon,
  Edit as EditIcon,
  Delete as DeleteIcon,
  Refresh as RefreshIcon,
} from '@mui/icons-material';
import Layout from '@/components/Layout/Layout';
import ProtectedRoute from '@/components/ProtectedRoute/ProtectedRoute';
import { useAppSelector } from '@/store/hooks';
import { useSnackbar } from 'notistack';
import { userService } from '@/services/userService';
import { UserRole } from '@/types';
import type { User } from '@/types';

function UsersContent() {
  const { enqueueSnackbar } = useSnackbar();
  const { user: currentUser } = useAppSelector((state) => state.auth);
  const [users, setUsers] = useState<User[]>([]);
  const [total, setTotal] = useState(0);
  /** Номер страницы API (с 1) */
  const [page, setPage] = useState(1);
  const [rowsPerPage, setRowsPerPage] = useState(20);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [dialogOpen, setDialogOpen] = useState(false);
  const [editingUser, setEditingUser] = useState<User | null>(null);
  const [formData, setFormData] = useState({
    username: '',
    email: '',
    password: '',
    role: UserRole.VIEWER as UserRole,
  });

  const isAdmin = currentUser?.role === 'ADMIN';

  const loadUsers = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const data = await userService.getUsers({ page, limit: rowsPerPage });
      setUsers(data.items);
      setTotal(data.total);
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Ошибка при загрузке пользователей';
      setError(message);
      enqueueSnackbar('Ошибка при загрузке пользователей', { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, [enqueueSnackbar, page, rowsPerPage]);

  useEffect(() => {
    if (isAdmin) {
      loadUsers();
    }
  }, [isAdmin, loadUsers]);

  const handleCreate = () => {
    setEditingUser(null);
    setFormData({
      username: '',
      email: '',
      password: '',
      role: UserRole.VIEWER,
    });
    setDialogOpen(true);
  };

  const handleEdit = (user: User) => {
    setEditingUser(user);
    setFormData({
      username: user.username,
      email: user.email ?? '',
      password: '',
      role: user.role,
    });
    setDialogOpen(true);
  };

  const handleSave = async () => {
    try {
      if (editingUser) {
        await userService.updateUser(editingUser.id, {
          username: formData.username,
          email: formData.email,
          role: formData.role,
          ...(formData.password && { password: formData.password }),
        });
        enqueueSnackbar('Пользователь обновлен', { variant: 'success' });
      } else {
        await userService.createUser({
          username: formData.username,
          email: formData.email,
          password: formData.password,
          role: formData.role,
        });
        enqueueSnackbar('Пользователь создан', { variant: 'success' });
      }
      setDialogOpen(false);
      await loadUsers();
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Ошибка при сохранении пользователя';
      enqueueSnackbar(message, { variant: 'error' });
    }
  };

  const handleDelete = async (userId: string) => {
    if (!confirm('Вы уверены, что хотите удалить этого пользователя?')) return;
    try {
      await userService.deleteUser(userId);
      enqueueSnackbar('Пользователь удален', { variant: 'success' });
      if (users.length === 1 && page > 1) {
        setPage((p) => p - 1);
      } else {
        await loadUsers();
      }
    } catch (err: unknown) {
      const message = err instanceof Error ? err.message : 'Ошибка при удалении пользователя';
      enqueueSnackbar(message, { variant: 'error' });
    }
  };

  const getRoleColor = (role: UserRole): 'default' | 'primary' | 'secondary' | 'error' | 'info' | 'success' | 'warning' => {
    switch (role) {
      case 'ADMIN':
        return 'error';
      case 'OPERATOR':
        return 'warning';
      case 'VIEWER':
        return 'info';
      default:
        return 'default';
    }
  };

  const getRoleLabel = (role: UserRole): string => {
    switch (role) {
      case 'ADMIN':
        return 'Администратор';
      case 'OPERATOR':
        return 'Оператор';
      case 'VIEWER':
        return 'Просмотр';
      case 'GUEST':
        return 'Гость';
      default:
        return role;
    }
  };

  if (!isAdmin) {
    return (
      <Layout>
        <Alert severity="error">У вас нет прав для просмотра этой страницы</Alert>
      </Layout>
    );
  }

  return (
    <Layout>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Пользователи</Typography>
        <Box>
          <Button startIcon={<RefreshIcon />} onClick={loadUsers} sx={{ mr: 1 }}>
            Обновить
          </Button>
          <Button variant="contained" startIcon={<AddIcon />} onClick={handleCreate}>
            Добавить пользователя
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
          <CircularProgress />
        </Box>
      ) : (
        <TableContainer component={Paper}>
          <Table>
            <TableHead>
              <TableRow>
                <TableCell>Имя пользователя</TableCell>
                <TableCell>Email</TableCell>
                <TableCell>Роль</TableCell>
                <TableCell>Создан</TableCell>
                <TableCell align="right">Действия</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {users.map((user) => (
                <TableRow key={user.id} hover>
                  <TableCell>{user.username}</TableCell>
                  <TableCell>{user.email}</TableCell>
                  <TableCell>
                    <Chip label={getRoleLabel(user.role)} color={getRoleColor(user.role)} size="small" />
                  </TableCell>
                  <TableCell>
                    {new Date(user.createdAt || Date.now()).toLocaleDateString('ru-RU')}
                  </TableCell>
                  <TableCell align="right">
                    <Tooltip title="Редактировать">
                      <IconButton size="small" onClick={() => handleEdit(user)}>
                        <EditIcon />
                      </IconButton>
                    </Tooltip>
                    {user.id !== currentUser?.id && (
                      <Tooltip title="Удалить">
                        <IconButton size="small" color="error" onClick={() => handleDelete(user.id)}>
                          <DeleteIcon />
                        </IconButton>
                      </Tooltip>
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
          <TablePagination
            component="div"
            count={total}
            page={page - 1}
            onPageChange={(_, newPage) => setPage(newPage + 1)}
            rowsPerPage={rowsPerPage}
            onRowsPerPageChange={(e) => {
              setRowsPerPage(parseInt(e.target.value, 10));
              setPage(1);
            }}
            rowsPerPageOptions={[10, 20, 50, 100]}
            labelRowsPerPage="Строк на странице:"
            labelDisplayedRows={({ from, to, count }) =>
              `${from}–${to} из ${count !== -1 ? count : `более чем ${to}`}`
            }
          />
        </TableContainer>
      )}

      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editingUser ? 'Редактировать пользователя' : 'Добавить пользователя'}</DialogTitle>
        <DialogContent>
          <Box sx={{ pt: 2, display: 'flex', flexDirection: 'column', gap: 2 }}>
            <TextField
              fullWidth
              label="Имя пользователя"
              value={formData.username}
              onChange={(e) => setFormData({ ...formData, username: e.target.value })}
              required
            />
            <TextField
              fullWidth
              label="Email"
              type="email"
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              required
            />
            <TextField
              fullWidth
              label={editingUser ? 'Новый пароль (оставьте пустым, чтобы не изменять)' : 'Пароль'}
              type="password"
              value={formData.password}
              onChange={(e) => setFormData({ ...formData, password: e.target.value })}
              required={!editingUser}
            />
            <FormControl fullWidth>
              <InputLabel>Роль</InputLabel>
              <Select
                value={formData.role}
                onChange={(e) => setFormData({ ...formData, role: e.target.value as UserRole })}
                label="Роль"
              >
                <MenuItem value="VIEWER">Просмотр</MenuItem>
                <MenuItem value="OPERATOR">Оператор</MenuItem>
                <MenuItem value="ADMIN">Администратор</MenuItem>
              </Select>
            </FormControl>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDialogOpen(false)}>Отмена</Button>
          <Button
            onClick={handleSave}
            variant="contained"
            disabled={!formData.username || !formData.email || (!editingUser && !formData.password)}
          >
            Сохранить
          </Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
}

export default function UsersPage() {
  return (
    <ProtectedRoute>
      <UsersContent />
    </ProtectedRoute>
  );
}



