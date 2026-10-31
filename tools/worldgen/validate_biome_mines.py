#!/usr/bin/env python3
"""Verify all 27 biome-specific mine templates contain their own ore.

A compressed Minecraft structure palette only contains states referenced by
block records, so a matching palette state proves actual ore placement.
Also checks that renewal provenance includes each registered mine and each
ore drops its matching regional material.
"""
import gzip
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[2]
DATA=ROOT/"src/main/resources/data/pipican"
MINES=DATA/"structure/regional"
TAGS=DATA/"tags/worldgen/structure/renewable_mines.json"
LOOT=DATA/"loot_table/blocks"
# Each region's renewable mine holds one T1 (common) ore of the tiered catalog (tools/ore_builder/catalog.py).
EXPECTED={"whitecrown_icecap":"frostcrystal","northern_ice_sea":"frostcrystal","pinewall_taiga":"cold_iron","fjordmark":"stormsilver","stormcoast":"stormsilver","khagan_steppe":"corundum","western_ocean":"tideglass","elderwood":"amber","heartlands":"electrum","greyspine_mountains":"runic_iron","jade_empire":"verdantite","marshfen":"tin","inner_sea":"pearlsilver","ashlands":"nitre","lotus_delta":"pearlsilver","sunscorch_desert":"sunglass","riverlands":"electrum","spice_coast":"corundum","emerald_jungle":"verdantite","coral_isles":"tideglass","goldgrass_savanna":"corundum","redrock_badlands":"redrock_iron","aetheris_expanse":"truesilver","verdant_depths":"verdantite","umbral_chasm":"morkite","crimson_wilds":"redrock_iron","shimmering_reefs":"tideglass"}
registered=set(json.loads(TAGS.read_text())["values"])
ore_ids=set(EXPECTED.values())
pickaxe=set(json.loads((ROOT/"src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json").read_text())["values"])
for region,ore in EXPECTED.items():
    for kind in ("mine","dungeon"):
        pool=json.loads((DATA/f"worldgen/template_pool/regional/{region}/{kind}.json").read_text())
        for element in pool["elements"]:
            processors=element["element"]["processors"]["processors"]
            assert any(p.get("processor_type")=="pipican:expedition_foundation" for p in processors),f"Missing native foundation: {region}/{kind}"
    assert f"pipican:{ore}_ore" in pickaxe,f"Ore cannot be mined with a pickaxe: {ore}"
    mine=MINES/region/"mine.nbt"
    assert mine.exists(),f"Missing NBT mine: {region}"
    payload=gzip.decompress(mine.read_bytes())
    wanted=f"pipican:{ore}_ore".encode()
    assert wanted in payload,f"{region} mine does not contain {ore} ore blocks"
    other=[o for o in ore_ids if o!=ore and f"pipican:{o}_ore".encode() in payload]
    assert not other,f"{region} mine contains foreign ores: {other}"
    assert f"pipican:regional/{region}/mine" in registered,f"Missing renewable structure tag {region}"
    loot=json.loads((LOOT/(ore+"_ore.json")).read_text())
    assert loot["type"]=="minecraft:block"
    entries=[e for p in loot["pools"] for e in p["entries"]]
    # Ores drop their raw form (metals) or the gem itself, somewhere among the loot alternatives.
    names=json.dumps(entries)
    assert f'"pipican:raw_{ore}"' in names or f'"pipican:{ore}"' in names,f"Wrong ore drop {ore}"
assert len(registered)==len(EXPECTED),f"Expected exactly 27 renewal-enabled mines, found {len(registered)}"
native_pools=list((DATA/"worldgen/template_pool/settlements").rglob("*.json"))
native_pools.extend((DATA/"worldgen/template_pool/regional").glob("*/village.json"))
for path in native_pools:
    pool=json.loads(path.read_text())
    for element in pool["elements"]:
        processors=element["element"]["processors"]["processors"]
        assert any(p.get("processor_type")=="pipican:expedition_foundation" for p in processors),f"Missing native settlement foundation: {path}"
for path in (DATA/"worldgen/template_pool/regional").glob("*/*.json"):
    if path.stem not in ("mine","dungeon"):continue
    for element in json.loads(path.read_text())["elements"]:
        assert element["element"]["element_type"]=="pipican:expedition",f"Missing native corridor bounds: {path}"
        processors=element["element"]["processors"]["processors"]
        assert any(p.get("processor_type")=="pipican:expedition_access" for p in processors),f"Missing expedition access processor: {path}"
print("PASS: 27 biome-specific mines hold one T1 ore each, with matching loot and renewal structure tags")
print(f"PASS: 54 expedition pools and {len(native_pools)} regional settlement pools have native foundations")
