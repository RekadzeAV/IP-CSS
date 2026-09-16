# Зависимости и аудит уязвимостей (10 August 2026)

## Статус dependabot
- GitHub сообщает о **121 vulnerability** на `main` (61 high / 55 moderate / 5 low).
- `gh` CLI **не авторизован** в этом окружении → детальная выгрузка advisory через API недоступна.
- OWASP Dependency-Check требует загрузки NVD-базы и не является офлайн-дружелюбным.

## Инструмент аудита (добавлен)
`scripts/ci/export-dependency-manifest.py` — автономный (offline) экспорт манифеста
зависимостей из `gradle/libs.versions.toml` в `group:artifact:version`.

```bash
python scripts/ci/export-dependency-manifest.py --output docs/dependency-manifest-2026-08-10.csv
```

Сгенерированный манифест: `docs/dependency-manifest-2026-08-10.csv` — **38 уникальных координат**:
- Ktor 2.3.5 (14 артефактов)
- Kotlinx (coroutines 1.7.3, serialization 1.7.0, datetime 0.6.0)
- SQLDelight 2.0.0, kotlin-logging 3.0.5
- AndroidX (core-ktx 1.12.0, lifecycle 2.6.2, security-crypto 1.1.0-alpha06, work 2.9.0)
- **BouncyCastle bcprov-jdk15on 1.70** ⚠️
- OpenCV 4.8.0, TensorFlow Lite 2.14.0, Koin 3.5.3, MockK 1.13.8, Turbine 1.0.0

## Наиболее вероятные кандидаты из 121 advisory (по версиям)
1. **BouncyCastle `bcprov-jdk15on:1.70`** — старая линейка (современные — `bcprov-jdk18on`); известны CVE в BC < 1.78/1.70. Рекомендация: перейти на `bcprov-jdk18on:1.78+`.
2. **Ktor 2.3.5** — известные DoS/SSRF-уязвимости, исправленные в более поздних 2.3.x (позже 2.3.12). Рекомендация: обновить до последней 2.3.x / 3.x.
3. **TensorFlow Lite 2.14.0** и OpenCV 4.8.0 — периодические CVE; проверить при интеграции нативных .so/.dll.
4. **SQLDelight 2.0.0** / kotlinx-* — вторичные транзитивные зависимости; обновлять вместе с Ktor.

## Следующие шаги
1. Авторизовать `gh auth login` → выгрузить точный список 121 advisory и сверить с манифестом.
2. Обновить Ktor → ≥2.3.12 и BouncyCastle → jdk18on ≥1.78 (самые высокие).
3. Повторно собрать/протестировать core:network, server:api, android после обновления версий.
4. Включить `dependency-check` (или `osv-scanner`) локально с кэшом NVD для регулярного гейта.