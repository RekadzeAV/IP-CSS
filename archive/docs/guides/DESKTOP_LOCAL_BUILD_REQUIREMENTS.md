# Требования для локальной сборки Desktop (IP-CSS)

**Дата:** 1 марта 2026

## 1. Необходимое ПО и окружение

| Требование | Версия / примечание |
|------------|----------------------|
| **JDK** | **17+** (рекомендуется 17; в проекте часть модулей настроена на JVM 11, но для сборки desktop на Windows с текущим toolchain нужен JDK 17 в PATH) |
| **Gradle** | Используется обёртка проекта: `gradlew.bat` (Windows) / `./gradlew` (Linux, macOS). Текущая версия в wrapper: **8.9** |
| **ОС** | Windows 10/11 (x64), Linux (x64), macOS (Intel или Apple Silicon) для сборки и запуска desktop-клиента |
| **Сеть** | Доступ к Maven Central, Google, JetBrains Compose Dev, JitPack (для загрузки зависимостей) |

### Проверка окружения

```powershell
# Windows (PowerShell)
java -version    # Должно быть 17 или выше
.\gradlew.bat --version
```

```bash
# Linux / macOS
java -version
./gradlew --version
```

---

## 2. Модули, участвующие в сборке Desktop

Сборка desktop-приложения задействует следующие модули (в порядке зависимостей):

1. **`:shared`** — общая KMP-логика (в т.ч. `desktop` target)
2. **`:core:common`** — общие утилиты и безопасность (desktop source set)
3. **`:core:network`** — сеть, ONVIF, RTSP (desktop target; на Windows без нативных библиотек часть native/Android не собирается)
4. **`:core:license`** — лицензирование (desktop target; см. блокеры ниже)
5. **`:platforms:client-desktop-x86_64:app`** — приложение Compose Desktop (JVM)

Команда сборки desktop (только компиляция Kotlin):

```bash
./gradlew :platforms:client-desktop-x86_64:app:compileKotlin
```

Полная сборка и создание дистрибутива (JAR/пакеты под ОС):

```bash
./gradlew :platforms:client-desktop-x86_64:app:packageDistributionForCurrentOS
# или
./gradlew :platforms:client-desktop-x86_64:app:run
```

---

## 3. Внесённые исправления в конфигурацию (для успешной конфигурации сборки)

- **`platforms/client-desktop-x86_64/app/build.gradle.kts`**
  - Для плагина `kotlin("jvm")` заменён неверный блок `jvm("desktop")` на `jvmToolchain(17)` (под текущее окружение с JDK 17).
  - Добавлен плагин **`org.jetbrains.kotlin.plugin.compose`** (обязателен для Compose Multiplatform с Kotlin 2.x).

- **`build.gradle.kts` (корень)**
  - В блок `plugins` добавлен:  
    `id("org.jetbrains.kotlin.plugin.compose") version "2.3.10" apply false`

- **`core/license/build.gradle.kts`**
  - `android()` заменён на `androidTarget()`.
  - Устаревший `kotlinOptions { jvmTarget = "11" }` заменён на `compilerOptions { jvmTarget.set(JVM_11) }`.
  - `desktopMain` объявлен как `by getting` (не `by creating`), т.к. создаётся таргетом `jvm("desktop")`.

---

## 4. Текущие блокеры полной сборки Desktop

### 4.1. Модуль `:core:license`

- В `commonMain` объявлены `expect`-типы/функции:  
  `PlatformCrypto`, `LicenseRepository`, `getPlatformCrypto`, `createLicenseRepository`.
- Реализации есть только в `androidMain` и `iosMain`; для JVM/desktop **actual-реализаций нет**, поэтому `:core:license:compileKotlinDesktop` падает.
- **Варианты:**  
  - Добавить в `core/license` source set `jvmMain`/`desktopMain` и реализовать там actual для перечисленных expect, **или**  
  - Временно убрать зависимость desktop-приложения от `:core:license` (в корне он помечен как отложенный).

### 4.2. Модуль `:core:network`

- При сборке `compileKotlinDesktop` возникают ошибки компиляции в общем коде (например, `OnvifEventIntegrationService`, `OnvifEventParser`, `OnvifEventServiceImpl`, `OnvifImagingServiceImpl` и др.): неразрешённые ссылки (типы из `shared`, XML, таймауты движка Ktor и т.д.).
- Это общие ошибки модуля `core:network` для desktop-таргета, а не только настройка окружения. Для успешной сборки desktop их нужно исправить в коде.

---

## 5. Краткий чек-лист для локальной сборки Desktop

- [ ] Установлен **JDK 17** (или выше), `JAVA_HOME` указывает на него, `java -version` показывает 17+.
- [ ] В корне проекта выполняется `.\gradlew.bat --version` (или `./gradlew --version`) без ошибок.
- [ ] Есть доступ в интернет к Maven Central, Google, JetBrains Compose Dev, JitPack.
- [ ] (Опционально) Для полной сборки с лицензированием: в `:core:license` добавлены JVM/desktop actual для всех expect.
- [ ] (Обязательно для успешной сборки) В `:core:network` исправлены ошибки компиляции для desktop-таргета (см. вывод `:core:network:compileKotlinDesktop`).

После устранения блокеров в `core:license` и `core:network` команда сборки desktop:

```bash
./gradlew :platforms:client-desktop-x86_64:app:compileKotlin
```

должна завершаться успешно при наличии JDK 17 и корректном окружении.
