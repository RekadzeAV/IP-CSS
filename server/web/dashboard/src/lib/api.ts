export interface ApiResponse<T> {
  success: boolean;
  data: T | null;
  message: string;
}

export interface PaginatedResponse<T> {
  items: T[];
  total: number;
  page: number;
  limit: number;
  totalPages: number;
}

export interface CameraDto {
  id: string;
  name: string;
  url: string;
  status: string;
  fps?: number;
  codec?: string;
  resolution?: { width: number; height: number } | null;
}

export interface UserInfoDto {
  id: string;
  username: string;
  email?: string | null;
  fullName?: string | null;
  role: string;
  permissions: string[];
}

export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  user: UserInfoDto;
  needs2fa?: boolean;
  tempToken?: string;
}

const AUTH_USER_KEY = 'ipcss_dashboard_user';

export function getStoredUser(): UserInfoDto | null {
  const raw = sessionStorage.getItem(AUTH_USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as UserInfoDto;
  } catch {
    return null;
  }
}

export function setStoredUser(user: UserInfoDto | null): void {
  if (user) {
    sessionStorage.setItem(AUTH_USER_KEY, JSON.stringify(user));
  } else {
    sessionStorage.removeItem(AUTH_USER_KEY);
  }
}

export async function apiFetch<T>(
  path: string,
  init: RequestInit = {},
): Promise<ApiResponse<T>> {
  const headers = new Headers(init.headers);
  if (init.body && !headers.has('Content-Type')) {
    headers.set('Content-Type', 'application/json');
  }

  const response = await fetch(path, {
    ...init,
    headers,
    credentials: 'include',
  });

  const json = (await response.json()) as ApiResponse<T>;
  if (!response.ok) {
    throw new Error(json.message || `HTTP ${response.status}`);
  }
  return json;
}

export async function login(username: string, password: string): Promise<UserInfoDto> {
  const result = await apiFetch<LoginResponse>('/api/v1/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password }),
  });

  if (!result.success || !result.data) {
    throw new Error(result.message || 'Login failed');
  }

  if (result.data.needs2fa) {
    throw new Error('Требуется 2FA — используйте основной веб-клиент');
  }

  setStoredUser(result.data.user);
  return result.data.user;
}

export async function logout(): Promise<void> {
  try {
    await apiFetch<null>('/api/v1/auth/logout', { method: 'POST' });
  } catch {
    // ignore network errors on logout
  } finally {
    setStoredUser(null);
  }
}

export async function fetchCameras(): Promise<CameraDto[]> {
  const result = await apiFetch<PaginatedResponse<CameraDto>>('/api/v1/cameras?limit=100');
  if (!result.success || !result.data) {
    throw new Error(result.message || 'Failed to load cameras');
  }
  return result.data.items;
}

export async function startCameraStream(cameraId: string): Promise<void> {
  const result = await apiFetch<string>(`/api/v1/cameras/${cameraId}/stream/start`, {
    method: 'POST',
  });
  if (!result.success) {
    throw new Error(result.message || 'Failed to start stream');
  }
}

export async function stopCameraStream(cameraId: string): Promise<void> {
  await apiFetch<null>(`/api/v1/cameras/${cameraId}/stream/stop`, { method: 'POST' });
}

export function cameraHlsPlaylistUrl(cameraId: string): string {
  return `/api/v1/cameras/${cameraId}/stream/hls/playlist.m3u8`;
}

export function normalizeCameraStatus(status: string): 'online' | 'offline' | 'error' {
  const s = status.toUpperCase();
  if (s === 'ONLINE') return 'online';
  if (s === 'ERROR') return 'error';
  return 'offline';
}

export interface DiscoveredCameraDto {
  name: string;
  url: string;
  model?: string | null;
  manufacturer?: string | null;
  ipAddress: string;
  port: number;
}

export interface EventDto {
  id: string;
  cameraId: string;
  cameraName?: string | null;
  type: string;
  severity: string;
  timestamp: number;
  description?: string | null;
  acknowledged: boolean;
}

export interface PaginatedEventsResponse {
  items: EventDto[];
  total: number;
  page: number;
  limit: number;
  hasMore: boolean;
}

export async function discoverCameras(refresh = false): Promise<DiscoveredCameraDto[]> {
  const query = refresh ? '?refresh=true' : '';
  const result = await apiFetch<DiscoveredCameraDto[]>(`/api/v1/cameras/discover${query}`);
  if (!result.success || !result.data) {
    throw new Error(result.message || 'Discovery failed');
  }
  return result.data;
}

export async function fetchEvents(page = 1, limit = 50): Promise<PaginatedEventsResponse> {
  const result = await apiFetch<PaginatedEventsResponse>(
    `/api/v1/events?page=${page}&limit=${limit}`,
  );
  if (!result.success || !result.data) {
    throw new Error(result.message || 'Failed to load events');
  }
  return result.data;
}

export async function fetchWsToken(): Promise<string> {
  const result = await apiFetch<{ token: string }>('/api/v1/auth/ws-token');
  if (!result.success || !result.data?.token) {
    throw new Error(result.message || 'Failed to get WebSocket token');
  }
  return result.data.token;
}
