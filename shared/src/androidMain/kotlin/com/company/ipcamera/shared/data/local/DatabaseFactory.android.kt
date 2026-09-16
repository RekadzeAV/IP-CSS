package com.company.ipcamera.shared.data.local

import android.content.Context
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlSchema
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.company.ipcamera.shared.database.CameraDatabase

/**
 * Android реализация DatabaseFactory
 * * Требует Context, который должен быть предоставлен через конструктор
 */
actual class DatabaseFactory actual constructor(context: Any?) {
    private val androidContext: Context =
        context as? Context ?: throw IllegalArgumentException(
            "Context is required for Android DatabaseFactory",
        )

    actual fun createDriver(): SqlDriver {
        @Suppress("UNCHECKED_CAST")
        val schema = CameraDatabase.Schema as SqlSchema<QueryResult.Value<Unit>>
        return AndroidSqliteDriver(
            schema = schema,
            context = androidContext,
            name = "camera_database.db",
        )
    }
}
