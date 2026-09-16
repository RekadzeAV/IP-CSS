import { useCallback, useEffect, useState } from 'react';
import {
  type CameraDto,
  type DiscoveredCameraDto,
  discoverCameras,
  fetchCameras,
  normalizeCameraStatus,
} from '../lib/api';
import { HlsCameraPlayer } from '../components/video/HlsCameraPlayer';

export function Cameras() {
  const [cameras, setCameras] = useState<CameraDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [previewId, setPreviewId] = useState<string | null>(null);
  const [discovered, setDiscovered] = useState<DiscoveredCameraDto[]>([]);
  const [discovering, setDiscovering] = useState(false);
  const [discoverMessage, setDiscoverMessage] = useState('');

  const loadCameras = useCallback(async () => {
    setLoading(true);
    setError('');
    try {
      const items = await fetchCameras();
      setCameras(items);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Ошибка загрузки камер');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    void loadCameras();
  }, [loadCameras]);

  return (
    <div>
      <div className="mb-6 flex flex-wrap items-center justify-between gap-4">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 dark:text-white">Камеры</h1>
          <p className="text-gray-600 dark:text-gray-400 mt-1">
            Список камер и live-превью через HLS (RTSP → сервер)
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            disabled={discovering}
            onClick={async () => {
              setDiscovering(true);
              setDiscoverMessage('');
              try {
                const found = await discoverCameras(true);
                setDiscovered(found);
                setDiscoverMessage(
                  found.length > 0
                    ? `Найдено в сети: ${found.length}`
                    : 'Камеры в сети не обнаружены (проверьте ONVIF и конфиг fallback)',
                );
              } catch (err) {
                setDiscoverMessage(
                  err instanceof Error ? err.message : 'Ошибка обнаружения',
                );
              } finally {
                setDiscovering(false);
              }
            }}
            className="px-4 py-2 rounded-lg border border-primary-600 text-primary-600 hover:bg-primary-50 dark:hover:bg-primary-900/20 transition disabled:opacity-50"
          >
            {discovering ? 'Поиск…' : 'ONVIF Discover'}
          </button>
          <button
            type="button"
            onClick={() => void loadCameras()}
            className="px-4 py-2 rounded-lg bg-primary-600 text-white hover:bg-primary-700 transition"
          >
            Обновить
          </button>
        </div>
      </div>

      {discoverMessage && (
        <div className="mb-4 rounded-lg bg-blue-50 dark:bg-blue-900/30 text-blue-800 dark:text-blue-200 px-4 py-3">
          {discoverMessage}
        </div>
      )}

      {discovered.length > 0 && (
        <section className="mb-6 rounded-lg bg-white dark:bg-gray-800 shadow p-4">
          <h2 className="text-lg font-semibold text-gray-900 dark:text-white mb-3">
            Обнаружено в сети
          </h2>
          <ul className="divide-y divide-gray-200 dark:divide-gray-700">
            {discovered.map((cam) => (
              <li key={`${cam.ipAddress}:${cam.port}`} className="py-3 flex flex-wrap gap-2 justify-between">
                <div>
                  <p className="font-medium text-gray-900 dark:text-white">{cam.name}</p>
                  <p className="text-sm text-gray-500 break-all">{cam.url}</p>
                  <p className="text-xs text-gray-500">
                    {cam.manufacturer ?? '—'} · {cam.model ?? '—'} · {cam.ipAddress}:{cam.port}
                  </p>
                </div>
              </li>
            ))}
          </ul>
          <p className="text-xs text-gray-500 mt-3">
            Добавление камеры в систему — через основной веб-клиент или API POST /api/v1/cameras
          </p>
        </section>
      )}

      {error && (
        <div className="mb-4 rounded-lg bg-red-50 dark:bg-red-900/30 text-red-700 dark:text-red-300 px-4 py-3">
          {error}
        </div>
      )}

      {loading ? (
        <p className="text-gray-600 dark:text-gray-400">Загрузка…</p>
      ) : cameras.length === 0 ? (
        <div className="rounded-lg bg-white dark:bg-gray-800 p-8 text-center shadow">
          <p className="text-gray-600 dark:text-gray-400">Камеры не найдены</p>
          <p className="text-sm text-gray-500 dark:text-gray-500 mt-2">
            Добавьте камеру через API или основной веб-клиент
          </p>
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          {cameras.map((camera) => {
            const status = normalizeCameraStatus(camera.status);
            const resolution = camera.resolution
              ? `${camera.resolution.width}×${camera.resolution.height}`
              : '—';

            return (
              <article
                key={camera.id}
                className="rounded-lg bg-white dark:bg-gray-800 shadow overflow-hidden"
              >
                {previewId === camera.id ? (
                  <HlsCameraPlayer cameraId={camera.id} cameraName={camera.name} />
                ) : (
                  <div className="aspect-video bg-gray-200 dark:bg-gray-700 flex items-center justify-center">
                    <button
                      type="button"
                      onClick={() => setPreviewId(camera.id)}
                      className="px-4 py-2 rounded-lg bg-primary-600 text-white hover:bg-primary-700"
                    >
                      Открыть превью
                    </button>
                  </div>
                )}

                <div className="p-4 space-y-2">
                  <div className="flex items-start justify-between gap-2">
                    <h2 className="text-lg font-semibold text-gray-900 dark:text-white">
                      {camera.name}
                    </h2>
                    <span
                      className={`text-xs px-2 py-1 rounded-full shrink-0 ${
                        status === 'online'
                          ? 'bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-200'
                          : status === 'error'
                            ? 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200'
                            : 'bg-gray-100 text-gray-800 dark:bg-gray-700 dark:text-gray-200'
                      }`}
                    >
                      {camera.status}
                    </span>
                  </div>
                  <p className="text-sm text-gray-500 dark:text-gray-400 break-all">{camera.url}</p>
                  <p className="text-xs text-gray-500 dark:text-gray-500">
                    {resolution} · {camera.fps ?? '—'} FPS · {camera.codec ?? '—'}
                  </p>
                  {previewId === camera.id && (
                    <button
                      type="button"
                      onClick={() => setPreviewId(null)}
                      className="text-sm text-primary-600 hover:underline"
                    >
                      Скрыть превью
                    </button>
                  )}
                </div>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
}
