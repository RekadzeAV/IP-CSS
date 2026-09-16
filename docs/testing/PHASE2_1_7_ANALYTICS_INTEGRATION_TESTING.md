# Testing Guide: Phase 2.1.7 - AI Analytics Integration with Video Streams

**Дата:** 2026-04-27  
**Версия:** 1.0  
**Статус:** Production Monitor интегрирован ✅

---

## 📋 Обзор

Этот документ описывает как тестировать интеграцию AI аналитики с видеопотоками через Production Monitor.

**Цель:** Проверить что:
1. Production Monitor корректно регистрирует метрики аналитики
2. REST API endpoints возвращают корректные данные
3. Метрики обновляются в реальном времени при обработке кадров
4. Health checks работают корректно

---

## 🧪 Тестовые сценарии

### Сценарий 1: Проверка структуры API ответов

**Цель:** Убедиться что все endpoints возвращают корректную JSON структуру.

#### 1.1 Global Metrics

```bash
curl -X GET http://localhost:8080/api/v1/analytics/metrics/global \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Ожидаемый ответ:**
```json
{
  "totalCameras": 0,
  "activeCameras": 0,
  "totalFramesProcessed": 0,
  "totalFramesSkipped": 0,
  "totalErrors": 0,
  "totalMotionDetections": 0,
  "totalObjectDetections": 0,
  "totalFaceDetections": 0,
  "totalAnprDetections": 0,
  "lastFrameProcessedAt": 0,
  "lastErrorAt": 0
}
```

#### 1.2 Cameras List

```bash
curl -X GET http://localhost:8080/api/v1/analytics/metrics/cameras \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Ожидаемый ответ:**
```json
{
  "totalCameras": 0,
  "activeCameras": 0,
  "cameras": []
}
```

#### 1.3 Camera Metrics (non-existent)

```bash
curl -X GET http://localhost:8080/api/v1/analytics/metrics/cameras/test-camera \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Ожидаемый ответ:**
```json
{
  "cameraId": "test-camera",
  "cameraName": "",
  "frameSourceKind": "",
  "isRunning": false,
  "totalFramesProcessed": 0,
  "totalFramesSkipped": 0,
  "totalErrors": 0,
  "averageProcessingTimeMs": 0.0,
  "currentFps": 0.0,
  "lastFrameProcessedAt": 0,
  "lastError": null,
  "errorHistory": [],
  "detectorStats": null
}
```

#### 1.4 Camera Stats

```bash
curl -X GET http://localhost:8080/api/v1/analytics/metrics/stats/test-camera \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Ожидаемый ответ:**
```json
{
  "cameraId": "test-camera",
  "frameCount": 0,
  "isActive": false,
  "motionDetectionsCount": 0,
  "objectDetectionsCount": 0,
  "faceDetectionsCount": 0,
  "licensePlateRecognitionsCount": 0,
  "lastProcessedTimestamp": null
}
```

#### 1.5 Health Check

```bash
curl -X GET http://localhost:8080/api/v1/analytics/metrics/cameras/test-camera/health \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Ожидаемый ответ:**
```json
{
  "cameraId": "test-camera",
  "healthy": false,
  "recommendations": []
}
```

#### 1.6 Optimization Recommendations

```bash
curl -X GET http://localhost:8080/api/v1/analytics/metrics/cameras/test-camera/recommendations \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Ожидаемый ответ:**
```json
{
  "cameraId": "test-camera",
  "recommendations": []
}
```

#### 1.7 Camera Status

```bash
curl -X GET http://localhost:8080/api/v1/analytics/metrics/cameras/test-camera/status \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Ожидаемый ответ:**
```json
{
  "cameraId": "test-camera",
  "isRunning": false,
  "activeDetectors": []
}
```

---

### Сценарий 2: Интеграционный тест с реальной аналитикой

**Предусловия:**
- Сервер запущен
- JWT токен получен
- Камера добавлена в систему
- Включена хотя бы одна детекция (motion/object/face/anpr)

#### 2.1 Запуск потока и аналитики

```powershell
# 1. Запустить поток камеры
$streamResponse = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/streams/start" `
  -Method POST `
  -Headers @{ "Authorization" = "Bearer $JWT_TOKEN" } `
  -ContentType "application/json" `
  -Body '{"cameraId":"cam-1"}'

# 2. Запустить аналитику (через VideoAnalyticsService startAnalytics)
# Примечание: Это делается автоматически при запуске потока если включена аналитика
```

