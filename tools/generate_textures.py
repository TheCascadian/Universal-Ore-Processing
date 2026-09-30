"""Generates the base textures for Universal Ore Processing.

Stage item textures are grayscale so the client ItemColor handler can tint them
per material. Reagent and machine textures are full color. Run from the
repository root:

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


# ---------------------------------------------------------------------------
# Refinery stage items (grayscale, tinted per material on the client)
# ---------------------------------------------------------------------------

def put(image, x, y, value):
    if 0 <= x < SIZE and 0 <= y < SIZE:
        image.putpixel((x, y), gray(max(40, min(255, int(value)))))


def lit(base, dx, dy, radius, rng, noise=8):
    return base + (-dx - dy) / (2.0 * max(radius, 1)) * 50 + rng.randint(-noise, noise)


def pile(seed, spots, base=190, holes=0):
    rng = random.Random(seed)
    image = blank()
    for cx, cy, radius in spots:
        draw_chunk(image, cx, cy, radius, base, rng)
    for _ in range(holes):
        x, y = rng.randint(3, 12), rng.randint(4, 12)
        if image.getpixel((x, y))[3] == 255:
            image.putpixel((x, y), gray(70))
    return image


def block_shape(seed, x0, y0, x1, y1, base, bevel=True, holes=0, stripes=0, lines=0):
    """A lit rectangular body: ingots, plates, billets and sponge all start here."""
    rng = random.Random(seed)
    image = blank()
    w, h = x1 - x0, y1 - y0
    for y in range(y0, y1):
        for x in range(x0, x1):
            value = base + rng.randint(-6, 6)
            if bevel:
                if y == y0 or x == x0:
                    value += 38
                if y == y1 - 1 or x == x1 - 1:
                    value -= 42
            put(image, x, y, value)
    for _ in range(holes):
        put(image, rng.randint(x0 + 1, x1 - 2), rng.randint(y0 + 1, y1 - 2), 60)
    for i in range(stripes):
        yy = y0 + 2 + i * max(2, h // (stripes + 1))
        for x in range(x0 + 1, x1 - 1):
            put(image, x, yy, base - 40)
    for i in range(lines):
        xx = x0 + 2 + i * max(2, w // (lines + 1))
        for y in range(y0 + 1, y1 - 1):
            put(image, xx, y, base - 38)
    return image


def vial(seed, fill, liquid=200, glass=230, wide=False):
    """A flask with a cork, filled from the bottom to the given height."""
    rng = random.Random(seed)
    image = blank()
    half = 4 if wide else 3
    for y in range(6, 14):
        for x in range(8 - half, 8 + half):
            edge = x in (8 - half, 8 + half - 1) or y == 13
            if y >= 13 - fill and not edge:
                put(image, x, y, liquid + rng.randint(-6, 6))
            else:
                put(image, x, y, glass if edge else 120)
    for y in range(3, 6):
        for x in (7, 8):
            put(image, x, y, glass if y == 3 else 150)
    put(image, 7, 2, 90)
    put(image, 8, 2, 90)
    return image


def bubbles(seed, count=9):
    rng = random.Random(seed)
    image = blank()
    for _ in range(count):
        cx, cy, r = rng.randint(3, 12), rng.randint(3, 12), rng.choice((1, 2, 2, 3))
        for y in range(SIZE):
            for x in range(SIZE):
                d2 = (x - cx) ** 2 + (y - cy) ** 2
                if d2 <= r * r:
                    put(image, x, y, 230 if d2 > (r - 1) ** 2 else 175)
    return image


def cylinder(seed, x0, x1, y0, y1, base=200, band=False, caps=True):
    rng = random.Random(seed)
    image = blank()
    w = x1 - x0
    for y in range(y0, y1):
        for x in range(x0, x1):
            t = (x - x0) / max(1, w - 1)
            value = base + 45 * (0.5 - t) + rng.randint(-5, 5)
            if caps and (y == y0 or y == y1 - 1):
                value += 25 if y == y0 else -35
            if band and y0 + (y1 - y0) // 2 - 1 <= y <= y0 + (y1 - y0) // 2:
                value -= 70
            put(image, x, y, value)
    return image


def crystal(seed, tall=True):
    rng = random.Random(seed)
    image = blank()
    for y in range(1, 15):
        if tall:
            half = 2 + min(y - 1, 4) if y < 12 else max(1, 2 + (14 - y))
        else:
            half = 4 - abs(y - 8) // 2
        for x in range(8 - half, 8 + half):
            facet = 40 if x < 8 else -30
            put(image, x, y, 190 + facet + rng.randint(-6, 6))
    return image


def blob(seed, cx, cy, rx, ry, base=170):
    rng = random.Random(seed)
    image = blank()
    for y in range(SIZE):
        for x in range(SIZE):
            d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
            if d <= 1 + rng.uniform(-0.15, 0.15):
                put(image, x, y, base + (cx - x + cy - y) * 4 + rng.randint(-10, 10))
    return image


def glass_cube(seed):
    rng = random.Random(seed)
    image = block_shape(seed, 3, 3, 13, 13, 170)
    for _ in range(5):
        put(image, rng.randint(4, 11), rng.randint(4, 11), 250)
    return image


def cathode():
    image = block_shape(61, 3, 4, 13, 14, 195, stripes=0)
    for x in (4, 5, 10, 11):
        for y in (1, 2, 3):
            put(image, x, y, 120)
    return image


def rod_bundle():
    image = blank()
    for i, x in enumerate((4, 7, 10)):
        rod = cylinder(70 + i, x, x + 3, 2, 14, base=215, band=(i == 1))
        for yy in range(SIZE):
            for xx in range(SIZE):
                px = rod.getpixel((xx, yy))
                if px[3]:
                    image.putpixel((xx, yy), px)
    return image


def slab_grid():
    image = block_shape(81, 2, 5, 14, 12, 185)
    for x in range(4, 13, 3):
        for y in range(6, 11):
            put(image, x, y, 120)
    for y in (7, 9):
        for x in range(3, 13):
            put(image, x, y, 130)
    return image


def flask_small():
    return vial(91, fill=3, liquid=215, glass=240)


def flask_wide():
    return vial(92, fill=5, liquid=150, glass=200, wide=True)


STAGE_TEXTURES = {
    "heavy_concentrate": lambda: pile(101, [(5, 10, 3), (10, 10, 4), (8, 5, 3)], base=150),
    "light_gangue": lambda: pile(102, [(5, 10, 3), (10, 11, 3), (8, 6, 3), (11, 6, 2)], base=225, holes=8),
    "mineral_froth": lambda: bubbles(103),
    "spent_tailings": lambda: pile(104, [(8, 11, 5), (4, 12, 2), (12, 12, 2)], base=150, holes=4),
    "magnetic_fraction": lambda: pile(105, [(6, 9, 4), (11, 10, 3), (9, 5, 3)], base=170, holes=3),
    "nonmagnetic_tailings": lambda: pile(106, [(4, 11, 2), (8, 12, 2), (12, 11, 2), (6, 6, 2), (11, 6, 2)],
                                          base=200, holes=2),
    "crude_metal": lambda: blob(107, 8, 9, 6, 4, base=165),
    "vitreous_slag": lambda: blob(108, 8, 9, 5, 5, base=120),
    "converted_billet": lambda: block_shape(109, 2, 6, 14, 11, 205),
    "metal_sponge": lambda: block_shape(110, 3, 4, 13, 13, 190, holes=16),
    "recovery_salt": lambda: pile(111, [(5, 11, 2), (8, 9, 3), (11, 11, 2), (8, 5, 2)], base=240),
    "leach_solution": lambda: vial(112, fill=6, liquid=190),
    "filter_cake": lambda: block_shape(113, 2, 8, 14, 12, 175, bevel=False, holes=6),
    "organic_extract": lambda: vial(114, fill=4, liquid=225, glass=245),
    "stripped_raffinate": lambda: vial(115, fill=2, liquid=140, glass=210),
    "solid_precipitate": lambda: pile(116, [(5, 11, 2), (8, 10, 2), (11, 11, 2), (7, 7, 2), (10, 7, 1)], base=215),
    "cathode_plate": cathode,
    "anode_slime": lambda: blob(117, 8, 10, 6, 3, base=110),
    "electrolytic_metal": lambda: block_shape(118, 2, 5, 14, 12, 225, lines=2),
    "degassed_metal": lambda: block_shape(119, 2, 5, 14, 12, 235),
    "modified_inclusions": lambda: pile(120, [(4, 10, 1), (8, 9, 1), (11, 11, 1), (6, 5, 1), (12, 6, 1)], base=120),
    "arc_remelted_ingot": lambda: block_shape(121, 2, 5, 14, 12, 245, stripes=1),
    "light_distillate": flask_small,
    "heavy_distillate": flask_wide,
    "vapor_refined_pellet": lambda: cylinder(122, 5, 11, 4, 12, base=235),
    "polycrystal_cylinder": lambda: cylinder(123, 3, 13, 2, 14, base=215, band=True),
    "monocrystal_boule": lambda: crystal(124),
    "crystal_crop_ends": lambda: cylinder(125, 3, 13, 9, 13, base=170),
    "sintered_monolith": slab_grid,
    "enriched_fraction": lambda: cylinder(126, 5, 11, 3, 13, base=225, band=True),
    "depleted_tails": lambda: cylinder(127, 5, 11, 3, 13, base=150),
    "fissile_stream": rod_bundle,
    "vitrified_waste": lambda: glass_cube(128),
}


# ---------------------------------------------------------------------------
# Reagent items (full color)
# ---------------------------------------------------------------------------

def tinted(image, rgb):
    out = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for y in range(SIZE):
        for x in range(SIZE):
            r, g, b, a = image.getpixel((x, y))
            if a:
                k = r / 255.0
                out.putpixel((x, y), (int(rgb[0] * k), int(rgb[1] * k), int(rgb[2] * k), 255))
    return out


def canister(seed, rgb):
    image = block_shape(seed, 5, 4, 11, 14, 210)
    for x in range(6, 10):
        put(image, x, 3, 150)
    put(image, 7, 2, 120)
    put(image, 8, 2, 120)
    for y in range(7, 10):
        for x in range(5, 11):
            put(image, x, y, 250)
    return tinted(image, rgb)


REAGENT_TEXTURES = {
    "dense_medium": lambda: tinted(pile(131, [(5, 10, 3), (10, 10, 4), (8, 5, 3)], base=160), (70, 70, 88)),
    "flotation_collector": lambda: tinted(vial(132, fill=5, liquid=200, glass=235), (214, 168, 56)),
    "smelting_flux": lambda: tinted(pile(133, [(6, 10, 4), (11, 10, 3), (8, 5, 3)], base=235), (230, 226, 210)),
    "coke": lambda: tinted(pile(134, [(5, 10, 3), (10, 11, 3), (8, 6, 3), (12, 7, 2)], base=150), (52, 50, 58)),
    "leach_acid": lambda: tinted(vial(135, fill=6, liquid=215, glass=235), (166, 214, 52)),
    "organic_solvent": lambda: tinted(vial(136, fill=5, liquid=225, glass=240), (122, 168, 214)),
    "precipitant": lambda: tinted(pile(137, [(6, 10, 4), (11, 11, 3), (8, 5, 3)], base=220), (188, 188, 204)),
    "reducing_agent": lambda: tinted(pile(138, [(6, 10, 4), (11, 11, 3), (8, 5, 3)], base=215), (196, 92, 62)),
    "inert_gas": lambda: canister(139, (122, 178, 226)),
    "process_gas": lambda: canister(140, (226, 150, 76)),
    "seed_crystal": lambda: tinted(crystal(141, tall=False), (188, 140, 226)),
}


# ---------------------------------------------------------------------------
# Refinery machines. Each front carries a small glyph and a tier color bar.
# ---------------------------------------------------------------------------

KIND_TIERS = {
    "density_classifier": 1, "flotation_cell": 1, "magnetic_separator": 1,
    "blast_furnace": 2, "oxidation_converter": 2, "thermal_retort": 2,
    "pressure_autoclave": 3, "phase_extractor": 3, "precipitation_array": 3,
    "electrorefining_cell": 4, "molten_salt_electrolyzer": 4,
    "vacuum_outgasser": 5, "arc_remelter": 5,
    "fractionation_column": 6, "volatile_vaporizer": 6,
    "vapor_deposition_furnace": 7, "crystal_puller": 7,
    "graphitizer": 8, "centrifuge_cascade": 8, "hot_cell": 8,
}

TIER_COLORS = {
    1: (96, 168, 96), 2: (214, 130, 54), 3: (150, 96, 200), 4: (222, 196, 70),
    5: (80, 190, 200), 6: (70, 168, 150), 7: (214, 110, 170), 8: (200, 70, 70),
}

GLYPHS = {
    "density_classifier": ["........", "########", "........", "######..", "........", "####....", "........", "........"],
    "flotation_cell": ["..o..o..", ".o.oo..o", "..o...o.", ".o..o.o.", "########", "#......#", "#......#", "########"],
    "magnetic_separator": ["##....##", "##....##", "##....##", "##....##", "###..###", ".######.", "..####..", "........"],
    "blast_furnace": ["...##...", "..####..", ".######.", "########", "#......#", "#.####.#", "#.####.#", "########"],
    "oxidation_converter": ["...##...", "..#..#..", ".#....#.", ".#....#.", ".#....#.", "..#..#..", "...##...", "..####.."],
    "thermal_retort": ["########", "#......#", "#.####.#", "#.#..#.#", "#.####.#", "#......#", "########", "..####.."],
    "pressure_autoclave": ["..####..", ".######.", "########", "##o##o##", "########", "########", ".######.", "..####.."],
    "phase_extractor": ["#######.", "#.....#.", "#######.", "..#.....", "..#####.", "..#...#.", "..#####.", "........"],
    "precipitation_array": ["#..#..#.", "#..#..#.", "#..#..#.", "#..#..#.", "o..o..o.", "o..o..o.", ".o..o..o", "........"],
    "electrorefining_cell": ["#......#", "#......#", "#..oo..#", "#..oo..#", "#..oo..#", "########", "########", "........"],
    "molten_salt_electrolyzer": ["..#..#..", "..#..#..", "########", "#oooooo#", "#oooooo#", "#oooooo#", "########", "........"],
    "vacuum_outgasser": ["########", "#......#", "#.####.#", "#.#..#.#", "#.####.#", "#......#", "########", "...##..."],
    "arc_remelter": ["...##...", "...##...", "..o##o..", ".o.##.o.", "...##...", "########", "#......#", "########"],
    "fractionation_column": ["..####..", "..#..#..", "########", "..#..#..", "########", "..#..#..", "########", "..####.."],
    "volatile_vaporizer": ["..o..o..", ".o..o..o", "..o..o..", "########", "#......#", "#.o..o.#", "#......#", "########"],
    "vapor_deposition_furnace": ["........", "..####..", ".#....#.", ".#.##.#.", ".#.##.#.", ".#....#.", "########", "........"],
    "crystal_puller": ["...##...", "...##...", "...##...", "..####..", ".######.", ".######.", "..####..", "........"],
    "graphitizer": ["########", "#.#.#.##", "########", "##.#.#.#", "########", "#.#.#.##", "########", "........"],
    "centrifuge_cascade": [".#.#.#.#", ".#.#.#.#", ".#.#.#.#", ".#.#.#.#", ".#.#.#.#", ".#.#.#.#", "########", "........"],
    "hot_cell": ["########", "#......#", "#.o..o.#", "#..oo..#", "#..oo..#", "#.o..o.#", "#......#", "########"],
}


def kind_front(name, tier, lit_state, index):
    image = machine_panel(112, random.Random(200 + index))
    fill_rect(image, 3, 3, 13, 13, (36, 36, 40, 255))
    fill_rect(image, 3, 2, 13, 3, TIER_COLORS[tier] + (255,))
    glow = (236, 220, 140, 255) if lit_state else (150, 150, 158, 255)
    accent = (232, 137, 43, 255) if lit_state else (110, 100, 96, 255)
    rows = GLYPHS[name]
    for gy, row in enumerate(rows):
        for gx, ch in enumerate(row):
            px, py = 4 + gx * 1, 4 + gy * 1
            if ch == "#" and px < 12 and py < 13:
                image.putpixel((px, py), glow)
            elif ch == "o" and px < 12 and py < 13:
                image.putpixel((px, py), accent)
    return image


def tier_panel(base, rgb, rng, rivets=True, band=True):
    """A machine panel tinted toward the tier color, with a band and corner rivets."""
    image = machine_panel(base, rng)
    for y in range(SIZE):
        for x in range(SIZE):
            r, g, b, a = image.getpixel((x, y))
            k = 0.22
            image.putpixel((x, y), (int(r * (1 - k) + rgb[0] * k), int(g * (1 - k) + rgb[1] * k),
                                    int(b * (1 - k) + rgb[2] * k), 255))
    if band:
        fill_rect(image, 1, 13, 15, 15, rgb + (255,))
    if rivets:
        for x, y in ((2, 2), (13, 2), (2, 11), (13, 11)):
            image.putpixel((x, y), (200, 200, 208, 255))
    return image


def guide_book():
    image = blank()
    fill_rect(image, 3, 2, 13, 14, (40, 92, 58, 255))
    fill_rect(image, 3, 2, 4, 14, (24, 60, 38, 255))
    fill_rect(image, 12, 2, 13, 14, (60, 120, 80, 255))
    fill_rect(image, 5, 4, 11, 5, (214, 172, 70, 255))
    fill_rect(image, 6, 7, 10, 8, (214, 172, 70, 255))
    fill_rect(image, 6, 9, 10, 10, (214, 172, 70, 255))
    fill_rect(image, 4, 13, 12, 14, (232, 220, 188, 255))
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

    for name, factory in STAGE_TEXTURES.items():
        save(factory(), ITEM_DIR / f"{name}.png")
    for name, factory in REAGENT_TEXTURES.items():
        save(factory(), ITEM_DIR / f"{name}.png")
    save(guide_book(), ITEM_DIR / "refinery_guide.png")
    for tier in range(9):
        rgb = (TIER_COLORS[tier] if tier else (124, 135, 150))
        save(tier_panel(128, rgb, random.Random(300 + tier), rivets=False, band=False), BLOCK_DIR / f"machine_top_t{tier}.png")
        save(tier_panel(104, rgb, random.Random(320 + tier)), BLOCK_DIR / f"machine_side_t{tier}.png")
        save(tier_panel(88, rgb, random.Random(340 + tier), rivets=False, band=False), BLOCK_DIR / f"machine_bottom_t{tier}.png")
    for index, (name, tier) in enumerate(KIND_TIERS.items()):
        save(kind_front(name, tier, False, index), BLOCK_DIR / f"{name}_front.png")
        save(kind_front(name, tier, True, index), BLOCK_DIR / f"{name}_front_on.png")


if __name__ == "__main__":
    main()
