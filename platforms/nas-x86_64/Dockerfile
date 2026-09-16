###############################################################################
# IP-CSS NAS Docker Image (x86_64)
# Дата: 17.07.2026
# Базируется на Alpine Linux для минимального размера
###############################################################################

FROM alpine:3.20 AS base

RUN apk add --no-cache \
    openjdk17-jre-headless \
    ffmpeg \
    ca-certificates \
    tzdata \
    su-exec

ENV LANG=en_US.UTF-8 \
    TZ=UTC \
    JAVA_OPTS="-Xms256m -Xmx1g -XX:+UseZGC"

FROM base AS server

WORKDIR /app

# Копируем сервер
COPY server/api/build/libs/server-api-*.jar /app/server.jar
COPY server/api/exports /app/exports
COPY server/api/streams /app/streams

# Копируем конфигурацию
COPY config /app/config

# Копируем web интерфейс
COPY server/web /app/web

# Копируем скрипты
COPY scripts/docker-entrypoint.sh /usr/local/bin/
RUN chmod +x /usr/local/bin/docker-entrypoint.sh

EXPOSE 8080 8443

HEALTHCHECK --interval=30s --timeout=10s --start-period=60s --retries=3 \
    CMD wget -qO- http://localhost:8080/api/v1/health || exit 1

ENTRYPOINT ["docker-entrypoint.sh"]
CMD ["java", $JAVA_OPTS, "-jar", "/app/server.jar"]

# ========== Multi-stage для AI-аналитики ==========
FROM base AS ai

RUN apk add --no-cache \
    python3 \
    py3-pip \
    py3-numpy \
    py3-opencv \
    tensorflow

WORKDIR /app
COPY ai-agent /app/ai-agent

RUN pip3 install -r /app/ai-agent/requirements.txt --no-cache-dir

EXPOSE 5000

CMD ["python3", "/app/ai-agent/src/main.py"]

# ========== Production образ ==========
FROM server AS production

# Дополнительные инструменты для production
RUN apk add --no-cache \
    curl \
    htop \
    iotop \
    logrotate

COPY docker-compose.yml /app/

LABEL \
    maintainer="IP-CSS Team" \
    version="0.3.0" \
    description="IP Camera Surveillance System"
