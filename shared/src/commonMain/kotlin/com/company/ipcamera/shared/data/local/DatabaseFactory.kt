package com.company.ipcamera.shared.data.local

import app.cash.sqldelight.db.SqlDriver
import com.company.ipcamera.shared.database.CameraDatabase

/**
 * Фабрика для создания экземпляра базы данных
 * 
 * Для Android требуется Context в конструкторе, для iOS - null
 */
expect class DatabaseFactory(context: Any?) {
    fun createDriver(): SqlDriver
}

fun createDatabase(driver: SqlDriver): CameraDatabase {
    return CameraDatabase(driver)
}
