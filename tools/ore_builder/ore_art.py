"""Each ore's own art: ore overlay, raw chunk, ingot or gem, storage blocks, and for set ores every gear piece and the
worn armor. Replaces the old grayscale-and-tint templates: every texture is drawn with the ore's own hue-shifted
palette (pixelart.py) from the hand-authored grids in ore_sprites.py. Rules: docs/PIXEL_ART.md.

    python3 tools/ore_builder/ore_art.py [assets/pipican/textures] [--preview out.png]

Where things go (under textures/):
  block/ore/<ore>.png               overlay drawn over the stone / deepslate / netherrack / end stone base
  block/ore/<ore>_block.png         storage block (and <ore>_raw_block.png for metals)
  item/ore/<item id>.png            raw_<ore>, <ore>_ingot, or the gem <ore>
  item/gear/<set>/<kind>.png        helmet ... hoe
  entity/equipment/humanoid/<set>.png, humanoid_leggings/<set>.png   the worn armor
"""
import hashlib
import os
import sys

sys.path.insert(0, os.path.dirname(__file__))
import pixelart as px  # noqa: E402
import ore_sprites as S  # noqa: E402
import gear_sprites as S2  # noqa: E402

MAIN = set('12345gG')
ACCENT = set('abcd')
HANDLE = set('wxy')
STONE = set('pqr')
# Fixed stone matrix ramp for raw chunks: a cold, slightly violet rock.
STONE_RAMP = px.fixed('#16131b', '#2f2c37', '#4a4753', '#6a6672')


def _h(oid, salt=''):
    return int(hashlib.md5((oid + salt).encode()).hexdigest(), 16)


# ---------------------------------------------------------------- palettes
def accent_for(o):
    """The trim metal: from blackened iron / bone / aged bronze on common gear up to dark gilt on legendary gear -
    whichever of the tier's choices stands out most against the ore itself (value first, then hue)."""
    rgb = px.hexrgb('%06X' % o['color'])
    tier = o['tier']
    choices = ['gold', 'iron', 'bronze'] if tier >= 7 else ['bronze', 'iron', 'bone'] if tier >= 4 else ['iron', 'bronze', 'bone']
    L, C, h = px.rgb_to_oklch(rgb)

    def contrast(name):
        mid = px.rgb_to_oklch(px.ACCENTS[name][2])
        dh = abs((mid[2] - h + 180) % 360 - 180) / 180 * min(C, mid[1]) * 4
        return abs(mid[0] - L) * 1.5 + dh + abs(mid[1] - C) + (0.08 if name == choices[0] else 0)
    return px.ACCENTS[max(choices, key=contrast)]


def palette(o, handle='wood'):
    rgb = px.hexrgb('%06X' % o['color'])
    is_gem = o['form'] == 'gem'
    r = px.ramp(rgb, shift=34 if is_gem else 26, desat=0.95 if is_gem else 0.85)
    acc = accent_for(o)
    hd = px.LEATHER if handle == 'leather' else px.WOOD
    gl = px.glow(rgb)
    pal = {'o': px.outline(rgb), 'k': acc[0], 'h': hd[0], 's': STONE_RAMP[0], 'g': gl[0], 'G': gl[1]}
    # Lit-side outlines (vanilla's two-tone outline): halfway between the outline and the darkest fill.
    pal.update({'O': px.mix(pal['o'], r[0], 0.45), 'K': px.mix(acc[0], acc[1], 0.45), 'H': px.mix(hd[0], hd[1], 0.45),
                'S': px.mix(STONE_RAMP[0], STONE_RAMP[1], 0.45)})
    pal.update({str(i + 1): c for i, c in enumerate(r)})
    pal.update(dict(zip('abcd', acc[1:])))
    pal.update(dict(zip('wxy', hd[1:])))
    pal.update(dict(zip('pqr', STONE_RAMP[1:])))
    return pal


