import reducer, {
  clearError,
  setConnected,
  setConnecting,
  setError,
  setLastMessage,
  subscribe,
  unsubscribe,
  type WebSocketState,
} from './websocketSlice';

describe('websocketSlice', () => {
  const initialState: WebSocketState = {
    connected: false,
    connecting: false,
    error: null,
    subscribedChannels: [],
    lastMessage: null,
  };

  it('sets connecting state and clears errors', () => {
    const stateWithError = { ...initialState, error: 'Network error' };
    const next = reducer(stateWithError, setConnecting(true));

    expect(next.connecting).toBe(true);
    expect(next.error).toBeNull();
  });

  it('subscribes once per channel and supports unsubscribe', () => {
    const subscribed = reducer(initialState, subscribe(['events', 'events', 'recordings']));
    expect(subscribed.subscribedChannels).toEqual(['events', 'recordings']);

    const unsubscribed = reducer(subscribed, unsubscribe(['events']));
    expect(unsubscribed.subscribedChannels).toEqual(['recordings']);
  });

  it('resets channels on disconnect and stores last message', () => {
    const withChannels = reducer(initialState, subscribe(['notifications']));
    const withMessage = reducer(
      withChannels,
      setLastMessage({ type: 'event.created', channel: 'events', data: { id: 'e-1' } })
    );
    expect(withMessage.lastMessage?.type).toBe('event.created');

    const disconnected = reducer(withMessage, setConnected(false));
    expect(disconnected.connected).toBe(false);
    expect(disconnected.subscribedChannels).toEqual([]);
  });

  it('sets and clears error', () => {
    const withError = reducer(initialState, setError('Socket failed'));
    expect(withError.error).toBe('Socket failed');
    expect(withError.connecting).toBe(false);

    const cleared = reducer(withError, clearError());
    expect(cleared.error).toBeNull();
  });
});
