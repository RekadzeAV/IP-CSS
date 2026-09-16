import { configureStore } from '@reduxjs/toolkit';
import reducer, { fetchCurrentUser, login, logout } from './authSlice';
import { authService } from '@/services/authService';
import { UserRole } from '@/types';

jest.mock('@/services/authService', () => ({
  authService: {
    login: jest.fn(),
    logout: jest.fn(),
    getCurrentUser: jest.fn(),
  },
}));

describe('authSlice', () => {
  const mockedAuthService = authService as jest.Mocked<typeof authService>;

  const createStore = () =>
    configureStore({
      reducer: {
        auth: reducer,
      },
    });

  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('sets authenticated user after successful login', async () => {
    mockedAuthService.login.mockResolvedValue({
      accessToken: '',
      refreshToken: '',
      expiresIn: 3600,
      user: {
        id: 'u-1',
        username: 'admin',
        email: 'admin@example.com',
        role: UserRole.ADMIN,
        permissions: [],
        createdAt: Date.now(),
        isActive: true,
      },
    });

    const store = createStore();
    await store.dispatch(login({ username: 'admin', password: 'secret' }));

    const state = store.getState().auth;
    expect(state.isAuthenticated).toBe(true);
    expect(state.user?.username).toBe('admin');
    expect(state.token).toBeNull();
    expect(state.error).toBeNull();
  });

  it('marks user unauthenticated when fetchCurrentUser fails', async () => {
    mockedAuthService.getCurrentUser.mockRejectedValue(new Error('Unauthorized'));

    const store = createStore();
    await store.dispatch(fetchCurrentUser());

    const state = store.getState().auth;
    expect(state.isAuthenticated).toBe(false);
    expect(state.error).toBeTruthy();
  });

  it('clears auth state on logout', async () => {
    mockedAuthService.login.mockResolvedValue({
      accessToken: '',
      refreshToken: '',
      expiresIn: 3600,
      user: {
        id: 'u-2',
        username: 'viewer',
        email: 'viewer@example.com',
        role: UserRole.VIEWER,
        permissions: [],
        createdAt: Date.now(),
        isActive: true,
      },
    });
    mockedAuthService.logout.mockResolvedValue();

    const store = createStore();
    await store.dispatch(login({ username: 'viewer', password: 'secret' }));
    await store.dispatch(logout());

    const state = store.getState().auth;
    expect(state.isAuthenticated).toBe(false);
    expect(state.user).toBeNull();
    expect(state.token).toBeNull();
  });
});
