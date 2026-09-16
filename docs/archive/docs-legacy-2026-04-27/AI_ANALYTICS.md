# AI-Аналитика в системах видеонаблюдения

**Версия документации:** 1.0
**Дата создания:** 26 January 2026
**Версия проекта:** Alfa-0.0.1

> **📚 Полный индекс документации:** [DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)

---

## Содержание

1. [Обзор AI-аналитики](#обзор-ai-аналитики)
2. [Архитектура AI-аналитики](#архитектура-ai-аналитики)
3. [Компоненты AI-аналитики](#компоненты-ai-аналитики)
4. [Машинное зрение](#машинное-зрение)
5. [Распознавание номеров авто (ANPR)](#распознавание-номеров-авто-anpr)
6. [Детекция объектов](#детекция-объектов)
7. [Трекинг объектов](#трекинг-объектов)
8. [Детекция движения](#детекция-движения)
9. [Распознавание лиц](#распознавание-лиц)
10. [Интеграция с системой](#интеграция-с-системой)
11. [Технологии и библиотеки](#технологии-и-библиотеки)
12. [Производительность и оптимизация](#производительность-и-оптимизация)
13. [Рекомендации по внедрению](#рекомендации-по-внедрению)
14. [Размещение конвейера (on-prem / VSaaS)](#размещение-конвейера-on-prem--vsaas)

---

## Размещение конвейера (on-prem / VSaaS)

В доменной модели `AnalyticsSettings` добавлено поле **`executionLocation`** (`ON_PREM`, `CUSTOMER_SERVER`, `VSAAS`) — см. `shared/.../AnalyticsExecutionLocation.kt`. Значение сериализуется в настройках камеры и в DTO `CameraSettingsDto` / `AnalyticsSettingsDto` для клиентов.

Требования к облачному VSaaS и границы ответственности описаны в черновике **[VSaaS_REQUIREMENTS_DRAFT.md](planning/VSaaS_REQUIREMENTS_DRAFT.md)**. Облачная доставка кадров в продакшене **не реализована** в текущей ветке.

---

## Обзор AI-аналитики

AI-аналитика в системах видеонаблюдения представляет собой применение технологий искусственного интеллекта и машинного обучения для автоматизированной обработки и интерпретации видеоданных в реальном времени и в архивных записях.

### Основные возможности

- **Детекция объектов**: Автоматическое обнаружение людей, транспортных средств, животных и других объектов в видеопотоке
- **Трекинг объектов**: Отслеживание перемещения объектов между кадрами
- **Распознавание номеров авто (ANPR)**: Автоматическое считывание и распознавание номерных знаков транспортных средств
- **Детекция движения**: Выявление движения в заданных зонах наблюдения
- **Распознавание лиц**: Идентификация и распознавание лиц в видеопотоке
- **Анализ поведения**: Выявление аномалий, подозрительного поведения, скоплений людей
- **Классификация событий**: Автоматическая категоризация событий по типам

### Преимущества AI-аналитики

1. **Повышение точности**: Снижение количества ложных срабатываний до 90%
2. **Автоматизация**: Уменьшение необходимости в ручном мониторинге на 70-80%
3. **Скорость реагирования**: Оперативное выявление инцидентов в реальном времени
4. **Масштабируемость**: Обработка множества видеопотоков одновременно
5. **Снижение затрат**: Оптимизация использования ресурсов и персонала
6. **Аналитика**: Получение статистических данных и аналитических отчетов

---

## Архитектура AI-аналитики

### Общая архитектура

```
┌─────────────────────────────────────────────────────────────┐
│                    Видеопоток (RTSP/ONVIF)                  │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│              Обработка кадров (Frame Processor)              │
│  - Декодирование видео                                       │
│  - Извлечение кадров                                         │
│  - Предобработка (нормализация, ресайз)                      │
└───────────────────────┬─────────────────────────────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
        ▼               ▼               ▼
┌───────────────┐ ┌───────────────┐ ┌───────────────┐
│  Детекция     │ │  ANPR Engine  │ │  Детекция     │
│  объектов     │ │               │ │  движения     │
└───────┬───────┘ └───────┬───────┘ └───────┬───────┘
        │                 │                 │
        └───────────────┬─┴─────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│                    Трекинг объектов                          │
│  - Сопоставление объектов между кадрами                      │
│  - Присвоение уникальных ID                                 │
│  - Отслеживание траекторий                                  │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│              Генерация событий и уведомлений                 │
│  - Создание событий на основе детекций                       │
│  - Фильтрация по правилам                                    │
│  - Отправка уведомлений                                      │
└───────────────────────┬─────────────────────────────────────┘
                        │
                        ▼
┌─────────────────────────────────────────────────────────────┐
│              Хранение результатов                             │
│  - Сохранение метаданных в БД                                │
│  - Привязка к видеозаписям                                   │
│  - Индексация для быстрого поиска                            │
└─────────────────────────────────────────────────────────────┘
```

### Компоненты системы

1. **Нативные C++ библиотеки** (`native/analytics/`)
   - Высокопроизводительная обработка видео
   - Интеграция с OpenCV, TensorFlow Lite
   - GPU ускорение (CUDA, OpenCL)

2. **Kotlin Multiplatform обертки** (`shared/src/commonMain/`)
   - Кроссплатформенный API
   - Управление жизненным циклом аналитики
   - Интеграция с бизнес-логикой

3. **Серверные сервисы** (`server/api/`)
   - REST API для управления аналитикой
   - WebSocket для real-time событий
   - Хранение результатов анализа

4. **Веб-интерфейс** (`server/web/`)
   - Визуализация результатов анализа
   - Настройка зон детекции
   - Просмотр событий и статистики

---

## Компоненты AI-аналитики

### 1. Детектор объектов (Object Detector)

**Назначение**: Обнаружение объектов различных типов в видеокадрах.

**Поддерживаемые типы объектов**:
- `OBJECT_TYPE_PERSON` - Люди
- `OBJECT_TYPE_VEHICLE` - Транспортные средства
- `OBJECT_TYPE_BICYCLE` - Велосипеды
- `OBJECT_TYPE_MOTORCYCLE` - Мотоциклы
- `OBJECT_TYPE_UNKNOWN` - Неопознанные объекты

**Технологии**:
- **YOLO (You Only Look Once)** - Быстрая детекция объектов в реальном времени
- **TensorFlow Lite** - Легковесные модели для мобильных устройств
- **OpenCV DNN** - Универсальный интерфейс для различных моделей

**Параметры**:
```cpp
typedef struct {
    float confidenceThreshold;  // Минимальный порог уверенности (0.0-1.0)
    int maxObjects;            // Максимальное количество объектов
    bool useGPU;                // Использовать GPU ускорение
} ObjectDetectorParams;
```

**API**:
```cpp
// Создание детектора
ObjectDetector* object_detector_create(const ObjectDetectorParams* params);

// Загрузка модели
bool object_detector_load_model(ObjectDetector* detector, const char* modelPath);

// Детекция объектов в кадре
bool object_detector_detect(
    ObjectDetector* detector,
    const uint8_t* frameData,
    int width,
    int height,
    DetectionResult* result
);
```

**Модели**:
- YOLOv8 - Рекомендуется для высокой точности
- YOLOv5 - Баланс между точностью и скоростью
- MobileNet-SSD - Для мобильных устройств

### 2. ANPR Engine (Распознавание номеров авто)

**Назначение**: Автоматическое распознавание номерных знаков транспортных средств.

**Точность**: До 99% при оптимальных условиях (хорошее освещение, правильный угол, качество камеры)

**Скорость**: Обработка до 200 км/ч движения автомобиля

**Процесс распознавания**:

1. **Детекция номерного знака**:
   - Поиск прямоугольных областей с характерными пропорциями
   - Фильтрация по размеру и соотношению сторон
   - Использование контурного анализа

2. **Предобработка**:
   - Преобразование в оттенки серого
   - Применение фильтров (Gaussian Blur, Adaptive Threshold)
   - Улучшение контраста и резкости

3. **Сегментация символов**:
   - Разделение номера на отдельные символы
   - Коррекция наклона и перспективы
   - Нормализация размера символов

4. **OCR (Optical Character Recognition)**:
   - Распознавание символов через Tesseract OCR или специализированные модели
   - Поддержка различных форматов номеров (российские, европейские, американские)
   - Валидация и коррекция результатов

**Параметры**:
```cpp
typedef struct {
    float confidenceThreshold;  // Минимальный порог уверенности
    const char* language;        // Язык для OCR (например, "eng", "rus")
} ANPREngineParams;
```

**API**:
```cpp
// Создание движка ANPR
ANPREngine* anpr_engine_create(const ANPREngineParams* params);

// Инициализация OCR
bool anpr_engine_init_ocr(ANPREngine* engine);

// Распознавание номеров в кадре
bool anpr_engine_recognize(
    ANPREngine* engine,
    const uint8_t* frameData,
    int width,
    int height,
    ANPRResult* result
);
```

**Применение**:
- Контроль въезда/выезда на объекты
- Автоматизация парковок
- Учет рабочего времени
- Интеграция с системами доступа (шлагбаумы, ворота)
- Поиск транспортных средств по номеру

### 3. Детектор движения (Motion Detector)

**Назначение**: Выявление движения в видеопотоке для оптимизации записи и генерации событий.

**Алгоритмы**:
- **MOG2 (Mixture of Gaussians)** - Адаптивный алгоритм вычитания фона
- **Frame Difference** - Сравнение последовательных кадров
- **Optical Flow** - Отслеживание оптического потока

**Параметры**:
```cpp
typedef struct {
    float threshold;        // Порог чувствительности (0.0-1.0)
    int minArea;           // Минимальная область движения в пикселях
    bool useGaussianBlur;  // Использовать размытие для уменьшения шума
    int blurSize;          // Размер ядра размытия
} MotionDetectorParams;
```

**API**:
```cpp
// Создание детектора движения
MotionDetector* motion_detector_create(
    int width,
    int height,
    const MotionDetectorParams* params
);

// Детекция движения в кадре
bool motion_detector_detect(
    MotionDetector* detector,
    const uint8_t* frameData,
    int width,
    int height,
    MotionDetectionResult* result
);
```

**Применение**:
- Запись по событиям (вместо непрерывной записи)
- Генерация уведомлений о движении
- Оптимизация использования хранилища
- Определение зон активности

### 4. Трекер объектов (Object Tracker)

**Назначение**: Отслеживание объектов между кадрами для анализа траекторий движения.

**Алгоритмы**:
- **IoU (Intersection over Union) Matching** - Сопоставление по пересечению областей
- **Kalman Filter** - Предсказание положения объектов
- **DeepSORT** - Глубокое обучение для трекинга

**Параметры**:
```cpp
typedef struct {
    float iouThreshold;     // Порог IoU для сопоставления
    int maxAge;             // Максимальный возраст объекта без обновления (в кадрах)
    float minConfidence;    // Минимальная уверенность для инициализации трека
} ObjectTrackerParams;
```

**API**:
```cpp
// Создание трекера
ObjectTracker* object_tracker_create(const ObjectTrackerParams* params);

// Обновление треков на основе новых детекций
bool object_tracker_update(
    ObjectTracker* tracker,
    const DetectionResult* detections,
    TrackingResult* result
);
```

**Применение**:
- Анализ траекторий движения
- Подсчет объектов (людей, автомобилей)
- Выявление аномального поведения
- Определение времени пребывания в зоне

### 5. Детектор лиц (Face Detector)

**Назначение**: Обнаружение и распознавание лиц в видеопотоке.

**Технологии**:
- **Haar Cascades** - Классический метод детекции
- **DNN (Deep Neural Networks)** - Современные модели на основе глубокого обучения
- **MTCNN** - Multi-task Cascaded Convolutional Networks

**Параметры**:
```cpp
typedef struct {
    float scaleFactor;        // Фактор масштабирования для каскада
    int minNeighbors;         // Минимальное количество соседей
    int minSize;              // Минимальный размер лица
    int maxSize;              // Максимальный размер лица
} FaceDetectorParams;
```

**API**:
```cpp
// Создание детектора лиц
FaceDetector* face_detector_create(const FaceDetectorParams* params);

// Загрузка модели каскада
bool face_detector_load_cascade(FaceDetector* detector, const char* cascadePath);

// Детекция лиц в кадре
bool face_detector_detect(
    FaceDetector* detector,
    const uint8_t* frameData,
    int width,
    int height,
    FaceDetectionResult* result
);
```

**Применение**:
- Контроль доступа
- Идентификация людей
- Подсчет посетителей
- Поиск по лицам в архиве

---

## Машинное зрение

### Определение

Машинное зрение (Computer Vision) - это область искусственного интеллекта, которая позволяет компьютерам интерпретировать и понимать визуальную информацию из изображений и видеопотоков.

### Компоненты машинного зрения

1. **Получение изображения**
   - Захват кадров из видеопотока
   - Предобработка (нормализация, коррекция освещения)

2. **Обработка изображения**
   - Фильтрация шума
   - Улучшение качества
   - Сегментация

3. **Извлечение признаков**
   - Детекция краев и контуров
   - Выделение ключевых точек
   - Извлечение дескрипторов

4. **Распознавание и классификация**
   - Идентификация объектов
   - Классификация по категориям
   - Распознавание текста

5. **Принятие решений**
   - Генерация событий
   - Триггеры действий
   - Аналитические выводы

### Применение в видеонаблюдении

- **Контроль качества**: Проверка соответствия стандартам
- **Безопасность**: Выявление угроз и аномалий
- **Автоматизация**: Управление процессами без участия человека
- **Аналитика**: Сбор статистики и метрик

---

## Распознавание номеров авто (ANPR)

### Обзор технологии

ANPR (Automatic Number Plate Recognition) или LPR (License Plate Recognition) - технология автоматического распознавания номерных знаков транспортных средств с использованием оптического распознавания символов (OCR).

### Точность и производительность

- **Точность распознавания**: 95-99% при оптимальных условиях
- **Скорость обработки**: До 200 км/ч движения автомобиля
- **Время обработки**: 50-200 мс на кадр (зависит от оборудования)
- **Поддержка форматов**: Российские, европейские, американские номера

### Факторы, влияющие на точность

1. **Качество камеры**:
   - Разрешение (минимум 2MP, рекомендуется 4MP+)
   - Чувствительность в условиях низкой освещенности
   - Широкий динамический диапазон (WDR)

2. **Условия съемки**:
   - Освещение (дневное, ночное, искусственное)
   - Угол обзора (рекомендуется 15-30 градусов)
   - Расстояние до объекта (оптимально 3-10 метров)
   - Скорость движения (до 200 км/ч)

3. **Качество номерного знака**:
   - Чистота и состояние
   - Стандартизация формата
   - Контрастность символов

### Алгоритм распознавания

#### Этап 1: Детекция номерного знака

```cpp
// Поиск прямоугольных областей с характерными пропорциями
cv::Mat gray;
cv::cvtColor(frame, gray, cv::COLOR_RGB2GRAY);

// Предобработка
cv::Mat processed;
cv::GaussianBlur(gray, processed, cv::Size(5, 5), 0);
cv::adaptiveThreshold(processed, processed, 255,
    cv::ADAPTIVE_THRESH_GAUSSIAN_C, cv::THRESH_BINARY, 11, 2);

// Поиск контуров
std::vector<std::vector<cv::Point>> contours;
cv::findContours(processed, contours, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_SIMPLE);

// Фильтрация по размеру и соотношению сторон
for (const auto& contour : contours) {
    cv::Rect rect = cv::boundingRect(contour);
    float aspectRatio = static_cast<float>(rect.width) / rect.height;

    // Номерные знаки обычно имеют соотношение сторон 1.5-5.0
    if (aspectRatio > 1.5f && aspectRatio < 5.0f && rect.area() > 1000) {
        // Обнаружен потенциальный номерной знак
    }
}
```

#### Этап 2: Предобработка области номера

- Коррекция перспективы (если номер под углом)
- Нормализация размера
- Улучшение контраста
- Бинаризация

#### Этап 3: Сегментация символов

- Разделение номера на отдельные символы
- Удаление шума и артефактов
- Выравнивание символов

#### Этап 4: OCR распознавание

**Tesseract OCR**:
```cpp
// Инициализация Tesseract
tesseract::TessBaseAPI *ocr = new tesseract::TessBaseAPI();
ocr->Init(NULL, "rus+eng");  // Русский и английский языки

// Распознавание
ocr->SetImage(plateROI.data, plateROI.cols, plateROI.rows,
              1, plateROI.step);
char* text = ocr->GetUTF8Text();
```

**Специализированные модели**:
- Модели, обученные специально на номерных знаках
- Более высокая точность для конкретных форматов
- Поддержка различных шрифтов и стилей

#### Этап 5: Валидация и коррекция

- Проверка формата номера (регулярные выражения)
- Коррекция типичных ошибок OCR
- Проверка контрольных сумм (для некоторых форматов)

### Интеграция с системой

```kotlin
// Kotlin Multiplatform API
class ANPRService {
    suspend fun recognizePlate(
        frame: ByteArray,
        width: Int,
        height: Int
    ): ANPRResult {
        // Вызов нативной C++ функции
        return nativeRecognizePlate(frame, width, height)
    }

    suspend fun processVideoStream(
        streamId: String,
        callback: (ANPRResult) -> Unit
    ) {
        // Обработка видеопотока в реальном времени
    }
}
```

### Применение

1. **Контроль доступа**:
   - Автоматическое открытие шлагбаумов и ворот
   - Белый/черный список номеров
   - Интеграция с системами контроля доступа

2. **Парковки**:
   - Автоматический учет времени парковки
   - Расчет стоимости
   - Поиск автомобилей

3. **Безопасность**:
   - Поиск разыскиваемых транспортных средств
   - Мониторинг подозрительных автомобилей
   - Анализ трафика

4. **Логистика**:
   - Учет рабочего времени водителей
   - Контроль маршрутов
   - Оптимизация процессов

---

## Детекция объектов

### Модели детекции

#### YOLO (You Only Look Once)

**Преимущества**:
- Очень высокая скорость обработки (30+ FPS)
- Хорошая точность
- Единая модель для всех классов объектов

**Версии**:
- **YOLOv8** (2023) - Последняя версия, лучшая точность
- **YOLOv7** (2022) - Улучшенная архитектура
- **YOLOv5** (2020) - Популярная и стабильная версия
- **YOLOv4** (2020) - Хороший баланс скорости и точности

#### Расположение и скачивание моделей

Все модели AI хранятся в папке `data/models/` проекта. По умолчанию используется модель YOLOv8n (Nano).

**Структура папки моделей**:
```
data/models/
├── yolov8n.onnx          # YOLOv8 Nano (по умолчанию, ~6 MB)
├── README.md             # Инструкции по использованию моделей
│
├── face-recognition/     # ✅ Модели распознавания лиц
│   ├── insightface.onnx  # InsightFace (рекомендуется)
│   ├── retinaface.onnx  # RetinaFace (альтернатива)
│   └── README.md        # Подробные инструкции
│
├── behavior-analysis/    # ✅ Модели анализа поведения
│   ├── yolov8n-pose.onnx # YOLOv8-Pose для детекции падений
│   └── README.md        # Подробные инструкции
│
└── crowd-analysis/       # ✅ Модели анализа толпы
    ├── csrnet.onnx      # CSRNet для оценки плотности
    └── README.md        # Подробные инструкции
```

**Подробная информация**: См. [data/models/README.md](../data/models/README.md)

**Скачивание моделей**:
- Windows: `.\scripts\download-yolo-models.ps1 yolov8n`
- Linux/macOS: `./scripts/download-yolo-models.sh yolov8n`

**Настройка пути к модели**:
- По умолчанию: `data/models/yolov8n.onnx`
- Через переменную окружения: `AI_MODEL_PATH=data/models/yolov8n.onnx`
- В настройках камеры: `camera.settings.analytics.objectDetectionModelPath`

**Использование**:
```cpp
// Загрузка модели YOLO через OpenCV DNN
cv::dnn::Net net = cv::dnn::readNetFromONNX("data/models/yolov8n.onnx");

// Подготовка входного блоба
cv::Mat blob;
cv::dnn::blobFromImage(frame, blob, 1.0/255.0,
    cv::Size(640, 640), cv::Scalar(0, 0, 0), true, false);

// Инференс
net.setInput(blob);
std::vector<cv::Mat> outputs;
net.forward(outputs, net.getUnconnectedOutLayersNames());

// Парсинг результатов
// outputs содержит bounding boxes, confidence scores, class IDs
```

#### TensorFlow Lite

**Преимущества**:
- Легковесные модели для мобильных устройств
- Оптимизация для edge computing
- Поддержка GPU ускорения

**Использование**:
```cpp
// Загрузка модели TensorFlow Lite
std::unique_ptr<tflite::FlatBufferModel> model =
    tflite::FlatBufferModel::BuildFromFile("model.tflite");

tflite::ops::builtin::BuiltinOpResolver resolver;
std::unique_ptr<tflite::Interpreter> interpreter;
tflite::InterpreterBuilder builder(*model, resolver);
builder(&interpreter);

// Выделение тензоров
interpreter->AllocateTensors();

// Инференс
interpreter->Invoke();
```

### Классы объектов

Система поддерживает детекцию следующих классов:

1. **Люди** (`OBJECT_TYPE_PERSON`):
   - Пешеходы
   - Люди в различных позах
   - Частично скрытые люди

2. **Транспортные средства** (`OBJECT_TYPE_VEHICLE`):
   - Легковые автомобили
   - Грузовики
   - Автобусы

3. **Двухколесный транспорт**:
   - Велосипеды (`OBJECT_TYPE_BICYCLE`)
   - Мотоциклы (`OBJECT_TYPE_MOTORCYCLE`)

### Оптимизация производительности

1. **Масштабирование входного изображения**:
   - Уменьшение разрешения для ускорения
   - Баланс между точностью и скоростью

2. **Пропуск кадров**:
   - Обработка каждого N-го кадра
   - Интерполяция результатов между кадрами

3. **GPU ускорение**:
   - CUDA для NVIDIA GPU
   - OpenCL для различных GPU
   - Neural Processing Units (NPU)

4. **Квантование моделей**:
   - INT8 квантование для ускорения
   - Минимальная потеря точности

---

## Анализ поведения (Behavior Analysis)

### Обзор

Анализ поведения позволяет обнаруживать критические события, такие как падения людей, драки, агрессию и подозрительное поведение.

### Модели анализа поведения

#### YOLOv8-Pose (Рекомендуется)

**Описание:** Модель для детекции объектов и оценки позы человека в одной модели.

**Характеристики:**
- Размер: ~12 MB (nano версия)
- Скорость: 8-15 FPS (CPU), 60-120 FPS (GPU)
- Поддержка: Детекция + оценка позы (17 ключевых точек)

**Расположение модели:**
- Путь: `data/models/behavior-analysis/yolov8n-pose.onnx`
- По умолчанию: `camera.settings.analytics.behaviorAnalysisModelPath`

**Скачивание:**
```bash
# Windows
.\scripts\download-behavior-analysis-models.ps1 yolov8-pose

# Linux/macOS
./scripts/download-behavior-analysis-models.sh yolov8-pose
```

**Подробная информация**: См. [data/models/behavior-analysis/README.md](../data/models/behavior-analysis/README.md)

### Детекция падений (Fall Detection)

Анализ позы человека для определения падения:

**Алгоритм:**
1. Детекция человека через YOLOv8-Pose
2. Извлечение ключевых точек позы (17 точек)
3. Анализ углов между частями тела
4. Определение ориентации тела
5. Детекция падения при определенных условиях

**Критерии падения:**
- Лежачее положение на горизонтальной поверхности
- Резкое изменение позы (вертикальная → горизонтальная)
- Отсутствие движения после падения
- Низкая высота центра масс тела

**Настройки:**
```kotlin
camera.settings.analytics.fallDetectionEnabled = true
camera.settings.analytics.fallDetectionSensitivity = 0.6f // 0.0 - 1.0
```

### Детекция драк и агрессии

Анализ взаимодействия между людьми:
- Близкое расположение людей (< 1 метра)
- Агрессивные позы (поднятые руки, наклон вперед)
- Быстрые движения рук
- Множественные люди в одной области

### Детекция подозрительного поведения

- Длительное нахождение в одной позе
- Необычные движения
- Взаимодействие с объектами
- Нахождение в запрещенных зонах

---

## Оценка плотности толпы (Crowd Density)

### Обзор

Оценка плотности толпы позволяет подсчитывать количество людей в кадре и оценивать плотность скоплений для безопасности и аналитики.

### Модели оценки плотности

#### CSRNet (Рекомендуется для плотных толп)

**Описание:** Модель для точной оценки плотности толпы и подсчета людей.

**Характеристики:**
- Размер: ~8 MB
- Скорость: 5-10 FPS (CPU), 40-80 FPS (GPU)
- Точность: Высокая для плотных толп
- Поддержка: Оценка плотности + подсчет

**Расположение модели:**
- Путь: `data/models/crowd-analysis/csrnet.onnx`
- По умолчанию: `camera.settings.analytics.crowdDensityModelPath`

**Установка:** Требует ручной установки (см. инструкции в README)

**Подробная информация**: См. [data/models/crowd-analysis/README.md](../data/models/crowd-analysis/README.md)

#### YOLOv8 + Counting (Альтернатива)

**Описание:** Использование YOLOv8 для детекции людей с последующим подсчетом.

**Преимущества:**
- Уже интегрирована в систему
- Быстрая и легковесная
- Хорошо работает для небольших групп

**Недостатки:**
- Менее точна для очень плотных толп
- Требует видимости каждого человека

**Настройки:**
```kotlin
camera.settings.analytics.useYOLOForCounting = true
camera.settings.analytics.objectDetectionModelPath = "data/models/yolov8n.onnx"
```

### Алгоритм подсчета на основе YOLOv8

1. Детекция всех объектов типа "person" через YOLOv8
2. Фильтрация по зонам (если заданы)
3. Подсчет уникальных детекций
4. Применение трекинга для избежания двойного подсчета
5. Агрегация результатов

### Метрики плотности

- **Количество людей в кадре**: Абсолютное число обнаруженных людей
- **Плотность (люди/м²)**: Количество людей на единицу площади
- **Карта плотности (heatmap)**: Визуализация распределения плотности
- **Скорость изменения**: Динамика изменения плотности во времени

### Применение

- Подсчет посетителей в торговых центрах
- Мониторинг скоплений людей на мероприятиях
- Предупреждение о переполнении помещений
- Анализ трафика в общественных местах
- Безопасность (избежание давки)

**Настройки:**
```kotlin
camera.settings.analytics.crowdDensityAnalysis = true
camera.settings.analytics.maxCrowdDensity = 5 // люди/м² для предупреждений
```

---

## Трекинг объектов

### Алгоритмы трекинга

#### IoU Matching

Простой и эффективный алгоритм для базового трекинга:

```cpp
// Вычисление IoU (Intersection over Union)
float calculateIoU(const cv::Rect& box1, const cv::Rect& box2) {
    cv::Rect intersection = box1 & box2;
    cv::Rect union_rect = box1 | box2;

    float intersectionArea = intersection.area();
    float unionArea = union_rect.area();

    return intersectionArea / unionArea;
}

// Сопоставление детекций с существующими треками
void matchDetectionsToTracks(
    const std::vector<DetectedObject>& detections,
    std::vector<Track>& tracks
) {
    for (auto& track : tracks) {
        float bestIoU = 0.0f;
        int bestMatch = -1;

        for (size_t i = 0; i < detections.size(); i++) {
            float iou = calculateIoU(track.lastBox, detections[i].box);
            if (iou > bestIoU && iou > track.params.iouThreshold) {
                bestIoU = iou;
                bestMatch = i;
            }
        }

        if (bestMatch >= 0) {
            // Обновление трека
            track.update(detections[bestMatch]);
        } else {
            // Увеличение возраста трека
            track.age++;
        }
    }
}
```

#### Kalman Filter

Предсказание положения объектов для более стабильного трекинга:

```cpp
// Использование Kalman Filter для предсказания
cv::KalmanFilter kf(4, 2, 0);  // 4 состояния, 2 измерения

// Инициализация
kf.statePre.at<float>(0) = x;  // x координата
kf.statePre.at<float>(1) = y;  // y координата
kf.statePre.at<float>(2) = 0;   // скорость по x
kf.statePre.at<float>(3) = 0;   // скорость по y

// Предсказание
cv::Mat prediction = kf.predict();

// Коррекция на основе измерения
cv::Mat measurement = (cv::Mat_<float>(2, 1) << measuredX, measuredY);
cv::Mat estimated = kf.correct(measurement);
```

#### DeepSORT

Продвинутый алгоритм с использованием глубокого обучения:

- Извлечение признаков объектов через CNN
- Сопоставление по признакам и положению
- Высокая точность при перекрытиях и окклюзиях

### Применение трекинга

1. **Подсчет объектов**:
   - Подсчет людей, входящих/выходящих
   - Подсчет транспортных средств
   - Анализ трафика

2. **Анализ траекторий**:
   - Выявление аномальных маршрутов
   - Определение зон активности
   - Анализ поведения

3. **Время пребывания**:
   - Измерение времени в зоне
   - Анализ эффективности пространства
   - Оптимизация процессов

### REST API и формат треков для UI

- **GET** `/api/v1/cameras/{id}/analytics/tracks` — возвращает текущие активные треки по камере (последний результат трекинга).
- Формат элемента трека для отрисовки поверх видео:
  - `trackId` (Int) — стабильный ID объекта между кадрами
  - `objectType` (String) — тип: person, vehicle, bicycle, motorcycle, unknown
  - `boundingBox` — `{ x, y, width, height }` в пикселях кадра
  - `confidence` (Float) — уверенность 0.0–1.0
  - `lastSeen` (Long) — временная метка последнего обнаружения (мс)

---

## Детекция движения

### Алгоритмы детекции движения

#### MOG2 (Mixture of Gaussians)

Адаптивный алгоритм вычитания фона:

```cpp
// Создание MOG2 детектора
cv::Ptr<cv::BackgroundSubtractor> bgSubtractor =
    cv::createBackgroundSubtractorMOG2(500, 16.0, false);

// Применение к кадру
cv::Mat fgMask;
bgSubtractor->apply(frame, fgMask);

// Поиск контуров движения
std::vector<std::vector<cv::Point>> contours;
cv::findContours(fgMask, contours, cv::RETR_EXTERNAL, cv::CHAIN_APPROX_SIMPLE);
```

**Параметры**:
- `history` - Количество кадров для обучения фона (500)
- `varThreshold` - Порог вариации (16.0)
- `detectShadows` - Обнаружение теней (false)

#### Frame Difference

Простое сравнение последовательных кадров:

```cpp
// Разница между текущим и предыдущим кадром
cv::Mat diff;
cv::absdiff(currentFrame, previousFrame, diff);

// Бинаризация
cv::Mat binary;
cv::threshold(diff, binary, threshold, 255, cv::THRESH_BINARY);
```

### Зоны детекции

Система поддерживает настройку зон детекции движения:

```kotlin
data class DetectionZone(
    val name: String,
    val polygon: List<Point>,  // Многоугольник зоны
    val sensitivity: Int = 80,  // Чувствительность 0-100
    val enabled: Boolean = true
)
```

**Применение зон**:
- Игнорирование движения вне важных областей
- Различная чувствительность для разных зон
- Генерация событий только для определенных зон

---

## Распознавание лиц

### Технологии

#### Haar Cascades

Классический метод на основе каскадов признаков:

```cpp
// Загрузка каскада
cv::CascadeClassifier faceCascade;
faceCascade.load("haarcascade_frontalface_alt.xml");

// Детекция лиц
std::vector<cv::Rect> faces;
faceCascade.detectMultiScale(
    grayFrame,
    faces,
    1.1,      // scaleFactor
    3,        // minNeighbors
    0,        // flags
    cv::Size(30, 30),  // minSize
    cv::Size()         // maxSize
);
```

#### DNN (Deep Neural Networks)

Современные модели на основе глубокого обучения:

```cpp
// Загрузка модели DNN
cv::dnn::Net faceNet = cv::dnn::readNetFromTensorflow(
    "opencv_face_detector_uint8.pb",
    "opencv_face_detector.pbtxt"
);

// Подготовка блоба
cv::Mat blob = cv::dnn::blobFromImage(
    frame, 1.0, cv::Size(300, 300),
    cv::Scalar(104, 177, 123)
);

// Инференс
faceNet.setInput(blob);
cv::Mat detection = faceNet.forward();
```

### Распознавание лиц

После детекции лица можно распознать через:

1. **Face Embeddings**:
   - Извлечение признаков лица
   - Сравнение с базой данных
   - Идентификация человека

2. **Face Recognition библиотеки**:
   - FaceNet
   - ArcFace
   - InsightFace

---

## Интеграция с системой

### Архитектура интеграции

```
┌─────────────────────────────────────────────────────────┐
│              Видеопоток (RTSP/ONVIF)                     │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│         VideoStreamService (Kotlin)                      │
│  - Управление видеопотоками                             │
│  - Извлечение кадров                                    │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│         AnalyticsService (Kotlin)                        │
│  - Координация AI-аналитики                              │
│  - Управление детекторами                                │
│  - Обработка результатов                                 │
└───────┬───────────┬───────────┬───────────┬─────────────┘
        │           │           │           │
        ▼           ▼           ▼           ▼
┌──────────┐ ┌──────────┐ ┌──────────┐ ┌──────────┐
│ Object   │ │   ANPR   │ │ Motion   │ │  Face    │
│Detector  │ │  Engine  │ │Detector  │ │Detector  │
│ (C++)    │ │  (C++)   │ │  (C++)   │ │  (C++)   │
└────┬─────┘ └────┬──────┘ └────┬─────┘ └────┬─────┘
     │           │             │            │
     └───────────┴─────────────┴────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│         ObjectTracker (C++)                              │
│  - Трекинг объектов                                      │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│         EventService (Kotlin)                            │
│  - Генерация событий                                     │
│  - Фильтрация по правилам                                │
│  - Отправка уведомлений                                  │
└──────────────────────┬──────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────┐
│         Database & WebSocket                              │
│  - Сохранение результатов                                │
│  - Real-time уведомления                                 │
└─────────────────────────────────────────────────────────┘
```

### Kotlin Multiplatform API

```kotlin
// Общий интерфейс для аналитики
interface VideoAnalytics {
    suspend fun initialize()
    suspend fun processFrame(frame: VideoFrame): AnalyticsResult
    suspend fun shutdown()
}

// Реализация для Android/iOS/Desktop
class VideoAnalyticsImpl : VideoAnalytics {
    private val objectDetector: ObjectDetector
    private val anprEngine: ANPREngine
    private val motionDetector: MotionDetector
    private val objectTracker: ObjectTracker

    override suspend fun processFrame(frame: VideoFrame): AnalyticsResult {
        // Параллельная обработка различными детекторами
        val detections = async { objectDetector.detect(frame) }
        val plates = async { anprEngine.recognize(frame) }
        val motion = async { motionDetector.detect(frame) }

        // Ожидание результатов
        val objects = detections.await()
        val recognizedPlates = plates.await()
        val motionResult = motion.await()

        // Трекинг объектов
        val tracks = objectTracker.update(objects)

        return AnalyticsResult(
            objects = objects,
            plates = recognizedPlates,
            motion = motionResult,
            tracks = tracks
        )
    }
}
```

### REST API

```kotlin
// Настройка аналитики для камеры
@POST("/api/v1/cameras/{id}/analytics")
suspend fun configureAnalytics(
    @Path id: String,
    @Body config: AnalyticsConfig
): Response

// Получение результатов аналитики
@GET("/api/v1/cameras/{id}/analytics/results")
suspend fun getAnalyticsResults(
    @Path id: String,
    @Query from: Long,
    @Query to: Long
): List<AnalyticsResult>

// WebSocket для real-time событий
@WebSocket("/api/v1/analytics/events")
suspend fun analyticsEvents(webSocket: WebSocketSession)
```

### Конфигурация

```kotlin
data class AnalyticsConfig(
    val motionDetection: MotionDetectionConfig,
    val objectDetection: ObjectDetectionConfig,
    val anpr: ANPRConfig,
    val faceDetection: FaceDetectionConfig,
    val zones: List<DetectionZone>
)

data class MotionDetectionConfig(
    val enabled: Boolean = true,
    val threshold: Float = 0.5f,
    val minArea: Int = 500,
    val zones: List<DetectionZone> = emptyList()
)

data class ObjectDetectionConfig(
    val enabled: Boolean = false,
    val confidenceThreshold: Float = 0.5f,
    val objectTypes: List<ObjectType> = emptyList(),
    val tracking: Boolean = true
)

data class ANPRConfig(
    val enabled: Boolean = false,
    val confidenceThreshold: Float = 0.7f,
    val language: String = "rus+eng",
    val whitelist: List<String> = emptyList(),
    val blacklist: List<String> = emptyList()
)
```

---

## Технологии и библиотеки

### OpenCV

**Версия**: 4.8+
**Использование**: Основная библиотека для обработки изображений и компьютерного зрения

**Возможности**:
- Обработка изображений (фильтры, морфология, геометрические преобразования)
- Детекция объектов (Haar Cascades, DNN)
- Вычитание фона (MOG2, KNN)
- Работа с контурами
- Оптическая потоковая обработка

**Интеграция**:
```cmake
find_package(OpenCV REQUIRED)
target_link_libraries(analytics PRIVATE ${OpenCV_LIBS})
target_include_directories(analytics PRIVATE ${OpenCV_INCLUDE_DIRS})
```

### TensorFlow Lite

**Версия**: 2.14+
**Использование**: Легковесные модели для детекции объектов

**Преимущества**:
- Оптимизация для мобильных устройств
- Квантование моделей (INT8)
- GPU ускорение
- Кроссплатформенность

**Интеграция**:
```cmake
set(TFLITE_DIR "${CMAKE_SOURCE_DIR}/third_party/tensorflow")
include_directories(${TFLITE_DIR}/tensorflow/lite/c)
```

### Tesseract OCR

**Версия**: 5.0+
**Использование**: Распознавание текста в номерных знаках

**Возможности**:
- Поддержка множества языков
- Высокая точность для печатного текста
- Настраиваемые параметры распознавания

**Интеграция**:
```cpp
#include <tesseract/baseapi.h>

tesseract::TessBaseAPI *ocr = new tesseract::TessBaseAPI();
ocr->Init(NULL, "rus+eng");
```

### FFmpeg

**Версия**: 6.0+
**Использование**: Декодирование видеопотоков, извлечение кадров

**Интеграция**: Через нативный модуль `native/video-processing/`

### GPU ускорение

#### CUDA (NVIDIA)

```cpp
#ifdef ENABLE_CUDA
    detector->dnnNet.setPreferableBackend(cv::dnn::DNN_BACKEND_CUDA);
    detector->dnnNet.setPreferableTarget(cv::dnn::DNN_TARGET_CUDA);
#endif
```

#### OpenCL

```cpp
#ifdef ENABLE_OPENCL
    cv::ocl::setUseOpenCL(true);
    // Автоматическое использование OpenCL где возможно
#endif
```

---

## Производительность и оптимизация

### Метрики производительности

1. **FPS (Frames Per Second)**:
   - Детекция объектов: 15-30 FPS (зависит от модели и оборудования)
   - ANPR: 10-20 FPS
   - Детекция движения: 30+ FPS
   - Трекинг: 25-30 FPS

2. **Задержка (Latency)**:
   - Детекция объектов: 30-100 мс
   - ANPR: 50-200 мс
   - Детекция движения: 10-30 мс

3. **Использование ресурсов**:
   - CPU: 20-80% (зависит от количества потоков)
   - GPU: 30-90% (при использовании GPU ускорения)
   - Память: 500MB - 2GB (зависит от моделей)

### Оптимизация

#### 1. Масштабирование входного изображения

```cpp
// Уменьшение разрешения для ускорения
cv::Mat resized;
cv::resize(frame, resized, cv::Size(640, 480));  // Вместо 1920x1080
// Обработка уменьшенного изображения
```

#### 2. Пропуск кадров

```cpp
int frameSkip = 2;  // Обрабатывать каждый 2-й кадр
int frameCount = 0;

if (frameCount % frameSkip == 0) {
    // Обработка кадра
    processFrame(frame);
}
frameCount++;
```

#### 3. Параллельная обработка

```kotlin
// Параллельная обработка несколькими детекторами
val results = listOf(
    async { objectDetector.detect(frame) },
    async { anprEngine.recognize(frame) },
    async { motionDetector.detect(frame) }
).awaitAll()
```

#### 4. Квантование моделей

- INT8 квантование для ускорения в 2-4 раза
- Минимальная потеря точности (1-3%)

#### 5. Оптимизация моделей

- Использование легковесных моделей (YOLOv8n вместо YOLOv8x)
- Pruning (удаление неважных весов)
- Knowledge Distillation (обучение меньшей модели)

### Рекомендации по оборудованию

#### Минимальные требования

- **CPU**: 4 ядра, 2.0 GHz
- **RAM**: 4 GB
- **GPU**: Опционально (для ускорения)

#### Рекомендуемые требования

- **CPU**: 8+ ядер, 3.0+ GHz
- **RAM**: 8+ GB
- **GPU**: NVIDIA GTX 1060+ или эквивалент (для GPU ускорения)

#### Для высокой производительности

- **CPU**: 16+ ядер, 3.5+ GHz
- **RAM**: 16+ GB
- **GPU**: NVIDIA RTX 3060+ или эквивалент
- **NPU**: Neural Processing Unit (для специализированного ускорения)

---

## Рекомендации по внедрению

### Этап 1: Планирование

1. **Определение целей**:
   - Какие задачи должна решать аналитика?
   - Какие объекты нужно детектировать?
   - Какая точность требуется?

2. **Анализ требований**:
   - Количество камер
   - Разрешение видеопотоков
   - Частота кадров
   - Условия съемки

3. **Выбор технологий**:
   - Модели детекции (YOLO, TensorFlow Lite)
   - Оборудование (CPU, GPU, NPU)
   - Библиотеки (OpenCV, TensorFlow Lite)

### Этап 2: Разработка и тестирование

1. **Прототипирование**:
   - Создание базовой реализации
   - Тестирование на тестовых данных
   - Оценка производительности

2. **Оптимизация**:
   - Настройка параметров
   - Оптимизация моделей
   - Улучшение производительности

3. **Тестирование**:
   - Тестирование на реальных данных
   - Валидация точности
   - Стресс-тестирование

### Этап 3: Внедрение

1. **Поэтапное развертывание**:
   - Начать с одной камеры
   - Постепенно добавлять камеры
   - Мониторинг производительности

2. **Настройка параметров**:
   - Калибровка детекторов
   - Настройка зон детекции
   - Оптимизация порогов

3. **Обучение персонала**:
   - Работа с системой
   - Интерпретация результатов
   - Настройка правил

### Этап 4: Мониторинг и улучшение

1. **Мониторинг**:
   - Отслеживание точности
   - Анализ ложных срабатываний
   - Производительность системы

2. **Улучшение**:
   - Тонкая настройка параметров
   - Обновление моделей
   - Оптимизация производительности

3. **Масштабирование**:
   - Добавление новых камер
   - Расширение функциональности
   - Интеграция с другими системами

### Лучшие практики

1. **Качество данных**:
   - Использование качественных камер
   - Правильное размещение камер
   - Оптимальные условия съемки

2. **Настройка параметров**:
   - Начальные значения на основе рекомендаций
   - Постепенная тонкая настройка
   - Тестирование изменений

3. **Производительность**:
   - Мониторинг использования ресурсов
   - Оптимизация при необходимости
   - Масштабирование оборудования

4. **Безопасность**:
   - Защита данных аналитики
   - Соблюдение законодательства
   - Конфиденциальность

---

## Заключение

AI-аналитика в системах видеонаблюдения предоставляет мощные возможности для автоматизации процессов безопасности, контроля доступа и анализа данных. Правильная реализация и настройка позволяют достичь высокой точности и производительности при разумных затратах на оборудование.

Ключевые факторы успеха:
- Правильный выбор технологий и моделей
- Качественное оборудование и условия съемки
- Тщательная настройка и калибровка
- Постоянный мониторинг и улучшение

---

**Связанная документация**:
- [INTEGRATION_GUIDE.md](INTEGRATION_GUIDE.md) - Интеграция библиотек
- [ARCHITECTURE.md](ARCHITECTURE.md) - Архитектура системы
- [REQUIRED_LIBRARIES.md](REQUIRED_LIBRARIES.md) - Требуемые библиотеки

