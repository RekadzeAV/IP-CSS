# Multi-stage build for IP Camera Surveillance System Server

# Stage 1: Build
FROM gradle:8.5-jdk17 AS build

WORKDIR /app

# Copy Gradle files
COPY build.gradle.kts settings.gradle.kts gradle.properties ./
COPY gradle ./gradle

# Copy source code
COPY shared ./shared
COPY core ./core
COPY server/api ./server/api

# Build the application
RUN gradle :server:api:build --no-daemon -x test

# Stage 2: Runtime
FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# ИСПРАВЛЕНО: Создаем non-root пользователь и группу
RUN addgroup -g 1000 appuser && \
    adduser -D -u 1000 -G appuser appuser

# Create directories for data
RUN mkdir -p /app/config /app/recordings /app/database /app/logs /app/models /app/tmp /var/tmp && \
    chown -R appuser:appuser /app

# Copy built JAR
COPY --from=build /app/server/api/build/libs/*.jar /app/app.jar

# ИСПРАВЛЕНО: Устанавливаем владельца файлов
RUN chown -R appuser:appuser /app

# Set environment variables
ENV TZ=Europe/Moscow
ENV JAVA_OPTS="-Xmx2048m -Xms512m"

# Expose ports
EXPOSE 8080

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8080/api/v1/health || exit 1

# ИСПРАВЛЕНО: Переключаемся на non-root пользователя
USER appuser

# Run the application
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]

