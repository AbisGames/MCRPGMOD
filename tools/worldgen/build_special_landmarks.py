#!/usr/bin/env python3
"""Generate four bespoke RPG landmarks using the canonical Minecraft NBT writer.

Run from repository root:
  python3 tools/worldgen/build_special_landmarks.py
Generated files appear under tools/worldgen/output/data/pipican/structure/special.
Use the ordinary resource-packing process to include the NBT templates.
These are models, NOT enabled world-generation placements.
"""
from pathlib import Path
from build_full_structures import Workshop, dump_nbt, BASE

def hall(b,x1,z1,x2,z2,y,h,wall,floor,roof):
    b.box(x1,y-1,z1,x2,y-1,z2,floor)
    b.air(x1+1,y,z1+1,x2-1,y+h-1,z2-1)
    for x in (x1,x2): b.wall(x,z1,x,z2,y,y+h,wall)
    for z in (z1,z2): b.wall(x,z,x2,z,y,y+h,wall)
    b.box(x1,y+h+1,z1,x2,y+h+1,z2,roof)

def outpost():
    b=Workshop('special/pillager_blackspire','redrock_badlands',(96,56,96))
    wall,floor,roof,post,wood=b.theme
    b.box(2,0,2,93,0,93,'red_sandstone')
    # Stronghold-sized raider courtyard with walkable gate.
    for x in (5,90): b.wall(x,5,x,90,1,13,wall)
    for z in (5,90): b.wall(5,z,90,z,1,13,wall)
    b.air(44,1,4,52,9,7)
    b.box(42,0,0,54,0,16,'polished_blackstone')
    for x,z in ((12,12),(83,12),(12,83),(83,83)):
        b.box(x-6,1,z-6,x+6,28,z+6,wall,hollow=True)
        b.air(x-5,2,z-5,x+5,27,z+5)
        for y in (1,9,17,25):b.box(x-6,y,z-6,x+6,y,z+6,floor)
        for y in range(2,26):b.set(x+4,y,z,'ladder[facing=west]')
        b.spire(x,z,6,29,18)
    hall(b,27,31,69,76,1,13,wall,floor,roof)
    b.air(44,1,31,52,8,33)
    for x in (31,64):
        for z in (39,53,68):
            b.set(x,2,z,'barrel');b.set(x,3,z,'crossbow' if False else 'fletching_table')
            b.set(x,6,z,'lantern')
    b.box(33,15,38,63,15,70,floor)
    for z in range(40,70):
        b.set(35,16,z,'iron_bars')
        b.set(61,16,z,'iron_bars')
    for y in range(2,16):b.set(31,y,35,'ladder[facing=east]')
    for x,z in ((26,25),(70,25),(26,78),(70,78)):
        b.set(x,1,z,'hay_block');b.set(x,2,z,'target')
    return b

def ancient():
    b=Workshop('special/umbral_ancient_city','umbral_chasm',(112,52,112))
    wall,floor,roof,post,wood=b.theme
    b.box(2,0,2,109,0,109,'reinforced_deepslate')
    for x in range(5,108,10):
        b.box(x,1,54,x+4,1,58,'polished_deepslate')
        b.set(x+2,2,53,'soul_lantern')
    for z in range(8,107,13):
        b.box(54,1,z,58,1,z+6,'polished_deepslate')
    for x,z in ((9,9),(78,9),(9,78),(78,78)):
        hall(b,x,z,x+24,z+24,1,13,wall,floor,roof)
        b.air(x+9,1,z+24,x+15,8,z+25)
        b.box(x+3,15,z+3,x+21,15,z+21,floor)
        for y in range(2,15):b.set(x+3,y,z+3,'ladder[facing=east]')
        b.set(x+12,2,z+10,'chiseled_deepslate')
        b.set(x+12,3,z+10,'soul_lantern')
    # A massive unlit echo gateway with ancient-city-like identity.
    for x in range(42,70):
        for y in range(1,38):
            if x in (42,43,68,69) or y in (36,37):
                b.set(x,y,55,'reinforced_deepslate')
            elif y<33:
                b.set(x,y,55,'air')
    for x in (37,74):
        for z in (37,74):
            b.box(x-2,1,z-2,x+2,8,z+2,'sculk')
            b.set(x,9,z,'soul_lantern')
    return b

def portal():
    b=Workshop('special/morvath_portal_chamber','ashlands',(64,38,64))
    wall,floor,roof,post,wood=b.theme
    hall(b,2,2,61,61,1,23,wall,floor,roof)
    b.air(28,1,2,36,9,4)
    for x in (7,56):
        for z in (7,56):
            b.box(x-2,1,z-2,x+2,20,z+2,'polished_blackstone_bricks')
            b.set(x,21,z,'soul_lantern')
    b.box(20,1,22,44,1,42,'polished_basalt')
    # Complete but intentionally unlit Nether portal frame.
    for x in range(26,39):
        for y in range(2,20):
            if x in (26,38) or y in (2,19):
                b.set(x,y,31,'obsidian')
            else:b.set(x,y,31,'air')
    for x in (21,43):
        for z in (24,40):
            b.box(x-1,2,z-1,x+1,5,z+1,'crying_obsidian')
            b.set(x,6,z,'soul_lantern')
    for x in range(5,59,7):
        b.set(x,2,7,'gilded_blackstone')
        b.set(x,2,56,'gilded_blackstone')
    return b

