import { configureStore } from '@reduxjs/toolkit';
import authReducer from './slices/authSlice';
import camerasReducer from './slices/camerasSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    cameras: camerasReducer,
  },
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;

