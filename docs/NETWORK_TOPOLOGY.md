# Сетевое Окружение: Детальная Информация

**Дата:** 2026-06-09  
**Версия:** 1.0  
**Статус:** Актуально

---

## 📊 Общая Информация

### Сеть:

| Параметр | Значение |
|----------|----------|
| **Подсеть** | 192.168.10.0/24 |
| **Шлюз** | 192.168.10.1 |
| **Маска** | 255.255.255.0 |
| **Диапазон** | 192.168.10.1 - 192.168.10.254 |
| **Доступные IP** | 253 адреса |

### Размещение Устройств:

| Устройство | IP Диапазон | Назначение |
|-----------|-------------|------------|
| Шлюз/Роутер | 192.168.10.1 | Маршрутизация |
| Dev Machine | 192.168.10.25 | Разработка |
| Камеры | 192.168.10.17, 192.168.10.20-24, 192.168.10.26 | Видеонаблюдение |
| NVR Регистратор | 192.168.10.250 | Второй контур (запись) |
| Raspberry Pi | 192.168.10.45-46 | Edge вычисления |
| NAS | 192.168.10.37-40 | Хранилище | Первый контур (запись) |
topology
---

## 📹 Камеры видеонаблюдения

### Конфигурация:

| ID | Имя | IP | RTSP URL | Порт | Пользователь | Статус |
|----|-----|-----|----------|------|--------------|--------|
| camera-1 | Living Room | 192.168.10.100 | rtsp://192.168.10.100:554/stream1 | 554 | admin | ⚠️ Не подтверждено |
| camera-2 | Garden | 192.168.10.101 | rtsp://192.168.10.101:554/live.sdp | 554 | admin | ⚠️ Не подтверждено |
| camera-3 | Front Door | 192.168.10.102 | rtsp://192.168.10.102:554/cam/real | 554 | admin | ⚠️ Не подтверждено |
| camera-4 | Garage | 192.168.10.103 | rtsp://192.168.10.103:554/h264Preview_01_sub | 554 | admin | ⚠️ Не подтверждено |
| camera-5 | Backyard | 192.168.10.104 | rtsp://192.168.10.104:554/stream/ch0 | 554 | admin | ⚠️ Не подтверждено |

### Технические Характеристики:

| Параметр | Значение |
|----------|----------|
| **Протокол** | RTSP/ONVIF |
| **Кодек видео** | H.264/H.265 |
| **Кодек аудио** | AAC/PCMU/PCMA |
| **Разрешение** | 1920x1080 (1080p) |
| **FPS** | 25-30 |
| **Битрейт** | 2-4 Mbps |

### Требует Проверки:

- [ ] Физическое наличие камер
- [ ] Корректность IP-адресов
- [ ] Работоспособность RTSP потоков
- [ ] Настройка ONVIF (если применимо)
- [ ] Обновление паролей (сейчас тестовые)

---

## 📼 NVR Регистратор (Второй Контур)

### Конфигурация:

| Параметр | Значение | Статус |
|----------|----------|--------|
| **IP Адрес** | 192.168.10.250 | ✅ Назначен |
| **Тип** | NVR (Network Video Recorder) | ✅ |
| **Логин** | `Kassian` | ✅ |
| **Пароль** | `123456@wSx` | ✅ |
| **Назначение** | Второй контур видеонаблюдения | ✅ Активен |

### Функции:

1. **Запись с камер:**
   - Непрерывная запись
   - Запись по движению
   - Запись по событиям

2. **Управление камерами:**
   - Поддержка ONVIF камер
   - Автообнаружение камер в сети
   - Конфигурация через веб-интерфейс

3. **Хранение данных:**
   - Встроенные HDD
   - Возможность расширения
   - Архивация записей

### Интеграция с IP-CSS:

