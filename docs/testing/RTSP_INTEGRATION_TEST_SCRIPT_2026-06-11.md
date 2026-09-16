# Test Script for RTSP Client Integration

**Дата:** 11 June 2026  
**Цель:** Автоматизированное тестирование RTSP client с MediaMTX

---

## Quick Start

### 1. Запуск MediaMTX

```powershell
# Если контейнер не запущен
docker start ip-camera-mediamtx

# Проверка что MediaMTX работает
docker logs ip-camera-mediamtx --tail 10
```

**Ожидаемый вывод:**
```
2026/06/11 INF [RTSP] started with listeners on :8554 (TCP/RTSP), :8000 (UDP/RTP), :8001 (UDP/RTCP)
```

---

### 2. Публикация тестового видео

**Вариант A: FFmpeg (рекомендуется)**

```powershell
# Публикация тестового видео в MediaMTX
ffmpeg -re -i test.mp4 -c copy -f rtsp rtsp://localhost:8554/test
```

**Вариант B: Генерация тестового паттерна**

```powershell
# Генерация видео-паттерна (не требует input файла)
ffmpeg -f lavfi -i testsrc=duration=60:size=1920x1080:rate=25 -c:v libx264 -preset ultrafast -f rtsp rtsp://localhost:8554/test
```

**Вариант C: Аудио + Видео**

```powershell
ffmpeg -re -f lavfi -i testsrc=duration=60:size=1920x1080:rate=25 -f lavfi -i sine=frequency=1000:duration=60 -c:v libx264 -preset ultrafast -c:a aac -f rtsp rtsp://localhost:8554/test
```

---

### 3. Запуск интеграционных тестов

**Windows PowerShell:**

```powershell
# Запуск всех RTSP тестов
.\gradlew.bat :core:network:desktopTest --tests "*NativeRtspClientLiveFrameTest*" --no-daemon

# Запуск конкретного теста
.\gradlew.bat :core:network:desktopTest --tests "*NativeRtspClientLiveFrameTest.testStreamDiscoveryAfterConnect*" --no-daemon
```

**Linux/Mac:**

```bash
./gradlew :core:network:desktopTest --tests "*NativeRtspClientLiveFrameTest*" --no-daemon
```

---

### 4. Проверка результатов

**MediaMTX Logs:**

```powershell
docker logs ip-camera-mediamtx --tail 50 | Select-String -Pattern "RTSP|test|conn"
```

**Ожидаемый вывод:**
```
2026/06/11 INF [RTSP] [conn 172.18.0.1:XXXXX] opened
2026/06/11 INF [RTSP] [path test] found
2026/06/11 INF [RTSP] [path test] ready: proto 0; port 0-0; codec H264
2026/06/11 INF [RTSP] [path test] running, ready to stream
```

**Test Output:**

```
> Task :core:network:desktopTest

NativeRtspClientLiveFrameTest[desktop] > testStreamDiscoveryAfterConnect()[desktop] PASSED
NativeRtspClientLiveFrameTest[desktop] > testVideoFrameReception()[desktop] PASSED
NativeRtspClientLiveFrameTest[desktop] > testAudioFrameReception()[desktop] PASSED

BUILD SUCCESSFUL
```

---

## Full Integration Test Script

### Windows PowerShell

```powershell
# test_rtsp_integration.ps1

Write-Host "=== RTSP Integration Test Suite ===" -ForegroundColor Cyan

# Step 1: Проверка MediaMTX
Write-Host "`n[1/5] Checking MediaMTX..." -ForegroundColor Yellow
$mediamtxStatus = docker ps --filter "name=ip-camera-mediamtx" --format "{{.Status}}"
if ($mediamtxStatus -notlike "*Up*") {
    Write-Host "Starting MediaMTX..." -ForegroundColor Yellow
    docker start ip-camera-mediamtx
    Start-Sleep -Seconds 3
}
Write-Host "MediaMTX is running" -ForegroundColor Green

# Step 2: Публикация тестового видео
Write-Host "`n[2/5] Publishing test video..." -ForegroundColor Yellow
$ffmpegProcess = Start-Process ffmpeg -ArgumentList "-f","lavfi","-i","testsrc=duration=30:size=1920x1080:rate=25","-c:v","libx264","-preset","ultrafast","-f","rtsp","rtsp://localhost:8554/test" -PassThru -WindowStyle Hidden
Write-Host "FFmpeg started (PID: $($ffmpegProcess.Id))" -ForegroundColor Green
Start-Sleep -Seconds 2

# Step 3: Проверка что поток опубликован
Write-Host "`n[3/5] Verifying stream publication..." -ForegroundColor Yellow
Start-Sleep -Seconds 2
$streamLogs = docker logs ip-camera-mediamtx --tail 20 2>&1 | Select-String -Pattern "ready|running"
if ($streamLogs) {
    Write-Host "Stream is ready" -ForegroundColor Green
} else {
    Write-Host "Warning: Stream may not be ready yet" -ForegroundColor Yellow
}

