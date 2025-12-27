import api from '@/utils/api';
import type { User, CreateUserRequest, UpdateUserRequest } from '@/types';

class UserService {
  async getUsers(): Promise<User[]> {
    const response = await api.get('/users');
    return response.data;
  }

  async getUserById(id: string): Promise<User> {
    const response = await api.get(`/users/${id}`);
    return response.data;
  }

  async createUser(data: CreateUserRequest): Promise<User> {
    const response = await api.post('/users', data);
    return response.data;
  }

  async updateUser(id: string, data: UpdateUserRequest): Promise<User> {
    const response = await api.put(`/users/${id}`, data);
    return response.data;
  }

  async deleteUser(id: string): Promise<void> {
    await api.delete(`/users/${id}`);
  }
}

export const userService = new UserService();

