import { useCallback, useEffect, useState } from 'react';
import { type EventDto, fetchEvents } from '../lib/api';
import { useDashboardWebSocket } from '../hooks/useDashboardWebSocket';

function formatTime(ts: number): string {
  return new Date(ts).toLocaleString();
}

export function Events() {
  const [events, setEvents] = useState<EventDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const { connected, lastLiveEvent } = useDashboardWebSocket(['events']);

  const loadEvents = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const page = await fetchEvents(1, 50);
      setEvents(page.items);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка загрузки событий');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadEvents();
  }, [loadEvents]);

  useEffect(() => {
    if (!lastLiveEvent) return;
    setEvents((prev) => {
      if (prev.some((e) => e.id === lastLiveEvent.id)) {
        return prev;
      }
      const incoming: EventDto = {
        id: lastLiveEvent.id,
        cameraId: lastLiveEvent.cameraId,
        type: lastLiveEvent.type,
        severity: lastLiveEvent.severity,
        timestamp: lastLiveEvent.timestamp,
        description: lastLiveEvent.description ?? null,
        acknowledged: false,
      };
      return [incoming, ...prev].slice(0, 100);
    });
  }, [lastLiveEvent]);

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white">События</h1>
          <p className="text-gray-600 dark:text-gray-400 mt-1">
            Лента событий системы и ONVIF
            <span
              className={`ml-2 inline-flex items-center text-xs px-2 py-0.5 rounded-full ${
                connected
                  ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
                  : 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300'
              }`}
            >
              {connected ? 'WebSocket подключён' : 'WebSocket отключён'}
            </span>
          </p>
        </div>
        <button
          type="button"
          onClick={() => void loadEvents()}
          className="px-4 py-2 rounded-lg bg-primary-600 text-white hover:bg-primary-700 transition"
        >
          Обновить
        </button>
      </div>

      {error && (
        <div className="mb-4 rounded-lg bg-red-50 dark:bg-red-900/30 text-red-700 dark:text-red-300 px-4 py-3">
          {error}
        </div>
      )}

      {loading ? (
        <p className="text-gray-600 dark:text-gray-400">Загрузка…</p>
      ) : events.length === 0 ? (
        <div className="rounded-lg bg-white dark:bg-gray-800 p-8 text-center shadow">
          <p className="text-gray-600 dark:text-gray-400">Событий пока нет</p>
        </div>
      ) : (
        <ul className="space-y-3">
          {events.map((event) => (
            <li
              key={event.id}
              className="rounded-lg bg-white dark:bg-gray-800 shadow p-4 flex flex-wrap gap-3 justify-between"
            >
              <div className="min-w-0 flex-1">
                <p className="font-medium text-gray-900 dark:text-white">{event.type}</p>
                {event.description && (
                  <p className="text-sm text-gray-600 dark:text-gray-400 mt-1">{event.description}</p>
                )}
                <p className="text-xs text-gray-500 dark:text-gray-500 mt-2">
                  Камера: {event.cameraName ?? event.cameraId}
                </p>
              </div>
              <div className="text-right shrink-0">
                <span
                  className={`text-xs px-2 py-1 rounded-full ${
                    event.severity === 'CRITICAL' || event.severity === 'ERROR'
                      ? 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
                      : 'bg-blue-100 text-blue-800 dark:bg-blue-900 dark:text-blue-200'
                  }`}
                >
                  {event.severity}
                </span>
                <p className="text-xs text-gray-500 dark:text-gray-500 mt-2">
                  {formatTime(event.timestamp)}
                </p>
                {event.acknowledged && (
                  <p className="text-xs text-green-600 dark:text-green-400 mt-1">Подтверждено</p>
                )}
              </div>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
