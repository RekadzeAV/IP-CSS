# 🛡️ План устранения уязвимостей (Dependencies & Supply-Chain)

**Версия проекта:** 0.5.1.1-beta
**Дата актуализации:** 15.09.2026
**Статус:** 🟡 В работе — npm-контур закрыт (0 known); dependabot активен (**5 github-actions-PR закрыты вручную 15.09 — W1 (Этап V2)**; **W2-волна исполнена напрямую 16.09**; **dependabot.yml актуализирован 16.09** для github-actions с `allow`+`ignore`, чтобы не порождать новые PR). Gradle/pip/github-actions контуры — по плану разбора волн W2–W9
**Оперативный контекст:** `PLAN_COMPLETION_2026_Q4.md` (Фазы) · `PLAN_EXECUTION_MASTER.md` (Этап 6 — bump зависимостей) · единый трекер — `REMAINING_TASKS.md`

---

## 0. Инвентаризация от GitHub (15.09.2026)

**Источник:** Dependabot (`.github/dependabot.yml` — npm/gradle/pip/github-actions, weekly) + `npm audit`.
**Локальный `npm audit`:** `server/web` — **0 уязвимостей** ✅ · `server/web/dashboard` — **0 уязвимостей** ✅

Dependabot открыл **23 PR** (23 ветки `dependabot/*`). Часть — плановые обновления, часть — закрытие CVE. Классификация по контурам:

| Контур | PR | Тип | Приоритет |
|---|---|---|---|
| github-actions | **5 закрыто вручную 15.09** (W1 — Этап V2); **не порождать новые** via `dependabot.yml allow`/`ignore` (16.09) | PR-бэклог 15.09: PR#6 actions/setup-android 3→4, PR#7 actions/upload-artifact 3→7, PR#8 docker/build-push-action 5→7, PR#9 docker/login-action 3→4, PR#10 gitleaks/gitleaks-action 2→3 | 🟢 P2 (закрыто) | закрыто 15.09 вручную, ветки не существуют на remote (подтверждено через `git ls-remote github 'refs/heads/dependabot/github_actions/android-actions/setup-android-4'` и др.) | закрыто как superseded; github-actions больше не порождает новые PR (dependabot.yml clean) |
| gradle | 5 | **Мажорные** (ktor 2→3, koin 3→4, ktlint 12→14) | 🔴 P0/P1 |
| npm web/dashboard | 8 | Мажорные dev-инструменты (eslint 9→10, jest 29→30, typescript 7) | 🟡 P1 |
| pip ai-agent | 5 | Патч/минор (`>=` → новые минимумы) | 🟢 P2 |

---

## 1. Принципы

1. **Ноль известных high/critical в runtime-контурах** — обязательное условие GO по `PLAN_RELEASE.md`.
2. **Обновления без breaking changes — по умолчанию.** Major-обновления (например, Next.js 15 → 16) не делаются ради закрытия CVE, если есть semver-совместимая альтернатива (`overrides`, точечный bump транзитивной зависимости).
3. **Dev/build-time vs runtime различаются по приоритету.** Уязвимость в build-инструменте (lighthouse, puppeteer) не блокирует релиз, но устраняется в обычном порядке; уязвимость в runtime-зависимости сервера/клиента — P0.
4. **Каждый фикс — коммитом** с указанием advisory ID в сообщении; реестр (раздел 2) и этот документ обновляются тем же коммитом.
5. **После каждого массового обновления** — полный прогон тестов затронутого контура + production-сборка (см. раздел 6).

---

## 2. Реестр уязвимостей

> Формат: один ID — один пакет. Статусы: ⬜ выявлена → 🟡 в работе → ✅ устранена / ⏸️ осознанно отложена (с обоснованием).
> Новые уязвимости дописываются в конец таблицы при выявлении (см. процесс, раздел 5).

