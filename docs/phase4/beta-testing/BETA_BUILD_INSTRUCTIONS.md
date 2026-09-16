# IP-CSS Beta Build Instructions

**Версия:** 1.0.0-beta  
**Дата:** 28 January 2026  
**Статус:** ✅ **READY FOR BETA TESTING**

---

## 🎯 Overview

Этот документ содержит полные инструкции по сборке и развёртыванию IP-CSS v1.0.0-beta на трёх платформах:

- 🥧 **Raspberry Pi** (ARM64)
- 🐳 **Docker** (linux/amd64, linux/arm64)
- 🪟 **Windows** (x64)

---

## 📋 Prerequisites

### Общие требования

| Компонент | Версия | Примечание |
|-----------|--------|------------|
| Git | 2.30+ | Для клонирования репозитория |
| Java | 17+ | OpenJDK или Oracle JDK |
| Gradle | 8.0+ | Для сборки JAR |

### Платформенные требования

#### Raspberry Pi
- Raspberry Pi 4/400/5 (4GB+ RAM)
- Raspberry Pi OS 64-bit (Bullseye+)
- Docker (опционально)

#### Docker
- Docker 20.10+
- Docker Compose 2.0+
- 4GB+ RAM, 2+ CPU

#### Windows
- Windows 10/11 или Server 2019+
- PowerShell 7.0+
- 4GB+ RAM (8GB recommended)

---

## 🚀 Quick Start

### 1. Клонирование репозитория

```bash
git clone https://github.com/nlp-core-team/ip-css.git
cd ip-css
```

### 2. Автоматическая сборка (все платформы)

```powershell
.\scripts\build-beta-release.ps1 -Platform all -Version "1.0.0-beta"
```

### 3. Проверка результатов

```powershell
ls release-builds/beta/
```

---

## 🥧 Raspberry Pi Build

### Вариант A: Автоматическая сборка

```powershell
.\scripts\build-beta-release.ps1 -Platform raspberry -Version "1.0.0-beta"
```

**Результат:** `release-builds/beta/raspberry/`

### Вариант B: Ручная сборка

#### 1. Сборка JAR

```bash
cd /path/to/ip-css
gradle :server:api:build -x test -Pversion=1.0.0-beta
```

#### 2. Копирование файлов

```bash
mkdir -p /opt/ip-css
cp server/api/build/libs/ip-css-server-1.0.0-beta.jar /opt/ip-css/
cp scripts/raspberry/* /opt/ip-css/
```

#### 3. Установка

```bash
cd /opt/ip-css
sudo chmod +x install.sh
sudo ./install.sh
```

#### 4. Конфигурация

```bash
sudo nano /opt/ip-css/application.yml
```

#### 5. Запуск

```bash
# Как сервис
sudo systemctl start ip-css
sudo systemctl enable ip-css

# Или напрямую
sudo /opt/ip-css/start-ip-css.sh
```

### Проверка

```bash
# Статус сервиса
systemctl status ip-css

# Логи
journalctl -u ip-css -f

# Проверка API
curl http://localhost:8080/api/health
```

### Доступ

- **Web UI:** http://<raspberry-ip>:8080
- **Credentials:** admin / admin

### Производительность

| Модель | Камеры | RAM Usage | Notes |
|--------|--------|-----------|-------|
| Pi 4 (4GB) | 4-6 | ~400MB | 1080p |
| Pi 4 (8GB) | 6-8 | ~600MB | 1080p |
| Pi 5 (4GB) | 6-8 | ~400MB | 1080p |
| Pi 5 (8GB) | 8-12 | ~600MB | 1080p |

---

## 🐳 Docker Build

### Вариант A: Автоматическая сборка

```powershell
.\scripts\build-beta-release.ps1 -Platform docker -Version "1.0.0-beta"
```

**Результат:** `release-builds/beta/docker/`

### Вариант B: Ручная сборка

#### 1. Сборка образа

```bash
cd release-builds/beta/docker

# Сборка для текущей платформы
docker build -t ghcr.io/nlp-core-team/ip-css:1.0.0-beta .

# Мульти-платформа (требует buildx)
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t ghcr.io/nlp-core-team/ip-css:1.0.0-beta \
  --push \
  .
```

#### 2. Запуск с Docker Compose (Рекомендуется)

