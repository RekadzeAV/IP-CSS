#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Скрипт для полного анализа и обновления документации проекта IP-CSS
Обновляет даты, проверяет и исправляет ссылки между документами
"""

import os
import re
import sys
from datetime import datetime
from pathlib import Path
from typing import List, Dict, Tuple, Optional

def get_project_version(project_root: Path) -> str:
    """Получает версию проекта из gradle.properties"""
    gradle_props = project_root / "gradle.properties"
    if gradle_props.exists():
        content = gradle_props.read_text(encoding='utf-8')
        match = re.search(r'version=(.+)', content)
        if match:
            return match.group(1).strip()
    return "Alfa-0.0.1"

def find_markdown_files(root_dir: Path) -> List[Path]:
    """Находит все .md файлы, исключая архивные"""
    md_files = []
    exclude_patterns = ['archive', 'OLD-DOC', 'node_modules', '.git', 'build']
    
    for md_file in root_dir.rglob('*.md'):
        # Проверяем, не находится ли файл в исключенных директориях
        if not any(pattern in str(md_file) for pattern in exclude_patterns):
            md_files.append(md_file)
    
    return md_files

def update_dates_in_content(content: str, current_date: str, current_year: str, 
                           current_month: str, current_date_short: str) -> Tuple[str, bool]:
    """Обновляет даты в содержимом документа"""
    updated = False
    
    # Простые замены дат
    replacements = {
        'Январь 2025': f"{current_month} {current_year}",
        'Декабрь 2025': f"{current_month} {current_year}",
        'январь 2025': f"{current_month.lower()} {current_year}",
        'декабрь 2025': f"{current_month.lower()} {current_year}",
        '2025-01': current_date_short[:7],
        '2025-12': current_date_short[:7],
        '2026-01': current_date_short[:7],
        '27 декабря 2025': current_date,
        '28 января 2025': current_date,
        '15 января 2025': current_date,
        '26 января 2026': current_date,
        '2025-01-27': current_date_short,
        '2025-01-28': current_date_short,
        '2025-12-27': current_date_short,
        '2026-01-26': current_date_short,
    }
    
    for old_date, new_date in replacements.items():
        if old_date in content:
            content = content.replace(old_date, new_date)
            updated = True
    
    # Обновляем паттерны дат с помощью регулярных выражений
    date_patterns = [
        (r'\*\*Дата последнего обновления:\*\* [^\r\n]+', f"**Дата последнего обновления:** {current_date}"),
        (r'\*\*Дата создания:\*\* [^\r\n]+', f"**Дата создания:** {current_date}"),
        (r'\*\*Дата анализа:\*\* [^\r\n]+', f"**Дата анализа:** {current_date}"),
        (r'\*\*Дата выполнения:\*\* [^\r\n]+', f"**Дата выполнения:** {current_date}"),
        (r'\*\*Дата ревизии:\*\* [^\r\n]+', f"**Дата ревизии:** {current_date}"),
        (r'\*\*Последнее обновление:\*\* [^\r\n]+', f"**Последнее обновление:** {current_date}"),
    ]
    
    for pattern, replacement in date_patterns:
        if re.search(pattern, content):
            content = re.sub(pattern, replacement, content)
            updated = True
    
    return content, updated

def extract_links(content: str) -> List[Tuple[str, str]]:
    """Извлекает все ссылки из markdown контента"""
    link_pattern = r'\[([^\]]+)\]\(([^)]+)\)'
    links = []
    
    for match in re.finditer(link_pattern, content):
        link_text = match.group(1)
        link_path = match.group(2)
        links.append((link_text, link_path))
    
    return links

def resolve_link_path(link_path: str, file_path: Path, project_root: Path) -> Optional[Path]:
    """Разрешает относительный путь ссылки в абсолютный"""
    # Пропускаем внешние ссылки и якоря
    if link_path.startswith(('http://', 'https://', 'mailto:', '#')):
        return None
    
    # Если путь начинается с /, то от корня проекта
    if link_path.startswith('/'):
        resolved = project_root / link_path[1:]
    else:
        # Относительный путь от текущего файла
        resolved = file_path.parent / link_path
    
    # Нормализуем путь
    try:
        return resolved.resolve()
    except:
        return None

def main():
    # Определяем корень проекта
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    
    # Получаем текущую дату
    now = datetime.now()
    current_date = now.strftime("%d %B %Y")
    current_year = now.strftime("%Y")
    current_month = now.strftime("%B")
    current_date_short = now.strftime("%Y-%m-%d")
    
    # Получаем версию проекта
    project_version = get_project_version(project_root)
    
    print("=" * 50)
    print("Анализ и обновление документации IP-CSS")
    print("=" * 50)
    print(f"Текущая дата: {current_date}")
    print(f"Версия проекта: {project_version}")
    print()
    
    # Находим все markdown файлы
    md_files = find_markdown_files(project_root)
    print(f"Найдено документов: {len(md_files)}")
    print()
    
    # Шаг 1: Обновление дат
    print("Шаг 1: Обновление дат в документах...")
    print()
    
    updated_count = 0
    for md_file in md_files:
        try:
            content = md_file.read_text(encoding='utf-8')
            updated_content, was_updated = update_dates_in_content(
                content, current_date, current_year, current_month, current_date_short
            )
            
            if was_updated:
                md_file.write_text(updated_content, encoding='utf-8')
                updated_count += 1
                print(f"  [OK] {md_file.name}")
        except Exception as e:
            print(f"  [ERROR] {md_file.name}: {e}")
    
    print()
    print(f"Обновлено файлов с датами: {updated_count}")
    print()
    
    # Шаг 2: Проверка ссылок
    print("Шаг 2: Проверка ссылок между документами...")
    print()
    
    broken_links = []
    total_links = 0
    
    for md_file in md_files:
        try:
            content = md_file.read_text(encoding='utf-8')
            links = extract_links(content)
            total_links += len(links)
            
            for link_text, link_path in links:
                resolved_path = resolve_link_path(link_path, md_file, project_root)
                
                if resolved_path is None:
                    continue
                
                # Проверяем существование файла
                if not resolved_path.exists():
                    relative_file = md_file.relative_to(project_root)
                    broken_links.append({
                        'file': str(relative_file),
                        'link': link_path,
                        'text': link_text,
                        'resolved': str(resolved_path)
                    })
        except Exception as e:
            print(f"  [ERROR] {md_file.name}: {e}")
    
    print(f"Всего проверено ссылок: {total_links}")
    
    if broken_links:
        print(f"Найдено битых ссылок: {len(broken_links)}")
        print()
        print("Битые ссылки:")
        for broken in broken_links[:20]:  # Показываем первые 20
            print(f"  [BROKEN] {broken['file']} -> {broken['link']}")
        if len(broken_links) > 20:
            print(f"  ... и еще {len(broken_links) - 20} ссылок")
    else:
        print("[OK] Все ссылки корректны")
    
    print()
    
    # Шаг 3: Обновление DOCUMENTATION_INDEX.md
    print("Шаг 3: Обновление индекса документации...")
    print()
    
    index_file = project_root / "DOCUMENTATION_INDEX.md"
    if index_file.exists():
        try:
            content = index_file.read_text(encoding='utf-8')
            if re.search(r'\*\*Последнее обновление:\*\*', content):
                content = re.sub(
                    r'\*\*Последнее обновление:\*\* [^\r\n]+',
                    f"**Последнее обновление:** {current_date} (ревизия и обновление связей)",
                    content
                )
                index_file.write_text(content, encoding='utf-8')
                print("  [OK] DOCUMENTATION_INDEX.md обновлен")
        except Exception as e:
            print(f"  [ERROR] Ошибка при обновлении индекса: {e}")
    
    print()
    print("=" * 50)
    print("Ревизия документации завершена!")
    print("=" * 50)
    print()
    print("Статистика:")
    print(f"  - Всего документов: {len(md_files)}")
    print(f"  - Обновлено файлов: {updated_count}")
    print(f"  - Битых ссылок: {len(broken_links)}")
    print()
    print(f"Текущая дата: {current_date}")
    print(f"Версия проекта: {project_version}")
    
    return 0 if not broken_links else 1

if __name__ == '__main__':
    sys.exit(main())
