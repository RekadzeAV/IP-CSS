# Проверка битых ссылок в Markdown документах

# Этот скрипт проверяет все markdown файлы на наличие битых ссылок

import os
import re
import sys
from pathlib import Path

def extract_links(content):
    """Извлекает все ссылки из markdown контента"""
    # Ссылки в формате [text](url)
    pattern = r'\[([^\]]+)\]\(([^)]+)\)'
    matches = re.findall(pattern, content)
    return [(text, url) for text, url in matches if not url.startswith(('http://', 'https://', '#', 'mailto:'))]

def check_links_in_file(filepath, root_dir):
    """Проверяет все ссылки в одном файле"""
    broken_links = []
    
    try:
        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()
        
        links = extract_links(content)
        file_dir = os.path.dirname(filepath)
        
        for text, url in links:
            # Пропускаем внешние ссылки и якоря
            if url.startswith(('http://', 'https://', '#', 'mailto:', 'ftp://')):
                continue
            
            # Обрабатывать относительные пути
            if url.startswith('./'):
                url = url[2:]
            
            # Определяем целевой путь
            if url.startswith('../'):
                # Ссылка на родительскую директорию
                target_path = os.path.normpath(os.path.join(file_dir, url))
            else:
                # Относительно корня проекта или текущей директории
                target_path = os.path.normpath(os.path.join(root_dir, url))
                if not os.path.exists(target_path):
                    # Попробовать относительно текущей директории
                    target_path = os.path.normpath(os.path.join(file_dir, url))
            
            # Проверяем существование файла
            if not os.path.exists(target_path):
                broken_links.append({
                    'file': filepath,
                    'text': text,
                    'url': url,
                    'target': target_path
                })
    
    except Exception as e:
        print(f"Ошибка при обработке {filepath}: {e}")
    
    return broken_links

def main():
    root_dir = sys.argv[1] if len(sys.argv) > 1 else '.'
    
    print(f"Проверка ссылок в: {os.path.abspath(root_dir)}")
    print("=" * 60)
    
    all_broken = []
    checked_files = 0
    
    # Проходим по всем markdown файлам
    for root, dirs, files in os.walk(root_dir):
        # Пропускаем .git и node_modules
        dirs[:] = [d for d in dirs if d not in ['.git', 'node_modules', '__pycache__', 'diagnostics']]
        
        for file in files:
            if file.endswith('.md'):
                filepath = os.path.join(root, file)
                checked_files += 1
                broken = check_links_in_file(filepath, root_dir)
                all_broken.extend(broken)
    
    print(f"\nПроверено файлов: {checked_files}")
    print(f"Найдено битых ссылок: {len(all_broken)}")
    
    if all_broken:
        print("\n" + "=" * 60)
        print("БИТЫЕ ССЫЛКИ:")
        print("=" * 60)
        
        for link in all_broken[:50]:  # Показываем первые 50
            print(f"\nФайл: {link['file']}")
            print(f"  Ссылка: [{link['text']}]({link['url']})")
            print(f"  Ожидается: {link['target']}")
        
        if len(all_broken) > 50:
            print(f"\n... и ещё {len(all_broken) - 50} битых ссылок")
        
        sys.exit(1)
    else:
        print("\n✅ Все ссылки работают!")
        sys.exit(0)

if __name__ == '__main__':
    main()
