"""Deterministic texture generator for Universal Ore Processing.

Every PNG under assets/universaloreprocessing/textures is produced here from
geometry and fixed palettes; nothing is random. Run from the repository root:

    python tools/textures/generate.py

Ladder forms are drawn as faceted shapes lit from the top left, quantised to
a six step value ramp, then split into three grayscale layers (shadow, mid,
light). Each layer has its own tint index in the item model, so the client
applies a hue-shifted ramp per band instead of one flat multiply.
"""
import math
from pathlib import Path

from PIL import Image

SIZE = 16
ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "src/main/resources/assets/universaloreprocessing/textures"
ITEM_DIR = ASSETS / "item"
BLOCK_DIR = ASSETS / "block"

# six step grayscale ramp for tinted bases; index 0 is transparent
GRAY = [None, 46, 84, 122, 162, 204, 242]
# which tint layer each ramp step belongs to
BAND = {1: 0, 2: 0, 3: 1, 4: 1, 5: 2, 6: 2}
LIGHT = (-0.6, -0.8)  # from the top left, slightly more from above, as in vanilla


# ---------------------------------------------------------------------------
# Grid helpers
# ---------------------------------------------------------------------------

def grid(fill=0):
    return [[fill] * SIZE for _ in range(SIZE)]


def neighbours(x, y):
    for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
        nx, ny = x + dx, y + dy
        if 0 <= nx < SIZE and 0 <= ny < SIZE:
            yield nx, ny


def inside_triangle(px, py, a, b, c):
    def sign(p1, p2, p3):
        return (p1[0] - p3[0]) * (p2[1] - p3[1]) - (p2[0] - p3[0]) * (p1[1] - p3[1])

    p = (px, py)
    d1, d2, d3 = sign(p, a, b), sign(p, b, c), sign(p, c, a)
    neg = d1 < 0 or d2 < 0 or d3 < 0
    pos = d1 > 0 or d2 > 0 or d3 > 0
    return not (neg and pos)


def facet_value(a, b, apex, lo, hi):
    """Ramp step of the facet a-b-apex, from the outward normal of edge a-b against the light."""
    ex, ey = b[0] - a[0], b[1] - a[1]
    nx, ny = ey, -ex
    mx, my = (a[0] + b[0]) / 2 - apex[0], (a[1] + b[1]) / 2 - apex[1]
    if nx * mx + ny * my < 0:
        nx, ny = -nx, -ny
    length = math.hypot(nx, ny) or 1.0
    lit = (nx * LIGHT[0] + ny * LIGHT[1]) / length  # -1 facing away, 1 facing the light
    step = lo + (lit + 1.0) / 2.0 * (hi - lo)
    return max(lo, min(hi, int(round(step))))


def draw_faceted(values, owner, ident, points, apex, lo=2, hi=6):
    """Rasterises a convex polygon split into facets around an off-centre apex."""
    tris = []
    for i, a in enumerate(points):
        b = points[(i + 1) % len(points)]
        tris.append((a, b, facet_value(a, b, apex, lo, hi)))
    for y in range(SIZE):
        for x in range(SIZE):
            px, py = x + 0.5, y + 0.5
            for a, b, value in tris:
                if inside_triangle(px, py, a, b, apex):
                    values[y][x] = value
                    owner[y][x] = ident
                    break


def outline_shadow_sides(values, owner):
    """Darkest step on edges facing right or down, including where a shape overlaps one behind it."""
    out = [row[:] for row in values]
    for y in range(SIZE):
        for x in range(SIZE):
            if not values[y][x]:
                continue
            for nx, ny in ((x + 1, y), (x, y + 1)):
                outside = not (0 <= nx < SIZE and 0 <= ny < SIZE) or not values[ny][nx]
                if outside or owner[ny][nx] < owner[y][x]:
                    out[y][x] = 1
    # a shape in front casts a one pixel shadow onto the shape behind its lower right edge
    for y in range(SIZE):
        for x in range(SIZE):
            if values[y][x] and out[y][x] != 1:
                for nx, ny in ((x - 1, y), (x, y - 1)):
                    if 0 <= nx < SIZE and 0 <= ny < SIZE and owner[ny][nx] > owner[y][x]:
                        out[y][x] = min(out[y][x], 2)
    return out


