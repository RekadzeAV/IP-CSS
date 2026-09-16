# Security MVP: полевая валидация (pinning, HTTPS, credentials, audit)

**Назначение:** закрыть хвосты 1.9 (см. [PROJECT_STATUS_PHASES.md](../status/PROJECT_STATUS_PHASES.md) § 1.9) на staging перед релизом. Автоматизируемая часть — скрипт репозитория; ручные шаги — там, где нужен реальный TLS/прокси.

## 1. Автоматизируемый gate

Из корня репозитория (Windows):

```powershell
.\scripts\security-field-staging-validation.ps1
```

Убедиться, что отчёты попали в `diagnostics/security/` (как в [RELEASE_GO_NO_GO_CHECKLIST.md](RELEASE_GO_NO_GO_CHECKLIST.md) § Config & Security Gate).

## 2. HTTPS и принудительный TLS

- Проверить `FORCE_HTTPS`, `USE_HTTPS`, `ALLOW_EXTERNAL_TLS_TERMINATION` на конфигурации с reverse proxy (см. [config/https-baseline.example.env](../../config/https-baseline.example.env)).
- Убедиться в отсутствии смешанного контента на веб-клиенте при staging URL.

## 3. Certificate pinning

- Android/Desktop: профиль пинов и документация — [config/README-CERTIFICATE-PINS.md](../../archive/config/certificate-pins/README-CERTIFICATE-PINS.md), [config/certificate-pins.production.example.json](../../config/certificate-pins.production.example.json).
- После смены сертификата на стенде обновить пины и повторить smoke клиента.

## 4. Шифрование учётных данных камер

- Подтвердить, что новые записи не хранят plaintext; при старте сервера смотреть логи миграции legacy plaintext (fail-closed политика в коде common security).
- Проверить restore из бэкапа на копии стенда.

## 5. Аудит

- При `AUDIT_PERSIST_ENABLED=true` и PostgreSQL — проверить запись критичных операций (см. [SECURITY_MVP_READINESS.md](../status/SECURITY_MVP_READINESS.md)).

## 6. Сводный чеклист релиза

[RELEASE_GO_NO_GO_CHECKLIST.md](RELEASE_GO_NO_GO_CHECKLIST.md) — разделы 4 и 6.
