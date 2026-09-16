# 🔐 Проверка паролей в Git

**Дата:** 2026-05-29  
**Статус:** ⚠️ ТРЕБУЕТСЯ ДЕЙСТВИЕ

---

## 📊 Результаты проверки

### Проверка .gitignore

| Паттерн | Статус | Строка |
|---------|--------|--------|
| `credentials/` | ✅ Игнорируется | 224 |
| `.env` | ✅ Игнорируется | 89 |
| `*.pem` | ✅ Игнорируется | 85 |
| `*.key` | ✅ Игнорируется | 86 |

### Проверка файлов в Git

**Закоммиченные файлы в credentials/:**
```
credentials/.env.ai
credentials/.env.example
credentials/.env.nas
credentials/.env.rpi4
credentials/README.md
credentials/README_NAS.md
```

---

## ⚠️ ОБНАРУЖЕННАЯ ПРОБЛЕМА

### Пароли в истории Git

**Файлы с реальными паролями уже закоммичены:**

| Файл | Последний коммит | Пароли в истории |
|------|------------------|------------------|
| `credentials/.env.nas` | 734ea7e | ✅ Есть |
| `credentials/.env.rpi4` | 734ea7e | ✅ Есть |
| `credentials/.env.ai` | 734ea7e | ✅ Есть |

### Обнаруженные пароли в истории

```
credentials/.env.nas (в коммите 734ea7e):
- NAS_PASSWORD=<REDACTED — см. credentials/.env.nas>
- GIT_PASSWORD=$QN~snl0
- NAS_PASSWORD2=git%eEMX8cm
- NAS_PASSWORD3=G)3;Th(U
```

---

## ✅ Что защищено

### Файлы, которые НЕ попадут в будущие коммиты

| Файл | Статус |
|------|--------|
| credentials/.env | ✅ Игнорируется |
| credentials/.env.nas | ✅ Игнорируется (после очистки) |
| credentials/.env.rpi4 | ✅ Игнорируется (после очистки) |
| credentials/.env.ai | ✅ Игнорируется (после очистки) |
| credentials/ssh-keys/* | ✅ Игнорируется |

### Шаблонные файлы (безопасно для Git)

| Файл | Содержит | Статус |
|------|----------|--------|
| credentials/.env.example | Заглушки паролей | ✅ Безопасно |
| credentials/README.md | Документация | ✅ Безопасно |
| credentials/README_NAS.md | Документация | ✅ Безопасно |

---

## 🛠️ Требуемые действия

### 1. Сменить пароли (КРИТИЧНО!)

**Немедленно смените все пароли:**

- [ ] NAS пароль Andrey: `<NAS_PASSWORD>` → НОВЫЙ
- [ ] Git сервер пароль VSCode: `$QN~snl0` → НОВЫЙ
- [ ] Git пользователь git: `git%eEMX8cm` → НОВЫЙ
- [ ] Git пользователь GitUser: `G)3;Th(U` → НОВЫЙ
- [ ] Raspberry Pi 4 пароль

### 2. Удалить из Git истории

**Команды для удаления:**

```bash
# Удалить credentials/ из всей истории
git filter-branch --force --index-filter \
  'git rm --cached --ignore-unmatch credentials/.env.nas credentials/.env.rpi4 credentials/.env.ai' \
  --prune-empty --tag-name-filter cat -- --all

# Очистить кэш и reflog
git reflog expire --expire=now --all
git gc --prune=now --aggressive

# Принудительно обновить remotes
git push origin --force --all
git push github --force --all
```

### 3. Обновить .gitignore

**Добавить в .gitignore:**
```gitignore
# Credentials (исключить реальные файлы, оставить шаблоны)
credentials/.env.nas
credentials/.env.rpi4
credentials/.env.ai
credentials/ssh-keys/
```

**Примечание:** `credentials/` уже игнорируется на строке 224, но закоммиченные файлы нужно удалить из истории.

### 4. Проверить результат

```bash
# Проверить, что файлы удалены из истории
git log --all --full-history -- credentials/.env.nas
# Должно быть пусто

# Проверить remote
git log origin/main -- credentials/.env.nas
# Должно быть пусто

# Проверить статус
git status --short
# credentials/.env.nas должен быть показан как untracked
```

---

## 📊 Текущее состояние

| Проверка | Статус |
|----------|--------|
| .gitignore настроен | ✅ |
| Файлы в .gitignore | ✅ |
| Файлы в staging | ⚠️ Нужно удалить |
| Файлы в истории | ⚠️ Требуют очистки |
| Пароли сменены | ❌ Не выполнено |

---

## 🔐 После очистки

### Безопасная структура

```
credentials/
├── .env.example          # Шаблон ✅ Коммитится
├── .env.nas              # Реальные данные ⚠️ Игнорируется
├── .env.rpi4             # Реальные данные ⚠️ Игнорируется
├── .env.ai               # Реальные данные ⚠️ Игнорируется
├── README.md             # Документация ✅ Коммитится
├── README_NAS.md         # Документация ✅ Коммитится
└── ssh-keys/             # Ключи ⚠️ Игнорируется
    ├── nas_andrey        # ⚠️ Игнорируется
    ├── rpi4_deploy       # ⚠️ Игнорируется
    └── gitserver_ci      # ⚠️ Игнорируется
```

### .gitignore (актуальный)

```gitignore
# Secrets and credentials
*.pem
*.key
*.crt
*.p12
*.keystore
!debug.keystore
secrets/
.env
.env.local
*.secret
credentials.json
credentials/          # Игнорирует ВСЁ в credentials/
```

---

## ⚠️ Важные замечания

### Риск

- Пароли видны в Git истории
- Доступны всем с доступом к репозиторию
- Могут быть в pull requests, forks, backups

### Рекомендации

1. **СРОЧНО смените все пароли**
2. **Очистите Git историю**
3. **Проверьте всех соавторов**
4. **Уведомите команду**
5. **Рассмотрите использование secrets manager**

---

## 📋 Чек-лист безопасности

- [ ] Сменены все пароли
- [ ] Удалены из Git истории
- [ ] Очищен Git reflog
- [ ] Принудительно обновлены remotes
- [ ] Проверено, что файлы удалены
- [ ] Обновлены CI/CD пайплайны
- [ ] Обновлены локальные копии
- [ ] Создана документация по безопасному использованию

---

**Статус:** ⚠️ ТРЕБУЕТСЯ ДЕЙСТВИЕ  
**Приоритет:** CRITICAL  
**Следующий шаг:** Смена паролей
