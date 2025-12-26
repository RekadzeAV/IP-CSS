import apiClient from '@/utils/api';
import type { ApiResponse, LoginRequest, LoginResponse, User } from '@/types';

export const authService = {
  /**
   * Вход в систему
   */
  async login(credentials: LoginRequest): Promise<LoginResponse> {
    const response = await apiClient.post<ApiResponse<LoginResponse>>('/auth/login', credentials);
    if (response.data.success && response.data.data) {
      // Сохраняем токен в localStorage
      if (typeof window !== 'undefined') {
        localStorage.setItem('token', response.data.data.token);
        localStorage.setItem('refreshToken', response.data.data.refreshToken);
      }
      return response.data.data;
    }
    throw new Error(response.data.message || 'Login failed');
  },

  /**
   * Выход из системы
   */
  async logout(): Promise<void> {
    try {
      await apiClient.post('/auth/logout');
    } catch (error) {
      // Игнорируем ошибки при выходе
      console.error('Logout error:', error);
    } finally {
      if (typeof window !== 'undefined') {
        localStorage.removeItem('token');
        localStorage.removeItem('refreshToken');
      }
    }
  },

  /**
   * Обновить токен
   */
  async refreshToken(): Promise<string> {
    const refreshToken = typeof window !== 'undefined' ? localStorage.getItem('refreshToken') : null;
    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    const response = await apiClient.post<ApiResponse<{ token: string }>>('/auth/refresh', {
      refreshToken,
    });

    if (response.data.success && response.data.data) {
      const newToken = response.data.data.token;
      if (typeof window !== 'undefined') {
        localStorage.setItem('token', newToken);
      }
      return newToken;
    }
    throw new Error(response.data.message || 'Failed to refresh token');
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
   */
  isAuthenticated(): boolean {
    if (typeof window === 'undefined') return false;
    return !!localStorage.getItem('token');
  },

  /**
   * Получить токен из localStorage
   */
  getToken(): string | null {
    if (typeof window === 'undefined') return null;
    return localStorage.getItem('token');
  },
};

