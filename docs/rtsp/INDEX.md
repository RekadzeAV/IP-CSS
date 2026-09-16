# RTSP Client Documentation Index

Полный указатель документации RTSP клиента.

---

## 🚀 Быстрый старт

| Документ | Описание | Размер |
|----------|----------|--------|
| **[RTSP_CLIENT_QUICK_START.md](../../_to_be_archived/ROOT_FILES_2026-06-21/RTSP_CLIENT_QUICK_START.md)** | Быстрый старт (1 стр) | 1 KB |
| **[native/video-processing/README.md](../../native/video-processing/README.md)** | README библиотеки | 3 KB |

---

## 📚 Основная документация

| Документ | Описание | Размер |
|----------|----------|--------|
| **[RTSP_CLIENT_QUICK_START.md](../../_to_be_archived/ROOT_FILES_2026-06-21/RTSP_CLIENT_QUICK_START.md)** | Быстрый старт (1 стр) | 1 KB |
| **[RTSP_CLIENT_README.md](RTSP_CLIENT_README.md)** | Полная API документация | 15 KB |
| **[RTSP_CLIENT_TROUBLESHOOTING.md](RTSP_CLIENT_TROUBLESHOOTING.md)** | Решение проблем | 12 KB |
| **[RTSP_CLIENT_EXAMPLES.md](RTSP_CLIENT_EXAMPLES.md)** | Примеры использования | 18 KB |
| **[CAMERA_CONFIGURATIONS.md](CAMERA_CONFIGURATIONS.md)** | Конфигурации камер | 12 KB |
| **[JNI_DEBUGGING.md](JNI_DEBUGGING.md)** | Отладка JNI | 10 KB |
| **[../SCRIPTS_README.md](../../_to_be_archived/ROOT_FILES_2026-06-21/SCRIPTS_README.md)** | Руководство по скриптам | 10 KB |

## 📊 Отчёты и статус

| Документ | Описание | Размер |
|----------|----------|--------|
| **[../reports/RTSP_INTEGRATION_COMPLETION_REPORT_2026-05-24.md](../reports/RTSP_INTEGRATION_COMPLETION_REPORT_2026-05-24.md)** | Финальный отчёт интеграции | 15 KB |
| **[../reports/PRODUCTION_CHECKLIST.md](../reports/PRODUCTION_CHECKLIST.md)** | Чеклист production | 12 KB |

---

## 📊 Отчёты и статус

| Документ | Описание | Размер |
|----------|----------|--------|
| **[../reports/RTSP_INTEGRATION_COMPLETION_REPORT_2026-05-24.md](../reports/RTSP_INTEGRATION_COMPLETION_REPORT_2026-05-24.md)** | Финальный отчёт интеграции | 15 KB |
| **[../reports/RTSP_TASKS_1-3_COMPLETION_REPORT_2026-05-24.md](../reports/RTSP_TASKS_1-3_COMPLETION_REPORT_2026-05-24.md)** | Отчёт по задачам 1-3 | 10 KB |
| **[../reports/RTSP_CLIENT_FINAL_INTEGRATION_REPORT_2026-05-24.md](../reports/RTSP_CLIENT_FINAL_INTEGRATION_REPORT_2026-05-24.md)** | Детальный финальный отчёт | 12 KB |
| **[../implementation/RTSP_CLIENT_INTEGRATION_PROGRESS.md](../implementation/RTSP_CLIENT_INTEGRATION_PROGRESS.md)** | Прогресс интеграции | 8 KB |

---

## 🛠️ Технические детали

| Файл | Описание |
|------|----------|
| **[native/video-processing/CMakeLists.txt](../../native/video-processing/CMakeLists.txt)** | Конфигурация CMake |
| **[native/video-processing/Dockerfile.linux](../../native/video-processing/Dockerfile.linux)** | Docker для Linux сборки |
| **[native/video-processing/Dockerfile.macos](../../native/video-processing/Dockerfile.macos)** | Заготовка для macOS |
| **[.github/workflows/rtsp-native-build.yml](../../.github/workflows/rtsp-native-build.yml)** | GitHub Actions workflow |

