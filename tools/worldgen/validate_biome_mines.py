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
EXPECTED={
"whitecrown_icecap":"frostcrystal","northern_ice_sea":"glacierite",
"pinewall_taiga":"wintersteel","fjordmark":"stormsilver",
"stormcoast":"thunderbasalt","khagan_steppe":"skyiron",
"western_ocean":"tideglass","elderwood":"mooncrystal",
"heartlands":"royal_silver","greyspine_mountains":"runic_iron",
"jade_empire":"jade_essence","marshfen":"mirestone",
"inner_sea":"pearlsilver","ashlands":"cinderite",
"lotus_delta":"lotus_opal","sunscorch_desert":"sunglass",
"riverlands":"sunstone","spice_coast":"saffron_gold",
"emerald_jungle":"verdantite","coral_isles":"tidalite",
"goldgrass_savanna":"dawnbronze","redrock_badlands":"redrock_iron",
"aetheris_expanse":"skycrystal","verdant_depths":"lifebloom_crystal",
"umbral_chasm":"umbrite","crimson_wilds":"bloodstone",
"shimmering_reefs":"abyssal_shard"}
registered=set(json.loads(TAGS.read_text())["values"])
ore_ids=set(EXPECTED.values())
pickaxe=set(json.loads((ROOT/"src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json").read_text())["values"])
for region,ore in EXPECTED.items():
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
    assert any(e.get("name")==f"pipican:{ore}" for e in entries),f"Wrong ore drop {ore}"
assert len(registered)==len(EXPECTED),f"Expected exactly 27 renewal-enabled mines, found {len(registered)}"
print("PASS: 27 biome-specific mines have their own ore, matching loot and renewal structure tags")
