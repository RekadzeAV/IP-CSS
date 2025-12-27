# План реализации Desktop приложения IP-CSS

**Дата создания:** Январь 2025
**Версия проекта:** Alfa-0.0.1
**Статус:** Планирование

> **📚 Полный индекс документации:** [DOCUMENTATION_INDEX.md](../DOCUMENTATION_INDEX.md)

---

## 📋 Содержание

1. [Обзор](#обзор)
2. [Архитектура](#архитектура)
3. [Технологический стек](#технологический-стек)
4. [Структура модулей](#структура-модулей)
5. [Этапы реализации](#этапы-реализации)
6. [Детальные задачи](#детальные-задачи)
7. [Интеграция с существующими модулями](#интеграция-с-существующими-модулями)
8. [Сборка и распространение](#сборка-и-распространение)

---

## Обзор

Desktop приложение IP-CSS - это нативное кроссплатформенное приложение для управления системой видеонаблюдения, работающее на Windows, Linux и macOS.

### Поддерживаемые платформы

- **Windows 10/11** (x64)
- **Linux** (Ubuntu, Debian, Fedora, Arch и др.) (x64, ARM64)
- **macOS** (Intel x64, Apple Silicon ARM64)

### Основной функционал

1. **Управление камерами**
   - Добавление/редактирование/удаление камер
   - Автоматическое обнаружение камер (ONVIF WS-Discovery)
   - Тестирование подключения к камерам
   - Настройка параметров камер

2. **Просмотр видео**
   - Просмотр видео в реальном времени (RTSP)
   - Многокамерный просмотр (grid layout)
   - Управление PTZ камерами
   - Снимки экрана

3. **Управление записями**
   - Просмотр списка записей
   - Воспроизведение записей
   - Экспорт записей
   - Удаление записей

4. **События и уведомления**
   - Просмотр событий в реальном времени
   - Фильтрация событий
   - Уведомления о событиях

5. **Настройки**
   - Настройки приложения
   - Управление пользователями
   - Лицензирование
   - Настройки сервера

---

## Архитектура

### Архитектурный паттерн

Desktop приложение использует **MVI (Model-View-Intent)** архитектуру с элементами **MVVM**:

```
┌─────────────────────────────────────────────────────────┐
│                      UI Layer                           │
│  (Compose Desktop - Windows, Linux, macOS)              │
│                                                          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐ │
│  │   Screens    │  │  Components  │  │  Navigation  │ │
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘ │
│         │                 │                 │          │
│         └─────────────────┼─────────────────┘          │
│                            │                            │
└────────────────────────────┼────────────────────────────┘
                             │
┌────────────────────────────┼────────────────────────────┐
│                      State Layer                         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐  │
│  │   ViewModel  │  │    State    │  │   Intent    │  │
│  └──────┬───────┘  └──────────────┘  └──────┬───────┘  │
│         │                                    │          │
└─────────┼────────────────────────────────────┼──────────┘
          │                                    │
┌─────────┼────────────────────────────────────┼──────────┐
│    Business Logic Layer (Shared KMP)         │          │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐│
│  │  Use Cases   │  │ Repositories │  │   Services   ││
│  └──────┬───────┘  └──────┬───────┘  └──────┬───────┘│
│         │                 │                 │         │
└─────────┼─────────────────┼─────────────────┼─────────┘
          │                 │                 │
┌─────────┼─────────────────┼─────────────────┼──────────┐
│    Data Layer (Shared KMP)                    │         │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐│
│  │  Local DB    │  │  API Client  │  │  WebSocket   ││
│  │ (SQLDelight) │  │   (Ktor)     │  │   Client     ││
│  └──────────────┘  └──────────────┘  └──────────────┘│
└───────────────────────────────────────────────────────┘
```

### Слои приложения

1. **UI Layer (Desktop-specific)**
   - Compose Desktop компоненты
   - Навигация
   - Обработка пользовательского ввода

2. **State Layer (Desktop-specific)**
   - ViewModels (управление состоянием UI)
   - State классы (иммутабельное состояние)
   - Intent/Action классы (пользовательские действия)

3. **Business Logic Layer (Shared KMP)**
   - Use Cases (бизнес-логика)
   - Domain модели

4. **Data Layer (Shared KMP)**
   - Repositories (абстракция источников данных)
   - API клиенты (Ktor)
   - Локальная БД (SQLDelight)

---

## Технологический стек

### UI Framework

- **Compose Desktop** (JetBrains Compose Multiplatform)
  - Версия: 1.7.0 (из `libs.versions.toml`)
  - Кроссплатформенный UI на Kotlin
  - Material Design 3 компоненты

### Dependency Injection

- **Koin** 3.6.0
  - Легковесный DI фреймворк
  - Поддержка Kotlin Multiplatform

### Навигация

- **Compose Navigation** (кастомная реализация)
  - Или библиотека `compose-navigation` (если доступна)

### Состояние

- **Kotlinx Coroutines Flow**
  - StateFlow для состояния UI
  - SharedFlow для событий

### Сетевые запросы

- **Ktor Client** (из `:core:network`)
  - REST API запросы
  - WebSocket соединения

### Локальная БД

- **SQLDelight** 2.0.3
  - JVM Driver для Desktop
  - Кэширование данных

### Логирование

- **Kotlin Logging** 3.0.5
  - Структурированное логирование

### Видео

- **Нативные библиотеки** (из `native/`)
  - RTSP клиент (C++)
  - Декодеры видео (H.264, H.265, MJPEG)
  - Интеграция через FFI

---

## Структура модулей

### Структура Gradle модулей

```
platforms/
├── client-desktop-x86_64/
│   └── app/
│       ├── build.gradle.kts
│       └── src/
│           └── main/
│               └── kotlin/
│                   └── com/company/ipcamera/desktop/
│                       ├── Main.kt                    # Точка входа
│                       ├── App.kt                     # Корневой Compose компонент
│                       ├── di/                        # Koin модули
│                       │   └── AppModule.kt
│                       ├── ui/                        # UI компоненты
│                       │   ├── theme/                 # Тема приложения
│                       │   │   ├── Theme.kt
│                       │   │   └── Color.kt
│                       │   ├── navigation/            # Навигация
│                       │   │   └── Navigation.kt
│                       │   ├── screens/               # Экраны
│                       │   │   ├── cameras/
│                       │   │   │   ├── CamerasScreen.kt
│                       │   │   │   ├── CameraDetailScreen.kt
│                       │   │   │   └── AddCameraScreen.kt
│                       │   │   ├── live/
│                       │   │   │   └── LiveViewScreen.kt
│                       │   │   ├── recordings/
│                       │   │   │   ├── RecordingsScreen.kt
│                       │   │   │   └── RecordingPlayerScreen.kt
│                       │   │   ├── events/
│                       │   │   │   └── EventsScreen.kt
│                       │   │   └── settings/
│                       │   │       └── SettingsScreen.kt
│                       │   └── components/            # Переиспользуемые компоненты
│                       │       ├── CameraCard.kt
│                       │       ├── VideoPlayer.kt
│                       │       └── EventList.kt
│                       └── viewmodel/                 # ViewModels
│                           ├── CamerasViewModel.kt
│                           ├── LiveViewViewModel.kt
│                           ├── RecordingsViewModel.kt
│                           └── EventsViewModel.kt
│
└── client-desktop-arm/
    └── app/
        └── (аналогичная структура)
```

### Зависимости модуля

```kotlin
dependencies {
    // Shared KMP модуль
    implementation(project(":shared"))

    // Core модули
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":core:license"))

    // Compose Desktop
    implementation(compose.desktop.currentOs)

    // Koin
    implementation(libs.koin.core)
    implementation(libs.koin.compose) // если доступен

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Logging
    implementation(libs.kotlin.logging)
}
```

---

## Этапы реализации

### Этап 1: Настройка инфраструктуры (1-2 недели)

**Цель:** Создать базовую структуру проекта и настроить сборку.

#### Задачи:

1. **Создание Gradle модулей**
   - [ ] Создать модуль `platforms/client-desktop-x86_64/app`
   - [ ] Создать модуль `platforms/client-desktop-arm/app`
   - [ ] Настроить `build.gradle.kts` для обоих модулей
   - [ ] Добавить модули в `settings.gradle.kts`
   - [ ] Настроить зависимости (Compose Desktop, Koin, shared)

2. **Базовая структура приложения**
   - [ ] Создать `Main.kt` (точка входа)
   - [ ] Создать `App.kt` (корневой Compose компонент)
   - [ ] Настроить тему приложения (Material Design 3)
   - [ ] Создать базовую навигацию

3. **Dependency Injection**
   - [ ] Настроить Koin модули
   - [ ] Создать `AppModule.kt` с зависимостями
   - [ ] Интегрировать с `:shared` модулем

4. **Тестирование сборки**
   - [ ] Убедиться, что проект компилируется
   - [ ] Запустить пустое приложение на всех платформах

**Результат:** Работающее пустое приложение с базовой структурой.

---

### Этап 2: UI компоненты и навигация (2-3 недели)

**Цель:** Создать основные UI компоненты и систему навигации.

#### Задачи:

1. **Система навигации**
   - [ ] Реализовать навигационный роутер
   - [ ] Создать экраны-заглушки для всех разделов
   - [ ] Реализовать боковое меню/навигационную панель

2. **Базовые UI компоненты**
   - [ ] Создать компонент `CameraCard` (карточка камеры)
   - [ ] Создать компонент `VideoPlayer` (плеер видео)
   - [ ] Создать компонент `EventList` (список событий)
   - [ ] Создать компонент `LoadingIndicator` (индикатор загрузки)
   - [ ] Создать компонент `ErrorView` (отображение ошибок)

3. **Экран списка камер**
   - [ ] Реализовать `CamerasScreen` с grid layout
   - [ ] Интегрировать с `CamerasViewModel`
   - [ ] Добавить поиск и фильтрацию

4. **Экран деталей камеры**
   - [ ] Реализовать `CameraDetailScreen`
   - [ ] Отображение информации о камере
   - [ ] Кнопки управления (тест подключения, редактирование, удаление)

**Результат:** Работающая навигация и базовые UI компоненты.

---

### Этап 3: Интеграция с бизнес-логикой (2-3 недели)

**Цель:** Интегрировать UI с Use Cases и Repositories из `:shared`.

#### Задачи:

1. **ViewModels**
   - [ ] Реализовать `CamerasViewModel`
     - Загрузка списка камер
     - Добавление/редактирование/удаление камер
     - Обнаружение камер
   - [ ] Реализовать `CameraDetailViewModel`
     - Загрузка деталей камеры
     - Тестирование подключения
   - [ ] Реализовать `LiveViewViewModel`
     - Управление видеопотоками
   - [ ] Реализовать `RecordingsViewModel`
     - Загрузка списка записей
     - Воспроизведение записей
   - [ ] Реализовать `EventsViewModel`
     - Загрузка событий
     - Фильтрация событий

2. **Интеграция с Use Cases**
   - [ ] Подключить `GetCamerasUseCase`
   - [ ] Подключить `AddCameraUseCase`
   - [ ] Подключить `UpdateCameraUseCase`
   - [ ] Подключить `DeleteCameraUseCase`
   - [ ] Подключить `DiscoverCamerasUseCase`

3. **Обработка состояний**
   - [ ] Реализовать состояния загрузки (Loading, Success, Error)
   - [ ] Обработка ошибок и отображение сообщений
   - [ ] Оптимистичные обновления UI

**Результат:** Полностью функциональное управление камерами через UI.

---

### Этап 4: Просмотр видео в реальном времени (3-4 недели)

**Цель:** Реализовать просмотр видео с камер в реальном времени.

#### Задачи:

1. **Интеграция с RTSP клиентом**
   - [ ] Интегрировать нативный RTSP клиент (из `native/video-processing`)
   - [ ] Создать обертку для работы с RTSP в Kotlin
   - [ ] Управление RTSP соединениями

2. **Видео плеер**
   - [ ] Реализовать компонент `VideoPlayer` с поддержкой RTSP
   - [ ] Декодирование видеопотока (H.264, H.265, MJPEG)
   - [ ] Отображение видео в Compose
   - [ ] Управление воспроизведением (play/pause/stop)

3. **Многокамерный просмотр**
   - [ ] Реализовать grid layout для нескольких камер
   - [ ] Управление количеством отображаемых камер (1, 4, 9, 16)
   - [ ] Оптимизация производительности при множественных потоках

4. **PTZ управление**
   - [ ] Реализовать UI для PTZ управления
   - [ ] Интеграция с ONVIF PTZ командами
   - [ ] Presets и паттерны

**Результат:** Работающий просмотр видео в реальном времени.

---

### Этап 5: Управление записями (2-3 недели)

**Цель:** Реализовать функционал работы с записями.

#### Задачи:

1. **Экран списка записей**
   - [ ] Реализовать `RecordingsScreen`
   - [ ] Отображение списка записей с фильтрацией
   - [ ] Поиск по дате/времени
   - [ ] Фильтрация по камере

2. **Воспроизведение записей**
   - [ ] Реализовать `RecordingPlayerScreen`
   - [ ] Воспроизведение записей через API (HLS)
   - [ ] Управление воспроизведением (play/pause/seek)
   - [ ] Отображение временной шкалы

3. **Экспорт записей**
   - [ ] Реализовать экспорт записей
   - [ ] Выбор директории для сохранения
   - [ ] Прогресс экспорта

4. **Удаление записей**
   - [ ] Реализовать удаление записей
   - [ ] Подтверждение удаления

**Результат:** Полнофункциональное управление записями.

---

### Этап 6: События и уведомления (2 недели)

**Цель:** Реализовать просмотр событий и систему уведомлений.

#### Задачи:

1. **Экран событий**
   - [ ] Реализовать `EventsScreen`
   - [ ] Отображение списка событий в реальном времени
   - [ ] Фильтрация по типу события, камере, дате
   - [ ] Подтверждение событий

2. **WebSocket интеграция**
   - [ ] Интегрировать WebSocket клиент (из `:core:network`)
   - [ ] Подписка на события в реальном времени
   - [ ] Обработка обновлений событий

3. **Системные уведомления**
   - [ ] Реализовать системные уведомления (Windows/Linux/macOS)
   - [ ] Уведомления о критических событиях
   - [ ] Настройки уведомлений

**Результат:** Работающая система событий и уведомлений.

---

### Этап 7: Настройки и лицензирование (2 недели)

**Цель:** Реализовать экраны настроек и лицензирования.

#### Задачи:

1. **Экран настроек**
   - [ ] Реализовать `SettingsScreen`
   - [ ] Настройки приложения (язык, тема, уведомления)
   - [ ] Настройки сервера (URL, порт, SSL)
   - [ ] Сохранение настроек в локальной БД

2. **Управление пользователями**
   - [ ] Экран управления пользователями (для администраторов)
   - [ ] Создание/редактирование/удаление пользователей
   - [ ] Управление ролями

3. **Лицензирование**
   - [ ] Интеграция с `:core:license`
   - [ ] Экран активации лицензии
   - [ ] Отображение информации о лицензии
   - [ ] Проверка лицензии при запуске

**Результат:** Полнофункциональные настройки и лицензирование.

---

### Этап 8: Оптимизация и полировка (2-3 недели)

**Цель:** Оптимизировать производительность и улучшить UX.

#### Задачи:

1. **Оптимизация производительности**
   - [ ] Оптимизация рендеринга видео
   - [ ] Кэширование данных
   - [ ] Ленивая загрузка компонентов
   - [ ] Оптимизация памяти

2. **UX улучшения**
   - [ ] Анимации и переходы
   - [ ] Обратная связь пользователю
   - [ ] Обработка edge cases
   - [ ] Улучшение сообщений об ошибках

3. **Тестирование**
   - [ ] Unit тесты для ViewModels
   - [ ] UI тесты для критических экранов
   - [ ] Тестирование на всех платформах

4. **Документация**
   - [ ] Документация для разработчиков
   - [ ] Руководство пользователя
   - [ ] Скриншоты и демо

**Результат:** Оптимизированное и отполированное приложение.

---

## Детальные задачи

### Модуль: platforms/client-desktop-x86_64/app

#### build.gradle.kts

```kotlin
plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
    id("com.android.application") // для Android, если нужно
}

kotlin {
    jvm("desktop") {
        compilations.all {
            kotlinOptions {
                jvmTarget = "11"
            }
        }
    }
}

dependencies {
    // Shared KMP
    implementation(project(":shared"))

    // Core modules
    implementation(project(":core:common"))
    implementation(project(":core:network"))
    implementation(project(":core:license"))

    // Compose Desktop
    implementation(compose.desktop.currentOs)

    // Koin
    implementation(libs.koin.core)

    // Coroutines
    implementation(libs.kotlinx.coroutines.core)

    // Logging
    implementation(libs.kotlin.logging)
}

compose.desktop {
    application {
        mainClass = "com.company.ipcamera.desktop.MainKt"

        nativeDistributions {
            targetFormats(
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Dmg,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Msi,
                org.jetbrains.compose.desktop.application.dsl.TargetFormat.Deb
            )

            packageName = "IP-CSS Desktop"
            packageVersion = "1.0.0"

            windows {
                msiPackageVersion = "1.0.0"
                upgradeUuid = "18159995-d967-4cd2-8885-77BFA97CFA9F"
            }

            macOS {
                bundleID = "com.company.ipcamera.desktop"
            }

            linux {
                debMaintainer = "company@example.com"
            }
        }
    }
}
```

#### Main.kt

```kotlin
package com.company.ipcamera.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.company.ipcamera.desktop.di.appModule
import org.koin.core.context.startKoin

fun main() = application {
    // Инициализация Koin
    startKoin {
        modules(appModule)
    }

    Window(
        onCloseRequest = ::exitApplication,
        title = "IP Camera Surveillance System"
    ) {
        App()
    }
}
```

#### App.kt

```kotlin
package com.company.ipcamera.desktop

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.company.ipcamera.desktop.ui.navigation.Navigation
import com.company.ipcamera.desktop.ui.theme.AppTheme

@Composable
fun App() {
    AppTheme {
        Navigation(
            modifier = Modifier.fillMaxSize()
        )
    }
}
```

---

## Интеграция с существующими модулями

### Использование :shared модуля

Desktop приложение использует все Use Cases и Repositories из `:shared`:

```kotlin
// В ViewModel
class CamerasViewModel(
    private val getCamerasUseCase: GetCamerasUseCase,
    private val addCameraUseCase: AddCameraUseCase,
    private val updateCameraUseCase: UpdateCameraUseCase,
    private val deleteCameraUseCase: DeleteCameraUseCase,
    private val discoverCamerasUseCase: DiscoverCamerasUseCase
) : ViewModel() {
    // ...
}
```

### Использование :core:network

```kotlin
// В Repository или Service
class DesktopApiService(
    private val apiClient: ApiClient
) {
    // Использование API клиента
}
```

### Использование нативных библиотек

Интеграция с C++ библиотеками через FFI:

```kotlin
// Обертка для RTSP клиента
expect class RtspClientWrapper {
    fun connect(url: String): Boolean
    fun disconnect()
    fun getFrame(): ByteArray?
}
```

---

## Сборка и распространение

### Локальная сборка

```bash
# Windows (MSI)
./gradlew :platforms:client-desktop-x86_64:app:packageMsi

# Linux (DEB)
./gradlew :platforms:client-desktop-x86_64:app:packageDeb

# macOS (DMG)
./gradlew :platforms:client-desktop-x86_64:app:packageDmg

# ARM версии
./gradlew :platforms:client-desktop-arm:app:packageDeb
```

### Запуск в режиме разработки

```bash
./gradlew :platforms:client-desktop-x86_64:app:run
```

### Создание установщиков

Compose Desktop автоматически создает установщики для всех платформ:
- Windows: MSI установщик
- Linux: DEB/RPM пакеты
- macOS: DMG образ

---

## Оценка времени

| Этап | Время | Приоритет |
|------|-------|-----------|
| Этап 1: Инфраструктура | 1-2 недели | Критический |
| Этап 2: UI компоненты | 2-3 недели | Критический |
| Этап 3: Интеграция с бизнес-логикой | 2-3 недели | Критический |
| Этап 4: Просмотр видео | 3-4 недели | Высокий |
| Этап 5: Управление записями | 2-3 недели | Высокий |
| Этап 6: События и уведомления | 2 недели | Средний |
| Этап 7: Настройки | 2 недели | Средний |
| Этап 8: Оптимизация | 2-3 недели | Низкий |

**Общее время:** 16-22 недели (4-5.5 месяцев)

---

## Риски и зависимости

### Риски

1. **Интеграция с нативными библиотеками**
   - Риск: Сложность интеграции C++ библиотек через FFI
   - Митигация: Создать обертки и протестировать на раннем этапе

2. **Производительность видео**
   - Риск: Проблемы с производительностью при множественных потоках
   - Митигация: Оптимизация рендеринга, использование hardware acceleration

3. **Кроссплатформенная совместимость**
   - Риск: Различия в поведении на разных ОС
   - Митигация: Тестирование на всех платформах на каждом этапе

### Зависимости

1. **Завершение Use Cases в :shared**
   - Необходимо для интеграции бизнес-логики

2. **Стабильность API сервера**
   - Необходимо для работы с записями и событиями

3. **Готовность нативных библиотек**
   - Необходимо для просмотра видео в реальном времени

---

## Следующие шаги

1. **Создать модули в Gradle**
   - Добавить `platforms/client-desktop-x86_64/app` в `settings.gradle.kts`
   - Настроить `build.gradle.kts`

2. **Создать базовую структуру**
   - `Main.kt`, `App.kt`, навигация

3. **Настроить DI**
   - Koin модули

4. **Начать с Этапа 1**

---

**Последнее обновление:** Январь 2025
**Статус:** Готов к реализации