| ID | Дата | Контур | Пакет | Advisory | Severity | Статус |
|----|------|--------|-------|----------|----------|--------|
| VULN-2026-0914-01 | 14.09 | npm `server/web` | `postcss` 8.4.31 (заперт `next@15`) | GHSA-6g55-p6wh-862q, GHSA-r28c-9q8g-f849 (path traversal / file read через `sourceMappingURL`) + 2 moderate | 🔴 high | ✅ устранена 14.09: `overrides: ^8.5.23` (semver-совместимо), коммит `bcc8193b`; production-build зелёный |
| VULN-2026-0914-02 | 14.09 | npm `server/web` | `extract-zip` ≤2.0.1 (через `@puppeteer/browsers` → `lighthouse`) | GHSA-jmr9-qjv8-65gv, GHSA-7pqw-9j4j-h8q3 (symlink path traversal) | 🔴 high | ✅ устранена 14.09: `overrides: ^2.0.2` + `lighthouse` 12 → 13.4.1, коммит `bcc8193b` |
| VULN-2026-0914-03 | 14.09 | npm `server/web` | `lighthouse` ^12 (dev-инструмент) | цепочка через `@sentry/node`, `puppeteer-core` (диапазон ≤13.4.0) | 🔴 high (транзитивно) | ✅ устранена 14.09: `^13.4.1`, коммит `bcc8193b` |
| VULN-2026-0914-04 | 14.09 | npm `server/web` | `@sentry/node` 9.47.1 (через lighthouse) | цепочка через `@opentelemetry/core` <2.8.0 (GHSA-8988-4f7v-96qf, unbounded memory) | 🟠 moderate | ✅ устранена 14.09: обновление `lighthouse` до ^13.4.1 вывело связку из уязвимого диапазона, коммит `bcc8193b` |
| VULN-2026-0914-05 | 14.09 | npm `server/web` | прочие 13 moderate-транзитивностей (OTel-инструментация, puppeteer-core и др.) | см. `npm audit` lockfile до правки 14.09 | 🟠 moderate | ✅ устранены 14.09: совокупно `npm audit` 22 (5 high + 17 moderate) → **0**, коммит `bcc8193b` |
| VULN-2026-0914-06 | 14.09 | манифест | `server/web/package.json` — невалидный JSON (дубль ключа `scripts`) | не CVE, но блокировал `npm install/build/audit` и маскировал уязвимости | 🔴 blocker | ✅ устранено 14.09 (коммит `bcc8193b`) |
| VULN-2026-0914-07 | 14.09 | Android runtime | `core:security`: `java.util.Base64` (API 26+) при `minSdk 24` | не CVE, crash на Android 7.x (12 lint-ошибок NewApi) | 🔴 crash | ✅ устранено 14.09: `android.util.Base64`, коммит `026702c1` |
| VULN-2026-0914-08 | 14.09 | python `ai-agent` | открытые диапазоны `>=` в `requirements.txt` (без lock); текущий резолв `python-jose` 3.5.0 безопасен | воспроизводимость/аудит невозможны без lock-файла | 🟡 процесс | ⬜ P1 → этап V3 |
| VULN-2026-0914-09 | 14.09 | gradle | версионный каталог без автоматического CVE-скана (dependabot не настроен) | процесс | 🟡 процесс | 🟡 **15.09: dependabot настроен (`.github/dependabot.yml`, gradle включён)** → остаток: обновления по этапу V4 / Этапу 6 |

### Итог на 15.09.2026
- `npm server/web`: **22 (5 high + 17 moderate) → 0** (проверено: `package-lock.json` → `postcss 8.5.28`) · `npm server/web/dashboard`: 0 · `python-jose`: 3.5.0 (безопасно)
- **Dependabot активен** (`.github/dependabot.yml`: npm/gradle/pip/github-actions, weekly) — этап V2 закрыт.
- **CI-гейт `security-audit`** присутствует в `ci.yml` (`ci.yml:50`).
- Остаток: gradle-контур CVE (V4), pip-lock (V3), прогон CI с новым job.

---

## 3. Инструменты и источники обнаружения

| Инструмент | Контур | Статус |
|---|---|---|
| `npm audit` (локально + CI-гейт) | npm (`server/web`, `server/web/dashboard`) | CI-гейт high+ — этап V1 |
| GitHub Dependabot (`dependabot.yml`) | npm, gradle, pip, github-actions | ✅ настроен 14.09 (`.github/dependabot.yml`) |
| Trivy (container-scan.yml) | Docker-образ (OS + library) | ✅ есть (CRITICAL,HIGH) |
| CodeQL | JS/TS/Kotlin (стат. анализ) | ✅ есть |
| Gradle `--write-verification-metadata sha256` | целостность артефактов зависимостей | ✅ есть (dependency-verification.yml) |
| `scripts/ci/export-dependency-manifest.py` | offline-манифест каталога для CVE-сверки | ✅ есть (гейт `deps:validate` — этап V1) |
| pip-audit | python | этап V3 (после lock-файла) |

---

## 4. План устранения (этапы)

> Порядок — по принципу «слоёв» мастер-плана: сначала автоматизация обнаружения, затем закрытие контуров. Каждый этап фиксируется коммитом; вход в следующий — только при зелёном предыдущем.

### Этап V1 — CI-гейты npm (P1, программный) — 🟡 частично
- [x] Починен `server/web/package.json` (VULN-2026-0914-06).
- [x] `npm audit`: 22 → 0.
- [x] Добавлен job `security-audit` в `ci.yml`: npm audit runtime (`--omit=dev`, high+) + full (critical) + dashboard + `deps:validate`.
- [ ] Прогон CI после push, фиксация результата.

