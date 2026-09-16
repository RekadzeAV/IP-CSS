# 📦 Архив: NAS Git-выкладка (отключена 15.09.2026)

**Дата архивации:** 15.09.2026
**Причина:** git-remote NAS (`origin` → `Andrey@192.168.10.38:/volume1/Git/IP-CSS-open.git`) **отключён** как канал доставки. Точка истины — `github/main` (`https://github.com/RekadzeAV/IP-CSS.git`).

---

## Что было отключено

| Объект | Было | Стало |
|---|---|---|
| Git-remote `origin` | `Andrey@192.168.10.38:/volume1/Git/IP-CSS-open.git` (push/fetch) | **удалён** (`git remote remove origin`) |
| Синхронизация `main` → NAS | опциональный push в `origin/main` (см. `PLAN_EXECUTION_MASTER.md`) | **не выполняется**; единственный remote — `github` |

### Основание для отключения
1. **Технически недоступен из окружения разработки** — при обращении к `origin` сервер запрашивает интерактивный пароль (`Andrey@192.168.10.38's password:`), SSH-ключ `credentials/ssh-keys/` без пароля не подключён. Проверено 15.09.2026: `git ls-remote origin` → `unable to fork` / запрос пароля.
2. **Дублирование канала доставки** — GitHub (`github/main`) полностью покрывает распространение кода, CI/CD и релизные артефакты.
3. **Безопасность** — NAS-доступ требовал хранения пароля в документации и скриптах (выведен из трекинга 15.09, коммит `2ac6cf17`); отключение устраняет класс риска утечки.

---

## Что сохранено (архив)

| Файл | Было | Содержание |
|---|---|---|
| `NAS_GIT_OPERATIONS_REPORT.md` | `docs/` | Операции git на NAS: создание bare-репозитория, настройка, служебные команды |
| `NAS_SSH_KEYS_REPORT.md` | `docs/` | SSH-ключи для NAS-доступа, команды `git push nas --all`, конфигурация |

> ⚠️ **Не удалено** — файлы перемещены; история сохранена. При необходимости восстановления NAS-канала — см. процедуру ниже.

---

## Что НЕ затронуто (остаётся активным)

Отключение касается **только git-remoting**. Следующее работает штатно:

- **NAS-пакеты как продукт**: `scripts/build-nas-packages.sh` (SPK/QPKG/APK), `platforms/nas-x86_64/`, `platforms/nas-arm/` — платформенная поддержка Synology/QNAP/Asustor/TrueNAS **сохраняется**.
- **Docker-образы для TrueNAS**: `platforms/nas-x86_64/Dockerfile`.
- **CI**: `.github/workflows/build-nas-packages.yml` (сборка пакетов по тегам), `ci.yml`/`release.yml` (docker-таргеты NAS).
- **Прочие NAS-документы** (`docs/NAS_BUILD_REQUIREMENTS_SUMMARY.md`, `NAS_DOCKER_CONFIG.md`, `NAS_INSTALL_AND_FAQ.md`, `NAS_LOCAL_BUILD_REQUIREMENTS.md`) — остаются активными как справочные по **сборке/установке**.

---

## Процедура восстановления (если понадобится)

```bash
# 1. Вернуть remote
git remote add origin Andrey@192.168.10.38:/volume1/Git/IP-CSS-open.git

# 2. Настроить беспарольный SSH-доступ (ключ из credentials/ssh-keys/)
#    или использовать plink с сохранённой сессией

# 3. Синхронизировать
git push origin main
```

Детали — в архивных `NAS_SSH_KEYS_REPORT.md` и `NAS_GIT_OPERATIONS_REPORT.md`.

---

**Версия проекта:** 0.5.1.1-beta · **Коммит отключения:** см. историю `main` (ревизия 15.09.2026)
