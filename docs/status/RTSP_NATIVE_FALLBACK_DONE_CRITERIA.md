# RtspClient: критерии готовности Native/Fallback

Дата: 2026-03-27  
Область: `1.4.5 RtspClient`

## Цель

Явно разделить критерии готовности для двух путей выполнения:

- **Native path**: реальный нативный RTSP клиент.
- **Fallback path**: режим деградации при недоступной нативной библиотеке.

## Критерии Done: Native path

- `NativeRtspClient.create/connect/play/pause/stop/disconnect/destroy` работают без фатальных ошибок.
- `RtspClient` корректно получает статус из native callbacks и транслирует его в `StateFlow`.
- Видео/аудио кадры приходят через frame callbacks и доходят до `getVideoFrames()/getAudioFrames()`.
- Повторное подключение после `disconnect()` работает и не оставляет "висячих" native handle.
- Базовые интеграционные тесты RTSP запускаются в opt-in режиме (`ENABLE_RTSP_INTEGRATION_TESTS=true`).

## Критерии Done: Fallback path

- При недоступной native-библиотеке `RtspClient` не падает и переходит в fallback.
- `connect()/play()/pause()/stop()/disconnect()` сохраняют консистентный жизненный цикл статусов.
- Потоки кадров в fallback не блокируют поток выполнения и корректно останавливаются при `stop()/disconnect()/close()`.
- Поведение fallback явно документировано как деградированный режим (не production media pipeline).

## Критерии перехода в production-ready

- Native path подтверждён длительными реальными прогонами (soak) на целевых камерах.
- Подтверждена устойчивость по CPU/RAM и отсутствие утечек handle/job.
- Сформирована матрица совместимости (камера/кодек/аутентификация/результат).

## Текущий статус

- Native integration + fallback присутствуют.
- Opt-in интеграционные тесты для RTSP включены.
- Требуется закрыть длительный soak и формальную матрицу совместимости.
