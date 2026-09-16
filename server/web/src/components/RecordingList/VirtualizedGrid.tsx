'use client';

import React, { useMemo, useRef, useState, useEffect } from 'react';
import { Box, Grid } from '@mui/material';
import RecordingListItem from './RecordingListItem';
import type { Recording } from '@/types';
import type { RecordingListViewMode } from './types';

interface VirtualizedGridProps {
  recordings: Recording[];
  viewMode: RecordingListViewMode;
  selectedIds: string[];
  onSelect: (recordingId: string, selected: boolean) => void;
  onRecordingClick?: (recordingId: string) => void;
  onDelete?: (id: string) => void;
  onDownload?: (id: string) => void;
  onExport?: (id: string) => void;
  showCheckbox: boolean;
  newRecordingIds?: Set<string>; // ID новых записей для анимации
}

/**
 * Виртуализированный компонент для отображения записей в grid/list режимах
 * Рендерит только видимые элементы для оптимизации производительности
 */
export default function VirtualizedGrid({
  recordings,
  viewMode,
  selectedIds,
  onSelect,
  onRecordingClick,
  onDelete,
  onDownload,
  onExport,
  showCheckbox,
  newRecordingIds = new Set(),
}: VirtualizedGridProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const [containerHeight, setContainerHeight] = useState(600);
  const [scrollTop, setScrollTop] = useState(0);

  // Вычисляем размеры элементов
  const itemHeight = viewMode === 'list' ? 120 : 200; // Высота элемента
  const itemsPerRow = useMemo(() => {
    if (viewMode === 'list') return 1;
    // Для grid режима вычисляем количество элементов в строке на основе ширины контейнера
    if (typeof window !== 'undefined') {
      const width = containerRef.current?.clientWidth || window.innerWidth;
      if (width >= 1200) return 3; // md и больше
      if (width >= 600) return 2; // sm
      return 1; // xs
    }
    return 3;
  }, [viewMode]);

  // Вычисляем видимые элементы
  const visibleItems = useMemo(() => {
    if (recordings.length === 0) return [];

    const rowHeight = itemHeight + 24; // itemHeight + spacing
    const startRow = Math.max(0, Math.floor(scrollTop / rowHeight) - 2);
    const endRow = Math.min(
      Math.ceil(recordings.length / itemsPerRow) - 1,
      Math.ceil((scrollTop + containerHeight) / rowHeight) + 2
    );

    const visible: Recording[] = [];
    for (let row = startRow; row <= endRow; row++) {
      const startIndex = row * itemsPerRow;
      const endIndex = Math.min(startIndex + itemsPerRow, recordings.length);
      for (let i = startIndex; i < endIndex; i++) {
        if (recordings[i]) {
          visible.push(recordings[i]);
        }
      }
    }

    return visible;
  }, [recordings, scrollTop, containerHeight, itemHeight, itemsPerRow]);

  // Обновляем высоту контейнера при изменении размера окна
  useEffect(() => {
    const updateHeight = () => {
      if (containerRef.current) {
        setContainerHeight(containerRef.current.clientHeight);
      }
    };

    updateHeight();
    window.addEventListener('resize', updateHeight);
    return () => window.removeEventListener('resize', updateHeight);
  }, []);

  // Обработчик скролла
  const handleScroll = (e: React.UIEvent<HTMLDivElement>) => {
    setScrollTop(e.currentTarget.scrollTop);
  };

  // Вычисляем общую высоту и offset для видимых элементов
  const totalRows = Math.ceil(recordings.length / itemsPerRow);
  const totalHeight = totalRows * (itemHeight + 24);
  const startRow = Math.max(0, Math.floor(scrollTop / (itemHeight + 24)) - 2);
  const offsetY = startRow * (itemHeight + 24);

  return (
    <Box
      ref={containerRef}
      onScroll={handleScroll}
      sx={{
        height: '70vh',
        overflowY: 'auto',
        overflowX: 'hidden',
        position: 'relative',
      }}
    >
      <Box
        sx={{
          height: totalHeight,
          position: 'relative',
        }}
      >
        <Box
          sx={{
            position: 'absolute',
            top: offsetY,
            left: 0,
            right: 0,
          }}
        >
          <Grid container spacing={3}>
            {visibleItems.map((recording: Recording) => (
              <Grid
                item
                xs={12}
                sm={viewMode === 'list' ? 12 : 6}
                md={viewMode === 'list' ? 12 : 4}
                key={recording.id}
              >
                <RecordingListItem
                  recording={recording}
                  selected={selectedIds.includes(recording.id)}
                  onSelect={onSelect}
                  onRecordingClick={onRecordingClick}
                  onDelete={onDelete}
                  onDownload={onDownload}
                  onExport={onExport}
                  showCheckbox={showCheckbox}
                  viewMode={viewMode}
                  isNew={newRecordingIds.has(recording.id)}
                />
              </Grid>
            ))}
          </Grid>
        </Box>
      </Box>
    </Box>
  );
}
