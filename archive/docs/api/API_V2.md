# IP-CSS API Documentation

**Version:** 1.0.0  
**Date:** 27 April 2026  
**Status:** 🟢 **PHASE 1 MVP READY FOR BETA**

> **📚 Full Documentation Index:** [DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)

---

## Base URL

```
https://api.company.com/v1
or
http://localhost:8080/api/v1
```

---

## Authentication

All endpoints (except public ones) require JWT authentication via httpOnly cookies.

### Login

```http
POST /api/v1/auth/login
Content-Type: application/json

{
  "username": "admin",
  "password": "password123"
}
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": "user-001",
      "username": "admin",
      "email": "admin@example.com",
      "role": "ADMIN",
      "permissions": ["*"]
    }
  },
  "message": "Login successful"
}
```

**Cookies Set:**
- `access_token` - JWT access token (15 min TTL, httpOnly, secure in production)
- `refresh_token` - JWT refresh token (7 days TTL, httpOnly, secure in production)

### Refresh Token

```http
POST /api/v1/auth/refresh
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "accessToken": "new_access_token",
    "refreshToken": "new_refresh_token"
  },
  "message": "Token refreshed successfully"
}
```

### Logout

```http
POST /api/v1/auth/logout
```

Deletes all authentication cookies and revokes refresh token.

### WebSocket Token

```http
GET /api/v1/auth/ws-token
Authorization: Bearer <jwt-token>
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "token": "ws_jwt_token",
    "expiresAt": 1642687200000
  },
  "message": "WebSocket token generated"
}
```

### Current User

```http
GET /api/v1/users/me
```

**Response (200):**
```json
{
  "id": "user-001",
  "username": "admin",
  "email": "admin@example.com",
  "fullName": "Administrator",
  "role": "ADMIN",
  "permissions": ["read", "write", "delete", "admin"],
  "createdAt": 1642683600000,
  "lastLoginAt": 1642683600000,
  "isActive": true
}
```

---

## Health & Monitoring

### Health Check

```http
GET /api/v1/health
```

**Response (200):**
```json
{
  "status": "OK",
  "timestamp": 1642683600000,
  "version": "1.0.0",
  "checks": {
    "database": "OK",
    "redis": "OK",
    "storage": "OK",
    "ffmpeg": "OK"
  }
}
```

### Readiness Check (for Kubernetes readiness probe)

```http
GET /api/v1/health/ready
```

**Response (200):**
```json
{
  "status": "READY",
  "timestamp": 1642683600000,
  "checks": {
    "database": "OK",
    "storage": "OK"
  }
}
```

### Liveness Check (for Kubernetes liveness probe)

```http
GET /api/v1/health/live
```

**Response (200):**
```json
{
  "status": "ALIVE",
  "timestamp": 1642683600000
}
```

### Health Metrics (Admin only)

```http
GET /api/v1/health/metrics
Authorization: Bearer <admin-jwt>
```

**Response (200):**
```json
{
  "metrics": {
    "discoverRequests": 150,
    "discoverFallbackUsed": 12,
    "authSuccess": 89,
    "authFailure": 3,
    "avgResponseTime": 45,
    "requestsPerMinute": 120
  }
}
```

---

## Cameras (`/api/v1/cameras`)

### List Cameras

```http
GET /api/v1/cameras?page=1&limit=20&status=ONLINE
```

**Query Parameters:**
- `page` (int, optional) - page number (default: 1)
- `limit` (int, optional) - items per page (default: 20)
- `status` (string, optional) - filter by status (ONLINE, OFFLINE, ERROR, CONNECTING, UNKNOWN)

**Response (200):**
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "cam-001",
        "name": "Camera 1",
        "url": "rtsp://192.168.1.101:554/stream1",
        "status": "ONLINE",
        "resolution": {
          "width": 1920,
          "height": 1080
        },
        "fps": 25,
        "bitrate": 4096,
        "codec": "H.264",
        "audio": true,
        "createdAt": 1642683600000,
        "updatedAt": 1642683600000
      }
    ],
    "total": 10,
    "page": 1,
    "limit": 20,
    "hasMore": false
  }
}
```

### Get Camera by ID

```http
GET /api/v1/cameras/{id}
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": "cam-001",
    "name": "Camera 1",
    "url": "rtsp://192.168.1.101:554/stream1",
    "status": "ONLINE",
    "model": "Hikvision DS-2CD2342WD-I",
    "manufacturer": "Hikvision",
    "resolution": {
      "width": 1920,
      "height": 1080
    },
    "fps": 25,
    "bitrate": 4096,
    "codec": "H.264",
    "audio": true,
    "ptz": {
      "enabled": true,
      "type": "PTZ",
      "presets": ["Home", "Position1"]
    },
    "createdAt": 1642683600000,
    "updatedAt": 1642683600000
  }
}
```

### Add Camera

```http
POST /api/v1/cameras
Content-Type: application/json

