# Core Common Module

Модуль `:core:common` содержит общие типы и модели, которые используются в других модулях проекта.

## Содержимое

### Модели

- **Resolution** - модель разрешения видео (ширина x высота)
- **CameraStatus** - статус камеры (ONLINE, OFFLINE, ERROR, CONNECTING, UNKNOWN)

## Использование

### Добавление зависимости

В `build.gradle.kts` вашего модуля:

```kotlin
dependencies {
    implementation(project(":core:common"))
}
```

### Импорт типов

```kotlin
import com.company.ipcamera.core.common.model.Resolution
import com.company.ipcamera.core.common.model.CameraStatus

// Пример использования
val resolution = Resolution(1920, 1080)
val status = CameraStatus.ONLINE
```

## Архитектурное назначение

Этот модуль был создан для устранения циклической зависимости между модулями `:shared` и `:core:network`. Базовые типы, используемые в обоих модулях, были вынесены в отдельный модуль `:core:common`.

**Структура зависимостей:**
- `:core:common` - базовые типы (независимый модуль)
- `:shared` зависит от `:core:common` и `:core:network`
- `:core:network` зависит только от `:core:common` (не зависит от `:shared`)

Это позволяет избежать циклических зависимостей и обеспечивает четкую архитектуру проекта.

