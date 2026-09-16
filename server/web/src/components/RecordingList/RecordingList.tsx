'use client';

import Pagination from '@/components/Pagination/Pagination';
import { useDebounce } from '@/hooks/useDebounce';
import { useWebSocket } from '@/hooks/useWebSocket';
import type { Recording } from '@/types';
import {
  Delete as DeleteIcon,
  FileDownload as FileDownloadIcon,
  FilterList as FilterListIcon,
  Search as SearchIcon,
  Sort as SortIcon,
  TableChart as TableChartIcon,
  ViewList as ViewListIcon,
  ViewModule as ViewModuleIcon,
} from '@mui/icons-material';
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  IconButton,
  InputAdornment,
  Menu,
  MenuItem,
  Paper,
  TextField,
  Tooltip,
  Typography
} from '@mui/material';
import React, { useEffect, useMemo, useState } from 'react';
import RecordingListFiltersComponent from './RecordingListFilters';
import RecordingListTable from './RecordingListTable';
import VirtualizedGrid from './VirtualizedGrid';
import type {
  RecordingListFilters,
  RecordingListProps,
  RecordingListViewMode,
  RecordingSortConfig,
} from './types';

const STORAGE_KEY_VIEW_MODE = 'recordingList_viewMode';
const STORAGE_KEY_FILTERS = 'recordingList_filters';
const STORAGE_KEY_SORT = 'recordingList_sort';

