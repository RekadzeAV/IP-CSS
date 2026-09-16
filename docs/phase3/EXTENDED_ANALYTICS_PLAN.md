# Extended Analytics Implementation Plan

**Дата:** 28 January 2026  
**Длительность:** 2-3 месяца (8-12 недель)  
**Приоритет:** Высокий  
**Статус:** В реализации

---

## 🎯 Цель

Реализация расширенных AI-функций: распознавание лиц, ANPR, поведенческий анализ и тепловые карты.

---

## 📊 Component 1: Face Recognition (3-4 недели)

### Неделя 1: Интеграция FaceNet/ArcFace

#### Модель и инфраструктура
```kotlin
// shared/src/commonMain/kotlin/com/company/ipcamera/shared/ai/face/FaceRecognitionService.kt
interface FaceRecognitionService {
    suspend fun initialize(modelPath: String): Result<Unit>
    suspend fun detectFaces(image: ByteArray): Result<List<DetectedFace>>
    suspend fun recognizeFace(image: ByteArray, galleryId: String): Result<FaceMatch?>
    suspend fun addFaceToGallery(galleryId: String, name: String, image: ByteArray): Result<String>
    suspend fun removeFaceFromGallery(galleryId: String, faceId: String): Result<Unit>
}

data class DetectedFace(
    val id: String,
    val boundingBox: BoundingBox,
    val confidence: Double,
    val embedding: FloatArray, // 512-dimensional for FaceNet
    val landmarks: List<FaceLandmark>
)

data class FaceMatch(
    val faceId: String,
    val name: String,
    val similarity: Double,
    val threshold: Double = 0.6
)
```

#### ONNX Runtime интеграция
```kotlin
// server/api/src/main/kotlin/com/company/ipcamera/server/service/face/FaceNetService.kt
class FaceNetService @Inject constructor(
    private val onnxSession: OnnxSession
) : FaceRecognitionService {
    
    private var modelLoaded = false
    
    override suspend fun initialize(modelPath: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            onnxSession.loadModel(modelPath)
            modelLoaded = true
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(FaceRecognitionException("Failed to load FaceNet model", e))
        }
    }
    
    override suspend fun detectFaces(image: ByteArray): Result<List<DetectedFace>> = withContext(Dispatchers.IO) {
        if (!modelLoaded) return@withContext Result.failure(ModelNotLoadedException())
        
        // Preprocessing
        val preprocessedImage = preprocessImage(image)
        
        // Inference
        val embeddings = onnxSession.run(preprocessedImage)
        
        // Post-processing
        val faces = extractFaces(embeddings)
        Result.success(faces)
    }
    
    private fun calculateSimilarity(embedding1: FloatArray, embedding2: FloatArray): Double {
        // Cosine similarity
        val dotProduct = embedding1.zip(embedding2).sumOf { it.first * it.second }
        val norm1 = sqrt(embedding1.sumOf { it * it })
        val norm2 = sqrt(embedding2.sumOf { it * it })
        return dotProduct / (norm1 * norm2)
    }
}
```

### Неделя 2: Face Gallery и база данных

#### SQLDelight схема
```sql
-- shared/src/commonMain/sqldelight/FaceGallery.sq
CREATE TABLE face_gallery (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL
);

CREATE TABLE face_embedding (
    id TEXT PRIMARY KEY,
    gallery_id TEXT NOT NULL REFERENCES face_gallery(id),
    embedding BLOB NOT NULL, -- 512 floats as bytes
    metadata TEXT,
    created_at INTEGER NOT NULL,
    INDEX idx_gallery (gallery_id)
);

CREATE TABLE face_match_log (
    id TEXT PRIMARY KEY,
    camera_id TEXT NOT NULL,
    face_id TEXT REFERENCES face_embedding(id),
    matched_name TEXT,
    similarity REAL,
    snapshot_path TEXT,
    timestamp INTEGER NOT NULL
);
```

#### Репозиторий
```kotlin
// server/api/src/main/kotlin/com/company/ipcamera/server/repository/FaceGalleryRepository.kt
interface FaceGalleryRepository {
    suspend fun createGallery(name: String): Result<FaceGallery>
    suspend fun getGallery(id: String): Result<FaceGallery?>
    suspend fun addFace(galleryId: String, name: String, embedding: FloatArray): Result<FaceEmbedding>
    suspend fun findMatches(galleryId: String, embedding: FloatArray, threshold: Double): Result<List<FaceMatch>>
    suspend fun deleteFace(faceId: String): Result<Unit>
}
```

### Неделя 3-4: Real-time распознавание