| Возможность | Статус | Примечание |
|------------|--------|------------|
| ONVIF подписка | ⏸️ Требуется проверка | ONVIF события |
| RTSP потоки | ⏸️ Требуется проверка | Прямой доступ |
| Удалённый просмотр | ⏸️ Требуется проверка | RTSP/HTTP |

### Подключение к IP-CSS:

```
IP-CSS API (192.168.10.25)
         |
         | ONVIF / RTSP
         |
   NVR 192.168.10.250
         |
    [Камеры контура]
```

### Требует Настройки:

- [ ] Проверка ONVIF доступности
- [ ] Добавление в IP-CSS систему
- [ ] Настройка подписки на события
- [ ] Тестирование RTSP потоков
- [ ] Настройка интеграции с NAS

---

## 🍓 Raspberry Pi 4

### Конфигурация:

| Параметр | Значение | Статус |
|----------|----------|--------|
| **Модель** | Raspberry Pi 4 | ✅ |
| **IP Адреса** | 192.168.10.45, 192.168.10.46 | ✅ Назначены |
| **OS** | Raspberry Pi OS | ⏸️ Не установлена |
| **Память** | 4GB/8GB RAM | ✅ |
| **Назначение** | Edge устройство | ⏸️ Не настроено |

### Планируемая Роли:

1. **RTSP Proxy:**
   - Перенаправление RTSP потоков
   - Транскодирование при необходимости
   - Буферизация для стабильности

2. **Local Recording:**
   - Локальная запись при потере сети
   - Кэширование событий

3. **AI Inference (опционально):**
   - Motion detection
   - Object detection
   - Анализ видео

### Требует Настройки:

- [ ] Установка Raspberry Pi OS
- [ ] Назначение статического IP
- [ ] Настройка SSH доступа
- [ ] Установка RTSP прокси (например, ffmpeg, mediamtx)
- [ ] Интеграция с NAS
- [ ] Настройка автозапуска

---

## 💾 NAS (Network Attached Storage)

### Конфигурация:

| Параметр | Значение | Статус |
|----------|----------|--------|
| **IP Адреса** | 192.168.10.37, 192.168.10.38, 192.168.10.39, 192.168.10.40 | ✅ Назначены |
| **Кластер** | 4 узла (High Availability) | ✅ |
| **Модель** | Не определено | ⏸️ |
| **Ёмкость** | Не определено | ⏸️ |
| **Протокол** | SMB/NFS | ⏸️ Не настроено |
| **Назначение** | **Первый контур видеонаблюдения + LDAP сервер** | ✅ Активен |

### Основные Функции:

1. **Видеонаблюдение (Первый Контур):**
   - Хранение записей с камер 192.168.10.17, 20-24, 26
   - Архивация видео (непрерывная запись)
   - Управление доступом к записям

2. **LDAP Directory Service:**
   - Центральная аутентификация пользователей
   - Управление учётными записями
   - Интеграция с IP-CSS API
   - Группа пользователей: `surveillance_users`

3. **Файловое Хранилище:**
   - SMB/NFS шары для доступа к файлам
   - Резервное копирование данных
   - Синхронизация между узлами

### LDAP Конфигурация:

| Параметр | Значение |
|----------|----------|
| **Base DN** | `dc=surveillance,dc=local` |
| **Bind DN** | `cn=admin,dc=surveillance,dc=local` |
| **Порт** | 389 (LDAP), 636 (LDAPS) |
| **Пользователи** | `ou=users,dc=surveillance,dc=local` |
| **Группы** | `ou=groups,dc=surveillance,dc=local` |

### Структура Данных:

```
/NAS/
├── recordings/
│   ├── first-contour/
│   │   ├── camera-17/
│   │   │   ├── 2026-06-09/
│   │   │   └── ...
│   │   ├── camera-20/
│   │   ├── camera-21/
│   │   ├── camera-22/
│   │   ├── camera-23/
│   │   ├── camera-24/
│   │   └── camera-26/
│   └── archive/
├── ldap/
│   ├── data.db
│   ├── log
│   └── backups/
├── events/
│   ├── motion/
│   ├── object-detection/
│   └── ...
├── snapshots/
│   └── ...
└── backups/
    └── postgres/
```

