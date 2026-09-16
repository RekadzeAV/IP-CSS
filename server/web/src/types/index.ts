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
  EVENT = 'EVENT',
  SCHEDULED = 'SCHEDULED',
  MANUAL = 'MANUAL'
}

export enum Quality {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  ULTRA = 'ULTRA'
}

export interface DetectionZone {
  id: string;
  name: string;
  points: Array<{ x: number; y: number }>;
  enabled: boolean;
  sensitivity?: number;
}

export interface AnalyticsSettings {
  enabled: boolean;
  motionDetection?: boolean;
  objectDetection?: boolean;
  faceDetection?: boolean;
  licensePlateRecognition?: boolean;
  detectionZones?: DetectionZone[];
}

export interface NotificationSettings {
  enabled: boolean;
  email?: boolean;
  push?: boolean;
  sms?: boolean;
}

export interface CameraStatistics {
  uptime: number;
  recordedHours: number;
  eventsCount: number;
  storageUsed: number;
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
  codec?: string;
  format: string;
  quality: Quality;
  status: RecordingStatus;
  thumbnailUrl?: string;
  createdAt: number;
}

export enum RecordingStatus {
  ACTIVE = 'ACTIVE',
  PAUSED = 'PAUSED',
  COMPLETED = 'COMPLETED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED'
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
  totalPages: number;
}

// DTOs для API запросов
export interface CameraDto {
  id: string;
  name: string;
  url: string;
  username?: string;
  model?: string;
  status: string;
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

export interface CreateCameraRequest {
  name: string;
  url: string;
  username?: string;
  password?: string;
  model?: string;
  resolution?: Resolution;
  fps?: number;
  bitrate?: number;
  codec?: string;
  audio?: boolean;
  ptz?: PTZConfig;
  streams?: StreamConfig[];
  settings?: CameraSettings;
}

export interface UpdateCameraRequest {
  name?: string;
  url?: string;
  username?: string;
  password?: string;
  model?: string;
  resolution?: Resolution;
  fps?: number;
  bitrate?: number;
  codec?: string;
  audio?: boolean;
  ptz?: PTZConfig;
  streams?: StreamConfig[];
  settings?: CameraSettings;
}

export interface LoginRequest {
  username: string;
  password: string;
}

/** Совпадает с серверным LoginResponse (Kotlin); токены в теле часто пустые — реальные значения в httpOnly cookies */
export interface LoginResponse {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  expiresIn: number;
  user: User;
  needs2fa?: boolean;
  tempToken?: string | null;
}

export interface RefreshTokenResponse {
  accessToken: string;
  refreshToken: string;
  tokenType?: string;
  expiresIn: number;
}

// DTOs для API ответов
export interface DiscoveredCameraDto {
  name: string;
  url: string;
  model?: string;
  manufacturer?: string;
  ipAddress: string;
  port: number;
  username?: string;
  password?: string;
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

export interface CameraObservationSummaryDto {
  computedPixelsPerMeter: number | null;
  targetPixelsPerMeterMin: number | null;
  lowResolutionWarning: boolean;
}

export enum NotificationType {
  EVENT = 'EVENT',
  ALERT = 'ALERT',
  INFO = 'INFO',
  WARNING = 'WARNING',
  ERROR = 'ERROR',
  SYSTEM = 'SYSTEM',
  RECORDING = 'RECORDING',
  LICENSE = 'LICENSE',
  USER = 'USER',
}export enum NotificationPriority {
  LOW = 'LOW',
  NORMAL = 'NORMAL',
  HIGH = 'HIGH',
  URGENT = 'URGENT',
}export interface Notification {
  id: string;
  title: string;
  message: string;
  type: NotificationType;
  priority: NotificationPriority;
  cameraId?: string;
  eventId?: string;
  recordingId?: string;
  read: boolean;
  timestamp: number;
  extras?: Record<string, string>;
}

// Report types
export interface Report {
  id: string;
  type: ReportType;
  cameraId: string;
  cameraName?: string;
  period: {
    start: number;
    end: number;
  };
  status: ReportStatus;
  format?: ExportFormat;
  fileUrl?: string;
  fileSize?: number;
  metadata?: Record<string, any>;
  createdAt: number;
  completedAt?: number;
}

export type ReportType = 'EVENTS_SUMMARY' | 'RECORDINGS_SUMMARY' | 'LICENSE_PLATES_SUMMARY' | 'ANALYTICS_DASHBOARD';

export type ReportStatus = 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';

export type ExportFormat = 'pdf' | 'csv' | 'xlsx';

export interface ReportFilter {
  cameraId?: string;
  type?: ReportType;
  status?: ReportStatus;
  startDate?: number;
  endDate?: number;
  page?: number;
  limit?: number;
}