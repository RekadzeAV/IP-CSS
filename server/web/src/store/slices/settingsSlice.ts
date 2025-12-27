import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { settingsService, SettingsDto } from '@/services/settingsService';
import type { SystemSettings } from '@/types';

interface SettingsState {
  settings: SettingsDto[];
  systemSettings: SystemSettings | null;
  loading: boolean;
  error: string | null;
  themeMode: 'light' | 'dark';
}

const getInitialThemeMode = (): 'light' | 'dark' => {
  if (typeof window !== 'undefined') {
    const saved = localStorage.getItem('themeMode');
    if (saved === 'light' || saved === 'dark') {
      return saved;
    }
  }
  return 'dark';
};

const initialState: SettingsState = {
  settings: [],
  systemSettings: null,
  loading: false,
  error: null,
  themeMode: getInitialThemeMode(),
};

// Async thunks
export const fetchSettings = createAsyncThunk(
  'settings/fetchSettings',
  async (category?: string, { rejectWithValue }) => {
    try {
      const settings = await settingsService.getSettings(category);
      return settings;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to fetch settings');
    }
  }
);

export const fetchSystemSettings = createAsyncThunk(
  'settings/fetchSystemSettings',
  async (_, { rejectWithValue }) => {
    try {
      const settings = await settingsService.getSystemSettings();
      return settings;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to fetch system settings');
    }
  }
);

export const updateSettings = createAsyncThunk(
  'settings/updateSettings',
  async (settings: Record<string, string>, { rejectWithValue }) => {
    try {
      await settingsService.updateSettings(settings);
      // После обновления перезагружаем настройки
      const updated = await settingsService.getSettings();
      return updated;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to update settings');
    }
  }
);

export const updateSetting = createAsyncThunk(
  'settings/updateSetting',
  async ({ key, value }: { key: string; value: string }, { rejectWithValue }) => {
    try {
      const setting = await settingsService.updateSetting(key, value);
      return setting;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to update setting');
    }
  }
);

export const deleteSetting = createAsyncThunk(
  'settings/deleteSetting',
  async (key: string, { rejectWithValue }) => {
    try {
      await settingsService.deleteSetting(key);
      return key;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to delete setting');
    }
  }
);

export const exportSettings = createAsyncThunk(
  'settings/exportSettings',
  async (_, { rejectWithValue }) => {
    try {
      const exported = await settingsService.exportSettings();
      return exported;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to export settings');
    }
  }
);

export const importSettings = createAsyncThunk(
  'settings/importSettings',
  async (settings: Record<string, string>, { rejectWithValue }) => {
    try {
      await settingsService.importSettings(settings);
      // После импорта перезагружаем настройки
      const updated = await settingsService.getSettings();
      return updated;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to import settings');
    }
  }
);

export const resetSettings = createAsyncThunk(
  'settings/resetSettings',
  async (category?: string, { rejectWithValue }) => {
    try {
      await settingsService.resetSettings(category);
      // После сброса перезагружаем настройки
      const updated = await settingsService.getSettings(category);
      return updated;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to reset settings');
    }
  }
);

const settingsSlice = createSlice({
  name: 'settings',
  initialState,
  reducers: {
    clearError: (state) => {
      state.error = null;
    },
    toggleTheme: (state) => {
      state.themeMode = state.themeMode === 'light' ? 'dark' : 'light';
    },
    setThemeMode: (state, action: PayloadAction<'light' | 'dark'>) => {
      state.themeMode = action.payload;
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch settings
      .addCase(fetchSettings.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchSettings.fulfilled, (state, action) => {
        state.loading = false;
        state.settings = action.payload;
      })
      .addCase(fetchSettings.rejected, (state, action) => {
        state.loading = false;
        state.error = (action.payload as string) || 'Failed to fetch settings';
      })
      // Fetch system settings
      .addCase(fetchSystemSettings.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchSystemSettings.fulfilled, (state, action) => {
        state.loading = false;
        state.systemSettings = action.payload;
      })
      .addCase(fetchSystemSettings.rejected, (state, action) => {
        state.loading = false;
        state.error = (action.payload as string) || 'Failed to fetch system settings';
      })
      // Update settings
      .addCase(updateSettings.fulfilled, (state, action) => {
        state.settings = action.payload;
      })
      // Update setting
      .addCase(updateSetting.fulfilled, (state, action) => {
        const index = state.settings.findIndex((s) => s.key === action.payload.key);
        if (index !== -1) {
          state.settings[index] = action.payload;
        } else {
          state.settings.push(action.payload);
        }
      })
      // Delete setting
      .addCase(deleteSetting.fulfilled, (state, action) => {
        state.settings = state.settings.filter((s) => s.key !== action.payload);
      })
      // Import settings
      .addCase(importSettings.fulfilled, (state, action) => {
        state.settings = action.payload;
      })
      // Reset settings
      .addCase(resetSettings.fulfilled, (state, action) => {
        state.settings = action.payload;
      });
  },
});

export const { clearError, toggleTheme, setThemeMode } = settingsSlice.actions;
export default settingsSlice.reducer;

