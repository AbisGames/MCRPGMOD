"""Build runic forge atlases and inventory icons."""
from pathlib import Path
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[2]/"src/main/resources/assets/pipican/textures"
def atlas(name,base,mid,light,shadow,phase):
 im=Image.new("RGBA",(256,256));px=im.load()
 for y in range(256):
  for x in range(256):
   n=(x*13+y*7+(x//5)*19+phase*31)%83;c=shadow if x%32<2 or y%32<2 else light if (x*5+y*11+phase*17)%137<9 else mid if n>52 else base;px[x,y]=(*c,255)
 d=ImageDraw.Draw(im)
 for y in range(18,256,32):
  d.line((0,y,255,y),fill=(*shadow,255),width=1)
  for x in range(8,256,32):d.polygon([(x,y-4),(x+4,y),(x,y+4),(x-4,y)],outline=(*light,255))
 im.save(ROOT/"entity/boss"/name)
def icon(name,trophy=False):
 im=Image.new("RGBA",(16,16),(0,0,0,0));d=ImageDraw.Draw(im)
 if trophy:d.polygon([(2,3),(6,4),(8,2),(10,4),(14,3),(13,10),(10,13),(6,13),(3,10)],fill=(86,73,112,255));d.line((8,4,8,14),fill=(222,185,255,255),width=2);d.line((5,14,11,14),fill=(222,185,255,255),width=2)
 else:d.polygon([(8,1),(13,5),(12,12),(8,15),(4,12),(3,5)],fill=(53,45,81,255));d.line((8,3,8,13),fill=(173,132,245,255),width=2);d.line((5,8,11,8),fill=(229,210,255,255),width=1)
 im.resize((256,256),Image.Resampling.NEAREST).save(ROOT/"item"/name)
atlas("durmagon_runic.png",(50,45,64),(91,76,115),(204,168,246),(26,24,38),0)
atlas("durmagon_awakened.png",(60,36,54),(119,57,91),(255,139,180),(30,18,34),1)
icon("durmagon_runic_titan_sigil.png");icon("durmagon_runic_titan_trophy.png",True)
