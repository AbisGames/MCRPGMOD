"""Build Hrothgar's salt-iron atlases and jarl sigil/trophy icons."""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/pipican/textures"


def atlas(name, base, mid, light, shadow, phase):
    image = Image.new("RGBA", (256, 256))
    pixels = image.load()
    for y in range(256):
        for x in range(256):
            grain = (x * 9 + y * 7 + (x // 6) * 13 + phase * 29) % 61
            color = shadow if x % 32 < 2 or y % 32 < 2 else light if (x * 11 + y * 3 + phase * 17) % 131 < 7 else mid if grain > 40 else base
            pixels[x, y] = (*color, 255)
    draw = ImageDraw.Draw(image)
    for offset in range(0, 256, 32):
        draw.line((offset, 0, offset, 255), fill=(*shadow, 255))
        draw.line((0, offset, 255, offset), fill=(*shadow, 255))
    for y in (24, 88, 152, 216):
        draw.line((40, y, 87, y), fill=(*light, 255), width=2)
        draw.line((168, y, 215, y), fill=(*light, 255), width=2)
    image.save(ROOT / "entity/boss" / name)


def icon(name, trophy=False):
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    if trophy:
        draw.polygon([(3, 2), (6, 5), (8, 2), (10, 5), (13, 2), (13, 11), (10, 14), (6, 14), (3, 11)], fill=(163, 187, 190, 255))
        draw.line((5, 8, 11, 8), fill=(44, 76, 89, 255), width=2)
    else:
        draw.polygon([(8, 1), (13, 5), (12, 12), (8, 15), (4, 12), (3, 5)], fill=(39, 68, 83, 255))
        draw.polygon([(8, 3), (11, 6), (9, 11), (8, 12), (5, 7)], fill=(132, 204, 209, 255))
        draw.point((8, 6), fill=(246, 255, 244, 255))
    image.resize((256, 256), Image.Resampling.NEAREST).save(ROOT / "item" / name)


atlas("hrothgar_jarl.png", (75, 97, 105), (131, 150, 143), (209, 222, 201), (38, 53, 68), 0)
atlas("hrothgar_drowned.png", (35, 64, 79), (58, 111, 121), (147, 203, 196), (18, 35, 54), 1)
icon("hrothgar_drowned_jarl_sigil.png")
icon("hrothgar_drowned_jarl_trophy.png", trophy=True)