export default function RecordingList({
  recordings,
  loading = false,
  error = null,
  pagination = null,
  cameras = [],
  viewMode: initialViewMode = 'grid',
  sortConfig: initialSortConfig,
  filters: initialFilters = {},
  selectedRecordings = [],
  onRecordingClick,
  onDelete,
  onDownload,
  onExport,
  onBulkDelete,
  onBulkExport,
  onFiltersChange,
  onSortChange,
  onViewModeChange,
  onSelectionChange,
  onPageChange,
  onLimitChange,
  onClearError,
}: RecordingListProps) {
  // Загрузка сохраненных настроек из localStorage
  const loadSavedSettings = () => {
    if (typeof window === 'undefined') return null;
    try {
      const savedViewMode = localStorage.getItem(STORAGE_KEY_VIEW_MODE) as RecordingListViewMode | null;
      const savedFilters = localStorage.getItem(STORAGE_KEY_FILTERS);
      const savedSort = localStorage.getItem(STORAGE_KEY_SORT);
      return {
        viewMode: savedViewMode || initialViewMode,
        filters: savedFilters ? JSON.parse(savedFilters) : initialFilters,
        sort: savedSort ? JSON.parse(savedSort) : (initialSortConfig || { field: 'startTime', order: 'desc' }),
      };
    } catch {
      return null;
    }
  };

  const savedSettings = loadSavedSettings();
  const [viewMode, setViewMode] = useState<RecordingListViewMode>(
    savedSettings?.viewMode || initialViewMode
  );
  const [filterDialogOpen, setFilterDialogOpen] = useState(false);
  const [filters, setFilters] = useState<RecordingListFilters>(
    savedSettings?.filters || initialFilters
  );
  const [searchQuery, setSearchQuery] = useState(filters.searchQuery || '');
  const debouncedSearchQuery = useDebounce(searchQuery, 300);
  const [sortConfig, setSortConfig] = useState<RecordingSortConfig>(
    savedSettings?.sort || initialSortConfig || { field: 'startTime', order: 'desc' }
  );
  const [selectedIds, setSelectedIds] = useState<string[]>(selectedRecordings);
  const [sortMenuAnchor, setSortMenuAnchor] = useState<null | HTMLElement>(null);
  const [wsConnected, setWsConnected] = useState(false);
  const [newRecordingIds, setNewRecordingIds] = useState<Set<string>>(new Set());
  const { connected, subscribe } = useWebSocket();

  // Подписка на WebSocket канал recordings
  useEffect(() => {
    if (connected) {
      subscribe(['recordings']);
      setWsConnected(true);
    } else {
      setWsConnected(false);
    }
  }, [connected, subscribe]);

  // Отслеживание новых записей из WebSocket
  useEffect(() => {
    // Добавляем новые записи в Set для анимации
    const newIds = new Set<string>();
    recordings.forEach((recording) => {
      // Проверяем, является ли запись новой (создана менее 5 секунд назад)
      const now = Date.now();
      const recordingAge = now - recording.startTime;
      if (recordingAge < 5000) {
        newIds.add(recording.id);
      }
    });
    setNewRecordingIds(newIds);

    // Очищаем флаг "новый" через 5 секунд
    const timer = setTimeout(() => {
      setNewRecordingIds(new Set());
    }, 5000);

    return () => clearTimeout(timer);
  }, [recordings]);

  // Сохранение настроек в localStorage
  useEffect(() => {
    if (typeof window === 'undefined') return;
    try {
      localStorage.setItem(STORAGE_KEY_VIEW_MODE, viewMode);
      localStorage.setItem(STORAGE_KEY_FILTERS, JSON.stringify(filters));
      localStorage.setItem(STORAGE_KEY_SORT, JSON.stringify(sortConfig));
    } catch (error) {
      console.warn('Failed to save settings to localStorage:', error);
    }
  }, [viewMode, filters, sortConfig]);

  // Фильтрация и поиск (используем debounced поиск)
  const filteredRecordings = useMemo(() => {
    let filtered = [...recordings];

    // Поиск (используем debounced значение)
    if (debouncedSearchQuery) {
      const query = debouncedSearchQuery.toLowerCase();
      filtered = filtered.filter(
        (r) =>
          (r.cameraName || r.cameraId).toLowerCase().includes(query) ||
          r.id.toLowerCase().includes(query)
      );
    }

    // Фильтр по камере
    if (filters.cameraId) {
      filtered = filtered.filter((r) => r.cameraId === filters.cameraId);
    }

    // Фильтр по статусу
    if (filters.status) {
      filtered = filtered.filter((r) => r.status === filters.status);
    }

    // Фильтр по дате
    if (filters.startTime) {
      const startTime = filters.startTime;
      filtered = filtered.filter((r) => r.startTime >= startTime);
    }
    if (filters.endTime) {
      const endTime = filters.endTime;
      filtered = filtered.filter((r) => r.startTime <= endTime);
    }

    return filtered;
  }, [recordings, debouncedSearchQuery, filters]);

  // Сортировка
  const sortedRecordings = useMemo(() => {
    const sorted = [...filteredRecordings];
    sorted.sort((a, b) => {
      let aValue: string | number;
      let bValue: string | number;

      switch (sortConfig.field) {
        case 'startTime':
          aValue = a.startTime;
          bValue = b.startTime;
          break;
        case 'duration':
          aValue = a.duration;
          bValue = b.duration;
          break;
        case 'fileSize':
          aValue = a.fileSize || 0;
          bValue = b.fileSize || 0;
          break;
        case 'status':
          aValue = a.status;
          bValue = b.status;
          break;
        case 'cameraName':
          aValue = (a.cameraName || a.cameraId).toLowerCase();
          bValue = (b.cameraName || b.cameraId).toLowerCase();
          break;
        default:
          return 0;
      }

      if (aValue < bValue) return sortConfig.order === 'asc' ? -1 : 1;
      if (aValue > bValue) return sortConfig.order === 'asc' ? 1 : -1;
      return 0;
    });

    return sorted;
  }, [filteredRecordings, sortConfig]);

  const handleViewModeChange = (mode: RecordingListViewMode) => {
    setViewMode(mode);
    onViewModeChange?.(mode);
  };

  const handleFiltersApply = (newFilters: RecordingListFilters) => {
    const updatedFilters = { ...newFilters, searchQuery };
    setFilters(updatedFilters);
    onFiltersChange?.(updatedFilters);
  };

  const handleSearchChange = (query: string) => {
    setSearchQuery(query);
    const updatedFilters = { ...filters, searchQuery: query };
    setFilters(updatedFilters);
    onFiltersChange?.(updatedFilters);
  };

  const handleSortChange = (field: RecordingSortConfig['field']) => {
    const newOrder: RecordingSortConfig['order'] =
      sortConfig.field === field && sortConfig.order === 'asc' ? 'desc' : 'asc';
    const newSortConfig: RecordingSortConfig = { field, order: newOrder };
    setSortConfig(newSortConfig);
    onSortChange?.(newSortConfig);
    setSortMenuAnchor(null);
  };

  const handleSelectRecording = (recordingId: string, selected: boolean) => {
    const newSelected = selected
      ? [...selectedIds, recordingId]
      : selectedIds.filter((id: string) => id !== recordingId);
    setSelectedIds(newSelected);
    onSelectionChange?.(newSelected);
  };

  const handleSelectAll = () => {
    if (selectedIds.length === sortedRecordings.length) {
      setSelectedIds([]);
      onSelectionChange?.([]);
    } else {
      const allIds: string[] = sortedRecordings.map((r: Recording) => r.id);
      setSelectedIds(allIds);
      onSelectionChange?.(allIds);
    }
  };

  const handleBulkDelete = () => {
    if (selectedIds.length > 0 && confirm(`Удалить ${selectedIds.length} записей?`)) {
      onBulkDelete?.(selectedIds);
      setSelectedIds([]);
      onSelectionChange?.([]);
    }
  };

  const handleBulkExport = () => {
    if (selectedIds.length > 0) {
      onBulkExport?.(selectedIds);
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: 'flex', justifyContent: 'center', mt: 4 }}>
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <Alert severity="error" sx={{ mb: 2 }} onClose={onClearError}>
        {error}
      </Alert>
    );
  }

  return (
    <Box>
      {/* Панель инструментов */}
      <Box
        sx={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          mb: 2,
          flexWrap: 'wrap',
          gap: 2,
        }}
      >
        {/* Индикатор WebSocket подключения */}
        {wsConnected && (
          <Box
            sx={{
              display: 'flex',
              alignItems: 'center',
              gap: 0.5,
              px: 1,
              py: 0.5,
              borderRadius: 1,
              bgcolor: 'success.light',
              color: 'success.contrastText',
            }}
          >
            <Box
              sx={{
                width: 8,
                height: 8,
                borderRadius: '50%',
                bgcolor: 'success.main',
                animation: 'pulse 2s infinite',
                '@keyframes pulse': {
                  '0%, 100%': { opacity: 1 },
                  '50%': { opacity: 0.5 },
                },
              }}
            />
            <Typography variant="caption">Live</Typography>
          </Box>
        )}
        {/* Поиск */}
        <TextField
          size="small"
          placeholder="Поиск по камере или ID..."
          value={searchQuery}
          onChange={(e: React.ChangeEvent<HTMLInputElement>) => handleSearchChange(e.target.value)}
          InputProps={{
            startAdornment: (
              <InputAdornment position="start">
                <SearchIcon />
              </InputAdornment>
            ),
          }}
          sx={{ minWidth: 250 }}
        />

        <Box sx={{ display: 'flex', gap: 1, alignItems: 'center' }}>
          {/* Режимы отображения */}
          <Box sx={{ display: 'flex', border: 1, borderColor: 'divider', borderRadius: 1 }}>
            <Tooltip title="Сетка">
              <IconButton
                size="small"
                onClick={() => handleViewModeChange('grid')}
                color={viewMode === 'grid' ? 'primary' : 'default'}
              >
                <ViewModuleIcon />
              </IconButton>
            </Tooltip>
            <Tooltip title="Список">
              <IconButton
                size="small"
                onClick={() => handleViewModeChange('list')}
                color={viewMode === 'list' ? 'primary' : 'default'}
              >
                <ViewListIcon />
              </IconButton>
            </Tooltip>
            <Tooltip title="Таблица">
              <IconButton
                size="small"
                onClick={() => handleViewModeChange('table')}
                color={viewMode === 'table' ? 'primary' : 'default'}
              >
                <TableChartIcon />
              </IconButton>
            </Tooltip>
          </Box>

          {/* Сортировка */}
          <Tooltip title="Сортировка">
            <IconButton
              size="small"
              onClick={(e: React.MouseEvent<HTMLButtonElement>) => setSortMenuAnchor(e.currentTarget)}
            >
              <SortIcon />
            </IconButton>
          </Tooltip>
          <Menu
            anchorEl={sortMenuAnchor}
            open={Boolean(sortMenuAnchor)}
            onClose={() => setSortMenuAnchor(null)}
          >
            <MenuItem
              onClick={() => handleSortChange('startTime')}
              selected={sortConfig.field === 'startTime'}
            >
              По дате {sortConfig.field === 'startTime' && (sortConfig.order === 'asc' ? '↑' : '↓')}
            </MenuItem>
            <MenuItem
              onClick={() => handleSortChange('duration')}
              selected={sortConfig.field === 'duration'}
            >
              По длительности{' '}
              {sortConfig.field === 'duration' && (sortConfig.order === 'asc' ? '↑' : '↓')}
            </MenuItem>
            <MenuItem
              onClick={() => handleSortChange('fileSize')}
              selected={sortConfig.field === 'fileSize'}
            >
              По размеру{' '}
              {sortConfig.field === 'fileSize' && (sortConfig.order === 'asc' ? '↑' : '↓')}
            </MenuItem>
            <MenuItem
              onClick={() => handleSortChange('cameraName')}
              selected={sortConfig.field === 'cameraName'}
            >
              По камере{' '}
              {sortConfig.field === 'cameraName' && (sortConfig.order === 'asc' ? '↑' : '↓')}
            </MenuItem>
          </Menu>

          {/* Фильтры */}
          <Button
            variant="outlined"
            startIcon={<FilterListIcon />}
            onClick={() => setFilterDialogOpen(true)}
          >
            Фильтры
          </Button>

          {/* Массовые операции */}
          {selectedIds.length > 0 && (
            <>
              <Button
                variant="outlined"
                startIcon={<DeleteIcon />}
                onClick={handleBulkDelete}
                color="error"
              >
                Удалить ({selectedIds.length})
              </Button>
              <Button
                variant="outlined"
                startIcon={<FileDownloadIcon />}
                onClick={handleBulkExport}
              >
                Экспорт ({selectedIds.length})
              </Button>
            </>
          )}
        </Box>
      </Box>

      {/* Выбор всех */}
      {sortedRecordings.length > 0 && (
        <Box sx={{ mb: 2 }}>
          <Button size="small" onClick={handleSelectAll}>
            {selectedIds.length === sortedRecordings.length ? 'Снять выбор' : 'Выбрать все'}
          </Button>
          {selectedIds.length > 0 && (
            <Typography variant="body2" color="text.secondary" sx={{ ml: 2, display: 'inline' }}>
              Выбрано: {selectedIds.length}
            </Typography>
          )}
        </Box>
      )}

      {/* Список записей */}
      {sortedRecordings.length === 0 ? (
        <Paper sx={{ p: 3, mt: 2 }}>
          <Typography variant="body1" color="text.secondary" align="center">
            Записи не найдены
          </Typography>
        </Paper>
      ) : viewMode === 'table' ? (
        <RecordingListTable
          recordings={sortedRecordings}
          selectedIds={selectedIds}
          onSelect={handleSelectRecording}
          onSelectAll={handleSelectAll}
          onRecordingClick={onRecordingClick}
          onDelete={onDelete}
          onDownload={onDownload}
          onExport={onExport}
        />
      ) : (
        <VirtualizedGrid
          newRecordingIds={newRecordingIds}
          recordings={sortedRecordings}
          viewMode={viewMode}
          selectedIds={selectedIds}
          onSelect={handleSelectRecording}
          onRecordingClick={onRecordingClick}
          onDelete={onDelete}
          onDownload={onDownload}
          onExport={onExport}
          showCheckbox={selectedIds.length > 0}
        />
      )}

      {/* Пагинация */}
      {pagination && (
        <Box sx={{ mt: 3 }}>
          <Pagination
            page={pagination.page}
            limit={pagination.limit}
            total={pagination.total || 0}
            onPageChange={onPageChange || (() => {})}
            onLimitChange={onLimitChange || (() => {})}
          />
        </Box>
      )}

      {/* Диалог фильтров */}
      <RecordingListFiltersComponent
        open={filterDialogOpen}
        onClose={() => setFilterDialogOpen(false)}
        onApply={handleFiltersApply}
        filters={filters}
        cameras={cameras}
      />
    </Box>
  );
}
