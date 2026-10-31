"""Generate reusable 256px pixel-art atlas phases and Zahira item icons."""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/pipican/textures"
ROOT.mkdir(parents=True, exist_ok=True)

def atlas(name, phase):
    image = Image.new("RGBA", (256, 256), (0, 0, 0, 0)); pixels = image.load()
    base = (83, 35, 43) if phase == 1 else (45, 25, 56)
    mid = (164, 46, 55) if phase == 1 else (136, 38, 83)
    gold = (231, 151, 55) if phase == 1 else (255, 99, 82)
    shadow = (44, 22, 39) if phase == 1 else (28, 20, 48)
    for y in range(256):
        for x in range(256):
            tile = (x // 32 + 2 * (y // 32)) % 5
            weave = (x * 7 + y * 3 + (x // 8) * 5) % 41
            if x % 32 in (0, 1) or y % 32 in (0, 1): color = shadow
            elif (x + y * 3) % 47 < 3: color = gold
            elif weave < 5: color = shadow
            elif weave > 32 or tile == 2 and (x // 4 + y // 4) % 7 == 0: color = mid
            else: color = base
            if (x % 64 in (30, 31, 32) and y % 64 in range(16, 27)) or (y % 64 in (31, 32) and x % 64 in range(18, 25)):
                color = gold
            pixels[x, y] = (*color, 255)
    draw = ImageDraw.Draw(image)
    for x in range(0, 256, 32): draw.line((x, 0, x, 255), fill=(*shadow, 255), width=1)
    for y in range(0, 256, 32): draw.line((0, y, 255, y), fill=(*shadow, 255), width=1)
    image.save(ROOT / "entity/boss" / name)

def icon(name, trophy=False):
    dark, red, gold = (43, 22, 33, 255), (151, 39, 55, 255), (240, 158, 51, 255)
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); draw = ImageDraw.Draw(image)
    if trophy:
        draw.polygon([(2, 3), (5, 4), (8, 1), (11, 4), (14, 3), (13, 10), (10, 12), (6, 12), (3, 10)], fill=gold)
        draw.rectangle((5, 9, 11, 12), fill=red); draw.rectangle((6, 12, 10, 14), fill=dark)
        draw.line((8, 3, 8, 10), fill=dark); draw.point((8, 6), fill=(255,220,125,255))
    else:
        draw.polygon([(8,1),(10,4),(13,3),(12,6),(15,8),(12,10),(11,14),(8,12),(5,14),(4,10),(1,8),(4,6),(3,3),(6,4)], fill=dark)
        draw.polygon([(8,3),(10,6),(12,7),(10,9),(8,11),(6,9),(4,7),(6,6)], fill=red)
        draw.line((8,4,8,11), fill=gold); draw.point((8,7), fill=(255,225,137,255))
    image.resize((256, 256), Image.Resampling.NEAREST).save(ROOT / "item" / name)

atlas("zahira_corsair.png", 1)
atlas("zahira_bloodtide.png", 2)
icon("zahira_crimson_corsair_sigil.png")
icon("zahira_crimson_corsair_trophy.png", True)
