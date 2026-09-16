# KMP Phase 1 DoD Checklist

Этот чеклист определяет критерии 100% завершения фазы "Архитектурная стабилизация KMP платформ".

## 1) Expect/actual согласованность

- [x] Все security/encryption `expect` в `commonMain` имеют `actual` в Android.
- [x] Все security/encryption `expect` в `commonMain` имеют `actual` в JVM/Desktop.
- [x] Все security/encryption `expect` в `commonMain` имеют `actual` в iOS.
- [x] Все security/encryption `expect` в `commonMain` имеют `actual` в Native.
- [x] Сигнатуры `expect/actual` совпадают по параметрам, типам возврата, nullability и `suspend`.

## 2) Границы commonMain

- [x] В `commonMain` нет импортов `java.*`.
- [x] В `commonMain` нет импортов `javax.*`.
- [x] В `commonMain` нет импортов `android.*`.
- [x] В `commonMain` нет fully-qualified использования `java.*`/`javax.*`/`android.*`.

## 3) Source sets и зависимости

- [x] Проверки `checkKotlinGradlePluginConfigurationErrors` проходят для `core/common`.
- [x] Проверки `checkKotlinGradlePluginConfigurationErrors` проходят для `core/network`.
- [x] Проверки `checkKotlinGradlePluginConfigurationErrors` проходят для `shared`.
- [x] В `iosMain/nativeMain` нет JVM/Android-only зависимостей.

## 4) CI-gates

- [x] В CI есть fail-fast проверка на запрещённые platform API в `commonMain`.
- [x] В CI есть fail-fast проверка сигнатур security `expect/actual`.
- [x] В CI есть KMP compile smoke для common-safe target matrix.
- [x] PR не может быть merged при падении любой из перечисленных KMP-проверок.

### Команда локальной приемки (рекомендуется)

- Cross-platform: `python scripts/ci/verify-kmp-phase1.py`
- Windows PowerShell: `.\scripts\ci\verify-kmp-phase1.ps1`
- Linux/macOS/WSL: `./scripts/ci/verify-kmp-phase1.sh`

## 5) Документация

- [x] `CONTRIBUTING.md` содержит правила по KMP-границам и `expect/actual`.
- [x] Прогресс фазы отражён в `docs/kmp-phase1-progress.md`.
- [x] Чеклист DoD актуален и используется в приемке.

## Статус закрытия фазы

- [x] Фаза 1 официально закрыта по DoD (локальная и CI-приемка пройдены).
- [x] CI публикует machine-readable и markdown отчёты в artifact `kmp-phase1-verify-report`.
