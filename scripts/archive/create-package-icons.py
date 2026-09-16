#!/usr/bin/env python3
"""
Скрипт для создания placeholder иконок для Synology SPK пакета
"""

import sys
import os
from pathlib import Path

try:
    from PIL import Image, ImageDraw, ImageFont
    HAS_PIL = True
except ImportError:
    HAS_PIL = False
    print("Warning: PIL/Pillow not installed. Install with: pip install Pillow")
    print("Creating simple placeholder files instead...")

def create_icon(size, output_path, text="IP-CSS"):
    """Создать иконку заданного размера"""
    if HAS_PIL:
        # Создать изображение с прозрачным фоном
        img = Image.new('RGBA', (size, size), (0, 0, 0, 0))
        draw = ImageDraw.Draw(img)

        # Цвет фона (синий)
        bg_color = (33, 150, 243, 255)  # #2196F3
        # Цвет текста (белый)
        text_color = (255, 255, 255, 255)

        # Нарисовать закругленный прямоугольник
        margin = max(4, size // 20)
        draw.rounded_rectangle(
            [margin, margin, size - margin, size - margin],
            radius=size // 7,
            fill=bg_color
        )

        # Добавить текст
        try:
            # Попробовать использовать системный шрифт
            font_size = size // 4
            try:
                font = ImageFont.truetype("arial.ttf", font_size)
            except:
                try:
                    font = ImageFont.truetype("C:/Windows/Fonts/arial.ttf", font_size)
                except:
                    font = ImageFont.load_default()
        except:
            font = ImageFont.load_default()

        # Центрировать текст
        bbox = draw.textbbox((0, 0), text, font=font)
        text_width = bbox[2] - bbox[0]
        text_height = bbox[3] - bbox[1]
        position = ((size - text_width) // 2, (size - text_height) // 2)

        draw.text(position, text, fill=text_color, font=font)

        # Сохранить
        img.save(output_path, 'PNG')
        print(f"[OK] Created: {output_path} ({size}x{size})")
    else:
        # Создать минимальный PNG файл (заглушка)
        # Это будет очень простой PNG с минимальным размером
        png_header = b'\x89PNG\r\n\x1a\n'
        # Простейший валидный PNG (1x1 пиксель, прозрачный)
        minimal_png = (
            png_header +
            b'\x00\x00\x00\rIHDR' +
            (size).to_bytes(4, 'big') + (size).to_bytes(4, 'big') +
            b'\x08\x06\x00\x00\x00' +
            b'\x00\x00\x00\x00IEND\xaeB`\x82'
        )
        with open(output_path, 'wb') as f:
            f.write(minimal_png)
        print(f"[WARN] Created minimal placeholder: {output_path} (install Pillow for better icons)")

def main():
    project_root = Path(__file__).parent.parent

    # Иконки для x86_64
    icons_x86 = project_root / "platforms" / "nas-x86_64" / "packages" / "synology" / "icons"
    icons_x86.mkdir(parents=True, exist_ok=True)

    # Иконки для ARM
    icons_arm = project_root / "platforms" / "nas-arm" / "packages" / "synology" / "icons"
    icons_arm.mkdir(parents=True, exist_ok=True)

    # Создать иконки
    sizes = [
        (72, "PACKAGE_ICON.PNG"),
        (256, "PACKAGE_ICON_256.PNG")
    ]

    for size, filename in sizes:
        # x86_64
        create_icon(size, icons_x86 / filename)
        # ARM
        create_icon(size, icons_arm / filename)

    print("\n[OK] All icons created successfully!")
    if not HAS_PIL:
        print("\nNote: For better quality icons, install Pillow:")
        print("  pip install Pillow")
        print("Then run this script again.")

if __name__ == "__main__":
    main()
