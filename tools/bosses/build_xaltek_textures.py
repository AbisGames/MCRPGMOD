"""Build Xaltek's 256px verdant body atlases and sigil/trophy icons."""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/pipican/textures"


def atlas(name, base, mid, light, shadow, phase):
    image = Image.new("RGBA", (256, 256), (*base, 255))
    px = image.load()
    for y in range(256):
        for x in range(256):
            noise = (x * 7 + y * 11 + (x // 8) * 19) % 43
            if x % 32 < 2 or y % 32 < 2:
                color = shadow
            elif (x * 3 + y * 5 + phase * 7) % 59 < 4:
                color = light
            elif noise > 31 or ((x // 4 + y // 8 + phase) % 17 == 0 and (x + y) % 5 < 2):
                color = mid
            else:
                color = base
            px[x, y] = (*color, 255)
    draw = ImageDraw.Draw(image)
    for offset in range(0, 256, 32):
        draw.line((offset, 0, offset, 255), fill=(*shadow, 255))
        draw.line((0, offset, 255, offset), fill=(*shadow, 255))
    image.save(ROOT / "entity/boss" / name)


def icon(name, trophy=False):
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    if trophy:
        draw.polygon([(8, 1), (12, 4), (15, 7), (12, 11), (9, 14), (6, 12), (2, 8), (5, 4)], fill=(117, 206, 83, 255))
        draw.polygon([(8, 4), (10, 7), (8, 11), (5, 8)], fill=(31, 95, 45, 255))
        draw.line((8, 3, 8, 12), fill=(206, 250, 138, 255))
    else:
        draw.polygon([(8, 1), (13, 4), (14, 8), (10, 14), (8, 15), (6, 14), (2, 8), (3, 4)], fill=(18, 57, 30, 255))
        draw.polygon([(8, 3), (11, 5), (11, 8), (8, 12), (5, 8), (5, 5)], fill=(68, 159, 66, 255))
        draw.line((8, 4, 8, 11), fill=(177, 240, 110, 255))
    image.resize((256, 256), Image.Resampling.NEAREST).save(ROOT / "item" / name)


atlas("xaltek_devourer.png", (28, 76, 34), (56, 139, 56), (156, 223, 93), (9, 40, 23), 0)
atlas("xaltek_overgrowth.png", (24, 105, 44), (45, 202, 83), (191, 255, 122), (9, 51, 34), 1)
icon("xaltek_emerald_devourer_sigil.png")
icon("xaltek_emerald_devourer_trophy.png", trophy=True)