# ---------------------------------------------------------------- grid helpers
def outlined(grid, two_tone=True):
    """Adds a 1 px outline around every filled pixel (4-neighbours only, so stair-steps stay single). Like vanilla,
    the outline is lighter on the lit side (the shape lies to its right or below it) and near black elsewhere."""
    h, w = len(grid), len(grid[0])
    out = [list(r) for r in grid]
    filled = lambda x, y: 0 <= x < w and 0 <= y < h and grid[y][x] != '.'  # noqa: E731
    for y in range(h):
        for x in range(w):
            if grid[y][x] != '.':
                continue
            dirs = [(dx, dy) for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)) if filled(x + dx, y + dy)]
            if not dirs:
                continue
            near = [grid[y + dy][x + dx] for dx, dy in dirs]
            if any(c in MAIN or c in 'oO' for c in near):
                ch = 'o'
            elif any(c in ACCENT or c in 'kK' for c in near):
                ch = 'k'
            elif any(c in HANDLE or c in 'hH' for c in near):
                ch = 'h'
            else:
                ch = 's'
            lit = two_tone and all(d in ((1, 0), (0, 1)) for d in dirs)
            out[y][x] = ch.upper() if lit else ch
    return [''.join(r) for r in out]


def compose(*parts):
    """Layers 16x16 part grids; later parts cover earlier ones."""
    rows = [list(r) for r in parts[0]]
    for p in parts[1:]:
        for y, r in enumerate(p):
            for x, c in enumerate(r):
                if c != '.':
                    rows[y][x] = c
    return [''.join(r) for r in rows]


def put(grid, points, ch, only_filled=True):
    rows = [list(r) for r in grid]
    for x, y in points:
        if not only_filled or rows[y][x] != '.':
            rows[y][x] = ch
    return [''.join(r) for r in rows]


def no_glow(grid):
    """Low tiers: inlays become plain metal."""
    return px.remap(grid, {'g': '3', 'G': '4'})


# ---------------------------------------------------------------- per-ore choices
CABOCHON = ('pearl', 'opal', 'jade', 'amber', 'moonstone', 'sekkiseki', 'eco', 'lacrima', 'nitre')
SHARD = ('shard', 'crystal', 'glass', 'quartz', 'stalhrim', 'glacierite', 'khaydarin', 'dilithium', 'tiberium',
         'kryptonite', 'thunderstone', 'aquamarine', 'kyanite', 'zonaite')
ROUGH = ('stone', 'basalt', 'cinnabar', 'morkite', 'dust', 'kairoseki', 'abyssium', 'umbrite', 'sunforged', 'titanite',
         'sam', 'mega_stone', 'dragonite')
CLUSTER = ('aetherium', 'energon', 'hextech', 'materia', 'element_zero', 'ion_cube', 'quantanium', 'eridium',
           'raritanium', 'elerium', 'astralite', 'verdantite', 'malachite', 'tidalite', 'vespene', 'warpstone')


def gem_shape(o):
    i = o['id']
    for names, shape in ((CLUSTER, 'cluster'), (CABOCHON, 'cabochon'), (SHARD, 'shard'), (ROUGH, 'rough')):
        if any(n in i for n in names):
            return shape
    return 'cut'


def ore_layout(o):
    if o['form'] == 'gem':
        return 'geodes' if gem_shape(o) in ('cabochon', 'rough') else 'crystals'
    return 'veins' if _h(o['id'], 'layout') % 2 else 'nuggets'


def glows(o, glow_ores):
    return o['tier'] >= 7 or o['id'] in glow_ores


# ---------------------------------------------------------------- textures of one ore
def ore_overlay(o, glow_ores=()):
    name = ore_layout(o)
    grid = {'nuggets': S.NUGGETS, 'veins': S.VEINS, 'crystals': S.CRYSTALS, 'geodes': S.GEODES}[name]
    if glows(o, glow_ores):
        grid = put(grid, S.OVERLAY_GLOW[name], 'G')
    grid = outlined(grid)
    if _h(o['id'], 'flip') % 2:
        grid = px.flip(grid)
    return px.render(grid, palette(o), o['id'] + ' overlay')


def raw_item(o):
    grid = S.RAW['chunk' if _h(o['id'], 'raw') % 2 else 'lump']
    return px.render(outlined(grid), palette(o), o['id'] + ' raw')


