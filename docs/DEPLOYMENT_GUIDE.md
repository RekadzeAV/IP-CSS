# Руководство по развертыванию

## Оглавление
1. [Требования к системе](#требования-к-системе)
2. [Быстрый старт](#быстрый-старт)
3. [Развертывание на различных платформах](#развертывание-на-различных-платформах)
4. [Конфигурация](#конфигурация)
5. [Обновление системы](#обновление-системы)
6. [Резервное копирование](#резервное-копирование)

## Требования к системе

### Минимальные требования

#### Для мобильных устройств:
- **Android**: Android 8.0+ (API 26+), 2 ГБ RAM, 16 ГБ памяти
- **iOS**: iOS 14.0+, iPhone 8+/iPad 5+, 2 ГБ RAM, 16 ГБ памяти

#### Для десктоп систем:
- **Windows**: Windows 10/11 64-bit, 4 ГБ RAM, 20 ГБ свободного места
- **Linux**: Ubuntu 20.04+/Debian 11+, 4 ГБ RAM, 20 ГБ свободного места
- **macOS**: macOS 11.0+ (Big Sur), 8 ГБ RAM, 20 ГБ свободного места

#### Для NAS устройств:
- **Минимальные**: Intel Celeron J4125/ARM Cortex-A72, 4 ГБ RAM, 50 ГБ + место под записи
- **Рекомендуемые**: Intel i5 с Quick Sync, 8 ГБ RAM, SSD кэш + HDD массив

## Быстрый старт

### Установка за 5 минут (Docker)

```bash
# 1. Скачайте docker-compose.yml
wget https://company.com/docker-compose.yml

# 2. Настройте переменные окружения
cp .env.example .env
nano .env  # Отредактируйте настройки

# 3. Запустите систему
docker-compose up -d

# 4. Откройте веб-интерфейс
# http://localhost:8080
# Логин: admin
# Пароль: admin (смените при первом входе)
```

## Развертывание на различных платформах

### Android

#### Способ 1: Google Play (рекомендуется)
1. Перейдите в Google Play Store
2. Найдите "IP Camera Surveillance Pro"
3. Установите приложение
4. Запустите и следуйте инструкциям мастера настройки

#### Способ 2: Прямая установка APK
```bash
adb install app-release.apk
```

### iOS

#### Способ 1: App Store
1. Откройте App Store
2. Найдите "IP Camera Surveillance Pro"
3. Установите приложение

#### Способ 2: TestFlight (бета-тестирование)
1. Установите TestFlight
2. Примите приглашение
3. Установите бета-версию

### Windows

#### Способ 1: Microsoft Store
1. Откройте Microsoft Store
2. Найдите "IP Camera Surveillance"
3. Установите

#### Способ 2: Установщик MSI/EXE
```powershell
.\ip-camera-surveillance-setup.exe
```

### Linux

#### Способ 1: Пакеты DEB/RPM
```bash
# Ubuntu/Debian
sudo apt update
sudo apt install ip-camera-surveillance

# RHEL/Fedora
sudo dnf install ip-camera-surveillance
```

#### Способ 2: Snap
```bash
sudo snap install ip-camera-surveillance
```

### macOS

#### Способ 1: Mac App Store
1. Откройте App Store
2. Найдите "IP Camera Surveillance"
3. Установите

#### Способ 2: Homebrew
```bash
brew install --cask ip-camera-surveillance
```

### NAS устройства

#### Synology DSM
1. Откройте Панель управления
2. Перейдите в Пакеты → Установка вручную
3. Выберите скачанный .spk файл
4. Следуйте инструкциям

#### QNAP QTS
1. Откройте App Center
2. Нажмите "Установка из файла"
3. Выберите .qpkg файл

## Конфигурация

### Базовые настройки

Создайте файл `config.yaml`:

```yaml
server:
  port: 8080
  host: 0.0.0.0

database:
  url: jdbc:sqlite:/app/database/surveillance.db
  driver: org.sqlite.JDBC

storage:
  recordings: /app/recordings
  max_days: 30

license:
  key: your_license_key_here
  offline_days: 30

features:
  analytics: true
  anpr: false
  notifications: true
```

### Настройка камер

1. Откройте веб-интерфейс
2. Перейдите в "Камеры" → "Добавить камеру"
3. Введите RTSP URL или используйте автообнаружение
4. Настройте параметры записи и аналитики

## Обновление системы

### Автоматическое обновление

Система поддерживает автоматические обновления через:
- Google Play (Android)
- App Store (iOS)
- Microsoft Store (Windows)
- Пакетные менеджеры (Linux)

### Ручное обновление

```bash
# Docker
docker-compose pull
docker-compose up -d

# Linux
sudo apt update
sudo apt upgrade ip-camera-surveillance
```

## Резервное копирование

### Автоматическое резервное копирование

Настройте cron задачу:

```bash
# Ежедневное резервное копирование в 2:00
0 2 * * * /usr/local/bin/backup.sh
```

### Ручное резервное копирование

```bash
# Создать резервную копию
./scripts/backup.sh

# Восстановить из резервной копии
./scripts/restore.sh /path/to/backup.tar.gz
```

## Дополнительная информация

Для более подробной информации см.:
- [ARCHITECTURE.md](ARCHITECTURE.md) - Архитектура системы
- [PLATFORMS.md](PLATFORMS.md) - Разделение разработки по платформам
- [API.md](API.md) - API документация
- [LICENSE_SYSTEM.md](LICENSE_SYSTEM.md) - Система лицензирования

