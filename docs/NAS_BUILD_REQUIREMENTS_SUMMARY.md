# Краткая сводка требований для сборки NAS пакета

## Таблица приоритетов

| Приоритет | Компонент | Минимальная версия | Критичность | Для чего |
|-----------|-----------|-------------------|-------------|----------|
| **P0** | JDK | 17 | 🔴 Критично | Компиляция Kotlin/JVM |
| **P0** | Gradle | 8.0+ | 🔴 Критично | Система сборки |
| **P0** | Kotlin | 2.0.0 | 🔴 Критично | Компилятор Kotlin (через Gradle) |
| **P1** | CMake | 3.15+ | 🟠 Высокий | Сборка нативных библиотек |
| **P1** | FFmpeg | latest | 🟠 Высокий | RTSP, видео обработка |
| **P1** | Node.js | 20+ | 🟠 Высокий | Веб-интерфейс |
| **P1** | npm | 10+ | 🟠 Высокий | Веб-интерфейс |
| **P1** | C++ компилятор | C++17 | 🟠 Высокий | Нативные библиотеки |
| **P2** | OpenCV | 4.0+ | 🟡 Средний | Обработка изображений |
| **P2** | pkg-config | latest | 🟡 Средний | Поиск библиотек |
| **P3** | TensorFlow Lite | 2.16+ | 🟢 Низкий | AI аналитика |
| **P3** | CUDA/OpenCL | latest | 🟢 Низкий | GPU ускорение |
| **P3** | NAS SDK | varies | 🟢 Низкий | Создание пакетов |
| **P1** | PostgreSQL | 16+ | 🟠 Высокий | База данных (интеграционные тесты + production) |
| **P1** | Redis | 7+ | 🟠 Высокий | Кэш, сессии, репликация (интеграционные тесты + production) |
| **P2** | Docker | 24.0+ | 🟡 Средний | Контейнеризация и локальная разработка |
| **P2** | Python | 3.9+ | 🟡 Средний | AI-агент, скрипты валидации |

## Минимальный набор (базовая сборка)

```
✅ JDK 17
✅ Gradle (wrapper)
✅ Kotlin (автоматически)
```

**Команда:** `./gradlew :server:api:build`

## Полный набор (полная сборка)

```
✅ JDK 17
✅ Gradle (wrapper)
✅ CMake 3.15+
✅ FFmpeg + dev библиотеки
✅ C++ компилятор (GCC/Clang)
✅ Node.js 20+ и npm 10+
✅ pkg-config
⚠️ OpenCV 4.x (опционально)
```

## Быстрая установка

### Windows (PowerShell)
```powershell
choco install openjdk17 cmake ffmpeg nodejs-lts mingw -y
```

### Linux (Ubuntu/Debian)
```bash
sudo apt-get update && sudo apt-get install -y \
  openjdk-17-jdk cmake build-essential g++ \
  ffmpeg libavformat-dev libavcodec-dev libavutil-dev \
  libswscale-dev libswresample-dev pkg-config

curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt-get install -y nodejs
```

### macOS
```bash
brew install openjdk@17 cmake ffmpeg node@20 pkg-config
xcode-select --install
```

## Проверка установки

```bash
java -version      # Должно быть 17.x.x
cmake --version    # Должно быть ≥ 3.15
ffmpeg -version    # Проверка FFmpeg
node --version     # Должно быть ≥ 20.0.0
npm --version      # Должно быть ≥ 10.0.0
g++ --version      # Проверка C++ компилятора
```

## Следующие шаги

1. Установить зависимости согласно приоритетам
2. Проверить установку всех компонентов
3. Собрать базовые модули: `./gradlew buildAll`
4. Собрать нативные библиотеки: `cd native && mkdir build && cd build && cmake .. && cmake --build .`
5. Собрать веб-интерфейс: `cd server/web && npm install && npm run build`

**Подробная документация:**
- `LOCAL_DEVELOPMENT_REQUIREMENTS.md` (в корне проекта — полный справочник для Windows/Linux/macOS)
- `docs/NAS_LOCAL_BUILD_REQUIREMENTS.md` (устаревший, в архиве `docs/archive/docs-legacy-2026-04-27/`)


