"""Draws the mod's own grayscale ore textures (16x16). The game tints them with each ore's colour, so one drawing
serves every ore. Light grays become the ore's colour; darker grays its shading.

    python3 tools/ore_builder/textures.py <assets/pipican/textures>
"""
import os
import random
import sys

from PIL import Image, ImageDraw

S = 16


def _img():
    return Image.new('RGBA', (S, S), (0, 0, 0, 0))


def _g(v, a=255):
    return (v, v, v, a)


def ore_overlay_metal():
    """Metal flecks: small shaded clusters, transparent around them (the stone shows through)."""
    im = _img()
    px = im.load()
    clusters = [(3, 3), (10, 2), (12, 8), (5, 9), (8, 13), (1, 13)]
    shapes = [[(0, 0), (1, 0), (0, 1), (1, 1), (2, 1)], [(0, 0), (1, 0), (1, 1)], [(0, 0), (0, 1), (1, 1), (1, 2), (2, 1)]]
    for i, (cx, cy) in enumerate(clusters):
        for j, (dx, dy) in enumerate(shapes[i % len(shapes)]):
            x, y = cx + dx, cy + dy
            if 0 <= x < S and 0 <= y < S:
                px[x, y] = _g(255 if j == 0 else (215 if j % 2 else 170))
        # a dark edge below/right for depth
        x, y = cx + 2, cy + 2
        if x < S and y < S and px[x, y][3] == 0:
            px[x, y] = _g(110)
    return im


def ore_overlay_gem():
    """Gem crystals: little diamonds with a bright facet."""
    im = _img()
    px = im.load()
    for cx, cy in [(4, 4), (11, 3), (12, 10), (5, 11), (9, 7)]:
        for dx, dy, v in [(0, -1, 255), (-1, 0, 230), (0, 0, 205), (1, 0, 160), (0, 1, 130)]:
            x, y = cx + dx, cy + dy
            if 0 <= x < S and 0 <= y < S:
                px[x, y] = _g(v)
    return im


def storage_metal():
    """A block of metal: bevelled edge and an inset panel."""
    im = Image.new('RGBA', (S, S), _g(200))
    d = ImageDraw.Draw(im)
    d.line([(0, 0), (15, 0)], fill=_g(250))
    d.line([(0, 0), (0, 15)], fill=_g(250))
    d.line([(0, 15), (15, 15)], fill=_g(120))
    d.line([(15, 0), (15, 15)], fill=_g(120))
    d.rectangle([3, 3, 12, 12], outline=_g(160))
    d.line([(4, 4), (11, 4)], fill=_g(230))
    for x, y in [(1, 1), (14, 1), (1, 14), (14, 14)]:
        im.putpixel((x, y), _g(240))
    return im


def storage_gem():
    """A block of gem: faceted diagonals."""
    im = Image.new('RGBA', (S, S), _g(195))
    px = im.load()
    for y in range(S):
        for x in range(S):
            if (x + y) % 8 == 0:
                px[x, y] = _g(245)
            elif (x - y) % 8 == 0:
                px[x, y] = _g(150)
    for i in range(S):
        px[i, 0] = _g(240)
        px[0, i] = _g(240)
        px[i, 15] = _g(125)
        px[15, i] = _g(125)
    return im


def raw_block():
    """A block of raw ore: lumpy, uneven shades."""
    rnd = random.Random(7)
    im = Image.new('RGBA', (S, S), _g(170))
    px = im.load()
    for y in range(S):
        for x in range(S):
            px[x, y] = _g(rnd.choice([140, 160, 175, 190, 205, 225]))
    for cx, cy in [(4, 4), (11, 5), (6, 11), (12, 12)]:
        for dx in (-1, 0, 1):
            for dy in (-1, 0, 1):
                px[(cx + dx) % S, (cy + dy) % S] = _g(235 if dx + dy < 0 else 200)
    return im


def ingot():
    """An ingot: light top face, mid front, dark side."""
    im = _img()
    d = ImageDraw.Draw(im)
    d.polygon([(2, 8), (9, 4), (14, 7), (7, 11)], fill=_g(240))
    d.polygon([(2, 8), (7, 11), (7, 13), (2, 10)], fill=_g(175))
    d.polygon([(7, 11), (14, 7), (14, 9), (7, 13)], fill=_g(130))
    d.line([(4, 8), (9, 5)], fill=_g(255))
    return im


def raw():
    """A raw chunk: a rough lump."""
    rnd = random.Random(3)
    im = _img()
    px = im.load()
    for y in range(3, 14):
        for x in range(3, 14):
            r = ((x - 8) ** 2) / 26 + ((y - 8.5) ** 2) / 22
            if r <= 1.0 and not (r > 0.75 and rnd.random() < 0.35):
                v = 225 if x + y < 14 else (185 if x + y < 19 else 140)
                px[x, y] = _g(v + rnd.choice([-15, 0, 0, 15]))
    return im


def gem():
    """A cut gem: four facets and a glint."""
    im = _img()
    d = ImageDraw.Draw(im)
    d.polygon([(8, 2), (13, 7), (8, 8)], fill=_g(215))
    d.polygon([(8, 2), (3, 7), (8, 8)], fill=_g(250))
    d.polygon([(3, 7), (8, 14), (8, 8)], fill=_g(175))
    d.polygon([(13, 7), (8, 14), (8, 8)], fill=_g(135))
    im.putpixel((6, 5), _g(255))
    return im


TEXTURES = {
    'block/ore_overlay_metal': ore_overlay_metal,
    'block/ore_overlay_gem': ore_overlay_gem,
    'block/storage_metal': storage_metal,
    'block/storage_gem': storage_gem,
    'block/raw_ore_block': raw_block,
    'item/ingot': ingot,
    'item/raw_ore': raw,
    'item/gem': gem,
}


def write_all(root):
    for name, draw in TEXTURES.items():
        path = os.path.join(root, name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        draw().save(path)


if __name__ == '__main__':
    write_all(sys.argv[1] if len(sys.argv) > 1 else 'src/main/resources/assets/pipican/textures')
