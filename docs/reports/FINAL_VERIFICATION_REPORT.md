# Финальный отчет проверки релизных сборок

**Дата:** 27 марта 2026  
**Версия проекта:** Alfa-0.1.1

## Итоговая матрица готовности

| Тип релизной сборки | Команда проверки | Статус |
|---|---|---|
| Android APK | `:android:app:assembleRelease` | ✅ GREEN |
| Android AAB | `:android:app:bundleRelease` | ✅ GREEN |
| Desktop x86_64 | `:platforms:client-desktop-x86_64:app:packageReleaseDistributionForCurrentOS` | ✅ GREEN |
| Desktop ARM | `:platforms:client-desktop-arm:app:packageReleaseDistributionForCurrentOS` | ✅ GREEN |
| Server JVM | `:server:api:build` | ✅ GREEN |
| NAS Packages (all) | `buildNasPackage*`, `buildNasPackages` | ✅ GREEN |

## Выполненные ключевые доработки

1. **Android release pipeline**
   - Закрыты compile/lint блокеры для `assembleRelease` и `bundleRelease`.
   - Успешно формируются APK и AAB.

2. **Desktop release pipeline**
   - Закрыты compile блокеры ARM (`try/catch` вокруг composable, `WindowPosition`, иконки).
   - Для стабильной упаковки отключен ProGuard в desktop release build type.
   - Успешно формируются desktop release дистрибутивы для x86_64 и ARM профилей.

3. **Server + NAS release pipeline**
   - `:server:api:build` стабильно проходит.
   - NAS задачи адаптированы под Windows (`.ps1`), добавлена поддержка `qnap`, `asustor`, `truenas`.
   - Все NAS release-задачи проходят и создают ожидаемые артефакты.

## Артефакты (ключевые)

- **Android AAB:** `android/app/build/outputs/bundle/release/`
- **Android APK:** `android/app/build/outputs/apk/release/`
- **Desktop x86_64:** `platforms/client-desktop-x86_64/app/build/compose/binaries/main-release/msi/IP-CSS Desktop-1.0.0.msi`
- **Desktop ARM:** `platforms/client-desktop-arm/app/build/compose/binaries/`
- **NAS Synology:** `build/ip-css-Alfa-0.1.1-synology-*.spk`
- **NAS QNAP:** `build/ip-css-Alfa-0.1.1-qnap-*.qpkg`
- **NAS Asustor:** `build/ip-css-Alfa-0.1.1-asustor-*.apk`
- **TrueNAS:** `build/truenas-Alfa-0.1.1/`

## Заключение

**Локальная готовность релизного цикла подтверждена:** все целевые типы сборок, предусмотренные мастер-планом, находятся в состоянии **GREEN** на текущем окружении.

См. также:
- `docs/planning/RELEASE_GO_NO_GO_CHECKLIST.md`
- `docs/reports/RELEASE_GO_NO_GO_RECORD_2026-03-27.md`
- `IMPLEMENTATION_STATUS_REPORT.md`
- `docs/status/PROJECT_STATUS.md`
