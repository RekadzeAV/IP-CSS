import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { cameraService } from '@/services/cameraService';
import type {
  Camera,
  CreateCameraRequest,
  UpdateCameraRequest,
  ConnectionTestResultDto,
  DiscoveredCameraDto,
} from '@/types';

interface CamerasState {
  cameras: Camera[];
  selectedCamera: Camera | null;
  discoveredCameras: DiscoveredCameraDto[];
  loading: boolean;
  error: string | null;
  connectionTest: ConnectionTestResultDto | null;
}

const initialState: CamerasState = {
  cameras: [],
  selectedCamera: null,
  discoveredCameras: [],
  loading: false,
  error: null,
  connectionTest: null,
};

// Async thunks
export const fetchCameras = createAsyncThunk(
  'cameras/fetchCameras',
  async (_, { rejectWithValue }) => {
    try {
      const cameras = await cameraService.getCameras();
      return cameras;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to fetch cameras');
    }
  }
);

export const fetchCameraById = createAsyncThunk(
  'cameras/fetchCameraById',
  async (id: string, { rejectWithValue }) => {
    try {
      const camera = await cameraService.getCameraById(id);
      return camera;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to fetch camera');
    }
  }
);

export const createCamera = createAsyncThunk(
  'cameras/createCamera',
  async (data: CreateCameraRequest, { rejectWithValue }) => {
    try {
      const camera = await cameraService.createCamera(data);
      return camera;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to create camera');
    }
  }
);

export const updateCamera = createAsyncThunk(
  'cameras/updateCamera',
  async ({ id, data }: { id: string; data: UpdateCameraRequest }, { rejectWithValue }) => {
    try {
      const camera = await cameraService.updateCamera(id, data);
      return camera;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to update camera');
    }
  }
);

export const deleteCamera = createAsyncThunk(
  'cameras/deleteCamera',
  async (id: string, { rejectWithValue }) => {
    try {
      await cameraService.deleteCamera(id);
      return id;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to delete camera');
    }
  }
);

export const testConnection = createAsyncThunk(
  'cameras/testConnection',
  async (id: string, { rejectWithValue }) => {
    try {
      const result = await cameraService.testConnection(id);
      return result;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to test connection');
    }
  }
);

export const discoverCameras = createAsyncThunk(
  'cameras/discoverCameras',
  async (_, { rejectWithValue }) => {
    try {
      const cameras = await cameraService.discoverCameras();
      return cameras;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to discover cameras');
    }
  }
);

const camerasSlice = createSlice({
  name: 'cameras',
  initialState,
  reducers: {
    setSelectedCamera: (state, action: PayloadAction<Camera | null>) => {
      state.selectedCamera = action.payload;
    },
    clearError: (state) => {
      state.error = null;
    },
    clearConnectionTest: (state) => {
      state.connectionTest = null;
    },
    // WebSocket обновление статуса камеры
    updateCameraStatus: (state, action: PayloadAction<{ cameraId: string; status: string }>) => {
      const camera = state.cameras.find((c) => c.id === action.payload.cameraId);
      if (camera) {
        camera.status = action.payload.status as any;
      }
      if (state.selectedCamera?.id === action.payload.cameraId) {
        state.selectedCamera.status = action.payload.status as any;
      }
    },
    // WebSocket обновление камеры
    updateCameraFromWebSocket: (state, action: PayloadAction<Camera>) => {
      const index = state.cameras.findIndex((c) => c.id === action.payload.id);
      if (index !== -1) {
        state.cameras[index] = action.payload;
      }
      if (state.selectedCamera?.id === action.payload.id) {
        state.selectedCamera = action.payload;
      }
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch cameras
      .addCase(fetchCameras.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchCameras.fulfilled, (state, action) => {
        state.loading = false;
        state.cameras = action.payload;
      })
      .addCase(fetchCameras.rejected, (state, action) => {
        state.loading = false;
        state.error = (action.payload as string) || 'Failed to fetch cameras';
      })
      // Fetch camera by ID
      .addCase(fetchCameraById.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchCameraById.fulfilled, (state, action) => {
        state.loading = false;
        state.selectedCamera = action.payload;
        // Обновляем камеру в списке, если она там есть
        const index = state.cameras.findIndex((c) => c.id === action.payload.id);
        if (index !== -1) {
          state.cameras[index] = action.payload;
        }
      })
      .addCase(fetchCameraById.rejected, (state, action) => {
        state.loading = false;
        state.error = (action.payload as string) || 'Failed to fetch camera';
      })
      // Create camera
      .addCase(createCamera.fulfilled, (state, action) => {
        state.cameras.push(action.payload);
      })
      // Update camera
      .addCase(updateCamera.fulfilled, (state, action) => {
        const index = state.cameras.findIndex((c) => c.id === action.payload.id);
        if (index !== -1) {
          state.cameras[index] = action.payload;
        }
        if (state.selectedCamera?.id === action.payload.id) {
          state.selectedCamera = action.payload;
        }
      })
      // Delete camera
      .addCase(deleteCamera.fulfilled, (state, action) => {
        state.cameras = state.cameras.filter((c) => c.id !== action.payload);
        if (state.selectedCamera?.id === action.payload) {
          state.selectedCamera = null;
        }
      })
      // Test connection
      .addCase(testConnection.pending, (state) => {
        state.connectionTest = null;
      })
      .addCase(testConnection.fulfilled, (state, action) => {
        state.connectionTest = action.payload;
      })
      // Discover cameras
      .addCase(discoverCameras.pending, (state) => {
        state.discoveredCameras = [];
      })
      .addCase(discoverCameras.fulfilled, (state, action) => {
        state.discoveredCameras = action.payload;
      });
  },
});

export const {
  setSelectedCamera,
  clearError,
  clearConnectionTest,
  updateCameraStatus,
  updateCameraFromWebSocket,
} = camerasSlice.actions;
export default camerasSlice.reducer;


