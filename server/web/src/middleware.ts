import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';

/**
 * Middleware для генерации nonce для CSP
 * Nonce используется для безопасной загрузки inline скриптов
 *
 * Примечание: В текущей реализации используется 'strict-dynamic' в production,
 * что позволяет загружать скрипты, созданные доверенными скриптами.
 * Nonce может быть добавлен позже для более строгой политики.
 */
export function middleware(request: NextRequest) {
  // Генерируем nonce для каждого запроса через Web Crypto (совместимо с Edge runtime)
  const bytes = new Uint8Array(16);
  crypto.getRandomValues(bytes);
  const nonce = Array.from(bytes, (byte) => byte.toString(16).padStart(2, '0')).join('');

  // Создаем клон заголовков для модификации
  const requestHeaders = new Headers(request.headers);

  // Устанавливаем nonce в заголовок для использования в layout или компонентах
  requestHeaders.set('x-nonce', nonce);

  // Создаем ответ с обновленными заголовками
  const response = NextResponse.next({
    request: {
      headers: requestHeaders,
    },
  });

  // Добавляем nonce в заголовок ответа (для будущего использования)
  response.headers.set('x-nonce', nonce);

  return response;
}

// Применяем middleware ко всем маршрутам
export const config = {
  matcher: [
    /*
     * Match all request paths except for the ones starting with:
     * - api (API routes)
     * - _next/static (static files)
     * - _next/image (image optimization files)
     * - favicon.ico (favicon file)
     */
    '/((?!api|_next/static|_next/image|favicon.ico).*)',
  ],
};
