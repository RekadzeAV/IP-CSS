# Структура проекта IP Camera Surveillance System (Автоматически сгенерировано)

> **⚠️ ВНИМАНИЕ:** Этот файл автоматически генерируется скриптом `scripts/generate-project-structure.py`
> 
> **Не редактируйте этот файл вручную!** Все изменения будут перезаписаны при следующей генерации.
> 
> Для изменения структуры проекта редактируйте файлы проекта, а затем запустите скрипт генерации.

**Дата генерации:** 2025-12-27 18:24:32

## Статистика проекта

- **Всего файлов:** 652
- **Всего директорий:** 352
- **Модулей:** 14

### Статистика по типам файлов

- `.kt`: 253 файлов
- `.md`: 227 файлов
- `.ts`: 25 файлов
- `.sh`: 23 файлов
- `.tsx`: 16 файлов
- `.cpp`: 14 файлов
- `.h`: 14 файлов
- `.ps1`: 13 файлов
- `.yml`: 8 файлов
- `.yaml`: 4 файлов
- `.txt`: 4 файлов
- `.xml`: 3 файлов
- `.json`: 2 файлов
- `.bat`: 1 файлов
- `.js`: 1 файлов
- `.py`: 1 файлов
- `.sq`: 1 файлов

## Дерево структуры проекта

