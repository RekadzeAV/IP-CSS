#!/usr/bin/env python3
"""
Скрипт для генерации графиков и диаграмм готовности проекта
"""

import matplotlib.pyplot as plt
import matplotlib.patches as mpatches
import numpy as np
from datetime import datetime, timedelta
import json
import os

# Настройка для поддержки русского языка
plt.rcParams['font.family'] = 'DejaVu Sans'
plt.rcParams['axes.unicode_minus'] = False

# Данные готовности по блокам
blocks_data = {
    'Инфраструктура': 100,
    'Доменный слой': 55,
    'Слой данных': 90,
    'Сетевой слой': 40,
    'Серверная часть': 85,
    'Веб-интерфейс': 80,
    'Мобильные платформы': 30,
    'Desktop приложения': 0,
    'Видео и запись': 70,
    'Безопасность': 75,
    'Тестирование': 15
}

# Цвета для статусов
def get_color(value):
    if value >= 90:
        return '#2ecc71'  # Зеленый
    elif value >= 70:
        return '#f39c12'  # Оранжевый
    elif value >= 40:
        return '#e67e22'  # Темно-оранжевый
    else:
        return '#e74c3c'  # Красный

# Создание директории для графиков
os.makedirs('docs/charts', exist_ok=True)

# 1. Горизонтальная столбчатая диаграмма готовности
def create_readiness_bar_chart():
    fig, ax = plt.subplots(figsize=(14, 10))

    blocks = list(blocks_data.keys())
    values = list(blocks_data.values())
    colors = [get_color(v) for v in values]

    y_pos = np.arange(len(blocks))
    bars = ax.barh(y_pos, values, color=colors, alpha=0.8)

    # Добавление значений на столбцы
    for i, (bar, value) in enumerate(zip(bars, values)):
        ax.text(value + 1, i, f'{value}%', va='center', fontsize=10, fontweight='bold')

    ax.set_yticks(y_pos)
    ax.set_yticklabels(blocks, fontsize=11)
    ax.set_xlabel('Процент готовности (%)', fontsize=12, fontweight='bold')
    ax.set_title('Карта готовности проекта IP-CSS по блокам', fontsize=14, fontweight='bold', pad=20)
    ax.set_xlim(0, 110)
    ax.grid(axis='x', alpha=0.3, linestyle='--')

    # Легенда
    legend_elements = [
        mpatches.Patch(color='#2ecc71', label='90-100% (Готово)'),
        mpatches.Patch(color='#f39c12', label='70-89% (Почти готово)'),
        mpatches.Patch(color='#e67e22', label='40-69% (В процессе)'),
        mpatches.Patch(color='#e74c3c', label='0-39% (Начато/Не начато)')
    ]
    ax.legend(handles=legend_elements, loc='lower right', fontsize=10)

    plt.tight_layout()
    plt.savefig('docs/charts/readiness_bar_chart.png', dpi=300, bbox_inches='tight')
    print("✅ Создан график: docs/charts/readiness_bar_chart.png")
    plt.close()

# 2. Круговая диаграмма общего прогресса
def create_progress_pie_chart():
    fig, ax = plt.subplots(figsize=(10, 10))

    # Категории по статусу
    categories = {
        'Готово (90-100%)': sum(1 for v in blocks_data.values() if v >= 90),
        'Почти готово (70-89%)': sum(1 for v in blocks_data.values() if 70 <= v < 90),
        'В процессе (40-69%)': sum(1 for v in blocks_data.values() if 40 <= v < 70),
        'Начато/Не начато (0-39%)': sum(1 for v in blocks_data.values() if v < 40)
    }

    colors = ['#2ecc71', '#f39c12', '#e67e22', '#e74c3c']
    explode = (0.05, 0.05, 0.05, 0.05)

    wedges, texts, autotexts = ax.pie(
        categories.values(),
        labels=categories.keys(),
        colors=colors,
        autopct='%1.1f%%',
        startangle=90,
        explode=explode,
        textprops={'fontsize': 11, 'fontweight': 'bold'}
    )

    for autotext in autotexts:
        autotext.set_color('white')
        autotext.set_fontsize(12)

    ax.set_title('Распределение блоков по статусу готовности', fontsize=14, fontweight='bold', pad=20)

    plt.tight_layout()
    plt.savefig('docs/charts/progress_pie_chart.png', dpi=300, bbox_inches='tight')
    print("✅ Создан график: docs/charts/progress_pie_chart.png")
    plt.close()

