import { createSlice, createAsyncThunk, PayloadAction } from '@reduxjs/toolkit';
import { eventService, EventFilters, EventStatistics } from '@/services/eventService';
import type { Event } from '@/types';

interface EventsState {
  events: Event[];
  selectedEvent: Event | null;
  statistics: EventStatistics | null;
  loading: boolean;
  error: string | null;
  pagination: {
    page: number;
    limit: number;
    total: number;
    hasMore: boolean;
  };
  filters: EventFilters;
}

const initialState: EventsState = {
  events: [],
  selectedEvent: null,
  statistics: null,
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
export const fetchEvents = createAsyncThunk(
  'events/fetchEvents',
  async (filters?: EventFilters, { rejectWithValue }) => {
    try {
      const result = await eventService.getEvents(filters);
      return result;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to fetch events');
    }
  }
);

export const fetchEventById = createAsyncThunk(
  'events/fetchEventById',
  async (id: string, { rejectWithValue }) => {
    try {
      const event = await eventService.getEventById(id);
      return event;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to fetch event');
    }
  }
);

export const deleteEvent = createAsyncThunk(
  'events/deleteEvent',
  async (id: string, { rejectWithValue }) => {
    try {
      await eventService.deleteEvent(id);
      return id;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to delete event');
    }
  }
);

export const acknowledgeEvent = createAsyncThunk(
  'events/acknowledgeEvent',
  async (id: string, { rejectWithValue }) => {
    try {
      const event = await eventService.acknowledgeEvent(id);
      return event;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to acknowledge event');
    }
  }
);

export const acknowledgeEvents = createAsyncThunk(
  'events/acknowledgeEvents',
  async (ids: string[], { rejectWithValue }) => {
    try {
      const events = await eventService.acknowledgeEvents(ids);
      return events;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to acknowledge events');
    }
  }
);

export const fetchStatistics = createAsyncThunk(
  'events/fetchStatistics',
  async (params: { cameraId?: string; startTime?: number; endTime?: number }, { rejectWithValue }) => {
    try {
      const statistics = await eventService.getStatistics(params.cameraId, params.startTime, params.endTime);
      return statistics;
    } catch (error: any) {
      return rejectWithValue(error.message || 'Failed to fetch statistics');
    }
  }
);

const eventsSlice = createSlice({
  name: 'events',
  initialState,
  reducers: {
    setSelectedEvent: (state, action: PayloadAction<Event | null>) => {
      state.selectedEvent = action.payload;
    },
    setFilters: (state, action: PayloadAction<EventFilters>) => {
      state.filters = { ...state.filters, ...action.payload };
    },
    clearFilters: (state) => {
      state.filters = {};
    },
    clearError: (state) => {
      state.error = null;
    },
    // WebSocket: добавление нового события
    addEventFromWebSocket: (state, action: PayloadAction<Event>) => {
      // Проверяем, соответствует ли событие текущим фильтрам
      const matchesFilters = 
        (!state.filters.type || state.filters.type === action.payload.type) &&
        (!state.filters.cameraId || state.filters.cameraId === action.payload.cameraId) &&
        (!state.filters.severity || state.filters.severity === action.payload.severity) &&
        (state.filters.acknowledged === undefined || state.filters.acknowledged === action.payload.acknowledged);

      if (matchesFilters) {
        // Добавляем в начало списка (новые события сначала)
        state.events.unshift(action.payload);
        // Ограничиваем размер списка
        if (state.events.length > state.pagination.limit * 2) {
          state.events = state.events.slice(0, state.pagination.limit);
        }
        state.pagination.total += 1;
      }
    },
    // WebSocket: обновление события
    updateEventFromWebSocket: (state, action: PayloadAction<Event>) => {
      const index = state.events.findIndex((e) => e.id === action.payload.id);
      if (index !== -1) {
        state.events[index] = action.payload;
      }
      if (state.selectedEvent?.id === action.payload.id) {
        state.selectedEvent = action.payload;
      }
    },
  },
  extraReducers: (builder) => {
    builder
      // Fetch events
      .addCase(fetchEvents.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchEvents.fulfilled, (state, action) => {
        state.loading = false;
        state.events = action.payload.items;
        state.pagination = {
          page: action.payload.page,
          limit: action.payload.limit,
          total: action.payload.total,
          hasMore: action.payload.hasMore,
        };
      })
      .addCase(fetchEvents.rejected, (state, action) => {
        state.loading = false;
        state.error = (action.payload as string) || 'Failed to fetch events';
      })
      // Fetch event by ID
      .addCase(fetchEventById.pending, (state) => {
        state.loading = true;
        state.error = null;
      })
      .addCase(fetchEventById.fulfilled, (state, action) => {
        state.loading = false;
        state.selectedEvent = action.payload;
        // Обновляем событие в списке, если оно там есть
        const index = state.events.findIndex((e) => e.id === action.payload.id);
        if (index !== -1) {
          state.events[index] = action.payload;
        }
      })
      .addCase(fetchEventById.rejected, (state, action) => {
        state.loading = false;
        state.error = (action.payload as string) || 'Failed to fetch event';
      })
      // Delete event
      .addCase(deleteEvent.fulfilled, (state, action) => {
        state.events = state.events.filter((e) => e.id !== action.payload);
        if (state.selectedEvent?.id === action.payload) {
          state.selectedEvent = null;
        }
        state.pagination.total = Math.max(0, state.pagination.total - 1);
      })
      // Acknowledge event
      .addCase(acknowledgeEvent.fulfilled, (state, action) => {
        const index = state.events.findIndex((e) => e.id === action.payload.id);
        if (index !== -1) {
          state.events[index] = action.payload;
        }
        if (state.selectedEvent?.id === action.payload.id) {
          state.selectedEvent = action.payload;
        }
      })
      // Acknowledge events
      .addCase(acknowledgeEvents.fulfilled, (state, action) => {
        action.payload.forEach((event) => {
          const index = state.events.findIndex((e) => e.id === event.id);
          if (index !== -1) {
            state.events[index] = event;
          }
        });
        if (state.selectedEvent) {
          const updated = action.payload.find((e) => e.id === state.selectedEvent?.id);
          if (updated) {
            state.selectedEvent = updated;
          }
        }
      })
      // Fetch statistics
      .addCase(fetchStatistics.fulfilled, (state, action) => {
        state.statistics = action.payload;
      });
  },
});

export const {
  setSelectedEvent,
  setFilters,
  clearFilters,
  clearError,
  addEventFromWebSocket,
  updateEventFromWebSocket,
} = eventsSlice.actions;
export default eventsSlice.reducer;

