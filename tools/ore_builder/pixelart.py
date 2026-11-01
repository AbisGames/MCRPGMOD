"""Pixel-art engine for the ore art: hue-shifted palettes and character-grid sprites (docs/PIXEL_ART.md).

Sprites are rows of characters, one character per pixel; a palette maps characters to colours. Every pixel is fully
opaque or fully transparent, and only palette colours are used.

Ramps are built in OKLCH (perceptual lightness / chroma / hue): shadows rotate toward a cool violet, highlights toward
a warm yellow, saturation peaks in the middle of the ramp. The art is for a dark fantasy setting, so ramps are a little
desaturated and their shadows deep.
"""
import math

from PIL import Image


# ---------------------------------------------------------------- colour maths (OKLab, Björn Ottosson)
def _lin(c):
    c /= 255
    return c / 12.92 if c <= 0.04045 else ((c + 0.055) / 1.055) ** 2.4


def _gam(c):
    c = 12.92 * c if c <= 0.0031308 else 1.055 * c ** (1 / 2.4) - 0.055
    return c * 255


def rgb_to_oklch(rgb):
    r, g, b = (_lin(v) for v in rgb)
    l = (0.4122214708 * r + 0.5363325363 * g + 0.0514459929 * b) ** (1 / 3)
    m = (0.2119034982 * r + 0.6806995451 * g + 0.1073969566 * b) ** (1 / 3)
    s = (0.0883024619 * r + 0.2817188376 * g + 0.6299787005 * b) ** (1 / 3)
    L = 0.2104542553 * l + 0.7936177850 * m - 0.0040720468 * s
    a = 1.9779984951 * l - 2.4285922050 * m + 0.4505937099 * s
    bb = 0.0259040371 * l + 0.7827717662 * m - 0.8086757660 * s
    return L, math.hypot(a, bb), math.degrees(math.atan2(bb, a)) % 360


def _oklch_to_lin(L, C, h):
    a, b = C * math.cos(math.radians(h)), C * math.sin(math.radians(h))
    l = (L + 0.3963377774 * a + 0.2158037573 * b) ** 3
    m = (L - 0.1055613458 * a - 0.0638541728 * b) ** 3
    s = (L - 0.0894841775 * a - 1.2914855480 * b) ** 3
    return (4.0767416621 * l - 3.3077115913 * m + 0.2309699292 * s,
            -1.2684380046 * l + 2.6097574011 * m - 0.3413193965 * s,
            -0.0041960863 * l - 0.7034186147 * m + 1.7076147010 * s)


def oklch_to_rgb(L, C, h):
    """Back to sRGB; chroma is reduced until the colour fits the gamut."""
    L = min(max(L, 0.0), 1.0)
    for _ in range(40):
        lin = _oklch_to_lin(L, C, h)
        if all(-1e-4 <= v <= 1.0001 for v in lin):
            break
        C *= 0.9
    return tuple(int(round(min(max(_gam(min(max(v, 0.0), 1.0)), 0), 255))) for v in lin)


def hexrgb(h):
    h = h.lstrip('#')
    return tuple(int(h[i:i + 2], 16) for i in (0, 2, 4))


def _toward(h, target, amount):
    """Rotate hue h toward target by at most `amount` degrees."""
    d = (target - h + 180) % 360 - 180
    return (h + max(-amount, min(amount, d))) % 360


COOL, WARM = 280.0, 85.0      # where shadows and highlights lean


# ---------------------------------------------------------------- palettes
def ramp(rgb, steps=5, shift=28.0, desat=0.85, glow=False):
    """A dark -> light ramp around a base colour. The base sits in the middle (step 3 of 5), unless it is very light
    or very dark - then the ramp is moved so it still has readable shadows and highlights."""
    L0, C0, h0 = rgb_to_oklch(rgb)
    if L0 > 0.84:
        Ls = [L0 - 0.40, L0 - 0.26, L0 - 0.13, L0 - 0.02, min(0.985, L0 + 0.06)]
    elif L0 < 0.36:
        # Very dark metals (darksteel, ebony): keep them dark, but give them a ramp you can read.
        Ls = [0.17, 0.25, 0.34, 0.46, 0.62]
    else:
        Ls = [L0 - 0.30, L0 - 0.16, L0, L0 + 0.12, L0 + 0.22]
    Ls = [min(max(v, 0.12), 0.975) for v in Ls]
    cw = [0.70, 0.92, 1.0, 0.85, 0.55] if not glow else [0.85, 1.0, 1.0, 0.9, 0.6]
    side = [-1.0, -0.5, 0.0, 0.5, 1.0]
    out = []
    neutral = C0 < 0.035
    for L, w, s in zip(Ls, cw, side):
        C = C0 * w * desat
        if s < 0:
            h = _toward(h0, COOL, shift * -s)
        elif s > 0:
            h = _toward(h0, WARM, shift * 0.8 * s)
        else:
            h = h0
        if neutral:
            # Gray metals: a breath of colour so the ramp isn't flat - cool shadows, warm light.
            if s < 0:
                h, C = 270.0, max(C, 0.022 * -s + 0.008)
            elif s > 0:
                h, C = 80.0, max(C, 0.012 * s)
        out.append(oklch_to_rgb(L, C, h))
    if steps != 5:
        idx = [round(i * 4 / (steps - 1)) for i in range(steps)]
        out = [out[i] for i in idx]
    return out