**Выход-гейт:** CI зелёный с новым job; красный при появлении high в runtime-зависимостях.

### Этап V2 — Dependabot (P1, программный) ✅ выполнено 14.09
- [x] `.github/dependabot.yml`: `npm` (`server/web`), `gradle`, `pip`, `github-actions`; weekly; PR-лимит 5–10; группировка minor/patch — **файл создан** (коммит `7104611f`).
- [ ] Политика разбора Dependabot-PR: 2–3 раза в неделю, приоритет high/critical; после каждого мёрджа — тесты затронутого контура.

**Выход-гейт:** dependabot активен ✅; первая PR-волна разобрана —  (процессная задача, не блокирует релиз).

### Этап V3 — Python-контур (P2)
- [ ] Зафиксировать `ai-agent/requirements.txt` через `pip-compile` (или `uv lock`) → lock-файл.
- [ ] Добавить pip-audit в `python-ci-gates.yml` (после lock).
- [ ] Прогнать pytest-гейт после фиксации версий.
- [ ] 5 открытых pip-PR Dependabot (langchain ≥1.4.0, pydantic ≥2.13.5, pytest ≥9.1.1, slowapi ≥0.1.10, uvicorn ≥0.52.4) закрывать **только после lock** — против lock-файла, иначе конфликты на каждом регенераторе.

### Этап V4 — Разбор Dependabot-волны 15.09 (23 PR → 18 открытых) — волнами, от дешёвых к дорогим
> Инвентаризация — раздел 0. Ветки Dependabot **сохранены** при чистке legacy-веток 15.09.
> PR размечены метками волн (`W2`…`W9`), контуров (`deps-gradle`/`deps-npm`/`deps-pip`) и приоритетов — см. таксонию меток: `docs/RULES_AND_AUTOMATION.md#labels`.
> Порядок строго W1→W9; после каждой волны — гейты затронутого контура (раздел 6); каждый мёрж — отдельный коммит.

| Волна | PR (контур) | Изменение | Риск | Гейт перед мёржем |
|---|---|---|---|---|
| W1 | ~~5 × github-actions~~ | ✅ **ЗАКРЫТА 15.09 (superseded прямым применением)**: бампы (upload-artifact→7 ×18, build-push→7, login→4, setup-android→4, gitleaks→3) применены в `main` в ходе актуализации CI/CD; PR #6–#10 закрыты с комментарием, ветки удалены GitHub-ом автоматически | 🟢 | YAML-валидация 13/13 OK + link-check 0 битых |
| W2 | gradle: foojay-resolver-convention 0.8.0→1.0.0, kotlinx-coroutines 1.10.1→1.10.2, bouncycastle 1.77→1.86, detekt 1.23.6→1.23.8 | 🟢 P2 | ✅ **исполнено напрямую 16.09** (`f70db0b6`, `0401cdc1`, `2b5d1432`) — PR#24 закрыт как superseded; kotlin/ktor/koin/ktlint осознанно не тронуты (отдельные волны); client-desktop-arm починен (компиляция восстановлена) | detekt `:server:api:detekt` + tests `:server:api:test` + компиляция `:platforms:client-desktop-arm:app:compileKotlin` | PR#24 supersede; группы KEEP kotlin/ignore majors отдельно; client-desktop-arm починен |

