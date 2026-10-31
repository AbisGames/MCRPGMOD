"""Build Lumynara's abyssal and oracle texture atlases."""
from pathlib import Path
from PIL import Image,ImageDraw
ROOT=Path(__file__).resolve().parents[2]/"src/main/resources/assets/pipican/textures"
def atlas(name,base,mid,light,shadow,phase):
 im=Image.new("RGBA",(256,256));p=im.load()
 for y in range(256):
  for x in range(256):
   v=(x*11+y*7+(x//5)*19+phase*31)%89;c=shadow if x%32<2 or y%32<2 else light if (x*3+y*13+phase*17)%157<11 else mid if v>53 else base;p[x,y]=(*c,255)
 d=ImageDraw.Draw(im)
 for y in range(20,256,32):
  d.arc((38,y-12,88,y+20),180,360,fill=(*light,255),width=3);d.arc((168,y-10,218,y+22),0,180,fill=(*light,255),width=3)
 im.save(ROOT/"entity/boss"/name)
def icon(name,trophy=False):
 im=Image.new("RGBA",(16,16),(0,0,0,0));d=ImageDraw.Draw(im)
 if trophy:d.ellipse((3,2,13,11),fill=(45,91,130,255));d.polygon([(4,10),(8,15),(12,10)],fill=(111,224,239,255));d.ellipse((6,4,10,8),fill=(210,255,255,255))
 else:d.polygon([(8,1),(13,5),(12,12),(8,15),(4,12),(3,5)],fill=(29,57,92,255));d.ellipse((6,4,10,9),fill=(123,238,255,255));d.point((8,6),fill=(255,255,220,255))
 im.resize((256,256),Image.Resampling.NEAREST).save(ROOT/"item"/name)
atlas("lumynara_abyssal.png",(24,50,78),(35,84,125),(99,224,250),(12,25,48),0)
atlas("lumynara_oracle.png",(34,38,90),(63,74,156),(177,193,255),(15,18,51),1)
icon("lumynara_abyssal_oracle_sigil.png");icon("lumynara_abyssal_oracle_trophy.png",True)
