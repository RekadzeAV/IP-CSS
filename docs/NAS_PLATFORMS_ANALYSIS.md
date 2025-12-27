# Аналитика по NAS устройствам для IP-CSS

**Дата создания:** Декабрь 2025
**Версия:** Alfa-0.0.1
**Статус:** Детальный анализ и рекомендации по реализации

---

## Содержание

1. [Обзор рынка NAS](#обзор-рынка-nas)
2. [Производители и платформы](#производители-и-платформы)
3. [Аппаратные архитектуры](#аппаратные-архитектуры)
4. [Операционные системы](#операционные-системы)
5. [Форматы пакетов и установки](#форматы-пакетов-и-установки)
6. [Программные реализации](#программные-реализации)
7. [План реализации поддержки NAS](#план-реализации-поддержки-nas)
8. [Рекомендации по архитектуре](#рекомендации-по-архитектуре)

---

## Обзор рынка NAS

### Распределение рынка (2024)

| Производитель | Доля рынка | Основные модели | Целевая аудитория |
|---------------|------------|-----------------|-------------------|
| **Synology** | ~35% | DS220+, DS920+, DS1621+, RS series | Домашние пользователи, SMB, Enterprise |
| **QNAP** | ~28% | TS-251+, TS-453D, TS-h886, TVS series | Энтузиасты, SMB, Enterprise |
| **Asustor** | ~12% | AS5304T, AS6508T, AS6004U | Домашние пользователи, энтузиасты |
| **TrueNAS (iXsystems)** | ~8% | TrueNAS Mini, TrueNAS Enterprise | Enterprise, серверы |
| **Terramaster** | ~5% | F2-210, F4-423, T9-450 | Домашние пользователи, бюджетный сегмент |
| **Другие (Thecus, Netgear, WD, Seagate)** | ~12% | Различные | Различные сегменты |

### Тренды рынка

- **Растущий спрос на домашние NAS** для видеонаблюдения и резервного копирования
- **Интеграция с облачными сервисами** и гибридное хранение
- **Развитие AI функций** на NAS (распознавание лиц, детекция объектов)
- **Расширение возможностей виртуализации** (Docker, Kubernetes, виртуальные машины)
- **Поддержка аппаратного ускорения** видео (Quick Sync, NPU)

---

## Производители и платформы

### 1. Synology

#### История и позиционирование
- Основана: 2000 год
- Штаб-квартира: Тайвань
- Позиционирование: Premium сегмент, дружелюбный интерфейс
- ОС: DSM (DiskStation Manager)

#### Линейки продуктов

**Домашние / SOHO:**
- **DS220+** (Intel Celeron J4025, 2 ГБ RAM, 2 слота)
- **DS420+** (Intel Celeron J4025, 4 ГБ RAM, 4 слота)
- **DS920+** (Intel Celeron J4125, 4 ГБ RAM, 4 слота, расширение до 9)

**SMB (Small/Medium Business):**
- **DS1621+** (AMD Ryzen V1500B, 4 ГБ RAM, 6 слотов, 10GbE)
- **DS1821+** (AMD Ryzen V1500B, 4 ГБ RAM, 8 слотов)
- **DS2422+** (Intel Xeon D-1531, 4 ГБ RAM, 12 слотов)

**Enterprise:**
- **RS3621RPxs** (Rack-mount, Intel Xeon, 12 слотов)
- **RS4021xs+** (Rack-mount, Intel Xeon, 24 слота)

#### Особенности платформы

**DSM (DiskStation Manager):**
- Основан на: Linux (Debian-based)
- Версия ядра: 4.4.x / 5.10.x (зависит от модели)
- Архитектуры: x86_64, ARMv7, ARMv8 (aarch64)
- Пакетный менеджер: Synology Package Manager (.spk)
- Язык разработки: Python, Bash, Node.js (для пакетов)
- API: Synology API, RESTful API

**Технические характеристики:**
- Поддержка Docker ✅
- Поддержка Kubernetes ✅ (на некоторых моделях)
- Виртуализация: Virtual Machine Manager
- Файловые системы: Btrfs, ext4
- Протоколы: SMB, NFS, AFP, FTP, SFTP, WebDAV
- Видео: Surveillance Station (проприетарное ПО для камер)

**Требования для разработки:**
- Synology Developer SDK
- GPL Source Code (для некоторых компонентов)
- Зависимости: Python 3.x, Node.js 14+

#### Формат пакетов: `.spk`

Структура SPK пакета:
```
package.spk
├── INFO           # Метаданные пакета
├── package.tgz    # Архив с файлами
│   ├── bin/       # Исполняемые файлы
│   ├── lib/       # Библиотеки
│   ├── web/       # Веб-интерфейс
│   ├── scripts/   # Скрипты установки/удаления
│   └── conf/      # Конфигурационные файлы
└── scripts/       # Pre/Post install скрипты
```

Пример INFO файла:
```ini
package="ip-css"
version="Alfa-0.0.1"
displayname="IP Camera Surveillance System"
arch="x86_64"
maintainer="Company"
description="Кроссплатформенная система видеонаблюдения"
```

---

### 2. QNAP

#### История и позиционирование
- Основана: 2004 год
- Штаб-квартира: Тайвань
- Позиционирование: Мощные функции, гибкость настроек
- ОС: QTS (QNAP Turbo Station)

#### Линейки продуктов

**Домашние / SOHO:**
- **TS-251+** (Intel Celeron J4125, 4 ГБ RAM, 2 слота)
- **TS-453D** (Intel Celeron J4125, 4 ГБ RAM, 4 слота)
- **TS-451D2** (Intel Celeron J4025, 4 ГБ RAM, 4 слота)

**SMB:**
- **TS-673A** (AMD Ryzen V1500B, 8 ГБ RAM, 6 слотов, 2.5GbE)
- **TS-h886** (Intel Core i5, 16 ГБ RAM, 8 слотов, 10GbE)
- **TVS-672XT** (Intel Core i5, 16 ГБ RAM, 6 слотов, Thunderbolt 3)

**Enterprise:**
- **TVS-h1288X** (Intel Core i9, 64 ГБ RAM, 12 слотов, 10GbE)
- **TS-1677XU-RP** (Rack-mount, AMD Ryzen, 16 слотов)

#### Особенности платформы

**QTS (QNAP Turbo Station):**
- Основан на: Linux (CentOS/RHEL-based)
- Версия ядра: 4.14.x / 5.10.x
- Архитектуры: x86_64, ARMv7, ARMv8 (aarch64)
- Пакетный менеджер: QNAP App Center (.qpkg)
- Язык разработки: Python, Bash, Node.js, PHP
- API: QNAP API, RESTful API

**Технические характеристики:**
- Поддержка Docker ✅
- Поддержка Kubernetes ✅ (QKVS - QNAP Kubernetes Virtualization Station)
- Виртуализация: Virtualization Station
- Файловые системы: ZFS (QTS Hero), ext4
- Протоколы: SMB, NFS, AFP, FTP, SFTP, WebDAV, iSCSI
- Видео: QVR Pro (проприетарное ПО для камер)
- Container Station: Docker, LXC, Kata Containers

**Требования для разработки:**
- QNAP Developer SDK
- QPKG Tool (для создания пакетов)
- Зависимости: Python 3.x, Node.js 16+, PHP 7.4+

#### Формат пакетов: `.qpkg`

Структура QPKG пакета:
```
package.qpkg
├── QPKG.INFO       # Метаданные пакета
├── package.tgz     # Архив с файлами
│   ├── bin/        # Исполняемые файлы
│   ├── lib/        # Библиотеки
│   ├── web/        # Веб-интерфейс
│   ├── scripts/    # Скрипты
│   └── conf/       # Конфигурационные файлы
└── scripts/        # Pre/Post install скрипты
```

Пример QPKG.INFO:
```xml
<?xml version="1.0" encoding="UTF-8"?>
<QPKG>
  <Name>ip-css</Name>
  <Version>Alfa-0.0.1</Version>
  <DisplayName>IP Camera Surveillance System</DisplayName>
  <Architecture>x86_64</Architecture>
  <Author>Company</Author>
  <Description>Кроссплатформенная система видеонаблюдения</Description>
  <InstallPath>/share/CACHEDEV1_DATA/.qpkg/ip-css</InstallPath>
</QPKG>
```

---

### 3. Asustor

#### История и позиционирование
- Основана: 2011 год (дочерняя компания ASUS)
- Штаб-квартира: Тайвань
- Позиционирование: Доступные цены, богатый функционал
- ОС: ADM (Asustor Data Master)

#### Линейки продуктов

**Домашние:**
- **AS5304T** (Intel Celeron J4125, 4 ГБ RAM, 4 слота)
- **AS6604T** (Intel Celeron J4125, 4 ГБ RAM, 4 слота, 2.5GbE)
- **AS5202T** (Realtek RTD1296 ARM, 2 ГБ RAM, 2 слота)

**SMB:**
- **AS6508T** (Intel Celeron J4125, 8 ГБ RAM, 8 слотов)
- **AS6210T** (Intel Atom C3538, 4 ГБ RAM, 10 слотов)

#### Особенности платформы

**ADM (Asustor Data Master):**
- Основан на: Linux (Debian-based)
- Версия ядра: 4.4.x / 5.10.x
- Архитектуры: x86_64, ARMv8 (aarch64), Realtek RTD1296
- Пакетный менеджер: Asustor App Central (.apk - не Android!)
- Язык разработки: Python, Bash, Node.js
- API: Asustor API, RESTful API

**Технические характеристики:**
- Поддержка Docker ✅
- Поддержка Kubernetes ❌ (планируется)
- Виртуализация: VirtualBox
- Файловые системы: Btrfs, ext4
- Протоколы: SMB, NFS, AFP, FTP, SFTP, WebDAV
- Видео: Surveillance Center (базовое ПО для камер)

**Требования для разработки:**
- Asustor Developer SDK
- ADM Toolkit
- Зависимости: Python 3.x, Node.js 14+

#### Формат пакетов: `.apk` (Asustor Package)

Структура APK пакета (Asustor):
```
package.apk
├── INFO           # Метаданные пакета
├── package.tgz    # Архив с файлами
└── scripts/       # Скрипты установки
```

---

### 4. TrueNAS (iXsystems)

#### История и позиционирование
- Разработчик: iXsystems (основана 1996)
- Штаб-квартира: США
- Позиционирование: Enterprise, open-source (TrueNAS CORE), коммерческая (TrueNAS SCALE)
- ОС: TrueNAS CORE (FreeBSD) / TrueNAS SCALE (Linux)

#### Линейки продуктов

**TrueNAS Mini:**
- **Mini XL+** (Intel Xeon D-1528, 32 ГБ RAM, 12 слотов)
- **Mini X+** (Intel Xeon D-1541, 64 ГБ RAM, 8 слотов)

**Enterprise:**
- **TrueNAS M60** (High-density storage)
- **TrueNAS R50** (Rack-mount, 60 слотов)

#### Особенности платформы

**TrueNAS CORE:**
- Основан на: **FreeBSD** 13.x
- Файловая система: **ZFS** (основная)
- Архитектуры: x86_64
- Пакетный менеджер: FreeBSD pkg, Jails, Docker (ограниченно)
- API: FreeNAS API v2.0, RESTful API

**TrueNAS SCALE:**
- Основан на: **Linux (Debian-based)**
- Файловая система: **ZFS**
- Архитектуры: x86_64
- Пакетный менеджер: Kubernetes, Docker, Helm charts
- API: RESTful API, Kubernetes API

**Технические характеристики:**

**TrueNAS CORE (FreeBSD):**
- Поддержка Docker: ❌ (через Jails)
- Поддержка Kubernetes: ❌
- Виртуализация: Bhyve, Jails
- Протоколы: SMB, NFS, AFP, FTP, SFTP, iSCSI
- Видео: Нет встроенного ПО, но можно установить стороннее

**TrueNAS SCALE (Linux):**
- Поддержка Docker: ✅
- Поддержка Kubernetes: ✅ (встроенный)
- Виртуализация: KVM
- Протоколы: SMB, NFS, AFP, FTP, SFTP, iSCSI
- Видео: Можно развернуть через Docker/Kubernetes

**Требования для разработки:**

**TrueNAS CORE:**
- FreeBSD SDK
- Jail упаковка или App Jail
- Зависимости: через FreeBSD pkg

**TrueNAS SCALE:**
- Docker образы или Helm charts
- Kubernetes манифесты
- Linux-совместимые зависимости

**Форматы пакетов:**
- **TrueNAS CORE**: Jails (.pbi), FreeBSD пакеты
- **TrueNAS SCALE**: Docker образы, Helm charts, Kubernetes манифесты

---

### 5. Terramaster

#### Особенности платформы

**TOS (TerraMaster Operating System):**
- Основан на: Linux (Debian-based)
- Версия ядра: 4.4.x
- Архитектуры: x86_64, ARMv7, ARMv8
- Пакетный менеджер: TerraMaster App Store (.tpp)
- API: TerraMaster API

**Технические характеристики:**
- Поддержка Docker: ✅ (на некоторых моделях)
- Файловые системы: Btrfs, ext4
- Протоколы: SMB, NFS, FTP, SFTP

---

### 6. Другие производители

#### Netgear ReadyNAS
- ОС: ReadyNAS OS (Linux-based)
- Формат: .deb пакеты или Docker

#### Western Digital (WD) My Cloud
- ОС: My Cloud OS (Linux-based)
- Формат: Проприетарные пакеты или Docker

#### Seagate Personal Cloud
- ОС: Linux-based
- Формат: Ограниченная поддержка пакетов

---

## Аппаратные архитектуры

### x86_64 (Intel/AMD)

**Характеристики:**
- Процессоры: Intel Celeron (J4025, J4125), Intel Core (i3, i5, i7), AMD Ryzen (V1500B, V1780B), Intel Xeon
- Память: DDR4, обычно 2-64 ГБ
- Быстродействие: Высокое
- Поддержка аппаратного ускорения: Intel Quick Sync Video, AMD VCE/VCN

**Поддерживаемые платформы:**
- ✅ Synology (DS920+, DS1621+, RS series)
- ✅ QNAP (TS-453D, TS-673A, TVS series)
- ✅ Asustor (AS5304T, AS6508T)
- ✅ TrueNAS (все модели)
- ✅ Terramaster (F4-423, T9-450)

**Рекомендации для IP-CSS:**
- Использовать аппаратное декодирование H.264/H.265 через Quick Sync или VCE
- Многопоточная обработка видео
- Поддержка больших объемов памяти для кэширования

### ARMv7 (32-bit ARM)

**Характеристики:**
- Процессоры: ARM Cortex-A7, Cortex-A9, Cortex-A15
- Память: DDR3/DDR4, обычно 1-4 ГБ
- Быстродействие: Среднее
- Поддержка аппаратного ускорения: Ограниченная

**Поддерживаемые платформы:**
- ✅ Synology (старые модели DS215j, DS216j)
- ✅ QNAP (старые модели TS-231+, TS-431+)
- ⚠️ Asustor (старые модели)

**Рекомендации для IP-CSS:**
- Базовая поддержка (без аппаратного ускорения)
- Ограниченное количество одновременных потоков
- Возможна задержка при обработке видео

### ARMv8 / aarch64 (64-bit ARM)

**Характеристики:**
- Процессоры: ARM Cortex-A53, Cortex-A55, Cortex-A72, Cortex-A76, Cortex-A78
- Память: DDR4, обычно 2-16 ГБ
- Быстродействие: Среднее-Высокое
- Поддержка аппаратного ускорения: Mali, VideoCore (на некоторых моделях)

**Поддерживаемые платформы:**
- ✅ Synology (DS220j, DS420j, некоторые RS модели)
- ✅ QNAP (TS-231P3, TS-332X)
- ✅ Asustor (AS5202T - Realtek RTD1296)
- ❌ TrueNAS (только x86_64)
- ✅ Terramaster (F2-210, F2-220)

**Рекомендации для IP-CSS:**
- Поддержка ARM64 нативных библиотек
- Оптимизация для ARM процессоров
- Возможна поддержка аппаратного ускорения на некоторых платформах (Mali, VideoCore)

### Realtek RTD1296 (ARM Cortex-A53)

**Характеристики:**
- Процессоры: Realtek RTD1296 (4x Cortex-A53 @ 1.4 GHz)
- Память: DDR4, обычно 2-4 ГБ
- Быстродействие: Среднее
- Поддержка аппаратного ускорения: Встроенный VPU

**Поддерживаемые платформы:**
- ✅ Asustor (AS5202T, AS5304T некоторые модели)

**Рекомендации для IP-CSS:**
- Специальная оптимизация для Realtek VPU
- Поддержка аппаратного декодирования H.264

---

## Операционные системы

### Linux-based системы

#### 1. Synology DSM

**Основа:** Debian-based Linux
**Версия ядра:** 4.4.x (старые модели), 5.10.x (новые модели)
**Архитектуры:** x86_64, ARMv7, ARMv8

**Особенности:**
- Закрытый исходный код (за исключением GPL компонентов)
- Управление через веб-интерфейс
- Пакетный менеджер: Synology Package Center
- Поддержка Docker: ✅
- Поддержка SSH: ✅ (требует активации)

**Разработка для DSM:**
- Synology Developer SDK
- Python 3.x, Node.js, Bash
- SPK Builder Tool

**Ограничения:**
- Ограниченный доступ к системным библиотекам
- Требуется сертификация для официального магазина пакетов
- Некоторые API доступны только для официальных приложений

#### 2. QNAP QTS

**Основа:** CentOS/RHEL-based Linux
**Версия ядра:** 4.14.x (старые модели), 5.10.x (новые модели)
**Архитектуры:** x86_64, ARMv7, ARMv8

**Особенности:**
- Открытый исходный код ядра (GPL)
- Управление через веб-интерфейс
- Пакетный менеджер: QNAP App Center
- Поддержка Docker: ✅
- Поддержка Kubernetes: ✅ (QKVS)
- Поддержка SSH: ✅

**Разработка для QTS:**
- QNAP Developer SDK
- Python 3.x, Node.js, PHP, Bash
- QPKG Tool

**Ограничения:**
- Требуется сертификация для официального магазина
- Некоторые системные ресурсы ограничены

#### 3. Asustor ADM

**Основа:** Debian-based Linux
**Версия ядра:** 4.4.x (старые модели), 5.10.x (новые модели)
**Архитектуры:** x86_64, ARMv8, Realtek RTD1296

**Особенности:**
- Открытый исходный код (GPL компоненты)
- Управление через веб-интерфейс
- Пакетный менеджер: Asustor App Central
- Поддержка Docker: ✅
- Поддержка SSH: ✅

**Разработка для ADM:**
- Asustor Developer SDK
- Python 3.x, Node.js, Bash
- ADM Toolkit

**Ограничения:**
- Меньше пользователей, чем Synology/QNAP
- Ограниченная документация

#### 4. Terramaster TOS

**Основа:** Debian-based Linux
**Версия ядра:** 4.4.x
**Архитектуры:** x86_64, ARMv7, ARMv8

**Особенности:**
- Простой интерфейс
- Пакетный менеджер: TerraMaster App Store
- Поддержка Docker: ✅ (на некоторых моделях)
- Поддержка SSH: ✅

**Разработка для TOS:**
- TerraMaster Developer SDK (ограниченный)
- Python 3.x, Node.js, Bash

**Ограничения:**
- Ограниченная документация
- Меньше возможностей для разработки

### FreeBSD-based системы

#### 5. TrueNAS CORE

**Основа:** FreeBSD 13.x
**Архитектуры:** x86_64

**Особенности:**
- Полностью открытый исходный код
- ZFS как основная файловая система
- Управление через веб-интерфейс
- Пакетный менеджер: FreeBSD pkg, Jails
- Поддержка Docker: ❌ (только через Jails)
- Поддержка SSH: ✅

**Разработка для TrueNAS CORE:**
- FreeBSD SDK
- Jail упаковка
- FreeBSD пакеты

**Ограничения:**
- Отличная от Linux экосистема
- Меньше готовых библиотек
- Требуется портирование Linux-приложений

### Linux-based (Kubernetes-focused)

#### 6. TrueNAS SCALE

**Основа:** Debian-based Linux с Kubernetes
**Архитектуры:** x86_64

**Особенности:**
- Открытый исходный код
- ZFS файловая система
- Встроенный Kubernetes
- Управление через веб-интерфейс и kubectl
- Поддержка Docker: ✅
- Поддержка Helm: ✅
- Поддержка SSH: ✅

**Разработка для TrueNAS SCALE:**
- Docker образы
- Helm charts
- Kubernetes манифесты

**Преимущества:**
- Современная платформа для контейнеризации
- Легкое развертывание через Kubernetes
- Горизонтальное масштабирование

---

## Форматы пакетов и установки

### 1. Synology SPK (.spk)

**Структура:**
```
package.spk
├── INFO              # Метаданные (обязательно)
├── package.tgz       # Архив с приложением
├── scripts/          # Pre/Post install скрипты
│   ├── preinst
│   ├── postinst
│   ├── preuninst
│   └── postuninst
└── PACKAGE_ICON.PNG  # Иконка (опционально)
```

**INFO файл:**
```ini
package="ip-css"
version="Alfa-0.0.1"
arch="x86_64 bromolow"
displayname="IP Camera Surveillance System"
description="Кроссплатформенная система видеонаблюдения с AI аналитикой"
maintainer="Company"
distributor="Company"
distributor_url="https://company.com"
support_url="https://support.company.com"
startable="yes"
```

**Архитектуры (arch):**
- `x86_64 bromolow` - Intel/AMD 64-bit (старые модели)
- `x86_64 apollolake` - Intel Celeron J3355/J3455
- `x86_64 geminilake` - Intel Celeron J4005/J4105/J5005
- `x86_64 denverton` - Intel Atom C3538
- `x86_64 v1000` - AMD Ryzen V1000
- `x86_64 broadwell` - Intel Broadwell
- `x86_64 broadwellnk` - Intel Broadwell (новые модели)
- `aarch64 rtd1296` - Realtek RTD1296
- `armv7 alpine` - ARMv7 (старые модели)

**Создание SPK:**
```bash
# Использование Synology SDK
synology-packager create \
  --name ip-css \
  --version Alfa-0.0.1 \
  --arch x86_64 \
  --package-dir ./package \
  --output ./ip-css.spk
```

### 2. QNAP QPKG (.qpkg)

**Структура:**
```
package.qpkg
├── QPKG.INFO        # Метаданные (обязательно)
├── package.tgz      # Архив с приложением
├── scripts/         # Скрипты установки
│   ├── init.sh      # Инициализация
│   ├── start.sh     # Запуск
│   ├── stop.sh      # Остановка
│   └── uninstall.sh # Удаление
└── icons/           # Иконки
    └── icon_80.png
```

**QPKG.INFO файл:**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<QPKG>
  <Name>ip-css</Name>
  <Version>Alfa-0.0.1</Version>
  <DisplayName>IP Camera Surveillance System</DisplayName>
  <Architecture>x86_64</Architecture>
  <Author>Company</Author>
  <Description>Кроссплатформенная система видеонаблюдения</Description>
  <InstallPath>/share/CACHEDEV1_DATA/.qpkg/ip-css</InstallPath>
  <DefaultPage>index.html</DefaultPage>
</QPKG>
```

**Архитектуры:**
- `x86_64` - Intel/AMD 64-bit
- `arm_64` - ARM 64-bit
- `arm_x31` - ARM 32-bit (старые модели)

**Создание QPKG:**
```bash
# Использование QPKG Tool
qbuild --create ip-css \
  --version Alfa-0.0.1 \
  --arch x86_64 \
  --source ./source \
  --output ./ip-css.qpkg
```

### 3. Asustor APK (.apk)

**Структура:**
```
package.apk
├── INFO            # Метаданные
├── package.tgz     # Архив с приложением
└── scripts/        # Скрипты
```

**INFO файл:**
```ini
package="ip-css"
version="Alfa-0.0.1"
displayname="IP Camera Surveillance System"
architecture="x86_64"
```

**Создание APK:**
```bash
# Использование ADM Toolkit
adm-packager create \
  --name ip-css \
  --version Alfa-0.0.1 \
  --arch x86_64 \
  --package-dir ./package \
  --output ./ip-css.apk
```

### 4. TrueNAS

**TrueNAS CORE (FreeBSD Jails):**

Создание Jail:
```bash
# Создание Jail образа
pkg create -a
tar -czf ip-css-jail.tar.gz ip-css/
```

**TrueNAS SCALE (Docker/Kubernetes):**

**Docker Compose:**
```yaml
version: '3.8'
services:
  ip-css:
    image: company/ip-css:Alfa-0.0.1
    ports:
      - "8080:8080"
    volumes:
      - /mnt/tank/recordings:/app/recordings
      - /mnt/tank/config:/app/config
```

**Helm Chart:**
```yaml
# values.yaml
image:
  repository: company/ip-css
  tag: "Alfa-0.0.1"

persistence:
  enabled: true
  storageClass: "zfs-storage"
  size: 100Gi
```

### 5. Универсальный подход: Docker

**Преимущества:**
- ✅ Работает на всех платформах с поддержкой Docker
- ✅ Единая сборка для всех архитектур (multi-arch)
- ✅ Изоляция зависимостей
- ✅ Легкое обновление

**Недостатки:**
- ❌ Требует Docker на целевой системе
- ❌ Может быть тяжелее нативных пакетов
- ❌ Меньше интеграции с системой NAS

**Multi-arch Docker образ:**
```dockerfile
# Dockerfile
FROM --platform=$BUILDPLATFORM company/base:latest AS builder
# ... сборка ...

FROM --platform=$TARGETPLATFORM company/runtime:latest
COPY --from=builder /app /app
```

**Создание multi-arch образа:**
```bash
docker buildx build \
  --platform linux/amd64,linux/arm64 \
  -t company/ip-css:Alfa-0.0.1 \
  --push .
```

---

## Программные реализации

### Общая архитектура для NAS

```
┌─────────────────────────────────────────┐
│         NAS Operating System            │
│  (DSM/QTS/ADM/TrueNAS/TOS)             │
├─────────────────────────────────────────┤
│                                         │
│  ┌───────────────────────────────────┐ │
│  │   IP-CSS Package/Container        │ │
│  │                                   │ │
│  │  ┌─────────────────────────────┐  │ │
│  │  │   Web UI (Next.js)          │  │ │
│  │  │   Port: 8080                │  │ │
│  │  └─────────────────────────────┘  │ │
│  │                                   │ │
│  │  ┌─────────────────────────────┐  │ │
│  │  │   API Server (Ktor)         │  │ │
│  │  │   Port: 8081                │  │ │
│  │  └─────────────────────────────┘  │ │
│  │                                   │ │
│  │  ┌─────────────────────────────┐  │ │
│  │  │   Shared Module (KMP)       │  │ │
│  │  │   - Business Logic          │  │ │
│  │  │   - Repositories            │  │ │
│  │  │   - Use Cases               │  │ │
│  │  └─────────────────────────────┘  │ │
│  │                                   │ │
│  │  ┌─────────────────────────────┐  │ │
│  │  │   Native Libraries (C++)    │  │ │
│  │  │   - Video Processing        │  │ │
│  │  │   - Analytics               │  │ │
│  │  └─────────────────────────────┘  │ │
│  │                                   │ │
│  │  ┌─────────────────────────────┐  │ │
│  │  │   Database (SQLite)         │  │ │
│  │  │   Location: /data/db/       │  │ │
│  │  └─────────────────────────────┘  │ │
│  │                                   │ │
│  │  ┌─────────────────────────────┐  │ │
│  │  │   Storage (Recordings)      │  │ │
│  │  │   Location: /recordings/    │  │ │
│  │  └─────────────────────────────┘  │ │
│  └───────────────────────────────────┘ │
│                                         │
└─────────────────────────────────────────┘
```

### Реализация для каждой платформы

#### 1. Synology DSM (SPK)

**Структура пакета:**
```
ip-css/
├── bin/
│   ├── ip-css-server       # JVM сервер
│   └── ip-css-web          # Node.js веб-сервер
├── lib/
│   ├── shared.jar          # Kotlin Multiplatform
│   ├── server.jar          # Ktor сервер
│   └── native/             # Нативные библиотеки
│       ├── libvideo.so     # x86_64
│       └── libanalytics.so
├── web/
│   └── dist/               # Next.js build
├── data/
│   ├── db/                 # SQLite база данных
│   └── recordings/         # Видео записи
├── scripts/
│   ├── start.sh
│   └── stop.sh
└── conf/
    └── config.yaml
```

**Скрипт запуска (start.sh):**
```bash
#!/bin/bash

INSTALL_DIR="/var/packages/ip-css/target"
PID_FILE="/var/run/ip-css.pid"

# Запуск API сервера
java -jar "$INSTALL_DIR/lib/server.jar" \
  --config "$INSTALL_DIR/conf/config.yaml" \
  --pid-file "$PID_FILE" &

# Запуск веб-сервера
cd "$INSTALL_DIR/web/dist"
node server.js &

echo $! > "$PID_FILE"
```

#### 2. QNAP QTS (QPKG)

**Структура аналогична SPK, но с QPKG.INFO**

**scripts/start.sh:**
```bash
#!/bin/sh

INSTALL_DIR="/share/CACHEDEV1_DATA/.qpkg/ip-css"

# Запуск через QNAP service framework
$INSTALL_DIR/bin/ip-css-server start
```

#### 3. Asustor ADM (APK)

**Структура аналогична SPK, но с APK INFO**

#### 4. TrueNAS SCALE (Docker/Kubernetes)

**Docker Compose:**
```yaml
version: '3.8'

services:
  ip-css-api:
    image: company/ip-css-api:Alfa-0.0.1
    platform: linux/amd64  # или linux/arm64
    ports:
      - "8081:8081"
    volumes:
      - ip-css-db:/app/db
      - ip-css-recordings:/app/recordings
      - ip-css-config:/app/config
    environment:
      - JAVA_OPTS=-Xmx2g
    restart: unless-stopped

  ip-css-web:
    image: company/ip-css-web:Alfa-0.0.1
    platform: linux/amd64  # или linux/arm64
    ports:
      - "8080:8080"
    depends_on:
      - ip-css-api
    environment:
      - API_URL=http://ip-css-api:8081
    restart: unless-stopped

volumes:
  ip-css-db:
    driver: local
  ip-css-recordings:
    driver: local
  ip-css-config:
    driver: local
```

**Kubernetes Deployment:**
```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: ip-css
spec:
  replicas: 1
  selector:
    matchLabels:
      app: ip-css
  template:
    metadata:
      labels:
        app: ip-css
    spec:
      containers:
      - name: api
        image: company/ip-css-api:Alfa-0.0.1
        ports:
        - containerPort: 8081
        volumeMounts:
        - name: recordings
          mountPath: /app/recordings
      - name: web
        image: company/ip-css-web:Alfa-0.0.1
        ports:
        - containerPort: 8080
      volumes:
      - name: recordings
        persistentVolumeClaim:
          claimName: ip-css-recordings
```

---

## План реализации поддержки NAS

### Фаза 1: Подготовка инфраструктуры (2 недели)

#### 1.1 Настройка сборки для NAS

**Задачи:**
- [ ] Создать модуль `:server:nas` в Gradle
- [ ] Настроить multi-arch сборку (x86_64, ARM64)
- [ ] Создать Docker образы для каждой архитектуры
- [ ] Настроить CI/CD для автоматической сборки

**Структура модуля:**
```
server/nas/
├── build.gradle.kts
├── src/
│   └── main/
│       ├── resources/
│       │   ├── spk/        # Synology SPK файлы
│       │   ├── qpkg/       # QNAP QPKG файлы
│       │   ├── apk/        # Asustor APK файлы
│       │   └── docker/     # Docker файлы
│       └── scripts/
│           ├── build-spk.sh
│           ├── build-qpkg.sh
│           ├── build-apk.sh
│           └── build-docker.sh
```

#### 1.2 Создание базовых пакетов

**Задачи:**
- [ ] SPK пакет для Synology (x86_64, ARM64)
- [ ] QPKG пакет для QNAP (x86_64, ARM64)
- [ ] APK пакет для Asustor (x86_64, ARM64)
- [ ] Docker образы для TrueNAS SCALE

### Фаза 2: Разработка интеграции (3 недели)

#### 2.1 Интеграция с системами NAS

**Synology:**
- [ ] Интеграция с Synology API (если необходимо)
- [ ] Использование системных путей для данных
- [ ] Интеграция с Synology Notification Center

**QNAP:**
- [ ] Интеграция с QNAP API (если необходимо)
- [ ] Использование системных путей для данных

**Asustor:**
- [ ] Интеграция с Asustor API (если необходимо)

**TrueNAS:**
- [ ] Оптимизация для Kubernetes
- [ ] Интеграция с ZFS snapshots (для резервного копирования)

#### 2.2 Аппаратное ускорение

**Задачи:**
- [ ] Определение доступности аппаратного ускорения
- [ ] Использование Intel Quick Sync (x86_64)
- [ ] Использование AMD VCE/VCN (x86_64)
- [ ] Fallback на программное декодирование

#### 2.3 Управление ресурсами

**Задачи:**
- [ ] Ограничение использования CPU/RAM
- [ ] Приоритизация процессов
- [ ] Мониторинг ресурсов
- [ ] Автоматическое масштабирование (для Kubernetes)

### Фаза 3: Тестирование и оптимизация (2 недели)

#### 3.1 Тестирование на реальном оборудовании

**Тестовые устройства:**
- [ ] Synology DS920+ (x86_64)
- [ ] Synology DS220j (ARM64)
- [ ] QNAP TS-453D (x86_64)
- [ ] Asustor AS5304T (x86_64)
- [ ] TrueNAS SCALE (x86_64)

#### 3.2 Оптимизация производительности

**Задачи:**
- [ ] Оптимизация использования памяти
- [ ] Оптимизация I/O операций
- [ ] Оптимизация сетевых операций
- [ ] Профилирование и устранение узких мест

#### 3.3 Документация

**Задачи:**
- [ ] Руководство по установке для каждой платформы
- [ ] Руководство по настройке
- [ ] Руководство по устранению неполадок
- [ ] FAQ по каждой платформе

### Фаза 4: Публикация и поддержка (постоянно)

#### 4.1 Публикация пакетов

**Задачи:**
- [ ] Загрузка в Synology Package Center (требует сертификации)
- [ ] Загрузка в QNAP App Center (требует сертификации)
- [ ] Загрузка в Asustor App Central
- [ ] Публикация Docker образов в Docker Hub
- [ ] Публикация Helm charts

#### 4.2 Поддержка пользователей

**Задачи:**
- [ ] Форум поддержки для каждой платформы
- [ ] Документация по обновлению
- [ ] Скрипты автоматического обновления
- [ ] Мониторинг ошибок и сбор обратной связи

---

## Рекомендации по архитектуре

### 1. Единая кодовая база

**Принцип:** Максимальная переиспользование кода между платформами

**Реализация:**
- Использование Kotlin Multiplatform для бизнес-логики
- Использование Docker для универсального развертывания
- Единые конфигурационные файлы
- Адаптеры для платформо-специфичных API

### 2. Модульная архитектура

**Компоненты:**
- **API Server** - Ktor сервер (JVM)
- **Web UI** - Next.js приложение (Node.js)
- **Shared Module** - Kotlin Multiplatform
- **Native Libraries** - C++ библиотеки (FFI)

**Преимущества:**
- Независимое развертывание компонентов
- Легкое масштабирование
- Возможность замены компонентов

### 3. Аппаратная абстракция

**Слой абстракции:**
```kotlin
interface HardwareAccelerator {
    suspend fun decodeH264(data: ByteArray): Frame
    suspend fun decodeH265(data: ByteArray): Frame
    suspend fun encodeH264(frame: Frame): ByteArray
}

// Реализации
class IntelQuickSyncAccelerator : HardwareAccelerator { ... }
class AMDVCAAccelerator : HardwareAccelerator { ... }
class SoftwareAccelerator : HardwareAccelerator { ... }

// Фабрика
object AcceleratorFactory {
    fun create(): HardwareAccelerator {
        return when {
            hasIntelQuickSync() -> IntelQuickSyncAccelerator()
            hasAMDVCA() -> AMDVCAAccelerator()
            else -> SoftwareAccelerator()
        }
    }
}
```

### 4. Хранение данных

**Рекомендации:**
- База данных SQLite в `/data/db/` (легко переносима)
- Видео записи в `/recordings/` (системный путь NAS)
- Конфигурация в `/config/` (системный путь NAS)
- Логи в `/var/log/ip-css/` (системный путь)

**Использование системных путей:**
- Synology: `/volume1/ip-css/`
- QNAP: `/share/CACHEDEV1_DATA/ip-css/`
- Asustor: `/volume1/ip-css/`
- TrueNAS: `/mnt/tank/ip-css/`

### 5. Мониторинг и логирование

**Рекомендации:**
- Структурированное логирование (JSON)
- Интеграция с системными логами NAS
- Метрики производительности
- Health checks для Docker/Kubernetes

### 6. Безопасность

**Рекомендации:**
- Использование системных пользователей NAS
- Ограничение прав доступа
- Шифрование данных в покое
- TLS для веб-интерфейса и API
- Регулярные обновления безопасности

---

## Выводы и следующие шаги

### Приоритеты

1. **Высокий приоритет:**
   - Synology (DSM) - наибольшая доля рынка
   - QNAP (QTS) - вторая по популярности
   - Docker универсальное решение

2. **Средний приоритет:**
   - Asustor (ADM)
   - TrueNAS SCALE (Kubernetes)

3. **Низкий приоритет:**
   - TrueNAS CORE (FreeBSD, меньшая доля рынка)
   - Terramaster и другие

### Рекомендуемый подход

1. **Начать с Docker** - работает на всех платформах с Docker
2. **Добавить нативные пакеты** - для лучшей интеграции
3. **Оптимизировать под каждую платформу** - используя специфичные возможности

### Метрики успеха

- ✅ Поддержка всех основных платформ NAS
- ✅ Работа на x86_64 и ARM64
- ✅ Использование аппаратного ускорения где возможно
- ✅ Легкая установка и настройка
- ✅ Хорошая производительность на бюджетных моделях

---

**Последнее обновление:** Декабрь 2025
**Следующий пересмотр:** После реализации Фазы 1