def ingot_item(o):
    t = o['tier']
    grid = S.INGOT['rune' if t >= 7 else 'stamped' if t >= 4 or o['use'] == 'alloy' else 'bar']
    return px.render(outlined(grid), palette(o), o['id'] + ' ingot')


def gem_item(o):
    grid = S.GEM[gem_shape(o)]
    if o['tier'] < 3 and gem_shape(o) == 'rough':
        grid = px.remap(grid, {'g': '2', 'G': '3'})
    return px.render(outlined(grid), palette(o), o['id'] + ' gem')


def storage(o, kind):
    grid = {'metal': S.STORAGE_METAL, 'gem': S.STORAGE_GEM, 'raw': S.STORAGE_RAW}[kind]
    if o['tier'] < 5:
        grid = no_glow(grid)
    return px.render(grid, palette(o), o['id'] + ' ' + kind)


STYLE_SHAPE = {'balanced': 'knight', 'sustain': 'knight', 'fast': 'slim', 'light': 'slim', 'ranged': 'slim',
               'heavy': 'brutal', 'tank': 'brutal', 'crit': 'cruel', 'magic': 'arcane'}
# Theme by where the ore is found (catalog.PLACES); names can override.
PLACE_THEME = {
    'snowy': 'frost', 'glacier': 'frost', 'taiga': 'frost', 'peaks': 'frost',
    'nether': 'fire', 'nether_volcanic': 'fire', 'nether_crimson': 'fire', 'badlands': 'fire',
    'ocean': 'sea', 'ocean_floor': 'sea', 'beach': 'sea', 'swamp': 'sea',
    'holy': 'holy', 'river_plains': 'holy', 'savanna': 'holy',
    'corrupted': 'shadow', 'night': 'shadow', 'umbral': 'shadow', 'end_void': 'shadow', 'nether_souls': 'shadow',
    'swamp_ruins': 'shadow',
    'jungle': 'nature', 'fey_forest': 'nature', 'old_forest': 'nature', 'forest_deep': 'nature',
    'magical': 'arcane', 'crystal_caves': 'arcane', 'end': 'arcane', 'end_islands': 'arcane', 'nether_warped': 'arcane',
}
NAME_THEME = (('frost', 'frost'), ('glacier', 'frost'), ('winter', 'frost'), ('stalhrim', 'frost'), ('flame', 'fire'),
              ('cinder', 'fire'), ('hell', 'fire'), ('scarlet', 'fire'), ('hallow', 'holy'), ('celest', 'holy'),
              ('sigmar', 'holy'), ('imperial', 'holy'), ('royal', 'holy'), ('dawn', 'holy'), ('demon', 'shadow'),
              ('void', 'shadow'), ('dark', 'shadow'), ('ebony', 'shadow'), ('stygian', 'shadow'), ('black', 'shadow'),
              ('umbr', 'shadow'), ('daedric', 'shadow'), ('crimtane', 'shadow'), ('chloro', 'nature'), ('terra', 'nature'),
              ('mana', 'arcane'), ('thaum', 'arcane'), ('aether', 'arcane'), ('arcan', 'arcane'), ('magi', 'arcane'),
              ('element', 'arcane'), ('tidal', 'sea'), ('pearl', 'sea'), ('orichalcum', 'sea'), ('dragon', 'fire'))


def theme_of(o):
    for key, theme in NAME_THEME:
        if key in o['id']:
            return theme
    return PLACE_THEME.get(o['place'], 'earth')


def finish_of(o):
    return 'regal' if o['tier'] >= 7 else 'fitted' if o['tier'] >= 4 else 'plain'


def _armor_mask(kind, weight, theme):
    if kind == 'boots':
        return [h + h for h in S2.BOOT[weight]]
    g = S2.mirror({'helmet': S2.HELM, 'chestplate': S2.CHEST, 'leggings': S2.LEGS}[kind][weight])
    if kind == 'helmet':
        g = place_mark(g, S2.CREST[theme])
    elif kind == 'chestplate':
        g = place_mark(g, S2.EMBLEM[theme])
    return g


