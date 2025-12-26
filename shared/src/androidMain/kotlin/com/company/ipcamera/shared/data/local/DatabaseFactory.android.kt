package com.company.ipcamera.shared.data.local

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.company.ipcamera.shared.database.CameraDatabase

/**
 * Android реализация DatabaseFactory
 * 
 * Требует Context, который должен быть предоставлен через конструктор
 */
actual class DatabaseFactory(context: Any?) {
    private val androidContext: Context = context as? Context 
        ?: throw IllegalArgumentException("Context is required for Android DatabaseFactory")
    
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = CameraDatabase.Schema,
            context = androidContext,
            name = "camera_database.db"
        )
    }
}
