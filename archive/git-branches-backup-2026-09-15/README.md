# Архив удалённых git-веток GitHub — 15.09.2026

**Событие:** по решению владельца из GitHub-репозитория `RekadzeAV/IP-CSS` удалены 30 устаревших веток (`develop/*`, `test/*`, `dev/*`, `feature/*`, `refactor/*`, `chore/*`, `main-old`, `old_prodject_alfa_0.0.1_defeat`, `commit`). Активные Dependabot-ветки (23) и `main` не затронуты.

**Полный бэкап:** `old-branches.bundle` — git-bundle с 34 refs (32 архивных tip + `main` + `HEAD`), полная история каждой удалённой ветки. Локально ссылки дополнительно закреплены в `refs/archive/branches-2026-09-15/*` (защита от GC).

> ⚠️ **Бандл (61,3 MB) хранится только локально** — исключён из git через `.gitignore` (`archive/**/*.bundle`), т.к. превышает рекомендации GitHub (50 MB). Для удалённого хранения скопируйте его на NAS/внешний диск.

## Как проверить
```bash
git bundle verify  archive/git-branches-backup-2026-09-15/old-branches.bundle
git bundle list-heads archive/git-branches-backup-2026-09-15/old-branches.bundle
```

## Как восстановить
```bash
git clone archive/git-branches-backup-2026-09-15/old-branches.bundle restored
# или в существующий репозиторий:
git fetch archive/git-branches-backup-2026-09-15/old-branches.bundle \
  'refs/archive/branches-2026-09-15/*:refs/heads/restored/*'
```

## Почему удалены
- Последние коммиты в них — декабрь 2025 … март 2026; содержимое давно слито в `main` или устарело.
- Единственная актуальная ветка — `main` (точка истины, `github/main`).
- Уменьшение шума в списке веток и в GitHub Actions (запуски от старых веток).

## Особые точки истории

| Коммит | Ветка | Дата | Примечание |
|---|---|---|---|
| `1c528561` | refactor/structural-cleanup | 2026-07-04 | **слит в main** — hardware acceleration + behavioral analysis |
| `03d5e244` | chore/kmp-phase1-closure-reporting | — | **слит в main** |
| `80f39512` | test/integration | 2025-12-27 | WSDiscovery, server API (уникальная история — только в бандле) |
| `e8dc0798` | feature/ui-components | 2025-12-27 | WIP UI components (только в бандле) |
| `3367626a` | commit | 2025-12-27 | Update project files (только в бандле) |
| `ce98b282` | main-old | 2026-03-16 | старая линия main (только в бандле) |
| `187cad53` | old_prodject_alfa_0.0.1_defeat | 2026-03-03 | старый проект alfa (только в бандле) |

> Уникальные (неслитые) tips: 30 веток. Подробный реестр tip-ов — `ARCHIVE_MANIFEST.md`, запись от 15.09.2026.