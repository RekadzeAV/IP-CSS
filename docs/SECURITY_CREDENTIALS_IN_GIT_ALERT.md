# 🚨 Security Alert: Пароли в Git

**Дата обнаружения:** 2026-05-29  
**Статус:** ⚠️ КРИТИЧЕСКАЯ ПРОБЛЕМА

---

## ⚠️ ОБНАРУЖЕНО

**Пароли уже закоммичены в Git историю!**

### Закоммиченные файлы с паролями

| Файл | Содержит | Статус |
|------|----------|--------|
| `credentials/.env.nas` | NAS пароли | ⚠️ В истории Git |
| `credentials/.env.rpi4` | Pi 4 пароли | ⚠️ В истории Git |
| `credentials/.env.ai` | AI API ключи | ⚠️ В истории Git |

### Обнаруженные пароли в истории

```
credentials/.env.nas:
- NAS_PASSWORD=<REDACTED — см. credentials/.env.nas>
- GIT_PASSWORD=$QN~snl0
- NAS_USER2=git
- NAS_PASSWORD2=git%eEMX8cm
- NAS_USER3=GitUser
- NAS_PASSWORD3=G)3;Th(U
```

---

## 🔴 КРИТИЧНОСТЬ

**Уровень риска:** 🔴 ВЫСОКИЙ

- Пароли видны в Git истории
- Любой с доступом к репозиторию может увидеть пароли
- Пароли могут быть скомпрометированы
- Требуется СРОЧНОЕ изменение паролей

---

## 🛠️ Требуемые действия

### Немедленно (прямо сейчас!)

1. **Сменить все пароли:**
   - NAS пароль Andrey
   - Git сервер пароль VSCode
   - Дополнительные пользователи git, GitUser
   - Raspberry Pi 4 пароль

2. **Удалить из Git истории:**
   ```bash
   # Удалить credentials/ из всей истории
   git filter-branch --force --index-filter \
     'git rm --cached --ignore-unmatch credentials/*' \
     --prune-empty --tag-name-filter cat -- --all
   
   # Очистить кэш
   git reflog expire --expire=now --all
   git gc --prune=now --aggressive
   
   # Принудительно обновить remote
   git push origin --force --all
   git push github --force --all
   ```

3. **Добавить в .gitignore:**
   ```gitignore
   credentials/
   credentials/**
   !credentials/.env.example
   ```

4. **Создать .env.example:**
   ```env
   NAS_HOST=192.168.10.38
   NAS_USER=Andrey
   NAS_PASSWORD=YOUR_PASSWORD_HERE
   ```

### После смены паролей

1. **Проверить историю:**
   ```bash
   git log --all --full-history -- credentials/
   # Должно быть пусто
   ```

2. **Проверить remote:**
   ```bash
   git log origin/main --full-history -- credentials/
   git log github/main --full-history -- credentials/
   ```

---

## ✅ Текущее состояние

| Проверка | Статус |
|----------|--------|
| Файлы в .gitignore | ✅ credentials/ добавлен |
| Файлы в staging | ⚠️ Нужно удалить |
| Файлы в истории | ⚠️ Требуют очистки |
| Пароли сменены | ❌ Не проверено |

---

## 📋 Чек-лист для исправления

- [ ] Сменить NAS пароль Andrey
- [ ] Сменить Git сервер пароль
- [ ] Сменить Raspberry Pi 4 пароль
- [ ] Сменить все дополнительные пароли
- [ ] Удалить credentials/ из Git истории
- [ ] Очистить Git reflog
- [ ] Принудительно обновить remote
- [ ] Проверить, что файлы удалены из истории
- [ ] Создать credentials/.env.example
- [ ] Обновить документацию

---

## 🔐 После исправления

### Безопасная структура

```
credentials/
├── .env.example          # Шаблон (коммитится)
├── .env.nas              # Реальные данные (игнорируется)
├── .env.rpi4             # Реальные данные (игнорируется)
├── .env.ai               # Реальные данные (игнорируется)
├── README.md             # Документация (коммитится)
└── ssh-keys/             # SSH ключи (игнорируется)
```

### .gitignore

```gitignore
credentials/
credentials/**
!credentials/.env.example
!credentials/README.md
```

---

## ⚠️ Важно

**После смены паролей:**
1. Обновите все локальные копии репозитория
2. Пересоздайте SSH ключи
3. Обновите CI/CD пайплайны
4. Проверьте все сервисы

---

**Статус:** ⚠️ ТРЕБУЕТСЯ СРОЧНОЕ ВМЕШАТЕЛЬСТВО  
**Приоритет:** CRITICAL  
**Исполнитель:** Владелец репозитория
