# Docker Configuration for NAS x86_64

This directory contains Docker configurations for deploying IP-CSS on NAS x86_64 devices.

## Files

- `Dockerfile` - Multi-stage Docker build for x86_64
- `docker-compose.yml` - Docker Compose configuration with API, Web, and Redis services

## Quick Start

### Using Docker Compose

```bash
cd platforms/nas-x86_64/docker
docker-compose up -d
```

### Manual Docker Build

```bash
# Build the image
docker build -f platforms/nas-x86_64/docker/Dockerfile -t ip-css-nas-x86_64:latest ../..

# Run the container
docker run -d \
  --name ip-css-nas \
  -p 8081:8081 \
  -v ip-css-data:/app/data \
  -e JWT_SECRET=your-secret-key \
  ip-css-nas-x86_64:latest
```

## Services

The `docker-compose.yml` includes:

1. **ip-css-api** - Ktor API server (port 8081)
2. **ip-css-web** - Next.js web interface (port 8080)
3. **redis** - Redis cache server

## Volumes

- `ip-css-data` - Application data (database, recordings, screenshots)
- `ip-css-logs` - Application logs
- `redis-data` - Redis data

## Environment Variables

See `../server/config.yaml` for configuration options.

## Health Checks

Both API and Redis services include health checks that verify the services are running correctly.

## Networking

All services are connected via a bridge network (`ip-css-network`) for internal communication.