def outline(rgb):
    """The sprite's dark outline: very dark, leaning violet, carrying a little of the material's hue."""
    L0, C0, h0 = rgb_to_oklch(rgb)
    L = 0.11 if L0 < 0.36 else 0.16
    return oklch_to_rgb(L, min(0.045, 0.02 + C0 * 0.25), _toward(h0, 300.0, 60))


def glow(rgb):
    """A bright core for gems, runes and inlays: the ore's own hue pushed bright and saturated. Gray or very dark
    ores get an ember red (dark ores) or a cold arcane blue (pale ones) instead."""
    L0, C0, h0 = rgb_to_oklch(rgb)
    if C0 < 0.04:
        h0, C0 = (32.0, 0.17) if L0 < 0.6 else (235.0, 0.12)
    C = max(C0, 0.13)
    return [oklch_to_rgb(0.62, C, h0), oklch_to_rgb(0.86, C * 0.75, _toward(h0, WARM, 15))]


def mix(c1, c2, t=0.5):
    """OKLab blend of two colours (for the lit-side outline)."""
    a, b = rgb_to_oklch(c1), rgb_to_oklch(c2)
    L = a[0] + (b[0] - a[0]) * t
    C = a[1] + (b[1] - a[1]) * t
    d = (b[2] - a[2] + 180) % 360 - 180
    return oklch_to_rgb(L, C, (a[2] + d * t) % 360)


def fixed(*hexes):
    return [hexrgb(h) for h in hexes]


# Trim and fittings. Hue-shifted by hand: cool shadows, warm light. (outline, dark, mid, light)
ACCENTS = {
    'iron': fixed('#121019', '#2b2936', '#4b4a5a', '#7d7b88', '#b9b3ae'),       # blackened iron
    'bronze': fixed('#1d1216', '#4e2f27', '#83552f', '#b98a46', '#e6c27c'),     # aged bronze
    'gold': fixed('#21130f', '#6e3d1d', '#b27a26', '#e3b545', '#fbe7a1'),       # dark gilt
    'bone': fixed('#1a141a', '#4d4148', '#857868', '#bfb196', '#ece2c4'),       # old bone / ivory
}
WOOD = fixed('#140c10', '#33201f', '#553628', '#7c5638')                       # outline, dark, mid, light
LEATHER = fixed('#120b12', '#2b1a24', '#4a2c30', '#6d4239')


def hue_family(rgb):
    L, C, h = rgb_to_oklch(rgb)
    if C < 0.04:
        return 'neutral'
    if 40 <= h < 115:
        return 'warm_yellow'
    if h < 40 or h >= 330:
        return 'red'
    return 'cool'


# ---------------------------------------------------------------- sprites
def check(grid, name=''):
    w = len(grid[0])
    bad = [i for i, r in enumerate(grid) if len(r) != w]
    if bad:
        raise ValueError(f'{name}: rows {bad} are not {w} wide')
    return w, len(grid)


def render(grid, pal, name=''):
    """Character grid -> RGBA image. '.' and ' ' are transparent; every other character must be in the palette."""
    w, h = check(grid, name)
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    px = im.load()
    for y, row in enumerate(grid):
        for x, ch in enumerate(row):
            if ch in '. ':
                continue
            if ch not in pal:
                raise KeyError(f'{name}: no colour for {ch!r} at {x},{y}')
            c = pal[ch]
            if c is not None:
                px[x, y] = tuple(c) + (255,)
    return im


def flip(grid):
    return [r[::-1] for r in grid]


def remap(grid, table):
    return [''.join(table.get(c, c) for c in r) for r in grid]
