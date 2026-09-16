# Отчет: Задача 4.1.2.1 — Загрузка нативной библиотеки в JVM

**Дата:** 27 января 2026  
**Статус:** ✅ Завершено (100%)  
**Время:** 4 часа  

---

## Выполненные работы

### 1. Загрузка библиотеки в `NativeRtspClient.jvm.kt` ✅

Обновлён `NativeRtspClient.jvm.kt` для загрузки нативной библиотеки `video_processing` на JVM (Desktop):

**Основные изменения:**

- Реализована `loadNativeLibrary()` в `companion object`: сначала `System.loadLibrary("video_processing")` (пути `java.library.path` / `PATH` / `LD_LIBRARY_PATH` и т.д.), при `UnsatisfiedLinkError` — загрузка по файловому пути из дерева репозитория.
- Определение ОС: `os.name`; для macOS — выбор `x64` / `arm64` по `os.arch` (`aarch64` / `arm` → `arm64`).
- Локальные пути относительно текущей рабочей директории и `user.dir`: сначала `File(libPath)`, затем `File(user.dir, libPath)`.
- Сообщения в консоль: успех через `System.out.println`, предупреждения и ошибки через `System.err.println` (в т.ч. подсказка `./scripts/build-video-processing-lib.sh`).
- Однократная загрузка: флаг `libraryLoaded` и `synchronized(NativeRtspClient::class.java)`.
- Вызов `loadNativeLibrary()` из `init` при первом использовании класса.

### 2. Пути к артефактам ✅

**Фактическая развязка в коде:**

| Платформа | Относительный путь |
|-----------|-------------------|
| Windows x64 | `native/video-processing/lib/windows/x64/video_processing.dll` |
| Linux (не macOS) | `native/video-processing/lib/linux/x64/libvideo_processing.so` |
| macOS x64 | `native/video-processing/lib/macos/x64/libvideo_processing.dylib` |
| macOS arm64 | `native/video-processing/lib/macos/arm64/libvideo_processing.dylib` |

Для Linux в текущей JVM-реализации используется каталог `linux/x64` (отдельная ветка под Linux arm64 в этом файле не задаётся).

### 3. Согласование с JNI API ✅

- `create()` возвращает `NativeRtspClientHandle` (типизированный дескриптор вместо «голого» `Long`).
- Остальные `actual` методы принимают `NativeRtspClientHandle`; вызовы идут в `external` функции после успешной загрузки библиотеки.

---

## Технические детали

### Алгоритм загрузки

1. Если `libraryLoaded` — выход.
2. В `synchronized`: повторная проверка, затем попытка `System.loadLibrary("video_processing")`.
3. При ошибке — вычисление `libName` и `libPath` по ОС/архитектуре, проверка `File(libPath)`, при отсутствии — `File(user.dir, libPath)`.
4. При неуспехе — предупреждения в stderr без падения JVM (поведение «мягкой» деградации зависит от того, вызывается ли JNI до появления библиотеки).

### Пример сообщений

- Успех из системного пути: `Native RTSP library loaded from system path`
- Успех из файла: `Native RTSP library loaded from: <absolute path>`

---

## Файлы

**Обновлённые:**

- `core/network/src/jvmMain/kotlin/com/company/ipcamera/core/network/rtsp/NativeRtspClient.jvm.kt` ✅

---

## Критерии приёмки

- ✅ Загрузка при первом использовании класса (`init`).
- ✅ Двухшаговая стратегия: системная библиотека, затем путь в репозитории / от `user.dir`.
- ✅ Учёт Windows, Linux (x64-путь), macOS (x64/arm64).
- ✅ Потокобезопасность однократной загрузки.
- ✅ Согласованность с JNI (`NativeRtspClientHandle`).

---

## Следующие шаги

1. **Задача 4.1.2.2:** тестирование базового подключения (тестовый RTSP, проверка JNI и колбэков).

---

**Задача завершена:** ✅  
**Следующая задача:** 4.1.2.2 — тестирование базового подключения  