def gear_item(o, kind, style, weight):
    """One gear icon: the style's shape, the theme's marks, the tier's finish (trim metal, pommel, gems)."""
    tier, shape, theme = o['tier'], STYLE_SHAPE[style], theme_of(o)
    if kind == 'sword':
        grid, pal = S2.SWORD[shape], palette(o, 'leather')
    elif kind in ('axe', 'spear') or kind in S2.TOOL:
        top, head = {'axe': S2.AXE, 'spear': S2.SPEAR}.get(kind, {}).get(shape) or S2.TOOL[kind]
        grid, pal = compose(S2.haft(top), head), palette(o)
        if kind in S2.GEM_AT and tier >= 5:
            grid = put(grid, [S2.GEM_AT[kind]], 'G')
    else:
        mask = _armor_mask(kind, weight, theme)
        grid, pal = shade_mask(mask), palette(o)
    if finish_of(o) == 'regal':
        # Legendary: the pommel / butt cap becomes a gem.
        grid = put(grid, [(1, 14)], 'G') if grid[14][1] in 'bc' else grid
    if tier < 3 and shape == 'arcane':
        grid = px.remap(grid, {'G': 'g'})
    validate(grid, f'{o["id"]} {kind}', 2 if kind == 'boots' else 1)
    return px.render(outlined(grid), pal, f'{o["id"]} {kind}')


# ---------------------------------------------------------------- worn armor (64x32, the player model's layout)
def _faces(u, v, w, h, d):
    """Texture rects of a model box: name -> (x, y, width, height)."""
    return {'top': (u + d, v, w, d), 'bottom': (u + d + w, v, w, d), 'right': (u, v + d, d, h),
            'front': (u + d, v + d, w, h), 'left': (u + d + w, v + d, d, h), 'back': (u + 2 * d + w, v + d, w, h)}


HEAD, BODY, ARM, LEG = (0, 0, 8, 8, 8), (16, 16, 8, 12, 4), (40, 16, 4, 12, 4), (0, 16, 4, 12, 4)
# Base value of a side face: light comes from the viewer's top left, so the wearer's right side and front are lit.
FACE_BASE = {'top': 4, 'right': 3, 'front': 3, 'left': 2, 'back': 2, 'bottom': 1}


class Canvas:
    def __init__(self):
        self.c = [['.'] * 64 for _ in range(32)]

    def set(self, x, y, ch):
        self.c[y][x] = ch

    def grid(self):
        return [''.join(r) for r in self.c]


