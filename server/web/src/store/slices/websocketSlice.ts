import { createSlice, PayloadAction } from '@reduxjs/toolkit';
import type { WebSocketChannel, WebSocketMessage } from '@/utils/websocket';

export interface WebSocketState {
  connected: boolean;
  connecting: boolean;
  error: string | null;
  subscribedChannels: WebSocketChannel[];
  lastMessage: WebSocketMessage | null;
}

const initialState: WebSocketState = {
  connected: false,
  connecting: false,
  error: null,
  subscribedChannels: [],
  lastMessage: null,
};

const websocketSlice = createSlice({
  name: 'websocket',
  initialState,
  reducers: {
    setConnecting: (state, action: PayloadAction<boolean>) => {
      state.connecting = action.payload;
      if (action.payload) {
        state.error = null;
      }
    },
    setConnected: (state, action: PayloadAction<boolean>) => {
      state.connected = action.payload;
      state.connecting = false;
      if (!action.payload) {
        state.subscribedChannels = [];
      }
    },
    setError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload;
      state.connecting = false;
    },
    subscribe: (state, action: PayloadAction<WebSocketChannel[]>) => {
      action.payload.forEach((channel) => {
        if (!state.subscribedChannels.includes(channel)) {
          state.subscribedChannels.push(channel);
        }
      });
    },
    unsubscribe: (state, action: PayloadAction<WebSocketChannel[]>) => {
      state.subscribedChannels = state.subscribedChannels.filter(
        (channel) => !action.payload.includes(channel)
      );
    },
    setLastMessage: (state, action: PayloadAction<WebSocketMessage>) => {
      state.lastMessage = action.payload;
    },
    clearError: (state) => {
      state.error = null;
    },
  },
});

export const {
  setConnecting,
  setConnected,
  setError,
  subscribe,
  unsubscribe,
  setLastMessage,
  clearError,
} = websocketSlice.actions;

export default websocketSlice.reducer;

