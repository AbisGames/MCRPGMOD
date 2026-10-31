"""Reproducible original basalt armor UV sheets; no vanilla texture dependencies.
These first-pass sheets require in-game art review before Morvath is marked complete.
"""
from pathlib import Path
from PIL import Image
ROOT=Path(__file__).resolve().parents[2]
TARGET=ROOT/'src/main/resources/assets/pipican/textures/entity/boss'
def build(enraged):
    image=Image.new('RGBA',(256,256))
    for y in range(256):
        for x in range(256):
            grain=((x*73+y*151+(x*y)*7)%17)-8
            seam=(x+(y//7)*3)%29==0 or (y+(x//13)*2)%37==0
            edge=x%16==0 or y%16==0
            if seam:
                color=(255,155 if enraged else 83,28 if enraged else 8,255)
            elif edge:
                color=(78+grain,59+grain,46+grain,255)
            else:
                color=(34+grain+(15 if enraged else 0),31+grain,36+grain,255)
            image.putpixel((x,y),color)
    return image
if __name__=='__main__':
    TARGET.mkdir(parents=True,exist_ok=True)
    for enraged,name in [(False,'armored'),(True,'enraged')]:
        base=build(enraged)
        glow=Image.new('RGBA',(256,256))
        for y in range(256):
            for x in range(256):
                seam=(x+(y//7)*3)%29==0 or (y+(x//13)*2)%37==0
                if seam or (enraged and (x+y)%43==0):
                    glow.putpixel((x,y),(255,155 if enraged else 83,28 if enraged else 8,255))
        for y in range(230,234):
            for x in range(230,240):
                base.putpixel((x,y),(255,190,35,255)); glow.putpixel((x,y),(255,190,35,255))
        base.save(TARGET/f'morvath_{name}.png')
        glow.save(TARGET/f'morvath_{name}_glow.png')