```bash
cd release-builds/beta

# Базовый запуск
docker-compose -f docker-compose.beta.yml up -d

# С мониторингом
docker-compose -f docker-compose.beta.yml --profile monitoring up -d

# С Nginx
docker-compose -f docker-compose.beta.yml --profile with-nginx up -d

# Всё включено
docker-compose -f docker-compose.beta.yml --profile monitoring --profile with-nginx up -d
```

#### 3. Запуск с Docker Run

```bash
docker run -d \
  --name ip-css \
  -p 8080:8080 \
  -p 8443:8443 \
  -v ./data:/data \
  -v ./config:/config \
  -e POSTGRES_HOST=postgres \
  -e POSTGRES_PASSWORD=changeme \
  ghcr.io/nlp-core-team/ip-css:1.0.0-beta
```

### Проверка

```bash
# Статус контейнеров
docker-compose -f docker-compose.beta.yml ps

# Логи
docker-compose -f docker-compose.beta.yml logs -f ip-css

# Проверка здоровья
docker inspect --format='{{.State.Health.Status}}' ip-css-beta
```

### Доступ

- **Web UI:** http://localhost:8080
- **API:** http://localhost:8080/api
- **Credentials:** admin / admin

### Производительность

| Resources | Камеры | Notes |
|-----------|--------|-------|
| 2 CPU, 4GB RAM | 8-12 | 1080p |
| 4 CPU, 8GB RAM | 16-24 | 1080p |
| 8 CPU, 16GB RAM | 32-48 | 1080p |

---

## 🪟 Windows Build

### Вариант A: Автоматическая сборка

```powershell
.\scripts\build-beta-release.ps1 -Platform windows -Version "1.0.0-beta"
```

**Результат:** `release-builds/beta/windows/`

### Вариант B: Ручная сборка

#### 1. Установка PostgreSQL

```powershell
# Скачайте и установите PostgreSQL 14+
# https://www.postgresql.org/download/windows/

# Создайте базу данных
psql -U postgres
CREATE DATABASE ipcss;
CREATE USER ipcss WITH PASSWORD 'changeme';
GRANT ALL PRIVILEGES ON DATABASE ipcss TO ipcss;
```

#### 2. Установка Java

```powershell
# Скачайте и установите Java 17
# https://adoptium.net/

# Проверка
java -version
```

#### 3. Подготовка directories

```powershell
mkdir "C:\Program Files\IP-CSS"
cd "C:\Program Files\IP-CSS"

# Копирование файлов
Copy-Item -Path "release-builds\beta\windows\*" -Destination . -Recurse
```

#### 4. Конфигурация

Откройте `config\application.yml` и обновите:

```yaml
database:
  url: jdbc:postgresql://localhost:5432/ipcss
  username: ipcss
  password: changeme
```

#### 5. Запуск

**Вариант 1: Через BAT файл**
```cmd
start-ip-css.bat
```

**Вариант 2: Через PowerShell**
```powershell
.\start-ip-css.ps1
```

**Вариант 3: Как Windows Service**
```cmd
# От имени администратора
install-service.bat
```

### Проверка

```powershell
# Проверка процесса
Get-Process -Name "java" | Where-Object { $_.CommandLine -like "*ip-css*" }

# Проверка API
Invoke-WebRequest http://localhost:8080/api/health

# Логи
Get-Content .\logs\ip-css.log -Tail 50 -Wait
```

### Доступ

- **Web UI:** http://localhost:8080
- **Credentials:** admin / admin

### Производительность

| Resources | Камеры | Notes |
|-----------|--------|-------|
| 4 CPU, 8GB RAM | 12-16 | 1080p |
| 8 CPU, 16GB RAM | 24-32 | 1080p |
| 16 CPU, 32GB RAM | 48-64 | 1080p |

---

## 🔧 Troubleshooting

### Общие проблемы

#### Java не найдена

**Ошибка:**
```
ERROR: Java not found
```

**Решение:**
```bash
# Установите Java 17
# https://adoptium.net/

# Или укажите JAVA_HOME
export JAVA_HOME=/path/to/java
```

#### Port уже занят

**Ошибка:**
```
Address already in use
```

**Решение:**
```yaml
# Измените порт в application.yml
server:
  port: 8081  # Вместо 8080
```

#### Database connection failed

**Ошибка:**
```
Connection refused
```

**Решение:**
1. Проверьте, что PostgreSQL запущен
2. Проверьте credentials в application.yml
3. Проверьте firewall

### Raspberry Pi специфичные