# Step 4: Запуск тестов
Write-Host "`n[4/5] Running integration tests..." -ForegroundColor Yellow
$testResult = & .\gradlew.bat :core:network:desktopTest --tests "*NativeRtspClientLiveFrameTest*" --no-daemon 2>&1
if ($LASTEXITCODE -eq 0) {
    Write-Host "All tests PASSED" -ForegroundColor Green
} else {
    Write-Host "Some tests FAILED" -ForegroundColor Red
    $testResult | Select-String -Pattern "FAILED|PASSED" | ForEach-Object { Write-Host $_ }
}

# Step 5: Остановка FFmpeg
Write-Host "`n[5/5] Cleaning up..." -ForegroundColor Yellow
Stop-Process -Id $ffmpegProcess.Id -Force -ErrorAction SilentlyContinue
Write-Host "FFmpeg stopped" -ForegroundColor Green

Write-Host "`n=== Test Suite Complete ===" -ForegroundColor Cyan
```

### Linux/Bash

```bash
#!/bin/bash
# test_rtsp_integration.sh

echo "=== RTSP Integration Test Suite ==="

# Step 1: Проверка MediaMTX
echo -e "\n[1/5] Checking MediaMTX..."
if ! docker ps --filter "name=ip-camera-mediamtx" --format "{{.Status}}" | grep -q "Up"; then
    echo "Starting MediaMTX..."
    docker start ip-camera-mediamtx
    sleep 3
fi
echo "MediaMTX is running"

# Step 2: Публикация тестового видео
echo -e "\n[2/5] Publishing test video..."
ffmpeg -f lavfi -i testsrc=duration=30:size=1920x1080:rate=25 -c:v libx264 -preset ultrafast -f rtsp rtsp://localhost:8554/test &
FFMPEG_PID=$!
echo "FFmpeg started (PID: $FFMPEG_PID)"
sleep 2

# Step 3: Проверка что поток опубликован
echo -e "\n[3/5] Verifying stream publication..."
sleep 2
if docker logs ip-camera-mediamtx --tail 20 2>&1 | grep -q "ready\|running"; then
    echo "Stream is ready"
else
    echo "Warning: Stream may not be ready yet"
fi

# Step 4: Запуск тестов
echo -e "\n[4/5] Running integration tests..."
./gradlew :core:network:desktopTest --tests "*NativeRtspClientLiveFrameTest*" --no-daemon
if [ $? -eq 0 ]; then
    echo "All tests PASSED"
else
    echo "Some tests FAILED"
fi

# Step 5: Остановка FFmpeg
echo -e "\n[5/5] Cleaning up..."
kill $FFMPEG_PID 2>/dev/null
echo "FFmpeg stopped"

echo -e "\n=== Test Suite Complete ==="
```

---

## Manual Testing

### Проверка подключения через VLC

```powershell
# Откройте VLC и перейдите:
# Media → Open Network Stream
# Введите: rtsp://localhost:8554/test
```

### Проверка через ffplay

```powershell
ffplay rtsp://localhost:8554/test
```

### Проверка через curl (RTSP OPTIONS)

```powershell
curl -v rtsp://localhost:8554/test
```

---

## Troubleshooting

### Проблема: MediaMTX не запускается

**Решение:**
```powershell
docker logs ip-camera-mediamtx
docker restart ip-camera-mediamtx
```

### Проблема: FFmpeg не может опубликовать поток

**Решение:**
```powershell
# Проверка что порт 8554 не занят
netstat -an | Select-String ":8554"

# Проверка что FFmpeg установлен
ffmpeg -version
```

### Проблема: Тесты падают с "Connection failed"

**Решение:**
```powershell
# Проверка что DLL обновлена
Get-Item native/video-processing/lib/windows/x64/video_processing.dll | Select-Object LastWriteTime

# Пересборка
cd native/video-processing/build
cmake --build . --config Release
```

### Проблема: Тесты падают с "no stream is available"

**Решение:**
```powershell
# Убедитесь что FFmpeg публикует поток
docker logs ip-camera-mediamtx --tail 20 | Select-String "test"

# Проверка что путь правильный
# RTSP URL должен быть: rtsp://127.0.0.1:8554/test
```

---

## Performance Testing

### Измерение задержки

```powershell
# Замер времени от PLAY до получения первого кадра
$startTime = Get-Date
rtsp_client_play($client)
$firstFrame = Wait-For-First-Frame -Timeout 10
$latency = (Get-Date) - $startTime
Write-Host "Latency: $($latency.TotalMilliseconds)ms"
```

### Измерение FPS

```powershell
# Подсчет кадров за 10 секунд
$frameCount = 0
$startTime = Get-Date
while ((Get-Date) - $startTime -lt [TimeSpan]::FromSeconds(10)) {
    Wait-For-Frame
    $frameCount++
}
$fps = $frameCount / 10
Write-Host "FPS: $fps"
```

---

**Создан:** 11 June 2026  
**Автор:** Koda AI Assistant  
**Версия:** 1.0
