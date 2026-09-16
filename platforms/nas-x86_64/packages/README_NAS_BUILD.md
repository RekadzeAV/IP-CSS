# Сборка NAS пакетов

## Блок 2: Инфраструктура

- **Gradle модули:** `:platforms:nas-x86_64:build`, `:platforms:nas-arm:build`
  - Задачи: `buildSynologySpk`, `buildQnapQpkg`, `buildAsustorApk`, `buildTruenas`, `buildAllNasPackages`
- **Скрипты:** `scripts/build-nas-package.sh`, `scripts/build-synology-spk.sh`
- **CI/CD:** `.github/workflows/build-nas-packages.yml` — сборка по тегам и ручной запуск

## Блок 3: Synology SPK

- **Структура SPK:** INFO, package.tgz, preinst, postinst, preuninst, postuninst (в корне архива)
- **Содержимое package.tgz:** bin/ (start.sh, stop.sh), lib/ (server.jar), web/dist/, scripts/, conf/
- **Скрипты установки:** preinst (проверка Java, места), postinst (каталоги, конфиг, запуск), preuninst/postuninst (остановка, очистка)
- **Архитектуры:** x86_64 (nas-x86_64), arm64 (nas-arm)

## Требования для сборки

1. Собрать API сервер: `./gradlew :server:api:build`
2. (Опционально) Собрать веб: `cd server/web && npm ci && npm run build`
3. Запустить сборку SPK:
   - `./gradlew :platforms:nas-x86_64:build:buildSynologySpk`
   - или `./scripts/build-synology-spk.sh x86_64 Alfa-0.1.1`

## Fat JAR для NAS

Стандартная сборка `:server:api:build` создаёт JAR без зависимостей. Для работы на NAS нужен «fat» JAR (со всеми зависимостями). Варианты:

1. Добавить в `server/api/build.gradle.kts` плагин Shadow и задачу `shadowJar`, копирующую выход в `server-api.jar` или настроить скрипт на использование этого артефакта.
2. Либо копировать в пакет все JAR из `server/api/build/libs/` и запускать с `-cp "lib/*"` и главным классом.

Результат сборки: `build/ip-css-<version>-synology-<arch>.spk`

## Блок 4: QNAP QPKG

### 4.1 Структура QPKG

- **Файлы пакета:** `QPKG.INFO`, `package.tgz`
- **Содержимое package.tgz (в корне установки):** `init.sh`, `start.sh`, `stop.sh`, `uninstall.sh`, `service.sh`, каталоги `lib/`, `web/`, `scripts/`
- **QPKG.INFO:** имя, версия, архитектура, порты, ServiceShell для App Center

### 4.2 Скрипты управления

| Скрипт       | Назначение |
|-------------|------------|
| `init.sh`   | Установка/обновление: каталоги, конфиг, systemd/init.d, автозапуск |
| `start.sh`  | Запуск API и веб-сервера, запись PID |
| `stop.sh`   | Остановка по PID-файлу |
| `uninstall.sh` | Перед удалением: остановка, удаление сервисов |
| `service.sh`   | Точка входа для App Center: `start` \| `stop` \| `restart` |

Вспомогательный скрипт: `scripts/detect-nas-paths.sh` — определение путей данных по типу NAS.

### 4.3 Интеграция с App Center

- В **QPKG.INFO** заданы `<ServiceShell>service.sh</ServiceShell>`, `<ServicePort>8080</ServicePort>`, `<ServicePortName>http</ServicePortName>`.
- QTS вызывает `$QPKG_ROOT/service.sh start` / `stop` при включении/выключении пакета.
- `init.sh` дополнительно регистрирует systemd или init.d для автозапуска после загрузки системы.

### 4.4 Поддержка архитектур

| Архитектура | Платформа  | Задача Gradle / вызов скрипта |
|------------|------------|-------------------------------|
| x86_64     | nas-x86_64 | `:platforms:nas-x86_64:build:buildQnapQpkg` или `build-nas-package.sh qnap x86_64` |
| ARMv8 (arm64) | nas-arm  | `:platforms:nas-arm:build:buildQnapQpkg` или `qnap arm64` |
| ARMv7      | nas-arm    | `:platforms:nas-arm:build:buildQnapQpkgArmv7` или `qnap armv7` |

В QPKG.INFO подставляются значения: `x86_64`, `arm_64`, `arm_32`.

Результат сборки: `build/ip-css-<version>-qnap-<arch>.qpkg`

## Блок 5: Asustor APK

### 5.1 Структура APK

- **Файлы пакета:** INFO, package.tgz, preinst, postinst, preuninst, postuninst (в корне архива .apk)
- **Содержимое package.tgz:** bin/ (start.sh, stop.sh), lib/ (server.jar), web/dist/, scripts/

### 5.2 Интеграция с App Central

- **INFO:** `startable="yes"`, скрипты preinst/postinst создают systemd или init.d и запускают `$INSTALL_DIR/bin/start.sh`.
- ADM вызывает start/stop через зарегистрированный сервис; данные — в `/volume1/.@plugins/AppCentral/ip-css/data`.

### 5.3 Поддержка архитектур

| Архитектура   | Платформа  | Задача / вызов |
|---------------|------------|----------------|
| x86_64        | nas-x86_64 | `buildAsustorApk` или `build-nas-package.sh asustor x86_64` |
| ARMv8 (arm64) | nas-arm    | `buildAsustorApk` или `asustor arm64` |
| Realtek RTD1296 | nas-arm  | `buildAsustorApkRtd1296` или `asustor rtd1296` |

В INFO подставляются: `x86_64`, `armv8`, `rtd1296`.

Результат: `build/ip-css-<version>-asustor-<arch>.apk`

## Блок 6: TrueNAS

### 6.1 TrueNAS CORE (FreeBSD Jail)

- В сборку входит каталог **core/**: `README.md`, `setup-jail.sh`.
- `setup-jail.sh` выводит пошаговые команды для создания iocage-джейла, монтирования данных и запуска Java-сервера.
- Рекомендуется разворачивать серверный JAR вручную в джейле (Java 17, конфиг, порты 8080/8081).

### 6.2 TrueNAS SCALE (Docker и Kubernetes)

- **Docker:** в сборке есть `docker-compose.yml` (образы company/ip-css-api, company/ip-css-web). Запуск: `docker-compose up -d`.
- **Kubernetes:** манифесты в `kubernetes/` (Deployment, Service, PVC). Применение: `kubectl apply -f kubernetes/`.

Сборка: `./gradlew :platforms:nas-x86_64:build:buildTruenas` → `build/truenas-<version>/`.

## Блок 7: Интеграция и финализация

### 7.1 Интеграция с NAS API

- **Synology:** при необходимости — DSM API (статус пакета, уведомления). Сейчас пакет самодостаточен.
- **QNAP:** опционально — QNAP API для автозапуска/статуса; основное управление через ServiceShell.
- **Asustor:** управление через systemd/init.d и App Central; при необходимости — ADM API.

### 7.2 Оптимизация

- **Память:** в start.sh заданы `-Xmx512m -Xms256m`; для слабых NAS можно уменьшить.
- **Диск:** записи и БД хранятся в каталогах данных платформы; SSD для БД предпочтителен.
- **Сеть:** порты 8080 (веб), 8081 (API); при прокси или фаерволе учесть оба.

### 7.3 Документация

- **Установка:** для каждой платформы — разделы выше и `docs/NAS_INSTALL_AND_FAQ.md`.
- **Настройка:** config.yaml в каталоге config пакета (пути к БД, записи, логи).
- **FAQ:** см. docs/NAS_INSTALL_AND_FAQ.md.