def _plates(cv, box, rows, weight, tier, seed, trims=(), clear=()):
    """Paints a box's faces in `rows` (local row range of the side faces) as overlapping plates: each lame catches
    light on its top row and casts a shadow on its bottom row. Trim rows are painted in the accent metal."""
    faces = _faces(*box)
    lame = {'light': 2, 'medium': 3, 'heavy': 4}[weight]
    for name, (fx, fy, fw, fh) in faces.items():
        base = FACE_BASE[name]
        if name in ('top', 'bottom'):
            if name == 'top' and rows[0] > 0:
                continue
            if name == 'bottom' and rows[1] < box[3]:
                continue
            for y in range(fh):
                for x in range(fw):
                    cv.set(fx + x, fy + y, str(base if name == 'bottom' or (x + y) % 5 else base + 1))
            continue
        for ly in range(rows[0], rows[1]):
            for x in range(fw):
                if (name, ly) in clear:
                    continue
                k = (ly - rows[0]) % lame
                v = base
                if weight == 'light':
                    # Scales: offset rows of small plates, lit on the upper left.
                    off = (ly // 2) % 2
                    v = base + (1 if (x + off) % 2 == 0 and ly % 2 == 0 else 0) - (1 if ly % 2 == 1 and (x + off) % 2 == 1 else 0)
                else:
                    if k == 0:
                        v = base + 1
                    elif k == lame - 1:
                        v = base - 1
                    if weight == 'heavy' and k == 1 and x in (0, fw - 1):
                        v = base + 2             # rivets at the plate edges
                if ly in trims:
                    ch = 'c' if name in ('front', 'right') else 'b' if name != 'back' else 'a'
                    if ly == trims[-1] and name in ('front', 'right') and x == 0:
                        ch = 'd'
                else:
                    ch = str(max(1, min(5, v)))
                cv.set(fx + x, fy + ly, ch)


def worn(o, weight, leggings_layer):
    tier = o['tier']
    cv = Canvas()
    seed = _h(o['id'])
    if leggings_layer:
        # Waist (lower half of the body box) and legs down to the boot tops.
        _plates(cv, BODY, (7, 12), weight, tier, seed, trims=(7,))
        _plates(cv, LEG, (0, 9), weight, tier, seed, trims=(0, 4) if weight != 'light' else (0,))
        if weight != 'light':
            fx, fy, fw, fh = _faces(*LEG)['front']
            cv.set(fx + 1, fy + 5, 'd' if tier >= 4 else 'c')     # knee cop boss
            cv.set(fx + 2, fy + 5, 'c')
    else:
        # Helmet.
        _plates(cv, HEAD, (0, 8), weight, tier, seed, trims=(7,))
        fx, fy, fw, fh = _faces(*HEAD)['front']
        if weight == 'light':
            for y in range(2, 8):
                for x in range(2, 6):
                    cv.set(fx + x, fy + y, '.')          # open face
        elif weight == 'medium':
            for x in range(1, 7):
                cv.set(fx + x, fy + 3, 'o')              # eye slit
            for y in range(4, 8):
                for x in (3, 4):
                    cv.set(fx + x, fy + y, 'o')          # T-visor
        else:
            for x in range(1, 7):
                if x not in (3, 4):
                    cv.set(fx + x, fy + 3, 'o')          # two eye slits
            for x, y in ((2, 5), (5, 5), (2, 6), (5, 6), (3, 6), (4, 6)):
                cv.set(fx + x, fy + y, 'o')              # breaths
            for y in range(1, 7):
                cv.set(fx + 3, fy + y, 'c' if y < 3 else cv.c[fy + y][fx + 3])   # brow ridge
        if tier >= 5:
            cv.set(fx + 3, fy + 1, 'G')
            cv.set(fx + 4, fy + 1, 'g')
        if tier >= 4:
            tx, ty, tw, th = _faces(*HEAD)['top']
            for y in range(th):                           # crest along the crown
                cv.set(tx + 3, ty + y, 'c')
                cv.set(tx + 4, ty + y, 'b')
            bx, by, bw, bh = _faces(*HEAD)['back']
            for y in range(0, 6):
                cv.set(bx + 3, by + y, 'b')
                cv.set(bx + 4, by + y, 'a')
        # Chest and back.
        _plates(cv, BODY, (0, 12), weight, tier, seed, trims=(0, 10))
        fx, fy, fw, fh = _faces(*BODY)['front']
        if weight != 'light':
            for y in range(2, 9):
                cv.set(fx + 4, fy + y, 'o' if weight == 'heavy' else '2')   # the breastplate's ridge
        cv.set(fx + 3, fy + 10, 'd')                     # belt buckle
        cv.set(fx + 4, fy + 10, 'd')
        if tier >= 5:
            cv.set(fx + 3, fy + 3, 'G')
            cv.set(fx + 4, fy + 3, 'g')
            cv.set(fx + 3, fy + 4, 'g')
            cv.set(fx + 4, fy + 4, 'g' if weight != 'heavy' else 'o')
        # Arms: pauldrons on top, a vambrace band at the forearm.
        _plates(cv, ARM, (0, 12), weight, tier, seed, trims=(3, 8) if weight != 'light' else (8,))
        if weight == 'heavy':
            for name in ('front', 'right', 'back', 'left'):
                ax, ay, aw, ah = _faces(*ARM)[name]
                for x in range(aw):
                    cv.set(ax + x, ay + 0, 'd' if name in ('front', 'right') and x == 0 else 'c')
        # Boots: the lower legs.
        _plates(cv, LEG, (7, 12), weight, tier, seed, trims=(7,))
        for name in ('front', 'right', 'back', 'left'):
            lx, ly, lw, lh = _faces(*LEG)[name]
            for x in range(lw):
                cv.set(lx + x, ly + 11, '1')             # soles
    return px.render(cv.grid(), palette(o), f'{o["id"]} worn')


# ---------------------------------------------------------------- writing
def write(root, ores, sets, styles, glow_ores=()):
    """Writes every ore's textures under `root` (assets/pipican/textures). Returns the relative paths written."""
    written = []

    def save(im, rel):
        path = os.path.join(root, rel + '.png')
        os.makedirs(os.path.dirname(path), exist_ok=True)
        im.save(path)
        written.append(rel)

    for o in ores:
        i = o['id']
        mined = o['use'] not in ('alloy', 'relic')
        if mined:
            save(ore_overlay(o, glow_ores), f'block/ore/{i}')
        if o['form'] == 'gem':
            save(gem_item(o), f'item/ore/{i}')
            save(storage(o, 'gem'), f'block/ore/{i}_block')
        else:
            save(ingot_item(o), f'item/ore/{i}_ingot')
            save(storage(o, 'metal'), f'block/ore/{i}_block')
            if mined:
                save(raw_item(o), f'item/ore/raw_{i}')
                save(storage(o, 'raw'), f'block/ore/raw_{i}_block')
    by_id = {o['id']: o for o in ores}
    for sid, spec in sets.items():
        o = by_id[sid]
        style = spec['style']
        weight = styles[style]['weight']
        for kind in ('helmet', 'chestplate', 'leggings', 'boots', 'sword', 'axe', 'spear', 'pickaxe', 'shovel', 'hoe'):
            save(gear_item(o, kind, style, weight), f'item/gear/{sid}/{kind}')
        save(worn(o, weight, False), f'entity/equipment/humanoid/{sid}')
        save(worn(o, weight, True), f'entity/equipment/humanoid_leggings/{sid}')
    return written


def preview(root, ores, sets, out, scale=4, stones=None):
    """A contact sheet: per ore one row - overlay on stone, items, storage, then the gear and the worn armor."""
    from PIL import Image
    rows = []
    for o in ores:
        i = o['id']
        tiles = []
        for rel in (f'block/ore/{i}', f'item/ore/raw_{i}', f'item/ore/{i}_ingot', f'item/ore/{i}', f'block/ore/{i}_block',
                    f'block/ore/raw_{i}_block'):
            p = os.path.join(root, rel + '.png')
            if os.path.exists(p):
                im = Image.open(p).convert('RGBA')
                if rel == f'block/ore/{i}' and stones:
                    base = stones.copy()
                    base.alpha_composite(im)
                    im = base
                tiles.append(im)
        if i in sets:
            for kind in ('helmet', 'chestplate', 'leggings', 'boots', 'sword', 'axe', 'spear', 'pickaxe', 'shovel', 'hoe'):
                tiles.append(Image.open(os.path.join(root, f'item/gear/{i}/{kind}.png')).convert('RGBA'))
            tiles.append(Image.open(os.path.join(root, f'entity/equipment/humanoid/{i}.png')).convert('RGBA'))
            tiles.append(Image.open(os.path.join(root, f'entity/equipment/humanoid_leggings/{i}.png')).convert('RGBA'))
        rows.append(tiles)
    width = max(sum(t.width + 2 for t in r) for r in rows)
    sheet = Image.new('RGBA', (width, 18 * len(rows) + 16 * 0), (58, 56, 66, 255))
    y = 0
    for r in rows:
        x = 0
        for t in r:
            sheet.alpha_composite(t, (x, y))
            x += t.width + 2
        y += 18 if all(t.height <= 16 for t in r) else 34
    sheet = sheet.crop((0, 0, width, y))
    sheet.resize((sheet.width * scale, sheet.height * scale), Image.NEAREST).save(out)


if __name__ == '__main__':
    sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'class_builder'))
    import catalog
    import sets as gear_sets
    from build_ores import GLOW
    root = sys.argv[1] if len(sys.argv) > 1 and not sys.argv[1].startswith('--') else 'src/main/resources/assets/pipican/textures'
    write(root, catalog.ORES, gear_sets.SETS, gear_sets.STYLES, GLOW)
    if '--preview' in sys.argv:
        preview(root, catalog.ORES, gear_sets.SETS, sys.argv[sys.argv.index('--preview') + 1])


