# NAS Platform Smoke Runbook (3.1)

**Дата:** 26 April 2026  
**Версия:** Alfa-0.1.1  
**Область:** Synology SPK, QNAP QPKG, Asustor APK, TrueNAS CORE/SCALE

## 1) Цель

Подтвердить, что NAS-пакеты, собранные в CI/локально, корректно:

- устанавливаются;
- запускаются;
- переживают restart/reboot;
- обновляются;
- удаляются без критичных следов.

## 2) Входные артефакты

- `build/ip-css-Alfa-0.1.1-synology-*.spk`
- `build/ip-css-Alfa-0.1.1-qnap-*.qpkg`
- `build/ip-css-Alfa-0.1.1-asustor-*.apk`
- `build/truenas-Alfa-0.1.1/`
- `build/checksums/*.sha256`

## 3) Preflight

- Проверить checksum каждого артефакта.
- Проверить наличие Java/JRE по требованиям платформы.
- Проверить свободное место: минимум 2 GB.
- Проверить, что порты `8080` и `8081` свободны.

## 4) Smoke сценарии (одинаковая последовательность)

### S1 — Fresh install

- Установить пакет через нативный менеджер платформы.
- Ожидаемо: статус пакета `Running`.

### S2 — Basic health

- Открыть `http://<NAS-IP>:8080`.
- Проверить API `http://<NAS-IP>:8081/health`.
- Ожидаемо: HTTP 200.

### S3 — Restart

- Выполнить stop/start из менеджера пакетов.
- Ожидаемо: сервис поднимается без ручного вмешательства.

### S4 — Reboot persistence

- Перезагрузить NAS.
- Ожидаемо: приложение поднимается автоматически, health = 200.

### S5 — Upgrade

- Установить поверх более новую сборку (или ту же как idempotency check).
- Ожидаемо: конфиг/данные не потеряны.

### S6 — Uninstall

- Удалить пакет штатно.
- Ожидаемо: сервис остановлен, остаточных процессов нет.

## 5) Платформенные проверки

### Synology

- Проверить lifecycle скриптов: `preinst/postinst/preuninst/postuninst`.
- Проверить отображение карточки в Package Center.

### QNAP

- Проверить `service.sh` path в App Center.
- Проверить `init.sh`/`uninstall.sh` на корректную регистрацию/очистку.

### Asustor

- Проверить lifecycle `preinst/postinst/preuninst/postuninst`.
- Для `rtd1296` отдельно повторить S1-S6.

### TrueNAS CORE

- Проверить jail deploy по `core/setup-jail.sh`.
- Проверить health endpoint и restart jail.

### TrueNAS SCALE

- Проверить `docker-compose up -d`.
- Проверить `kubectl apply -f kubernetes/`.
- Проверить persistence volume после restart pod/container.

## 6) Шаблон отчёта (на платформу)

- Platform/Model:
- Version/Artifact:
- Scenario results: S1/S2/S3/S4/S5/S6 (PASS/FAIL)
- Logs path:
- Issues found:
- Go/No-Go:

Готовые шаблоны:

- `docs/reports/NAS_SMOKE_REPORT_TEMPLATE_SYNOLOGY_2026-04-26.md`
- `docs/reports/NAS_SMOKE_REPORT_TEMPLATE_QNAP_2026-04-26.md`
- `docs/reports/NAS_SMOKE_REPORT_TEMPLATE_ASUSTOR_2026-04-26.md`
- `docs/reports/NAS_SMOKE_REPORT_TEMPLATE_TRUENAS_2026-04-26.md`

## 7) Правило решения

- **GO:** все обязательные S1-S6 = PASS.
- **CONDITIONAL GO:** есть только некритичные замечания (документированы с workaround).
- **NO-GO:** любой FAIL по install/start/health/reboot persistence.

## 8) Сводные документы релиза

- Master index: `docs/reports/NAS_RELEASE_MASTER_INDEX_2026-04-26.md`
- GO/NO-GO агрегатор: `docs/reports/NAS_GO_NO_GO_AGGREGATOR_2026-04-26.md`
- Executive summary: `docs/reports/NAS_EXECUTIVE_SUMMARY_TEMPLATE_2026-04-26.md`
- Publish checklist: `docs/reports/NAS_RELEASE_PUBLISH_CHECKLIST_2026-04-26.md`
