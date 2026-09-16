import reducer, { addRealtimeEvent, clearRealtimeEvents } from './analyticsSlice';

describe('analyticsSlice', () => {
  it('adds realtime events and clears them', () => {
    const initial = reducer(undefined, { type: 'unknown' });
    const withEvent = reducer(
      initial,
      addRealtimeEvent({
        cameraId: 'cam-1',
        timestamp: 1710000000000,
        resultType: 'motion_detected',
        detectors: ['motion'],
        summary: { motionDetected: true, objectsCount: 0, facesCount: 0, platesCount: 0 },
      })
    );

    expect(withEvent.recentEvents).toHaveLength(1);
    expect(withEvent.recentEvents[0].cameraId).toBe('cam-1');

    const cleared = reducer(withEvent, clearRealtimeEvents());
    expect(cleared.recentEvents).toEqual([]);
  });
});

