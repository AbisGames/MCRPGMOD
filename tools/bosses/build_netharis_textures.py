"""Build water-current pixel atlases and compact Netharis inventory icons."""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/pipican/textures"
ROOT.mkdir(parents=True, exist_ok=True)

def atlas(name, phase):
    image = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    pixels = image.load()
    base = (25, 90, 111) if phase == 1 else (23, 48, 104)
    mid = (38, 154, 166) if phase == 1 else (45, 117, 204)
    light = (131, 220, 204) if phase == 1 else (158, 230, 255)
    shadow = (12, 44, 70) if phase == 1 else (21, 29, 75)
    for y in range(256):
        for x in range(256):
            tile = ((x // 32) + (y // 32)) % 4
            ripple = (x * 5 + y * 3 + (x // 8) * 11) % 37
            if x % 32 in (0, 1) or y % 32 in (0, 1): color = shadow
            elif (x + y * 2) % 43 < 3: color = light
            elif ripple < 5: color = shadow
            elif ripple > 29 or tile == 2 and (x // 4 + y // 4) % 6 == 0: color = mid
            else: color = base
            if (x % 64 in (30, 31, 32) and y % 64 in range(17, 25)) or (y % 64 in (31, 32) and x % 64 in range(20, 27)):
                color = light
            pixels[x, y] = (*color, 255)
    draw = ImageDraw.Draw(image)
    for x in range(0, 256, 32): draw.line((x, 0, x, 255), fill=(*shadow, 255), width=1)
    for y in range(0, 256, 32): draw.line((0, y, 255, y), fill=(*shadow, 255), width=1)
    image.save(ROOT / "entity/boss" / name)

def icon(name, trophy=False):
    dark, blue, light = (12, 43, 75, 255), (27, 135, 162, 255), (126, 222, 213, 255)
    image = Image.new("RGBA", (16, 16), (0, 0, 0, 0)); draw = ImageDraw.Draw(image)
    if trophy:
        draw.polygon([(2, 3), (6, 5), (8, 2), (10, 5), (14, 3), (13, 11), (10, 13), (6, 13), (3, 10)], fill=light)
        draw.rectangle((5, 9, 11, 12), fill=blue); draw.rectangle((6, 12, 10, 14), fill=dark)
        draw.point((8, 7), fill=(217, 255, 245, 255))
    else:
        draw.polygon([(8, 1), (12, 4), (13, 8), (10, 13), (8, 15), (6, 13), (3, 8), (4, 4)], fill=dark)
        draw.polygon([(8, 3), (11, 5), (11,8), (8,12), (5,8), (5,5)], fill=blue)
        draw.line((8, 4, 8, 11), fill=light, width=1); draw.point((8, 7), fill=(226,255,247,255))
    image.resize((256, 256), Image.Resampling.NEAREST).save(ROOT / "item" / name)

atlas("netharis_river_guardian.png", 1)
atlas("netharis_flooded.png", 2)
icon("netharis_river_guardian_sigil.png")
icon("netharis_river_guardian_trophy.png", True)
