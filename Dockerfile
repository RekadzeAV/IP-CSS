# Multi-stage build for IP Camera Surveillance System Server
# Uses: gradle:8.9-jdk17 for build, eclipse-temurin:17-jre-alpine for runtime

# Stage 1: Build
FROM gradle:8.9-jdk17 AS build

WORKDIR /app

# Copy Gradle wrapper and configuration first (leveraging Docker layer cache)
COPY gradle ./gradle
COPY gradlew gradlew.bat build.gradle.kts settings.gradle.kts gradle.properties ./

# Copy source code
COPY shared ./shared
COPY core ./core
COPY server/api ./server/api

# Build server:api distribution (installDist creates bin/ + lib/)
RUN gradle :server:api:installDist --no-daemon -x :shared:verifyCommonMainCameraDatabaseMigration -x ktlintCheck -q

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install runtime tools + create non-root user (single layer)
RUN apk add --no-cache curl ffmpeg && \
    addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser && \
    mkdir -p /app/config /app/recordings /app/database /app/logs /app/models /app/tmp /var/tmp

# Copy built distribution from build stage
COPY --from=build /app/server/api/build/install/api /app/dist

# Set ownership
RUN chown -R appuser:appuser /app

# Environment variables
ENV TZ=Europe/Moscow \
    JAVA_OPTS="-Xmx2048m -Xms512m"

# Expose ports
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/health || exit 1

# Switch to non-root user
USER appuser

# Run the server
ENTRYPOINT ["/app/dist/bin/api"]
