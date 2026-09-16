package com.company.ipcamera.shared.data.datasource.local.impl

import app.cash.sqldelight.db.SqlDriver

/**
 * Android implementation: JdbcDriver is not available on Android.
 * Always returns false (PostgreSQL not supported on Android).
 */
internal actual fun isPostgresDriver(driver: SqlDriver): Boolean {
    return false
}