#### Недостаточно памяти

**Решение:**
```bash
# Уменьшите JVM heap
JVM_OPTS="-Xmx512m -Xms256m"
```

#### Перегрев

**Решение:**
```bash
# Проверка температуры
vcgencmd measure_temp

# Установите активное охлаждение
# Undervolt CPU (опционально)
```

### Docker специфичные

#### Container не запускается

**Решение:**
```bash
# Проверка логов
docker logs ip-css-beta

# Пересоздание
docker-compose -f docker-compose.beta.yml down
docker-compose -f docker-compose.beta.yml up -d
```

### Windows специфичные

#### Service не устанавливается

**Решение:**
```cmd
# Запустите от имени администратора
# Проверка прав
net session
```

#### Firewall блокирует

**Решение:**
```powershell
# Добавьте правило firewall
New-NetFirewallRule -DisplayName "IP-CSS" -Direction Inbound -LocalPort 8080 -Protocol TCP -Action Allow
```

---

## 📊 Beta Testing Checklist

### Перед началом

- [ ] ✅ Прочитана документация
- [ ] ✅ Подготовлена среда
- [ ] ✅ Резервная копия данных (если применимо)

### После установки

- [ ] ✅ Сервис запущен
- [ ] ✅ Web UI доступен
- [ ] ✅ API health check проходит
- [ ] ✅ База данных подключена

### Функциональное тестирование

- [ ] ✅ Регистрация/Вход
- [ ] ✅ Добавление камеры
- [ ] ✅ Просмотр live stream
- [ ] ✅ Запись видео
- [ ] ✅ Воспроизведение записи
- [ ] ✅ Детекция движения
- [ ] ✅ Уведомления

### Нагрузочное тестирование

- [ ] ✅ 4 камеры одновременно
- [ ] ✅ 8 камер одновременно
- [ ] ✅ 24 часа непрерывной работы
- [ ] ✅ Проверка стабильности

### Отчётность

- [ ] ✅ Заполнен feedback form
- [ ] ✅ Отправлены логи (если проблемы)
- [ ] ✅ Отправлен отчёт о багах

---

## 📞 Support

### Beta Test Support

- **Email:** beta@ip-css.com
- **GitHub Issues:** https://github.com/nlp-core-team/ip-css/issues
- **Documentation:** https://docs.ip-css.com

### Bug Reporting

При сообщении об ошибках укажите:

1. **Платформа:** Raspberry Pi / Docker / Windows
2. **Версия:** 1.0.0-beta
3. **Шаги воспроизведения**
4. **Ожидаемое поведение**
5. **Фактическое поведение**
6. **Логи** (приложите файлы)

---

## 📈 Performance Tuning

### Raspberry Pi

```yaml
# application.yml
performance:
  max_threads: 4
  stream_buffer_size: 2MB
  hardware_acceleration: true  # Если доступен
  jvm_opts: "-Xmx512m -Xms256m"
```

### Docker

```yaml
# docker-compose.yml
services:
  ip-css:
    deploy:
      resources:
        limits:
          cpus: '4'
          memory: 4G
```

### Windows

```powershell
# Оптимизация JVM
$JVM_OPTS = @(
    "-Xmx1024m",
    "-Xms512m",
    "-XX:+UseG1GC",
    "-XX:MaxGCPauseMillis=200"
)
```

---

## 📝 Version Information

| Component | Version |
|-----------|---------|
| **IP-CSS** | 1.0.0-beta |
| **API** | v1 |
| **Database Schema** | v3 |
| **Minimum Java** | 17 |
| **Minimum PostgreSQL** | 14 |

---

## ✅ Build Verification

### Checksums

Проверка целостности файлов:

```bash
# Raspberry Pi
sha256sum release-builds/beta/raspberry/ip-css-server.jar

# Docker
sha256sum release-builds/beta/docker/ip-css-1.0.0-beta.tar.gz

# Windows
Get-FileHash release-builds/beta/windows/bin/ip-css-server.jar -Algorithm SHA256
```

Сравните с `release-builds/beta/checksums/SHA256SUMS.txt`

---

**Document Version:** 1.0  
**Last Updated:** 28 January 2026  
**Status:** ✅ **READY FOR BETA TESTING**

---

# BETA BUILD INSTRUCTIONS COMPLETE

**IP-CSS v1.0.0-beta is ready for deployment on Raspberry Pi, Docker, and Windows.**