{
  "name": "New Camera",
  "url": "rtsp://192.168.1.101:554/stream1",
  "username": "admin",
  "password": "camera123",
  "model": "Hikvision DS-2CD2342WD-I",
  "resolution": {
    "width": 1920,
    "height": 1080
  },
  "fps": 25,
  "bitrate": 4096,
  "codec": "H.264",
  "audio": true,
  "ptz": {
    "enabled": true,
    "type": "PTZ",
    "presets": ["Home", "Position1"]
  }
}
```

**Response (201):**
```json
{
  "success": true,
  "data": {
    "id": "cam-002",
    "message": "Camera created successfully"
  }
}
```

### Update Camera

```http
PUT /api/v1/cameras/{id}
Content-Type: application/json

{
  "name": "Updated Camera Name",
  "fps": 30
}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Camera updated successfully"
}
```

### Delete Camera

```http
DELETE /api/v1/cameras/{id}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Camera deleted successfully"
}
```

### Test Camera Connection

```http
POST /api/v1/cameras/{id}/test
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "status": "connected",
    "latency": "45ms",
    "resolution": "1920x1080",
    "capabilities": {
      "ptz": true,
      "audio": true,
      "onvif": true
    }
  },
  "message": "Connection test successful"
}
```

### Discover Cameras (WS-Discovery + UPnP)

```http
GET /api/v1/cameras/discover?refresh=true
```

**Query Parameters:**
- `refresh` (boolean, optional) - force fresh discovery (default: false)

**Response (200):**
```json
{
  "success": true,
  "data": {
    "discovered": [
      {
        "id": "temp-001",
        "name": "Hikvision Camera",
        "url": "http://192.168.1.102",
        "ipAddress": "192.168.1.102",
        "port": 80,
        "manufacturer": "Hikvision",
        "model": "DS-2CD2342WD-I",
        "isOnvifCompatible": true,
        "discoveryMethod": "WS_DISCOVERY"
      }
    ],
    "totalCount": 3,
    "methods": ["WS_DISCOVERY", "UPnP"]
  },
  "message": "Discovery completed"
}
```

### Get Camera Status

```http
GET /api/v1/cameras/{id}/status
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": "cam-001",
    "status": "ONLINE",
    "lastSeen": 1642683600000,
    "streamActive": true,
    "recordingActive": false,
    "connectionQuality": "GOOD"
  },
  "message": "Camera status retrieved"
}
```

### Get Camera Observation Summary

```http
GET /api/v1/cameras/{id}/observation-summary
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "computedPixelsPerMeter": 12.4,
    "targetPixelsPerMeterMin": 10.0,
    "lowResolutionWarning": false
  },
  "message": "Observation summary computed"
}
```

### Export Cameras

```http
GET /api/v1/cameras/export/csv
GET /api/v1/cameras/export/json
```

**Response:** CSV or JSON file download

---

## Recordings (`/api/v1/recordings`)

### List Recordings

```http
GET /api/v1/recordings?camera_id=cam-001&start_time=1642683600000&end_time=1642770000000&page=1&limit=20
```

**Query Parameters:**
- `camera_id` (string, optional) - filter by camera ID
- `start_time` (long, optional) - start time (timestamp in milliseconds)
- `end_time` (long, optional) - end time (timestamp in milliseconds)
- `page` (int, optional) - page number
- `limit` (int, optional) - items per page

**Response (200):**
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "rec-001",
        "cameraId": "cam-001",
        "cameraName": "Camera 1",
        "startTime": 1642683600000,
        "endTime": 1642687200000,
        "duration": 3600,
        "filePath": "/recordings/rec-001.mp4",
        "fileSize": 104857600,
        "codec": "H.265",
        "format": "mp4",
        "quality": "HIGH",
        "status": "COMPLETED",
        "thumbnailUrl": "/thumbnails/rec-001.jpg",
        "createdAt": 1642683600000
      }
    ],
    "total": 50,
    "page": 1,
    "limit": 20,
    "hasMore": true
  }
}
```

