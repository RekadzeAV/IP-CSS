# Реализация 4.1: Синхронизация, облачное хранилище, резервное копирование

**Дата:** 1 March 2026

---

## 4.1.1 Синхронизация между устройствами

### 4.1.1.1 Механизм синхронизации
- **Конфиг:** `CloudSyncConfig` (env: `CLOUD_SYNC_ENABLED`, `CLOUD_SYNC_INTERVAL_SEC`, `CLOUD_SYNC_CONFLICT_STRATEGY`).
- **Модели:** `SyncState`, `SyncStatus`, `SyncConflict`, `SyncResourceVersion`, `ConflictResolutionStrategy`.
- **Сервис:** `CloudSyncService` / `CloudSyncServiceImpl` — синхронизация камер, настроек и событий с локальными репозиториями (`CameraRepository`, `SettingsRepository`, `EventRepository`). Состояние и конфликты в памяти.
- **API (JWT, admin):**
  - `GET /api/v1/sync/status` — текущее состояние синхронизации.
  - `POST /api/v1/sync/start` — запуск полной синхронизации.
  - `GET /api/v1/sync/conflicts` — список конфликтов.
  - `POST /api/v1/sync/resolve` — разрешение конфликтов (тело: `conflictIds`, `strategy`).

### 4.1.1.2 Разрешение конфликтов
- **ConflictResolver** — стратегии last-write-wins и merge (по `updatedAt`).
- В API поддерживаются стратегии: `LAST_WRITE_WINS`, `MERGE`, `MANUAL` (ручное разрешение через выбор версии — при необходимости расширить API).

---

## 4.1.2 Облачное хранилище

### 4.1.2.1 Интеграция с провайдерами
- **Конфиг:** `CloudStorageConfig` (env: `CLOUD_STORAGE_ENABLED`, `CLOUD_STORAGE_PROVIDER`, `CLOUD_STORAGE_BUCKET`, `CLOUD_STORAGE_ENDPOINT`, `CLOUD_STORAGE_REGION`, `CLOUD_STORAGE_ACCESS_KEY`, `CLOUD_STORAGE_SECRET_KEY`, `CLOUD_STORAGE_PREFIX`, `CLOUD_STORAGE_PATH_STYLE`, `CLOUD_STORAGE_LOCAL_PATH`).
- **Провайдер:** интерфейс `CloudStorageProvider` (upload, download, delete, list). Реализация **FileBasedCloudStorageProvider** — локальная файловая система (для разработки и без облака). Для AWS S3 / MinIO / GCS / Azure Blob — добавить реализации, использующие соответствующие SDK (например, AWS SDK for Java 2.x для S3/MinIO).

### 4.1.2.2 Управление хранилищем
- **CloudStorageService** — загрузка записи по `recordingId` и пути к файлу, скачивание по `recordingId` и ключу, удаление по `recordingId`, список объектов под префиксом.
- **API (JWT, admin):**
  - `GET /api/v1/cloud/storage` — список записей в облаке.
  - `GET /api/v1/cloud/storage/download/{recordingId}?key=...` — скачивание.
  - `DELETE /api/v1/cloud/storage/{recordingId}` — удаление записи из облака.

---

## 4.1.3 Резервное копирование

### 4.1.3.1 Автоматическое резервирование
- **BackupSchedulerService** — планировщик по интервалу (`BACKUP_SCHEDULER_INTERVAL_MIN`). При старте приложения вызывается `start()`; по таймеру вызывается `DatabaseBackupService.createBackup(null)`.
- Восстановление уже реализовано в **DatabaseBackupService** (pg_dump / pg_restore).

### 4.1.3.2 Восстановление
- Реализовано в **DatabaseRoutes**: `POST /api/v1/database/backup/restore` с телом `{ "backupPath": "..." }`, вызов `DatabaseBackupService.restoreBackup(path)`.

---

## Переменные окружения

См. раздел «Enterprise: облачная синхронизация и хранилище (4.1)» в [ENVIRONMENT_VARIABLES.md](../ENVIRONMENT_VARIABLES.md).

---

## Дальнейшие шаги

1. **Синхронизация:** подключить удалённый источник (другой инстанс API или облачный хаб) для реального обмена камерами/настройками/событиями.
2. **Облако:** реализовать провайдеры S3 (AWS SDK), MinIO, при необходимости GCS и Azure Blob.
3. **Бэкапы:** опционально — загрузка созданных бэкапов в облачное хранилище через `CloudStorageService`.