# 3. График прогресса по времени (roadmap)
def create_roadmap_chart():
    fig, ax = plt.subplots(figsize=(16, 10))

    # Данные по кварталам
    quarters = ['Q1 2026\n(Янв-Мар)', 'Q2 2026\n(Апр-Июн)', 'Q3 2026\n(Июл-Сен)', 'Q4 2026\n(Окт-Дек)']
    progress = [77, 100, 140, 180]  # Процент готовности

    # Критические блокеры
    blockers = {
        'Q1': ['RTSP клиент', 'Certificate Pinning', 'ONVIF WS-Discovery'],
        'Q2': ['Стабилизация', 'Тестирование', 'Багфиксы'],
        'Q3': ['AI-аналитика', 'Детекция объектов', 'Трекинг'],
        'Q4': ['NAS платформы', 'Desktop приложения', 'Enterprise безопасность']
    }

    # График прогресса
    ax.plot(quarters, progress, marker='o', linewidth=3, markersize=12, color='#3498db', label='Прогресс проекта')
    ax.fill_between(quarters, progress, alpha=0.3, color='#3498db')

    # Добавление значений
    for i, (q, p) in enumerate(zip(quarters, progress)):
        ax.text(i, p + 5, f'{p}%', ha='center', fontsize=11, fontweight='bold', color='#2c3e50')

    # Добавление блокеров
    blocker_y = [85, 105, 145, 185]
    for i, (q, blockers_list) in enumerate(blockers.items()):
        blocker_text = '\n'.join([f'• {b}' for b in blockers_list])
        ax.text(i, blocker_y[i], blocker_text, ha='center', fontsize=9,
                bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.7),
                va='bottom')

    ax.set_ylabel('Процент готовности (%)', fontsize=12, fontweight='bold')
    ax.set_title('Карта реализации проекта IP-CSS до конца 2026 года', fontsize=14, fontweight='bold', pad=20)
    ax.grid(True, alpha=0.3, linestyle='--')
    ax.set_ylim(60, 200)

    # Вехи
    milestones = [
        (0, 90, 'Завершение\nкритических\nблокеров'),
        (1, 100, 'Релиз\nv1.0.0\n(MVP)'),
        (2, 140, 'AI-аналитика\n90%'),
        (3, 180, 'Релиз\nv2.0.0\n(Enterprise)')
    ]

    for x, y, label in milestones:
        ax.annotate(label, xy=(x, y), xytext=(x, y + 15),
                   ha='center', fontsize=9, fontweight='bold',
                   bbox=dict(boxstyle='round,pad=0.5', facecolor='yellow', alpha=0.7),
                   arrowprops=dict(arrowstyle='->', lw=1.5, color='black'))

    plt.tight_layout()
    plt.savefig('docs/charts/roadmap_chart.png', dpi=300, bbox_inches='tight')
    print("✅ Создан график: docs/charts/roadmap_chart.png")
    plt.close()

# 4. Детальная декомпозиция по подблокам
def create_detailed_decomposition():
    fig, ax = plt.subplots(figsize=(16, 12))

    # Детальные данные по подблокам
    detailed_data = {
        'Инфраструктура': {
            'Структура проекта': 100,
            'Конфигурация': 100,
            'CI/CD': 100,
            'Документация': 100
        },
        'Доменный слой': {
            'Модели данных': 86,
            'Интерфейсы репозиториев': 86,
            'Use Cases': 78,
            'Доменные сервисы': 60
        },
        'Слой данных': {
            'База данных': 75,
            'Реализации репозиториев': 98,
            'Источники данных': 96
        },
        'Сетевой слой': {
            'REST API клиент': 100,
            'WebSocket клиент': 80,
            'RTSP клиент': 10,
            'ONVIF клиент': 70
        },
        'Серверная часть': {
            'REST API сервер': 100,
            'Аутентификация': 100,
            'WebSocket сервер': 100,
            'Сервисы': 100,
            'База данных': 70
        },
        'Веб-интерфейс': {
            'Конфигурация': 100,
            'Страницы': 100,
            'Компоненты': 92,
            'API интеграция': 100,
            'Безопасность': 100
        },
        'Мобильные платформы': {
            'Android': 75,
            'iOS': 0
        },
        'Видео и запись': {
            'Запись видео': 93,
            'Потоки и скриншоты': 100,
            'RTSP клиент': 10,
            'Видеоплеер': 78
        },
        'Безопасность': {
            'Аутентификация': 100,
            'Шифрование': 60,
            'Защита от атак': 100
        },
        'Тестирование': {
            'Unit тесты': 50,
            'Integration тесты': 10,
            'UI тесты': 0
        }
    }

    # Подготовка данных для графика
    y_pos = 0
    y_ticks = []
    y_labels = []
    colors_list = []
    values_list = []

    for main_block, sub_blocks in detailed_data.items():
        for sub_block, value in sub_blocks.items():
            y_ticks.append(y_pos)
            y_labels.append(f"{main_block}\n{sub_block}")
            colors_list.append(get_color(value))
            values_list.append(value)
            y_pos += 1
        y_pos += 0.5  # Промежуток между основными блоками

    # Создание графика
    bars = ax.barh(range(len(values_list)), values_list, color=colors_list, alpha=0.8)

    # Добавление значений
    for i, (bar, value) in enumerate(zip(bars, values_list)):
        ax.text(value + 1, i, f'{value}%', va='center', fontsize=9, fontweight='bold')

    ax.set_yticks(y_ticks)
    ax.set_yticklabels(y_labels, fontsize=9)
    ax.set_xlabel('Процент готовности (%)', fontsize=12, fontweight='bold')
    ax.set_title('Детальная декомпозиция готовности по подблокам', fontsize=14, fontweight='bold', pad=20)
    ax.set_xlim(0, 110)
    ax.grid(axis='x', alpha=0.3, linestyle='--')

    # Легенда
    legend_elements = [
        mpatches.Patch(color='#2ecc71', label='90-100% (Готово)'),
        mpatches.Patch(color='#f39c12', label='70-89% (Почти готово)'),
        mpatches.Patch(color='#e67e22', label='40-69% (В процессе)'),
        mpatches.Patch(color='#e74c3c', label='0-39% (Начато/Не начато)')
    ]
    ax.legend(handles=legend_elements, loc='lower right', fontsize=10)

    plt.tight_layout()
    plt.savefig('docs/charts/detailed_decomposition.png', dpi=300, bbox_inches='tight')
    print("✅ Создан график: docs/charts/detailed_decomposition.png")
    plt.close()

