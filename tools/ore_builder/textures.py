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


# ---------------------------------------------------------------- gear: tools and weapons are two layers - a wooden
# handle that keeps its colour (layer 0) and a grayscale head the game tints (layer 1).
WOOD = [(0x6B, 0x4A, 0x2B, 255), (0x8A, 0x63, 0x3A, 255), (0x4A, 0x32, 0x1C, 255)]


def _handle(points):
    im = _img()
    for n, (x, y) in enumerate(points):
        im.putpixel((x, y), WOOD[n % 2])
        if 0 <= x + 1 < S and 0 <= y + 1 < S:
            im.putpixel((x + 1, y + 1), WOOD[2]) if im.getpixel((x + 1, y + 1))[3] == 0 else None
    return im


def _diag(x0, y0, n):
    """n pixels going up-right from (x0, y0)."""
    return [(x0 + i, y0 - i) for i in range(n)]


def handle_long():
    return _handle(_diag(2, 13, 9))


def handle_short():
    return _handle(_diag(2, 13, 5))


def sword_head():
    im = _img()
    for i in range(9):
        x, y = 6 + i, 9 - i
        im.putpixel((x, y), _g(250))
        if x - 1 >= 0:
            im.putpixel((x - 1, y), _g(205))
        if y + 1 < S:
            im.putpixel((x, y + 1), _g(150))
    for x, y in [(4, 9), (5, 10), (6, 11), (7, 12), (3, 8), (8, 13)]:
        im.putpixel((x, y), _g(185))  # the guard
    im.putpixel((14, 1), _g(255))
    return im


def axe_head():
    im = _img()
    d = ImageDraw.Draw(im)
    d.polygon([(8, 2), (13, 3), (14, 8), (10, 9), (8, 6)], fill=_g(205))
    d.line([(9, 3), (13, 4)], fill=_g(250))
    d.line([(14, 5), (14, 8)], fill=_g(250))
    d.line([(8, 6), (10, 9)], fill=_g(140))
    return im


def pickaxe_head():
    im = _img()
    pts = [(3, 3), (4, 2), (5, 2), (6, 2), (7, 2), (8, 2), (9, 2), (10, 3), (11, 3), (12, 4), (13, 5), (13, 6), (14, 7), (14, 8)]
    for n, (x, y) in enumerate(pts):
        im.putpixel((x, y), _g(250 if n % 3 == 0 else 210))
        if y + 1 < S:
            im.putpixel((x, y + 1), _g(150))
    return im


def shovel_head():
    im = _img()
    d = ImageDraw.Draw(im)
    d.polygon([(9, 3), (12, 1), (14, 3), (12, 6), (10, 6)], fill=_g(210))
    d.line([(12, 1), (14, 3)], fill=_g(250))
    d.line([(10, 6), (12, 6)], fill=_g(145))
    return im


def hoe_head():
    im = _img()
    for x, y, v in [(7, 3, 205), (8, 3, 250), (9, 3, 250), (10, 3, 210), (11, 4, 210), (12, 5, 180), (7, 4, 150), (8, 4, 150)]:
        im.putpixel((x, y), _g(v))
    return im


def spear_head():
    im = _img()
    for x, y, v in [(12, 3, 255), (13, 2, 255), (14, 1, 240), (11, 4, 220), (12, 2, 200), (13, 3, 160), (11, 2, 180), (13, 4, 140)]:
        im.putpixel((x, y), _g(v))
    return im


def handle_spear():
    return _handle(_diag(1, 14, 10))


def _armor(shape):
    """An armor icon from rows of characters: '#' plate, '+' highlight, '-' shadow."""
    im = _img()
    top = (S - len(shape)) // 2
    for y, row in enumerate(shape):
        left = (S - len(row)) // 2
        for x, ch in enumerate(row):
            v = {'#': 200, '+': 245, '-': 140}.get(ch)
            if v:
                im.putpixel((left + x, top + y), _g(v))
    return im


def helmet():
    return _armor(['  ++++++  ', ' +######- ', '+########-', '##      ##', '##      ##', '-#      #-'])


def chestplate():
    return _armor(['++++  ++++', '+###++###-', '##########', ' +######- ', ' ######## ', ' ######## ', ' +######- ',
                   ' -######- ', ' -------- '])


def leggings():
    return _armor(['++++++++', '########', '###--###', '##-  -##', '##    ##', '##    ##', '##    ##', '-#    #-'])


def boots():
    return _armor(['+#    #+', '##    ##', '##    ##', '###  ###', '-##  ##-'])


def armor_layer(weight, leggings_layer):
    """The worn armor (64x32, the player model's layout), in three weights: light, medium, heavy."""
    w, h = 64, 32
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    px = im.load()
    rnd = random.Random(11)
    if leggings_layer:
        boxes = [(0, 16, 16, 32), (16, 16, 40, 32)]           # legs, waist
    else:
        boxes = [(0, 0, 32, 16), (16, 16, 40, 32), (40, 16, 56, 32), (0, 16, 16, 32)]  # head, body, arms, legs (boots)
    plate = {'light': 6, 'medium': 4, 'heavy': 3}[weight]
    for x0, y0, x1, y1 in boxes:
        for y in range(y0, y1):
            for x in range(x0, x1):
                v = 195 + rnd.choice([-10, 0, 0, 10])
                if (x - x0) % plate == 0 or (y - y0) % plate == 0:
                    v = 230 if weight != 'light' else 210        # plate seams catch the light
                if (x - x0) % plate == plate - 1:
                    v = 150
                if weight == 'heavy' and (x - x0) % plate == 1 and (y - y0) % plate == 1:
                    v = 250                                       # rivets
                px[x, y] = _g(v)
    # Leave the face of the helmet open.
    if not leggings_layer:
        for y in range(8, 16):
            for x in range(10, 14):
                px[x, y] = (0, 0, 0, 0)
    return im


GEAR = {
    'item/gear/handle_long': handle_long, 'item/gear/handle_short': handle_short, 'item/gear/handle_spear': handle_spear,
    'item/gear/sword': sword_head, 'item/gear/axe': axe_head, 'item/gear/pickaxe': pickaxe_head,
    'item/gear/shovel': shovel_head, 'item/gear/hoe': hoe_head, 'item/gear/spear': spear_head,
    'item/gear/helmet': helmet, 'item/gear/chestplate': chestplate, 'item/gear/leggings': leggings, 'item/gear/boots': boots,
}
for _w in ('light', 'medium', 'heavy'):
    GEAR[f'entity/equipment/humanoid/plate_{_w}'] = (lambda w: lambda: armor_layer(w, False))(_w)
    GEAR[f'entity/equipment/humanoid_leggings/plate_{_w}'] = (lambda w: lambda: armor_layer(w, True))(_w)


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
    for name, draw in {**TEXTURES, **GEAR}.items():
        path = os.path.join(root, name + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        draw().save(path)


if __name__ == '__main__':
    write_all(sys.argv[1] if len(sys.argv) > 1 else 'src/main/resources/assets/pipican/textures')
