import { configureStore } from '@reduxjs/toolkit';
import { authListenerMiddleware } from './authListenerMiddleware';
import authReducer from './slices/authSlice';
import camerasReducer from './slices/camerasSlice';
import eventsReducer from './slices/eventsSlice';
import recordingsReducer from './slices/recordingsSlice';
import settingsReducer from './slices/settingsSlice';
import websocketReducer from './slices/websocketSlice';
import notificationsReducer from './slices/notificationsSlice';
import streamsReducer from './slices/streamsSlice';
import analyticsReducer from './slices/analyticsSlice';
import reportsReducer from './slices/reportsSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    cameras: camerasReducer,
    events: eventsReducer,
    recordings: recordingsReducer,
    settings: settingsReducer,
    websocket: websocketReducer,
    notifications: notificationsReducer,
    streams: streamsReducer,
    analytics: analyticsReducer,
    reports: reportsReducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware().prepend(authListenerMiddleware.middleware),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;