---

## 📜 Скрипты

| Скрипт | Описание | Платформа |
|--------|----------|-----------|
| **[native/video-processing/build-windows.ps1](../../native/video-processing/build-windows.ps1)** | Сборка на Windows | Windows |
| **[native/video-processing/build-macos.sh](../../native/video-processing/build-macos.sh)** | Сборка на macOS (полная) | macOS |
| **test-rtsp-client.ps1 *(утерян/в архиве)*** | Базовое тестирование | Windows |
| **test-rtsp-integration.ps1 *(утерян/в архиве)*** | Интеграционные тесты | Windows |
| **test-rtsp-integration.sh *(утерян/в архиве)*** | Интеграционные тесты | Linux/macOS |
| **test-rtsp-connection.ps1 *(утерян/в архиве)*** | Тестирование подключения | Windows |

---

## 📖 Навигация по темам

### Для разработчиков

1. **Начало работы**
   - [RTSP_CLIENT_QUICK_START.md](../../_to_be_archived/ROOT_FILES_2026-06-21/RTSP_CLIENT_QUICK_START.md)
   - [RTSP_CLIENT_README.md](RTSP_CLIENT_README.md)

2. **Примеры кода**
   - [RTSP_CLIENT_EXAMPLES.md](RTSP_CLIENT_EXAMPLES.md)

3. **Решение проблем**
   - [RTSP_CLIENT_TROUBLESHOOTING.md](RTSP_CLIENT_TROUBLESHOOTING.md)

4. **Конфигурация камер**
   - [CAMERA_CONFIGURATIONS.md](CAMERA_CONFIGURATIONS.md)

### Для тестировщиков

1. **Запуск тестов**
   - test-rtsp-integration.ps1 *(утерян/в архиве)*
   - test-rtsp-integration.sh *(утерян/в архиве)*

2. **Настройка RTSP сервера**
   - [RTSP_CLIENT_TROUBLESHOOTING.md](RTSP_CLIENT_TROUBLESHOOTING.md) (раздел "Тестирование")

### Для DevOps

1. **Сборка библиотек**
   - [native/video-processing/README.md](../../native/video-processing/README.md)
   - [.github/workflows/rtsp-native-build.yml](../../.github/workflows/rtsp-native-build.yml)

2. **CI/CD настройка**
   - [rtsp-native-build.yml](../../.github/workflows/rtsp-native-build.yml)

### Для руководителей

1. **Статус проекта**
   - [RTSP_INTEGRATION_COMPLETION_REPORT_2026-05-24.md](../reports/RTSP_INTEGRATION_COMPLETION_REPORT_2026-05-24.md)

2. **Детальные отчёты**
   - [RTSP_CLIENT_FINAL_INTEGRATION_REPORT_2026-05-24.md](../reports/RTSP_CLIENT_FINAL_INTEGRATION_REPORT_2026-05-24.md)

---

## 📊 Статус проекта

```
Общий прогресс: 85% ✅

✅ Сборка библиотек (Windows, Linux)
✅ JNI интеграция и тестирование
✅ Функциональность RTSP клиента
✅ Документация
✅ CI/CD настройка
🟡 macOS сборка (заготовка)
🟡 Интеграционные тесты (требуют RTSP сервер)
```

---

## 📞 Контакты и поддержка

- **GitHub Issues**: https://github.com/your-org/IP-CSS/issues
- **Документация**: Этот индекс
- **Примеры**: [RTSP_CLIENT_EXAMPLES.md](RTSP_CLIENT_EXAMPLES.md)
- **Troubleshooting**: [RTSP_CLIENT_TROUBLESHOOTING.md](RTSP_CLIENT_TROUBLESHOOTING.md)

---

**Версия:** 1.0  
**Обновлено:** 24 мая 2026  
**Всего документов:** 15+  
**Общий размер:** ~150 KB
