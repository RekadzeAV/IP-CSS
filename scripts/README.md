# Скрипты проекта IP-CSS

## Скрипты автоматизации

### `generate-project-structure.py`

Автоматически генерирует структуру проекта в файл `PROJECT_STRUCTURE_AUTO.md`.

**Использование:**
```bash
python3 scripts/generate-project-structure.py
```

**Что делает:**
- Анализирует структуру проекта
- Подсчитывает статистику (файлы, директории, модули)
- Генерирует дерево структуры
- Создает файл `PROJECT_STRUCTURE_AUTO.md`

**Автоматический запуск:**
- При каждом коммите (pre-commit hook)
- В CI/CD pipeline (GitHub Actions)

### `setup-git-hooks.sh`

Устанавливает git hooks для автоматической проверки структуры проекта.

**Использование:**
```bash
./scripts/setup-git-hooks.sh
```

**Что делает:**
- Устанавливает pre-commit hook
- Hook автоматически генерирует структуру перед коммитом
- Предлагает добавить обновленный файл в коммит

### `build-all-platforms.sh`

Собирает проект для всех платформ.

**Использование:**
```bash
./scripts/build-all-platforms.sh
```

### `install-ffmpeg.sh` / `install-ffmpeg.ps1`

Устанавливает FFmpeg для полной поддержки записи аудио.

**Использование:**
```bash
# Linux/macOS
./scripts/install-ffmpeg.sh

# Windows
.\scripts\install-ffmpeg.ps1
```

**Что делает:**
- Автоматически определяет ОС
- Устанавливает FFmpeg через менеджер пакетов (apt, yum, brew, choco, scoop)
- Проверяет наличие необходимых кодеков
- Настраивает PATH (Windows)
- Показывает информацию о доступных кодеках

**Поддерживаемые платформы:**
- Ubuntu/Debian
- RHEL/Fedora/CentOS
- Alpine Linux
- macOS (через Homebrew)
- Windows (через Chocolatey, Scoop или ручная установка)

**Документация:** [docs/FFMPEG_INSTALLATION.md](../docs/FFMPEG_INSTALLATION.md)

### `monitor-ffmpeg-performance.sh` / `monitor-ffmpeg-performance.ps1`

Мониторинг производительности FFmpeg и системы записи.

**Использование:**
```bash
# Linux/macOS
./scripts/monitor-ffmpeg-performance.sh [длительность_в_секундах] [файл_отчета]

# Windows
.\scripts\monitor-ffmpeg-performance.ps1 -Duration 60 -OutputFile "report.txt"
```

**Что делает:**
- Собирает информацию о системе (CPU, память)
- Проверяет наличие FFmpeg и его версию
- Определяет доступность hardware acceleration
- Мониторит использование CPU и памяти процессами FFmpeg
- Генерирует отчет с рекомендациями по оптимизации

**Примеры:**
```bash
# Мониторинг в течение 60 секунд
./scripts/monitor-ffmpeg-performance.sh 60

# Мониторинг в течение 5 минут с указанием файла отчета
./scripts/monitor-ffmpeg-performance.sh 300 performance-report.txt
```

### `create-platform-branches.sh` / `create-platform-branches.ps1`

Создает ветки для платформо-специфичной разработки.

**Использование:**
```bash
# Linux/macOS
./scripts/create-platform-branches.sh

# Windows
.\scripts\create-platform-branches.ps1
```

### `cleanup-old-branches.ps1`

Очищает старые ветки Git.

**Использование:**
```powershell
.\scripts\cleanup-old-branches.ps1
```

### `install-vscode-extensions.sh` / `install-vscode-extensions.ps1`

Устанавливает рекомендуемые расширения VS Code для проекта.

**Использование:**
```bash
# Linux/macOS
./scripts/install-vscode-extensions.sh

# Windows
.\scripts\install-vscode-extensions.ps1
```

## Требования

- **Python 3.7+** - для `generate-project-structure.py`
- **Bash** - для `.sh` скриптов
- **PowerShell** - для `.ps1` скриптов (Windows)

## Автоматизация

Все скрипты автоматически запускаются в CI/CD pipeline через GitHub Actions.

См. [docs/PROJECT_STRUCTURE_MANAGEMENT.md](../docs/PROJECT_STRUCTURE_MANAGEMENT.md) для подробной информации об управлении структурой проекта.

