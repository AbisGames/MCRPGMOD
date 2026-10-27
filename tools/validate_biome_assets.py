#!/usr/bin/env python3
"""Validate original capital-themed biome assets independently from Minecraft.

This is a strict static preflight: it checks registry references, texture
integrity and 125 biome data entries. Runtime testing is still required.
"""
from pathlib import Path
import json
import struct
import zlib

ROOT=Path(__file__).resolve().parents[1]
DATA=ROOT/"src/main/resources/data/pipican"
ASSETS=ROOT/"src/main/resources/assets/pipican"
catalog=json.loads((DATA/"worldgen/biome_theme_catalog.json").read_text())
realms=json.loads((DATA/"worldgen/region_map.json").read_text())
palettes=catalog["biomes"]

assert len(palettes)==27
assert len({r["id"] for r in palettes})==27
assert len(realms["macro_regions"])==22
assert sum(len(r["subregions"]) for r in realms["macro_regions"])==120

def check_png(path):
    src=path.read_bytes()
    assert src[:8]==bytes([137,80,78,71,13,10,26,10]),path
    off=8
    idat=bytearray()
    width=height=0
    while off<len(src):
        n=struct.unpack_from(">I",src,off)[0]
        tag=src[off+4:off+8]
        val=src[off+8:off+8+n]
        actual=struct.unpack_from(">I",src,off+8+n)[0]
        assert zlib.crc32(tag+val)&0xffffffff==actual,(path,tag)
        if tag==b"IHDR":
            width,height=struct.unpack_from(">II",val,0)
            assert (width,height)==(16,16),path
        if tag==b"IDAT":idat.extend(val)
        off+=12+n
    payload=zlib.decompress(idat)
    assert len(payload)==16*(1+16*4),path

for p in palettes:
    id=p["id"]
    for typ in ("strata","trunk","canopy","bloom"):
        name=f"{id}_{typ}"
        texture=ASSETS/f"textures/block/{name}.png"
        check_png(texture)
        for ref in (ASSETS/f"models/block/{name}.json",ASSETS/f"items/{name}.json",
                    ASSETS/f"blockstates/{name}.json",DATA/f"loot_table/blocks/{name}.json"):
            json.loads(ref.read_text())
    for feature in ("tree","bloom","outcrop"):
        feature_name=f"{id}_{feature}"
        for folder in ("configured_feature","placed_feature"):
            resource=DATA/f"worldgen/{folder}/{feature_name}.json"
            assert resource.is_file(),resource
            json.loads(resource.read_text())
    for modifier in ("blooms","outcrops"):
        res=DATA/f"neoforge/biome_modifier/{id}_{modifier}.json"
        assert json.loads(res.read_text())["type"]=="neoforge:add_features"
    tree_modifier=DATA/f"neoforge/biome_modifier/{id}_trees.json"
    assert tree_modifier.is_file()==(p["trees_per_chunk"]>0)

count=0
for macro in realms["macro_regions"]:
    for child in macro["subregions"]:
        path=DATA/f"worldgen/biome/{macro['id']}/{child['id']}.json"
        assert path.is_file(),path
        json.loads(path.read_text())
        count+=1
assert count==120
for p in palettes[22:]:
    assert (DATA/f"worldgen/biome/{p['id']}.json").is_file()
surface=json.loads((DATA/"worldgen/noise_settings/rpg_overworld.json").read_text())["surface_rule"]["sequence"]
geology=[e for e in surface if isinstance(e,dict) and e.get("type")=="minecraft:sequence" and len(e.get("sequence",[]))==27]
assert len(geology)==1,"expected region-specific cliff strata rules"

capitals=json.loads((DATA/"worldgen/fixed_capital_locations.json").read_text())["capitals"]
assert len(capitals)==22
region_lookup={r["id"]:r for r in realms["macro_regions"]}
for castle in capitals:
    assert castle["region"] in region_lookup
    assert castle["subregion"] in {x["id"] for x in region_lookup[castle["region"]]["subregions"]}
    assert castle["placement"]=="fixed"

print("PASS: 27 palettes; 108 custom block textures; 81 configured and 81 placed features;")
print("      125 biome definitions; 22 protected capital subregions; geology rules.")
