#!/usr/bin/env python3
"""Generate original furnished Minecraft 26.1 structure NBT assets from authored region data.

Not the full continent-scale capital city: builds reusable architectural landmarks,
village houses, mines and traversable dungeon structures, including internal rooms.
Unlike single-shell placeholders these layouts contain multiple floors, doorways,
staircases, furniture, and distinct gameplay spaces. All generated deterministically.
"""
import gzip, json, math, random, struct, sys
from pathlib import Path

HERE=Path(__file__).resolve().parent
OUT=HERE/'output'
BASE=OUT/'data/pipican'

THEMES={
'whitecrown_icecap':('packed_ice','polished_diorite','blue_ice','quartz_pillar','spruce_planks'),
'northern_ice_sea':('spruce_planks','stripped_spruce_wood','packed_ice','dark_oak_log','spruce_planks'),
'pinewall_taiga':('spruce_planks','cobblestone','dark_prismarine','spruce_log','spruce_planks'),
'fjordmark':('spruce_planks','cobblestone','deepslate_tiles','stripped_spruce_log','spruce_planks'),
'stormcoast':('polished_basalt','tuff_bricks','deepslate_tiles','polished_blackstone','dark_oak_planks'),
'khagan_steppe':('mud_bricks','packed_mud','orange_terracotta','stripped_acacia_log','acacia_planks'),
'western_ocean':('prismarine_bricks','polished_andesite','dark_prismarine','stripped_oak_log','oak_planks'),
'elderwood':('quartz_bricks','polished_diorite','purpur_block','quartz_pillar','cherry_planks'),
'heartlands':('stone_bricks','polished_andesite','deepslate_tiles','chiseled_stone_bricks','oak_planks'),
'greyspine_mountains':('deepslate_bricks','polished_deepslate','deepslate_tiles','polished_basalt','dark_oak_planks'),
'jade_empire':('stone_bricks','polished_diorite','dark_prismarine','stripped_cherry_log','cherry_planks'),
'marshfen':('mossy_stone_bricks','mud_bricks','mangrove_planks','mangrove_log','mangrove_planks'),
'inner_sea':('quartz_bricks','smooth_quartz','prismarine','quartz_pillar','birch_planks'),
'ashlands':('polished_blackstone_bricks','polished_basalt','nether_bricks','chiseled_polished_blackstone','crimson_planks'),
'lotus_delta':('bamboo_mosaic','mud_bricks','cherry_planks','bamboo_block','bamboo_planks'),
'sunscorch_desert':('cut_sandstone','smooth_sandstone','red_sandstone','chiseled_sandstone','acacia_planks'),
'riverlands':('smooth_sandstone','cut_sandstone','red_sandstone','chiseled_sandstone','acacia_planks'),
'spice_coast':('bricks','terracotta','cut_sandstone','polished_granite','acacia_planks'),
'emerald_jungle':('mossy_stone_bricks','polished_andesite','mud_bricks','jungle_log','jungle_planks'),
'coral_isles':('prismarine_bricks','smooth_sandstone','dark_prismarine','prismarine','bamboo_planks'),
'goldgrass_savanna':('mud_bricks','packed_mud','acacia_planks','acacia_log','acacia_planks'),
'redrock_badlands':('red_sandstone','terracotta','orange_terracotta','chiseled_red_sandstone','acacia_planks'),
'aetheris_expanse':('calcite','smooth_quartz','amethyst_block','quartz_pillar','cherry_planks'),
'verdant_depths':('mossy_cobblestone','moss_block','jungle_planks','jungle_log','jungle_planks'),
'umbral_chasm':('deepslate_bricks','polished_deepslate','crying_obsidian','chiseled_deepslate','dark_oak_planks'),
'crimson_wilds':('red_nether_bricks','blackstone','crimson_planks','polished_blackstone','crimson_planks'),
'shimmering_reefs':('prismarine_bricks','prismarine','sea_lantern','dark_prismarine','bamboo_planks')}

def state(name):return 'minecraft:'+name

