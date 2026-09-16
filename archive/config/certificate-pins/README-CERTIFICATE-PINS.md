# Certificate Pinning — конфигурация

Конфигурация pinning загружается **во всех клиентах** (Android, iOS, Desktop/JVM) из файла или переменных окружения.

## Файлы

- **`certificate-pins.example.json`** — пример формата (можно коммитить). Скопируйте в `certificate-pins.json` и подставьте свои домены и SHA-256 fingerprints.
- **`certificate-pins.production.example.json`** — production baseline (включен strict enforce и backup pins).
- **`certificate-pins.json`** — рабочий конфиг (по умолчанию не коммитить; путь задаётся через `CERTIFICATE_PINS_FILE` или используется путь по умолчанию).
- **`https-baseline.example.env`** — минимальный HTTPS/TLS baseline для production окружений.

## Путь по умолчанию

- **Общий путь в коде:** `config/certificate-pins.json` ([CertificatePinningManager.DEFAULT_CONFIG_PATH](../core/network/src/commonMain/kotlin/com/company/ipcamera/core/network/security/CertificatePinningManager.kt)).
- **JVM/Desktop:** файл ищется относительно рабочей директории (например корень проекта).
- **Android:** сначала `assets/config/certificate-pins.json` или `assets/certificate-pins.json`, затем `filesDir`. Перед первым вызовом `loadConfig()` нужно вызвать `CertificatePinningConfigLoader.init(context)` (в Android-приложении это делается в DI при старте).
- **iOS:** файл должен быть добавлен в target (Copy Bundle Resources); поиск по пути внутри main bundle (в т.ч. подпапка `config`).

## Формат JSON

Поддерживаются оба варианта имён полей:

| Пример (example) | Альтернатива |
|------------------|-------------|
| `enabled`        | `enablePinning` |
| `enforce`        | `enforcePinning` |
| `certificates`   | `hosts`         |

- **enabled** / **enablePinning** — включить pinning.
- **enforce** / **enforcePinning** — при `true` соединение отклоняется при несовпадении сертификата.
- **certificates** / **hosts** — объект: ключ = хост (например `api.example.com`), значение = массив pin в формате `sha256/Base64...`.
- Для production рекомендуется минимум 2 pins на хост (active + backup).

## Переменные окружения

- `CERTIFICATE_PINS_FILE` — путь к JSON-файлу (перекрывает путь по умолчанию).
- `CERTIFICATE_PINNING_ENABLED` — `true`/`false`.
- `CERTIFICATE_PINNING_ENFORCE` — `true`/`false`.
- `CERTIFICATE_PINS_<host>` — список pin через запятую (например `CERTIFICATE_PINS_api_example_com=sha256/xxx,sha256/yyy`). На iOS полный список env недоступен — используется только загрузка из файла.

## Использование в коде

- **ApiClient:** передать `certificatePinningConfig` в `ApiClientConfig` (например из `CertificatePinningManager.loadConfig()`).
- **OnvifClient:** `OnvifClientFactory.createWithAutoConfig()` — загружает конфиг из пути по умолчанию или из переданного пути.

Подробнее: [CERTIFICATE_PINNING.md](../core/network/CERTIFICATE_PINNING.md).