### Get Recording by ID

```http
GET /api/v1/recordings/{id}
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "id": "rec-001",
    "cameraId": "cam-001",
    "startTime": 1642683600000,
    "endTime": 1642687200000,
    "duration": 3600,
    "fileSize": 104857600,
    "format": "mp4",
    "quality": "HIGH",
    "status": "COMPLETED"
  },
  "message": "Recording retrieved successfully"
}
```

### Recording Passport (declared vs actual file parameters)

```http
GET /api/v1/recordings/{id}/passport
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "recordingId": "rec-001",
    "cameraId": "cam-001",
    "declaredFormat": "MP4",
    "declaredQuality": "HIGH",
    "status": "COMPLETED",
    "declaredCodec": "H.265",
    "actualCodec": "hevc",
    "width": 1920,
    "height": 1080,
    "durationSeconds": "12.341000",
    "bitrate": "8000000",
    "fileSizeBytes": 104857600
  },
  "message": "Recording passport retrieved successfully"
}
```

### Start Recording

```http
POST /api/v1/recordings/start
Content-Type: application/json

{
  "cameraId": "cam-001",
  "duration": 300,
  "quality": "HIGH",
  "format": "mp4"
}
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "recordingId": "rec-002",
    "cameraId": "cam-001",
    "startTime": 1642683600000,
    "estimatedEndTime": 1642683900000
  },
  "message": "Recording started"
}
```

### Stop Recording

```http
POST /api/v1/recordings/{id}/stop
```

**Response (200):**
```json
{
  "success": true,
  "message": "Recording stopped"
}
```

### Pause Recording

```http
POST /api/v1/recordings/{id}/pause
```

### Resume Recording

```http
POST /api/v1/recordings/{id}/resume
```

### Delete Recording

```http
DELETE /api/v1/recordings/{id}
```

**Response (200):**
```json
{
  "success": true,
  "message": "Recording deleted successfully"
}
```

### Download Recording

```http
GET /api/v1/recordings/{id}/download
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "downloadUrl": "https://api.company.com/api/v1/recordings/rec-001/download/file?token=signed_url",
    "expiresAt": 1642770000000
  },
  "message": "Download URL generated"
}
```

### Export Recording

```http
POST /api/v1/recordings/{id}/export
Content-Type: application/json

{
  "format": "mp4",
  "quality": "medium",
  "startTime": 1642683600000,
  "endTime": 1642687200000
}
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "exportId": "exp-001",
    "downloadUrl": "/exports/exp-001.mp4",
    "expiresAt": 1642770000000
  },
  "message": "Export started"
}
```

### Export Recordings (CSV/JSON)

```http
GET /api/v1/recordings/export/csv
GET /api/v1/recordings/export/json
```

### HLS Playlist

```http
GET /api/v1/recordings/{id}/hls/playlist.m3u8
```

**Response:** HLS playlist for video playback

---

## Events (`/api/v1/events`)

### List Events

```http
GET /api/v1/events?type=motion&camera_id=cam-001&severity=WARNING&acknowledged=false&page=1&limit=20
```

**Query Parameters:**
- `type` (string, optional) - event type (motion, object_detection, face_detection, etc.)
- `camera_id` (string, optional) - filter by camera ID
- `severity` (string, optional) - severity (INFO, WARNING, ERROR, CRITICAL)
- `acknowledged` (boolean, optional) - filter by acknowledgment status
- `start_time` (long, optional) - start time
- `end_time` (long, optional) - end time
- `page` (int, optional) - page number
- `limit` (int, optional) - items per page

**Response (200):**
```json
{
  "success": true,
  "data": {
    "items": [
      {
        "id": "evt-001",
        "cameraId": "cam-001",
        "cameraName": "Camera 1",
        "type": "motion",
        "severity": "WARNING",
        "timestamp": 1642683600000,
        "description": "Motion detected",
        "metadata": {
          "zone": "Zone1",
          "confidence": 0.95
        },
        "acknowledged": false,
        "thumbnailUrl": "/thumbnails/evt-001.jpg",
        "videoUrl": "/videos/evt-001.mp4"
      }
    ],
    "total": 100,
    "page": 1,
    "limit": 20,
    "hasMore": true
  }
}
```

