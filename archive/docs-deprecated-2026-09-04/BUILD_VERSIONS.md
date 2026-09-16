# Версии сборки и зависимости

**Версия документации:** 1.0  
**Дата создания:** 5 июля 2026  
**Дата последнего обновления:** 5 июля 2026

## 📋 Актуальные версии компонентов

### Kotlin и плагины
- **Kotlin:** 2.0.0
- **Kotlin Multiplatform:** 2.0.0
- **Kotlin Android:** 2.0.0
- **Kotlin Serialization:** 2.0.0
- **Kotlin Compose:** 2.0.0

### Android Gradle Plugin (AGP)
- **AGP Application:** 8.3.0
- **AGP Library:** 8.3.0

> **Примечание:** AGP 8.3.0 выбран для совместимости с Kotlin 2.0.0. AGP 8.7.3+ требует Kotlin 2.1.0+.

### Gradle
- **Gradle Wrapper:** Проверить в `gradle/wrapper/gradle-wrapper.properties`
- **Рекомендуемая версия:** 8.5+ (для AGP 8.3.0)

### Ktor
- **Ktor:** 2.3.5
- **Ktor Client Core:** 2.3.5
- **Ktor Client Android:** 2.3.5
- **Ktor Client OkHttp:** 2.3.5
- **Ktor Client Java:** 2.3.5
- **Ktor Client Darwin:** 2.3.5
- **Ktor Client Content Negotiation:** 2.3.5
- **Ktor Serialization Kotlinx JSON:** 2.3.5
- **Ktor Serialization Kotlinx XML:** 2.3.5
- **Ktor Client Logging:** 2.3.5
- **Ktor Client WebSockets:** 2.3.5
- **Ktor Server Core:** 2.3.5
- **Ktor Server Netty:** 2.3.5
- **Ktor Server Content Negotiation:** 2.3.5

### SQLDelight
- **SQLDelight:** 2.0.2
- **SQLDelight Runtime:** 2.0.0
- **SQLDelight Android Driver:** 2.0.0
- **SQLDelight Native Driver:** 2.0.0
- **SQLDelight SQLite Driver:** 2.0.0
- **SQLDelight Async Extensions:** 2.0.0

### Kotlinx
- **Kotlinx Coroutines:** 1.7.3
- **Kotlinx Serialization JSON:** 1.7.0
- **Kotlinx DateTime:** 0.6.0

### Логирование
- **Kotlin Logging:** 3.0.5

### AndroidX
- **AndroidX Core KTX:** 1.12.0
- **AndroidX Lifecycle Runtime KTX:** 2.6.2
- **AndroidX Security Crypto:** 1.1.0-alpha06
- **AndroidX Work Runtime KTX:** 2.9.0

### Безопасность
- **BouncyCastle Provider:** 1.70
- **BouncyCastle PKIX:** 1.70
- **JBCrypt:** 0.9.0 (используется в core:security)

### Тестирование
- **MockK:** 1.13.8
- **Turbine:** 1.0.0

### Dependency Injection
- **Koin Core:** 3.5.3
- **Koin Test:** 3.5.3

### Compose
- **Compose:** 1.7.1

### Инструменты сборки
- **Detekt:** 1.23.6
- **Ktlint:** 12.1.2
- **Dokka:** 1.9.20
- **Kover:** 0.9.1

### Computer Vision (опционально)
- **OpenCV Android:** 4.8.0
- **TensorFlow Lite:** 2.14.0
- **TensorFlow Lite GPU:** 2.14.0
- **TensorFlow Lite Support:** 0.4.4

## 🔧 Флаги сборки

### Основные флаги (gradle.properties)
```properties
# Нативные сборки
ipcss.buildAndroidNative=false
ipcss.buildNative=false

# Платформы
ipcss.buildKotlin=true
ipcss.buildWeb=true
ipcss.buildDocker=false

# Оптимизация тестов
ipcss.skipNativeTargets=true
ipcss.disableNativeTargets=true
```

### Kotlin MPP флаги
```properties
kotlin.mpp.applyDefaultHierarchyTemplate=false
kotlin.mpp.enableCInteropCommonization=true
kotlin.native.ignoreDisabledTargets=true
```

## 📦 Структура артефактов

### Release сборки
```
build/release/
├── android/          # Android APK/AAB
├── desktop/          # Desktop JAR/EXE
├── ios/              # iOS frameworks
├── linux/            # Linux binaries
└── windows/          # Windows binaries
```

### Логи сборки
```
build-logs/
├── platform-{name}-{timestamp}.log    # Детальные логи по платформе
└── build-summary-{timestamp}.txt      # Сводный отчет
```

## 🚀 Быстрый старт

### Сборка для Desktop (JVM)
```bash
./gradlew :core:common:compileKotlinDesktop
./gradlew :core:network:compileKotlinDesktop
./gradlew :shared:compileKotlinDesktop
```

### Сборка для Android
```bash
./gradlew :core:common:compileKotlinAndroid
./gradlew :shared:build
```

### Полная сборка (JVM + Android)
```bash
./gradlew buildCoreDesktop
./gradlew buildServerApi
```

### Тестирование платформ с логированием
```bash
# Windows
.\scripts\test-platform-builds.ps1

# Linux/macOS
bash scripts/test-platform-builds.sh
```

## ⚠️ Известные ограничения

1. **Нативные таргеты** отключены по умолчанию (`ipcss.buildNative=false`)
2. **Android Native** отключен по умолчанию (`ipcss.buildAndroidNative=false`)
3. **AGP 8.3.0** используется вместо 8.7.3+ для совместимости с Kotlin 2.0.0
4. **Default hierarchy template** отключен (`kotlin.mpp.applyDefaultHierarchyTemplate=false`)

## 📝 История изменений

### Версия 1.0 (5 июля 2026)
- Создание документации
- Фиксация версий после стабилизации сборки
- AGP понижен до 8.3.0 для совместимости с Kotlin 2.0.0
- Отключен default hierarchy template
- Условные нативные таргеты в core:security и shared

## 🔗 Связанные документы

- [PROJECT_STRUCTURE.md](../PROJECT_STRUCTURE.md) - Структура проекта
- [BUILD_GUIDE.md](../BUILD_GUIDE.md) - Руководство по сборке
- [DEVELOPMENT.md](../DEVELOPMENT.md) - Руководство для разработчиков
- [ARCHITECTURE.md](../ARCHITECTURE.md) - Архитектура проекта