#### 2.2 Проверка метрик в реальном времени

```powershell
# Через 5 секунд после запуска
Start-Sleep -Seconds 5

# Получить метрики камеры
$metrics = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/analytics/metrics/cameras/cam-1" `
  -Method GET `
  -Headers @{ "Authorization" = "Bearer $JWT_TOKEN" }

# Ожидаем что:
Write-Host "Total Frames Processed: $($metrics.totalFramesProcessed)"
Write-Host "Is Running: $($metrics.isRunning)"
Write-Host "FPS: $($metrics.currentFps)"

# Проверить что кадры обрабатываются
if ($metrics.totalFramesProcessed -gt 0) {
    Write-Host "✅ Камеры обрабатывают кадры" -ForegroundColor Green
} else {
    Write-Host "⚠️ Камера не обрабатывает кадры" -ForegroundColor Yellow
}
```

#### 2.3 Проверка детекций

```powershell
# Получить статистику аналитики
$stats = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/analytics/metrics/stats/cam-1" `
  -Method GET `
  -Headers @{ "Authorization" = "Bearer $JWT_TOKEN" }

Write-Host "Motion Detections: $($stats.motionDetectionsCount)"
Write-Host "Object Detections: $($stats.objectDetectionsCount)"
Write-Host "Face Detections: $($stats.faceDetectionsCount)"
Write-Host "ANPR Detections: $($stats.licensePlateRecognitionsCount)"
```

#### 2.4 Проверка здоровья пайплайна

```powershell
$health = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/analytics/metrics/cameras/cam-1/health" `
  -Method GET `
  -Headers @{ "Authorization" = "Bearer $JWT_TOKEN" }

if ($health.healthy) {
    Write-Host "✅ Пайплайн аналитики здоров" -ForegroundColor Green
} else {
    Write-Host "⚠️ Пайплайн аналитики требует внимания" -ForegroundColor Yellow
    Write-Host "Рекомендации: $($health.recommendations -join ', ')"
}
```

#### 2.5 Проверка рекомендаций по оптимизации

```powershell
$recommendations = Invoke-RestMethod -Uri "http://localhost:8080/api/v1/analytics/metrics/cameras/cam-1/recommendations" `
  -Method GET `
  -Headers @{ "Authorization" = "Bearer $JWT_TOKEN" }

if ($recommendations.recommendations.Count -gt 0) {
    Write-Host "Рекомендации по оптимизации:" -ForegroundColor Cyan
    $recommendations.recommendations | ForEach-Object { Write-Host "  - $_" }
} else {
    Write-Host "✅ Нет рекомендаций по оптимизации" -ForegroundColor Green
}
```

---

### Сценарий 3: Unit тесты для Production Monitor

**Расположение:** `server/api/src/test/kotlin/.../service/analytics/AnalyticsProductionMonitorTest.kt`

#### 3.1 Тест регистрации камеры

```kotlin
@Test
fun `test startMonitoring registers camera`() {
    // Arrange
    val cameraId = "test-camera"
    val cameraName = "Test Camera"
    val frameSourceKind = "RTSP_DECODED"
    
    // Act
    productionMonitor.startMonitoring(cameraId, cameraName, frameSourceKind)
    
    // Assert
    val metrics = productionMonitor.getCameraMetrics(cameraId)
    assertNotNull(metrics)
    assertEquals(cameraId, metrics.cameraId)
    assertEquals(cameraName, metrics.cameraName)
    assertEquals(frameSourceKind, metrics.frameSourceKind)
    assertTrue(metrics.isRunning)
}
```

#### 3.2 Тест записи обработанного кадра

