#!/usr/bin/env python3
"""Create original, furnished Minecraft structures from the authoritative 331 pool manifest.

Produces reproducible binary Java-Edition .nbt templates, jigsaw pool JSON,
biome tags, a command-installable capital kit, and a validation catalog.
This is a content-generation tool, not a promise of completed in-game placement.\nThe 26.1.2 GameTest suite separately verifies selected binary template loads.
"""
import gzip
import hashlib
import json
import math
import random
import struct
import sys
from collections import Counter
from pathlib import Path
from legacy import Build, THEMES

HERE=Path(__file__).resolve().parent
OUT=HERE/'output'
BASE=OUT/'data/pipican'
# Local copies are convenient for standalone use; in the mod repository read
# the canonical files that are already checked into resources.
ASSETS=HERE if (HERE/'region_map.json').is_file() else HERE.parents[1]/'src/main/resources/data/pipican/worldgen'
REGIONS=json.loads((ASSETS/'region_map.json').read_text())['macro_regions']
REGIONIDS={r['id'] for r in REGIONS}
POOLS=json.loads((ASSETS/('asset_manifest.json' if (ASSETS/'asset_manifest.json').exists() else 'structure_pools_manifest.json')).read_text())['pools']
SITES=json.loads((ASSETS/'settlements.json').read_text())['settlements']
EXTRAS=['aetheris_expanse','verdant_depths','umbral_chasm','crimson_wilds','shimmering_reefs']
ORE={
'whitecrown_icecap':'frostcrystal','northern_ice_sea':'glacierite',
'pinewall_taiga':'wintersteel','fjordmark':'stormsilver',
'stormcoast':'thunderbasalt','khagan_steppe':'skyiron',
'western_ocean':'tideglass','elderwood':'mooncrystal',
'heartlands':'royal_silver','greyspine_mountains':'runic_iron',
'jade_empire':'jade_essence','marshfen':'mirestone',
'inner_sea':'pearlsilver','ashlands':'cinderite','lotus_delta':'lotus_opal',
'sunscorch_desert':'sunglass','riverlands':'sunstone',
'spice_coast':'saffron_gold','emerald_jungle':'verdantite',
'coral_isles':'tidalite','goldgrass_savanna':'dawnbronze',
'redrock_badlands':'redrock_iron','aetheris_expanse':'skycrystal',
'verdant_depths':'lifebloom_crystal','umbral_chasm':'umbrite',
'crimson_wilds':'bloodstone','shimmering_reefs':'abyssal_shard'}
ARCHETYPE={
'elderwood':'elven','greyspine_mountains':'dwarven','heartlands':'medieval',
'whitecrown_icecap':'ice','northern_ice_sea':'nordic','pinewall_taiga':'nordic',
'fjordmark':'nordic','stormcoast':'volcanic','ashlands':'volcanic',
'jade_empire':'eastern','lotus_delta':'eastern','marshfen':'swamp',
'sunscorch_desert':'desert','riverlands':'desert','spice_coast':'desert',
'coral_isles':'island','western_ocean':'island','inner_sea':'medieval',
'emerald_jungle':'jungle','goldgrass_savanna':'savanna','redrock_badlands':'canyon',
'khagan_steppe':'steppe','aetheris_expanse':'aether','verdant_depths':'jungle',
'umbral_chasm':'underground','crimson_wilds':'volcanic','shimmering_reefs':'island'}