def remove_orphans(values):
    """A pixel whose step matches none of its opaque neighbours takes the most common one."""
    out = [row[:] for row in values]
    for y in range(SIZE):
        for x in range(SIZE):
            v = values[y][x]
            if not v or v == 1:
                continue
            near = [values[ny][nx] for nx, ny in neighbours(x, y) if values[ny][nx]]
            if near and v not in near:
                near = [n for n in near if n != 1] or near
                out[y][x] = max(set(near), key=lambda n: (near.count(n), n))
    return out


def drop_isolated(values):
    """Removes opaque pixels with no opaque 4-neighbour."""
    out = [row[:] for row in values]
    for y in range(SIZE):
        for x in range(SIZE):
            if values[y][x] and not any(values[ny][nx] for nx, ny in neighbours(x, y)):
                out[y][x] = 0
    return out


def finish(values, owner):
    values = drop_isolated(values)
    values = outline_shadow_sides(values, owner)
    return remove_orphans(values)


# ---------------------------------------------------------------------------
# Ladder form shapes
# ---------------------------------------------------------------------------

def clumps():
    """Three angular, broken chunks: one large at the back, two smaller in front."""
    values, owner = grid(), grid(-1)
    draw_faceted(values, owner, 0,
                 [(1.5, 6.5), (5.5, 2.0), (10.5, 3.5), (11.5, 8.0), (7.5, 10.5), (2.5, 10.0)], (5.0, 5.0), lo=3)
    draw_faceted(values, owner, 1,
                 [(9.0, 9.5), (12.0, 6.5), (15.0, 9.0), (14.0, 13.0), (9.5, 13.0)], (11.0, 8.5), lo=3)
    draw_faceted(values, owner, 2,
                 [(2.5, 12.5), (5.5, 9.0), (9.5, 11.0), (8.5, 15.0), (3.5, 15.0)], (5.0, 11.0), lo=3)
    return finish(values, owner)


def dust():
    """A soft pile: a low mound whose facets turn gradually, with a small heap beside it."""
    values, owner = grid(), grid(-1)
    pile = []
    for i in range(13):
        t = math.pi * i / 12
        pile.append((8.0 - 6.5 * math.cos(t), 13.5 - 8.0 * math.sin(t)))
    pile.append((8.0, 14.5))
    draw_faceted(values, owner, 0, pile, (6.8, 9.0), lo=2, hi=6)
    heap = []
    for i in range(7):
        t = math.pi * i / 6
        heap.append((12.5 - 2.5 * math.cos(t), 14.5 - 3.0 * math.sin(t)))
    draw_faceted(values, owner, 1, heap, (12.0, 13.0), lo=2, hi=5)
    return finish(values, owner)


def crystal(cx, base, tip, half, lean):
    """An elongated hexagonal prism standing on base y, leaning by lean pixels at its tip."""
    tx = cx + lean
    return [
        (cx - half, base), (cx - half + lean * 0.7, tip + 2.5), (tx, tip),
        (cx + half + lean * 0.7, tip + 2.5), (cx + half, base), (cx, base + 1.0),
    ], (cx - half * 0.35 + lean * 0.5, (base + tip) / 2)


def shards():
    """Three clear faceted crystals of different heights."""
    values, owner = grid(), grid(-1)
    for ident, (cx, base, tip, half, lean) in enumerate([
            (7.5, 14.0, 1.5, 2.5, 0.5),
            (3.5, 14.0, 6.0, 1.8, -1.5),
            (12.0, 14.5, 5.0, 2.0, 1.5)]):
        points, apex = crystal(cx, base, tip, half, lean)
        draw_faceted(values, owner, ident, points, apex, lo=3, hi=6)
    values = finish(values, owner)
    # a clear crystal keeps its brightest step along the lit edge
    for y in range(SIZE):
        for x in range(1, SIZE):
            if values[y][x] == 5 and not values[y][x - 1]:
                values[y][x] = 6
    return values


def write_layers(name, values):
    for band in range(3):
        image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
        for y in range(SIZE):
            for x in range(SIZE):
                v = values[y][x]
                if v and BAND[v] == band:
                    g = GRAY[v]
                    image.putpixel((x, y), (g, g, g, 255))
        image.save(ITEM_DIR / f"{name}_{band}.png")


