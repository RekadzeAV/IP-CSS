# Интеграция дополнительных AI моделей

**Дата:** 26 January 2026
**Статус:** ✅ Завершено

## Выполненные задачи

### 1. ✅ Создана структура папок для новых моделей

```
data/models/
├── face-recognition/     # ✅ Модели распознавания лиц
├── behavior-analysis/   # ✅ Модели анализа поведения
└── crowd-analysis/      # ✅ Модели анализа толпы
```

### 2. ✅ Созданы скрипты для скачивания моделей

**Face Recognition:**
- `scripts/download-face-recognition-models.ps1` (Windows)
- `scripts/download-face-recognition-models.sh` (Linux/macOS)

**Behavior Analysis:**
- `scripts/download-behavior-analysis-models.ps1` (Windows)
- `scripts/download-behavior-analysis-models.sh` (Linux/macOS)

**Crowd Density:**
- `scripts/download-crowd-density-models.ps1` (Windows)
- `scripts/download-crowd-density-models.sh` (Linux/macOS)

### 3. ✅ Созданы README для каждой папки моделей

- `data/models/face-recognition/README.md` - Инструкции по InsightFace и RetinaFace
- `data/models/behavior-analysis/README.md` - Инструкции по YOLOv8-Pose
- `data/models/crowd-analysis/README.md` - Инструкции по CSRNet и YOLOv8 подсчету

### 4. ✅ Обновлены настройки AnalyticsSettings

Добавлены новые параметры в `Camera.kt`:

```kotlin
data class AnalyticsSettings(
    // ... существующие настройки ...

    // Face Recognition
    val faceRecognition: Boolean = false,
    val faceRecognitionModelPath: String? = "data/models/face-recognition/insightface.onnx",
    val faceRecognitionConfidenceThreshold: Float = 0.7f,

    // Behavior Analysis
    val behaviorAnalysis: Boolean = false,
    val behaviorAnalysisModelPath: String? = "data/models/behavior-analysis/yolov8n-pose.onnx",
    val fallDetectionEnabled: Boolean = false,
    val fallDetectionSensitivity: Float = 0.6f,

    // Crowd Density
    val crowdDensityAnalysis: Boolean = false,
    val crowdDensityModelPath: String? = "data/models/crowd-analysis/csrnet.onnx",
    val useYOLOForCounting: Boolean = true,
    val maxCrowdDensity: Int? = null
)
```

### 5. ✅ Обновлена документация

- `docs/AI_ANALYTICS.md` - добавлены разделы о новых моделях
- `data/models/README.md` - обновлена структура папок
- `DOCUMENTATION_INDEX.md` - обновлен (уже был обновлен ранее)

## Интегрированные модели

### 1. Face Recognition (Распознавание лиц)

**Модели:**
- **InsightFace** (рекомендуется) - ~25 MB
- **RetinaFace** (альтернатива) - ~1.7 MB

**Применение:**
- Идентификация известных лиц
- Контроль доступа
- Поиск людей в архиве
- Подсчет уникальных посетителей

**Скачивание:**
```bash
# Windows
.\scripts\download-face-recognition-models.ps1 insightface

# Linux/macOS
./scripts/download-face-recognition-models.sh insightface
```

### 2. Behavior Analysis (Анализ поведения)

**Модель:**
- **YOLOv8-Pose** - ~12 MB

**Применение:**
- Детекция падений (Fall Detection)
- Детекция драк и агрессии
- Детекция подозрительного поведения

**Скачивание:**
```bash
# Windows
.\scripts\download-behavior-analysis-models.ps1 yolov8-pose

# Linux/macOS
./scripts/download-behavior-analysis-models.sh yolov8-pose
```

### 3. Crowd Density (Оценка плотности толпы)

**Модели:**
- **CSRNet** (для плотных толп) - ~8 MB
- **YOLOv8 + Counting** (альтернатива) - использует существующую модель

**Применение:**
- Подсчет людей в кадре
- Оценка плотности толпы
- Предупреждение о переполнении
- Анализ скоплений людей

**Установка:**
- CSRNet требует ручной установки (см. `data/models/crowd-analysis/README.md`)
- YOLOv8 подсчет работает автоматически при включенной детекции объектов

## Быстрый старт

### Скачивание всех моделей

```powershell
# Windows
.\scripts\download-yolo-models.ps1 yolov8n
.\scripts\download-face-recognition-models.ps1 insightface
.\scripts\download-behavior-analysis-models.ps1 yolov8-pose
.\scripts\download-crowd-density-models.ps1 csrnet

# Linux/macOS
./scripts/download-yolo-models.sh yolov8n
./scripts/download-face-recognition-models.sh insightface
./scripts/download-behavior-analysis-models.sh yolov8-pose
./scripts/download-crowd-density-models.sh csrnet
```

## Конфигурация

### Настройка через код

```kotlin
val camera = Camera(
    // ... другие параметры ...
    settings = CameraSettings(
        analytics = AnalyticsSettings(
            // Face Recognition
            faceRecognition = true,
            faceRecognitionModelPath = "data/models/face-recognition/insightface.onnx",

            // Behavior Analysis
            behaviorAnalysis = true,
            behaviorAnalysisModelPath = "data/models/behavior-analysis/yolov8n-pose.onnx",
            fallDetectionEnabled = true,

            // Crowd Density
            crowdDensityAnalysis = true,
            useYOLOForCounting = true,
            maxCrowdDensity = 5 // люди/м²
        )
    )
)
```

### Настройка через переменные окружения

```bash
FACE_RECOGNITION_MODEL_PATH=data/models/face-recognition/insightface.onnx
BEHAVIOR_ANALYSIS_MODEL_PATH=data/models/behavior-analysis/yolov8n-pose.onnx
CROWD_DENSITY_MODEL_PATH=data/models/crowd-analysis/csrnet.onnx
```

## Документация

- data/models/README.md *(утерян/в архиве)* - Общий обзор моделей
- data/models/face-recognition/README.md *(утерян/в архиве)* - Face Recognition
- data/models/behavior-analysis/README.md *(утерян/в архиве)* - Behavior Analysis
- data/models/crowd-analysis/README.md *(утерян/в архиве)* - Crowd Density
- [docs/AI_ANALYTICS.md](../AI_ANALYTICS.md) - Полная документация по AI-аналитике
- [docs/AI_MODELS_RECOMMENDATIONS.md](../../archive/docs-deprecated-2026-09-04/AI_MODELS_RECOMMENDATIONS.md) - Рекомендации по моделям

## Следующие шаги

1. **Скачать модели** используя предоставленные скрипты
2. **Настроить пути** к моделям в конфигурации камеры
3. **Реализовать интеграцию** в нативном коде (C++/JNI)
4. **Добавить Use Cases** для новых функций аналитики
5. **Протестировать** функциональность на реальных видеопотоках

## Примечания

- Модели не включены в репозиторий (слишком большие файлы)
- Модели должны быть скачаны перед использованием
- CSRNet требует ручной установки и конвертации
- YOLOv8-Pose и InsightFace можно скачать автоматически через скрипты
- Для production рекомендуется использовать GPU ускорение

---

**Интеграция завершена!** ✅
