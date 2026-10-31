"""Build Sylthera's verdant and primordial texture atlases."""
from pathlib import Path
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[2]/"src/main/resources/assets/pipican/textures"
def atlas(name,base,mid,light,shadow,phase):
 im=Image.new("RGBA",(256,256));p=im.load()
 for y in range(256):
  for x in range(256):
   v=(x*7+y*17+(x//6)*13+phase*23)%79;c=shadow if x%32<2 or y%32<2 else light if (x*9+y*5+phase*19)%149<10 else mid if v>49 else base;p[x,y]=(*c,255)
 d=ImageDraw.Draw(im)
 for y in range(16,256,32):
  d.arc((32,y-8,96,y+24),0,180,fill=(*light,255),width=2);d.arc((160,y-6,224,y+26),0,180,fill=(*light,255),width=2)
 im.save(ROOT/"entity/boss"/name)
def icon(name,trophy=False):
 im=Image.new("RGBA",(16,16),(0,0,0,0));d=ImageDraw.Draw(im)
 if trophy:d.ellipse((3,2,13,12),fill=(79,142,70,255));d.polygon([(4,10),(8,15),(12,10)],fill=(164,218,126,255));d.line((5,6,11,6),fill=(230,255,196,255),width=2)
 else:d.polygon([(8,1),(13,5),(12,12),(8,15),(4,12),(3,5)],fill=(45,92,53,255));d.line((8,3,8,13),fill=(177,239,113,255),width=2);d.line((5,8,11,8),fill=(229,255,180,255))
 im.resize((256,256),Image.Resampling.NEAREST).save(ROOT/"item"/name)
atlas("sylthera_bloom.png",(44,74,44),(73,125,58),(164,211,102),(24,43,33),0)
atlas("sylthera_primordial.png",(42,82,54),(55,145,75),(182,248,127),(17,48,34),1)
icon("sylthera_primordial_bloom_sigil.png");icon("sylthera_primordial_bloom_trophy.png",True)
