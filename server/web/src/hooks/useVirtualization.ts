import { useState, useRef, useCallback } from 'react';

interface UseVirtualizationOptions {
  itemHeight: number;
  containerHeight: number;
  overscan?: number; // Количество элементов для рендеринга за пределами видимой области
}

interface VirtualizedItem {
  index: number;
  offset: number;
  height: number;
}

/**
 * Хук для виртуализации списков
 * Рендерит только видимые элементы для оптимизации производительности
 */
export function useVirtualization<T>(
  items: T[],
  options: UseVirtualizationOptions
) {
  const { itemHeight, containerHeight, overscan = 3 } = options;
  const [scrollTop, setScrollTop] = useState(0);
  const containerRef = useRef<HTMLDivElement>(null);

  // Вычисляем видимые элементы
  const visibleItems = useCallback((): VirtualizedItem[] => {
    if (items.length === 0) return [];

    const startIndex = Math.max(0, Math.floor(scrollTop / itemHeight) - overscan);
    const endIndex = Math.min(
      items.length - 1,
      Math.ceil((scrollTop + containerHeight) / itemHeight) + overscan
    );

    const result: VirtualizedItem[] = [];
    for (let i = startIndex; i <= endIndex; i++) {
      result.push({
        index: i,
        offset: i * itemHeight,
        height: itemHeight,
      });
    }

    return result;
  }, [items.length, itemHeight, containerHeight, scrollTop, overscan]);

  // Обработчик скролла
  const handleScroll = useCallback((e: React.UIEvent<HTMLDivElement>) => {
    setScrollTop(e.currentTarget.scrollTop);
  }, []);

  // Общая высота списка
  const totalHeight = items.length * itemHeight;

  // Видимые элементы
  const visible = visibleItems();

  return {
    containerRef,
    visibleItems: visible,
    totalHeight,
    handleScroll,
    startIndex: visible.length > 0 ? visible[0].index : 0,
    endIndex: visible.length > 0 ? visible[visible.length - 1].index : 0,
  };
}
