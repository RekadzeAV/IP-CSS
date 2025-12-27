import { configureStore } from '@reduxjs/toolkit';
import authReducer from './slices/authSlice';
import camerasReducer from './slices/camerasSlice';
import eventsReducer from './slices/eventsSlice';
import recordingsReducer from './slices/recordingsSlice';
import settingsReducer from './slices/settingsSlice';
import websocketReducer from './slices/websocketSlice';
import notificationsReducer from './slices/notificationsSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    cameras: camerasReducer,
    events: eventsReducer,
    recordings: recordingsReducer,
    settings: settingsReducer,
    websocket: websocketReducer,
    notifications: notificationsReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;



