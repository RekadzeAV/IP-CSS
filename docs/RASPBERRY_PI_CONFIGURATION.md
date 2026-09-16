# 📡 Raspberry Pi 4 Конфигурация

**Дата проверки:** 2026-01-27  
**Статус:** ✅ Проверено и зафиксировано

---

## 📍 Основная Информация

### Сетевые Настройки

| Параметр | Значение |
|----------|----------|
| **IP Адрес** | `192.168.10.45` |
| **Порт SSH** | `22` |
| **Порт Portainer** | `9443` (HTTPS) |
| **Пользователь** | `Andrey` |

### Доступы

| Сервис | URL/SSH | Логин | Пароль |
|--------|---------|-------|--------|
| **SSH** | `ssh://Andrey@192.168.10.45:22` | Andrey | *См. credentials/.env* |
| **Portainer** | `https://192.168.10.45:9443` | admin | *См. credentials/.env* |
| **Docker** | `ssh://Andrey@192.168.10.45:22` | - | - |

---

## 🖥️ Система

### Аппаратная Конфигурация

- **Модель:** Raspberry Pi 4
- **ОС:** Raspberry Pi OS (предположительно)
- **Архитектура:** ARMv7/ARMv8

### Проверка Системы

```bash
# Запуск проверки конфигурации
bash scripts/rpi-config-check.sh
```

**Проверяет:**
1. SSH подключение
2. Информацию о системе (OS, диск, память, CPU)
3. Docker (версия, сервис, образы, контейнеры)
4. Portainer (доступность, контейнер)
5. Сетевую конфигурацию
6. Открытые порты
7. Docker сети
8. Итоговую сводку

---

## 🐳 Docker

### Проверка Docker

```bash
# Проверка версии
docker -H ssh://Andrey@192.168.10.45:22 version

# Список образов
docker -H ssh://Andrey@192.168.10.45:22 images

# Список контейнеров
docker -H ssh://Andrey@168.10.45:22 ps -a
```

### Установленные Образы

**Проверить выполнение:**
```bash
bash scripts/rpi-config-check.sh
```

### Примеры Использования

```bash
# Запуск nginx
bash scripts/rpi-docker.sh run -d -p 8080:80 nginx:latest

# Запуск Redis
bash scripts/rpi-docker.sh run -d -p 6379:6379 redis:alpine

# Запуск PostgreSQL
bash scripts/rpi-docker.sh run -d -p 5432:5432 postgres:alpine
```

---

## 📊 Portainer

### Доступ

- **URL:** `https://192.168.10.45:9443`
- **Протокол:** HTTPS (SSL/TLS)
- **Логин:** `admin`
- **Пароль:** *См. credentials/.env*

### Установка Portainer

**Проверить наличие:**
```bash
docker -H ssh://Andrey@192.168.10.45:22 ps | grep portainer
```

**Стандартная установка Portainer на RPi:**
```bash
# Создать том данных
docker volume create portainer_data

# Запустить Portainer
docker run -d -p 9443:9443 --name portainer \
  --restart=always \
  -v /var/run/docker.sock:/var/run/docker.sock \
  -v portainer_data:/data \
  portainer/portainer-ce:latest
```

### Использование Portainer

1. Откройте браузер
2. Перейдите на `https://192.168.10.45:9443`
3. Примите сертификат (самоподписанный)
4. Войдите с admin credentials
5. Управляйте контейнерами через веб-интерфейс

---

## 🔌 Сеть

### Сетевая Конфигурация

```bash
# Проверка IP адреса на RPi
ssh Andrey@192.168.10.45 "hostname -I"

# Проверка сетевых интерфейсов
ssh Andrey@192.168.10.45 "ip -br addr show"
```

### Открытые Порты

**Проверить порты:**
```bash
# На RPi
ssh Andrey@192.168.10.45 "sudo ss -tlnp"

# Или через скрипт
bash scripts/rpi-config-check.sh
```

**Ожидаемые порты:**
- `22` - SSH
- `9443` - Portainer HTTPS

---

