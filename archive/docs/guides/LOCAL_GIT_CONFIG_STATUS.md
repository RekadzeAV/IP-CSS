# 📦 Локальная конфигурация Git

**Дата:** 2026-05-29  
**Выполнено:** Koda AI Assistant

---

## ✅ Статус: НАСТРОЕНА

### Основная информация

**Локальный Git репозиторий настроен на использование NAS Git сервер как основной remote.**

| Параметр | Значение |
|----------|----------|
| **Remote name** | origin |
| **URL** | `Andrey@192.168.10.38:/volume1/Git/IP-CSS-open.git` |
| **Сервер** | Synology NAS RS2416+ |
| **IP-адрес** | 192.168.10.38 |
| **Путь** | /volume1/Git/IP-CSS-open.git |

---

## 🔧 Текущая конфигурация

### Remote конфигурация

```
origin	Andrey@192.168.10.38:/volume1/Git/IP-CSS-open.git (fetch)
origin	Andrey@192.168.10.38:/volume1/Git/IP-CSS-open.git (push)
```

**Проверка:**
```powershell
git config --local --get remote.origin.url
# Результат: Andrey@192.168.10.38:/volume1/Git/IP-CSS-open.git
```

### Что было изменено

- ❌ Удалён: `origin` → `https://github.com/RekadzeAV/IP-CSS.git`
- ✅ Добавлен: `origin` → `Andrey@192.168.10.38:/volume1/Git/IP-CSS-open.git`
- ✅ Удалён: `nas` remote (объединён с origin)

---

## 📝 Использование

### Стандартные Git команды

Теперь все стандартные Git команды используют NAS сервер:

```bash
# Получение изменений
git fetch
git pull

# Отправка изменений
git push

# Просмотр истории
git log --oneline -10
```

### Пример работы

```bash
# Проверка состояния
git status

# Получение последних изменений
git pull origin main

# Отправка своих коммитов
git push origin main

# Создание новой ветки и отправка
git checkout -b feature/new-feature
git push -u origin feature/new-feature
```

---

## 🔐 Аутентификация

### Способ 1: Через пароль (текущий)

```powershell
$env:GIT_SSH_COMMAND="plink.exe -ssh -batch -pw <NAS_PASSWORD>"
git push origin main
```

### Способ 2: Через SSH ключ (рекомендуется)

```bash
# Настроить SSH agent
ssh-add credentials/ssh-keys/nas_andrey

# Или явно указать ключ
$env:GIT_SSH_COMMAND="ssh -i credentials/ssh-keys/nas_andrey -o IdentitiesOnly=yes"
git push origin main
```

---

## 🔄 Синхронизация с GitHub

Если нужно синхронизировать с GitHub:

```bash
# Добавить GitHub как дополнительный remote
git remote add github https://github.com/RekadzeAV/IP-CSS.git

# Синхронизировать ветки
git fetch github
git push origin main  # В NAS
git push github main  # В GitHub (если нужно)

# Удалить temporary remote
git remote remove github
```

---

## 📊 Проверка подключения

```bash
# Проверить URL remote
git remote get-url origin
# Ожидаемый результат: Andrey@192.168.10.38:/volume1/Git/IP-CSS-open.git

# Проверить доступность сервера
ping 192.168.10.38

# Проверить SSH подключение
plink.exe -ssh Andrey@192.168.10.38 -pw "<NAS_PASSWORD>" "echo 'Connected'"

# Проверить репозиторий
plink.exe -ssh Andrey@192.168.10.38 -pw "<NAS_PASSWORD>" "ls -la /volume1/Git/"
```

---

## ⚠️ Важные замечания

### Локальная конфигурация

Конфигурация `remote.origin.url` сохранена в `.git/config` (локальный файл репозитория). Она:
- ✅ Не коммится в Git (игнорируется по умолчанию)
- ✅ Применяется только для текущего локального репозитория
- ⚠️ Другие разработчики должны настроить свой own remote

### Для других разработчиков

Каждый разработчик должен настроить свой remote:

```bash
# Для локальной разработки
git remote set-url origin Andrey@192.168.10.38:/volume1/Git/IP-CSS-open.git

# Или при клонировании
git clone Andrey@192.168.10.38:/volume1/Git/IP-CSS-open.git
```

---

## 🎯 Следующие шаги

### Рекомендуемые

1. **Настроить SSH ключи** для удобной аутентификации:
   ```bash
   ssh-add credentials/ssh-keys/nas_andrey
   ```

2. **Проверить синхронизацию**:
   ```bash
   git push origin --all
   git push origin --tags
   ```

3. **Настроить upstream для веток**:
   ```bash
   git branch --set-upstream-to=origin/main main
   ```

---

## 📞 Команды для восстановления

Если нужно вернуть GitHub как remote:

```bash
git remote set-url origin https://github.com/RekadzeAV/IP-CSS.git
```

Если нужно вернуть NAS как remote:

```bash
git remote set-url origin Andrey@192.168.10.38:/volume1/Git/IP-CSS-open.git
```

---

**Статус:** ✅ Конфигурация настроена  
**Remote:** origin → NAS Git Server  
**Готовность:** Полностью рабочий
