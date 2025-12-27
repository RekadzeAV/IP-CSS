import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { recordingService, RecordingFilters } from '@/services/recordingService';
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
  filters: RecordingFilters;
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

// Async thunks
export const fetchRecordings = createAsyncThunk(
  'recordings/fetchRecordings',
  async (filters?: RecordingFilters, { rejectWithValue }) => {
    try {
      const result = await recordingService.getRecordings(filters);
      return result;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to fetch recordings');
    }
  }
);

export const fetchRecordingById = createAsyncThunk(
  'recordings/fetchRecordingById',
  async (id: string, { rejectWithValue }) => {
    try {
      const recording = await recordingService.getRecordingById(id);
      return recording;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to fetch recording');
    }
  }
);

export const deleteRecording = createAsyncThunk(
  'recordings/deleteRecording',
  async (id: string, { rejectWithValue }) => {
    try {
      await recordingService.deleteRecording(id);
      return id;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to delete recording');
    }
  }
);

export const getDownloadUrl = createAsyncThunk(
  'recordings/getDownloadUrl',
  async (id: string, { rejectWithValue }) => {
    try {
      const url = await recordingService.getDownloadUrl(id);
      return { id, url };
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to get download URL');
    }
  }
);

export const exportRecording = createAsyncThunk(
  'recordings/exportRecording',
  async (params: { id: string; format?: string; quality?: string }, { rejectWithValue }) => {
    try {
      const url = await recordingService.exportRecording(params.id, params.format, params.quality);
      return { id: params.id, url };
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to export recording');
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
    setFilters: (state, action: PayloadAction<RecordingFilters>) => {
      state.filters = { ...state.filters, ...action.payload };
    },
    clearFilters: (state) => {
      state.filters = {};
    },
    clearError: (state) => {
      state.error = null;
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
        state.recordings = action.payload.items;
        state.pagination = {
          page: action.payload.page,
          limit: action.payload.limit,
          total: action.payload.total,
          hasMore: action.payload.hasMore,
        };
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

export const { setSelectedRecording, setFilters, clearFilters, clearError } = recordingsSlice.actions;
export default recordingsSlice.reducer;

