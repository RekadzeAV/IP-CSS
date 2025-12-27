# Docker Configuration for NAS ARM

This directory contains Docker configurations for deploying IP-CSS on NAS ARM devices.

## Files

- `Dockerfile.arm64` - Multi-stage Docker build for ARM64
- `docker-compose.yml` - Docker Compose configuration with API, Web, and Redis services

## Quick Start

### Using Docker Compose

```bash
cd platforms/nas-arm/docker
docker-compose up -d
```

### Manual Docker Build

```bash
# Build the image
docker build -f platforms/nas-arm/docker/Dockerfile.arm64 -t ip-css-nas-arm64:latest ../..

# Run the container
docker run -d \
  --name ip-css-nas \
  -p 8081:8081 \
  -v ip-css-data:/app/data \
  -e JWT_SECRET=your-secret-key \
  ip-css-nas-arm64:latest
```

## Services

The `docker-compose.yml` includes:

1. **ip-css-api** - Ktor API server (port 8081)
2. **ip-css-web** - Next.js web interface (port 8080)
3. **redis** - Redis cache server (ARM64 image)

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

## ARM Architecture Notes

This Dockerfile uses `arm64v8/eclipse-temurin` and `arm64v8/redis` base images to ensure compatibility with ARM64 NAS devices.