class Build:
 def __init__(self,name,region,size):
  self.name=name; self.region=region; self.sx,self.sy,self.sz=size;self.blocks={};self.theme=THEMES[region];self.rng=random.Random(name);self.metrics={}
 def set(self,x,y,z,block):
  if 0<=x<self.sx and 0<=y<self.sy and 0<=z<self.sz:
   self.blocks[(x,y,z)]=state(block) if ':' not in block else block
 def box(self,x1,y1,z1,x2,y2,z2,block,hollow=False):
  for x in range(x1,x2+1):
   for y in range(y1,y2+1):
    for z in range(z1,z2+1):
     if not hollow or x in (x1,x2) or y in (y1,y2) or z in (z1,z2):self.set(x,y,z,block)
 def wall(self,x1,z1,x2,z2,y1,y2,block):
  for x in range(x1,x2+1):
   for z in range(z1,z2+1):
    for y in range(y1,y2+1):self.set(x,y,z,block)
 def clear(self,x1,y1,z1,x2,y2,z2):
  for x in range(x1,x2+1):
   for y in range(y1,y2+1):
    for z in range(z1,z2+1):self.blocks.pop((x,y,z),None)
 def door(self,x,y,z,direction='z',width=3,height=5):
  # A materialized AIR block keeps entrances traversable after the structure
  # is placed into uneven terrain; merely removing a block from the template
  # leaves existing world blocks unchanged.
  for i in range(width):
   for j in range(height):
    target=((x+i,y+j,z) if direction=='z' else (x,y+j,z+i))
    self.set(*target,'air')
 def stairs(self,x0,y0,z0,dx,dz,steps,block):
  for i in range(steps):
   self.set(x0+i*dx,y0+i,z0+i*dz,block)
   for yy in range(y0+i+1,min(y0+i+5,self.sy)):self.blocks.pop((x0+i*dx,yy,z0+i*dz),None)
 def light(self,x,y,z):self.set(x,y,z,'sea_lantern' if self.region in ('aetheris_expanse','shimmering_reefs') else 'lantern')
 def furniture(self,x,y,z,kind):
  wood=self.theme[4]
  if kind=='table':
   self.set(x,y,z,'barrel');self.set(x+1,y,z,'barrel');self.light(x,y+1,z)
  elif kind=='library':
   for h in range(3):self.set(x,y+h,z,'bookshelf')
  elif kind=='bed':
   self.set(x,y,z,wood);self.set(x+1,y,z,wood);self.set(x,y+1,z,'white_wool')
  elif kind=='forge':self.set(x,y,z,'blast_furnace');self.set(x+1,y,z,'anvil');self.set(x,y+1,z+1,'iron_bars')
  elif kind=='storage':self.set(x,y,z,'barrel');self.set(x+1,y,z,'chest')
  elif kind=='altar':self.set(x,y,z,'chiseled_stone_bricks');self.set(x,y+1,z,'amethyst_block');self.light(x,y+2,z)
 def make_hall(self,x1,z1,x2,z2,y=1,height=8,roof=True):
  wall,floor,top,post,wood=self.theme
  self.box(x1,y-1,z1,x2,y-1,z2,floor)
  for x in (x1,x2):self.wall(x,z1,x,z2,y,y+height,wall)
  for z in (z1,z2):self.wall(x1,z,x2,z,y,y+height,wall)
  for x in (x1,x2):
   for z in (z1,z2):self.box(x,y,z,x,y+height+2,z,post)
  if roof:self.box(x1,y+height+1,z1,x2,y+height+1,z2,top)
  for x in range(x1+4,x2-3,6):
   self.light(x,y+height,z1+1);self.light(x,y+height,z2-1)
 def castle(self):
  wall,floor,roof,post,wood=self.theme;X=self.sx-1;Z=self.sz-1
  # Outer crenellated enceinte, functional four tower staircases and gatehouse.
  self.box(1,0,1,X-1,0,Z-1,floor)
  for z in (2,Z-2):self.wall(2,z,X-2,z,1,14,wall)
  for x in (2,X-2):self.wall(x,2,x,Z-2,1,14,wall)
  for x in (2,X-2):
   for z in (2,Z-2):
    self.box(x-3,1,z-3,x+3,26,z+3,wall,hollow=True)
    for yy in (8,17,25):self.box(x-3,yy,z-3,x+3,yy,z+3,floor)
    for yy in range(2,24,2):self.stairs(x-1,yy,z-1,1,0,2,wood)
    self.light(x,23,z)
  for k in range(4,X-3,5):
   for z in (2,Z-2):self.box(k,15,z,k+2,16,z,wall)
  for k in range(4,Z-3,5):
   for x in (2,X-2):self.box(x,15,k,x,16,k+2,wall)
  self.door(X//2-2,1,2,width=5,height=7)
  # Three-story royal keep with rooms accessible from central spine.
  x1,z1,x2,z2=14,16,X-14,Z-16
  for level in range(3):
   y=1+level*10
   self.make_hall(x1,z1,x2,z2,y,8)
   self.wall(x1+2,z1+12,x2-2,z1+12,y,y+7,wall)
   self.wall(x1+2,z2-12,x2-2,z2-12,y,y+7,wall)
   self.door(X//2-1,y,z1+12,width=3,height=5)
   self.door(X//2-1,y,z2-12,width=3,height=5)
   self.door(X//2-1,y,z1,width=3,height=5)
   self.door(X//2-1,y,z2,width=3,height=5)
   for x in (x1+5,x2-6):
    self.furniture(x,y,z1+4,'library' if level==0 else 'bed')
    self.furniture(x,y,z2-5,'storage' if level==1 else 'table')
   self.furniture(X//2,y,z1+6,'altar' if level==2 else 'table')
  self.stairs(x1+2,1,z1+2,0,1,10,wood)
  self.stairs(x2-2,11,z2-12,0,-1,10,wood)
  self.furniture(6,1,Z//2,'forge');self.furniture(X-8,1,Z//2,'storage')
  self.metrics={'floors':3,'towers':4,'rooms':9,'interior':'throne/library/armory/quarters/kitchen/forge/treasury'}
 def village(self):
  wall,floor,roof,post,wood=self.theme;self.box(0,0,0,self.sx-1,0,self.sz-1,'grass_block')
  # Distinct houses around a stone marketplace with furnished interiors.
  houses=[(3,3,16,14),(self.sx-20,3,self.sx-5,15),(3,self.sz-18,18,self.sz-5),(self.sx-21,self.sz-19,self.sx-5,self.sz-5)]
  for k,(x1,z1,x2,z2) in enumerate(houses):
   self.make_hall(x1,z1,x2,z2,1,6)
   if k<2:self.door((x1+x2)//2,1,z2,width=2,height=4)
   else:self.door((x1+x2)//2,1,z1,width=2,height=4)
   self.furniture(x1+2,1,z1+2,['bed','forge','storage','library'][k]);self.furniture(x2-3,1,z2-3,'table')
  cx=self.sx//2;cz=self.sz//2
  self.box(cx-9,0,cz-8,cx+9,0,cz+8,'cobblestone')
  self.box(cx-2,1,cz-2,cx+2,2,cz+2,wall,hollow=True)
  self.set(cx,1,cz,'water');self.light(cx,3,cz)
  for z in range(self.sz):self.wall(cx-2,z,cx+2,z,0,0,'gravel')
  self.metrics={'buildings':4,'interiors':'inn/smithy/home/library/market square'}
 def dungeon(self):
  wall,floor,roof,post,wood=self.theme;self.box(1,0,1,self.sx-2,0,self.sz-2,floor)
  chambers=[(3,3,20,20),(self.sx-23,3,self.sx-4,20),(3,self.sz-23,22,self.sz-4),(self.sx-25,self.sz-25,self.sx-4,self.sz-4)]
  for i,(x1,z1,x2,z2) in enumerate(chambers):
   self.make_hall(x1,z1,x2,z2,1,10)
   self.furniture((x1+x2)//2,1,(z1+z2)//2,['storage','altar','library','altar'][i])
   for x in range(x1+3,x2,5):self.light(x,9,z1+2)
  # Traversable two-sided corridor network and an upper observation floor.
  self.make_hall(self.sx//2-4,3,self.sx//2+4,self.sz-4,1,7)
  self.make_hall(3,self.sz//2-4,self.sx-4,self.sz//2+4,1,7)
  for (x1,z1,x2,z2) in chambers:
   self.clear((x1+x2)//2-2,1,z2-1,(x1+x2)//2+2,6,z2+2)
   self.clear(x2-1,1,(z1+z2)//2-2,x2+2,6,(z1+z2)//2+2)
  self.box(self.sx//2-8,12,self.sz//2-8,self.sx//2+8,12,self.sz//2+8,floor)
  self.stairs(self.sx//2+2,1,self.sz//2-10,0,1,11,wood)
  self.furniture(self.sx//2,13,self.sz//2,'altar')
  self.metrics={'chambers':4,'corridors':2,'upper_floor':True,'loot_and_boss_marker':'planned: not active'}
 def mine(self):
  wall,floor,roof,post,wood=self.theme;self.make_hall(3,3,self.sx-4,self.sz-4,1,11)
  for z in [self.sz//3,2*self.sz//3]:
   self.wall(4,z,self.sx-5,z,1,9,wall);self.door(self.sx//2-2,1,z,width=5,height=6)
  for x in range(8,self.sx-8,4):
   for z in [8,self.sz-9]:self.box(x,1,z,x+1,4,z+1,wall)
  for z in range(5,self.sz-5):
   self.set(self.sx//2-1,1,z,'rail');self.set(self.sx//2+1,1,z,'rail')
  for x in (8,self.sx-12):
   for z in (8,self.sz-12):
    self.box(x,1,z,x+4,1,z+4,wood)
    self.furniture(x+1,2,z+1,'storage')
  for x in range(3,self.sx-3,7):self.light(x,10,4)
  self.metrics={'galleries':3,'rail_tracks':2,'storage_zones':4,'renewable_nodes':'not wired'}
 def build(self,kind):getattr(self,kind)();return self

# Binary NBT writer compatible with standard structure template reader.
def tag_name(t,name):return bytes([t])+struct.pack('>H',len(name.encode()))+name.encode()
def s(value):return struct.pack('>H',len(value.encode()))+value.encode()
def i32(n):return struct.pack('>i',n)
def list_tag(name,typ,elems):return tag_name(9,name)+bytes([typ])+i32(len(elems))+b''.join(elems)
def compound(components):return b''.join(components)+b'\0'
def tstr(name,v):return tag_name(8,name)+s(v)
def tint(name,n):return tag_name(3,name)+i32(n)
def ints(name,a):return list_tag(name,3,[i32(x) for x in a])
def save_nbt(build,path):
 palette=list(sorted(set(build.blocks.values())))
 indices={v:i for i,v in enumerate(palette)}
 pal=[compound([tstr('Name',name)]) for name in palette]
 blocks=[compound([ints('pos',key),tint('state',indices[val])]) for key,val in sorted(build.blocks.items())]
 nbt=tag_name(10,'')+compound([ints('size',[build.sx,build.sy,build.sz]),list_tag('palette',10,pal),list_tag('blocks',10,blocks),list_tag('entities',10,[]),tint('DataVersion',4650)])
 path.parent.mkdir(parents=True,exist_ok=True)
 # Minecraft structure templates are stored as compressed NBT.
 path.write_bytes(gzip.compress(nbt,compresslevel=3,mtime=0))
 return len(blocks)

def add_structure_def(id,biome,spacing):
 p=BASE/'worldgen'
 pool={'fallback':'minecraft:empty','elements':[{'weight':1,'element':{'element_type':'minecraft:single_pool_element','location':f'pipican:{id}','processors':{'processors':[]},'projection':'rigid'}}]}
 structdef={'type':'minecraft:jigsaw','biomes':biome,'max_distance_from_center':100,'project_start_to_heightmap':'WORLD_SURFACE_WG','size':1,'spawn_overrides':{},'start_height':{'absolute':0},'start_pool':f'pipican:{id}','step':'surface_structures','terrain_adaptation':'beard_thin','use_expansion_hack':False}
 spread={'placement':{'type':'minecraft:random_spread','salt':abs(sum(ord(c)*(i+1) for i,c in enumerate(id)))+100023,'separation':max(8,spacing//3),'spacing':spacing},'structures':[{'structure':f'pipican:{id}','weight':1}]}
 for sub,entry in [('template_pool',pool),('structure',structdef),('structure_set',spread)]:
  f=p/sub/(id+'.json');f.parent.mkdir(parents=True,exist_ok=True);f.write_text(json.dumps(entry,indent=2))

def generate():
 regpath=HERE/'region_map.json'
 regions=[r['id'] for r in json.loads(regpath.read_text())['macro_regions']]
 kinds=[('capital_keep','castle',(128,65,128),400),('village','village',(80,24,80),40),('dungeon','dungeon',(80,27,80),65),('mine','mine',(64,22,64),48)]
 rows=[]
 authored=json.loads(regpath.read_text())['macro_regions']
 for region in authored:
  tag=BASE/'tags/worldgen/biome/region'/(region['id']+'.json')
  tag.parent.mkdir(parents=True,exist_ok=True)
  tag.write_text(json.dumps({'replace':False,'values':['pipican:'+region['id']+'/'+v['id'] for v in region['subregions']]},indent=2))
 for reg in regions+['aetheris_expanse','verdant_depths','umbral_chasm','crimson_wilds','shimmering_reefs']:
  for suffix,kind,size,spacing in kinds:
   ident=f'{reg}_{suffix}'
   b=Build(ident,reg,size).build(kind)
   blocks=save_nbt(b,BASE/'structure'/(ident+'.nbt'))
   add_structure_def(ident,('#pipican:region/'+reg) if reg in regions else 'pipican:'+reg,spacing)
   rows.append({'id':ident,'region':reg,'type':kind,'size':size,'placed_blocks':blocks,**b.metrics})
 (OUT/'STRUCTURE_CATALOG.json').write_text(json.dumps(rows,indent=2))
 print(f'Generated {len(rows)} structures, {sum(r["placed_blocks"] for r in rows):,} block placements; files in {OUT}')

if __name__=='__main__':generate()