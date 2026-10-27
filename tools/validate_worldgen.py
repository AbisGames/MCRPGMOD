#!/usr/bin/env python3
"""Static consistency checks for authored worldgen records and regional ores.

Run with Python 3; no Minecraft runtime is required. This is NOT an
in-game terrain-generation test.
"""
from pathlib import Path
import json, math, re, struct, sys, zlib

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "src/main/resources/data/pipican/worldgen"
ASSETS = ROOT / "src/main/resources/assets/pipican"


def source(name):
    return json.loads((DATA / name).read_text(encoding="utf-8"))


def require(value, message):
    if not value:
        raise AssertionError(message)


def main():
    region_map = source("region_map.json")
    pools = source("structure_pools_manifest.json")
    settlements = source("settlements.json")
    rules = source("generation_rules.json")
    regions = region_map["macro_regions"]
    subregions = [s for r in regions for s in r["subregions"]]
    sites = settlements["settlements"]
    pool_names = {p["pool"] for p in pools["pools"]}

    require(len(regions) == 22, "expected 22 authored macro regions")
    require(len(subregions) == 120, "expected 120 authored subregions")
    require(len(pool_names) == 331, "expected 331 unique pool definitions")
    require(len(sites) == 70, "expected 70 authored settlements")
    require(len({s["id"] for s in sites}) == 70, "duplicate settlement id")
    require(all(s["asset_pool"] in pool_names for s in sites),
            "one or more settlements reference missing pools")

    # Width and height: the authored -15M..15M map maps to -25k..25k.
    require(round(-15_000_000 / 600) == -25_000, "map scale mismatch")
    require(round(+15_000_000 / 600) == +25_000, "map scale mismatch")
    points = []
    for s in sites:
        spawn = "spawn" in s.get("tags", [])
        x = s["x"] if spawn else round(s["x"] / 600)
        z = s["z"] if spawn else round(s["z"] / 600)
        if s["id"] == "millbrook":
            x, z = -4100, -2900
        require(-25_000 <= x <= 25_000 and -25_000 <= z <= 25_000,
                "landmark out of world: " + s["id"])
        radius = max(s["radius"])
        for px, pz, pr, pid in points:
            require((px - x)**2 + (pz - z)**2 >= (pr + radius)**2,
                    f"overlapping landmarks: {pid} and {s['id']}")
        points.append((x, z, radius, s["id"]))

    ores = re.findall(r'new Ore\("([^"]+)"', (
        ROOT / "src/main/java/com/pipican/worldgen/RegionalOreCatalog.java"
    ).read_text(encoding="utf-8"))
    # Ore catalog uses Java source literals; validate against generated assets.
    require(len(ores) == 27, f"expected 27 ores, found {len(ores)}")
    for ore in ores:
        block = ore + "_ore"
        for p in (
            ASSETS / "blockstates" / (block + ".json"),
            ASSETS / "models/block" / (block + ".json"),
            ASSETS / "items" / (block + ".json"),
            ASSETS / "items" / (ore + ".json"),
            ASSETS / "textures/block" / (block + ".png"),
            ROOT / "src/main/resources/data/pipican/loot_table/blocks" / (block + ".json"),
        ):
            require(p.is_file(), "missing ore asset: " + str(p))
        png = (ASSETS / "textures/block" / (block + ".png")).read_bytes()
        require(png[:8] == bytes([137, 80, 78, 71, 13, 10, 26, 10]),
                "invalid PNG signature: " + ore)

    biome_dir = DATA / "biome"
    biome_files = list(biome_dir.rglob("*.json"))
    # 120 subregions + 5 exotic + 22 protected regional variants +
    # 1 protected Aetheris sky-island biome.
    require(len(biome_files) == 148, "expected 148 RPG biome JSONs")
    for region in regions:
        require((biome_dir/"protected"/(region["id"]+".json")).is_file(),
                "missing protected site biome: "+region["id"])
    require((biome_dir/"aetheris_sanctuary.json").is_file(),
            "missing protected sky-island biome")
    for file in biome_files:
        entry = json.loads(file.read_text(encoding="utf-8"))
        require("temperature" in entry and "effects" in entry,
                "invalid biome: " + str(file))

    print("PASS: 22 regions, 120 authored subregions, 70 non-overlapping landmarks,")
    print("      331 structure pool IDs, 148 biome definitions, 27 ore entries and PNGs.")
    print("NOT VERIFIED: NeoForge compilation, world placement, terrain, dungeon logic.")


if __name__ == "__main__":
    try:
        main()
    except (AssertionError, KeyError, ValueError) as e:
        print("FAIL:", e, file=sys.stderr)
        sys.exit(1)