# ---------------------------------------------------------------- shape masks and validation
def _components(grid, members):
    h, w = len(grid), len(grid[0])
    seen, comps = set(), []
    for y in range(h):
        for x in range(w):
            if (x, y) in seen or grid[y][x] not in members:
                continue
            stack, comp = [(x, y)], []
            seen.add((x, y))
            while stack:
                cx, cy = stack.pop()
                comp.append((cx, cy))
                for nx, ny in ((cx + 1, cy), (cx - 1, cy), (cx, cy + 1), (cx, cy - 1)):
                    if 0 <= nx < w and 0 <= ny < h and (nx, ny) not in seen and grid[ny][nx] in members:
                        seen.add((nx, ny))
                        stack.append((nx, ny))
            comps.append(comp)
    return comps


def _volume(comp, val_of):
    """Shades one part as a volume lit from the top left: a lit band on the left, shadow on the right, surfaces that
    face up catch light and surfaces that face down fall into shadow."""
    xs, ys = [p[0] for p in comp], [p[1] for p in comp]
    x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
    cs = set(comp)
    out = {}
    for x, y in comp:
        u = (x - x0) / max(1, x1 - x0)
        v = (y - y0) / max(1, y1 - y0)
        val = 3
        if u <= 0.3:
            val = 4
        if u <= 0.15 and v <= 0.5:
            val = 5
        if u >= 0.7:
            val = 2
        if u >= 0.9 and v >= 0.5:
            val = 1
        if (x, y - 1) not in cs:
            val += 1
        elif (x, y + 1) not in cs:
            val -= 1
        out[(x, y)] = val_of(max(1, min(5, val)))
    return out


