'use client';

import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react';
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControlLabel,
  MenuItem,
  Paper,
  Select,
  Checkbox,
  Switch,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  TextField,
  Typography,
} from '@mui/material';
import Layout from '@/components/Layout/Layout';
import ProtectedRoute from '@/components/ProtectedRoute/ProtectedRoute';
import { useAppDispatch, useAppSelector } from '@/store/hooks';
import { useSnackbar } from 'notistack';
import { clearRealtimeEvents } from '@/store/slices/analyticsSlice';
import {
  analyticsRuleService,
  type AnalyticsRuleUpsertPayload,
  type AnalyticsRuleListItem,
} from '@/services/analyticsRuleService';

const RULES_UI_PREFS_KEY = 'analyticsRules.uiPrefs.v1';

function AnalyticsRulesPageContent() {
  const dispatch = useAppDispatch();
  const { enqueueSnackbar } = useSnackbar();
  const enqueueSnackbarRef = useRef(enqueueSnackbar);
  enqueueSnackbarRef.current = enqueueSnackbar;
  const { user } = useAppSelector((state) => state.auth);
  const analyticsEvents = useAppSelector((state) => state.analytics.recentEvents);
  const [rules, setRules] = useState<AnalyticsRuleListItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [actionRuleId, setActionRuleId] = useState<string | null>(null);

  const [targetUserId, setTargetUserId] = useState('');
  const [targetCameraId, setTargetCameraId] = useState('');
  const [notifyInApp, setNotifyInApp] = useState(true);
  const [notifyEmail, setNotifyEmail] = useState(false);
  const [notifyTelegram, setNotifyTelegram] = useState(false);
  const [searchQuery, setSearchQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState<'all' | 'enabled' | 'disabled'>('all');
  const [showOnlyProblematic, setShowOnlyProblematic] = useState(false);
  const [sortBy, setSortBy] = useState<'name' | 'enabled' | 'problematic'>('name');
  const [editorOpen, setEditorOpen] = useState(false);
  const [savingEditor, setSavingEditor] = useState(false);
  const [editingRuleId, setEditingRuleId] = useState<string | null>(null);
  const [formName, setFormName] = useState('');
  const [formDescription, setFormDescription] = useState('');
  const [formCameraId, setFormCameraId] = useState('');
  const [formAnalyticsType, setFormAnalyticsType] = useState('MOTION_DETECTION');
  const [formEnabled, setFormEnabled] = useState(true);
  const [formSendNotification, setFormSendNotification] = useState(true);
  const [formNotifyInApp, setFormNotifyInApp] = useState(true);
  const [formNotifyEmail, setFormNotifyEmail] = useState(false);
  const [formNotifyTelegram, setFormNotifyTelegram] = useState(false);
  const [formNotifySms, setFormNotifySms] = useState(false);
  const [formNotifyWebhook, setFormNotifyWebhook] = useState(false);
  const [formSendWebhook, setFormSendWebhook] = useState(false);
  const [formWebhookUrl, setFormWebhookUrl] = useState('');
  const [formMinConfidence, setFormMinConfidence] = useState('0.5');
  const [formMinObjectCount, setFormMinObjectCount] = useState('1');
  const [formZones, setFormZones] = useState('');
  const [formObjectTypes, setFormObjectTypes] = useState('');
  const [formTimeWindow, setFormTimeWindow] = useState('');
  const [formDaysOfWeek, setFormDaysOfWeek] = useState<number[]>([]);
  const [livePaused, setLivePaused] = useState(false);
  const [pausedEventsSnapshot, setPausedEventsSnapshot] = useState<typeof analyticsEvents>([]);
  const [liveCameraFilter, setLiveCameraFilter] = useState('');
  const [liveTypeFilter, setLiveTypeFilter] = useState('');
  const [liveVisibleCount, setLiveVisibleCount] = useState(8);

  const conditionPreview = useMemo(() => {
    const dayLabelMap: Record<number, string> = {
      1: 'Mon',
      2: 'Tue',
      3: 'Wed',
      4: 'Thu',
      5: 'Fri',
      6: 'Sat',
      7: 'Sun',
    };
    const daysText =
      formDaysOfWeek.length === 0
        ? 'every day'
        : formDaysOfWeek
            .slice()
            .sort((a, b) => a - b)
            .map((d) => dayLabelMap[d] ?? String(d))
            .join(', ');
    const timeText = formTimeWindow.trim() ? formTimeWindow.trim() : 'all day';
    return `${daysText}, ${timeText}`;
  }, [formDaysOfWeek, formTimeWindow]);
  const hasNotificationChannelSelected = formNotifyInApp || formNotifyEmail || formNotifyTelegram || formNotifySms;
  const hasAnyActionSelected = formSendNotification || formNotifyWebhook;

  const canOperate = user?.role === 'ADMIN' || user?.role === 'OPERATOR';

  const sortedRules = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    const filtered = [...rules]
      .filter((rule) => {
        if (statusFilter === 'enabled' && !rule.enabled) return false;
        if (statusFilter === 'disabled' && rule.enabled) return false;
        if (showOnlyProblematic) {
          const hasNotificationAction = Boolean(rule.actions?.sendNotification);
          const hasNotificationChannels =
            Boolean(rule.actions?.notifyInApp) ||
            Boolean(rule.actions?.notifyEmail) ||
            Boolean(rule.actions?.notifyTelegram);
          const hasWebhookAction = Boolean(rule.actions?.sendWebhook);
          const hasAnyAction = hasNotificationAction || hasWebhookAction;
          const hasRiskNoAction = !hasAnyAction;
          const hasRiskNotificationNoChannels = hasNotificationAction && !hasNotificationChannels;
          if (!hasRiskNoAction && !hasRiskNotificationNoChannels) return false;
        }
        if (!q) return true;
        return (
          rule.name.toLowerCase().includes(q) ||
          rule.id.toLowerCase().includes(q) ||
          (rule.analyticsType ?? '').toLowerCase().includes(q) ||
          (rule.cameraId ?? '').toLowerCase().includes(q)
        );
      });

    const isProblematic = (rule: AnalyticsRuleListItem): boolean => {
      const hasNotificationAction = Boolean(rule.actions?.sendNotification);
      const hasNotificationChannels =
        Boolean(rule.actions?.notifyInApp) ||
        Boolean(rule.actions?.notifyEmail) ||
        Boolean(rule.actions?.notifyTelegram);
      const hasWebhookAction = Boolean(rule.actions?.sendWebhook);
      const hasAnyAction = hasNotificationAction || hasWebhookAction;
      return !hasAnyAction || (hasNotificationAction && !hasNotificationChannels);
    };

    if (sortBy === 'enabled') {
      return filtered.sort((a, b) => {
        if (a.enabled !== b.enabled) return a.enabled ? -1 : 1;
        return a.name.localeCompare(b.name);
      });
    }
    if (sortBy === 'problematic') {
      return filtered.sort((a, b) => {
        const pa = isProblematic(a);
        const pb = isProblematic(b);
        if (pa !== pb) return pa ? -1 : 1;
        if (a.enabled !== b.enabled) return a.enabled ? -1 : 1;
        return a.name.localeCompare(b.name);
      });
    }
    return filtered.sort((a, b) => a.name.localeCompare(b.name));
  }, [rules, searchQuery, statusFilter, showOnlyProblematic, sortBy]);

  const stats = useMemo(() => {
    const total = rules.length;
    const enabled = rules.filter((r) => r.enabled).length;
    const problematic = rules.filter((rule) => {
      const hasNotificationAction = Boolean(rule.actions?.sendNotification);
      const hasNotificationChannels =
        Boolean(rule.actions?.notifyInApp) ||
        Boolean(rule.actions?.notifyEmail) ||
        Boolean(rule.actions?.notifyTelegram);
      const hasWebhookAction = Boolean(rule.actions?.sendWebhook);
      const hasAnyAction = hasNotificationAction || hasWebhookAction;
      const hasRiskNoAction = !hasAnyAction;
      const hasRiskNotificationNoChannels = hasNotificationAction && !hasNotificationChannels;
      return hasRiskNoAction || hasRiskNotificationNoChannels;
    }).length;
    return { total, enabled, problematic };
  }, [rules]);

  const liveEventsToRender = useMemo(() => {
    const source = livePaused ? pausedEventsSnapshot : analyticsEvents;
    return source
      .filter((event) => (liveCameraFilter ? event.cameraId === liveCameraFilter : true))
      .filter((event) => (liveTypeFilter ? event.resultType === liveTypeFilter : true))
      .slice(0, liveVisibleCount);
  }, [
    analyticsEvents,
    liveCameraFilter,
    livePaused,
    liveTypeFilter,
    liveVisibleCount,
    pausedEventsSnapshot,
  ]);

  const liveCameraOptions = useMemo(
    () => Array.from(new Set(analyticsEvents.map((event) => event.cameraId))),
    [analyticsEvents]
  );
  const liveTypeOptions = useMemo(
    () => Array.from(new Set(analyticsEvents.map((event) => event.resultType))),
    [analyticsEvents]
  );
  const liveKpi = useMemo(() => {
    const now = Date.now();
    const lastMinute = analyticsEvents.filter((event) => now - event.timestamp <= 60_000);
    const perCamera = new Map<string, number>();
    for (const event of lastMinute) {
      perCamera.set(event.cameraId, (perCamera.get(event.cameraId) ?? 0) + 1);
    }
    const topCameraEntry = Array.from(perCamera.entries()).sort((a, b) => b[1] - a[1])[0];
    return {
      eventsPerMinute: lastMinute.length,
      activeCameraCount: perCamera.size,
      topCameraId: topCameraEntry?.[0] ?? '-',
      topCameraEvents: topCameraEntry?.[1] ?? 0,
    };
  }, [analyticsEvents]);

  const loadRules = useCallback(async () => {
    try {
      setLoading(true);
      const data = await analyticsRuleService.getRules();
      setRules(data);
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Ошибка загрузки правил аналитики';
      enqueueSnackbarRef.current(message, { variant: 'error' });
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadRules();
  }, [loadRules]);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    const raw = window.localStorage.getItem(RULES_UI_PREFS_KEY);
    if (!raw) return;
    try {
      const prefs = JSON.parse(raw) as {
        searchQuery?: string;
        statusFilter?: 'all' | 'enabled' | 'disabled';
        showOnlyProblematic?: boolean;
        sortBy?: 'name' | 'enabled' | 'problematic';
      };
      if (typeof prefs.searchQuery === 'string') setSearchQuery(prefs.searchQuery);
      if (prefs.statusFilter && ['all', 'enabled', 'disabled'].includes(prefs.statusFilter)) {
        setStatusFilter(prefs.statusFilter);
      }
      if (typeof prefs.showOnlyProblematic === 'boolean') {
        setShowOnlyProblematic(prefs.showOnlyProblematic);
      }
      if (prefs.sortBy && ['name', 'enabled', 'problematic'].includes(prefs.sortBy)) {
        setSortBy(prefs.sortBy);
      }
    } catch {
      // Ignore malformed localStorage data.
    }
  }, []);

  useEffect(() => {
    if (typeof window === 'undefined') return;
    const prefs = {
      searchQuery,
      statusFilter,
      showOnlyProblematic,
      sortBy,
    };
    window.localStorage.setItem(RULES_UI_PREFS_KEY, JSON.stringify(prefs));
  }, [searchQuery, showOnlyProblematic, sortBy, statusFilter]);

  useEffect(() => {
    if (livePaused) {
      setPausedEventsSnapshot(analyticsEvents);
    }
  }, [analyticsEvents, livePaused]);

  const openCreateDialog = () => {
    setEditingRuleId(null);
    setFormName('');
    setFormDescription('');
    setFormCameraId('');
    setFormAnalyticsType('MOTION_DETECTION');
    setFormEnabled(true);
    setFormSendNotification(true);
    setFormNotifyInApp(true);
    setFormNotifyEmail(false);
    setFormNotifyTelegram(false);
    setFormNotifySms(false);
    setFormNotifyWebhook(false);
    setFormWebhookUrl('');
    setFormMinConfidence('0.5');
    setFormMinObjectCount('1');
    setFormZones('');
    setFormObjectTypes('');
    setFormTimeWindow('');
    setFormDaysOfWeek([]);
    setEditorOpen(true);
  };

  const openEditDialog = (rule: AnalyticsRuleListItem) => {
    setEditingRuleId(rule.id);
    setFormName(rule.name);
    setFormDescription(rule.description ?? '');
    setFormCameraId(rule.cameraId ?? '');
    setFormAnalyticsType(rule.analyticsType ?? 'MOTION_DETECTION');
    setFormEnabled(rule.enabled);
    setFormSendNotification(Boolean(rule.actions?.sendNotification));
    setFormNotifyInApp(rule.actions?.notifyInApp ?? true);
    setFormNotifyEmail(Boolean(rule.actions?.notifyEmail));
    setFormNotifyTelegram(Boolean(rule.actions?.notifyTelegram));
    setFormNotifySms(Boolean(rule.actions?.additionalActions?.__notifySms));
    setFormNotifyWebhook(Boolean(rule.actions?.sendWebhook));
    setFormWebhookUrl(rule.actions?.webhookUrl ?? '');
    setFormMinConfidence(String(rule.conditions?.minConfidence ?? 0.5));
    setFormMinObjectCount(String(rule.conditions?.minObjectCount ?? 1));
    setFormZones((rule.conditions?.zones ?? []).join(', '));
    setFormObjectTypes((rule.conditions?.objectTypes ?? []).join(', '));
    setFormTimeWindow(rule.conditions?.timeWindow ?? '');
    setFormDaysOfWeek(rule.conditions?.daysOfWeek ?? []);
    setEditorOpen(true);
  };

  const buildUpsertPayload = (): AnalyticsRuleUpsertPayload => ({
    name: formName.trim(),
    description: formDescription.trim() || undefined,
    cameraId: formCameraId.trim() || undefined,
    analyticsType: formAnalyticsType,
    enabled: formEnabled,
    priority: 0,
    conditions: {
      minConfidence: Number(formMinConfidence),
      objectTypes: formObjectTypes
        .split(',')
        .map((it) => it.trim())
        .filter(Boolean),
      zones: formZones
        .split(',')
        .map((it) => it.trim())
        .filter(Boolean),
      timeWindow: formTimeWindow.trim() || null,
      daysOfWeek: formDaysOfWeek,
      minObjectCount: Number(formMinObjectCount),
      maxObjectCount: null,
      additionalConditions: {},
    },
    actions: {
      createEvent: false,
      eventType: formAnalyticsType,
      eventSeverity: 'INFO',
      sendNotification: formSendNotification,
      notifyInApp: formNotifyInApp,
      notifyEmail: formNotifyEmail,
      notifyTelegram: formNotifyTelegram,
      notificationType: 'INFO',
      startRecording: false,
      recordingDuration: 60,
      sendWebhook: formNotifyWebhook,
      webhookUrl: formNotifyWebhook ? formWebhookUrl.trim() || null : null,
      additionalActions: {
        __notifySms: formNotifySms,
        __notifyWebhook: formNotifyWebhook,
      },
    },
  });

  const handleToggleRule = async (rule: AnalyticsRuleListItem) => {
    if (!canOperate) return;
    try {
      setActionRuleId(rule.id);
      if (rule.enabled) {
        await analyticsRuleService.disableRule(rule.id);
        enqueueSnackbar(`Правило "${rule.name}" отключено`, { variant: 'success' });
      } else {
        await analyticsRuleService.enableRule(rule.id);
        enqueueSnackbar(`Правило "${rule.name}" включено`, { variant: 'success' });
      }
      await loadRules();
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Ошибка изменения состояния правила';
      enqueueSnackbar(message, { variant: 'error' });
    } finally {
      setActionRuleId(null);
    }
  };

  const handleTestSend = async (rule: AnalyticsRuleListItem) => {
    if (!canOperate) return;
    if (!notifyInApp && !notifyEmail && !notifyTelegram) {
      enqueueSnackbar('Выберите хотя бы один канал уведомления', { variant: 'warning' });
      return;
    }
    try {
      setActionRuleId(rule.id);
      const result = await analyticsRuleService.sendTestNotification(rule.id, {
        userId: targetUserId.trim() || undefined,
        cameraId: targetCameraId.trim() || undefined,
        notifyInApp,
        notifyEmail,
        notifyTelegram,
      });
      enqueueSnackbar(
        `Тест отправлен для "${rule.name}": ${result.notificationId} (${result.channels.join(', ')})`,
        { variant: 'success' }
      );
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Ошибка тестовой отправки';
      enqueueSnackbar(message, { variant: 'error' });
    } finally {
      setActionRuleId(null);
    }
  };

  const handleDeleteRule = async (rule: AnalyticsRuleListItem) => {
    if (!canOperate) return;
    const confirmed = window.confirm(`Удалить правило "${rule.name}"?`);
    if (!confirmed) return;
    try {
      setActionRuleId(rule.id);
      await analyticsRuleService.deleteRule(rule.id);
      enqueueSnackbar(`Правило "${rule.name}" удалено`, { variant: 'success' });
      await loadRules();
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Ошибка удаления правила';
      enqueueSnackbar(message, { variant: 'error' });
    } finally {
      setActionRuleId(null);
    }
  };

  const handleSaveEditor = async () => {
    if (!formName.trim()) {
      enqueueSnackbar('Название правила обязательно', { variant: 'warning' });
      return;
    }
    if (formNotifyWebhook && !formWebhookUrl.trim()) {
      enqueueSnackbar('Webhook URL обязателен при включенном webhook', { variant: 'warning' });
      return;
    }
    if (formSendNotification && !hasNotificationChannelSelected) {
      enqueueSnackbar('Включите хотя бы один канал уведомления (in-app/email/telegram/sms)', {
        variant: 'warning',
      });
      return;
    }
    if (!hasAnyActionSelected) {
      enqueueSnackbar('Правило не выполняет действий: включите уведомления и/или webhook', {
        variant: 'warning',
      });
      return;
    }
    const minConfidence = Number(formMinConfidence);
    if (Number.isNaN(minConfidence) || minConfidence < 0 || minConfidence > 1) {
      enqueueSnackbar('Min confidence должен быть в диапазоне 0..1', { variant: 'warning' });
      return;
    }
    const minObjectCount = Number(formMinObjectCount);
    if (Number.isNaN(minObjectCount) || minObjectCount < 0) {
      enqueueSnackbar('Min object count должен быть >= 0', { variant: 'warning' });
      return;
    }
    if (formTimeWindow.trim()) {
      const timeWindowPattern = /^([01]\d|2[0-3]):[0-5]\d-([01]\d|2[0-3]):[0-5]\d$/;
      if (!timeWindowPattern.test(formTimeWindow.trim())) {
        enqueueSnackbar('Time window должен быть в формате HH:mm-HH:mm', { variant: 'warning' });
        return;
      }
    }
    try {
      setSavingEditor(true);
      const payload = buildUpsertPayload();
      if (editingRuleId) {
        await analyticsRuleService.updateRule(editingRuleId, payload);
        enqueueSnackbar('Правило обновлено', { variant: 'success' });
      } else {
        await analyticsRuleService.createRule(payload);
        enqueueSnackbar('Правило создано', { variant: 'success' });
      }
      setEditorOpen(false);
      await loadRules();
    } catch (error: unknown) {
      const message = error instanceof Error ? error.message : 'Ошибка сохранения правила';
      enqueueSnackbar(message, { variant: 'error' });
    } finally {
      setSavingEditor(false);
    }
  };

  const applyConditionPreset = (preset: 'business-hours' | 'night' | 'weekends' | 'always') => {
    switch (preset) {
      case 'business-hours':
        setFormTimeWindow('09:00-18:00');
        setFormDaysOfWeek([1, 2, 3, 4, 5]);
        break;
      case 'night':
        setFormTimeWindow('22:00-06:00');
        setFormDaysOfWeek([]);
        break;
      case 'weekends':
        setFormTimeWindow('');
        setFormDaysOfWeek([6, 7]);
        break;
      case 'always':
        setFormTimeWindow('');
        setFormDaysOfWeek([]);
        break;
    }
  };

  const handleResetFilters = () => {
    setSearchQuery('');
    setStatusFilter('all');
    setShowOnlyProblematic(false);
    setSortBy('name');
    if (typeof window !== 'undefined') {
      window.localStorage.removeItem(RULES_UI_PREFS_KEY);
    }
    enqueueSnackbar('Фильтры сброшены', { variant: 'info' });
  };

  return (
    <Layout>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h4">Правила аналитики</Typography>
        <Box sx={{ display: 'flex', gap: 1 }}>
          <Button variant="outlined" onClick={() => void loadRules()} disabled={loading}>
            Обновить
          </Button>
          <Button variant="text" onClick={handleResetFilters} disabled={loading}>
            Сбросить фильтры
          </Button>
          <Button variant="contained" onClick={openCreateDialog} disabled={!canOperate || loading}>
            Создать правило
          </Button>
        </Box>
      </Box>

      <Paper sx={{ p: 2, mb: 2 }}>
        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 2 }}>
          <Chip size="small" label={`Total: ${stats.total}`} />
          <Chip size="small" color="success" label={`Enabled: ${stats.enabled}`} />
          <Chip size="small" color={stats.problematic > 0 ? 'warning' : 'default'} label={`Problematic: ${stats.problematic}`} />
          <Chip size="small" variant="outlined" label={`Visible: ${sortedRules.length}`} />
        </Box>
        <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', md: '2fr 1fr' } }}>
          <TextField
            label="Поиск по названию, ID, типу, cameraId"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            fullWidth
          />
          <Select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value as 'all' | 'enabled' | 'disabled')}
            fullWidth
            size="small"
          >
            <MenuItem value="all">Все статусы</MenuItem>
            <MenuItem value="enabled">Только enabled</MenuItem>
            <MenuItem value="disabled">Только disabled</MenuItem>
          </Select>
        </Box>
        <Box sx={{ mt: 1, maxWidth: 320 }}>
          <Select
            value={sortBy}
            onChange={(e) => setSortBy(e.target.value as 'name' | 'enabled' | 'problematic')}
            fullWidth
            size="small"
          >
            <MenuItem value="name">Сортировка: Name (A-Z)</MenuItem>
            <MenuItem value="enabled">Сортировка: Enabled first</MenuItem>
            <MenuItem value="problematic">Сортировка: Problematic first</MenuItem>
          </Select>
        </Box>
        <Box sx={{ mt: 1 }}>
          <FormControlLabel
            control={
              <Switch
                checked={showOnlyProblematic}
                onChange={(e) => setShowOnlyProblematic(e.target.checked)}
              />
            }
            label="Только проблемные правила (no actions / no notify channel)"
          />
        </Box>
      </Paper>

      {!canOperate && (
        <Alert severity="info" sx={{ mb: 2 }}>
          У вас режим просмотра. Для тестовой отправки и enable/disable требуется роль OPERATOR или ADMIN.
        </Alert>
      )}

      <Paper sx={{ p: 2, mb: 2 }}>
        <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 1 }}>
          <Typography variant="h6">Live analytics stream</Typography>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
            <FormControlLabel
              control={
                <Switch
                  checked={livePaused}
                  onChange={(e) => {
                    const next = e.target.checked;
                    setLivePaused(next);
                    if (next) {
                      setPausedEventsSnapshot(analyticsEvents);
                    }
                  }}
                />
              }
              label="Пауза"
            />
            <Button
              variant="text"
              size="small"
              onClick={() => {
                dispatch(clearRealtimeEvents());
                setPausedEventsSnapshot([]);
              }}
              disabled={analyticsEvents.length === 0}
            >
              Очистить
            </Button>
          </Box>
        </Box>
        <Box sx={{ display: 'grid', gap: 1, gridTemplateColumns: { xs: '1fr', md: '1fr 1fr 180px' }, mb: 1 }}>
          <Select
            displayEmpty
            value={liveCameraFilter}
            onChange={(e) => setLiveCameraFilter(String(e.target.value))}
            size="small"
          >
            <MenuItem value="">Все камеры</MenuItem>
            {liveCameraOptions.map((cameraId) => (
              <MenuItem key={cameraId} value={cameraId}>
                {cameraId}
              </MenuItem>
            ))}
          </Select>
          <Select
            displayEmpty
            value={liveTypeFilter}
            onChange={(e) => setLiveTypeFilter(String(e.target.value))}
            size="small"
          >
            <MenuItem value="">Все типы</MenuItem>
            {liveTypeOptions.map((type) => (
              <MenuItem key={type} value={type}>
                {type}
              </MenuItem>
            ))}
          </Select>
          <Select
            value={String(liveVisibleCount)}
            onChange={(e) => setLiveVisibleCount(Number(e.target.value))}
            size="small"
          >
            <MenuItem value="8">8 событий</MenuItem>
            <MenuItem value="16">16 событий</MenuItem>
            <MenuItem value="32">32 события</MenuItem>
          </Select>
        </Box>
        <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mb: 1 }}>
          <Chip size="small" color="primary" label={`Events/min: ${liveKpi.eventsPerMinute}`} />
          <Chip size="small" label={`Active cameras/min: ${liveKpi.activeCameraCount}`} />
          <Chip size="small" variant="outlined" label={`Top camera: ${liveKpi.topCameraId}`} />
          <Chip size="small" variant="outlined" label={`Top camera events: ${liveKpi.topCameraEvents}`} />
          {livePaused && <Chip size="small" color="warning" label="Stream paused" />}
        </Box>
        {liveEventsToRender.length === 0 ? (
          <Alert severity="info">Нет live-событий аналитики. Ожидание входящих данных по каналу analytics...</Alert>
        ) : (
          <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
            {liveEventsToRender.map((event, idx) => (
              <Chip
                key={`${event.cameraId}-${event.timestamp}-${idx}`}
                size="small"
                color="info"
                label={`${event.resultType} · ${event.cameraId} · obj:${event.summary?.objectsCount ?? 0} face:${event.summary?.facesCount ?? 0}`}
              />
            ))}
          </Box>
        )}
      </Paper>

      <Paper sx={{ p: 2, mb: 2 }}>
        <Typography variant="h6" gutterBottom>
          Параметры тестовой отправки
        </Typography>
        <Box sx={{ display: 'grid', gap: 2, gridTemplateColumns: { xs: '1fr', md: '1fr 1fr' } }}>
          <TextField
            label="User ID (опционально)"
            value={targetUserId}
            onChange={(e) => setTargetUserId(e.target.value)}
            fullWidth
          />
          <TextField
            label="Camera ID (опционально)"
            value={targetCameraId}
            onChange={(e) => setTargetCameraId(e.target.value)}
            fullWidth
          />
        </Box>
        <Box sx={{ mt: 1, display: 'flex', gap: 2, flexWrap: 'wrap' }}>
          <FormControlLabel
            control={<Switch checked={notifyInApp} onChange={(e) => setNotifyInApp(e.target.checked)} />}
            label="In-app"
          />
          <FormControlLabel
            control={<Switch checked={notifyEmail} onChange={(e) => setNotifyEmail(e.target.checked)} />}
            label="Email"
          />
          <FormControlLabel
            control={<Switch checked={notifyTelegram} onChange={(e) => setNotifyTelegram(e.target.checked)} />}
            label="Telegram"
          />
        </Box>
      </Paper>

      {loading ? (
        <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
          <CircularProgress />
        </Box>
      ) : (
        <TableContainer component={Paper}>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Название</TableCell>
                <TableCell>Тип</TableCell>
                <TableCell>Камера</TableCell>
                <TableCell>Статус</TableCell>
                <TableCell>Каналы</TableCell>
                <TableCell align="right">Действия</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {sortedRules.map((rule) => {
                const busy = actionRuleId === rule.id;
                const channels: string[] = [];
                if (rule.actions?.notifyInApp) channels.push('in-app');
                if (rule.actions?.notifyEmail) channels.push('email');
                if (rule.actions?.notifyTelegram) channels.push('telegram');
                if (rule.actions?.additionalActions?.__notifySms) channels.push('sms');
                if (channels.length === 0 && rule.actions?.sendNotification) channels.push('notification');
                if (rule.actions?.sendWebhook || rule.actions?.additionalActions?.__notifyWebhook) channels.push('webhook');
                const hasNotificationAction = Boolean(rule.actions?.sendNotification);
                const hasNotificationChannels =
                  Boolean(rule.actions?.notifyInApp) ||
                  Boolean(rule.actions?.notifyEmail) ||
                  Boolean(rule.actions?.notifyTelegram);
                const hasWebhookAction = Boolean(rule.actions?.sendWebhook);
                const hasAnyAction = hasNotificationAction || hasWebhookAction;
                const hasRiskNoAction = !hasAnyAction;
                const hasRiskNotificationNoChannels = hasNotificationAction && !hasNotificationChannels;
                return (
                  <TableRow key={rule.id} hover>
                    <TableCell>{rule.name}</TableCell>
                    <TableCell>{rule.analyticsType ?? '-'}</TableCell>
                    <TableCell>{rule.cameraId || 'all'}</TableCell>
                    <TableCell>
                      <Chip
                        size="small"
                        label={rule.enabled ? 'enabled' : 'disabled'}
                        color={rule.enabled ? 'success' : 'default'}
                      />
                    </TableCell>
                    <TableCell>
                      <Box sx={{ display: 'flex', gap: 0.5, flexWrap: 'wrap' }}>
                        {hasNotificationAction && (
                          <Chip size="small" variant="outlined" color="primary" label="notify" />
                        )}
                        {hasWebhookAction && (
                          <Chip size="small" variant="outlined" color="secondary" label="webhook" />
                        )}
                        {channels
                          .filter((c) => c !== 'notification' && c !== 'webhook')
                          .map((c) => (
                            <Chip key={`${rule.id}-${c}`} size="small" label={c} />
                          ))}
                        {hasRiskNoAction && (
                          <Chip size="small" color="warning" label="no actions" />
                        )}
                        {hasRiskNotificationNoChannels && (
                          <Chip size="small" color="warning" label="no notify channel" />
                        )}
                        {!hasRiskNoAction && channels.length === 0 && (
                          <Chip size="small" label="-" />
                        )}
                      </Box>
                    </TableCell>
                    <TableCell align="right">
                      <Box sx={{ display: 'flex', gap: 1, justifyContent: 'flex-end' }}>
                        <Button
                          variant="outlined"
                          size="small"
                          onClick={() => void handleToggleRule(rule)}
                          disabled={!canOperate || busy}
                        >
                          {busy ? '...' : rule.enabled ? 'Disable' : 'Enable'}
                        </Button>
                        <Button
                          variant="contained"
                          size="small"
                          onClick={() => void handleTestSend(rule)}
                          disabled={!canOperate || busy}
                        >
                          {busy ? '...' : 'Test'}
                        </Button>
                        <Button
                          variant="text"
                          size="small"
                          onClick={() => openEditDialog(rule)}
                          disabled={!canOperate || busy}
                        >
                          {busy ? '...' : 'Edit'}
                        </Button>
                        <Button
                          variant="text"
                          color="error"
                          size="small"
                          onClick={() => void handleDeleteRule(rule)}
                          disabled={!canOperate || busy}
                        >
                          {busy ? '...' : 'Delete'}
                        </Button>
                      </Box>
                    </TableCell>
                  </TableRow>
                );
              })}
              {sortedRules.length === 0 && (
                <TableRow>
                  <TableCell colSpan={6} align="center">
                    Правила аналитики не найдены
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <Dialog open={editorOpen} onClose={() => setEditorOpen(false)} maxWidth="sm" fullWidth>
        <DialogTitle>{editingRuleId ? 'Редактирование правила' : 'Создание правила'}</DialogTitle>
        <DialogContent>
          <Box sx={{ display: 'grid', gap: 2, mt: 1 }}>
            <TextField
              label="Название"
              value={formName}
              onChange={(e) => setFormName(e.target.value)}
              fullWidth
            />
            <TextField
              label="Описание (опционально)"
              value={formDescription}
              onChange={(e) => setFormDescription(e.target.value)}
              fullWidth
            />
            <TextField
              label="Camera ID (опционально)"
              value={formCameraId}
              onChange={(e) => setFormCameraId(e.target.value)}
              fullWidth
            />
            <Box>
              <Typography variant="caption" color="text.secondary">
                Тип аналитики
              </Typography>
              <Select
                value={formAnalyticsType}
                onChange={(e) => setFormAnalyticsType(e.target.value)}
                fullWidth
                size="small"
              >
                <MenuItem value="MOTION_DETECTION">MOTION_DETECTION</MenuItem>
                <MenuItem value="OBJECT_DETECTION">OBJECT_DETECTION</MenuItem>
                <MenuItem value="FACE_DETECTION">FACE_DETECTION</MenuItem>
                <MenuItem value="LICENSE_PLATE_RECOGNITION">LICENSE_PLATE_RECOGNITION</MenuItem>
                <MenuItem value="COMPLEX_ANALYSIS">COMPLEX_ANALYSIS</MenuItem>
              </Select>
            </Box>
            <FormControlLabel
              control={<Switch checked={formEnabled} onChange={(e) => setFormEnabled(e.target.checked)} />}
              label="Rule enabled"
            />
            <FormControlLabel
              control={
                <Switch
                  checked={formSendNotification}
                  onChange={(e) => setFormSendNotification(e.target.checked)}
                />
              }
              label="Send notification"
            />
            <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', pl: 1 }}>
              <FormControlLabel
                control={
                  <Switch checked={formNotifyInApp} onChange={(e) => setFormNotifyInApp(e.target.checked)} />
                }
                label="In-app"
              />
              <FormControlLabel
                control={
                  <Switch checked={formNotifyEmail} onChange={(e) => setFormNotifyEmail(e.target.checked)} />
                }
                label="Email"
              />
              <FormControlLabel
                control={
                  <Switch
                    checked={formNotifyTelegram}
                    onChange={(e) => setFormNotifyTelegram(e.target.checked)}
                  />
                }
                label="Telegram"
              />
              <FormControlLabel
                control={
                  <Switch
                    checked={formNotifySms}
                    onChange={(e) => setFormNotifySms(e.target.checked)}
                  />
                }
                label="SMS"
              />
            </Box>
            <Box sx={{ pl: 1 }}>
              <FormControlLabel
                control={
                  <Switch
                    checked={formNotifyWebhook}
                    onChange={(e) => setFormNotifyWebhook(e.target.checked)}
                  />
                }
                label="Webhook"
              />
            </Box>
            {formSendNotification && !hasNotificationChannelSelected && (
              <Alert severity="warning">
                Уведомления включены, но ни один канал не выбран.
              </Alert>
            )}
            {formNotifyWebhook && (
              <TextField
                label="Webhook URL"
                value={formWebhookUrl}
                onChange={(e) => setFormWebhookUrl(e.target.value)}
                fullWidth
                helperText="URL для отправки webhook уведомлений"
              />
            )}
            {!hasAnyActionSelected && (
              <Alert severity="warning">
                Правило ничего не делает. Включите хотя бы уведомления или webhook.
              </Alert>
            )}
            <Typography variant="subtitle2" sx={{ mt: 1 }}>
              Conditions
            </Typography>
            <TextField
              label="Min confidence (0..1)"
              value={formMinConfidence}
              onChange={(e) => setFormMinConfidence(e.target.value)}
              fullWidth
            />
            <TextField
              label="Min object count"
              value={formMinObjectCount}
              onChange={(e) => setFormMinObjectCount(e.target.value)}
              fullWidth
            />
            <TextField
              label="Zones (comma separated)"
              value={formZones}
              onChange={(e) => setFormZones(e.target.value)}
              fullWidth
            />
            <TextField
              label="Object types (comma separated)"
              value={formObjectTypes}
              onChange={(e) => setFormObjectTypes(e.target.value)}
              fullWidth
            />
            <TextField
              label="Time window (HH:mm-HH:mm)"
              value={formTimeWindow}
              onChange={(e) => setFormTimeWindow(e.target.value)}
              fullWidth
              helperText="Например: 09:00-18:00 (пусто = всегда)"
            />
            <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap' }}>
              <Button size="small" variant="outlined" onClick={() => applyConditionPreset('business-hours')}>
                Рабочее время
              </Button>
              <Button size="small" variant="outlined" onClick={() => applyConditionPreset('night')}>
                Ночь
              </Button>
              <Button size="small" variant="outlined" onClick={() => applyConditionPreset('weekends')}>
                Выходные
              </Button>
              <Button size="small" variant="text" onClick={() => applyConditionPreset('always')}>
                Всегда
              </Button>
            </Box>
            <Box>
              <Typography variant="caption" color="text.secondary">
                Days of week (пусто = все дни)
              </Typography>
              <Box sx={{ display: 'flex', gap: 1, flexWrap: 'wrap', mt: 1 }}>
                {[
                  { value: 1, label: 'Mon' },
                  { value: 2, label: 'Tue' },
                  { value: 3, label: 'Wed' },
                  { value: 4, label: 'Thu' },
                  { value: 5, label: 'Fri' },
                  { value: 6, label: 'Sat' },
                  { value: 7, label: 'Sun' },
                ].map((day) => (
                  <FormControlLabel
                    key={day.value}
                    control={
                      <Checkbox
                        checked={formDaysOfWeek.includes(day.value)}
                        onChange={(e) => {
                          if (e.target.checked) {
                            setFormDaysOfWeek((prev) => [...prev, day.value].sort((a, b) => a - b));
                          } else {
                            setFormDaysOfWeek((prev) => prev.filter((v) => v !== day.value));
                          }
                        }}
                        size="small"
                      />
                    }
                    label={day.label}
                  />
                ))}
              </Box>
            </Box>
            <Alert severity="info">
              Preview: {conditionPreview}
            </Alert>
          </Box>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEditorOpen(false)}>Отмена</Button>
          <Button onClick={() => void handleSaveEditor()} variant="contained" disabled={savingEditor}>
            {savingEditor ? 'Сохранение...' : 'Сохранить'}
          </Button>
        </DialogActions>
      </Dialog>
    </Layout>
  );
}

export default function AnalyticsRulesPage() {
  return (
    <ProtectedRoute>
      <AnalyticsRulesPageContent />
    </ProtectedRoute>
  );
}

