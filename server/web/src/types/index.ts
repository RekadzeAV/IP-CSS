// Основные типы данных, соответствующие моделям из Kotlin shared модуля

export enum CameraStatus {
  ONLINE = 'ONLINE',
  OFFLINE = 'OFFLINE',
  ERROR = 'ERROR',
  CONNECTING = 'CONNECTING',
  UNKNOWN = 'UNKNOWN'
}

export enum UserRole {
  ADMIN = 'ADMIN',
  OPERATOR = 'OPERATOR',
  VIEWER = 'VIEWER',
  GUEST = 'GUEST'
}

export enum EventType {
  MOTION_DETECTION = 'MOTION_DETECTION',
  OBJECT_DETECTION = 'OBJECT_DETECTION',
  FACE_DETECTION = 'FACE_DETECTION',
  LICENSE_PLATE_RECOGNITION = 'LICENSE_PLATE_RECOGNITION',
  CAMERA_OFFLINE = 'CAMERA_OFFLINE',
  CAMERA_ONLINE = 'CAMERA_ONLINE',
  RECORDING_STARTED = 'RECORDING_STARTED',
  RECORDING_STOPPED = 'RECORDING_STOPPED',
  STORAGE_FULL = 'STORAGE_FULL',
  SYSTEM_ERROR = 'SYSTEM_ERROR',
  USER_ACTION = 'USER_ACTION',
  OTHER = 'OTHER'
}

export enum EventSeverity {
  INFO = 'INFO',
  WARNING = 'WARNING',
  ERROR = 'ERROR',
  CRITICAL = 'CRITICAL'
}

// Отложено: лицензирование вынесено за рамки проекта
// export enum LicenseType {
//   TRIAL = 'TRIAL',
//   BASIC = 'BASIC',
//   PROFESSIONAL = 'PROFESSIONAL',
//   ENTERPRISE = 'ENTERPRISE'
// }

// export enum LicenseStatus {
//   ACTIVE = 'ACTIVE',
//   INACTIVE = 'INACTIVE',
//   EXPIRED = 'EXPIRED',
//   REVOKED = 'REVOKED',
//   PENDING = 'PENDING'
// }

export interface Resolution {
  width: number;
  height: number;
}

export interface Camera {
  id: string;
  name: string;
  url: string;
  username?: string;
  password?: string;
  model?: string;
  status: CameraStatus;
  resolution?: Resolution;
  fps: number;
  bitrate: number;
  codec: string;
  audio: boolean;
  ptz?: PTZConfig;
  streams?: StreamConfig[];
  settings?: CameraSettings;
  statistics?: CameraStatistics;
  createdAt: number;
  updatedAt: number;
  lastSeen?: number;
}

export interface PTZConfig {
  enabled: boolean;
  type: PTZType;
  presets: string[];
}

export enum PTZType {
  PTZ = 'PTZ',
  PT = 'PT',
  FIXED = 'FIXED'
}

export interface StreamConfig {
  type: StreamType;
  resolution: Resolution;
  fps: number;
  bitrate: number;
}

export enum StreamType {
  MAIN = 'MAIN',
  SUB = 'SUB',
  AUDIO = 'AUDIO',
  METADATA = 'METADATA'
}

export interface CameraSettings {
  recording?: RecordingSettings;
  analytics?: AnalyticsSettings;
  notifications?: NotificationSettings;
}

export interface RecordingSettings {
  enabled: boolean;
  mode: RecordingMode;
  quality: Quality;
  schedule: string;
}

export enum RecordingMode {
  CONTINUOUS = 'CONTINUOUS',
  MOTION = 'MOTION',
  SCHEDULED = 'SCHEDULED'
}

export enum Quality {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  ULTRA = 'ULTRA'
}

export interface AnalyticsSettings {
  enabled: boolean;
  motionDetection?: boolean;
  objectDetection?: boolean;
  faceDetection?: boolean;
  licensePlateRecognition?: boolean;
}

export interface NotificationSettings {
  enabled: boolean;
  email?: boolean;
  push?: boolean;
  sms?: boolean;
}

