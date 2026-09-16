import apiClient from '@/utils/api';
import type { ApiResponse, PaginatedResponse, User, UserRole } from '@/types';

export interface CreateUserRequest {
  username: string;
  email: string;
  password: string;
  role: UserRole;
}

export interface UpdateUserRequest {
  username?: string;
  email?: string;
  password?: string;
  role?: UserRole;
}

export interface UserListParams {
  page?: number;
  limit?: number;
  role?: UserRole;
}

class UserService {
  async getUsers(params?: UserListParams): Promise<PaginatedResponse<User>> {
    const page = params?.page ?? 1;
    const limit = params?.limit ?? 20;
    const response = await apiClient.get<ApiResponse<PaginatedResponse<User>>>('/users', {
      params: {
        page,
        limit,
        ...(params?.role ? { role: params.role } : {}),
      },
    });
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to fetch users');
  }

  async getUserById(id: string): Promise<User> {
    const response = await apiClient.get<ApiResponse<User>>(`/users/${id}`);
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to fetch user');
  }

  async createUser(data: CreateUserRequest): Promise<User> {
    const response = await apiClient.post<ApiResponse<User>>('/users', data);
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to create user');
  }

  async updateUser(id: string, data: UpdateUserRequest): Promise<User> {
    const response = await apiClient.put<ApiResponse<User>>(`/users/${id}`, data);
    if (response.data.success && response.data.data) {
      return response.data.data;
    }
    throw new Error(response.data.message || 'Failed to update user');
  }

  async deleteUser(id: string): Promise<void> {
    const response = await apiClient.delete<ApiResponse<unknown>>(`/users/${id}`);
    if (!response.data.success) {
      throw new Error(response.data.message || 'Failed to delete user');
    }
  }
}

export const userService = new UserService();



