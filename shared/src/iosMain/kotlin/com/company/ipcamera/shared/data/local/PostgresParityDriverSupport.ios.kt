package com.company.ipcamera.shared.data.local

import app.cash.sqldelight.db.SqlDriver

internal actual fun isPostgresFlywayParityDriver(driver: SqlDriver): Boolean = false
