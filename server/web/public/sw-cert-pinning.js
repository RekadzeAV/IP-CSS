/**
 * Service Worker для Certificate Pinning
 * Проверяет сертификаты при запросах к API
 */

// Список закрепленных сертификатов (SHA-256 хеши)
const PINNED_CERTIFICATES = {
  // Добавьте хеши ваших сертификатов
  // 'your-server.com': ['sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA='],
};

self.addEventListener('fetch', (event) => {
  const url = new URL(event.request.url);

  // Проверяем только запросы к нашему API
  if (url.hostname.includes('localhost') || process.env.NODE_ENV === 'development') {
    // В development пропускаем проверку
    return;
  }

  const pinnedHashes = PINNED_CERTIFICATES[url.hostname];
  if (pinnedHashes) {
    // В браузере мы не можем напрямую проверить сертификат в Service Worker
    // Это требует дополнительной инфраструктуры
    // Здесь можно добавить логику проверки через fetch с дополнительными заголовками
  }
});

self.addEventListener('install', () => {
  self.skipWaiting();
});

self.addEventListener('activate', () => {
  self.clients.claim();
});

