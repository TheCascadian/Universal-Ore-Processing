"""Generates the base textures for Universal Ore Processing.

Item textures are grayscale so the client ItemColor handler can tint them per
material. Machine textures are full color. Run from the repository root:

    python tools/generate_textures.py
"""
import random
from pathlib import Path

from PIL import Image

SIZE = 16
ASSETS = Path("src/main/resources/assets/universaloreprocessing/textures")
ITEM_DIR = ASSETS / "item"
BLOCK_DIR = ASSETS / "block"


def gray(value):
    return (value, value, value, 255)


def blank():
    return Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))


def draw_chunk(image, cx, cy, radius, base, rng):
    """Draws a lit, roughly round chunk: bright upper left, dark lower right."""
    for y in range(SIZE):
        for x in range(SIZE):
            dx, dy = x - cx, y - cy
            if dx * dx + dy * dy > radius * radius + rng.randint(-1, 1):
                continue
            light = (-dx - dy) / (2.0 * radius)
            value = max(60, min(255, int(base + light * 55 + rng.randint(-8, 8))))
            image.putpixel((x, y), gray(value))


def crushed_ore():
    rng = random.Random(11)
    image = blank()
    for cx, cy, radius in [(5, 10, 3), (10, 11, 3), (8, 6, 3), (12, 7, 2), (4, 5, 2)]:
        draw_chunk(image, cx, cy, radius, 175, rng)
    return image


def purified_ore():
    rng = random.Random(23)
    image = blank()
    for cx, cy, radius in [(6, 9, 4), (11, 10, 3), (9, 5, 3)]:
        draw_chunk(image, cx, cy, radius, 215, rng)
    for _ in range(6):
        x, y = rng.randint(3, 12), rng.randint(3, 12)
        if image.getpixel((x, y))[3] == 255:
            image.putpixel((x, y), gray(255))
    return image


def material_dust():
    rng = random.Random(37)
    image = blank()
    for y in range(6, 14):
        half = (y - 4) * 7 // 9
        for x in range(8 - half, 8 + half):
            if 0 <= x < SIZE:
                value = 200 - (y - 6) * 6 + rng.randint(-12, 12)
                image.putpixel((x, y), gray(max(90, min(255, value))))
    return image


def machine_panel(base, rng, noise=6):
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 255))
    for y in range(SIZE):
        for x in range(SIZE):
            value = base + rng.randint(-noise, noise)
            if x in (0, SIZE - 1) or y in (0, SIZE - 1):
                value -= 28
            elif x in (1, SIZE - 2) or y in (1, SIZE - 2):
                value += 10
            image.putpixel((x, y), (value, value, value + 4, 255))
    return image


def fill_rect(image, x0, y0, x1, y1, color):
    for y in range(y0, y1):
        for x in range(x0, x1):
            image.putpixel((x, y), color)


def crusher_front(lit):
    image = machine_panel(112, random.Random(5))
    fill_rect(image, 3, 3, 13, 13, (36, 36, 40, 255))
    glow = (232, 137, 43, 255) if lit else (150, 150, 158, 255)
    for x in range(4, 12, 2):
        fill_rect(image, x, 4, x + 1, 8, glow)
        fill_rect(image, x + 1, 8, x + 2, 12, glow)
    return image


def washer_front(lit):
    image = machine_panel(112, random.Random(7))
    fill_rect(image, 3, 3, 13, 13, (36, 36, 40, 255))
    water = (63, 118, 228, 255) if lit else (52, 82, 140, 255)
    for y in range(5, 12, 3):
        for x in range(4, 12):
            image.putpixel((x, y + (x % 2)), water)
    return image


def smelter_front(lit):
    image = machine_panel(112, random.Random(9))
    fill_rect(image, 3, 4, 13, 13, (36, 36, 40, 255))
    glow = (255, 160, 40, 255) if lit else (70, 40, 30, 255)
    fill_rect(image, 4, 7, 12, 12, glow)
    if lit:
        fill_rect(image, 6, 8, 10, 11, (255, 226, 120, 255))
    return image


def save(image, path):
    path.parent.mkdir(parents=True, exist_ok=True)
    image.save(path)


def main():
    save(crushed_ore(), ITEM_DIR / "crushed_ore.png")
    save(purified_ore(), ITEM_DIR / "purified_ore.png")
    save(material_dust(), ITEM_DIR / "material_dust.png")

    save(machine_panel(128, random.Random(1)), BLOCK_DIR / "machine_top.png")
    save(machine_panel(104, random.Random(2)), BLOCK_DIR / "machine_side.png")
    save(machine_panel(88, random.Random(3)), BLOCK_DIR / "machine_bottom.png")
    for name, factory in (("ore_crusher", crusher_front), ("ore_washer", washer_front),
                          ("ore_smelter", smelter_front)):
        save(factory(False), BLOCK_DIR / f"{name}_front.png")
        save(factory(True), BLOCK_DIR / f"{name}_front_on.png")


if __name__ == "__main__":
    main()