# ---------------------------------------------------------------------------
# Full colour textures: hue-shifted ramps (warm highlights, cool shadows)
# ---------------------------------------------------------------------------

def ramp(hue, sat, steps=6, shift=18.0, lo=0.22, hi=0.92):
    """Colour ramp from shadow to highlight; hue rotates toward blue in shadow and yellow in light."""
    import colorsys
    colours = []
    for i in range(steps):
        t = i / (steps - 1)
        h = (hue + (t - 0.5) * -shift * 2) % 360 if hue > 60 else (hue + (t - 0.5) * shift) % 360
        s = sat * (1.15 - 0.45 * t)
        v = lo + (hi - lo) * t
        r, g, b = colorsys.hsv_to_rgb(h / 360.0, min(1.0, s), v)
        colours.append((int(r * 255), int(g * 255), int(b * 255), 255))
    return colours


STONE = ramp(225, 0.10, lo=0.25, hi=0.80)
WOOD = ramp(28, 0.55, lo=0.20, hi=0.78)
WOOD_DARK = ramp(22, 0.60, lo=0.14, hi=0.52)
HANDLE = ramp(30, 0.55, lo=0.24, hi=0.74)


def paint(rows, palette):
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, ch in enumerate(row):
            if ch != ".":
                image.putpixel((x, y), palette[int(ch) - 1])
    return image


def stone_side():
    # two pixel lip at the top (the rim), a chiselled body, a bevelled foot
    rows = []
    for y in range(SIZE):
        row = ""
        for x in range(SIZE):
            if y == 0:
                v = 6
            elif y == 1:
                v = 4 if x < SIZE - 1 else 3
            elif y == 2:
                v = 1
            elif y == SIZE - 1:
                v = 1
            elif y == SIZE - 2:
                v = 2
            else:
                # large ashlar blocks: a vertical joint offset by course
                joint = 5 if y < 9 else 11
                if y == 9:
                    v = 2
                elif x == joint:
                    v = 2
                elif x == joint + 1 or y in (3, 10):
                    v = 5
                else:
                    v = 4
            row += str(v)
        rows.append(row)
    return paint(rows, STONE)


def bevel(x, y, flat, chips=()):
    """Flat face with a lit top-left edge, a shaded bottom-right edge and a few deliberate chips."""
    if x == 0 or y == 0:
        return 6
    if x == SIZE - 1 or y == SIZE - 1:
        return 2
    for cx, cy in chips:
        if (x, y) == (cx, cy):
            return flat - 1
        if (x, y) == (cx - 1, cy) or (x, y) == (cx, cy - 1):
            return flat + 1
    return flat


def stone_rim():
    chips = ((5, 3), (11, 9), (3, 12))
    return paint(["".join(str(bevel(x, y, 5, chips)) for x in range(SIZE)) for y in range(SIZE)], STONE)


def stone_floor():
    # the striking floor sits two pixels below the rim, so the rim shades its top and left edges
    rows = []
    chips = ((6, 6), (10, 8), (8, 11), (12, 12), (4, 10))
    for y in range(SIZE):
        row = ""
        for x in range(SIZE):
            if x < 2 or y < 2:
                v = 2
            elif x == 2 or y == 2:
                v = 3
            else:
                v = 4
                for cx, cy in chips:
                    if (x, y) == (cx, cy):
                        v = 3
                    elif (x, y) in ((cx - 1, cy), (cx, cy - 1)):
                        v = 5
            row += str(v)
        rows.append(row)
    return paint(rows, STONE)


def stone_bottom():
    return paint(["".join(str(2 if (x in (0, SIZE - 1) or y in (0, SIZE - 1)) else 3) for x in range(SIZE))
                  for y in range(SIZE)], STONE)


# vanilla destroy stages grow a branching crack from the centre outward; these do the same
CRACK_PATHS = [
    [(7, 7), (8, 7), (8, 8), (9, 9)],
    [(6, 6), (5, 5), (5, 4), (10, 9), (11, 10), (9, 10), (9, 11)],
    [(4, 4), (3, 3), (7, 8), (6, 9), (5, 10), (11, 11), (12, 12), (10, 6), (11, 5)],
    [(2, 3), (12, 4), (13, 3), (4, 11), (3, 12), (12, 13), (13, 13), (8, 12), (8, 13), (6, 3), (6, 2)],
]


