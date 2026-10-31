"""Build Azhar's sun-bronze atlases and sigil/trophy icons."""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/pipican/textures"


def atlas(filename, base, mid, light, shadow, phase):
    image = Image.new("RGBA", (256, 256))
    pixels = image.load()
    for y in range(256):
        for x in range(256):
            grain = (x * 11 + y * 7 + (x // 8) * 13 + phase * 19) % 53
            if x % 32 < 2 or y % 32 < 2:
                color = shadow
            elif ((x - 128) ** 2 + (y - 128) ** 2 + phase * 23) % 197 < 9:
                color = light
            elif grain > 38:
                color = mid
            else:
                color = base
            pixels[x, y] = (*color, 255)
    draw = ImageDraw.Draw(image)
    for offset in range(0, 256, 32):
        draw.line((offset, 0, offset, 255), fill=(*shadow, 255))
        draw.line((0, offset, 255, offset), fill=(*shadow, 255))
    # Distinctive horn and plate highlights in UV regions used by the source model.
    for box in [(40, 24, 87, 39), (168, 24, 215, 39), (104, 104, 151, 119)]:
        draw.rectangle(box, fill=(*light, 255))
        draw.line((box[0], box[3], box[2], box[3]), fill=(*shadow, 255), width=2)
    image.save(ROOT / "entity/boss" / filename)


def icon(filename, trophy=False):
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    if trophy:
        draw.polygon([(8, 1), (12, 4), (15, 8), (12, 12), (8, 15), (4, 12), (1, 8), (4, 4)], fill=(250, 203, 75, 255))
        draw.polygon([(8, 4), (10, 7), (8, 12), (5, 8)], fill=(159, 91, 20, 255))
        draw.rectangle((6, 13, 10, 14), fill=(255, 232, 141, 255))
    else:
        draw.polygon([(8, 1), (13, 4), (14, 8), (10, 14), (8, 15), (6, 14), (2, 8), (3, 4)], fill=(125, 72, 19, 255))
        draw.polygon([(8, 3), (11, 5), (11, 8), (8, 12), (5, 8), (5, 5)], fill=(244, 179, 46, 255))
        draw.point((8, 6), fill=(255, 247, 177, 255))
    image.resize((256, 256), Image.Resampling.NEAREST).save(ROOT / "item" / filename)


atlas("azhar_golden.png", (131, 83, 29), (197, 137, 43), (255, 218, 107), (72, 43, 22), 0)
atlas("azhar_sunheart.png", (112, 56, 25), (216, 119, 28), (255, 237, 155), (55, 32, 21), 1)
icon("azhar_golden_behemoth_sigil.png")
icon("azhar_golden_behemoth_trophy.png", trophy=True)