### LDAP Учётные Данные:

| Поле | Значение | Примечание |
|------|----------|------------|
| **Admin DN** | `cn=admin,dc=surveillance,dc=local` | Администратор LDAP |
| **Пароль** | Не раскрывается | ✅ Хранится безопасно |
| **Base DN** | `dc=surveillance,dc=local` | Корневой домен |
| **Порт** | 389 (LDAP) | ⏸️ Требуется проверка |

### Требует Настройки (NAS):

#### 2.1. Настройка NAS Хранилища

**Цель:** Интеграция NAS с IP-CSS системой

**Шаги:**

1. **Определить модель и возможности NAS:**
   ```bash
   # Подключиться к веб-интерфейсу
   curl http://192.168.10.37
   
   # Проверить доступность
   ping 192.168.10.37
   ```

2. **Проверить LDAP доступность:**
   ```bash
   # Тест подключения к LDAP
   ldapsearch -x -H ldap://192.168.10.37 -b "dc=surveillance,dc=local"
   ```

3. **Создать SMB/NFS шары для записей:**
   ```bash
   # На SMB (Windows)
   New-SmbShare -Name "recordings" -Path "/storage/recordings" -FullAccess "surveillance_users"
   
   # На NFS (Linux)
   echo "/storage/recordings 192.168.10.0/24(rw,sync,no_subtree_check)" >> /etc/exports
   exportfs -ra
   ```

4. **Настроить права доступа:**
   ```bash
   # Создать пользователя для IP-CSS
   ldapadd -x -D "cn=admin,dc=surveillance,dc=local" -W -f ipcss_user.ldif
   
   # Пример LDIF файла:
   # dn: uid=ipcss,ou=users,dc=surveillance,dc=local
   # objectClass: inetOrgPerson
   # uid: ipcss
   # userPassword: {SSHA}encrypted_password
   ```

5. **Интегрировать LDAP с IP-CSS API:**
   ```yaml
   # docker-compose.yml или .env
   AUTH_LDAP_SERVER: "ldap://192.168.10.37:389"
   AUTH_LDAP_BASE_DN: "dc=surveillance,dc=local"
   AUTH_LDAP_BIND_DN: "cn=admin,dc=surveillance,dc=local"
   AUTH_LDAP_BIND_PASSWORD: "your_ldap_password"
   AUTH_LDAP_USER_SEARCH_FILTER: "(uid=%(user)s)"
   ```

6. **Настроить монтирование NAS на сервере IP-CSS:**
   ```bash
   # SMB mount (Windows)
   net use Z: \\192.168.10.37\recordings /user:surveillance\ipcss password
   
   # NFS mount (Linux/Docker)
   mkdir -p /storage/recordings
   mount -t nfs 192.168.10.37:/storage/recordings /storage/recordings
   ```

7. **Настроить автоматическую синхронизацию:**
   ```bash
   # crontab для резервного копирования
   0 2 * * * rsync -avz /storage/recordings/ backup-server:/backup/recordings/
   ```

8. **Проверить доступность записей:**
   ```bash
   # Тест записи
   touch /storage/recordings/test_file
   ls -la /storage/recordings/
   ```

**Результат:**
- ✅ NAS подключён и доступен
- ✅ LDAP аутентификация работает
- ✅ Записи сохраняются на NAS
- ✅ IP-CSS использует LDAP для авторизации

---

## 🍓 Raspberry Pi 4

### Конфигурация:

| Параметр | Значение | Статус |
|----------|----------|--------|
| **Модель** | Raspberry Pi 4 | ✅ |
| **IP Адреса** | 192.168.10.45, 192.168.10.46 | ✅ Назначены |
| **OS** | Raspberry Pi OS | ⏸️ Не установлена |
| **Память** | 4GB/8GB RAM | ✅ |
| **Назначение** | Edge устройство + RTSP прокси | ⏸️ Не настроено |

