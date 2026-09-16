import { createSlice, PayloadAction } from '@reduxjs/toolkit';

export interface AnalyticsRealtimeEvent {
  cameraId: string;
  timestamp: number;
  resultType: string;
  detectors: string[];
  summary?: {
    motionDetected?: boolean;
    objectsCount?: number;
    facesCount?: number;
    platesCount?: number;
  };
}

interface AnalyticsState {
  recentEvents: AnalyticsRealtimeEvent[];
  maxRecentEvents: number;
}

const initialState: AnalyticsState = {
  recentEvents: [],
  maxRecentEvents: 50,
};

const analyticsSlice = createSlice({
  name: 'analytics',
  initialState,
  reducers: {
    addRealtimeEvent: (state, action: PayloadAction<AnalyticsRealtimeEvent>) => {
      const event = action.payload;
      state.recentEvents.unshift(event);
      if (state.recentEvents.length > state.maxRecentEvents) {
        state.recentEvents = state.recentEvents.slice(0, state.maxRecentEvents);
      }
    },
    clearRealtimeEvents: (state) => {
      state.recentEvents = [];
    },
  },
});

export const { addRealtimeEvent, clearRealtimeEvents } = analyticsSlice.actions;
export default analyticsSlice.reducer;

