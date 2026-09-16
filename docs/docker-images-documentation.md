# 📋 Docker Images Documentation

## ✅ Task Progress Checklist:
- [x] Create comprehensive documentation for all Docker images used in IP-CSS project
- [x] Include purpose, usage, testing methods, performance evaluation criteria
- [ ] Document design assessment procedures using these images  
- [ ] Update existing docker-compose.ai.yml if needed for clarity

## 📂 Current Project Context:
1. **Dockerfile**: Multi-stage build for server API (build stage with Gradle, runtime stage with Java 17 JRE)
2. **docker-compose.ai.yml**: AI infrastructure services (ChromaDB, Redis)

## 🎯 Objectives:
- Serve as reference for all Docker-based operations in the project
- Standardize image usage across testing and performance evaluation
- Maintain clear separation between development, test, and production images

## 📋 Image Categories to Document:

### 1. **Base Build Image** (`Dockerfile`)
   - Purpose: Build server API application
   - Usage: CI/CD pipelines, local development with Docker
   - Testing: Unit/integration tests within container (MigrationDataSafetyIntegrationTest.kt)
   - Performance Evaluation: Benchmarking API response times (<200ms avg)

### 2. **AI Infrastructure Services**
   - `chromadb/chroma:latest`
     - Purpose: Vector database for agent memory
     - Usage: Local API testing, data persistence
     - Design Assessment: Memory vectorization efficiency (measure embedding generation time)
     - Performance Evaluation: Throughput under load (100ms per 100 queries)

   - `redis:7-alpine`
     - Purpose: AI queue system (separate from main Redis)
     - Usage: Message passing between services
     - Design Assessment: Memory footprint optimization
     - Performance Evaluation: Queue depth and latency (<5ms response time)
     - Throughput Test: 100 messages/sec sustained

### 3. **Manual Testing Setup**
   - Current setup: `docker-compose up` command with Ollama host integration
   - Docker images for manual validation
   - Verification procedures using port mappings (8000, 6380)

## 📝 Documentation Structure:
1. Table of Contents (auto-generated)
2. Image Overview Matrix
3. Usage Instructions
4. Testing Procedures
5. Performance Evaluation Methods
6. Design Assessment Framework

## ⚙️ Performance Evaluation Methods:

### For Base Build Image:
- API latency testing: `ab -n 1000 -c 10 http://localhost:8080/api/v1/health`
- Resource consumption monitoring via Docker stats

### For AI Infrastructure Services:
- ChromaDB: Embedding generation time, query performance
- Redis: Queue depth, response latency, memory usage (via redis-cli memory)

## 🎨 Design Assessment Framework:

### Vector Database (`chromadb`):
- Embedding quality: Vector retrieval accuracy (<0.25 MSE threshold)
- Memory efficiency: Storage vs processing ratio

### AI Queue System:
- Throughput: Messages/sec
- Latency: Request to response time  
- Stability: No dropped messages under 100 concurrent connections

## 🔧 Docker Usage Patterns:

- All images use `ai-network` bridge for inter-service communication
- Services restart on failure but preserve data (ChromaDB)
- Health checks prevent service disruption
- Port mappings allow direct host access when needed