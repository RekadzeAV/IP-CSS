# Результат проверки сборки и тестов (Фаза 0.2)

**Дата:** 2026-03-01  
**Источник:** Поэтапный план реализации, задача 0.2.

---

## Выполненные действия

1. Запуск полной сборки: `.\gradlew.bat build --no-daemon -x test`
2. Исправлен `platforms/client-desktop-arm/app/build.gradle.kts`: заменён устаревший `kotlinOptions { jvmTarget = "11" }` на `compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }` (миграция на Kotlin 2.x DSL).
3. Попытка сборки основных модулей: `.\gradlew.bat buildAll --no-daemon`

---

## Результат

**Сборка:** не проходит.

### Текущие причины

1. **Полная сборка (`build`):** падает на конфигурации `:platforms:client-desktop-arm:app` с требованием плагина `org.jetbrains.kotlin.plugin.compose` (плагин в блоке `plugins` уже указан; возможна проблема порядка или кэша).
2. **buildAll / полная сборка:** падает при разрешении зависимостей `:core:network` — для конфигурации `androidNativeX64CompilationDependenciesMetadata` не находится подходящий вариант проекта `:core:common` (core:common не публикует вариант под `android_x64`/native).
3. **Сборка без Android:** при сборке только `:server:api` или `:shared:compileKotlinDesktop` возможна ошибка разрешения зависимостей (например, `io.insert-koin:koin-core:3.6.0` не найден в репозиториях). На машине без ANDROID_HOME сборка модулей с Android target не выполняется.

### Что уже исправлено в рамках 0.2

- В `platforms/client-desktop-arm/app/build.gradle.kts`: убран неверный блок `jvm("desktop") { compilations.all { kotlinOptions } }` (для плагина `kotlin("jvm")` нет named target); заменён на `kotlin { jvmToolchain(11) }`.
- В `android/app/build.gradle.kts`: устаревший `kotlinOptions { jvmTarget = "11" }` заменён на `tasks.withType<KotlinCompile>().configureEach { compilerOptions { jvmTarget.set(JvmTarget.JVM_11) } }`; зависимости Compose — `compose.uiTooling`, `androidx.activity:activity-compose:1.9.3`.

### Рекомендации для прохождения сборки

1. **Desktop-arm:** убедиться, что плагин Compose применяется до использования Compose DSL; при необходимости выполнить `.\gradlew.bat clean` и повторную сборку.
2. **core:network + core:common:** применены правки стабилизации — см. [BUILD_STABILIZATION.md](BUILD_STABILIZATION.md). В `gradle.properties` задано `ipcss.buildAndroidNative=false`; цели androidNative* в `:core:network` отключены по умолчанию, разрешение зависимостей проходит. Для сборки только core (desktop): `.\gradlew.bat buildCoreDesktop -x test`.
3. **Koin:** версия в `libs.versions.toml` изменена на 3.5.6 (стабильная в Maven Central).
4. **Тесты:** после успешной сборки выполнить, например:  
   `.\gradlew.bat testAll --no-daemon`  
   или прогон тестов по отдельным модулям (`:shared:test`, `:core:network:desktopTest`, `:server:api:test` и т.д.) по гайду [TESTING_EXECUTION_GUIDE.md](TESTING_EXECUTION_GUIDE.md).

---

## Ссылки

- Стабилизация сборки: [BUILD_STABILIZATION.md](BUILD_STABILIZATION.md).
- План: [ПЛАН_РЕАЛИЗАЦИИ.md](ПЛАН_РЕАЛИЗАЦИИ.md) (Фаза 0.2), [POETAPNYJ_PLAN_REALIZACII.md](POETAPNYJ_PLAN_REALIZACII.md).
- Гайд по тестам: [TESTING_EXECUTION_GUIDE.md](TESTING_EXECUTION_GUIDE.md).
