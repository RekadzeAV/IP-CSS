package com.company.ipcamera.shared.data.local

import app.cash.sqldelight.db.SqlDriver

/**
 * Parity + Flyway путь в [createDatabase] допустим только на JVM PostgreSQL (SQLDelight [JdbcDriver]),
 * а не на [JdbcSqliteDriver] (desktop тесты / локальный sqlite).
 */
internal expect fun isPostgresFlywayParityDriver(driver: SqlDriver): Boolean
