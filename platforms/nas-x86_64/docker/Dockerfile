# Dockerfile for IP-CSS NAS x86_64
# Multi-stage build for smaller image size

FROM eclipse-temurin:17-jdk-alpine AS builder

WORKDIR /app

# Copy Gradle wrapper and build files
COPY gradle/ ./gradle/
COPY gradlew ./
COPY gradlew.bat ./
COPY build.gradle.kts ./
COPY settings.gradle.kts ./
COPY gradle.properties ./

# Copy source code
COPY server/ ./server/
COPY shared/ ./shared/
COPY core/ ./core/

# Build the application
RUN ./gradlew :server:api:build --no-daemon -x test

# Runtime stage
FROM eclipse-temurin:17-jre-alpine

# Install dependencies
RUN apk add --no-cache \
    ffmpeg \
    bash \
    curl

# Create app user
RUN addgroup -g 1000 appgroup && \
    adduser -D -u 1000 -G appgroup appuser

WORKDIR /app

# Copy built JAR from builder
COPY --from=builder /app/server/api/build/libs/server-api-*.jar /app/server.jar

# Copy configuration
COPY platforms/nas-x86_64/server/application.conf /app/application.conf
COPY platforms/nas-x86_64/server/config.yaml /app/config.yaml

# Create directories
RUN mkdir -p /app/data/db /app/data/recordings /app/data/screenshots /app/logs && \
    chown -R appuser:appgroup /app

USER appuser

EXPOSE 8081

ENTRYPOINT ["java", "-jar", "/app/server.jar", "--config", "/app/config.yaml"]
