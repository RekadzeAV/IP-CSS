# Модели для AI-аналитики

**Версия:** 1.0  
**Дата:** 1 March 2026

Описание форматов и источников моделей для модуля аналитики (детекция объектов, лиц, ANPR).

---

## Детекция объектов (YOLO)

- **Формат:** ONNX или TensorFlow (OpenCV DNN). Рекомендуется YOLOv5/YOLOv8 в ONNX.
- **Источники:**  
  - [Ultralytics YOLOv8](https://github.com/ultralytics/ultralytics) — экспорт в ONNX.  
  - [YOLOv5](https://github.com/ultralytics/yolov5) — экспорт в ONNX.
- **Размер входа:** обычно 640×640; см. документацию конкретной модели.
- **Размещение:** каталог `data/models/` (или путь из настроек камеры). Загрузка: `scripts/download-models.ps1` (Windows) или `scripts/download-models.sh` (Linux/macOS).

---

## Детекция лиц

- **Каскады Haar:** OpenCV поставляется с `haarcascade_frontalface_alt.xml` и др. (данные OpenCV).
- **DNN-модели:** при использовании DNN — пути и форматы задаются в конфигурации модуля analytics.

---

## ANPR (Tesseract)

- Языковые данные Tesseract (rus, eng и др.) устанавливаются отдельно от приложения (системные пакеты или каталог `tessdata`).  
См. **docs/DEPENDENCIES.md**.

---

## Конвертация и квантизация

- Конвертация в ONNX: см. документацию выбранного фреймворка (PyTorch, TensorFlow).  
- Квантизация TFLite/ONNX для ускорения: см. план реализации, фаза 8 и блок 3.2.
