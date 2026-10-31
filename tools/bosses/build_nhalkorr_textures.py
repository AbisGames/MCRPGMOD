"""Build the glacial/abyssal leviathan atlases and trophy/sigil icons."""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/pipican/textures"


def atlas(name, base, mid, light, shadow, phase):
    image = Image.new("RGBA", (256, 256))
    pixels = image.load()
    for y in range(256):
        for x in range(256):
            grain = (x * 7 + y * 13 + x // 5 * 17 + phase * 23) % 59
            if x % 32 < 2 or y % 32 < 2:
                color = shadow
            elif (x * 3 + y * 5 + phase * 31) % 127 < 7:
                color = light
            elif grain > 41:
                color = mid
            else:
                color = base
            pixels[x, y] = (*color, 255)
    draw = ImageDraw.Draw(image)
    for u in range(0, 256, 32):
        draw.line((u, 0, u, 255), fill=(*shadow, 255))
        draw.line((0, u, 255, u), fill=(*shadow, 255))
    # Pale ridges accent the head and layered tail UV islands.
    for y in (30, 94, 158, 222):
        draw.line((40, y, 87, y), fill=(*light, 255), width=3)
        draw.line((168, y, 215, y), fill=(*light, 255), width=2)
    image.save(ROOT / "entity/boss" / name)


def icon(name, trophy=False):
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    if trophy:
        draw.polygon([(8, 1), (12, 4), (15, 8), (12, 12), (8, 15), (4, 12), (1, 8), (4, 4)], fill=(148, 229, 237, 255))
        draw.polygon([(8, 3), (10, 7), (8, 12), (5, 8)], fill=(42, 90, 134, 255))
    else:
        draw.polygon([(8, 1), (13, 5), (14, 9), (10, 14), (8, 15), (6, 14), (2, 9), (3, 5)], fill=(36, 82, 122, 255))
        draw.polygon([(8, 3), (11, 6), (10, 9), (8, 12), (5, 8), (5, 5)], fill=(125, 213, 226, 255))
        draw.point((8, 6), fill=(242, 255, 255, 255))
    image.resize((256, 256), Image.Resampling.NEAREST).save(ROOT / "item" / name)


atlas("nhalkorr_glacial.png", (52, 112, 142), (86, 171, 191), (205, 247, 249), (24, 56, 91), 0)
atlas("nhalkorr_abyssal.png", (29, 53, 96), (47, 117, 160), (130, 225, 233), (12, 25, 55), 1)
icon("nhalkorr_leviathan_sigil.png")
icon("nhalkorr_leviathan_trophy.png", trophy=True)
