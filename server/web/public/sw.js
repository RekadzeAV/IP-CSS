/* eslint-disable no-restricted-globals */

const CACHE_NAME = 'ipcss-v1';
const STATIC_ASSETS = [
  '/',
  '/manifest.json',
  '/offline.html',
  '/_next/static/css/app.css',
  '/_next/static/js/app.js',
];

// Установка Service Worker
self.addEventListener('install', (event) => {
  console.log('[ServiceWorker] Install');
  event.waitUntil(
    caches.open(CACHE_NAME).then((cache) => {
      console.log('[ServiceWorker] Pre-caching static assets');
      return cache.addAll(STATIC_ASSETS);
    })
  );
  // Skip waiting для немедленной активации
  self.skipWaiting();
});

// Активация Service Worker
self.addEventListener('activate', (event) => {
  console.log('[ServiceWorker] Activate');
  event.waitUntil(
    caches.keys().then((cacheNames) => {
      return Promise.all(
        cacheNames.map((cacheName) => {
          if (cacheName !== CACHE_NAME) {
            console.log('[ServiceWorker] Removing old cache', cacheName);
            return caches.delete(cacheName);
          }
        })
      );
    })
  );
  // Claim всех клиентов немедленно
  self.clients.claim();
});

// Интерцепция запросов
self.addEventListener('fetch', (event) => {
  // Пропускаем non-GET запросы
  if (event.request.method !== 'GET') {
    return;
  }

  // Пропускаем WebSocket и другие специальные протоколы
  const requestUrl = new URL(event.request.url);
  if (
    requestUrl.protocol === 'ws:' ||
    requestUrl.protocol === 'wss:' ||
    requestUrl.pathname.startsWith('/api/v1/ws')
  ) {
    return;
  }

  // Strategy: Cache First, falling back to network
  event.respondWith(
    caches.match(event.request).then((cachedResponse) => {
      if (cachedResponse) {
        // Возвращаем кэшированную версию и обновляем кэш в фоне
        fetchAndCache(event.request);
        return cachedResponse;
      }

      // Если не в кэше, запрашиваем с сети
      return fetch(event.request).then((response) => {
        // Проверяем валидный ответ
        if (!response || response.status !== 200 || response.type !== 'basic') {
          return response;
        }

        // Клонируем ответ для кэширования
        const responseToCache = response.clone();
        caches.open(CACHE_NAME).then((cache) => {
          cache.put(event.request, responseToCache);
        });

        return response;
      }).catch(() => {
        // Если сеть недоступна, возвращаем offline страницу для HTML
        if (event.request.headers.get('accept').includes('text/html')) {
          return caches.match('/offline.html');
        }
      });
    })
  );
});

// Функция для кэширования в фоне
async function fetchAndCache(request) {
  try {
    const response = await fetch(request);
    if (response.status === 200) {
      const cache = await caches.open(CACHE_NAME);
      cache.put(request, response.clone());
    }
  } catch (error) {
    // Игнорируем ошибки фоновой записи в кэш
    console.log('[ServiceWorker] Background cache failed:', error);
  }
}

// Обработка сообщений от страницы
self.addEventListener('message', (event) => {
  if (event.data && event.data.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }
});

// Background Sync для уведомлений (опционально)
self.addEventListener('sync', (event) => {
  if (event.tag === 'sync-data') {
    event.waitUntil(syncData());
  }
});

async function syncData() {
  // Логика синхронизации данных в фоне
  console.log('[ServiceWorker] Background sync');
}