### Планируемая Роли:

1. **RTSP Proxy:**
   - Перенаправление RTSP потоков
   - Транскодирование при необходимости
   - Буферизация для стабильности

2. **Local Recording:**
   - Локальная запись при потере сети
   - Кэширование событий

3. **AI Inference (опционально):**
   - Motion detection
   - Object detection
   - Анализ видео

#### 2.2. Настройка Raspberry Pi Edge

**Цель:** Подготовка Raspberry Pi как Edge устройства для IP-CSS

**Шаги:**

1. **Установка Raspberry Pi OS:**
   ```bash
   # Скачать образ
   wget https://downloads.raspberrypi.org/raspios_latest
   
   # Записать на SD карту
   sudo dd if=raspios.img of=/dev/sdX bs=4M status=progress
   
   # Включить SSH (создать файл ssh на boot разделе)
   touch /media/boot/ssh
   ```

2. **Первый Запуск и Настройка Сети:**
   ```bash
   # Подключиться по SSH
   ssh pi@192.168.10.45
   
   # Изменить пароль
   passwd
   
   # Настроить статический IP
   sudo nano /etc/dhcpcd.conf
   
   # Добавить:
   interface eth0
   static ip_address=192.168.10.45/24
   static routers=192.168.10.1
   static domain_name_servers=192.168.10.1
   ```

3. **Установка Необходимых Пакетов:**
   ```bash
   # Обновление системы
   sudo apt update && sudo apt upgrade -y
   
   # Установка зависимостей
   sudo apt install -y ffmpeg gstreamer1.0-tools gstreamer1.0-plugins-base
   sudo apt install -y nginx curl git build-essential
   
   # Установка Docker (опционально)
   curl -fsSL https://get.docker.com | sh
   sudo usermod -aG docker pi
   ```

4. **Настройка RTSP Прокси (mediamtx):**
   ```bash
   # Скачать mediamtx
   wget https://github.com/aler9/mediamtx/releases/latest/download/mediamtx_linux_amd64.tar.gz
   tar -xzf mediamtx_linux_amd64.tar.gz
   
   # Настройка конфигурации
   nano mediamtx.yml
   
   # Пример конфигурации:
   # paths:
   #   cam1:
   #     source: rtsp://192.168.10.17:554/stream1
   #     sourceOnDemand: yes
   
   # Запуск
   ./mediamtx &
   ```

5. **Настройка Автозапуска (systemd):**
   ```bash
   # Создать сервис
   sudo nano /etc/systemd/system/mediamtx.service
   
   # Контент:
   [Unit]
   Description=RTSP Proxy Service
   After=network.target
   
   [Service]
   Type=simple
   User=pi
   WorkingDirectory=/home/pi/mediamtx
   ExecStart=/home/pi/mediamtx/mediamtx
   Restart=always
   
   [Install]
   WantedBy=multi-user.target
   
   # Включить автозапуск
   sudo systemctl enable mediamtx
   sudo systemctl start mediamtx
   ```

6. **Настройка Локальной Записи:**
   ```bash
   # Создать директорию для записей
   sudo mkdir -p /var/recordings
   sudo chown pi:pi /var/recordings
   
   # Скрипт записи
   cat > /home/pi/recorder.sh << 'EOF'
   #!/bin/bash
   CAMERA_URL="rtsp://192.168.10.17:554/stream1"
   RECORD_DIR="/var/recordings/$(date +%Y-%m-%d)"
   mkdir -p $RECORD_DIR
   ffmpeg -i $CAMERA_URL -c copy "$RECORD_DIR/rec_%H_%M_%S.mp4"
   EOF
   
   chmod +x /home/pi/recorder.sh
   ```

