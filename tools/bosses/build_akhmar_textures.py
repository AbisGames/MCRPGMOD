"""Build pixel-art Akhmar atlas and inventory icons in a repeatable way."""
from pathlib import Path
from PIL import Image, ImageDraw
import random

ROOT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/pipican/textures"
ROOT.mkdir(parents=True, exist_ok=True)

def atlas(name, phase):
    rng = random.Random(2107 + phase)
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    px = img.load()
    base = (126, 68, 35) if phase == 1 else (107, 38, 27)
    mid = (190, 123, 52) if phase == 1 else (202, 91, 34)
    gold = (239, 193, 82) if phase == 1 else (255, 143, 45)
    shadow = (71, 39, 32) if phase == 1 else (60, 27, 31)
    for y in range(256):
        for x in range(256):
            # Fine sandstone grain, broken by dark engraved bands and gold inlay.
            tile = ((x // 32) + (y // 32)) % 4
            wave = (x * 3 + y * 5 + (x // 8) * 7) % 31
            if (x % 32 in (0, 1) or y % 32 in (0, 1)):
                color = shadow
            elif (x + 2 * y) % 37 < 2 or (x % 64 in (30, 31) and y % 64 < 28):
                color = gold
            elif wave < 3:
                color = shadow
            elif wave > 25 or tile == 1 and (x // 4 + y // 4) % 7 == 0:
                color = mid
            else:
                color = base
            if (x % 64 in (31, 32) and y % 64 in range(18, 26)) or (y % 64 in (31, 32) and x % 64 in range(18, 26)):
                color = gold
            px[x, y] = (*color, 255)
    # The original UV island faces are separated by a dark pixel outline.
    draw = ImageDraw.Draw(img)
    for x in range(0, 256, 32): draw.line((x, 0, x, 255), fill=(*shadow, 255), width=1)
    for y in range(0, 256, 32): draw.line((0, y, 255, y), fill=(*shadow, 255), width=1)
    img.save(ROOT / "entity/boss" / name)

def icon(name, kind, colors):
    img = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); d = ImageDraw.Draw(img)
    dark, mid, light = colors
    if kind == "sigil":
        d.rectangle((3,2,12,13), fill=dark); d.rectangle((4,3,11,12), fill=mid)
        d.polygon([(8,3),(10,6),(12,7),(10,9),(8,12),(6,9),(4,7),(6,6)], fill=light)
        d.rectangle((7,5,8,10), fill=dark); d.rectangle((5,7,10,8), fill=dark)
        d.point((8,7), fill=(255,245,179,255))
    else:
        d.rectangle((3,10,12,13), fill=dark); d.rectangle((4,9,11,11), fill=mid)
        d.polygon([(3,9),(4,3),(7,7),(8,2),(10,7),(13,3),(13,10)], fill=light)
        d.rectangle((5,9,11,10), fill=mid); d.point((8,7), fill=(255,245,179,255))
    img.resize((256,256),Image.Resampling.NEAREST).save(ROOT / "item" / name)

atlas("akhmar_sand_emperor.png",1)
atlas("akhmar_sunfire.png",2)
icon("akhmar_sand_emperor_sigil.png","sigil",((91,47,25,255),(181,104,43,255),(242,190,67,255)))
icon("akhmar_sand_emperor_trophy.png","trophy",((76,37,26,255),(156,77,37,255),(242,188,67,255)))
