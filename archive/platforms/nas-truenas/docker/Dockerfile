# Multi-stage Dockerfile for IP-CSS
# Optimized for TrueNAS SCALE and general Docker deployment

# Stage 1: Build
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copy build files
COPY build.gradle.kts settings.gradle.kts ./
COPY shared/build.gradle.kts shared/
COPY server/api/build.gradle.kts server/api/
COPY webApp/build.gradle.kts webApp/
COPY desktopApp/build.gradle.kts desktopApp/

# Download dependencies
RUN gradle dependencies --no-daemon || true

# Copy source code
COPY shared/src shared/src
COPY server/api/src server/api/src
COPY webApp/src webApp/src
COPY desktopApp/src desktopApp/src

# Build application
RUN gradle :server/api:assembleDist --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

# Install required packages
RUN apk add --no-cache \
    ffmpeg \
    opencv \
    libstdc++ \
    curl \
    tini

# Create non-root user
RUN addgroup -g 1001 ipcss && \
    adduser -D -u 1001 -G ipcss ipcss

WORKDIR /app

# Copy built application from builder
COPY --from=builder --chown=ipcss:ipcss /app/server/api/build/distributions/ ./

# Create data directories
RUN mkdir -p /data/recordings /data/config /data/database /data/logs && \
    chown -R ipcss:ipcss /data

# Set environment variables
ENV JAVA_OPTS="-Xmx2g -XX:+UseG1GC -XX:MaxGCPauseMillis=200"
ENV SERVER_PORT=8080
ENV DATA_PATH=/data

# Expose ports
EXPOSE 8080 8443 554 8554

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
    CMD curl -f http://localhost:8080/api/health || exit 1

# Switch to non-root user
USER ipcss

# Use tini as init system
ENTRYPOINT ["/sbin/tini", "--"]

# Run application
CMD ["sh", "-c", "java $JAVA_OPTS -jar ip-css-server.jar"]