```kotlin
@Test
fun `test recordFrameProcessed increments counter`() {
    // Arrange
    val cameraId = "test-camera"
    productionMonitor.startMonitoring(cameraId, "Test", "RTSP_DECODED")
    
    // Act
    productionMonitor.recordFrameProcessed(cameraId, 50L)
    productionMonitor.recordFrameProcessed(cameraId, 50L)
    productionMonitor.recordFrameProcessed(cameraId, 50L)
    
    // Assert
    val metrics = productionMonitor.getCameraMetrics(cameraId)
    assertNotNull(metrics)
    assertEquals(3, metrics.totalFramesProcessed)
}
```

#### 3.3 Тест записи детекции

```kotlin
@Test
fun `test recordDetection increments detector stats`() {
    // Arrange
    val cameraId = "test-camera"
    productionMonitor.startMonitoring(cameraId, "Test", "RTSP_DECODED")
    
    // Act
    productionMonitor.recordDetection(cameraId, DetectionType.MOTION, 0.8f)
    productionMonitor.recordDetection(cameraId, DetectionType.MOTION, 0.9f)
    productionMonitor.recordDetection(cameraId, DetectionType.OBJECT, 0.7f)
    
    // Assert
    val metrics = productionMonitor.getCameraMetrics(cameraId)
    assertNotNull(metrics)
    assertNotNull(metrics.detectorStats)
    assertEquals(2, metrics.detectorStats.motionDetections)
    assertEquals(1, metrics.detectorStats.objectDetections)
}
```

#### 3.4 Тест записи ошибки

```kotlin
@Test
fun `test recordError logs error and increments counter`() {
    // Arrange
    val cameraId = "test-camera"
    productionMonitor.startMonitoring(cameraId, "Test", "RTSP_DECODED")
    val testError = RuntimeException("Test error")
    
    // Act
    productionMonitor.recordError(cameraId, testError, AnalyticsErrorType.FRAME_DECODE_ERROR)
    
    // Assert
    val metrics = productionMonitor.getCameraMetrics(cameraId)
    assertNotNull(metrics)
    assertEquals(1, metrics.totalErrors)
    assertNotNull(metrics.lastError)
    assertEquals("FRAME_DECODE_ERROR", metrics.lastError.errorType)
}
```

#### 3.5 Тест health check

```kotlin
@Test
fun `test isPipelineHealthy returns true when no recent errors`() {
    // Arrange
    val cameraId = "test-camera"
    productionMonitor.startMonitoring(cameraId, "Test", "RTSP_DECODED")
    productionMonitor.recordFrameProcessed(cameraId, 50L)
    
    // Act
    val isHealthy = productionMonitor.isPipelineHealthy(cameraId)
    
    // Assert
    assertTrue(isHealthy)
}
```

---

### Сценарий 4: Автоматизированный E2E тест

**Скрипт:** `scripts/test-analytics-integration.ps1`

