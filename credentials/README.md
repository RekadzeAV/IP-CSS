# 🔐 Креденциалы для Подключения

**ВАЖНО:** Этот файл содержит инструкции по хранению чувствительных данных.

---

## 📁 Структура

```
credentials/
├── README.md              # Этот файл (безопасный)
├── .env.example           # Пример переменных окружения
└── .env                   # РЕАЛЬНЫЕ данные (в .gitignore!)
```

---

## 🚫 Безопасность

### Что НЕЛЬЗЯ делать:
- ❌ Хранить реальные пароли в коде
- ❌ Коммитить `.env` файл в Git
- ❌ Публиковать credentials в чатах/форумах
- ❌ Делиться паролями без необходимости

### Что НУЖНО делать:
- ✅ Использовать `.env` файл для credentials
- ✅ Добавить `.env` в `.gitignore`
- ✅ Использовать `.env.example` с заглушками
- ✅ Хранить реальные данные в секрете

---

## 📝 Создание .env Файла

```bash
# В корне проекта
cd credentials/
cp .env.example .env
nano .env  # или vim, code, и т.д.
```

### Пример содержимого `.env`:

```env
# Raspberry Pi 4 Connection
RPI_HOST=192.168.1.XXX
RPI_PORT=22
RPI_USER=Andrey
RPI_PASSWORD=YOUR_PASSWORD_HERE  # Замените на реальный пароль

# Docker on Raspberry Pi
DOCKER_HOST=ssh://Andrey@192.168.1.XXX:22

# Portainer
PORTAINER_URL=http://192.168.1.XXX:9000
PORTAINER_ADMIN=admin
PORTAINER_PASSWORD=YOUR_PORTAINER_PASSWORD_HERE  # Замените на реальный пароль
```

---

## 🔒 .gitignore

Файл `.env` должен быть добавлен в `.gitignore`:

```bash
# В корне проекта
echo "credentials/.env" >> .gitignore
git add .gitignore
git commit -m "Add .env to gitignore"
```

---

## 🌐 Получение IP Raspberry Pi

```bash
# На Raspberry Pi
hostname -I

# Или через роутер
# Зайдите в панель управления роутером
# Найдите устройство с именем "raspberrypi" или MAC-адресом
```

---

## 🔑 SSH Подключение

```bash
# Подключение к Raspberry Pi
ssh Andrey@192.168.1.XXX

# С указанием пароля (введется автоматически из .env)
ssh Andrey@$(grep RPI_HOST credentials/.env | cut -d'=' -f2)
```

---

## 🐳 Docker Команды

```bash
# Подключение к Docker на Raspberry Pi
docker -H ssh://Andrey@192.168.1.XXX:22 ps

# Запуск контейнера
docker -H ssh://Andrey@192.168.1.XXX:22 run -d nginx
```

---

## 📊 Portainer

### Доступ:
- URL: `http://192.168.1.XXX:9000`
- Логин: `admin`
- Пароль: [см. `.env` файл]

### Использование:
1. Откройте браузер
2. Перейдите на `http://192.168.1.XXX:9000`
3. Войдите с credentials из `.env`
4. Управляйте контейнерами через веб-интерфейс

---

## 🔄 Обновление Паролей

Если пароль изменится:

```bash
# Обновите credentials/.env
nano credentials/.env

# Перезагрузите переменные
export $(cat credentials/.env | xargs)
```

---

## 🚨 Если Credentials Скомпрометированы

1. Немедленно смените пароли
2. Обновите `.env` файл
3. Проверьте логи доступа
4. Удалите скомпрометированные ключи

---

**Дата создания:** 2026-01-27  
**Последнее обновление:** 2026-01-27
