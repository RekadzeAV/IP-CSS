import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { reportService } from '@/services/reportService';
import { isAxiosError } from 'axios';
import type { Report, ReportFilter, ExportFormat } from '@/types';

interface ReportsState {
  reports: Report[];
  selectedReport: Report | null;
  loading: boolean;
  error: string | null;
  exportLoading: boolean;
}

const initialState: ReportsState = {
  reports: [],
  selectedReport: null,
  loading: false,
  error: null,
  exportLoading: false,
};

const getErrorMessage = (error: unknown, fallback: string): string =>
  isAxiosError<{ message?: string }>(error)
    ? (error.response?.data?.message || error.message || fallback)
    : error instanceof Error
      ? error.message
      : fallback;

// Async thunks
export const fetchReports = createAsyncThunk(
  'reports/fetchReports',
  async (filter: ReportFilter, { rejectWithValue }) => {
    try {
      const reports = await reportService.getReports(filter);
      return reports;
    } catch (error: unknown) {
      return rejectWithValue(getErrorMessage(error, 'Failed to fetch reports'));
    }
  }
);

export const fetchReportById = createAsyncThunk(
  'reports/fetchReportById',
  async (id: string, { rejectWithValue }) => {
    try {
      const report = await reportService.getReportById(id);
      return report;
    } catch (error: unknown) {
      return rejectWithValue(getErrorMessage(error, 'Failed to fetch report'));
    }
  }
);

export const generateReport = createAsyncThunk(
  'reports/generateReport',
  async (data: Partial<Report>, { rejectWithValue }) => {
    try {
      const report = await reportService.generateReport(data);
      return report;
    } catch (error: unknown) {
      return rejectWithValue(getErrorMessage(error, 'Failed to generate report'));
    }
  }
);

export const exportReport = createAsyncThunk(
  'reports/exportReport',
  async ({ format }: { format: ExportFormat }, { rejectWithValue }) => {
    try {
      await reportService.exportReport(format);
      return format;
    } catch (error: unknown) {
      return rejectWithValue(getErrorMessage(error, 'Failed to export report'));
    }
  }
);

const reportsSlice = createSlice({
  name: 'reports',
  initialState,
  reducers: {
    setSelectedReport: (state, action: PayloadAction<Report | null>) => {
      state.selectedReport = action.payload;
    },
    clearError: (state) => {
      state.error = null;
    },
    clearSelectedReport: (state) => {
      state.selectedReport = null;
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch reports
      .addCase(fetchReports.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchReports.fulfilled, (state, action) => {
        state.loading = false;
        state.reports = action.payload;
      })
      .addCase(fetchReports.rejected, (state, action) => {
        state.loading = false;
        state.error = (action.payload as string) || 'Failed to fetch reports';
      })
      // Fetch report by ID
      .addCase(fetchReportById.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchReportById.fulfilled, (state, action) => {
        state.loading = false;
        state.selectedReport = action.payload;
      })
      .addCase(fetchReportById.rejected, (state, action) => {
        state.loading = false;
        state.error = (action.payload as string) || 'Failed to fetch report';
      })
      // Generate report
      .addCase(generateReport.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(generateReport.fulfilled, (state, action) => {
        state.loading = false;
        state.reports.unshift(action.payload);
      })
      .addCase(generateReport.rejected, (state, action) => {
        state.loading = false;
        state.error = (action.payload as string) || 'Failed to generate report';
      })
      // Export report
      .addCase(exportReport.pending, (state) => {
        state.exportLoading = true;
      })
      .addCase(exportReport.fulfilled, (state) => {
        state.exportLoading = false;
      })
      .addCase(exportReport.rejected, (state, action) => {
        state.exportLoading = false;
        state.error = (action.payload as string) || 'Failed to export report';
      });
  },
});

export const { setSelectedReport, clearError, clearSelectedReport } = reportsSlice.actions;
export default reportsSlice.reducer;