#### Face Detection + Recognition pipeline
```kotlin
// server/api/src/main/kotlin/com/company/ipcamera/server/service/FaceDetectionPipeline.kt
class FaceDetectionPipeline @Inject constructor(
    private val faceDetectionService: FaceDetectionService,
    private val faceRecognitionService: FaceRecognitionService,
    private val faceGalleryRepository: FaceGalleryRepository,
    private val eventService: EventService
) {
    suspend fun processFrame(cameraId: String, frame: ByteArray) {
        val faces = faceDetectionService.detectFaces(frame)
        
        for (face in faces) {
            val matches = faceGalleryRepository.findMatches(
                galleryId = "default",
                embedding = face.embedding,
                threshold = 0.6
            )
            
            if (matches.isSuccess && matches.getOrNull()?.isNotEmpty() == true) {
                val match = matches.getOrNull()!!.first()
                eventService.createFaceRecognitionEvent(
                    cameraId = cameraId,
                    personName = match.name,
                    similarity = match.similarity,
                    snapshotPath = saveSnapshot(frame)
                )
            }
        }
    }
}
```

---

## 🚗 Component 2: License Plate Recognition (ANPR) (3-4 недели)

### Неделя 1: Интеграция ALPR/OpenALPR

#### Native ALPR binding
```kotlin
// shared/src/commonMain/kotlin/com/company/ipcamera/shared/ai/anpr/AnprService.kt
interface AnprService {
    suspend fun initialize(config: AnprConfig): Result<Unit>
    suspend fun recognizePlate(image: ByteArray): Result<List<PlateResult>>
    suspend fun processVideoStream(streamUrl: String): Flow<PlateResult>
}

data class PlateResult(
    val plateNumber: String,
    val confidence: Double,
    val region: String?,
    val regionConfidence: Double,
    val vehicleType: VehicleType?,
    val boundingBox: BoundingBox,
    val processingTimeMs: Long,
    val coordinates: List<Point>
)

enum class VehicleType {
    SEDAN, SUV, TRUCK, MOTORCYCLE, BUS, VAN, UNKNOWN
}
```

#### OpenALPR интеграция через JNI
```kotlin
// native/anpr/src/main/cpp/alpr_wrapper.cpp
#include "alpr.h"
#include <jni.h>

using namespace alpr;

class AlprWrapper {
private:
    Alpr* alpr;
    
public:
    AlprWrapper(const std::string& country, const std::string& config_file) {
        alpr = new Alpr(country, config_file);
    }
    
    JniResult recognize(JNIEnv* env, jbyteArray image_bytes) {
        std::vector<uint8_t> image_vector = jbyteArrayToVector(env, image_bytes);
        
        AlprResults results = alpr->recognize(image_vector.data(), image_vector.size());
        
        return convertToJniResult(env, results);
    }
    
    ~AlprWrapper() {
        delete alpr;
    }
};
```

### Неделя 2-3: Plate Database и поиск

#### SQLDelight схема
```sql
-- shared/src/commonMain/sqldelight/ANPR.sq
CREATE TABLE license_plate (
    id TEXT PRIMARY KEY,
    plate_number TEXT NOT NULL,
    region TEXT,
    vehicle_type TEXT,
    owner_name TEXT,
    owner_contact TEXT,
    notes TEXT,
    is_blacklisted INTEGER DEFAULT 0,
    created_at INTEGER NOT NULL,
    updated_at INTEGER NOT NULL,
    INDEX idx_plate (plate_number),
    INDEX idx_blacklist (is_blacklisted)
);

CREATE TABLE plate_detection (
    id TEXT PRIMARY KEY,
    camera_id TEXT NOT NULL,
    plate_id TEXT REFERENCES license_plate(id),
    plate_number TEXT NOT NULL,
    confidence REAL,
    snapshot_path TEXT,
    direction TEXT, -- entering, exiting, unknown
    timestamp INTEGER NOT NULL,
    INDEX idx_camera_time (camera_id, timestamp),
    INDEX idx_plate_time (plate_number, timestamp)
);
```

### Неделя 4: Интеграция и события

#### ANPR Event Generation
```kotlin
// server/api/src/main/kotlin/com/company/ipcamera/server/service/AnprEventService.kt
class AnprEventService @Inject constructor(
    private val anprService: AnprService,
    private val plateRepository: LicensePlateRepository,
    private val eventService: EventService
) {
    suspend fun processDetection(cameraId: String, image: ByteArray) {
        val results = anprService.recognizePlate(image)
        
        results.getOrNull()?.forEach { plate ->
            // Check if plate is in database
            val existingPlate = plateRepository.findByPlateNumber(plate.plateNumber)
            
            // Generate event
            val eventType = when {
                existingPlate?.isBlacklisted == true -> EventType.PLATE_BLACKLISTED
                existingPlate != null -> EventType.PLATE_RECOGNIZED
                else -> EventType.PLATE_DETECTED
            }
            
            eventService.createAnprEvent(
                cameraId = cameraId,
                plateNumber = plate.plateNumber,
                eventType = eventType,
                confidence = plate.confidence,
                snapshotPath = saveSnapshot(image)
            )
        }
    }
}
```

---

## 📈 Component 3: Behavioral Analysis (2-3 недели)

### Неделя 1: Anomaly Detection

