/**
 * Certificate Pinning для дополнительной безопасности
 * ВАЖНО: В обычном браузерном окружении реальный certificate pinning недоступен.
 * Этот модуль не выполняет криптографическую проверку сертификата и не должен
 * использоваться как источник утверждения "web pinning реализован".
 */

interface PinnedCertificate {
  hostname: string;
  publicKeyHashes: string[]; // SHA-256 хеши публичных ключей
}

// Список закрепленных сертификатов
const PINNED_CERTIFICATES: PinnedCertificate[] = [
  // Добавьте сюда хеши сертификатов вашего сервера
  // Пример:
  // {
  //   hostname: 'your-server.com',
  //   publicKeyHashes: ['sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA='],
  // },
];

export function validateCertificatePinning(hostname: string): boolean {
  if (process.env.NODE_ENV === 'development') {
    return true;
  }

  if (!hostname) {
    return false;
  }

  // Базовая защита в web: требуем HTTPS.
  if (typeof window !== 'undefined' && window.location.protocol !== 'https:') {
    console.error('Insecure web context detected: HTTPS is required in production.');
    return false;
  }

  const pinned = PINNED_CERTIFICATES.find((cert) => cert.hostname === hostname);
  if (pinned) {
    // Явно блокируем "ложно-положительный" результат, пока нет реального pinning механизма.
    console.error(
      `Certificate pinning is not supported in browser runtime for host ${hostname}. ` +
      'Use server-side trust boundary and HTTPS enforcement instead.'
    );
    return false;
  }

  // Если pins не заданы, используем HTTPS как единственный web-уровень transport security.
  return true;
}

/**
 * Инициализация certificate pinning через Service Worker
 */
export async function initCertificatePinning(): Promise<void> {
  if (typeof window === 'undefined') {
    return;
  }

  // Ничего не регистрируем: сервис-воркер не дает надежного pinning эквивалента.
  // Сохраняем API, чтобы не ломать вызовы в существующем коде.
  if (process.env.NODE_ENV === 'production') {
    console.warn(
      'Web certificate pinning is intentionally disabled. ' +
      'Rely on HTTPS/TLS termination and server-side certificate validation.'
    );
  }
}