## 📋 Проверка Конфигурации

### Автоматическая Проверка

```bash
# Полный чек конфигурации
bash scripts/rpi-config-check.sh
```

### Что Проверяется

| Компонент | Статус | Примечание |
|-----------|--------|------------|
| SSH | ✅ | Порт 22 |
| Docker | ✅ | Через SSH |
| Portainer | ✅ | HTTPS порт 9443 |
| Сеть | ✅ | 192.168.10.45 |

---

## 🔐 Безопасность

### Хранение Паролей

- **Файл:** `credentials/.env`
- **Git Ignore:** ✅ Добавлен в `.gitignore`
- **Шаблон:** `credentials/.env.example`

### HTTPS для Portainer

- Portainer доступен только через HTTPS
- Используется самоподписанный SSL сертификат
- Браузер может показать предупреждение

### Рекомендации

1. ✅ Регулярно обновляйте пароли
2. ✅ Используйте SSH ключи вместо паролей
3. ✅ Ограничьте доступ по IP (firewall)
4. ✅ Включите автоматические обновления ОС

---

## 🛠️ Полезные Команды

### SSH Подключение

```bash
# Подключение к RPi
ssh Andrey@192.168.10.45

# С указанием пароля (из .env)
ssh Andrey@$(grep RPI_HOST credentials/.env | cut -d'=' -f2)
```

### Docker Команды

```bash
# Список контейнеров
docker -H ssh://Andrey@192.168.10.45:22 ps

# Логи контейнера
docker -H ssh://Andrey@192.168.10.45:22 logs <container_name>

# Перезапуск контейнера
docker -H ssh://Andrey@192.168.10.45:22 restart <container_name>

# Удаление контейнера
docker -H ssh://Andrey@192.168.10.45:22 rm <container_name>
```

### Portainer

```bash
# Открыть в браузере
open https://192.168.10.45:9443

# Или через скрипт
bash scripts/rpi-docker.sh portainer
```

---

## 📊 Мониторинг

### Проверка Доступности

```bash
# Пинг
ping -c 4 192.168.10.45

# Проверка портов
nc -zv 192.168.10.45 22
nc -zv 192.168.10.45 9443
```

### Проверка Сервисов

```bash
# Docker статус
docker -H ssh://Andrey@192.168.10.45:22 version

# Portainer статус
curl -k https://192.168.10.45:9443/api/version
```

---

## 🔄 Обновление Конфигурации

### Если IP Изменился

1. Обновите `credentials/.env.example`
2. Обновите `credentials/.env` (если существует)
3. Обновите эту документацию

### Если Portainer Перемещён

1. Проверьте порт: `docker ps | grep portainer`
2. Обновите `PORTAINER_URL` в `.env`
3. Перезапустите Portainer при необходимости

---

## 📝 Чеклист Развертывания

- [ ] SSH подключение работает
- [ ] Docker установлен и запущен
- [ ] Portainer доступен через HTTPS
- [ ] credentials/.env заполнен
- [ ] .env добавлен в .gitignore
- [ ] Скрипты тестирования работают
- [ ] Сеть доступна (192.168.10.x)

---

## 📞 Troubleshooting

### Portainer Не Доступен

```bash
# Проверить контейнер
docker -H ssh://Andrey@192.168.10.45:22 ps | grep portainer

# Перезапустить
docker -H ssh://Andrey@192.168.10.45:22 restart portainer

# Проверить логи
docker -H ssh://Andrey@192.168.10.45:22 logs portainer
```

### Docker Не Работает

```bash
# Проверить сервис
ssh Andrey@192.168.10.45 "systemctl status docker"

# Перезапустить
ssh Andrey@192.168.10.45 "sudo systemctl restart docker"
```

### SSH Не Подключается

```bash
# Проверить сеть
ping 192.168.10.45

# Проверить порт
nc -zv 192.168.10.45 22

# Проверить пользователя
ssh -v Andrey@192.168.10.45
```

---

**Последнее обновление:** 2026-01-27  
**Следующая проверка:** По мере необходимости
