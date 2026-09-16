package com.company.ipcamera.shared.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.JdbcDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

internal actual fun isPostgresFlywayParityDriver(driver: SqlDriver): Boolean {
    if (driver is JdbcSqliteDriver) return false
    if (driver is JdbcDriver) return true
    return false
}
