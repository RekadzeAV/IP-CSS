# Android Testing Plan

**Дата:** 2026-04-27  
**Статус:** 🟡 READY FOR EXECUTION  
**Оценка:** 2-3 недели

---

## 📋 Обзор

Цель: Полное тестирование Android приложения на реальных устройствах.

### Scope:
- Video playback
- Background recording
- Permissions handling
- Performance monitoring
- Crash reporting

---

## 📱 Тестовые устройства

### Required Devices:

| Device | Android Version | Resolution | Priority |
|--------|----------------|------------|----------|
| Samsung Galaxy S21 | 13 | 1080p | P0 |
| Samsung Galaxy A52 | 12 | 1080p | P0 |
| Google Pixel 6 | 13 | 1080p | P0 |
| Google Pixel 4a | 11 | 1080p | P1 |
| Xiaomi Mi 11 | 12 | 1440p | P1 |
| OnePlus 9 | 12 | 1080p | P1 |
| Huawei P40 | 10 | 1080p | P2 |

### Minimum Coverage:
- Android 10, 11, 12, 13, 14
- Different manufacturers (Samsung, Google, Xiaomi, OnePlus)
- Different screen sizes (6", 6.5", 7")

---

## 🎯 Test Scenarios

### 1. Video Playback Testing

#### 1.1 Basic Playback
**Цель:** Проверить базовое воспроизведение видео

**Сценарий:**
```
1. Открыть приложение
2. Добавить RTSP камеру
3. Нажать на превью камеры
4. Проверить что видео запускается
5. Проверить FPS и качество
```

**Ожидаемые результаты:**
- Video starts within 3 seconds
- FPS >= 20 для 1080p stream
- No stuttering или freezing
- Audio sync correct

**Метрики:**
- Startup time: _____ ms
- First frame time: _____ ms
- Average FPS: _____
- Dropped frames: _____

---

#### 1.2 Multi-Camera View
**Цель:** Проверить воспроизведение нескольких камер

**Сценарий:**
```
1. Добавить 4 камеры
2. Переключиться на grid view
3. Проверить все 4 потока одновременно
4. Проверить CPU и memory usage
```

**Ожидаемые результаты:**
- All 4 streams visible
- FPS >= 15 per camera
- CPU usage < 80%
- Memory usage < 2GB

**Метрики:**
- Total FPS: _____
- CPU usage: _____%
- Memory usage: _____ MB
- Thermal throttling: Yes/No

---

#### 1.3 Landscape/Portrait Handling
**Цель:** Проверить ориентацию экрана

**Сценарий:**
```
1. Открыть видео в portrait
2. Повернуть устройство в landscape
3. Проверить что video переориентировалось
4. Повернуть обратно в portrait
```

**Ожидаемые результаты:**
- Smooth orientation change
- No video restart
- Correct aspect ratio

---

#### 1.4 Picture-in-Picture Mode
**Цель:** Проверить PiP режим

**Сценарий:**
```
1. Начать воспроизведение видео
2. Выйти из приложения (PiP активирован)
3. Проверить что видео продолжается в PiP
4. Вернуться в приложение
```

**Ожидаемые результаты:**
- PiP mode activated
- Video continues in PiP
- Smooth return to app

---

### 2. Background Recording Testing

#### 2.1 Background Recording Start
**Цель:** Проверить запись в фоне

**Сценарий:**
```
1. Начать воспроизведение видео
2. Выйти из приложения
3. Проверить что запись продолжается
4. Через 5 минут вернуться в приложение
5. Проверить что запись сохранена
```

**Ожидаемые результаты:**
- Recording continues in background
- No interruption
- File saved correctly

**Метрики:**
- Recording duration: _____ min
- File size: _____ MB
- Playback works: Yes/No

---

#### 2.2 Background Recording Lifecycle
**Цель:** Проверить lifecycle при фоновой записи

**Сценарий:**
```
1. Начать запись в фоне
2. Убить процесс приложения
3. Перезапустить приложение
4. Проверить статус записи
```

**Ожидаемые результаты:**
- Recording stopped gracefully
- Partial file saved
- No crash

---

#### 2.3 Notification Persistence
**Цель:** Проверить notification при фоновой записи

**Сценарий:**
```
1. Начать запись в фоне
2. Проверить notification
3. Нажать stop в notification
4. Проверить что запись остановлена
```

**Ожидаемые результаты:**
- Notification visible
- Stop button works
- Recording stopped

---

### 3. Permissions Testing

#### 3.1 Camera Permission
**Цель:** Проверить обработку camera permission

**Сценарий:**
```
1. Установить приложение
2. Открыть приложение (permission запрос)
3. Разрешить camera
4. Проверить что camera работает
5. Запретить camera
6. Проверить error handling
```

**Ожидаемые результаты:**
- Permission request shown
- Camera works when granted
- Graceful error when denied

---

#### 3.2 Storage Permission
**Цель:** Проверить обработку storage permission

**Сценарий:**
```
1. Начать запись
2. Permission request для storage
3. Разрешить storage
4. Проверить что запись работает
5. Запретить storage
6. Проверить error handling
```

**Ожидаемые результаты:**
- Permission request shown
- Recording works when granted
- Error message when denied
- Option to re-request permission

---

#### 3.3 Microphone Permission
**Цель:** Проверить обработку microphone permission

**Сценарий:**
```
1. Начать запись с аудио
2. Permission request для microphone
3. Разрешить microphone
4. Проверить что audio записывается
5. Запретить microphone
6. Проверить что видео записывается без audio
```

**Ожидаемые результаты:**
- Permission request shown
- Audio recorded when granted
- Video only when denied

---

#### 3.4 Permission Revocation Recovery
**Цель:** Проверить recovery после revoke permission

**Сценарий:**
```
1. Запустить приложение с разрешениями
2. Убрать разрешения в Settings
3. Перезапустить приложение
4. Проверить что приложение просит разрешения снова
```

**Ожидаемые результаты:**
- App detects revoked permissions
- Re-request permissions
- Graceful handling

---

### 4. Performance Testing

#### 4.1 Memory Usage
**Цель:** Измерить memory usage

**Инструменты:** Android Profiler, LeakCanary

**Сценарий:**
```
1. Запустить приложение
2. Записать baseline memory
3. Открыть 4 камеры
4. Измерить memory через 10 минут
5. Закрыть все камеры
6. Проверить memory leak
```

**Ожидаемые результаты:**
- Baseline memory: < 200MB
- With 4 cameras: < 2GB
- After close: < 300MB
- No memory leaks

**Метрики:**
```
Baseline: _____ MB
With 4 cameras: _____ MB
After close: _____ MB
Memory leak: Yes/No
```

---

#### 4.2 CPU Usage
**Цель:** Измерить CPU usage

**Инструменты:** Android Profiler

**Сценарий:**
```
1. Запустить приложение
2. Открыть 1 камеру
3. Измерить CPU usage
4. Открыть 4 камеры
5. Измерить CPU usage
```

**Ожидаемые результаты:**
- 1 camera: CPU < 30%
- 4 cameras: CPU < 80%
- No thermal throttling

**Метрики:**
```
1 camera: _____%
4 cameras: _____%
Thermal throttling: Yes/No
```

---

#### 4.3 Battery Usage
**Цель:** Измерить battery impact

**Инструменты:** Battery Historian

**Сценарий:**
```
1. Зарядить устройство до 100%
2. Запустить приложение
3. Открыть 2 камеры
4. Запустить 1 час
5. Проверить battery drain
```

**Ожидаемые результаты:**
- 1 hour: Battery drain < 15%
- Background recording: Battery drain < 10%

**Метрики:**
```
1 hour drain: _____%
Background drain: _____%
```

---

#### 4.4 Network Usage
**Цель:** Измерить network usage

**Инструменты:** Network Profiler

**Сценарий:**
```
1. Запустить приложение
2. Открыть 4 камеры (1080p)
3. Запустить 30 минут
4. Проверить network usage
```

**Ожидаемые результаты:**
- 4 cameras (1080p): < 1GB / hour
- Adaptive bitrate работает

**Метрики:**
```
30 minutes: _____ MB
1 hour estimate: _____ MB
```

---

### 5. Crash Testing

#### 5.1 OOM Handling
**Цель:** Проверить обработку Out Of Memory

**Сценарий:**
```
1. Запустить приложение
2. Открыть 10 камер
3. Подождать OOM
4. Проверить что приложение не крашится
```

**Ожидаемые результаты:**
- Graceful degradation
- Close some cameras
- Error message to user

---

#### 5.2 Network Loss Recovery
**Цель:** Проверить recovery после потери сети

**Сценарий:**
```
1. Запустить приложение
2. Открыть камеры
3. Отключить network
4. Подождать 30 секунд
5. Включить network
6. Проверить что камеры восстановились
```

**Ожидаемые результаты:**
- Auto-reconnect
- No manual intervention needed
- Recovery time < 10 seconds

---

#### 5.3 Low Storage Handling
**Цель:** Проверить обработку low storage

**Сценарий:**
```
1. Запустить запись
2. Заполнить storage до 95%
3. Проверить что запись остановлена
4. Проверить error message
```

**Ожидаемые результаты:**
- Graceful stop
- Error message
- Option to free space

---

## 📊 Crashlytics Setup

### Step 1: Добавить Firebase

```gradle
// build.gradle (app)
plugins {
    id 'com.google.gms.google-services'
    id 'com.google.firebase.crashlytics'
}

dependencies {
    implementation 'com.google.firebase:firebase-crashlytics-ktx'
    implementation 'com.google.firebase:firebase-analytics-ktx'
}
```

### Step 2: Инициализация

```kotlin
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        
        FirebaseCrashlytics.getInstance().setCustomKey("app_version", BuildConfig.VERSION_NAME)
        FirebaseCrashlytics.getInstance().setCustomKey("device_model", Build.MODEL)
        FirebaseCrashlytics.getInstance().setCustomKey("android_version", Build.VERSION.RELEASE)
    }
}
```

### Step 3: Отслеживание ошибок

```kotlin
try {
    // code that might throw
} catch (e: Exception) {
    FirebaseCrashlytics.getInstance().recordException(e)
    FirebaseCrashlytics.getInstance().setCustomKey("error_context", "video_playback")
}
```

---

## 📁 Test Execution Script

### PowerShell Script:

```powershell
# android-test-execution.ps1

param(
    [string]$DeviceId = "",
    [string]$TestSuite = "all"
)

# Install app
adb $DeviceId install -r app-release.apk

# Run tests
if ($TestSuite -eq "video") {
    adb $DeviceId shell am instrument -w \
        com.company.ipcamera.test/androidx.test.runner.AndroidJUnitRunner
}

# Collect logs
adb $DeviceId logcat -d > test-logcat.txt

# Collect crash reports
adb $DeviceId shell dumpsys crash > crash-reports.txt

# Generate report
GenerateTestReport -Logs "test-logcat.txt" -Crashes "crash-reports.txt"
```

---

## 📊 Test Report Template

```markdown
# Android Test Report

**Device:** Samsung Galaxy S21  
**Android Version:** 13  
**App Version:** 1.0.0  
**Date:** 2026-04-27

## Test Results

| Test Suite | Passed | Failed | Skipped |
|------------|--------|--------|---------|
| Video Playback | 15 | 1 | 2 |
| Background Recording | 8 | 0 | 0 |
| Permissions | 10 | 0 | 0 |
| Performance | 5 | 1 | 0 |
| Crash Recovery | 7 | 0 | 0 |
| **TOTAL** | **45** | **2** | **2** |

## Issues Found

1. Video stuttering on multi-camera view (P1)
2. High memory usage with 4 cameras (P2)

## Metrics

- Baseline Memory: 180 MB
- 4 Cameras Memory: 1.8 GB
- 1 Hour Battery Drain: 12%
- Average FPS (1080p): 24

## Recommendations

1. Optimize memory usage for multi-camera view
2. Implement adaptive bitrate for low-end devices
```

---

## 🚀 Execution Plan

### Week 1: Setup и Basic Tests

**Day 1-2:** Setup
- Подготовить тестовые устройства
- Установить Firebase Crashlytics
- Настроить test automation

**Day 3-5:** Video Playback Tests
- Basic playback
- Multi-camera
- Orientation handling
- PiP mode

### Week 2: Advanced Tests

**Day 6-8:** Background Recording
- Background recording start
- Lifecycle management
- Notification persistence

**Day 9-10:** Permissions
- All permission tests
- Revocation recovery

### Week 3: Performance и Finalization

**Day 11-13:** Performance
- Memory profiling
- CPU profiling
- Battery testing
- Network usage

**Day 14-15:** Crash Testing
- OOM handling
- Network loss recovery
- Low storage handling

**Day 16-17:** Analysis и Reporting
- Анализ результатов
- Создание отчёта
- Recommendations

---

## 📁 Deliverables

1. **Test Reports**
   - Per-device test results
   - Issue tracking
   - Performance metrics

2. **Test Automation Scripts**
   - PowerShell scripts
   - ADB command sequences
   - Log parsing scripts

3. **Documentation**
   - Testing guidelines
   - Known issues
   - Recommendations

---

**Plan created:** 2026-04-27  
**Status:** Ready for execution  
**Estimated time:** 2-3 weeks
