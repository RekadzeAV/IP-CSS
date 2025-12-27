'use client';

import React, { useState, useEffect, useCallback } from 'react';
import {
  TextField,
  InputAdornment,
  IconButton,
  Box,
  Paper,
  List,
  ListItem,
  ListItemButton,
  ListItemText,
  Typography,
  Chip,
  Divider,
} from '@mui/material';
import {
  Search as SearchIcon,
  Clear as ClearIcon,
  FilterList as FilterListIcon,
} from '@mui/icons-material';
import { useRouter, useSearchParams } from 'next/navigation';
import type { Camera, Event, Recording } from '@/types';

export interface SearchResult {
  type: 'camera' | 'event' | 'recording';
  id: string;
  title: string;
  description?: string;
  data: Camera | Event | Recording;
}

interface SearchBarProps {
  onSearch?: (query: string) => void;
  onResultClick?: (result: SearchResult) => void;
  placeholder?: string;
  debounceMs?: number;
}

export default function SearchBar({
  onSearch,
  onResultClick,
  placeholder = 'Поиск...',
  debounceMs = 300,
}: SearchBarProps) {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [query, setQuery] = useState(searchParams.get('q') || '');
  const [results, setResults] = useState<SearchResult[]>([]);
  const [loading, setLoading] = useState(false);
  const [showResults, setShowResults] = useState(false);

  // Debounced поиск
  useEffect(() => {
    if (!query.trim()) {
      setResults([]);
      setShowResults(false);
      return;
    }

    const timer = setTimeout(() => {
      performSearch(query);
    }, debounceMs);

    return () => clearTimeout(timer);
  }, [query, debounceMs]);

  const performSearch = async (searchQuery: string) => {
    if (!searchQuery.trim()) {
      setResults([]);
      return;
    }

    setLoading(true);
    try {
      // Импортируем сервисы динамически
      const { cameraService } = await import('@/services/cameraService');
      const { eventService } = await import('@/services/eventService');
      const { recordingService } = await import('@/services/recordingService');

      const searchResults: SearchResult[] = [];

      // Поиск камер
      try {
        const cameras = await cameraService.getCameras();
        const cameraMatches = cameras.filter(
          (camera) =>
            camera.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
            camera.url.toLowerCase().includes(searchQuery.toLowerCase()) ||
            camera.model?.toLowerCase().includes(searchQuery.toLowerCase())
        );
        cameraMatches.forEach((camera) => {
          searchResults.push({
            type: 'camera',
            id: camera.id,
            title: camera.name,
            description: camera.url,
            data: camera,
          });
        });
      } catch (error) {
        console.error('Error searching cameras:', error);
      }

      // Поиск событий
      try {
        const events = await eventService.getEvents({ limit: 50 });
        const eventMatches = events.filter(
          (event) =>
            event.type.toLowerCase().includes(searchQuery.toLowerCase()) ||
            event.description?.toLowerCase().includes(searchQuery.toLowerCase()) ||
            event.cameraName?.toLowerCase().includes(searchQuery.toLowerCase())
        );
        eventMatches.forEach((event) => {
          searchResults.push({
            type: 'event',
            id: event.id,
            title: `${event.type} - ${event.cameraName || event.cameraId}`,
            description: event.description,
            data: event,
          });
        });
      } catch (error) {
        console.error('Error searching events:', error);
      }

      // Поиск записей
      try {
        const recordings = await recordingService.getRecordings({ limit: 50 });
        const recordingMatches = recordings.filter(
          (recording) =>
            recording.cameraName?.toLowerCase().includes(searchQuery.toLowerCase()) ||
            recording.cameraId.toLowerCase().includes(searchQuery.toLowerCase())
        );
        recordingMatches.forEach((recording) => {
          searchResults.push({
            type: 'recording',
            id: recording.id,
            title: `Запись - ${recording.cameraName || recording.cameraId}`,
            description: `Длительность: ${recording.duration}с`,
            data: recording,
          });
        });
      } catch (error) {
        console.error('Error searching recordings:', error);
      }

      setResults(searchResults.slice(0, 10)); // Ограничиваем 10 результатами
      setShowResults(searchResults.length > 0);
    } catch (error) {
      console.error('Search error:', error);
      setResults([]);
    } finally {
      setLoading(false);
    }
  };

  const handleQueryChange = (event: React.ChangeEvent<HTMLInputElement>) => {
    const value = event.target.value;
    setQuery(value);
    if (onSearch) {
      onSearch(value);
    }
  };

  const handleClear = () => {
    setQuery('');
    setResults([]);
    setShowResults(false);
    if (onSearch) {
      onSearch('');
    }
  };

  const handleResultClick = (result: SearchResult) => {
    setShowResults(false);
    if (onResultClick) {
      onResultClick(result);
    } else {
      // Навигация по умолчанию
      switch (result.type) {
        case 'camera':
          router.push(`/cameras/${result.id}`);
          break;
        case 'event':
          router.push(`/events/${result.id}`);
          break;
        case 'recording':
          router.push(`/recordings/${result.id}`);
          break;
      }
    }
  };

  const getTypeLabel = (type: SearchResult['type']): string => {
    switch (type) {
      case 'camera':
        return 'Камера';
      case 'event':
        return 'Событие';
      case 'recording':
        return 'Запись';
    }
  };

  const getTypeColor = (type: SearchResult['type']): 'primary' | 'secondary' | 'success' | 'error' | 'warning' | 'info' => {
    switch (type) {
      case 'camera':
        return 'primary';
      case 'event':
        return 'warning';
      case 'recording':
        return 'info';
    }
  };

  return (
    <Box sx={{ position: 'relative', width: '100%' }}>
      <TextField
        fullWidth
        value={query}
        onChange={handleQueryChange}
        placeholder={placeholder}
        InputProps={{
          startAdornment: (
            <InputAdornment position="start">
              <SearchIcon />
            </InputAdornment>
          ),
          endAdornment: query && (
            <InputAdornment position="end">
              <IconButton size="small" onClick={handleClear}>
                <ClearIcon />
              </IconButton>
            </InputAdornment>
          ),
        }}
        onFocus={() => {
          if (results.length > 0) {
            setShowResults(true);
          }
        }}
        onBlur={() => {
          // Задержка для обработки клика по результату
          setTimeout(() => setShowResults(false), 200);
        }}
      />

      {showResults && results.length > 0 && (
        <Paper
          sx={{
            position: 'absolute',
            top: '100%',
            left: 0,
            right: 0,
            mt: 1,
            maxHeight: 400,
            overflow: 'auto',
            zIndex: 1000,
            boxShadow: 3,
          }}
        >
          <List dense>
            {results.map((result, index) => (
              <React.Fragment key={result.id}>
                <ListItem disablePadding>
                  <ListItemButton onClick={() => handleResultClick(result)}>
                    <ListItemText
                      primary={
                        <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                          <Chip
                            label={getTypeLabel(result.type)}
                            color={getTypeColor(result.type)}
                            size="small"
                          />
                          <Typography variant="body1" noWrap>
                            {result.title}
                          </Typography>
                        </Box>
                      }
                      secondary={result.description}
                    />
                  </ListItemButton>
                </ListItem>
                {index < results.length - 1 && <Divider />}
              </React.Fragment>
            ))}
          </List>
        </Paper>
      )}

      {showResults && results.length === 0 && !loading && query && (
        <Paper
          sx={{
            position: 'absolute',
            top: '100%',
            left: 0,
            right: 0,
            mt: 1,
            p: 2,
            zIndex: 1000,
            boxShadow: 3,
          }}
        >
          <Typography variant="body2" color="text.secondary" align="center">
            Ничего не найдено
          </Typography>
        </Paper>
      )}
    </Box>
  );
}

