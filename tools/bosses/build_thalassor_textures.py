"""Build Thalassor's pressure-sculpted abyssal atlases and item icons."""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[2]/"src/main/resources/assets/pipican/textures"

def atlas(name,base,mid,light,shadow,phase):
    image=Image.new("RGBA",(256,256));p=image.load()
    for y in range(256):
        for x in range(256):
            grain=(x*9+y*13+(x//6)*17+phase*29)%71
            color=shadow if x%32<2 or y%32<2 else light if (x*7+y*11+phase*23)%151<8 else mid if grain>46 else base
            p[x,y]=(*color,255)
    d=ImageDraw.Draw(image)
    for n in range(0,256,32):d.line((n,0,n,255),fill=(*shadow,255));d.line((0,n,255,n),fill=(*shadow,255))
    for y in (24,88,152,216):
        d.arc((42,y-14,86,y+18),0,180,fill=(*light,255),width=3);d.arc((170,y-12,214,y+20),0,180,fill=(*light,255),width=3)
    image.save(ROOT/"entity/boss"/name)
def icon(name,trophy=False):
    im=Image.new("RGBA",(16,16),(0,0,0,0));d=ImageDraw.Draw(im)
    if trophy:
        d.polygon([(2,2),(6,4),(8,2),(10,4),(14,2),(13,11),(10,14),(6,14),(3,11)],fill=(32,108,132,255));d.line((7,4,10,9,7,12),fill=(176,245,242,255),width=2)
    else:
        d.polygon([(8,1),(13,5),(12,12),(8,15),(4,12),(3,5)],fill=(20,61,85,255));d.polygon([(8,3),(11,6),(9,11),(8,12),(5,7)],fill=(97,215,215,255));d.point((8,5),fill=(230,255,243,255))
    im.resize((256,256),Image.Resampling.NEAREST).save(ROOT/"item"/name)
atlas("thalassor_deep_tyrant.png",(24,65,83),(44,114,132),(123,225,213),(13,37,57),0)
atlas("thalassor_abyssal.png",(17,35,68),(34,76,117),(105,210,237),(7,20,45),1)
icon("thalassor_deep_tyrant_sigil.png");icon("thalassor_deep_tyrant_trophy.png",True)