export interface CameraStatistics {
  uptime: number;
  framesReceived: number;
  framesDropped: number;
  bytesReceived: number;
  averageLatency: number;
}

export interface Event {
  id: string;
  cameraId: string;
  cameraName?: string;
  type: EventType;
  severity: EventSeverity;
  timestamp: number;
  description?: string;
  metadata: Record<string, string>;
  acknowledged: boolean;
  acknowledgedAt?: number;
  acknowledgedBy?: string;
  thumbnailUrl?: string;
  videoUrl?: string;
}

export interface Recording {
  id: string;
  cameraId: string;
  cameraName?: string;
  startTime: number;
  endTime: number;
  duration: number;
  filePath: string;
  fileSize: number;
  format: string;
  quality: Quality;
  status: RecordingStatus;
  thumbnailUrl?: string;
  createdAt: number;
}

export enum RecordingStatus {
  PENDING = 'PENDING',
  RECORDING = 'RECORDING',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  DELETED = 'DELETED'
}

export interface User {
  id: string;
  username: string;
  email?: string;
  fullName?: string;
  role: UserRole;
  permissions: string[];
  createdAt: number;
  lastLoginAt?: number;
  isActive: boolean;
}

// Отложено: лицензирование вынесено за рамки проекта
// export interface License {
//   id: string;
//   licenseKey: string;
//   type: LicenseType;
//   status: LicenseStatus;
//   features: string[];
//   maxCameras?: number;
//   maxUsers?: number;
//   expiresAt?: number;
//   activatedAt?: number;
//   deviceId?: string;
//   isValid: boolean;
// }

export interface SystemSettings {
  recording?: RecordingSystemSettings;
  storage?: StorageSettings;
  notifications?: NotificationSystemSettings;
  security?: SecuritySettings;
  network?: NetworkSettings;
}

export interface RecordingSystemSettings {
  defaultQuality: Quality;
  defaultFormat: RecordingFormat;
  maxDuration: number;
  autoDelete: boolean;
  retentionDays: number;
}

export enum RecordingFormat {
  MP4 = 'MP4',
  AVI = 'AVI',
  MKV = 'MKV'
}

export interface StorageSettings {
  maxStorageSize: number;
  currentStorageUsed: number;
  storagePath: string;
  autoCleanup: boolean;
}

export interface NotificationSystemSettings {
  emailEnabled: boolean;
  smsEnabled: boolean;
  pushEnabled: boolean;
  webhookUrl?: string;
}

export interface SecuritySettings {
  requireAuth: boolean;
  sessionTimeout: number;
  passwordPolicy?: PasswordPolicy;
}

export interface PasswordPolicy {
  minLength: number;
  requireUppercase: boolean;
  requireLowercase: boolean;
  requireNumbers: boolean;
  requireSpecialChars: boolean;
}

export interface NetworkSettings {
  apiPort: number;
  websocketPort: number;
  allowRemoteAccess: boolean;
  sslEnabled: boolean;
}

// API Response types
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
  hasMore: boolean;
}

// DTOs для API запросов
export interface CameraDto {
  id: string;
  name: string;
  url: string;
  username?: string;
  status: string;
  createdAt: number;
  updatedAt: number;
}

export interface CreateCameraRequest {
  name: string;
  url: string;
  username?: string;
  password?: string;
}

export interface UpdateCameraRequest {
  name?: string;
  url?: string;
  username?: string;
  password?: string;
}

export interface LoginRequest {
  username: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  expiresIn: number;
  user: User;
}

// DTOs для API ответов
export interface DiscoveredCameraDto {
  name: string;
  url: string;
  model?: string;
  manufacturer?: string;
  ipAddress: string;
  port: number;
}

export interface ConnectionTestResultDto {
  success: boolean;
  streams?: StreamInfoDto[];
  capabilities?: CameraCapabilitiesDto;
  error?: string;
  errorCode?: string;
}

export interface StreamInfoDto {
  type: string;
  resolution: string;
  fps: number;
  codec: string;
}

export interface CameraCapabilitiesDto {
  ptz: boolean;
  audio: boolean;
  onvif: boolean;
  analytics: boolean;
}

