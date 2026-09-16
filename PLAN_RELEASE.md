# План релиза

**Версия проекта:** 0.5.1.1-beta
**Дата:** 08 August 2026

---

## 1. Фиксация состояния
- Текущее состояние закоммичено и влито в `main` (commit `a3ce215` → merge `4bb836e`), затем отправлено в удалённый `github/main`.
- Все изменения вести через ветки + PR; релиз — только по тегу `v*`.

## 2. Гейт GO/NO-GO
- [ ] `./gradlew :server:api:build` — зелёный (unit + интеграционные тесты).
- [ ] KMP-гейты CI прошли.
- [ ] Trivy/secret-scan чист (0 high-критичных).
- [ ] Link-check документации (`docs-link-check-active-scope`).
- [ ] Открытые stub-блокеры закрыты ИЛИ явно помечены в release notes.
- [x] Kover `server:api` ≥30% — **пройдено** (LINE 30.1%; порог `minBound(30)` закреплён в `koverVerify`, 474 теста / 0 failures).

## 3. Настройка публикации
- GitHub Secrets: `DOCKER_USERNAME`, `DOCKER_PASSWORD`, `GH_TOKEN`.
- Registry: ghcr.io `rekadzeav/ip-css` (+ Docker Hub при необходимости).
- Проверить `.github/workflows/release.yml` (тип артефактов: JAR, APK, MSI, DEB, DMG, NAS-пакеты).

## 4. Артефакты и подпись
- Собрать/подписать все платформенные артефакты (см. `PLAN_TEST_BUILDS.md`).
- Release notes — из актуального `CHANGELOG.md`.

## 5. Шаги выпуска
1. Merge финальной ветки в `main`.
2. `git tag v0.5.1.1-beta && git push github v0.5.1.1-beta`
3. GitHub Release: собрать артефакты пайплайном, опубликовать.
4. Опубликовать Docker-образы.