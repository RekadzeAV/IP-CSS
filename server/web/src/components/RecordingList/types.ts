import type { Camera, Recording } from '@/types';

export type RecordingListViewMode = 'grid' | 'list' | 'table';

export type RecordingSortField = 'startTime' | 'duration' | 'fileSize' | 'status' | 'cameraName';
export type RecordingSortOrder = 'asc' | 'desc';

export interface RecordingSortConfig {
  field: RecordingSortField;
  order: RecordingSortOrder;
}

export interface RecordingListFilters {
  cameraId?: string;
  startTime?: number;
  endTime?: number;
  status?: string;
  searchQuery?: string;
}

export interface RecordingListProps {
  recordings: Recording[];
  loading?: boolean;
  error?: string | null;
  pagination?: {
    page: number;
    limit: number;
    total: number;
  } | null;
  cameras?: Camera[];
  viewMode?: RecordingListViewMode;
  sortConfig?: RecordingSortConfig;
  filters?: RecordingListFilters;
  selectedRecordings?: string[];
  onRecordingClick?: (recordingId: string) => void;
  onDelete?: (recordingId: string) => void;
  onDownload?: (recordingId: string) => void;
  onExport?: (recordingId: string) => void;
  onBulkDelete?: (recordingIds: string[]) => void;
  onBulkExport?: (recordingIds: string[]) => void;
  onFiltersChange?: (filters: RecordingListFilters) => void;
  onSortChange?: (sortConfig: RecordingSortConfig) => void;
  onViewModeChange?: (viewMode: RecordingListViewMode) => void;
  onSelectionChange?: (selectedIds: string[]) => void;
  onPageChange?: (page: number) => void;
  onLimitChange?: (limit: number) => void;
  onClearError?: () => void;
}
