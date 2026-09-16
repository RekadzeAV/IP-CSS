# Production Deployment Checklist

Чеклист для развертывания RTSP клиента в production.

---

## 📋 Содержание

1. [Подготовка](#1-подготовка)
2. [Сборка библиотек](#2-сборка-библиотек)
3. [Тестирование](#3-тестирование)
4. [Деплой](#4-деплой)
5. [Мониторинг](#5-мониторинг)
6. [Резервное копирование](#6-резервное-копирование)

---

## 1. Подготовка

### 1.1 Проверка зависимостей

- [ ] FFmpeg 6.0+ установлен на всех целевых платформах
- [ ] Java 17+ настроена для JNI биндингов
- [ ] CMake 3.20+ доступен
- [ ] Visual Studio 2022 (для Windows)
- [ ] Xcode 14+ (для macOS)

### 1.2 Проверка конфигурации

- [ ] RTSP URL камер проверены
- [ ] Аутентификация настроена
- [ ] Порты 554 доступны в сети
- [ ] Брандмауэр разрешает RTSP трафик

### 1.3 Подготовка окружения

```bash
# Проверка версий
cmake --version
ffmpeg -version
java -version

# Проверка доступности камер
ping 192.168.1.100
telnet 192.168.1.100 554
```

---

## 2. Сборка библиотек

### 2.1 Windows

```powershell
# Переход в каталог
cd native/video-processing

# Очистка предыдущей сборки
.\build-windows.ps1 -Clean

# Сборка Release
.\build-windows.ps1 Release

# Проверка результата
Test-Path "lib\windows\x64\video_processing.dll"

# Проверка экспортируемых символов
dumpbin /exports lib\windows\x64\video_processing.dll | findstr rtsp_client
```

**Результаты:**
- [ ] `video_processing.dll` создана
- [ ] Размер ~2.5 MB
- [ ] Символы RTSP экспортированы
- [ ] Нет предупреждений при сборке

### 2.2 Linux

```bash
# Через Docker
cd native/video-processing
docker build -f Dockerfile.linux -t rtsp-builder .
docker create --name rtsp rtsp-builder
docker cp rtsp:/build/build/lib/linux/x64/libvideo_processing.so ./lib/linux/x64/
docker rm rtsp

# Или вручную (если FFmpeg установлен)
mkdir build && cd build
cmake .. -DCMAKE_BUILD_TYPE=Release
cmake --build .

# Проверка
nm -D lib/linux/x64/libvideo_processing.so | grep rtsp_client
```

**Результаты:**
- [ ] `libvideo_processing.so` создана
- [ ] Размер ~3.2 MB
- [ ] Символы RTSP экспортированы
- [ ] Нет ошибок при сборке

### 2.3 macOS

```bash
# Сборка
cd native/video-processing
./build-macos.sh Release arm64

# Проверка
otool -L lib/macos/libvideo_processing.dylib
nm -g lib/macos/libvideo_processing.dylib | grep rtsp_client
```

**Результаты:**
- [ ] `libvideo_processing.dylib` создана
- [ ] Размер ~3 MB
- [ ] Правильные зависимости
- [ ] Символы RTSP экспортированы

---

## 3. Тестирование

### 3.1 Unit-тесты

```bash
# Запуск всех тестов
./gradlew :core:network:desktopTest

# Проверка результатов
# Все тесты должны пройти
```

**Результаты:**
- [ ] Все unit-тесты пройдены
- [ ] Нет предупреждений
- [ ] Покрытие кода > 80%

### 3.2 Интеграционные тесты

```bash
# С локальным RTSP сервером
.\test-rtsp-integration.ps1

# С реальными камерами
$env:TEST_RTSP_URL="rtsp://admin:pass@192.168.1.100:554/stream"
.\test-rtsp-integration.ps1
```

**Результаты:**
- [ ] Подключение работает
- [ ] Воспроизведение работает
- [ ] Получение кадров работает
- [ ] Переподключение работает

### 3.3 Тестирование с реальными камерами

```powershell
# Проверка подключения
.\test-rtsp-connection.ps1 -Url "rtsp://camera/stream"

# Для каждой камеры в системе
.\test-rtsp-connection.ps1 -Url "rtsp://192.168.1.101:554/stream"
.\test-rtsp-connection.ps1 -Url "rtsp://192.168.1.102:554/stream"
.\test-rtsp-connection.ps1 -Url "rtsp://192.168.1.103:554/stream"
```

**Результаты:**
- [ ] Все камеры доступны
- [ ] Кодеки поддерживаются (H.264/H.265)
- [ ] Задержка < 300ms
- [ ] Нет пропущенных кадров

### 3.4 Долгосрочные тесты

```kotlin
// Запуск на 24 часа
suspend fun main() {
    val client = RtspClient(RtspClientConfig(
        url = "rtsp://camera/stream",
        reconnectEnabled = true
    ))
    
    client.connect()
    client.play()
    
    var frameCount = 0L
    client.getVideoFrames().collect {
        frameCount++
        if (frameCount % 1000 == 0) {
            println("Получено $frameCount кадров")
        }
    }
}
```

**Результаты:**
- [ ] Стабильная работа 24 часа
- [ ] Нет утечек памяти
- [ ] Нет падений
- [ ] Переподключение работает

---

## 4. Деплой

### 4.1 Подготовка артефактов

```bash
# Создание каталога релиза
mkdir release-2026-05-24

# Копирование библиотек
cp native/video-processing/lib/windows/x64/video_processing.dll release-2026-05-24/
cp native/video-processing/lib/linux/x64/libvideo_processing.so release-2026-05-24/
cp native/video-processing/lib/macos/libvideo_processing.dylib release-2026-05-24/

# Создание архива
cd release-2026-05-24
zip -r rtsp-client-1.0.0.zip *.dll *.so *.dylib
```

### 4.2 Развертывание на серверах

#### Windows Server

```powershell
# Копирование библиотеки
Copy-Item video_processing.dll "C:\Program Files\IP-CSS\lib\"

# Регистрация в PATH
[System.Environment]::SetEnvironmentVariable(
    "PATH", 
    "$env:Path;C:\Program Files\IP-CSS\lib", 
    "Machine"
)

# Перезапуск сервиса
Restart-Service "IP-CSS-Service"
```

#### Linux Server

```bash
# Копирование библиотеки
sudo cp libvideo_processing.so /opt/ip-css/lib/

# Настройка LD_LIBRARY_PATH
echo "export LD_LIBRARY_PATH=/opt/ip-css/lib:\$LD_LIBRARY_PATH" | \
  sudo tee /etc/profile.d/ip-css.sh

# Перезапуск службы
sudo systemctl restart ip-css
```

#### macOS

```bash
# Копирование библиотеки
sudo cp libvideo_processing.dylib /opt/ip-css/lib/

# Настройка DYLD_LIBRARY_PATH
echo "export DYLD_LIBRARY_PATH=/opt/ip-css/lib:\$DYLD_LIBRARY_PATH" | \
  tee ~/.zshrc

# Перезапуск службы
sudo launchctl unload /Library/LaunchDaemons/com.ipcss.service.plist
sudo launchctl load /Library/LaunchDaemons/com.ipcss.service.plist
```

### 4.3 Проверка деплоя

```bash
# Проверка загрузки библиотеки
ldd /path/to/app | grep video_processing  # Linux
dumpbin /dependents app.exe | grep video_processing  # Windows
otool -L app  # macOS

# Проверка работы
./app --test-rtsp-connection
```

---

## 5. Мониторинг

### 5.1 Логирование

```kotlin
// Включить подробное логирование
val config = RtspClientConfig(
    url = "rtsp://camera/stream",
    enableVideo = true,
    logLevel = RtspClientLogLevel.DEBUG
)

// Мониторинг статусов
client.getStatus().collect { status ->
    logger.info { "RTSP статус: $status" }
}

// Мониторинг кадров
client.getVideoFrames().collect { frame ->
    // Подсчет кадров
    frameCount++
}
```

### 5.2 Метрики

- [ ] FPS стабильный (25-30 кадров/сек)
- [ ] Задержка < 300ms
- [ ] Количество реконнектов < 5 в час
- [ ] Использование памяти < 500MB
- [ ] Использование CPU < 20%

### 5.3 Оповещения

```kotlin
// Настройка оповещений
client.getStatus().collect { status ->
    when (status) {
        RtspClientStatus.ERROR -> {
            sendAlert("RTSP Error: ${client.getRuntimeDiagnostics().value.lastError}")
        }
        RtspClientStatus.DISCONNECTED -> {
            sendAlert("RTSP Disconnected")
        }
        RtspClientStatus.RECONNECTING -> {
            logger.warn { "RTSP Reconnecting..." }
        }
    }
}
```

---

## 6. Резервное копирование

### 6.1 Конфигурация

```json
{
  "rtsp": {
    "cameras": [
      {
        "id": "entrance",
        "url": "rtsp://192.168.1.101:554/stream",
        "username": "admin",
        "password": "encrypted_password"
      }
    ],
    "reconnect": {
      "enabled": true,
      "maxRetries": 5,
      "backoffMultiplier": 2.0
    }
  }
}
```

- [ ] Конфигурация сохранена
- [ ] Пароли зашифрованы
- [ ] Резервная копия создана

### 6.2 Библиотеки

- [ ] Архив с библиотеками сохранен
- [ ] Хеш-суммы проверены
- [ ] Документация сохранена

---

## 📊 Статус проверки

| Шаг | Статус | Примечание |
|-----|--------|------------|
| 1. Подготовка | ⏳ | |
| 2. Сборка библиотек | ⏳ | |
| 3. Тестирование | ⏳ | |
| 4. Деплой | ⏳ | |
| 5. Мониторинг | ⏳ | |
| 6. Резервное копирование | ⏳ | |

---

**Версия:** 1.0  
**Дата:** 24 мая 2026  
**Ответственный:** ___________________
