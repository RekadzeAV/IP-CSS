# Установка и настройка IP-CSS на NAS

Краткое руководство по установке, настройке и типичным вопросам для платформ Synology, QNAP, Asustor и TrueNAS.

---

## Установка по платформам

### Synology (SPK)

1. Соберите или скачайте `ip-css-<version>-synology-<arch>.spk` (arch: x86_64 или arm64).
2. В **Package Center** → **Manual Install** выберите файл .spk.
3. Убедитесь, что установлена **Java 17+** (через Package Center или вручную).
4. После установки пакет появится в списке; запуск/остановка — через интерфейс пакета или по умолчанию при включении.

**Доступ:** Web UI — `http://<NAS-IP>:8080`, API — `http://<NAS-IP>:8081`.

### QNAP (QPKG)

1. Соберите или скачайте `ip-css-<version>-qnap-<arch>.qpkg` (x86_64, arm_64, arm_32).
2. В **App Center** → **Install from file** выберите .qpkg.
3. Установите **Java 17+** через App Center при необходимости.
4. Включите приложение в App Center; запуск/остановка — через кнопки приложения.

**Доступ:** Web UI — `http://<NAS-IP>:8080`, API — `http://<NAS-IP>:8081`.

### Asustor (APK)

1. Соберите или скачайте `ip-css-<version>-asustor-<arch>.apk` (x86_64, armv8, rtd1296).
2. В **App Central** → **Manual Install** загрузите .apk.
3. Установите **Java 11+** из App Central при необходимости.
4. Запустите приложение из App Central.

**Доступ:** Web UI — `http://<NAS-IP>:8080`, API — `http://<NAS-IP>:8081`.

### TrueNAS CORE (Jail)

1. Скопируйте содержимое сборки `truenas-<version>/core/` на хост TrueNAS.
2. Следуйте инструкциям в `core/README.md` и командам из `setup-jail.sh` (создание jail, монтирование данных, установка Java, запуск JAR).

### TrueNAS SCALE (Docker)

1. Скопируйте `docker-compose.yml` из сборки `truenas-<version>/` на хост.
2. Выполните: `docker-compose up -d`.
3. Образы должны быть собраны или доступны (company/ip-css-api, company/ip-css-web).

### TrueNAS SCALE (Kubernetes)

1. Примените манифесты: `kubectl apply -f kubernetes/`.
2. Настройте Ingress или NodePort при необходимости (по умолчанию в манифестах может быть NodePort 30080 для веб).

---

## Настройка

- **Конфиг:** после установки пакета конфиг создаётся в каталоге config пакета (например `config/config.yaml`). В нём задаются:
  - `server.port` / `server.host`
  - `database.path`
  - `storage.recordings_path`
  - `logging.level` и `logging.file`
- **Переменные окружения:** при необходимости можно задать `JWT_SECRET`, `REDIS_HOST`, `REDIS_PORT` в скриптах запуска или в systemd/unit.
- **Порты:** по умолчанию 8080 (веб) и 8081 (API). При изменении обновите конфиг и, при необходимости, настройки фаервола/прокси на NAS.

---

## FAQ

**Q: Пакет не запускается после установки.**  
A: Проверьте, что установлена Java 17 (или 11+ для Asustor). Проверьте логи (каталог logs пакета или вывод systemd/journal).

**Q: Где хранятся записи и база данных?**  
A: В каталогах данных пакета (data/recordings, data/db), путь задаётся в config.yaml. На Synology/QNAP/Asustor он обычно под каталогом пакета или в volume (см. скрипты start.sh и detect-nas-paths).

**Q: Как ограничить потребление памяти на слабом NAS?**  
A: В скриптах start.sh заданы `-Xmx512m -Xms256m`. Можно уменьшить, отредактировав соответствующий start.sh в установленном пакете или в исходниках перед сборкой.

**Q: Нужен ли отдельный веб-интерфейс?**  
A: Если в пакет не включён каталог web/dist (Next.js), будет работать только API. Для полного UI соберите веб и включите его в пакет при сборке.

**Q: Поддержка NAS-специфичных API (DSM, QNAP, ADM)?**  
A: Сейчас пакеты работают автономно (сервис + конфиг). Интеграция с API Synology/QNAP/Asustor опциональна и может быть добавлена для уведомлений или единого центра управления.

---

## См. также

- Сборка пакетов: `platforms/nas-x86_64/packages/README_NAS_BUILD.md`
- Fat JAR для NAS: раздел «Fat JAR для NAS» в том же файле.
- Smoke runbook и go/no-go: `docs/reports/NAS_PLATFORM_SMOKE_RUNBOOK_2026-04-26.md`
- Локальная smoke-проверка (Windows): `powershell -ExecutionPolicy Bypass -File scripts/test-nas-build.ps1 -PackageType synology -Arch x86_64`
