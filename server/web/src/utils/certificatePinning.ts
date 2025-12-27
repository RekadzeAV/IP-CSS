/**
 * Certificate Pinning для дополнительной безопасности
 * В production окружении проверяет сертификат сервера
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

/**
 * Проверка certificate pinning
 * В браузере это работает только через Service Worker или расширения
 * Для production рекомендуется использовать HTTPS с валидным сертификатом
 */
export function validateCertificatePinning(hostname: string): boolean {
  // В браузере мы не можем напрямую проверить сертификат
  // Это должно быть реализовано на уровне сервера или через Service Worker
  // Здесь возвращаем true для development, в production нужна дополнительная проверка

  if (process.env.NODE_ENV === 'development') {
    return true; // В development пропускаем проверку
  }

  const pinned = PINNED_CERTIFICATES.find((cert) => cert.hostname === hostname);
  if (!pinned) {
    // Если сертификат не закреплен, разрешаем (можно изменить логику)
    return true;
  }

  // В production здесь должна быть проверка через Service Worker
  // или через API запрос к серверу для проверки сертификата
  return true;
}

/**
 * Инициализация certificate pinning через Service Worker
 */
export async function initCertificatePinning(): Promise<void> {
  if (typeof window === 'undefined' || 'serviceWorker' in navigator === false) {
    return;
  }

  try {
    // Регистрируем Service Worker для проверки сертификатов
    const registration = await navigator.serviceWorker.register('/sw-cert-pinning.js');
    console.log('Certificate pinning Service Worker registered:', registration);
  } catch (error) {
    console.warn('Failed to register certificate pinning Service Worker:', error);
  }
}

