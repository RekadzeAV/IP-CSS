package com.company.ipcamera.android.data.local

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ========== Entities ==========

@Entity(tableName = "cameras")
data class CameraEntity(
    @PrimaryKey val id: String,
    val name: String,
    val url: String,
    val username: String? = null,
    val password: String? = null,
    val isOnline: Boolean = false,
    val lastSeen: Long = System.currentTimeMillis()
)

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val cameraId: String? = null,
    val type: String,
    val message: String,
    val severity: String = "INFO",
    val timestamp: Long = System.currentTimeMillis(),
    val acknowledged: Boolean = false
)

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey val id: String,
    val cameraId: String,
    val startTime: Long,
    val endTime: Long? = null,
    val duration: Long = 0,
    val size: Long = 0,
    val format: String = "mp4",
    val status: String = "RECORDING"
)

// ========== DAOs ==========

@Dao
interface CameraDao {
    @Query("SELECT * FROM cameras ORDER BY name ASC")
    fun getAllCameras(): Flow<List<CameraEntity>>

    @Query("SELECT * FROM cameras WHERE id = :id")
    suspend fun getCameraById(id: String): CameraEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCamera(camera: CameraEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCameras(cameras: List<CameraEntity>)

    @Delete
    suspend fun deleteCamera(camera: CameraEntity)

    @Query("DELETE FROM cameras WHERE id = :id")
    suspend fun deleteCameraById(id: String)

    @Query("SELECT COUNT(*) FROM cameras")
    fun getCameraCount(): Flow<Int>

    @Query("UPDATE cameras SET isOnline = :isOnline, lastSeen = :lastSeen WHERE id = :id")
    suspend fun updateCameraStatus(id: String, isOnline: Boolean, lastSeen: Long = System.currentTimeMillis())
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY timestamp DESC")
    fun getAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE cameraId = :cameraId ORDER BY timestamp DESC")
    fun getEventsByCamera(cameraId: String): Flow<List<EventEntity>>

    @Query("SELECT * FROM events WHERE severity = :severity ORDER BY timestamp DESC")
    fun getEventsBySeverity(severity: String): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: EventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<EventEntity>)

    @Query("UPDATE events SET acknowledged = 1 WHERE id = :id")
    suspend fun acknowledgeEvent(id: String)

    @Query("DELETE FROM events WHERE timestamp < :before")
    suspend fun deleteOldEvents(before: Long)

    @Query("SELECT COUNT(*) FROM events WHERE acknowledged = 0")
    fun getUnacknowledgedCount(): Flow<Int>
}

@Dao
interface RecordingDao {
    @Query("SELECT * FROM recordings ORDER BY startTime DESC")
    fun getAllRecordings(): Flow<List<RecordingEntity>>

    @Query("SELECT * FROM recordings WHERE cameraId = :cameraId ORDER BY startTime DESC")
    fun getRecordingsByCamera(cameraId: String): Flow<List<RecordingEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecording(recording: RecordingEntity)

    @Query("DELETE FROM recordings WHERE id = :id")
    suspend fun deleteRecording(id: String)

    @Query("DELETE FROM recordings WHERE startTime < :before")
    suspend fun deleteOldRecordings(before: Long)
}

// ========== Database ==========

@Database(
    entities = [CameraEntity::class, EventEntity::class, RecordingEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun cameraDao(): CameraDao
    abstract fun eventDao(): EventDao
    abstract fun recordingDao(): RecordingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "ipcss_cache.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