### Get Event by ID

```http
GET /api/v1/events/{id}
```

### Acknowledge Event

```http
POST /api/v1/events/{id}/acknowledge
```

**Response (200):**
```json
{
  "success": true,
  "message": "Event acknowledged"
}
```

### Acknowledge Multiple Events

```http
POST /api/v1/events/acknowledge
Content-Type: application/json

{
  "ids": ["evt-001", "evt-002", "evt-003"]
}
```

### Delete Event

```http
DELETE /api/v1/events/{id}
```

### Event Statistics

```http
GET /api/v1/events/statistics?camera_id=cam-001&start_time=1642683600000&end_time=1642770000000
```

**Response (200):**
```json
{
  "success": true,
  "data": {
    "total": 150,
    "byType": {
      "motion": 100,
      "object_detection": 30,
      "face_detection": 20
    },
    "bySeverity": {
      "INFO": 50,
      "WARNING": 70,
      "ERROR": 20,
      "CRITICAL": 10
    }
  }
}
```

### Export Events (CSV/JSON)

```http
GET /api/v1/events/export/csv
GET /api/v1/events/export/json
```

---

## Users (`/api/v1/users`)

### Register User

```http
POST /api/v1/auth/register
Content-Type: application/json

{
  "username": "newuser",
  "email": "user@example.com",
  "password": "password123",
  "fullName": "New User"
}
```

### Get All Users (Admin only)

```http
GET /api/v1/users?page=1&limit=20&role=USER
```

### Get User by ID (Admin only)

```http
GET /api/v1/users/{id}
```

### Update User (Admin only)

```http
PUT /api/v1/users/{id}
Content-Type: application/json

{
  "email": "updated@example.com",
  "fullName": "Updated Name"
}
```

### Delete User (Admin only)

```http
DELETE /api/v1/users/{id}
```

---

## Settings (`/api/v1/settings`)

### List Settings

```http
GET /api/v1/settings?category=recording
```

**Response (200):**
```json
[
  {
    "id": "setting-001",
    "category": "recording",
    "key": "default_quality",
    "value": "HIGH",
    "type": "string",
    "description": "Default recording quality",
    "updatedAt": 1642683600000
  }
]
```

### Get Setting by Key

```http
GET /api/v1/settings/{key}
```

### Update Settings

```http
PUT /api/v1/settings
Content-Type: application/json

{
  "settings": {
    "default_quality": "ULTRA",
    "max_storage_size": "1000000000"
  }
}
```

### Update Single Setting

```http
PUT /api/v1/settings/{key}
Content-Type: application/json

{
  "value": "ULTRA"
}
```

### Delete Setting

```http
DELETE /api/v1/settings/{key}
```

### System Settings

```http
GET /api/v1/settings/system
```

**Response (200):**
```json
{
  "recording": {
    "defaultQuality": "HIGH",
    "defaultFormat": "mp4",
    "maxDuration": 3600,
    "autoDelete": true,
    "retentionDays": 30
  },
  "storage": {
    "maxStorageSize": 1000000000000,
    "currentStorageUsed": 50000000000,
    "storagePath": "/recordings",
    "autoCleanup": true
  },
  "notifications": {
    "emailEnabled": false,
    "smsEnabled": false,
    "pushEnabled": true,
    "webhookUrl": null
  },
  "security": {
    "requireAuth": true,
    "sessionTimeout": 3600,
    "passwordPolicy": {
      "minLength": 8,
      "requireUppercase": true,
      "requireLowercase": true,
      "requireNumbers": true,
      "requireSpecialChars": false
    }
  },
  "network": {
    "apiPort": 8080,
    "websocketPort": 8081,
    "allowRemoteAccess": false,
    "sslEnabled": false
  }
}
```

### Reset Settings

```http
POST /api/v1/settings/reset?category=recording
```

### Export Settings

```http
GET /api/v1/settings/export
```

### Import Settings

```http
POST /api/v1/settings/import
Content-Type: application/json

{
  "settings": {
    "default_quality": "HIGH",
    "max_storage_size": "1000000000"
  }
}
```

---

## WebSocket API

WebSocket server supports real-time communication for camera status updates, events, recordings, and notifications.

