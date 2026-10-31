"""Build Corallith's reef/undertow pixel atlases and coral sigil/trophy icons."""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/pipican/textures"


def atlas(filename, base, mid, light, shadow, phase):
    image = Image.new("RGBA", (256, 256))
    pixels = image.load()
    for y in range(256):
        for x in range(256):
            noise = (x * 9 + y * 5 + (x // 8) * 17) % 47
            if x % 32 < 2 or y % 32 < 2:
                color = shadow
            elif (x * 7 + y * 3 + phase * 13) % 61 < 4:
                color = light
            elif noise > 34:
                color = mid
            else:
                color = base
            pixels[x, y] = (*color, 255)
    draw = ImageDraw.Draw(image)
    for offset in range(0, 256, 32):
        draw.line((offset, 0, offset, 255), fill=(*shadow, 255))
        draw.line((0, offset, 255, offset), fill=(*shadow, 255))
    image.save(ROOT / "entity/boss" / filename)


def icon(filename, trophy=False):
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    if trophy:
        draw.polygon([(8, 1), (12, 4), (15, 8), (12, 12), (8, 15), (4, 12), (1, 8), (4, 4)], fill=(102, 214, 203, 255))
        draw.polygon([(8, 4), (10, 7), (8, 12), (5, 8)], fill=(30, 92, 105, 255))
    else:
        draw.polygon([(8, 1), (13, 4), (14, 8), (10, 14), (8, 15), (6, 14), (2, 8), (3, 4)], fill=(11, 51, 66, 255))
        draw.polygon([(8, 3), (11, 5), (11, 8), (8, 12), (5, 8), (5, 5)], fill=(62, 166, 171, 255))
        draw.point((8, 6), fill=(188, 255, 239, 255))
    image.resize((256, 256), Image.Resampling.NEAREST).save(ROOT / "item" / filename)


atlas("corallith_reef.png", (24, 92, 105), (36, 157, 163), (146, 232, 222), (9, 43, 57), 0)
atlas("corallith_undertow.png", (30, 57, 101), (47, 124, 184), (141, 224, 244), (13, 29, 61), 1)
icon("corallith_reef_colossus_sigil.png")
icon("corallith_reef_colossus_trophy.png", trophy=True)
