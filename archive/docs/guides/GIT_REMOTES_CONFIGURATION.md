# 📦 Git Remotes Конфигурация

**Дата:** 2026-05-29  
**Выполнено:** Koda AI Assistant

---

## ✅ Статус: НАСТРОЕНА

### Конфигурация Git Remotes

В проекте настроены **два Git remote**:

| Remote | URL | Назначение | Статус |
|--------|-----|------------|--------|
| **origin** | `Andrey@192.168.10.38:/volume1/Git/IP-CSS-open.git` | Локальный NAS Git Server | ✅ Основной |
| **github** | `https://github.com/RekadzeAV/IP-CSS.git` | Внешний GitHub репозиторий | ✅ Дополнительный |

---

## 🔧 Детали конфигурации

### Remote: origin (NAS)

**Параметры:**
- **URL:** `Andrey@192.168.10.38:/volume1/Git/IP-CSS-open.git`
- **Тип:** SSH
- **Сервер:** Synology NAS RS2416+
- **IP-адрес:** 192.168.10.38
- **Назначение:** Основной локальный Git сервер

**Использование:**
```bash
# Основная работа
git push origin main
git pull origin main
```

**Аутентификация:**
- SSH пароль (временно)
- SSH ключ `credentials/ssh-keys/nas_andrey` (рекомендуется)

---

### Remote: github (GitHub)

**Параметры:**
- **URL:** `https://github.com/RekadzeAV/IP-CSS.git`
- **Тип:** HTTPS
- **Сервер:** GitHub.com
- **Назначение:** Внешний резервный репозиторий

**Использование:**
```bash
# Синхронизация с GitHub
git push github main
git pull github main
```

**Аутентификация:**
- GitHub Token / Personal Access Token
- SSH ключ GitHub (если настроен)

---

## 📊 Текущее состояние

### Все remotes

```
github	https://github.com/RekadzeAV/IP-CSS.git (fetch)
github	https://github.com/RekadzeAV/IP-CSS.git (push)
origin	Andrey@192.168.10.38:/volume1/Git/IP-CSS-open.git (fetch)
origin	Andrey@192.168.10.38:/volume1/Git/IP-CSS-open.git (push)
```

### Ветки на NAS (origin)

- `main` ✅
- `chore/kmp-phase1-closure-reporting` ✅
- `chore/kmp-phase1.5-execution` ✅

### Ветки на GitHub

- `main` ✅
- `main-old` ✅
- `commit` ✅
- `dev/android` ✅
- `dev/desktop` ✅
- `dev/ios` ✅
- `develop/platform-client-android` ✅
- `develop/platform-client-desktop-arm` ✅
- `develop/platform-client-desktop-x86_64` ✅
- `develop/platform-client-ios` ✅
- `develop/platform-nas-arm` ✅
- `develop/platform-nas-x86_64` ✅
- `develop/platform-sbc-arm` ✅
- `develop/platform-server-x86_64` ✅
- `feature/native-libraries` ✅
- `feature/network-layer` ✅
- `feature/server-part` ✅
- `feature/ui-components` ✅

---

## 🔄 Использование

### Основная работа (по умолчанию)

Все стандартные команды используют **NAS (origin)**:

```bash
# Просмотр статуса
git status

# Получение изменений
git fetch
git pull

# Отправка изменений
git push

# Создание ветки
git checkout -b feature/new-feature
git push -u origin feature/new-feature
```

### Синхронизация с GitHub

```bash
# Получение из GitHub
git fetch github

# Отправка в GitHub
git push github main

# Синхронизация всех веток
git push github --all
git push github --tags
```

### Синхронизация между NAS и GitHub

```bash
# Копирование ветки с NAS на GitHub
git push github $(git branch --show-current)

# Обновление GitHub из NAS
git fetch origin
git push github origin/main:main

# Двусторонняя синхронизация
git pull origin main
git push github main
```

---

## 🎯 Сценарии использования

### Сценарий 1: Ежедневная разработка

```bash
# Работа с локальным NAS (быстрее)
git pull origin main
# ... сделать изменения ...
git push origin main
```

### Сценарий 2: Резервное копирование на GitHub

```bash
# Раз в день/неделю
git push github --all
git push github --tags
```

### Сценарий 3: Синхронизация с другими разработчиками

```bash
# Если другие разработчики используют GitHub
git fetch github
git merge github/main
git push origin main
```

