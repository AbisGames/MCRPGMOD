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
import shutil
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
SPAWN_BY_REGION={}
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
    def furnish_district_interiors(self,x,z,x2,z2,y,role,level):
        """Add functional-looking interiors without sealing exterior doorways.

        Furniture stays close to lateral walls: the central aisle, entry
        threshold, ladder shafts and floor-to-floor vertical openings remain
        clear. The town building type determines furnishing, not only color.
        """
        midx=(x+x2)//2
        midz=(z+z2)//2
        if x2-x<13 or z2-z<13:return
        accent='light_blue_carpet' if self.style in ('elven','ice','aether') else (
            'red_carpet' if self.style in ('volcanic','underground') else
            'yellow_carpet' if self.style in ('desert','savanna','steppe') else
            'green_carpet')
        for xx in range(midx-1,midx+2):
            for zz in range(midz-2,midz+3):
                if (xx,zz)!=(midx,midz):
                    self.set(xx,y,zz,accent)
        self.set(x+5,y+4,z+3,'lantern')
        self.set(x2-5,y+4,z2-3,'lantern')
        if role in ('smithy','forge','artisan'):
            for xx,zz,st in ((x+5,z+5,'blast_furnace'),(x+6,z+5,'anvil'),
                             (x+7,z+5,'smithing_table'),(x2-6,z2-5,'grindstone'),
                             (x2-7,z2-5,'barrel')):
                self.set(xx,y,zz,st)
            for xx in (x+5,x2-5):
                self.set(xx,y+3,midz,'chain')
                self.set(xx,y+4,midz,'lantern')
            self.acc['functional_workshops']+=1
        elif role in ('library','archive'):
            for zz in range(z+5,z2-4,3):
                for h in range(3):
                    self.set(x+2,y+h,zz,'bookshelf')
                    self.set(x2-2,y+h,zz,'bookshelf')
            for xx in (x+6,x2-6):
                self.set(xx,y,midz,'lectern')
            self.acc['library_stacks']+=1
        elif role in ('inn','barracks'):
            for xx in (x+5,x2-6):
                for zz in (z+6,z2-6):
                    self.set(xx,y,zz,'white_wool' if role=='inn' else 'gray_wool')
                    self.set(xx,y,zz+1,'spruce_planks')
                    self.set(xx+1,y,zz,'barrel')
            self.set(x2-5,y,midz,'brewing_stand' if role=='inn' else 'grindstone')
            self.acc['bunkrooms']+=1
        elif role in ('shrine','temple'):
            for zz in range(z+5,z2-4,5):
                self.set(x+4,y,zz,'polished_diorite')
                self.set(x+4,y+1,zz,'amethyst_cluster')
                self.set(x2-4,y,zz,'polished_diorite')
                self.set(x2-4,y+1,zz,'soul_lantern')
            self.set(midx,y,midz+5,'enchanting_table')
            self.acc['chapels']+=1
        elif role=='storage':
            for xx in (x+4,x2-4):
                for zz in range(z+5,z2-4,4):
                    self.set(xx,y,zz,'barrel')
                    self.set(xx,y+1,zz,'barrel')
            self.set(x2-5,y,midz,'crafting_table')
            self.acc['storerooms']+=1
        else:
            for xx,zz in ((x+5,z+5),(x2-5,z+5),(x+5,z2-5),(x2-5,z2-5)):
                self.set(xx,y,zz,'barrel')
                self.set(xx,y+1,zz,'flower_pot')
            self.set(x+6,y,midz,'crafting_table')
            self.set(x2-6,y,midz,'bookshelf')
            self.acc['domestic_quarters']+=1
        self.acc['furnished_room_clusters']+=1

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
            self.furnish_district_interiors(x,z,x2,z2,y,role,level)
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
        # Open the other cardinal gates so every city district can reach the keep.
        self.door(W//2-3,1,D-4,width=7,height=10)
        self.door(3,1,D//2-3,'x',width=7,height=10)
        self.door(W-4,1,D//2-3,'x',width=7,height=10)
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
                # North-facing ladders require a solid support block to the south.
                # Clearing this position made upper palace floors inaccessible.
                self.set(xx,yy,zz+1,post)
                self.set(xx,yy,zz,'ladder[facing=north]')
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
        self.cultural_landmark()
        self.furnish_royal_interiors()
        self.acc['floors']=4
        self.acc['gate_width']=7

    def furnish_royal_interiors(self):
        """Art-direct the palace's four walkable floors into named RPG rooms.

        Coordinates deliberately avoid the central processional aisle, outer
        cardinal gates and the two permanent ladder shafts. Decorative block
        placements never replace the major partition doors.
        """
        W,D=self.sx,self.sz
        cx,cz=W//2,D//2
        signature=self.region
        wall,floor,roof,post,wood=self.theme
        # Ground floor: an accessible ceremonial throne dais and receiving hall.
        for z in range(cz+8,D-30):
            for x in (cx-2,cx-1,cx,cx+1,cx+2):
                self.set(x,1,z,'red_carpet' if signature not in
                         ('elderwood','jade_empire','whitecrown_icecap') else
                         'light_blue_carpet')
        for x in range(cx-5,cx+6):
            for z in range(D-34,D-29):
                self.set(x,1,z,post if z==D-34 else floor)
        self.box(cx-2,2,D-33,cx+2,3,D-33,post)
        self.set(cx,4,D-33,'sea_lantern' if signature in
                 ('elderwood','jade_empire','whitecrown_icecap','aetheris_expanse')
                 else 'soul_lantern' if signature in
                 ('ashlands','crimson_wilds','umbral_chasm') else 'lantern')
        self.acc['throne_halls']+=1

        # Ground floor lateral hospitality quarters: kitchens and royal stores.
        for x in (28,W-29):
            for z in (41,65):
                self.box(x,1,z,x+2,1,z+2,'polished_andesite')
                self.set(x+1,2,z+1,'barrel')
                self.set(x,2,z+2,'smoker')
                self.set(x+2,2,z,'crafting_table')
                self.set(x,3,z,'lantern')
                self.acc['service_rooms']+=1

        # First gallery: council chamber and illuminated map room.
        for z in range(cz-7,cz+8):
            for x in range(cx-5,cx+6):
                if abs(x-cx)<=4 and abs(z-cz)<=6:
                    self.set(x,12,z,'brown_carpet')
        for z in range(cz-4,cz+5,2):
            self.set(cx-2,13,z,'spruce_slab[type=top]')
            self.set(cx+2,13,z,'spruce_slab[type=top]')
        self.set(cx,13,cz,'cartography_table')
        self.set(cx,14,cz,'lantern')
        self.acc['council_rooms']+=1
        for x in (28,W-29):
            for z in (39,63,78):
                self.set(x,12,z,'lectern')
                self.set(x+2,12,z,'bookshelf')
                self.set(x+2,13,z,'bookshelf')
                self.set(x,14,z,'lantern')
        self.acc['archives']+=2

        # Second gallery: reliquary and a defended equipment store.
        for x in range(cx-6,cx+7,3):
            for z in (cz-7,cz+7):
                self.set(x,23,z,'chiseled_stone_bricks')
                self.set(x,24,z,'lantern')
        self.set(cx,23,cz,'enchanting_table')
        self.set(cx,24,cz+2,'end_rod')
        for x in (28,W-29):
            for z in (42,60,78):
                self.set(x,23,z,'grindstone')
                self.set(x+2,23,z,'smithing_table')
                self.set(x+2,24,z,'lantern')
        self.acc['reliquary_rooms']+=1
        self.acc['armories']+=2

        # Fourth floor: observatory and accessible upper study.
        for x in range(cx-7,cx+8):
            for z in range(cz-7,cz+8):
                if abs(x-cx)==7 or abs(z-cz)==7:
                    self.set(x,34,z,'polished_diorite')
        self.set(cx,34,cz,'lodestone')
        for x,z in ((cx-6,cz-6),(cx+6,cz-6),(cx-6,cz+6),(cx+6,cz+6)):
            self.set(x,35,z,'sea_lantern')
        self.acc['observatories']+=1

        # A final regional signature makes rooms meaningfully different, not
        # merely the same interior coloured using different wall blocks.
        if signature in ('elderwood','verdant_depths'):
            for x,z in ((cx-11,cz-9),(cx+11,cz-9),(cx-11,cz+9),(cx+11,cz+9)):
                self.set(x,12,z,'moss_block')
                self.set(x,13,z,'flowering_azalea')
                self.set(x,14,z,'shroomlight')
            self.acc['living_galleries']+=1
        elif signature in ('greyspine_mountains','umbral_chasm'):
            for x,z in ((cx-10,cz-10),(cx+10,cz-10),(cx-10,cz+10),(cx+10,cz+10)):
                self.set(x,23,z,'anvil')
                self.set(x,24,z,'chain')
                self.set(x,25,z,'lantern')
            self.acc['runic_forgeries']+=1
        elif signature in ('jade_empire','lotus_delta'):
            for x,z in ((cx-10,cz),(cx+10,cz)):
                self.set(x,12,z,'cherry_planks')
                self.set(x,13,z,'bamboo')
                self.set(x,14,z,'lantern')
            self.acc['imperial_studies']+=1
        elif signature in ('ashlands','crimson_wilds','stormcoast'):
            for x,z in ((cx-9,cz-9),(cx+9,cz-9),(cx-9,cz+9),(cx+9,cz+9)):
                self.set(x,23,z,'crying_obsidian')
                self.set(x,24,z,'soul_lantern')
            self.acc['infernal_rituals']+=1
        elif signature in ('whitecrown_icecap','northern_ice_sea'):
            for x,z in ((cx-10,cz-9),(cx+10,cz-9),(cx-10,cz+9),(cx+10,cz+9)):
                self.set(x,23,z,'blue_ice')
                self.set(x,24,z,'sea_lantern')
            self.acc['frozen_reliquaries']+=1
        elif signature in ('sunscorch_desert','riverlands','spice_coast'):
            for x,z in ((cx-9,cz-9),(cx+9,cz-9),(cx-9,cz+9),(cx+9,cz+9)):
                self.set(x,12,z,'chiseled_sandstone')
                self.set(x,13,z,'lantern')
            self.acc['solar_chambers']+=1
        elif signature in ('coral_isles','inner_sea','western_ocean','shimmering_reefs'):
            for x,z in ((cx-9,cz-9),(cx+9,cz-9),(cx-9,cz+9),(cx+9,cz+9)):
                self.set(x,12,z,'prismarine_bricks')
                self.set(x,13,z,'sea_lantern')
            self.acc['tidal_sanctuaries']+=1
        elif signature in ('marshfen','emerald_jungle'):
            for x,z in ((cx-9,cz-9),(cx+9,cz-9),(cx-9,cz+9),(cx+9,cz+9)):
                self.set(x,12,z,'moss_block')
                self.set(x,13,z,'mangrove_roots')
            self.acc['nature_labs']+=1
        else:
            for x,z in ((cx-10,cz-9),(cx+10,cz-9),(cx-10,cz+9),(cx+10,cz+9)):
                self.set(x,12,z,'decorated_pot')
                self.set(x,13,z,'lantern')
            self.acc['heritage_galleries']+=1

    def cultural_landmark(self):
        """Compose a landmark silhouette unique to this region and accessible courtyards.

        This adds biome-specific arches, waterworks, observatories, watchtowers
        and ceremonial surfaces without replacing the palace's existing rooms.
        All geometry remains within the 112 x 72 x 112 Minecraft structure
        template; the outer 5x5 district layout is assembled separately.
        """
        W,D=self.sx,self.sz
        cx,cz=W//2,D//2
        a=self.theme[0]
        b=self.theme[3]
        c=self.theme[2]
        rid=self.region
        def arch(x,z,width=9,height=12,axis='x',stone=None):
            block=stone or b
            for i in range(width):
                for y in range(2,height+2):
                    edge=i in (0,1,width-2,width-1) or y>=height
                    if edge:
                        px=x+i if axis=='x' else x
                        pz=z if axis=='x' else z+i
                        self.set(px,y,pz,block)
            for i in range(2,width-2):
                for y in range(2,height):
                    self.set(x+i if axis=='x' else x,y,z if axis=='x' else z+i,'air')
        def beacon(x,z,h,stone,light='sea_lantern'):
            for y in range(1,h):
                self.set(x,y,z,stone)
                if y in (h//2,h-2):
                    for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
                        self.set(x+dx,y,z+dz,stone)
            self.set(x,h,z,light)
        if rid=='elderwood':
            # Rivendell-inspired slender white arcades, high walkways and
            # waterfall-fed reflection pools, all original block geometry.
            for x in range(19,W-15,14):
                arch(x,17,9,16,stone='quartz_pillar')
                arch(x,D-18,9,16,stone='quartz_pillar')
                self.set(x+4,19,17,'sea_lantern')
            for z in range(21,D-20):
                for x in (15,W-16):
                    self.set(x,25,z,'smooth_quartz')
                    if z%7==0:self.set(x,26,z,'sea_lantern')
            for z in (19,D-20):
                for x in range(31,W-30):
                    self.set(x,1,z,'water')
            for x,z in ((12,34),(W-13,34),(12,D-35),(W-13,D-35)):
                beacon(x,z,24,'quartz_pillar','sea_lantern')
        elif rid=='greyspine_mountains':
            for z in (12,D-13):
                arch(cx-13,z,27,22,stone='polished_deepslate')
            for x in (13,W-14):
                for z in (36,D-35,8):
                    beacon(x,z,15,'polished_basalt','lava')
            for x,z in ((16,16),(W-17,16),(16,D-17),(W-17,D-17)):
                self.spire(x,z,4,22,16)
        elif rid in ('jade_empire','lotus_delta'):
            for x,z in ((15,15),(W-16,15),(15,D-16),(W-16,D-16)):
                self.spire(x,z,4,22,20)
            for x in range(26,W-25):
                for z in (18,D-19):
                    self.set(x,23,z,'dark_prismarine')
            for z in range(26,D-25):
                for x in (18,W-19):
                    self.set(x,23,z,'cherry_planks')
            for x in (24,W-25):
                for z in (24,D-25):
                    beacon(x,z,12,'cherry_log','lantern')
        elif rid in ('ashlands','crimson_wilds','stormcoast'):
            for x,z in ((15,15),(W-16,15),(15,D-16),(W-16,D-16)):
                beacon(x,z,28,'polished_blackstone','soul_lantern')
            for z in (14,D-15):
                for x in range(24,W-23,10):
                    self.set(x,2,z,'magma_block')
                    self.set(x,3,z,'iron_bars')
            for z in range(25,D-24,11):
                self.set(14,2,z,'crying_obsidian')
                self.set(W-15,2,z,'crying_obsidian')
        elif rid in ('whitecrown_icecap','northern_ice_sea'):
            for x,z in ((15,15),(W-16,15),(15,D-16),(W-16,D-16)):
                beacon(x,z,30,'blue_ice')
            for x in range(22,W-21,6):
                for z in (15,D-16):
                    self.set(x,15,z,'packed_ice')
                    self.set(x,16,z,'blue_ice')
        elif rid in ('sunscorch_desert','riverlands','spice_coast'):
            for x in (20,W-21):
                for z in (23,D-24,18):
                    arch(x,z,11,12,'z','chiseled_sandstone')
            for x,z in ((19,19),(W-20,19),(19,D-20),(W-20,D-20)):
                beacon(x,z,19,'cut_sandstone','lantern')
        elif rid=='marshfen':
            for x in (13,W-14):
                for z in range(25,D-24):
                    self.set(x,3,z,'mangrove_planks')
                    if z%6==0:beacon(x,z,9,'mangrove_log','soul_lantern')
            for x,z in ((15,15),(W-16,15),(15,D-16),(W-16,D-16)):
                beacon(x,z,21,'mangrove_log','shroomlight')
        elif rid in ('coral_isles','western_ocean','inner_sea','shimmering_reefs'):
            for x,z in ((16,16),(W-17,16),(16,D-17),(W-17,D-17)):
                beacon(x,z,24,'prismarine_bricks')
            for z in (18,D-19):
                for x in range(24,W-23):
                    self.set(x,2,z,'prismarine')
                    if x%7==0:self.set(x,3,z,'sea_lantern')
        elif rid in ('emerald_jungle','verdant_depths'):
            for x,z in ((13,13),(W-14,13),(13,D-14),(W-14,D-14)):
                for y in range(2,31):self.set(x,y,z,'jungle_log')
                for dx in range(-5,6):
                    for dz in range(-5,6):
                        if dx*dx+dz*dz<27:
                            self.set(x+dx,30,z+dz,'jungle_leaves')
            for z in (18,D-19):
                arch(cx-8,z,17,14,stone='mossy_stone_bricks')
        elif rid=='aetheris_expanse':
            for x,z in ((15,15),(W-16,15),(15,D-16),(W-16,D-16)):
                beacon(x,z,32,'amethyst_block')
            for x in range(27,W-26):
                for z in (15,D-16):
                    self.set(x,31,z,'quartz_bricks')
                    if x%8==0:self.set(x,32,z,'sea_lantern')
        elif rid=='umbral_chasm':
            for x,z in ((15,15),(W-16,15),(15,D-16),(W-16,D-16)):
                beacon(x,z,24,'crying_obsidian','soul_lantern')
            for x in range(14,W-13,7):
                for z in (12,D-13):
                    self.set(x,21,z,'pointed_dripstone[vertical_direction=down,thickness=tip]')
        elif rid in ('pinewall_taiga','fjordmark'):
            for x in (17,W-18):
                for z in (19,D-20):
                    arch(x,z,13,11,axis='z',stone='stripped_spruce_log')
            for x,z in ((17,17),(W-18,17),(17,D-18),(W-18,D-18)):
                beacon(x,z,23,'spruce_log','lantern')
        elif rid in ('khagan_steppe','goldgrass_savanna'):
            for x,z in ((15,15),(W-16,15),(15,D-16),(W-16,D-16)):
                beacon(x,z,22,'acacia_log','lantern')
            for x in range(20,W-19,9):
                self.set(x,21,17,'orange_terracotta')
                self.set(x,21,D-18,'orange_terracotta')
        elif rid=='redrock_badlands':
            for x,z in ((17,17),(W-18,17),(17,D-18),(W-18,D-18)):
                beacon(x,z,26,'red_sandstone','lantern')
            for x in (15,W-16):
                for z in range(20,D-19,4):
                    self.set(x,20,z,'orange_terracotta')
                    self.set(x,21,z,'red_sandstone')
        self.acc['cultural_signature']=1

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

    def cultural_square(self):
        """Distinct village-square monuments around each region's crossroads."""
        cx,cz=self.sx//2,self.sz//2
        r=self.region
        if r in ('whitecrown_icecap','northern_ice_sea'):
            core,light='blue_ice','sea_lantern'
        elif r in ('elderwood','emerald_jungle','verdant_depths'):
            core,light='quartz_pillar','shroomlight'
        elif r in ('ashlands','stormcoast','crimson_wilds'):
            core,light='polished_blackstone','soul_lantern'
        elif r in ('marshfen','lotus_delta'):
            core,light='mangrove_log','shroomlight'
        elif r in ('jade_empire','aetheris_expanse'):
            core,light='amethyst_block','end_rod'
        elif r in ('greyspine_mountains','umbral_chasm'):
            core,light='chiseled_deepslate','soul_lantern'
        elif r in ('sunscorch_desert','spice_coast','redrock_badlands'):
            core,light='chiseled_sandstone','lantern'
        elif r in ('western_ocean','coral_isles','shimmering_reefs','inner_sea'):
            core,light='prismarine_bricks','sea_lantern'
        else:
            core,light=self.theme[3],'lantern'
        # Four freestanding landmarks; the crossing itself remains passable.
        for x,z in ((cx-13,cz-13),(cx+13,cz-13),(cx-13,cz+13),(cx+13,cz+13)):
            for y in range(1,7):
                self.set(x,y,z,core)
            self.set(x,7,z,light)
            for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
                self.set(x+dx,4,z+dz,core)
        self.acc['regional_square_markers']=4
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
        self.cultural_square()
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

    def cultural_cave_identity(self,kind):
        """Set-dress each dungeon and mine with region-specific architecture.

        Decorations stay adjacent to the side walls and leave the main
        9-block central approach to trial spawners, ore rooms and exits clear.
        Nothing changes the functional loot and renewable mining markers.
        """
        W,D=self.sx,self.sz
        region=self.region
        def pillar(x,z,material,light):
            for y in range(1,12):
                self.set(x,y,z,material)
            self.set(x,12,z,light)
            for y in (3,8):
                for dx,dz in ((1,0),(-1,0),(0,1),(0,-1)):
                    self.set(x+dx,y,z+dz,material)
        if region in ('whitecrown_icecap','northern_ice_sea'):
            deco,glow='packed_ice','sea_lantern'
        elif region in ('elderwood','emerald_jungle','verdant_depths'):
            deco,glow='mossy_stone_bricks','shroomlight'
        elif region in ('greyspine_mountains','umbral_chasm'):
            deco,glow='chiseled_deepslate','soul_lantern'
        elif region in ('ashlands','crimson_wilds','stormcoast'):
            deco,glow='polished_blackstone_bricks','magma_block'
        elif region in ('sunscorch_desert','spice_coast','redrock_badlands'):
            deco,glow='chiseled_sandstone','lantern'
        elif region in ('marshfen','lotus_delta'):
            deco,glow='mangrove_planks','shroomlight'
        elif region in ('jade_empire','aetheris_expanse'):
            deco,glow='chiseled_quartz_block','sea_lantern'
        elif region in ('western_ocean','coral_isles','shimmering_reefs','inner_sea'):
            deco,glow='prismarine_bricks','sea_lantern'
        elif region in ('khagan_steppe','goldgrass_savanna','riverlands'):
            deco,glow='cut_copper','lantern'
        else:
            deco,glow=self.theme[3],'lantern'

        for z in (31,D-32):
            for x in (6,W-7):
                pillar(x,z,deco,glow)
        for z in range(9,D-8,7):
            for x in (2,W-3):
                for y in (4,5,6):
                    self.set(x,y,z,deco)
                self.set(x,7,z,glow)
        # Frozen columns, fungal roots, crystalline veins and volcanic
        # arcades add silhouettes visible from the centre of the maze.
        if region in ('elderwood','emerald_jungle','verdant_depths','marshfen'):
            for z in (21,D-22):
                for x in (9,W-10):
                    self.set(x,12,z,'moss_block')
                    self.set(x,11,z,'glow_lichen')
        elif region in ('whitecrown_icecap','northern_ice_sea','aetheris_expanse'):
            for x in (14,W-15):
                for z in (14,D-15):
                    for y in range(2,12):
                        if y<9 or (x+z)%2==0:self.set(x,y,z,'blue_ice' if region!='aetheris_expanse' else 'amethyst_block')
                    self.set(x,12,z,'sea_lantern')
        elif region in ('ashlands','stormcoast','crimson_wilds'):
            for x in (14,W-15):
                for z in (14,D-15):
                    self.set(x,1,z,'polished_basalt')
                    self.set(x,2,z,'magma_block')
                    self.set(x,3,z,'iron_bars')
        elif region in ('jade_empire','lotus_delta'):
            for x in (17,W-18):
                for z in (17,D-18):
                    self.set(x,2,z,'chiseled_quartz_block')
                    self.set(x,3,z,'lantern')
        elif region in ('coral_isles','western_ocean','shimmering_reefs'):
            for x in (16,W-17):
                for z in (16,D-17):
                    self.set(x,3,z,'prismarine')
                    self.set(x,4,z,'sea_lantern')
        self.acc['regional_cave_identity']=1
    def cavern_upper_gallery(self,kind):
        """Build a second, player-accessible dungeon/mine floor.

        The original plan has a continuous roof at Y=14. This method carves
        an atrium above its main encounter, constructs a bounded upper gallery,
        and adds a protected climbing shaft so the whole feature is traversable.
        It never overwrites the lower trial spawner, award chests or ore seams.
        """
        W,D=self.sx,self.sz
        cx,cz=W//2,D//2
        wall,floor,roof,post,wood=self.theme

        # Empty above the original galleries; terminate the structure with a roof.
        self.air(2,15,2,W-3,25,D-3)
        self.box(1,26,1,W-2,26,D-2,roof)
        for x in (1,W-2):
            self.wall(x,1,x,D-2,15,25,wall)
        for z in (1,D-2):
            self.wall(1,z,W-2,z,15,25,wall)

        # Retain the original 14th-level floor as a continuous upper walkway
        # except for the open void above the battle/mining chamber.
        self.air(cx-9,14,cz-9,cx+9,14,cz+9)
        for z in range(cz-10,cz+11):
            for x in (cx-10,cx+10):
                self.set(x,15,z,'iron_bars')
        for x in range(cx-10,cx+11):
            for z in (cz-10,cz+10):
                self.set(x,15,z,'iron_bars')

        # Fully unobstructed two-block-wide access shaft and its anchor wall.
        shaftx,shaftz=cx-17,cz
        for y in range(1,20):
            self.set(shaftx-1,y,shaftz,post)
            self.set(shaftx,y,shaftz,'ladder[facing=east]')
            self.set(shaftx+1,y,shaftz,'air')
        self.air(shaftx,14,shaftz,shaftx+1,15,shaftz+1)

        # Side balconies contain separate navigable rooms and route markers.
        for x1,x2 in ((5,24),(W-25,W-6)):
            for z1,z2 in ((8,27),(D-28,D-9)):
                for x in (x1,x2):
                    self.wall(x,z1,x,z2,15,22,wall)
                for z in (z1,z2):
                    self.wall(x1,z,x2,z,15,22,wall)
                self.box(x1,23,z1,x2,23,z2,roof)
                self.door((x1+x2)//2-2,15,z2 if z2<cz else z1,
                          width=5,height=5)
                self.set((x1+x2)//2,16,(z1+z2)//2,
                         'cartography_table' if kind=='dungeon' else 'smithing_table')
                self.set(x1+3,16,z1+3,'barrel')
                self.set(x2-3,16,z2-3,'barrel')
                self.light((x1+x2)//2,21,(z1+z2)//2)
                self.acc['upper_rooms']+=1

        # Each biome's feature uses a real ore block, never a fake metadata
        # marker. Only mines receive these recoverable upper-gallery nodes.
        if kind=='mine':
            mineral=f'pipican:{ORE[self.region]}_ore'
            for z in range(10,D-9,8):
                for x in (2,W-3):
                    self.set(x,19,z,mineral)
                    self.set(x,18,z,post)
                    self.acc['upper_ore_nodes']+=1
            for z in range(12,D-11,7):
                self.set(cx+16,15,z,'rail')
                self.set(cx+17,15,z,'rail')
            self.acc['mine_mezzanines']+=1
        else:
            # Viewing platforms, ritual torches and barred trial galleries.
            for z in range(cz-8,cz+9,4):
                for x in (cx-12,cx+12):
                    self.set(x,16,z,'chiseled_stone_bricks')
                    self.set(x,17,z,'soul_lantern')
            for x in range(cx-7,cx+8,5):
                for z in (cz-12,cz+12):
                    self.set(x,16,z,'end_rod')
            self.acc['upper_trial_galleries']+=1
        self.acc['accessible_second_levels']+=1

    def cave(self,kind):
        # Underground plan with traversable three-aisle corridors, rooms and floors.
        wall,floor,roof,post,wood=self.theme
        W=self.sx;D=self.sz
        self.box(1,0,1,W-2,0,D-2,floor)
        self.air(2,1,2,W-3,13,D-3)
        self.box(1,14,1,W-2,14,D-2,roof)
        for xx in (1,W-2):self.wall(xx,1,xx,D-2,1,13,wall)
        for zz in (1,D-2):self.wall(1,zz,W-2,zz,1,13,wall)
        # Accessible front entry, rather than a completely sealed cave box.
        # Removing only the wall blocks is insufficient: explicit air is
        # required to clear the original terrain when templates are placed.
        self.air(W//2-4,1,1,W//2+4,8,3)
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
            # Ore veins are exposed in mine walls rather than floating blocks;
            # they are reachable from the galleries and can be renewed safely.
            for k in range(28):
                x=(1 if k%2==0 else W-2)
                z=self.rand.randrange(7,D-7)
                y=self.rand.randrange(2,8)
                self.set(x,y,z,f'pipican:{ore}_ore')
                self.acc['regional_ore_nodes']+=1
            self.acc['rails']=2
        else:
            # Loot position and boss chamber fixed for server-side reward system.
            for dx in (-2,0,2):
                pos=(cx+dx,2,cz+8)
                self.set(*pos,'chest[facing=north]')
                self.block_entities[pos]={'LootTable':f'pipican:chests/{self.region}_dungeon'}
            self.acc['reward_chests']=3
            # Renewable vanilla trial arena, configured by a regional datapack.
            # Its reward is emitted only after players defeat the spawned wave.
            # This keeps the repeatable dungeon loop independent of player PvP
            # and of the still-unfinished bespoke RPG boss implementations.
            spawner=(cx,2,cz)
            self.set(*spawner,'trial_spawner')
            self.block_entities[spawner]={
                'id':'minecraft:trial_spawner',
                'normal_config':f'pipican:{self.region}_trial',
                'target_cooldown_length':36000,
                'required_player_range':14}
            self.acc['renewable_encounters']=1
        self.cultural_cave_identity(kind)
        self.cavern_upper_gallery(kind)
        self.acc['main_chamber']=1

    def access_bridge(self, axis):
        """Traversable 20-block capital street bridge between district tiles.

        The city tiles are spaced 112 blocks apart, but ordinary residential
        tiles are 92 blocks wide. These connectors close each 20-block gap
        while keeping a seven-block pedestrian lane and safe railings.
        """
        pavement=self.theme[1]
        rail=self.theme[3]
        horizontal=(axis=='eastwest')
        for x in range(self.sx):
            for z in range(self.sz):
                self.set(x,0,z,pavement)
                side=(z in (0,self.sz-1)) if horizontal else (x in (0,self.sx-1))
                if side:
                    self.set(x,1,z,rail)
                    if (x%5==0 if horizontal else z%5==0):
                        self.set(x,2,z,'lantern')
                elif (x%4==0 if horizontal else z%4==0):
                    self.set(x,0,z,self.theme[4])
        self.acc['passable_bridge']=1

    def make(self,typ):
        if typ=='castle':self.core_castle()
        elif typ in ('bridge_eastwest','bridge_northsouth'):self.access_bridge(typ.split('_',1)[1])
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
            parts.append(tag('nbt',10)+cmp([
                pint(key,value) if isinstance(value,int) else pstr(key,value)
                for key,value in b.block_entities[pos].items()]))
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
      SPAWN_BY_REGION.setdefault(region,[]).append(registry_id)

def generate():
    OUT.mkdir(parents=True,exist_ok=True)
    SPAWN_BY_REGION.clear()
    # Purge former per-template spread sets: all procedural structures in
    # a region must share one native random-spread grid to prevent collisions.
    set_dir=BASE/'worldgen/structure_set'
    if set_dir.exists():shutil.rmtree(set_dir)
    for region in REGIONS:
        savejson(Path('tags/worldgen/biome/region')/(region['id']+'.json'),{
          'replace':False,'values':['pipican:'+region['id']+'/'+sub['id'] for sub in region['subregions']]})
    catalog=[];count=Counter();assets=[];bridge_regions=set()
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
            # Hash each tile independently to make every capital's district
            # placement a unique and reproducible civic plan.
            i=int(hashlib.sha256(f'{key}:{reg}:{dx}:{dz}'.encode()).hexdigest()[:8],16)
            typ=districts[i%len(districts)]
            ident=f'capital_districts/{reg}/{key}/{dx+2}_{dz+2}_{typ}'
            write(ident,reg,typ,(92,34,92))
            rows.append({'template':'pipican:'+ident,'x':dx*112,'z':dz*112,'kind':typ,'size':[92,34,92]})
        # Generate distinct biome-specific horizontal and vertical street spans
        # and connect otherwise separated 92-wide district modules.
        xbridge=f'capital_connections/{reg}/eastwest'
        zbridge=f'capital_connections/{reg}/northsouth'
        if reg not in bridge_regions:
            write(xbridge,reg,'bridge_eastwest',(20,4,7))
            write(zbridge,reg,'bridge_northsouth',(7,4,20))
            bridge_regions.add(reg)
        connectors=[]
        for zrow in range(-2,3):
          for xcol in range(-2,2):
            if xcol==0 and zrow==0:continue  # central keep occupies full 112 width
            connectors.append({'template':'pipican:'+xbridge,
                               'x':xcol*112+92,'z':zrow*112+42,
                               'kind':'walkway_eastwest','size':[20,4,7]})
        for xcol in range(-2,3):
          for zrow in range(-2,2):
            if xcol==0 and zrow==0:continue
            connectors.append({'template':'pipican:'+zbridge,
                               'x':xcol*112+42,'z':zrow*112+92,
                               'kind':'walkway_northsouth','size':[7,4,20]})
        savejson(Path('worldgen/capital_assemblies')/(key+'.json'),{
           'id':key,'region':reg,'anchor':[s['x'],s['z']],
           'author_coord_scale':600,'tile_pitch':112,
           'relative_positions':rows,'connectors':connectors,
           'implementation':'25 roomed city tiles, plus accessible capital road connectors.'})
        # Manual-placement functions for singleplayer world debugging only.
        # Position at city centre x/y/z and run /function pipican:build_capital/<id>.
        lines=[]
        for row in rows+connectors:
            xx,zz=row['x'],row['z']
            lines.append(f'place template {row["template"]} ~{xx} ~ ~{zz}')
        f=BASE/'function/build_capital'/(key+'.mcfunction');f.parent.mkdir(parents=True,exist_ok=True)
        f.write_text('\n'.join(lines)+'\n')
        count['capital_district_modules']+=24
        count['capital_road_links']+=len(connectors)
    # Extras receive similarly original 3x3 city kits, plus fully furnished
    # biome dungeon and mines containing their own regional ore blocks.
    for reg in sorted(REGIONIDS|set(EXTRAS)):
        write('regional/'+reg+'/dungeon',reg,'dungeon',(80,28,80),pool='regional/'+reg+'/dungeon',structure=True,spawn=True,spacing=115)
        write('regional/'+reg+'/mine',reg,'mine',(80,28,80),pool='regional/'+reg+'/mine',structure=True,spawn=True,spacing=90)
        # Regional boss-reward chests contain the same resource family needed
        # for dungeon-equipment upgrades. Real boss gating comes separately.
        # Side chests have common exploration loot. Unique ore/catalyst boss
        # rewards are only ejected by the configured regional trial spawner.
        loot={'type':'minecraft:chest','pools':[{
          'rolls':2,'entries':[
            {'type':'minecraft:item','name':'minecraft:emerald','weight':4},
            {'type':'minecraft:item','name':'minecraft:iron_ingot','weight':6},
            {'type':'minecraft:item','name':'minecraft:diamond','weight':1},
            {'type':'minecraft:item','name':'minecraft:amethyst_shard','weight':2}]}]}
        savejson(Path('loot_table/chests')/(reg+'_dungeon.json'),loot)
        if reg in EXTRAS:
            write('regional/'+reg+'/castle',reg,'castle',(112,72,112),pool='regional/'+reg+'/castle',structure=True,spawn=False)
            write('regional/'+reg+'/village',reg,'village',(80,30,80),pool='regional/'+reg+'/village',structure=True,spawn=True,spacing=100)
    # One weighted spread per region. Native structures formerly had 320
    # competing grids, so unrelated villages/mines/dungeons could collide.
    for region,ids in sorted(SPAWN_BY_REGION.items()):
        is_extra=region in EXTRAS
        def weight(ident):
            if ident.endswith('/mine'):return 4
            if ident.endswith('/dungeon'):return 3
            if ident.endswith('/village'):return 4
            if ident.endswith('/hamlet'):return 3
            if ident.endswith('/town'):return 2
            return 1
        salt=7919
        for ch in region:salt=(salt*31+ord(ch))%1000000000
        salt+=900000
        savejson(Path('worldgen/structure_set')/('regional/'+region+'/expeditions.json'),{
          'placement':{'type':'minecraft:random_spread','salt':salt,
                       'separation':12 if is_extra else 22,
                       'spacing':35 if is_extra else 60},
          'structures':[{'structure':'pipican:'+ident,'weight':weight(ident)}
                        for ident in sorted(ids)]})
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