```powershell
#!/usr/bin/env pwsh
param(
    [string]$BaseUrl = "http://localhost:8080",
    [string]$JwtToken,
    [string]$CameraId = "cam-e2e-test"
)

Write-Host "=== Phase 2.1.7 Analytics Integration Test ===" -ForegroundColor Cyan

# 1. Проверить глобальные метрики
Write-Host "`n[1/6] Checking global metrics..." -ForegroundColor Yellow
$globalMetrics = Invoke-RestMethod -Uri "$BaseUrl/api/v1/analytics/metrics/global" `
  -Headers @{ "Authorization" = "Bearer $JwtToken" }
Write-Host "  Total cameras: $($globalMetrics.totalCameras)"
Write-Host "  Active cameras: $($globalMetrics.activeCameras)"

# 2. Проверить список камер
Write-Host "`n[2/6] Checking cameras list..." -ForegroundColor Yellow
$cameras = Invoke-RestMethod -Uri "$BaseUrl/api/v1/analytics/metrics/cameras" `
  -Headers @{ "Authorization" = "Bearer $JwtToken" }
Write-Host "  Cameras count: $($cameras.totalCameras)"

# 3. Проверить метрики конкретной камеры
Write-Host "`n[3/6] Checking camera metrics..." -ForegroundColor Yellow
$cameraMetrics = Invoke-RestMethod -Uri "$BaseUrl/api/v1/analytics/metrics/cameras/$CameraId" `
  -Headers @{ "Authorization" = "Bearer $JwtToken" }
Write-Host "  Camera ID: $($cameraMetrics.cameraId)"
Write-Host "  Is running: $($cameraMetrics.isRunning)"

# 4. Проверить статус аналитики
Write-Host "`n[4/6] Checking analytics status..." -ForegroundColor Yellow
$analyticsStatus = Invoke-RestMethod -Uri "$BaseUrl/api/v1/analytics/metrics/cameras/$CameraId/status" `
  -Headers @{ "Authorization" = "Bearer $JwtToken" }
Write-Host "  Is running: $($analyticsStatus.isRunning)"
Write-Host "  Active detectors: $($analyticsStatus.activeDetectors -join ', ')"

# 5. Проверить health check
Write-Host "`n[5/6] Checking health..." -ForegroundColor Yellow
$health = Invoke-RestMethod -Uri "$BaseUrl/api/v1/analytics/metrics/cameras/$CameraId/health" `
  -Headers @{ "Authorization" = "Bearer $JwtToken" }
if ($health.healthy) {
    Write-Host "  Status: HEALTHY ✅" -ForegroundColor Green
} else {
    Write-Host "  Status: UNHEALTHY ⚠️" -ForegroundColor Yellow
    Write-Host "  Recommendations: $($health.recommendations -join ', ')"
}

# 6. Проверить рекомендации
Write-Host "`n[6/6] Checking recommendations..." -ForegroundColor Yellow
$recommendations = Invoke-RestMethod -Uri "$BaseUrl/api/v1/analytics/metrics/cameras/$CameraId/recommendations" `
  -Headers @{ "Authorization" = "Bearer $JwtToken" }
if ($recommendations.recommendations.Count -eq 0) {
    Write-Host "  No recommendations needed ✅" -ForegroundColor Green
} else {
    Write-Host "  Recommendations:" -ForegroundColor Yellow
    $recommendations.recommendations | ForEach-Object { Write-Host "    - $_" -ForegroundColor Yellow }
}

Write-Host "`n=== Test Complete ===" -ForegroundColor Cyan
```

**Запуск:**
```powershell
.\scripts\test-analytics-integration.ps1 `
  -JwtToken "YOUR_JWT_TOKEN" `
  -CameraId "cam-1"
```

---

## 📊 Метрики успеха

**Критерии приемки 2.1.7:**

1. ✅ Все 7 API endpoints возвращают корректную JSON структуру
2. ✅ Production Monitor регистрирует метрики при обработке кадров
3. ✅ Метрики обновляются в реальном времени (проверка через polling)
4. ✅ Health checks работают корректно
5. ✅ Рекомендации генерируются на основе метрик
6. ✅ Нет ошибок в логах при работе Production Monitor

---

## 🔍 Troubleshooting

### Проблема: Endpoints возвращают 401 Unauthorized

**Решение:** Убедиться что JWT токен передан в заголовке Authorization:
```bash
curl -H "Authorization: Bearer YOUR_TOKEN" ...
```

### Проблема: Метрики не обновляются

**Возможные причины:**
1. Аналитика не запущена для камеры
2. Камера не обрабатывает кадры
3. Production Monitor не инжектен в VideoAnalyticsService

**Проверка:**
```kotlin
// В логах сервера искать:
"Started analytics for camera: cam-1 (frameSource=RTSP_DECODED)"
```

### Проблема: Health check возвращает unhealthy

**Возможные причины:**
1. Много ошибок за последние 5 минут (>10)
2. Камера не обрабатывала кадры последние 60 секунд

**Решение:** Проверить логи на ошибки, убедиться что камера активна

---

## 📝 Next Steps

После успешного тестирования 2.1.7:

1. Перейти к **1.8.4 RTSP Native Integration** (критический blocker)
2. Улучшить стабильность RTSP клиента
3. Довести fallback path
4. Runtime stability testing

---

**Документ создан:** 2026-04-27  
**Версия:** 1.0  
**Статус:** Актуален