def crack(stage):
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for i in range(stage):
        for x, y in CRACK_PATHS[i]:
            image.putpixel((x, y), STONE[0])
            # the lit lip of the crack sits up and to the left of the dark line
            lx, ly = x - 1, y - 1
            if image.getpixel((lx, ly))[3] == 0:
                image.putpixel((lx, ly), STONE[1])
    return image


def planks():
    rows = []
    for y in range(SIZE):
        row = ""
        board = y // 4
        seam = (3, 11, 7, 13)[board]
        for x in range(SIZE):
            if y % 4 == 3:
                v = 1
            elif y % 4 == 0:
                v = 5
            elif x == seam:
                v = 2
            elif x == seam + 1:
                v = 5 if y % 4 == 1 else 4
            else:
                v = 4 if (x + board * 5) % 6 else 3
            row += str(v)
        rows.append(row)
    return paint(rows, WOOD)


def riffle():
    # a dark hardwood bar: lit top edge, plain body with two grain lines, shaded underside, nails at each end
    rows = []
    for y in range(SIZE):
        row = ""
        for x in range(SIZE):
            if y == 0:
                v = 6
            elif y == SIZE - 1:
                v = 1
            elif y == SIZE - 2:
                v = 2
            elif (x, y % 8) in ((2, 4), (13, 4)):
                v = 1
            elif (y == 5 and 4 <= x <= 9) or (y == 11 and 7 <= x <= 12):
                v = 3
            else:
                v = 4
            row += str(v)
        rows.append(row)
    return paint(rows, WOOD_DARK)


def gray_image(rows):
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for y, row in enumerate(rows):
        for x, v in enumerate(row):
            if v:
                g = GRAY[v]
                image.putpixel((x, y), (g, g, g, 255))
    return image


def slurry_still():
    # calm surface: one flat value with a few short ripples, each lit above and shaded below
    values = [[4] * SIZE for _ in range(SIZE)]
    for x0, y, length in ((2, 3, 4), (9, 5, 5), (4, 9, 3), (10, 11, 4), (1, 13, 3)):
        for x in range(x0, min(SIZE, x0 + length)):
            values[y][x] = 5
            if y + 1 < SIZE and x > x0:
                values[y + 1][x] = 3
        values[y][x0] = 6
    return gray_image(values)


def slurry_murky():
    # unstirred: settled dust swirled in loose arcs, darker than the still surface
    rows = []
    for y in range(SIZE):
        row = []
        for x in range(SIZE):
            r = math.hypot(x - 7.5, y - 7.5)
            arc = int(r + math.atan2(y - 7.5, x - 7.5) * 1.6) % 6
            row.append({0: 4, 1: 3, 2: 3, 3: 2, 4: 3, 5: 3}[arc])
        rows.append(row)
    return gray_image(remove_orphans(rows))


# crystal clusters on the surface: (x, y) of each crystal base; later stages add crystals and height
CLUSTERS = [(4, 6), (11, 4), (8, 11), (12, 12), (3, 12)]


def slurry_crystals(stage):
    values = grid()
    count = (2, 4, 5)[stage - 1]
    for index, (cx, cy) in enumerate(CLUSTERS[:count]):
        height = stage + 1 - (1 if index >= 2 and stage > 1 else 0)
        # a two pixel wide crystal: lit left column, shaded right column, a bright point on top
        for i in range(height):
            y = cy - i
            values[y][cx] = 5 if i < height - 1 else 6
            values[y][cx + 1] = 3 if i < height - 1 else 4
        values[cy][cx + 1] = 2
        values[cy - height][cx] = 6
    return gray_image(values)


def hammer():
    """A stone hammer in the vanilla tool pose: handle from the lower left, head across the upper right."""
    head = ramp(220, 0.08, lo=0.24, hi=0.84)
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for i in range(10):
        x, y = 1 + i, 14 - i
        image.putpixel((x, y), HANDLE[3] if i % 3 else HANDLE[4])
        if i < 8:
            image.putpixel((x + 1, y), HANDLE[1])
    for y in range(SIZE):
        for x in range(SIZE):
            u = (x - 11) + (y - 4)   # along the head, top left to bottom right
            w = (x - 11) - (y - 4)   # across the head
            if -5 <= u <= 5 and -3 <= w <= 1:
                if u == 5 or w == -3:
                    step = 0
                elif u == -5 or w == 1:
                    step = 5 if u < 3 else 3
                else:
                    step = 4 if u < 0 else 3 if u < 3 else 2
                image.putpixel((x, y), head[step])
    return image


