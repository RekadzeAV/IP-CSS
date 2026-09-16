# Границы Фазы 1 (MVP): must-have и вне scope

**Версия проекта:** Alfa-0.1.1  
**Статус:** зафиксировано для планирования релиза и приёмки (см. [MVP_DOMAIN_CONTRACT_FREEZE.md](MVP_DOMAIN_CONTRACT_FREEZE.md), [RELEASE_GO_NO_GO_CHECKLIST.md](RELEASE_GO_NO_GO_CHECKLIST.md)).

## Must-have сценарии MVP (Фаза 1)

Конец-to-end контур, который должен быть воспроизводимо проверяем на целевом окружении (staging / полевой стенд):

1. **Discovery** — обнаружение камер (ONVIF / ручной ввод URL при необходимости).
2. **Connect** — проверка подключения, учётные данные, Digest при необходимости.
3. **Live playback** — просмотр живого потока (RTSP/HLS по поддерживаемому профилю клиента).
4. **Recording** — старт/стоп (и при необходимости pause/resume) согласно контракту `RecordingRepository`.
5. **Replay** — воспроизведение записи через поддерживаемый клиент (веб / desktop; Android — по дорожной карте 1.7).
6. **Events** — доставка/просмотр событий (ONVIF PullPoint / серверные события) согласно `EventRepository`.
7. **Auth** — JWT, RBAC на REST; для веб — согласованный httpOnly/refresh контур.

Заморозка доменных контрактов для интеграции: [MVP_DOMAIN_CONTRACT_FREEZE.md](MVP_DOMAIN_CONTRACT_FREEZE.md).

## Вне обязательного scope Фазы 1 (MVP)

| Элемент | Решение | Куда переносится |
|--------|---------|------------------|
| **Нативное iOS-приложение** (UIKit/SwiftUI клиент в `platforms`) | Не входит в критический путь MVP Фазы 1; не блокирует GO по веб + Android + Desktop + server | Фаза 2+ / отдельный релизный трек |
| **AI analytics use cases** (motion/objects/track) | Не MVP Фазы 1 | Фаза 2 ([PROJECT_STATUS_PHASES.md](../status/PROJECT_STATUS_PHASES.md) § 1.2.4) |
| **LDAP/SSO/Kerberos** для сервера | Не MVP | Фаза 4 |
| **Полный E2E UI** на всех платформах | Минимальный набор в 1.10; ручной runbook там, где нужно железо | [W4_MVP_PLATFORM_SMOKE_RUNBOOK.md](../reports/W4_MVP_PLATFORM_SMOKE_RUNBOOK.md) |

## Клиенты в scope MVP

- **Server** (Ktor): REST + WebSocket, production path PostgreSQL по [POSTGRESQL_STAGING_CUTOVER_AND_ROLLBACK_RUNBOOK.md](POSTGRESQL_STAGING_CUTOVER_AND_ROLLBACK_RUNBOOK.md).
- **Web** (Next.js): страницы камер / записей / событий / настроек; real-time; HLS-плеер.
- **Desktop** (Compose): live, записи, события, базовая стабильность long-run.
- **Android**: навигация и экраны; видео и фоновая запись — по плану 1.7/1.8 до приёмочного уровня.

## Синхронизация документов

При изменении границ MVP обновляйте:

- [docs/status/PROJECT_STATUS_PHASES.md](../status/PROJECT_STATUS_PHASES.md) (таблица Фазы 1).
- [docs/planning/PHASE1_MVP_TO_100_PLAN.md](PHASE1_MVP_TO_100_PLAN.md) при смене последовательности приоритетов.
- [docs/planning/RELEASE_GO_NO_GO_CHECKLIST.md](RELEASE_GO_NO_GO_CHECKLIST.md) при смене обязательных gate.
