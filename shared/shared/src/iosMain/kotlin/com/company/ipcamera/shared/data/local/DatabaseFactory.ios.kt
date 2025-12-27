package com.company.ipcamera.shared.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.company.ipcamera.shared.database.CameraDatabase

actual class DatabaseFactory(actual context: Any?) {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = CameraDatabase.Schema,
            name = "camera_database.db"
        )
    }
}