### Connection

```
wss://api.company.com/api/v1/ws
or
ws://localhost:8080/api/v1/ws
```

### Authentication

After connecting to WebSocket, authenticate with JWT token:

```json
{
  "type": "auth",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

**Successful Authentication Response:**
```json
{
  "type": "auth_response",
  "data": {
    "success": true,
    "message": "Authenticated successfully"
  }
}
```

**Failed Authentication Response:**
```json
{
  "type": "auth_response",
  "data": {
    "success": false,
    "message": "Invalid token"
  }
}
```

### Subscribe to Channels

After authentication, subscribe to channels:

```json
{
  "type": "subscribe",
  "data": {
    "channels": ["cameras", "events", "recordings", "notifications"],
    "filters": {
      "camera_ids": ["cam-001", "cam-002"]
    }
  }
}
```

**Available Channels:**
- `cameras` - camera status updates
- `events` - new events
- `recordings` - recording updates
- `notifications` - system notifications

**Successful Subscription Response:**
```json
{
  "type": "subscribe_response",
  "data": {
    "success": true,
    "channels": ["cameras", "events"],
    "message": "Subscribed successfully"
  }
}
```

### Unsubscribe from Channels

```json
{
  "type": "unsubscribe",
  "data": {
    "channels": ["events"]
  }
}
```

### Receiving Messages

After subscribing, server sends messages in this format:

#### Camera Status Update
```json
{
  "type": "event",
  "channel": "cameras",
  "data": {
    "cameraId": "cam-001",
    "status": "ONLINE",
    "timestamp": 1642683600000
  }
}
```

#### New Event
```json
{
  "type": "event",
  "channel": "events",
  "data": {
    "id": "evt-001",
    "cameraId": "cam-001",
    "cameraName": "Camera 1",
    "type": "motion",
    "severity": "WARNING",
    "timestamp": 1642683600000,
    "description": "Motion detected",
    "metadata": {
      "zone": "Zone1",
      "confidence": 0.95
    },
    "acknowledged": false
  }
}
```

#### Recording Update
```json
{
  "type": "event",
  "channel": "recordings",
  "data": {
    "id": "rec-001",
    "cameraId": "cam-001",
    "status": "COMPLETED",
    "duration": 3600,
    "fileSize": 104857600
  }
}
```

#### System Notification
```json
{
  "type": "event",
  "channel": "notifications",
  "data": {
    "id": "notif-001",
    "type": "system",
    "severity": "INFO",
    "title": "System Notification",
    "message": "Camera cam-001 restored",
    "timestamp": 1642683600000
  }
}
```

### Error Handling

On error, server sends:

```json
{
  "type": "error",
  "data": {
    "error": "Invalid channel name",
    "code": "INVALID_CHANNEL"
  }
}
```

---

## Error Handling

All API errors follow this format:

```json
{
  "success": false,
  "error": {
    "code": "ERROR_CODE",
    "message": "Human-readable error message",
    "details": "Additional error details (optional)"
  },
  "timestamp": 1642683600000
}
```

### Common Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `UNAUTHORIZED` | 401 | Authentication required |
| `FORBIDDEN` | 403 | Insufficient permissions |
| `NOT_FOUND` | 404 | Resource not found |
| `VALIDATION_ERROR` | 400 | Request validation failed |
| `INTERNAL_ERROR` | 500 | Internal server error |
| `CONNECTION_FAILED` | 503 | External service unavailable |

---

## Rate Limiting

Rate limiting is applied per user:

- **Default limit:** 100 requests per minute
- **Burst limit:** 200 requests per minute

Rate limit headers in response:

```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 95
X-RateLimit-Reset: 1642683660
```

---

## Pagination

All list endpoints support pagination with these standard fields:

```json
{
  "data": {
    "items": [...],
    "total": 100,
    "page": 1,
    "limit": 20,
    "hasMore": true
  }
}
```

---

## Related Documentation

- [E2E_TESTING.md](E2E_TESTING.md) - E2E testing guide
- [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md) - Deployment guide
- [ARCHITECTURE.md](ARCHITECTURE.md) - Architecture documentation
- [CHANGELOG.md](../CHANGELOG.md) - Version history

---

**Last Updated:** 27 April 2026  
**Next Review:** 2026-05-27  
**Maintainer:** Tech Lead
