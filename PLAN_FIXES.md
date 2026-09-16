# План исправлений (багов и рисков)

**Версия проекта:** 0.5.1.1-beta
**Дата актуализации:** 04.09.2026 (первичная: 08.08.2026)
**Статус:** большая часть выполнена; остатки перенесены в `PLAN_EXECUTION_MASTER.md` (Этапы 3, 4, 6)

---

## 🛡️ Безопасность

| № | Задача | Статус на 04.09.2026 |
|---|--------|----------------------|
| 1 | Плейсхолдер admin-пароля (`ServerUserRepositoryPostgres.kt:59`) | ✅ Выполнено 29.08: production блокируется без env-пароля; dev-only случайный пароль хешируется |
| 2 | Вывести `config/postgresql.env` из-под git | ✅ Выполнено 29.08: снят с трекинга; `.gitignore` защищает `config/*.env` (коммитятся только `*.example.env`) |
| 3 | Заполнить пустой `LICENSE` | ✅ Выполнено: GPLv3 + SPDX |
| 4 | Уязвимости dependabot (исходно 112: 61 high, 54 moderate, 5 low) | 🟡 **Актуализировано 15.09:** npm-контур закрыт (**22→0**, 5 high + 17 moderate, коммит `bcc8193b`); dependabot включён (`.github/dependabot.yml`), npm-гейт `security-audit` в `ci.yml` добавлен; Android-crash `java.util.Base64` (API 24–25) устранён (`026702c1`). **Остаток: gradle-контур + pip-lock** → этапы V3–V4 `PLAN_VULNERABILITIES.md` / **Этап 6** мастер-плана (итерации по 5–10 обновлений, 0 high как гейт) |

## 🛠️ Сборка

| № | Задача | Статус |
|---|--------|--------|
| 5 | D8 OutOfMemoryError при Android-сборке | ✅ Решено конфигурацией: `-Xmx12g`, `android.d8.maxHeapSize=12g`, `d8.maxWorkers=1`; APK ~33MB собирается стабильно |
| 6 | Проверить `android/gradle.properties` (D8 heap, workers) | ✅ Настроено (см. п.5) |

## 🧹 Мусор/дубликаты

| № | Задача | Статус |
|---|--------|--------|
| 7 | `.bak`, `--help`, `data/logs/server.*.tmp` | ✅ Удалены 29.08 (AppModule.kt.bak + bin-копия, корневой `--help`, tmp-логи) |
| 8 | Дубль Android-приложений | ✅ **Устранён (проверено 15.09):** `platforms/client-android/` содержит только `README.md` (1 файл в git); `:android:app` — единый источник, включён в `settings.gradle.kts`; CI android-джоба собирает `:android:app:assembleDebug` (`ci.yml:200`) |

## 🔗 Рассогласование статусов

| № | Задача | Статус |
|---|--------|--------|
| 9 | Синхронизировать README / WORK_PLAN / CHANGELOG / индексы с фактическим кодом | 🟡 Частично (04.09: аудит документации, архивация устаревших в `archive/docs-deprecated-2026-09-04/`, актуализация PLAN_*; WORK_PLAN архивирован). Остаток: линк-чек CI — **Этап 3** |