#### Pattern recognition
```kotlin
// server/api/src/main/kotlin/com/company/ipcamera/server/service/analytics/BehavioralAnalyticsService.kt
interface BehavioralAnalyticsService {
    suspend fun analyzePatterns(cameraId: String, timeRange: TimeRange): Result<BehavioralReport>
    suspend fun detectAnomaly(cameraId: String, eventData: EventData): Result<Anomaly?>
    suspend fun getHeatmap(cameraId: String, timeRange: TimeRange): Result<HeatmapData>
    suspend fun predictActivity(cameraId: String, time: Long): Result<ActivityPrediction>
}

data class BehavioralReport(
    val cameraId: String,
    val timeRange: TimeRange,
    val averageActivity: Double,
    val peakHours: List<Int>,
    val lowActivityHours: List<Int>,
    val commonPatterns: List<Pattern>,
    val anomalies: List<Anomaly>
)

data class Anomaly(
    val type: AnomalyType,
    val severity: Severity,
    val description: String,
    val timestamp: Long,
    val confidence: Double
)

enum class AnomalyType {
    UNUSUAL_TIME_ACTIVITY,
    CROWD_FORMATION,
    LOITERING,
    ABANDONED_OBJECT,
    RESTRICTED_AREA_ENTRY,
    SPEED_ANOMALY,
    DIRECTION_ANOMALY
}
```

### Неделя 2: Heatmap Generation

#### Spatial analysis
```kotlin
// server/api/src/main/kotlin/com/company/ipcamera/server/service/analytics/HeatmapService.kt
class HeatmapService @Inject constructor(
    private val eventRepository: EventRepository,
    private val motionEventRepository: MotionEventRepository
) {
    suspend fun generateHeatmap(
        cameraId: String,
        timeRange: TimeRange,
        resolution: Int = 64 // 64x64 grid
    ): Result<HeatmapData> = withContext(Dispatchers.IO) {
        val events = eventRepository.getByCameraAndTime(cameraId, timeRange.startTime, timeRange.endTime)
        
        val grid = Array(resolution) { Array(resolution) { 0 } }
        
        events.forEach { event ->
            event.zoneId?.let { zoneId ->
                val (x, y) = zoneIdToGridCoordinates(zoneId, resolution)
                if (x in 0 until resolution && y in 0 until resolution) {
                    grid[y][x]++
                }
            }
        }
        
        // Normalize and smooth
        val normalizedGrid = normalizeGrid(grid)
        val smoothedGrid = applyGaussianBlur(normalizedGrid)
        
        Result.success(
            HeatmapData(
                cameraId = cameraId,
                timeRange = timeRange,
                grid = smoothedGrid,
                maxIntensity = smoothedGrid.flatten().maxOrNull() ?: 0
            )
        )
    }
}
```

### Неделя 3: Prediction Engine

#### ML-based prediction
```kotlin
// server/api/src/main/kotlin/com/company/ipcamera/server/service/analytics/PredictionEngine.kt
class PredictionEngine @Inject constructor(
    private val historicalRepository: HistoricalDataRepository
) {
    suspend fun predictActivity(
        cameraId: String,
        time: Long,
        horizon: Long = 3600000 // 1 hour
    ): Result<ActivityPrediction> {
        val historicalData = historicalRepository.getHistoricalPatterns(
            cameraId = cameraId,
            daysBack = 30
        )
        
        // Time-based features
        val hourOfDay = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).hour
        val dayOfWeek = Instant.ofEpochMilli(time).atZone(ZoneId.systemDefault()).dayOfWeek
        
        // Find similar historical periods
        val similarPeriods = historicalData.filter {
            it.hour == hourOfDay && it.dayOfWeek == dayOfWeek
        }
        
        // Calculate prediction
        val predictedActivity = similarPeriods.averageOf { it.activityLevel }
        val confidence = calculateConfidence(similarPeriods)
        
        return Result.success(
            ActivityPrediction(
                cameraId = cameraId,
                predictedTime = time,
                predictedActivityLevel = predictedActivity,
                confidence = confidence,
                peakProbability = calculatePeakProbability(similarPeriods)
            )
        )
    }
}
```

---

## 📊 Метрики успеха

| Метрика | Face Recognition | ANPR | Behavioral |
|---------|------------------|------|------------|
| Точность | 95%+ | 90%+ | 85%+ |
| Задержка | <500ms | <300ms | <1s |
| FP rate | <2% | <5% | <10% |
| Поддержка камер | 50+ | 50+ | 100+ |

---

## 📅 Timeline

```
Месяц 1: Face Recognition (детекция + распознавание)
Месяц 2: ANPR (распознавание номеров)
Месяц 3: Behavioral Analysis + интеграция
```

---

## 🎯 Acceptance Criteria

### Face Recognition:
- ✅ FaceNet/ArcFace интеграция
- ✅ Face Gallery (CRUD)
- ✅ Real-time распознавание
- ✅ События при распознавании
- ✅ >95% точность

### ANPR:
- ✅ OpenALPR интеграция
- ✅ Plate Database
- ✅ Blacklist поддержка
- ✅ События для распознанных номеров
- ✅ >90% точность

### Behavioral:
- ✅ Anomaly Detection
- ✅ Heatmap Generation
- ✅ Activity Prediction
- ✅ Pattern Recognition
- ✅ Интеграция с Events

---

**Статус:** В реализации  
**Следующий шаг:** Face Recognition implementation
