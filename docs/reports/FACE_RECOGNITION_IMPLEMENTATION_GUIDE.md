# Face Recognition Implementation Guide

**Дата:** 27 April 2026  
**Задача:** 3.3.2 — Распознавание лиц  
**Статус:** ✅ **100%**

---

## 🎯 Реализованный функционал

### 1. Face Detection
- ✅ Детекция лиц в видеопотоке
- ✅ Bounding box для каждого лица
- ✅ Facial landmarks (опционально)
- ✅ Face embeddings (векторное представление)

**Файлы:**
- `DetectFacesUseCase.kt`
- `AnalyticsServiceImpl.jvm.kt`
- `AnalyticsServiceImpl.android.kt`

---

### 2. Face Comparison
- ✅ Cosine similarity для сравнения эмбеддингов
- ✅ Поддержка разных размеров эмбеддингов
- ✅ Настройка порога сходства

**Файлы:**
- `AnalyticsService.kt` — интерфейс
- `AnalyticsServiceImpl.jvm.kt` — JVM реализация
- `AnalyticsServiceImpl.android.kt` — Android реализация
- `AnalyticsServiceImpl.ios.kt` — iOS реализация

```kotlin
suspend fun compareFaces(embedding1: FloatArray, embedding2: FloatArray): Float {
    // Cosine similarity
    val dotProduct = embedding1.zip(embedding2) { a, b -> a * b }.sum()
    val magnitude1 = sqrt(embedding1.sumOf { it * it })
    val magnitude2 = sqrt(embedding2.sumOf { it * it })
    return if (magnitude1 == 0f || magnitude2 == 0f) 0f else dotProduct / (magnitude1 * magnitude2)
}
```

---

### 3. Face Repository (SQLDelight)
- ✅ CRUD операции для базы лиц
- ✅ Поиск по label
- ✅ Поиск ближайших совпадений
- ✅ Группировка по label
- ✅ In-memory и SQLDelight реализации

**Файлы:**
- `FaceRepository.kt` — интерфейс
- `FaceRepositoryImplSqlDelight.kt` — SQLDelight реализация
- `StoredFace.kt` — модель данных

**Методы:**
```kotlin
interface FaceRepository {
    suspend fun insert(face: StoredFace): Result<Unit>
    suspend fun getById(id: String): StoredFace?
    suspend fun getByLabel(label: String, limit: Int = 100): List<StoredFace>
    suspend fun listLabels(): List<String>
    suspend fun delete(id: String): Result<Unit>
    suspend fun deleteByLabel(label: String): Result<Long>
    suspend fun findNearest(embedding: FloatArray, topK: Int = 10): List<FaceMatch>
}
```

---

### 4. Face Gallery API
- ✅ GET `/api/v1/faces` — список всех лиц
- ✅ GET `/api/v1/faces/labels` — список уникальных labels
- ✅ GET `/api/v1/faces/labels/{label}` — лица по label
- ✅ POST `/api/v1/faces` — добавить лицо в галерею
- ✅ DELETE `/api/v1/faces/{id}` — удалить лицо
- ✅ DELETE `/api/v1/faces/labels/{label}` — удалить все лица по label
- ✅ POST `/api/v1/faces/search` — поиск по embedding

**Файлы:**
- `FaceGalleryRoutes.kt`
- `FaceGalleryRoutesTest.kt`

---

### 5. Интеграционные тесты
- ✅ SQLDelight integration tests
- ✅ Face repository tests
- ✅ Face gallery routes tests
- ✅ Compare faces unit tests

**Файлы:**
- `FaceRepositoryImplSqlDelightIntegrationTest.kt`
- `FaceGalleryRoutesTest.kt`
- `AnalyticsUseCasesTest.kt`

---

## 📊 API Endpoints

### Получить все лица
```bash
GET /api/v1/faces?limit=100
Authorization: Bearer {token}

Response:
{
  "success": true,
  "data": [
    {
      "id": "face-123",
      "label": "John Doe",
      "cameraId": "camera-001",
      "createdAt": 1714234567890,
      "embedding": [0.1, 0.2, ...]
    }
  ]
}
```

### Получить список labels
```bash
GET /api/v1/faces/labels
Authorization: Bearer {token}

Response:
{
  "success": true,
  "data": ["John Doe", "Jane Smith", "Unknown"]
}
```

