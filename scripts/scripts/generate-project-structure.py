#!/usr/bin/env python3
"""
Скрипт для автоматической генерации структуры проекта IP-CSS.
Генерирует файл PROJECT_STRUCTURE_AUTO.md с актуальной структурой проекта.
"""

import os
import sys
from pathlib import Path
from datetime import datetime
from typing import List, Tuple, Dict
import json

# Игнорируемые директории и файлы
IGNORE_DIRS = {
    '.git', '.gradle', '.idea', 'build', 'node_modules', '.next', 
    'dist', 'out', '__pycache__', '.vscode', '.DS_Store', 'gradle',
    'wrapper', 'libs.versions.toml'
}

IGNORE_FILES = {
    '.gitignore', '.DS_Store', 'gradle-wrapper.jar', 'gradle-wrapper.properties',
    '*.class', '*.jar', '*.aar', '*.so', '*.dylib', '*.dll'
}

# Расширения файлов для подсчета
CODE_EXTENSIONS = {
    '.kt', '.java', '.cpp', '.c', '.h', '.hpp', '.ts', '.tsx', '.js', 
    '.jsx', '.py', '.xml', '.json', '.md', '.sq', '.gradle.kts', '.gradle',
    '.cmake', '.txt', '.yml', '.yaml', '.sh', '.ps1', '.bat'
}

# Важные файлы для отображения
IMPORTANT_FILES = {
    'build.gradle.kts', 'settings.gradle.kts', 'package.json', 'CMakeLists.txt',
    'README.md', 'Dockerfile', 'docker-compose.yml', 'tsconfig.json', 
    'next.config.js', 'gradle.properties', 'detekt.yml'
}

def should_ignore(path: Path) -> bool:
    """Проверяет, нужно ли игнорировать путь."""
    parts = path.parts
    # Игнорируем скрытые директории, кроме .github
    for part in parts:
        if part.startswith('.') and part != '.github':
            return True
    # Игнорируем служебные директории
    return any(ignore in parts for ignore in IGNORE_DIRS)

def get_file_stats(root: Path) -> Dict[str, int]:
    """Подсчитывает статистику файлов по типам."""
    stats = {}
    for ext in CODE_EXTENSIONS:
        stats[ext] = 0
    
    for file_path in root.rglob('*'):
        if file_path.is_file() and not should_ignore(file_path):
            ext = file_path.suffix.lower()
            if ext in CODE_EXTENSIONS:
                stats[ext] = stats.get(ext, 0) + 1
    
    return stats

def format_size(size: int) -> str:
    """Форматирует размер файла."""
    for unit in ['B', 'KB', 'MB', 'GB']:
        if size < 1024.0:
            return f"{size:.1f} {unit}"
        size /= 1024.0
    return f"{size:.1f} TB"

def count_files_in_dir(path: Path) -> Tuple[int, int]:
    """Подсчитывает количество файлов и директорий в директории."""
    files = 0
    dirs = 0
    try:
        for item in path.iterdir():
            if should_ignore(item):
                continue
            if item.is_file():
                files += 1
            elif item.is_dir():
                dirs += 1
                sub_files, sub_dirs = count_files_in_dir(item)
                files += sub_files
                dirs += sub_dirs
    except PermissionError:
        pass
    return files, dirs

def get_tree_structure(root: Path, prefix: str = "", max_depth: int = 5, current_depth: int = 0) -> List[str]:
    """Генерирует дерево структуры проекта."""
    if current_depth >= max_depth:
        return []
    
    lines = []
    items = []
    
    try:
        for item in sorted(root.iterdir()):
            if should_ignore(item):
                continue
            items.append(item)
    except PermissionError:
        return []
    
    for i, item in enumerate(items):
        is_last = i == len(items) - 1
        current_prefix = "└── " if is_last else "├── "
        next_prefix = prefix + ("    " if is_last else "│   ")
        
        if item.is_dir():
            name = item.name + "/"
            lines.append(prefix + current_prefix + name)
            # Рекурсивно добавляем содержимое директории
            sub_lines = get_tree_structure(item, next_prefix, max_depth, current_depth + 1)
            lines.extend(sub_lines)
        else:
            name = item.name
            # Показываем важные файлы или все файлы на первом уровне
            if current_depth < 2 or item.name in IMPORTANT_FILES or item.suffix in ['.kt', '.ts', '.tsx', '.cpp', '.h']:
                lines.append(prefix + current_prefix + name)
    
    return lines

def get_module_info(root: Path) -> Dict:
    """Собирает информацию о модулях проекта."""
    modules = {}
    
    # Gradle модули
    for gradle_file in root.rglob('build.gradle.kts'):
        if should_ignore(gradle_file):
            continue
        module_path = gradle_file.parent.relative_to(root)
        modules[str(module_path)] = {
            'type': 'gradle',
            'path': str(module_path),
            'build_file': str(gradle_file.relative_to(root))
        }
    
    # CMake модули
    for cmake_file in root.rglob('CMakeLists.txt'):
        if should_ignore(cmake_file):
            continue
        module_path = cmake_file.parent.relative_to(root)
        if str(module_path) not in modules:
            modules[str(module_path)] = {
                'type': 'cmake',
                'path': str(module_path),
                'build_file': str(cmake_file.relative_to(root))
            }
    
    # Node.js модули
    for package_json in root.rglob('package.json'):
        if should_ignore(package_json):
            continue
        module_path = package_json.parent.relative_to(root)
        if str(module_path) not in modules:
            modules[str(module_path)] = {
                'type': 'nodejs',
                'path': str(module_path),
                'build_file': str(package_json.relative_to(root))
            }
    
    return modules

