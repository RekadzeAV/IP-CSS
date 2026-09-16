# ONVIF Events: проверка для MVP (профиль камер)

**Связь:** [MVP_PHASE1_AUTOMATED_ACCEPTANCE.md](MVP_PHASE1_AUTOMATED_ACCEPTANCE.md), [VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md](../reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md), [docs/automation/ONVIF_FOLLOWUP_BACKLOG.md](ONVIF_FOLLOWUP_BACKLOG.md).

## Без реальной камеры

- Юнит и сервисные тесты `OnvifEvent*` в `core/network` (см. `./gradlew :core:network:test`).
- JVM lifecycle: `CameraEventMonitoringServiceJvmTest` (если присутствует в дереве).

## С камерой и конфигом

1. Подготовить `config/onvif-events.example.env` (копия в `.local` или секреты в CI).
2. Запуск:

```powershell
.\scripts\onvif-events-api-verification.ps1
```

3. Зафиксировать артефакт в `diagnostics/onvif-events/` для сводки GO/NO-GO (`generate-phase1-go-no-go-summary.ps1`).

## Профиль совместимости

Матрица камер и strict/profile-aware решения по видео: [VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md](../reports/VIDEO_E2E_ACCEPTANCE_PROFILE_RUNBOOK.md) и `scripts/video-e2e-go-no-go.ps1`. События ONVIF могут иметь **CONDITIONAL** acceptance при частичной совместимости профиля — это ожидаемо до расширения профиля или списка устройств.
