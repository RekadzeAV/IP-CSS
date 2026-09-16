# Android RTSP в MVP — использование NativeRtspClient и ExoPlayer

## Итог проверки (задача 3.2.1)

### Воспроизведение на Android

- **Для воспроизведения используется ExoPlayer (Media3), а не NativeRtspClient.**
- Экран просмотра: `VideoViewScreen` → `ExoVideoPlayer` с URL из API (`StreamApiService.getRtspUrl`).
- ExoPlayer получает RTSP URL и воспроизводит поток напрямую (поддержка RTSP встроена в Media3/ExoPlayer).
- Декодирование выполняет ExoPlayer/системные кодеки; отдельный нативный декодер для воспроизведения не используется.

### NativeRtspClient на Android

- `NativeRtspClient` (Android) в `core/network/.../NativeRtspClient.android.kt` объявляет JNI-методы и загружает библиотеку `video_processing`.
- В проекте **нет сборки нативной библиотеки `video_processing` под Android** (есть только cinterop для Kotlin/Native в `nativeInterop`).
- На Android `NativeRtspClient` **нигде не используется для воспроизведения**: плеер — только ExoPlayer.
- `RtspClient` (общий слой поверх NativeRtspClient) используется на **сервере** (аналитика/кадры) и в **десктопном** приложении, но не в Android-клиенте.

**Вывод:** для MVP нативного RTSP на Android достаточно текущей схемы: **ExoPlayer + RTSP URL**. Доводка или переключение на ExoPlayer не требуются — воспроизведение уже идёт через ExoPlayer. NativeRtspClient на Android опционален (например, для будущей аналитики по кадрам) и сейчас не обязателен для просмотра.

---

## Рекомендации

1. **Оставить воспроизведение через ExoPlayer** — без изменений архитектуры.
2. **Fallback на HLS:** при ошибках RTSP (сеть, парсинг, декодер) ExoVideoPlayer переключается на HLS. Передавать в плеер реальный `hlsUrl` из API (`getStreamStatus`), а не собирать URL по шаблону с `localhost` (см. правки в `ExoVideoPlayer` и `VideoViewScreen`).
3. **NativeRtspClient на Android:** если позже понадобятся кадры/аналитика на устройстве — потребуется либо сборка `video_processing` под Android (JNI), либо отдельная легковесная реализация. Для MVP можно не подключать.

---

## Ограничения и кодеки (задача 3.2.2)

- **Видео:** ExoPlayer/Media3 на Android обычно поддерживает H.264/AVC; H.265/HEVC зависит от устройства и версии ОС.
- **Аудио:** типично AAC и другие кодеки из MediaCodec.
- Ограничения по профилям/уровням определяются драйверами и производителем устройства; при проблемах с воспроизведением — проверять логи ExoPlayer и при необходимости переключать на HLS (сервер перекодирует через FFmpeg).

Документацию по кодекам/профилям при необходимости можно вынести в отдельный раздел в README или в конфиг тестовых камер.