def generate_structure_document(root: Path) -> str:
    """Генерирует полный документ структуры проекта."""
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    # Статистика
    total_files, total_dirs = count_files_in_dir(root)
    file_stats = get_file_stats(root)
    modules = get_module_info(root)
    
    # Генерация дерева
    tree_lines = get_tree_structure(root)
    
    doc = f"""# Структура проекта IP Camera Surveillance System (Автоматически сгенерировано)

> **⚠️ ВНИМАНИЕ:** Этот файл автоматически генерируется скриптом `scripts/generate-project-structure.py`
> 
> **Не редактируйте этот файл вручную!** Все изменения будут перезаписаны при следующей генерации.
> 
> Для изменения структуры проекта редактируйте файлы проекта, а затем запустите скрипт генерации.

**Дата генерации:** {timestamp}

## Статистика проекта

- **Всего файлов:** {total_files}
- **Всего директорий:** {total_dirs}
- **Модулей:** {len(modules)}

### Статистика по типам файлов

"""
    
    # Добавляем статистику по типам файлов
    for ext, count in sorted(file_stats.items(), key=lambda x: x[1], reverse=True):
        if count > 0:
            doc += f"- `{ext}`: {count} файлов\n"
    
    doc += f"""
## Дерево структуры проекта

```
IP-CSS/
"""
    
    doc += "\n".join(tree_lines)
    doc += "\n```\n"
    
    doc += """
## Модули проекта

### Gradle модули (Kotlin Multiplatform / Android)

"""
    
    gradle_modules = {k: v for k, v in modules.items() if v['type'] == 'gradle'}
    for module_path, info in sorted(gradle_modules.items()):
        doc += f"- **`{module_path}`**\n"
        doc += f"  - Build file: `{info['build_file']}`\n"
    
    doc += """
### CMake модули (C++ нативные библиотеки)

"""
    
    cmake_modules = {k: v for k, v in modules.items() if v['type'] == 'cmake'}
    for module_path, info in sorted(cmake_modules.items()):
        doc += f"- **`{module_path}`**\n"
        doc += f"  - Build file: `{info['build_file']}`\n"
    
    doc += """
### Node.js модули

"""
    
    node_modules = {k: v for k, v in modules.items() if v['type'] == 'nodejs'}
    for module_path, info in sorted(node_modules.items()):
        doc += f"- **`{module_path}`**\n"
        doc += f"  - Build file: `{info['build_file']}`\n"
    
    doc += """
## Описание основных директорий

### `core/` - Кроссплатформенные модули
- `core/common/` - Общие типы и утилиты
- `core/license/` - Система лицензирования
- `core/network/` - Сетевое взаимодействие (Ktor, ONVIF, RTSP, WebSocket)

### `shared/` - Kotlin Multiplatform модуль
Основной модуль с кроссплатформенной бизнес-логикой:
- `commonMain/` - Общий код для всех платформ
- `androidMain/` - Android-специфичные реализации
- `iosMain/` - iOS-специфичные реализации
- `desktopMain/` - Desktop-специфичные реализации
- `commonTest/` - Тесты

### `native/` - C++ нативные библиотеки
- `video-processing/` - Обработка видеопотоков
- `analytics/` - AI-аналитика (детекция объектов, лиц, движения, ANPR)
- `codecs/` - Поддержка кодеков (H.264, H.265, MJPEG)

### `server/` - Серверная часть
- `server/api/` - REST API сервер (Ktor)
- `server/web/` - Веб-интерфейс (Next.js)

### `android/` - Android приложение
- `android/app/` - Android app модуль

### `platforms/` - Платформо-специфичные реализации
- `sbc-arm/` - Микрокомпьютеры ARM
- `server-x86_64/` - Серверы x86-x64
- `nas-arm/` - NAS ARM
- `nas-x86_64/` - NAS x86-x64
- `client-desktop-x86_64/` - Клиенты Desktop x86-x64
- `client-desktop-arm/` - Клиенты Desktop ARM
- `client-android/` - Клиенты Android
- `client-ios/` - Клиенты iOS/macOS

### `docs/` - Документация
Вся документация проекта

### `scripts/` - Скрипты
Скрипты для сборки, развертывания и автоматизации

## Зависимости между модулями

```
android/ios/desktop приложения (platforms/)
    ↓
shared (KMM)
    ↓     ↓
    ↓  core:network
    ↓     ↓
    ↓  core:common (базовые типы: Resolution, CameraStatus)
    ↓
core:license
    ↓
native (C++ библиотеки через FFI, не Gradle модули)
```

## Как обновить структуру

Структура автоматически обновляется:
1. При каждом коммите через GitHub Actions (CI)
2. Вручную: запустите `./scripts/generate-project-structure.py`

Для локального запуска:
```bash
python3 scripts/generate-project-structure.py
```

## Примечания

- Игнорируются: `.git`, `.gradle`, `build`, `node_modules`, `.next`, и другие служебные директории
- Показываются только важные файлы и файлы с кодом
- Максимальная глубина дерева: 5 уровней
"""
    
    return doc

def main():
    """Главная функция."""
    # Определяем корневую директорию проекта
    script_dir = Path(__file__).parent
    root_dir = script_dir.parent
    
    # Генерируем структуру
    print(f"Генерация структуры проекта из {root_dir}...")
    structure_doc = generate_structure_document(root_dir)
    
    # Сохраняем в файл
    output_file = root_dir / "PROJECT_STRUCTURE_AUTO.md"
    output_file.write_text(structure_doc, encoding='utf-8')
    
    print(f"✓ Структура проекта сохранена в {output_file}")
    print(f"✓ Файл содержит {len(structure_doc)} символов")
    
    return 0

if __name__ == "__main__":
    sys.exit(main())

