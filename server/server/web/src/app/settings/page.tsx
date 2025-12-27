'use client';

import React, { useEffect, useState } from 'react';
import {
  Box,
  Typography,
  Paper,
  Grid,
  TextField,
  Button,
  CircularProgress,
  Alert,
  Tabs,
  Tab,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  List,
  ListItem,
  ListItemText,
  ListItemSecondaryAction,
  IconButton,
  Divider,
  Switch,
  FormControlLabel,
} from '@mui/material';
import {
  Save as SaveIcon,
  Refresh as RefreshIcon,
  Upload as UploadIcon,
  Download as DownloadIcon,
  Delete as DeleteIcon,
} from '@mui/icons-material';
import Layout from '@/components/Layout/Layout';
import ProtectedRoute from '@/components/ProtectedRoute/ProtectedRoute';
import { useAppSelector, useAppDispatch } from '@/store/hooks';
import {
  fetchSettings,
  fetchSystemSettings,
  updateSettings,
  updateSetting,
  deleteSetting,
  exportSettings,
  importSettings,
  resetSettings,
  clearError,
} from '@/store/slices/settingsSlice';
import { useSnackbar } from 'notistack';

interface TabPanelProps {
  children?: React.ReactNode;
  index: number;
  value: number;
}

function TabPanel(props: TabPanelProps) {
  const { children, value, index, ...other } = props;
  return (
    <div
      role="tabpanel"
      hidden={value !== index}
      id={`settings-tabpanel-${index}`}
      aria-labelledby={`settings-tab-${index}`}
      {...other}
    >
      {value === index && <Box sx={{ p: 3 }}>{children}</Box>}
    </div>
  );
}