class Workshop(Build):
    """Parameterised blocks with actual rooms, access shafts, roofing and furnishing."""
    def __init__(self,name,region,size):
        super().__init__(name,region,size)
        self.style=ARCHETYPE[region]
        self.acc=Counter()
        self.block_entities={}
        self.rand=random.Random(int(hashlib.sha256(name.encode()).hexdigest()[:16],16))
    def air(self,x1,y1,z1,x2,y2,z2):
        # Explicit air clears natural terrain when a template is embedded in slopes.
        for x in range(max(0,x1),min(self.sx,x2+1)):
            for y in range(max(0,y1),min(self.sy,y2+1)):
                for z in range(max(0,z1),min(self.sz,z2+1)):
                    self.set(x,y,z,'air')
    def path(self,x1,z1,x2,z2,base=0):
        xlo,xhi=sorted((x1,x2));zlo,zhi=sorted((z1,z2))
        for x in range(xlo,xhi+1):
            for z in range(zlo,zhi+1):self.set(x,base,z,self.theme[1])
    def column(self,x,z,y0,y1,kind=None):
        self.box(x,y0,z,x,y1,z,kind or self.theme[3])
    def window(self,x,y,z,along='z'):
        if along=='z':
            self.box(x,y,z,x+1,y+2,z,'glass_pane')
        else:self.box(x,y,z,x,y+2,z+1,'glass_pane')
    def dome(self,x,z,r,y,block=None):
        roof=block or self.theme[2]
        for dy in range(0,max(2,r//2)):
            rad=max(1,round(r*math.sqrt(max(0.02,1-(dy/(r/2))**2))))
            for xx in range(x-rad,x+rad+1):
                for zz in range(z-rad,z+rad+1):
                    if (xx-x)**2+(zz-z)**2<=rad*rad and (xx-x)**2+(zz-z)**2 >= (rad-1)**2:
                        self.set(xx,y+dy,zz,roof)
        self.set(x,y+r//2,z,'sea_lantern')
    def spire(self,x,z,r,y,h):
        stone=self.theme[0];roof=self.theme[2]
        for yy in range(y,y+h):
            rad=max(1,int(r*(1-(yy-y)/h)))
            for a in range(-rad,rad+1):
                self.set(x+a,yy,z-rad,roof);self.set(x+a,yy,z+rad,roof)
                self.set(x-rad,yy,z+a,roof);self.set(x+rad,yy,z+a,roof)
        self.light(x,y+h,z)
    def detailed_building(self,x,z,w,d,floors=2,role='home',rotation=0):
        wall,floor,roof,post,wood=self.theme
        x2=min(self.sx-2,x+w-1);z2=min(self.sz-2,z+d-1)
        if x<1 or z<1 or x2-x<9 or z2-z<9:return
        sy=7
        for level in range(floors):
            y=1+level*sy
            # Room volume is explicitly air-filled, with a solid floor/roof.
            self.box(x,y-1,z,x2,y-1,z2,floor)
            self.air(x+1,y,z+1,x2-1,y+5,z2-1)
            for xx in (x,x2):self.wall(xx,z,xx,z2,y,y+5,wall)
            for zz in (z,z2):self.wall(x,zz,x2,zz,y,y+5,wall)
            for xx in (x,x2):
                for zz in (z,z2):self.column(xx,zz,y,y+6,post)
            self.box(x,y+6,z,x2,y+6,z2,roof)
            self.door((x+x2)//2-1,y,z if rotation%2==0 else z2, width=3,height=4)
            self.window(x+1,y+2,z,along='z')
            self.window(x2-2,y+2,z2,along='z')
            for xx in range(x+3,x2-2,6):self.light(xx,y+5,z+2)
            interior=['library','bed','storage','table','forge','altar'][int(hashlib.sha256((role+str(level)).encode()).hexdigest()[:4],16)%6]
            if role in ('smithy','forge'):interior='forge'
            elif role in ('library','archive'):interior='library'
            elif role in ('shrine','temple'):interior='altar'
            elif role in ('inn','barracks'):interior='bed'
            self.furniture(x+3,y,z+3,interior)
            self.furniture(x2-4,y,z2-4,'storage' if level>0 else 'table')
        # Interior ladder with a clear 1x1 vertical shaft for all upper floors.
        if floors>1:
            self.box(x+2,1,z+1,x+2,1+floors*sy,z+1,wood)
            for y in range(1,1+floors*sy):
                self.set(x+2,y,z+2,'ladder[facing=south]')
                self.set(x+2,y,z+3,'air')
            for lv in range(1,floors):self.set(x+2,lv*sy,z+2,'air')
        if self.style in ('elven','eastern','aether'):
            for xx in range(x+2,x2-1,3):self.set(xx,1+floors*sy,z+2,roof)
            self.spire((x+x2)//2,(z+z2)//2,3,1+floors*sy,max(4,floors*3))
        elif self.style in ('desert','island','swamp'):
            self.dome((x+x2)//2,(z+z2)//2,min(w,d)//3,1+floors*sy,roof)
        else:
            for xx in range(x+2,x2,4):
                self.set(xx,1+floors*sy,z,roof);self.set(xx,1+floors*sy,z2,roof)
        self.acc['buildings']+=1
        self.acc['furnished_rooms']+=floors
    def core_castle(self):
        W=self.sx;D=self.sz;wall,floor,roof,post,wood=self.theme
        self.box(1,0,1,W-2,0,D-2,floor)
        # Monumental perimeter with four liveable towers, gate and battlements.
        for xx in (3,W-4):self.wall(xx,3,xx,D-4,1,17,wall)
        for zz in (3,D-4):self.wall(3,zz,W-4,zz,1,17,wall)
        self.door(W//2-3,1,3,width=7,height=10)
        for xx in (7,W-8):
            for zz in (7,D-8):
                r=6
                self.box(xx-r,1,zz-r,xx+r,29,zz+r,wall,hollow=True)
                self.air(xx-r+1,2,zz-r+1,xx+r-1,28,zz+r-1)
                for yy in (1,9,17,25):self.box(xx-r,yy,zz-r,xx+r,yy,zz+r,floor)
                for yy in range(2,27):self.set(xx+r-2,yy,zz,'ladder[facing=east]')
                for yy in (5,13,21):self.window(xx-1,yy,zz-r)
                self.spire(xx,zz,r,29,12 if self.style not in ('elven','aether') else 24)
                self.acc['towers']+=1
        for xx in range(10,W-10,5):
            for zz in (3,D-4):self.box(xx,18,zz,xx+2,20,zz,wall)
        for zz in range(10,D-10,5):
            for xx in (3,W-4):self.box(xx,18,zz,xx,20,zz+2,wall)
        # Enormous central great hall with four levels and meaningful rooms.
        x1,z1=21,23;x2,z2=W-22,D-22
        for k in range(4):
            y=1+11*k
            self.box(x1,y-1,z1,x2,y-1,z2,floor)
            self.air(x1+1,y,z1+1,x2-1,y+8,z2-1)
            for xx in (x1,x2):self.wall(xx,z1,xx,z2,y,y+9,wall)
            for zz in (z1,z2):self.wall(x1,zz,x2,zz,y,y+9,wall)
            self.box(x1,y+10,z1,x2,y+10,z2,roof)
            for xx in (x1+5,x2-5):
                for zz in range(z1+5,z2-4,13):self.column(xx,zz,y,y+9,post)
            # Two gallery wings and a processional central aisle.
            if k in (0,1,2):
                self.wall(x1+17,z1+2,x1+17,z2-2,y,y+8,wall)
                self.wall(x2-17,z1+2,x2-17,z2-2,y,y+8,wall)
                for zz in (z1+9,(z1+z2)//2,z2-9):
                    self.door(x1+17,y,zz,'x',width=4,height=6)
                    self.door(x2-17,y,zz,'x',width=4,height=6)
            self.door(W//2-3,y,z1,width=7,height=7)
            self.door(W//2-3,y,z2,width=7,height=7)
            for xx in (x1+5,x2-8):
                for zz in (z1+6,z1+25,z2-18):
                    role=['library','forge','storage','bed','table','altar'][(k+xx+zz)//5%6]
                    self.furniture(xx,y,zz,role);self.acc['furnished_rooms']+=1
            self.furniture(W//2,y,z2-9,'altar' if k==3 else 'table')
        # Two opposing protected ladder shafts with 1x1 landings.
        for xx,zz in ((x1+3,z1+3),(x2-3,z2-3)):
            for yy in range(1,44):
                self.set(xx,yy,zz,'ladder[facing=north]')
                self.set(xx,yy,zz+1,'air')
            self.acc['vertical_connections']+=1
        # Concentric courtyard pools, archery platform, palace gardens.
        for xx in range(15,W-15,7):
            for zz in (13,D-14):self.set(xx,1,zz,'lantern')
        for xx in (12,W-13):
            for zz in range(20,D-19,7):
                self.set(xx,1,zz,'oak_leaves')
        if self.style=='elven':
            self.elven_terrace()
        elif self.style=='dwarven':self.dwarven_gate()
        elif self.style in ('eastern','aether'):self.pagoda_roof()
        elif self.style in ('volcanic','underground'):self.obsidian_throne()
        elif self.style=='desert':self.desert_court()
        self.acc['floors']=4
        self.acc['gate_width']=7
    def elven_terrace(self):
        # White arcades, elevated living bridges, tree-lined reflection basins.
        wall=self.theme[0];post=self.theme[3]
        for xx in range(19,self.sx-18,12):
            for zz in (15,self.sz-16):
                self.column(xx,zz,2,16,post)
                self.column(xx+5,zz,2,16,post)
                for x in range(xx,xx+6):self.set(x,17,zz,wall)
        for xx in (15,self.sx-16):
            for zz in range(25,self.sz-24):
                self.set(xx,20,zz,'quartz_bricks')
                if zz%5==0:self.set(xx,21,zz,'sea_lantern')
        for xx in range(self.sx//2-5,self.sx//2+6):
            for zz in (13,self.sz-14):self.set(xx,1,zz,'water')
        self.acc['arcade_sections']=12
    def dwarven_gate(self):
        for y in range(6,20):
            for x in range(self.sx//2-12,self.sx//2+13):
                if abs(x-self.sx//2)+y//2<22:
                    self.set(x,y,13,'chiseled_deepslate')
        for x in range(self.sx//2-6,self.sx//2+7):
            for y in range(1,12):self.set(x,y,13,'air')
        self.furniture(self.sx//2-15,1,17,'forge')
        self.furniture(self.sx//2+12,1,17,'forge')
        self.acc['mountain_gates']=1
    def pagoda_roof(self):
        for y in (46,50,54):
            for x in range(28,self.sx-28):
                for z in (27,self.sz-28):self.set(x,y,z,self.theme[2])
            for z in range(28,self.sz-28):
                for x in (27,self.sx-28):self.set(x,y,z,self.theme[2])
        self.spire(self.sx//2,self.sz//2,8,48,16)
        self.acc['pagoda_levels']=3
    def obsidian_throne(self):
        x=self.sx//2;z=self.sz-28
        self.box(x-5,1,z-5,x+5,1,z+5,'magma_block')
        self.box(x-2,2,z-2,x+2,5,z+2,'polished_blackstone_bricks')
        self.set(x,6,z,'soul_lantern');self.acc['dark_altar']=1
    def desert_court(self):
        x=self.sx//2;z=self.sz//2
        self.dome(x,z,12,47,'smooth_sandstone')
        for xx in (13,self.sx-14):
            for zz in (13,self.sz-14):self.set(xx,1,zz,'water')
        self.acc['domes']=1
    def district(self,category='market'):
        wall,floor,roof,post,wood=self.theme
        W=self.sx;D=self.sz
        self.box(0,0,0,W-1,0,D-1,'grass_block' if self.style not in ('desert','volcanic','underground') else floor)
        cx,cz=W//2,D//2
        for x in range(cx-3,cx+4):self.path(x,0,x,D-1)
        for z in range(cz-3,cz+4):self.path(0,z,W-1,z)
        entries=[(4,4,24,25),(W-29,4,W-5,26),(4,D-30,27,D-5),(W-29,D-30,W-5,D-5)]
        roles={
          'temple':['shrine','library','shrine','archive'],
          'market':['smithy','inn','storage','home'],
          'residences':['home','inn','home','home'],
          'military':['barracks','forge','storage','barracks'],
          'harbor':['storage','inn','smithy','storage'],
          'academy':['library','archive','shrine','library'],
          'artisan':['forge','smithy','storage','inn'],
          'garden':['shrine','home','home','storage']}
        # Pool identifiers and city district names seed a deterministic,
        # genuinely different footprint, doorway orientation and roof skyline.
        for i,(x1,z1,x2,z2) in enumerate(entries):
            role=roles.get(category,roles['market'])[i]
            shiftx=self.rand.randint(-3,3)
            shiftz=self.rand.randint(-3,3)
            sx=min(max(2,x1+shiftx),self.sx-32)
            sz=min(max(2,z1+shiftz),self.sz-32)
            sx2=max(13,x2-x1+1+self.rand.randrange(-3,4))
            sz2=max(13,z2-z1+1+self.rand.randrange(-3,4))
            floors=max(1,min(3,(2 if category!='military' else 3)+self.rand.choice([-1,0,0,1])))
            self.detailed_building(sx,sz,sx2,sz2,floors,role,(i+self.rand.randrange(2))%4)
        # Subregion-specific monuments and gathering spots distinguish villages
        # even if two settlements share the same macro culture and tier.
        accent=self.theme[3]
        for j in range(3+self.rand.randrange(5)):
            mx=cx+self.rand.randint(-15,15)
            mz=cz+self.rand.randint(-15,15)
            if abs(mx-cx)<6 and abs(mz-cz)<6:continue
            self.column(mx,mz,1,3+self.rand.randint(0,7),accent)
            if j%2==0:self.light(mx,5,mz)
        self.acc['local_monuments']+=j+1
        if category in ('garden','temple'):
            for xx in range(cx-8,cx+9):
                for zz in range(cz-8,cz+9):
                    if abs(xx-cx)==8 or abs(zz-cz)==8:self.set(xx,1,zz,'oak_leaves')
            self.furniture(cx,1,cz,'altar');self.acc['gardens']+=1
        elif category=='harbor':
            for x in range(cx-13,cx+14):
                for z in range(cz-13,cz+14):
                    if (x-cx)**2+(z-cz)**2<169:self.set(x,1,z,'water')
            for x in range(cx-2,cx+3):
                for z in range(cz-12,cz+13):self.set(x,2,z,wood)
            self.acc['harbor_basin']+=1
        elif category=='market':
            for xx in range(cx-12,cx+13,5):
                for zz in range(cz-12,cz+13,5):self.set(xx,1,zz,'barrel');self.set(xx,2,zz,'lantern')
        else:self.furniture(cx,1,cz,'table')
        self.acc['paths']=2
    def settlement(self,tier):
        # Fully furnished interior village/town/city kit; scaled by settlement tier.
        self.district({'capital':'temple','city':'market','town':'artisan','village':'residences','hamlet':'residences','outpost':'military'}[tier])
        if tier in ('capital','city','outpost'):
            wall,_,roof,_,_=self.theme
            for zz in (2,self.sz-3):self.wall(2,zz,self.sx-3,zz,1,5,wall)
            for xx in (2,self.sx-3):self.wall(xx,2,xx,self.sz-3,1,5,wall)
            self.door(self.sx//2-2,1,2,width=5,height=5)
            self.door(self.sx//2-2,1,self.sz-3,width=5,height=5)
            self.acc['defensive_wall']=1
    def cave(self,kind):
        # Underground plan with traversable three-aisle corridors, rooms and floors.
        wall,floor,roof,post,wood=self.theme
        W=self.sx;D=self.sz
        self.box(1,0,1,W-2,0,D-2,floor)
        self.air(2,1,2,W-3,13,D-3)
        self.box(1,14,1,W-2,14,D-2,roof)
        for xx in (1,W-2):self.wall(xx,1,xx,D-2,1,13,wall)
        for zz in (1,D-2):self.wall(1,zz,W-2,zz,1,13,wall)
        cx,cz=W//2,D//2
        room_bounds=[(4,4,25,24),(W-27,4,W-5,24),(4,D-26,27,D-5),(W-28,D-27,W-5,D-5)]
        for i,(x1,z1,x2,z2) in enumerate(room_bounds):
            for x in (x1,x2):self.wall(x,z1,x,z2,1,12,wall)
            for z in (z1,z2):self.wall(x1,z,x2,z,1,12,wall)
            self.door((x1+x2)//2-2,1,z2 if z2<cz else z1,width=5,height=6)
            self.door(x2 if x2<cx else x1,1,(z1+z2)//2-2,'x',5,6)
            kindrole=['storage','library','forge','altar'][i]
            if kind=='mine':kindrole=['forge','storage','storage','forge'][i]
            self.furniture(x1+6,1,z1+6,kindrole)
            self.furniture(x2-6,1,z2-6,'storage')
            self.acc['rooms']+=1
        # Reinforced vault with boss reward room and recoverable ore nodes.
        self.box(cx-13,1,cz-13,cx+13,1,cz+13,floor)
        self.furniture(cx,1,cz,'altar' if kind=='dungeon' else 'forge')
        for zz in range(6,D-6):
            if kind=='mine':
                self.set(cx-2,1,zz,'rail');self.set(cx+2,1,zz,'rail')
            if zz%6==0:
                self.light(cx-5,10,zz);self.light(cx+5,10,zz)
        if kind=='mine':
            ore=ORE[self.region]
            for k in range(22):
                x,z=self.rand.randrange(6,W-6),self.rand.randrange(6,D-6)
                if x not in range(cx-7,cx+8) and z not in range(cz-7,cz+8):
                    self.set(x,2,z,f'pipican:{ore}_ore')
                    self.acc['regional_ore_nodes']+=1
            self.acc['rails']=2
        else:
            # Loot position and boss chamber fixed for server-side reward system.
            for dx in (-2,0,2):
                pos=(cx+dx,2,cz+8)
                self.set(*pos,'chest[facing=north]')
                self.block_entities[pos]={'LootTable':f'pipican:chests/{self.region}_dungeon'}
            self.acc['reward_chests']=3
        self.acc['main_chamber']=1
    def make(self,typ):
        if typ=='castle':self.core_castle()
        elif typ in ('dungeon','mine'):self.cave(typ)
        elif typ in ('market','residences','temple','military','academy','harbor','artisan','garden'):self.district(typ)
        else:self.settlement(typ)
        self.metrics=dict(self.acc);return self

# Minecraft Java Edition structure NBT. Palette entries contain compound Properties
# for directional blocks; the previous flat-string writer lost those blockstates.
def pstr(name,value):return bytes([8])+struct.pack('>H',len(name))+name.encode()+struct.pack('>H',len(value.encode()))+value.encode()
def pint(name,value):return bytes([3])+struct.pack('>H',len(name))+name.encode()+struct.pack('>i',value)
def tag(name,kind):return bytes([kind])+struct.pack('>H',len(name))+name.encode()
def ints(name,vals):return tag(name,9)+bytes([3])+struct.pack('>i',len(vals))+b''.join(struct.pack('>i',i) for i in vals)
def lst(name,kind,values):return tag(name,9)+bytes([kind])+struct.pack('>i',len(values))+b''.join(values)
def cmp(inside):return b''.join(inside)+b'\0'
def statecompound(st):
    if '[' not in st:return cmp([pstr('Name',st)])
    base,prop=st.split('[',1);prop=prop.rstrip(']');props=[pstr(*x.split('=',1)) for x in prop.split(',')]
    return cmp([pstr('Name',base),tag('Properties',10)+cmp(props)])
def dump_nbt(b,path):
    palette=sorted(set(b.blocks.values()));indices={v:i for i,v in enumerate(palette)}
    blocks=[]
    for pos,st in sorted(b.blocks.items()):
        parts=[ints('pos',pos),pint('state',indices[st])]
        if pos in b.block_entities:
            parts.append(tag('nbt',10)+cmp([pstr(key,value) for key,value in b.block_entities[pos].items()]))
        blocks.append(cmp(parts))
    data=tag('',10)+cmp([ints('size',[b.sx,b.sy,b.sz]),lst('palette',10,[statecompound(st) for st in palette]),lst('blocks',10,blocks),lst('entities',10,[]),pint('DataVersion',4790)])
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_bytes(gzip.compress(data,compresslevel=3,mtime=0))
    return len(blocks),len(data)

def savejson(rel,obj):
    path=BASE/rel;path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(obj,indent=2,ensure_ascii=False)+'\n',encoding='utf-8')

def biome_tag(region):return '#pipican:region/'+region if region in REGIONIDS else 'pipican:'+region

def add_pool(pool_id,structure_id):
    savejson(Path('worldgen/template_pool')/(pool_id+'.json'),{
        'fallback':'minecraft:empty','elements':[{'weight':1,'element':{
          'element_type':'minecraft:single_pool_element','location':'pipican:'+structure_id,
          'processors':{'processors':[]},'projection':'rigid'}}]})

def add_structure(registry_id,pool_id,region,spacing=55,spawn=True):
    savejson(Path('worldgen/structure')/(registry_id+'.json'),{
      'type':'minecraft:jigsaw','biomes':biome_tag(region),
      'step':'surface_structures','spawn_overrides':{},'terrain_adaptation':'beard_thin',
      'size':1,'start_height':{'absolute':0},'project_start_to_heightmap':'WORLD_SURFACE_WG',
      'max_distance_from_center':96,'start_pool':'pipican:'+pool_id,'use_expansion_hack':False})
    if spawn:
      salt=int(hashlib.sha256(registry_id.encode()).hexdigest()[:8],16)%2000000000
      savejson(Path('worldgen/structure_set')/(registry_id+'.json'),{
        'placement':{'type':'minecraft:random_spread','salt':salt,'separation':max(8,spacing//4),'spacing':spacing},
        'structures':[{'structure':'pipican:'+registry_id,'weight':1}]})

def generate():
    OUT.mkdir(parents=True,exist_ok=True)
    for region in REGIONS:
        savejson(Path('tags/worldgen/biome/region')/(region['id']+'.json'),{
          'replace':False,'values':['pipican:'+region['id']+'/'+sub['id'] for sub in region['subregions']]})
    catalog=[];count=Counter();assets=[]
    def write(name,region,kind,size,pool=None,structure=False,spawn=False,spacing=55):
        b=Workshop(name,region,size).make(kind)
        placed,raw=dump_nbt(b,BASE/'structure'/(name+'.nbt'))
        if pool:add_pool(pool,name)
        if structure:add_structure(name,pool or name,region,spacing,spawn)
        catalog.append({'id':name,'region':region,'type':kind,'size':list(size),
                        'blocks':placed,'uncompressed_bytes':raw,'features':dict(b.acc),'pool':pool})
        count[kind]+=1
    # All 331 authored pool IDs each reference their own unique actual template.
    for entry in POOLS:
        id=entry['pool'];region=entry['region'];tier=entry['tier']
        size={'capital':(112,72,112),'city':(92,46,92),'town':(80,38,80),
              'village':(80,31,80),'hamlet':(64,29,64),'outpost':(64,39,64)}[tier]
        kind='castle' if tier=='capital' else tier
        write('authored/'+id,region,kind,size,pool=id,structure=True,
              spawn=(entry['used_by']=='procedural'),spacing={'capital':420,'city':220,'town':115,'village':67,'hamlet':61,'outpost':85}[tier])
    # Every authored capital receives 24 *additional* distinct city tiles
    # and one central palace keep, arranged as a five by five 480x480 complex.
    districts=['military','residences','garden','artisan','temple','academy','market','harbor']
    for s in SITES:
        if s['tier']!='capital':continue
        key=s['id'];reg=s['region'];rows=[]
        for dz in range(-2,3):
          for dx in range(-2,3):
            if dx==0 and dz==0:
                rows.append({'template':'pipican:authored/'+s['asset_pool'],'x':0,'z':0,
                             'kind':'central_keep','size':[112,72,112]});continue
            i=((dx+2)*11+(dz+2)*17+list(REGIONIDS).count(reg))
            typ=districts[i%len(districts)]
            ident=f'capital_districts/{reg}/{key}/{dx+2}_{dz+2}_{typ}'
            write(ident,reg,typ,(92,34,92))
            rows.append({'template':'pipican:'+ident,'x':dx*112,'z':dz*112,'kind':typ,'size':[92,34,92]})
        savejson(Path('worldgen/capital_assemblies')/(key+'.json'),{
           'id':key,'region':reg,'anchor':[s['x'],s['z']],
           'author_coord_scale':600,'tile_pitch':112,'relative_positions':rows,
           'implementation':'Requires structure assembly placement into generated chunks; command functions supplied.'})
        # Manual-placement functions for singleplayer world debugging only.
        # Position at city centre x/y/z and run /function pipican:build_capital/<id>.
        lines=[]
        for row in rows:
            xx,zz=row['x'],row['z']
            lines.append(f'place template {row["template"]} ~{xx} ~ ~{zz}')
        f=BASE/'function/build_capital'/(key+'.mcfunction');f.parent.mkdir(parents=True,exist_ok=True)
        f.write_text('\n'.join(lines)+'\n')
        count['capital_district_modules']+=24
    # Extras receive similarly original 3x3 city kits, plus fully furnished
    # biome dungeon and mines containing their own regional ore blocks.
    for reg in sorted(REGIONIDS|set(EXTRAS)):
        write('regional/'+reg+'/dungeon',reg,'dungeon',(80,28,80),pool='regional/'+reg+'/dungeon',structure=True,spawn=True,spacing=115)
        write('regional/'+reg+'/mine',reg,'mine',(80,28,80),pool='regional/'+reg+'/mine',structure=True,spawn=True,spacing=90)
        # Regional boss-reward chests contain the same resource family needed
        # for dungeon-equipment upgrades. Real boss gating comes separately.
        loot={'type':'minecraft:chest','pools':[
           {'rolls':1,'entries':[{'type':'minecraft:item','name':'pipican:'+ORE[reg],
             'functions':[{'function':'minecraft:set_count','count':{'type':'minecraft:uniform','min':3,'max':8}}]}]},
           {'rolls':1,'entries':[{'type':'minecraft:item','name':'minecraft:diamond','weight':1},
                                 {'type':'minecraft:item','name':'minecraft:amethyst_shard','weight':3},
                                 {'type':'minecraft:item','name':'minecraft:emerald','weight':2}]}]}
        savejson(Path('loot_table/chests')/(reg+'_dungeon.json'),loot)
        if reg in EXTRAS:
            write('regional/'+reg+'/castle',reg,'castle',(112,72,112),pool='regional/'+reg+'/castle',structure=True,spawn=False)
            write('regional/'+reg+'/village',reg,'village',(80,30,80),pool='regional/'+reg+'/village',structure=True,spawn=True,spacing=100)
    # Machine-readable summary. All pools from authoritative manifest appear.
    (OUT/'STRUCTURE_CATALOG.json').write_text(json.dumps(catalog,indent=2))
    report={'original_pools':len(POOLS),'authored_landmarks':len(SITES),
            'capital_cities':sum(s['tier']=='capital' for s in SITES),
            'new_biomes':EXTRAS,'total_nbt':len(catalog),
            'counts':dict(count),'block_placements':sum(x['blocks'] for x in catalog),
            'limitations':[
              'Buildings are procedural furnished shells and districts, not hand-sculpted bespoke architecture.',
              'Capital assembly functions are manual until a region-coordinate placement service exists.',
              'Dungeons contain vanilla chest blocks but boss logic and loot attribution are not implemented.',
              'Mine nodes need cooldown/restoration behavior wired into server SavedData.',
              'Some jigsaw pools are manifest only; their direct structure entries may not be automatically placed.',
              'A Minecraft 26.1.2 singleplayer load test has not been run on these generated outputs.']}
    (OUT/'BUILD_REPORT.json').write_text(json.dumps(report,indent=2))
    print(json.dumps(report,indent=2))

if __name__=='__main__':generate()