# NAS Packages для ARM

Эта директория содержит пакеты для различных NAS систем на архитектуре ARM (ARMv8/aarch64).

## Структура

```
packages/
├── synology/      # Synology DSM пакеты (.spk)
├── qnap/          # QNAP QTS пакеты (.qpkg)
└── asustor/       # Asustor ADM пакеты (.apk)
```

## Сборка пакетов

### Использование скрипта

```bash
# Synology SPK
./scripts/build-nas-package.sh synology arm64 Alfa-0.0.1

# QNAP QPKG
./scripts/build-nas-package.sh qnap arm64 Alfa-0.0.1

# Asustor APK
./scripts/build-nas-package.sh asustor arm64 Alfa-0.0.1
```

### Использование Gradle

```bash
# Конкретные пакеты
./gradlew buildNasPackageSynologyArm
./gradlew buildNasPackageQnapArm
./gradlew buildNasPackageAsustorArm
```

## Установка

### Synology DSM

1. Откройте Package Center
2. Нажмите "Ручная установка"
3. Выберите файл `.spk`
4. Следуйте инструкциям установки

### QNAP QTS

1. Откройте App Center
2. Нажмите "Установить вручную"
3. Выберите файл `.qpkg`
4. Следуйте инструкциям установки

### Asustor ADM

1. Откройте App Central
2. Нажмите "Ручная установка"
3. Выберите файл `.apk`
4. Следуйте инструкциям установки

## Доступ

После установки:

- **Web UI:** http://your-nas-ip:8080
- **API:** http://your-nas-ip:8081

## Требования

- Java 11 или выше
- Node.js 18+ (для веб-интерфейса)
- Минимум 500MB свободного места
- Минимум 2GB RAM

## Документация

Подробная документация по NAS платформам: [docs/NAS_PLATFORMS_ANALYSIS.md](../../../docs/NAS_PLATFORMS_ANALYSIS.md)


