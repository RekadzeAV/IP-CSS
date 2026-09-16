# WebSocket Subscription Protocol (v1)

This document describes the current protocol implemented by the server endpoint:

- Endpoint: `/api/v1/ws`
- Transport: WebSocket text frames (JSON payloads)
- Auth model: explicit `auth` message with JWT token

## Client -> Server messages

### 1) Authenticate

```json
{
  "type": "auth",
  "data": {
    "token": "<jwt>"
  }
}
```

### 2) Subscribe to channels

```json
{
  "type": "subscribe",
  "data": {
    "channels": ["cameras", "events"]
  }
}
```

### 3) Unsubscribe from channels

```json
{
  "type": "unsubscribe",
  "data": {
    "channels": ["events"]
  }
}
```

## Server -> Client messages

### Authentication response

```json
{
  "success": true,
  "message": "Authentication successful"
}
```

### Subscription response

```json
{
  "success": true,
  "channels": ["cameras", "events"],
  "message": "Subscribed to 2 channel(s)"
}
```

### Unsubscription response

```json
{
  "success": true,
  "channels": ["events"],
  "message": "Unsubscribed from 1 channel(s)"
}
```

### Event message

```json
{
  "type": "camera_created",
  "channel": "cameras",
  "data": {
    "cameraId": "cam-123",
    "timestamp": 1714155505123
  }
}
```

### Error message

```json
{
  "error": "Authentication required",
  "code": "AUTH_REQUIRED"
}
```

## Supported channels

Channel names are case-insensitive in incoming requests, but canonical lowercase names are used in outgoing events:

- `cameras`
- `events`
- `recordings`
- `notifications`
- `analytics`

## Error codes

- `AUTH_REQUIRED` - command sent before successful `auth`
- `UNKNOWN_TYPE` - unsupported message type
- `PROCESSING_ERROR` - malformed message or unexpected processing exception

## Security notes

- JWT is validated server-side using configured verifier.
- Client should re-authenticate after reconnect.
- Use secure transport (`wss`) in production environments.
