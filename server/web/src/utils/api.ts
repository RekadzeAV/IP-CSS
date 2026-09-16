import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios';
import { validateCertificatePinning } from './certificatePinning';

export const API_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080/api/v1';

// Создание axios instance с поддержкой cookies
const apiClient: AxiosInstance = axios.create({
  baseURL: API_URL,
  timeout: 30000,
  withCredentials: true, // Включаем отправку cookies (httpOnly cookies)
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor для certificate pinning (в production)
if (process.env.NODE_ENV === 'production') {
  apiClient.interceptors.request.use(
    (config) => {
      if (config.url && config.baseURL) {
        const url = new URL(config.url, config.baseURL);
        if (!validateCertificatePinning(url.hostname)) {
          return Promise.reject(new Error('Certificate pinning validation failed'));
        }
      }
      return config;
    },
    (error) => Promise.reject(error)
  );
}

/**
 * Получить CSRF токен из cookie
 * CSRF токен хранится в обычной cookie (не httpOnly) для чтения из JavaScript
 */
function getCsrfToken(): string | null {
  if (typeof document === 'undefined') {
    return null;
  }
  const cookies = document.cookie.split(';');
  for (const cookie of cookies) {
    const [name, value] = cookie.trim().split('=');
    if (name === 'csrf_token') {
      return decodeURIComponent(value);
    }
  }
  return null;
}

// Request interceptor - добавляем CSRF токен к state-changing запросам
apiClient.interceptors.request.use(
  (config) => {
    // Добавляем CSRF токен для POST, PUT, DELETE, PATCH запросов
    const method = config.method?.toUpperCase();
    if (method && ['POST', 'PUT', 'DELETE', 'PATCH'].includes(method)) {
      const csrfToken = getCsrfToken();
      if (csrfToken && config.headers) {
        config.headers['X-CSRF-Token'] = csrfToken;
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Флаг для предотвращения множественных одновременных refresh запросов
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (value?: unknown) => void;
  reject: (error?: unknown) => void;
}> = [];

const processQueue = (error: unknown, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (error) {
      prom.reject(error);
    } else {
      prom.resolve(token);
    }
  });
  failedQueue = [];
};

// Response interceptor для обработки ошибок и автоматического обновления токенов
apiClient.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };

    // Если ошибка 401 и это не запрос на refresh или login
    if (error.response?.status === 401 && !originalRequest._retry) {
      // Если это запрос на refresh или login, перенаправляем на страницу входа
      if (
        originalRequest.url?.includes('/auth/refresh') ||
        originalRequest.url?.includes('/auth/login')
      ) {
        if (typeof window !== 'undefined') {
          window.location.href = '/login';
        }
        return Promise.reject(error);
      }

      // Если уже идет обновление токена, добавляем запрос в очередь
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        })
          .then(() => {
            if (originalRequest.headers) {
              originalRequest.headers['X-CSRF-Token'] = getCsrfToken() || '';
            }
            return apiClient(originalRequest);
          })
          .catch((err) => {
            return Promise.reject(err);
          });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // Пытаемся обновить токен
        const { authService } = await import('../services/authService');
        const expiresIn = await authService.refreshToken();
        const { scheduleAccessTokenRefresh } = await import('./accessTokenRefreshScheduler');
        scheduleAccessTokenRefresh(expiresIn);

        // Успешно обновили токен, обрабатываем очередь
        processQueue(null);
        isRefreshing = false;

        // Повторяем оригинальный запрос
        if (originalRequest.headers) {
          originalRequest.headers['X-CSRF-Token'] = getCsrfToken() || '';
        }
        return apiClient(originalRequest);
      } catch (refreshError) {
        // Не удалось обновить токен, обрабатываем очередь с ошибкой
        processQueue(refreshError, null);
        isRefreshing = false;

        // Перенаправляем на страницу входа
        if (typeof window !== 'undefined') {
          window.location.href = '/login';
        }
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default apiClient;