def shade_mask(grid):
    """Turns an armor shape mask (gear_sprites legend) into a fill grid."""
    rows = [list(r) for r in grid]
    for comp in _components(grid, set('MRrvo')):
        metal = [p for p in comp if grid[p[1]][p[0]] == 'M']
        for (x, y), c in _volume(metal, str).items():
            rows[y][x] = c
    for comp in _components(grid, set('Tv')):
        for (x, y), c in _volume(comp, lambda v: 'abcdd'[v - 1]).items():
            rows[y][x] = c
    for y, r in enumerate(grid):
        for x, c in enumerate(r):
            if c == 'R':
                rows[y][x] = '5'
            elif c == 'r':
                rows[y][x] = '2'
            elif c == 'v':
                rows[y][x] = 'd'
    return [''.join(r) for r in rows]


def validate(grid, name, parts=1):
    """Fails on orphan pixels (no filled 4-neighbour) and on shapes that fall apart into more than `parts` pieces."""
    filled = set('.') ^ set(''.join(grid))
    comps = _components(grid, filled - {'.'})
    for comp in comps:
        if len(comp) == 1:
            raise ValueError(f'{name}: orphan pixel at {comp[0]}')
    if len(comps) > parts:
        raise ValueError(f'{name}: {len(comps)} separate pieces (expected {parts}): ' +
                         ', '.join(str(sorted(c)[0]) for c in comps))
    return grid


def place_mark(grid, mark, mirror=True):
    """Draws a theme mark (left-half coordinates, mirrored) onto a mask; a mark that would float free is slid
    toward the centre until it touches the piece."""
    if not mark:
        return grid
    for shift in range(0, 6):
        rows = [list(r) for r in grid]
        for (x, y), c in mark.items():
            for xx in ((x + shift, 15 - x - shift) if mirror else (x + shift,)):
                if rows[y][xx] == '.':
                    rows[y][xx] = c
        out = [''.join(r) for r in rows]
        if len(_components(out, set(''.join(out)) - {'.'})) == len(_components(grid, set(''.join(grid)) - {'.'})):
            return out
    raise ValueError(f'mark {mark} never touches the piece')