def panning_tray():
    """A shallow wooden pan seen from above at an angle: a lit far rim, a darker bowl, a shaded near lip."""
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    cx, cy, rx, ry = 7.5, 8.5, 7.2, 5.2
    for y in range(SIZE):
        for x in range(SIZE):
            d = ((x - cx) / rx) ** 2 + ((y - cy) / ry) ** 2
            if d > 1.0:
                continue
            inner = ((x - cx) / (rx - 1.6)) ** 2 + ((y - cy + 0.6) / (ry - 1.4)) ** 2
            if inner > 1.0:
                # rim: lit along the far edge, shaded along the near edge
                step = 5 if y < cy else 2 if y > cy + 1 else 3
                if d > 0.86 and y > cy:
                    step = 1
            else:
                # bowl floor: darker toward the far wall, a few concentric grooves
                ring = int(inner * 3)
                step = 3 if ring % 2 else 4
                if y < cy - 2:
                    step -= 1
            image.putpixel((x, y), WOOD[step])
    return image


def guide():
    """A bound book: a dark leather cover with a stone-grey clasp band and a small ore emblem."""
    leather = ramp(14, 0.55, lo=0.16, hi=0.58)
    paper = ramp(42, 0.22, lo=0.62, hi=0.95)
    emblem = ramp(30, 0.70, lo=0.35, hi=0.95)
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    for y in range(1, 15):
        for x in range(2, 14):
            if x == 13 or y == 14:
                colour = paper[2] if (x + y) % 2 else paper[1]
            elif x == 2:
                colour = leather[0]
            elif y == 1 or x == 3:
                colour = leather[4]
            elif x == 12 or y == 13:
                colour = leather[1]
            else:
                colour = leather[2] if (x * 3 + y) % 7 else leather[3]
            image.putpixel((x, y), colour)
    for y in range(2, 14):
        image.putpixel((10, y), STONE[3] if y % 3 else STONE[4])
    for x, y, step in ((6, 6, 5), (7, 6, 4), (6, 7, 4), (7, 7, 3), (8, 7, 2), (7, 8, 2), (5, 7, 3), (6, 8, 1)):
        image.putpixel((x, y), emblem[step])
    return image


def preview(path, images, scale=12):
    sheet = Image.new("RGBA", (len(images) * (SIZE * scale + 4), SIZE * scale), (40, 44, 52, 255))
    for i, image in enumerate(images):
        big = image.resize((SIZE * scale, SIZE * scale), Image.NEAREST)
        sheet.alpha_composite(big, (i * (SIZE * scale + 4), 0))
    sheet.save(path)


def main():
    ITEM_DIR.mkdir(parents=True, exist_ok=True)
    BLOCK_DIR.mkdir(parents=True, exist_ok=True)

    for name, values in (("clumps", clumps()), ("dust", dust()), ("shards", shards())):
        write_layers(name, values)
    hammer().save(ITEM_DIR / "hammer.png")
    panning_tray().save(ITEM_DIR / "panning_tray.png")
    guide().save(ITEM_DIR / "guide.png")

    stone_side().save(BLOCK_DIR / "crushing_slab_side.png")
    stone_rim().save(BLOCK_DIR / "crushing_slab_rim.png")
    stone_floor().save(BLOCK_DIR / "crushing_slab_floor.png")
    stone_bottom().save(BLOCK_DIR / "crushing_slab_bottom.png")
    for stage in range(1, 5):
        crack(stage).save(BLOCK_DIR / f"crushing_slab_crack_{stage}.png")

    planks().save(BLOCK_DIR / "sluice_planks.png")
    riffle().save(BLOCK_DIR / "sluice_riffle.png")

    slurry_still().save(BLOCK_DIR / "slurry_still.png")
    slurry_murky().save(BLOCK_DIR / "slurry_murky.png")
    for stage in range(1, 4):
        slurry_crystals(stage).save(BLOCK_DIR / f"slurry_crystals_{stage}.png")


if __name__ == "__main__":
    main()