```
IP-CSS/
├── .github/
│   ├── pull_request_template.md
│   └── workflows/
├── BUILD_NATIVE_LIBRARY.md
├── CHANGELOG.md
├── CONTRIBUTING.md
├── CRITICAL_BLOCKERS_REMEDIATION_PLAN.md
├── CURRENT_STATUS.md
├── DEVELOPMENT_MAP.md
├── DEVELOPMENT_ROADMAP.md
├── DOCUMENTATION_ANALYSIS_TABLE.md
├── DOCUMENTATION_INDEX.md
├── DOCUMENTATION_V2_SUMMARY.md
├── Dockerfile
├── IMPLEMENTATION_TASKS.md
├── INSTALLATION_SUMMARY.md
├── INSTALL_INSTRUCTIONS.md
├── Inf-pipeline/
│   ├── INFRASTRUCTURE_ANALYSIS.md
│   ├── RASPBERRY_PI_JENKINS_ANALYSIS.md
│   ├── README.md
│   └── SYNOLOGY_GIT_MIRROR_ANALYSIS.md
├── LICENSE
├── LOCAL_BUILD_REQUIREMENTS.md
├── Log-server/
│   ├── RASPBERRY_PI_4_ANALYSIS.md
│   └── README.md
├── NAS_PLATFORM_STATUS_TABLE.md
├── NATIVE_LIBRARIES_ANALYSIS.md
├── NATIVE_LIBRARIES_BUILD_COMPLETE.md
├── NATIVE_LIBRARIES_BUILD_STATUS.md
├── NATIVE_LIBRARIES_FIXES_SUMMARY.md
├── NATIVE_LIBRARIES_INTEGRATION_COMPLETE.md
├── OLD-DOC-2025-01-27/
│   └── README.md
├── PLATFORM_STRUCTURE.md
├── PROJECT_DOCUMENTATION_ANALYSIS.md
├── PROJECT_PROMPT.md
├── PROJECT_REVIEW.md
├── PROJECT_ROADMAP.md
├── PROJECT_STATUS.md
├── PROJECT_STATUS_TABLE.md
├── PROJECT_STRUCTURE.md
├── PROJECT_STRUCTURE_AUTO.md
├── QUICK_INSTALL.md
├── README.md
├── RECOMMENDATIONS_COMPLETED.md
├── android/
│   └── app/
│       ├── build.gradle.kts
│       └── src/
│           └── main/
│               ├── java/
│               └── res/
├── build.gradle.kts
├── core/
│   ├── common/
│   │   ├── README.md
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── androidMain/
│   │       │   └── kotlin/
│   │       ├── commonMain/
│   │       │   └── kotlin/
│   │       ├── desktopMain/
│   │       │   └── kotlin/
│   │       └── iosMain/
│   │           └── kotlin/
│   ├── license/
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── androidMain/
│   │       │   └── kotlin/
│   │       ├── commonMain/
│   │       │   └── kotlin/
│   │       ├── commonTest/
│   │       │   ├── README.md
│   │       │   └── kotlin/
│   │       └── iosMain/
│   │           └── kotlin/
│   └── network/
│       ├── README.md
│       ├── build.gradle.kts
│       └── src/
│           ├── androidMain/
│           │   └── kotlin/
│           ├── commonMain/
│           │   └── kotlin/
│           ├── iosMain/
│           │   └── kotlin/
│           ├── jvmMain/
│           │   └── kotlin/
│           ├── nativeInterop/
│           │   └── cinterop/
│           └── nativeMain/
│               └── kotlin/
├── detekt.yml
├── docker-compose.yml
├── docs/
│   ├── ACTIVATE_NATIVE_DECODER.md
│   ├── ADMINISTRATOR_GUIDE.md
│   ├── AI_ANALYTICS.md
│   ├── ANALYSIS_ERRORS.md
│   ├── ANALYSIS_SUMMARY_2025.md
│   ├── API.md
│   ├── API_EXAMPLES.md
│   ├── ARCHITECTURE.md
│   ├── BUILD_ORGANIZATION.md
│   ├── BUILD_QUICK_REFERENCE.md
│   ├── BUILD_TROUBLESHOOTING.md
│   ├── CONFIGURATION.md
│   ├── DEEP_ANALYSIS_2025.md
│   ├── DEPLOYMENT_GUIDE.md
│   ├── DESKTOP_IMPLEMENTATION_PLAN.md
│   ├── DEVELOPMENT.md
│   ├── DEVELOPMENT_PLAN.md
│   ├── DEVELOPMENT_TOOLS.md
│   ├── DOCUMENTATION_ANALYSIS_REPORT.md
│   ├── DOCUMENTATION_CONSOLIDATION_ANALYSIS.md
│   ├── DOCUMENTATION_CONSOLIDATION_REPORT.md
│   ├── DOCUMENTATION_GAPS.md
│   ├── DOCUMENTATION_MANAGEMENT.md
│   ├── DOCUMENTATION_OPTIMIZATION_PROPOSALS.md
│   ├── DOCUMENTATION_OPTIMIZATION_REPORT.md
│   ├── DOCUMENTATION_REVISION_REPORT.md
│   ├── DOCUMENTATION_UPDATE.md
│   ├── DOCUMENTATION_VERSIONING.md
│   ├── DOCUMENTATION_VERSIONING_QUICK_START.md
│   ├── ENVIRONMENT_VARIABLES.md
│   ├── FFMPEG_INSTALLATION.md
│   ├── FFMPEG_SETUP_REPORT.md
│   ├── FUNCTIONALITY_ANALYSIS.md
│   ├── IMPLEMENTATION_PROGRESS.md
│   ├── IMPLEMENTATION_STATUS.md
│   ├── INTEGRATION_COMPLETE.md
│   ├── INTEGRATION_GUIDE.md
│   ├── LIBRARIES_INTEGRATION_SUMMARY.md
│   ├── LICENSE_SYSTEM.md
│   ├── LOCAL_BUILD.md
│   ├── MISSING_FUNCTIONALITY.md
│   ├── NAS_BUILD_REQUIREMENTS_SUMMARY.md
│   ├── NAS_LOCAL_BUILD_REQUIREMENTS.md
│   ├── NAS_PLATFORMS_ANALYSIS.md
│   ├── NATIVE_LIBRARIES_INTEGRATION.md
│   ├── ONVIF_CLIENT.md
│   ├── OPERATOR_GUIDE.md
│   ├── OPTIMIZATION_COMPLETED.md
│   ├── OPTIMIZATION_PROGRESS.md
│   ├── PERFORMANCE.md
│   ├── PLATFORMS.md
│   ├── PROJECT_ANALYSIS_DISCREPANCIES.md
│   ├── PROJECT_FULL_ANALYSIS.md
│   ├── PROJECT_STRUCTURE_MANAGEMENT.md
│   ├── PROMPT_ANALYSIS.md
│   ├── README.md
│   ├── REQUIRED_LIBRARIES.md
│   ├── REQUIRED_LIBRARIES_SUMMARY.md
│   ├── RTSP_CLIENT.md
│   ├── RTSP_CLIENT_ACTIVATION_GUIDE.md
│   ├── RTSP_CLIENT_IMPLEMENTATION_STATUS.md
│   ├── RTSP_CLIENT_INTEGRATION.md
│   ├── RTSP_CLIENT_SETUP_SUMMARY.md
│   ├── RTSP_CODE_ACTIVATION_TEMPLATE.md
│   ├── RTSP_PLAYER_INTEGRATION_SUMMARY.md
│   ├── RTSP_QUICK_START.md
│   ├── RTSP_VIDEO_PLAYER_INTEGRATION.md
│   ├── SECURITY_AUDIT_REPORT.md
│   ├── SECURITY_BEST_PRACTICES.md
│   ├── SECURITY_REMEDIATION_PLAN.md
│   ├── SERVER_IMPLEMENTATION_COMPLETED.md
│   ├── SERVER_IMPLEMENTATION_PLAN.md
│   ├── SOLUTIONS_FOR_DISCREPANCIES.md
│   ├── TECHNICAL_DEBT.md
│   ├── TESTING.md
│   ├── TESTING_IMPLEMENTATION_PROGRESS.md
│   ├── TESTING_PLAN_ANALYSIS.md
│   ├── TESTS_SUMMARY.md
│   ├── TEST_DOCUMENTATION.md
│   ├── TIMELINE_GUIDE.md
│   ├── TIMELINE_SETUP.md
│   ├── TROUBLESHOOTING.md
│   ├── TYPESCRIPT_NAVIGATION_EXTENSIONS.md
│   ├── USER_GUIDE.md
│   ├── USER_MANAGEMENT_SSO_KERBEROS_ANALYSIS.md
│   ├── VIDEO_CODECS.md
│   ├── VIDEO_DECODER_COMPLETE.md
│   ├── VIDEO_DECODER_INTEGRATION.md
│   ├── VIDEO_DECODER_SETUP.md
│   ├── VIDEO_RECORDING_ENHANCEMENTS.md
│   ├── VIDEO_RECORDING_IMPLEMENTATION.md
│   ├── VSCODE_EXTENSIONS.md
│   ├── WEBSOCKET_CLIENT.md
│   ├── archive/
│   │   ├── 2025-12-27/
│   │   │   └── README.md
│   │   ├── OLD-DOC-2025-01-27/
│   │   │   ├── README.md
│   │   └── README.md
│   ├── reports/
│   └── rtsp/
├── gradle.properties
├── gradlew
├── gradlew.bat
├── native/
│   ├── CMakeLists.txt
│   ├── analytics/
│   │   ├── CMakeLists.txt
│   │   ├── include/
│   │   │   ├── anpr_engine.h
│   │   │   ├── face_detector.h
│   │   │   ├── motion_detector.h
│   │   │   ├── object_detector.h
│   │   │   └── object_tracker.h
│   │   └── src/
│   │       ├── anpr_engine.cpp
│   │       ├── face_detector.cpp
│   │       ├── motion_detector.cpp
│   │       ├── object_detector.cpp
│   │       └── object_tracker.cpp
│   ├── build-stub-libs.ps1
│   ├── codecs/
│   │   ├── CMakeLists.txt
│   │   ├── include/
│   │   │   ├── codec_manager.h
│   │   │   ├── h264_codec.h
│   │   │   ├── h265_codec.h
│   │   │   └── mjpeg_codec.h
│   │   └── src/
│   │       ├── codec_manager.cpp
│   │       ├── h264_codec.cpp
│   │       ├── h265_codec.cpp
│   │       └── mjpeg_codec.cpp
│   └── video-processing/
│       ├── CMakeLists.txt
│       ├── README.md
│       ├── include/
│       │   ├── frame_processor.h
│       │   ├── rtsp_client.h
│       │   ├── stream_manager.h
│       │   ├── video_decoder.h
│       │   └── video_encoder.h
│       └── src/
│           ├── frame_processor.cpp
│           ├── rtsp_client.cpp
│           ├── stream_manager.cpp
│           ├── video_decoder.cpp
│           └── video_encoder.cpp
├── platforms/
│   ├── client-android/
│   │   └── README.md
│   ├── client-desktop-arm/
│   │   ├── README.md
│   │   └── app/
│   │       ├── README.md
│   │       ├── build.gradle.kts
│   │       └── src/
│   │           └── main/
│   ├── client-desktop-x86_64/
│   │   ├── README.md
│   │   └── app/
│   │       ├── README.md
│   │       ├── build.gradle.kts
│   │       └── src/
│   │           └── main/
│   ├── client-ios/
│   │   └── README.md
│   ├── nas-arm/
│   │   ├── README.md
│   │   ├── docker/
│   │   │   ├── README.md
│   │   │   └── docker-compose.yml
│   │   ├── packages/
│   │   │   ├── README.md
│   │   │   ├── asustor/
│   │   │   │   ├── icons/
│   │   │   │   └── scripts/
│   │   │   ├── qnap/
│   │   │   │   ├── icons/
│   │   │   │   └── scripts/
│   │   │   ├── synology/
│   │   │   │   ├── icons/
│   │   │   │   └── scripts/
│   │   │   └── truenas/
│   │   │       ├── docker-compose.yml
│   │   │       └── kubernetes/
│   │   └── server/
│   │       ├── README.md
│   ├── nas-x86_64/
│   │   ├── README.md
│   │   ├── docker/
│   │   │   ├── Dockerfile
│   │   │   ├── README.md
│   │   │   └── docker-compose.yml
│   │   ├── packages/
│   │   │   ├── README.md
│   │   │   ├── asustor/
│   │   │   │   ├── icons/
│   │   │   │   └── scripts/
│   │   │   ├── qnap/
│   │   │   │   ├── icons/
│   │   │   │   └── scripts/
│   │   │   ├── synology/
│   │   │   │   ├── icons/
│   │   │   │   └── scripts/
│   │   │   └── truenas/
│   │   │       ├── docker-compose.yml
│   │   │       └── kubernetes/
│   │   └── server/
│   │       ├── README.md
│   ├── sbc-arm/
│   │   └── README.md
│   └── server-x86_64/
│       └── README.md
├── scripts/
│   ├── README.md
│   ├── README_DOCUMENTATION_MANAGEMENT.md
│   ├── activate-rtsp-client.sh
│   ├── archive-documentation.ps1
│   ├── archive-documentation.sh
│   ├── build-all-native-libs.ps1
│   ├── build-all-native-libs.sh
│   ├── build-all-platforms.sh
│   ├── build-nas-package.sh
│   ├── build-native-lib.sh
│   ├── build-video-processing-lib.ps1
│   ├── build-video-processing-lib.sh
│   ├── check-dependencies.ps1
│   ├── check-timeline.sh
│   ├── cleanup-old-branches.ps1
│   ├── create-platform-branches.ps1
│   ├── create-platform-branches.sh
│   ├── generate-project-structure.py
│   ├── install-build-dependencies.ps1
│   ├── install-ffmpeg.ps1
│   ├── install-ffmpeg.sh
│   ├── install-vscode-extensions.ps1
│   ├── install-vscode-extensions.sh
│   ├── manage-documentation.ps1
│   ├── manage-documentation.sh
│   ├── monitor-ffmpeg-performance.ps1
│   ├── monitor-ffmpeg-performance.sh
│   ├── publish-local.sh
│   ├── quick-install.ps1
│   └── setup-git-hooks.sh
├── server/
│   ├── api/
│   │   ├── build.gradle.kts
│   │   └── src/
│   │       ├── main/
│   │       │   └── kotlin/
│   │       └── test/
│   │           └── kotlin/
│   └── web/
│       ├── README.md
│       ├── next.config.js
│       ├── package.json
│       ├── src/
│       │   ├── app/
│       │   │   ├── cameras/
│       │   │   ├── dashboard/
│       │   │   ├── events/
│       │   │   ├── layout.tsx
│       │   │   ├── login/
│       │   │   ├── page.tsx
│       │   │   ├── providers.tsx
│       │   │   ├── recordings/
│       │   │   └── settings/
│       │   ├── components/
│       │   │   ├── CameraCard/
│       │   │   ├── Layout/
│       │   │   ├── ProtectedRoute/
│       │   │   ├── VideoPlayer/
│       │   │   ├── WebSocketNotificationHandler/
│       │   │   └── WebSocketProvider/
│       │   ├── hooks/
│       │   │   └── useWebSocket.ts
│       │   ├── services/
│       │   │   ├── authService.ts
│       │   │   ├── cameraService.ts
│       │   │   ├── eventService.ts
│       │   │   ├── recordingService.ts
│       │   │   ├── settingsService.ts
│       │   │   └── streamService.ts
│       │   ├── store/
│       │   │   ├── hooks.ts
│       │   │   ├── index.ts
│       │   │   └── slices/
│       │   ├── types/
│       │   │   ├── env.d.ts
│       │   │   └── index.ts
│       │   └── utils/
│       │       ├── api.ts
│       │       └── websocket.ts
│       └── tsconfig.json
├── settings.gradle.kts
└── shared/
    ├── build.gradle.kts
    └── src/
        ├── androidMain/
        │   └── kotlin/
        │       └── com/
        ├── commonMain/
        │   ├── kotlin/
        │   │   └── com/
        │   └── sqldelight/
        │       └── com/
        ├── commonTest/
        │   ├── README.md
        │   └── kotlin/
        │       └── com/
        ├── desktopMain/
        │   └── kotlin/
        │       └── com/
        └── iosMain/
            └── kotlin/
                └── com/
```

