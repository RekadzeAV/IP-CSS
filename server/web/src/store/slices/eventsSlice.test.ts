import eventsReducer, {
  addEventFromWebSocket,
  setFilters,
} from '@/store/slices/eventsSlice';
import { EventSeverity, EventType, type Event } from '@/types';

const baseEvent = (id: string, overrides: Partial<Event> = {}): Event => ({
  id,
  cameraId: 'cam-1',
  cameraName: 'Camera 1',
  type: EventType.MOTION_DETECTION,
  severity: EventSeverity.INFO,
  timestamp: 1_700_000_000_000,
  description: 'Motion',
  metadata: {},
  acknowledged: false,
  ...overrides,
});

describe('eventsSlice websocket regression', () => {
  it('keeps active filters and appends only matching realtime events', () => {
    const withFilters = eventsReducer(undefined, setFilters({ cameraId: 'cam-1' }));

    const sameCameraState = eventsReducer(
      withFilters,
      addEventFromWebSocket(baseEvent('evt-1', { cameraId: 'cam-1' }))
    );
    const otherCameraState = eventsReducer(
      sameCameraState,
      addEventFromWebSocket(baseEvent('evt-2', { cameraId: 'cam-2' }))
    );

    expect(otherCameraState.filters.cameraId).toBe('cam-1');
    expect(otherCameraState.events).toHaveLength(1);
    expect(otherCameraState.events[0].id).toBe('evt-1');
  });

  it('prepends new realtime events preserving descending recency', () => {
    const state1 = eventsReducer(undefined, addEventFromWebSocket(baseEvent('evt-1', { timestamp: 100 })));
    const state2 = eventsReducer(state1, addEventFromWebSocket(baseEvent('evt-2', { timestamp: 200 })));

    expect(state2.events.map((it) => it.id)).toEqual(['evt-2', 'evt-1']);
  });

  it('does not duplicate event after websocket reconnect replay', () => {
    const state1 = eventsReducer(undefined, addEventFromWebSocket(baseEvent('evt-1')));
    const state2 = eventsReducer(state1, addEventFromWebSocket(baseEvent('evt-1')));

    expect(state2.events).toHaveLength(1);
    expect(state2.events[0].id).toBe('evt-1');
  });
});
