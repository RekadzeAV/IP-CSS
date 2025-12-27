import axios, { AxiosInstance, AxiosError, InternalAxiosRequestConfig } from 'axios';

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

// Request interceptor - токены теперь в httpOnly cookies, не нужно добавлять вручную
// Cookies автоматически отправляются браузером с каждым запросом

// Response interceptor для обработки ошибок
apiClient.interceptors.response.use(
  (response) => response,
  (error: AxiosError) => {
    if (error.response?.status === 401) {
      // Токен истек или невалиден - перенаправляем на страницу входа
      if (typeof window !== 'undefined') {
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

export default apiClient;