## Модули проекта

### Gradle модули (Kotlin Multiplatform / Android)

- **`.`**
  - Build file: `build.gradle.kts`
- **`android/app`**
  - Build file: `android/app/build.gradle.kts`
- **`core/common`**
  - Build file: `core/common/build.gradle.kts`
- **`core/license`**
  - Build file: `core/license/build.gradle.kts`
- **`core/network`**
  - Build file: `core/network/build.gradle.kts`
- **`platforms/client-desktop-arm/app`**
  - Build file: `platforms/client-desktop-arm/app/build.gradle.kts`
- **`platforms/client-desktop-x86_64/app`**
  - Build file: `platforms/client-desktop-x86_64/app/build.gradle.kts`
- **`server/api`**
  - Build file: `server/api/build.gradle.kts`
- **`shared`**
  - Build file: `shared/build.gradle.kts`

### CMake модули (C++ нативные библиотеки)

- **`native`**
  - Build file: `native/CMakeLists.txt`
- **`native/analytics`**
  - Build file: `native/analytics/CMakeLists.txt`
- **`native/codecs`**
  - Build file: `native/codecs/CMakeLists.txt`
- **`native/video-processing`**
  - Build file: `native/video-processing/CMakeLists.txt`

### Node.js модули

- **`server/web`**
  - Build file: `server/web/package.json`