function SettingsContent() {
  const dispatch = useAppDispatch();
  const { enqueueSnackbar } = useSnackbar();
  const { settings, systemSettings, loading, error } = useAppSelector((state) => state.settings);
  const { user } = useAppSelector((state) => state.auth);
  const [tabValue, setTabValue] = useState(0);
  const [localSettings, setLocalSettings] = useState<Record<string, string>>({});
  const [importDialogOpen, setImportDialogOpen] = useState(false);
  const [importData, setImportData] = useState('');

  const isAdmin = user?.role === 'ADMIN';

  useEffect(() => {
    dispatch(fetchSettings());
    dispatch(fetchSystemSettings());
  }, [dispatch]);

  useEffect(() => {
    const settingsMap: Record<string, string> = {};
    settings.forEach((setting) => {
      settingsMap[setting.key] = setting.value;
    });
    setLocalSettings(settingsMap);
  }, [settings]);

  const handleSave = async () => {
    try {
      await dispatch(updateSettings(localSettings)).unwrap();
      enqueueSnackbar('Настройки сохранены', { variant: 'success' });
    } catch (error: any) {
      enqueueSnackbar(error || 'Ошибка при сохранении настроек', { variant: 'error' });
    }
  };

  const handleSettingChange = (key: string, value: string) => {
    setLocalSettings({ ...localSettings, [key]: value });
  };

  const handleExport = async () => {
    try {
      const exported = await dispatch(exportSettings()).unwrap();
      const dataStr = JSON.stringify(exported, null, 2);
      const dataBlob = new Blob([dataStr], { type: 'application/json' });
      const url = URL.createObjectURL(dataBlob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `settings-${new Date().toISOString()}.json`;
      link.click();
      URL.revokeObjectURL(url);
      enqueueSnackbar('Настройки экспортированы', { variant: 'success' });
    } catch (error: any) {
      enqueueSnackbar(error || 'Ошибка при экспорте настроек', { variant: 'error' });
    }
  };

  const handleImport = async () => {
    try {
      const parsed = JSON.parse(importData);
      await dispatch(importSettings(parsed)).unwrap();
      setImportDialogOpen(false);
      setImportData('');
      enqueueSnackbar('Настройки импортированы', { variant: 'success' });
    } catch (error: any) {
      enqueueSnackbar(error?.message || 'Ошибка при импорте настроек', { variant: 'error' });
    }
  };

  const handleReset = async (category?: string) => {
    if (!confirm('Вы уверены, что хотите сбросить настройки?')) return;
    try {
      await dispatch(resetSettings(category)).unwrap();
      enqueueSnackbar('Настройки сброшены', { variant: 'success' });
    } catch (error: any) {
      enqueueSnackbar(error || 'Ошибка при сбросе настроек', { variant: 'error' });
    }
  };

  const handleDelete = async (key: string) => {
    if (!confirm('Вы уверены, что хотите удалить эту настройку?')) return;
    try {
      await dispatch(deleteSetting(key)).unwrap();
      enqueueSnackbar('Настройка удалена', { variant: 'success' });
    } catch (error: any) {
      enqueueSnackbar(error || 'Ошибка при удалении настройки', { variant: 'error' });
    }
  };

  const settingsByCategory = settings.reduce((acc, setting) => {
    if (!acc[setting.category]) {
      acc[setting.category] = [];
    }
    acc[setting.category].push(setting);
    return acc;
  }, {} as Record<string, typeof settings>);

  const categories = Object.keys(settingsByCategory);

  return (
    <Layout>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 3 }}>
        <Typography variant="h4">Настройки</Typography>
        <Box>
          {isAdmin && (
            <>
              <Button
                variant="outlined"
                startIcon={<DownloadIcon />}
                onClick={handleExport}
                sx={{ mr: 1 }}
              >
                Экспорт
              </Button>
              <Button
                variant="outlined"
                startIcon={<UploadIcon />}
                onClick={() => setImportDialogOpen(true)}
                sx={{ mr: 1 }}
              >
                Импорт
              </Button>
              <Button
                variant="outlined"
                startIcon={<RefreshIcon />}
                onClick={() => handleReset()}
                sx={{ mr: 1 }}
                color="warning"
              >
                Сбросить
              </Button>
            </>
          )}
          <Button variant="contained" startIcon={<SaveIcon />} onClick={handleSave} disabled={!isAdmin}>
            Сохранить
          </Button>
        </Box>
      </Box>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => dispatch(clearError())}>
          {error}
        </Alert>
      )}

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
          <CircularProgress />
        </Box>
      ) : (
        <Paper>
          <Tabs value={tabValue} onChange={(e, newValue) => setTabValue(newValue)}>
            {categories.map((category) => (
              <Tab key={category} label={category} />
            ))}
            <Tab label="Системные настройки" />
          </Tabs>

          {categories.map((category, index) => (
            <TabPanel key={category} value={tabValue} index={index}>
              <Grid container spacing={3}>
                {settingsByCategory[category].map((setting) => (
                  <Grid item xs={12} md={6} key={setting.key}>
                    {setting.type === 'BOOLEAN' ? (
                      <FormControlLabel
                        control={
                          <Switch
                            checked={localSettings[setting.key] === 'true'}
                            onChange={(e) =>
                              handleSettingChange(setting.key, String(e.target.checked))
                            }
                            disabled={!isAdmin}
                          />
                        }
                        label={
                          <Box>
                            <Typography variant="body1">{setting.key}</Typography>
                            {setting.description && (
                              <Typography variant="caption" color="text.secondary">
                                {setting.description}
                              </Typography>
                            )}
                          </Box>
                        }
                      />
                    ) : (
                      <TextField
                        fullWidth
                        label={setting.key}
                        value={localSettings[setting.key] || ''}
                        onChange={(e) => handleSettingChange(setting.key, e.target.value)}
                        disabled={!isAdmin}
                        helperText={setting.description}
                        type={setting.type === 'NUMBER' ? 'number' : 'text'}
                      />
                    )}
                    {isAdmin && (
                      <IconButton
                        size="small"
                        onClick={() => handleDelete(setting.key)}
                        color="error"
                        sx={{ ml: 1 }}
                      >
                        <DeleteIcon />
                      </IconButton>
                    )}
                  </Grid>
                ))}
              </Grid>
            </TabPanel>
          ))}

          <TabPanel value={tabValue} index={categories.length}>
            {systemSettings ? (
              <Grid container spacing={3}>
                <Grid item xs={12}>
                  <Typography variant="h6" gutterBottom>
                    Настройки записи
                  </Typography>
                  <Typography variant="body2" color="text.secondary">
                    Эти настройки отображаются только для просмотра
                  </Typography>
                </Grid>
              </Grid>
            ) : (
              <Typography variant="body1" color="text.secondary">
                Системные настройки не настроены
              </Typography>
            )}
          </TabPanel>
        </Paper>
      )}

      <Dialog open={importDialogOpen} onClose={() => setImportDialogOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>Импорт настроек</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            multiline
            rows={10}
            value={importData}
            onChange={(e) => setImportData(e.target.value)}
            placeholder="Вставьте JSON с настройками"
            sx={{ mt: 2 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setImportDialogOpen(false)}>Отмена</Button>
          <Button onClick={handleImport} variant="contained">
            Импортировать
          </Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
}

export default function SettingsPage() {
  return (
    <ProtectedRoute>
      <SettingsContent />
    </ProtectedRoute>
  );
}