def aether():
    b=Workshop('special/aetheris_sky_citadel','aetheris_expanse',(112,78,112))
    wall,floor,roof,post,wood=b.theme
    # Layered floating island: the base tapers downward to a rocky point.
    for y in range(0,18):
        radius=int(44*(y+1)/18)
        for x in range(56-radius,57+radius):
            for z in range(56-radius,57+radius):
                if (x-56)**2+(z-56)**2 <= radius*radius:
                    b.set(x,y,z,'calcite' if y>12 else 'amethyst_block')
    b.box(13,18,13,99,18,99,'smooth_quartz')
    for x in (16,95):b.wall(x,16,x,95,19,32,wall)
    for z in (16,95):b.wall(16,z,95,z,19,32,wall)
    for x,z in ((22,22),(90,22),(22,90),(90,90)):
        b.box(x-5,19,z-5,x+5,44,z+5,wall,hollow=True)
        b.air(x-4,20,z-4,x+4,43,z+4)
        for y in (19,27,35,43):b.box(x-5,y,z-5,x+5,y,z+5,floor)
        for y in range(20,43):b.set(x+3,y,z,'ladder[facing=west]')
        b.spire(x,z,5,45,16)
    hall(b,35,35,77,77,19,23,wall,floor,roof)
    b.air(52,19,35,60,26,37)
    b.air(52,19,77,60,26,79)
    for y in range(20,44):b.set(38,y,38,'ladder[facing=east]')
    b.box(40,44,40,72,44,72,'quartz_bricks')
    # End portal centre: inactive, no eyes inserted. Placement/gameplay code
    # must enable activation and frame-state validation separately.
    # Vanilla-compatible 3x3 interior and exactly twelve portal frames.
    for x in range(54,59):
        for z in range(54,59):b.set(x,45,z,'polished_deepslate')
    for x in range(55,58):
        for z in (54,58):
            facing='south' if z==54 else 'north'
            b.set(x,45,z,f'end_portal_frame[facing={facing},eye=false]')
    for z in range(55,58):
        for x in (54,58):
            facing='east' if x==54 else 'west'
            b.set(x,45,z,f'end_portal_frame[facing={facing},eye=false]')
    for x,z in ((45,45),(67,45),(45,67),(67,67)):
        b.set(x,45,z,'sea_lantern');b.set(x,46,z,'end_rod')
    return b

def morvath_integrated():
    """Rebuild the authored Morvath central keep with an integrated, unlit
    Nether portal room in its western interior wing; never overlay a 64x64
    room onto a completed city in the live world."""
    b=Workshop('authored/landmarks/ashlands/morvath','ashlands',(112,72,112))
    b.make('castle')
    # Small enclosed sanctuary entirely within the original palace footprint.
    x1,x2,z1,z2=24,42,43,63
    wall='polished_blackstone_bricks'
    b.box(x1,1,z1,x2,12,z2,wall,hollow=True)
    b.air(x1+1,2,z1+1,x2-1,11,z2-1)
    b.box(x1+1,1,z1+1,x2-1,1,z2-1,'polished_basalt')
    b.box(x1,13,z1,x2,13,z2,'nether_bricks')
    # Connection to the existing palace west gallery.
    b.air(x2-1,2,51,x2+1,7,55)
    # Complete but unlit portal (no portal blocks and no fire).
    for x in range(28,39):
        for y in range(2,14):
            if x in (28,38) or y in (2,13):
                b.set(x,y,51,'obsidian')
            else:
                b.set(x,y,51,'air')
    for x,z in ((26,46),(40,46),(26,60),(40,60)):
        b.set(x,2,z,'crying_obsidian')
        b.set(x,3,z,'soul_lantern')
    return b


def main():
    # The portal is part of the authored capital keep, not a second world
    # structure which could overwrite the city's incremental placement.
    keep=BASE/'structure/authored/landmarks/ashlands/morvath.nbt'
    count,raw=dump_nbt(morvath_integrated(),keep)
    print(f'{keep}: {count} blocks; {raw} uncompressed bytes')
    for name,build in [('pillager_blackspire',outpost()),('umbral_ancient_city',ancient()),
                       ('morvath_portal_chamber',portal()),('aetheris_sky_citadel',aether())]:
        path=BASE/'structure/special'/(name+'.nbt')
        count,raw=dump_nbt(build,path)
        print(f'{path}: {count} blocks; {raw} uncompressed bytes')

if __name__=='__main__':main()
