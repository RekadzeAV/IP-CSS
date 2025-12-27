import apiClient from '@/utils/api';
import type { ApiResponse, LoginRequest, LoginResponse, User } from '@/types';

export const authService = {
  /**
   * Вход в систему
   * Токены теперь хранятся в httpOnly cookies, автоматически устанавливаются сервером
   */
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const response = await apiClient.post<ApiResponse<LoginResponse>>('/auth/login', credentials, {
      withCredentials: true, // Важно для отправки cookies
    });
    if (response.data.success && response.data.data) {
      // Токены теперь в httpOnly cookies, не нужно сохранять в localStorage
      // Сервер устанавливает cookies автоматически
      return response.data.data;
    }
    throw new Error(response.data.message || 'Login failed');
  },

  /**
   * Выход из системы
   * Сервер удалит cookies автоматически
   */
  async logout(): Promise<void> {
    try {
      await apiClient.post('/auth/logout', {}, { withCredentials: true });
      // Cookies будут удалены сервером автоматически
    } catch (error) {
      // Игнорируем ошибки при выходе
      console.error('Logout error:', error);
    }
  },

  /**
   * Обновить токен
   * Refresh token берется из cookie автоматически
   */
  async refreshToken(): Promise<void> {
    // Refresh token теперь в httpOnly cookie, отправляется автоматически
    const response = await apiClient.post<ApiResponse<LoginResponse>>('/auth/refresh', {}, {
      withCredentials: true,
    });

    if (!response.data.success) {
      throw new Error(response.data.message || 'Failed to refresh token');
    }
    // Новые токены устанавливаются в cookies сервером
  },

  /**
   * Получить информацию о текущем пользователе
   */
  async getCurrentUser(): Promise<User> {
    const response = await apiClient.get<ApiResponse<User>>('/users/me');
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to get current user');
  },

  /**
   * Проверить, авторизован ли пользователь
   * Проверяем через запрос к API, так как токен в httpOnly cookie недоступен из JS
   */
  async isAuthenticated(): Promise<boolean> {
    try {
      await this.getCurrentUser();
      return true;
    } catch {
      return false;
    }
  },

  /**
   * Получить токен из cookie
   * Токен в httpOnly cookie недоступен из JavaScript для безопасности
   * Используйте getCurrentUser() для проверки аутентификации
   */
  getToken(): string | null {
    // Токен в httpOnly cookie недоступен из JavaScript
    // Это сделано для защиты от XSS атак
    return null;
  },
};


