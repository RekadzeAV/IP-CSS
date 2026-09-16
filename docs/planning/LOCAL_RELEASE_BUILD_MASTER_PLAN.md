# Локальная релизная сборка: мастер-план по типам выпуска

**Версия проекта:** Alfa-0.1.1  
**Последнее обновление:** 27 March 2026

## Цель

Этот документ объединяет процессы локальной релизной сборки по каждому типу выпуска и служит точкой входа для проверки готовности окружения.

## Карта типов выпуска

1. **Android APK Release**  
   Документ: `docs/planning/LOCAL_RELEASE_ANDROID_APK.md`
2. **Android AAB Release**  
   Документ: `docs/planning/LOCAL_RELEASE_ANDROID_AAB.md`
3. **Desktop x86_64 Release**  
   Документ: `docs/planning/LOCAL_RELEASE_DESKTOP_X86_64.md`
4. **Desktop ARM Release**  
   Документ: `docs/planning/LOCAL_RELEASE_DESKTOP_ARM.md`
5. **Server JVM Release Build**  
   Документ: `docs/planning/LOCAL_RELEASE_SERVER_JVM.md`
6. **NAS Packages Release Build**  
   Документ: `docs/planning/LOCAL_RELEASE_NAS_PACKAGES.md`

## Общие требования окружения

- JDK 17 (установлен и доступен в PATH)
- Android SDK (для Android типов сборки)
- Gradle Wrapper (`gradlew.bat`/`gradlew`)
- Доступ к репозиториям зависимостей

## Базовый порядок проверки

1. Выполнить проверку окружения (`java -version`, `./gradlew -version`)
2. Проверить release-задачу конкретного типа выпуска
3. Зафиксировать результат (green/red) и блокеры
4. Перейти к следующему типу

## Текущее состояние (на момент обновления)

- Android APK Release: GREEN (`:android:app:assembleRelease`)
- Android AAB Release: GREEN (`:android:app:bundleRelease`)
- Desktop x86_64 Release: GREEN (`:platforms:client-desktop-x86_64:app:packageReleaseDistributionForCurrentOS`)
- Desktop ARM Release: GREEN (`:platforms:client-desktop-arm:app:packageReleaseDistributionForCurrentOS`)
- Server JVM Release Build: GREEN (`:server:api:build`)
- NAS Packages Release Build: GREEN (все `buildNasPackage*` задачи проходят локально)

## Связанные документы

- `docs/LOCAL_BUILD.md`
- `docs/BUILD_ORGANIZATION.md`
- `docs/BUILD_TROUBLESHOOTING.md`
- `docs/status/PROJECT_STATUS.md`
- `docs/planning/RELEASE_GO_NO_GO_CHECKLIST.md`
