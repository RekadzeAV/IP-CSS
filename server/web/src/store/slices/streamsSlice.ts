import { createSlice, PayloadAction } from '@reduxjs/toolkit';

export interface StreamStatus {
  cameraId: string;
  active: boolean;
  streamId: string | null;
  hlsUrl: string | null;
  rtspUrl: string | null;
  quality: 'low' | 'medium' | 'high' | 'ultra';
  lastUpdate: number;
  /** Сервер: в настройках камеры включена хотя бы одна аналитика (WS stream_started). */
  serverAnalyticsRequested?: boolean;
  /** Сервер: пайплайн аналитики реально запущен на кадрах RtspClient (не HLS-only). */
  serverAnalyticsPipelineActive?: boolean;
  /** Сервер: есть ли источник кадров RTSP для аналитики на JVM. */
  serverHasRtspFrameSource?: boolean;
}

interface StreamsState {
  streams: Record<string, StreamStatus>;
  loading: Record<string, boolean>;
  errors: Record<string, string | null>;
}

const initialState: StreamsState = {
  streams: {},
  loading: {},
  errors: {},
};

const streamsSlice = createSlice({
  name: 'streams',
  initialState,
  reducers: {
    setStreamStatus: (state, action: PayloadAction<StreamStatus>) => {
      const { cameraId } = action.payload;
      state.streams[cameraId] = {
        ...action.payload,
        lastUpdate: Date.now(),
      };
    },
    updateStreamStatus: (
      state,
      action: PayloadAction<Partial<StreamStatus> & { cameraId: string }>
    ) => {
      const { cameraId, ...updates } = action.payload;
      if (state.streams[cameraId]) {
        state.streams[cameraId] = {
          ...state.streams[cameraId],
          ...updates,
          lastUpdate: Date.now(),
        };
      } else {
        state.streams[cameraId] = {
          cameraId,
          active: false,
          streamId: null,
          hlsUrl: null,
          rtspUrl: null,
          quality: 'medium',
          lastUpdate: Date.now(),
          ...updates,
        };
      }
    },
    setStreamLoading: (
      state,
      action: PayloadAction<{ cameraId: string; loading: boolean }>
    ) => {
      state.loading[action.payload.cameraId] = action.payload.loading;
    },
    setStreamError: (
      state,
      action: PayloadAction<{ cameraId: string; error: string | null }>
    ) => {
      state.errors[action.payload.cameraId] = action.payload.error;
    },
    removeStream: (state, action: PayloadAction<string>) => {
      const cameraId = action.payload;
      delete state.streams[cameraId];
      delete state.loading[cameraId];
      delete state.errors[cameraId];
    },
    // WebSocket обновления
    updateStreamFromWebSocket: (
      state,
      action: PayloadAction<{
        cameraId: string;
        streamId?: string;
        active?: boolean;
        event: 'stream_started' | 'stream_stopped';
        serverAnalyticsRequested?: boolean;
        serverAnalyticsPipelineActive?: boolean;
        serverHasRtspFrameSource?: boolean;
      }>
    ) => {
      const {
        cameraId,
        streamId,
        active,
        event,
        serverAnalyticsRequested,
        serverAnalyticsPipelineActive,
        serverHasRtspFrameSource,
      } = action.payload;
      const isActive = active ?? event === 'stream_started';
      const clearServerAnalytics = event === 'stream_stopped';

      if (state.streams[cameraId]) {
        const row = state.streams[cameraId];
        row.active = isActive;
        row.streamId = streamId ?? row.streamId;
        row.lastUpdate = Date.now();
        if (clearServerAnalytics) {
          delete row.serverAnalyticsRequested;
          delete row.serverAnalyticsPipelineActive;
          delete row.serverHasRtspFrameSource;
        } else {
          if (serverAnalyticsRequested !== undefined) {
            row.serverAnalyticsRequested = serverAnalyticsRequested;
          }
          if (serverAnalyticsPipelineActive !== undefined) {
            row.serverAnalyticsPipelineActive = serverAnalyticsPipelineActive;
          }
          if (serverHasRtspFrameSource !== undefined) {
            row.serverHasRtspFrameSource = serverHasRtspFrameSource;
          }
        }
      } else {
        state.streams[cameraId] = {
          cameraId,
          active: isActive,
          streamId: streamId ?? null,
          hlsUrl: null,
          rtspUrl: null,
          quality: 'medium',
          lastUpdate: Date.now(),
        };
        if (!clearServerAnalytics) {
          const row = state.streams[cameraId];
          if (serverAnalyticsRequested !== undefined) {
            row.serverAnalyticsRequested = serverAnalyticsRequested;
          }
          if (serverAnalyticsPipelineActive !== undefined) {
            row.serverAnalyticsPipelineActive = serverAnalyticsPipelineActive;
          }
          if (serverHasRtspFrameSource !== undefined) {
            row.serverHasRtspFrameSource = serverHasRtspFrameSource;
          }
        }
      }

      // Очищаем ошибку при успешном запуске
      if (isActive) {
        state.errors[cameraId] = null;
      }
    },
  },
});

export const {
  setStreamStatus,
  updateStreamStatus,
  setStreamLoading,
  setStreamError,
  removeStream,
  updateStreamFromWebSocket,
} = streamsSlice.actions;

export default streamsSlice.reducer;