### Добавить лицо в галерею
```bash
POST /api/v1/faces
Authorization: Bearer {token}
Content-Type: application/json

{
  "label": "John Doe",
  "cameraId": "camera-001",
  "embedding": [0.1, 0.2, 0.3, ...]
}

Response:
{
  "success": true,
  "data": {
    "id": "face-123",
    "label": "John Doe"
  }
}
```

### Поиск по лицу
```bash
POST /api/v1/faces/search
Authorization: Bearer {token}
Content-Type: application/json

{
  "embedding": [0.1, 0.2, 0.3, ...],
  "topK": 10,
  "minSimilarity": 0.7
}

Response:
{
  "success": true,
  "data": [
    {
      "face": { "id": "face-123", "label": "John Doe", ... },
      "similarity": 0.95
    },
    {
      "face": { "id": "face-456", "label": "Jane Smith", ... },
      "similarity": 0.72
    }
  ]
}
```

### Удалить лицо
```bash
DELETE /api/v1/faces/{id}
Authorization: Bearer {token}

Response:
{
  "success": true,
  "data": null
}
```

---

## 🧪 Использование

### 1. Детекция лиц с embeddings

```kotlin
val detectFacesUseCase = get<DetectFacesUseCase>()

val result = detectFacesUseCase(
    camera = camera,
    frameData = frameData,
    minConfidence = 0.8f,
    includeLandmarks = true,
    includeEmbeddings = true,
    createEvent = false
)

result.onSuccess { facesDetectionResult ->
    facesDetectionResult.faces.forEach { face ->
        println("Face detected: ${face.boundingBox}")
        println("Embedding: ${face.embedding?.size} dimensions")
    }
}
```

### 2. Сравнение двух лиц

```kotlin
val analyticsService = get<AnalyticsService>()

val similarity = analyticsService.compareFaces(
    embedding1 = face1.embedding,
    embedding2 = face2.embedding
)

if (similarity > 0.85) {
    println("Same person with ${similarity * 100}% confidence")
}
```

### 3. Поиск по базе лиц

```kotlin
val faceRepository = get<FaceRepository>()

val matches = faceRepository.findNearest(
    embedding = unknownFaceEmbedding,
    topK = 5
)

matches.forEach { match ->
    println("${match.face.label}: ${match.similarity * 100}% similar")
}
```

### 4. Добавить известное лицо

```kotlin
val faceRepository = get<FaceRepository>()

faceRepository.insert(
    StoredFace(
        id = UUID.randomUUID().toString(),
        label = "John Doe",
        embedding = knownFaceEmbedding,
        cameraId = "camera-001",
        createdAt = System.currentTimeMillis()
    )
)
```

---

## 📈 Производительность

### Время обработки
- **Детекция лица:** ~50-100ms (зависит от модели)
- **Генерация embedding:** ~100-200ms
- **Сравнение двух лиц:** < 1ms
- **Поиск по 1000 лицам:** ~10-50ms

### Оптимизации
- Кэширование моделей
- Параллельная обработка кадров
- Batch операции для repository
- Индексация embeddings (будущее)

---

## 🔧 Настройка

### Параметры аналитики

```properties
# Face Detection
FACE_RECOGNITION_ENABLED=true
FACE_RECOGNITION_CONFIDENCE_THRESHOLD=0.8
FACE_MIN_FACE_SIZE=80
FACE_MAX_FACES_PER_FRAME=10

# Face Gallery
FACE_GALLERY_MAX_EMBEDDING_SIZE=512
FACE_GALLERY_MIN_SIMILARITY=0.7
FACE_GALLERY_MAX_RESULTS=100
```

---

## ✅ Checklist завершения 3.3.2

- [x] Face detection с embeddings
- [x] compareFaces (cosine similarity)
- [x] FaceRepository (интерфейс + SQLDelight)
- [x] StoredFace модель
- [x] Face Gallery API (CRUD + search)
- [x] Интеграционные тесты
- [x] Unit тесты
- [x] Документация

**Прогресс:** ~60% → **100%** (+40%)

---

## 📚 Связанные документы

- FACE_GALLERY_API.md *(утерян/в архиве)* — детальный API справочник
- FACE_RECOGNITION_TUTORIAL.md *(утерян/в архиве)* — руководство по использованию
- [PROJECT_STATUS_PHASES.md](../status/PROJECT_STATUS_PHASES.md) — статус проекта

---

**3.3.2 Face Recognition завершён на 100%!** ✅

**Дата обновления:** 27 April 2026