# 5. График покрытия тестами
def create_test_coverage_chart():
    fig, ax = plt.subplots(figsize=(12, 8))

    test_categories = {
        'Unit тесты': 50,
        'Integration тесты': 10,
        'UI тесты': 0
    }

    colors = ['#3498db', '#9b59b6', '#e74c3c']
    bars = ax.bar(test_categories.keys(), test_categories.values(), color=colors, alpha=0.8)

    # Добавление значений
    for bar, value in zip(bars, test_categories.values()):
        height = bar.get_height()
        ax.text(bar.get_x() + bar.get_width()/2., height + 1,
               f'{value}%', ha='center', va='bottom', fontsize=11, fontweight='bold')

    # Целевая линия
    ax.axhline(y=50, color='green', linestyle='--', linewidth=2, label='Целевое покрытие (50%)')
    ax.axhline(y=70, color='blue', linestyle='--', linewidth=2, label='Целевое покрытие (70%)')

    ax.set_ylabel('Покрытие тестами (%)', fontsize=12, fontweight='bold')
    ax.set_title('Покрытие тестами по категориям', fontsize=14, fontweight='bold', pad=20)
    ax.set_ylim(0, 80)
    ax.grid(axis='y', alpha=0.3, linestyle='--')
    ax.legend(fontsize=10)

    plt.tight_layout()
    plt.savefig('docs/charts/test_coverage_chart.png', dpi=300, bbox_inches='tight')
    print("✅ Создан график: docs/charts/test_coverage_chart.png")
    plt.close()

# 6. График критических блокеров
def create_blockers_chart():
    fig, ax = plt.subplots(figsize=(12, 8))

    blockers = {
        'RTSP клиент': 10,
        'Certificate Pinning': 50,
        'ONVIF WS-Discovery': 70,
        'Покрытие тестами': 15
    }

    colors = ['#e74c3c', '#e67e22', '#f39c12', '#3498db']
    bars = ax.barh(list(blockers.keys()), list(blockers.values()), color=colors, alpha=0.8)

    # Добавление значений
    for bar, value in zip(bars, blockers.values()):
        ax.text(value + 2, bar.get_y() + bar.get_height()/2,
               f'{value}%', va='center', fontsize=11, fontweight='bold')

    # Целевая линия
    ax.axvline(x=100, color='green', linestyle='--', linewidth=2, label='Цель (100%)')

    ax.set_xlabel('Процент готовности (%)', fontsize=12, fontweight='bold')
    ax.set_title('Критические блокеры MVP', fontsize=14, fontweight='bold', pad=20)
    ax.set_xlim(0, 110)
    ax.grid(axis='x', alpha=0.3, linestyle='--')
    ax.legend(fontsize=10)

    plt.tight_layout()
    plt.savefig('docs/charts/blockers_chart.png', dpi=300, bbox_inches='tight')
    print("✅ Создан график: docs/charts/blockers_chart.png")
    plt.close()

def main():
    print("🚀 Генерация графиков и диаграмм готовности проекта...")
    print()

    create_readiness_bar_chart()
    create_progress_pie_chart()
    create_roadmap_chart()
    create_detailed_decomposition()
    create_test_coverage_chart()
    create_blockers_chart()

    print()
    print("✅ Все графики успешно созданы в директории docs/charts/")
    print()
    print("Созданные файлы:")
    print("  - readiness_bar_chart.png - Карта готовности по блокам")
    print("  - progress_pie_chart.png - Распределение блоков по статусу")
    print("  - roadmap_chart.png - Карта реализации до конца года")
    print("  - detailed_decomposition.png - Детальная декомпозиция")
    print("  - test_coverage_chart.png - Покрытие тестами")
    print("  - blockers_chart.png - Критические блокеры")

if __name__ == '__main__':
    main()
