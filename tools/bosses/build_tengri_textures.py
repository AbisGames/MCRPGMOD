"""Build Tengri's steppe-bronze atlases and khanate sigil/trophy icons."""
from pathlib import Path
from PIL import Image, ImageDraw

ROOT=Path(__file__).resolve().parents[2]/"src/main/resources/assets/pipican/textures"

def atlas(name,base,mid,light,shadow,phase):
    image=Image.new("RGBA",(256,256));pixels=image.load()
    for y in range(256):
        for x in range(256):
            grain=(x*11+y*7+(x//5)*19+phase*23)%67
            color=shadow if x%32<2 or y%32<2 else light if (x*13+y*5+phase*17)%139<8 else mid if grain>43 else base
            pixels[x,y]=(*color,255)
    draw=ImageDraw.Draw(image)
    for offset in range(0,256,32):
        draw.line((offset,0,offset,255),fill=(*shadow,255));draw.line((0,offset,255,offset),fill=(*shadow,255))
    for y in (28,92,156,220):
        draw.line((40,y,72,y-9,88,y+4),fill=(*light,255),width=3)
        draw.line((168,y,184,y+8,216,y-4),fill=(*light,255),width=3)
    image.save(ROOT/"entity/boss"/name)

def icon(name,trophy=False):
    image=Image.new("RGBA",(16,16),(0,0,0,0));draw=ImageDraw.Draw(image)
    if trophy:
        draw.polygon([(2,2),(6,4),(8,2),(10,4),(14,2),(13,11),(10,14),(6,14),(3,11)],fill=(179,134,48,255))
        draw.line((4,8,12,8),fill=(255,221,119,255),width=2);draw.rectangle((7,12,9,14),fill=(121,76,27,255))
    else:
        draw.polygon([(8,1),(14,5),(12,12),(8,15),(4,12),(2,5)],fill=(105,70,34,255))
        draw.polygon([(8,3),(11,6),(9,11),(8,12),(5,7)],fill=(248,206,103,255));draw.point((8,5),fill=(255,255,214,255))
    image.resize((256,256),Image.Resampling.NEAREST).save(ROOT/"item"/name)

atlas("tengri_champion.png",(83,69,45),(147,118,69),(225,192,111),(42,46,49),0)
atlas("tengri_bannerlord.png",(72,48,38),(142,84,51),(255,194,100),(37,34,39),1)
icon("tengri_champion_sigil.png");icon("tengri_champion_trophy.png",True)