7. **Интеграция с NAS:**
   ```bash
   # Монтировать NAS для записей
   sudo mkdir -p /mnt/nas-recordings
   sudo mount -t nfs 192.168.10.37:/storage/recordings /mnt/nas-recordings
   
   # Автомонтирование через fstab
   echo "192.168.10.37:/storage/recordings /mnt/nas-recordings nfs defaults 0 0" | sudo tee -a /etc/fstab
   ```

8. **Настройка AI (опционально - OpenCV + TensorFlow):**
   ```bash
   # Установка Python и зависимостей
   sudo apt install -y python3-pip python3-venv
   
   # Создание виртуального окружения
   python3 -m venv /home/pi/venv
   source /home/pi/venv/bin/activate
   
   # Установка библиотек
   pip install opencv-python tensorflow numpy
   
   # Пример скрипта детекции движения
   cat > /home/pi/motion_detect.py << 'EOF'
   import cv2
   # Motion detection logic here
   EOF
   ```

9. **Настройка Мониторинга:**
   ```bash
   # Установка мониторинга (опционально)
   sudo apt install -y htop iotop nethogs
   
   # Настройка лога
   sudo journalctl -u mediamtx -f
   ```

10. **Тестирование Работы:**
    ```bash
    # Проверка RTSP потока
    ffprobe rtsp://192.168.10.45:8554/cam1
    
    # Проверка записи
    ls -lh /var/recordings/
    
    # Проверка нагрузки
    top -b -n 1 | head -20
    ```

**Результат:**
- ✅ Raspberry Pi OS установлена
- ✅ Статический IP назначен
- ✅ RTSP прокси работает
- ✅ Автозапуск настроен
- ✅ Интеграция с NAS выполнена
- ✅ Локальная запись работает
- ✅ Мониторинг настроен

---

## 🌐 Сетевая Топология

```
                    [Internet]
                        |
                [Router/Gateway]
                192.168.10.1
                        |
        ┌───────────────┼───────────────┐
        |               |               |
        |               |               |
 [Dev Machine]    [Cameras]       [Edge Devices]
 192.168.10.25   192.168.10.17,    Raspberry Pi
  (Docker)        20-24, 26         192.168.10.45-46
                        |               |
                        |           [NAS]
                        |         192.168.10.37-40
                        |
                  [NVR Регистратор]
                    192.168.10.250
```

### Сетевые Порты:

| Порт | Протокол | Назначение | Доступ |
|------|----------|------------|--------|
| 554 | RTSP | Видеопотоки | Local network |
| 80 | HTTP | NVR веб-интерфейс | Local network |
| 8080 | ONVIF | ONVIF события | Local network |
| 5432 | PostgreSQL | База данных | Local (Docker) |
| 6379 | Redis | Кэш | Local (Docker) |
| 8080 | HTTP/HTTPS | API сервер | Local network |
| 445 | SMB | Файловый доступ | Local network |
| 22 | SSH | Удалённый доступ | Local network |

---

## 🔐 Безопасность

### Текущее Состояние:

| Компонент | Шифрование | Аутентификация | Статус |
|-----------|------------|----------------|--------|
| RTSP | ❌ Нет | ✅ Базовая | ⚠️ Требует TLS |
| PostgreSQL | ✅ TLS | ✅ Username/Password | ✅ |
| Redis | ✅ TLS | ✅ Password | ✅ |
| API | 🟡 HTTP | ✅ JWT | ⚠️ Требуется HTTPS |
| SMB | 🟡 зависит | ✅ Username/Password | ⏸️ |

### Рекомендации:

1. **RTSP over TLS:**
   - Настроить RTSPS (RTSP over TLS)
   - Использовать сертификаты

2. **HTTPS для API:**
   - Настроить SSL/TLS
   - Certificate pinning

3. **Сетевая сегментация:**
   - Отдельная VLAN для камер
   - Firewall правила

---

## 📋 Состояние Компонентов

