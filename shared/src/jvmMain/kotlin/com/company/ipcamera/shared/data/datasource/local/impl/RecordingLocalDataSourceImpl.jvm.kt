package com.company.ipcamera.shared.data.datasource.local.impl

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.JdbcDriver

/**
 * JVM implementation: check for PostgreSQL JDBC driver
 */
internal actual fun isPostgresDriver(driver: SqlDriver): Boolean {
    return driver is JdbcDriver
}
