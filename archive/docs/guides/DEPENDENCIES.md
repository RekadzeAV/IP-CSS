# Зависимости нативных модулей (IP-CSS)

**Версия:** 1.0  
**Дата:** 1 March 2026

Документ описывает внешние зависимости для сборки нативных библиотек проекта (video-processing, analytics, codecs).

---

## Общие требования

- **CMake** 3.15+
- **C++17** (компилятор: MSVC, GCC, Clang)
- **Потоки:** `Threads::Threads` (стандартная зависимость CMake)

---

## OpenCV

Используется в модуле **analytics** для обработки изображений, детекции движения, DNN (YOLO и др.).

### Установка

- **Windows:**  
  - [vcpkg](https://vcpkg.io): `vcpkg install opencv4`  
  - Или сборка из исходников; указать `OpenCV_DIR` при конфигурации CMake.
- **Linux:**  
  - `sudo apt install libopencv-dev` (Ubuntu/Debian)  
  - или `sudo dnf install opencv-devel` (Fedora)
- **macOS:**  
  - `brew install opencv`

### CMake

В корневом `native/CMakeLists.txt` выполняется `find_package(OpenCV REQUIRED)` при `ENABLE_OPENCV=ON`.  
В `native/analytics/CMakeLists.txt` при необходимости выполняется `find_package(OpenCV QUIET)` для автономной сборки analytics.

Переменные: `OpenCV_FOUND`, `OpenCV_INCLUDE_DIRS`, `OpenCV_LIBS`.

---

## TensorFlow Lite

Используется в модуле **analytics** для запуска моделей детекции (опционально).

### Установка

- Скачать или собрать TensorFlow Lite C++ (см. [official docs](https://www.tensorflow.org/lite/guide/build_cmake)).
- Указать путь к include: при конфигурации задать `TFLITE_INCLUDE_DIR` или положить под `native/third_party/tensorflow`.

### CMake

В корневом `native/CMakeLists.txt` ищется `tensorflow/lite/interpreter.h`.  
Если не найден, сборка продолжается без TFLite (`ENABLE_TENSORFLOW=OFF`).

---

## Tesseract OCR

Используется в модуле **analytics** для ANPR (распознавание номерных знаков).

### Установка

- **Windows:**  
  - Установить [Tesseract для Windows](https://github.com/UB-Mannheim/tesseract/wiki), добавить в PATH или указать `C:/tesseract/include`, `C:/tesseract/lib`.
- **Linux:**  
  - `sudo apt install libtesseract-dev`  
  - Языковые данные: `sudo apt install tesseract-ocr-rus tesseract-ocr-eng`
- **macOS:**  
  - `brew install tesseract tesseract-lang`

### CMake

В `native/analytics/CMakeLists.txt` используется `pkg_check_modules(TESSERACT)` или ручной поиск `tesseract/baseapi.h` и библиотеки.  
При отсутствии Tesseract ANPR собирается без OCR (предупреждение в конфигурации).

---

## FFmpeg

Используется в **video-processing** для RTSP, декодирования и кодирования.

### Установка

- **Windows:** собрать или взять сборку с [gyan.dev](https://www.gyan.dev/ffmpeg/builds/), указать пути в CMake.
- **Linux:** `sudo apt install libavformat-dev libavcodec-dev libavutil-dev libswscale-dev libswresample-dev`
- **macOS:** `brew install ffmpeg`

---

## Скрипты сборки

- **Windows:** `scripts/build-all-native-libs.ps1`
- **Linux/macOS:** `scripts/build-all-native-libs.sh`

Перед сборкой убедитесь, что OpenCV (и при необходимости Tesseract, FFmpeg) установлены и доступны для CMake.

---

## Модели для аналитики

Отдельно от системных библиотек требуются файлы моделей (YOLO, каскады лиц и т.д.).  
См. **docs/MODELS.md** (если создан) и `docs/AI_ANALYTICS.md` для описания форматов и путей.