| Компонент | Наличие | Конфигурация | Интеграция | Статус |
|-----------|---------|--------------|------------|--------|
| **Камеры (7 шт)** | ✅ | ✅ | ✅ | ✅ Добавлены в IP-CSS |
| **NVR Регистратор** | ✅ | ✅ | ⏸️ | 🟡 Готов к интеграции |
| **Raspberry Pi 4** | ✅ | ❌ | ❌ | ⏸️ Не готов |
| **NAS** | ✅ | ❌ | ❌ | ⏸️ Не готов |
| **Сеть 192.168.10.0/24** | ✅ | ✅ | ✅ | ✅ Готово |

---

## 🚀 План Настройки

### Этап 1: Настройка NAS (Приоритет 1)

1. [ ] Подключиться к веб-интерфейсу NAS (192.168.10.37)
2. [ ] Проверить доступность LDAP (порт 389)
3. [ ] Создать SMB/NFS шару `/storage/recordings`
4. [ ] Настроить права доступа для пользователя `ipcss`
5. [ ] Подмонтировать NAS на сервере IP-CSS
6. [ ] Настроить интеграцию LDAP с IP-CSS API
7. [ ] Протестировать запись на NAS

### Этап 2: Настройка Raspberry Pi (Приоритет 2)

1. [ ] Установить Raspberry Pi OS на оба устройства (192.168.10.45, 46)
2. [ ] Настроить статический IP и SSH доступ
3. [ ] Установить RTSP прокси (mediamtx)
4. [ ] Настроить автозапуск сервисов
5. [ ] Интегрировать с NAS для хранения записей
6. [ ] Протестировать RTSP потоки
7. [ ] Настроить мониторинг

### Этап 3: Подключение Камер

1. [ ] Проверить физическое наличие камер
2. [ ] Подтвердить/обновить IP-адреса
3. [ ] Протестировать RTSP потоки
4. [ ] Обновить конфигурацию в IP-CSS

### Этап 4: Интеграция

1. [ ] Настроить RTSP прокси на Raspberry Pi
2. [ ] Настроить запись на NAS через IP-CSS
3. [ ] Интегрировать LDAP аутентификацию
4. [ ] Провести полевую валидацию
5. [ ] Настроить резервное копирование

---

## 📁 Конфигурационные Файлы

| Файл | Назначение | Статус |
|------|------------|--------|
| `config/test-cameras-local-network.json` | Конфигурация камер | ✅ |
| `docs/ENVIRONMENTS.md` | Документация окружений | ✅ |
| `docs/NETWORK_TOPOLOGY.md` | Сетевая топология | ✅ (текущий файл) |

---

## 📝 Примечания

### Известные Проблемы:

1. **Камеры:** 
   - Физическое наличие в сети не подтверждено
   - Требуется тестирование RTSP потоков

2. **Raspberry Pi:**
   - OS не установлена
   - Требуется настройка RTSP прокси
   - Интеграция с NAS не выполнена

3. **NAS:**
   - Модель не определена
   - LDAP интеграция не протестирована
   - SMB/NFS шары не настроены
   - Монтирование на сервере не выполнено

### Рекомендации:

1. **Приоритет 1 - NAS:**
   - Подключиться к веб-интерфейсу
   - Проверить LDAP доступность
   - Настроить SMB/NFS шары
   - Интегрировать с IP-CSS

2. **Приоритет 2 - Raspberry Pi:**
   - Установить OS
   - Настроить RTSP прокси
   - Интегрировать с NAS

3. **Приоритет 3 - Камеры:**
   - Проверить физическое наличие
   - Протестировать потоки
   - Обновить конфигурацию

4. **Общие:**
   - Провести комплексную полевую валидацию
   - Настроить мониторинг
   - Настроить резервное копирование

---

*Документ создан: 2026-06-09*  
*Версия: 1.0*  
*Статус: АКТУАЛЬНО*
