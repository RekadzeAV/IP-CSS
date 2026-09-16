# IP-CSS Deployment Guide

**Version:** 1.0.0  
**Date:** 27 April 2026  
**Status:** 🟢 **PHASE 1 MVP READY FOR BETA**

> **📚 Full Documentation Index:** [DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)

---

## Table of Contents

1. [System Requirements](#system-requirements)
2. [Quick Start](#quick-start)
3. [Docker Deployment](#docker-deployment)
4. [Kubernetes Deployment](#kubernetes-deployment)
5. [CI/CD Pipelines](#cicd-pipelines)
6. [Configuration](#configuration)
7. [Monitoring & Logging](#monitoring--logging)
8. [Backup & Recovery](#backup--recovery)
9. [Troubleshooting](#troubleshooting)

---

## System Requirements

### Server Requirements

#### Minimum Requirements
- **CPU:** 4 cores (Intel i5 / AMD Ryzen 5)
- **RAM:** 8 GB
- **Storage:** 100 GB SSD + additional for recordings
- **Network:** 1 Gbps
- **OS:** Ubuntu 20.04+, Debian 11+, CentOS 8+

#### Recommended Requirements
- **CPU:** 8 cores with hardware transcoding (Intel i7/i9 with Quick Sync)
- **RAM:** 16 GB
- **Storage:** 500 GB SSD (OS + cache) + HDD array for recordings
- **Network:** 10 Gbps
- **OS:** Ubuntu 22.04 LTS

### Client Requirements

#### Mobile Devices
- **Android:** Android 8.0+ (API 26+), 2 GB RAM
- **iOS:** iOS 14.0+, iPhone 8+/iPad 5+, 2 GB RAM

#### Desktop
- **Windows:** Windows 10/11 64-bit, 4 GB RAM
- **Linux:** Ubuntu 20.04+, 4 GB RAM
- **macOS:** macOS 11.0+, 8 GB RAM

---

## Quick Start

### Docker Compose (5 Minutes)

```bash
# 1. Clone repository
git clone https://github.com/RekadzeAV/IP-CSS.git
cd IP-CSS

# 2. Copy environment file
cp .env.example .env

# 3. Edit configuration
nano .env

# 4. Start services
docker-compose up -d

# 5. Check status
docker-compose ps

# 6. Access web interface
# http://localhost:8080
# Username: admin
# Password: admin123
```

### Verify Deployment

```bash
# Check health endpoint
curl http://localhost:8080/api/v1/health

# Expected response:
# {
#   "status": "OK",
#   "timestamp": 1642683600000,
#   "checks": {
#     "database": "OK",
#     "storage": "OK"
#   }
# }
```

---

## Docker Deployment

### Docker Compose Configuration

**File:** `docker-compose.yml`

```yaml
version: '3.8'

services:
  # API Server
  api:
    image: rekadzeav/ipcss-api:1.0.0
    container_name: ipcss-api
    ports:
      - "8080:8080"
    environment:
      - DB_URL=jdbc:postgresql://postgres:5432/ipcss
      - DB_USER=postgres
      - DB_PASSWORD=postgres
      - REDIS_URL=redis:6379
      - JWT_SECRET=${JWT_SECRET}
      - JWT_EXPIRATION=15m
      - REFRESH_TOKEN_EXPIRATION=7d
    volumes:
      - ./data/recordings:/app/recordings
      - ./data/logs:/app/logs
    depends_on:
      - postgres
      - redis
    restart: unless-stopped
    networks:
      - ipcss-network
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/api/v1/health"]
      interval: 30s
      timeout: 10s
      retries: 3
      start_period: 40s

  # PostgreSQL Database
  postgres:
    image: postgres:15-alpine
    container_name: ipcss-postgres
    ports:
      - "5432:5432"
    environment:
      - POSTGRES_DB=ipcss
      - POSTGRES_USER=postgres
      - POSTGRES_PASSWORD=postgres
    volumes:
      - postgres_data:/var/lib/postgresql/data
      - ./init-scripts:/docker-entrypoint-initdb.d
    restart: unless-stopped
    networks:
      - ipcss-network
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Redis Cache
  redis:
    image: redis:7-alpine
    container_name: ipcss-redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data
    restart: unless-stopped
    networks:
      - ipcss-network
    command: redis-server --appendonly yes
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5

  # Web Interface
  web:
    image: rekadzeav/ipcss-web:1.0.0
    container_name: ipcss-web
    ports:
      - "3000:3000"
    environment:
      - API_URL=http://localhost:8080/api/v1
    depends_on:
      - api
    restart: unless-stopped
    networks:
      - ipcss-network

  # Nginx Reverse Proxy
  nginx:
    image: nginx:alpine
    container_name: ipcss-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx.conf:/etc/nginx/nginx.conf:ro
      - ./ssl:/etc/nginx/ssl:ro
    depends_on:
      - api
      - web
    restart: unless-stopped
    networks:
      - ipcss-network

volumes:
  postgres_data:
  redis_data:

networks:
  ipcss-network:
    driver: bridge
```

### Nginx Configuration

**File:** `nginx.conf`

```nginx
events {
    worker_connections 1024;
}

http {
    upstream api_backend {
        server api:8080;
    }

    upstream web_backend {
        server web:3000;
    }

    server {
        listen 80;
        server_name localhost;

        # API routes
        location /api/ {
            proxy_pass http://api_backend/api/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_connect_timeout 60s;
            proxy_send_timeout 60s;
            proxy_read_timeout 60s;
        }

        # WebSocket routes
        location /api/v1/ws {
            proxy_pass http://api_backend/api/v1/ws;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_read_timeout 86400;
        }

        # Web interface
        location / {
            proxy_pass http://web_backend/;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }

        # SSL configuration (uncomment for production)
        # listen 443 ssl http2;
        # ssl_certificate /etc/nginx/ssl/cert.pem;
        # ssl_certificate_key /etc/nginx/ssl/key.pem;
    }
}
```

### Environment Variables

**File:** `.env`

```bash
# Database
DB_URL=jdbc:postgresql://postgres:5432/ipcss
DB_USER=postgres
DB_PASSWORD=your_secure_password_here

# Redis
REDIS_URL=redis:6379

# JWT
JWT_SECRET=your_super_secret_jwt_key_change_this_in_production
JWT_EXPIRATION=15m
REFRESH_TOKEN_EXPIRATION=7d

# Storage
STORAGE_PATH=/app/recordings
MAX_RECORDING_DAYS=30

# Server
API_PORT=8080
NODE_ENV=production

# Email (optional)
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=your_email@gmail.com
SMTP_PASSWORD=your_app_password

# Telegram (optional)
TELEGRAM_BOT_TOKEN=your_bot_token
```

### Production Deployment

```bash
# 1. Generate secure JWT secret
openssl rand -base64 64

# 2. Update .env with production values
nano .env

# 3. Build images
docker-compose build --no-cache

# 4. Start in detached mode
docker-compose up -d

# 5. Check logs
docker-compose logs -f

# 6. Monitor resources
docker stats
```

---

## Kubernetes Deployment

### Kubernetes Manifests

**File:** `k8s/namespace.yaml`

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: ipcss
  labels:
    name: ipcss
```

**File:** `k8s/configmap.yaml`

```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: ipcss-config
  namespace: ipcss
data:
  DB_HOST: postgres-service
  DB_PORT: "5432"
  DB_NAME: ipcss
  REDIS_HOST: redis-service
  REDIS_PORT: "6379"
  API_PORT: "8080"
  JWT_EXPIRATION: "15m"
  REFRESH_TOKEN_EXPIRATION: "168h"
  STORAGE_PATH: "/app/recordings"
  MAX_RECORDING_DAYS: "30"
```

**File:** `k8s/secrets.yaml`

```yaml
apiVersion: v1
kind: Secret
metadata:
  name: ipcss-secrets
  namespace: ipcss
type: Opaque
stringData:
  DB_PASSWORD: "your_secure_password_here"
  JWT_SECRET: "your_super_secret_jwt_key"
```

**File:** `k8s/postgres.yaml`

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-pvc
  namespace: ipcss
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 10Gi
  storageClassName: standard
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: postgres
  namespace: ipcss
spec:
  replicas: 1
  selector:
    matchLabels:
      app: postgres
  template:
    metadata:
      labels:
        app: postgres
    spec:
      containers:
        - name: postgres
          image: postgres:15-alpine
          ports:
            - containerPort: 5432
          env:
            - name: POSTGRES_DB
              valueFrom:
                configMapKeyRef:
                  name: ipcss-config
                  key: DB_NAME
            - name: POSTGRES_USER
              value: postgres
            - name: POSTGRES_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: ipcss-secrets
                  key: DB_PASSWORD
          volumeMounts:
            - name: postgres-storage
              mountPath: /var/lib/postgresql/data
          resources:
            requests:
              memory: "512Mi"
              cpu: "500m"
            limits:
              memory: "2Gi"
              cpu: "2000m"
          livenessProbe:
            exec:
              command:
                - pg_isready
                - -U
                - postgres
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            exec:
              command:
                - pg_isready
                - -U
                - postgres
            initialDelaySeconds: 5
            periodSeconds: 5
      volumes:
        - name: postgres-storage
          persistentVolumeClaim:
            claimName: postgres-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: postgres-service
  namespace: ipcss
spec:
  selector:
    app: postgres
  ports:
    - port: 5432
      targetPort: 5432
  type: ClusterIP
```

**File:** `k8s/redis.yaml`

```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: redis-pvc
  namespace: ipcss
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 5Gi
---
apiVersion: apps/v1
kind: Deployment
metadata:
  name: redis
  namespace: ipcss
spec:
  replicas: 1
  selector:
    matchLabels:
      app: redis
  template:
    metadata:
      labels:
        app: redis
    spec:
      containers:
        - name: redis
          image: redis:7-alpine
          ports:
            - containerPort: 6379
          volumeMounts:
            - name: redis-storage
              mountPath: /data
          resources:
            requests:
              memory: "256Mi"
              cpu: "250m"
            limits:
              memory: "1Gi"
              cpu: "1000m"
          livenessProbe:
            exec:
              command:
                - redis-cli
                - ping
            initialDelaySeconds: 30
            periodSeconds: 10
          readinessProbe:
            exec:
              command:
                - redis-cli
                - ping
            initialDelaySeconds: 5
            periodSeconds: 5
      volumes:
        - name: redis-storage
          persistentVolumeClaim:
            claimName: redis-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: redis-service
  namespace: ipcss
spec:
  selector:
    app: redis
  ports:
    - port: 6379
      targetPort: 6379
  type: ClusterIP
```

**File:** `k8s/api-deployment.yaml`

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ipcss-api
  namespace: ipcss
spec:
  replicas: 3
  selector:
    matchLabels:
      app: ipcss-api
  template:
    metadata:
      labels:
        app: ipcss-api
    spec:
      containers:
        - name: api
          image: rekadzeav/ipcss-api:1.0.0
          imagePullPolicy: Always
          ports:
            - containerPort: 8080
          envFrom:
            - configMapRef:
                name: ipcss-config
            - secretRef:
                name: ipcss-secrets
          volumeMounts:
            - name: recordings-storage
              mountPath: /app/recordings
          resources:
            requests:
              memory: "512Mi"
              cpu: "500m"
            limits:
              memory: "2Gi"
              cpu: "2000m"
          livenessProbe:
            httpGet:
              path: /api/v1/health/live
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 10
            timeoutSeconds: 5
            failureThreshold: 3
          readinessProbe:
            httpGet:
              path: /api/v1/health/ready
              port: 8080
            initialDelaySeconds: 10
            periodSeconds: 5
            timeoutSeconds: 3
            failureThreshold: 3
      volumes:
        - name: recordings-storage
          persistentVolumeClaim:
            claimName: recordings-pvc
---
apiVersion: v1
kind: Service
metadata:
  name: ipcss-api-service
  namespace: ipcss
spec:
  selector:
    app: ipcss-api
  ports:
    - port: 8080
      targetPort: 8080
  type: ClusterIP
```

**File:** `k8s/ingress.yaml`

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: ipcss-ingress
  namespace: ipcss
  annotations:
    nginx.ingress.kubernetes.io/rewrite-target: /
    nginx.ingress.kubernetes.io/proxy-body-size: "50m"
    nginx.ingress.kubernetes.io/proxy-read-timeout: "3600"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"
spec:
  ingressClassName: nginx
  tls:
    - hosts:
        - api.company.com
      secretName: ipcss-tls
  rules:
    - host: api.company.com
      http:
        paths:
          - path: /api
            pathType: Prefix
            backend:
              service:
                name: ipcss-api-service
                port:
                  number: 8080
```

### Deploy to Kubernetes

```bash
# 1. Create namespace
kubectl apply -f k8s/namespace.yaml

# 2. Apply configurations
kubectl apply -f k8s/

# 3. Check status
kubectl get all -n ipcss

# 4. Check logs
kubectl logs -n ipcss -l app=ipcss-api -f

# 5. Scale API deployment
kubectl scale deployment ipcss-api -n ipcss --replicas=5

# 6. Rolling update
kubectl set image deployment/ipcss-api ipcss-api=rekadzeav/ipcss-api:1.0.1 -n ipcss
kubectl rollout status deployment/ipcss-api -n ipcss
```

---

## CI/CD Pipelines

### GitHub Actions

**File:** `.github/workflows/ci.yml`

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  # Build and Test
  build:
    runs-on: ubuntu-latest
    strategy:
      matrix:
        module: [shared, server-api, server-web, client-desktop]

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Setup Gradle
        uses: gradle/gradle-build-action@v2

      - name: Build and Test
        run: |
          ./gradlew :${{ matrix.module }}:build

      - name: Upload Test Results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: test-results-${{ matrix.module }}
          path: ${{ matrix.module }}/build/test-results/

  # Run E2E Tests
  e2e:
    runs-on: ubuntu-latest
    needs: build

    steps:
      - uses: actions/checkout@v3

      - name: Set up JDK 17
        uses: actions/setup-java@v3
        with:
          java-version: '17'
          distribution: 'temurin'

      - name: Start Services
        run: |
          docker-compose up -d
          sleep 60

      - name: Run E2E Tests
        run: |
          ./gradlew :platforms:client-desktop-x86_64:app:e2eTest

      - name: Upload E2E Results
        uses: actions/upload-artifact@v3
        if: always()
        with:
          name: e2e-results
          path: platforms/client-desktop-x86_64/app/build/reports/tests/e2eTest/

  # Build Docker Images
  docker:
    runs-on: ubuntu-latest
    needs: [build, e2e]
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'

    steps:
      - uses: actions/checkout@v3

      - name: Log in to Container Registry
        uses: docker/login-action@v2
        with:
          registry: ${{ env.REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v4
        with:
          images: ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=ref,event=branch
            type=sha,prefix={{branch}}-
            type=semver,pattern={{version}}

      - name: Build and push
        uses: docker/build-push-action@v4
        with:
          context: .
          file: Dockerfile.api
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}

  # Deploy to Production
  deploy:
    runs-on: ubuntu-latest
    needs: docker
    if: github.event_name == 'push' && github.ref == 'refs/heads/main'

    steps:
      - uses: actions/checkout@v3

      - name: Deploy to Kubernetes
        uses: azure/k8s-deploy@v4
        with:
          namespace: ipcss
          manifests: |
            k8s/api-deployment.yaml
            k8s/ingress.yaml
          images: |
            ${{ env.REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}
```

### GitLab CI

**File:** `.gitlab-ci.yml`

```yaml
image: gradle:7.6-jdk17

variables:
  DOCKER_REGISTRY: registry.gitlab.com
  DOCKER_IMAGE: $CI_PROJECT_PATH

stages:
  - build
  - test
  - docker
  - deploy

build:
  stage: build
  script:
    - gradle build
  artifacts:
    paths:
      - build/libs/

test:
  stage: test
  script:
    - gradle test
  artifacts:
    reports:
      junit:
        - build/test-results/**/*.xml

docker-build:
  stage: docker
  only:
    - main
  script:
    - docker build -t $DOCKER_REGISTRY/$DOCKER_IMAGE:$CI_COMMIT_SHA .
    - docker login -u $CI_REGISTRY_USER -p $CI_REGISTRY_PASSWORD $DOCKER_REGISTRY
    - docker push $DOCKER_REGISTRY/$DOCKER_IMAGE:$CI_COMMIT_SHA

deploy:
  stage: deploy
  only:
    - main
  script:
    - kubectl set image deployment/ipcss-api ipcss-api=$DOCKER_REGISTRY/$DOCKER_IMAGE:$CI_COMMIT_SHA
    - kubectl rollout status deployment/ipcss-api
```

---

## Configuration

### Production Configuration

**File:** `application-prod.yml`

```yaml
server:
  port: 8080
  ssl:
    enabled: true
    key-store: classpath:keystore.p12
    key-store-password: ${SSL_KEYSTORE_PASSWORD}
    key-store-type: PKCS12

spring:
  datasource:
    url: jdbc:postgresql://${DB_HOST}:${DB_PORT}/${DB_NAME}
    username: ${DB_USER}
    password: ${DB_PASSWORD}
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000

logging:
  level:
    root: INFO
    com.company.ipcamera: DEBUG
  file:
    name: /var/log/ipcss/application.log
    max-size: 100MB
    max-history: 30

security:
  jwt:
    secret: ${JWT_SECRET}
    expiration: 900000
    refresh-expiration: 604800000

storage:
  recordings-path: ${STORAGE_PATH}
  max-days: ${MAX_RECORDING_DAYS}
  cleanup-cron: "0 0 2 * * *"
```

### Environment-Specific Configurations

```bash
# Development
export NODE_ENV=development
export DB_HOST=localhost
export DB_PORT=5432

# Staging
export NODE_ENV=staging
export DB_HOST=staging-db.internal
export DB_PORT=5432

# Production
export NODE_ENV=production
export DB_HOST=prod-db.internal
export DB_PORT=5432
```

---

## Monitoring & Logging

### Prometheus Metrics

```yaml
# File: prometheus.yml
scrape_configs:
  - job_name: 'ipcss-api'
    static_configs:
      - targets: ['api:8080']
    metrics_path: '/api/v1/health/metrics'
```

### Grafana Dashboard

Import dashboard ID: `12345` (example)

### Log Aggregation

**File:** `fluentd.conf`

```ruby
<source>
  @type tail
  path /var/log/ipcss/*.log
  pos_file /var/log/fluentd/ipcss.pos
  tag ipcss.api
  <parse>
    @type json
  </parse>
</source>

<match ipcss.**>
  @type elasticsearch
  host elasticsearch
  port 9200
  index_name ipcss-logs
</match>
```

---

## Backup & Recovery

### Automated Backup Script

**File:** `scripts/backup.sh`

```bash
#!/bin/bash

# Configuration
BACKUP_DIR="/backups"
DATE=$(date +%Y%m%d_%H%M%S)
RETENTION_DAYS=30

# Backup database
echo "Backing up database..."
docker exec ipcss-postgres pg_dump -U postgres ipcss > "$BACKUP_DIR/database_$DATE.sql"

# Backup recordings
echo "Backing up recordings..."
tar -czf "$BACKUP_DIR/recordings_$DATE.tar.gz" /app/recordings

# Backup configuration
echo "Backing up configuration..."
tar -czf "$BACKUP_DIR/config_$DATE.tar.gz" /app/config

# Cleanup old backups
echo "Cleaning up old backups..."
find $BACKUP_DIR -name "*.sql" -mtime +$RETENTION_DAYS -delete
find $BACKUP_DIR -name "*.tar.gz" -mtime +$RETENTION_DAYS -delete

echo "Backup completed: $DATE"
```

### Restore Procedure

```bash
# 1. Stop services
docker-compose down

# 2. Restore database
cat database_20260427_120000.sql | docker exec -i ipcss-postgres psql -U postgres ipcss

# 3. Restore recordings
tar -xzf recordings_20260427_120000.tar.gz -C /app/

# 4. Start services
docker-compose up -d
```

---

## Troubleshooting

### Common Issues

#### 1. Database Connection Failed

**Symptoms:**
```
Cannot create PoolableConnectionFactory
```

**Solution:**
```bash
# Check PostgreSQL is running
docker ps | grep postgres

# Check network
docker exec ipcss-api ping postgres

# Check credentials
docker exec ipcss-postgres psql -U postgres -c "SELECT version();"
```

#### 2. Out of Memory

**Symptoms:**
```
Java heap space
```

**Solution:**
```bash
# Increase memory limits in docker-compose.yml
services:
  api:
    mem_limit: 4g
    mem_reservation: 2g

# Or increase JVM heap
export JAVA_OPTS="-Xmx2g -Xms1g"
```

#### 3. Disk Space Full

**Symptoms:**
```
No space left on device
```

**Solution:**
```bash
# Check disk usage
df -h

# Clean old recordings
find /app/recordings -name "*.mp4" -mtime +30 -delete

# Clean Docker
docker system prune -a
```

#### 4. WebSocket Connection Failed

**Symptoms:**
```
WebSocket upgrade failed
```

**Solution:**
```bash
# Check nginx configuration
nginx -t

# Check WebSocket route
curl -i -N \
  -H "Connection: Upgrade" \
  -H "Upgrade: websocket" \
  -H "Host: localhost:8080" \
  http://localhost:8080/api/v1/ws
```

---

## Related Documentation

- [API.md](API.md) - API documentation
- [ARCHITECTURE.md](ARCHITECTURE.md) - Architecture overview
- [E2E_TESTING.md](../archive/docs/guides/E2E_TESTING.md) - E2E testing guide
- [CHANGELOG.md](../CHANGELOG.md) - Version history

---

**Last Updated:** 27 April 2026  
**Next Review:** 2026-05-27  
**Maintainer:** DevOps Team