### Сценарий 4: Восстановление из резервной копии

```bash
# Если NAS недоступен
git pull github main
# Или клонировать из GitHub:
git clone https://github.com/RekadzeAV/IP-CSS.git
```

---

## 🔐 Безопасность

### NAS (origin)

- **Аутентификация:** SSH ключ или пароль
- **Доступ:** Локальная сеть (192.168.10.38)
- **Шифрование:** SSH (зашифровано)
- **Рекомендация:** Использовать SSH ключ `credentials/ssh-keys/nas_andrey`

### GitHub (github)

- **Аутентификация:** Personal Access Token / OAuth
- **Доступ:** Интернет (публичный/приватный)
- **Шифрование:** HTTPS (зашифровано)
- **Рекомендация:** Использовать GitHub Token без прав на запись для pull

---

## 📝 Команды для управления

### Просмотр конфигурации

```bash
# Все remotes
git remote -v

# URL конкретного remote
git remote get-url origin
git remote get-url github

# Детали remote
git remote show origin
git remote show github
```

### Изменение конфигурации

```bash
# Изменить URL origin
git remote set-url origin <новый-url>

# Изменить URL github
git remote set-url github <новый-url>

# Удалить remote
git remote remove github

# Добавить новый remote
git remote add <имя> <url>
```

### Синхронизация

```bash
# Получить все обновления
git fetch --all

# Показать какие ветки отличаются
git remote show origin
git remote show github

# Синхронизировать все ветки
git push origin --all
git push github --all
```

---

## ⚠️ Важные замечания

### Локальная конфигурация

Конфигурация remotes сохраняется в `.git/config` (локально):
- ✅ Не коммится в Git
- ✅ Применяется только к текущему локальному репозиторию
- ⚠️ Другие разработчики должны настроить свои remotes

### При клонировании

При клонировании проекта:
```bash
# Клонировать из NAS (рекомендуется для локальной работы)
git clone Andrey@192.168.10.38:/volume1/Git/IP-CSS-open.git

# Клонировать из GitHub (для внешних разработчиков)
git clone https://github.com/RekadzeAV/IP-CSS.git

# Добавить дополнительный remote после клонирования
git remote add github https://github.com/RekadzeAV/IP-CSS.git
# или
git remote add origin Andrey@192.168.10.38:/volume1/Git/IP-CSS-open.git
```

### При переключении между remotes

```bash
# Переключить основной remote на GitHub временно
git remote set-url origin https://github.com/RekadzeAV/IP-CSS.git

# Вернуть обратно на NAS
git remote set-url origin Andrey@192.168.10.38:/volume1/Git/IP-CSS-open.git
```

---

## 🚀 Рекомендации

### Для локальной разработки

1. **Используйте NAS (origin) по умолчанию**
   - Быстрее (локальная сеть)
   - Полный контроль
   - Нет зависимости от интернета

2. **Регулярно синхронизируйте с GitHub**
   - Раз в день/неделю
   - Перед важными релизами
   - Для резервного копирования

3. **Настройте SSH ключи**
   - Удобнее паролей
   - Безопаснее
   - Автоматизация

### Для командной работы

1. **Определите основной источник истины**
   - NAS для локальной команды
   - GitHub для внешних участников

2. **Настройте автоматическую синхронизацию**
   - GitHub Actions / Jenkins
   - Webhook на NAS
   - Cron задача

3. **Документируйте конфигурацию**
   - В README проекта
   - В документации для разработчиков

---

## 📞 Проверка

```bash
# Проверить remotes
git remote -v

# Проверить доступность NAS
ping 192.168.10.38

# Проверить доступность GitHub
ping github.com

# Проверить ветки на NAS
git branch -r | grep origin

# Проверить ветки на GitHub
git branch -r | grep github

# Проверить последнюю синхронизацию
git log --oneline -5
```

---

## 📄 Сопутствующая документация

- `docs/NAS_SSH_KEYS_REPORT.md` - SSH ключи для NAS
- `docs/NAS_REPO_PUSH_STATUS.md` - Статус репозитория на NAS
- `docs/LOCAL_GIT_CONFIG_STATUS.md` - Локальная конфигурация Git
- `credentials/ssh-keys/README.md` - Документация по SSH ключам

---

**Статус:** ✅ Конфигурация настроена  
**Remotes:** origin (NAS) + github (GitHub)  
**Готовность:** Полностью рабочий
