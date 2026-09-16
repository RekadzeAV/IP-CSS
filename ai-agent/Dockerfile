# Dockerfile for IP-CSS AI Agent
# Multi-stage build for production

# ============================================
# Build Stage
# ============================================
FROM python:3.11-slim as builder

WORKDIR /app

# Install build dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    build-essential \
    && rm -rf /var/lib/apt/lists/*

# Install Python dependencies
COPY ai-agent/requirements.txt .
RUN pip install --no-cache-dir --user -r requirements.txt

# ============================================
# Production Stage
# ============================================
FROM python:3.11-slim as production

WORKDIR /app

# Create non-root user
RUN groupadd -r aiagent && useradd -r -g aiagent aiagent

# Copy Python packages from builder
COPY --from=builder /root/.local /home/aiagent/.local

# Copy application code
COPY ai-agent/ src/
COPY credentials/ .

# Set environment variables
ENV PATH=/home/aiagent/.local/bin:$PATH
ENV PYTHONPATH=/app/src
ENV PYTHONDONTWRITEBYTECODE=1
ENV PYTHONUNBUFFERED=1

# Set working directory
WORKDIR /app/src

# Switch to non-root user
USER aiagent

# Expose port
EXPOSE 8001

# Health check
HEALTHCHECK --interval=30s --timeout=10s --start-period=5s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8001/api/v1/health')" || exit 1

# Run the application
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001"]

# ============================================
# Development Stage
# ============================================
FROM production as development

USER root

# Install development dependencies
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    vim \
    && rm -rf /var/lib/apt/lists/*

# Switch back to non-root user
USER aiagent

# Enable reload for development
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8001", "--reload"]
