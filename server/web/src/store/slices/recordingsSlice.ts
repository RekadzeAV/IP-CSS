import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { recordingService } from '@/services/recordingService';
import type { RecordingFilters } from '@/services/recordingService';
import type { Recording } from '@/types';

interface RecordingsState {
  recordings: Recording[];
  selectedRecording: Recording | null;
  loading: boolean;
  error: string | null;
  pagination: {
    page: number;
    limit: number;
    total: number;
    hasMore: boolean;
  };
  filters: {
    cameraId?: string;
    startTime?: number;
    endTime?: number;
    status?: string;
  };
}

const initialState: RecordingsState = {
  recordings: [],
  selectedRecording: null,
  loading: false,
  error: null,
  pagination: {
    page: 1,
    limit: 20,
    total: 0,
    hasMore: false,
  },
  filters: {},
};

const getErrorMessage = (error: unknown, fallback: string): string =>
  error instanceof Error ? error.message : fallback;

// Async thunks
export const fetchRecordings = createAsyncThunk(
  'recordings/fetchRecordings',
  async (filters: RecordingFilters | undefined, { rejectWithValue }) => {
    try {
      const response = await recordingService.getRecordings(filters);
      return response;
    } catch (error: unknown) {
      return rejectWithValue(getErrorMessage(error, 'Failed to fetch recordings'));
    }
  }
);

export const fetchRecordingById = createAsyncThunk(
  'recordings/fetchRecordingById',
  async (id: string, { rejectWithValue }) => {
    try {
      const recording = await recordingService.getRecordingById(id);
      return recording;
    } catch (error: unknown) {
      return rejectWithValue(getErrorMessage(error, 'Failed to fetch recording'));
    }
  }
);

export const deleteRecording = createAsyncThunk(
  'recordings/deleteRecording',
  async (id: string, { rejectWithValue }) => {
    try {
      await recordingService.deleteRecording(id);
      return id;
    } catch (error: unknown) {
      return rejectWithValue(getErrorMessage(error, 'Failed to delete recording'));
    }
  }
);

export const getDownloadUrl = createAsyncThunk(
  'recordings/getDownloadUrl',
  async (id: string, { rejectWithValue }) => {
    try {
      const result = await recordingService.getDownloadUrl(id);
      return result;
    } catch (error: unknown) {
      return rejectWithValue(getErrorMessage(error, 'Failed to get download URL'));
    }
  }
);

export const exportRecording = createAsyncThunk(
  'recordings/exportRecording',
  async (
    { id, format, quality }: { id: string; format: string; quality: string },
    { rejectWithValue }
  ) => {
    try {
      const result = await recordingService.exportRecording(id, format, quality);
      return result;
    } catch (error: unknown) {
      return rejectWithValue(getErrorMessage(error, 'Failed to export recording'));
    }
  }
);

const recordingsSlice = createSlice({
  name: 'recordings',
  initialState,
  reducers: {
    setSelectedRecording: (state, action: PayloadAction<Recording | null>) => {
      state.selectedRecording = action.payload;
    },
    setFilters: (state, action: PayloadAction<RecordingsState['filters']>) => {
      state.filters = { ...state.filters, ...action.payload };
    },
    clearFilters: (state) => {
      state.filters = {};
    },
    clearError: (state) => {
      state.error = null;
    },
    // WebSocket: добавление новой записи
    addRecordingFromWebSocket: (state, action: PayloadAction<Recording>) => {
      // Проверяем, соответствует ли запись текущим фильтрам
      const matchesFilters =
        (!state.filters.cameraId || state.filters.cameraId === action.payload.cameraId) &&
        (!state.filters.status || state.filters.status === action.payload.status) &&
        (!state.filters.startTime || action.payload.startTime >= state.filters.startTime) &&
        (!state.filters.endTime || action.payload.startTime <= state.filters.endTime);

      if (matchesFilters) {
        // Добавляем в начало списка (новые записи сначала)
        state.recordings.unshift(action.payload);
        // Ограничиваем размер списка
        if (state.recordings.length > state.pagination.limit * 2) {
          state.recordings = state.recordings.slice(0, state.pagination.limit);
        }
        state.pagination.total += 1;
      }
    },
    // WebSocket: обновление записи
    updateRecordingFromWebSocket: (state, action: PayloadAction<Recording>) => {
      const index = state.recordings.findIndex((r) => r.id === action.payload.id);
      if (index !== -1) {
        state.recordings[index] = action.payload;
      }
      if (state.selectedRecording?.id === action.payload.id) {
        state.selectedRecording = action.payload;
      }
    },
    // WebSocket: удаление записи
    removeRecordingFromWebSocket: (state, action: PayloadAction<string>) => {
      state.recordings = state.recordings.filter((r) => r.id !== action.payload);
      if (state.selectedRecording?.id === action.payload) {
        state.selectedRecording = null;
      }
      state.pagination.total = Math.max(0, state.pagination.total - 1);
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch recordings
      .addCase(fetchRecordings.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchRecordings.fulfilled, (state, action) => {
        state.loading = false;
        state.recordings = action.payload.items || action.payload;
        if (action.payload.page !== undefined) {
          state.pagination = {
            page: action.payload.page,
            limit: action.payload.limit,
            total: action.payload.total,
            hasMore: action.payload.hasMore || false,
          };
        }
      })
      .addCase(fetchRecordings.rejected, (state, action) => {
        state.loading = false;
        state.error = (action.payload as string) || 'Failed to fetch recordings';
      })
      // Fetch recording by ID
      .addCase(fetchRecordingById.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchRecordingById.fulfilled, (state, action) => {
        state.loading = false;
        state.selectedRecording = action.payload;
        // Обновляем запись в списке, если она там есть
        const index = state.recordings.findIndex((r) => r.id === action.payload.id);
        if (index !== -1) {
          state.recordings[index] = action.payload;
        }
      })
      .addCase(fetchRecordingById.rejected, (state, action) => {
        state.loading = false;
        state.error = (action.payload as string) || 'Failed to fetch recording';
      })
      // Delete recording
      .addCase(deleteRecording.fulfilled, (state, action) => {
        state.recordings = state.recordings.filter((r) => r.id !== action.payload);
        if (state.selectedRecording?.id === action.payload) {
          state.selectedRecording = null;
        }
        state.pagination.total = Math.max(0, state.pagination.total - 1);
      });
  },
});

export const {
  setSelectedRecording,
  setFilters,
  clearFilters,
  clearError,
  addRecordingFromWebSocket,
  updateRecordingFromWebSocket,
  removeRecordingFromWebSocket,
} = recordingsSlice.actions;

export default recordingsSlice.reducer;
