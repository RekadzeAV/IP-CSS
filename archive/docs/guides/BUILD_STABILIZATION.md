# Стабилизация сборки (KMP, репозитории, сборка без Android)

**Дата:** 2026-03-01  
**Связано:** [TESTING_EXECUTION_RESULT_PHASE0.md](TESTING_EXECUTION_RESULT_PHASE0.md), [POETAPNYJ_PLAN_REALIZACII.md](POETAPNYJ_PLAN_REALIZACII.md).

---

## Цель

Обеспечить проходимость сборки на машинах без Android SDK и без нативных тулчейнов (Windows), а также устранить ошибки разрешения зависимостей KMP и репозиториев.

---

## 1. KMP: опциональные Android Native targets в core:network

### Проблема

Модуль `:core:network` объявляет цели `androidNativeArm32`, `androidNativeArm64`, `androidNativeX86`, `androidNativeX64`. При разрешении зависимостей для этих целей требуется вариант проекта `:core:common` под `android_x64` (и др.), но `:core:common` таких вариантов не публикует → **No matching variant**.

### Решение

Цели `androidNative*` в `:core:network` сделаны **опциональными** и по умолчанию отключены.

| Параметр | Файл | Описание |
|----------|------|----------|
| `ipcss.buildAndroidNative` | `gradle.properties` | `false` (по умолчанию) — не регистрировать `androidNative*` в `:core:network`. При `true` цели включаются (нужна полная цепочка KMP/native и вариант `core:common` под android native). |

**Изменения в коде:**

- `gradle.properties`: добавлено свойство `ipcss.buildAndroidNative=false`.
- `core/network/build.gradle.kts`: блоки с `androidNativeArm32`, `androidNativeArm64`, `androidNativeX86`, `androidNativeX64` и соответствующие source sets обёрнуты в `if (buildAndroidNative) { ... }`.

Приложение для Android по-прежнему собирается через **androidTarget()** (JVM/ART), а не через Kotlin/Native; ExoPlayer и т.д. не затрагиваются. Включение `androidNative*` нужно только при явной сборке под Kotlin/Native для Android NDK.

---

## 2. Репозитории и зависимость Koin

### Проблема

Версия `io.insert-koin:koin-core:3.6.0` не находилась в Maven Central (стабильный 3.6 для core не опубликован).

### Решение

В `gradle/libs.versions.toml` версия Koin изменена на **3.5.6** (стабильная в Maven Central).

```toml
koin = "3.5.6"
```

Все артефакты, использующие `version.ref = "koin"`, подхватывают эту версию. При необходимости перехода на 3.6+ — после появления стабильного артефакта обновить версию в каталоге.

---

## 3. Варианты сборки без Android

### Задачи в корневом `build.gradle.kts`

| Задача | Описание |
|--------|----------|
| `buildCoreDesktop` | Сборка только `:core:common` и `:core:network` для Desktop (JVM). Не требует Android SDK и не тянет native. Подходит для проверки, что core собирается. |
| `buildServerApi` | Сборка `:server:api` (JVM). Зависит от `:shared`; может падать, если в `:shared` есть ошибки компиляции. |
| `buildAll` | Полная сборка (в т.ч. native libraries, core, shared, network). Требует окружение и, при необходимости, `ipcss.buildAndroidNative=true` для androidNative*. |

### Примеры команд

```bash
# Только core (desktop) — стабильный вариант без Android/native
./gradlew buildCoreDesktop -x test

# Сервер API (после исправления ошибок в :shared при их наличии)
./gradlew buildServerApi -x test

# Сборка с тестами только для core
./gradlew :core:common:test :core:network:desktopTest -x test
```

### Сборка без Android SDK

- **Core (Desktop):** `buildCoreDesktop` или явно `:core:common:compileKotlinDesktop` и `:core:network:compileKotlinDesktop` — не требуют Android SDK.
- **Shared/Android app:** для `:shared:compileDebugKotlinAndroid` и `:android:app:assemble` нужны `ANDROID_HOME` или `sdk.dir` в `local.properties`.
- **Сервер:** `:server:api:build` не требует Android SDK, но зависит от `:shared`; при сборке shared для desktop могут потребоваться только JVM/Desktop-варианты.

---

## 4. Текущее состояние после правок

| Проверка | Результат |
|----------|-----------|
| Разрешение зависимостей для `:core:network` (без androidNative) | Успешно |
| Разрешение Koin (3.5.6) | Успешно |
| Сборка `:core:common:compileKotlinDesktop` и `:core:network:compileKotlinDesktop` | Успешно |
| Сборка `:server:api:build` | Зависит от `:shared`; при наличии ошибок компиляции в `:shared` падает (не из-за KMP/Koin). |

---

## 5. С чего продолжить

1. **Проверить сборку core (desktop):**  
   `.\gradlew buildCoreDesktop -x test`  
   Должна завершаться успешно при `ipcss.buildAndroidNative=false` и Koin 3.5.6.

2. **Исправить ошибки компиляции в `:shared`** (если нужна сборка server/api или полная):  
   В логе сборки `:shared:compileKotlinDesktop` фигурируют, в частности:
   - `AnalyzeVideoUseCase` / `DetectMotionUseCase` — неразрешённые ссылки на поля (например, `enableMotionDetection`, `motionThreshold` и т.д.).
   - `BackgroundWorker.desktop.kt` — вызов suspend из не-suspend контекста.
   - `DatabaseFactory.desktop.kt` — некорректное использование `actual`.
   - `AnalyticsServiceImpl` (desktop/jvm) — неразрешённые ссылки, несовпадение типов.
   - `NasPlatformDetectorImpl` — отсутствие ожидаемой декларации, неразрешённые ссылки.
   - `NativeMotionDetector` / `NativeObjectDetector` / `NativeObjectTracker` — дублирование companion object, неразрешённые ссылки.

   После исправления этих мест повторить `.\gradlew buildServerApi -x test`.

3. **Полная сборка с Android:**  
   - Задать `ANDROID_HOME` или `sdk.dir` в `local.properties`.  
   - При необходимости сборки androidNative* в `:core:network`: в `gradle.properties` выставить `ipcss.buildAndroidNative=true` и обеспечить публикацию (или наличие) вариантов `:core:common` под соответствующие native-цели либо убрать зависимость network от common для этих целей.

4. **Дальнейшие шаги по плану MVP:**  
   См. [POETAPNYJ_PLAN_REALIZACII.md](POETAPNYJ_PLAN_REALIZACII.md) и [MVP_PHASED_IMPLEMENTATION_PLAN.md](MVP_PHASED_IMPLEMENTATION_PLAN.md) (фаза 1.4.3 — ручная проверка ONVIF; фаза 3.1.1 — тесты RTSP при необходимости).

---

## Связанные документы

- [TESTING_EXECUTION_RESULT_PHASE0.md](TESTING_EXECUTION_RESULT_PHASE0.md) — результат проверки сборки по Фазе 0.
- [POETAPNYJ_PLAN_REALIZACII.md](POETAPNYJ_PLAN_REALIZACII.md) — поэтапный план реализации.
- [MVP_PHASED_IMPLEMENTATION_PLAN.md](MVP_PHASED_IMPLEMENTATION_PLAN.md) — детальные задачи и статусы.