## Описание основных директорий

### `core/` - Кроссплатформенные модули
- `core/common/` - Общие типы и утилиты
- `core/license/` - Система лицензирования
- `core/network/` - Сетевое взаимодействие (Ktor, ONVIF, RTSP, WebSocket)

### `shared/` - Kotlin Multiplatform модуль
Основной модуль с кроссплатформенной бизнес-логикой:
- `commonMain/` - Общий код для всех платформ
- `androidMain/` - Android-специфичные реализации
- `iosMain/` - iOS-специфичные реализации
- `desktopMain/` - Desktop-специфичные реализации
- `commonTest/` - Тесты

### `native/` - C++ нативные библиотеки
- `video-processing/` - Обработка видеопотоков
- `analytics/` - AI-аналитика (детекция объектов, лиц, движения, ANPR)
- `codecs/` - Поддержка кодеков (H.264, H.265, MJPEG)

### `server/` - Серверная часть
- `server/api/` - REST API сервер (Ktor)
- `server/web/` - Веб-интерфейс (Next.js)

### `android/` - Android приложение
- `android/app/` - Android app модуль

### `platforms/` - Платформо-специфичные реализации
- `sbc-arm/` - Микрокомпьютеры ARM
- `server-x86_64/` - Серверы x86-x64
- `nas-arm/` - NAS ARM
- `nas-x86_64/` - NAS x86-x64
- `client-desktop-x86_64/` - Клиенты Desktop x86-x64
- `client-desktop-arm/` - Клиенты Desktop ARM
- `client-android/` - Клиенты Android
- `client-ios/` - Клиенты iOS/macOS

### `docs/` - Документация
Вся документация проекта

### `scripts/` - Скрипты
Скрипты для сборки, развертывания и автоматизации

## Зависимости между модулями

```
android/ios/desktop приложения (platforms/)
    ↓
shared (KMM)
    ↓     ↓
    ↓  core:network
    ↓     ↓
    ↓  core:common (базовые типы: Resolution, CameraStatus)
    ↓
core:license
    ↓
native (C++ библиотеки через FFI, не Gradle модули)
```

## Как обновить структуру

Структура автоматически обновляется:
1. При каждом коммите через GitHub Actions (CI)
2. Вручную: запустите `./scripts/generate-project-structure.py`

Для локального запуска:
```bash
python3 scripts/generate-project-structure.py
```

## Примечания

- Игнорируются: `.git`, `.gradle`, `build`, `node_modules`, `.next`, и другие служебные директории
- Показываются только важные файлы и файлы с кодом
- Максимальная глубина дерева: 5 уровней
