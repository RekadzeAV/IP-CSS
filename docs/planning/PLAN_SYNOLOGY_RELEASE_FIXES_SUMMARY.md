# Краткий план устранения недочетов для Synology NAS

## 🔴 КРИТИЧЕСКИЕ (P0) - 2.5 часа

| № | Задача | Время | Файлы |
|---|--------|------|-------|
| 1.1 | Создать иконки (72x72, 256x256) | 1-2ч | `platforms/nas-*/packages/synology/icons/` |
| 1.2 | Обновить скрипт сборки для иконок | 30мин | `scripts/build-nas-package.sh` |
| 1.3 | Собрать артефакты (API + Web) | 20-30мин | `server/api/`, `server/web/` |
| 1.4 | Улучшить обработку отсутствия Web UI | 15мин | `scripts/build-nas-package.sh`, `start.sh` |

## 🟠 ВАЖНЫЕ (P1) - 50 минут

| № | Задача | Время | Файлы |
|---|--------|------|-------|
| 2.1 | Синхронизировать версию из gradle.properties | 10мин | `scripts/build-nas-package.sh` |
| 2.2 | Добавить проверку Node.js в preinst | 20мин | `platforms/nas-*/packages/synology/scripts/preinst` |
| 2.3 | Улучшить проверку Java (17+) | 15мин | `platforms/nas-*/packages/synology/scripts/preinst` |
| 2.4 | Удалить дубликаты в INFO файлах | 5мин | `platforms/nas-*/packages/synology/INFO` |

## 🟡 ПРОВЕРКИ (P2) - 1 час

| № | Задача | Время | Файлы |
|---|--------|------|-------|
| 3.1 | Проверить права доступа на скрипты | 5мин | Все `.sh` файлы |
| 3.2 | Валидировать пути в скриптах | 15мин | `start.sh`, `stop.sh`, `detect-nas-paths.sh` |
| 3.3 | Создать инструкцию по установке | 30мин | `INSTALLATION.md` |
| 3.4 | Создать тестовый SPK пакет | 10мин | `build/` |

## 🚀 Быстрый старт

```bash
# 1. Создать иконки (ImageMagick)
convert -size 72x72 xc:transparent -fill "#2196F3" -draw "roundrectangle 0,0 72,72 10,10" -pointsize 20 -fill white -gravity center -annotate +0+0 "IP-CSS" platforms/nas-x86_64/packages/synology/icons/PACKAGE_ICON.PNG
convert -size 256x256 xc:transparent -fill "#2196F3" -draw "roundrectangle 0,0 256,256 30,30" -pointsize 60 -fill white -gravity center -annotate +0+0 "IP-CSS" platforms/nas-x86_64/packages/synology/icons/PACKAGE_ICON_256.PNG

# 2. Собрать артефакты
./gradlew :server:api:build
cd server/web && npm install && npm run build && cd ../..

# 3. Собрать пакет
./scripts/build-nas-package.sh synology x86_64 Alfa-0.0.1
```

**Полный план:** [PLAN_SYNOLOGY_RELEASE_FIXES.md](PLAN_SYNOLOGY_RELEASE_FIXES.md)
