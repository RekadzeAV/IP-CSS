#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Скрипт для проверки ссылок между документами проекта IP-CSS
"""

import os
import re
import sys
from pathlib import Path
from typing import List, Dict, Tuple

def find_markdown_files(root_dir: Path) -> List[Path]:
    """Находит все .md файлы, исключая архивные"""
    md_files = []
    exclude_patterns = ['archive', 'OLD-DOC', 'node_modules', '.git', 'build']
    
    for md_file in root_dir.rglob('*.md'):
        # Проверяем, не находится ли файл в исключенных директориях
        if not any(pattern in str(md_file) for pattern in exclude_patterns):
            md_files.append(md_file)
    
    return md_files

def extract_links(content: str) -> List[Tuple[str, str]]:
    """Извлекает все ссылки из markdown контента"""
    # Паттерн для markdown ссылок: [текст](путь)
    link_pattern = r'\[([^\]]+)\]\(([^)]+)\)'
    links = []
    
    for match in re.finditer(link_pattern, content):
        link_text = match.group(1)
        link_path = match.group(2)
        links.append((link_text, link_path))
    
    return links

def resolve_link_path(link_path: str, file_path: Path, project_root: Path) -> Path:
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

def check_links(project_root: Path) -> Dict[str, List[Dict]]:
    """Проверяет все ссылки в документации"""
    md_files = find_markdown_files(project_root)
    broken_links = []
    all_links_count = 0
    
    print(f"Найдено документов: {len(md_files)}")
    print("Проверка ссылок...\n")
    
    for md_file in md_files:
        try:
            content = md_file.read_text(encoding='utf-8')
            links = extract_links(content)
            all_links_count += len(links)
            
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
            print(f"Ошибка при обработке {md_file}: {e}", file=sys.stderr)
    
    return {
        'broken': broken_links,
        'total_links': all_links_count,
        'total_files': len(md_files)
    }

def main():
    # Определяем корень проекта (родительская директория scripts/)
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    
    print("=" * 50)
    print("Проверка ссылок в документации IP-CSS")
    print("=" * 50)
    print()
    
    results = check_links(project_root)
    
    print(f"Всего проверено ссылок: {results['total_links']}")
    print(f"Битых ссылок: {len(results['broken'])}")
    print()
    
    if results['broken']:
        print("Битые ссылки:")
        print("-" * 50)
        for broken in results['broken']:
            print(f"Файл: {broken['file']}")
            print(f"  Ссылка: {broken['link']}")
            print(f"  Текст: {broken['text']}")
            print(f"  Ожидаемый путь: {broken['resolved']}")
            print()
        
        return 1
    else:
        print("✓ Все ссылки корректны!")
        return 0

if __name__ == '__main__':
    sys.exit(main())
