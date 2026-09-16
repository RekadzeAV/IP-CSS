package com.company.ipcamera.shared.data.local

import app.cash.sqldelight.async.coroutines.await
import app.cash.sqldelight.db.SqlDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Когда схему БД на PostgreSQL создаёт **Flyway** (server `db/migration`), таблицы уже существуют
 * и путь [CameraDatabase.Schema.migrate] (SQLite-миграции 1.sqm, …) несовместим: дублирует DDL
 * и использует функции вроде `strftime` применительно к PostgreSQL.
 *
 * В этом сценарии доводим схему до ожидаемой SQLDelight-версии отдельными `IF NOT EXISTS` DDL
 * и строкой [schema_version], не вызывая `.sqm` на JDBC PostgreSQL.
 */
object PostgresFlywaySchemaSync {
    /**
     * Сервер в Docker/CI: `DB_MODE=postgres` и Flyway по умолчанию включён — схема DDL от Flyway.
     * Допускаем и `jdbc:postgresql` в `DATABASE_URL`, если `DB_MODE` не задан.
     */
    fun shouldApplyPostgresFlywayParitySync(): Boolean {
        val enableFlyway = System.getenv("ENABLE_FLYWAY")?.toBoolean() ?: true
        if (!enableFlyway) return false
        if (System.getenv("DB_MODE")?.trim().equals("postgres", ignoreCase = true)) {
            return true
        }
        val url = System.getenv("DATABASE_URL") ?: System.getenv("POSTGRES_URL") ?: return false
        return url.contains("postgresql", ignoreCase = true)
    }

    /**
     * @return `true` если `flyway_schema_history` существует и запрос к ней успешен
     * (дополнительная эвристика; на JDBC `execute`+SELECT без mapper может вести себя иначе).
     */
    suspend fun isPostgresWithFlywayHistory(driver: SqlDriver): Boolean {
        return withContext(Dispatchers.Default) {
            runCatching {
                driver.execute(null, "SELECT 1 FROM flyway_schema_history LIMIT 1", 0).await()
            }.isSuccess
        }
    }

