#!/usr/bin/env python3
"""
Скрипт для поиска дубликатов файлов и неиспользуемых файлов в проекте
"""
import os
import hashlib
from pathlib import Path
from collections import defaultdict
import json

def get_file_hash(filepath):
    """Вычисляет MD5 хэш файла"""
    hash_md5 = hashlib.md5()
    try:
        with open(filepath, "rb") as f:
            for chunk in iter(lambda: f.read(4096), b""):
                hash_md5.update(chunk)
        return hash_md5.hexdigest()
    except Exception as e:
        return None

def find_duplicates_by_name(root_dir):
    """Находит файлы с одинаковыми именами"""
    files_by_name = defaultdict(list)

    ignore_dirs = {'.git', '.gradle', 'build', 'node_modules', '.next', '__pycache__', '.idea'}

    for root, dirs, files in os.walk(root_dir):
        # Фильтруем игнорируемые директории
        dirs[:] = [d for d in dirs if d not in ignore_dirs]

        for file in files:
            filepath = os.path.join(root, file)
            rel_path = os.path.relpath(filepath, root_dir)
            files_by_name[file].append(rel_path)

    # Возвращаем только файлы с дубликатами
    duplicates = {name: paths for name, paths in files_by_name.items() if len(paths) > 1}
    return duplicates

def find_duplicates_by_content(root_dir):
    """Находит файлы с одинаковым содержимым"""
    files_by_hash = defaultdict(list)

    ignore_dirs = {'.git', '.gradle', 'build', 'node_modules', '.next', '__pycache__', '.idea'}
    ignore_extensions = {'.jar', '.class', '.so', '.dll', '.dylib', '.a', '.o'}

    for root, dirs, files in os.walk(root_dir):
        dirs[:] = [d for d in dirs if d not in ignore_dirs]

        for file in files:
            filepath = os.path.join(root, file)
            _, ext = os.path.splitext(file)

            if ext.lower() in ignore_extensions:
                continue

            file_hash = get_file_hash(filepath)
            if file_hash:
                rel_path = os.path.relpath(filepath, root_dir)
                files_by_hash[file_hash].append(rel_path)

    # Возвращаем только группы с дубликатами
    duplicates = {hash_val: paths for hash_val, paths in files_by_hash.items() if len(paths) > 1}
    return duplicates

def find_backup_files(root_dir):
    """Находит резервные копии и временные файлы"""
    backup_patterns = ['.bak', '.backup', '.old', '.tmp', '.temp', '_old', '_backup', '_bak']
    backup_files = []

    ignore_dirs = {'.git', '.gradle', 'build', 'node_modules', '.next', '__pycache__', '.idea'}

    for root, dirs, files in os.walk(root_dir):
        dirs[:] = [d for d in dirs if d not in ignore_dirs]

        for file in files:
            filepath = os.path.join(root, file)
            rel_path = os.path.relpath(filepath, root_dir)

            # Проверяем паттерны резервных копий
            for pattern in backup_patterns:
                if pattern in file.lower():
                    backup_files.append(rel_path)
                    break

    return backup_files

def main():
    root_dir = Path(__file__).parent.parent
    os.chdir(root_dir)

    print("=" * 80)
    print("АНАЛИЗ ПРОЕКТА НА ДУБЛИКАТЫ И МУСОРНЫЕ ФАЙЛЫ")
    print("=" * 80)
    print()

    # 1. Дубликаты по имени
    print("1. ПОИСК ДУБЛИКАТОВ ПО ИМЕНИ ФАЙЛА...")
    print("-" * 80)
    duplicates_by_name = find_duplicates_by_name(str(root_dir))

    # Фильтруем только интересные дубликаты (исключаем стандартные файлы типа README.md, .gitignore и т.д.)
    interesting_duplicates = {}
    for name, paths in duplicates_by_name.items():
        # Пропускаем стандартные файлы, которые могут быть в разных местах
        if name in ['README.md', '.gitignore', '.gitattributes', 'LICENSE', 'CHANGELOG.md']:
            continue
        # Пропускаем файлы, которые должны быть в разных местах (например, build.gradle.kts)
        if name in ['build.gradle.kts', 'settings.gradle.kts', 'gradle.properties']:
            continue
        interesting_duplicates[name] = paths

    if interesting_duplicates:
        print(f"Найдено {len(interesting_duplicates)} файлов с дублирующимися именами:\n")
        for name, paths in sorted(interesting_duplicates.items()):
            print(f"  {name}:")
            for path in paths:
                print(f"    - {path}")
            print()
    else:
        print("  Дубликатов по имени не найдено (кроме стандартных файлов)\n")

    # 2. Дубликаты по содержимому
    print("\n2. ПОИСК ДУБЛИКАТОВ ПО СОДЕРЖИМОМУ...")
    print("-" * 80)
    print("  Это может занять некоторое время...")
    duplicates_by_content = find_duplicates_by_content(str(root_dir))

    if duplicates_by_content:
        print(f"\nНайдено {len(duplicates_by_content)} групп файлов с одинаковым содержимым:\n")
        for hash_val, paths in list(duplicates_by_content.items())[:20]:  # Показываем первые 20
            print(f"  Группа (hash: {hash_val[:8]}...):")
            for path in paths:
                print(f"    - {path}")
            print()
        if len(duplicates_by_content) > 20:
            print(f"  ... и еще {len(duplicates_by_content) - 20} групп\n")
    else:
        print("  Дубликатов по содержимому не найдено\n")

    # 3. Резервные копии
    print("\n3. ПОИСК РЕЗЕРВНЫХ КОПИЙ И ВРЕМЕННЫХ ФАЙЛОВ...")
    print("-" * 80)
    backup_files = find_backup_files(str(root_dir))

    if backup_files:
        print(f"Найдено {len(backup_files)} резервных копий:\n")
        for file in backup_files:
            print(f"  - {file}")
        print()
    else:
        print("  Резервных копий не найдено\n")

    # 4. Специальные проверки
    print("\n4. СПЕЦИАЛЬНЫЕ ПРОВЕРКИ...")
    print("-" * 80)

    # Проверка android/android vs android/app
    android_android = root_dir / "android" / "android"
    android_app = root_dir / "android" / "app"

    if android_android.exists() and android_app.exists():
        print("  ⚠️  ОБНАРУЖЕН ДУБЛИКАТ ДИРЕКТОРИЙ:")
        print(f"     - android/android/")
        print(f"     - android/app/")
        print("     Эти директории могут быть дубликатами!\n")

    # Итоговый отчет
    print("\n" + "=" * 80)
    print("ИТОГОВЫЙ ОТЧЕТ")
    print("=" * 80)
    print(f"Дубликаты по имени: {len(interesting_duplicates)}")
    print(f"Дубликаты по содержимому: {len(duplicates_by_content)}")
    print(f"Резервные копии: {len(backup_files)}")
    print("=" * 80)

if __name__ == "__main__":
    main()
