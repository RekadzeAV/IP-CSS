# IP-CSS Desktop (ARM)

Клиент для платформ ARM64 (Linux, macOS Apple Silicon).

## Синхронизация с x86_64

Для полной функциональности (все экраны, видеоплеер, экспорт, временная шкала событий, системный трей и горячие клавиши) рекомендуется синхронизировать исходники с модулем **client-desktop-x86_64**:

- Скопировать недостающие экраны и компоненты из `platforms/client-desktop-x86_64/app/src/main/kotlin/` в этот модуль.
- Либо настроить общий исходный набор (shared source set) между `client-desktop-x86_64` и `client-desktop-arm` в Gradle.

Текущая реализация включает:
- Системный трей (иконка, меню «Показать» / «Выход»)
- Сворачивание в трей при закрытии окна (если трей доступен)
- Базовые экраны: камеры, прямой эфир, записи, события, настройки

## Сборка

```bash
./gradlew :platforms:client-desktop-arm:app:run
./gradlew :platforms:client-desktop-arm:app:packageDeb   # Linux
./gradlew :platforms:client-desktop-arm:app:packageDmg  # macOS
```