> **W2 статус (16.09):** безопасная часть исполнена напрямую в `main` (`49a53f4c`): coroutines 1.10.2, bouncycastle 1.86, detekt 1.23.8 + фикс `client-desktop-arm` (компиляция восстановлена). Kotlin/ktor/koin/ktlint осознанно не тронуты (отдельные волны). **PR#24 закрыт как superseded** (группа регенерирована бы; kotlin исключён из группы в `dependabot.yml`, чтобы миноры Kotlin не маскировались под minor-and-patch). Остаток волны: `foojay-resolver-convention 0.9.0→1.0.0` (PR#27 открыт, деп без CVE, отдельный коммит).
| W3 | gradle: ktlint 12.1.2→14.2.0 | build-плагин | 🟡 средний | ⏸️ **планируется** (не в W2) | `ktlintCheck` + detekt + пересборка | отдельная итерация Этапа 6; не смешивать с другими PR; до 0.6.0-beta опционально |
| W4 | gradle: koin 3.5.6→4.2.2 | мажор DI API | 🟡 средний | ⏸️ **планируется** (не в W2) | компиляция + тесты всех KMP-модулей | отдельная итерация Этапа 6; мажор DI API — проверка сигнатур expect/actual |
| W5 | gradle: ktor 2.3.13→3.5.2 | **мажор** server+client (netty/websockets/content-negotiation) | 🔴 высокий | ⏸️ **планируется** (после релиза 0.6.0-beta) | отдельная итерация Этапа 6, полный тест-матрикс; **не смешивать с другими PR** | мажор server+client — проверка обратной совместимости; допустимо после релиза 0.6.0-beta |
| W6 | npm: minor-and-patch (web + dashboard) + types/node 26.5.1 | патчи/миноры dev | 🟢 низкий | ⏸️ **планируется** | `npm ci` → `npm run build` → `npm test` | можно мёржить пакетно групповым PR |
| W7 | npm: eslint 10, jest 30, eslint-config-next 16 | мажоры dev-инструментов | 🟡 средний | ⏸️ **планируется** | по одному: lint + test + build | не смешивать |
| W8 | npm: typescript 7.0.2 | tsc на Go | 🔴 высокий | ⏸️ **послерелизная ревизия** (не в 0.6.0-beta) | `npm ci` → `npm run build` | tsc на Go — проверка совместимости; ⏸️ до послерелизной ревизии |
| W9 | pip 5 PR | новые минимумы `>=` | 🟡 средний | ⏸️ **планируется** (после V3 lock) | `pytest` (без `-m e2e`) + `pip-audit` | только после lock (V3) + pytest |

**Ориентир:** W1 закрыта 15.09; W2 исполнено напрямую 16.09; после W3–W4 и W6 — 0 known high в runtime-контурах; W5/W8 — плановые мажоры за пределами релизного окна 0.6.0-beta; W9 — только после V3 lock.
**Процесс:** если мёрж растянулся и Dependabot перегенерировал PR — ребейзить, не игнорировать; волны W1/W2/W6 можно мёржить пакетно групповым PR.

### Этап V5 — Периодический цикл (P2, процесс)
- [ ] Раз в месяц: `npm audit` + pip-audit + `export-dependency-manifest.py` + сверка с GitHub Advisory + разбор Dependabot-алертов (`gh api repos/:owner/:repo/dependabot/alerts` — требует `GITHUB_TOKEN`; анонимно → 401, проверено 15.09); результаты — в этот документ.
- [ ] Перед каждым релизом: полный обход раздела 3 + реестр без открытых high/critical — часть чеклиста GO/NO-GO (`PLAN_RELEASE.md`).
- [ ] Образы: покрыты Trivy в CI; дополнительно — скан финального релизного образа перед публикацией (`cd.yml`/`release.yml`).

---

## 5. Процесс при выявлении новой уязвимости

1. **Зафиксировать:** дописать строку в реестр (раздел 2) с новым ID (`VULN-<ГГГГ-ММДД>-<NN>`), advisory, severity, контуром.
2. **Оценить:** runtime или dev/build-time? эксплуатируемость в нашем контуре (достижима ли уязвимая точка из нашего кода/запроса)?
3. **Приоритет:** runtime high/critical — P0 (фикс немедленно, отдельным коммитом); runtime moderate — P1 (до следующего релиза); dev/build-time — P2 (обычный порядок); недостижимая в контуре — ⏸️ с обоснованием в реестре.
4. **Устранить:** semver-совместимый bump → `overrides` (npm) / прямое обновление (gradle/pip); при major — по правилу 2 раздела 1 (отдельная итерация с полным прогоном).
5. **Валидировать:** тесты затронутого контура + production-сборка (`npm run build` для web; см. раздел 6).
6. **Закрепить:** коммит (advisory ID в сообщении) + обновление реестра + запись в CHANGELOG; push.
7. **Если невозможно устранить сейчас:** пометить ⏸️ с обоснованием и планом; при high/critical в runtime — явный блокер в `REMAINING_TASKS.md`.

## 6. Чеклист валидации после обновления зависимостей

| Контур | Команды |
|---|---|
| npm web | `npm ci` → `npm audit` → `npm run build` → `npm test` (если затронут runtime-набор) |
| Gradle | `./gradlew :server:api:test :shared:desktopTest :core:network:desktopTest detekt ktlintCheck --no-daemon` |
| Python | `pytest` (без `-m e2e`) + pip-audit |
| Docker | локальный `docker build` + Trivy перед публикацией релиза |

## 7. История обработок

- **14.09.2026 (ревизия проекта):** выявлены 22 npm-уязвимости (5 high) + невалидный `package.json`; все устранены одним коммитом `bcc8193b` без breaking changes (см. реестр, раздел 2). Причина позднего обнаружения — отсутствие dependabot и npm-гейта в CI (устраняется этапами V1–V2).