    /**
     * Idempotent: безопасно вызывать на каждом старте.
     */
    suspend fun ensureSqlDelightParityAfterFlyway(
        driver: SqlDriver,
        targetSchemaVersion: Int,
    ) {
        withContext(Dispatchers.Default) {
            // schema_version
            driver.execute(
                null,
                """
                CREATE TABLE IF NOT EXISTS schema_version (
                    version INTEGER NOT NULL PRIMARY KEY,
                    applied_at INTEGER NOT NULL,
                    description TEXT
                );
                """.trimIndent(),
                0,
            ).await()
            driver.execute(
                null,
                "CREATE INDEX IF NOT EXISTS schema_version_version_index ON schema_version(version);",
                0,
            ).await()

            // Базовые таблицы могут быть созданы в старой/частично мигрированной схеме.
            // Подтягиваем критичные колонки SQLDelight/Flyway до ожидаемого контракта.
            runCatching {
                driver.execute(
                    null,
                    "ALTER TABLE camera ADD COLUMN IF NOT EXISTS created_at BIGINT NOT NULL DEFAULT 0;",
                    0,
                ).await()
                driver.execute(
                    null,
                    "ALTER TABLE camera ADD COLUMN IF NOT EXISTS updated_at BIGINT NOT NULL DEFAULT 0;",
                    0,
                ).await()
                driver.execute(null, "ALTER TABLE camera ADD COLUMN IF NOT EXISTS last_seen BIGINT;", 0).await()
                driver.execute(
                    null,
                    "ALTER TABLE recording ADD COLUMN IF NOT EXISTS created_at BIGINT NOT NULL DEFAULT 0;",
                    0,
                ).await()
                driver.execute(
                    null,
                    "ALTER TABLE \"user\" ADD COLUMN IF NOT EXISTS created_at BIGINT NOT NULL DEFAULT 0;",
                    0,
                ).await()
                driver.execute(null, "ALTER TABLE \"user\" ADD COLUMN IF NOT EXISTS last_login_at BIGINT;", 0).await()
                driver.execute(
                    null,
                    "ALTER TABLE \"user\" ADD COLUMN IF NOT EXISTS is_active INTEGER NOT NULL DEFAULT 1;",
                    0,
                ).await()
            }.onFailure { e ->
                logger.warn(e) { "Could not apply core schema parity ALTER TABLE statements" }
            }

            // license_plate (2.sqm)
            driver.execute(
                null,
                """
                CREATE TABLE IF NOT EXISTS license_plate (
                    id TEXT NOT NULL PRIMARY KEY,
                    camera_id TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    plate_number TEXT NOT NULL,
                    confidence REAL NOT NULL,
                    country TEXT,
                    bbox_x INTEGER NOT NULL,
                    bbox_y INTEGER NOT NULL,
                    bbox_width INTEGER NOT NULL,
                    bbox_height INTEGER NOT NULL,
                    created_at INTEGER NOT NULL
                );
                """.trimIndent(),
                0,
            ).await()
            driver.execute(
                null,
                "CREATE INDEX IF NOT EXISTS license_plate_camera_id_index ON license_plate(camera_id);",
                0,
            ).await()
            driver.execute(
                null,
                "CREATE INDEX IF NOT EXISTS license_plate_timestamp_index ON license_plate(timestamp);",
                0,
            ).await()
            driver.execute(
                null,
                "CREATE INDEX IF NOT EXISTS license_plate_plate_number_index ON license_plate(plate_number);",
                0,
            ).await()

            // face_gallery (3.sqm)
            driver.execute(
                null,
                """
                CREATE TABLE IF NOT EXISTS face_gallery (
                    id TEXT NOT NULL PRIMARY KEY,
                    label TEXT NOT NULL,
                    embedding_blob TEXT NOT NULL,
                    camera_id TEXT,
                    created_at INTEGER NOT NULL,
                    metadata TEXT
                );
                """.trimIndent(),
                0,
            ).await()
            driver.execute(
                null,
                "CREATE INDEX IF NOT EXISTS face_gallery_label_index ON face_gallery(label);",
                0,
            ).await()
            driver.execute(
                null,
                "CREATE INDEX IF NOT EXISTS face_gallery_camera_id_index ON face_gallery(camera_id);",
                0,
            ).await()
            driver.execute(
                null,
                "CREATE INDEX IF NOT EXISTS face_gallery_created_at_index ON face_gallery(created_at);",
                0,
            ).await()

            // 4.sqm: recording.codec (column may already be created by a prior partial migrate)
            runCatching {
                driver.execute(
                    null,
                    "ALTER TABLE recording ADD COLUMN IF NOT EXISTS codec TEXT;",
                    0,
                ).await()
            }.onFailure { e ->
                logger.warn(e) { "Could not add recording.codec (may be unsupported on older PostgreSQL?)" }
            }

            // Зафиксировать целевую версию для MAX(schema_version) / валидации
            val epochSeconds = Clock.System.now().toEpochMilliseconds() / 1000L
            driver.execute(
                null,
                """
                INSERT INTO schema_version (version, applied_at, description)
                VALUES (?, ?, ?)
                ON CONFLICT (version) DO UPDATE SET
                  applied_at = EXCLUDED.applied_at,
                  description = EXCLUDED.description
                """.trimIndent(),
                3,
            ) {
                bindLong(0, targetSchemaVersion.toLong())
                bindLong(1, epochSeconds)
                bindString(2, "PostgreSQL: Flyway + SQLDelight parity sync (PostgresFlywaySchemaSync)")
            }.await()

            logger.info { "Postgres Flyway + SQLDelight parity sync done (target schema version=$targetSchemaVersion)" }
        }
    }
}
