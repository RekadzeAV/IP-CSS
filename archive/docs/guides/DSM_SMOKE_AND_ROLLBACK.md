# DSM Smoke and Rollback Runbook

Краткий runbook для ручной проверки пакета `ip-css` на Synology DSM после сборки `.spk`.

## Предусловия

- Пакет собран локально:
  - `build/ip-css-Alfa-0.1.1-synology-x86_64.spk` или
  - `build/ip-css-Alfa-0.1.1-synology-arm64.spk`
- На NAS включен SSH доступ для технической проверки.

## Smoke Checklist (install/start/health)

1. Установить пакет через DSM UI (*Package Center -> Manual Install*).
2. Проверить статус пакета:
   - `synopkg status ip-css`
3. Проверить процесс:
   - `ps aux | grep -E "ip-css|server\\.jar|java" | grep -v grep`
4. Проверить прослушиваемые порты:
   - `netstat -tulpen | grep -E "8080|8081"`
5. Проверить health endpoint:
   - `curl -fsS http://127.0.0.1:8081/api/v1/health`
6. Проверить наличие конфигурации и данных:
   - `ls -la /var/packages/ip-css/etc /var/packages/ip-css/var /var/packages/ip-css/target`
7. Проверить runtime-логи:
   - `tail -n 100 /var/packages/ip-css/var/logs/ip-css.log`

Ожидаемый результат:
- пакет в статусе `running`;
- `health` возвращает успешный ответ;
- в логах нет фатальных ошибок старта.

## Rollback/Reinstall Checklist

1. Остановить пакет:
   - `synopkg stop ip-css`
2. Проверить остановку:
   - `synopkg status ip-css`
3. Удалить пакет:
   - `synopkg uninstall ip-css`
4. Убедиться, что пакет удален:
   - `synopkg list | grep ip-css`
5. Переустановить `.spk`:
   - `synopkg install /volume1/path/ip-css-Alfa-0.1.1-synology-x86_64.spk`
6. Повторить базовый smoke:
   - `synopkg status ip-css`
   - `curl -fsS http://127.0.0.1:8081/api/v1/health`
   - `tail -n 100 /var/packages/ip-css/var/logs/ip-css.log`

## Опционально: полная очистка данных

Выполнять только если нужен "factory reset" перед повторной установкой:

- `rm -rf /var/packages/ip-css/var`
- `rm -rf /var/packages/ip-css/etc`

Внимание: это удалит БД, логи и записи.
