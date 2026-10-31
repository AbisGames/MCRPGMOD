"""Build Valthor's two storm atlas skins and readable sigil/trophy icons."""
from pathlib import Path
from PIL import Image, ImageDraw
ROOT = Path(__file__).resolve().parents[2] / "src/main/resources/assets/pipican/textures"
def atlas(name, base, mid, light, shadow, phase):
    image=Image.new("RGBA",(256,256)); pix=image.load()
    for y in range(256):
        for x in range(256):
            grain=(x*7+y*11+(x//5)*17+phase*31)%67
            color=shadow if x%32<2 or y%32<2 else light if (x*13+y*3+phase*19)%149<8 else mid if grain>43 else base
            pix[x,y]=(*color,255)
    d=ImageDraw.Draw(image)
    for offset in range(0,256,32):
        d.line((offset,0,offset,255),fill=(*shadow,255)); d.line((0,offset,255,offset),fill=(*shadow,255))
    # Lightning seams make phase two's blue storm skin legible at normal render distance.
    for y in (24,88,152,216):
        d.line((40,y,74,y-8,86,y+4),fill=(*light,255),width=3)
        d.line((168,y,184,y+8,216,y-3),fill=(*light,255),width=3)
    image.save(ROOT/"entity/boss"/name)
def icon(name,trophy=False):
    im=Image.new("RGBA",(16,16),(0,0,0,0)); d=ImageDraw.Draw(im)
    if trophy:
        d.polygon([(2,2),(6,5),(8,2),(10,5),(14,2),(13,11),(10,14),(6,14),(3,11)],fill=(89,112,151,255))
        d.line((4,8,12,8),fill=(202,232,255,255),width=2); d.line((7,12,9,12),fill=(245,255,255,255))
    else:
        d.polygon([(8,1),(13,5),(12,12),(8,15),(4,12),(3,5)],fill=(35,54,85,255))
        d.polygon([(8,3),(11,6),(9,11),(8,12),(5,7)],fill=(116,194,255,255)); d.point((8,5),fill=(255,255,224,255))
    im.resize((256,256),Image.Resampling.NEAREST).save(ROOT/"item"/name)
atlas("valthor_stormbringer.png",(48,63,87),(78,101,131),(184,222,255),(23,32,56),0)
atlas("valthor_tempest.png",(26,49,83),(47,100,145),(154,230,255),(12,25,50),1)
icon("valthor_stormbringer_sigil.png"); icon("valthor_stormbringer_trophy.png",True)
