export interface Camera {
  id: string;
  name: string;
  url: string;
  status: 'online' | 'offline' | 'error';
  resolution: string;
  fps: number;
  codec: 'H.264' | 'H.265' | 'MJPEG';
  location: string;
  lastSeen: Date;
  recording: boolean;
  motionDetection: boolean;
}

export interface AnalyticsEvent {
  id: string;
  type: 'motion' | 'object' | 'face' | 'license_plate';
  cameraId: string;
  cameraName: string;
  timestamp: Date;
  confidence: number;
  thumbnail?: string;
  metadata?: any;
}

export interface SystemHealth {
  cpuUsage: number;
  memoryUsage: number;
  diskUsage: number;
  networkUsage: number;
  activeStreams: number;
  totalCameras: number;
}

export interface PerformanceMetrics {
  avgFps: number;
  minFps: number;
  maxFps: number;
  avgLatency: number;
  droppedFrames: number;
  reconnections: number;
}

export interface StorageUsage {
  totalSpace: number;
  usedSpace: number;
  freeSpace: number;
  recordingsSize: number;
  snapshotsSize: number;
  estimatedDaysRemaining: number;
}

export interface Alert {
  id: string;
  severity: 'low' | 'medium' | 'high' | 'critical';
  title: string;
  message: string;
  timestamp: Date;
  acknowledged: boolean;
}
